# Keycloak Terms & Conditions Administration Extension

A Keycloak 26.4.7 server-side extension for publishing Terms & Conditions versions. It stores publication metadata as realm attributes, requires realm-management permissions, marks existing users with Keycloak's built-in `TERMS_AND_CONDITIONS` required action, and records an admin event.

No custom Admin Console UI is included yet.

> [!IMPORTANT]
> This project currently contains two detached experiments: Terms version publication and Terms hash enforcement. The login hash flow calculates its state directly from localized `termsText` values and does not read `terms.version` or `terms.publishedAt`. Likewise, no login or registration flow uses the publication version metadata. The publication API currently only maintains experimental metadata and, when called, assigns Keycloak's built-in `TERMS_AND_CONDITIONS` required action to existing users. Do not treat the publication version as the Terms revision accepted by a user.

## Prerequisites

- JDK 17
- No Maven installation is required: use the included Maven wrapper (`./mvnw`).
- Docker with the Docker Compose plugin

## Run locally

Build the provider JAR, then start Keycloak:

```bash
./mvnw clean package
docker compose up -d
```

The first start downloads the Keycloak image and performs its provider build, so it can take a little longer.

Open the Admin Console at [http://localhost:8080/admin](http://localhost:8080/admin).

| Setting | Value |
| --- | --- |
| Admin username | `admin` |
| Admin password | `admin` |
| Keycloak version | `26.4.7` |

The Compose configuration mounts `target/keycloak-terms-and-conditions-1.0.0-SNAPSHOT.jar` into Keycloak's `/opt/keycloak/providers` directory. Rebuild the JAR and restart the container after changing provider code:

```bash
./mvnw package
docker compose restart keycloak
```

Follow startup logs with:

```bash
docker compose logs -f keycloak
```

## Enable Terms hash enforcement

Define at least one `termsText` realm localization override. In **Realm settings → Events → Event listeners**, add `terms-hash-acceptance`. In **Authentication → Required actions**, enable `Terms hash trigger` (provider ID `terms-hash-trigger`).

At each login the trigger calculates a deterministic SHA-256 hash from every localized `termsText` realm override, sorted by locale. It compares that live hash with the user's `terms.acceptedHash` and adds Keycloak's built-in `TERMS_AND_CONDITIONS` action when they differ. Consequently, changing any localized `termsText` value automatically requires a new acceptance; no `terms.currentHash` realm attribute or startup synchronization is needed.

Once the user accepts the built-in Terms page, the event listener calculates the same live hash and records it in `terms.acceptedHash`. The built-in `terms_and_conditions` timestamp remains managed by Keycloak. If no localized `termsText` override exists, hash enforcement is skipped.

### Record acceptance during registration

To store the hash when a user accepts Terms on the registration form, copy the built-in **Registration** flow and add these required executions to its registration form subflow in this order:

1. **Registration User Profile Creation**
2. **Terms and conditions**
3. **Record Terms acceptance hash** (provider ID `registration-terms-hash`)

Bind the copied flow as the realm's registration flow. The hash recorder does not render or validate a checkbox; it records `terms.acceptedHash` only when Keycloak's built-in `terms_and_conditions` acceptance attribute is present on the newly created user. If no localized `termsText` exists, it leaves the hash unset.

## Test the API

Create a test realm in the Admin Console, for example `acme`. Obtain an admin access token (replace credentials if you changed them):

```bash
TOKEN=$(curl --silent --show-error --fail \
  --data 'grant_type=password' \
  --data 'client_id=admin-cli' \
  --data 'username=admin' \
  --data 'password=admin' \
  http://localhost:8080/realms/master/protocol/openid-connect/token \
  | jq -r '.access_token')
```

Read the publication status:

```bash
curl --silent --show-error --fail \
  --header "Authorization: Bearer $TOKEN" \
  http://localhost:8080/admin/realms/master/terms-and-conditions
```

Publish a new version:

```bash
curl --silent --show-error --fail \
  --request POST \
  --header "Authorization: Bearer $TOKEN" \
  http://localhost:8080/admin/realms/master/terms-and-conditions/publish
```

The first publication returns version `1`. Subsequent calls increment it. The caller needs the realm's `view-realm` permission for `GET` and `manage-realm` for `POST`.

## Stop and reset

Stop the local instance while retaining data:

```bash
docker compose down
```
