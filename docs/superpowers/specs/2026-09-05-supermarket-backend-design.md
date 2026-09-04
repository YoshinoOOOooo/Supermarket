# 本地超市后端设计规格

## 1. 项目目标

构建一个简便、可本地部署、无前端的超市后端。系统支持苹果、草莓、芒果等按斤销售的商品，完成原价、单品折扣和订单满减计算；管理员可以维护商品、价格和促销规则，并处理订单状态。

项目重点是清晰展示 Java 面向对象设计、可扩展计价规则、后端认证、数据库持久化和自动化测试。系统不包含顾客账号、库存、支付网关、退款、配送和前端页面。

## 2. 技术栈

- Java 8
- Spring Boot 2.7.x
- Maven
- Spring Web
- Spring Security + HTTP Basic
- MySQL 8.x
- MyBatis-Plus
- Bean Validation
- springdoc-openapi / Swagger UI
- JUnit 5、Mockito、MockMvc
- BigDecimal 处理金额

应用采用单个 Maven 模块，构建为一个可执行 JAR。数据库连接和管理员凭据通过环境变量或本地配置提供，仓库不保存真实密码。

## 3. 包结构与职责

根包使用 `com.supermarket`：

```text
com.supermarket
├─ controller       REST 接口、输入校验和响应组装
├─ service          业务能力接口
├─ serviceimpl      业务流程编排和事务
├─ mapper           MyBatis-Plus 数据访问接口
├─ entity           数据库实体
├─ dto              API 请求对象
├─ vo               API 响应对象
├─ pricing          计价上下文及促销策略
├─ enums            促销类型、订单状态等枚举
├─ config           Security、MyBatis 和 Swagger 配置
└─ exception        业务异常及统一异常处理
```

Controller 不直接访问 Mapper，Controller 不返回 Entity。Service 接口放在 `service`，实现放在同级 `serviceimpl`。项目保持单体和单模块，不拆分微服务或 Maven 子模块。

## 4. 核心组件

### 4.1 商品服务

`ProductService` 负责商品的新增、查询、修改和启停。商品编码全局唯一且创建后不可更改，名称和单价可以修改。对外的“删除”采用停用语义，保留历史订单引用和审计信息。

### 4.2 促销服务

`PromotionService` 负责商品折扣和订单满减规则的新增、查询、修改及启停。规则配置必须符合对应类型的字段约束：

- `PRODUCT_DISCOUNT` 必须关联商品，折扣率大于 0 且不大于 1。
- `ORDER_THRESHOLD_REDUCTION` 不关联商品，门槛金额大于 0，减免金额大于 0 且不大于门槛金额。

同一商品在同一时间最多生效一条商品折扣；同一时间最多生效一条订单满减。服务在写入或启用规则时校验冲突。

### 4.3 计价服务

`CheckoutService` 接收购物项并读取当前有效商品和促销。`PricingRule` 定义统一规则接口，首期实现：

- `ProductDiscountRule`：按商品应用折扣。
- `OrderThresholdReductionRule`：对单品折扣后的整单金额应用满减。

规则按优先级执行，但业务约束固定为单品折扣先于订单满减。斤数使用非负整数；金额使用 `BigDecimal`，人民币结果按 `HALF_UP` 保留两位小数。服务端只信任商品编码和斤数，不接受客户端提交的价格或总价。

### 4.4 订单服务

试算不持久化。正式下单时，`OrderService` 在单个数据库事务中重新读取商品和促销、重新计价，并保存订单及明细快照。历史订单保存商品名称、成交单价、折扣和实付金额，不受后续商品或促销修改影响。

订单状态为：

```text
UNPAID ──确认收款──> COMPLETED
   │
   └────取消──────> CANCELLED
```

`COMPLETED` 和 `CANCELLED` 是终态。重复执行已经达成的目标状态按幂等成功处理；其他非法转换返回冲突错误。当前不对接支付网关，确认收款由管理员执行。

## 5. 数据模型

### 5.1 product

- `id`：主键
- `code`：唯一商品编码
- `name`：商品名称
- `unit_price`：`DECIMAL(10,2)`，大于等于 0
- `enabled`：是否可售
- `created_at`、`updated_at`

### 5.2 promotion

- `id`：主键
- `name`：规则名称
- `type`：`PRODUCT_DISCOUNT` 或 `ORDER_THRESHOLD_REDUCTION`
- `product_id`：商品折扣关联的商品，满减时为空
- `discount_rate`：折扣率
- `threshold_amount`：满减门槛
- `reduction_amount`：减免金额
- `priority`：执行优先级
- `enabled`：是否启用
- `start_time`、`end_time`：可为空的生效区间
- `created_at`、`updated_at`

### 5.3 customer_order

- `id`：主键
- `order_no`：不可预测的 UUID，唯一
- `status`：`UNPAID`、`COMPLETED` 或 `CANCELLED`
- `original_amount`：商品原价合计
- `discount_amount`：优惠合计
- `payable_amount`：应付金额
- `created_at`、`updated_at`

### 5.4 order_item

