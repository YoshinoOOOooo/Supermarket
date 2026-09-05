# Supermarket Project Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复项目审计发现的凭据泄露、失效验收测试、并发覆盖、响应不一致、API 无边界、N+1 查询、异常不可观测和文档失真问题。

**Architecture:** 保持现有 Spring Boot 单体和 controller/service/serviceimpl/mapper 分层。使用环境变量管理凭据，使用 MyBatis-Plus 乐观锁保护可修改聚合，使用测试专用 SQL 隔离验收数据库，并通过批量查询消除订单列表 N+1。

**Tech Stack:** Java 8、Spring Boot 2.7.18、MyBatis-Plus 3.5.5、MySQL 8、JUnit 5、Mockito、MockMvc。

**Spec:** `docs/superpowers/specs/2026-09-06-project-hardening-design.md`

## Global Constraints

- 不引入 Redis、JWT、Flyway 或新的运行时服务。
- 生产数据库初始化入口只保留 `src/main/resources/db/init.sql`。
- 不重写 Git 历史、不修改本机 MySQL 用户、不自动推送 GitHub。
- 所有业务修改执行 RED → GREEN → REFACTOR，并按任务独立提交。
- 不把真实密码写入源码、测试、README、命令示例或提交信息。

---

### Task 1: Environment-only credentials and repository cleanup

**Files:**
- Modify: `src/main/resources/application.yml`
- Modify: `src/test/java/com/supermarket/config/SecurityConfigTest.java`
- Modify: `src/test/java/com/supermarket/config/AdminOpenApiContractTest.java`
- Delete: `.idea/.gitignore`

**Interfaces:**
- Consumes: environment variables supplied by the local shell.
- Produces: `${DB_URL:...}`, `${DB_USERNAME:root}`, `${DB_PASSWORD}`, `${ADMIN_USERNAME:admin}`, `${ADMIN_PASSWORD}` configuration contract.

- [ ] **Step 1: Write failing configuration tests**

Add tests that create `SecurityConfig.userDetailsService` with blank username/password and assert `IllegalStateException`. Add explicit non-secret datasource/admin test properties to every Spring context test that loads `application.yml`, so tests do not depend on developer credentials.

```java
@Test
void blankAdministratorPasswordStopsStartup() {
    assertThrows(IllegalStateException.class,
            () -> config.userDetailsService("admin", " ", config.passwordEncoder()));
}
```

- [ ] **Step 2: Run the focused tests and confirm RED where configuration is still permissive or context properties are missing**

Run:

```powershell
mvn -Dtest=SecurityConfigTest,AdminOpenApiContractTest,SupermarketApplicationTest test
```

- [ ] **Step 3: Replace hard-coded credentials**

Use this configuration shape:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/supermarket?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD}
app:
  security:
    admin:
      username: ${ADMIN_USERNAME:admin}
      password: ${ADMIN_PASSWORD}
```

Remove `.idea/.gitignore` with `git rm`; root `.gitignore` already ignores the whole IDE directory. Do not delete `docs/` or the active worktree.

- [ ] **Step 4: Run focused tests and verify GREEN**

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/application.yml src/test/java/com/supermarket/config/SecurityConfigTest.java src/test/java/com/supermarket/config/AdminOpenApiContractTest.java src/test/java/com/supermarket/SupermarketApplicationTest.java
git commit -m "security: load credentials from environment"
```

### Task 2: Restore isolated MySQL acceptance-test fixtures

**Files:**
- Create: `src/test/resources/db/test-schema.sql`
- Create: `src/test/resources/db/test-data.sql`
- Modify: `src/test/java/com/supermarket/acceptance/PricingScenariosAcceptanceTest.java`

**Interfaces:**
- Produces: test-only schema/data scripts that never contain `CREATE DATABASE` or `USE`.

- [ ] **Step 1: Change the acceptance test to the new resource names and run it to confirm RED**

```java
new ResourceDatabasePopulator(new ClassPathResource("db/test-schema.sql")).execute(dataSource);
// destructive cleanup remains after the supermarket_test URL guard
new ResourceDatabasePopulator(new ClassPathResource("db/test-data.sql")).execute(dataSource);
```

Run with the dedicated test environment variables:

```powershell
mvn -DexcludedGroups= -Dgroups=mysql -Dtest=PricingScenariosAcceptanceTest test
```

Expected before scripts exist: failure for missing classpath resource.

- [ ] **Step 2: Create test-only schema and seed scripts**

`test-schema.sql` creates the five tables in dependency order and includes `version INT NOT NULL DEFAULT 0` on `promotion` and `customer_order`. `test-data.sql` inserts APPLE/STRAWBERRY/MANGO and the two default promotions. Neither script selects or creates a database.

- [ ] **Step 3: Add an acceptance assertion that clearing `end_time` persists as SQL NULL**

Issue an authenticated promotion PUT with `"endTime":null`, then assert with `JdbcTemplate`:

