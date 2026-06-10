package com.boggle.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDataAccessObject {
    @Insert
    suspend fun insertGame(gameResult: GameResult): Long

    @Update
    suspend fun updateGame(gameResult: GameResult)

    @Query("SELECT * FROM game_history ORDER BY dateMillis DESC")
    fun getAllGames(): Flow<List<GameResult>>

    @Query("DELETE FROM game_history")
    suspend fun deleteAllGames()
}