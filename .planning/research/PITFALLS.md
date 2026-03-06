# Domain Pitfalls: Android Background Photo Auto-Organization

**Domain:** Android background service + on-device ML photo classification  
**Researched:** 2025-03-06  
**Confidence:** LOW (Official docs unavailable for verification)

---

## Critical Pitfalls

Mistakes that cause data loss, app rejection, or fundamental rewrites.

### Pitfall 1: Service Killed by Doze/App Standby

**What goes wrong:** Background service stops running after device enters Doze mode or the app is unused for a few days. Photos pile up unsorted; users think the app is broken.

**Why it happens:** Android 6.0+ Doze mode defers background jobs for battery optimization. Foreground services need notifications. JobScheduler/WorkManager have delays. Battery optimization settings vary by OEM (Samsung, Xiaomi especially aggressive).

**Consequences:**
- Missed photos not sorted
- User uninstalls app
- Negative reviews about "stops working"

**Warning signs:**
- Service stops after device idle overnight
- Works on Pixel but fails on Samsung
- High "force stop" rate in Play Console

**Prevention strategy:**
1. Use **WorkManager** with expedited work (API 31+) for immediate processing
2. Register **MediaStore.ContentObserver** in foreground service with persistent notification
3. Request **REQUEST_IGNORE_BATTERY_OPTIMIZATIONS** (requires Play Console justification)
4. Provide **manual sync button** as fallback
5. Track last-run timestamp; alert user if stale >24h

**Phase to address:** Phase 1 (Architecture) — Wrong service type = rewrite required

---

### Pitfall 2: Photos Lost During Move Operation

**What goes wrong:** File move interrupted (crash, battery die, process killed), resulting in partial move, duplicate, or lost photo.

**Why it happens:** Android file operations aren't atomic. Move involves: copy to dest → verify → delete source. Any interruption mid-operation = data loss.

**Consequences:**
- User loses irreplaceable photos
- Permanent data loss (no Recycle Bin on Android by default)
- App uninstall + legal liability

**Warning signs:**
- User reports "photos disappeared"
- Files in destination folder incomplete/corrupt
- Logs show move started but never completed

**Prevention strategy:**
1. **Copy-then-delete pattern:** Copy to dest → verify checksum/size match → update MediaStore → delete source
2. **Transaction log:** SQLite log of pending operations; resume on restart
3. **Dry-run mode:** First phase copies without deleting, second phase cleans up after verification period
4. **Trash folder:** Move to hidden `.trash/` folder for 7 days before permanent delete
5. **Test interruptions:** Instrumented tests killing process mid-move

**Phase to address:** Phase 1 (Core Engine) — Data safety is foundational

---

### Pitfall 3: MediaStore Out of Sync

**What goes wrong:** Gallery app shows duplicates, missing thumbnails, or "file not found" errors after moving photos. Photos exist in filesystem but invisible to gallery apps.

**Why it happens:** MediaStore is the source of truth for gallery apps. Moving files via Java/Kotlin File API doesn't update MediaStore. Apps using old URIs get stale data.

**Consequences:**
- Ghost entries in gallery
- Third-party apps can't find photos
- User confusion about where photos went

**Warning signs:**
- Gallery shows duplicate entries for moved photos
- Photos visible in file manager but not gallery
- Third-party editors crash opening "moved" photos

**Prevention strategy:**
1. Always use **MediaStore API** (insert/update/delete) when moving files
2. Call **MediaScannerConnection.scanFile()** or use ContentResolver to notify system
3. For Android 10+: Use **Scoped Storage** MediaStore operations (SAF Intent for legacy locations)
4. Test with Google Photos, Samsung Gallery, Simple Gallery
5. Invalidate thumbnails after move

**Phase to address:** Phase 1 (File Operations)

---

### Pitfall 4: Scoped Storage Permission Denials

**What goes wrong:** App can't access Downloads/WhatsApp/DCIM folders on Android 10+ (API 29+), or requires broad storage permissions rejected by Play Store.

**Why it happens:** Android 10 introduced Scoped Storage. Android 11+ restricts access to external app directories. `MANAGE_EXTERNAL_STORAGE` permission requires Play Store declaration and justification.

