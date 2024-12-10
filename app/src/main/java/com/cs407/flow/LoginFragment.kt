package com.cs407.flow

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cs407.flow.data.TaskDatabase
import com.cs407.flow.data.User
import com.cs407.flow.data.resetDatabase
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.cs407.flow.data.resetDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class LoginFragment(
    private val injectedUserViewModel: UserViewModel? = null
) : Fragment() {

    private lateinit var usernameEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var signUpButton: MaterialButton
    private lateinit var forgotPasswordTextView: TextView
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

        userViewModel = injectedUserViewModel ?: ViewModelProvider(requireActivity())[UserViewModel::class.java]
        userPasswdKV = requireContext().getSharedPreferences(getString(R.string.userPasswdKV), Context.MODE_PRIVATE)
        taskDB = TaskDatabase.getDatabase(requireContext())

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // Hide error when text changes
        usernameEditText.doAfterTextChanged { errorTextView.visibility = View.GONE }
        passwordEditText.doAfterTextChanged { errorTextView.visibility = View.GONE }

        loginButton.setOnClickListener {
            //resetDatabase(requireContext())
            attemptLogin()
        }

    }

    private fun attemptLogin() {
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            errorTextView.text = getString(R.string.fail_login)
            errorTextView.visibility = View.VISIBLE
        } else {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val isSuccessful = withContext(Dispatchers.IO) { getUserPasswd(username, password) }
                    if (isSuccessful) {
                        val userId = withContext(Dispatchers.IO) { taskDB.userDao().getByName(username).userId }
                        userViewModel.setUser(UserState(userId, username, password))
                        findNavController().navigate(R.id.action_loginFragment_to_taskListFragment)
                    } else {
                        errorTextView.text = getString(R.string.fail_login)
                        errorTextView.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    errorTextView.text = "An error occurred: ${e.message}"
                    errorTextView.visibility = View.VISIBLE
                }
            }
        }
    }

    private suspend fun getUserPasswd(name: String, passwdPlain: String): Boolean {
        val hashedPassword = hash(passwdPlain)
        return withContext(Dispatchers.IO) {
            if (userPasswdKV.contains(name)) {
                val passwordInKV = userPasswdKV.getString(name, null)
                if (hashedPassword != passwordInKV) {
                    return@withContext false
                }
            } else {
                taskDB.userDao().insert(User(userName = name))
                with(userPasswdKV.edit()) {
                    putString(name, hashedPassword)
                    apply()
                }
            }
            return@withContext true
        }
    }

    private fun hash(input: String): String {
        return MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
