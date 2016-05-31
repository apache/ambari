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

import applicationAdapter from 'hive/adapters/database';

export default function() {
  var baseUrl = applicationAdapter.create().buildURL();
  var databases = ['db1', 'db2', 'db3'];

  this.get(baseUrl + '/resources/ddl/database', function (req) {
    var db = {
      databases: databases
    };

    return [200, {"Content-Type": "application/json"}, JSON.stringify(db)];
  });

  this.get(baseUrl + '/resources/ddl/database/db1/table.page', function (req) {
    var tables = {
      rows: [
        ['table1'],
        ['table2'],
        ['table3']
      ]
    };

    return [200, {"Content-Type": "application/json"}, JSON.stringify(tables)];
  });

  this.get(baseUrl + '/resources/ddl/database/db1/table', function (req) {
    var tables = {
      tables: [
        ['table1'],
        ['table2'],
        ['table3']
      ],
      database: 'db1'
    };

    return [200, {"Content-Type": "application/json"}, JSON.stringify(tables)];
  });

  this.get(baseUrl + '/resources/ddl/database/db1/table/table1.page', function (req) {
    var columns = {
      rows: [
        ['column1', 'STRING'],
        ['column2', 'STRING'],
        ['column3', 'STRING']
      ]
    };

    return [200, {"Content-Type": "application/json"}, JSON.stringify(columns)];
  });

  this.get(baseUrl + '/udfs', function (req) {
    var udf = {
      "udfs": [{
        "name": "TestColumn",
        "classname": "TestClassName",
        "fileResource": 1,
        "id": 1,
        "owner": "owner1"
      },
      {
        "name": "Test2Columns",
        "classname": "Test2ClassName",
        "fileResource": 1,
        "id": 2,
        "owner": "owner2"
      }]
    };

    return [200, {"Content-Type": "application/json"}, JSON.stringify(udf)];
  });

  this.post(baseUrl + '/jobs', function (req) {
    var job = {
      "job": {
        "status":"Finished",
        "dataBase":"db1",
        "dateSubmitted":1421677418,
        "logFile":"job1/logs",
        "properties":{},
        "fileResources":[],
        "statusDir":"job1",
        "id":1,
        "title":"Worksheet",
        "duration":2,
        "forcedContent":"",
        "owner":"admin",
        "confFile":"job1/conf",
        "queryId":null,
        "queryFile":"job1.hql"
      }
    };

    return [200, {"Content-Type": "application/json"}, JSON.stringify(job)];
  });

  this.get(baseUrl + '/resources/file/job1.hql', function (req) {
    var file = {
      "file": {
        "filePath": "job1.hql",
        "fileContent": "select * from big",
        "hasNext": false,
        "page": 0,
        "pageCount": 1
      }
    };

    return [200, {"Content-Type": "application/json"}, JSON.stringify(file)];
  });

  this.get(baseUrl + '/savedQueries', function(req) {
    var savedQueries = {
      "savedQueries": [{
        "queryFile": "saved1.hql",
        "dataBase": "db1",
        "title": "saved1",
        "shortQuery": "",
        "id": 1,
        "owner": "owner1"
      }, {
        "queryFile": "saved2.hql",
        "dataBase": "db2",
        "title": "saved2",
        "shortQuery": "select count(field_0) from big;",
        "id": 2,
        "owner": "owner2"
      }]
    };

    return [200, {"Content-Type": "application/json"}, JSON.stringify(savedQueries)];
  });

  this.get(baseUrl + '/savedQueries/defaultSettings', function (req) {
    var defaultSettings = {
      "defaultSettings" : []
    };

    return [200, {"Content-Type": "application/json"}, JSON.stringify(defaultSettings)];
  });

  this.get(baseUrl + '/resources/file/saved1.hql', function (req) {
    var file = {
      "file": {
        "filePath": "saved1.hql",
        "fileContent": "select * from saved1",
        "hasNext": false,
        "page": 0,
        "pageCount": 0
      }
    };

    return [200, {"Content-Type": "application/json"}, JSON.stringify(file)];
  });

  this.get(baseUrl + '/jobs', function (req) {
    var jobs = {
      "jobs": [
        {
          "title": "Query1",
          "queryFile": "saved1.hql",
          "statusDir": "statusdir",
          "dateSubmitted": 1421240048,
          "duration": 97199,
          "status": "Finished",
          "forcedContent": "",
          "id": 1,
          "owner": "admin",
          "logFile": "logs1",
          "confFile": "conf1"
        },
        {
          "title": "Query2",
          "queryFile": "saved1.hql",
          "statusDir": "statusdir",
          "dateSubmitted": 1421240048,
          "duration": 97199,
          "status": "Finished",
          "forcedContent": "",
          "id": 2,
          "owner": "admin",
          "logFile": "logs2",
          "confFile": "conf2"
        },
        {
          "title": "Query3",
          "queryFile": "saved1.hql",
          "statusDir": "statusdir",
          "dateSubmitted": 1421240048,
          "duration": 97199,
          "status": "Running",
          "forcedContent": "",
          "id": 3,
          "owner": "admin",
          "logFile": "logs3",
          "confFile": "conf3"
        },
        {
          "title": "Query4",
          "queryFile": "saved1.hql",
          "statusDir": "statusdir",
          "dateSubmitted": 1421240048,
          "duration": 97199,
          "status": "Error",
          "forcedContent": "",
          "id": 4,
          "owner": "admin",
          "logFile": "logs4",
          "confFile": "con51"
        }
      ]
    };

    return [200, {"Content-Type": "application/json"}, JSON.stringify(jobs)];
  });

  this.get(baseUrl + '/fileResources', function (req) {
    var files = {
      "fileResources": [
        {
          "name": "TestName",
          "path": "TestPath",
          "id": 1,
          "owner": "owner1"
        }
      ]
    };

    return [200, {"Content-Type": "application/json"}, JSON.stringify(files)];
  });

  this.get(baseUrl + '/fileResources/1', function (req) {
    var files = {
      "fileResources": [
        {
          "name": "TestName",
          "path": "TestPath",
          "id": 1,
          "owner": "owner1"
        }
      ]
    };

    return [200, {"Content-Type": "application/json"}, JSON.stringify(files)];
  });

  this.get(baseUrl + '/api/v1/views/TEZ', function (req) {
    var data = {
      versions: [
        {
          href: baseUrl + '/api/v1/view/TEZ/versions/1',
          ViewVersionInfo: {version: '1', view_name: 'TEZ'}
        }
      ]
    };

    return [200, {"Content-Type": "application/json"}, JSON.stringify(data)];
  });

  this.get(baseUrl + '/api/v1/views/TEZ/versions/1', function (req) {
    var data = {
      instances: [
        {
          ViewInstanceInfo: {
            instance_name: 'tez',
            version: 1
          }
        }
      ]
    };

    return [200, {"Content-Type": "application/json"}, JSON.stringify(data)];
  });
}
