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

import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

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
import org.apache.ambari.server.events.AmbariEvent;
import org.apache.ambari.server.events.AmbariLdapConfigChangedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.apache.ambari.server.orm.entities.ConfigurationBaseEntity;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Resource provider for AmbariConfiguration resources.
 */
public class AmbariConfigurationResourceProvider extends AbstractAuthorizedResourceProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmbariConfigurationResourceProvider.class);
  private static final String DEFAULT_VERSION_TAG = "Default version";
  private static final Integer DEFAULT_VERSION = 1;

  /**
   * Resource property id constants.
   */
  public enum ResourcePropertyId {

    ID("AmbariConfiguration/id"),
    TYPE("AmbariConfiguration/type"),
    VERSION("AmbariConfiguration/version"),
    VERSION_TAG("AmbariConfiguration/version_tag"),
    DATA("AmbariConfiguration/data");

    private String propertyId;

    ResourcePropertyId(String propertyId) {
      this.propertyId = propertyId;
    }

    String getPropertyId() {
      return this.propertyId;
    }

    public static ResourcePropertyId fromString(String propertyIdStr) {
      ResourcePropertyId propertyIdFromStr = null;

      for (ResourcePropertyId id : ResourcePropertyId.values()) {
        if (id.getPropertyId().equals(propertyIdStr)) {
          propertyIdFromStr = id;
          break;
        }
      }

      if (propertyIdFromStr == null) {
        throw new IllegalArgumentException("Unsupported property type: " + propertyIdStr);
      }

      return propertyIdFromStr;

    }
  }

  private static Set<String> PROPERTIES = Sets.newHashSet(
    ResourcePropertyId.ID.getPropertyId(),
    ResourcePropertyId.TYPE.getPropertyId(),
    ResourcePropertyId.VERSION.getPropertyId(),
    ResourcePropertyId.VERSION_TAG.getPropertyId(),
    ResourcePropertyId.DATA.getPropertyId());

  private static Map<Resource.Type, String> PK_PROPERTY_MAP = Collections.unmodifiableMap(
    new HashMap<Resource.Type, String>() {{
      put(Resource.Type.AmbariConfiguration, ResourcePropertyId.ID.getPropertyId());
    }}
  );


  @Inject
  private AmbariConfigurationDAO ambariConfigurationDAO;

  @Inject
  private AmbariEventPublisher publisher;


  private Gson gson;

  @AssistedInject
  public AmbariConfigurationResourceProvider() {
    super(PROPERTIES, PK_PROPERTY_MAP);
    setRequiredCreateAuthorizations(EnumSet.of(RoleAuthorization.AMBARI_MANAGE_CONFIGURATION));
    setRequiredDeleteAuthorizations(EnumSet.of(RoleAuthorization.AMBARI_MANAGE_CONFIGURATION));

    gson = new GsonBuilder().create();
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return Sets.newHashSet(ResourcePropertyId.ID.getPropertyId());
  }

  @Override
  public RequestStatus createResourcesAuthorized(Request request) throws SystemException, UnsupportedPropertyException,
    ResourceAlreadyExistsException, NoSuchParentResourceException {

    LOGGER.info("Creating new ambari configuration resource ...");
    AmbariConfigurationEntity ambariConfigurationEntity = null;
    try {
      ambariConfigurationEntity = getEntityFromRequest(request);
    } catch (AmbariException e) {
      throw new NoSuchParentResourceException(e.getMessage());
    }

    LOGGER.info("Persisting new ambari configuration: {} ", ambariConfigurationEntity);

    try {
      ambariConfigurationDAO.create(ambariConfigurationEntity);
    } catch (Exception e) {
      LOGGER.error("Failed to create resource", e);
      throw new ResourceAlreadyExistsException(e.getMessage());
    }

    // todo filter by configuration type
    // notify subscribers about the configuration changes
    publisher.publish(new AmbariLdapConfigChangedEvent(AmbariEvent.AmbariEventType.LDAP_CONFIG_CHANGED,
      ambariConfigurationEntity.getId()));

    return getRequestStatus(null);
  }


  @Override
  protected Set<Resource> getResourcesAuthorized(Request request, Predicate predicate) throws SystemException,
    UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    Set<Resource> resources = Sets.newHashSet();

    // retrieves allconfigurations, filtering is done at a higher level
    List<AmbariConfigurationEntity> ambariConfigurationEntities = ambariConfigurationDAO.findAll();
    for (AmbariConfigurationEntity ambariConfigurationEntity : ambariConfigurationEntities) {
      try {
        resources.add(toResource(ambariConfigurationEntity, getPropertyIds()));
      } catch (AmbariException e) {
        LOGGER.error("Error while retrieving ambari configuration", e);
      }
    }
    return resources;
  }

  @Override
  protected RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate) throws SystemException,
    UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Long idFromRequest = Long.valueOf((String) PredicateHelper.getProperties(predicate).get(ResourcePropertyId.ID.getPropertyId()));

    if (null == idFromRequest) {
      LOGGER.debug("No resource id provided in the request");
    } else {
      LOGGER.debug("Deleting amari configuration with id: {}", idFromRequest);
      try {
        ambariConfigurationDAO.removeByPK(idFromRequest);
      } catch (IllegalStateException e) {
        throw new NoSuchResourceException(e.getMessage());
      }

    }

    // notify subscribers about the configuration changes
    publisher.publish(new AmbariLdapConfigChangedEvent(AmbariEvent.AmbariEventType.LDAP_CONFIG_CHANGED, idFromRequest));


    return getRequestStatus(null);

  }

  @Override
  protected RequestStatus updateResourcesAuthorized(Request request, Predicate predicate) throws SystemException,
    UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    Long idFromRequest = Long.valueOf((String) PredicateHelper.getProperties(predicate).get(ResourcePropertyId.ID.getPropertyId()));

    AmbariConfigurationEntity persistedEntity = ambariConfigurationDAO.findByPK(idFromRequest);
    if (persistedEntity == null) {
      String errorMsg = String.format("Entity with primary key [ %s ] not found in the database.", idFromRequest);
      LOGGER.error(errorMsg);
      throw new NoSuchResourceException(errorMsg);
    }

    try {

      AmbariConfigurationEntity entityFromRequest = getEntityFromRequest(request);
      persistedEntity.getConfigurationBaseEntity().setVersionTag(entityFromRequest.getConfigurationBaseEntity().getVersionTag());
      persistedEntity.getConfigurationBaseEntity().setVersion(entityFromRequest.getConfigurationBaseEntity().getVersion());
      persistedEntity.getConfigurationBaseEntity().setType(entityFromRequest.getConfigurationBaseEntity().getType());
      persistedEntity.getConfigurationBaseEntity().setConfigurationData(entityFromRequest.getConfigurationBaseEntity().getConfigurationData());
      persistedEntity.getConfigurationBaseEntity().setConfigurationAttributes(entityFromRequest.getConfigurationBaseEntity().getConfigurationAttributes());


      ambariConfigurationDAO.update(persistedEntity);
    } catch (AmbariException e) {
      throw new NoSuchParentResourceException(e.getMessage());
    }

    publisher.publish(new AmbariLdapConfigChangedEvent(AmbariEvent.AmbariEventType.LDAP_CONFIG_CHANGED,
      persistedEntity.getId()));


    return getRequestStatus(null);

  }

  private Resource toResource(AmbariConfigurationEntity entity, Set<String> requestedIds) throws AmbariException {

    if (null == entity) {
      throw new IllegalArgumentException("Null entity can't be transformed into a resource");
    }

    if (null == entity.getConfigurationBaseEntity()) {
      throw new IllegalArgumentException("Invalid configuration entity can't be transformed into a resource");
    }
    Resource resource = new ResourceImpl(Resource.Type.AmbariConfiguration);
    Set<Map<String, String>> configurationSet = gson.fromJson(entity.getConfigurationBaseEntity().getConfigurationData(), Set.class);

    setResourceProperty(resource, ResourcePropertyId.ID.getPropertyId(), entity.getId(), requestedIds);
    setResourceProperty(resource, ResourcePropertyId.TYPE.getPropertyId(), entity.getConfigurationBaseEntity().getType(), requestedIds);
    setResourceProperty(resource, ResourcePropertyId.DATA.getPropertyId(), configurationSet, requestedIds);
    setResourceProperty(resource, ResourcePropertyId.VERSION.getPropertyId(), entity.getConfigurationBaseEntity().getVersion(), requestedIds);
    setResourceProperty(resource, ResourcePropertyId.VERSION_TAG.getPropertyId(), entity.getConfigurationBaseEntity().getVersionTag(), requestedIds);

    return resource;
  }

  private AmbariConfigurationEntity getEntityFromRequest(Request request) throws AmbariException {

    AmbariConfigurationEntity ambariConfigurationEntity = new AmbariConfigurationEntity();
    ambariConfigurationEntity.setConfigurationBaseEntity(new ConfigurationBaseEntity());

    // set of resource properties (eache entry in the set belongs to a different resource)
    Set<Map<String, Object>> resourcePropertiesSet = request.getProperties();

    if (resourcePropertiesSet.size() != 1) {
      throw new AmbariException("There must be only one resource specified in the request");
    }

    // the configuration type must be set
    if (getValueFromResourceProperties(ResourcePropertyId.TYPE, resourcePropertiesSet.iterator().next()) == null) {
      throw new AmbariException("The configuration type must be set");
    }


    for (ResourcePropertyId resourcePropertyId : ResourcePropertyId.values()) {
      Object requestValue = getValueFromResourceProperties(resourcePropertyId, resourcePropertiesSet.iterator().next());

      switch (resourcePropertyId) {
        case DATA:
          if (requestValue == null) {
            throw new IllegalArgumentException("No configuration data is provided in the request");
          }
          ambariConfigurationEntity.getConfigurationBaseEntity().setConfigurationData(gson.toJson(requestValue));
          break;
        case TYPE:
          ambariConfigurationEntity.getConfigurationBaseEntity().setType((String) requestValue);
          break;
        case VERSION:
          Integer version = (requestValue == null) ? DEFAULT_VERSION : Integer.valueOf((String) requestValue);
          ambariConfigurationEntity.getConfigurationBaseEntity().setVersion((version));
          break;
        case VERSION_TAG:
          String versionTag = requestValue == null ? DEFAULT_VERSION_TAG : (String) requestValue;
          ambariConfigurationEntity.getConfigurationBaseEntity().setVersionTag(versionTag);
          break;
        default:
          LOGGER.debug("Ignored property in the request: {}", resourcePropertyId);
          break;
      }
    }
    ambariConfigurationEntity.getConfigurationBaseEntity().setCreateTimestamp(Calendar.getInstance().getTimeInMillis());
    return ambariConfigurationEntity;

  }

  private Object getValueFromResourceProperties(ResourcePropertyId resourcePropertyIdEnum, Map<String, Object> resourceProperties) {
    LOGGER.debug("Locating resource property [{}] in the resource properties map ...", resourcePropertyIdEnum);
    Object requestValue = null;

    if (resourceProperties.containsKey(resourcePropertyIdEnum.getPropertyId())) {
      requestValue = resourceProperties.get(resourcePropertyIdEnum.getPropertyId());
      LOGGER.debug("Found resource property {} in the resource properties map, value: {}", resourcePropertyIdEnum, requestValue);
    }
    return requestValue;
  }

}
