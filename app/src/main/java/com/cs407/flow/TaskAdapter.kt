package com.cs407.flow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.cs407.flow.data.TaskSummary
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private val onClick: (Int) -> Unit,
    private val onLongClick: (TaskSummary) -> Unit
) : PagingDataAdapter<TaskSummary, TaskAdapter.TaskViewHolder>(TASK_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(itemView, onClick, onLongClick)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val taskSummary = getItem(position)
        if (taskSummary != null) {
            holder.bind(taskSummary)
        }
    }

    class TaskViewHolder(
        itemView: View,
        private val onClick: (Int) -> Unit,
        private val onLongClick: (TaskSummary) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val taskTitle: TextView = itemView.findViewById(R.id.titleTextView)
        private val taskAbstract: TextView = itemView.findViewById(R.id.abstractTextView)
        private val dueDate: TextView = itemView.findViewById(R.id.dueDateTextView)
        private val priority: TextView = itemView.findViewById(R.id.priorityTextView)

        fun bind(taskSummary: TaskSummary) {
            taskTitle.text = taskSummary.taskTitle
            taskAbstract.text = taskSummary.taskAbstract
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            dueDate.text = taskSummary.dueDate?.let { dateFormatter.format(it) }
            priority.text = taskSummary.priority.toString()

            itemView.setOnClickListener {
                onClick(taskSummary.taskId)
            }
            itemView.setOnLongClickListener {
                onLongClick(taskSummary)
                true
            }
        }
    }

    companion object {
        private val TASK_COMPARATOR = object : DiffUtil.ItemCallback<TaskSummary>() {
            override fun areItemsTheSame(oldItem: TaskSummary, newItem: TaskSummary): Boolean =
                oldItem.taskId == newItem.taskId

            override fun areContentsTheSame(oldItem: TaskSummary, newItem: TaskSummary): Boolean =
                oldItem == newItem
        }
    }
}
