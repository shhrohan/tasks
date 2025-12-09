# PRIME DIRECTIVES (NON-NEGOTIABLE EXECUTION PROTOCOLS)

> [!IMPORTANT]
> **YOU MUST FOLLOW THESE RULES WITHOUT EXCEPTION. THEY TAKE PRECEDENCE OVER EVERYTHING ELSE.**

---

## STAGE 1: PRE-IMPLEMENTATION

Before writing any code, complete these steps:

### 1.1 STOP & THINK (OPTIMIZATION)
*   **PAUSE** before every change. Ask: "Is this the best/cleanest way?"
*   **PROPOSE** better alternatives to the user if the original request is sub-optimal.
*   **REFUSE** blind obedience to bad patterns.

### 1.2 UNDERSTAND SCOPE
*   **READ** relevant sections of this `GEMINI.md` to understand existing patterns.
*   **IDENTIFY** which files/components will be affected.
*   **CHECK** for existing similar implementations to maintain consistency.

### 1.3 PLAN THE APPROACH
*   For complex changes, **OUTLINE** the approach before coding.
*   **CONFIRM** understanding with user if requirements are ambiguous.

---

## STAGE 2: DURING IMPLEMENTATION

While actively coding:

### 2.1 FRONTEND VERIFICATION PROTOCOL (THE GOLDEN RULE)
*   **NEVER** modify source files (JS/HTML/CSS) blindly.
*   **ALWAYS** verify fixes/features by **injecting code into the running browser tab** first.
    *   Use Console for JS logic.
    *   Use DOM editing for HTML structure.
    *   Use Style Editor for CSS.
*   **ONLY** when the injected fix is verified working in the browser, propagate it to the source code.
*   **LOGGING**: All frontend changes **MUST** include console logs (`[App] ...`) to prove execution.

### 2.2 BROWSER USAGE
*   **ALWAYS** reuse the already open browser tab.
*   **NEVER** open a new tab for localhost.

### 2.3 COMMIT & RESTART CYCLE
*   **VALIDATION FIRST**: Before committing, you MUST:
    1.  Apply changes.
    2.  Restart the application (`mvn spring-boot:run`).
    3.  Verify functionality via Browser Subagent.
    4.  **ASK USER FOR PERMISSION** to commit.
*   **AFTER APPROVAL**:
    *   Commit with semantic message: `git commit -am "feat/fix: Description"`

### 2.4 MANDATORY TEST COVERAGE
*   **EVERY** new feature or bug fix **MUST** have corresponding test cases (Unit or Integration).
*   **VERIFY** tests pass before asking for commit approval.

### 2.5 CHANGELOG MANAGEMENT
*   **UPDATE** `CHANGELOG.md` for **EVERY** significant change (feature, fix, refactor).
*   **DO NOT DEFER** this. It is part of the implementation task.

---

## STAGE 3: POST FEATURE IMPLEMENTATION (User Confirmation Trigger)

When the user says they are **confident of the changes** (or similar confirmation), you MUST perform these steps in order:

1.  **COMMIT**: Create a semantic commit with proper message format:
    ```bash
    git commit -am "feat/fix: Brief description of the change"
    ```
    Use `feat:` for new features, `fix:` for bug fixes, `refactor:` for refactoring, `docs:` for documentation.

2.  **UPDATE CHANGELOG**: Add entry to `todo-app/CHANGELOG.md` under `[Unreleased]`:
    *   Use proper sections (Added, Changed, Fixed, etc.)
    *   Include brief description of what was done
    *   Reference relevant files or components

3.  **UPDATE GEMINI.md**: Review and update THIS file (`GEMINI.md`) as needed:
    *   **ADD** new sections for new features, APIs, or architectural patterns
    *   **UPDATE** existing sections if behavior changed (e.g., new fields, new endpoints)
    *   **DELETE** outdated information that no longer applies

> [!NOTE]
> These steps ensure documentation stays in sync with the codebase and provide a complete audit trail.

---

# Project: To-Do and Reminders Application

## Project Overview

This is a full-stack, single-page web application for managing tasks and to-do items. It presents a "Task Board 3D" interface with swimlanes for organizing tasks. The application is built with a Java/Spring Boot backend and a dynamic frontend that uses Thymeleaf for initial rendering and Alpine.js for interactivity.

