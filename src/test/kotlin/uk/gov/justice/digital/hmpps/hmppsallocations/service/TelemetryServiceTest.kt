package uk.gov.justice.digital.hmpps.hmppsallocations.service

import com.microsoft.applicationinsights.TelemetryClient
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.time.ZonedDateTime

class TelemetryServiceTest {
  @MockK
  lateinit var telemetryClient: TelemetryClient

  @InjectMockKs
  lateinit var telemetryService: TelemetryService

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
  }

  @Test
  fun trackUnallocatedCaseAllocated() {
    val crn = "X1234567"
    val name = "Bob Jones"
    val teamCode = "N54ERT"
    val providerCode = "PC001"
    val tier = "C"
    val unallocatedCaseEntity = UnallocatedCaseEntity(1L, name, crn, tier, false, teamCode, providerCode, ZonedDateTime.now(), 1)
    coEvery { telemetryClient.trackEvent(TelemetryEventType.EventAllocated.eventName, any(), null) } returns Unit
    telemetryService.trackUnallocatedCaseAllocated(unallocatedCaseEntity, teamCode)
    verify(exactly = 1) { telemetryClient.trackEvent(TelemetryEventType.EventAllocated.eventName, any(), null) }
  }

  @Test
  fun trackAllocationDemandRaised() {
    val crn = "X1234567"
    val teamCode = "N54ERT"
    val providerCode = "PC001"
    coEvery { telemetryClient.trackEvent(TelemetryEventType.ALLOCATION_DEMAND_RAISED.eventName, any(), null) } returns Unit
    telemetryService.trackAllocationDemandRaised(crn, teamCode, providerCode)
    verify(exactly = 1) { telemetryClient.trackEvent(TelemetryEventType.ALLOCATION_DEMAND_RAISED.eventName, any(), null) }
  }
}
