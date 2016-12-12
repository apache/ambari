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
 * A component resource provider for a gsInstaller defined cluster.
 */
public class GSInstallerComponentProvider extends GSInstallerResourceProvider{

  // Components
  protected static final String COMPONENT_CLUSTER_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("ServiceComponentInfo", "cluster_name");
  protected static final String COMPONENT_SERVICE_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("ServiceComponentInfo", "service_name");
  protected static final String COMPONENT_COMPONENT_NAME_PROPERTY_ID  = PropertyHelper.getPropertyId("ServiceComponentInfo", "component_name");
  protected static final String COMPONENT_STATE_PROPERTY_ID           = PropertyHelper.getPropertyId("ServiceComponentInfo", "state");


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a resource provider based on the given cluster definition.
   *
   * @param clusterDefinition  the cluster definition
   */
  public GSInstallerComponentProvider(ClusterDefinition clusterDefinition) {
    super(Resource.Type.Component, clusterDefinition);
    initComponentResources();
  }


  // ----- GSInstallerResourceProvider ---------------------------------------

  @Override
  public void updateProperties(Resource resource, Request request, Predicate predicate) {

    Set<String> propertyIds = getRequestPropertyIds(request, predicate);
    if (contains(propertyIds, COMPONENT_STATE_PROPERTY_ID)) {
      String serviceName   = (String) resource.getPropertyValue(COMPONENT_SERVICE_NAME_PROPERTY_ID);
      String componentName = (String) resource.getPropertyValue(COMPONENT_COMPONENT_NAME_PROPERTY_ID);
      resource.setProperty(COMPONENT_STATE_PROPERTY_ID, getClusterDefinition().getComponentState(serviceName, componentName));
    }
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Create the resources based on the cluster definition.
   */
  private void initComponentResources() {
    String      clusterName = getClusterDefinition().getClusterName();
    Set<String> services    = getClusterDefinition().getServices();
    for (String serviceName : services) {
      Set<String> components = getClusterDefinition().getComponents(serviceName);
      for (String componentName : components) {
        Resource component = new ResourceImpl(Resource.Type.Component);
        component.setProperty(COMPONENT_CLUSTER_NAME_PROPERTY_ID, clusterName);
        component.setProperty(COMPONENT_SERVICE_NAME_PROPERTY_ID, serviceName);
        component.setProperty(COMPONENT_COMPONENT_NAME_PROPERTY_ID, componentName);

        addResource(component);
      }
    }
  }
}
