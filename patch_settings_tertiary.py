with open('app/src/main/java/com/example/ui/settings/SettingsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('MaterialTheme.colorScheme.tertiary', 'MaterialTheme.colorScheme.surfaceVariant')
content = content.replace('MaterialTheme.colorScheme.onTertiary', 'MaterialTheme.colorScheme.onSurfaceVariant')

with open('app/src/main/java/com/example/ui/settings/SettingsScreen.kt', 'w') as f:
    f.write(content)
