package com.aruba.simpl.keycloak.terms;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/** Registers the Terms hash trigger with Keycloak's required-action SPI. */
public final class TermsHashRequiredActionFactory implements RequiredActionFactory {

    public static final String ID = "terms-hash-trigger";

    private static final Logger LOG = Logger.getLogger(TermsHashRequiredActionFactory.class);

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        LOG.debugf("Creating Terms hash required-action provider '%s'", ID);
        return new TermsHashRequiredAction();
    }

    @Override
    public String getDisplayText() {
        LOG.trace("Returning Terms hash required-action display text");
        return "Terms hash trigger";
    }

    @Override
    public boolean isConfigurable() {
        LOG.trace("Terms hash required-action provider is not configurable");
        return false;
    }

    @Override
    public void init(Config.Scope config) {
        // No provider configuration is required.
        LOG.debugf("Initializing Terms hash required-action provider '%s'", ID);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post-initialization is required.
        LOG.debugf("Post-initialized Terms hash required-action provider '%s'", ID);
    }

    @Override
    public void close() {
        // No resources are held by the factory.
        LOG.debugf("Closing Terms hash required-action provider factory '%s'", ID);
    }

    @Override
    public String getId() {
        LOG.tracef("Returning Terms hash required-action provider ID '%s'", ID);
        return ID;
    }
}
