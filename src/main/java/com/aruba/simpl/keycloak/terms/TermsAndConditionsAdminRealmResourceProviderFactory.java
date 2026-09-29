package com.aruba.simpl.keycloak.terms;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProviderFactory;

public final class TermsAndConditionsAdminRealmResourceProviderFactory
        implements AdminRealmResourceProviderFactory {

    public static final String ID = "terms-and-conditions";

    @Override
    public AdminRealmResourceProvider create(KeycloakSession session) {
        return new TermsAndConditionsAdminRealmResourceProvider();
    }

    @Override
    public void init(Config.Scope config) {
        // No provider configuration is required.
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post-initialization is required.
    }

    @Override
    public void close() {
        // No resources are held by the factory.
    }

    @Override
    public String getId() {
        return ID;
    }
}
