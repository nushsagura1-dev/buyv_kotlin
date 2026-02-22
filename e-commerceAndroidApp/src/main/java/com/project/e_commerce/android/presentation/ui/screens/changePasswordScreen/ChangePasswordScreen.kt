package com.project.e_commerce.android.presentation.ui.screens.changePasswordScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.achiver.presentation.ui.composable.spacerComposable.SpacerVerticalSmall
import com.project.e_commerce.android.R
import com.project.e_commerce.android.presentation.ui.composable.spacerComposable.SpacerHorizontalSmall
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import com.project.e_commerce.android.presentation.ui.utail.BlackColor80
import com.project.e_commerce.android.presentation.ui.utail.ErrorPrimaryColor
import com.project.e_commerce.android.presentation.ui.utail.UnitsApplication
import com.project.e_commerce.android.presentation.ui.utail.UnitsApplication.mediumUnit
import com.project.e_commerce.android.presentation.ui.utail.noRippleClickable
import com.project.e_commerce.android.presentation.viewModel.restPasswordViewModel.RestPasswordViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ChangePasswordScreen(navController: NavController) {
    val viewModel = koinViewModel<RestPasswordViewModel>()
    val state by viewModel.state.collectAsState()

    val scrollState = rememberScrollState()

    var password by remember { mutableStateOf("") }
    var rePassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rePasswordVisible by remember { mutableStateOf(false) }

    // ✅ التنقّل يتم بعد نجاح التغيير فعلاً
    LaunchedEffect(state.isSuccessChanged) {
        if (state.isSuccessChanged) {
            navController.navigate(Screens.LoginScreen.PasswordChangedSuccessScreen.route)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = mediumUnit),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_left_arrow),
                contentDescription = null,
                tint = BlackColor80,
                modifier = Modifier.noRippleClickable {
                    viewModel.onClickBackArrowButton(navController)
                }
            )
            SpacerHorizontalSmall()
            Text(
                text = "Change Password",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF114B7F),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        PasswordFieldWithLabel(
            label = "New Password",
            value = password,
            onValueChange = {
                password = it

                runCatching { viewModel.onWriteNewPassword(it) }
            },
            placeholder = "Enter your password",
            passwordVisible = passwordVisible,
            onToggleVisibility = { passwordVisible = !passwordVisible }
        )
        Spacer(modifier = Modifier.height(16.dp))

        SpacerVerticalSmall()
        if (state.newPassword.isError) {
            Text(
                text = state.newPassword.errorMessage,
                color = ErrorPrimaryColor,
                fontWeight = FontWeight.Normal,
                fontSize = UnitsApplication.tinyFontSize
            )
        }

        PasswordFieldWithLabel(
            label = "Re-Password",
            value = rePassword,
            onValueChange = {
                rePassword = it
                // ✅ نحدّث الـ VM للتحقق من التطابق
                runCatching { viewModel.onWriteNewPasswordConfirmed(it) }

            },
            placeholder = "Re-Enter your password",
            passwordVisible = rePasswordVisible,
            onToggleVisibility = { rePasswordVisible = !rePasswordVisible }
        )
        SpacerVerticalSmall()
        if (state.confirmNewPassword.isError) {
            Text(
                text = state.confirmNewPassword.errorMessage,
                color = ErrorPrimaryColor,
                fontWeight = FontWeight.Normal,
                fontSize = UnitsApplication.tinyFontSize
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
        Button(
            onClick = {
                // ✅ نستدعي لوجيك تغيير الباسورد في الـ VM بدلاً من الـ navigate المباشر
                // ملاحظة: لو توقيع الدالة مختلف عندك، غيّر السطرين دول لنفس اسم الدالة الموجودة
                runCatching {
                    viewModel.onWriteNewPassword(password)
                    viewModel.onWriteNewPasswordConfirmed(rePassword)
                    viewModel.onClickChangePassword(navController)
                }
            },
            enabled = !state.isLoading, // Disable button while loading
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF6F00)),
            shape = RoundedCornerShape(10.dp),
            elevation = ButtonDefaults.elevation(8.dp)
        ) {
            if (state.isLoading) {
                androidx.compose.material.CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = "Change Password",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.isError) {
            Text(
                text = state.errorMessage,
                color = ErrorPrimaryColor,
                fontWeight = FontWeight.Normal,
                fontSize = UnitsApplication.tinyFontSize,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** حقل باسورد قابل لإعادة الاستخدام — UI كما هو بدون تغيير */
@Composable
fun PasswordFieldWithLabel(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    passwordVisible: Boolean,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            IconButton(onClick = onToggleVisibility) {
                Icon(imageVector = image, contentDescription = "Toggle password visibility")
            }
        },
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
private fun ChangePasswordScreenPreview() {
    ChangePasswordScreen(navController = rememberNavController())
}
