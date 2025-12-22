---
description: Autonomous frontend/backend debugging workflow
---

# Autonomous Debugging Workflow

This workflow guides autonomous debugging sessions.

## 1. Information Gathering
- [ ] **Read** relevant files to understand context.
- [ ] **Check** logs (`target/surefire-reports` or app logs).
- [ ] **Reproduce** the issue with a test case if possible.

## 2. Analysis
- [ ] **Trace** the execution flow.
- [ ] **Hypothesize** root causes.
- [ ] **Verify** hypotheses with targeted logging or tests.

## 3. Resolution
- [ ] **Apply** fix (remember Stage 1: Stop & Think).
- [ ] **Verify** fix (remember Stage 2.1: Frontend Verification).
- [ ] **Regression Test** (remember Stage 2.4: Mandatory Test Coverage).
