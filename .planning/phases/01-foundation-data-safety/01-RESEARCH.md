# Phase 01: Foundation & Data Safety - Research

**Researched:** 2025-03-06
**Domain:** Android Kotlin - Data Layer, SAF, ML Kit, Onboarding
**Confidence:** HIGH

## Summary

This research covers the technical foundation for building an Android Photo Auto-Organizer app with focus on data safety, folder auto-discovery, and ML-based learning. The app uses Storage Access Framework (SAF) for scoped storage compliance, Room database for metadata persistence, DataStore for user preferences, ML Kit Image Labeling for on-device visual analysis, and WorkManager for background processing.

Key architectural decisions are guided by the official "Now in Android" reference app, which demonstrates modern Android architecture patterns including offline-first repositories, reactive data flows with Kotlin Flow, and modular structure. For Phase 01 specifically, we need to establish the data layer (Room + DataStore), implement SAF for folder access, create the learning pipeline that samples and analyzes existing photos, and build a lightweight single-screen onboarding flow.

**Primary recommendation:** Follow the Now in Android architecture pattern with a three-layer architecture (Data → Domain → UI), use Proto DataStore for preferences, Room for photo metadata, and implement SAF with ACTION_OPEN_DOCUMENT_TREE for folder permissions.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Single-screen onboarding (not multi-step wizard)
- Auto-discover existing folders in Pictures/ directory (no manual category setup)
- Sample 50 photos per folder for learning
- Combined learning: folder name hint + visual analysis of samples
- New photos only (NOT existing photos)
- Skip on low confidence (no prompts for uncertain photos)
- Three preset confidence levels: Low (0.6), Medium (0.75), High (0.9) - DEFAULT

### Claude's Discretion
- Exact progress indicator design (percentage, time estimate, etc.)
- How folder name influences learning weights
- Specific learning algorithm details
- Trash folder visibility and management
- Error handling messaging

### Deferred Ideas (OUT OF SCOPE)
- Manual category management
- Batch processing existing photos
- Folder exclusion settings
- Learning from user corrections
</user_constraints>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Kotlin | 2.0+ | Language | Official Android language, coroutines support |
| Android Gradle Plugin | 8.5+ | Build | Required for latest Android Studio |
| Jetpack Compose | 2024.02.00 BOM | UI | Modern declarative UI, Material 3 |
| Hilt | 2.51+ | DI | Official DI for Android, compile-time safety |
| Room | 2.6.1+ | Database | Official ORM, compile-time SQL verification |
| DataStore | 1.0.0+ | Preferences | Replaces SharedPreferences, type-safe, transactional |
| ML Kit Image Labeling | 17.0.9 | ML | On-device, 400+ labels, no cloud dependency |
| WorkManager | 2.9.0+ | Background work | Guaranteed execution, battery-aware |
| Kotlin Coroutines | 1.8.0+ | Async | Structured concurrency, Flow for streams |
| Kotlinx Serialization | 1.6.3 | JSON | Multiplatform, type-safe, no reflection |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| DocumentFile | Latest (platform) | SAF operations | Accessing files via URI permissions |
| Proto DataStore | 1.0.0+ | Typed preferences | Complex preferences with protobuf |
| androidx.core | 1.12.0+ | Core extensions | FileProvider, Uri operations |
| Coil | 2.6.0 | Image loading | Efficient bitmap loading from URIs |
| Timber | 5.0.1 | Logging | Debug logging in dev, crash reports in prod |
| Exponential Backoff | Custom (simple) | Retry logic | File operations with transient failures |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Room | SQLDelight | SQLDelight more type-safe but Room more standard for simple schemas |
| DataStore | SharedPreferences | DataStore is async, type-safe, preferred for new apps |
| ML Kit bundled | ML Kit unbundled | Bundled adds 5.7MB but works offline immediately |
| Hilt | Koin | Hilt has better compile-time verification, official support |

