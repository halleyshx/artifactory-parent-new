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

package org.artifactory.descriptor.replication;

import org.artifactory.descriptor.Descriptor;
import org.jfrog.common.GenerateDiffFunction;

import javax.xml.bind.annotation.XmlType;

/**
 * @author Noam Y. Tenne
 */
//TODO [by dan]: NADAV:
//TODO [by dan]: for now a remote replication can be both event and non-event (meaning you can have event-based
//TODO [by dan]: that also runs every time the cron timer hist -- need to think this through with PM -> do we allow like
//TODO [by dan]: this or want 2 remote replications per repo - one event and one cron?
@XmlType(name = "RemoteReplicationType", propOrder = {}, namespace = Descriptor.NS)
@GenerateDiffFunction
public class RemoteReplicationDescriptor extends ReplicationBaseDescriptor {
}
