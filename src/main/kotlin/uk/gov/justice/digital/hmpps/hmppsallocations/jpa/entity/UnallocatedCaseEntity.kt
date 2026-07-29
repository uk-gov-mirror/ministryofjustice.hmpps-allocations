package uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@Entity
@Table(name = "unallocated_cases")
data class UnallocatedCaseEntity(
  @Id
  @Column
  @GeneratedValue(strategy = IDENTITY)
  val id: Long? = null,

  @Column
  var name: String,

  @Column
  @NotNull
  val crn: String,

  @Column
  var tier: String,

  @Column
  var provisionalTier: Boolean,

  @Column
  var teamCode: String,

  @Column
  var providerCode: String,

  @Column
  val createdDate: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),

  @Column
  var convictionNumber: Int,
)
