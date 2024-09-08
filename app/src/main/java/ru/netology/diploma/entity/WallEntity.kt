package ru.netology.diploma.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.netology.diploma.dto.Attachment
import ru.netology.diploma.dto.AttachmentType
import ru.netology.diploma.dto.Coordinates
import ru.netology.diploma.dto.Post
import ru.netology.diploma.dto.UserPreview

@Entity(tableName = "WallEntity")
data class WallEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val authorId: Int,
    val author: String,
    val authorJob: String?,
    val authorAvatar: String?,
    val content: String,
    val published: String,
    val link: String?,
    val mentionedMe: Boolean,
    val likedByMe: Boolean = false,
    val likes: Int = 0,
    val ownedByMe: Boolean = false,
    val coords: String?,
    @Embedded
    val attachment: AttachmentEntityWall?,
    val mentionIds: String,
    val likeOwnerIds: String,
    val users: String,
) {

    fun toDto() = Post(
        id, authorId, author, authorJob, authorAvatar, content, published, coordsToDto(coords),
        link, mentionedMe, likedByMe, likes, ownedByMe,
        attachmentToDto(), toListInt(mentionIds), toListInt(likeOwnerIds), usersToDto(users)
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
        fun fromDto(dto: Post): WallEntity {
            return WallEntity(
                dto.id,
                dto.authorId,
                dto.author,
                dto.authorJob,
                dto.authorAvatar,
                dto.content,
                dto.published,
                dto.link,
                dto.mentionedMe,
                dto.likedByMe,
                dto.likes,
                dto.ownedByMe,
                coordsFromDto(dto.coords),
                attachmentFromDto(dto.attachment),
                fromListInt(dto.mentionIds),
                fromListInt(dto.likeOwnerIds),
                usersFromDto(dto.users)
            )
        }

        private fun attachmentFromDto(dto: Attachment?) = dto?.let {
            AttachmentEntityWall(it.url, it.type, it.isPlaying)
        }

        private fun coordsFromDto(coords: Coordinates?): String {
            return Gson().toJson(coords)
        }

        private fun fromListInt(list: List<Int>): String {
            return list.joinToString(",")
        }


        private fun usersFromDto(users: Map<Int, UserPreview>): String {
            return Gson().toJson(users)
        }

    }
}

fun List<WallEntity>.toDto(): List<Post> = map(WallEntity::toDto)
fun List<Post>.toWallEntity(): List<WallEntity> = map(WallEntity::fromDto)

data class AttachmentEntityWall(
    val url: String?,
    val type: AttachmentType?,
    val isPlaying: Boolean
)