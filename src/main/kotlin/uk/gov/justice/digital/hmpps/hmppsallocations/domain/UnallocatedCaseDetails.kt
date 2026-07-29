package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityPersonManager
import uk.gov.justice.digital.hmpps.hmppsallocations.client.InitialAppointment
import uk.gov.justice.digital.hmpps.hmppsallocations.client.RecentAllocatedEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.client.Staff
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.CaseViewDocument
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusCaseView
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.MainAddress
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.Offence
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.Requirement
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime

data class UnallocatedCaseDetails @JsonCreator constructor(

  @Schema(description = "Offender Name", example = "John Smith")
  val name: String,
  @Schema(description = "CRN", example = "J111111")
  val crn: String,
  @Schema(description = "Latest tier of case", example = "D")
  val tier: String,
  val provisionalTier: Boolean,
  @Schema(description = "Sentence Date", example = "2020-01-16")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = STRING)
  val sentenceDate: LocalDate,
  @Schema(description = "Gender", example = "Male")
  val gender: String?,
  @Schema(description = "Date Of Birth", example = "2021-06-19")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = STRING)
  val dateOfBirth: LocalDate?,
  @Schema(description = "Age", example = "34")
  val age: Int,
  val offences: List<UnallocatedCaseOffence>?,
  @Schema(description = "Expected Sentence Date", example = "2020-05-16")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = STRING)
  val expectedSentenceEndDate: LocalDate?,
  @Schema(description = "Sentence description", example = "SA2020 Suspended Sentence Order")
  val sentenceDescription: String?,
  val requirements: List<UnallocatedCaseRequirement>?,
  @Schema(description = "PNC Number")
  val pncNumber: String?,
  val courtReport: UnallocatedCaseDocument?,
  val assessment: UnallocatedAssessment?,
  val cpsPack: UnallocatedCaseDocument?,
  val preConvictionDocument: UnallocatedCaseDocument?,
  val address: MainAddress?,
  @Schema(description = "Sentence Length")
  val sentenceLength: String?,
  val convictionNumber: Int,
  val riskVersion: String?,
  val risk: Any?,
  val activeRiskRegistration: String?,
  val outOfAreaTransfer: Boolean,
) {

  companion object {

    @Suppress("LongParameterList")
    fun from(
      case: UnallocatedCaseEntity,
      deliusCaseView: DeliusCaseView,
      assessment: Assessment?,
      unallocatedCaseRisks: UnallocatedCaseRisks<Any>?,
      outOfAreaTransfer: Boolean,
    ): UnallocatedCaseDetails = UnallocatedCaseDetails(
      deliusCaseView.name.getCombinedName(),
      case.crn, case.tier, case.provisionalTier, deliusCaseView.sentence.startDate,
      deliusCaseView.gender,
      deliusCaseView.dateOfBirth,
      deliusCaseView.age,
      deliusCaseView.offences.map { UnallocatedCaseOffence.from(it) },
      deliusCaseView.sentence.endDate,
      deliusCaseView.sentence.description,
      deliusCaseView.requirements.map { UnallocatedCaseRequirement.from(it) },
      deliusCaseView.pncNumber,
      UnallocatedCaseDocument.from(deliusCaseView.courtReport),
      UnallocatedAssessment.from(assessment),
      UnallocatedCaseDocument.from(deliusCaseView.cpsPack),
      UnallocatedCaseDocument.from(deliusCaseView.preConvictionDocument),
      deliusCaseView.mainAddress,
      deliusCaseView.sentence.length,
      case.convictionNumber,
      unallocatedCaseRisks?.riskVersion,
      unallocatedCaseRisks?.let { getOverallRisk(it) },
      unallocatedCaseRisks?.activeRegistrations?.takeUnless { it.isEmpty() }?.joinToString(", ") { it.type },
      outOfAreaTransfer,
    )
  }
}

data class OffenderManagerDetails @JsonCreator constructor(
  @Schema(description = "Forenames", example = "John William")
  val forenames: String?,
  @Schema(description = "Surname", example = "Smith")
  val surname: String?,
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(description = "Grade", example = "PSO")
  val grade: String?,
) {

  companion object {

    fun from(communityPersonManager: CommunityPersonManager?, probationStatus: String): OffenderManagerDetails? = communityPersonManager?.name?.forename?.takeUnless { probationStatus == "New to probation" }?.let {
      OffenderManagerDetails(
        communityPersonManager.name.forename,
        communityPersonManager.name.surname,
        gradeFrom(probationStatus, communityPersonManager.grade),
      )
    }

    fun from(recentAllocatedEvent: RecentAllocatedEvent?, probationStatus: String): OffenderManagerDetails? = recentAllocatedEvent?.manager?.name?.forename?.takeUnless { probationStatus == "New to probation" }?.let {
      OffenderManagerDetails(
        recentAllocatedEvent.manager.name.forename,
        recentAllocatedEvent.manager.name.surname,
        gradeFrom(probationStatus, recentAllocatedEvent.manager.grade),
      )
    }

    private fun gradeFrom(probationStatus: String, grade: String?): String? {
      if (probationStatus == "Currently managed") {
        return grade
      }
      return null
    }
  }
}

