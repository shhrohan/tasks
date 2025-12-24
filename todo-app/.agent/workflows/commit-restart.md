---
description: Standard cycle for applying changes, verifying, and committing
---

# Commit & Restart Cycle

This workflow enforces **Stage 2: Implementation** protocols from `GEMINI.md`.

## 1. Apply & Restart
// turbo
Apply changes, kill existing process, and restart the backend:
```bash
# Kill existing Spring Boot/Java process if running
pkill -f 'spring-boot:run' || true
pkill -f 'java -jar' || true
# Restart the application
mvn spring-boot:run
```

## 2. Browser Verification
- [ ] **Inject** JS/CSS in the browser console/tools first.
- [ ] **Verify** the fix works in the running tab (`http://localhost:8080`).
- [ ] **Log** `console.log('[App] ...')` for frontend verification.

## 3. Run Tests
// turbo
Ensure no regressions:
```bash
mvn test
```

## 4. Commit Protocol
- [ ] **Ask** user for permission: "Validation complete... Do you approve?"
- [ ] **Commit** with semantic message: `git commit -am "feat/fix: ..."`
