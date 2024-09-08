package ru.netology.diploma.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ru.netology.diploma.dto.Job
import ru.netology.diploma.dto.Post
import ru.netology.diploma.dto.UserResponse
import ru.netology.diploma.model.FeedModelState
import ru.netology.diploma.repository.Repository
import ru.netology.diploma.util.SingleLiveEvent
import ru.netology.diploma.util.publishedEvent
import javax.inject.Inject

private val defaultJob = Job(
    id = 0,
    name = "",
    position = "",
    start = publishedEvent(),
    finish = null,
    link = null,
    ownedByMe = false,
)

@HiltViewModel
class UserViewModel @Inject constructor(
    private val repository: Repository,
) : ViewModel() {

    val userList: Flow<List<UserResponse>> = repository.userList

    val wall: Flow<List<Post>> = repository.wall

    val jobs: Flow<List<Job>> = repository.jobs


    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    private val _jobCreated = SingleLiveEvent<Unit>()
    val jobCreated: LiveData<Unit>
        get() = _jobCreated

    private val _error = SingleLiveEvent<Unit>()
    val error: LiveData<Unit> = _error

    private val edited = MutableLiveData(defaultJob)


    init {
        loadUsers()
    }

    fun loadUsers() = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.getAllUsers()
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun loadWall(authorId: Int) = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.getWall(authorId)
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }

    }

    fun loadJobs(userId: Int) = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.getJobs(userId)
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }

    }

    fun loadMyJobs() = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.getMyJobs()
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }

    }

    fun createJob(
        name: String,
        position: String,
        start: String,
        finish: String?,
        link: String?
    ) {
        edited.value = edited.value?.copy(
            name = name.trim(),
            position = position.trim(),
            start = start,
            finish = finish,
            link = link
        )
        edited.value?.let { job ->
            viewModelScope.launch {
                try {
                    repository.createJob(job)
                    _jobCreated.value = Unit
                    _dataState.value = FeedModelState()
                } catch (e: Exception) {
                    _dataState.value = FeedModelState(error = true)
                }
            }
            edited.value = defaultJob
        }
    }

    fun editJob(
        id: Int,
        name: String,
        position: String,
        start: String,
        finish: String?,
        link: String?
    ) {
        edited.value = edited.value?.copy(
            id = id,
            name = name.trim(),
            position = position.trim(),
            start = start,
            finish = finish,
            link = link
        )
        edited.value?.let { job ->
            viewModelScope.launch {
                try {
                    repository.createJob(job)
                    _jobCreated.value = Unit
                    _dataState.value = FeedModelState()
                } catch (e: Exception) {
                    _dataState.value = FeedModelState(error = true)
                }
            }
            edited.value = defaultJob
        }
    }


    fun removeJobByd(id: Int) = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.removeJobById(id)
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }


    val uniqueSet = mutableSetOf<Int>()

    fun choosing(user: UserResponse) = viewModelScope.launch {
        if (!user.isSelected) {
            uniqueSet.add(user.id)
            repository.updateUsers(user, true)
        } else {
            uniqueSet.remove(user.id)
            repository.updateUsers(user, false)
        }
    }

    fun clearChoosing() = viewModelScope.launch {
        uniqueSet.clear()
        repository.deselectUsers(false)
    }

}