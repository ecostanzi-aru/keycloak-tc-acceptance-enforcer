# Keycloak Terms & Conditions Administration Extension

## 1. Objective

Implement a Keycloak 26.4.x extension that adds a dedicated **Terms & Conditions** administration screen to the Keycloak Admin Console.

The screen allows an administrator to publish a new version of the realm Terms & Conditions.

Publishing a new version must:

1. update T&C metadata stored as realm attributes;
2. record the publication timestamp;
3. optionally/ultimately force existing users to execute Keycloak's built-in `TERMS_AND_CONDITIONS` required action;
4. perform the operation through a server-side Keycloak Admin REST extension rather than implementing business logic in the browser.

Target Keycloak version:

```text
26.4.x
```

Development should initially target:

```text
26.4.7
```

Do not depend on APIs introduced after Keycloak 26.4.

---

# 2. High-level architecture

Implement two components:

```text
┌───────────────────────────────────┐
│ Custom Keycloak Admin Console     │
│                                   │
│ Terms & Conditions                │
│                                   │
│ Version: 4                        │
│ Published: 2026-09-29T14:00Z      │
│                                   │
│ [ Publish new version ]           │
└─────────────────┬─────────────────┘
                  │
                  │ authenticated Admin API request
                  ▼
┌───────────────────────────────────┐
│ Keycloak server extension         │
│                                   │
│ AdminRealmResourceProvider        │
│                                   │
│ POST .../terms-and-conditions/    │
│      publish                      │
└─────────────────┬─────────────────┘
                  │
                  ├─ update realm attributes
                  │
                  ├─ generate admin event
                  │
                  └─ assign required action
                     to existing users
```

The UI must contain presentation logic only.

All mutation/business logic must execute server-side.

---

# 3. Keycloak extension mechanisms

## 3.1 Admin Console

Build a custom Admin Console using:

```text
@keycloak/keycloak-admin-ui
```

Use Keycloak's composable Admin Console mechanism rather than patching or forking the Keycloak Admin Console source tree.

Keycloak officially exposes `@keycloak/keycloak-admin-ui` as the basis for custom Admin Consoles.

The resulting theme must be buildable into a JAR and deployable through the normal Keycloak `/providers` mechanism.

Do not modify files inside a Keycloak distribution at runtime.

## 3.2 Server API

Implement the backend extension using:

```java
AdminRealmResourceProvider
AdminRealmResourceProviderFactory
```

These APIs exist in Keycloak 26.4.x and are intended to expose additional resources below the realm Admin REST API.

Prefer this over the generic:

```java
RealmResourceProvider
```

because the Admin variant receives:

```java
KeycloakSession
RealmModel
AdminPermissionEvaluator
AdminEventBuilder
```

directly.

This allows the extension to participate naturally in Keycloak administration authorization and auditing.

---

# 4. Realm attributes

Store T&C metadata using realm attributes.

Use the following names:

```text
terms.version
terms.publishedAt
```

Example:

```text
terms.version = 4
terms.publishedAt = 2026-09-29T14:42:17Z
```

`RealmRepresentation` supports custom attributes through:

```java
Map<String, String> attributes
```

and Keycloak allows realm top-level configuration to be updated through the Admin API.

## 4.1 `terms.version`

Semantic type:

```text
positive integer
```

Stored physically as a string because realm attributes are strings.

Examples:

```text
1
2
3
4
```

The first publication must produce:

```text
1
```

Every subsequent publication increments the existing value by exactly one.

If the attribute is missing:

```text
current version = 0
```

If the attribute exists but cannot be parsed as a non-negative integer, the publish operation must fail rather than silently reset it.

## 4.2 `terms.publishedAt`

UTC timestamp in ISO-8601 format.

Example:

```text
2026-09-29T14:42:17Z
```

Generate the value server-side.

Do not accept the publication timestamp from the browser.

---

# 5. Terms content

This feature does NOT introduce an alternative storage mechanism for the Terms & Conditions HTML.

Continue using the existing Keycloak Terms & Conditions mechanism and the realm localization override:

```text
termsText
```

The custom page is responsible for publishing/versioning the terms, not replacing Keycloak's existing rendering of the terms page.

For the first implementation, assume the administrator edits `termsText` using the existing Keycloak facilities before pressing **Publish new version**.

---

# 6. Admin REST API

Expose an Admin API extension.

Provider ID:

```text
terms-and-conditions
```

Target endpoint:

```http
POST /admin/realms/{realm}/terms-and-conditions/publish
```

