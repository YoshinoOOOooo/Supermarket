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
