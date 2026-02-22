package com.project.e_commerce.android.presentation.ui.screens.reelsScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.e_commerce.android.presentation.ui.screens.HeaderStyle

/**
 * Header du Reels avec onglets Explore/Following/For you
 * 
 * Composant extrait de ReelsView.kt pour améliorer la maintenabilité.
 */
@Composable
fun ReelsTopHeader(
    onClickSearch: () -> Unit,
    selectedTab: String,
    onTabChange: (String) -> Unit,
    onClickExplore: () -> Unit,
    headerStyle: HeaderStyle,
    modifier: Modifier = Modifier
) {
    val underlineColor = if (selectedTab == "Explore") Color(0xFF0066CC) else Color.White

    // Completely transparent header with only text buttons visible
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            HeaderTab(
                text = "Explore",
                isSelected = selectedTab == "Explore",
                onClick = {
                    onTabChange("Explore")
                    onClickExplore()
                },
                textColor = Color.White,
                underlineColor = if (selectedTab == "Explore") Color(0xFF0066CC) else Color.White
            )
            HeaderTab(
                text = "Following",
                isSelected = selectedTab == "Following",
                onClick = { onTabChange("Following") },
                textColor = Color.White,
                underlineColor = underlineColor
            )
            HeaderTab(
                text = "For you",
                isSelected = selectedTab == "For you",
                onClick = { onTabChange("For you") },
                textColor = Color.White,
                underlineColor = underlineColor
            )
        }
        IconButton(
            onClick = onClickSearch,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = if (selectedTab == "Explore") Color(0xFF0066CC) else Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

/**
 * Onglet individuel du header
 */
@Composable
fun HeaderTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    textColor: Color,
    underlineColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 16.sp,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.7f),
                    offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                    blurRadius = 4f
                )
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(3.dp)
                    .shadow(3.dp, RoundedCornerShape(2.dp))
                    .background(underlineColor, RoundedCornerShape(2.dp))
            )
        } else {
            Spacer(modifier = Modifier.height(3.dp))
        }
    }
}
