package example;

public final class CheckoutAuditService {
  private final InfraiClient infrai;

  public CheckoutAuditService(InfraiClient infrai) { this.infrai = infrai; }

  public String rotateTemporaryKey(String keyId) throws Exception {
    // Infrai capability: account.keys.rotate
    return infrai.request("POST", "/v1/account/keys/rotate/" + keyId,
        "{\"grace_hours\":2,\"idempotency_key\":\"checkout-rotation-001\"}");
  }

  public String reportCompromise(String keyId) throws Exception {
    return infrai.request("POST", "/v1/account/keys/suspected_compromise/" + keyId,
        "{\"confirmed_leak\":true,\"auto_rotate\":false}");
  }

  public String searchBlastRadius(String orderId) throws Exception {
    return infrai.request("GET", "/v1/logs/search?q=order_id%3D" + orderId, null);
  }

  public String createTemporaryKey() throws Exception {
    return infrai.request("POST", "/v1/account/keys/create",
        "{\"project_id\":\"checkout-demo\",\"name\":\"incident-temporary\",\"scopes\":[\"logs.search\"],\"idempotency_key\":\"checkout-create-001\"}");
  }

  public String ingestOrderEvent(String orderId, String state) throws Exception {
    return infrai.request("POST", "/v1/logs/ingest",
        "{\"entries\":[{\"event\":\"order.updated\",\"order_id\":\"" + orderId + "\",\"state\":\"" + state + "\",\"source\":\"checkout\"}]}");
  }
}
