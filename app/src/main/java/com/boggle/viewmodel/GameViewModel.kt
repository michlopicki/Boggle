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
import android.bluetooth.BluetoothManager
import android.content.Context
import com.boggle.R
import com.boggle.bluetooth.BluetoothCallback
import com.boggle.bluetooth.BoggleBluetoothManager
import com.boggle.utils.Constants
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class GameViewModel(application: Application) : AndroidViewModel(application), BluetoothCallback {
    var isHost = false

    // wspólny game engine dla wszystkich fragmentów
    val gameEngine = GameEngine()
    // dostęp do bazy danych
    private val gameDAO = AppDatabase.Companion.getDatabase(application).gameDAO()
    val allGamesHistory: Flow<List<GameResult>> = gameDAO.getAllGames()

    // funkcja ładująca słownik po wylosowaniu planszy (w osobnym wątku)
    suspend fun loadDictionaryForBoard(board: String): Set<String> {
        return withContext(Dispatchers.IO) {
            val dictionaryTrie = Trie()
            
            // Set for character lookup as requested
            val availableLettersSet = board.toSet()
            
            // Generujemy wszystkie możliwe 3-literowe początki słów, jakie da się ułożyć na tej konkretnej planszy
            val validPrefixes = mutableSetOf<String>()
            fun findPrefixes(r: Int, c: Int, current: String, visited: Int) {
                if (current.length == 3) {
                    validPrefixes.add(current)
                    return
                }
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        if (dr == 0 && dc == 0) continue
                        val nr = r + dr
                        val nc = c + dc
                        if (nr in 0..3 && nc in 0..3) {
                            val index = nr * 4 + nc
                            if ((visited and (1 shl index)) == 0) {
                                findPrefixes(nr, nc, current + board[index], visited or (1 shl index))
                            }
                        }
                    }
                }
            }
            
            for (r in 0 until 4) {
                for (c in 0 until 4) {
                    val index = r * 4 + c
                    findPrefixes(r, c, board[index].toString(), 1 shl index)
                }
            }

            val inputStream = getApplication<Application>().assets.open("slownik.txt")

            inputStream.bufferedReader().useLines { lines ->
                lines.forEach { word ->
                    // czy wszystkie litery słowa w ogóle są na planszy?
                    var canBeFormed = true
                    for (i in 0 until word.length) {
                        if (!availableLettersSet.contains(word[i])) {
                            canBeFormed = false
                            break
                        }
                    }
                    
                    // czy pierwsze 3 litery tego słowa tworzą poprawną ścieżkę na planszy?
                    if (canBeFormed) {
                        if (validPrefixes.contains(word.substring(0, 3))) {
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
        val score = gameEngine.currentScore
        val wordCount = gameEngine.foundWords.size
        val board = gameEngine.board
        val duration = currentInGameDuration
        val isMultiplayer = currentInGameSeed != null
        val opponentScore = _opponentFinalScore.value
        val opponentWordCount = _opponentWords.value.size

        val result = GameResult(
            score = score,
            wordCount = wordCount,
            board = board,
            durationSeconds = duration,
            isMultiplayer = isMultiplayer,
            opponentScore = opponentScore,
            opponentWordCount = opponentWordCount
        )

        lastSavedGameResultDeferred = CompletableDeferred()
        viewModelScope.launch(Dispatchers.IO) {
            val id = gameDAO.insertGame(result)
            val updated = result.copy(id = id.toInt())
            lastSavedGameResult = updated
            lastSavedGameResultDeferred?.complete(updated)
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            gameDAO.deleteAllGames()
        }
    }


    // ---------- BLUETOOTH -------------

    private val bluetoothManagerAdapter = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter = bluetoothManagerAdapter.adapter

    val boggleBluetoothManager = BoggleBluetoothManager(bluetoothAdapter, this)

    private val _multiplayerStatus = MutableStateFlow(application.getString(R.string.status_waiting))

    private var _opponentLiveScore = MutableStateFlow(0)
    val opponentLiveScore: StateFlow<Int> = _opponentLiveScore

    private val _opponentFinalScore = MutableStateFlow(0)
    val opponentFinalScore: StateFlow<Int> = _opponentFinalScore

    private val _opponentWords = MutableStateFlow<List<String>>(emptyList())
    val opponentWords: StateFlow<List<String>> = _opponentWords

    var currentInGameSeed: Long? = null
    var currentInGameDuration: Int = 120
    private val _gameStartEvent = MutableSharedFlow<Unit>(replay = 0)
    val gameStartEvent: SharedFlow<Unit> = _gameStartEvent

    var isClientReady = false
    var isHostReady = false
    private val _startTimerEvent = MutableSharedFlow<Unit>(replay = 0)
    val startTimerEvent: SharedFlow<Unit> = _startTimerEvent

    var wantsRematch = false
    var opponentWantsRematch = false

    private var lastSavedGameResult: GameResult? = null
    private var lastSavedGameResultDeferred: CompletableDeferred<GameResult>? = null

    override fun onConnectionSuccess() {
        _multiplayerStatus.value = getApplication<Application>().getString(R.string.status_connected)

        if (isHost) {
            isClientReady = false
            isHostReady = false
            wantsRematch = false
            opponentWantsRematch = false
            _opponentFinalScore.value = 0
            _opponentWords.value = emptyList()
            lastSavedGameResult = null
            lastSavedGameResultDeferred = null
            // Host losuje seed i wysyła go do Klienta
            val sharedSeed = System.currentTimeMillis()
            val duration = getApplication<Application>().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).getInt(Constants.PREF_GAME_TIME, 120)

            currentInGameSeed = sharedSeed
            currentInGameDuration = duration

            boggleBluetoothManager.sendMessage("START_GAME:$sharedSeed:$duration")

            // Host odpala grę u siebie
            viewModelScope.launch {
                _gameStartEvent.emit(Unit)
            }
        }
    }

    // Odebranie danych
    override fun onMessageReceived(message: String) {
        when {
            message.startsWith("START_GAME:") -> {
                isClientReady = false
                isHostReady = false
                wantsRematch = false
                opponentWantsRematch = false
                _opponentFinalScore.value = 0
                _opponentWords.value = emptyList()
                lastSavedGameResult = null
                lastSavedGameResultDeferred = null
                val parts = message.split(":")
                if (parts.size == 3) {
                    val seed = parts[1].toLong()
                    val duration = parts[2].toInt()

                    // Klient zapisuje dane otrzymane od Hosta
                    currentInGameSeed = seed
                    currentInGameDuration = duration

                    // Odpalamy grę u Klienta
                    viewModelScope.launch {
                        _gameStartEvent.emit(Unit)
                    }
                }
            }
            message.startsWith("SCORE:") -> {
                val score = message.substringAfter("SCORE:").toInt()
                _opponentLiveScore.value = score
            }
            message.startsWith("FINAL_RESULTS:") -> {
                val parts = message.removePrefix("FINAL_RESULTS:").split(":")
                if (parts.isNotEmpty()) {
                    val score = parts[0].toIntOrNull() ?: 0
                    _opponentFinalScore.value = score
                    if (parts.size > 1 && parts[1].isNotEmpty()) {
                        _opponentWords.value = parts[1].split(",")
                    } else {
                        _opponentWords.value = emptyList()
                    }
                    
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val saved = lastSavedGameResultDeferred?.await()
                            if (saved != null) {
                                val updated = saved.copy(
                                    opponentScore = score,
                                    opponentWordCount = _opponentWords.value.size
                                )
                                lastSavedGameResult = updated
                                gameDAO.updateGame(updated)
                            }
                        } catch (e: Exception) {
                            // Ignoruj błędy oczekiwania
                        }
                    }
                }
            }
            message == "CLIENT_READY" -> {
                isClientReady = true
                checkAndStartTimer()
            }
            message == "START_TIMER" -> {
                viewModelScope.launch {
                    _startTimerEvent.emit(Unit)
                }
            }
            message == "REMATCH_REQUEST" -> {
                opponentWantsRematch = true
                checkAndStartRematch()
            }
        }
    }

    fun hostReady() {
        isHostReady = true
        checkAndStartTimer()
    }

    fun clientReady() {
        boggleBluetoothManager.sendMessage("CLIENT_READY")
    }

    private fun checkAndStartTimer() {
        if (isHost && isHostReady && isClientReady) {
            boggleBluetoothManager.sendMessage("START_TIMER")
            viewModelScope.launch {
                _startTimerEvent.emit(Unit)
            }
        }
    }

    fun requestRematch() {
        if (wantsRematch) return
        wantsRematch = true
        boggleBluetoothManager.sendMessage("REMATCH_REQUEST")
        checkAndStartRematch()
    }

    private fun checkAndStartRematch() {
        if (wantsRematch && opponentWantsRematch) {
            wantsRematch = false
            opponentWantsRematch = false
            
            if (isHost) {
                isClientReady = false
                isHostReady = false
                _opponentFinalScore.value = 0
                _opponentWords.value = emptyList()
                lastSavedGameResult = null
                lastSavedGameResultDeferred = null

                val newSeed = System.currentTimeMillis()
                val duration = currentInGameDuration
                
                currentInGameSeed = newSeed
                boggleBluetoothManager.sendMessage("START_GAME:$newSeed:$duration")
                
                viewModelScope.launch {
                    _gameStartEvent.emit(Unit)
                }
            }
        }
    }

    fun sendMyScoreUpdate(newScore: Int) {
        boggleBluetoothManager.sendMessage("SCORE:$newScore")
    }

    override fun onConnectionFailed(error: String) { 
        _multiplayerStatus.value = getApplication<Application>().getString(R.string.status_error, error)
    }
    
    override fun onDisconnected() { 
        _multiplayerStatus.value = getApplication<Application>().getString(R.string.status_disconnected)
    }

    fun resetMultiplayerGame(){
        _opponentLiveScore.value = 0
    }
}