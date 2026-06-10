package com.boggle.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boggle.viewmodel.GameViewModel
import kotlinx.coroutines.launch
import android.view.LayoutInflater
import android.view.ViewGroup
import com.boggle.R

@SuppressLint("MissingPermission") // Zakładamy, że uprawnienia zebraliśmy w MainActivity
class LobbyFragment : Fragment(R.layout.fragment_lobby) {

    private val gameViewModel: GameViewModel by activityViewModels()
    private lateinit var deviceAdapter: DeviceAdapter

    // radar
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Złapaliśmy sygnał
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.let { deviceAdapter.addDevice(it) }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnHost = view.findViewById<Button>(R.id.btn_host_game)
        val btnJoin = view.findViewById<Button>(R.id.btn_join_game)
        val btnBack = view.findViewById<Button>(R.id.btn_lobby_back)
        val tvStatus = view.findViewById<TextView>(R.id.tv_bluetooth_status)
        val progressBar = view.findViewById<ProgressBar>(R.id.progress_bluetooth)
        val recyclerDevices = view.findViewById<RecyclerView>(R.id.recycler_devices)
        val tvDevicesLabel = view.findViewById<TextView>(R.id.tv_devices_label)

        // Konfiguracja listy urządzeń
        deviceAdapter = DeviceAdapter { clickedDevice ->
            // Gdy gracz kliknie w urządzenie z listy
            gameViewModel.bluetoothAdapter.cancelDiscovery()
            gameViewModel.boggleBluetoothManager.startAsClient(clickedDevice)
            tvStatus.text = "Łączenie z ${clickedDevice.name}..."
            progressBar.visibility = View.VISIBLE
        }
        recyclerDevices.layoutManager = LinearLayoutManager(requireContext())
        recyclerDevices.adapter = deviceAdapter

        // Nasłuchiwanie zmian statusu
        viewLifecycleOwner.lifecycleScope.launch {
            gameViewModel.gameStartEvent.collect {
                findNavController().navigate(R.id.action_lobbyFragment_to_gameFragment)
            }
        }

        // Obsługa przycisku Załóż grę
        btnHost.setOnClickListener {
            gameViewModel.isHost = true
            gameViewModel.boggleBluetoothManager.startAsHost()

            // Włączamy widoczność naszego telefonu dla innych na 300 sekund (5 minut)
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
            startActivity(discoverableIntent)

            tvStatus.text = "Czekam na przeciwnika..."
            progressBar.visibility = View.VISIBLE
            recyclerDevices.visibility = View.GONE
            tvDevicesLabel.visibility = View.GONE
        }

        // Obsługa przycisku Szukaj hosta
        btnJoin.setOnClickListener {
            gameViewModel.isHost = false
            tvStatus.text = "Szukam Hosta..."
            progressBar.visibility = View.VISIBLE
            recyclerDevices.visibility = View.VISIBLE
            tvDevicesLabel.visibility = View.VISIBLE

            // Uruchamiamy radar
            if (gameViewModel.bluetoothAdapter.isDiscovering) {
                gameViewModel.bluetoothAdapter.cancelDiscovery()
            }
            gameViewModel.bluetoothAdapter.startDiscovery()
        }

        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Rejestrujemy radar
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        requireActivity().registerReceiver(receiver, filter)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Odpinamy radar po wyjściu z ekranu
        requireActivity().unregisterReceiver(receiver)

        // Zatrzymujemy skanowanie
        @SuppressLint("MissingPermission")
        if (gameViewModel.bluetoothAdapter.isDiscovering) {
            gameViewModel.bluetoothAdapter.cancelDiscovery()
        }
    }
}

class DeviceAdapter(
    private val onDeviceClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    private val devices = mutableListOf<BluetoothDevice>()

    @SuppressLint("MissingPermission")
    fun addDevice(device: BluetoothDevice) {
        // Dodajemy tylko urządzenia, które mają nazwę i których jeszcze nie ma na liście
        if (device.name != null && !devices.contains(device)) {
            devices.add(device)
            notifyItemInserted(devices.size - 1)
        }
    }

    class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text1: TextView = view.findViewById(android.R.id.text1)
        val text2: TextView = view.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return DeviceViewHolder(view)
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.text1.text = device.name
        holder.text2.text = device.address
        holder.itemView.setOnClickListener { onDeviceClick(device) }
    }

    override fun getItemCount() = devices.size
}