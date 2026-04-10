package uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.SavedEmailsEntity

@Repository
interface SavedEmailsRepository : CrudRepository<SavedEmailsEntity, Long> {

  fun existsByUserId(userId: String): Boolean

  fun existsByUserIdAndSavedEmail(userId: String, savedEmail: String): Boolean

  fun findByUserId(userId: String): List<SavedEmailsEntity>

  fun findByUserIdAndSavedEmail(userId: String, savedEmail: String): SavedEmailsEntity?
}
