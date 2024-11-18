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
export const groupsList = {
    "href": "http://host.example.com:8080/api/v1/groups?fields=*&_=1725623294531",
    "items": [
        {
            "href": "http://host.example.com:8080/api/v1/groups/56123478123456781234567812345678123456781234567812345678123456781234567812345678",
            "Groups": {
                "group_name": "56123478123456781234567812345678123456781234567812345678123456781234567812345678",
                "group_type": "LOCAL",
                "ldap_group": false
            },
            "privileges": [],
            "members": []
        },
        {
            "href": "http://host.example.com:8080/api/v1/groups/ferfew",
            "Groups": {
                "group_name": "ferfew",
                "group_type": "LOCAL",
                "ldap_group": false
            },
            "privileges": [],
            "members": [
                {
                    "href": "http://host.example.com:8080/api/v1/groups/ferfew/members/admin",
                    "MemberInfo": {
                        "group_name": "ferfew",
                        "user_name": "admin"
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/groups/ferfew/members/fghhjhjh",
                    "MemberInfo": {
                        "group_name": "ferfew",
                        "user_name": "fghhjhjh"
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/groups/ferfew/members/himanshu%40h",
                    "MemberInfo": {
                        "group_name": "ferfew",
                        "user_name": "himanshu@h"
                    }
                }
            ]
        },
        {
            "href": "http://host.example.com:8080/api/v1/groups/gdgdeg",
            "Groups": {
                "group_name": "gdgdeg",
                "group_type": "LOCAL",
                "ldap_group": false
            },
            "privileges": [],
            "members": [
                {
                    "href": "http://host.example.com:8080/api/v1/groups/gdgdeg/members/dsasd",
                    "MemberInfo": {
                        "group_name": "gdgdeg",
                        "user_name": "dsasd"
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/groups/gdgdeg/members/himanshu%40h",
                    "MemberInfo": {
                        "group_name": "gdgdeg",
                        "user_name": "himanshu@h"
                    }
                }
            ]
        },
        {
            "href": "http://host.example.com:8080/api/v1/groups/group1",
            "Groups": {
                "group_name": "group1",
                "group_type": "LOCAL",
                "ldap_group": false
            },
            "privileges": [],
            "members": [
                {
                    "href": "http://host.example.com:8080/api/v1/groups/group1/members/12345678123456781234567812345678123456781234567812345678123456781234567812345678",
                    "MemberInfo": {
                        "group_name": "group1",
                        "user_name": "12345678123456781234567812345678123456781234567812345678123456781234567812345678"
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/groups/group1/members/dsasd",
                    "MemberInfo": {
                        "group_name": "group1",
                        "user_name": "dsasd"
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/groups/group1/members/jdgjsaas",
                    "MemberInfo": {
                        "group_name": "group1",
                        "user_name": "jdgjsaas"
                    }
                }
            ]
        },
        {
            "href": "http://host.example.com:8080/api/v1/groups/group2",
            "Groups": {
                "group_name": "group2",
                "group_type": "LOCAL",
                "ldap_group": false
            },
            "privileges": [],
            "members": [
                {
                    "href": "http://host.example.com:8080/api/v1/groups/group2/members/jdgjsaas",
                    "MemberInfo": {
                        "group_name": "group2",
                        "user_name": "jdgjsaas"
                    }
                }
            ]
        },
        {
            "href": "http://host.example.com:8080/api/v1/groups/group3",
            "Groups": {
                "group_name": "group3",
                "group_type": "LOCAL",
                "ldap_group": false
            },
            "privileges": [],
            "members": [
                {
                    "href": "http://host.example.com:8080/api/v1/groups/group3/members/12345678123456781234567812345678123456781234567812345678123456781234567812345678",
                    "MemberInfo": {
                        "group_name": "group3",
                        "user_name": "12345678123456781234567812345678123456781234567812345678123456781234567812345678"
                    }
                },
                {
                    "href": "http://host.example.com:8080/api/v1/groups/group3/members/jdgjsaas",
                    "MemberInfo": {
                        "group_name": "group3",
                        "user_name": "jdgjsaas"
                    }
                }
            ]
        },
        {
            "href": "http://host.example.com:8080/api/v1/groups/group4",
            "Groups": {
                "group_name": "group4",
                "group_type": "LOCAL",
                "ldap_group": false
            },
            "privileges": [],
            "members": []
        },
        {
            "href": "http://host.example.com:8080/api/v1/groups/group5",
            "Groups": {
                "group_name": "group5",
                "group_type": "LOCAL",
                "ldap_group": false
            },
            "privileges": [],
            "members": []
        }
    ]
  }
  
  export const paginatedGroupsList = {
      "href": "http://host.example.com:8080/api/v1/groups?fields=*&_=1725623294531",
      "items": [
          {
              "href": "http://host.example.com:8080/api/v1/groups/56123478123456781234567812345678123456781234567812345678123456781234567812345678",
              "Groups": {
                  "group_name": "56123478123456781234567812345678123456781234567812345678123456781234567812345678",
                  "group_type": "LOCAL",
                  "ldap_group": false
              },
              "privileges": [],
              "members": []
          },
          {
              "href": "http://host.example.com:8080/api/v1/groups/ferfew",
              "Groups": {
                  "group_name": "ferfew",
                  "group_type": "LOCAL",
                  "ldap_group": false
              },
              "privileges": [],
              "members": [
                  {
                      "href": "http://host.example.com:8080/api/v1/groups/ferfew/members/admin",
                      "MemberInfo": {
                          "group_name": "ferfew",
                          "user_name": "admin"
                      }
                  },
                  {
                      "href": "http://host.example.com:8080/api/v1/groups/ferfew/members/fghhjhjh",
                      "MemberInfo": {
                          "group_name": "ferfew",
                          "user_name": "fghhjhjh"
                      }
                  },
                  {
                      "href": "http://host.example.com:8080/api/v1/groups/ferfew/members/himanshu%40h",
                      "MemberInfo": {
                          "group_name": "ferfew",
                          "user_name": "himanshu@h"
                      }
                  }
              ]
          },
          {
              "href": "http://host.example.com:8080/api/v1/groups/gdgdeg",
              "Groups": {
                  "group_name": "gdgdeg",
                  "group_type": "LOCAL",
                  "ldap_group": false
              },
              "privileges": [],
              "members": [
                  {
                      "href": "http://host.example.com:8080/api/v1/groups/gdgdeg/members/dsasd",
                      "MemberInfo": {
                          "group_name": "gdgdeg",
                          "user_name": "dsasd"
                      }
                  },
                  {
                      "href": "http://host.example.com:8080/api/v1/groups/gdgdeg/members/himanshu%40h",
                      "MemberInfo": {
                          "group_name": "gdgdeg",
                          "user_name": "himanshu@h"
                      }
                  }
              ]
          },
          {
              "href": "http://host.example.com:8080/api/v1/groups/group1",
              "Groups": {
                  "group_name": "group1",
                  "group_type": "LOCAL",
                  "ldap_group": false
              },
              "privileges": [],
              "members": [
                  {
                      "href": "http://host.example.com:8080/api/v1/groups/group1/members/12345678123456781234567812345678123456781234567812345678123456781234567812345678",
                      "MemberInfo": {
                          "group_name": "group1",
                          "user_name": "12345678123456781234567812345678123456781234567812345678123456781234567812345678"
                      }
                  },
                  {
                      "href": "http://host.example.com:8080/api/v1/groups/group1/members/dsasd",
                      "MemberInfo": {
                          "group_name": "group1",
                          "user_name": "dsasd"
                      }
                  },
                  {
                      "href": "http://host.example.com:8080/api/v1/groups/group1/members/jdgjsaas",
                      "MemberInfo": {
                          "group_name": "group1",
                          "user_name": "jdgjsaas"
                      }
                  }
              ]
          },
          {
              "href": "http://host.example.com:8080/api/v1/groups/group2",
              "Groups": {
                  "group_name": "group2",
                  "group_type": "LOCAL",
                  "ldap_group": false
              },
              "privileges": [],
              "members": [
                  {
                      "href": "http://host.example.com:8080/api/v1/groups/group2/members/jdgjsaas",
                      "MemberInfo": {
                          "group_name": "group2",
                          "user_name": "jdgjsaas"
                      }
                  }
              ]
          },
          {
              "href": "http://host.example.com:8080/api/v1/groups/group3",
              "Groups": {
                  "group_name": "group3",
                  "group_type": "LOCAL",
                  "ldap_group": false
              },
              "privileges": [],
              "members": [
                  {
                      "href": "http://host.example.com:8080/api/v1/groups/group3/members/12345678123456781234567812345678123456781234567812345678123456781234567812345678",
                      "MemberInfo": {
                          "group_name": "group3",
                          "user_name": "12345678123456781234567812345678123456781234567812345678123456781234567812345678"
                      }
                  },
                  {
                      "href": "http://host.example.com:8080/api/v1/groups/group3/members/jdgjsaas",
                      "MemberInfo": {
                          "group_name": "group3",
                          "user_name": "jdgjsaas"
                      }
                  }
              ]
          },
          {
              "href": "http://host.example.com:8080/api/v1/groups/group4",
              "Groups": {
                  "group_name": "group4",
                  "group_type": "LOCAL",
                  "ldap_group": false
              },
              "privileges": [],
              "members": []
          },
          {
              "href": "http://host.example.com:8080/api/v1/groups/group5",
              "Groups": {
                  "group_name": "group5",
                  "group_type": "LOCAL",
                  "ldap_group": false
              },
              "privileges": [],
              "members": []
          },
          {
              "href": "http://host.example.com:8080/api/v1/groups/group6",
              "Groups": {
                  "group_name": "group6",
                  "group_type": "LOCAL",
                  "ldap_group": false
              },
              "privileges": [],
              "members": []
          },
          {
              "href": "http://host.example.com:8080/api/v1/groups/group8",
              "Groups": {
                  "group_name": "group8",
                  "group_type": "LOCAL",
                  "ldap_group": false
              },
              "privileges": [],
              "members": []
          },
          {
              "href": "http://host.example.com:8080/api/v1/groups/group9",
              "Groups": {
                  "group_name": "group9",
                  "group_type": "LOCAL",
                  "ldap_group": false
              },
              "privileges": [],
              "members": []
          },
          {
              "href": "http://host.example.com:8080/api/v1/groups/hmaurya.12",
              "Groups": {
                  "group_name": "hmaurya.12",
                  "group_type": "LOCAL",
                  "ldap_group": false
              },
              "privileges": [
                  {
                      "href": "http://host.example.com:8080/api/v1/groups/hmaurya.12/privileges/754",
                      "PrivilegeInfo": {
                          "group_name": "hmaurya.12",
                          "privilege_id": 754
                      }
                  }
              ],
              "members": [
                  {
                      "href": "http://host.example.com:8080/api/v1/groups/hmaurya.12/members/admin",
                      "MemberInfo": {
                          "group_name": "hmaurya.12",
                          "user_name": "admin"
                      }
                  }
              ]
          }
      ]
  }