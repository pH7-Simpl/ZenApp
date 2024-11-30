package com.example.zenapp

data class ToDoItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val isDone: Boolean = false
)