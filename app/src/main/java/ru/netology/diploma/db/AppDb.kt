package ru.netology.diploma.db

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.netology.diploma.dao.EventDao
import ru.netology.diploma.dao.EventRemoteKeyDao
import ru.netology.diploma.dao.JobDao
import ru.netology.diploma.dao.PostDao
import ru.netology.diploma.dao.PostRemoteKeyDao
import ru.netology.diploma.dao.UserDao
import ru.netology.diploma.dao.WallDao
import ru.netology.diploma.entity.EventEntity
import ru.netology.diploma.entity.EventRemoteKeyEntity
import ru.netology.diploma.entity.JobEntity
import ru.netology.diploma.entity.PostEntity
import ru.netology.diploma.entity.PostRemoteKeyEntity
import ru.netology.diploma.entity.UserEntity
import ru.netology.diploma.entity.WallEntity

@Database(
    entities = [PostEntity::class, EventEntity::class, UserEntity::class, WallEntity::class, JobEntity::class, PostRemoteKeyEntity::class, EventRemoteKeyEntity::class],
    version = 1
)

abstract class AppDb : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun eventDao(): EventDao
    abstract fun userDao(): UserDao
    abstract fun wallDao(): WallDao
    abstract fun jobDao(): JobDao

    abstract fun postRemoteKeyDao(): PostRemoteKeyDao
    abstract fun eventRemoteKeyDao(): EventRemoteKeyDao
}