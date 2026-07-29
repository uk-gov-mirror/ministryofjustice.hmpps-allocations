package uk.gov.justice.digital.hmpps.hmppsallocations.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import com.ninjasquad.springmockk.MockkBean
import io.mockk.justRun
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.AssessRisksNeedsApiExtension
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.ProbateEstateApiExtension
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.TierApiExtension
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.WorkforceAllocationsToDeliusApiExtension
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.SavedEmailsRepository
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.CalculationEventListener.CalculationEventData
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.CalculationEventListener.PersonReference
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.CalculationEventListener.PersonReferenceType
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.HmppsOffenderEvent
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue

@ExtendWith(
  AssessRisksNeedsApiExtension::class,
  TierApiExtension::class,
  ProbateEstateApiExtension::class,
  WorkforceAllocationsToDeliusApiExtension::class,
  HmppsAuthApiExtension::class,
)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "720000")
@ActiveProfiles("test")
abstract class IntegrationTestBase {

  val previouslyManagedCase = UnallocatedCaseEntity(
    null,
    "Hannah Francis",
    "J680660",
    "C",
    provisionalTier = false,
    providerCode = "",
    teamCode = "TEAM1",
    convictionNumber = 4,
  )

  val laoCase1 = UnallocatedCaseEntity(
    null,
    "Joe Bloggs",
    "XXXXXXX",
    "C",
    provisionalTier = false,
    providerCode = "",
    teamCode = "TEAM1",
    convictionNumber = 1,
  )

  val laoCase2 = UnallocatedCaseEntity(
    null,
    "Joe Bloggs",
    "ZZZZZZZ",
    "C",
    provisionalTier = false,
    providerCode = "",
    teamCode = "TEAM1",
    convictionNumber = 1,
  )

  fun insertCases() {
    repository.saveAll(
      listOf(
        UnallocatedCaseEntity(
          null,
          "Dylan Adam Armstrong",
          "J678910",
          "C",
          provisionalTier = false,
          providerCode = "",
          teamCode = "TEAM1",
          convictionNumber = 1,
        ),
        UnallocatedCaseEntity(
          null,
          "Andrei Edwards",
          "J680648",
          "A",
          provisionalTier = false,
          providerCode = "",
          teamCode = "TEAM1",
          convictionNumber = 2,
        ),
        UnallocatedCaseEntity(
          null,
          "William Jones",
          "X4565764",
          "C",
          provisionalTier = false,
          providerCode = "",
          teamCode = "TEAM1",
          convictionNumber = 3,
        ),
        previouslyManagedCase,
        UnallocatedCaseEntity(
          null,
          "Dylan Adam Armstrong",
          "J678910",
          "C",
          provisionalTier = false,
          providerCode = "",
          teamCode = "TEAM2",
          convictionNumber = 5,
        ),
        UnallocatedCaseEntity(
          null,
          "Jim Doe",
          "C3333333",
          "B",
          provisionalTier = false,
          providerCode = "",
          teamCode = "TEAM3",
          convictionNumber = 6,
        ),
        UnallocatedCaseEntity(
          null,
          "Jane Doe",
          "X6666222",
          "C",
          provisionalTier = false,
          providerCode = "",
          teamCode = "TEAM1",
          convictionNumber = 1,
        ),
        laoCase1,
        laoCase2,
      ),
    )
  }

  fun insertCase(unallocatedCase: UnallocatedCaseEntity) {
    repository.save(unallocatedCase)
  }

