package ru.netology.diploma.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.diploma.auth.AppAuth
import ru.netology.diploma.dto.AttachmentType
import ru.netology.diploma.model.AttachmentModel
import ru.netology.diploma.util.SingleLiveEvent
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(appAuth: AppAuth) : ViewModel() {
    private val dataAuth = appAuth

    private val _response = SingleLiveEvent<Unit>()
    val response: LiveData<Unit> = _response

    private val _error = SingleLiveEvent<Unit>()
    val error: LiveData<Unit> = _error

    fun registerAndSetAuth(login: String, password: String, name: String) = viewModelScope.launch {
        try {
            val user = dataAuth.registerUser(login, password, name)
            user.token?.let { dataAuth.setAuth(user.id, it) }
            _response.value = Unit
        } catch (e: Exception) {
            _error.value = Unit
        }
    }

    private val _photo = MutableLiveData<AttachmentModel?>(null)
    val photo: LiveData<AttachmentModel?>
        get() = _photo

    fun setPhoto(uri: Uri, file: File, type: AttachmentType) {
        _photo.value = AttachmentModel(uri, file, type)
    }

    fun clearPhoto() {
        _photo.value = null
    }

    fun registerWithAvatarAndSetAuth(login: String, password: String, name: String) =
        viewModelScope.launch {
            try {
                val photoModel = _photo.value
                if (photoModel == null) {
                    val user = dataAuth.registerUser(login, password, name)
                    user.token?.let { dataAuth.setAuth(user.id, it) }
                    _response.value = Unit
                } else {
                    val user =
                        dataAuth.registerUserWithAvatar(login, password, name, photoModel.file)
                    user.token?.let { dataAuth.setAuth(user.id, it) }
                    _response.value = Unit
                }
            } catch (e: Exception) {
                _error.value = Unit
            }
        }
}