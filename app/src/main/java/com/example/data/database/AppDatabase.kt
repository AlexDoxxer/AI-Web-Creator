package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.MapsDataDao
import com.example.data.dao.ModelSettingDao
import com.example.data.dao.StyleDesignDao
import com.example.data.dao.WebpageDao
import com.example.data.entity.MapsData
import com.example.data.entity.ModelSetting
import com.example.data.entity.StyleDesign
import com.example.data.entity.Webpage

@Database(
    entities = [Webpage::class, MapsData::class, StyleDesign::class, ModelSetting::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun webpageDao(): WebpageDao
    abstract fun mapsDataDao(): MapsDataDao
    abstract fun styleDesignDao(): StyleDesignDao
    abstract fun modelSettingDao(): ModelSettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "webpage_creator_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
