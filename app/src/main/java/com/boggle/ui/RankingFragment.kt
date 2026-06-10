package com.boggle.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boggle.R
import com.boggle.data.GameResult
import com.boggle.viewmodel.GameViewModel
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.launch

class RankingFragment : Fragment(R.layout.fragment_ranking) {
    private val gameViewModel: GameViewModel by activityViewModels()
    private var allGames: List<GameResult> = emptyList()
    private var isMultiplayerSelected = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_ranking)
        val backButton = view.findViewById<Button>(R.id.back_to_menu_button)

        val toggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.toggle_ranking)

        val rankingAdapter = RankingAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = rankingAdapter

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isMultiplayerSelected = (checkedId == R.id.btn_multiplayer_ranking)
                updateList(rankingAdapter)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            gameViewModel.allGamesHistory.collect { gamesList ->
                allGames = gamesList
                updateList(rankingAdapter)
            }
        }

        backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun updateList(adapter: RankingAdapter) {
        val filtered = allGames.filter { it.isMultiplayer == isMultiplayerSelected }
        adapter.submitList(filtered)
    }
}