# Changelog

All notable changes to the **Task Board 3D** Todo Application are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
  - Touch-friendly drag targets
- **Dark Theme**: Full dark mode with CSS custom properties

---

## [Unreleased] - 2025-12-21

### Added
- **Mobile Sidebar Refinement** - Enhanced mobile experience with permanent push layout:
  - **Push Layout**: Sidebar now pushes/shrinks main content instead of overlaying it, keeping both visible.
  - **Relative Width**: Converted sidebar width to `12.5em` for better scaling with device font settings.
  - **Word Wrapping**: Implemented `break-word` wrapping for long swimlane names in the sidebar.
  - **Cleaner UI**: Removed header hamburger and footer collapse buttons in favor of the persistent navbar trigger.
- **AOP-Based Idempotency** - Comprehensive protection against duplicate operations using Spring AOP:
  
  **Custom Annotation (`@Idempotent`):**
  - Declarative idempotency via annotation on service methods
  - SpEL expressions for flexible key generation
  - Configurable time window (default 5 seconds)
  
  **AOP Aspect (`IdempotencyAspect.java`):**
  - Intercepts `@Idempotent` annotated methods
  - Evaluates SpEL expressions against method parameters
  - Throws `DuplicateOperationException` (→ 409 Conflict) for duplicates
  - Automatic key cleanup after operation completes
  
  **Frontend Layer (store.js & app.js):**
  - Loading state flags for all mutating operations
  - Guard clauses prevent duplicate API calls
  - Modal spinners show during operations
  
  **Idempotency Coverage Matrix:**
  | Operation | Frontend | Backend | Key Expression |
  |-----------|:--------:|:-------:|----------------|
  | **Task Operations** ||||
  | Create Task | ✅ | ✅ | `'createTask:' + #task.name + ':' + #task.swimLane.id` |
  | Delete Task | ✅ | ✅ | `'deleteTask:' + #id` |
  | Update Task | ❌ | ❌ | *(Inherently idempotent - same update = same result)* |
  | Move Task | ❌ | ❌ | *(Preserves drag-drop responsiveness)* |
  | **Lane Operations** ||||
  | Create Lane | ✅ | ✅ | `'createLane:' + #swimLane.name` |
  | Delete Lane | ✅ | ✅ | `'deleteLane:' + #id` |
  | Complete Lane | ✅ | ✅ | `'completeLane:' + #id` |
  | Reorder Lanes | ✅ | ✅ | `'reorderLanes:' + #orderedIds.hashCode()` |
  | **Comment Operations** ||||
  | Add Comment | ✅ | ✅ | `'addComment:' + #taskId + ':' + #text.hashCode()` |
  | Update Comment | ✅ | ✅ | `'updateComment:' + #commentId + ':' + #newText.hashCode()` |
  | Delete Comment | ✅ | ✅ | `'deleteComment:' + #taskId + ':' + #commentId` |


- **CacheWarmupService** (`a0c5e6f`) - Pre-warms user and task caches on application startup to ensure O(1) retrieval for first-time requests.
- **CacheLoggingInterceptor** (`2a9c690`) - Real-time monitoring of cache hits/misses for API endpoints, logged with `[CACHE HIT]` or `[CACHE MISS]` prefixes.
- **Spring Security Integration** (`e54629b`) - Comprehensive authentication and authorization flow:
  - Custom `UserDetailsService` for database-backed authentication.
  - Password hashing via `BCryptPasswordEncoder`.
  - Session management with support for up to 5 concurrent sessions per user.
  - CSRF protection enabled for web routes, disabled for `/api/**` to support programmatic access.
- **User Data Isolation** (`d300ad3`) - All swimlane and task operations are scope-filtered by the currently authenticated user.
- **Performance Optimizations** (`838b587`, `a704080`)
  - Self-hosted Font Awesome with `font-display: swap` for improved First Contentful Paint (FCP).
  - Gzip compression for all text-based assets.
  - HikariCP connection pool tuning for high-concurrency database access.
  - Database-level indexes on frequently queried columns (`user_id`, `status`, `position_order`).

