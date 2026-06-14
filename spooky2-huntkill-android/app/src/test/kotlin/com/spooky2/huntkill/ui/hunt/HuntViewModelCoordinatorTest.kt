package com.spooky2.huntkill.ui.hunt

import com.spooky2.huntkill.core.scan.ScanEngine
import com.spooky2.huntkill.data.GeneratorSession
import com.spooky2.huntkill.data.SessionHolder
import com.spooky2.huntkill.data.UsbPortInfo
import com.spooky2.huntkill.log.LogBus
import com.spooky2.huntkill.transport.GeneratorClient
import com.spooky2.huntkill.transport.SerialTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verifies the multi-generator coordinator behaviour of [HuntViewModel]:
 *  (a) two registry sessions yield two controllers and two tabs,
 *  (b) [HuntViewModel.setActiveGenerator] flips the observed state to that controller's
 *      own snapshot instantly, with no session I/O, and
 *  (c) hunt mutual-exclusion: the acquiring controller's onAcquireHuntSlot hook pauses
 *      any OTHER controller that is currently Hunting.
 *
 * State is driven directly per-controller (via reflection on each controller's own
 * `_state`) so the run engine doesn't need to be exercised — the coordinator wiring is
 * the unit under test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HuntViewModelCoordinatorTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Minimal always-open transport; never asked to read in these wiring tests. */
    private class StubTransport : SerialTransport {
        override val isOpen: Boolean = true
        override suspend fun open(baudRate: Int) = Unit
        override suspend fun close() = Unit
        override suspend fun write(bytes: ByteArray) = Unit
        override suspend fun readLine(timeoutMs: Long): String? {
            delay(Long.MAX_VALUE)
            return null
        }
    }

    private fun buildSession(portIndex: Int): GeneratorSession {
        val transport = StubTransport()
        val client = GeneratorClient(transport = transport)
        return GeneratorSession(
            baudRate = GeneratorClient.BAUD_GENERATORX,
            generatorType = GeneratorClient.GENERATOR_TYPE_GENERATORX,
            authToken = null,
            transport = transport,
            client = client,
            engine = ScanEngine(client),
            usbPort = UsbPortInfo(index = portIndex, count = 2),
            isDemo = true,
        )
    }

    /** Reflect on ONE controller's private `_state` so we can drive it without the engine. */
    @Suppress("UNCHECKED_CAST")
    private fun controllerState(controller: GeneratorRunController): MutableStateFlow<HuntUiState> {
        val field = controller.javaClass.getDeclaredField("_state")
        field.isAccessible = true
        return field.get(controller) as MutableStateFlow<HuntUiState>
    }

    /** Reflect the coordinator's controller list. */
    @Suppress("UNCHECKED_CAST")
    private fun controllers(viewModel: HuntViewModel): List<GeneratorRunController> {
        val field = viewModel.javaClass.getDeclaredField("_controllers")
        field.isAccessible = true
        val flow = field.get(viewModel) as MutableStateFlow<List<GeneratorRunController>>
        return flow.value
    }

    private fun twoSessionViewModel(): HuntViewModel {
        val holder = SessionHolder()
        holder.put(0, buildSession(portIndex = 0))
        holder.put(1, buildSession(portIndex = 1))
        return HuntViewModel(holder, LogBus())
    }

    @Test
    fun `two sessions yield two controllers and two tabs`() {
        val viewModel = twoSessionViewModel()

        assertEquals("one controller per connected session", 2, controllers(viewModel).size)
        assertEquals("one tab per controller", 2, viewModel.tabs.value.size)
        assertEquals(0, viewModel.tabs.value[0].index)
        assertEquals(1, viewModel.tabs.value[1].index)
    }

    @Test
    fun `setActiveGenerator flips observed state to that controller's snapshot with no IO`() {
        val viewModel = twoSessionViewModel()
        val list = controllers(viewModel)

        // Give each controller a distinct snapshot so the active flip is observable.
        controllerState(list[0]).value = controllerState(list[0]).value.copy(phase = HuntPhase.Hunting)
        controllerState(list[1]).value = controllerState(list[1]).value.copy(phase = HuntPhase.Killing)

        // Active is 0 by default → Hunting from controller 0.
        assertEquals(HuntPhase.Hunting, viewModel.state.value.phase)

        // Pure index flip → controller 1's own snapshot, no session I/O.
        viewModel.setActiveGenerator(1)
        assertEquals(1, viewModel.activeIndex.value)
        assertEquals(HuntPhase.Killing, viewModel.state.value.phase)
    }

    @Test
    fun `starting a hunt pauses the other hunting generator`() = runBlocking {
        val viewModel = twoSessionViewModel()
        val list = controllers(viewModel)

        // Controller 1 is actively hunting; controller 0 will acquire the hunt slot.
        controllerState(list[1]).value = controllerState(list[1]).value.copy(phase = HuntPhase.Hunting, isPaused = false)
        assertFalse("precondition: controller 1 not paused", controllerState(list[1]).value.isPaused)

        // Invoke the coordinator's mutual-exclusion exactly as controller 0's
        // onAcquireHuntSlot hook does (the suspend hook guarantees happens-before).
        // pauseOtherHunts only calls the non-suspending pauseForExclusion(), so it
        // completes synchronously: method.invoke returns Unit (never COROUTINE_SUSPENDED).
        val method = viewModel.javaClass.getDeclaredMethod(
            "pauseOtherHunts",
            Int::class.java,
            kotlin.coroutines.Continuation::class.java,
        )
        method.isAccessible = true
        val noopContinuation = object : kotlin.coroutines.Continuation<Any?> {
            override val context = kotlin.coroutines.EmptyCoroutineContext
            override fun resumeWith(result: Result<Any?>) = Unit
        }
        method.invoke(viewModel, 0, noopContinuation)

        assertTrue(
            "the other hunting generator must be paused for mutual-exclusion",
            controllerState(list[1]).value.isPaused,
        )
    }
}
