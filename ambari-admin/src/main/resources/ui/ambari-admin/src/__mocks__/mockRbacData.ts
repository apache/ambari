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
export const rbacData = {
    "href": "http://host.example.com:8080/api/v1/permissions?PermissionInfo/resource_name.in(CLUSTER,AMBARI)&_=1726470229341&fields=PermissionInfo/*,authorizations/AuthorizationInfo/*",
    "items": [
        {
            "href": "http://host.example.com:8080/api/v1/permissions/1",
            "PermissionInfo": {
                "permission_id": 1,
                "permission_label": "Ambari Administrator",
                "permission_name": "AMBARI.ADMINISTRATOR",
                "resource_name": "AMBARI",
                "sort_order": 1
            },
            "authorizations": [
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/AMBARI.ADD_DELETE_CLUSTERS",
                    "AuthorizationInfo": {
                        "authorization_id": "AMBARI.ADD_DELETE_CLUSTERS",
                        "authorization_name": "Create new clusters",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/AMBARI.ASSIGN_ROLES",
                    "AuthorizationInfo": {
                        "authorization_id": "AMBARI.ASSIGN_ROLES",
                        "authorization_name": "Assign roles",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/AMBARI.EDIT_STACK_REPOS",
                    "AuthorizationInfo": {
                        "authorization_id": "AMBARI.EDIT_STACK_REPOS",
                        "authorization_name": "Edit stack repository URLs",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/AMBARI.MANAGE_CONFIGURATION",
                    "AuthorizationInfo": {
                        "authorization_id": "AMBARI.MANAGE_CONFIGURATION",
                        "authorization_name": "Manage ambari configuration",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/AMBARI.MANAGE_GROUPS",
                    "AuthorizationInfo": {
                        "authorization_id": "AMBARI.MANAGE_GROUPS",
                        "authorization_name": "Manage groups",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/AMBARI.MANAGE_SETTINGS",
                    "AuthorizationInfo": {
                        "authorization_id": "AMBARI.MANAGE_SETTINGS",
                        "authorization_name": "Manage administrative settings",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/AMBARI.MANAGE_STACK_VERSIONS",
                    "AuthorizationInfo": {
                        "authorization_id": "AMBARI.MANAGE_STACK_VERSIONS",
                        "authorization_name": "Manage stack versions",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/AMBARI.MANAGE_USERS",
                    "AuthorizationInfo": {
                        "authorization_id": "AMBARI.MANAGE_USERS",
                        "authorization_name": "Manage users",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/AMBARI.MANAGE_VIEWS",
                    "AuthorizationInfo": {
                        "authorization_id": "AMBARI.MANAGE_VIEWS",
                        "authorization_name": "Manage Ambari Views",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/AMBARI.RENAME_CLUSTER",
                    "AuthorizationInfo": {
                        "authorization_id": "AMBARI.RENAME_CLUSTER",
                        "authorization_name": "Rename clusters",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/AMBARI.RUN_CUSTOM_COMMAND",
                    "AuthorizationInfo": {
                        "authorization_id": "AMBARI.RUN_CUSTOM_COMMAND",
                        "authorization_name": "Perform custom administrative actions",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/AMBARI.VIEW_STATUS_INFO",
                    "AuthorizationInfo": {
                        "authorization_id": "AMBARI.VIEW_STATUS_INFO",
                        "authorization_name": "View status information",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/CLUSTER.MANAGE_ALERTS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.MANAGE_ALERTS",
                        "authorization_name": "Manage cluster-level alerts",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/CLUSTER.MANAGE_ALERT_NOTIFICATIONS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.MANAGE_ALERT_NOTIFICATIONS",
                        "authorization_name": "Manage alert notifications configuration",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/CLUSTER.MANAGE_AUTO_START",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.MANAGE_AUTO_START",
                        "authorization_name": "Manage service auto-start configuration",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/CLUSTER.MANAGE_CONFIG_GROUPS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.MANAGE_CONFIG_GROUPS",
                        "authorization_name": "Manage cluster config groups",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/CLUSTER.MANAGE_CREDENTIALS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.MANAGE_CREDENTIALS",
                        "authorization_name": "Manage external credentials",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/CLUSTER.MANAGE_USER_PERSISTED_DATA",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.MANAGE_USER_PERSISTED_DATA",
                        "authorization_name": "Manage cluster-level user persisted data",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/CLUSTER.MANAGE_WIDGETS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.MANAGE_WIDGETS",
                        "authorization_name": "Manage widgets",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/CLUSTER.MODIFY_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.MODIFY_CONFIGS",
                        "authorization_name": "Modify cluster configurations",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/CLUSTER.RUN_CUSTOM_COMMAND",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.RUN_CUSTOM_COMMAND",
                        "authorization_name": "Perform custom cluster-level actions",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/CLUSTER.TOGGLE_ALERTS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.TOGGLE_ALERTS",
                        "authorization_name": "Enable/disable cluster-level alerts",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/CLUSTER.TOGGLE_KERBEROS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.TOGGLE_KERBEROS",
                        "authorization_name": "Enable/disable Kerberos",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/CLUSTER.UPGRADE_DOWNGRADE_STACK",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.UPGRADE_DOWNGRADE_STACK",
                        "authorization_name": "Upgrade/downgrade stack",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/CLUSTER.VIEW_ALERTS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_ALERTS",
                        "authorization_name": "View cluster-level alerts",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/CLUSTER.VIEW_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_CONFIGS",
                        "authorization_name": "View configuration",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/CLUSTER.VIEW_METRICS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_METRICS",
                        "authorization_name": "View metrics",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/CLUSTER.VIEW_STACK_DETAILS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_STACK_DETAILS",
                        "authorization_name": "View stack version details",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/CLUSTER.VIEW_STATUS_INFO",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_STATUS_INFO",
                        "authorization_name": "View status information",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/HOST.ADD_DELETE_COMPONENTS",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.ADD_DELETE_COMPONENTS",
                        "authorization_name": "Install components",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/HOST.ADD_DELETE_HOSTS",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.ADD_DELETE_HOSTS",
                        "authorization_name": "Add/Delete hosts",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/HOST.TOGGLE_MAINTENANCE",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.TOGGLE_MAINTENANCE",
                        "authorization_name": "Turn on/off maintenance mode",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/HOST.VIEW_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.VIEW_CONFIGS",
                        "authorization_name": "View configuration",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/HOST.VIEW_METRICS",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.VIEW_METRICS",
                        "authorization_name": "View metrics",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/HOST.VIEW_STATUS_INFO",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.VIEW_STATUS_INFO",
                        "authorization_name": "View status information",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/SERVICE.ADD_DELETE_SERVICES",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.ADD_DELETE_SERVICES",
                        "authorization_name": "Add/delete services",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/SERVICE.COMPARE_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.COMPARE_CONFIGS",
                        "authorization_name": "Compare configurations",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/SERVICE.DECOMMISSION_RECOMMISSION",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.DECOMMISSION_RECOMMISSION",
                        "authorization_name": "Decommission/recommission",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/SERVICE.ENABLE_HA",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.ENABLE_HA",
                        "authorization_name": "Enable HA",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/SERVICE.MANAGE_ALERTS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.MANAGE_ALERTS",
                        "authorization_name": "Manage service-level alerts",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/SERVICE.MANAGE_AUTO_START",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.MANAGE_AUTO_START",
                        "authorization_name": "Manage service auto-start",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/SERVICE.MANAGE_CONFIG_GROUPS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.MANAGE_CONFIG_GROUPS",
                        "authorization_name": "Manage configuration groups",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/SERVICE.MODIFY_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.MODIFY_CONFIGS",
                        "authorization_name": "Modify configurations",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/SERVICE.MOVE",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.MOVE",
                        "authorization_name": "Move to another host",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/SERVICE.RUN_CUSTOM_COMMAND",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.RUN_CUSTOM_COMMAND",
                        "authorization_name": "Perform service-specific tasks",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/SERVICE.RUN_SERVICE_CHECK",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.RUN_SERVICE_CHECK",
                        "authorization_name": "Run service checks",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/SERVICE.SET_SERVICE_USERS_GROUPS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.SET_SERVICE_USERS_GROUPS",
                        "authorization_name": "Set service users and groups",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/SERVICE.START_STOP",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.START_STOP",
                        "authorization_name": "Start/Stop/Restart Service",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/SERVICE.TOGGLE_ALERTS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.TOGGLE_ALERTS",
                        "authorization_name": "Enable/disable service-level alerts",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/SERVICE.TOGGLE_MAINTENANCE",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.TOGGLE_MAINTENANCE",
                        "authorization_name": "Turn on/off maintenance mode",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/SERVICE.VIEW_ALERTS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_ALERTS",
                        "authorization_name": "View service-level alerts",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/SERVICE.VIEW_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_CONFIGS",
                        "authorization_name": "View configurations",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/SERVICE.VIEW_METRICS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_METRICS",
                        "authorization_name": "View metrics",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/SERVICE.VIEW_OPERATIONAL_LOGS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_OPERATIONAL_LOGS",
                        "authorization_name": "View service operational logs",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/SERVICE.VIEW_STATUS_INFO",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_STATUS_INFO",
                        "authorization_name": "View status information",
                        "permission_id": 1
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/1/authorizations/VIEW.USE",
                    "AuthorizationInfo": {
                        "authorization_id": "VIEW.USE",
                        "authorization_name": "Use View",
                        "permission_id": 1
                    }
                }
            ]
        },
        {
            "href": "http://host.example.com:8080/api/v1/permissions/2",
            "PermissionInfo": {
                "permission_id": 2,
                "permission_label": "Cluster User",
                "permission_name": "CLUSTER.USER",
                "resource_name": "CLUSTER",
                "sort_order": 6
            },
            "authorizations": [
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/2/authorizations/CLUSTER.MANAGE_USER_PERSISTED_DATA",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.MANAGE_USER_PERSISTED_DATA",
                        "authorization_name": "Manage cluster-level user persisted data",
                        "permission_id": 2
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/2/authorizations/CLUSTER.VIEW_ALERTS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_ALERTS",
                        "authorization_name": "View cluster-level alerts",
                        "permission_id": 2
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/2/authorizations/CLUSTER.VIEW_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_CONFIGS",
                        "authorization_name": "View configuration",
                        "permission_id": 2
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/2/authorizations/CLUSTER.VIEW_METRICS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_METRICS",
                        "authorization_name": "View metrics",
                        "permission_id": 2
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/2/authorizations/CLUSTER.VIEW_STACK_DETAILS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_STACK_DETAILS",
                        "authorization_name": "View stack version details",
                        "permission_id": 2
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/2/authorizations/CLUSTER.VIEW_STATUS_INFO",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_STATUS_INFO",
                        "authorization_name": "View status information",
                        "permission_id": 2
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/2/authorizations/HOST.VIEW_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.VIEW_CONFIGS",
                        "authorization_name": "View configuration",
                        "permission_id": 2
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/2/authorizations/HOST.VIEW_METRICS",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.VIEW_METRICS",
                        "authorization_name": "View metrics",
                        "permission_id": 2
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/2/authorizations/HOST.VIEW_STATUS_INFO",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.VIEW_STATUS_INFO",
                        "authorization_name": "View status information",
                        "permission_id": 2
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/2/authorizations/SERVICE.COMPARE_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.COMPARE_CONFIGS",
                        "authorization_name": "Compare configurations",
                        "permission_id": 2
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/2/authorizations/SERVICE.VIEW_ALERTS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_ALERTS",
                        "authorization_name": "View service-level alerts",
                        "permission_id": 2
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/2/authorizations/SERVICE.VIEW_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_CONFIGS",
                        "authorization_name": "View configurations",
                        "permission_id": 2
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/2/authorizations/SERVICE.VIEW_METRICS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_METRICS",
                        "authorization_name": "View metrics",
                        "permission_id": 2
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/2/authorizations/SERVICE.VIEW_STATUS_INFO",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_STATUS_INFO",
                        "authorization_name": "View status information",
                        "permission_id": 2
                    }
                }
            ]
        },
        {
            "href": "http://host.example.com:8080/api/v1/permissions/3",
            "PermissionInfo": {
                "permission_id": 3,
                "permission_label": "Cluster Administrator",
                "permission_name": "CLUSTER.ADMINISTRATOR",
                "resource_name": "CLUSTER",
                "sort_order": 2
            },
            "authorizations": [
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/CLUSTER.MANAGE_ALERTS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.MANAGE_ALERTS",
                        "authorization_name": "Manage cluster-level alerts",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/CLUSTER.MANAGE_ALERT_NOTIFICATIONS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.MANAGE_ALERT_NOTIFICATIONS",
                        "authorization_name": "Manage alert notifications configuration",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/CLUSTER.MANAGE_AUTO_START",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.MANAGE_AUTO_START",
                        "authorization_name": "Manage service auto-start configuration",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/CLUSTER.MANAGE_CONFIG_GROUPS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.MANAGE_CONFIG_GROUPS",
                        "authorization_name": "Manage cluster config groups",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/CLUSTER.MANAGE_CREDENTIALS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.MANAGE_CREDENTIALS",
                        "authorization_name": "Manage external credentials",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/CLUSTER.MANAGE_USER_PERSISTED_DATA",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.MANAGE_USER_PERSISTED_DATA",
                        "authorization_name": "Manage cluster-level user persisted data",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/CLUSTER.MANAGE_WIDGETS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.MANAGE_WIDGETS",
                        "authorization_name": "Manage widgets",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/CLUSTER.MODIFY_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.MODIFY_CONFIGS",
                        "authorization_name": "Modify cluster configurations",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/CLUSTER.RUN_CUSTOM_COMMAND",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.RUN_CUSTOM_COMMAND",
                        "authorization_name": "Perform custom cluster-level actions",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/CLUSTER.TOGGLE_ALERTS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.TOGGLE_ALERTS",
                        "authorization_name": "Enable/disable cluster-level alerts",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/CLUSTER.TOGGLE_KERBEROS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.TOGGLE_KERBEROS",
                        "authorization_name": "Enable/disable Kerberos",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/CLUSTER.UPGRADE_DOWNGRADE_STACK",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.UPGRADE_DOWNGRADE_STACK",
                        "authorization_name": "Upgrade/downgrade stack",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/CLUSTER.VIEW_ALERTS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_ALERTS",
                        "authorization_name": "View cluster-level alerts",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/CLUSTER.VIEW_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_CONFIGS",
                        "authorization_name": "View configuration",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/CLUSTER.VIEW_METRICS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_METRICS",
                        "authorization_name": "View metrics",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/CLUSTER.VIEW_STACK_DETAILS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_STACK_DETAILS",
                        "authorization_name": "View stack version details",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/CLUSTER.VIEW_STATUS_INFO",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_STATUS_INFO",
                        "authorization_name": "View status information",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/HOST.ADD_DELETE_COMPONENTS",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.ADD_DELETE_COMPONENTS",
                        "authorization_name": "Install components",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/HOST.ADD_DELETE_HOSTS",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.ADD_DELETE_HOSTS",
                        "authorization_name": "Add/Delete hosts",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/HOST.TOGGLE_MAINTENANCE",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.TOGGLE_MAINTENANCE",
                        "authorization_name": "Turn on/off maintenance mode",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/HOST.VIEW_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.VIEW_CONFIGS",
                        "authorization_name": "View configuration",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/HOST.VIEW_METRICS",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.VIEW_METRICS",
                        "authorization_name": "View metrics",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/HOST.VIEW_STATUS_INFO",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.VIEW_STATUS_INFO",
                        "authorization_name": "View status information",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/SERVICE.ADD_DELETE_SERVICES",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.ADD_DELETE_SERVICES",
                        "authorization_name": "Add/delete services",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/SERVICE.COMPARE_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.COMPARE_CONFIGS",
                        "authorization_name": "Compare configurations",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/SERVICE.DECOMMISSION_RECOMMISSION",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.DECOMMISSION_RECOMMISSION",
                        "authorization_name": "Decommission/recommission",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/SERVICE.ENABLE_HA",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.ENABLE_HA",
                        "authorization_name": "Enable HA",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/SERVICE.MANAGE_ALERTS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.MANAGE_ALERTS",
                        "authorization_name": "Manage service-level alerts",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/SERVICE.MANAGE_AUTO_START",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.MANAGE_AUTO_START",
                        "authorization_name": "Manage service auto-start",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/SERVICE.MANAGE_CONFIG_GROUPS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.MANAGE_CONFIG_GROUPS",
                        "authorization_name": "Manage configuration groups",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/SERVICE.MODIFY_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.MODIFY_CONFIGS",
                        "authorization_name": "Modify configurations",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/SERVICE.MOVE",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.MOVE",
                        "authorization_name": "Move to another host",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/SERVICE.RUN_CUSTOM_COMMAND",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.RUN_CUSTOM_COMMAND",
                        "authorization_name": "Perform service-specific tasks",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/SERVICE.RUN_SERVICE_CHECK",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.RUN_SERVICE_CHECK",
                        "authorization_name": "Run service checks",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/SERVICE.SET_SERVICE_USERS_GROUPS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.SET_SERVICE_USERS_GROUPS",
                        "authorization_name": "Set service users and groups",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/SERVICE.START_STOP",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.START_STOP",
                        "authorization_name": "Start/Stop/Restart Service",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/SERVICE.TOGGLE_ALERTS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.TOGGLE_ALERTS",
                        "authorization_name": "Enable/disable service-level alerts",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/SERVICE.TOGGLE_MAINTENANCE",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.TOGGLE_MAINTENANCE",
                        "authorization_name": "Turn on/off maintenance mode",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/SERVICE.VIEW_ALERTS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_ALERTS",
                        "authorization_name": "View service-level alerts",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/SERVICE.VIEW_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_CONFIGS",
                        "authorization_name": "View configurations",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/SERVICE.VIEW_METRICS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_METRICS",
                        "authorization_name": "View metrics",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/SERVICE.VIEW_OPERATIONAL_LOGS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_OPERATIONAL_LOGS",
                        "authorization_name": "View service operational logs",
                        "permission_id": 3
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/3/authorizations/SERVICE.VIEW_STATUS_INFO",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_STATUS_INFO",
                        "authorization_name": "View status information",
                        "permission_id": 3
                    }
                }
            ]
        },
        {
            "href": "http://host.example.com:8080/api/v1/permissions/5",
            "PermissionInfo": {
                "permission_id": 5,
                "permission_label": "Cluster Operator",
                "permission_name": "CLUSTER.OPERATOR",
                "resource_name": "CLUSTER",
                "sort_order": 3
            },
            "authorizations": [
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/CLUSTER.MANAGE_AUTO_START",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.MANAGE_AUTO_START",
                        "authorization_name": "Manage service auto-start configuration",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/CLUSTER.MANAGE_CONFIG_GROUPS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.MANAGE_CONFIG_GROUPS",
                        "authorization_name": "Manage cluster config groups",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/CLUSTER.MANAGE_CREDENTIALS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.MANAGE_CREDENTIALS",
                        "authorization_name": "Manage external credentials",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/CLUSTER.MANAGE_USER_PERSISTED_DATA",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.MANAGE_USER_PERSISTED_DATA",
                        "authorization_name": "Manage cluster-level user persisted data",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/CLUSTER.MANAGE_WIDGETS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.MANAGE_WIDGETS",
                        "authorization_name": "Manage widgets",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/CLUSTER.VIEW_ALERTS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_ALERTS",
                        "authorization_name": "View cluster-level alerts",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/CLUSTER.VIEW_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_CONFIGS",
                        "authorization_name": "View configuration",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/CLUSTER.VIEW_METRICS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_METRICS",
                        "authorization_name": "View metrics",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/CLUSTER.VIEW_STACK_DETAILS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_STACK_DETAILS",
                        "authorization_name": "View stack version details",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/CLUSTER.VIEW_STATUS_INFO",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_STATUS_INFO",
                        "authorization_name": "View status information",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/HOST.ADD_DELETE_COMPONENTS",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.ADD_DELETE_COMPONENTS",
                        "authorization_name": "Install components",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/HOST.ADD_DELETE_HOSTS",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.ADD_DELETE_HOSTS",
                        "authorization_name": "Add/Delete hosts",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/HOST.TOGGLE_MAINTENANCE",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.TOGGLE_MAINTENANCE",
                        "authorization_name": "Turn on/off maintenance mode",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/HOST.VIEW_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.VIEW_CONFIGS",
                        "authorization_name": "View configuration",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/HOST.VIEW_METRICS",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.VIEW_METRICS",
                        "authorization_name": "View metrics",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/HOST.VIEW_STATUS_INFO",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.VIEW_STATUS_INFO",
                        "authorization_name": "View status information",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/SERVICE.COMPARE_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.COMPARE_CONFIGS",
                        "authorization_name": "Compare configurations",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/SERVICE.DECOMMISSION_RECOMMISSION",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.DECOMMISSION_RECOMMISSION",
                        "authorization_name": "Decommission/recommission",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/SERVICE.ENABLE_HA",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.ENABLE_HA",
                        "authorization_name": "Enable HA",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/SERVICE.MANAGE_AUTO_START",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.MANAGE_AUTO_START",
                        "authorization_name": "Manage service auto-start",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/SERVICE.MANAGE_CONFIG_GROUPS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.MANAGE_CONFIG_GROUPS",
                        "authorization_name": "Manage configuration groups",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/SERVICE.MODIFY_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.MODIFY_CONFIGS",
                        "authorization_name": "Modify configurations",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/SERVICE.MOVE",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.MOVE",
                        "authorization_name": "Move to another host",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/SERVICE.RUN_CUSTOM_COMMAND",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.RUN_CUSTOM_COMMAND",
                        "authorization_name": "Perform service-specific tasks",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/SERVICE.RUN_SERVICE_CHECK",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.RUN_SERVICE_CHECK",
                        "authorization_name": "Run service checks",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/SERVICE.START_STOP",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.START_STOP",
                        "authorization_name": "Start/Stop/Restart Service",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/SERVICE.TOGGLE_MAINTENANCE",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.TOGGLE_MAINTENANCE",
                        "authorization_name": "Turn on/off maintenance mode",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/SERVICE.VIEW_ALERTS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_ALERTS",
                        "authorization_name": "View service-level alerts",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/SERVICE.VIEW_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_CONFIGS",
                        "authorization_name": "View configurations",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/SERVICE.VIEW_METRICS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_METRICS",
                        "authorization_name": "View metrics",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/SERVICE.VIEW_OPERATIONAL_LOGS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_OPERATIONAL_LOGS",
                        "authorization_name": "View service operational logs",
                        "permission_id": 5
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/5/authorizations/SERVICE.VIEW_STATUS_INFO",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_STATUS_INFO",
                        "authorization_name": "View status information",
                        "permission_id": 5
                    }
                }
            ]
        },
        {
            "href": "http://host.example.com:8080/api/v1/permissions/6",
            "PermissionInfo": {
                "permission_id": 6,
                "permission_label": "Service Administrator",
                "permission_name": "SERVICE.ADMINISTRATOR",
                "resource_name": "CLUSTER",
                "sort_order": 4
            },
            "authorizations": [
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/6/authorizations/CLUSTER.MANAGE_CONFIG_GROUPS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.MANAGE_CONFIG_GROUPS",
                        "authorization_name": "Manage cluster config groups",
                        "permission_id": 6
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/6/authorizations/CLUSTER.MANAGE_USER_PERSISTED_DATA",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.MANAGE_USER_PERSISTED_DATA",
                        "authorization_name": "Manage cluster-level user persisted data",
                        "permission_id": 6
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/6/authorizations/CLUSTER.VIEW_ALERTS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_ALERTS",
                        "authorization_name": "View cluster-level alerts",
                        "permission_id": 6
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/6/authorizations/CLUSTER.VIEW_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_CONFIGS",
                        "authorization_name": "View configuration",
                        "permission_id": 6
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/6/authorizations/CLUSTER.VIEW_METRICS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_METRICS",
                        "authorization_name": "View metrics",
                        "permission_id": 6
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/6/authorizations/CLUSTER.VIEW_STACK_DETAILS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_STACK_DETAILS",
                        "authorization_name": "View stack version details",
                        "permission_id": 6
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/6/authorizations/CLUSTER.VIEW_STATUS_INFO",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_STATUS_INFO",
                        "authorization_name": "View status information",
                        "permission_id": 6
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/6/authorizations/HOST.VIEW_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.VIEW_CONFIGS",
                        "authorization_name": "View configuration",
                        "permission_id": 6
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/6/authorizations/HOST.VIEW_METRICS",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.VIEW_METRICS",
                        "authorization_name": "View metrics",
                        "permission_id": 6
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/6/authorizations/HOST.VIEW_STATUS_INFO",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.VIEW_STATUS_INFO",
                        "authorization_name": "View status information",
                        "permission_id": 6
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/6/authorizations/SERVICE.COMPARE_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.COMPARE_CONFIGS",
                        "authorization_name": "Compare configurations",
                        "permission_id": 6
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/6/authorizations/SERVICE.DECOMMISSION_RECOMMISSION",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.DECOMMISSION_RECOMMISSION",
                        "authorization_name": "Decommission/recommission",
                        "permission_id": 6
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/6/authorizations/SERVICE.MANAGE_AUTO_START",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.MANAGE_AUTO_START",
                        "authorization_name": "Manage service auto-start",
                        "permission_id": 6
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/6/authorizations/SERVICE.MANAGE_CONFIG_GROUPS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.MANAGE_CONFIG_GROUPS",
                        "authorization_name": "Manage configuration groups",
                        "permission_id": 6
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/6/authorizations/SERVICE.MODIFY_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.MODIFY_CONFIGS",
                        "authorization_name": "Modify configurations",
                        "permission_id": 6
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/6/authorizations/SERVICE.RUN_CUSTOM_COMMAND",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.RUN_CUSTOM_COMMAND",
                        "authorization_name": "Perform service-specific tasks",
                        "permission_id": 6
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/6/authorizations/SERVICE.RUN_SERVICE_CHECK",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.RUN_SERVICE_CHECK",
                        "authorization_name": "Run service checks",
                        "permission_id": 6
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/6/authorizations/SERVICE.START_STOP",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.START_STOP",
                        "authorization_name": "Start/Stop/Restart Service",
                        "permission_id": 6
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/6/authorizations/SERVICE.TOGGLE_MAINTENANCE",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.TOGGLE_MAINTENANCE",
                        "authorization_name": "Turn on/off maintenance mode",
                        "permission_id": 6
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/6/authorizations/SERVICE.VIEW_ALERTS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_ALERTS",
                        "authorization_name": "View service-level alerts",
                        "permission_id": 6
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/6/authorizations/SERVICE.VIEW_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_CONFIGS",
                        "authorization_name": "View configurations",
                        "permission_id": 6
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/6/authorizations/SERVICE.VIEW_METRICS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_METRICS",
                        "authorization_name": "View metrics",
                        "permission_id": 6
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/6/authorizations/SERVICE.VIEW_OPERATIONAL_LOGS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_OPERATIONAL_LOGS",
                        "authorization_name": "View service operational logs",
                        "permission_id": 6
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/6/authorizations/SERVICE.VIEW_STATUS_INFO",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_STATUS_INFO",
                        "authorization_name": "View status information",
                        "permission_id": 6
                    }
                }
            ]
        },
        {
            "href": "http://host.example.com:8080/api/v1/permissions/7",
            "PermissionInfo": {
                "permission_id": 7,
                "permission_label": "Service Operator",
                "permission_name": "SERVICE.OPERATOR",
                "resource_name": "CLUSTER",
                "sort_order": 5
            },
            "authorizations": [
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/7/authorizations/CLUSTER.MANAGE_USER_PERSISTED_DATA",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.MANAGE_USER_PERSISTED_DATA",
                        "authorization_name": "Manage cluster-level user persisted data",
                        "permission_id": 7
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/7/authorizations/CLUSTER.VIEW_ALERTS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_ALERTS",
                        "authorization_name": "View cluster-level alerts",
                        "permission_id": 7
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/7/authorizations/CLUSTER.VIEW_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_CONFIGS",
                        "authorization_name": "View configuration",
                        "permission_id": 7
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/7/authorizations/CLUSTER.VIEW_METRICS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_METRICS",
                        "authorization_name": "View metrics",
                        "permission_id": 7
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/7/authorizations/CLUSTER.VIEW_STACK_DETAILS",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_STACK_DETAILS",
                        "authorization_name": "View stack version details",
                        "permission_id": 7
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/7/authorizations/CLUSTER.VIEW_STATUS_INFO",
                    "AuthorizationInfo": {
                        "authorization_id": "CLUSTER.VIEW_STATUS_INFO",
                        "authorization_name": "View status information",
                        "permission_id": 7
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/7/authorizations/HOST.VIEW_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.VIEW_CONFIGS",
                        "authorization_name": "View configuration",
                        "permission_id": 7
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/7/authorizations/HOST.VIEW_METRICS",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.VIEW_METRICS",
                        "authorization_name": "View metrics",
                        "permission_id": 7
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/7/authorizations/HOST.VIEW_STATUS_INFO",
                    "AuthorizationInfo": {
                        "authorization_id": "HOST.VIEW_STATUS_INFO",
                        "authorization_name": "View status information",
                        "permission_id": 7
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/7/authorizations/SERVICE.COMPARE_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.COMPARE_CONFIGS",
                        "authorization_name": "Compare configurations",
                        "permission_id": 7
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/7/authorizations/SERVICE.DECOMMISSION_RECOMMISSION",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.DECOMMISSION_RECOMMISSION",
                        "authorization_name": "Decommission/recommission",
                        "permission_id": 7
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/7/authorizations/SERVICE.RUN_CUSTOM_COMMAND",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.RUN_CUSTOM_COMMAND",
                        "authorization_name": "Perform service-specific tasks",
                        "permission_id": 7
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/7/authorizations/SERVICE.RUN_SERVICE_CHECK",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.RUN_SERVICE_CHECK",
                        "authorization_name": "Run service checks",
                        "permission_id": 7
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/7/authorizations/SERVICE.START_STOP",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.START_STOP",
                        "authorization_name": "Start/Stop/Restart Service",
                        "permission_id": 7
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/7/authorizations/SERVICE.TOGGLE_MAINTENANCE",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.TOGGLE_MAINTENANCE",
                        "authorization_name": "Turn on/off maintenance mode",
                        "permission_id": 7
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/7/authorizations/SERVICE.VIEW_ALERTS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_ALERTS",
                        "authorization_name": "View service-level alerts",
                        "permission_id": 7
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/7/authorizations/SERVICE.VIEW_CONFIGS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_CONFIGS",
                        "authorization_name": "View configurations",
                        "permission_id": 7
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/7/authorizations/SERVICE.VIEW_METRICS",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_METRICS",
                        "authorization_name": "View metrics",
                        "permission_id": 7
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/permissions/7/authorizations/SERVICE.VIEW_STATUS_INFO",
                    "AuthorizationInfo": {
                        "authorization_id": "SERVICE.VIEW_STATUS_INFO",
                        "authorization_name": "View status information",
                        "permission_id": 7
                    }
                }
            ]
        }
    ]
}