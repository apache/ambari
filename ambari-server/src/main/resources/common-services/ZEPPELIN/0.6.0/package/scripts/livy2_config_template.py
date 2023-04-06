#!/usr/bin/env python
"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

"""

template = '''
{
  "id": "2C8A4SZ9T_livy2",
  "status": "READY",
  "group": "livy",
  "name": "livy2",
  "properties": {
    "zeppelin.livy.keytab": "",
    "zeppelin.livy.spark.sql.maxResult": "1000",
    "livy.spark.executor.instances": "",
    "livy.spark.executor.memory": "",
    "livy.spark.dynamicAllocation.enabled": "",
    "livy.spark.dynamicAllocation.cachedExecutorIdleTimeout": "",
    "livy.spark.dynamicAllocation.initialExecutors": "",
    "zeppelin.livy.session.create_timeout": "120",
    "livy.spark.driver.memory": "",
    "zeppelin.livy.displayAppInfo": "false",
    "livy.spark.jars.packages": "",
    "livy.spark.dynamicAllocation.maxExecutors": "",
    "zeppelin.livy.concurrentSQL": "false",
    "zeppelin.livy.principal": "",
    "livy.spark.executor.cores": "",
    "zeppelin.livy.url": "http://localhost:8998",
    "zeppelin.livy.pull_status.interval.millis": "1000",
    "livy.spark.driver.cores": "",
    "livy.spark.dynamicAllocation.minExecutors": ""
  },
  "interpreterGroup": [
    {
      "class": "org.apache.zeppelin.livy.LivySparkInterpreter",
      "editor": {
        "editOnDblClick": false,
        "language": "scala"
      },
      "name": "spark",
      "defaultInterpreter": false
    },
    {
      "class": "org.apache.zeppelin.livy.LivySparkSQLInterpreter",
      "editor": {
        "editOnDblClick": false,
        "language": "sql"
      },
      "name": "sql",
      "defaultInterpreter": false
    },
    {
      "class": "org.apache.zeppelin.livy.LivyPySparkInterpreter",
      "editor": {
        "editOnDblClick": false,
        "language": "python"
      },
      "name": "pyspark",
      "defaultInterpreter": false
              },
    {
      "class": "org.apache.zeppelin.livy.LivyPySpark3Interpreter",
      "editor": {
        "editOnDblClick": false,
        "language": "python"
      },
      "name": "pyspark3",
      "defaultInterpreter": false
    },
    {
      "class": "org.apache.zeppelin.livy.LivySparkRInterpreter",
      "editor": {
        "editOnDblClick": false,
        "language": "r"
      },
      "name": "sparkr",
      "defaultInterpreter": false
    }
  ],
  "dependencies": [],
  "option": {
    "setPermission": false,
    "remote": true,
    "users": [],
    "isExistingProcess": false,
    "perUser": "scoped",
    "isUserImpersonate": false,
    "perNote": "shared",
    "port": -1
  }
}
'''
