package com.aruba.simpl.keycloak.terms;

import org.jboss.logging.Logger;

/** Realm and user attributes used to track the accepted Terms revision. */
final class TermsHashAttributes {

    private static final Logger LOG = Logger.getLogger(TermsHashAttributes.class);

    static final String CURRENT_HASH = "terms.currentHash";
    static final String ACCEPTED_HASH = "terms.acceptedHash";

    static String abbreviateHash(String hash) {
        if (hash == null) {
            LOG.debug("Representing a missing Terms hash");
            return "<missing>";
        }
        if (hash.length() <= 12) {
            LOG.debugf("Terms hash is already short enough to log safely (length=%d)", hash.length());
            return hash;
        }
        LOG.debugf("Abbreviating Terms hash for logging (length=%d)", hash.length());
        return hash.substring(0, 12) + "…";
    }

    private TermsHashAttributes() {
        LOG.debug("Constructed Terms hash attributes utility");
    }
}
