package com.cs407.lab5_milestone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.cs407.lab5_milestone.data.TaskSummary
import com.cs407.lab5_milestone.data.TodoItem
import java.text.SimpleDateFormat
import java.util.Locale

class TodoAdapter(
    private val onClick: (Int) -> Unit,
    private val onLongClick: (TaskSummary) -> Unit
) : PagingDataAdapter<TaskSummary, TodoAdapter.TodoViewHolder>(TODO_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_todo, parent, false)
        return TodoViewHolder(itemView, onClick, onLongClick)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val taskSummary = getItem(position)
        if (taskSummary != null) {
            holder.bind(taskSummary)
        }
    }

    class TodoViewHolder(
        itemView: View,
        private val onClick: (Int) -> Unit,
        private val onLongClick: (TaskSummary) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val taskTitleTextView: TextView = itemView.findViewById(R.id.taskTitleTextView)
        private val taskDescriptionTextView: TextView = itemView.findViewById(R.id.taskDescriptionTextView)
        private val dueDateTextView: TextView = itemView.findViewById(R.id.dueDateTextView)
        private val priorityTextView: TextView = itemView.findViewById(R.id.priorityTextView)

        fun bind(taskSummary: TaskSummary) {
            taskTitleTextView.text = taskSummary.taskTitle
            taskDescriptionTextView.text = taskSummary.taskDescription
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            dueDateTextView.text = "Due Date: ${dateFormatter.format(taskSummary.dueDate)}"
            priorityTextView.text = "Priority: ${taskSummary.priority}"

            itemView.setOnClickListener { onClick(taskSummary.taskId) }
            itemView.setOnLongClickListener {
                onLongClick(taskSummary)
                true
            }
        }
    }

    companion object {
        private val TODO_COMPARATOR = object : DiffUtil.ItemCallback<TaskSummary>() {
            override fun areItemsTheSame(oldItem: TaskSummary, newItem: TaskSummary): Boolean {
                return oldItem.taskId == newItem.taskId
            }

            override fun areContentsTheSame(oldItem: TaskSummary, newItem: TaskSummary): Boolean {
                return oldItem == newItem
            }
        }
    }
}