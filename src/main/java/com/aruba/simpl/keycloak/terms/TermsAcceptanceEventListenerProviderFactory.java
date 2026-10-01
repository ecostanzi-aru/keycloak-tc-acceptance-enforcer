package com.aruba.simpl.keycloak.terms;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/** Factory for the listener that persists successful Terms acceptances. */
public final class TermsAcceptanceEventListenerProviderFactory implements EventListenerProviderFactory {

    public static final String ID = "terms-hash-acceptance";

    private static final Logger LOG = Logger.getLogger(TermsAcceptanceEventListenerProviderFactory.class);

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        LOG.debugf("Creating Terms acceptance event listener provider '%s'", ID);
        return new TermsAcceptanceEventListenerProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        // No provider configuration is required.
        LOG.debugf("Initializing Terms acceptance event listener provider '%s'", ID);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post-initialization is required.
        LOG.debugf("Post-initialized Terms acceptance event listener provider '%s'", ID);
    }

    @Override
    public void close() {
        // No resources are held by the factory.
        LOG.debugf("Closing Terms acceptance event listener provider factory '%s'", ID);
    }

    @Override
    public String getId() {
        return ID;
    }
}
