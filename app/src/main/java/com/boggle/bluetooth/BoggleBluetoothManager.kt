package com.boggle.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket

class BoggleBluetoothManager(
    private val bluetoothAdapter: BluetoothAdapter,
    private val callback: BluetoothCallback
) {
    private var acceptThread: AcceptThread? = null
    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null

    // Odpala Host
    fun startAsHost() {
        stopAllThreads()
        acceptThread = AcceptThread(bluetoothAdapter, callback) { socket ->
            manageConnectedSocket(socket)
        }
        acceptThread?.start()
    }

    // Odpala Klient
    fun startAsClient(device: BluetoothDevice) {
        stopAllThreads()
        connectThread = ConnectThread(device, bluetoothAdapter, callback) { socket ->
            manageConnectedSocket(socket)
        }
        connectThread?.start()
    }

    private fun manageConnectedSocket(socket: BluetoothSocket) {
        connectedThread = ConnectedThread(socket, callback)
        connectedThread?.start()
    }

    fun sendMessage(message: String) {
        connectedThread?.write(message)
    }

    fun stopAllThreads() {
        acceptThread?.cancel()
        acceptThread = null
        connectThread?.cancel()
        connectThread = null
        connectedThread?.cancel()
        connectedThread = null
    }
}