```java
assertNull(jdbcTemplate.queryForObject(
        "SELECT end_time FROM promotion WHERE id = ?", LocalDateTime.class, promotionId));
```

- [ ] **Step 4: Run acceptance test when credentials are available; otherwise record it as not run, never as passed**

- [ ] **Step 5: Commit**

```bash
git add src/test/resources/db src/test/java/com/supermarket/acceptance/PricingScenariosAcceptanceTest.java
git commit -m "test: isolate mysql acceptance fixtures"
```

### Task 3: MyBatis-Plus pagination and optimistic locking

**Files:**
- Create: `src/main/java/com/supermarket/config/MybatisPlusConfig.java`
- Modify: `src/main/java/com/supermarket/entity/Promotion.java`
- Modify: `src/main/java/com/supermarket/entity/CustomerOrder.java`
- Modify: `src/main/resources/db/init.sql`
- Modify: `src/test/java/com/supermarket/entity/DomainModelTest.java`
- Create: `src/test/java/com/supermarket/config/MybatisPlusConfigTest.java`

**Interfaces:**
- Produces: `MybatisPlusInterceptor` bean with `OptimisticLockerInnerInterceptor` and `PaginationInnerInterceptor(DbType.MYSQL)`; entity `Integer version` fields.

- [ ] **Step 1: Write failing tests for version metadata and interceptor bean**

```java
@Test
void mutableAggregatesDeclareVersionFields() {
    assertTrue(Promotion.class.getDeclaredField("version").isAnnotationPresent(Version.class));
    assertTrue(CustomerOrder.class.getDeclaredField("version").isAnnotationPresent(Version.class));
}
```

The config test instantiates `MybatisPlusConfig`, obtains the interceptor, and verifies both inner interceptors and their order: optimistic lock first, pagination second.

- [ ] **Step 2: Run tests and verify RED because fields/config do not exist**

- [ ] **Step 3: Add configuration and fields**

```java
@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

Add `@Version private Integer version;` to both entities and `version INT NOT NULL DEFAULT 0` to both tables in `init.sql`.

- [ ] **Step 4: Run focused tests and verify GREEN**

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/supermarket/config/MybatisPlusConfig.java src/main/java/com/supermarket/entity src/main/resources/db/init.sql src/test/java/com/supermarket/config/MybatisPlusConfigTest.java src/test/java/com/supermarket/entity/DomainModelTest.java
git commit -m "feat: add optimistic locking and pagination"
```

### Task 4: Consistent write results and conflict handling

**Files:**
- Modify: `src/main/java/com/supermarket/serviceimpl/ProductServiceImpl.java`
- Modify: `src/main/java/com/supermarket/serviceimpl/PromotionServiceImpl.java`
- Modify: `src/main/java/com/supermarket/serviceimpl/OrderServiceImpl.java`
- Modify: corresponding three service implementation tests.

**Interfaces:**
- Produces: database-reloaded VO responses and `RESOURCE_CONFLICT`/`INVALID_ORDER_STATE` on zero affected rows.

- [ ] **Step 1: Write failing tests**

Cover product create/update/enable returning the second `selectById` result, promotion create returning reloaded timestamps, promotion update returning conflict when `updateById` returns 0, and concurrent order update including the loaded version.

```java
when(mapper.insert(any())).thenReturn(1);
when(mapper.selectById(7L)).thenReturn(persistedWithTimestamps);
assertEquals(persistedWithTimestamps.getUpdatedAt(), service.update(7L, request).getUpdatedAt());
```

- [ ] **Step 2: Run focused tests and verify RED**

- [ ] **Step 3: Implement minimal write-count checks and reloads**

After successful insert/update, call `required(id)` before mapping. For order updates, carry the loaded `version` into the update entity; for promotion `updateById`, treat return value 0 as concurrent modification. Do not change public endpoint paths or DTO JSON names.

- [ ] **Step 4: Run focused tests and verify GREEN**

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/supermarket/serviceimpl src/test/java/com/supermarket/serviceimpl
git commit -m "fix: keep write responses and concurrent updates consistent"
```

### Task 5: Bound API inputs and document local date-time format

**Files:**
- Modify: `CheckoutRequest.java`, `CheckoutItemRequest.java`, `PromotionCreateRequest.java`, `PromotionUpdateRequest.java`
- Modify: `OrderServiceImpl.java`
- Modify: controller/OpenAPI/service tests.

**Interfaces:**
- Produces: maximum 100 items, maximum 100000斤 per item, maximum order page size 100, and OpenAPI local-time examples without `Z`.

- [ ] **Step 1: Add failing MockMvc and OpenAPI tests**

Assert 101 items and quantity 100001 return 400; admin order `size=101` returns 400; OpenAPI promotion schemas expose example `2026-09-06T09:30:00`.

- [ ] **Step 2: Run focused tests and verify RED**

- [ ] **Step 3: Add Bean Validation and service limits**

```java
@NotEmpty
@Size(max = 100)
private List<@Valid CheckoutItemRequest> items;

