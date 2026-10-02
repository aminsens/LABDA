package ai.aminrezaei.dataloggerapp.components.datamanagement

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        LocationEntity::class,
        AccelerometerEntity::class,
        DeviceStateEntity::class,
        EmaResponseEntity::class,
        UploadLogEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun accelerometerDao(): AccelerometerDao
    abstract fun deviceStateDao(): DeviceStateDao
    abstract fun emaResponseDao(): EmaResponseDao
    abstract fun uploadLogDao(): UploadLogDao

    companion object {
        const val DATABASE_NAME = "DataLoggerApp_Dev.db"
    }
}
