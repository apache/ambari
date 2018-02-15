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


import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.internal.AmbariServerConfigurationCategory;
import org.apache.ambari.server.controller.internal.CalculatedStatus;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfigurationKeys;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.security.authorization.UserAuthenticationType;
import org.apache.ambari.server.serveraction.kerberos.KerberosServerAction;
import org.apache.ambari.server.serveraction.kerberos.PrepareKerberosIdentitiesServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.common.net.HostAndPort;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class UpgradeCatalog270 extends AbstractUpgradeCatalog {

  public static final String HOST_ID_COLUMN = "host_id";
  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog270.class);

  protected static final String STAGE_TABLE = "stage";
  protected static final String STAGE_STATUS_COLUMN = "status";
  protected static final String STAGE_DISPLAY_STATUS_COLUMN = "display_status";
  protected static final String REQUEST_TABLE = "request";
  protected static final String REQUEST_DISPLAY_STATUS_COLUMN = "display_status";
  protected static final String HOST_ROLE_COMMAND_TABLE = "host_role_command";
  protected static final String HRC_OPS_DISPLAY_NAME_COLUMN = "ops_display_name";
  protected static final String COMPONENT_DESIRED_STATE_TABLE = "hostcomponentdesiredstate";
  protected static final String COMPONENT_STATE_TABLE = "hostcomponentstate";
  protected static final String SERVICE_DESIRED_STATE_TABLE = "servicedesiredstate";
  protected static final String SECURITY_STATE_COLUMN = "security_state";

  protected static final String AMBARI_CONFIGURATION_TABLE = "ambari_configuration";
  protected static final String AMBARI_CONFIGURATION_CATEGORY_NAME_COLUMN = "category_name";
  protected static final String AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN = "property_name";
  protected static final String AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN = "property_value";

  protected static final String USER_AUTHENTICATION_TABLE = "user_authentication";
  protected static final String USER_AUTHENTICATION_USER_AUTHENTICATION_ID_COLUMN = "user_authentication_id";
  protected static final String USER_AUTHENTICATION_USER_ID_COLUMN = "user_id";
  protected static final String USER_AUTHENTICATION_AUTHENTICATION_TYPE_COLUMN = "authentication_type";
  protected static final String USER_AUTHENTICATION_AUTHENTICATION_KEY_COLUMN = "authentication_key";
  protected static final String USER_AUTHENTICATION_CREATE_TIME_COLUMN = "create_time";
  protected static final String USER_AUTHENTICATION_UPDATE_TIME_COLUMN = "update_time";
  protected static final String USER_AUTHENTICATION_PRIMARY_KEY = "PK_user_authentication";
  protected static final String USER_AUTHENTICATION_USER_AUTHENTICATION_USER_ID_INDEX = "IDX_user_authentication_user_id";
  protected static final String USER_AUTHENTICATION_USER_AUTHENTICATION_USERS_FOREIGN_KEY = "FK_user_authentication_users";

  protected static final String USERS_TABLE = "users";
  protected static final String USERS_USER_ID_COLUMN = "user_id";
  protected static final String USERS_PRINCIPAL_ID_COLUMN = "principal_id";
  protected static final String USERS_USER_TYPE_COLUMN = "user_type";
  protected static final String USERS_USER_PASSWORD_COLUMN = "user_password";
  protected static final String USERS_CREATE_TIME_COLUMN = "create_time";
  protected static final String USERS_LDAP_USER_COLUMN = "ldap_user";
  protected static final String USERS_CONSECUTIVE_FAILURES_COLUMN = "consecutive_failures";
  protected static final String USERS_USER_NAME_COLUMN = "user_name";
  protected static final String USERS_DISPLAY_NAME_COLUMN = "display_name";
  protected static final String USERS_LOCAL_USERNAME_COLUMN = "local_username";
  protected static final String USERS_VERSION_COLUMN = "version";
  protected static final String UNIQUE_USERS_0_INDEX = "UNQ_users_0";

  protected static final String MEMBERS_TABLE = "members";
  protected static final String MEMBERS_MEMBER_ID_COLUMN = "member_id";
  protected static final String MEMBERS_GROUP_ID_COLUMN = "group_id";
  protected static final String MEMBERS_USER_ID_COLUMN = "user_id";

  protected static final String ADMINPRIVILEGE_TABLE = "adminprivilege";
  protected static final String ADMINPRIVILEGE_PRIVILEGE_ID_COLUMN = "privilege_id";
  protected static final String ADMINPRIVILEGE_PERMISSION_ID_COLUMN = "permission_id";
  protected static final String ADMINPRIVILEGE_RESOURCE_ID_COLUMN = "resource_id";
  protected static final String ADMINPRIVILEGE_PRINCIPAL_ID_COLUMN = "principal_id";

  // kerberos tables constants
  protected static final String KERBEROS_KEYTAB_TABLE = "kerberos_keytab";
  protected static final String KERBEROS_KEYTAB_PRINCIPAL_TABLE = "kerberos_keytab_principal";
  protected static final String KKP_MAPPING_SERVICE_TABLE = "kkp_mapping_service";
  protected static final String KEYTAB_PATH_FIELD = "keytab_path";
  protected static final String OWNER_NAME_FIELD = "owner_name";
  protected static final String OWNER_ACCESS_FIELD = "owner_access";
  protected static final String GROUP_NAME_FIELD = "group_name";
  protected static final String GROUP_ACCESS_FIELD = "group_access";
  protected static final String IS_AMBARI_KEYTAB_FIELD = "is_ambari_keytab";
  protected static final String WRITE_AMBARI_JAAS_FIELD = "write_ambari_jaas";
  protected static final String PK_KERBEROS_KEYTAB = "PK_kerberos_keytab";
  protected static final String KKP_ID_COLUMN = "kkp_id";
  protected static final String PRINCIPAL_NAME_COLUMN = "principal_name";
  protected static final String IS_DISTRIBUTED_COLUMN = "is_distributed";
  protected static final String PK_KKP = "PK_kkp";
  protected static final String UNI_KKP = "UNI_kkp";
  protected static final String SERVICE_NAME_COLUMN = "service_name";
  protected static final String COMPONENT_NAME_COLUMN = "component_name";
  protected static final String PK_KKP_MAPPING_SERVICE = "PK_kkp_mapping_service";
  protected static final String FK_KKP_KEYTAB_PATH = "FK_kkp_keytab_path";
  protected static final String FK_KKP_HOST_ID = "FK_kkp_host_id";
  protected static final String FK_KKP_PRINCIPAL_NAME = "FK_kkp_principal_name";
  protected static final String HOSTS_TABLE = "hosts";
  protected static final String KERBEROS_PRINCIPAL_TABLE = "kerberos_principal";
  protected static final String FK_KKP_SERVICE_PRINCIPAL = "FK_kkp_service_principal";
  protected static final String KKP_ID_SEQ_NAME = "kkp_id_seq";
  protected static final String KERBEROS_PRINCIPAL_HOST_TABLE = "kerberos_principal_host";

  @Inject
  DaoUtils daoUtils;

  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   *
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog270(Injector injector) {
    super(injector);

    daoUtils = injector.getInstance(DaoUtils.class);
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.7.0";
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.6.2";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    updateStageTable();
    addOpsDisplayNameColumnToHostRoleCommand();
    removeSecurityState();
    addAmbariConfigurationTable();
    upgradeUserTables();
    upgradeKerberosTables();
  }

  /**
   * Upgrade the users table as well as supporting tables.
   * <p>
   * Affected table are
   * <ul>
   * <li>users</li>
   * <li>user_authentication (new)</li>
   * <li>members</li>
   * <li>adminprivilege</li>
   * </ul>
   *
   * @throws SQLException if an error occurs while executing SQL statements
   * @see #createUserAuthenticationTable()
   * @see #updateGroupMembershipRecords()
   * @see #updateAdminPrivilegeRecords()
   * @see #updateUsersTable()
   */
  protected void upgradeUserTables() throws SQLException {
    createUserAuthenticationTable();
    updateGroupMembershipRecords();
    updateAdminPrivilegeRecords();
    updateUsersTable();
  }

  /**
   * If the <code>users</code> table has not yet been migrated, create the <code>user_authentication</code>
   * table and generate relevant records for that table based on data in the <code>users</code> table.
   * <p>
   * The records in the new <code>user_authentication</code> table represent all of the types associated
   * with a given (case-insensitive) username.  If <code>UserA:LOCAL</code>, <code>usera:LOCAL</code> and
   * <code>usera:LDAP</code> exist in the original <code>users</code> table, three records will be created
   * in the <code>user_authentication</code> table: one for each t
   * to <code>Role1</code>, the three <code>adminprivilege</code> records will be merged into a single
   * record for <code>usera</code>.
   *
   * @throws SQLException if an error occurs while executing SQL statements
   */
  private void createUserAuthenticationTable() throws SQLException {
    if (!usersTableUpgraded()) {
      final String temporaryTable = USER_AUTHENTICATION_TABLE + "_tmp";

      List<DBAccessor.DBColumnInfo> columns = new ArrayList<>();
      columns.add(new DBAccessor.DBColumnInfo(USER_AUTHENTICATION_USER_AUTHENTICATION_ID_COLUMN, Long.class, null, null, false));
      columns.add(new DBAccessor.DBColumnInfo(USER_AUTHENTICATION_USER_ID_COLUMN, Long.class, null, null, false));
      columns.add(new DBAccessor.DBColumnInfo(USER_AUTHENTICATION_AUTHENTICATION_TYPE_COLUMN, String.class, 50, null, false));
      columns.add(new DBAccessor.DBColumnInfo(USER_AUTHENTICATION_AUTHENTICATION_KEY_COLUMN, Clob.class, null, null, true));
      columns.add(new DBAccessor.DBColumnInfo(USER_AUTHENTICATION_CREATE_TIME_COLUMN, Timestamp.class, null, null, true));
      columns.add(new DBAccessor.DBColumnInfo(USER_AUTHENTICATION_UPDATE_TIME_COLUMN, Timestamp.class, null, null, true));

      // Make sure the temporary table does not exist
      dbAccessor.dropTable(temporaryTable);

      // Create temporary table
      dbAccessor.createTable(temporaryTable, columns);

      dbAccessor.executeUpdate(
        "insert into " + temporaryTable +
          "(" + USER_AUTHENTICATION_USER_AUTHENTICATION_ID_COLUMN + ", " + USER_AUTHENTICATION_USER_ID_COLUMN + ", " + USER_AUTHENTICATION_AUTHENTICATION_TYPE_COLUMN + ", " + USER_AUTHENTICATION_AUTHENTICATION_KEY_COLUMN + ", " + USER_AUTHENTICATION_CREATE_TIME_COLUMN + ", " + USER_AUTHENTICATION_UPDATE_TIME_COLUMN + ")" +
          " select" +
          "  u." + USERS_USER_ID_COLUMN + "," +
          "  t.min_user_id," +
          "  u." + USERS_USER_TYPE_COLUMN + "," +
          "  u." + USERS_USER_PASSWORD_COLUMN + "," +
          "  u." + USERS_CREATE_TIME_COLUMN + "," +
          "  u." + USERS_CREATE_TIME_COLUMN +
          " from " + USERS_TABLE + " as u inner join" +
          "   (select" +
          "     lower(" + USERS_USER_NAME_COLUMN + ") as " + USERS_USER_NAME_COLUMN + "," +
          "     min(" + USERS_USER_ID_COLUMN + ") as min_user_id" +
          "    from " + USERS_TABLE +
          "    group by lower(" + USERS_USER_NAME_COLUMN + ")) as t" +
          " on (lower(u." + USERS_USER_NAME_COLUMN + ") = lower(t." + USERS_USER_NAME_COLUMN + "))"
      );

      // Ensure only LOCAL users have keys set in the user_authentication table
      dbAccessor.executeUpdate("update " + temporaryTable +
        " set " + USER_AUTHENTICATION_AUTHENTICATION_KEY_COLUMN + "=null" +
        " where " + USER_AUTHENTICATION_AUTHENTICATION_TYPE_COLUMN + "!='" + UserAuthenticationType.LOCAL.name() + "'");

      dbAccessor.createTable(USER_AUTHENTICATION_TABLE, columns);
      dbAccessor.addPKConstraint(USER_AUTHENTICATION_TABLE, USER_AUTHENTICATION_PRIMARY_KEY, USER_AUTHENTICATION_USER_AUTHENTICATION_ID_COLUMN);
      dbAccessor.addFKConstraint(USER_AUTHENTICATION_TABLE, USER_AUTHENTICATION_USER_AUTHENTICATION_USERS_FOREIGN_KEY, USER_AUTHENTICATION_USER_ID_COLUMN, USERS_TABLE, USERS_USER_ID_COLUMN, false);

      dbAccessor.executeUpdate(
        "insert into " + USER_AUTHENTICATION_TABLE +
          "(" + USER_AUTHENTICATION_USER_AUTHENTICATION_ID_COLUMN + ", " + USER_AUTHENTICATION_USER_ID_COLUMN + ", " + USER_AUTHENTICATION_AUTHENTICATION_TYPE_COLUMN + ", " + USER_AUTHENTICATION_AUTHENTICATION_KEY_COLUMN + ", " + USER_AUTHENTICATION_CREATE_TIME_COLUMN + ", " + USER_AUTHENTICATION_UPDATE_TIME_COLUMN + ")" +
          " select distinct " +
          USER_AUTHENTICATION_USER_AUTHENTICATION_ID_COLUMN + ", " + USER_AUTHENTICATION_USER_ID_COLUMN + ", " + USER_AUTHENTICATION_AUTHENTICATION_TYPE_COLUMN + ", " + USER_AUTHENTICATION_AUTHENTICATION_KEY_COLUMN + ", " + USER_AUTHENTICATION_CREATE_TIME_COLUMN + ", " + USER_AUTHENTICATION_UPDATE_TIME_COLUMN +
          " from " + temporaryTable
      );

      // Delete the temporary table
      dbAccessor.dropTable(temporaryTable);
    }
  }

  private boolean usersTableUpgraded() {
    try {
      dbAccessor.getColumnType(USERS_TABLE, USERS_USER_TYPE_COLUMN);
      return false;
    } catch (SQLException e) {
      return true;
    }
  }

  /**
   * Update the <code>users</code> table by adjusting the relevant columns, contained data, and indicies.
   * <p>
   * This method should be executed after creating the <code>user_authentication</code> table and
   * adjusting the <code>members</code> and <code>adminprivilege</code> data by merging data while
   * combine user entries with the same username (but different type).
   * <p>
   * <ol>
   * <li>
   * Orphaned data is removed.  These will be the records where the usernamne is duplicated but
   * the user type is different.  Only a single record with a given username should be left.
   * </li>
   * <li>
   * Remove the unique record constraint so it may be added back later declaring new constraints
   * </li>
   * <li>
   * Obsolete columns are removed: <code>user_type</code>, <code>ldap_user</code>, <code>user_password</code>.
   * These columns are handled by the <codee>user_authentication</codee> table.
   * </li>
   * <li>
   * Add new columns: <code>consecutive_failures</code>, <code>display_name</code>,
   * <code>local_username</code>, <code>version</code>.
   * The non-null constraints are to be set after all the date is set properly.
   * </li>
   * <li>
   * Ensure the <code>display_name</code> and <code>local_username</code> columns have properly set data.
   * </li>
   * <li>
   * Add the non-null constraint back for the <code>display_name</code> and <code>local_username</code> columns.
   * </li>
   * <li>
   * Add a unique index on the <code>user_name</code> column
   * </li>
   * </ol>
   *
   * @throws SQLException if an error occurs while executing SQL statements
   * @see #createUserAuthenticationTable()
   * @see #updateGroupMembershipRecords()
   * @see #updateAdminPrivilegeRecords()
   */
  private void updateUsersTable() throws SQLException {
    // Remove orphaned user records...
    dbAccessor.executeUpdate("delete from " + USERS_TABLE +
      " where " + USERS_USER_ID_COLUMN + " not in (select " + USER_AUTHENTICATION_USER_ID_COLUMN + " from " + USER_AUTHENTICATION_TABLE + ")");

    // Update the users table
    dbAccessor.dropUniqueConstraint(USERS_TABLE, UNIQUE_USERS_0_INDEX);
    dbAccessor.dropColumn(USERS_TABLE, USERS_USER_TYPE_COLUMN);
    dbAccessor.dropColumn(USERS_TABLE, USERS_LDAP_USER_COLUMN);
    dbAccessor.dropColumn(USERS_TABLE, USERS_USER_PASSWORD_COLUMN);
    dbAccessor.addColumn(USERS_TABLE, new DBAccessor.DBColumnInfo(USERS_CONSECUTIVE_FAILURES_COLUMN, Integer.class, null, 0, false));
    dbAccessor.addColumn(USERS_TABLE, new DBAccessor.DBColumnInfo(USERS_DISPLAY_NAME_COLUMN, String.class, 255, null, true)); // Set to non-null later
    dbAccessor.addColumn(USERS_TABLE, new DBAccessor.DBColumnInfo(USERS_LOCAL_USERNAME_COLUMN, String.class, 255, null, true)); // Set to non-null later
    dbAccessor.addColumn(USERS_TABLE, new DBAccessor.DBColumnInfo(USERS_VERSION_COLUMN, Long.class, null, 0, false));

    // Set the display name and local username values based on the username value
    dbAccessor.executeUpdate("update " + USERS_TABLE +
      " set " + USERS_DISPLAY_NAME_COLUMN + "=" + USERS_USER_NAME_COLUMN +
      ", " + USERS_LOCAL_USERNAME_COLUMN + "= lower(" + USERS_USER_NAME_COLUMN + ")" +
      ", " + USERS_USER_NAME_COLUMN + "= lower(" + USERS_USER_NAME_COLUMN + ")");

    // Change columns to non-null
    dbAccessor.alterColumn(USERS_TABLE, new DBAccessor.DBColumnInfo(USERS_DISPLAY_NAME_COLUMN, String.class, 255, null, false));
    dbAccessor.alterColumn(USERS_TABLE, new DBAccessor.DBColumnInfo(USERS_LOCAL_USERNAME_COLUMN, String.class, 255, null, false));

    // Add a unique constraint on the user_name column
    dbAccessor.addUniqueConstraint(USERS_TABLE, UNIQUE_USERS_0_INDEX, USERS_USER_NAME_COLUMN);
  }

  /**
   * Update the <code>members</code> table to ensure records for the same username but different user
   * records are referencing the main user record. Duplicate records will be be ignored when updating
   * the <code>members</code> table.
   * <p>
   * If <code>UserA:LOCAL</code>, <code>usera:LOCAL</code> and <code>usera:LDAP</code> all belong to
   * <code>Group1</code>, the three <code>members</code> records will be merged into a single record
   * for <code>usera</code>.
   * <p>
   * This method may be executed multiple times and will yield the same results each time.
   *
   * @throws SQLException if an error occurs while executing SQL statements
   */
  private void updateGroupMembershipRecords() throws SQLException {
    final String temporaryTable = MEMBERS_TABLE + "_tmp";

    // Make sure the temporary table does not exist
    dbAccessor.dropTable(temporaryTable);

    // Create temporary table
    List<DBAccessor.DBColumnInfo> columns = new ArrayList<>();
    columns.add(new DBAccessor.DBColumnInfo(MEMBERS_MEMBER_ID_COLUMN, Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo(MEMBERS_USER_ID_COLUMN, Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo(MEMBERS_GROUP_ID_COLUMN, Long.class, null, null, false));
    dbAccessor.createTable(temporaryTable, columns);

    // Insert updated data
    /* *******
     * Find the user id for the merged user records for the user that is related to each member record.
     * - Using the user_id from the original member record, find the user_name of that user.
     * - Using the found user_name, find the user_id for the _merged_ record.  This will be the value of the
     *   smallest user_id for all user_ids where the user_name matches that found user_name.
     * - The user_name value is case-insensitive.
     * ******* */
    dbAccessor.executeUpdate(
      "insert into " + temporaryTable + " (" + MEMBERS_MEMBER_ID_COLUMN + ", " + MEMBERS_USER_ID_COLUMN + ", " + MEMBERS_GROUP_ID_COLUMN + ")" +
        "  select" +
        "    m." + MEMBERS_MEMBER_ID_COLUMN + "," +
        "    u.min_user_id," +
        "    m." + MEMBERS_GROUP_ID_COLUMN +
        "  from " + MEMBERS_TABLE + " as m inner join" +
        "    (" +
        "      select" +
        "        iu." + USERS_USER_NAME_COLUMN + "," +
        "        iu." + USERS_USER_ID_COLUMN + "," +
        "        t.min_user_id" +
        "      from " + USERS_TABLE + " iu inner join" +
        "        (" +
        "          select" +
        "           lower(" + USERS_USER_NAME_COLUMN + ") as " + USERS_USER_NAME_COLUMN + "," +
        "            min(" + USERS_USER_ID_COLUMN + ") as min_user_id" +
        "          from " + USERS_TABLE +
        "          group by lower(" + USERS_USER_NAME_COLUMN + ")" +
        "        ) as t on (lower(t." + USERS_USER_NAME_COLUMN + ") = lower(iu." + USERS_USER_NAME_COLUMN + "))" +
        "    ) as u on (m." + MEMBERS_USER_ID_COLUMN + " = u." + USERS_USER_ID_COLUMN + ")");

    // Truncate existing membership records
    dbAccessor.truncateTable(MEMBERS_TABLE);

    // Insert temporary records into members table
    /*
     * Copy the generated data to the original <code>members</code> table, effectively skipping
     * duplicate records.
     */
    dbAccessor.executeUpdate(
      "insert into " + MEMBERS_TABLE + " (" + MEMBERS_MEMBER_ID_COLUMN + ", " + MEMBERS_USER_ID_COLUMN + ", " + MEMBERS_GROUP_ID_COLUMN + ")" +
        "  select " +
        "    min(" + MEMBERS_MEMBER_ID_COLUMN + ")," +
        "    " + MEMBERS_USER_ID_COLUMN + "," +
        "    " + MEMBERS_GROUP_ID_COLUMN +
        "  from " + temporaryTable +
        "  group by " + MEMBERS_USER_ID_COLUMN + ", " + MEMBERS_GROUP_ID_COLUMN);

    // Delete the temporary table
    dbAccessor.dropTable(temporaryTable);
  }

  /**
   * Update the <code>adminprivilege</code> table to ensure records for the same username but different user
   * records are referencing the main user record. Duplicate records will be be ignored when updating
   * the <code>adminprivilege</code> table.
   * <p>
   * If <code>UserA:LOCAL</code>, <code>usera:LOCAL</code> and <code>usera:LDAP</code> are assigned
   * to <code>Role1</code>, the three <code>adminprivilege</code> records will be merged into a single
   * record for <code>usera</code>.
   * <p>
   * This method may be executed multiple times and will yield the same results each time.
   *
   * @throws SQLException if an error occurs while executing SQL statements
   */
  private void updateAdminPrivilegeRecords() throws SQLException {
    final String temporaryTable = ADMINPRIVILEGE_TABLE + "_tmp";

    // Make sure the temporary table does not exist
    dbAccessor.dropTable(temporaryTable);

    // Create temporary table
    List<DBAccessor.DBColumnInfo> columns = new ArrayList<>();
    columns.add(new DBAccessor.DBColumnInfo(ADMINPRIVILEGE_PRIVILEGE_ID_COLUMN, Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo(ADMINPRIVILEGE_PERMISSION_ID_COLUMN, Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo(ADMINPRIVILEGE_RESOURCE_ID_COLUMN, Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo(ADMINPRIVILEGE_PRINCIPAL_ID_COLUMN, Long.class, null, null, false));
    dbAccessor.createTable(temporaryTable, columns);

    // Insert updated data
    /* *******
     * Find the principal id for the merged user records for the user that is related to each relevant
     * adminprivilege record.
     * - Using the principal_id from the original adminprivilege record, find the user_name of that user.
     * - Using the found user_name, find the user_id for the _merged_ record.  This will be the value of the
     *   smallest user_id for all user_ids where the user_name matches that found user_name.
     * - Using the found user_id, obtain the relevant principal_id
     * - The user_name value is case-insensitive.
     * ******* */
    dbAccessor.executeUpdate(
      "insert into " + temporaryTable + " (" + ADMINPRIVILEGE_PRIVILEGE_ID_COLUMN + ", " + ADMINPRIVILEGE_PERMISSION_ID_COLUMN + ", " + ADMINPRIVILEGE_RESOURCE_ID_COLUMN + ", " + ADMINPRIVILEGE_PRINCIPAL_ID_COLUMN + ")" +
        "  select" +
        "    ap." + ADMINPRIVILEGE_PRIVILEGE_ID_COLUMN + "," +
        "    ap." + ADMINPRIVILEGE_PERMISSION_ID_COLUMN + "," +
        "    ap." + ADMINPRIVILEGE_RESOURCE_ID_COLUMN + "," +
        "    ap." + ADMINPRIVILEGE_PRINCIPAL_ID_COLUMN +
        "  from " + ADMINPRIVILEGE_TABLE + " as ap" +
        "  where ap." + ADMINPRIVILEGE_PRINCIPAL_ID_COLUMN + " not in" +
        "        (" +
        "          select " + USERS_PRINCIPAL_ID_COLUMN +
        "          from " + USERS_TABLE +
        "        )" +
        "  union" +
        "  select" +
        "    ap." + ADMINPRIVILEGE_PRIVILEGE_ID_COLUMN + "," +
        "    ap." + ADMINPRIVILEGE_PERMISSION_ID_COLUMN + "," +
        "    ap." + ADMINPRIVILEGE_RESOURCE_ID_COLUMN + "," +
        "    t.new_principal_id" +
        "  from " + ADMINPRIVILEGE_TABLE + " as ap inner join" +
        "    (" +
        "      select" +
        "        u." + USERS_USER_ID_COLUMN + "," +
        "        u." + USERS_USER_NAME_COLUMN + "," +
        "        u." + USERS_PRINCIPAL_ID_COLUMN + " as new_principal_id," +
        "        t1." + USERS_PRINCIPAL_ID_COLUMN + " as orig_principal_id" +
        "      from " + USERS_TABLE + " as u inner join" +
        "        (" +
        "          select" +
        "            u1." + USERS_USER_NAME_COLUMN + "," +
        "            u1." + USERS_PRINCIPAL_ID_COLUMN + "," +
        "            t2.min_user_id" +
        "          from " + USERS_TABLE + " as u1 inner join" +
        "            (" +
        "              select" +
        "                lower(" + USERS_USER_NAME_COLUMN + ") as " + USERS_USER_NAME_COLUMN + "," +
        "                min(" + USERS_USER_ID_COLUMN + ") as min_user_id" +
        "              from " + USERS_TABLE +
        "              group by lower(" + USERS_USER_NAME_COLUMN + ")" +
        "            ) as t2 on (lower(u1." + USERS_USER_NAME_COLUMN + ") = lower(t2." + USERS_USER_NAME_COLUMN + "))" +
        "        ) as t1 on (u." + USERS_USER_ID_COLUMN + " = t1.min_user_id)" +
        "    ) as t on (ap." + ADMINPRIVILEGE_PRINCIPAL_ID_COLUMN + " = t.orig_principal_id);");

    // Truncate existing adminprivilege records
    dbAccessor.truncateTable(ADMINPRIVILEGE_TABLE);

    // Insert temporary records into adminprivilege table
    /*
     * Copy the generated data to the original <code>adminprivilege</code> table, effectively skipping
     * duplicate records.
     */
    dbAccessor.executeUpdate(
      "insert into " + ADMINPRIVILEGE_TABLE + " (" + ADMINPRIVILEGE_PRIVILEGE_ID_COLUMN + ", " + ADMINPRIVILEGE_PERMISSION_ID_COLUMN + ", " + ADMINPRIVILEGE_RESOURCE_ID_COLUMN + ", " + ADMINPRIVILEGE_PRINCIPAL_ID_COLUMN + ")" +
        "  select " +
        "    min(" + ADMINPRIVILEGE_PRIVILEGE_ID_COLUMN + ")," +
        "    " + ADMINPRIVILEGE_PERMISSION_ID_COLUMN + "," +
        "    " + ADMINPRIVILEGE_RESOURCE_ID_COLUMN + "," +
        "    " + ADMINPRIVILEGE_PRINCIPAL_ID_COLUMN +
        "  from " + temporaryTable +
        "  group by " + ADMINPRIVILEGE_PERMISSION_ID_COLUMN + ", " + ADMINPRIVILEGE_RESOURCE_ID_COLUMN + ", " + ADMINPRIVILEGE_PRINCIPAL_ID_COLUMN);

    // Delete the temporary table
    dbAccessor.dropTable(temporaryTable);
  }

  protected void updateStageTable() throws SQLException {
    dbAccessor.addColumn(STAGE_TABLE,
      new DBAccessor.DBColumnInfo(STAGE_STATUS_COLUMN, String.class, 255, HostRoleStatus.PENDING, false));
    dbAccessor.addColumn(STAGE_TABLE,
      new DBAccessor.DBColumnInfo(STAGE_DISPLAY_STATUS_COLUMN, String.class, 255, HostRoleStatus.PENDING, false));
    dbAccessor.addColumn(REQUEST_TABLE,
      new DBAccessor.DBColumnInfo(REQUEST_DISPLAY_STATUS_COLUMN, String.class, 255, HostRoleStatus.PENDING, false));
  }

  protected void addAmbariConfigurationTable() throws SQLException {
    List<DBAccessor.DBColumnInfo> columns = new ArrayList<>();
    columns.add(new DBAccessor.DBColumnInfo(AMBARI_CONFIGURATION_CATEGORY_NAME_COLUMN, String.class, 100, null, false));
    columns.add(new DBAccessor.DBColumnInfo(AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN, String.class, 100, null, false));
    columns.add(new DBAccessor.DBColumnInfo(AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN, String.class, 255, null, true));

    dbAccessor.createTable(AMBARI_CONFIGURATION_TABLE, columns);
    dbAccessor.addPKConstraint(AMBARI_CONFIGURATION_TABLE, "PK_ambari_configuration", AMBARI_CONFIGURATION_CATEGORY_NAME_COLUMN, AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN);
  }

  /**
   * Creates new tables for changed kerberos data.
   *
   * @throws SQLException
   */
  protected void upgradeKerberosTables() throws SQLException {
    List<DBAccessor.DBColumnInfo> kerberosKeytabColumns = new ArrayList<>();
    kerberosKeytabColumns.add(new DBAccessor.DBColumnInfo(KEYTAB_PATH_FIELD, String.class, 255, null, false));
    kerberosKeytabColumns.add(new DBAccessor.DBColumnInfo(OWNER_NAME_FIELD, String.class, 255, null, true));
    kerberosKeytabColumns.add(new DBAccessor.DBColumnInfo(OWNER_ACCESS_FIELD, String.class, 255, null, true));
    kerberosKeytabColumns.add(new DBAccessor.DBColumnInfo(GROUP_NAME_FIELD, String.class, 255, null, true));
    kerberosKeytabColumns.add(new DBAccessor.DBColumnInfo(GROUP_ACCESS_FIELD, String.class, 255, null, true));
    kerberosKeytabColumns.add(new DBAccessor.DBColumnInfo(IS_AMBARI_KEYTAB_FIELD, Integer.class, null, 0, false));
    kerberosKeytabColumns.add(new DBAccessor.DBColumnInfo(WRITE_AMBARI_JAAS_FIELD, Integer.class, null, 0, false));
    dbAccessor.createTable(KERBEROS_KEYTAB_TABLE, kerberosKeytabColumns);
    dbAccessor.addPKConstraint(KERBEROS_KEYTAB_TABLE, PK_KERBEROS_KEYTAB, KEYTAB_PATH_FIELD);

    List<DBAccessor.DBColumnInfo> kkpColumns = new ArrayList<>();
    kkpColumns.add(new DBAccessor.DBColumnInfo(KKP_ID_COLUMN, Long.class, null, 0L, false));
    kkpColumns.add(new DBAccessor.DBColumnInfo(KEYTAB_PATH_FIELD, String.class, 255, null, false));
    kkpColumns.add(new DBAccessor.DBColumnInfo(PRINCIPAL_NAME_COLUMN, String.class, 255, null, false));
    kkpColumns.add(new DBAccessor.DBColumnInfo(HOST_ID_COLUMN, Long.class, null, null, true));
    kkpColumns.add(new DBAccessor.DBColumnInfo(IS_DISTRIBUTED_COLUMN, Integer.class, null, 0, false));
    dbAccessor.createTable(KERBEROS_KEYTAB_PRINCIPAL_TABLE, kkpColumns);
    dbAccessor.addPKConstraint(KERBEROS_KEYTAB_PRINCIPAL_TABLE, PK_KKP, KKP_ID_COLUMN);
    dbAccessor.addUniqueConstraint(KERBEROS_KEYTAB_PRINCIPAL_TABLE, UNI_KKP, KEYTAB_PATH_FIELD, PRINCIPAL_NAME_COLUMN, HOST_ID_COLUMN);

    List<DBAccessor.DBColumnInfo> kkpMappingColumns = new ArrayList<>();
    kkpMappingColumns.add(new DBAccessor.DBColumnInfo(KKP_ID_COLUMN, Long.class, null, 0L, false));
    kkpMappingColumns.add(new DBAccessor.DBColumnInfo(SERVICE_NAME_COLUMN, String.class, 255, null, false));
    kkpMappingColumns.add(new DBAccessor.DBColumnInfo(COMPONENT_NAME_COLUMN, String.class, 255, null, false));
    dbAccessor.createTable(KKP_MAPPING_SERVICE_TABLE, kkpMappingColumns);
    dbAccessor.addPKConstraint(KKP_MAPPING_SERVICE_TABLE, PK_KKP_MAPPING_SERVICE, KKP_ID_COLUMN, SERVICE_NAME_COLUMN, COMPONENT_NAME_COLUMN);


    //  cross tables constraints
    dbAccessor.addFKConstraint(KERBEROS_KEYTAB_PRINCIPAL_TABLE, FK_KKP_KEYTAB_PATH, KEYTAB_PATH_FIELD, KERBEROS_KEYTAB_TABLE, KEYTAB_PATH_FIELD, false);
    dbAccessor.addFKConstraint(KERBEROS_KEYTAB_PRINCIPAL_TABLE, FK_KKP_HOST_ID, HOST_ID_COLUMN, HOSTS_TABLE, HOST_ID_COLUMN, false);
    dbAccessor.addFKConstraint(KERBEROS_KEYTAB_PRINCIPAL_TABLE, FK_KKP_PRINCIPAL_NAME, PRINCIPAL_NAME_COLUMN, KERBEROS_PRINCIPAL_TABLE, PRINCIPAL_NAME_COLUMN, false);
    dbAccessor.addFKConstraint(KKP_MAPPING_SERVICE_TABLE, FK_KKP_SERVICE_PRINCIPAL, KKP_ID_COLUMN, KERBEROS_KEYTAB_PRINCIPAL_TABLE, KKP_ID_COLUMN, false);

    addSequence(KKP_ID_SEQ_NAME, 0L, false);
    dbAccessor.dropTable(KERBEROS_PRINCIPAL_HOST_TABLE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    addNewConfigurationsFromXml();
    showHcatDeletedUserMessage();
    setStatusOfStagesAndRequests();
    updateLogSearchConfigs();
    updateKerberosConfigurations();
    upgradeLdapConfiguration();
    createRoleAuthorizations();
    addUserAuthenticationSequence();
  }

  protected void addUserAuthenticationSequence() throws SQLException {
    final long maxUserAuthenticationId = fetchMaxId(USER_AUTHENTICATION_TABLE, USER_AUTHENTICATION_USER_AUTHENTICATION_ID_COLUMN);
    LOG.info("Maximum user authentication ID = " + maxUserAuthenticationId);
    addSequence("user_authentication_id_seq", maxUserAuthenticationId + 1, false);
  }

  protected void createRoleAuthorizations() throws SQLException {
    addRoleAuthorization("AMBARI.MANAGE_CONFIGURATION",
      "Manage ambari configuration",
      Collections.singleton("AMBARI.ADMINISTRATOR:AMBARI"));
  }

  protected void showHcatDeletedUserMessage() {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
      for (final Cluster cluster : clusterMap.values()) {
        Config hiveEnvConfig = cluster.getDesiredConfigByType("hive-env");
        if (hiveEnvConfig != null) {
          Map<String, String> hiveEnvProperties = hiveEnvConfig.getProperties();
          String webhcatUser = hiveEnvProperties.get("webhcat_user");
          String hcatUser = hiveEnvProperties.get("hcat_user");
          if (!StringUtils.equals(webhcatUser, hcatUser)) {
            System.out.print("WARNING: In hive-env config, webhcat and hcat user are different. In current ambari release (3.0.0), hcat user was removed from stack, so potentially you could have some problems.");
            LOG.warn("In hive-env config, webhcat and hcat user are different. In current ambari release (3.0.0), hcat user was removed from stack, so potentially you could have some problems.");
          }
        }
      }
    }

  }

  protected void setStatusOfStagesAndRequests() {
    executeInTransaction(new Runnable() {
      @Override
      public void run() {
        try {
          RequestDAO requestDAO = injector.getInstance(RequestDAO.class);
          StageFactory stageFactory = injector.getInstance(StageFactory.class);
          EntityManager em = getEntityManagerProvider().get();
          List<RequestEntity> requestEntities = requestDAO.findAll();
          for (RequestEntity requestEntity : requestEntities) {
            Collection<StageEntity> stageEntities = requestEntity.getStages();
            List<HostRoleStatus> stageDisplayStatuses = new ArrayList<>();
            List<HostRoleStatus> stageStatuses = new ArrayList<>();
            for (StageEntity stageEntity : stageEntities) {
              Stage stage = stageFactory.createExisting(stageEntity);
              List<HostRoleCommand> hostRoleCommands = stage.getOrderedHostRoleCommands();
              Map<HostRoleStatus, Integer> statusCount = CalculatedStatus.calculateStatusCountsForTasks(hostRoleCommands);
              HostRoleStatus stageDisplayStatus = CalculatedStatus.calculateSummaryDisplayStatus(statusCount, hostRoleCommands.size(), stage.isSkippable());
              HostRoleStatus stageStatus = CalculatedStatus.calculateStageStatus(hostRoleCommands, statusCount, stage.getSuccessFactors(), stage.isSkippable());
              stageEntity.setStatus(stageStatus);
              stageStatuses.add(stageStatus);
              stageEntity.setDisplayStatus(stageDisplayStatus);
              stageDisplayStatuses.add(stageDisplayStatus);
              em.merge(stageEntity);
            }
            HostRoleStatus requestStatus = CalculatedStatus.getOverallStatusForRequest(stageStatuses);
            requestEntity.setStatus(requestStatus);
            HostRoleStatus requestDisplayStatus = CalculatedStatus.getOverallDisplayStatusForRequest(stageDisplayStatuses);
            requestEntity.setDisplayStatus(requestDisplayStatus);
            em.merge(requestEntity);
          }
        } catch (Exception e) {
          LOG.warn("Setting status for stages and Requests threw exception. ", e);
        }
      }
    });
  }

  /**
   * Adds the {@value #HRC_OPS_DISPLAY_NAME_COLUMN} column to the
   * {@value #HOST_ROLE_COMMAND_TABLE} table.
   *
   * @throws SQLException
   */
  private void addOpsDisplayNameColumnToHostRoleCommand() throws SQLException {
    dbAccessor.addColumn(HOST_ROLE_COMMAND_TABLE,
      new DBAccessor.DBColumnInfo(HRC_OPS_DISPLAY_NAME_COLUMN, String.class, 255, null, true));
  }

  private void removeSecurityState() throws SQLException {
    dbAccessor.dropColumn(COMPONENT_DESIRED_STATE_TABLE, SECURITY_STATE_COLUMN);
    dbAccessor.dropColumn(COMPONENT_STATE_TABLE, SECURITY_STATE_COLUMN);
    dbAccessor.dropColumn(SERVICE_DESIRED_STATE_TABLE, SECURITY_STATE_COLUMN);
  }

  /**
   * Updates Log Search configs.
   *
   * @throws AmbariException
   */
  protected void updateLogSearchConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);
      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Collection<Config> configs = cluster.getAllConfigs();
          for (Config config : configs) {
            String configType = config.getType();
            if (configType.endsWith("-logsearch-conf")) {
              configHelper.removeConfigsByType(cluster, configType);
            }
          }

          Config logSearchEnv = cluster.getDesiredConfigByType("logsearch-env");

          String oldProtocolProperty = null;
          String oldPortProperty = null;
          if (logSearchEnv != null) {
            oldProtocolProperty = logSearchEnv.getProperties().get("logsearch_ui_port");
            oldPortProperty = logSearchEnv.getProperties().get("logsearch_ui_protocol");
          }

          Config logSearchProperties = cluster.getDesiredConfigByType("logsearch-properties");
          Config logFeederProperties = cluster.getDesiredConfigByType("logfeeder-properties");
          if (logSearchProperties != null && logFeederProperties != null) {
            configHelper.createConfigType(cluster, cluster.getDesiredStackVersion(), ambariManagementController,
              "logsearch-common-properties", Collections.emptyMap(), "ambari-upgrade",
              String.format("Updated logsearch-common-properties during Ambari Upgrade from %s to %s",
                getSourceVersion(), getTargetVersion()));

            String defaultLogLevels = logSearchProperties.getProperties().get("logsearch.logfeeder.include.default.level");

            Set<String> removeProperties = Sets.newHashSet("logsearch.logfeeder.include.default.level");
            removeConfigurationPropertiesFromCluster(cluster, "logsearch-properties", removeProperties);

            Map<String, String> newLogSearchProperties = new HashMap<>();
            if (oldProtocolProperty != null) {
              newLogSearchProperties.put("logsearch.protocol", oldProtocolProperty);
            }

            if (oldPortProperty != null) {
              newLogSearchProperties.put("logsearch.http.port", oldPortProperty);
              newLogSearchProperties.put("logsearch.https.port", oldPortProperty);
            }
            if (!newLogSearchProperties.isEmpty()) {
              updateConfigurationPropertiesForCluster(cluster, "logsearch-properties", newLogSearchProperties, true, true);
            }

            Map<String, String> newLogfeederProperties = new HashMap<>();
            newLogfeederProperties.put("logfeeder.include.default.level", defaultLogLevels);
            updateConfigurationPropertiesForCluster(cluster, "logfeeder-properties", newLogfeederProperties, true, true);
          }

          Config logFeederLog4jProperties = cluster.getDesiredConfigByType("logfeeder-log4j");
          if (logFeederLog4jProperties != null) {
            String content = logFeederLog4jProperties.getProperties().get("content");
            if (content.contains("<!DOCTYPE log4j:configuration SYSTEM \"log4j.dtd\">")) {
              content = content.replace("<!DOCTYPE log4j:configuration SYSTEM \"log4j.dtd\">", "<!DOCTYPE log4j:configuration SYSTEM \"http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/xml/doc-files/log4j.dtd\">");
              updateConfigurationPropertiesForCluster(cluster, "logfeeder-log4j", Collections.singletonMap("content", content), true, true);
            }
          }

          Config logSearchLog4jProperties = cluster.getDesiredConfigByType("logsearch-log4j");
          if (logSearchLog4jProperties != null) {
            String content = logSearchLog4jProperties.getProperties().get("content");
            if (content.contains("<!DOCTYPE log4j:configuration SYSTEM \"log4j.dtd\">")) {
              content = content.replace("<!DOCTYPE log4j:configuration SYSTEM \"log4j.dtd\">", "<!DOCTYPE log4j:configuration SYSTEM \"http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/xml/doc-files/log4j.dtd\">");
              updateConfigurationPropertiesForCluster(cluster, "logsearch-log4j", Collections.singletonMap("content", content), true, true);
            }
          }

          Config logSearchServiceLogsConfig = cluster.getDesiredConfigByType("logsearch-service_logs-solrconfig");
          if (logSearchServiceLogsConfig != null) {
            String content = logSearchServiceLogsConfig.getProperties().get("content");
            if (content.contains("class=\"solr.admin.AdminHandlers\"")) {
              content = content.replaceAll("(?s)<requestHandler name=\"/admin/\".*?class=\"solr.admin.AdminHandlers\" />", "");
              updateConfigurationPropertiesForCluster(cluster, "logsearch-service_logs-solrconfig", Collections.singletonMap("content", content), true, true);
            }
          }

          Config logSearchAuditLogsConfig = cluster.getDesiredConfigByType("logsearch-audit_logs-solrconfig");
          if (logSearchAuditLogsConfig != null) {
            String content = logSearchAuditLogsConfig.getProperties().get("content");
            if (content.contains("class=\"solr.admin.AdminHandlers\"")) {
              content = content.replaceAll("(?s)<requestHandler name=\"/admin/\".*?class=\"solr.admin.AdminHandlers\" />", "");
              updateConfigurationPropertiesForCluster(cluster, "logsearch-audit_logs-solrconfig", Collections.singletonMap("content", content), true, true);
            }
          }

          Config logFeederOutputConfig = cluster.getDesiredConfigByType("logfeeder-output-config");
          if (logFeederOutputConfig != null) {
            String content = logFeederOutputConfig.getProperties().get("content");
            content = content.replace(
              "      \"collection\":\"{{logsearch_solr_collection_service_logs}}\",\n" +
                "      \"number_of_shards\": \"{{logsearch_collection_service_logs_numshards}}\",\n" +
                "      \"splits_interval_mins\": \"{{logsearch_service_logs_split_interval_mins}}\",\n",
              "      \"type\": \"service\",\n");

            content = content.replace(
              "      \"collection\":\"{{logsearch_solr_collection_audit_logs}}\",\n" +
                "      \"number_of_shards\": \"{{logsearch_collection_audit_logs_numshards}}\",\n" +
                "      \"splits_interval_mins\": \"{{logsearch_audit_logs_split_interval_mins}}\",\n",
              "      \"type\": \"audit\",\n");

            updateConfigurationPropertiesForCluster(cluster, "logfeeder-output-config", Collections.singletonMap("content", content), true, true);
          }
        }
      }
    }
  }

  protected PrepareKerberosIdentitiesServerAction getPrepareIdentityServerAction() {
    return new PrepareKerberosIdentitiesServerAction();
  }

  /**
   * Upgrades kerberos related data.
   * Also creates keytabs and principals database records. This happens via code in PrepareKerberosIdentitiesServerAction,
   * so code reused and all changes will be reflected in upgrade.
   *
   * @throws AmbariException
   */
  protected void updateKerberosConfigurations() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (!MapUtils.isEmpty(clusterMap)) {
        for (Cluster cluster : clusterMap.values()) {
          Config config = cluster.getDesiredConfigByType("kerberos-env");
          if (config != null) {
            Map<String, String> properties = config.getProperties();
            if (properties.containsKey("group")) {
              // Covert kerberos-env/group to kerberos-env/ipa_user_group
              updateConfigurationPropertiesForCluster(cluster, "kerberos-env",
                Collections.singletonMap("ipa_user_group", properties.get("group")), Collections.singleton("group"),
                true, false);
            }
          }
          if (config != null) {
            PrepareKerberosIdentitiesServerAction prepareIdentities = getPrepareIdentityServerAction();
            ExecutionCommand executionCommand = new ExecutionCommand();
            executionCommand.setCommandParams(new HashMap<String, String>() {{
              put(KerberosServerAction.DEFAULT_REALM, config.getProperties().get("realm"));
            }});
            prepareIdentities.setExecutionCommand(executionCommand);

            // inject whatever we need for calling desired server action
            injector.injectMembers(prepareIdentities);
            KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);

            injector.getInstance(AmbariServer.class).performStaticInjection();
            AmbariServer.setController(injector.getInstance(AmbariManagementController.class));

            KerberosDescriptor kerberosDescriptor = kerberosHelper.getKerberosDescriptor(cluster, false);
            Map<String, Map<String, String>> kerberosConfigurations = new HashMap<>();
            Map<String, Set<String>> propertiesToIgnore = new HashMap<>();
            List<ServiceComponentHost> schToProcess = kerberosHelper.getServiceComponentHostsToProcess(cluster, kerberosDescriptor, null, null);
            Map<String, Map<String, String>> configurations = kerberosHelper.calculateConfigurations(cluster, null, kerberosDescriptor, false, false);
            boolean includeAmbariIdentity = true;
            String dataDirectory = kerberosHelper.createTemporaryDirectory().getAbsolutePath();
            try {
              executeInTransaction(new Runnable() {
                @Override
                public void run() {
                  try {
                    prepareIdentities.processServiceComponentHosts(cluster, kerberosDescriptor, schToProcess, null, dataDirectory, configurations, kerberosConfigurations, includeAmbariIdentity, propertiesToIgnore);
                  } catch (AmbariException e) {
                    throw new RuntimeException(e);
                  }
                }
              });
            } catch (RuntimeException e) {
              throw new AmbariException("Failed to upgrade kerberos tables", e);
            }
          }
        }
      }
    }


  }

  /**
   * Moves LDAP related properties from ambari.properties to ambari_configuration DB table
   *
   * @throws AmbariException if there was any issue when clearing ambari.properties
   */
  protected void upgradeLdapConfiguration() throws AmbariException {
    LOG.info("Moving LDAP related properties from ambari.properties to ambari_congiuration DB table...");
    final AmbariConfigurationDAO ambariConfigurationDao = injector.getInstance(AmbariConfigurationDAO.class);
    final Map<String, String> propertiesToBeSaved = new HashMap<>();
    final Map<AmbariLdapConfigurationKeys, String> ldapConfigurationMap = getLdapConfigurationMap();
    ldapConfigurationMap.forEach((key, oldPropertyName) -> {
      String ldapPropertyValue = configuration.getProperty(oldPropertyName);
      if (StringUtils.isNotBlank(ldapPropertyValue)) {
        if (AmbariLdapConfigurationKeys.SERVER_HOST == key || AmbariLdapConfigurationKeys.SECONDARY_SERVER_HOST == key) {
          final HostAndPort hostAndPort = HostAndPort.fromString(ldapPropertyValue);
          AmbariLdapConfigurationKeys keyToBesaved = AmbariLdapConfigurationKeys.SERVER_HOST == key ? AmbariLdapConfigurationKeys.SERVER_HOST
            : AmbariLdapConfigurationKeys.SECONDARY_SERVER_HOST;
          populateLdapConfigurationToBeUpgraded(propertiesToBeSaved, oldPropertyName, keyToBesaved.key(), hostAndPort.getHostText());

          keyToBesaved = AmbariLdapConfigurationKeys.SERVER_HOST == key ? AmbariLdapConfigurationKeys.SERVER_PORT : AmbariLdapConfigurationKeys.SECONDARY_SERVER_PORT;
          populateLdapConfigurationToBeUpgraded(propertiesToBeSaved, oldPropertyName, keyToBesaved.key(), String.valueOf(hostAndPort.getPort()));
        } else {
          populateLdapConfigurationToBeUpgraded(propertiesToBeSaved, oldPropertyName, key.key(), ldapPropertyValue);
        }
      }
    });

    if (propertiesToBeSaved.isEmpty()) {
      LOG.info("There was no LDAP related properties in ambari.properties; moved 0 elements");
    } else {
      ambariConfigurationDao.reconcileCategory(AmbariServerConfigurationCategory.LDAP_CONFIGURATION.getCategoryName(), propertiesToBeSaved, false);
      configuration.removePropertiesFromAmbariProperties(ldapConfigurationMap.values());
      LOG.info(propertiesToBeSaved.size() + " LDAP related properties " + (propertiesToBeSaved.size() == 1 ? "has" : "have") + " been moved to DB");
    }
  }

  private void populateLdapConfigurationToBeUpgraded(Map<String, String> propertiesToBeSaved, String oldPropertyName, String newPropertyName, String value) {
    propertiesToBeSaved.put(newPropertyName, value);
    LOG.info("About to upgrade '" + oldPropertyName + "' as '" + newPropertyName + "' (value=" + value + ")");
  }

  /**
   * @return a map describing the new LDAP configuration key to the old ambari.properties property name
   */
  @SuppressWarnings("serial")
  private Map<AmbariLdapConfigurationKeys, String> getLdapConfigurationMap() {
    return Collections.unmodifiableMap(new HashMap<AmbariLdapConfigurationKeys, String>() {
      {
        put(AmbariLdapConfigurationKeys.LDAP_ENABLED, "ambari.ldap.isConfigured");
        put(AmbariLdapConfigurationKeys.SERVER_HOST, "authentication.ldap.primaryUrl");
        put(AmbariLdapConfigurationKeys.SECONDARY_SERVER_HOST, "authentication.ldap.secondaryUrl");
        put(AmbariLdapConfigurationKeys.USE_SSL, "authentication.ldap.useSSL");
        put(AmbariLdapConfigurationKeys.ANONYMOUS_BIND, "authentication.ldap.bindAnonymously");
        put(AmbariLdapConfigurationKeys.BIND_DN, "authentication.ldap.managerDn");
        put(AmbariLdapConfigurationKeys.BIND_PASSWORD, "authentication.ldap.managerPassword");
        put(AmbariLdapConfigurationKeys.DN_ATTRIBUTE, "authentication.ldap.dnAttribute");
        put(AmbariLdapConfigurationKeys.USER_OBJECT_CLASS, "authentication.ldap.userObjectClass");
        put(AmbariLdapConfigurationKeys.USER_NAME_ATTRIBUTE, "authentication.ldap.usernameAttribute");
        put(AmbariLdapConfigurationKeys.USER_SEARCH_BASE, "authentication.ldap.baseDn");
        put(AmbariLdapConfigurationKeys.USER_BASE, "authentication.ldap.userBase");
        put(AmbariLdapConfigurationKeys.GROUP_OBJECT_CLASS, "authentication.ldap.groupObjectClass");
        put(AmbariLdapConfigurationKeys.GROUP_NAME_ATTRIBUTE, "authentication.ldap.groupNamingAttr");
        put(AmbariLdapConfigurationKeys.GROUP_MEMBER_ATTRIBUTE, "authentication.ldap.groupMembershipAttr");
        put(AmbariLdapConfigurationKeys.GROUP_SEARCH_BASE, "authentication.ldap.baseDn");
        put(AmbariLdapConfigurationKeys.GROUP_BASE, "authentication.ldap.groupBase");
        put(AmbariLdapConfigurationKeys.USER_SEARCH_FILTER, "authentication.ldap.userSearchFilter");
        put(AmbariLdapConfigurationKeys.USER_MEMBER_REPLACE_PATTERN, "authentication.ldap.sync.userMemberReplacePattern");
        put(AmbariLdapConfigurationKeys.USER_MEMBER_FILTER, "authentication.ldap.sync.userMemberFilter");
        put(AmbariLdapConfigurationKeys.ALTERNATE_USER_SEARCH_ENABLED, "authentication.ldap.alternateUserSearchEnabled");
        put(AmbariLdapConfigurationKeys.ALTERNATE_USER_SEARCH_FILTER, "authentication.ldap.alternateUserSearchFilter");
        put(AmbariLdapConfigurationKeys.GROUP_SEARCH_FILTER, "authorization.ldap.groupSearchFilter");
        put(AmbariLdapConfigurationKeys.GROUP_MEMBER_REPLACE_PATTERN, "authentication.ldap.sync.groupMemberReplacePattern");
        put(AmbariLdapConfigurationKeys.GROUP_MEMBER_FILTER, "authentication.ldap.sync.groupMemberFilter");
        put(AmbariLdapConfigurationKeys.GROUP_MAPPING_RULES, "authorization.ldap.adminGroupMappingRules");
        put(AmbariLdapConfigurationKeys.FORCE_LOWERCASE_USERNAMES, "authentication.ldap.username.forceLowercase");
        put(AmbariLdapConfigurationKeys.REFERRAL_HANDLING, "authentication.ldap.referral");
        put(AmbariLdapConfigurationKeys.PAGINATION_ENABLED, "authentication.ldap.pagination.enabled");
        put(AmbariLdapConfigurationKeys.COLLISION_BEHAVIOR, "ldap.sync.username.collision.behavior");
      }
    });
  }
}
