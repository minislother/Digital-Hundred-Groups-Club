# Code Style Rules

本文件用于记录项目代码风格规则。

## Backend

- 保持 Spring Boot、MyBatis-Plus 当前项目风格。
- Controller 只处理请求参数、调用 service、返回结果。
- Service 放业务逻辑，不把业务逻辑写进 Controller。
- Mapper 只负责数据库访问。
- 异常处理优先走统一异常处理机制。

## Frontend

- 保持 Vue admin template 当前项目风格。
- API 请求统一放在 `src/api/`。
- 请求封装优先复用 `src/utils/request.js`。
- 避免在业务页面里散落重复请求逻辑。

## Formatting

- 避免无关格式化。
- 避免扩大历史乱码、换行符或编码变更范围。

