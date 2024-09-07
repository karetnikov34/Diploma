package ru.netology.diploma.dto

data class Event (
    val id: Int,
    val authorId: Int,
    val author: String,
    val authorJob: String? = null,
    val authorAvatar: String? = null,
    val content: String,
    val datetime: String?,
    val published: String,
    val coords: Coordinates?,
    val type: String?,
    val likeOwnerIds: List<Int> = emptyList(),
    val likedByMe: Boolean = false,
    val likes: Int = 0,
    val speakerIds: List<Int> = emptyList(),
    val participantsIds: List<Int> = emptyList(),
    val participatedByMe: Boolean = false,
    val attachment: Attachment?,
    val link: String? = null,
    val users: Map<Int, UserPreview> = emptyMap(),
    val ownedByMe: Boolean = false,
)

enum class EventType {
    OFFLINE,
    ONLINE,
}