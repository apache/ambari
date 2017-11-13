/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
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

/**
 * Resource provider for AmbariConfiguration resources.
 */
public class AmbariConfigurationResourceProvider extends AbstractAuthorizedResourceProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmbariConfigurationResourceProvider.class);

  static final String AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID = PropertyHelper.getPropertyId("AmbariConfiguration", "category");
  static final String AMBARI_CONFIGURATION_PROPERTIES_PROPERTY_ID = PropertyHelper.getPropertyId("AmbariConfiguration", "properties");

  private static final Set<String> PROPERTIES = Collections.unmodifiableSet(
      new HashSet<>(Arrays.asList(
          AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID,
          AMBARI_CONFIGURATION_PROPERTIES_PROPERTY_ID)
      )
  );

  private static final Map<Resource.Type, String> PK_PROPERTY_MAP = Collections.unmodifiableMap(
      Collections.singletonMap(Resource.Type.AmbariConfiguration, AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID)
  );

  private static final Set<String> PK_PROPERTY_IDS = Collections.unmodifiableSet(
      new HashSet<>(PK_PROPERTY_MAP.values())
  );

  @Inject
  private AmbariConfigurationDAO ambariConfigurationDAO;

  @Inject
  private AmbariEventPublisher publisher;

  public AmbariConfigurationResourceProvider() {
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

    createOrAddProperties(null, request.getProperties(), true);

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
          Set<Resource> _resources = getAmbariConfigurationResources(requestedIds, null);
          if (!CollectionUtils.isEmpty(_resources)) {
            resources.addAll(_resources);
          }
        } else {
          for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
            Set<Resource> _resources = getAmbariConfigurationResources(requestedIds, propertyMap);
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

    String categoryName = (String) PredicateHelper.getProperties(predicate).get(AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID);

    if (null == categoryName) {
      LOGGER.debug("No resource id provided in the request");
    } else {
      LOGGER.debug("Deleting Ambari configuration with id: {}", categoryName);
      try {
        ambariConfigurationDAO.removeByCategory(categoryName);
      } catch (IllegalStateException e) {
        throw new NoSuchResourceException(e.getMessage());
      }
    }

    // notify subscribers about the configuration changes
    publisher.publish(new AmbariConfigurationChangedEvent(categoryName));
    return getRequestStatus(null);
  }

  @Override
  protected RequestStatus updateResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    String categoryName = (String) PredicateHelper.getProperties(predicate).get(AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID);
    createOrAddProperties(categoryName, request.getProperties(), false);

    return getRequestStatus(null);
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
   * @param defaultCategoryName            the default category to use if needed
   * @param requestProperties              a collection of property maps parsed from the request
   * @param removePropertiesIfNotSpecified <code>true</code> to remove existing properties that have not been specifed in the request; <code>false</code> append or update the existing set of properties with values from the request
   * @throws SystemException if an error occurs saving the configuration data
   */
  private void createOrAddProperties(String defaultCategoryName, Set<Map<String, Object>> requestProperties, boolean removePropertiesIfNotSpecified)
      throws SystemException {
    // set of resource properties (each entry in the set belongs to a different resource)
    if (requestProperties != null) {
      for (Map<String, Object> resourceProperties : requestProperties) {
        Map<String, Map<String, String>> entityMap = parseProperties(defaultCategoryName, resourceProperties);

        if (entityMap != null) {
          for (Map.Entry<String, Map<String, String>> entry : entityMap.entrySet()) {
            String categoryName = entry.getKey();

            if (ambariConfigurationDAO.reconcileCategory(categoryName, entry.getValue(), removePropertiesIfNotSpecified)) {
              // notify subscribers about the configuration changes
              publisher.publish(new AmbariConfigurationChangedEvent(categoryName));
            }
          }
        }
      }
    }
  }

  private Resource toResource(String categoryName, Map<String, String> properties, Set<String> requestedIds) {
    Resource resource = new ResourceImpl(Resource.Type.AmbariConfiguration);
    setResourceProperty(resource, AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID, categoryName, requestedIds);
    setResourceProperty(resource, AMBARI_CONFIGURATION_PROPERTIES_PROPERTY_ID, properties, requestedIds);
    return resource;
  }

  /**
   * Parse the property map from a request into a map of category names to maps of property names and values.
   *
   * @param defaultCategoryName the default category name to use if one is not found in the map of properties
   * @param resourceProperties  a map of properties from a request item
   * @return a map of category names to maps of name/value pairs
   * @throws SystemException if an issue with the data is determined
   */
  private Map<String, Map<String, String>> parseProperties(String defaultCategoryName, Map<String, Object> resourceProperties) throws SystemException {
    String categoryName = null;
    Map<String, String> properties = new HashMap<>();

    for (Map.Entry<String, Object> entry : resourceProperties.entrySet()) {
      String propertyName = entry.getKey();

      if (AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID.equals(propertyName)) {
        if (entry.getValue() instanceof String) {
          categoryName = (String) entry.getValue();
        }
      } else {
        String propertyCategory = PropertyHelper.getPropertyCategory(entry.getKey());
        if ((propertyCategory != null) && propertyCategory.equals(AMBARI_CONFIGURATION_PROPERTIES_PROPERTY_ID)) {
          String name = PropertyHelper.getPropertyName(entry.getKey());
          Object value = entry.getValue();
          properties.put(name, (value == null) ? null : value.toString());
        }
      }
    }

    if (categoryName == null) {
      categoryName = defaultCategoryName;
    }

    if (StringUtils.isEmpty(categoryName)) {
      throw new SystemException("The configuration type must be set");
    }

    if (properties.isEmpty()) {
      throw new SystemException("The configuration properties must be set");
    }

    return Collections.singletonMap(categoryName, properties);
  }

  private Set<Resource> getAmbariConfigurationResources(Set<String> requestedIds, Map<String, Object> propertyMap) {
    Set<Resource> resources = new HashSet<>();

    String categoryName = getStringProperty(propertyMap, AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID);

    List<AmbariConfigurationEntity> entities = (categoryName == null)
        ? ambariConfigurationDAO.findAll()
        : ambariConfigurationDAO.findByCategory(categoryName);

    if (entities != null) {
      Map<String, Map<String, String>> configurations = new HashMap<>();

      for (AmbariConfigurationEntity entity : entities) {
        String category = entity.getCategoryName();
        Map<String, String> properties = configurations.get(category);

        if (properties == null) {
          properties = new TreeMap<>();
          configurations.put(category, properties);
        }

        properties.put(entity.getPropertyName(), entity.getPropertyValue());
      }

      for (Map.Entry<String, Map<String, String>> entry : configurations.entrySet()) {
        resources.add(toResource(entry.getKey(), entry.getValue(), requestedIds));
      }
    }

    return resources;
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
}
