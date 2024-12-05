package com.cs407.lab5_milestone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.cs407.lab5_milestone.data.TaskSummary
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private val onClick: (Int) -> Unit,
    private val onLongClick: (TaskSummary) -> Unit
) : PagingDataAdapter<TaskSummary, TaskAdapter.TaskViewHolder>(TASK_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
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
        private val taskDate: TextView = itemView.findViewById(R.id.dateTextView)

        fun bind(taskSummary: TaskSummary) {
            taskTitle.text = taskSummary.taskTitle
            taskAbstract.text = taskSummary.taskAbstract
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            taskDate.text = dateFormatter.format(taskSummary.lastEdited)

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
