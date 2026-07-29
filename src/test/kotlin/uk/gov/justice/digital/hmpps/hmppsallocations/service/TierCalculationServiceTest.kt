package uk.gov.justice.digital.hmpps.hmppsallocations.service

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.TierWithStatus
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import java.time.ZonedDateTime

class TierCalculationServiceTest {

  @MockK
  lateinit var hmppsTierApiClient: HmppsTierApiClient

  @MockK
  lateinit var repository: UnallocatedCasesRepository

  @InjectMockKs
  lateinit var cut: TierCalculationService

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
  }

  @Test
  fun updateTier() = runTest {
    val crn = "X1234567"
    val name = "Bob Jones"
    val teamCode = "N54ERT"
    val providerCode = "PC001"
    val tier = TierWithStatus("C", true)
    val unallocatedCaseEntity = UnallocatedCaseEntity(1L, name, crn, tier.tierScore, tier.provisional, teamCode, providerCode, ZonedDateTime.now(), 1)
    coEvery { repository.existsByCrn(crn) }.returns(true)
    coEvery { repository.findByCrn(crn) } returns listOf(unallocatedCaseEntity)
    coEvery { hmppsTierApiClient.getTierByCrn(crn) }.returns(tier)
    coEvery { repository.save(unallocatedCaseEntity) } returns unallocatedCaseEntity
    cut.updateTier(crn)
    verify(exactly = 1) { repository.save(any()) }
  }

  @Test
  fun getTier() = runTest {
    val crn = "X1234567"
    val tier = TierWithStatus("C", false)
    coEvery { hmppsTierApiClient.getTierByCrn(crn) } returns tier
    val actual = cut.getTier(crn)
    assertEquals(tier, actual)
  }
}
