# GEMINI.MD - OPTIMIZED EXECUTION PROTOCOL (100% COMPLIANCE VERSION)

## 🚨 AGENT ROLE (NON-NEGOTIABLE - READ FIRST)
**YOU ARE GEMINI-AGENT**: Ruthless protocol enforcer. 

**SINGLE MISSION**: Execute **EVERY** checklist step **IN EXACT ORDER**.

**VIOLATION PROTOCOL**: Skip any step → **IMMEDIATELY STOP** + "❌ VIOLATION: Missing Step X" + **SELF-CORRECT**.

## ❌ ABSOLUTE FAILURE MODES (NEVER DO THESE)
❌ Skip browser verification (ALWAYS inject first)  
❌ Commit without: restart → mvn test → user approval  
❌ Forget tests + CHANGELOG + GEMINI.md updates  
❌ Say "done" without ✓ responses for ALL steps  
❌ Open new browser tabs (ONLY localhost:8080 existing tab)  
❌ Merge to main automatically (NEVER)  
❌ Skip console.log('[App] ...') for frontend changes  

---

## ✅ EXECUTION CHECKLIST (MANDATORY - RESPOND ✓ FOR EACH STEP)

### STAGE 1: PRE-IMPLEMENTATION (COMPLETE BEFORE CODING)

1. **STOP & THINK**  
    - **PAUSE** before every change. Ask: "Is this the **BEST**/**CLEANEST**/**EFFICIENT**/**SCALABLE**/**MAINTAINABLE**/**TESTABLE**/**EXTENSIBLE**/**PERFORMANT**/**SECURE**/**RELIABLE**?"
   - PROPOSE better alternatives if you find the request aor any part of solution sub-optimal.
   - Try to think of unique and innovative solutions with user guidance where necessary.
   - REFUSE bad patterns: explicitly say `"REJECTED: Sub-optimal pattern, proposing X instead"`.

2. **UNDERSTAND SCOPE**  
   - READ all relevant sections of this `GEMINI.md` to understand existing patterns.  
   - IDENTIFY which files/components will be affected.  
   - CHECK for existing similar implementations to maintain consistency.

3. **PLAN APPROACH**  
   - OUTLINE a clear, step-by-step approach for the change before coding.  
   - If requirements are ambiguous, ASK the user to confirm:  
     `"Here is my understanding and plan: ... Can you confirm or correct it?"`

**BEFORE STARTING ANY CODING YOU MUST RESPOND INTERNALLY**:  
`"STAGE 1 ✓ ALL COMPLETE: Ready for implementation."`

---

### STAGE 2: IMPLEMENTATION CYCLE (APPLIES TO EVERY CHANGE)

#### 2.1 FRONTEND VERIFICATION PROTOCOL (THE GOLDEN RULE)
4. **BROWSER-FIRST CHANGES (NEVER EDIT SOURCE BLINDLY)**  
   - ALWAYS verify JS/HTML/CSS changes by **injecting into the running browser tab** first:  
     - Use **Console** for JS logic.  
     - Use **DOM editing** for HTML structure.  
     - Use **Style editor or DevTools** for CSS.  
   - ONLY after the injected change works correctly in the browser, propagate it into source files.  
   - EVERY frontend change MUST include at least one log line of the form:  
     `console.log('[App] <meaningful message>')` to prove execution.

#### 2.2 BROWSER USAGE
5. **SINGLE TAB RULE**  
   - ALWAYS reuse the **already open** tab for `http://localhost:8080`.  
   - NEVER open a new tab for localhost unless the user explicitly asks.

#### 2.3 COMMIT & RESTART CYCLE
6. **VALIDATION FIRST (MANDATORY ORDER)**  
   For each meaningful change set:

   1. Apply source code changes.  
   2. Restart the application:  
      ```
      mvn spring-boot:run
      ```  
   3. Verify functionality via the browser (using the same tab as above).  
   4. Run tests:  
      ```
      mvn test
      ```  
      - Ensure new features / bug fixes have corresponding unit or integration tests.  
    - **VERIFY** tests pass before asking for commit approval.
