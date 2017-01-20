/*
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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.HostRequest;
import org.apache.ambari.server.controller.HostResponse;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.ambari.server.topology.InvalidTopologyTemplateException;
import org.apache.ambari.server.topology.LogicalRequest;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;


/**
 * Resource provider for host resources.
 */
public class HostResourceProvider extends AbstractControllerResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  // Hosts
  public static final String HOST_CLUSTER_NAME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "cluster_name");
  public static final String HOST_NAME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "host_name");
  public static final String HOST_PUBLIC_NAME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "public_host_name");
  public static final String HOST_IP_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "ip");
  public static final String HOST_TOTAL_MEM_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "total_mem");
  public static final String HOST_CPU_COUNT_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "cpu_count");
  public static final String HOST_PHYSICAL_CPU_COUNT_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "ph_cpu_count");
  public static final String HOST_OS_ARCH_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "os_arch");
  public static final String HOST_OS_TYPE_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "os_type");
  public static final String HOST_OS_FAMILY_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "os_family");
  public static final String HOST_RACK_INFO_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "rack_info");
  public static final String HOST_LAST_HEARTBEAT_TIME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "last_heartbeat_time");
  public static final String HOST_LAST_REGISTRATION_TIME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "last_registration_time");
  public static final String HOST_DISK_INFO_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "disk_info");


  public static final String HOST_HOST_STATUS_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "host_status");
  public static final String HOST_MAINTENANCE_STATE_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "maintenance_state");

  public static final String HOST_HOST_HEALTH_REPORT_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "host_health_report");
  public static final String HOST_RECOVERY_REPORT_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "recovery_report");
  public static final String HOST_RECOVERY_SUMMARY_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "recovery_summary");
  public static final String HOST_STATE_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "host_state");
  public static final String HOST_LAST_AGENT_ENV_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "last_agent_env");
  public static final String HOST_DESIRED_CONFIGS_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "desired_configs");

  public static final String BLUEPRINT_PROPERTY_ID =
      PropertyHelper.getPropertyId(null, "blueprint");
  public static final String HOSTGROUP_PROPERTY_ID =
      PropertyHelper.getPropertyId(null, "host_group");
  public static final String HOST_NAME_NO_CATEGORY_PROPERTY_ID =
      PropertyHelper.getPropertyId(null, "host_name");
  public static final String HOST_COUNT_PROPERTY_ID =
      PropertyHelper.getPropertyId(null, "host_count");
  public static final String HOST_PREDICATE_PROPERTY_ID =
      PropertyHelper.getPropertyId(null, "host_predicate");

  //todo use the same json structure for cluster host addition (cluster template and upscale)
  public static final String HOST_RACK_INFO_NO_CATEGORY_PROPERTY_ID =
      PropertyHelper.getPropertyId(null, "rack_info");

  protected static final String FORCE_DELETE_COMPONENTS = "force_delete_components";


  private static Set<String> pkPropertyIds =
      new HashSet<String>(Arrays.asList(new String[]{
          HOST_NAME_PROPERTY_ID}));

  @Inject
  private MaintenanceStateHelper maintenanceStateHelper;

  @Inject
  private OsFamily osFamily;

  @Inject
  private static TopologyManager topologyManager;

  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds           the property ids
   * @param keyPropertyIds        the key property ids
   * @param managementController  the management controller
   */
  @AssistedInject
  HostResourceProvider(@Assisted Set<String> propertyIds,
                       @Assisted Map<Resource.Type, String> keyPropertyIds,
                       @Assisted AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);

    Set<RoleAuthorization> authorizationsAddDelete = EnumSet.of(RoleAuthorization.HOST_ADD_DELETE_HOSTS);

    setRequiredCreateAuthorizations(authorizationsAddDelete);
    setRequiredDeleteAuthorizations(authorizationsAddDelete);
    setRequiredGetAuthorizations(RoleAuthorization.AUTHORIZATIONS_VIEW_CLUSTER);
    setRequiredUpdateAuthorizations(RoleAuthorization.AUTHORIZATIONS_UPDATE_CLUSTER);
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  protected RequestStatus createResourcesAuthorized(final Request request)
      throws SystemException,
          UnsupportedPropertyException,
          ResourceAlreadyExistsException,
          NoSuchParentResourceException {

    RequestStatusResponse createResponse = null;
    if (isHostGroupRequest(request)) {
      createResponse = submitHostRequests(request);
    } else {
      createResources(new Command<Void>() {
        @Override
        public Void invoke() throws AmbariException {
          createHosts(request);
          return null;
        }
      });
    }
    notifyCreate(Resource.Type.Host, request);

    return getRequestStatus(createResponse);
  }

  @Override
  protected Set<Resource> getResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<HostRequest> requests = new HashSet<HostRequest>();

    if (predicate == null) {
      requests.add(getRequest(null));
    }
    else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<HostResponse> responses = getResources(new Command<Set<HostResponse>>() {
      @Override
      public Set<HostResponse> invoke() throws AmbariException {
        return getHosts(requests);
      }
    });

    Set<String>   requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources    = new HashSet<Resource>();

    for (HostResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.Host);

      // TODO : properly handle more than one cluster
      if (response.getClusterName() != null
          && !response.getClusterName().isEmpty()) {
        setResourceProperty(resource, HOST_CLUSTER_NAME_PROPERTY_ID,
            response.getClusterName(), requestedIds);
      }
      setResourceProperty(resource, HOST_NAME_PROPERTY_ID,
          response.getHostname(), requestedIds);
      setResourceProperty(resource, HOST_PUBLIC_NAME_PROPERTY_ID,
          response.getPublicHostName(), requestedIds);
      setResourceProperty(resource, HOST_IP_PROPERTY_ID,
          response.getIpv4(), requestedIds);
      setResourceProperty(resource, HOST_TOTAL_MEM_PROPERTY_ID,
          response.getTotalMemBytes(), requestedIds);
      setResourceProperty(resource, HOST_CPU_COUNT_PROPERTY_ID,
          (long) response.getCpuCount(), requestedIds);
      setResourceProperty(resource, HOST_PHYSICAL_CPU_COUNT_PROPERTY_ID,
          (long) response.getPhCpuCount(), requestedIds);
      setResourceProperty(resource, HOST_OS_ARCH_PROPERTY_ID,
          response.getOsArch(), requestedIds);
      setResourceProperty(resource, HOST_OS_TYPE_PROPERTY_ID,
          response.getOsType(), requestedIds);

      String hostOsFamily = osFamily.find(response.getOsType());
      if (hostOsFamily == null) {
        LOG.error("Can not find host OS family. For OS type = '{}' and host name = '{}'",
            response.getOsType(), response.getHostname());
      }
      setResourceProperty(resource, HOST_OS_FAMILY_PROPERTY_ID,
          hostOsFamily, requestedIds);

      setResourceProperty(resource, HOST_RACK_INFO_PROPERTY_ID,
          response.getRackInfo(), requestedIds);
      setResourceProperty(resource, HOST_LAST_HEARTBEAT_TIME_PROPERTY_ID,
          response.getLastHeartbeatTime(), requestedIds);
      setResourceProperty(resource, HOST_LAST_AGENT_ENV_PROPERTY_ID,
          response.getLastAgentEnv(), requestedIds);
      setResourceProperty(resource, HOST_LAST_REGISTRATION_TIME_PROPERTY_ID,
          response.getLastRegistrationTime(), requestedIds);
      setResourceProperty(resource, HOST_HOST_STATUS_PROPERTY_ID,
          response.getStatus(),requestedIds);
      setResourceProperty(resource, HOST_HOST_HEALTH_REPORT_PROPERTY_ID,
          response.getHealthStatus().getHealthReport(), requestedIds);
      setResourceProperty(resource, HOST_RECOVERY_REPORT_PROPERTY_ID,
          response.getRecoveryReport(), requestedIds);
      setResourceProperty(resource, HOST_RECOVERY_SUMMARY_PROPERTY_ID,
          response.getRecoverySummary(), requestedIds);
      setResourceProperty(resource, HOST_DISK_INFO_PROPERTY_ID,
          response.getDisksInfo(), requestedIds);
      setResourceProperty(resource, HOST_STATE_PROPERTY_ID,
          response.getHostState(), requestedIds);
      setResourceProperty(resource, HOST_DESIRED_CONFIGS_PROPERTY_ID,
          response.getDesiredHostConfigs(), requestedIds);

      // only when a cluster request
      if (null != response.getMaintenanceState()) {
        setResourceProperty(resource, HOST_MAINTENANCE_STATE_PROPERTY_ID,
            response.getMaintenanceState(), requestedIds);
      }

      resources.add(resource);
    }
    return resources;
  }

  @Override
  protected RequestStatus updateResourcesAuthorized(final Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<HostRequest> requests = new HashSet<HostRequest>();
    for (Map<String, Object> propertyMap : getPropertyMaps(request.getProperties().iterator().next(), predicate)) {
      requests.add(getRequest(propertyMap));
    }

    modifyResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException, AuthorizationException {
        updateHosts(requests);
        return null;
      }
    });

    notifyUpdate(Resource.Type.Host, request, predicate);

    return getRequestStatus(null);
  }

  @Override
  protected RequestStatus deleteResourcesAuthorized(final Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<HostRequest> requests = new HashSet<>();
    Map<String, String> requestInfoProperties = request.getRequestInfoProperties();
    final boolean forceDelete = requestInfoProperties.containsKey(FORCE_DELETE_COMPONENTS) &&
                  requestInfoProperties.get(FORCE_DELETE_COMPONENTS).equals("true");

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }

    DeleteStatusMetaData deleteStatusMetaData = modifyResources(new Command<DeleteStatusMetaData>() {
      @Override
      public DeleteStatusMetaData invoke() throws AmbariException {
        return deleteHosts(requests, request.isDryRunRequest(), forceDelete);
      }
    });

    if (!request.isDryRunRequest()) {
      notifyDelete(Resource.Type.Host, predicate);
    }

    return getRequestStatus(null, null, deleteStatusMetaData);
  }

  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    Set<String> baseUnsupported = super.checkPropertyIds(propertyIds);

    baseUnsupported.remove(BLUEPRINT_PROPERTY_ID);
    baseUnsupported.remove(HOSTGROUP_PROPERTY_ID);
    baseUnsupported.remove(HOST_NAME_NO_CATEGORY_PROPERTY_ID);
    //todo: constants
    baseUnsupported.remove(HOST_COUNT_PROPERTY_ID);
    baseUnsupported.remove(HOST_PREDICATE_PROPERTY_ID);
    baseUnsupported.remove(HOST_RACK_INFO_NO_CATEGORY_PROPERTY_ID);

    return checkConfigPropertyIds(baseUnsupported, "Hosts");
  }


  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }


  // ----- utility methods ---------------------------------------------------

  /**
   * Determine if a request is a high level "add hosts" call or a simple lower level request
   * to add a host resources.
   *
   * @param request  current request
   * @return true if this is a high level "add hosts" request;
   *         false if it is a simple create host resources request
   */
  private boolean isHostGroupRequest(Request request) {
    boolean isHostGroupRequest = false;
    Set<Map<String, Object>> properties = request.getProperties();
    if (properties != null && ! properties.isEmpty()) {
      //todo: for now, either all or none of the hosts need to specify a hg.  Unable to mix.
      String hgName = (String) properties.iterator().next().get(HOSTGROUP_PROPERTY_ID);
      isHostGroupRequest = hgName != null && ! hgName.isEmpty();
    }
    return isHostGroupRequest;
  }

  /**
   * Get a host request object from a map of property values.
   *
   * @param properties  the predicate
   *
   * @return the component request object
   */
  private HostRequest getRequest(Map<String, Object> properties) {

    if (properties == null) {
      return  new HostRequest(null, null, null);
    }

    HostRequest hostRequest = new HostRequest(
        getHostNameFromProperties(properties),
        (String) properties.get(HOST_CLUSTER_NAME_PROPERTY_ID),
        null);
    hostRequest.setPublicHostName((String) properties.get(HOST_PUBLIC_NAME_PROPERTY_ID));

    String rackInfo = (String) ((null != properties.get(HOST_RACK_INFO_PROPERTY_ID))? properties.get(HOST_RACK_INFO_PROPERTY_ID):
            properties.get(HOST_RACK_INFO_NO_CATEGORY_PROPERTY_ID));

    hostRequest.setRackInfo(rackInfo);
    hostRequest.setBlueprintName((String) properties.get(BLUEPRINT_PROPERTY_ID));
    hostRequest.setHostGroupName((String) properties.get(HOSTGROUP_PROPERTY_ID));

    Object o = properties.get(HOST_MAINTENANCE_STATE_PROPERTY_ID);
    if (null != o) {
      hostRequest.setMaintenanceState(o.toString());
    }

    List<ConfigurationRequest> cr = getConfigurationRequests("Hosts", properties);

    hostRequest.setDesiredConfigs(cr);

    return hostRequest;
  }


  /**
   * Accepts a request with registered hosts and if the request contains a cluster name then will map all of the
   * hosts onto that cluster.
   * @param request Request that must contain registered hosts, and optionally a cluster.
   * @throws AmbariException
   */
  public synchronized void createHosts(Request request)
      throws AmbariException {

    Set<Map<String, Object>> propertySet = request.getProperties();
    if (propertySet == null || propertySet.isEmpty()) {
      LOG.warn("Received a create host request with no associated property sets");
      return;
    }

    AmbariManagementController controller = getManagementController();
    Clusters                   clusters   = controller.getClusters();

    Set<String> duplicates = new HashSet<String>();
    Set<String> unknowns = new HashSet<String>();
    Set<String> allHosts = new HashSet<String>();


    Set<HostRequest> hostRequests = new HashSet<HostRequest>();
    for (Map<String, Object> propertyMap : propertySet) {
      HostRequest hostRequest = getRequest(propertyMap);
      hostRequests.add(hostRequest);
      if (! propertyMap.containsKey(HOSTGROUP_PROPERTY_ID)) {
        createHostResource(clusters, duplicates, unknowns, allHosts, hostRequest);
      }
    }

    if (!duplicates.isEmpty()) {
      StringBuilder names = new StringBuilder();
      boolean first = true;
      for (String hName : duplicates) {
        if (!first) {
          names.append(",");
        }
        first = false;
        names.append(hName);
      }
      throw new IllegalArgumentException("Invalid request contains"
          + " duplicate hostnames"
          + ", hostnames=" + names.toString());
    }

    if (!unknowns.isEmpty()) {
      StringBuilder names = new StringBuilder();
      boolean first = true;
      for (String hName : unknowns) {
        if (!first) {
          names.append(",");
        }
        first = false;
        names.append(hName);
      }

      throw new IllegalArgumentException("Attempted to add unknown hosts to a cluster.  " +
          "These hosts have not been registered with the server: " + names.toString());
    }

    Map<String, Set<String>> hostClustersMap = new HashMap<String, Set<String>>();
    Map<String, Map<String, String>> hostAttributes = new HashMap<String, Map<String, String>>();
    Set<String> allClusterSet = new HashSet<String>();

    for (HostRequest hostRequest : hostRequests) {
      if (hostRequest.getHostname() != null &&
          !hostRequest.getHostname().isEmpty() &&
          hostRequest.getClusterName() != null &&
          !hostRequest.getClusterName().isEmpty()){

        Set<String> clusterSet = new HashSet<String>();
        clusterSet.add(hostRequest.getClusterName());
        allClusterSet.add(hostRequest.getClusterName());
        hostClustersMap.put(hostRequest.getHostname(), clusterSet);
        if (hostRequest.getHostAttributes() != null) {
          hostAttributes.put(hostRequest.getHostname(), hostRequest.getHostAttributes());
        }
      }
    }
    clusters.updateHostWithClusterAndAttributes(hostClustersMap, hostAttributes);

    for (String clusterName : allClusterSet) {
      clusters.getCluster(clusterName).recalculateAllClusterVersionStates();
    }
  }

  private void createHostResource(Clusters clusters, Set<String> duplicates,
                                  Set<String> unknowns, Set<String> allHosts,
                                  HostRequest request)
      throws AmbariException {


    if (request.getHostname() == null
        || request.getHostname().isEmpty()) {
      throw new IllegalArgumentException("Invalid arguments, hostname"
          + " cannot be null");
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Received a createHost request"
          + ", hostname=" + request.getHostname()
          + ", request=" + request);
    }

    if (allHosts.contains(request.getHostname())) {
      // throw dup error later
      duplicates.add(request.getHostname());
      return;
    }
    allHosts.add(request.getHostname());

    try {
      // ensure host is registered
      clusters.getHost(request.getHostname());
    }
    catch (HostNotFoundException e) {
      unknowns.add(request.getHostname());
      return;
    }

    if (request.getClusterName() != null) {
      try {
        // validate that cluster_name is valid
        clusters.getCluster(request.getClusterName());
      } catch (ClusterNotFoundException e) {
        throw new ParentObjectNotFoundException("Attempted to add a host to a cluster which doesn't exist: "
            + " clusterName=" + request.getClusterName());
      }
    }
  }

  public RequestStatusResponse install(final String cluster, final String hostname, Collection<String> skipInstallForComponents, Collection<String> dontSkipInstallForComponents, final boolean skipFailure)
      throws ResourceAlreadyExistsException,
      SystemException,
      NoSuchParentResourceException,
      UnsupportedPropertyException {


    return ((HostComponentResourceProvider) getResourceProvider(Resource.Type.HostComponent)).
        install(cluster, hostname, skipInstallForComponents, dontSkipInstallForComponents, skipFailure);
  }

  public RequestStatusResponse start(final String cluster, final String hostname)
      throws ResourceAlreadyExistsException,
      SystemException,
      NoSuchParentResourceException,
      UnsupportedPropertyException {

    return ((HostComponentResourceProvider) getResourceProvider(Resource.Type.HostComponent)).
        start(cluster, hostname);
  }

  protected Set<HostResponse> getHosts(Set<HostRequest> requests) throws AmbariException {
    Set<HostResponse> response = new HashSet<HostResponse>();

    AmbariManagementController controller = getManagementController();

    for (HostRequest request : requests) {
      try {
        response.addAll(getHosts(controller, request));
      } catch (HostNotFoundException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          throw e;
        }
      }
    }
    return response;
  }

  protected static Set<HostResponse> getHosts(AmbariManagementController controller, HostRequest request)
      throws AmbariException {

    //TODO/FIXME host can only belong to a single cluster so get host directly from Cluster
    //TODO/FIXME what is the requirement for filtering on host attributes?

    List<Host> hosts;
    Set<HostResponse> response = new HashSet<HostResponse>();
    Cluster           cluster  = null;

    Clusters                   clusters   = controller.getClusters();

    String clusterName = request.getClusterName();
    String hostName    = request.getHostname();

    if (clusterName != null) {
      //validate that cluster exists, throws exception if it doesn't.
      try {
        cluster = clusters.getCluster(clusterName);
      } catch (ObjectNotFoundException e) {
        throw new ParentObjectNotFoundException("Parent Cluster resource doesn't exist", e);
      }
    }

    if (hostName == null) {
      hosts = clusters.getHosts();
    } else {
      hosts = new ArrayList<Host>();
      try {
        hosts.add(clusters.getHost(request.getHostname()));
      } catch (HostNotFoundException e) {
        // add cluster name
        throw new HostNotFoundException(clusterName, hostName);
      }
    }

    // retrieve the cluster desired configs once instead of per host
    Map<String, DesiredConfig> desiredConfigs = null;
    if (null != cluster) {
      cluster.getDesiredConfigs();
    }

    for (Host h : hosts) {
      if (clusterName != null) {
        if (clusters.getClustersForHost(h.getHostName()).contains(cluster)) {
          HostResponse r = h.convertToResponse();

          r.setClusterName(clusterName);
          r.setDesiredHostConfigs(h.getDesiredHostConfigs(cluster, desiredConfigs));
          r.setMaintenanceState(h.getMaintenanceState(cluster.getClusterId()));

          response.add(r);
        } else if (hostName != null) {
          throw new HostNotFoundException(clusterName, hostName);
        }
      } else {
        HostResponse r = h.convertToResponse();

        Set<Cluster> clustersForHost = clusters.getClustersForHost(h.getHostName());
        //todo: host can only belong to a single cluster
        if (clustersForHost != null && clustersForHost.size() != 0) {
          Cluster clusterForHost = clustersForHost.iterator().next();
          r.setClusterName(clusterForHost.getClusterName());
          r.setDesiredHostConfigs(h.getDesiredHostConfigs(clusterForHost, desiredConfigs));
          r.setMaintenanceState(h.getMaintenanceState(clusterForHost.getClusterId()));
        }

        response.add(r);
      }
    }
    return response;
  }

  protected synchronized void updateHosts(Set<HostRequest> requests) throws AmbariException, AuthorizationException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return;
    }

    AmbariManagementController controller = getManagementController();
    Clusters                   clusters   = controller.getClusters();

    for (HostRequest request : requests) {
      if (request.getHostname() == null || request.getHostname().isEmpty()) {
        throw new IllegalArgumentException("Invalid arguments, hostname should be provided");
      }
    }

    for (HostRequest request : requests) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Received an updateHost request"
            + ", hostname=" + request.getHostname()
            + ", request=" + request);
      }

      Host host = clusters.getHost(request.getHostname());

      String clusterName = request.getClusterName();
      Cluster cluster = clusters.getCluster(clusterName);
      Long clusterId = cluster.getClusterId();
      Long resourceId = cluster.getResourceId();

      try {
        // The below method call throws an exception when trying to create a duplicate mapping in the clusterhostmapping
        // table. This is done to detect duplicates during host create. In order to be robust, handle these gracefully.
        clusters.mapAndPublishHostsToCluster(new HashSet<>(Arrays.asList(request.getHostname())), clusterName);
      } catch (DuplicateResourceException e) {
        // do nothing
      }

      if (null != request.getHostAttributes()) {
        if(!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, resourceId, RoleAuthorization.HOST_ADD_DELETE_HOSTS)) {
          throw new AuthorizationException("The authenticated user is not authorized to update host attributes");
        }
        host.setHostAttributes(request.getHostAttributes());
      }

      String  rackInfo        = host.getRackInfo();
      String  requestRackInfo = request.getRackInfo();
      boolean rackChange      = requestRackInfo != null && !requestRackInfo.equals(rackInfo);

      if (rackChange) {
        if(!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, resourceId, RoleAuthorization.HOST_ADD_DELETE_HOSTS)) {
          throw new AuthorizationException("The authenticated user is not authorized to update host rack information");
        }
        host.setRackInfo(requestRackInfo);
      }

      if (null != request.getPublicHostName()) {
        if(!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, resourceId, RoleAuthorization.HOST_ADD_DELETE_HOSTS)) {
          throw new AuthorizationException("The authenticated user is not authorized to update host attributes");
        }
        host.setPublicHostName(request.getPublicHostName());
      }

      if (null != clusterName && null != request.getMaintenanceState()) {
        if(!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, resourceId, RoleAuthorization.HOST_TOGGLE_MAINTENANCE)) {
          throw new AuthorizationException("The authenticated user is not authorized to update host maintenance state");
        }
        MaintenanceState newState = MaintenanceState.valueOf(request.getMaintenanceState());
        MaintenanceState oldState = host.getMaintenanceState(clusterId);
        if (!newState.equals(oldState)) {
          if (newState.equals(MaintenanceState.IMPLIED_FROM_HOST)
              || newState.equals(MaintenanceState.IMPLIED_FROM_SERVICE)) {
            throw new IllegalArgumentException("Invalid arguments, can only set " +
              "maintenance state to one of " + EnumSet.of(MaintenanceState.OFF, MaintenanceState.ON));
          } else {
            host.setMaintenanceState(clusterId, newState);
          }
        }
      }

      // Create configurations
      if (null != clusterName && null != request.getDesiredConfigs()) {
        if (clusters.getHostsForCluster(clusterName).containsKey(host.getHostName())) {

          for (ConfigurationRequest cr : request.getDesiredConfigs()) {
            if (null != cr.getProperties() && cr.getProperties().size() > 0) {
              LOG.info(MessageFormat.format("Applying configuration with tag ''{0}'' to host ''{1}'' in cluster ''{2}''",
                  cr.getVersionTag(),
                  request.getHostname(),
                  clusterName));

              cr.setClusterName(cluster.getClusterName());
              controller.createConfiguration(cr);
            }

            Config baseConfig = cluster.getConfig(cr.getType(), cr.getVersionTag());
            if (null != baseConfig) {
              String authName = controller.getAuthName();
              DesiredConfig oldConfig = host.getDesiredConfigs(clusterId).get(cr.getType());

              if (host.addDesiredConfig(clusterId, cr.isSelected(), authName,  baseConfig)) {
                Logger logger = LoggerFactory.getLogger("configchange");
                logger.info("cluster '" + cluster.getClusterName() + "', "
                    + "host '" + host.getHostName() + "' "
                    + "changed by: '" + authName + "'; "
                    + "type='" + baseConfig.getType() + "' "
                    + "version='" + baseConfig.getVersion() + "'"
                    + "tag='" + baseConfig.getTag() + "'"
                    + (null == oldConfig ? "" : ", from='" + oldConfig.getTag() + "'"));
              }
            }
          }
        }
      }

      if (clusterName != null && !clusterName.isEmpty()) {
        clusters.getCluster(clusterName).recalculateAllClusterVersionStates();
        if (rackChange) {
          // Authorization check for this update was performed before we got to this point.
          controller.registerRackChange(clusterName);
        }
      }

      //todo: if attempt was made to update a property other than those
      //todo: that are allowed above, should throw exception
    }
  }

  @Transactional
  protected DeleteStatusMetaData deleteHosts(Set<HostRequest> requests, boolean dryRun, boolean forceDelete)
      throws AmbariException {

    AmbariManagementController controller = getManagementController();
    Clusters                   clusters   = controller.getClusters();
    DeleteStatusMetaData deleteStatusMetaData = new DeleteStatusMetaData();
    List<HostRequest> okToRemove = new ArrayList<>();

    for (HostRequest hostRequest : requests) {
      String hostName = hostRequest.getHostname();
      if (null == hostName) {
        continue;
      }

      try {
        validateHostInDeleteFriendlyState(hostRequest, clusters, forceDelete);
        okToRemove.add(hostRequest);
      } catch (Exception ex) {
        deleteStatusMetaData.addException(hostName, ex);
      }
    }

    //If dry run, don't delete. just assume it can be successfully deleted.
    if (dryRun) {
      for (HostRequest request : okToRemove) {
        deleteStatusMetaData.addDeletedKey(request.getHostname());
      }
    } else {
      processDeleteHostRequests(okToRemove, clusters, deleteStatusMetaData);
    }

    //Do not break behavior for existing clients where delete request contains only 1 host.
    //Response for these requests will have empty body with appropriate error code.
    //dryRun is a new feature so its ok to unify the behavior
    if (!dryRun) {
      if (deleteStatusMetaData.getDeletedKeys().size() + deleteStatusMetaData.getExceptionForKeys().size() == 1) {
        if (deleteStatusMetaData.getDeletedKeys().size() == 1) {
          return null;
        }
        for (Map.Entry<String, Exception> entry : deleteStatusMetaData.getExceptionForKeys().entrySet()) {
          Exception ex = entry.getValue();
          if (ex instanceof AmbariException) {
            throw (AmbariException) ex;
          } else {
            throw new AmbariException(ex.getMessage(), ex);
          }
        }
      }
    }

    return deleteStatusMetaData;
  }

  private void processDeleteHostRequests(List<HostRequest> requests,  Clusters clusters, DeleteStatusMetaData deleteStatusMetaData) throws AmbariException {
    Set<String> hostsClusters = new HashSet<>();
    Set<String> hostNames = new HashSet<>();
    Set<Cluster> allClustersWithHosts = new HashSet<>();
    for (HostRequest hostRequest : requests) {
      // Assume the user also wants to delete it entirely, including all clusters.
      String hostname = hostRequest.getHostname();
      hostNames.add(hostname);

      if (hostRequest.getClusterName() != null) {
        hostsClusters.add(hostRequest.getClusterName());
      }

      LOG.info("Received Delete request for host {} from cluster {}.", hostname, hostRequest.getClusterName());

      // delete all host components
      Set<ServiceComponentHostRequest> schrs = new HashSet<>();
      for (Cluster cluster : clusters.getClustersForHost(hostname)) {
        List<ServiceComponentHost> list = cluster.getServiceComponentHosts(hostname);
        for (ServiceComponentHost sch : list) {
          ServiceComponentHostRequest schr = new ServiceComponentHostRequest(cluster.getClusterName(),
                                                                             sch.getServiceName(),
                                                                             sch.getServiceComponentName(),
                                                                             sch.getHostName(),
                                                                             null);
          schrs.add(schr);
        }
      }
      DeleteStatusMetaData componentDeleteStatus = null;
      if (schrs.size() > 0) {
        try {
          componentDeleteStatus = getManagementController().deleteHostComponents(schrs);
        } catch (Exception ex) {
          deleteStatusMetaData.addException(hostname, ex);
        }
      }

      if (componentDeleteStatus != null) {
        for (String key : componentDeleteStatus.getDeletedKeys()) {
          deleteStatusMetaData.addDeletedKey(key);
        }
        for (String key : componentDeleteStatus.getExceptionForKeys().keySet()) {
          deleteStatusMetaData.addException(key, componentDeleteStatus.getExceptionForKeys().get(key));
        }
      }

      if (hostRequest.getClusterName() != null) {
        hostsClusters.add(hostRequest.getClusterName());
      }
      try {
        clusters.deleteHost(hostname);
        deleteStatusMetaData.addDeletedKey(hostname);
      } catch (Exception ex) {
        deleteStatusMetaData.addException(hostname, ex);
      }
      removeHostFromClusterTopology(clusters, hostRequest);
      for (LogicalRequest logicalRequest: topologyManager.getRequests(Collections.<Long>emptyList())) {
        logicalRequest.removeHostRequestByHostName(hostname);
      }
    }
    clusters.publishHostsDeletion(allClustersWithHosts, hostNames);
    for (String clustername : hostsClusters) {
      clusters.getCluster(clustername).recalculateAllClusterVersionStates();
    }
  }

  private void validateHostInDeleteFriendlyState(HostRequest hostRequest, Clusters clusters, boolean forceDelete) throws AmbariException {
    Set<String> clusterNamesForHost = new HashSet<>();
    String hostName = hostRequest.getHostname();
    if (null != hostRequest.getClusterName()) {
      clusterNamesForHost.add(hostRequest.getClusterName());
    } else {
      Set<Cluster> clustersForHost = clusters.getClustersForHost(hostRequest.getHostname());
      if (null != clustersForHost) {
        for (Cluster c : clustersForHost) {
          clusterNamesForHost.add(c.getClusterName());
        }
      }
    }

    for (String clusterName : clusterNamesForHost) {
      Cluster cluster = clusters.getCluster(clusterName);

      List<ServiceComponentHost> list = cluster.getServiceComponentHosts(hostName);

      if (!list.isEmpty()) {
        List<String> componentsToRemove = new ArrayList<>();
        List<String> componentsStarted = new ArrayList<>();
        for (ServiceComponentHost sch : list) {
          componentsToRemove.add(sch.getServiceComponentName());
          if (sch.getState() == State.STARTED) {
            componentsStarted.add(sch.getServiceComponentName());
          }
        }

        if (forceDelete) {
          // error if components are running
          if (!componentsStarted.isEmpty()) {
            StringBuilder reason = new StringBuilder("Cannot remove host ")
                .append(hostName)
                .append(" from ")
                .append(hostRequest.getClusterName())
                .append(
                    ".  The following roles exist, and these components must be stopped: ");

            reason.append(StringUtils.join(componentsToRemove, ", "));

            throw new AmbariException(reason.toString());
          }
        } else {
          if (!componentsToRemove.isEmpty()) {
            StringBuilder reason = new StringBuilder("Cannot remove host ")
                .append(hostName)
                .append(" from ")
                .append(hostRequest.getClusterName())
                .append(
                    ".  The following roles exist, and these components must be stopped if running, and then deleted: ");

            reason.append(StringUtils.join(componentsToRemove, ", "));

            throw new AmbariException(reason.toString());
          }
        }
      }
    }
  }

  /**
   * Removes hostname from the stateful cluster topology
   * @param clusters
   * @param hostRequest
   * @throws AmbariException
   */
  private void removeHostFromClusterTopology(Clusters clusters, HostRequest hostRequest) throws AmbariException{
    if (hostRequest.getClusterName() == null) {
      for (Cluster c : clusters.getClusters().values()) {
        removeHostFromClusterTopology(c.getClusterId(), hostRequest.getHostname());
      }
    } else {
      long clusterId = clusters.getCluster(hostRequest.getClusterName()).getClusterId();
      removeHostFromClusterTopology(clusterId, hostRequest.getHostname());
    }
  }

  private void removeHostFromClusterTopology(long clusterId, String hostname) {
    ClusterTopology clusterTopology = topologyManager.getClusterTopology(clusterId);
    if(clusterTopology != null) {
      clusterTopology.removeHost(hostname);
    }
  }

  /**
   * Obtain the hostname from the request properties.  The hostname property name may differ
   * depending on the request type.  For the low level host resource creation calls, it is always
   * "Hosts/host_name".  For multi host "add host from hostgroup", the hostname property is a top level
   * property "host_name".
   *
   * @param properties  request properties
   *
   * @return the host name for the host request
   */
  private String getHostNameFromProperties(Map<String, Object> properties) {
    String hostname = (String) properties.get(HOST_NAME_PROPERTY_ID);

    return hostname != null ? hostname :
        (String) properties.get(HOST_NAME_NO_CATEGORY_PROPERTY_ID);
  }

  //todo: for api/v1/hosts we also end up here so we need to ensure proper 400 response
  //todo: since a user shouldn't be posing to that endpoint
  private RequestStatusResponse submitHostRequests(Request request) throws SystemException {
    ScaleClusterRequest requestRequest;
    try {
      requestRequest = new ScaleClusterRequest(request.getProperties());
    } catch (InvalidTopologyTemplateException e) {
      throw new IllegalArgumentException("Invalid Add Hosts Template: " + e, e);
    }

    try {
      return topologyManager.scaleHosts(requestRequest);
    } catch (InvalidTopologyException e) {
      throw new IllegalArgumentException("Topology validation failed: " + e, e);
    } catch (AmbariException e) {
      //todo: handle non-system exceptions
      e.printStackTrace();
      //todo: for now just throw SystemException
      throw new SystemException("Unable to add hosts", e);
    }
  }

  //todo: proper static injection of topology manager
  public static void setTopologyManager(TopologyManager topologyManager) {
    HostResourceProvider.topologyManager = topologyManager;
  }
}
