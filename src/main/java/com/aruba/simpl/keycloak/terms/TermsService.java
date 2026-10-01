package com.aruba.simpl.keycloak.terms;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/** Encapsulates realm-level terms metadata independently from the JAX-RS resource. */
public final class TermsService {

    private static final Logger LOG = Logger.getLogger(TermsService.class);

    static final String VERSION_ATTRIBUTE = "terms.version";
    static final String PUBLISHED_AT_ATTRIBUTE = "terms.publishedAt";

    public TermsStatus getStatus(RealmModel realm) {
        LOG.debugf("Reading terms status for realm '%s'", realm.getName());
        int version = parseCurrentVersion(realm.getAttribute(VERSION_ATTRIBUTE));
        String publishedAt = realm.getAttribute(PUBLISHED_AT_ATTRIBUTE);
        LOG.debugf("Read terms status for realm '%s': version=%d, publishedAt='%s'",
                realm.getName(), version, publishedAt);
        return new TermsStatus(version, publishedAt);
    }

    static int parseCurrentVersion(String value) {
        if (value == null) {
            LOG.debugf("Realm attribute %s is missing; using version 0", VERSION_ATTRIBUTE);
            return 0;
        }

        try {
            int version = Integer.parseInt(value);
            if (version < 0) {
                LOG.warnf("Realm attribute %s has invalid negative value '%s'", VERSION_ATTRIBUTE, value);
                throw new IllegalArgumentException("Realm attribute terms.version must be a non-negative integer");
            }
            LOG.debugf("Parsed realm attribute %s as version %d", VERSION_ATTRIBUTE, version);
            return version;
        } catch (NumberFormatException exception) {
            LOG.warnf("Realm attribute %s has invalid non-integer value '%s'", VERSION_ATTRIBUTE, value);
            throw new IllegalArgumentException("Realm attribute terms.version must be a non-negative integer", exception);
        }
    }

    public TermsStatus publish(KeycloakSession session, RealmModel realm) {
        LOG.debugf("Starting terms publication for realm '%s'", realm.getName());
        int currentVersion = parseCurrentVersion(realm.getAttribute(VERSION_ATTRIBUTE));
        if (currentVersion == Integer.MAX_VALUE) {
            LOG.errorf("Cannot publish terms for realm '%s': version is already at %d",
                    realm.getName(), Integer.MAX_VALUE);
            throw new IllegalStateException("Realm attribute terms.version cannot be incremented further");
        }
        int version = currentVersion + 1;
        String publishedAt = Instant.now().toString();
        LOG.debugf("Updating terms metadata for realm '%s' from version %d to %d at '%s'",
                realm.getName(), currentVersion, version, publishedAt);
        realm.setAttribute(VERSION_ATTRIBUTE, Integer.toString(version));
        realm.setAttribute(PUBLISHED_AT_ATTRIBUTE, publishedAt);
        AtomicInteger affectedUsers = new AtomicInteger();
        try (var users = session.users().searchForUserStream(realm, Map.of())) {
            users.forEach(user -> {
                LOG.debugf("Adding %s required action to user '%s' in realm '%s'",
                        UserModel.RequiredAction.TERMS_AND_CONDITIONS, user.getId(), realm.getName());
                user.addRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS);
                affectedUsers.incrementAndGet();
            });
        }
        LOG.infof("Published terms version %d for realm '%s' and marked %d users for acceptance",
                version, realm.getName(), affectedUsers.get());
        return new TermsStatus(version, publishedAt);
    }
}
