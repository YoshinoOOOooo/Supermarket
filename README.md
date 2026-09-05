# 本地超市后端

这是一个基于 Java 8、Spring Boot 2.7、MyBatis-Plus 和 MySQL 8 的本地 REST 后端。它支持商品与促销管理、购物车试算、订单创建与查询，以及管理员确认收款或取消订单。金额统一以两位小数字符串返回；管理员接口使用 HTTP Basic。

## 1. 环境要求

- JDK 8
- Maven 3.8+
- MySQL 8.x
- PowerShell 5.1+（以下环境变量和 API 示例按 PowerShell 编写）

检查工具：

```powershell
java -version
mvn -version
mysql --version
```

## 2. 初始化本地数据库

完整初始化脚本会依次创建 `supermarket` 数据库、全部数据表和演示数据。命令会提示输入 MySQL 密码：

```powershell
cmd /c "mysql -u root -p < src\main\resources\db\init.sql"
```

在 Bash、Git Bash 或 CMD 中可直接写为：

```bash
mysql -u root -p < src/main/resources/db/init.sql
```

`init.sql` 从零完成建库、建表和初始化，写入苹果（8.00 元/斤）、草莓（13.00 元/斤）、芒果（20.00 元/斤）、草莓 8 折和满 100 减 10。脚本可重复执行，但会恢复这些演示基准配置。原 `schema.sql` 和 `data.sql` 继续供自动化测试加载。

## 3. 配置与启动

开发数据库和管理员账号已经直接配置在 `src/main/resources/application.yml` 中，本机数据库名称为 `supermarket`。本地演示无需再设置环境变量；如需更换账号或密码，请直接修改该文件。

运行快速测试和启动应用：

```powershell
mvn test
mvn spring-boot:run
```

服务默认地址为 `http://localhost:8080`。可访问：

- Swagger UI：<http://localhost:8080/swagger-ui.html>
- OpenAPI JSON：<http://localhost:8080/v3/api-docs>
- 健康检查：<http://localhost:8080/actuator/health>

`/api/checkout/**` 和 `/api/orders/**` 是公开接口；`/api/admin/**` 必须使用上面设置的 Basic 凭据。

## 4. 接口演示

先准备公共变量和管理员认证头：

```powershell
$base = "http://localhost:8080"
$pair = "${env:ADMIN_USERNAME}:${env:ADMIN_PASSWORD}"
$basic = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($pair))
$headers = @{ Authorization = "Basic $basic" }
```

### 商品维护

查看商品并更新商品（把 `$productId` 替换为列表中的真实 ID）：

```powershell
Invoke-RestMethod -Method Get -Uri "$base/api/admin/products" -Headers $headers
$productId = 1
$product = '{"name":"Fresh Apple","unitPrice":8.50}'
Invoke-RestMethod -Method Put -Uri "$base/api/admin/products/$productId" -Headers $headers -ContentType "application/json" -Body $product
Invoke-RestMethod -Method Patch -Uri "$base/api/admin/products/$productId/enabled?enabled=true" -Headers $headers
```

### 促销启用

查看促销。初始化 SQL 没有写死促销主键，因此应从管理接口返回值中取得真实 ID；启停接口使用查询参数 `enabled`，不需要请求体：

```powershell
$promotions = Invoke-RestMethod -Method Get -Uri "$base/api/admin/promotions" -Headers $headers
$strawberryDiscount = $promotions | Where-Object { $_.type -eq "PRODUCT_DISCOUNT" -and $_.name -eq "Strawberry 80% Discount" } | Select-Object -First 1
$thresholdReduction = $promotions | Where-Object { $_.type -eq "ORDER_THRESHOLD_REDUCTION" -and $_.name -eq "Spend 100 Save 10" } | Select-Object -First 1

if ($null -eq $strawberryDiscount -or $null -eq $thresholdReduction) {
    throw "未找到 data.sql 初始化的两条促销，请重新执行初始化 SQL"
}

$strawberryDiscountId = $strawberryDiscount.id
$thresholdReductionId = $thresholdReduction.id
```

### A–D 试算

`data.sql` 默认启用两项促销。下面的命令在每个场景前都通过真实管理接口明确设置两条规则的状态，因此从默认状态开始按顺序整段执行即可复现四个题目结果。

