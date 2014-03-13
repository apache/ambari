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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.api.services.PersistKeyValueService;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ClusterResponse;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.StackConfigurationRequest;
import org.apache.ambari.server.controller.StackConfigurationResponse;
import org.apache.ambari.server.controller.StackServiceComponentRequest;
import org.apache.ambari.server.controller.StackServiceComponentResponse;
import org.apache.ambari.server.controller.StackServiceRequest;
import org.apache.ambari.server.controller.StackServiceResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.BlueprintDAO;
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.orm.entities.HostGroupComponentEntity;
import org.apache.ambari.server.orm.entities.HostGroupEntity;

/**
 * Resource provider for cluster resources.
 */
public class ClusterResourceProvider extends AbstractControllerResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  // Clusters
  protected static final String CLUSTER_ID_PROPERTY_ID      = PropertyHelper.getPropertyId("Clusters", "cluster_id");
  protected static final String CLUSTER_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("Clusters", "cluster_name");
  protected static final String CLUSTER_VERSION_PROPERTY_ID = PropertyHelper.getPropertyId("Clusters", "version");
  protected static final String CLUSTER_DESIRED_CONFIGS_PROPERTY_ID = PropertyHelper.getPropertyId("Clusters", "desired_configs");
  protected static final String BLUEPRINT_PROPERTY_ID = PropertyHelper.getPropertyId(null, "blueprint");


  private static Set<String> pkPropertyIds =
      new HashSet<String>(Arrays.asList(new String[]{
          CLUSTER_ID_PROPERTY_ID}));

  /**
   * Data access object used to obtain blueprint entities.
   */
  private static BlueprintDAO blueprintDAO;

  /**
   * Maps properties to updaters which update the property when provisioning a cluster via a blueprint
   */
  private Map<String, PropertyUpdater> propertyUpdaters =
      new HashMap<String, PropertyUpdater>();

  /**
   * Maps configuration type (string) to associated properties
   */
  private Map<String, Map<String, String>> mapClusterConfigurations =
      new HashMap<String, Map<String, String>>();


  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds           the property ids
   * @param keyPropertyIds        the key property ids
   * @param managementController  the management controller
   */
  ClusterResourceProvider(Set<String> propertyIds,
                          Map<Resource.Type, String> keyPropertyIds,
                          AmbariManagementController managementController) {

    super(propertyIds, keyPropertyIds, managementController);
    registerPropertyUpdaters();
  }

  /**
   * Inject the blueprint data access object which is used to obtain blueprint entities
   *
   * @param dao  blueprint data access object
   */
  public static void injectBlueprintDAO(BlueprintDAO dao) {
    blueprintDAO = dao;
  }


