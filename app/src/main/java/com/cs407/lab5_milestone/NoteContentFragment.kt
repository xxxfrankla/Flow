package com.cs407.lab5_milestone

import android.content.Context
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
import com.cs407.lab5_milestone.data.Note
import com.cs407.lab5_milestone.data.NoteDatabase
import com.cs407.lab5_milestone.data.UserNoteRelation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import java.util.Date

class NoteContentFragment(
    private val injectedUserViewModel: UserViewModel? = null
) : Fragment() {

    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var saveButton: Button

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
            // TODO - Use ViewModelProvider to init UserViewModel
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

        if (noteId != 0) {
            // TODO: Launch a coroutine to fetch the note from the database in the background
            lifecycleScope.launch{
                val note = noteDB.noteDao().getById(noteId)
                var content: String? = note?.noteDetail
                withContext(Dispatchers.IO){
                    if(note.notePath!= null){
                        content = readNoteContentFromFile(note.notePath)
                    }
                }

                withContext(Dispatchers.Main){
                    if(note != null) {
                        titleEditText.setText(note.noteTitle)
                        contentEditText.setText(content)
                    }
                }
            }
            // TODO: Retrieve the note from the Room database using the noteId

            // TODO: Check if the note content is stored in the database or in a file

            // TODO: If the content is too large and stored as a file, read the file content

            // TODO: Switch back to the main thread to update the UI with the note content

            // TODO: Set the retrieved note title to the title EditText field

            // TODO: Set the note content (either from the file or the database) to the content EditText field

            // TODO: Optionally handle exceptions (e.g., file not found, database errors) if necessary
        }

        saveButton.setOnClickListener {
            saveContent()
        }
    }
    private fun readNoteContentFromFile(filePath: String): String {
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
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    private fun saveContent() {
        // Retrieve the title and content from EditText fields
        val title = titleEditText.text.toString()
        val content = contentEditText.text.toString()

        lifecycleScope.launch(Dispatchers.IO) {
            val notePath: String? = if(content.length > 1024){
                saveNoteContentToFile(userId, content)
            }else{
                null
            }
            val noteAbstract = splitAbstractDetail(content)
            val note = Note(
                noteId = if(noteId== 0) 0 else noteId,
                noteTitle = title,
                noteAbstract = noteAbstract,
                noteDetail = if (notePath == null) content else null,
                notePath = notePath,
                lastEdited = Calendar.getInstance().time
            )
            noteDB.noteDao().upsertNote(note, userId)

            withContext(Dispatchers.Main){
                if(isAdded && view != null){
                    findNavController().popBackStack()
                }
            }
        }
    }


    // TODO: Launch a coroutine to save the note in the background (non-UI thread)
        // TODO: Check if the note content is too large for direct storage in the database
        // TODO: Save the content as a file if it's too large for the database
        // TODO: Store the note content directly in the database if it's small enough
        // TODO: Insert or update the note in the Room database using the DAO method
        // TODO: Ensure that noteId is assigned (could be auto-generated in Room)
        // TODO: Implement logic to create an abstract from the content
        // TODO: Ensure that userId is passed correctly (it should be associated with the note)
        // TODO: Switch back to the main thread to navigate the UI after saving
        // TODO: Navigate back to the previous screen (e.g., after saving the note)

    private fun saveNoteContentToFile(userId: Int, content: String): String {
        val timestamp = System.currentTimeMillis()
        val fileName = "note-$userId-$noteId-$timestamp.txt"
        val fileDir = requireContext().filesDir
        val file = File(fileDir, fileName)
        file.writeText(content)
        return file.absolutePath
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
