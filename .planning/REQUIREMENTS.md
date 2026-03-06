# Requirements: Android Photo Auto-Organizer

**Defined:** 2025-03-06
**Core Value:** Users never have to manually organize photos again — every image is automatically sorted into the right folder immediately upon arrival

---

## v1 Requirements

### Background Service & Monitoring

- [ ] **MON-01**: Foreground service maintains persistent notification while running
- [ ] **MON-02**: MediaStore ContentObserver watches for new photos in DCIM/Camera, Downloads, and WhatsApp directories
- [ ] **MON-03**: Service restarts automatically after device reboot
- [ ] **MON-04**: Service handles Doze mode and battery optimization gracefully
- [ ] **MON-05**: WorkManager schedules ML inference jobs with battery constraints

### ML Classification

- [ ] **ML-01**: ML Kit Image Labeling analyzes photos on-device with confidence scores
- [ ] **ML-02**: Classification maps ML labels to user-defined categories
- [ ] **ML-03**: Confidence threshold (default 0.7) determines auto-sort vs manual review
- [ ] **ML-04**: Low-confidence photos queue for user decision via notification
- [ ] **ML-05**: Photos downscaled to 224x224 before classification for performance

### File Operations

- [ ] **FILE-01**: Storage Access Framework obtains permissions for user-selected target folders
- [ ] **FILE-02**: Copy-then-verify-then-delete pattern prevents data loss
- [ ] **FILE-03**: Transaction log tracks pending operations for crash recovery
- [ ] **FILE-04**: MediaStore API updates database after file moves
- [ ] **FILE-05**: 7-day trash folder holds deleted photos before permanent removal
- [ ] **FILE-06**: Duplicate detection via SHA-256 hash prevents reprocessing

### Categories & Configuration

- [ ] **CAT-01**: Users define categories with target folders (existing folders in Pictures/)
- [ ] **CAT-02**: Maximum 10 categories to prevent accuracy degradation
- [ ] **CAT-03**: Categories map to ML Kit labels with customizable confidence thresholds
- [ ] **CAT-04**: Hierarchical categories supported (e.g., Documents/Receipts)
- [ ] **CAT-05**: Configuration persists via DataStore preferences

### Notifications

- [ ] **NOTE-01**: Daily summary notification for low-confidence photos (not per-photo)
- [ ] **NOTE-02**: Notification shows photo thumbnail and suggested categories
- [ ] **NOTE-03**: User can approve, reject, or reclassify from notification
- [ ] **NOTE-04**: Silent operation by default; notification only on uncertainty

### Image Format Support

- [ ] **IMG-01**: JPEG files (.jpg, .jpeg)
- [ ] **IMG-02**: PNG files (.png)
- [ ] **IMG-03**: WebP files (.webp)
- [ ] **IMG-04**: HEIC/HEIF files (Android 9+ only)
- [ ] **IMG-05**: Unsupported formats skipped with log entry

### Data & State Management

- [ ] **DATA-01**: Room database stores photo metadata and processing history
- [ ] **DATA-02**: Processed photos tracked to prevent duplicate processing
- [ ] **DATA-03**: Processing queue handles photos discovered while offline
- [ ] **DATA-04**: Storage full detection prevents failed operations
- [ ] **DATA-05**: Configuration backup/restore via Android backup service

### Error Handling & Recovery

- [ ] **ERR-01**: Failed file operations retry with exponential backoff
- [ ] **ERR-02**: Corrupt or unreadable images skipped gracefully
- [ ] **ERR-03**: Permission denials trigger re-onboarding flow
- [ ] **ERR-04**: Transaction log enables recovery after app/process crash
- [ ] **ERR-05**: Manual sync button triggers reprocessing of missed photos

---

## v2 Requirements (Deferred)

### Advanced Features

