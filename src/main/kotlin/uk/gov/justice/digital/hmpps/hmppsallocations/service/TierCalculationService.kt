package uk.gov.justice.digital.hmpps.hmppsallocations.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.TierNotFoundException
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.TierWithStatus
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository

@Service
class TierCalculationService(
  private val hmppsTierApiClient: HmppsTierApiClient,
  private val repository: UnallocatedCasesRepository,
) {

  @Transactional
  suspend fun updateTier(crn: String) {
    if (repository.existsByCrn(crn)) {
      val tier = getTier(crn)
      repository.findByCrn(crn).forEach {
        it.tier = tier.tierScore
        it.provisionalTier = tier.provisional
        repository.save(it)
      }
    }
  }

  suspend fun getTier(crn: String): TierWithStatus = hmppsTierApiClient.getTierByCrn(crn = crn) ?: throw TierNotFoundException("Tier not found: $crn")
}
