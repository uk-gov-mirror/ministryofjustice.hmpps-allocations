package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsallocations.client.DeliusCaseDetail
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.time.LocalDate

data class UnallocatedCase @JsonCreator constructor(
  @Schema(description = "Offender Name", example = "John Smith")
  val name: String,
  @Schema(description = "CRN", example = "J111111")
  val crn: String,
  @Schema(description = "Latest tier of case", example = "D")
  val tier: String,
  val provisionalTier: Boolean,
  @Schema(description = "Sentence Date", example = "2020-01-16")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val sentenceDate: LocalDate,
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val handoverDate: LocalDate?,
  @JsonInclude(JsonInclude.Include.NON_NULL)
  val initialAppointment: InitialAppointmentDetails?,
  @Schema(description = "Probation Status", example = "Currently managed")
  val status: String,
  @JsonInclude(JsonInclude.Include.NON_NULL)
  val offenderManager: OffenderManagerDetails?,
  @Schema(description = "Case Type")
  val caseType: String,
  @Schema(description = "Sentence Length")
  val sentenceLength: String?,
  val convictionNumber: Int,
  val outOfAreaTransfer: Boolean,
  val excluded: Boolean,
  val apopExcluded: Boolean,
) {
  companion object {
    @Suppress("LongParameterList")
    fun from(
      case: UnallocatedCaseEntity,
      deliusCaseDetail: DeliusCaseDetail,
      outOfAreaTransfer: Boolean,
      excluded: Boolean,
      apopExcluded: Boolean,
    ): UnallocatedCase = UnallocatedCase(
      name = "${deliusCaseDetail.name.forename} ${deliusCaseDetail.name.surname}",
      crn = deliusCaseDetail.crn,
      tier = case.tier,
      provisionalTier = case.provisionalTier,
      sentenceDate = deliusCaseDetail.sentence.date,
      initialAppointment = InitialAppointmentDetails.from(
        deliusCaseDetail.initialAppointment,
      ),
      status = deliusCaseDetail.probationStatus.description,
      offenderManager = OffenderManagerDetails.from(
        deliusCaseDetail.mostRecentAllocatedEvent,
        deliusCaseDetail.probationStatus.description,
      ),
      caseType = deliusCaseDetail.type,
      sentenceLength = deliusCaseDetail.sentence.length,
      convictionNumber = case.convictionNumber,
      outOfAreaTransfer = outOfAreaTransfer,
      handoverDate = deliusCaseDetail.handoverDate,
      excluded = excluded,
      apopExcluded = apopExcluded,
    )
  }
}
