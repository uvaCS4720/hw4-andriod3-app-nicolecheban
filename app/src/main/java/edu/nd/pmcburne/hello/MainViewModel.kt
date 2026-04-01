package edu.nd.pmcburne.hello

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CampusMapsUiState(
    val selectedTag: String = "All",
    val tags: List<String> = listOf("All", "Academic", "Housing", "Dining", "Athletics"),
    val isDropdownExpanded: Boolean = false
)

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CampusMapsUiState())
    val uiState: StateFlow<CampusMapsUiState> = _uiState.asStateFlow()

    fun onTagSelected(tag: String) {
        _uiState.update { it.copy(selectedTag = tag, isDropdownExpanded = false) }
    }

    fun onDropdownExpandedChange(expanded: Boolean) {
        _uiState.update { it.copy(isDropdownExpanded = expanded) }
    }
}