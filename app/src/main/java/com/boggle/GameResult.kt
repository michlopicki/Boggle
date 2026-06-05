package com.boggle

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_history")
data class GameResult(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val score: Int,
    val wordCount: Int,
    val board: String,
    val durationSeconds: Int,
    val dateMillis: Long = System.currentTimeMillis()
)