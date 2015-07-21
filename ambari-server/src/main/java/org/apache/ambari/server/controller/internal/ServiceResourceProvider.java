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

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
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
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.serveraction.kerberos.KerberosAdminAuthenticationException;
import org.apache.ambari.server.serveraction.kerberos.KerberosInvalidConfigurationException;
import org.apache.ambari.server.serveraction.kerberos.KerberosMissingAdminCredentialsException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.commons.lang.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resource provider for service resources.
 */
public class ServiceResourceProvider extends AbstractControllerResourceProvider {


  // ----- Property ID constants ---------------------------------------------

  // Services
  public static final String SERVICE_CLUSTER_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("ServiceInfo", "cluster_name");
  public static final String SERVICE_SERVICE_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("ServiceInfo", "service_name");
  public static final String SERVICE_SERVICE_STATE_PROPERTY_ID   = PropertyHelper.getPropertyId("ServiceInfo", "state");
  public static final String SERVICE_MAINTENANCE_STATE_PROPERTY_ID = PropertyHelper.getPropertyId("ServiceInfo", "maintenance_state");

  public static final String SERVICE_ATTRIBUTES_PROPERTY_ID = PropertyHelper.getPropertyId("Services", "attributes");

  //Parameters from the predicate
  private static final String QUERY_PARAMETERS_RUN_SMOKE_TEST_ID =
    "params/run_smoke_test";

  private static final String QUERY_PARAMETERS_RECONFIGURE_CLIENT =
    "params/reconfigure_client";

  private static final String QUERY_PARAMETERS_START_DEPENDENCIES =
    "params/start_dependencies";

  private static Set<String> pkPropertyIds =
      new HashSet<String>(Arrays.asList(new String[]{
          SERVICE_CLUSTER_NAME_PROPERTY_ID,
          SERVICE_SERVICE_NAME_PROPERTY_ID}));

  // Service state calculation
  private static final Map<String, ServiceState> serviceStateMap = new HashMap<String, ServiceState>();
  static {
    serviceStateMap.put("HDFS", new HDFSServiceState());
    serviceStateMap.put("HBASE", new HBaseServiceState());
    serviceStateMap.put("FLUME", new FlumeServiceState());
    serviceStateMap.put("HIVE", new HiveServiceState());
    serviceStateMap.put("OOZIE", new OozieServiceState());
  }

  private static final ServiceState DEFAULT_SERVICE_STATE = new DefaultServiceState();

  private MaintenanceStateHelper maintenanceStateHelper;

