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

*   **3D Kanban Board**: Visually distinct swimlanes with 3D depth effects.
*   **Interactive Statistics**: Collapsible accordion dashboard with 3D Pie and Bar charts.
*   **Drag-and-Drop**: Intuitive task management powered by Sortable.js.
*   **Mobile Optimized**: Responsive layout with persistent push-sidebar navigation and bottom-anchored task details.
*   **Glassmorphism UI**: Modern, translucent design aesthetic with dynamic CSS transitions.
*   **Task Filters**: Hide Done and Blocked Only filter buttons in navbar.
*   **Tag Filter Bar**: Sticky, permanently visible tag bar with discovery mode (shows all tags on mobile by default) and persistence (selected tags always stay visible).
*   **AOP Idempotency**: Transparent protection against duplicate/concurrent operations using Spring AOP and custom annotations.

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

## Backend Performance Optimizations

The application implements several latency reduction strategies:

### Async Write-Behind Pattern
- **File**: `AsyncWriteService.java`
- Database writes are decoupled from HTTP responses via `@Async("asyncWriteExecutor")`
- Single-threaded executor ensures sequential writes without race conditions
- API returns immediately (~5ms) while DB writes happen in background (~100-500ms)
- SSE broadcasts notify clients after DB commit completes

### Caching Layer
| Service | Cache Name | Key |
|---------|------------|-----|
| `TaskService` | `tasks` | All tasks |
| `TaskService` | `tasksByLane` | `swimLaneId` |
| `SwimLaneService` | `lanes` | `currentUserId` |

Cache is evicted via `@CacheEvict` on create/delete operations.

### Cache Management & Monitoring
| Component | Function |
|-----------|----------|
| `CacheWarmupService` | Pre-warms `tasks` and `tasksByLane` caches on `ApplicationReadyEvent`. |
| `CacheLoggingInterceptor` | Intercepts `/api/**` requests to log hit/miss stats via Caffeine native metrics. |

### HikariCP Connection Pool
```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
```

### Database Indexes
| Entity | Index | Columns |
|--------|-------|---------|
| `SwimLane` | `idx_swim_lanes_user_id` | `user_id` |
| `SwimLane` | `idx_swim_lanes_user_deleted_position` | `user_id, is_deleted, position_order` |
| `SwimLane` | `idx_swim_lanes_user_completed_deleted` | `user_id, is_completed, is_deleted, position_order` |
| `Task` | `idx_tasks_swim_lane_id` | `swim_lane_id` |
| `Task` | `idx_tasks_status` | `status` |
| `Task` | `idx_tasks_lane_status_position` | `swim_lane_id, status, position_order` |

### Bulk UPDATE Queries
Position shifts use single bulk UPDATE instead of N individual saves:
```java
@Query("UPDATE Task t SET t.position = t.position + 1 WHERE ...")
int shiftPositionsDown(...);
```

### Gzip Compression
```properties
server.compression.enabled=true
server.compression.min-response-size=1024
```

### Other Optimizations
- `spring.jpa.open-in-view=false` - Prevents N+1 lazy loading issues
- Optimistic UI updates with SSE-based eventual consistency

## Architectural Patterns

### AOP-Based Idempotency
- **Annotation**: `@Idempotent`
- **Logic**: Uses Spring AOP to intercept mutating operations. Generates a unique key via SpEL expressions (e.g., `'createTask:' + #task.name`).
- **Storage**: `IdempotencyService` manages a time-windowed cache of operation keys (default 5s).
- **Error Handling**: Throws `DuplicateOperationException` (409 Conflict) if a duplicate is detected.

### Global Exception Handling
- **Component**: `GlobalExceptionHandler`
- **Purpose**: Centralized handling of business and runtime exceptions (e.g., `DuplicateOperationException`, `IllegalArgumentException`) to return consistent JSON error responses.

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