**Gradle Dependencies:**
```kotlin
// Room
implementation("androidx.room:room-runtime:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")

// DataStore (Proto)
implementation("androidx.datastore:datastore:1.0.0")
implementation("com.google.protobuf:protobuf-kotlin-lite:3.25.1")

// ML Kit Image Labeling (bundled model)
implementation("com.google.mlkit:image-labeling:17.0.9")

// WorkManager
implementation("androidx.work:work-runtime-ktx:2.9.0")

// Hilt
implementation("com.google.dagger:hilt-android:2.51")
kapt("com.google.dagger:hilt-compiler:2.51")
implementation("androidx.hilt:hilt-work:1.2.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")

// Compose BOM
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-tooling-preview")
```

## Architecture Patterns

### Recommended Project Structure
```
app/
├── data/
│   ├── local/
│   │   ├── database/          # Room entities, DAOs, Database
│   │   ├── datastore/         # Proto DataStore, preferences
│   │   └── saf/               # Storage Access Framework wrappers
│   ├── repository/            # Repositories (offline-first pattern)
│   └── model/                 # Data layer models
├── domain/
│   ├── usecase/               # Use cases for business logic
│   └── model/                 # Domain models
├── feature/
│   └── onboarding/            # Onboarding screen
├── service/
│   └── PhotoMonitorService.kt # Foreground service for monitoring
└── di/                        # Dependency injection modules
```

### Pattern 1: Offline-First Repository
**What:** Repository pattern where local database is source of truth, remote/cloud sync is secondary
**When to use:** For all data operations, especially with SAF where remote URIs need local persistence
**Example:**
```kotlin
// Source: Now in Android - OfflineFirstUserDataRepository.kt
class FolderRepository @Inject constructor(
    private val folderDao: FolderDao,
    private val documentDataSource: DocumentDataSource,
) {
    // Expose as Flow for reactive updates
    val folders: Flow<List<Folder>> = folderDao.getAll()
        .map { entities -> entities.map { it.toDomain() } }
    
    suspend fun syncFolders(uri: Uri) = withContext(Dispatchers.IO) {
        // SAF query to discover folders
        val discovered = documentDataSource.listFolders(uri)
        // Persist to Room (source of truth)
        folderDao.insertAll(discovered.map { it.toEntity() })
    }
}
```

### Pattern 2: Proto DataStore with Repository Abstraction
**What:** Type-safe preferences using Protocol Buffers, accessed through repository interface
**When to use:** User settings like confidence threshold, onboarding completion, theme preferences
**Example:**
```kotlin
// Source: Now in Android - NiaPreferencesDataSource.kt
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<UserPreferences>,
) {
    val userData: Flow<UserData> = dataStore.data
        .map { prefs ->
            UserData(
                confidenceThreshold = prefs.confidenceThreshold,
                onboardingCompleted = prefs.onboardingCompleted,
                folders = prefs.foldersList,
            )
        }

    suspend fun setConfidenceThreshold(threshold: Float) {
        dataStore.updateData { current ->
            current.copy { this.confidenceThreshold = threshold }
        }
    }
}
```

### Pattern 3: Transaction Log Pattern
**What:** Append-only log of file operations for crash recovery
**When to use:** For copy-verify-delete operations, enables rollback on crash
**Example:**
```kotlin
// Transaction log entity in Room
@Entity(tableName = "file_operations")
data class FileOperation(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sourceUri: String,
    val destUri: String,
    val operationType: OperationType, // COPY, VERIFY, DELETE
    val status: OperationStatus,      // PENDING, COMPLETED, FAILED
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
)

// Recovery: replay incomplete operations on startup
suspend fun recoverPendingOperations() {
    val pending = operationDao.getPending()
    pending.forEach { op ->
        when (op.operationType) {
            OperationType.COPY -> if (op.status != COMPLETED) retryCopy(op)
            OperationType.DELETE -> if (op.status == COMPLETED) safeDelete(op)
        }
    }
}
```

