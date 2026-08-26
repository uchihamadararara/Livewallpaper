package com.example.ui.premium

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.di.AppContainer
import com.example.di.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: PremiumViewModel = viewModel(
        factory = ViewModelFactory(
            wallpaperRepository = AppContainer.getWallpaperRepository(context),
            userRepository = AppContainer.userRepository,
            authUserId = kotlinx.coroutines.runBlocking { com.example.di.AppContainer.authRepositoryImpl.getUserId() },
            billingRepository = AppContainer.getBillingRepository(context)
        )
    )

    val products by viewModel.subscriptionProducts.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val isActive = userProfile?.subscriptionStatus == "ACTIVE"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Premium Subscriptions") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isActive) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("You are a Premium Member!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Your subscription is active.", fontSize = 14.sp)
                        }
                    }
                }
            } else {
                Text(
                    text = "Upgrade to Premium",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Unlock all exclusive wallpapers, advanced live animations, and remove all ads.",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            if (products == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (products!!.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Premium plans are not configured yet.\nPlease check back later.",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(products!!) { product ->
                        val offerDetails = product.subscriptionOfferDetails?.firstOrNull()
                        val pricingPhase = offerDetails?.pricingPhases?.pricingPhaseList?.firstOrNull()
                        
                        val price = pricingPhase?.formattedPrice ?: "N/A"
                        val period = pricingPhase?.billingPeriod ?: ""
                        
                        // Parse billing period roughly (P1M -> Monthly, P1Y -> Yearly, P1W -> Weekly)
                        val periodText = when (period) {
                            "P1M" -> "Monthly"
                            "P1Y" -> "Yearly"
                            "P1W" -> "Weekly"
                            "P3D" -> "3 Days"
                            else -> period
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!isActive) {
                                        viewModel.subscribe(context as Activity, product)
                                    }
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = product.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text(text = product.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = price, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                                    Text(text = periodText, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
