package com.cs407.lab5_milestone

import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import com.cs407.lab5_milestone.data.Task
import com.cs407.lab5_milestone.data.TaskDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TaskContentFragment(
    private val injectedUserViewModel: UserViewModel? = null
) : Fragment() {

    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var priorityEditText: EditText
    private lateinit var estimatedTimeEditText: EditText
    private lateinit var dueDateEditText: EditText

    private var taskId: Int = 0
    private lateinit var taskDB: TaskDatabase
    private lateinit var userViewModel: UserViewModel
    private var userId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskId = arguments?.getInt("taskId") ?: 0
        taskDB = TaskDatabase.getDatabase(requireContext())
        userViewModel = if (injectedUserViewModel != null) {
            injectedUserViewModel
        } else {
            // TODO - Use ViewModelProvider to init UserViewModel
            ViewModelProvider(requireActivity())[UserViewModel::class.java]
        }
        userId = userViewModel.userState.value.id
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_task_content, container, false)
        titleEditText = view.findViewById(R.id.titleEditText)
        contentEditText = view.findViewById(R.id.contentEditText)
        saveButton = view.findViewById(R.id.saveButton)
        priorityEditText = view.findViewById(R.id.priorityEditText)
        estimatedTimeEditText = view.findViewById(R.id.estimatedTimeEditText)
        dueDateEditText = view.findViewById(R.id.dueDateEditText)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()
        setupBackNavigation()

        if (taskId != 0) {
            // TODO: Launch a coroutine to fetch the note from the database in the background
            lifecycleScope.launch {
                val task = taskDB.taskDao().getById(taskId)
                var content: String? = task?.taskDetail
                withContext(Dispatchers.IO) {
                    if (task.taskPath != null) {
                        val file = File(context?.filesDir, task.taskPath)
                        content = file.readText()
                    }
                }

                withContext(Dispatchers.Main) {
                    if (task != null) {
                        titleEditText.setText(task.taskTitle)
                        contentEditText.setText(content)
                        priorityEditText.setText(task.priority.toString())
                        estimatedTimeEditText.setText(task.estimatedTime?.toString())
                        dueDateEditText.setText(task.dueDate?.let { date ->
                            java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                .format(date)
                        })
                    }
                }
            }
        }

        saveButton.setOnClickListener {
            saveContent()
        }
    }
    private fun readTaskContentFromFile(filePath: String): String {
        val file = File(filePath)
        return file.readText()
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    android.R.id.home -> {
                        findNavController().popBackStack()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        if (activity is AppCompatActivity) {
            (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupBackNavigation() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (activity is AppCompatActivity) {
            (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        }
    }

    private fun saveContent() {
        val title = titleEditText.text.toString()
        val content = contentEditText.text.toString()
        val priority = priorityEditText.text.toString().toIntOrNull() ?: 0
        val estimatedTime = estimatedTimeEditText.text.toString().toIntOrNull() ?: 0
        val dueDate = parseDueDate()

        if (!validateInputs()) return

        lifecycleScope.launch(Dispatchers.IO) {
            val taskPath = if (content.length > 1024) saveTaskContentToFile(userId, content) else null
            val taskAbstract = splitAbstractDetail(content)

            val task = Task(
                taskId = if (taskId == 0) 0 else taskId,
                taskTitle = title,
                taskAbstract = taskAbstract,
                taskDetail = if (taskPath == null) content else null,
                taskPath = taskPath,
                lastEdited = Calendar.getInstance().time,
                priority = priority,
                estimatedTime = estimatedTime,
                dueDate = dueDate
            )
            taskDB.taskDao().upsertTask(task, userId)

            withContext(Dispatchers.Main) {
                if (isAdded && view != null) {
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun parseDueDate(): Date? {
        return try {
            if (dueDateEditText.text.isNotBlank()) {
                java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .parse(dueDateEditText.text.toString())
            } else null
        } catch (e: Exception) {
            dueDateEditText.error = "Invalid date format (YYYY-MM-DD)"
            null
        }
    }



    private fun validateInputs(): Boolean {
        if (titleEditText.text.isBlank()) {
            titleEditText.error = "Title is required"
            return false
        }
        if (priorityEditText.text.toString().toIntOrNull() == null) {
            priorityEditText.error = "Priority must be a valid number"
            return false
        }
        if (dueDateEditText.text.isNotBlank()) {
            try {
                java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dueDateEditText.text.toString())
            } catch (e: Exception) {
                dueDateEditText.error = "Invalid date format (YYYY-MM-DD)"
                return false
            }
        }
        return true
    }



    private fun saveTaskContentToFile(userId: Int, content: String): String {
        val timestamp = Calendar.getInstance().time.time
        val fileName = "task-$userId-$taskId-$timestamp.txt"
        val fileDir = requireContext().filesDir
        val file = File(fileDir, fileName)
        file.writeText(content)
        return fileName
    }

    private fun splitAbstractDetail(content: String?): String {
        val stringList = content?.split('\n', limit = 2) ?: listOf("")
        var stringAbstract = stringList[0]
        if (stringAbstract.length > 20) {
            stringAbstract = stringAbstract.substring(0, 20) + "..."
        }
        return stringAbstract
    }
}
