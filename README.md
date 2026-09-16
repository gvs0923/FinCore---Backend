# FinCore — Intelligent Payment & Ledger Platform

FinCore is a production-inspired, event-driven payment infrastructure platform.
It processes payments, maintains financial truth via a double-entry ledger,
uses Kafka for asynchronous workflows, Redis for low-latency operations, and
(later) a hybrid rules/ML risk engine plus an AI operations copilot.

This is a personal learning/portfolio project. No proprietary code, data, or
tooling from any employer is used — only open-source software and publicly
available documentation.

## Status

🚧 Early development — see `docs/architecture/roadmap.md` for the build order.

## Modules (planned)

| Module            | Purpose                                      |
|-------------------|-----------------------------------------------|
| `ledger-service`  | Double-entry ledger, source of financial truth |
| `payment-service` | Payment state machine, idempotency, orchestration |
| `risk-service`    | Fraud/risk rules engine                       |
| `settlement-service` | Merchant settlement batches                |
| `reconciliation-service` | Internal vs. processor reconciliation  |
| `ai-service`      | Python ML risk scoring + LLM explanations (later phase) |

## Local development

Prerequisites: Docker (this project was set up against [Colima](https://github.com/abiosoft/colima)
rather than Docker Desktop), Gradle (`brew install gradle` — we use the
system Gradle install rather than the wrapper here, see note below), Java 21
(via Gradle toolchain).

```bash
docker-compose up -d       # Postgres, Redis (Kafka added in Phase 3)
gradle build               # build all modules
```

### Setup notes specific to this machine

- **Gradle wrapper vs. system Gradle**: on this network, the Gradle wrapper's
  distribution download (multi-hop redirect through GitHub release assets)
  fails TLS validation even though direct Maven Central access works fine.
  We install Gradle via Homebrew and invoke `gradle` directly instead of
  `./gradlew`.
- **`GRADLE_OPTS` for TLS**: some HTTPS calls made by the JVM need to trust
  certs already trusted by macOS (e.g. a network-level TLS-intercepting
  proxy). Rather than modifying Java's global cacerts, point the JVM at the
  macOS Keychain's trust store for Gradle invocations:
  ```bash
  export GRADLE_OPTS="-Djavax.net.ssl.trustStoreType=KeychainStore -Djavax.net.ssl.trustStore=NONE"
  ```
- **Colima + Testcontainers**: `build.gradle` sets `DOCKER_HOST` (pointing at
  Colima's socket) and `TESTCONTAINERS_RYUK_DISABLED=true` (Ryuk has known
  startup issues under Colima's VM) as environment for the `test` task, so
  integration tests work out of the box — no global machine config needed.
- **Account balance provisioning**: `account_balances` rows must be created
  (at zero) when an account is created, not lazily on first posting —
  see ADR-001.

## Documentation

- `docs/architecture/` — system design docs
- `docs/decisions/` — Architecture Decision Records (ADRs)
- `docs/reliability/` — failure scenarios, disaster recovery
- `docs/performance/` — load test results
- `docs/security/` — threat model
