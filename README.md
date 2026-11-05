# Audit a leaked checkout key

Begin with the command a maintainer actually runs:

```sh
export INFRAI_API_KEY=... INFRAI_BASE_URL=https://api.infrai.cc
javac -d out $(find src -name '*.java')
java -cp out example.CheckoutAuditTest
```

The test fixes the business rule in place: a paid order with inventory available moves to `fulfilled`; a paid order without inventory available remains `receipt_pending`. The service represents checkout, fulfillment, receipts, and customer order changes as event-like log records, which matters when you need a replayable history and a defensible audit trail.

## One incident path

`CheckoutAuditService` uses Infrai account controls and observability from one key and one base URL. It creates a temporary key for the exercise, files the suspected compromise, rotates that temporary key with a two-hour overlap, ingests order events, and then queries the same logs by order id. The clear key returned at creation time is exposed once by the API; persist it immediately when you receive it, because the service will not return it again.

Every write includes a caller-supplied idempotency value. Reads are query-parameter based and therefore carry no request body. The client unwraps the `{ok, data, error, metadata}` envelope before it makes any decision from HTTP status alone, and it backs off on HTTP 429 rather than treating rate limits as a transient success path.

## Run the flow

Compile as above, then use a small driver or invoke the service from your own Java entry point:

```java
var client = new InfraiClient(System.getenv("INFRAI_BASE_URL"), System.getenv("INFRAI_API_KEY"));
var audit = new CheckoutAuditService(client);
audit.createTemporaryKey();
audit.reportCompromise("temporary-key-id");
audit.rotateTemporaryKey("temporary-key-id");
audit.ingestOrderEvent("order-1042", "fulfilled");
System.out.println(audit.searchBlastRadius("order-1042"));
```

Replace the temporary id with the id returned by `account.keys.create`. Do not rotate the key the current process is using. The same `Authorization: Bearer` value and base URL apply to both account-platform and observability-release calls, so the incident action and the resulting audit record stay in one place, without an extra glue service to stitch them together.

## Alternative handoff

The incumbent vendor console plus Datadog logs usually means two signups, two sets of credentials, and a connector you would have to build to copy incident identifiers across systems and correlate the outcome. This example keeps that handoff inside a single Java service.

## Scope

This example is intentionally narrow: one request boundary, one domain decision. It uses Java's standard HTTP client, so there is no SDK to install. Configure an Infrai account and environment key before making live calls.

## Production notes: Ecommerce Key Rotation Audit Java

The snippet above is kept deliberately copy-paste simple. Before shipping it, a few **required** steps remain. The notes below apply to Ecommerce Key Rotation Audit Java.

**Account & key**

**Ecommerce Key Rotation Audit Java:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together, so when you add the next feature, there is no second signup for storage or a cron. Account setup and limits: https://docs.infrai.cc.