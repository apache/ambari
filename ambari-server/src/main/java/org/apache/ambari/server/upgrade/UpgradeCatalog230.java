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

package org.apache.ambari.server.upgrade;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.RoleAuthorizationDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.RoleAuthorizationEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;


/**
 * Upgrade catalog for version 2.3.0.
 */
public class UpgradeCatalog230 extends AbstractUpgradeCatalog {
  private static final String HOST_ROLE_COMMAND_TABLE = "host_role_command";
  private static final String USERS_TABLE = "users";

  private static final String HOST_ID_COL = "host_id";
  private static final String USER_TYPE_COL = "user_type";

  private static final String ADMIN_PERMISSION_TABLE = "adminpermission";
  private static final String PERMISSION_ID_COL = "permission_id";
  private static final String PERMISSION_NAME_COL = "permission_name";
  private static final String PERMISSION_LABEL_COL = "permission_label";

  private static final String ROLE_AUTHORIZATION_TABLE = "roleauthorization";
  private static final String PERMISSION_ROLE_AUTHORIZATION_TABLE = "permission_roleauthorization";
  private static final String ROLE_AUTHORIZATION_ID_COL = "authorization_id";
  private static final String ROLE_AUTHORIZATION_NAME_COL = "authorization_name";

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.2.1";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.3.0";
  }

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog230.class);

  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog230(Injector injector) {
    super(injector);
    this.injector = injector;
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {

    dbAccessor.alterColumn(HOST_ROLE_COMMAND_TABLE, new DBColumnInfo(HOST_ID_COL, Long.class, null, null, true));
    dbAccessor.addColumn(USERS_TABLE, new DBColumnInfo(USER_TYPE_COL, String.class, null, "LOCAL", true));

    dbAccessor.executeQuery("UPDATE users SET user_type='LDAP' WHERE ldap_user=1");

    dbAccessor.addUniqueConstraint(USERS_TABLE, "UNQ_users_0", "user_name", "user_type");

    updateAdminPermissionTable();
    createRoleAuthorizationTables();
  }

  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
  }

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    setPermissionLabels();
    updatePermissionNames();
    addNewPermissions();
    createRoleAuthorizations();
    createPermissionRoleAuthorizationMap();
  }

  private void addNewPermissions() throws SQLException {
    LOG.info("Adding new permissions: CLUSTER.OPERATOR, SERVICE.ADMINISTRATOR, SERVICE.OPERATOR");

    PermissionDAO permissionDAO = injector.getInstance(PermissionDAO.class);
    ResourceTypeDAO resourceTypeDAO = injector.getInstance(ResourceTypeDAO.class);
    ResourceTypeEntity clusterResourceTypeEntity = resourceTypeDAO.findByName("CLUSTER");

    // CLUSTER.OPERATOR: Cluster Operator
    if(permissionDAO.findPermissionByNameAndType("CLUSTER.OPERATOR", clusterResourceTypeEntity) == null) {
      PermissionEntity permissionEntity = new PermissionEntity();
      permissionEntity.setId(null);
      permissionEntity.setPermissionName("CLUSTER.OPERATOR");
      permissionEntity.setPermissionLabel("Cluster Operator");
      permissionEntity.setResourceType(clusterResourceTypeEntity);
      permissionDAO.create(permissionEntity);
    }

    // SERVICE.ADMINISTRATOR: Service Administrator
    if(permissionDAO.findPermissionByNameAndType("SERVICE.ADMINISTRATOR", clusterResourceTypeEntity) == null) {
      PermissionEntity permissionEntity = new PermissionEntity();
      permissionEntity.setId(null);
      permissionEntity.setPermissionName("SERVICE.ADMINISTRATOR");
      permissionEntity.setPermissionLabel("Service Administrator");
      permissionEntity.setResourceType(clusterResourceTypeEntity);
      permissionDAO.create(permissionEntity);
    }

    // SERVICE.OPERATOR: Service Operator
    if(permissionDAO.findPermissionByNameAndType("SERVICE.OPERATOR", clusterResourceTypeEntity) == null) {
      PermissionEntity permissionEntity = new PermissionEntity();
      permissionEntity.setId(null);
      permissionEntity.setPermissionName("SERVICE.OPERATOR");
      permissionEntity.setPermissionLabel("Service Operator");
      permissionEntity.setResourceType(clusterResourceTypeEntity);
      permissionDAO.create(permissionEntity);
    }
  }


  private void createRoleAuthorizations() throws SQLException {
    LOG.info("Adding authorizations");

    RoleAuthorizationDAO roleAuthorizationDAO = injector.getInstance(RoleAuthorizationDAO.class);

    createRoleAuthorization(roleAuthorizationDAO, "VIEW.USE", "Use View");

    createRoleAuthorization(roleAuthorizationDAO, "SERVICE.VIEW_METRICS", "View metrics");
    createRoleAuthorization(roleAuthorizationDAO, "SERVICE.VIEW_STATUS_INFO", "View status information");
    createRoleAuthorization(roleAuthorizationDAO, "SERVICE.VIEW_CONFIGS", "View configurations");
    createRoleAuthorization(roleAuthorizationDAO, "SERVICE.COMPARE_CONFIGS", "Compare configurations");
    createRoleAuthorization(roleAuthorizationDAO, "SERVICE.VIEW_ALERTS", "View service-level alerts");
    createRoleAuthorization(roleAuthorizationDAO, "SERVICE.START_STOP", "Start/Stop/Restart Service");
    createRoleAuthorization(roleAuthorizationDAO, "SERVICE.DECOMMISSION_RECOMMISSION", "Decommission/recommission");
    createRoleAuthorization(roleAuthorizationDAO, "SERVICE.RUN_SERVICE_CHECK", "Run service checks");
    createRoleAuthorization(roleAuthorizationDAO, "SERVICE.TOGGLE_MAINTENANCE", "Turn on/off maintenance mode");
    createRoleAuthorization(roleAuthorizationDAO, "SERVICE.RUN_CUSTOM_COMMAND", "Perform service-specific tasks");
    createRoleAuthorization(roleAuthorizationDAO, "SERVICE.MODIFY_CONFIGS", "Modify configurations");
    createRoleAuthorization(roleAuthorizationDAO, "SERVICE.MANAGE_CONFIG_GROUPS", "Manage configuration groups");
    createRoleAuthorization(roleAuthorizationDAO, "SERVICE.MANAGE_ALERTS", "Manage service-level alerts");
    createRoleAuthorization(roleAuthorizationDAO, "SERVICE.MOVE", "Move to another host");
    createRoleAuthorization(roleAuthorizationDAO, "SERVICE.ENABLE_HA", "Enable HA");
    createRoleAuthorization(roleAuthorizationDAO, "SERVICE.TOGGLE_ALERTS", "Enable/disable service-level alerts");
    createRoleAuthorization(roleAuthorizationDAO, "SERVICE.ADD_DELETE_SERVICES", "Add/delete services");
    createRoleAuthorization(roleAuthorizationDAO, "SERVICE.SET_SERVICE_USERS_GROUPS", "Set service users and groups");

    createRoleAuthorization(roleAuthorizationDAO, "HOST.VIEW_METRICS", "View metrics");
    createRoleAuthorization(roleAuthorizationDAO, "HOST.VIEW_STATUS_INFO", "View status information");
    createRoleAuthorization(roleAuthorizationDAO, "HOST.VIEW_CONFIGS", "View configuration");
    createRoleAuthorization(roleAuthorizationDAO, "HOST.TOGGLE_MAINTENANCE", "Turn on/off maintenance mode");
    createRoleAuthorization(roleAuthorizationDAO, "HOST.ADD_DELETE_COMPONENTS", "Install components");
    createRoleAuthorization(roleAuthorizationDAO, "HOST.ADD_DELETE_HOSTS", "Add/Delete hosts");

    createRoleAuthorization(roleAuthorizationDAO, "CLUSTER.VIEW_METRICS", "View metrics");
    createRoleAuthorization(roleAuthorizationDAO, "CLUSTER.VIEW_STATUS_INFO", "View status information");
    createRoleAuthorization(roleAuthorizationDAO, "CLUSTER.VIEW_CONFIGS", "View configuration");
    createRoleAuthorization(roleAuthorizationDAO, "CLUSTER.VIEW_STACK_DETAILS", "View stack version details");
    createRoleAuthorization(roleAuthorizationDAO, "CLUSTER.VIEW_ALERTS", "View cluster-level alerts");
    createRoleAuthorization(roleAuthorizationDAO, "CLUSTER.MANAGE_CREDENTIALS", "Manage external credentials");
    createRoleAuthorization(roleAuthorizationDAO, "CLUSTER.MODIFY_CONFIGS", "Modify cluster configurations");
    createRoleAuthorization(roleAuthorizationDAO, "CLUSTER.MANAGE_CONFIG_GROUPS", "Manage cluster configuration groups");
    createRoleAuthorization(roleAuthorizationDAO, "CLUSTER.MANAGE_ALERTS", "Manage cluster-level alerts");
    createRoleAuthorization(roleAuthorizationDAO, "CLUSTER.TOGGLE_ALERTS", "Enable/disable cluster-level alerts");
    createRoleAuthorization(roleAuthorizationDAO, "CLUSTER.TOGGLE_KERBEROS", "Enable/disable Kerberos");
    createRoleAuthorization(roleAuthorizationDAO, "CLUSTER.UPGRADE_DOWNGRADE_STACK", "Upgrade/downgrade stack");

    createRoleAuthorization(roleAuthorizationDAO, "AMBARI.ADD_DELETE_CLUSTERS", "Create new clusters");
    createRoleAuthorization(roleAuthorizationDAO, "AMBARI.RENAME_CLUSTER", "Rename clusters");
    createRoleAuthorization(roleAuthorizationDAO, "AMBARI.MANAGE_USERS", "Manage users");
    createRoleAuthorization(roleAuthorizationDAO, "AMBARI.MANAGE_GROUPS", "Manage groups");
    createRoleAuthorization(roleAuthorizationDAO, "AMBARI.MANAGE_VIEWS", "Manage Ambari Views");
    createRoleAuthorization(roleAuthorizationDAO, "AMBARI.ASSIGN_ROLES", "Assign roles");
    createRoleAuthorization(roleAuthorizationDAO, "AMBARI.MANAGE_STACK_VERSIONS", "Manage stack versions");
    createRoleAuthorization(roleAuthorizationDAO, "AMBARI.EDIT_STACK_REPOS", "Edit stack repository URLs");
  }

  private void createRoleAuthorization(RoleAuthorizationDAO roleAuthorizationDAO, String id, String name) {
    if(roleAuthorizationDAO.findById(id) == null) {
      RoleAuthorizationEntity roleAuthorizationEntity = new RoleAuthorizationEntity();
      roleAuthorizationEntity.setAuthorizationId(id);
      roleAuthorizationEntity.setAuthorizationName(name);
      roleAuthorizationDAO.create(roleAuthorizationEntity);
    }
  }

  private void createPermissionRoleAuthorizationMap() throws SQLException {
    LOG.info("Creating permission to authorizations map");

    // Determine the role entities
    PermissionDAO permissionDAO = injector.getInstance(PermissionDAO.class);
    ResourceTypeDAO resourceTypeDAO = injector.getInstance(ResourceTypeDAO.class);

    ResourceTypeEntity ambariResource = resourceTypeDAO.findByName("AMBARI");
    ResourceTypeEntity clusterResource = resourceTypeDAO.findByName("CLUSTER");
    ResourceTypeEntity viewResource = resourceTypeDAO.findByName("VIEW");

    PermissionEntity viewPermission = permissionDAO.findPermissionByNameAndType("VIEW.USER", viewResource);
    PermissionEntity administratorPermission = permissionDAO.findPermissionByNameAndType("AMBARI.ADMINISTRATOR", ambariResource);
    PermissionEntity clusterUserPermission = permissionDAO.findPermissionByNameAndType("CLUSTER.USER", clusterResource);
    PermissionEntity clusterOperatorPermission = permissionDAO.findPermissionByNameAndType("CLUSTER.OPERATOR", clusterResource);
    PermissionEntity clusterAdministratorPermission = permissionDAO.findPermissionByNameAndType("CLUSTER.ADMINISTRATOR", clusterResource);
    PermissionEntity serviceAdministratorPermission = permissionDAO.findPermissionByNameAndType("SERVICE.ADMINISTRATOR", clusterResource);
    PermissionEntity serviceOperatorPermission = permissionDAO.findPermissionByNameAndType("SERVICE.OPERATOR", clusterResource);

    // Create role groups
    Collection<PermissionEntity> viewUserAndAdministrator = Arrays.asList(viewPermission, administratorPermission);
    Collection<PermissionEntity> clusterUserAndUp = Arrays.asList(
        clusterUserPermission,
        serviceOperatorPermission,
        serviceAdministratorPermission,
        clusterOperatorPermission,
        clusterAdministratorPermission,
        administratorPermission);
    Collection<PermissionEntity> serviceOperatorAndUp = Arrays.asList(
        serviceOperatorPermission,
        serviceAdministratorPermission,
        clusterOperatorPermission,
        clusterAdministratorPermission,
        administratorPermission);
    Collection<PermissionEntity> serviceAdministratorAndUp = Arrays.asList(
        serviceAdministratorPermission,
        clusterOperatorPermission,
        clusterAdministratorPermission,
        administratorPermission);
    Collection<PermissionEntity> clusterOperatorAndUp = Arrays.asList(
        clusterOperatorPermission,
        clusterAdministratorPermission,
        administratorPermission);
    Collection<PermissionEntity> clusterAdministratorAndUp = Arrays.asList(
        clusterAdministratorPermission,
        administratorPermission);
    Collection<PermissionEntity> administratorOnly = Collections.singleton(administratorPermission);

    // A map of the authorizations to the relevant roles
    Map<String, Collection<PermissionEntity>> map = new HashMap<String, Collection<PermissionEntity>>();
    map.put("VIEW.USE", viewUserAndAdministrator);
    map.put("SERVICE.VIEW_METRICS", clusterUserAndUp);
    map.put("SERVICE.VIEW_STATUS_INFO", clusterUserAndUp);
    map.put("SERVICE.VIEW_CONFIGS", clusterUserAndUp);
    map.put("SERVICE.COMPARE_CONFIGS", clusterUserAndUp);
    map.put("SERVICE.VIEW_ALERTS", clusterUserAndUp);
    map.put("SERVICE.START_STOP", serviceOperatorAndUp);
    map.put("SERVICE.DECOMMISSION_RECOMMISSION", serviceOperatorAndUp);
    map.put("SERVICE.RUN_SERVICE_CHECK", serviceOperatorAndUp);
    map.put("SERVICE.TOGGLE_MAINTENANCE", serviceOperatorAndUp);
    map.put("SERVICE.RUN_CUSTOM_COMMAND", serviceOperatorAndUp);
    map.put("SERVICE.MODIFY_CONFIGS", serviceAdministratorAndUp);
    map.put("SERVICE.MANAGE_CONFIG_GROUPS", serviceAdministratorAndUp);
    map.put("CLUSTER.MANAGE_CONFIG_GROUPS", serviceAdministratorAndUp);
    map.put("SERVICE.MANAGE_ALERTS", serviceAdministratorAndUp);
    map.put("SERVICE.MOVE", serviceAdministratorAndUp);
    map.put("SERVICE.ENABLE_HA", serviceAdministratorAndUp);
    map.put("SERVICE.TOGGLE_ALERTS", serviceAdministratorAndUp);
    map.put("SERVICE.ADD_DELETE_SERVICES", clusterAdministratorAndUp);
    map.put("SERVICE.SET_SERVICE_USERS_GROUPS", clusterAdministratorAndUp);
    map.put("HOST.VIEW_METRICS", clusterUserAndUp);
    map.put("HOST.VIEW_STATUS_INFO", clusterUserAndUp);
    map.put("HOST.VIEW_CONFIGS", clusterUserAndUp);
    map.put("HOST.TOGGLE_MAINTENANCE", clusterOperatorAndUp);
    map.put("HOST.ADD_DELETE_COMPONENTS", clusterOperatorAndUp);
    map.put("HOST.ADD_DELETE_HOSTS", clusterOperatorAndUp);
    map.put("CLUSTER.VIEW_METRICS", clusterUserAndUp);
    map.put("CLUSTER.VIEW_STATUS_INFO", clusterUserAndUp);
    map.put("CLUSTER.VIEW_CONFIGS", clusterUserAndUp);
    map.put("CLUSTER.VIEW_STACK_DETAILS", clusterUserAndUp);
    map.put("CLUSTER.VIEW_ALERTS", clusterUserAndUp);
    map.put("CLUSTER.MANAGE_CREDENTIALS", clusterAdministratorAndUp);
    map.put("CLUSTER.MODIFY_CONFIGS", clusterAdministratorAndUp);
    map.put("CLUSTER.MANAGE_ALERTS", clusterAdministratorAndUp);
    map.put("CLUSTER.TOGGLE_ALERTS", clusterAdministratorAndUp);
    map.put("CLUSTER.TOGGLE_KERBEROS", clusterAdministratorAndUp);
    map.put("CLUSTER.UPGRADE_DOWNGRADE_STACK", clusterAdministratorAndUp);
    map.put("AMBARI.ADD_DELETE_CLUSTERS", administratorOnly);
    map.put("AMBARI.RENAME_CLUSTER", administratorOnly);
    map.put("AMBARI.MANAGE_USERS", administratorOnly);
    map.put("AMBARI.MANAGE_GROUPS", administratorOnly);
    map.put("AMBARI.MANAGE_VIEWS", administratorOnly);
    map.put("AMBARI.ASSIGN_ROLES", administratorOnly);
    map.put("AMBARI.MANAGE_STACK_VERSIONS", administratorOnly);
    map.put("AMBARI.EDIT_STACK_REPOS", administratorOnly);

    // Iterate over the map of authorizations to role to find the set of roles to map to each
    // authorization and then add the relevant record
    for (Map.Entry<String, Collection<PermissionEntity>> entry : map.entrySet()) {
      String authorizationId = entry.getKey();

      for (PermissionEntity permission : entry.getValue()) {
        addAuthorizationToRole(permission, authorizationId);
      }
    }
  }


  // ----- UpgradeCatalog ----------------------------------------------------

  private void updateAdminPermissionTable() throws SQLException {
    // Add the permission_label column to the adminpermission table
    dbAccessor.addColumn(ADMIN_PERMISSION_TABLE, new DBColumnInfo(PERMISSION_LABEL_COL, String.class, 255, null, true));
  }

  private void createRoleAuthorizationTables() throws SQLException {

    ArrayList<DBColumnInfo> columns;

    //  Add roleauthorization table
    LOG.info("Creating " + ROLE_AUTHORIZATION_TABLE + " table");
    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo(ROLE_AUTHORIZATION_ID_COL, String.class, 100, null, false));
    columns.add(new DBColumnInfo(ROLE_AUTHORIZATION_NAME_COL, String.class, 255, null, false));
    dbAccessor.createTable(ROLE_AUTHORIZATION_TABLE, columns, ROLE_AUTHORIZATION_ID_COL);

    //  Add permission_roleauthorization table to map roleauthorizations to permissions (aka roles)
    LOG.info("Creating " + PERMISSION_ROLE_AUTHORIZATION_TABLE + " table");
    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo(PERMISSION_ID_COL, Long.class, null, null, false));
    columns.add(new DBColumnInfo(ROLE_AUTHORIZATION_ID_COL, String.class, 100, null, false));
    dbAccessor.createTable(PERMISSION_ROLE_AUTHORIZATION_TABLE, columns, PERMISSION_ID_COL, ROLE_AUTHORIZATION_ID_COL);

    dbAccessor.addFKConstraint(PERMISSION_ROLE_AUTHORIZATION_TABLE, "FK_permission_roleauth_pid",
        PERMISSION_ID_COL, ADMIN_PERMISSION_TABLE, PERMISSION_ID_COL, false);

    dbAccessor.addFKConstraint(PERMISSION_ROLE_AUTHORIZATION_TABLE, "FK_permission_roleauth_aid",
        ROLE_AUTHORIZATION_ID_COL, ROLE_AUTHORIZATION_TABLE, ROLE_AUTHORIZATION_ID_COL, false);
  }

  private void setPermissionLabels() throws SQLException {
    String updateStatement = "UPDATE " + ADMIN_PERMISSION_TABLE + " SET " + PERMISSION_LABEL_COL + "='%s' WHERE " + PERMISSION_ID_COL + "=%d";

    LOG.info("Setting permission labels");
    dbAccessor.executeUpdate(String.format(updateStatement,
        "Ambari Administrator", PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION));
    dbAccessor.executeUpdate(String.format(updateStatement,
        "Cluster User", PermissionEntity.CLUSTER_USER_PERMISSION));
    dbAccessor.executeUpdate(String.format(updateStatement,
        "Cluster Administrator", PermissionEntity.CLUSTER_ADMINISTRATOR_PERMISSION));
    dbAccessor.executeUpdate(String.format(updateStatement,
        "View User", PermissionEntity.VIEW_USER_PERMISSION));
  }

  private void updatePermissionNames() throws SQLException {
    String updateStatement = "UPDATE " + ADMIN_PERMISSION_TABLE + " SET " + PERMISSION_NAME_COL + "='%s' WHERE " + PERMISSION_ID_COL + "=%d";

    // Update permissions names
    LOG.info("Updating permission names");
    dbAccessor.executeUpdate(String.format(updateStatement,
        PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION_NAME, PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION));
    dbAccessor.executeUpdate(String.format(updateStatement,
        PermissionEntity.CLUSTER_USER_PERMISSION_NAME, PermissionEntity.CLUSTER_USER_PERMISSION));
    dbAccessor.executeUpdate(String.format(updateStatement,
        PermissionEntity.CLUSTER_ADMINISTRATOR_PERMISSION_NAME, PermissionEntity.CLUSTER_ADMINISTRATOR_PERMISSION));
    dbAccessor.executeUpdate(String.format(updateStatement,
        PermissionEntity.VIEW_USER_PERMISSION_NAME, PermissionEntity.VIEW_USER_PERMISSION));
  }

}
