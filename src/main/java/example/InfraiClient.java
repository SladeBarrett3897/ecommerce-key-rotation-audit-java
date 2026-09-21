package example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiClient {
  private final HttpClient http = HttpClient.newHttpClient();
  private final String baseUrl;
  private final String apiKey;

  public InfraiClient(String baseUrl, String apiKey) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
  }

  public String request(String method, String path, String body) throws IOException, InterruptedException {
    for (int attempt = 0; attempt < 3; attempt++) {
      HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
          .timeout(Duration.ofSeconds(20))
          .header("Authorization", "Bearer " + apiKey)
          .header("Content-Type", "application/json");
      builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
      HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      String payload = response.body();
      if (payload.contains("\"ok\":false")) throw new IOException("Infrai envelope error: " + payload);
      if (response.statusCode() == 429 && attempt < 2) {
        Thread.sleep((long) Math.pow(2, attempt) * 250);
        continue;
      }
      if (response.statusCode() >= 500) throw new IOException("Infrai transport failure: " + response.statusCode());
      return payload;
    }
    throw new IOException("request retries exhausted");
  }
}
