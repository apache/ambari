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
import org.apache.ambari.server.controller.ganglia.GangliaHostProvider;
import org.apache.ambari.server.controller.ganglia.GangliaPropertyProvider;
import org.apache.ambari.server.controller.jmx.JMXHostProvider;
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

import com.google.inject.Inject;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The default provider module implementation.
 */
public class DefaultProviderModule implements ProviderModule {

  private static final PropertyId HOST_CLUSTER_NAME_PROPERTY_ID             = PropertyHelper.getPropertyId("cluster_name", "HostRoles");
  private static final PropertyId HOST_NAME_PROPERTY_ID                     = PropertyHelper.getPropertyId("host_name", "Hosts");
  private static final PropertyId HOST_IP_PROPERTY_ID                       = PropertyHelper.getPropertyId("ip", "Hosts");
  private static final PropertyId HOST_ATTRIBUTES_PROPERTY_ID               = PropertyHelper.getPropertyId("attributes", "Hosts");
  private static final PropertyId CLUSTER_NAME_PROPERTY_ID                  = PropertyHelper.getPropertyId("cluster_name", "Clusters");
  private static final PropertyId HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID   = PropertyHelper.getPropertyId("cluster_name", "HostRoles");
  private static final PropertyId HOST_COMPONENT_HOST_NAME_PROPERTY_ID      = PropertyHelper.getPropertyId("host_name", "HostRoles");
  private static final PropertyId HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("component_name", "HostRoles");
  private static final String     GANGLIA_SERVER                            = "GANGLIA_SERVER";
  private static final String     GANGLIA_SERVER_OLD                        = "GANGLIA_MONITOR_SERVER";

  /**
   * The map of resource providers.
   */
  private final Map<Resource.Type, ResourceProvider> resourceProviders = new HashMap<Resource.Type, ResourceProvider>();

  /**
   * The map of lists of property providers.
   */
  private final Map<Resource.Type,List<PropertyProvider>> propertyProviders = new HashMap<Resource.Type, List<PropertyProvider>>();

  private final JMXHostProvider     jmxHostProvider = new DefaultJMXHostProvider();
  private final GangliaHostProvider gangliaHostProvider = new DefaultGangliaHostProvider(jmxHostProvider);

  @Inject
  private AmbariManagementController managementController;


  // ----- Constructors ------------------------------------------------------

  /**
   * Create a default provider module.
   */
  public DefaultProviderModule() {
    if (managementController == null) {
      managementController = AmbariServer.getController();
    }
  }


  // ----- ProviderModule ----------------------------------------------------

  @Override
  public ResourceProvider getResourceProvider(Resource.Type type) {
    if (!propertyProviders.containsKey(type)) {
      createResourceProvider(type);
    }
    return resourceProviders.get(type);
  }

  @Override
  public List<PropertyProvider> getPropertyProviders(Resource.Type type) {

    if (!propertyProviders.containsKey(type)) {
      createPropertyProviders(type);
    }
    return propertyProviders.get(type);
  }


  // ----- utility methods ---------------------------------------------------

  protected void putResourceProvider(Resource.Type type, ResourceProvider resourceProvider) {
    resourceProviders.put( type , resourceProvider);
  }

  protected void createResourceProvider(Resource.Type type) {
    ResourceProvider resourceProvider =
        ResourceProviderImpl.getResourceProvider(type, PropertyHelper.getPropertyIds(type),
            PropertyHelper.getKeyPropertyIds(type), managementController);

    putResourceProvider(type, resourceProvider);
  }

  protected void putPropertyProviders(Resource.Type type, List<PropertyProvider> providers) {
    propertyProviders.put(type, providers);
  }

