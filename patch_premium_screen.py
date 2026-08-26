import re

with open('app/src/main/java/com/example/ui/premium/PremiumScreen.kt', 'r') as f:
    content = f.read()

# Replace Icon(Icons.Default.ArrowBack with AutoMirrored version
content = content.replace('Icon(Icons.Default.ArrowBack, contentDescription = "Back")', 'Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")')

# Replace the products empty checking logic
old_ui = """            if (products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(products) { product ->"""

new_ui = """            if (products == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (products!!.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Premium plans are not configured yet.\\nPlease check back later.",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(products!!) { product ->"""

content = content.replace(old_ui, new_ui)

with open('app/src/main/java/com/example/ui/premium/PremiumScreen.kt', 'w') as f:
    f.write(content)
