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
            component_value: ''
          }),
          Ember.Object.create({
            display_name: 'SecondaryNameNode',
            component_value: ''
          }),
          Ember.Object.create({
            display_name: 'DataNodes',
            component_value: ''
          })
        ]
      }),
      Ember.Object.create({
        service_name: 'HCFS',
        display_name: 'HCFS',
        service_components: [
          Ember.Object.create({
            display_name: 'HCFS Client',
            component_value: ''
          })
        ]
      }),
      Ember.Object.create({
        service_name: 'MAPREDUCE',
        display_name: 'MapReduce',
        service_components: [
          Ember.Object.create({
            display_name: 'JobTracker',
            component_value: ''
          }),
          Ember.Object.create({
            display_name: 'TaskTrackers',
            component_value: ''
          })
        ]
      }),
      Ember.Object.create({
        service_name: 'YARN',
        display_name: 'YARN + MapReduce2',
        service_components: [
          Ember.Object.create({
            display_name: 'NodeManager',
            component_value: ''
          }),
          Ember.Object.create({
            display_name: 'ResourceManager',
            component_value: ''
          }),
          Ember.Object.create({
            display_name: 'History Server',
            component_value: ''
          })
        ]
      }),
      Ember.Object.create({
        service_name: 'HIVE',
        display_name: 'Hive + HCatalog',
        service_components: [
          Ember.Object.create({
            display_name: 'Hive Metastore',
            component_value: ''
          }),
          Ember.Object.create({
            display_name: 'Database',
            component_value: ''
          })
        ]
      }),
      Ember.Object.create({
        service_name: 'HBASE',
        display_name: 'HBase',
        service_components: [
          Ember.Object.create({
            display_name: 'Master',
            component_value: ''
          }),
          Ember.Object.create({
            display_name: 'RegionServers',
            component_value: ''
          })
        ]
      }),
      Ember.Object.create({
        service_name: 'ZOOKEEPER',
        display_name: 'ZooKeeper',
        service_components: [
          Ember.Object.create({
            display_name: 'Servers',
            component_value: ''
          })
        ]
      }),
      Ember.Object.create({
        service_name: 'OOZIE',
        display_name: 'Oozie',
        service_components: [
          Ember.Object.create({
            display_name: 'Server',
            component_value: ''
          }),
          // TODO: uncomment when ready to integrate with database other than Derby
          Ember.Object.create({
             display_name: 'Database',
             component_value: ''
          })
        ]
      }),
      Ember.Object.create({
        service_name: 'NAGIOS',
        display_name: 'Nagios',
        service_components: [
          Ember.Object.create({
            display_name: 'Server',
            component_value: ''
          }),
          Ember.Object.create({
            display_name: 'Administrator',
            component_value: ''
          })
        ]
      }),
      Ember.Object.create({
        service_name: 'GANGLIA',
        display_name: 'Ganglia',
        service_components: [
          Ember.Object.create({
            display_name: 'Server',
            component_value: ''
          })
        ]
      }),
      Ember.Object.create({
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
        service_components: [
          Ember.Object.create({

          })
        ]
      }),
      Ember.Object.create({
        service_name: 'HUE',
        display_name: 'Hue',
        service_components: [
          Ember.Object.create({
            display_name: 'Server',
            component_value: ''
          })
        ]
      })

    ]
  }
];