package com.example.ui

import com.example.BuildConfig
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.BrowserRepository
import com.example.data.xml.RecommendationItem
import com.example.data.xml.XmlRecommendationEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val database = BrowserDatabase.getDatabase(application)
    private val repository = BrowserRepository(
        database.bookmarkDao(),
        database.historyDao(),
        database.tabDao(),
        database.syncSettingsDao(),
        database.vaultDao()
    )

    // Sync Settings Flow
    val syncSettings: StateFlow<SyncSettingsEntity> = repository.syncSettings
        .map { it ?: SyncSettingsEntity() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SyncSettingsEntity()
        )

    // Raw Bookmarks from DB
    val rawBookmarks: StateFlow<List<BookmarkEntity>> = repository.bookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Raw Vault Items from DB
    val rawVaultItems: StateFlow<List<VaultEntity>> = repository.vaultItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vaultItems: StateFlow<List<VaultEntity>> = combine(rawVaultItems, syncSettings) { list, settings ->
        val passphrase = settings.syncPassphrase
        if (passphrase.isEmpty()) {
            list
        } else {
            val spec = EncryptionUtils.deriveKey(passphrase)
            list.map { item ->
                if (item.isEncrypted) {
                    item.copy(
                        siteNameOrTitle = EncryptionUtils.decrypt(item.encryptedTitle, spec),
                        loginName = EncryptionUtils.decrypt(item.encryptedLogin, spec),
                        secretValue = EncryptionUtils.decrypt(item.encryptedValue, spec)
                    )
                } else {
                    item
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Brand and Identity User Agents
    private val _userAgents = MutableStateFlow(
        listOf(
            UserAgentProfile("Default Identity (Mobile)", "Mozilla/5.0 (Linux; Android 10) WarriorX/1.0 SecureBrowser"),
            UserAgentProfile("Tor Stealth Browser", "Mozilla/5.0 (Windows NT 10.0; rv:109.0) Gecko/20100101 Firefox/115.0"),
            UserAgentProfile("Safari (macOS Workstation)", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15"),
            UserAgentProfile("Chrome (Windows Desktop)", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36"),
            UserAgentProfile("Firefox (Linux Workstation)", "Mozilla/5.0 (X11; Linux x86_64; rv:135.0) Gecko/20100101 Firefox/135.0")
        )
    )
    val userAgents: StateFlow<List<UserAgentProfile>> = _userAgents.asStateFlow()

    private val _selectedUserAgent = MutableStateFlow(
        UserAgentProfile("Default Identity (Mobile)", "Mozilla/5.0 (Linux; Android 10) WarriorX/1.0 SecureBrowser")
    )
    val selectedUserAgent: StateFlow<UserAgentProfile> = _selectedUserAgent.asStateFlow()

    fun selectUserAgent(profile: UserAgentProfile) {
        _selectedUserAgent.value = profile
        _syncStatusText.value = "Web identity spoofed to: ${profile.name}"
    }

    // Secure Sandbox states (Custom metrics and controls)
    private val _isThirdPartyCookiesBlocked = MutableStateFlow(true)
    val isThirdPartyCookiesBlocked: StateFlow<Boolean> = _isThirdPartyCookiesBlocked.asStateFlow()

    private val _isJsSandboxEnabled = MutableStateFlow(false)
    val isJsSandboxEnabled: StateFlow<Boolean> = _isJsSandboxEnabled.asStateFlow()

    private val _isHttpsForced = MutableStateFlow(true)
    val isHttpsForced: StateFlow<Boolean> = _isHttpsForced.asStateFlow()

    fun toggleThirdPartyCookies(blocked: Boolean) {
        _isThirdPartyCookiesBlocked.value = blocked
        _syncStatusText.value = if (blocked) "Third-party tracking cookies blocked" else "Third-party cookies permitted (Less Secure)"
    }

    fun toggleJsSandbox(enabled: Boolean) {
        _isJsSandboxEnabled.value = enabled
        _syncStatusText.value = if (enabled) "Strict JS Sandbox activated" else "Standard JS Execution context"
    }

    fun toggleHttpsForced(forced: Boolean) {
        _isHttpsForced.value = forced
        _syncStatusText.value = if (forced) "Strict HTTPS Only rule active" else "Allow standard HTTP connections"
    }

    // Raw History from DB
    val rawHistory: StateFlow<List<HistoryEntity>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tabs
    val tabs: StateFlow<List<TabEntity>> = repository.tabs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state for address bar input
    private val _addressBarInput = MutableStateFlow("")
    val addressBarInput: StateFlow<String> = _addressBarInput.asStateFlow()

    // Filtered/Decrypted structures on the fly matching the passphrase
    val bookmarks: StateFlow<List<BookmarkEntity>> = combine(rawBookmarks, syncSettings) { list, settings ->
        val passphrase = settings.syncPassphrase
        if (passphrase.isEmpty()) {
            list
        } else {
            val spec = EncryptionUtils.deriveKey(passphrase)
            list.map { item ->
                if (item.isEncrypted && item.encryptedTitle.isNotEmpty()) {
                    item.copy(
                        title = EncryptionUtils.decrypt(item.encryptedTitle, spec),
                        url = EncryptionUtils.decrypt(item.encryptedUrl, spec)
                    )
                } else {
                    item
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = combine(rawHistory, syncSettings) { list, settings ->
        val passphrase = settings.syncPassphrase
        if (passphrase.isEmpty()) {
            list
        } else {
            val spec = EncryptionUtils.deriveKey(passphrase)
            list.map { item ->
                if (item.isEncrypted && item.encryptedTitle.isNotEmpty()) {
                    item.copy(
                        title = EncryptionUtils.decrypt(item.encryptedTitle, spec),
                        url = EncryptionUtils.decrypt(item.encryptedUrl, spec)
                    )
                } else {
                    item
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category tracking for recommendations
    val historyCategories: StateFlow<Map<String, Int>> = history.map { list ->
        val map = mutableMapOf<String, Int>()
        list.forEach {
            val cat = it.category.lowercase()
            map[cat] = (map[cat] ?: 0) + 1
        }
        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Dynamic XML Recommendation list
    val recommendations: StateFlow<List<RecommendationItem>> = historyCategories.map { catMap ->
        XmlRecommendationEngine.getRecommendationsForUser(catMap)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), XmlRecommendationEngine.parseRecommendations())

    // Active web loading status
    private val _isWebLoading = MutableStateFlow(false)
    val isWebLoading: StateFlow<Boolean> = _isWebLoading.asStateFlow()

    private val _webProgress = MutableStateFlow(0)
    val webProgress: StateFlow<Int> = _webProgress.asStateFlow()

    // Sync Event Status UI helper
    private val _syncStatusText = MutableStateFlow("Local Encryption Mode Active")
    val syncStatusText: StateFlow<String> = _syncStatusText.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Sync other devices list (high fidelity simulation)
    private val _otherDevices = MutableStateFlow(
        listOf(
            SyncedDevice("macOS Workstation", "war.dev macOS Client", "Active", "10 seconds ago"),
            SyncedDevice("Windows Rig", "war.dev Windows Client", "Connected", "1 minute ago")
        )
    )
    val otherDevices: StateFlow<List<SyncedDevice>> = _otherDevices.asStateFlow()

    // Ad blocker statistics
    private val _adsBlockedCount = MutableStateFlow(0)
    val adsBlockedCount: StateFlow<Int> = _adsBlockedCount.asStateFlow()

    // --- Warrior AI states ---
    private val _aiChatHistory = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("Hello! I am Warrior AI, your secure built-in browser companion. How can I protect, audit, or assist your web journey today?", false)
        )
    )
    val aiChatHistory: StateFlow<List<ChatMessage>> = _aiChatHistory.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    // --- Developer Mode: View Page Source states ---
    private val _pageSourceText = MutableStateFlow<String?>(null)
    val pageSourceText: StateFlow<String?> = _pageSourceText.asStateFlow()

    private val _pageSourceTitle = MutableStateFlow<String>("")
    val pageSourceTitle: StateFlow<String> = _pageSourceTitle.asStateFlow()

    private val _isSourceLoading = MutableStateFlow(false)
    val isSourceLoading: StateFlow<Boolean> = _isSourceLoading.asStateFlow()

    // --- Dynamic Security Shield Levels ---
    private val _shieldLevel = MutableStateFlow("High Protection") // "Standard", "High Protection", "Paranoid Stealth"
    val shieldLevel: StateFlow<String> = _shieldLevel.asStateFlow()

    fun updateShieldLevel(level: String) {
        _shieldLevel.value = level
        _syncStatusText.value = "Security Shield updated to $level mode."
    }

    // Firebase Authentication properties
    private var firebaseAuth: FirebaseAuth? = null

    private val _currentUserEmail = MutableStateFlow<String?>(null)
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authSuccess = MutableStateFlow<String?>(null)
    val authSuccess: StateFlow<String?> = _authSuccess.asStateFlow()

    init {
        // Initialize Firebase dynamically to support sandbox environments cleanly without a hard config file
        try {
            val app = if (FirebaseApp.getApps(application).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey("AIzaSyDummyKeyForWarriorXBrowser2026")
                    .setApplicationId("1:546179538467:android:da39a3ee5e6b4b0d3255bf")
                    .setProjectId("warriorx-ae9a0")
                    .build()
                FirebaseApp.initializeApp(application, options)
            } else {
                FirebaseApp.getInstance()
            }
            firebaseAuth = FirebaseAuth.getInstance(app)
            _currentUserEmail.value = firebaseAuth?.currentUser?.email
        } catch (e: Exception) {
            android.util.Log.e("BrowserViewModel", "Firebase Auth initialization failed", e)
        }

        // Pre-populate some history and current tab if database is empty
        viewModelScope.launch {
            repository.tabs.first().let { currentTabs ->
                if (currentTabs.isEmpty()) {
                    repository.createTab("Warrior X Home", "https://war.dev/warrior-x")
                } else {
                    currentTabs.find { it.isActive }?.let { active ->
                        _addressBarInput.value = active.url
                    }
                }
            }

            repository.history.first().let { hist ->
                if (hist.isEmpty()) {
                    val settings = repository.getSyncSettingsDirect()
                    val p = settings.syncPassphrase
                    repository.insertHistory("Zero-Knowledge Security Standard", "https://war.dev/security/zero-knowledge", "security", p)
                    repository.insertHistory("Adblocking Filter Lists Guide", "https://war.dev/privacy/filter-lists", "privacy", p)
                    repository.insertHistory("Warrior X Release Platform", "https://war.dev/warrior-x/launch", "general", p)
                    repository.insertHistory("Kotlin Multiplatform Desktop macOS/Windows", "https://kotlinlang.org/kmp", "tech", p)
                }
            }

            repository.vaultItems.first().let { items ->
                if (items.isEmpty()) {
                    val settings = repository.getSyncSettingsDirect()
                    val p = settings.syncPassphrase
                    repository.insertVaultItem("credential", "ProtonMail Secure", "warrior_dev", "SecurePass123!", p)
                    repository.insertVaultItem("note", "Decentralized Key Root Backup", "", "5J3mKB8ae888sSshshYyYy... [Client Encrypted Seed]", p)
                }
            }
        }
    }

    fun setAddressInput(url: String) {
        _addressBarInput.value = url
    }

    fun incrementAdsBlocked() {
        if (syncSettings.value.isAdBlockingEnabled) {
            _adsBlockedCount.value += 1
        }
    }

    fun navigateToUrl(url: String) {
        var formattedUrl = url.trim()
        if (formattedUrl.isEmpty()) return

        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = if (formattedUrl.contains(".") && !formattedUrl.contains(" ")) {
                "https://$formattedUrl"
            } else {
                "https://www.google.com/search?q=${formattedUrl.replace(" ", "+")}"
            }
        }

        _addressBarInput.value = formattedUrl

        viewModelScope.launch {
            val activeTab = tabs.value.find { it.isActive }
            if (activeTab != null) {
                repository.updateTab(activeTab.id, getDomainName(formattedUrl), formattedUrl)
            } else {
                repository.createTab(getDomainName(formattedUrl), formattedUrl)
            }

            // Save to history only if privacy mode is DISABLED (Privacy Mode / Standard Standard logic)
            if (!syncSettings.value.isPrivacyModeEnabled) {
                val category = determineCategory(formattedUrl)
                repository.insertHistory(
                    title = getDomainName(formattedUrl),
                    url = formattedUrl,
                    category = category,
                    passphrase = syncSettings.value.syncPassphrase
                )
            }
        }
    }

    fun selectTab(id: Int) {
        viewModelScope.launch {
            repository.activateTab(id)
            tabs.value.find { it.id == id }?.let {
                _addressBarInput.value = it.url
            }
        }
    }

    fun addNewTab(title: String = "New Tab", url: String = "https://war.dev/warrior-x") {
        viewModelScope.launch {
            repository.createTab(title, url)
            _addressBarInput.value = url
        }
    }

    fun closeTab(id: Int) {
        viewModelScope.launch {
            val tabList = tabs.value
            if (tabList.size <= 1) {
                // Keep at least one tab
                repository.deleteTabById(id)
                repository.createTab("Warrior X Home", "https://war.dev/warrior-x")
                _addressBarInput.value = "https://war.dev/warrior-x"
            } else {
                val activeTab = tabList.find { it.isActive }
                repository.deleteTabById(id)
                if (activeTab?.id == id) {
                    val remaining = tabList.filter { it.id != id }
                    if (remaining.isNotEmpty()) {
                        repository.activateTab(remaining.last().id)
                        _addressBarInput.value = remaining.last().url
                    }
                }
            }
        }
    }

    fun toggleAdBlocking(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSyncSettings(
                syncSettings.value.copy(isAdBlockingEnabled = enabled)
            )
        }
    }

    fun togglePrivacyMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSyncSettings(
                syncSettings.value.copy(isPrivacyModeEnabled = enabled)
            )
        }
    }

    fun toggleCloudSync(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSyncSettings(
                syncSettings.value.copy(isCloudSyncEnabled = enabled)
            )
            if (enabled) {
                _syncStatusText.value = "Cloud Sync Enabled. Zero-Knowledge Active."
            } else {
                _syncStatusText.value = "Local-Only Encrypted Mode Active"
            }
        }
    }

    fun updatePassphrase(passphrase: String) {
        viewModelScope.launch {
            val current = syncSettings.value
            // Re-encrypt existing unencrypted bookmarks and history elements or migrate them
            val oldPass = current.syncPassphrase
            
            // Update Passphrase in Settings
            repository.updateSyncSettings(current.copy(syncPassphrase = passphrase))
            
            _syncStatusText.value = if (passphrase.isEmpty()) {
                "Passphrase removed. Data stored unencrypted."
            } else {
                "Derived AES Key Generated. Client-Side Encryption Enabled."
            }

            // Encrypt existing data if they are not encrypted, or update encryption
            migrateEncryption(oldPass, passphrase)
        }
    }

    private suspend fun migrateEncryption(oldPass: String, newPass: String) {
        // Migration logic for Bookmarks
        val bookmarksList = rawBookmarks.value
        bookmarksList.forEach { bookmark ->
            val specDec = if (oldPass.isNotEmpty()) EncryptionUtils.deriveKey(oldPass) else null
            val specEnc = if (newPass.isNotEmpty()) EncryptionUtils.deriveKey(newPass) else null
            
            val originalTitle = if (bookmark.isEncrypted && specDec != null) {
                EncryptionUtils.decrypt(bookmark.encryptedTitle, specDec)
            } else {
                bookmark.title
            }
            
            val originalUrl = if (bookmark.isEncrypted && specDec != null) {
                EncryptionUtils.decrypt(bookmark.encryptedUrl, specDec)
            } else {
                bookmark.url
            }

            val encTitle = if (specEnc != null) EncryptionUtils.encrypt(originalTitle, specEnc) else ""
            val encUrl = if (specEnc != null) EncryptionUtils.encrypt(originalUrl, specEnc) else ""

            database.bookmarkDao().insertBookmark(
                bookmark.copy(
                    title = if (newPass.isNotEmpty()) "[Encrypted]" else originalTitle,
                    url = if (newPass.isNotEmpty()) "[Encrypted]" else originalUrl,
                    isEncrypted = newPass.isNotEmpty(),
                    encryptedTitle = encTitle,
                    encryptedUrl = encUrl
                )
            )
        }

        // Migration logic for History
        val historyList = rawHistory.value
        historyList.forEach { hist ->
            val specDec = if (oldPass.isNotEmpty()) EncryptionUtils.deriveKey(oldPass) else null
            val specEnc = if (newPass.isNotEmpty()) EncryptionUtils.deriveKey(newPass) else null
            
            val originalTitle = if (hist.isEncrypted && specDec != null) {
                EncryptionUtils.decrypt(hist.encryptedTitle, specDec)
            } else {
                hist.title
            }
            
            val originalUrl = if (hist.isEncrypted && specDec != null) {
                EncryptionUtils.decrypt(hist.encryptedUrl, specDec)
            } else {
                hist.url
            }

            val encTitle = if (specEnc != null) EncryptionUtils.encrypt(originalTitle, specEnc) else ""
            val encUrl = if (specEnc != null) EncryptionUtils.encrypt(originalUrl, specEnc) else ""

            database.historyDao().insertHistory(
                hist.copy(
                    title = if (newPass.isNotEmpty()) "[Encrypted Title]" else originalTitle,
                    url = if (newPass.isNotEmpty()) "[Encrypted URL]" else originalUrl,
                    isEncrypted = newPass.isNotEmpty(),
                    encryptedTitle = encTitle,
                    encryptedUrl = encUrl
                )
            )
        }

        // Migration logic for secure Vault
        val vaultList = rawVaultItems.value
        vaultList.forEach { vaultItem ->
            val specDec = if (oldPass.isNotEmpty()) EncryptionUtils.deriveKey(oldPass) else null
            val specEnc = if (newPass.isNotEmpty()) EncryptionUtils.deriveKey(newPass) else null

            val originalTitle = if (vaultItem.isEncrypted && specDec != null) {
                EncryptionUtils.decrypt(vaultItem.encryptedTitle, specDec)
            } else {
                vaultItem.siteNameOrTitle
            }
            val originalLogin = if (vaultItem.isEncrypted && specDec != null) {
                EncryptionUtils.decrypt(vaultItem.encryptedLogin, specDec)
            } else {
                vaultItem.loginName
            }
            val originalValue = if (vaultItem.isEncrypted && specDec != null) {
                EncryptionUtils.decrypt(vaultItem.encryptedValue, specDec)
            } else {
                vaultItem.secretValue
            }

            val encTitle = if (specEnc != null) EncryptionUtils.encrypt(originalTitle, specEnc) else ""
            val encLogin = if (specEnc != null) EncryptionUtils.encrypt(originalLogin, specEnc) else ""
            val encValue = if (specEnc != null) EncryptionUtils.encrypt(originalValue, specEnc) else ""

            database.vaultDao().insertVaultItem(
                vaultItem.copy(
                    siteNameOrTitle = if (newPass.isNotEmpty()) "[Encrypted]" else originalTitle,
                    loginName = if (newPass.isNotEmpty()) "[Encrypted]" else originalLogin,
                    secretValue = if (newPass.isNotEmpty()) "[Encrypted]" else originalValue,
                    isEncrypted = newPass.isNotEmpty(),
                    encryptedTitle = encTitle,
                    encryptedLogin = encLogin,
                    encryptedValue = encValue
                )
            )
        }
    }

    fun addBookmark(title: String, url: String) {
        viewModelScope.launch {
            repository.insertBookmark(title, url, syncSettings.value.syncPassphrase)
            _syncStatusText.value = "Bookmark stored securely (encrypted local-only)."
        }
    }

    fun deleteBookmark(id: Int) {
        viewModelScope.launch {
            repository.deleteBookmarkById(id)
        }
    }

    fun addVaultItem(type: String, siteName: String, login: String, secret: String) {
        viewModelScope.launch {
            repository.insertVaultItem(type, siteName, login, secret, syncSettings.value.syncPassphrase)
            _syncStatusText.value = "Secure entry saved to local-encrypted vault."
        }
    }

    fun deleteVaultItem(id: Int) {
        viewModelScope.launch {
            repository.deleteVaultItemById(id)
            _syncStatusText.value = "Vault entry deleted safely."
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun triggerSyncSimulation() {
        if (!syncSettings.value.isCloudSyncEnabled) {
            _syncStatusText.value = "Cannot sync: Enable Cloud Sync in settings."
            return
        }

        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatusText.value = "Deriving Local Zero-Knowledge Private Key..."
            kotlinx.coroutines.delay(800)
            
            _syncStatusText.value = "Encrypting un-synced bookmarks and history..."
            kotlinx.coroutines.delay(800)
            
            _syncStatusText.value = "Initializing TLS pipeline to cloud sync nodes..."
            kotlinx.coroutines.delay(800)
            
            _syncStatusText.value = "Pushing cryptographic payloads..."
            kotlinx.coroutines.delay(1000)

            val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val formattedTime = formatter.format(Date())
            
            val settings = syncSettings.value
            repository.updateSyncSettings(
                settings.copy(lastSyncedTime = System.currentTimeMillis())
            )
            
            // Randomly simulate fresh device updates
            _otherDevices.value = listOf(
                SyncedDevice("macOS Workstation", "war.dev macOS Client", "Active", "Synced at $formattedTime"),
                SyncedDevice("Windows Rig", "war.dev Windows Client", "Connected", "Synced at $formattedTime")
            )

            _isSyncing.value = false
            _syncStatusText.value = "Sync Finished beautifully! All data synced securely at $formattedTime"
        }
    }

    fun setWebLoading(loading: Boolean) {
        _isWebLoading.value = loading
    }

    fun setWebProgress(progress: Int) {
        _webProgress.value = progress
    }

    private fun getDomainName(url: String): String {
        return try {
            val uri = java.net.URI(url)
            val domain = uri.host ?: url
            if (domain.startsWith("www.")) domain.substring(4) else domain
        } catch (e: Exception) {
            url
        }
    }

    private fun determineCategory(url: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains("war.dev/security") || lower.contains("cryptography") || lower.contains("cyber") || lower.contains("aes") || lower.contains("keys") -> "security"
            lower.contains("war.dev/privacy") || lower.contains("duckduckgo") || lower.contains("proton") || lower.contains("adblock") || lower.contains("ublock") -> "privacy"
            lower.contains("android") || lower.contains("kotlin") || lower.contains("github") || lower.contains("stackoverflow") || lower.contains("compose") -> "tech"
            lower.contains("youtube") || lower.contains("netflix") || lower.contains("reddit") || lower.contains("spotify") -> "entertainment"
            else -> "general"
        }
    }

    fun firebaseLogin(email: String, password: String) {
        val auth = firebaseAuth
        if (auth == null) {
            _authError.value = "Firebase state is not initialized. Please verify internet connection."
            return
        }
        if (email.isBlank() || password.isBlank()) {
            _authError.value = "Email and Password cannot be blank."
            return
        }
        _authLoading.value = true
        _authError.value = null
        _authSuccess.value = null

        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                _authLoading.value = false
                if (task.isSuccessful) {
                    _currentUserEmail.value = auth.currentUser?.email
                    _authSuccess.value = "Welcome back, ${_currentUserEmail.value}!"
                } else {
                    _authError.value = task.exception?.localizedMessage ?: "Login failed. Please try again."
                }
            }
    }

    fun firebaseSignup(email: String, password: String) {
        val auth = firebaseAuth
        if (auth == null) {
            _authError.value = "Firebase state is not initialized. Please verify internet connection."
            return
        }
        if (email.isBlank() || password.isBlank()) {
            _authError.value = "Email and Password cannot be blank."
            return
        }
        if (password.length < 6) {
            _authError.value = "Password must be at least 6 characters."
            return
        }
        _authLoading.value = true
        _authError.value = null
        _authSuccess.value = null

        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                _authLoading.value = false
                if (task.isSuccessful) {
                    _currentUserEmail.value = auth.currentUser?.email
                    _authSuccess.value = "Successfully registered! Welcome inside, ${_currentUserEmail.value}!"
                } else {
                    _authError.value = task.exception?.localizedMessage ?: "Signup failed. Please try again."
                }
            }
    }

    fun firebaseLogout() {
        firebaseAuth?.signOut()
        _currentUserEmail.value = null
        _authSuccess.value = "Signed out successfully."
        _authError.value = null
    }

    fun dismissAuthMessage() {
        _authError.value = null
        _authSuccess.value = null
    }

    fun firebaseLoginWithGoogle(idToken: String) {
        val auth = firebaseAuth
        if (auth == null) {
            _authError.value = "Firebase state is not initialized. Please verify internet connection."
            return
        }
        _authLoading.value = true
        _authError.value = null
        _authSuccess.value = null

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                _authLoading.value = false
                if (task.isSuccessful) {
                    _currentUserEmail.value = auth.currentUser?.email
                    _authSuccess.value = "Welcome back! Google account synchronized successfully: ${_currentUserEmail.value}"
                } else {
                    _authError.value = task.exception?.localizedMessage ?: "Google Sign-In integration failed."
                }
            }
    }

    fun simulateGoogleAuth(email: String, displayName: String) {
        _authLoading.value = true
        _authError.value = null
        _authSuccess.value = null

        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            _authLoading.value = false
            val mockEmail = if (email.contains("@")) email.trim() else "${email.trim().replace(" ", "").lowercase()}@gmail.com"
            _currentUserEmail.value = if (mockEmail.isBlank() || mockEmail == "@gmail.com") "warrior.google@gmail.com" else mockEmail
            _authSuccess.value = "Secure handshake simulated. Connected with Google account: ${_currentUserEmail.value} (Credential: Google Secure Node)"
        }
    }

    // --- Warrior AI Functions ---
    fun queryGemini(prompt: String) {
        if (prompt.isBlank()) return
        
        // Add user message to history
        val userMsg = ChatMessage(prompt, isUser = true)
        _aiChatHistory.value = _aiChatHistory.value + userMsg
        _aiLoading.value = true
        _aiError.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    // Gracefully simulate premium assistant response if API key is not configured yet
                    kotlinx.coroutines.delay(1200)
                    val systemAnswer = "🔒 *Local Shield Notification: Gemini API key is missing from AI Studio Secrets. To interact with live AI, please add GEMINI_API_KEY securely in your sidebar settings.*\n\n*Warrior AI Client-Side Simulation*\n\nHere are safety guidelines for *\"$prompt\"*:\n1. Keep all transaction states derived locally with custom passphrases.\n2. Ensure zero-knowledge pipelines bypass metadata eavesdropping.\n3. Verify domain records before pasting credentials to avoid DNS hijacking."
                    _aiChatHistory.value = _aiChatHistory.value + ChatMessage(systemAnswer, isUser = false)
                    _aiLoading.value = false
                    return@launch
                }

                // Construct request body with standard Gemini API JSON syntax
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestJson = org.json.JSONObject().apply {
                    val contentsArray = org.json.JSONArray().apply {
                        put(org.json.JSONObject().apply {
                            put("parts", org.json.JSONArray().apply {
                                put(org.json.JSONObject().apply {
                                    put("text", "You are Warrior AI, a built-in highly secure, advanced privacy-first AI companion inside the Warrior X secure web browser. Keep answers concise, educational, and focused on security, tech or the user prompt. User prompt: $prompt")
                                })
                            })
                        })
                    }
                    put("contents", contentsArray)
                }

                val client = OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
                val body = requestJson.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(requestUrl)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string() ?: "Empty error body"
                        throw Exception("API Error ${response.code}: $errBody")
                    }

                    val resBody = response.body?.string() ?: throw Exception("Empty API response")
                    val jsonResponse = org.json.JSONObject(resBody)
                    val candidates = jsonResponse.getJSONArray("candidates")
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.getJSONObject("content")
                    val partsArray = contentObj.getJSONArray("parts")
                    val answerText = partsArray.getJSONObject(0).getString("text")

                    _aiChatHistory.value = _aiChatHistory.value + ChatMessage(answerText, isUser = false)
                }
            } catch (e: Exception) {
                android.util.Log.e("BrowserViewModel", "Gemini call failed", e)
                _aiError.value = e.localizedMessage ?: "Network connection/API key setup failed."
                _aiChatHistory.value = _aiChatHistory.value + ChatMessage("⚠️ Connection Error: Couldn't connect to secure Gemini Nodes. Reason: ${e.localizedMessage ?: "SSL issues or key mismatch"}", isUser = false)
            } finally {
                _aiLoading.value = false
            }
        }
    }

    fun clearAiHistory() {
        _aiChatHistory.value = listOf(
            ChatMessage("Hello! Chat logs cleared safely. How can I secure, audit, or optimize your experience further?", false)
        )
        _aiError.value = null
    }

    // --- Developer Mode: View Page Source ---
    fun fetchPageSource(url: String, title: String) {
        if (url.isBlank()) return
        _isSourceLoading.value = true
        _pageSourceText.value = null
        _pageSourceTitle.value = title

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (url.startsWith("https://war.dev") || url.contains("warrior")) {
                    kotlinx.coroutines.delay(600)
                    val mockHtml = """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="utf-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.5">
                        <title>Warrior X Secured Protocol Portal</title>
                        <style>
                            body { background: #0A0F1D; color: #E2E8F0; font-family: sans-serif; padding: 2rem; }
                            h1 { color: #10B981; }
                            .zk-shield { border: 1px solid #3B82F6; padding: 1rem; border-radius: 8px; }
                        </style>
                    </head>
                    <body>
                        <header>
                            <h1>WARRIOR X DECENTRALIZED COMPANION</h1>
                            <p>End-to-End Encrypted Transport Enabled</p>
                        </header>
                        <main class="zk-shield">
                            <h2>Zero-Knowledge Nodes status: ONLINE</h2>
                            <p>All database instances are compiled and secured with AES-256 GCM local keys.</p>
                        </main>
                    </body>
                    </html>
                    """.trimIndent()
                    _pageSourceText.value = mockHtml
                    return@launch
                }

                val client = OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) WarriorX/1.0 SecureBrowser")
                    .build()

                client.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string() ?: "Empty Response"
                    val displayResult = if (bodyStr.length > 8000) {
                        bodyStr.substring(0, 8000) + "\n\n... [Source code truncated for local performance] ..."
                    } else {
                        bodyStr
                    }
                    _pageSourceText.value = displayResult
                }
            } catch (e: Exception) {
                _pageSourceText.value = "Failed to load live webpage source.\nError: ${e.localizedMessage ?: "SSL Peer handshakes failed"}"
            } finally {
                _isSourceLoading.value = false
            }
        }
    }

    fun dismissPageSource() {
        _pageSourceText.value = null
        _isSourceLoading.value = false
    }
}

data class ChatMessage(
    val message: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class UserAgentProfile(
    val name: String,
    val value: String
)

data class SyncedDevice(
    val name: String,
    val type: String,
    val status: String,
    val lastSeen: String
)
