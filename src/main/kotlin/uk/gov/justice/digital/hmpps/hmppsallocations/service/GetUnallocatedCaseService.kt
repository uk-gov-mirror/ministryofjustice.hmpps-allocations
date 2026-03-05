package uk.gov.justice.digital.hmpps.hmppsallocations.service

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.client.AssessRisksNeedsApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.DeliusCaseDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.client.DeliusUserAccess
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.CrnStaffRestrictions
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusCaseView
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusCrnRestrictionStatus
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Assessment
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseCountByTeam
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseOverview
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.RiskPredictorV1
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.RiskPredictorV2
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCase
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseConfirmInstructions
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseConvictions
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseRisks
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseRisksV1
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseRisksV2
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.NotAllowedForLAOException
import java.time.LocalDateTime

@Suppress("TooManyFunctions")
@Service
class GetUnallocatedCaseService(
  private val unallocatedCasesRepository: UnallocatedCasesRepository,
  private val outOfAreaTransferService: OutOfAreaTransferService,
  @Qualifier("assessRisksNeedsApiClientUserEnhanced")
  private val assessRisksNeedsApiClient: AssessRisksNeedsApiClient,
  @Qualifier("workforceAllocationsToDeliusApiClientUserEnhanced")
  private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
  @Qualifier("laoService")
  private val laoService: LaoService,
) {

  suspend fun getCase(userName: String, crn: String, convictionNumber: Long): UnallocatedCaseDetails? {
    if (laoService.getCrnRestrictions(userName, crn).apopUserExcluded) {
      throw NotAllowedForLAOException("User is excluded from viewing this case", crn)
    }
    return findUnallocatedCaseByConvictionNumber(crn, convictionNumber)?.let { unallocatedCaseEntity ->
      val assessment = assessRisksNeedsApiClient.getLatestCompleteAssessment(crn)
      val getDeliusCaseViewApiCall = workforceAllocationsToDeliusApiClient.getDeliusCaseView(crn, convictionNumber)
      val getDeliusCaseDetailsApiCall = workforceAllocationsToDeliusApiClient
        .getDeliusCaseDetails(
          crn,
          convictionNumber,
        )
      callDeliusCaseViewAndCaseDetailApisInParallel(
        crn,
        convictionNumber,
        unallocatedCaseEntity,
        assessment,
        getDeliusCaseViewApiCall,
        getDeliusCaseDetailsApiCall,
      )
    }
  }

  @Suppress("LongParameterList")
  private suspend fun callDeliusCaseViewAndCaseDetailApisInParallel(
    crn: String,
    convictionNumber: Long,
    unallocatedCaseEntity: UnallocatedCaseEntity,
    assessment: Assessment?,
    getDeliusCaseViewCall: Mono<DeliusCaseView>,
    getDeliusCaseDetailsCall: Mono<DeliusCaseDetails>,
  ): UnallocatedCaseDetails? = Mono.zip(getDeliusCaseViewCall, getDeliusCaseDetailsCall)
    .awaitSingle()
    .let {
      val caseView = it.t1
      val caseDetails = it.t2
      val caseIsOutOfAreaTransfer = if (caseDetails.cases.firstOrNull() != null) {
        outOfAreaTransferService.isCaseCurrentlyManagedOutsideOfCurrentTeamsRegion(
          currentTeamCode = unallocatedCaseEntity.teamCode,
          unallocatedCasesFromDelius = caseDetails.cases.first(),
        )
      } else {
        false
      }
      getRisksAndGenerateUnallocatedCaseDetails(
        crn,
        convictionNumber,
        unallocatedCaseEntity,
        caseView,
        assessment,
        caseIsOutOfAreaTransfer,
      )
    }

  @Suppress("LongParameterList")
  private suspend fun getRisksAndGenerateUnallocatedCaseDetails(
    crn: String,
    convictionNumber: Long,
    unallocatedCaseEntity: UnallocatedCaseEntity,
    caseView: DeliusCaseView,
    assessment: Assessment?,
    outOfAreaTransfer: Boolean,
  ): UnallocatedCaseDetails? {
    val unallocatedCaseRisks = getCaseRisks(crn, convictionNumber)
    return UnallocatedCaseDetails.from(
      unallocatedCaseEntity,
      caseView,
      assessment,
      unallocatedCaseRisks,
      outOfAreaTransfer,
    )
  }

  suspend fun getCaseOverview(crn: String, convictionNumber: Long): CaseOverview? = findUnallocatedCaseByConvictionNumber(crn, convictionNumber)?.let {
    CaseOverview.from(it)
  }

  @SuppressWarnings("LongMethod")
  suspend fun getAllByTeam(userName: String, teamCode: String): List<UnallocatedCase> {
    log.info("Getting all unallocated cases for team $teamCode")
    val unallocatedCases = unallocatedCasesRepository.findByTeamCode(teamCode)
    if (unallocatedCases.isEmpty()) {
      return emptyList()
    } else {
      val unallocatedCasesUserAccess = workforceAllocationsToDeliusApiClient.getUserAccess(
        crns = unallocatedCases.map { it.crn },
      ).access

      val unallocatedCasesFromDelius = workforceAllocationsToDeliusApiClient
        .getDeliusCaseDetailsCases(cases = unallocatedCases)
        .filter { deliusUnallocatedCase ->
          unallocatedCases
            .any { deliusUnallocatedCase.crn == it.crn && deliusUnallocatedCase.event.number.toInt() == it.convictionNumber }
        }
        .toList()

      if (unallocatedCasesFromDelius.isEmpty()) {
        return emptyList()
      } else {
        val crnsThatAreCurrentlyManagedOutsideOfThisTeamsRegion = outOfAreaTransferService
          .getCasesThatAreCurrentlyManagedOutsideOfCurrentTeamsRegion(
            teamCode,
            unallocatedCasesFromDelius,
          ).map { it.crn }

        return unallocatedCasesFromDelius
          .map { deliusCaseDetail ->
            val unallocatedCase =
              unallocatedCases.first { it.crn == deliusCaseDetail.crn && it.convictionNumber == deliusCaseDetail.event.number.toInt() }
            val limitedAccess = unallocatedCasesUserAccess.any { it.crn == unallocatedCase.crn && (it.userExcluded || it.userRestricted) }
            var restricted = false

            if (limitedAccess) {
              val crnLaoAccess = laoService.getCrnRestrictions(userName, unallocatedCase.crn)
              restricted = crnLaoAccess.apopUserExcluded
            }

            UnallocatedCase.from(
              unallocatedCase,
              deliusCaseDetail,
              outOfAreaTransfer = crnsThatAreCurrentlyManagedOutsideOfThisTeamsRegion.contains(unallocatedCase.crn),
              limitedAccess,
              restricted,
            )
          }
      }
    }
  }

  suspend fun getCaseConvictions(crn: String, excludeConvictionNumber: Long): UnallocatedCaseConvictions? {
    return findUnallocatedCaseByConvictionNumber(crn, excludeConvictionNumber)?.let {
      val probationRecord = workforceAllocationsToDeliusApiClient.getProbationRecord(crn, excludeConvictionNumber)
      return UnallocatedCaseConvictions.from(it, probationRecord)
    }
  }

  suspend fun getCaseRisks(crn: String, convictionNumber: Long): UnallocatedCaseRisks<Any>? {
    return findUnallocatedCaseByConvictionNumber(crn, convictionNumber)?.let { unallocatedCaseEntity ->
      val riskPredictor = assessRisksNeedsApiClient.getRiskPredictors(crn)
        .filter { (it.getRSRScoreLevel() != null && it.getRSRPercentageScore() != null) || (it.getOGRSScoreLevel() != null && it.getOGRSPercentageScore() != null) }
        .toList().maxByOrNull { it.completedDate ?: LocalDateTime.MIN }
      val deliusRisk = workforceAllocationsToDeliusApiClient.getDeliusRisk(crn)
      val rosh = assessRisksNeedsApiClient.getRosh(crn)
      return if (riskPredictor?.outputVersion == "2") {
        UnallocatedCaseRisksV2.from(deliusRisk, unallocatedCaseEntity, rosh, riskPredictor as RiskPredictorV2?)
      } else {
        UnallocatedCaseRisksV1.from(deliusRisk, unallocatedCaseEntity, rosh, riskPredictor as RiskPredictorV1?)
      }
    }
  }

  fun getCaseCountByTeam(teamCodes: List<String>): Flux<CaseCountByTeam> = Flux.fromIterable(unallocatedCasesRepository.getCaseCountByTeam(teamCodes))
    .map { CaseCountByTeam(it.getTeamCode(), it.getCaseCount()) }

  private fun findUnallocatedCaseByConvictionNumber(
    crn: String,
    convictionNumber: Long,
  ) = unallocatedCasesRepository.findCaseByCrnAndConvictionNumber(crn, convictionNumber.toInt())

  suspend fun getCaseConfirmInstructions(crn: String, convictionNumber: Long, staffCode: String): UnallocatedCaseConfirmInstructions? {
    return findUnallocatedCaseByConvictionNumber(crn, convictionNumber)?.let { unallocatedCaseEntity ->
      val personOnProbationStaffDetailsResponse = workforceAllocationsToDeliusApiClient.personOnProbationStaffDetails(crn, staffCode)
      return UnallocatedCaseConfirmInstructions.from(
        unallocatedCaseEntity,
        personOnProbationStaffDetailsResponse,
      )
    }
  }

  suspend fun restricted(crn: String): Boolean = workforceAllocationsToDeliusApiClient.getUserAccess(crn)?.run { userRestricted } ?: true

  suspend fun getCrnStaffRestrictions(crn: String, staffCodes: List<String>): CrnStaffRestrictions? = laoService.getCrnRestrictionsForUsers(crn, staffCodes)

  suspend fun isCrnRestricted(userName: String, crn: String): Boolean? = laoService.isCrnRestricted(userName, crn)
  suspend fun getCaseRestrictions(userName: String, crn: List<String>): DeliusUserAccess = laoService.getCrnRestrictions(userName, crn)
  suspend fun getCaseRestrictions(userName: String, crn: String): DeliusCrnRestrictionStatus = laoService.getCrnRestrictionStatus(userName, crn)
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
