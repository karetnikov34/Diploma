package ru.netology.diploma.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import ru.netology.diploma.BuildConfig
import ru.netology.diploma.api.EventApiService
import ru.netology.diploma.dao.EventDao
import ru.netology.diploma.dao.EventRemoteKeyDao
import ru.netology.diploma.db.AppDb
import ru.netology.diploma.entity.EventEntity
import ru.netology.diploma.entity.EventRemoteKeyEntity
import ru.netology.diploma.error.ApiError

@OptIn(ExperimentalPagingApi::class)
class EventRemoteMediator(
    private val service: EventApiService,
    private val eventDao: EventDao,
    private val eventRemoteKeyDao: EventRemoteKeyDao,
    private val appDb: AppDb
) : RemoteMediator<Int, EventEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, EventEntity>
    ): MediatorResult {

        try {

            val response = when (loadType) {

                LoadType.REFRESH -> service.getLatest(state.config.pageSize, BuildConfig.API_KEY)

                LoadType.PREPEND -> {
                    return MediatorResult.Success(endOfPaginationReached = true)
                }

                LoadType.APPEND -> {
                    val id = eventRemoteKeyDao.min() ?: return MediatorResult.Success(
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

                        eventRemoteKeyDao.removeAll()
                        eventRemoteKeyDao.insert(
                            listOf(
                                EventRemoteKeyEntity(
                                    type = EventRemoteKeyEntity.KeyType.AFTER,
                                    id = bodyResponse.first().id,
                                ),
                                EventRemoteKeyEntity(
                                    type = EventRemoteKeyEntity.KeyType.BEFORE,
                                    id = bodyResponse.last().id,
                                )
                            )
                        )
                        eventDao.removeAll()
                    }


                    LoadType.PREPEND -> Unit

                    LoadType.APPEND -> {
                        eventRemoteKeyDao.insert(
                            EventRemoteKeyEntity(
                                type = EventRemoteKeyEntity.KeyType.BEFORE,
                                id = bodyResponse.last().id,
                            )
                        )
                    }

                }

                eventDao.insert(bodyResponse.map { EventEntity.fromDto(it) })

            }


            return MediatorResult.Success(endOfPaginationReached = bodyResponse.isEmpty())

        } catch (e: Exception) {
            return MediatorResult.Error(e)
        }
    }
}