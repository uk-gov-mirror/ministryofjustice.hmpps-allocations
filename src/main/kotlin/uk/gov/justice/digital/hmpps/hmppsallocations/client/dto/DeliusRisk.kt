package uk.gov.justice.digital.hmpps.hmppsallocations.client.dto

import org.owasp.html.Sanitizers
import uk.gov.justice.digital.hmpps.hmppsallocations.client.Name
import java.time.LocalDate

data class DeliusRisk constructor(
  val name: Name,
  val crn: String,
  val activeRegistrations: List<Registrations>,
  val inactiveRegistrations: List<Registrations>,
)

data class Registrations constructor(
  var description: String,
  val startDate: LocalDate,
  val endDate: LocalDate?,
  var notes: String?,
  val flag: Flag,
) {
  init {
    description = Sanitizers.FORMATTING.and(Sanitizers.LINKS).sanitize(description)
    notes = Sanitizers.FORMATTING.and(Sanitizers.LINKS).sanitize(notes)
  }
}

data class Flag constructor(val description: String)
