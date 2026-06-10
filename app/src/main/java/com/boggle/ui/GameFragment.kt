package com.boggle.ui



import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import android.graphics.Rect
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.boggle.R
import com.boggle.utils.Constants
import com.boggle.viewmodel.GameViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

class GameFragment : Fragment(R.layout.fragment_game) {
    private lateinit var timer: CountDownTimer
    private val selectedViews = mutableListOf<TextView>()
    private val currentWord = StringBuilder()
    private val selectedCoordinates = mutableListOf<Int>()
    private val gameViewModel : GameViewModel by activityViewModels()

    private var soundPool: android.media.SoundPool? = null
    private var soundWrongId: Int = 0
    private var soundLen3Id: Int = 0
    private var soundLen4Id: Int = 0
    private var soundLen5Id: Int = 0
    private var soundLenMaxId: Int = 0

    private var isSoundEnabled: Boolean = true
    private var isVibrationEnabled: Boolean = true
    
    private var tvGuessedWords: TextView? = null
    private var tvScore: TextView? = null
    private val guessedWordsBuilder = java.lang.StringBuilder()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        tvGuessedWords = view.findViewById(R.id.tv_guessed_words)
        tvScore = view.findViewById(R.id.tv_score)
        guessedWordsBuilder.clear()
        tvGuessedWords?.text = ""

