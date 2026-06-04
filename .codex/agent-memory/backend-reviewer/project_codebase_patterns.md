---
name: Lumie Backend Codebase Established Patterns
description: Conventions observed across all domain modules — use to distinguish intentional patterns from violations
type: project
---

# Established conventions (not violations)

- JPA annotations (`@Entity`, `@Table`, `@Column`) on domain entities are intentional and consistent across all modules. Do NOT flag as violation.
- Domain entity constructors are `private @Builder`, with public `static create(...)` factory methods — enforced everywhere.
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` on all entities — correct JPA pattern.
- `Page<T>` and `Pageable` from Spring Data appear in domain repository interfaces (e.g., ExamRepository, AssignmentRepository). Accepted convention; not flagged as violation.
- `@Getter @Builder` Lombok on entities — consistent. No `@Data` on entities.
- Post-B3/F4 cleanup (2026-05-20): `*ErrorCode` enums live in `domain/exception/` (billing, tenant, homepage) or `application/exception/` (auth). No `infrastructure/exception/` packages remain.
- Post-B2 cleanup: `adapter/internal/` package is gone in all 10 modules; canonical path is `adapter/in/internal/` for libs/internal-api impls.
- `CookieUtils` and `SessionMetadataExtractor` now live in `adapter/in/web/` (auth-svc) — utility helpers tightly coupled to HTTP layer, not domain. Acceptable.
- `JwtAuthenticationFilter` and `AuthenticatedUser` now live in `adapter/in/security/` (auth-svc). Correct.
- Internal cross-module calls go through `TenantService` / `AdminService` etc. (in `libs/internal-api`), implemented in each module's `adapter/in/internal/`. Consistent across all modules.
- Use cases: one interface per use case (ISP respected), multiple use cases implemented by a single `*CommandService` or `*QueryService` is codebase convention.
- `StatisticsCalculator` and `GradeCalculator` (exam-svc) and `SqlValidator` (ai-svc) are now in `application/service/` — they carry `@Service`/`@Component` and depend on Spring, so not domain-pure. Correct placement.

# Known violations/debts (updated 2026-05-20)

## Pre-existing (not introduced by this cleanup)
- `TenantCommandService` directly injects `BillingInternalAdapter` (concrete adapter) instead of a port — **CRITICAL**.
- `withinTenantContext` helper duplicated locally in multiple internal adapters — should be extracted to `libs/common`.
- `extractTenantId(slug)` duplicated in `OmrGradingJobProcessor`, `ResultCommandService`, `file-svc/FilePath`.
- `User.status` is `String` with string constants — should be `UserStatus` enum (now exists as VO, but entity field not yet migrated).
- `AdminCommandService` defines role hierarchy as `Set<String>` / `List<String>`.
- `StudentAnalyticsQueryService.getNormalizedScores` has N+1: per-result `findByExamId` inside stream.
- `getDashboardStatistics()` loads all exams + results via `findAll()` — O(N), no pagination.
- `"NO_ANSWER"` magic string in `ExamStatisticsQueryService`.

## Introduced or surfaced by this cleanup
- `AdminRepositoryCustom` interface (`domain/repository/`) imports `AdminFilterRequest` from `application/dto/request/` — domain→application layer violation. Port fragment contract should use a domain-native filter VO, not an application DTO.
- `AuthErrors.UserLoginIdAlreadyExistsException` and `AuthErrors.TenantCreationFailedException` extend raw `RuntimeException` rather than `BusinessException(ErrorCode)` — HARD_RULES violation. `GlobalExceptionHandler` has no handler for them; both fall through to the catch-all 500 handler, losing 409/422 semantics.
- `SqlValidator.validate()` compiles a new `Pattern` on every call per FORBIDDEN_KEYWORD (inside the loop at line 48). Patterns should be pre-compiled and stored as static finals.

**Why:** Architecture audit findings (2026-05-08) + naming-coherence cleanup review (2026-05-20).
**How to apply:** When reviewing or refactoring these modules, use this as a baseline rather than rediscovering the same issues.

# Exception consolidation refactor (2026-05-26) — design decisions and patterns

## What was done
- 32 typed exception classes collapsed to 3 Tier-1 + `BusinessException(ErrorCode, msg)` for everything else.
- `GlobalExceptionHandler` now extends `ResponseEntityExceptionHandler`, returns `ResponseEntity<ProblemDetail>` for all business exceptions.
- `BusinessException.contributeProperties(ProblemDetail)` hook added — default no-op; Tier-1 subclasses override to publish typed fields.
- `MetricType` promoted from `billing.domain.vo` to `libs/common/exception/MetricType` (cross-module usage).
- `QuotaExceededException` unified in `libs/common/exception/` — accepts any module's `ErrorCode` so the module prefix is preserved.

## Established conventions from this refactor
- Tier-1 typed exceptions are justified ONLY when typed fields are consumed by UI flows (upsell CTAs, retry UI). Otherwise, `BusinessException(ErrorCode, msg)` is the correct throw.
- `contributeProperties(ProblemDetail body)` is the canonical hook for Tier-1 exceptions to publish structured fields to RFC 7807 responses.
- `GlobalExceptionHandler.decorate()` sets `code`, `timestamp`, `tenantSlug` properties on every `ProblemDetail`. Any new handler method must call `decorate()`.
- Error code strings in `GlobalExceptionHandler` must reference `CommonErrorCode.X.getCode()` — no hardcoded strings (W2/W3 violations still unresolved as of 2026-05-26).

## Blockers found in this review (unresolved as of 2026-05-26)
- `PaymentFailedException` and `InvalidTenantStateException` import `org.springframework.http.ProblemDetail` inside their module's `domain/exception/` package — Spring HTTP type in domain layer. Must be moved to `application/exception/`.
- `BillingService.checkQuota(String tenantSlug, String metricType)` interface still uses `String` — callers pass `"MAX_ADMINS"` / `"MAX_STUDENTS"` magic strings. Must be typed to `MetricType`. Callers: `StaffCommandService:250`, `StudentCommandService:434`.
- `OmrGradingJobProcessor.Tx.saveSuccess` (line 182) catches bare `BusinessException` without error-code guard — any domain exception from `resultRepository.save()` is silently treated as "duplicate result". Needs guard or `DataIntegrityViolationException` catch instead.

# OmrGradingJob terminal-state refactor (2026-05-28)

## Resolved from prior blockers
- RESOLVED: `OmrGradingJobProcessor.Tx.saveSuccess` bare `BusinessException` catch — now guarded by `e.getErrorCode() != ExamErrorCode.DUPLICATE_RESULT` check (line 158). No longer a silent swallow.

## New debt items surfaced by this review
- `OmrGradingJobProcessor.extractFileName(String)` (line 95) is private and has zero callers — dead code left from `resultMap`/`appendResult` removal. Delete it.
- `OmrGradingJobProcessor` uses `import java.util.*;` wildcard (line 23). The only java.util type actually used is `Optional` and `List` (both in `Tx`). Replace with explicit imports.
- `ReportGenerationJobRepository` has `markCompletedIfAllProcessed` but no `markFailedIfAllProcessed` counterpart — missing terminal FAILED path, same bug that existed here before this PR. Follow-up required (out of scope for this PR per PR description).

**Why:** Observed during 2026-05-28 PR review of OmrGradingJob terminal-state cleanup.
**How to apply:** Flag `extractFileName` as dead-code in next exam-module touch; flag `ReportGenerationJobRepository` gap if that flow is changed.

# Exam dashboard rollup feature (2026-05-29)

## Design decisions (not violations)
- `ExamDashboardRollup` extends `TenantScopedEntity` — correct; row is tenant-scoped and uses RLS.
- `ExamGradingCompletedEvent` is a plain Spring event (not `DomainEvent`/outbox) by design — in-process only, never RabbitMQ. Acceptable for derived-projection refresh.
- `fallbackExecution = true` on `@TransactionalEventListener` is correct for the OMR path (publishes after its own tx committed, no surrounding tx).
- Caffeine in-process cache with explicit eviction is the established `CacheConfig` pattern — not a violation.
- `adapter/in/event/` and `adapter/in/scheduling/` are non-canonical sub-packages; canonical home is `adapter/in/messaging/`.

## Critical blockers (unresolved as of 2026-05-29)
- Native query aliases `resultCount`, `scoreSum`, `questionNumber`, `selectedChoice` are camelCase but unquoted. Postgres lowercases them; Hibernate 6 proxy returns null for unmatched camelCase getters. Fix: use quoted aliases (`"resultCount"`) or snake_case. Affects `ExamResultRepository.java:86-120`.
- `ExamDashboardRollupService.recomputeAll()` calls `recomputeForExam()` via `this.*` (self-invocation) — per-exam `@Transactional` is bypassed; one exam failure aborts entire tenant rebuild. Fix: self-inject via `@Lazy` or restructure to use proxy. File: `ExamDashboardRollupService.java:89`.
- `ExamDashboardRollupScheduler` uses bare `setTenant/setTenantId + clear` instead of `withinContext` — HARD_RULES violation. File: `ExamDashboardRollupScheduler.java:41`.
- Race on `findByExamId → create → save` emits unhandled `DataIntegrityViolationException` in `recomputeAll` path (event path is already swallowed). File: `ExamDashboardRollupService.java:72`.

## Semantic debt (unresolved as of 2026-05-29)
- `totalAttempts` in rollup = sum of choice rows per question (not `results.size()` as in original). Diverges when students have no `question_results` row.
- `gradeScale == null` routes to FIVE_GRADE bucket (line 66-68 of `ExamDashboardRollupService`); PASS_FAIL exams incorrectly inject into five-grade histogram.
- `ExamDashboardRollup` entity missing `equals`/`hashCode` by ID — required by entity rules.
- `calculateExamTypeAccuracy` (single-exam path) uses int[] slot 0=correct/1=total; rollup path uses 0=total/1=correct — inverted convention, maintenance hazard.

**Why:** 2026-05-29 review of exam dashboard rollup feature (OOM-fix: event-driven rollup table + Caffeine cache).
**How to apply:** When touching `ExamResultRepository` native queries, always verify quoted aliases. When touching `recomputeAll`, ensure self-invocation is fixed before marking complete.

# Phase 1 tenant domain resolution (2026-05-31)

## Design decisions (not violations)
- `GET /v1/tenants/public/by-domain` is correctly permitted unauthenticated via `SecurityConfig` line 46: `.requestMatchers("/v1/tenants/public/**").permitAll()`. Pre-existing wildcard; no change needed.
- `@Transactional(readOnly = true)` is class-level on `TenantQueryService`; new method inherits it. Spring proxy ordering: `@Cacheable` intercepts first (cache hit = no TX opened at all), `@Transactional` wraps only cache-miss paths. Correct and efficient.
- `PublicTenantResponse` record exposes: `slug`, `name`, `customId`, `logoUrl` (relative path), `phone`. No `ownerEmail`, `businessRegistrationNumber`, `address`, `customDomain`, `status` — correct public projection.
- `Tenant.setCustomDomain()` is only called in tests; no application-layer command path writes it yet. Domain-cache eviction gap (suspension/domain reassignment) is a known future concern, not a current bug.

## Warning items (unresolved as of 2026-05-31)
- `TenantQueryService.java:63` — `@Cacheable(key = "#host")` uses the RAW `host` argument as the cache key, but line 65 normalizes to `host.toLowerCase(Locale.ROOT).trim()` before querying. "ABC.com" and "abc.com" produce different cache entries mapping to the same tenant row. Fix: move normalization before the `@Cacheable` boundary — either `key = "#host.toLowerCase().trim()"` in SpEL, or normalize in `TenantController` before delegation so the service receives a canonical value.
- `TenantController.java:38` — `@RequestParam String host` with no `@NotBlank` constraint. An empty-string `?host=` passes Spring's required-param check, reaches the service, normalizes to `""`, calls `findByCustomDomain("")` against the DB (or hits a stale cache entry for the empty string), then throws a generic ResourceNotFoundException. Should add `@NotBlank` via `@Validated` on the controller class + `@NotBlank @RequestParam String host`, or at minimum validate in the service body.
- OpenAPI spec (`api-docs.json`) — `host` parameter schema is `{ "type": "string" }` with no `minLength` or `format` constraint documented. FE codegen (orval) will accept any string. Consider adding `@Schema(description = "...", minLength = 1)` to the parameter so the generated client reflects the actual contract.
- `TenantController.java:38-39` — no `@Operation` / `@ApiResponse(responseCode = "404")` annotation on the new endpoint. The existing sibling `getPublicTenantByCustomId` also lacks it, so this is consistent with current convention, but neither endpoint documents its 404 response in the OpenAPI spec. Low-priority but a gap for FE contract consumers.

## Suspension-cache gap (future concern, not in-scope for Phase 1)
- When a tenant is suspended or its `customDomain` field is reassigned, the `tenants-by-domain` Caffeine cache will serve stale data for up to 5 min on the affected pod. The `CACHE_TENANTS_BY_SLUG` cache has a `@CacheEvict` wired to `updatePlan` in `TenantServiceAdapter`; the domain cache has no write-path eviction at all. For Phase 2 (when suspension/domain-management commands are wired), add `@CacheEvict(value = CACHE_TENANTS_BY_DOMAIN, key = "#tenant.customDomain")` on the suspend/reactivate/setCustomDomain command paths.

**Why:** 2026-05-31 review of Phase 1 public by-domain tenant resolution endpoint.
**How to apply:** When touching `TenantQueryService.getPublicTenantByCustomDomain`, verify cache key is normalized. When adding domain-management commands, add matching `@CacheEvict`.

# ENTERPRISE→MAX plan rename (2026-06-02)

## What was changed
- `SubscribeRequest.java:25` — `@Schema` description updated from `(e.g. BASIC, PRO, ENTERPRISE)` to `(e.g. BASIC, PRO, MAX)`.
- `PlanFeatures.java:45` — static factory method `enterprise()` renamed to `max()`. Zero callers in main source, zero in tests.

## Findings
- CRITICAL: `app/src/test/resources/openapi/api-docs.json:4895` still contains the old string `(e.g. BASIC, PRO, ENTERPRISE)`. The source (`SubscribeRequest.java`) is updated but the OpenAPI snapshot was not regenerated — drift is immediately caught by `OpenApiSnapshotTest`. Must run `./gradlew :app:snapshotOpenApi` and commit the updated snapshot.
- No stale callers of `enterprise()` anywhere in main or test sources.
- `Plan.java:119` `toFeatures()` constructs `PlanFeatures` via builder directly (not via the static factories). This is intentional — `Plan` maps its own stored columns, not a hard-coded preset. Not a violation.
- Migration coverage is complete: app-level V51 handles the rename idempotently; billing-module V3 covers the billing track.

## How to apply
- Any rename of a plan ID or @Schema example value in a DTO must be followed by `./gradlew :app:snapshotOpenApi` to keep api-docs.json in sync. The snapshot drift test will fail CI otherwise.