### Key Features

*   **3D Kanban Board**: Visually distinct swimlanes with 3D depth effects.
*   **Interactive Statistics**: Collapsible accordion dashboard with 3D Pie and Bar charts.
*   **Drag-and-Drop**: Intuitive task management powered by Sortable.js.
*   **Mobile Optimized**: Responsive layout with vertical stacking and touch-friendly targets.
*   **Glassmorphism UI**: Modern, translucent design aesthetic.

## Technology Stack

### Backend
*   **Java 21**
*   **Spring Boot 3.3.0** (Web, Data JPA)
*   **PostgreSQL**: Primary database (configured in `application.properties`).
*   **Maven**: Build tool.
*   **Log4j2**: Logging framework.

### Frontend
*   **Alpine.js (v3.x)**: Lightweight reactive framework for declarative UI.
*   **Bootstrap 5**: UI Components, Grid, and Modals.
*   **Axios**: HTTP client for API communication.
*   **GSAP (v3.12.2)**: Professional animation library.
*   **Sortable.js**: Drag-and-drop list management.
*   **Highcharts 3D**: Interactive Charts.
*   **Thymeleaf**: Server-side template engine (initial load).

## Architecture

The application follows a standard layered Spring Boot architecture:

1.  **Controller Layer**: REST endpoints (`TaskController`, `SwimLaneController`).
2.  **Service Layer**: Business logic (`TaskService`, `SwimLaneService`).
3.  **DAO Layer**: Data Access Objects (`TaskDAO`, `SwimLaneDAO`).
4.  **Repository Layer**: Spring Data JPA interfaces (`TaskRepository`, `SwimLaneRepository`).
5.  **Database**: PostgreSQL running on port 5432 (default).

## Data Model

### SwimLane
*   `id` (Long): Primary Key.
*   `name` (String): Name of the swimlane (e.g., "Feature A").
*   `isCompleted` (Boolean): Status of the swimlane.
*   `isDeleted` (Boolean): Soft delete flag.
*   `position` (Integer): Display order for lane reordering.

### Task
*   `id` (Long): Primary Key.
*   `name` (String): Task description.
*   `status` (Enum): `TODO`, `IN_PROGRESS`, `DONE`, `BLOCKED`, `DEFERRED`.
*   `comments` (String/JSON): JSON array of comment objects.
*   `tags` (String/JSON): JSON array of tag strings.
*   `swimLane` (SwimLane): Many-to-One relationship.
*   `position` (Integer): Order within the status column.

## API Reference

### SwimLanes (`/api/swimlanes`)
*   `GET /`: Get all swimlanes.
*   `GET /active`: Get active swimlanes.
*   `GET /completed`: Get completed swimlanes.
*   `POST /`: Create a new swimlane (`{name}`).
*   `PATCH /{id}/complete`: Mark swimlane as complete.
*   `PATCH /{id}/uncomplete`: Reactivate swimlane.
*   `PATCH /reorder`: Reorder swimlanes (body: `Long[]` of IDs).
*   `DELETE /{id}`: Soft delete swimlane.

### Tasks (`/api/tasks`)
*   `GET /`: Get all tasks.
*   `GET /swimlane/{swimLaneId}`: Get tasks by lane (for incremental loading).
*   `GET /{id}`: Get specific task.
*   `POST /`: Create a new task.
*   `PUT /{id}`: Update task details.
*   `DELETE /{id}`: Delete task.
*   `PATCH /{id}/move`: Move task (params: `status`, `swimLaneId`, `position`).
*   `POST /{id}/comments`: Add comment.
*   `PUT /{id}/comments/{commentId}`: Update comment.
*   `DELETE /{id}/comments/{commentId}`: Delete comment.

### Server-Sent Events (`/api/sse`)
*   `GET /stream`: Subscribe to real-time updates (SSE connection).
    *   Events: `init`, `task-updated`, `task-deleted`, `lane-updated`, `heartbeat`

## Frontend Architecture

The frontend was rewritten from Vanilla JS to **Alpine.js** to improve maintainability and performance.

