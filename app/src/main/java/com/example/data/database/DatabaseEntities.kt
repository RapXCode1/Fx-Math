package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calc_history")
data class CalcHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

@Entity(tableName = "ai_sessions")
data class AiSession(
    @PrimaryKey val id: String, // UUID as String
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val selectedModel: String = "FYY-Llama 3.3"
)

@Entity(tableName = "ai_messages")
data class AiMessageItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: String,
    val sender: String, // "user" or "ai" or "system"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
