package uk.gov.justice.digital.hmpps.hmppsallocations.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsallocations.config.Principal
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.AllocatedCaseDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.AssessmentDate
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseConvictions
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseRisks
import uk.gov.justice.digital.hmpps.hmppsallocations.service.GetAllocatedCaseService
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.EntityNotFoundException

private const val CASE_NOT_FOUND_FOR = "Case not found for "

@Suppress("StringLiteralDuplication")
@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class ReallocationCasesController(private val getAllocatedCaseService: GetAllocatedCaseService) {

  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/allocated/{crn}")
  suspend fun getAllocatedCase(
    @AuthenticationPrincipal principal: Principal,
    @PathVariable(required = true) crn: String,
  ): AllocatedCaseDetails = getAllocatedCaseService.getCase(principal.userName, crn)
    ?: throw EntityNotFoundException("$CASE_NOT_FOUND_FOR $crn")

  @Operation(summary = "Retrieve case risks by crn")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/allocated/{crn}/record/exclude-conviction/{excludeConvictionNumber}")
  suspend fun getCaseProbationRecord(
    @PathVariable(required = true) crn: String,
    @PathVariable(required = true) excludeConvictionNumber: Long,
  ): UnallocatedCaseConvictions = getAllocatedCaseService.getAllocatedCaseConvictions(crn, excludeConvictionNumber)
    ?: throw EntityNotFoundException("$CASE_NOT_FOUND_FOR $crn")

  @Operation(summary = "Retrieve case risks by crn")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/allocated/{crn}/risks")
  suspend fun getCaseRisks(
    @PathVariable(required = true) crn: String,
  ): UnallocatedCaseRisks<Any> = getAllocatedCaseService.getCaseRisks(crn)
    ?: throw EntityNotFoundException("Case risks Not Found for $crn")

  @Operation(summary = "Retrieve assessment date by crn")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/{crn}/assessmentDate")
  suspend fun getCaseAssessment(
    @PathVariable(required = true) crn: String,
  ): AssessmentDate = getAllocatedCaseService.getCaseAssessmentDate(crn)
}
