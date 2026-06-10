package com.boggle.bluetooth

import java.util.UUID

object BluetoothConfig {
    // Unikalny identyfikator dla aplikacji
    val BOGGLE_UUID: UUID = UUID.fromString("7b9a50c2-12a4-4927-b23c-8346766cf175")
    const val APP_NAME = "BoggleMultiplayer"
}

interface BluetoothCallback {
    fun onConnectionSuccess()
    fun onConnectionFailed(error: String)
    fun onMessageReceived(message: String)
    fun onDisconnected()
}