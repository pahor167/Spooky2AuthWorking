package com.spooky2.huntkill.ui.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spooky2.huntkill.data.GeneratorSessionFactory
import com.spooky2.huntkill.data.SessionHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Connect/Auth screen state: runs [GeneratorSessionFactory.connect] (demo by default). */
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
) : ViewModel() {

    private val _state = MutableStateFlow(ConnectUiState())
    val state: StateFlow<ConnectUiState> = _state.asStateFlow()

    fun connect() {
        if (_state.value.status == ConnectStatus.Connecting) return
        _state.value = ConnectUiState(status = ConnectStatus.Connecting)

        viewModelScope.launch {
            runCatching { sessionFactory.connect() }
                .onSuccess { session ->
                    sessionHolder.set(session)
                    _state.value = ConnectUiState(
                        status = ConnectStatus.Connected,
                        generatorType = session.generatorType,
                        baudRate = session.baudRate,
                        authToken = session.authToken,
                    )
                }
                .onFailure { error ->
                    _state.value = ConnectUiState(
                        status = ConnectStatus.Error,
                        errorMessage = error.message ?: "Connection failed",
                    )
                }
        }
    }
}
