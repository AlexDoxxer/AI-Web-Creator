package com.example.data.repository

import com.example.data.dao.MapsDataDao
import com.example.data.dao.ModelSettingDao
import com.example.data.dao.StyleDesignDao
import com.example.data.dao.WebpageDao
import com.example.data.entity.MapsData
import com.example.data.entity.ModelSetting
import com.example.data.entity.StyleDesign
import com.example.data.entity.Webpage
import com.example.data.network.NetworkService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class AppRepository(
    private val webpageDao: WebpageDao,
    private val mapsDataDao: MapsDataDao,
    private val styleDesignDao: StyleDesignDao,
    private val modelSettingDao: ModelSettingDao
) {
    // --- WEBPAGES ---
    val allWebpages: Flow<List<Webpage>> = webpageDao.getAllWebpages()

    fun getWebpageByIdFlow(id: Int): Flow<Webpage?> = webpageDao.getWebpageByIdFlow(id)

    suspend fun getWebpageById(id: Int): Webpage? = webpageDao.getWebpageById(id)

    suspend fun insertWebpage(webpage: Webpage): Long = webpageDao.insertWebpage(webpage)

    suspend fun updateWebpage(webpage: Webpage) = webpageDao.updateWebpage(webpage)

    suspend fun deleteWebpage(webpage: Webpage) = webpageDao.deleteWebpage(webpage)

    suspend fun incrementClicks(id: Int) = webpageDao.incrementClicks(id)

    suspend fun updateDeploymentState(
        id: Int,
        isDeployed: Boolean,
        cpu: Double,
        ram: Double,
        bandwidth: Double,
        port: Int
    ) = webpageDao.updateDeploymentState(id, isDeployed, cpu, ram, bandwidth, port)

    // --- MAPS DATA ---
    val allScrapedMapsData: Flow<List<MapsData>> = mapsDataDao.getAllScrapedMapsData()

    suspend fun insertMapsData(mapsData: MapsData): Long = mapsDataDao.insertMapsData(mapsData)

    suspend fun deleteMapsData(mapsData: MapsData) = mapsDataDao.deleteMapsData(mapsData)

    // --- STYLES (MCP SEARCH) ---
    val allStyleDesigns: Flow<List<StyleDesign>> = styleDesignDao.getAllStyleDesigns()

    fun searchStyles(query: String): Flow<List<StyleDesign>> = styleDesignDao.searchStyles(query)

    suspend fun insertStyle(style: StyleDesign): Long = styleDesignDao.insertStyle(style)

    suspend fun deleteStyleById(id: Int) = styleDesignDao.deleteStyleById(id)

    // --- SETTINGS ---
    val modelSettingsFlow: Flow<ModelSetting?> = modelSettingDao.getModelSettingsFlow()

    suspend fun getModelSettings(): ModelSetting? = modelSettingDao.getModelSettings()

    suspend fun updateModelSettings(setting: ModelSetting) = modelSettingDao.insertOrUpdateSettings(setting)

    // --- AI CONTENT GENERATION ---
    suspend fun generateWebpageFromAI(
        name: String,
        detailsPrompt: String,
        selectedStyle: StyleDesign?,
        mapsData: MapsData?
    ): Result<Webpage> {
        // Fetch current model setting or default
        val settings = getModelSettings() ?: ModelSetting()
        
        val styleInstructions = if (selectedStyle != null) {
            """
            Design instructions from selected Custom Style:
            - Theme: ${selectedStyle.themeName}
            - Colors: Primary ${selectedStyle.primaryColor}, Secondary ${selectedStyle.secondaryColor}, Background ${selectedStyle.backgroundColor}, Surface ${selectedStyle.surfaceColor}
            - Font constraints: ${selectedStyle.fontStyle}
            - Desired styling snippets: ${selectedStyle.cssTemplatesJson}
            Build elements strictly styled according to this theme and match these HEX colors!
            """.trimIndent()
        } else {
            "Default style: Modern, minimal, elegant Tailwind card layout or webpage."
        }

        val mapsInstructions = if (mapsData != null) {
            """
            Include business information from Google Maps:
            - Business Name: ${mapsData.businessName}
            - Category: ${mapsData.category}
            - Derived atmosphere theme: ${mapsData.businessTheme}
            - Address: ${mapsData.address}
            - Phone: ${mapsData.phone}
            - Website: ${mapsData.website}
            - Rating: ${mapsData.rating} ⭐ out of 5 (${mapsData.reviewsCount} reviews)
            Please integrate some realistic mock reviews from our scraped Google Maps data:
            ${mapsData.reviewsJson}
            """.trimIndent()
        } else {
            ""
        }

        val finalPrompt = """
            Create a highly polished, responsive Single Page HTML Website for: "$name".
            
            Description / Requirements specified by user:
            $detailsPrompt
            
            $styleInstructions
            
            $mapsInstructions
            
            Return ONLY a complete, beautiful HTML5 page starting with <!DOCTYPE html> and containing all required CSS (use standard CDN Tailwind CSS config, e.g. <script src="https://cdn.tailwindcss.com"></script> inside <head> is perfectly fine, and add custom embedded styles <style> for theme parameters like background, buttons, margins). Ensure spectacular typography, gorgeous margins, spacing, background designs, cards, a clear call-to-action button, and maybe visual review columns if maps details are supplied. The layout should look top-tier and expensive.
            DO NOT output markdown block wrappers (like ```html), just pure raw HTML text.
        """.trimIndent()

        val aiResult = NetworkService.generateWebpageContent(
            provider = settings.selectedProvider,
            apiKey = settings.customApiKey,
            baseUrl = settings.customBaseUrl,
            modelName = settings.modelName,
            systemGuidance = settings.systemGuidance,
            prompt = finalPrompt
        )

        return aiResult.map { html ->
            val webpage = Webpage(
                name = name,
                description = detailsPrompt,
                generatedHtml = html,
                selectedStyleName = selectedStyle?.themeName ?: "Aesthetic Default",
                customStylesText = selectedStyle?.cssTemplatesJson ?: "",
                mapsDataId = mapsData?.id
            )
            webpage
        }
    }
}
