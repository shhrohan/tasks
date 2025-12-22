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
      - DO NOT ask for commit approval before tests are passing.  
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

13. **NO AUTOMATIC MERGE TO MAIN**  
 - NEVER merge to `main` or push to `main` unless the user explicitly says so.  
 - DO NOT ask or suggest merging to main. Wait for the user to initiate.

14. **FINAL CONFIRMATION MESSAGE**  
 - After performing all required documentation and commit tasks, summarize:  
   `"ALL STAGES ✓ COMPLETE: Code, tests, CHANGELOG, and GEMINI.md are now in sync."`

---



# PROJECT: To-Do and Reminders Application

## Project Overview
This is a full-stack, single-page web application for managing tasks and to-do items. It presents a "Task Board 3D" interface with swimlanes for organizing tasks. The application is built with a Java/Spring Boot backend and a dynamic frontend that uses Thymeleaf for initial rendering and Alpine.js for interactivity.

### Key Features
- **3D Kanban Board**: Visually distinct swimlanes with 3D depth effects.  
- **Interactive Statistics**: Collapsible accordion dashboard with 3D pie and bar charts.  
- **Drag-and-Drop**: Intuitive task management powered by Sortable.js.  
- **Mobile Optimized**: Responsive layout with persistent push-sidebar navigation and bottom-anchored task details.  
- **Glassmorphism UI**: Modern, translucent design aesthetic with dynamic CSS transitions.  
- **Task Filters**: Hide Done and Blocked Only filter buttons in navbar.  
- **Tag Filter Bar**: Sticky, permanently visible tag bar with discovery mode (shows all tags on mobile by default) and persistence (selected tags always stay visible).  
- **AOP Idempotency**: Transparent protection against duplicate/concurrent operations using Spring AOP and custom annotations.

---

## Technology Stack

### Backend
- **Java 21**  
- **Spring Boot 3.3.0** (Web, Data JPA)  
- **PostgreSQL**: Primary database (configured in `application.properties`).  
- **Maven**: Build tool.  
- **Log4j2**: Logging framework.

### Frontend
- **Alpine.js (v3.x)**: Lightweight reactive framework for declarative UI.  
- **Bootstrap 5**: UI components, grid, and modals.  
- **Axios**: HTTP client for API communication.  
- **GSAP (v3.12.2)**: Animation library.  
- **Sortable.js**: Drag-and-drop list management.  
- **Highcharts 3D**: Interactive charts.  
- **Thymeleaf**: Server-side template engine (initial load).

---

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
```
@Query("UPDATE Task t SET t.position = t.position + 1 WHERE ...")
int shiftPositionsDown(...);
```

### Gzip Compression
```
spring.http.compression.enabled=true
spring.http.compression.min-response-size=1024
```


### Other Optimizations
- `spring.jpa.open-in-view=false` – prevents N+1 lazy loading issues.  
- Optimistic UI updates with SSE-based eventual consistency.

---

## Architectural Patterns

### AOP-Based Idempotency
- **Annotation**: `@Idempotent`  
- **Logic**: Uses Spring AOP to intercept mutating operations. Generates a unique key via SpEL expressions (e.g., `'createTask:' + #task.name`).  
- **Storage**: `IdempotencyService` manages a time-windowed cache of operation keys (default 5s).  
- **Error Handling**: Throws `DuplicateOperationException` (409 Conflict) if a duplicate is detected.

### Global Exception Handling
- **Component**: `GlobalExceptionHandler`  
- **Purpose**: Centralized handling of business and runtime exceptions (e.g., `DuplicateOperationException`, `IllegalArgumentException`) to return consistent JSON error responses.

---

## Building and Running

### Prerequisites
- Java 21  
- Maven  
- PostgreSQL (running on `localhost:5432`, db: `todo_db`, user: `postgres`, pass: `Database@123`)

### Running the Application
```
mvn spring-boot:run
```
The application will be available at [**http://localhost:8080**](http://localhost:8080).

### Running Tests
```
mvn test
```

---

**THIS VERSION OF GEMINI.MD IS OPTIMIZED FOR STRICT, STEP-BY-STEP INSTRUCTION COMPLIANCE BY THE AGENT.**
