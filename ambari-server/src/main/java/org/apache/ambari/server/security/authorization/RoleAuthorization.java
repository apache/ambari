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

package org.apache.ambari.server.security.authorization;

import java.util.EnumSet;
import java.util.Set;

/**
 * RoleAuthorization is an enumeration of granular authorizations that can be applied to resources
 * like clusters and views.
 * <p/>
 * This data matches the <code>roleauthorization</code> table in the Ambari database. The value field
 * of each item represents the <code>roleauthorization.authorization_id</code> value.
 */
public enum RoleAuthorization {
  AMBARI_ADD_DELETE_CLUSTERS("AMBARI.ADD_DELETE_CLUSTERS"),
  AMBARI_ASSIGN_ROLES("AMBARI.ASSIGN_ROLES"),
  AMBARI_EDIT_STACK_REPOS("AMBARI.EDIT_STACK_REPOS"),
  AMBARI_MANAGE_SETTINGS("AMBARI.MANAGE_SETTINGS"),
  AMBARI_MANAGE_GROUPS("AMBARI.MANAGE_GROUPS"),
  AMBARI_MANAGE_STACK_VERSIONS("AMBARI.MANAGE_STACK_VERSIONS"),
  AMBARI_MANAGE_USERS("AMBARI.MANAGE_USERS"),
  AMBARI_MANAGE_VIEWS("AMBARI.MANAGE_VIEWS"),
  AMBARI_RENAME_CLUSTER("AMBARI.RENAME_CLUSTER"),
  AMBARI_RUN_CUSTOM_COMMAND("AMBARI.RUN_CUSTOM_COMMAND"),
  AMBARI_MANAGE_CONFIGURATION("AMBARI.MANAGE_CONFIGURATION"),
  CLUSTER_MANAGE_CREDENTIALS("CLUSTER.MANAGE_CREDENTIALS"),
  CLUSTER_MODIFY_CONFIGS("CLUSTER.MODIFY_CONFIGS"),
  CLUSTER_MANAGE_CONFIG_GROUPS("CLUSTER.MANAGE_CONFIG_GROUPS"),
  CLUSTER_MANAGE_ALERTS("CLUSTER.MANAGE_ALERTS"),
  CLUSTER_MANAGE_USER_PERSISTED_DATA("CLUSTER.MANAGE_USER_PERSISTED_DATA"),
  CLUSTER_TOGGLE_ALERTS("CLUSTER.TOGGLE_ALERTS"),
  CLUSTER_TOGGLE_KERBEROS("CLUSTER.TOGGLE_KERBEROS"),
  CLUSTER_UPGRADE_DOWNGRADE_STACK("CLUSTER.UPGRADE_DOWNGRADE_STACK"),
  CLUSTER_VIEW_ALERTS("CLUSTER.VIEW_ALERTS"),
  CLUSTER_VIEW_CONFIGS("CLUSTER.VIEW_CONFIGS"),
  CLUSTER_VIEW_METRICS("CLUSTER.VIEW_METRICS"),
  CLUSTER_VIEW_STACK_DETAILS("CLUSTER.VIEW_STACK_DETAILS"),
  CLUSTER_VIEW_STATUS_INFO("CLUSTER.VIEW_STATUS_INFO"),
  CLUSTER_RUN_CUSTOM_COMMAND("CLUSTER.RUN_CUSTOM_COMMAND"),
  CLUSTER_MANAGE_AUTO_START("CLUSTER.MANAGE_AUTO_START"),
  CLUSTER_MANAGE_ALERT_NOTIFICATIONS("CLUSTER.MANAGE_ALERT_NOTIFICATIONS"),
  HOST_ADD_DELETE_COMPONENTS("HOST.ADD_DELETE_COMPONENTS"),
  HOST_ADD_DELETE_HOSTS("HOST.ADD_DELETE_HOSTS"),
  HOST_TOGGLE_MAINTENANCE("HOST.TOGGLE_MAINTENANCE"),
  HOST_VIEW_CONFIGS("HOST.VIEW_CONFIGS"),
  HOST_VIEW_METRICS("HOST.VIEW_METRICS"),
  HOST_VIEW_STATUS_INFO("HOST.VIEW_STATUS_INFO"),
  SERVICE_ADD_DELETE_SERVICES("SERVICE.ADD_DELETE_SERVICES"),
  SERVICE_VIEW_OPERATIONAL_LOGS("SERVICE.VIEW_OPERATIONAL_LOGS"),
  SERVICE_COMPARE_CONFIGS("SERVICE.COMPARE_CONFIGS"),
  SERVICE_DECOMMISSION_RECOMMISSION("SERVICE.DECOMMISSION_RECOMMISSION"),
  SERVICE_ENABLE_HA("SERVICE.ENABLE_HA"),
  SERVICE_MANAGE_CONFIG_GROUPS("SERVICE.MANAGE_CONFIG_GROUPS"),
  SERVICE_MANAGE_ALERTS("SERVICE.MANAGE_ALERTS"),
  SERVICE_MODIFY_CONFIGS("SERVICE.MODIFY_CONFIGS"),
  SERVICE_MOVE("SERVICE.MOVE"),
  SERVICE_RUN_CUSTOM_COMMAND("SERVICE.RUN_CUSTOM_COMMAND"),
  SERVICE_RUN_SERVICE_CHECK("SERVICE.RUN_SERVICE_CHECK"),
  SERVICE_SET_SERVICE_USERS_GROUPS("SERVICE.SET_SERVICE_USERS_GROUPS"),
  SERVICE_START_STOP("SERVICE.START_STOP"),
  SERVICE_TOGGLE_ALERTS("SERVICE.TOGGLE_ALERTS"),
  SERVICE_TOGGLE_MAINTENANCE("SERVICE.TOGGLE_MAINTENANCE"),
  SERVICE_VIEW_ALERTS("SERVICE.VIEW_ALERTS"),
  SERVICE_VIEW_CONFIGS("SERVICE.VIEW_CONFIGS"),
  SERVICE_VIEW_METRICS("SERVICE.VIEW_METRICS"),
  SERVICE_VIEW_STATUS_INFO("SERVICE.VIEW_STATUS_INFO"),
  SERVICE_MANAGE_AUTO_START("SERVICE.MANAGE_AUTO_START"),
  VIEW_USE("VIEW.USE");