- **ML-CUSTOM-01**: Custom TensorFlow Lite models for domain-specific categories
- **BATCH-01**: One-time batch processing of existing photo libraries
- **LEARN-01**: Category learning from user corrections (feedback loop)
- **OCR-01**: Receipt/document OCR with ML Kit Text Recognition
- **APK-01**: Play Services model distribution for smaller APK size

### Enhanced UX

- **UX-01**: Category management UI with drag-and-drop
- **UX-02**: Processing statistics and insights dashboard
- **UX-03**: Widget showing recent organization activity

---

## Out of Scope

| Feature | Reason |
|---------|--------|
| Cloud-based classification | Privacy requirement; must run entirely on-device |
| Manual photo organization UI | Violates "automatic" core value; use system Gallery |
| Image editing/enhancement | Scope creep; not core to organization |
| Backup or sync functionality | Different problem space; integrate with existing solutions |
| iOS support | Android-only v1; doubles complexity |
| Web interface | Native Android app only |
| Real-time camera classification | Battery drain; process after save only |
| Face recognition/people grouping | Privacy-sensitive; requires biometric permissions |
| Complex rule engine | ML-based over rule-based for "just works" experience |
| Gallery replacement | Work alongside existing gallery apps |

---

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| MON-01 | Phase 2 | Pending |
| MON-02 | Phase 2 | Pending |
| MON-03 | Phase 2 | Pending |
| MON-04 | Phase 2 | Pending |
| MON-05 | Phase 2 | Pending |
| ML-01 | Phase 2 | Pending |
| ML-02 | Phase 2 | Pending |
| ML-03 | Phase 2 | Pending |
| ML-04 | Phase 2 | Pending |
| ML-05 | Phase 2 | Pending |
| FILE-01 | Phase 1 | Pending |
| FILE-02 | Phase 1 | Pending |
| FILE-03 | Phase 1 | Pending |
| FILE-04 | Phase 2 | Pending |
| FILE-05 | Phase 1 | Pending |
| FILE-06 | Phase 3 | Pending |
| CAT-01 | Phase 1 | Pending |
| CAT-02 | Phase 1 | Pending |
| CAT-03 | Phase 2 | Pending |
| CAT-04 | Phase 2 | Pending |
| CAT-05 | Phase 1 | Pending |
| NOTE-01 | Phase 3 | Pending |
| NOTE-02 | Phase 3 | Pending |
| NOTE-03 | Phase 3 | Pending |
| NOTE-04 | Phase 3 | Pending |
| IMG-01 | Phase 2 | Pending |
| IMG-02 | Phase 2 | Pending |
| IMG-03 | Phase 2 | Pending |
| IMG-04 | Phase 2 | Pending |
| IMG-05 | Phase 2 | Pending |
| DATA-01 | Phase 1 | Pending |
| DATA-02 | Phase 1 | Pending |
| DATA-03 | Phase 1 | Pending |
| DATA-04 | Phase 1 | Pending |
| DATA-05 | Phase 3 | Pending |
| ERR-01 | Phase 1 | Pending |
| ERR-02 | Phase 2 | Pending |
| ERR-03 | Phase 1 | Pending |
| ERR-04 | Phase 1 | Pending |
| ERR-05 | Phase 3 | Pending |

**Coverage:**
- v1 requirements: 35 total
- Mapped to phases: 35 (100% coverage)
- Unmapped: 0

### Phase Distribution

| Phase | Name | Requirement Count | Percentage |
|-------|------|-------------------|------------|
| Phase 1 | Foundation & Data Safety | 11 | 31% |
| Phase 2 | Detection & Classification Pipeline | 14 | 40% |
| Phase 3 | Notifications & Polish | 7 | 20% |
| Phase 4 | Release Preparation | 3 | 9% |

*See [ROADMAP.md](ROADMAP.md) for detailed phase definitions and success criteria.*

---

*Requirements defined: 2025-03-06*
*Last updated: 2025-03-06 after research synthesis*
