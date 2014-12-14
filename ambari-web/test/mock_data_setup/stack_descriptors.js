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

module.exports = {
  "href": "http://162.216.148.139:8080/api/v1/stacks/HDP/versions/2.2?fields=Versions/kerberos_descriptor",
  "Versions": {
    "stack_name": "HDP",
    "stack_version": "2.2",
    "kerberos_descriptor": {
      "properties": {
        "realm": "${cluster-env/kerberos_domain}",
        "keytab_dir": "/etc/security/keytabs"
      },
      "identities": [
        {
          "name": "spnego",
          "principal": {
            "value": "HTTP/_HOST@${realm}"
          },
          "keytab": {
            "file": "${keytab_dir}/spnego.service.keytab",
            "owner": {
              "name": "root",
              "access": "r"
            },
            "group": {
              "name": "${cluster-env/user_group}",
              "access": "r"
            }
          }
        }
      ],
      "configurations": [
        {
          "core-site": {
            "hadoop.security.authentication": "kerberos",
            "hadoop.rpc.protection": "authentication; integrity; privacy",
            "hadoop.security.authorization": "true"
          }
        }
      ],
      "services": [
        {
          "name": "HDFS",
          "components": [
            {
              "name": "NAMENODE",
              "identities": [
                {
                  "name": "namenode_nn",
                  "principal": {
                    "value": "nn/_HOST@${realm}",
                    "configuration": "hdfs-site/dfs.namenode.kerberos.principal"
                  },
                  "keytab": {
                    "file": "${keytab_dir}/nn.service.keytab",
                    "owner": {
                      "name": "${hadoop-env/hdfs_user}",
                      "access": "r"
                    },
                    "group": {
                      "name": "${cluster-env/user_group}",
                      "access": ""
                    },
                    "configuration": "hdfs-site/dfs.namenode.keytab.file"
                  }
                },
                {
                  "name": "namenode_host",
                  "principal": {
                    "value": "host/_HOST@${realm}",
                    "configuration": "hdfs-site/dfs.namenode.kerberos.https.principal"
                  },
                  "keytab": {
                    "file": "${keytab_dir}/host.keytab",
                    "owner": {
                      "name": "${hadoop-env/hdfs_user}",
                      "access": "r"
                    },
                    "group": {
                      "name": "${cluster-env/user_group}",
                      "access": ""
                    },
                    "configuration": "hdfs-site/dfs.namenode.keytab.file"
                  }
                },
                {
                  "name": "/spnego",
                  "principal": {
                    "configuration": "hdfs-site/dfs.web.authentication.kerberos.principal"
                  },
                  "keytab": {
                    "configuration": "hdfs/dfs.web.authentication.kerberos.keytab"
                  }
                }
              ]
            },
            {
              "name": "DATANODE",
              "identities": [
                {
                  "name": "datanode_dn",
                  "principal": {
                    "value": "dn/_HOST@${realm}",
                    "configuration": "hdfs-site/dfs.namenode.kerberos.principal"
                  },
                  "keytab": {
                    "file": "${keytab_dir}/dn.service.keytab",
                    "owner": {
                      "name": "${hadoop-env/hdfs_user}",
                      "access": "r"
                    },
                    "group": {
                      "name": "${cluster-env/user_group}",
                      "access": ""
                    },
                    "configuration": "hdfs-site/dfs.namenode.keytab.file"
                  }
                },
                {
                  "name": "datanode_host",
                  "principal": {
                    "value": "host/_HOST@${realm}",
                    "configuration": "hdfs-site/dfs.datanode.kerberos.https.principal"
                  },
                  "keytab": {
                    "file": "${keytab_dir}/host.keytab.file",
                    "owner": {
                      "name": "${hadoop-env/hdfs_user}",
                      "access": "r"
                    },
                    "group": {
                      "name": "${cluster-env/user_group}",
                      "access": ""
                    },
                    "configuration": "hdfs-site/dfs.namenode.secondary.keytab.file"
                  }
                }
              ]
            },
            {
              "name": "SECONDARY_NAMENODE",
              "identities": [
                {
                  "name": "secondary_namenode_nn",
                  "principal": {
                    "value": "nn/_HOST@${realm}",
                    "configuration": "hdfs-site/dfs.namenode.secondary.kerberos.principal"
                  },
                  "keytab": {
                    "file": "${keytab_dir}/snn.service.keytab",
                    "owner": {
                      "name": "${hadoop-env/hdfs_user}",
                      "access": "r"
                    },
                    "group": {
                      "name": "${cluster-env/user_group}",
                      "access": ""
                    },
                    "configuration": "hdfs-site/dfs.namenode.secondary.keytab.file"
                  }
                },
                {
                  "name": "secondary_namenode_host",
                  "principal": {
                    "value": "host/_HOST@${realm}",
                    "configuration": "hdfs-site/dfs.namenode.secondary.kerberos.https.principal"
                  },
                  "keytab": {
                    "file": "${keytab_dir}/host.keytab.file",
                    "owner": {
                      "name": "${hadoop-env/hdfs_user}",
                      "access": "r"
                    },
                    "group": {
                      "name": "${cluster-env/user_group}",
                      "access": ""
                    },
                    "configuration": "hdfs-site/dfs.namenode.secondary.keytab.file"
                  }
                },
                {
                  "name": "/spnego",
                  "principal": {
                    "configuration": "hdfs-site/dfs.web.authentication.kerberos.principal"
                  },
                  "keytab": {
                    "configuration": "hdfs/dfs.web.authentication.kerberos.keytab"
                  }
                }
              ]
            }
          ]
        }
      ]
    }
  }
};
