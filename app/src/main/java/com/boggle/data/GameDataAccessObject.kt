package com.boggle.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDataAccessObject {
    // Zapisuje nowy wynik do bazy (działa w tle)
    @Insert
    suspend fun insertGame(gameResult: GameResult)

    // Pobiera całą historię
    @Query("SELECT * FROM game_history ORDER BY dateMillis DESC")
    fun getAllGames(): Flow<List<GameResult>>

    @Query("DELETE FROM game_history")
    suspend fun deleteAllGames()
}