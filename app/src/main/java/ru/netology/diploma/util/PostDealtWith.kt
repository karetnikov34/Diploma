package ru.netology.diploma.util

import ru.netology.diploma.dto.Post

object PostDealtWith {
    private lateinit var postDealtWith: Post

    fun get() = postDealtWith

    fun savePostDealtWith (post: Post) {
        postDealtWith = post
    }
}