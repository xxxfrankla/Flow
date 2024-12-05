package com.cs407.flow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class InitialPage : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_initial_page, container, false)

        // Find the start button and set a click listener
        val startButton: Button = view.findViewById(R.id.start_button)
        startButton.setOnClickListener {
            // Navigate to the LoginFragment
            findNavController().navigate(R.id.action_initialPage_to_loginFragment)
        }

        return view
    }
}