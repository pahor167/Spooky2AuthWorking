package com.spooky2.huntkill.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spooky2.huntkill.core.lookup.LookupMatch
import com.spooky2.huntkill.core.lookup.ReverseLookup
import com.spooky2.huntkill.core.lookup.ReverseLookupParameters
import com.spooky2.huntkill.data.FrequencyDatabaseSource
import com.spooky2.huntkill.data.RunRecord
import com.spooky2.huntkill.data.RunHistoryRepository
import com.spooky2.huntkill.ui.hunt.DEFAULT_LOOKUP_TOLERANCE_PERCENT
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** State for the run-history list + detail screens. */
data class HistoryUiState(
    val isLoading: Boolean = true,
    val runs: List<RunRecord> = emptyList(),
    /** The run shown on the detail screen, resolved by id; null until loaded / not found. */
    val selected: RunRecord? = null,
    /** Reverse-lookup matches per frequency for the selected run (empty list = no matches). */
    val lookupResults: Map<Double, List<LookupMatch>> = emptyMap(),
    val lookupBusy: Boolean = false,
)

/**
 * Backs the History list and detail screens. Reads persisted runs from
 * [RunHistoryRepository] (newest first) and, for the detail view, runs the existing
 * reverse lookup per saved frequency off the UI thread.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: RunHistoryRepository,
    // Optional so the detail screen still renders frequencies when the DB is unavailable.
    private val frequencyDatabase: FrequencyDatabaseSource? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    /** (Re)load the full run list, newest first. */
    fun refresh() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val runs = runCatching { repository.all() }.getOrDefault(emptyList())
            _state.update { it.copy(isLoading = false, runs = runs) }
        }
    }

    /** Delete a saved run, then refresh the list. */
    fun delete(id: String) {
        viewModelScope.launch {
            runCatching { repository.delete(id) }
            refresh()
        }
    }

    /** Load a single run by id for the detail screen and kick off reverse lookup. */
    fun loadDetail(id: String) {
        viewModelScope.launch {
            val record = runCatching { repository.get(id) }.getOrNull()
            _state.update { it.copy(selected = record, lookupResults = emptyMap()) }
            record?.let { runReverseLookup(it) }
        }
    }

    private fun runReverseLookup(record: RunRecord) {
        val database = frequencyDatabase ?: return
        if (record.hits.isEmpty()) return
        _state.update { it.copy(lookupBusy = true) }
        viewModelScope.launch(Dispatchers.Default) {
            val db = runCatching { database.database() }.getOrElse {
                _state.update { s -> s.copy(lookupBusy = false) }
                return@launch
            }
            val params = ReverseLookupParameters(tolerancePercent = DEFAULT_LOOKUP_TOLERANCE_PERCENT)
            val results = withContext(Dispatchers.Default) {
                record.hits.associate { hit ->
                    hit.frequency to ReverseLookup.lookup(hit.frequency, db.entries, params)
                }
            }
            _state.update { it.copy(lookupResults = results, lookupBusy = false) }
        }
    }
}
