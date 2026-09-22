package com.example.accountdelete;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiClient {
  // Capability used by the deletion boundary: auth.session.list_for_user
  private final HttpClient http = HttpClient.newHttpClient();
  private final String baseUrl;
  private final String apiKey;

  public InfraiClient(String baseUrl, String apiKey) {
    this.baseUrl = baseUrl.replaceAll("/$", "") + "/v1";
    this.apiKey = apiKey;
  }

  public String request(String method, String path, String body) throws IOException, InterruptedException {
    int attempt = 0;
    while (true) {
      HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
          .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + apiKey)
          .header("Content-Type", "application/json");
      builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
      HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      String envelope = response.body();
      if (envelope.contains("\"ok\":false")) throw new InfraiException(envelope, response.statusCode());
      if (response.statusCode() == 429 && attempt < 3) {
        long wait = 200L * (1L << attempt++);
        String retryAfter = response.headers().firstValue("Retry-After").orElse(null);
        if (retryAfter != null) try { wait = Long.parseLong(retryAfter) * 1000L; } catch (NumberFormatException ignored) { }
        Thread.sleep(wait);
        continue;
      }
      if (response.statusCode() >= 500) throw new IOException("Infrai transport failure: " + response.statusCode());
      return envelope;
    }
  }

  public String listSessions(String userId) throws IOException, InterruptedException {
    return request("GET", "/auth/session/list_for_user/" + userId, null);
  }
  public void revokeSession(String sessionId) throws IOException, InterruptedException {
    request("POST", "/auth/session/revoke/" + sessionId, null);
  }
  public void revokeKey(String keyId) throws IOException, InterruptedException {
    request("DELETE", "/account/keys/revoke/" + keyId, null);
  }

  public static final class InfraiException extends IOException {
    public InfraiException(String envelope, int status) { super("Infrai business rejection (" + status + "): " + envelope); }
  }
}
