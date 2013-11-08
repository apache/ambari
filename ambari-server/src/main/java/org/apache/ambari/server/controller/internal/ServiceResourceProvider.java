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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.controller.ServiceRequest;
import org.apache.ambari.server.controller.ServiceResponse;
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
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.commons.lang.StringUtils;

/**
 * Resource provider for service resources.
 */
class ServiceResourceProvider extends AbstractControllerResourceProvider {


  // ----- Property ID constants ---------------------------------------------

  // Services
  protected static final String SERVICE_CLUSTER_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("ServiceInfo", "cluster_name");
  protected static final String SERVICE_SERVICE_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("ServiceInfo", "service_name");
  protected static final String SERVICE_SERVICE_STATE_PROPERTY_ID   = PropertyHelper.getPropertyId("ServiceInfo", "state");

  //Parameters from the predicate
  private static final String QUERY_PARAMETERS_RUN_SMOKE_TEST_ID =
    "params/run_smoke_test";

  private static final String QUERY_PARAMETERS_RECONFIGURE_CLIENT =
    "params/reconfigure_client";

  private static Set<String> pkPropertyIds =
      new HashSet<String>(Arrays.asList(new String[]{
          SERVICE_CLUSTER_NAME_PROPERTY_ID,
          SERVICE_SERVICE_NAME_PROPERTY_ID}));

  // Service state calculation
  private static final Map<String, ServiceState> serviceStateMap = new HashMap<String, ServiceState>();
  static {
    serviceStateMap.put("HDFS", new HDFSServiceState());
    serviceStateMap.put("HBASE", new HBaseServiceState());
  }

