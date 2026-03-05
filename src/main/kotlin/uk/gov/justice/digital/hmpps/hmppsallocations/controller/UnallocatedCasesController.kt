package uk.gov.justice.digital.hmpps.hmppsallocations.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.hmppsallocations.client.DeliusUserAccess
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.CrnListRequest
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.CrnStaffRestrictions
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusCrnRestrictionStatus
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.StaffCodesRequest
import uk.gov.justice.digital.hmpps.hmppsallocations.config.Principal
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseCountByTeam
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseOverview
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseConfirmInstructions
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseConvictions
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseRisks
import uk.gov.justice.digital.hmpps.hmppsallocations.service.GetUnallocatedCaseService
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.EntityNotFoundException

private const val UNALLOCATED_CASE_NOT_FOUND_FOR = "Unallocated case Not Found for"

@Suppress("StringLiteralDuplication")
@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class UnallocatedCasesController(
  private val getUnallocatedCaseService: GetUnallocatedCaseService,
) {
  @Operation(summary = "Retrieve count of all unallocated cases by team")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/unallocated/teamCount")
  fun getCaseCountByTeam(@RequestParam(required = true) teams: List<String>): Flux<CaseCountByTeam> = getUnallocatedCaseService.getCaseCountByTeam(teams)

  @Operation(summary = "Retrieve unallocated case by crn and conviction id")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "403", description = "Unauthorized"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/unallocated/{crn}/convictions/{convictionNumber}")
  suspend fun getUnallocatedCase(
    @AuthenticationPrincipal principal: Principal,
    @PathVariable(required = true) crn: String,
    @PathVariable(required = true) convictionNumber: Long,
  ): UnallocatedCaseDetails = getUnallocatedCaseService.getCase(principal.userName, crn, convictionNumber)
    ?: throw EntityNotFoundException("$UNALLOCATED_CASE_NOT_FOUND_FOR $crn")

  @Operation(summary = "Retrieve unallocated case overview by crn and conviction id")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/unallocated/{crn}/convictions/{convictionNumber}/overview")
  suspend fun getUnallocatedCaseOverview(
    @PathVariable(required = true) crn: String,
    @PathVariable(required = true) convictionNumber: Long,
  ): CaseOverview = getUnallocatedCaseService.getCaseOverview(crn, convictionNumber)
    ?: throw EntityNotFoundException("$UNALLOCATED_CASE_NOT_FOUND_FOR $crn")

  @Operation(summary = "Retrieve probation record")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/unallocated/{crn}/record/exclude-conviction/{excludeConvictionNumber}")
  suspend fun getUnallocatedCaseProbationRecord(
    @PathVariable(required = true) crn: String,
    @PathVariable(required = true) excludeConvictionNumber: Long,
  ): UnallocatedCaseConvictions = getUnallocatedCaseService.getCaseConvictions(crn, excludeConvictionNumber)
    ?: throw EntityNotFoundException("$UNALLOCATED_CASE_NOT_FOUND_FOR $crn")

  @Operation(summary = "Retrieve unallocated case risks by crn")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/unallocated/{crn}/convictions/{convictionNumber}/risks")
  suspend fun getUnallocatedCaseRisks(
    @PathVariable(required = true) crn: String,
    @PathVariable(required = true) convictionNumber: Long,
  ): UnallocatedCaseRisks<Any> = getUnallocatedCaseService.getCaseRisks(crn, convictionNumber)
    ?: throw EntityNotFoundException("Unallocated case risks Not Found for $crn")

  @Operation(summary = "Retrieve unallocated case confirm instructions by crn")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/unallocated/{crn}/convictions/{convictionNumber}/confirm-instructions")
  suspend fun getUnallocatedCaseConfirmInstructions(
    @PathVariable(required = true) crn: String,
    @PathVariable(required = true) convictionNumber: Long,
    @RequestParam(required = true) staffCode: String,
  ): UnallocatedCaseConfirmInstructions = getUnallocatedCaseService.getCaseConfirmInstructions(crn, convictionNumber, staffCode)
    ?: throw EntityNotFoundException("$UNALLOCATED_CASE_NOT_FOUND_FOR $crn and conviction $convictionNumber")

  @Operation(summary = "Retrieve staff restrictions for given list and crn")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @PostMapping("/cases/unallocated/{crn}/restrictions")
  suspend fun getCaseRestrictionsByStaffCodes(
    @PathVariable(required = true) crn: String,
    @RequestBody(required = true) staffCodesRequest: StaffCodesRequest,
  ): CrnStaffRestrictions = getUnallocatedCaseService.getCrnStaffRestrictions(crn, staffCodesRequest.staffCodes)
    ?: throw EntityNotFoundException("Unallocated case Not Found for $crn")

  @Operation(summary = "Retrieve is crn restricted for logged on user")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "403", description = "Unauthorized"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/unallocated/{crn}/restricted")
  suspend fun isCaseRestricted(
    @AuthenticationPrincipal principal: Principal,
    @PathVariable(required = true) crn: String,
  ): Boolean = getUnallocatedCaseService.isCrnRestricted(principal.userName, crn)
    ?: throw EntityNotFoundException("Unallocated case Not Found for $crn")

  @Operation(summary = "Retrieve case restrictions for given crn")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "403", description = "Unauthorized"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/{crn}/restrictions")
  suspend fun getCaseRestrictions(
    @AuthenticationPrincipal principal: Principal,
    @PathVariable(required = true) crn: String,
  ): DeliusCrnRestrictionStatus = getUnallocatedCaseService.getCaseRestrictions(principal.userName, crn)
    ?: throw EntityNotFoundException("Unallocated case Not Found for $crn")

  @Operation(summary = "Retrieve case restrictions for given list of crns")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "403", description = "Unauthorized"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @PostMapping("/cases/restrictions/crn/list")
  suspend fun getCaseRestrictions(
    @AuthenticationPrincipal principal: Principal,
    @RequestBody(required = true) crnListRequest: CrnListRequest,
  ): DeliusUserAccess = getUnallocatedCaseService.getCaseRestrictions(principal.userName, crnListRequest.crns)
}
