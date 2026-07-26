package com.phantasmaa.panoplia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phantasmaa.panoplia.data.local.SessionManager
import com.phantasmaa.panoplia.data.model.User
import com.phantasmaa.panoplia.data.repo.PanopliaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginState {
    data object Idle : LoginState
    data object Loading : LoginState
    data object Success : LoginState
    data class Error(val msg: String) : LoginState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repo: PanopliaRepository,
    private val session: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _state.value = LoginState.Error("Completá usuario y contraseña")
            return
        }
        _state.value = LoginState.Loading
        viewModelScope.launch {
            try {
                val resp = repo.login(username.trim(), password)
                if (resp.ok && resp.user != null) {
                    session.save(resp.user)
                    _state.value = LoginState.Success
                } else {
                    _state.value = LoginState.Error(resp.error ?: "Credenciales inválidas")
                }
            } catch (e: Exception) {
                _state.value = LoginState.Error("Error de red: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }
}

@Composable
fun LoginScreen(
    onSuccess: () -> Unit,
    vm: LoginViewModel = hiltViewModel()
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(vm.state.value) {
        if (vm.state.value is LoginState.Success) onSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Panoplia", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Text("Iniciá sesión", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it; vm.state.value.let { _ -> } },
            label = { Text("Usuario") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { vm.login(username, password) },
            enabled = vm.state.value !is LoginState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (vm.state.value is LoginState.Loading) "Conectando..." else "Entrar")
        }

        val s = vm.state.value
        if (s is LoginState.Error) {
            Spacer(Modifier.height(12.dp))
            Text(s.msg, color = MaterialTheme.colorScheme.error)
        }
    }
}
