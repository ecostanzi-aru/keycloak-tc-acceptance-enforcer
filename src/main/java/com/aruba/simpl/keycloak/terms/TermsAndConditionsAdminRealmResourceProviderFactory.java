package com.aruba.simpl.keycloak.terms;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProviderFactory;

public final class TermsAndConditionsAdminRealmResourceProviderFactory
        implements AdminRealmResourceProviderFactory {

    public static final String ID = "terms-and-conditions";

    private static final Logger LOG = Logger.getLogger(TermsAndConditionsAdminRealmResourceProviderFactory.class);

    @Override
    public AdminRealmResourceProvider create(KeycloakSession session) {
        LOG.tracef("Creating admin realm resource provider '%s'", ID);
        return new TermsAndConditionsAdminRealmResourceProvider();
    }

    @Override
    public void init(Config.Scope config) {
        // No provider configuration is required.
        LOG.tracef("Initializing admin realm resource provider '%s'", ID);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post-initialization is required.
        LOG.tracef("Post-initialized admin realm resource provider '%s'", ID);
    }

    @Override
    public void close() {
        // No resources are held by the factory.
        LOG.tracef("Closing admin realm resource provider factory '%s'", ID);
    }

    @Override
    public String getId() {
        LOG.tracef("Returning admin realm resource provider ID '%s'", ID);
        return ID;
    }
}
