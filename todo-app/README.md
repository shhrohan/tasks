<p align="center">
  <img src="https://img.icons8.com/fluency/96/task.png" alt="TaskBoard 3D Logo" width="80"/>
</p>

<h1 align="center">TaskBoard 3D</h1>

<p align="center">
  <em>A stunning, modern Kanban board with glassmorphism design and smooth drag-and-drop</em>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#tech-stack">Tech Stack</a> •
  <a href="#quick-start">Quick Start</a> •
  <a href="#api-reference">API</a> •
  <a href="#contributing">Contributing</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3.0-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/PostgreSQL-15+-316192?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL"/>
  <img src="https://img.shields.io/badge/Alpine.js-3.x-8BC0D0?style=for-the-badge&logo=alpine.js&logoColor=white" alt="Alpine.js"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Tests-103%20Passing-brightgreen?style=flat-square" alt="Tests"/>
  <img src="https://img.shields.io/badge/Coverage-93%25-brightgreen?style=flat-square" alt="Coverage"/>
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="License"/>
</p>

---

## ✨ Features

### 🎯 **Core Functionality**
- **Kanban Board** — Organize tasks across 5 status columns: Todo, In Progress, Done, Blocked, Deferred
- **Swimlanes** — Group tasks by project, sprint, or any category
- **Drag-and-Drop** — Intuitive task management powered by Sortable.js
- **Real-Time Sync** — Server-Sent Events (SSE) for live updates across tabs/devices

### 🎨 **Premium UI/UX**
- **Glassmorphism Design** — Modern frosted-glass aesthetic with depth effects
- **Dark Theme** — Easy on the eyes with carefully crafted color palette
- **Smooth Animations** — GSAP-powered transitions and micro-interactions
- **Mobile Responsive** — Touch-optimized layout with vertical stacking

### 🚀 **Productivity Features**
- **Task Filters** — Hide Done tasks or show only Blocked items with one click
- **Progress Pills** — Visual status distribution in swimlane headers
- **Collapse/Expand** — Focus on what matters with lane toggling
- **Tags** — Categorize tasks with color-coded labels

### ⚡ **Performance**
- **Async Write-Behind** — Optimistic UI updates with background persistence
- **Incremental Loading** — Lanes load first, then tasks stream in per-lane
- **Parallel Tests** — 103 tests run with unlimited thread parallelism

---

## 🛠 Tech Stack

### Backend
| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 21 | Language |
| **Spring Boot** | 3.3.0 | Framework |
| **Spring Data JPA** | - | ORM |
| **PostgreSQL** | 15+ | Database |
| **Log4j2** | - | Logging |
| **Maven** | - | Build tool |

### Frontend
| Technology | Version | Purpose |
|------------|---------|---------|
| **Alpine.js** | 3.x | Reactivity |
| **Bootstrap** | 5.3 | Layout & components |
| **Sortable.js** | 1.15 | Drag-and-drop |
| **Axios** | - | HTTP client |
| **Font Awesome** | 6.4 | Icons |

---

## 🚀 Quick Start

### Prerequisites
- **Java 21** or higher
- **Maven 3.8+**
- **PostgreSQL 15+** (or Docker)

### 1️⃣ Clone the Repository
```bash
git clone https://github.com/shhrohan/tasks.git
cd tasks/todo-app
```

### 2️⃣ Configure Database
Create a PostgreSQL database:
```sql
CREATE DATABASE todo_db;
```

Update `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/todo_db
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 3️⃣ Run the Application
```bash
mvn spring-boot:run
```

### 4️⃣ Open in Browser
Navigate to **[http://localhost:8080](http://localhost:8080)** 🎉

---

## 📦 Docker Quick Start

```bash
# Start PostgreSQL
docker run -d --name todo-db \
  -e POSTGRES_DB=todo_db \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15

