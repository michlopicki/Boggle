package com.boggle.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import java.io.IOException

@SuppressLint("MissingPermission") // Uprawnienia sprawdzamy w MainActivity
class AcceptThread(
    private val bluetoothAdapter: BluetoothAdapter,
    private val callback: BluetoothCallback,
    private val onSocketReady: (BluetoothSocket) -> Unit
) : Thread() {

    private val serverSocket: BluetoothServerSocket? by lazy {
        bluetoothAdapter.listenUsingRfcommWithServiceRecord(
            BluetoothConfig.APP_NAME,
            BluetoothConfig.BOGGLE_UUID
        )
    }

    override fun run() {
        var shouldLoop = true
        var socket: BluetoothSocket? = null

        while (shouldLoop) {
            try {
                // Ta linia blokuje wątek i czeka, aż klient się połączy
                socket = serverSocket?.accept()
            } catch (e: IOException) {
                callback.onConnectionFailed("Serwer przestał nasłuchiwać: ${e.message}")
                shouldLoop = false
            }

            if (socket != null) {
                // Połączenie zaakceptowane
                onSocketReady(socket)
                serverSocket?.close()
                shouldLoop = false
            }
        }
    }

    fun cancel() {
        try { serverSocket?.close() } catch (e: IOException) {}
    }
}