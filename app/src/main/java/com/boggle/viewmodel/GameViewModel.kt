package com.boggle.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.boggle.data.AppDatabase
import com.boggle.logic.BoggleSolver
import com.boggle.logic.GameEngine
import com.boggle.data.GameResult
import com.boggle.logic.Trie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.text.iterator

class GameViewModel(application: Application) : AndroidViewModel(application) {
    // wspólny game engine dla wszystkich fragmentów
    val gameEngine = GameEngine()
    // dostęp do bazy danych
    private val gameDAO = AppDatabase.Companion.getDatabase(application).gameDAO()
    val allGamesHistory: Flow<List<GameResult>> = gameDAO.getAllGames()

    // funkcja ładująca słownik po wylosowaniu planszy (w osobnym wątku)
    suspend fun loadDictionaryForBoard(board: String): Set<String> {
        return withContext(Dispatchers.IO) {
            val dictionaryTrie = Trie()
            val availableLettersSet = board.toSet()
            // TODO: początki słów 2literowe - trzyliterowe?

            val inputStream = getApplication<Application>().assets.open("slownik.txt")

            inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val word = line.trim().uppercase()

                    if (word.length in 3..16) {

                        var canBeFormed = true
                        for (char in word) {
                            if (!availableLettersSet.contains(char)) {
                                canBeFormed = false
                                break
                            }
                        }
                        if (canBeFormed) {
                            dictionaryTrie.insert(word)
                        }
                    }
                }
                val solver = BoggleSolver(dictionaryTrie)
                return@withContext solver.findAllWords(board)
            }
        }
    }

    fun saveGameResult() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = GameResult(
                score = gameEngine.currentScore,
                wordCount = gameEngine.foundWords.size,
                board = gameEngine.board,
                durationSeconds = gameEngine.roundTimeSecond,
            )
            gameDAO.insertGame(result)
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            gameDAO.deleteAllGames()
        }
    }
}