# Development Guidelines: WA Task Management API

## Core Principles

### I. Code Quality Is Non-Negotiable
Changes must be maintainable: no new duplication, keep cognitive load flat, idiomatic Java 21, strictly typed models, modular Spring components, and structured JSON responses. Checkstyle, PMD, and compilation errors block merge; behavior changes require tests. Use existing naming patterns: match package, class, method, and endpoint names to nearby domains; avoid new abbreviations unless established in HMCTS taxonomy.

### II. Tests Define Release Readiness
Every feature ships with appropriate unit tests (JUnit 5 + Mockito), integration tests (Testcontainers + SpringBootTest), and functional tests (REST Assured + Serenity BDD). Contract tests (Pact) are mandatory for all external client integrations (Camunda, CCD, Role Assignment). Tests act as living documentation for the intended behavior of the Task Management API.

### III. Security & Access Management Are Paramount
HMCTS Role-Based Access Control (RBAC) and identity protocols are mandatory. Every new endpoint must implement proper authorization checks, validating both ServiceAuthorization (s2s) and user Authorization (IDAM) tokens. Data access must be filtered by the user's role and permissions. Security bypasses or hardcoded roles are strictly prohibited.

### IV. Unit Test Quality And Maintainability Standards
Unit tests must remain clear, deterministic, and resistant to implementation-only refactors. For all non-trivial test changes, apply these mandatory standards:
*   Assert behavior and collaborator contracts at module boundaries (e.g., verify(mockRepository).save(...)).
*   Cover both happy paths and exception/error paths for dependency-bound logic (assertThrows, doThrow, or equivalent).
*   Use explicit negative-path assertions (error type/status/message); avoid broad generic Exception.class catch-alls.
*   Freeze time for time-sensitive logic by mocking Spring's Clock bean or passing fixed temporal instances; never rely on LocalDateTime.now() without a clock abstraction.
*   Avoid coupling tests to framework/private internals (e.g., reflection to set private fields) unless absolutely no public seam or constructor exists, and document the reason.
*   Extract repeated large fixtures (Task objects, Role assignments) into typed builders/factories in src/testUtils once duplication appears.
*   Prefer high-signal assertions using AssertJ; avoid low-value checks like assertNotNull when deep field equality (usingRecursiveComparison()) is available.
*   Keep tests focused: one behavior per test where practical, avoiding large omnibus cases that obscure failure diagnosis.

## Active Technologies
*   Backend: Java 21, Spring Boot 3.4.x, Spring Data JPA, Spring Security, Lombok.
*   Integrations: Camunda (Workflow), CCD (Core Case Data), Role Assignment Service (AM), IDAM/S2S (Auth).
*   Database: PostgreSQL with Flyway. Dual-stack: Primary (Read/Write) and Replica (Read-only). Heavy use of PL/pgSQL, JSONB, arrays, and PostgreSQL-specific index/operator support.
*   Infrastructure: Gradle (via Wrapper), Docker/Docker-Compose, Minikube (wa-kube-environment).
*   Testing: JUnit 5, Mockito, AssertJ, Testcontainers, REST Assured, Serenity BDD, Pact (Contract Testing).
*   Observability: Application Insights, Prometheus, structured JSON logging.
*   Quality: Checkstyle, PMD, Pitest (Mutation Testing), OWASP Dependency Check.

## Subagents (When Available)
Use subagents to parallelize work that can be done independently, then consolidate findings in the main thread. Good fit examples:
*   Broad discovery: One agent scans docs/, another scans src/main/java/uk/gov/hmcts/reform/wataskmanagementapi/domain/, another scans src/test/.
*   Cross-cutting refactors: Database/Flyway schema updates vs. Entity/Repository updates vs. Controller/Service updates.
*   Test coverage work: One agent identifies missing coverage in SonarQube/JaCoCo reports, another drafts the missing JUnit/Integration tests.
*   Verification orchestration: Delegate linting, unit tests, and integration tests to separate agents and aggregate results.

For verification after code changes, use subagents by default and run independent checks in parallel:
*   Spawn one subagent each for ./gradlew check (Checkstyle/PMD), ./gradlew test (unit tests), and ./gradlew integration (Testcontainer-backed tests).
*   Run these checks in parallel unless a concrete dependency requires sequencing.

