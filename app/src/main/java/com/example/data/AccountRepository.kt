package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AccountRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("fb_login_prefs", Context.MODE_PRIVATE)

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val type = Types.newParameterizedType(List::class.java, AccountItem::class.java)
    private val jsonAdapter = moshi.adapter<List<AccountItem>>(type)

    private val _accounts = MutableStateFlow<List<AccountItem>>(emptyList())
    val accounts: StateFlow<List<AccountItem>> = _accounts.asStateFlow()

    private val _savedNumber = MutableStateFlow(prefs.getString("saved_number", "") ?: "")
    val savedNumber: StateFlow<String> = _savedNumber.asStateFlow()

    private val _savedPassword = MutableStateFlow(prefs.getString("saved_pass", "") ?: "")
    val savedPassword: StateFlow<String> = _savedPassword.asStateFlow()

    private val _savedApiUrl = MutableStateFlow(prefs.getString("saved_api_url", "") ?: "")
    val savedApiUrl: StateFlow<String> = _savedApiUrl.asStateFlow()

    private val _latestOtp = MutableStateFlow("")
    val latestOtp: StateFlow<String> = _latestOtp.asStateFlow()

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        val json = prefs.getString("accounts_json", null)
        if (!json.isNullOrEmpty()) {
            try {
                val list = jsonAdapter.fromJson(json) ?: emptyList()
                _accounts.value = list
            } catch (e: Exception) {
                _accounts.value = emptyList()
            }
        }
    }

    fun saveAccount(account: AccountItem) {
        val current = _accounts.value.toMutableList()
        current.add(0, account) // newest on top
        _accounts.value = current
        saveAccountsToPrefs(current)
    }

    fun removeAccount(id: String) {
        val current = _accounts.value.filter { it.id != id }
        _accounts.value = current
        saveAccountsToPrefs(current)
    }

    fun clearAllAccounts() {
        _accounts.value = emptyList()
        prefs.edit().remove("accounts_json").apply()
    }

    private fun saveAccountsToPrefs(list: List<AccountItem>) {
        try {
            val json = jsonAdapter.toJson(list)
            prefs.edit().putString("accounts_json", json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveNumber(num: String) {
        _savedNumber.value = num
        prefs.edit().putString("saved_number", num).apply()
    }

    fun savePassword(pass: String) {
        _savedPassword.value = pass
        prefs.edit().putString("saved_pass", pass).apply()
    }

    fun saveApiUrl(url: String) {
        _savedApiUrl.value = url
        prefs.edit().putString("saved_api_url", url).apply()
    }

    fun setLatestOtp(otp: String) {
        _latestOtp.value = otp
    }

    companion object {
        @Volatile
        private var instance: AccountRepository? = null

        fun getInstance(context: Context): AccountRepository {
            return instance ?: synchronized(this) {
                instance ?: AccountRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
