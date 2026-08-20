package com.cszyapp.cmal.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * CMAL 本地数据库
 * 存储服务器、皮肤、世界、资源
 */
@Database(
    entities = [
        McServer::class,
        McSkin::class,
        McWorld::class,
        McResource::class,
        McInstance::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun serverDao(): ServerDao
    abstract fun skinDao(): SkinDao
    abstract fun worldDao(): WorldDao
    abstract fun resourceDao(): ResourceDao
    abstract fun instanceDao(): InstanceDao

    /** 关闭数据库连接（恢复备份前调用，配合进程重启让 Room 重新加载） */
    fun close() {
        INSTANCE?.close()
        INSTANCE = null
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cmal.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
