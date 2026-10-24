package com.example.quickpay.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.quickpay.data.model.User

class UserSessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("quickpay_user_session", Context.MODE_PRIVATE)

    fun saveUserSession(user: User) {
        prefs.edit().apply {
            putInt("KEY_USER_ID", user.id)
            putString("KEY_FULL_NAME", user.fullName)
            putString("KEY_EMAIL", user.email)
            putString("KEY_PHONE", user.phone)
            putString("KEY_ROLE", user.role)
            putString("KEY_ADDRESS", user.address ?: "")
            apply()
        }
    }

    fun getUser(): User? {
        val id = prefs.getInt("KEY_USER_ID", -1)
        if (id == -1) return null

        return User(
            id = id,
            fullName = prefs.getString("KEY_FULL_NAME", "") ?: "",
            email = prefs.getString("KEY_EMAIL", "") ?: "",
            phone = prefs.getString("KEY_PHONE", "") ?: "",
            role = prefs.getString("KEY_ROLE", "customer") ?: "customer",
            address = prefs.getString("KEY_ADDRESS", "")
        )
    }

    fun isLoggedIn(): Boolean = prefs.getInt("KEY_USER_ID", -1) != -1

    fun isAdmin(): Boolean = getUser()?.role == "admin"

    fun logout() {
        prefs.edit().clear().apply()
    }
}
