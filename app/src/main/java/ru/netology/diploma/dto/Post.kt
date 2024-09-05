package ru.netology.diploma.dto

data class Post(
    val id: Int,
    val authorId: Int,
    val author: String,
    val authorJob: String? = null,
    val authorAvatar: String? = null,
    val content: String,
    val published: String,
    val coords: Coordinates?,
    val link: String? = null,
    val mentionedMe: Boolean,
    val likedByMe: Boolean = false,
    val likes: Int = 0,
    val ownedByMe: Boolean = false,
    val attachment: Attachment?,
    val mentionIds: List<Int> = emptyList(),
    val likeOwnerIds: List<Int> = emptyList(),
    val users: Map<Int, UserPreview> = emptyMap()
)
