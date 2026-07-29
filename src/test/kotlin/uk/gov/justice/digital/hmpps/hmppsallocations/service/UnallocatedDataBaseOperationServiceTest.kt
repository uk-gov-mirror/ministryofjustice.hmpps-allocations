package uk.gov.justice.digital.hmpps.hmppsallocations.service

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.client.AllocatedEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.client.DeliusCaseAccess
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.ActiveEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import java.time.ZonedDateTime

class UnallocatedDataBaseOperationServiceTest {
  val storedUnallocatedEvents = listOf(
    UnallocatedCaseEntity(1L, "Bob Jones", "J778881", "C", false, "N54ERT", "PC001", ZonedDateTime.now(), 1),
    UnallocatedCaseEntity(2L, "Bob Jones", "J778881", "C", false, "N54ERT", "PC001", ZonedDateTime.now(), 2),
  )
  val storedUnallocatedEventsSameConNumber = listOf(
    UnallocatedCaseEntity(1L, "Bob Jones", "J778881", "C", false, "N54ERT", "PC001", ZonedDateTime.now(), 1),
    UnallocatedCaseEntity(2L, "Bob Jones", "J778881", "C", false, "N54ERT", "PC001", ZonedDateTime.now(), 1),
  )
  val storedUnallocatedEventsForSave = listOf(
    UnallocatedCaseEntity(1L, "Bob Jones", "J778881", "C", false, "N54ERT", "PC001", ZonedDateTime.now(), 3),
    UnallocatedCaseEntity(2L, "Bob Jones", "J778881", "C", false, "N54ERT", "PC001", ZonedDateTime.now(), 4),
    UnallocatedCaseEntity(3L, "Bob Jones", "J778881", "C", true, "N54ERT", "PC001", ZonedDateTime.now(), 5),
  )
  val storedUnallocatedEventsForTierUpdate = listOf(
    UnallocatedCaseEntity(1L, "Bob Jones", "J778881", "B", false, "N54ERT", "PC001", ZonedDateTime.now(), 1),
    UnallocatedCaseEntity(2L, "Bob Jones", "J778881", "C", false, "N54ERT", "PC001", ZonedDateTime.now(), 2),
  )
  val storedUnallocatedEventsForProvisionalUpdate = listOf(
    UnallocatedCaseEntity(1L, "Bob Jones", "J778881", "C", true, "N54ERT", "PC001", ZonedDateTime.now(), 1),
    UnallocatedCaseEntity(2L, "Bob Jones", "J778881", "C", false, "N54ERT", "PC001", ZonedDateTime.now(), 2),
  )
  val activeEvents = hashMapOf(Pair(1, ActiveEvent("1", "N54ERT", "PC001")))

  @MockK
  lateinit var repository: UnallocatedCasesRepository

  @MockK
  lateinit var telemetryService: TelemetryService

  @MockK
  lateinit var workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient

  @InjectMockKs
  lateinit var cut: UnallocatedDataBaseOperationService

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
  }

  @Test
  fun `delete the correct event`() = runTest {
    val unallocatedCaseEntity = storedUnallocatedEvents.get(0)
    coEvery { workforceAllocationsToDeliusApiClient.getAllocatedTeam(any(), any()) } returns AllocatedEvent(unallocatedCaseEntity.teamCode)
    cut.deleteOldEvents(storedUnallocatedEvents, activeEvents)
    verify(exactly = 1) { repository.delete(storedUnallocatedEvents.get(1)) }
    verify(exactly = 1) { telemetryService.trackUnallocatedCaseAllocated(storedUnallocatedEvents.get(1), any()) }
  }

  @Test
  fun `conviction number the same - dont delete`() = runTest {
    val unallocatedCaseEntity = storedUnallocatedEventsSameConNumber.get(0)
    coEvery { workforceAllocationsToDeliusApiClient.getAllocatedTeam(any(), any()) } returns AllocatedEvent(unallocatedCaseEntity.teamCode)
    cut.deleteOldEvents(storedUnallocatedEventsSameConNumber, activeEvents)
    verify(exactly = 0) { telemetryService.trackUnallocatedCaseAllocated(any(), any()) }
  }

  @Test
  fun `will save a new event`() = runTest {
    val unallocatedCaseEntity = storedUnallocatedEvents.get(1)
    coEvery { repository.upsertUnallocatedCase(any(), any(), any(), any(), any(), any(), any()) } just runs
    cut.saveNewEvents(activeEvents, storedUnallocatedEventsForSave, unallocatedCaseEntity.name, unallocatedCaseEntity.crn, unallocatedCaseEntity.tier, unallocatedCaseEntity.provisionalTier)
    verify(exactly = 1) { telemetryService.trackAllocationDemandRaised(any(), any(), any()) }
  }

  @Test
  fun `wont save if the event isnt eligible`() = runTest {
    val unallocatedCaseEntity = storedUnallocatedEvents.get(1)
    coEvery { repository.upsertUnallocatedCase(any(), any(), any(), any(), any(), any(), any()) } just runs
    cut.saveNewEvents(activeEvents, storedUnallocatedEvents, unallocatedCaseEntity.name, unallocatedCaseEntity.crn, unallocatedCaseEntity.tier, unallocatedCaseEntity.provisionalTier)
    verify(exactly = 0) { telemetryService.trackAllocationDemandRaised(any(), any(), any()) }
  }

  @Test
  fun `update event if tier is different`() {
    val unallocatedCaseEntity = storedUnallocatedEventsForTierUpdate.get(1)
    coEvery { repository.upsertUnallocatedCase(any(), any(), any(), any(), any(), any(), any()) } just runs
    cut.updateExistingEvents(activeEvents, storedUnallocatedEventsForTierUpdate, unallocatedCaseEntity.name, unallocatedCaseEntity.tier, unallocatedCaseEntity.provisionalTier)
    verify(exactly = 1) { repository.upsertUnallocatedCase(any(), any(), any(), any(), any(), any(), any()) }
  }

  @Test
  fun `update event if tier provisional status is different`() {
    val unallocatedCaseEntity = storedUnallocatedEventsForProvisionalUpdate.get(1)
    coEvery { repository.upsertUnallocatedCase(any(), any(), any(), any(), any(), any(), any()) } just runs
    cut.updateExistingEvents(activeEvents, storedUnallocatedEventsForTierUpdate, unallocatedCaseEntity.name, unallocatedCaseEntity.tier, unallocatedCaseEntity.provisionalTier)
    verify(exactly = 1) { repository.upsertUnallocatedCase(any(), any(), any(), any(), any(), any(), any()) }
  }

  @Test
  fun deleteEventsForNoActiveEvents() = runTest {
    val crn = "J77881"
    coEvery { repository.findByCrn(crn) } returns storedUnallocatedEvents
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccess(crn, any()) } returns DeliusCaseAccess(crn, false, false)
    cut.deleteEventsForNoActiveEvents(crn)
    verify(exactly = 2) { repository.delete(any()) }
  }
}
