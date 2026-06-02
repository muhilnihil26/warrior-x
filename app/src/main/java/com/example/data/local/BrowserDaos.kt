package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Int)

    @Query("DELETE FROM bookmarks")
    suspend fun clearAllBookmarks()
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int): List<HistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Delete
    suspend fun deleteHistory(history: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clearHistory()
}

@Dao
interface TabDao {
    @Query("SELECT * FROM tabs ORDER BY timestamp ASC")
    fun getAllTabs(): Flow<List<TabEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: TabEntity): Long

    @Update
    suspend fun updateTab(tab: TabEntity)

    @Delete
    suspend fun deleteTab(tab: TabEntity)

    @Query("DELETE FROM tabs WHERE id = :id")
    suspend fun deleteTabById(id: Int)

    @Query("UPDATE tabs SET isActive = 0")
    suspend fun deactivateAllTabs()

    @Query("UPDATE tabs SET isActive = 1 WHERE id = :id")
    suspend fun activateTab(id: Int)
}

@Dao
interface SyncSettingsDao {
    @Query("SELECT * FROM sync_settings WHERE id = 1")
    fun getSyncSettings(): Flow<SyncSettingsEntity?>

    @Query("SELECT * FROM sync_settings WHERE id = 1")
    suspend fun getSyncSettingsDirect(): SyncSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSyncSettings(settings: SyncSettingsEntity)
}
