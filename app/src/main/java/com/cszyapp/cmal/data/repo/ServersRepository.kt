package com.cszyapp.cmal.data.repo

import com.cszyapp.cmal.data.db.AppDatabase
import com.cszyapp.cmal.data.db.McServer
import kotlinx.coroutines.flow.Flow

/** 服务器仓库 */
class ServersRepository(private val database: AppDatabase) {

    fun observeAll(): Flow<List<McServer>> = database.serverDao().observeAll()

    suspend fun add(server: McServer): Long = database.serverDao().upsert(server)

    suspend fun update(server: McServer) {
        database.serverDao().upsert(server)
    }

    suspend fun delete(server: McServer) {
        database.serverDao().delete(server)
    }
}
