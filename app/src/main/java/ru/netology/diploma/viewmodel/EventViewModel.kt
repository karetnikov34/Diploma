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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.diploma.auth.AppAuth
import ru.netology.diploma.dto.AttachmentType
import ru.netology.diploma.dto.Coordinates
import ru.netology.diploma.dto.Event
import ru.netology.diploma.dto.EventType
import ru.netology.diploma.model.AttachmentModel
import ru.netology.diploma.model.FeedModelState
import ru.netology.diploma.repository.Repository
import ru.netology.diploma.util.SingleLiveEvent
import ru.netology.diploma.util.formatDateTimeEvent
import ru.netology.diploma.util.publishedEvent
import java.io.File
import javax.inject.Inject

private val defaultEvent = Event(
    id = 0,
    author = "",
    authorId = 0,
    authorJob = null,
    authorAvatar = null,
    content = "",
    datetime = publishedEvent(),
    published = publishedEvent(),
    coords = null,
    link = null,
    likeOwnerIds = emptyList(),
    likedByMe = false,
    attachment = null,
    users = emptyMap(),
    type = "ONLINE",
    participantsIds = emptyList(),
    speakerIds = emptyList(),
    participatedByMe = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EventViewModel @Inject constructor(
    private val repository: Repository,
    appAuth: AppAuth
) : ViewModel() {

    val eventData: Flow<PagingData<Event>> = appAuth.authStateFlow
        .flatMapLatest { (myId, _) ->
            repository.eventData.map { eventPagingData ->
                eventPagingData.map { event ->
                    event.copy(ownedByMe = event.authorId == myId)
                }
            }
        }.flowOn(Dispatchers.Default)
        .cachedIn(viewModelScope)


    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    private val _eventCreated = SingleLiveEvent<Unit>()
    val eventCreated: LiveData<Unit> = _eventCreated

    private val _eventCreatedError = SingleLiveEvent<Unit>()
    val eventCreatedError: LiveData<Unit> = _eventCreatedError

    private val edited = MutableLiveData(defaultEvent)

    fun changeContentAndSave(content: String, coordsEvent: Coordinates?, speakers: List<Int>) {
        edited.value?.let { event ->
            val text = content.trim()
            if (text != event.content) {

                viewModelScope.launch {
                    try {
                        val attachmentModel =
                            _attachment.value
                        if (attachmentModel == null) {
                            repository.saveEvent(
                                event.copy(
                                    content = text,
                                    coords = coordsEvent,
                                    type = _eventFormat.value,
                                    datetime = _eventDateTime.value,
                                    speakerIds = speakers
                                )
                            )
                        } else {
                            repository.saveEventWithAttachment(
                                event.copy(
                                    content = text,
                                    coords = coordsEvent,
                                    type = _eventFormat.value,
                                    datetime = _eventDateTime.value,
                                    speakerIds = speakers
                                ),
                                attachmentModel
                            )
                        }
                        _dataState.value = FeedModelState(loading = true)
                        _dataState.value = FeedModelState()
                        _eventCreated.value = Unit
                    } catch (e: Exception) {
                        _dataState.value = FeedModelState(error = true)
                    }
                }
            }
        }
        edited.value = defaultEvent

    }

    private val _eventFormat = MutableLiveData<String>()
    val eventFormat: LiveData<String> = _eventFormat

    fun setEventFormat(format: EventType) {
        _eventFormat.value = format.toString()
    }


    private val _eventDateTime = MutableLiveData<String?>()
    val eventDateTime: LiveData<String?> = _eventDateTime

    fun setEventDateTime(date: String) {
        _eventDateTime.value = formatDateTimeEvent(date)
    }

    fun clearEventDateTime() {
        _eventDateTime.value = null
    }


    private val _eventToEdit = MutableLiveData<Event>()
    val eventToEdit: LiveData<Event> = _eventToEdit

    fun setEventToEdit(event: Event) {
        _eventToEdit.value = event
    }

    fun clearAttachmentEditEvent() {
        _eventToEdit.value = eventToEdit.value?.copy(attachment = null)
    }

    fun clearLocationEditEvent() {
        _eventToEdit.value = eventToEdit.value?.copy(coords = null)
    }

    fun clearSpeakersEdit() {
        val list: List<Int> = emptyList()
        _eventToEdit.value = eventToEdit.value?.copy(speakerIds = list)
    }

    fun editEvent(event: Event, coordsEvent: Coordinates?) {
        viewModelScope.launch {
            try {
                val attachmentModel =
                    _attachment.value
                if (attachmentModel == null) {
                    if (coordsEvent == null) {
                        repository.saveEvent(event)
                    } else {
                        repository.saveEvent(event.copy(coords = coordsEvent))
                    }
                } else {
                    if (coordsEvent == null) {
                        repository.saveEventWithAttachment(
                            event,
                            attachmentModel
                        )
                    } else {
                        repository.saveEventWithAttachment(
                            event.copy(coords = coordsEvent),
                            attachmentModel
                        )
                    }
                }
                _dataState.value = FeedModelState(loading = true)
                _dataState.value = FeedModelState()
                _eventCreated.value = Unit
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true)
            }
        }
    }


    fun likeEventById(event: Event) {
        viewModelScope.launch {
            try {
                repository.likeEventById(event.id, event.likedByMe)
                _dataState.value = FeedModelState()
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true)
            }
        }
    }


    fun removeEventById(id: Int) {
        viewModelScope.launch {
            try {
                repository.removeEventById(id)
                _dataState.value = FeedModelState()
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true)
            }
        }
    }

    var speaker: Boolean = false

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

    fun updatePlayer() = viewModelScope.launch {
        repository.updatePlayer()
    }

    fun updateIsPlayingEvent(postId: Int, isPlaying: Boolean) = viewModelScope.launch {
        repository.updateIsPlayingEvent(postId, isPlaying)
    }
}