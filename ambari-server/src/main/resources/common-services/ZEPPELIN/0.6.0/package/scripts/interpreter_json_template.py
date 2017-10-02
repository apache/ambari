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
  "interpreterSettings": {
    "2CKEKWY8Z": {
      "id": "2CKEKWY8Z",
      "name": "angular",
      "group": "angular",
      "properties": {},
      "status": "READY",
      "interpreterGroup": [
        {
          "name": "angular",
          "class": "org.apache.zeppelin.angular.AngularInterpreter",
          "defaultInterpreter": false,
          "editor": {
            "editOnDblClick": true
          }
        }
      ],
      "dependencies": [],
      "option": {
        "remote": true,
        "port": -1,
        "perNote": "shared",
        "perUser": "shared",
        "isExistingProcess": false,
        "setPermission": false,
        "users": [],
        "isUserImpersonate": false
      }
    },
    "2CKX8WPU1": {
      "id": "2CKX8WPU1",
      "name": "spark",
      "group": "spark",
      "properties": {
        "spark.executor.memory": "512m",
        "args": "",
        "zeppelin.spark.printREPLOutput": "true",
        "spark.cores.max": "",
        "zeppelin.dep.additionalRemoteRepository": "spark-packages,http://dl.bintray.com/spark-packages/maven,false;",
        "zeppelin.spark.sql.stacktrace": "false",
        "zeppelin.spark.importImplicit": "true",
        "zeppelin.spark.concurrentSQL": "false",
        "zeppelin.spark.useHiveContext": "true",
        "zeppelin.pyspark.python": "python",
        "zeppelin.dep.localrepo": "local-repo",
        "zeppelin.R.knitr": "true",
        "zeppelin.spark.maxResult": "1000",
        "master": "yarn-client",
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
          "defaultInterpreter": true,
          "editor": {
            "language": "scala"
          }
        },
        {
          "name": "sql",
          "class": "org.apache.zeppelin.spark.SparkSqlInterpreter",
          "defaultInterpreter": false,
          "editor": {
            "language": "sql"
          }
        },
        {
          "name": "dep",
          "class": "org.apache.zeppelin.spark.DepInterpreter",
          "defaultInterpreter": false,
          "editor": {
            "language": "scala"
          }
        },
        {
          "name": "pyspark",
          "class": "org.apache.zeppelin.spark.PySparkInterpreter",
          "defaultInterpreter": false,
          "editor": {
            "language": "python"
          }
        },
        {
          "name": "r",
          "class": "org.apache.zeppelin.spark.SparkRInterpreter",
          "defaultInterpreter": false,
          "editor": {
            "language": "r"
          }
        }
      ],
      "dependencies": [],
      "option": {
        "remote": true,
        "port": -1,
        "perNote": "shared",
        "perUser": "shared",
        "isExistingProcess": false,
        "setPermission": false,
        "users": [],
        "isUserImpersonate": false
      }
    },
    "2CK8A9MEG": {
      "id": "2CK8A9MEG",
      "name": "jdbc",
      "group": "jdbc",
      "properties": {
        "default.password": "",
        "zeppelin.jdbc.auth.type": "",
        "common.max_count": "1000",
        "zeppelin.jdbc.principal": "",
        "default.user": "gpadmin",
        "default.url": "jdbc:postgresql://localhost:5432/",
        "default.driver": "org.postgresql.Driver",
        "zeppelin.jdbc.keytab.location": "",
        "zeppelin.jdbc.concurrent.use": "true",
        "zeppelin.jdbc.concurrent.max_connection": "10"
      },
      "status": "READY",
      "interpreterGroup": [
        {
          "name": "sql",
          "class": "org.apache.zeppelin.jdbc.JDBCInterpreter",
          "defaultInterpreter": false,
          "editor": {
            "language": "sql",
            "editOnDblClick": false
          }
        }
      ],
      "dependencies": [],
      "option": {
        "remote": true,
        "port": -1,
        "perNote": "shared",
        "perUser": "shared",
        "isExistingProcess": false,
        "setPermission": false,
        "users": [],
        "isUserImpersonate": false
      }
    },
    "2CKX6DGQZ": {
      "id": "2CKX6DGQZ",
      "name": "livy",
      "group": "livy",
      "properties": {
        "zeppelin.livy.pull_status.interval.millis": "1000",
        "livy.spark.executor.memory": "",
        "zeppelin.livy.session.create_timeout": "120",
        "zeppelin.livy.principal": "",
        "zeppelin.livy.spark.sql.maxResult": "1000",
        "zeppelin.livy.keytab": "",
        "zeppelin.livy.concurrentSQL": "false",
        "zeppelin.livy.spark.sql.field.truncate": "true",
        "livy.spark.executor.cores": "",
        "zeppelin.livy.displayAppInfo": "false",
        "zeppelin.livy.url": "http://localhost:8998",
        "livy.spark.dynamicAllocation.minExecutors": "",
        "livy.spark.driver.cores": "",
        "livy.spark.jars.packages": "",
        "livy.spark.dynamicAllocation.enabled": "",
        "livy.spark.executor.instances": "",
        "livy.spark.dynamicAllocation.cachedExecutorIdleTimeout": "",
        "livy.spark.dynamicAllocation.maxExecutors": "",
        "livy.spark.dynamicAllocation.initialExecutors": "",
        "livy.spark.driver.memory": ""
      },
      "status": "READY",
      "interpreterGroup": [
        {
          "name": "spark",
          "class": "org.apache.zeppelin.livy.LivySparkInterpreter",
          "defaultInterpreter": true,
          "editor": {
            "language": "scala",
            "editOnDblClick": false
          }
        },
        {
          "name": "sql",
          "class": "org.apache.zeppelin.livy.LivySparkSQLInterpreter",
          "defaultInterpreter": false,
          "editor": {
            "language": "sql",
            "editOnDblClick": false
          }
        },
        {
          "name": "pyspark",
          "class": "org.apache.zeppelin.livy.LivyPySparkInterpreter",
          "defaultInterpreter": false,
          "editor": {
            "language": "python",
            "editOnDblClick": false
          }
        },
        {
          "name": "pyspark3",
          "class": "org.apache.zeppelin.livy.LivyPySpark3Interpreter",
          "defaultInterpreter": false,
          "editor": {
            "language": "python",
            "editOnDblClick": false
          }
        },
        {
          "name": "sparkr",
          "class": "org.apache.zeppelin.livy.LivySparkRInterpreter",
          "defaultInterpreter": false,
          "editor": {
            "language": "r",
            "editOnDblClick": false
          }
        }
      ],
      "dependencies": [],
      "option": {
        "remote": true,
        "port": -1,
        "perNote": "shared",
        "perUser": "scoped",
        "isExistingProcess": false,
        "setPermission": false,
        "users": [],
        "isUserImpersonate": false
      }
    },
    "2CKAY1A8Y": {
      "id": "2CKAY1A8Y",
      "name": "md",
      "group": "md",
      "properties": {
        "markdown.parser.type": "pegdown"
      },
      "status": "READY",
      "interpreterGroup": [
        {
          "name": "md",
          "class": "org.apache.zeppelin.markdown.Markdown",
          "defaultInterpreter": false,
          "editor": {
            "language": "markdown",
            "editOnDblClick": true
          }
        }
      ],
      "dependencies": [],
      "option": {
        "remote": true,
        "port": -1,
        "perNote": "shared",
        "perUser": "shared",
        "isExistingProcess": false,
        "setPermission": false,
        "users": [],
        "isUserImpersonate": false
      }
    },
    "2CHS8UYQQ": {
      "id": "2CHS8UYQQ",
      "name": "sh",
      "group": "sh",
      "properties": {
        "zeppelin.shell.keytab.location": "",
        "shell.command.timeout.millisecs": "60000",
        "zeppelin.shell.principal": "",
        "zeppelin.shell.auth.type": ""
      },
      "status": "READY",
      "interpreterGroup": [
        {
          "name": "sh",
          "class": "org.apache.zeppelin.shell.ShellInterpreter",
          "defaultInterpreter": false,
          "editor": {
            "language": "sh",
            "editOnDblClick": false
          }
        }
      ],
      "dependencies": [],
      "option": {
        "remote": true,
        "port": -1,
        "perNote": "shared",
        "perUser": "shared",
        "isExistingProcess": false,
        "setPermission": false,
        "users": [],
        "isUserImpersonate": false
      }
    }
  },
  "interpreterBindings": {},
  "interpreterRepositories": [
    {
      "id": "central",
      "type": "default",
      "url": "http://repo1.maven.org/maven2/",
      "releasePolicy": {
        "enabled": true,
        "updatePolicy": "daily",
        "checksumPolicy": "warn"
      },
      "snapshotPolicy": {
        "enabled": true,
        "updatePolicy": "daily",
        "checksumPolicy": "warn"
      },
      "mirroredRepositories": [],
      "repositoryManager": false
    },
    {
      "id": "local",
      "type": "default",
      "url": "file:///home/zeppelin/.m2/repository",
      "releasePolicy": {
        "enabled": true,
        "updatePolicy": "daily",
        "checksumPolicy": "warn"
      },
      "snapshotPolicy": {
        "enabled": true,
        "updatePolicy": "daily",
        "checksumPolicy": "warn"
      },
      "mirroredRepositories": [],
      "repositoryManager": false
    }
  ]
}
'''
