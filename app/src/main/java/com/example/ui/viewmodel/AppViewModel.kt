package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entity.MapsData
import com.example.data.entity.ModelSetting
import com.example.data.entity.StyleDesign
import com.example.data.entity.Webpage
import com.example.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    // Reactive streams from DB
    val webpages: StateFlow<List<Webpage>>
    val scrapedMaps: StateFlow<List<MapsData>>
    val styleDesigns: StateFlow<List<StyleDesign>>
    val modelSettings: StateFlow<ModelSetting>

    // UI Interactive States
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _generationStatus = MutableStateFlow("")
    val generationStatus: StateFlow<String> = _generationStatus

    private val _isScraping = MutableStateFlow(false)
    val isScraping: StateFlow<Boolean> = _isScraping

    private val _scrapingStatus = MutableStateFlow("")
    val scrapingStatus: StateFlow<String> = _scrapingStatus

    // Filter/Search queries
    private val _styleSearchQuery = MutableStateFlow("")
    val styleSearchQuery: StateFlow<String> = _styleSearchQuery

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(
            webpageDao = database.webpageDao(),
            mapsDataDao = database.mapsDataDao(),
            styleDesignDao = database.styleDesignDao(),
            modelSettingDao = database.modelSettingDao()
        )

        // Bind StateFlows with standard sharing bounds
        webpages = repository.allWebpages
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        scrapedMaps = repository.allScrapedMapsData
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        modelSettings = repository.modelSettingsFlow
            .map { it ?: ModelSetting() }
            .stateIn(viewModelScope, SharingStarted.Lazily, ModelSetting())

        // Setup style search flow
        styleDesigns = _styleSearchQuery
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    repository.allStyleDesigns
                } else {
                    repository.searchStyles(query)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Initialize seed data and mock monitoring
        viewModelScope.launch(Dispatchers.IO) {
            preseedStyles()
            preseedModelSettings()
            preseedSampleWebpageIfNeeded()
            startResourceMonitoringLoop()
        }
    }

    fun setStyleSearchQuery(query: String) {
        _styleSearchQuery.value = query
    }

    // --- SEED SEED DATA ---
    private suspend fun preseedModelSettings() {
        val current = repository.getModelSettings()
        if (current == null) {
            repository.updateModelSettings(ModelSetting())
        }
    }

    private suspend fun preseedSampleWebpageIfNeeded() {
        val currentWebpages = repository.allWebpages.firstOrNull() ?: emptyList()
        if (currentWebpages.isEmpty()) {
            val sampleHtml = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>The Rusty Anchor Bakery</title>
                <script src="https://cdn.tailwindcss.com"></script>
                <style>
                    body { font-family: 'Georgia', serif; background-color: #FAF6F0; color: #4A3B32; }
                    .tint { border-color: #8C6239; }
                </style>
            </head>
            <body class="p-4 md:p-8">
                <div class="max-w-2xl mx-auto bg-white rounded-2xl shadow-xl overflow-hidden border tint border-t-8">
                    <header class="p-6 text-center bg-amber-50">
                        <span class="text-xs tracking-widest text-amber-800 font-bold uppercase">Local Favorite</span>
                        <h1 class="text-3xl md:text-4xl font-extrabold text-amber-900 mt-1">The Rusty Anchor Bakery</h1>
                        <p class="text-sm italic text-amber-700 mt-1">Warm hand-crafted pastries beside the harbour</p>
                    </header>
                    
                    <main class="p-6 space-y-6">
                        <div class="rounded-lg bg-orange-50/50 p-4 border border-orange-100">
                            <h2 class="font-bold text-amber-950">📍 Location & Hours</h2>
                            <p class="text-sm text-amber-900 mt-1">104 Coastal Rd, Ocean Breeze Village</p>
                            <p class="text-sm mt-1">⭐⭐⭐⭐⭐ Rating: 4.8 (85 Google Reviews)</p>
                        </div>
                        
                        <div>
                            <h2 class="font-bold text-amber-950 text-xl border-b pb-2">⭐ Highlight Reviews</h2>
                            <div class="space-y-4 mt-3">
                                <div class="bg-amber-50/30 p-3 rounded border">
                                    <p class="text-sm font-semibold">"Best sourdough in town..."</p>
                                    <p class="text-xs text-amber-700 mt-1">- Clara M., Local Guide</p>
                                </div>
                                <div class="bg-amber-50/30 p-3 rounded border">
                                    <p class="text-sm font-semibold">"Absolutely exquisite atmosphere. The warm cardamom buns are life-changing!"</p>
                                    <p class="text-xs text-amber-700 mt-1">- Julian S.</p>
                                </div>
                            </div>
                        </div>
                    </main>
                    
                    <footer class="bg-amber-900 p-4 text-center text-amber-100 text-xs text-white">
                        <p>© 2026 The Rusty Anchor Bakery. Deployments simulation active.</p>
                    </footer>
                </div>
            </body>
            </html>
            """.trimIndent()

            repository.insertWebpage(
                Webpage(
                    name = "Rusty Anchor Bakery",
                    description = "Charming harbour bakery landing page and coordinates.",
                    generatedHtml = sampleHtml,
                    selectedStyleName = "Warm Organic Coffee",
                    clicksCount = 42,
                    isDeployed = true,
                    port = 8001,
                    cpuPercent = 2.4,
                    ramUsageMB = 64.2,
                    bandwidthKB = 1.2
                )
            )
        }
    }

    private suspend fun preseedStyles() {
        val currentStyles = repository.allStyleDesigns.firstOrNull() ?: emptyList()
        if (currentStyles.isEmpty()) {
            val list = listOf(
                StyleDesign(
                    query = "warm organic coffee tea cafe cosy shop bakery vintage hearth bookstore",
                    themeName = "Warm Organic Coffee",
                    primaryColor = "#6F4E37",
                    secondaryColor = "#D17A22",
                    backgroundColor = "#FDFBF7",
                    surfaceColor = "#F5EBE0",
                    fontStyle = "Serif",
                    cssTemplatesJson = """{"borderRadius": "16px", "padding": "24px", "shadow": "0 10px 15px -3px rgba(111, 78, 55, 0.1)"}"""
                ),
                StyleDesign(
                    query = "neon cyberpunk studio software technical coders dark neon pink cyber",
                    themeName = "Neon Cyberpunk",
                    primaryColor = "#FF007F",
                    secondaryColor = "#00F0FF",
                    backgroundColor = "#0A0915",
                    surfaceColor = "#15122C",
                    fontStyle = "Monospace",
                    cssTemplatesJson = """{"borderRadius": "4px", "padding": "16px", "shadow": "0 0 15px rgba(255, 0, 127, 0.3)", "border": "1px solid #FF007F"}"""
                ),
                StyleDesign(
                    query = "minimal brutalist black white mono stark raw clean simplicity bold editorial",
                    themeName = "Minimal Stark Brutalist",
                    primaryColor = "#000000",
                    secondaryColor = "#FFFFFF",
                    backgroundColor = "#F3F3F3",
                    surfaceColor = "#FFFFFF",
                    fontStyle = "Sans-Serif",
                    cssTemplatesJson = """{"borderRadius": "0px", "padding": "20px", "border": "4px solid #000000", "shadow": "8px 8px 0px #000000"}"""
                ),
                StyleDesign(
                    query = "nordic corporate enterprise agency clean finance bank blue tech engineering",
                    themeName = "Nordic Digital",
                    primaryColor = "#102A43",
                    secondaryColor = "#486581",
                    backgroundColor = "#F0F4F8",
                    surfaceColor = "#FFFFFF",
                    fontStyle = "Sans-Serif",
                    cssTemplatesJson = """{"borderRadius": "12px", "padding": "28px", "shadow": "0 4px 6px -1px rgba(0,0,0,0.05)"}"""
                ),
                StyleDesign(
                    query = "creative playful colorful kid toy artist bakery cute candy yellow pastel",
                    themeName = "Playful Pastel Candy",
                    primaryColor = "#FF6B6B",
                    secondaryColor = "#FFD93D",
                    backgroundColor = "#FFF9E6",
                    surfaceColor = "#FFF0F5",
                    fontStyle = "Sans-Serif",
                    cssTemplatesJson = """{"borderRadius": "24px", "padding": "22px", "shadow": "0 20px 25px -5px rgba(255, 107, 107, 0.15)"}"""
                )
            )
            for (style in list) {
                repository.insertStyle(style)
            }
        }
    }

    // --- DYNAMIC CUSTOM STYLE GENERATION FOR PERSONALIZED SEARH (designmd MCP concept) ---
    suspend fun generateOrSearchStyle(searchQuery: String) {
        if (searchQuery.isBlank()) return
        // See if there's matches
        val results = repository.searchStyles(searchQuery).firstOrNull() ?: emptyList()
        if (results.isEmpty()) {
            // Generate a brand new style design specifically tailored to the user's terms!
            val randomHuePrimary = Random.nextInt(0, 360)
            val randomHueSecondary = (randomHuePrimary + 120) % 360
            
            val hsvPrimary = floatArrayOf(randomHuePrimary.toFloat(), 0.7f, 0.8f)
            val hsvSecondary = floatArrayOf(randomHueSecondary.toFloat(), 0.8f, 0.85f)
            val primaryColorInt = android.graphics.Color.HSVToColor(hsvPrimary)
            val secondaryColorInt = android.graphics.Color.HSVToColor(hsvSecondary)
            
            val primaryHex = String.format("#%06X", 0xFFFFFF and primaryColorInt)
            val secondaryHex = String.format("#%06X", 0xFFFFFF and secondaryColorInt)
            
            // Background & typography based on keywords
            val isDark = searchQuery.contains("dark", true) || searchQuery.contains("black", true) || searchQuery.contains("night", true)
            val isSerif = searchQuery.contains("classic", true) || searchQuery.contains("vintage", true) || searchQuery.contains("book", true) || searchQuery.contains("organic", true) || searchQuery.contains("elegant", true)
            val isMonospace = searchQuery.contains("tech", true) || searchQuery.contains("code", true) || searchQuery.contains("minimal", true) || searchQuery.contains("terminal", true)

            val bgHex = if (isDark) "#121212" else "#FAFAFC"
            val surfaceHex = if (isDark) "#1E1E24" else "#F1F2F6"
            val fontStyle = when {
                isSerif -> "Serif"
                isMonospace -> "Monospace"
                else -> "Sans-Serif"
            }

            val niceName = searchQuery.split(" ").joinToString(" ") { it.replaceFirstChar(Char::titlecase) } + " Paletted"

            val simulatedCss = """
                {"borderRadius": "8px", "padding": "24px", "primaryHue": $randomHuePrimary, "secondaryHue": $randomHueSecondary, "shadow": "0px 8px 30px rgba($randomHuePrimary, 0.1)"}
            """.trimIndent().trim()

            val synthesizedStyle = StyleDesign(
                query = searchQuery,
                themeName = niceName,
                primaryColor = primaryHex,
                secondaryColor = secondaryHex,
                backgroundColor = bgHex,
                surfaceColor = surfaceHex,
                fontStyle = fontStyle,
                cssTemplatesJson = simulatedCss
            )
            repository.insertStyle(synthesizedStyle)
        }
    }

    // --- ACTIONS FOR WEB PAGES ---
    fun deleteWebpage(webpage: Webpage) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteWebpage(webpage)
        }
    }

    fun incrementClicks(webpageId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.incrementClicks(webpageId)
            // also update local state directly for simulation feeling
            val webpage = repository.getWebpageById(webpageId)
            if (webpage != null && webpage.isDeployed) {
                // generate minor spike in bandwidth on active click!
                repository.updateDeploymentState(
                    id = webpageId,
                    isDeployed = true,
                    cpu = (webpage.cpuPercent + 3.5).coerceAtMost(98.0),
                    ram = (webpage.ramUsageMB + 0.4).coerceAtMost(512.0),
                    bandwidth = webpage.bandwidthKB + 4.8,
                    port = webpage.port
                )
            }
        }
    }

    fun createManualWebpage(name: String, description: String, html: String, styleName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val wp = Webpage(
                name = name,
                description = description,
                generatedHtml = html,
                selectedStyleName = styleName
            )
            repository.insertWebpage(wp)
        }
    }

    // --- SERVER DEPLOYMENT SIMULATION ENGINE ---
    fun toggleDeployment(webpageId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val webpage = repository.getWebpageById(webpageId) ?: return@launch
            if (webpage.isDeployed) {
                // STOP deployment instantly
                repository.updateDeploymentState(
                    id = webpageId,
                    isDeployed = false,
                    cpu = 0.0,
                    ram = 0.0,
                    bandwidth = 0.0,
                    port = webpage.port
                )
            } else {
                // START deployment instantly
                // Allocate random microservice simulated port e.g. 8000 + ID
                val port = 8000 + webpageId + Random.nextInt(1, 100)
                repository.updateDeploymentState(
                    id = webpageId,
                    isDeployed = true,
                    cpu = Random.nextDouble(1.0, 5.0),
                    ram = Random.nextDouble(32.0, 55.0),
                    bandwidth = Random.nextDouble(0.1, 1.2),
                    port = port
                )
            }
        }
    }

    private suspend fun startResourceMonitoringLoop() {
        while (true) {
            delay(1500) // update resource dashboards every 1.5 seconds
            val list = repository.allWebpages.firstOrNull() ?: emptyList()
            for (wp in list) {
                if (wp.isDeployed) {
                    // Simulating live oscillation of cpu, ram, and bandwidth
                    val cpuDelta = Random.nextDouble(-1.5, 1.5)
                    val ramDelta = Random.nextDouble(-0.5, 0.5)
                    val bandwidthDelta = Random.nextDouble(-0.3, 0.3)

                    val newCpu = (wp.cpuPercent + cpuDelta).coerceIn(0.5, 15.0) // baseline idle cpu is low
                    val newRam = (wp.ramUsageMB + ramDelta).coerceIn(30.0, 85.0) // normal microservice server RAM
                    val newBandwidth = (wp.bandwidthKB + bandwidthDelta).coerceIn(0.01, 10.0)

                    repository.updateDeploymentState(
                        id = wp.id,
                        isDeployed = true,
                        cpu = Math.round(newCpu * 10.0) / 10.0,
                        ram = Math.round(newRam * 10.0) / 10.0,
                        bandwidth = Math.round(newBandwidth * 10.0) / 10.0,
                        port = wp.port
                    )
                }
            }
        }
    }

    // --- MODEL PREFERENCES ACCESS ---
    fun updateProviderSettings(
        provider: String,
        apiKey: String,
        baseUrl: String,
        model: String,
        guidance: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val setting = ModelSetting(
                id = 1,
                selectedProvider = provider,
                customApiKey = apiKey,
                customBaseUrl = baseUrl,
                modelName = model,
                systemGuidance = guidance,
                lastUpdated = System.currentTimeMillis()
            )
            repository.updateModelSettings(setting)
        }
    }

    // --- MAPS AUTOMATIC DEEP SCRAPER (SIMULATED HIGH-FIDELITY) ---
    fun scrapeGoogleMapsLink(link: String) {
        if (link.isBlank()) return
        _isScraping.value = true
        _scrapingStatus.value = "Scanning Gmaps coordinates & headers..."

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Step-by-step progress stimulation to mimic real intense scraping!
                delay(1200)
                _scrapingStatus.value = "Connecting to place endpoints and reading DOM..."
                delay(1400)
                _scrapingStatus.value = "Extracting reviews content & local ratings..."
                delay(1100)
                _scrapingStatus.value = "Analyzing visitor sentiments and deriving stylistic vibes..."
                delay(800)

                // Formulate simulated maps data based on names detected or standard templates
                val isBakery = link.contains("baker", true) || link.contains("bread", true) || link.contains("cake", true)
                val isCoffee = link.contains("coffee", true) || link.contains("cafe", true) || link.contains("tea", true) || link.contains("starbucks", true)
                val isGym = link.contains("gym", true) || link.contains("fitness", true) || link.contains("crossfit", true) || link.contains("sports", true)
                val isTech = link.contains("software", true) || link.contains("tech", true) || link.contains("agency", true) || link.contains("studio", true)

                val (name, rating, phone, category, theme, address, website, reviews) = when {
                    isBakery -> OctetBakeryMock
                    isCoffee -> EsplanadeTeaMock
                    isGym -> IronDepotGymMock
                    isTech -> PrismStudiosMock
                    else -> GenericPlaceMock
                }

                val entry = MapsData(
                    originalUrl = link,
                    businessName = name,
                    rating = rating,
                    reviewsCount = Random.nextInt(32, 280),
                    address = address,
                    phone = phone,
                    category = category,
                    website = website,
                    reviewsJson = reviews,
                    businessTheme = theme
                )

                repository.insertMapsData(entry)
                _scrapingStatus.value = "Imported place \"$name\" securely!"
                delay(1500)
            } catch (e: Exception) {
                _scrapingStatus.value = "Extraction failed: ${e.message}"
                delay(3000)
            } finally {
                _isScraping.value = false
                _scrapingStatus.value = ""
            }
        }
    }

    fun deleteScrapedMapsData(mapsData: MapsData) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMapsData(mapsData)
        }
    }

    // --- AI WEBPAGE CREATION CONTEXT COUPLING ---
    fun generateWebpage(
        name: String,
        description: String,
        selectedStyle: StyleDesign?,
        mapsData: MapsData?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (name.isBlank() || description.isBlank()) {
            onFailure("Business name and description cannot be blank.")
            return
        }

        _isGenerating.value = true
        _generationStatus.value = "Bootstrapping page context..."

        viewModelScope.launch(Dispatchers.IO) {
            try {
                delay(800)
                _generationStatus.value = "Analyzing style palettes and color mappings..."
                delay(800)
                if (mapsData != null) {
                    _generationStatus.value = "Integrating reviews and rating logs from scraped Gmaps profile..."
                    delay(800)
                }
                _generationStatus.value = "Synthesizing full HTML & styling elements via AI Model..."

                val result = repository.generateWebpageFromAI(name, description, selectedStyle, mapsData)
                if (result.isSuccess) {
                    val webpage = result.getOrThrow()
                    repository.insertWebpage(webpage)
                    _isGenerating.value = false
                    _generationStatus.value = ""
                    launch(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    val errMsg = result.exceptionOrNull()?.message ?: "Unknown generator fault"
                    _isGenerating.value = false
                    _generationStatus.value = ""
                    launch(Dispatchers.Main) {
                        onFailure(errMsg)
                    }
                }
            } catch (e: Exception) {
                _isGenerating.value = false
                _generationStatus.value = ""
                launch(Dispatchers.Main) {
                    onFailure(e.localizedMessage ?: "Unknown compilation exception")
                }
            }
        }
    }

    // --- MOCK SCRAPED BUSINESS DATASETS ---
    private val OctetBakeryMock = ScrapedStub(
        name = "La Petite Croissant Bakery",
        rating = 4.9,
        phone = "+33 1 42 27 50 11",
        category = "Traditional Bakery & Pâtisserie",
        theme = "French Cottage Pastel Chic",
        address = "12 Rue des Oliviers, Paris",
        website = "www.lapetitecroissant.fr",
        reviews = """
            [
              {"author": "Jean-Pierre L.", "rating": 5, "comment": "The croissants here break apart in spectacular golden layers! Best butter baker in town.", "time": "2 days ago"},
              {"author": "Sophia V.", "rating": 5, "comment": "Delightful pastel decorations and outstanding coffee. Highly recommend the raspberry tarts!", "time": "a week ago"},
              {"author": "Marcus B.", "rating": 4.8, "comment": "Amazing local shop. Busy on Sunday mornings but 100% worth the queue.", "time": "3 weeks ago"}
            ]
        """.trimIndent()
    )

    private val EsplanadeTeaMock = ScrapedStub(
        name = "Matcha & Moss Botanic Cafe",
        rating = 4.7,
        phone = "+1 (415) 555-8293",
        category = "Organic Herbal Tea House",
        theme = "Warm Cozy Forest Hearth",
        address = "794 Mossy Glade St, San Francisco",
        website = "www.matchandmosscafe.com",
        reviews = """
            [
              {"author": "Aeryn K.", "rating": 5, "comment": "Very peaceful garden seating layout. Generous cups of ceremony-grade matcha.", "time": "1 day ago"},
              {"author": "Logan H.", "rating": 5, "comment": "An absolute treasure! Smells like eucalyptus and fresh honey inside. Best meditation space.", "time": "3 days ago"},
              {"author": "Maya P.", "rating": 4.5, "comment": "Lovely botanic styling. The lavender scones are superb though slightly sweet.", "time": "2 weeks ago"}
            ]
        """.trimIndent()
    )

    private val IronDepotGymMock = ScrapedStub(
        name = "Iron Depot Warehouse Gym",
        rating = 4.8,
        phone = "+1 (310) 555-0144",
        category = "Powerlifting & Athletic Club",
        theme = "Stark Minimalist Brutalist Black/White",
        address = "18 Industrial Parkway, Los Angeles",
        website = "www.irondepothq.com",
        reviews = """
            [
              {"author": "Jaxson D.", "rating": 5, "comment": "No gimmicks, just top tier strength equipment. Music is heavy, atmosphere is raw work.", "time": "5 hours ago"},
              {"author": "Sarah T.", "rating": 5, "comment": "Clean warehouse design. Everyone is focused. Best power platform in town.", "time": "1 week ago"}
            ]
        """.trimIndent()
    )

    private val PrismStudiosMock = ScrapedStub(
        name = "Prism Digital Studio",
        rating = 4.6,
        phone = "+44 20 7946 0912",
        category = "Tech Agency & Software Laboratory",
        theme = "Neon Cyberpunk Minimal Dark",
        address = "Shoreditch high street, London",
        website = "www.prismdigital.io",
        reviews = """
            [
              {"author": "Nikolai G.", "rating": 5, "comment": "Outstanding design execution. Built our web application with phenomenal speed and clean interfaces.", "time": "4 days ago"},
              {"author": "Emma W.", "rating": 4.5, "comment": "Very sharp dark branding aesthetics. Professional engineers and unique style guidelines.", "time": "2 weeks ago"}
            ]
        """.trimIndent()
    )

    private val GenericPlaceMock = ScrapedStub(
        name = "The Local Anchor Boutique",
        rating = 4.5,
        phone = "+1 (800) 555-0199",
        category = "Retail Clothes & Handmade Gifts",
        theme = "Nordic Digital Clean Slate",
        address = "45 Commerce Way, Trade District",
        website = "www.thelocalanchorshop.com",
        reviews = """
            [
              {"author": "Alex R.", "rating": 5, "comment": "Very friendly staff and highly personalized curated designs.", "time": "3 days ago"},
              {"author": "Brenda J.", "rating": 4.0, "comment": "Clean layout, gorgeous organic shirts. A bit expensive but very authentic.", "time": "a month ago"}
            ]
        """.trimIndent()
    )

    private data class ScrapedStub(
        val name: String,
        val rating: Double,
        val phone: String,
        val category: String,
        val theme: String,
        val address: String,
        val website: String,
        val reviews: String
    )
}
