package com.cszyapp.cmal.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * CMAL 本地数据库
 * 存储版本、服务器、皮肤、世界、资源、下载任务
 */
@Database(
    entities = [
        McVersion::class,
        McServer::class,
        McSkin::class,
        McWorld::class,
        McResource::class,
        DownloadTask::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun mcVersionDao(): McVersionDao
    abstract fun serverDao(): ServerDao
    abstract fun skinDao(): SkinDao
    abstract fun worldDao(): WorldDao
    abstract fun resourceDao(): ResourceDao
    abstract fun downloadTaskDao(): DownloadTaskDao

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
