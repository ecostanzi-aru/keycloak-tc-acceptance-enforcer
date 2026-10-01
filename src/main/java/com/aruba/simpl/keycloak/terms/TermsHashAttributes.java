package com.aruba.simpl.keycloak.terms;

/** Realm and user attributes used to track the accepted Terms revision. */
final class TermsHashAttributes {

    static final String CURRENT_HASH = "terms.currentHash";
    static final String ACCEPTED_HASH = "terms.acceptedHash";

    static String abbreviateHash(String hash) {
        if (hash == null) {
            return "<missing>";
        }
        if (hash.length() <= 12) {
            return hash;
        }
        return hash.substring(0, 12) + "…";
    }

    private TermsHashAttributes() {
    }
}