@Min(0)
@Max(100000)
private Integer quantity;
```

Add `@Schema(type="string", format="date-time", example="2026-09-06T09:30:00", nullable=true)` to promotion time fields. Reject page sizes above 100 in `OrderServiceImpl.list`.

- [ ] **Step 4: Run focused tests and verify GREEN**

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/supermarket/dto src/main/java/com/supermarket/serviceimpl/OrderServiceImpl.java src/test/java
git commit -m "fix: enforce api request limits"
```

### Task 6: Remove order-list N+1 queries

**Files:**
- Modify: `src/main/java/com/supermarket/serviceimpl/OrderServiceImpl.java`
- Modify: `src/test/java/com/supermarket/serviceimpl/OrderServiceImplTest.java`

**Interfaces:**
- Produces: private `Map<Long,List<OrderItem>> snapshotsByOrderIds(List<Long> orderIds)` using one `IN` query.

- [ ] **Step 1: Write a failing test with two orders**

Assert the returned orders each receive their own sorted items and verify `itemMapper.selectList` is invoked exactly once for the page.

- [ ] **Step 2: Run focused test and verify RED because current code queries once per order**

- [ ] **Step 3: Implement batch load and grouping**

Collect page order IDs, return an empty map for an empty page, query with `.in(OrderItem::getOrderId, orderIds).orderByAsc(OrderItem::getId)`, group in insertion order, and use empty lists for orders without items.

- [ ] **Step 4: Run focused and full order tests**

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/supermarket/serviceimpl/OrderServiceImpl.java src/test/java/com/supermarket/serviceimpl/OrderServiceImplTest.java
git commit -m "perf: batch load paged order items"
```

### Task 7: Observable failures and consistent duplicate-code errors

**Files:**
- Modify: `GlobalExceptionHandler.java`
- Modify: `ProductServiceImpl.java`
- Modify: their tests.

**Interfaces:**
- Produces: SLF4J error logging for unexpected exceptions and HTTP 409 `RESOURCE_CONFLICT` for every duplicate product code path.

- [ ] **Step 1: Write failing tests**

Assert the pre-check duplicate path returns `RESOURCE_CONFLICT`; attach a test Logback appender, invoke `handleUnexpected(new RuntimeException("boom"))`, and assert one ERROR event contains the exception without exposing it in `ApiError.message`.

- [ ] **Step 2: Run tests and verify RED**

- [ ] **Step 3: Implement minimal changes**

Use `LoggerFactory.getLogger(GlobalExceptionHandler.class)` and `log.error("Unhandled request exception", exception)`. Change the duplicate pre-check to `ErrorCode.RESOURCE_CONFLICT`.

- [ ] **Step 4: Run focused tests and verify GREEN**

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/supermarket/exception/GlobalExceptionHandler.java src/main/java/com/supermarket/serviceimpl/ProductServiceImpl.java src/test/java
git commit -m "fix: make failures observable and consistent"
```

### Task 8: Documentation, final verification, and generated-directory cleanup

**Files:**
- Modify: `README.md`
- Delete locally after verification: `.idea/`, `.superpowers/`, `target/`

- [ ] **Step 1: Update README to match the repository**

Remove all production references to deleted `schema.sql`, `data.sql`, and migration SQL. Document the five environment variables, Git Bash/PowerShell examples without real secrets, the test-only scripts, request limits, local time format, and this existing-database upgrade:

```sql
ALTER TABLE promotion ADD COLUMN version INT NOT NULL DEFAULT 0;
ALTER TABLE customer_order ADD COLUMN version INT NOT NULL DEFAULT 0;
```

Warn that the commands are one-time operations and should first be checked with `SHOW COLUMNS ... LIKE 'version'`.

- [ ] **Step 2: Run documentation consistency searches**

```powershell
rg -n "schema\.sql|data\.sql|migration-add-promotion-code|20010528ok" README.md src/main src/test
```

Expected: no real credential and no stale production script reference; test fixture names are allowed.

- [ ] **Step 3: Run complete verification**

```powershell
mvn clean test
git diff --check
git status --short
```

Expected: all default tests pass; only intentional files differ. Run MySQL acceptance tests separately only when `TEST_DB_*` variables point to `supermarket_test`.

- [ ] **Step 4: Commit documentation**

```bash
git add README.md
git commit -m "docs: align setup and verification guidance"
```

- [ ] **Step 5: Remove ignored generated directories after resolving exact paths**

Verify all three paths are inside the active worktree, then remove `.idea`, `.superpowers`, and `target`. These are ignored/generated and recoverable (`.idea` from IDE, `target` from Maven); do not remove `.git`, `.worktrees`, `src`, or `docs`.

- [ ] **Step 6: Report remaining external actions**

Report that the user must rotate previously exposed credentials, optionally clean Git history in a separate explicitly authorized operation, set environment variables, run the existing-database ALTER statements, and push with `git push origin HEAD:main`.
