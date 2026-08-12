# Otchet-Master

Mobile app for independent repair and finishing specialists (Russia/CIS). A specialist takes photos, describes the job by voice or text, and the app turns it into a professional structured report and PDF — offline-first, in minutes.

> MVP: Android app for a repair/finishing master. Generate a professional report after a completed job and send it to the client as PDF.

## Features (MVP)

- **Job capture**: date, address, client (name/phone), required photos, description (voice or text), materials
- **Offline-first**: all data is stored locally (Room) and synced to the cloud when the network is available
- **AI report generation**: from the text description only (photos are never sent to AI) into structured JSON: `work_performed`, `materials`, `notes`
- **Manual mode**: create a report without AI when there is no network
- **Editing**: the user reviews and edits the AI result before generating the PDF
- **PDF**: generated on the device, sent to the client
- **Job history**: stored locally and backed up to the cloud

Out of MVP scope: invoices, client database, warranty block, photo captions, analytics, monetization.

## Tech stack

- **Mobile**: Android, Kotlin, Jetpack Compose
- **Local DB**: Room (source of truth, works offline)
- **Backend**: Supabase (Postgres + Storage + Auth)
- **AI**: edge function `generate-report` with an abstract AI provider interface (provider to be selected by test)
- **Speech recognition**: on-device (Android SpeechRecognizer)
- **PDF**: generated on the device from structured JSON

## Repository structure

```
android/                 # Android application
supabase/                # Supabase project (edge functions)
docs/                    # Documentation (in Russian)
├── product.md           # Product definition and decisions
├── requirements.md      # Functional and non-functional requirements
├── architecture.md      # Architecture overview
├── database.md          # Data model
├── api.md               # API description
├── ai.md                # AI pipeline and provider abstraction
├── security.md          # Security decisions
├── deployment.md        # Deployment guide
└── decisions/           # Architecture Decision Records (ADR)
```

## Getting started

### Prerequisites

- JDK 17+
- Android Studio (latest stable)
- Android SDK (API level 33+)
- A Supabase project (cloud or self-hosted)

### Setup

1. Clone the repository:

```bash
git clone https://github.com/your-org/otchet-master.git
cd otchet-master
```

2. Copy the environment example and fill in your Supabase credentials:

```bash
cp .env.example .env
```

3. Open `android/` in Android Studio, wait for Gradle sync, and run on a device or emulator.

### Edge functions

See `supabase/README.md` for local development and deployment of edge functions.

## Git workflow

- `main` — stable, production-ready
- `develop` — integration branch
- `feature/*` — new features
- `fix/*` — bug fixes

## Releases

Releases are created by tagging `vX.Y.Z` on `main`. GitHub Actions builds a signed APK and attaches it to a GitHub Release for the test group. Details: [docs/deployment.md](docs/deployment.md).

## License

[MIT](LICENSE)
