package uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull

@Entity
@Table(name = "saved_emails")
data class SavedEmailsEntity(
  @Id
  @Column
  @GeneratedValue(strategy = IDENTITY)
  val id: Long? = null,

  @Column
  @NotNull
  val userId: String,

  @Column
  @NotNull
  var savedEmail: String,
)
