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
  [
    {
      "id": "site property",
      "name": "yarn.scheduler.capacity.root.<queue-name>.capacity",
      "displayName": "Capacity",
      "value": '',
      "defaultValue": '',
      "description": "Percentage of the number of slots in the cluster that are made to be available for jobs in this queue. The sum of capacities for all queues should be less than or equal 100.",
      "isVisible": true,
      "isRequired": true,
      "serviceName": "",
      "category": "CapacityScheduler",
      "unit": "%",
      "isQueue": true,
      "filename": 'capacity-scheduler.xml',
      "inTable": true,
      "index": 1
    },
    {
      "id": "site property",
      "name": "yarn.scheduler.capacity.root.<queue-name>.maximum-capacity",
      "displayName": "Max Capacity",
      "value": '',
      "defaultValue": '100',
      "displayType": "int",
      "description": "Defines a limit beyond which a queue cannot use the capacity of the cluster." +
        "This provides a means to limit how much excess capacity a queue can use. By default, there is no limit." +
        "The Max Capacity of a queue can only be greater than or equal to its minimum capacity. " +
        "This property could be to curtail certain jobs which are long running in nature from occupying more than a certain " +
        "percentage of the cluster, which in the absence of pre-emption, could lead to capacity guarantees of other queues being affected. " +
        "One important thing to note is that maximum-capacity is a percentage , so based on the cluster's capacity it would change. " +
        "So if large no of nodes or racks get added to the cluster, Max Capacity in absolute terms would increase accordingly. ",
      "isVisible": true,
      "isRequired": true,
      "serviceName": "YARN",
      "category": "CapacityScheduler",
      "unit": "%",
      "valueRange": [0, 100],
      "isQueue": true,
      "filename": 'capacity-scheduler.xml',
      "inTable": true,
      "index": 2
    },
    {
      "id": "site property",
      "name": "yarn.scheduler.capacity.root.<queue-name>.user-limit-factor",
      "displayName": "User Limit Factor",
      "value": '',
      "defaultValue": '1',
      "displayType": "int",
      "description": "The multiple of the queue capacity which can be configured to allow a single user to acquire more slots. " +
        "By default this is set to 1 which ensure that a single user can never take more than the queue's configured capacity " +
        "irrespective of how idle the cluster is.",
      "isVisible": true,
      "isRequired": true,
      "serviceName": "YARN",
      "category": "CapacityScheduler",
      "isQueue": true,
      "filename": 'capacity-scheduler.xml',
      "inTable": true,
      "index": 7
    },
    {
      "id": "site property",
      "name": "yarn.scheduler.capacity.root.<queue-name>.state",
      "displayName": "State",
      "value": '',
      "defaultValue": 'RUNNING',
      "description": "state",
      "isVisible": true,
      "isRequired": true,
      "serviceName": "YARN",
      "category": "CapacityScheduler",
      "isQueue": true,
      "filename": 'capacity-scheduler.xml',
      "inTable": true,
      "index": 8
    },
    {
      "id": "site property",
      "name": "yarn.scheduler.capacity.root.<queue-name>.acl_submit_jobs",
      "displayName": "",
      "value": '',
      "defaultValue": '*',
      "description": "",
      "isVisible": false,
      "isRequired": true,
      "serviceName": "YARN",
      "category": "CapacityScheduler",
      "isQueue": true,
      "filename": 'capacity-scheduler.xml'
    },
    {
      "id": "site property",
      "name": "yarn.scheduler.capacity.root.<queue-name>.acl_administer_jobs",
      "displayName": "",
      "value": '',
      "defaultValue": '*',
      "description": "",
      "isVisible": false,
      "isRequired": true,
      "serviceName": "YARN",
      "category": "CapacityScheduler",
      "isQueue": true,
      "filename": 'capacity-scheduler.xml'
    }
  ];
