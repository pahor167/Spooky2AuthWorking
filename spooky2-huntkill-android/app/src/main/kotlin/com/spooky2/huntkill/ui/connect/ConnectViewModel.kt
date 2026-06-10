package com.spooky2.huntkill.ui.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spooky2.huntkill.data.GeneratorSession
import com.spooky2.huntkill.data.GeneratorSessionFactory
import com.spooky2.huntkill.data.SessionHolder
import com.spooky2.huntkill.data.UsbConnectionManager
import com.spooky2.huntkill.log.LogBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Connect/Auth screen state: runs the demo [GeneratorSessionFactory] or real USB connect. */
data class ConnectUiState(
    val status: ConnectStatus = ConnectStatus.Idle,
    val generatorType: String? = null,
    val baudRate: Int? = null,
    val authToken: String? = null,
    val errorMessage: String? = null,
) {
    val isConnected: Boolean get() = status == ConnectStatus.Connected
}

enum class ConnectStatus { Idle, Connecting, Connected, Error }

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val sessionFactory: GeneratorSessionFactory,
    private val sessionHolder: SessionHolder,
    private val usbConnectionManager: UsbConnectionManager,
    private val log: LogBus,
) : ViewModel() {

    private val _state = MutableStateFlow(ConnectUiState())
    val state: StateFlow<ConnectUiState> = _state.asStateFlow()

    /** Demo connect: replays the bundled dump; a fresh replay is rebuilt per hunt. */
    fun connect() {
        if (_state.value.status == ConnectStatus.Connecting) return
        _state.value = ConnectUiState(status = ConnectStatus.Connecting)
        log.i(TAG, "Demo connect attempt")

        viewModelScope.launch {
            runCatching { sessionFactory.connect() }
                .onSuccess { session ->
                    sessionHolder.set(session)
                    // Demo replay is single-use: rebuild a fresh session each hunt.
                    sessionHolder.setReconnect { sessionFactory.connect() }
                    logConnected("Demo", session)
                    _state.value = connectedState(session)
                }
                .onFailure { error -> fail("Demo", error) }
        }
    }

    /** Real USB connect: enumerate, request permission, probe + auth; reuse per hunt. */
    fun connectUsb() {
        if (_state.value.status == ConnectStatus.Connecting) return
        _state.value = ConnectUiState(status = ConnectStatus.Connecting)
        log.i(TAG, "USB connect attempt")

        viewModelScope.launch {
            runCatching { usbConnectionManager.connect() }
                .onSuccess { session ->
                    sessionHolder.set(session)
                    // Live USB session is reused across hunts — do NOT reconnect.
                    sessionHolder.setReconnect(null)
                    logConnected("USB", session)
                    _state.value = connectedState(session)
                }
                .onFailure { error -> fail("USB", error) }
        }
    }

    private fun logConnected(path: String, session: GeneratorSession) {
        log.i(
            TAG,
            "$path connected: type=${session.generatorType} baud=${session.baudRate} " +
                "auth=${if (session.authToken != null) "present" else "none"}",
        )
    }

    private fun connectedState(session: GeneratorSession) = ConnectUiState(
        status = ConnectStatus.Connected,
        generatorType = session.generatorType,
        baudRate = session.baudRate,
        authToken = session.authToken,
    )

    private fun fail(path: String, error: Throwable) {
        val message = error.message ?: "Connection failed"
        log.e(TAG, "$path connect failed: $message")
        _state.value = ConnectUiState(status = ConnectStatus.Error, errorMessage = message)
    }

    companion object {
        private const val TAG = "Connect"
    }
}
