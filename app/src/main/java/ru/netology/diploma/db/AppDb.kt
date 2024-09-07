package ru.netology.diploma.db

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.netology.diploma.dao.EventDao
import ru.netology.diploma.dao.EventRemoteKeyDao
import ru.netology.diploma.dao.PostDao
import ru.netology.diploma.dao.PostRemoteKeyDao
import ru.netology.diploma.entity.EventEntity
import ru.netology.diploma.entity.EventRemoteKeyEntity
import ru.netology.diploma.entity.PostEntity
import ru.netology.diploma.entity.PostRemoteKeyEntity

@Database(
    entities = [PostEntity::class, EventEntity::class, PostRemoteKeyEntity::class, EventRemoteKeyEntity::class],
    version = 1
)

abstract class AppDb : RoomDatabase() {
    abstract fun postDao(): PostDao
        abstract fun eventDao(): EventDao
    abstract fun postRemoteKeyDao(): PostRemoteKeyDao
        abstract fun eventRemoteKeyDao(): EventRemoteKeyDao
}