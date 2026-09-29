package com.aruba.simpl.keycloak.terms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TermsAndConditionsAdminRealmResourceProviderFactoryTest {

    @Test
    void exposesTheConfiguredProviderIdAndCreatesTheAdminProvider() {
        var factory = new TermsAndConditionsAdminRealmResourceProviderFactory();

        assertEquals("terms-and-conditions", factory.getId());
        assertInstanceOf(TermsAndConditionsAdminRealmResourceProvider.class,
                factory.create(Mockito.mock(org.keycloak.models.KeycloakSession.class)));
    }
}
