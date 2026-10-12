package com.aruba.simpl.keycloak.terms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.locale.LocaleSelectorProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.models.ThemeManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.theme.Theme;

class TermsHashProvidersTest {

    @Test
    void doesNotAddTermsActionWhenTheAcceptedHashAndLanguageMatch() {
        RequiredActionContext context = triggerContext(
                "current", "en", hashForTerms("current"), "en", null);

        new TermsHashRequiredAction().evaluateTriggers(context);

        verify(context.getUser(), never()).addRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS);
    }

    @Test
    void addsTermsActionWhenTheLocalizedTermsContentDiffers() {
        RequiredActionContext context = triggerContext(
                "updated", "it", hashForTerms("previous"), "en", false);

        new TermsHashRequiredAction().evaluateTriggers(context);

        verify(context.getUser()).addRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS);
    }

    @Test
    void addsTermsActionWhenThereIsNoAcceptedHash() {
        RequiredActionContext context = triggerContext("current", "en", null, null, null);

        new TermsHashRequiredAction().evaluateTriggers(context);

        verify(context.getUser()).addRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS);
    }

    @Test
    void languageChangeDoesNotRequireAcceptanceWhenConfigurationIsDisabled() {
        RequiredActionContext context = triggerContext(
                "same content", "it", hashForTerms("same content"), "en", false);

        new TermsHashRequiredAction().evaluateTriggers(context);

        verify(context.getUser(), never()).addRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS);
    }

    @Test
    void languageChangeRequiresAcceptanceWhenConfiguredEvenIfContentMatches() {
        RequiredActionContext context = triggerContext(
                "same content", "it", hashForTerms("same content"), "en", true);

        new TermsHashRequiredAction().evaluateTriggers(context);

        verify(context.getUser()).addRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS);
    }

    @Test
    void doesNothingWhenTermsTextCannotBeResolved() {
        RequiredActionContext context = triggerContext(null, "en", "previous", "en", true);

        new TermsHashRequiredAction().evaluateTriggers(context);

        verify(context.getUser(), never()).addRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS);
    }

    @Test
    void termsHashChangesWhenTheResolvedTextChanges() {
        assertNotEquals(hashForTerms("original terms"), hashForTerms("updated terms"));
    }

    @Test
    void termsHashIsIndependentOfLanguageWhenResolvedTextMatches() {
        assertEquals(hashForTerms("same terms"), hashForTerms("same terms"));
    }

    @Test
    void recordsCurrentHashAndEffectiveLanguageAfterSuccessfulTermsAcceptance() {
        ListenerFixture fixture = listenerFixture("current", "it");

        new TermsAcceptanceEventListenerProvider(fixture.session()).onEvent(requiredActionEvent(
                EventType.CUSTOM_REQUIRED_ACTION, UserModel.RequiredAction.TERMS_AND_CONDITIONS.name()));

        verify(fixture.user()).setSingleAttribute(
                TermsHashAttributes.ACCEPTED_HASH, hashForTerms("current"));
        verify(fixture.user()).setSingleAttribute(TermsHashAttributes.ACCEPTED_LANGUAGE, "it");
    }

    @Test
    void doesNotRecordAcceptanceForAnotherSuccessfulRequiredAction() {
        ListenerFixture fixture = listenerFixture("current", "en");

        new TermsAcceptanceEventListenerProvider(fixture.session()).onEvent(requiredActionEvent(
                EventType.CUSTOM_REQUIRED_ACTION, UserModel.RequiredAction.UPDATE_PASSWORD.name()));

        verify(fixture.session().users(), never()).getUserById(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doesNotRecordAcceptanceWhenTermsAcceptanceIsRejected() {
        ListenerFixture fixture = listenerFixture("current", "en");

        new TermsAcceptanceEventListenerProvider(fixture.session()).onEvent(requiredActionEvent(
                EventType.CUSTOM_REQUIRED_ACTION_ERROR, UserModel.RequiredAction.TERMS_AND_CONDITIONS.name()));

        verify(fixture.session().users(), never()).getUserById(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doesNotWriteAcceptanceWhenTermsTextCannotBeResolved() {
        ListenerFixture fixture = listenerFixture(null, "en");

        new TermsAcceptanceEventListenerProvider(fixture.session()).onEvent(requiredActionEvent(
                EventType.CUSTOM_REQUIRED_ACTION, UserModel.RequiredAction.TERMS_AND_CONDITIONS.name()));

        verify(fixture.user(), never()).setSingleAttribute(anyString(), anyString());
    }

    @Test
    void requiredActionFactoryExposesLanguageChangeConfiguration() {
        TermsHashRequiredActionFactory factory = new TermsHashRequiredActionFactory();

        assertTrue(factory.isConfigurable());
        ProviderConfigProperty property = factory.getConfigMetadata().stream()
                .filter(candidate -> TermsHashRequiredActionFactory.REQUIRE_ACCEPTANCE_ON_LANGUAGE_CHANGE
                        .equals(candidate.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals(ProviderConfigProperty.BOOLEAN_TYPE, property.getType());
        assertEquals(Boolean.FALSE, property.getDefaultValue());

        RequiredActionConfigModel enabled = requiredActionConfig(true);
        assertTrue(TermsHashRequiredActionFactory.requireAcceptanceOnLanguageChange(enabled));
        assertFalse(TermsHashRequiredActionFactory.requireAcceptanceOnLanguageChange(requiredActionConfig(false)));
        assertFalse(TermsHashRequiredActionFactory.requireAcceptanceOnLanguageChange(null));
    }

    @Test
    void factoriesExposeTheirProviderIds() {
        assertEquals("terms-hash-acceptance", new TermsAcceptanceEventListenerProviderFactory().getId());
        assertEquals("terms-hash-trigger", new TermsHashRequiredActionFactory().getId());
        assertInstanceOf(TermsHashRequiredAction.class,
                new TermsHashRequiredActionFactory().create(mock(KeycloakSession.class)));
    }

    private RequiredActionContext triggerContext(String termsText, String currentLanguage,
                                                  String acceptedHash, String acceptedLanguage,
                                                  Boolean requireAcceptanceOnLanguageChange) {
        RequiredActionContext context = mock(RequiredActionContext.class);
        KeycloakSession session = mock(KeycloakSession.class);
        RealmModel realm = mock(RealmModel.class);
        UserModel user = mock(UserModel.class);
        when(context.getSession()).thenReturn(session);
        when(context.getRealm()).thenReturn(realm);
        when(context.getUser()).thenReturn(user);
        when(user.getFirstAttribute(TermsHashAttributes.ACCEPTED_HASH)).thenReturn(acceptedHash);
        when(user.getFirstAttribute(TermsHashAttributes.ACCEPTED_LANGUAGE)).thenReturn(acceptedLanguage);
        if (requireAcceptanceOnLanguageChange != null) {
            when(context.getConfig()).thenReturn(requiredActionConfig(requireAcceptanceOnLanguageChange));
        }
        configureResolvedTerms(session, realm, user, currentLanguage, termsText);
        return context;
    }

    private ListenerFixture listenerFixture(String termsText, String language) {
        KeycloakSession session = mock(KeycloakSession.class);
        RealmModel realm = mock(RealmModel.class);
        UserModel user = mock(UserModel.class);
        RealmProvider realms = mock(RealmProvider.class);
        UserProvider users = mock(UserProvider.class);
        when(session.realms()).thenReturn(realms);
        when(session.users()).thenReturn(users);
        when(realms.getRealm("realm-id")).thenReturn(realm);
        when(users.getUserById(realm, "user-id")).thenReturn(user);
        configureResolvedTerms(session, realm, user, language, termsText);
        return new ListenerFixture(session, user);
    }

    private void configureResolvedTerms(KeycloakSession session, RealmModel realm, UserModel user,
                                        String language, String termsText) {
        LocaleSelectorProvider localeSelector = mock(LocaleSelectorProvider.class);
        ThemeManager themeManager = mock(ThemeManager.class);
        Theme theme = mock(Theme.class);
        Locale locale = Locale.forLanguageTag(language);
        Properties messages = new Properties();
        if (termsText != null) {
            messages.setProperty(TermsHashAttributes.TERMS_TEXT_KEY, termsText);
        }

        when(session.getProvider(LocaleSelectorProvider.class)).thenReturn(localeSelector);
        when(localeSelector.resolveLocale(realm, user, Theme.Type.LOGIN)).thenReturn(locale);
        when(session.theme()).thenReturn(themeManager);
        try {
            when(themeManager.getTheme(Theme.Type.LOGIN)).thenReturn(theme);
            when(theme.getEnhancedMessages(realm, locale)).thenReturn(messages);
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private RequiredActionConfigModel requiredActionConfig(boolean requireAcceptanceOnLanguageChange) {
        RequiredActionConfigModel config = new RequiredActionConfigModel();
        config.setConfig(Map.of(
                TermsHashRequiredActionFactory.REQUIRE_ACCEPTANCE_ON_LANGUAGE_CHANGE,
                Boolean.toString(requireAcceptanceOnLanguageChange)));
        return config;
    }

    private String hashForTerms(String termsText) {
        return TermsHashAttributes.hashTermsText(termsText);
    }

    private Event requiredActionEvent(EventType type, String action) {
        Event event = new Event();
        event.setType(type);
        event.setRealmId("realm-id");
        event.setUserId("user-id");
        event.setDetails(Map.of(Details.CUSTOM_REQUIRED_ACTION, action));
        return event;
    }

    private record ListenerFixture(KeycloakSession session, UserModel user) {
    }
}
