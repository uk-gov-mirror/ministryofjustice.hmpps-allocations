package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsallocations.client.Name
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.PersonOnProbationStaffDetailsResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.StaffMember
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity

data class UnallocatedCaseConfirmInstructions @JsonCreator constructor(
  val name: Name,
  @Schema(description = "CRN", example = "J111111")
  val crn: String,
  @Schema(description = "Latest tier of case", example = "D")
  val tier: String,
  val provisionalTier: Boolean,
  val convictionNumber: Int,
  val staff: StaffMember,
) {

  companion object {
    fun from(
      case: UnallocatedCaseEntity,
      personOnProbationStaffDetailsResponse: PersonOnProbationStaffDetailsResponse,
    ): UnallocatedCaseConfirmInstructions = UnallocatedCaseConfirmInstructions(
      personOnProbationStaffDetailsResponse.name,
      case.crn,
      case.tier,
      case.provisionalTier,
      case.convictionNumber,
      personOnProbationStaffDetailsResponse.staff,
    )
  }
}
