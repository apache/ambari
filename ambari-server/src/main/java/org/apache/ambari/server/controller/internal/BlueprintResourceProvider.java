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
import com.google.inject.Inject;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
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
import org.apache.ambari.server.orm.dao.BlueprintDAO;
import org.apache.ambari.server.orm.entities.BlueprintConfigEntity;
import org.apache.ambari.server.orm.entities.BlueprintConfiguration;
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.orm.entities.HostGroupComponentEntity;
import org.apache.ambari.server.orm.entities.HostGroupConfigEntity;
import org.apache.ambari.server.orm.entities.HostGroupEntity;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.PropertyInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Resource Provider for Blueprint resources.
 */
public class BlueprintResourceProvider extends BaseBlueprintProcessor {

  // ----- Property ID constants ---------------------------------------------

  // Blueprints
  protected static final String BLUEPRINT_NAME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Blueprints", "blueprint_name");
  protected static final String STACK_NAME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Blueprints", "stack_name");
  protected static final String STACK_VERSION_PROPERTY_ID =
      PropertyHelper.getPropertyId("Blueprints", "stack_version");

  // Host Groups
  protected static final String HOST_GROUP_PROPERTY_ID = "host_groups";
  protected static final String HOST_GROUP_NAME_PROPERTY_ID = "name";
  protected static final String HOST_GROUP_CARDINALITY_PROPERTY_ID = "cardinality";

  // Host Group Components
  protected static final String COMPONENT_PROPERTY_ID ="components";
  protected static final String COMPONENT_NAME_PROPERTY_ID ="name";

  // Configurations
  protected static final String CONFIGURATION_PROPERTY_ID = "configurations";
  protected static final String PROPERTIES_PROPERTY_ID = "properties";
  protected static final String PROPERTIES_ATTRIBUTES_PROPERTY_ID = "properties_attributes";
  protected static final String SCHEMA_IS_NOT_SUPPORTED_MESSAGE =
      "Configuration format provided in Blueprint is not supported";

  // Primary Key Fields
  private static Set<String> pkPropertyIds =
      new HashSet<String>(Arrays.asList(new String[]{
          BLUEPRINT_NAME_PROPERTY_ID}));

  /**
   * Used to serialize to/from json.
   */
  private static Gson jsonSerializer;


  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds     the property ids
   * @param keyPropertyIds  the key property ids
   * @param controller      management controller
   */
  BlueprintResourceProvider(Set<String> propertyIds,
                            Map<Resource.Type, String> keyPropertyIds,
                            AmbariManagementController controller) {

    super(propertyIds, keyPropertyIds, controller);
  }

  /**
   * Static initialization.
   *
   * @param dao       blueprint data access object
   * @param gson      json serializer
   * @param metaInfo  stack related information
   */
  @Inject
  public static void init(BlueprintDAO dao, Gson gson, AmbariMetaInfo metaInfo) {
    blueprintDAO   = dao;
    jsonSerializer = gson;
    stackInfo      = metaInfo;
  }


