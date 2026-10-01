package com.aruba.simpl.keycloak.terms;

import org.jboss.logging.Logger;

/** JSON response returned by the terms-and-conditions administration resource. */
public record TermsStatus(int version, String publishedAt) {

    private static final Logger LOG = Logger.getLogger(TermsStatus.class);

    public TermsStatus {
        LOG.debugf("Created terms status: version=%d, publishedAt='%s'", version, publishedAt);
    }
}
