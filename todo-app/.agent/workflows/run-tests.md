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
Check coverage report (Mac/Linux compatible):
```bash
mvn clean test
grep -A 5 "Total" target/site/jacoco/index.html | head -n 6
```

## 3. Compliance Check
- [ ] **Verify** new features/fixes have tests.
- [ ] **Branch Coverage**: Maintain >90% branch coverage for service and utility classes.
- [ ] **Pass** all tests before asking for commit approval.
- [ ] **Warn** user if skipping tests (only if explicitly requested).