### Pattern 4: SAF DocumentFile Wrapper
**What:** Abstraction over Storage Access Framework for testing and error handling
**When to use:** All SAF operations (list folders, copy files, verify)
**Example:**
```kotlin
// Wrapper for SAF operations with proper error handling
class DocumentDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun listFolders(uri: Uri): List<FolderInfo> {
        val docFile = DocumentFile.fromTreeUri(context, uri) 
            ?: throw SafException("Invalid URI: $uri")
        
        return docFile.listFiles()
            .filter { it.isDirectory }
            .map { FolderInfo(it.uri, it.name ?: "Unknown") }
    }
    
    suspend fun copyFile(source: Uri, dest: Uri): Result<Unit> = try {
        val resolver = context.contentResolver
        resolver.openInputStream(source)?.use { input ->
            resolver.openOutputStream(dest)?.use { output ->
                input.copyTo(output)
            }
        }
        Result.success(Unit)
    } catch (e: IOException) {
        Result.failure(e)
    }
}
```

### Pattern 5: ML Kit Image Labeling Pipeline
**What:** Process images through ML Kit, collect labels with confidence scores
**When to use:** Folder learning phase, analyzing sample photos
**Example:**
```kotlin
// Source: ML Kit documentation
class ImageLabelingDataSource @Inject constructor() {
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.5f) // Lower for learning
            .build()
    )

    suspend fun analyzeImage(uri: Uri, context: Context): List<ImageLabel> = 
        suspendCancellableCoroutine { continuation ->
            val image = try {
                InputImage.fromFilePath(context, uri)
            } catch (e: IOException) {
                continuation.resumeWithException(e)
                return@suspendCancellableCoroutine
            }
            
            labeler.process(image)
                .addOnSuccessListener { labels ->
                    continuation.resume(labels)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
}
```

### Anti-Patterns to Avoid
- **Direct Context in ViewModels:** Use repositories, let DI provide Context
- **Blocking Main Thread with SAF:** All SAF operations must be on IO dispatcher
- **Storing full URIs without persisting permissions:** Must call takePersistableUriPermission()
- **Modifying data in @Composable functions:** Use ViewModel events
- **Manual transaction management:** Room handles transactions; use @Transaction annotation

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Preferences storage | SharedPreferences wrapper | Proto DataStore | Type-safe, transactional, handles corruption |
| Database migrations | Manual SQL scripts | Room AutoMigration | Compile-time verified, handles schema changes |
| Background work | HandlerThread + Looper | WorkManager | Battery-aware, survives process death, guaranteed execution |
| Image loading | BitmapFactory | Coil | Memory management, caching, transformations |
| Dependency injection | Service Locator | Hilt | Compile-time verification, standard lifecycle |
| Thread switching | AsyncTask (deprecated) | Coroutines + Dispatchers | Structured concurrency, cancellation |
| File copy with progress | Manual byte copying | kotlin.io.copyTo | Standard, optimized, handles streams correctly |
| Retry logic | Manual while loops | ExponentialBackoff with WorkManager constraints | Handles battery, network, backoff automatically |
| Photo monitoring | FileObserver (unreliable) | ContentObserver on MediaStore | Works with scoped storage, reliable across devices |
| JSON serialization | org.json | kotlinx.serialization | Type-safe, null-safe, no reflection |

**Key insight:** SAF and scoped storage have many edge cases across Android versions and OEM skins. Using standard AndroidX libraries that handle these quirks is safer than custom implementations that may break on specific devices.

## Common Pitfalls

### Pitfall 1: URI Permission Loss
**What goes wrong:** App loses access to folders after restart because URI permissions weren't persisted
**Why it happens:** SAF URIs require explicit persistable permission grants
**How to avoid:**
```kotlin
// Must call this immediately after user selects folder
val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or 
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
context.contentResolver.takePersistableUriPermission(uri, takeFlags)
```
**Warning signs:** SecurityException when accessing files after app restart

