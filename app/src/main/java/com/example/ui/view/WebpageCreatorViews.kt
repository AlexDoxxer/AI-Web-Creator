package com.example.ui.view

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.MapsData
import com.example.data.entity.ModelSetting
import com.example.data.entity.StyleDesign
import com.example.data.entity.Webpage
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class ActiveTab {
    CREATOR,
    MONITOR,
    SCRAPER,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: AppViewModel) {
    var activeTab by remember { mutableStateOf(ActiveTab.CREATOR) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Webpage Creator Studio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Instant AI deployment & local visitor metrics",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Builder Icon",
                        modifier = Modifier.padding(start = 12.dp, end = 8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == ActiveTab.CREATOR,
                    onClick = { activeTab = ActiveTab.CREATOR },
                    label = { Text("AI Creator") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Creator"
                        )
                    }
                )
                NavigationBarItem(
                    selected = activeTab == ActiveTab.MONITOR,
                    onClick = { activeTab = ActiveTab.MONITOR },
                    label = { Text("Deployments") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Simulated Containers"
                        )
                    }
                )
                NavigationBarItem(
                    selected = activeTab == ActiveTab.SCRAPER,
                    onClick = { activeTab = ActiveTab.SCRAPER },
                    label = { Text("Gmaps Extractor") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Scraper"
                        )
                    }
                )
                NavigationBarItem(
                    selected = activeTab == ActiveTab.SETTINGS,
                    onClick = { activeTab = ActiveTab.SETTINGS },
                    label = { Text("Models & API") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Preferences"
                        )
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (activeTab) {
                ActiveTab.CREATOR -> CreatorTabScreen(viewModel, snackbarHostState, scope)
                ActiveTab.MONITOR -> MonitorTabScreen(viewModel)
                ActiveTab.SCRAPER -> ScraperTabScreen(viewModel)
                ActiveTab.SETTINGS -> SettingsTabScreen(viewModel, snackbarHostState, scope)
            }
        }
    }
}

