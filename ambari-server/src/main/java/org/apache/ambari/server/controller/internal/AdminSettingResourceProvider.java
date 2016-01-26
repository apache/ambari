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
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.AdminSettingDAO;
import org.apache.ambari.server.orm.entities.AdminSettingEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This class deals with managing CRUD operation on {@link AdminSettingEntity}.
 */
@StaticallyInject
public class AdminSettingResourceProvider extends AbstractAuthorizedResourceProvider {

  protected static final String ID = "id";
  protected static final String ADMINSETTING = "AdminSetting";
  protected static final String ADMINSETTING_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("AdminSettings", "name");
  protected static final String ADMINSETTING_SETTING_TYPE_PROPERTY_ID = PropertyHelper.getPropertyId("AdminSettings", "setting_type");
  protected static final String ADMINSETTING_CONTENT_PROPERTY_ID = PropertyHelper.getPropertyId("AdminSettings", "content");
  protected static final String ADMINSETTING_UPDATED_BY_PROPERTY_ID = PropertyHelper.getPropertyId("AdminSettings", "updated_by");
  protected static final String ADMINSETTING_UPDATE_TIMESTAMP_PROPERTY_ID = PropertyHelper.getPropertyId("AdminSettings", "update_timestamp");

  /**
   * The property ids for a admin setting resource.
   */
  private static final Set<String> propertyIds = new HashSet<>();

