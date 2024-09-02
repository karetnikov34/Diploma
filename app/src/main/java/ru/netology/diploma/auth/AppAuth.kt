package ru.netology.diploma.auth

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.diploma.BuildConfig
import ru.netology.diploma.api.AuthApiService
import ru.netology.diploma.error.ApiError
import ru.netology.diploma.error.NetworkError
import ru.netology.diploma.error.UnknownError
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppAuth @Inject constructor  (
    @ApplicationContext
    private val context: Context
) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val idKey = "id"
    private val tokenKey = "token"

    private val _authStateFlow = MutableStateFlow(
        AuthState (
            prefs.getInt(idKey, 0),
            prefs.getString(tokenKey, null)
        )
    )

    val authStateFlow: StateFlow<AuthState> = _authStateFlow.asStateFlow()

    @Synchronized
    fun setAuth(id: Int, token: String) {
        _authStateFlow.value = AuthState(id, token)
        with(prefs.edit()) {
            putInt(idKey, id)
            putString(tokenKey, token)
            apply()
        }
    }

    @Synchronized
    fun removeAuth() {
        _authStateFlow.value = AuthState()
        with(prefs.edit()) {
            clear()
            commit()
        }

    }

    @InstallIn(SingletonComponent::class)
    @EntryPoint
    interface AppAuthEntryPoint {
        fun getAuthApiService(): AuthApiService
    }

    suspend fun checkAuth(login: String, password: String): AuthState =
        try {
            val entryPoint = EntryPointAccessors.fromApplication(context, AppAuthEntryPoint::class.java)
            val response =  entryPoint.getAuthApiService().checkUser(login, password, BuildConfig.API_KEY)
            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            val responseBody = response.body()
            responseBody?: throw ApiError(response.message())

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }

    suspend fun registerUser (login: String, password: String, name: String): AuthState =
        try {
            val entryPoint = EntryPointAccessors.fromApplication(context, AppAuthEntryPoint::class.java)
            val response =  entryPoint.getAuthApiService().registerUser(login, password, name, BuildConfig.API_KEY)
            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            val responseBody = response.body()
            responseBody?: throw ApiError(response.message())

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }

    suspend fun registerUserWithAvatar (login: String, password: String, name: String, avatar: File): AuthState =
        try {
            val part = MultipartBody.Part.createFormData("file", "image.png", avatar.asRequestBody())
            val loginRequest = login.toRequestBody("text/plain".toMediaType())
            val passwordRequest = password.toRequestBody("text/plain".toMediaType())
            val nameRequest = name.toRequestBody("text/plain".toMediaType())
            val entryPoint = EntryPointAccessors.fromApplication(context, AppAuthEntryPoint::class.java)
            val response =  entryPoint.getAuthApiService().registerWithPhoto (loginRequest, passwordRequest, nameRequest, part, BuildConfig.API_KEY)
            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            val responseBody = response.body()
            responseBody?: throw ApiError(response.message())

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }


}

data class AuthState(val id: Int = 0, val token: String? = null)