# Supermarket Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a locally deployed REST backend that manages products and promotions, calculates the four required fruit-pricing scenarios, and creates lifecycle-controlled orders.

**Architecture:** A single Spring Boot module uses controllers, service interfaces, sibling `serviceimpl` implementations, MyBatis-Plus mappers, and MySQL. Pricing is isolated behind ordered `PricingRule` implementations; checkout is stateless, while formal order creation recalculates and persists immutable price snapshots in one transaction.

**Tech Stack:** Java 8, Spring Boot 2.7.x, Maven, Spring Web, Spring Security HTTP Basic, MySQL 8.x, MyBatis-Plus, Bean Validation, springdoc-openapi, JUnit 5, Mockito, MockMvc, BigDecimal.

**Spec:** `docs/superpowers/specs/2026-09-05-supermarket-backend-design.md`

## Global Constraints

- Java source and target compatibility must remain Java 8.
- Use one Maven module and root package `com.supermarket`.
- Use `BigDecimal` for all money and discount calculations; final monetary values use scale 2 and `RoundingMode.HALF_UP`.
- Quantities are non-negative integers; a formal order must contain at least one positive quantity.
- Product discounts execute before the single order threshold reduction.
- Controllers never access mappers or return persistence entities.
- Service interfaces live in `service`; implementations live in sibling `serviceimpl`.
- `/api/admin/**` uses HTTP Basic; checkout, order creation, order lookup, Swagger, and health endpoints are public.
- Database and administrator secrets come from environment variables; no real credentials are committed.
- The current directory is not a Git repository. Commit steps may run only after the user explicitly authorizes `git init`; otherwise record them as skipped without changing repository state.

---

## File Map

- `pom.xml`: Spring Boot build and dependencies.
- `src/main/java/com/supermarket/SupermarketApplication.java`: application entry point.
- `src/main/java/com/supermarket/config/{SecurityConfig,OpenApiConfig}.java`: authentication and API documentation.
- `src/main/java/com/supermarket/entity/{Product,Promotion,CustomerOrder,OrderItem}.java`: persistence models.
- `src/main/java/com/supermarket/enums/{PromotionType,OrderStatus}.java`: closed business vocabularies.
- `src/main/java/com/supermarket/mapper/{ProductMapper,PromotionMapper,CustomerOrderMapper,OrderItemMapper}.java`: persistence access.
- `src/main/java/com/supermarket/dto/...`: validated product, promotion, checkout, and order requests.
- `src/main/java/com/supermarket/vo/...`: stable API views.
- `src/main/java/com/supermarket/pricing/...`: money calculator and ordered pricing rules.
- `src/main/java/com/supermarket/service/...`: business interfaces.
- `src/main/java/com/supermarket/serviceimpl/...`: business implementations and transaction boundaries.
- `src/main/java/com/supermarket/controller/...`: public and administrator REST endpoints.
- `src/main/java/com/supermarket/exception/...`: typed failures and uniform error responses.
- `src/main/resources/application.yml`: safe environment-backed defaults.
- `src/main/resources/db/schema.sql`: MySQL schema.
- `src/main/resources/db/data.sql`: required default products and promotions.
- `src/test/java/com/supermarket/...`: pricing, service, controller, and security tests.
- `README.md`: setup, test, launch, Swagger, and demonstration guide.

### Task 1: Bootstrap the Spring Boot Application

**Files:**
- Modify: `pom.xml`
- Delete: `src/main/java/org/example/Main.java`
- Create: `src/main/java/com/supermarket/SupermarketApplication.java`
- Create: `src/main/resources/application.yml`
- Test: `src/test/java/com/supermarket/SupermarketApplicationTest.java`

**Interfaces:**
- Consumes: None.
- Produces: Spring application context and environment keys `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`.

- [ ] **Step 1: Replace the generated POM with Spring Boot 2.7.x dependencies**

Use `spring-boot-starter-parent:2.7.18`, Java 8, and dependencies for web, validation, security, actuator, JDBC, MySQL runtime, `mybatis-plus-boot-starter:3.5.5`, `springdoc-openapi-ui:1.7.0`, and `spring-boot-starter-test`. Configure `spring-boot-maven-plugin`. Configure Surefire with `${excludedGroups}` and set that property to `mysql` by default so local-database acceptance tests are opt-in.

- [ ] **Step 2: Write the failing context test**

```java
@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class SupermarketApplicationTest {
    @Test void contextLoads() {}
}
```

- [ ] **Step 3: Run the test and verify the missing application class failure**

