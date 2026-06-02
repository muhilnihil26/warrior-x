package com.example.ui.components

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.BookmarkEntity
import com.example.data.local.HistoryEntity
import com.example.data.xml.RecommendationItem
import com.example.ui.BrowserViewModel
import com.example.ui.SyncedDevice
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.*

// Custom Color Palette following frontend-design: "Professional Polish"
val ObsidianBg = Color(0xFFF3F4F9)      // Main background: #F3F4F9
val CardBg = Color(0xFFFFFFFF)          // Container backgrounds: #FFFFFF
val PrimaryBlue = Color(0xFF2563EB)     // Accent blue: #2563EB
val CyberTeal = Color(0xFF0D9488)       // Cyber Teal/Green accent: #0D9488
val AdBlockGreen = Color(0xFF059669)    // AdBlock / active green: #059669
val IncognitoPurple = Color(0xFF6D28D9) // Dynamic Privacy Purple / Indigo: #6D28D9
val BorderColor = Color(0xFFE2E8F0)     // Border color: #E2E8F0
val WhiteSoft = Color(0xFF1E293B)       // Strong contrast text (Slate-800): #1E293B
val GrayMuted = Color(0xFF64748B)       // Subtitle / muted text: #64748B

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarriorBrowserScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val rawBookmarksList by viewModel.bookmarks.collectAsStateWithLifecycle()
    val rawHistoryList by viewModel.history.collectAsStateWithLifecycle()
    val syncSettings by viewModel.syncSettings.collectAsStateWithLifecycle()
    val addressInput by viewModel.addressBarInput.collectAsStateWithLifecycle()
    val isWebLoading by viewModel.isWebLoading.collectAsStateWithLifecycle()
    val webProgress by viewModel.webProgress.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    val adsBlockedCount by viewModel.adsBlockedCount.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncStatusText by viewModel.syncStatusText.collectAsStateWithLifecycle()
    val otherDevices by viewModel.otherDevices.collectAsStateWithLifecycle()
    val currentUserEmail by viewModel.currentUserEmail.collectAsStateWithLifecycle()
    val authLoading by viewModel.authLoading.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val authSuccess by viewModel.authSuccess.collectAsStateWithLifecycle()

    // Page source observers
    val pageSourceText by viewModel.pageSourceText.collectAsStateWithLifecycle()
    val pageSourceTitle by viewModel.pageSourceTitle.collectAsStateWithLifecycle()
    val isSourceLoading by viewModel.isSourceLoading.collectAsStateWithLifecycle()

    val activeTab = tabs.find { it.isActive }
    val focusManager = LocalFocusManager.current

    // Navigation state in details views
    var activeViewSection by remember { mutableStateOf("home") } // "home", "bookmarks", "history", "sync"

    // Set colors according to Privacy Mode
    val primaryThemeColor = if (syncSettings.isPrivacyModeEnabled) IncognitoPurple else PrimaryBlue
    val topBarBg = if (syncSettings.isPrivacyModeEnabled) Color(0xFFF5F3FF) else CardBg

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        topBar = {
            Column(
                modifier = Modifier
                    .background(topBarBg)
                    .statusBarsPadding()
            ) {
                // Header brand description bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(primaryThemeColor, CyberTeal)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "X",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "WARRIOR X",
                                    color = WhiteSoft,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "by war.dev",
                                    color = primaryThemeColor,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                if (syncSettings.isPrivacyModeEnabled) "Stealth Privacy Mode Active" else "Zero-Knowledge Cloud Ready",
                                color = if (syncSettings.isPrivacyModeEnabled) IncognitoPurple else GrayMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Top quick switches (AdBlock and Privacy quick toggle icons)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Shield Adblocker
                        IconButton(
                            onClick = { viewModel.toggleAdBlocking(!syncSettings.isAdBlockingEnabled) },
                            modifier = Modifier.testTag("adblock_quick_toggle")
                        ) {
                            Icon(
                                imageVector = if (syncSettings.isAdBlockingEnabled) Icons.Filled.Shield else Icons.Filled.Shield,
                                contentDescription = "Adblock",
                                tint = if (syncSettings.isAdBlockingEnabled) AdBlockGreen else GrayMuted
                            )
                        }
                        // Incognito mask
                        IconButton(
                            onClick = { viewModel.togglePrivacyMode(!syncSettings.isPrivacyModeEnabled) },
                            modifier = Modifier.testTag("privacy_quick_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = "Incognito Mode",
                                tint = if (syncSettings.isPrivacyModeEnabled) IncognitoPurple else GrayMuted
                            )
                        }
                    }
                }

                // Address Bar & Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back/Forward
                    IconButton(
                        onClick = {
                            // If not home, back to starting page
                            viewModel.navigateToUrl("https://war.dev/warrior-x")
                        },
                        enabled = activeTab != null && activeTab.url != "https://war.dev/warrior-x"
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = "Go Home",
                            tint = if (activeTab != null && activeTab.url != "https://war.dev/warrior-x") WhiteSoft else BorderColor
                        )
                    }

                    // Input Address field
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { viewModel.setAddressInput(it) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("address_url_input"),
                        placeholder = { Text("Search or Type URL", color = GrayMuted, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (syncSettings.isPrivacyModeEnabled) Icons.Filled.Lock else Icons.Filled.Search,
                                contentDescription = "Security Badge",
                                tint = if (syncSettings.isPrivacyModeEnabled) IncognitoPurple else primaryThemeColor,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (addressInput.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setAddressInput("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = GrayMuted)
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                viewModel.navigateToUrl(addressInput)
                                focusManager.clearFocus()
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CardBg,
                            unfocusedContainerColor = CardBg,
                            focusedBorderColor = primaryThemeColor,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = WhiteSoft,
                            unfocusedTextColor = WhiteSoft
                        ),
                        shape = RoundedCornerShape(100.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Bookmark active page
                    IconButton(
                        onClick = {
                            activeTab?.let {
                                viewModel.addBookmark(it.title, it.url)
                            }
                        },
                        enabled = activeTab != null,
                        modifier = Modifier.testTag("bookmark_toggle_btn")
                    ) {
                        val isBookmarked = rawBookmarksList.any { it.url == activeTab?.url }
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) AdBlockGreen else WhiteSoft
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Developer page source view
                    IconButton(
                        onClick = {
                            activeTab?.let {
                                viewModel.fetchPageSource(it.url, it.title)
                            }
                        },
                        enabled = activeTab != null,
                        modifier = Modifier.testTag("view_source_toggle_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Code,
                            contentDescription = "Verify source",
                            tint = if (activeTab != null) primaryThemeColor else GrayMuted
                        )
                    }
                }

                // Loading bar
                if (isWebLoading) {
                    LinearProgressIndicator(
                        progress = { webProgress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = primaryThemeColor,
                        trackColor = Color.Transparent,
                    )
                }

                // Horizontal Tab management bar
                TabManagementBar(
                    tabs = tabs,
                    primaryColor = primaryThemeColor,
                    onSelect = { viewModel.selectTab(it) },
                    onClose = { viewModel.closeTab(it) },
                    onAdd = { viewModel.addNewTab() }
                )
            }
        },
        bottomBar = {
            // M3 dynamic primary bottom navigation bar
            NavigationBar(
                containerColor = Color(0xFFEFF1F8),
                contentColor = WhiteSoft,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .border(width = 0.5.dp, color = BorderColor)
            ) {
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = primaryThemeColor,
                    selectedTextColor = primaryThemeColor,
                    unselectedIconColor = GrayMuted,
                    unselectedTextColor = GrayMuted,
                    indicatorColor = primaryThemeColor.copy(alpha = 0.12f)
                )

                NavigationBarItem(
                    selected = activeViewSection == "home",
                    onClick = { activeViewSection = "home" },
                    label = { Text("Browser", fontSize = 11.sp, fontWeight = if (activeViewSection == "home") FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Filled.Web, contentDescription = "Browser Screen") },
                    colors = navItemColors
                )

                NavigationBarItem(
                    selected = activeViewSection == "bookmarks",
                    onClick = { activeViewSection = "bookmarks" },
                    label = { Text("Bookmarks", fontSize = 11.sp, fontWeight = if (activeViewSection == "bookmarks") FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Filled.Bookmark, contentDescription = "Secured Bookmarks") },
                    colors = navItemColors
                )

                NavigationBarItem(
                    selected = activeViewSection == "history",
                    onClick = { activeViewSection = "history" },
                    label = { Text("History", fontSize = 11.sp, fontWeight = if (activeViewSection == "history") FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Filled.History, contentDescription = "History Logs") },
                    colors = navItemColors
                )

                NavigationBarItem(
                    selected = activeViewSection == "sync",
                    onClick = { activeViewSection = "sync" },
                    label = { Text("Secure Sync", fontSize = 11.sp, fontWeight = if (activeViewSection == "sync") FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Filled.Sync, contentDescription = "Zero-Knowledge Sync") },
                    colors = navItemColors
                )

                NavigationBarItem(
                    selected = activeViewSection == "ai",
                    onClick = { activeViewSection = "ai" },
                    label = { Text("Warrior AI", fontSize = 11.sp, fontWeight = if (activeViewSection == "ai") FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = "Warrior AI") },
                    colors = navItemColors
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(ObsidianBg)
        ) {
            when (activeViewSection) {
                "home" -> {
                    // Check if current active tab is indeed Warrior start page or real site
                    val currentTabUrl = activeTab?.url ?: "https://war.dev/warrior-x"
                    if (currentTabUrl.startsWith("https://war.dev") || currentTabUrl.contains("about:blank")) {
                        // Start Portal (Native UI)
                        WarriorStartPortal(
                            viewModel = viewModel,
                            syncSettings = syncSettings,
                            primaryThemeColor = primaryThemeColor,
                            recommendations = recommendations,
                            adsBlockedCount = adsBlockedCount,
                            onNavigateToSection = { activeViewSection = it }
                        )
                    } else {
                        // Render full Interactive secure WebView with ad blocking
                        WarriorSecureWebView(
                            url = currentTabUrl,
                            viewModel = viewModel,
                            adBlockingEnabled = syncSettings.isAdBlockingEnabled
                        )
                    }
                }
                "bookmarks" -> {
                    SecureBookmarksView(
                        bookmarks = rawBookmarksList,
                        passphrase = syncSettings.syncPassphrase,
                        onDeleteBookmark = { viewModel.deleteBookmark(it) },
                        onNavigateToUrl = {
                            viewModel.navigateToUrl(it)
                            activeViewSection = "home"
                        },
                        primaryThemeColor = primaryThemeColor
                    )
                }
                "history" -> {
                    SecureHistoryView(
                        history = rawHistoryList,
                        passphrase = syncSettings.syncPassphrase,
                        onClearHistory = { viewModel.clearAllHistory() },
                        onNavigateToUrl = {
                            viewModel.navigateToUrl(it)
                            activeViewSection = "home"
                        },
                        primaryThemeColor = primaryThemeColor,
                        privacyModeEnabled = syncSettings.isPrivacyModeEnabled
                    )
                }
                "sync" -> {
                    ZeroKnowledgeSyncSettingsView(
                        syncSettings = syncSettings,
                        isSyncing = isSyncing,
                        syncStatusText = syncStatusText,
                        otherDevices = otherDevices,
                        onUpdatePassphrase = { viewModel.updatePassphrase(it) },
                        onToggleCloudSync = { viewModel.toggleCloudSync(it) },
                        onTriggerSyncNow = { viewModel.triggerSyncSimulation() },
                        primaryThemeColor = primaryThemeColor,
                        currentUserEmail = currentUserEmail,
                        authLoading = authLoading,
                        authError = authError,
                        authSuccess = authSuccess,
                        onLogin = { email, pass -> viewModel.firebaseLogin(email, pass) },
                        onSignup = { email, pass -> viewModel.firebaseSignup(email, pass) },
                        onLogout = { viewModel.firebaseLogout() },
                        onDismissMessage = { viewModel.dismissAuthMessage() }
                    )
                }
                "ai" -> {
                    WarriorAiView(
                        viewModel = viewModel,
                        activeTab = activeTab,
                        primaryThemeColor = primaryThemeColor
                    )
                }
            }
        }
    }

    if (pageSourceText != null || isSourceLoading) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPageSource() },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissPageSource() }) {
                    Text("Close Panel", color = primaryThemeColor)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Code, contentDescription = null, tint = primaryThemeColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSourceLoading) "Fetching Source..." else "Source: $pageSourceTitle",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = WhiteSoft
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                    if (isSourceLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = primaryThemeColor)
                        }
                    } else {
                        pageSourceText?.let { htmlText ->
                            Surface(
                                modifier = Modifier.fillMaxSize().border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                                color = Color(0xFF0F172A), // Elegant dark slate editor background
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                    LazyColumn {
                                        item {
                                            Text(
                                                text = htmlText,
                                                color = Color(0xFFE2E8F0),
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = CardBg
        )
    }
}

// Custom horizontal scroll TabBar
@Composable
fun TabManagementBar(
    tabs: List<com.example.data.local.TabEntity>,
    primaryColor: Color,
    onSelect: (Int) -> Unit,
    onClose: (Int) -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier
                .weight(1f)
                .padding(end = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(tabs) { tab ->
                val isSelected = tab.isActive
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) primaryColor.copy(alpha = 0.25f) else CardBg)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) primaryColor else BorderColor,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelect(tab.id) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (tab.isActive) Icons.Filled.WebAsset else Icons.Filled.WebAssetOff,
                        contentDescription = "Tab status",
                        tint = if (isSelected) primaryColor else GrayMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tab.title,
                        color = if (isSelected) primaryColor else GrayMuted,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 100.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close tab",
                        tint = GrayMuted,
                        modifier = Modifier
                            .size(12.dp)
                            .clickable { onClose(tab.id) }
                    )
                }
            }
        }

        // Add Tab Button
        IconButton(
            onClick = onAdd,
            modifier = Modifier
                .size(28.dp)
                .background(CardBg, CircleShape)
                .border(1.dp, BorderColor, CircleShape)
                .testTag("add_tab_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add new tab",
                tint = primaryColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// --------------------- SUB VIEWS ---------------------

// Native Startup Area / Privacy Hub
@Composable
fun WarriorStartPortal(
    viewModel: BrowserViewModel,
    syncSettings: com.example.data.local.SyncSettingsEntity,
    primaryThemeColor: Color,
    recommendations: List<RecommendationItem>,
    adsBlockedCount: Int,
    onNavigateToSection: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Status Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Warrior X Protection",
                                color = WhiteSoft,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "v1.0.4 • Secured by war.dev",
                                color = GrayMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(if (syncSettings.isPrivacyModeEnabled) IncognitoPurple.copy(alpha = 0.15f) else Color(0xFFD1FAE5))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                if (syncSettings.isPrivacyModeEnabled) "STEALTH ACTIVE" else "ACTIVE",
                                color = if (syncSettings.isPrivacyModeEnabled) IncognitoPurple else Color(0xFF047857),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Stat 1: Ad blocker count
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("🚫", fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    adsBlockedCount.toString(),
                                    color = WhiteSoft,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Ads Blocked",
                                    color = GrayMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Stat 2: Zero-Knowledge state
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("🔐", fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (syncSettings.syncPassphrase.isNotEmpty()) "AES-256" else "None",
                                    color = if (syncSettings.syncPassphrase.isNotEmpty()) AdBlockGreen else WhiteSoft,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Sync Lock",
                                    color = GrayMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Stat 3: Cloud sync toggle state
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("☁️", fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (syncSettings.isCloudSyncEnabled) "Enabled" else "Local",
                                    color = if (syncSettings.isCloudSyncEnabled) primaryThemeColor else WhiteSoft,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Sync State",
                                    color = GrayMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Shortcuts Row
        item {
            Text("QUICK CHANNELS", color = GrayMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Shortcut 1: war.dev Search
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.navigateToUrl("https://www.google.com") },
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, BorderColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(primaryThemeColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search Google",
                                tint = primaryThemeColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Google",
                            color = WhiteSoft,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Shortcut 2: Zero-Knowledge guide
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.navigateToUrl("https://war.dev/privacy/zero-knowledge") },
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, BorderColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyberTeal.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = "Tech Specs",
                                tint = CyberTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Zero-Knowledge",
                            color = WhiteSoft,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Shortcut 3: System settings page
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToSection("sync") },
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, BorderColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AdBlockGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Configure Sync",
                                tint = AdBlockGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Setup Sync",
                            color = WhiteSoft,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Local Recommendation Banner (Design Match)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF312E81)), // bg-indigo-900
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.navigateToUrl("https://war.dev/privacy/zero-knowledge") }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4338CA)), // bg-indigo-700
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💡", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Zero-Knowledge Suggestion",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Based on your local browsing patterns...",
                            color = Color(0xFFC7D2FE), // text-indigo-200
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF3730A3)), // bg-indigo-800
                        contentAlignment = Alignment.Center
                    ) {
                        Text("➔", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }

        // XML Recommendation Engine Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ElectricBolt, contentDescription = "XML recommendations", tint = primaryThemeColor)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "XML COGNITIVE RECOMMENDATIONS",
                        color = WhiteSoft,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    "Local Feed Only",
                    color = AdBlockGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "Personalized locally, built entirely on top of offline recommendation files and local browsing history weights.",
                color = GrayMuted,
                fontSize = 10.sp
            )
        }

        items(recommendations) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.navigateToUrl(item.url) },
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (item.category) {
                                    "security" -> CyberTeal.copy(alpha = 0.15f)
                                    "privacy" -> AdBlockGreen.copy(alpha = 0.15f)
                                    "tech" -> primaryThemeColor.copy(alpha = 0.15f)
                                    else -> GrayMuted.copy(alpha = 0.15f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (item.category) {
                                "security" -> Icons.Filled.Fingerprint
                                "privacy" -> Icons.Filled.PrivacyTip
                                "tech" -> Icons.Filled.DeveloperMode
                                else -> Icons.Filled.Newspaper
                            },
                            contentDescription = item.category,
                            tint = when (item.category) {
                                "security" -> CyberTeal
                                "privacy" -> AdBlockGreen
                                "tech" -> primaryThemeColor
                                else -> WhiteSoft
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            item.title,
                            color = WhiteSoft,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            item.description,
                            color = GrayMuted,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = Icons.Filled.ArrowForwardIos,
                        contentDescription = "Navigate",
                        tint = GrayMuted,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

// Full WebView Rendering with Web Intercept ad blocker demo
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WarriorSecureWebView(
    url: String,
    viewModel: BrowserViewModel,
    adBlockingEnabled: Boolean
) {
    val context = LocalContext.current
    val adBlockState = rememberUpdatedState(adBlockingEnabled)

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            // Let Android WebView handle URL
                            return false
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            viewModel.setWebLoading(true)
                            viewModel.setWebProgress(15)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            viewModel.setWebLoading(false)
                            viewModel.setWebProgress(100)
                        }

                        @Suppress("DeprecatedCallableAddReplaceWith")
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            url: String?
                        ): WebResourceResponse? {
                            // Simple string helper
                            val u = url ?: ""
                            if (adBlockState.value && isAdOrTracker(u)) {
                                Handler(Looper.getMainLooper()).post {
                                    viewModel.incrementAdsBlocked()
                                }
                                return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                            }
                            return super.shouldInterceptRequest(view, url)
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val u = request?.url?.toString() ?: ""
                            if (adBlockState.value && isAdOrTracker(u)) {
                                Handler(Looper.getMainLooper()).post {
                                    viewModel.incrementAdsBlocked()
                                }
                                return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                    }
                }
            },
            update = { webView ->
                // Check if we need to load new URL
                if (webView.url != url) {
                    webView.loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun isAdOrTracker(url: String): Boolean {
    val lower = url.lowercase()
    val adKeywords = listOf(
        "doubleclick.net", "googleads", "pagead2", "googlesyndication",
        "adservice.google", "analytics.google", "google-analytics",
        "ads.youtube", "adnxs.com", "pubmatic.com", "rubiconproject",
        "adsystem", "telemetry", "trackers", "adserver", "adskeeper",
        "mgid.com", "popads.net", "outbrain.com", "taboola.com"
    )
    return adKeywords.any { lower.contains(it) }
}

// Bookmarks Panel (Secure client decryptable)
@Composable
fun SecureBookmarksView(
    bookmarks: List<BookmarkEntity>,
    passphrase: String,
    onDeleteBookmark: (Int) -> Unit,
    onNavigateToUrl: (String) -> Unit,
    primaryThemeColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Bookmark, contentDescription = "Bookmarks", tint = primaryThemeColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "SECURE BOOKMARKS",
                    color = WhiteSoft,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Text(
                if (passphrase.isNotEmpty()) "🔒 Zero-Knowledge AES-256 Enabled" else "🔓 Plaintext Mode",
                color = if (passphrase.isNotEmpty()) AdBlockGreen else GrayMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.BookmarkBorder,
                        contentDescription = "Empty Bookmarks",
                        tint = GrayMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No Bookmarks Saved", color = WhiteSoft, fontWeight = FontWeight.SemiBold)
                    Text("Navigate to any site and tap the Bookmark icon to lock it in.", color = GrayMuted, fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(bookmarks) { b ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (b.title != "🔓 [Decryption Error]" && b.url != "[Encrypted]") onNavigateToUrl(b.url) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (b.isEncrypted) Icons.Filled.Https else Icons.Filled.Launch,
                                contentDescription = "Bookmark",
                                tint = if (b.isEncrypted) primaryThemeColor else AdBlockGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = b.title,
                                    color = WhiteSoft,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = b.url,
                                    color = primaryThemeColor,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (b.isEncrypted) {
                                    Text(
                                        text = "Raw AES payload locked client-side in SQLite",
                                        color = AdBlockGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Light
                                    )
                                }
                            }
                            IconButton(onClick = { onDeleteBookmark(b.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = GrayMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// History Logs Panel (Secure client-side database logs)
@Composable
fun SecureHistoryView(
    history: List<HistoryEntity>,
    passphrase: String,
    onClearHistory: () -> Unit,
    onNavigateToUrl: (String) -> Unit,
    primaryThemeColor: Color,
    privacyModeEnabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.History, contentDescription = "History", tint = primaryThemeColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "SECURE HISTORY",
                    color = WhiteSoft,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            if (history.isNotEmpty()) {
                TextButton(
                    onClick = onClearHistory,
                    modifier = Modifier.testTag("clear_history_btn")
                ) {
                    Text("Clear Logs", color = Color.Red, fontSize = 12.sp)
                }
            }
        }

        // Stealth explanation indicator
        if (privacyModeEnabled) {
            Card(
                colors = CardDefaults.cardColors(containerColor = IncognitoPurple.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, IncognitoPurple),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.VisibilityOff, contentDescription = "Privacy active", tint = IncognitoPurple)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Stealth Privacy Mode is currently ON. No local search history or tabs tracking payloads are written to the SQLite database.",
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Web,
                        contentDescription = "Empty History",
                        tint = GrayMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No Navigation Logs", color = WhiteSoft, fontWeight = FontWeight.SemiBold)
                    Text("Standard browsing writes local encrypted transaction states.", color = GrayMuted, fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history) { h ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (h.url != "[Encrypted URL]") onNavigateToUrl(h.url) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(primaryThemeColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    h.category.substring(0, 1).uppercase(),
                                    color = primaryThemeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = h.title,
                                    color = WhiteSoft,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = h.url,
                                    color = primaryThemeColor,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val date = remember(h.timestamp) {
                                        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                        fmt.format(Date(h.timestamp))
                                    }
                                    Text(
                                        text = date,
                                        color = GrayMuted,
                                        fontSize = 9.sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(BorderColor)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = h.category.uppercase(),
                                            color = primaryThemeColor,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Device Synchronization & Passphrase setup Panel
@Composable
fun ZeroKnowledgeSyncSettingsView(
    syncSettings: com.example.data.local.SyncSettingsEntity,
    isSyncing: Boolean,
    syncStatusText: String,
    otherDevices: List<SyncedDevice>,
    onUpdatePassphrase: (String) -> Unit,
    onToggleCloudSync: (Boolean) -> Unit,
    onTriggerSyncNow: () -> Unit,
    primaryThemeColor: Color,
    currentUserEmail: String?,
    authLoading: Boolean,
    authError: String?,
    authSuccess: String?,
    onLogin: (String, String) -> Unit,
    onSignup: (String, String) -> Unit,
    onLogout: () -> Unit,
    onDismissMessage: () -> Unit
) {
    var passphraseInput by remember { mutableStateOf(syncSettings.syncPassphrase) }
    var showPassphrase by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Firebase Authentication Sync Profile Card
        item {
            var authEmail by remember { mutableStateOf("") }
            var authPassword by remember { mutableStateOf("") }
            var isRegisterMode by remember { mutableStateOf(false) }

            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("firebase_auth_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = "Firebase Sync Profile",
                                tint = primaryThemeColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "WARRIOR ACCOUNT (FIREBASE)",
                                color = WhiteSoft,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        
                        if (currentUserEmail != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AdBlockGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "SECURED",
                                    color = AdBlockGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (currentUserEmail != null) {
                        // User is signed in
                        Text(
                            "Account ID (Email):",
                            color = GrayMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = currentUserEmail,
                            color = WhiteSoft,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp).testTag("authenticated_user_email")
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "You are successfully logged in! Your browser bookmarks, configurations and tabs can now sync under secure end-to-end cloud protection nodes.",
                            color = GrayMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onLogout,
                            modifier = Modifier.fillMaxWidth().testTag("logout_account_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)), // Red signout
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Logout, contentDescription = "Sign Out", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign Out Sync Profile", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    } else {
                        // User is NOT signed in (Form Mode)
                        Text(
                            text = if (isRegisterMode) 
                                "Create a free cloud account using Firebase Auth to keep your bookmarks and history synced across browsers." 
                            else 
                                "Log in with your email and password to connect with real Firebase Sync Nodes.",
                            color = GrayMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Email Field
                        OutlinedTextField(
                            value = authEmail,
                            onValueChange = { authEmail = it },
                            modifier = Modifier.fillMaxWidth().testTag("auth_email_input"),
                            label = { Text("Email Address", color = GrayMuted, fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ObsidianBg,
                                unfocusedContainerColor = ObsidianBg,
                                focusedTextColor = WhiteSoft,
                                unfocusedTextColor = WhiteSoft,
                                focusedBorderColor = primaryThemeColor,
                                unfocusedBorderColor = BorderColor
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Password Field
                        var showAuthPassword by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = authPassword,
                            onValueChange = { authPassword = it },
                            modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
                            label = { Text("Password", color = GrayMuted, fontSize = 12.sp) },
                            singleLine = true,
                            visualTransformation = if (showAuthPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showAuthPassword = !showAuthPassword }) {
                                    Icon(
                                        imageVector = if (showAuthPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = "Toggle password",
                                        tint = GrayMuted
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ObsidianBg,
                                unfocusedContainerColor = ObsidianBg,
                                focusedTextColor = WhiteSoft,
                                unfocusedTextColor = WhiteSoft,
                                focusedBorderColor = primaryThemeColor,
                                unfocusedBorderColor = BorderColor
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (authLoading) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = primaryThemeColor, modifier = Modifier.size(24.dp))
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (isRegisterMode) {
                                            onSignup(authEmail, authPassword)
                                        } else {
                                            onLogin(authEmail, authPassword)
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("auth_submit_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryThemeColor),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = if (isRegisterMode) "Create Account" else "Log In",
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = { 
                                        isRegisterMode = !isRegisterMode 
                                        onDismissMessage()
                                    },
                                    modifier = Modifier.weight(1f).testTag("auth_toggle_mode_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = primaryThemeColor),
                                    border = BorderStroke(1.dp, primaryThemeColor),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = if (isRegisterMode) "Switch to Login" else "Create Account",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Display Error / Success Notifications cleanly
                    if (authError != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEF2F2))
                                .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Error, contentDescription = "Error", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(authError, color = Color(0xFF991B1B), fontSize = 11.sp, modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = onDismissMessage,
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = Color(0xFF991B1B), modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }

                    if (authSuccess != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFECFDF5))
                                .border(1.dp, Color(0xFF6EE7B7), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = "Success", tint = AdBlockGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(authSuccess, color = Color(0xFF065F46), fontSize = 11.sp, modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = onDismissMessage,
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = Color(0xFF065F46), modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        // Explanatory Intro Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Fingerprint,
                            contentDescription = "Zero-Knowledge",
                            tint = CyberTeal,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Zero-Knowledge Architecture",
                            color = WhiteSoft,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "All of your navigation histories, bookmarks and session data are cryptographically encrypted on this device using an AES-256 key derived from your sync passphrase. " +
                        "No plaintext data is ever sent over the network, ensuring complete user confidentiality across macOS, Windows, and Android.",
                        color = GrayMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Passphrase Setup Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "SYNC PASSPHRASE",
                        color = WhiteSoft,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Sets client-side Master AES encryption. If you clear this passphrase, data is decrypted into standard storage.",
                        color = GrayMuted,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = passphraseInput,
                        onValueChange = { passphraseInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("passphrase_text_input"),
                        label = { Text("Enter Passphrase", color = GrayMuted, fontSize = 12.sp) },
                        visualTransformation = if (showPassphrase) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassphrase = !showPassphrase }) {
                                Icon(
                                    imageVector = if (showPassphrase) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle visibility",
                                    tint = GrayMuted
                                )
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CardBg,
                            unfocusedContainerColor = CardBg,
                            focusedBorderColor = primaryThemeColor,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = WhiteSoft,
                            unfocusedTextColor = WhiteSoft
                        ),
                        shape = RoundedCornerShape(100.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onUpdatePassphrase(passphraseInput) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_passphrase_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryThemeColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Apply AES Encryption Key", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Cross-platform synchronization trigger card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "CROSS-PLATFORM CLOUD SYNC",
                                color = WhiteSoft,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                "Keep database synced with macOS & Windows",
                                color = GrayMuted,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = syncSettings.isCloudSyncEnabled,
                            onCheckedChange = { onToggleCloudSync(it) },
                            modifier = Modifier.testTag("cloud_sync_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AdBlockGreen,
                                checkedTrackColor = AdBlockGreen.copy(alpha = 0.3f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sync Status message box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianBg)
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = primaryThemeColor,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (syncSettings.isCloudSyncEnabled) Icons.Filled.SyncAlt else Icons.Filled.SyncDisabled,
                                    contentDescription = "Sync status",
                                    tint = if (syncSettings.isCloudSyncEnabled) AdBlockGreen else GrayMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = syncStatusText,
                                color = if (isSyncing) primaryThemeColor else WhiteSoft,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onTriggerSyncNow,
                        enabled = syncSettings.isCloudSyncEnabled && !isSyncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trigger_sync_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = AdBlockGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.CloudSync, contentDescription = "Sync Now", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Secure Sync All Devices", fontWeight = FontWeight.Black)
                        }
                    }

                    if (syncSettings.lastSyncedTime > 0) {
                        val lastSyncedFormatted = remember(syncSettings.lastSyncedTime) {
                            val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            fmt.format(Date(syncSettings.lastSyncedTime))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Last Synced Successfully: $lastSyncedFormatted",
                            color = GrayMuted,
                            fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }

        // Active devices list section (macOS / Windows browser clients)
        item {
            Text(
                "CONNECTED DEVICES (macOS / WINDOWS)",
                color = WhiteSoft,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        items(otherDevices) { dev ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(primaryThemeColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (dev.name.contains("macOS")) Icons.Filled.LaptopMac else Icons.Filled.Laptop,
                                contentDescription = "Device icon",
                                tint = primaryThemeColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(dev.name, color = WhiteSoft, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(dev.type, color = GrayMuted, fontSize = 11.sp)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AdBlockGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(dev.status, color = AdBlockGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(dev.lastSeen, color = GrayMuted, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun WarriorAiView(
    viewModel: BrowserViewModel,
    activeTab: com.example.data.local.TabEntity?,
    primaryThemeColor: Color
) {
    val aiHistory by viewModel.aiChatHistory.collectAsStateWithLifecycle()
    val aiLoading by viewModel.aiLoading.collectAsStateWithLifecycle()
    val aiError by viewModel.aiError.collectAsStateWithLifecycle()
    val shieldLevel by viewModel.shieldLevel.collectAsStateWithLifecycle()
    
    var customPromptInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Core Card header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(primaryThemeColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = primaryThemeColor
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "WARRIOR AI XP-1",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhiteSoft,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "De-coupled Client Sandbox",
                                fontSize = 10.sp,
                                color = GrayMuted
                            )
                        }
                    }

                    TextButton(
                        onClick = { viewModel.clearAiHistory() },
                        colors = ButtonDefaults.textButtonColors(contentColor = GrayMuted)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Chat", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BorderColor)
                Spacer(modifier = Modifier.height(12.dp))

                // Security Shield Controls
                Text(
                    "SECURITY SHIELD STRENGTH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrayMuted,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val levels = listOf("Standard", "High Protection", "Paranoid Stealth")
                    levels.forEach { lvl ->
                        val isSelected = shieldLevel == lvl
                        val badgeColor = when (lvl) {
                            "Paranoid Stealth" -> IncognitoPurple
                            "High Protection" -> primaryThemeColor
                            else -> CyberTeal
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) badgeColor.copy(alpha = 0.12f) else BorderColor.copy(alpha = 0.3f))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) badgeColor else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.updateShieldLevel(lvl) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lvl,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) badgeColor else GrayMuted
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Actions Header
        Text(
            text = "FAST GRAPH INTEGRATED COMMANDS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = GrayMuted,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // AI Shortcuts row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            item {
                ActionChip(
                    title = "🛡️ Audit Page",
                    subtitle = "Scan active domain",
                    onClick = {
                        val activeUrl = activeTab?.url ?: "https://war.dev/warrior-x"
                        viewModel.queryGemini("Analyze security of URL: $activeUrl. Does it present phishing risks or bad headers?")
                    }
                )
            }
            item {
                ActionChip(
                    title = "📋 Summarize Site",
                    subtitle = "Audit key items",
                    onClick = {
                        val activeUrl = activeTab?.url ?: "https://war.dev/warrior-x"
                        val activeTitle = activeTab?.title ?: "Warrior Portal"
                        viewModel.queryGemini("Summarize the website '$activeTitle' ($activeUrl) in 3 key protective bullet points.")
                    }
                )
            }
            item {
                ActionChip(
                    title = "🔑 Key Entropy",
                    subtitle = "Safe Pass generator",
                    onClick = {
                        viewModel.queryGemini("Generate an example military-grade highly secure password. Detail entropy rules in bytes.")
                    }
                )
            }
            item {
                ActionChip(
                    title = "📜 Handshake Docs",
                    subtitle = "SSL/TLS breakdown",
                    onClick = {
                        viewModel.queryGemini("Explain SSL/TLS handshakes, certificate signatures, and AES-256 GCM in three easy bullets.")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Scrollable Chat area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(0.5.dp, BorderColor)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(aiHistory) { msg ->
                            val isUser = msg.isUser
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .widthIn(max = 280.dp)
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 12.dp,
                                                topEnd = 12.dp,
                                                bottomStart = if (isUser) 12.dp else 2.dp,
                                                bottomEnd = if (isUser) 2.dp else 12.dp
                                            )
                                        )
                                        .background(
                                            if (isUser) primaryThemeColor else Color(0xFFF1F5F9)
                                        )
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = if (isUser) "You" else "WARRIOR AI XP-1",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isUser) Color.White.copy(alpha = 0.7f) else primaryThemeColor
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = msg.message,
                                            fontSize = 12.sp,
                                            color = if (isUser) Color.White else WhiteSoft,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        if (aiLoading) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFF1F5F9))
                                            .padding(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = primaryThemeColor
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Warrior AI is processing secure response...",
                                                fontSize = 11.sp,
                                                color = GrayMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        aiError?.let { err ->
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "Secure AI Node Exception: $err",
                                        fontSize = 12.sp,
                                        color = Color(0xFF991B1B)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderColor)

                // Input bar
                Surface(
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customPromptInput,
                            onValueChange = { customPromptInput = it },
                            placeholder = { Text("Ask anything about this page or privacy...", fontSize = 12.sp, color = GrayMuted) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_prompt_input_field"),
                            maxLines = 3,
                            singleLine = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = primaryThemeColor,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = WhiteSoft,
                                unfocusedTextColor = WhiteSoft
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        FloatingActionButton(
                            onClick = {
                                if (customPromptInput.isNotBlank()) {
                                    viewModel.queryGemini(customPromptInput.trim())
                                    customPromptInput = ""
                                    focusManager.clearFocus()
                                }
                            },
                            containerColor = primaryThemeColor,
                            contentColor = Color.White,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("ai_send_prompt_btn"),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionChip(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(136.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(0.5.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WhiteSoft)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 9.sp, color = GrayMuted)
        }
    }
}
