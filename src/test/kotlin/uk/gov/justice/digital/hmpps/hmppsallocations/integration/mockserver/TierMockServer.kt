package uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.TierApiExtension.Companion.hmppsTier

class TierApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {

    lateinit var hmppsTier: TierMockServer
  }

  override fun beforeAll(context: ExtensionContext?) {
    hmppsTier = TierMockServer()
  }
  override fun beforeEach(context: ExtensionContext?) {
    hmppsTier.reset()
  }
  override fun afterAll(context: ExtensionContext?) {
    hmppsTier.stop()
  }
}
class TierMockServer : ClientAndServer(MOCKSERVER_PORT) {

  companion object {
    private const val MOCKSERVER_PORT = 8082
  }

  fun tierCalculationResponse(crn: String): HttpRequest {
    val request = HttpRequest.request().withPath("/v3/crn/$crn/tier")
    hmppsTier.`when`(request).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody("{\"tierScore\":\"B\",\"provisional\":false}"),
    )
    return request
  }
}
