---
description: Steps to finalize a feature or task
---

# Feature Completion Workflow

This workflow enforces **Stage 3: Post Feature Implementation** protocols from `GEMINI.md`.

## 1. User Confidence Check
- [ ] **Confirm** with user: "Are you confident of the changes?"
- [ ] **Wait** for specific approval (e.g., "ship it", "looks good").

## 2. Final Commit
// turbo
Create semantic commit if needed:
```bash
git commit -am "feat/fix: Final description"
```

## 3. Documentation Updates
- [ ] **Update** `CHANGELOG.md` under `[Unreleased]`.
- [ ] **Update** `GEMINI.md` with new patterns or instructions.
- [ ] **Review** `task.md` for completion status.

## 4. Merge Policy
- [ ] **Check** if user requested merge to `main`.
- [ ] **NEVER** merge automatically without explicit request.

## 5. Final Confirmation
- [ ] **Notify** user: "ALL STAGES ✓ COMPLETE: Code, tests, CHANGELOG, and GEMINI.md are synced."
