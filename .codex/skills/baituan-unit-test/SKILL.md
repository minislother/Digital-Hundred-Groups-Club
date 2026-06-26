---
name: baituan-unit-test
description: Generate or update unit tests for the Digital Hundred Groups Club project using the project TEST_RULES.md contract. Use when the user asks to create, supplement, modify, or run unit tests for baituan changes, especially requests mentioning TEST_RULES.md, unit tests, 单元测试, missing tests, or backend test generation. Scope follows the original rule and normally targets Spring Boot backend code under club_springboot.
---

# Baituan Unit Test

## Workflow

1. Read `references/original-rule.md` completely before creating or changing tests.
2. Identify the changed code or the missing-test items named by the user.
3. Follow the original rule for allowed scope, framework, naming, Given-When-Then structure, assertions, mocks, and test location.
4. Keep generated tests within `club_springboot/` when the rule limits test generation to backend code.
5. Do not create frontend, Android, or unrelated tests unless the user's latest request explicitly overrides the rule.
6. Run the relevant test command after editing whenever the local environment allows it.

## Test Style

- Use clear scenario-based method names and Chinese `@DisplayName` when the original rule requires it.
- Cover success, invalid input, null or empty input, exception paths, and boundary cases that are relevant to the changed code.
- Keep tests deterministic. Mock external dependencies instead of requiring live MySQL, Redis, Kafka, network services, or UI runtime unless the rule asks for integration tests.
- Do not invent a separate report unless the original rule explicitly asks for one.

## Reference

- `references/original-rule.md` is the copied source rule from `.codex/rules/TEST_RULES.md`.
