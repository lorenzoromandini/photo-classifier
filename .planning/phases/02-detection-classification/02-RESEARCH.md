# Phase 02: Detection & Classification Pipeline - Research

**Researched:** 2025-03-08
**Domain:** Flutter Foreground Service, MediaStore Monitoring, ML Kit Image Labeling, WorkManager Background Tasks
**Confidence:** HIGH

## Summary

This research covers the technical implementation for Phase 2 of the Photo Classifier app - the Detection & Classification Pipeline. The goal is to build an automatic photo detection system that monitors for new photos, classifies them using on-device ML, and moves them to appropriate folders without user interaction for high-confidence matches.

The architecture requires a Flutter foreground service (`flutter_foreground_task`) to maintain persistent monitoring via MediaStore ContentObserver, WorkManager (`workmanager`) for scheduling ML classification jobs with battery constraints, and ML Kit Image Labeling (`google_mlkit_image_labeling`) for on-device classification. The service must handle Android-specific requirements like Doze mode, battery optimization exemptions, and automatic restart on boot.

Key technical challenges include: implementing MediaStore ContentObserver through Flutter platform channels (no native Dart API exists), managing the 6-hour timeout restriction for dataSync foreground services on Android 15+, handling battery optimization gracefully across different Android versions, and coordinating between foreground service (detection) and WorkManager (classification) to avoid duplicate processing.

**Primary recommendation:** Use `flutter_foreground_task` v9.2.1+ for persistent monitoring service with `dataSync` type, implement MediaStore ContentObserver through platform channels, schedule ML jobs via `workmanager` with battery constraints, and use `flutter_image_compress` for efficient 224x224 downscaling before ML classification.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| flutter_foreground_task | ^9.2.1 | Persistent foreground service | Production-ready, two-way communication, auto-run on boot |
| workmanager | ^0.9.0 | Background ML job scheduling | Official FlutterCommunity wrapper around Android WorkManager |
| google_mlkit_image_labeling | ^0.14.2 | On-device ML classification | Official Flutter ML Kit, no cloud dependency |
| google_mlkit_commons | ^0.11.0 | InputImage utilities | Required for ML Kit preprocessing |
| flutter_local_notifications | ^17.2.1 | User notifications | Already in project, needed for low-confidence queue |
| flutter_image_compress | ^2.4.0 | Image downscaling | Native performance, supports HEIC/HEIF |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| image | ^4.8.0 | Dart image manipulation | Fallback if flutter_image_compress fails |
| path_provider | ^2.1.2 | Temporary directories | For compressed image cache |
| permission_handler | ^11.3.0 | Battery optimization whitelist | Request ignore battery optimizations |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| flutter_foreground_task | flutter_background_service | flutter_foreground_task has better boot auto-run, two-way communication |
| workmanager | Custom WorkManager wrapper | workmanager is officially maintained, better documentation |
| flutter_image_compress | image package | Native compression is faster for large images |
| google_mlkit | TensorFlow Lite | ML Kit is pre-trained with 400+ labels, no training needed |

**Installation:**
```yaml
dependencies:
  # Already in project
  google_mlkit_image_labeling: ^0.14.2
  workmanager: ^0.5.2  # Note: Upgrade to ^0.9.0+3
  flutter_local_notifications: ^17.2.1
  permission_handler: ^11.3.0
  
  # New for Phase 2
  flutter_foreground_task: ^9.2.1
  flutter_image_compress: ^2.4.0
  path_provider: ^2.1.2
```

## Architecture Patterns

### Recommended Project Structure
```
lib/
├── main.dart                    # Initialize foreground task port
├── services/
│   ├── photo_monitor_service.dart    # Foreground service task handler
│   ├── photo_processor.dart          # ML classification coordinator
│   └── notification_service.dart     # Low-confidence notifications
├── platform/
│   └── media_store_observer.dart     # Platform channel to MediaStore
├── workers/
│   └── classification_worker.dart    # WorkManager background task
├── data/
│   └── repositories/
│       └── photo_queue_repository.dart # Pending photo queue
└── domain/
    └── models/
        ├── photo_detection.dart      # New photo event
        ├── classification_result.dart # ML output
        └── confidence_threshold.dart  # Threshold config
```

