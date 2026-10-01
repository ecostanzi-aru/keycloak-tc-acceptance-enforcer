package com.aruba.simpl.keycloak.terms;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

public final class TermsAndConditionsAdminRealmResourceProvider implements AdminRealmResourceProvider {

    private static final Logger LOG = Logger.getLogger(TermsAndConditionsAdminRealmResourceProvider.class);

    @Override
    public Object getResource(KeycloakSession session, RealmModel realm,
                              AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        LOG.tracef("Creating terms-and-conditions admin resource for realm '%s'", realm.getName());
        return new TermsAndConditionsResource(session, realm, auth, adminEvent, new TermsService());
    }

    @Override
    public void close() {
        // The request-scoped provider does not own resources.
        LOG.debug("Closing terms-and-conditions admin realm resource provider");
    }
}
