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
    }

    @Override
    public void onEvent(Event event) {
        if (event == null || event.getType() != EventType.CUSTOM_REQUIRED_ACTION) {
            return;
        }
        if (event.getDetails() == null || !UserModel.RequiredAction.TERMS_AND_CONDITIONS.name()
                .equals(event.getDetails().get(Details.CUSTOM_REQUIRED_ACTION))) {
            LOG.tracef("Ignoring successful custom required-action event for action '%s' in realm '%s'",
                    event.getDetails() == null ? null : event.getDetails().get(Details.CUSTOM_REQUIRED_ACTION),
                    event.getRealmId());
            return;
        }

        LOG.debugf("Processing successful Terms acceptance for user '%s' in realm '%s'",
                event.getUserId(), event.getRealmId());

        RealmModel realm = session.realms().getRealm(event.getRealmId());
        if (realm == null) {
            LOG.warnf("Cannot record Terms acceptance: realm '%s' no longer exists", event.getRealmId());
            return;
        }

        String currentHash = realm.getAttribute(TermsHashAttributes.CURRENT_HASH);
        if (currentHash == null || currentHash.isBlank()) {
            LOG.warnf("Cannot record Terms acceptance for realm '%s': %s is not configured",
                    realm.getName(), TermsHashAttributes.CURRENT_HASH);
            return;
        }

        UserModel user = session.users().getUserById(realm, event.getUserId());
        if (user == null) {
            LOG.warnf("Cannot record Terms acceptance: user '%s' no longer exists in realm '%s'",
                    event.getUserId(), realm.getName());
            return;
        }

        user.setSingleAttribute(TermsHashAttributes.ACCEPTED_HASH, currentHash);
        LOG.debugf("Recorded accepted Terms hash '%s' for user '%s' in realm '%s'",
                TermsHashAttributes.abbreviateHash(currentHash), user.getId(), realm.getName());
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // This listener reacts only to user events.
        LOG.trace("Ignoring admin event");
    }

    @Override
    public void close() {
        // The Keycloak session owns all resources.
        LOG.trace("Closing Terms acceptance event listener");
    }
}
