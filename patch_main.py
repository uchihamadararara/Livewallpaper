with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace(
"""                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.tertiary,
                            selectedIconColor = MaterialTheme.colorScheme.onTertiary,
                            unselectedIconColor = MaterialTheme.colorScheme.secondary,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedTextColor = MaterialTheme.colorScheme.secondary
                        ),""",
"""                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),"""
)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