data class InitialAppointmentDetails @JsonCreator constructor(
  @Schema(description = "Initial appointment Date", example = "2020-01-16")
  val date: LocalDate?,
  @JsonInclude(JsonInclude.Include.NON_NULL)
  val staff: Staff?,
) {
  companion object {
    fun from(initialAppointment: InitialAppointment?): InitialAppointmentDetails = InitialAppointmentDetails(
      initialAppointment?.date,
      initialAppointment?.staff,
    )
  }
}

data class UnallocatedCaseOffence @JsonCreator constructor(
  @Schema(description = "Main Offence", example = "True")
  val mainOffence: Boolean,
  @Schema(description = "Main Category", example = "Abstracting electricity")
  val mainCategory: String,
  @Schema(description = "Sub Category", example = "Abstracting electricity")
  val subCategory: String,
) {
  companion object {
    fun from(offence: Offence): UnallocatedCaseOffence = UnallocatedCaseOffence(
      offence.mainOffence,
      offence.mainCategory,
      offence.subCategory,
    )
  }
}

data class UnallocatedCaseRequirement @JsonCreator constructor(
  @Schema(description = "Main Category", example = "Unpaid Work")
  val mainCategory: String,
  @Schema(description = "Sub Category", example = "Regular")
  val subCategory: String?,
  @Schema(description = "Length", example = "100")
  val length: String?,
) {
  companion object {
    fun from(requirement: Requirement): UnallocatedCaseRequirement = UnallocatedCaseRequirement(
      requirement.mainCategory,
      requirement.subCategory,
      requirement.length,
    )
  }
}

data class UnallocatedCaseDocument @JsonCreator constructor(
  @Schema(description = "Description", example = "Fast")
  var description: String?,
  @Schema(description = "Completed Date", example = "2019-11-11")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = STRING)
  val completedDate: LocalDate?,
  @Schema(description = "Document Id used to download the document", example = "00000000-0000-0000-0000-000000000000")
  val documentId: String?,
  @Schema(description = "Name of document")
  val name: String,
) {
  companion object {
    fun from(document: CaseViewDocument?): UnallocatedCaseDocument? = document?.let {
      UnallocatedCaseDocument(
        it.description,
        it.dateCreated,
        it.documentId,
        it.documentName,
      )
    }
  }
}

data class AssessmentDate @JsonCreator constructor(
  val updatedDate: LocalDateTime?,
)
data class UnallocatedAssessment @JsonCreator constructor(
  @Schema(description = "Completed Date", example = "2019-11-11")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = STRING)
  val lastAssessedOn: LocalDateTime,
  val type: String,
) {
  companion object {
    fun from(assessment: Assessment?): UnallocatedAssessment? = assessment?.let {
      UnallocatedAssessment(assessment.completed!!, assessment.assessmentType)
    }
  }
}

data class OverallRiskV1 @JsonCreator constructor(
  val roshLevel: String?,
  val rsrLevel: String?,
  val ogrsScore: BigInteger?,
) {
  companion object {
    fun from(unallocatedCaseRisksV1: UnallocatedCaseRisks<RiskV1>) = OverallRiskV1(
      unallocatedCaseRisksV1.getROSHLevel(),
      unallocatedCaseRisksV1.getRSRLevel(),
      unallocatedCaseRisksV1.getOGRSScore()?.toBigInteger(),
    )
  }
}

data class OverallRiskV2 @JsonCreator constructor(
  val roshLevel: String?,
  val combinedSeriousReoffendingPredictor: CombinedSeriousReoffendingPredictor?,
  val allReoffendingPredictor: AllReoffendingPredictor?,
) {
  companion object {
    fun from(unallocatedCaseRisksV2: UnallocatedCaseRisks<RiskV2>) = OverallRiskV2(
      unallocatedCaseRisksV2.getROSHLevel(),
      unallocatedCaseRisksV2.risk?.combinedSeriousReoffendingPredictor,
      unallocatedCaseRisksV2.risk?.allReoffendingPredictor,
    )
  }
}

fun getOverallRisk(unallocatedCaseRisks: UnallocatedCaseRisks<Any>?): Any {
  if (unallocatedCaseRisks?.riskVersion == "2") {
    return OverallRiskV2.from(unallocatedCaseRisks as UnallocatedCaseRisks<RiskV2>)
  } else {
    return OverallRiskV1.from(unallocatedCaseRisks as UnallocatedCaseRisks<RiskV1>)
  }
}
