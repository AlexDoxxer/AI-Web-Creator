package com.example.data.dao

import androidx.room.*
import com.example.data.entity.MapsData
import com.example.data.entity.ModelSetting
import com.example.data.entity.StyleDesign
import com.example.data.entity.Webpage
import kotlinx.coroutines.flow.Flow

@Dao
interface WebpageDao {
    @Query("SELECT * FROM webpages ORDER BY createdAt DESC")
    fun getAllWebpages(): Flow<List<Webpage>>

    @Query("SELECT * FROM webpages WHERE id = :id")
    suspend fun getWebpageById(id: Int): Webpage?

    @Query("SELECT * FROM webpages WHERE id = :id")
    fun getWebpageByIdFlow(id: Int): Flow<Webpage?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWebpage(webpage: Webpage): Long

    @Update
    suspend fun updateWebpage(webpage: Webpage)

    @Delete
    suspend fun deleteWebpage(webpage: Webpage)

    @Query("UPDATE webpages SET clicksCount = clicksCount + 1 WHERE id = :id")
    suspend fun incrementClicks(id: Int)

    @Query("UPDATE webpages SET isDeployed = :isDeployed, cpuPercent = :cpu, ramUsageMB = :ram, bandwidthKB = :bandwidth, port = :port WHERE id = :id")
    suspend fun updateDeploymentState(id: Int, isDeployed: Boolean, cpu: Double, ram: Double, bandwidth: Double, port: Int)
}

@Dao
interface MapsDataDao {
    @Query("SELECT * FROM maps_data ORDER BY createdAt DESC")
    fun getAllScrapedMapsData(): Flow<List<MapsData>>

    @Query("SELECT * FROM maps_data WHERE id = :id")
    suspend fun getMapsDataById(id: Int): MapsData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapsData(mapsData: MapsData): Long

    @Delete
    suspend fun deleteMapsData(mapsData: MapsData)
}

@Dao
interface StyleDesignDao {
    @Query("SELECT * FROM style_designs ORDER BY createdAt DESC")
    fun getAllStyleDesigns(): Flow<List<StyleDesign>>

    @Query("SELECT * FROM style_designs WHERE query LIKE '%' || :searchQuery || '%' OR themeName LIKE '%' || :searchQuery || '%'")
    fun searchStyles(searchQuery: String): Flow<List<StyleDesign>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStyle(style: StyleDesign): Long

    @Query("DELETE FROM style_designs WHERE id = :id")
    suspend fun deleteStyleById(id: Int)
}

@Dao
interface ModelSettingDao {
    @Query("SELECT * FROM model_settings WHERE id = 1")
    fun getModelSettingsFlow(): Flow<ModelSetting?>

    @Query("SELECT * FROM model_settings WHERE id = 1")
    suspend fun getModelSettings(): ModelSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(setting: ModelSetting)
}
