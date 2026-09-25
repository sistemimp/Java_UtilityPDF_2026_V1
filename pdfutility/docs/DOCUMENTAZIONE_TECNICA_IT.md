# PDFUtility 2026 - Documentazione tecnica di sviluppo

## 1. Identita' progetto

- Root progetto Maven: `pdfutility`
- GroupId: `olivieri.alex`
- ArtifactId: `PDFUtility_2026_V1`
- Versione Maven analizzata: `1.9`
- Main class del JAR: `olivieri.alex.Launcher`
- UI attiva: JavaFX
- UI legacy presente: Swing (`olivieri.alex.App`, `PdfUtilityGui`, package `olivieri.alex.tab`)
- Target Java: 21
- JavaFX: 22.0.2 con classifier Windows

## 2. Struttura repository

```text
pdfutility/
  pom.xml
  INSTALLER.md
  WIX_README_IT.md
  docs/
    GUIDA_UTENTE_IT.md
    DOCUMENTAZIONE_TECNICA_IT.md
  src/main/java/olivieri/alex/
    Launcher.java
    App.java
    PdfUtilityGui.java
    fx/
    fx/tab/
    tab/
    util/
    quality/
  src/main/resources/ui/
  src/main/resources/installer/
  src/test/java/
  target/
```

## 3. Dipendenze principali

Dal `pom.xml`:

- iText 9.2.0: creazione, copia, merge, compressione e PDF/A;
- PDFBox 2.0.30: estrazione testo, rendering pagine, conversione PDF in immagini;
- Apache POI 5.4.1: generazione `.xlsx` e `.docx`;
- ZXing 3.5.1: lettura QR Code, Code 39 e ITF;
- JavaFX 22.0.2: interfaccia desktop;
- Log4j 2.23.1: provider logging richiesto da librerie;
- JUnit 4.11: test.

Sono incluse anche `jai-imageio-core` e `jai-imageio-jpeg2000`, utili a PDFBox per formati immagine non base.

## 4. Build e comandi

### Compilazione e test

```powershell
cd pdfutility
mvn clean test
```

### Package JAR

```powershell
cd pdfutility
mvn clean package
```

Output atteso:

```text
target/PDFUtility_2026_V1-1.9.jar
```

### Avvio JavaFX da Maven

```powershell
cd pdfutility
mvn javafx:run
```

### Avvio JAR

```powershell
cd pdfutility
java -jar target\PDFUtility_2026_V1-1.9.jar
```

### Installer Windows

```powershell
cd pdfutility
mvn clean package -Pinstaller jpackage:jpackage
```

Output:

```text
target/installer/PDFUtility_2026_V1-1.9.exe
```

Il profilo `installer` usa `org.panteleyev:jpackage-maven-plugin` e copia i moduli JavaFX in `target/jfx-modules`.

## 5. Entry point e flusso UI

### Entry point attivo

```text
Launcher.main()
  -> Application.launch(PdfUtilityFxApplication.class, args)
  -> PdfUtilityFxApplication.start(Stage)
  -> carica /ui/PdfUtilityLayout.fxml
  -> applica /ui/app.css
  -> PdfUtilityFxLayoutController.setStage(stage)
  -> initializeTabs()
```

`PdfUtilityFxApplication` crea una `Scene` da 900x600 e imposta titolo `Utility PDF`.

### Layout JavaFX

`src/main/resources/ui/PdfUtilityLayout.fxml` definisce:

- barra superiore con titolo versione e pulsante `Apri log audit`;
- `TabPane` principale;
- tab ad alto livello: `PDF`, `CSV`, `Cartelle`, `Rinomina QR`, `Excel`, `Word`.

Il layout iniziale contiene placeholder o versioni storiche di alcuni moduli. `PdfUtilityFxLayoutController` sostituisce il contenuto dei tab con componenti creati dalle classi in `olivieri.alex.fx.tab`.

### Controller UI centrale

`olivieri.alex.fx.PdfUtilityFxController` e' la facciata applicativa per JavaFX:

- valida input testuali;
- risolve percorsi e nomi output;
- invoca i servizi in `util`;
- scrive audit success/failure;
- centralizza suggerimenti di nomi file.

I controller dei singoli tab non dovrebbero duplicare logica PDF: devono chiamare questa facciata.

## 6. Architettura logica

```text
FXML + CSS
  -> Fx*Tab / Fx*ContentController
    -> PdfUtilityFxController
      -> util service classes
        -> iText / PDFBox / POI / ZXing
      -> AuditLogger
```

Separazione attuale:

- `fx/`: bootstrap JavaFX, layout controller, dialog utility;
- `fx/tab/`: controller/contenuti dei tab JavaFX;
- `util/`: servizi di dominio senza dipendenza diretta da JavaFX;
- `quality/`: log audit;
- `tab/` e `App.java`: UI Swing legacy, ancora compilata ma non entry point del JAR.

## 7. Servizi di dominio

### PdfMergeService

Responsabilita':

- merge di tutti i PDF in una cartella;
- merge in blocchi;
- rotazione condizionale delle pagine non A4 portrait;
- adattamento opzionale a foglio A4.

Dettagli:

- elenca solo file regolari `.pdf` nel primo livello;
- ordina alfabeticamente case-insensitive;
- usa `App.writerProperties`;
- per rotazione usa `RotationMode.NONE`, `CLOCKWISE`, `COUNTERCLOCKWISE`;
- per A4 usa tolleranza 2 punti PDF.

### PdfAlternatingMergeService

Responsabilita':

- accodare blocchi alternati da due PDF.

Regole:

- chunk size minimo 1;
- prosegue anche se uno dei due documenti finisce prima.

### PdfRepeater

Responsabilita':

- copiare l'intero documento sorgente N volte nello stesso output.

### PdfPairMerger

Responsabilita':

- unire coppie di PDF con lo stesso nome da due cartelle.

Regole:

- output in cartella `Result` accanto alla prima cartella;
- report mancanti `missing_pairs.txt`;
- opzione `normalizeFR` per aggiungere una pagina bianca se il primo PDF ha pagine dispari.

### PdfPageFilter

Responsabilita':

- rimuovere pagine dispari o pari.

Enum:

```java
Mode.ODD
Mode.EVEN
```

### PdfLastPageRemover

Responsabilita':

- creare copie senza ultima pagina per tutti i PDF in una cartella.

Output:

- `SenzaUltimaPagina`;
- `SenzaUltimaPagina_1`, ecc. se esiste gia'.

### PdfStringPageRemover

Responsabilita':

- rimuovere pagine che contengono una query testuale.

Regole:

- supporta case-sensitive;
- se tutte le pagine verrebbero rimosse, elimina l'output temporaneo e solleva errore.

### PdfBlankPageInserter

Responsabilita':

- inserire una pagina bianca dopo ogni pagina originale, mantenendo dimensioni pagina.

### PdfConditionalBlankPageInserter

Responsabilita':

- inserire pagina bianca dopo pagine che contengono una frase.

Regole:

- supporta case-sensitive;
- puo' limitarsi a pagine dispari.

### PdfMarkerSplitter

Responsabilita':

- dividere un PDF in documenti separati quando una pagina contiene un marker.

Regole:

- usa PDFBox per estrarre testo pagina per pagina;
- il nome file deriva dal testo successivo al marker nella riga;
- sanifica il nome e lo converte in maiuscolo;
- la prima pagina del gruppo deve contenere il marker;
- con append attivo riusa lo stesso documento output per marker duplicati;
- senza append genera suffissi progressivi.

### PdfShipmentSplitter

Responsabilita':

- dividere un PDF in file omogenei per numero di invii.

Regole:

- ogni invio inizia su una pagina contenente il marker;
- la prima pagina deve contenere il marker;
- output: `prefisso_001_invii_1-N.pdf`;
- mantiene dimensioni pagina copiando il range sorgente.

### PdfOptimizer

Responsabilita':

- riscrivere PDF con writer properties condivise;
- ottimizzare cartelle creando `{nome_cartella}_optimized`.

Nota: il commento parla di PDF 2.0, ma le writer properties condivise in `App` e `PdfUtilityFxController` impostano `PdfVersion.PDF_1_7`.

### RisoGl9730Optimizer e RisoComcolorGd9630Optimizer

Responsabilita':

- conversione in PDF/A-3B;
- output intent sRGB;
- metadato catalogo `RecordID` se valorizzato.

Le due classi sono quasi identiche e differiscono principalmente nel nome operativo/log.

### PdfCsvRenamer

Responsabilita':

- rinominare PDF tramite mapping CSV.

Formato:

```text
originale;nuovo
originale,nuovo
```

Regole:

- legge UTF-8;
- ignora righe vuote e commenti `#`;
- accetta nomi con o senza `.pdf`;
- modalita' rebuild: puo' accodare piu' sorgenti allo stesso target e aggiunge timbro tecnico `anchorage->...`;
- modalita' direct move: solo rinomina, senza merge e senza ricostruzione.

Attenzione tecnica:

- in `readCsv`, se una riga contiene sia `;` sia `,`, la seconda condizione puo' sovrascrivere il parsing precedente. Se serve robustezza CSV, conviene sostituire questa logica con un parser CSV esplicito.

### PdfProgressiveRenamer