- `id`：主键
- `order_id`：订单外键
- `product_id`：商品引用
- `product_code`、`product_name`：下单快照
- `quantity`：非负整数斤数
- `unit_price`：成交时原单价
- `original_amount`：明细原价
- `discount_amount`：明细商品优惠
- `payable_amount`：明细折后金额

订单级满减记录在订单优惠总额中。订单优惠总额等于所有明细商品优惠加订单级满减。

初始化 SQL 提供苹果 `8.00` 元/斤、草莓 `13.00` 元/斤、芒果 `20.00` 元/斤，以及草莓 8 折和折后满 100 减 10 的默认规则。

## 6. API 与权限

### 6.1 公开接口

- `POST /api/checkout/calculate`：试算，不生成订单。
- `POST /api/orders`：服务端重新计价并创建 `UNPAID` 订单。
- `GET /api/orders/{orderNo}`：根据 UUID 订单号查询详情。
- Swagger/OpenAPI 文档和健康检查接口允许本地访问。

试算和下单请求示例：

```json
{
  "items": [
    {"productCode": "APPLE", "quantity": 2},
    {"productCode": "STRAWBERRY", "quantity": 3},
    {"productCode": "MANGO", "quantity": 1}
  ]
}
```

请求中商品编码不能重复；空购物项、负数斤数、小数斤数均为非法。允许单项数量为 0，但至少应有一个数量大于 0 的购物项才能正式下单；全为 0 的试算结果为 `0.00`。

### 6.2 管理员接口

- `/api/admin/products/**`：商品新增、修改、启停和查询。
- `/api/admin/promotions/**`：促销新增、修改、启停和查询。
- `GET /api/admin/orders`：分页查询订单。
- `POST /api/admin/orders/{orderNo}/complete`：确认收款。
- `POST /api/admin/orders/{orderNo}/cancel`：取消未支付订单。

所有 `/api/admin/**` 接口使用 Spring Security HTTP Basic 认证。首期只有一个本地管理员，不创建用户表、不签发 JWT。

## 7. 结算数据流

1. Bean Validation 和业务校验验证购物项。
2. 按商品编码批量查询可售商品，避免逐项查询。
3. 使用数据库单价计算每个明细原价。
4. 查询当前时间有效且启用的促销规则。
5. 先执行商品折扣并汇总折后金额。
6. 以折后金额判断满减门槛；满 100 元只减 10 元一次，不循环满减。
7. 将结果统一舍入为两位小数并返回。
8. 正式下单时在事务内保存相同计算结果及快照；试算不执行持久化。

## 8. 异常处理

统一异常响应字段为 `code`、`message`、`timestamp`：

- `400 Bad Request`：空购物项、非法斤数、重复商品、非法促销参数。
- `401 Unauthorized`：管理员凭据缺失或错误。
- `404 Not Found`：商品或订单不存在。
- `409 Conflict`：商品停用、促销冲突、订单状态转换非法。
- `500 Internal Server Error`：未预期异常；响应不暴露堆栈和数据库信息。

数据库唯一约束作为并发场景的最后防线，业务异常由全局异常处理器转换为稳定的 API 响应。

## 9. 测试与验收

### 9.1 题目场景

- A：苹果 2 斤、草莓 3 斤，无促销，结果 `55.00`。
- B：苹果 2 斤、草莓 3 斤、芒果 1 斤，无促销，结果 `75.00`。
- C：与 B 相同，草莓 8 折，结果 `67.20`。
- D：苹果 5 斤、草莓 5 斤、芒果 1 斤；折后 `112.00`，满减后 `102.00`。

### 9.2 边界与行为

- 全为 0 斤的试算返回 `0.00`。
- 负数、小数、空购物项及重复商品被拒绝。
- 折后恰好 `100.00` 触发满减，`99.99` 不触发。
- 金额按 `HALF_UP` 保留两位。
- 不存在或停用商品不能结算。
- 商品折扣先于订单满减。
- 试算不产生订单，下单保存金额快照。
- 合法订单转换成功，终态不能非法变更。
- 管理接口未认证返回 `401`，公开接口无需认证。

### 9.3 测试分层

- 计价策略单元测试：纯 Java 测试金额公式、执行顺序和边界。
- Service 测试：使用 Mockito 隔离 Mapper，验证流程、事务边界和状态机。
- Controller/Security 测试：使用 MockMvc 验证参数、响应结构和访问权限。
- MySQL 集成验证：使用独立测试配置及初始化 SQL，在本地 MySQL 执行 Mapper 和完整 API 验证。

## 10. 本地运行与交付

README 说明以下内容：

1. JDK 8、Maven 和 MySQL 8.x 前置要求。
2. 创建数据库及执行初始化 SQL。
3. 通过环境变量配置数据库 URL、用户名、密码和管理员凭据。
4. 执行 Maven 测试和启动应用。
5. 通过 Swagger UI 或示例 curl 命令演示商品、促销、试算、下单和状态转换。

首期交付不包含前端、部署容器、云服务、支付、库存、顾客账号或多管理员权限模型。
