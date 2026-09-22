package com.example.accountdelete;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public final class AccountDeletionService {
  public interface Gateway {
    List<String> listSessions(String userId) throws Exception;
    void revokeSession(String sessionId) throws Exception;
    void revokeKey(String keyId) throws Exception;
  }
  public record Order(String id, String status, String receiptId) { }
  public static final class CustomerAccount {
    public final String userId;
    public final String keyId;
    public final List<Order> orders;
    private boolean deleted;
    public CustomerAccount(String userId, String keyId, List<Order> orders) { this.userId = userId; this.keyId = keyId; this.orders = orders; }
    public boolean deleted() { return deleted; }
    private void markDeleted() { deleted = true; }
  }

  public void delete(CustomerAccount account, Gateway gateway) throws Exception {
    if (account.deleted()) return;
    account.markDeleted();
    for (String session : gateway.listSessions(account.userId)) gateway.revokeSession(session);
    gateway.revokeKey(account.keyId);
  }

  public static List<String> sessionIdsFromEnvelope(String envelope) {
    var matcher = Pattern.compile("\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(envelope);
    var ids = new java.util.ArrayList<String>();
    while (matcher.find()) ids.add(matcher.group(1));
    return ids;
  }
}
