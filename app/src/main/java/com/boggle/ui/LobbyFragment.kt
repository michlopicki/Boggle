package com.boggle.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.fragment.app.activityViewModels
import com.boggle.R
import com.boggle.viewmodel.GameViewModel
import kotlin.getValue

class LobbyFragment : Fragment(R.layout.fragment_lobby) {
    private val gameViewModel: GameViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

    }
}