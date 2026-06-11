package com.spooky2.huntkill.ui.hunt

import com.spooky2.huntkill.data.SessionHolder
import com.spooky2.huntkill.log.LogBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Guard-level tests for the kill-phase "Treat now" forwarding. The end-to-end jump
 * (engine actually driving the generator to the new index) is covered in core by
 * KillControlJumpTest; here we only assert the VM's [HuntViewModel.jumpToHit] guards:
 * it is a no-op outside the Killing phase and for out-of-range indices, so a stray UI
 * tap can never crash or mutate state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HuntViewModelJumpTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(): HuntViewModel = HuntViewModel(SessionHolder(), LogBus())

    @Test
    fun `jumpToHit outside the kill phase is a no-op`() {
        val viewModel = newViewModel()
        // Idle phase by default: requesting a jump must not change state or throw.
        val before = viewModel.state.value
        viewModel.jumpToHit(0)
        assertEquals(before, viewModel.state.value)
    }

    @Test
    fun `jumpToHit with an out-of-range index is a no-op`() {
        val viewModel = newViewModel()
        val before = viewModel.state.value
        viewModel.jumpToHit(42)
        assertEquals(before, viewModel.state.value)
    }
}
