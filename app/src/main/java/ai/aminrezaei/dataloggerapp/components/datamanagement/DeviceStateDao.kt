package ai.aminrezaei.dataloggerapp.components.datamanagement

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DeviceStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DeviceStateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<DeviceStateEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM device_state")
    suspend fun getCount(): Int

    @Query("SELECT * FROM device_state WHERE id BETWEEN :minId AND :maxId ORDER BY id ASC")
    suspend fun getRange(minId: Long, maxId: Long): List<DeviceStateEntity>

    @Query("DELETE FROM device_state WHERE id BETWEEN :minId AND :maxId")
    suspend fun deleteRange(minId: Long, maxId: Long)

    @Query("SELECT * FROM device_state ORDER BY id ASC")
    suspend fun getAll(): List<DeviceStateEntity>

    @Query("SELECT * FROM device_state ORDER BY id DESC LIMIT 1")
    suspend fun getLatest(): DeviceStateEntity?
}