```powershell
$scenarioA = '{"items":[{"productCode":"APPLE","quantity":2},{"productCode":"STRAWBERRY","quantity":3}]}'
$scenarioB = '{"items":[{"productCode":"APPLE","quantity":2},{"productCode":"STRAWBERRY","quantity":3},{"productCode":"MANGO","quantity":1}]}'
$scenarioC = $scenarioB
$scenarioD = '{"items":[{"productCode":"APPLE","quantity":5},{"productCode":"STRAWBERRY","quantity":5},{"productCode":"MANGO","quantity":1}]}'

# A：关闭草莓 8 折，关闭满 100 减 10
Invoke-RestMethod -Method Patch -Uri "$base/api/admin/promotions/$strawberryDiscountId/enabled?enabled=false" -Headers $headers
Invoke-RestMethod -Method Patch -Uri "$base/api/admin/promotions/$thresholdReductionId/enabled?enabled=false" -Headers $headers
Invoke-RestMethod -Method Post -Uri "$base/api/checkout/calculate" -ContentType "application/json" -Body $scenarioA # 55.00，无促销

# B：关闭草莓 8 折，关闭满 100 减 10
Invoke-RestMethod -Method Patch -Uri "$base/api/admin/promotions/$strawberryDiscountId/enabled?enabled=false" -Headers $headers
Invoke-RestMethod -Method Patch -Uri "$base/api/admin/promotions/$thresholdReductionId/enabled?enabled=false" -Headers $headers
Invoke-RestMethod -Method Post -Uri "$base/api/checkout/calculate" -ContentType "application/json" -Body $scenarioB # 75.00，无促销

# C：启用草莓 8 折，关闭满 100 减 10
Invoke-RestMethod -Method Patch -Uri "$base/api/admin/promotions/$strawberryDiscountId/enabled?enabled=true" -Headers $headers
Invoke-RestMethod -Method Patch -Uri "$base/api/admin/promotions/$thresholdReductionId/enabled?enabled=false" -Headers $headers
Invoke-RestMethod -Method Post -Uri "$base/api/checkout/calculate" -ContentType "application/json" -Body $scenarioC # 67.20，仅草莓 8 折

# D：启用草莓 8 折，启用满 100 减 10
Invoke-RestMethod -Method Patch -Uri "$base/api/admin/promotions/$strawberryDiscountId/enabled?enabled=true" -Headers $headers
Invoke-RestMethod -Method Patch -Uri "$base/api/admin/promotions/$thresholdReductionId/enabled?enabled=true" -Headers $headers
Invoke-RestMethod -Method Post -Uri "$base/api/checkout/calculate" -ContentType "application/json" -Body $scenarioD # 102.00，两项促销
```

### 订单生命周期

创建订单、公开查询、管理员确认完成：

```powershell
$created = Invoke-RestMethod -Method Post -Uri "$base/api/orders" -ContentType "application/json" -Body $scenarioD
$orderNo = $created.orderNo
$created.status # UNPAID

Invoke-RestMethod -Method Get -Uri "$base/api/orders/$orderNo"
# 仅 UNPAID 可修改；服务端会按当前商品与促销重新计价并替换快照
$changed = '{"items":[{"productCode":"APPLE","quantity":3}]}'
Invoke-RestMethod -Method Put -Uri "$base/api/orders/$orderNo" -ContentType "application/json" -Body $changed
Invoke-RestMethod -Method Post -Uri "$base/api/admin/orders/$orderNo/complete" -Headers $headers
```

取消订单使用另一个仍为 `UNPAID` 的订单：

```powershell
$toCancel = Invoke-RestMethod -Method Post -Uri "$base/api/orders" -ContentType "application/json" -Body $scenarioA
Invoke-RestMethod -Method Post -Uri "$base/api/admin/orders/$($toCancel.orderNo)/cancel" -Headers $headers
```

也可以使用 `curl.exe` 验证 Basic 认证，例如：

```powershell
curl.exe -u "${env:ADMIN_USERNAME}:${env:ADMIN_PASSWORD}" "$base/api/admin/products"
```

## 5. 测试

默认测试会通过 Maven Surefire 的 `excludedGroups=mysql` 排除需要本地数据库的验收测试：

```powershell
mvn clean test
mvn -DskipTests package
```

### 可选 MySQL 验收测试

验收测试会删除并重建测试数据，因此只能使用专用 `supermarket_test`，绝不能指向开发库或其他已有数据库。测试本身还会校验 JDBC URL 的 schema 名；不是 `supermarket_test` 时会立即失败。

先创建空测试库：

```powershell
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS supermarket_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
$env:TEST_DB_URL = "jdbc:mysql://localhost:3306/supermarket_test?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$env:TEST_DB_USERNAME = "root"
$env:TEST_DB_PASSWORD = Read-Host "MySQL test password"
```

显式启用 `mysql` 标签：

```powershell
mvn -DexcludedGroups= -Dgroups=mysql -Dtest=PricingScenariosAcceptanceTest test
```

测试在执行场景前加载 `db/schema.sql` 和 `db/data.sql`，断言 A=`55.00`、B=`75.00`、C=`67.20`、D=`102.00`，并验证订单从 `UNPAID` 转为 `COMPLETED`。

## 6. 安全与本地配置

- 当前 `application.yml` 按本地演示要求保存了明文数据库与管理员凭据，不应将当前仓库直接发布为公开仓库。
- 若后续需要部署到其他电脑或对外提供服务，应恢复为环境变量或使用不提交到 Git 的 `application-local.yml`。
- `data.sql` 是可重复执行的演示基准重置脚本：会恢复三种水果的名称、价格和启用状态，并补齐默认促销；请勿对需要保留人工改价的生产数据重复执行。
- 全新本地库可重新执行 `schema.sql` 和 `data.sql`。旧版库请先备份，再仅执行一次 `db/migration-add-promotion-code.sql`；迁移脚本按主键生成稳定占位 code，种子规则只按 code 识别，禁止依赖可修改的 name。
- 新建促销时 `code` 可省略；系统会生成 `PROMO_` 加 UUID 的稳定标识。显式填写时会转为大写并把分隔符规范化为下划线，长度不能超过 64，且必须至少包含字母或数字。
- 正式下单会在事务中重新计价，客户端不能提交单价或总价。
- 当前项目不包含库存、支付网关、退款、配送、顾客账号或多管理员管理。
