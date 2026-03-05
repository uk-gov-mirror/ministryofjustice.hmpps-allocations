package uk.gov.justice.digital.hmpps.hmppsallocations.domain
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.math.BigDecimal
import java.time.LocalDateTime

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.EXISTING_PROPERTY,
  property = "outputVersion",
)
@JsonSubTypes(
  JsonSubTypes.Type(value = RiskPredictorV1::class, name = "1"),
  JsonSubTypes.Type(value = RiskPredictorV2::class, name = "2"),
)
sealed interface RiskPredictor<out T> {
  val completedDate: LocalDateTime?
  val source: String?
  val status: String?
  val outputVersion: String
  val output: T?

  fun getRSRScoreLevel(): String?
  fun getRSRPercentageScore(): BigDecimal?
  fun getOGRSScoreLevel(): String?
  fun getOGRSPercentageScore(): BigDecimal?
}
