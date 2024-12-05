package com.cs407.flow

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
import com.cs407.flow.data.TaskDatabase
import com.cs407.flow.data.User
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
    private lateinit var taskDB: TaskDatabase

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
        userPasswdKV = requireContext().getSharedPreferences(getString(R.string.userPasswdKV), Context.MODE_PRIVATE)
        taskDB = TaskDatabase.getDatabase(requireContext())
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
            //resetDatabase(requireContext())
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            if (username.isEmpty() || password.isEmpty()) {
                errorTextView.visibility = View.VISIBLE
            }else{
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val ins = withContext(Dispatchers.IO) { getUserPasswd(username, password) }
                        if (ins) {
                            val userId = withContext(Dispatchers.IO) { taskDB.userDao().getByName(username).userId }
                            userViewModel.setUser(UserState(userId, username, password))
                            findNavController().navigate(R.id.action_loginFragment_to_taskListFragment)
                        } else {
                            errorTextView.visibility = View.VISIBLE
                        }
                    } catch (e: Exception) {
                        errorTextView.text = "An error occurred: ${e.message}"
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
        val hashedPassword = hash(passwdPlain)
        if (userPasswdKV.contains(name)) {
            val passwordInKV = userPasswdKV.getString(name, null)
            if (hashedPassword != passwordInKV) {
                return false
            }
        }else {
            withContext(Dispatchers.IO) {
                taskDB.userDao().insert(User(userName = name))
            }
            with(userPasswdKV.edit()) {
                putString(name, hashedPassword)
                apply()
            }
        }
        return true
    }

    private fun hash(input: String): String {
        return MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }
}