*   **Reactive State**: `todoApp` Alpine component manages `tasks`, `swimLanes`, and `currentView`.
*   **Declarative UI**: HTML uses `x-for`, `x-if`, and `x-model` instead of manual DOM manipulation.
*   **Optimized Animations**: GSAP handles complex animations (ripples, transitions) instead of CSS/JS hybrids.
*   **Modular Structure**:
    *   `app.js`: Alpine component entry point, initialization orchestration.
    *   `modules/store.js`: Centralized state management, modal handling, optimistic updates.
    *   `modules/api.js`: Axios HTTP client with interceptors, SSE initialization.
    *   `modules/drag.js`: SortableJS integration for drag-and-drop.
    *   `index.html`: Thymeleaf template with Alpine directives.

## Add Task Dialog

The "Add Task" modal allows creating tasks with several fields:

*   **Task Name/Description**: Multi-line `<textarea>` for detailed task descriptions.
*   **Status Dropdown**: Select initial status (TODO, IN_PROGRESS, DONE, BLOCKED, DEFERRED).
    *   Custom styled with dark theme gradient background.
    *   `<option>` elements have explicit dark styling for visibility.
*   **Tags Input**: Chip-based tag system.
    *   Type tag name and press Enter to add.
    *   Click tag chip to remove.
    *   Tags stored as JSON array in `Task.tags` field.
*   **Swimlane Header**: Purple gradient header showing which lane the task belongs to.

### Modal State (`inputModal` in store.js)
```javascript
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

## Notification System

Toast notifications use Alpine.js transitions with custom CSS utilities:

### Success Toast (`.notification-toast`)
*   **Position**: Fixed, top center, flush with screen edge (`top: 0`).
*   **Animation**: Slide-down from top using transform utilities.
*   **Trigger**: `triggerSave()` sets `showSaved = true` for 1.5 seconds.
*   **Z-index**: 10000 (above all content).

### Error Toast (`.error-toast`)
*   **Color**: Red theme for error visibility.
*   **Trigger**: `triggerError(msg)` sets `showErrorToast = true` for 3 seconds.

### CSS Transition Utilities
```css
.-translate-y-full { transform: translateX(-50%) translateY(-100%); }
.translate-y-0 { transform: translateX(-50%) translateY(0); }
```

> [!IMPORTANT]
> The toast uses `translateX(-50%)` for centering. Animation classes must preserve this!

## Real-Time Updates (SSE Architecture)

The application uses Server-Sent Events for real-time synchronization:

1.  **SseService**: Manages client connections with `CopyOnWriteArrayList<SseEmitter>`.
2.  **SseController**: Exposes `/api/sse/stream` endpoint.
3.  **AsyncWriteService**: After async DB writes, broadcasts updates via SSE.
4.  **Frontend (api.js)**: `initSSE()` subscribes and handles `task-updated`, `task-deleted`, `lane-updated` events.
5.  **Heartbeat**: Every 30 seconds to keep connections alive and clean up dead clients.

## Async Write-Behind Architecture

Database writes are optimized for perceived performance:

1.  **Optimistic UI**: Frontend updates state immediately.
2.  **Fire-and-Forget**: Service layer queues writes to `AsyncWriteService`.
3.  **Single-Threaded Executor**: Guarantees sequential DB writes (no race conditions).
4.  **SSE Broadcast**: After DB write completes, broadcasts to all clients.
5.  **Simulated Latency**: 100ms delay built-in to prove decoupling works.

## DRAG-AND-DROP SYSTEM - COMPLETE REFERENCE

⚠️ **CRITICAL: This section contains EVERYTHING needed to understand and debug drag-drop. Read it fully before making any changes!**

---

### ARCHITECTURE OVERVIEW

```
┌─────────────────────────────────────────────────────────────┐
│  index.html                                                 │
│  ├── .board-container     (Lane Sortable - reorder lanes)   │
│  │   └── .swimlane-row    (Lane wrapper)                    │
│  │       └── .lane-column (Task Sortable - reorder tasks)   │
│  │           └── .task-card (Draggable item)                │
│                                                             │
│  app.js                                                     │
│  ├── init()               → Loads lanes, then tasks async   │
│  ├── setupDrag()          → Inits lane-level Sortable       │
│  ├── initColumn(el)       → Called by x-init (may be empty) │
│  └── reinitSortableForLane() → CRITICAL: Reinits after load │
│                                                             │
│  drag.js                                                    │
│  ├── initOneColumn()      → Creates Sortable on column      │
│  └── initLaneSortable()   → Creates Sortable on board       │
└─────────────────────────────────────────────────────────────┘
```

---

### INITIALIZATION TIMING (THE #1 CAUSE OF BUGS)

**The Problem:**
```
Timeline:
─────────────────────────────────────────────────────────────►
1. Alpine starts
2. Lanes fetched from API
3. HTML rendered (columns created) ← x-init fires HERE!
4. Tasks fetched ASYNC per lane    ← Cards don't exist yet!
5. Cards rendered in DOM           ← Cards exist NOW
6. reinitSortableForLane() called  ← Sortable reinit with cards
```

**Why this matters:**
- `x-init="initColumn($el)"` fires at step 3
- Sortable is initialized on EMPTY columns (0 draggable items)
- Drag events have no targets = nothing works

**The Solution:**
```javascript
// In app.js, after tasks load for a lane:
this.$nextTick(() => {
    this.reinitSortableForLane(lane.id);
});
```

**Console logs to verify:**
- `[Drag] Column has 0 task-card children` = BAD (too early)
- `[Drag] Column has 5 task-card children` = GOOD (after reinit)

---

### CSS 3D TRANSFORMS - THE SILENT KILLER

**These CSS properties BREAK drag events:**

| Property | Effect | Why It Breaks Drag |
|----------|--------|-------------------|
| `perspective: Npx` | Creates 3D viewport | Changes coordinate system for hit-testing |
| `transform-style: preserve-3d` | Enables 3D for children | Children's positions calculated in 3D space |
| `transform: translateZ(Npx)` | Moves element in Z-axis | Mouse coordinates don't match visual position |
| `transform: rotateX/Y()` | Rotates in 3D | Click position != element position |

**Symptoms:**
- Hovering shows correct cursor (`grab`)
- Clicking does nothing
- `[Drag] onChoose` never fires
- `[Event MouseDown]` logs show wrong target (parent instead of card)
- Lane drag works, task drag doesn't

**Safe Alternatives:**
```css
/* BAD - breaks drag */
.task-card {
    transform: translateZ(5px);
    transform-style: preserve-3d;
}