        val audioAttributes = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_GAME)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = android.media.SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool?.let { pool ->
            soundWrongId = pool.load(requireContext(), R.raw.sound_wrong, 1)
            soundLen3Id = pool.load(requireContext(), R.raw.sound_3, 1)
            soundLen4Id = pool.load(requireContext(), R.raw.sound_4, 1)
            soundLen5Id = pool.load(requireContext(), R.raw.sound_5, 1)
            soundLenMaxId = pool.load(requireContext(), R.raw.sound_max, 1)
        }

        val prefs = requireActivity().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        isSoundEnabled = prefs.getBoolean(Constants.PREF_SOUND_ENABLED, true)
        isVibrationEnabled = prefs.getBoolean(Constants.PREF_VIBRATION_ENABLED, true)

        // reset gry i ustawienie nowej planszy
        // ustawianie seeda do losowania planszy i czasu gry
        val seed = gameViewModel.currentInGameSeed
        if (seed != null) {
            gameViewModel.resetMultiplayerGame()
            gameViewModel.gameEngine.setNewGame(seed)
            gameViewModel.gameEngine.roundTimeSecond = gameViewModel.currentInGameDuration
        } else {
            gameViewModel.gameEngine.setNewGame()
            gameViewModel.gameEngine.roundTimeSecond = prefs.getInt(Constants.PREF_GAME_TIME, 120)
        }


        val tvTimer = view.findViewById<TextView>(R.id.tv_timer)
        val btnExit = view.findViewById<ImageButton>(R.id.btn_exit)
        val btnRotate = view.findViewById<ImageButton>(R.id.btn_rotate)
        val progressBar = view.findViewById<ProgressBar>(R.id.progress_game_loading)
        val gridLayout = view.findViewById<GridLayout>(R.id.grid_board)
        val tvOpponentScore = view.findViewById<TextView>(R.id.tv_rival_score)
        if (seed == null) tvOpponentScore.visibility = View.INVISIBLE
        progressBar.visibility = View.VISIBLE
        gridLayout.visibility = View.INVISIBLE

        // Blokowanie przycisku wstecz
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // NIC
            }
        })

        // load all possible word
        viewLifecycleOwner.lifecycleScope.launch {
            val possibleWords = gameViewModel.loadDictionaryForBoard(gameViewModel.gameEngine.board)
            gameViewModel.gameEngine.setAllPossibleWords(possibleWords)
            setupBoard(gridLayout)
            
            val isMultiplayer = gameViewModel.currentInGameSeed != null
            if (isMultiplayer) {
                if (gameViewModel.isHost) {
                    gameViewModel.hostReady()
                } else {
                    gameViewModel.clientReady()
                }
            } else {
                progressBar.visibility = View.GONE
                gridLayout.visibility = View.VISIBLE
                timer.start()
            }
        }

        if (gameViewModel.currentInGameSeed != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                gameViewModel.startTimerEvent.collect {
                    progressBar.visibility = View.GONE
                    gridLayout.visibility = View.VISIBLE
                    timer.start()
                }
            }
        }

        // Powrót do menu
        btnExit.setOnClickListener {
            findNavController().navigate(R.id.action_gameFragment_to_menuFragment)
        }

        // Obrót planszy
        btnRotate.setOnClickListener {
            gridLayout.rotation += 90f
            for (i in 0 until gridLayout.childCount) {
                val child = gridLayout.getChildAt(i)
                child.rotation -= 90f
            }
        }

        // Start timera
        timer = object : CountDownTimer(gameViewModel.gameEngine.roundTimeSecond * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                tvTimer.text = String.format("%02d:%02d", seconds / 60, seconds % 60)
            }

            // po końcu czasu
            override fun onFinish() {
                if (gameViewModel.currentInGameSeed != null) {
                    val finalScore = gameViewModel.gameEngine.currentScore
                    val wordsList = gameViewModel.gameEngine.foundWords.joinToString(",")
                    gameViewModel.boggleBluetoothManager.sendMessage("FINAL_RESULTS:$finalScore:$wordsList")
                }
                gameViewModel.saveGameResult()
                findNavController().navigate(R.id.action_gameFragment_to_resultsFragment)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            gameViewModel.opponentLiveScore.collect { score ->
                tvOpponentScore.text = getString(R.string.game_opponent_score_simple, score)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    private fun setupBoard(gridLayout: GridLayout) {
        val stringBoard = gameViewModel.gameEngine.board

        // Generujemy 16 liter na planszy
        for (i in 0 until 16) {
            val textView = TextView(context).apply {
                text = stringBoard[i].toString()
                textSize = 32f
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.tile_normal)
                // liczenie kafelków z szerokości ekranu
                val widthPixels = resources.displayMetrics.widthPixels
                val density = resources.displayMetrics.density
                val screenPaddingPx = (32 * density).toInt()
                val availableWidth = widthPixels - screenPaddingPx
                val marginDp = 13 // przy mniejszym łatwo zaznaczyć niechciany
                val blockMarginPx = (marginDp * density).toInt()
                val totalBlockMarginsInRowPx = blockMarginPx * 8
                val cellSizePx = (availableWidth - totalBlockMarginsInRowPx) / 4

                layoutParams = GridLayout.LayoutParams().apply {
                    width = cellSizePx
                    height = cellSizePx

                    setMargins(blockMarginPx, blockMarginPx, blockMarginPx, blockMarginPx)
                }
            }
            gridLayout.addView(textView)
        }

        // Dodajemy "detektor ruchu" na CAŁĄ planszę
        gridLayout.setOnTouchListener { _, event ->
            val x = event.x.toInt()
            val y = event.y.toInt()

            when (event.action) {
                // Gdy użytkownik dotyka ekranu LUB przesuwa palcem
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {

                    // Sprawdzamy, nad którym klockiem znajduje się palec
                    for (i in 0 until gridLayout.childCount) {
                        val child = gridLayout.getChildAt(i) as TextView
                        val rect = Rect()
                        child.getHitRect(rect) // Pobiera granice klocka

                        // Jeśli palec jest wewnątrz granic tego klocka
                        if (rect.contains(x, y)) {
                            // sprawdzenie, czy sąsiaduje z ostatnim
                            if (selectedCoordinates.isNotEmpty() &&
                                (abs(selectedCoordinates[selectedCoordinates.size - 1] % 4 - i % 4) > 1
                                        || abs(selectedCoordinates[selectedCoordinates.size - 1] / 4 - i / 4) > 1))
                                break
                            // powrot i usuniecie ostatniego
                            if (selectedViews.size >= 2 && selectedViews[selectedViews.size - 2] == child) {
                                selectedViews[selectedViews.size - 1].setBackgroundResource(R.drawable.tile_normal)
                                selectedViews.removeAt(selectedViews.size - 1)
                                currentWord.deleteCharAt(currentWord.length - 1)
                                selectedCoordinates.removeAt(selectedCoordinates.size - 1)
                            }
                            // Jeśli jeszcze go nie zaznaczyliśmy w tym ruchu
                            if (!selectedViews.contains(child)) {
                                selectedViews.add(child)
                                currentWord.append(child.text)
                                selectedCoordinates.add(i)
                                child.setBackgroundResource(R.drawable.tile_selected)
                            }
                            break
                        }
                    }
                }

                // Gdy użytkownik oderwie palec od ekranu (koniec rysowania)
                MotionEvent.ACTION_UP -> {
                    val finalWord = currentWord.toString()

                    if (finalWord.isNotEmpty()) {
                        val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        if (gameViewModel.gameEngine.checkWord(finalWord)) {
                            // Dobre słowo - heavy click
                            if (isVibrationEnabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator.vibrate(50)
                                }
                            }

                            // dodajemy slowo na gorze listy
                            guessedWordsBuilder.insert(0, finalWord + "\n")
                            tvGuessedWords?.text = guessedWordsBuilder.toString()

                            tvScore?.text = getString(R.string.game_score, gameViewModel.gameEngine.currentScore)
                            if (gameViewModel.currentInGameSeed != null) {
                                gameViewModel.sendMyScoreUpdate(gameViewModel.gameEngine.currentScore)
                            }
                            
                            // Dźwięk sukcesu
                            if (isSoundEnabled) {
                                val soundId = when {
                                    finalWord.length == 3 -> soundLen3Id
                                    finalWord.length == 4 -> soundLen4Id
                                    finalWord.length == 5 -> soundLen5Id
                                    finalWord.length > 5 -> soundLenMaxId
                                    else -> 0
                                }
                                if (soundId != 0) {
                                    soundPool?.play(soundId, 1f, 1f, 0, 0, 1f)
                                }
                            }
                        } else {
                            // Złe słowo-wibracja 200ms
                            if (finalWord.length > 1) {
                                if (isVibrationEnabled) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                                    } else {
                                        @Suppress("DEPRECATION")
                                        vibrator.vibrate(200)
                                    }
                                }
                                if (isSoundEnabled) {
                                    soundPool?.play(soundWrongId, 1f, 1f, 0, 0, 1f)
                                }
                            }
                        }
                    }

                    // Resetujemy planszę do kolejnego słowa
                    selectedViews.forEach { it.setBackgroundResource(R.drawable.tile_normal) }
                    selectedViews.clear()
                    currentWord.clear()
                    selectedCoordinates.clear()
                }
            }
            true // dla androida: zajęliśmy się tym dotykiem
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer.cancel() // Ważne
        soundPool?.release()
        soundPool = null
        tvGuessedWords = null
        tvScore = null
    }
}