/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller.gsinstaller;

import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.Set;

/**
 * A host resource provider for a gsInstaller defined cluster.
 */
public class GSInstallerHostProvider extends GSInstallerResourceProvider{

  // Hosts
  protected static final String HOST_CLUSTER_NAME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "cluster_name");
  protected static final String HOST_NAME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "host_name");
  protected static final String HOST_STATE_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "host_state");


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a resource provider based on the given cluster definition.
   *
   * @param clusterDefinition  the cluster definition
   */
  public GSInstallerHostProvider(ClusterDefinition clusterDefinition) {
    super(Resource.Type.Host, clusterDefinition);
    initHostResources();
  }


  // ----- GSInstallerResourceProvider ---------------------------------------

  @Override
  public void updateProperties(Resource resource, Request request, Predicate predicate) {
    Set<String> propertyIds = getRequestPropertyIds(request, predicate);
    if (contains(propertyIds, HOST_STATE_PROPERTY_ID)) {
      String hostName = (String) resource.getPropertyValue(HOST_NAME_PROPERTY_ID);
      resource.setProperty(HOST_STATE_PROPERTY_ID, getClusterDefinition().getHostState(hostName));
    }
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Create the resources based on the cluster definition.
   */
  private void initHostResources() {
    ClusterDefinition clusterDefinition = getClusterDefinition();
    String            clusterName       = clusterDefinition.getClusterName();
    Set<String>       hosts             = clusterDefinition.getHosts();

    for (String hostName : hosts) {
      Resource host = new ResourceImpl(Resource.Type.Host);
      host.setProperty(HOST_CLUSTER_NAME_PROPERTY_ID, clusterName);
      host.setProperty(HOST_NAME_PROPERTY_ID, hostName);

      addResource(host);
    }
  }
}