If Keycloak's `AdminRealmResourceProvider` path resolution requires a slightly different concrete mapping, preserve the logical API structure above.

No request body is required for the MVP.

Example:

```http
POST /admin/realms/acme/terms-and-conditions/publish
Authorization: Bearer <admin-token>
```

Response:

```json
{
  "version": 4,
  "publishedAt": "2026-09-29T14:42:17Z"
}
```

Status:

```text
200 OK
```

Also expose:

```http
GET /admin/realms/{realm}/terms-and-conditions
```

Response:

```json
{
  "version": 4,
  "publishedAt": "2026-09-29T14:42:17Z"
}
```

If the terms have never been published:

```json
{
  "version": 0,
  "publishedAt": null
}
```

---

# 7. Authorization

The extension must never rely only on the fact that the caller possesses a Keycloak access token.

Use the supplied:

```java
AdminPermissionEvaluator
```

to enforce realm-management permissions.

The GET operation requires permission to view the realm.

The POST operation requires permission to manage the realm.

Conceptually:

```java
auth.realm().requireViewRealm();
```

for GET and:

```java
auth.realm().requireManageRealm();
```

for POST.

Use the actual method names exposed by Keycloak 26.4.7 rather than blindly copying the pseudo-code above if the API differs slightly.

An unauthorized caller must receive:

```text
403 Forbidden
```

Do not expose an equivalent unauthenticated endpoint through `RealmResourceProvider`.

---

# 8. Publish operation

The server-side publish operation performs the following steps.

```text
1. Verify realm-management permission.

2. Read realm attribute:
      terms.version

3. Parse the version.

4. Compute:
      newVersion = currentVersion + 1

5. Compute server-side UTC timestamp:
      publishedAt = Instant.now()

6. Set:
      terms.version = newVersion
      terms.publishedAt = publishedAt

7. Mark existing users as requiring
      TERMS_AND_CONDITIONS

8. Generate an appropriate Keycloak admin event.

9. Return the resulting version and timestamp.
```

The browser MUST NOT:

```text
calculate the next version
generate the authoritative timestamp
loop through users
assign required actions
```

Those responsibilities belong to the server.

---

# 9. Required action

Use Keycloak's built-in required action:

```text
TERMS_AND_CONDITIONS
```

Do not create a duplicate custom required action unless implementation constraints make the built-in one unusable.

After publication, existing applicable users must have the required action added to their user model.

Conceptually:

```java
user.addRequiredAction(
    UserModel.RequiredAction.TERMS_AND_CONDITIONS
);
```

Use the appropriate Keycloak 26.4 API.

Adding the same required action multiple times must be harmless/idempotent.

---

# 10. User enumeration

Do not load every user into memory.

Process users using the Keycloak user provider stream/pagination facilities.

Conceptually:

```java
session.users()
       .getUsersStream(realm)
       .forEach(user -> ...);
```

Verify the appropriate API for Keycloak 26.4.7.

Avoid:

```java
List<UserModel> allUsers = ...
```

for potentially large realms.

---

# 11. Transaction semantics

Realm attribute changes and user required-action changes should participate in the Keycloak transaction associated with the REST request where practical.

The implementation must not return success if the realm metadata itself could not be updated.

For the initial implementation, synchronous user processing is acceptable.

However, structure the code so the operation can later be changed to asynchronous/batched execution without changing the Admin Console API significantly.

Suggested abstraction:

```java
TermsService
```

with methods such as:

```java
TermsStatus getStatus(RealmModel realm);

TermsPublicationResult publish(
    KeycloakSession session,
    RealmModel realm
);
```

Keep iteration over users out of the JAX-RS resource class.

---

# 12. Large realm consideration

The MVP may synchronously update all users.

Document clearly that this has scalability implications for realms containing very large numbers of users.

Do not prematurely introduce:

```text
Kafka
external queues
scheduled jobs
custom database tables
```

for the initial implementation.

Keep the service layer isolated so that asynchronous publication can be introduced later if necessary.

---

# 13. Concurrent publication protection

Two administrators pressing **Publish** simultaneously must not silently create the same version.

At minimum, investigate Keycloak transaction behavior around realm attribute updates.

Preferred behavior:

```text
publication A -> version 4
publication B -> version 5
```

Never:

```text
publication A -> version 4
publication B -> version 4
```

If reliable atomic increment semantics cannot be implemented using the existing realm model, document the limitation clearly and return a conflict where detectable.

Do NOT introduce a new database table solely for this problem in the initial implementation unless necessary.

---

# 14. Admin audit event

Use the supplied:

```java
AdminEventBuilder
```

