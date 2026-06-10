package com.boggle.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.IOException

@SuppressLint("MissingPermission")
class ConnectThread(
    private val device: BluetoothDevice,
    private val bluetoothAdapter: BluetoothAdapter,
    private val callback: BluetoothCallback,
    private val onSocketReady: (BluetoothSocket) -> Unit
) : Thread() {

    private val socket: BluetoothSocket? by lazy {
        device.createRfcommSocketToServiceRecord(BluetoothConfig.BOGGLE_UUID)
    }

    override fun run() {
        bluetoothAdapter.cancelDiscovery()

        try {
            // Próba połączenia do Hosta
            socket?.connect()
            socket?.let { onSocketReady(it) }
        } catch (e: IOException) {
            try { socket?.close() } catch (e2: IOException) {}
            callback.onConnectionFailed("Nie udało się połączyć z Hostem.")
        }
    }

    fun cancel() {
        try { socket?.close() } catch (e: IOException) {}
    }
}