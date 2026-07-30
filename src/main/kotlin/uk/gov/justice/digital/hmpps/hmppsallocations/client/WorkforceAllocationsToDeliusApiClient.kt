package uk.gov.justice.digital.hmpps.hmppsallocations.client

import com.fasterxml.jackson.annotation.JsonCreator
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withTimeout
import org.owasp.html.PolicyFactory
import org.owasp.html.Sanitizers
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.function.client.bodyToFlow
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.createExceptionAndAwait
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.CrnDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusAccessRestrictionDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusAllocatedCaseView
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusApopUser
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusCaseView
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusProbationRecord
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusRisk
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusTeams
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.PersonOnProbationStaffDetailsResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.UnallocatedEvents
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.EntityNotFoundException
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

private const val NUMBER_OF_RETRIES = 3L
private const val RETRY_INTERVAL = 3L
private const val TIMEOUT_VALUE = 30000L

@Suppress("TooManyFunctions", "SwallowedException")
class WorkforceAllocationsToDeliusApiClient(private val webClient: WebClient) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val policy: PolicyFactory = Sanitizers.FORMATTING.and(Sanitizers.LINKS)
  }

  /*
   * returns a list of all users with permission to use the aPop tool (Delius role 'MAABT001')
   */
  suspend fun getApopUsers(): List<DeliusApopUser> {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .get()
          .uri("/users")
          .retrieve()
          .onStatus({ it.is5xxServerError }) { res ->
            res.createException().flatMap {
              Mono.error(
                AllocationsFailedDependencyException("/users failed with ${res.statusCode()}"),
              )
            }
          }
          .awaitBody()
      }
    } catch (e: TimeoutCancellationException) {
      log.warn("/users failed for timeout", e)
      throw AllocationsWebClientTimeoutException(e.message!!)
    }
  }
  suspend fun getOfficerView(staffCode: String): OfficerView {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .get()
          .uri("/staff/{staffCode}/officer-view", staffCode)
          .retrieve()
          .onStatus({ it.is5xxServerError }) { res ->
            res.createException().flatMap {
              Mono.error(
                AllocationsFailedDependencyException("/staff/$staffCode/officer-view failed with ${res.statusCode()}"),
              )
            }
          }
          .awaitBodyOrNull<OfficerView>()
          ?: throw EntityNotFoundException("Officer view not found for staff code $staffCode")
      }
    } catch (e: TimeoutCancellationException) {
      log.warn("/staff/$staffCode/officer-view failed for timeout", e)
      throw AllocationsWebClientTimeoutException(e.message!!)
    }
  }

  suspend fun getUserAccessRestrictionsByCrn(crn: String): DeliusAccessRestrictionDetails {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .get()
          .uri("person/{crn}/limited-access/all", crn)
          .retrieve()
          .onStatus({ it.is5xxServerError }) { res ->
            res.createException().flatMap {
              Mono.error(
                AllocationsFailedDependencyException("/person/$crn/limited-access/all failed with ${res.statusCode()}"),
              )
            }
          }
          .awaitBody()
      }
    } catch (e: TimeoutCancellationException) {
      log.warn("/person/$crn/limited-access/all failed for timeout", e)
      throw AllocationsWebClientTimeoutException(e.message!!)
    }
  }

  suspend fun getAccessRestrictionsForStaffCodesByCrn(crn: String, staffCodes: List<String>): DeliusAccessRestrictionDetails {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .post()
          .uri("person/{crn}/limited-access", crn)
          .bodyValue(staffCodes)
          .retrieve()
          .onStatus({ it.is5xxServerError }) { res ->
            res.createException().flatMap {
              Mono.error(
                AllocationsFailedDependencyException("/person/$crn/limited-access failed with ${res.statusCode()}"),
              )
            }
          }
          .awaitBody()
      }
    } catch (e: TimeoutCancellationException) {
      log.warn("/person/$crn/limited-access failed for timeout", e)
      throw AllocationsWebClientTimeoutException(e.message!!)
    }
  }
  suspend fun getUserAccess(crns: List<String>, username: String? = null): DeliusUserAccess {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .post()
          .uri { ub ->
            ub.path("/users/limited-access")
            username?.also { ub.queryParam("username", it) }
            ub.build()
          }
          .body(BodyInserters.fromValue(crns))
          .awaitExchange { response ->
            when (response.statusCode()) {
              HttpStatus.OK -> response.awaitBody()
              HttpStatus.INTERNAL_SERVER_ERROR -> {
                log.warn("/users/limited-access failed for $crns")
                throw AllocationsFailedDependencyException("/users/limited-access failed")
              }

              else -> throw response.createExceptionAndAwait()
            }
          }
      }
    } catch (e: TimeoutCancellationException) {
      log.warn("/users/limited-access failed for timeout", e)
      throw AllocationsWebClientTimeoutException(e.message!!)
    }
  }

  suspend fun getTeamsByUsername(username: String): DeliusTeams {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .get()
          .uri("users/{username}/teams", username)
          .retrieve()
          .onStatus({ it.is5xxServerError }) { res ->
            res.createException().flatMap {
              Mono.error(
                AllocationsFailedDependencyException("/users/$username/teams failed with ${res.statusCode()}"),
              )
            }
          }
          .awaitBody()
      }
    } catch (e: TimeoutCancellationException) {
      log.warn("/users/$username/teams failed for timeout", e)
      throw AllocationsWebClientTimeoutException(e.message!!)
    }
  }

  suspend fun getUserAccess(crn: String, username: String? = null): DeliusCaseAccess? = getUserAccess(listOf(crn), username).access.firstOrNull { it.crn == crn }

  suspend fun getDeliusCaseDetailsCases(cases: List<UnallocatedCaseEntity>): Flow<DeliusCaseDetail> = getDeliusCaseDetails(
    caseDetails = GetCaseDetails(
      cases.map { CaseIdentifier(it.crn, it.convictionNumber.toString()) },
    ),
  )
    .flatMapIterable { it.cases }
    .asFlow()

  suspend fun getDeliusCaseDetails(crn: String, convictionNumber: Long): Mono<DeliusCaseDetails> = getDeliusCaseDetails(
    caseDetails = GetCaseDetails(
      listOf(CaseIdentifier(crn, convictionNumber.toString())),
    ),
  ).onErrorReturn(
    DeliusCaseDetails(
      cases = emptyList(),
    ),
  )

  private suspend fun getDeliusCaseDetails(caseDetails: GetCaseDetails): Mono<DeliusCaseDetails> {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .post()
          .uri("/allocation-demand")
          .body(Mono.just(caseDetails), GetCaseDetails::class.java)
          .retrieve()
          .onStatus({ it.is5xxServerError }) { res ->
            res.createException().flatMap {
              Mono.error(
                AllocationsFailedDependencyException("/allocation-demand failed with ${res.statusCode()}"),
              )
            }
          }
          .bodyToMono(DeliusCaseDetails::class.java)
      }
    } catch (e: TimeoutCancellationException) {
      log.warn("/allocation-demand failed for timeout", e)
      throw AllocationsWebClientTimeoutException(e.message!!)
    }
  }

  suspend fun getDocuments(crn: String): Flow<Document> {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .get()
          .uri("/offenders/{crn}/documents", crn)
          .retrieve()
          .onStatus({ it.is5xxServerError }) { res ->
            res.createException().flatMap {
              Mono.error(
                AllocationsFailedDependencyException("/offenders/$crn/documents failed with ${res.statusCode()}"),
              )
            }
          }
          .bodyToFlow<Document>()
      }
    } catch (e: TimeoutCancellationException) {
      log.warn("/offenders/$crn/documents failed for timeout", e)
      throw AllocationsWebClientTimeoutException(e.message!!)
    }
  }

  fun getDocumentById(crn: String, documentId: String): Mono<ResponseEntity<Resource>> = webClient
    .get()
    .uri("/offenders/{crn}/documents/{documentId}", crn, documentId)
    .retrieve()
    .onStatus({ it.is5xxServerError }) { res ->
      res.createException().flatMap {
        Mono.error(
          AllocationsFailedDependencyException("/offenders/$crn/documents/{documentId} failed with ${res.statusCode()}"),
        )
      }
    }
    .toEntity(Resource::class.java)
    .timeout(Duration.ofMillis(TIMEOUT_VALUE))
    .onErrorMap(AllocationsWebClientTimeoutException::class.java) {
      AllocationsWebClientTimeoutException(it.localizedMessage)
    }

  suspend fun getDeliusCaseView(crn: String, convictionNumber: Long): Mono<DeliusCaseView> {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .get()
          .uri("/allocation-demand/{crn}/{convictionNumber}/case-view", crn, convictionNumber)
          .retrieve()
          .onStatus({ it.is5xxServerError }) { res ->
            res.createException().flatMap {
              Mono.error(
                AllocationsFailedDependencyException("/allocation-demand/$crn/{convictionNumber}/case-view failed with ${res.statusCode()}"),
              )
            }
          }
          .bodyToMono()
      }
    } catch (e: TimeoutCancellationException) {
      log.warn("/allocation-demand/$crn/$convictionNumber/case-view failed for timeout", e)
      throw AllocationsWebClientTimeoutException(e.message!!)
    }
  }

  suspend fun getAllocatedDeliusCaseView(crn: String): Mono<DeliusAllocatedCaseView> {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .get()
          .uri("/reallocation/{crn}/case-view", crn)
          .retrieve()
          .onStatus({ it.is5xxServerError }) { res ->
            res.createException().flatMap {
              Mono.error(
                AllocationsFailedDependencyException("/reallocation/$crn/case-view failed with ${res.statusCode()}"),
              )
            }
          }.bodyToMono()
      }
    } catch (e: TimeoutCancellationException) {
      log.warn("/reallocation/$crn/case-view failed for timeout", e)
      throw AllocationsWebClientTimeoutException(e.message!!)
    }
  }

  suspend fun getProbationRecord(crn: String, excludeConvictionNumber: Long): DeliusProbationRecord {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .get()
          .uri("/allocation-demand/{crn}/{excludeConvictionNumber}/probation-record", crn, excludeConvictionNumber)
          .retrieve()
          .onStatus({ it.is5xxServerError }) { res ->
            res.createException().flatMap {
              Mono.error(
                AllocationsFailedDependencyException("/allocation-demand/$crn/$excludeConvictionNumber/probation-record failed with ${res.statusCode()}"),
              )
            }
          }
          .awaitBody()
      }
    } catch (e: TimeoutCancellationException) {
      log.warn("/allocation-demand/$crn/$excludeConvictionNumber/probation-record failed for timeout", e)
      throw AllocationsWebClientTimeoutException(e.message!!)
    }
  }

  suspend fun getDeliusRisk(crn: String): DeliusRisk {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .get()
          .uri("/allocation-demand/{crn}/risk", crn)
          .retrieve()
          .onStatus({ it.is5xxServerError }) { res ->
            res.createException().flatMap {
              Mono.error(
                AllocationsFailedDependencyException("/allocation-demand/$crn/risk failed with ${res.statusCode()}"),
              )
            }
          }
          .awaitBody()
      }
    } catch (e: TimeoutCancellationException) {
      log.warn("/allocation-demand/$crn/risk failed for timeout", e)
      throw AllocationsWebClientTimeoutException(e.message!!)
    }
  }

  suspend fun getUnallocatedEvents(crn: String): UnallocatedEvents? {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .get()
          .uri("/allocation-demand/{crn}/unallocated-events", crn)
          .retrieve()
          .onStatus({ status -> status == HttpStatus.GATEWAY_TIMEOUT }) {
            Mono.error(AllocationsGatewayTimeoutError("Gateway timeout"))
          }
          .onStatus({ status -> status.is5xxServerError }) {
            Mono.error(AllocationsServerError("Internal server error"))
          }
          .onStatus({ status -> status.value() == HttpStatus.FORBIDDEN.value() }) {
            Mono.error(ForbiddenOffenderError("Unable to access offender details for $crn"))
          }
          .onStatus({ status -> status.value() == HttpStatus.NOT_FOUND.value() }) {
            Mono.error(EventsNotFoundError("Unallocated events not found for $crn"))
          }
          .bodyToMono(UnallocatedEvents::class.java)
          .retryWhen(
            Retry.backoff(NUMBER_OF_RETRIES, Duration.ofSeconds(RETRY_INTERVAL))
              .filter { (it is AllocationsServerError || it is AllocationsGatewayTimeoutError) },
          )
          .doOnError { log.warn("getUnallocatedEvents failed for $crn", it) }
          .awaitSingleOrNull()
      }
    } catch (e: TimeoutCancellationException) {
      log.warn("/allocation-demand/$crn/unallocated-events failed for timeout", e)
      throw AllocationsWebClientTimeoutException(e.message!!)
    } catch (e: AllocationsServerError) {
      throw AllocationsFailedDependencyException("/allocation-demand/$crn/unallocated-events failed for 500 error ${e.message}")
    }
  }

  suspend fun personOnProbationStaffDetails(crn: String, staffCode: String): PersonOnProbationStaffDetailsResponse {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .get()
          .uri("/allocation-demand/impact?crn={crn}&staff={staffCode}", crn, staffCode)
          .retrieve()
          .onStatus({ it.is5xxServerError }) { res ->
            res.createException().flatMap {
              Mono.error(
                AllocationsFailedDependencyException("/allocation-demand/impact?crn=$crn&staff=$staffCode failed with ${res.statusCode()}"),
              )
            }
          }
          .awaitBody()
      }
    } catch (e: TimeoutCancellationException) {
      log.warn("/allocation-demand/impact/crn=$crn&staff=$staffCode failed for timeout", e)
      throw AllocationsWebClientTimeoutException(e.message!!)
    }
  }

  suspend fun getCrnDetails(crn: String): CrnDetails {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .get()
          .uri("/person/{crn}/reallocation-details", crn)
          .retrieve()
          .onStatus({ it.is5xxServerError }) { res ->
            res.createException().flatMap {
              Mono.error(
                AllocationsFailedDependencyException("/person/{crn}/reallocation-details failed with ${res.statusCode()}"),
              )
            }
          }
          .awaitBody()
      }
    } catch (e: TimeoutCancellationException) {
      log.warn("/person/{crn}/reallocation-details failed for timeout", e)
      throw AllocationsWebClientTimeoutException(e.message!!)
    }
  }

  suspend fun getAllocatedTeam(crn: String, convictionNumber: Int): AllocatedEvent? {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .get()
          .uri("allocation-completed/order-manager?crn={crn}&eventNumber={convictionNumber}", crn, convictionNumber)
          .retrieve()
          .onStatus({ status -> status.value() == HttpStatus.NOT_FOUND.value() }) {
            Mono.error(EmptyTeamForEventException("Unable to find allocated team for $crn"))
          }
          .onStatus({ status -> status.value() == HttpStatus.FORBIDDEN.value() }) {
            Mono.error(ForbiddenOffenderError("Unable to access allocated team for $crn , event number: $convictionNumber"))
          }
          .onStatus({ status -> status.value() == HttpStatus.GATEWAY_TIMEOUT.value() }) {
            Mono.error(AllocationsGatewayTimeoutError("Gateway timeout"))
          }
          .onStatus({ status -> status.value() == HttpStatus.INTERNAL_SERVER_ERROR.value() }) {
            Mono.error(AllocationsServerError("Internal server error"))
          }
          .bodyToMono(AllocatedEvent::class.java)
          .retryWhen(
            Retry.backoff(NUMBER_OF_RETRIES, Duration.ofSeconds(RETRY_INTERVAL))
              .filter { it is AllocationsServerError || it is AllocationsGatewayTimeoutError },
          )
          .doOnError { log.warn("getAllocatedTeam failed for $crn", it) }
          .awaitSingleOrNull()
      }
    } catch (e: TimeoutCancellationException) {
      log.warn("/allocation-completed/order-manager?crn=$crn&eventNumber=$convictionNumber failed for timeout", e)
      throw AllocationsWebClientTimeoutException(e.message!!)
    } catch (e: AllocationsServerError) {
      throw AllocationsFailedDependencyException("/allocation-completed/order-manager?crn=$crn&eventNumber=$convictionNumber failed for 500, ${e.message}")
    }
  }
}

