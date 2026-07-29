package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.CrnDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusRisk
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.math.BigDecimal
import java.time.LocalDateTime

data class UnallocatedCaseRisksV2 @JsonCreator constructor(
  @Schema(description = "Offender Name", example = "John Smith")
  override val name: String,
  @Schema(description = "CRN", example = "J111111")
  override val crn: String,
  @Schema(description = "Latest tier of case", example = "D")
  override val tier: String,
  override val provisionalTier: Boolean,
  override val completedDate: LocalDateTime?,
  override val riskVersion: String?,
  override val activeRegistrations: List<UnallocatedCaseRegistration>,
  override val inactiveRegistrations: List<UnallocatedCaseRegistration>,
  override val risk: RiskV2?,
  override val convictionNumber: Int?,
) : UnallocatedCaseRisks<RiskV2> {
  override fun getROSHLevel(): String? = risk?.roshRisk?.getOverallRisk()
  override fun getRSRLevel(): String? = risk?.combinedSeriousReoffendingPredictor?.band
  override fun getOGRSScore(): BigDecimal? = risk?.allReoffendingPredictor?.score

  companion object {
    @Suppress("LongParameterList")
    fun from(
      deliusRisk: DeliusRisk,
      case: UnallocatedCaseEntity,
      rosh: RoshSummary?,
      riskPredictor: RiskPredictorV2?,
    ): UnallocatedCaseRisksV2 = UnallocatedCaseRisksV2(
      case.name,
      case.crn,
      case.tier,
      case.provisionalTier,
      riskPredictor?.completedDate,
      riskPredictor?.outputVersion,
      deliusRisk.activeRegistrations.map { UnallocatedCaseRegistration.from(it) },
      deliusRisk.inactiveRegistrations.map { UnallocatedCaseRegistration.from(it) },
      RiskV2.from(rosh, riskPredictor),
      case.convictionNumber,
    )

    @Suppress("LongParameterList")
    fun from(
      deliusRisk: DeliusRisk,
      case: CrnDetails,
      rosh: RoshSummary?,
      riskPredictor: RiskPredictorV2?,
      tier: String,
      provisionalTier: Boolean,
    ): UnallocatedCaseRisksV2 = UnallocatedCaseRisksV2(
      case.name.getCombinedName(),
      case.crn,
      tier,
      provisionalTier,
      riskPredictor?.completedDate,
      "2",
      deliusRisk.activeRegistrations.map { UnallocatedCaseRegistration.from(it) },
      deliusRisk.inactiveRegistrations.map { UnallocatedCaseRegistration.from(it) },
      RiskV2.from(rosh, riskPredictor),
      null,
    )
  }
}

data class RiskV2 @JsonCreator constructor(
  val roshRisk: RoshSummary?,
  val allReoffendingPredictor: AllReoffendingPredictor?,
  val combinedSeriousReoffendingPredictor: CombinedSeriousReoffendingPredictor?,
) {
  companion object {
    fun from(
      roshRisk: RoshSummary?,
      riskPredictor: RiskPredictorV2?,
    ): RiskV2 = RiskV2(
      roshRisk,
      riskPredictor?.output?.allReoffendingPredictor,
      riskPredictor?.output?.combinedSeriousReoffendingPredictor,
    )
  }
}
