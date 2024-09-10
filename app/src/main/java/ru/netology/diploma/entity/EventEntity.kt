package ru.netology.diploma.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.netology.diploma.dto.Attachment
import ru.netology.diploma.dto.Coordinates
import ru.netology.diploma.dto.Event
import ru.netology.diploma.dto.UserPreview

@Entity(tableName = "EventEntity")
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val authorId: Int,
    val author: String,
    val authorJob: String?,
    val authorAvatar: String?,
    val content: String,
    val dateTime: String?,
    val published: String,
    val coords: String?,
    val eventType: String?,
    val likeOwnerIds: String,
    val likedByMe: Boolean = false,
    val likes: Int = 0,
    val speakerIds: String,
    val participantsIds: String,
    val participatedByMe: Boolean = false,
    @Embedded
    val attachment: AttachmentEntity?,
    val link: String?,
    val users: String,
    val ownedByMe: Boolean = false,
) {

    fun toDto() = Event(
        id,
        authorId,
        author,
        authorJob,
        authorAvatar,
        content,
        dateTime,
        published,
        coordsToDto(coords),
        eventType,
        toListInt(likeOwnerIds),
        likedByMe,
        likes,
        toListInt(speakerIds),
        toListInt(participantsIds),
        participatedByMe,
        attachmentToDto(),
        link,
        usersToDto(users),
        ownedByMe
    )


    private fun attachmentToDto() =
        attachment?.url?.let { Attachment(it, attachment.type, attachment.isPlaying) }

    private fun coordsToDto(coords: String?): Coordinates? {
        return Gson().fromJson(coords, object : TypeToken<Coordinates?>() {}.type)
    }

    private fun toListInt(string: String): List<Int> {
        if (string == "") return emptyList()
        return string.split(",").map { it.toInt() }
    }

    private fun usersToDto(users: String): Map<Int, UserPreview> {
        return Gson().fromJson(users, object : TypeToken<Map<Int, UserPreview>>() {}.type)
    }

    companion object {
        fun fromDto(dto: Event): EventEntity {
            return EventEntity(
                dto.id,
                dto.authorId,
                dto.author,
                dto.authorJob,
                dto.authorAvatar,
                dto.content,
                dto.datetime,
                dto.published,
                coordsFromDto(dto.coords),
                dto.type,
                fromListInt(dto.likeOwnerIds),
                dto.likedByMe,
                dto.likes,
                fromListInt(dto.speakerIds),
                fromListInt(dto.participantsIds),
                dto.participatedByMe,
                attachmentFromDto(dto.attachment),
                dto.link,
                usersFromDto(dto.users),
                dto.ownedByMe,
            )
        }

        private fun attachmentFromDto(dto: Attachment?) = dto?.let {
            AttachmentEntity(it.url, it.type, it.isPlaying)
        }

        private fun coordsFromDto(coords: Coordinates?): String {
            return Gson().toJson(coords)
        }

        private fun fromListInt(list: List<Int>?): String {
            return list?.joinToString(",") ?: ""
        }


        private fun usersFromDto(users: Map<Int, UserPreview>): String {
            return Gson().toJson(users)
        }
    }
}