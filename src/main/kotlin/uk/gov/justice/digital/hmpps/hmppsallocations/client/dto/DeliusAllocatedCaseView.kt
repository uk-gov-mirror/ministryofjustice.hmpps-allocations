package uk.gov.justice.digital.hmpps.hmppsallocations.client.dto

import com.fasterxml.jackson.annotation.JsonCreator
import uk.gov.justice.digital.hmpps.hmppsallocations.client.Name
import java.time.LocalDate

data class DeliusAllocatedCaseView(
  val name: Name,
  val dateOfBirth: LocalDate,
  val gender: String,
  val pncNumber: String?,
  val mainAddress: MainAddressDto?,
  val nextAppointmentDate: LocalDate?,
  val activeEvents: List<AllocatedActiveEvent>,
)

data class MainAddressDto constructor(
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
)

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
