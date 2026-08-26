import re

with open('app/src/main/java/com/example/ui/premium/PremiumScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")', 'Icon(Icons.Default.ArrowBack, contentDescription = "Back")')

with open('app/src/main/java/com/example/ui/premium/PremiumScreen.kt', 'w') as f:
    f.write(content)
