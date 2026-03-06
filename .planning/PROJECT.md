# Android Photo Auto-Organizer

## What This Is

An always-running background agent for Android devices that automatically organizes every new photo as soon as it appears. The agent analyzes image content using on-device ML, classifies photos into user-defined categories (receipts, memes, food, selfies, screenshots, documents, landscapes, pets, products, artwork, etc.), and moves them from default folders (Downloads, DCIM/WhatsApp) into existing gallery folders that the user has already created within /Pictures/. The system operates silently without user interaction in normal cases, only notifying when classification confidence is low. The agent works with the user's existing folder structure — it doesn't create new gallery folders but organizes photos into folders the user has already set up.

## Core Value

Users never have to manually organize photos again — every image is automatically sorted into the right folder immediately upon arrival, keeping the gallery clean and organized without effort.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] Monitor file system for new images in default locations
- [ ] Analyze image content using on-device ML classification
- [ ] Classify images into user-defined categories
- [ ] Move classified images to appropriate user-defined folders
- [ ] Remove source files from default locations after successful sorting
- [ ] Run silently in background without user interaction
- [ ] Notify user only on low-confidence classifications requiring choice
- [ ] Support fully customizable categories and target folders
- [ ] Handle WhatsApp downloaded images
- [ ] Handle camera-captured photos
- [ ] Handle images from other apps and sources
- [ ] Support multiple image formats (JPEG, PNG, WebP, etc.)

### Out of Scope

- Cloud-based classification — must run entirely on device for privacy and offline operation
- Manual photo organization UI — the agent should be fully automatic
- Image editing or enhancement — classification and moving only
- Backup or sync functionality — focus purely on organization
- iOS support — Android-only v1
- Web interface for management — native Android UI only

## Context

### Technical Environment

- Android platform (minimum version TBD)
- Must run as background service
- On-device ML for privacy and offline operation
- File system access to Pictures/, DCIM/, Downloads/ directories
- MediaStore API integration for gallery management

### User Scenarios

1. **WhatsApp user**: Receives photos via WhatsApp, they auto-save to Downloads/WhatsApp Images, agent moves them to appropriate folders
2. **Photographer**: Takes photos with camera, they save to DCIM/Camera, agent organizes by content type
3. **Screenshot user**: Captures screenshots, agent detects and moves to Screenshots folder
4. **Receipt collector**: Takes photos of receipts for expense tracking, agent recognizes and files in Receipts folder

### Constraints

- **Privacy**: All classification must happen on-device — no cloud processing
- **Performance**: Minimal battery impact, efficient background operation
- **Storage**: Handle large image libraries without excessive storage overhead
- **Battery**: Background service must be battery-efficient
- **Permissions**: Requires storage permissions, possibly background execution permissions

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| On-device ML only | Privacy is paramount; users don't want photos sent to cloud | — Pending |
| Silent operation default | Normal case should require zero user interaction | — Pending |
| User-defined categories | Different users have different organizational needs | — Pending |
| Android-only v1 | Focus on one platform for initial release | — Pending |

---
*Last updated: 2025-03-06 after initialization*
