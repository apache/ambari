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

var hdp22properties = require('data/HDP2.2/site_properties').configProperties;

var excludedConfigs = [
  'DB_FLAVOR',
  'db_host',
  'SQL_COMMAND_INVOKER',
  'db_name',
  'db_root_user',
  'db_root_password',
  'nimbus.host'
];

var hdp23properties = hdp22properties.filter(function (item) {
  return !excludedConfigs.contains(item.name);
});

hdp23properties.push({
    "id": "site property",
    "name": "DB_FLAVOR",
    "displayName": "DB FLAVOR",
    "value": "",
    "defaultValue": "",
    "isReconfigurable": true,
    "options": [
      {
        displayName: 'MYSQL',
        foreignKeys: ['ranger_mysql_database', 'ranger_mysql_host']
      },
      {
        displayName: 'ORACLE',
        foreignKeys: ['ranger_oracle_database', 'ranger_oracle_host']
      },
      {
        displayName: 'POSTGRES',
        foreignKeys: ['ranger_postgres_database', 'ranger_postgres_host']
      },
      {
        displayName: 'MSSQL',
        foreignKeys: ['ranger_mssql_database', 'ranger_mssql_host']
      }
    ],
    "displayType": "radio button",
    "radioName": "RANGER DB_FLAVOR",
    "isOverridable": false,
    "isVisible": true,
    "isObserved": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 0
  },
  {
    "name": "ranger_mysql_database",
    "id": "puppet var",
    "displayName": "Database Type",
    "value": "",
    "defaultValue": "MySQL",
    "description": "Using a MySQL database for Ranger",
    "displayType": "masterHost",
    "isOverridable": false,
    "isVisible": false,
    "isReconfigurable": false,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 1
  },
  {
    "name": "ranger_oracle_database",
    "id": "puppet var",
    "displayName": "Database Type",
    "value": "",
    "defaultValue": "ORACLE",
    "description": "Using an Oracle database for Ranger",
    "displayType": "masterHost",
    "isOverridable": false,
    "isVisible": false,
    "isReconfigurable": false,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 2
  },
  {
    "name": "ranger_postgres_database",
    "id": "puppet var",
    "displayName": "Database Type",
    "value": "",
    "defaultValue": "Postgres",
    "description": "Using a Postgres database for Ranger",
    "displayType": "masterHost",
    "isOverridable": false,
    "isVisible": false,
    "isReconfigurable": false,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 3
  },
  {
    "name": "ranger_mssql_database",
    "id": "puppet var",
    "displayName": "Database Type",
    "value": "",
    "defaultValue": "MSSQL",
    "description": "Using a MS SQL database for Ranger",
    "displayType": "masterHost",
    "isOverridable": false,
    "isVisible": false,
    "isReconfigurable": false,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 4
  },
  {
    "name": "rangerserver_host",
    "id": "puppet var",
    "displayName": "Ranger Server host",
    "value": "",
    "defaultValue": "",
    "description": "The host that has been assigned to run Ranger Server",
    "displayType": "masterHost",
    "isOverridable": false,
    "isVisible": true,
    "isRequiredByAgent": false,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 0
  },
  {
    "name": "db_host",
    "id": "site property",
    "displayName": "Ranger DB host",
    "defaultValue": "",
    "isObserved": true,
    "isReconfigurable": true,
    "displayType": "masterHost",
    "isOverridable": false,
    "isVisible": false,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings"
  },
  {
    "name": "ranger_mysql_host",
    "id": "puppet var",
    "displayName": "MYSQL database Host",
    "description": "Specify the host on which the existing database is hosted",
    "defaultValue": "",
    "value": "",
    "displayType": "host",
    "isOverridable": false,
    "isVisible": false,
    "isObserved": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 6
  },
  {
    "name": "ranger_oracle_host",
    "id": "puppet var",
    "displayName": "Oracle database Host",
    "description": "Specify the host on which the existing database is hosted",
    "defaultValue": "",
    "value": "",
    "displayType": "host",
    "isOverridable": false,
    "isVisible": false,
    "isObserved": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 7
  },
  {
    "name": "ranger_postgres_host",
    "id": "puppet var",
    "displayName": "Database Host",
    "description": "Specify the host on which the existing database is hosted",
    "defaultValue": "",
    "value": "",
    "displayType": "host",
    "isOverridable": false,
    "isVisible": false,
    "isObserved": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 8
  },
  {
    "name": "ranger_mssql_host",
    "id": "puppet var",
    "displayName": "Database Host",
    "description": "Specify the host on which the existing database is hosted",
    "defaultValue": "",
    "value": "",
    "displayType": "host",
    "isOverridable": false,
    "isVisible": false,
    "isObserved": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 9
  },
  {
    "name": "SQL_COMMAND_INVOKER",
    "id": "site property",
    "displayName": "SQL Command Invoker",
    "defaultValue": "mysql",
    "isObserved": true,
    "isReconfigurable": true,
    "displayType": "",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 10
  },
  {
    "name": "ranger_jdbc_connection_url",
    "id": "site property",
    "displayName": "JDBC connect string for a Ranger database",
    "defaultValue": "",
    "isReconfigurable": true,
    "displayType": "",
    "isOverridable": false,
    "isObserved": true,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 11
  },
  {
    "name": "ranger_jdbc_driver",
    "id": "site property",
    "displayName": "Driver class name for a JDBC Ranger database",
    "defaultValue": "",
    "isReconfigurable": true,
    "isObserved": true,
    "displayType": "",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 12
  },
  {
    "name": "db_root_user",
    "id": "site property",
    "displayName": "Ranger DB root user",
    "defaultValue": "",
    "isReconfigurable": true,
    "isObserved": true,
    "displayType": "",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 13
  },
  {
    "name": "db_root_password",
    "id": "site property",
    "displayName": "Ranger DB root password",
    "defaultValue": "",
    "isReconfigurable": true,
    "isObserved": true,
    "displayType": "password",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 14
  },
  {
    "name": "db_name",
    "id": "site property",
    "displayName": "Ranger DB name",
    "defaultValue": "",
    "isReconfigurable": true,
    "isObserved": true,
    "displayType": "",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 15
  },
  {
    "id": "site property",
    "name": "tez.am.view-acls",
    "displayName": "tez.am.view-acls",
    "isRequired": false,
    "serviceName": "TEZ",
    "filename": "tez-site.xml",
    "category": "Advanced tez-site"
  }
);

module.exports =
{
  "configProperties": hdp23properties
};
