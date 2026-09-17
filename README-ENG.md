# PacheteScan

**PacheteScan** is an Android companion app for a courier/parcel-tracking platform. It lets warehouse and delivery staff scan a parcel's barcode, pull up its full history, and log damage reports or missing-information notices directly from the field — photos included.

The app is built around **Honeywell AIDC-enabled scanners** (via the Honeywell AIDC SDK) for fast, hardware-triggered barcode reads, but it also runs on any standard Android device. On non-Honeywell hardware, barcode capture falls back to manual AWB entry instead of hardware scanning, so the app remains usable — just without the dedicated scan trigger and continuous-read performance of Honeywell terminals.

## What it does

- **Scan or type a parcel code** to pull up everything known about it in one screen: package details, weight, addresses, and the full list of damage reports and missing-information notices tied to it, each with any attached photos.
- **Log a damage report** for a scanned parcel, with an unlimited queue of photos attached before submitting — take as many as needed, review small previews, remove any by long-press, then submit once.
- **Log a missing-information notice** (e.g. an unreadable or missing AWB label), with a single required photo of the label/issue.
- **Attach photos reliably**: every photo is queued locally first, uploaded after the parent record is created, and automatically retried a few times on transient network failures before asking the user to intervene.
- **Browse photos as thumbnails** everywhere they appear, with full-resolution view on tap — thumbnails are generated server-side so the app never has to pull full images just to show a preview grid.
- **Sign in once**: a JWT-based login persists across sessions, with the token attached automatically to every API call.

## Architecture

The app follows a consistent, hand-rolled layered pattern across every feature (auth, packages, damage reports, missing-info notices, image uploads) rather than relying on a networking or DI library:

```
Activity  →  ViewModel  →  Repository  →  DataSource  →  REST API
                ↑                              |
             LiveData  ←──────────────── Result<T> (Success / Error)
```

- **DataSource** — one class per feature, talks to the backend directly over `HttpURLConnection` (including hand-built `multipart/form-data` for photo uploads), parses JSON responses with `org.json`, and maps HTTP status codes to typed results.
- **Repository** — owns a background `ExecutorService`, hands work off the UI thread, and posts results back via a `Handler` on the main looper. Implemented as thread-safe singletons.
- **ViewModel** — exposes `LiveData<Result>` to the UI and mediates between Activities and Repositories; some flows also expose a direct callback path for sequential batch operations (e.g. uploading a queue of photos one at a time with retry).
- **Activity** — plain Android Views (no Compose), CameraX for photo capture, and the Honeywell AIDC SDK for barcode events where available.

No JSON library, HTTP client, or DI framework is used — everything is written against the Android/Java standard library plus the Honeywell SDK, to keep the runtime footprint small on handheld scanner hardware.

## Key modules

| Package | Responsibility |
|---|---|
| `data.*` | Per-feature `DataSource` + `Repository` + request/response models |
| `ui.*` | Per-feature `ViewModel`, `Result`, and `ViewModelFactory` |
| Activities (root package) | Screens: login, menu, package lookup, damage report, missing-info report, photo capture |
| `ui.imagini` | Shared photo pipeline: capture handoff, retry-queue uploader, thumbnail loading |

## Photo pipeline

Photo capture is decoupled from upload:

1. A dedicated capture screen takes a picture with CameraX and hands the local file back to the calling screen — it doesn't upload anything itself.
2. The calling screen (damage report / missing-info) queues the file locally, shows an efficiently-decoded local thumbnail (downsampled, never loading a full-resolution bitmap just for a preview), and lets the user remove queued photos before submitting.
3. Once the parent record (damage report / missing-info notice) is created successfully, queued photos upload sequentially, each with a few silent retries on failure before surfacing a partial-failure dialog with a manual retry option.
4. Server-generated thumbnails (a small, separate resized copy created at upload time) are used everywhere photos are listed, keeping data usage low; the full-resolution original is only fetched on demand.

## Backend

PacheteScan talks to a companion ASP.NET Core Web API (shared with the platform's web interface) over JWT-authenticated REST endpoints for authentication, package lookup, damage/missing-info creation, and image upload/retrieval.

## Requirements

- Android device or Honeywell AIDC-compatible scanner running a supported Android version
- Network access to the backend API
- Camera permission (for photo capture)
- For full barcode-scan functionality: a Honeywell scanner with the AIDC service available

## Status

Actively developed alongside the backend platform. Some planned functionality (e.g. offline photo queuing with background retry via WorkManager) is still in design.
