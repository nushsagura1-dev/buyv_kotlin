package com.project.e_commerce.android.presentation.ui.screens.loginScreen

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import com.project.e_commerce.android.presentation.ui.utail.ErrorPrimaryColor
import com.project.e_commerce.android.presentation.viewModel.AuthEffect
import com.project.e_commerce.android.presentation.viewModel.AuthViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(navController: NavController) {
    val vm: AuthViewModel = koinViewModel()
    val s by vm.state.collectAsState()
    val context = LocalContext.current

    // One-shot effects: Navigation + Toasts (بدون أي تغيير بصري)
    LaunchedEffect(Unit) {
        vm.effect.collectLatest { eff ->
            when (eff) {
                is AuthEffect.NavigateToHome -> {
                    navController.navigate(Screens.ReelsScreen.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                is AuthEffect.NavigateToLogin -> {
                    navController.navigate(Screens.LoginScreen.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                is AuthEffect.Toast ->
                    Toast.makeText(context, eff.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    var passwordVisible by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Login",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF114B7F),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Email
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            Text(
                text = "Email",
                fontSize = 14.sp,
                color = Color(0xFF114B7F),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            OutlinedTextField(
                value = s.email,
                onValueChange = { vm.onEmailChanged(it) },
                placeholder = { Text("Enter your email") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF1B7ACE),
                    unfocusedBorderColor = Color(0xFFB3C1D1),
                    cursorColor = Color(0xFF174378),
                    backgroundColor = Color.White
                )
            )

            s.emailError?.let {
                Text(
                    text = it,
                    color = ErrorPrimaryColor,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Password
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text(
                text = "Password",
                fontSize = 14.sp,
                color = Color(0xFF114B7F),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            OutlinedTextField(
                value = s.password,
                onValueChange = { vm.onPasswordChanged(it) },
                placeholder = { Text("Enter your password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null
                        )
                    }
                },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF1B7ACE),
                    unfocusedBorderColor = Color(0xFFB3C1D1),
                    cursorColor = Color(0xFF174378),
                    backgroundColor = Color.White
                )
            )

            s.passwordError?.let {
                Text(
                    text = it,
                    color = ErrorPrimaryColor,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "Forgot Password?",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF114B7F),
                modifier = Modifier.clickable {
                    // روح لشاشة إدخال الإيميل
                    navController.navigate(Screens.LoginScreen.EnterEmailScreen.route)
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { vm.login() },
            enabled = !s.loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF6F00)),
            shape = RoundedCornerShape(10.dp),
            elevation = ButtonDefaults.elevation(8.dp)
        ) {
            Text(
                text = if (s.loading) "Please wait..." else "Login",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        s.generalError?.let {
            Text(
                text = it,
                color = ErrorPrimaryColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Don't have an account?",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Register Now",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF174378),
                modifier = Modifier.clickable {
                    navController.navigate(Screens.LoginScreen.CreateAccountScreen.route)
                }
            )
        }

        Spacer(modifier = Modifier.height(400.dp))
    }
}

@Composable
fun SocialLoginButton(iconResId: Int) {
    Button(
        onClick = { },
        modifier = Modifier.size(50.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
        shape = RoundedCornerShape(25.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        elevation = ButtonDefaults.elevation(2.dp)
    ) {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
private fun LoginScreenPreview() {
    LoginScreen(navController = rememberNavController())
}
