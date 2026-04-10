package uk.gov.justice.digital.hmpps.hmppsallocations.client.dto

data class SavedEmailRequest constructor(
  val userId: String,
  val email: String,
)
