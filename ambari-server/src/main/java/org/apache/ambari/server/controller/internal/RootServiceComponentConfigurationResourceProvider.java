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

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.events.AmbariConfigurationChangedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class RootServiceComponentConfigurationResourceProvider extends AbstractAuthorizedResourceProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(RootServiceComponentConfigurationResourceProvider.class);

  static final String RESOURCE_KEY = "Configuration";

  public static final String CONFIGURATION_CATEGORY_PROPERTY_ID = PropertyHelper.getPropertyId(RESOURCE_KEY, "category");
  public static final String CONFIGURATION_PROPERTIES_PROPERTY_ID = PropertyHelper.getPropertyId(RESOURCE_KEY, "properties");
  public static final String CONFIGURATION_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(RESOURCE_KEY, "component_name");
  public static final String CONFIGURATION_SERVICE_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(RESOURCE_KEY, "service_name");

  private static final Set<String> PROPERTIES;

  private static final Map<Resource.Type, String> PK_PROPERTY_MAP;

  private static final Set<String> PK_PROPERTY_IDS;

  static {
    Set<String> set = new HashSet<>();
    set.add(CONFIGURATION_SERVICE_NAME_PROPERTY_ID);
    set.add(CONFIGURATION_COMPONENT_NAME_PROPERTY_ID);
    set.add(CONFIGURATION_CATEGORY_PROPERTY_ID);
    set.add(CONFIGURATION_PROPERTIES_PROPERTY_ID);

    PROPERTIES = Collections.unmodifiableSet(set);

    Map<Resource.Type, String> map = new HashMap<>();
    map.put(Resource.Type.RootService, CONFIGURATION_SERVICE_NAME_PROPERTY_ID);
    map.put(Resource.Type.RootServiceComponent, CONFIGURATION_COMPONENT_NAME_PROPERTY_ID);
    map.put(Resource.Type.RootServiceComponentConfiguration, CONFIGURATION_CATEGORY_PROPERTY_ID);

    PK_PROPERTY_MAP = Collections.unmodifiableMap(map);
    PK_PROPERTY_IDS = Collections.unmodifiableSet(new HashSet<>(PK_PROPERTY_MAP.values()));
  }

  @Inject
  private AmbariConfigurationDAO ambariConfigurationDAO;

  @Inject
  private AmbariEventPublisher publisher;

  public RootServiceComponentConfigurationResourceProvider() {
    super(PROPERTIES, PK_PROPERTY_MAP);

    Set<RoleAuthorization> authorizations = EnumSet.of(RoleAuthorization.AMBARI_MANAGE_CONFIGURATION);
    setRequiredCreateAuthorizations(authorizations);
    setRequiredDeleteAuthorizations(authorizations);
    setRequiredUpdateAuthorizations(authorizations);
    setRequiredGetAuthorizations(authorizations);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }

  @Override
  public RequestStatus createResourcesAuthorized(Request request)
      throws SystemException, UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {

    createOrAddProperties(null, null, null, request.getProperties(), true);

    return getRequestStatus(null);
  }

  @Override
  protected Set<Resource> getResourcesAuthorized(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    return getResources(new Command<Set<Resource>>() {
      @Override
      public Set<Resource> invoke() throws AmbariException {
        Set<Resource> resources = new HashSet<>();
        Set<String> requestedIds = getRequestPropertyIds(request, predicate);

        if (CollectionUtils.isEmpty(requestedIds)) {
          requestedIds = PROPERTIES;
        }

        if (predicate == null) {
          Set<Resource> _resources;
          try {
            _resources = getConfigurationResources(requestedIds, null);
          } catch (NoSuchResourceException e) {
            throw new AmbariException(e.getMessage(), e);
          }

          if (!CollectionUtils.isEmpty(_resources)) {
            resources.addAll(_resources);
          }
        } else {
          for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
            Set<Resource> _resources;
            try {
              _resources = getConfigurationResources(requestedIds, propertyMap);
            } catch (NoSuchResourceException e) {
              throw new AmbariException(e.getMessage(), e);
            }

            if (!CollectionUtils.isEmpty(_resources)) {
              resources.addAll(_resources);
            }
          }
        }

        return resources;
      }
    });
  }


  @Override
  protected RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    String serviceName = (String) PredicateHelper.getProperties(predicate).get(CONFIGURATION_SERVICE_NAME_PROPERTY_ID);
    String componentName = (String) PredicateHelper.getProperties(predicate).get(CONFIGURATION_COMPONENT_NAME_PROPERTY_ID);
    String categoryName = (String) PredicateHelper.getProperties(predicate).get(CONFIGURATION_CATEGORY_PROPERTY_ID);

    ConfigurationHandler handler = getConfigurationHandler(serviceName, componentName);
    if (handler != null) {
      handler.removeConfiguration(categoryName);
    } else {
      throw new SystemException(String.format("Configurations may not be updated for the %s component of the root service %s", componentName, serviceName));
    }

    return getRequestStatus(null);
  }

  @Override
  protected RequestStatus updateResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    String serviceName = (String) PredicateHelper.getProperties(predicate).get(CONFIGURATION_SERVICE_NAME_PROPERTY_ID);
    String componentName = (String) PredicateHelper.getProperties(predicate).get(CONFIGURATION_COMPONENT_NAME_PROPERTY_ID);
    String categoryName = (String) PredicateHelper.getProperties(predicate).get(CONFIGURATION_CATEGORY_PROPERTY_ID);

    createOrAddProperties(serviceName, componentName, categoryName, request.getProperties(), false);

    return getRequestStatus(null);
  }

  private Resource toResource(String serviceName, String componentName, String categoryName, Map<String, String> properties, Set<String> requestedIds) {
    Resource resource = new ResourceImpl(Resource.Type.RootServiceComponentConfiguration);
    setResourceProperty(resource, CONFIGURATION_SERVICE_NAME_PROPERTY_ID, serviceName, requestedIds);
    setResourceProperty(resource, CONFIGURATION_COMPONENT_NAME_PROPERTY_ID, componentName, requestedIds);
    setResourceProperty(resource, CONFIGURATION_CATEGORY_PROPERTY_ID, categoryName, requestedIds);
    setResourceProperty(resource, CONFIGURATION_PROPERTIES_PROPERTY_ID, properties, requestedIds);
    return resource;
  }

  /**
   * Retrieves groups of properties from the request data and create or updates them as needed.
   * <p>
   * Each group of properties is expected to have a category (<code>AmbariConfiguration/category</code>)
   * value and one or more property (<code>AmbariConfiguration/properties/property.name</code>) values.
   * If a category cannot be determined from the propery set, the default category value (passed in)
   * is used.  If a default category is set, it is assumed that it was parsed from the request predicate
   * (if availabe).
   *
   * @param defaultServiceName             the default service name to use if needed
   * @param defaultComponentName           the default component name to use if needed
   * @param defaultCategoryName            the default category to use if needed
   * @param requestProperties              a collection of property maps parsed from the request
   * @param removePropertiesIfNotSpecified <code>true</code> to remove existing properties that have not been specifed in the request;
   *                                       <code>false</code> append or update the existing set of properties with values from the request
   * @throws SystemException if an error occurs saving the configuration data
   */
  private void createOrAddProperties(String defaultServiceName, String defaultComponentName, String defaultCategoryName,
                                     Set<Map<String, Object>> requestProperties, boolean removePropertiesIfNotSpecified)
      throws SystemException {
    // set of resource properties (each entry in the set belongs to a different resource)
    if (requestProperties != null) {
      for (Map<String, Object> resourceProperties : requestProperties) {
        RequestDetails requestDetails = parseProperties(defaultServiceName, defaultComponentName, defaultCategoryName, resourceProperties);

        ConfigurationHandler handler = getConfigurationHandler(requestDetails.serviceName, requestDetails.componentName);

        if (handler != null) {
          handler.updateCategory(requestDetails.categoryName, requestDetails.properties, removePropertiesIfNotSpecified);
        } else {
          throw new SystemException(String.format("Configurations may not be updated for the %s component of the root service, %s", requestDetails.serviceName, requestDetails.componentName));
        }
      }
    }
  }

  /**
   * Parse the property map from a request into a map of services to components to category names to maps of property names and values.
   *
   * @param defaultServiceName   the default service name to use if one is not found in the map of properties
   * @param defaultComponentName the default component name to use if one is not found in the map of properties
   * @param defaultCategoryName  the default category name to use if one is not found in the map of properties
   * @param resourceProperties   a map of properties from a request item   @return a map of category names to maps of name/value pairs
   * @throws SystemException if an issue with the data is determined
   */
  private RequestDetails parseProperties(String defaultServiceName, String defaultComponentName, String defaultCategoryName, Map<String, Object> resourceProperties) throws SystemException {
    String serviceName = defaultServiceName;
    String componentName = defaultComponentName;
    String categoryName = defaultCategoryName;
    Map<String, String> properties = new HashMap<>();

    for (Map.Entry<String, Object> entry : resourceProperties.entrySet()) {
      String propertyName = entry.getKey();

      if (CONFIGURATION_CATEGORY_PROPERTY_ID.equals(propertyName)) {
        if (entry.getValue() instanceof String) {
          categoryName = (String) entry.getValue();
        }
      } else if (CONFIGURATION_COMPONENT_NAME_PROPERTY_ID.equals(propertyName)) {
        if (entry.getValue() instanceof String) {
          componentName = (String) entry.getValue();
        }
      } else if (CONFIGURATION_SERVICE_NAME_PROPERTY_ID.equals(propertyName)) {
        if (entry.getValue() instanceof String) {
          serviceName = (String) entry.getValue();
        }
      } else {
        String propertyCategory = PropertyHelper.getPropertyCategory(entry.getKey());
        if ((propertyCategory != null) && propertyCategory.equals(CONFIGURATION_PROPERTIES_PROPERTY_ID)) {
          String name = PropertyHelper.getPropertyName(entry.getKey());
          Object value = entry.getValue();
          properties.put(name, (value == null) ? null : value.toString());
        }
      }
    }

    if (StringUtils.isEmpty(serviceName)) {
      throw new SystemException("The service name must be set");
    }

    if (StringUtils.isEmpty(componentName)) {
      throw new SystemException("The component name must be set");
    }

    if (StringUtils.isEmpty(categoryName)) {
      throw new SystemException("The configuration category must be set");
    }

    if (properties.isEmpty()) {
      throw new SystemException("The configuration properties must be set");
    }

    return new RequestDetails(serviceName, componentName, categoryName, properties);
  }

  /**
   * Retrieves the requested configration resources
   *
   * @param requestedIds the requested properties ids
   * @param propertyMap  the request properties
   * @return a set of resources built from the found data
   * @throws NoSuchResourceException if the requested resource was not found
   */
  private Set<Resource> getConfigurationResources(Set<String> requestedIds, Map<String, Object> propertyMap) throws NoSuchResourceException {
    Set<Resource> resources = new HashSet<>();

    String serviceName = getStringProperty(propertyMap, CONFIGURATION_SERVICE_NAME_PROPERTY_ID);
    String componentName = getStringProperty(propertyMap, CONFIGURATION_COMPONENT_NAME_PROPERTY_ID);

    ConfigurationHandler handler = getConfigurationHandler(serviceName, componentName);

    if (handler != null) {
      String categoryName = getStringProperty(propertyMap, CONFIGURATION_CATEGORY_PROPERTY_ID);
      Map<String, Map<String, String>> configurations = handler.getConfigurations(categoryName);

      if (configurations != null) {
        for (Map.Entry<String, Map<String, String>> entry : configurations.entrySet()) {
          resources.add(toResource(serviceName, componentName, entry.getKey(), entry.getValue(), requestedIds));
        }
      }
    }

    return resources;
  }

  /**
   * Returns the internal configuration handler used to support various configuration storage facilites.
   *
   * @param serviceName   the service name
   * @param componentName the component name
   * @return
   */
  private ConfigurationHandler getConfigurationHandler(String serviceName, String componentName) {
    if (RootService.AMBARI.name().equals(serviceName)) {
      if (RootComponent.AMBARI_SERVER.name().equals(componentName)) {
        return new AmbariServerConfigurationHandler();
      }
    }

    return null;
  }


  private String getStringProperty(Map<String, Object> propertyMap, String propertyId) {
    String value = null;

    if (propertyMap != null) {
      Object o = propertyMap.get(propertyId);
      if (o instanceof String) {
        value = (String) o;
      }
    }

    return value;
  }

  /**
   * ConfigurationHandler is an interface to be implemented to support the relevant types of storage
   * used to persist root-level component configurations.
   */
  private abstract class ConfigurationHandler {
    /**
     * Retrieve the request configurations.
     *
     * @param categoryName the category name (or <code>null</code> for all)
     * @return a map of category names to properties (name/value pairs).
     * @throws NoSuchResourceException if the requested data is not found
     */
    public abstract Map<String, Map<String, String>> getConfigurations(String categoryName) throws NoSuchResourceException;

    /**
     * Delete the requested configuration.
     *
     * @param categoryName the category name
     * @throws NoSuchResourceException if the requested category does not exist
     */
    public abstract void removeConfiguration(String categoryName) throws NoSuchResourceException;

    /**
     * Set or update a configuration category with the specified properties.
     * <p>
     * If <code>removePropertiesIfNotSpecified</code> is <code>true</code>, the persisted category is to include only the specified properties.
     * <p>
     * If <code>removePropertiesIfNotSpecified</code> is <code>false</code>, the persisted category is to include the union of the existing and specified properties.
     * <p>
     * In any case, existing property values will be overwritten by the one specified in the property map.
     *
     * @param categoryName                   the category name
     * @param properties                     a map of properties to set
     * @param removePropertiesIfNotSpecified <code>true</code> to ensure the set of properties are only those that have be explicitly specified;
     *                                       <code>false</code> to update the set of exising properties with the specified set of properties, adding missing properties but not removing any properties
     */
    public abstract void updateCategory(String categoryName, Map<String, String> properties, boolean removePropertiesIfNotSpecified);
  }

  /**
   * AmbariServerConfigurationHandler handle Ambari server specific configuration properties.
   */
  private class AmbariServerConfigurationHandler extends ConfigurationHandler {
    @Override
    public Map<String, Map<String, String>> getConfigurations(String categoryName)
        throws NoSuchResourceException {
      Map<String, Map<String, String>> configurations = null;

      List<AmbariConfigurationEntity> entities = (categoryName == null)
          ? ambariConfigurationDAO.findAll()
          : ambariConfigurationDAO.findByCategory(categoryName);

      if (entities != null) {
        configurations = new HashMap<>();

        for (AmbariConfigurationEntity entity : entities) {
          String category = entity.getCategoryName();
          Map<String, String> properties = configurations.get(category);

          if (properties == null) {
            properties = new TreeMap<>();
            configurations.put(category, properties);
          }

          properties.put(entity.getPropertyName(), entity.getPropertyValue());
        }
      }

      return configurations;
    }

    @Override
    public void removeConfiguration(String categoryName) throws NoSuchResourceException {
      if (null == categoryName) {
        LOGGER.debug("No resource id provided in the request");
      } else {
        LOGGER.debug("Deleting Ambari configuration with id: {}", categoryName);
        try {
          if (ambariConfigurationDAO.removeByCategory(categoryName) > 0) {
            publisher.publish(new AmbariConfigurationChangedEvent(categoryName));
          }
        } catch (IllegalStateException e) {
          throw new NoSuchResourceException(e.getMessage());
        }
      }
    }

    @Override
    public void updateCategory(String categoryName, Map<String, String> properties, boolean removePropertiesIfNotSpecified) {
      if (ambariConfigurationDAO.reconcileCategory(categoryName, properties, removePropertiesIfNotSpecified)) {
        // notify subscribers about the configuration changes
        publisher.publish(new AmbariConfigurationChangedEvent(categoryName));
      }
    }
  }

  /**
   * RequestDetails is a container for details parsed from the request.
   */
  private class RequestDetails {
    final String serviceName;
    final String componentName;
    final String categoryName;
    final Map<String, String> properties;

    private RequestDetails(String serviceName, String componentName, String categoryName, Map<String, String> properties) {
      this.serviceName = serviceName;
      this.componentName = componentName;
      this.categoryName = categoryName;
      this.properties = properties;
    }
  }
}