  /**
   * kerberos helper
   */
  @Inject
  private KerberosHelper kerberosHelper;

  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds           the property ids
   * @param keyPropertyIds        the key property ids
   * @param managementController  the management controller
   */
  @AssistedInject
  public ServiceResourceProvider(@Assisted Set<String> propertyIds,
                          @Assisted Map<Resource.Type, String> keyPropertyIds,
                          @Assisted AmbariManagementController managementController,
                          MaintenanceStateHelper maintenanceStateHelper) {
    super(propertyIds, keyPropertyIds, managementController);
    this.maintenanceStateHelper = maintenanceStateHelper;
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
      setResourceProperty(resource, SERVICE_MAINTENANCE_STATE_PROPERTY_ID,
          response.getMaintenanceState(), requestedIds);

      Map<String, Object> serviceSpecificProperties = getServiceSpecificProperties(
          response.getClusterName(), response.getServiceName(), requestedIds);

      for (Map.Entry<String, Object> entry : serviceSpecificProperties.entrySet()) {
        setResourceProperty(resource, entry.getKey(), entry.getValue(), requestedIds);
      }

      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(final Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    RequestStageContainer requestStages = doUpdateResources(null, request, predicate);

    RequestStatusResponse response = null;
    if (requestStages != null) {
      try {
        requestStages.persist();
      } catch (AmbariException e) {
        throw new SystemException(e.getMessage(), e);
      }
      response = requestStages.getRequestStatusResponse();
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

  private RequestStageContainer doUpdateResources(final RequestStageContainer stages, final Request request, Predicate predicate)
      throws UnsupportedPropertyException, SystemException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    RequestStageContainer requestStages = null;

    Iterator<Map<String,Object>> iterator = request.getProperties().iterator();
    if (iterator.hasNext()) {
      for (Map<String, Object> propertyMap : getPropertyMaps(iterator.next(), predicate)) {
        requests.add(getRequest(propertyMap));
      }

      final boolean runSmokeTest = "true".equals(getQueryParameterValue(
          QUERY_PARAMETERS_RUN_SMOKE_TEST_ID, predicate));

      final boolean reconfigureClients = !"false".equals(getQueryParameterValue(
          QUERY_PARAMETERS_RECONFIGURE_CLIENT, predicate));

      final boolean startDependencies = "true".equals(getQueryParameterValue(
          QUERY_PARAMETERS_START_DEPENDENCIES, predicate));

      requestStages = modifyResources(new Command<RequestStageContainer>() {
        @Override
        public RequestStageContainer invoke() throws AmbariException {
          return updateServices(stages, requests, request.getRequestInfoProperties(),
              runSmokeTest, reconfigureClients, startDependencies);
        }
      });
    }
    return requestStages;
  }

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

    Object o = properties.get(SERVICE_MAINTENANCE_STATE_PROPERTY_ID);
    if (null != o) {
      svcRequest.setMaintenanceState(o.toString());
    }

    return svcRequest;
  }

  // Create services from the given request.
  public synchronized void createServices(Set<ServiceRequest> requests)
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
      // Initialize service widgets
      getManagementController().initializeWidgetsAndLayouts(cluster, s);

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
  private Set<ServiceResponse> getServices(ServiceRequest request)
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
  protected synchronized RequestStageContainer updateServices(RequestStageContainer requestStages, Set<ServiceRequest> requests,
                                                      Map<String, String> requestProperties, boolean runSmokeTest,
                                                      boolean reconfigureClients, boolean startDependencies) throws AmbariException {

    AmbariManagementController controller = getManagementController();

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return null;
    }

    Map<State, List<Service>> changedServices
        = new EnumMap<State, List<Service>>(State.class);
    Map<State, List<ServiceComponent>> changedComps =
        new EnumMap<State, List<ServiceComponent>>(State.class);
    Map<String, Map<State, List<ServiceComponentHost>>> changedScHosts =
        new HashMap<String, Map<State, List<ServiceComponentHost>>>();
    Collection<ServiceComponentHost> ignoredScHosts =
        new ArrayList<ServiceComponentHost>();

    Set<String> clusterNames = new HashSet<String>();
    Map<String, Set<String>> serviceNames = new HashMap<String, Set<String>>();
    Set<State> seenNewStates = new HashSet<State>();

    // Determine operation level
    Resource.Type reqOpLvl;
    if (requestProperties.containsKey(RequestOperationLevel.OPERATION_LEVEL_ID)) {
      RequestOperationLevel operationLevel = new RequestOperationLevel(requestProperties);
      reqOpLvl = operationLevel.getLevel();
    } else {
      String message = "Can not determine request operation level. " +
              "Operation level property should " +
              "be specified for this request.";
      LOG.warn(message);
      reqOpLvl = Resource.Type.Cluster;
    }

    Clusters       clusters        = controller.getClusters();

    // We don't expect batch requests for different clusters, that's why
    // nothing bad should happen if value is overwritten few times
    String maintenanceCluster = null;

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

      // Setting Maintenance state for service
      if (null != request.getMaintenanceState()) {
        MaintenanceState newMaint = MaintenanceState.valueOf(request.getMaintenanceState());
        if (newMaint  != s.getMaintenanceState()) {
          if (newMaint.equals(MaintenanceState.IMPLIED_FROM_HOST)
              || newMaint.equals(MaintenanceState.IMPLIED_FROM_SERVICE)) {
            throw new IllegalArgumentException("Invalid arguments, can only set " +
              "maintenance state to one of " + EnumSet.of(MaintenanceState.OFF, MaintenanceState.ON));
          } else {
            s.setMaintenanceState(newMaint);
            maintenanceCluster = cluster.getClusterName();
          }
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

      if (! maintenanceStateHelper.isOperationAllowed(reqOpLvl, s)) {
        LOG.info("Operations cannot be applied to service " + s.getName() +
            " in the maintenance state of " + s.getMaintenanceState());
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

      updateServiceComponents(requestStages, changedComps, changedScHosts,
        ignoredScHosts, reqOpLvl, s, newState);
    }

    if (startDependencies && changedServices.containsKey(State.STARTED)) {
      HashSet<Service> depServices = new HashSet<Service>();
      for (Service service : changedServices.get(State.STARTED)) {
        RoleCommandOrder rco = controller.getRoleCommandOrder(service.getCluster());
        Set<Service> dependencies = rco.getTransitiveServices(service, RoleCommand.START);
        for (Service dependency: dependencies) {
          if (!changedServices.get(State.STARTED).contains(dependency)){
            depServices.add(dependency);
          }
        }
      }
      for (Service service : depServices) {
        updateServiceComponents(requestStages, changedComps, changedScHosts,
          ignoredScHosts, reqOpLvl, service, State.STARTED);
        changedServices.get(State.STARTED).add(service);
      }

    }

    if (seenNewStates.size() > 1) {
      // TODO should we handle this scenario
      throw new IllegalArgumentException("Cannot handle different desired state"
          + " changes for a set of services at the same time");
    }

    Cluster cluster = clusters.getCluster(clusterNames.iterator().next());

    return controller.addStages(requestStages, cluster, requestProperties,
      null, changedServices, changedComps, changedScHosts,
        ignoredScHosts, runSmokeTest, reconfigureClients);
  }

  private void updateServiceComponents(RequestStageContainer requestStages,
                                       Map<State, List<ServiceComponent>> changedComps,
                                       Map<String, Map<State, List<ServiceComponentHost>>> changedScHosts,
                                       Collection<ServiceComponentHost> ignoredScHosts,
                                       Resource.Type reqOpLvl,
                                       Service service, State newState)
    throws AmbariException {

    Cluster cluster = service.getCluster();
    AmbariManagementController controller = getManagementController();
    AmbariMetaInfo ambariMetaInfo = controller.getAmbariMetaInfo();

    for (ServiceComponent sc : service.getServiceComponents().values()) {
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
            + ", clusterName=" + cluster.getClusterName()
            + ", serviceName=" + service.getName()
            + ", componentName=" + sc.getName()
            + ", currentDesiredState=" + oldScState
            + ", newDesiredState=" + newState);
      }

      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        State oldSchState = sch.getState();
        if (oldSchState == State.DISABLED || oldSchState == State.UNKNOWN) {
          //Ignore host components updates in this state
          if (LOG.isDebugEnabled()) {
            LOG.debug("Ignoring ServiceComponentHost"
                + ", clusterName=" + cluster.getClusterName()
                + ", serviceName=" + service.getName()
                + ", componentName=" + sc.getName()
                + ", hostname=" + sch.getHostName()
                + ", currentState=" + oldSchState
                + ", newDesiredState=" + newState);
          }
          continue;
        }
                                         //
        if (newState == oldSchState) {
          ignoredScHosts.add(sch);
          if (LOG.isDebugEnabled()) {
            LOG.debug("Ignoring ServiceComponentHost"
                + ", clusterName=" + cluster.getClusterName()
                + ", serviceName=" + service.getName()
                + ", componentName=" + sc.getName()
                + ", hostname=" + sch.getHostName()
                + ", currentState=" + oldSchState
                + ", newDesiredState=" + newState);
          }
          continue;
        }

        if (! maintenanceStateHelper.isOperationAllowed(reqOpLvl, sch)) {
          ignoredScHosts.add(sch);
          if (LOG.isDebugEnabled()) {
            LOG.debug("Ignoring ServiceComponentHost"
                + ", clusterName=" + cluster.getClusterName()
                + ", serviceName=" + service.getName()
                + ", componentName=" + sc.getName()
                + ", hostname=" + sch.getHostName());
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
        if (! isValidStateTransition(requestStages, oldSchState, newState, sch)) {
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

          if ( ambariMetaInfo.getComponent(
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
              new EnumMap<State, List<ServiceComponentHost>>(State.class));
        }
        if (!changedScHosts.get(sc.getName()).containsKey(newState)) {
          changedScHosts.get(sc.getName()).put(newState,
              new ArrayList<ServiceComponentHost>());
        }
        if (LOG.isDebugEnabled()) {
          LOG.debug("Handling update to ServiceComponentHost"
              + ", clusterName=" + cluster.getClusterName()
              + ", serviceName=" + service.getName()
              + ", componentName=" + sc.getName()
              + ", hostname=" + sch.getHostName()
              + ", currentState=" + oldSchState
              + ", newDesiredState=" + newState);
        }
        changedScHosts.get(sc.getName()).get(newState).add(sch);
      }
    }
  }

  // Delete services based on the given set of requests
  protected RequestStatusResponse deleteServices(Set<ServiceRequest> request)
      throws AmbariException {

    Clusters clusters    = getManagementController().getClusters();

    Set<Service> removable = new HashSet<Service>();

    for (ServiceRequest serviceRequest : request) {
      if (StringUtils.isEmpty(serviceRequest.getClusterName()) || StringUtils.isEmpty(serviceRequest.getServiceName())) {
        // FIXME throw correct error
        throw new AmbariException("invalid arguments");
      } else {

        Service service = clusters.getCluster(
            serviceRequest.getClusterName()).getService(
                serviceRequest.getServiceName());

        if (!service.getDesiredState().isRemovableState()) {
          throw new AmbariException("Cannot remove " + service.getName() + ". Desired state " +
              service.getDesiredState() + " is not removable.  Service must be stopped or disabled.");
        } else {
          for (ServiceComponent sc : service.getServiceComponents().values()) {
            if (!sc.canBeRemoved()) {
              throw new AmbariException ("Cannot remove " +
                  serviceRequest.getClusterName() + "/" + serviceRequest.getServiceName() +
                  ". " + sc.getName() + " is in a non-removable state.");
            }
          }
        }

        removable.add(service);
      }
    }

    for (Service service : removable) {
      service.getCluster().deleteService(service.getName());
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
   * The following rules should apply :
   * For services that have all components DISABLED, the service state should be DISABLED.
   * For services that have any master components, the service state should
   * be STARTED if all master components are STARTED.
   * For services that have all client components, the service state should
   * be INSTALLED if all of the components are INSTALLED.
   * For all other cases the state of the service should match the highest state of all
   * of its component states or UNKNOWN if the component states can not be determined.
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

            State   masterState = null;
            State   clientState = null;
            State   otherState = null;
            State   maxMMState = null; // The worst state among components in MM

            boolean hasDisabled  = false;
            boolean hasMaster    = false;
            boolean hasOther     = false;
            boolean hasClient    = false;
            boolean hasMM        = false;

            for (ServiceComponentHostResponse hostComponentResponse : hostComponentResponses ) {
              try {
                ComponentInfo componentInfo = ambariMetaInfo.getComponent(stackId.getStackName(),
                    stackId.getStackVersion(), hostComponentResponse.getServiceName(),
                    hostComponentResponse.getComponentName());

                State state = getHostComponentState(hostComponentResponse);
                // Components in MM should not affect service status,
                // so we tend to ignore them
                boolean isInMaintenance = ! MaintenanceState.OFF.toString().
                    equals(hostComponentResponse.getMaintenanceState());

                if (state.equals(State.DISABLED)) {
                  hasDisabled = true;
                }

                if (isInMaintenance & !componentInfo.isClient()) {
                  hasMM = true;
                  if ( maxMMState == null || state.ordinal() > maxMMState.ordinal()) {
                    maxMMState = state;
                  }
                }

                if (componentInfo.isMaster()) {
                  if (state.equals(State.STARTED) || ! isInMaintenance) {
                    // We rely on master's state to determine service state
                    hasMaster = true;
                  }

                  if (! state.equals(State.STARTED) &&
                      ! isInMaintenance &&  // Ignore status of MM component
                      ( masterState == null || state.ordinal() > masterState.ordinal())) {
                    masterState = state;
                  }
                } else if (componentInfo.isClient()) {
                  hasClient = true;
                  if (!state.equals(State.INSTALLED) &&
                      (clientState == null || state.ordinal() > clientState.ordinal())) {
                    clientState = state;
                  }
                } else {
                  if (state.equals(State.STARTED) || ! isInMaintenance) {
                    // We rely on slaves's state to determine service state
                    hasOther = true;
                  }
                  if (! state.equals(State.STARTED) &&
                      ! isInMaintenance && // Ignore status of MM component
                      ( otherState == null || state.ordinal() > otherState.ordinal())) {
                    otherState = state;
                  }
                }
              } catch (ObjectNotFoundException e) {
                // component doesn't exist, nothing to do
              }
            }

            return hasMaster   ? masterState == null ? State.STARTED : masterState :
                   hasOther    ? otherState == null ? State.STARTED : otherState :
                   hasClient   ? clientState == null ? State.INSTALLED : clientState :
                   hasDisabled ? State.DISABLED :
                   hasMM       ? maxMMState : State.UNKNOWN;
          }
        } catch (AmbariException e) {
          LOG.error("Can't determine service state.", e);
        }
      }
      return State.UNKNOWN;
    }
  }

  /**
   * Calculator of Oozie service state.
   */
  protected static class OozieServiceState implements ServiceState {

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

            int     oozieServerActiveCount = 0;
            State   nonStartedState        = null;

            for (ServiceComponentHostResponse hostComponentResponse : hostComponentResponses ) {
              try {
                ComponentInfo componentInfo = ambariMetaInfo.getComponent(stackId.getStackName(),
                        stackId.getStackVersion(), hostComponentResponse.getServiceName(),
                        hostComponentResponse.getComponentName());

                if (componentInfo.isMaster()) {
                  State state = getHostComponentState(hostComponentResponse);

                  switch (state) {
                    case STARTED:
                    case DISABLED:
                      String componentName = hostComponentResponse.getComponentName();
                      if (componentName.equals("OOZIE_SERVER")) {
                        ++oozieServerActiveCount;
                      }
                      break;
                    default:
                      nonStartedState = state;
                  }
                }
              } catch (ObjectNotFoundException e) {
                // component doesn't exist, nothing to do
              }
            }

            // should have state INSTALLED when there is no active OOZIE_SERVER
            if (oozieServerActiveCount > 0) {
              return State.STARTED;
            }
            return nonStartedState == null ? State.INSTALLED : nonStartedState;
          }
        } catch (AmbariException e) {
          LOG.error("Can't determine service state.", e);
        }
      }
      return State.UNKNOWN;
    }
  }

  /**
   * Calculator of HIVE service state.
   */
  protected static class HiveServiceState implements ServiceState {

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

            int activeHiveMetastoreComponentCount = 0;
            State nonStartedState = null;
            boolean embeddedMysqlComponentExists = false;
            boolean hiveServerComponentStarted = false;
            boolean webHcatComponentStarted = false;
            boolean mysqlComponentStarted = false;

            for (ServiceComponentHostResponse hostComponentResponse : hostComponentResponses ) {
              try {
                ComponentInfo componentInfo = ambariMetaInfo.getComponent(stackId.getStackName(),
                        stackId.getStackVersion(), hostComponentResponse.getServiceName(),
                        hostComponentResponse.getComponentName());

                if (componentInfo.isMaster()) {
                  State state = getHostComponentState(hostComponentResponse);

                  String componentName = hostComponentResponse.getComponentName();
                  if (componentName.equals("MYSQL_SERVER")) {
                    embeddedMysqlComponentExists = true;
                  }

                  switch (state) {
                    case STARTED:
                    case DISABLED:
                      if (componentName.equals("HIVE_METASTORE")) {
                        ++activeHiveMetastoreComponentCount;
                      } else if (componentName.equals("HIVE_SERVER")) {
                        hiveServerComponentStarted = true;
                      } else if (componentName.equals("MYSQL_SERVER")) {
                        mysqlComponentStarted = true;
                      } else if (componentName.equals("WEBHCAT_SERVER")) {
                        webHcatComponentStarted = true;
                      }
                      break;
                    default:
                      nonStartedState = state;
                  }
                }
              } catch (ObjectNotFoundException e) {
                // component doesn't exist, nothing to do
              }
            }

            if (nonStartedState == null ||
                (hiveServerComponentStarted && webHcatComponentStarted && activeHiveMetastoreComponentCount > 0 &&
                 (embeddedMysqlComponentExists ? mysqlComponentStarted : true))) {
              return State.STARTED;
            }
            return nonStartedState == null ? State.INSTALLED : nonStartedState;
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
              try {
                ComponentInfo componentInfo = ambariMetaInfo.getComponent(stackId.getStackName(),
                    stackId.getStackVersion(), hostComponentResponse.getServiceName(),
                    hostComponentResponse.getComponentName());

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
                    case DISABLED:
                      if (isNameNode) {
                        ++nameNodeActiveCount;
                      }
                      break;
                    default:
                      nonStartedState = state;
                  }
                }
              } catch (ObjectNotFoundException e) {
                // component doesn't exist, nothing to do
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
              try {
                ComponentInfo componentInfo = ambariMetaInfo.getComponent(stackId.getStackName(),
                    stackId.getStackVersion(), hostComponentResponse.getServiceName(),
                    hostComponentResponse.getComponentName());

                if (componentInfo.isMaster()) {
                  State state = getHostComponentState(hostComponentResponse);

                  switch (state) {
                    case STARTED:
                    case DISABLED:
                      String componentName = hostComponentResponse.getComponentName();
                      if (componentName.equals("HBASE_MASTER")) {
                        ++hBaseMasterActiveCount;
                      }
                      break;
                    default:
                      nonStartedState = state;
                  }
                }
              } catch (ObjectNotFoundException e) {
                // component doesn't exist, nothing to do
              }
            }

            // should have state INSTALLED when there is no active HBASE_MASTER
            if (hBaseMasterActiveCount > 0) {
              return State.STARTED;
            }
            return nonStartedState == null ? State.INSTALLED : nonStartedState;
          }
        } catch (AmbariException e) {
          LOG.error("Can't determine service state.", e);
        }
      }
      return State.UNKNOWN;
    }
  }

  /**
   * Determines the service status for Flume.  Generically, this means that
   * the state of Flume is the lowest ordinal state calculated.  For example:
   * <ul>
   *   <li>If all handlers are STARTED, service is STARTED.</li>
   *   <li>If one handler is INSTALLED, the service is INSTALLED.</li>
   * </ul>
   */
  protected static class FlumeServiceState implements ServiceState {
    @Override
    public State getState(AmbariManagementController controller,
        String clusterName, String serviceName) {
      Clusters       clusters       = controller.getClusters();

      if (clusterName != null && clusterName.length() > 0) {
        try {
          Cluster cluster = clusters.getCluster(clusterName);
          if (cluster != null) {

            ServiceComponentHostRequest request = new ServiceComponentHostRequest(clusterName,
                serviceName, null, null, null);

            Set<ServiceComponentHostResponse> hostComponentResponses =
                controller.getHostComponents(Collections.singleton(request));

            State state = State.UNKNOWN;
            for (ServiceComponentHostResponse schr : hostComponentResponses) {
              State schState = getHostComponentState(schr);
              if (schState.ordinal() < state.ordinal()) {
                state = schState;
              }
            }
            return state;
          }
        } catch (AmbariException e) {
          LOG.error("Can't determine service state.", e);
        }
      }

      return State.UNKNOWN;
    }
  }

  /**
   * Determine whether a service state change is valid.
   * Looks at projected state from the current stages associated with the request.
   *
   *
   * @param stages        request stages
   * @param startState    service start state
   * @param desiredState  service desired state
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
   * Get any service specific properties for the request.
   *
   * @param clusterName  cluster name
   * @param serviceName  service name
   * @param requestedIds relevant request property ids
   */
  private Map<String, Object> getServiceSpecificProperties(String clusterName, String serviceName, Set<String> requestedIds) {
    Map<String, Object> serviceSpecificProperties = new HashMap<String, Object>();
    if (serviceName.equals("KERBEROS")) {
      // Only include details on whether the KDC administrator credentials are set and correct if
      // implicitly (Service/attributes) or explicitly (Service/attributes/kdc_...) queried
      if (requestedIds.contains(SERVICE_ATTRIBUTES_PROPERTY_ID) ||
          isPropertyCategoryRequested(SERVICE_ATTRIBUTES_PROPERTY_ID, requestedIds) ||
          isPropertyEntryRequested(SERVICE_ATTRIBUTES_PROPERTY_ID, requestedIds)) {
        Map<String, String> kerberosAttributes = new HashMap<String, String>();
        String kdcValidationResult = "OK";
        String failureDetails = "";
        try {
          kerberosHelper.validateKDCCredentials(
              getManagementController().getClusters().getCluster(clusterName));

        } catch (KerberosInvalidConfigurationException e) {
          kdcValidationResult = "INVALID_CONFIGURATION";
          failureDetails = e.getMessage();
        } catch (KerberosAdminAuthenticationException e) {
          kdcValidationResult = "INVALID_CREDENTIALS";
          failureDetails = e.getMessage();
        } catch (KerberosMissingAdminCredentialsException e) {
          kdcValidationResult = "MISSING_CREDENTIALS";
          failureDetails = e.getMessage();
        } catch (AmbariException e) {
          kdcValidationResult = "VALIDATION_ERROR";
          failureDetails = e.getMessage();
        }

        kerberosAttributes.put("kdc_validation_result", kdcValidationResult);
        kerberosAttributes.put("kdc_validation_failure_details", failureDetails);
        serviceSpecificProperties.put(SERVICE_ATTRIBUTES_PROPERTY_ID, kerberosAttributes);
      }
    }

    return serviceSpecificProperties;
  }
}
