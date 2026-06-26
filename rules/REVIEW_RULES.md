# Review Rules

本文件用于补充 code review 规则。

正式 code review 仍以根目录 `CODE_REVIEW_RULES.md` 为准。

## Output

- 问题优先，摘要靠后。
- 按严重程度排序。
- 每个问题包含文件位置、问题说明、影响和建议。
- 没有明确问题时说明“未发现明显问题”，并列出剩余风险。

## Focus

- 权限认证
- 文件上传下载
- 数据一致性
- 缓存一致性
- 消息队列
- 异常处理
- 前端 token 和请求封装

