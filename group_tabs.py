from pathlib import Path
from textwrap import indent

lt = chr(60)
gt = chr(62)

path = Path( pdfutility/src/main/resources/ui/PdfUtilityLayout.fxml)
data = path.read_text()
center_start = data.index(  + lt +  center + gt)
center_end = data.index(  + lt +  /center + gt, center_start)
old_center = data[center_start:center_end + len(  + lt +  /center + gt)]
tabs_open = lt +  tabs + gt
tabs_close = lt +  /tabs + gt
tabs_start = old_center.index(tabs_open)
tabs_end = old_center.index(tabs_close, tabs_start)
tabs_block = old_center[tabs_start + len(tabs_open):tabs_end]
segments = []
pos = 0
length = len(tabs_block)

while pos < length:
    idx = tabs_block.find(lt +  Tab, pos)
    if idx == -1:
        break
    depth = 0
    i = idx
    while i < length:
        if tabs_block.startswith(lt +  Tab, i):
            depth += 1
            i += 4
        elif tabs_block.startswith(lt +  /Tab, i):
tab_open = lt +  Tab
tab_close = lt +  /Tab + gt
            depth -= 1
            i += len(tab_close)
            if depth == 0:
                segment = tabs_block[idx:i]
                segments.append(segment)
                pos = i
                break
        else:
            i += 1
    else:
        break

categories = {
categories = {
    'PDF': ['mergeTab','optimizeTab','risoTab','blankPagesTab','conditionalBlankTab','repeatTab','pageFilterTab','pairMergeTab','markerSplitTab','folderStampTab','keywordStampTab','pdfToWordTab'],
    'CSV': ['csvRenameTab','csvTxtMergeTab'],
    'Excel': ['csvToExcelTab'],
}
id_to_segment = {}

for segment in segments:
for segment in segments:
