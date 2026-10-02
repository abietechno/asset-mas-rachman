package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.AssetEntity
import com.example.model.CategoryEntity

@Database(entities = [AssetEntity::class, CategoryEntity::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun assetDao(): AssetDao

    companion object {
        /**
         * Menambah kolom sinkronisasi tanpa menghapus data. Baris lama ditandai LOCAL_ONLY (3)
         * supaya data contoh/uji tidak terkirim ke server; setiap baris diberi UUID unik.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE assets ADD COLUMN serverId INTEGER")
                db.execSQL("ALTER TABLE assets ADD COLUMN clientUuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE assets ADD COLUMN syncState INTEGER NOT NULL DEFAULT 3")
                db.execSQL("ALTER TABLE assets ADD COLUMN serverVersion INTEGER")
                db.execSQL("UPDATE assets SET clientUuid = lower(hex(randomblob(16)))")
            }
        }

        /** Kategori dari dashboard + kolom categoryId pada aset. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE assets ADD COLUMN categoryId INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, " +
                        "`kind` TEXT NOT NULL, `usefulLifeYears` INTEGER, `isSystem` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        /** Pin lokasi aset. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE assets ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE assets ADD COLUMN longitude REAL")
                db.execSQL("ALTER TABLE assets ADD COLUMN locationAccuracyM REAL")
                db.execSQL("ALTER TABLE assets ADD COLUMN locationCapturedAt INTEGER")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "company_assets.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
