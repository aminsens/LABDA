package ai.aminrezaei.dataloggerapp

import ai.aminrezaei.dataloggerapp.components.datamanagement.AccelerometerDao
import ai.aminrezaei.dataloggerapp.components.datamanagement.AccelerometerEntity
import ai.aminrezaei.dataloggerapp.components.datamanagement.AppDatabase
import ai.aminrezaei.dataloggerapp.components.datamanagement.DeviceStateDao
import ai.aminrezaei.dataloggerapp.components.datamanagement.DeviceStateEntity
import ai.aminrezaei.dataloggerapp.components.datamanagement.EmaResponseDao
import ai.aminrezaei.dataloggerapp.components.datamanagement.EmaResponseEntity
import ai.aminrezaei.dataloggerapp.components.datamanagement.LocationDao
import ai.aminrezaei.dataloggerapp.components.datamanagement.LocationEntity
import ai.aminrezaei.dataloggerapp.components.datamanagement.UploadLogDao
import ai.aminrezaei.dataloggerapp.components.datamanagement.UploadLogEntity
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDaoInstrumentedTest {

    private lateinit var db: AppDatabase
    private lateinit var locationDao: LocationDao
    private lateinit var accelerometerDao: AccelerometerDao
    private lateinit var deviceStateDao: DeviceStateDao
    private lateinit var emaResponseDao: EmaResponseDao
    private lateinit var uploadLogDao: UploadLogDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        locationDao = db.locationDao()
        accelerometerDao = db.accelerometerDao()
        deviceStateDao = db.deviceStateDao()
        emaResponseDao = db.emaResponseDao()
        uploadLogDao = db.uploadLogDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndRetrieve_location() = runBlocking {
        val entity = LocationEntity(
            timestamp = 1_700_000_000_000L,
            elapsedRealtimeNanos = 123_456_789L,
            elapsedRealtimeUncertaintyNs = 0.0,
            latitude = 55.6601718,
            longitude = 12.3925959,
            altitude = 51.6,
            accuracy = 20.0,
            verticalAccuracy = 2.9,
            speed = 0.0,
            speedAccuracy = 1.5,
            bearing = 0.0,
            bearingAccuracy = 45.0,
            provider = "fused",
            isFreshFix = 1
        )

        locationDao.insert(entity)
        val rows = locationDao.getAll()

        assertEquals(1, rows.size)
        assertEquals(55.6601718, rows[0].latitude ?: 0.0, 0.0000001)
        assertEquals(1, rows[0].isFreshFix)
    }

    @Test
    fun insertAndRetrieve_accelerometer() = runBlocking {
        val entity = AccelerometerEntity(
            timestamp = 1_700_000_000_000L,
            elapsedNs = 100_000_000L,
            x = 0.1,
            y = 0.2,
            z = 9.8
        )

        accelerometerDao.insert(entity)
        val rows = accelerometerDao.getAll()

        assertEquals(1, rows.size)
        assertEquals(9.8, rows[0].z, 0.0001)
    }

    @Test
    fun insertAndRetrieve_deviceState() = runBlocking {
        val entity = DeviceStateEntity(
            timestamp = 1_700_000_000_000L,
            trigger = "periodic",
            batteryPct = 88,
            chargingState = "discharging",
            chargeType = "none",
            screenOn = 1,
            screenLocked = 0,
            unlockCount = 4,
            wifiConnected = 1,
            wifiNetworkHash = "deadbeef",
            signalDbm = -72,
            signalBars = 4,
            ringerMode = "normal",
            audioOutput = "speaker",
            lightLux = 123.0,
            proximityNear = 0,
            activityType = "STILL",
            activityConfidence = 90,
            stepsSinceBoot = 101,
            deviceUptimeMs = 99_999L
        )

        deviceStateDao.insert(entity)
        val latest = deviceStateDao.getLatest()

        assertEquals("periodic", latest?.trigger)
        assertEquals("deadbeef", latest?.wifiNetworkHash)
        assertEquals(-72, latest?.signalDbm)
    }

    @Test
    fun insertAndRetrieve_emaResponses() = runBlocking {
        val entity = EmaResponseEntity(
            promptTimestamp = 1_700_000_000_000L,
            responseTimestamp = 1_700_000_000_050L,
            activityLabel = "Sedentary",
            locationLabel = "Home",
            socialLabel = "Alone",
            optionalTags = "[]",
            latencySeconds = 50,
            dismissed = 0
        )

        emaResponseDao.insert(entity)
        val rows = emaResponseDao.getAll()

        assertEquals(1, rows.size)
        assertEquals("Home", rows[0].locationLabel)
    }

    @Test
    fun insertAndRetrieve_uploadLog() = runBlocking {
        val entity = UploadLogEntity(
            uploadTimestamp = 1_700_000_001_000L,
            tableName = "accelerometer",
            rowsUploaded = 50,
            minRowId = 1,
            maxRowId = 50,
            minTimestamp = 1_700_000_000_000L,
            maxTimestamp = 1_700_000_001_999L,
            success = 1,
            errorMessage = null
        )

        uploadLogDao.insert(entity)
        val rows = uploadLogDao.getAll()

        assertEquals(1, rows.size)
        assertEquals("accelerometer", rows[0].tableName)
        assertEquals(1, rows[0].success)
    }

    @Test
    fun batchInsert_accelerometer_producesCorrectCount() = runBlocking {
        val batch = (1..50).map { i ->
            AccelerometerEntity(
                timestamp = 1_700_000_000_000L + (i * 40L),
                elapsedNs = 100_000_000L + (i * 40_000_000L),
                x = 0.1 * i,
                y = 0.2 * i,
                z = 9.8
            )
        }

        accelerometerDao.insertAll(batch)

        assertEquals(50, accelerometerDao.getCount())
    }

    @Test
    fun rangeQuery_accelerometer_returnsOnlyRequestedRange() = runBlocking {
        val batch = (1..100).map { i ->
            AccelerometerEntity(
                timestamp = 1_700_000_000_000L + i,
                elapsedNs = i.toLong(),
                x = 0.0,
                y = 0.0,
                z = 9.8
            )
        }
        accelerometerDao.insertAll(batch)

        val range = accelerometerDao.getRange(minId = 25, maxId = 74)

        assertEquals(50, range.size)
        assertEquals(25L, range.first().id)
        assertEquals(74L, range.last().id)
    }

    @Test
    fun deleteRange_removesOnlyTargetRows() = runBlocking {
        val batch = (1..100).map { i ->
            AccelerometerEntity(
                timestamp = 1_700_000_000_000L + i,
                elapsedNs = i.toLong(),
                x = 0.0,
                y = 0.0,
                z = 9.8
            )
        }
        accelerometerDao.insertAll(batch)

        accelerometerDao.deleteRange(minId = 1, maxId = 50)

        assertEquals(50, accelerometerDao.getCount())
        val remaining = accelerometerDao.getAll()
        assertEquals(51L, remaining.first().id)
    }

    @Test
    fun ema_nullResponseTimestamp_isValid() = runBlocking {
        val entity = EmaResponseEntity(
            promptTimestamp = 1_700_000_000_000L,
            responseTimestamp = null,
            activityLabel = null,
            locationLabel = null,
            socialLabel = null,
            optionalTags = "[]",
            latencySeconds = null,
            dismissed = 0
        )

        emaResponseDao.insert(entity)
        val rows = emaResponseDao.getAll()

        assertEquals(1, rows.size)
        assertNull(rows[0].responseTimestamp)
        assertEquals(0, rows[0].dismissed)
    }

    @Test
    fun wifiHashShape_deviceStateRowUsesShortHex() = runBlocking {
        val entity = DeviceStateEntity(
            timestamp = 1_700_000_000_000L,
            trigger = "wifi_change",
            batteryPct = 50,
            chargingState = "charging",
            chargeType = "usb",
            screenOn = 1,
            screenLocked = 0,
            unlockCount = 1,
            wifiConnected = 1,
            wifiNetworkHash = "a1b2c3d4",
            signalDbm = -80,
            signalBars = 3,
            ringerMode = "normal",
            audioOutput = "speaker",
            lightLux = null,
            proximityNear = null,
            activityType = "UNKNOWN",
            activityConfidence = 0,
            stepsSinceBoot = 10,
            deviceUptimeMs = 1_000L
        )

        deviceStateDao.insert(entity)
        val latest = deviceStateDao.getLatest() ?: error("Missing row")

        assertTrue(latest.wifiNetworkHash?.matches(Regex("[0-9a-f]{8}")) == true)
    }
}
