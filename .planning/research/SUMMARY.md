# Research Summary: Android Photo Auto-Organization Agent

**Project:** Photo-Classificator  
**Research Date:** March 2026  
**Confidence:** HIGH

---

## 1. Executive Summary

This research establishes a clear technical path for building an Android background agent that automatically organizes photos using on-device machine learning. The core value proposition is **zero-touch operation**: the app works silently in the background, only surfacing when classification confidence is low.

The recommended architecture follows a **Foreground Service + WorkManager + ContentObserver** pattern. A persistent foreground service maintains a ContentObserver watching MediaStore for new photos, while battery-heavy operations (ML inference, file moves) are delegated to WorkManager with proper constraints. This approach balances responsiveness with battery efficiency while respecting Android's background execution restrictions.

For ML classification, **ML Kit Image Labeling** is the clear choice. It provides 400+ labels covering receipts, documents, food, pets, screenshots, and more—all running entirely on-device with confidence scores. This eliminates cloud dependencies, privacy concerns, and network latency. The bundled model adds ~5.7MB to APK size, with a Play Services option (~200KB) available for size optimization.

The most critical risk is **data safety during file operations**. Moving photos carries inherent risks: interrupted operations can cause data loss, MediaStore can become out of sync with the filesystem, and scoped storage permissions may block access to key directories (Downloads, WhatsApp). Mitigation requires a transaction-log approach (copy → verify → delete), MediaStore API usage for all operations, and Storage Access Framework (SAF) for directory access on Android 10+.

---

## 2. Key Findings

### Stack Recommendation

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Language | Kotlin 2.3.10+ | Primary language with coroutines |
| ML Engine | ML Kit Image Labeling 17.0.9 | On-device classification (400+ labels) |
| Background | WorkManager 2.9.1+ | Battery-aware job scheduling |
| Monitoring | MediaStore ContentObserver | Event-driven photo detection |
| Service | Foreground Service | Persistent monitoring |
| Database | Room 2.6.1+ | Photo metadata and queue |
| Preferences | DataStore 1.1.0+ | User settings |
| Files | Storage Access Framework | Scoped storage compliance |

**Key decision:** ML Kit over TensorFlow Lite for initial release. ML Kit covers 80%+ of use cases (receipts, food, documents, screenshots, pets) without model management overhead. Custom TFLite reserved for v2 if domain-specific categories are needed.

### Architecture Pattern

The system is **event-driven** rather than polling-based:

1. **Foreground Service** maintains ContentObserver on MediaStore (always-on)
2. **ContentObserver** detects new photos via system broadcasts (zero battery drain)
3. **WorkManager** handles ML inference with battery constraints (Doze-aware)
4. **Classification Logic** maps ML labels to user categories with configurable thresholds
5. **SAF/MediaStore** moves files while maintaining database consistency

Critical boundary: Service NEVER does ML inference—always delegates to WorkManager to avoid ANRs and battery drain.

### Critical Pitfalls

Five pitfalls require architecture-level mitigation:

| Pitfall | Risk | Mitigation |
|---------|------|------------|
| **Service killed by Doze** | Missed photos, app feels broken | WorkManager expedited work + persistent notification + battery optimization exemption request |
| **Photo loss during move** | Permanent data loss | Copy → verify → delete pattern + transaction log + 7-day trash folder |
| **MediaStore desync** | Ghost entries, missing thumbnails | Always use MediaStore API + ContentResolver + scanFile after operations |
| **Scoped storage blocks** | Can't access Downloads/WhatsApp | SAF directory picker + graceful degradation + Play Store justification |
| **ML battery drain** | 10-20% per photo, app killed | Downscale to 224x224 + NNAPI delegate + batch processing when charging |

**Most critical:** Data loss prevention. A failed file move on a user's irreplaceable photo is catastrophic. The transaction-log pattern must be implemented in Phase 1, not retrofitted.

---

## 3. Implications for Roadmap

### Phase 1: Foundation (Weeks 1-3)
**Goal:** Risk mitigation and core pipeline

Components:
- Room database (PhotoRecord, Category entities)
- DataStore preferences (thresholds, categories)
- SAF onboarding flow (directory permissions)
- Transaction-log file operations (copy-verify-delete)
- Test harness for interruption scenarios

