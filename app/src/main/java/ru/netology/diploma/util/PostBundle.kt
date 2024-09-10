package ru.netology.diploma.util

import android.os.Bundle
import ru.netology.diploma.dto.Attachment
import ru.netology.diploma.dto.AttachmentType
import ru.netology.diploma.dto.Coordinates
import ru.netology.diploma.dto.Post
import ru.netology.diploma.dto.UserPreview
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
@Suppress("unused")
object PostBundle : ReadWriteProperty<Bundle, Post?> {
    override fun getValue(thisRef: Bundle, property: KProperty<*>): Post? {
        return thisRef.getBundle(property.name)?.let { bundleToPost(it) }
    }

    override fun setValue(thisRef: Bundle, property: KProperty<*>, value: Post?) {
        thisRef.putBundle(property.name, postToBundle(value))
    }
}

fun postToBundle(post: Post?): Bundle {
    post ?: return Bundle()
    val bundle = Bundle()
    bundle.putInt("id", post.id)
    bundle.putInt("authorId", post.authorId)
    bundle.putString("author", post.author)
    post.authorJob?.let { bundle.putString("authorJob", it) }
    post.authorAvatar?.let { bundle.putString("authorAvatar", it) }
    bundle.putString("content", post.content)
    bundle.putString("published", post.published)
    post.coords?.let { bundle.putBundle("coords", coordsToBundle(it)) }
    post.link?.let { bundle.putString("link", it) }
    bundle.putBoolean("mentionedMe", post.mentionedMe)
    bundle.putBoolean("likedByMe", post.likedByMe)
    bundle.putInt("likes", post.likes)
    bundle.putBoolean("ownedByMe", post.ownedByMe)
    post.attachment?.let { bundle.putBundle("attachment", attachmentToBundle(it)) }
    bundle.putIntegerArrayList("mentionIds", ArrayList(post.mentionIds))
    bundle.putIntegerArrayList("likeOwnerIds", ArrayList(post.likeOwnerIds))
    val usersBundle = Bundle()
    post.users.forEach { (key, userPreview) ->
        usersBundle.putBundle(key.toString(), userPreviewToBundle(userPreview))
    }
    bundle.putBundle("users", usersBundle)

    return bundle
}

fun bundleToPost(bundle: Bundle): Post {
    val usersBundle = bundle.getBundle("users")
    val post = Post(
        bundle.getInt("id"),
        bundle.getInt("authorId"),
        bundle.getString("author") ?: "",
        bundle.getString("authorJob"),
        bundle.getString("authorAvatar"),
        bundle.getString("content") ?: "",
        bundle.getString("published") ?: "",
        bundle.getBundle("coords")?.let { coordsFromBundle(it) },
        bundle.getString("link"),
        bundle.getBoolean("mentionedMe"),
        bundle.getBoolean("likedByMe"),
        bundle.getInt("likes"),
        bundle.getBoolean("ownedByMe"),
        bundle.getBundle("attachment")?.let { attachmentFromBundle(it) },
        bundle.getIntegerArrayList("mentionIds")?.toList() ?: emptyList(),
        bundle.getIntegerArrayList("likeOwnerIds")?.toList() ?: emptyList(),
        if (usersBundle != null) {
            usersFromBundle(bundle.getBundle("users")!!)
        } else emptyMap()
    )

    return post
}

fun coordsToBundle(coords: Coordinates?): Bundle {
    val bundle = Bundle()
    if (coords == null) {
        bundle.putDouble("latitude", 0.00)
        bundle.putDouble("longitude", 0.00)
    }
    coords?.lat?.let { bundle.putDouble("latitude", it) }
    coords?.long?.let { bundle.putDouble("longitude", it) }

    return bundle
}

fun coordsFromBundle(bundle: Bundle): Coordinates {
    val latitude = bundle.getDouble("latitude")
    val longitude = bundle.getDouble("longitude")

    return Coordinates(latitude, longitude)
}

fun attachmentToBundle(attachment: Attachment): Bundle {
    val bundle = Bundle()
    bundle.putString("url", attachment.url)
    bundle.putBoolean("isPlaying", attachment.isPlaying)
    bundle.putParcelable("type", attachment.type?.let { attachmentTypeToBundle(it) })
    return bundle
}

fun attachmentTypeToBundle(type: AttachmentType?): Bundle {
    val bundle = Bundle()
    bundle.putString("type", type?.name)
    return bundle
}

fun attachmentTypeFromBundle(bundle: Bundle): AttachmentType {
    val typeName = bundle.getString("type", null)
    return typeName?.let { enumValueOf<AttachmentType>(it) } ?: AttachmentType.Unknown
}

fun attachmentFromBundle(bundle: Bundle): Attachment {
    val url = bundle.getString("url")
    val isplaying = bundle.getBoolean("isPlaying")
    val type = bundle.getBundle("type")?.let { attachmentTypeFromBundle(it) }
    return Attachment(url, type, isplaying)
}

fun userPreviewToBundle(userPreview: UserPreview): Bundle {
    val bundle = Bundle()
    bundle.putString("name", userPreview.name)
    bundle.putString("avatar", userPreview.avatar)
    return bundle
}

fun userPreviewFromBundle(bundle: Bundle): UserPreview? {
    val name = bundle.getString("name")
    val avatar = bundle.getString("avatar")
    return name?.let { UserPreview(it, avatar) }
}

fun usersFromBundle(bundle: Bundle): Map<Int, UserPreview> {
    val users = mutableMapOf<Int, UserPreview>()
    bundle.keySet().forEach { key ->
        val userId = key.toInt()
        val userPreviewBundle =
            bundle.getBundle(key)
        if (userPreviewBundle != null) {
            val userPreview = userPreviewFromBundle(userPreviewBundle)
            if (userPreview != null) {
                users[userId] = userPreview
            }
        }
    }
    return users
}