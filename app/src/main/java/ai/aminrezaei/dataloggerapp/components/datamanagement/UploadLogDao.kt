package ai.aminrezaei.dataloggerapp.components.datamanagement

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UploadLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: UploadLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<UploadLogEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM upload_log")
    suspend fun getCount(): Int

    @Query("SELECT * FROM upload_log WHERE id BETWEEN :minId AND :maxId ORDER BY id ASC")
    suspend fun getRange(minId: Long, maxId: Long): List<UploadLogEntity>

    @Query("DELETE FROM upload_log WHERE id BETWEEN :minId AND :maxId")
    suspend fun deleteRange(minId: Long, maxId: Long)

    @Query("SELECT * FROM upload_log ORDER BY id ASC")
    suspend fun getAll(): List<UploadLogEntity>

    @Query("SELECT MAX(upload_timestamp) FROM upload_log")
    suspend fun getLastUploadTimestamp(): Long?

    @Query("SELECT MAX(upload_timestamp) FROM upload_log WHERE success = 1")
    suspend fun getLastSuccessfulTimestamp(): Long?

    @Query("SELECT * FROM upload_log ORDER BY id DESC LIMIT 1")
    suspend fun getLatestEntry(): UploadLogEntity?
}