**Rationale:** Phase 1 addresses the highest-risk pitfalls (data loss, permissions). Without a safe file operation foundation, subsequent phases are built on sand. The transaction log and verification system must be production-ready before handling real user photos.

### Phase 2: Monitoring & ML (Weeks 4-6)
**Goal:** Photo detection and classification

Components:
- Foreground Service with persistent notification
- MediaStore ContentObserver
- WorkManager workers with battery constraints
- ML Kit integration (bundled model)
- Classification logic with confidence thresholds

**Rationale:** Building on Phase 1's safe file operations, this phase implements the core value proposition. The WorkManager + ML Kit combination provides production-ready classification with automatic battery optimization. Confidence thresholds (default 0.7) determine auto-sort vs. low-confidence queue.

### Phase 3: Polish (Weeks 7-8)
**Goal:** UX refinement and edge cases

Components:
- Batch notification for low-confidence photos (daily summary, not per-photo)
- Duplicate detection (SHA-256 hash)
- Storage full handling
- Category management UI
- Hierarchical categories (10 max top-level)

**Rationale:** Phase 3 moves from "works" to "delights." Batched notifications prevent notification fatigue. Duplicate detection avoids reprocessing. The 10-category limit prevents category explosion that degrades accuracy.

### Phase 4: Enhancement (Post-MVP)
**Goal:** Advanced features based on user feedback

Components:
- Custom TensorFlow Lite models (if ML Kit insufficient)
- Batch processing for existing photo libraries
- Category learning from user corrections
- Receipt/document OCR with ML Kit Text Recognition
- Play Services model distribution (APK size optimization)

**Rationale:** These features expand capability but depend on v1 data and feedback. Custom models require training data; category learning requires baseline behavior; OCR builds on the classification pipeline.

---

## 4. Confidence Assessment

| Area | Confidence | Reasoning |
|------|------------|-----------|
| **Stack Selection** | HIGH | Official Google recommendations, verified Maven versions, production usage |
| **Architecture Pattern** | HIGH | Established Android patterns, documented in official guides |
| **ML Kit Capability** | HIGH | 400+ labels covers project requirements; confidence scores provided |
| **Data Safety** | MEDIUM-HIGH | Transaction patterns are standard; Android 14+ behavior needs verification |
| **Battery Efficiency** | HIGH | WorkManager is Android's standard for battery-aware work |
| **Scoped Storage** | MEDIUM | SAF approach is correct; Play Store policy interpretation carries risk |

**Overall Confidence:** HIGH for core architecture, MEDIUM for edge cases (OEM-specific Doze behavior, Play Store approval).

---

## 5. Gaps to Address

### Before Phase 1 Implementation

1. **Android 14/15 Verification:** Verify current foreground service type requirements (`dataSync` vs `mediaPlayback`)
2. **Play Store Policy:** Confirm `MANAGE_EXTERNAL_STORAGE` approval likelihood for photo organizer category
3. **OEM Testing Matrix:** Define minimum device set (Pixel, Samsung, Xiaomi) for Doze behavior testing

### During Phase 1

4. **MediaStore Reliability:** Validate ContentObserver behavior on Android 10-15 across target directories (DCIM, Downloads, WhatsApp)
5. **SAF UX Flow:** Design directory picker onboarding that explains why access is needed (prevents user abandonment)

### During Phase 2

6. **ML Performance Baseline:** Benchmark inference time on mid-range device (target: <100ms per image)
7. **Battery Impact Measurement:** Profile ML battery consumption with/without NNAPI delegate

### Before Production

8. **Device Coverage:** Test on 2GB RAM devices to validate OOM prevention strategies
9. **Interruption Testing:** Instrumented tests killing process mid-file-move
10. **Play Store Pre-Launch:** Pre-launch report validation for scoped storage compliance

---

## Research Sources

All findings based on:
- ML Kit official documentation (developers.google.com/ml-kit)
- Android Architecture Components (WorkManager, Room, DataStore)
- Android Storage Access Framework documentation
- Maven repository version verification (current as of March 2026)

**Document Generated:** March 6, 2026  
**Next Review:** Before Phase 1 implementation
