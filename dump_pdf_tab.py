from pathlib import Path
lines = Path('pdfutility/src/main/resources/ui/PdfUtilityLayout.fxml').read_text().splitlines()
start = 0
for i,line in enumerate(lines):
    if '<Tab text= PDF>' in line:
        start = i
        break
end = len(lines)
for j in range(start,len(lines)):
    if '<Tab text=CSV>' in lines[j]:
        end = j
        break
for line in lines[start:end]:
    print(line)
