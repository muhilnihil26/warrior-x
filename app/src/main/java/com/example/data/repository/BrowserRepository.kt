package com.example.data.repository

import com.example.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class BrowserRepository(
    private val bookmarkDao: BookmarkDao,
    private val historyDao: HistoryDao,
    private val tabDao: TabDao,
    private val syncSettingsDao: SyncSettingsDao
) {
    val bookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()
    val history: Flow<List<HistoryEntity>> = historyDao.getAllHistory()
    val tabs: Flow<List<TabEntity>> = tabDao.getAllTabs()
    val syncSettings: Flow<SyncSettingsEntity?> = syncSettingsDao.getSyncSettings()

    suspend fun insertBookmark(title: String, url: String, passphrase: String) = withContext(Dispatchers.IO) {
        val hasPassphrase = passphrase.isNotEmpty()
        val (encTitle, encUrl) = if (hasPassphrase) {
            val spec = EncryptionUtils.deriveKey(passphrase)
            EncryptionUtils.encrypt(title, spec) to EncryptionUtils.encrypt(url, spec)
        } else {
            "" to ""
        }

        bookmarkDao.insertBookmark(
            BookmarkEntity(
                title = if (hasPassphrase) "[Encrypted]" else title,
                url = if (hasPassphrase) "[Encrypted]" else url,
                isEncrypted = hasPassphrase,
                encryptedTitle = encTitle,
                encryptedUrl = encUrl,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteBookmarkById(id: Int) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteBookmarkById(id)
    }

    suspend fun clearAllBookmarks() = withContext(Dispatchers.IO) {
        bookmarkDao.clearAllBookmarks()
    }

    suspend fun insertHistory(title: String, url: String, category: String, passphrase: String) = withContext(Dispatchers.IO) {
        val hasPassphrase = passphrase.isNotEmpty()
        val (encTitle, encUrl) = if (hasPassphrase) {
            val spec = EncryptionUtils.deriveKey(passphrase)
            EncryptionUtils.encrypt(title, spec) to EncryptionUtils.encrypt(url, spec)
        } else {
            "" to ""
        }

        historyDao.insertHistory(
            HistoryEntity(
                title = if (hasPassphrase) "[Encrypted Title]" else title,
                url = if (hasPassphrase) "[Encrypted URL]" else url,
                isEncrypted = hasPassphrase,
                encryptedTitle = encTitle,
                encryptedUrl = encUrl,
                category = category,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        historyDao.clearHistory()
    }

    suspend fun createTab(title: String, url: String): Int = withContext(Dispatchers.IO) {
        tabDao.deactivateAllTabs()
        val id = tabDao.insertTab(
            TabEntity(
                title = title,
                url = url,
                isActive = true,
                timestamp = System.currentTimeMillis()
            )
        )
        id.toInt()
    }

    suspend fun updateTab(id: Int, title: String, url: String) = withContext(Dispatchers.IO) {
        tabDao.updateTab(
            TabEntity(
                id = id,
                title = title,
                url = url,
                isActive = true,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun activateTab(id: Int) = withContext(Dispatchers.IO) {
        tabDao.deactivateAllTabs()
        tabDao.activateTab(id)
    }

    suspend fun deleteTabById(id: Int) = withContext(Dispatchers.IO) {
        tabDao.deleteTabById(id)
    }

    suspend fun updateSyncSettings(settings: SyncSettingsEntity) = withContext(Dispatchers.IO) {
        syncSettingsDao.insertOrUpdateSyncSettings(settings)
    }

    suspend fun getSyncSettingsDirect(): SyncSettingsEntity {
        return withContext(Dispatchers.IO) {
            syncSettingsDao.getSyncSettingsDirect() ?: SyncSettingsEntity()
        }
    }
}
