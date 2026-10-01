package com.aruba.simpl.keycloak.terms;

import jakarta.ws.rs.GET;
import jakarta.enterprise.inject.Vetoed;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;
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

    private static final Logger LOG = Logger.getLogger(TermsAndConditionsResource.class);

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
        LOG.debugf("Constructed terms-and-conditions admin resource for realm '%s'", realm.getName());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public TermsStatus getStatus() {
        LOG.debugf("Terms status requested for realm '%s'; checking view permission", realm.getName());
        auth.realm().requireViewRealm();
        LOG.debugf("View permission granted for terms status in realm '%s'", realm.getName());
        TermsStatus status = termsService.getStatus(realm);
        LOG.debugf("Returning terms status for realm '%s': version=%d, publishedAt='%s'",
                realm.getName(), status.version(), status.publishedAt());
        return status;
    }

    @POST
    @Path("publish")
    @Produces(MediaType.APPLICATION_JSON)
    public TermsStatus publish() {
        LOG.debugf("Terms publication requested for realm '%s'; checking manage permission", realm.getName());
        auth.realm().requireManageRealm();
        LOG.debugf("Manage permission granted for terms publication in realm '%s'", realm.getName());
        TermsStatus publication = termsService.publish(session, realm);
        LOG.debugf("Recording admin event for terms publication in realm '%s': version=%d, publishedAt='%s'",
                realm.getName(), publication.version(), publication.publishedAt());
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.CUSTOM)
                .resourcePath("terms-and-conditions", "publish")
                .detail("terms.version", Integer.toString(publication.version()))
                .detail("terms.publishedAt", publication.publishedAt())
                .success();
        LOG.infof("Published terms version %d in realm '%s'", publication.version(), realm.getName());
        return publication;
    }
}
