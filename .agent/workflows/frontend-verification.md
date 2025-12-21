---
description: Protocol for verifying frontend changes before applying them
---

# Frontend Verification Protocol (The Golden Rule)

> [!IMPORTANT]
> Never open new Tab. Always Hard Reload the current tab.

1. **Verify Correctness**:
   - Ensure the change works as expected in the *running* browser tab.
   - Do NOT reload the page unless necessary (reloading wipes injected code).

2. **Log Execution**:
   - Ensure your changes include console logs (e.g., `console.log('[App] ...')`) to visually prove execution in the browser console.

3. **Propagate to Source**:
   - ONLY after verification, copy the working code into the actual source file (e.g., `app.js`, `style.css`).

4. **Restart & Reload**:
   - After saving the source file, restart the backend (`mvn spring-boot:run`) if required (usually necessary for Thymeleaf/Java changes, or static resource serving changes).
   - Reload the browser page to verify the persistent fix.