## Project Structure
uk.gov.hmcts.reform.wataskmanagementapi/
├── auth/           # IDAM, s2s, and RBAC filters (role, access, restrict, permission)
├── cft/            # Case Framework Tooling (replica repo, query logic)
├── clients/        # Feign/Rest clients for Camunda, CCD, RAS, BankHolidays
├── config/         # Feature flags, database/executor configs
├── controllers/    # REST API Entrypoints
├── domain/         # Entities, Enums, DTOs, Search Criteria, DMN models
├── entity/         # JPA Entities (Primary and Replica)
├── repository/     # Spring Data JPA interfaces (Primary)
├── schedulers/     # Background tasks
└── services/       # Core Business Logic (TaskManagementService, DmnEvaluationService)
resources/
├── db/migration/   # Flyway SQL scripts (Primary)
├── dbreplica/migration/ # Flyway SQL scripts (Replica)
└── application.yaml

## Key Commands
Always use the Gradle wrapper (./gradlew).
*   ./gradlew clean build (Compiles code and runs full checks)
*   ./gradlew check (Runs static analysis: Checkstyle, PMD)
*   ./gradlew test (Runs unit tests and generates JaCoCo coverage)
*   ./gradlew integration (Runs integration tests — Requires Docker)
*   ./gradlew functional (Runs functional tests against a deployed/local environment)
*   ./gradlew contract (Runs Pact/Contract tests)
*   ./gradlew pitest (Runs mutation testing)
*   ./gradlew bootRun (Starts the Spring Boot application locally)

## Implementation Guidance
*   Domain-Driven Design: Keep business logic in services/. Controllers should only handle HTTP routing, payload validation, and delegating to services. Repositories should only handle persistence.
*   DMN-First Configuration: Task attributes and permissions are driven by Camunda DMN tables (WA_TASK_CONFIGURATION, WA_TASK_PERMISSIONS). See DmnEvaluationService.
*   Search & Indexing: The project uses a signature-based RBAC search system. The legacy `search_index` GIN expression index has been replaced by materialised signature arrays on `tasks`, plus GiST `intarray` hash indexes and exact `text[]` overlap checks. For full technical details on queries, relations, and indexes, refer to [docs/database_search_context.md](docs/database_search_context.md).
*   Search Migration: Any search-indexing change must account for the `indexed` flag, `filter_signatures`, `role_signatures`, materialised signature/hash columns, and the refresh triggers on both `tasks` and `task_roles`. Do not add side tables for search indexing unless the architecture is explicitly revisited; prefer scoped columns/indexes on existing tables.
*   Search Performance: Avoid replacing signature search with JSONB unless there is a measured plan showing equal or better performance without GIN. JSONB search normally relies on GIN for competitive containment/overlap behavior, which conflicts with the current non-GIN direction.
*   Access Control: Every new controller endpoint must be protected. Ensure @PreAuthorize annotations or explicit RBAC service checks validate the requester's permissions against the Task's required roles.
*   Mapping Precedence: CFTTaskMapper handles complex entity-to-resource transformations. Be aware of precedence rules for DUE_DATE vs PRIORITY_DATE and timezone normalizations.
*   Database Migrations: Never modify existing Flyway scripts under db/migration/ or dbreplica/migration/. Always create a new versioned script (e.g., V1.0.X__My_Change.sql).
*   Replica Mode: To enable logical replication features locally, run with SPRING_PROFILES_ACTIVE=replica.
*   Documentation: Before researching or planning a change, review the relevant README.md and OpenAPI specs. Update docs in sync with implementation.
*   Test Coverage: Branch and line coverage per modified file should exceed 80%.
*   Mandatory Verification: The final step after any code, config, or SQL change is to run ./gradlew check test integration. For narrowly scoped search-indexing changes, at minimum run the focused repository/service unit tests and Testcontainers-backed search integration tests before broader verification.

## ExecPlans
When writing complex features (e.g., complex role assignment logic, new replication profiles, heavy SQL updates) or significant refactors, use an ExecPlan (as described in PLANS.md) from design to implementation.
*   ExecPlans may be treated as working artifacts and can remain uncommitted, but important, durable outcomes must be transferred into committed README.md or docs/ before the related code change is considered complete.
*   Transfer only what helps future contributors understand and evolve the system (e.g., database indexing strategies, RBAC permission matrices). Omit transient planning artifacts.

## Repo Skills
*   gradle-dependency-upgrades: Upgrade dependencies via ./gradlew dependencyUpdates. Focus on Spring Boot version alignment, CVE remediation, and HMCTS shared library bumps. Ensure build.gradle and config/owasp/suppressions.xml are updated correctly for false-positive CVEs.