  public static final Set<RoleAuthorization> AUTHORIZATIONS_VIEW_CLUSTER = EnumSet.of(
    CLUSTER_VIEW_STATUS_INFO,
    CLUSTER_VIEW_ALERTS,
    CLUSTER_VIEW_CONFIGS,
    CLUSTER_VIEW_METRICS,
    CLUSTER_VIEW_STACK_DETAILS,
    CLUSTER_MODIFY_CONFIGS,
    CLUSTER_MANAGE_CONFIG_GROUPS,
    CLUSTER_TOGGLE_ALERTS,
    CLUSTER_TOGGLE_KERBEROS,
    CLUSTER_UPGRADE_DOWNGRADE_STACK);

  public static final Set<RoleAuthorization> AUTHORIZATIONS_UPDATE_CLUSTER = EnumSet.of(
    CLUSTER_TOGGLE_ALERTS,
    CLUSTER_TOGGLE_KERBEROS,
    CLUSTER_UPGRADE_DOWNGRADE_STACK,
    CLUSTER_MODIFY_CONFIGS,
    CLUSTER_MANAGE_AUTO_START,
    SERVICE_MODIFY_CONFIGS);

  public static final Set<RoleAuthorization> AUTHORIZATIONS_VIEW_SERVICE = EnumSet.of(
    SERVICE_VIEW_ALERTS,
    SERVICE_VIEW_CONFIGS,
    SERVICE_VIEW_METRICS,
    SERVICE_VIEW_STATUS_INFO,
    SERVICE_COMPARE_CONFIGS,
    SERVICE_ADD_DELETE_SERVICES,
    SERVICE_DECOMMISSION_RECOMMISSION,
    SERVICE_ENABLE_HA,
    SERVICE_MANAGE_CONFIG_GROUPS,
    SERVICE_MODIFY_CONFIGS,
    SERVICE_START_STOP,
    SERVICE_TOGGLE_MAINTENANCE,
    SERVICE_TOGGLE_ALERTS,
    SERVICE_MOVE,
    SERVICE_RUN_CUSTOM_COMMAND,
    SERVICE_RUN_SERVICE_CHECK);

  public static final Set<RoleAuthorization> AUTHORIZATIONS_UPDATE_SERVICE = EnumSet.of(
    SERVICE_ADD_DELETE_SERVICES,
    SERVICE_DECOMMISSION_RECOMMISSION,
    SERVICE_ENABLE_HA,
    SERVICE_MANAGE_CONFIG_GROUPS,
    SERVICE_MODIFY_CONFIGS,
    SERVICE_START_STOP,
    SERVICE_TOGGLE_MAINTENANCE,
    SERVICE_TOGGLE_ALERTS,
    SERVICE_MOVE,
    SERVICE_RUN_CUSTOM_COMMAND,
    SERVICE_RUN_SERVICE_CHECK,
    SERVICE_MANAGE_ALERTS,
    SERVICE_MANAGE_AUTO_START,
    SERVICE_SET_SERVICE_USERS_GROUPS);

  private final String id;

  /**
   * Constructor
   *
   * @param id the ID value for this RoleAuthorization
   */
  RoleAuthorization(String id) {
    this.id = id;
  }

  /**
   * Get's the ID value for this RoleAuthorization
   * <p/>
   * This value represents the <code>roleauthorization.authorization_id</code> value from the Ambari database
   *
   * @return an string
   */
  public String getId() {
    return id;
  }

  /**
   * Safely translates a role authorization Id to a RoleAuthorization
   *
   * @param authenticationId an authentication id
   * @return a RoleAuthorization or null if no translation can be made
   */
  public static RoleAuthorization translate(String authenticationId) {
    if (authenticationId == null) {
      return null;
    } else {
      authenticationId = authenticationId.trim();

      if (authenticationId.isEmpty()) {
        return null;
      } else {
        return RoleAuthorization.valueOf(authenticationId.replace(".", "_").toUpperCase());
      }
    }
  }

}