- **BRANCH COVERAGE**: Maintain >90% branch coverage for service and utility classes.  
- **NO BROWSER/UI TASKS (PERMANENT RULE)**: Never perform UI tasks or browser-based validations/testing. The USER will handle all visual and UI-related testing and verification. Do not use `browser_subagent` for these purposes.
   5. ASK THE USER FOR PERMISSION TO COMMIT:  
      - Message format:  
        `"Validation complete: app restarted, browser verified, tests passing. Do you approve a commit now?"`

7. **COMMIT ONLY AFTER APPROVAL**  
    - Once the user explicitly approves:
        ```
        git commit -am "feat/fix: Brief description of the change"
        ```
- Use `feat:` for new features.  
- Use `fix:` for bug fixes.  
- Use `refactor:` for refactoring.  
- Use `docs:` for documentation-only changes.

#### 2.4 MANDATORY TEST COVERAGE
8. **TESTS ARE NOT OPTIONAL**  
- EVERY new feature or bug fix MUST have corresponding tests (unit or integration).  
- DO NOT consider work "done" unless tests exist and pass.  
- **MANDATORY COVERAGE**: Maintain >93% instruction and branch coverage across ALL packages.
- If the user explicitly requests skipping tests, warn them and clearly restate what is being skipped.

#### 2.5 CHANGELOG MANAGEMENT
9. **ALWAYS UPDATE CHANGELOG**  
- For EVERY significant change (feature, fix, refactor), UPDATE:  
  `todo-app/CHANGELOG.md`  
- Under `[Unreleased]`, add entries in the correct section (`Added`, `Changed`, `Fixed`, etc.).  
- DO NOT defer this; it is part of the implementation, not an afterthought.

**AFTER COMPLETING ALL ITEMS 4–9 FOR A CHANGESET, YOU MUST INTERNALLY CONFIRM**:  
`"STAGE 2 ✓ CHANGE CYCLE COMPLETE: [files updated, tests passing]."`

---

### STAGE 3: POST FEATURE IMPLEMENTATION (TRIGGERED BY USER CONFIDENCE)

When the user says they are **confident of the changes** (or uses similar wording like "looks good", "approved", "ship it"):

10. **COMMIT (FINAL)**  
 Create a semantic commit if not already created for the final state:

 ```
 git commit -am "feat/fix: Brief description of the final change"
 ```

11. **UPDATE CHANGELOG**  
 - Ensure `todo-app/CHANGELOG.md` under `[Unreleased]` includes:  
   - What was added/changed/fixed.  
   - Any relevant implementation details or entry points.  
   - References to key files or components.

12. **UPDATE GEMINI.MD**  
 - REVIEW this file (`GEMINI.md`) after the feature is implemented.  
 - ADD new sections for new features, APIs, or architectural patterns that should guide future work.  
 - UPDATE existing sections if behavior or patterns changed.  
 - DELETE outdated or misleading instructions.

12.1 **MANDATORY PRE-PUSH TEST**  
- **ALWAYS** run `mvn test` immediately before pushing the `main` branch to `remote origin`.  
- If any test fails, **ABORT** the push and fix the issue.

13. **NO AUTOMATIC MERGE TO MAIN**  
 - NEVER merge to `main` or push to `main` unless the user explicitly says so.  
 - DO NOT ask or suggest merging to main. Wait for the user to initiate.
 - **CRITICAL**: Before pushing to `main`, ensure `mvn test` has passed in the final state.

14. **FINAL CONFIRMATION MESSAGE**  
 - After performing all required documentation and commit tasks, summarize:  
   `"ALL STAGES ✓ COMPLETE: Code, tests, CHANGELOG, and GEMINI.md are now in sync."`

---

# PROJECT: To-Do and Reminders Application

## Project Overview
This is a full-stack, single-page web application for managing tasks and to-do items. It presents a "Task Board 3D" interface with swimlanes for organizing tasks. The application is built with a Java/Spring Boot backend and a dynamic frontend that uses Thymeleaf for initial rendering and Alpine.js for interactivity.

