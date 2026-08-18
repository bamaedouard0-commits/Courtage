package com.ouagadousoft.filerescuelibre.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RootState {
    Checking,
    Granted,
    Denied,
}

class RootViewModel : ViewModel() {

    private val _rootState = MutableStateFlow(RootState.Checking)
    val rootState: StateFlow<RootState> = _rootState

    init {
        checkRoot()
    }

    private fun checkRoot() {
        viewModelScope.launch {
            val hasRoot = withContext(Dispatchers.IO) {
                Shell.getShell().isRoot
            }
            _rootState.value = if (hasRoot) RootState.Granted else RootState.Denied
        }
    }
}
