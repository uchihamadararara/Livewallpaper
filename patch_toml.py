import re

with open('gradle/libs.versions.toml', 'r') as f:
    content = f.read()

# Remove firebase versions
content = re.sub(r'firebaseBom\s*=\s*".*?"\n', '', content)
content = re.sub(r'googleServices\s*=\s*".*?"\n', '', content)
content = re.sub(r'crashlyticsPlugin\s*=\s*".*?"\n', '', content)

# Remove firebase libraries
content = re.sub(r'firebase-[^\n]+\n', '', content)

# Remove firebase plugins
content = re.sub(r'google-services\s*=\s*\{[^\n]+\n', '', content)
content = re.sub(r'firebase-crashlytics\s*=\s*\{[^\n]+\n', '', content)

with open('gradle/libs.versions.toml', 'w') as f:
    f.write(content)
