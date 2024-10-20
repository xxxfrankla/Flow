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
import androidx.paging.cachedIn
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cs407.lab5_milestone.data.Note
import com.cs407.lab5_milestone.data.NoteDatabase
import com.cs407.lab5_milestone.data.NoteSummary
import com.cs407.lab5_milestone.data.User
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Calendar

class NoteListFragment(
    private val injectedUserViewModel: UserViewModel? = null
) : Fragment() {

    private lateinit var greetingTextView: TextView
    private lateinit var noteRecyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton

    private lateinit var userViewModel: UserViewModel

    private lateinit var noteDB: NoteDatabase
    private lateinit var userPasswdKV: SharedPreferences

    private var deleteIt: Boolean = false
    private lateinit var noteToDelete: NoteSummary

    private lateinit var adapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        noteDB = NoteDatabase.getDatabase(requireContext())
        userPasswdKV = requireContext().getSharedPreferences(
            getString(R.string.userPasswdKV), Context.MODE_PRIVATE)
        userViewModel = if (injectedUserViewModel != null) {
            injectedUserViewModel
        } else {
            // TODO - Use ViewModelProvider to init UserViewModel
            ViewModelProvider(requireActivity())[UserViewModel::class.java]
        }

        // Manually create 1000 notes for "large" user
        val userState = userViewModel.userState.value
        lifecycleScope.launch {
            val countNote = noteDB.noteDao().userNoteCount(userState.id)
            if (countNote == 0 && userState.name == "large") {
                for (i in 1..1000) {
                    noteDB.noteDao().upsertNote(
                        Note(
                            noteTitle = "Note $i",
                            noteAbstract = "This is Note $i",
                            noteDetail = "Welcome to Note $i",
                            notePath = null,
                            lastEdited = Calendar.getInstance().time
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
        val view = inflater.inflate(R.layout.fragment_note_list, container, false)
        greetingTextView = view.findViewById(R.id.greetingTextView)
        noteRecyclerView = view.findViewById(R.id.noteRecyclerView)
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
                        findNavController().navigate(R.id.action_noteListFragment_to_loginFragment)
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

        adapter = NoteAdapter(
            onClick = { noteId ->
                val action = NoteListFragmentDirections.actionNoteListFragmentToNoteContentFragment(noteId)
                findNavController().navigate(action)
            },
            onLongClick = { noteSummary ->
                deleteIt = true
                noteToDelete = noteSummary
                showDeleteBottomSheet()
            }
        )

        noteRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        noteRecyclerView.adapter = adapter

        loadNotes()

        fab.setOnClickListener {
            val action = NoteListFragmentDirections.actionNoteListFragmentToNoteContentFragment(0)
            findNavController().navigate(action)
        }
    }

    private fun loadNotes() {
        // TODO: Retrieve the current user state from the ViewModel (to get the user ID)
        val userState = userViewModel.userState.value

        // TODO: Set up paging configuration with a specified page size and prefetch distance
        val pagingConfig = PagingConfig(pageSize = 20, prefetchDistance = 10)

        // TODO: Implement a query to retrieve the paged list of notes associated with the user
        val pager = Pager(pagingConfig) {
            noteDB.userDao().getUsersWithNoteListsByIdPaged(userState.id)
        }
        // TODO: Launch a coroutine to collect the paginated flow and submit it to the RecyclerView adapter
        lifecycleScope.launch{
            pager.flow.cachedIn(lifecycleScope).collect{
                pagingData -> adapter.submitData(pagingData)
            }
        }
        // TODO: Cache the paging flow in the lifecycle scope and collect the paginated data
        // TODO: Submit the paginated data to the adapter to display it in the RecyclerView

    }


    private fun showDeleteBottomSheet() {
        if (deleteIt) {
            val bottomSheetDialog = BottomSheetDialog(requireContext())
            val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_delete, null)
            bottomSheetDialog.setContentView(bottomSheetView)

            val deleteButton = bottomSheetView.findViewById<Button>(R.id.deleteButton)
            val cancelButton = bottomSheetView.findViewById<Button>(R.id.cancelButton)
            val deletePrompt = bottomSheetView.findViewById<TextView>(R.id.deletePrompt)

            deletePrompt.text = "Delete Note: ${noteToDelete.noteTitle}"

            deleteButton.setOnClickListener {
                // TODO: Launch a coroutine to perform the note deletion in the background
                lifecycleScope.launch(Dispatchers.IO){
                    noteDB.deleteDao().deleteNotes(listOf(noteToDelete.noteId))
                }
                // TODO: Implement the logic to delete the note from the Room database using the DAO

                // TODO: Reset any flags or variables that control the delete state
                cancelButton.setOnClickListener {
                    deleteIt = false
                    bottomSheetDialog.dismiss()
                }
                bottomSheetDialog.setOnDismissListener {
                    deleteIt = false
                }
                // TODO: Dismiss the bottom sheet dialog after the deletion is completed
                // TODO: Reload the list of notes to reflect the deleted note (e.g., refresh UI)
                loadNotes() // Implement the function to refresh or reload the notes
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
            val deleteDao = noteDB.deleteDao()
            deleteDao.delete(userState.id)

            val editor = userPasswdKV.edit()
            editor.remove(userState.name)
            editor.remove("currentUserName")
            editor.apply()

            userViewModel.setUser(UserState())
            withContext(Dispatchers.Main) {
                findNavController().navigate(R.id.action_noteListFragment_to_loginFragment)
            }
        }
        // TODO: Implement the logic to delete the user's data from the Room database
        // TODO: Remove the user's credentials from SharedPreferences
        // TODO: Reset the user state in the ViewModel to represent a logged-out state
        // TODO: Navigate back to the login screen after the account is deleted and user is logged out
    }
}