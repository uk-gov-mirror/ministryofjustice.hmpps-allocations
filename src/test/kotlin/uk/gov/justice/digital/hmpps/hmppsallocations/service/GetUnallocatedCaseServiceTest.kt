package uk.gov.justice.digital.hmpps.hmppsallocations.service

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityPersonManager
import uk.gov.justice.digital.hmpps.hmppsallocations.client.DeliusCaseAccess
import uk.gov.justice.digital.hmpps.hmppsallocations.client.DeliusCaseDetail
import uk.gov.justice.digital.hmpps.hmppsallocations.client.DeliusUserAccess
import uk.gov.justice.digital.hmpps.hmppsallocations.client.Event
import uk.gov.justice.digital.hmpps.hmppsallocations.client.InitialAppointment
import uk.gov.justice.digital.hmpps.hmppsallocations.client.Name
import uk.gov.justice.digital.hmpps.hmppsallocations.client.ProbationStatus
import uk.gov.justice.digital.hmpps.hmppsallocations.client.RecentAllocatedEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.client.RecentManager
import uk.gov.justice.digital.hmpps.hmppsallocations.client.Sentence
import uk.gov.justice.digital.hmpps.hmppsallocations.client.Staff
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusCrnRestrictions
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import java.time.LocalDate

internal class GetUnallocatedCaseServiceTest {

  private val mockWorkforceAllocationsToDeliusApiClientClient: WorkforceAllocationsToDeliusApiClient = mockk()
  private val mockRepo: UnallocatedCasesRepository = mockk()
  private val mockOutOfAreaTransferService: OutOfAreaTransferService = mockk()
  private val mockLaoService: LaoService = mockk()

  @Test
  fun `must not return unallocated cases which get deleted during enrichment`() {
    runBlocking {
      val crn = "X123456"
      val id = 2L
      val unallocatedCaseEntity = UnallocatedCaseEntity(
        name = "case1",
        crn = crn,
        providerCode = "PC1",
        teamCode = "TM1",
        tier = "C",
        provisionalTier = false,
        id = id,
        convictionNumber = 1,
      )
      every { mockRepo.findByTeamCode("TM1") } returns listOf(unallocatedCaseEntity)
      every { mockRepo.existsById(id) } returns false
      coEvery { mockWorkforceAllocationsToDeliusApiClientClient.getUserAccess(listOf(crn)) } returns
        DeliusUserAccess(
          access = listOf(DeliusCaseAccess(crn = crn, userRestricted = false, userExcluded = false)),
        )
      coEvery { mockWorkforceAllocationsToDeliusApiClientClient.getDeliusCaseDetailsCases(listOf(unallocatedCaseEntity)) } returns emptyFlow()
      coEvery { mockLaoService.getCrnRestrictions("userone", crn) } returns
        DeliusCrnRestrictions(false, false, false)
      val cases = GetUnallocatedCaseService(mockRepo, mockOutOfAreaTransferService, mockk(), mockWorkforceAllocationsToDeliusApiClientClient, mockLaoService)
        .getAllByTeam("UsErONe", "TM1").toList()
      assertEquals(0, cases.size)
    }
  }

  @Test
  fun `must return unallocated cases which are restricted and excluded `() = runBlocking {
    val crn = "X123456"
    val unallocatedCaseEntity = UnallocatedCaseEntity(
      name = "restricted",
      crn = crn,
      providerCode = "PC1",
      teamCode = "TM1",
      tier = "C",
      provisionalTier = false,
      id = 2L,
      convictionNumber = 1,
    )

    every { mockRepo.findByTeamCode("TM1") } returns listOf(unallocatedCaseEntity)
    coEvery { mockWorkforceAllocationsToDeliusApiClientClient.getUserAccess(listOf(crn)) } returns
      DeliusUserAccess(
        access = listOf(DeliusCaseAccess(crn = crn, userRestricted = true, userExcluded = true)),
      )

    coEvery { mockLaoService.getCrnRestrictions("UserOne", crn) } returns
      DeliusCrnRestrictions(true, true, true)

    mockServices(crn)

    val cases =
      GetUnallocatedCaseService(mockRepo, mockOutOfAreaTransferService, mockk(), mockWorkforceAllocationsToDeliusApiClientClient, mockLaoService).getAllByTeam("UserOne", "TM1")
        .toList()

    coVerify(exactly = 1) { mockWorkforceAllocationsToDeliusApiClientClient.getDeliusCaseDetailsCases(listOf(unallocatedCaseEntity)) }
    assertEquals(1, cases.size)
  }

  private fun mockServices(crn: String) {
    coEvery { mockWorkforceAllocationsToDeliusApiClientClient.getDeliusCaseDetailsCases(any()) } returns
      flowOf(
        DeliusCaseDetail(
          name = Name(forename = "me", surname = "you", middleName = ""),
          crn = crn,
          sentence = Sentence(date = LocalDate.now(), length = "10"),
          initialAppointment = InitialAppointment(date = LocalDate.now(), staff = Staff(Name(forename = "me", surname = "you", middleName = ""))),
          event = Event("1"),
          probationStatus = ProbationStatus(
            status = "help",
            description = "help",
          ),
          communityPersonManager = CommunityPersonManager(
            name = Name(forename = "me", surname = "you", middleName = ""),
            grade = "SPO",
            teamCode = "TM1",
          ),
          mostRecentAllocatedEvent = RecentAllocatedEvent(
            number = "1",
            manager = RecentManager(
              code = "001",
              name = Name(
                forename = "me",
                surname = "you",
                middleName = "",
              ),
              teamCode = "TM1",
              grade = "SPO",
              allocated = false,
            ),
          ),
          type = "",
          handoverDate = LocalDate.now(),
        ),
      )

    coEvery { mockOutOfAreaTransferService.getCasesThatAreCurrentlyManagedOutsideOfCurrentTeamsRegion(any(), any()) } returns emptyList()
  }

  @Test
  fun `return unallocated cases which are restricted `() = runBlocking {
    val crn = "X123456"
    val unallocatedCaseEntity = UnallocatedCaseEntity(
      name = "restricted",
      crn = crn,
      providerCode = "PC1",
      teamCode = "TM1",
      tier = "C",
      provisionalTier = false,
      id = 2L,
      convictionNumber = 1,
    )

    every { mockRepo.findByTeamCode("TM1") } returns listOf(unallocatedCaseEntity)
    coEvery { mockWorkforceAllocationsToDeliusApiClientClient.getUserAccess(listOf(crn)) } returns
      DeliusUserAccess(
        access = listOf(DeliusCaseAccess(crn = crn, userRestricted = true, userExcluded = false)),
      )
    coEvery { mockLaoService.getCrnRestrictions("UserOne", crn) } returns
      DeliusCrnRestrictions(false, true, true)

    mockServices(crn)

    val cases =
      GetUnallocatedCaseService(mockRepo, mockOutOfAreaTransferService, mockk(), mockWorkforceAllocationsToDeliusApiClientClient, mockLaoService).getAllByTeam("UserOne", "TM1")
        .toList()

    coVerify(exactly = 1) { mockWorkforceAllocationsToDeliusApiClientClient.getDeliusCaseDetailsCases(listOf(unallocatedCaseEntity)) }
    assertEquals(1, cases.size)
  }
}
