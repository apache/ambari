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

package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.ganglia.GangliaPropertyProvider;
import org.apache.ambari.server.controller.jmx.JMXPropertyProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.ProviderModule;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The default provider module implementation.
 */
public class DefaultProviderModule implements ProviderModule {

  private static final PropertyId HOST_ATTRIBUTES_PROPERTY_ID               = PropertyHelper.getPropertyId("attributes", "Hosts");
  private static final PropertyId HOST_COMPONENT_HOST_NAME_PROPERTY_ID      = PropertyHelper.getPropertyId("host_name", "HostRoles");
  private static final PropertyId HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("component_name", "HostRoles");

  /**
   * The map of resource providers.
   */
  private final Map<Resource.Type, ResourceProvider> resourceProviders = new HashMap<Resource.Type, ResourceProvider>();

  /**
   * The map of lists of property providers.
   */
  private final Map<Resource.Type,List<PropertyProvider>> propertyProviders = new HashMap<Resource.Type, List<PropertyProvider>>();

  /**
   * The map of hosts.
   */
  private Map<String, String> hostMapping;

  /**
   * The host name of the Ganglia collector.
   */
  private String gangliaCollectorHostName;


  // ----- Constructors ------------------------------------------------------

  /**
   * Create a default provider module.
   */
  public DefaultProviderModule() {
    AmbariManagementController managementController = AmbariServer.getController();

    // First create all of the resource providers...
    for (Resource.Type type : Resource.Type.values()){
      createResourceProvider(type, managementController);
    }

    // ... then create the things needed to create the property providers ...
    try {
      hostMapping              = getHostMap();
      gangliaCollectorHostName = getGangliaCollectorHost();
    } catch (AmbariException e) {
      // TODO ...
    }

    // ... then create all of the property providers
    for (Resource.Type type : Resource.Type.values()){
      createPropertyProviders(type);
    }
  }


  // ----- ProviderModule ----------------------------------------------------

  @Override
  public ResourceProvider getResourceProvider(Resource.Type type) {
    return resourceProviders.get(type);
  }

  @Override
  public List<PropertyProvider> getPropertyProviders(Resource.Type type) {
    return propertyProviders.get(type);
  }


  // ----- utility methods ---------------------------------------------------

  private void createResourceProvider(Resource.Type type, AmbariManagementController managementController) {
    resourceProviders.put( type , ResourceProviderImpl.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type), managementController));
  }

  private void createPropertyProviders(Resource.Type type) {
    List<PropertyProvider> providers = new LinkedList<PropertyProvider>();
    if (type == Resource.Type.HostComponent) {
      providers.add(new JMXPropertyProvider(
          PropertyHelper.getJMXPropertyIds(type),
          new URLStreamProvider(),
          hostMapping));

      providers.add(new GangliaPropertyProvider(
          PropertyHelper.getGangliaPropertyIds(type),
          new URLStreamProvider(),
          gangliaCollectorHostName,
          PropertyHelper.getPropertyId("host_name", "HostRoles"),
          PropertyHelper.getPropertyId("component_name", "HostRoles")));
    }
    propertyProviders.put(type, providers);
  }

  public Map<String, String> getHostMap() throws AmbariException {
    Map<String, String> hostMap      = new HashMap<String, String>();
    ResourceProvider    hostProvider = getResourceProvider(Resource.Type.Host);
    ObjectMapper        mapper       = new ObjectMapper();
    Request             request      = PropertyHelper.getReadRequest(Collections.singleton(HOST_ATTRIBUTES_PROPERTY_ID));

    Set<Resource> hosts = hostProvider.getResources(request, null);
    for (Resource host : hosts) {
      String attributes = (String) host.getPropertyValue(HOST_ATTRIBUTES_PROPERTY_ID);
      if (attributes != null && !attributes.startsWith("[]")) {
        try {
          Map<String, String> attributeMap = mapper.readValue(attributes, new TypeReference<Map<String, String>>() {});
          hostMap.put(attributeMap.get("privateFQDN"), attributeMap.get("publicFQDN"));
        } catch (IOException e) {
          throw new IllegalStateException("Can't read hosts " + attributes, e);
        }
      }
    }
    return hostMap;
  }

  public String getGangliaCollectorHost() throws AmbariException {
    ResourceProvider provider = getResourceProvider(Resource.Type.HostComponent);
    Request          request  = PropertyHelper.getReadRequest(Collections.singleton(HOST_COMPONENT_HOST_NAME_PROPERTY_ID));

    Predicate predicate = new PredicateBuilder().property(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID).
        equals("GANGLIA_MONITOR_SERVER").toPredicate();

    Set<Resource> hostComponents = provider.getResources(request, predicate);
    // should only be one. TODO add check
    String hostName = (String) hostComponents.iterator().next().getPropertyValue(HOST_COMPONENT_HOST_NAME_PROPERTY_ID);

    return hostMapping.get(hostName);
  }

}
