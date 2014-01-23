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
      "name": "mapred.capacity-scheduler.queue.<queue-name>.capacity",
      "displayName": "Capacity",
      "value": '',
      "defaultValue": '100',
      "description": "Percentage of the number of slots in the cluster that are made to be available for jobs in this queue. The sum of capacities for all queues should be less than or equal 100.",
      "isVisible": true,
      "isRequired": true,
      "serviceName": "MAPREDUCE",
      "category": "CapacityScheduler",
      "unit": "%",
      "isQueue": true,
      "filename": 'capacity-scheduler.xml',
      "inTable": true,
      "index": 1
    },
    {
      "id": "site property",
      "name": "mapred.capacity-scheduler.queue.<queue-name>.maximum-capacity",
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
      "serviceName": "MAPREDUCE",
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
      "name": "mapred.capacity-scheduler.queue.<queue-name>.minimum-user-limit-percent",
      "displayName": "Min User Limit",
      "value": '',
      "defaultValue": '100',
      "displayType": "int",
      "description": "Each queue enforces a limit on the percentage of resources allocated to a user at any given time, " +
        "if there is competition for them. This user limit can vary between a minimum and maximum value. " +
        "The former depends on the number of users who have submitted jobs, and the latter is set to this property value. " +
        "For example, suppose the value of this property is 25. If two users have submitted jobs to a queue, no single user " +
        "can use more than 50% of the queue resources. If a third user submits a job, no single user can use more than 33% of " +
        "the queue resources. With 4 or more users, no user can use more than 25% of the queue's resources. " +
        "A value of 100 implies no user limits are imposed.",
      "isVisible": true,
      "isRequired": true,
      "serviceName": "MAPREDUCE",
      "category": "CapacityScheduler",
      "unit": "%",
      "valueRange": [1, 100],
      "isQueue": true,
      "filename": 'capacity-scheduler.xml',
      "inTable": true,
      "index": 9
    },
    {
      "id": "site property",
      "name": "mapred.capacity-scheduler.queue.<queue-name>.user-limit-factor",
      "displayName": "User Limit Factor",
      "value": '',
      "defaultValue": '1',
      "displayType": "int",
      "description": "The multiple of the queue capacity which can be configured to allow a single user to acquire more slots. " +
        "By default this is set to 1 which ensure that a single user can never take more than the queue's configured capacity " +
        "irrespective of how idle the cluster is.",
      "isVisible": true,
      "isRequired": true,
      "serviceName": "MAPREDUCE",
      "category": "CapacityScheduler",
      "isQueue": true,
      "filename": 'capacity-scheduler.xml',
      "inTable": true,
      "index": 8
    },
    {
      "id": "site property",
      "name": "mapred.capacity-scheduler.queue.<queue-name>.supports-priority",
      "displayName": "Supports Priority",
      "value": 'false',
      "defaultValue": 'false',
      "displayType": "checkbox",
      "description": "If true, priorities of jobs will be taken into account in scheduling decisions.",
      "isVisible": true,
      "isRequired": true,
      "serviceName": "MAPREDUCE",
      "category": "CapacityScheduler",
      "isQueue": true,
      "filename": 'capacity-scheduler.xml',
      "inTable": true,
      "index": 7
    },
    {
      "id": "site property",
      "name": "mapred.queue.<queue-name>.acl-submit-job",
      "displayName": "",
      "value": '',
      "defaultValue": '*',
      "description": "",
      "isVisible": false,
      "isRequired": true,
      "serviceName": "MAPREDUCE",
      "category": "CapacityScheduler",
      "isQueue": true,
      "filename": 'mapred-queue-acls.xml'
    },
    {
      "id": "site property",
      "name": "mapred.queue.<queue-name>.acl-administer-jobs",
      "displayName": "",
      "value": '',
      "defaultValue": '*',
      "description": "",
      "isVisible": false,
      "isRequired": true,
      "serviceName": "MAPREDUCE",
      "category": "CapacityScheduler",
      "isQueue": true,
      "filename": 'mapred-queue-acls.xml'
    },
    {
      "id": "site property",
      "name": "mapred.capacity-scheduler.queue.<queue-name>.maximum-initialized-active-tasks",
      "displayName": "Max initialized active tasks",
      "value": '',
      "defaultValue": '200000',
      "displayType": "int",
      "description": "The maximum number of tasks, across all jobs in the queue, which can be initialized concurrently. " +
        "Once the queue's jobs exceed this limit they will be queued on disk.",
      "isVisible": true,
      "isRequired": true,
      "serviceName": "MAPREDUCE",
      "category": "CapacityScheduler",
      "isQueue": true,
      "filename": 'capacity-scheduler.xml',
      "index": 10
    },
    {
      "id": "site property",
      "name": "mapred.capacity-scheduler.queue.<queue-name>.maximum-initialized-active-tasks-per-user",
      "displayName": "Max initialized active tasks per user",
      "value": '',
      "defaultValue": '100000',
      "displayType": "int",
      "description": "The maximum number of tasks per-user, across all the of the user's jobs in the queue, which " +
        "can be initialized concurrently. Once the user's jobs exceed this limit they will be queued on disk.",
      "isVisible": true,
      "isRequired": true,
      "serviceName": "MAPREDUCE",
      "category": "CapacityScheduler",
      "isQueue": true,
      "filename": 'capacity-scheduler.xml',
      "index": 11
    },
    {
      "id": "site property",
      "name": "mapred.capacity-scheduler.queue.<queue-name>.init-accept-jobs-factor",
      "displayName": "Init accept jobs factor",
      "value": '',
      "defaultValue": '10',
      "displayType": "int",
      "description": "The multiple of (maximum-system-jobs * queue-capacity) used to determine the number of " +
        "jobs which are accepted by the scheduler. The default value is 10. If number of jobs submitted to " +
        "the queue exceeds this limit, job submission are rejected.",
      "isVisible": true,
      "isRequired": true,
      "serviceName": "MAPREDUCE",
      "category": "CapacityScheduler",
      "isQueue": true,
      "filename": 'capacity-scheduler.xml',
      "index": 12
    }
  ];