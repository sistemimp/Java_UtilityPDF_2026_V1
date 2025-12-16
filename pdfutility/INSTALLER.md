# Installer

1. Ensure you are running JDK 17+ (the same `java.home` that Maven uses), and if you plan to build an MSI installer install the [WiX Toolset](https://wixtoolset.org/) 3.11+ and keep its binaries on `%PATH%`.
2. Generate the native installer with:

   ```sh
   mvn clean package -Pinstaller
   ```

3. When the build succeeds, the MSI installer lives under `target/installer` and is named `PDFUtility-<version>.msi`.

### Notes

- The profile copies the project JAR plus the runtime dependencies into `target/jpackage` before calling `jpackage`.
- The `installer` profile currently targets Windows MSI installers; change `<installer.type>` or add `--type` arguments in the profile if you need a different format.
- To provide a custom icon or other resources, add them to `src/main/resources/installer` and extend the jpackage arguments inside the `installer` profile.
