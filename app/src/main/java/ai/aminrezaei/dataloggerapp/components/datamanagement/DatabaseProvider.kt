package ai.aminrezaei.dataloggerapp.components.datamanagement

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var database: AppDatabase? = null

    fun init(context: Context) {
        if (database == null) {
            synchronized(this) {
                if (database == null) {
                    normalizeLegacyDbVersionIfNeeded(context.applicationContext)
                    database = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        AppDatabase.DATABASE_NAME
                    ).addMigrations(DatabaseMigrations.MIGRATION_1_2).build()
                }
            }
        }
    }

    fun getDatabase(): AppDatabase {
        return database ?: error("DatabaseProvider.init(context) must be called before getDatabase()")
    }

    private fun normalizeLegacyDbVersionIfNeeded(context: Context) {
        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        if (!dbFile.exists()) {
            return
        }

        val sqliteDb = SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE
        )

        sqliteDb.use { db ->
            val hasLocationDataTable = hasTable(db, "LocationData")
            val hasNewLocationTable = hasTable(db, "location")

            // Legacy SQLiteOpenHelper path used version=7 with table LocationData.
            // Force it to version=1 so Room can apply migration(1,2).
            if (db.version > 2 && hasLocationDataTable && !hasNewLocationTable) {
                Log.w(
                    "DatabaseProvider",
                    "Legacy DB version ${db.version} detected; normalizing to v1 for Room migration"
                )
                db.version = 1
            }
        }
    }

    private fun hasTable(db: SQLiteDatabase, tableName: String): Boolean {
        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(tableName)
        )
        cursor.use {
            return it.moveToFirst()
        }
    }
}
