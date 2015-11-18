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

import com.google.inject.Inject;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A write-only resource provider for securely stored credentials
 */
@StaticallyInject
public class RoleAuthorizationResourceProvider extends ReadOnlyResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  public static final String AUTHORIZATION_ID_PROPERTY_ID = PropertyHelper.getPropertyId("AuthorizationInfo", "authorization_id");
  public static final String PERMISSION_ID_PROPERTY_ID = PropertyHelper.getPropertyId("AuthorizationInfo", "permission_id");
  public static final String AUTHORIZATION_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("AuthorizationInfo", "authorization_name");

  private static final Set<String> PK_PROPERTY_IDS;
  private static final Set<String> PROPERTY_IDS;
  private static final Map<Type, String> KEY_PROPERTY_IDS;

  static {
    Set<String> set;
    set = new HashSet<String>();
    set.add(AUTHORIZATION_ID_PROPERTY_ID);
    set.add(PERMISSION_ID_PROPERTY_ID);
    PK_PROPERTY_IDS = Collections.unmodifiableSet(set);

    set = new HashSet<String>();
    set.add(AUTHORIZATION_ID_PROPERTY_ID);
    set.add(PERMISSION_ID_PROPERTY_ID);
    set.add(AUTHORIZATION_NAME_PROPERTY_ID);
    PROPERTY_IDS = Collections.unmodifiableSet(set);

    HashMap<Type, String> map = new HashMap<Type, String>();
    map.put(Type.Permission, PERMISSION_ID_PROPERTY_ID);
    map.put(Type.RoleAuthorization, AUTHORIZATION_ID_PROPERTY_ID);
    KEY_PROPERTY_IDS = Collections.unmodifiableMap(map);
  }

  /**
   * Data access object used to obtain permission entities.
   */
  @Inject
  protected static PermissionDAO permissionDAO;

  /**
   * Create a new resource provider.
   */
  public RoleAuthorizationResourceProvider(AmbariManagementController managementController) {
    super(PROPERTY_IDS, KEY_PROPERTY_IDS, managementController);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources = new HashSet<Resource>();

    Set<Map<String, Object>> propertyMaps;

    if (predicate == null) {
      // The request must be from /
      propertyMaps = Collections.singleton(Collections.<String, Object>emptyMap());
    } else {
      propertyMaps = getPropertyMaps(predicate);
    }

    if (propertyMaps != null) {
      for (Map<String, Object> propertyMap : propertyMaps) {
        Object object = propertyMap.get(PERMISSION_ID_PROPERTY_ID);
        Collection<RoleAuthorizationEntity> authorizationEntities;
        Integer permissionId;

        if (object instanceof String) {
          try {
            permissionId = Integer.valueOf((String) object);
          } catch (NumberFormatException e) {
            LOG.warn(PERMISSION_ID_PROPERTY_ID + " is not a valid integer value", e);
            throw new NoSuchResourceException("The requested resource doesn't exist: Authorization not found, " + predicate, e);
          }
        } else if (object instanceof Number) {
          permissionId = ((Number) object).intValue();
        } else {
          permissionId = null;
        }

        if (permissionId == null) {
          // TODO: ** This is stubbed out until the data layer catches up...
          // TODO: entities = roleAuthorizationDAO.findAll();
          authorizationEntities = createAdminAuthorizations();
        } else {
          PermissionEntity permissionEntity = permissionDAO.findById(permissionId);

          if(permissionEntity == null)
            authorizationEntities = null;
          else
          {
            // TODO: ** This is stubbed out until the data layer catches up...
            // TODO: authorizationEntities = (permissionEntity == null)
            // TODO: ? null
            // TODO: : permissionEntity.getAuthorizations();
            String permissionName = permissionEntity.getPermissionName();
            if (permissionName.startsWith("AMBARI")) {
              authorizationEntities = createAdminAuthorizations();
            } else if (permissionName.startsWith("CLUSTER")) {
              authorizationEntities = createOperatorAuthorizations();
            } else {
              authorizationEntities = null;
            }
          }
        }

        if (authorizationEntities != null) {
          String authorizationId = (String) propertyMap.get(AUTHORIZATION_ID_PROPERTY_ID);

          if(!StringUtils.isEmpty(authorizationId)) {
            // Filter the entities
            Iterator<RoleAuthorizationEntity> iterator = authorizationEntities.iterator();
            while(iterator.hasNext()) {
              if(!authorizationId.equals(iterator.next().getAuthorizationId())) {
                iterator.remove();
              }
            }
          }

          for (RoleAuthorizationEntity entity : authorizationEntities) {
            resources.add(toResource(permissionId, entity, requestedIds));
          }
        }
      }
    }

    return resources;
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }

  /**
   * Creates a new resource from the given RoleAuthorizationEntity and set of requested ids.
   *
   * @param entity       the RoleAuthorizationEntity
   * @param requestedIds the properties to include in the resulting resource instance
   * @return a resource
   */
  private Resource toResource(Integer permissionId, RoleAuthorizationEntity entity, Set<String> requestedIds) {
    Resource resource = new ResourceImpl(Type.RoleAuthorization);
    setResourceProperty(resource, AUTHORIZATION_ID_PROPERTY_ID, entity.getAuthorizationId(), requestedIds);
    if(permissionId != null) {
      setResourceProperty(resource, PERMISSION_ID_PROPERTY_ID, permissionId, requestedIds);
    }
    setResourceProperty(resource, AUTHORIZATION_NAME_PROPERTY_ID, entity.getAuthorizationName(), requestedIds);
    return resource;
  }

  /**
   * Fills RoleAuthorizationEntities for an administrator user
   * <p/>
   * This is a temporary method until the data layer catches up
   * <p/>
   * TODO: Remove when the data later catches up
   *
   * @return an array of RoleAuthorizationEntity objects
   */
  private Collection<RoleAuthorizationEntity> createAdminAuthorizations() {
    Collection<RoleAuthorizationEntity> authorizationEntities = new ArrayList<RoleAuthorizationEntity>();
    authorizationEntities.add(new RoleAuthorizationEntity("VIEW.USE", "Use View"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.VIEW_METRICS", "View metrics"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.VIEW_STATUS_INFO", "View status information"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.VIEW_CONFIGS", "View configurations"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.COMPARE_CONFIGS", "Compare configurations"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.VIEW_ALERTS", "View service alerts"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.START_STOP", "Start/Stop/Restart Service"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.DECOMMISSION_RECOMMISSION", "Decommission/recommission"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.RUN_SERVICE_CHECK", "Run service checks"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.TOGGLE_MAINTENANCE", "Turn on/off maintenance mode"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.RUN_CUSTOM_COMMAND", "Perform service-specific tasks"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.MODIFY_CONFIGS", "Modify configurations"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.MANAGE_CONFIG_GROUPS", "Manage configuration groups"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.MOVE", "Move to another host"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.ENABLE_HA", "Enable HA"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.TOGGLE_ALERTS", "Enable/disable service alerts"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.ADD_DELETE_SERVICES", "Add Service to cluster"));
    authorizationEntities.add(new RoleAuthorizationEntity("HOST.VIEW_METRICS", "View metrics"));
    authorizationEntities.add(new RoleAuthorizationEntity("HOST.VIEW_STATUS_INFO", "View status information"));
    authorizationEntities.add(new RoleAuthorizationEntity("HOST.VIEW_CONFIGS", "View configuration"));
    authorizationEntities.add(new RoleAuthorizationEntity("HOST.COMPARE_CONFIGS", "Turn on/off maintenance mode"));
    authorizationEntities.add(new RoleAuthorizationEntity("HOST.ADD_DELETE_COMPONENTS", "Install components"));
    authorizationEntities.add(new RoleAuthorizationEntity("HOST.ADD_DELETE_HOSTS", "Add/Delete hosts"));
    authorizationEntities.add(new RoleAuthorizationEntity("CLUSTER.VIEW_METRICS", "View metrics"));
    authorizationEntities.add(new RoleAuthorizationEntity("CLUSTER.VIEW_STATUS_INFO", "View status information"));
    authorizationEntities.add(new RoleAuthorizationEntity("CLUSTER.VIEW_CONFIGS", "View configuration"));
    authorizationEntities.add(new RoleAuthorizationEntity("CLUSTER.VIEW_STACK_DETAILS", "View stack version details"));
    authorizationEntities.add(new RoleAuthorizationEntity("CLUSTER.VIEW_ALERTS", "View alerts"));
    authorizationEntities.add(new RoleAuthorizationEntity("CLUSTER.TOGGLE_ALERTS", "Enable/disable alerts"));
    authorizationEntities.add(new RoleAuthorizationEntity("CLUSTER.TOGGLE_KERBEROS", "Enable/disable Kerberos"));
    authorizationEntities.add(new RoleAuthorizationEntity("CLUSTER.UPGRADE_DOWNGRADE_STACK", "Upgrade/downgrade stack"));
    authorizationEntities.add(new RoleAuthorizationEntity("AMBARI.ADD_DELETE_CLUSTERS", "Create new clusters"));
    authorizationEntities.add(new RoleAuthorizationEntity("AMBARI.SET_SERVICE_USERS_GROUPS", "Set service users and groups"));
    authorizationEntities.add(new RoleAuthorizationEntity("AMBARI.RENAME_CLUSTER", "Rename clusters"));
    authorizationEntities.add(new RoleAuthorizationEntity("AMBARI.MANAGE_USERS", "Manage users"));
    authorizationEntities.add(new RoleAuthorizationEntity("AMBARI.MANAGE_GROUPS", "Manage groups"));
    authorizationEntities.add(new RoleAuthorizationEntity("AMBARI.MANAGE_VIEWS", "Manage Ambari Views"));
    authorizationEntities.add(new RoleAuthorizationEntity("AMBARI.ASSIGN_ROLES", "Assign roles"));
    authorizationEntities.add(new RoleAuthorizationEntity("AMBARI.MANAGE_STACK_VERSIONS", "Manage stack versions"));
    authorizationEntities.add(new RoleAuthorizationEntity("AMBARI.EDIT_STACK_REPOS", "Edit stack repository URLs"));
    return authorizationEntities;
  }

  /**
   * Fills RoleAuthorizationEntities for an administrator user
   * <p/>
   * This is a temporary method until the data layer catches up
   * <p/>
   * TODO: Remove when the data later catches up
   *
   * @return an array of RoleAuthorizationEntity objects
   */
  private Collection<RoleAuthorizationEntity> createOperatorAuthorizations() {
    Collection<RoleAuthorizationEntity> authorizationEntities = new ArrayList<RoleAuthorizationEntity>();
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.VIEW_METRICS", "View metrics"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.VIEW_STATUS_INFO", "View status information"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.VIEW_CONFIGS", "View configurations"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.COMPARE_CONFIGS", "Compare configurations"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.VIEW_ALERTS", "View service alerts"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.START_STOP", "Start/Stop/Restart Service"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.DECOMMISSION_RECOMMISSION", "Decommission/recommission"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.RUN_SERVICE_CHECK", "Run service checks"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.TOGGLE_MAINTENANCE", "Turn on/off maintenance mode"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.RUN_CUSTOM_COMMAND", "Perform service-specific tasks"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.MODIFY_CONFIGS", "Modify configurations"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.MANAGE_CONFIG_GROUPS", "Manage configuration groups"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.MOVE", "Move to another host"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.ENABLE_HA", "Enable HA"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.TOGGLE_ALERTS", "Enable/disable service alerts"));
    authorizationEntities.add(new RoleAuthorizationEntity("SERVICE.ADD_DELETE_SERVICES", "Add Service to cluster"));
    authorizationEntities.add(new RoleAuthorizationEntity("HOST.VIEW_METRICS", "View metrics"));
    authorizationEntities.add(new RoleAuthorizationEntity("HOST.VIEW_STATUS_INFO", "View status information"));
    authorizationEntities.add(new RoleAuthorizationEntity("HOST.VIEW_CONFIGS", "View configuration"));
    authorizationEntities.add(new RoleAuthorizationEntity("HOST.COMPARE_CONFIGS", "Turn on/off maintenance mode"));
    authorizationEntities.add(new RoleAuthorizationEntity("HOST.ADD_DELETE_COMPONENTS", "Install components"));
    authorizationEntities.add(new RoleAuthorizationEntity("HOST.ADD_DELETE_HOSTS", "Add/Delete hosts"));
    authorizationEntities.add(new RoleAuthorizationEntity("CLUSTER.VIEW_METRICS", "View metrics"));
    authorizationEntities.add(new RoleAuthorizationEntity("CLUSTER.VIEW_STATUS_INFO", "View status information"));
    authorizationEntities.add(new RoleAuthorizationEntity("CLUSTER.VIEW_CONFIGS", "View configuration"));
    authorizationEntities.add(new RoleAuthorizationEntity("CLUSTER.VIEW_STACK_DETAILS", "View stack version details"));
    authorizationEntities.add(new RoleAuthorizationEntity("CLUSTER.VIEW_ALERTS", "View alerts"));
    authorizationEntities.add(new RoleAuthorizationEntity("CLUSTER.TOGGLE_ALERTS", "Enable/disable alerts"));
    authorizationEntities.add(new RoleAuthorizationEntity("CLUSTER.TOGGLE_KERBEROS", "Enable/disable Kerberos"));
    authorizationEntities.add(new RoleAuthorizationEntity("CLUSTER.UPGRADE_DOWNGRADE_STACK", "Upgrade/downgrade stack"));
    return authorizationEntities;
  }

  /**
   * RoleAuthorizationEntity is a stubbed out Entity class to be replaced by a real Entity class
   * TODO: Replace with real RoleAuthorizationEntity class when the data later catches up
   */
  private static class RoleAuthorizationEntity {
    private final String authorizationId;
    private final String authorizationName;

    private RoleAuthorizationEntity(String authorizationId, String authorizationName) {
      this.authorizationId = authorizationId;
      this.authorizationName = authorizationName;
    }

    public String getAuthorizationId() {
      return authorizationId;
    }

    public String getAuthorizationName() {
      return authorizationName;
    }
  }
}
