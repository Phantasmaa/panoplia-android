package com.phantasmaa.panoplia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phantasmaa.panoplia.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val session: SessionManager
) : ViewModel() {

    private val _hasSession = MutableStateFlow<Boolean?>(null)
    val hasSession: StateFlow<Boolean?> = _hasSession

    init {
        viewModelScope.launch {
            _hasSession.value = session.user.first() != null
        }
    }
}

@Composable
fun SplashScreen(
    onLoggedIn: () -> Unit,
    onLoggedOut: () -> Unit,
    vm: SplashViewModel = hiltViewModel()
) {
    LaunchedEffect(vm.hasSession.value) {
        when (vm.hasSession.value) {
            true -> onLoggedIn()
            false -> onLoggedOut()
            null -> {} // still loading
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Panoplia", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
    }
}
