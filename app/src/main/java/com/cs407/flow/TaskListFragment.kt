package com.cs407.flow

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cs407.flow.data.Task
import com.cs407.flow.data.TaskDatabase
import com.cs407.flow.data.TaskSummary
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class TaskListFragment(
    private val injectedUserViewModel: UserViewModel? = null
) : Fragment() {

    private lateinit var greetingTextView: TextView
    private lateinit var taskRecyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton

    private lateinit var userViewModel: UserViewModel

    private lateinit var taskDB: TaskDatabase
    private lateinit var userPasswdKV: SharedPreferences

    private var deleteIt: Boolean = false
    private lateinit var taskToDelete: TaskSummary

    private lateinit var adapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskDB = TaskDatabase.getDatabase(requireContext())
        userPasswdKV = requireContext().getSharedPreferences(
            getString(R.string.userPasswdKV), Context.MODE_PRIVATE
        )
        userViewModel = if (injectedUserViewModel != null) {
            injectedUserViewModel
        } else {
            // TODO - Use ViewModelProvider to init UserViewModel
            ViewModelProvider(requireActivity())[UserViewModel::class.java]
        }

        // Manually create 1000 notes for "large" user
        val userState = userViewModel.userState.value
        lifecycleScope.launch {
            val countTask = taskDB.taskDao().userTaskCount(userState.id)
            if (countTask == 0 && userState.name == "large") {
                for (i in 1..1000) {
                    taskDB.taskDao().upsertTask(
                        Task(
                            taskTitle = "Task $i",
                            taskAbstract = "This is Task $i",
                            taskDetail = "Welcome to Task $i",
                            taskPath = null,
                            lastEdited = Calendar.getInstance().time,
                            priority = 0,
                            dueDate = Calendar.getInstance().time,
                            estimatedTime = 0
                        ), userState.id
                    )
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.fragment_task_list, container, false)
        greetingTextView = view.findViewById(R.id.greetingTextView)
        taskRecyclerView = view.findViewById(R.id.taskRecyclerView)
        fab = view.findViewById(R.id.fab)
        createNotificationChannel()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkAndRequestNotificationPermission()
        val menuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.task_list_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_logout -> {
                        userViewModel.setUser(UserState())
                        findNavController().navigate(R.id.action_taskListFragment_to_loginFragment)
                        true
                    }

                    R.id.action_delete_account -> {
                        deleteAccountAndLogout()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner)

        val userState = userViewModel.userState.value
        greetingTextView.text = getString(R.string.greeting_text, userState.name)

        adapter = TaskAdapter(
            onClick = { taskId ->
                val action =
                    TaskListFragmentDirections.actionTaskListFragmentToTaskContentFragment(taskId)
                findNavController().navigate(action)
            },
            onLongClick = { taskSummary ->
                deleteIt = true
                taskToDelete = taskSummary
                showDeleteBottomSheet()
            }
        )

        taskRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        taskRecyclerView.adapter = adapter

        loadTasks()

        fab.setOnClickListener {
            val action = TaskListFragmentDirections.actionTaskListFragmentToTaskContentFragment(0)
            findNavController().navigate(action)
        }
    }

    private fun loadTasks() {
        val userState = userViewModel.userState.value

        val pagingConfig = PagingConfig(pageSize = 20, prefetchDistance = 10)

        val pager = Pager(
            config = pagingConfig,
            pagingSourceFactory = {
                taskDB.userDao().getUsersWithTaskListsByIdPaged(userState.id)
            }
        ).flow
        lifecycleScope.launch {
            pager.collectLatest { pagingData ->
                adapter.submitData(pagingData)
            }
        }
        lifecycleScope.launch {
            //pager.collectLatest { pagingData -> adapter.submitData(pagingData) }
            val tasks = taskDB.userDao().getUsersWithTaskListsById(userState.id)
            showNotification(tasks)
        }
    }


    private fun showDeleteBottomSheet() {
        if (deleteIt) {
            val bottomSheetDialog = BottomSheetDialog(requireContext())
            val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_delete, null)
            bottomSheetDialog.setContentView(bottomSheetView)

            val deleteButton = bottomSheetView.findViewById<Button>(R.id.deleteButton)
            val cancelButton = bottomSheetView.findViewById<Button>(R.id.cancelButton)
            val deletePrompt = bottomSheetView.findViewById<TextView>(R.id.deletePrompt)

            deletePrompt.text = "Delete Task: ${taskToDelete.taskTitle}"

            deleteButton.setOnClickListener {
                lifecycleScope.launch{
                    taskDB.deleteDao().deleteTasks(listOf(taskToDelete.taskId))
                    deleteIt = false
                    bottomSheetDialog.dismiss()
                    loadTasks()
                }
            }

            cancelButton.setOnClickListener {
                deleteIt = false
                bottomSheetDialog.dismiss()
            }

            bottomSheetDialog.setOnDismissListener {
                deleteIt = false
            }

            bottomSheetDialog.show()
        }
    }

    private fun deleteAccountAndLogout() {
        val userState = userViewModel.userState.value
        lifecycleScope.launch(Dispatchers.IO){
            val deleteDao = taskDB.deleteDao()
            deleteDao.delete(userState.id)

            with(userPasswdKV.edit()){
                remove(userState.name)
                apply()
            }


            withContext(Dispatchers.Main) {
                userViewModel.setUser(UserState())
                findNavController().navigate(R.id.action_taskListFragment_to_loginFragment)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Task Notifications"
            val descriptionText = "Channel for task notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("taskChannel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request the permission
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }
    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                // Permission denied, notify the user
                Toast.makeText(
                    requireContext(),
                    "Notification permission denied. Notifications won't work.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun showNotification(taskSummaries: List<TaskSummary>) {
        // Check if POST_NOTIFICATIONS permission is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, don't show the notification
            return
        }
        if (taskSummaries.isEmpty()) return
        // Build the task list as a string
        val taskList = taskSummaries.joinToString(separator = "\n") { task ->
            "- ${task.taskTitle}: ${task.taskAbstract}"
        }
        // Build the notification
        val builder = NotificationCompat.Builder(requireContext(), "taskChannel")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your app's notification icon
            .setContentTitle("Your Tasks")
            .setContentText("You have ${taskSummaries.size} tasks!")
            .setStyle(NotificationCompat.BigTextStyle().bigText(taskList))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        // Show the notification
        with(NotificationManagerCompat.from(requireContext())) {
            notify(1, builder.build())
        }
    }

}