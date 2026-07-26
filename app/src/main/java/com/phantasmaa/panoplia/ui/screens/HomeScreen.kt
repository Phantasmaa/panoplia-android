package com.phantasmaa.panoplia.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phantasmaa.panoplia.data.local.SessionManager
import com.phantasmaa.panoplia.data.model.ServiceInfo
import com.phantasmaa.panoplia.data.repo.PanopliaRepository
import com.phantasmaa.panoplia.ui.theme.PanopliaColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: PanopliaRepository,
    private val session: SessionManager
) : ViewModel() {

    val services: List<ServiceInfo> = repo.services()

    val user: StateFlow<com.phantasmaa.panoplia.data.model.User?> =
        session.user.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun logout() {
        viewModelScope.launch { session.clear() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenImageEnhancer: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val user by vm.user.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Panoplia", fontWeight = FontWeight.SemiBold)
                        if (user != null) {
                            Text(
                                "Hola, ${user!!.username}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { vm.logout() }) {
                        Icon(Icons.Filled.Logout, contentDescription = "Cerrar sesión")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PanopliaColors.BgSurface
                )
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(vm.services) { svc ->
                ServiceCard(
                    svc = svc,
                    onClick = {
                        if (svc.native) {
                            onOpenImageEnhancer()
                        } else {
                            // For now: open in browser via the public tunnel.
                            val base = com.phantasmaa.panoplia.BuildConfig.API_BASE_URL
                            val full = base.trimEnd('/') + svc.url
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(full))
                            try {
                                ctx.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ServiceCard(svc: ServiceInfo, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = PanopliaColors.BgSurface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .padding(0.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(svc.icon, fontSize = 24.sp)
            }
            Column {
                Text(
                    svc.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PanopliaColors.TextPrimary
                )
                Text(
                    svc.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = PanopliaColors.TextSecondary,
                    maxLines = 2
                )
            }
        }
    }
}
