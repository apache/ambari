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
export const userData = {
    "href": "http://host.example.com:8080/api/v1/users/dsasd?fields=privileges/PrivilegeInfo,Users",
    "Users": {
        "active": true,
        "admin": false,
        "consecutive_failures": 0,
        "created": 1724325120429,
        "display_name": "dsasd",
        "groups": [
            "gdgdeg",
            "group1"
        ],
        "ldap_user": false,
        "local_user_name": "dsasd",
        "user_name": "dsasd",
        "user_type": "LOCAL"
    },
    "privileges": [
        {
            "href": "http://host.example.com:8080/api/v1/users/dsasd/privileges/1005",
            "PrivilegeInfo": {
                "instance_name": "jukende",
                "permission_label": "View User",
                "permission_name": "VIEW.USER",
                "principal_name": "dsasd",
                "principal_type": "USER",
                "privilege_id": 1005,
                "type": "VIEW",
                "user_name": "dsasd",
                "version": "1.0.0",
                "view_name": "FILES"
            }
        },
        {
            "href": "http://host.example.com:8080/api/v1/users/dsasd/privileges/1017",
            "PrivilegeInfo": {
                "instance_name": "Files",
                "permission_label": "View User",
                "permission_name": "VIEW.USER",
                "principal_name": "group1",
                "principal_type": "GROUP",
                "privilege_id": 1017,
                "type": "VIEW",
                "user_name": "dsasd",
                "version": "1.0.0",
                "view_name": "CAPACITY-SCHEDULER"
            }
        },
        {
            "href": "http://host.example.com:8080/api/v1/users/dsasd/privileges/1019",
            "PrivilegeInfo": {
                "cluster_name": "testCluster",
                "permission_label": "Cluster User",
                "permission_name": "CLUSTER.USER",
                "principal_name": "dsasd",
                "principal_type": "USER",
                "privilege_id": 1019,
                "type": "CLUSTER",
                "user_name": "dsasd"
            }
        },
        {
            "href": "http://host.example.com:8080/api/v1/users/dsasd/privileges/1020",
            "PrivilegeInfo": {
                "instance_name": "2345678123456781234567812345678123456781212345678123456",
                "permission_label": "View User",
                "permission_name": "VIEW.USER",
                "principal_name": "dsasd",
                "principal_type": "USER",
                "privilege_id": 1020,
                "type": "VIEW",
                "user_name": "dsasd",
                "version": "1.0.0",
                "view_name": "CAPACITY-SCHEDULER"
            }
        },
        {
            "href": "http://host.example.com:8080/api/v1/users/dsasd/privileges/1021",
            "PrivilegeInfo": {
                "instance_name": "cdsadcwe@",
                "permission_label": "View User",
                "permission_name": "VIEW.USER",
                "principal_name": "dsasd",
                "principal_type": "USER",
                "privilege_id": 1021,
                "type": "VIEW",
                "user_name": "dsasd",
                "version": "1.0.0",
                "view_name": "FILES"
            }
        }
    ]
}