### Pattern 1: Foreground Service with Two-Way Communication
**What:** Use `flutter_foreground_task` to maintain a persistent foreground service that communicates with the UI isolate for photo detection events.
**When to use:** For monitoring that must persist even when app is in background.
**Example:**
```dart
// Source: https://pub.dev/packages/flutter_foreground_task
def main() {
  // Initialize port for communication between TaskHandler and UI
  FlutterForegroundTask.initCommunicationPort();
  runApp(const PhotoClassifierApp());
}

@pragma('vm:entry-point')
void startPhotoMonitorService() {
  FlutterForegroundTask.setTaskHandler(PhotoMonitorTaskHandler());
}

class PhotoMonitorTaskHandler extends TaskHandler {
  StreamSubscription? _mediaStoreSubscription;

  @override
  Future<void> onStart(DateTime timestamp, TaskStarter starter) async {
    // Initialize MediaStore observer via platform channel
    _initMediaStoreObserver();
  }

  @override
  void onRepeatEvent(DateTime timestamp) {
    // Periodic health check or empty
  }

  @override
  Future<void> onDestroy(DateTime timestamp, bool isTimeout) async {
    await _mediaStoreSubscription?.cancel();
  }

  @override
  void onReceiveData(Object data) {
    // Receive commands from UI (pause/resume, etc.)
  }
}
```

### Pattern 2: WorkManager ML Job Scheduling
**What:** Schedule ML classification tasks with battery constraints to run in background.
**When to use:** For CPU-intensive ML processing that doesn't need to run immediately.
**Example:**
```dart
// Source: https://docs.page/fluttercommunity/flutter_workmanager
def scheduleClassificationJob(String photoPath, String categoryId) {
  Workmanager().registerOneOffTask(
    "classify-$photoPath",
    "photoClassification",
    constraints: Constraints(
      networkType: NetworkType.notRequired,
      requiresBatteryNotLow: true,
      requiresCharging: false,
      requiresDeviceIdle: false,
      requiresStorageNotLow: true,
    ),
    inputData: {
      'photoPath': photoPath,
      'categoryId': categoryId,
      'sourceFolder': 'DCIM/Camera',
    },
  );
}

@pragma('vm:entry-point')
void classificationWorker() {
  Workmanager().executeTask((task, inputData) async {
    final photoPath = inputData?['photoPath'];
    if (photoPath == null) return Future.value(false);
    
    try {
      // Downscale image to 224x224
      final compressed = await FlutterImageCompress.compressWithFile(
        photoPath,
        minWidth: 224,
        minHeight: 224,
        quality: 90,
      );
      
      // Run ML classification
      final inputImage = InputImage.fromBytes(
        bytes: compressed,
        metadata: InputImageMetadata(
          size: Size(224, 224),
          rotation: InputImageRotation.rotation0,
          format: InputImageFormat.nv21,
          bytesPerRow: 224 * 3,
        ),
      );
      
      final labeler = ImageLabeler(
        options: ImageLabelerOptions(confidenceThreshold: 0.7),
      );
      final labels = await labeler.processImage(inputImage);
      
      // Map labels to category and move if confidence high
      await _processClassificationResult(photoPath, labels);
      
      return Future.value(true);
    } catch (e) {
      return Future.value(false); // Will retry
    }
  });
}
```

### Pattern 3: MediaStore ContentObserver via Platform Channel
**What:** Monitor for new photos by observing MediaStore changes through Android platform channel.
**When to use:** For detecting new photos added to DCIM, Downloads, WhatsApp.
**Example:**
```kotlin
// Source: Android MediaStore API
class MediaStoreObserver(private val context: Context) : ContentObserver(Handler(Looper.getMainLooper())) {
    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        
        // Query for new images since last check
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED
        )
        
        val selection = "${MediaStore.Images.Media.DATE_ADDED} > ?"
        val lastCheckTime = getLastCheckTime()
        val selectionArgs = arrayOf(lastCheckTime.toString())
        
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                // Filter for specific directories
                if (isMonitoredDirectory(path)) {
                    // Send to Flutter via platform channel
                    notifyFlutterOfNewPhoto(path)
                }
            }
        }
        
        updateLastCheckTime(System.currentTimeMillis() / 1000)
    }
    
    private fun isMonitoredDirectory(path: String): Boolean {
        return path.contains("/DCIM/Camera") ||
               path.contains("/Download") ||
               path.contains("/WhatsApp/Media/WhatsApp Images")
    }
}
```

