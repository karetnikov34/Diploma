package ru.netology.diploma.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.diploma.dto.UserResponse

@Entity(tableName = "UserEntity")
class UserEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val login: String,
    val name: String,
    val avatar: String?,
    val isSelected: Boolean
) {

    fun toDto() = UserResponse(id, login,name, avatar, isSelected)


    companion object {
        fun fromDto(dto: UserResponse): UserEntity {
            return UserEntity(
                dto.id,
                dto.login,
                dto.name,
                dto.avatar,
                dto.isSelected
            )
        }
    }
}

fun List<UserEntity>.toDto(): List<UserResponse> = map(UserEntity::toDto)
fun List<UserResponse>.toEntity(): List<UserEntity> = map(UserEntity::fromDto)