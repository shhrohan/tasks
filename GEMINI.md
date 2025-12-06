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

### Task
*   `id` (Long): Primary Key.
*   `name` (String): Task description.
*   `status` (Enum): `TODO`, `IN_PROGRESS`, `DONE`, `BLOCKED`, `DEFERRED`.
*   `comments` (String/JSON): JSON array of comment objects.
*   `tags` (String/JSON): JSON array of tag strings.
*   `swimLane` (SwimLane): Many-to-One relationship.

## API Reference

### SwimLanes (`/api/swimlanes`)
*   `GET /`: Get all swimlanes.
*   `GET /active`: Get active swimlanes.
*   `GET /completed`: Get completed swimlanes.
*   `POST /`: Create a new swimlane (`{name}`).
*   `PATCH /{id}/complete`: Mark swimlane as complete.
*   `PATCH /{id}/uncomplete`: Reactivate swimlane.
*   `DELETE /{id}`: Soft delete swimlane.

### Tasks (`/api/tasks`)
*   `GET /`: Get all tasks.
*   `GET /{id}`: Get specific task.
*   `POST /`: Create a new task.
*   `PUT /{id}`: Update task details.
*   `DELETE /{id}`: Delete task.
*   `PATCH /{id}/move`: Move task (params: `status`, `swimLaneId`).
*   `POST /{id}/comments`: Add comment.
*   `PUT /{id}/comments/{commentId}`: Update comment.
*   `DELETE /{id}/comments/{commentId}`: Delete comment.

## Frontend Architecture

The frontend was rewritten from Vanilla JS to **Alpine.js** to improve maintainability and performance.

*   **Reactive State**: `todoApp` Alpine component manages `tasks`, `swimLanes`, and `currentView`.
*   **Declarative UI**: HTML uses `x-for`, `x-if`, and `x-model` instead of manual DOM manipulation.
*   **Optimized Animations**: GSAP handles complex animations (ripples, transitions) instead of CSS/JS hybrids.
*   **Component Structure**:
    *   `app.js`: Contains the Alpine data object and API logic.
    *   `index.html`: Contains the template and Alpine directives.

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

## CRITICAL WORKFLOW RULES
*   **BROWSER USAGE**:
    *   For loading the website and testing, **ALWAYS** reuse the already open browser tab. **DO NOT** open a new tab.

*   **MANDATORY LOGGING & VERIFICATION**:
    *   **ALWAYS** add helper debug logs for all frontend and backend functionalities created or updated.
    *   All frontend changes **MUST** be accompanied by logs printed on the browser console.
    *   Changes are considered correctly working **ONLY** when logs are available as proof of correctness.

*   **MANDATORY OPTIMIZATION (STOP & THINK)**:
    *   Before *every* code change, pause and ask: "Is there a better/cleaner/more standard way to do this?"
    *   If a better approach exists, you **MUST** propose it to the user **BEFORE** writing any code.
    *   **Blind obedience to sub-optimal requests is a FAILURE.**
    *   Support your suggestion with reasoning (e.g., "This approach scales better," "This matches the existing pattern," "This avoids X edge case").

*   **COMMIT & RESTART CYCLE**:
    *   **AFTER EVERY CHANGE**: You must perform the following sequence:
        1.  **Commit**: `git commit -am "Type: Description"`
        2.  **Restart**: Kill the existing process and run `mvn spring-boot:run` (or `java -jar`).
    *   **NEVER** skip this step. It ensures the app is always in a known, working state.

*   **Override**: Only proceed with the original (sub-optimal) request if the user explicitly overrides your suggestion *after* you have presented it.
