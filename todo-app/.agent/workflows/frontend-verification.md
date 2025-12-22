---
description: Protocol for verifying frontend changes before applying them
---

# Frontend Verification Protocol

This workflow enforces **Stage 2.1: Frontend Verification** protocols from `GEMINI.md`.

## 1. Browser Injection (MANDATORY)
- [ ] **Console**: Use for JS logic verification.
- [ ] **DOM Editing**: Use for HTML structure verification.
- [ ] **Style Editor**: Use for CSS verification.

> [!IMPORTANT]
> **NEVER** modify source files blindly. Verify in the running browser first.

## 2. Propagation
- [ ] **Apply** verified changes to source code.
- [ ] **Log**: Ensure `console.log('[App] ...')` is present for execution proof.

## 3. Verification
- [ ] **Restart** application.
- [ ] **Verify** persistence of the change.
