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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.apache.ambari.server.orm.dao.BlueprintV2DAO;
import org.apache.ambari.server.orm.entities.BlueprintV2Entity;
import org.apache.ambari.server.topology.BlueprintV2;
import org.apache.ambari.server.topology.BlueprintV2Factory;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.ambari.server.topology.SecurityConfigurationFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;


/**
 * Resource Provider for Blueprint resources.
 */
public class BlueprintV2ResourceProvider extends AbstractControllerResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  // Blueprints
  public static final String BLUEPRINT_NAME_PROPERTY_ID =
    PropertyHelper.getPropertyId("Blueprints", "blueprint_name");

  public static final String BLUEPRINT_SECURITY_PROPERTY_ID =
    PropertyHelper.getPropertyId("Blueprints", "security");

  public static final String BLUEPRINTS_PROPERTY_ID = "Blueprints";

  // Host Groups
  public static final String HOST_GROUP_PROPERTY_ID = "host_groups";
  public static final String HOST_GROUP_NAME_PROPERTY_ID = "name";
  public static final String HOST_GROUP_CARDINALITY_PROPERTY_ID = "cardinality";

  // Host Group Components
  public static final String COMPONENT_PROPERTY_ID ="components";
  public static final String COMPONENT_NAME_PROPERTY_ID ="name";
  public static final String COMPONENT_PROVISION_ACTION_PROPERTY_ID = "provision_action";

  // Configurations
  public static final String CONFIGURATION_PROPERTY_ID = "configurations";


  // Setting
  public static final String SETTING_PROPERTY_ID = "settings";
  public static final String CLUSTER_SETTING_PROPERTY_ID = "cluster_settings";

  public static final String PROPERTIES_PROPERTY_ID = "properties";
  public static final String PROPERTIES_ATTRIBUTES_PROPERTY_ID = "properties_attributes";
  public static final String SCHEMA_IS_NOT_SUPPORTED_MESSAGE =
    "Configuration format provided in Blueprint is not supported";
  public static final String REQUEST_BODY_EMPTY_ERROR_MESSAGE =
    "Request body for Blueprint create request is empty";
  public static final String CONFIGURATION_LIST_CHECK_ERROR_MESSAGE =
    "Configurations property must be a List of Maps";
  public static final String CONFIGURATION_MAP_CHECK_ERROR_MESSAGE =
    "Configuration elements must be Maps";
  public static final String CONFIGURATION_MAP_SIZE_CHECK_ERROR_MESSAGE =
    "Configuration Maps must hold a single configuration type each";

  // Primary Key Fields
  private static Set<String> pkPropertyIds =
    new HashSet<>(Arrays.asList(new String[]{
      BLUEPRINT_NAME_PROPERTY_ID}));

  /**
   * Used to create Blueprint instances
   */
  private BlueprintV2Factory blueprintFactory;

  /**
   * Used to create SecurityConfiguration instances
   */
  private static SecurityConfigurationFactory securityConfigurationFactory;

  /**
   * Blueprint Data Access Object
   */
  private static BlueprintV2DAO blueprintDAO;

  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds     the property ids
   * @param keyPropertyIds  the key property ids
   * @param controller      management controller
   */
  BlueprintV2ResourceProvider(Set<String> propertyIds,
                              Map<Resource.Type, String> keyPropertyIds,
                              AmbariManagementController controller) {

    super(propertyIds, keyPropertyIds, controller);
    blueprintFactory = BlueprintV2Factory.create(controller);
  }

  /**
   * Static initialization.
   *
   * @param dao       blueprint data access object
   * @param securityFactory
   * @param metaInfo
   */
  public static void init(BlueprintV2DAO dao, SecurityConfigurationFactory
    securityFactory, AmbariMetaInfo metaInfo) {
    blueprintDAO = dao;
    securityConfigurationFactory = securityFactory;
    ambariMetaInfo = metaInfo;
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
      try {
        createResources(getCreateCommand(properties, request.getRequestInfoProperties()));
      }catch(IllegalArgumentException e) {
        LOG.error("Exception while creating blueprint", e);
        throw e;
      }
    }
    notifyCreate(Resource.Type.Blueprint, request);

    return getRequestStatus(null);
  }

  @Override
  //todo: continue to use dao/entity directly or use blueprint factory?
  public Set<Resource> getResources(Request request, Predicate predicate)
    throws SystemException, UnsupportedPropertyException,
    NoSuchResourceException, NoSuchParentResourceException {

    List<BlueprintV2Entity> results = null;
    boolean applyPredicate = false;

    if (predicate != null) {
      Set<Map<String, Object>> requestProps = getPropertyMaps(predicate);
      if (requestProps.size() == 1 ) {
        String name = (String) requestProps.iterator().next().get(
          BLUEPRINT_NAME_PROPERTY_ID);

        if (name != null) {
          BlueprintV2Entity entity = blueprintDAO.findByName(name);
          results = entity == null ? Collections.emptyList() : Collections.singletonList(entity);
        }
      }
    }

    if (results == null) {
      applyPredicate = true;
      results = blueprintDAO.findAll();
    }

    Set<Resource> resources  = new HashSet<>();
    for (BlueprintV2Entity entity : results) {
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
  public RequestStatus deleteResources(Request request, Predicate predicate)
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

  /**
   * Used to get stack metainfo.
   */
  private static AmbariMetaInfo ambariMetaInfo;

  // ----- Instance Methods ------------------------------------------------

  /**
   * Create a resource instance from a blueprint entity.
   *
   * @param entity        blueprint entity
   * @param requestedIds  requested id's
   *
   * @return a new resource instance for the given blueprint entity
   */
  protected Resource toResource(BlueprintV2Entity entity, Set<String> requestedIds) throws NoSuchResourceException {
    try {
      Resource resource = new ResourceImpl(Resource.Type.Blueprint);
      Map<String, Object> blueprintAsMap = blueprintFactory.convertToMap(entity);
      if (!requestedIds.isEmpty()) {
        Map<String, Object> filteredMap = new HashMap<>();
        applySelectFilters(requestedIds, blueprintAsMap, filteredMap);
        blueprintAsMap = filteredMap;
      }
      // flatten the Blueprint property category
      Map<String, Object> blueprintPc = (Map<String, Object>)blueprintAsMap.remove(BLUEPRINTS_PROPERTY_ID);
      for (Map.Entry<String, Object> entry: blueprintPc.entrySet()) {
        blueprintAsMap.put(BLUEPRINTS_PROPERTY_ID + "/" + entry.getKey(), entry.getValue());
      }
      // set resources
      blueprintAsMap.entrySet().forEach( entry -> resource.setProperty(entry.getKey(), entry.getValue()) );
      return resource;
    }
    catch (IOException e) {
      throw new NoSuchResourceException("Cannot convert blueprint entity to resource. name=" + entity.getBlueprintName(), e);
    }
  }

  /**
   * Recursively applies select filters on an input map. Only properties matchig the filters will be preserved.
   * @param filters list of filters. Each filter is a string that can contain subfilters sepatated by '/'
   * @param startingMap The map to filter
   * @param collectingMap The map to put the results to
   */
  private void applySelectFilters(Set<String> filters, Map<String, Object> startingMap, Map<String, Object> collectingMap) {
    // Identify filters that apply to this level and those that will be applied on lower levels of the recursion
    Splitter splitter = Splitter.on('/').omitEmptyStrings().trimResults();
    Joiner joiner = Joiner.on('/');
    SetMultimap<String, String> lowerLevelFilters = HashMultimap.create();
    List<String> currentLevelFilters = new ArrayList<>();
    filters.forEach( filter -> {
      List<String> filterParts = ImmutableList.copyOf(splitter.split(filter));
      if (filterParts.size() == 1) {
        currentLevelFilters.add(filter);
      }
      else {
        lowerLevelFilters.put(filterParts.get(0), joiner.join(filterParts.subList(1, filterParts.size())));
      }
    });
    startingMap.entrySet().forEach( entry -> {
      if (currentLevelFilters.contains(entry.getKey())) {
        collectingMap.put(entry.getKey(), entry.getValue());
      }
      else if (lowerLevelFilters.containsKey(entry.getKey()) && entry.getValue() instanceof Map) {
        Map<String, Object> lowerLevelCollector = (Map<String, Object>)collectingMap.get(entry.getKey());
        if (null == lowerLevelCollector) {
          lowerLevelCollector = new HashMap<>();
          collectingMap.put(entry.getKey(), lowerLevelCollector);
        }
        applySelectFilters(lowerLevelFilters.get(entry.getKey()), (Map<String, Object>)entry.getValue(), lowerLevelCollector);
      }
    });
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
      @SuppressWarnings("rawtypes")
      @Override
      public Void invoke() throws AmbariException {
        String rawRequestBody = requestInfoProps.get(Request.REQUEST_INFO_BODY_PROPERTY);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(rawRequestBody), REQUEST_BODY_EMPTY_ERROR_MESSAGE);

        BlueprintV2 blueprint = null;
        try {
          blueprint = blueprintFactory.convertFromJson(rawRequestBody);
        }
        catch (IOException e) {
            throw new AmbariException("Unable to parse blueprint", e);
        }

        if (blueprintDAO.findByName(blueprint.getName()) != null) {
          throw new DuplicateResourceException(
            "Attempted to create a Blueprint which already exists, blueprint_name=" +
              blueprint.getName());
        }

        try {
          blueprint.validateRequiredProperties();
        } catch (InvalidTopologyException e) {
          throw new IllegalArgumentException("Blueprint configuration validation failed: " + e.getMessage(), e);
        }

        String validateTopology =  requestInfoProps.get("validate_topology");
        if (validateTopology == null || ! validateTopology.equalsIgnoreCase("false")) {
          try {
            blueprint.validateTopology();
          } catch (InvalidTopologyException e) {
            throw new IllegalArgumentException("Invalid blueprint topology", e);
          }
        }

        // TODO: handle security descriptor

        try {
          BlueprintV2Entity entity = blueprintFactory.convertToEntity(blueprint);
          blueprintDAO.create(entity);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        return null;
      }
    };
  }
}
