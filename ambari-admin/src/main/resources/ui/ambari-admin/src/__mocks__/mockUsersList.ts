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
export const usersList = {
    "href": "http://host.example.com:8080/api/v1/users?fields=Users/*,privileges/*&_=1725622903674",
    "items": [
      {
        "href": "http://host.example.com:8080/api/v1/users/12345678123456781234567812345678123456781234567812345678123456781234567812345678",
        "Users": {
          "active": true,
          "admin": false,
          "consecutive_failures": 0,
          "created": 1724736468537,
          "display_name": "12345678123456781234567812345678123456781234567812345678123456781234567812345678",
          "groups": [
            "group3",
            "group1"
          ],
          "ldap_user": false,
          "local_user_name": "12345678123456781234567812345678123456781234567812345678123456781234567812345678",
          "user_name": "12345678123456781234567812345678123456781234567812345678123456781234567812345678",
          "user_type": "LOCAL"
        },
        "privileges": [
          {
            "href": "http://host.example.com:8080/api/v1/users/12345678123456781234567812345678123456781234567812345678123456781234567812345678/privileges/802",
            "PrivilegeInfo": {
              "cluster_name": "dshcjhcwj",
              "permission_label": "Cluster User",
              "permission_name": "CLUSTER.USER",
              "principal_name": "12345678123456781234567812345678123456781234567812345678123456781234567812345678",
              "principal_type": "USER",
              "privilege_id": 802,
              "type": "CLUSTER",
              "user_name": "12345678123456781234567812345678123456781234567812345678123456781234567812345678"
            }
          }
        ]
      },
      {
        "href": "http://host.example.com:8080/api/v1/users/1234567890123456789012345678901234567890123456789",
        "Users": {
          "active": false,
          "admin": false,
          "consecutive_failures": 0,
          "created": 1724655063232,
          "display_name": "1234567890123456789012345678901234567890123456789",
          "groups": [],
          "ldap_user": false,
          "local_user_name": "1234567890123456789012345678901234567890123456789",
          "user_name": "1234567890123456789012345678901234567890123456789",
          "user_type": "LOCAL"
        },
        "privileges": [
          {
            "href": "http://host.example.com:8080/api/v1/users/1234567890123456789012345678901234567890123456789/privileges/755",
            "PrivilegeInfo": {
              "cluster_name": "dshcjhcwj",
              "permission_label": "Cluster User",
              "permission_name": "CLUSTER.USER",
              "principal_name": "1234567890123456789012345678901234567890123456789",
              "principal_type": "USER",
              "privilege_id": 755,
              "type": "CLUSTER",
              "user_name": "1234567890123456789012345678901234567890123456789"
            }
          }
        ]
      },
      {
        "href": "http://host.example.com:8080/api/v1/users/12345678901234567890123456789012345678901234567890123456789012345678901234567890",
        "Users": {
          "active": true,
          "admin": false,
          "consecutive_failures": 0,
          "created": 1725360999195,
          "display_name": "12345678901234567890123456789012345678901234567890123456789012345678901234567890",
          "groups": [],
          "ldap_user": false,
          "local_user_name": "12345678901234567890123456789012345678901234567890123456789012345678901234567890",
          "user_name": "12345678901234567890123456789012345678901234567890123456789012345678901234567890",
          "user_type": "LOCAL"
        },
        "privileges": []
      },
      {
        "href": "http://host.example.com:8080/api/v1/users/admin",
        "Users": {
          "active": true,
          "admin": true,
          "consecutive_failures": 0,
          "created": 1717156373000,
          "display_name": "Administrator",
          "groups": [
            "test",
            "ferfew",
            "vdfgdsf",
            "hmaurya.12"
          ],
          "ldap_user": false,
          "local_user_name": "admin",
          "user_name": "admin",
          "user_type": "LOCAL"
        },
        "privileges": [
          {
            "href": "http://host.example.com:8080/api/v1/users/admin/privileges/1",
            "PrivilegeInfo": {
              "permission_label": "Ambari Administrator",
              "permission_name": "AMBARI.ADMINISTRATOR",
              "principal_name": "admin",
              "principal_type": "USER",
              "privilege_id": 1,
              "type": "AMBARI",
              "user_name": "admin"
            }
          },
          {
            "href": "http://host.example.com:8080/api/v1/users/admin/privileges/397",
            "PrivilegeInfo": {
              "instance_name": "sjhadja",
              "permission_label": "View User",
              "permission_name": "VIEW.USER",
              "principal_name": "admin",
              "principal_type": "USER",
              "privilege_id": 397,
              "type": "VIEW",
              "user_name": "admin",
              "version": "1.0.0",
              "view_name": "CAPACITY-SCHEDULER"
            }
          },
          {
            "href": "http://host.example.com:8080/api/v1/users/admin/privileges/404",
            "PrivilegeInfo": {
              "instance_name": "jfkdfd",
              "permission_label": "View User",
              "permission_name": "VIEW.USER",
              "principal_name": "admin",
              "principal_type": "USER",
              "privilege_id": 404,
              "type": "VIEW",
              "user_name": "admin",
              "version": "1.0.0",
              "view_name": "CAPACITY-SCHEDULER"
            }
          },
          {
            "href": "http://host.example.com:8080/api/v1/users/admin/privileges/558",
            "PrivilegeInfo": {
              "instance_name": "Files",
              "permission_label": "View User",
              "permission_name": "VIEW.USER",
              "principal_name": "admin",
              "principal_type": "USER",
              "privilege_id": 558,
              "type": "VIEW",
              "user_name": "admin",
              "version": "1.0.0",
              "view_name": "FILES"
            }
          },
          {
            "href": "http://host.example.com:8080/api/v1/users/admin/privileges/657",
            "PrivilegeInfo": {
              "instance_name": "Files2",
              "permission_label": "View User",
              "permission_name": "VIEW.USER",
              "principal_name": "admin",
              "principal_type": "USER",
              "privilege_id": 657,
              "type": "VIEW",
              "user_name": "admin",
              "version": "1.0.0",
              "view_name": "FILES"
            }
          },
          {
            "href": "http://host.example.com:8080/api/v1/users/admin/privileges/754",
            "PrivilegeInfo": {
              "cluster_name": "dshcjhcwj",
              "permission_label": "Cluster Administrator",
              "permission_name": "CLUSTER.ADMINISTRATOR",
              "principal_name": "hmaurya.12",
              "principal_type": "GROUP",
              "privilege_id": 754,
              "type": "CLUSTER",
              "user_name": "admin"
            }
          }
        ]
      },
      {
        "href": "http://host.example.com:8080/api/v1/users/dsasd",
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
            "href": "http://host.example.com:8080/api/v1/users/dsasd/privileges/702",
            "PrivilegeInfo": {
              "cluster_name": "dshcjhcwj",
              "permission_label": "Cluster Administrator",
              "permission_name": "CLUSTER.ADMINISTRATOR",
              "principal_name": "dsasd",
              "principal_type": "USER",
              "privilege_id": 702,
              "type": "CLUSTER",
              "user_name": "dsasd"
            }
          }
        ]
      },
      {
        "href": "http://host.example.com:8080/api/v1/users/fghhjhjh",
        "Users": {
          "active": true,
          "admin": false,
          "consecutive_failures": 0,
          "created": 1725355896220,
          "display_name": "fghhjhjh",
          "groups": [
            "ferfew"
          ],
          "ldap_user": false,
          "local_user_name": "fghhjhjh",
          "user_name": "fghhjhjh",
          "user_type": "LOCAL"
        },
        "privileges": []
      },
      {
        "href": "http://host.example.com:8080/api/v1/users/himanshu%40h",
        "Users": {
          "active": true,
          "admin": false,
          "consecutive_failures": 0,
          "created": 1724652183474,
          "display_name": "Himanshu@h",
          "groups": [
            "ferfew",
            "gdgdeg"
          ],
          "ldap_user": false,
          "local_user_name": "Himanshu@h",
          "user_name": "himanshu@h",
          "user_type": "LOCAL"
        },
        "privileges": [
          {
            "href": "http://host.example.com:8080/api/v1/users/himanshu%40h/privileges/753",
            "PrivilegeInfo": {
              "cluster_name": "dshcjhcwj",
              "permission_label": "Cluster Administrator",
              "permission_name": "CLUSTER.ADMINISTRATOR",
              "principal_name": "himanshu@h",
              "principal_type": "USER",
              "privilege_id": 753,
              "type": "CLUSTER",
              "user_name": "himanshu@h"
            }
          },
          {
            "href": "http://host.example.com:8080/api/v1/users/himanshu%40h/privileges/850",
            "PrivilegeInfo": {
              "instance_name": "asa",
              "permission_label": "View User",
              "permission_name": "VIEW.USER",
              "principal_name": "himanshu@h",
              "principal_type": "USER",
              "privilege_id": 850,
              "type": "VIEW",
              "user_name": "himanshu@h",
              "version": "1.0.0",
              "view_name": "CAPACITY-SCHEDULER"
            }
          },
          {
            "href": "http://host.example.com:8080/api/v1/users/himanshu%40h/privileges/851",
            "PrivilegeInfo": {
              "instance_name": "cscddwc",
              "permission_label": "View User",
              "permission_name": "VIEW.USER",
              "principal_name": "himanshu@h",
              "principal_type": "USER",
              "privilege_id": 851,
              "type": "VIEW",
              "user_name": "himanshu@h",
              "version": "1.0.0",
              "view_name": "CAPACITY-SCHEDULER"
            }
          },
          {
            "href": "http://host.example.com:8080/api/v1/users/himanshu%40h/privileges/855",
            "PrivilegeInfo": {
              "instance_name": "12345678901234567890123456789012345678901234567890123456789012345678901234567890",
              "permission_label": "View User",
              "permission_name": "VIEW.USER",
              "principal_name": "himanshu@h",
              "principal_type": "USER",
              "privilege_id": 855,
              "type": "VIEW",
              "user_name": "himanshu@h",
              "version": "1.0.0",
              "view_name": "CAPACITY-SCHEDULER"
            }
          }
        ]
      },
      {
        "href": "http://host.example.com:8080/api/v1/users/jdgjsaas",
        "Users": {
          "active": true,
          "admin": true,
          "consecutive_failures": 0,
          "created": 1723460606840,
          "display_name": "jdgjsaas",
          "groups": [
            "group3",
            "group2",
            "group1"
          ],
          "ldap_user": false,
          "local_user_name": "jdgjsaas",
          "user_name": "jdgjsaas",
          "user_type": "LOCAL"
        },
        "privileges": [
          {
            "href": "http://host.example.com:8080/api/v1/users/jdgjsaas/privileges/654",
            "PrivilegeInfo": {
              "instance_name": "test5",
              "permission_label": "View User",
              "permission_name": "VIEW.USER",
              "principal_name": "jdgjsaas",
              "principal_type": "USER",
              "privilege_id": 654,
              "type": "VIEW",
              "user_name": "jdgjsaas",
              "version": "1.0.0",
              "view_name": "FILES"
            }
          },
          {
            "href": "http://host.example.com:8080/api/v1/users/jdgjsaas/privileges/656",
            "PrivilegeInfo": {
              "instance_name": "Files2",
              "permission_label": "View User",
              "permission_name": "VIEW.USER",
              "principal_name": "jdgjsaas",
              "principal_type": "USER",
              "privilege_id": 656,
              "type": "VIEW",
              "user_name": "jdgjsaas",
              "version": "1.0.0",
              "view_name": "FILES"
            }
          },
          {
            "href": "http://host.example.com:8080/api/v1/users/jdgjsaas/privileges/703",
            "PrivilegeInfo": {
              "cluster_name": "dshcjhcwj",
              "permission_label": "Cluster Administrator",
              "permission_name": "CLUSTER.ADMINISTRATOR",
              "principal_name": "jdgjsaas",
              "principal_type": "USER",
              "privilege_id": 703,
              "type": "CLUSTER",
              "user_name": "jdgjsaas"
            }
          },
          {
            "href": "http://host.example.com:8080/api/v1/users/jdgjsaas/privileges/752",
            "PrivilegeInfo": {
              "permission_label": "Ambari Administrator",
              "permission_name": "AMBARI.ADMINISTRATOR",
              "principal_name": "jdgjsaas",
              "principal_type": "USER",
              "privilege_id": 752,
              "type": "AMBARI",
              "user_name": "jdgjsaas"
            }
          }
        ]
      },
      {
        "href": "http://host.example.com:8080/api/v1/users/sandeep",
        "Users": {
          "active": true,
          "admin": false,
          "consecutive_failures": 0,
          "created": 1724143724415,
          "display_name": "sandeep",
          "groups": [],
          "ldap_user": false,
          "local_user_name": "sandeep",
          "user_name": "sandeep",
          "user_type": "LOCAL"
        },
        "privileges": []
      }
    ]
  }
  
  export const paginatedUsersList = {
    "href": "http://host.example.com:8080/api/v1/users?fields=Users/*,privileges/*&_=1725622903674",
    "items": [
      {
        "href": "http://host.example.com:8080/api/v1/users/12345678123456781234567812345678123456781234567812345678123456781234567812345678",
        "Users": {
          "active": true,
          "admin": false,
          "consecutive_failures": 0,
          "created": 1724736468537,
          "display_name": "12345678123456781234567812345678123456781234567812345678123456781234567812345678",
          "groups": [
            "group3",
            "group1"
          ],
          "ldap_user": false,
          "local_user_name": "12345678123456781234567812345678123456781234567812345678123456781234567812345678",
          "user_name": "12345678123456781234567812345678123456781234567812345678123456781234567812345678",
          "user_type": "LOCAL"
        },
        "privileges": [
          {
            "href": "http://host.example.com:8080/api/v1/users/12345678123456781234567812345678123456781234567812345678123456781234567812345678/privileges/802",
            "PrivilegeInfo": {
              "cluster_name": "dshcjhcwj",
              "permission_label": "Cluster User",
              "permission_name": "CLUSTER.USER",
              "principal_name": "12345678123456781234567812345678123456781234567812345678123456781234567812345678",
              "principal_type": "USER",
              "privilege_id": 802,
              "type": "CLUSTER",
              "user_name": "12345678123456781234567812345678123456781234567812345678123456781234567812345678"
            }
          }
        ]
      },
      {
        "href": "http://host.example.com:8080/api/v1/users/1234567890123456789012345678901234567890123456789",
        "Users": {
          "active": false,
          "admin": false,
          "consecutive_failures": 0,
          "created": 1724655063232,
          "display_name": "1234567890123456789012345678901234567890123456789",
          "groups": [],
          "ldap_user": false,
          "local_user_name": "1234567890123456789012345678901234567890123456789",
          "user_name": "1234567890123456789012345678901234567890123456789",
          "user_type": "LOCAL"
        },
        "privileges": [
          {
            "href": "http://host.example.com:8080/api/v1/users/1234567890123456789012345678901234567890123456789/privileges/755",
            "PrivilegeInfo": {
              "cluster_name": "dshcjhcwj",
              "permission_label": "Cluster User",
              "permission_name": "CLUSTER.USER",
              "principal_name": "1234567890123456789012345678901234567890123456789",
              "principal_type": "USER",
              "privilege_id": 755,
              "type": "CLUSTER",
              "user_name": "1234567890123456789012345678901234567890123456789"
            }
          }
        ]
      },
      {
        "href": "http://host.example.com:8080/api/v1/users/12345678901234567890123456789012345678901234567890123456789012345678901234567890",
        "Users": {
          "active": true,
          "admin": false,
          "consecutive_failures": 0,
          "created": 1725360999195,
          "display_name": "12345678901234567890123456789012345678901234567890123456789012345678901234567890",
          "groups": [],
          "ldap_user": false,
          "local_user_name": "12345678901234567890123456789012345678901234567890123456789012345678901234567890",
          "user_name": "12345678901234567890123456789012345678901234567890123456789012345678901234567890",
          "user_type": "LOCAL"
        },
        "privileges": []
      },
      {
        "href": "http://host.example.com:8080/api/v1/users/admin",
        "Users": {
          "active": true,
          "admin": true,
          "consecutive_failures": 0,
          "created": 1717156373000,
          "display_name": "Administrator",
          "groups": [
            "test",
            "ferfew",
            "vdfgdsf",
            "hmaurya.12"
          ],
          "ldap_user": false,
          "local_user_name": "admin",
          "user_name": "admin",
          "user_type": "LOCAL"
        },
        "privileges": [
          {
            "href": "http://host.example.com:8080/api/v1/users/admin/privileges/1",
            "PrivilegeInfo": {
              "permission_label": "Ambari Administrator",
              "permission_name": "AMBARI.ADMINISTRATOR",
              "principal_name": "admin",
              "principal_type": "USER",
              "privilege_id": 1,
              "type": "AMBARI",
              "user_name": "admin"
            }
          },
          {
            "href": "http://host.example.com:8080/api/v1/users/admin/privileges/397",
            "PrivilegeInfo": {
              "instance_name": "sjhadja",
              "permission_label": "View User",
              "permission_name": "VIEW.USER",
              "principal_name": "admin",
              "principal_type": "USER",
              "privilege_id": 397,
              "type": "VIEW",
              "user_name": "admin",
              "version": "1.0.0",
              "view_name": "CAPACITY-SCHEDULER"
            }
          },
          {
            "href": "http://host.example.com:8080/api/v1/users/admin/privileges/404",
            "PrivilegeInfo": {
              "instance_name": "jfkdfd",
              "permission_label": "View User",
              "permission_name": "VIEW.USER",
              "principal_name": "admin",
              "principal_type": "USER",
              "privilege_id": 404,
              "type": "VIEW",
              "user_name": "admin",
              "version": "1.0.0",
              "view_name": "CAPACITY-SCHEDULER"
            }
          },
          {
            "href": "http://host.example.com:8080/api/v1/users/admin/privileges/558",
            "PrivilegeInfo": {
              "instance_name": "Files",
              "permission_label": "View User",
              "permission_name": "VIEW.USER",
              "principal_name": "admin",
              "principal_type": "USER",
              "privilege_id": 558,
              "type": "VIEW",
              "user_name": "admin",
              "version": "1.0.0",
              "view_name": "FILES"
            }
          },
          {
            "href": "http://host.example.com:8080/api/v1/users/admin/privileges/657",
            "PrivilegeInfo": {
              "instance_name": "Files2",
              "permission_label": "View User",
              "permission_name": "VIEW.USER",
              "principal_name": "admin",
              "principal_type": "USER",
              "privilege_id": 657,
              "type": "VIEW",
              "user_name": "admin",
              "version": "1.0.0",
              "view_name": "FILES"
            }
          },
          {
            "href": "http://host.example.com:8080/api/v1/users/admin/privileges/754",
            "PrivilegeInfo": {
              "cluster_name": "dshcjhcwj",
              "permission_label": "Cluster Administrator",
              "permission_name": "CLUSTER.ADMINISTRATOR",
              "principal_name": "hmaurya.12",
              "principal_type": "GROUP",
              "privilege_id": 754,
              "type": "CLUSTER",
              "user_name": "admin"
            }
          }
        ]
      },
      {
        "href": "http://host.example.com:8080/api/v1/users/dsasd",
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
            "href": "http://host.example.com:8080/api/v1/users/dsasd/privileges/702",
            "PrivilegeInfo": {
              "cluster_name": "dshcjhcwj",
              "permission_label": "Cluster Administrator",
              "permission_name": "CLUSTER.ADMINISTRATOR",
              "principal_name": "dsasd",
              "principal_type": "USER",
              "privilege_id": 702,
              "type": "CLUSTER",
              "user_name": "dsasd"
            }
          }
        ]
      },
      {
        "href": "http://host.example.com:8080/api/v1/users/fghhjhjh",
        "Users": {
          "active": true,
          "admin": false,
          "consecutive_failures": 0,
          "created": 1725355896220,
          "display_name": "fghhjhjh",
          "groups": [
            "ferfew"
          ],
          "ldap_user": false,
          "local_user_name": "fghhjhjh",
          "user_name": "fghhjhjh",
          "user_type": "LOCAL"
        },
        "privileges": []
      },
      {
        "href": "http://host.example.com:8080/api/v1/users/himanshu%40h",
        "Users": {
          "active": true,
          "admin": false,
          "consecutive_failures": 0,
          "created": 1724652183474,
          "display_name": "Himanshu@h",
          "groups": [
            "ferfew",
            "gdgdeg"
          ],
          "ldap_user": false,
          "local_user_name": "Himanshu@h",
          "user_name": "himanshu@h",
          "user_type": "LOCAL"
        },
        "privileges": [
          {
            "href": "http://host.example.com:8080/api/v1/users/himanshu%40h/privileges/753",
            "PrivilegeInfo": {
              "cluster_name": "dshcjhcwj",
              "permission_label": "Cluster Administrator",
              "permission_name": "CLUSTER.ADMINISTRATOR",
              "principal_name": "himanshu@h",
              "principal_type": "USER",
              "privilege_id": 753,
              "type": "CLUSTER",
              "user_name": "himanshu@h"
            }
          },
          {
            "href": "http://host.example.com:8080/api/v1/users/himanshu%40h/privileges/850",
            "PrivilegeInfo": {
              "instance_name": "asa",
              "permission_label": "View User",
              "permission_name": "VIEW.USER",
              "principal_name": "himanshu@h",
              "principal_type": "USER",
              "privilege_id": 850,
              "type": "VIEW",
              "user_name": "himanshu@h",
              "version": "1.0.0",
              "view_name": "CAPACITY-SCHEDULER"
            }
          },
          {
            "href": "http://host.example.com:8080/api/v1/users/himanshu%40h/privileges/851",
            "PrivilegeInfo": {
              "instance_name": "cscddwc",
              "permission_label": "View User",
              "permission_name": "VIEW.USER",
              "principal_name": "himanshu@h",
              "principal_type": "USER",
              "privilege_id": 851,
              "type": "VIEW",
              "user_name": "himanshu@h",
              "version": "1.0.0",
              "view_name": "CAPACITY-SCHEDULER"
            }
          },
          {
            "href": "http://host.example.com:8080/api/v1/users/himanshu%40h/privileges/855",
            "PrivilegeInfo": {
              "instance_name": "12345678901234567890123456789012345678901234567890123456789012345678901234567890",
              "permission_label": "View User",
              "permission_name": "VIEW.USER",
              "principal_name": "himanshu@h",
              "principal_type": "USER",
              "privilege_id": 855,
              "type": "VIEW",
              "user_name": "himanshu@h",
              "version": "1.0.0",
              "view_name": "CAPACITY-SCHEDULER"
            }
          }
        ]
      },
      {
        "href": "http://host.example.com:8080/api/v1/users/jdgjsaas",
        "Users": {
          "active": true,
          "admin": true,
          "consecutive_failures": 0,
          "created": 1723460606840,
          "display_name": "jdgjsaas",
          "groups": [
            "group3",
            "group2",
            "group1"
          ],
          "ldap_user": false,
          "local_user_name": "jdgjsaas",
          "user_name": "jdgjsaas",
          "user_type": "LOCAL"
        },
        "privileges": [
          {
            "href": "http://host.example.com:8080/api/v1/users/jdgjsaas/privileges/654",
            "PrivilegeInfo": {
              "instance_name": "test5",
              "permission_label": "View User",
              "permission_name": "VIEW.USER",
              "principal_name": "jdgjsaas",
              "principal_type": "USER",
              "privilege_id": 654,
              "type": "VIEW",
              "user_name": "jdgjsaas",
              "version": "1.0.0",
              "view_name": "FILES"
            }
          },
          {
            "href": "http://host.example.com:8080/api/v1/users/jdgjsaas/privileges/656",
            "PrivilegeInfo": {
              "instance_name": "Files2",
              "permission_label": "View User",
              "permission_name": "VIEW.USER",
              "principal_name": "jdgjsaas",
              "principal_type": "USER",
              "privilege_id": 656,
              "type": "VIEW",
              "user_name": "jdgjsaas",
              "version": "1.0.0",
              "view_name": "FILES"
            }
          },
          {
            "href": "http://host.example.com:8080/api/v1/users/jdgjsaas/privileges/703",
            "PrivilegeInfo": {
              "cluster_name": "dshcjhcwj",
              "permission_label": "Cluster Administrator",
              "permission_name": "CLUSTER.ADMINISTRATOR",
              "principal_name": "jdgjsaas",
              "principal_type": "USER",
              "privilege_id": 703,
              "type": "CLUSTER",
              "user_name": "jdgjsaas"
            }
          },
          {
            "href": "http://host.example.com:8080/api/v1/users/jdgjsaas/privileges/752",
            "PrivilegeInfo": {
              "permission_label": "Ambari Administrator",
              "permission_name": "AMBARI.ADMINISTRATOR",
              "principal_name": "jdgjsaas",
              "principal_type": "USER",
              "privilege_id": 752,
              "type": "AMBARI",
              "user_name": "jdgjsaas"
            }
          }
        ]
      },
      {
        "href": "http://host.example.com:8080/api/v1/users/sandeep",
        "Users": {
          "active": true,
          "admin": false,
          "consecutive_failures": 0,
          "created": 1724143724415,
          "display_name": "sandeep",
          "groups": [],
          "ldap_user": false,
          "local_user_name": "sandeep",
          "user_name": "sandeep",
          "user_type": "LOCAL"
        },
        "privileges": []
      },
      {
        "href": "http://host.example.com:8080/api/v1/users/scscadcsa",
        "Users": {
          "active": true,
          "admin": false,
          "consecutive_failures": 0,
          "created": 1721627578812,
          "display_name": "scscadcsa",
          "groups": [],
          "ldap_user": false,
          "local_user_name": "scscadcsa",
          "user_name": "scscadcsa",
          "user_type": "LOCAL"
        },
        "privileges": [
          {
            "href": "http://host.example.com:8080/api/v1/users/scscadcsa/privileges/653",
            "PrivilegeInfo": {
              "instance_name": "qrwre",
              "permission_label": "View User",
              "permission_name": "VIEW.USER",
              "principal_name": "scscadcsa",
              "principal_type": "USER",
              "privilege_id": 653,
              "type": "VIEW",
              "user_name": "scscadcsa",
              "version": "1.0.0",
              "view_name": "CAPACITY-SCHEDULER"
            }
          },
          {
            "href": "http://host.example.com:8080/api/v1/users/scscadcsa/privileges/854",
            "PrivilegeInfo": {
              "cluster_name": "dshcjhcwj",
              "permission_label": "Cluster User",
              "permission_name": "CLUSTER.USER",
              "principal_name": "scscadcsa",
              "principal_type": "USER",
              "privilege_id": 854,
              "type": "CLUSTER",
              "user_name": "scscadcsa"
            }
          }
        ]
      },
      {
        "href": "http://host.example.com:8080/api/v1/users/test1",
        "Users": {
          "active": true,
          "admin": false,
          "consecutive_failures": 0,
          "created": 1723549891953,
          "display_name": "test1",
          "groups": [],
          "ldap_user": false,
          "local_user_name": "test1",
          "user_name": "test1",
          "user_type": "LOCAL"
        },
        "privileges": []
      },
      {
        "href": "http://host.example.com:8080/api/v1/users/test10",
        "Users": {
          "active": false,
          "admin": false,
          "consecutive_failures": 0,
          "created": 1723559647764,
          "display_name": "test10",
          "groups": [],
          "ldap_user": false,
          "local_user_name": "test10",
          "user_name": "test10",
          "user_type": "LOCAL"
        },
        "privileges": []
      }
    ]
  }