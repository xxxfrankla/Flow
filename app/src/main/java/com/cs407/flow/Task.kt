package com.cs407.flow

data class Task(
    val id: String,
    val title: String,
    val description: String,
    val dueDate: Long,
    val isCompleted: Boolean = false
)