Run: `mvn -Dtest=SupermarketApplicationTest test`

Expected: FAIL because `com.supermarket.SupermarketApplication` does not exist.

- [ ] **Step 4: Create the application entry point and safe configuration**

```java
@SpringBootApplication
@MapperScan("com.supermarket.mapper")
public class SupermarketApplication {
    public static void main(String[] args) {
        SpringApplication.run(SupermarketApplication.class, args);
    }
}
```

Configure datasource values as `${DB_URL:jdbc:mysql://localhost:3306/supermarket?...}`, `${DB_USERNAME:root}`, `${DB_PASSWORD:}`, and administrator values as `${ADMIN_USERNAME:admin}` and `${ADMIN_PASSWORD:change-me}`. Disable automatic SQL initialization; scripts are executed explicitly.

- [ ] **Step 5: Run the context test**

Run: `mvn -Dtest=SupermarketApplicationTest test`

Expected: PASS.

- [ ] **Step 6: Commit if Git has been initialized with authorization**

```bash
git add pom.xml src/main src/test
git commit -m "build: bootstrap Spring Boot application"
```

### Task 2: Define Schema, Entities, Enums, and Mappers

**Files:**
- Create: `src/main/resources/db/schema.sql`
- Create: `src/main/resources/db/data.sql`
- Create: `src/main/java/com/supermarket/enums/PromotionType.java`
- Create: `src/main/java/com/supermarket/enums/OrderStatus.java`
- Create: `src/main/java/com/supermarket/entity/{Product,Promotion,CustomerOrder,OrderItem}.java`
- Create: `src/main/java/com/supermarket/mapper/{ProductMapper,PromotionMapper,CustomerOrderMapper,OrderItemMapper}.java`
- Test: `src/test/java/com/supermarket/entity/DomainModelTest.java`

**Interfaces:**
- Consumes: MyBatis-Plus `BaseMapper<T>`.
- Produces: `PromotionType.PRODUCT_DISCOUNT`, `PromotionType.ORDER_THRESHOLD_REDUCTION`; `OrderStatus.UNPAID`, `COMPLETED`, `CANCELLED`; four mapper interfaces.

- [ ] **Step 1: Write failing enum and model contract tests**

```java
class DomainModelTest {
    @Test void exposesRequiredOrderStates() {
        assertArrayEquals(new OrderStatus[]{UNPAID, COMPLETED, CANCELLED}, OrderStatus.values());
    }

    @Test void productCarriesMoneyAsBigDecimal() throws Exception {
        assertEquals(BigDecimal.class, Product.class.getDeclaredField("unitPrice").getType());
    }
}
```

- [ ] **Step 2: Run the test and verify compilation fails**

Run: `mvn -Dtest=DomainModelTest test`

Expected: FAIL because enums and entities do not exist.

- [ ] **Step 3: Implement enums, entities, and mappers**

Map snake_case tables with MyBatis-Plus annotations. Use `Long` IDs, `LocalDateTime` timestamps, `Integer quantity`, and `BigDecimal` money fields. Each mapper is exactly:

```java
public interface ProductMapper extends BaseMapper<Product> {}
```

Repeat the same typed contract for the other three entities.

- [ ] **Step 4: Create idempotent MySQL scripts**

`schema.sql` creates `product`, `promotion`, `customer_order`, and `order_item`, unique keys for product code and order number, foreign keys for promotion/product and order item/order, and indexes for enabled promotions and order status. `data.sql` inserts `APPLE/8.00`, `STRAWBERRY/13.00`, `MANGO/20.00`, strawberry rate `0.80`, and threshold `100.00` reduction `10.00` without duplicating existing codes/rules.

- [ ] **Step 5: Run the model test and compile**

Run: `mvn -Dtest=DomainModelTest test`

Expected: PASS.

- [ ] **Step 6: Commit if authorized**

```bash
git add src/main/resources/db src/main/java/com/supermarket/entity src/main/java/com/supermarket/enums src/main/java/com/supermarket/mapper src/test/java/com/supermarket/entity
git commit -m "feat: add supermarket persistence model"
```

### Task 3: Implement the Pricing Domain Test-First

**Files:**
- Create: `src/main/java/com/supermarket/pricing/{PricingItem,PricingContext,PricingResult,PricingRule,ProductDiscountRule,OrderThresholdReductionRule,PricingCalculator}.java`
- Test: `src/test/java/com/supermarket/pricing/PricingCalculatorTest.java`

