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
import java.util.HashMap;
import java.util.HashSet;
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
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceComponentRequest;
import org.apache.ambari.server.controller.ServiceComponentResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;

/**
 * Resource provider for component resources.
 */
public class ComponentResourceProvider extends AbstractControllerResourceProvider {


  // ----- Property ID constants ---------------------------------------------

  // Components
  protected static final String COMPONENT_CLUSTER_NAME_PROPERTY_ID    = "ServiceComponentInfo/cluster_name";
  protected static final String COMPONENT_SERVICE_NAME_PROPERTY_ID    = "ServiceComponentInfo/service_name";
  protected static final String COMPONENT_COMPONENT_NAME_PROPERTY_ID  = "ServiceComponentInfo/component_name";
  protected static final String COMPONENT_DISPLAY_NAME_PROPERTY_ID    = "ServiceComponentInfo/display_name";
  protected static final String COMPONENT_STATE_PROPERTY_ID           = "ServiceComponentInfo/state";
  protected static final String COMPONENT_CATEGORY_PROPERTY_ID        = "ServiceComponentInfo/category";
  protected static final String COMPONENT_TOTAL_COUNT_PROPERTY_ID     = "ServiceComponentInfo/total_count";
  protected static final String COMPONENT_STARTED_COUNT_PROPERTY_ID   = "ServiceComponentInfo/started_count";
  protected static final String COMPONENT_INSTALLED_COUNT_PROPERTY_ID = "ServiceComponentInfo/installed_count";

  //Parameters from the predicate
  private static final String QUERY_PARAMETERS_RUN_SMOKE_TEST_ID =
      "params/run_smoke_test";

  private static Set<String> pkPropertyIds =
      new HashSet<String>(Arrays.asList(new String[]{
          COMPONENT_CLUSTER_NAME_PROPERTY_ID,
          COMPONENT_SERVICE_NAME_PROPERTY_ID,
          COMPONENT_COMPONENT_NAME_PROPERTY_ID}));

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
  ComponentResourceProvider(@Assisted Set<String> propertyIds,
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

    final Set<ServiceComponentRequest> requests = new HashSet<ServiceComponentRequest>();
    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }

    createResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        createComponents(requests);
        return null;
      }
    });

    notifyCreate(Resource.Type.Component, request);

    return getRequestStatus(null);
  }

  @Override
  @Transactional
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceComponentRequest> requests = new HashSet<ServiceComponentRequest>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }

    Set<ServiceComponentResponse> responses = getResources(new Command<Set<ServiceComponentResponse>>() {
      @Override
      public Set<ServiceComponentResponse> invoke() throws AmbariException {
        return getComponents(requests);
      }
    });

    Set<String>   requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources    = new HashSet<Resource>();

    for (ServiceComponentResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.Component);
      setResourceProperty(resource, COMPONENT_CLUSTER_NAME_PROPERTY_ID, response.getClusterName(), requestedIds);
      setResourceProperty(resource, COMPONENT_SERVICE_NAME_PROPERTY_ID, response.getServiceName(), requestedIds);
      setResourceProperty(resource, COMPONENT_COMPONENT_NAME_PROPERTY_ID, response.getComponentName(), requestedIds);
      setResourceProperty(resource, COMPONENT_DISPLAY_NAME_PROPERTY_ID, response.getDisplayName(), requestedIds);
      setResourceProperty(resource, COMPONENT_STATE_PROPERTY_ID, response.getDesiredState(), requestedIds);
      setResourceProperty(resource, COMPONENT_CATEGORY_PROPERTY_ID, response.getCategory(), requestedIds);
      setResourceProperty(resource, COMPONENT_TOTAL_COUNT_PROPERTY_ID, response.getTotalCount(), requestedIds);
      setResourceProperty(resource, COMPONENT_STARTED_COUNT_PROPERTY_ID, response.getStartedCount(), requestedIds);
      setResourceProperty(resource, COMPONENT_INSTALLED_COUNT_PROPERTY_ID, response.getInstalledCount(), requestedIds);

      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(final Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceComponentRequest> requests = new HashSet<ServiceComponentRequest>();
    for (Map<String, Object> propertyMap : getPropertyMaps(request.getProperties().iterator().next(), predicate)) {
      ServiceComponentRequest compRequest = getRequest(propertyMap);

      requests.add(compRequest);
    }

    Object queryParameterValue = getQueryParameterValue(QUERY_PARAMETERS_RUN_SMOKE_TEST_ID, predicate);
    final boolean runSmokeTest = queryParameterValue != null && queryParameterValue.equals("true");

    RequestStatusResponse response = modifyResources(new Command<RequestStatusResponse>() {
      @Override
      public RequestStatusResponse invoke() throws AmbariException {
        return updateComponents(requests, request.getRequestInfoProperties(), runSmokeTest);
      }
    });

    notifyUpdate(Resource.Type.Component, request, predicate);

    return getRequestStatus(response);
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceComponentRequest> requests = new HashSet<ServiceComponentRequest>();
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    RequestStatusResponse response = modifyResources(new Command<RequestStatusResponse>() {
      @Override
      public RequestStatusResponse invoke() throws AmbariException {
        return deleteComponents(requests);
      }
    });

    notifyDelete(Resource.Type.Component, predicate);
    return getRequestStatus(response);
  }


  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }


  // ----- utility methods ---------------------------------------------------

  /**
   * Get a component request object from a map of property values.
   *
   * @param properties  the predicate
   *
   * @return the component request object
   */
  private ServiceComponentRequest getRequest(Map<String, Object> properties) {
    return new ServiceComponentRequest(
        (String) properties.get(COMPONENT_CLUSTER_NAME_PROPERTY_ID),
        (String) properties.get(COMPONENT_SERVICE_NAME_PROPERTY_ID),
        (String) properties.get(COMPONENT_COMPONENT_NAME_PROPERTY_ID),
        (String) properties.get(COMPONENT_STATE_PROPERTY_ID),
        (String) properties.get(COMPONENT_CATEGORY_PROPERTY_ID));
  }

  // Create the components for the given requests.
  public synchronized void createComponents(
      Set<ServiceComponentRequest> requests) throws AmbariException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return;
    }

    Clusters clusters = getManagementController().getClusters();
    AmbariMetaInfo ambariMetaInfo = getManagementController().getAmbariMetaInfo();
    ServiceComponentFactory serviceComponentFactory = getManagementController().getServiceComponentFactory();

    // do all validation checks
    Map<String, Map<String, Set<String>>> componentNames =
        new HashMap<String, Map<String, Set<String>>>();
    Set<String> duplicates = new HashSet<String>();

    for (ServiceComponentRequest request : requests) {
      if (request.getClusterName() == null
          || request.getClusterName().isEmpty()
          || request.getComponentName() == null
          || request.getComponentName().isEmpty()) {
        throw new IllegalArgumentException("Invalid arguments"
            + ", clustername and componentname should be"
            + " non-null and non-empty when trying to create a"
            + " component");
      }

      Cluster cluster;
      try {
        cluster = clusters.getCluster(request.getClusterName());
      } catch (ClusterNotFoundException e) {
        throw new ParentObjectNotFoundException(
            "Attempted to add a component to a cluster which doesn't exist:", e);
      }

      if (request.getServiceName() == null
          || request.getServiceName().isEmpty()) {
        StackId stackId = cluster.getDesiredStackVersion();
        String serviceName =
            ambariMetaInfo.getComponentToService(stackId.getStackName(),
                stackId.getStackVersion(), request.getComponentName());
        if (LOG.isDebugEnabled()) {
          LOG.debug("Looking up service name for component"
              + ", componentName=" + request.getComponentName()
              + ", serviceName=" + serviceName);
        }

        if (serviceName == null
            || serviceName.isEmpty()) {
          throw new AmbariException("Could not find service for component"
              + ", componentName=" + request.getComponentName()
              + ", clusterName=" + cluster.getClusterName()
              + ", stackInfo=" + stackId.getStackId());
        }
        request.setServiceName(serviceName);
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a createComponent request"
            + ", clusterName=" + request.getClusterName()
            + ", serviceName=" + request.getServiceName()
            + ", componentName=" + request.getComponentName()
            + ", request=" + request);
      }

      if (!componentNames.containsKey(request.getClusterName())) {
        componentNames.put(request.getClusterName(),
            new HashMap<String, Set<String>>());
      }
      if (!componentNames.get(request.getClusterName())
          .containsKey(request.getServiceName())) {
        componentNames.get(request.getClusterName()).put(
            request.getServiceName(), new HashSet<String>());
      }
      if (componentNames.get(request.getClusterName())
          .get(request.getServiceName()).contains(request.getComponentName())) {
        // throw error later for dup
        duplicates.add("[clusterName=" + request.getClusterName() + ", serviceName=" + request.getServiceName() +
            ", componentName=" + request.getComponentName() + "]");
        continue;
      }
      componentNames.get(request.getClusterName())
          .get(request.getServiceName()).add(request.getComponentName());

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

      Service s;
      try {
        s = cluster.getService(request.getServiceName());
      } catch (ServiceNotFoundException e) {
        throw new ParentObjectNotFoundException(
            "Attempted to add a component to a service which doesn't exist:", e);
      }
      try {
        ServiceComponent sc = s.getServiceComponent(request.getComponentName());
        if (sc != null) {
          // throw error later for dup
          duplicates.add("[clusterName=" + request.getClusterName() + ", serviceName=" + request.getServiceName() +
              ", componentName=" + request.getComponentName() + "]");
          continue;
        }
      } catch (AmbariException e) {
        // Expected
      }

      StackId stackId = s.getDesiredStackVersion();
      if (!ambariMetaInfo.isValidServiceComponent(stackId.getStackName(),
          stackId.getStackVersion(), s.getName(), request.getComponentName())) {
        throw new IllegalArgumentException("Unsupported or invalid component"
            + " in stack"
            + ", clusterName=" + request.getClusterName()
            + ", serviceName=" + request.getServiceName()
            + ", componentName=" + request.getComponentName()
            + ", stackInfo=" + stackId.getStackId());
      }
    }

    // ensure only a single cluster update
    if (componentNames.size() != 1) {
      throw new IllegalArgumentException("Invalid arguments, updates allowed"
          + "on only one cluster at a time");
    }

    // Validate dups
    if (!duplicates.isEmpty()) {
      StringBuilder names = new StringBuilder();
      boolean first = true;
      for (String cName : duplicates) {
        if (!first) {
          names.append(",");
        }
        first = false;
        names.append(cName);
      }
      String msg;
      if (duplicates.size() == 1) {
        msg = "Attempted to create a component which already exists: ";
      } else {
        msg = "Attempted to create components which already exist: ";
      }
      throw new DuplicateResourceException(msg + names.toString());
    }

    // now doing actual work
    for (ServiceComponentRequest request : requests) {
      Cluster cluster = clusters.getCluster(request.getClusterName());
      Service s = cluster.getService(request.getServiceName());
      ServiceComponent sc = serviceComponentFactory.createNew(s,
          request.getComponentName());
      sc.setDesiredStackVersion(s.getDesiredStackVersion());

      if (request.getDesiredState() != null
          && !request.getDesiredState().isEmpty()) {
        State state = State.valueOf(request.getDesiredState());
        sc.setDesiredState(state);
      } else {
        sc.setDesiredState(s.getDesiredState());
      }

      s.addServiceComponent(sc);
      sc.persist();
    }
  }

  // Get the components for the given requests.
  protected Set<ServiceComponentResponse> getComponents(
      Set<ServiceComponentRequest> requests) throws AmbariException {

    Set<ServiceComponentResponse> response = new HashSet<ServiceComponentResponse>();
    for (ServiceComponentRequest request : requests) {
      try {
        response.addAll(getComponents(request));
      } catch (ObjectNotFoundException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          throw e;
        }
      }
    }
    return response;
  }

  // Get the components for the given request.
  private Set<ServiceComponentResponse> getComponents(
      ServiceComponentRequest request) throws AmbariException {
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()) {
      throw new IllegalArgumentException("Invalid arguments, cluster name"
          + " should be non-null");
    }

    Clusters clusters = getManagementController().getClusters();
    AmbariMetaInfo ambariMetaInfo = getManagementController().getAmbariMetaInfo();

    final Cluster cluster;
    try {
      cluster = clusters.getCluster(request.getClusterName());
    } catch (ObjectNotFoundException e) {
      throw new ParentObjectNotFoundException("Parent Cluster resource doesn't exist", e);
    }

    Set<ServiceComponentResponse> response =
        new HashSet<ServiceComponentResponse>();
    String category = null;

    StackId stackId = cluster.getDesiredStackVersion();

    if (request.getComponentName() != null) {
      if (request.getServiceName() == null) {
        String serviceName =
            ambariMetaInfo.getComponentToService(stackId.getStackName(),
                stackId.getStackVersion(), request.getComponentName());
        if (LOG.isDebugEnabled()) {
          LOG.debug("Looking up service name for component"
              + ", componentName=" + request.getComponentName()
              + ", serviceName=" + serviceName);
        }
        if (serviceName == null
            || serviceName.isEmpty()) {
          throw new ObjectNotFoundException("Could not find service for component"
              + ", componentName=" + request.getComponentName()
              + ", clusterName=" + cluster.getClusterName()
              + ", stackInfo=" + stackId.getStackId());
        }
        request.setServiceName(serviceName);
      }

      final Service s;
      try {
        s = cluster.getService(request.getServiceName());
      } catch (ObjectNotFoundException e) {
        throw new ParentObjectNotFoundException("Parent Service resource doesn't exist", e);
      }

      ServiceComponent sc = s.getServiceComponent(request.getComponentName());
      ServiceComponentResponse serviceComponentResponse = sc.convertToResponse();

      try {
        ComponentInfo componentInfo = ambariMetaInfo.getComponent(stackId.getStackName(),
            stackId.getStackVersion(), s.getName(), request.getComponentName());
        category = componentInfo.getCategory();
        if (category != null) {
          serviceComponentResponse.setCategory(category);
        }
      } catch (ObjectNotFoundException e) {
        // nothing to do, component doesn't exist
      }

      response.add(serviceComponentResponse);
      return response;
    }

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

    Set<Service> services = new HashSet<Service>();
    if (request.getServiceName() != null
        && !request.getServiceName().isEmpty()) {
      try {
        services.add(cluster.getService(request.getServiceName()));
      } catch (ObjectNotFoundException e) {
        throw new ParentObjectNotFoundException("Could not find service", e);
      }
    } else {
      services.addAll(cluster.getServices().values());
    }

    for (Service s : services) {
      // filter on request.getDesiredState()
      for (ServiceComponent sc : s.getServiceComponents().values()) {
        if (checkDesiredState
            && (desiredStateToCheck != sc.getDesiredState())) {
          // skip non matching state
          continue;
        }

        ServiceComponentResponse serviceComponentResponse = sc.convertToResponse();
        try {
          ComponentInfo componentInfo = ambariMetaInfo.getComponent(stackId.getStackName(),
              stackId.getStackVersion(), s.getName(), sc.getName());
          category = componentInfo.getCategory();
          if (category != null) {
            serviceComponentResponse.setCategory(category);
          }
        } catch (ObjectNotFoundException e) {
          // component doesn't exist, nothing to do
        }
        String requestedCategory = request.getComponentCategory();
        if (requestedCategory != null && !requestedCategory.isEmpty() &&
            category != null && !requestedCategory.equalsIgnoreCase(category)) {
          continue;
        }

        response.add(serviceComponentResponse);
      }
    }
    return response;
  }

  // Update the components for the given requests.
  protected synchronized RequestStatusResponse updateComponents(Set<ServiceComponentRequest> requests,
                                                             Map<String, String> requestProperties, boolean runSmokeTest)
      throws AmbariException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return null;
    }

    AmbariManagementController controller = getManagementController();
    Clusters clusters = controller.getClusters();
    AmbariMetaInfo ambariMetaInfo = controller.getAmbariMetaInfo();

    Map<State, List<ServiceComponent>> changedComps =
        new HashMap<State, List<ServiceComponent>>();
    Map<String, Map<State, List<ServiceComponentHost>>> changedScHosts =
        new HashMap<String, Map<State, List<ServiceComponentHost>>>();
    Collection<ServiceComponentHost> ignoredScHosts =
        new ArrayList<ServiceComponentHost>();

    Set<String> clusterNames = new HashSet<String>();
    Map<String, Map<String, Set<String>>> componentNames =
        new HashMap<String, Map<String,Set<String>>>();
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

    for (ServiceComponentRequest request : requests) {
      final String clusterName = request.getClusterName();
      String componentName = request.getComponentName();
      if (clusterName == null
          || clusterName.isEmpty()
          || componentName == null
          || componentName.isEmpty()) {
        throw new IllegalArgumentException("Invalid arguments, cluster name"
            + ", service name and component name should be provided to"
            + " update components");
      }

      String serviceName = request.getServiceName();
      LOG.info("Received a updateComponent request"
          + ", clusterName=" + clusterName
          + ", serviceName=" + serviceName
          + ", componentName=" + componentName
          + ", request=" + request.toString());

      Cluster cluster = clusters.getCluster(clusterName);

      if (serviceName == null
          || serviceName.isEmpty()) {
        StackId stackId = cluster.getDesiredStackVersion();
        String alternativeServiceName =
            ambariMetaInfo.getComponentToService(stackId.getStackName(),
                stackId.getStackVersion(), componentName);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Looking up service name for component"
              + ", componentName=" + componentName
              + ", serviceName=" + alternativeServiceName);
        }

        if (alternativeServiceName == null
            || alternativeServiceName.isEmpty()) {
          throw new AmbariException("Could not find service for component"
              + ", componentName=" + componentName
              + ", clusterName=" + cluster.getClusterName()
              + ", stackInfo=" + stackId.getStackId());
        }
        request.setServiceName(alternativeServiceName);
        serviceName = alternativeServiceName;
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a updateComponent request"
                + ", clusterName=" + clusterName
                + ", serviceName=" + serviceName
                + ", componentName=" + componentName
                + ", request=" + request);
      }

      clusterNames.add(clusterName);

      if (clusterNames.size() > 1) {
        // FIXME throw correct error
        throw new IllegalArgumentException("Updates to multiple clusters is not"
            + " supported");
      }

      if (!componentNames.containsKey(clusterName)) {
        componentNames.put(clusterName,
            new HashMap<String, Set<String>>());
      }
      if (!componentNames.get(clusterName)
          .containsKey(serviceName)) {
        componentNames.get(clusterName).put(
                serviceName, new HashSet<String>());
      }
      if (componentNames.get(clusterName)
          .get(serviceName).contains(componentName)){
        // throw error later for dup
        throw new IllegalArgumentException("Invalid request contains duplicate"
            + " service components");
      }
      componentNames.get(clusterName)
          .get(serviceName).add(componentName);

      Service s = cluster.getService(serviceName);
      ServiceComponent sc = s.getServiceComponent(componentName);
      State newState = null;
      if (request.getDesiredState() != null) {
        newState = State.valueOf(request.getDesiredState());
        if (!newState.isValidDesiredState()) {
          throw new IllegalArgumentException("Invalid arguments, invalid"
              + " desired state, desiredState=" + newState.toString());
        }
      }

      if (! maintenanceStateHelper.isOperationAllowed(reqOpLvl, s)) {
        LOG.info("Operations cannot be applied to component " + componentName
                + " because service " + serviceName +
                " is in the maintenance state of " + s.getMaintenanceState());
        continue;
      }

      if (newState == null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Nothing to do for new updateServiceComponent request"
              + ", clusterName=" + clusterName
              + ", serviceName=" + serviceName
              + ", componentName=" + componentName
              + ", newDesiredState=null");
        }
        continue;
      }

      if (sc.isClientComponent() &&
          !newState.isValidClientComponentState()) {
        throw new AmbariException("Invalid desired state for a client"
            + " component");
      }

      seenNewStates.add(newState);

      State oldScState = sc.getDesiredState();
      if (newState != oldScState) {
        if (!State.isValidDesiredStateTransition(oldScState, newState)) {
          // FIXME throw correct error
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
        if (LOG.isDebugEnabled()) {
          LOG.debug("Handling update to ServiceComponent"
              + ", clusterName=" + clusterName
              + ", serviceName=" + serviceName
              + ", componentName=" + sc.getName()
              + ", currentDesiredState=" + oldScState
              + ", newDesiredState=" + newState);
        }
        changedComps.get(newState).add(sc);
      }

      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        State oldSchState = sch.getState();
        if (oldSchState == State.DISABLED || oldSchState == State.UNKNOWN) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Ignoring ServiceComponentHost"
                + ", clusterName=" + clusterName
                + ", serviceName=" + serviceName
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
                + ", clusterName=" + clusterName
                + ", serviceName=" + serviceName
                + ", componentName=" + sc.getName()
                + ", hostname=" + sch.getHostName()
                + ", currentState=" + oldSchState
                + ", newDesiredState=" + newState);
          }
          continue;
        }

        // do not update or alter any HC that is not active
        if (! maintenanceStateHelper.isOperationAllowed(reqOpLvl, sch)) {
          ignoredScHosts.add(sch);
          if (LOG.isDebugEnabled()) {
            LOG.debug("Ignoring ServiceComponentHost in maintenance state"
                + ", clusterName=" + clusterName
                + ", serviceName=" + serviceName
                + ", componentName=" + sc.getName()
                + ", hostname=" + sch.getHostName());
          }
          continue;
        }

        if (!State.isValidStateTransition(oldSchState, newState)) {
          // FIXME throw correct error
          throw new AmbariException("Invalid transition for"
              + " servicecomponenthost"
              + ", clusterName=" + cluster.getClusterName()
              + ", clusterId=" + cluster.getClusterId()
              + ", serviceName=" + sch.getServiceName()
              + ", componentName=" + sch.getServiceComponentName()
              + ", hostname=" + sch.getHostName()
              + ", currentState=" + oldSchState
              + ", newDesiredState=" + newState);
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
              + ", clusterName=" + clusterName
              + ", serviceName=" + serviceName
              + ", componentName=" + sc.getName()
              + ", hostname=" + sch.getHostName()
              + ", currentState=" + oldSchState
              + ", newDesiredState=" + newState);
        }
        changedScHosts.get(sc.getName()).get(newState).add(sch);
      }
    }

    if (seenNewStates.size() > 1) {
      // FIXME should we handle this scenario
      throw new IllegalArgumentException("Cannot handle different desired"
          + " state changes for a set of service components at the same time");
    }

    // TODO additional validation?

    Cluster cluster = clusters.getCluster(clusterNames.iterator().next());

    return getManagementController().createAndPersistStages(cluster, requestProperties, null, null, changedComps, changedScHosts,
        ignoredScHosts, runSmokeTest, false);
  }

  protected RequestStatusResponse deleteComponents(Set<ServiceComponentRequest> requests) throws AmbariException {
    AmbariManagementController controller = getManagementController();
    Clusters clusters = controller.getClusters();
    AmbariMetaInfo ambariMetaInfo = controller.getAmbariMetaInfo();

    for (ServiceComponentRequest request : requests) {

      if (request.getClusterName() == null || request.getClusterName().isEmpty()
          || request.getComponentName() == null || request.getComponentName().isEmpty()) {
          throw new IllegalArgumentException("Invalid arguments"
          + ", clustername and componentname should be"
          + " non-null and non-empty when trying to create a"
          + " component");
      }

      Cluster cluster;
      try {
        cluster = clusters.getCluster(request.getClusterName());
      } catch (ClusterNotFoundException e) {
        throw new ParentObjectNotFoundException(
              "Attempted to add a component to a cluster which doesn't exist:", e);
      }

      if (request.getServiceName() == null || request.getServiceName().isEmpty()) {
        StackId stackId = cluster.getDesiredStackVersion();
        String serviceName = ambariMetaInfo.getComponentToService(stackId.getStackName(),
                                                                stackId.getStackVersion(), request.getComponentName());
        if (LOG.isDebugEnabled()) {
          LOG.debug("Looking up service name for component"
                    + ", componentName=" + request.getComponentName()
                    + ", serviceName=" + serviceName);
        }

        if (serviceName == null || serviceName.isEmpty()) {
            throw new AmbariException("Could not find service for component"
                         + ", componentName=" + request.getComponentName()
                         + ", clusterName=" + cluster.getClusterName()
                         + ", stackInfo=" + stackId.getStackId());
        }
        request.setServiceName(serviceName);
        }

      Service s;
      try {
        s = cluster.getService(request.getServiceName());
      } catch (ServiceNotFoundException e) {
        throw new ParentObjectNotFoundException(
                    "Attempted to add a component to a service which doesn't exist:", e);
      }

      ServiceComponent sc = s.getServiceComponent(request.getComponentName());
      if (sc != null) {
        if (!sc.getDesiredState().isRemovableState()) {
          throw new AmbariException("Could not delete service component from cluster. To remove service component," +
                 " it must be in DISABLED/INIT/INSTALLED/INSTALL_FAILED/UNKNOWN/UNINSTALLED/INSTALLING state."
                 + ", clusterName=" + request.getClusterName()
                 + ", serviceName=" + request.getServiceName()
                 + ", componentName=" + request.getComponentName()
                 + ", current state=" + sc.getDesiredState() + ".");
          }

        for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
          if (!sch.getDesiredState().isRemovableState()) {
            throw new AmbariException("Found non removable host component when trying to delete service component." +
                      " To remove host component, it must be in DISABLED/INIT/INSTALLED/INSTALL_FAILED/UNKNOWN" +
                      "/UNINSTALLED/INSTALLING state."
                      + ", clusterName=" + request.getClusterName()
                      + ", serviceName=" + request.getServiceName()
                      + ", componentName=" + request.getComponentName()
                      + ", current state=" + sc.getDesiredState() + ".");

          }
        }

        s.deleteServiceComponent(request.getComponentName());
      }
    }
    return null;
  }

}
