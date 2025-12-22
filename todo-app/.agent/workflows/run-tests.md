---
description: Steps to run tests and verify coverage
---

# Run Tests Workflow

This workflow enforces **Stage 2.4: Mandatory Test Coverage** protocols from `GEMINI.md`.

## 1. Execute Tests
// turbo
Run the full test suite:
```bash
mvn test
```

## 2. Coverage Verification
// turbo
Check coverage report:
```bash
mvn clean test
Get-Content target/site/jacoco/index.html | Select-String -Pattern "Total" -Context 0,5
```

## 3. Compliance Check
- [ ] **Verify** new features/fixes have tests.
- [ ] **Pass** all tests before asking for commit approval.
- [ ] **Warn** user if skipping tests (only if explicitly requested).