// ----- ResourceProvider ------------------------------------------------

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException,
             UnsupportedPropertyException,
             ResourceAlreadyExistsException,
             NoSuchParentResourceException {

    RequestStatusResponse createResponse = null;
    for (final Map<String, Object> properties : request.getProperties()) {
      if (isCreateFromBlueprint(properties)) {
        createResponse = processBlueprintCreate(properties);
      } else {
        createClusterResource(properties);
      }
    }

    notifyCreate(Resource.Type.Cluster, request);
    return getRequestStatus(createResponse);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ClusterRequest> requests = new HashSet<ClusterRequest>();

    if (predicate == null) {
      requests.add(getRequest(Collections.<String, Object>emptyMap()));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }
    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<ClusterResponse> responses = getResources(new Command<Set<ClusterResponse>>() {
      @Override
      public Set<ClusterResponse> invoke() throws AmbariException {
        return getManagementController().getClusters(requests);
      }
    });

    Set<Resource> resources = new HashSet<Resource>();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Found clusters matching getClusters request"
          + ", clusterResponseCount=" + responses.size());
    }
    for (ClusterResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.Cluster);
      setResourceProperty(resource, CLUSTER_ID_PROPERTY_ID, response.getClusterId(), requestedIds);
      setResourceProperty(resource, CLUSTER_NAME_PROPERTY_ID, response.getClusterName(), requestedIds);
      setResourceProperty(resource, CLUSTER_DESIRED_CONFIGS_PROPERTY_ID, response.getDesiredConfigs(), requestedIds);

      resource.setProperty(CLUSTER_VERSION_PROPERTY_ID,
          response.getDesiredStackVersion());

      if (LOG.isDebugEnabled()) {
        LOG.debug("Adding ClusterResponse to resource"
            + ", clusterResponse=" + response.toString());
      }

      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(final Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ClusterRequest>   requests = new HashSet<ClusterRequest>();
    RequestStatusResponse       response;

    for (Map<String, Object> requestPropertyMap : request.getProperties()) {
      Set<Map<String, Object>> propertyMaps = getPropertyMaps(requestPropertyMap, predicate);
      for (Map<String, Object> propertyMap : propertyMaps) {
        requests.add(getRequest(propertyMap));
      }
    }
    response = modifyResources(new Command<RequestStatusResponse>() {
      @Override
      public RequestStatusResponse invoke() throws AmbariException {
        return getManagementController().updateClusters(requests, request.getRequestInfoProperties());
      }
    });
    notifyUpdate(Resource.Type.Cluster, request, predicate);
    return getRequestStatus(response);
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      final ClusterRequest clusterRequest = getRequest(propertyMap);
      modifyResources(new Command<Void>() {
        @Override
        public Void invoke() throws AmbariException {
          getManagementController().deleteCluster(clusterRequest);
          return null;
        }
      });
    }
    notifyDelete(Resource.Type.Cluster, predicate);
    return getRequestStatus(null);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  /**
   * {@inheritDoc}  Overridden to support configuration.
   */
  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    Set<String> baseUnsupported = super.checkPropertyIds(propertyIds);

    // extract to own method
    baseUnsupported.remove("blueprint");
    baseUnsupported.remove("host-groups");

    return checkConfigPropertyIds(baseUnsupported, "Clusters");
  }

  // ----- utility methods -------------------------------------------------

  /**
   * Get a cluster request object from a map of property values.
   *
   * @param properties  the predicate
   *
   * @return the cluster request object
   */
  private ClusterRequest getRequest(Map<String, Object> properties) {
    ClusterRequest cr = new ClusterRequest(
        (Long) properties.get(CLUSTER_ID_PROPERTY_ID),
        (String) properties.get(CLUSTER_NAME_PROPERTY_ID),
        (String) properties.get(CLUSTER_VERSION_PROPERTY_ID),
        null);

    
    ConfigurationRequest configRequest = getConfigurationRequest("Clusters", properties);
    
    if (null != configRequest)
      cr.setDesiredConfig(configRequest);
    
    return cr;
  }

  /**
   * Determine if the request is a create using a blueprint.
   *
   * @param properties  request properties
   *
   * @return true if request is a create using a blueprint; false otherwise
   */
  private boolean isCreateFromBlueprint(Map<String, Object> properties) {
    return properties.get("blueprint") != null;
  }

  /**
   * Process a create request specifying a blueprint.  This includes creation of all resources,
   * setting of configuration and installing and starting of all services.  The end result of this
   * call will be a running cluster based on the topology and configuration specified in the blueprint.
   *
   * @param properties  request body properties
   *
   * @return asynchronous response information
   *
   * @throws ResourceAlreadyExistsException if cluster already exists
   * @throws SystemException                if an unexpected exception occurs
   * @throws UnsupportedPropertyException   if an invalid property is specified in the request
   * @throws NoSuchParentResourceException  if a necessary parent resource doesn't exist
   */
  private RequestStatusResponse processBlueprintCreate(Map<String, Object> properties)
      throws ResourceAlreadyExistsException, SystemException, UnsupportedPropertyException,
      NoSuchParentResourceException {

    String blueprintName = (String) properties.get(BLUEPRINT_PROPERTY_ID);

    LOG.info("Creating Cluster '" + properties.get(CLUSTER_NAME_PROPERTY_ID) +
        "' based on blueprint '" + blueprintName + "'.");

    //todo: build up a proper topology object
    BlueprintEntity blueprint = getBlueprint(blueprintName);
    Stack stack = parseStack(blueprint);

    Map<String, HostGroup> blueprintHostGroups = parseBlueprintHostGroups(blueprint, stack);
    applyRequestInfoToHostGroups(properties, blueprintHostGroups);
    processConfigurations(stack, blueprintHostGroups);

    String clusterName = (String) properties.get(CLUSTER_NAME_PROPERTY_ID);
    createClusterResource(buildClusterResourceProperties(stack, clusterName));
    setConfigurationsOnCluster(clusterName);

    Set<String> services = getServicesToDeploy(stack, blueprintHostGroups);

    createServiceAndComponentResources(blueprintHostGroups, clusterName, services);
    createHostAndComponentResources(blueprintHostGroups, clusterName);

    persistInstallStateForUI();
    return ((ServiceResourceProvider) getResourceProviderByType(Resource.Type.Service)).
        installAndStart(clusterName);
  }

  /**
   * Obtain a blueprint entity based on name.
   *
   * @param blueprintName  name of blueprint to obtain
   *
   * @return blueprint entity for the given name
   * @throws IllegalArgumentException no blueprint with the given name found
   */
  private BlueprintEntity getBlueprint(String blueprintName) {
    BlueprintEntity blueprint = blueprintDAO.findByName(blueprintName);
    if (blueprint == null) {
      throw new IllegalArgumentException("Specified blueprint doesn't exist: " + blueprintName);
    }
    return blueprint;
  }

  /**
   * Create service and component resources.
   *
   * @param blueprintHostGroups  host groups contained in blueprint
   * @param clusterName          cluster name
   * @param services             services to be deployed
   *
   * @throws SystemException                an unexpected exception occurred
   * @throws UnsupportedPropertyException   an unsupported property was specified in the request
   * @throws ResourceAlreadyExistsException attempted to create a service or component that already exists
   * @throws NoSuchParentResourceException  a required parent resource is missing
   */
  private void createServiceAndComponentResources(Map<String, HostGroup> blueprintHostGroups,
                                                  String clusterName, Set<String> services)
                                                  throws SystemException,
                                                         UnsupportedPropertyException,
                                                         ResourceAlreadyExistsException,
                                                         NoSuchParentResourceException {

    Set<Map<String, Object>> setServiceRequestProps = new HashSet<Map<String, Object>>();
    for (String service : services) {
      Map<String, Object> serviceProperties = new HashMap<String, Object>();
      serviceProperties.put(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID, clusterName);
      serviceProperties.put(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID, service);
      setServiceRequestProps.add(serviceProperties);
    }

    Request serviceRequest = new RequestImpl(null, setServiceRequestProps, null, null);
    getResourceProviderByType(Resource.Type.Service).createResources(serviceRequest);
    createComponentResources(blueprintHostGroups, clusterName, services);
  }

  /**
   * Build the cluster properties necessary for creating a cluster resource.
   *
   * @param stack        associated stack
   * @param clusterName  cluster name
   * @return map of cluster properties used to create a cluster resource
   */
  private Map<String, Object> buildClusterResourceProperties(Stack stack, String clusterName) {
    Map<String, Object> clusterProperties = new HashMap<String, Object>();
    clusterProperties.put(CLUSTER_NAME_PROPERTY_ID, clusterName);
    clusterProperties.put(CLUSTER_VERSION_PROPERTY_ID, stack.name + "-" + stack.version);
    return clusterProperties;
  }

  /**
   * Create host and host_component resources.
   *
   * @param blueprintHostGroups  host groups specified in blueprint
   * @param clusterName          cluster name
   *
   * @throws SystemException                an unexpected exception occurred
   * @throws UnsupportedPropertyException   an invalid property was specified
   * @throws ResourceAlreadyExistsException attempt to create a host or host_component which already exists
   * @throws NoSuchParentResourceException  a required parent resource is missing
   */
  //todo: ambari agent must already be installed and registered on all hosts
  private void createHostAndComponentResources(Map<String, HostGroup> blueprintHostGroups, String clusterName)
      throws SystemException, UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {

    ResourceProvider hostProvider = getResourceProviderByType(Resource.Type.Host);
    ResourceProvider hostComponentProvider = getResourceProviderByType(Resource.Type.HostComponent);
    for (HostGroup group : blueprintHostGroups.values()) {
      for (String host : group.getHostInfo()) {
        Map<String, Object> hostProperties = new HashMap<String, Object>();
        hostProperties.put("Hosts/cluster_name", clusterName);
        hostProperties.put("Hosts/host_name", host);

        hostProvider.createResources(new RequestImpl(
            null, Collections.singleton(hostProperties), null, null));

        // create clusters/hosts/host_components
        Set<Map<String, Object>> setHostComponentRequestProps = new HashSet<Map<String, Object>>();
        for (String hostComponent : group.getComponents()) {
          // AMBARI_SERVER is not recognized by Ambari as a component
          if (! hostComponent.equals("AMBARI_SERVER")) {
            Map<String, Object> hostComponentProperties = new HashMap<String, Object>();
            hostComponentProperties.put("HostRoles/cluster_name", clusterName);
            hostComponentProperties.put("HostRoles/host_name", host);
            hostComponentProperties.put("HostRoles/component_name", hostComponent);
            setHostComponentRequestProps.add(hostComponentProperties);
          }
        }
        hostComponentProvider.createResources(new RequestImpl(
            null, setHostComponentRequestProps, null, null));
      }
    }
  }

  /**
   * Create component resources.
   *
   * @param blueprintHostGroups  host groups specified in blueprint
   * @param clusterName          cluster name
   * @param services             services to be deployed
   *
   * @throws SystemException                an unexpected exception occurred
   * @throws UnsupportedPropertyException   an invalid property was specified
   * @throws ResourceAlreadyExistsException attempt to create a component which already exists
   * @throws NoSuchParentResourceException  a required parent resource is missing
   */
  private void createComponentResources(Map<String, HostGroup> blueprintHostGroups,
                                        String clusterName, Set<String> services)
                                        throws SystemException,
                                               UnsupportedPropertyException,
                                               ResourceAlreadyExistsException,
                                               NoSuchParentResourceException {
    for (String service : services) {
      Set<String> components = new HashSet<String>();
      for (HostGroup hostGroup : blueprintHostGroups.values()) {
        Collection<String> serviceComponents = hostGroup.getComponents(service);
        if (serviceComponents != null && !serviceComponents.isEmpty()) {
          components.addAll(serviceComponents);
        }
      }

      Set<Map<String, Object>> setComponentRequestProps = new HashSet<Map<String, Object>>();
      for (String component : components) {
        Map<String, Object> componentProperties = new HashMap<String, Object>();
        componentProperties.put("ServiceComponentInfo/cluster_name", clusterName);
        componentProperties.put("ServiceComponentInfo/service_name", service);
        componentProperties.put("ServiceComponentInfo/component_name", component);
        setComponentRequestProps.add(componentProperties);
      }
      Request componentRequest = new RequestImpl(null, setComponentRequestProps, null, null);
      ResourceProvider componentProvider = getResourceProviderByType(Resource.Type.Component);
      componentProvider.createResources(componentRequest);
    }
  }

  /**
   * Set all configurations on the cluster resource.
   *
   * @param clusterName  cluster name
   *
   * @throws SystemException an unexpected exception occurred
   */
  private void setConfigurationsOnCluster(String clusterName) throws SystemException {
    for (Map.Entry<String, Map<String, String>> entry : mapClusterConfigurations.entrySet()) {
      String type = entry.getKey();
      if (type.endsWith(".xml")) {
        type = type.substring(0, type.length() - 4);
      }
      try {
        //todo: properly handle non system exceptions
        setConfigurationsOnCluster(clusterName, type, entry.getValue());
      } catch (AmbariException e) {
        throw new SystemException("Unable to set configurations on cluster.", e);
      }
    }
  }

  /**
   * Set configuration of a specific type on the cluster resource.
   *
   * @param clusterName  cluster name
   * @param type         configuration type that is to be set
   * @param properties   properties to set
   *
   * @throws AmbariException if an exception occurs setting the properties
   */
  private void setConfigurationsOnCluster(String clusterName, String type,
                                          Map<String, String> properties) throws AmbariException {

    Map<String, Object> clusterProperties = new HashMap<String, Object>();
    clusterProperties.put(CLUSTER_NAME_PROPERTY_ID, clusterName);
    clusterProperties.put(CLUSTER_DESIRED_CONFIGS_PROPERTY_ID + "/type", type);
    clusterProperties.put(CLUSTER_DESIRED_CONFIGS_PROPERTY_ID + "/tag", "1");
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      clusterProperties.put(CLUSTER_DESIRED_CONFIGS_PROPERTY_ID +
          "/properties/" + entry.getKey(), entry.getValue());
    }

    getManagementController().updateClusters(
        Collections.singleton(getRequest(clusterProperties)), null);
  }

  /**
   * Apply the information contained in the cluster request body such as host an configuration properties to
   * the associated blueprint.
   *
   * @param properties           request properties
   * @param blueprintHostGroups  blueprint host groups
   *
   * @throws IllegalArgumentException a host-group in the request doesn't match a host-group in the blueprint
   */
  private void applyRequestInfoToHostGroups(Map<String, Object> properties, Map<String,
                                            HostGroup> blueprintHostGroups)
                                            throws IllegalArgumentException {

    @SuppressWarnings("unchecked")
    Collection<Map<String, Object>> hostGroups =
        (Collection<Map<String, Object>>) properties.get("host-groups");

    // iterate over host groups provided in request body
    for (Map<String, Object> hostGroupProperties : hostGroups) {
      String name = (String) hostGroupProperties.get("name");
      HostGroup hostGroup = blueprintHostGroups.get(name);

      if (hostGroup == null) {
        throw new IllegalArgumentException("Invalid host-group specified: " + name +
          ".  All request host groups must have a corresponding host group in the specified blueprint");
      }

      Collection hosts = (Collection) hostGroupProperties.get("hosts");
      for (Object oHost : hosts) {
        @SuppressWarnings("unchecked")
        Map<String, String> mapHostProperties = (Map<String, String>) oHost;
        //add host information to host group
        hostGroup.addHostInfo(mapHostProperties.get("fqdn"));
      }
    }
  }

  /**
   * Parse blueprint host groups.
   *
   * @param blueprint  associated blueprint
   * @param stack      associated stack
   *
   * @return map of host group name to host group
   */
  private Map<String, HostGroup> parseBlueprintHostGroups(BlueprintEntity blueprint, Stack stack) {
    Map<String, HostGroup> mapHostGroups = new HashMap<String, HostGroup>();

    for (HostGroupEntity hostGroup : blueprint.getHostGroups()) {
      mapHostGroups.put(hostGroup.getName(), new HostGroup(hostGroup, stack));
    }
    return mapHostGroups;
  }

  /**
   * Parse stack information.
   *
   * @param blueprint  associated blueprint
   *
   * @return stack instance
   *
   * @throws SystemException an unexpected exception occurred
   */
  private Stack parseStack(BlueprintEntity blueprint) throws SystemException {
    Stack stack;
    try {
      stack = new Stack(blueprint.getStackName(), blueprint.getStackVersion());
    } catch (StackAccessException e) {
      throw new IllegalArgumentException("Invalid stack information provided for cluster.  " +
          "stack name: " + blueprint.getStackName() +
          " stack version: " + blueprint.getStackVersion());
    } catch (AmbariException e) {
      //todo: review all exception handling associated with cluster creation via blueprint
      throw new SystemException("Unable to obtain stack information.", e);
    }
    return stack;
  }

  /**
   * Create the cluster resource.
   *
   * @param properties  cluster resource request properties
   *
   * @throws ResourceAlreadyExistsException  cluster resource already exists
   * @throws SystemException                 an unexpected exception occurred
   * @throws NoSuchParentResourceException   shouldn't be thrown as a cluster doesn't have a parent resource
   */
  private void createClusterResource(final Map<String, Object> properties)
      throws ResourceAlreadyExistsException, SystemException, NoSuchParentResourceException {

    createResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        getManagementController().createCluster(getRequest(properties));
        return null;
      }
    });
  }
  /**
   * Obtain a resource provider based on type.
   *
   * @param type  resource provider type
   *
   * @return resource provider for the specified type
   */
  //todo: inject a better mechanism for getting resource providers
  ResourceProvider getResourceProviderByType(Resource.Type type) {
    return ((ClusterControllerImpl) ClusterControllerHelper.getClusterController()).
        ensureResourceProvider(type);
  }

  /**
   * Persist cluster state for the ambari UI.  Setting this state informs that UI that a cluster has been
   * installed and started and that the monitoring screen for the cluster should be displayed to the user.
   *
   * @throws SystemException if an unexpected exception occurs
   */
  private void persistInstallStateForUI() throws SystemException {
    PersistKeyValueService persistService = new PersistKeyValueService();
    try {
      persistService.update("{\"CLUSTER_CURRENT_STATUS\": \"{\\\"clusterState\\\":\\\"CLUSTER_STARTED_5\\\"}\"}");
    } catch (Exception e) {
      throw new SystemException("Unable to finalize state of cluster for UI.  " +
          "Cluster creation will not be affected but the cluster may be inaccessible by Ambari UI." );
    }
  }

  /**
   * Process cluster configurations.  This includes obtaining the default configuration properties from the stack,
   * overlaying configuration properties specified in the cluster create request and updating properties with
   * topology specific information.
   *
   * @param stack                associated stack
   * @param blueprintHostGroups  host groups contained in the blueprint
   */
  // processing at cluster level only now
  public void processConfigurations(Stack stack, Map<String, HostGroup> blueprintHostGroups)  {
    Set<String> services = getServicesToDeploy(stack, blueprintHostGroups);

    for (String service : services) {
      Collection<String> configTypes = stack.getConfigurationTypes(service);
      for (String type : configTypes) {
        Map<String, String> properties = stack.getConfigurationProperties(service, type);
        for (Map.Entry<String, String> entry : properties.entrySet()) {
          String propName = entry.getKey();
          String value    = entry.getValue();

          Map<String, String> typeProps = mapClusterConfigurations.get(type);
          if (typeProps == null) {
            typeProps = new HashMap<String, String>();
            mapClusterConfigurations.put(type, typeProps);
          }
          // todo: should an exception be thrown if a property is included under multiple services
          if (! typeProps.containsKey(propName)) {
            // see if property needs to be updated
            PropertyUpdater propertyUpdater = propertyUpdaters.get(propName);
            if (propertyUpdater != null) {
              value = propertyUpdater.update(blueprintHostGroups, entry.getValue());
            }
            typeProps.put(propName, value);
          }
        }
      }
    }
    // AMBARI-4921
    //todo: hard-coding default values for required global config properties which are not in stack definition
    Map<String, String> globalProperties = mapClusterConfigurations.get("global.xml");
    if (globalProperties == null) {
      globalProperties = new HashMap<String, String>();
      mapClusterConfigurations.put("global.xml", globalProperties);
    }
    globalProperties.put("user_group", "hadoop");
    globalProperties.put("smokeuser", "ambari-qa");
    globalProperties.put("nagios_contact", "default@REPLACEME.NOWHERE");
    globalProperties.put("nagios_web_password", "admin");
  }

  /**
   * Get set of services which are to be deployed.
   *
   * @param stack                stack information
   * @param blueprintHostGroups  host groups contained in blueprint
   *
   * @return set of service names which will be deployed
   */
  private Set<String> getServicesToDeploy(Stack stack, Map<String, HostGroup> blueprintHostGroups) {
    Set<String> services = new HashSet<String>();
    for (HostGroup group : blueprintHostGroups.values()) {
      if (! group.getHostInfo().isEmpty()) {
        services.addAll(stack.getServicesForComponents(group.getComponents()));
      }
    }
    //remove entry associated with Ambari Server since this isn't recognized by Ambari
    services.remove(null);

    return services;
  }

  /**
   * Register updaters for configuration properties.
   */
  private void registerPropertyUpdaters() {
    // NAMENODE
    propertyUpdaters.put("dfs.http.address", new SingleHostPropertyUpdater("NAMENODE"));
    propertyUpdaters.put("dfs.namenode.http-address", new SingleHostPropertyUpdater("NAMENODE"));
    propertyUpdaters.put("dfs.https.address", new SingleHostPropertyUpdater("NAMENODE"));
    propertyUpdaters.put("dfs.namenode.https-address", new SingleHostPropertyUpdater("NAMENODE"));
    propertyUpdaters.put("fs.default.name", new SingleHostPropertyUpdater("NAMENODE"));
    propertyUpdaters.put("fs.defaultFS", new SingleHostPropertyUpdater("NAMENODE"));
    propertyUpdaters.put("hbase.rootdir", new SingleHostPropertyUpdater("NAMENODE"));

    // SECONDARY_NAMENODE
    propertyUpdaters.put("dfs.secondary.http.address", new SingleHostPropertyUpdater("SECONDARY_NAMENODE"));
    propertyUpdaters.put("dfs.namenode.secondary.http-address", new SingleHostPropertyUpdater("SECONDARY_NAMENODE"));

    // HISTORY_SERVER
    propertyUpdaters.put("yarn.log.server.url", new SingleHostPropertyUpdater("HISTORYSERVER"));
    propertyUpdaters.put("mapreduce.jobhistory.webapp.address", new SingleHostPropertyUpdater("HISTORYSERVER"));
    propertyUpdaters.put("mapreduce.jobhistory.address", new SingleHostPropertyUpdater("HISTORYSERVER"));

    // RESOURCEMANAGER
    propertyUpdaters.put("yarn.resourcemanager.hostname", new SingleHostPropertyUpdater("RESOURCEMANAGER"));
    propertyUpdaters.put("yarn.resourcemanager.resource-tracker.address", new SingleHostPropertyUpdater("RESOURCEMANAGER"));
    propertyUpdaters.put("yarn.resourcemanager.webapp.address", new SingleHostPropertyUpdater("RESOURCEMANAGER"));
    propertyUpdaters.put("yarn.resourcemanager.scheduler.address", new SingleHostPropertyUpdater("RESOURCEMANAGER"));
    propertyUpdaters.put("yarn.resourcemanager.address", new SingleHostPropertyUpdater("RESOURCEMANAGER"));
    propertyUpdaters.put("yarn.resourcemanager.admin.address", new SingleHostPropertyUpdater("RESOURCEMANAGER"));

    // JOBTRACKER
    propertyUpdaters.put("mapred.job.tracker", new SingleHostPropertyUpdater("JOBTRACKER"));
    propertyUpdaters.put("mapred.job.tracker.http.address", new SingleHostPropertyUpdater("JOBTRACKER"));
    propertyUpdaters.put("mapreduce.history.server.http.address", new SingleHostPropertyUpdater("JOBTRACKER"));

    // HIVE_SERVER
    propertyUpdaters.put("hive.metastore.uris", new SingleHostPropertyUpdater("HIVE_SERVER"));
    propertyUpdaters.put("hive_ambari_host", new SingleHostPropertyUpdater("HIVE_SERVER"));

    // OOZIE_SERVER
    propertyUpdaters.put("oozie.base.url", new SingleHostPropertyUpdater("OOZIE_SERVER"));
    propertyUpdaters.put("oozie_ambari_host", new SingleHostPropertyUpdater("OOZIE_SERVER"));

    // ZOOKEEPER_SERVER
    propertyUpdaters.put("hbase.zookeeper.quorum", new MultipleHostPropertyUpdater("ZOOKEEPER_SERVER"));
    propertyUpdaters.put("templeton.zookeeper.hosts", new MultipleHostPropertyUpdater("ZOOKEEPER_SERVER"));

    // properties which need "m' appended.  Required due to AMBARI-4933
    propertyUpdaters.put("namenode_heapsize", new MPropertyUpdater());
    propertyUpdaters.put("namenode_opt_newsize", new MPropertyUpdater());
    propertyUpdaters.put("namenode_opt_maxnewsize", new MPropertyUpdater());
    propertyUpdaters.put("dtnode_heapsize", new MPropertyUpdater());
    propertyUpdaters.put("jtnode_opt_newsize", new MPropertyUpdater());
    propertyUpdaters.put("jtnode_opt_maxnewsize", new MPropertyUpdater());
    propertyUpdaters.put("jtnode_heapsize", new MPropertyUpdater());
    propertyUpdaters.put("hbase_master_heapsize", new MPropertyUpdater());
    propertyUpdaters.put("hbase_regionserver_heapsize", new MPropertyUpdater());
  }

  /**
   * Get host groups which contain a component.
   *
   * @param component   component name
   * @param hostGroups  collection of host groups to check
   *
   * @return collection of host groups which contain the specified component
   */
  private Collection<HostGroup> getHostGroupsForComponent(String component, Collection<HostGroup> hostGroups) {
    Collection<HostGroup> resultGroups = new HashSet<HostGroup>();
    for (HostGroup group : hostGroups ) {
      if (group.getComponents().contains(component)) {
        resultGroups.add(group);
      }
    }
    return resultGroups;
  }


  // ----- Inner Classes -----------------------------------------------------

  /**
   * Encapsulates stack information.
   */
  private class Stack {
    /**
     * Stack name
     */
    private String name;

    /**
     * Stack version
     */
    private String version;

    /**
     * Map of service name to components
     */
    private Map<String, Collection<String>> serviceComponents = new HashMap<String, Collection<String>>();

    /**
     * Map of component to service
     */
    private Map<String, String> componentService = new HashMap<String, String>();

    /**
     * Map of service to config type properties
     */
    private Map<String, Map<String, Map<String, String>>> serviceConfigurations =
        new HashMap<String, Map<String, Map<String, String>>>();

    /**
     * Constructor.
     *
     * @param name     stack name
     * @param version  stack version
     *
     * @throws AmbariException an exception occurred getting stack information
     *                         for the specified name and version
     */
    public Stack(String name, String version) throws AmbariException {
      this.name = name;
      this.version = version;

      Set<StackServiceResponse> stackServices = getManagementController().getStackServices(
          Collections.singleton(new StackServiceRequest(name, version, null)));

      for (StackServiceResponse stackService : stackServices) {
        String serviceName = stackService.getServiceName();
        parseComponents(serviceName);
        parseConfigurations(serviceName);
      }
    }

    /**
     * Get services contained in the stack.
     *
     * @return collection of all services for the stack
     */
    public Collection<String> getServices() {
      return serviceComponents.keySet();
    }

    /**
     * Get components contained in the stack for the specified service.
     *
     * @param service  service name
     *
     * @return collection of component names for the specified service
     */
    public Collection<String> getComponents(String service) {
      return serviceComponents.get(service);
    }

    /**
     * Get configuration types for the specified service.
     *
     * @param service  service name
     *
     * @return collection of configuration types for the specified service
     */
    public Collection<String> getConfigurationTypes(String service) {
      return serviceConfigurations.get(service).keySet();
    }

    /**
     * Get config properties for the specified service and configuration type.
     *
     * @param service  service name
     * @param type     configuration type
     *
     * @return map of property names to values for the specified service and configuration type
     */
    public Map<String, String> getConfigurationProperties(String service, String type) {
      return serviceConfigurations.get(service).get(type);
    }

    /**
     * Get the service for the specified component.
     *
     * @param component  component name
     *
     * @return service name that contains tha specified component
     */
    public String getServiceForComponent(String component) {
      return componentService.get(component);
    }

    /**
     * Get the names of the services which contains the specified components.
     *
     * @param components collection of components
     *
     * @return collection of services which contain the specified components
     */
    public Collection<String> getServicesForComponents(Collection<String> components) {
      Set<String> services = new HashSet<String>();
      for (String component : components) {
        services.add(getServiceForComponent(component));
      }

      return services;
    }

    /**
     * Parse components for the specified service from the stack definition.
     *
     * @param service  service name
     *
     * @throws AmbariException an exception occurred getting components from the stack definition
     */
    private void parseComponents(String service) throws AmbariException{
      Collection<String> componentSet = new HashSet<String>();

      Set<StackServiceComponentResponse> components = getManagementController().getStackComponents(
          Collections.singleton(new StackServiceComponentRequest(name, version, service, null)
      ));

      // stack service components
      for (StackServiceComponentResponse component : components) {
        String componentName = component.getComponentName();
        componentSet.add(componentName);
        componentService.put(componentName, service);
      }
      this.serviceComponents.put(service, componentSet);
    }

    /**
     * Parse configurations for the specified service from the stack definition.
     *
     * @param service  service name
     *
     * @throws AmbariException an exception occurred getting configurations from the stack definition
     */
    private void parseConfigurations(String service) throws AmbariException {
      Map<String, Map<String, String>> mapServiceConfig =
          new HashMap<String, Map<String, String>>();

      serviceConfigurations.put(service, mapServiceConfig);

      Set<StackConfigurationResponse> serviceConfigs = getManagementController().getStackConfigurations(
          Collections.singleton(new StackConfigurationRequest(name, version, service, null)
      ));

      for (StackConfigurationResponse config : serviceConfigs) {
        String type = config.getType();
        Map<String, String> mapTypeConfig = mapServiceConfig.get(type);
        if (mapTypeConfig == null) {
          mapTypeConfig = new HashMap<String, String>();
          mapServiceConfig.put(type, mapTypeConfig);
        }
        mapTypeConfig.put(config.getPropertyName(), config.getPropertyValue());
      }
    }
  }

  /**
   * Host group representation.
   */
  private class HostGroup {
    /**
     * Host group entity
     */
    private HostGroupEntity hostGroup;

    /**
     * Components contained in the host group
     */
    private Collection<String> components = new HashSet<String>();

    /**
     * Hosts contained associated with the host group
     */
    private Collection<String> hosts = new HashSet<String>();

    /**
     * Map of service to components for the host group
     */
    private Map<String, Set<String>> componentsForService = new HashMap<String, Set<String>>();

    /**
     * Associated stack
     */
    private Stack stack;

    /**
     * Constructor.
     *
     * @param hostGroup  host group
     * @param stack      stack
     */
    public HostGroup(HostGroupEntity hostGroup, Stack stack) {
      this.hostGroup = hostGroup;
      this.stack = stack;
      parseComponents();
    }

    /**
     * Associate a host with the host group.
     *
     * @param fqdn  fully qualified domain name of the host being added
     */
    public void addHostInfo(String fqdn) {
      this.hosts.add(fqdn);
    }

    /**
     * Get associated host information.
     *
     * @return collection of hosts associated with the host group
     */
    public Collection<String> getHostInfo() {
      return this.hosts;
    }

    /**
     * Get the components associated with the host group.
     *
     * @return  collection of component names for the host group
     */
    public Collection<String> getComponents() {
      return this.components;
    }

    /**
     * Get the components for the specified service which are associated with the host group.
     *
     * @param service  service name
     *
     * @return set of component names
     */
    public Collection<String> getComponents(String service) {
      return componentsForService.get(service);
    }

    /**
     * Parse component information.
     */
    private void parseComponents() {
      for (HostGroupComponentEntity componentEntity : hostGroup.getComponents() ) {
        String name = componentEntity.getName();
        components.add(name);
        String service = stack.getServiceForComponent(name);
        Set<String> serviceComponents = componentsForService.get(service);
        if (serviceComponents == null) {
          serviceComponents = new HashSet<String>();
          componentsForService.put(service, serviceComponents);
        }
        serviceComponents.add(name);
      }
    }
  }

  /**
   * Provides functionality to update a property value.
   */
  public interface PropertyUpdater {
    /**
     * Update a property value.
     *
     * @param hostGroups  host groups
     * @param origValue   original value of property
     *
     * @return new property value
     */
    public String update(Map<String, HostGroup> hostGroups, String origValue);
  }

  /**
   * Topology based updater which replaces the original host name of a property with the host name
   * which runs the associated (master) component in the new cluster.
   */
  private class SingleHostPropertyUpdater implements PropertyUpdater {
    /**
     * Component name
     */
    private String component;

    /**
     * Constructor.
     *
     * @param component  component name associated with the property
     */
    public SingleHostPropertyUpdater(String component) {
      this.component = component;
    }

    /**
     * Update the property with the new host name which runs the associated component.
     *
     * @param hostGroups  host groups                 host groups
     * @param origValue   original value of property  original property value
     *
     * @return updated property value with old host name replaced by new host name
     */
    public String update(Map<String, HostGroup> hostGroups, String origValue)  {
      Collection<HostGroup> matchingGroups = getHostGroupsForComponent(component, hostGroups.values());
      if (matchingGroups.size() == 1) {
        return origValue.replace("localhost", matchingGroups.iterator().next().getHostInfo().iterator().next());
      } else {
        throw new IllegalArgumentException("Unable to update configuration properties with topology information. " +
            "Component '" + this.component + "' is not mapped to any host group or is mapped to multiple groups.");
      }
    }
  }

  /**
   * Topology based updater which replaces original host names (possibly more than one) contained in a property
   * value with the host names which runs the associated component in the new cluster.
   */
  private class MultipleHostPropertyUpdater implements PropertyUpdater {
    /**
     * Component name
     */
    private String component;

    /**
     * Constructor.
     *
     * @param component  component name associated with the property
     */
    public MultipleHostPropertyUpdater(String component) {
      this.component = component;
    }

    //todo: specific to default values of EXACTLY 'localhost' or 'localhost:port'.
    //todo: when blueprint contains source configurations, these props will contain actual host names, not localhost.
    //todo: currently assuming that all hosts will share the same port
    /**
     * Update all host names included in the original property value with new host names which run the associated
     * component.
     *
     * @param hostGroups  host groups                 host groups
     * @param origValue   original value of property  original value
     *
     * @return updated property value with old host names replaced by new host names
     */
    public String update(Map<String, HostGroup> hostGroups, String origValue) {
      String newValue;
      Collection<HostGroup> matchingGroups = getHostGroupsForComponent(component, hostGroups.values());
      boolean containsPort = origValue.contains(":");

      if (containsPort) {
        String port = origValue.substring(origValue.indexOf(":") + 1);
        StringBuilder sb = new StringBuilder();
        boolean firstHost = true;
        for (HostGroup group : matchingGroups) {
          for (String host : group.getHostInfo()) {
            if (! firstHost) {
              sb.append(',');
            } else {
              firstHost = false;
            }
            sb.append(host);
            sb.append(":");
            sb.append(port);
          }
        }
        newValue = sb.toString();
      } else {
        newValue = matchingGroups.iterator().next().getHostInfo().iterator().next();
      }
      return newValue;
    }
  }

  /**
   * Updater which appends "m" to the original property value.
   * For example, "1024" would be updated to "1024m".
   */
  private class MPropertyUpdater implements PropertyUpdater {
    /**
     * Append 'm' to the original property value if it doesn't already exist.
     *
     * @param hostGroups  host groups                 host groups
     * @param origValue   original value of property  original property value
     *
     * @return property with 'm' appended
     */
    public String update(Map<String, HostGroup> hostGroups, String origValue) {
      return origValue.endsWith("m") ? origValue : origValue + 'm';
    }
  }
}

