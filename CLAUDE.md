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

All UI text goes through `stringResource`/`values/strings.xml` (French, the default locale) with
an English translation in `values-en/strings.xml` — every screen follows this now. Add both when
introducing new user-facing text; don't hardcode strings in Composables.

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
`app/build.gradle.kts`. `libsu` is resolved via JitPack (declared in `settings.gradle.kts`). Room
uses **kapt**, not KSP, for its annotation processor (`kotlin.kapt` plugin, pinned to the same
`kotlin` version — chosen over KSP to avoid tracking a separate KSP-release version number).

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
`ui/FileRescueNavHost.kt` (`home → scan_progress → scan_results`, plus `home → history →
scan_results`):

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
- **Recovery/export** — `data/recovery/RecoveryRepositoryImpl`: copies a found file (quick-scan
  trash hit or deep-scan carved output) to `/storage/emulated/0/FileRescueLibre/Recupere`, a
  normal folder outside the app, visible to any file manager/gallery — the original is never
  moved or deleted. Runs via root shell (`cp`), same as the rest of the data layer, since carved
  files live in the app's private storage and trashed files sit under scoped-storage paths the
  app can't otherwise write from. `ScanViewModel.recoverFile` tracks per-file `RecoveryStatus`
  (`InProgress`/`Success`/`Error`) keyed by `RecoverableFile.path`, surfaced as a button/spinner/
  checkmark on each row in `ScanResultsScreen`.
- **Thumbnails** — `data/thumbnail/ThumbnailLoader`: decodes a small (`MAX_DIMENSION_PX` =
  160px) `Bitmap`, reading via `SuFileInputStream` (root) for the same reason as recovery —
  trashed files aren't readable through normal file APIs. Deliberately plain
  `BitmapFactory`/`MediaMetadataRetriever`, no Coil/Glide dependency yet.
  Images: `BitmapFactory` with a bounds-only pre-pass to compute `inSampleSize`. Videos:
  `MediaMetadataRetriever.getFrameAtTime()` via `SuFileMediaDataSource`, a private
  `MediaDataSource` implementation that bridges the retriever's random-access reads onto the same
  root `SuFileInputStream`/`FileChannel` pattern `DeepScanRepositoryImpl` uses for the raw
  partition — `MediaMetadataRetriever.setDataSource(String path)` isn't usable for a trashed file
  scoped storage won't let the app read directly. `SuFileMediaDataSource` reports the already-known
  `RecoverableFile.sizeBytes` as its size rather than calling `channel.size()` (unreliable for
  this root-backed stream). `ScanResultsScreen`'s `Thumbnail` composable draws a small
  play-icon badge over a successfully-decoded video frame so it doesn't read as a photo; on
  `null` (codec/format not supported, corrupt carve, or a category with no dedicated path) it
  falls back to a generic category icon instead. Returns `null` for any image that fails to
  decode too, including HEIC on API 26/27 devices (`minSdk` = 26; HEIF/HEIC decoding in
  `BitmapFactory` only landed in API 28). Loads per-row via `LaunchedEffect(file.path)`, with no
  cross-scroll cache — reloads on recomposition.
- **Filters** — `ScanResultsScreen` derives `filteredResults` locally (plain `remember`, no
  ViewModel state) from two independent single-select filters: `FileCategory` and
  `ReliabilityLevel`. Each renders as a `FilterChipRow` only when it would actually narrow
  anything — the category row is hidden unless results span more than one category, and the
  reliability row is hidden unless at least one `PARTIAL` result exists — so a typical quick-scan
  result set (single category, all `INTACT`) shows no filter chips at all.
