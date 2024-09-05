package ru.netology.diploma.dto

data class UserResponse(
    val id: Int,
    val login: String,
    val name: String,
    val avatar: String?,
    val isSelected: Boolean = false
)
