with open('app/src/main/java/com/example/ui/settings/SettingsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('RoundedCornerShape(12.dp)', 'MaterialTheme.shapes.medium')
content = content.replace('import androidx.compose.foundation.shape.RoundedCornerShape\n', '')

with open('app/src/main/java/com/example/ui/settings/SettingsScreen.kt', 'w') as f:
    f.write(content)
