package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsallocations.client.InitialAppointment
import uk.gov.justice.digital.hmpps.hmppsallocations.client.Name
import uk.gov.justice.digital.hmpps.hmppsallocations.client.Staff
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.domain.CaseDetailsIntegration
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.ProbateEstateApiExtension.Companion.hmppsProbateEstate
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.WorkforceAllocationsToDeliusApiExtension.Companion.workforceAllocationsToDelius
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GetUnallocatedCasesByTeamTests : IntegrationTestBase() {

  private fun testUnallocatedCasesByTeamSuccessWithAllDataSetupAndAssertions(
    probationEstateTeamsAndRegionsApiIsWorking: Boolean,
    probationEstateStatusCode: HttpStatus = HttpStatus.OK,
    vararg extraCaseDetailsIntegrations: CaseDetailsIntegration,
  ) {
    workforceAllocationsToDelius.setuserAccessToCases(
      listOf(
        Triple("J678910", false, false),
        Triple("J680648", false, false),
        Triple("X4565764", false, false),
        Triple("J680660", false, false),
        Triple("X6666222", false, false),
        Triple("XXXXXXX", true, false),
        Triple("ZZZZZZZ", false, true),
      ),
    )
    insertCases()
    val initialAppointment = LocalDate.of(2022, 10, 11)
    val firstSentenceDate = LocalDate.of(2022, 11, 5)

    workforceAllocationsToDelius.setupTeam1CaseDetails(*extraCaseDetailsIntegrations)

    workforceAllocationsToDelius.setExcludedUsersByCrn(
      listOf("J678910", "J680660", "X4565764", "J680648", "X6666222", "XXXXXXX", "ZZZZZZZ"),
    )
    workforceAllocationsToDelius.setApopUsers()

    webTestClient.get()
      .uri("/team/TEAM1/cases/unallocated")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isEqualTo(probationEstateStatusCode)
      .expectBody()
      .jsonPath("$.length()")
      .isEqualTo(5)
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].sentenceDate")
      .isEqualTo(firstSentenceDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].initialAppointment.date")
      .isEqualTo(initialAppointment.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].initialAppointment.staff.name.forename")
      .isEqualTo("Beverley")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].initialAppointment.staff.name.middleName")
      .isEqualTo("Rose")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].initialAppointment.staff.name.surname")
      .isEqualTo("Smith")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].initialAppointment.staff.name.combinedName")
      .isEqualTo("Beverley Rose Smith")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].name")
      .isEqualTo("Dylan Adam Armstrong")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].crn")
      .isEqualTo("J678910")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].tier")
      .isEqualTo("C")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].provisionalTier")
      .isEqualTo(false)
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].status")
      .isEqualTo("Currently managed")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].offenderManager.forenames")
      .isEqualTo("Beverley")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].offenderManager.surname")
      .isEqualTo("Smith")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].offenderManager.grade")
      .isEqualTo("SPO")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].caseType")
      .isEqualTo("CUSTODY")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].outOfAreaTransfer")
      .isEqualTo(false)
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].excluded")
      .isEqualTo(false)
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].apopExcluded")
      .isEqualTo(false)
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].handoverDate")
      .isEqualTo(null)
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'X6666222')].sentenceDate")
      .isEqualTo(firstSentenceDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'X6666222')].initialAppointment.staff.name.forename")
      .isEqualTo("Beverley")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'X6666222')].initialAppointment.staff.name.middleName")
      .isEqualTo("Rose")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'X6666222')].initialAppointment.staff.name.surname")
      .isEqualTo("Smith")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'X6666222')].initialAppointment.staff.name.combinedName")
      .isEqualTo("Beverley Rose Smith")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'X6666222')].crn")
      .isEqualTo("X6666222")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'X6666222')].status")
      .isEqualTo("Currently managed")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'X6666222')].offenderManager.forenames")
      .isEqualTo("Joe")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'X6666222')].offenderManager.surname")
      .isEqualTo("Bloggs")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'X6666222')].offenderManager.grade")
      .isEqualTo("SPO")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'X6666222')].outOfAreaTransfer")
      .isEqualTo(probationEstateTeamsAndRegionsApiIsWorking)
      .jsonPath("$.[?(@.convictionNumber == 2 && @.crn == 'J680648')].status")
      .isEqualTo("Previously managed")
      .jsonPath("$.[?(@.convictionNumber == 2 && @.crn == 'J680648')].offenderManager.forenames")
      .isEqualTo("Janie")
      .jsonPath("$.[?(@.convictionNumber == 2 && @.crn == 'J680648')].offenderManager.surname")
      .isEqualTo("Jones")
      .jsonPath("$.[?(@.convictionNumber == 2 && @.crn == 'J680648')].offenderManager.grade")
      .doesNotExist()
      .jsonPath("$.[?(@.convictionNumber == 2 && @.crn == 'J680648')].outOfAreaTransfer")
      .isEqualTo(false)
      .jsonPath("$.[?(@.convictionNumber == 4 && @.crn == 'J680660')].offenderManager")
      .doesNotExist()
      .jsonPath("$.[?(@.convictionNumber == 4 && @.crn == 'J680660')].status")
      .isEqualTo("Previously managed")
      .jsonPath("$.[?(@.convictionNumber == 4 && @.crn == 'J680660')].outOfAreaTransfer")
      .isEqualTo(false)
      .jsonPath("$.[?(@.convictionNumber == 3 && @.crn == 'X4565764')].status")
      .isEqualTo("New to probation")
      .jsonPath("$.[?(@.convictionNumber == 3 && @.crn == 'X4565764')].offenderManager")
      .doesNotExist()
      .jsonPath("$.[?(@.convictionNumber == 3 && @.crn == 'X4565764')].outOfAreaTransfer")
      .isEqualTo(false)
  }

  private fun testUnallocatedCasesByTeamSuccessWithAllDataSetupAndRestrictedUsers(
    probationEstateTeamsAndRegionsApiIsWorking: Boolean,
    vararg extraCaseDetailsIntegrations: CaseDetailsIntegration,
  ) {
    workforceAllocationsToDelius.setuserAccessToCases(
      listOf(
        Triple("J678910", true, false),
        Triple("J680648", false, true),
        Triple("X4565764", false, true),
        Triple("J680660", true, true),
        Triple("X6666222", true, true),
        Triple("XXXXXXX", true, true),
        Triple("ZZZZZZZ", true, true),
      ),
    )
    insertCases()

    val initialAppointment = LocalDate.of(2022, 10, 11)
    val firstSentenceDate = LocalDate.of(2022, 11, 5)

    workforceAllocationsToDelius.setupTeam1CaseDetails(*extraCaseDetailsIntegrations)

    // NOt excluded
    workforceAllocationsToDelius.setRestrictedUsersByCrn("J678910", "SOmeoneelse", "someoneelse")
    workforceAllocationsToDelius.setRestrictedUsersByCrn("J680660", "SOmeoneelse", "someoneelse")

    // excluded user
    workforceAllocationsToDelius.setExcludedUsersByCrn("J680648")
    // excluded APoP User
    workforceAllocationsToDelius.setExcludedUsersByCrn("X4565764", "test3", "TomjonEs")
    workforceAllocationsToDelius.setRestrictedUsersByCrn("X6666222", "SOmeoneelse", "someoneelse")

    workforceAllocationsToDelius.setApopUsers()

    webTestClient.get()
      .uri("/team/TEAM1/cases/unallocated")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.length()")
      .isEqualTo(5)
      .jsonPath("$.[?(@.convictionNumber == 2 && @.crn == 'J680648')].status")
      .isEqualTo("Previously managed")
      .jsonPath("$.[?(@.convictionNumber == 2 && @.crn == 'J680648')].offenderManager.forenames")
      .isEqualTo("Janie")
      .jsonPath("$.[?(@.convictionNumber == 2 && @.crn == 'J680648')].offenderManager.surname")
      .isEqualTo("Jones")
      .jsonPath("$.[?(@.convictionNumber == 2 && @.crn == 'J680648')].offenderManager.grade")
      .doesNotExist()
      .jsonPath("$.[?(@.convictionNumber == 2 && @.crn == 'J680648')].outOfAreaTransfer")
      .isEqualTo(false)
      .jsonPath("$.[?(@.convictionNumber == 2 && @.crn == 'J680648')].excluded")
      .isEqualTo(true)
      .jsonPath("$.[?(@.convictionNumber == 2 && @.crn == 'J680648')].apopExcluded")
      .isEqualTo(false) // ** has excluded users but not  Apop users
      .jsonPath("$.[?(@.convictionNumber == 3 && @.crn == 'X4565764')].status")
      .isEqualTo("New to probation")
      .jsonPath("$.[?(@.convictionNumber == 3 && @.crn == 'X4565764')].offenderManager")
      .doesNotExist()
      .jsonPath("$.[?(@.convictionNumber == 3 && @.crn == 'X4565764')].outOfAreaTransfer")
      .isEqualTo(false)
      .jsonPath("$.[?(@.convictionNumber == 3 && @.crn == 'X4565764')].excluded")
      .isEqualTo(true)
      .jsonPath("$.[?(@.convictionNumber == 3 && @.crn == 'X4565764')].apopExcluded")
      .isEqualTo(true) // ** has excluded users that are  Apop users
      .jsonPath("$.[?(@.crn == 'J678910')].apopExcluded")
      .isEqualTo(true) // ** restricted case
      .jsonPath("$.[?(@.crn == 'J680660')].apopExcluded")
      .isEqualTo(true)
      .jsonPath("$.[?(@.crn == 'X6666222')].apopExcluded")
      .isEqualTo(true)
  }

  @Test
  fun `Get unallocated cases by team where probation-estate API is successful and does not return restricted cases`() {
    hmppsProbateEstate.regionsAndTeamsSuccessResponse(
      teams = listOf(
        "TEAM1" to "Team 1",
        "TEAM2" to "Team 2",
      ),
      regions = listOf(
        "REGION1" to "Region 1",
        "REGION2" to "Region 2",
      ),
    )
    testUnallocatedCasesByTeamSuccessWithAllDataSetupAndAssertions(
      probationEstateTeamsAndRegionsApiIsWorking = true,
    )
  }

  @Test
  fun `Get unallocated cases by team where probation-estate API is successful and cases have restrictions`() {
    hmppsProbateEstate.regionsAndTeamsSuccessResponse(
      teams = listOf(
        "TEAM1" to "Team 1",
        "TEAM2" to "Team 2",
      ),
      regions = listOf(
        "REGION1" to "Region 1",
        "REGION2" to "Region 2",
      ),
    )
    testUnallocatedCasesByTeamSuccessWithAllDataSetupAndRestrictedUsers(
      probationEstateTeamsAndRegionsApiIsWorking = true,
    )
  }

  @Test
  fun `Get unallocated cases by team where all cases are LAO restricted cases`() {
    insertCases()
    workforceAllocationsToDelius.setuserAccessToCases(
      listOf(
        Triple("J678910", true, true),
        Triple("J680648", true, true),
        Triple("X4565764", true, true),
        Triple("J680660", true, true),
        Triple("X6666222", true, true),
        Triple("XXXXXXX", true, true),
        Triple("ZZZZZZZ", true, true),
      ),
    )
    workforceAllocationsToDelius.setExcludedUsersByCrn("J678910", "test3", "tomJones")
    workforceAllocationsToDelius.setRestrictedUsersByCrn("J680648", "SOmeoneelse", "someoneelse")
    workforceAllocationsToDelius.setExcludedUsersByCrn("X4565764", "test3", "TomJones")
    workforceAllocationsToDelius.setRestrictedUsersByCrn("J680660", "SOmeoneelse", "someoneelse")
    workforceAllocationsToDelius.setExcludedUsersByCrn("X6666222", "test3", "TomJoNes")
    workforceAllocationsToDelius.setRestrictedUsersByCrn("XXXXXXX", "SOmeoneelse", "someoneelse")
    workforceAllocationsToDelius.setExcludedAndRestrictedUsersCrn("ZZZZZZZ", "test3", "tomjones")

    workforceAllocationsToDelius.setupTeam1CaseDetails()

    webTestClient.get()
      .uri("/team/TEAM1/cases/unallocated")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.length()")
      .isEqualTo(5)
  }

  @Test
  fun `Get unallocated cases by team where 0 cases in database for team`() {
    webTestClient.get()
      .uri("/team/TEAM1/cases/unallocated")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.length()")
      .isEqualTo(0)
  }

  @Test
  fun `Get unallocated cases by team where Delius gives back more than we expect and we filter out the extras`() {
    val extraUnexpectedCaseFromDelius = CaseDetailsIntegration(
      crn = "AAAAAAA",
      eventNumber = "1",
      initialAppointment = InitialAppointment(LocalDate.now(), Staff(Name("Beverley", "Rose", "Smith"))),
      probationStatus = "NEW_TO_PROBATION",
      probationStatusDescription = "New to probation",
      communityPersonManager = null,
      mostRecentAllocatedEvent = null,
      handoverDate = null,
    )
    hmppsProbateEstate.regionsAndTeamsSuccessResponse(
      teams = listOf(
        "TEAM1" to "Team 1",
        "TEAM2" to "Team 2",
      ),
      regions = listOf(
        "REGION1" to "Region 1",
        "REGION2" to "Region 2",
      ),
    )
    workforceAllocationsToDelius.setExcludedUsersByCrn(
      listOf("J678910", "J680660", "X4565764", "J680648", "X6666222", "XXXXXXX", "ZZZZZZZ"),
    )
    workforceAllocationsToDelius.setApopUsers()

    testUnallocatedCasesByTeamSuccessWithAllDataSetupAndAssertions(
      probationEstateTeamsAndRegionsApiIsWorking = true,
      probationEstateStatusCode = HttpStatus.OK,
      extraUnexpectedCaseFromDelius,
    )
  }

  @Test
  fun `Get unallocated cases by team where probation-estate API is failing with InternalServerError response`() {
    hmppsProbateEstate.regionsAndTeamsFailsWithInternalServerErrorResponse()
    testUnallocatedCasesByTeamSuccessWithAllDataSetupAndAssertions(
      probationEstateTeamsAndRegionsApiIsWorking = false,
      probationEstateStatusCode = HttpStatus.OK,
    )
  }

  @Test
  fun `Get unallocated cases by team where probation-estate API is failing with BadRequest response`() {
    hmppsProbateEstate.regionsAndTeamsFailsWithBadRequestResponse()
    testUnallocatedCasesByTeamSuccessWithAllDataSetupAndAssertions(
      probationEstateTeamsAndRegionsApiIsWorking = false,
    )
  }

  @Test
  fun `return error when error on Delius API call`() {
    workforceAllocationsToDelius.setuserAccessToCases(
      listOf(
        Triple("J678910", false, false),
        Triple("J680648", false, false),
        Triple("X4565764", false, false),
        Triple("J680660", false, false),
        Triple("X6666222", false, false),
        Triple("XXXXXXX", true, false),
        Triple("ZZZZZZZ", false, true),
      ),
    )
    insertCases()

    workforceAllocationsToDelius.errorDeliusCaseDetailsResponse()

    webTestClient.get()
      .uri("/team/TEAM1/cases/unallocated")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.FAILED_DEPENDENCY)
  }

  @Test
  fun `cannot get unallocated cases by team when no auth token supplied`() {
    webTestClient.get()
      .uri("/team/TEAM1/cases/unallocated")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `not found`() {
    webTestClient.post()
      .uri("/cases/someotherurl")
      .headers { it.authToken(roles = listOf("ROLE_QUEUE_WORKLOAD_ADMIN")) }
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.NOT_FOUND)
  }

  @Test
  fun `method not allowed`() {
    webTestClient.post()
      .uri("/team/TEAM1/cases/unallocated")
      .headers { it.authToken(roles = listOf("ROLE_QUEUE_WORKLOAD_ADMIN")) }
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
  }

  @Test
  fun `cannot get unallocated cases when no auth token supplied`() {
    webTestClient.get()
      .uri("/team/TEAM1/cases/unallocated")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `must return sentence length`() {
    workforceAllocationsToDelius.setuserAccessToCases(
      listOf(
        Triple("J678910", false, false),
        Triple("J680648", false, false),
        Triple("X4565764", false, false),
        Triple("J680660", false, false),
        Triple("X6666222", false, false),
        Triple("XXXXXXX", true, false),
        Triple("ZZZZZZZ", false, true),
      ),
    )

    workforceAllocationsToDelius.setupTeam1CaseDetails()
    workforceAllocationsToDelius.setExcludedUsersByCrn(
      listOf("J678910", "J680660", "X4565764", "J680648", "X6666222", "XXXXXXX", "ZZZZZZZ"),
    )
    workforceAllocationsToDelius.setApopUsers()

    hmppsProbateEstate.regionsAndTeamsSuccessResponse(
      teams = listOf(
        "TEAM1" to "Team 1",
        "TEAM2" to "Team 2",
      ),
      regions = listOf(
        "REGION1" to "Region 1",
        "REGION2" to "Region 2",
      ),
    )

    insertCases()
    webTestClient.get()
      .uri("/team/TEAM1/cases/unallocated")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].sentenceLength")
      .isEqualTo("5 Weeks")
  }

  @Test
  fun `Get unallocated cases which includes a custody-to-community case with a handover-date`() {
    val handoverDate = "2024-12-01"
    insertCases()

    // insert extra C2C case to DB
    val c2cCaseCrn = "AB77711"
    insertCase(
      unallocatedCase = UnallocatedCaseEntity(
        id = null,
        name = "Donald Duck",
        crn = c2cCaseCrn,
        tier = "A",
        provisionalTier = false,
        providerCode = "",
        teamCode = "TEAM1",
        convictionNumber = 2,
      ),
    )

    // mock Delius allocation-demand response (inc. c2c case) + LAO response (i.e. user access stuff)
    val c2cCase = CaseDetailsIntegration(
      crn = c2cCaseCrn,
      eventNumber = "2",
      initialAppointment = null,
      probationStatus = "PREVIOUSLY_MANAGED",
      probationStatusDescription = "Previously managed",
      communityPersonManager = null,
      mostRecentAllocatedEvent = null,
      handoverDate = handoverDate,
    )
    workforceAllocationsToDelius.setupTeam1CaseDetails(c2cCase)
    workforceAllocationsToDelius.setuserAccessToCases(
      listOf(
        Triple("J678910", false, false),
        Triple("J680648", false, false),
        Triple("X4565764", false, false),
        Triple("J680660", false, false),
        Triple("X6666222", false, false),
        Triple("XXXXXXX", true, false),
        Triple("ZZZZZZZ", false, true),
        Triple(c2cCaseCrn, false, false),
      ),
    )

    workforceAllocationsToDelius.setExcludedUsersByCrn(
      listOf("J678910", "J680660", "X4565764", "J680648", "X6666222", "XXXXXXX", "ZZZZZZZ", "AB77711"),
    )
    workforceAllocationsToDelius.setApopUsers()

    // mock estate api call for out of areas related functionality
    hmppsProbateEstate.regionsAndTeamsSuccessResponse(
      teams = listOf(
        "TEAM1" to "Team 1",
        "TEAM2" to "Team 2",
      ),
      regions = listOf(
        "REGION1" to "Region 1",
        "REGION2" to "Region 2",
      ),
    )

    workforceAllocationsToDelius.setExcludedUsersByCrn(
      listOf("J678910", "J680660", "X4565764", "J680648", "X6666222", "XXXXXXX", "ZZZZZZZ", "AB77711"),
    )
    workforceAllocationsToDelius.setApopUsers()

    webTestClient.get()
      .uri("/team/TEAM1/cases/unallocated")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.[?(@.convictionNumber == 2 && @.crn == 'AB77711')].handoverDate")
      .isEqualTo(handoverDate)
  }

  @Test
  fun `Propogate Bad request when invalid body passed`() {
    webTestClient.post()
      .uri("/cases/restrictions/crn/list")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue("{ \"staffCodes\": [ \"invalid\" ] }"))
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.BAD_REQUEST)
  }

  @Test
  fun `Propogate Bad request when no body passed`() {
    webTestClient.post()
      .uri("/cases/restrictions/crn/list")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .contentType(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.BAD_REQUEST)
  }
}
