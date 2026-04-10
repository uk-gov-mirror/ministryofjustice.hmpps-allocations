package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.SavedEmailRequest
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.SavedEmailsEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.SavedEmailsRepository

@Service
class SavedEmailService(
  private val repository: SavedEmailsRepository,
) {

  suspend fun getSavedEmails(userId: String): List<String> = repository.findByUserId(userId).map { it.savedEmail }

  suspend fun saveEmail(savedEmailRequest: SavedEmailRequest) {
    if (!repository.existsByUserIdAndSavedEmail(savedEmailRequest.userId, savedEmailRequest.email)) {
      repository.save(SavedEmailsEntity(userId = savedEmailRequest.userId, savedEmail = savedEmailRequest.email))
    }
  }

  suspend fun deleteSavedEmail(savedEmailRequest: SavedEmailRequest) {
    repository.findByUserIdAndSavedEmail(savedEmailRequest.userId, savedEmailRequest.email)?.let {
      repository.delete(it)
    }
  }
}
