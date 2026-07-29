package uk.gov.justice.digital.hmpps.hmppsallocations.client

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.TierWithStatus

class HmppsTierApiClientTest {

  @Test
  fun `test 500 retries on tier client`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build())
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val exception = assertThrows<RuntimeException> {
      HmppsTierApiClient(webClient).getTierByCrn("X123456")
    }
    assert(exception.message!!.contains("Retries exhausted: 3/3"))
  }

  @Test
  fun `test 404 on tier client`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build())
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val exception = assertThrows<RuntimeException> {
      HmppsTierApiClient(webClient).getTierByCrn("X123456")
    }
    assert(exception is TierNotFoundException)
  }

  @Test
  fun `test successful response on tier client`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(
        ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body("""{"tierScore":"A","provisional":false}""")
          .build(),
      )
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = HmppsTierApiClient(webClient).getTierByCrn("X123456")
    assert(result == TierWithStatus("A", false))
  }
}
