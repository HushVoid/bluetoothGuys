package com.example.bluetoothguys.bluetooth

import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class BluetoothPeer(
    private val socket: BluetoothSocket,
    private val scope: CoroutineScope,
    private val onIncomingLine: (String) -> Unit,
    private val onClosed: (Throwable?) -> Unit,
) {
    private val writer: BufferedWriter =
        BufferedWriter(OutputStreamWriter(socket.outputStream, StandardCharsets.UTF_8))
    private val reader: BufferedReader =
        BufferedReader(InputStreamReader(socket.inputStream, StandardCharsets.UTF_8))

    private val outgoing = Channel<String>(capacity = Channel.BUFFERED)
    private val job = Job()

    fun start() {
        scope.launch(Dispatchers.IO + job) {
            try {
                while (true) {
                    val line = reader.readLine() ?: break
                    onIncomingLine(line)
                }
                onClosed(null)
            } catch (e: CancellationException) {
                // Normal shutdown path (e.g. user navigated away / reconnect).
                onClosed(null)
                return@launch
            } catch (t: Throwable) {
                onClosed(t)
            } finally {
                closeQuietly()
            }
        }

        scope.launch(Dispatchers.IO + job) {
            try {
                for (line in outgoing) {
                    writer.write(line)
                    writer.newLine()
                    writer.flush()
                }
            } catch (e: CancellationException) {
                // Normal shutdown path.
                return@launch
            } catch (t: Throwable) {
                onClosed(t)
            } finally {
                closeQuietly()
            }
        }
    }

    suspend fun send(line: String) {
        withContext(Dispatchers.IO) {
            outgoing.send(line)
        }
    }

    fun close() {
        job.cancel()
        outgoing.close()
        closeQuietly()
    }

    private fun closeQuietly() {
        try {
            socket.close()
        } catch (_: Throwable) {
        }
    }
}

