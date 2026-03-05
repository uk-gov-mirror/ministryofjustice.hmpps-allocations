package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDateTime

data class RiskPredictorV2 @JsonCreator constructor(
  override val completedDate: LocalDateTime?,
  override val source: String?,
  override val status: String?,
  @Schema(description = "Version of the output", allowableValues = ["2"], defaultValue = "2")
  override val outputVersion: String = "2",
  override val output: RiskPredictorOutputV2?,
) : RiskPredictor<RiskPredictorOutputV2> {
  override fun getRSRScoreLevel(): String? = this.output?.combinedSeriousReoffendingPredictor?.band
  override fun getRSRPercentageScore(): BigDecimal? = this.output?.combinedSeriousReoffendingPredictor?.score
  override fun getOGRSScoreLevel(): String? = this.output?.allReoffendingPredictor?.band
  override fun getOGRSPercentageScore(): BigDecimal? = this.output?.allReoffendingPredictor?.score
}

data class RiskPredictorOutputV2 @JsonCreator constructor(
  val allReoffendingPredictor: AllReoffendingPredictor?,
  val violentReoffendingPredictor: ViolentReoffendingPredictor?,
  val seriousViolentReoffendingPredictor: SeriousViolentReoffendingPredictor?,
  val directContactSexualReoffendingPredictor: DirectContactSexualReoffendingPredictor?,
  val indirectImageContactSexualReoffendingPredictor: IndirectImageContactSexualReoffendingPredictor?,
  val combinedSeriousReoffendingPredictor: CombinedSeriousReoffendingPredictor?,
)

data class AllReoffendingPredictor @JsonCreator constructor(
  val staticOrDynamic: String?,
  val score: BigDecimal?,
  val band: String?,
)

data class ViolentReoffendingPredictor @JsonCreator constructor(
  val staticOrDynamic: String?,
  val score: BigDecimal?,
  val band: String?,
)

data class SeriousViolentReoffendingPredictor @JsonCreator constructor(
  val staticOrDynamic: String?,
  val score: BigDecimal?,
  val band: String?,
)

data class DirectContactSexualReoffendingPredictor @JsonCreator constructor(
  val score: BigDecimal?,
  val band: String?,
)

data class IndirectImageContactSexualReoffendingPredictor @JsonCreator constructor(
  val score: BigDecimal?,
  val band: String?,
)

data class CombinedSeriousReoffendingPredictor @JsonCreator constructor(
  val algorithmVersion: String?,
  val staticOrDynamic: String?,
  val score: BigDecimal?,
  val band: String?,
)
