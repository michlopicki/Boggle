package com.boggle

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
import com.google.android.material.button.MaterialButtonToggleGroup

class ResultsFragment : Fragment(R.layout.fragment_results) {

    private val gameViewModel: GameViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val container = view.findViewById<LinearLayout>(R.id.container_words)
        val toggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.toggle_group)

        // TODO: Hide opponent in singleplayer

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
        view.findViewById<Button>(R.id.btn_replay).setOnClickListener {
            findNavController().navigate(R.id.action_resultsFragment_to_gameFragment)
        }
        view.findViewById<Button>(R.id.btn_all_words).setOnClickListener {
            findNavController().navigate(R.id.action_resultsFragment_to_allWordsFragment)
        }

    }

    private fun findViewById(containerWords: Int) {}

    private fun displayWords(container: LinearLayout, isOpponent: Boolean, view: View) {
        val tvFinalScore = view.findViewById<TextView>(R.id.tv_final_score)
        val finalScore = if (isOpponent) gameViewModel.gameEngine.opponentScore else gameViewModel.gameEngine.currentScore
        tvFinalScore.text = "Suma: ${finalScore} pkt"

        val tvWordCount = view.findViewById<TextView>(R.id.tv_word_count)
        val wordsMap = if (isOpponent) gameViewModel.gameEngine.opponentWords else gameViewModel.gameEngine.foundWords
        tvWordCount.text = "Znaleziono: ${wordsMap.size} słów"

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