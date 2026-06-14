package com.spooky2.huntkill.ui.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spooky2.huntkill.data.GeneratorSession
import com.spooky2.huntkill.data.SessionHolder
import com.spooky2.huntkill.data.UsbConnectionManager
import com.spooky2.huntkill.log.LogBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Connect/Auth screen state. The flow is connect-FIRST: the user taps a single
 * "Connect (USB)" button — no port picker. USB connect targets port 0 by default;
 * choosing/switching the generator happens later on the Hunt config screen.
 *
 * [usbAttached] only drives a small "no device attached" hint; it is not a selector.
 */
data class ConnectUiState(
    val status: ConnectStatus = ConnectStatus.Idle,
    val generatorType: String? = null,
    val baudRate: Int? = null,
    val authToken: String? = null,
    val errorMessage: String? = null,
    val usbAttached: Boolean = false,
) {
    val isConnected: Boolean get() = status == ConnectStatus.Connected
}

enum class ConnectStatus { Idle, Connecting, Connected, Error }

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val sessionHolder: SessionHolder,
    private val usbConnectionManager: UsbConnectionManager,
    private val log: LogBus,
) : ViewModel() {

    private val _state = MutableStateFlow(ConnectUiState())
    val state: StateFlow<ConnectUiState> = _state.asStateFlow()

    init {
        refreshUsbDevices()
    }

    /** Re-check whether any supported USB generator is attached (drives the hint only). */
    fun refreshUsbDevices() {
        val attached = usbConnectionManager.findGenerator() != null
        _state.update { it.copy(usbAttached = attached) }
    }

    /**
     * Real USB connect: auto-detect the first supported generator, request permission
     * once, and open ALL available ports simultaneously.
     *
     * Each port becomes its own [GeneratorSession] stored in [SessionHolder] at its
     * port index. The UI status is driven by the primary (port 0) session — the same
     * single-session callers downstream (HuntViewModel, back-compat shims) observe key 0
     * unchanged.
     */
    fun connectUsb() {
        if (_state.value.status == ConnectStatus.Connecting) return

        _state.update { it.copy(status = ConnectStatus.Connecting, errorMessage = null) }
        log.i(TAG, "USB connect attempt (all ports)")

        viewModelScope.launch {
            runCatching { usbConnectionManager.connectAll() }
                .onSuccess { sessions ->
                    sessions.forEach { session ->
                        val portIndex = session.usbPort?.index ?: 0
                        sessionHolder.put(portIndex, session)
                    }
                    // Live USB sessions are reused across hunts — do NOT reconnect.
                    sessionHolder.setReconnect(null)
                    // Drive UI from the primary (port-0) session for back-compat.
                    val primary = sessions.firstOrNull() ?: return@onSuccess
                    logConnected("USB", primary)
                    setConnected(primary)
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

    private fun setConnected(session: GeneratorSession) {
        _state.update {
            it.copy(
                status = ConnectStatus.Connected,
                generatorType = session.generatorType,
                baudRate = session.baudRate,
                authToken = session.authToken,
                errorMessage = null,
            )
        }
    }

    private fun fail(path: String, error: Throwable) {
        val message = error.message ?: "Connection failed"
        log.e(TAG, "$path connect failed: $message")
        _state.update { it.copy(status = ConnectStatus.Error, errorMessage = message) }
    }

    companion object {
        private const val TAG = "Connect"
    }
}
