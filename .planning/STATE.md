# STATE: Android Photo Auto-Organizer

**Project:** Photo-Classificator  
**Current Phase:** Phase 2 — Detection & Classification Pipeline (Planned)  
**Last Updated:** 2026-03-08  

---

## Project Reference

- **Project Vision:** [.planning/PROJECT.md](PROJECT.md)
- **Requirements:** [.planning/REQUIREMENTS.md](REQUIREMENTS.md)
- **Roadmap:** [.planning/ROADMAP.md](ROADMAP.md)
- **Research Summary:** [.planning/research/SUMMARY.md](research/SUMMARY.md)
- **Workflow Config:** [.planning/config.json](config.json)

---

## Phase Status

| Phase | Name | Status | Requirements Complete | Success Criteria Met |
|-------|------|--------|----------------------|---------------------|
| 1 | Foundation & Data Safety | ✅ **PLANNED** | 7/7 | 0/4 |
| 2 | Detection & Classification Pipeline | **PLANNED** | 0/14 | 0/5 |
| 3 | Notifications & Polish | Blocked | 0/7 | 0/4 |
| 4 | Release Preparation | Blocked | 0/3 | 0/3 |

### Current Plan Progress

**01-01:** ✅ Platform Channels & Database (Flutter rewrite) - [PLAN](phases/01-foundation-data-safety/01-01-PLAN.md) | [SUMMARY](phases/01-foundation-data-safety/01-01-SUMMARY.md)  
**01-02:** ✅ SAF Integration via Platform Channels (Flutter rewrite) - [PLAN](phases/01-foundation-data-safety/01-02-PLAN.md) | [SUMMARY](phases/01-foundation-data-safety/01-02-SUMMARY.md)  
**01-03:** ✅ Safe File Operations (Flutter rewrite) - [PLAN](phases/01-foundation-data-safety/01-03-PLAN.md) | [SUMMARY](phases/01-foundation-data-safety/01-03-SUMMARY.md)  
**01-04:** ✅ Onboarding Flow (Flutter rewrite) - [PLAN](phases/01-foundation-data-safety/01-04-PLAN.md) | [SUMMARY](phases/01-foundation-data-safety/01-04-SUMMARY.md)  
**01-05:** ✅ Folder Learning System (Flutter rewrite) - [PLAN](phases/01-foundation-data-safety/01-05-PLAN.md) | [SUMMARY](phases/01-foundation-data-safety/01-05-SUMMARY.md)  
**01-06:** ✅ Trash System (Flutter rewrite) - [PLAN](phases/01-foundation-data-safety/01-06-PLAN.md) | [SUMMARY](phases/01-foundation-data-safety/01-06-SUMMARY.md)  
**01-07:** ✅ Main Screen & Settings (Flutter rewrite) - [PLAN](phases/01-foundation-data-safety/01-07-PLAN.md) | [SUMMARY](phases/01-foundation-data-safety/01-07-SUMMARY.md)

### Phase 2 Plans (Ready for Execution)

**02-01:** Foreground Service & MediaStore Observer - [PLAN](phases/02-detection-classification/02-01-PLAN.md)  
**02-02:** Photo Detection Queue - [PLAN](phases/02-detection-classification/02-02-PLAN.md)  
**02-03:** Image Preprocessing & ML Classification - [PLAN](phases/02-detection-classification/02-03-PLAN.md)  
**02-04:** Classification Engine - [PLAN](phases/02-detection-classification/02-04-PLAN.md)  
**02-05:** Auto-Sort Execution - [PLAN](phases/02-detection-classification/02-05-PLAN.md)  
**02-06:** Monitoring UI & Queue Review - [PLAN](phases/02-detection-classification/02-06-PLAN.md)  

*Note: Previous Kotlin implementation summaries archived. Plans re-executing for Flutter.*

---

## Current Focus

**Active Phase:** Phase 2 — Detection & Classification Pipeline  
**Goal:** App automatically detects new photos, classifies them using on-device ML, and moves them to appropriate folders without user interaction for high-confidence matches.

### Current Priorities

1. **Foreground Service**: Set up flutter_foreground_task for persistent monitoring
2. **MediaStore Observer**: Implement ContentObserver via platform channels  
3. **Photo Queue**: Create detection queue with pending photo tracking
4. **Image Preprocessing**: Downscale to 224x224 before ML classification
5. **ML Classification**: Run google_mlkit_image_labeling in background
6. **Classification Engine**: Map labels to categories with confidence thresholds
7. **Auto-Sort**: Execute safe file moves for high-confidence matches
8. **Monitoring UI**: Build screen for service status and queue review

### Phase 2 Requirements

| Requirement | Description | Status |
|-------------|-------------|--------|
| MON-01 | Foreground service maintains persistent notification | Planned |
| MON-02 | MediaStore ContentObserver watches for new photos | Planned |
| MON-03 | Service restarts automatically after device reboot | Planned |
| MON-04 | Service handles Doze mode gracefully | Planned |
| MON-05 | WorkManager schedules ML jobs with battery constraints | Planned |
| ML-01 | ML Kit Image Labeling analyzes photos | Planned |
| ML-02 | Classification maps ML labels to categories | Planned |
| ML-03 | Confidence threshold (0.7) determines auto-sort | Planned |
| ML-04 | Low-confidence photos queue for user decision | Planned |
| ML-05 | Photos downscaled to 224x224 for performance | Planned |
| CAT-03 | Categories map to ML Kit labels with thresholds | Planned |
| CAT-04 | Hierarchical categories supported | Planned |
| IMG-01-05 | JPEG, PNG, WebP, HEIC support | Planned |
| ERR-02 | Corrupt images skipped gracefully | Planned |

