package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.AllocatedActiveEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusAllocatedCaseView
import java.time.LocalDate
import java.time.Period

data class AllocatedCaseDetails @JsonCreator constructor(
  @Schema(description = "Offender Name", example = "John Smith")
  val name: String,
  @Schema(description = "CRN", example = "J111111")
  val crn: String,
  @Schema(description = "PNC Number")
  val pncNumber: String?,
  @Schema(description = "Gender", example = "Male")
  val gender: String?,
  @Schema(description = "Date Of Birth", example = "2021-06-19")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = STRING)
  val dateOfBirth: LocalDate?,
  @Schema(description = "Age", example = "34")
  val age: Int,
  @Schema(description = "Latest tier of case", example = "D")
  val tier: String,
  val provisionalTier: Boolean,
  val address: MainAddress?,
  @Schema(description = "Next Appointment Date", example = "2021-06-19")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = STRING)
  val nextAppointmentDate: LocalDate?,
  val activeEvents: List<AllocatedActiveEvent>,
  val outOfAreaTransfer: Boolean,
) {
  companion object {
    @Suppress("LongParameterList")
    fun from(
      deliusCaseView: DeliusAllocatedCaseView,
      outOfAreaTransfer: Boolean,
      tier: String,
      provisionalTier: Boolean,
      crn: String,
    ): AllocatedCaseDetails = AllocatedCaseDetails(
      deliusCaseView.name.getCombinedName(),
      crn,
      deliusCaseView.pncNumber,
      deliusCaseView.gender, deliusCaseView.dateOfBirth,
      calculateAge(deliusCaseView.dateOfBirth),
      tier,
      provisionalTier,
      deliusCaseView.mainAddress?.let { MainAddress.from(it) },
      deliusCaseView.nextAppointmentDate,
      deliusCaseView.activeEvents,
      outOfAreaTransfer,
    )
  }
}

data class AllocatedActiveEvent @JsonCreator constructor(
  val number: Int,
  val failureToComplyCount: Int,
  val failureToComplyStartDate: LocalDate?,
  val sentence: AllocatedEventSentence?,
  val offences: List<AllocatedEventOffences>,
  val requirements: List<AllocatedEventRequirement>,
)

data class AllocatedEventRequirement @JsonCreator constructor(
  val mainCategory: String,
  val subCategory: String?,
  val length: String,
)

data class AllocatedEventOffences @JsonCreator constructor(
  val mainCategory: String,
  val subCategory: String,
  val mainOffence: Boolean,
)

data class AllocatedEventSentence @JsonCreator constructor(
  val description: String,
  val startDate: LocalDate,
  val endDate: LocalDate,
  val length: String,
)

data class MainAddress constructor(
  val buildingName: String?,
  val addressNumber: String?,
  val streetName: String?,
  val town: String?,
  val county: String?,
  val postcode: String?,
  val noFixedAbode: Boolean?,
  val typeVerified: Boolean?,
  val typeDescription: String?,
  val startDate: LocalDate?,
) {
  companion object {
    fun from(address: uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.MainAddressDto): MainAddress = MainAddress(
      address.buildingName ?: "",
      address.addressNumber ?: "",
      address.streetName ?: "",
      address.town ?: "",
      address.county ?: "",
      address.postcode ?: "",
      address.noFixedAbode,
      address.typeVerified,
      address.typeDescription ?: "",
      address.startDate,
    )
  }
}

fun calculateAge(dateOfBirth: LocalDate?): Int {
  if (dateOfBirth == null) return 0
  return Period.between(dateOfBirth, LocalDate.now()).years
}
