package com.cs407.lab5_milestone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.lab5_milestone.data.Task
import com.cs407.lab5_milestone.data.TaskDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.times

data class UserState(
    val id: Int = 0, val name: String = "", val passwd: String = ""
)

class UserViewModel : ViewModel() {
    private val _userState = MutableStateFlow(UserState())
    val userState = _userState.asStateFlow()

    private val _sortedTasks = MutableStateFlow<List<Task>>(emptyList())
    val sortedTasks = _sortedTasks.asStateFlow()

    fun setUser(state: UserState) {
        _userState.update {
            state
        }
    }

    // Function to fetch and sort tasks based on the user's ID
    fun fetchAndSortTasks(taskDao: TaskDao) {
        viewModelScope.launch {
            userState.collect { user ->
                if (user.id != 0) { // Ensure we have a valid user ID
                    val tasks = taskDao.getTasksOrderedByDueDate() // Fetch tasks
                    val sorted = sortTasksByPriorityAndDueDate(tasks) // Apply sorting
                    _sortedTasks.emit(sorted) // Emit sorted tasks
                }
            }
        }
    }

    private fun sortTasksByPriorityAndDueDate(tasks: List<Task>): List<Task> {
        val currentDate = System.currentTimeMillis()

        return tasks.sortedByDescending { task ->
            val daysUntilDue = if (task.dueDate != null) {
                ((task.dueDate.time - currentDate) / (1000 * 60 * 60 * 24)).toInt()
            } else {
                Int.MAX_VALUE // If no due date, place it at the end
            }

            val weightDueDate = -1         // Weight for due date (more urgent = higher priority)
            val weightPriority = 210         // Weight for task priority (higher priority = higher score)
            val weightEstimatedTime = 0.5 // Weight for estimated time (shorter time = higher score)

            // Include estimatedTime in the score
            (weightDueDate * daysUntilDue) +
                    (weightPriority * task.priority) +
                    (weightEstimatedTime * task.estimatedTime)
        }
    }

}
