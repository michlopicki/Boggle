package com.boggle.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.boggle.R
import com.boggle.viewmodel.GameViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val gameViewModel: GameViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Dostęp do pamięci podręcznej z ustawieniami
        val prefs = requireActivity().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)

        val tvTimeLabel = view.findViewById<TextView>(R.id.tv_time_label)
        val sliderTime = view.findViewById<Slider>(R.id.slider_time)
        val switchSound = view.findViewById<MaterialSwitch>(R.id.switch_sound)
        val switchVibration = view.findViewById<MaterialSwitch>(R.id.switch_vibration)
        val btnReset = view.findViewById<Button>(R.id.btn_reset_history)
        val btnBack = view.findViewById<Button>(R.id.btn_settings_back)

        val savedTimeSeconds = prefs.getInt("game_time", 120)
        sliderTime.value = savedTimeSeconds.toFloat()
        updateTimeLabel(tvTimeLabel, savedTimeSeconds)

        switchSound.isChecked = prefs.getBoolean("sound_enabled", true)

        switchVibration.isChecked = prefs.getBoolean("vibration_enabled", true)

        sliderTime.addOnChangeListener { _, value, _ ->
            val seconds = value.toInt()
            updateTimeLabel(tvTimeLabel, seconds)
            prefs.edit().putInt("game_time", seconds).apply()
        }

        switchSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("sound_enabled", isChecked).apply()
        }

        switchVibration.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("vibration_enabled", isChecked).apply()
        }

        btnReset.setOnClickListener {
            // Bezpieczeństwo przede wszystkim - pytamy o potwierdzenie!
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Usunięcie historii")
                .setMessage("Czy na pewno chcesz usunąć całą historię gier?")
                .setPositiveButton("Usuń") { _, _ ->
                    gameViewModel.clearHistory()
                    Toast.makeText(requireContext(), "Historia została wyczyszczona!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Anuluj", null)
                .show()
        }

        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun updateTimeLabel(textView: TextView, totalSeconds: Int) {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        textView.text = String.format("Czas rundy: %d:%02d", minutes, seconds)
    }
}