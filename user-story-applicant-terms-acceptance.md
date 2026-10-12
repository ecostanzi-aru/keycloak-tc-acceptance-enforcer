# Applicant accepts current Terms and Conditions

**Traceability:** Parent Epic: _TBD during refinement_  
**Functional objective:** Ensure that an applicant reviews and accepts the Terms and Conditions that apply to their registration, login, and selected language.

## Terms acceptance decision table

On registration or login, Terms and Conditions are requested when their resolved content hash has changed. A language-only change requests Terms and Conditions only when `requireAcceptanceOnLanguageChange` is enabled. The setting defaults to `false`.

| `requireAcceptanceOnLanguageChange` | Hash changed | Language changed | Outcome |
| --- | --- | --- | --- |
| `false` | `false` | `false` | Terms and Conditions not requested |
| `false` | `false` | `true` | Terms and Conditions not requested |
| `false` | `true` | `false` | Terms and Conditions requested |
| `false` | `true` | `true` | Terms and Conditions requested |
| `true` | `false` | `false` | Terms and Conditions not requested |
| `true` | `false` | `true` | Terms and Conditions requested |
| `true` | `true` | `false` | Terms and Conditions requested |
| `true` | `true` | `true` | Terms and Conditions requested |

---

**As a**

applicant

**I want to**

review and accept Terms and Conditions whenever the Terms that apply to me are new or changed

**So that**

I can make an informed decision before registering or accessing the service.

**Acceptance criteria:**

1. **GIVEN** Terms and Conditions have been configured  
   **AND** I am registering for the first time  
   **WHEN** I reach the Terms and Conditions step of registration  
   **THEN** I see the configured Terms and Conditions  
   **AND** I must accept them before I can complete registration.

2. **GIVEN** Terms and Conditions have been configured  
   **AND** I have previously accepted the current Terms and Conditions in my current language  
   **WHEN** I log in again using the same language  
   **THEN** I do not see the Terms and Conditions screen  
   **AND** I can continue with my login.

3. **GIVEN** Terms and Conditions have been configured  
   **AND** I have previously accepted them  
   **AND** the Terms and Conditions content that applies to me has changed  
   **WHEN** I next register or log in  
   **THEN** I see the updated Terms and Conditions screen  
   **AND** I must accept the updated Terms before I can continue.

4. **GIVEN** Terms and Conditions have been configured  
   **AND** I have previously accepted them in one language  
   **AND** the Terms content is equivalent in my newly selected language  
   **AND** the setting to require acceptance when the language changes is disabled  
   **WHEN** I log in using the newly selected language  
   **THEN** I do not see the Terms and Conditions screen  
   **AND** I can continue with my login.

5. **GIVEN** Terms and Conditions have been configured  
   **AND** I have previously accepted them in one language  
   **AND** the setting to require acceptance when the language changes is enabled  
   **WHEN** I log in using a different language  
   **THEN** I see the Terms and Conditions screen in the newly selected language  
   **AND** I must accept them before I can continue.

6. **GIVEN** Terms and Conditions have been configured  
   **AND** I am shown the Terms and Conditions screen during registration or login  
   **WHEN** I decline the Terms and Conditions  
   **THEN** I do not complete the registration or login flow  
   **AND** I am not recorded as having accepted the Terms and Conditions.

## Dependencies and references

- Terms and Conditions content is configured for the applicable login language.
- The `terms-hash-acceptance` event listener and `terms-hash-trigger` required action are enabled for the realm.
- The language-change setting is available on the Terms hash trigger required action.
- Related design: [tc-hash-spec.md](tc-hash-spec.md).