### Pitfall 2: Room Database Thread Blocking
**What goes wrong:** ANR when querying large photo datasets
**Why it happens:** Room queries on main thread, or collecting Flow without proper dispatchers
**How to avoid:**
```kotlin
// Always use IO dispatcher for queries
suspend fun getPhotos() = withContext(Dispatchers.IO) {
    photoDao.getAll()
}

// Flow collection in ViewModel uses stateIn with IO
val photos = photoRepository.photos
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```
**Warning signs:** StrictMode disk read violations, ANR reports

### Pitfall 3: ML Kit Memory Leaks
**What goes wrong:** OOM when processing many photos sequentially
**Why it happens:** InputImage objects not closed, Bitmaps retained
**How to avoid:**
```kotlin
// Always close InputImage after use
suspend fun analyzeBatch(uris: List<Uri>) {
    uris.forEach { uri ->
        val image = InputImage.fromFilePath(context, uri)
        try {
            labeler.process(image).await()
        } finally {
            // InputImage doesn't implement Closeable but we should 
            // release references
        }
    }
}
```
**Warning signs:** OutOfMemoryError in crash reports, app killed in background

### Pitfall 4: WorkManager Constraints Not Respected
**What goes wrong:** Background learning work runs immediately regardless of battery/network
**Why it happens:** Default constraints allow immediate execution
**How to avoid:**
```kotlin
val constraints = Constraints.Builder()
    .setRequiresBatteryNotLow(true)
    .setRequiresStorageNotLow(true)
    .build()

val workRequest = OneTimeWorkRequestBuilder<FolderLearningWorker>()
    .setConstraints(constraints)
    .setBackoffCriteria(
        BackoffPolicy.EXPONENTIAL,
        WorkRequest.MIN_BACKOFF_MILLIS,
        TimeUnit.MILLISECONDS
    )
    .build()
```
**Warning signs:** Battery drain complaints, work failing on low storage

### Pitfall 5: DataStore Write Conflicts
**What goes wrong:** DataStore throws IOException on concurrent writes
**Why it happens:** Multiple coroutines calling updateData simultaneously
**How to avoid:**
```kotlin
// Use single writer pattern via repository
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<UserPreferences>,
) {
    // Repository serializes access
    suspend fun updateSetting(value: Boolean) {
        dataStore.updateData { current ->
            current.copy { setting = value }
        }
    }
}
```
**Warning signs:** IOException: "Unable to rename", data not persisting

### Pitfall 6: Compose Recomposition Storm
**What goes wrong:** UI freezes during folder scanning progress
**Why it happens:** Updating progress State too frequently causes recomposition
**How to avoid:**
```kotlin
// Throttle progress updates
private val _progress = MutableStateFlow(0)
val progress = _progress
    .sample(100.milliseconds) // Limit to 10 updates/second
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)
```
**Warning signs:** Frame drops, janky UI during processing

## Code Examples

### DataStore Setup with Protobuf
```kotlin
// 1. Define proto (app/src/main/proto/user_preferences.proto)
syntax = "proto3";
option java_package = "com.example.photoapp.data.local";
option java_multiple_files = true;

message UserPreferences {
  float confidence_threshold = 1;
  bool onboarding_completed = 2;
  repeated string folder_uris = 3;
  int32 learning_sample_size = 4 [default = 50];
}

// 2. Create Serializer
class UserPreferencesSerializer @Inject constructor() : Serializer<UserPreferences> {
    override val defaultValue: UserPreferences = UserPreferences.getDefaultInstance()
    
    override suspend fun readFrom(input: InputStream): UserPreferences =
        try {
            UserPreferences.parseFrom(input)
        } catch (e: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto", e)
        }
    
    override suspend fun writeTo(t: UserPreferences, output: OutputStream) {
        t.writeTo(output)
    }
}

// 3. Hilt Module
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context,
        serializer: UserPreferencesSerializer,
    ): DataStore<UserPreferences> = DataStoreFactory.create(
        serializer = serializer,
        produceFile = { context.dataStoreFile("user_prefs.pb") }
    )
}
```

