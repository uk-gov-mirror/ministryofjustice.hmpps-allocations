package uk.gov.justice.digital.hmpps.hmppsallocations.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.SavedEmailRequest
import uk.gov.justice.digital.hmpps.hmppsallocations.service.SavedEmailService

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class SavedEmailController(
  private val savedEmailService: SavedEmailService,
) {
  @Operation(summary = "Get saved emails for a User")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "403", description = "Unauthorized"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/user/{userId}/savedEmails")
  suspend fun getSavedEmails(@PathVariable userId: String): List<String> = savedEmailService.getSavedEmails(userId)

  @Operation(summary = "Save email for a User")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "403", description = "Unauthorized"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @PostMapping("/user/savedEmails")
  suspend fun saveEmail(@RequestBody(required = true) savedEmailRequest: SavedEmailRequest) = savedEmailService.saveEmail(savedEmailRequest)

  @Operation(summary = "Delete email for a User")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "403", description = "Unauthorized"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @DeleteMapping("/user/savedEmails")
  suspend fun deleteEmail(@RequestBody(required = true) savedEmailRequest: SavedEmailRequest) = savedEmailService.deleteSavedEmail(savedEmailRequest)
}
