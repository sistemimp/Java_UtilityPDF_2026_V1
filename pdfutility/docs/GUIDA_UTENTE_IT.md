# PDFUtility 2026 - Guida di utilizzo

## 1. Panoramica

PDFUtility 2026 e' un'applicazione desktop Windows per lavorazioni ricorrenti su PDF, CSV, TXT, DU, Excel e Word. L'interfaccia attiva del progetto e' JavaFX e viene avviata dal launcher `olivieri.alex.Launcher`.

Le funzioni principali sono:

- unione e miscelazione di PDF;
- filtri, split e rimozione pagine;
- ottimizzazione PDF e conversione PDF/A per stampanti Riso;
- rinomina da CSV, rinomina progressiva e rinomina da QR/barcode;
- conversione CSV in Excel ed estrazione dati da PDF verso Excel;
- unione file CSV/TXT e DU;
- conversione PDF in Word;
- creazione cartelle progressive;
- timbri testuali su PDF;
- log audit per tracciabilita' operazioni.

## 2. Avvio e installazione

### Avvio da installer

Se hai ricevuto l'installer Windows, esegui:

```bat
PDFUtility_2026_V1-1.9.exe
```

Al termine l'applicazione e' disponibile dal menu Start e, se previsto dalla build, tramite collegamento desktop.

### Avvio da sorgente

Prerequisiti:

- Windows;
- JDK 21 consigliato;
- Maven;
- accesso in scrittura alle cartelle dove verranno generati i file.

Comando:

```powershell
cd pdfutility
mvn javafx:run
```

In alternativa, dopo il package:

```powershell
cd pdfutility
mvn clean package
java -jar target\PDFUtility_2026_V1-1.9.jar
```

## 3. Regole generali

- Le elaborazioni non modificano quasi mai il PDF originale: producono un nuovo file o una sottocartella di output.
- Le cartelle vengono elaborate solo al primo livello: i PDF in sottocartelle non vengono inclusi.
- Quando una cartella contiene piu' PDF, l'ordine e' alfabetico per nome file, salvo funzioni specifiche.
- Se lasci vuoto il campo output, l'app propone un nome di default vicino al file o alla cartella sorgente.
- I PDF devono contenere testo selezionabile per le funzioni che cercano stringhe. I PDF solo immagine possono non produrre risultati nelle ricerche testuali.
- Al termine delle operazioni viene scritto il log audit in `logs/iso_audit.log`, oppure nel percorso impostato dalla proprieta' Java `pdfutility.audit.log`.

## 4. Log audit

Il pulsante `Apri log audit` apre il file di tracciabilita'. Ogni riga contiene:

```text
timestamp;utente;azione;dettagli;output;stato;sha256;errore
```

Per le operazioni concluse con successo su un file, il log calcola anche l'hash SHA-256 dell'output. Per output a cartella l'hash resta vuoto.

## 5. Moduli PDF

### Accoda PDF

Percorso: `PDF > Unione PDF > Accoda PDF`

Input:

- cartella contenente PDF;
- file di output;
- rotazione opzionale delle pagine non A4 portrait;
- opzione `Forza foglio A4`.

Comportamento:

- prende tutti i PDF direttamente nella cartella;
- li ordina alfabeticamente;
- copia tutte le pagine in un unico PDF;
- se richiesto, ruota solo le pagine che non sono A4 verticale;
- se richiesto, adatta il contenuto a pagina A4.

Output di default: `merged.pdf` oppure nome indicato nel campo output.

### Merge a blocchi

Percorso: `PDF > Unione PDF > Merge a blocchi`

Input:

- cartella PDF;
- cartella output;
- numero di PDF per gruppo;
- prefisso file output;
- rotazione opzionale delle pagine non A4 portrait.

Comportamento:

- ordina i PDF alfabeticamente;
- li divide in gruppi consecutivi da N file;
- crea un PDF per ogni gruppo;
- l'ultimo gruppo contiene gli eventuali file rimanenti.

Output:

- cartella di output;
- file con formato simile a `prefisso_001_primo-ultimo.pdf`.

### Ripeti PDF

