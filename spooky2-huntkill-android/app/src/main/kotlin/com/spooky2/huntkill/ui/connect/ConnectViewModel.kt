package com.spooky2.huntkill.ui.connect

import android.hardware.usb.UsbDevice
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One selectable USB generator, identified by its stable [deviceName]. */
data class UsbGeneratorOption(
    val deviceName: String,
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
    val selectedDeviceName: String? = null,
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

    /** Live device handles keyed by their stable deviceName, refreshed by [refreshUsbDevices]. */
    private var deviceCache: Map<String, UsbDevice> = emptyMap()

    init {
        refreshUsbDevices()
    }

    /** Re-enumerate attached USB generators; default the selection to the first. */
    fun refreshUsbDevices() {
        val devices = usbConnectionManager.listGenerators()
        deviceCache = devices.associateBy { it.deviceName }
        val options = devices.map { device ->
            UsbGeneratorOption(
                deviceName = device.deviceName,
                label = "0x%04X:0x%04X %s".format(device.vendorId, device.productId, device.deviceName),
            )
        }
        _state.update { current ->
            val selected = current.selectedDeviceName
                ?.takeIf { name -> options.any { it.deviceName == name } }
                ?: options.firstOrNull()?.deviceName
            current.copy(usbDevices = options, selectedDeviceName = selected)
        }
    }

    /** Pick which attached generator a USB connect will target. */
    fun selectUsbDevice(deviceName: String) {
        _state.update { it.copy(selectedDeviceName = deviceName) }
    }

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

        val selectedName = _state.value.selectedDeviceName
        val device = selectedName?.let { deviceCache[it] }
        if (device == null) {
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
        log.i(TAG, "USB connect attempt on $selectedName")

        viewModelScope.launch {
            runCatching { usbConnectionManager.connect(device) }
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
