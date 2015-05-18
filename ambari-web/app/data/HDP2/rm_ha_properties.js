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

module.exports =
{
  "haConfig": {
    serviceName: 'MISC',
    displayName: 'MISC',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'YARN', displayName: 'YARN'})
    ],
    sites: ['yarn-site'],
    configs: [
    /**********************************************HDFS***************************************/
      {
        "id": "site property",
        "name": "yarn.resourcemanager.ha.enabled",
        "displayName": "yarn.resourcemanager.ha.enabled",
        "isReconfigurable": false,
        "recommendedValue": true,
        "value": true,
        "displayType": "checkbox",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },
      {
        "id": "site property",
        "name": "yarn.resourcemanager.ha.rm-ids",
        "displayName": "yarn.resourcemanager.ha.rm-ids",
        "isReconfigurable": false,
        "recommendedValue": "rm1,rm2",
        "value": "rm1,rm2",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },
      {
        "id": "site property",
        "name": "yarn.resourcemanager.hostname.rm1",
        "displayName": "yarn.resourcemanager.hostname.rm1",
        "isReconfigurable": false,
        "recommendedValue": "",
        "value": "",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },

      {
        "id": "site property",
        "name": "yarn.resourcemanager.webapp.address.rm1",
        "displayName": "yarn.resourcemanager.webapp.address.rm1",
        "isReconfigurable": false,
        "recommendedValue": "",
        "value": "",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },
      {
        "id": "site property",
        "name": "yarn.resourcemanager.webapp.address.rm2",
        "displayName": "yarn.resourcemanager.webapp.address.rm2",
        "isReconfigurable": false,
        "recommendedValue": "",
        "value": "",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },
      {
        "id": "site property",
        "name": "yarn.resourcemanager.webapp.https.address.rm1",
        "displayName": "yarn.resourcemanager.webapp.https.address.rm1",
        "isReconfigurable": false,
        "recommendedValue": "",
        "value": "",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },
      {
        "id": "site property",
        "name": "yarn.resourcemanager.webapp.https.address.rm2",
        "displayName": "yarn.resourcemanager.webapp.https.address.rm2",
        "isReconfigurable": false,
        "recommendedValue": "",
        "value": "",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },

      {
        "id": "site property",
        "name": "yarn.resourcemanager.hostname.rm2",
        "displayName": "yarn.resourcemanager.hostname.rm2",
        "isReconfigurable": false,
        "recommendedValue": "",
        "value": "",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },
      {
        "id": "site property",
        "name": "yarn.resourcemanager.recovery.enabled",
        "displayName": "yarn.resourcemanager.recovery.enabled",
        "isReconfigurable": false,
        "recommendedValue": true,
        "value": true,
        "displayType": "checkbox",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },
      {
        "id": "site property",
        "name": "yarn.resourcemanager.store.class",
        "displayName": "yarn.resourcemanager.store.class",
        "isReconfigurable": false,
        "recommendedValue": "org.apache.hadoop.yarn.server.resourcemanager.recovery.ZKRMStateStore",
        "value": "org.apache.hadoop.yarn.server.resourcemanager.recovery.ZKRMStateStore",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },
      {
        "id": "site property",
        "name": "yarn.resourcemanager.zk-address",
        "displayName": "yarn.resourcemanager.zk-address",
        "isReconfigurable": false,
        "recommendedValue": "",
        "value": "",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },
      {
        "id": "site property",
        "name": "yarn.resourcemanager.cluster-id",
        "displayName": "yarn.resourcemanager.cluster-id",
        "isReconfigurable": false,
        "recommendedValue": "yarn-cluster",
        "value": "yarn-cluster",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },
      {
        "id": "site property",
        "name": "yarn.resourcemanager.ha.automatic-failover.zk-base-path",
        "displayName": "yarn.resourcemanager.ha.automatic-failover.zk-base-path",
        "isReconfigurable": false,
        "recommendedValue": "/yarn-leader-election",
        "value": "/yarn-leader-election",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      }
    ]
  }
};
