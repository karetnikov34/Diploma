package ru.netology.diploma.util

import ru.netology.diploma.dto.UserResponse

object UserDealtWith {
    private lateinit var userDealtWith: UserResponse

    fun get() = userDealtWith

    fun saveUserDealtWith (user: UserResponse) {
        userDealtWith = user
    }
}