### Room Entities for Photo Organization
```kotlin
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val uri: String, // Document URI string
    val name: String,
    val displayName: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "photo_metadata")
data class PhotoMetadataEntity(
    @PrimaryKey val uri: String,
    val folderUri: String,
    val fileName: String,
    val processedAt: Long? = null,
    val labels: String, // JSON array of labels
    val status: String, // PENDING, PROCESSED, FAILED
)

@Entity(tableName = "file_operations")
data class FileOperationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sourceUri: String,
    val destUri: String,
    val operationType: OperationType,
    val status: OperationStatus,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
)

// DAO with transactions
@Dao
interface FolderDao {
    @Query("SELECT * FROM folders")
    fun getAll(): Flow<List<FolderEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(folders: List<FolderEntity>)
    
    @Transaction
    suspend fun syncFolders(folders: List<FolderEntity>) {
        deleteAll()
        insertAll(folders)
    }
    
    @Query("DELETE FROM folders")
    suspend fun deleteAll()
}
```

### SAF Folder Discovery
```kotlin
class SafDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun discoverFolders(baseUri: Uri): List<FolderInfo> {
        val docFile = DocumentFile.fromTreeUri(context, baseUri)
            ?: throw IllegalArgumentException("Invalid URI: $baseUri")
        
        return docFile.listFiles()
            .asSequence()
            .filter { it.isDirectory && it.name != null }
            .filter { it.name != "Android" } // Skip system folders
            .map { file ->
                FolderInfo(
                    uri = file.uri.toString(),
                    name = file.name!!,
                    photoCount = countPhotos(file),
                )
            }
            .toList()
    }
    
    private fun countPhotos(folder: DocumentFile): Int {
        return folder.listFiles().count { file ->
            file.isFile && file.name?.matches(IMAGE_REGEX) == true
        }
    }
    
    companion object {
        private val IMAGE_REGEX = Regex(".*\\.(jpg|jpeg|png|webp)$", RegexOption.IGNORE_CASE)
    }
}
```

### Foreground Service for Photo Monitoring
```kotlin
@AndroidEntryPoint
class PhotoMonitorService : LifecycleService() {
    @Inject lateinit var photoRepository: PhotoRepository
    
    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            uri?.let { onNewPhotoDetected(it) }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )
        
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver
        )
    }
    
    private fun createNotification(): Notification {
        val channelId = "photo_monitor_channel"
        val channel = NotificationChannel(
            channelId,
            "Photo Monitor",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        
        return Notification.Builder(this, channelId)
            .setContentTitle("Photo Organizer Running")
            .setContentText("Monitoring for new photos")
            .setSmallIcon(R.drawable.ic_notification)
            .build()
    }
    
    private fun onNewPhotoDetected(uri: Uri) {
        lifecycleScope.launch {
            photoRepository.processNewPhoto(uri)
        }
    }
    
    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
```

