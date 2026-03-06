# Feature Landscape: Android Photo Auto-Organization

**Domain:** Android background agent for automatic photo classification and organization  
**Researched:** 2026-03-06  
**Confidence:** HIGH (based on Android ecosystem knowledge and Google ML Kit documentation)

---

## Table Stakes

Features users expect. Missing = product feels incomplete.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| **Background monitoring** | Core to value proposition - app must "just work" | High | Requires foreground service, WorkManager, or JobScheduler; battery optimization handling critical |
| **Basic image classification** | Must understand what's in photos | Medium | ML Kit Image Labeling API provides 400+ labels out-of-the-box |
| **File system operations** | Moving files is the primary action | Medium | Requires Storage Access Framework (SAF) on Android 10+, legacy permissions on older versions |
| **Multiple image formats** | Users have JPEG, PNG, WebP, HEIC | Low | Android's BitmapFactory handles most formats; HEIC needs Android 9+ |
| **Configurable target folders** | Users have different organizational needs | Low | Simple preference storage; create folders in /Pictures/ |
| **Source folder monitoring** | Must watch Downloads, DCIM, WhatsApp | Medium | FileObserver or MediaStore ContentObserver |
| **Notification on low confidence** | User needs to know when manual intervention required | Low | Standard Android notification system |

---

## Differentiators

Features that set product apart. Not expected, but valued.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **Zero-touch operation** | Silent background operation with no UI in normal case | High | Requires careful service design to avoid ANRs and battery drain |
| **Custom ML models** | Domain-specific classification (receipts, memes, screenshots) | High | Requires TensorFlow Lite custom model training; receipts/screenshots have visual patterns distinguishable from generic labels |
| **Smart duplicate detection** | Avoid organizing same image twice | Medium | Hash-based (MD5) or perceptual hashing (pHash) for near-duplicates |
| **Confidence threshold tuning** | User control over when to notify | Low | Slider preference; affects balance of automation vs accuracy |
| **Category learning** | Improve classification based on user corrections | High | Requires on-device model fine-tuning or feedback loop |
| **Batch processing** | Handle existing photo libraries, not just new ones | Medium | One-time scan + continuous monitoring mode |
| **Privacy-first (on-device only)** | No cloud processing, no data leaves device | Medium | ML Kit runs on-device; custom TFLite models also local |
| **WhatsApp-specific handling** | Knows about WhatsApp Image subfolders | Low | Specific path monitoring for /Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/ |
| **Screenshot detection** | Distinguish screenshots from photos | Medium | Metadata analysis (EXIF) + visual patterns + file naming conventions |
| **Receipt/document OCR** | Extract text from receipts for expense tracking | High | ML Kit Text Recognition + custom heuristics for receipt structure |

---

## Anti-Features

Features to explicitly NOT build.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| **Manual photo organization UI** | Violates "automatic" core value; users don't want another gallery app | Let system Gallery/Photos handle manual organization; focus on silent automation |
| **Cloud-based classification** | Privacy risk, requires internet, adds latency | Keep all ML on-device using ML Kit or TensorFlow Lite |
| **Image editing/enhancement** | Scope creep; many apps do this well | Stay focused on classification and file movement only |
| **Backup or sync functionality** | Complex infrastructure, different problem space | Integrate with existing backup solutions (Google Photos, etc.) |
| **Social sharing features** | Not relevant to organization use case | Let users share from their organized folders using system share sheet |
| **Face recognition/people grouping** | Privacy-sensitive, complex, requires biometric permissions | Use ML Kit face detection only if needed; avoid identifying individuals |
| **Complex rule engine** | Users don't want to write rules; they want it to "just work" | Smart defaults with minimal configuration; ML-based classification over rule-based |
| **Real-time camera classification** | Battery drain, not needed for post-capture organization | Process after save is complete, not during capture |
| **Gallery replacement** | Users already have preferred gallery apps | Work alongside existing gallery, not replace it |
| **Cross-platform (iOS) in v1** | Doubles complexity, different permissions models | Focus on Android first; prove concept before expanding |

---

## Feature Dependencies

```
Background monitoring → File system operations (can't move without monitoring)
Basic image classification → Notification on low confidence (need confidence scores)
Custom ML models → Category learning (custom models enable user feedback loops)
Source folder monitoring → Smart duplicate detection (need to track what's been processed)
Receipt/document OCR → Custom ML models (OCR is enhanced by domain-specific training)
Zero-touch operation → Background monitoring + Basic classification (silent requires reliable core)
```

---

## MVP Recommendation

**Prioritize for v1:**

1. **Background monitoring** (table stakes) - Core to the product existing
2. **Basic image classification** (table stakes) - ML Kit Image Labeling gets us 80% there
3. **File system operations** (table stakes) - Actually move the files
4. **Zero-touch operation** (differentiator) - This is the key differentiator; do it right
5. **WhatsApp-specific handling** (differentiator) - Low effort, high user value given common use case

**Defer to v2:**

- **Custom ML models**: Requires training data and model development; ML Kit generic labels cover most cases initially
- **Category learning**: Needs v1 user data to train on; feedback loop requires baseline working
- **Receipt/document OCR**: Can use ML Kit Text Recognition as v2 enhancement
- **Batch processing**: Focus on real-time first; backfill can come later

**Never build:**

- Manual photo organization UI (anti-feature)
- Cloud-based classification (anti-feature)
- Image editing (anti-feature)

---

## Category-Specific Implementation Notes

Based on the project's target categories (receipts, memes, food, selfies, screenshots, documents, landscapes, pets, products, artwork):

| Category | Detection Approach | Confidence |
|----------|-------------------|------------|
| **Receipts** | ML Kit Text Recognition + heuristics (contains "$", numbers, store names) | MEDIUM - custom model would help |
| **Memes** | Generic image labeling ("text", "poster") + aspect ratio + has text | MEDIUM |
| **Food** | ML Kit has "Food" label - works well | HIGH |
| **Selfies** | ML Kit Face Detection + front camera metadata | HIGH |
| **Screenshots** | Aspect ratio (16:9, 9:16), metadata (no EXIF camera data), filename patterns | HIGH |
| **Documents** | ML Kit Text Recognition + paper-like aspect ratios | MEDIUM |
| **Landscapes** | ML Kit has "Mountain", "Sky", "Nature" labels | HIGH |
| **Pets** | ML Kit has "Dog", "Cat", "Animal" labels | HIGH |
| **Products** | Generic object detection; challenging without custom training | LOW - needs custom model |
| **Artwork** | ML Kit has "Art", "Painting" labels | MEDIUM |

---

## Sources

- Google ML Kit documentation: https://developers.google.com/ml-kit (HIGH confidence - official docs)
- Android MediaStore API documentation (HIGH confidence - Android SDK knowledge)
- Android Storage Access Framework (SAF) behavior (HIGH confidence - Android SDK knowledge)
- Google Play Photography category analysis (MEDIUM confidence - app store observation)
