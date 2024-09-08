package ru.netology.diploma.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.diploma.dto.Job

@Entity(tableName = "JobEntity")
class JobEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    val position: String,
    val start: String,
    val finish: String? = null,
    val link: String? = null,
    val ownedByMe: Boolean = false
) {

    fun toDto() = Job(id, name, position, start, finish, link, ownedByMe)


    companion object {
        fun fromDto(dto: Job): JobEntity {
            return JobEntity(
                dto.id,
                dto.name,
                dto.position,
                dto.start,
                dto.finish,
                dto.link,
                dto.ownedByMe,
            )
        }
    }
}

fun List<JobEntity>.toDto(): List<Job> = map(JobEntity::toDto)
fun List<Job>.toEntity(): List<JobEntity> = map(JobEntity::fromDto)