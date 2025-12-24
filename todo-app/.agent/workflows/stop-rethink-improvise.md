---
description: stop rethink and improvise checklist
---

# Pre-Implementation Workflow

This workflow enforces the **Stage 1: Pre-Implementation** protocols defined in `GEMINI.md`. Run this before starting any coding task to ensure alignment and optimizing.

## 1. Stop & Think (Optimization)
- [ ] **Pause** and ask: "Is this the **BEST**/**CLEANEST**/**EFFICIENT**/**SCALABLE**/**MAINTAINABLE**/**TESTABLE**/**EXTENSIBLE**/**PERFORMANT**/**SECURE**/**RELIABLE**?"
- [ ] **Propose** better alternatives if the request is sub-optimal.
- [ ] **Innovate** by thinking of unique solutions with user guidance.
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
Check project status and recent commits:
```bash
git status
git branch
git log -n 5 --oneline
```

// turbo
Verify project structure:
```bash
ls -R | grep ":$" | head -n 20
```

// turbo
Check for recent changes in the changelog:
```bash
cat CHANGELOG.md | head -n 30
```