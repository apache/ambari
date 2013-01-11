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

import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.ganglia.GangliaComponentPropertyProvider;
import org.apache.ambari.server.controller.ganglia.GangliaHostComponentPropertyProvider;
import org.apache.ambari.server.controller.ganglia.GangliaHostPropertyProvider;
import org.apache.ambari.server.controller.ganglia.GangliaReportPropertyProvider;
import org.apache.ambari.server.controller.ganglia.GangliaHostProvider;
import org.apache.ambari.server.controller.jmx.JMXHostProvider;
import org.apache.ambari.server.controller.jmx.JMXPropertyProvider;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.AmbariManagementController;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The default provider module implementation.
 */
public class DefaultProviderModule implements ProviderModule, ResourceProviderObserver, JMXHostProvider, GangliaHostProvider {

  private static final String HOST_CLUSTER_NAME_PROPERTY_ID             = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
  private static final String HOST_NAME_PROPERTY_ID                     = PropertyHelper.getPropertyId("Hosts", "host_name");
  private static final String HOST_IP_PROPERTY_ID                       = PropertyHelper.getPropertyId("Hosts", "ip");
  private static final String HOST_ATTRIBUTES_PROPERTY_ID               = PropertyHelper.getPropertyId("Hosts", "attributes");
  private static final String CLUSTER_NAME_PROPERTY_ID                  = PropertyHelper.getPropertyId("Clusters", "cluster_name");
  private static final String HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID   = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
  private static final String HOST_COMPONENT_HOST_NAME_PROPERTY_ID      = PropertyHelper.getPropertyId("HostRoles", "host_name");
  private static final String HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "component_name");
  private static final String GANGLIA_SERVER                            = "GANGLIA_SERVER";
  private static final String GANGLIA_SERVER_OLD                        = "GANGLIA_MONITOR_SERVER";

  /**
   * The map of resource providers.
   */
  private final Map<Resource.Type, ResourceProvider> resourceProviders = new HashMap<Resource.Type, ResourceProvider>();

  /**
   * The map of lists of property providers.
   */
  private final Map<Resource.Type,List<PropertyProvider>> propertyProviders = new HashMap<Resource.Type, List<PropertyProvider>>();

  @Inject
  private AmbariManagementController managementController;

  /**
   * The map of hosts.
   */
  private Map<String, Map<String, String>> clusterHostMap;

  private Map<String, Map<String, String>> clusterHostComponentMap;

  /**
   * The host name of the Ganglia collector.
   */
  private Map<String, String> clusterGangliaCollectorMap;


  private volatile boolean initialized = false;




  protected final static Logger LOG =
      LoggerFactory.getLogger(DefaultProviderModule.class);


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


  // ----- ResourceProviderObserver ------------------------------------------

  @Override
  public void update(ResourceProviderEvent event) {
    Resource.Type type = event.getResourceType();

    if (type == Resource.Type.Cluster ||
        type == Resource.Type.Host ||
        type == Resource.Type.HostComponent) {
      resetInit();
    }
  }


  // ----- JMXHostProvider ---------------------------------------------------

  @Override
  public String getHostName(String clusterName, String componentName) throws SystemException {
    checkInit();
    return clusterHostComponentMap.get(clusterName).get(componentName);
  }

  @Override
  public Map<String, String> getHostMapping(String clusterName) throws SystemException {
    checkInit();
    return clusterHostMap.get(clusterName);
  }


  // ----- GangliaHostProvider -----------------------------------------------

  @Override
  public String getGangliaCollectorHostName(String clusterName) throws SystemException {
    checkInit();
    return clusterGangliaCollectorMap.get(clusterName);
  }


  // ----- utility methods ---------------------------------------------------

  protected void putResourceProvider(Resource.Type type, ResourceProvider resourceProvider) {
    resourceProviders.put( type , resourceProvider);
  }

  protected void createResourceProvider(Resource.Type type) {
    ResourceProvider resourceProvider =
        ResourceProviderImpl.getResourceProvider(type, PropertyHelper.getPropertyIds(type),
            PropertyHelper.getKeyPropertyIds(type), managementController);

    if (resourceProvider instanceof ObservableResourceProvider) {
      ((ObservableResourceProvider)resourceProvider).addObserver(this);
    }

    putResourceProvider(type, resourceProvider);
  }

  protected void putPropertyProviders(Resource.Type type, List<PropertyProvider> providers) {
    propertyProviders.put(type, providers);
  }

  protected void createPropertyProviders(Resource.Type type) {

    List<PropertyProvider> providers = new LinkedList<PropertyProvider>();

    URLStreamProvider streamProvider = new URLStreamProvider();

    switch (type){
      case Cluster :
        providers.add(new GangliaReportPropertyProvider(
            PropertyHelper.getGangliaPropertyIds(type).get("*"),
            streamProvider,
            this,
            PropertyHelper.getPropertyId("Clusters", "cluster_name")));
        break;
      case Host :
        providers.add(new GangliaHostPropertyProvider(
            PropertyHelper.getGangliaPropertyIds(type),
            streamProvider,
            this,
            PropertyHelper.getPropertyId("Hosts", "cluster_name"),
            PropertyHelper.getPropertyId("Hosts", "host_name")
        ));
        break;
      case Component :
        providers.add(new JMXPropertyProvider(
            PropertyHelper.getJMXPropertyIds(type),
            streamProvider,
            this,
            PropertyHelper.getPropertyId("ServiceComponentInfo", "cluster_name"),
            null,
            PropertyHelper.getPropertyId("ServiceComponentInfo", "component_name")));

        providers.add(new GangliaComponentPropertyProvider(
            PropertyHelper.getGangliaPropertyIds(type),
            streamProvider,
            this,
            PropertyHelper.getPropertyId("ServiceComponentInfo", "cluster_name"),
            PropertyHelper.getPropertyId("ServiceComponentInfo", "component_name")));
        break;
      case HostComponent:
        providers.add(new JMXPropertyProvider(
            PropertyHelper.getJMXPropertyIds(type),
            streamProvider,
            this,
            PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
            PropertyHelper.getPropertyId("HostRoles", "host_name"),
            PropertyHelper.getPropertyId("HostRoles", "component_name")));

        providers.add(new GangliaHostComponentPropertyProvider(
            PropertyHelper.getGangliaPropertyIds(type),
            streamProvider,
            this,
            PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
            PropertyHelper.getPropertyId("HostRoles", "host_name"),
            PropertyHelper.getPropertyId("HostRoles", "component_name")));
        break;
      default :
        break;
    }
    putPropertyProviders(type, providers);
  }

  private void checkInit() throws SystemException{
    if (!initialized) {
      synchronized (this) {
        if (!initialized) {
          initProviderMaps();
          initialized = true;
        }
      }
    }
  }

  private void resetInit() {
    if (initialized) {
      synchronized (this) {
        initialized = false;
      }
    }
  }

  private void initProviderMaps() throws SystemException{
    ResourceProvider provider = getResourceProvider(Resource.Type.Cluster);
    Request          request  = PropertyHelper.getReadRequest(CLUSTER_NAME_PROPERTY_ID);

    try {
      Set<Resource> clusters = provider.getResources(request, null);

      clusterHostMap             = new HashMap<String, Map<String, String>>();
      clusterHostComponentMap    = new HashMap<String, Map<String, String>>();
      clusterGangliaCollectorMap = new HashMap<String, String>();

      for (Resource cluster : clusters) {

        String clusterName = (String) cluster.getPropertyValue(CLUSTER_NAME_PROPERTY_ID);

        // initialize the host map from the known hosts...
        provider = getResourceProvider(Resource.Type.Host);
        request  = PropertyHelper.getReadRequest(HOST_NAME_PROPERTY_ID, HOST_IP_PROPERTY_ID,
            HOST_ATTRIBUTES_PROPERTY_ID);

        Predicate predicate   = new PredicateBuilder().property(HOST_CLUSTER_NAME_PROPERTY_ID).
            equals(clusterName).toPredicate();

        Set<Resource>       hosts   = provider.getResources(request, predicate);
        Map<String, String> hostMap = clusterHostMap.get(clusterName);

        if (hostMap == null) {
          hostMap = new HashMap<String, String>();
          clusterHostMap.put(clusterName, hostMap);
        }

        for (Resource host : hosts) {
          hostMap.put((String) host.getPropertyValue(HOST_NAME_PROPERTY_ID),
              (String) host.getPropertyValue(HOST_IP_PROPERTY_ID));
        }

        // initialize the host component map and Ganglia server from the known hosts components...
        provider = getResourceProvider(Resource.Type.HostComponent);

        request = PropertyHelper.getReadRequest(HOST_COMPONENT_HOST_NAME_PROPERTY_ID,
            HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID);

        predicate = new PredicateBuilder().property(HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID).
            equals(clusterName).toPredicate();

        Set<Resource>       hostComponents   = provider.getResources(request, predicate);
        Map<String, String> hostComponentMap = clusterHostComponentMap.get(clusterName);

        if (hostComponentMap == null) {
          hostComponentMap = new HashMap<String, String>();
          clusterHostComponentMap.put(clusterName, hostComponentMap);
        }

        for (Resource hostComponent : hostComponents) {
          String componentName = (String) hostComponent.getPropertyValue(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID);
          String hostName      = (String) hostComponent.getPropertyValue(HOST_COMPONENT_HOST_NAME_PROPERTY_ID);

          hostComponentMap.put(componentName, hostMap.get(hostName));

          // record the Ganglia server for the current cluster
          if (componentName.equals(GANGLIA_SERVER) || componentName.equals(GANGLIA_SERVER_OLD)) {
            clusterGangliaCollectorMap.put(clusterName, clusterHostMap.get(clusterName).get(hostName));
          }
        }
      }
    } catch (UnsupportedPropertyException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Caught UnsupportedPropertyException while trying to get the host mappings.", e);
      }
      throw new SystemException("An exception occurred while initializing the host mappings: " + e, e);
    } catch (NoSuchResourceException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Caught NoSuchResourceException exception while trying to get the host mappings.", e);
      }
      throw new SystemException("An exception occurred while initializing the host mappings: " + e, e);
    } catch (NoSuchParentResourceException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Caught NoSuchParentResourceException exception while trying to get the host mappings.", e);
      }
      throw new SystemException("An exception occurred while initializing the host mappings: " + e, e);
    }
  }
}
