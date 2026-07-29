package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.WorkforceAllocationsToDeliusApiExtension.Companion.workforceAllocationsToDelius

class GetCaseInstructionDetailsByCrnTests : IntegrationTestBase() {

  @Test
  fun `can get case instruction details by crn and convictionNumber`() {
    val crn = "J678910"
    val staffCode = "STAFF1"
    val convictionNumber = 1
    workforceAllocationsToDelius.userHasAccess("J678910")
    insertCases()
    workforceAllocationsToDelius.getImpactResponse(crn, staffCode)

    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber/confirm-instructions?staffCode=$staffCode")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.name.combinedName")
      .isEqualTo("Jonathon Jones")
      .jsonPath("$.crn")
      .isEqualTo("J678910")
      .jsonPath("$.tier")
      .isEqualTo("C")
      .jsonPath("$.provisionalTier")
      .isEqualTo(false)
      .jsonPath("$.convictionNumber")
      .isEqualTo(convictionNumber)
      .jsonPath("$.staff.name.combinedName")
      .isEqualTo("Sheila Hancock")
      .jsonPath("$.staff.email")
      .isEqualTo("Sheila.hancock@email.com")
  }

  @Test
  fun `get 404 if no staff is found`() {
    val crn = "J678910"
    val staffCode = "STAFF1"
    val convictionNumber = 1
    workforceAllocationsToDelius.userHasAccess("J678910")
    insertCases()
    workforceAllocationsToDelius.getImpactNotFoundResponse(crn, staffCode)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber/confirm-instructions?staffCode=$staffCode")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `get 404 if crn not found`() {
    workforceAllocationsToDelius.userHasAccess("J678910")
    webTestClient.get()
      .uri("/cases/unallocated/J678912/convictions/1/confirm-instructions?staffCode=STAFF1")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `get 404 if crn is restricted or excluded`() {
    webTestClient.get()
      .uri("/cases/unallocated/J678912/convictions/1/confirm-instructions?staffCode=STAFF1")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isNotFound
  }
}
