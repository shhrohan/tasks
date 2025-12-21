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

### 2.2.1 TERMINAL USAGE
*   **NEVER** run multiple terminal sessions in parallel.
*   **ALWAYS** wait for the previous terminal command to complete before starting a new one.
*   **ALWAYS** kill the existing process (e.g., `mvn spring-boot:run` or java process) BEFORE starting a new one.
*   **ALWAYS** restart the backend application after ANY frontend or backend changes.

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

> [!CAUTION]
> ## NEVER MERGE TO MAIN AUTOMATICALLY
> **NEVER** merge to main or push to main unless the user **explicitly requests it**.
> Do not ask or offer to merge to main. The user will tell you when they want to merge.

> [!NOTE]
> These steps ensure documentation stays in sync with the codebase and provide a complete audit trail.

---

# Project: To-Do and Reminders Application

## Project Overview

This is a full-stack, single-page web application for managing tasks and to-do items. It presents a "Task Board 3D" interface with swimlanes for organizing tasks. The application is built with a Java/Spring Boot backend and a dynamic frontend that uses Thymeleaf for initial rendering and Alpine.js for interactivity.

### Key Features

*   **3D Kanban Board**: Visually distinct swimlanes with collapsible headers and status columns.
*   **Status Pills**: Inline progress visualization in swimlane headers showing task distribution (percentage + count).
*   **Drag-and-Drop**: Intuitive task management powered by Sortable.js with cross-column support.
*   **Task Filters**: "Hide Done" and "Blocked Only" filter buttons in navbar.
*   **Tag Filter Bar**: Sticky bar below navbar showing all unique tags as capsules for filtering tasks.
*   **Task Card Resize**: Drag resize handle on task cards to adjust height; double-click to reset.
*   **Toast Notifications**: Success (green) and Error (red) notifications with slide-down animations.
*   **Optimistic Updates**: UI updates instantly, with background API sync.
*   **Real-Time Sync**: Server-Sent Events (SSE) for multi-client synchronization.
*   **Glassmorphism UI**: Modern, translucent dark theme design aesthetic.

## Technology Stack

### Backend
*   **Java 21**
*   **Spring Boot 3.3.0** (Web, Data JPA)
*   **PostgreSQL**: Primary database (Azure-hosted in prod, local in dev).
*   **Maven**: Build tool.
*   **Log4j2**: Logging framework.

### Frontend
*   **Alpine.js (v3.12.0)**: Lightweight reactive framework loaded via ESM.
*   **Bootstrap 5.3.0**: UI Components, Grid system.
*   **Axios**: HTTP client for API communication.
*   **Sortable.js (v1.15.0)**: Drag-and-drop list management.
*   **Font Awesome 6.4.0**: Icons.
*   **Thymeleaf**: Server-side template engine (initial load only).

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
- `heartbeat` - Keep-alive (every 30s)

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

## Notification System

### Success Toast (`.notification-toast.success`)
- **Position**: Fixed, top center (`top: 0`, `left: 50%`, `transform: translateX(-50%)`)
- **Animation**: Slide-down from top using Alpine.js x-transition
- **Trigger**: `triggerSave()` sets `showSaved = true` for 1.5 seconds
- **Z-index**: 10000

### Error Toast (`.notification-toast.error`)
- **Color**: Red border and icon
- **Animation**: Same slide-down animation
- **Trigger**: `showError(msg)` sets `showErrorToast = true` for 3 seconds

### CSS Transition Utilities
```css
.-translate-y-full { transform: translateX(-50%) translateY(-100%); }
.translate-y-0 { transform: translateX(-50%) translateY(0); }
```

> [!IMPORTANT]
> The toast uses `translateX(-50%)` for centering. Animation classes must preserve this!

## Real-Time Updates (SSE Architecture)

1.  **SseService**: Manages client connections with `CopyOnWriteArrayList<SseEmitter>`.
2.  **SseController**: Exposes `/api/sse/stream` endpoint.
3.  **AsyncWriteService**: After async DB writes, broadcasts updates via SSE.
4.  **Frontend (api.js)**: `initSSE()` subscribes and handles events.
5.  **Heartbeat**: Every 30 seconds to keep connections alive.

