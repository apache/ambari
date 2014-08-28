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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;
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
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;


/**
 * Resource provider for host resources.
 */
public class HostResourceProvider extends AbstractControllerResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  // Hosts
  protected static final String HOST_CLUSTER_NAME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "cluster_name");
  protected static final String HOST_NAME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "host_name");
  protected static final String HOST_PUBLIC_NAME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "public_host_name");
  protected static final String HOST_IP_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "ip");
  protected static final String HOST_TOTAL_MEM_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "total_mem");
  protected static final String HOST_CPU_COUNT_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "cpu_count");
  protected static final String HOST_PHYSICAL_CPU_COUNT_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "ph_cpu_count");  
  protected static final String HOST_OS_ARCH_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "os_arch");
  protected static final String HOST_OS_TYPE_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "os_type");
  protected static final String HOST_RACK_INFO_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "rack_info");
  protected static final String HOST_LAST_HEARTBEAT_TIME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "last_heartbeat_time");
  protected static final String HOST_LAST_REGISTRATION_TIME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "last_registration_time");
  protected static final String HOST_DISK_INFO_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "disk_info");
  
  
  protected static final String HOST_HOST_STATUS_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "host_status");
  protected static final String HOST_MAINTENANCE_STATE_PROPERTY_ID = 
      PropertyHelper.getPropertyId("Hosts", "maintenance_state");
  
  protected static final String HOST_HOST_HEALTH_REPORT_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "host_health_report");
  protected static final String HOST_STATE_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "host_state");
  protected static final String HOST_LAST_AGENT_ENV_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "last_agent_env");
  protected static final String HOST_DESIRED_CONFIGS_PROPERTY_ID = 
      PropertyHelper.getPropertyId("Hosts", "desired_configs");

  private static Set<String> pkPropertyIds =
      new HashSet<String>(Arrays.asList(new String[]{
          HOST_NAME_PROPERTY_ID}));

  @Inject
  private MaintenanceStateHelper maintenanceStateHelper;

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
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException,
          UnsupportedPropertyException,
          ResourceAlreadyExistsException,
          NoSuchParentResourceException {

    final Set<HostRequest> requests = new HashSet<HostRequest>();
    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }
    createResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        createHosts(requests);
        return null;
      }
    });

    notifyCreate(Resource.Type.Host, request);

    return getRequestStatus(null);
  }

  @Override
  @Transactional
  public Set<Resource> getResources(Request request, Predicate predicate)
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
  public RequestStatus updateResources(final Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<HostRequest> requests = new HashSet<HostRequest>();
    for (Map<String, Object> propertyMap : getPropertyMaps(request.getProperties().iterator().next(), predicate)) {
      requests.add(getRequest(propertyMap));
    }

    modifyResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        updateHosts(requests, request.getRequestInfoProperties());
        return null;
      }
    });

    notifyUpdate(Resource.Type.Host, request, predicate);

    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<HostRequest> requests = new HashSet<HostRequest>();
    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }

    modifyResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        deleteHosts(requests);
        return null;
      }
    });

    notifyDelete(Resource.Type.Host, predicate);

    return getRequestStatus(null);
  }

  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    Set<String> baseUnsupported = super.checkPropertyIds(propertyIds);

    return checkConfigPropertyIds(baseUnsupported, "Hosts");
  }


  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }


  // ----- utility methods ---------------------------------------------------

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
        (String) properties.get(HOST_NAME_PROPERTY_ID),
        (String) properties.get(HOST_CLUSTER_NAME_PROPERTY_ID),
        null);
    hostRequest.setPublicHostName((String) properties.get(HOST_PUBLIC_NAME_PROPERTY_ID));
    hostRequest.setRackInfo((String) properties.get(HOST_RACK_INFO_PROPERTY_ID));
    
    Object o = properties.get(HOST_MAINTENANCE_STATE_PROPERTY_ID);
    if (null != o)
      hostRequest.setMaintenanceState(o.toString());
    
    List<ConfigurationRequest> cr = getConfigurationRequests("Hosts", properties);
    
    hostRequest.setDesiredConfigs(cr);

    return hostRequest;
  }


  protected synchronized void createHosts(Set<HostRequest> requests)
      throws AmbariException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return;
    }

    AmbariManagementController controller = getManagementController();
    Clusters                   clusters   = controller.getClusters();

    Set<String> duplicates = new HashSet<String>();
    Set<String> unknowns = new HashSet<String>();
    Set<String> allHosts = new HashSet<String>();
    for (HostRequest request : requests) {
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
        continue;
      }
      allHosts.add(request.getHostname());

      try {
        // ensure host is registered
        clusters.getHost(request.getHostname());
      }
      catch (HostNotFoundException e) {
        unknowns.add(request.getHostname());
        continue;
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
    for (HostRequest request : requests) {
      if (request.getHostname() != null) {
        Set<String> clusterSet = new HashSet<String>();
        clusterSet.add(request.getClusterName());
        hostClustersMap.put(request.getHostname(), clusterSet);
        if (request.getHostAttributes() != null) {
          hostAttributes.put(request.getHostname(), request.getHostAttributes());
        }
      }
    }
    clusters.updateHostWithClusterAndAttributes(hostClustersMap, hostAttributes);
  }


  protected Set<HostResponse> getHosts(Set<HostRequest> requests)
      throws AmbariException {
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


    for (Host h : hosts) {
      if (clusterName != null) {
        if (clusters.getClustersForHost(h.getHostName()).contains(cluster)) {
          HostResponse r = h.convertToResponse();
          
          r.setClusterName(clusterName);
          r.setDesiredHostConfigs(h.getDesiredHostConfigs(cluster));
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
          r.setClusterName(clustersForHost.iterator().next().getClusterName());
        }
        response.add(r);
      }
    }
    return response;
  }

  protected synchronized void updateHosts(Set<HostRequest> requests,
      Map<String, String> requestProperties)
      throws AmbariException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return;
    }

    AmbariManagementController controller = getManagementController();
    Clusters                   clusters   = controller.getClusters();

    // We don't expect batch requests for different clusters, that's why
    // nothing bad should happen if value is overwritten few times
    String maintenanceCluster = null;
    
    for (HostRequest request : requests) {
      if (request.getHostname() == null
          || request.getHostname().isEmpty()) {
        throw new IllegalArgumentException("Invalid arguments, hostname should"
            + " be provided");
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a updateHost request"
            + ", hostname=" + request.getHostname()
            + ", request=" + request);
      }

      Host h = clusters.getHost(request.getHostname());

      try {
        //todo: the below method throws an exception when trying to create a duplicate mapping.
        //todo: this is done to detect duplicates during host create.  Unless it is allowable to
        //todo: add a host to a cluster by modifying the cluster_name prop, we should not do this mapping here.
        //todo: Determine if it is allowable to associate a host to a cluster via this mechanism.
        clusters.mapHostToCluster(request.getHostname(), request.getClusterName());
      } catch (DuplicateResourceException e) {
        // do nothing
      }

      if (null != request.getHostAttributes())
        h.setHostAttributes(request.getHostAttributes());

      if (null != request.getRackInfo()) {
        h.setRackInfo(request.getRackInfo());
      }

      if (null != request.getPublicHostName()) {
        h.setPublicHostName(request.getPublicHostName());
      }
      
      if (null != request.getClusterName() && null != request.getMaintenanceState()) {
        Cluster c = clusters.getCluster(request.getClusterName());
        MaintenanceState newState = MaintenanceState.valueOf(request.getMaintenanceState());
        MaintenanceState oldState = h.getMaintenanceState(c.getClusterId());
        if (!newState.equals(oldState)) {
          if (newState.equals(MaintenanceState.IMPLIED_FROM_HOST)
              || newState.equals(MaintenanceState.IMPLIED_FROM_SERVICE)) {
            throw new IllegalArgumentException("Invalid arguments, can only set " +
              "maintenance state to one of " + EnumSet.of(MaintenanceState.OFF, MaintenanceState.ON));
          } else {
            h.setMaintenanceState(c.getClusterId(), newState);
            maintenanceCluster = c.getClusterName();
          }
        }
      }

      if (null != request.getClusterName() && null != request.getDesiredConfigs()) {
        Cluster c = clusters.getCluster(request.getClusterName());

        if (clusters.getHostsForCluster(request.getClusterName()).containsKey(h.getHostName())) {

          for (ConfigurationRequest cr : request.getDesiredConfigs()) {

            if (null != cr.getProperties() && cr.getProperties().size() > 0) {
              LOG.info(MessageFormat.format("Applying configuration with tag ''{0}'' to host ''{1}'' in cluster ''{2}''",
                  cr.getVersionTag(),
                  request.getHostname(),
                  request.getClusterName()));

              cr.setClusterName(c.getClusterName());
              controller.createConfiguration(cr);
            }

            Config baseConfig = c.getConfig(cr.getType(), cr.getVersionTag());
            if (null != baseConfig) {
              String authName = controller.getAuthName();
              DesiredConfig oldConfig = h.getDesiredConfigs(c.getClusterId()).get(cr.getType());

              if (h.addDesiredConfig(c.getClusterId(), cr.isSelected(), authName,  baseConfig)) {
                Logger logger = LoggerFactory.getLogger("configchange");
                logger.info("cluster '" + c.getClusterName() + "', "
                    + "host '" + h.getHostName() + "' "
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
      //todo: if attempt was made to update a property other than those
      //todo: that are allowed above, should throw exception
    }
    
    if (maintenanceCluster != null) {
      try {
        maintenanceStateHelper.createRequests(controller, requestProperties, maintenanceCluster);
      } catch (Exception e) {
        LOG.warn("Could not send maintenance status to Nagios (" + e.getMessage() + ")");
      }
    }
  }


  protected void deleteHosts(Set<HostRequest> requests)
      throws AmbariException {

    AmbariManagementController controller = getManagementController();
    Clusters                   clusters   = controller.getClusters();

    List<HostRequest> okToRemove = new ArrayList<HostRequest>();

    for (HostRequest hostRequest : requests) {
      String hostName = hostRequest.getHostname();
      if (null == hostName)
        continue;

      if (null != hostRequest.getClusterName()) {
        Cluster cluster = clusters.getCluster(hostRequest.getClusterName());

        List<ServiceComponentHost> list = cluster.getServiceComponentHosts(hostName);

        if (0 != list.size()) {
          StringBuilder reason = new StringBuilder("Cannot remove host ")
              .append(hostName)
              .append(" from ")
              .append(hostRequest.getClusterName())
              .append(".  The following roles exist: ");

          int i = 0;
          for (ServiceComponentHost sch : list) {
            if ((i++) > 0)
              reason.append(", ");
            reason.append(sch.getServiceComponentName());
          }

          throw new AmbariException(reason.toString());
        }
        okToRemove.add(hostRequest);

      } else {
        // check if host exists (throws exception if not found)
        clusters.getHost(hostName);

        // delete host outright
        Set<Cluster> clusterSet = clusters.getClustersForHost(hostName);
        if (0 != clusterSet.size()) {
          StringBuilder reason = new StringBuilder("Cannot remove host ")
              .append(hostName)
              .append(".  It belongs to clusters: ");
          int i = 0;
          for (Cluster c : clusterSet) {
            if ((i++) > 0)
              reason.append(", ");
            reason.append(c.getClusterName());
          }
          throw new AmbariException(reason.toString());
        }
        okToRemove.add(hostRequest);
      }
    }

    for (HostRequest hostRequest : okToRemove) {
      if (null != hostRequest.getClusterName()) {
        clusters.unmapHostFromCluster(hostRequest.getHostname(),
            hostRequest.getClusterName());
      } else {
        clusters.deleteHost(hostRequest.getHostname());
      }
    }
  }
}
