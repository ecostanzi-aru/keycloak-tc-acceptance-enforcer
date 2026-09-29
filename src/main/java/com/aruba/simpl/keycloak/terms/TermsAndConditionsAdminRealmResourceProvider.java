package com.aruba.simpl.keycloak.terms;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

public final class TermsAndConditionsAdminRealmResourceProvider implements AdminRealmResourceProvider {

    @Override
    public Object getResource(KeycloakSession session, RealmModel realm,
                              AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        return new TermsAndConditionsResource(session, realm, auth, adminEvent, new TermsService());
    }

    @Override
    public void close() {
        // The request-scoped provider does not own resources.
    }
}
