package ru.netology.diploma.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import ru.netology.diploma.BuildConfig
import ru.netology.diploma.api.EventApiService
import ru.netology.diploma.api.PostsApiService
import ru.netology.diploma.dao.EventDao
import ru.netology.diploma.dao.EventRemoteKeyDao
import ru.netology.diploma.dao.JobDao
import ru.netology.diploma.dao.PostDao
import ru.netology.diploma.dao.PostRemoteKeyDao
import ru.netology.diploma.dao.UserDao
import ru.netology.diploma.dao.WallDao
import ru.netology.diploma.db.AppDb
import ru.netology.diploma.dto.Attachment
import ru.netology.diploma.dto.Event
import ru.netology.diploma.dto.Job
import ru.netology.diploma.dto.Media
import ru.netology.diploma.dto.Post
import ru.netology.diploma.dto.UserResponse
import ru.netology.diploma.entity.EventEntity
import ru.netology.diploma.entity.JobEntity
import ru.netology.diploma.entity.PostEntity
import ru.netology.diploma.entity.UserEntity
import ru.netology.diploma.entity.WallEntity
import ru.netology.diploma.entity.toDto
import ru.netology.diploma.entity.toEntity
import ru.netology.diploma.entity.toWallEntity
import ru.netology.diploma.error.ApiError
import ru.netology.diploma.error.NetworkError
import ru.netology.diploma.error.UnknownError
import ru.netology.diploma.model.AttachmentModel
import java.io.IOException
import javax.inject.Inject

