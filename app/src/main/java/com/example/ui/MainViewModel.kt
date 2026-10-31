package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AccountItem
import com.example.data.AccountRepository
import com.example.network.LoginResult
import com.example.network.WBloksLoginRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val accountRepo = AccountRepository.getInstance(application)
    private val loginRepo = WBloksLoginRepository()

    val accounts: StateFlow<List<AccountItem>> = accountRepo.accounts
    val savedNumber: StateFlow<String> = accountRepo.savedNumber
    val savedPassword: StateFlow<String> = accountRepo.savedPassword
    val savedApiUrl: StateFlow<String> = accountRepo.savedApiUrl
    val latestOtp: StateFlow<String> = accountRepo.latestOtp

    private val _isOverlayRunning = MutableStateFlow(false)
    val isOverlayRunning: StateFlow<Boolean> = _isOverlayRunning.asStateFlow()

    private val _loginStatusMessage = MutableStateFlow("")
    val loginStatusMessage: StateFlow<String> = _loginStatusMessage.asStateFlow()

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    fun updateNumber(phone: String) {
        accountRepo.saveNumber(phone)
    }

    fun updatePassword(pass: String) {
        accountRepo.savePassword(pass)
    }

    fun updateApiUrl(url: String) {
        accountRepo.saveApiUrl(url)
    }

    fun setOverlayRunning(running: Boolean) {
        _isOverlayRunning.value = running
    }

    fun deleteAccount(id: String) {
        accountRepo.removeAccount(id)
    }

    fun clearAll() {
        accountRepo.clearAllAccounts()
    }

    fun triggerLogin(phoneInput: String? = null, passInput: String? = null) {
        val phone = phoneInput ?: savedNumber.value.trim()
        val pass = passInput ?: savedPassword.value.trim()
        val customUrl = savedApiUrl.value.trim()

        if (phone.isBlank()) {
            _loginStatusMessage.value = "Enter Number first!"
            return
        }
        if (pass.isBlank()) {
            _loginStatusMessage.value = "Enter Password first!"
            return
        }

        _isLoggingIn.value = true
        _loginStatusMessage.value = "Logging in: $phone..."

        viewModelScope.launch {
            val result = loginRepo.executeWBloksLogin(phone, pass, customUrl)
            _isLoggingIn.value = false
            when (result) {
                is LoginResult.Success -> {
                    accountRepo.saveAccount(result.account)
                    _loginStatusMessage.value = "Success! UID: ${result.account.uid}"
                }
                is LoginResult.Failure -> {
                    val failItem = AccountItem(
                        number = phone,
                        password = pass,
                        cookie = "",
                        uid = "",
                        status = result.status
                    )
                    accountRepo.saveAccount(failItem)
                    _loginStatusMessage.value = "Failed (${result.status}): ${result.message}"
                }
            }
        }
    }
}
