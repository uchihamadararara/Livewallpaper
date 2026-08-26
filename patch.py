with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace('imageVector = Icons.Default.Home, // simplify for compilation', 'imageVector = screen.icon,')

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
