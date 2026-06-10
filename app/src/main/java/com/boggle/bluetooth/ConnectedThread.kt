package com.boggle.bluetooth

import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class ConnectedThread(
    private val socket: BluetoothSocket,
    private val callback: BluetoothCallback
) : Thread() {

    private val inputStream: InputStream? = socket.inputStream
    private val outputStream: OutputStream? = socket.outputStream
    private var isRunning = true

    override fun run() {
        callback.onConnectionSuccess()

        val reader = java.io.BufferedReader(java.io.InputStreamReader(inputStream))

        // Pętla stale nasłuchująca wiadomości
        while (isRunning) {
            try {
                val incomingMessage = reader.readLine() ?: break
                
                // Przekazujemy wiadomość
                callback.onMessageReceived(incomingMessage)
            } catch (e: IOException) {
                Log.e("Bluetooth", "Utracono połączenie", e)
                callback.onDisconnected()
                break
            }
        }
        cancel()
    }

    // Funkcja do wysyłania
    fun write(message: String) {
        try {
            val msgWithNewLine = if (message.endsWith("\n")) message else message + "\n"
            outputStream?.write(msgWithNewLine.toByteArray())
            outputStream?.flush()
        } catch (e: IOException) {
            Log.e("Bluetooth", "Błąd podczas wysyłania danych", e)
        }
    }

    fun cancel() {
        isRunning = false
        try {
            socket.close()
        } catch (e: IOException) {
            Log.e("Bluetooth", "Nie można zamknąć gniazda", e)
        }
    }
}