Responsabilita':

- aggiungere zeri a sinistra al nome base dei PDF.

Regole:

- width minimo 1, massimo gestito lato UI 12;
- non sovrascrive destinazioni esistenti;
- elabora file ordinati alfabeticamente.

### PdfQrRenamer

Responsabilita':

- copiare e rinominare PDF in base a codice letto sulla prima pagina.

Tecnica:

- PDFBox render a 200 DPI;
- ZXing prova QR Code, Code 39, ITF;
- output `codice_nomeoriginale.pdf`;
- PDF non riconosciuti vengono spostati in `output/scarti`.

### CsvToExcelConverter

Responsabilita':

- convertire CSV in `.xlsx`.

Regole:

- UTF-8;
- rimozione BOM;
- delimitatore rilevato tra `;` e `,`;
- supporto virgolette e doppi apici;
- tutte le celle vengono scritte come testo.

### PdfSearchExcelExtractor

Responsabilita':

- estrarre righe PDF contenenti una chiave e salvarle in Excel.

Output:

- colonne fisse `PDF`, `Pagina`, `Chiave`, `Riga completa`, `Valore grezzo`;
- colonne dinamiche `Campo N` ricavate dallo split su `|`.

### CsvTxtMerger

Responsabilita':

- concatenare file `.csv` e `.txt` in UTF-8.

### DuFileMerger

Responsabilita':

- unire file `.DU`.

Regole:

- ordina alfabeticamente;
- file con `_01_` diventa master se presente;
- dal secondo file in poi salta la prima riga;
- impedisce output uguale a uno degli input.

### FolderProgressiveCreator

Responsabilita':

- creare cartelle `prefisso001` ... `prefisso999`.

Regole:

- massimo 999;
- prefisso obbligatorio;
- prefisso senza spazi iniziali/finali;
- caratteri Windows vietati non ammessi.

### PdfFolderStamper

Responsabilita':

- applicare un timbro testuale sulla prima pagina di ogni PDF in cartella.

Output:

- `Stamped`;
- `Stamped_1`, ecc. se esiste gia'.

Tecnica:

- font Helvetica;
- dimensione 1;
- coordinate assolute PDF.

### PdfKeywordStamper

Responsabilita':

- applicare un timbro alle sole pagine che contengono una keyword.

Tecnica:

- estrazione testo con iText `PdfTextExtractor`;
- supporto case-sensitive;
- output sempre con tutte le pagine.

### PdfToWordConverter

Responsabilita':

- convertire PDF in DOCX preservando il layout visivo.

Tecnica:

- render PDFBox a 300 DPI;
- immagini PNG inserite in Apache POI XWPF;
- margini Word a zero;
- orientamento ricavato dalla prima pagina;
- conversione cartella: un DOCX per ogni PDF.

Limite importante:

- il DOCX risultante contiene immagini delle pagine, non testo modificabile.

## 8. Writer properties e gestione memoria

`App.writerProperties` e `PdfUtilityFxController.WRITER_PROPERTIES` impostano:

- PDF 1.7;
- smart mode;
- full compression;
- livello `BEST_COMPRESSION`.

`App.createReaderProperties()` configura limiti iText:

- `pdfutility.itext.maxXrefElements`, default `20_000_000`;
- `pdfutility.itext.maxDecompressedStreamsSum`, default `1_073_741_824`.

`App.newPdfReader(Path)` abilita `memorySavingMode`.

Nota di manutenzione: alcune classi usano `new PdfReader(path.toString())`, altre usano `App.newPdfReader(path)`. Per PDF grandi o complessi conviene uniformare l'uso di `App.newPdfReader`.

## 9. Audit logging

Classe:

```text
olivieri.alex.quality.AuditLogger
```

Percorso default:

```text
logs/iso_audit.log
```

Override:

```powershell
java -Dpdfutility.audit.log=C:\log\iso_audit.log -jar target\PDFUtility_2026_V1-1.9.jar
```

Formato:

```text
timestamp;user;action;details;output;status;sha256;error
```

Caratteristiche:

- righe append-only;
- creazione automatica cartella log;
- sanitizzazione newline e `;`;
- SHA-256 solo per output file su success;
- errori di scrittura log non bloccano l'app.

Attenzione: diversi livelli loggano la stessa operazione, ad esempio controller e service. Questo produce piu' righe audit per una singola azione UI.

## 10. Pattern dei tab JavaFX

Ogni tab in `olivieri.alex.fx.tab` segue il pattern:

- metodo statico `create(PdfUtilityFxController controller, Stage ownerStage)`;
- caricamento FXML specifico, quando presente;
- binding pulsanti e campi;
- validazione UI minima;
- chiamata asincrona al controller per non bloccare la UI;
- dialog successo/errore tramite `FxDialogUtils`.

