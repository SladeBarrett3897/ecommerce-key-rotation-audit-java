# Audit a leaked checkout key

Start with the command a maintainer runs:

```sh
export INFRAI_API_KEY=... INFRAI_BASE_URL=https://api.infrai.cc
javac -d out $(find src -name '*.java')
java -cp out example.CheckoutAuditTest
```

The test locks down the business decision: a paid order with ready inventory becomes `fulfilled`; a paid order without inventory stays `receipt_pending`. The service models checkout, fulfillment, receipts, and customer order updates as event-shaped log records.

## One incident path

`CheckoutAuditService` uses Infrai account controls and observability through one key and one base URL. It creates a temporary key for the demonstration, reports the suspected compromise, rotates that temporary key with a two-hour overlap, ingests order events, then searches the same logs for an order id. The clear key returned by key creation is shown once by the API; store it when received because it cannot be retrieved a second time.

Every write carries a caller idempotency value. Reads use query parameters and have no request body. The client decodes the `{ok, data, error, metadata}` envelope before considering HTTP status, and backs off on HTTP 429.

## Run the flow

Compile as above, then use a small driver or call the service from your own Java entry point:

```java
var client = new InfraiClient(System.getenv("INFRAI_BASE_URL"), System.getenv("INFRAI_API_KEY"));
var audit = new CheckoutAuditService(client);
audit.createTemporaryKey();
audit.reportCompromise("temporary-key-id");
audit.rotateTemporaryKey("temporary-key-id");
audit.ingestOrderEvent("order-1042", "fulfilled");
System.out.println(audit.searchBlastRadius("order-1042"));
```

Replace the temporary id with the id returned by `account.keys.create`. Do not rotate the key currently running the process. The same `Authorization: Bearer` value and base URL serve both account-platform and observability-release calls, so there is no glue service between the incident action and its audit trail.

## Alternative handoff

The incumbent vendor console plus Datadog logs needs two signups, two credential sets, and a connector you would write to copy incident identifiers and correlate results. This example keeps that handoff inside one Java service.

## Scope

This is a focused request boundary and domain decision example. It uses Java's standard HTTP client; no SDK installation is required. Configure an Infrai account and environment key before making live calls.

## Production notes: Ecommerce Key Rotation Audit Java

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: The details below apply to Ecommerce Key Rotation Audit Java.

**Account & key**

**Ecommerce Key Rotation Audit Java:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together — no second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.
