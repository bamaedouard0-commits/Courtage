# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

**FileRescue Libre** is an open-source Android app (published by OUAGADOUSOFT) that recovers
deleted files (photos, videos, and eventually music/documents) on **rooted devices only**. It is
a free, no-paywall alternative to proprietary tools (Wondershare Recoverit, DiskDigger Pro,
dr.fone), using publicly documented recovery techniques (file carving, file-signature analysis).
It has zero telemetry, no accounts, and no required network access — everything runs locally.

The V1/MVP targets root-only devices because scoped storage (Android 10+) makes reliable
low-level block-device scanning impossible without root.

Code comments and commit messages in this repo are written in **French**; identifiers (classes,
functions, variables) are in English. Follow this convention when editing existing files.

## Build, lint, and run

This is a standard single-module Gradle/Android project (Kotlin + Jetpack Compose). There is no
CI config and no test suite in the repo yet (`src/test` / `src/androidTest` do not exist) — do
not assume test commands work until test source sets are actually added.

```bash
./gradlew assembleDebug          # build the debug APK
./gradlew build                  # full build (compile + lint + assemble)
./gradlew lint                   # Android lint only
./gradlew installDebug           # build and install on a connected/rooted device or emulator
```

Because the app requires root at runtime (it shells out via `libsu` to read raw block devices),
most functionality (quick scan, deep scan) cannot be exercised on a non-rooted emulator — the app
will fall back to `NoRootScreen`. Testing the actual scan engines requires a rooted device or a
rooted emulator image (Magisk).

Key config: `applicationId`/`namespace` = `com.ouagadousoft.filerescuelibre`, `minSdk` = 26,
`compileSdk`/`targetSdk` = 35, Kotlin/Java target 17. Version catalog is
`gradle/libs.versions.toml` — add new dependencies there, not as inline coordinates in
`app/build.gradle.kts`. `libsu` is resolved via JitPack (declared in `settings.gradle.kts`).

## Architecture

Layered, unidirectional flow:

```
UI (Compose) → ViewModel → Domain (contracts/models) → Data/Native (implementations)
```

```
app/src/main/java/com/ouagadousoft/filerescuelibre/
├── ui/          # Compose screens, navigation (FileRescueNavHost), theme
├── viewmodel/   # App state, orchestrates scans via domain repository interfaces
├── domain/      # Models, repository interfaces, business rules — no Android/root dependencies
├── data/        # Root shell access, quick-scan and carving implementations
└── native/      # Reserved for a future NDK (C/C++) port of the carving engine — currently empty
```

There is no DI framework (no Hilt/Koin). `ScanViewModel` directly `new`s its repository
implementations as default constructor parameters (see `ScanViewModel` in `viewmodel/`), which
also makes them swappable for tests via constructor injection. Follow this pattern rather than
introducing a DI framework unless the project explicitly adopts one.

Two independent scan features share one `ScanViewModel`/`ScanUiState` and are wired together in
`ui/FileRescueNavHost.kt` (`home → scan_progress → scan_results`):

- **Quick scan (F3)** — `data/scan/QuickScanRepositoryImpl`: no low-level reading. Shells out
  (via `RootShell`/`libsu`) to `find`/`stat` for files already flagged as deleted by the system:
  MediaStore-trashed files (`.trashed-*` prefix, Android 11+) and per-app FUSE trash bins
  (`.Trash-<uid>` directories). Scoped to a `ScanZone` (F2: DCIM, Pictures, Movies, Download,
  WhatsApp, or full storage) selected in `HomeScreen`; `.Trash-<uid>` bins live at the storage
  root so they're only searched when the zone is `FULL_STORAGE`.
- **Deep scan (F4)** — `data/carving/DeepScanRepositoryImpl` + `FileCarver.kt`: raw file
  carving on the `userdata` block device, PhotoRec-style (signature/magic-number search, not
  PhotoRec's code). `BlockDeviceLocator` resolves the device path (known
  `/dev/block/by-name/userdata`-style candidates, then a broad `find` fallback) and its size via
  root shell commands. The engine streams the device in `CHUNK_SIZE` (4 MiB) blocks with a
  `CHUNK_OVERLAP` so a signature straddling a chunk boundary is never missed, finds the earliest
  matching signature per chunk, then determines file length either deterministically (a
  container's declared size field: BMP, RIFF/WEBP/AVI, ISO-BMFF/MP4 box walking) or by searching
  for a known footer within a bounded window (JPEG `FFD9`, PNG `IEND`+CRC). A carve that can't
  establish a reliable length returns `null` and is skipped rather than emitting a truncated file
  presented as recovered — extending a format here means adding both a deterministic-length path
  and thorough truncation handling, not just a header signature.
  Carved files are streamed straight to the app's private storage (`filesDir/carved`) as they're
  found, never buffered fully in memory.
  **GIF and MKV are intentionally excluded** from carving in V1: their end-of-file can't be
  determined reliably by signature search alone (GIF trailer too short to be discriminant, MKV/
  EBML needs a real parser). Do not add naive signature-only support for these without solving
  that problem, or re-check `README.md` in case a real parser has since been added.
- Root access itself goes through `data/root/RootShell` (thin `libsu` `Shell.cmd(...).exec()`
  wrapper) and `viewmodel/RootViewModel`, which gates the whole UI: `MainActivity` shows
  `NoRootScreen` unless `RootViewModel.rootState` is `Granted`.

`ReliabilityLevel` (`INTACT` vs `PARTIAL`) on `RecoverableFile` reflects whether a carved file's
end was proven (footer found / natural box-walk end) or only bounded by a size cap / truncated
container walk — preserve this distinction in the UI and don't collapse it silently when adding
new carve formats.

`SupportedFormats.categoryOf` (domain layer) currently accepts a broader extension set (including
gif/mkv) than the carving engine actually recovers — it's shared with quick scan, which doesn't
carve and isn't limited by carving's footer/EBML problem. Don't assume the two lists are meant to
stay in sync.

## Known V1 limitations (see README.md for current status)

- Carving is pure Kotlin via `libsu:io`, not a native NDK module — functional but slower than a
  C/C++ engine on large volumes. A native port is a documented follow-up, not a blocker.
  `native/` is reserved for this.
- No persistent pause/resume across scan sessions yet.
- No Room database yet (planned for scan history) despite being listed in the tech stack table in
  `README.md`.
- No thumbnails yet (Coil/Glide planned).

Check `README.md`'s "Statut" section for the current up-to-date feature checklist before assuming
something is or isn't implemented.