### Anti-Patterns to Avoid
- **Don't process ML in foreground service:** Use WorkManager for ML to avoid ANR on slow devices
- **Don't poll MediaStore:** Use ContentObserver for event-driven detection, not polling
- **Don't skip battery optimization handling:** Always check and request whitelist for reliable background operation
- **Don't schedule duplicate jobs:** Track pending photo IDs to avoid queueing same photo multiple times

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Foreground service management | Custom Service + NotificationManager | flutter_foreground_task | Handles notification, lifecycle, boot auto-run, two-way comm |
| MediaStore polling | Timer-based polling | ContentObserver | Event-driven, battery efficient, immediate notification |
| Image compression | Dart-only compression | flutter_image_compress | Native performance, handles all formats including HEIC |
| Boot receiver | Custom BroadcastReceiver | flutter_foreground_task autoRunOnBoot | Handles Android version differences, permission checks |
| ML labeling | Custom TensorFlow model | google_mlkit_image_labeling | Pre-trained, optimized, 400+ labels, no training needed |
| Work scheduling | Handler.postDelayed | workmanager | Battery-aware, Doze-compatible, retry logic |

**Key insight:** Foreground services and MediaStore observation have complex Android version compatibility requirements. flutter_foreground_task handles foreground service types, runtime permissions, and Android 15's new dataSync timeout restrictions. MediaStore ContentObserver requires content URI permissions that must be maintained across reboots.

## Common Pitfalls

### Pitfall 1: Android 15 Foreground Service Timeout
**What goes wrong:** Foreground services with `dataSync` type are limited to 6 hours per 24-hour period on Android 15+ (targetSdk 35+).
**Why it happens:** Android 15 hardened foreground service restrictions to improve battery life.
**How to avoid:** 
- Design service to be restartable (autoRunOnBoot)
- Use WorkManager for actual ML processing (not foreground service)
- Foreground service should only monitor and queue, not process
- Consider using `specialUse` or `systemExempted` for critical monitoring apps
**Warning signs:** Service killed after 6 hours, "ForegroundServiceDidNotStartInTime" in logs

### Pitfall 2: MediaStore URI Permission Loss
**What goes wrong:** URIs obtained via SAF or MediaStore lose permission after device reboot.
**Why it happens:** Android revokes URI permissions on reboot for security.
**How to avoid:**
- Persist URIs with takePersistableUriPermission()
- Implement boot receiver to re-initialize monitoring
- Store folder mappings locally, re-query on startup
**Warning signs:** "Permission Denial: opening provider" errors after reboot

### Pitfall 3: Doze Mode Stops Background Work
**What goes wrong:** WorkManager jobs and alarms don't fire when device is in Doze mode.
**Why it happens:** Android's Doze mode defers background work to conserve battery.
**How to avoid:**
- Request REQUEST_IGNORE_BATTERY_OPTIMIZATIONS permission
- Use workmanager with requiresCharging or requiresDeviceIdle constraints
- Guide user to whitelist app in battery settings
**Warning signs:** Photos not processed for hours, then all at once when device wakes

### Pitfall 4: Duplicate Photo Processing
**What goes wrong:** Same photo queued multiple times if MediaStore fires multiple change events.
**Why it happens:** ContentObserver can fire for batch operations or media scanner runs.
**How to avoid:**
- Maintain in-memory Set of pending photo IDs in foreground service
- Check database before queueing new WorkManager job
- Use unique task names: `"classify-${photoHash}"`
**Warning signs:** Same photo moved multiple times, duplicate notifications

### Pitfall 5: HEIC/HEIF Image Handling
**What goes wrong:** ML Kit fails to process HEIC images on older Android versions.
**Why it happens:** HEIC support requires API 28+ and specific hardware encoder.
**How to avoid:**
- Detect format before processing
- Convert HEIC to JPEG using flutter_image_compress before ML
- Gracefully skip unsupported formats with logging
**Warning signs:** "Unsupported image format" errors, null results from ML Kit

### Pitfall 6: Corrupt Image Crashes
**What goes wrong:** ML Kit crashes when processing corrupt or incomplete images.
**Why it happens:** Image file may be partially written or corrupted.
**How to avoid:**
- Verify file exists and is readable before processing
- Wrap ML call in try-catch with specific exception handling
- Skip and log corrupt images instead of crashing
**Warning signs:** App crashes when processing new photos, especially downloads in progress

## Code Examples

### Foreground Service Initialization
```dart
// Source: https://pub.dev/packages/flutter_foreground_task
void _initService() {
  FlutterForegroundTask.init(
    androidNotificationOptions: AndroidNotificationOptions(
      channelId: 'photo_monitor',
      channelName: 'Photo Monitor Service',
      channelDescription: 'Monitoring for new photos to organize',
      channelImportance: NotificationChannelImportance.LOW,
      priority: NotificationPriority.LOW,
      onlyAlertOnce: true,
    ),
    iosNotificationOptions: const IOSNotificationOptions(
      showNotification: false,
      playSound: false,
    ),
    foregroundTaskOptions: ForegroundTaskOptions(
      eventAction: ForegroundTaskEventAction.repeat(60000), // 1 min health check
      autoRunOnBoot: true,
      autoRunOnMyPackageReplaced: true,
      allowWakeLock: true,
      allowWifiLock: false,
    ),
  );
}
```

