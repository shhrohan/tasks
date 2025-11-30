# Project: To-Do and Reminders Application

## Project Overview

This is a full-stack, single-page web application for managing tasks and to-do items. It presents a "Task Board 3D" interface with swimlanes for organizing tasks. The application is built with a Java/Spring Boot backend and a dynamic frontend that uses Thymeleaf for initial rendering and Alpine.js for interactivity.

### Key Features

*   **3D Kanban Board**: Visually distinct swimlanes with 3D depth effects.
*   **Interactive Statistics**: Collapsible accordion dashboard with 3D Pie and Bar charts.
*   **Drag-and-Drop**: Intuitive task management powered by Sortable.js.
*   **Mobile Optimized**: Responsive layout with vertical stacking and touch-friendly targets.
*   **Glassmorphism UI**: Modern, translucent design aesthetic.

### Key Technologies

*   **Backend:**
    *   Java 21
    *   Spring Boot 3.3.0
    *   Spring Web
    *   Spring Data JPA
    *   Maven
    *   Log4j2 (Logging)
*   **Frontend:**
    *   Thymeleaf
    *   Alpine.js
    *   Bootstrap 5 (UI Components & Modals)
    *   Axios
    *   GSAP (for animations)
    *   Sortable.js (for drag-and-drop)
    *   Highcharts 3D (Interactive Charts)
*   **Database:**
    *   H2 (embedded, file-based)

### Architecture

The application follows a layered Spring Boot architecture:

*   **Controller Layer**: Exposes REST endpoints (`TaskController`, `SwimLaneController`).
*   **Service Layer**: Contains business logic (`TaskService`, `SwimLaneService`).
*   **DAO Layer**: Handles data access, decoupling services from repositories (`TaskDAO`, `SwimLaneDAO`).
*   **Repository Layer**: Spring Data JPA interfaces.
*   **Frontend**: A single `index.html` file dynamically rendered by Thymeleaf. Interactions are handled by Alpine.js, communicating with the backend via REST.
*   **Logging**: Configured with Log4j2 (`log4j2.xml`) to capture application and Hibernate SQL logs.
*   **Data Persistence**: H2 database file (`data/todo-db.mv.db`).

## Building and Running

### Prerequisites

*   Java 21
*   Maven

### Running the Application

To run the application, use the following Maven command from the `todo-app` directory:

```bash
mvn spring-boot:run
```

The application will be available at [http://localhost:8080](http://localhost:8080).

### Running Tests

The project includes unit tests for Controllers, Services, and DAOs. To run all tests:

```bash
mvn test
```

## Development Conventions

*   **Backend**:
    *   Follows Controller -> Service -> DAO -> Repository pattern.
    *   Use `TaskDAO` and `SwimLaneDAO` for all data access.
    *   Lombok is used for boilerplate reduction.
    *   Log4j2 is used for logging (avoid `System.out`).
*   **Frontend**:
    *   Single Alpine.js component (`todoApp`) manages UI state.
    *   Bootstrap 5 is used for layout and modals.
    *   **Mobile First**: CSS is optimized for mobile devices (vertical stacking, touch targets).
    *   Status headers are injected via CSS on mobile views.
    *   **Statistics**: Implemented as a collapsible accordion (no modal) for better UX.

## CRITICAL WORKFLOW RULES

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