to record publication.

The event should contain enough information to identify:

```text
realm
operation
new terms version
publication timestamp
```

Never log the complete Terms HTML.

Prefer an UPDATE-style admin event associated with a resource path such as:

```text
terms-and-conditions
```

or:

```text
terms-and-conditions/publish
```

Use the Keycloak 26.4 admin-event API idioms.

---

# 15. Admin Console page

Add a dedicated navigation destination named:

```text
Terms & Conditions
```

The page should visually follow the existing Keycloak Admin Console conventions and PatternFly components.

Page layout:

```text
Terms & Conditions

Manage publication of the Terms & Conditions for this realm.

Current version
4

Published at
29 September 2026, 16:42

[ Publish new version ]
```

If never published:

```text
Current version
Not published

Published at
—

[ Publish first version ]
```

---

# 16. Publish confirmation

Publishing affects all existing users.

Therefore, pressing the button must first display a confirmation modal.

Example:

```text
Publish new Terms & Conditions?

Publishing a new version will require users to accept the
Terms & Conditions again when they next authenticate.

Current version: 4
New version: 5

[Cancel] [Publish]
```

The UI may display:

```text
New version: currentVersion + 1
```

for informational purposes.

The server remains authoritative and independently calculates the version.

---

# 17. UI request lifecycle

On page load:

```text
GET /admin/realms/{realm}/terms-and-conditions
```

Display the returned state.

On confirmation:

```text
POST /admin/realms/{realm}/terms-and-conditions/publish
```

While the request is running:

```text
disable Publish button
show loading state
```

On success:

```text
update version
update publishedAt
show success notification
```

Example:

```text
Terms & Conditions version 5 published successfully.
```

On failure:

```text
keep existing displayed state
re-enable button
show Keycloak-style error notification
```

Do not optimistically update the published version before the server returns success.

---

# 18. Double-click protection

The UI must prevent accidental duplicate publication.

While POST `/publish` is pending:

```text
Publish button disabled
modal confirmation disabled
```

This is UI protection only.

Server-side concurrency handling is still required because multiple administrators may publish concurrently.

---

# 19. UI permissions

If the administrator can view but not manage the realm:

display the current T&C publication metadata but either hide or disable the publication button.

The server MUST independently enforce permission regardless of the UI.

Do not treat hidden UI controls as authorization.

---

# 20. Frontend code structure

Suggested layout:

```text
admin-ui/
  src/
    terms/
      TermsPage.tsx
      TermsPublishDialog.tsx
      termsApi.ts
      types.ts
```

Example models:

```ts
export interface TermsStatus {
  version: number;
  publishedAt: string | null;
}

export interface TermsPublicationResult {
  version: number;
  publishedAt: string;
}
```

API layer:

```ts
getTermsStatus(realm: string): Promise<TermsStatus>

publishTerms(realm: string): Promise<TermsPublicationResult>
```

Do not embed REST calls throughout React components.

---

# 21. Backend code structure

Suggested Maven module structure:

```text
keycloak-terms-extension/
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   │   └── ...
    │   │       ├── TermsAdminResourceProvider.java
    │   │       ├── TermsAdminResourceProviderFactory.java
    │   │       ├── TermsAdminResource.java
    │   │       ├── TermsService.java
    │   │       ├── TermsStatus.java
    │   │       └── TermsPublicationResult.java
    │   └── resources
    │       └── META-INF
    │           └── services
    │               └── org.keycloak.services.resources.admin.ext.AdminRealmResourceProviderFactory
    └── test
```

The SPI service registration file must contain the concrete factory class.

Example:

```text
com.example.keycloak.terms.TermsAdminResourceProviderFactory
```

---

# 22. Factory

Factory provider ID:

```java
public static final String ID = "terms-and-conditions";
```

Conceptually:

```java
public final class TermsAdminResourceProviderFactory
        implements AdminRealmResourceProviderFactory {

    @Override
    public String getId() {
        return "terms-and-conditions";
    }

    // implement create/init/postInit/close as required
}
```

Use Keycloak 26.4.7 API signatures exactly.

---

# 23. Provider

Implement:

```java
AdminRealmResourceProvider
```

The provider receives:

```java
KeycloakSession
RealmModel
AdminPermissionEvaluator
AdminEventBuilder
```

from Keycloak. This is explicitly part of the 26.4-era Admin extension API.

The provider should return the JAX-RS resource implementing:

```text
GET
POST /publish
```

Do not put business logic into the provider itself.

---

# 24. JAX-RS resource

Conceptual structure:

```java
public final class TermsAdminResource {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private final TermsService service;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public TermsStatus getStatus() {
        // require view permission
        // return service.getStatus(...)
    }

    @POST
    @Path("publish")
    @Produces(MediaType.APPLICATION_JSON)
    public TermsPublicationResult publish() {
        // require manage permission
        // call service.publish(...)
        // generate admin event
        // return result
    }
}
```

Use Jakarta namespaces appropriate to Keycloak 26:

```java
jakarta.ws.rs.*
```

not legacy:

```java
javax.ws.rs.*
```

---

# 25. Error handling

Use predictable HTTP semantics.

### Invalid existing version

If:

```text
terms.version = abc
```

return:

```text
500 Internal Server Error
```

with a meaningful server log entry.

Do not overwrite it.

### Forbidden

```text
403 Forbidden
```

### Realm not found

Allow normal Keycloak handling to return:

```text
404 Not Found
```

### Successful publication

```text
200 OK
```

Do not leak:

```text
stack traces
internal Java class names
tokens
user data
```

to the browser.

---

# 26. Logging

Log:

```text
realm name
old version
new version
number of users processed
operation duration
success/failure
```

Do not log:

```text
terms HTML
access tokens
user credentials
personal user attributes
```

Example:

```text
Published T&C realm=customer-a oldVersion=3 newVersion=4 usersUpdated=1542
```

---

# 27. Deployment

Backend extension output:

```text
keycloak-terms-extension.jar
```

Admin Console/theme output:

```text
custom Keycloak admin theme JAR
```

Depending on the chosen build layout these may be packaged together or separately.

Prefer a single deployable artifact if this remains simple and does not complicate frontend build tooling.

Deployment follows Keycloak's provider mechanism:

```text
/opt/keycloak/providers/
```

then:

```text
kc.sh build
```

where required by the deployment model.

The Admin Console customization mechanism itself supports packaging the custom console as a JAR for deployment to Keycloak.

---

# 28. Maven constraints

Pin all Keycloak dependencies to the target version.

Example:

```xml
<keycloak.version>26.4.7</keycloak.version>
```

Do not use:

```text
latest
26+
version ranges
```

Keycloak server dependencies should normally have:

```xml
<scope>provided</scope>
```

to avoid packaging duplicate Keycloak server classes into the extension JAR.

---

# 29. Frontend dependency constraints

Pin the Keycloak UI package to a version compatible with the target Keycloak server.

Do not use an unconstrained dependency such as:

```json
"@keycloak/keycloak-admin-ui": "*"
```

Use the 26.4-compatible package version determined from the Keycloak 26.4.7 distribution/build metadata.

Avoid depending directly on internal Admin Console source files where an exported API/component exists.

---

# 30. Explicit non-goals

The initial implementation does NOT need to provide:

```text
Terms HTML editor
Terms history
Terms diffing
rollback
per-user acceptance history
database schema changes
asynchronous jobs
email notifications
scheduled publication
multiple simultaneous active versions
language-specific publication versions
```

The feature provides:

```text
publication metadata
publication action
required-action propagation
```

only.

---

# 31. Important distinction: publication vs editing

Editing:

```text
termsText
```

does NOT automatically constitute publication.

The administrator may edit the Terms text multiple times.

Only pressing:

```text
Publish new version
```

causes:

```text
terms.version++
terms.publishedAt = now
TERMS_AND_CONDITIONS assigned to users
```

This distinction is intentional.

---

# 32. Behaviour for newly created users

Do NOT automatically add `TERMS_AND_CONDITIONS` to every future user from this publishing operation.

New-user behaviour should continue to follow the realm's configured Keycloak authentication/required-action policy.

This extension's publication action operates on users existing at publication time.

If later requirements demand explicit enforcement for newly created users, address that separately.

---

# 33. Tests

## Backend unit/integration tests

At minimum cover:

### First publication

Initial:

```text
terms.version = missing
terms.publishedAt = missing
```

After publication:

```text
terms.version = 1
terms.publishedAt != null
```

### Subsequent publication

Initial:

```text
terms.version = 4
```

After:

```text
terms.version = 5
```

### Required action

Given existing user:

```text
requiredActions = []
```

After publication:

```text
requiredActions contains TERMS_AND_CONDITIONS
```

### Existing required action

Given:

```text
requiredActions contains TERMS_AND_CONDITIONS
```

Publishing must not fail and must not create invalid duplicate state.

### Permissions

Viewer:

```text
GET -> allowed
POST -> forbidden
```

Realm manager:

```text
GET -> allowed
POST -> allowed
```

