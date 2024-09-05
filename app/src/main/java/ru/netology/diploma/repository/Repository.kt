package ru.netology.diploma.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.diploma.dto.Post
import ru.netology.diploma.dto.UserResponse
import ru.netology.diploma.model.AttachmentModel

interface Repository {
    val data: Flow<PagingData<Post>>

    suspend fun save(post: Post)
    suspend fun likeById(id: Int, flag: Boolean)
    suspend fun removeById(id: Int)
    suspend fun saveWithAttachment(post: Post, attachmentModel: AttachmentModel)
    suspend fun updatePlayer()
    suspend fun updateIsPlaying (postId: Int, isPlaying: Boolean)

    suspend fun getUserById (id: Int): UserResponse

}