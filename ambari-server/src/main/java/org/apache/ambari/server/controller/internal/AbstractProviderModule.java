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

import static org.apache.ambari.server.controller.metrics.MetricsServiceProvider.MetricsService.GANGLIA;
import static org.apache.ambari.server.controller.metrics.MetricsServiceProvider.MetricsService.TIMELINE_METRICS;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.jmx.JMXHostProvider;
import org.apache.ambari.server.controller.metrics.MetricHostProvider;
import org.apache.ambari.server.controller.metrics.MetricPropertyProviderFactory;
import org.apache.ambari.server.controller.metrics.MetricsCollectorHAManager;
import org.apache.ambari.server.controller.metrics.MetricsPropertyProvider;
import org.apache.ambari.server.controller.metrics.MetricsReportPropertyProvider;
import org.apache.ambari.server.controller.metrics.MetricsServiceProvider;
import org.apache.ambari.server.controller.metrics.timeline.cache.TimelineMetricCacheProvider;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.ProviderModule;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.apache.ambari.server.events.ClusterConfigChangedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Service;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;

/**
 * An abstract provider module implementation.
 */
public abstract class AbstractProviderModule implements ProviderModule,
    ResourceProviderObserver, JMXHostProvider, MetricHostProvider,
    MetricsServiceProvider {

  private static final int PROPERTY_REQUEST_CONNECT_TIMEOUT = 5000;
  private static final int PROPERTY_REQUEST_READ_TIMEOUT    = 10000;

  private static final String CLUSTER_NAME_PROPERTY_ID                  = PropertyHelper.getPropertyId("Clusters", "cluster_name");
  private static final String HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID   = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
  private static final String HOST_COMPONENT_HOST_NAME_PROPERTY_ID      = PropertyHelper.getPropertyId("HostRoles", "host_name");
  private static final String HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "component_name");
  private static final String GANGLIA_SERVER                            = "GANGLIA_SERVER";
  private static final String METRIC_SERVER                             = "METRICS_COLLECTOR";
  private static final String PROPERTIES_CATEGORY = "properties";
  private static final Map<String, String> serviceConfigVersions = new ConcurrentHashMap<String, String>();
  private static final Map<Service.Type, String> serviceConfigTypes = new EnumMap<Service.Type, String>(Service.Type.class);
  private static final Map<Service.Type, Map<String, String[]>> serviceDesiredProperties = new EnumMap<Service.Type, Map<String, String[]>>(Service.Type.class);
  private static final Map<String, Service.Type> componentServiceMap = new HashMap<String, Service.Type>();

  private static final Map<String, List<HttpPropertyProvider.HttpPropertyRequest>> HTTP_PROPERTY_REQUESTS = new HashMap<>();

  private static final String PROPERTY_HDFS_HTTP_POLICY_VALUE_HTTPS_ONLY = "HTTPS_ONLY";

  private static final String COLLECTOR_DEFAULT_PORT = "6188";
  private static boolean vipHostConfigPresent = false;

  private static final Map<String, Map<String, String[]>> jmxDesiredProperties = new HashMap<String, Map<String, String[]>>();
  private static final Map<String, Map<String, String[]>> jmxDesiredRpcSuffixProperties = new ConcurrentHashMap<>();
  private volatile Map<String, Map<String, Map<String, String>>> jmxDesiredRpcSuffixes = new HashMap<String, Map<String, Map<String,String>>>();
  private volatile Map<String, String> clusterHdfsSiteConfigVersionMap = new HashMap<String, String>();
  private volatile Map<String, String> clusterJmxProtocolMap = new ConcurrentHashMap<>();
  private volatile String clusterMetricServerPort = null;
  private volatile String clusterMetricServerVipPort = null;
  private volatile String clusterMetricserverVipHost = null;

  static {
    serviceConfigTypes.put(Service.Type.HDFS, "hdfs-site");
    serviceConfigTypes.put(Service.Type.HBASE, "hbase-site");
    serviceConfigTypes.put(Service.Type.YARN, "yarn-site");
    serviceConfigTypes.put(Service.Type.MAPREDUCE2, "mapred-site");
    serviceConfigTypes.put(Service.Type.AMBARI_METRICS, "ams-site");

    componentServiceMap.put("NAMENODE", Service.Type.HDFS);
    componentServiceMap.put("DATANODE", Service.Type.HDFS);
    componentServiceMap.put("JOURNALNODE", Service.Type.HDFS);
    componentServiceMap.put("HBASE_MASTER", Service.Type.HBASE);
    componentServiceMap.put("HBASE_REGIONSERVER", Service.Type.HBASE);
    componentServiceMap.put("RESOURCEMANAGER", Service.Type.YARN);
    componentServiceMap.put("NODEMANAGER", Service.Type.YARN);
    componentServiceMap.put("HISTORYSERVER", Service.Type.MAPREDUCE2);

    Map<String, String[]> initPropMap = new HashMap<String, String[]>();
    initPropMap.put("NAMENODE", new String[]{"dfs.http.address", "dfs.namenode.http-address"});
    initPropMap.put("NAMENODE-HTTPS", new String[]{"dfs.namenode.https-address", "dfs.https.port"});
    initPropMap.put("NAMENODE-HA", new String[]{"dfs.namenode.http-address.%s.%s"});
    initPropMap.put("NAMENODE-HTTPS-HA", new String[]{"dfs.namenode.https-address.%s.%s"});
    initPropMap.put("DATANODE", new String[]{"dfs.datanode.http.address"});
    initPropMap.put("DATANODE-HTTPS", new String[]{"dfs.datanode.https.address"});
    initPropMap.put("JOURNALNODE-HTTPS", new String[]{"dfs.journalnode.https-address"});
    initPropMap.put("JOURNALNODE", new String[]{"dfs.journalnode.http-address"});
    serviceDesiredProperties.put(Service.Type.HDFS, initPropMap);


    initPropMap = new HashMap<String, String[]>();
    initPropMap.put("HBASE_MASTER", new String[]{"hbase.master.info.port"});
    initPropMap.put("HBASE_REGIONSERVER", new String[]{"hbase.regionserver.info.port"});
    initPropMap.put("HBASE_MASTER-HTTPS", new String[]{"hbase.master.info.port"});
    initPropMap.put("HBASE_REGIONSERVER-HTTPS", new String[]{"hbase.regionserver.info.port"});
    serviceDesiredProperties.put(Service.Type.HBASE, initPropMap);

    initPropMap = new HashMap<String, String[]>();
    initPropMap.put("RESOURCEMANAGER", new String[]{"yarn.resourcemanager.webapp.address"});
    initPropMap.put("RESOURCEMANAGER-HTTPS", new String[]{"yarn.resourcemanager.webapp.https.address"});
    initPropMap.put("NODEMANAGER", new String[]{"yarn.nodemanager.webapp.address"});
    initPropMap.put("NODEMANAGER-HTTPS", new String[]{"yarn.nodemanager.webapp.https.address"});
    serviceDesiredProperties.put(Service.Type.YARN, initPropMap);

    initPropMap = new HashMap<String, String[]>();
    initPropMap.put("HISTORYSERVER", new String[]{"mapreduce.jobhistory.webapp.address"});
    initPropMap.put("HISTORYSERVER-HTTPS", new String[]{"mapreduce.jobhistory.webapp.https.address"});
    serviceDesiredProperties.put(Service.Type.MAPREDUCE2, initPropMap);

    initPropMap = new HashMap<String, String[]>();
    initPropMap.put("NAMENODE", new String[]{"dfs.http.policy"});
    jmxDesiredProperties.put("NAMENODE", initPropMap);

    initPropMap = new HashMap<String, String[]>();
    initPropMap.put("DATANODE", new String[]{"dfs.http.policy"});
    jmxDesiredProperties.put("DATANODE", initPropMap);

    initPropMap = new HashMap<String, String[]>();
    initPropMap.put("JOURNALNODE", new String[]{"dfs.http.policy"});
    jmxDesiredProperties.put("JOURNALNODE", initPropMap);

    initPropMap = new HashMap<String, String[]>();
    initPropMap.put("RESOURCEMANAGER", new String[]{"yarn.http.policy"});
    jmxDesiredProperties.put("RESOURCEMANAGER", initPropMap);

    initPropMap = new HashMap<String, String[]>();
    initPropMap.put("HBASE_MASTER", new String[]{"hbase.http.policy"});
    jmxDesiredProperties.put("HBASE_MASTER", initPropMap);

    initPropMap = new HashMap<String, String[]>();
    initPropMap.put("HBASE_REGIONSERVER", new String[]{"hbase.http.policy"});
    jmxDesiredProperties.put("HBASE_REGIONSERVER", initPropMap);

    initPropMap = new HashMap<String, String[]>();
    initPropMap.put("NODEMANAGER", new String[]{"yarn.http.policy"});
    jmxDesiredProperties.put("NODEMANAGER", initPropMap);

    initPropMap = new HashMap<String, String[]>();
    initPropMap.put("HISTORYSERVER", new String[]{"mapreduce.jobhistory.http.policy"});
    jmxDesiredProperties.put("HISTORYSERVER", initPropMap);

    initPropMap = new HashMap<String, String[]>();
    initPropMap.put("client", new String[]{"dfs.namenode.rpc-address"});
    initPropMap.put("datanode", new String[]{"dfs.namenode.servicerpc-address"});
    initPropMap.put("healthcheck", new String[]{"dfs.namenode.lifeline.rpc-address"});
    jmxDesiredRpcSuffixProperties.put("NAMENODE", initPropMap);

    initPropMap = new HashMap<String, String[]>();
    initPropMap.put("client", new String[]{"dfs.namenode.rpc-address.%s.%s"});
    initPropMap.put("datanode", new String[]{"dfs.namenode.servicerpc-address.%s.%s"});
    initPropMap.put("healthcheck", new String[]{"dfs.namenode.lifeline.rpc-address.%s.%s"});
    jmxDesiredRpcSuffixProperties.put("NAMENODE-HA", initPropMap);

    HTTP_PROPERTY_REQUESTS.put("RESOURCEMANAGER",
        Collections.<HttpPropertyProvider.HttpPropertyRequest>singletonList(new ResourceManagerHttpPropertyRequest()));

    HTTP_PROPERTY_REQUESTS.put("ATLAS_SERVER",
        Collections.<HttpPropertyProvider.HttpPropertyRequest>singletonList(new AtlasServerHttpPropertyRequest()));
  }

  /**
   * The map of resource providers.
   */
  private final Map<Resource.Type, ResourceProvider> resourceProviders = new HashMap<Resource.Type, ResourceProvider>();

  /**
   * The map of lists of property providers.
   */
  private final Map<Resource.Type, List<PropertyProvider>> propertyProviders = new HashMap<Resource.Type, List<PropertyProvider>>();

  @Inject
  AmbariManagementController managementController;

  @Inject
  TimelineMetricCacheProvider metricCacheProvider;

  @Inject
  MetricsCollectorHAManager metricsCollectorHAManager;

  /**
   * A factory used to retrieve Guice-injected instances of a metric
   * {@link PropertyProvider}.
   */
  @Inject
  private MetricPropertyProviderFactory metricPropertyProviderFactory;

  /**
   * Used to respond to the following events:
   * <ul>
   * <li>{@link ClusterConfigChangedEvent}
   */
  @Inject
  protected AmbariEventPublisher eventPublisher;


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
  private final Map<String, ConcurrentMap<String, ConcurrentMap<String, String>> >jmxPortMap =
      Collections.synchronizedMap(new HashMap<String, ConcurrentMap<String, ConcurrentMap<String, String>>>());

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

    if (metricCacheProvider == null && managementController != null) {
      metricCacheProvider = managementController.getTimelineMetricCacheProvider();
    }

    if (metricPropertyProviderFactory == null && managementController != null) {
      metricPropertyProviderFactory = managementController.getMetricPropertyProviderFactory();
    }

    if (null == eventPublisher && null != managementController) {
      eventPublisher = managementController.getAmbariEventPublisher();
      eventPublisher.register(this);
    }

    if (null == metricsCollectorHAManager && null != managementController) {
      metricsCollectorHAManager = managementController.getMetricsCollectorHAManager();
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
  public List<PropertyProvider>  getPropertyProviders(Resource.Type type) {

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

  // ----- MetricsServiceProvider ---------------------------------------------

  /**
   * Get type of Metrics system installed.
   * @return @MetricsService, null if none found.
   */
  @Override
  public MetricsService getMetricsServiceType() {
    try {
      checkInit();
    } catch (SystemException e) {
      LOG.error("Exception during checkInit.", e);
    }

    if (!metricsCollectorHAManager.isEmpty()) {
      return TIMELINE_METRICS;
    } else if (!clusterGangliaCollectorMap.isEmpty()) {
      return GANGLIA;
    }
    return null;
  }

  // ----- MetricsHostProvider ------------------------------------------------

  @Override
  public String getCollectorHostName(String clusterName, MetricsService service)
    throws SystemException {

    checkInit();
    if (service.equals(GANGLIA)) {
      return clusterGangliaCollectorMap.get(clusterName);
    } else if (service.equals(TIMELINE_METRICS)) {
      return getMetricsCollectorHostName(clusterName);
    }
    return null;
  }

  private String getMetricsCollectorHostName(String clusterName)
    throws SystemException {
    try {
      // try to get vip properties from cluster-env
      String configType = "cluster-env";
      String currentConfigVersion = getDesiredConfigVersion(clusterName, configType);
      String oldConfigVersion = serviceConfigVersions.get(configType);
      if (!currentConfigVersion.equals(oldConfigVersion)) {
        vipHostConfigPresent = false;
        serviceConfigVersions.put(configType, currentConfigVersion);
        Map<String, String> configProperties = getDesiredConfigMap
          (clusterName, currentConfigVersion, configType,
            Collections.singletonMap("METRICS_COLLECTOR",
              new String[]{"metrics_collector_vip_host"}));

        if (!configProperties.isEmpty()) {
          clusterMetricserverVipHost = configProperties.get("METRICS_COLLECTOR");
          if (clusterMetricserverVipHost != null) {
            LOG.info("Setting Metrics Collector Vip Host : " + clusterMetricserverVipHost);
            vipHostConfigPresent = true;
          }
        }
        // updating the port value, because both vip properties are stored in
        // cluster-env
        configProperties = getDesiredConfigMap
          (clusterName, currentConfigVersion, configType,
            Collections.singletonMap("METRICS_COLLECTOR",
              new String[]{"metrics_collector_vip_port"}));

        if (!configProperties.isEmpty()) {
          clusterMetricServerVipPort = configProperties.get("METRICS_COLLECTOR");
        }
      }
    } catch (NoSuchParentResourceException | UnsupportedPropertyException e) {
      LOG.warn("Failed to retrieve collector hostname.", e);
    }

    //If vip config not present
    //  If current collector host is null or if the host or the host component not live
    //    Update clusterMetricCollectorMap with a live metric collector host.
    String currentCollectorHost = null;
    if (!vipHostConfigPresent) {
      currentCollectorHost = metricsCollectorHAManager.getCollectorHost(clusterName);
      }
    LOG.debug("Cluster Metrics Vip Host : " + clusterMetricserverVipHost);

    return (clusterMetricserverVipHost != null) ? clusterMetricserverVipHost : currentCollectorHost;
  }

  @Override
  public String getCollectorPort(String clusterName, MetricsService service) throws SystemException {
    checkInit();
    if (service.equals(GANGLIA)) {
      return "80"; // Not called by the provider
    } else if (service.equals(TIMELINE_METRICS)) {
      try {
        if (clusterMetricServerVipPort == null) {
          String configType = serviceConfigTypes.get(Service.Type.AMBARI_METRICS);
          String currentConfigVersion = getDesiredConfigVersion(clusterName, configType);
          String oldConfigVersion = serviceConfigVersions.get(configType);
          if (!currentConfigVersion.equals(oldConfigVersion)) {
            serviceConfigVersions.put(configType, currentConfigVersion);

            Map<String, String> configProperties = getDesiredConfigMap(clusterName,
              currentConfigVersion, configType,
              Collections.singletonMap("METRICS_COLLECTOR",
                new String[]{"timeline.metrics.service.webapp.address"}));

            if (!configProperties.isEmpty()) {
              clusterMetricServerPort = getPortString(configProperties.get("METRICS_COLLECTOR"));
            } else {
              clusterMetricServerPort = COLLECTOR_DEFAULT_PORT;
            }
          }
        }
      } catch (NoSuchParentResourceException | UnsupportedPropertyException e) {
        LOG.warn("Failed to retrieve collector port.", e);
      }
    }
    return clusterMetricServerVipPort != null ? clusterMetricServerVipPort : clusterMetricServerPort;
  }

  @Override
  public boolean isCollectorHostLive(String clusterName, MetricsService service) throws SystemException {
    return metricsCollectorHAManager.isCollectorHostLive(clusterName);
  }

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

  @Override
  public boolean isCollectorComponentLive(String clusterName, MetricsService service) throws SystemException {

    final String collectorHostName = getCollectorHostName(clusterName, service);

    if (service.equals(GANGLIA)) {
      return HostStatusHelper.isHostComponentLive(managementController, clusterName, collectorHostName, "GANGLIA",
        Role.GANGLIA_SERVER.name());
    } else if (service.equals(TIMELINE_METRICS)) {
      return metricsCollectorHAManager.isCollectorComponentLive(clusterName);
    }
    return false;
  }

  // ----- JMXHostProvider ---------------------------------------------------

  @Override
  public String getPort(String clusterName, String componentName, String hostName) throws SystemException {
    return getPort(clusterName, componentName, hostName, false);
  }

  @Override
  public String getPort(String clusterName, String componentName, String hostName, boolean httpsEnabled) throws SystemException {
    // Parent map need not be synchronized
    ConcurrentMap<String, ConcurrentMap<String, String>> clusterJmxPorts = jmxPortMap.get(clusterName);
    if (clusterJmxPorts == null) {
      synchronized (jmxPortMap) {
        clusterJmxPorts = jmxPortMap.get(clusterName);
        if (clusterJmxPorts == null) {
          clusterJmxPorts = new ConcurrentHashMap<String, ConcurrentMap<String, String>>();
          jmxPortMap.put(clusterName, clusterJmxPorts);
        }
      }
    }
    Service.Type service = componentServiceMap.get(componentName);

    if (service != null) {
      try {
        String configType = serviceConfigTypes.get(service);
        String currVersion = getDesiredConfigVersion(clusterName, configType);
        String oldVersion = serviceConfigVersions.get(configType);

        // We only update port map when a config version updates,
        // Since concurrent thread access is expected we err on the side of
        // performance with a ConcurrentHashMap and maybe get default/existing
        // ports for a few calls.
        if (!StringUtils.equals(currVersion, oldVersion) ||
            !(clusterJmxPorts.containsKey(hostName) && clusterJmxPorts.get(hostName).containsKey(componentName))) {

          serviceConfigVersions.put(configType, currVersion);

          Map<String, Object> configProperties = getConfigProperties(
              clusterName,
              currVersion,
              serviceConfigTypes.get(service)
          );

          Map<String, String[]> componentPortsProperties = new HashMap<String, String[]>();
          componentPortsProperties.put(
              componentName,
              getPortProperties(service,
                  componentName,
                  hostName,
                  configProperties,
                  httpsEnabled
              )
          );

          Map<String, String> portMap = getDesiredConfigMap(clusterName,
              currVersion, serviceConfigTypes.get(service),
              componentPortsProperties, configProperties);

          for (Entry<String, String> entry : portMap.entrySet()) {
            // portString will be null if the property defined for the component doesn't exist
            // this will trigger using the default port for the component
            String portString = getPortString(entry.getValue());
            if (null != portString) {
              clusterJmxPorts.putIfAbsent(hostName, new ConcurrentHashMap<String, String>());
              clusterJmxPorts.get(hostName).put(entry.getKey(), portString);
            }
          }

          initRpcSuffixes(clusterName, componentName, configType, currVersion, hostName);
        }
      } catch (Exception e) {
        LOG.error("Exception initializing jmx port maps. ", e);
      }
    }

    LOG.debug("jmxPortMap -> " + jmxPortMap);

    ConcurrentMap<String, String> hostJmxPorts = clusterJmxPorts.get(hostName);
    if (hostJmxPorts == null) {
      LOG.debug("Jmx ports not loaded from properties: clusterName={}, componentName={}, hostName={}, " +
          "clusterJmxPorts={}, jmxPortMap.get(clusterName)={}",
          clusterName, componentName, hostName, clusterJmxPorts, jmxPortMap.get(clusterName));
      //returning null is acceptable in cases when property with port not present
      //or loading from property is not supported for specific component
      return null;
    }
    return hostJmxPorts.get(componentName);
  }

  /**
   * Computes properties that contains proper port for {@code componentName} on {@code hostName}. Must contain custom logic
   * for different configurations(like NAMENODE HA).
   * @param service service type
   * @param componentName component name
   * @param hostName host which contains requested component
   * @param properties properties which used for special cases(like NAMENODE HA)
   * @param httpsEnabled indicates if https enabled for component
   * @return property name that contain port for {@code componentName} on {@code hostName}
   */
  String[] getPortProperties(Service.Type service, String componentName, String hostName, Map<String, Object> properties, boolean httpsEnabled) {
    componentName = httpsEnabled ? componentName + "-HTTPS" : componentName;
    if(componentName.startsWith("NAMENODE") && properties.containsKey("dfs.internal.nameservices")) {
      componentName += "-HA";
      return getNamenodeHaProperty(properties, serviceDesiredProperties.get(service).get(componentName), hostName);
    }
    return serviceDesiredProperties.get(service).get(componentName);
  }

  private String[] getNamenodeHaProperty(Map<String, Object> properties, String pattern[], String hostName) {
    // iterate over nameservices and namenodes, to find out namenode http(s) property for concrete host
    for(String nameserviceId : ((String)properties.get("dfs.internal.nameservices")).split(",")) {
      if(properties.containsKey("dfs.ha.namenodes."+nameserviceId)) {
        for (String namenodeId : ((String)properties.get("dfs.ha.namenodes." + nameserviceId)).split(",")) {
          String propertyName = String.format(
            pattern[0],
            nameserviceId,
            namenodeId
          );
          if (properties.containsKey(propertyName)) {
            String propertyValue = (String)properties.get(propertyName);
            if (propertyValue.split(":")[0].equals(hostName)) {
              return new String[] {propertyName};
            }
          }
        }
      }
    }
    return pattern;
  }

  /**
   * Post process property value. If value has one ore some substrings
   * started with "${" and ended with "}" these substrings will replace
   * with properties from current propertiesMap. It is doing recursively.
   *
   * @param key        - properties name
   * @param value      - properties value
   * @param properties - map with properties
   */
  private String postProcessPropertyValue(String key, String value, Map<String, String> properties, Set<String> prevProps) {
    if (value != null && key != null && value.contains("${")) {
      if (prevProps == null) {
        prevProps = new HashSet<String>();
      }
      if (prevProps.contains(key)) {
        return value;
      }
      prevProps.add(key);
      String refValueString = value;
      Map<String, String> refMap = new HashMap<String, String>();
      while (refValueString.contains("${")) {
        int startValueRef = refValueString.indexOf("${") + 2;
        int endValueRef = refValueString.indexOf('}');
        String valueRef = refValueString.substring(startValueRef, endValueRef);
        refValueString = refValueString.substring(endValueRef + 1);
        String trueValue = postProcessPropertyValue(valueRef, properties.get(valueRef), properties, prevProps);
        if (trueValue != null) {
          refMap.put("${" + valueRef + '}', trueValue);
        }
      }
      for (Entry<String, String> entry : refMap.entrySet()) {
        refValueString = entry.getValue();
        value = value.replace(entry.getKey(), refValueString);
      }
      properties.put(key, value);
    }
    return value;
  }

  // ----- utility methods ---------------------------------------------------

  protected abstract ResourceProvider createResourceProvider(Resource.Type type);

  protected void registerResourceProvider(Resource.Type type) {
    ResourceProvider resourceProvider = createResourceProvider(type);

    if (resourceProvider instanceof ObservableResourceProvider) {
      ((ObservableResourceProvider) resourceProvider).addObserver(this);
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
          providers.add(createMetricsReportPropertyProvider(
            type,
            streamProvider,
            ComponentSSLConfiguration.instance(),
            this,
            this,
            PropertyHelper.getPropertyId("Clusters", "cluster_name")));
          providers.add(new AlertSummaryPropertyProvider(type,
            "Clusters/cluster_name", null));
          break;
        case Service:
          providers.add(new AlertSummaryPropertyProvider(type,
              "ServiceInfo/cluster_name", "ServiceInfo/service_name"));
          break;
        case Host:
          providers.add(createMetricsHostPropertyProvider(
            type,
            streamProvider,
            ComponentSSLConfiguration.instance(),
            this,
            this,
            PropertyHelper.getPropertyId("Hosts", "cluster_name"),
            PropertyHelper.getPropertyId("Hosts", "host_name")
          ));
          providers.add(new AlertSummaryPropertyProvider(type,
              "Hosts/cluster_name", "Hosts/host_name"));
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
          PropertyProvider gpp = null;
          gpp = createMetricsComponentPropertyProvider(
              type,
              streamProvider,
              ComponentSSLConfiguration.instance(),
              this,
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
          break;
        }
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
          PropertyProvider gpp = null;
          gpp = createMetricsHostComponentPropertyProvider(
            type,
            streamProvider,
            ComponentSSLConfiguration.instance(),
            this,
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

          providers.add(new HttpPropertyProvider(streamProvider,
            managementController.getClusters(),
            PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
            PropertyHelper.getPropertyId("HostRoles", "host_name"),
            PropertyHelper.getPropertyId("HostRoles", "component_name"),
            HTTP_PROPERTY_REQUESTS));

          // injecting the Injector type won't seem to work in this module, so
          // this follows the current pattern of relying on the management controller
          // to instantiate this PropertyProvider
          providers.add(managementController.getLoggingSearchPropertyProvider());
          break;
        }
        case RootServiceComponent:
          providers.add(new RootServiceComponentPropertyProvider());
          break;
        default:
          break;
      }
    }
    putPropertyProviders(type, providers);
  }

  private void checkInit() throws SystemException {
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
        if (initialized) {
          initialized = false;
        }
      }
    }
  }

  private void initProviderMaps() throws SystemException {
    ResourceProvider provider = getResourceProvider(Resource.Type.Cluster);

    Set<String> propertyIds = new HashSet<String>();
    propertyIds.add(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID);

    Map<String, String> requestInfoProperties = new HashMap<String, String>();
    requestInfoProperties.put(ClusterResourceProvider.GET_IGNORE_PERMISSIONS_PROPERTY_ID, "true");

    Request request = PropertyHelper.getReadRequest(propertyIds,
        requestInfoProperties, null, null, null);

    try {
      jmxPortMap.clear();
      Set<Resource> clusters = provider.getResources(request, null);

      clusterHostComponentMap = new HashMap<String, Map<String, String>>();
      clusterGangliaCollectorMap = new HashMap<String, String>();

      for (Resource cluster : clusters) {

        String clusterName = (String) cluster.getPropertyValue(CLUSTER_NAME_PROPERTY_ID);

        // initialize the host component map and Ganglia server from the known hosts components...
        provider = getResourceProvider(Resource.Type.HostComponent);

        request = PropertyHelper.getReadRequest(HOST_COMPONENT_HOST_NAME_PROPERTY_ID,
            HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID);

        Predicate predicate = new PredicateBuilder().property(HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID).
            equals(clusterName).toPredicate();

        Set<Resource> hostComponents = provider.getResources(request, predicate);
        Map<String, String> hostComponentMap = clusterHostComponentMap.get(clusterName);

        if (hostComponentMap == null) {
          hostComponentMap = new HashMap<String, String>();
          clusterHostComponentMap.put(clusterName, hostComponentMap);
        }

        for (Resource hostComponent : hostComponents) {
          String componentName = (String) hostComponent.getPropertyValue(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID);
          String hostName = (String) hostComponent.getPropertyValue(HOST_COMPONENT_HOST_NAME_PROPERTY_ID);

          hostComponentMap.put(componentName, hostName);

          // record the Ganglia server for the current cluster
          if (componentName.equals(GANGLIA_SERVER)) {
            clusterGangliaCollectorMap.put(clusterName, hostName);
          }
          if (componentName.equals(METRIC_SERVER)) {
            //  If current collector host is null or if the host or the host component not live
            //    Update clusterMetricCollectorMap.
            metricsCollectorHAManager.addCollectorHost(clusterName, hostName);
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
    return value != null && value.contains(":") ?
      value.substring(value.lastIndexOf(":") + 1, value.length()) : value;
  }

  /**
   * Gets the desired configuration version tag for the given cluster and config
   * type.
   *
   * @param clusterName
   *          the cluster name
   * @param configType
   *          the configuration type (for example {@value hdfs-site}).
   * @return
   */
  private String getDesiredConfigVersion(String clusterName,
      String configType) {

    String versionTag = ConfigHelper.FIRST_VERSION_TAG;

    try {
      Clusters clusters = managementController.getClusters();
      Cluster cluster = clusters.getCluster(clusterName);
      Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();

      DesiredConfig config = desiredConfigs.get(configType);
      if (config != null) {
        versionTag = config.getTag();
      }
    } catch (AmbariException ambariException) {
      LOG.error(
          "Unable to lookup the desired configuration tag for {} on cluster {}, defaulting to {}",
          configType, clusterName, versionTag, ambariException);
    }

    return versionTag;
  }

  private Map<String, Object> getConfigProperties(String clusterName, String versionTag, String configType)
      throws NoSuchParentResourceException,
      UnsupportedPropertyException, SystemException {
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
    for (Resource res : configResources) {
      return res.getPropertiesMap().get(PROPERTIES_CATEGORY);
    }
    return Collections.emptyMap();
  }

  private Map<String, String> getDesiredConfigMap(String clusterName, String versionTag,
                                                  String configType, Map<String, String[]> keys) throws NoSuchParentResourceException,
      UnsupportedPropertyException, SystemException {
    return getDesiredConfigMap(clusterName, versionTag, configType, keys, null);
  }

  // TODO get configs using ConfigHelper
  private Map<String, String> getDesiredConfigMap(String clusterName, String versionTag,
                                                  String configType, Map<String, String[]> keys,
                                                  Map<String, Object> configProperties)
    throws NoSuchParentResourceException, UnsupportedPropertyException, SystemException {
    if(configProperties == null) {
      configProperties = getConfigProperties(clusterName, versionTag, configType);
    }
    Map<String, String> mConfigs = new HashMap<String, String>();
    if (!configProperties.isEmpty()) {
      Map<String, String> evaluatedProperties = null;
      for (Entry<String, String[]> entry : keys.entrySet()) {
        String propName = null;
        String value = null;

        for (String pname : entry.getValue()) {
          propName = pname;
          // For NN HA the property key contains nameservice id
          for (Map.Entry<String, Object> propertyEntry : configProperties.entrySet()) {
            if (propertyEntry.getKey().startsWith(pname)) {
              value = (String) propertyEntry.getValue();
              break;
            }
          }

          if (null != value) {
            break;
          }
        }

        if (value != null && value.contains("${")) {
          if (evaluatedProperties == null) {
            evaluatedProperties = new HashMap<String, String>();
            for (Map.Entry<String, Object> subentry : configProperties.entrySet()) {
              String keyString = subentry.getKey();
              Object object = subentry.getValue();
              String valueString;
              if (object != null && object instanceof String) {
                valueString = (String) object;
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
    return mConfigs;
  }

  /**
   * Create the JMX property provider for the given type.
   */
  private PropertyProvider createJMXPropertyProvider(Resource.Type type,
                                                     StreamProvider streamProvider,
                                                     JMXHostProvider jmxHostProvider,
                                                     MetricHostProvider metricsHostProvider,
                                                     String clusterNamePropertyId,
                                                     String hostNamePropertyId,
                                                     String componentNamePropertyId,
                                                     String statePropertyId) {

    return metricPropertyProviderFactory.createJMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(type), streamProvider,
        jmxHostProvider, metricsHostProvider, clusterNamePropertyId, hostNamePropertyId,
        componentNamePropertyId, statePropertyId);
  }

  /**
   * Create the Ganglia report property provider for the given type.
   */
  private PropertyProvider createMetricsReportPropertyProvider(Resource.Type type, URLStreamProvider streamProvider,
                                                               ComponentSSLConfiguration configuration,
                                                               MetricHostProvider hostProvider,
                                                               MetricsServiceProvider serviceProvider,
                                                               String clusterNamePropertyId) {

    return MetricsReportPropertyProvider.createInstance(
      PropertyHelper.getMetricPropertyIds(type), streamProvider,
      configuration, metricCacheProvider, hostProvider, serviceProvider,
      clusterNamePropertyId);
  }

  /**
   * Create the Ganglia host property provider for the given type.
   */
  private PropertyProvider createMetricsHostPropertyProvider(Resource.Type type,
                                                             URLStreamProvider streamProvider,
                                                             ComponentSSLConfiguration configuration,
                                                             MetricHostProvider hostProvider,
                                                             MetricsServiceProvider serviceProvider,
                                                             String clusterNamePropertyId,
                                                             String hostNamePropertyId) {
    return MetricsPropertyProvider.createInstance(type,
      PropertyHelper.getMetricPropertyIds(type), streamProvider, configuration,
      metricCacheProvider, hostProvider, serviceProvider, clusterNamePropertyId,
      hostNamePropertyId, null);
  }

  /**
   * Create the Ganglia component property provider for the given type.
   */
  private PropertyProvider createMetricsComponentPropertyProvider(Resource.Type type,
                                                                  URLStreamProvider streamProvider,
                                                                  ComponentSSLConfiguration configuration,
                                                                  MetricHostProvider hostProvider,
                                                                  MetricsServiceProvider serviceProvider,
                                                                  String clusterNamePropertyId,
                                                                  String componentNamePropertyId) {
    return MetricsPropertyProvider.createInstance(type,
      PropertyHelper.getMetricPropertyIds(type), streamProvider, configuration,
      metricCacheProvider, hostProvider, serviceProvider,
      clusterNamePropertyId, null,
      componentNamePropertyId);
  }


  /**
   * Create the Ganglia host component property provider for the given type.
   */
  private PropertyProvider createMetricsHostComponentPropertyProvider(Resource.Type type,
                                                                      URLStreamProvider streamProvider,
                                                                      ComponentSSLConfiguration configuration,
                                                                      MetricHostProvider hostProvider,
                                                                      MetricsServiceProvider serviceProvider,
                                                                      String clusterNamePropertyId,
                                                                      String hostNamePropertyId,
                                                                      String componentNamePropertyId) {

    return MetricsPropertyProvider.createInstance(type,
      PropertyHelper.getMetricPropertyIds(type), streamProvider, configuration,
      metricCacheProvider, hostProvider, serviceProvider, clusterNamePropertyId,
      hostNamePropertyId, componentNamePropertyId);
  }

  @Override
  public String getJMXProtocol(String clusterName, String componentName) {
    String mapKey = String.format("%s-%s", clusterName, componentName);
    String jmxProtocolString = clusterJmxProtocolMap.get(mapKey);
    if (null != jmxProtocolString) {
      return jmxProtocolString;
    }

    try {
      if (componentName.equals("NAMENODE") || componentName.equals("DATANODE") || componentName.equals("RESOURCEMANAGER") || componentName.equals("NODEMANAGER") || componentName.equals("JOURNALNODE") || componentName.equals("HISTORYSERVER") || componentName.equals("HBASE_MASTER") || componentName.equals("HBASE_REGIONSERVER")) {
        Service.Type service = componentServiceMap.get(componentName);
        String config = serviceConfigTypes.get(service);
        String newSiteConfigVersion = getDesiredConfigVersion(clusterName, config);
        String cachedSiteConfigVersion = clusterHdfsSiteConfigVersionMap.get(clusterName);
        if (!newSiteConfigVersion.equals(cachedSiteConfigVersion)) {
          Map<String, String> protocolMap = getDesiredConfigMap(
              clusterName,
              newSiteConfigVersion, config,
              jmxDesiredProperties.get(componentName));
          jmxProtocolString = getJMXProtocolString(protocolMap.get(componentName));
        }
      } else {
        jmxProtocolString = "http";
      }
    } catch (Exception e) {
      LOG.info("Exception while detecting JMX protocol for clusterName = " + clusterName +
          ", componentName = " + componentName, e);
      LOG.info("Defaulting JMX to HTTP protocol for  for clusterName = " + clusterName +
          ", componentName = " + componentName +
          componentName);
      jmxProtocolString = "http";
    }
    if (jmxProtocolString == null) {
      LOG.debug("Detected JMX protocol is null for clusterName = " + clusterName +
          ", componentName = " + componentName);
      LOG.debug("Defaulting JMX to HTTP protocol for  for clusterName = " + clusterName +
          ", componentName = " + componentName);
      jmxProtocolString = "http";
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("JMXProtocol = " + jmxProtocolString + ", for clusterName=" + clusterName +
          ", componentName = " + componentName);
    }
    clusterJmxProtocolMap.put(mapKey, jmxProtocolString);
    return jmxProtocolString;
  }

  private String getJMXProtocolString(String value) {
    if (PROPERTY_HDFS_HTTP_POLICY_VALUE_HTTPS_ONLY.equals(value)) {
      return "https";
    } else {
      return "http";
    }
  }

  @Override
  public String getJMXRpcMetricTag(String clusterName, String componentName, String port){
    Map<String, Map<String, String>> componentToPortsMap = jmxDesiredRpcSuffixes.get(clusterName);
    if (componentToPortsMap != null && componentToPortsMap.containsKey(componentName)) {
      Map<String, String> portToTagMap = componentToPortsMap.get(componentName);
      if (portToTagMap != null) {
        return portToTagMap.get(port);
      }
    }
    return null;
  }

  private void initRpcSuffixes(String clusterName, String componentName,
                               String config, String configVersion,
                               String hostName)
                              throws Exception {
    if (jmxDesiredRpcSuffixProperties.containsKey(componentName)) {
      Map<String, Map<String, String>> componentToPortsMap;
      if (jmxDesiredRpcSuffixes.containsKey(clusterName)) {
        componentToPortsMap = jmxDesiredRpcSuffixes.get(clusterName);
      } else {
        componentToPortsMap = new HashMap<>();
        componentToPortsMap.put(componentName, new HashMap<String, String>());
        jmxDesiredRpcSuffixes.put(clusterName, componentToPortsMap);
      }

      Map<String, String> portToTagMap = componentToPortsMap.get(componentName);
      portToTagMap.clear();

      Map<String, String[]> keys = jmxDesiredRpcSuffixProperties.get(componentName);

      if ("NAMENODE".equals(componentName)) {
        Map<String, Object> configProperties = getConfigProperties(
          clusterName,
          configVersion,
          serviceConfigTypes.get(componentServiceMap.get(componentName))
        );
        if (configProperties.containsKey("dfs.internal.nameservices")) {
          componentName += "-HA";
          keys = jmxDesiredRpcSuffixProperties.get(componentName);
          Map<String, String[]> stringMap = jmxDesiredRpcSuffixProperties.get(componentName);
          for (String tag: stringMap.keySet()) {
            keys.put(tag, getNamenodeHaProperty(configProperties, stringMap.get(tag), hostName));
          }
        }
      }

      Map<String, String> props = getDesiredConfigMap(
        clusterName, configVersion, config,
        keys);
      for (Entry<String, String> prop: props.entrySet()) {
        if (prop.getValue() != null) {
          portToTagMap.put(getPortString(prop.getValue()).trim(), prop.getKey());
        }
      }
    }
  }

  /**
   * Handles {@link ClusterConfigChangedEvent} which means that some caches
   * should invalidate.
   *
   * @param event
   *          the change event.
   */
  @Subscribe
  public void onConfigurationChangedEvent(ClusterConfigChangedEvent event) {
    clusterJmxProtocolMap.clear();
  }

}
