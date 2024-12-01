package com.cs407.lab5_milestone

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
import com.cs407.lab5_milestone.data.NoteSummary
import com.cs407.lab5_milestone.data.TaskSummary
import com.cs407.lab5_milestone.data.ToDoDatabase
import com.cs407.lab5_milestone.data.TodoItem
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TodoListFragment(
    private val injectedUserViewModel: UserViewModel? = null
) : Fragment() {

    private lateinit var greetingTextView: TextView
    private lateinit var todoRecyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var userViewModel: UserViewModel

    private lateinit var adapter: TodoAdapter
    private lateinit var todoDB: ToDoDatabase
    private lateinit var userPasswdKV: SharedPreferences

    private var deleteIt: Boolean = false
    private lateinit var taskToDelete: TaskSummary

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        todoDB = ToDoDatabase.getDatabase(requireContext())
        userPasswdKV = requireContext().getSharedPreferences(
            getString(R.string.userPasswdKV), Context.MODE_PRIVATE
        )
        userViewModel = if (injectedUserViewModel != null) {
            injectedUserViewModel
        } else {
            // TODO - Use ViewModelProvider to init UserViewModel
            ViewModelProvider(requireActivity())[UserViewModel::class.java]
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_todo_list, container, false)
        greetingTextView = view.findViewById(R.id.greetingTextView)
        todoRecyclerView = view.findViewById(R.id.noteRecyclerView)
        fab = view.findViewById(R.id.fab)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val menuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.note_list_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_logout -> {
                        userViewModel.setUser(UserState())
                        findNavController().navigate(R.id.action_todoListFragment_to_loginFragment)
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

        adapter = TodoAdapter(
            onClick = { taskId ->
                val action =
                    TodoListFragmentDirections.actionTodoListFragmentToNoteContentFragment(taskId)
                findNavController().navigate(action)
            },
            onLongClick = { taskSummary ->
                deleteIt = true
                taskToDelete = taskSummary
                showDeleteBottomSheet()
            }
        )

        todoRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        todoRecyclerView.adapter = adapter

        loadTodos()

        fab.setOnClickListener {
            val action = TodoListFragmentDirections.actionTodoListFragmentToNoteContentFragment(0)
            findNavController().navigate(action)
        }
    }

    private fun loadTodos() {

        val userState = userViewModel.userState.value
        // Set up paging configuration
        val pagingConfig = PagingConfig(pageSize = 20, prefetchDistance = 10)

        // Create a Pager object
        val pager = Pager(
            config = pagingConfig,
            pagingSourceFactory = { todoDB.userDao().getUsersWithTodoListsByIdPaged(userState.id) }
        ).flow

        // Launch a coroutine to collect the paginated flow and submit it to the RecyclerView adapter
        lifecycleScope.launch {
            pager.collectLatest { pagingData ->
                adapter.submitData(pagingData)
            }
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
                // TODO: Launch a coroutine to perform the note deletion in the background
                lifecycleScope.launch{
                    todoDB.deleteDao().deleteTodos(listOf(taskToDelete.taskId))
                    deleteIt = false
                    bottomSheetDialog.dismiss()
                    loadTodos()
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
        // TODO: Retrieve the current user state from the ViewModel (contains user details)
        val userState = userViewModel.userState.value
        // TODO: Launch a coroutine to perform account deletion in the background
        lifecycleScope.launch(Dispatchers.IO){
            val deleteDao = todoDB.deleteDao()
            deleteDao.delete(userState.id)

            with(userPasswdKV.edit()){
                remove(userState.name)
                apply()
            }


            withContext(Dispatchers.Main) {
                userViewModel.setUser(UserState())
                findNavController().navigate(R.id.action_todoListFragment_to_loginFragment)
            }
        }
        // TODO: Implement the logic to delete the user's data from the Room database
        // TODO: Remove the user's credentials from SharedPreferences
        // TODO: Reset the user state in the ViewModel to represent a logged-out state
        // TODO: Navigate back to the login screen after the account is deleted and user is logged out
    }
}