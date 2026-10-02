package ai.aminrezaei.dataloggerapp.components.datamanagement

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            val hasLocationData = hasTable(database, "LocationData")

            if (hasLocationData) {
                database.execSQL("ALTER TABLE LocationData RENAME TO location_legacy")
            }

            createTargetTables(database)

            if (hasTable(database, "location_legacy")) {
                database.execSQL(
                    """
                    INSERT INTO location (
                        timestamp, elapsed_realtime_nanos, elapsed_realtime_uncertainty_ns,
                        latitude, longitude, altitude,
                        accuracy, vertical_accuracy,
                        speed, speed_accuracy,
                        bearing, bearing_accuracy,
                        provider, is_fresh_fix
                    )
                    SELECT
                        timestamp, elapsedRealtimeNanos, elapsedRealtimeUncertaintyNanos,
                        latitude, longitude, altitude,
                        accuracy, verticalAccuracy,
                        speed, speedAccuracy,
                        bearing, bearingAccuracy,
                        provider, 0
                    FROM location_legacy
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    INSERT INTO device_state (
                        timestamp, trigger,
                        battery_pct, charging_state, charge_type,
                        screen_on, screen_locked, unlock_count,
                        wifi_connected, signal_bars,
                        activity_type, activity_confidence,
                        steps_since_boot, device_uptime_ms
                    )
                    SELECT
                        timestamp,
                        'migrated',
                        batteryPercentage,
                        CASE
                            WHEN chargeMode = 'On Battery' THEN 'discharging'
                            WHEN chargeMode = 'Full' THEN 'full'
                            WHEN chargeMode = 'Not Charging' THEN 'not_charging'
                            WHEN chargeMode LIKE 'Charging%' THEN 'charging'
                            ELSE 'not_charging'
                        END,
                        CASE
                            WHEN chargeMode LIKE '%USB%' THEN 'usb'
                            WHEN chargeMode LIKE '%AC%' THEN 'ac'
                            WHEN chargeMode LIKE '%Wireless%' THEN 'wireless'
                            ELSE 'none'
                        END,
                        CASE screenStatus WHEN 'On' THEN 1 ELSE 0 END,
                        CASE lockUnlockStatus WHEN 'Locked' THEN 1 ELSE 0 END,
                        lockUnlockCount,
                        wifiStatus,
                        signalStrength,
                        'UNKNOWN',
                        0,
                        stepsCount,
                        deviceUptime
                    FROM location_legacy
                    """.trimIndent()
                )
            }
        }
    }

    private fun createTargetTables(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS location (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                timestamp INTEGER NOT NULL,
                elapsed_realtime_nanos INTEGER,
                elapsed_realtime_uncertainty_ns REAL,
                latitude REAL,
                longitude REAL,
                altitude REAL,
                accuracy REAL,
                vertical_accuracy REAL,
                speed REAL,
                speed_accuracy REAL,
                bearing REAL,
                bearing_accuracy REAL,
                provider TEXT,
                is_fresh_fix INTEGER
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS accelerometer (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                timestamp INTEGER NOT NULL,
                elapsed_ns INTEGER NOT NULL,
                x REAL NOT NULL,
                y REAL NOT NULL,
                z REAL NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_acc_timestamp ON accelerometer(timestamp)"
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS device_state (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                timestamp INTEGER NOT NULL,
                trigger TEXT NOT NULL,
                battery_pct INTEGER,
                charging_state TEXT,
                charge_type TEXT,
                screen_on INTEGER,
                screen_locked INTEGER,
                unlock_count INTEGER,
                wifi_connected INTEGER,
                wifi_network_hash TEXT,
                signal_dbm INTEGER,
                signal_bars INTEGER,
                ringer_mode TEXT,
                audio_output TEXT,
                light_lux REAL,
                proximity_near INTEGER,
                activity_type TEXT,
                activity_confidence INTEGER,
                steps_since_boot INTEGER,
                device_uptime_ms INTEGER
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ema_responses (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                prompt_timestamp INTEGER NOT NULL,
                response_timestamp INTEGER,
                activity_label TEXT,
                location_label TEXT,
                social_label TEXT,
                optional_tags TEXT,
                latency_seconds INTEGER,
                dismissed INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS upload_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                upload_timestamp INTEGER NOT NULL,
                table_name TEXT NOT NULL,
                rows_uploaded INTEGER,
                min_row_id INTEGER,
                max_row_id INTEGER,
                min_timestamp INTEGER,
                max_timestamp INTEGER,
                success INTEGER NOT NULL DEFAULT 0,
                error_message TEXT
            )
            """.trimIndent()
        )
    }

    private fun hasTable(database: SupportSQLiteDatabase, tableName: String): Boolean {
        val cursor = database.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(tableName)
        )
        cursor.use {
            return it.moveToFirst()
        }
    }
}
