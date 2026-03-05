package uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType
import org.mockserver.verify.VerificationTimes
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.AssessRisksNeedsApiExtension.Companion.assessRisksNeedsApi
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.assessment.assessmentNotFoundResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.assessment.assessmentResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.assessrisksneeds.riskPredictorNotFoundResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.assessrisksneeds.riskPredictorResponseV1
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.assessrisksneeds.riskPredictorResponseV2
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.assessrisksneeds.riskPredictorUnavailableResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.assessrisksneeds.roshResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.assessrisksneeds.roshResponseNoOverallRisk

class AssessRisksNeedsApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    lateinit var assessRisksNeedsApi: AssessRisksNeedsMockServer
  }

  override fun beforeAll(context: ExtensionContext?) {
    assessRisksNeedsApi = AssessRisksNeedsMockServer()
  }

  override fun beforeEach(context: ExtensionContext?) {
    assessRisksNeedsApi.reset()
  }

  override fun afterAll(context: ExtensionContext?) {
    assessRisksNeedsApi.stop()
  }
}

class AssessRisksNeedsMockServer : ClientAndServer(MOCKSERVER_PORT) {

  companion object {
    private const val MOCKSERVER_PORT = 8085
  }

  fun getRoshForCrn(crn: String) {
    val riskRequest =
      HttpRequest.request().withPath("/risks/crn/$crn/widget")

    assessRisksNeedsApi.`when`(riskRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(roshResponse()),
    )
  }

  fun getRoshNoLevelForCrn(crn: String) {
    val riskRequest =
      HttpRequest.request().withPath("/risks/crn/$crn/widget")

    assessRisksNeedsApi.`when`(riskRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(roshResponseNoOverallRisk()),
    )
  }

  fun getRoshNotFoundForCrn(crn: String) {
    val riskRequest =
      HttpRequest.request().withPath("/risks/crn/$crn/widget")

    assessRisksNeedsApi.`when`(riskRequest, Times.exactly(1)).respond(
      HttpResponse.response().withStatusCode(HttpStatus.NOT_FOUND.value()).withContentType(MediaType.APPLICATION_JSON).withBody(
        "{\n" +
          "  \"status\": 404,\n" +
          "  \"developerMessage\": \"System is down\",\n" +
          "  \"errorCode\": 20012,\n" +
          "  \"userMessage\": \"Prisoner Not Found\",\n" +
          "  \"moreInfo\": \"Hard disk failure\"\n" +
          "}",
      ),
    )
  }

  fun getRoshNotFoundForCrnRetry(crn: String) {
    val riskRequest =
      HttpRequest.request().withPath("/risks/crn/$crn/widget")

    assessRisksNeedsApi.`when`(riskRequest, Times.exactly(1)).respond(
      HttpResponse.response().withStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).withContentType(MediaType.APPLICATION_JSON).withBody(
        "{\n" +
          "  \"status\": 500,\n" +
          "  \"developerMessage\": \"System is down\",\n" +
          "  \"errorCode\": 20012,\n" +
          "  \"userMessage\": \"Prisoner Not Found\",\n" +
          "  \"moreInfo\": \"Hard disk failure\"\n" +
          "}",
      ),
    )

