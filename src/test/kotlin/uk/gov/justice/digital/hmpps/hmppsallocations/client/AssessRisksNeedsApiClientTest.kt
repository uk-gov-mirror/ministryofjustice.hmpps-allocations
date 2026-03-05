package uk.gov.justice.digital.hmpps.hmppsallocations.client

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class AssessRisksNeedsApiClientTest {

  @Test
  fun `test getLatestCompleteAssessment successful response`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(
        ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body("""{"timeline":[{"status":"COMPLETE","completed":"2023-01-01T12:00:00","assessmentType":"12345"}]}""")
          .build(),
      )
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = AssessRisksNeedsApiClient(webClient).getLatestCompleteAssessment("X123456")
    assert(result != null)
    assert(result?.assessmentType == "12345")
  }

  @Test
  fun `test getLatestCompleteAssessment 404 response`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build())
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = AssessRisksNeedsApiClient(webClient).getLatestCompleteAssessment("X123456")
    assert(result == null)
  }

  @Test
  fun `test getLatestCompleteAssessment 504 response`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(ClientResponse.create(HttpStatus.GATEWAY_TIMEOUT).build())
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val exception = assertThrows<RuntimeException> {
      AssessRisksNeedsApiClient(webClient).getLatestCompleteAssessment("X123456")
    }
    assert(exception.message!!.contains("Retries exhausted"))
  }

  @Test
  fun `test getLatestCompleteAssessment 500 response`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build())
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val exception = assertThrows<RuntimeException> {
      AssessRisksNeedsApiClient(webClient).getLatestCompleteAssessment("X123456")
    }
    assert(exception.message!!.contains("Retries exhausted"))
  }

  @Test
  fun `test getRosh successful response`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(
        ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body("""{"overallRisk":"HIGH","assessedOn":"2023-01-01","riskInCommunity":{"COMMUNITY":"HIGH"}}""")
          .build(),
      )
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = AssessRisksNeedsApiClient(webClient).getRosh("X123456")
    assert(result != null)
    assert(result?.assessedOn == LocalDate.parse("2023-01-01"))
    assert(result?.riskInCommunity?.get("COMMUNITY") == "HIGH")
  }

  @Test
  fun `test getRosh 504 response`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(ClientResponse.create(HttpStatus.GATEWAY_TIMEOUT).build())
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = AssessRisksNeedsApiClient(webClient).getRosh("X123456")

    assert(result?.riskInCommunity == emptyMap<String, String?>())
  }

  @Test
  fun `test getRosh 404 response`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build())
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = AssessRisksNeedsApiClient(webClient).getRosh("X123456")
    assert(result != null)
  }

  @Test
  fun `test getRiskPredictors V1 successful response`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(
        ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body("""[{"completedDate": "2023-01-01T12:00:00","source": "OASYS","status": "COMPLETE","outputVersion": "1","output": {"riskOfSeriousRecidivismScore": {"percentageScore": 0.5,"staticOrDynamic": "STATIC","source": "OASYS","algorithmVersion": "5", "scoreLevel": "HIGH"}}}]""")
          .build(),
      )
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = AssessRisksNeedsApiClient(webClient).getRiskPredictors("X123456").toList().get(0)
    assert(result.completedDate == LocalDateTime.parse("2023-01-01T12:00:00"))
    assert(result.getRSRScoreLevel() == "HIGH")
    assert(result.getRSRPercentageScore() == BigDecimal.valueOf(0.5))
  }

  @Test
  fun `test getRiskPredictors V2 successful response`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(
        ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body(
            """[{"completedDate": "2025-10-23T03:02:59","source": "OASYS","status": "COMPLETE","outputVersion": "2","output": {"combinedSeriousReoffendingPredictor": {"algorithmVersion": "6","staticOrDynamic": "STATIC","score": 0.5,"band": "HIGH"}}}]""",
          )
          .build(),
      )
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = AssessRisksNeedsApiClient(webClient).getRiskPredictors("X123456").toList().get(0)
    assert(result.completedDate == LocalDateTime.parse("2025-10-23T03:02:59"))
    assert(result.getRSRScoreLevel() == "HIGH")
    assert(result.getRSRPercentageScore() == BigDecimal.valueOf(0.5))
  }

  @Test
  fun `test getRiskPredictors 404 response`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build())
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = AssessRisksNeedsApiClient(webClient).getRiskPredictors("X123456").toList()
  }

  @Test
  fun `test getRiskPredictors 504 response`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(ClientResponse.create(HttpStatus.GATEWAY_TIMEOUT).build())
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = AssessRisksNeedsApiClient(webClient).getRiskPredictors("X123456").toList()
    assert(result.get(0).completedDate == null)
    assert(result.get(0).getRSRScoreLevel() == "UNAVAILABLE")
    assert(result.get(0).getRSRPercentageScore() == BigDecimal(Int.MIN_VALUE))
  }
}