### Key Features
- **3D Kanban Board**: Visually distinct swimlanes with 3D depth effects and collapsible headers.
- **Interactive Statistics**: Collapsible accordion dashboard with 3D pie and bar charts.
- **Status Pills**: Proportional progress visualization in swimlane headers showing task distribution (percentage + count).
- **Drag-and-Drop**: Intuitive task management powered by Sortable.js with cross-column support.
- **Mobile Optimized**: Responsive layout with persistent push-sidebar navigation and bottom-anchored task details.
- **Mobile Liquid UI**: Responsive push-based layout where the bottom navigation bar compressively "squeezes" its buttons to the right when the sidebar is open.
- **Glassmorphism UI**: Modern, translucent dark theme design aesthetic with dynamic CSS transitions.
- **Task Filters**: "Hide Done" and "Blocked Only" filter buttons in navbar.
- **Tag Filter Bar**: Sticky, horizontally scrollable filter bar below the navbar.
  - **Faceted Logic**: Displays only tags present on tasks matching the current selection.
  - **Smart Selection**: Dynamic sorting and auto-collapse of lanes during filtering on desktop.
- **Task Card Resize**: Drag resize handle on task cards to adjust height; double-click to reset.
  - **Horizontal Resize**: Dragging significantly to the right/left expands/shrinks the entire column width.
- **Toast Notifications**: Slide-down Success (green) and Error (red) notifications.
- **Real-Time Sync**: Server-Sent Events (SSE) for multi-client synchronization.
- **Async Write-Behind**: Optimistic UI updates with queued background database writes.
- **AOP Idempotency**: Transparent protection against duplicate operations using Spring AOP.

## Architecture

The application follows a standard layered Spring Boot architecture:

```
┌─────────────────────────────────────────────────────────────┐
│  Controller Layer                                           │
│  └── TaskController, SwimLaneController, SseController      │
├─────────────────────────────────────────────────────────────┤
│  Service Layer                                              │
│  └── TaskService, SwimLaneService, AsyncWriteService        │
├─────────────────────────────────────────────────────────────┤
│  DAO Layer                                                  │
│  └── TaskDAO, SwimLaneDAO                                   │
├─────────────────────────────────────────────────────────────┤
│  Repository Layer (Spring Data JPA)                         │
│  └── TaskRepository, SwimLaneRepository                     │
├─────────────────────────────────────────────────────────────┤
│  Database: PostgreSQL                                       │
└─────────────────────────────────────────────────────────────┘
```

## Technology Stack

### Backend
- **Java 21**
- **Spring Boot 3.3.0** (Web, Data JPA)
- **PostgreSQL**: Primary database (Azure-hosted in prod, local in dev).
- **Maven**: Build tool.
- **Log4j2**: Logging framework.

### Frontend
- **Alpine.js (v3.x)**: Lightweight reactive framework loaded via ESM.
- **Bootstrap 5.3.0**: UI Components, Grid system.
- **Axios**: HTTP client for API communication.
- **Sortable.js (v1.15.0)**: Drag-and-drop list management.
- **GSAP (v3.12.2)**: Animation library for advanced transitions.
- **Highcharts 3D**: Interactive 3D charts for statistics.
- **Font Awesome 6.4.0**: Icons.
- **Thymeleaf**: Server-side template engine (initial load only).

## Data Model

### SwimLane
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary Key |
| `name` | String | Name of the swimlane |
| `isCompleted` | Boolean | Status of the swimlane |
| `isDeleted` | Boolean | Soft delete flag |
| `position` | Integer | Display order for lane reordering |

### Task
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary Key |
| `name` | String | Task description |
| `status` | Enum | `TODO`, `IN_PROGRESS`, `DONE`, `BLOCKED`, `DEFERRED` |
| `comments` | String (JSON) | JSON array of comment objects |
| `tags` | String (JSON) | JSON array of tag strings |
| `swimLane` | SwimLane | Many-to-One relationship |
| `position` | Integer | Order within the status column |