### Starting Service with Permissions
```dart
// Source: https://pub.dev/packages/flutter_foreground_task
Future<void> _startPhotoMonitorService() async {
  // Android 13+, need notification permission
  final notificationPermission = await FlutterForegroundTask.checkNotificationPermission();
  if (notificationPermission != NotificationPermission.granted) {
    await FlutterForegroundTask.requestNotificationPermission();
  }
  
  // Android 12+, need to allow ignore battery optimizations for auto-restart
  if (!await FlutterForegroundTask.isIgnoringBatteryOptimizations) {
    await FlutterForegroundTask.requestIgnoreBatteryOptimization();
  }
  
  if (await FlutterForegroundTask.isRunningService) {
    await FlutterForegroundTask.restartService();
  } else {
    await FlutterForegroundTask.startService(
      serviceId: 256,
      notificationTitle: 'Photo Monitor',
      notificationText: 'Watching for new photos...',
      notificationButtons: [
        const NotificationButton(id: 'pause', text: 'Pause'),
        const NotificationButton(id: 'settings', text: 'Settings'),
      ],
      callback: startPhotoMonitorService,
    );
  }
}
```

### ML Kit Image Classification
```dart
// Source: https://pub.dev/packages/google_mlkit_image_labeling
Future<ClassificationResult> classifyImage(String imagePath) async {
  // Downscale to 224x224 for performance
  final compressed = await FlutterImageCompress.compressWithFile(
    imagePath,
    minWidth: 224,
    minHeight: 224,
    quality: 90,
    format: CompressFormat.jpeg,
  );
  
  if (compressed == null) {
    throw Exception('Failed to compress image');
  }
  
  // Create InputImage from bytes
  final inputImage = InputImage.fromBytes(
    bytes: compressed,
    metadata: InputImageMetadata(
      size: const Size(224, 224),
      rotation: InputImageRotation.rotation0,
      format: InputImageFormat.nv21,
      bytesPerRow: 224 * 3,
    ),
  );
  
  // Initialize labeler with confidence threshold
  final labeler = ImageLabeler(
    options: ImageLabelerOptions(confidenceThreshold: 0.5),
  );
  
  try {
    final labels = await labeler.processImage(inputImage);
    
    // Sort by confidence
    labels.sort((a, b) => b.confidence.compareTo(a.confidence));
    
    return ClassificationResult(
      labels: labels.map((l) => ImageLabel(
        label: l.label,
        confidence: l.confidence,
        index: l.index,
      )).toList(),
      topLabel: labels.isNotEmpty ? labels.first : null,
      timestamp: DateTime.now(),
    );
  } finally {
    await labeler.close();
  }
}
```

### WorkManager with Constraints
```dart
// Source: https://docs.page/fluttercommunity/flutter_workmanager
def scheduleDelayedClassification(String photoPath) {
  Workmanager().registerOneOffTask(
    "classify-${photoPath.hashCode}",
    "photoClassification",
    initialDelay: const Duration(seconds: 5), // Debounce rapid changes
    constraints: Constraints(
      networkType: NetworkType.notRequired,
      requiresBatteryNotLow: true,
      requiresCharging: false,
      requiresDeviceIdle: false,
      requiresStorageNotLow: true,
    ),
    inputData: {
      'photoPath': photoPath,
      'timestamp': DateTime.now().millisecondsSinceEpoch,
    },
    existingWorkPolicy: ExistingWorkPolicy.keep, // Don't duplicate
  );
}
```

