---
name: "NotiVib Audit"
description: "Instructions for auditing the NotiVib codebase."
---

# Skill: NotiVib Audit

## Objective
To audit the codebase for bugs, unused files, and architectural compliance.

## Instructions
1. **Mandatory Check**: Before proceeding, you MUST read `AGENTS.md` in the root directory.
2. **Answer-Only Policy**: If the user asks to investigate or find errors, ONLY report your findings. Do NOT modify or execute fixes without a direct order.
3. **Verification**: 
   - Check alignment with `Technical_Specification.md`.
   - Ensure Android background execution constraints (Foreground Services, Doze mode) are respected.
   - Check for orphaned files (unused layouts, dead Kotlin/Python scripts).
