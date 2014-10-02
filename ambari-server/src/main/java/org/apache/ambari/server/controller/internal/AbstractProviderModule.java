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
import org.apache.ambari.server.controller.metrics.MetricsHostProvider;
import org.apache.ambari.server.controller.nagios.NagiosPropertyProvider;
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
import org.apache.ambari.server.state.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An abstract provider module implementation.
 */
public abstract class AbstractProviderModule implements ProviderModule, ResourceProviderObserver, JMXHostProvider, GangliaHostProvider, MetricsHostProvider {

  private static final int PROPERTY_REQUEST_CONNECT_TIMEOUT = 5000;
  private static final int PROPERTY_REQUEST_READ_TIMEOUT    = 10000;
  // nagios can take longer on big clusters
  private static final int NAGIOS_READ_TIMEOUT              = 30000;

  private static final String CLUSTER_NAME_PROPERTY_ID                  = PropertyHelper.getPropertyId("Clusters", "cluster_name");
  private static final String HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID   = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
  private static final String HOST_COMPONENT_HOST_NAME_PROPERTY_ID      = PropertyHelper.getPropertyId("HostRoles", "host_name");
  private static final String HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "component_name");
  private static final String GANGLIA_SERVER                            = "GANGLIA_SERVER";
  private static final String PROPERTIES_CATEGORY = "properties";
  private static final Map<Service.Type, String> serviceConfigVersions = new ConcurrentHashMap<Service.Type, String>();
  private static final Map<Service.Type, String> serviceConfigTypes = new EnumMap<Service.Type, String>(Service.Type.class);
  private static final Map<Service.Type, Map<String, String[]>> serviceDesiredProperties = new EnumMap<Service.Type, Map<String, String[]>>(Service.Type.class);
  private static final Map<String, Service.Type> componentServiceMap = new HashMap<String, Service.Type>();
  
  private static final Map<String, Map<String, String[]>> jmxDesiredProperties = new HashMap<String, Map<String,String[]>>();
  private volatile Map<String, String> clusterCoreSiteConfigVersionMap = new HashMap<String, String>();
  private volatile Map<String, String> clusterJmxProtocolMap = new HashMap<String, String>();
  
  static {
    serviceConfigTypes.put(Service.Type.HDFS, "hdfs-site");
    serviceConfigTypes.put(Service.Type.MAPREDUCE, "mapred-site");
    serviceConfigTypes.put(Service.Type.HBASE, "hbase-site");
    serviceConfigTypes.put(Service.Type.YARN, "yarn-site");
    serviceConfigTypes.put(Service.Type.MAPREDUCE2, "mapred-site");
 
    componentServiceMap.put("NAMENODE", Service.Type.HDFS);
    componentServiceMap.put("DATANODE", Service.Type.HDFS);
    componentServiceMap.put("JOBTRACKER", Service.Type.MAPREDUCE);
    componentServiceMap.put("TASKTRACKER", Service.Type.MAPREDUCE);
    componentServiceMap.put("HBASE_MASTER", Service.Type.HBASE);
    componentServiceMap.put("RESOURCEMANAGER", Service.Type.YARN);
    componentServiceMap.put("NODEMANAGER", Service.Type.YARN);
    componentServiceMap.put("HISTORYSERVER", Service.Type.MAPREDUCE2);

    Map<String, String[]> initPropMap = new HashMap<String, String[]>();
    initPropMap.put("NAMENODE", new String[] {"dfs.http.address", "dfs.namenode.http-address"});
    initPropMap.put("DATANODE", new String[] {"dfs.datanode.http.address"});
    serviceDesiredProperties.put(Service.Type.HDFS, initPropMap);
    
    initPropMap = new HashMap<String, String[]>();
    initPropMap.put("JOBTRACKER", new String[] {"mapred.job.tracker.http.address"});
    initPropMap.put("TASKTRACKER", new String[] {"mapred.task.tracker.http.address"});
    serviceDesiredProperties.put(Service.Type.MAPREDUCE, initPropMap);
    
    initPropMap = new HashMap<String, String[]>();
    initPropMap.put("HBASE_MASTER", new String[] {"hbase.master.info.port"});
    serviceDesiredProperties.put(Service.Type.HBASE, initPropMap);

    initPropMap = new HashMap<String, String[]>();
    initPropMap.put("RESOURCEMANAGER", new String[] {"yarn.resourcemanager.webapp.address"});
    initPropMap.put("NODEMANAGER", new String[] {"yarn.nodemanager.webapp.address"});
    serviceDesiredProperties.put(Service.Type.YARN, initPropMap);

    initPropMap = new HashMap<String, String[]>();
    initPropMap.put("HISTORYSERVER", new String[] {"mapreduce.jobhistory.webapp.address"});
    serviceDesiredProperties.put(Service.Type.MAPREDUCE2, initPropMap);

    initPropMap = new HashMap<String, String[]>();
    initPropMap.put("NAMENODE", new String[] {"hadoop.ssl.enabled"});
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
  AmbariManagementController managementController;

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
  private final Map<String, Map<String, String>> jmxPortMap =
    new HashMap<String, Map<String, String>>();

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
    if (!resourceProviders.containsKey(type)) {
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

    if (type == Resource.Type.Cluster ||
        type == Resource.Type.Host ||
        type == Resource.Type.HostComponent) {
      resetInit();
    }
  }


  // ----- MetricsHostProvider ---------------------------------------------------

  @Override
  public String getHostName(String clusterName, String componentName) throws SystemException {
    checkInit();
    return clusterHostComponentMap.get(clusterName).get(componentName);
  }

  @Override
  public Set<String> getHostNames(String clusterName, String componentName) {
    Set<String> hosts = null;
    try {
      Cluster cluster = managementController.getClusters().getCluster(clusterName);
      String serviceName = managementController.findServiceName(cluster, componentName);
      hosts = cluster.getService(serviceName).getServiceComponent(componentName).getServiceComponentHosts().keySet();
    } catch (Exception e) {
      LOG.warn("Exception in getting host names for jmx metrics: ", e);
    }
    return hosts;
  }

  // ----- JMXHostProvider ---------------------------------------------------

  @Override
  public String getPort(String clusterName, String componentName) throws SystemException {
    // Parent map need not be synchronized
    Map<String, String> clusterJmxPorts = jmxPortMap.get(clusterName);
    if (clusterJmxPorts == null) {
      synchronized (jmxPortMap) {
        clusterJmxPorts = jmxPortMap.get(clusterName);
        if (clusterJmxPorts == null) {
          clusterJmxPorts = new ConcurrentHashMap<String, String>();
          jmxPortMap.put(clusterName, clusterJmxPorts);
        }
      }
    }
    Service.Type service = componentServiceMap.get(componentName);
    
    if (service != null) {
      try {
        String currVersion = getDesiredConfigVersion(clusterName, serviceConfigTypes.get(service));
        String oldVersion = serviceConfigVersions.get(service);

        // We only update port map when a config version updates,
        // Since concurrent thread access is expected we err on the side of
        // performance with a ConcurrentHashMap and maybe get default/existing
        // ports for a few calls.
        if (!currVersion.equals(oldVersion) ||
          !clusterJmxPorts.containsKey(componentName)) {

          serviceConfigVersions.put(service, currVersion);
          
          Map<String, String> portMap = getDesiredConfigMap(clusterName,
            currVersion, serviceConfigTypes.get(service),
            serviceDesiredProperties.get(service));
          
          for (Entry<String, String> entry : portMap.entrySet()) {
            // portString will be null if the property defined for the component doesn't exist
            // this will trigger using the default port for the component
            String portString = getPortString(entry.getValue());
            if (null != portString)
              clusterJmxPorts.put(entry.getKey(), portString);
          }
        }
      } catch (Exception e) {
        LOG.error("Exception initializing jmx port maps. " + e);
      }
    }

    LOG.debug("jmxPortMap -> " + jmxPortMap);
    return clusterJmxPorts.get(componentName);
  }

  /**Post process property value. If value has one ore some substrings
   * started with "${" and ended with "}" these substrings will replace
   * with properties from current propertiesMap. It is doing recursively.
   * @param key - properties name
   * @param value - properties value
   * @param properties - map with properties
   */
  private String postProcessPropertyValue(String key, String value, Map<String, String> properties, Set<String> prevProps) {
      if (value != null && key != null && value.contains("${")){
          if (prevProps == null) prevProps = new HashSet<String>();
          if (prevProps.contains(key)){
            return value;
          }
          prevProps.add(key);
          String refValueString = value;
          Map<String, String> refMap = new HashMap<String, String>();
          while(refValueString.contains("${")) {
              int startValueRef = refValueString.indexOf("${") + 2;
              int endValueRef = refValueString.indexOf('}');
              String valueRef = refValueString.substring(startValueRef, endValueRef);
              refValueString = refValueString.substring(endValueRef+1);
              String trueValue = postProcessPropertyValue(valueRef, properties.get(valueRef), properties, prevProps);
              if (trueValue != null){
               refMap.put("${"+valueRef+ '}', trueValue);
              } 
          }
          for (Entry<String, String> entry : refMap.entrySet()){
            refValueString = entry.getValue();
            value = value.replace(entry.getKey(), refValueString);
          }
          properties.put(key, value);
    }
    return value;
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
        configuration);

    if (type.isInternalType()) {
      switch (type.getInternalType()) {
        case Cluster:
          providers.add(createGangliaReportPropertyProvider(
              type,
              streamProvider,
              ComponentSSLConfiguration.instance(),
              this,
              PropertyHelper.getPropertyId("Clusters", "cluster_name")));
          providers.add(new NagiosPropertyProvider(type,
              new URLStreamProvider(
                PROPERTY_REQUEST_CONNECT_TIMEOUT, NAGIOS_READ_TIMEOUT,
                configuration),
              "Clusters/cluster_name",
              "Clusters/version"));
          break;
        case Service:
          providers.add(new NagiosPropertyProvider(type,
              new URLStreamProvider(
                PROPERTY_REQUEST_CONNECT_TIMEOUT, NAGIOS_READ_TIMEOUT,
                configuration),
              "ServiceInfo/cluster_name",
              "ServiceInfo/service_name"));
          break;
        case Host:
          providers.add(createGangliaHostPropertyProvider(
              type,
              streamProvider,
              ComponentSSLConfiguration.instance(),
              this,
              PropertyHelper.getPropertyId("Hosts", "cluster_name"),
              PropertyHelper.getPropertyId("Hosts", "host_name")
          ));
          providers.add(new NagiosPropertyProvider(type,
              new URLStreamProvider(
                PROPERTY_REQUEST_CONNECT_TIMEOUT, NAGIOS_READ_TIMEOUT,
                configuration),
              "Hosts/cluster_name",
              "Hosts/host_name"));
          break;
        case Component: {
          // TODO as we fill out stack metric definitions, these can be phased out
          PropertyProvider jpp = createJMXPropertyProvider(
              type,
              streamProvider,
              this,
              this,
              PropertyHelper.getPropertyId("ServiceComponentInfo", "cluster_name"),
              null,
              PropertyHelper.getPropertyId("ServiceComponentInfo", "component_name"),
              PropertyHelper.getPropertyId("ServiceComponentInfo", "state"));

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
              this,
              streamProvider,
              PropertyHelper.getPropertyId("ServiceComponentInfo", "cluster_name"),
              null,
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
              this,
              PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
              PropertyHelper.getPropertyId("HostRoles", "host_name"),
              PropertyHelper.getPropertyId("HostRoles", "component_name"),
              PropertyHelper.getPropertyId("HostRoles", "state"));

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
        default:
          break;
      }
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

  private void initProviderMaps() throws SystemException {
    ResourceProvider provider = getResourceProvider(Resource.Type.Cluster);

    Set<String> propertyIds = new HashSet<String>();
    propertyIds.add(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID);

    Map<String, String> requestInfoProperties = new HashMap<String, String>();
    requestInfoProperties.put(ClusterResourceProvider.GET_IGNORE_PERMISSIONS_PROPERTY_ID, "true");

    Request request = PropertyHelper.getReadRequest(propertyIds, requestInfoProperties, null);

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

      Set<String> propertyIds = new HashSet<String>();
      propertyIds.add(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID);
      propertyIds.add(ClusterResourceProvider.CLUSTER_DESIRED_CONFIGS_PROPERTY_ID);

      Map<String, String> requestInfoProperties = new HashMap<String, String>();
      requestInfoProperties.put(ClusterResourceProvider.GET_IGNORE_PERMISSIONS_PROPERTY_ID, "true");

      Request readRequest = PropertyHelper.getReadRequest(propertyIds, requestInfoProperties, null);

      clusterResource = clusterResourceProvider.getResources(readRequest, basePredicate);
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
            versionTag = config.getTag();
          }
        }
      }
    }
    return versionTag;
  }

  private Map<String, String> getDesiredConfigMap(String clusterName,
      String versionTag, String configType, Map<String, String[]> keys) throws
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
       Map<String, String> evaluatedProperties = null;
        for (Entry<String,String[]> entry : keys.entrySet()) {
          String propName = null;
          String value = null;
          
          for (String pname : entry.getValue()) {
            propName = pname;
            value = (String) res.getPropertyValue(PropertyHelper.getPropertyId(
                PROPERTIES_CATEGORY, pname));
            if (null != value)
              break;
          }
          
          if (value != null && value.contains("${")) {
            if (evaluatedProperties == null){
              evaluatedProperties = new HashMap<String, String>();
              Map<String, Object> properties = res.getPropertiesMap().get(PROPERTIES_CATEGORY);
              for (Map.Entry<String, Object> subentry : properties.entrySet()) {
                String keyString = subentry.getKey();
                Object object = subentry.getValue();
                String valueString;
                if (object != null && object instanceof String){
                  valueString = (String)object;
                  evaluatedProperties.put(keyString, valueString);
                  postProcessPropertyValue(keyString, valueString, evaluatedProperties, null);
                }
              }              
            }
          }
          value = postProcessPropertyValue(propName, value, evaluatedProperties, null);
          LOG.debug("PROPERTY -> key: " + propName + ", " + "value: " + value);
          
          mConfigs.put(entry.getKey(), value);
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
                                                     MetricsHostProvider metricsHostProvider,
                                                     String clusterNamePropertyId,
                                                     String hostNamePropertyId,
                                                     String componentNamePropertyId,
                                                     String statePropertyId) {
    
    return new JMXPropertyProvider(PropertyHelper.getJMXPropertyIds(type), streamProvider,
        jmxHostProvider, metricsHostProvider, clusterNamePropertyId, hostNamePropertyId,
                    componentNamePropertyId, statePropertyId);
  }

  /**
   * Create the Ganglia report property provider for the given type.
   */
  private PropertyProvider createGangliaReportPropertyProvider( Resource.Type type, StreamProvider streamProvider,
                                                                ComponentSSLConfiguration configuration,
                                                                GangliaHostProvider hostProvider,
                                                                String clusterNamePropertyId) {
    
    return new GangliaReportPropertyProvider(PropertyHelper.getGangliaPropertyIds(type), streamProvider,
          configuration, hostProvider, clusterNamePropertyId);
  }

  /**
   * Create the Ganglia host property provider for the given type.
   */
  private PropertyProvider createGangliaHostPropertyProvider( Resource.Type type, StreamProvider streamProvider,
                                                              ComponentSSLConfiguration configuration,
                                                              GangliaHostProvider hostProvider,
                                                              String clusterNamePropertyId,
                                                              String hostNamePropertyId) {
    return new GangliaHostPropertyProvider(PropertyHelper.getGangliaPropertyIds(type), streamProvider,
          configuration, hostProvider, clusterNamePropertyId, hostNamePropertyId);
  }

  /**
   * Create the Ganglia component property provider for the given type.
   */
  private PropertyProvider createGangliaComponentPropertyProvider( Resource.Type type, StreamProvider streamProvider,
                                                                   ComponentSSLConfiguration configuration,
                                                                   GangliaHostProvider hostProvider,
                                                                   String clusterNamePropertyId,
                                                                   String componentNamePropertyId) {
    return new GangliaComponentPropertyProvider(PropertyHelper.getGangliaPropertyIds(type),
              streamProvider, configuration, hostProvider, clusterNamePropertyId, componentNamePropertyId);
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

    return new GangliaHostComponentPropertyProvider(PropertyHelper.getGangliaPropertyIds(type), streamProvider,
          configuration, hostProvider, clusterNamePropertyId, hostNamePropertyId, componentNamePropertyId);
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
      LOG.info("Exception while detecting JMX protocol for clusterName = " + clusterName +
          ", componentName = " + componentName,  e);
      LOG.info("Defaulting JMX to HTTP protocol for  for clusterName = " + clusterName +
          ", componentName = " + componentName +
          componentName);
      jmxProtocolString = "http";
    }
    if (jmxProtocolString == null) {
      LOG.debug("Detected JMX protocol is null for clusterName = " + clusterName +
          ", componentName = " + componentName);
      LOG.debug("Defaulting JMX to HTTP protocol for  for clusterName = " + clusterName +
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
