package ru.netology.diploma.util

import ru.netology.diploma.dto.Event

object EventDealtWith {
    private lateinit var eventDealtWith: Event

    fun get() = eventDealtWith

    fun saveEventDealtWith (event: Event) {
        eventDealtWith = event
    }
}