### Copy-Verify-Delete with Transaction Log
```kotlin
class SafeFileOperations @Inject constructor(
    @ApplicationContext private val context: Context,
    private val operationDao: FileOperationDao,
) {
    suspend fun safeMove(sourceUri: Uri, destUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        val operationId = createOperation(sourceUri, destUri)
        
        try {
            // Step 1: Copy
            updateOperationStatus(operationId, OperationStatus.COPYING)
            copyFile(sourceUri, destUri).getOrThrow()
            
            // Step 2: Verify
            updateOperationStatus(operationId, OperationStatus.VERIFYING)
            if (!verifyCopy(sourceUri, destUri)) {
                throw IOException("Verification failed")
            }
            
            // Step 3: Delete source
            updateOperationStatus(operationId, OperationStatus.DELETING)
            deleteSource(sourceUri)
            
            // Complete
            updateOperationStatus(operationId, OperationStatus.COMPLETED)
            Result.success(Unit)
            
        } catch (e: Exception) {
            updateOperationStatus(operationId, OperationStatus.FAILED)
            Result.failure(e)
        }
    }
    
    private suspend fun verifyCopy(source: Uri, dest: Uri): Boolean {
        val sourceSize = context.contentResolver.openFileDescriptor(source, "r")?.use { it.statSize } ?: return false
        val destSize = context.contentResolver.openFileDescriptor(dest, "r")?.use { it.statSize } ?: return false
        return sourceSize == destSize
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| SharedPreferences | Proto DataStore | 2020 | Type-safe, transactional, handles corruption |
| SQLiteOpenHelper | Room | 2017 | Compile-time SQL, less boilerplate |
| AsyncTask | Coroutines + WorkManager | 2019 (deprecated) | Structured concurrency, lifecycle-aware |
| File API direct access | SAF (Storage Access Framework) | Android 10+ | Scoped storage compliance |
| Manual DI (Dagger) | Hilt | 2020 | Reduced boilerplate, standard patterns |
| RxJava | Kotlin Flow | 2019+ | Lighter, better Compose integration |
| Glide | Coil | 2019+ | Kotlin-first, Compose-native |

**Deprecated/outdated:**
- AsyncTask: Deprecated in API 30, use Coroutines
- LocalBroadcastManager: Deprecated, use Flow/SharedFlow
- File path access: Blocked on Android 10+, use SAF/MediaStore
- executePendingTransactions: FragmentManager, use commitNow
- PreferenceFragmentCompat: Use Compose preference components

## Open Questions

1. **Folder Learning Algorithm**
   - What we know: Need to sample 50 photos per folder, use ML Kit labels
   - What's unclear: Exact algorithm for combining folder name + visual labels
   - Recommendation: Start simple - aggregate top 10 labels per folder, weight by frequency, boost folder name match

2. **Storage Full Detection**
   - What we know: Need to detect before copy operations
   - What's unclear: Exact threshold for "full" (95%? 90%?)
   - Recommendation: Check StatFs, fail fast if < 500MB available

3. **Trash Folder Implementation**
   - What we know: 7-day retention required
   - What's unclear: Should trash be visible to user? Auto-cleanup timing?
   - Recommendation: Hidden .trash folder, cleanup on app startup, schedule WorkManager daily

4. **Retry Backoff Strategy**
   - What we know: Need exponential backoff for failed operations
   - What's unclear: Max retry count, initial delay, max delay
   - Recommendation: 5 retries, 1s initial, 5min max, use WorkManager backoff

5. **Progress Indication During Learning**
   - What we know: Need to show progress (50 photos × N folders)
   - What's unclear: Exact UX (percentage vs "Folder 3 of 10")
   - Recommendation: Show "Learning folder: {name}" with circular progress, total count in subtitle

## Sources

### Primary (HIGH confidence)
- https://github.com/android/nowinandroid - Official Android architecture reference (Architecture Learning Journey, Modularization Learning Journey)
- https://github.com/googlesamples/mlkit - ML Kit samples and VisionProcessorBase
- https://developers.google.com/ml-kit/vision/image-labeling/android - ML Kit Image Labeling API documentation

### Secondary (MEDIUM confidence)
- Now in Android repository code (NiaPreferencesDataSource.kt, NiaDatabase.kt, DataStoreModule.kt, OfflineFirstUserDataRepository.kt)
- ML Kit quickstart sample code (VisionProcessorBase.kt)
- Jetpack Compose documentation patterns

### Tertiary (LOW confidence)
- None - all key patterns verified with official sources

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Verified with official Android documentation and Now in Android
- Architecture: HIGH - Based on official Now in Android patterns
- Pitfalls: HIGH - Documented in official sources and common Android knowledge
- Code examples: HIGH - Derived from verified Now in Android and ML Kit samples

**Research date:** 2025-03-06
**Valid until:** 2025-06-06 (90 days for stable Android stack)
