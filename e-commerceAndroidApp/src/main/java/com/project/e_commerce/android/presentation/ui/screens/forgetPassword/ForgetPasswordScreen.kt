package com.project_e_commerce.android.presentation.ui.screens.forgetPassword

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import org.koin.androidx.compose.koinViewModel
import com.project.e_commerce.android.R
import com.project_e_commerce.android.presentation.ui.screens.createAccountScreen.InputFieldWithLabel
import com.project.e_commerce.android.presentation.viewModel.AuthEffect
import com.project.e_commerce.android.presentation.viewModel.AuthViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ForgetPasswordRequestScreen(navController: NavController) {
    val vm: AuthViewModel = koinViewModel()
    val s by vm.state.collectAsState()
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }

    // الاستماع للـ Effects بدون أي تغيير بصري
    LaunchedEffect(Unit) {
        vm.effect.collectLatest { eff ->
            when (eff) {
                is AuthEffect.NavigateToLogin -> {
                    navController.navigate(Screens.LoginScreen.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                is AuthEffect.Toast -> Toast.makeText(context, eff.message, Toast.LENGTH_SHORT).show()
                else -> {}
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Forgot Password?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF114B7F),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, bottom = 32.dp),
            textAlign = TextAlign.Center
        )
        Image(
            painter = painterResource(id = R.drawable.group),
            contentDescription = "Forgot Password Icon",
            modifier = Modifier
                .size(150.dp)
                .background(Color(0xFFFFE0E0), CircleShape)
                .padding(top = 22.dp, bottom = 16.dp)
                .padding(bottom = 16.dp)
        )
        Text(
            text = "Don't worry! It occurs. Please enter the email address linked with your account.",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 24.dp),
            textAlign = TextAlign.Center
        )

        // ✅ لوجيك بس: نخلي الـ VM يعرف الإيميل كمان (لا تغيير بصري)
        InputFieldWithLabel(
            "Email",
            email,
            onValueChange = {
                email = it
                runCatching { vm.onEmailChanged(it) }
            },
            placeholder = "Enter your email"
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val trimmed = email.trim()
                if (trimmed.isNotBlank()) vm.sendResetEmail(trimmed)
            },
            enabled = !s.loading && email.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF6F00)),
            shape = RoundedCornerShape(10.dp),
            elevation = ButtonDefaults.elevation(8.dp)
        ) {
            Text(
                text = if (s.loading) "Please wait..." else "Send Message",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
private fun ForgetPasswordRequestScreenPreview() {
    ForgetPasswordRequestScreen(navController = rememberNavController())
}
