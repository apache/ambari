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

module.exports = new Ember.Set([

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
      {
        service_name: 'HDFS',
        service_components:  [
          {
            display_name: 'NameNode',
            component_value: ''
          },
          {
            display_name: 'SecondaryNameNode',
            component_value: ''
          },
          {
            display_name: 'DataNodes',
            component_value: ''
          }
        ]
      },
      {
        service_name: 'MapReduce',
        service_components:  [
          {
            display_name: 'JobTracker',
            component_value: ''
          },
          {
            display_name: 'TaskTrackers',
            component_value: ''
          }
        ]
      },
      {
        service_name: 'Hive + HCatalog',
        service_components:  [
          {
            display_name: 'Hive Metastore Server',
            component_value: ''
          },
          {
            display_name: 'Database',
            component_value: ''
          }
        ]
      },
      {
        service_name: 'HBase',
        service_components:  [
          {
            display_name: 'Master',
            component_value: ''
          },
          {
            display_name: 'Region Servers',
            component_value: ''
          }
        ]
      },
      {
        service_name: 'ZooKeeper',
        service_components:  [
          {
            display_name: 'Servers',
            component_value: ''
          }
        ]
      },
      {
        service_name: 'Oozie',
        service_components:  [
          {
            display_name: 'Server',
            component_value: ''
          }
        ]
      },
      {
        service_name: 'Nagios',
        service_components:  [
          {
            display_name: 'Server',
            component_value: ''
          },
          {
            display_name: 'Administrator',
            component_value: ''
          }
        ]
      },
      {
        service_name: 'Ganglia',
        service_components:  [
          {
            display_name: 'Server',
            component_value: ''
          }
        ]
      }

    ]
  }
]);