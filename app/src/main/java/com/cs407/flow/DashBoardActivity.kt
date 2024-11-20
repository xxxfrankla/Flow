


package com.cs407.flow

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton

class DashBoardActivity : AppCompatActivity() {

    private lateinit var fab: FloatingActionButton
    private lateinit var dashBoardLayout: LinearLayout
    private lateinit var navHostFragmentContainer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dash_board)

        // Set up the NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Get references to the views
        dashBoardLayout = findViewById(R.id.dashBoard)
        navHostFragmentContainer = findViewById(R.id.nav_host_fragment)
        fab = findViewById(R.id.fab)

        // Initially, ensure the FAB is visible on the dashboard
        fab.visibility = View.VISIBLE

        // Set up the Floating Action Button to navigate to NoteContentFragment
        fab.setOnClickListener {
            // Hide the Dashboard layout
            dashBoardLayout.visibility = View.GONE

            // Hide the FAB when navigating
            fab.visibility = View.GONE

            // Show the FragmentContainerView to host the NoteContentFragment
            navHostFragmentContainer.visibility = View.VISIBLE

            // Navigate to NoteContentFragment
            navController.navigate(R.id.noteContentFragment)
        }

        // Listen for navigation changes to update UI accordingly
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.noteContentFragment -> {
                    // Hide FAB when in NoteContentFragment
                    fab.visibility = View.GONE
                }
                else -> {
                    // Show FAB when in the dashboard or any other fragment
                    fab.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onBackPressed() {
        val navController = findNavController(R.id.nav_host_fragment)

        if (navController.currentDestination?.id == R.id.noteContentFragment) {
            // When pressing back from NoteContentFragment, return to dashboard
            navController.popBackStack()

            // Make the dashboard layout visible again
            dashBoardLayout.visibility = View.VISIBLE

            // Ensure the FAB is visible again
            fab.visibility = View.VISIBLE

            // Hide the fragment container again since we're back on the dashboard
            navHostFragmentContainer.visibility = View.GONE
        } else {
            super.onBackPressed()
        }
    }
}