data class OfficerView(
  val code: String,
  val name: Name,
  val grade: String,
  val email: String,
  val casesDueToEndInNext4Weeks: Int,
  val releasesWithinNext4Weeks: Int,
  val paroleReportsToCompleteInNext4Weeks: Int,
)

class ForbiddenOffenderError(msg: String) : RuntimeException(msg)
class AllocationsServerError(msg: String) : RuntimeException(msg)
class AllocationsGatewayTimeoutError(msg: String) : RuntimeException(msg)
class AllocationsWebClientTimeoutException(msg: String) : RuntimeException(msg)
class AllocationsFailedDependencyException(msg: String) : RuntimeException(msg)
class EventsNotFoundError(msg: String) : RuntimeException(msg)
class EmptyTeamForEventException(msg: String) : RuntimeException(msg)
data class CaseIdentifier(val crn: String, val eventNumber: String)

data class GetCaseDetails(val cases: List<CaseIdentifier>)

data class DeliusCaseDetail(
  val crn: String,
  val name: Name,
  val sentence: Sentence,
  val initialAppointment: InitialAppointment?,
  val event: Event,
  val probationStatus: ProbationStatus,
  val communityPersonManager: CommunityPersonManager?,
  val mostRecentAllocatedEvent: RecentAllocatedEvent?,
  var type: String,
  val handoverDate: LocalDate?,
)

