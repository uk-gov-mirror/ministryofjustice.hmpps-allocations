package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.CommunityEventManager
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusProbationRecord
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.SentenceOffence
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.SentencedEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.time.LocalDate

data class UnallocatedCaseConvictions @JsonCreator constructor(
  @Schema(description = "Offender Name", example = "John Smith")
  val name: String,
  @Schema(description = "CRN", example = "J111111")
  val crn: String,
  @Schema(description = "Latest tier of case", example = "D")
  val tier: String,
  val provisionalTier: Boolean,
  val active: List<UnallocatedCaseConviction>,
  val previous: List<UnallocatedCaseConviction>,
  val convictionNumber: Int,
) {
  companion object {
    fun from(
      case: UnallocatedCaseEntity,
      probationRecord: DeliusProbationRecord,
    ): UnallocatedCaseConvictions = UnallocatedCaseConvictions(
      probationRecord.name.getCombinedName(),
      case.crn,
      case.tier,
      case.provisionalTier,
      probationRecord.activeEvents.map { UnallocatedCaseConviction.from(it) },
      probationRecord.inactiveEvents.map { UnallocatedCaseConviction.from(it) },
      case.convictionNumber,
    )

    @Suppress("LongParameterList")
    fun from(
      probationRecord: DeliusProbationRecord,
      crn: String,
      convictionNumber: Int,
      tier: String,
      provisionalTier: Boolean,
    ): UnallocatedCaseConvictions = UnallocatedCaseConvictions(
      probationRecord.name.getCombinedName(),
      crn,
      tier,
      provisionalTier,
      probationRecord.activeEvents.map { UnallocatedCaseConviction.from(it) },
      probationRecord.inactiveEvents.map { UnallocatedCaseConviction.from(it) },
      convictionNumber,
    )
  }
}

data class UnallocatedCaseConviction @JsonCreator constructor(
  @Schema(description = "Description", example = "ORA Community Order")
  val description: String,
  @Schema(description = "Length", example = "5 Months")
  val length: String,
  val offenderManager: UnallocatedCaseConvictionPractitioner?,
  @Schema(description = "Start of sentence", example = "2021-11-15")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val startDate: LocalDate?,
  @Schema(description = "End of sentence", example = "2021-11-15")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val endDate: LocalDate?,
  val offences: List<UnallocatedCaseConvictionOffence>,
) {
  companion object {

    fun from(sentencedEvent: SentencedEvent): UnallocatedCaseConviction = UnallocatedCaseConviction(
      sentencedEvent.sentence.description,
      sentencedEvent.sentence.length,
      UnallocatedCaseConvictionPractitioner.from(sentencedEvent.manager),
      sentencedEvent.sentence.startDate,
      sentencedEvent.sentence.terminationDate,
      sentencedEvent.offences.map { UnallocatedCaseConvictionOffence.from(it) },
    )
  }
}

data class UnallocatedCaseConvictionPractitioner @JsonCreator constructor(
  @Schema(description = "Full Name", example = "John William Smith")
  val name: String?,
  @Schema(description = "Grade", example = "PSO")
  val grade: String?,
) {
  companion object {
    fun from(communityEventManager: CommunityEventManager?): UnallocatedCaseConvictionPractitioner? = communityEventManager?.let { UnallocatedCaseConvictionPractitioner(it.name.getCombinedName(), it.grade) }
  }
}

data class UnallocatedCaseConvictionOffence @JsonCreator constructor(
  @Schema(description = "Description", example = "ORA Community Order")
  val description: String,
  @Schema(description = "Main offence", example = "true|false")
  val mainOffence: Boolean,
) {
  companion object {
    fun from(sentenceOffence: SentenceOffence): UnallocatedCaseConvictionOffence = UnallocatedCaseConvictionOffence(sentenceOffence.description, sentenceOffence.main)
  }
}
