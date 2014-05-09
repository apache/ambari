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

import com.google.gson.Gson;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.PersistKeyValueService;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ClusterResponse;
import org.apache.ambari.server.controller.ConfigGroupRequest;
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
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.BlueprintDAO;
import org.apache.ambari.server.orm.entities.BlueprintConfigEntity;
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.orm.entities.HostGroupComponentEntity;
import org.apache.ambari.server.orm.entities.HostGroupConfigEntity;
import org.apache.ambari.server.orm.entities.HostGroupEntity;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigImpl;
import org.apache.ambari.server.state.PropertyInfo;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
      new HashSet<String>(Arrays.asList(new String[]{CLUSTER_ID_PROPERTY_ID}));

  /**
   * Data access object used to obtain blueprint entities.
   */
  private static BlueprintDAO blueprintDAO;

  /**
   * Stack related information.
   */
  private static AmbariMetaInfo stackInfo;

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
   * Inject the blueprint data access object which is used to obtain blueprint entities.
   *
   * @param dao  blueprint data access object
   */
  public static void init(BlueprintDAO dao, AmbariMetaInfo metaInfo) {
    blueprintDAO = dao;
    stackInfo    = metaInfo;
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
    baseUnsupported.remove("host_groups");
    baseUnsupported.remove("default_password");
    baseUnsupported.remove("configurations");

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
  @SuppressWarnings("unchecked")
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
    processConfigurations(processBlueprintConfigurations(blueprint, (Collection<Map<String, String>>)
        properties.get("configurations")), stack, blueprintHostGroups);
    validatePasswordProperties(blueprint, blueprintHostGroups, (String) properties.get("default_password"));

    String clusterName = (String) properties.get(CLUSTER_NAME_PROPERTY_ID);
    createClusterResource(buildClusterResourceProperties(stack, clusterName));
    setConfigurationsOnCluster(clusterName);

    Set<String> services = getServicesToDeploy(stack, blueprintHostGroups);

    createServiceAndComponentResources(blueprintHostGroups, clusterName, services);
    createHostAndComponentResources(blueprintHostGroups, clusterName);

    registerConfigGroups(clusterName, blueprintHostGroups, stack);

    persistInstallStateForUI();
    return ((ServiceResourceProvider) getResourceProvider(Resource.Type.Service)).
        installAndStart(clusterName);
  }

  /**
   * Validate that all required password properties have been set or that 'default_password' is specified.
   *
   * @param blueprint        associated blueprint entity
   * @param hostGroups       host groups in blueprint
   * @param defaultPassword  specified default password, may be null
   *
   * @throws IllegalArgumentException if required password properties are missing and no
   *                                  default is specified via 'default_password'
   */
  private void validatePasswordProperties(BlueprintEntity blueprint, Map<String, HostGroup> hostGroups,
                                          String defaultPassword) {

    Map<String, Map<String, Collection<String>>> missingPasswords = blueprint.validateConfigurations(
        stackInfo, PropertyInfo.PropertyType.PASSWORD);

    Iterator<Map.Entry<String, Map<String, Collection<String>>>> iter;
    for(iter = missingPasswords.entrySet().iterator(); iter.hasNext(); ) {
      Map.Entry<String, Map<String, Collection<String>>> entry = iter.next();
      Map<String, Collection<String>> missingProps = entry.getValue();
      Iterator<Map.Entry<String, Collection<String>>> hostGroupIter;

      for (hostGroupIter = missingProps.entrySet().iterator(); hostGroupIter.hasNext(); ) {
        Map.Entry<String, Collection<String>> hgEntry = hostGroupIter.next();
        String configType = hgEntry.getKey();
        Collection<String> propertySet = hgEntry.getValue();

        for (Iterator<String> propIter = propertySet.iterator(); propIter.hasNext(); ) {
          String property = propIter.next();
          if (isPropertyInConfiguration(mapClusterConfigurations.get(configType), property)){
              propIter.remove();
          } else {
            HostGroup hg = hostGroups.get(entry.getKey());
            if (hg != null && isPropertyInConfiguration(hg.getConfigurations().get(configType), property)) {
              propIter.remove();
            }  else if (setDefaultPassword(defaultPassword, configType, property)) {
              propIter.remove();
            }
          }
        }
        if (propertySet.isEmpty()) {
          hostGroupIter.remove();
        }
      }
      if (entry.getValue().isEmpty()) {
        iter.remove();
      }
    }

    if (! missingPasswords.isEmpty()) {
      throw new IllegalArgumentException("Missing required password properties.  Specify a value for these " +
          "properties in the cluster or host group configurations or include 'default_password' field in request. " +
          missingPasswords);
    }
  }

  /**
   * Attempt to set the default password in cluster configuration for missing password property.
   *
   * @param defaultPassword  default password specified in request, may be null
   * @param configType       configuration type
   * @param property         password property name
   *
   * @return true if password was set, otherwise false.  Currently the password will always be set
   *         unless it is null
   */
  private boolean setDefaultPassword(String defaultPassword, String configType, String property) {
    boolean setDefaultPassword = false;
    Map<String, String> typeProps = mapClusterConfigurations.get(configType);
    if (defaultPassword != null && ! defaultPassword.trim().isEmpty()) {
      // set default password in cluster config
      if (typeProps == null) {
        typeProps = new HashMap<String, String>();
        mapClusterConfigurations.put(configType, typeProps);
      }
      typeProps.put(property, defaultPassword);
      setDefaultPassword = true;
    }
    return setDefaultPassword;
  }

  /**
   * Determine if a specific property is in a configuration.
   *
   * @param props     property map to check
   * @param property  property to check for
   *
   * @return true if the property is contained in the configuration, otherwise false
   */
  private boolean isPropertyInConfiguration(Map<String, String> props, String property) {
    boolean foundProperty = false;
    if (props != null) {
      String val = props.get(property);
      foundProperty = (val != null && ! val.trim().isEmpty());
    }
    return foundProperty;
  }

  /**
   * Override existing properties or add new.
   *
   * @param existingProperties  property overrides
   * @param configOverrides     current property values
   */
  private void overrideExistingProperties(Map<String, Map<String, String>> existingProperties,
                                          Collection<Map<String, String>> configOverrides) {
    if (configOverrides != null) {
      for (Map<String, String> properties : configOverrides) {
        String category = null;
        int propertyOffset = -1;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
          String absolutePropName = entry.getKey();
          if (category == null) {
            propertyOffset =  absolutePropName.indexOf('/');
            category = absolutePropName.substring(0, propertyOffset);
          }
          Map<String, String> existingCategoryProperties = existingProperties.get(category);
          if (existingCategoryProperties == null) {
            existingCategoryProperties = new HashMap<String, String>();
            existingProperties.put(category, existingCategoryProperties);
          }
          //override existing property or add new
          existingCategoryProperties.put(absolutePropName.substring(propertyOffset + 1), entry.getValue());
        }
      }
    }
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
    getResourceProvider(Resource.Type.Service).createResources(serviceRequest);
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
  private void createHostAndComponentResources(Map<String, HostGroup> blueprintHostGroups, String clusterName)
      throws SystemException, UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {

    ResourceProvider hostProvider = getResourceProvider(Resource.Type.Host);
    ResourceProvider hostComponentProvider = getResourceProvider(Resource.Type.HostComponent);
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
      ResourceProvider componentProvider = getResourceProvider(Resource.Type.Component);
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
   * @throws IllegalArgumentException a host_group in the request doesn't match a host-group in the blueprint
   */
  @SuppressWarnings("unchecked")
  private void applyRequestInfoToHostGroups(Map<String, Object> properties,
                                            Map<String, HostGroup> blueprintHostGroups)
                                            throws IllegalArgumentException {

    @SuppressWarnings("unchecked")
    Collection<Map<String, Object>> hostGroups =
        (Collection<Map<String, Object>>) properties.get("host_groups");

    if (hostGroups == null || hostGroups.isEmpty()) {
      throw new IllegalArgumentException("'host_groups' element must be included in cluster create body");
    }

    // iterate over host groups provided in request body
    for (Map<String, Object> hostGroupProperties : hostGroups) {
      String name = (String) hostGroupProperties.get("name");
      if (name == null || name.isEmpty()) {
        throw new IllegalArgumentException("Every host_group must include a non-null 'name' property");
      }
      HostGroup hostGroup = blueprintHostGroups.get(name);

      if (hostGroup == null) {
        throw new IllegalArgumentException("Invalid host_group specified: " + name +
          ".  All request host groups must have a corresponding host group in the specified blueprint");
      }

      Collection hosts = (Collection) hostGroupProperties.get("hosts");
      if (hosts == null || hosts.isEmpty()) {
        throw new IllegalArgumentException("Host group '" + name + "' must contain a 'hosts' element");
      }
      for (Object oHost : hosts) {
        Map<String, String> mapHostProperties = (Map<String, String>) oHost;
        //add host information to host group
        String fqdn = mapHostProperties.get("fqdn");
        if (fqdn == null || fqdn.isEmpty()) {
          throw new IllegalArgumentException("Host group '" + name + "' hosts element must include at least one fqdn");
        }
        hostGroup.addHostInfo(fqdn);
      }
      Map<String, Map<String, String>> existingConfigurations = hostGroup.getConfigurations();
      overrideExistingProperties(existingConfigurations, (Collection<Map<String, String>>)
          hostGroupProperties.get("configurations"));

    }
    validateHostMappings(blueprintHostGroups);
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
   * Process configurations contained in blueprint.
   *
   * @param blueprint  blueprint entity
   *
   * @return configuration properties contained within in blueprint
   */
  private Map<String, Map<String, String>> processBlueprintConfigurations(BlueprintEntity blueprint,
                                                                          Collection<Map<String, String>> configOverrides) {
    Map<String, Map<String, String>> mapConfigurations = new HashMap<String, Map<String, String>>();
    Collection<BlueprintConfigEntity> configs = blueprint.getConfigurations();
    Gson jsonSerializer = new Gson();

    for (BlueprintConfigEntity config : configs) {
      mapConfigurations.put(config.getType(), jsonSerializer.<Map<String, String>> fromJson(
          config.getConfigData(), Map.class));
    }
    overrideExistingProperties(mapConfigurations, configOverrides);

    return mapConfigurations;
  }

  /**
   * Process cluster configurations.  This includes obtaining the default configuration properties
   * from the stack,overlaying configuration properties specified in the blueprint and cluster
   * create request and updating properties with topology specific information.
   *
   * @param stack                associated stack
   * @param blueprintHostGroups  host groups contained in the blueprint
   */
  private void processConfigurations(Map<String, Map<String, String>> blueprintConfigurations,
                                    Stack stack, Map<String, HostGroup> blueprintHostGroups)  {

    for (String service : getServicesToDeploy(stack, blueprintHostGroups)) {
      for (String type : stack.getConfigurationTypes(service)) {
        Map<String, String> typeProps = mapClusterConfigurations.get(type);
        if (typeProps == null) {
          typeProps = new HashMap<String, String>();
          mapClusterConfigurations.put(type, typeProps);
        }
        typeProps.putAll(stack.getConfigurationProperties(service, type));
      }
    }
    processBlueprintClusterConfigurations(blueprintConfigurations);

    for (Map.Entry<String, Map<String, String>> entry : mapClusterConfigurations.entrySet()) {
      for (Map.Entry<String, String> propertyEntry : entry.getValue().entrySet()) {
        String propName = propertyEntry.getKey();
        // see if property needs to be updated
        PropertyUpdater propertyUpdater = propertyUpdaters.get(propName);
        if (propertyUpdater != null) {
          propertyEntry.setValue(propertyUpdater.update(blueprintHostGroups, propertyEntry.getValue()));
        }
      }
    }
    setMissingConfigurations();
  }

  /**
   * Process cluster scoped configurations provided in blueprint.
   *
   * @param blueprintConfigurations  map of blueprint configurations keyed by type
   */
  private void processBlueprintClusterConfigurations(Map<String, Map<String, String>> blueprintConfigurations) {
    for (Map.Entry<String, Map<String, String>> entry : blueprintConfigurations.entrySet()) {
      Map<String, String> properties = entry.getValue();
      if (properties != null && !properties.isEmpty()) {
        String type = entry.getKey();
        Map<String, String> typeProps = mapClusterConfigurations.get(type);
        if (typeProps == null) {
          typeProps = new HashMap<String, String>();
          mapClusterConfigurations.put(type, typeProps);
        }
        // override default properties
        typeProps.putAll(properties);
      }
    }
  }

  /**
   * Explicitly set any properties that are required but not currently provided in the stack definition.
   */
  private void setMissingConfigurations() {
    // AMBARI-4921
    ensureProperty("global", "user_group", "hadoop");
    ensureProperty("global", "nagios_contact", "default@REPLACEME.NOWHERE");
    ensureProperty("global", "nagios_web_password", "admin");
    ensureProperty("global", "smokeuser", "ambari-qa");

    // AMBARI-5206
    Map<String, String> globalConfig = mapClusterConfigurations.get("global");
    String[] userProps = {"oozie_user", "hive_user", "hcat_user", "hbase_user", "falcon_user"};
    String proxyUserHosts  = "hadoop.proxyuser.%s.hosts";
    String proxyUserGroups = "hadoop.proxyuser.%s.groups";

    for (String userProp : userProps) {
      String user = globalConfig.get(userProp);
      if (user != null && !user.isEmpty()) {
        ensureProperty("core-site", String.format(proxyUserHosts, user), "*");
        ensureProperty("core-site", String.format(proxyUserGroups, user), "users");
      }
    }
  }

  /**
   * Ensure that the specified property exists.
   * If not, set a default value.
   *
   * @param type          config type
   * @param property      property name
   * @param defaultValue  default value
   */
  private void ensureProperty(String type, String property, String defaultValue) {
    Map<String, String> properties = mapClusterConfigurations.get(type);
    if (properties == null) {
      properties = new HashMap<String, String>();
      mapClusterConfigurations.put(type, properties);
    }

    if (! properties.containsKey(property)) {
      properties.put(property, defaultValue);
    }
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
    propertyUpdaters.put("javax.jdo.option.ConnectionURL", new SingleHostPropertyUpdater("MYSQL_SERVER"));

    // OOZIE_SERVER
    propertyUpdaters.put("oozie.base.url", new SingleHostPropertyUpdater("OOZIE_SERVER"));
    propertyUpdaters.put("oozie_ambari_host", new SingleHostPropertyUpdater("OOZIE_SERVER"));

    // ZOOKEEPER_SERVER
    propertyUpdaters.put("hbase.zookeeper.quorum", new MultipleHostPropertyUpdater("ZOOKEEPER_SERVER"));
    propertyUpdaters.put("templeton.zookeeper.hosts", new MultipleHostPropertyUpdater("ZOOKEEPER_SERVER"));

    // STORM
    propertyUpdaters.put("nimbus.host", new SingleHostPropertyUpdater("NIMBUS"));
    propertyUpdaters.put("worker.childopts", new SingleHostPropertyUpdater("GANGLIA_SERVER"));
    propertyUpdaters.put("supervisor.childopts", new SingleHostPropertyUpdater("GANGLIA_SERVER"));
    propertyUpdaters.put("nimbus.childopts", new SingleHostPropertyUpdater("GANGLIA_SERVER"));
    propertyUpdaters.put("storm.zookeeper.servers",
      new YamlMultiValuePropertyDecorator(new MultipleHostPropertyUpdater("ZOOKEEPER_SERVER")));

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

  /**
   * Register config groups for host group scoped configuration.
   * For each host group with configuration specified in the blueprint, a config group is created
   * and the hosts associated with the host group are assigned to the config group.
   *
   * @param clusterName  name of cluster
   * @param hostGroups   map of host group name to host group
   * @param stack        associated stack information
   *
   * @throws ResourceAlreadyExistsException attempt to create a config group that already exists
   * @throws SystemException                an unexpected exception occurs
   * @throws UnsupportedPropertyException   an invalid property is provided when creating a config group
   * @throws NoSuchParentResourceException  attempt to create a config group for a non-existing cluster
   */
  private void registerConfigGroups(String clusterName, Map<String, HostGroup> hostGroups, Stack stack) throws
      ResourceAlreadyExistsException, SystemException,
      UnsupportedPropertyException, NoSuchParentResourceException {

    for (HostGroup group : hostGroups.values()) {
      HostGroupEntity entity = group.getEntity();
      Map<String, Map<String, Config>> groupConfigs = new HashMap<String, Map<String, Config>>();
      for (Map.Entry<String, Map<String, String>> entry: group.getConfigurations().entrySet()) {
        String type = entry.getKey();
        String service = stack.getServiceForConfigType(type);
        Config config = new ConfigImpl(type);
        config.setVersionTag(entity.getName());
        config.setProperties(entry.getValue());
        Map<String, Config> serviceConfigs = groupConfigs.get(service);
        if (serviceConfigs == null) {
          serviceConfigs = new HashMap<String, Config>();
          groupConfigs.put(service, serviceConfigs);
        }
        serviceConfigs.put(type, config);
      }

      for (Map.Entry<String, Map<String, Config>> entry : groupConfigs.entrySet()) {
        String service = entry.getKey();
        Map<String, Config> serviceConfigs = entry.getValue();
        ConfigGroupRequest request = new ConfigGroupRequest(
            null, clusterName, entity.getName(), service, "Host Group Configuration",
            new HashSet<String>(group.getHostInfo()), serviceConfigs);

        ((ConfigGroupResourceProvider) getResourceProvider(Resource.Type.ConfigGroup)).
            createResources(Collections.singleton(request));
      }
    }
  }

  /**
   * Validate that a host is only mapped to a single host group.
   *
   * @param hostGroups map of host group name to host group
   */
  private void validateHostMappings(Map<String, HostGroup> hostGroups) {
    Collection<String> mappedHosts = new HashSet<String>();
    Collection<String> flaggedHosts = new HashSet<String>();

    for (HostGroup hostgroup : hostGroups.values()) {
      for (String host : hostgroup.getHostInfo()) {
        if (mappedHosts.contains(host)) {
          flaggedHosts.add(host);
        } else {
          mappedHosts.add(host);
        }
      }
    }

    if (! flaggedHosts.isEmpty())  {
      throw new IllegalArgumentException("A host may only be mapped to a single host group at this time." +
                                         "  The following hosts are mapped to more than one host group: " +
                                         flaggedHosts);
    }
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
     * Obtain the service name which corresponds to the specified configuration.
     *
     * @param config  configuration type
     *
     * @return name of service which corresponds to the specified configuration type
     */
    public String getServiceForConfigType(String config) {
      for (Map.Entry<String, Map<String, Map<String, String>>> entry : serviceConfigurations.entrySet()) {
        Map<String, Map<String, String>> typeMap = entry.getValue();
        if (typeMap.containsKey(config)) {
          return entry.getKey();
        }
      }
      throw new IllegalArgumentException(
          "Specified configuration type is not associated with any service: " + config);
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
      Map<String, Map<String, String>> mapServiceConfig = new HashMap<String, Map<String, String>>();

      serviceConfigurations.put(service, mapServiceConfig);

      Set<StackConfigurationResponse> serviceConfigs =
        getManagementController().getStackConfigurations(
          Collections.singleton(new StackConfigurationRequest(name, version, service, null)
        ));

      for (StackConfigurationResponse config : serviceConfigs) {
        String type = config.getType();
        //strip .xml from type
        if (type.endsWith(".xml")) {
          type = type.substring(0, type.length() - 4);
        }
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
  protected class HostGroup {
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
     * Map of host group configurations.
     * Type -> Map<Key, Val>
     */
    private Map<String, Map<String, String>> configurations =
        new HashMap<String, Map<String, String>>();

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
      parseConfigurations();
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
     * Get the configurations associated with the host group.
     *
     * @return map of configuration type to a map of properties
     */
    public Map<String, Map<String, String>> getConfigurations() {
      return configurations;
    }

    /**
     * Get the associated entity.
     *
     * @return  associated host group entity
     */
    public HostGroupEntity getEntity() {
      return hostGroup;
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

    /**
     * Parse host group configurations.
     */
    private void parseConfigurations() {
      Gson jsonSerializer = new Gson();
      for (HostGroupConfigEntity configEntity : hostGroup.getConfigurations()) {
        String type = configEntity.getType();
        Map<String, String> typeProperties = configurations.get(type);
        if ( typeProperties == null) {
          typeProperties = new HashMap<String, String>();
          configurations.put(type, typeProperties);
        }
        configurations.put(type, jsonSerializer.<Map<String, String>>fromJson(
            configEntity.getConfigData(), Map.class));
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
     * Separator for multiple property values
     */
    private Character separator = ',';

    /**
     * Constructor.
     *
     * @param component  component name associated with the property
     */
    public MultipleHostPropertyUpdater(String component) {
      this.component = component;
    }

    /**
     * Constructor with customized separator.
     * @param component Component name
     * @param separator separator character
     */
    public MultipleHostPropertyUpdater(String component, Character separator) {
      this.component = component;
      this.separator = separator;
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
      Collection<HostGroup> matchingGroups = getHostGroupsForComponent(component, hostGroups.values());
      boolean containsPort = origValue.contains(":");
      String port = null;
      if (containsPort) {
        port = origValue.substring(origValue.indexOf(":") + 1);
      }
      StringBuilder sb = new StringBuilder();
      boolean firstHost = true;
      for (HostGroup group : matchingGroups) {
        for (String host : group.getHostInfo()) {
          if (!firstHost) {
            sb.append(separator);
          } else {
            firstHost = false;
          }
          sb.append(host);
          if (containsPort) {
            sb.append(":");
            sb.append(port);
          }
        }
      }

      return sb.toString();
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

  /**
   * Class to facilitate special formatting needs of property values.
   */
  private abstract class AbstractPropertyValueDecorator implements PropertyUpdater {
    PropertyUpdater propertyUpdater;

    public AbstractPropertyValueDecorator(PropertyUpdater propertyUpdater) {
      this.propertyUpdater = propertyUpdater;
    }

    /**
     * Return decorated form of the updated input property value.
     * @param hostGroupMap Map of host group name to HostGroup
     * @param origValue   original value of property
     *
     * @return Formatted output string
     */
    @Override
    public String update(Map<String, HostGroup> hostGroupMap, String origValue) {
      return doFormat(propertyUpdater.update(hostGroupMap, origValue));
    }

    /**
     * Transform input string to required output format.
     * @param originalValue Original value of property
     * @return Formatted output string
     */
    public abstract String doFormat(String originalValue);
  }

  /**
   * Return properties of the form ['value']
   */
  private class YamlMultiValuePropertyDecorator extends AbstractPropertyValueDecorator {

    public YamlMultiValuePropertyDecorator(PropertyUpdater propertyUpdater) {
      super(propertyUpdater);
    }

    /**
     * Format input String of the form, str1,str2 to ['str1','str2']
     * @param origValue Input string
     * @return Formatted string
     */
    @Override
    public String doFormat(String origValue) {
      StringBuilder sb = new StringBuilder();
      if (origValue != null) {
        sb.append("[");
        boolean isFirst = true;
        for (String value : origValue.split(",")) {
          if (!isFirst) {
            sb.append(",");
          } else {
            isFirst = false;
          }
          sb.append("'");
          sb.append(value);
          sb.append("'");
        }
        sb.append("]");
      }
      return sb.toString();
    }
  }
}

