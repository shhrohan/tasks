---
description: Autonomous frontend/backend debugging workflow
---

# Autonomous Debugging Protocol

1. **Analyze Symptoms**:
   - Check browser console logs using `browser_subagent`.
   - Check backend logs: `cat logs/application.log` (or terminal output).

2. **Isolate Issue**:
   - Determine if it's Frontend (JS/Alpine) or Backend (Java/API).

3. **Frontend Debugging**:
   - Inject `console.log` into the live browser session if possible.
   - Verify variable states.
   - Check for "Element not found" or "Alpine error".

4. **Backend Debugging**:
   - Add debug logging to Controller/Service.
   - Restart app: `mvn spring-boot:run`.
   - Replicate issue.

5. **Apply Fix**:
   - Follow `frontend-verification.md` for JS/HTML fixes.
   - Follow `commit-restart.md` for Backend fixes.