**Interfaces:**
- Consumes: product code, quantity, unit price, applicable product discount rates, optional threshold and reduction.
- Produces: `PricingResult PricingCalculator.calculate(List<PricingItem> items, List<PricingRule> rules)` and `void PricingRule.apply(PricingContext context)`.

- [ ] **Step 1: Write failing tests for scenarios A and B**

```java
@Test void calculatesApplesAndStrawberries() {
    assertMoney("55.00", calculate(items(item("APPLE", 2, "8.00"), item("STRAWBERRY", 3, "13.00"))));
}

@Test void includesMango() {
    assertMoney("75.00", calculate(items(item("APPLE", 2, "8.00"), item("STRAWBERRY", 3, "13.00"), item("MANGO", 1, "20.00"))));
}
```

- [ ] **Step 2: Run and verify the pricing types are missing**

Run: `mvn -Dtest=PricingCalculatorTest test`

Expected: FAIL at compilation.

- [ ] **Step 3: Implement base-price calculation**

`PricingItem` validates non-null code/price and quantity >= 0. `PricingContext` owns mutable per-line discount totals internally. `PricingResult` exposes original amount, discount amount, payable amount, and immutable line results. Normalize returned money to scale 2 with `HALF_UP`.

- [ ] **Step 4: Run scenarios A and B**

Run: `mvn -Dtest=PricingCalculatorTest test`

Expected: PASS for A and B.

- [ ] **Step 5: Add failing tests for scenarios C and D and rule order**

```java
@Test void discountsStrawberriesToEightyPercent() {
    assertMoney("67.20", calculateWithStrawberryDiscount(twoThreeOneItems()));
}

@Test void appliesThresholdOnceAfterProductDiscount() {
    assertMoney("102.00", calculateWithAllPromotions(items(item("APPLE", 5, "8.00"), item("STRAWBERRY", 5, "13.00"), item("MANGO", 1, "20.00"))));
}
```

Also test exactly `100.00`, `99.99`, all-zero result, and a negative quantity exception.

- [ ] **Step 6: Implement ordered pricing rules**

`ProductDiscountRule` receives `Map<String, BigDecimal> rates` and order `100`; `OrderThresholdReductionRule` receives threshold/reduction and order `200`. `PricingCalculator` sorts by order, applies each rule exactly once, and rejects a payable result below zero.

- [ ] **Step 7: Run the complete pricing suite**

Run: `mvn -Dtest=PricingCalculatorTest test`

Expected: PASS for A-D and every boundary case.

- [ ] **Step 8: Commit if authorized**

```bash
git add src/main/java/com/supermarket/pricing src/test/java/com/supermarket/pricing
git commit -m "feat: implement composable fruit pricing rules"
```

### Task 4: Add Uniform Errors and Request/Response Contracts

