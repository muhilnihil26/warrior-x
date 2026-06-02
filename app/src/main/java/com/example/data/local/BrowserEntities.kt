package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val isEncrypted: Boolean = false,
    val encryptedTitle: String = "",
    val encryptedUrl: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val isEncrypted: Boolean = false,
    val encryptedTitle: String = "",
    val encryptedUrl: String = "",
    val category: String = "general", // "tech", "security", "privacy", "general", "entertainment"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val isActive: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_settings")
data class SyncSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val deviceName: String = "Android Device",
    val syncPassphrase: String = "",
    val isCloudSyncEnabled: Boolean = false,
    val isAdBlockingEnabled: Boolean = true,
    val isPrivacyModeEnabled: Boolean = false,
    val lastSyncedTime: Long = 0L
)

@Entity(tableName = "secure_vault")
data class VaultEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "credential" or "note"
    val siteNameOrTitle: String, // Decrypted title if plaintext
    val loginName: String = "", // Decrypted username
    val secretValue: String = "", // Decrypted password/payload
    val isEncrypted: Boolean = false,
    val encryptedTitle: String = "",
    val encryptedLogin: String = "",
    val encryptedValue: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

