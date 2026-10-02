package ai.aminrezaei.dataloggerapp.components.datamanagement

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AccelerometerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AccelerometerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<AccelerometerEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM accelerometer")
    suspend fun getCount(): Int

    @Query("SELECT * FROM accelerometer WHERE id BETWEEN :minId AND :maxId ORDER BY id ASC")
    suspend fun getRange(minId: Long, maxId: Long): List<AccelerometerEntity>

    @Query("DELETE FROM accelerometer WHERE id BETWEEN :minId AND :maxId")
    suspend fun deleteRange(minId: Long, maxId: Long)

    @Query("SELECT * FROM accelerometer ORDER BY id ASC")
    suspend fun getAll(): List<AccelerometerEntity>
}