### Fixed
- **Branch Coverage** (`ed47726`, `6a0bcd4`) - Increased branch coverage from **34% → 90%** through rigorous testing of services and models.
- **Session Management** (`6a2826d`) - Added EOD auto-logout and back-button prevention after logout for enhanced security.
- **Login UI Bugs** (`d8a7120`, `52131dc`)
  - Fixed Chrome autofill text visibility issues.
  - Added Bootstrap JS for functional logout dropdowns.
- **Tag Filter User Data Isolation** - Fixed bug where tag filter bar showed tags from all users' tasks instead of just the current user's tasks.
  - Added `getTasksForCurrentUser()` method to `TaskService` to filter tasks by user ownership.
  - Updated `HomeController.index()` to use the new filtered method.

---

## [1.3.1] - 2025-12-08

### Chores
- **Comprehensive .gitignore** (`020ddf4`) - Added 90+ entries organized by category:
  - Build outputs, Maven/Gradle artifacts, IDE configs (IntelliJ, Eclipse, VS Code, NetBeans)
  - OS-specific files, logs, Spring Boot, Java artifacts, environment files, temp files

### Fixed
- **TaskControllerTest parallel execution** (`3b3381e`) - Made assertions resilient to parallel test runs by checking task existence rather than exact counts/positions

### Added
- **Task Filter Buttons** (`44d6866`) - Navbar buttons to filter tasks:
  - "Hide Done" - Hides all completed tasks (green when active)
  - "Blocked Only" - Shows only blocked tasks (red when active)

- **Build Time Optimizations** (`44d6866`)
  - Enabled incremental compilation for faster rebuilds
  - Changed test parallelism from fixed 4 threads to unlimited threads per CPU core

- **Add Task Dialog Enhancements** (`08ecf07`)
  - Multi-line textarea for task name/description
  - Status dropdown with styled dark theme options
  - Chip-based tag input system (Enter to add, click to remove)
  - Prominent swimlane name header with purple gradient
  - Bold, uppercase field labels for better visibility
  
- **Toast Notifications** (`08ecf07`)
  - Slide-down animation from top of screen
  - Fixed lane reorder toast not appearing (this context issue)
  - Flush positioning with screen edge (no gap)
  - Error toast variant with red styling

- **Collapse/Expand All Lanes** button in navbar

- **Lane Progress Pills** - Status distribution shown inline in swimlane headers

### Fixed
- **Lane reorder toast notification** - Toast now properly appears after lane drag
  - Root cause: `initLaneSortable` was receiving `Store` module instead of Alpine component
  - Fix: Pass `this` (Alpine component) to maintain reactive context
- **Orphan HTML elements** - Removed stray `<i>`, `</button>`, `</div>` tags from index.html
- **Dropdown overflow** - Fixed modal overflow preventing dropdown options from displaying

### Documentation
- **GEMINI.md** - Reorganized Prime Directives into 3 lifecycle stages:
  - Stage 1: Pre-Implementation (Stop & Think, Understand Scope, Plan Approach)
  - Stage 2: During Implementation (Browser Verification, Commit Cycle, Test Coverage)
  - Stage 3: Post-Implementation (Commit, Update Changelog, Update GEMINI.md)
- **GEMINI.md** - Added Backend Performance Optimizations section documenting:
  - Async Write-Behind Pattern (`AsyncWriteService.java`)
  - Spring Cache integration (`@Cacheable`, `@CacheEvict`)
  - HikariCP connection pool configuration
  - Database indexes (7 composite indexes on SwimLane, Task, User entities)
  - Bulk UPDATE queries for position shifts
  - Gzip compression settings
  - Open-in-view disabled for N+1 prevention

### Test Fixes
  - `AsyncWriteServiceTest` - Added missing `SseService` mock
  - `TaskControllerTest` - Updated assertions for optimistic async responses
  - `SwimLaneControllerTest` - Made assertions resilient to parallel execution
- **Modal Scroll Lock** (`55b0b9e`) - Page no longer scrolls when modals are open
  - Applies to New Board, New Task, and confirmation modals
  
