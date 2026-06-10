package com.spooky2.huntkill.ui.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spooky2.huntkill.data.GeneratorSession
import com.spooky2.huntkill.data.GeneratorSessionFactory
import com.spooky2.huntkill.data.SessionHolder
import com.spooky2.huntkill.data.UsbConnectionManager
import com.spooky2.huntkill.data.UsbGeneratorPort
import com.spooky2.huntkill.log.LogBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One selectable USB generator port, identified by a stable [id] (deviceName + port).
 * A dual-generator device shows up as two options with the same [deviceName].
 */
data class UsbGeneratorOption(
    val id: String,
    val deviceName: String,
    val portIndex: Int,
    val label: String,
)

/** Connect/Auth screen state: runs the demo [GeneratorSessionFactory] or real USB connect. */
data class ConnectUiState(
    val status: ConnectStatus = ConnectStatus.Idle,
    val generatorType: String? = null,
    val baudRate: Int? = null,
    val authToken: String? = null,
    val errorMessage: String? = null,
    val usbDevices: List<UsbGeneratorOption> = emptyList(),
    val selectedId: String? = null,
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

    /** Live generator-port handles keyed by their stable id, refreshed by [refreshUsbDevices]. */
    private var portCache: Map<String, UsbGeneratorPort> = emptyMap()

    init {
        refreshUsbDevices()
    }

    /** Re-enumerate attached USB generator ports; default the selection to the first. */
    fun refreshUsbDevices() {
        val ports = usbConnectionManager.listGenerators()
        val options = ports.map { port -> port.toOption() }
        portCache = ports.associateBy { it.id() }
        _state.update { current ->
            val selected = current.selectedId
                ?.takeIf { id -> options.any { it.id == id } }
                ?: options.firstOrNull()?.id
            current.copy(usbDevices = options, selectedId = selected)
        }
    }

    /** Pick which attached generator port a USB connect will target. */
    fun selectUsbDevice(id: String) {
        _state.update { it.copy(selectedId = id) }
    }

    private fun UsbGeneratorPort.id(): String = "${device.deviceName}#$portIndex"

    private fun UsbGeneratorPort.toOption(): UsbGeneratorOption =
        UsbGeneratorOption(
            id = id(),
            deviceName = device.deviceName,
            portIndex = portIndex,
            label = label,
        )

    /** Demo connect: replays the bundled dump; a fresh replay is rebuilt per hunt. */
    fun connect() {
        if (_state.value.status == ConnectStatus.Connecting) return
        _state.update { it.copy(status = ConnectStatus.Connecting, errorMessage = null) }
        log.i(TAG, "Demo connect attempt")

        viewModelScope.launch {
            runCatching { sessionFactory.connect() }
                .onSuccess { session ->
                    sessionHolder.set(session)
                    // Demo replay is single-use: rebuild a fresh session each hunt.
                    sessionHolder.setReconnect { sessionFactory.connect() }
                    logConnected("Demo", session)
                    setConnected(session)
                }
                .onFailure { error -> fail("Demo", error) }
        }
    }

    /** Real USB connect: connect to the selected generator, probe + auth; reuse per hunt. */
    fun connectUsb() {
        if (_state.value.status == ConnectStatus.Connecting) return

        val selectedId = _state.value.selectedId
        val port = selectedId?.let { portCache[it] }
        if (port == null) {
            log.w(TAG, "USB connect blocked: no generator selected/attached")
            _state.update {
                it.copy(
                    status = ConnectStatus.Error,
                    errorMessage = "No USB generator attached. Tap Refresh after connecting one.",
                )
            }
            return
        }

        _state.update { it.copy(status = ConnectStatus.Connecting, errorMessage = null) }
        log.i(TAG, "USB connect attempt on ${port.device.deviceName} port ${port.portIndex}")

        viewModelScope.launch {
            runCatching { usbConnectionManager.connect(port.device, port.portIndex) }
                .onSuccess { session ->
                    sessionHolder.set(session)
                    // Live USB session is reused across hunts — do NOT reconnect.
                    sessionHolder.setReconnect(null)
                    logConnected("USB", session)
                    setConnected(session)
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
