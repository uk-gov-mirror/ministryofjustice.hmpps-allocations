package uk.gov.justice.digital.hmpps.hmppsallocations.service

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.AssessRisksNeedsApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusAllocatedCaseView
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.AllocatedCaseDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.AssessmentDate
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.RiskPredictorV1
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.RiskPredictorV2
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseConvictions
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseRisks
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseRisksV1
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseRisksV2
import java.time.LocalDateTime

@Service
class GetAllocatedCaseService(
  @Qualifier("workforceAllocationsToDeliusApiClientUserEnhanced")
  private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
  @Qualifier("laoService")
  private val laoService: LaoService,
  private val tierApiClient: HmppsTierApiClient,
  @Qualifier("assessRisksNeedsApiClientUserEnhanced")
  private val assessRisksNeedsApiClient: AssessRisksNeedsApiClient,
) {
  suspend fun getCase(userName: String, crn: String): AllocatedCaseDetails? {
    val deliusAllocatedCaseView: DeliusAllocatedCaseView = workforceAllocationsToDeliusApiClient
      .getAllocatedDeliusCaseView(crn).awaitSingle()

    val laoDetails = laoService.getCrnRestrictionStatus(userName, crn)
    val tier = tierApiClient.getTierByCrn(crn)

    return AllocatedCaseDetails.from(
      deliusAllocatedCaseView,
      false,
      tier!!,
      crn,
    )
  }

  suspend fun getAllocatedCaseConvictions(crn: String, excludeConvictionNumber: Long): UnallocatedCaseConvictions? {
    val deliusAllocatedCaseView: DeliusAllocatedCaseView = workforceAllocationsToDeliusApiClient
      .getAllocatedDeliusCaseView(crn).awaitSingle()
    val tier = tierApiClient.getTierByCrn(crn)
    return deliusAllocatedCaseView.let {
      val probationRecord = workforceAllocationsToDeliusApiClient.getProbationRecord(crn, excludeConvictionNumber)
      return UnallocatedCaseConvictions.from(probationRecord, crn, excludeConvictionNumber.toInt(), tier!!)
    }
  }

  suspend fun getCaseRisks(crn: String): UnallocatedCaseRisks<Any>? {
    val tier = tierApiClient.getTierByCrn(crn)
    return workforceAllocationsToDeliusApiClient.getCrnDetails(crn)?.let { caseDetails ->
      val riskPredictor = assessRisksNeedsApiClient.getRiskPredictors(crn)
        .filter { (it.getRSRScoreLevel() != null && it.getRSRPercentageScore() != null) || (it.getOGRSScoreLevel() != null && it.getOGRSPercentageScore() != null) }
        .toList().maxByOrNull { it.completedDate ?: LocalDateTime.MIN }
      val deliusRisk = workforceAllocationsToDeliusApiClient.getDeliusRisk(crn)
      val rosh = assessRisksNeedsApiClient.getRosh(crn)
      return if (riskPredictor?.outputVersion == "2") {
        UnallocatedCaseRisksV2.from(deliusRisk, caseDetails, rosh, riskPredictor as RiskPredictorV2?, tier!!)
      } else {
        UnallocatedCaseRisksV1.from(deliusRisk, caseDetails, rosh, riskPredictor as RiskPredictorV1?, tier!!)
      }
    }
  }

  suspend fun getCaseAssessmentDate(crn: String): AssessmentDate {
    val assessment = assessRisksNeedsApiClient.getLatestCompleteAssessment(crn)
    return AssessmentDate(assessment?.completed)
  }
}