## API Reference

### SwimLanes (`/api/swimlanes`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Get all swimlanes |
| GET | `/active` | Get active (non-completed) swimlanes |
| GET | `/completed` | Get completed swimlanes |
| POST | `/` | Create a new swimlane (`{name}`) |
| PATCH | `/{id}/complete` | Mark swimlane as complete |
| PATCH | `/{id}/uncomplete` | Reactivate swimlane |
| PATCH | `/reorder` | Reorder swimlanes (body: `Long[]` of IDs) |
| DELETE | `/{id}` | Soft delete swimlane |

### Tasks (`/api/tasks`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Get all tasks |
| GET | `/swimlane/{swimLaneId}` | Get tasks by lane (incremental loading) |
| GET | `/{id}` | Get specific task |
| POST | `/` | Create a new task |
| PUT | `/{id}` | Update task details |
| DELETE | `/{id}` | Delete task |
| PATCH | `/{id}/move` | Move task (params: `status`, `swimLaneId`, `position`) |
| POST | `/{id}/comments` | Add comment |
| PUT | `/{id}/comments/{commentId}` | Update comment |
| DELETE | `/{id}/comments/{commentId}` | Delete comment |

### Server-Sent Events (`/api/sse`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/stream` | Subscribe to real-time updates |

**SSE Event Types:**
- `init` - Initial connection established
- `task-updated` - Task was created/modified
- `task-deleted` - Task was deleted
- `lane-updated` - Lane was modified
- `heartbeat` - Keep-alive (every 10s)

## Backend Performance Optimizations

The application implements several latency reduction strategies.

### Async Write-Behind Pattern
- **File**: `AsyncWriteService.java`  
- Database writes are decoupled from HTTP responses via `@Async("asyncWriteExecutor")`.  
- Single-threaded executor ensures sequential writes without race conditions.  
- API returns quickly (~5ms) while DB writes happen in the background (~100–500ms).  
- SSE broadcasts notify clients after DB commit completes.

### Caching Layer
| Service          | Cache Name    | Key            |
|------------------|---------------|----------------|
| `TaskService`    | `tasks`       | All tasks      |
| `TaskService`    | `tasksByLane` | `swimLaneId`   |
| `SwimLaneService`| `lanes`       | `currentUserId`|

- Cache is evicted via `@CacheEvict` on create/delete operations.

### Cache Management & Monitoring
| Component                | Function                                                                 |
|--------------------------|--------------------------------------------------------------------------|
| `CacheWarmupService`     | Pre-warms `tasks` and `tasksByLane` caches on `ApplicationReadyEvent`.  |
| `CacheLoggingInterceptor`| Intercepts `/api/**` requests to log hit/miss stats via Caffeine metrics.|

### HikariCP Connection Pool
```
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
```

### Database Indexes
| Entity    | Index                                   | Columns                                   |
|-----------|-----------------------------------------|-------------------------------------------|
| `SwimLane`| `idx_swim_lanes_user_id`                | `user_id`                                 |
| `SwimLane`| `idx_swim_lanes_user_deleted_position`  | `user_id, is_deleted, position_order`     |
| `SwimLane`| `idx_swim_lanes_user_completed_deleted` | `user_id, is_completed, is_deleted, position_order` |
| `Task`    | `idx_tasks_swim_lane_id`                | `swim_lane_id`                            |
| `Task`    | `idx_tasks_status`                      | `status`                                  |
| `Task`    | `idx_tasks_lane_status_position`        | `swim_lane_id, status, position_order`    |

### Bulk UPDATE Queries
Position shifts use a single bulk UPDATE instead of N individual saves:
```sql
@Query("UPDATE Task t SET t.position = t.position + 1 WHERE ...")
int shiftPositionsDown(...);
```

### Gzip Compression
```
spring.http.compression.enabled=true
spring.http.compression.min-response-size=1024
```