  // ----- ResourceProvider ------------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException, UnsupportedPropertyException,
             ResourceAlreadyExistsException, NoSuchParentResourceException {

    for (Map<String, Object> properties : request.getProperties()) {
      createResources(getCreateCommand(properties, request.getRequestInfoProperties()));
    }
    notifyCreate(Resource.Type.Blueprint, request);

    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
             NoSuchResourceException, NoSuchParentResourceException {

    List<BlueprintEntity> results        = null;
    boolean               applyPredicate = false;

    if (predicate != null) {
      Set<Map<String, Object>> requestProps = getPropertyMaps(predicate);
      if (requestProps.size() == 1 ) {
        String name = (String) requestProps.iterator().next().get(
            BLUEPRINT_NAME_PROPERTY_ID);

        if (name != null) {
          BlueprintEntity entity = blueprintDAO.findByName(name);
          results = entity == null ? Collections.<BlueprintEntity>emptyList() :
              Collections.singletonList(entity);
        }
      }
    }

    if (results == null) {
      applyPredicate = true;
      results = blueprintDAO.findAll();
    }

    Set<Resource> resources  = new HashSet<Resource>();
    for (BlueprintEntity entity : results) {
      Resource resource = toResource(entity, getRequestPropertyIds(request, predicate));
      if (predicate == null || ! applyPredicate || predicate.evaluate(resource)) {
        resources.add(resource);
      }
    }

    if (predicate != null && resources.isEmpty()) {
      throw new NoSuchResourceException(
          "The requested resource doesn't exist: Blueprint not found, " + predicate);
    }

    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
             NoSuchResourceException, NoSuchParentResourceException {

    // no-op, blueprints are immutable.  Service doesn't support PUT so should never get here.
    return null;
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
             NoSuchResourceException, NoSuchParentResourceException {

    //TODO (jspeidel): Revisit concurrency control
    Set<Resource> setResources = getResources(
        new RequestImpl(null, null, null, null), predicate);

    for (final Resource resource : setResources) {
      final String blueprintName =
        (String) resource.getPropertyValue(BLUEPRINT_NAME_PROPERTY_ID);

      LOG.info("Deleting Blueprint, name = " + blueprintName);

      modifyResources(new Command<Void>() {
        @Override
        public Void invoke() throws AmbariException {
        blueprintDAO.removeByName(blueprintName);
        return null;
        }
      });
    }

    notifyDelete(Resource.Type.Blueprint, predicate);
    return getRequestStatus(null);
  }


  // ----- Instance Methods ------------------------------------------------

  /**
   * Create a resource instance from a blueprint entity.
   *
   * @param entity        blueprint entity
   * @param requestedIds  requested id's
   *
   * @return a new resource instance for the given blueprint entity
   */
  protected Resource toResource(BlueprintEntity entity, Set<String> requestedIds) {
    Resource resource = new ResourceImpl(Resource.Type.Blueprint);
    setResourceProperty(resource, BLUEPRINT_NAME_PROPERTY_ID, entity.getBlueprintName(), requestedIds);
    setResourceProperty(resource, STACK_NAME_PROPERTY_ID, entity.getStackName(), requestedIds);
    setResourceProperty(resource, STACK_VERSION_PROPERTY_ID, entity.getStackVersion(), requestedIds);

    List<Map<String, Object>> listGroupProps = new ArrayList<Map<String, Object>>();
    Collection<HostGroupEntity> hostGroups = entity.getHostGroups();
    for (HostGroupEntity hostGroup : hostGroups) {
      Map<String, Object> mapGroupProps = new HashMap<String, Object>();
      mapGroupProps.put(HOST_GROUP_NAME_PROPERTY_ID, hostGroup.getName());
      listGroupProps.add(mapGroupProps);
      mapGroupProps.put(HOST_GROUP_CARDINALITY_PROPERTY_ID, hostGroup.getCardinality());

      List<Map<String, String>> listComponentProps = new ArrayList<Map<String, String>>();
      Collection<HostGroupComponentEntity> components = hostGroup.getComponents();
      for (HostGroupComponentEntity component : components) {
        Map<String, String> mapComponentProps = new HashMap<String, String>();
        mapComponentProps.put(COMPONENT_NAME_PROPERTY_ID, component.getName());
        listComponentProps.add(mapComponentProps);
      }
      mapGroupProps.put(COMPONENT_PROPERTY_ID, listComponentProps);
      mapGroupProps.put(CONFIGURATION_PROPERTY_ID, populateConfigurationList(
          hostGroup.getConfigurations()));
    }
    setResourceProperty(resource, HOST_GROUP_PROPERTY_ID, listGroupProps, requestedIds);
    setResourceProperty(resource, CONFIGURATION_PROPERTY_ID,
        populateConfigurationList(entity.getConfigurations()), requestedIds);

    return resource;
  }

  /**
   * Convert a map of properties to a blueprint entity.
   *
   * @param properties  property map
   * @return new blueprint entity
   */
  @SuppressWarnings("unchecked")
  protected BlueprintEntity toBlueprintEntity(Map<String, Object> properties) {
    String name = (String) properties.get(BLUEPRINT_NAME_PROPERTY_ID);
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Blueprint name must be provided");
    }

    BlueprintEntity blueprint = new BlueprintEntity();
    blueprint.setBlueprintName(name);
    blueprint.setStackName((String) properties.get(STACK_NAME_PROPERTY_ID));
    blueprint.setStackVersion((String) properties.get(STACK_VERSION_PROPERTY_ID));

    createHostGroupEntities(blueprint,
        (HashSet<HashMap<String, Object>>) properties.get(HOST_GROUP_PROPERTY_ID));

    createBlueprintConfigEntities((Collection<Map<String, String>>)
        properties.get(CONFIGURATION_PROPERTY_ID), blueprint);

    return blueprint;
  }

  /**
   * Create host group entities and add to the parent blueprint entity.
   *
   * @param blueprint      parent blueprint entity
   * @param setHostGroups  set of host group property maps
   */
  @SuppressWarnings("unchecked")
  private void createHostGroupEntities(BlueprintEntity blueprint,
                                       HashSet<HashMap<String, Object>> setHostGroups) {

    if (setHostGroups == null || setHostGroups.isEmpty()) {
      throw new IllegalArgumentException("At least one host group must be specified in a blueprint");
    }

    Collection<HostGroupEntity> entities = new ArrayList<HostGroupEntity>();
    Collection<String> stackComponentNames = getAllStackComponents(
        blueprint.getStackName(), blueprint.getStackVersion());

    for (HashMap<String, Object> hostGroupProperties : setHostGroups) {
      HostGroupEntity hostGroup = new HostGroupEntity();
      entities.add(hostGroup);

      String hostGroupName = (String) hostGroupProperties.get(HOST_GROUP_NAME_PROPERTY_ID);
      if (hostGroupName == null || hostGroupName.isEmpty()) {
        throw new IllegalArgumentException("Every host group must include a non-null 'name' property");
      }

      hostGroup.setName(hostGroupName);
      hostGroup.setBlueprintEntity(blueprint);
      hostGroup.setBlueprintName(blueprint.getBlueprintName());
      hostGroup.setCardinality((String) hostGroupProperties.get(HOST_GROUP_CARDINALITY_PROPERTY_ID));

      createHostGroupConfigEntities((Collection<Map<String,
          String>>) hostGroupProperties.get(CONFIGURATION_PROPERTY_ID), hostGroup);

      createComponentEntities(hostGroup, (HashSet<HashMap<String, String>>)
          hostGroupProperties.get(COMPONENT_PROPERTY_ID), stackComponentNames);
    }
    blueprint.setHostGroups(entities);
  }

  /**
   * Create component entities and add to parent host group.
   *
   * @param group           parent host group
   * @param setComponents   set of component property maps
   * @param componentNames  set of all component names for the associated stack
   */
  @SuppressWarnings("unchecked")
  private void createComponentEntities(HostGroupEntity group, HashSet<HashMap<String, String>> setComponents,
                                       Collection<String> componentNames) {
    
    Collection<HostGroupComponentEntity> components = new ArrayList<HostGroupComponentEntity>();
    String groupName = group.getName();
    group.setComponents(components);

    if (setComponents == null || setComponents.isEmpty()) {
      throw new IllegalArgumentException("Host group '" + groupName + "' must contain at least one component");
    }

    for (HashMap<String, String> componentProperties : setComponents) {
      HostGroupComponentEntity component = new HostGroupComponentEntity();
      components.add(component);

      String componentName = componentProperties.get(COMPONENT_NAME_PROPERTY_ID);
      if (componentName == null || componentName.isEmpty()) {
        throw new IllegalArgumentException("Host group '" + groupName +
            "' contains a component with no 'name' property");
      }

      if (! componentNames.contains(componentName)) {
        throw new IllegalArgumentException("The component '" + componentName + "' in host group '" +
            groupName + "' is not valid for the specified stack");
      }

      component.setName(componentName);
      component.setBlueprintName(group.getBlueprintName());
      component.setHostGroupEntity(group);
      component.setHostGroupName(group.getName());
    }
    group.setComponents(components);
  }

  /**
   * Obtain all component names for the specified stack.
   *
   * @param stackName     stack name
   * @param stackVersion  stack version
   *
   * @return collection of component names for the specified stack
   * @throws IllegalArgumentException if the specified stack doesn't exist
   */
  private Collection<String> getAllStackComponents(String stackName, String stackVersion) {
    Collection<String> componentNames = new HashSet<String>();
    componentNames.add("AMBARI_SERVER");
    Collection<ComponentInfo> components;
    try {
      components = getComponents(stackName, stackVersion);
    } catch (AmbariException e) {
      throw new IllegalArgumentException("The specified stack doesn't exist.  Name='" +
          stackName + "', Version='" + stackVersion + "'");
    }
    if (components != null) {
      for (ComponentInfo component : components) {
        componentNames.add(component.getName());
      }
    }
    return componentNames;
  }

  /**
   * Get all the components for the specified stack.
   *
   * @param stackName  stack name
   * @param version    stack version
   *
   * @return all components for the specified stack
   * @throws AmbariException if the stack doesn't exist
   */
  private Collection<ComponentInfo> getComponents(String stackName, String version) throws AmbariException {
    Collection<ComponentInfo> components = new HashSet<ComponentInfo>();
    Map<String, ServiceInfo> services = stackInfo.getServices(stackName, version);

    for (ServiceInfo service : services.values()) {
      List<ComponentInfo> serviceComponents = stackInfo.getComponentsByService(
          stackName, version, service.getName());
      for (ComponentInfo component : serviceComponents) {
        components.add(component);
      }
    }
    return components;
  }

  /**
   * Populate a list of configuration property maps from a collection of configuration entities.
   *
   * @param configurations  collection of configuration entities
   *
   * @return list of configuration property maps
   */
  List<Map<String, Map<String, Object>>> populateConfigurationList(
      Collection<? extends BlueprintConfiguration> configurations) {

    List<Map<String, Map<String, Object>>> listConfigurations = new ArrayList<Map<String, Map<String, Object>>>();
    for (BlueprintConfiguration config : configurations) {
      Map<String, Map<String, Object>> mapConfigurations = new HashMap<String, Map<String, Object>>();
      Map<String, Object> configTypeDefinition = new HashMap<String, Object>();
      String type = config.getType();
      Map<String, Object> properties = jsonSerializer.<Map<String, Object>>fromJson(
          config.getConfigData(), Map.class);
      configTypeDefinition.put(PROPERTIES_PROPERTY_ID, properties);
      Map<String, Map<String, String>> attributes = jsonSerializer.<Map<String, Map<String, String>>>fromJson(
          config.getConfigAttributes(), Map.class);
      if (attributes != null && !attributes.isEmpty()) {
        configTypeDefinition.put(PROPERTIES_ATTRIBUTES_PROPERTY_ID, attributes);
      }
      mapConfigurations.put(type, configTypeDefinition);
      listConfigurations.add(mapConfigurations);
    }

    return listConfigurations;
  }

  /**
   * Populate blueprint configurations.
   *
   * @param propertyMaps  collection of configuration property maps
   * @param blueprint     blueprint entity to set configurations on
   */
  void createBlueprintConfigEntities(Collection<Map<String, String>> propertyMaps,
                                             BlueprintEntity blueprint) {

    Collection<BlueprintConfigEntity> configurations = new ArrayList<BlueprintConfigEntity>();
    if (propertyMaps != null) {
      for (Map<String, String> configuration : propertyMaps) {
        BlueprintConfigEntity configEntity = new BlueprintConfigEntity();
        configEntity.setBlueprintEntity(blueprint);
        configEntity.setBlueprintName(blueprint.getBlueprintName());
        populateConfigurationEntity(configuration, configEntity);
        configurations.add(configEntity);
      }
    }
    blueprint.setConfigurations(configurations);
  }

  /**
   * Populate host group configurations.
   *
   * @param propertyMaps  collection of configuration property maps
   * @param hostGroup     host group entity to set configurations on
   */
  private void createHostGroupConfigEntities(Collection<Map<String, String>> propertyMaps,
                                             HostGroupEntity hostGroup) {

    Collection<HostGroupConfigEntity> configurations = new ArrayList<HostGroupConfigEntity>();
    if (propertyMaps != null) {
      for (Map<String, String> configuration : propertyMaps) {
        HostGroupConfigEntity configEntity = new HostGroupConfigEntity();
        configEntity.setHostGroupEntity(hostGroup);
        configEntity.setHostGroupName(hostGroup.getName());
        configEntity.setBlueprintName(hostGroup.getBlueprintName());
        populateConfigurationEntity(configuration, configEntity);
        configurations.add(configEntity);
      }
    }
    hostGroup.setConfigurations(configurations);
  }

  /**
   * Populate a configuration entity from properties.
   *
   * @param configuration  property map
   * @param configEntity   config entity to populate
   */
  void populateConfigurationEntity(Map<String, String> configuration, BlueprintConfiguration configEntity) {
    BlueprintConfigPopulationStrategy p = decidePopulationStrategy(configuration);
    p.applyConfiguration(configuration, configEntity);
  }

  BlueprintConfigPopulationStrategy decidePopulationStrategy(Map<String, String> configuration) {
    if (configuration != null && !configuration.isEmpty()) {
      String keyEntry = configuration.keySet().iterator().next();
      String[] keyNameTokens = keyEntry.split("/");
      int levels = keyNameTokens.length;
      String propertiesType = keyNameTokens[1];
      if (levels == 2) {
        return new BlueprintConfigPopulationStrategyV1();
      } else if ((levels == 3 && PROPERTIES_PROPERTY_ID.equals(propertiesType))
          || (levels == 4 && PROPERTIES_ATTRIBUTES_PROPERTY_ID.equals(propertiesType))) {
        return new BlueprintConfigPopulationStrategyV2();
      } else {
        throw new IllegalArgumentException(SCHEMA_IS_NOT_SUPPORTED_MESSAGE);
      }
    } else {
      return new BlueprintConfigPopulationStrategyV2();
    }
  }

  /**
   * Create a create command with all properties set.
   *
   * @param properties        properties to be applied to blueprint
   * @param requestInfoProps  request info properties
   *
   * @return a new create command
   */
  private Command<Void> getCreateCommand(final Map<String, Object> properties, final Map<String, String> requestInfoProps) {
    return new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        BlueprintEntity blueprint = toBlueprintEntity(properties);

        if (blueprintDAO.findByName(blueprint.getBlueprintName()) != null) {
          throw new DuplicateResourceException(
              "Attempted to create a Blueprint which already exists, blueprint_name=" +
              blueprint.getBlueprintName());
        }
        Map<String, Map<String, Collection<String>>> missingProperties = blueprint.validateConfigurations(
            stackInfo, false);

        if (! missingProperties.isEmpty()) {
          throw new IllegalArgumentException("Required configurations are missing from the specified host groups: " +
                                             missingProperties);
        }

        String validateTopology =  requestInfoProps.get("validate_topology");
        if (validateTopology == null || ! validateTopology.equalsIgnoreCase("false")) {
          validateTopology(blueprint);
        }

        if (LOG.isDebugEnabled()) {
          LOG.debug("Creating Blueprint, name=" + blueprint.getBlueprintName());
        }
        try {
          blueprintDAO.create(blueprint);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        return null;
      }
    };
  }

  /**
   * The structure of blueprints is evolving where multiple resource
   * structures are to be supported. This class abstracts the population
   * of configurations which have changed from a map of key-value strings,
   * to an map containing 'properties' and 'properties_attributes' maps.
   *
   * Extending classes can determine how they want to populate the
   * configuration maps depending on input.
   */
  protected static abstract class BlueprintConfigPopulationStrategy {

    public void applyConfiguration(Map<String, String> configuration, BlueprintConfiguration blueprintConfiguration) {
      Map<String, String> configData = new HashMap<String, String>();
      Map<String, Map<String, String>> configAttributes = new HashMap<String, Map<String, String>>();

      if (configuration != null) {
        for (Map.Entry<String, String> entry : configuration.entrySet()) {
          String absolutePropName = entry.getKey();
          String propertyValue = entry.getValue();
          String[] propertyNameTokens = absolutePropName.split("/");

          if (blueprintConfiguration.getType() == null) {
            blueprintConfiguration.setType(propertyNameTokens[0]);
          }

          addProperty(configData, configAttributes, propertyNameTokens, propertyValue);
        }
      }

      blueprintConfiguration.setConfigData(jsonSerializer.toJson(configData));
      blueprintConfiguration.setConfigAttributes(jsonSerializer.toJson(configAttributes));
    }

    protected abstract void addProperty(Map<String, String> configData,
                                        Map<String, Map<String, String>> configAttributes,
                                        String[] propertyNameTokens, String propertyValue);
  }

  /**
   * Original blueprint configuration format where configs were a map
   * of strings.
   */
  protected static class BlueprintConfigPopulationStrategyV1 extends BlueprintConfigPopulationStrategy {

    @Override
    protected void addProperty(Map<String, String> configData,
                               Map<String, Map<String, String>> configAttributes,
                               String[] propertyNameTokens, String propertyValue) {
      configData.put(propertyNameTokens[1], propertyValue);
    }

  }

  /**
   * New blueprint configuration format where configs are a map from 'properties' and
   * 'properties_attributes' to a map of strings.
   * 
   * @since 1.7.0
   */
  protected static class BlueprintConfigPopulationStrategyV2 extends BlueprintConfigPopulationStrategy {

    @Override
    protected void addProperty(Map<String, String> configData,
                               Map<String, Map<String, String>> configAttributes,
                               String[] propertyNameTokens, String propertyValue) {
      if (PROPERTIES_PROPERTY_ID.equals(propertyNameTokens[1])) {
        configData.put(propertyNameTokens[2], propertyValue);
      } else if (PROPERTIES_ATTRIBUTES_PROPERTY_ID.equals(propertyNameTokens[1])) {
        addConfigAttribute(configAttributes, propertyNameTokens, propertyValue);
      }
    }

    private void addConfigAttribute(Map<String, Map<String, String>> configDependencyProperties,
                                    String[] propertyNameTokens, String value) {
      if (!configDependencyProperties.containsKey(propertyNameTokens[2])) {
        configDependencyProperties.put(propertyNameTokens[2], new HashMap<String, String>());
      }
      Map<String, String> propertiesGroup = configDependencyProperties.get(propertyNameTokens[2]);
      propertiesGroup.put(propertyNameTokens[3], value);
    }
  }
}