**Consequences:**
- Can't monitor Downloads/WhatsApp for new photos
- Play Store rejection for `READ_EXTERNAL_STORAGE` alone
- App works on older Android but broken on new devices

**Warning signs:**
- `SecurityException` on file access
- Play Console pre-launch report warnings
- Works on Android 9, fails on Android 12

**Prevention strategy:**
1. Target API 33+ with proper **Scoped Storage** usage
2. Use **ACTION_OPEN_DOCUMENT_TREE** for user-selected directories (Downloads, WhatsApp)
3. For Downloads: Use **DownloadManager** query or MediaStore with `Downloads` collection
4. Request `MANAGE_EXTERNAL_STORAGE` only if justified; prepare Play Store appeal
5. Graceful degradation: Process only accessible directories, prompt user for others

**Phase to address:** Phase 1 (Permissions Architecture)

---

### Pitfall 5: On-Device ML Battery Drain

**What goes wrong:** Image classification consumes 10-20% battery per photo; users uninstall or Android force-stops app for excessive battery use.

**Why it happens:** TensorFlow Lite inference on CPU is expensive. Loading model repeatedly, processing full-resolution images, running on main thread.

**Consequences:**
- Play Console "excessive battery use" warnings
- Negative reviews
- Android kills app during classification

**Warning signs:**
- Battery usage >5% in system settings
- Phone heats up during classification
- ANR (Application Not Responding) dialogs

**Prevention strategy:**
1. **Model optimization:** Use quantization (INT8), pruning, or smaller MobileNet variant
2. **Downscale images:** Resize to 224x224 or 299x299 before inference; never process full 12MP
3. **NNAPI delegate:** Use hardware acceleration (GPU/NPU) when available
4. **Batch processing:** Process photos when charging or on WiFi; defer on low battery
5. **Background thread:** Strictly off main thread; use coroutines with Dispatchers.Default
6. **Benchmark:** Profile inference time; target <100ms per image on mid-range device

**Phase to address:** Phase 2 (ML Integration)

---

### Pitfall 6: Model Loading Memory Pressure

**What goes wrong:** App OOMs (Out of Memory) when loading ML model; crashes on devices with 2-3GB RAM.

**Why it happens:** TensorFlow Lite model + image tensors + Bitmap allocations exceed available memory. Leaked model references, processing multiple images concurrently.

**Consequences:**
- App crashes on low-end devices
- Poor Play Store "device stability" score
- User churn in emerging markets (cheaper devices)

**Warning signs:**
- `OutOfMemoryError` in Crashlytics
- Crashes clustered on low-RAM devices (<4GB)
- Increasing memory usage across classification sessions

**Prevention strategy:**
1. **Single interpreter instance:** Reuse Interpreter across sessions; don't load per-image
2. **Memory-mapped model:** Use `Interpreter.Options().setUseNNAPI(false)` with mapped file
3. **Bitmap recycling:** Call `bitmap.recycle()` after inference; don't retain references
4. **Concurrent limit:** Process one image at a time; queue others
5. **Heap monitoring:** Cancel low-confidence requests if memory pressure detected
6. **Test on low-end:** Use Android Studio emulator with 2GB RAM limit

**Phase to address:** Phase 2 (ML Integration)

---

## Moderate Pitfalls

### Pitfall 7: Notification Fatigue

**What goes wrong:** Low-confidence classification prompts too often; user disables notifications or uninstalls.

**Why it happens:** Model uncertainty on edge cases (screenshot vs document, meme vs artwork). Threshold too low. No batching of decisions.

**Prevention:**
1. **Smart thresholding:** 0.7+ confidence = auto-sort; 0.4-0.7 = queue for batch review; <0.4 = "Other" folder
2. **Batched notifications:** One daily summary of ambiguous photos, not per-photo
3. **Learn from corrections:** Update model or local rules when user overrides

**Phase to address:** Phase 3 (User Experience)

---

### Pitfall 8: Category Explosion

**What goes wrong:** User creates 50+ custom categories; classification accuracy drops; UI becomes unusable.

**Why it happens:** Model trained on N classes struggles with N+50. Category confusion ("pets" vs "dogs" vs "animals").

**Prevention:**
1. **Hierarchical categories:** Top-level (10) → sub-categories (user-defined)
2. **Onboarding:** Start with 5-10 categories; allow adding later
3. **Category similarity detection:** Warn if new category overlaps existing

