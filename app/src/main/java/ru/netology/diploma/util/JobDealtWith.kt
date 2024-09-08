package ru.netology.diploma.util

import ru.netology.diploma.dto.Job

object JobDealtWith {
    private lateinit var jobDealtWith: Job

    fun get() = jobDealtWith

    fun saveJobDealtWith (job: Job) {
        jobDealtWith = job
    }
}