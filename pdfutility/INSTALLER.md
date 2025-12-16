# Installatore

1. Verifica di usare JDK 17+ (lo stesso `java.home` usato da Maven) e, se intendi generare un installer MSI, installa il [WiX Toolset](https://wixtoolset.org/) 3.11+ aggiungendo i binari al `%PATH%`.
2. Genera l'installer nativo con:

   ```sh
   mvn clean package -Pinstaller
   ```

3. Quando la build riesce, l'MSI si trova in `target/installer` con nome `PDFUtility-<version>.msi`.

### Distribuzione enterprise

- **Per-machine/SYSTEM-ready**: il template WiX personalizzato imposta `InstallScope="perMachine"` e `InstallPrivileges="elevated"`, quindi `ALLUSERS=1` e il pacchetto si installa correttamente sotto laccount SYSTEM locale (adatto alle GPO computer in modalita Assigned).
- **Silenzioso e senza UI**: l'MSI non registra piu i dialoghi standard, quindi puoi lanciarlo con `msiexec /i … /qn /norestart` o distribuirlo via automazione senza interazione utente o prompt UAC (l'elevazione viene gestita dal contesto SYSTEM che esegue linstallazione).
- **Compatibile GPO**: copia l'MSI in una GPO computer (Assigned), punta su `PDFUtility_2026_V1-<version>.msi` e Windows Installer lo installera silenziosamente su ogni macchina raggiunta dalla policy.

Comando di esempio:

```bat
msiexec /i target\installer\PDFUtility_2026_V1-1.0.msi /qn /norestart /l*v "%TEMP%\\PDFUtility-install.log"
```

Per personalizzare traduzioni, icone o altre risorse di questo build modifica i file sotto `src/main/resources/installer` prima di ricompilarlo.

### Note

- Il profilo copia il JAR del progetto e le dipendenze di runtime in `target/jpackage` prima di invocare `jpackage`.
- Il profilo `installer` attualmente produce MSI per Windows; cambia `<installer.type>` o aggiungi argomenti `--type` se serve un formato diverso.
- Per fornire un'icona o altre risorse personalizzate aggiungile in `src/main/resources/installer` ed estendi gli argomenti di jpackage nel profilo.
