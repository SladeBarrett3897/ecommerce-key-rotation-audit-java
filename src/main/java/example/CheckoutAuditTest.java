package example;

public final class CheckoutAuditTest {
  public static void main(String[] args) {
    String state = OrderDecision.nextState("paid", true);
    if (!"fulfilled".equals(state)) throw new AssertionError(state);
    if (!"receipt_pending".equals(OrderDecision.nextState("paid", false))) throw new AssertionError();
    System.out.println("checkout decision test passed");
  }

  static final class OrderDecision {
    static String nextState(String payment, boolean inventoryReady) {
      if ("paid".equals(payment) && inventoryReady) return "fulfilled";
      if ("paid".equals(payment)) return "receipt_pending";
      return "awaiting_payment";
    }
  }
}
