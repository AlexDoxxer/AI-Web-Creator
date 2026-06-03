package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "webpages")
data class Webpage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val generatedHtml: String,
    val customStylesText: String = "", // custom CSS stylesheet or custom styling text
    val selectedStyleName: String = "Modern Slate", // style name found/selected
    val clicksCount: Int = 0,
    val isDeployed: Boolean = false,
    val port: Int = 8080,
    // Real-time server simulation metrics
    val cpuPercent: Double = 0.0,
    val ramUsageMB: Double = 0.0,
    val bandwidthKB: Double = 0.0,
    val mapsDataId: Int? = null, // linked google maps scrape data
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "maps_data")
data class MapsData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalUrl: String,
    val businessName: String,
    val rating: Double,
    val reviewsCount: Int,
    val address: String,
    val phone: String = "",
    val category: String = "",
    val website: String = "",
    val reviewsJson: String = "", // JSON string containing a list of reviews
    val businessTheme: String = "", // derived theme/vibe based on reviews e.g. "Warm Cozy", "Cyberpunk Modern"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "style_designs")
data class StyleDesign(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val query: String,
    val themeName: String,
    val primaryColor: String, // hex colors
    val secondaryColor: String,
    val backgroundColor: String,
    val surfaceColor: String,
    val fontStyle: String, // Serif, Sans-Serif, Monospace
    val cssTemplatesJson: String, // sample style blocks
    val ratingScore: Int = 5,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "model_settings")
data class ModelSetting(
    @PrimaryKey val id: Int = 1, // Single constant row
    val selectedProvider: String = "Gemini", // Gemini, TheOpenCode GO, OpenRouter, Custom
    val customApiKey: String = "",
    val customBaseUrl: String = "",
    val modelName: String = "gemini-3.5-flash",
    val systemGuidance: String = "You are an expert full stack UI builder. Create highly beautiful independent standalone single-page HTML files utilizing Tailwind CSS classes or custom styled custom variables. Always reply with clean HTML in your response.",
    val lastUpdated: Long = System.currentTimeMillis()
)
