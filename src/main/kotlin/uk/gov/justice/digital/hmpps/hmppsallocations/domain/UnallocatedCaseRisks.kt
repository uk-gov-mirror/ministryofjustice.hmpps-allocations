package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import java.math.BigDecimal
import java.time.LocalDateTime

interface UnallocatedCaseRisks<out T> {
  val name: String
  val crn: String
  val tier: String
  val completedDate: LocalDateTime?
  val riskVersion: String?
  val activeRegistrations: List<UnallocatedCaseRegistration>
  val inactiveRegistrations: List<UnallocatedCaseRegistration>
  val risk: T?
  val convictionNumber: Int?

  fun getROSHLevel(): String?
  fun getRSRLevel(): String?
  fun getOGRSScore(): BigDecimal?
}
