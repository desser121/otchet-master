# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- App theme switcher (system / light / dark) in a new Settings screen.
- Material cost per item (entered as "name — quantity — price") and work cost field on the report screen.
- Cost summary card with materials, work, and total ("like in wallets") on the report screen.
- PDF now shows material prices, work cost, and an ИТОГО total line.
- Projects / folders: jobs can be grouped into named projects, with a filter on the home screen and inline editing.
- Photos are now compressed harder (1600 px max, JPEG 80) and camera photos are compressed too.
- Job creation: date, address, client name and phone (Room storage).
- Job details screen: description (text or voice dictation), photos (gallery / camera), materials.
- Manual report screen: editable description and materials.
- PDF generation on device and sharing via system menu.
- Job history list on the home screen.

## [0.7.6] - 2026-08-13

### Added

- System notification when a new app version is released: checked on app launch, tapping it opens the app.

## [0.7.5] - 2026-08-13

### Fixed

- Home screen header now uses the Material 3 top app bar, so it no longer slides under the system status bar on phones with edge-to-edge displays.

## [0.7.4] - 2026-08-13

### Fixed

- PDF buttons no longer silently do nothing when the report has no description yet: a default report is used and the PDF is always generated.
- The description field is now optional: a PDF with date, address, client and photos is generated even when the work description is empty.

## [0.7.3] - 2026-08-13

### Fixed

- PDF is now actually written: saving no longer silently succeeds when the file could not be opened for writing (previously produced a 0-byte "invalid format" PDF).
- PDF generation is validated (header check and file size) before saving; the real error is shown if generation fails.
- Added `WRITE_EXTERNAL_STORAGE` (API <= 28) and external path for the PDF viewer.

### Added

- Main "Создать PDF" button: generates, auto-saves to Downloads and opens a preview.
- PDF preview via system viewer from the app cache.
- Auto-save to the Downloads folder via MediaStore on Android 10+.

## [0.7.2] - 2026-08-13

### Changed

- Removed the preset material catalog quick-select from the job screen (manual input remains).
- Renamed the copy button to "Создать копию работы".

## [0.7.1] - 2026-08-13

### Changed

- Update flow split into two explicit steps: "Download" then "Install" (no automatic install after download), reducing Play Protect concerns.

## [0.7.0] - 2026-08-13

### Added

- Job status: in progress / done / sent to client, switchable from the job screen.
- Photo captions: add or edit a caption under each photo, included in the PDF.
- "Copy as new job": duplicates the job with photos, materials and report.
- Material catalog: quick add from 28 common building materials.
- PDF header changed to "Акт выполненных работ" with status and photo captions.
- Save PDF to a file (in addition to sharing).
- Statistics on the home screen: counters by status.
- Local backup: export / import all data as a JSON file.

## [0.3.0] - 2026-08-12

### Added

- In-app update check: auto-check on launch and a button on the home screen.
- Download and install of the new APK from GitHub Releases.