# Run the app
mvn spring-boot:run
```

---

## 📚 API Reference

### Swimlanes

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/swimlanes` | Get all swimlanes |
| `GET` | `/api/swimlanes/active` | Get active swimlanes |
| `GET` | `/api/swimlanes/completed` | Get completed swimlanes |
| `POST` | `/api/swimlanes` | Create swimlane |
| `PATCH` | `/api/swimlanes/{id}/complete` | Mark as complete |
| `PATCH` | `/api/swimlanes/{id}/uncomplete` | Reactivate |
| `PATCH` | `/api/swimlanes/reorder` | Reorder swimlanes |
| `DELETE` | `/api/swimlanes/{id}` | Delete (soft) |

### Tasks

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/tasks` | Get all tasks |
| `GET` | `/api/tasks/swimlane/{id}` | Get tasks by lane |
| `GET` | `/api/tasks/{id}` | Get specific task |
| `POST` | `/api/tasks` | Create task |
| `PUT` | `/api/tasks/{id}` | Update task |
| `DELETE` | `/api/tasks/{id}` | Delete task |
| `PATCH` | `/api/tasks/{id}/move` | Move task |

### Real-Time Updates

| Endpoint | Description |
|----------|-------------|
| `GET /api/sse/stream` | SSE subscription |

**Event Types:**
- `init` — Connection established
- `task-updated` — Task created/modified
- `task-deleted` — Task removed
- `lane-updated` — Lane modified
- `heartbeat` — Keep-alive ping

---

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run with coverage report
mvn test jacoco:report
# Report: target/site/jacoco/index.html
```

**Test Summary:**
- ✅ 103 tests passing
- 📊 93% instruction coverage
- ⚡ Parallel execution enabled

---

## 📁 Project Structure

```
todo-app/
├── src/
│   ├── main/
│   │   ├── java/com/example/todo/
│   │   │   ├── controller/     # REST endpoints
│   │   │   ├── service/        # Business logic
│   │   │   ├── dao/            # Data access layer
│   │   │   ├── repository/     # JPA repositories
│   │   │   └── model/          # Entities
│   │   └── resources/
│   │       ├── static/
│   │       │   ├── css/        # Glassmorphism styles
│   │       │   └── js/         # Alpine.js modules
│   │       └── templates/      # Thymeleaf templates
│   └── test/                   # Unit & integration tests
├── CHANGELOG.md                # Version history
├── GEMINI.md                   # AI assistant rules
└── pom.xml                     # Maven config
```

---

## 🔧 Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8080` | Application port |
| `SPRING_PROFILES_ACTIVE` | `default` | Profile (dev/prod) |
| `SPRING_DATASOURCE_URL` | - | Database URL |
| `SPRING_DATASOURCE_USERNAME` | - | DB username |
| `SPRING_DATASOURCE_PASSWORD` | - | DB password |

### Profiles

| Profile | Description |
|---------|-------------|
| `default` | Local development with PostgreSQL |
| `prod` | Production with Azure PostgreSQL |
| `test` | H2 in-memory for tests |

---

## 🤝 Contributing

1. **Fork** the repository
2. Create a **feature branch**: `git checkout -b feat/amazing-feature`
3. **Commit** your changes: `git commit -m 'feat: Add amazing feature'`
4. **Push** to the branch: `git push origin feat/amazing-feature`
5. Open a **Pull Request**

### Commit Convention
- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation
- `refactor:` Code refactoring
- `test:` Adding tests
- `chore:` Maintenance

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- [Alpine.js](https://alpinejs.dev/) — Lightweight reactivity
- [Sortable.js](https://sortablejs.github.io/Sortable/) — Drag-and-drop
- [Spring Boot](https://spring.io/projects/spring-boot) — Backend framework
- [Bootstrap](https://getbootstrap.com/) — UI components

---

<p align="center">
  Made with ❤️ in 🇮🇳 India by <a href="https://github.com/shhrohan">@shhrohan</a>
</p>

<p align="center">
  <a href="#top">⬆ Back to Top</a>
</p>
