package ai.aminrezaei.dataloggerapp.components.datamanagement

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "upload_log")
data class UploadLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "upload_timestamp")
    val uploadTimestamp: Long,
    @ColumnInfo(name = "table_name")
    val tableName: String,
    @ColumnInfo(name = "rows_uploaded")
    val rowsUploaded: Int?,
    @ColumnInfo(name = "min_row_id")
    val minRowId: Long?,
    @ColumnInfo(name = "max_row_id")
    val maxRowId: Long?,
    @ColumnInfo(name = "min_timestamp")
    val minTimestamp: Long?,
    @ColumnInfo(name = "max_timestamp")
    val maxTimestamp: Long?,
    @ColumnInfo(name = "success")
    val success: Int = 0,
    @ColumnInfo(name = "error_message")
    val errorMessage: String?
)
