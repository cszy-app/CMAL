package com.cszyapp.cmal.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

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
