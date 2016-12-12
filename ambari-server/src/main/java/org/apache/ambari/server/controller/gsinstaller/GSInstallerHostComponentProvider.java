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

import java.util.Set;

import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

/**
 * A host component resource provider for a gsInstaller defined cluster.
 */
public class GSInstallerHostComponentProvider extends GSInstallerResourceProvider{

  // Host Components
  protected static final String HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID   = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
  protected static final String HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID   = PropertyHelper.getPropertyId("HostRoles", "service_name");
  protected static final String HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "component_name");
  protected static final String HOST_COMPONENT_HOST_NAME_PROPERTY_ID      = PropertyHelper.getPropertyId("HostRoles", "host_name");
  protected static final String HOST_COMPONENT_STATE_PROPERTY_ID          = PropertyHelper.getPropertyId("HostRoles", "state");
  protected static final String HOST_COMPONENT_DESIRED_STATE_PROPERTY_ID  = PropertyHelper.getPropertyId("HostRoles", "desired_state");


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a resource provider based on the given cluster definition.
   *
   * @param clusterDefinition  the cluster definition
   */
  public GSInstallerHostComponentProvider(ClusterDefinition clusterDefinition) {
    super(Resource.Type.HostComponent, clusterDefinition);
    initHostComponentResources();
  }


  // ----- GSInstallerResourceProvider ---------------------------------------

  @Override
  public void updateProperties(Resource resource, Request request, Predicate predicate) {
    Set<String> propertyIds = getRequestPropertyIds(request, predicate);
    if (contains(propertyIds, HOST_COMPONENT_STATE_PROPERTY_ID) ||
        contains(propertyIds, HOST_COMPONENT_DESIRED_STATE_PROPERTY_ID)) {
      String serviceName   = (String) resource.getPropertyValue(HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID);
      String componentName = (String) resource.getPropertyValue(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID);
      String hostName      = (String) resource.getPropertyValue(HOST_COMPONENT_HOST_NAME_PROPERTY_ID);

      String hostComponentState = getClusterDefinition().getHostComponentState(hostName, serviceName, componentName);

      resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, hostComponentState);
      resource.setProperty(HOST_COMPONENT_DESIRED_STATE_PROPERTY_ID, hostComponentState);
    }
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Create the resources based on the cluster definition.
   */
  private void initHostComponentResources() {
    String      clusterName = getClusterDefinition().getClusterName();
    Set<String> services    = getClusterDefinition().getServices();
    for (String serviceName : services) {
      Set<String> hosts = getClusterDefinition().getHosts();
      for (String hostName : hosts) {
        Set<String> hostComponents = getClusterDefinition().getHostComponents(serviceName, hostName);
        for (String componentName : hostComponents) {
          Resource hostComponent = new ResourceImpl(Resource.Type.HostComponent);
          hostComponent.setProperty(HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID, clusterName);
          hostComponent.setProperty(HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID, serviceName);
          hostComponent.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, componentName);
          hostComponent.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, hostName);

          addResource(hostComponent);
        }
      }
    }
  }
}