## Frontend Architecture

### Module Structure
```
static/js/
├── app.js           # Alpine.js component entry point
└── modules/
    ├── store.js     # State management, modals, API wrappers
    ├── api.js       # Axios HTTP client, SSE initialization
    └── drag.js      # SortableJS integration
```

### Alpine.js Component (`app.js`)

**Reactive State:**
```javascript
{
    lanes: [],           // Array of swimlane objects
    tasks: [],           // Array of all task objects
    showSaved: false,    // Success toast visibility
    showErrorToast: false, // Error toast visibility
    errorMessage: '',    // Current error message
    columns: ['TODO', 'IN_PROGRESS', 'DONE', 'BLOCKED', 'DEFERRED'],
    hideDone: false,     // Filter: hide DONE tasks
    showOnlyBlocked: false, // Filter: show only BLOCKED tasks
    selectedTags: [],    // Tag filter: active tag filters
    taskSizes: {},       // Task resize: {taskId: {height: px}}
    ...Store             // Spread Store methods
}
```

**Key Methods:**
- `init()` - Lifecycle hook: loads lanes, fetches tasks per lane async, sets up SSE
- `getTasks(laneId, status)` - Filters tasks with filter support
- `getTags(tagsRaw)` - Parses JSON tags
- `reinitSortableForLane(laneId)` - Reinitializes Sortable after async task load
- `getAllUniqueTags()` - Returns array of all unique tags across tasks
- `toggleTag(tag)` - Toggles tag in selectedTags filter
- `isTagSelected(tag)` - Returns boolean if tag is active
- `clearSelectedTags()` - Clears all tag filters
- `laneHasMatchingTasks(laneId)` - Returns true if lane has tasks matching filter
- `startResize(event, taskId, laneId, status)` - Initiates resize drag
- `getTaskStyle(taskId)` - Returns height style for resized task
- `resetTaskSize(taskId)` - Double-click to reset task height

### State Management (`store.js`)

**Modal State:**
```javascript
modal: {
    open: false,
    title: '',
    message: '',
    type: 'info',
    confirmText: 'Confirm',
    action: null,
    payload: null
}

inputModal: {
    open: false,
    mode: 'SWIMLANE' | 'TASK',
    title: '',
    value: '',
    laneId: null,
    laneName: '',
    status: 'TODO',
    tags: [],
    tagInput: ''
}
```

**Key Methods:**
- `loadData()` - Fetches lanes from API
- `fetchLaneTasks(laneId)` - Fetches tasks for specific lane
- `moveTaskOptimistic(...)` - Optimistic task move with rollback
- `reorderLanesOptimistic(newIds)` - Optimistic lane reorder
- `triggerSave()` - Shows success toast for 1.5s
- `triggerError(msg)` / `showError(msg)` - Shows error toast for 3s
- `getLaneStats(laneId)` - Calculates task statistics for status pills

## Frontend Performance Optimizations

The application employs viewport-aware loading strategies to maintain responsiveness.

### Desktop Lazy Loading Pattern
- **Objective**: Minimize initial DOM weight and memory usage on desktop.
- **Implementation**:
  - `init()`: Detects desktop via `isMobile` check and clears `initialData.tasks`. 
  - All swimlanes start in `collapsed: true` and `tasksLoaded: false` state.
  - **Async Fetch**: Expanding a lane triggers `fetchLaneTasks(laneId)` via `toggleLaneCollapse` override.
  - **Sortable Re-init**: `reinitSortableForLane` is called after tasks DOM renders to enable drag-and-drop on lazy-loaded items.

### View Mode Management
- **States**: `ACTIVE` vs `COMPLETED`.
- **Sync Protocol**:
  - Switching `viewMode` via `setViewMode` triggers a full backend refresh for the target mode.
  - `tasks` array is cleared to prevent cross-view data leakage.

## Architectural Patterns

