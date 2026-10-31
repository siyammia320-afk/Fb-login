package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AccountItem
import com.example.data.AccountRepository
import com.example.network.LoginResult
import com.example.network.WBloksLoginRepository
import com.example.service.FloatingOverlayService
import com.example.util.AppUpdateManager
import com.example.util.SecurityGuard
import com.example.util.UpdateInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val accountRepo = AccountRepository.getInstance(application)
    private val loginRepo = WBloksLoginRepository(application)

    val accounts: StateFlow<List<AccountItem>> = accountRepo.accounts
    val savedNumber: StateFlow<String> = accountRepo.savedNumber
    val savedPassword: StateFlow<String> = accountRepo.savedPassword
    val savedApiUrl: StateFlow<String> = accountRepo.savedApiUrl
    val latestOtp: StateFlow<String> = accountRepo.latestOtp

    private val _isOverlayRunning = MutableStateFlow(false)
    val isOverlayRunning: StateFlow<Boolean> = _isOverlayRunning.asStateFlow()

    // Real-Time Pastebin Switch State: null = checking, true = ON, false = OFF
    private val _isRemoteSwitchOn = MutableStateFlow<Boolean?>(null)
    val isRemoteSwitchOn: StateFlow<Boolean?> = _isRemoteSwitchOn.asStateFlow()

    private val _loginStatusMessage = MutableStateFlow("")
    val loginStatusMessage: StateFlow<String> = _loginStatusMessage.asStateFlow()

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    // Auto-Update State
    private val _isCheckingUpdate = MutableStateFlow(true)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _requiredUpdate = MutableStateFlow<UpdateInfo?>(null)
    val requiredUpdate: StateFlow<UpdateInfo?> = _requiredUpdate.asStateFlow()

    private val _isDownloadingUpdate = MutableStateFlow(false)
    val isDownloadingUpdate: StateFlow<Boolean> = _isDownloadingUpdate.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    private val _updateErrorMessage = MutableStateFlow("")
    val updateErrorMessage: StateFlow<String> = _updateErrorMessage.asStateFlow()

    private var downloadedApkFile: File? = null

    init {
        SecurityGuard.verifySecurityOrExit(application)
        checkForAppUpdate()
        startRealtimeRemoteSwitchPolling()
    }

    private fun startRealtimeRemoteSwitchPolling() {
        viewModelScope.launch {
            while (isActive) {
                val app = getApplication<Application>()
                SecurityGuard.verifySecurityOrExit(app)
                val isOn = SecurityGuard.checkRemoteSwitchIsOn(app)
                _isRemoteSwitchOn.value = isOn
                if (!isOn) {
                    app.stopService(Intent(app, FloatingOverlayService::class.java))
                    _isOverlayRunning.value = false
                }
                delay(4000)
            }
        }
    }

    fun refreshRemoteSwitchNow() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            SecurityGuard.verifySecurityOrExit(app)
            val isOn = SecurityGuard.checkRemoteSwitchIsOn(app)
            _isRemoteSwitchOn.value = isOn
            if (!isOn) {
                app.stopService(Intent(app, FloatingOverlayService::class.java))
                _isOverlayRunning.value = false
            }
        }
    }

    fun checkForAppUpdate() {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val update = AppUpdateManager.checkForUpdate()
            _requiredUpdate.value = update
            _isCheckingUpdate.value = false

            if (update != null) {
                // Force stop floating overlay if an update is required
                val app = getApplication<Application>()
                app.stopService(Intent(app, FloatingOverlayService::class.java))
                _isOverlayRunning.value = false
            }
        }
    }

    fun startOrInstallUpdate(context: Context) {
        val update = _requiredUpdate.value ?: return
        if (_isDownloadingUpdate.value) return

        if (!AppUpdateManager.canInstallUnknownApps(context)) {
            AppUpdateManager.openInstallUnknownAppsSettings(context)
            return
        }

        val existingFile = downloadedApkFile
        if (existingFile != null && existingFile.exists() && existingFile.length() > 0) {
            AppUpdateManager.installApk(context, existingFile)
            return
        }

        _isDownloadingUpdate.value = true
        _downloadProgress.value = 0
        _updateErrorMessage.value = ""

        viewModelScope.launch {
            val file = AppUpdateManager.downloadUpdateApk(
                context = context,
                downloadUrl = update.downloadUrl,
                onProgress = { progress ->
                    _downloadProgress.value = progress
                }
            )
            _isDownloadingUpdate.value = false
            if (file != null && file.exists()) {
                downloadedApkFile = file
                AppUpdateManager.installApk(context, file)
            } else {
                _updateErrorMessage.value = "Download failed! Check internet and try again."
            }
        }
    }

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
