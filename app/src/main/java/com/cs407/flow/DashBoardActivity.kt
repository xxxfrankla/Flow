


package com.cs407.flow

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton

class DashBoardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dash_board) // Your activity layout

        // Find FloatingActionButton and set up a click listener
        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener {
            // Navigate to NoteContentFragment
            navigateToFragment(NoteContentFragment())
        }
    }

    // Function to navigate to a fragment
    private fun navigateToFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, fragment) // Replace with the ID of your container
        transaction.addToBackStack(null) // Add the transaction to back stack
        transaction.commit()
    }
}