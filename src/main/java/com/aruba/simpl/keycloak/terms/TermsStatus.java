package com.aruba.simpl.keycloak.terms;

/** JSON response returned by the terms-and-conditions administration resource. */
public record TermsStatus(int version, String publishedAt) {
}
