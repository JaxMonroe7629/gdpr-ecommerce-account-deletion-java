package com.example.accountdelete;

import java.util.List;

public final class AccountDeletionServiceTest {
  public static void main(String[] args) throws Exception {
    var calls = new java.util.ArrayList<String>();
    var account = new AccountDeletionService.CustomerAccount("u-7", "k-9", List.of(new AccountDeletionService.Order("o-1", "fulfilled", "r-1")));
    new AccountDeletionService().delete(account, new AccountDeletionService.Gateway() {
      public List<String> listSessions(String userId) { calls.add("list:" + userId); return List.of("s-1", "s-2"); }
      public void revokeSession(String id) { calls.add("revoke-session:" + id); }
      public void revokeKey(String id) { calls.add("revoke-key:" + id); }
    });
    if (!account.deleted() || !calls.equals(List.of("list:u-7", "revoke-session:s-1", "revoke-session:s-2", "revoke-key:k-9")) || account.orders.size() != 1) throw new AssertionError(calls);
    System.out.println("account deletion decision: PASS");
  }
}