### Tests
- **Comprehensive Test Coverage Improvements** (`008c88f`)
  - Coverage increased from **60.7% → 93%** instructions, **30.3% → 74%** branches
  - Added 47 new test cases across 9 test files
  
- **New Test Files**:
  - `SseServiceTest` - Tests for subscribe, broadcast, heartbeat methods
  - `SseControllerTest` - Integration test for SSE stream endpoint
  - `StartupLoggerTest` - Tests for startup logging and error handling
  
- **Enhanced Test Files**:
  - `SwimLaneServiceTest` - Added `reorderSwimLanes`, null position, exception cases
  - `TaskServiceTest` - Added legacy comment parsing, malformed JSON, swimlane not found
  - `AsyncWriteServiceTest` - Added skip shift conditions, task not found scenarios
  - `SwimLaneDAOTest` - Added `findMaxPosition`, `saveAll`, `findAllById`
  - `SwimLaneControllerTest` - Added reorder endpoint test
  - `TaskControllerTest` - Added JSON-wrapped text, comment error handling
  
- **Classes at 100% Coverage**:
  - `SwimLaneDAO`, `TaskDAO`, `SwimLaneController`, `SseController`, `SwimLaneService`, `TaskStatus`
  
- **Total Tests**: 103 (all passing)


---

## [1.3.0] - 2025-12-07 — Drag-and-Drop Stability & Documentation
- Added event lifecycle reference

### Technical Details
| Commit | Time | Description |
|--------|------|-------------|
| `0570f48` | Latest | Premium toast + task position support |
| `b9590e1` | Latest | Core task management with async writes |
| `5bd2b30` | Latest | Comprehensive API logging |
| `3657a12` | Dec 7 | GEMINI.md drag-drop reference |
| `01eb1c4` | Dec 7 | Logging documentation |
| `a7c7a64` | Dec 7 | Sortable reinit fix + CSS fixes |
| `230c0a8` | Dec 7 | Per-column x-init + z-index fix |
| `7b797ae` | Dec 7 | Restore drag-drop + verbose logging |
| `f266408` | Dec 7 | Resolve drag conflicts + observability |

---

## [1.2.0] - 2025-12-06 — Alpine.js Store & Frontend Rewrite

### Added
- **Alpine.js Store** for centralized state management of tasks and swimlanes
### Technical Details
| Commit | Time | Description |
|--------|------|-------------|
| `8b71ec2` | Dec 6 | Alpine.js store implementation |
| `7f991b6` | Dec 6 | Initial Alpine.js frontend |
| `0e6f848` | Dec 6 | Per-column Alpine x-init |
| `904d78d` | Dec 6 | Custom delete modal |
| `7648b23` | Dec 6 | Modular import structure |
| `9274c37` | Dec 6 | Modular frontend rewrite (v11) |
  - **Issue**: `openTaskModal` function not found
  - **Attempts**: Cache busting, script reordering, Alpine presence logging
  - **Solution**: Switched from Bootstrap Offcanvas to custom Alpine pane
-  **Swimlane Drag Init and Stats Visibility**
- **Reactive Swimlane Stats Binding** - Stats now update in real-time
- **Missing `div` Tag** in swimlane header
- **Collapsed Class Binding** for arrow rotation animation
- **Persisted Loader Issue** - Loading spinner would not dismiss
- **Duplicate Keys in `x-for`** - Safe array update pattern
- **Sortable.js Error in SSE Handler**
- **Compilation Error in AsyncWriteService**
- **Loose Equality for Swimlane ID** comparison
- **Side Pane Height** issues

### Refactored
- `showNotification` refactored to use pure CSS
- **Bootstrap Offcanvas → Custom Alpine Pane** for task details
- Axios request/response logging added
- Redundant thread logging removed

### Technical Details - Morning Session (Dec 5)
| Commit | Time | Description |
|--------|------|-------------|
| `2430e92` | 20:45 | Initial ToDo app with SSE |
| `0c7f373` | 05:34 | Async Write-Behind Caching |
| `8db2b45` | 02:47 | List View liquid width |
| `3b29b66` | 02:42 | List View vertical center |
| `b6aa58c` | 02:39 | View Switcher button group |

