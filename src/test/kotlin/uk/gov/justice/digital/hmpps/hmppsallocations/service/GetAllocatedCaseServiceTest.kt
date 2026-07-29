package uk.gov.justice.digital.hmpps.hmppsallocations.service

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.client.AssessRisksNeedsApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.Name
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.AllocatedActiveEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.AllocatedEventOffences
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.AllocatedEventRequirement
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.AllocatedEventSentence
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.CrnDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusAllocatedCaseView
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusCrnRestrictionStatus
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusProbationRecord
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusRisk
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.Flag
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.MainAddressDto
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.Manager
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.ProbationRecordSentence
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.Registrations
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.SentenceOffence
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.SentencedEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.TierWithStatus
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Assessment
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.GroupReconvictionScore
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.RiskOfSeriousRecidivismScore
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.RiskPredictorOutputV1
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.RiskPredictorV1
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.RoshSummary
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class GetAllocatedCaseServiceTest {

  private val workforceAllocationsToDeliusApiClient = mockk<WorkforceAllocationsToDeliusApiClient>()
  private val laoService = mockk<LaoService>()
  private val tierApiClient = mockk<HmppsTierApiClient>()
  private val assessRisksNeedsApiClient = mockk<AssessRisksNeedsApiClient>()

  private lateinit var service: GetAllocatedCaseService

  @BeforeEach
  fun setUp() {
    service = GetAllocatedCaseService(
      workforceAllocationsToDeliusApiClient,
      laoService,
      tierApiClient,
      assessRisksNeedsApiClient,
    )
  }

  @Test
  fun `should return allocated case details`() = runBlocking {
    val crn = "X123456"
    val deliusCaseView = DeliusAllocatedCaseView(
      Name(forename = "John", middleName = null, surname = "Doe"),
      LocalDate.of(1980, 1, 1),
      "Male",
      "pnc123456",
      MainAddressDto(
        "The Manor", "1", "High Street", "London", "Greater London", "AB1 2CD", false, true, "home",
        LocalDate.of(2020, 1, 1),
      ),
      nextAppointmentDate = LocalDate.of(2023, 12, 1),
      activeEvents = listOf(
        AllocatedActiveEvent(
          1,
          0,
          LocalDate.of(2022, 1, 27),
          AllocatedEventSentence(
            "Custodial",
            LocalDate.of(2022, 1, 15),
            LocalDate.of(2023, 1, 15),
            "12 months",
          ),
          listOf(AllocatedEventOffences("Burglary", "Theft", true)),
          listOf(AllocatedEventRequirement("Curfew", null, "6 months")),
        ),
      ),
    )

    val deliusCrnRestrictionStatus = DeliusCrnRestrictionStatus(
      crn = crn,
      isRestricted = false,
      isRedacted = false,
    )

    val tier = TierWithStatus("C", false)

    // Arrange
    coEvery { workforceAllocationsToDeliusApiClient.getAllocatedDeliusCaseView(any()) } returns Mono.just(deliusCaseView)
    coEvery { laoService.getCrnRestrictionStatus(any(), any()) } returns deliusCrnRestrictionStatus
    coEvery { tierApiClient.getTierByCrn(any()) } returns tier

    // Act
    val result = service.getCase("userName", crn)

    // Assert
    assert(result!!.tier == tier.tierScore)
    assert(result.crn == crn)
    assert(result.activeEvents.size == 1)
    assert(result.activeEvents.first().sentence!!.length == "12 months")
    assert(result.activeEvents.first().offences.first().mainOffence == true)
  }

  @Test
  fun `should not populate assesment date if not assessment found`() = runBlocking {
    val crn = "X123456"

    // Arrange
    coEvery { assessRisksNeedsApiClient.getLatestCompleteAssessment(crn) } returns null

    // Act
    val result = service.getCaseAssessmentDate(crn)

    // Assert
    assert(result!!.updatedDate == null)
  }

  @Test
  fun `should return assesment date`() = runBlocking {
    val crn = "X123456"

    val now = LocalDateTime.now()
    val assessment = Assessment(now, "", "")

    // Arrange
    coEvery { assessRisksNeedsApiClient.getLatestCompleteAssessment(crn) } returns assessment

    // Act
    val result = service.getCaseAssessmentDate(crn)

    // Assert
    assert(result.updatedDate == now)
  }

  @Test
  fun `should return allocated case details with null address`() = runBlocking {
    val crn = "X123456"
    val deliusCaseView = DeliusAllocatedCaseView(
      Name(forename = "John", middleName = null, surname = "Doe"),
      LocalDate.of(1980, 1, 1),
      "Male",
      "pnc123456",
      null,
      nextAppointmentDate = LocalDate.of(2023, 12, 1),
      activeEvents = listOf(
        AllocatedActiveEvent(
          1,
          0,
          LocalDate.of(2022, 1, 27),
          AllocatedEventSentence(
            "Custodial",
            LocalDate.of(2022, 1, 15),
            LocalDate.of(2023, 1, 15),
            "12 months",
          ),
          listOf(AllocatedEventOffences("Burglary", "Theft", true)),
          listOf(AllocatedEventRequirement("Curfew", "Must comply with curfew", "6 months")),
        ),
      ),
    )

    val deliusCrnRestrictionStatus = DeliusCrnRestrictionStatus(
      crn = crn,
      isRestricted = false,
      isRedacted = false,
    )

    val tier = TierWithStatus("C", false)

    // Arrange
    coEvery { workforceAllocationsToDeliusApiClient.getAllocatedDeliusCaseView(any()) } returns Mono.just(deliusCaseView)
    coEvery { laoService.getCrnRestrictionStatus(any(), any()) } returns deliusCrnRestrictionStatus
    coEvery { tierApiClient.getTierByCrn(any()) } returns tier

    // Act
    val result = service.getCase("userName", crn)

    // Assert
    assert(result!!.tier == tier.tierScore)
    assert(result.crn == crn)
    assert(result.activeEvents.size == 1)
    assert(result.activeEvents.first().sentence!!.length == "12 months")
    assert(result.activeEvents.first().offences.first().mainOffence == true)
  }

  @Test
  fun `should return probation record details`() = runBlocking {
    val crn = "X123456"
    val convictionNumber = 2L

    val deliusCaseView = DeliusAllocatedCaseView(
      Name(forename = "John", middleName = null, surname = "Doe"),
      LocalDate.of(1980, 1, 1),
      "Male",
      "pnc123456",
      null,
      nextAppointmentDate = LocalDate.of(2023, 12, 1),
      activeEvents = listOf(
        AllocatedActiveEvent(
          1,
          0,
          LocalDate.of(2022, 1, 27),
          AllocatedEventSentence(
            "Custodial",
            LocalDate.of(2022, 1, 15),
            LocalDate.of(2023, 1, 15),
            "12 months",
          ),
          listOf(AllocatedEventOffences("Burglary", "Theft", true)),
          listOf(AllocatedEventRequirement("Curfew", "Must comply with curfew", "6 months")),
        ),
      ),
    )

    val tier = TierWithStatus("C", false)

    val sentenceOffence = SentenceOffence("Thievery", true)
    val sentenceOffence2 = SentenceOffence("Vagrancy", false)
    val sentenceOffence3 = SentenceOffence("Revelry", false)

    val probationRecordSentence = ProbationRecordSentence("Clink", "forever", LocalDate.now(), null)
    val probationRecordSentence2 = ProbationRecordSentence("Slammer", "eternity", LocalDate.now(), null)

    val sentencedEvent = SentencedEvent(probationRecordSentence, listOf(sentenceOffence, sentenceOffence2), null)
    val sentencedEvent2 = SentencedEvent(probationRecordSentence2, listOf(sentenceOffence, sentenceOffence3), null)

    val probationRecord = DeliusProbationRecord(
      crn,
      Name(forename = "John", middleName = null, surname = "Doe"),
      listOf(sentencedEvent),
      listOf(sentencedEvent, sentencedEvent2),
    )

    // Arrange
    coEvery { workforceAllocationsToDeliusApiClient.getAllocatedDeliusCaseView(any()) } returns Mono.just(deliusCaseView)
    coEvery { tierApiClient.getTierByCrn(any()) } returns tier
    coEvery { workforceAllocationsToDeliusApiClient.getProbationRecord(crn, convictionNumber) } returns probationRecord

    // Act
    val result = service.getAllocatedCaseConvictions(crn, convictionNumber)

    // Assert
    assert(result!!.crn == crn)
    assert(result!!.previous.size == 2)
    assert(result!!.tier == tier.tierScore)
    assert(result!!.active.size == 1)
    assert(result!!.convictionNumber.toLong() == convictionNumber)
  }

  @Test
  fun `should return probation risks `() = runBlocking {
    val crn = "X123456"

    val crnDetails = CrnDetails(
      crn,
      Name(
        forename = "Dylan",
        middleName = null,
        surname = "Armstrong",
      ),
      LocalDate.of(1959, 1, 20),
      Manager(
        "N57A046",
        Name("Samantha", "", "Bryant"),
        "N03S0D",
        "PO",
        true,
      ),
      true,
    )

    val tier = TierWithStatus("C", false)

    val deliusRisk = DeliusRisk(
      Name(forename = "Dylan", middleName = null, surname = "Armstrong"),
      crn,
      listOf(
        Registrations(
          "ALT Under MAPPA Arrangements",
          LocalDate.of(2021, 8, 30),
          null,
          "some Notes",
          Flag("MAPPA Risk"),
        ),
        Registrations(
          "Suicide/self-harm",
          LocalDate.of(2021, 8, 30),
          null,
          "some Notes",
          Flag("MAPPA Risk"),
        ),
      ),
      listOf(
        Registrations(
          "Child Protection",
          LocalDate.of(2021, 5, 20),
          LocalDate.of(2021, 8, 30),
          "some Notes",
          Flag("Child Protection Flag"),
        ),
      ),
    )

    val roshSummary = RoshSummary(
      "VERY_HIGH",
      LocalDate.of(2022, 10, 7),
      mapOf(
        "crn" to crn,
        "Public" to "HIGH",
        "Children" to "LOW",
        "Known Adult" to "MEDIUM",
        "Staff" to "VERY_HIGH",
        "Prisoners" to "MEDIUM",
      ),
    )

    val riskPredictor = RiskPredictorV1(
      LocalDateTime.parse("2019-02-12T16:09:10.271"),
      null,
      null,
      "1",
      RiskPredictorOutputV1(
        GroupReconvictionScore(
          null,
          BigDecimal.valueOf(85),
          null,
        ),
        null,
        null,
        RiskOfSeriousRecidivismScore(
          BigDecimal.valueOf(50),
          null,
          null,
          null,
          "MEDIUM",
        ),
        null,
      ),
    )

    // Arrange
    coEvery { workforceAllocationsToDeliusApiClient.getCrnDetails(any()) } returns crnDetails
    coEvery { workforceAllocationsToDeliusApiClient.getDeliusRisk(any()) } returns deliusRisk
    coEvery { tierApiClient.getTierByCrn(any()) } returns tier
    coEvery { assessRisksNeedsApiClient.getRosh(crn) } returns roshSummary
    coEvery { assessRisksNeedsApiClient.getRiskPredictors(crn) } returns flowOf(riskPredictor)

    // Act
    val result = service.getCaseRisks(crn)

    // Assert
    assert(result!!.crn == crn)
    assert(result!!.name == "Dylan Armstrong")
    assert(result!!.tier == tier.tierScore)
    assert(result!!.activeRegistrations.size == 2)
    assert(result!!.inactiveRegistrations.size == 1)
    assert(result!!.getOGRSScore() == BigDecimal.valueOf(85))
    assert(result!!.getROSHLevel() == "VERY_HIGH")
    assert(result!!.getRSRLevel() == "MEDIUM")
    assert(result!!.activeRegistrations.get(0).type == "ALT Under MAPPA Arrangements")
  }
}
