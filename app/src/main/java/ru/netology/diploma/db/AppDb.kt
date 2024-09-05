package ru.netology.diploma.db

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.netology.diploma.dao.PostDao
import ru.netology.diploma.dao.PostRemoteKeyDao
import ru.netology.diploma.entity.PostEntity
import ru.netology.diploma.entity.PostRemoteKeyEntity


@Database(
    entities = [PostEntity::class, PostRemoteKeyEntity::class],
    version = 1
)

abstract class AppDb : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun postRemoteKeyDao(): PostRemoteKeyDao
}