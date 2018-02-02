package org.artifactory.rest.resource.system;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.HaAddon;
import org.artifactory.addon.ha.message.HaMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Resource handles incoming propagation events
 *
 * @author Shay Bagants
 */
public class PropagationResource {
    private static final Logger log = LoggerFactory.getLogger(PropagationResource.class);
    private AddonsManager addonsManager;

    PropagationResource(AddonsManager addonsManager) {
        this.addonsManager = addonsManager;
    }

    @POST
    @Path("{topic}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response handlePropagationEvent(@PathParam("topic") String topic, HaMessage message) {
        HaAddon haAddon = addonsManager.addonByType(HaAddon.class);
        if (message == null) {
            log.warn("Received empty propagation event, ignoring request.");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Received empty propagation event, ignoring request.").build();
        }
        if (haAddon.getCurrentMemberServerId().equals(message.getPublishingMemberId())) {
            log.warn("Received propagation event from itself, ignoring request.");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Received propagation event from itself, ignoring request.").build();
        }

        try {
            haAddon.handleIncomingPropagationEvent(topic, message);
        } catch (Exception e) {
            log.error("An error occurred while processing propagation event. {}", e.getMessage());
            log.debug("An error occurred while processing propagation event.", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Failed processing propagation event, see logs for further details.").build();
        }
        return Response.ok().build();
    }
}
