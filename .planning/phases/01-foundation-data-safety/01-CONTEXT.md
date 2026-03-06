# Phase 1: Foundation & Data Safety - Context

**Gathered:** 2025-03-06
**Status:** Ready for planning

<domain>
## Phase Boundary

Users install the app, complete onboarding, and the app learns from their existing photo folders. Phase 1 establishes the safe file handling infrastructure and folder learning system. The app auto-discovers existing folders in Pictures/, learns what each contains by analyzing sample photos, and is ready to organize new photos as they arrive. No manual category setup by users.

</domain>

<decisions>
## Implementation Decisions

### Onboarding Flow
- Single-screen onboarding (not multi-step wizard)
- Start with brief value explanation before any setup
- Permissions requested one-by-one with context (not all at once)
- No demo/example of organization (keep it short)
- Focus: explain what app does → grant permissions → done

### Folder Learning Strategy
- **Auto-discover existing folders** - Scan Pictures/ directory, no manual category setup
- **Sample 50 photos per folder** - Random sample for learning, not just recent
- **Combined learning approach** - Use both folder name AND visual analysis
  - Folder name as initial hint (e.g., "Receipts" suggests document category)
  - Visual analysis of sample photos to confirm/learn content patterns
- **Show learning progress** - Progress indicator while analyzing folders (50 photos × number of folders)
- **Background processing** - Learning happens after onboarding, not blocking
- **Folder selection** - Include all folders in Pictures/ by default (no exclusion UI in v1)

### Photo Organization Scope
- **New photos only** - Do NOT organize existing photos, only incoming new photos
- **Skip on low confidence** - If new photo doesn't match any folder well, leave it in source location
- **No prompts for uncertain photos** - Silent skip, no user interruption

### Confidence Threshold
- Three preset levels:
  - **Low (0.6)** - More photos organized, accepts more uncertainty
  - **Medium (0.75)** - Balanced approach
  - **High (0.9)** - Conservative, fewer mistakes (DEFAULT)
- Default: High (0.9) - Conservative to build user trust initially
- User can adjust in settings

### Claude's Discretion
- Exact progress indicator design (percentage, time estimate, etc.)
- How folder name influences learning weights
- Specific learning algorithm details
- Trash folder visibility and management
- Error handling messaging

</decisions>

<specifics>
## Specific Ideas

- App should feel invisible - users shouldn't notice it running
- Folder learning should complete before first new photo arrives
- Conservative by default (High threshold) to avoid misplacing photos
- Trust is critical - first misplaced photo = uninstall

</specifics>

<deferred>
## Deferred Ideas

- Manual category management UI (not needed - auto-learning replaces this)
- Batch processing existing photos (v2 consideration)
- Folder exclusion/inclusion settings (v2)
- Learning from user corrections (v2)

</deferred>

---

*Phase: 01-foundation-data-safety*
*Context gathered: 2025-03-06*
