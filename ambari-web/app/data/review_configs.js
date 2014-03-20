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

module.exports = [

  {
    config_name: 'Admin',
    display_name: 'Admin Name',
    config_value: ''
  },
  {
    config_name: 'cluster',
    display_name: 'Cluster Name',
    config_value: ''
  },
  {
    config_name: 'hosts',
    display_name: 'Total Hosts',
    config_value: ''
  },
  {
    config_name: 'Repo',
    display_name: 'Local Repository',
    config_value: ''
  },
  {
    config_name: 'services',
    display_name: 'Services',
    config_value: [
      Ember.Object.create({
        service_name: 'HDFS',
        display_name: 'HDFS',
        service_components: [
          Ember.Object.create({
            display_name: 'NameNode',
            component_name: 'NAMENODE',
            component_value: '',
            isMaster: true
          }),
          Ember.Object.create({
            display_name: 'SecondaryNameNode',
            component_name: 'SECONDARY_NAMENODE',
            component_value: '',
            isMaster: true
          }),
          Ember.Object.create({
            display_name: 'DataNodes',
            component_name: 'DATANODE',
            component_value: '',
            isMaster: false
          })
        ]
      }),
      Ember.Object.create({
        service_name: 'GLUSTERFS',
        display_name: 'GLUSTERFS',
        service_components: [
          Ember.Object.create({
            display_name: 'GLUSTERFS Client',
            component_name: 'GLUSTERFS_CLIENT',
            component_value: '',
            isMaster: false
          })
        ]
      }),
      Ember.Object.create({
        service_name: 'MAPREDUCE',
        display_name: 'MapReduce',
        service_components: [
          Ember.Object.create({
            display_name: 'JobTracker',
            component_name: 'JOBTRACKER',
            component_value: '',
            isMaster: true
          }),
          Ember.Object.create({
            display_name: 'TaskTrackers',
            component_name: 'TASKTRACKER',
            component_value: '',
            isMaster: false
          }),
          Ember.Object.create({
            display_name: 'History Server',
            component_name: 'HISTORYSERVER',
            component_value: '',
            isMaster: true
          })
        ]
      }),
      Ember.Object.create({
        service_name: 'YARN',
        display_name: 'YARN + MapReduce2',
        service_components: [
          Ember.Object.create({
            display_name: 'NodeManager',
            component_name: 'NODEMANAGER',
            component_value: '',
            isMaster: false
          }),
          Ember.Object.create({
            display_name: 'ResourceManager',
            component_name: 'RESOURCEMANAGER',
            component_value: '',
            isMaster: true
          }),
          Ember.Object.create({
            display_name: 'History Server',
            component_name: 'HISTORYSERVER',
            component_value: '',
            isMaster: true
          })
          // @todo uncomment after Application Timeline Server API implementation
//          Ember.Object.create({
//            display_name: 'App Timeline Server',
//            component_name: 'APP_TIMELINE_SERVER',
//            component_value: '',
//            isMaster: true
//          })
        ]
      }),
      Ember.Object.create({
        service_name: 'HIVE',
        display_name: 'Hive + HCatalog',
        service_components: [
          Ember.Object.create({
            display_name: 'Hive Metastore',
            component_name: 'HIVE_METASTORE',
            component_value: '',
            isMaster: true
          }),
          Ember.Object.create({
            display_name: 'Database',
            component_value: '',
            customHandler: 'loadHiveDbValue'
          })
        ]
      }),
      Ember.Object.create({
        service_name: 'HBASE',
        display_name: 'HBase',
        service_components: [
          Ember.Object.create({
            display_name: 'Master',
            component_name: 'HBASE_MASTER',
            component_value: '',
            customHandler: 'loadHbaseMasterValue'
          }),
          Ember.Object.create({
            display_name: 'RegionServers',
            component_name: 'HBASE_REGIONSERVER',
            component_value: '',
            isMaster: false
          })
        ]
      }),
      Ember.Object.create({
        service_name: 'ZOOKEEPER',
        display_name: 'ZooKeeper',
        service_components: [
          Ember.Object.create({
            display_name: 'Servers',
            component_name: 'ZOOKEEPER_SERVER',
            component_value: '',
            customHandler: 'loadZkServerValue'
          })
        ]
      }),
      Ember.Object.create({
        service_name: 'OOZIE',
        display_name: 'Oozie',
        service_components: [
          Ember.Object.create({
            display_name: 'Server',
            component_name: 'OOZIE_SERVER',
            component_value: '',
            isMaster: true
          }),
          // TODO: uncomment when ready to integrate with database other than Derby
          Ember.Object.create({
            display_name: 'Database',
            component_value: '',
            customHandler: 'loadOozieDbValue'
          })
        ]
      }),
      Ember.Object.create({
        service_name: 'NAGIOS',
        display_name: 'Nagios',
        service_components: [
          Ember.Object.create({
            display_name: 'Server',
            component_name: 'NAGIOS_SERVER',
            component_value: '',
            isMaster: true
          }),
          Ember.Object.create({
            display_name: 'Administrator',
            component_value: '',
            customHandler: 'loadNagiosAdminValue'
          })
        ]
      }),
      Ember.Object.create({
        service_name: 'GANGLIA',
        display_name: 'Ganglia',
        service_components: [
          Ember.Object.create({
            display_name: 'Server',
            component_name: 'GANGLIA_SERVER',
            component_value: '',
            isMaster: true
          })
        ]
      }),
     /* Ember.Object.create({
        service_name: 'TEZ',
        display_name: 'TEZ',
        service_components: []
      }),
      Ember.Object.create({
        service_name: 'PIG',
        display_name: 'Pig',
        service_components: []
      }),
      Ember.Object.create({
        service_name: 'SQOOP',
        display_name: 'Sqoop',
        service_components: []
      }),
      Ember.Object.create({
        service_name: 'HCATALOG',
        display_name: 'HCatalog',
        service_components: []
      }),*/
      Ember.Object.create({
        service_name: 'HUE',
        display_name: 'Hue',
        service_components: [
          Ember.Object.create({
            display_name: 'Server',
            component_name: 'HUE_SERVER',
            component_value: '',
            isMaster: true
          })
        ]
      }),
      Ember.Object.create({
        service_name: 'FALCON',
        display_name: 'Falcon',
        service_components: [
          Ember.Object.create({
            display_name: 'Server',
            component_name: 'FALCON_SERVER',
            component_value: '',
            isMaster: true
          })
        ]
      }),
      Ember.Object.create({
        service_name: 'STORM',
        display_name: 'Storm',
        service_components: [
          Ember.Object.create({
            display_name: 'Nimbus',
            component_name: 'NIMBUS',
            component_value: '',
            isMaster: true
          }),
          Ember.Object.create({
            display_name: 'Storm REST API Server',
            component_name: 'STORM_REST_API',
            component_value: '',
            isMaster: true
          }),
          Ember.Object.create({
            display_name: 'Storm UI Server',
            component_name: 'STORM_UI_SERVER',
            component_value: '',
            isMaster: true
          }),
          Ember.Object.create({
            display_name: 'DRPC Server',
            component_name: 'DRPC_SERVER',
            component_value: '',
            isMaster: true
          }),
          Ember.Object.create({
            display_name: 'Supervisor',
            component_name: 'SUPERVISOR',
            component_value: '',
            isMaster: false
          })
        ]
      }),
      Ember.Object.create({
        service_name: 'PIG',
        display_name: 'Pig',
        service_components: [
          Ember.Object.create({
            display_name: 'Clients',
            component_name: 'CLIENT',
            component_value: '',
            isMaster: false
          })
        ]
      }),
      Ember.Object.create({
        service_name: 'SQOOP',
        display_name: 'Sqoop',
        service_components: [
          Ember.Object.create({
            display_name: 'Clients',
            component_name: 'CLIENT',
            component_value: '',
            isMaster: false
          })
        ]
      }),
      Ember.Object.create({
        service_name: 'TEZ',
        display_name: 'Tez',
        service_components: [
          Ember.Object.create({
            display_name: 'Clients',
            component_name: 'CLIENT',
            component_value: '',
            isMaster: false
          })
        ]
      })
    ]
  }
];

// @todo remove after Application Timeline Server API implementation
if (App.supports.appTimelineServer) {
  var yarnServiceComponents = module.exports.findProperty('config_name', 'services').config_value.findProperty('service_name','YARN').get('service_components');
  yarnServiceComponents.push(
    Ember.Object.create({
      display_name: 'App Timeline Server',
      component_name: "APP_TIMELINE_SERVER",
      component_value: '',
      isMaster: true
    })
  )
}
