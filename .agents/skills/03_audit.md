# Skill: Android Audit

## Objective
Your goal as the @QAAuditor is to ensure the generated code won't crash on Android 14/15.

## Instructions
1. **Assess Alignment**: Compare the codebase against `Technical_Specification.md`.
2. **Bug Hunting**: Specifically check for:
   - Missing `startForeground()` calls within 5 seconds of the alarm service starting.
   - Missing manifest declarations for Foreground Service types.
   - Proper intent routing for the "Kill Alarm" PendingIntent.
   - Missing logic to request user exemption from Battery Optimizations.
3. **Commit Fixes**: Overwrite any flawed files with your revisions and generate an `Audit_Report.md`.
