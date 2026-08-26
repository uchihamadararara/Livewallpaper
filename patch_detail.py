import re

with open('app/src/main/java/com/example/ui/wallpapers/WallpaperDetailScreen.kt', 'r') as f:
    content = f.read()

# I want to replace everything from `    Scaffold(` to the end of the file.
new_ui = """    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.3f), androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleFavorite() },
                        modifier = Modifier
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.3f), androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(
                            imageVector = if (wp.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (wp.isFavorite) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = wp.imageUrl ?: wp.thumbnailUrl,
                contentDescription = wp.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isLive = wp.type == "LIVE" || wp.type == "ADVANCED_LIVE"
                    if (isLive) {
                        Box(modifier = Modifier.background(Color.White.copy(alpha=0.2f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                    if (wp.isPremium) {
                        Box(modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha=0.2f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Star, contentDescription = "Premium", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                Text("PREMIUM", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = wp.title,
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-1).sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                if (!wp.description.isNullOrEmpty()) {
                    Text(
                        text = wp.description,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { viewModel.onApplyClicked() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = if (wp.isPremium) MaterialTheme.colorScheme.primary else Color.White),
                    enabled = applyState !is ApplyState.Applying
                ) {
                    if (applyState is ApplyState.Applying) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.background, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = if (wp.isPremium) "Apply Premium" else "Apply Wallpaper",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (wp.isPremium) MaterialTheme.colorScheme.onPrimary else Color.Black
                        )
                    }
                }
            }
        }
    }
}
"""

content = re.sub(r'    Scaffold\([\s\S]*$', new_ui, content)

with open('app/src/main/java/com/example/ui/wallpapers/WallpaperDetailScreen.kt', 'w') as f:
    f.write(content)