  @BeforeEach
  fun `clear queues and database`() {
    repository.deleteAll()
    tierCalculationSqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(tierCalculationQueue.queueUrl).build())
    hmppsOffenderSqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(hmppsOffenderQueue.queueUrl).build())
    hmppsOffenderSqsDlqClient?.purgeQueue(PurgeQueueRequest.builder().queueUrl(hmppsOffenderQueue.dlqUrl).build())
    justRun { telemetryClient.trackEvent(any(), any(), any()) }
  }

  private val tierCalculationQueue by lazy {
    hmppsQueueService.findByQueueId("tiercalculationqueue")
      ?: throw MissingQueueException("HmppsQueue tiercalculationqueue not found")
  }
  private val hmppsOffenderQueue by lazy {
    hmppsQueueService.findByQueueId("hmppsoffenderqueue")
      ?: throw MissingQueueException("HmppsQueue hmppsoffenderqueue not found")
  }

  private val hmppsDomainTopic by lazy {
    hmppsQueueService.findByTopicId("hmppsdomaintopic")
      ?: throw MissingQueueException("HmppsTopic hmppsdomaintopic not found")
  }
  private val hmppsOffenderTopic by lazy {
    hmppsQueueService.findByTopicId("hmppsoffendertopic")
      ?: throw MissingQueueException("HmppsTopic hmppsoffendertopic not found")
  }

  private val hmppsOffenderSqsDlqClient by lazy { hmppsOffenderQueue.sqsDlqClient }

  protected val tierCalculationSqsClient by lazy { tierCalculationQueue.sqsClient }
  protected val hmppsOffenderSqsClient by lazy { hmppsOffenderQueue.sqsClient }

  protected val hmppsDomainSnsClient by lazy { hmppsDomainTopic.snsClient }
  protected val hmppsDomainTopicArn by lazy { hmppsDomainTopic.arn }

  protected val hmppsOffenderSnsClient by lazy { hmppsOffenderTopic.snsClient }
  protected val hmppsOffenderTopicArn by lazy { hmppsOffenderTopic.arn }

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var objectMapper: ObjectMapper

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  protected lateinit var repository: UnallocatedCasesRepository

  @Autowired
  protected lateinit var savedEmailRepository: SavedEmailsRepository

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @MockkBean
  lateinit var telemetryClient: TelemetryClient

  internal fun HttpHeaders.authToken(roles: List<String> = emptyList()) {
    this.setBearerAuth(
      jwtAuthHelper.createJwt(
        subject = "SOME_USER",
        roles = roles,
        clientId = "some-client",
        userName = "TomJones",
        name = "Tom Jones",
        authSource = "delius",

      ),
    )
  }

  protected fun publishConvictionChangedMessage(crn: String) {
    hmppsOffenderSnsClient
      .publish(
        PublishRequest.builder()
          .topicArn(hmppsOffenderTopicArn)
          .message(jsonString(offenderEvent(crn)))
          .messageAttributes(
            mapOf(
              "eventType" to MessageAttributeValue.builder()
                .dataType("String")
                .stringValue("CONVICTION_CHANGED")
                .build(),
            ),
          ).build(),
      )
  }

  protected fun countMessagesOnOffenderEventQueue(): Int = hmppsOffenderSqsClient.countAllMessagesOnQueue(hmppsOffenderQueue.queueUrl).get()

  protected fun countMessagesOnOffenderEventDeadLetterQueue(): Int? = hmppsOffenderSqsDlqClient?.countAllMessagesOnQueue(hmppsOffenderQueue.dlqUrl!!)?.get()

  protected fun jsonString(any: Any) = objectMapper.writeValueAsString(any) as String

  protected fun offenderEvent(crn: String) = HmppsOffenderEvent(crn)

  protected fun tierCalculationEvent(crn: String) = CalculationEventData(
    PersonReference(listOf(PersonReferenceType("CRN", crn))),
  )

  private fun getNumberOfMessagesCurrentlyOnQueue(client: SqsAsyncClient, queueUrl: String): Int? = client.countAllMessagesOnQueue(queueUrl).get()

  protected fun whenCalculationQueueIsEmpty() {
    await untilCallTo {
      getNumberOfMessagesCurrentlyOnQueue(
        tierCalculationSqsClient,
        tierCalculationQueue.queueUrl,
      )
    } matches { it == 0 }
  }

  protected fun whenCalculationMessageHasBeenProcessed() {
    await untilCallTo {
      getNumberOfMessagesCurrentlyOnQueue(
        tierCalculationSqsClient,
        tierCalculationQueue.queueUrl,
      )
    } matches { it == 0 }
  }
}