  protected void createPropertyProviders(Resource.Type type) {

    List<PropertyProvider> providers = new LinkedList<PropertyProvider>();

    URLStreamProvider streamProvider = new URLStreamProvider();

    switch (type){
      case Host :
        providers.add(new GangliaPropertyProvider(
            PropertyHelper.getGangliaPropertyIds(type),
            streamProvider,
            gangliaHostProvider,
            PropertyHelper.getPropertyId("cluster_name", "Hosts"),
            PropertyHelper.getPropertyId("host_name", "Hosts"),
            null));
        break;
      case Component :
        providers.add(new JMXPropertyProvider(
            PropertyHelper.getJMXPropertyIds(type),
            streamProvider,
            jmxHostProvider,
            PropertyHelper.getPropertyId("cluster_name", "ServiceComponentInfo"),
            null,
            PropertyHelper.getPropertyId("component_name", "ServiceComponentInfo")));

        providers.add(new GangliaPropertyProvider(
            PropertyHelper.getGangliaPropertyIds(type),
            streamProvider,
            gangliaHostProvider,
            PropertyHelper.getPropertyId("cluster_name", "ServiceComponentInfo"),
            null,
            PropertyHelper.getPropertyId("component_name", "ServiceComponentInfo")));
        break;
      case HostComponent:
        providers.add(new JMXPropertyProvider(
            PropertyHelper.getJMXPropertyIds(type),
            streamProvider,
            jmxHostProvider,
            PropertyHelper.getPropertyId("cluster_name", "HostRoles"),
            PropertyHelper.getPropertyId("host_name", "HostRoles"),
            PropertyHelper.getPropertyId("component_name", "HostRoles")));

        providers.add(new GangliaPropertyProvider(
            PropertyHelper.getGangliaPropertyIds(type),
            streamProvider,
            gangliaHostProvider,
            PropertyHelper.getPropertyId("cluster_name", "HostRoles"),
            PropertyHelper.getPropertyId("host_name", "HostRoles"),
            PropertyHelper.getPropertyId("component_name", "HostRoles")));
        break;
      default :
        break;
    }
    putPropertyProviders(type, providers);
  }

  private class DefaultJMXHostProvider implements JMXHostProvider {

    /**
     * The map of hosts.
     */
    private Map<String, Map<String, String>> hostMapping;
    private Map<String, Map<String, String>> hostComponentMapping;

    private boolean init = false;


    @Override
    public String getHostName(String clusterName, String componentName) {
      if (!init) {
        init = true;
        getHostMap();
      }
      return hostComponentMapping.get(clusterName).get(componentName);
    }

    @Override
    public Map<String, String> getHostMapping(String clusterName) {
      if (!init) {
        init = true;
        getHostMap();
      }

      return hostMapping.get(clusterName);
    }

    protected void getHostMap() {
      ResourceProvider provider = getResourceProvider(Resource.Type.Cluster);
      Request          request  = PropertyHelper.getReadRequest(CLUSTER_NAME_PROPERTY_ID);

      try {
        Set<Resource> clusters = provider.getResources(request, null);
        provider = getResourceProvider(Resource.Type.Host);
        request  = PropertyHelper.getReadRequest(HOST_NAME_PROPERTY_ID, HOST_IP_PROPERTY_ID, HOST_ATTRIBUTES_PROPERTY_ID);

        ObjectMapper mapper = new ObjectMapper();

        hostMapping = new HashMap<String, Map<String, String>>();
        hostComponentMapping = new HashMap<String, Map<String, String>>();

        for (Resource cluster : clusters) {

          String clusterName = (String) cluster.getPropertyValue(CLUSTER_NAME_PROPERTY_ID);

          Predicate predicate = new PredicateBuilder().property(HOST_CLUSTER_NAME_PROPERTY_ID).equals(clusterName).toPredicate();

          Set<Resource> hosts = provider.getResources(request, predicate);

          Map<String, String> map = hostMapping.get(clusterName);

          if (map == null) {
            map = new HashMap<String, String>();
            hostMapping.put(clusterName, map);
          }

          for (Resource host : hosts) {
            String attributes = (String) host.getPropertyValue(HOST_ATTRIBUTES_PROPERTY_ID);
            if (attributes != null && !attributes.startsWith("[]")) {
              try {
                Map<String, String> attributeMap = mapper.readValue(attributes, new TypeReference<Map<String, String>>() {});
                map.put(attributeMap.get("privateFQDN"), attributeMap.get("publicFQDN"));
              } catch (IOException e) {
                throw new IllegalStateException("Can't read hosts " + attributes, e);
              }
            } else {
              String hostName = (String) host.getPropertyValue(HOST_NAME_PROPERTY_ID);
              String ip = (String) host.getPropertyValue(HOST_IP_PROPERTY_ID);
              map.put(hostName, ip);
            }
          }

          request = PropertyHelper.getReadRequest(HOST_COMPONENT_HOST_NAME_PROPERTY_ID,
              HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID);

          predicate = new PredicateBuilder().property(HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID).equals(clusterName).toPredicate();

          provider = getResourceProvider(Resource.Type.HostComponent);

          Set<Resource> hostComponents = provider.getResources(request, predicate);

          Map<String, String> cmap = hostComponentMapping.get(clusterName);

          if (cmap == null) {
            cmap = new HashMap<String, String>();
            hostComponentMapping.put(clusterName, cmap);
          }

          for (Resource hostComponent : hostComponents) {
            String componentName = (String) hostComponent.getPropertyValue(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID);
            String hostName      = (String) hostComponent.getPropertyValue(HOST_COMPONENT_HOST_NAME_PROPERTY_ID);

            if (componentName.equals("NAMENODE")) {
              cmap.put(componentName, map.get(hostName));
            }
          }
        }
      } catch (AmbariException e) {
        //TODO
      }
    }
  }

