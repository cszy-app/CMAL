package com.cszyapp.cmal.data.repo

import com.cszyapp.cmal.data.db.AppDatabase
import com.cszyapp.cmal.data.db.McWorld
import kotlinx.coroutines.flow.Flow

/** 世界（存档）仓库 */
class WorldsRepository(private val database: AppDatabase) {

    fun observeAll(): Flow<List<McWorld>> = database.worldDao().observeAll()

    suspend fun findByName(name: String): List<McWorld> = database.worldDao().getByName(name)

    suspend fun add(world: McWorld): Long = database.worldDao().upsert(world)

    suspend fun delete(world: McWorld) {
        database.worldDao().delete(world)
    }
}
