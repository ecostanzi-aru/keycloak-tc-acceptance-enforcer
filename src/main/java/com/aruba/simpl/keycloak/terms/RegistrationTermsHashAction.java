package com.aruba.simpl.keycloak.terms;

import org.jboss.logging.Logger;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.authentication.requiredactions.TermsAndConditions;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/** Records the current Terms hash for a user created after accepting Terms during registration. */
public final class RegistrationTermsHashAction implements FormAction {

    private static final Logger LOG = Logger.getLogger(RegistrationTermsHashAction.class);

    @Override
    public void buildPage(FormContext context, LoginFormsProvider form) {
        // This action does not add fields to the registration page.
    }

    @Override
    public void validate(ValidationContext context) {
        // Keycloak's built-in registration Terms action owns checkbox validation.
        context.success();
    }

    @Override
    public void success(FormContext context) {
        UserModel user = context.getUser();
        if (user == null) {
            LOG.warn("Cannot record the Terms hash during registration: no user has been created; "
                    + "place this execution after Registration User Profile Creation");
            return;
        }

        String acceptedAt = user.getFirstAttribute(TermsAndConditions.USER_ATTRIBUTE);
        if (acceptedAt == null) {
            LOG.debugf("Not recording a Terms hash for user '%s': Terms were not accepted during registration",
                    user.getId());
            return;
        }

        String currentHash = TermsHashAttributes.calculateCurrentHash(context.getRealm());
        if (currentHash == null) {
            LOG.warnf("Cannot record Terms acceptance for user '%s' in realm '%s': no localized %s value is configured",
                    user.getId(), context.getRealm().getName(), TermsHashAttributes.TERMS_TEXT_KEY);
            return;
        }

        user.setSingleAttribute(TermsHashAttributes.ACCEPTED_HASH, currentHash);
        LOG.debugf("Recorded registration Terms hash '%s' for user '%s' in realm '%s'",
                TermsHashAttributes.abbreviateHash(currentHash), user.getId(), context.getRealm().getName());
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // This action is used only while processing the registration form.
    }

    @Override
    public void close() {
        // This stateless action owns no resources.
    }
}
