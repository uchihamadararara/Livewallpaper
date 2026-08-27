package com.example.ui.premium

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.data.billing.BillingManager
import com.example.di.AppContainer
import com.example.di.ViewModelFactory

data class SubscriptionTier(
    val id: String,
    val aliasIds: List<String>,
    val title: String,
    val fallbackPrice: String,
    val subtitle: String,
    val badge: String? = null
)

@Composable
fun SubscriptionScreen(
    navController: NavController,
    billingManager: BillingManager? = null
) {
    val context = LocalContext.current
    val viewModel: PremiumViewModel = viewModel(
        factory = ViewModelFactory(
            wallpaperRepository = AppContainer.getWallpaperRepository(context),
            userRepository = AppContainer.userRepository,
            authUserId = kotlinx.coroutines.runBlocking { AppContainer.authRepositoryImpl.getUserId() },
            billingRepository = AppContainer.getBillingRepository(context)
        )
    )

    val products by viewModel.subscriptionProducts.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isActive = userProfile?.subscriptionStatus == "ACTIVE"

    // 5 requested tiers: 3 days, 7 days, 14 days, 1 month, and lifetime
    val tiers = remember {
        listOf(
            SubscriptionTier(
                id = "vip_lifetime",
                aliasIds = listOf("vip_lifetime", "premium_lifetime"),
                title = "Lifetime VIP Pass",
                fallbackPrice = "$14.99",
                subtitle = "One-time purchase • Forever Access",
                badge = "BEST VALUE"
            ),
            SubscriptionTier(
                id = "vip_1month",
                aliasIds = listOf("vip_1month", "premium_monthly"),
                title = "1 Month VIP Pass",
                fallbackPrice = "$4.99 / mo",
                subtitle = "Standard Monthly Pass • Cancel Anytime"
            ),
            SubscriptionTier(
                id = "vip_14days",
                aliasIds = listOf("vip_14days", "premium_14_days"),
                title = "14 Days VIP Pass",
                fallbackPrice = "$3.49",
                subtitle = "2 Weeks Unrestricted Access",
                badge = "POPULAR"
            ),
            SubscriptionTier(
                id = "vip_7days",
                aliasIds = listOf("vip_7days", "premium_7_days"),
                title = "7 Days VIP Pass",
                fallbackPrice = "$1.99",
                subtitle = "Full Week Access • All VIP Wallpapers"
            ),
            SubscriptionTier(
                id = "vip_3days",
                aliasIds = listOf("vip_3days", "premium_3_days"),
                title = "3 Days VIP Pass",
                fallbackPrice = "$0.99",
                subtitle = "Quick 3-Day Pass • Ultra-HD & FX"
            )
        )
    }

    var selectedTierIndex by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080B10))
    ) {
        // TOP APP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF101622))
                    .border(1.dp, Color(0xFF1E2A3C), CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = "VIP SUBSCRIPTION",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF60A5FA),
                letterSpacing = 1.sp
            )

            TextButton(
                onClick = {
                    Toast.makeText(context, "Checking Google Play purchases...", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text("Restore", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // SCROLLABLE CONTENT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // HERO BANNER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0F1E36), Color(0xFF090E17))
                        )
                    )
                    .border(1.5.dp, Color(0xFF1E3A8A), RoundedCornerShape(24.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "VIP Crown",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Text(
                        text = if (isActive) "VIP Pass Active" else "Unlock All Live Wallpapers",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Unlimited Ultra-HD artworks, dynamic charging loops, audio FX & clean ad-free experience.",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }

            // PERKS CONTAINER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0E1522))
                    .border(1.dp, Color(0xFF1E2A3C), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(
                    "All exclusive live & 4K wallpapers unlocked",
                    "Full dynamic charging loop transitions",
                    "Studio audio FX on active wallpapers",
                    "Ad-free pure high-speed experience",
                    "Direct artist updates & fresh drops"
                ).forEach { perk ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = perk,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // TIERS LIST HEADER
            Text(
                text = "AVAILABLE SUBSCRIPTION TIERS",
                color = Color(0xFF60A5FA),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            // 5 SUBSCRIPTION TIERS CARDS
            tiers.forEachIndexed { index, tier ->
                val isSelected = selectedTierIndex == index
                
                // Fetch dynamic price from BillingManager/ViewModel with fallback
                val dynamicPrice = if (billingManager != null) {
                    val prod = billingManager.getProductDetailsForPlan(tier.aliasIds)
                    billingManager.getFormattedPrice(prod) ?: tier.fallbackPrice
                } else {
                    viewModel.getFormattedPriceForPlan(tier.aliasIds, tier.fallbackPrice)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) Brush.horizontalGradient(
                                listOf(Color(0xFF0F2347), Color(0xFF0B172E))
                            ) else Brush.horizontalGradient(
                                listOf(Color(0xFF0E1522), Color(0xFF0E1522))
                            )
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF1E2A3C),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedTierIndex = index }
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedTierIndex = index },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF38BDF8),
                                unselectedColor = Color(0xFF475569)
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        // Title and subtitle with weight constraint ensuring stable width
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = tier.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1
                                )
                                if (tier.badge != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (tier.badge == "BEST VALUE") Color(0xFF2563EB) else Color(0xFF0284C7)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = tier.badge,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tier.subtitle,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }

                        // Formatted localized price - softWrap=false ensures consistent single-line alignment
                        Text(
                            text = dynamicPrice,
                            color = if (isSelected) Color(0xFF60A5FA) else Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            // PRIMARY CTA BUTTON
            val selectedTier = tiers.getOrElse(selectedTierIndex) { tiers.first() }
            val selectedProduct = if (billingManager != null) {
                billingManager.getProductDetailsForPlan(selectedTier.aliasIds)
            } else {
                viewModel.getProductForPlan(selectedTier.aliasIds)
            }

            Button(
                onClick = {
                    if (selectedProduct != null) {
                        viewModel.subscribe(context as Activity, selectedProduct)
                    } else if (products != null && products!!.isNotEmpty()) {
                        viewModel.subscribe(context as Activity, products!!.first())
                    } else {
                        Toast.makeText(
                            context,
                            "Google Play Billing is connecting... Please ensure your device has Google Play Store and internet connection.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF2563EB), Color(0xFF1D4ED8), Color(0xFF0284C7))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isActive) "YOU ARE VIP" else "START ${selectedTier.title.uppercase()}",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Text(
                text = "Secured by Google Play Billing • Cancel anytime in Subscriptions",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
