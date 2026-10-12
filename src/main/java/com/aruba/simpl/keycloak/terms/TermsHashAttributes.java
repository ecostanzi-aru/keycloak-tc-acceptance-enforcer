package com.aruba.simpl.keycloak.terms;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Properties;

import org.jboss.logging.Logger;
import org.keycloak.locale.LocaleSelectorProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.theme.Theme;

/** Realm and user attributes used to track the accepted Terms revision. */
final class TermsHashAttributes {

    private static final Logger LOG = Logger.getLogger(TermsHashAttributes.class);

    static final String ACCEPTED_HASH = "terms.acceptedHash";
    static final String ACCEPTED_LANGUAGE = "terms.acceptedLanguage";
    static final String TERMS_TEXT_KEY = "termsText";

    private static final byte[] HASH_FORMAT = "termsText-v2".getBytes(StandardCharsets.UTF_8);

    static CurrentTerms resolveCurrentTerms(KeycloakSession session, RealmModel realm, UserModel user) {
        LocaleSelectorProvider localeSelector = session.getProvider(LocaleSelectorProvider.class);
        if (localeSelector == null) {
            LOG.warnf("Cannot resolve Terms for realm '%s': no locale selector provider is available", realm.getName());
            return null;
        }

        Locale locale = localeSelector.resolveLocale(realm, user, Theme.Type.LOGIN);
        if (locale == null) {
            LOG.warnf("Cannot resolve Terms for user '%s' in realm '%s': no login locale is available",
                    user.getId(), realm.getName());
            return null;
        }

        String language = locale.toLanguageTag();
        try {
            Properties messages = session.theme().getTheme(Theme.Type.LOGIN).getEnhancedMessages(realm, locale);
            String termsText = messages.getProperty(TERMS_TEXT_KEY);
            if (termsText == null) {
                LOG.warnf("Cannot resolve Terms for locale '%s' in realm '%s': message '%s' is missing",
                        language, realm.getName(), TERMS_TEXT_KEY);
                return null;
            }

            String hash = hashTermsText(termsText);
            LOG.debugf("Resolved Terms hash '%s' for locale '%s', user '%s', realm '%s'",
                    abbreviateHash(hash), language, user.getId(), realm.getName());
            return new CurrentTerms(hash, language);
        } catch (IOException exception) {
            LOG.errorf(exception, "Cannot load login-theme messages for locale '%s' in realm '%s'",
                    language, realm.getName());
            return null;
        }
    }

    static String hashTermsText(String termsText) {
        MessageDigest digest = sha256();
        digest.update(HASH_FORMAT);
        updateDigest(digest, termsText);
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static void updateDigest(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    static String abbreviateHash(String hash) {
        if (hash == null) {
            LOG.debug("Representing a missing Terms hash");
            return "<missing>";
        }
        if (hash.length() <= 12) {
            LOG.debugf("Terms hash is already short enough to log safely (length=%d)", hash.length());
            return hash;
        }
        LOG.debugf("Abbreviating Terms hash for logging (length=%d)", hash.length());
        return hash.substring(0, 12) + "…";
    }

    private TermsHashAttributes() {
        LOG.debug("Constructed Terms hash attributes utility");
    }

    record CurrentTerms(String hash, String language) {
    }
}
