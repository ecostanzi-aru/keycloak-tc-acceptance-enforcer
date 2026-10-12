package com.aruba.simpl.keycloak.terms;

import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

/** Registers the Terms hash recorder as a registration-flow execution. */
public final class RegistrationTermsHashActionFactory implements FormActionFactory {

    public static final String ID = "registration-terms-hash";

    private static final Logger LOG = Logger.getLogger(RegistrationTermsHashActionFactory.class);
    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public FormAction create(KeycloakSession session) {
        return new RegistrationTermsHashAction();
    }

    @Override
    public String getDisplayType() {
        return "Record Terms acceptance hash";
    }

    @Override
    public String getReferenceCategory() {
        return "terms-and-conditions";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES.clone();
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Records the current localized Terms hash after Terms are accepted during registration. "
                + "Place this execution after Registration User Profile Creation.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }

    @Override
    public void init(Config.Scope config) {
        LOG.debugf("Initializing registration Terms hash action '%s'", ID);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        LOG.debugf("Post-initialized registration Terms hash action '%s'", ID);
    }

    @Override
    public void close() {
        LOG.debugf("Closing registration Terms hash action factory '%s'", ID);
    }

    @Override
    public String getId() {
        return ID;
    }
}
