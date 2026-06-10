package com.spooky2.huntkill.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-scoped holder for the single active [GeneratorSession].
 *
 * Connect/Auth establishes the session here; Hunt/Live/Kill read it back. A
 * [Singleton] holder is the simplest correct way to share one open transport across
 * the navigation-scoped ViewModels without re-running the handshake on every screen.
 */
@Singleton
class SessionHolder @Inject constructor() {
    private val _session = MutableStateFlow<GeneratorSession?>(null)
    val session: StateFlow<GeneratorSession?> = _session.asStateFlow()

    fun set(session: GeneratorSession) {
        _session.value = session
    }

    /** Swap in a new session, closing the previous one (its transport) first. */
    suspend fun replace(session: GeneratorSession) {
        val previous = _session.value
        _session.value = session
        if (previous != null && previous !== session) {
            runCatching { previous.close() }
        }
    }

    fun current(): GeneratorSession? = _session.value

    suspend fun clear() {
        _session.value?.close()
        _session.value = null
    }
}
