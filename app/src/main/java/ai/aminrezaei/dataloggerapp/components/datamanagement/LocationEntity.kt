package ai.aminrezaei.dataloggerapp.components.datamanagement

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "elapsed_realtime_nanos")
    val elapsedRealtimeNanos: Long?,
    @ColumnInfo(name = "elapsed_realtime_uncertainty_ns")
    val elapsedRealtimeUncertaintyNs: Double?,
    @ColumnInfo(name = "latitude")
    val latitude: Double?,
    @ColumnInfo(name = "longitude")
    val longitude: Double?,
    @ColumnInfo(name = "altitude")
    val altitude: Double?,
    @ColumnInfo(name = "accuracy")
    val accuracy: Double?,
    @ColumnInfo(name = "vertical_accuracy")
    val verticalAccuracy: Double?,
    @ColumnInfo(name = "speed")
    val speed: Double?,
    @ColumnInfo(name = "speed_accuracy")
    val speedAccuracy: Double?,
    @ColumnInfo(name = "bearing")
    val bearing: Double?,
    @ColumnInfo(name = "bearing_accuracy")
    val bearingAccuracy: Double?,
    @ColumnInfo(name = "provider")
    val provider: String?,
    @ColumnInfo(name = "is_fresh_fix")
    val isFreshFix: Int?
)
