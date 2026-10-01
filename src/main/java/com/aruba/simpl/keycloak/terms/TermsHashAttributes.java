package com.aruba.simpl.keycloak.terms;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.models.RealmModel;

/** Realm and user attributes used to track the accepted Terms revision. */
final class TermsHashAttributes {

    private static final Logger LOG = Logger.getLogger(TermsHashAttributes.class);

    static final String ACCEPTED_HASH = "terms.acceptedHash";
    static final String TERMS_TEXT_KEY = "termsText";

    private static final byte[] HASH_FORMAT = "termsText-v1".getBytes(StandardCharsets.UTF_8);

    static String calculateCurrentHash(RealmModel realm) {
        Map<String, Map<String, String>> localizations = realm.getRealmLocalizationTexts();
        if (localizations == null || localizations.isEmpty()) {
            LOG.debugf("No realm localization overrides exist in realm '%s'; no Terms hash is available",
                    realm.getName());
            return null;
        }

        List<Map.Entry<String, String>> localizedTerms = localizations.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> entry.getValue().get(TERMS_TEXT_KEY) != null)
                .map(entry -> Map.entry(entry.getKey(), entry.getValue().get(TERMS_TEXT_KEY)))
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .toList();
        if (localizedTerms.isEmpty()) {
            LOG.debugf("No %s localization overrides exist in realm '%s'; no Terms hash is available",
                    TERMS_TEXT_KEY, realm.getName());
            return null;
        }

        MessageDigest digest = sha256();
        digest.update(HASH_FORMAT);
        localizedTerms.forEach(entry -> {
            updateDigest(digest, entry.getKey());
            updateDigest(digest, entry.getValue());
        });
        String hash = HexFormat.of().formatHex(digest.digest());
        LOG.debugf("Calculated Terms hash '%s' from %d localized %s value(s) in realm '%s'",
                abbreviateHash(hash), localizedTerms.size(), TERMS_TEXT_KEY, realm.getName());
        return hash;
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
}
