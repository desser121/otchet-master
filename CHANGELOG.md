# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Job creation: date, address, client name and phone (Room storage).
- Job details screen: description (text or voice dictation), photos (gallery / camera), materials.
- Manual report screen: editable description and materials.
- PDF generation on device and sharing via system menu.
- Job history list on the home screen.

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