`FxTabControllerSupport` contiene supporto comune per task JavaFX, progress indicator e messaggi.

Per aggiungere un nuovo modulo:

1. Creare servizio in `olivieri.alex.util`.
2. Esporre metodo in `PdfUtilityFxController`.
3. Creare FXML in `src/main/resources/ui`.
4. Creare `FxNomeTab` e relativo content controller.
5. Aggiungere `Tab fx:id` in `PdfUtilityLayout.fxml`.
6. Popolare il tab in `PdfUtilityFxLayoutController.initializeTabs()`.
7. Aggiungere audit action coerente.
8. Aggiungere test sul servizio.

## 11. Test

Test presenti:

```text
src/test/java/olivieri/alex/util/FolderProgressiveCreatorTest.java
```

Copertura attuale: limitata al creatore di cartelle progressive.

Test consigliati prioritari:

- `PdfCsvRenamer`: parsing CSV, collisioni, direct move vs rebuild;
- `PdfMergeService`: ordinamento, merge in blocchi, rotazione, A4;
- `PdfMarkerSplitter`: marker duplicati, append, prima pagina senza marker;
- `PdfShipmentSplitter`: raggruppamento invii e validazione prima pagina;
- `DuFileMerger`: master `_01_`, skip header, output uguale a input;
- `CsvToExcelConverter`: delimiter, quoting, BOM;
- `PdfSearchExcelExtractor`: split campi e case-sensitive;
- audit logger con path custom.

## 12. Packaging e distribuzione

Il profilo `installer`:

- copia i moduli JavaFX Windows in `target/jfx-modules`;
- invoca `jpackage`;
- genera EXE;
- imposta shortcut e menu Start;
- usa icona `src/main/resources/ui/logo.ico`;
- usa `win.upgrade.uuid` stabile per aggiornamenti.

Per distribuzione enterprise via GPO puo' servire MSI. La documentazione WiX e' in `WIX_README_IT.md`.

## 13. Convenzioni operative di output

Nomi default principali:

```text
merged.pdf
{cartella}_merge_blocchi/
{nome}_blank_pages.pdf
{nome}_after_{frase}.pdf
{nome}_optimized.pdf
{cartella}_optimized/
{nome}_riso_gl9730.pdf
{nome}_riso_comcolor_gd9630.pdf
{nome}_xN.pdf
{nome}_alternating.pdf
{nome}_without_odd.pdf
{nome}_without_even.pdf
{nome}_filtered_text.pdf
SenzaUltimaPagina/
Result/
Stamped/
{nome}_keyword_{keyword}.pdf
{nome}.xlsx
{nome}_estrazione_{chiave}.xlsx
{nome}_word.docx
{cartella}_word/
```

## 14. Rischi e debito tecnico osservato

- Doppia UI Swing/JavaFX ancora presente: aumenta superficie di manutenzione.
- `PdfOptimizer` dichiara nel commento PDF 2.0, ma le writer properties sono PDF 1.7.
- `RisoGl9730Optimizer` e `RisoComcolorGd9630Optimizer` sono duplicati quasi identici.
- Parsing CSV di `PdfCsvRenamer` e' semplice e puo' fallire su CSV complessi o righe con entrambi i separatori.
- Test automatici molto limitati rispetto al numero di trasformazioni file.
- Alcuni servizi usano `App.newPdfReader`, altri no: gestione memoria non uniforme.
- Le funzioni basate su estrazione testo non supportano OCR.
- Alcuni timbri usano font size 1: probabilmente sono timbri tecnici/invisibili, ma va documentata la scelta funzionale.
- Il target `target/` e' presente nel repository/worktree e contiene artefatti compilati: conviene verificare policy Git.

## 15. Checklist per modifiche future

Prima di modificare una lavorazione PDF:

1. Identificare il servizio in `util`.
2. Verificare se esiste chiamata da JavaFX e Swing legacy.
3. Mantenere compatibilita' dei nomi output se usati dagli operatori.
4. Aggiungere o aggiornare audit action.
5. Aggiungere test su file temporanei.
6. Eseguire `mvn test`.
7. Se cambia packaging, eseguire anche build con profilo `installer`.

## 16. Comandi diagnostici utili

Elenco sorgenti:

```powershell
rg --files src\main\java src\main\resources
```

Metodi pubblici del controller:

```powershell
rg "public Path|public .*Result|public BatchMergeResult" src\main\java\olivieri\alex\fx\PdfUtilityFxController.java
```

Esecuzione test:

```powershell
mvn test
```

Verifica dipendenze:

```powershell
mvn dependency:tree
```
