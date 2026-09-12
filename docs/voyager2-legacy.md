# Voyager 2 legacy import (CN1SDK)

UrsulaGIS can import harvest data from **Case IH Voyager 2** cards (`.vy1`) through a legacy compatibility layer based on the obsolete **CN1SDK** project. This path exists only for **old machines** that still export `.vy1` cards; it is not the primary import workflow.

The Windows MSI built by GitHub Actions ships everything required. End users do **not** need to install CN1SDK or edit `config.properties`.

## What gets bundled

During `mvn package` on Windows, files from `libs/voyager2/` are copied into the jpackage input folder and end up inside the installed app:

```
%LOCALAPPDATA%\UrsulaGIS-Desktop_Zulu\
└── app\
    ├── lib\
    │   └── cnh-voyager2-java-wrapper-1.0.0.jar
    └── voyager2\
        ├── native\
        │   ├── CNHVoyager2JNI.dll
        │   ├── CNHVoyager2Bridge.dll
        │   └── nethost.dll
        └── sdk\
            ├── CNHVoyager2.dll
            ├── Voyager2SampleApp.dll
            └── …other .NET dependencies
```

At runtime, `Voyager2Settings` resolves paths automatically:

1. `config.properties` overrides (`VOYAGER2_SDK_PATH`, `VOYAGER2_NATIVE_LIB_PATH`) — optional
2. Installed app folder (parent of `jpackage.app-path` exe, or parent of `java.home` → `app/voyager2/…`)
3. Development tree (`libs/voyager2/…` relative to the project root)

The license key is embedded in the application for this legacy path.

## End-user steps

1. Install the **Windows x64 MSI** from the GitHub Actions build (branch `java17`).
2. Open UrsulaGIS → **Cosecha** → **Importar Voyager (.vy1)**.
3. Select a `.vy1` file on the Voyager card.

If the card contains more than one harvest dataset, a selection dialog is shown.

### .NET 8 Desktop Runtime

The bundled SDK is **framework-dependent** (not self-contained). On very old PCs, install [.NET 8 Desktop Runtime (x64)](https://dotnet.microsoft.com/download/dotnet/8.0) once if import fails with a host/runtime error (`ERROR_HOSTFXR_LOAD_FAILED`).

## Repository layout

```
libs/voyager2/
├── native/     JNI bridge DLLs (from CN1SDK JavaWrapper\build\Release)
└── sdk/        .NET SDK output (from Voyager2SampleApp\bin\Release\net8.0-windows7.0)
```

These binaries are committed to the repo so CI can build the MSI without compiling CN1SDK.

## Refreshing the bundle (developers)

When CN1SDK binaries change locally, run from the repo root:

```powershell
.\scripts\refresh-voyager2-bundle.ps1
```

Or copy manually:

| Source (CN1SDK) | Destination |
|---|---|
| `JavaWrapper\build\Release\CNHVoyager2JNI.dll` | `libs/voyager2/native/` |
| `JavaWrapper\build\Release\CNHVoyager2Bridge.dll` | `libs/voyager2/native/` |
| `JavaWrapper\build\Release\nethost.dll` | `libs/voyager2/native/` |
| `bin\Release\net8.0-windows7.0\*` | `libs/voyager2/sdk/` |

Then commit `libs/voyager2/` and push.

## Build pipeline

The workflow [`.github/workflows/build-windows-installer.yml`](../.github/workflows/build-windows-installer.yml):

1. Checks out the repo
2. Installs JDK 17 (Liberica + JavaFX) and .NET 8 SDK
3. **Verifies** that `libs/voyager2/` contains the required DLLs
4. Runs `mvn clean install` (Maven copies `voyager2/` into `target/dependency/` before jpackage)
5. Publishes the `.msi` artifact

Maven step (Windows profile only): `bundle-voyager2-legacy` in `pom.xml` copies the bundle into `target/dependency/voyager2/` during the `package` phase.

## Platform scope

| Platform | Voyager import |
|---|---|
| Windows x64 MSI | Supported (bundled) |
| Linux / macOS installers | Not supported (no native DLLs) |
| IDE / `mvn javafx:run` on Windows | Supported via `libs/voyager2/` |

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| `Voyager 2 SDK path is not configured` | Old build that mis-resolved `jpackage.app-path` (exe vs folder), or MSI without `app/voyager2` — update/reinstall Windows MSI |
| `Voyager 2 SDK path does not exist` | MSI built without bundle, or corrupt install — reinstall |
| `CNHVoyager2.dll not found under: .../app/voyager2` | Config pointed at the `voyager2` parent instead of `voyager2/sdk` (or a stale absolute path). Current builds auto-append `sdk/` and ignore broken overrides; clear `VOYAGER2_SDK_PATH` / `VOYAGER2_NATIVE_LIB_PATH` in `%APPDATA%\UrsulaGIS\config.properties` and restart, or reinstall a build that includes this fix |
| `CNHVoyager2JNI.dll not found` | Missing `app/voyager2/native` in install, or an old `cnh-voyager2-java-wrapper` JAR that hardcodes a developer path — rebuild/replace the wrapper JAR and MSI |
| `CNHVoyager2JNI.dll not found in D:\worskpaces\CN1SDK_...` | Broken wrapper JAR ignoring the runtime path; update `libs/cnh-voyager2-java-wrapper-1.0.0.jar`. Also clear developer paths left in `%APPDATA%\UrsulaGIS\config.properties` |
| `ERROR_HOSTFXR_LOAD_FAILED` | Install .NET 8 Desktop Runtime x64 |
| `No harvest dataset found on card` | Card has no harvest data, or wrong folder selected |
| Import menu missing / disabled on Windows x64 | Unsupported reason shown by chat / logs — usually missing bundle, wrong arch, or stale `VOYAGER2_*` paths in config |
| Import menu missing on Linux/macOS | Expected — Windows-only legacy feature |

### Quick fix on an installed PC

1. Confirm files exist:
   - `%LOCALAPPDATA%\UrsulaGIS-Desktop_Zulu\app\voyager2\sdk\CNHVoyager2.dll`
   - `%LOCALAPPDATA%\UrsulaGIS-Desktop_Zulu\app\voyager2\native\CNHVoyager2JNI.dll`
2. Edit `%APPDATA%\UrsulaGIS\config.properties` and **clear** (leave empty) or delete:
   - `VOYAGER2_SDK_PATH=`
   - `VOYAGER2_NATIVE_LIB_PATH=`
3. Restart UrsulaGIS. Paths are resolved from the install folder automatically.

## Related code

- `ImportarCosechaVoyagerTask` — import task
- `Voyager2Settings` — path resolution
- `Voyager2NativeLoader` — loads JNI DLLs before SDK use
- `CosechaGUIController.doOpenCosechaVoyager()` — UI entry point
