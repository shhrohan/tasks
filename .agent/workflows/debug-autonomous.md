---
description: Autonomous frontend/backend debugging workflow
---

# Autonomous Debugging Workflow

This workflow enables autonomous debugging of both frontend and backend without waiting for user input.

## Prerequisites
- The todo-app is running on localhost:8081
- Browser is open to the application

## Steps

// turbo-all

### 1. Restart Backend (if needed)
```bash
# Kill existing process on port 8081
Get-NetTCPConnection -LocalPort 8081 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

### 2. Start Backend
```bash
cd "z:\garage projects\tasks\todo-app"
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```
Wait for "Started TodoApplication" in logs before proceeding.

### 3. Verify Backend Health
```bash
curl http://localhost:8081/api/swimlanes
```
Should return JSON array of swimlanes.

### 4. Frontend Verification via Browser Subagent
Use browser_subagent to:
1. Navigate to http://localhost:8081
2. Resize to mobile dimensions (390x844)
3. Capture screenshot
4. Test specific UI element (hamburger, sidebar, etc.)
5. Inject JavaScript fixes if needed
6. Verify fix works
7. Report findings

### 5. Apply Fixes to Source
If browser injection works:
1. Propagate CSS changes to style.css
2. Propagate JS changes to app.js
3. Propagate HTML changes to index.html

### 6. Restart and Verify
1. Restart backend
2. Clear browser cache (Ctrl+Shift+R)
3. Re-run browser verification

## Key Principles
- **NEVER ask user to test** - use browser_subagent instead
- **ALWAYS restart backend** after code changes
- **ALWAYS use mobile viewport** (390x844) for mobile testing
- **ALWAYS capture screenshots** before and after changes
- **ALWAYS inject and verify** before modifying source files
