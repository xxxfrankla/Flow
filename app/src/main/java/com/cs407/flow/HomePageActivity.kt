package com.cs407.flow

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class HomePageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepage)

        // Set up RecyclerView
        val recyclerView: RecyclerView = findViewById(R.id.todo_list_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = TaskAdapter(getDummyTasks())

        // Set up Dashboard Button to navigate to DashboardActivity
        val dashboardButton: Button = findViewById(R.id.dashboard_button)
        dashboardButton.setOnClickListener {
            val intent = Intent(this@HomePageActivity, NoteContentFragment::class.java)
            startActivity(intent)
        }
    }

    // Create dummy tasks for testing purposes
    private fun getDummyTasks(): List<Task> {
        val taskList = mutableListOf<Task>()
        for (i in 0..17) {
            taskList.add(Task("$i", "Item $i", "Description for item $i", System.currentTimeMillis() + 86400000))
        }
        return taskList
    }
}



