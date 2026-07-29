package uk.gov.justice.digital.hmpps.hmppsallocations.integration.tier

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.mockserver.verify.VerificationTimes
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.TierApiExtension.Companion.hmppsTier
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity

internal class CalculationEventListenerTest : IntegrationTestBase() {

  @Test
  fun `change tier after event calculation is consumed`() {
    val crn = "X123456"
    hmppsTier.tierCalculationResponse(crn)
    writeUnallocatedCaseToDatabase(crn, "D", 1)
    publishTierCalculationCompleteMessage(crn)
    checkTierHasBeenUpdated(crn, "B", 1)
  }

  @Test
  fun `does not get tier calculation when the crn is not for an unallocated case`() {
    val crn = "J678910"
    val tierCalculationRequest = hmppsTier.tierCalculationResponse(crn)
    publishTierCalculationCompleteMessage(crn)
    whenCalculationQueueIsEmpty()
    whenCalculationMessageHasBeenProcessed()
    hmppsTier.verify(tierCalculationRequest, VerificationTimes.exactly(0))
  }

  @Test
  fun `updates all occurrences of crn after event calculation is consumed`() {
    val crn = "X123456"
    hmppsTier.tierCalculationResponse(crn)
    writeUnallocatedCaseToDatabase(crn, "D", 1)
    writeUnallocatedCaseToDatabase(crn, "D", 2)
    publishTierCalculationCompleteMessage(crn)
    checkTierHasBeenUpdated(crn, "B", 1)
    checkTierHasBeenUpdated(crn, "B", 2)
  }

  private fun writeUnallocatedCaseToDatabase(crn: String, tier: String, convictionNumber: Int) {
    repository.save(
      UnallocatedCaseEntity(
        crn = crn,
        tier = tier,
        provisionalTier = false,
        name = "foo",
        providerCode = "",
        teamCode = "",
        convictionNumber = convictionNumber,
      ),
    )
  }

  private fun checkTierHasBeenUpdated(crn: String, tier: String, convictionNumber: Int) {
    await untilCallTo { repository.findCaseByCrnAndConvictionNumber(crn, convictionNumber) } matches {
      it!!.tier == tier
    }
  }

  private fun publishTierCalculationCompleteMessage(crn: String) {
    hmppsDomainSnsClient
      .publish(
        PublishRequest.builder()
          .topicArn(hmppsDomainTopicArn)
          .message(jsonString(tierCalculationEvent(crn)))
          .messageAttributes(
            mapOf(
              "eventType" to MessageAttributeValue.builder()
                .dataType("String")
                .stringValue("TIER_CALCULATION_COMPLETE")
                .build(),
            ),
          ).build(),
      )
  }
}
