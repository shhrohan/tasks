---
description: Pre-implementation checklist and context gathering protocol
---

# Pre-Implementation Workflow

This workflow enforces the **Stage 1: Pre-Implementation** protocols defined in `GEMINI.md`. Run this before starting any coding task to ensure alignment and optimizing.

## 1. Stop & Think (Optimization)
- [ ] **Pause** and ask: "Is this the best/cleanest way?"
- [ ] **Propose** better alternatives if the request is sub-optimal.
- [ ] **Refuse** blind obedience to bad patterns.

## 2. Understand Scope
- [ ] **Read** `GEMINI.md` to understand existing patterns (AOP, Caching, UI).
- [ ] **Identify** affected files and components.
- [ ] **Check** for similar existing implementations.

## 3. Plan the Approach
- [ ] **Outline** the approach for complex changes.
- [ ] **Confirm** requirements if ambiguous.
- [ ] **Create/Update** `implementation_plan.md` if the task is significant.

## 4. Context Gathering (Automated)
// turbo
Run the following checks to understand the current state:

```bash
git status
git branch
```

// turbo
Check for recent changes in the changelog:
```bash
cat CHANGELOG.md | head -n 30
```