Percorso: `PDF > Unione PDF > Ripeti PDF`

Input:

- PDF sorgente;
- numero di ripetizioni, minimo 1;
- file di output.

Comportamento:

- copia l'intero PDF sorgente N volte nello stesso documento.

Output di default: `nomefile_xN.pdf`.

### Miscelazione alternata

Percorso: `PDF > Unione PDF > Miscelazione alternata`

Input:

- primo PDF;
- secondo PDF;
- pagine del primo per blocco;
- pagine del secondo per blocco;
- file di output.

Comportamento:

- prende un blocco dal primo PDF, poi un blocco dal secondo;
- continua fino a esaurimento di entrambi i documenti;
- se un PDF finisce prima, continua con le pagine residue dell'altro.

Output di default: `nomeprimo_alternating.pdf`.

### Unisci per nome

Percorso: `PDF > Unione PDF > Unisci per nome`

Input:

- cartella 1;
- cartella 2;
- opzione `Normalizza F/R`.

Comportamento:

- cerca PDF con lo stesso nome nelle due cartelle;
- per ogni coppia crea un PDF nella cartella `Result`;
- l'output prende il nome del file presente in cartella 1;
- genera `missing_pairs.txt` se esistono file senza corrispondente;
- se `Normalizza F/R` e' attivo e il primo PDF ha un numero dispari di pagine, aggiunge una pagina bianca prima di accodare il secondo PDF.

Output: cartella `Result` accanto alla prima cartella.

## 6. Filtri e rimozione pagine

### Filtro pagine

Percorso: `PDF > Filtra PDF > Filtro pagine`

Input:

- PDF sorgente;
- scelta tra `Rimuovi pagine dispari` e `Rimuovi pagine pari`;
- file di output.

Output di default:

- `nome_without_odd.pdf`;
- `nome_without_even.pdf`.

### Rimuovi ultima pagina

Percorso: `PDF > Filtra PDF > Rimuovi ultima pagina`

Input: cartella PDF.

Comportamento:

- crea una copia di ogni PDF senza l'ultima pagina;
- salta i PDF con una sola pagina;
- raccoglie avvisi per file non elaborati.

Output: sottocartella `SenzaUltimaPagina`, con suffisso numerico se esiste gia'.

### Rimuovi pagine contenenti testo

Questa funzione e' esposta dal controller applicativo ed e' presente nella base Swing legacy. Rimuove dal PDF tutte le pagine che contengono una stringa indicata, con opzione case-sensitive. Se tutte le pagine verrebbero rimosse, non genera il documento.

Output di default: `nome_filtered_text.pdf`.

## 7. Pagine bianche

### Pagine bianche

Percorso: `PDF > Pagine Bianche > Pagine Bianche`

Input:

- PDF sorgente;
- file di output.

Comportamento:

- dopo ogni pagina originale inserisce una pagina bianca della stessa dimensione.

Output di default: `nome_blank_pages.pdf`.

### Pagine bianche dopo testo

Percorso: `PDF > Pagine Bianche > Pagine Bianche dopo testo`

Input:

- PDF sorgente;
- frase o parola da cercare;
- opzione `Rispetta maiuscole/minuscole`;
- opzione `Solo se la frase si trova in una pagina dispari`;
- file di output.

Comportamento:

- copia tutte le pagine;
- dopo ogni pagina che contiene la frase aggiunge una pagina bianca;
- se l'opzione pagine dispari e' attiva, aggiunge la bianca solo quando la pagina originale e' dispari.

Output di default: `nome_after_frase.pdf`.

## 8. Split PDF

### Split per stringa

Percorso: `PDF > Split per stringa`

Input:

- PDF o cartella sorgente;
- stringa marker;
- opzione case-sensitive;
- cartella base;
- nome cartella risultati;
- opzione `Aggiungi pagine se esiste`.

Comportamento:

- ogni pagina che contiene il marker apre un nuovo documento;
- il nome del documento deriva dal testo che segue il marker nella stessa riga;
- se `Aggiungi pagine se esiste` e' attivo, segmenti con lo stesso nome vengono accodati allo stesso output;
- se e' disattivo, i duplicati diventano `NOME.pdf`, `NOME_2.pdf`, ecc.;
- la prima pagina del gruppo deve contenere il marker.

