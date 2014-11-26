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
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.predicate.NotPredicate;
import org.apache.ambari.server.controller.predicate.OrPredicate;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostDisableEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostRestoreEvent;
import org.apache.commons.lang.StringUtils;

/**
 * Resource provider for host component resources.
 */
public class HostComponentResourceProvider extends AbstractControllerResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  // Host Components
  protected static final String HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID
      = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
  protected static final String HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID
      = PropertyHelper.getPropertyId("HostRoles", "service_name");
  protected static final String HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID
      = PropertyHelper.getPropertyId("HostRoles", "component_name");
  protected static final String HOST_COMPONENT_HOST_NAME_PROPERTY_ID
      = PropertyHelper.getPropertyId("HostRoles", "host_name");
  protected static final String HOST_COMPONENT_STATE_PROPERTY_ID
      = PropertyHelper.getPropertyId("HostRoles", "state");
  protected static final String HOST_COMPONENT_DESIRED_STATE_PROPERTY_ID
      = PropertyHelper.getPropertyId("HostRoles", "desired_state");
  protected static final String HOST_COMPONENT_STACK_ID_PROPERTY_ID
      = PropertyHelper.getPropertyId("HostRoles", "stack_id");
  protected static final String HOST_COMPONENT_DESIRED_STACK_ID_PROPERTY_ID
      = PropertyHelper.getPropertyId("HostRoles", "desired_stack_id");
  protected static final String HOST_COMPONENT_ACTUAL_CONFIGS_PROPERTY_ID
      = PropertyHelper.getPropertyId("HostRoles", "actual_configs");
  protected static final String HOST_COMPONENT_STALE_CONFIGS_PROPERTY_ID
      = PropertyHelper.getPropertyId("HostRoles", "stale_configs");
  protected static final String HOST_COMPONENT_DESIRED_ADMIN_STATE_PROPERTY_ID
      = PropertyHelper.getPropertyId("HostRoles", "desired_admin_state");
  protected static final String HOST_COMPONENT_MAINTENANCE_STATE_PROPERTY_ID
      = "HostRoles/maintenance_state";

  //Component name mappings
  private final Map<String, PropertyProvider> HOST_COMPONENT_PROPERTIES_PROVIDER = new HashMap<String, PropertyProvider>();
  private static final int HOST_COMPONENT_HTTP_PROPERTY_REQUEST_CONNECT_TIMEOUT = 1500;   //milliseconds
  private static final int HOST_COMPONENT_HTTP_PROPERTY_REQUEST_READ_TIMEOUT = 10000;  //milliseconds

  //Parameters from the predicate
  private static final String QUERY_PARAMETERS_RUN_SMOKE_TEST_ID =
      "params/run_smoke_test";
  private static Set<String> pkPropertyIds =
      new HashSet<String>(Arrays.asList(new String[]{
          HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID,
          HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID,
          HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID,
          HOST_COMPONENT_HOST_NAME_PROPERTY_ID}));

  /**
   * maintenance state helper
   */
  @Inject
  private MaintenanceStateHelper maintenanceStateHelper;


  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds          the property ids
   * @param keyPropertyIds       the key property ids
   * @param managementController the management controller
   */
  @AssistedInject
  public HostComponentResourceProvider(@Assisted Set<String> propertyIds,
                                       @Assisted Map<Resource.Type, String> keyPropertyIds,
                                       @Assisted AmbariManagementController managementController,
                                       Injector injector) {
    super(propertyIds, keyPropertyIds, managementController);
    ComponentSSLConfiguration configuration = ComponentSSLConfiguration.instance();
    URLStreamProvider streamProvider = new URLStreamProvider(
            HOST_COMPONENT_HTTP_PROPERTY_REQUEST_CONNECT_TIMEOUT,
            HOST_COMPONENT_HTTP_PROPERTY_REQUEST_READ_TIMEOUT,
            configuration.getTruststorePath(), configuration.getTruststorePassword(), configuration.getTruststoreType());

    HttpProxyPropertyProvider httpPropertyProvider = new HttpProxyPropertyProvider(streamProvider,
            configuration, injector,
            PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
            PropertyHelper.getPropertyId("HostRoles", "host_name"),
            PropertyHelper.getPropertyId("HostRoles", "component_name"));

    HOST_COMPONENT_PROPERTIES_PROVIDER.put("RESOURCEMANAGER", httpPropertyProvider);
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException,
      UnsupportedPropertyException,
      ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    final Set<ServiceComponentHostRequest> requests = new HashSet<ServiceComponentHostRequest>();
    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }

    createResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        getManagementController().createHostComponents(requests);
        return null;
      }
    });

    notifyCreate(Resource.Type.HostComponent, request);

    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceComponentHostRequest> requests = new HashSet<ServiceComponentHostRequest>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }

    Set<Resource> resources = new HashSet<Resource>();
    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<ServiceComponentHostResponse> responses = getResources(new Command<Set<ServiceComponentHostResponse>>() {
      @Override
      public Set<ServiceComponentHostResponse> invoke() throws AmbariException {
        return getManagementController().getHostComponents(requests);
      }
    });

    for (ServiceComponentHostResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.HostComponent);
      setResourceProperty(resource, HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID,
          response.getClusterName(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID,
          response.getServiceName(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID,
          response.getComponentName(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_HOST_NAME_PROPERTY_ID,
          response.getHostname(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_STATE_PROPERTY_ID,
          response.getLiveState(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_DESIRED_STATE_PROPERTY_ID,
          response.getDesiredState(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_STACK_ID_PROPERTY_ID,
          response.getStackVersion(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_DESIRED_STACK_ID_PROPERTY_ID,
          response.getDesiredStackVersion(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_ACTUAL_CONFIGS_PROPERTY_ID,
          response.getActualConfigs(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_STALE_CONFIGS_PROPERTY_ID,
          response.isStaleConfig(), requestedIds);
      
      if (response.getAdminState() != null) {
        setResourceProperty(resource, HOST_COMPONENT_DESIRED_ADMIN_STATE_PROPERTY_ID,
            response.getAdminState(), requestedIds);
      }
      
      if (null != response.getMaintenanceState()) {
        setResourceProperty(resource, HOST_COMPONENT_MAINTENANCE_STATE_PROPERTY_ID,
            response.getMaintenanceState(), requestedIds);
      }

      String componentName = (String) resource.getPropertyValue(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID);
      PropertyProvider propertyProvider = HOST_COMPONENT_PROPERTIES_PROVIDER.get(componentName);
      if (propertyProvider != null) {
        Set<Resource> resourcesToPopulate = new HashSet<Resource>();
        resourcesToPopulate.add(resource);
        propertyProvider.populateResources(resourcesToPopulate, request, predicate);
      }

      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(final Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    if (request.getProperties().isEmpty()) {
      throw new IllegalArgumentException("Received an update request with no properties");
    }

    RequestStageContainer requestStages = doUpdateResources(null, request, predicate);

    RequestStatusResponse response = null;
    if (requestStages != null) {
      try {
        requestStages.persist();
      } catch (AmbariException e) {
        throw new SystemException(e.getMessage(), e);
      }
      response = requestStages.getRequestStatusResponse();
      notifyUpdate(Resource.Type.HostComponent, request, predicate);
    }

    return getRequestStatus(response);
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<ServiceComponentHostRequest> requests = new HashSet<ServiceComponentHostRequest>();
    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }
    RequestStatusResponse response = modifyResources(new Command<RequestStatusResponse>() {
      @Override
      public RequestStatusResponse invoke() throws AmbariException {
        return getManagementController().deleteHostComponents(requests);
      }
    });

    notifyDelete(Resource.Type.HostComponent, predicate);

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

  RequestStatusResponse installAndStart(String cluster, Collection<String> hosts) throws  SystemException,
      UnsupportedPropertyException, NoSuchParentResourceException {

    final RequestStageContainer requestStages;
    Map<String, Object> installProperties = new HashMap<String, Object>();

    installProperties.put(HOST_COMPONENT_STATE_PROPERTY_ID, "INSTALLED");
    Map<String, String> requestInfo = new HashMap<String, String>();
    requestInfo.put("context", "Install and start components on added hosts");
    Request installRequest = PropertyHelper.getUpdateRequest(installProperties, requestInfo);

    Collection<EqualsPredicate> hostPredicates = new ArrayList<EqualsPredicate>();
    for (String host : hosts) {
      hostPredicates.add(new EqualsPredicate<String>(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, host));
    }

    Predicate statePredicate = new EqualsPredicate<String>(HOST_COMPONENT_STATE_PROPERTY_ID, "INIT");
    Predicate clusterPredicate = new EqualsPredicate<String>(HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID, cluster);
    Predicate hostPredicate = new OrPredicate(hostPredicates.toArray(new Predicate[hostPredicates.size()]));
    Predicate hostAndStatePredicate = new AndPredicate(statePredicate, hostPredicate);
    Predicate installPredicate = new AndPredicate(hostAndStatePredicate, clusterPredicate);

    try {
      LOG.info("Installing all components on added hosts");
      requestStages = doUpdateResources(null, installRequest, installPredicate);
      notifyUpdate(Resource.Type.HostComponent, installRequest, installPredicate);

      Map<String, Object> startProperties = new HashMap<String, Object>();
      startProperties.put(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");
      Request startRequest = PropertyHelper.getUpdateRequest(startProperties, requestInfo);
      // Important to query against desired_state as this has been updated when install stage was created
      // If I query against state, then the getRequest compares predicate prop against desired_state and then when the predicate
      // is later applied explicitly, it gets compared to live_state. Since live_state == INSTALLED == INIT at this point and
      // desired_state == INSTALLED, we will always get 0 matches since both comparisons can't be true :(
      Predicate installedStatePredicate = new EqualsPredicate<String>(HOST_COMPONENT_DESIRED_STATE_PROPERTY_ID, "INSTALLED");
      Predicate notClientPredicate = new NotPredicate(new ClientComponentPredicate());
      Predicate clusterAndClientPredicate = new AndPredicate(clusterPredicate, notClientPredicate);
      hostAndStatePredicate = new AndPredicate(installedStatePredicate, hostPredicate);
      Predicate startPredicate = new AndPredicate(clusterAndClientPredicate, hostAndStatePredicate);

      LOG.info("Starting all non-client components on added hosts");
      //todo: if a host in in state HEARTBEAT_LOST, no stage will be created, so if this occurs during INSTALL
      //todo: then no INSTALL stage will exist which will result in invalid state transition INIT->STARTED
      doUpdateResources(requestStages, startRequest, startPredicate);
      notifyUpdate(Resource.Type.HostComponent, startRequest, startPredicate);
      try {
        requestStages.persist();
      } catch (AmbariException e) {
        throw new SystemException(e.getMessage(), e);
      }
      return requestStages.getRequestStatusResponse();
    } catch (NoSuchResourceException e) {
      // shouldn't encounter this exception here
      throw new SystemException("An unexpected exception occurred while processing add hosts",  e);
    }
  }


  /**
   * Update the host component identified by the given request object with the
   * values carried by the given request object.
   *
   * @param stages             stages of the associated request
   * @param requests           the request object which defines which host component to
   *                           update and the values to set
   * @param requestProperties  the request properties
   * @param runSmokeTest       indicates whether or not to run a smoke test
   *
   * @return a track action response
   *
   * @throws AmbariException thrown if the resource cannot be updated
   */
  //todo: This was moved from AmbariManagementController and needs a lot of refactoring.
  //todo: Look into using the predicate instead of Set<ServiceComponentHostRequest>
  //todo: change to private access when all AMC tests have been moved.
  protected synchronized RequestStageContainer updateHostComponents(RequestStageContainer stages,
                                                                    Set<ServiceComponentHostRequest> requests,
                                                                    Map<String, String> requestProperties,
                                                                    boolean runSmokeTest) throws AmbariException {

    Clusters clusters = getManagementController().getClusters();


    Map<String, Map<State, List<ServiceComponentHost>>> changedScHosts = new HashMap<String, Map<State, List<ServiceComponentHost>>>();
    Collection<ServiceComponentHost> ignoredScHosts = new ArrayList<ServiceComponentHost>();
    Set<String> clusterNames = new HashSet<String>();
    Map<String, Map<String, Map<String, Set<String>>>> requestClusters = new HashMap<String, Map<String, Map<String, Set<String>>>>();
    Map<ServiceComponentHost, State> directTransitionScHosts = new HashMap<ServiceComponentHost, State>();

    Resource.Type reqOpLvl = determineOperationLevel(requestProperties);


    for (ServiceComponentHostRequest request : requests) {
      validateServiceComponentHostRequest(request);

      Cluster cluster = clusters.getCluster(request.getClusterName());

      if (StringUtils.isEmpty(request.getServiceName())) {
        request.setServiceName(getManagementController().findServiceName(cluster, request.getComponentName()));
      }

      ServiceComponent sc = getServiceComponent(
          request.getClusterName(), request.getServiceName(), request.getComponentName());

      logRequestInfo("Received a updateHostComponent request", request);

      clusterNames.add(request.getClusterName());

      if (clusterNames.size() > 1) {
        throw new IllegalArgumentException("Updates to multiple clusters is not"
            + " supported");
      }

      // maps of cluster->services, services->components, components->hosts
      Map<String, Map<String, Set<String>>> clusterServices = requestClusters.get(request.getClusterName());
      if (clusterServices == null) {
        clusterServices = new HashMap<String, Map<String, Set<String>>>();
        requestClusters.put(request.getClusterName(), clusterServices);
      }

      Map<String, Set<String>> serviceComponents = clusterServices.get(request.getServiceName());
      if (serviceComponents == null) {
        serviceComponents = new HashMap<String, Set<String>>();
        clusterServices.put(request.getServiceName(), serviceComponents);
      }

      Set<String> componentHosts = serviceComponents.get(request.getComponentName());
      if (componentHosts == null) {
        componentHosts = new HashSet<String>();
        serviceComponents.put(request.getComponentName(), componentHosts) ;
      }

      if (componentHosts.contains(request.getHostname())) {
        throw new IllegalArgumentException("Invalid request contains duplicate hostcomponents");
      }

      componentHosts.add(request.getHostname());


      ServiceComponentHost sch = sc.getServiceComponentHost(request.getHostname());
      State oldState = sch.getState();
      State newState = null;
      if (request.getDesiredState() != null) {
        // set desired state on host component
        newState = State.valueOf(request.getDesiredState());
        // throw exception if desired state isn't a valid desired state (static check)
        if (!newState.isValidDesiredState()) {
          throw new IllegalArgumentException("Invalid arguments, invalid"
              + " desired state, desiredState=" + newState.toString());
        }
      }

      // Setting Maintenance state for host component
      if (null != request.getMaintenanceState()) {
        MaintenanceState newMaint = MaintenanceState.valueOf(request.getMaintenanceState());
        MaintenanceState oldMaint = maintenanceStateHelper.getEffectiveState(sch);

        if (newMaint != oldMaint) {
          if (sc.isClientComponent()) {
            throw new IllegalArgumentException("Invalid arguments, cannot set maintenance state on a client component");
          } else if (newMaint.equals(MaintenanceState.IMPLIED_FROM_HOST)  || newMaint.equals(MaintenanceState.IMPLIED_FROM_SERVICE)) {
            throw new IllegalArgumentException("Invalid arguments, can only set maintenance state to one of " +
                EnumSet.of(MaintenanceState.OFF, MaintenanceState.ON));
          } else {
            sch.setMaintenanceState(newMaint);
          }
        }
      }

      if (newState == null) {
        logComponentInfo("Nothing to do for new updateServiceComponentHost", request, oldState, null);
        continue;
      }

      if (sc.isClientComponent() &&
          !newState.isValidClientComponentState()) {
        throw new IllegalArgumentException("Invalid desired state for a client"
            + " component");
      }

      State oldSchState = sch.getState();
      // Client component reinstall allowed
      if (newState == oldSchState && !sc.isClientComponent() &&
          !requestProperties.containsKey(sch.getServiceComponentName().toLowerCase())) {

        ignoredScHosts.add(sch);
        logComponentInfo("Ignoring ServiceComponentHost", request, oldState, newState);
        continue;
      }

      if (! maintenanceStateHelper.isOperationAllowed(reqOpLvl, sch)) {
        ignoredScHosts.add(sch);
        logComponentInfo("Ignoring ServiceComponentHost", request, oldState, newState);
        continue;
      }

      if (! isValidStateTransition(stages, oldSchState, newState, sch)) {
        throw new AmbariException("Invalid state transition for host component"
            + ", clusterName=" + cluster.getClusterName()
            + ", clusterId=" + cluster.getClusterId()
            + ", serviceName=" + sch.getServiceName()
            + ", componentName=" + sch.getServiceComponentName()
            + ", hostname=" + sch.getHostName()
            + ", currentState=" + oldSchState
            + ", newDesiredState=" + newState);
      }

      if (isDirectTransition(oldSchState, newState)) {
        logComponentInfo("Handling direct transition update to host component", request, oldState, newState);
        directTransitionScHosts.put(sch, newState);
      } else {
        if (!changedScHosts.containsKey(sc.getName())) {
          changedScHosts.put(sc.getName(),
              new EnumMap<State, List<ServiceComponentHost>>(State.class));
        }
        if (!changedScHosts.get(sc.getName()).containsKey(newState)) {
          changedScHosts.get(sc.getName()).put(newState,
              new ArrayList<ServiceComponentHost>());
        }
        logComponentInfo("Handling update to host component", request, oldState, newState);
        changedScHosts.get(sc.getName()).get(newState).add(sch);
      }
    }

    doDirectTransitions(directTransitionScHosts);

    // just getting the first cluster
    Cluster cluster = clusters.getCluster(clusterNames.iterator().next());

    return getManagementController().addStages(stages, cluster, requestProperties, null, null, null,
        changedScHosts, ignoredScHosts, runSmokeTest, false);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }


  // ----- utility methods -------------------------------------------------

  /**
   * Get a component request object from a map of property values.
   *
   * @param properties the predicate
   * @return the component request object
   */
  private ServiceComponentHostRequest getRequest(Map<String, Object> properties) {
    ServiceComponentHostRequest serviceComponentHostRequest = new ServiceComponentHostRequest(
        (String) properties.get(HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID),
        (String) properties.get(HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID),
        (String) properties.get(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID),
        (String) properties.get(HOST_COMPONENT_HOST_NAME_PROPERTY_ID),
        (String) properties.get(HOST_COMPONENT_STATE_PROPERTY_ID));
    serviceComponentHostRequest.setDesiredStackId(
        (String) properties.get(HOST_COMPONENT_STACK_ID_PROPERTY_ID));
    if (properties.get(HOST_COMPONENT_STALE_CONFIGS_PROPERTY_ID) != null) {
      serviceComponentHostRequest.setStaleConfig(
          properties.get(HOST_COMPONENT_STALE_CONFIGS_PROPERTY_ID).toString().toLowerCase());
    }
    
    if (properties.get(HOST_COMPONENT_DESIRED_ADMIN_STATE_PROPERTY_ID) != null) {
      serviceComponentHostRequest.setAdminState(
          properties.get(HOST_COMPONENT_DESIRED_ADMIN_STATE_PROPERTY_ID).toString());
    }
    
    Object o = properties.get(HOST_COMPONENT_MAINTENANCE_STATE_PROPERTY_ID);
    if (null != o) {
      serviceComponentHostRequest.setMaintenanceState (o.toString());
    }

    return serviceComponentHostRequest;
  }

  private RequestStageContainer doUpdateResources(final RequestStageContainer stages, final Request request, Predicate predicate)
      throws UnsupportedPropertyException, SystemException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceComponentHostRequest> requests = new HashSet<ServiceComponentHostRequest>();

    final boolean runSmokeTest = "true".equals(getQueryParameterValue(
        QUERY_PARAMETERS_RUN_SMOKE_TEST_ID, predicate));

    Set<String> queryIds = Collections.singleton(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID);

    Request queryRequest = PropertyHelper.getReadRequest(queryIds);
    // will take care of 404 exception
    Set<Resource> matchingResources = getResources(queryRequest, predicate);

    for (Resource queryResource : matchingResources) {
      if (predicate.evaluate(queryResource)) {
        Map<String, Object> updateRequestProperties = new HashMap<String, Object>();

        // add props from query resource
        updateRequestProperties.putAll(PropertyHelper.getProperties(queryResource));

        // add properties from update request
        //todo: should we flag value size > 1?
        if (request.getProperties() != null && request.getProperties().size() != 0) {
          updateRequestProperties.putAll(request.getProperties().iterator().next());
        }
        requests.add(getRequest(updateRequestProperties));
      }
    }

    RequestStageContainer requestStages = modifyResources(new Command<RequestStageContainer>() {
      @Override
      public RequestStageContainer invoke() throws AmbariException {
        return updateHostComponents(stages, requests, request.getRequestInfoProperties(),
            runSmokeTest);
      }
    });
    notifyUpdate(Resource.Type.HostComponent, request, predicate);

    return requestStages;
  }

  /**
   * Determine whether a host component state change is valid.
   * Looks at projected state from the current stages associated with the request.
   *
   *
   * @param stages        request stages
   * @param startState    host component start state
   * @param desiredState  host component desired state
   * @param host          host where state change is occurring
   *
   * @return whether the state transition is valid
   */
  private boolean isValidStateTransition(RequestStageContainer stages, State startState,
                                         State desiredState, ServiceComponentHost host) {

    if (stages != null) {
      State projectedState = stages.getProjectedState(host.getHostName(), host.getServiceComponentName());
      startState = projectedState == null ? startState : projectedState;
    }

    return State.isValidStateTransition(startState, desiredState);
  }

  /**
   * Checks if assigning new state does not require performing
   * any additional actions
   */
  public boolean isDirectTransition(State oldState, State newState) {
    switch (newState) {
      case INSTALLED:
        if (oldState == State.DISABLED) {
          return true;
        }
        break;
      case DISABLED:
        if (oldState == State.INSTALLED ||
            oldState == State.INSTALL_FAILED ||
            oldState == State.UNKNOWN) {
          return true;
        }
        break;
      default:
        break;
    }
    return false;
  }

  private ServiceComponent getServiceComponent(String clusterName, String serviceName, String componentName)
      throws AmbariException {

    Clusters clusters = getManagementController().getClusters();
    return clusters.getCluster(clusterName).getService(serviceName).getServiceComponent(componentName);
  }

  // Perform direct transitions (without task generation)
  private void doDirectTransitions(Map<ServiceComponentHost, State> directTransitionScHosts) throws AmbariException {
    for (Map.Entry<ServiceComponentHost, State> entry : directTransitionScHosts.entrySet()) {
      ServiceComponentHost componentHost = entry.getKey();
      State newState = entry.getValue();
      long timestamp = System.currentTimeMillis();
      ServiceComponentHostEvent event;
      componentHost.setDesiredState(newState);
      switch (newState) {
        case DISABLED:
          event = new ServiceComponentHostDisableEvent(
              componentHost.getServiceComponentName(),
              componentHost.getHostName(),
              timestamp);
          break;
        case INSTALLED:
          event = new ServiceComponentHostRestoreEvent(
              componentHost.getServiceComponentName(),
              componentHost.getHostName(),
              timestamp);
          break;
        default:
          throw new AmbariException("Direct transition from " + componentHost.getState() + " to " + newState + " not supported");
      }
      try {
        componentHost.handleEvent(event);
      } catch (InvalidStateTransitionException e) {
        //Should not occur, must be covered by previous checks
        throw new AmbariException("Internal error - not supported transition", e);
      }
    }
  }

  /**
   * Logs request info.
   *
   * @param msg      base log msg
   * @param request  the request to log
   */
  private void logRequestInfo(String msg, ServiceComponentHostRequest request) {
    LOG.info("{}, clusterName={}, serviceName={}, componentName={}, hostname={}, request={}",
        msg,
        request.getClusterName(),
        request.getServiceName(),
        request.getComponentName(),
        request.getHostname(),
        request);
  }

  /**
   * Logs component info.
   *
   * @param msg              base log msg
   * @param request          the request to log
   * @param oldState         current state
   * @param newDesiredState  new desired state
   */
  private void logComponentInfo(String msg, ServiceComponentHostRequest request, State oldState, State newDesiredState) {
    LOG.debug("{}, clusterName={}, serviceName={}, componentName={}, hostname={}, currentState={}, newDesiredState={}",
        msg,
        request.getClusterName(),
        request.getServiceName(),
        request.getComponentName(),
        request.getHostname(),
        oldState == null ? "null" : oldState,
        newDesiredState == null ? "null" : newDesiredState);
  }

  /**
   * Get the "operation level" from the request.
   *
   * @param requestProperties  request properties
   * @return  the "operation level"
   */
  private Resource.Type determineOperationLevel(Map<String, String> requestProperties) {
    // Determine operation level
    Resource.Type reqOpLvl;
    if (requestProperties.containsKey(RequestOperationLevel.OPERATION_LEVEL_ID)) {
      reqOpLvl = new RequestOperationLevel(requestProperties).getLevel();
    } else {
      String message = "Can not determine request operation level. " +
          "Operation level property should " +
          "be specified for this request.";
      LOG.warn(message);
      reqOpLvl = Resource.Type.Cluster;
    }
    return reqOpLvl;
  }

  /**
   * Validate a host component request.
   *
   * @param request  request to validate
   * @throws IllegalArgumentException if the request is invalid
   */
  private void validateServiceComponentHostRequest(ServiceComponentHostRequest request) {
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()
        || request.getComponentName() == null
        || request.getComponentName().isEmpty()
        || request.getHostname() == null
        || request.getHostname().isEmpty()) {
      throw new IllegalArgumentException("Invalid arguments"
          + ", cluster name, component name and host name should be"
          + " provided");
    }

    if (request.getAdminState() != null) {
      throw new IllegalArgumentException("Property adminState cannot be modified through update. Use service " +
          "specific DECOMMISSION action to decommision/recommission components.");
    }
  }


  // ----- inner classes ---------------------------------------------------

  /**
   * Predicate that identifies client components.
   */
  private  class ClientComponentPredicate implements Predicate {
    @Override
    public boolean evaluate(Resource resource) {
      boolean isClient = false;

      String componentName = (String) resource.getPropertyValue(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID);
      try {
        if (componentName != null && !componentName.isEmpty()) {
          AmbariManagementController managementController = getManagementController();
          String clusterName = (String) resource.getPropertyValue(HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID);
          String serviceName = (String) resource.getPropertyValue(HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID);
          if (StringUtils.isEmpty(serviceName)) {
            Cluster cluster = managementController.getClusters().getCluster(clusterName);
            serviceName = managementController.findServiceName(cluster, componentName);
          }

          ServiceComponent sc = getServiceComponent((String) resource.getPropertyValue(
              HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID), serviceName, componentName);
          isClient = sc.isClientComponent();
        }
      } catch (AmbariException e) {
        // this is really a system exception since cluster/service should have been already verified
        throw new RuntimeException(
            "An unexpected exception occurred while trying to determine if a component is a client", e);
      }
      return isClient;
    }
  }
}
