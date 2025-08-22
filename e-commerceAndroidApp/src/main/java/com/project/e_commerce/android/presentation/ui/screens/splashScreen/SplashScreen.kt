package com.project.e_commerce.android.presentation.ui.screens.splashScreen

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.edit
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.project.e_commerce.android.R
import kotlinx.coroutines.delay

@Composable
    fun SplashScreen(navController: NavController) {
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            delay(3000)
            val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
            prefs.edit { putBoolean("isFirstTime", false) }
            navController.navigate("onboarding1") {
                popUpTo("splash_screen") { inclusive = true }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.splash),
                contentDescription = "Splash Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }


@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    SplashScreen(navController = rememberNavController())
}