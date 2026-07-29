package uk.gov.justice.digital.hmpps.hmppsallocations.service

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.client.DeliusCaseAccess
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.Name
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.ActiveEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.TierWithStatus
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.UnallocatedEvents
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import java.time.ZonedDateTime

class UpsertUnallocatedCaseServiceTest {

  @MockK
  lateinit var repository: UnallocatedCasesRepository

  @MockK
  lateinit var dataBaseOperationService: UnallocatedDataBaseOperationService

  @MockK
  lateinit var hmppsTierApiClient: HmppsTierApiClient

  @MockK
  lateinit var workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient

  @InjectMockKs
  lateinit var cut: UpsertUnallocatedCaseService

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
  }

  @Test
  fun `upsertUnallocatedCase non lao case`() = runTest {
    val crn = "X1234567"
    val name = "Bob Jones"
    val teamCode = "N54ERT"
    val providerCode = "PC001"
    val tier = TierWithStatus("C", false)
    val unallocatedCaseEntity = UnallocatedCaseEntity(1L, name, crn, tier.tierScore, tier.provisional, teamCode, providerCode, ZonedDateTime.now(), 1)
    val deliusCaseAccess = DeliusCaseAccess(crn, false, false)
    val activeEvent = ActiveEvent("1", teamCode, providerCode)
    val unallocatedEvents = UnallocatedEvents(crn, Name("Bob", "Crusher", "Jones"), listOf(activeEvent))
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccess(crn, any()) } returns deliusCaseAccess
    coEvery { workforceAllocationsToDeliusApiClient.getUnallocatedEvents(crn) } returns unallocatedEvents
    coEvery { hmppsTierApiClient.getTierByCrn(crn) } returns tier
    coEvery { repository.findByCrn(crn) } returns listOf(unallocatedCaseEntity)
    cut.upsertUnallocatedCase(crn)
    verify(exactly = 1) { dataBaseOperationService.saveNewEvents(any(), any(), any(), crn, any(), any()) }
  }

  @Test
  fun `upsertUnallocatedCase lao restricted case`() = runTest {
    val crn = "X1234567"
    val name = "Bob Jones"
    val teamCode = "N54ERT"
    val providerCode = "PC001"
    val tier = TierWithStatus("C", false)
    val unallocatedCaseEntity = UnallocatedCaseEntity(1L, name, crn, tier.tierScore, tier.provisional, teamCode, providerCode, ZonedDateTime.now(), 1)
    val deliusCaseAccess = DeliusCaseAccess(crn, true, false)
    val activeEvent = ActiveEvent("1", teamCode, providerCode)
    val unallocatedEvents = UnallocatedEvents(crn, Name("Bob", "Crusher", "Jones"), listOf(activeEvent))
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccess(crn, any()) } returns deliusCaseAccess
    coEvery { workforceAllocationsToDeliusApiClient.getUnallocatedEvents(crn) } returns unallocatedEvents
    coEvery { hmppsTierApiClient.getTierByCrn(crn) } returns tier
    coEvery { repository.findByCrn(crn) } returns listOf(unallocatedCaseEntity)
    cut.upsertUnallocatedCase(crn)
    verify(exactly = 1) { dataBaseOperationService.saveNewEvents(any(), any(), any(), crn, any(), any()) }
  }

  @Test
  fun `upsertUnallocatedCase lao excluded case`() = runTest {
    val crn = "X1234567"
    val name = "Bob Jones"
    val teamCode = "N54ERT"
    val providerCode = "PC001"
    val tier = TierWithStatus("C", false)
    val unallocatedCaseEntity = UnallocatedCaseEntity(1L, name, crn, tier.tierScore, tier.provisional, teamCode, providerCode, ZonedDateTime.now(), 1)
    val deliusCaseAccess = DeliusCaseAccess(crn, false, true)
    val activeEvent = ActiveEvent("1", teamCode, providerCode)
    val unallocatedEvents = UnallocatedEvents(crn, Name("Bob", "Crusher", "Jones"), listOf(activeEvent))
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccess(crn, any()) } returns deliusCaseAccess
    coEvery { workforceAllocationsToDeliusApiClient.getUnallocatedEvents(crn) } returns unallocatedEvents
    coEvery { hmppsTierApiClient.getTierByCrn(crn) } returns tier
    coEvery { repository.findByCrn(crn) } returns listOf(unallocatedCaseEntity)
    cut.upsertUnallocatedCase(crn)
    verify(exactly = 1) { dataBaseOperationService.saveNewEvents(any(), any(), any(), crn, any(), any()) }
  }
}