  private static final ServiceState DEFAULT_SERVICE_STATE = new DefaultServiceState();


  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds           the property ids
   * @param keyPropertyIds        the key property ids
   * @param managementController  the management controller
   */
  ServiceResourceProvider(Set<String> propertyIds,
                          Map<Resource.Type, String> keyPropertyIds,
                          AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException,
             UnsupportedPropertyException,
             ResourceAlreadyExistsException,
             NoSuchParentResourceException {

    final Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }
    createResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        createServices(requests);
        return null;
      }
    });
    notifyCreate(Resource.Type.Service, request);

    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws
      SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceRequest> requests = new HashSet<ServiceRequest>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }

    Set<ServiceResponse> responses = getResources(new Command<Set<ServiceResponse>>() {
      @Override
      public Set<ServiceResponse> invoke() throws AmbariException {
        return getServices(requests);
      }
    });

    Set<String>   requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources    = new HashSet<Resource>();

    for (ServiceResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.Service);
      setResourceProperty(resource, SERVICE_CLUSTER_NAME_PROPERTY_ID,
          response.getClusterName(), requestedIds);
      setResourceProperty(resource, SERVICE_SERVICE_NAME_PROPERTY_ID,
          response.getServiceName(), requestedIds);
      setResourceProperty(resource, SERVICE_SERVICE_STATE_PROPERTY_ID,
          calculateServiceState(response.getClusterName(), response.getServiceName()),
          requestedIds);
      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(final Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    RequestStatusResponse     response = null;

    Iterator<Map<String,Object>> iterator = request.getProperties().iterator();
    if (iterator.hasNext()) {
      for (Map<String, Object> propertyMap : getPropertyMaps(iterator.next(), predicate)) {
        requests.add(getRequest(propertyMap));
      }

      final boolean runSmokeTest = "true".equals(getQueryParameterValue(
          QUERY_PARAMETERS_RUN_SMOKE_TEST_ID, predicate));

      final boolean reconfigureClients = !"false".equals(getQueryParameterValue(
          QUERY_PARAMETERS_RECONFIGURE_CLIENT, predicate));

      response = modifyResources(new Command<RequestStatusResponse>() {
        @Override
        public RequestStatusResponse invoke() throws AmbariException {
          return updateServices(requests,
              request.getRequestInfoProperties(), runSmokeTest, reconfigureClients);
        }
      });
    }
    notifyUpdate(Resource.Type.Service, request, predicate);

    return getRequestStatus(response);
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }
    RequestStatusResponse response = modifyResources(new Command<RequestStatusResponse>() {
      @Override
      public RequestStatusResponse invoke() throws AmbariException {
        return deleteServices(requests);
      }
    });

    notifyDelete(Resource.Type.Service, predicate);
    return getRequestStatus(response);
  }

  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    propertyIds = super.checkPropertyIds(propertyIds);

    if (propertyIds.isEmpty()) {
      return propertyIds;
    }
    Set<String> unsupportedProperties = new HashSet<String>();

    for (String propertyId : propertyIds) {
      if (!propertyId.equals("config")) {
        String propertyCategory = PropertyHelper.getPropertyCategory(propertyId);
        if (propertyCategory == null || !propertyCategory.equals("config")) {
          unsupportedProperties.add(propertyId);
        }
      }
    }
    return unsupportedProperties;
  }


  // ----- AbstractResourceProvider ----------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }


  // ----- utility methods -------------------------------------------------
  /**
   * Get a service request object from a map of property values.
   *
   * @param properties  the predicate
   *
   * @return the service request object
   */
  private ServiceRequest getRequest(Map<String, Object> properties) {
    ServiceRequest svcRequest = new ServiceRequest(
        (String) properties.get(SERVICE_CLUSTER_NAME_PROPERTY_ID),
        (String) properties.get(SERVICE_SERVICE_NAME_PROPERTY_ID),
        (String) properties.get(SERVICE_SERVICE_STATE_PROPERTY_ID));

    return svcRequest;
  }

  // Create services from the given request.
  protected synchronized void createServices(Set<ServiceRequest> requests)
      throws AmbariException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return;
    }

    Clusters       clusters       = getManagementController().getClusters();
    AmbariMetaInfo ambariMetaInfo = getManagementController().getAmbariMetaInfo();

    // do all validation checks
    Map<String, Set<String>> serviceNames = new HashMap<String, Set<String>>();
    Set<String> duplicates = new HashSet<String>();
    for (ServiceRequest request : requests) {
      if (request.getClusterName() == null
          || request.getClusterName().isEmpty()
          || request.getServiceName() == null
          || request.getServiceName().isEmpty()) {
        throw new IllegalArgumentException("Cluster name and service name"
            + " should be provided when creating a service");
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a createService request"
            + ", clusterName=" + request.getClusterName()
            + ", serviceName=" + request.getServiceName()
            + ", request=" + request);
      }

      if (!serviceNames.containsKey(request.getClusterName())) {
        serviceNames.put(request.getClusterName(), new HashSet<String>());
      }
      if (serviceNames.get(request.getClusterName())
          .contains(request.getServiceName())) {
        // throw error later for dup
        duplicates.add(request.getServiceName());
        continue;
      }
      serviceNames.get(request.getClusterName()).add(request.getServiceName());

      if (request.getDesiredState() != null
          && !request.getDesiredState().isEmpty()) {
        State state = State.valueOf(request.getDesiredState());
        if (!state.isValidDesiredState()
            || state != State.INIT) {
          throw new IllegalArgumentException("Invalid desired state"
              + " only INIT state allowed during creation"
              + ", providedDesiredState=" + request.getDesiredState());
        }
      }

      Cluster cluster;
      try {
        cluster = clusters.getCluster(request.getClusterName());
      } catch (ClusterNotFoundException e) {
        throw new ParentObjectNotFoundException("Attempted to add a service to a cluster which doesn't exist", e);
      }
      try {
        Service s = cluster.getService(request.getServiceName());
        if (s != null) {
          // throw error later for dup
          duplicates.add(request.getServiceName());
          continue;
        }
      } catch (ServiceNotFoundException e) {
        // Expected
      }

      StackId stackId = cluster.getDesiredStackVersion();
      if (!ambariMetaInfo.isValidService(stackId.getStackName(),
          stackId.getStackVersion(), request.getServiceName())) {
        throw new IllegalArgumentException("Unsupported or invalid service"
            + " in stack"
            + ", clusterName=" + request.getClusterName()
            + ", serviceName=" + request.getServiceName()
            + ", stackInfo=" + stackId.getStackId());
      }
    }

    // ensure only a single cluster update
    if (serviceNames.size() != 1) {
      throw new IllegalArgumentException("Invalid arguments, updates allowed"
          + "on only one cluster at a time");
    }

    // Validate dups
    if (!duplicates.isEmpty()) {
      StringBuilder svcNames = new StringBuilder();
      boolean first = true;
      for (String svcName : duplicates) {
        if (!first) {
          svcNames.append(",");
        }
        first = false;
        svcNames.append(svcName);
      }
      String clusterName = requests.iterator().next().getClusterName();
      String msg;
      if (duplicates.size() == 1) {
        msg = "Attempted to create a service which already exists: "
            + ", clusterName=" + clusterName  + " serviceName=" + svcNames.toString();
      } else {
        msg = "Attempted to create services which already exist: "
            + ", clusterName=" + clusterName  + " serviceNames=" + svcNames.toString();
      }
      throw new DuplicateResourceException(msg);
    }

    ServiceFactory serviceFactory = getManagementController().getServiceFactory();

    // now to the real work
    for (ServiceRequest request : requests) {
      Cluster cluster = clusters.getCluster(request.getClusterName());

      State state = State.INIT;

      // Already checked that service does not exist
      Service s = serviceFactory.createNew(cluster, request.getServiceName());

      s.setDesiredState(state);
      s.setDesiredStackVersion(cluster.getDesiredStackVersion());
      cluster.addService(s);
      s.persist();
    }
  }

  // Get services from the given set of requests.
  protected Set<ServiceResponse> getServices(Set<ServiceRequest> requests)
      throws AmbariException {
    Set<ServiceResponse> response = new HashSet<ServiceResponse>();
    for (ServiceRequest request : requests) {
      try {
        response.addAll(getServices(request));
      } catch (ServiceNotFoundException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          throw e;
        }
      }
    }
    return response;
  }

  // Get services from the given request.
  private synchronized Set<ServiceResponse> getServices(ServiceRequest request)
      throws AmbariException {
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()) {
      throw new AmbariException("Invalid arguments, cluster name"
          + " cannot be null");
    }
    Clusters clusters    = getManagementController().getClusters();
    String   clusterName = request.getClusterName();

    final Cluster cluster;
    try {
      cluster = clusters.getCluster(clusterName);
    } catch (ObjectNotFoundException e) {
      throw new ParentObjectNotFoundException("Parent Cluster resource doesn't exist", e);
    }

    Set<ServiceResponse> response = new HashSet<ServiceResponse>();
    if (request.getServiceName() != null) {
      Service s = cluster.getService(request.getServiceName());
      response.add(s.convertToResponse());
      return response;
    }

    // TODO support search on predicates?

    boolean checkDesiredState = false;
    State desiredStateToCheck = null;
    if (request.getDesiredState() != null
        && !request.getDesiredState().isEmpty()) {
      desiredStateToCheck = State.valueOf(request.getDesiredState());
      if (!desiredStateToCheck.isValidDesiredState()) {
        throw new IllegalArgumentException("Invalid arguments, invalid desired"
            + " state, desiredState=" + desiredStateToCheck);
      }
      checkDesiredState = true;
    }

    for (Service s : cluster.getServices().values()) {
      if (checkDesiredState
          && (desiredStateToCheck != s.getDesiredState())) {
        // skip non matching state
        continue;
      }
      response.add(s.convertToResponse());
    }
    return response;
  }

  // Update services based on the given requests.
  protected synchronized RequestStatusResponse updateServices(
      Set<ServiceRequest> requests, Map<String, String> requestProperties,
      boolean runSmokeTest, boolean reconfigureClients) throws AmbariException {

    AmbariManagementController controller = getManagementController();

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return null;
    }

    Map<State, List<Service>> changedServices
        = new HashMap<State, List<Service>>();
    Map<State, List<ServiceComponent>> changedComps =
        new HashMap<State, List<ServiceComponent>>();
    Map<String, Map<State, List<ServiceComponentHost>>> changedScHosts =
        new HashMap<String, Map<State, List<ServiceComponentHost>>>();
    Collection<ServiceComponentHost> ignoredScHosts =
        new ArrayList<ServiceComponentHost>();

    Set<String> clusterNames = new HashSet<String>();
    Map<String, Set<String>> serviceNames = new HashMap<String, Set<String>>();
    Set<State> seenNewStates = new HashSet<State>();

    Clusters       clusters       = controller.getClusters();
    AmbariMetaInfo ambariMetaInfo = controller.getAmbariMetaInfo();

    for (ServiceRequest request : requests) {
      if (request.getClusterName() == null
          || request.getClusterName().isEmpty()
          || request.getServiceName() == null
          || request.getServiceName().isEmpty()) {
        throw new IllegalArgumentException("Invalid arguments, cluster name"
            + " and service name should be provided to update services");
      }

      LOG.info("Received a updateService request"
          + ", clusterName=" + request.getClusterName()
          + ", serviceName=" + request.getServiceName()
          + ", request=" + request.toString());

      clusterNames.add(request.getClusterName());

      if (clusterNames.size() > 1) {
        throw new IllegalArgumentException("Updates to multiple clusters is not"
            + " supported");
      }

      if (!serviceNames.containsKey(request.getClusterName())) {
        serviceNames.put(request.getClusterName(), new HashSet<String>());
      }
      if (serviceNames.get(request.getClusterName())
          .contains(request.getServiceName())) {
        // TODO throw single exception
        throw new IllegalArgumentException("Invalid request contains duplicate"
            + " service names");
      }
      serviceNames.get(request.getClusterName()).add(request.getServiceName());

      Cluster cluster = clusters.getCluster(request.getClusterName());
      Service s = cluster.getService(request.getServiceName());
      State oldState = s.getDesiredState();
      State newState = null;
      if (request.getDesiredState() != null) {
        newState = State.valueOf(request.getDesiredState());
        if (!newState.isValidDesiredState()) {
          throw new IllegalArgumentException("Invalid arguments, invalid"
              + " desired state, desiredState=" + newState);
        }
      }

      if (newState == null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Nothing to do for new updateService request"
              + ", clusterName=" + request.getClusterName()
              + ", serviceName=" + request.getServiceName()
              + ", newDesiredState=null");
        }
        continue;
      }

      seenNewStates.add(newState);

      if (newState != oldState) {
        if (!State.isValidDesiredStateTransition(oldState, newState)) {
          throw new AmbariException("Invalid transition for"
              + " service"
              + ", clusterName=" + cluster.getClusterName()
              + ", clusterId=" + cluster.getClusterId()
              + ", serviceName=" + s.getName()
              + ", currentDesiredState=" + oldState
              + ", newDesiredState=" + newState);

        }
        if (!changedServices.containsKey(newState)) {
          changedServices.put(newState, new ArrayList<Service>());
        }
        changedServices.get(newState).add(s);
      }

      // TODO should we check whether all servicecomponents and
      // servicecomponenthosts are in the required desired state?

      for (ServiceComponent sc : s.getServiceComponents().values()) {
        State oldScState = sc.getDesiredState();
        if (newState != oldScState) {
          if (sc.isClientComponent() &&
              !newState.isValidClientComponentState()) {
            continue;
          }
          if (!State.isValidDesiredStateTransition(oldScState, newState)) {
            throw new AmbariException("Invalid transition for"
                + " servicecomponent"
                + ", clusterName=" + cluster.getClusterName()
                + ", clusterId=" + cluster.getClusterId()
                + ", serviceName=" + sc.getServiceName()
                + ", componentName=" + sc.getName()
                + ", currentDesiredState=" + oldScState
                + ", newDesiredState=" + newState);
          }
          if (!changedComps.containsKey(newState)) {
            changedComps.put(newState, new ArrayList<ServiceComponent>());
          }
          changedComps.get(newState).add(sc);
        }
        if (LOG.isDebugEnabled()) {
          LOG.debug("Handling update to ServiceComponent"
              + ", clusterName=" + request.getClusterName()
              + ", serviceName=" + s.getName()
              + ", componentName=" + sc.getName()
              + ", currentDesiredState=" + oldScState
              + ", newDesiredState=" + newState);
        }
        for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()){
          State oldSchState = sch.getState();
          if (oldSchState == State.MAINTENANCE || oldSchState == State.UNKNOWN) {
            //Ignore host components updates in this state
            if (LOG.isDebugEnabled()) {
              LOG.debug("Ignoring ServiceComponentHost"
                  + ", clusterName=" + request.getClusterName()
                  + ", serviceName=" + s.getName()
                  + ", componentName=" + sc.getName()
                  + ", hostname=" + sch.getHostName()
                  + ", currentState=" + oldSchState
                  + ", newDesiredState=" + newState);
            }
            continue;
          }
          if (newState == oldSchState) {
            ignoredScHosts.add(sch);
            if (LOG.isDebugEnabled()) {
              LOG.debug("Ignoring ServiceComponentHost"
                  + ", clusterName=" + request.getClusterName()
                  + ", serviceName=" + s.getName()
                  + ", componentName=" + sc.getName()
                  + ", hostname=" + sch.getHostName()
                  + ", currentState=" + oldSchState
                  + ", newDesiredState=" + newState);
            }
            continue;
          }
          if (sc.isClientComponent() &&
              !newState.isValidClientComponentState()) {
            continue;
          }
          /**
           * This is hack for now wherein we don't fail if the
           * sch is in INSTALL_FAILED
           */
          if (!State.isValidStateTransition(oldSchState, newState)) {
            String error = "Invalid transition for"
                + " servicecomponenthost"
                + ", clusterName=" + cluster.getClusterName()
                + ", clusterId=" + cluster.getClusterId()
                + ", serviceName=" + sch.getServiceName()
                + ", componentName=" + sch.getServiceComponentName()
                + ", hostname=" + sch.getHostName()
                + ", currentState=" + oldSchState
                + ", newDesiredState=" + newState;
            StackId sid = cluster.getDesiredStackVersion();

            if ( ambariMetaInfo.getComponentCategory(
                sid.getStackName(), sid.getStackVersion(), sc.getServiceName(),
                sch.getServiceComponentName()).isMaster()) {
              throw new AmbariException(error);
            } else {
              LOG.warn("Ignoring: " + error);
              continue;
            }
          }
          if (!changedScHosts.containsKey(sc.getName())) {
            changedScHosts.put(sc.getName(),
                new HashMap<State, List<ServiceComponentHost>>());
          }
          if (!changedScHosts.get(sc.getName()).containsKey(newState)) {
            changedScHosts.get(sc.getName()).put(newState,
                new ArrayList<ServiceComponentHost>());
          }
          if (LOG.isDebugEnabled()) {
            LOG.debug("Handling update to ServiceComponentHost"
                + ", clusterName=" + request.getClusterName()
                + ", serviceName=" + s.getName()
                + ", componentName=" + sc.getName()
                + ", hostname=" + sch.getHostName()
                + ", currentState=" + oldSchState
                + ", newDesiredState=" + newState);
          }
          changedScHosts.get(sc.getName()).get(newState).add(sch);
        }
      }
    }

    if (seenNewStates.size() > 1) {
      // TODO should we handle this scenario
      throw new IllegalArgumentException("Cannot handle different desired state"
          + " changes for a set of services at the same time");
    }

    Cluster cluster = clusters.getCluster(clusterNames.iterator().next());

    return controller.createStages(cluster, requestProperties, null, changedServices, changedComps, changedScHosts,
        ignoredScHosts, runSmokeTest, reconfigureClients);
  }

  // Delete services based on the given set of requests
  protected RequestStatusResponse deleteServices(Set<ServiceRequest> request)
      throws AmbariException {

    Clusters clusters    = getManagementController().getClusters();

    for (ServiceRequest serviceRequest : request) {
      if (StringUtils.isEmpty(serviceRequest.getClusterName()) || StringUtils.isEmpty(serviceRequest.getServiceName())) {
        // FIXME throw correct error
        throw new AmbariException("invalid arguments");
      } else {
        clusters.getCluster(serviceRequest.getClusterName()).deleteService(serviceRequest.getServiceName());
      }
    }
    return null;
  }

  // Get the State of a host component
  private static State getHostComponentState(ServiceComponentHostResponse hostComponent) {
    return State.valueOf(hostComponent.getLiveState());
  }

  // calculate the service state, accounting for the state of the host components
  private String calculateServiceState(String clusterName, String serviceName) {

    ServiceState serviceState = serviceStateMap.get(serviceName);
    if (serviceState == null) {
      serviceState = DEFAULT_SERVICE_STATE;
    }
    State state = serviceState.getState(getManagementController(), clusterName, serviceName);

    return state.toString();
  }


  // ----- inner class ServiceState ------------------------------------------

  /**
   * Interface to allow for different state calculations for different services.
   * TODO : see if this functionality can be moved to service definitions.
   */
  protected interface ServiceState {
    public State getState(AmbariManagementController controller, String clusterName, String serviceName);
  }

  /**
   * Default calculator of service state.
   */
  protected static class DefaultServiceState implements ServiceState {

    @Override
    public State getState(AmbariManagementController controller, String clusterName, String serviceName) {
      AmbariMetaInfo ambariMetaInfo = controller.getAmbariMetaInfo();
      Clusters       clusters       = controller.getClusters();

      if (clusterName != null && clusterName.length() > 0) {
        try {
          Cluster cluster = clusters.getCluster(clusterName);
          if (cluster != null) {
            StackId stackId = cluster.getDesiredStackVersion();

            ServiceComponentHostRequest request = new ServiceComponentHostRequest(clusterName,
                serviceName, null, null, null);

            Set<ServiceComponentHostResponse> hostComponentResponses =
                controller.getHostComponents(Collections.singleton(request));

            for (ServiceComponentHostResponse hostComponentResponse : hostComponentResponses ) {
              ComponentInfo componentInfo = ambariMetaInfo.getComponentCategory(stackId.getStackName(),
                  stackId.getStackVersion(), hostComponentResponse.getServiceName(),
                  hostComponentResponse.getComponentName());

              if (componentInfo != null) {
                if (componentInfo.isMaster()) {
                  State state = getHostComponentState(hostComponentResponse);
                  switch (state) {
                    case STARTED:
                    case MAINTENANCE:
                      break;
                    default:
                      return state;
                  }
                }
              }
            }
            return State.STARTED;
          }
        } catch (AmbariException e) {
          LOG.error("Can't determine service state.", e);
        }
      }
      return State.UNKNOWN;
    }
  }

  /**
   * Calculator of HDFS service state.
   */
  protected static class HDFSServiceState implements ServiceState {

    @Override
    public State getState(AmbariManagementController controller,String clusterName, String serviceName) {
      AmbariMetaInfo ambariMetaInfo = controller.getAmbariMetaInfo();
      Clusters       clusters       = controller.getClusters();

      if (clusterName != null && clusterName.length() > 0) {
        try {
          Cluster cluster = clusters.getCluster(clusterName);
          if (cluster != null) {
            StackId stackId = cluster.getDesiredStackVersion();

            ServiceComponentHostRequest request = new ServiceComponentHostRequest(clusterName,
                serviceName, null, null, null);

            Set<ServiceComponentHostResponse> hostComponentResponses =
                controller.getHostComponents(Collections.singleton(request));

            int     nameNodeCount       = 0;
            int     nameNodeActiveCount = 0;
            boolean hasSecondary        = false;
            boolean hasJournal          = false;
            State   nonStartedState     = null;

            for (ServiceComponentHostResponse hostComponentResponse : hostComponentResponses ) {
              ComponentInfo componentInfo = ambariMetaInfo.getComponentCategory(stackId.getStackName(),
                  stackId.getStackVersion(), hostComponentResponse.getServiceName(),
                  hostComponentResponse.getComponentName());

              if (componentInfo != null) {
                if (componentInfo.isMaster()) {

                  String componentName = hostComponentResponse.getComponentName();
                  boolean isNameNode = false;

                  if (componentName.equals("NAMENODE")) {
                    ++nameNodeCount;
                    isNameNode = true;
                  } else if (componentName.equals("SECONDARY_NAMENODE")) {
                    hasSecondary = true;
                  } else if (componentName.equals("JOURNALNODE")) {
                    hasJournal = true;
                  }

                  State state = getHostComponentState(hostComponentResponse);

                  switch (state) {
                    case STARTED:
                    case MAINTENANCE:
                      if (isNameNode) {
                        ++nameNodeActiveCount;
                      }
                      break;
                    default:
                      nonStartedState = state;
                  }
                }
              }
            }

            if ( nonStartedState == null ||  // all started
                ((nameNodeCount > 0 && !hasSecondary || hasJournal) &&
                    nameNodeActiveCount > 0)) {  // at least one active namenode
              return State.STARTED;
            }
            return nonStartedState;
          }
        } catch (AmbariException e) {
          LOG.error("Can't determine service state.", e);
        }
      }
      return State.UNKNOWN;
    }
  }

  /**
   * Calculator of HBase service state.
   */
  protected static class HBaseServiceState implements ServiceState {

    @Override
    public State getState(AmbariManagementController controller,String clusterName, String serviceName) {
      AmbariMetaInfo ambariMetaInfo = controller.getAmbariMetaInfo();
      Clusters       clusters       = controller.getClusters();

      if (clusterName != null && clusterName.length() > 0) {
        try {
          Cluster cluster = clusters.getCluster(clusterName);
          if (cluster != null) {
            StackId stackId = cluster.getDesiredStackVersion();

            ServiceComponentHostRequest request = new ServiceComponentHostRequest(clusterName,
                serviceName, null, null, null);

            Set<ServiceComponentHostResponse> hostComponentResponses =
                controller.getHostComponents(Collections.singleton(request));

            int     hBaseMasterActiveCount = 0;
            State   nonStartedState        = null;

            for (ServiceComponentHostResponse hostComponentResponse : hostComponentResponses ) {
              ComponentInfo componentInfo = ambariMetaInfo.getComponentCategory(stackId.getStackName(),
                  stackId.getStackVersion(), hostComponentResponse.getServiceName(),
                  hostComponentResponse.getComponentName());

              if (componentInfo != null) {
                if (componentInfo.isMaster()) {

                  State state = getHostComponentState(hostComponentResponse);

                  switch (state) {
                    case STARTED:
                    case MAINTENANCE:
                      String componentName = hostComponentResponse.getComponentName();
                      if (componentName.equals("HBASE_MASTER")) {
                        ++hBaseMasterActiveCount;
                      }
                      break;
                    default:
                      nonStartedState = state;
                  }
                }
              }
            }

            if ( nonStartedState == null ||     // all started
                hBaseMasterActiveCount > 0) {   // at least one active hbase_master
              return State.STARTED;
            }
            return nonStartedState;
          }
        } catch (AmbariException e) {
          LOG.error("Can't determine service state.", e);
        }
      }
      return State.UNKNOWN;    }
  }
}
