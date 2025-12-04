from pathlib import Path
lines = Path('src/main/java/olivieri/alex/PdfUtilityGui.java').read_text().splitlines()
for i in range(820, 900):
    print(f"{i+1:04}: {lines[i]}")
