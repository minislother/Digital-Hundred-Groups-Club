# Git Workflow Rules

本文件用于记录项目 Git 工作流规则。

## Branch

- 如果当前在 `master`，先创建新分支再继续修改。
- 默认分支名可以使用 `codex-<task-name>`。
- 当前环境如果无法创建带 `/` 的分支名，可以使用 `codex-xxx` 形式。

## Diff

- 需要生成当前工作区相对 `master` 的 diff 时，输出到：

```text
current-vs-master.diff
```

- 如果当前分支就是 `master`，该 diff 表示工作区相对 `master` 的改动。

## Commit

- 提交前先确认 staged 文件。
- 不要把无关 `.idea/`、`target/`、测试输出、临时文件加入提交。
- 提交信息使用清晰的英文或中文摘要。

