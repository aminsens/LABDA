package ai.aminrezaei.dataloggerapp.components.datamanagement

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LocationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<LocationEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM location")
    suspend fun getCount(): Int

    @Query("SELECT * FROM location WHERE id BETWEEN :minId AND :maxId ORDER BY id ASC")
    suspend fun getRange(minId: Long, maxId: Long): List<LocationEntity>

    @Query("DELETE FROM location WHERE id BETWEEN :minId AND :maxId")
    suspend fun deleteRange(minId: Long, maxId: Long)

    @Query("SELECT * FROM location ORDER BY id ASC")
    suspend fun getAll(): List<LocationEntity>
}
