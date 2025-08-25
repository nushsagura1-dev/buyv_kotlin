package com.project.e_commerce.android.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.project.e_commerce.android.R
import com.project.e_commerce.android.presentation.viewModel.editProfileViewModel.EditProfileViewModel
import org.koin.androidx.compose.koinViewModel
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun EditProfileScreen(navController: NavHostController) {
    val editProfileViewModel: EditProfileViewModel = koinViewModel()
    val uiState by editProfileViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            editProfileViewModel.updateProfileImage(it.toString())
        }
    }

    // Success dialog
    if (uiState.isSuccess) {
        AlertDialog(
            onDismissRequest = { 
                editProfileViewModel.resetSuccessState()
                navController.popBackStack()
            },
            title = { Text("Success!") },
            text = { Text("Your profile has been updated successfully.") },
            confirmButton = {
                TextButton(
                    onClick = { 
                        editProfileViewModel.resetSuccessState()
                        navController.popBackStack()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    // Error dialog
    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { editProfileViewModel.clearError() },
            title = { Text("Error") },
            text = { Text(uiState.error!!) },
            confirmButton = {
                TextButton(onClick = { editProfileViewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-20).dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Back",
                    tint = Color(0xFF0066CC),
                    modifier = Modifier.padding(10.dp)
                )
            }

            Text(
                text = "Edit Profile",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF0066CC),
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Profile Image Section
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .border(3.dp, Color(0xFF0066CC), CircleShape)
                .clickable { imagePickerLauncher.launch("image/*") }
        ) {
            if (uiState.profileImageUrl != null && uiState.profileImageUrl != "") {
                AsyncImage(
                    model = uiState.profileImageUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.profile),
                    contentDescription = "Default Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Camera icon overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp)
                    .background(Color(0xFF0066CC), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Change Photo",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap to change photo",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Form Fields
        CustomOutlinedTextField(
            value = uiState.displayName,
            onValueChange = { editProfileViewModel.updateDisplayName(it) },
            label = "Display Name",
            placeholder = "Enter your display name",
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Color(0xFF0066CC),
                    modifier = Modifier.size(20.dp)
                )
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        CustomOutlinedTextField(
            value = uiState.username,
            onValueChange = { editProfileViewModel.updateUsername(it) },
            label = "Username",
            placeholder = "Enter your username",
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Color(0xFF0066CC),
                    modifier = Modifier.size(20.dp)
                )
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        CustomOutlinedTextField(
            value = uiState.email,
            onValueChange = { /* Email is read-only */ },
            label = "Email",
            placeholder = "Your email address",
            enabled = false
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        CustomOutlinedTextField(
            value = uiState.phone,
            onValueChange = { editProfileViewModel.updatePhone(it) },
            label = "Phone Number",
            placeholder = "Enter your phone number",
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Color(0xFF0066CC),
                    modifier = Modifier.size(20.dp)
                )
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        CustomOutlinedTextField(
            value = uiState.bio,
            onValueChange = { editProfileViewModel.updateBio(it) },
            label = "Bio",
            placeholder = "Tell us about yourself...",
            minLines = 3,
            imeAction = ImeAction.Done,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Color(0xFF0066CC),
                    modifier = Modifier.size(20.dp)
                )
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Save Button
        Button(
            onClick = { 
                focusManager.clearFocus()
                editProfileViewModel.saveProfile()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6F00)),
            shape = RoundedCornerShape(12.dp),
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    "Save Changes", 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun CustomOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        Text(
            label, 
            fontSize = 14.sp, 
            fontWeight = FontWeight.Medium, 
            color = Color(0xFF0066CC)
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF114B7F)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = (minLines * 24).dp),
            shape = RoundedCornerShape(8.dp),
            trailingIcon = trailingIcon,
            enabled = enabled,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1B7ACE),
                unfocusedBorderColor = Color(0xFFB3C1D1),
                cursorColor = Color(0xFF174378),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                disabledTextColor = Color.Gray
            )
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewEditProfileScreen() {
    val navController = rememberNavController()
    EditProfileScreen(navController = navController)
}
