package ru.netology.diploma.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.netology.diploma.auth.AppAuth
import ru.netology.diploma.dto.AttachmentType
import ru.netology.diploma.dto.Coordinates
import ru.netology.diploma.dto.Post
import ru.netology.diploma.dto.UserResponse
import ru.netology.diploma.model.AttachmentModel
import ru.netology.diploma.model.FeedModelState
import ru.netology.diploma.repository.Repository
import ru.netology.diploma.util.SingleLiveEvent
import ru.netology.diploma.util.published
import java.io.File
import javax.inject.Inject

private val defaultPost = Post(
    id = 0,
    author = "",
    authorId = 0,
    authorJob = null,
    authorAvatar = null,
    content = "",
    published = published(),
    coords = null,
    link = null,
    mentionIds = emptyList(),
    mentionedMe = false,
    likeOwnerIds = emptyList(),
    likedByMe = false,
    attachment = null,
    users = emptyMap(),
)

@HiltViewModel
class PostViewModel @Inject constructor(
    private val repository: Repository,
    appAuth: AppAuth
) : ViewModel() {

    val data: Flow<PagingData<Post>> = appAuth.authStateFlow
        .flatMapLatest { (myId, _) ->
            repository.data.map {postPagingData ->
                postPagingData.map { post ->
                    if (post is Post) {
                        post.copy(ownedByMe = post.authorId == myId)
                    } else {
                        post
                    }
                }
            }
        }.flowOn(Dispatchers.Default)
        .cachedIn(viewModelScope)

    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit> = _postCreated

    private val _postCreatedError = SingleLiveEvent<Unit>()
    val postCreatedError: LiveData<Unit> = _postCreatedError

    private val edited = MutableLiveData(defaultPost)


    fun changeContentAndSave(content: String, mentioned: List<Int>) {
        edited.value?.let { itPost ->
            val text = content.trim()
            if (text != itPost.content) {

                viewModelScope.launch {
                    try {
                        val attachmentModel =
                            _attachment.value
                        if (attachmentModel == null) {
                            repository.save(itPost.copy(content = text, coords = _coords.value, mentionIds = mentioned))
                        } else {
                            repository.saveWithAttachment(
                                itPost.copy(content = text, coords = _coords.value, mentionIds = mentioned),
                                attachmentModel
                            )
                        }
                        _dataState.value = FeedModelState(loading = true)
                        _dataState.value = FeedModelState()
                        _postCreated.value = Unit
                    } catch (e: Exception) {
                        _dataState.value = FeedModelState(error = true)
                    }

                    _coords.value = null
                }
            }
        }
        edited.value = defaultPost

    }

    //edit

    private val _postToEdit = MutableLiveData<Post>()
    val postToEdit: LiveData<Post> = _postToEdit

    fun setPostToEdit(post: Post) {
        _postToEdit.value = post
    }

    fun clearAttachmentEdit() {
        _postToEdit.value = postToEdit.value?.copy(attachment = null)
    }

    fun clearLocationEdit() {
        _postToEdit.value = postToEdit.value?.copy(coords = null)
    }

    fun clearMentionedEdit() {
        val list: List<Int> = emptyList()
        _postToEdit.value = postToEdit.value?.copy(mentionIds = list)
    }


    fun edit(post: Post) {
        viewModelScope.launch {
            try {
                val attachmentModel =
                    _attachment.value
                if (attachmentModel == null) {
                    if (_coords.value == null) {
                        repository.save(post)
                    } else {
                        repository.save(post.copy(coords = _coords.value))
                    }
                } else {
                    if (_coords.value == null) {
                        repository.saveWithAttachment(
                            post,
                            attachmentModel
                        )
                    } else {
                        repository.saveWithAttachment(
                            post.copy(coords = _coords.value),
                            attachmentModel
                        )
                    }
                }
                _dataState.value = FeedModelState(loading = true)
                _dataState.value = FeedModelState()
                _postCreated.value = Unit
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true)
            }

            _coords.value = null
        }
    }


    fun likeById(post: Post) {

        viewModelScope.launch {
            try {
                repository.likeById(post.id, post.likedByMe)
                _dataState.value = FeedModelState()
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true)
            }
        }

    }


    fun removeById(id: Int) {
        viewModelScope.launch {
            try {
                repository.removeById(id)
                _dataState.value = FeedModelState()
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true)
            }
        }
    }

    private val _userList = MutableLiveData<UserResponse>()
    val userList: LiveData<UserResponse> = _userList


    fun getUserById(id: Int) = runBlocking {
        try {
            _userList.value = repository.getUserById(id)
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    private val _userChosen = MutableLiveData<List<Int>?>()
    val userChosen: LiveData<List<Int>?> = _userChosen

    fun setUserChosen (list: List<Int>) {
        _userChosen.value = list
    }

    fun clearUserChosen () {
        _userChosen.value = null
    }



    private val _attachment = MutableLiveData<AttachmentModel?>(null)
    val attachment: LiveData<AttachmentModel?>
        get() = _attachment

    fun setAttachment(
        uri: Uri,
        file: File,
        type: AttachmentType
    ) {
        _attachment.value = AttachmentModel(uri, file, type)
    }

    fun clearAttachment() {
        _attachment.value = null
    }


    fun updatePlayer() {
        viewModelScope.launch {
            repository.updatePlayer()
        }
    }

    fun updateIsPlaying (postId: Int, isPlaying: Boolean) = viewModelScope.launch {
        repository.updateIsPlaying(postId, isPlaying)
    }


    private val _coords = MutableLiveData<Coordinates?>()
    val coords: LiveData<Coordinates?> = _coords

    fun setCoords(lat: Double, long: Double) {
        _coords.value = Coordinates(lat, long)
    }

    fun clearCoords() {
        _coords.value = null
    }

    val placeName: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
}