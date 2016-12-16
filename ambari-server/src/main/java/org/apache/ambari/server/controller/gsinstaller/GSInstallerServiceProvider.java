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
 * A service resource provider for a gsInstaller defined cluster.
 */
public class GSInstallerServiceProvider extends GSInstallerResourceProvider{

  // Services
  protected static final String SERVICE_CLUSTER_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("ServiceInfo", "cluster_name");
  protected static final String SERVICE_SERVICE_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("ServiceInfo", "service_name");
  protected static final String SERVICE_SERVICE_STATE_PROPERTY_ID   = PropertyHelper.getPropertyId("ServiceInfo", "state");


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a resource provider based on the given cluster definition.
   *
   * @param clusterDefinition  the cluster definition
   */
  public GSInstallerServiceProvider(ClusterDefinition clusterDefinition) {
    super(Resource.Type.Service, clusterDefinition);
    initServiceResources();
  }


  // ----- GSInstallerResourceProvider ---------------------------------------

  @Override
  public void updateProperties(Resource resource, Request request, Predicate predicate) {
    Set<String> propertyIds = getRequestPropertyIds(request, predicate);
    if (contains(propertyIds, SERVICE_SERVICE_STATE_PROPERTY_ID)) {
      String serviceName = (String) resource.getPropertyValue(SERVICE_SERVICE_NAME_PROPERTY_ID);
      resource.setProperty(SERVICE_SERVICE_STATE_PROPERTY_ID, getClusterDefinition().getServiceState(serviceName));
    }
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Create the resources based on the cluster definition.
   */
  private void initServiceResources() {
    String      clusterName = getClusterDefinition().getClusterName();
    Set<String> services    = getClusterDefinition().getServices();

    for (String serviceName : services) {
      Resource service = new ResourceImpl(Resource.Type.Service);
      service.setProperty(SERVICE_CLUSTER_NAME_PROPERTY_ID, clusterName);
      service.setProperty(SERVICE_SERVICE_NAME_PROPERTY_ID, serviceName);

      addResource(service);
    }
  }
}