**Phase to address:** Phase 3 (Customization)

---

### Pitfall 9: Duplicate Processing

**What goes wrong:** Same photo processed multiple times, moved to wrong folders, or creating duplicates.

**Why it happens:** MediaStore observer fires multiple times for same URI. No deduplication of processing queue.

**Prevention:**
1. **Content hash:** SHA-256 of file content; skip if already processed
2. **In-flight tracking:** Set of URIs currently being processed
3. **Processed cache:** SQLite table of (file_hash, destination_folder, timestamp)

**Phase to address:** Phase 1 (Core Engine)

---

## Minor Pitfalls

### Pitfall 10: Wrong File Type Processing

**What goes wrong:** Attempting to classify videos, gifs, or raw files as static images.

**Prevention:** Filter by MIME type (`image/jpeg`, `image/png`, `image/webp`) and file extension before processing.

**Phase to address:** Phase 1 (Core Engine)

---

### Pitfall 11: Storage Full During Move

**What goes wrong:** Copy succeeds, delete fails due to no space for destination; partial state.

**Prevention:** Check available storage > 2x file size before copy operation. Handle `IOException` gracefully.

**Phase to address:** Phase 1 (File Operations)

---

## Phase-Specific Pitfalls Map

| Phase | Likely Pitfall | Mitigation Strategy |
|-------|---------------|---------------------|
| **Phase 1: Foundation** | Service killed (Doze) | WorkManager + expedited work + foreground service hybrid |
| **Phase 1: Foundation** | Photo loss during move | Transaction log + copy-verify-delete + trash folder |
| **Phase 1: Foundation** | MediaStore desync | Use ContentResolver operations; scanFile after move |
| **Phase 1: Foundation** | Scoped storage blocks access | SAF directory picker + graceful degradation |
| **Phase 2: ML** | Battery drain | Quantized model + downscale + hardware delegate |
| **Phase 2: ML** | OOM crashes | Single interpreter + bitmap recycling + memory limits |
| **Phase 3: UX** | Notification spam | Smart thresholds + batched daily review |
| **Phase 3: UX** | Category chaos | Hierarchical + gradual onboarding |
| **Phase 4: Polish** | Edge case crashes | Comprehensive file type filtering + error boundaries |

---

## Confidence Assessment

| Pitfall | Confidence | Reason |
|---------|------------|--------|
| Doze/Service killing | MEDIUM | Well-documented Android behavior, patterns established |
| Data loss during move | HIGH | Universal file system behavior, transaction pattern standard |
| MediaStore sync | MEDIUM | Scoped Storage evolution; behavior may vary by Android version |
| Scoped Storage | MEDIUM | Play Store policies change; verify current requirements |
| ML battery drain | MEDIUM | TensorFlow Lite docs confirm quantization benefits |
| OOM with ML | HIGH | Memory management principles universal |

---

## Open Questions (Need Phase-Specific Research)

1. **Android 14+ restrictions:** Have foreground service rules changed? Verify current `FOREGROUND_SERVICE` permission requirements.

2. **Photo Picker API:** Does Android 13+ Photo Picker change how we should access photos? Does it obsolete ContentObserver?

3. **TensorFlow Lite Task Library:** Are `ImageClassifier` APIs (vs raw Interpreter) production-ready? Performance comparison?

4. **OEM-specific behaviors:** Xiaomi, Oppo, Vivo background killing policies vary. Need device-specific testing matrix.

5. **Play Store policy:** Current stance on `MANAGE_EXTERNAL_STORAGE` for photo organizer apps? Approval likelihood?

6. **ML model size vs accuracy tradeoff:** Benchmark MobileNet variants (v3-Small, EfficientNet-Lite0) on mid-range devices.

---

## Sources

- Android Background Execution Limits (training data, verify with official docs)
- TensorFlow Lite Performance Best Practices (training data, verify with tflite.dev)
- Android Storage Access Framework documentation (training data)
- Play Store Policy on Sensitive Permissions (training data, verify current policy)

**Note:** Official Android documentation URLs returned errors during research. Recommend verifying current API behavior (Android 14/15) during Phase 1 implementation. Consider this document a starting hypothesis, not authoritative.
