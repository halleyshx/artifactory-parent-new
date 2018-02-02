/*
 *
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2016 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.artifactory.rest.resource.system;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.replication.GlobalReplicationsConfigDescriptor;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.Serializable;

import static org.artifactory.api.rest.constant.SystemRestConstants.PATH_REPLICATIONS;
import static org.artifactory.api.rest.constant.SystemRestConstants.PATH_ROOT;

/**
 * @author gidis
 */
@Path(PATH_ROOT+ "/" + PATH_REPLICATIONS)
@RolesAllowed(AuthorizationService.ROLE_ADMIN)
@Component
public class GlobalReplicationResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGlobalReplicationConfig() {
        GlobalReplicationsConfigDescriptor config = ContextHelper.get()
                .getCentralConfig().getDescriptor().getReplicationsConfig();
        // Copy the info from the descriptor to
        GlobalReplicationConfig result=new GlobalReplicationConfig();
        result.setBlockPullReplications(config.isBlockPullReplications());
        result.setBlockPushReplications(config.isBlockPushReplications());
        return Response.ok().entity(result).build();
    }
    /**
     * Globally disable the push replication
     */
    @POST
    @Path("block")
    public Response blockPushPull(@QueryParam("push") String push,
                                  @QueryParam("pull") String pull) throws IOException {
        updateDescriptor(push, pull, true);
        String msg = "Successfully blocked all replications, no replication will be triggered.";
        boolean noPush = StringUtils.isNotBlank(push) && !Boolean.parseBoolean(push);
        if (noPush) {
            msg = "Successfully blocked all pull replications, no pull replication will be triggered.";
        }
        boolean noPull = StringUtils.isNotBlank(pull) && !Boolean.parseBoolean(pull);
        if (noPull) {
            msg = "Successfully blocked all push replications, no push replication will be triggered.";
        }
        if (noPush && noPull) {
            msg = "No action taken.";
        }
        return Response.ok(msg).build();
    }

    /**
     * Globally enable the push replication
     */
    @POST
    @Path("unblock")
    public Response unblockPushPull(@QueryParam("push") String push,
                                    @QueryParam("pull") String pull) throws IOException {
        updateDescriptor(push, pull, false);
        String msg = "Successfully unblocked all replications.";
        boolean noPush = StringUtils.isNotBlank(push) && !Boolean.parseBoolean(push);
        if (noPush) {
            msg = "Successfully unblocked all pull replications.";
        }
        boolean noPull = StringUtils.isNotBlank(pull) && !Boolean.parseBoolean(pull);
        if (noPull) {
            msg = "Successfully unblocked all push replications.";
        }
        if (noPush && noPull) {
            msg = "No action taken.";
        }
        return Response.ok(msg).build();
    }

    private void updateDescriptor(String push, String pull, boolean status) {
        boolean pushVal = push == null || Boolean.parseBoolean(push);
        boolean pullVal = pull == null || Boolean.parseBoolean(pull);
        CentralConfigService centralConfig = ContextHelper.get().getCentralConfig();
        MutableCentralConfigDescriptor descriptor = centralConfig.getMutableDescriptor();
        if(pushVal) {
            descriptor.getReplicationsConfig().setBlockPushReplications(status);
        }
        if(pullVal) {
            descriptor.getReplicationsConfig().setBlockPullReplications(status);
        }
        centralConfig.saveEditedDescriptorAndReload(descriptor);
    }

    private class GlobalReplicationConfig implements Serializable{
        private boolean blockPullReplications;

        private boolean blockPushReplications;

        public boolean isBlockPullReplications() {
            return blockPullReplications;
        }

        public void setBlockPullReplications(boolean blockPullReplications) {
            this.blockPullReplications = blockPullReplications;
        }

        public boolean isBlockPushReplications() {
            return blockPushReplications;
        }

        public void setBlockPushReplications(boolean blockPushReplications) {
            this.blockPushReplications = blockPushReplications;
        }
    }
}
