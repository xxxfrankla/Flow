package com.cs407.lab5_milestone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

// Fake class, you need to define your own @Entity for Note
class Note(noteTitle: String, noteAbstract: String, noteDetail: String, notePath: String?, lastEdited: Date);

// Fake function, you need to use your own @Dao function
fun upsertNote(note: Note, userId: Int) {}

class NoteListFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Manually create 1000 notes for "large" user (only valid for bonus part)
        lifecycleScope.launch {
            val countNote = 0; // The total number of notes in the table
            val userName = "large";
            val userId = 0;
            if (countNote == 0 && userName == "large") {
                for (i in 1..1000) {
                    upsertNote(
                        Note(
                            noteTitle = "Note $i",
                            noteAbstract = "This is Note $i",
                            noteDetail = "Welcome to Note $i",
                            notePath = null,
                            lastEdited = Calendar.getInstance().time
                        ), userId
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
        return view
    }
}