Output: cartella sotto la cartella base selezionata.

### Split per invii

Percorso: `PDF > Split per invii`

Input:

- PDF sorgente;
- stringa marker invio;
- opzione case-sensitive;
- invii per file;
- cartella base;
- nome cartella risultati;
- prefisso file output.

Comportamento:

- ogni pagina contenente il marker viene considerata inizio di un invio;
- la prima pagina del PDF deve contenere il marker;
- raggruppa N invii per file;
- genera file progressivi.

Output: `prefisso_001_invii_1-N.pdf`, ecc.

## 9. Ottimizzazione PDF

### Ottimizzazione PDF

Percorso: `PDF > Ottimizza PDF > Ottimizzazione PDF`

Input:

- singolo PDF o cartella PDF;
- file output, se l'input e' un singolo PDF.

Comportamento:

- ricrea il documento con impostazioni iText di compressione e smart mode;
- per cartella crea una cartella sorella `{nome_cartella}_optimized`.

Output di default per file: `nome_optimized.pdf`.

### Riso GL9730

Percorso: `PDF > Ottimizza PDF > Riso GL9730`

Input:

- PDF sorgente;
- file output;
- Record ID opzionale.

Comportamento:

- genera PDF/A-3B;
- imposta output intent sRGB;
- se compilato, aggiunge `RecordID` nel catalogo PDF.

Output di default: `nome_riso_gl9730.pdf`.

### Riso ComColor GD9630

Percorso: `PDF > Ottimizza PDF > Riso ComColor GD9630`

Input e comportamento equivalenti al modulo GL9730, con output di default:

```text
nome_riso_comcolor_gd9630.pdf
```

## 10. Timbri PDF

### Timbro cartella

Percorso: `PDF > Timbro PDF > Timbro cartella`

Input:

- cartella PDF;
- testo timbro;
- coordinate X e Y.

Comportamento:

- applica il testo alla prima pagina di ogni PDF;
- usa font Helvetica a dimensione 1;
- scrive avvisi per file non elaborabili.

Output: sottocartella `Stamped`, con suffisso numerico se esiste gia'.

### Timbro parole chiave

Percorso: `PDF > Timbro PDF > Timbro parole chiave`

Input:

- PDF sorgente;
- parola chiave;
- testo timbro;
- output;
- opzione case-sensitive;
- coordinate X e Y.

Comportamento:

- copia tutte le pagine;
- applica il timbro solo alle pagine che contengono la parola chiave.

Output di default: `nome_keyword_chiave.pdf`.

## 11. Moduli CSV e DU

### Rinomina da CSV

Percorso: `CSV > Rinomina da CSV`

Input:

- cartella PDF;
- file CSV;
- opzione di rinomina diretta.

Formato CSV atteso:

```csv
originale.pdf;nuovo_nome.pdf
altro_originale;altro_nome
```

Sono accettati separatori `;` o `,`. Le righe vuote e quelle che iniziano con `#` vengono ignorate.

Comportamento standard:

- se piu' righe puntano allo stesso nuovo nome, i PDF sorgente vengono accodati in un unico PDF;
- aggiunge un timbro tecnico invisibile/minimo `anchorage->nomefile`;
- avvisa per file mancanti.

Comportamento con rinomina diretta:

- esegue solo move/rename;
- non ricostruisce il PDF;
- non accoda piu' sorgenti nello stesso output;
- se la destinazione esiste gia', salta la riga e segnala un avviso.

### Rinomina progressiva

Percorso: `CSV > Rinomina progressiva`

Input:

- cartella PDF;
- lunghezza con zeri, da 1 a 12.

Comportamento:

- ordina i PDF alfabeticamente;
- mantiene il nome base e aggiunge zeri a sinistra fino alla lunghezza richiesta.

Esempio con lunghezza 3:

```text
1.pdf -> 001.pdf
25.pdf -> 025.pdf
```

### Unisci CSV/TXT

Percorso: `CSV > Unisci CSV/TXT`

Input:

- lista di file `.csv` o `.txt`;
- file di output.

Comportamento:

- concatena i file nell'ordine selezionato;
- usa codifica UTF-8;
- inserisce newline tra le righe.

### Unisci DU

Percorso: `CSV > Unisci DU`

Input:

- lista di file `.DU`;
- file output `.DU`.

Comportamento:

- ordina i file per nome;
- se un file contiene `_01_` nel nome, lo mette come master iniziale;
- scrive tutto il primo file;
- dai file successivi salta la prima riga;
- impedisce di usare come output uno dei file sorgente.

## 12. Rinomina QR

Percorso: `Rinomina QR`

Input:

- cartella PDF sorgente;
- cartella output.

Comportamento:

- legge la prima pagina di ogni PDF a 200 DPI;
- prova a decodificare QR Code, Code 39 e ITF;
- copia il PDF in output con nome `valorecodice_nomeoriginale.pdf`;
- se non trova codici, sposta il PDF originale nella sottocartella `scarti` dell'output;
- in caso di nomi duplicati aggiunge un suffisso progressivo.

Nota: questa funzione puo' spostare in `scarti` i PDF senza codice rilevabile.

## 13. Moduli Excel

### CSV in Excel

Percorso: `Excel > CSV in Excel`

Input:

- file CSV;
- file Excel `.xlsx`.

Comportamento:

- legge il CSV in UTF-8;
- rimuove BOM iniziale se presente;
- rileva delimitatore tra `;` e `,`;
- gestisce virgolette e doppi apici escapati;
- scrive tutte le celle come testo.

### Estrai da PDF

Percorso: `Excel > Estrai da PDF`

Input:

- PDF;
- chiave di ricerca;
- opzione case-sensitive;
- file Excel.

Comportamento:

- estrae testo pagina per pagina;
- cerca righe contenenti la chiave;
- prende il testo dopo la chiave;
- rimuove prefissi come `:`, `;`, `=`, `-`, `|`;
- divide il valore su `|` in campi separati.

Colonne output:

- `PDF`;
- `Pagina`;
- `Chiave`;
- `Riga completa`;
- `Valore grezzo`;
- `Campo 1`, `Campo 2`, ecc.

## 14. Modulo Word

### PDF in Word

Percorso: `Word > PDF in Word`

Input:

- singolo PDF o cartella PDF;
- output file o cartella.

Comportamento:

- renderizza ogni pagina PDF a 300 DPI;
- inserisce ogni pagina come immagine PNG nel DOCX;
- conserva il layout visivo piu' che il testo modificabile;
- per cartelle genera un DOCX per ogni PDF.

Output di default:

- file singolo: `nome_word.docx`;
- cartella: `{nome_cartella}_word`.

## 15. Modulo Cartelle

Percorso: `Cartelle`

Input:

- cartella destinazione;
- prefisso cartelle;
- numero cartelle, da 1 a 999.

Comportamento:

- crea cartelle `prefisso001`, `prefisso002`, ecc.;
- salta quelle gia' esistenti;
- il prefisso non puo' iniziare/finire con spazi e non puo' contenere caratteri Windows non validi: `\ / : * ? " < > |`.

## 16. Errori comuni

- `Nessun file PDF trovato`: la cartella non contiene PDF nel primo livello.
- `La stringa specificata non e' stata trovata`: il testo non e' presente o il PDF e' composto da immagini.
- `La prima pagina non contiene il marker`: nei moduli di split il documento deve iniziare con una pagina marker.
- `Destinazione gia' esistente`: un output con lo stesso nome esiste gia' e l'operazione scelta non puo' sovrascriverlo.
- `Percorso non valido`: controllare caratteri non ammessi, permessi e file aperti in altre applicazioni.

## 17. Buone pratiche operative

- Lavora su copie quando i file sorgenti hanno valore legale o produttivo.
- Chiudi PDF/Excel/Word prima di lanciare elaborazioni sugli stessi file.
- Usa nomi file coerenti e ordinabili alfabeticamente prima dei merge.
- Verifica sempre il log audit dopo elaborazioni massive.
- Per split e ricerca testo, prova prima su un campione di poche pagine.