  private class DefaultGangliaHostProvider implements GangliaHostProvider {

    /**
     * The host name of the Ganglia collector.
     */
    private Map<String, String> gangliaCollectorHostName;

    /**
     * Map of hosts to Ganglia cluster names.
     */
    private Map<String, Map<String, String>> gangliaHostClusterMap;

    private final JMXHostProvider hostProvider;

    private boolean init = false;

    private DefaultGangliaHostProvider(JMXHostProvider hostProvider) {
      this.hostProvider = hostProvider;
    }

    @Override
    public String getGangliaCollectorHostName(String clusterName) {
      if (!init) {
        init = true;
        getGangliaCollectorHost();
      }
      return gangliaCollectorHostName.get(clusterName);
    }

    @Override
    public Map<String, String> getGangliaHostClusterMap(String clusterName) {
      if (!init) {
        init = true;
        getGangliaCollectorHost();
      }
      return gangliaHostClusterMap.get(clusterName);
    }

    protected void getGangliaCollectorHost() {

      ResourceProvider provider = getResourceProvider(Resource.Type.Cluster);
      Request          request  = PropertyHelper.getReadRequest(CLUSTER_NAME_PROPERTY_ID);

      try {
        Set<Resource> clusters = provider.getResources(request, null);

        provider = getResourceProvider(Resource.Type.HostComponent);
        request  = PropertyHelper.getReadRequest(HOST_COMPONENT_HOST_NAME_PROPERTY_ID,
            HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID);

        gangliaHostClusterMap = new HashMap<String, Map<String, String>>();
        gangliaCollectorHostName = new HashMap<String, String>();

        for (Resource cluster : clusters) {


          String clusterName = (String) cluster.getPropertyValue(CLUSTER_NAME_PROPERTY_ID);

          Predicate predicate = new PredicateBuilder().property(HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID).equals(clusterName).toPredicate();

          Set<Resource> hostComponents = provider.getResources(request, predicate);

          Map<String, String> hostClusterMap = gangliaHostClusterMap.get(clusterName);

          if (hostClusterMap == null) {
            hostClusterMap = new HashMap<String, String>();
            gangliaHostClusterMap.put(clusterName, hostClusterMap);
          }

          for (Resource hostComponent : hostComponents) {
            String componentName = (String) hostComponent.getPropertyValue(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID);
            String hostName      = (String) hostComponent.getPropertyValue(HOST_COMPONENT_HOST_NAME_PROPERTY_ID);

            String gangliaCluster = GangliaPropertyProvider.GANGLIA_CLUSTER_NAMES.get(componentName);
            if (gangliaCluster != null) {
              if (!hostClusterMap.containsKey(hostName)) {
                hostClusterMap.put(hostName, gangliaCluster);
              }
            }
            if (componentName.equals(GANGLIA_SERVER) || componentName.equals(GANGLIA_SERVER_OLD)) {
              gangliaCollectorHostName.put(clusterName, hostProvider.getHostMapping(clusterName).get(hostName));
            }
          }
        }
      } catch (AmbariException e) {
        //TODO
      }
    }
  }
}
