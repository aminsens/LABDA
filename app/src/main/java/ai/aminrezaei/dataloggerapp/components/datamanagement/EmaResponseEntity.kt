package ai.aminrezaei.dataloggerapp.components.datamanagement

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ema_responses")
data class EmaResponseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "prompt_timestamp")
    val promptTimestamp: Long,
    @ColumnInfo(name = "response_timestamp")
    val responseTimestamp: Long?,
    @ColumnInfo(name = "activity_label")
    val activityLabel: String?,
    @ColumnInfo(name = "location_label")
    val locationLabel: String?,
    @ColumnInfo(name = "social_label")
    val socialLabel: String?,
    @ColumnInfo(name = "optional_tags")
    val optionalTags: String?,
    @ColumnInfo(name = "latency_seconds")
    val latencySeconds: Int?,
    @ColumnInfo(name = "dismissed")
    val dismissed: Int = 0
)
