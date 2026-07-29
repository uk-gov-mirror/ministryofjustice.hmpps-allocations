package uk.gov.justice.digital.hmpps.hmppsallocations.client

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.TierWithStatus
import java.time.Duration

private const val NUMBER_OF_RETRIES = 3L
private const val RETRY_INTERVAL = 3L
private const val TIMEOUT_VALUE = 30000L

@Suppress("SwallowedException")
class HmppsTierApiClient(private val webClient: WebClient) {

  suspend fun getTierByCrn(crn: String): TierWithStatus? {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .get()
          .uri("/v3/crn/{crn}/tier", crn)
          .retrieve()
          .onStatus({ status -> status.is5xxServerError }) {
            Mono.error(AllocationsServerError("Internal server error"))
          }
          .onStatus({ status -> status.value() == HttpStatus.NOT_FOUND.value() }) {
            log.debug("Tier not found for crn $crn")
            Mono.error(TierNotFoundException("Tier not found for CRN $crn"))
          }
          .bodyToMono(TierWithStatus::class.java)
          .retryWhen(
            Retry.backoff(NUMBER_OF_RETRIES, Duration.ofSeconds(RETRY_INTERVAL))
              .filter { it is AllocationsServerError },
          )
          .awaitSingleOrNull()!!
      }
    } catch (e: TimeoutCancellationException) {
      AssessRisksNeedsApiClient.Companion.log.warn("/crn/$crn/tier failed for timeout", e)
      throw AllocationsWebClientTimeoutException(e.message!!)
    } catch (e: AllocationsServerError) {
      throw AllocationsFailedDependencyException("/crn/$crn/tier failed for 500 error ${e.message}")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
class TierNotFoundException(msg: String) : RuntimeException()