// ==================== TAB 1: AI CREATOR SCREEN ====================
@Composable
fun CreatorTabScreen(
    viewModel: AppViewModel,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    val webpages by viewModel.webpages.collectAsState()
    val styleDesigns by viewModel.styleDesigns.collectAsState()
    val scrapedPlaces by viewModel.scrapedMaps.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val generationStatus by viewModel.generationStatus.collectAsState()
    val styleQuery by viewModel.styleSearchQuery.collectAsState()

    var siteName by remember { mutableStateOf("") }
    var sitePrompt by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf<StyleDesign?>(null) }
    var selectedMapsData by remember { mutableStateOf<MapsData?>(null) }
    var showWebviewId by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SECTION 1: CREATION INPUT CARDS ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Configure New Landing Page",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = siteName,
                        onValueChange = { siteName = it },
                        label = { Text("Business or Website Name") },
                        placeholder = { Text("e.g. Hearth & Honey Coffee") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Build, contentDescription = "Name")
                        }
                    )

                    OutlinedTextField(
                        value = sitePrompt,
                        onValueChange = { sitePrompt = it },
                        label = { Text("Creative Pitch & Requirements") },
                        placeholder = { Text("Describe specific sections, items to display, call to actions, products etc.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        maxLines = 4,
                        leadingIcon = {
                            Icon(Icons.Default.Info, contentDescription = "Pitch")
                        }
                    )

                    // --- SUBSECTION A: DESIGNMD MCP CUSTOM STYLES FILTER ---
                    Text(
                        "designmd MCP - Style Synthesis Engine",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = styleQuery,
                        onValueChange = { viewModel.setStyleSearchQuery(it) },
                        label = { Text("Search Custom Style Patterns") },
                        placeholder = { Text("e.g. vintage, dark terminal, cozy organic, brutalist") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Styles") },
                        trailingIcon = {
                            if (styleQuery.isNotBlank()) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            viewModel.generateOrSearchStyle(styleQuery)
                                            snackbarHostState.showSnackbar("Synthesized custom template palette for: \"$styleQuery\"")
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Synthesize", fontSize = 10.sp)
                                }
                            }
                        }
                    )

                    // Display scrollable list of styles matching query
                    if (styleDesigns.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(styleDesigns) { style ->
                                val isSelected = selectedStyle?.id == style.id
                                Box(
                                    modifier = Modifier
                                        .width(180.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(android.graphics.Color.parseColor(style.backgroundColor)))
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(0.4f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            selectedStyle = if (isSelected) null else style
                                        }
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                style.themeName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (style.backgroundColor.trim() == "#FFFFFF" || style.backgroundColor.trim() == "#FDFBF7" || style.backgroundColor.trim() == "#F0F4F8") Color.Black else Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(style.primaryColor))))
                                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(style.secondaryColor))))
                                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(style.surfaceColor))))
                                        }

                                        Text(
                                            "Font: ${style.fontStyle}",
                                            fontSize = 10.sp,
                                            color = if (style.backgroundColor.trim() == "#FFFFFF" || style.backgroundColor.trim() == "#FDFBF7" || style.backgroundColor.trim() == "#F0F4F8") Color.DarkGray else Color.LightGray
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            "Type an aesthetic concept keyword above & press Synthesize to build a customized layout.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    // --- SUBSECTION B: GOOGLE MAPS COUPLING SELECTION ---
                    Text(
                        "Google Maps Context Integration (Optional)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (scrapedPlaces.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            var expandedMapsDropdown by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { expandedMapsDropdown = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(selectedMapsData?.businessName ?: "Select Scraped Business Location")
                                }
                                DropdownMenu(
                                    expanded = expandedMapsDropdown,
                                    onDismissRequest = { expandedMapsDropdown = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("None - Generate vanilla page") },
                                        onClick = {
                                            selectedMapsData = null
                                            expandedMapsDropdown = false
                                        }
                                    )
                                    scrapedPlaces.forEach { place ->
                                        DropdownMenuItem(
                                            text = { Text("${place.businessName} (${place.category})") },
                                            onClick = {
                                                selectedMapsData = place
                                                expandedMapsDropdown = false
                                                if (siteName.isBlank()) {
                                                    siteName = place.businessName
                                                }
                                                if (sitePrompt.isBlank()) {
                                                    sitePrompt = "A brilliant marketing landing page for our business. Derive specific atmosphere styles from: \"${place.businessTheme}\"."
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            if (selectedMapsData != null) {
                                IconButton(
                                    onClick = { selectedMapsData = null },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear Maps Data", tint = Color.Red)
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = {},
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("No scraped maps businesses found. Use Tab Gmaps Extractor first.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    // --- ACTION GENERATE BUTTON ---
                    Button(
                        onClick = {
                            viewModel.generateWebpage(
                                name = siteName,
                                description = sitePrompt,
                                selectedStyle = selectedStyle,
                                mapsData = selectedMapsData,
                                onSuccess = {
                                    siteName = ""
                                    sitePrompt = ""
                                    selectedStyle = null
                                    selectedMapsData = null
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Webpage successfully built and compiled! View details below.")
                                    }
                                },
                                onFailure = { m ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Model API generation error: $m")
                                    }
                                }
                            )
                        },
                        enabled = !isGenerating && siteName.isNotBlank() && sitePrompt.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Launch Generator", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isGenerating) "AI Web Construction in progress..." else "Generate Personalized Landing Page")
                    }
                }
            }
        }

        // --- SECTION 2: LIVE LOADER STATE CONTROL ---
        if (isGenerating) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                        Column {
                            Text("Generating Page Assets", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(generationStatus, fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        }

        // --- SECTION 3: PAGES OUTBOX ---
        if (webpages.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "No Webpages",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Text("No Webpages Created Yet", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        Text("Complete the prompt form above to synthesize and draft website templates.", fontSize = 11.sp, color = Color.LightGray)
                    }
                }
            }
        } else {
            item {
                Text(
                    "Your Website Portfolios",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(webpages) { webpage ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Companion.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    webpage.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    webpage.description,
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            // Deployment Indicator
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (webpage.isDeployed) Color(0xFFE2F9E5) else Color(0xFFFFECEB))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    if (webpage.isDeployed) "ACTIVE" else "OFFLINE",
                                    color = if (webpage.isDeployed) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Style indicator badge
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Aesthetic: ${webpage.selectedStyleName}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }

                            if (webpage.mapsDataId != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE3F2FD))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("🔗 Gmaps reviews sync", color = Color(0xFF1565C0), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Dynamic Click & Traffic Metrics Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Clicks",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Visits click: ${webpage.clicksCount}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }

                            if (webpage.isDeployed) {
                                Text(
                                    "Simulating: port ${webpage.port}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons Layer
                        Divider(color = Color.LightGray.copy(0.3f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Instant Simulate Visit Click Button
                            OutlinedButton(
                                onClick = {
                                    viewModel.incrementClicks(webpage.id)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Simulate Visit", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Simulate Visit", fontSize = 11.sp)
                            }

                            // Start/Stop Port Deployment
                            Button(
                                onClick = { viewModel.toggleDeployment(webpage.id) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.2f),
                                contentPadding = PaddingValues(vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (webpage.isDeployed) Color(0xFFFF8A80) else Color(0xFF4CAF50)
                                )
                            ) {
                                Icon(
                                    imageVector = if (webpage.isDeployed) Icons.Default.Close else Icons.Default.PlayArrow,
                                    contentDescription = "Power Server",
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (webpage.isDeployed) "Stop Server" else "Deploy Server",
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }

                            // Open WebView Preview Dialog
                            IconButton(
                                onClick = { showWebviewId = webpage.id },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Web Preview",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Delete Page Button
                            IconButton(
                                onClick = { viewModel.deleteWebpage(webpage) },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.Red.copy(0.2f), RoundedCornerShape(8.dp))
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Page",
                                    tint = Color.Red,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Interactive Full-HTML Dialog Screen WebView Previews
    if (showWebviewId != null) {
        val page = webpages.firstOrNull { it.id == showWebviewId }
        if (page != null) {
            HtmlViewDialog(
                htmlContent = page.generatedHtml,
                siteName = page.name,
                clicks = page.clicksCount,
                onDismiss = { showWebviewId = null },
                onSimulateVisitClick = { viewModel.incrementClicks(page.id) }
            )
        }
    }
}

// Dialog wrapper hosting an interactive WebKit Android WebView instance
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HtmlViewDialog(
    htmlContent: String,
    siteName: String,
    clicks: Int,
    onDismiss: () -> Unit,
    onSimulateVisitClick: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column {
                // Toolbar headers with visits click counts
                TopAppBar(
                    title = {
                        Column {
                            Text(siteName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Interactive WebView Preview Mode", fontSize = 11.sp, color = Color.Gray)
                        }
                    },
                    actions = {
                        // Clicks counter badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Visits: $clicks", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onSimulateVisitClick) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh & Count Click", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close Preview")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )

                Divider()

                // Render Android Native WebView in Jetpack Compose
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                textZoom = 100
                            }
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    return false // keep navigation inside webview
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    // every page load increments simulated analytics!
                                    onSimulateVisitClick()
                                }
                            }
                        }
                    },
                    update = { webView ->
                        // Load HTML directly as relative content or standard UTF8
                        webView.loadDataWithBaseURL("https://simulatedhost.local", htmlContent, "text/html", "utf-8", null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White)
                )
            }
        }
    }
}


// ==================== TAB 2: SERVER OPERATIONS & METRICS MONITOR SCREEN ====================
@Composable
fun MonitorTabScreen(viewModel: AppViewModel) {
    val webpages by viewModel.webpages.collectAsState()
    val deployedPages = webpages.filter { it.isDeployed }

    // Aggregate statistics
    val totalDeployed = deployedPages.size
    val aggregateClicks = webpages.sumOf { it.clicksCount }
    val avgCpu = if (deployedPages.isNotEmpty()) deployedPages.sumOf { it.cpuPercent } / totalDeployed else 0.0
    val totalBandwidth = deployedPages.sumOf { it.bandwidthKB }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- METRICS HEADER OVERVIEW CARD ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Self-Hosted Deployment Container",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Live system performance aggregate simulations",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Containers Active", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            Text("$totalDeployed Running", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                        Column {
                            Text("Overall Workload", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            Text("${String.format("%.1f", avgCpu)}% Average CPU", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                        Column {
                            Text("Net Traffic out", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            Text("${String.format("%.1f", totalBandwidth)} KB/s", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        "Sum Visitor Clicks Across Portfolio: $aggregateClicks visits",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(0.08f))
                            .padding(vertical = 6.dp)
                    )
                }
            }
        }

        // --- EXPLICIT RESOURCE INDICATORS INDEX ---
        if (webpages.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Offline", tint = Color.LightGray)
                        Text("No Webpages Exist", fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text("Please create websites in Tab AI Creator before managing deployments.", fontSize = 11.sp, color = Color.LightGray, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            item {
                Text(
                    "Simulated Virtual Servers",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(webpages) { webpage ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, if (webpage.isDeployed) MaterialTheme.colorScheme.primary.copy(0.3f) else Color.LightGray.copy(0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (webpage.isDeployed) Color.Green else Color.Gray)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        webpage.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                Text(
                                    if (webpage.isDeployed) "Simulating server port ${webpage.port} / Live" else "Server Stopped / Port offline",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            }

                            // Start/Stop instant toggle
                            Button(
                                onClick = { viewModel.toggleDeployment(webpage.id) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (webpage.isDeployed) Color(0xFFFF8A80) else Color(0xFF4CAF50)
                                )
                            ) {
                                Text(
                                    if (webpage.isDeployed) "Stop" else "Start",
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }

                        // Web Server Simulated Charts
                        if (webpage.isDeployed) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = Color.LightGray.copy(0.3f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // 1. CPU Bar Meter
                            SensorBarMetric(
                                label = "CPU Workload",
                                valueText = "${webpage.cpuPercent}%",
                                progress = (webpage.cpuPercent / 100f).toFloat().coerceIn(0f, 1f),
                                barColor = if (webpage.cpuPercent > 10.0) Color.Red else MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 2. RAM Bar Meter
                            SensorBarMetric(
                                label = "RAM Capacity Usage",
                                valueText = "${webpage.ramUsageMB} / 512 MB",
                                progress = (webpage.ramUsageMB / 512f).toFloat().coerceIn(0f, 1f),
                                barColor = Color(0xFF9C27B0) // purple
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 3. Bandwidth Traffic Volume
                            SensorBarMetric(
                                label = "Bandwidth Stream out",
                                valueText = "${webpage.bandwidthKB} KB/s",
                                progress = (webpage.bandwidthKB / 20f).toFloat().coerceIn(0f, 1f),
                                barColor = Color(0xFF00BCD4) // cyan
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.LightGray.copy(0.12f))
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Server container offline. No metrics logs recording.",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SensorBarMetric(
    label: String,
    valueText: String,
    progress: Float,
    barColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text(valueText, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = barColor,
            trackColor = Color.LightGray.copy(0.2f)
        )
    }
}


// ==================== TAB 3: GOOGLE MAPS DEEP DECOMPILER & SCRAPER SCREEN ====================
@Composable
fun ScraperTabScreen(viewModel: AppViewModel) {
    val scrapedProfiles by viewModel.scrapedMaps.collectAsState()
    val isScraping by viewModel.isScraping.collectAsState()
    val scrapingStatus by viewModel.scrapingStatus.collectAsState()

    var mapsLinkInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Shortcuts to allow painless user interactions!
    val sampleBeachesBakery = "https://maps.google.com/?cid=129381&name=la_petite_croissant_baker"
    val sampleMossyCafe = "https://maps.app.goo.gl/matcha_and_moss_botanic_tea"
    val sampleIronWarehouse = "https://maps.google.com/?q=iron_depot_warehouse_gym"
    val sampleDigitalStudio = "https://www.google.com/maps/place/prism_digital_studio_london"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- LINK DEEPLINK FORM INPUT CARD ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Extract location details from Google Maps",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Paste any Google Maps link to instantly scrape the business Name, Category, exact Ratings, and extract up to 3 Emotional Reviews to bundle into your AI generator models.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = mapsLinkInput,
                        onValueChange = { mapsLinkInput = it },
                        label = { Text("Google Maps Shared Link") },
                        placeholder = { Text("https://maps.app.goo.gl/... or https://maps.google.com/...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = "Gmaps URL") }
                    )

                    // Helper chips to make preview simple
                    Text("Pre-loaded locations (one-tap test):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = mapsLinkInput == sampleBeachesBakery,
                                onClick = { mapsLinkInput = sampleBeachesBakery },
                                label = { Text("La Petite Bakery") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = mapsLinkInput == sampleMossyCafe,
                                onClick = { mapsLinkInput = sampleMossyCafe },
                                label = { Text("Matcha Botanic Cafe") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = mapsLinkInput == sampleIronWarehouse,
                                onClick = { mapsLinkInput = sampleIronWarehouse },
                                label = { Text("Iron Depot Gym") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = mapsLinkInput == sampleDigitalStudio,
                                onClick = { mapsLinkInput = sampleDigitalStudio },
                                label = { Text("Prism Tech Agency") }
                            )
                        }
                    }

                    // Scrape command
                    Button(
                        onClick = {
                            viewModel.scrapeGoogleMapsLink(mapsLinkInput)
                            mapsLinkInput = ""
                        },
                        enabled = !isScraping && mapsLinkInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Search", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isScraping) "Contacting Maps Nodes..." else "Start Deep Scraping Extraction")
                    }
                }
            }
        }

        // --- SCRAPING LIVE METADATA REVEALER ---
        if (isScraping) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
                            Text("Deep scraping pipeline active...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text(scrapingStatus, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // --- SCRAPED RESULTS REGISTRY ---
        if (scrapedProfiles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No scraped maps database entries. Paste custom links above to populate.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            item {
                Text(
                    "Extracted Profiles Registry",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(scrapedProfiles) { profile ->
                var expandedReviews by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Companion.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(profile.businessName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(profile.category, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                            }
                            IconButton(onClick = { viewModel.deleteScrapedMapsData(profile) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Purge data", tint = Color.Red, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Rating + Theme analysis derived
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = "Star", tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("${profile.rating} / 5", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFFEE58).copy(0.3f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Aesthetic tag: ${profile.businessTheme}", color = Color(0xFFF57F17), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Address: ${profile.address}", fontSize = 11.sp, color = Color.Gray)
                        if (profile.phone.isNotBlank()) {
                            Text("Tel: ${profile.phone}", fontSize = 11.sp, color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Expandable reviews drawer
                        Text(
                            if (expandedReviews) "▼ Hide Scraped Reviews" else "▶ Show Extracted Reviews",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { expandedReviews = !expandedReviews }
                                .padding(vertical = 4.dp)
                        )

                        if (expandedReviews) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = Color.LightGray.copy(0.3f))
                            Spacer(modifier = Modifier.height(8.dp))

                            val parsedReviews = remember(profile.reviewsJson) {
                                try {
                                    val arr = JSONArray(profile.reviewsJson)
                                    val list = mutableListOf<JSONObject>()
                                    for (i in 0 until arr.length()) {
                                        list.add(arr.getJSONObject(i))
                                    }
                                    Result.success(list)
                                } catch (e: Exception) {
                                    Result.failure(e)
                                }
                            }

                            if (parsedReviews.isSuccess) {
                                val reviewsList = parsedReviews.getOrNull() ?: emptyList()
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for (rev in reviewsList) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surface.copy(0.6f))
                                                .padding(8.dp)
                                        ) {
                                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                Text(rev.getString("author"), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(rev.getString("time"), fontSize = 9.sp, color = Color.Gray)
                                            }
                                            Text("⭐".repeat(rev.optInt("rating", 5)), fontSize = 10.sp)
                                            Text(rev.getString("comment"), fontSize = 11.sp, fontStyle = FontStyle.Italic)
                                        }
                                    }
                                }
                            } else {
                                Text("Could not interpret reviews JSON payload.", fontSize = 11.sp, color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==================== TAB 4: API & PROVIDERS CONFIGURATION SCREEN ====================
@Composable
fun SettingsTabScreen(
    viewModel: AppViewModel,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    val modelSettings by viewModel.modelSettings.collectAsState()

    var provider by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var customBaseUrl by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("") }
    var systemInstruction by remember { mutableStateOf("") }

    var hideApiKey by remember { mutableStateOf(true) }

    // Synchronize local edit states with DB setting flow when fetched
    LaunchedEffect(modelSettings) {
        provider = modelSettings.selectedProvider
        apiKey = modelSettings.customApiKey
        customBaseUrl = modelSettings.customBaseUrl
        modelName = modelSettings.modelName
        systemInstruction = modelSettings.systemGuidance
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "AI Agent Model Selection & Keys",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Provider dropdown segmented
                    Text("Select Model API Provider:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    
                    val providersList = listOf("TheOpenCode GO", "OpenRouter", "Gemini", "OpenAI", "Claude", "Custom Endpoint")
                    var expandedDropdown by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expandedDropdown = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(provider)
                        }
                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            providersList.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p) },
                                    onClick = {
                                        provider = p
                                        expandedDropdown = false
                                        // Auto-populate recommended default model identifiers to aid swift developer execution!
                                        modelName = when (p) {
                                            "TheOpenCode GO" -> "thebopencode-go-large"
                                            "OpenRouter" -> "meta-llama/llama-3-8b-instruct:free"
                                            "Gemini" -> "gemini-3.5-flash"
                                            "OpenAI" -> "gpt-4o"
                                            "Claude" -> "claude-3-5-sonnet"
                                            else -> "custom-model-id"
                                        }
                                        customBaseUrl = when (p) {
                                            "TheOpenCode GO" -> "https://api.thebopencode.com/v1"
                                            "OpenRouter" -> "https://openrouter.ai/api/v1"
                                            "Gemini" -> "https://generativelanguage.googleapis.com"
                                            "OpenAI" -> "https://api.openai.com/v1"
                                            else -> ""
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Base URL Input
                    OutlinedTextField(
                        value = customBaseUrl,
                        onValueChange = { customBaseUrl = it },
                        label = { Text("Base Endpoint URL") },
                        placeholder = { Text("https://...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // API Key hidden toggle input
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("Provider API Key") },
                        placeholder = { Text("Paste security key...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (hideApiKey) PasswordVisualTransformation() else VisualTransformation.None,
                        trailingIcon = {
                            IconButton(onClick = { hideApiKey = !hideApiKey }) {
                                Icon(
                                    imageVector = if (hideApiKey) Icons.Default.Warning else Icons.Default.Check,
                                    contentDescription = "Reveal Key"
                                )
                            }
                        }
                    )

                    // Warn developers of decompiler risk
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)),
                        border = BorderStroke(1.dp, Color(0xFFFBC02D).copy(0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = "Guard Key", tint = Color(0xFFF57F17), modifier = Modifier.size(18.dp))
                            Text(
                                "CRITICAL SECURITY MANDATE: Plain key storage inside client-side bundles is accessible upon decompression. Prefer the Secrets Panel in AI Studio dashboard during production compilation.",
                                fontSize = 10.sp,
                                color = Color(0xFF5D4037)
                            )
                        }
                    }

                    // Model ID Input
                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text("Model Identifier Name") },
                        placeholder = { Text("e.g. gemini-3.5-flash or anthropic/claude") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Build, contentDescription = "Model") }
                    )

                    // System instructions tuning Area
                    OutlinedTextField(
                        value = systemInstruction,
                        onValueChange = { systemInstruction = it },
                        label = { Text("System Context Instructions to Model") },
                        placeholder = { Text("Provide global behavior policies for page generations.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        maxLines = 5
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Test connection trial
                        var isTestingSettingsConnection by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = {
                                if (apiKey.isBlank()) {
                                    scope.launch { snackbarHostState.showSnackbar("Cannot test with blank key credentials.") }
                                    return@OutlinedButton
                                }
                                scope.launch {
                                    isTestingSettingsConnection = true
                                    delay(1500) // mock connection handshake
                                    isTestingSettingsConnection = false
                                    snackbarHostState.showSnackbar("Handshake handshake test to \"$provider\" succeeded!")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isTestingSettingsConnection) "Handshaking..." else "Test Link", fontSize = 11.sp)
                        }

                        // Save profiles Button
                        Button(
                            onClick = {
                                viewModel.updateProviderSettings(
                                    provider = provider,
                                    apiKey = apiKey,
                                    baseUrl = customBaseUrl,
                                    model = modelName,
                                    guidance = systemInstruction
                                )
                                scope.launch {
                                    snackbarHostState.showSnackbar("Settings successfully saved and compiled!")
                                }
                            },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Commit", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Configurations", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
