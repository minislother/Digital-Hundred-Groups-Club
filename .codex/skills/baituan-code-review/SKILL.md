---
name: baituan-code-review
description: Perform Digital Hundred Groups Club backend code review using the project CODE_REVIEW_RULES.md contract. Use when the user asks to review a diff, branch, commit, or backend change set for the baituan project, especially requests mentioning CODE_REVIEW_RULES.md, CodeReview, Code Review, CODE_REVIEW_REPORT.md, or review reports. Scope is the Spring Boot backend under club_springboot unless the original rule explicitly says otherwise.
---

# Baituan Code Review

## Workflow

1. Read `references/original-rule.md` completely before reviewing code.
2. Resolve the target diff or change set requested by the user.
3. Apply the original rule as the source of truth for scope, severity, report structure, and output file names.
4. Keep review scope focused on `club_springboot/` backend content when the rule limits analysis to backend code.
5. Do not invent extra reports, timestamps, suffixes, or sections beyond the rule template.
6. If a requested target conflicts with the original rule, state the conflict and follow the user's latest explicit instruction only when it is unambiguous.

## Output Discipline

- Write the review result only where the original rule requires it.
- Prefer the fixed report name required by the rule, such as `CODE_REVIEW_REPORT.md`, when applicable.
- Lead with actionable findings. Include file paths and concrete evidence.
- Do not analyze generated artifacts, dependency folders, build output, or unrelated dirty worktree changes unless the rule or user explicitly asks.

## Reference

- `references/original-rule.md` is the copied source rule from `.codex/rules/CODE_REVIEW_RULES.md`.
