# Compilare WiX Toolset

Questo documento spiega come compilare localmente il **WiX Toolset** (versione 3.11 o successiva) quando si lavora con gli installer MSI generati da `jpackage`.

## Prerequisiti

1. **Visual Studio 2022 o successivo** con il carico di lavoro `Desktop development with C++`.
2. **.NET Framework 4.8** (automatica con Visual Studio o SDK separato).
3. **Git** per clonare il repository ufficiale (`https://github.com/wixtoolset/wix3`).
4. **PowerShell o Developer Command Prompt** con le variabili d'ambiente caricate.

## Passaggi di compilazione

1. Clona il codice di WiX:

   ```powershell
   git clone https://github.com/wixtoolset/wix3.git
   cd wix3
   ```

2. Apri una **Developer Command Prompt** (per esempio quella di Visual Studio) e imposta `/p:Configuration=Release` se vuoi la build ottimizzata.

3. Verifica di trovarti nella root del repository (`E:\Java_Project\wix3` se lo hai clonato lì). Puoi lanciare direttamente il file di progetto principale:

   ```powershell
   msbuild wix.proj /m /p:Configuration=Release
   ```

   oppure, se preferisci compilare solo la soluzione WiX, punta alla soluzione `Wix.sln` dentro `src`:

   ```powershell
   msbuild src\Wix.sln /m /p:Configuration=Release
   ```

4. Al termine troverai gli eseguibili `candle.exe`, `light.exe`, `heat.exe` e gli altri strumenti dentro `src\bin\Release`.

5. Aggiungi questa cartella `%WIX%` (per esempio `C:\repos\wix3\src\bin\Release`) alla variabile `%PATH%` per renderli disponibili a `jpackage`.

## Verifica

- Esegui `candle.exe -?` per assicurarti che lo strumento abbia compilato correttamente.
- Una volta pronto, `jpackage` userà automaticamente `candle.exe` e `light.exe` quando costruisce un MSI con WiX presente nel PATH.

## Note

- WiX 3.11 è l’ultima release compatibile col profilo jpackage di default; WiX 4 potrebbe avere differenze nella struttura dei progetti.
- Se il tuo progetto richiede modifiche ai template WiX (ad es. `main.wxs` in `src/main/resources/installer`), modifica i file dentro `installer` e rilancia `mvn clean package -Pinstaller`.
