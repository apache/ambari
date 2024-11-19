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
export const groupData = {
    "href": "http://host.example.com:8080/api/v1/groups/group5?fields=Groups,privileges/PrivilegeInfo/*,members/MemberInfo&_=1726662954266",
    "Groups": {
        "group_name": "group5",
        "group_type": "LOCAL",
        "ldap_group": false
    },
    "privileges": [
        {
            "href": "http://host.example.com:8080/api/v1/groups/group5/privileges/1007",
            "PrivilegeInfo": {
                "group_name": "group5",
                "instance_name": "jukende",
                "permission_label": "View User",
                "permission_name": "VIEW.USER",
                "principal_name": "group5",
                "principal_type": "GROUP",
                "privilege_id": 1007,
                "type": "VIEW",
                "version": "1.0.0",
                "view_name": "FILES"
            }
        },
        {
            "href": "http://host.example.com:8080/api/v1/groups/group5/privileges/1068",
            "PrivilegeInfo": {
                "cluster_name": "testCluster",
                "group_name": "group5",
                "permission_label": "Cluster User",
                "permission_name": "CLUSTER.USER",
                "principal_name": "group5",
                "principal_type": "GROUP",
                "privilege_id": 1068,
                "type": "CLUSTER"
            }
        }
    ],
    "members": [
        {
            "href": "http://host.example.com:8080/api/v1/groups/group5/members/dsasd",
            "MemberInfo": {
                "group_name": "group5",
                "user_name": "dsasd"
            }
        }
    ]
}