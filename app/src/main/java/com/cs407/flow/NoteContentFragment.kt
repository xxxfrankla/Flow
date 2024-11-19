package com.cs407.flow

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

class NoteContentFragment(
    private val injectedUserViewModel: UserViewModel? = null
) : Fragment() {

    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var spinner: Spinner
    private var noteId: Int = 0
    private lateinit var noteDB: NoteDatabase
    private lateinit var userViewModel: UserViewModel
    private var userId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        noteId = arguments?.getInt("noteId") ?: 0
        noteDB = NoteDatabase.getDatabase(requireContext())
        userViewModel = if (injectedUserViewModel != null) {
            injectedUserViewModel
        } else {
            ViewModelProvider(requireActivity())[UserViewModel::class.java]
        }
        userId = userViewModel.userState.value.id


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_note_content, container, false)
        titleEditText = view.findViewById(R.id.titleEditText)
        contentEditText = view.findViewById(R.id.contentEditText)
        saveButton = view.findViewById(R.id.saveButton)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()
        setupBackNavigation()

        // Check if this is an existing note (if noteId is not 0)
        if (noteId != 0) {
            lifecycleScope.launch {
                // Fetch the note from the database in the background
                val note = noteDB.noteDao().getById(noteId)
                var content: String? = note?.noteDetail

                // Switch to IO dispatcher for file reading
                withContext(Dispatchers.IO) {
                    if (note?.notePath != null) {
                        // Read content from file if it exists
                        val file = File(context?.filesDir, note.notePath)
                        content = file.readText()
                    }
                }

                // Switch back to Main dispatcher to update the UI
                withContext(Dispatchers.Main) {
                    if (note != null) {
                        titleEditText.setText(note.noteTitle)
                        contentEditText.setText(content)

                        // Force the UI to update (Espresso synchronization)
                        contentEditText.requestFocus()
                    } else {
                        Toast.makeText(context, "Note not found.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        saveButton.setOnClickListener {
            saveContent()
        }
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

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val notePath: String? = if (content.length > 1024) {
                    saveNoteContentToFile(userId, content)
                } else {
                    null
                }

                val noteAbstract = splitAbstractDetail(content)
                val lastEdited = Calendar.getInstance().time

                val note = Note(
                    noteId = if (noteId == 0) 0 else noteId,  // Pass 0 for auto-generation if new note
                    noteTitle = title,
                    noteAbstract = noteAbstract,
                    noteDetail = if (notePath == null) content else null,  // If large, save only the path
                    notePath = notePath,
                    lastEdited = lastEdited
                )

                noteDB.noteDao().upsert(note)

                withContext(Dispatchers.Main) {
                    // Ensure NavController is available before navigating
                    if (isAdded && view != null) {
                        findNavController().popBackStack()  // Only navigate if the fragment is attached
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveNoteContentToFile(userId: Int, content: String): String {
        val timestamp = Calendar.getInstance().time.time
        val fileName = "note-${userId}-${noteId}-${timestamp}.txt"
        val file = File(requireContext().filesDir, fileName)
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
