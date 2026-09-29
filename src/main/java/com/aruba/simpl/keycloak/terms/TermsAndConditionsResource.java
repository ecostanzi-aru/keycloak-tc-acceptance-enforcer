package com.aruba.simpl.keycloak.terms;

import jakarta.ws.rs.GET;
import jakarta.enterprise.inject.Vetoed;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

@Vetoed
/** Root JAX-RS resource for the terms-and-conditions admin extension. */
@Path("/")
public final class TermsAndConditionsResource {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private final TermsService termsService;

    TermsAndConditionsResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth,
                               AdminEventBuilder adminEvent, TermsService termsService) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent;
        this.termsService = termsService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public TermsStatus getStatus() {
        auth.realm().requireViewRealm();
        return termsService.getStatus(realm);
    }

    @POST
    @Path("publish")
    @Produces(MediaType.APPLICATION_JSON)
    public TermsStatus publish() {
        auth.realm().requireManageRealm();
        TermsStatus publication = termsService.publish(session, realm);
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.CUSTOM)
                .resourcePath("terms-and-conditions", "publish")
                .detail("terms.version", Integer.toString(publication.version()))
                .detail("terms.publishedAt", publication.publishedAt())
                .success();
        return publication;
    }
}
