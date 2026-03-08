# Phase 1: Foundation & Data Safety - Context

**Gathered:** 2025-03-08 (updated for Flutter)
**Status:** Ready for planning
**Previous version:** 2025-03-06 (native Android, superseded)

<domain>
## Phase Boundary

Users install the app, complete onboarding, and the app learns from their existing photo folders. Phase 1 establishes the safe file handling infrastructure and folder learning system. The app auto-discovers existing folders in Pictures/, learns what each contains by analyzing sample photos, and is ready to organize new photos as they arrive. No manual category setup by users.

**Tech stack:** Flutter + platform channels to native Android (SAF, WorkManager, Foreground Service)

</domain>

<decisions>
## Implementation Decisions

### Onboarding Flow
- Single-screen onboarding maintained from original context
- Permissions requested with context (not all at once) via SAF picker
- Value explanation first, then permissions
- No demo/example of organization (keep it short)
- **Permission response handling:** Spinner while waiting for platform channel
- **Post-permission flow:** Success confirmation before advancing to main screen

### Folder Learning Strategy (unchanged from original)
- **Auto-discover existing folders** - Scan Pictures/ directory, no manual category setup
- **Sample 50 photos per folder** - Random sample for learning, not just recent
- **Combined learning approach** - Use both folder name AND visual analysis
  - Folder name as initial hint (e.g., "Receipts" suggests document category)
  - Visual analysis of sample photos to confirm/learn content patterns
- **Background processing** - Learning happens after onboarding, not blocking
- **Folder selection** - Include all folders in Pictures/ by default (no exclusion UI in v1)

### Photo Organization Scope (unchanged)
- **New photos only** - Do NOT organize existing photos, only incoming new photos
- **Skip on low confidence** - If new photo doesn't match any folder well, leave it in source location
- **No prompts for uncertain photos** - Silent skip, no user interruption

### Confidence Threshold (unchanged)
- Three preset levels:
  - **Low (0.6)** - More photos organized, accepts more uncertainty
  - **Medium (0.75)** - Balanced approach
  - **High (0.9)** - Conservative, fewer mistakes (DEFAULT)
- Default: High (0.9) - Conservative to build user trust initially
- User can adjust in settings

### Claude's Discretion (new areas for Flutter)
- **Onboarding UX specifics:** 
  - Exact spinner design and animation
  - Success confirmation design and duration
  - Permission request ordering and wording
  
- **Folder learning presentation:**
  - Progress indicator style (bar vs checklist vs simple text)
  - Where learning runs (background isolate vs WorkManager)
  - Behavior when user leaves app during learning
  - Completion notification style

- **File operations:**
  - Transaction log replay triggers (startup vs periodic vs manual)
  - Recovery visibility (silent vs toast vs banner)
  - Storage warning presentation

- **Permission denial handling:**
  - Retry mechanism UX
  - Whether to allow skip-and-try-later

</decisions>

<specifics>
## Specific Ideas

- App should feel invisible - users shouldn't notice it running
- Folder learning should complete before first new photo arrives
- Conservative by default (High threshold) to avoid misplacing photos
- Trust is critical - first misplaced photo = uninstall
- **Flutter-specific:** Platform channels must be robust - SAF operations can fail silently if not handled correctly

</specifics>

<deferred>
## Deferred Ideas

- Manual category management UI (not needed - auto-learning replaces this)
- Batch processing existing photos (v2 consideration)
- Folder exclusion/inclusion settings (v2)
- Learning from user corrections (v2)
- **Native Android fallback** (if SAF proves too complex in Flutter, consider native implementation) - contingency only

</deferred>

<tech-notes>
## Flutter Implementation Notes

**Platform Channel Requirements:**
- SAF folder picker (`ACTION_OPEN_DOCUMENT_TREE`)
- File operations (copy/verify/delete via DocumentFile API)
- WorkManager integration for background learning
- Foreground service for photo monitoring (Phase 2)

**Key Packages:**
- `file_picker` - May not support SAF properly, may need native implementation
- `google_mlkit_image_labeling` - Works for learning phase
- `workmanager` - For background processing
- `sqflite` - Already in codebase

</tech-notes>

---

*Phase: 01-foundation-data-safety*
*Context gathered: 2025-03-08*
*Updated for: Flutter + platform channels architecture*
