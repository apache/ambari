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
      "name": "hdfs-site",
      "displayName": "Custom HDFS Configs",
      "value": "",
      "defaultValue": "",
      "description": "Enter in \"key=value\" format to set hdfs-site.xml parameters not exposed through this page.<br> New line is the delimiter for every key-value pair.",
      "displayType": "custom",
      "isVisible": true,
      "isRequired": false,
      "serviceName": "HDFS",
      "category": "Advanced"
    },
    {
      "id": "conf-site",
      "name": "mapred-site",
      "displayName": "Custom MapReduce Configs",
      "value": "",
      "defaultValue": "",
      "description": "Enter in \"key=value\" format to set mapred-site.xml parameters not exposed through this page.<br> New line is the delimiter for every key-value pair.",
      "displayType": "custom",
      "isVisible": true,
      "isRequired": false,
      "serviceName": "MAPREDUCE",
      "category": "Advanced"
    },
    {
      "id": "conf-site",
      "name": "hbase-site",
      "displayName": "Custom HBase Configs",
      "description": "Enter in \"key=value\" format to set hbase-site.xml parameters not exposed through this page.<br> New line is the delimiter for every key-value pair.",
      "defaultValue": "",
      "isRequired": false,
      "displayType": "custom",
      "isVisible": true,
      "serviceName": "HBASE",
      "category": "Advanced"
    },
    {
      "id": "conf-site",
      "name": "hive-site",
      "displayName": "Custom Hive Configs",
      "description": "Enter in \"key=value\" format to set hive-site.xml parameters not exposed through this page.<br> New line is the delimiter for every key-value pair.",
      "defaultValue": "",
      "isRequired": false,
      "displayType": "custom",
      "isVisible": true,
      "serviceName": "HIVE",
      "category": "Advanced"
    },
    {
      "id": "conf-site",
      "name": "webhcat-site",
      "displayName": "Custom WebHCat Configs",
      "description": "Enter in \"key=value\" format to set webhcat-site.xml parameters not exposed through this page.<br> New line is the delimiter for every key-value pair.",
      "defaultValue": "",
      "isRequired": false,
      "displayType": "custom",
      "isVisible": true,
      "serviceName": "WEBHCAT",
      "category": "Advanced"
    },
    {
      "id": "conf-site",
      "name": "oozie-site",
      "displayName": "Custom Oozie Configs",
      "description": "Enter in \"key=value\" format to set oozie-site.xml parameters not exposed through this page.<br> New line is the delimiter for every key-value pair.",
      "defaultValue": "",
      "isRequired": false,
      "displayType": "custom",
      "isVisible": true,
      "serviceName": "OOZIE",
      "category": "Advanced"
    }
  ]