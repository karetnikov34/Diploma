package ru.netology.diploma.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ru.netology.diploma.auth.AppAuth
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor (private val appAuth: AppAuth): ViewModel() {

    val dataAuth = appAuth.authStateFlow

    val authenticated: Boolean
        get() = appAuth.authStateFlow.value.id != 0

    val authenticatedId: Int
        get() = appAuth.authStateFlow.value.id
}