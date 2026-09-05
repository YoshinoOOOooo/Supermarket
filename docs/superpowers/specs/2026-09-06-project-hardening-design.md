# 超市后端审计整改设计

## 目标

在保持 Java 8、Spring Boot 2.7、MyBatis-Plus、MySQL、HTTP Basic 和本地单体部署方式不变的前提下，修复全项目审计发现的安全、数据库测试、并发一致性、API 一致性、性能和文档问题。整改不引入 Redis、JWT、Flyway 或新的运行时服务。

## 配置与凭据

`application.yml` 不再保存真实数据库密码或管理员密码，数据库连接和管理员凭据统一从 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`ADMIN_USERNAME`、`ADMIN_PASSWORD` 环境变量读取。非敏感配置可以保留本地默认值，两个密码不提供默认值，缺失时应用启动失败并给出明确配置错误。

本次不重写 Git 历史，也不修改本机 MySQL 用户。README 必须提醒：已经提交过的凭据需要人工更换；删除当前文件中的密码不能使旧提交失效。

## 数据库与测试

生产资源只保留 `src/main/resources/db/init.sql` 作为从零初始化入口。MySQL 验收测试使用 `src/test/resources/db/` 下的专用 schema 和种子脚本，脚本不包含 `CREATE DATABASE` 或 `USE supermarket`，并继续在代码中校验 JDBC schema 必须是 `supermarket_test`，避免测试误删开发数据。

默认单元测试继续不依赖本机 MySQL；显式 MySQL 验收命令负责验证真实 Mapper、事务、计价、订单状态和可空促销字段更新。

并发控制在 `customer_order` 和 `promotion` 表增加 `version` 字段，并在实体上使用 MyBatis-Plus `@Version`。更新失败时返回稳定的 409 业务错误。`init.sql` 面向新数据库包含版本字段；README 提供已有数据库的一次性 `ALTER TABLE` 命令，不额外恢复生产迁移脚本。

## 写入后的响应一致性

商品和促销的新增、修改及启停操作在写入成功后重新读取数据库，再构造 VO。这样数据库生成的主键、`created_at`、`updated_at` 和最终持久化空值均与 HTTP 响应一致。写入影响行数不是 1 时返回明确冲突或不存在错误。

## API 边界

- 促销时间继续使用 `LocalDateTime`，OpenAPI 明确格式为不带 `Z` 的 `yyyy-MM-dd'T'HH:mm:ss`，`null` 表示无边界。
- 单次购物项数量设置合理上限，每项斤数设置上限，同时保持题目要求的非负整数语义。
- 管理员订单分页设置最大页大小。
- 商品编码预检查和数据库唯一键冲突统一映射为 `409 RESOURCE_CONFLICT`。
- 公开订单与试算接口按原需求保留；README 说明它们仅适合本地部署，不应直接暴露公网。

## 查询与异常

订单分页先查询一页订单，再使用一次 `order_id IN (...)` 批量加载全部明细并分组，消除逐单查询的 N+1 问题。单订单查询保持原逻辑。

全局异常处理器记录未预期异常的完整堆栈，客户端仍只收到通用 500 响应；业务异常和请求格式错误不记录敏感请求内容。

## 文件清理

- 删除已被根 `.gitignore` 覆盖的 `.idea/.gitignore`。
- 删除本地忽略目录 `.superpowers/` 和构建产物 `target/`；测试期间产生的 `target/` 在最终验证后再次清理。
- 保留 `docs/superpowers/specs` 和 `docs/superpowers/plans`，它们是项目设计与实施记录，不视为无用文件。
- 不删除 `.worktrees` 或当前 worktree，因为当前任务就在其中运行。

## 测试策略

所有行为修改遵循红—绿—重构：先补充失败测试，再写最小实现。重点覆盖：环境变量缺失、商品和促销数据库回读、乐观锁失败、同订单并发修改、批量明细查询、请求上限、时间格式文档、异常日志以及 MySQL 专用脚本加载。

完成条件：默认 Maven 测试全部通过；若专用 MySQL 测试环境变量可用，再运行 MySQL 验收测试；`git diff --check` 通过；工作区仅保留用户明确要求保留的未提交内容；README 与实际文件和命令一致。
