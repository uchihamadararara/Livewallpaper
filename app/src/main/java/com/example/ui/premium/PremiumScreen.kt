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
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.di.AppContainer
import com.example.di.ViewModelFactory
import com.example.ui.theme.ChampagnePrimary

@Composable
fun PremiumScreen(navController: NavController) {
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

    var selectedPlanIndex by remember { mutableStateOf(1) } // Default Yearly

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
                text = "VIP STUDIO",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF60A5FA),
                letterSpacing = 1.sp
            )

            TextButton(
                onClick = {
                    Toast.makeText(context, "Checking existing Google Play purchases...", Toast.LENGTH_SHORT).show()
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // HERO CROWN BOX
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
                        text = "Unlimited Ultra-HD video artworks, interactive charging engines, sound fx & zero interruptions.",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }

            // PERKS LIST
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
                    "Access to all exclusive wallpapers",
                    "Dynamic Live Wallpapers with Audio FX",
                    "Interactive Charging Animations Unlocked",
                    "Ad-Free Pure High-Speed Experience",
                    "New Artist Collections Added Weekly"
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

            // PLAN SELECTION CARDS
            Text(
                text = "CHOOSE YOUR VIP PASS",
                color = Color(0xFF60A5FA),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            val plans = listOf(
                Triple("Lifetime VIP Pass", "$19.99", "One-time purchase • Forever Access"),
                Triple("1 Year VIP", "$9.99 / yr", "Just $0.83/mo • Save 60%"),
                Triple("1 Month VIP", "$2.49 / mo", "Standard Monthly Pass • Cancel Anytime")
            )

            plans.forEachIndexed { index, (title, price, subtitle) ->
                val isSelected = selectedPlanIndex == index
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
                        .clickable { selectedPlanIndex = index }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedPlanIndex = index },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF38BDF8),
                                    unselectedColor = Color(0xFF475569)
                                )
                            )
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    if (index == 0) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF2563EB))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("BEST VALUE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                        }
                                    } else if (index == 1) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF0284C7))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("POPULAR", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                                Text(
                                    text = subtitle,
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Text(
                            text = price,
                            color = if (isSelected) Color(0xFF60A5FA) else Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // PRIMARY CTA ACTION BUTTON
            Button(
                onClick = {
                    if (products != null && products!!.isNotEmpty()) {
                        val product = products!!.firstOrNull()
                        if (product != null) {
                            viewModel.subscribe(context as Activity, product)
                        }
                    } else {
                        Toast.makeText(context, "VIP pass activated! Enjoy your live wallpapers.", Toast.LENGTH_LONG).show()
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
                            text = if (isActive) "YOU ARE VIP" else "START VIP ACCESS",
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
