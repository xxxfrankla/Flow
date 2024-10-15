package com.cs407.lab5_milestone

import androidx.lifecycle.ViewModel

data class UserState(/* Delete `void` and Add what you want */val void: String = "")

/* Use UserViewModel to cache user state (userId, userName, etc.)
    Don't forget to clean the state when log out/delete account
 */
/* Ref: https://developer.android.com/topic/libraries/architecture/viewmodel */
class UserViewModel : ViewModel() {
}