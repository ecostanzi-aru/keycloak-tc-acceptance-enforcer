package com.aruba.simpl.keycloak.terms;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class TermsHashProvidersTest {

    @Test
    void doesNotAddTermsActionWhenTheAcceptedHashMatches() {
        RequiredActionContext context = triggerContext("current", "current");

        new TermsHashRequiredAction().evaluateTriggers(context);

        verify(context.getUser(), never()).addRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS);
    }

    @Test
    void addsTermsActionWhenTheAcceptedHashDiffers() {
        RequiredActionContext context = triggerContext("current", "previous");

        new TermsHashRequiredAction().evaluateTriggers(context);

        verify(context.getUser()).addRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS);
    }

    @Test
    void addsTermsActionWhenThereIsNoAcceptedHash() {
        RequiredActionContext context = triggerContext("current", null);

        new TermsHashRequiredAction().evaluateTriggers(context);

        verify(context.getUser()).addRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS);
    }

    @Test
    void doesNothingWhenTheCurrentHashIsMissing() {
        RequiredActionContext context = triggerContext(null, "previous");

        new TermsHashRequiredAction().evaluateTriggers(context);

        verify(context.getUser(), never()).addRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS);
    }

    @Test
    void recordsCurrentHashAfterSuccessfulTermsAcceptance() {
        KeycloakSession session = mock(KeycloakSession.class);
        RealmModel realm = mock(RealmModel.class);
        UserModel user = mock(UserModel.class);
        RealmProvider realms = mock(RealmProvider.class);
        UserProvider users = mock(UserProvider.class);
        when(session.realms()).thenReturn(realms);
        when(session.users()).thenReturn(users);
        when(realms.getRealm("realm-id")).thenReturn(realm);
        when(realm.getAttribute(TermsHashAttributes.CURRENT_HASH)).thenReturn("current");
        when(users.getUserById(realm, "user-id")).thenReturn(user);

        new TermsAcceptanceEventListenerProvider(session).onEvent(requiredActionEvent(
                EventType.CUSTOM_REQUIRED_ACTION, UserModel.RequiredAction.TERMS_AND_CONDITIONS.name()));

        verify(user).setSingleAttribute(TermsHashAttributes.ACCEPTED_HASH, "current");
    }

    @Test
    void doesNotRecordHashForAnotherSuccessfulRequiredAction() {
        KeycloakSession session = sessionWithCurrentHash("current");

        new TermsAcceptanceEventListenerProvider(session).onEvent(requiredActionEvent(
                EventType.CUSTOM_REQUIRED_ACTION, UserModel.RequiredAction.UPDATE_PASSWORD.name()));

        verify(session.users(), never()).getUserById(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doesNotRecordHashWhenTermsAcceptanceIsRejected() {
        KeycloakSession session = sessionWithCurrentHash("current");

        new TermsAcceptanceEventListenerProvider(session).onEvent(requiredActionEvent(
                EventType.CUSTOM_REQUIRED_ACTION_ERROR, UserModel.RequiredAction.TERMS_AND_CONDITIONS.name()));

        verify(session.users(), never()).getUserById(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doesNotWriteAnEmptyAcceptedHashWhenCurrentHashIsMissing() {
        KeycloakSession session = sessionWithCurrentHash(null);

        new TermsAcceptanceEventListenerProvider(session).onEvent(requiredActionEvent(
                EventType.CUSTOM_REQUIRED_ACTION, UserModel.RequiredAction.TERMS_AND_CONDITIONS.name()));

        verify(session.users(), never()).getUserById(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void factoriesExposeTheirProviderIds() {
        assertEquals("terms-hash-acceptance", new TermsAcceptanceEventListenerProviderFactory().getId());
        assertEquals("terms-hash-trigger", new TermsHashRequiredActionFactory().getId());
        assertInstanceOf(TermsHashRequiredAction.class,
                new TermsHashRequiredActionFactory().create(mock(KeycloakSession.class)));
    }

    private RequiredActionContext triggerContext(String currentHash, String acceptedHash) {
        RequiredActionContext context = mock(RequiredActionContext.class);
        RealmModel realm = mock(RealmModel.class);
        UserModel user = mock(UserModel.class);
        when(context.getRealm()).thenReturn(realm);
        when(context.getUser()).thenReturn(user);
        when(realm.getAttribute(TermsHashAttributes.CURRENT_HASH)).thenReturn(currentHash);
        when(user.getFirstAttribute(TermsHashAttributes.ACCEPTED_HASH)).thenReturn(acceptedHash);
        return context;
    }

    private KeycloakSession sessionWithCurrentHash(String currentHash) {
        KeycloakSession session = mock(KeycloakSession.class);
        RealmModel realm = mock(RealmModel.class);
        RealmProvider realms = mock(RealmProvider.class);
        UserProvider users = mock(UserProvider.class);
        when(session.realms()).thenReturn(realms);
        when(realms.getRealm("realm-id")).thenReturn(realm);
        when(session.users()).thenReturn(users);
        when(realm.getAttribute(TermsHashAttributes.CURRENT_HASH)).thenReturn(currentHash);
        return session;
    }

    private Event requiredActionEvent(EventType type, String action) {
        Event event = new Event();
        event.setType(type);
        event.setRealmId("realm-id");
        event.setUserId("user-id");
        event.setDetails(Map.of(Details.CUSTOM_REQUIRED_ACTION, action));
        return event;
    }
}
