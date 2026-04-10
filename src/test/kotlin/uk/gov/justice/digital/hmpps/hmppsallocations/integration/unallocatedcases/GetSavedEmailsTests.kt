package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.SavedEmailsEntity

class GetSavedEmailsTests : IntegrationTestBase() {

  @BeforeEach
  fun beforeEach() {
    savedEmailRepository.deleteAll()
  }

  @Test
  fun `get emails test`() {
    val userId = "TestID"
    savedEmailRepository.save(SavedEmailsEntity(userId = userId, savedEmail = "testemail@justice.gov.uk"))
    webTestClient.get().uri("/user/$userId/savedEmails")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange().expectStatus().isOk
      .expectBody()
      .json("""[ "testemail@justice.gov.uk" ]""")
  }

  @Test
  fun `save email test`() {
    webTestClient.post().uri("/user/savedEmails").contentType(MediaType.APPLICATION_JSON)
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .body(
        BodyInserters.fromValue(
          """{
        "userId": "TestID",
        "email": "testemail@justice.gov.uk"
        }""",
        ),
      ).exchange().expectStatus().isOk
    savedEmailRepository.findByUserId("TestID").let {
      assertThat(it).hasSize(1)
      assertThat(it[0].savedEmail).isEqualTo("testemail@justice.gov.uk")
    }
  }

  @Test
  fun `save duplicate email test`() {
    webTestClient.post().uri("/user/savedEmails").contentType(MediaType.APPLICATION_JSON)
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .body(
        BodyInserters.fromValue(
          """{
        "userId": "TestID",
        "email": "testemail@justice.gov.uk"
        }""",
        ),
      ).exchange().expectStatus().isOk
    webTestClient.post().uri("/user/savedEmails").contentType(MediaType.APPLICATION_JSON)
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .body(
        BodyInserters.fromValue(
          """{
        "userId": "TestID",
        "email": "testemail@justice.gov.uk"
        }""",
        ),
      ).exchange().expectStatus().isOk
    savedEmailRepository.findByUserId("TestID").let {
      assertThat(it).hasSize(1)
      assertThat(it[0].savedEmail).isEqualTo("testemail@justice.gov.uk")
    }
  }

  @Test
  fun `delete email test`() {
    val userId = "TestID"
    savedEmailRepository.save(SavedEmailsEntity(userId = userId, savedEmail = "testemail@justice.gov.uk"))

    webTestClient.method(HttpMethod.DELETE).uri("/user/savedEmails").contentType(MediaType.APPLICATION_JSON)
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .body(
        BodyInserters.fromValue(
          """{
        "userId": "TestID",
        "email": "testemail@justice.gov.uk"
        }""",
        ),
      ).exchange().expectStatus().isOk
    savedEmailRepository.findByUserId(userId).let {
      assertThat(it).hasSize(0)
    }
  }
}
