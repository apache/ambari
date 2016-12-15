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
  "id": "2C4U48MY3_spark2",
  "name": "spark2",
  "group": "spark",
  "properties": {
    "spark.executor.memory": "",
    "args": "",
    "zeppelin.spark.printREPLOutput": "true",
    "spark.cores.max": "",
    "zeppelin.dep.additionalRemoteRepository": "spark-packages,http://dl.bintray.com/spark-packages/maven,false;",
    "zeppelin.spark.importImplicit": "true",
    "zeppelin.spark.sql.stacktrace": "false",
    "zeppelin.spark.concurrentSQL": "false",
    "zeppelin.spark.useHiveContext": "true",
    "zeppelin.pyspark.python": "python",
    "zeppelin.dep.localrepo": "local-repo",
    "zeppelin.R.knitr": "true",
    "zeppelin.spark.maxResult": "1000",
    "master": "local[*]",
    "spark.app.name": "Zeppelin",
    "zeppelin.R.image.width": "100%",
    "zeppelin.R.render.options": "out.format \u003d \u0027html\u0027, comment \u003d NA, echo \u003d FALSE, results \u003d \u0027asis\u0027, message \u003d F, warning \u003d F",
    "zeppelin.R.cmd": "R"
  },
  "status": "READY",
  "interpreterGroup": [
    {
      "name": "spark",
      "class": "org.apache.zeppelin.spark.SparkInterpreter",
      "defaultInterpreter": true
    },
    {
      "name": "sql",
      "class": "org.apache.zeppelin.spark.SparkSqlInterpreter",
      "defaultInterpreter": false
    },
    {
      "name": "dep",
      "class": "org.apache.zeppelin.spark.DepInterpreter",
      "defaultInterpreter": false
    },
    {
      "name": "pyspark",
      "class": "org.apache.zeppelin.spark.PySparkInterpreter",
      "defaultInterpreter": false
    },
    {
      "name": "r",
      "class": "org.apache.zeppelin.spark.SparkRInterpreter",
      "defaultInterpreter": false
    }
  ],
  "dependencies": [],
  "option": {
    "remote": true,
    "port": -1,
    "perNoteSession": false,
    "perNoteProcess": false,
    "isExistingProcess": false,
    "setPermission": false
  }
}
'''