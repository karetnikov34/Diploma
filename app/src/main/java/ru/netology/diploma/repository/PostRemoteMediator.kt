package ru.netology.diploma.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import ru.netology.diploma.BuildConfig
import ru.netology.diploma.api.PostsApiService
import ru.netology.diploma.dao.PostDao
import ru.netology.diploma.dao.PostRemoteKeyDao
import ru.netology.diploma.db.AppDb
import ru.netology.diploma.entity.PostEntity
import ru.netology.diploma.entity.PostRemoteKeyEntity
import ru.netology.diploma.error.ApiError

    @OptIn(ExperimentalPagingApi::class)
    class PostRemoteMediator(
        private val service: PostsApiService,
        private val postDao: PostDao,
        private val postRemoteKeyDao: PostRemoteKeyDao,
        private val appDb: AppDb
    ) : RemoteMediator<Int, PostEntity>() {

        override suspend fun load(
            loadType: LoadType,
            state: PagingState<Int, PostEntity>
        ): MediatorResult {

            try {

                val response = when (loadType) {

                    LoadType.REFRESH -> service.getLatest(state.config.pageSize, BuildConfig.API_KEY)

                    LoadType.PREPEND -> {
                        return MediatorResult.Success(endOfPaginationReached = true)
                    }

                    LoadType.APPEND -> {
                        val id = postRemoteKeyDao.min() ?: return MediatorResult.Success(
                            endOfPaginationReached = false
                        )
                        service.getBefore(id, state.config.pageSize, BuildConfig.API_KEY)
                    }

                }


                if (!response.isSuccessful) {
                    throw ApiError(response.message())
                }
                val bodyResponse = response.body() ?: throw ApiError(
                    response.message(),
                )

                if (bodyResponse.isEmpty()) return MediatorResult.Success(endOfPaginationReached = true)

                appDb.withTransaction {
                    when (loadType) {
                        LoadType.REFRESH -> {

                            postRemoteKeyDao.removeAll()
                            postRemoteKeyDao.insert(
                                listOf(
                                    PostRemoteKeyEntity(
                                        type = PostRemoteKeyEntity.KeyType.AFTER,
                                        id = bodyResponse.first().id,
                                    ),
                                    PostRemoteKeyEntity(
                                        type = PostRemoteKeyEntity.KeyType.BEFORE,
                                        id = bodyResponse.last().id,
                                    )
                                )
                            )
                            postDao.removeAll()
                        }


                        LoadType.PREPEND -> Unit

                        LoadType.APPEND -> {
                            postRemoteKeyDao.insert(
                                PostRemoteKeyEntity(
                                    type = PostRemoteKeyEntity.KeyType.BEFORE,
                                    id = bodyResponse.last().id,
                                )
                            )
                        }

                    }

                    postDao.insert(bodyResponse.map { PostEntity.fromDto(it) })

                }


                return MediatorResult.Success(endOfPaginationReached = bodyResponse.isEmpty())

            } catch (e: Exception) {
                return MediatorResult.Error(e)
            }
        }
    }