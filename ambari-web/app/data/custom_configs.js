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
      "id": "conf-site",
      "name": "capacity-scheduler",
      "displayName": "Custom Capacity Scheduler Configs",
      "value": "",
      "defaultValue": require('data/capacity-scheduler_default'),
      "description": "Enter in key=value format to set capacity-scheduler.xml  parameters not exposed through this page.<br> New line is the delimiter for every key-value pair.",
      "displayType": "custom",
      "isVisible": true,
      "isRequired": false,
      "serviceName": "MAPREDUCE",
      "category": "Capacity Scheduler"
    },
    {
      "id": "conf-site",
      "name": "mapred-queue-acls",
      "displayName": "Custom MapReduce Queue Configs",
      "value": "",
      "defaultValue": require('data/mapred-queue-acl_default'),
      "description": "Enter in key=value format to set mapred-queue-acls.xml parameters not exposed through this page.<br> New line is the delimiter for every key-value pair.",
      "displayType": "custom",
      "isVisible": true,
      "isRequired": false,
      "serviceName": "MAPREDUCE",
      "category": "Capacity Scheduler"
    }
  ]