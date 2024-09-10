package ru.netology.diploma.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import ru.netology.diploma.auth.AuthState

interface AuthApiService {

    @FormUrlEncoded
    @POST("users/authentication")
    suspend fun checkUser(
        @Field("login") login: String,
        @Field("pass") pass: String,
        @Header("Api-Key") apiKey: String
    ): Response<AuthState>

    @FormUrlEncoded
    @POST("users/registration")
    suspend fun registerUser(
        @Field("login") login: String,
        @Field("pass") pass: String,
        @Field("name") name: String,
        @Header("Api-Key") apiKey: String
    ): Response<AuthState>

    @Multipart
    @POST("users/registration")
    suspend fun registerWithPhoto(
        @Part("login") login: RequestBody,
        @Part("pass") pass: RequestBody,
        @Part("name") name: RequestBody,
        @Part media: MultipartBody.Part,
        @Header("Api-Key") apiKey: String
    ): Response<AuthState>
}