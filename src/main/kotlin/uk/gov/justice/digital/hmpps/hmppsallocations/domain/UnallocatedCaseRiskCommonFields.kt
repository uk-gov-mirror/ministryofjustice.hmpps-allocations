package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.Registrations
import java.time.LocalDate

data class UnallocatedCaseRegistration @JsonCreator constructor(
  @Schema(description = "Type", example = "Suicide/self-harm")
  val type: String,
  @Schema(description = "Registered date", example = "2020-03-21")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val registered: LocalDate,
  @Schema(description = "Notes", example = "Previous suicide /self-harm attempt. Needs further investigating.")
  val notes: String?,
  @Schema(description = "End Date", example = "2020-01-16")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val endDate: LocalDate?,
  @Schema(description = "Flag", example = "RoSH")
  @JsonFormat(shape = JsonFormat.Shape.OBJECT)
  val flag: Flag,
) {
  companion object {
    fun from(registrations: Registrations): UnallocatedCaseRegistration = UnallocatedCaseRegistration(
      registrations.description,
      registrations.startDate,
      registrations.notes,
      registrations.endDate,
      Flag(registrations.flag.description),
    )
  }
}

data class Flag(val description: String)
