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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.PrincipalDAO;
import org.apache.ambari.server.orm.dao.PrincipalTypeDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.Transactional;

/**
 * Upgrade catalog for version 2.4.2.
 */
public class UpgradeCatalog242 extends AbstractUpgradeCatalog {

  protected static final String EXTENSION_TABLE = "extension";
  protected static final String USERS_TABLE = "users";
  protected static final String HOST_ROLE_COMMAND_TABLE = "host_role_command";
  protected static final String BLUEPRINT_TABLE = "blueprint";
  protected static final String HOST_GROUP_TABLE = "hostgroup";
  protected static final String BLUEPRINT_CONFIGURATION = "blueprint_configuration";
  protected static final String BLUEPRINT_SETTING = "blueprint_setting";
  protected static final String HOSTGROUP_COMPONENT = "hostgroup_component";
  protected static final String HOSTGROUP_CONFIGURATION = "hostgroup_configuration";

  protected static final String BLUEPRINT_NAME_COLUMN = "blueprint_name";
  protected static final String EXTENSION_NAME_COLUMN = "extension_name";
  protected static final String EXTENSION_VERSION_COLUMN = "extension_version";
  protected static final String USER_TYPE_COLUMN = "user_type";
  protected static final String USER_NAME_COLUMN = "user_name";
  protected static final String ROLE_COLUMN = "role";
  protected static final String STATUS_COLUMN = "status";
  protected static final String NAME_COLUMN = "name";


  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog242.class);




  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   *
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog242(Injector injector) {
    super(injector);
    this.injector = injector;
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.4.2";
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.4.0.2";
  }


  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    updateTablesForMysql();
  }

  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    addNewConfigurationsFromXml();
    createRoleAuthorizations();
    convertRolePrincipals();
  }

  /**
   * Create new role authorizations: CLUSTER.RUN_CUSTOM_COMMAND and AMBARI.RUN_CUSTOM_COMMAND
   *
   * @throws SQLException
   */
  @Transactional
  protected void createRoleAuthorizations() throws SQLException {
    addRoleAuthorization("CLUSTER.RUN_CUSTOM_COMMAND", "Perform custom cluster-level actions",
        Arrays.asList("AMBARI.ADMINISTRATOR:AMBARI", "CLUSTER.ADMINISTRATOR:CLUSTER"));

    addRoleAuthorization("AMBARI.RUN_CUSTOM_COMMAND", "Perform custom administrative actions",
        Collections.singletonList("AMBARI.ADMINISTRATOR:AMBARI"));
  }

  protected void updateTablesForMysql() throws SQLException {
    final Configuration.DatabaseType databaseType = configuration.getDatabaseType();
    if (databaseType == Configuration.DatabaseType.MYSQL) {
      dbAccessor.alterColumn(EXTENSION_TABLE, new DBAccessor.DBColumnInfo(EXTENSION_NAME_COLUMN, String.class, 100, null, false));
      dbAccessor.alterColumn(EXTENSION_TABLE, new DBAccessor.DBColumnInfo(EXTENSION_VERSION_COLUMN, String.class, 100, null, false));

      dbAccessor.alterColumn(USERS_TABLE, new DBAccessor.DBColumnInfo(USER_TYPE_COLUMN, String.class, 100, null, false));
      dbAccessor.alterColumn(USERS_TABLE, new DBAccessor.DBColumnInfo(USER_NAME_COLUMN, String.class, 100, null, false));

      dbAccessor.alterColumn(HOST_ROLE_COMMAND_TABLE, new DBAccessor.DBColumnInfo(ROLE_COLUMN, String.class, 100, null, true));
      dbAccessor.alterColumn(HOST_ROLE_COMMAND_TABLE, new DBAccessor.DBColumnInfo(STATUS_COLUMN, String.class, 100, null, true));

      dbAccessor.dropFKConstraint(HOST_GROUP_TABLE, "FK_hg_blueprint_name");

      dbAccessor.dropFKConstraint(HOST_GROUP_TABLE, "FK_hostgroup_blueprint_name");

      dbAccessor.dropFKConstraint(BLUEPRINT_CONFIGURATION, "FK_cfg_blueprint_name");

      dbAccessor.dropFKConstraint(BLUEPRINT_CONFIGURATION, "FK_blueprint_configuration_blueprint_name");

      dbAccessor.dropFKConstraint(BLUEPRINT_SETTING, "FK_blueprint_setting_blueprint_name");

      dbAccessor.dropFKConstraint(BLUEPRINT_SETTING, "FK_blueprint_setting_name");

      dbAccessor.alterColumn(BLUEPRINT_TABLE, new DBAccessor.DBColumnInfo(BLUEPRINT_NAME_COLUMN, String.class, 100, null, false));

      String[] uniqueColumns1 = new String[] { BLUEPRINT_NAME_COLUMN };

      dbAccessor.addFKConstraint(HOST_GROUP_TABLE, "FK_hg_blueprint_name",
              uniqueColumns1, BLUEPRINT_TABLE, uniqueColumns1, false);

      dbAccessor.addFKConstraint(BLUEPRINT_CONFIGURATION, "FK_cfg_blueprint_name",
              uniqueColumns1, BLUEPRINT_TABLE, uniqueColumns1, false);

      dbAccessor.addFKConstraint(BLUEPRINT_SETTING, "FK_blueprint_setting_name",
              uniqueColumns1, BLUEPRINT_TABLE, uniqueColumns1, false);
    }
  }

  /**
   * Convert the previously set inherited privileges to the more generic inherited privileges model
   * based on role-based principals rather than specialized principal types.
   */
  @Transactional
  void convertRolePrincipals() {
    LOG.info("Converting pseudo principle types to role principals");

    PermissionDAO permissionDAO = injector.getInstance(PermissionDAO.class);
    PrivilegeDAO privilegeDAO = injector.getInstance(PrivilegeDAO.class);
    PrincipalDAO principalDAO = injector.getInstance(PrincipalDAO.class);
    PrincipalTypeDAO principalTypeDAO = injector.getInstance(PrincipalTypeDAO.class);

    Map<String, String> principalTypeToRole = new HashMap<String, String>();
    principalTypeToRole.put("ALL.CLUSTER.ADMINISTRATOR", "CLUSTER.ADMINISTRATOR");
    principalTypeToRole.put("ALL.CLUSTER.OPERATOR", "CLUSTER.OPERATOR");
    principalTypeToRole.put("ALL.CLUSTER.USER", "CLUSTER.USER");
    principalTypeToRole.put("ALL.SERVICE.ADMINISTRATOR", "SERVICE.ADMINISTRATOR");
    principalTypeToRole.put("ALL.SERVICE.OPERATOR", "SERVICE.OPERATOR");

    // Handle a typo introduced in org.apache.ambari.server.upgrade.UpgradeCatalog240.updateClusterInheritedPermissionsConfig
    principalTypeToRole.put("ALL.SERVICE.OPERATIOR", "SERVICE.OPERATOR");

    for (Map.Entry<String, String> entry : principalTypeToRole.entrySet()) {
      String principalTypeName = entry.getKey();
      String roleName = entry.getValue();

      PermissionEntity role = permissionDAO.findByName(roleName);
      PrincipalEntity rolePrincipalEntity = (role == null) ? null : role.getPrincipal();

      // Convert Privilege Records
      PrincipalTypeEntity principalTypeEntity = principalTypeDAO.findByName(principalTypeName);

      if (principalTypeEntity != null) {
        List<PrincipalEntity> principalEntities = principalDAO.findByPrincipalType(principalTypeName);

        for (PrincipalEntity principalEntity : principalEntities) {
          Set<PrivilegeEntity> privilegeEntities = principalEntity.getPrivileges();

          for (PrivilegeEntity privilegeEntity : privilegeEntities) {
            if (rolePrincipalEntity == null) {
              LOG.info("Removing privilege (id={}) since no role principle was found for {}:\n{}",
                  privilegeEntity.getId(), roleName, formatPrivilegeEntityDetails(privilegeEntity));
              // Remove this privilege
              privilegeDAO.remove(privilegeEntity);
            } else {
              LOG.info("Updating privilege (id={}) to use role principle for {}:\n{}",
                  privilegeEntity.getId(), roleName, formatPrivilegeEntityDetails(privilegeEntity));

              // Set the principal to the updated principal value
              privilegeEntity.setPrincipal(rolePrincipalEntity);
              privilegeDAO.merge(privilegeEntity);
            }
          }

          // Remove the obsolete principal
          principalDAO.remove(principalEntity);
        }

        // Remove the obsolete principal type
        principalTypeDAO.remove(principalTypeEntity);
      }
    }

    LOG.info("Converting pseudo principle types to role principals - complete.");
  }

  private String formatPrivilegeEntityDetails(PrivilegeEntity privilegeEntity) {
    if (privilegeEntity == null) {
      return "";
    } else {
      ResourceEntity resource = privilegeEntity.getResource();
      PrincipalEntity principal = privilegeEntity.getPrincipal();
      PermissionEntity permission = privilegeEntity.getPermission();

      return String.format("" +
              "\tPrivilege ID: %d" +
              "\n\tResource ID: %d" +
              "\n\tPrincipal ID: %d" +
              "\n\tPermission ID: %d",
          privilegeEntity.getId(),
          resource.getId(),
          principal.getId(),
          permission.getId()
      );
    }
  }
}
