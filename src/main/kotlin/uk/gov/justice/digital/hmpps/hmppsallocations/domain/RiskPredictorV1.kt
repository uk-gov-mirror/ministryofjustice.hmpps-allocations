package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDateTime

data class RiskPredictorV1 @JsonCreator constructor(
  override val completedDate: LocalDateTime?,
  override val source: String?,
  override val status: String?,
  @Schema(description = "Version of the output", allowableValues = ["1"], defaultValue = "1")
  override val outputVersion: String = "1",
  override val output: RiskPredictorOutputV1?,
) : RiskPredictor<RiskPredictorOutputV1> {
  override fun getRSRScoreLevel(): String? = this.output?.riskOfSeriousRecidivismScore?.scoreLevel
  override fun getRSRPercentageScore(): BigDecimal? = this.output?.riskOfSeriousRecidivismScore?.percentageScore
  override fun getOGRSScoreLevel(): String? = this.output?.groupReconvictionScore?.scoreLevel
  override fun getOGRSPercentageScore(): BigDecimal? = this.output?.groupReconvictionScore?.twoYears
}

data class RiskPredictorOutputV1 @JsonCreator constructor(
  val groupReconvictionScore: GroupReconvictionScore?,
  val violencePredictorScore: ViolencePredictorScore?,
  val generalPredictorScore: GeneralPredictorScore?,
  val riskOfSeriousRecidivismScore: RiskOfSeriousRecidivismScore?,
  val sexualPredictorScore: SexualPredictorScore?,
)

data class GroupReconvictionScore @JsonCreator constructor(
  val oneYear: BigDecimal?,
  val twoYears: BigDecimal?,
  val scoreLevel: String?,
)

data class ViolencePredictorScore @JsonCreator constructor(
  val ovpStaticWeightedScore: BigDecimal?,
  val ovpDynamicWeightedScore: BigDecimal?,
  val ovpTotalWeightedScore: BigDecimal?,
  val oneYear: BigDecimal?,
  val twoYears: BigDecimal?,
  val ovpRisk: String?,
)

data class GeneralPredictorScore @JsonCreator constructor(
  val ogpStaticWeightedScore: BigDecimal?,
  val ogpDynamicWeightedScore: BigDecimal?,
  val ogpTotalWeightedScore: BigDecimal?,
  val ogp1Year: BigDecimal?,
  val ogp2Year: BigDecimal?,
  val ogpRisk: String?,
)

data class RiskOfSeriousRecidivismScore @JsonCreator constructor(
  val percentageScore: BigDecimal?,
  val staticOrDynamic: String?,
  val source: String?,
  val algorithmVersion: String?,
  val scoreLevel: String?,
)

data class SexualPredictorScore @JsonCreator constructor(
  val ospIndecentPercentageScore: BigDecimal?,
  val ospContactPercentageScore: BigDecimal?,
  val ospIndecentScoreLevel: String?,
  val ospContactScoreLevel: String?,
  val ospIndirectImagePercentageScore: BigDecimal?,
  val ospDirectContactPercentageScore: BigDecimal?,
  val ospIndirectImageScoreLevel: String?,
  val ospDirectContactScoreLevel: String?,
)
