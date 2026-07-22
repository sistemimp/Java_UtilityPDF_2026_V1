# Installatore

1. Verifica di usare JDK 17+ (lo stesso `java.home` usato da Maven).
2. Genera l'installer nativo con:

   ```sh
   cd pdfutility
   mvn clean package -Pinstaller jpackage:jpackage
   ```

3. Quando la build riesce, l'installer EXE si trova in `target/installer` con nome `PDFUtility_2026_V1-<version>.exe`.

4. Se `clean` fallisce su Windows con errore di delete (`Failed to delete ... .exe`), rimuovi prima il flag read-only:

   ```bat
   attrib -R target\installer\* /S /D
   ```

### Distribuzione enterprise

- **Output corrente**: con la configurazione attuale del `pom.xml` il profilo installer genera `EXE` (`<type>EXE</type>`).
- **Deploy enterprise via GPO**: se ti serve distribuzione GPO "Assigned" nativa, è richiesto un pacchetto MSI.

Comando di esempio:

```bat
target\installer\PDFUtility_2026_V1-1.3.exe
```

Per personalizzare traduzioni, icone o altre risorse di questo build modifica i file sotto `src/main/resources/installer` prima di ricompilarlo.

### Aggiornamenti

- Il `pom.xml` ora definisce `win.upgrade.uuid` e la proprietà viene passata a `jpackage` (`--win-upgrade-uuid`) per generare ogni volta lo stesso `UpgradeCode`. Finché non cambi quella costante, Windows Installer rileverà la release precedente come correlata e lancerà la rimozione prima dell'upgrade.
- Per pubblicare una nuova versione basta incrementare `<version>` nel `pom.xml`, rigenerare l'EXE con `mvn clean package -Pinstaller jpackage:jpackage` e distribuire il nuovo `PDFUtility_2026_V1-<version>.exe`.
- Se devi forzare lo switch di `UpgradeCode` (ad esempio perché una vecchia build ha già lasciato tracce), disinstalla la versione legacy con `msiexec /x {ProductCode}` prima di installare quella nuova.

### Note

- Il profilo copia il JAR del progetto e le dipendenze di runtime in `target/jpackage` prima di invocare `jpackage`.
- Il profilo `installer` attualmente produce EXE per Windows.
- Per fornire un'icona o altre risorse personalizzate aggiungile in `src/main/resources/installer` ed estendi gli argomenti di jpackage nel profilo.
