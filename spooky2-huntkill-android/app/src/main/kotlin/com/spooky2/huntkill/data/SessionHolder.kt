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

    /**
     * Optional per-hunt reconnector. The live USB path leaves this null so the open
     * session is reused across hunts. The no-hardware replay tests set it to rebuild a
     * fresh single-use FakeTransport session each run.
     */
    private var reconnect: (suspend () -> GeneratorSession)? = null

    fun set(session: GeneratorSession) {
        _session.value = session
    }

    fun setReconnect(block: (suspend () -> GeneratorSession)?) {
        reconnect = block
    }

    /**
     * Resolve the session to run a hunt on. If a reconnector is set, build a fresh
     * session and swap it in (test replay reset); otherwise reuse the current one.
     */
    suspend fun acquireForHunt(): GeneratorSession? {
        val block = reconnect
        return if (block != null) {
            val session = block()
            replace(session)
            session
        } else {
            current()
        }
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
