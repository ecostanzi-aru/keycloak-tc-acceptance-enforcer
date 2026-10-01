# Keycloak Terms & Conditions Hash Enforcement

## Target

Keycloak **26.4.7**.

Continue using Keycloak's built-in `TERMS_AND_CONDITIONS` required action and existing `termsText` realm-localization mechanism. Calculate the current hash on demand from all localized `termsText` overrides; do not persist a derived `terms.currentHash` realm attribute.

The calculation sorts the values by locale and uses an unambiguous, length-prefixed UTF-8 representation before applying SHA-256. Changing any localized Terms value therefore changes the global current hash, while localization map iteration order and unrelated message keys do not.

The implementation must add two Keycloak SPIs.

---

## 1. Terms Acceptance Event Listener

Implement an `EventListenerProvider` that records which Terms version the user accepted.

When the built-in `TERMS_AND_CONDITIONS` required action completes successfully:

1. Detect the successful `CUSTOM_REQUIRED_ACTION` event.
2. Verify that `Details.CUSTOM_REQUIRED_ACTION` identifies `TERMS_AND_CONDITIONS`.
3. Calculate the current hash from the realm's localized `termsText` overrides.

```text
currentHash = SHA-256(all localized termsText values, sorted by locale)
```

4. Store that value on the user as:

```text
terms.acceptedHash
```

Result:

```text
User:
  terms.acceptedHash = abc123
```

Do not replace or modify Keycloak's built-in Terms and Conditions provider.

Keycloak should continue managing its existing:

```text
terms_and_conditions = <acceptance timestamp>
```

attribute.

The custom listener only adds `terms.acceptedHash`. If no localized `termsText` override exists, do not write an empty `terms.acceptedHash`; log a warning instead.

---

## 2. Terms Hash Required Action Trigger

Implement a custom `RequiredActionProvider`.

Its purpose is only to implement `evaluateTriggers()`.

On every authentication:

```text
currentHash  = SHA-256(realm localized termsText values)
acceptedHash = user["terms.acceptedHash"]
```

If:

```text
currentHash != null
AND
acceptedHash != currentHash
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

If no localized `termsText` override is configured, do nothing and log/debug appropriately. Do not block authentication because the Terms content has not yet been configured.

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
        AAA != hash(localized termsText)
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
            +--> EventListener stores the live termsText hash
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

localized termsText missing
→ authentication is not blocked

successful TERMS_AND_CONDITIONS
→ acceptedHash becomes currentHash

other successful required actions
→ acceptedHash is not modified

rejected/cancelled TERMS_AND_CONDITIONS
→ acceptedHash is not modified
```

---

# Phase 2 — Language-aware Terms Acceptance

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

Whether changing language alone should require reacceptance should be configurable/business-defined.

Default recommendation:

```text
If the translated Terms content has a different hash:
    require acceptance

If the user changes language but both languages represent the same accepted revision:
    do not automatically require acceptance solely because the locale changed
```

Keep Phase 1 independent from this language-aware extension so it can be implemented and tested first.
