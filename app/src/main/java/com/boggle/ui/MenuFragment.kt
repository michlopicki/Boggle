package com.boggle.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import androidx.navigation.fragment.findNavController
import com.boggle.R

import androidx.fragment.app.activityViewModels
import com.boggle.viewmodel.GameViewModel

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts

class MenuFragment : Fragment(R.layout.fragment_menu) {

    private val gameViewModel: GameViewModel by activityViewModels()

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            findNavController().navigate(R.id.action_menuFragment_to_lobbyFragment)
        } else {
            Toast.makeText(requireContext(), "Musisz włączyć Bluetooth, aby zagrać w Multiplayer!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        view.findViewById<Button>(R.id.btn_singleplayer).setOnClickListener{
            gameViewModel.currentInGameSeed = null
            findNavController().navigate(R.id.action_menuFragment_to_gameFragment)
        }

        view.findViewById<Button>(R.id.btn_multiplayer).setOnClickListener{
            val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            
            if (bluetoothAdapter == null) {
                Toast.makeText(requireContext(), "To urządzenie nie obsługuje Bluetooth", Toast.LENGTH_SHORT).show()
            } else if (!bluetoothAdapter.isEnabled) {
                // Wyświetla okno systemowe proszące o włączenie Bluetooth
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetoothLauncher.launch(enableBtIntent)
            } else {
                findNavController().navigate(R.id.action_menuFragment_to_lobbyFragment)
            }
        }

        view.findViewById<Button>(R.id.btn_ranking).setOnClickListener{
            findNavController().navigate(R.id.action_menuFragment_to_rankingFragment)
        }

        view.findViewById<Button>(R.id.btn_settings).setOnClickListener{
            findNavController().navigate(R.id.action_menuFragment_to_settingsFragment)
        }
    }

}