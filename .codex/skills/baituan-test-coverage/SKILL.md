---
name: baituan-test-coverage
description: Analyze unit test coverage for the Digital Hundred Groups Club project using the project TEST_ACC_RULES.md contract. Use when the user asks for test coverage analysis, 单元测试覆盖率分析, TEST_ACC_RULES.md, TEST_ACC_REPORT.md, or coverage gaps for baituan diffs or test changes. Scope follows the original rule and normally targets Spring Boot backend code under club_springboot.
---

# Baituan Test Coverage

## Workflow

1. Read `references/original-rule.md` completely before analyzing coverage.
2. Resolve the diff, test files, or changed backend code requested by the user.
3. Run the coverage or test command required by the original rule when available.
4. Analyze coverage only for the scope permitted by the rule, normally backend content under `club_springboot/`.
5. Write the coverage analysis only in the required report file and template, such as `TEST_ACC_REPORT.md`, when the rule specifies it.
6. Do not generate extra coverage reports, timestamps, alternate filenames, or unrelated summaries.

## Analysis Discipline

- Tie every missing-coverage point to concrete changed code or test files.
- Separate uncovered branches, exception paths, validation paths, and integration risks when the rule asks for that distinction.
- If coverage tooling cannot run, record the blocker clearly and still provide static analysis only when the rule allows it.
- Ignore frontend, Android, build output, generated files, dependency folders, and unrelated changes unless the latest user request explicitly overrides the rule.

## Reference

- `references/original-rule.md` is the copied source rule from `.codex/rules/TEST_ACC_RULES.md`.
