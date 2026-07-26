package com.phantasmaa.panoplia.ui.screens

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phantasmaa.panoplia.data.repo.PanopliaRepository
import com.phantasmaa.panoplia.ui.theme.PanopliaColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

sealed interface EnhanceState {
    data object Idle : EnhanceState
    data class Picked(val uri: Uri) : EnhanceState
    data object Uploading : EnhanceState
    data class Done(val bitmap: android.graphics.Bitmap) : EnhanceState
    data class Error(val msg: String) : EnhanceState
}

@HiltViewModel
class ImageEnhancerViewModel @Inject constructor(
    private val repo: PanopliaRepository
) : ViewModel() {

    private val _state = MutableStateFlow<EnhanceState>(EnhanceState.Idle)
    val state: StateFlow<EnhanceState> = _state.asStateFlow()

    fun onPicked(uri: Uri) {
        _state.value = EnhanceState.Picked(uri)
    }

    fun enhance(resolver: ContentResolver, uri: Uri) {
        _state.value = EnhanceState.Uploading
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalStateException("No se pudo leer la imagen")
                }
                val body = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("image", "upload.jpg", body)
                val response = repo.enhanceImage(part)
                val enhancedBytes = withContext(Dispatchers.IO) {
                    response.byteStream().use { it.readBytes() }
                }
                val bmp = android.graphics.BitmapFactory.decodeByteArray(enhancedBytes, 0, enhancedBytes.size)
                    ?: throw IllegalStateException("Respuesta no es una imagen")
                _state.value = EnhanceState.Done(bmp)
            } catch (e: Exception) {
                _state.value = EnhanceState.Error(e.message ?: e.javaClass.simpleName)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEnhancerScreen(
    onBack: () -> Unit,
    vm: ImageEnhancerViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()
    val resolver = ctx.contentResolver

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { vm.onPicked(it) }
    }

    LaunchedEffect(Unit) {
        pickImage.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mejorar imagen", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PanopliaColors.BgSurface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val s = state) {
                EnhanceState.Idle -> {
                    Text("Tocá el botón para elegir una imagen",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(
                        onClick = {
                            pickImage.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Elegir imagen") }
                }
                is EnhanceState.Picked -> {
                    Text("Imagen seleccionada", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(
                        onClick = { vm.enhance(resolver, s.uri) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Mejorar con Real-ESRGAN") }
                    OutlinedButton(
                        onClick = {
                            pickImage.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Elegir otra") }
                }
                EnhanceState.Uploading -> {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Text("Procesando en el servidor...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                is EnhanceState.Done -> {
                    Text("Resultado", fontWeight = FontWeight.SemiBold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        Image(
                            bitmap = s.bitmap.asImageBitmap(),
                            contentDescription = "Imagen mejorada",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            pickImage.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Mejorar otra") }
                }
                is EnhanceState.Error -> {
                    Text("Error: ${s.msg}", color = MaterialTheme.colorScheme.error)
                    Button(
                        onClick = {
                            pickImage.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Reintentar") }
                }
            }
        }
    }
}
