---
description: Steps to run tests and verify coverage
---

# Run Tests & Verify Coverage

1. **Run Unit Tests**:
   - Command: `mvn test`
   - Wait for "BUILD SUCCESS".

2. **Check Coverage (Optional)**:
   - If coverage is critical, parse the Jacoco report.
   - Command: `cat target/site/jacoco/jacoco.csv | awk -F, 'NR>1 { missed+=$6; total+=$6+$7 } END { print "Branch Coverage:", (1 - missed/total)*100, "%" }'`

3. **Verify No Regressions**:
   - Ensure `Failures: 0, Errors: 0`.
