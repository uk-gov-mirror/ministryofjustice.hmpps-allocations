package uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.projection.CaseCountByTeam

@Repository
interface UnallocatedCasesRepository : CrudRepository<UnallocatedCaseEntity, Long> {
  fun existsByCrn(crn: String): Boolean

  fun findByCrn(crn: String): List<UnallocatedCaseEntity>

  fun findCaseByCrnAndConvictionNumber(crn: String, convictionNumber: Int): UnallocatedCaseEntity?

  fun findByTeamCode(teamCode: String): List<UnallocatedCaseEntity>

  @Query("select u.teamCode AS teamCode, count(*) AS caseCount from UnallocatedCaseEntity u where u.teamCode in :teamCodes GROUP BY u.teamCode")
  fun getCaseCountByTeam(teamCodes: List<String>): List<CaseCountByTeam>

  @Suppress("LongParameterList")
  @Modifying
  @Query(
    value = """INSERT INTO unallocated_cases ("name", crn, tier, provisional_tier, team_code, provider_code, conviction_number) VALUES (:name, :crn, :tier, :provisionalTier, :teamCode, :providerCode, :convictionNumber)
      ON CONFLICT (conviction_number, crn) DO UPDATE SET "name" = excluded.name, tier = excluded.tier, provisional_tier = excluded.provisional_tier, team_code = excluded.team_code, provider_code = excluded.provider_code;""",
    nativeQuery = true,
  )
  fun upsertUnallocatedCase(name: String, crn: String, tier: String, provisionalTier: Boolean, teamCode: String, providerCode: String, convictionNumber: Int)
}
