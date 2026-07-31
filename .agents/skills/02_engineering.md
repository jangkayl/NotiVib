---
name: "NotiVib Engineering"
description: "Instructions for modifying the NotiVib codebase."
---

# Skill: NotiVib Engineering

## Objective
To implement features, fix bugs, and modify the NotiVib Android application safely and correctly.

## Instructions
1. **Mandatory Check**: Before proceeding, you MUST read `AGENTS.md` in the root directory.
2. **Execution Workflow**:
   - Only modify code if the user gives a direct order.
   - For non-trivial changes, create an implementation plan and request user approval first.
   - Execute changes across all relevant Clean Architecture layers (Model -> Data -> Domain -> Presentation).
3. **Compilation**: After making code changes, you must verify the build by running:
   `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat compileDebugKotlin`
4. **No Dead Code**: Remove unused imports, variables, and files when refactoring. Preserve existing unrelated docstrings.
