package uk.gov.justice.digital.hmpps.hmppsallocations.service

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.EmptyTeamForEventException
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.ActiveEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository

@Service
class UnallocatedDataBaseOperationService(
  private val repository: UnallocatedCasesRepository,
  private val telemetryService: TelemetryService,
  @Qualifier("workforceAllocationsToDeliusApiClient") private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Suppress("LongParameterList")
  @Transactional
  fun saveNewEvents(
    activeEvents: Map<Int, ActiveEvent>,
    storedUnallocatedEvents: List<UnallocatedCaseEntity>,
    name: String,
    crn: String,
    tier: String,
    provisionalTier: Boolean,
  ) {
    activeEvents
      .filter { activeEvent -> storedUnallocatedEvents.none { entry -> entry.convictionNumber == activeEvent.key } }
      .map { it.value }
      .forEach { createEvent ->
        logger.debug("Saving new event with CRN $crn, teamCode ${createEvent.teamCode}, convictionNumber ${createEvent.eventNumber.toInt()}")
        repository.upsertUnallocatedCase(name, crn, tier, provisionalTier, createEvent.teamCode, createEvent.providerCode, Integer.parseInt(createEvent.eventNumber))
        telemetryService.trackAllocationDemandRaised(crn, createEvent.teamCode, createEvent.providerCode)
      }
  }

  @Transactional
  suspend fun deleteOldEvents(
    storedUnallocatedEvents: List<UnallocatedCaseEntity>,
    activeEvents: Map<Int, ActiveEvent>,
  ) {
    storedUnallocatedEvents
      .filter { !activeEvents.containsKey(it.convictionNumber) }
      .forEach { deleteEvent ->
        logger.debug("Deleting event for CRN: ${deleteEvent.crn}, conviction number: ${deleteEvent.convictionNumber}, teamCode: ${deleteEvent.teamCode}")
        try {
          repository.delete(deleteEvent)
          logger.debug("Event $deleteEvent deleted")
          val team = workforceAllocationsToDeliusApiClient.getAllocatedTeam(deleteEvent.crn, deleteEvent.convictionNumber)
          telemetryService.trackUnallocatedCaseAllocated(deleteEvent, team?.teamCode)
        } catch (e: ObjectOptimisticLockingFailureException) {
          logger.error("Event with id ${deleteEvent.id} Optimistic Locking failure, probably already deleted; ${e.message}")
        } catch (e: EmptyTeamForEventException) {
          logger.error("Event with id ${deleteEvent.id} could not find team; ${e.message}")
        }
      }
  }

  @Suppress("LongParameterList")
  @Transactional
  fun updateExistingEvents(
    activeEvents: Map<Int, ActiveEvent>,
    storedUnallocatedEvents: List<UnallocatedCaseEntity>,
    name: String,
    tier: String,
    provisionalTier: Boolean,
  ) {
    storedUnallocatedEvents
      .filter { activeEvents.containsKey(it.convictionNumber) }
      .forEach { unallocatedCaseEntity ->
        val activeEvent = activeEvents[unallocatedCaseEntity.convictionNumber]!!
        logger.debug("Updating existing event for crn ${unallocatedCaseEntity.crn}, convictionNumber ${unallocatedCaseEntity.convictionNumber}, teamCode ${activeEvent.teamCode}")
        repository.upsertUnallocatedCase(name, unallocatedCaseEntity.crn, tier, provisionalTier, activeEvent.teamCode, activeEvent.providerCode, unallocatedCaseEntity.convictionNumber)
      }
  }

  suspend fun deleteEventsForNoActiveEvents(crn: String) {
    logger.debug("delete events for crn: $crn")
    val storedUnallocatedEvents = repository.findByCrn(crn)
    storedUnallocatedEvents.forEach {
      repository.delete(it)
      logger.debug("Event ${it.id} deleted")
    }
  }
}
