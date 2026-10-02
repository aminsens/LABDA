package ai.aminrezaei.dataloggerapp

import ai.aminrezaei.dataloggerapp.components.datamanagement.AppDatabase
import ai.aminrezaei.dataloggerapp.components.datamanagement.DatabaseMigrations
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import android.database.sqlite.SQLiteDatabase

@RunWith(AndroidJUnit4::class)
class RoomMigrationInstrumentedTest {

    private val testDbName = "room-migration-test.db"
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(testDbName)
        createLegacyDatabase(context.getDatabasePath(testDbName))

        db = Room.databaseBuilder(context, AppDatabase::class.java, testDbName)
            .allowMainThreadQueries()
            .addMigrations(DatabaseMigrations.MIGRATION_1_2)
            .build()

        // Force open/migration
        db.openHelper.writableDatabase
    }

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db.close()
        context.deleteDatabase(testDbName)
    }

    @Test
    fun migration_locationAndDeviceStateRowCounts_matchLegacyCount() = runBlocking {
        val legacyCount = queryInt("SELECT COUNT(*) FROM location_legacy")
        val locationCount = db.locationDao().getCount()
        val deviceStateCount = db.deviceStateDao().getCount()

        assertEquals(10, legacyCount)
        assertEquals(legacyCount, locationCount)
        assertEquals(legacyCount, deviceStateCount)
    }

    @Test
    fun migration_coordinates_preserved() = runBlocking {
        val first = db.locationDao().getAll().first()
        assertEquals(55.6601718, first.latitude ?: 0.0, 0.0000001)
        assertEquals(12.3925959, first.longitude ?: 0.0, 0.0000001)
    }

    @Test
    fun migration_chargeModeMappedToChargingState() = runBlocking {
        val migratedRows = db.deviceStateDao().getAll().filter { it.trigger == "migrated" }
        assertTrue(migratedRows.isNotEmpty())
        assertEquals("discharging", migratedRows.first().chargingState)
    }

    @Test
    fun migration_preservesLegacyTable() {
        assertEquals(1, queryInt("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='location_legacy'"))
        assertEquals(10, queryInt("SELECT COUNT(*) FROM location_legacy"))
    }

    private fun queryInt(sql: String): Int {
        val cursor = db.openHelper.readableDatabase.query(sql)
        cursor.use {
            if (!it.moveToFirst()) {
                return 0
            }
            return it.getInt(0)
        }
    }

    private fun createLegacyDatabase(dbFile: File) {
        dbFile.parentFile?.mkdirs()
        val sqliteDb = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        sqliteDb.use { db ->
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS LocationData (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp LONG,
                    latitude REAL,
                    longitude REAL,
                    accuracy REAL,
                    provider TEXT,
                    chargeMode TEXT,
                    batteryPercentage INTEGER,
                    deviceUptime LONG,
                    bearing REAL,
                    speed REAL,
                    altitude REAL,
                    verticalAccuracy REAL,
                    speedAccuracy REAL,
                    bearingAccuracy REAL,
                    elapsedRealtimeNanos LONG,
                    elapsedRealtimeUncertaintyNanos REAL,
                    stepsCount INTEGER,
                    wifiStatus INTEGER,
                    lockUnlockStatus TEXT,
                    lockUnlockCount INTEGER,
                    screenStatus TEXT,
                    signalStrength INTEGER
                )
                """.trimIndent()
            )

            repeat(10) { index ->
                val ts = 1_772_144_714_853L + index
                val lat = if (index == 0) 55.6601718 else 55.6601718 + (index * 0.00001)
                val lon = if (index == 0) 12.3925959 else 12.3925959 + (index * 0.00001)

                db.execSQL(
                    """
                    INSERT INTO LocationData (
                        timestamp,
                        latitude,
                        longitude,
                        accuracy,
                        provider,
                        chargeMode,
                        batteryPercentage,
                        deviceUptime,
                        bearing,
                        speed,
                        altitude,
                        verticalAccuracy,
                        speedAccuracy,
                        bearingAccuracy,
                        elapsedRealtimeNanos,
                        elapsedRealtimeUncertaintyNanos,
                        stepsCount,
                        wifiStatus,
                        lockUnlockStatus,
                        lockUnlockCount,
                        screenStatus,
                        signalStrength
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf(
                        ts,
                        lat,
                        lon,
                        20.0,
                        "fused",
                        "On Battery",
                        76,
                        123_456L + index,
                        0.0,
                        0.0,
                        50.0,
                        2.9,
                        1.5,
                        45.0,
                        9_000_000_000L + index,
                        0.0,
                        1_000 + index,
                        1,
                        "Locked",
                        index,
                        "On",
                        3
                    )
                )
            }

            db.version = 1
        }
    }
}
