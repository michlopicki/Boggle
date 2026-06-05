package com.boggle

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class RankingFragment : Fragment(R.layout.fragment_ranking) {
    private val gameViewModel: GameViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_ranking)
        val backButton = view.findViewById<Button>(R.id.back_to_menu_button)

        // TODO: toggle button for multiplayer

        val rankingAdapter = RankingAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = rankingAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            gameViewModel.allGamesHistory.collect { gamesList ->
                rankingAdapter.submitList(gamesList)
            }
        }

        backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}