/* GOOD - works with drag */
.task-card {
    transform: none;
    transform-style: flat;
    box-shadow: 0 5px 15px rgba(0,0,0,0.2);  /* Use shadow for depth */
}
```

---

### REQUIRED HTML ATTRIBUTES

**Container (`.lane-column`):**
```html
<div class="lane-column" 
     :id="'lane-' + lane.id + '-' + status"
     :data-status="status"           <!-- REQUIRED: For API call -->
     :data-lane-id="lane.id"         <!-- REQUIRED: For API call -->
     x-init="initColumn($el)">       <!-- Initializes Sortable -->
```

**Draggable Item (`.task-card`):**
```html
<div class="task-card"
     :data-task-id="task.id"         <!-- REQUIRED: Identifies task -->
     :data-status="task.status"      <!-- For visual styling -->
     draggable="true">               <!-- REQUIRED: Enables HTML5 drag -->
```

---

### SORTABLE.JS CONFIGURATION

**Current settings in `drag.js`:**
```javascript
new Sortable(col, {
    group: 'tasks',           // All columns share group = cross-column drag
    animation: 150,           // Animation duration (ms)
    delay: 0,                 // No delay for desktop
    delayOnTouchOnly: true,   // Delay only on mobile
    touchStartThreshold: 3,   // Pixels before drag starts on touch
    ghostClass: 'task-ghost', // CSS class for ghost/placeholder
    dragClass: 'task-drag',   // CSS class while dragging
    // forceFallback: false,  // Use native HTML5 drag (default)
});
```

**When to use `forceFallback: true`:**
- Only if native HTML5 drag has issues
- Creates JS-based drag simulation
- Heavier on performance
- Usually NOT needed if CSS is correct

---

### EVENT LIFECYCLE

```
User clicks card:
    │
    ▼