### Invalid version

Given:

```text
terms.version = "banana"
```

Publishing fails and does not silently overwrite the value.

### Timestamp

Returned and stored timestamp must:

```text
be UTC
be valid ISO-8601
originate server-side
```

### Multi-realm isolation

Publishing realm A must not modify:

```text
realm B attributes
realm B users
```

This test is mandatory.

---

# 34. Frontend tests

Cover:

```text
page displays current version
page displays publication time
never-published state works
publish button opens confirmation dialog
cancel does not call backend
confirm calls backend exactly once
button disabled during request
success updates UI
failure leaves current version unchanged
error notification displayed
```

---

# 35. Manual acceptance scenario

Create realm:

```text
demo
```

Create users:

```text
alice
bob
```

Ensure neither has:

```text
TERMS_AND_CONDITIONS
```

Open:

```text
Terms & Conditions
```

Expected:

```text
Not published
```

Press:

```text
Publish first version
```

Confirm.

Expected UI:

```text
Current version: 1
Published at: <current timestamp>
```

Verify realm attributes:

```text
terms.version = 1
terms.publishedAt = <same timestamp>
```

Verify:

```text
alice -> TERMS_AND_CONDITIONS
bob   -> TERMS_AND_CONDITIONS
```

Authenticate as Alice.

Alice must be presented with Keycloak's Terms & Conditions required action.

Accept.

Alice can log in normally.

Publish again.

Expected:

```text
terms.version = 2
```

Alice must once again receive:

```text
TERMS_AND_CONDITIONS
```

at her next applicable authentication.

---

# 36. Implementation priorities

Implement in this order:

```text
1. Backend AdminRealmResourceProvider skeleton

2. GET terms status

3. POST publish updating realm attributes only

4. Backend tests for versioning and permissions

5. Required-action propagation

6. Admin event

7. Custom Admin Console page

8. Confirmation dialog

9. UI error/loading states

10. End-to-end tests
```

Do not begin with the React UI.

Prove the server API and authorization model first.

---

# 37. Architectural constraints

Codex should follow these rules during implementation:

1. Do not patch Keycloak source code.

2. Do not fork the standard Admin Console.

3. Use the supported/custom-console package:
   ```text
   @keycloak/keycloak-admin-ui
   ```

4. Use:
   ```text
   AdminRealmResourceProvider
   ```
   for the server mutation API.

5. Do not expose an unauthenticated realm endpoint.

6. All authoritative state changes happen server-side.

7. Reuse Keycloak's built-in:
   ```text
   TERMS_AND_CONDITIONS
   ```
   required action.

8. Store metadata as realm attributes:
   ```text
   terms.version
   terms.publishedAt
   ```

9. Do not introduce a custom database schema for the MVP.

10. Keep Keycloak dependencies pinned to `26.4.7`.

11. Prefer exported/stable APIs over internal implementation classes.

12. If an API shown in this specification differs slightly in Keycloak 26.4.7, inspect the 26.4.7 API/source and adapt to that version rather than upgrading the dependency.

---

# 38. Note about UiTabProvider

Keycloak also contains:

```text
UiTabProvider
UiTabProviderFactory
UiPageProvider
UiPageProviderFactory
```

APIs in this version family. They can appear attractive for adding a small piece of UI directly to the existing console.

Do NOT make them the primary implementation approach for this task.

They have historically been marked experimental and had Admin UI regressions; for example, Keycloak 26.3 release notes include a fix for custom `UiTabProvider` tabs not being displayed since 26.2.0.

The chosen implementation should therefore use the composable/custom Admin Console mechanism unless investigation of the exact 26.4.7 environment provides a compelling reason otherwise.

---

# 39. Definition of Done

The implementation is complete when:

```text
✓ Keycloak 26.4.7 starts successfully with the extension installed

✓ Admin Console contains a Terms & Conditions page

✓ Page shows current version and published timestamp

✓ Realm viewer can read the information

✓ Only an administrator with realm-management permission can publish

✓ Publishing increments terms.version

✓ Publishing sets terms.publishedAt using server UTC time

✓ Publishing assigns TERMS_AND_CONDITIONS to existing realm users

✓ Users in other realms are untouched

✓ Publication generates a Keycloak admin event

✓ UI prevents accidental double submission

✓ Errors are displayed using Admin Console conventions

✓ No Keycloak source patch is required

✓ No custom database schema is introduced

✓ Automated backend tests cover publication and authorization

✓ Automated frontend tests cover the basic workflow

✓ Build artifacts are deployable to a stock Keycloak 26.4.7 installation
```