data class Event(val number: String)

data class ProbationStatus(
  val status: String,
  val description: String,
)

data class InitialAppointment(val date: LocalDate, val staff: Staff)

data class RecentAllocatedEvent(val number: String, val manager: RecentManager)

data class RecentManager(val code: String?, val name: Name, val teamCode: String?, val grade: String?, val allocated: Boolean)

data class Staff @JsonCreator constructor(
  val name: Name,
)
data class DeliusCaseDetails(val cases: List<DeliusCaseDetail>)

data class Name(var forename: String, var middleName: String?, var surname: String) {
  init {
    forename = Sanitizers.FORMATTING.and(Sanitizers.LINKS).sanitize(forename)
    middleName = Sanitizers.FORMATTING.and(Sanitizers.LINKS).sanitize(middleName)
    surname = Sanitizers.FORMATTING.and(Sanitizers.LINKS).sanitize(surname)
  }
  fun getCombinedName() = "$forename ${middleName?.takeUnless { it.isBlank() }?.let { "$middleName " } ?: ""}$surname"
}

data class CommunityPersonManager(val name: Name, val grade: String?, val teamCode: String?)

data class Sentence(val date: LocalDate, val length: String)

data class Document @JsonCreator constructor(
  val id: String?,
  var name: String,
  val dateCreated: ZonedDateTime?,
  val sensitive: Boolean,
  val relatedTo: DocumentRelatedTo,
) {
  init {
    name = Sanitizers.FORMATTING.and(Sanitizers.LINKS).sanitize(name)
  }
}

data class DocumentRelatedTo @JsonCreator constructor(
  val type: String,
  var name: String,
  var description: String,
  val event: DocumentEvent?,
) {
  init {
    name = Sanitizers.FORMATTING.and(Sanitizers.LINKS).sanitize(name)
    description = Sanitizers.FORMATTING.and(Sanitizers.LINKS).sanitize(description)
  }
}

data class DocumentEvent @JsonCreator constructor(
  val eventType: String,
  val eventNumber: String,
  val mainOffence: String,
)

data class DeliusCaseAccess(
  val crn: String,
  var userRestricted: Boolean,
  var userExcluded: Boolean,
)

data class DeliusUserAccess(
  val access: List<DeliusCaseAccess>,
)

data class AllocatedEvent @JsonCreator constructor(
  val teamCode: String,
)
