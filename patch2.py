with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace('label = { Text(screen.route) },', 'label = { Text(screen.label) },')

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
