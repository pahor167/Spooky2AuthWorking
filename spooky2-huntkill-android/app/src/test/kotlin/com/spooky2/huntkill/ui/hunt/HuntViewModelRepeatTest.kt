package com.spooky2.huntkill.ui.hunt

import com.spooky2.huntkill.data.SessionHolder
import com.spooky2.huntkill.log.LogBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verifies the kill-phase repeat toggle on [HuntViewModel]: it defaults to ON and
 * [HuntViewModel.toggleRepeatKill] flips the published state. The engine-level looping is
 * covered in core by KillRepeatTest; here we only assert the VM's state mirror.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HuntViewModelRepeatTest {

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
    fun `repeatKill defaults to true`() {
        val viewModel = newViewModel()
        assertTrue(viewModel.state.value.repeatKill)
    }

    @Test
    fun `toggleRepeatKill flips the state`() {
        val viewModel = newViewModel()
        assertTrue(viewModel.state.value.repeatKill)

        viewModel.toggleRepeatKill()
        assertFalse(viewModel.state.value.repeatKill)

        viewModel.toggleRepeatKill()
        assertTrue(viewModel.state.value.repeatKill)
    }
}
