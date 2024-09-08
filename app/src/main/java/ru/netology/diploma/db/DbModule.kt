package ru.netology.diploma.db

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.netology.diploma.dao.EventDao
import ru.netology.diploma.dao.EventRemoteKeyDao
import ru.netology.diploma.dao.JobDao
import ru.netology.diploma.dao.PostDao
import ru.netology.diploma.dao.PostRemoteKeyDao
import ru.netology.diploma.dao.UserDao
import ru.netology.diploma.dao.WallDao
import javax.inject.Singleton

@InstallIn(SingletonComponent :: class)
@Module
class DbModule {

    @Singleton
    @Provides
    fun provideDb (
        @ApplicationContext
        context: Context
    ): AppDb = Room.databaseBuilder(context, AppDb::class.java, "app.db")
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun providePostDao (
        appDb: AppDb
    ): PostDao = appDb.postDao()

    @Provides
    fun provideEventDao (
        appDb: AppDb
    ): EventDao = appDb.eventDao()

    @Provides
    fun provideUserDao (
        appDb: AppDb
    ): UserDao = appDb.userDao()

    @Provides
    fun provideWallDao (
        appDb: AppDb
    ): WallDao = appDb.wallDao()

    @Provides
    fun provideJobDao (
        appDb: AppDb
    ): JobDao = appDb.jobDao()



    @Provides
    fun providePostRemoteKeyDao (
        appDb: AppDb
    ): PostRemoteKeyDao = appDb.postRemoteKeyDao()

    @Provides
    fun provideEventRemoteKeyDao (
        appDb: AppDb
    ): EventRemoteKeyDao = appDb.eventRemoteKeyDao()


}