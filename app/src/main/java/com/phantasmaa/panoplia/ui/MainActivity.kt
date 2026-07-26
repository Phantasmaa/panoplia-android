package com.phantasmaa.panoplia.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.phantasmaa.panoplia.ui.screens.HomeScreen
import com.phantasmaa.panoplia.ui.screens.ImageEnhancerScreen
import com.phantasmaa.panoplia.ui.screens.LoginScreen
import com.phantasmaa.panoplia.ui.screens.SplashScreen
import com.phantasmaa.panoplia.ui.theme.PanopliaTheme
import dagger.hilt.android.AndroidEntryPoint

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val HOME = "home"
    const val IMAGE_ENHANCER = "image-enhancer"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PanopliaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PanopliaNavHost()
                }
            }
        }
    }
}

@Composable
private fun PanopliaNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onLoggedIn = {
                    nav.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onLoggedOut = {
                    nav.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(onSuccess = {
                nav.navigate(Routes.HOME) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            })
        }
        composable(Routes.HOME) {
            HomeScreen(
                onOpenImageEnhancer = { nav.navigate(Routes.IMAGE_ENHANCER) }
            )
        }
        composable(Routes.IMAGE_ENHANCER) {
            ImageEnhancerScreen(onBack = { nav.popBackStack() })
        }
    }
}
