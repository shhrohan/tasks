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
> ## MERGE TO MAIN - DOUBLE CONFIRMATION REQUIRED
> **NEVER** merge to main or push to main without **DOUBLE CONFIRMATION** from the user.
> 1. First, ask: "Would you like me to merge to main?"
> 2. After user says yes, ask again: "Confirming: merge dev to main and push? (yes/no)"
> 3. Only proceed after receiving the second explicit "yes" confirmation.
> 
> This rule exists to prevent accidental deployments to production.

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
*   **Task Filters**: Hide Done and Blocked Only filter buttons in navbar.

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
