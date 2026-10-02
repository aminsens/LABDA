package ai.aminrezaei.dataloggerapp.components.datamanagement

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EmaResponseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: EmaResponseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<EmaResponseEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM ema_responses")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM ema_responses WHERE response_timestamp IS NOT NULL")
    suspend fun getAnsweredCount(): Int

    @Query("SELECT * FROM ema_responses WHERE id BETWEEN :minId AND :maxId ORDER BY id ASC")
    suspend fun getRange(minId: Long, maxId: Long): List<EmaResponseEntity>

    @Query("DELETE FROM ema_responses WHERE id BETWEEN :minId AND :maxId")
    suspend fun deleteRange(minId: Long, maxId: Long)

    @Query("SELECT * FROM ema_responses ORDER BY id ASC")
    suspend fun getAll(): List<EmaResponseEntity>

    @Query("SELECT * FROM ema_responses WHERE id = :id")
    suspend fun getById(id: Long): EmaResponseEntity?

    /**
     * Returns the most recent prompt that is still awaiting a response and has not
     * been dismissed, or null if there is no active prompt. Used to avoid stacking
     * multiple unanswered prompts.
     */
    @Query(
        """
        SELECT * FROM ema_responses
        WHERE response_timestamp IS NULL AND dismissed = 0
        ORDER BY prompt_timestamp DESC
        LIMIT 1
        """
    )
    suspend fun getActivePrompt(): EmaResponseEntity?

    @Query("""
        UPDATE ema_responses
        SET response_timestamp = :responseTs,
            activity_label     = :activity,
            location_label     = :location,
            social_label       = :social,
            optional_tags      = :tags,
            latency_seconds    = :latency
        WHERE id = :id
    """)
    suspend fun updateResponse(
        id: Long,
        responseTs: Long,
        activity: String?,
        location: String?,
        social: String?,
        tags: String?,
        latency: Int
    )

    @Query("UPDATE ema_responses SET dismissed = 1 WHERE id = :id")
    suspend fun markDismissed(id: Long)
}
