package ai.aminrezaei.dataloggerapp.components.datamanagement

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accelerometer",
    indices = [Index(value = ["timestamp"], name = "idx_acc_timestamp")]
)
data class AccelerometerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "elapsed_ns")
    val elapsedNs: Long,
    @ColumnInfo(name = "x")
    val x: Double,
    @ColumnInfo(name = "y")
    val y: Double,
    @ColumnInfo(name = "z")
    val z: Double
)
