---
description: Steps to finalize a feature or task
---

# Feature Completion Protocol

When the user is confident in the changes:

1. **Commit**:
   - Ensure you have a semantic commit message.
   - Example: `git commit -am "feat: Add drag and drop support"`

2. **Update Changelog**:
   - Open `todo-app/CHANGELOG.md`.
   - Add a new entry under `[Unreleased]`.
   - Format: `- Added/Fixed/Changed: Description of change`.

3. **Update GEMINI.md**:
   - Open `.gemini/GEMINI.md`.
   - Update "Key Features", "Architecture", or "API Reference" if changed.
   - Delete outdated instructions if applicable.

4. **Notify User**:
   - Inform the user that documentation and changelogs are up to date.
