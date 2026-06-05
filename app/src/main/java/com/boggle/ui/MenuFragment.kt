package com.boggle.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import androidx.navigation.fragment.findNavController
import com.boggle.R

class MenuFragment : Fragment(R.layout.fragment_menu) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        view.findViewById<Button>(R.id.btn_singleplayer).setOnClickListener{
            findNavController().navigate(R.id.action_menuFragment_to_gameFragment)
        }

        view.findViewById<Button>(R.id.btn_multiplayer).setOnClickListener{
            findNavController().navigate(R.id.action_menuFragment_to_lobbyFragment)
        }

        view.findViewById<Button>(R.id.btn_ranking).setOnClickListener{
            findNavController().navigate(R.id.action_menuFragment_to_rankingFragment)
        }

        view.findViewById<Button>(R.id.btn_settings).setOnClickListener{
            findNavController().navigate(R.id.action_menuFragment_to_settingsFragment)
        }
    }

}