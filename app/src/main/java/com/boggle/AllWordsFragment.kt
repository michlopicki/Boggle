package com.boggle

import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import kotlin.getValue

class AllWordsFragment : Fragment(R.layout.fragment_all_words) {
    private val gameViewModel: GameViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvWordCount = view.findViewById<TextView>(R.id.tv_total_possible_words)
        tvWordCount.text = "Możliwych słów na planszy: ${gameViewModel.gameEngine.getAllWords().size}"

        val container = view.findViewById<LinearLayout>(R.id.container_all_words)
        displayWords(container)

        // Obsługa przycisków dolnych
        view.findViewById<Button>(R.id.btn_main_menu_all).setOnClickListener {
            findNavController().navigate(R.id.action_allWordsFragment_to_menuFragment)
        }
        view.findViewById<Button>(R.id.btn_replay_all).setOnClickListener {
            findNavController().navigate(R.id.action_allWordsFragment_to_gameFragment)
        }
        view.findViewById<Button>(R.id.btn_back_to_results).setOnClickListener {
            findNavController().popBackStack()
        }

    }

    private fun displayWords(container: LinearLayout) {
        container.removeAllViews()

        val wordsMap = gameViewModel.gameEngine.getAllWords()

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