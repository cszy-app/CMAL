package com.cszyapp.cmal.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface McVersionDao {
    @Query("SELECT * FROM mc_versions ORDER BY versionCode DESC")
    fun observeAll(): Flow<List<McVersion>>

    @Query("SELECT * FROM mc_versions WHERE versionCode = :code")
    suspend fun get(code: Int): McVersion?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(version: McVersion)

    @Delete
    suspend fun delete(version: McVersion)

    @Query("DELETE FROM mc_versions")
    suspend fun clear()

    @Query("SELECT * FROM mc_versions WHERE downloaded = 1 ORDER BY versionCode DESC LIMIT 1")
    suspend fun latestDownloaded(): McVersion?
}

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY featured DESC, id DESC")
    fun observeAll(): Flow<List<McServer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(server: McServer): Long

    @Delete
    suspend fun delete(server: McServer)
}

@Dao
interface SkinDao {
    @Query("SELECT * FROM skins ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<McSkin>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(skin: McSkin): Long

    @Delete
    suspend fun delete(skin: McSkin)
}

@Dao
interface WorldDao {
    @Query("SELECT * FROM worlds ORDER BY importedAt DESC")
    fun observeAll(): Flow<List<McWorld>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(world: McWorld): Long

    @Delete
    suspend fun delete(world: McWorld)
}

@Dao
interface ResourceDao {
    @Query("SELECT * FROM resources ORDER BY importedAt DESC")
    fun observeAll(): Flow<List<McResource>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(resource: McResource): Long

    @Delete
    suspend fun delete(resource: McResource)
}

@Dao
interface DownloadTaskDao {
    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: DownloadTask)

    @Delete
    suspend fun delete(task: DownloadTask)
}
