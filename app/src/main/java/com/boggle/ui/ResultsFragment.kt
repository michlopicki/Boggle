package com.boggle.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.fragment.app.activityViewModels
import com.boggle.R
import com.boggle.viewmodel.GameViewModel
import com.google.android.material.button.MaterialButtonToggleGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ResultsFragment : Fragment(R.layout.fragment_results) {

    private val gameViewModel: GameViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val container = view.findViewById<LinearLayout>(R.id.container_words)
        val toggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.toggle_group)

        // Hide opponent in singleplayer
        if (gameViewModel.currentInGameSeed == null) {
            toggleGroup.visibility = View.GONE
        }

        // Blokowanie przycisku wstecz
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // NIC
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            gameViewModel.opponentFinalScore.collect {
                if (toggleGroup.checkedButtonId == R.id.btn_opponent_results) {
                    displayWords(container, isOpponent = true, view)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            gameViewModel.gameStartEvent.collect {
                findNavController().navigate(R.id.action_resultsFragment_to_gameFragment)
            }
        }
        displayWords(container, isOpponent = false, view)

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_my_results -> displayWords(container, isOpponent = false, view)
                    R.id.btn_opponent_results -> displayWords(container, isOpponent = true, view)
                }
            }
        }

        // Obsługa przycisków dolnych
        view.findViewById<Button>(R.id.btn_main_menu).setOnClickListener {
            findNavController().navigate(R.id.action_resultsFragment_to_menuFragment)
        }
        val btnReplay = view.findViewById<Button>(R.id.btn_replay)
        btnReplay.setOnClickListener {
            if (gameViewModel.currentInGameSeed != null) {
                btnReplay.isEnabled = false
                btnReplay.text = "Czekam na przeciwnika..."
                gameViewModel.requestRematch()
            } else {
                findNavController().navigate(R.id.action_resultsFragment_to_gameFragment)
            }
        }
        view.findViewById<Button>(R.id.btn_all_words).setOnClickListener {
            findNavController().navigate(R.id.action_resultsFragment_to_allWordsFragment)
        }

    }

    private fun displayWords(container: LinearLayout, isOpponent: Boolean, view: View) {
        val tvFinalScore = view.findViewById<TextView>(R.id.tv_final_score)
        val finalScore = if (isOpponent) gameViewModel.opponentFinalScore.value else gameViewModel.gameEngine.currentScore
        tvFinalScore.text = getString(R.string.results_total_score, finalScore)

        val tvWordCount = view.findViewById<TextView>(R.id.tv_word_count)
        val wordsMap = if (isOpponent) gameViewModel.opponentWords.value.toSet() else gameViewModel.gameEngine.foundWords
        tvWordCount.text = getString(R.string.results_found_words, wordsMap.size)

        container.removeAllViews()


        wordsMap.forEach { word ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                setPadding(0, 8, 0, 8)
            }

            val tvWord = TextView(context).apply {
                text = word
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            }
            val points = gameViewModel.gameEngine.calculateScore(word)
            val tvPoints = TextView(context).apply {
                text = "$points pkt"
                gravity = Gravity.END
            }

            row.addView(tvWord)
            row.addView(tvPoints)
            container.addView(row)
        }

    }
}