**Files:**
- Create: `src/main/java/com/supermarket/exception/{ErrorCode,BusinessException,ApiError,GlobalExceptionHandler}.java`
- Create: `src/main/java/com/supermarket/dto/{CheckoutItemRequest,CheckoutRequest,ProductCreateRequest,ProductUpdateRequest,PromotionCreateRequest,PromotionUpdateRequest}.java`
- Create: `src/main/java/com/supermarket/vo/{CheckoutItemView,CheckoutResultView,ProductView,PromotionView,OrderItemView,OrderView}.java`
- Test: `src/test/java/com/supermarket/exception/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Consumes: Bean Validation failures and `BusinessException(ErrorCode, message)`.
- Produces: JSON errors `{code,message,timestamp}` and validated DTOs used by all controllers.

- [ ] **Step 1: Write a failing standalone MockMvc validation test**

Post a checkout item with `quantity: -1` to a test controller and assert status 400, `$.code == "INVALID_REQUEST"`, and a non-empty timestamp.

- [ ] **Step 2: Run the test and verify missing handler/contracts failure**

Run: `mvn -Dtest=GlobalExceptionHandlerTest test`

Expected: FAIL at compilation.

- [ ] **Step 3: Implement exact validation contracts**

Use `@NotEmpty List<@Valid CheckoutItemRequest> items`, `@NotBlank String productCode`, and `@NotNull @Min(0) Integer quantity`. Product prices use `@DecimalMin("0.00")`; promotion request validation combines annotations with service-level type-specific validation.

- [ ] **Step 4: Implement errors and immutable views**

Define error codes `INVALID_REQUEST`, `PRODUCT_NOT_FOUND`, `PRODUCT_DISABLED`, `PROMOTION_CONFLICT`, `ORDER_NOT_FOUND`, `INVALID_ORDER_STATE`, and `INTERNAL_ERROR`. Map them to 400/404/409/500 as specified; Spring Security owns 401 responses.

- [ ] **Step 5: Run error tests**

Run: `mvn -Dtest=GlobalExceptionHandlerTest test`

Expected: PASS.

- [ ] **Step 6: Commit if authorized**

```bash
git add src/main/java/com/supermarket/dto src/main/java/com/supermarket/vo src/main/java/com/supermarket/exception src/test/java/com/supermarket/exception
git commit -m "feat: add validated API contracts and error handling"
```

### Task 5: Implement Product and Promotion Administration

**Files:**
- Create: `src/main/java/com/supermarket/service/{ProductService,PromotionService}.java`
- Create: `src/main/java/com/supermarket/serviceimpl/{ProductServiceImpl,PromotionServiceImpl}.java`
- Create: `src/main/java/com/supermarket/controller/admin/{ProductAdminController,PromotionAdminController}.java`
- Test: `src/test/java/com/supermarket/serviceimpl/{ProductServiceImplTest,PromotionServiceImplTest}.java`
- Test: `src/test/java/com/supermarket/controller/admin/AdminCatalogControllerTest.java`

**Interfaces:**
- Produces: `ProductView create/update/setEnabled/find/list`; `PromotionView create/update/setEnabled/find/list`; REST routes under `/api/admin/products` and `/api/admin/promotions`.

- [ ] **Step 1: Write failing product service tests**

Verify unique normalized product code, mutable name/price, soft disable, missing product error, and no physical delete call. Mock `ProductMapper` and capture saved entities.

- [ ] **Step 2: Run and verify failure**

Run: `mvn -Dtest=ProductServiceImplTest test`

Expected: FAIL because service types do not exist.

- [ ] **Step 3: Implement product service and admin controller**

Expose create, get, list, update, and `PATCH /{id}/enabled` endpoints. Normalize code with `trim().toUpperCase(Locale.ROOT)` and inject `ProductService`, never its implementation.

- [ ] **Step 4: Run product tests**

Run: `mvn -Dtest=ProductServiceImplTest,AdminCatalogControllerTest test`

Expected: PASS for product cases.

- [ ] **Step 5: Write failing promotion tests**

Test type-specific required fields, rate range `(0,1]`, reduction `(0,threshold]`, one active discount per product/time range, and one active threshold rule/time range.

- [ ] **Step 6: Implement promotion service and controller**

Use mapper overlap queries built with `LambdaQueryWrapper`; validate `startTime < endTime` when both exist. Re-run conflict validation when a rule is created, updated, or enabled.

- [ ] **Step 7: Run catalog and promotion tests**

Run: `mvn -Dtest=ProductServiceImplTest,PromotionServiceImplTest,AdminCatalogControllerTest test`

Expected: PASS.

- [ ] **Step 8: Commit if authorized**

```bash
git add src/main/java/com/supermarket/service src/main/java/com/supermarket/serviceimpl src/main/java/com/supermarket/controller/admin src/test/java/com/supermarket/serviceimpl src/test/java/com/supermarket/controller/admin
git commit -m "feat: add product and promotion administration"
```

### Task 6: Implement Checkout and Public Calculation API

**Files:**
- Create: `src/main/java/com/supermarket/service/CheckoutService.java`
- Create: `src/main/java/com/supermarket/serviceimpl/CheckoutServiceImpl.java`
- Create: `src/main/java/com/supermarket/controller/CheckoutController.java`
- Test: `src/test/java/com/supermarket/serviceimpl/CheckoutServiceImplTest.java`
- Test: `src/test/java/com/supermarket/controller/CheckoutControllerTest.java`

**Interfaces:**
- Consumes: `CheckoutRequest`, product and promotion mappers, `PricingCalculator`.
- Produces: `CheckoutResultView CheckoutService.calculate(CheckoutRequest request)` and `POST /api/checkout/calculate`.

- [ ] **Step 1: Write failing service tests**

Verify one batch product query, missing/disabled product errors, duplicate request code rejection, all-zero `0.00`, selection of currently active promotions, and A-D outputs using mapper mocks.

- [ ] **Step 2: Run and verify failure**

Run: `mvn -Dtest=CheckoutServiceImplTest test`

Expected: FAIL because checkout service does not exist.

- [ ] **Step 3: Implement checkout orchestration**

Normalize and reject duplicate codes, batch load products with `in(Product::getCode, codes)`, compare loaded codes to requested codes, build pricing rules from active promotions, calculate, and map to immutable views. Do not write through any mapper.

- [ ] **Step 4: Run service tests**

Run: `mvn -Dtest=CheckoutServiceImplTest test`

Expected: PASS.

- [ ] **Step 5: Write controller tests and implement endpoint**

Use `@WebMvcTest(CheckoutController.class)` and mock `CheckoutService`. Assert valid JSON returns 200 and money strings with two decimals; invalid quantity returns 400.

- [ ] **Step 6: Run checkout tests**

Run: `mvn -Dtest=CheckoutServiceImplTest,CheckoutControllerTest test`

Expected: PASS.

- [ ] **Step 7: Commit if authorized**

```bash
git add src/main/java/com/supermarket/service/CheckoutService.java src/main/java/com/supermarket/serviceimpl/CheckoutServiceImpl.java src/main/java/com/supermarket/controller/CheckoutController.java src/test/java/com/supermarket/serviceimpl/CheckoutServiceImplTest.java src/test/java/com/supermarket/controller/CheckoutControllerTest.java
git commit -m "feat: add public checkout calculation"
```

### Task 7: Implement Transactional Orders and State Machine

**Files:**
- Create: `src/main/java/com/supermarket/service/OrderService.java`
- Create: `src/main/java/com/supermarket/serviceimpl/OrderServiceImpl.java`
- Create: `src/main/java/com/supermarket/controller/{OrderController}.java`
- Create: `src/main/java/com/supermarket/controller/admin/OrderAdminController.java`
- Test: `src/test/java/com/supermarket/serviceimpl/OrderServiceImplTest.java`
- Test: `src/test/java/com/supermarket/controller/OrderControllerTest.java`

**Interfaces:**
- Produces: `OrderView create(CheckoutRequest)`, `OrderView findByOrderNo(UUID)`, `OrderView complete(UUID)`, `OrderView cancel(UUID)`, and paginated administrator listing.

- [ ] **Step 1: Write failing creation tests**

Mock current catalog and promotions, then assert `create` recalculates rather than accepting totals, assigns UUID and `UNPAID`, inserts order before items, saves snapshots, and is annotated `@Transactional`.

- [ ] **Step 2: Run and verify failure**

Run: `mvn -Dtest=OrderServiceImplTest test`

Expected: FAIL because order service does not exist.

- [ ] **Step 3: Implement order creation and lookup**

Reject all-zero formal orders. Reuse an internal pricing capability shared with checkout rather than calling a controller. Persist `originalAmount`, aggregate `discountAmount`, and `payableAmount`, then persist item snapshots in the same transaction.

- [ ] **Step 4: Add failing state-machine tests**

Cover `UNPAID -> COMPLETED`, `UNPAID -> CANCELLED`, idempotent repeat of the same target, rejection of `COMPLETED -> CANCELLED`, and rejection of `CANCELLED -> COMPLETED`.

- [ ] **Step 5: Implement guarded transitions**

Load by UUID, return `ORDER_NOT_FOUND` when absent, short-circuit equal target status, allow transitions only from `UNPAID`, update with an optimistic status condition (`WHERE id=? AND status='UNPAID'`), and return `INVALID_ORDER_STATE` on a concurrent conflict.

- [ ] **Step 6: Implement controllers and tests**

Public routes are `POST /api/orders` and `GET /api/orders/{orderNo}`. Administrator routes are `GET /api/admin/orders`, `POST /api/admin/orders/{orderNo}/complete`, and `/cancel`. Assert response states and status codes with MockMvc.

- [ ] **Step 7: Run order tests**

Run: `mvn -Dtest=OrderServiceImplTest,OrderControllerTest test`

Expected: PASS.

- [ ] **Step 8: Commit if authorized**

```bash
git add src/main/java/com/supermarket/service/OrderService.java src/main/java/com/supermarket/serviceimpl/OrderServiceImpl.java src/main/java/com/supermarket/controller src/test/java/com/supermarket/serviceimpl/OrderServiceImplTest.java src/test/java/com/supermarket/controller/OrderControllerTest.java
git commit -m "feat: add transactional orders and lifecycle"
```

### Task 8: Configure Security and OpenAPI

**Files:**
- Create: `src/main/java/com/supermarket/config/{SecurityConfig,OpenApiConfig}.java`
- Test: `src/test/java/com/supermarket/config/SecurityConfigTest.java`

**Interfaces:**
- Consumes: `app.security.admin.username` and `app.security.admin.password`.
- Produces: a `SecurityFilterChain`, in-memory administrator, password encoder, and OpenAPI HTTP Basic scheme.

- [ ] **Step 1: Write failing security tests**

With MockMvc, assert public checkout and order routes do not return 401, `/api/admin/products` without credentials returns 401, wrong credentials return 401, and configured administrator credentials pass authentication.

- [ ] **Step 2: Run and verify failure**

Run: `mvn -Dtest=SecurityConfigTest test`

Expected: FAIL because default Spring Security protects public routes.

- [ ] **Step 3: Implement Java 8-compatible Spring Security configuration**

Declare `SecurityFilterChain` without `WebSecurityConfigurerAdapter`; permit `/api/checkout/**`, `/api/orders/**`, `/v3/api-docs/**`, `/swagger-ui/**`, and `/actuator/health`; require role `ADMIN` for `/api/admin/**`; enable HTTP Basic; disable CSRF for this stateless local REST API; use `BCryptPasswordEncoder` and encode the configured password at startup.

- [ ] **Step 4: Add OpenAPI metadata and Basic scheme**

Document API title, version, public endpoints, administrator security scheme, and money field examples.

- [ ] **Step 5: Run security tests**

Run: `mvn -Dtest=SecurityConfigTest test`

Expected: PASS.

- [ ] **Step 6: Commit if authorized**

```bash
git add src/main/java/com/supermarket/config src/test/java/com/supermarket/config src/main/resources/application.yml
git commit -m "feat: secure administration and document API"
```

### Task 9: MySQL Verification, Documentation, and Final Acceptance

**Files:**
- Create: `src/test/java/com/supermarket/acceptance/PricingScenariosAcceptanceTest.java`
- Create: `src/test/resources/application-test.yml`
- Create: `README.md`
- Modify: `.gitignore`

**Interfaces:**
- Consumes: local MySQL test database variables and all public/admin APIs.
- Produces: repeatable setup and proof that scenarios A-D and lifecycle endpoints work.

- [ ] **Step 1: Add an opt-in MySQL acceptance test**

Use `@SpringBootTest`, `@AutoConfigureMockMvc`, `@ActiveProfiles("test")`, and JUnit tag `mysql`. Execute schema/data scripts before the class, then post scenario A-D requests and assert `55.00`, `75.00`, `67.20`, and `102.00`. Create an order and assert `UNPAID`, then authenticate as admin and assert completion yields `COMPLETED`.

- [ ] **Step 2: Run fast tests excluding local MySQL**

Run: `mvn test`

Expected: all unit, service, controller, and security tests PASS; the tagged MySQL acceptance test is excluded by default through Surefire configuration.

- [ ] **Step 3: Run the opt-in MySQL acceptance test**

After creating a disposable `supermarket_test` database and exporting test credentials, run:

`mvn -DexcludedGroups= -Dgroups=mysql -Dtest=PricingScenariosAcceptanceTest test`

Expected: PASS with all four exact totals and the order transition.

- [ ] **Step 4: Write README with exact local commands**

Document prerequisites, `mysql -u root -p < src/main/resources/db/schema.sql`, data initialization, PowerShell environment variable examples, `mvn test`, `mvn spring-boot:run`, Swagger URL, Basic credentials, and curl/PowerShell examples for product update, promotion enablement, A-D checkout, order creation, lookup, completion, and cancellation.

- [ ] **Step 5: Harden ignored files**

Ignore `target/`, `.idea/`, `*.iml`, `.env`, `application-local.yml`, logs, and OS files. Do not ignore SQL scripts, README, specs, or plans.

- [ ] **Step 6: Run final verification**

Run: `mvn clean test`

Expected: `BUILD SUCCESS` with no failed tests.

Run: `mvn -DskipTests package`

Expected: executable JAR created under `target/`.

- [ ] **Step 7: Commit if authorized**

```bash
git add README.md .gitignore src/test src/test/resources
git commit -m "test: verify supermarket workflows and document setup"
```

## Completion Gate

- All fast tests pass from a clean Maven build.
- Opt-in MySQL acceptance test passes against the disposable local database.
- Scenario totals are exactly A `55.00`, B `75.00`, C `67.20`, D `102.00`.
- Swagger exposes all public and administrator routes.
- Unauthenticated administrator requests return 401.
- Checkout performs no writes; formal order creation persists immutable snapshots.
- Only `UNPAID -> COMPLETED` and `UNPAID -> CANCELLED` transitions are permitted.
- No real credential or machine-local configuration is tracked.
