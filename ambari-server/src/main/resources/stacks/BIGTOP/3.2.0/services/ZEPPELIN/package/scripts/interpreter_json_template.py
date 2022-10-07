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
    "angular": {
      "id": "angular",
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
    "spark2": {
      "id": "spark2",
      "name": "spark2",
      "group": "spark",
      "properties": {
        "spark.executor.memory": {
          "type": "string", 
          "name": "spark.executor.memory", 
          "value": ""
        },
        "args": {
          "type": "string", 
          "name": "args", 
          "value": ""
        },
		"zeppelin.spark.printREPLOutput": {
          "type": "string", 
          "name": "zeppelin.spark.printREPLOutput", 
          "value": "true"
        },
        "spark.cores.max": {
          "type": "string", 
          "name": "spark.cores.max", 
          "value": ""
        },
        "zeppelin.dep.additionalRemoteRepository": {
          "type": "string", 
          "name": "zeppelin.dep.additionalRemoteRepository", 
          "value": "spark-packages,http://dl.bintray.com/spark-packages/maven,false;"
        },
        "zeppelin.spark.importImplicit": {
          "type": "string", 
          "name": "zeppelin.spark.importImplicit", 
          "value": "true"
        },
        "zeppelin.spark.sql.stacktrace": {
          "type": "string", 
          "name": "zeppelin.spark.sql.stacktrace", 
          "value": "false"
        },
        "zeppelin.spark.concurrentSQL": {
          "type": "string", 
          "name": "zeppelin.spark.concurrentSQL", 
          "value": "false"
        },
        "zeppelin.spark.useHiveContext": {
          "type": "string", 
          "name": "zeppelin.spark.useHiveContext", 
          "value": "true"
        },
        "zeppelin.pyspark.python": {
          "type": "string", 
          "name": "zeppelin.pyspark.python", 
          "value": "python"
        },
        "zeppelin.dep.localrepo": {
          "type": "string", 
          "name": "zeppelin.dep.localrepo", 
          "value": "local-repo"
        },
        "zeppelin.R.knitr": {
          "type": "string", 
          "name": "zeppelin.R.knitr", 
          "value": "true"
        },        
        "zeppelin.spark.maxResult": {
          "type": "string", 
          "name": "zeppelin.spark.maxResult", 
          "value": "1000"
        },
        "master": {
          "type": "string", 
          "name": "master", 
          "value": "local[*]"
        },
        "spark.app.name": {
          "type": "string", 
          "name": "spark.app.name", 
          "value": "Zeppelin"
        },
        "zeppelin.R.image.width": {
          "type": "string", 
          "name": "zeppelin.R.image.width", 
          "value": "100%"
        },
        "zeppelin.R.render.options": {
          "type": "string", 
          "name": "zeppelin.R.render.options", 
          "value": "out.format \u003d \u0027html\u0027, comment \u003d NA, echo \u003d FALSE, results \u003d \u0027asis\u0027, message \u003d F, warning \u003d F"
        },
        "zeppelin.R.cmd": {
          "type": "string", 
          "name": "zeppelin.R.cmd", 
          "value": "R"
        }        
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
    },
    "jdbc": {
      "id": "jdbc",
      "name": "jdbc",
      "group": "jdbc",
      "properties": {
	    "default.password": {
          "type": "string", 
          "name": "default.password", 
          "value": ""
        },
        "zeppelin.jdbc.auth.type": {
          "type": "string", 
          "name": "zeppelin.jdbc.auth.type", 
          "value": ""
        },
        "common.max_count": {
          "type": "string", 
          "name": "common.max_count", 
          "value": "1000"
        },
		"zeppelin.jdbc.principal": {
          "type": "string", 
          "name": "zeppelin.jdbc.principal", 
          "value": ""
        },
        "default.user": {
          "type": "string", 
          "name": "default.user", 
          "value": "gpadmin"
        },
        "default.url": {
          "type": "string", 
          "name": "default.url", 
          "value": "jdbc:postgresql://localhost:5432/"
        },
        "default.driver": {
          "type": "string", 
          "name": "default.driver", 
          "value": "org.postgresql.Driver"
        },
        "zeppelin.jdbc.keytab.location": {
          "type": "string", 
          "name": "zeppelin.jdbc.keytab.location", 
          "value": ""
        },
        "zeppelin.jdbc.concurrent.use": {
          "type": "string", 
          "name": "zeppelin.jdbc.concurrent.use", 
          "value": "true"
        },
        "zeppelin.jdbc.concurrent.max_connection": {
          "type": "string", 
          "name": "zeppelin.jdbc.concurrent.max_connection", 
          "value": "10"
        }        
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
    "livy2": {
      "id": "livy2",
      "status": "READY",
      "group": "livy",
      "name": "livy2",
      "properties": {
	    "zeppelin.livy.keytab": {
          "type": "string", 
          "name": "zeppelin.livy.keytab", 
          "value": ""
        },
        "zeppelin.livy.spark.sql.maxResult": {
          "type": "string", 
          "name": "zeppelin.livy.spark.sql.maxResult", 
          "value": "1000"
        },
        "livy.spark.executor.instances": {
          "type": "string", 
          "name": "livy.spark.executor.instances", 
          "value": ""
        },
        "livy.spark.executor.memory": {
          "type": "string", 
          "name": "livy.spark.executor.memory", 
          "value": ""
        },
        "livy.spark.dynamicAllocation.enabled": {
          "type": "string", 
          "name": "livy.spark.dynamicAllocation.enabled", 
          "value": ""
        },
        "livy.spark.dynamicAllocation.cachedExecutorIdleTimeout": {
          "type": "string", 
          "name": "livy.spark.dynamicAllocation.cachedExecutorIdleTimeout", 
          "value": ""
        },
        "livy.spark.dynamicAllocation.initialExecutors": {
          "type": "string", 
          "name": "livy.spark.dynamicAllocation.initialExecutors", 
          "value": ""
        },
        "zeppelin.livy.session.create_timeout": {
          "type": "string", 
          "name": "zeppelin.livy.session.create_timeout", 
          "value": "120"
        },
        "livy.spark.driver.memory": {
          "type": "string", 
          "name": "livy.spark.driver.memory", 
          "value": ""
        },
        "zeppelin.livy.displayAppInfo": {
          "type": "string", 
          "name": "zeppelin.livy.displayAppInfo", 
          "value": "true"
        },
        "livy.spark.jars.packages": {
          "type": "string", 
          "name": "livy.spark.jars.packages", 
          "value": ""
        },
        "livy.spark.dynamicAllocation.maxExecutors": {
          "type": "string", 
          "name": "livy.spark.dynamicAllocation.maxExecutors", 
          "value": ""
        },
        "zeppelin.livy.concurrentSQL": {
          "type": "string", 
          "name": "zeppelin.livy.concurrentSQL", 
          "value": "false"
        },
        "zeppelin.livy.principal": {
          "type": "string", 
          "name": "zeppelin.livy.principal", 
          "value": ""
        },
        "livy.spark.executor.cores": {
          "type": "string", 
          "name": "livy.spark.executor.cores", 
          "value": ""
        },
        "zeppelin.livy.url": {
          "type": "string", 
          "name": "zeppelin.livy.url", 
          "value": "http://localhost:8998"
        },
        "zeppelin.livy.pull_status.interval.millis": {
          "type": "string", 
          "name": "zeppelin.livy.pull_status.interval.millis", 
          "value": "1000"
        },
        "livy.spark.driver.cores": {
          "type": "string", 
          "name": "livy.spark.driver.cores", 
          "value": ""
        },
        "livy.spark.dynamicAllocation.minExecutors": {
          "type": "string", 
          "name": "livy.spark.dynamicAllocation.minExecutors", 
          "value": ""
        }
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
          "class": "org.apache.zeppelin.livy.LivySparkRInterpreter",
          "editor": {
            "editOnDblClick": false,
            "language": "r"
          },
          "name": "sparkr",
          "defaultInterpreter": false
        },
        {
          "name": "shared",
          "class": "org.apache.zeppelin.livy.LivySharedInterpreter",
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
    },
    "md": {
      "id": "md",
      "name": "md",
      "group": "md",
      "properties": {
	    "markdown.parser.type": {
          "type": "string", 
          "name": "markdown.parser.type", 
          "value": "markdown4j"
        }        
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