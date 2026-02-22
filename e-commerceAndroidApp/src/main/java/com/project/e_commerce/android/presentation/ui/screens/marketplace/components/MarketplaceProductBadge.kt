package com.project.e_commerce.android.presentation.ui.screens.marketplace.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import com.project.e_commerce.android.data.repository.TrackingRepository

/**
 * Badge affichant le produit Marketplace lié à un Reel
 * Affiche le nom, prix et commission du produit
 * Cliquable pour ouvrir les détails du produit
 * Phase 6: Tracks affiliate clicks automatically
 */
@Composable
fun MarketplaceProductBadge(
    productName: String,
    productPrice: Double,
    commissionRate: Double,
    productId: String,  // NEW: Phase 6 - Required for tracking
    reelId: String,     // NEW: Phase 6 - Required for tracking
    promoterUid: String, // NEW: Phase 6 - Required for tracking
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trackingRepository: TrackingRepository = koinViewModel()
    val scope = rememberCoroutineScope()
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                // Phase 6: Track click before navigation
                scope.launch {
                    try {
                        trackingRepository.trackAffiliateClick(
                            reelId = reelId,
                            productId = productId,
                            promoterUid = promoterUid
                        )
                        Log.d("MarketplaceBadge", "✅ Click tracked for product $productId")
                    } catch (e: Exception) {
                        Log.e("MarketplaceBadge", "❌ Failed to track click: ${e.message}")
                    }
                }
                onClick()
            }
            .border(
                width = 1.5.dp,
                color = Color(0xFFFF9800),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFF9800).copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icône produit
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFF9800)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Infos produit
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = productName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = String.format("$%.2f", productPrice),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            // Badge commission
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF4CAF50)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${String.format("%.1f", commissionRate)}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Version compacte du badge pour overlay sur la vidéo
 */
@Composable
fun CompactMarketplaceBadge(
    commissionRate: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFF9800).copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingBag,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "🎯 ${String.format("%.1f", commissionRate)}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
