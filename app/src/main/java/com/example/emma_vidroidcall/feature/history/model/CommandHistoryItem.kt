package com.example.emma_vidroidcall.feature.history.model

data class CommandHistoryItem(
    val id: String,
    val commandText: String,
    val time: String,
    val status: String,
    val category: String
)
