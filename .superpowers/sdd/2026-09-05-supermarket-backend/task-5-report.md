# Task 5 Report

## Status

Implemented product and promotion administration using the existing DTO, VO, entity, mapper, and error contracts. Service interfaces are in `service`, implementations are in sibling `serviceimpl`, and admin controllers are under `controller/admin`.

## Behavior

- Product codes are trimmed and normalized with `Locale.ROOT`, then checked for uniqueness.
- Product name and unit price are mutable; enabled changes use `updateById` and never physically delete.
- Promotion type-specific fields, numeric ranges, and time ordering are validated.
- Active promotion conflicts are queried with `LambdaQueryWrapper` on create, enabled update, and enable.
- Product discount conflicts are scoped to product and overlapping time; threshold-rule conflicts are scoped by type and overlapping time.
- Controllers expose create, get, list, update, and enabled-patch routes and depend only on service interfaces.

## TDD and Tests

Tests added:

- `ProductServiceImplTest`
- `PromotionServiceImplTest`
- `AdminCatalogControllerTest`

The prescribed Maven commands were invoked with full JDK and Maven paths. Both online and offline attempts stopped during POM resolution because `spring-boot-starter-parent:2.7.18` is absent from the local Maven cache and network access is denied. Therefore no tests compiled or executed in this environment; parent verification is required.

## Self-review

- `git diff --check` passed before commit.
- Controllers contain no mapper, entity, or implementation dependencies.
- Product implementation contains no delete operation.
- Tests cover normalized uniqueness, mutable fields, soft disable, missing products, promotion validation, conflict checks on create/update/enable, and admin routes.

## Concern

The existing `ErrorCode` contract has no promotion-not-found value, so missing promotions use `INVALID_REQUEST` rather than introducing an unrequested contract change.

## Parent Verification Evidence

Parent verification against commit `3c899b4` completed GREEN:

- `ProductServiceImplTest`: 6 tests
- `PromotionServiceImplTest`: 9 tests
- `AdminCatalogControllerTest`: 2 tests
- Total: 17 tests, 0 failures, 0 errors, 0 skipped
- Maven result: `BUILD SUCCESS`

## Remaining Concern

The compiler emitted an unchecked-operation note in `ProductServiceImplTest`. This does not affect the GREEN result, but the test contains a raw generic matcher that could be tightened in a future cleanup.

## Fix Round 1

Covering files:

- `src/test/java/com/supermarket/serviceimpl/ProductServiceImplTest.java`
- `src/test/java/com/supermarket/serviceimpl/PromotionServiceImplTest.java`
- `src/test/java/com/supermarket/controller/admin/AdminCatalogControllerTest.java`

Changes:

- Replaced the raw product wrapper matcher with a typed `LambdaQueryWrapper<Product>` captor and asserted that the uniqueness query targets normalized code `APPLE-01`.
- Asserted promotion conflict wrapper scope for enabled state, promotion type, product, update self-exclusion, and both time-overlap predicates.
- Added independent missing, zero, and negative promotion-value tests.
- Covered create, find, update, list, enabled patch, valid DTO binding, and invalid DTO rejection for both admin controllers.
- No production changes were required.

Focused command:

```powershell
$env:JAVA_HOME='C:\Users\a8923\.jdks\corretto-1.8.0_462'; & 'C:\Users\a8923\.m2\wrapper\dists\apache-maven-3.8.5-bin\5i5jha092a3i37g0paqnfr15e0\apache-maven-3.8.5\bin\mvn.cmd' '-Dtest=ProductServiceImplTest,PromotionServiceImplTest,AdminCatalogControllerTest' test
```

Exact result summary:

```text
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.868 s - in com.supermarket.controller.admin.AdminCatalogControllerTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.093 s - in com.supermarket.serviceimpl.ProductServiceImplTest
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.047 s - in com.supermarket.serviceimpl.PromotionServiceImplTest
[INFO] Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  3.309 s
[INFO] Finished at: 2026-09-05T03:29:13+08:00
```

The previous unchecked-operation compiler note is resolved; the focused compilation emitted no unchecked-operation warning.
