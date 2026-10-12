package com.aruba.simpl.keycloak.terms;

import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

/** Registers the Terms hash trigger with Keycloak's required-action SPI. */
public final class TermsHashRequiredActionFactory implements RequiredActionFactory {

    public static final String ID = "terms-hash-trigger";
    public static final String REQUIRE_ACCEPTANCE_ON_LANGUAGE_CHANGE = "requireAcceptanceOnLanguageChange";

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
        LOG.trace("Terms hash required-action provider is configurable");
        return true;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        LOG.trace("Returning Terms hash required-action configuration metadata");
        return ProviderConfigurationBuilder.create()
                .property()
                .name(REQUIRE_ACCEPTANCE_ON_LANGUAGE_CHANGE)
                .label("Require acceptance when language changes")
                .helpText("Require a new Terms acceptance when the effective login language changes, even if the translated Terms text has the same hash.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(Boolean.FALSE)
                .add()
                .build();
    }

    static boolean requireAcceptanceOnLanguageChange(RequiredActionConfigModel config) {
        return config != null && Boolean.parseBoolean(
                config.getConfigValue(REQUIRE_ACCEPTANCE_ON_LANGUAGE_CHANGE, Boolean.FALSE.toString()));
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
