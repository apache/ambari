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

var App = require('app');
require('mappers/server_data_mapper');
require('mappers/stack_mapper');
require('models/stack');
require('models/operating_system');
require('models/repository');
require('models/stack_version/service_simple');

describe('App.stackMapper', function () {
	describe("#map", function() {
    var testData = {
      "href": "http://172.22.91.73:8080/api/v1/mpacks/1",
      "MpackInfo": {
          "id": 1,
          "modules": [
              {
                  "id": "zookeeper_clients",
                  "displayName": "ZooKeeper Clients",
                  "description": "Clients for ZooKeeper",
                  "category": "CLIENT",
                  "name": "ZOOKEEPER_CLIENTS",
                  "version": "3.4.0.0-b34",
                  "definition": "zookeeper_clients-3.4.0.0-b34-definition.tar.gz",
                  "dependencies": [
                      {
                          "id": "zookeeper",
                          "name": "ZOOKEEPER",
                          "dependencyType": "RUNTIME"
                      }
                  ],
                  "components": [
                      {
                          "id": "zookeeper_client",
                          "name": "ZOOKEEPER_CLIENT",
                          "category": "CLIENT",
                          "isExternal": false,
                          "version": "3.4.0.0-b34"
                      }
                  ]
              },
              {
                  "id": "mapreduce2",
                  "displayName": "MAPREDUCE2",
                  "description": "Apache Hadoop Mapreduce2",
                  "category": "SERVER",
                  "name": "MAPREDUCE2",
                  "version": "3.0.0.0-b95",
                  "definition": "mapreduce2-3.0.0.0-b95-definition.tar.gz",
                  "dependencies": [
                      {
                          "id": "hadoop_clients",
                          "name": "HADOOP_CLIENTS",
                          "dependencyType": "INSTALL"
                      },
                      {
                          "id": "yarn",
                          "name": "YARN",
                          "dependencyType": "RUNTIME"
                      }
                  ],
                  "components": [
                      {
                          "id": "historyserver",
                          "name": "HISTORYSERVER",
                          "category": "MASTER",
                          "isExternal": false,
                          "version": "3.0.0.0-b95"
                      }
                  ]
              },
              {
                  "id": "hdfs",
                  "displayName": "HDFS",
                  "description": "Apache Hadoop Distributed File System",
                  "category": "SERVER",
                  "name": "HDFS",
                  "version": "3.0.0.0-b95",
                  "definition": "hdfs-3.0.0.0-b95-definition.tar.gz",
                  "dependencies": [
                      {
                          "id": "hadoop_clients",
                          "name": "HADOOP_CLIENTS",
                          "dependencyType": "INSTALL"
                      }
                  ],
                  "components": [
                      {
                          "id": "namenode",
                          "name": "NAMENODE",
                          "category": "MASTER",
                          "isExternal": false,
                          "version": "3.0.0.0-b95"
                      },
                      {
                          "id": "datanode",
                          "name": "DATANODE",
                          "category": "SLAVE",
                          "isExternal": false,
                          "version": "3.0.0.0-b95"
                      },
                      {
                          "id": "journalnode",
                          "name": "JOURNALNODE",
                          "category": "SLAVE",
                          "isExternal": false,
                          "version": "3.0.0.0-b95"
                      },
                      {
                          "id": "secondary_namenode",
                          "name": "SECONDARY_NAMENODE",
                          "category": "MASTER",
                          "isExternal": false,
                          "version": "3.0.0.0-b95"
                      },
                      {
                          "id": "zkfc",
                          "name": "ZKFC",
                          "category": "SLAVE",
                          "isExternal": false,
                          "version": "3.0.0.0-b95"
                      },
                      {
                          "id": "nfs_gateway",
                          "name": "NFS_GATEWAY",
                          "category": "SLAVE",
                          "isExternal": false,
                          "version": "3.0.0.0-b95"
                      }
                  ]
              },
              {
                  "id": "hadoop_clients",
                  "displayName": "Hadoop client",
                  "description": "Clients for HDFS, YARN and MAPREDUCE services",
                  "category": "CLIENT",
                  "name": "HADOOP_CLIENTS",
                  "version": "3.0.0.0-b95",
                  "definition": "hadoop_clients-3.0.0.0-b95-definition.tar.gz",
                  "dependencies": [
                      {
                          "id": "hdfs",
                          "name": "HDFS",
                          "dependencyType": "RUNTIME"
                      },
                      {
                          "id": "yarn",
                          "name": "YARN",
                          "dependencyType": "RUNTIME"
                      },
                      {
                          "id": "mapreduce2",
                          "name": "MAPREDUCE2",
                          "dependencyType": "RUNTIME"
                      }
                  ],
                  "components": [
                      {
                          "id": "hadoop_client",
                          "name": "HADOOP_CLIENT",
                          "category": "CLIENT",
                          "isExternal": false,
                          "version": "3.0.0.0-b95"
                      }
                  ]
              },
              {
                  "id": "zookeeper",
                  "displayName": "ZooKeeper",
                  "description": "Centralized service which provides highly reliable distributed coordination",
                  "category": "SERVER",
                  "name": "ZOOKEEPER",
                  "version": "3.4.0.0-b34",
                  "definition": "zookeeper-3.4.0.0-b34-definition.tar.gz",
                  "dependencies": [
                      {
                          "id": "zookeeper_clients",
                          "name": "ZOOKEEPER_CLIENTS",
                          "dependencyType": "INSTALL"
                      }
                  ],
                  "components": [
                      {
                          "id": "zookeeper_server",
                          "name": "ZOOKEEPER_SERVER",
                          "category": "MASTER",
                          "isExternal": false,
                          "version": "3.4.0.0-b34"
                      }
                  ]
              },
              {
                  "id": "yarn",
                  "displayName": "YARN",
                  "description": "Apache Hadoop Yarn",
                  "category": "SERVER",
                  "name": "YARN",
                  "version": "3.0.0.0-b95",
                  "definition": "yarn-3.0.0.0-b95-definition.tar.gz",
                  "dependencies": [
                      {
                          "id": "hadoop_clients",
                          "name": "HADOOP_CLIENTS",
                          "dependencyType": "INSTALL"
                      },
                      {
                          "id": "hdfs",
                          "name": "HDFS",
                          "dependencyType": "RUNTIME"
                      },
                      {
                          "id": "mapreduce2",
                          "name": "MAPREDUCE2",
                          "dependencyType": "RUNTIME"
                      },
                      {
                          "id": "zookeeper",
                          "name": "ZOOKEEPER",
                          "dependencyType": "RUNTIME"
                      }
                  ],
                  "components": [
                      {
                          "id": "resourcemanager",
                          "name": "RESOURCEMANAGER",
                          "category": "MASTER",
                          "isExternal": false,
                          "version": "3.0.0.0-b95"
                      },
                      {
                          "id": "nodemanager",
                          "name": "NODEMANAGER",
                          "category": "SLAVE",
                          "isExternal": false,
                          "version": "3.0.0.0-b95"
                      },
                      {
                          "id": "app_timeline_server",
                          "name": "APP_TIMELINE_SERVER",
                          "category": "MASTER",
                          "isExternal": false,
                          "version": "3.0.0.0-b95"
                      },
                      {
                          "id": "timeline_reader",
                          "name": "TIMELINE_READER",
                          "category": "MASTER",
                          "isExternal": false,
                          "version": "3.0.0.0-b95"
                      },
                      {
                          "id": "yarn_registry_dns",
                          "name": "YARN_REGISTRY_DNS",
                          "category": "MASTER",
                          "isExternal": false,
                          "version": "3.0.0.0-b95"
                      }
                  ]
              }
          ],
          "mpack_description": "Hortonworks Data Platform Core",
          "mpack_display_name": null,
          "mpack_id": "hdpcore",
          "mpack_name": "HDPCORE",
          "mpack_uri": "http://dev.hortonworks.com.s3.amazonaws.com/HDPCORE/centos7/1.x/BUILDS/1.0.0-b201/mpack.json",
          "mpack_version": "1.0.0-b201",
          "registry_id": null
      },
      "operating_systems": [
          {
              "href": "http://172.22.91.73:8080/api/v1/mpacks/1/operating_systems/redhat7",
              "OperatingSystems": {
                  "is_ambari_managed": true,
                  "mpack_id": 1,
                  "os_type": "redhat7",
                  "repositories": [
                      {
                          "distribution": null,
                          "components": null,
                          "unique": true,
                          "tags": [],
                          "base_url": "http://dev.hortonworks.com.s3.amazonaws.com/HDPCORE/centos7/1.x/BUILDS/1.0.0-b201",
                          "os_type": "redhat7",
                          "repo_id": "HDPCORE-1.0.0-b201",
                          "repo_name": "HDPCORE",
                          "mirrors_list": null,
                          "default_base_url": null,
                          "ambari_managed": true
                      },
                      {
                          "distribution": null,
                          "components": null,
                          "unique": false,
                          "tags": [],
                          "base_url": "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos7",
                          "os_type": "redhat7",
                          "repo_id": "HDP-UTILS-1.1.0.21",
                          "repo_name": "HDP-UTILS",
                          "mirrors_list": null,
                          "default_base_url": null,
                          "ambari_managed": true
                      }
                  ]
              }
          },
          {
              "href": "http://172.22.91.73:8080/api/v1/mpacks/1/operating_systems/ubuntu12",
              "OperatingSystems": {
                  "is_ambari_managed": true,
                  "mpack_id": 1,
                  "os_type": "ubuntu12",
                  "repositories": [
                      {
                          "distribution": null,
                          "components": null,
                          "unique": true,
                          "tags": [],
                          "base_url": "http://dev.hortonworks.com.s3.amazonaws.com/HDPCORE/ubuntu12/1.x/BUILDS/1.0.0-b201",
                          "os_type": "ubuntu12",
                          "repo_id": "HDPCORE-1.0.0-b201",
                          "repo_name": "HDPCORE",
                          "mirrors_list": null,
                          "default_base_url": null,
                          "ambari_managed": true
                      },
                      {
                          "distribution": null,
                          "components": null,
                          "unique": false,
                          "tags": [],
                          "base_url": "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/ubuntu12",
                          "os_type": "ubuntu12",
                          "repo_id": "HDP-UTILS-1.1.0.21",
                          "repo_name": "HDP-UTILS",
                          "mirrors_list": null,
                          "default_base_url": null,
                          "ambari_managed": true
                      }
                  ]
              }
          }
      ],
      "version": [
          {
              "href": "http://172.22.91.73:8080/api/v1/stacks/HDPCORE/versions/1.0.0-b201",
              "Versions": {
                  "id": 1,
                  "stack_name": "HDPCORE",
                  "stack_version": "1.0.0-b201"
              }
          }
      ],
      "default_operating_systems": [
          {
              "href": "http://172.22.91.73:8080/api/v1/mpacks/1/default_operating_systems/redhat7",
              "OperatingSystems": {
                  "mpack_id": 1,
                  "os_type": "redhat7",
                  "repositories": [
                      {
                          "distribution": null,
                          "components": null,
                          "unique": true,
                          "tags": [],
                          "base_url": "http://dev.hortonworks.com.s3.amazonaws.com/HDPCORE/centos7/1.x/BUILDS/1.0.0-b201",
                          "mirrors_list": null,
                          "repo_id": "HDPCORE-1.0.0-b201",
                          "repo_name": "HDPCORE"
                      },
                      {
                          "distribution": null,
                          "components": null,
                          "unique": false,
                          "tags": [],
                          "base_url": "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos7",
                          "mirrors_list": null,
                          "repo_id": "HDP-UTILS-1.1.0.21",
                          "repo_name": "HDP-UTILS"
                      }
                  ]
              }
          },
          {
              "href": "http://172.22.91.73:8080/api/v1/mpacks/1/default_operating_systems/ubuntu12",
              "OperatingSystems": {
                  "mpack_id": 1,
                  "os_type": "ubuntu12",
                  "repositories": [
                      {
                          "distribution": null,
                          "components": null,
                          "unique": true,
                          "tags": [],
                          "base_url": "http://dev.hortonworks.com.s3.amazonaws.com/HDPCORE/ubuntu12/1.x/BUILDS/1.0.0-b201",
                          "mirrors_list": null,
                          "repo_id": "HDPCORE-1.0.0-b201",
                          "repo_name": "HDPCORE"
                      },
                      {
                          "distribution": null,
                          "components": null,
                          "unique": false,
                          "tags": [],
                          "base_url": "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/ubuntu12",
                          "mirrors_list": null,
                          "repo_id": "HDP-UTILS-1.1.0.21",
                          "repo_name": "HDP-UTILS"
                      }
                  ]
              }
          }
      ]
  };
    beforeEach(function () {
      App.resetDsStoreTypeMap(App.Repository);
      App.resetDsStoreTypeMap(App.OperatingSystem);
      App.resetDsStoreTypeMap(App.Stack);
      App.resetDsStoreTypeMap(App.ServiceSimple);
      sinon.stub(App.store, 'commit', Em.K);
      App.stackMapper.map(testData);
    });
    afterEach(function(){
      App.store.commit.restore();
    });

    it ('should map all Stack data', function() {
      expect(App.Stack.find().get('length')).to.equal(1);
    });

    it ('all stacks are showAvailable', function() {
      expect(App.Stack.find().everyProperty('showAvailable')).to.equal(true);
    });

    it ('no one stack is selected', function() {
      expect(App.Stack.find().everyProperty('isSelected')).to.equal(false);
    });

    it ('2 OSes are mapped', function() {
      expect(App.OperatingSystem.find().get('length')).to.equal(2);
    });

    it ('OSes have valid ids', function() {
      expect(App.OperatingSystem.find().mapProperty('id')).to.eql(
        [
          'HDPCORE-1.0.0-b201-redhat7',
          'HDPCORE-1.0.0-b201-ubuntu12'
        ]);
    });

    it ('4 repositories are mapped', function() {
      expect(App.Repository.find().get('length')).to.equal(4);
    });

    it ('Repositories ids are valid', function() {
      expect(App.Repository.find().mapProperty('id')).to.eql(
        [
          'HDPCORE-1.0.0-b201-redhat7-HDPCORE-1.0.0-b201',
          'HDPCORE-1.0.0-b201-redhat7-HDP-UTILS-1.1.0.21',
          'HDPCORE-1.0.0-b201-ubuntu12-HDPCORE-1.0.0-b201',
          'HDPCORE-1.0.0-b201-ubuntu12-HDP-UTILS-1.1.0.21'
        ]);
    });
  });
});
