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
import org.apache.ambari.server.Role;
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.HostRequest;
import org.apache.ambari.server.controller.HostResponse;
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
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
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.State;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * An abstract provider module implementation.
 */
public abstract class AbstractProviderModule implements ProviderModule, ResourceProviderObserver, JMXHostProvider, GangliaHostProvider {

  private static final int PROPERTY_REQUEST_CONNECT_TIMEOUT = 5000;
  private static final int PROPERTY_REQUEST_READ_TIMEOUT    = 10000;

  private static final String CLUSTER_NAME_PROPERTY_ID                  = PropertyHelper.getPropertyId("Clusters", "cluster_name");
  private static final String CLUSTER_VERSION_PROPERTY_ID               = PropertyHelper.getPropertyId("Clusters", "version");
  private static final String HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID   = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
  private static final String HOST_COMPONENT_HOST_NAME_PROPERTY_ID      = PropertyHelper.getPropertyId("HostRoles", "host_name");
  private static final String HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "component_name");
  private static final String GANGLIA_SERVER                            = "GANGLIA_SERVER";
  private static final String PROPERTIES_CATEGORY = "properties";
  private static final Map<Service.Type, String> serviceConfigVersions =
    Collections.synchronizedMap(new HashMap<Service.Type, String>());
  private static final Map<Service.Type, String> serviceConfigTypes = new HashMap<Service.Type, String>();
  private static final Map<Service.Type, Map<String, String>> serviceDesiredProperties = new
    HashMap<Service.Type, Map<String, String>>();
  private static final Map<String, Service.Type> componentServiceMap = new
    HashMap<String, Service.Type>();
  
  private static final Map<String, Map<String, String>> jmxDesiredProperties = new HashMap<String, Map<String,String>>();
  private volatile Map<String, String> clusterCoreSiteConfigVersionMap = new HashMap<String, String>();
  private volatile Map<String, String> clusterJmxProtocolMap = new HashMap<String, String>();
  
  static {
    serviceConfigTypes.put(Service.Type.HDFS, "hdfs-site");
    serviceConfigTypes.put(Service.Type.MAPREDUCE, "mapred-site");
    serviceConfigTypes.put(Service.Type.HBASE, "hbase-site");
 
    componentServiceMap.put("NAMENODE", Service.Type.HDFS);
    componentServiceMap.put("DATANODE", Service.Type.HDFS);
    componentServiceMap.put("JOBTRACKER", Service.Type.MAPREDUCE);
    componentServiceMap.put("TASKTRACKER", Service.Type.MAPREDUCE);
    componentServiceMap.put("HBASE_MASTER", Service.Type.HBASE);

    Map<String, String> initPropMap = new HashMap<String, String>();
    initPropMap.put("NAMENODE", "dfs.http.address");
    initPropMap.put("DATANODE", "dfs.datanode.http.address");
    serviceDesiredProperties.put(Service.Type.HDFS, initPropMap);
    initPropMap = new HashMap<String, String>();
    initPropMap.put("JOBTRACKER", "mapred.job.tracker.http.address");
    initPropMap.put("TASKTRACKER", "mapred.task.tracker.http.address");
    serviceDesiredProperties.put(Service.Type.MAPREDUCE, initPropMap);
    initPropMap = new HashMap<String, String>();
    initPropMap.put("HBASE_MASTER", "hbase.master.info.port");
    serviceDesiredProperties.put(Service.Type.HBASE, initPropMap);
    
    initPropMap = new HashMap<String, String>();
    initPropMap.put("NAMENODE", "hadoop.ssl.enabled");
    jmxDesiredProperties.put("NAMENODE", initPropMap);
  }

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
   * Cluster versions.
   */
  private final Map<String, PropertyHelper.MetricsVersion> clusterVersionsMap =
      new HashMap<String, PropertyHelper.MetricsVersion>();

  /**
   * The map of host components.
   */
  private Map<String, Map<String, String>> clusterHostComponentMap;

  /**
   * The host name of the Ganglia collector.
   */
  private Map<String, String> clusterGangliaCollectorMap;

  /**
   * JMX ports read from the configs
   */
  private final Map<String, Map<String, String>> jmxPortMap = Collections
    .synchronizedMap(new HashMap<String, Map<String, String>>());

  private volatile boolean initialized = false;

  protected final static Logger LOG =
      LoggerFactory.getLogger(AbstractProviderModule.class);


  // ----- Constructors ------------------------------------------------------

  /**
   * Create a default provider module.
   */
  public AbstractProviderModule() {
    if (managementController == null) {
      managementController = AmbariServer.getController();
    }
  }


  // ----- ProviderModule ----------------------------------------------------

  @Override
  public ResourceProvider getResourceProvider(Resource.Type type) {
    if (!propertyProviders.containsKey(type)) {
      registerResourceProvider(type);
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

    if (type == Resource.Type.Cluster) {
      resetInit();
      updateClusterVersion();
    }
    if (type == Resource.Type.Host ||
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
  public String getPort(String clusterName, String componentName) throws
    SystemException {
    Map<String,String> clusterJmxPorts = jmxPortMap.get(clusterName);
    if (clusterJmxPorts == null) {
      synchronized (jmxPortMap) {
        clusterJmxPorts = jmxPortMap.get(clusterName);
        if (clusterJmxPorts == null) {
          clusterJmxPorts = new HashMap<String, String>();
          jmxPortMap.put(clusterName, clusterJmxPorts);
        }
      }
    }
    Service.Type service = componentServiceMap.get(componentName);
    if (service != null) {
      try {
        String currVersion = getDesiredConfigVersion(clusterName,
          serviceConfigTypes.get(service));

        String oldVersion = serviceConfigVersions.get(service);
        if (!currVersion.equals(oldVersion)) {
          serviceConfigVersions.put(service, currVersion);
          Map<String, String> portMap = getDesiredConfigMap(clusterName,
            currVersion, serviceConfigTypes.get(service),
            serviceDesiredProperties.get(service));
          for (String compName : portMap.keySet()) {
            clusterJmxPorts.put(compName, getPortString(portMap.get
              (compName)));
          }
        }
      } catch (Exception e) {
        LOG.error("Exception initializing jmx port maps. " + e);
      }
    }

    LOG.debug("jmxPortMap -> " + jmxPortMap);
    return clusterJmxPorts.get(componentName);
  }

  // ----- GangliaHostProvider -----------------------------------------------

  @Override
  public String getGangliaCollectorHostName(String clusterName) throws SystemException {
    checkInit();
    return clusterGangliaCollectorMap.get(clusterName);
  }
  
  @Override
  public boolean isGangliaCollectorHostLive(String clusterName) throws SystemException {

    if (clusterName == null) {
      return false;
    }

    HostResponse gangliaCollectorHost;

    try {

      final String gangliaCollectorHostName = getGangliaCollectorHostName(clusterName);

      HostRequest hostRequest = new HostRequest(gangliaCollectorHostName, clusterName, Collections.<String, String>emptyMap());
      Set<HostResponse> hosts = HostResourceProvider.getHosts(managementController, hostRequest);

      gangliaCollectorHost = hosts.size() == 1 ? hosts.iterator().next() : null;
    } catch (AmbariException e) {
      LOG.debug("Error checking of Ganglia server host live status: ", e);
      return false;
    }
    
    //Cluster without Ganglia
    return gangliaCollectorHost != null &&
        !gangliaCollectorHost.getHostState().equals(HostState.HEARTBEAT_LOST.name());
  }
  
  @Override
  public boolean isGangliaCollectorComponentLive(String clusterName) throws SystemException {
    if (clusterName == null) {
      return false;
    }

    ServiceComponentHostResponse gangliaCollectorHostComponent;

    try {
      final String gangliaCollectorHostName = getGangliaCollectorHostName(clusterName);

      ServiceComponentHostRequest componentRequest = new ServiceComponentHostRequest(clusterName, "GANGLIA",
                                                                                     Role.GANGLIA_SERVER.name(),
                                                                                     gangliaCollectorHostName,
                                                                                     null);
      
      Set<ServiceComponentHostResponse> hostComponents =
          managementController.getHostComponents(Collections.singleton(componentRequest));

      gangliaCollectorHostComponent = hostComponents.size() == 1 ? hostComponents.iterator().next() : null;
    } catch (AmbariException e) {
      LOG.debug("Error checking of Ganglia server host component state: ", e);
      return false;
    }
    
    //Cluster without Ganglia
    return gangliaCollectorHostComponent != null &&
        gangliaCollectorHostComponent.getLiveState().equals(State.STARTED.name());
  }


  // ----- utility methods ---------------------------------------------------

  protected abstract ResourceProvider createResourceProvider(Resource.Type type);

  protected void registerResourceProvider(Resource.Type type) {
    ResourceProvider resourceProvider = createResourceProvider(type);

    if (resourceProvider instanceof ObservableResourceProvider) {
      ((ObservableResourceProvider)resourceProvider).addObserver(this);
    }

    putResourceProvider(type, resourceProvider);
  }

  protected void putResourceProvider(Resource.Type type, ResourceProvider resourceProvider) {
    resourceProviders.put(type, resourceProvider);
  }

  protected void putPropertyProviders(Resource.Type type, List<PropertyProvider> providers) {
    propertyProviders.put(type, providers);
  }

  protected void createPropertyProviders(Resource.Type type) {

    List<PropertyProvider> providers = new LinkedList<PropertyProvider>();

    ComponentSSLConfiguration configuration = ComponentSSLConfiguration.instance();
    URLStreamProvider streamProvider = new URLStreamProvider(
        PROPERTY_REQUEST_CONNECT_TIMEOUT, PROPERTY_REQUEST_READ_TIMEOUT,
        configuration.getTruststorePath(), configuration.getTruststorePassword(), configuration.getTruststoreType());

    switch (type){
      case Cluster :
        providers.add(createGangliaReportPropertyProvider(
            type,
            streamProvider,
            ComponentSSLConfiguration.instance(),
            this,
            PropertyHelper.getPropertyId("Clusters", "cluster_name")));
        break;
      case Host :
        providers.add(createGangliaHostPropertyProvider(
            type,
            streamProvider,
            ComponentSSLConfiguration.instance(),
            this,
            PropertyHelper.getPropertyId("Hosts", "cluster_name"),
            PropertyHelper.getPropertyId("Hosts", "host_name")
        ));
        break;
      case Component : {
        // TODO as we fill out stack metric definitions, these can be phased out
        PropertyProvider jpp = createJMXPropertyProvider(
            type,
            streamProvider,
            this,
            PropertyHelper.getPropertyId("ServiceComponentInfo", "cluster_name"),
            null,
            PropertyHelper.getPropertyId("ServiceComponentInfo", "component_name"),
            PropertyHelper.getPropertyId("ServiceComponentInfo", "state"),
            Collections.singleton("STARTED"));

        PropertyProvider gpp = createGangliaComponentPropertyProvider(
            type,
            streamProvider,
            ComponentSSLConfiguration.instance(),
            this,
            PropertyHelper.getPropertyId("ServiceComponentInfo", "cluster_name"),
            PropertyHelper.getPropertyId("ServiceComponentInfo", "component_name"));
        
        providers.add(new StackDefinedPropertyProvider(
            type,
            this,
            this,
            streamProvider,
            PropertyHelper.getPropertyId("ServiceComponentInfo", "cluster_name"),
            PropertyHelper.getPropertyId("ServiceComponentInfo", "host_name"),
            PropertyHelper.getPropertyId("ServiceComponentInfo", "component_name"),
            PropertyHelper.getPropertyId("ServiceComponentInfo", "state"),
            jpp,
            gpp));
        }
        break;
      case HostComponent: {
        // TODO as we fill out stack metric definitions, these can be phased out
        PropertyProvider jpp = createJMXPropertyProvider(
            type,
            streamProvider,
            this,
            PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
            PropertyHelper.getPropertyId("HostRoles", "host_name"),
            PropertyHelper.getPropertyId("HostRoles", "component_name"),
            PropertyHelper.getPropertyId("HostRoles", "state"),
            Collections.singleton("STARTED"));

        PropertyProvider gpp = createGangliaHostComponentPropertyProvider(
            type,
            streamProvider,
            ComponentSSLConfiguration.instance(),
            this,
            PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
            PropertyHelper.getPropertyId("HostRoles", "host_name"),
            PropertyHelper.getPropertyId("HostRoles", "component_name"));
        
        providers.add(new StackDefinedPropertyProvider(
            type,
            this,
            this,
            streamProvider,
            PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
            PropertyHelper.getPropertyId("HostRoles", "host_name"),
            PropertyHelper.getPropertyId("HostRoles", "component_name"),
            PropertyHelper.getPropertyId("HostRoles", "state"),
            jpp,
            gpp));
        }
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

  /**
   * Update a map of known cluster names to version of JMX metrics.  The JMX metrics version is based on the
   * HDP version of the cluster.
   */
  private void updateClusterVersion() {
    synchronized (clusterVersionsMap) {
      clusterVersionsMap.clear();

      ResourceProvider provider = getResourceProvider(Resource.Type.Cluster);
      Request request = PropertyHelper.getReadRequest(CLUSTER_NAME_PROPERTY_ID, CLUSTER_VERSION_PROPERTY_ID);

      try {
        Set<Resource> clusters = provider.getResources(request, null);

        for (Resource cluster : clusters) {
          String clusterVersion = (String) cluster.getPropertyValue(CLUSTER_VERSION_PROPERTY_ID);

          PropertyHelper.MetricsVersion version =  clusterVersion.startsWith("HDP-1") ?
              PropertyHelper.MetricsVersion.HDP1 : PropertyHelper.MetricsVersion.HDP2;

          clusterVersionsMap.put(
              (String) cluster.getPropertyValue(CLUSTER_NAME_PROPERTY_ID),
              version);
        }
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Caught exception while trying to get the cluster versions.", e);
        }
      }
    }
  }

  private void initProviderMaps() throws SystemException {
    ResourceProvider provider = getResourceProvider(Resource.Type.Cluster);
    Request          request  = PropertyHelper.getReadRequest(CLUSTER_NAME_PROPERTY_ID);

    try {
      jmxPortMap.clear();
      Set<Resource> clusters = provider.getResources(request, null);

      clusterHostComponentMap    = new HashMap<String, Map<String, String>>();
      clusterGangliaCollectorMap = new HashMap<String, String>();

      for (Resource cluster : clusters) {

        String clusterName = (String) cluster.getPropertyValue(CLUSTER_NAME_PROPERTY_ID);

        // initialize the host component map and Ganglia server from the known hosts components...
        provider = getResourceProvider(Resource.Type.HostComponent);

        request = PropertyHelper.getReadRequest(HOST_COMPONENT_HOST_NAME_PROPERTY_ID,
            HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID);

        Predicate predicate = new PredicateBuilder().property(HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID).
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

          hostComponentMap.put(componentName, hostName);

          // record the Ganglia server for the current cluster
          if (componentName.equals(GANGLIA_SERVER)) {
            clusterGangliaCollectorMap.put(clusterName, hostName);
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

  private String getPortString(String value) {
    return value != null && value.contains(":") ? value.substring
      (value.lastIndexOf(":") + 1, value.length()) : value;
  }

  private String getDesiredConfigVersion(String clusterName,
                                         String configType) throws
      NoSuchParentResourceException, UnsupportedPropertyException,
      SystemException {

    // Get config version tag
    ResourceProvider clusterResourceProvider = getResourceProvider(Resource
      .Type.Cluster);
    Predicate basePredicate = new PredicateBuilder().property
      (ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID).equals(clusterName)
      .toPredicate();

    Set<Resource> clusterResource = null;
    try {
      clusterResource = clusterResourceProvider.getResources(
        PropertyHelper.getReadRequest(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID,
          ClusterResourceProvider.CLUSTER_DESIRED_CONFIGS_PROPERTY_ID), basePredicate);
    } catch (NoSuchResourceException e) {
      LOG.error("Resource for the desired config not found. " + e);
    }

    String versionTag = "version1";
    if (clusterResource != null) {
      for (Resource resource : clusterResource) {
        Map<String, Object> configs =
        resource.getPropertiesMap().get(ClusterResourceProvider
          .CLUSTER_DESIRED_CONFIGS_PROPERTY_ID);
        if (configs != null) {
          DesiredConfig config = (DesiredConfig) configs.get(configType);
          if (config != null) {
            versionTag = config.getVersion();
          }
        }
      }
    }
    return versionTag;
  }

  private Map<String, String> getDesiredConfigMap(String clusterName,
      String versionTag, String configType, Map<String, String> keys) throws
        NoSuchParentResourceException, UnsupportedPropertyException,
        SystemException {
    // Get desired configs based on the tag
    ResourceProvider configResourceProvider = getResourceProvider(Resource.Type.Configuration);
    Predicate configPredicate = new PredicateBuilder().property
      (ConfigurationResourceProvider.CONFIGURATION_CLUSTER_NAME_PROPERTY_ID).equals(clusterName).and()
      .property(ConfigurationResourceProvider.CONFIGURATION_CONFIG_TYPE_PROPERTY_ID).equals(configType).and()
      .property(ConfigurationResourceProvider.CONFIGURATION_CONFIG_TAG_PROPERTY_ID).equals(versionTag).toPredicate();
    Set<Resource> configResources;
    try {
      configResources = configResourceProvider.getResources
        (PropertyHelper.getReadRequest(ConfigurationResourceProvider.CONFIGURATION_CLUSTER_NAME_PROPERTY_ID,
          ConfigurationResourceProvider.CONFIGURATION_CONFIG_TYPE_PROPERTY_ID,
          ConfigurationResourceProvider.CONFIGURATION_CONFIG_TAG_PROPERTY_ID), configPredicate);
    } catch (NoSuchResourceException e) {
      LOG.info("Resource for the desired config not found. " + e);
      return Collections.emptyMap();
    }
    Map<String, String> mConfigs = new HashMap<String, String>();
    if (configResources != null) {
      for (Resource res : configResources) {
        for (String key : keys.keySet()) {
          String value = (String) res.getPropertyValue
            (PropertyHelper.getPropertyId(PROPERTIES_CATEGORY, keys.get(key)));
          LOG.debug("PROPERTY -> key: " + keys.get(key) + ", " +
            "value: " + value);

          mConfigs.put(key, value);
        }
      }
    }
    return mConfigs;
  }

  /**
   * Create the JMX property provider for the given type.
   */
  private PropertyProvider createJMXPropertyProvider(Resource.Type type, StreamProvider streamProvider,
                                                     JMXHostProvider jmxHostProvider,
                                                     String clusterNamePropertyId,
                                                     String hostNamePropertyId,
                                                     String componentNamePropertyId,
                                                     String statePropertyId,
                                                     Set<String> healthyStates) {
    updateClusterVersion();

    Map<PropertyHelper.MetricsVersion, AbstractPropertyProvider> providers =
        new HashMap<PropertyHelper.MetricsVersion, AbstractPropertyProvider>();
    AbstractPropertyProvider lastProvider = null;

    for (PropertyHelper.MetricsVersion version : PropertyHelper.MetricsVersion.values()) {

      lastProvider = new JMXPropertyProvider(PropertyHelper.getJMXPropertyIds(type, version), streamProvider,
          jmxHostProvider, clusterNamePropertyId, hostNamePropertyId, componentNamePropertyId, statePropertyId, healthyStates);
      providers.put(version, lastProvider);
    }

    return new VersioningPropertyProvider(clusterVersionsMap, providers, lastProvider, clusterNamePropertyId);
  }

  /**
   * Create the Ganglia report property provider for the given type.
   */
  private PropertyProvider createGangliaReportPropertyProvider( Resource.Type type, StreamProvider streamProvider,
                                                                ComponentSSLConfiguration configuration,
                                                                GangliaHostProvider hostProvider,
                                                                String clusterNamePropertyId) {
    updateClusterVersion();

    Map<PropertyHelper.MetricsVersion, AbstractPropertyProvider> providers =
        new HashMap<PropertyHelper.MetricsVersion, AbstractPropertyProvider>();
    AbstractPropertyProvider lastProvider = null;

    for (PropertyHelper.MetricsVersion version : PropertyHelper.MetricsVersion.values()) {

      lastProvider = new GangliaReportPropertyProvider(PropertyHelper.getGangliaPropertyIds(type, version), streamProvider,
          configuration, hostProvider, clusterNamePropertyId);
      providers.put(version, lastProvider);
    }

    return new VersioningPropertyProvider(clusterVersionsMap, providers, lastProvider, clusterNamePropertyId);
  }

  /**
   * Create the Ganglia host property provider for the given type.
   */
  private PropertyProvider createGangliaHostPropertyProvider( Resource.Type type, StreamProvider streamProvider,
                                                              ComponentSSLConfiguration configuration,
                                                              GangliaHostProvider hostProvider,
                                                              String clusterNamePropertyId,
                                                              String hostNamePropertyId) {
    updateClusterVersion();

    Map<PropertyHelper.MetricsVersion, AbstractPropertyProvider> providers =
        new HashMap<PropertyHelper.MetricsVersion, AbstractPropertyProvider>();
    AbstractPropertyProvider lastProvider = null;

    for (PropertyHelper.MetricsVersion version : PropertyHelper.MetricsVersion.values()) {

      lastProvider = new GangliaHostPropertyProvider(PropertyHelper.getGangliaPropertyIds(type, version), streamProvider,
          configuration, hostProvider, clusterNamePropertyId, hostNamePropertyId);
      providers.put(version, lastProvider);
    }

    return new VersioningPropertyProvider(clusterVersionsMap, providers, lastProvider, clusterNamePropertyId);
  }

  /**
   * Create the Ganglia component property provider for the given type.
   */
  private PropertyProvider createGangliaComponentPropertyProvider( Resource.Type type, StreamProvider streamProvider,
                                                                   ComponentSSLConfiguration configuration,
                                                                   GangliaHostProvider hostProvider,
                                                                   String clusterNamePropertyId,
                                                                   String componentNamePropertyId) {
    updateClusterVersion();

    Map<PropertyHelper.MetricsVersion, AbstractPropertyProvider> providers =
        new HashMap<PropertyHelper.MetricsVersion, AbstractPropertyProvider>();
    AbstractPropertyProvider lastProvider = null;

    for (PropertyHelper.MetricsVersion version : PropertyHelper.MetricsVersion.values()) {
      lastProvider =
          new GangliaComponentPropertyProvider(PropertyHelper.getGangliaPropertyIds(type, version),
              streamProvider, configuration, hostProvider, clusterNamePropertyId, componentNamePropertyId);

      providers.put(version, lastProvider);
    }

    return new VersioningPropertyProvider(clusterVersionsMap, providers, lastProvider, clusterNamePropertyId);
  }


  /**
   * Create the Ganglia host component property provider for the given type.
   */
  private PropertyProvider createGangliaHostComponentPropertyProvider( Resource.Type type, StreamProvider streamProvider,
                                                                       ComponentSSLConfiguration configuration,
                                                                       GangliaHostProvider hostProvider,
                                                                       String clusterNamePropertyId,
                                                                       String hostNamePropertyId,
                                                                       String componentNamePropertyId) {
    updateClusterVersion();

    Map<PropertyHelper.MetricsVersion, AbstractPropertyProvider> providers =
        new HashMap<PropertyHelper.MetricsVersion, AbstractPropertyProvider>();
    AbstractPropertyProvider lastProvider = null;

    for (PropertyHelper.MetricsVersion version : PropertyHelper.MetricsVersion.values()) {

      lastProvider = new GangliaHostComponentPropertyProvider(PropertyHelper.getGangliaPropertyIds(type, version), streamProvider,
          configuration, hostProvider, clusterNamePropertyId, hostNamePropertyId, componentNamePropertyId);
      providers.put(version, lastProvider);
    }

    return new VersioningPropertyProvider(clusterVersionsMap, providers, lastProvider, clusterNamePropertyId);
  }
  
  @Override
  public String getJMXProtocol(String clusterName, String componentName) {
    String jmxProtocolString = clusterJmxProtocolMap.get(clusterName);
    try {
      String newCoreSiteConfigVersion = getDesiredConfigVersion(clusterName, "core-site");
      String cachedCoreSiteConfigVersion = clusterCoreSiteConfigVersionMap.get(clusterName);
      if (!newCoreSiteConfigVersion.equals(cachedCoreSiteConfigVersion)) {
        clusterCoreSiteConfigVersionMap.put(clusterName, newCoreSiteConfigVersion);
        
        // Getting protocolMap for NAMENODE as it is the same property hadoop.ssl.enabled for all components
        Map<String, String> protocolMap = getDesiredConfigMap(
            clusterName,
            newCoreSiteConfigVersion, "core-site",
            jmxDesiredProperties.get("NAMENODE")); 
        jmxProtocolString = getJMXProtocolString(protocolMap.get("NAMENODE"));
        clusterJmxProtocolMap.put(clusterName, jmxProtocolString);
      }

    } catch (Exception e) {
      LOG.error("Exception while detecting JMX protocol for clusterName = " + clusterName +
          ", componentName = " + componentName,  e);
      LOG.error("Defaulting JMX to HTTP protocol for  for clusterName = " + clusterName +
          ", componentName = " + componentName +
          componentName);
      jmxProtocolString = "http";
    }
    if (jmxProtocolString == null) {
      LOG.error("Detected JMX protocol is null for clusterName = " + clusterName +
          ", componentName = " + componentName);
      LOG.error("Defaulting JMX to HTTP protocol for  for clusterName = " + clusterName +
          ", componentName = " + componentName +
          componentName);
      jmxProtocolString = "http";
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("JMXProtocol = " + jmxProtocolString + ", for clusterName=" + clusterName +
          ", componentName = " + componentName);
    }
    return jmxProtocolString;
  }

  private String getJMXProtocolString(String value) {
    return Boolean.valueOf(value) ? "https" : "http";
  }
  
}
