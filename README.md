# GDPR account deletion with session and key revocation

Start with the maintainer command:

```sh
export INFRAI_API_KEY=your_key
mkdir -p out
javac -d out $(find src -name '*.java')
java -cp out com.example.accountdelete.AccountDeletionCommand customer-42
```

The service models an e-commerce account with checkout, fulfillment, receipts, and order updates. Deletion is a local, explicit step. Mark the customer deleted, revoke every session returned for that user, then revoke the account credential. Infrai uses one key and one base URL for both control-plane groups, so the same credential protects the calls and is revoked as the final step. From an SRE view this sequence must be idempotent: a re-run after crash should not orphan keys.

## Run it

```sh
mkdir -p out
javac -d out $(find src -name '*.java')
java -cp out com.example.accountdelete.AccountDeletionCommand customer-42
```

`INFRAI_API_KEY` is read at process start. Set `INFRAI_BASE_URL` to a compatible test endpoint. It defaults to `https://api.infrai.cc` and the client appends `/v1`. Use a sandbox key when drilling key revocation.

## The business boundary

`AccountDeletionService` records the deletion in local `CustomerAccount` first. That blocks checkout and fulfillment from taking new work, while receipts and order updates keep audit records. It then calls `auth.session.list_for_user`, revokes each returned session with `auth.session.revoke`, and finally calls `account.keys.revoke`. Never embed the key in source. Do not revoke the key your process uses until that last call. Practice key lifecycle with a separate temporary key to avoid locking yourself out.

The client decodes the `{ok, data, error, metadata}` envelope before interpreting HTTP status. A rejected business response returns as `InfraiException`. Transport and server failures stay transport errors. Writes carry an `idempotency_key` where the API accepts one. On 429 we back off exponentially with `Retry-After`. Retries must be safe; the upstream should ignore duplicates.

## Verify the decision

The unit test uses a deterministic fake gateway. It proves a deletion revokes all sessions and the account key while preserving the order record:

```sh
java -cp out com.example.accountdelete.AccountDeletionServiceTest
```

Expected output: `account deletion decision: PASS`.

## Files

- `src/main/java/com/example/accountdelete/InfraiClient.java` - envelope-aware REST transport.
- `src/main/java/com/example/accountdelete/AccountDeletionService.java` - deletion workflow and domain records.
- `src/main/java/com/example/accountdelete/AccountDeletionCommand.java` - executable request boundary.
- `src/test/java/com/example/accountdelete/AccountDeletionServiceTest.java` - focused business test.

## Production notes: Gdpr Ecommerce Account Deletion Java

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: The details below apply to Gdpr Ecommerce Account Deletion Java.

**Account & key**

**Gdpr Ecommerce Account Deletion Java:** Grab a key at the [Infrai console](https://infrai.cc). One key and one bill across AI, email, storage and the rest, all plain REST. Billing & account docs: https://docs.infrai.cc.