  /**
   * The key property ids for a admin setting resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = new HashMap<>();

  private static final Set<String> requiredProperties = new HashSet<>();

  @Inject
  private static AdminSettingDAO dao;

  static {
    propertyIds.add(ADMINSETTING_NAME_PROPERTY_ID);
    propertyIds.add(ADMINSETTING_SETTING_TYPE_PROPERTY_ID);
    propertyIds.add(ADMINSETTING_CONTENT_PROPERTY_ID);
    propertyIds.add(ADMINSETTING_UPDATED_BY_PROPERTY_ID);
    propertyIds.add(ADMINSETTING_UPDATE_TIMESTAMP_PROPERTY_ID);
    propertyIds.add(ADMINSETTING_SETTING_TYPE_PROPERTY_ID);
    propertyIds.add(ADMINSETTING);

    keyPropertyIds.put(Resource.Type.AdminSetting, ADMINSETTING_NAME_PROPERTY_ID);

    requiredProperties.add(ADMINSETTING_NAME_PROPERTY_ID);
    requiredProperties.add(ADMINSETTING_SETTING_TYPE_PROPERTY_ID);
    requiredProperties.add(ADMINSETTING_CONTENT_PROPERTY_ID);
  }

  protected AdminSettingResourceProvider() {
    super(propertyIds, keyPropertyIds);
    EnumSet<RoleAuthorization> requiredAuthorizations = EnumSet.of(RoleAuthorization.AMBARI_MANAGE_ADMIN_SETTINGS);
    setRequiredCreateAuthorizations(requiredAuthorizations);
    setRequiredDeleteAuthorizations(requiredAuthorizations);
    setRequiredUpdateAuthorizations(requiredAuthorizations);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }

  @Override
  public RequestStatus createResourcesAuthorized(Request request)
          throws NoSuchParentResourceException, ResourceAlreadyExistsException, SystemException {
    Set<Resource> associatedResources = new HashSet<>();

    for (Map<String, Object> properties : request.getProperties()) {
      AdminSettingEntity settingEntity = createResources(newCreateCommand(request, properties));
      Resource resource = new ResourceImpl(Resource.Type.AdminSetting);
      resource.setProperty(ADMINSETTING_NAME_PROPERTY_ID, settingEntity.getName());
      associatedResources.add(resource);
    }

    return getRequestStatus(null, associatedResources);
  }

  @Override
  public Set<Resource> getResourcesAuthorized(Request request, Predicate predicate) throws NoSuchResourceException {
    List<AdminSettingEntity> entities = new LinkedList<>();
    final Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);
    if (propertyMaps.isEmpty()) {
      entities = dao.findAll();
    }
    for (Map<String, Object> propertyMap: propertyMaps) {
      if (propertyMap.containsKey(ADMINSETTING_NAME_PROPERTY_ID)) {
        String name = propertyMap.get(ADMINSETTING_NAME_PROPERTY_ID).toString();
        AdminSettingEntity entity = dao.findByName(name);
        if (entity == null) {
          throw new NoSuchResourceException(String.format("AdminSetting with name %s does not exists", name));
        }
        entities.add(entity);
      } else {
        entities = dao.findAll();
        break;
      }
    }
    Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources = new HashSet<>();
    for(AdminSettingEntity entity : entities) {
      resources.add(toResource(entity, requestedIds));
    }
    return resources;
  }

  @Override
  public RequestStatus updateResourcesAuthorized(Request request, Predicate predicate)
          throws NoSuchResourceException, NoSuchParentResourceException, SystemException {
    modifyResources(newUpdateCommand(request));
    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResourcesAuthorized(Predicate predicate) {
    final Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);
    for (Map<String, Object> propertyMap : propertyMaps) {
      if (propertyMap.containsKey(ADMINSETTING_NAME_PROPERTY_ID)) {
        dao.removeByName(propertyMap.get(ADMINSETTING_NAME_PROPERTY_ID).toString());
      }
    }
    return getRequestStatus(null);
  }


  private Command<AdminSettingEntity> newCreateCommand(final Request request, final Map<String, Object> properties) {
    return new Command<AdminSettingEntity>() {
      @Override
      public AdminSettingEntity invoke() throws AmbariException, AuthorizationException {
        AdminSettingEntity entity = toEntity(properties);
        dao.create(entity);
        notifyCreate(Resource.Type.AdminSetting, request);
        return entity;
      }
    };
  }

  private Command<Void> newUpdateCommand(final Request request) throws NoSuchResourceException, SystemException {
    return new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        final Set<Map<String, Object>> propertyMaps = request.getProperties();
        for (Map<String, Object> propertyMap : propertyMaps) {
          if (propertyMap.containsKey(ADMINSETTING_NAME_PROPERTY_ID)) {
            String name = propertyMap.get(ADMINSETTING_NAME_PROPERTY_ID).toString();
            AdminSettingEntity entity = dao.findByName(name);
            if (entity == null) {
              throw new AmbariException(String.format("There is no admin setting with name: %s ", name));
            }
            updateEntity(entity, propertyMap);
            dao.merge(entity);
          }
        }
        return null;
      }
    };
  }

  private void updateEntity(AdminSettingEntity entity, Map<String, Object> propertyMap) throws AmbariException {
    String name = propertyMap.get(ADMINSETTING_NAME_PROPERTY_ID).toString();
    if (!Objects.equals(name, entity.getName())) {
      throw new AmbariException("Name for AdminSetting is immutable, cannot change name.");
    }

    if (StringUtils.isNotBlank(ObjectUtils.toString(propertyMap.get(ADMINSETTING_CONTENT_PROPERTY_ID)))) {
      entity.setContent(propertyMap.get(ADMINSETTING_CONTENT_PROPERTY_ID).toString());
    }

    if (StringUtils.isNotBlank(ObjectUtils.toString(propertyMap.get(ADMINSETTING_SETTING_TYPE_PROPERTY_ID)))) {
      entity.setSettingType(propertyMap.get(ADMINSETTING_SETTING_TYPE_PROPERTY_ID).toString());
    }

    entity.setUpdatedBy(AuthorizationHelper.getAuthenticatedName());
    entity.setUpdateTimestamp(System.currentTimeMillis());
  }

  private Resource toResource(final AdminSettingEntity adminSettingEntity, final Set<String> requestedIds) {
    Resource resource = new ResourceImpl(Resource.Type.AdminSetting);
    setResourceProperty(resource, ADMINSETTING_NAME_PROPERTY_ID, adminSettingEntity.getName(), requestedIds);
    setResourceProperty(resource, ADMINSETTING_SETTING_TYPE_PROPERTY_ID, adminSettingEntity.getSettingType(), requestedIds);
    setResourceProperty(resource, ADMINSETTING_CONTENT_PROPERTY_ID, adminSettingEntity.getContent(), requestedIds);
    setResourceProperty(resource, ADMINSETTING_UPDATED_BY_PROPERTY_ID, adminSettingEntity.getUpdatedBy(), requestedIds);
    setResourceProperty(resource, ADMINSETTING_UPDATE_TIMESTAMP_PROPERTY_ID, adminSettingEntity.getUpdateTimestamp(), requestedIds);
    return resource;
  }

  private AdminSettingEntity toEntity(final Map<String, Object> properties) throws AmbariException {
    for (String propertyName: requiredProperties) {
      if (properties.get(propertyName) == null) {
        throw new AmbariException(String.format("Property %s should be provided", propertyName));
      }
    }

    AdminSettingEntity entity = new AdminSettingEntity();
    entity.setName(properties.get(ADMINSETTING_NAME_PROPERTY_ID).toString());
    entity.setSettingType(properties.get(ADMINSETTING_SETTING_TYPE_PROPERTY_ID).toString());
    entity.setContent(properties.get(ADMINSETTING_CONTENT_PROPERTY_ID).toString());
    entity.setUpdatedBy(AuthorizationHelper.getAuthenticatedName());
    entity.setUpdateTimestamp(System.currentTimeMillis());
    return entity;
  }
}