### Technical Details - Evening Session (Dec 5)
| Commit | Time | Description |
|--------|------|-------------|
| `86ffd32` | 20:57 | Swimlane drag-and-drop |
| `afdae50` | 20:53 | Gray out empty swimlanes |
| `f8e03d7` | 20:40 | Enhanced swimlane headers |
| `b0f5cbf` | 20:16 | Smart expansion + spinners |
| `47803822` | 20:23 | Top-center notifications |

### Technical Details - Night Session (Dec 5, debugging)
| Commit | Time | Description |
|--------|------|-------------|
| `779bf82` | 23:25 | Global function pattern |
| `7b763e2` | 23:12 | Debug factory execution |
| `572fb86` | 23:04 | Debug Alpine presence |
| `8e0ed7b` | 22:07 | Replace Bootstrap Offcanvas |
| `117085c` | 22:02 | Axios logging |

---

## [1.0.0] - 2025-12-04 — Testing Infrastructure & Core Features

### Added
- **Async Write-Behind Caching** for improved write performance
- **Test Summary Extension** with execution time reporting
- **Integration Test Infrastructure**:
  - `BaseIntegrationTest` for Spring context reuse
  - Parallel test execution support
  - Controller integration tests (upgraded from unit tests)
- **Comprehensive Test Coverage** for DAOs, Controllers, and Services

### Fixed
- **TestSummaryExtension** reliability with `CloseableResource` pattern

### Refactored
- Moved `BaseIntegrationTest` to `com.example.todo.base` package
- View switcher converted to Bootstrap button group

### Chores
- Added comprehensive `.gitignore` for build artifacts and IDE files
- Apache Maven directory exclusion
- `.agent` folder handling

### Technical Details
| Commit | Time | Description |
|--------|------|-------------|
| `0c7f373` | 05:34 | Async Write-Behind Caching |
| `3c5e922` | 12:33 | BaseIntegrationTest reorganization |
| `c7fdd67` | 12:23 | Controller integration tests |
| `9af48af` | 12:14 | Parallel test execution |
| `bfd885c` | 14:52 | DAO test coverage |
| `7f3a098` | 14:11 | Controller/Service test coverage |

---

## [0.3.0] - 2025-12-03 — Test Coverage Expansion

### Added
- Missing test cases for Data Access Objects (DAOs)
- Missing test cases for Controllers and Services
- Apache Maven path to gitignore

### Technical Details
| Commit | Time | Description |
|--------|------|-------------|
| `bfd885c` | 14:52 | DAO tests |
| `7f3a098` | 14:11 | Controller/Service tests |
| `e85dcf1` | 15:41 | Gitignore update |

---

## [0.2.0] - 2025-12-01 — UI Polish & Animation

### Added
- **3D Flip Animation** for view toggle (vertical rotateX)
- **Global Loading Indicator** (later refactored out)
- H2 database configuration for CI testing

### Fixed
- **Offcanvas Height Bug** by relocating outside view-stack (later reverted)
- **Jittery View Transition** with opacity fade
- **Dynamic Drag Tinting** - multiple attempts:
  - Initial implementation broke drag functionality
  - `forceFallback` mode attempt (reverted - performance issues)
  - CSS-based re-implementation (still problematic)
  - **Final Resolution**: Feature completely removed to restore stability

### Removed
- Dynamic drag tinting feature (stability issues)

> [!WARNING]
> **Issue Encountered**: Dynamic drag tinting was attempted multiple times over a 17-minute window.
> Each attempt caused drag-and-drop to break. The feature was ultimately abandoned.

### Technical Details - Drag Tinting Timeline
| Time | Commit | Action |
|------|--------|--------|
| 23:20 | `e2ded22` | Initial implementation |
| 23:22 | `13cc913` | Revert - broke drag |
| 23:26 | `74d22db` | CSS-based re-implementation |
| 23:32 | `d37cb17` | Enable forceFallback |
| 23:35 | `beefe70` | Revert forceFallback |
| 23:37 | `0239ece` | **Complete removal** |

