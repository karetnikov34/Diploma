package ru.netology.diploma.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import ru.netology.diploma.dto.Job
import ru.netology.diploma.dto.Media
import ru.netology.diploma.dto.Post
import ru.netology.diploma.dto.UserResponse

interface PostsApiService {
    @GET("posts/latest")
    suspend fun getLatest (@Query("count") count: Int, @Header("Api-Key") apiKey: String) : Response<List<Post>>

    @GET("posts/{id}/before")
    suspend fun getBefore (@Path("id") id: Int, @Query("count") count: Int, @Header("Api-Key") apiKey: String): Response<List<Post>>

    @POST("posts/{id}/likes")
    suspend fun likeById(@Path("id") id: Int, @Header("Api-Key") apiKey: String): Response<Post>

    @DELETE("posts/{id}/likes")
    suspend fun dislikeById(@Path("id") id: Int, @Header("Api-Key") apiKey: String): Response<Post>

    @POST("posts")
    suspend fun save(@Body post: Post, @Header("Api-Key") apiKey: String): Response<Post>

    @DELETE("posts/{id}")
    suspend fun removeById(@Path("id") id: Int, @Header("Api-Key") apiKey: String): Response<Unit>

    @Multipart
    @POST("media")
    suspend fun saveMedia(@Part media: MultipartBody.Part, @Header("Api-Key") apiKey: String): Response<Media>

    @GET("users/{id}")
    suspend fun getUserById (@Path("id") id: Int, @Header("Api-Key") apiKey: String): Response<UserResponse>

    @GET("users")
    suspend fun getAllUsers (@Header("Api-Key") apiKey: String): Response<List<UserResponse>>

    @GET("{authorId}/wall")
    suspend fun getWall (@Path("authorId") id: Int, @Header("Api-Key") apiKey: String): Response<List<Post>>

    @GET("{userId}/jobs")
    suspend fun getJobs (@Path("userId") id: Int, @Header("Api-Key") apiKey: String): Response<List<Job>>

    @GET("my/jobs")
    suspend fun getMyJobs (@Header("Api-Key") apiKey: String): Response<List<Job>>

    @POST("my/jobs")
    suspend fun createJob(@Body job: Job, @Header("Api-Key") apiKey: String): Response<Job>

    @DELETE("my/jobs/{id}")
    suspend fun removeJobById(@Path("id") id: Int, @Header("Api-Key") apiKey: String): Response<Unit>
}