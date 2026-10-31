package com.example.data

import androidx.annotation.Keep

@Keep
data class AccountItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val number: String,
    val password: String = "",
    val cookie: String = "",
    val uid: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SUCCESS"
)
