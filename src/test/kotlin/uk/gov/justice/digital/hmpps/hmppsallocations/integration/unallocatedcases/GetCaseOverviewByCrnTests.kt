package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.WorkforceAllocationsToDeliusApiExtension

class GetCaseOverviewByCrnTests : IntegrationTestBase() {

  @Test
  fun `can get case overview by crn and convictionNumber`() {
    val crn = "J678910"
    val convictionNumber = 1
    WorkforceAllocationsToDeliusApiExtension.workforceAllocationsToDelius.userHasAccess("J678910")
    insertCases()

    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber/overview")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.name")
      .isEqualTo("Dylan Adam Armstrong")
      .jsonPath("$.crn")
      .isEqualTo("J678910")
      .jsonPath("$.tier")
      .isEqualTo("C")
      .jsonPath("$.provisionalTier")
      .isEqualTo(false)
      .jsonPath("$.convictionNumber")
      .isEqualTo(convictionNumber)
  }

  @Test
  fun `get 404 if crn not found`() {
    WorkforceAllocationsToDeliusApiExtension.workforceAllocationsToDelius.userHasAccess("J678912")
    webTestClient.get()
      .uri("/cases/unallocated/J678912/convictions/1/overview")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Disabled
  @Test
  fun `get 404 if crn is restricted `() {
    val crn = "J678910"
    val convictionNumber = 1
    WorkforceAllocationsToDeliusApiExtension.workforceAllocationsToDelius.userHasAccess("J678910", true, false)
    insertCases()
    webTestClient.get()
      .uri("/cases/unallocated/J678910/convictions/1/overview")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Disabled
  @Test
  fun `get 404 if crn is restricted and excluded`() {
    val crn = "J678910"
    val convictionNumber = 1
    WorkforceAllocationsToDeliusApiExtension.workforceAllocationsToDelius.userHasAccess("J678910", true, true)
    insertCases()
    webTestClient.get()
      .uri("/cases/unallocated/J678910/convictions/1/overview")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `get 200 if crn is not restricted or excluded`() {
    val crn = "J678910"
    val convictionNumber = 1
    WorkforceAllocationsToDeliusApiExtension.workforceAllocationsToDelius.userHasAccess("J678910", false, false)
    insertCases()
    webTestClient.get()
      .uri("/cases/unallocated/J678910/convictions/1/overview")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
  }

  @Test
  fun `get 200 if crn is excluded`() {
    val crn = "J678910"
    val convictionNumber = 1
    WorkforceAllocationsToDeliusApiExtension.workforceAllocationsToDelius.userHasAccess("J678910", false, true)
    insertCases()
    webTestClient.get()
      .uri("/cases/unallocated/J678910/convictions/1/overview")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
  }
}