- **Scan history** — `data/history/` (Room: `AppDatabase`, `ScanHistoryDao`,
  `ScanSessionEntity` + `RecoveredFileEntity`, one-to-many via `sessionId` with cascade delete).
  Every completed quick/deep scan is persisted automatically at the end of
  `ScanViewModel.startQuickScan`/`startDeepScan` (`scanHistoryRepository.saveScan(...)`) — every
  scan is saved unconditionally, there's no cap or pruning of old sessions in V1. `HomeScreen` has
  a "Historique des scans" entry point (`ui/FileRescueNavHost.kt`'s `history` route) listing past
  sessions via `ScanHistoryViewModel`; tapping one calls `ScanViewModel.loadHistoryEntry`, which
  reloads that session's saved `RecoverableFile` rows straight into `ScanUiState.Completed` —
  reusing `ScanResultsScreen` as-is, including recovery and thumbnails, instead of a separate
  read-only view. Recovery/thumbnails on a historical entry re-read from `RecoverableFile.path` at
  the time of loading, so a quick-scan trash hit whose file has since been purged by the OS will
  simply fail to recover/decode (handled the same way as any other missing file) rather than
  crash.
- **Deep scan pause/resume (F9)** — persisted across app kills *and* a manual pause button.
  `data/history/DeepScanProgressEntity` is a singleton row (`id = 0`) written at
  `ScanHistoryRepositoryImpl.beginDeepScanSession` (device path + total size), updated via
  `recordDeepScanPosition` — throttled in `ScanViewModel` to every `PROGRESS_PERSIST_INTERVAL_BYTES`
  (64 MiB) of progress, not every chunk, to avoid a DB write storm — and cleared in
  `finishDeepScanSession` once the scan reaches the end. Every found file is written immediately
  via `recordDeepScanFile` (not batched), so pausing or killing the app never loses an
  already-found result — only up to 64 MiB of unflushed position, which just gets re-scanned.
  On `ScanViewModel` init, `getResumableDeepScan()` populates
  `resumableDeepScan: StateFlow<ResumableDeepScan?>`, surfaced as a "Reprendre le scan approfondi"
  card on `HomeScreen`.
  `ScanViewModel.pauseDeepScan()` cancels the tracked `deepScanJob` (`Job.cancelAndJoin()`, run
  from a *different* coroutine than the one being cancelled, since the cancelled coroutine's own
  code past the cancellation point never resumes — `CancellationException` is deliberately
  rethrown, not swallowed, matching the rest of this ViewModel's cancellation handling) and only
  then flips `uiState` back to `Idle` and refreshes `resumableDeepScan`; `ScanProgressScreen`
  shows the pause button whenever `progressFraction != null`, the same signal already used to
  distinguish a deep scan in progress from a quick one (which never sets it) — no separate
  "which scan is running" state needed.
  `DeepScanRepository.deepScan(startOffset, expectedDevicePath)` only honors `startOffset` if
  `BlockDeviceLocator` resolves the same `expectedDevicePath` as before — resuming at a byte
  offset on a *different* resolved device would read nonsense — signaled back via
  `DeepScanEvent.Started.resumedFromOffset`; `ScanViewModel.startDeepScan` checks that flag
  before deciding whether to reuse the old `sessionId`/pre-seeded results or start a fresh
  session at 0. Treat this device-identity check as load-bearing if you touch `startOffset`
  handling — silently trusting a stale offset on a mismatched device is a correctness bug, not
  just a UX one.

`ReliabilityLevel` (`INTACT` vs `PARTIAL`) on `RecoverableFile` reflects whether a carved file's
end was proven (footer found / natural box-walk end) or only bounded by a size cap / truncated
container walk — preserve this distinction in the UI and don't collapse it silently when adding
new carve formats.

`SupportedFormats.categoryOf` (domain layer) currently accepts a broader extension set (including
gif/mkv) than the carving engine actually recovers — it's shared with quick scan, which doesn't
carve and isn't limited by carving's footer/EBML problem. Don't assume the two lists are meant to
stay in sync.

## Not yet implemented (as of this doc)

- No cap/pruning on scan history — every scan is kept forever; a heavy user could grow
  `filerescue.db` unbounded.

## Known V1 limitations (see README.md for current status)

- Carving is pure Kotlin via `libsu:io`, not a native NDK module — functional but slower than a
  C/C++ engine on large volumes. A native port is a documented follow-up, not a blocker.
  `native/` is reserved for this.

Check `README.md`'s "Statut" section for the current up-to-date feature checklist before assuming
something is or isn't implemented.