---

## [0.1.0] - 2025-11-30 — Initial Release

### Added
- **Core Task Board Application** with swimlane-based organization
- **Kanban Board View** with TODO, IN_PROGRESS, DONE, BLOCKED, DEFERRED columns
- Body background class removal

### Removed
- Azure deployment workflow (simplified deployment strategy)

### Technical Details
| Commit | Time | Description |
|--------|------|-------------|
| `4baf23a` | 17:46 | Initial commit |
| `de1683b` | 22:24 | Global loading indicator |
| `7ebb49f` | 23:06 | x-collapse animation |
| `EB05642` | 23:10 | toggleLaneCollapse Alpine.js |

---

## Issue Resolution Summary

### Major Issues Encountered & Solutions

#### 1. Drag-and-Drop Initialization Timing (Dec 6-7)
- **Symptom**: Tasks not draggable after page load
- **Root Cause**: Sortable.js initialized on empty DOM (tasks loaded async)
- **Solution**: Added `reinitSortableForLane()` with `$nextTick()` callback
- **Commits**: `a7c7a64`, `230c0a8`, `7b797ae`, `f266408`

#### 2. Alpine.js Component Discovery (Dec 5)
- **Symptom**: `openTaskModal is not defined` error
- **Root Cause**: Bootstrap's JavaScript conflicting with Alpine.js scope
- **Debugging Attempts**: Cache busting, script reordering, logging
- **Solution**: Replaced Bootstrap Offcanvas with pure Alpine.js pane
- **Commits**: `8e0ed7b`, `779bf82`, `acc22cf`, `bb35b72`

#### 3. Dynamic Drag Tinting (Nov 30)
- **Symptom**: Visual enhancement broke core drag functionality
- **Root Cause**: CSS transforms and forceFallback interference
- **Resolution**: Feature removed entirely - stability > aesthetics
- **Commits**: `e2ded22` → `0239ece` (5 attempts, final removal)

#### 4. CSS 3D Transforms Breaking Drag Events (Dec 7)
- **Symptom**: Click events not reaching task cards
- **Root Cause**: `perspective`, `transform-style: preserve-3d`, `translateZ` disrupting hit-testing
- **Solution**: Removed 3D transforms from draggable elements, documented in GEMINI.md
- **Commits**: `3657a12`, `01eb1c4`

---

## Commit Convention Reference

This project uses conventional commit prefixes:

| Prefix | Usage |
|--------|-------|
| `feat:` | New feature |
| `fix:` | Bug fix |
| `refactor:` | Code restructuring |
| `docs:` | Documentation |
| `test:` | Test additions/fixes |
| `chore:` | Build/config changes |
| `style:` | Formatting (no logic change) |
| `UI:` | User interface changes |
| `Debug:` | Temporary debug commits |
| `WIP:` | Work in progress |
| `Undo:` | Revert/undo changes |

---

## Timeline Visualization

```
Nov 30, 2025  ──┬── Initial Commit & Core Features
               └── Drag Tinting Saga (17min, 5 attempts, abandoned)

Dec 01, 2025  ──┬── 3D Flip Animation
               └── Offcanvas Height Fixes

Dec 03, 2025  ────── Test Coverage (DAOs, Controllers, Services)

Dec 04, 2025  ──┬── Test Infrastructure (Integration, Parallel)
               └── Async Write-Behind Caching

Dec 05, 2025  ──┬── AM: SSE + Async Writes
               ├── PM: Swimlane Enhancements
               └── Night: Alpine/Bootstrap Conflict (8+ commits)

Dec 06, 2025  ──┬── Frontend Rewrite to Alpine.js
               └── Modular Architecture (Store, API, Drag)

Dec 07, 2025  ──┬── Drag-Drop Stability Fixes
               └── GEMINI.md Documentation Expansion

Dec 08, 2025  ──┬── Task Position Support
               └── Premium Toast Notifications
```

---

*Changelog generated on 2025-12-08 scanning the `dev` branch.*