### AOP-Based Idempotency
- **Annotation**: `@Idempotent`  
- **Logic**: Uses Spring AOP to intercept mutating operations. Generates a unique key via SpEL expressions.
- **Storage**: `IdempotencyService` manages a time-windowed cache of operation keys (default 5s).  
- **Error Handling**: Throws `DuplicateOperationException` (409 Conflict).

### Global Exception Handling
- **Component**: `GlobalExceptionHandler`  
- **Purpose**: Centralized handling of business and runtime exceptions to return consistent JSON error responses.

### Session Management & Security
- **SSE Session Verification**: On reconnection, `api.js` calls `getUser()` to verify session validity.
- **HTML Response Handling**: Frontend API interceptors detect masked auth redirects and force login.

## UI Systems

### Notification System
#### Success Toast (`.notification-toast.success`)
- **Position**: Fixed, top center (`top: 0`, `left: 50%`, `transform: translateX(-50%)`)
- **Animation**: Slide-down from top using Alpine.js x-transition
- **Trigger**: `triggerSave()` sets `showSaved = true` for 1.5 seconds

#### Error Toast (`.notification-toast.error`)
- **Color**: Red border and icon
- **Trigger**: `showError(msg)` sets `showErrorToast = true` for 3 seconds

### Real-Time Updates (SSE Architecture)
1.  **SseService**: Manages client connections with `CopyOnWriteArrayList<SseEmitter>`.
2.  **SseController**: Exposes `/api/sse/stream` endpoint.
3.  **AsyncWriteService**: After async DB writes, broadcasts updates via SSE.
4.  **Heartbeat**: Every 30 seconds to keep connections alive.

### Drag-and-Drop System
#### Architecture Overview
```
index.html
├── .board-container     (Lane Sortable - reorder lanes)
│   └── .swimlane-row    (Lane wrapper)
│       └── .lane-column (Task Sortable - reorder tasks)
│           └── .task-card (Draggable item)
```

#### Critical Initialization Timing
- `x-init="initColumn($el)"` fires when column is CREATED.
- Tasks load asynchronously; use `reinitSortableForLane(laneId)` after DOM update.

> [!CAUTION]
> CSS properties like `perspective` or `transform-style: preserve-3d` break drag events.

### Status Pills UI
Swimlane headers display proportional status pills using `flex-grow` based on task count:
```html
<template x-if="getLaneStats(lane.id).todo > 0">
    <div class="badge status-pill-lg"
         :style="'flex-grow: ' + getLaneStats(lane.id).todo"
         x-text="getLaneStats(lane.id).todoPct + '% (' + getLaneStats(lane.id).todo + ')'">
    </div>
</template>
```

### Tag Filter Bar
- Sticky bar below navbar toggles `selectedTags`.
- Uses faceted logic to only show tags present on tasks matching current selection.
- Desktop: Automatically sorts/collapses lanes based on matches.

### Task Card Resize
- Resize handle appears on hover.
- Double-click to reset height.
- Sizes stored in `taskSizes` object: `{taskId: {height: px, initialHeight: px}}`.
- Horizontal drag (>50px) triggers column expansion via `.has-expanded-column` and `.col-expanded` classes.

## Building and Running

### Prerequisites
- Java 21
- Maven
- PostgreSQL (localhost:5432, db: todo_db)

### Running the Application
```bash
mvn spring-boot:run
```
The application will be available at [**http://localhost:8080**](http://localhost:8080).

### Running Tests
```bash
mvn test
```

---

## CHANGELOG MANAGEMENT

> [!IMPORTANT]
> **Update `CHANGELOG.md` at every significant milestone!**

### Changelog Location
- **File**: `todo-app/CHANGELOG.md`

### When to Update
- New feature implementation completed
- Major bug fix deployed
- API/Database schema changes
- Frontend architecture/performance improvements

### Changelog Format
Follows [Keep a Changelog](https://keepachangelog.com/) format with sections: `Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, `Security`.

---

**THIS VERSION OF GEMINI.MD IS OPTIMIZED FOR STRICT, STEP-BY-STEP INSTRUCTION COMPLIANCE BY THE AGENT.**