## Async Write-Behind Architecture

1.  **Optimistic UI**: Frontend updates state immediately.
2.  **Fire-and-Forget**: Service layer queues writes to `AsyncWriteService`.
3.  **Single-Threaded Executor**: Guarantees sequential DB writes.
4.  **SSE Broadcast**: After DB write completes, broadcasts to all clients.

## Drag-and-Drop System

### Architecture Overview
```
index.html
├── .board-container     (Lane Sortable - reorder lanes)
│   └── .swimlane-row    (Lane wrapper)
│       └── .lane-column (Task Sortable - reorder tasks)
│           └── .task-card (Draggable item)
```

### Critical Initialization Timing

**The Problem:**
- `x-init="initColumn($el)"` fires when column is CREATED
- Tasks are loaded asynchronously AFTER columns exist
- Sortable initialized on EMPTY columns = no draggable items

**The Solution:**
```javascript
// In app.js, after tasks load for a lane:
this.$nextTick(() => {
    this.reinitSortableForLane(lane.id);
});
```

### Required HTML Attributes

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
     draggable="true">               <!-- REQUIRED: Enables HTML5 drag -->
```

### CSS 3D Transforms Warning

> [!CAUTION]
> These CSS properties BREAK drag events:
> - `perspective`
> - `transform-style: preserve-3d`
> - `transform: translateZ()`
> - `transform: rotateX/Y()`
> 
> Use `box-shadow` instead for depth effects.

## Status Pills UI

Swimlane headers display proportional status pills showing task distribution:

```html
<template x-if="getLaneStats(lane.id).todo > 0">
    <div class="badge status-pill-lg"
         :style="'flex-grow: ' + getLaneStats(lane.id).todo"
         x-text="getLaneStats(lane.id).todoPct + '% (' + getLaneStats(lane.id).todo + ')'">
    </div>
</template>
```

**Key Points:**
- Uses `<template x-if>` to completely remove zero-count pills from DOM
- `flex-grow` based on task count for proportional sizing
- Displays format: `XX% (N)` where XX is percentage and N is count

## Tag Filter Bar

A sticky bar below the navbar allows filtering tasks by tags:

```html
<div class="tag-filter-bar glass-panel px-4 py-2" x-show="getAllUniqueTags().length > 0">
    <template x-for="tag in getAllUniqueTags()" :key="tag">
        <button class="tag-capsule" :class="{'active': isTagSelected(tag)}" 
                @click="toggleTag(tag)" x-text="tag">
        </button>
    </template>
</div>
```

**Key Points:**
- Only shows when tasks have tags (`getAllUniqueTags().length > 0`)
- Clicking a tag toggles it in `selectedTags` array
- Active tags get `.active` class (gradient background)
- Lanes without matching tasks are hidden via `laneHasMatchingTasks(laneId)`
- "Clear All" button resets filter

### CSS Classes
- `.tag-filter-bar` - Sticky positioning below navbar, glassmorphism style
- `.tag-capsule` - Pill-shaped tag buttons with hover/active states

## Task Card Resize

Task cards can be resized by dragging a corner handle:

**HTML Structure:**
```html
<div class="task-card" :style="getTaskStyle(task.id)" @dblclick="resetTaskSize(task.id)">
    <!-- Card content -->
    <div class="resize-handle" @mousedown.stop.prevent="startResize($event, task.id, lane.id, status)">
    </div>
</div>
```

**Key Points:**
- Resize handle appears on hover (bottom-right corner)
- Drag to resize height
- Double-click card to reset to default size
- Sizes stored in `taskSizes` object: `{taskId: {height: px}}`
- CSS class `.resizing` applied during drag for visual feedback

## Building and Running

### Prerequisites
*   Java 21
*   Maven
*   PostgreSQL

### Running the Application
```bash
mvn spring-boot:run
```
The application will be available at **[http://localhost:8080](http://localhost:8080)**.

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
