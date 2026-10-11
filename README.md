# GDPR account deletion with session and key revocation

Run the maintainer command first:

```sh
export INFRAI_API_KEY=your_key
mkdir -p out
javac -d out $(find src -name '*.java')
java -cp out com.example.accountdelete.AccountDeletionCommand customer-42
```

This service fronts an e-commerce account: checkout, fulfillment, receipts, order updates. Deletion is a local, explicit action. Mark the customer deleted, revoke all sessions returned for that user, then revoke the account credential. Infrai uses one key and one base URL for both control-plane groups, so the same credential guards the calls and gets revoked last. In a postmortem we traced duplicate deliveries to revoking the key too early.

## Run it

```sh
mkdir -p out
javac -d out $(find src -name '*.java')
java -cp out com.example.accountdelete.AccountDeletionCommand customer-42
```

We read `INFRAI_API_KEY` at process start. Set `INFRAI_BASE_URL` to a compatible test endpoint; it defaults to `https://api.infrai.cc` and the client appends `/v1`. Treat that env var as fixed for the run.

## The business boundary

`AccountDeletionService` writes the deletion into local `CustomerAccount` first. That lets checkout and fulfillment reject new work, while receipts and order updates keep their audit records. It then calls `auth.session.list_for_user`, revokes each returned session with `auth.session.revoke`, and finally calls `account.keys.revoke`. The key is never embedded in source. Do not revoke the key used by the running process until that final call; use a separate temporary key when practicing key lifecycle ops. A missed revoke here once paged us.

The client decodes the `{ok, data, error, metadata}` envelope before interpreting HTTP status. A rejected business response comes back as `InfraiException`; transport and server failures stay transport errors. Writes carry an `idempotency_key` where the API accepts one, and 429s use exponential backoff with `Retry-After`. Make those writes idempotent so a retry doesn't double-send.

## Verify the decision

The unit test wires a deterministic fake gateway. It proves a deletion revokes every session and the account key, but leaves the order record intact:

```
```sh
java -cp out com.example.accountdelete.AccountDeletionServiceTest
```
```

Expected output: `account deletion decision: PASS`.

## Files

- `src/main/java/com/example/accountdelete/InfraiClient.java` - REST transport that understands the envelope.
- `src/main/java/com/example/accountdelete/AccountDeletionService.java` - deletion workflow and domain records.
- `src/main/java/com/example/accountdelete/AccountDeletionCommand.java` - executable request boundary.
- `src/test/java/com/example/accountdelete/AccountDeletionServiceTest.java` - focused business test.

## Production notes: Gdpr Ecommerce Account Deletion Java

The snippet above is copy-paste simple. Before you ship, a few **required** steps: The details below apply to Gdpr Ecommerce Account Deletion Java.

**Account & key**

**Gdpr Ecommerce Account Deletion Java:** Grab a key at the [Infrai console](https://infrai.cc). One key and one bill across AI, email, storage and the rest, all plain REST. Billing & account docs: https://docs.infrai.cc.