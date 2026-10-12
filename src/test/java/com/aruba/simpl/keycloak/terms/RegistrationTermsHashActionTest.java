package com.aruba.simpl.keycloak.terms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.authentication.requiredactions.TermsAndConditions;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

class RegistrationTermsHashActionTest {

    @Test
    void validationAlwaysDelegatesTermsValidationToTheBuiltInAction() {
        ValidationContext context = mock(ValidationContext.class);

        new RegistrationTermsHashAction().validate(context);

        verify(context).success();
    }

    @Test
    void recordsCurrentHashWhenTermsWereAcceptedDuringRegistration() {
        FormContext context = mock(FormContext.class);
        RealmModel realm = realmWithTerms("current terms");
        UserModel user = mock(UserModel.class);
        when(context.getRealm()).thenReturn(realm);
        when(context.getUser()).thenReturn(user);
        when(user.getFirstAttribute(TermsAndConditions.USER_ATTRIBUTE)).thenReturn("1234567890");

        new RegistrationTermsHashAction().success(context);

        verify(user).setSingleAttribute(TermsHashAttributes.ACCEPTED_HASH,
                TermsHashAttributes.calculateCurrentHash(realm));
    }

    @Test
    void doesNotRecordHashWhenTermsWereNotAccepted() {
        FormContext context = mock(FormContext.class);
        UserModel user = mock(UserModel.class);
        when(context.getUser()).thenReturn(user);

        new RegistrationTermsHashAction().success(context);

        verify(user, never()).setSingleAttribute(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void doesNothingWhenRunBeforeUserCreation() {
        FormContext context = mock(FormContext.class);

        new RegistrationTermsHashAction().success(context);

        verify(context).getUser();
        verify(context, never()).getRealm();
    }

    @Test
    void doesNotRecordHashWhenTermsTextIsMissing() {
        FormContext context = mock(FormContext.class);
        RealmModel realm = realmWithTerms(null);
        UserModel user = mock(UserModel.class);
        when(context.getRealm()).thenReturn(realm);
        when(context.getUser()).thenReturn(user);
        when(user.getFirstAttribute(TermsAndConditions.USER_ATTRIBUTE)).thenReturn("1234567890");

        new RegistrationTermsHashAction().success(context);

        verify(user, never()).setSingleAttribute(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void factoryExposesRegistrationExecution() {
        RegistrationTermsHashActionFactory factory = new RegistrationTermsHashActionFactory();

        assertEquals("registration-terms-hash", factory.getId());
        assertEquals("Record Terms acceptance hash", factory.getDisplayType());
        assertInstanceOf(RegistrationTermsHashAction.class, factory.create(null));
    }

    private RealmModel realmWithTerms(String termsText) {
        RealmModel realm = mock(RealmModel.class);
        when(realm.getRealmLocalizationTexts()).thenReturn(termsText == null
                ? Map.of()
                : Map.of("en", Map.of(TermsHashAttributes.TERMS_TEXT_KEY, termsText)));
        return realm;
    }
}
