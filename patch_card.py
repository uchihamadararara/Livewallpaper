with open('app/src/main/java/com/example/ui/components/WallpaperCard.kt', 'r') as f:
    content = f.read()

content = content.replace('.premiumClickable { onClick() }', '.premiumClickable(onClick = onClick)')

with open('app/src/main/java/com/example/ui/components/WallpaperCard.kt', 'w') as f:
    f.write(content)
