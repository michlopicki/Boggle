package com.boggle.ui

//TODO: rotate board button (potrzasanie?)
//TODO: obrot ekranu (na razie w manifest)
//TODO: notch nachodzi na tekst
//TODO: głośnik i wibracje


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
import android.view.Gravity
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.boggle.R
import com.boggle.viewmodel.GameViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

class GameFragment : Fragment(R.layout.fragment_game) {
    private lateinit var timer: CountDownTimer
    private val selectedViews = mutableListOf<TextView>()
    private val currentWord = StringBuilder()
    private val selectedCordinates = mutableListOf<Int>()
    private val gameViewModel : GameViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // reset gry i ustawienie nowej planszy
        gameViewModel.gameEngine.setNewGame()
        // pobranie czsu gry
        gameViewModel.gameEngine.roundTimeSecond = requireActivity().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE).getInt("game_time", 120)

        val tvTimer = view.findViewById<TextView>(R.id.tv_timer)
        val btnExit = view.findViewById<ImageButton>(R.id.btn_exit)
        val progressBar = view.findViewById<ProgressBar>(R.id.progress_game_loading)
        val gridLayout = view.findViewById<GridLayout>(R.id.grid_board)

        progressBar.visibility = View.VISIBLE

        // TODO: handle back button

        // load all possible word
        viewLifecycleOwner.lifecycleScope.launch {
            val possibleWords = gameViewModel.loadDictionaryForBoard(gameViewModel.gameEngine.board)
            gameViewModel.gameEngine.setAllPossibleWords(possibleWords)
            setupBoard(gridLayout)
            progressBar.visibility = View.GONE
            gridLayout.visibility = View.VISIBLE

            timer.start()
        }

        // Powrót do menu
        btnExit.setOnClickListener {
            findNavController().navigate(R.id.action_gameFragment_to_menuFragment)
        }

        // Start timera
        timer = object : CountDownTimer(gameViewModel.gameEngine.roundTimeSecond * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                tvTimer.text = String.format("%02d:%02d", seconds / 60, seconds % 60)
            }

            // po końcu czasu
            override fun onFinish() {
                gameViewModel.saveGameResult()
                findNavController().navigate(R.id.action_gameFragment_to_resultsFragment)
            }
        }


    }

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
                val marginDp = 8
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
                            // sprawdzenie czy sąsiaduje z ostatnim
                            if (selectedCordinates.isNotEmpty() &&
                                (abs(selectedCordinates[selectedCordinates.size - 1] % 4 - i % 4) > 1
                                        || abs(selectedCordinates[selectedCordinates.size - 1] / 4 - i / 4) > 1))
                                break
                            // powrot i usuniecie ostatniego
                            if (selectedViews.size >= 2 && selectedViews[selectedViews.size - 2] == child) {
                                selectedViews[selectedViews.size - 1].setBackgroundResource(R.drawable.tile_normal)
                                selectedViews.removeAt(selectedViews.size - 1)
                                currentWord.deleteCharAt(currentWord.length - 1)
                                selectedCordinates.removeAt(selectedCordinates.size - 1)
                            }
                            // Jeśli jeszcze go nie zaznaczyliśmy w tym ruchu
                            if (!selectedViews.contains(child)) {
                                selectedViews.add(child)
                                currentWord.append(child.text)
                                selectedCordinates.add(i)
                                child.setBackgroundResource(R.drawable.tile_selected)
                            }
                            break
                        }
                    }
                }

                // Gdy użytkownik oderwie palec od ekranu (koniec rysowania)
                MotionEvent.ACTION_UP -> {
                    val finalWord = currentWord.toString()

                    if (gameViewModel.gameEngine.checkWord(finalWord)){
                        val tvGuessedWords = requireView().findViewById<TextView>(R.id.tv_guessed_words)
                        val tvScore = requireView().findViewById<TextView>(R.id.tv_score)

                        // dodajemy slowo na gorze listy
                        tvGuessedWords.text = finalWord + "\n" + tvGuessedWords.text.toString()

                        tvScore.text = "Pkt: ${gameViewModel.gameEngine.currentScore}"
                    }


                    // Resetujemy planszę do kolejnego słowa
                    selectedViews.forEach { it.setBackgroundResource(R.drawable.tile_normal) }
                    selectedViews.clear()
                    currentWord.clear()
                    selectedCordinates.clear()
                }
            }
            true // dla androida: zajęliśmy się tym dotykiem
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer.cancel() // Ważne
    }
}