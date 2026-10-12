package com.aruba.simpl.keycloak.terms;

import java.util.Objects;

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
        TermsHashAttributes.CurrentTerms currentTerms = TermsHashAttributes.resolveCurrentTerms(
                context.getSession(), context.getRealm(), context.getUser());
        if (currentTerms == null) {
            LOG.debugf("Terms hash trigger skipped for user '%s' in realm '%s': current Terms could not be resolved",
                    context.getUser().getId(), context.getRealm().getName());
            return;
        }

        String acceptedHash = context.getUser().getFirstAttribute(TermsHashAttributes.ACCEPTED_HASH);
        String acceptedLanguage = context.getUser().getFirstAttribute(TermsHashAttributes.ACCEPTED_LANGUAGE);
        boolean hashChanged = !currentTerms.hash().equals(acceptedHash);
        boolean languageChanged = !Objects.equals(currentTerms.language(), acceptedLanguage);
        boolean requireAcceptanceOnLanguageChange =
                TermsHashRequiredActionFactory.requireAcceptanceOnLanguageChange(context.getConfig());

        LOG.debugf("Terms acceptance state for user '%s' in realm '%s': acceptedHash='%s', currentHash='%s', acceptedLanguage='%s', currentLanguage='%s', requireOnLanguageChange=%s",
                context.getUser().getId(), context.getRealm().getName(),
                TermsHashAttributes.abbreviateHash(acceptedHash),
                TermsHashAttributes.abbreviateHash(currentTerms.hash()),
                acceptedLanguage, currentTerms.language(), requireAcceptanceOnLanguageChange);
        if (hashChanged || (requireAcceptanceOnLanguageChange && languageChanged)) {
            LOG.debugf("Terms acceptance is outdated for user '%s' in realm '%s' (hashChanged=%s, languageChanged=%s); adding %s",
                    context.getUser().getId(), context.getRealm().getName(),
                    hashChanged, languageChanged,
                    UserModel.RequiredAction.TERMS_AND_CONDITIONS);
            context.getUser().addRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS);
            return;
        }

        LOG.debugf("Terms acceptance is current for user '%s' in realm '%s' (hash='%s', language='%s'); no action added",
                context.getUser().getId(), context.getRealm().getName(),
                TermsHashAttributes.abbreviateHash(currentTerms.hash()), currentTerms.language());
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