onChoose ─────► "[Drag] onChoose - item selected"
    │           (If this doesn't fire, CSS is blocking events)
    ▼
onStart ──────► "[Drag] onStart - drag began"
    │           (Card picked up, ghost created)
    ▼
onMove ───────► "[Drag] onMove - dragging" (multiple times)
    │           (Card moving over containers)
    ▼
onEnd ────────► "[Drag] onEnd - drag completed"
                (Drop happened, API call made)
```

---

### COMPLETE DEBUG CHECKLIST

**Step 1: Check Console on Page Load**
```
✓ "[App] ====== Component Initializing ======"
✓ "[App] Loaded X lanes"
✓ "[App] Fetching tasks for lane X"
✓ "[App] Lane X returned Y tasks"
✓ "[App] reinitSortableForLane called for lane X"
✓ "[Drag] Column has Y task-card children" (Y > 0)
✓ "[Drag] Sortable instance created successfully"
```

**Step 2: Click a Task Card**
```
✓ "[Event MouseDown]" shows cardId: "123" (not "N/A")
✓ "[Drag] onChoose - item selected"
```

**Step 3: Drag the Card**
```
✓ "[Drag] onStart - drag began"
✓ "[Drag] onMove - dragging" (while moving)
✓ "[Drag] onEnd - drag completed"
```

**If Something Fails:**

| Symptom | Cause | Fix |
|---------|-------|-----|
| `Column has 0 children` | Sortable init too early | Check `reinitSortableForLane()` is called |
| `MouseDown` shows `cardId: N/A` | Click not reaching card | Check CSS stacking context, z-index |
| `onChoose` never fires | 3D transforms blocking | Remove perspective, preserve-3d, translateZ |
| `onStart` never fires | Missing draggable attr | Add `draggable="true"` to task-card |
| Drag works but API fails | Wrong data-attributes | Check data-task-id, data-status, data-lane-id |

---

### FILES REFERENCE

| File | Purpose | Key Functions |
|------|---------|---------------|
| `static/js/app.js` | Alpine component, init orchestration | `init()`, `reinitSortableForLane()`, `initColumn()` |
| `static/js/modules/drag.js` | Sortable configuration | `initOneColumn()`, `initLaneSortable()` |
| `static/css/style.css` | Styling (NO 3D transforms!) | `.task-card`, `.lane-column` |
| `templates/index.html` | HTML structure | x-init, data-attributes, draggable |

---

### CSS CLASS REFERENCE

| Class | Element | Notes |
|-------|---------|-------|
| `.board-container` | Outer container | Lane Sortable attached here |
| `.swimlane-row` | Lane wrapper | Must NOT have 3D transforms |
| `.lane-column` | Status column | Task Sortable attached here |
| `.task-card` | Draggable task | Must have `draggable="true"` |
| `.task-ghost` | Sortable ghost | Shows drop position |
| `.task-drag` | Card while dragging | Applied during drag |
| `.lane-ghost` | Lane ghost | For lane reordering |

## Building and Running

### Prerequisites
*   Java 21
*   Maven
*   PostgreSQL (running on localhost:5432, db: `todo_db`, user: `postgres`, pass: `Database@123`)

### Running the Application
```bash
mvn spring-boot:run
```
The application will be available at **[http://localhost:8080](http://localhost:8080)** (Note: Port 8080).

### Running Tests
```bash
mvn test
```

## CHANGELOG MANAGEMENT

> [!IMPORTANT]
> **Update `CHANGELOG.md` at every significant milestone!**

### Changelog Location
- **File**: `todo-app/CHANGELOG.md`

### When to Update
- New feature implementation completed
- Major bug fix deployed
- API changes
- Database schema changes
- Frontend architecture changes
- Performance improvements
- Any breaking changes

### Changelog Format
The changelog follows [Keep a Changelog](https://keepachangelog.com/) format with sections:
- **Added** - New features
- **Changed** - Changes in existing functionality  
- **Deprecated** - Soon-to-be removed features
- **Removed** - Now removed features
- **Fixed** - Bug fixes
- **Security** - Vulnerability fixes

### Update Process
1. Add entry under `[Unreleased]` section
2. Include commit hash and timestamp
3. Document issue resolution if applicable
4. When releasing, move unreleased items to versioned section
