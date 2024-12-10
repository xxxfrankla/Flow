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
            taskAbstract.text = "Task: ${taskSummary.taskAbstract}"
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            dueDate.text = taskSummary.dueDate?.let { "Due: ${dateFormatter.format(it)}" } ?: "Due: Not specified"
            priority.text = "Priority: ${taskSummary.priority.toString()}"

            val cardView = itemView as androidx.cardview.widget.CardView
            // Set item background color based on priority
            val backgroundColor = when (taskSummary.priority) {
                1 -> itemView.context.getColor(R.color.priority_1)
                2 -> itemView.context.getColor(R.color.priority_2)
                3 -> itemView.context.getColor(R.color.priority_3)
                4 -> itemView.context.getColor(R.color.priority_4)
                5 -> itemView.context.getColor(R.color.priority_5)
                6 -> itemView.context.getColor(R.color.priority_6)
                7 -> itemView.context.getColor(R.color.priority_7)
                8 -> itemView.context.getColor(R.color.priority_8)
                9 -> itemView.context.getColor(R.color.priority_9)
                10 -> itemView.context.getColor(R.color.priority_10)
                else -> itemView.context.getColor(R.color.white) // Fallback for undefined priorities
            }
            cardView.setCardBackgroundColor(backgroundColor)


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
