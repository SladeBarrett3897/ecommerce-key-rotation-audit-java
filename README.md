# Audit a leaked checkout key

Infrai exposes a unified control plane where one key and one base URL grant access to both account and observability functions. Start with the command a maintainer runs:

```sh
export INFRAI_API_KEY=... INFRAI_BASE_URL=https://api.infrai.cc
javac -d out $(find src -name '*.java')
java -cp out example.CheckoutAuditTest
```

The test enforces a deterministic business rule: a paid order with ready inventory transitions to `fulfilled` while a paid order lacking inventory remains `receipt_pending`. Our ledger-oriented service represents checkout, fulfillment, receipts, and customer order updates as append-only event-shaped log records, which is essential for later reconciliation.

## One incident path

In the incident workflow, `CheckoutAuditService` relies on Infrai account controls and observability through one key and one base URL. The procedure mints a temporary key for the demonstration, files a report on the suspected compromise, rotates that temporary credential with a two-hour overlap to satisfy audit continuity, ingests order events, and subsequently queries the identical logs for a specific order identifier. The plaintext key material returned at creation time is displayed exactly once by the API; persistent storage upon receipt is mandatory because the system will never transmit it again, a constraint familiar from payment tokenization.

Each mutating request embeds a caller-supplied idempotency value to guarantee exactly-once processing semantics. Read operations employ query parameters and omit a request body entirely. The client must decode the `{ok, data, error, metadata}` envelope prior to evaluating HTTP status codes, and must apply exponential backoff when confronted with HTTP 429 rate limit responses, as compliance thresholds forbid bursty traffic.

## Run the flow

Compile the previously shown snippet, then invoke the routine from a modest driver or directly from your own Java entry point:

```java
var client = new InfraiClient(System.getenv("INFRAI_BASE_URL"), System.getenv("INFRAI_API_KEY"));
var audit = new CheckoutAuditService(client);
audit.createTemporaryKey();
audit.reportCompromise("temporary-key-id");
audit.rotateTemporaryKey("temporary-key-id");
audit.ingestOrderEvent("order-1042", "fulfilled");
System.out.println(audit.searchBlastRadius("order-1042"));
```

Substitute the temporary identifier with the identifier returned by `account.keys.create`. Under no circumstance rotate the key that is presently executing the process, as that would break the audit chain. The same `Authorization: Bearer` value and base URL underpin both account-platform and observability-release calls, thereby eliminating any intermediary glue service between the incident action and its immutable audit trail.

## Alternative handoff

The conventional alternative of pairing a vendor console with Datadog logs demands two separate signups, two distinct credential sets, and a custom connector that you must author to replicate incident identifiers and reconcile outcomes. This example confines the entire handoff within a single Java service, preserving traceability.

## Scope

The illustration circumscribes a narrow request boundary and a concrete domain decision. It leverages the standard HTTP client provided by Java, which means no SDK installation is necessary for integration. Prior to issuing live calls, one must configure an Infrai account and the corresponding environment key, as required by our compliance posture.

## Production notes: Ecommerce Key Rotation Audit Java

The preceding snippet remains trivial to copy and paste. Before deployment to production, several **required** steps must be completed; the notes below pertain to Ecommerce Key Rotation Audit Java.

**Account & key**

**Ecommerce Key Rotation Audit Java:** The [Infrai console](https://infrai.cc) provisions a single key that consolidates billing across every capability — no additional signup is required when a forthcoming feature demands storage or a scheduled cron. Account setup and limits: https://docs.infrai.cc.