### Sending Data from Service to UI
```dart
// Source: https://pub.dev/packages/flutter_foreground_task
class PhotoMonitorTaskHandler extends TaskHandler {
  @override
  void onRepeatEvent(DateTime timestamp) {
    // Check for new photos
    final newPhotos = checkForNewPhotos();
    
    for (final photo in newPhotos) {
      // Send to UI isolate
      FlutterForegroundTask.sendDataToMain({
        'type': 'new_photo',
        'path': photo.path,
        'timestamp': photo.timestamp.millisecondsSinceEpoch,
      });
      
      // Queue for classification
      scheduleClassificationJob(photo.path);
    }
  }
}

// In Flutter UI
void _onReceiveTaskData(Object data) {
  if (data is Map<String, dynamic>) {
    if (data['type'] == 'new_photo') {
      final path = data['path'];
      print('New photo detected: $path');
      // Update UI or state management
    }
  }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Manual SAF permission per file | Persistable URI permission | Android 10+ | Can maintain access across reboots with takePersistableUriPermission |
| Background Service | Foreground Service + WorkManager | Android 8.0 (API 26) | Background services limited, foreground required for persistent monitoring |
| JobScheduler | WorkManager | 2018+ | WorkManager is backwards compatible wrapper around JobScheduler/AlarmManager |
| Polling MediaStore | ContentObserver | Always | Event-driven is more battery efficient than polling |
| JPEG only | HEIC/HEIF support | Android 9+ (API 28) | iPhone photos often in HEIC, need conversion before ML |

**Deprecated/outdated:**
- **flutter_background_service**: Less maintained than flutter_foreground_task, lacks boot auto-run
- **Manual WorkManager binding**: Use workmanager package instead of direct Android implementation
- **Firebase ML Kit**: Deprecated, use Google ML Kit (on-device) instead

## Android Manifest Requirements

```xml
<!-- Required for foreground service -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

<!-- Required for boot auto-start -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- Required for battery optimization whitelist -->
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<!-- Existing permissions -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<application ...>
    <!-- Foreground service declaration -->
    <service 
        android:name="com.pravera.flutter_foreground_task.service.ForegroundService"
        android:foregroundServiceType="dataSync|remoteMessaging"
        android:exported="false" />
    
    <!-- Boot receiver for auto-start -->
    <receiver
        android:name="com.pravera.flutter_foreground_task.BootReceiver"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.BOOT_COMPLETED" />
        </intent-filter>
    </receiver>
</application>
```

## Open Questions

1. **MediaStore ContentObserver Latency**
   - What we know: ContentObserver is event-driven but can have slight delay
   - What's unclear: Exact latency on different Android versions/devices
   - Recommendation: Implement with logging to measure, expect <1s delay

2. **Android 15 Foreground Service Timeout Workarounds**
   - What we know: 6-hour limit for dataSync, timer resets when app in foreground
   - What's unclear: Whether specialUse exemption is approved for photo organizers
   - Recommendation: Design for restartability, use WorkManager for actual processing

3. **WhatsApp Directory Monitoring**
   - What we know: WhatsApp stores images in /Android/media/com.whatsapp/WhatsApp/Media/
   - What's unclear: Whether SAF permission can access this on Android 11+ (scoped storage)
   - Recommendation: Test with actual WhatsApp install, may need DocumentFile traversal

4. **ML Kit Processing Time**
   - What we know: Processing time varies by image size and device
   - What's unclear: Exact performance on mid-range devices with 224x224 input
   - Recommendation: Benchmark on target devices, expect 50-200ms per image

5. **Corrupt Image Detection**
   - What we know: Partially downloaded images can crash ML Kit
   - What's unclear: Best way to detect incomplete write before processing
   - Recommendation: Check file size stability (wait for size to stop changing) or use file lock detection

## Sources

### Primary (HIGH confidence)
- pub.dev/packages/flutter_foreground_task (v9.2.1) - Foreground service implementation, boot auto-run, two-way communication
- pub.dev/packages/google_mlkit_image_labeling (v0.14.2) - ML Kit Image Labeling API, base model usage
- pub.dev/packages/workmanager (v0.9.0) - WorkManager scheduling with constraints
- pub.dev/packages/flutter_image_compress (v2.4.0) - Image compression for preprocessing

### Secondary (MEDIUM confidence)
- docs.page/fluttercommunity/flutter_workmanager/customization - WorkManager constraints and configuration
- github.com/Dev-hwang/flutter_foreground_task/documentation/migration_documentation.md - Android 15 changes, service types
- github.com/flutter-ml/google_ml_kit_flutter/packages/google_mlkit_commons - InputImage creation from various sources

### Tertiary (LOW confidence)
- Android MediaStore documentation (developer.android.com) - ContentObserver patterns
- Android Battery Optimization documentation - Doze mode behavior
- Android Foreground Service documentation (API 34/35 changes)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All packages verified on pub.dev, actively maintained
- Architecture: HIGH - Patterns from official package documentation
- Pitfalls: MEDIUM-HIGH - Based on Android version changes and package changelogs

**Research date:** 2025-03-08
**Valid until:** 30 days (packages update frequently, verify versions before implementation)

**Upgrade Notes:**
- workmanager: Project currently has 0.5.2, upgrade to 0.9.0+3 for latest features
- flutter_foreground_task: New version 9.x requires Flutter 3.22+, Kotlin 1.9.10+, Gradle 8.6.0+
- Ensure Android compileSdkVersion is 35 for flutter_foreground_task v9+
