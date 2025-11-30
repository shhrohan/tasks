---
description: Restart the Spring Boot application with mandatory commit
---

1. Commit all changes (if any)
// turbo
2. Kill existing Java process, Build, and Start Application
   - Command: `(git commit -am "WIP: Auto-commit before restart" || true) && pids=$(ps aux | grep "todo-app-0.0.1-SNAPSHOT.jar" | grep -v grep | awk '{print $2}') && if [ -n "$pids" ]; then kill -9 $pids; fi && mvn clean package -DskipTests && java -jar target/todo-app-0.0.1-SNAPSHOT.jar`
   - Cwd: `/Volumes/work data/work/projects/tasks/todo-app`
