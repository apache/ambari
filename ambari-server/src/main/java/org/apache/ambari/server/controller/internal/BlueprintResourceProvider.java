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
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.orm.entities.HostGroupComponentEntity;
import org.apache.ambari.server.orm.entities.HostGroupEntity;

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
public class BlueprintResourceProvider extends AbstractResourceProvider {

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

  // Primary Key Fields
  private static Set<String> pkPropertyIds =
      new HashSet<String>(Arrays.asList(new String[]{
          BLUEPRINT_NAME_PROPERTY_ID}));

  /**
   * Blueprint data access object.
   */
  private static BlueprintDAO dao;

  private static Gson jsonSerializer;


  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds     the property ids
   * @param keyPropertyIds  the key property ids
   */
  BlueprintResourceProvider(Set<String> propertyIds, Map<Resource.Type, String> keyPropertyIds) {
    super(propertyIds, keyPropertyIds);
  }

  /**
   * Static initialization.
   *
   * @param blueprintDAO  blueprint data access object
   * @param gson          gson json serializer
   */
  @Inject
  public static void init(BlueprintDAO blueprintDAO, Gson gson) {
    dao            = blueprintDAO;
    jsonSerializer = gson;
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
      createResources(getCreateCommand(properties));
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
          BlueprintEntity entity = dao.findByName(name);
          results = entity == null ? Collections.<BlueprintEntity>emptyList() :
              Collections.singletonList(entity);
        }
      }
    }

    if (results == null) {
      applyPredicate = true;
      results = dao.findAll();
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

    // no-op, blueprints are immutable
    //todo: meaningful error message
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
      if (LOG.isDebugEnabled()) {
        LOG.debug("Deleting Blueprint, name=" +
            resource.getPropertyValue(BLUEPRINT_NAME_PROPERTY_ID));
      }
      modifyResources(new Command<Void>() {
        @Override
        public Void invoke() throws AmbariException {
          dao.remove(toEntity(resource));
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
    }
    setResourceProperty(resource, HOST_GROUP_PROPERTY_ID, listGroupProps, requestedIds);

    List<Map<String, Object>> listConfigurations = new ArrayList<Map<String, Object>>();
    Collection<BlueprintConfigEntity> configurations = entity.getConfigurations();
    for (BlueprintConfigEntity config : configurations) {
      Map<String, Object> mapConfigurations = new HashMap<String, Object>();
      String type = config.getType();
      Map<String, String> properties = jsonSerializer.<Map<String, String>>fromJson(
          config.getConfigData(), Map.class);
      mapConfigurations.put(type, properties);
      listConfigurations.add(mapConfigurations);
    }
    setResourceProperty(resource, CONFIGURATION_PROPERTY_ID, listConfigurations, requestedIds);

    return resource;
  }

  /**
   * Convert a resource to a blueprint entity.
   *
   * @param resource the resource to convert
   * @return  a new blueprint entity
   */
  @SuppressWarnings("unchecked")
  protected BlueprintEntity toEntity(Resource resource) {
    BlueprintEntity entity = new BlueprintEntity();
    entity.setBlueprintName((String) resource.getPropertyValue(BLUEPRINT_NAME_PROPERTY_ID));
    entity.setStackName((String) resource.getPropertyValue(STACK_NAME_PROPERTY_ID));
    entity.setStackVersion((String) resource.getPropertyValue(STACK_VERSION_PROPERTY_ID));

    Collection<HostGroupEntity> blueprintHostGroups = new ArrayList<HostGroupEntity>();
    entity.setHostGroups(blueprintHostGroups);

    Collection<Map<String, Object>> hostGroupProps = (Collection<Map<String, Object>>)
        resource.getPropertyValue(HOST_GROUP_PROPERTY_ID);

    for (Map<String, Object> properties : hostGroupProps) {
      HostGroupEntity group = new HostGroupEntity();
      group.setName((String) properties.get(BlueprintResourceProvider.HOST_GROUP_NAME_PROPERTY_ID));
      group.setBlueprintEntity(entity);
      group.setBlueprintName(entity.getBlueprintName());
      group.setCardinality((String) properties.get(HOST_GROUP_CARDINALITY_PROPERTY_ID));

      Collection<HostGroupComponentEntity> hostGroupComponents = new ArrayList<HostGroupComponentEntity>();
      group.setComponents(hostGroupComponents);

      List<Map<String, String>> listComponents = (List<Map<String, String>>)
          properties.get(BlueprintResourceProvider.COMPONENT_PROPERTY_ID);

      for (Map<String, String> componentProperties : listComponents) {
        HostGroupComponentEntity component = new HostGroupComponentEntity();
        component.setName(componentProperties.get(COMPONENT_NAME_PROPERTY_ID));
        component.setBlueprintName(entity.getBlueprintName());
        component.setHostGroupEntity(group);
        component.setHostGroupName((String) properties.get(HOST_GROUP_NAME_PROPERTY_ID));

        hostGroupComponents.add(component);
      }
      blueprintHostGroups.add(group);
    }

    entity.setConfigurations(createConfigEntities(
        (Collection<Map<String, String>>) resource.getPropertyValue(CONFIGURATION_PROPERTY_ID), entity));

    return entity;
  }

  /**
   * Convert a map of properties to a blueprint entity.
   *
   * @param properties  property map
   * @return new blueprint entity
   */
  @SuppressWarnings("unchecked")
  protected BlueprintEntity toEntity(Map<String, Object> properties) {
    String name = (String) properties.get(BLUEPRINT_NAME_PROPERTY_ID);
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Blueprint name must be provided");
    }

    BlueprintEntity blueprint = new BlueprintEntity();
    blueprint.setBlueprintName(name);
    blueprint.setStackName((String) properties.get(STACK_NAME_PROPERTY_ID));
    blueprint.setStackVersion((String) properties.get(STACK_VERSION_PROPERTY_ID));

    Collection<HostGroupEntity> blueprintHostGroups = new ArrayList<HostGroupEntity>();
    blueprint.setHostGroups(blueprintHostGroups);

    HashSet<HashMap<String, Object>> setHostGroups =
        (HashSet<HashMap<String, Object>>) properties.get(HOST_GROUP_PROPERTY_ID);

    for (HashMap<String, Object> hostGroupProperties : setHostGroups) {
      HostGroupEntity group = new HostGroupEntity();
      group.setName((String) hostGroupProperties.get(HOST_GROUP_NAME_PROPERTY_ID));
      group.setBlueprintEntity(blueprint);
      group.setBlueprintName(name);
      group.setCardinality((String) hostGroupProperties.get(HOST_GROUP_CARDINALITY_PROPERTY_ID));

      Collection<HostGroupComponentEntity> components = new ArrayList<HostGroupComponentEntity>();
      group.setComponents(components);

      HashSet<HashMap<String, String>> setComponents =
          (HashSet<HashMap<String, String>>) hostGroupProperties.get(COMPONENT_PROPERTY_ID);
      for (HashMap<String, String> componentProperties : setComponents) {
        HostGroupComponentEntity component = new HostGroupComponentEntity();
        component.setName(componentProperties.get(COMPONENT_NAME_PROPERTY_ID));
        component.setBlueprintName(name);
        component.setHostGroupEntity(group);
        component.setHostGroupName((String) hostGroupProperties.get(HOST_GROUP_NAME_PROPERTY_ID));

        components.add(component);
      }
      blueprintHostGroups.add(group);
    }

    blueprint.setConfigurations(createConfigEntities(
        (Collection<Map<String, String>>) properties.get(CONFIGURATION_PROPERTY_ID), blueprint));

    return blueprint;
  }

  /**
   * Create blueprint configuration entities from properties.
   *
   * @param setConfigurations  set of property maps
   * @param blueprint          blueprint entity
   *
   * @return collection of blueprint config entities
   */
  private Collection<BlueprintConfigEntity> createConfigEntities(Collection<Map<String, String>> setConfigurations,
                                                                 BlueprintEntity blueprint) {

    Collection<BlueprintConfigEntity> configurations = new ArrayList<BlueprintConfigEntity>();
    if (setConfigurations != null) {
      for (Map<String, String> configuration : setConfigurations) {
        BlueprintConfigEntity configEntity = new BlueprintConfigEntity();
        configEntity.setBlueprintEntity(blueprint);
        configEntity.setBlueprintName(blueprint.getBlueprintName());
        Map<String, String> configData = new HashMap<String, String>();

        for (Map.Entry<String, String> entry : configuration.entrySet()) {
          String absolutePropName = entry.getKey();

          int idx = absolutePropName.indexOf('/');
          if (configEntity.getType() == null) {
            configEntity.setType(absolutePropName.substring(0, idx));
          }
          configData.put(absolutePropName.substring(idx + 1), entry.getValue());
        }
        configEntity.setConfigData(jsonSerializer.toJson(configData));
        configurations.add(configEntity);
      }
    }

    return configurations;
  }

  /**
   * Create a create command with all properties set.
   *
   * @param properties  properties to be applied to blueprint
   *
   * @return a new create command
   */
  private Command<Void> getCreateCommand(final Map<String, Object> properties) {
    return new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        BlueprintEntity blueprint = toEntity(properties);

        if (LOG.isDebugEnabled()) {
          LOG.debug("Creating Blueprint, name=" + blueprint.getBlueprintName());
        }

        if (dao.findByName(blueprint.getBlueprintName()) != null) {
          throw new DuplicateResourceException(
              "Attempted to create a Blueprint which already exists, blueprint_name=" +
              blueprint.getBlueprintName());
        }
        dao.create(blueprint);
        return null;
      }
    };
  }
}