    assessRisksNeedsApi.`when`(riskRequest, Times.exactly(1)).respond(
      HttpResponse.response().withStatusCode(HttpStatus.NOT_FOUND.value()).withContentType(MediaType.APPLICATION_JSON).withBody(
        "{\n" +
          "  \"status\": 404,\n" +
          "  \"developerMessage\": \"System is down\",\n" +
          "  \"errorCode\": 20012,\n" +
          "  \"userMessage\": \"Prisoner Not Found\",\n" +
          "  \"moreInfo\": \"Hard disk failure\"\n" +
          "}",
      ),
    )
  }

  fun verifyRoshCalled(crn: String, times: Int) {
    assessRisksNeedsApi.verify(
      HttpRequest.request()
        .withPath("/risks/crn/$crn/widget"),
      VerificationTimes.exactly(times),
    )
  }

  fun getRoshUnavailableForCrn(crn: String) {
    val riskRequest =
      HttpRequest.request().withPath("/risks/crn/$crn/widget")

    assessRisksNeedsApi.`when`(riskRequest, Times.exactly(4)).respond(
      HttpResponse.response().withStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).withContentType(MediaType.APPLICATION_JSON).withBody(
        "{\n" +
          "  \"status\": 500,\n" +
          "  \"developerMessage\": \"System is down\",\n" +
          "  \"errorCode\": 20012,\n" +
          "  \"userMessage\": \"Prisoner Not Found\",\n" +
          "  \"moreInfo\": \"Hard disk failure\"\n" +
          "}",
      ),
    )
  }

  fun getRiskPredictorsV1ForCrn(crn: String) {
    val riskRequest =
      HttpRequest.request().withPath("/risks/predictors/all/crn/$crn")

    assessRisksNeedsApi.`when`(riskRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(riskPredictorResponseV1()),
    )
  }

  fun getRiskPredictorsV2ForCrn(crn: String) {
    val riskRequest =
      HttpRequest.request().withPath("/risks/predictors/all/crn/$crn")

    assessRisksNeedsApi.`when`(riskRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(riskPredictorResponseV2()),
    )
  }

  fun getRiskPredictorsForCrnRetry(crn: String) {
    val riskRequest =
      HttpRequest.request().withPath("/risks/predictors/all/crn/$crn")

    assessRisksNeedsApi.`when`(riskRequest, Times.exactly(4)).respond(
      HttpResponse.response().withStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).withContentType(MediaType.APPLICATION_JSON).withBody(
        "{\n" +
          "  \"status\": 500,\n" +
          "  \"developerMessage\": \"System is down\",\n" +
          "  \"errorCode\": 20012,\n" +
          "  \"userMessage\": \"Prisoner Not Found\",\n" +
          "  \"moreInfo\": \"Hard disk failure\"\n" +
          "}",
      ),
    )
    assessRisksNeedsApi.`when`(riskRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(riskPredictorResponseV1()),
    )
  }

  fun getRiskPredictorsNotFoundForCrn(crn: String) {
    val riskRequest =
      HttpRequest.request().withPath("/risks/predictors/all/crn/$crn")

    assessRisksNeedsApi.`when`(riskRequest, Times.exactly(1)).respond(
      HttpResponse.response().withStatusCode(HttpStatus.NOT_FOUND.value()).withContentType(MediaType.APPLICATION_JSON).withBody(riskPredictorNotFoundResponse()),
    )
  }

  fun getRiskPredictorsUnavailableForCrn(crn: String) {
    val riskRequest =
      HttpRequest.request().withPath("/risks/predictors/all/crn/$crn")

    assessRisksNeedsApi.`when`(riskRequest, Times.exactly(4)).respond(
      HttpResponse.response().withStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).withContentType(MediaType.APPLICATION_JSON).withBody(riskPredictorUnavailableResponse()),
    )
  }

  fun getRiskPredictorsForCrnEmptyList(crn: String) {
    val riskRequest =
      HttpRequest.request().withPath("/risks/predictors/all/crn/$crn")

    assessRisksNeedsApi.`when`(riskRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody("[]"),
    )
  }

  fun verifyRiskPredictorCalled(crn: String, times: Int) {
    assessRisksNeedsApi.verify(
      HttpRequest.request()
        .withPath("/risks/predictors/all/crn/$crn"),
      VerificationTimes.exactly(times),
    )
  }

  fun verifyRiskAssesmentCalled(crn: String, times: Int) {
    assessRisksNeedsApi.verify(
      HttpRequest.request()
        .withPath("/assessments/timeline/crn/$crn"),
      VerificationTimes.exactly(times),
    )
  }

  fun getAssessmentsForCrn(crn: String) {
    val assessmentRequest = HttpRequest.request().withPath("/assessments/timeline/crn/$crn")
    assessRisksNeedsApi.`when`(assessmentRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(assessmentResponse()),
    )
  }

  fun notFoundAssessmentForCrn(crn: String) {
    val assessmentRequest = HttpRequest.request().withPath("/assessments/timeline/crn/$crn")
    assessRisksNeedsApi.`when`(assessmentRequest, Times.exactly(1)).respond(
      HttpResponse.response().withStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).withContentType(MediaType.APPLICATION_JSON)
        .withBody(assessmentNotFoundResponse(crn)),
    )
    assessRisksNeedsApi.`when`(assessmentRequest, Times.exactly(1)).respond(
      HttpResponse.response().withStatusCode(HttpStatus.NOT_FOUND.value()).withContentType(MediaType.APPLICATION_JSON)
        .withBody(assessmentNotFoundResponse(crn)),
    )
  }
}
