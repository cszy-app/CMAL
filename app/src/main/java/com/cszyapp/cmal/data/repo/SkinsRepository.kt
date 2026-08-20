package com.cszyapp.cmal.data.repo

import com.cszyapp.cmal.data.db.AppDatabase
import com.cszyapp.cmal.data.db.McSkin
import kotlinx.coroutines.flow.Flow

/** 皮肤仓库 */
class SkinsRepository(private val database: AppDatabase) {

    fun observeAll(): Flow<List<McSkin>> = database.skinDao().observeAll()

    suspend fun findByName(name: String): List<McSkin> = database.skinDao().getByName(name)

    suspend fun add(skin: McSkin): Long = database.skinDao().upsert(skin)

    suspend fun delete(skin: McSkin) {
        database.skinDao().delete(skin)
    }
}
