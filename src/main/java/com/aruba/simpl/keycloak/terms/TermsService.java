package com.aruba.simpl.keycloak.terms;

import org.keycloak.models.RealmModel;

/** Encapsulates realm-level terms metadata independently from the JAX-RS resource. */
public final class TermsService {

    static final String VERSION_ATTRIBUTE = "terms.version";
    static final String PUBLISHED_AT_ATTRIBUTE = "terms.publishedAt";

    public TermsStatus getStatus(RealmModel realm) {
        return new TermsStatus(parseCurrentVersion(realm.getAttribute(VERSION_ATTRIBUTE)),
                realm.getAttribute(PUBLISHED_AT_ATTRIBUTE));
    }

    static int parseCurrentVersion(String value) {
        if (value == null) {
            return 0;
        }

        try {
            int version = Integer.parseInt(value);
            if (version < 0) {
                throw new IllegalArgumentException("Realm attribute terms.version must be a non-negative integer");
            }
            return version;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Realm attribute terms.version must be a non-negative integer", exception);
        }
    }

    public TermsStatus publish(org.keycloak.models.KeycloakSession session, RealmModel realm) {
        int currentVersion = parseCurrentVersion(realm.getAttribute(VERSION_ATTRIBUTE));
        if (currentVersion == Integer.MAX_VALUE) {
            throw new IllegalStateException("Realm attribute terms.version cannot be incremented further");
        }
        int version = currentVersion + 1;
        String publishedAt = java.time.Instant.now().toString();
        realm.setAttribute(VERSION_ATTRIBUTE, Integer.toString(version));
        realm.setAttribute(PUBLISHED_AT_ATTRIBUTE, publishedAt);
        try (var users = session.users().searchForUserStream(realm, java.util.Map.of())) {
            users.forEach(user -> user.addRequiredAction(
                    org.keycloak.models.UserModel.RequiredAction.TERMS_AND_CONDITIONS));
        }
        return new TermsStatus(version, publishedAt);
    }
}
