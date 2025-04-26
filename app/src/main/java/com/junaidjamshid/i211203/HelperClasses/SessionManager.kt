package com.junaidjamshid.i211203.HelperClasses

import android.content.Context
import android.content.SharedPreferences
import com.junaidjamshid.i211203.HelperClasses.DatabaseHelper

class SessionManager(context: Context) {
    private var prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private var dbHelper = DatabaseHelper(context)

    companion object {
        private const val PREF_NAME = "AppSession"
        private const val KEY_USER_ID = "userId"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_AUTH_TOKEN = "authToken"
        private const val KEY_TOKEN_EXPIRY = "tokenExpiry"
        private const val KEY_IS_FIRST_TIME = "isFirstTime"
    }

    fun createLoginSession(userId: String, token: String, expiryTimeMillis: Long) {
        val editor = prefs.edit()
        editor.putString(KEY_USER_ID, userId)
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_AUTH_TOKEN, token)
        editor.putLong(KEY_TOKEN_EXPIRY, expiryTimeMillis)
        editor.apply()

        // Also save token in SQLite for offline access
        dbHelper.saveAuthToken(userId, token, expiryTimeMillis)
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun isTokenValid(): Boolean {
        val expiry = prefs.getLong(KEY_TOKEN_EXPIRY, 0)
        return System.currentTimeMillis() < expiry
    }

    fun logout() {
        val userId = getUserId()
        val editor = prefs.edit()
        editor.clear()
        editor.apply()

        // Remove token from SQLite
        userId?.let {
            dbHelper.deleteAuthToken(it)
        }
    }

    fun setFirstTimeUser(isFirstTime: Boolean) {
        val editor = prefs.edit()
        editor.putBoolean(KEY_IS_FIRST_TIME, isFirstTime)
        editor.apply()
    }

    fun isFirstTimeUser(): Boolean {
        return prefs.getBoolean(KEY_IS_FIRST_TIME, true)
    }
}