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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phantasmaa.panoplia.data.local.SessionManager
import com.phantasmaa.panoplia.data.repo.PanopliaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
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
                    // Prefer the human-readable message from the backend over the
                    // machine error code. "bad_credentials" is not what users
                    // want to see — they want "Usuario o contraseña incorrectos".
                    val friendly = resp.message
                        ?: resp.error
                            ?.takeIf { it.isNotBlank() && it != "bad_credentials" }
                        ?: "Usuario o contraseña incorrectos"
                    _state.value = LoginState.Error(friendly)
                }
            } catch (e: HttpException) {
                val httpCode = e.code()
                val msg = when (httpCode) {
                    401 -> "Usuario o contraseña incorrectos"
                    403 -> "No tenés permiso para entrar"
                    in 500..599 -> "El servidor tuvo un problema, probá en un rato"
                    else -> "Error de red (HTTP $httpCode)"
                }
                _state.value = LoginState.Error(msg)
            } catch (e: java.net.UnknownHostException) {
                _state.value = LoginState.Error("Sin conexión a internet")
            } catch (e: java.net.ConnectException) {
                _state.value = LoginState.Error("No se pudo contactar al servidor")
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

    val state by vm.state.collectAsState()

    // Navega solo cuando el state pasa a Success
    LaunchedEffect(state) {
        if (state is LoginState.Success) onSuccess()
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
            onValueChange = { username = it },
            label = { Text("Usuario") },
            singleLine = true,
            enabled = state !is LoginState.Loading,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            singleLine = true,
            enabled = state !is LoginState.Loading,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { vm.login(username, password) },
            enabled = state !is LoginState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state is LoginState.Loading) "Conectando..." else "Entrar")
        }

        if (state is LoginState.Error) {
            Spacer(Modifier.height(12.dp))
            Text(
                (state as LoginState.Error).msg,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