class RepositoryImpl @Inject constructor (
    private val dao: PostDao,
    private val eventDao: EventDao,
    private val userDao: UserDao,
    private val wallDao: WallDao,
    private val jobDao: JobDao,
    private val postApiService: PostsApiService,
    private val eventApiService: EventApiService,
    postRemoteKeyDao: PostRemoteKeyDao,
    eventRemoteKeyDao: EventRemoteKeyDao,
    appDb: AppDb
) : Repository {

    @OptIn(ExperimentalPagingApi::class)
    override val data: Flow<PagingData<Post>> = Pager(
        config = PagingConfig(pageSize = 15, enablePlaceholders = true),
        pagingSourceFactory = {dao.getPagingSource()},
        remoteMediator = PostRemoteMediator(postApiService, dao, postRemoteKeyDao = postRemoteKeyDao, appDb = appDb)
    ).flow.map { pagingData ->
        pagingData.map(PostEntity::toDto)
    }

    @OptIn(ExperimentalPagingApi::class)
    override val eventData: Flow<PagingData<Event>> = Pager(
        config = PagingConfig(pageSize = 15, enablePlaceholders = true),
        pagingSourceFactory = {eventDao.getPagingSource()},
        remoteMediator = EventRemoteMediator(eventApiService, eventDao, eventRemoteKeyDao = eventRemoteKeyDao, appDb = appDb)
    ).flow.map { pagingData ->
        pagingData.map(EventEntity::toDto)
    }

    override val userList = userDao.getAll()
        .map(List<UserEntity>::toDto)
        .flowOn(Dispatchers.Default)

    override val wall = wallDao.getAll()
        .map(List<WallEntity>::toDto)
        .flowOn(Dispatchers.Default)

    override val jobs = jobDao.getJobs()
        .map(List<JobEntity>::toDto)
        .flowOn(Dispatchers.Default)


    override suspend fun updatePlayer() {
        withContext(Dispatchers.IO) {
            dao.updatePlayer(false)
            eventDao.updatePlayerEvent(false)
            wallDao.updatePlayerWall(false)
        }
    }

    override suspend fun updateIsPlaying(postId: Int, isPlaying: Boolean) {
        withContext(Dispatchers.IO) {
            dao.updateIsPlaying(postId, isPlaying)
        }
    }

    override suspend fun updateIsPlayingEvent (postId: Int, isPlaying: Boolean) {
        withContext(Dispatchers.IO) {
            eventDao.updateIsPlayingEvent(postId, isPlaying)
        }
    }

    override suspend fun updateIsPlayingWall (postId: Int, isPlaying: Boolean) {
        withContext(Dispatchers.IO) {
            wallDao.updateIsPlayingWall(postId, isPlaying)
        }
    }



    override suspend fun save(post: Post) {

        try {
            val response = postApiService.save(post, BuildConfig.API_KEY)
            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            val result = response.body() ?: throw ApiError(response.message())

            dao.insert(PostEntity.fromDto(result))

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun saveWithAttachment(post: Post, attachmentModel: AttachmentModel) {

        try {
            val mediaResponse =saveMedia(attachmentModel)

            if (!mediaResponse.isSuccessful) {
                throw ApiError(mediaResponse.message())
            }

            val media = mediaResponse.body() ?: throw ApiError(mediaResponse.message())

            val response = postApiService.save(post.copy(attachment = Attachment(media.url, attachmentModel.type)), BuildConfig.API_KEY)

            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            val result = response.body() ?: throw ApiError(response.message())

            dao.insert(PostEntity.fromDto(result))

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    private suspend fun saveMedia (attachmentModel: AttachmentModel): Response<Media> {
        val part = MultipartBody.Part.createFormData("file", attachmentModel.file.name, attachmentModel.file.asRequestBody())
        return postApiService.saveMedia (part, BuildConfig.API_KEY)
    }

    override suspend fun likeById(id: Int, flag: Boolean) {

        try {
            val response = if (!flag) {
                postApiService.likeById(id, BuildConfig.API_KEY)
            } else {
                postApiService.dislikeById(id, BuildConfig.API_KEY)
            }

            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }
            val body = response.body() ?: throw ApiError(response.message())

            dao.likeById(body.id)
            dao.insert(PostEntity.fromDto(body))

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun removeById(id: Int) {

        try {
            val response = postApiService.removeById(id, BuildConfig.API_KEY)
            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            response.body() ?: throw ApiError(response.message())
            dao.removeById(id)
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }



    override suspend fun getUserById(id: Int): UserResponse {
        try {
            val response = postApiService.getUserById (id, BuildConfig.API_KEY)
            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            return response.body() ?: throw ApiError(response.message())

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun getAllUsers() {
        try {
            val response = postApiService.getAllUsers(BuildConfig.API_KEY)
            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            val body = response.body() ?: throw ApiError(response.message())

            userDao.insert(body.toEntity())

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun updateUsers (user: UserResponse, isSelected: Boolean) {
        withContext(Dispatchers.IO) {
            userDao.updateUsers(user.id, isSelected)
        }
    }

    override suspend fun deselectUsers (isSelected: Boolean) {
        withContext(Dispatchers.IO) {
            userDao.deselectUsers(isSelected)
        }
    }


    override suspend fun saveEvent (event: Event) {

        try {
            val response = eventApiService.save(event, BuildConfig.API_KEY)
            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            val result = response.body() ?: throw ApiError(response.message())

            eventDao.insert(EventEntity.fromDto(result))

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun saveEventWithAttachment (event: Event, attachmentModel: AttachmentModel) {

        try {
            val mediaResponse =saveMedia(attachmentModel)

            if (!mediaResponse.isSuccessful) {
                throw ApiError(mediaResponse.message())
            }

            val media = mediaResponse.body() ?: throw ApiError(mediaResponse.message())

            val response = eventApiService.save(event.copy(attachment = Attachment(media.url, attachmentModel.type)), BuildConfig.API_KEY)

            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            val result = response.body() ?: throw ApiError(response.message())

            eventDao.insert(EventEntity.fromDto(result))

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun likeEventById(id: Int, flag: Boolean) {
        try {
            val response = if (!flag) {
                eventApiService.likeById(id, BuildConfig.API_KEY)
            } else {
                eventApiService.dislikeById(id, BuildConfig.API_KEY)
            }

            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }
            val body = response.body() ?: throw ApiError(response.message())

            eventDao.likeById(body.id)
            eventDao.insert(EventEntity.fromDto(body))

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun removeEventById(id: Int) {
        try {
            val response = eventApiService.removeById(id, BuildConfig.API_KEY)
            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            response.body() ?: throw ApiError(response.message())
            eventDao.removeById(id)
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun getWall(authorId: Int) {
        try {
            val response = postApiService.getWall(authorId, BuildConfig.API_KEY)
            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            val body = response.body() ?: throw ApiError(response.message())

            wallDao.updateWall(body.toWallEntity())

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun getJobs (userId: Int) {
        try {
            val response = postApiService.getJobs(userId, BuildConfig.API_KEY)
            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            val body = response.body() ?: throw ApiError(response.message())

            jobDao.updateJobs(body.toEntity())

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun getMyJobs () {
        try {
            val response = postApiService.getMyJobs(BuildConfig.API_KEY)
            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            val body = response.body() ?: throw ApiError(response.message())
            val newBody = body.map {
                it.copy(ownedByMe = true)
            }

            jobDao.updateJobs(newBody.toEntity())

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun createJob(job: Job) {
        try {
            val response = postApiService.createJob(job, BuildConfig.API_KEY)
            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            val result = response.body() ?: throw ApiError(response.message())

            jobDao.insert(JobEntity.fromDto(result))

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun removeJobById(id: Int) {
        try {
            val response = postApiService.removeJobById(id, BuildConfig.API_KEY)
            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            response.body() ?: throw ApiError(response.message())
            jobDao.removeJobById(id)
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

}