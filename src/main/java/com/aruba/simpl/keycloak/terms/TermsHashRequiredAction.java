package com.aruba.simpl.keycloak.terms;

import org.jboss.logging.Logger;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.UserModel;

/** Adds Keycloak's built-in Terms action when the user's accepted hash is out of date. */
public final class TermsHashRequiredAction implements RequiredActionProvider {

    private static final Logger LOG = Logger.getLogger(TermsHashRequiredAction.class);

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        LOG.debugf("Evaluating Terms hash trigger for user '%s' in realm '%s'",
                context.getUser().getId(), context.getRealm().getName());
        String currentHash = context.getRealm().getAttribute(TermsHashAttributes.CURRENT_HASH);
        if (currentHash == null) {
            LOG.debugf("Terms hash trigger skipped for realm '%s': %s is not configured",
                    context.getRealm().getName(), TermsHashAttributes.CURRENT_HASH);
            return;
        }
        if (currentHash.isBlank()) {
            LOG.debugf("Terms hash trigger skipped for realm '%s': %s is blank",
                    context.getRealm().getName(), TermsHashAttributes.CURRENT_HASH);
            return;
        }

        
        String acceptedHash = context.getUser().getFirstAttribute(TermsHashAttributes.ACCEPTED_HASH);
        LOG.debugf("Accepted hash is %s, current hash is %s", currentHash, acceptedHash);
        if (!currentHash.equals(acceptedHash)) {
            LOG.debugf("Terms hash mismatch for user '%s' in realm '%s' (accepted='%s', current='%s'); adding %s",
                    context.getUser().getId(), context.getRealm().getName(),
                    TermsHashAttributes.abbreviateHash(acceptedHash), TermsHashAttributes.abbreviateHash(currentHash),
                    UserModel.RequiredAction.TERMS_AND_CONDITIONS);
            context.getUser().addRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS);
            return;
        }

        LOG.debugf("Terms hash is current for user '%s' in realm '%s' (hash='%s'); no action added",
                context.getUser().getId(), context.getRealm().getName(),
                TermsHashAttributes.abbreviateHash(currentHash));
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        // This provider is only an evaluateTriggers hook and must never render a page.
        LOG.warnf("Terms hash trigger was invoked as a required-action challenge for user '%s'; completing it",
                context.getUser().getId());
        context.success();
        LOG.debugf("Completed unexpected Terms hash challenge for user '%s'", context.getUser().getId());
    }

    @Override
    public void processAction(RequiredActionContext context) {
        // This provider is only an evaluateTriggers hook and must never process a form.
        LOG.warnf("Terms hash trigger was invoked to process a required-action form for user '%s'; completing it",
                context.getUser().getId());
        context.success();
        LOG.debugf("Completed unexpected Terms hash form processing for user '%s'", context.getUser().getId());
    }

    @Override
    public void close() {
        // No resources are held by this stateless provider.
        LOG.debug("Closing Terms hash required-action provider");
    }
}
