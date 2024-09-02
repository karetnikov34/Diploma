package ru.netology.diploma.dto

data class Attachment(
    val url: String?,
    val type: AttachmentType?,
    val isPlaying: Boolean = false
)

enum class AttachmentType {
    IMAGE,
    VIDEO,
    AUDIO,
    Unknown
}