### Blockers

None

### Current Session

**Last session:** 2026-03-08T02:40:51Z
**Duration:** 11 min
**Status:** Trash system implementation complete

### Decisions Made

| Decision | Plan | Rationale |
|----------|------|-----------|
| Confidence threshold default 0.9 | 01-01 | Per user decision for High confidence |
| Transaction log state machine (PENDING → COMPLETED) | 01-01 | Enables crash recovery |
| Foreign keys with CASCADE delete | 01-01 | Photo metadata auto-removes when folder deleted |
| Proto DataStore over SharedPreferences | 01-01 | Type-safe, migration-friendly |
| System folder blacklist | 01-02 | Filter Android, .thumbnails, .trash during discovery |
| Result<T> pattern for SAF operations | 01-02 | Type-safe error propagation without exceptions |
| Offline-first repository pattern | 01-02 | DB is source of truth, SAF is data source |
| Size verification over hash-based | 01-03 | Performance on mobile, sufficient for use case |
| 100MB minimum storage buffer | 01-03 | Safety margin prevents mid-operation failures |
| Three retry strategies (default/conservative/aggressive) | 01-03 | Different criticality needs different retry behavior |
| Single-screen onboarding flow | 01-04 | Per user decision - no wizard, tabs, or pages |
| SAF only (no MANAGE_EXTERNAL_STORAGE) | 01-04 | Play Store compatibility, per user constraints |
| Animated transitions between states | 01-04 | Better UX with AnimatedContent API |
| Learning threshold 0.5 vs organization 0.9 | 01-05 | Lower threshold captures more labels during learning; higher ensures accuracy during organization |
| 50 photos sampled per folder | 01-05 | User decision: sufficient for pattern recognition without excessive processing |
| Folder name boosts matching labels | 01-05 | Exact +15%, partial +8% - combines visual and contextual signals |
| Bundled ML Kit model (5.7MB) | 01-05 | Works offline, no API keys needed |
| 7-day retention period for trash | 01-06 | Matches desktop OS conventions for user safety |
| Hidden .trash folder with .nomedia | 01-06 | Prevents gallery apps from showing deleted items |
| KEEP policy for periodic work | 01-06 | Prevents duplicate work requests on multiple schedules |
| Default confidence threshold 0.9 (High) | 01-07 | Conservative organization with fewer mistakes |
- [Phase 01-foundation-data-safety]: Size verification over hash-based for performance on mobile

### Decisions Pending

| Decision | Context | Impact |
|----------|---------|--------|
| Android 14 service type | Foreground service type (`dataSync` vs `mediaPlayback`) | Phase 2 implementation |
| Play Store policy strategy | `MANAGE_EXTERNAL_STORAGE` vs SAF only | Phase 1 onboarding design |

---

## Completed Artifacts

| Artifact | Location | Phase | Date |
|----------|----------|-------|------|
| Project Charter | PROJECT.md | N/A | 2025-03-06 |
| Research Summary | research/SUMMARY.md | N/A | 2025-03-06 |
| Requirements v1 | REQUIREMENTS.md | N/A | 2025-03-06 |
| Roadmap | ROADMAP.md | N/A | 2025-03-06 |
| State Tracking | STATE.md | N/A | 2025-03-06 |
| 01-01 Database Foundation | phases/01-foundation-data-safety/01-01-SUMMARY.md | 1 | 2026-03-06 |
| 01-02 SAF Integration | phases/01-foundation-data-safety/01-02-SUMMARY.md | 1 | 2026-03-08 |
| 01-03 Safe File Operations | phases/01-foundation-data-safety/01-03-SUMMARY.md | 1 | 2026-03-08 |
| 01-04 Onboarding Flow | phases/01-foundation-data-safety/01-04-SUMMARY.md | 1 | 2026-03-06 |
| 01-05 Folder Learning System | phases/01-foundation-data-safety/01-05-SUMMARY.md | 1 | 2026-03-06 |
| 01-06 Trash System | phases/01-foundation-data-safety/01-06-SUMMARY.md | 1 | 2026-03-08 |

---

## Quality Gates

### Phase 1 Exit Criteria

Before transitioning to Phase 2, the following must be true:

- [ ] User can create and persist 10 categories
- [ ] SAF onboarding grants permissions for target folders
- [ ] Transaction log records all file operations
- [ ] Crash recovery test passes (simulate mid-move crash)
- [ ] Storage full condition handled gracefully
- [ ] All Phase 1 success criteria verified

### Definition of Done (per requirement)

- [ ] Code implemented and reviewed
- [ ] Unit tests pass (>80% coverage)
- [ ] Instrumented tests pass (if UI-related)
- [ ] Documentation updated
- [ ] No critical/blocker bugs

---

## Metrics

| Metric | Current | Target |
|--------|---------|--------|
| Requirements implemented | 6/35 | 35/35 |
| Success criteria verified | 0/16 | 16/16 |
| Plans completed | 6/7 | 7/7 |
| Test coverage | 0% | >80% |
| Open blockers | 0 | 0 |

---

## Notes

- **Phase 1 Start:** 2025-03-06
- **Estimated Phase 1 End:** 2025-03-27 (3 weeks)
- **Next checkpoint:** Weekly, every Friday
- **Emergency contacts:** N/A (personal project)

---

*Document updated: 2026-03-06*  
*Last session: Completed 01-07-PLAN.md (45 min)*  
*Update frequency: Daily during active development, weekly otherwise*
