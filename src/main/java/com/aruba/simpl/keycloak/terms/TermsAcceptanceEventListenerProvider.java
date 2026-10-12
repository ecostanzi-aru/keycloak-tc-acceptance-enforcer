package com.aruba.simpl.keycloak.terms;

import org.jboss.logging.Logger;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/** Records the realm's current Terms hash after Keycloak accepts its built-in Terms action. */
public final class TermsAcceptanceEventListenerProvider implements EventListenerProvider {

    private static final Logger LOG = Logger.getLogger(TermsAcceptanceEventListenerProvider.class);

    private final KeycloakSession session;

    TermsAcceptanceEventListenerProvider(KeycloakSession session) {
        this.session = session;
        LOG.debug("Constructed Terms acceptance event listener");
    }

    @Override
    public void onEvent(Event event) {
        if (event == null) {
            LOG.debug("Ignoring null user event");
            return;
        }
        if (event.getType() != EventType.CUSTOM_REQUIRED_ACTION) {
            LOG.debugf("Ignoring user event of type '%s' in realm '%s'", event.getType(), event.getRealmId());
            return;
        }
        if (event.getDetails() == null) {
            LOG.debugf("Ignoring custom required-action event without details in realm '%s'", event.getRealmId());
            return;
        }

        String action = event.getDetails().get(Details.CUSTOM_REQUIRED_ACTION);
        if (!UserModel.RequiredAction.TERMS_AND_CONDITIONS.name().equals(action)) {
            LOG.debugf("Ignoring successful custom required-action event for action '%s' in realm '%s'",
                    action, event.getRealmId());
            return;
        }

        LOG.debugf("Processing successful Terms acceptance for user '%s' in realm '%s'",
                event.getUserId(), event.getRealmId());

        RealmModel realm = session.realms().getRealm(event.getRealmId());
        if (realm == null) {
            LOG.warnf("Cannot record Terms acceptance: realm '%s' no longer exists", event.getRealmId());
            return;
        }

        UserModel user = session.users().getUserById(realm, event.getUserId());
        if (user == null) {
            LOG.warnf("Cannot record Terms acceptance: user '%s' no longer exists in realm '%s'",
                    event.getUserId(), realm.getName());
            return;
        }

        TermsHashAttributes.CurrentTerms currentTerms = TermsHashAttributes.resolveCurrentTerms(session, realm, user);
        if (currentTerms == null) {
            LOG.warnf("Cannot record Terms acceptance for user '%s' in realm '%s': current Terms could not be resolved",
                    user.getId(), realm.getName());
            return;
        }

        user.setSingleAttribute(TermsHashAttributes.ACCEPTED_HASH, currentTerms.hash());
        user.setSingleAttribute(TermsHashAttributes.ACCEPTED_LANGUAGE, currentTerms.language());
        LOG.infof("Recorded accepted Terms hash '%s' and language '%s' for user '%s' in realm '%s'",
                TermsHashAttributes.abbreviateHash(currentTerms.hash()), currentTerms.language(),
                user.getId(), realm.getName());
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // This listener reacts only to user events.
        LOG.debug("Ignoring admin event");
    }

    @Override
    public void close() {
        // The Keycloak session owns all resources.
        LOG.debug("Closing Terms acceptance event listener");
    }
}
