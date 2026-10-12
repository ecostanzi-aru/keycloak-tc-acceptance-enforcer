# Keycloak Terms & Conditions Hash Enforcement

## Target

Keycloak **26.4.7**.

Continue using Keycloak's built-in `TERMS_AND_CONDITIONS` required action and existing `termsText` theme/localization mechanism. Resolve the effective login locale and calculate the current hash on demand from the `termsText` that Keycloak renders for that locale; do not persist a derived `terms.currentHash` realm attribute.

The calculation uses a length-prefixed UTF-8 representation before applying SHA-256. The language is stored separately and is not part of the content hash, so identical text has the same hash in different languages.

The implementation must add two Keycloak SPIs.

---

## 1. Terms Acceptance Event Listener

Implement an `EventListenerProvider` that records which Terms version the user accepted.

When the built-in `TERMS_AND_CONDITIONS` required action completes successfully:

1. Detect the successful `CUSTOM_REQUIRED_ACTION` event.
2. Verify that `Details.CUSTOM_REQUIRED_ACTION` identifies `TERMS_AND_CONDITIONS`.
3. Resolve the effective login locale using Keycloak's `LocaleSelectorProvider`.
4. Resolve the effective `termsText` from the active login theme enhanced with realm localization overrides.
5. Calculate the hash of that resolved text.

```text
currentLanguage = effective login locale
currentHash = SHA-256(effective termsText)
```

6. Store both values on the user as:

```text
terms.acceptedHash
terms.acceptedLanguage
```

Result:

```text
User:
  terms.acceptedHash     = abc123
  terms.acceptedLanguage = it
```

Do not replace or modify Keycloak's built-in Terms and Conditions provider.

Keycloak should continue managing its existing:

```text
terms_and_conditions = <acceptance timestamp>
```

attribute.

The custom listener only adds `terms.acceptedHash` and `terms.acceptedLanguage`. If the effective `termsText` or locale cannot be resolved, do not write partial acceptance state; log a warning instead.

---

## 2. Terms Hash Required Action Trigger

Implement a custom `RequiredActionProvider`.

Its purpose is only to implement `evaluateTriggers()`.

On every authentication:

```text
currentLanguage = effective login locale
currentHash  = SHA-256(effective termsText for currentLanguage)
acceptedHash = user["terms.acceptedHash"]
acceptedLanguage = user["terms.acceptedLanguage"]
```

If:

```text
acceptedHash != currentHash
OR
(requireAcceptanceOnLanguageChange AND acceptedLanguage != currentLanguage)
```

add the built-in required action:

```java
user.addRequiredAction(
    UserModel.RequiredAction.TERMS_AND_CONDITIONS
);
```

This includes the case where:

```text
terms.acceptedHash == null
```

so users who have never accepted the current Terms must accept them.

If the effective locale or `termsText` cannot be resolved, do nothing and log/debug appropriately. Do not block authentication because the Terms content could not be loaded.

The custom required action does not render any UI and does not replace `TERMS_AND_CONDITIONS`; it only decides whether the built-in action needs to run.

Expected flow:

```text
User:
  terms.acceptedHash = AAA

          LOGIN
            |
            v
TermsHashRequiredAction.evaluateTriggers()
            |
        AAA != hash(effective localized termsText)
            |
            v
add TERMS_AND_CONDITIONS
            |
            v
Keycloak built-in Terms page
            |
         Accept
            |
            +--> Keycloak stores terms_and_conditions timestamp
            |
            +--> EventListener stores the live termsText hash and language
```

## Registration

Register both providers using the standard Keycloak SPI mechanism and service loader files.

The providers should be packaged as a normal Keycloak provider JAR and deployable under:

```text
/opt/keycloak/providers/
```

Follow normal Keycloak 26.4.7 provider build/deployment conventions.

## Tests

At minimum cover:

```text
acceptedHash == currentHash
→ TERMS_AND_CONDITIONS is not added

acceptedHash != currentHash
→ TERMS_AND_CONDITIONS is added

acceptedHash missing
→ TERMS_AND_CONDITIONS is added

effective locale or termsText missing
→ authentication is not blocked

successful TERMS_AND_CONDITIONS
→ acceptedHash and acceptedLanguage become current values

other successful required actions
→ acceptedHash is not modified

rejected/cancelled TERMS_AND_CONDITIONS
→ acceptedHash is not modified
```

---

# Phase 2 — Language-aware Terms Acceptance (implemented)

After the basic hash mechanism works, extend it to record the language of the Terms accepted by the user.

Add:

```text
terms.acceptedLanguage
```

to the user.

On successful Terms acceptance, store both:

```text
terms.acceptedHash     = <hash>
terms.acceptedLanguage = <locale used for the Terms page>
```

For example:

```text
terms.acceptedHash     = 98d823...
terms.acceptedLanguage = it
```

Do not simply use the user's permanent `locale` attribute without checking how Keycloak resolved the locale for the current authentication. The stored language should represent the language of the Terms actually presented during that acceptance.

### Preferred language-aware model

If different translations can change independently, evolve the on-demand global hash into on-demand hashes per locale:

```text
hash.en = SHA-256(termsText.en)
hash.it = SHA-256(termsText.it)
hash.de = SHA-256(termsText.de)
```

Then at authentication determine the effective Terms language and compare:

```text
currentLanguage = effective login/Terms locale
currentHash     = realm hash for currentLanguage

acceptedLanguage = user["terms.acceptedLanguage"]
acceptedHash     = user["terms.acceptedHash"]
```

Require `TERMS_AND_CONDITIONS` if:

```text
acceptedHash != currentHash
```

The acceptance event should then record:

```text
terms.acceptedHash     = currentHash
terms.acceptedLanguage = currentLanguage
```

Whether changing language alone requires reacceptance is configured per realm on the `Terms hash trigger` required action with:

```text
requireAcceptanceOnLanguageChange = false | true
```

Default recommendation:

```text
If the translated Terms content has a different hash:
    require acceptance

If the user changes language but both languages represent the same accepted revision:
    do not automatically require acceptance solely because the locale changed
```

The default is `false`. Content changes always require acceptance; language-only changes require acceptance only when this setting is enabled.
