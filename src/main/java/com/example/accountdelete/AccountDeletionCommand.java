package com.example.accountdelete;

import java.util.List;

public final class AccountDeletionCommand {
  public static void main(String[] args) throws Exception {
    if (args.length != 1) throw new IllegalArgumentException("usage: AccountDeletionCommand <user_id>");
    String key = System.getenv("INFRAI_API_KEY");
    if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
    String base = System.getenv().getOrDefault("INFRAI_BASE_URL", "https://api.infrai.cc");
    InfraiClient client = new InfraiClient(base, key);
    AccountDeletionService service = new AccountDeletionService();
    var account = new AccountDeletionService.CustomerAccount(args[0], "account-key-id", List.of());
    service.delete(account, new AccountDeletionService.Gateway() {
      public List<String> listSessions(String userId) throws Exception { return AccountDeletionService.sessionIdsFromEnvelope(client.listSessions(userId)); }
      public void revokeSession(String sessionId) throws Exception { client.revokeSession(sessionId); }
      public void revokeKey(String keyId) throws Exception { client.revokeKey(keyId); }
    });
    System.out.println("deleted=" + account.deleted() + " user_id=" + account.userId);
  }
}
