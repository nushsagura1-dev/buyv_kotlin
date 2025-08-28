package com.project.e_commerce.android.presentation.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon as Material3Icon
import androidx.compose.material3.Text as Material3Text
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign

@Composable
fun AppBottomBar(
    titles: List<String>,
    icons: List<Int>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    profileBadgeCount: Int = 0,
    productsBadgeCount: Int = 0,
    showFab: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color.White)
                .drawBehind {
                    val shadowHeight = 12.dp.toPx()
                    drawRoundRect(
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            28.dp.toPx(), 28.dp.toPx()
                        ),
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x11000000),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = shadowHeight
                        ),
                        size = size
                    )
                }
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {
                            onTabSelected(0)
                        }
                ) {
                    Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.size(28.dp)) {
                        if (icons.getOrNull(0) != null) {
                            Material3Icon(
                                painter = painterResource(id = icons.getOrNull(0)!!),
                                contentDescription = null,
                                tint = if (selectedTab == 0) Color(0xFF176DBA) else Color(0xD8000000),
                                modifier = Modifier
                                    .size(26.dp)
                                    .padding(top = 4.dp)
                            )
                        }
                        // Profile badge
                        if (0 == 3 && profileBadgeCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .offset(x = 8.dp, y = (-2).dp)
                                    .background(Color.Red, shape = CircleShape)
                            )
                        }
                        // Products badge
                        if (0 == 1 && productsBadgeCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .offset(x = 8.dp, y = (-2).dp)
                                    .background(Color.Red, shape = CircleShape)
                            )
                        }
                    }
                    Material3Text(
                        text = titles.getOrNull(0) ?: "",
                        fontSize = 12.sp,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 0) Color(0xFF176DBA) else Color(0xD8000000),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {
                            onTabSelected(1)
                        }
                ) {
                    Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.size(28.dp)) {
                        if (icons.getOrNull(1) != null) {
                            Material3Icon(
                                painter = painterResource(id = icons.getOrNull(1)!!),
                                contentDescription = null,
                                tint = if (selectedTab == 1) Color(0xFF176DBA) else Color(0xD8000000),
                                modifier = Modifier
                                    .size(26.dp)
                                    .padding(top = 4.dp)
                            )
                        }
                        // Profile badge
                        if (1 == 3 && profileBadgeCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .offset(x = 8.dp, y = (-2).dp)
                                    .background(Color.Red, shape = CircleShape)
                            )
                        }
                        // Products badge
                        if (1 == 1 && productsBadgeCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .offset(x = 8.dp, y = (-2).dp)
                                    .background(Color.Red, shape = CircleShape)
                            )
                        }
                    }
                    Material3Text(
                        text = titles.getOrNull(1) ?: "",
                        fontSize = 12.sp,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 1) Color(0xFF176DBA) else Color(0xD8000000),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {
                            onTabSelected(2)
                        }
                ) {
                    Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.size(28.dp)) {
                        if (icons.getOrNull(2) != null) {
                            Material3Icon(
                                painter = painterResource(id = icons.getOrNull(2)!!),
                                contentDescription = null,
                                tint = if (selectedTab == 2) Color(0xFF176DBA) else Color(0xD8000000),
                                modifier = Modifier
                                    .size(26.dp)
                                    .padding(top = 4.dp)
                            )
                        }
                        // Profile badge
                        if (2 == 3 && profileBadgeCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .offset(x = 8.dp, y = (-2).dp)
                                    .background(Color.Red, shape = CircleShape)
                            )
                        }
                        // Products badge
                        if (2 == 1 && productsBadgeCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .offset(x = 8.dp, y = (-2).dp)
                                    .background(Color.Red, shape = CircleShape)
                            )
                        }
                    }
                    Material3Text(
                        text = titles.getOrNull(2) ?: "",
                        fontSize = 12.sp,
                        fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 2) Color(0xFF176DBA) else Color(0xD8000000),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {
                            onTabSelected(3)
                        }
                ) {
                    Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.size(28.dp)) {
                        if (icons.getOrNull(3) != null) {
                            Material3Icon(
                                painter = painterResource(id = icons.getOrNull(3)!!),
                                contentDescription = null,
                                tint = if (selectedTab == 3) Color(0xFF176DBA) else Color(0xD8000000),
                                modifier = Modifier
                                    .size(26.dp)
                                    .padding(top = 4.dp)
                            )
                        }
                        // Profile badge
                        if (3 == 3 && profileBadgeCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .offset(x = 8.dp, y = (-2).dp)
                                    .background(Color.Red, shape = CircleShape)
                            )
                        }
                        // Products badge
                        if (3 == 1 && productsBadgeCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .offset(x = 8.dp, y = (-2).dp)
                                    .background(Color.Red, shape = CircleShape)
                            )
                        }
                    }
                    Material3Text(
                        text = titles.getOrNull(3) ?: "",
                        fontSize = 12.sp,
                        fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 3) Color(0xFF176DBA) else Color(0xD8000000),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
