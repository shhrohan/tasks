---
description: Standard cycle for applying changes, verifying, and committing
---

# Commit & Restart Cycle

1. **Apply Changes**:
   - Make necessary code edits.

2. **Restart Application**:
   - Kill existing process: `lsof -i :8080 -t | xargs kill -9` (or relevant port).
// turbo
   - Start application: `mvn spring-boot:run`
   - Wait for startup log (e.g., "Started TodoApplication").

3. **Verify Functionality**:
   - Use the `browser_subagent` to visit the local URL.
   - Perform user actions to verify the fix/feature.

4. **Request Commit Permission**:
   - Ask the user: "Changes verified. May I commit?"

5. **Commit**:
   - Upon approval, run: `git commit -am "feat/fix: <Description>"`
