package com.cs407.lab5_milestone

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cs407.lab5_milestone.data.NoteDatabase
import com.cs407.lab5_milestone.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class LoginFragment(
    private val injectedUserViewModel: UserViewModel? = null // For testing only
) : Fragment() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var errorTextView: TextView

    private lateinit var userViewModel: UserViewModel

    private lateinit var userPasswdKV: SharedPreferences
    private lateinit var noteDB: NoteDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        usernameEditText = view.findViewById(R.id.usernameEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        loginButton = view.findViewById(R.id.loginButton)
        errorTextView = view.findViewById(R.id.errorTextView)

        userViewModel = if (injectedUserViewModel != null) {
            injectedUserViewModel
        } else {
            ViewModelProvider(requireActivity())[UserViewModel::class.java]
        }

        // TODO - Get shared preferences from using R.string.userPasswdKV as the name
        userPasswdKV = requireContext().getSharedPreferences(getString(R.string.userPasswdKV), Context.MODE_PRIVATE)
        noteDB = NoteDatabase.getDatabase(requireContext())
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        usernameEditText.doAfterTextChanged {
            errorTextView.visibility = View.GONE
        }
        passwordEditText.doAfterTextChanged {
            errorTextView.visibility = View.GONE
        }
        // Set the login button click action
        loginButton.setOnClickListener {
            // TODO: Get the entered username and password from EditText fields
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            if (username.isEmpty() || password.isEmpty()) {
                errorTextView.visibility = View.VISIBLE
            }else{
                lifecycleScope.launch(Dispatchers.IO) {
                    val ins = withContext(Dispatchers.IO) {getUserPasswd(username, password)}
                    if (ins) {
                        val userId = withContext(Dispatchers.IO){noteDB.userDao().getByName(username).userId}
                        withContext(Dispatchers.Main) {
                            userViewModel.setUser(UserState(userId, username, password))
                            findNavController().navigate(R.id.noteListFragment)
                        }
                    } else {
                        errorTextView.visibility = View.VISIBLE
                    }
                }
            }
        }
    }


    private suspend fun getUserPasswd(
        name: String,
        passwdPlain: String
    ): Boolean {
        // TODO: Hash the plain password using a secure hashing function
        val hashedPassword = hash(passwdPlain)
        if (userPasswdKV.contains(name)) {
            val passwordInKV = userPasswdKV.getString(name, null)
            if (hashedPassword != passwordInKV) {
                return false
            }
        }else {
            withContext(Dispatchers.IO) {
                noteDB.userDao().insert(User(userName = name))
            }
            with(userPasswdKV.edit()) {
                putString(name, hashedPassword)
                apply()
            }
        }
        return true
    }


        // TODO: Retrieve the stored password from SharedPreferences

        // TODO: Compare the hashed password with the stored one and return false if they don't match

        // TODO: If the user doesn't exist in SharedPreferences, create a new user

        // TODO: Insert the new user into the Room database (implement this in your User DAO)

        // TODO: Store the hashed password in SharedPreferences for future logins

        // TODO: Return true if the user login is successful or the user was newly created

    private fun hash(input: String): String {
        return MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }
}