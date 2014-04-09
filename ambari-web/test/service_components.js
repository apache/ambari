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

module.exports = {
  "items" : [
    {
      "StackServices" : {
        "comments" : "Apache Hadoop Distributed Processing Framework",
        "service_name" : "MAPREDUCE",
        "service_version" : "1.2.0.1.3.3.0",
        "stack_name" : "HDP",
        "stack_version" : "1.3.2"
      },
      "serviceComponents" : [
        {
          "StackServiceComponents" : {
            "component_category" : "MASTER",
            "component_name" : "HISTORYSERVER",
            "is_client" : false,
            "is_master" : true,
            "service_name" : "MAPREDUCE",
            "stack_name" : "HDP",
            "stack_version" : "1.3.2"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "MASTER",
            "component_name" : "JOBTRACKER",
            "is_client" : false,
            "is_master" : true,
            "service_name" : "MAPREDUCE",
            "stack_name" : "HDP",
            "stack_version" : "1.3.2"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "CLIENT",
            "component_name" : "MAPREDUCE_CLIENT",
            "is_client" : true,
            "is_master" : false,
            "service_name" : "MAPREDUCE",
            "stack_name" : "HDP",
            "stack_version" : "1.3.2"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "SLAVE",
            "component_name" : "TASKTRACKER",
            "is_client" : false,
            "is_master" : false,
            "service_name" : "MAPREDUCE",
            "stack_name" : "HDP",
            "stack_version" : "1.3.2"
          }
        }
      ]
    },
    {
      "StackServices" : {
        "comments" : "Data management and processing platform",
        "service_name" : "FALCON",
        "service_version" : "0.4.0.2.1.1",
        "stack_name" : "HDP",
        "stack_version" : "2.1"
      },
      "serviceComponents" : [
        {
          "StackServiceComponents" : {
            "component_category" : "CLIENT",
            "component_name" : "FALCON_CLIENT",
            "is_client" : true,
            "is_master" : false,
            "service_name" : "FALCON",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "MASTER",
            "component_name" : "FALCON_SERVER",
            "is_client" : false,
            "is_master" : true,
            "service_name" : "FALCON",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        }
      ]
    },
    {
      "StackServices" : {
        "comments" : "Ganglia Metrics Collection system",
        "service_name" : "GANGLIA",
        "service_version" : "3.5.0",
        "stack_name" : "HDP",
        "stack_version" : "2.1"
      },
      "serviceComponents" : [
        {
          "StackServiceComponents" : {
            "component_category" : "SLAVE",
            "component_name" : "GANGLIA_MONITOR",
            "is_client" : false,
            "is_master" : false,
            "service_name" : "GANGLIA",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "MASTER",
            "component_name" : "GANGLIA_SERVER",
            "is_client" : false,
            "is_master" : true,
            "service_name" : "GANGLIA",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        }
      ]
    },
    {
      "StackServices" : {
        "comments" : "Non-relational distributed database and centralized service for configuration management &\n        synchronization\n      ",
        "service_name" : "HBASE",
        "service_version" : "0.96.0.2.1",
        "stack_name" : "HDP",
        "stack_version" : "2.1"
      },
      "serviceComponents" : [
        {
          "StackServiceComponents" : {
            "component_category" : "CLIENT",
            "component_name" : "HBASE_CLIENT",
            "is_client" : true,
            "is_master" : false,
            "service_name" : "HBASE",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "MASTER",
            "component_name" : "HBASE_MASTER",
            "is_client" : false,
            "is_master" : true,
            "service_name" : "HBASE",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "SLAVE",
            "component_name" : "HBASE_REGIONSERVER",
            "is_client" : false,
            "is_master" : false,
            "service_name" : "HBASE",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        }
      ]
    },
    {
      "StackServices" : {
        "comments" : "This is comment for HCATALOG service",
        "service_name" : "HCATALOG",
        "service_version" : "0.12.0.2.1",
        "stack_name" : "HDP",
        "stack_version" : "2.1"
      },
      "serviceComponents" : [
        {
          "StackServiceComponents" : {
            "component_category" : "CLIENT",
            "component_name" : "HCAT",
            "is_client" : true,
            "is_master" : false,
            "service_name" : "HCATALOG",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        }
      ]
    },
    {
      "StackServices" : {
        "comments" : "Apache Hadoop Distributed File System",
        "service_name" : "HDFS",
        "service_version" : "2.1.0.2.1",
        "stack_name" : "HDP",
        "stack_version" : "2.1"
      },
      "serviceComponents" : [
        {
          "StackServiceComponents" : {
            "component_category" : "SLAVE",
            "component_name" : "DATANODE",
            "is_client" : false,
            "is_master" : false,
            "service_name" : "HDFS",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "CLIENT",
            "component_name" : "HDFS_CLIENT",
            "is_client" : true,
            "is_master" : false,
            "service_name" : "HDFS",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "MASTER",
            "component_name" : "JOURNALNODE",
            "is_client" : false,
            "is_master" : true,
            "service_name" : "HDFS",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "MASTER",
            "component_name" : "NAMENODE",
            "is_client" : false,
            "is_master" : true,
            "service_name" : "HDFS",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "MASTER",
            "component_name" : "SECONDARY_NAMENODE",
            "is_client" : false,
            "is_master" : true,
            "service_name" : "HDFS",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "SLAVE",
            "component_name" : "ZKFC",
            "is_client" : false,
            "is_master" : false,
            "service_name" : "HDFS",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        }
      ]
    },
    {
      "StackServices" : {
        "comments" : "Data warehouse system for ad-hoc queries & analysis of large datasets and table & storage management service",
        "service_name" : "HIVE",
        "service_version" : "0.12.0.2.1",
        "stack_name" : "HDP",
        "stack_version" : "2.1"
      },
      "serviceComponents" : [
        {
          "StackServiceComponents" : {
            "component_category" : "CLIENT",
            "component_name" : "HIVE_CLIENT",
            "is_client" : true,
            "is_master" : false,
            "service_name" : "HIVE",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "MASTER",
            "component_name" : "HIVE_METASTORE",
            "is_client" : false,
            "is_master" : true,
            "service_name" : "HIVE",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "MASTER",
            "component_name" : "HIVE_SERVER",
            "is_client" : false,
            "is_master" : true,
            "service_name" : "HIVE",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "MASTER",
            "component_name" : "MYSQL_SERVER",
            "is_client" : false,
            "is_master" : true,
            "service_name" : "HIVE",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        }
      ]
    },
    {
      "StackServices" : {
        "comments" : "Apache Hadoop NextGen MapReduce (YARN)",
        "service_name" : "MAPREDUCE2",
        "service_version" : "2.1.0.2.0.6.0",
        "stack_name" : "HDP",
        "stack_version" : "2.1"
      },
      "serviceComponents" : [
        {
          "StackServiceComponents" : {
            "component_category" : "MASTER",
            "component_name" : "HISTORYSERVER",
            "is_client" : false,
            "is_master" : true,
            "service_name" : "MAPREDUCE2",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "CLIENT",
            "component_name" : "MAPREDUCE2_CLIENT",
            "is_client" : true,
            "is_master" : false,
            "service_name" : "MAPREDUCE2",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        }
      ]
    },
    {
      "StackServices" : {
        "comments" : "Nagios Monitoring and Alerting system",
        "service_name" : "NAGIOS",
        "service_version" : "3.5.0",
        "stack_name" : "HDP",
        "stack_version" : "2.1"
      },
      "serviceComponents" : [
        {
          "StackServiceComponents" : {
            "component_category" : "MASTER",
            "component_name" : "NAGIOS_SERVER",
            "is_client" : false,
            "is_master" : true,
            "service_name" : "NAGIOS",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        }
      ]
    },
    {
      "StackServices" : {
        "comments" : "System for workflow coordination and execution of Apache Hadoop jobs.  This also includes the installation of the optional Oozie Web Console which relies on and will install the <a target=\"_blank\" href=\"http://www.sencha.com/products/extjs/license/\">ExtJS</a> Library.\n      ",
        "service_name" : "OOZIE",
        "service_version" : "4.0.0.2.1",
        "stack_name" : "HDP",
        "stack_version" : "2.1"
      },
      "serviceComponents" : [
        {
          "StackServiceComponents" : {
            "component_category" : "CLIENT",
            "component_name" : "OOZIE_CLIENT",
            "is_client" : true,
            "is_master" : false,
            "service_name" : "OOZIE",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "MASTER",
            "component_name" : "OOZIE_SERVER",
            "is_client" : false,
            "is_master" : true,
            "service_name" : "OOZIE",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        }
      ]
    },
    {
      "StackServices" : {
        "comments" : "Scripting platform for analyzing large datasets",
        "service_name" : "PIG",
        "service_version" : "0.12.0.2.1",
        "stack_name" : "HDP",
        "stack_version" : "2.1"
      },
      "serviceComponents" : [
        {
          "StackServiceComponents" : {
            "component_category" : "CLIENT",
            "component_name" : "PIG",
            "is_client" : true,
            "is_master" : false,
            "service_name" : "PIG",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        }
      ]
    },
    {
      "StackServices" : {
        "comments" : "Tool for transferring bulk data between Apache Hadoop and\n        structured data stores such as relational databases\n      ",
        "service_name" : "SQOOP",
        "service_version" : "1.4.4.2.1",
        "stack_name" : "HDP",
        "stack_version" : "2.1"
      },
      "serviceComponents" : [
        {
          "StackServiceComponents" : {
            "component_category" : "CLIENT",
            "component_name" : "SQOOP",
            "is_client" : true,
            "is_master" : false,
            "service_name" : "SQOOP",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        }
      ]
    },
    {
      "StackServices" : {
        "comments" : "Apache Hadoop Stream processing framework",
        "service_name" : "STORM",
        "service_version" : "0.9.0.1",
        "stack_name" : "HDP",
        "stack_version" : "2.1"
      },
      "serviceComponents" : [
        {
          "StackServiceComponents" : {
            "component_category" : "MASTER",
            "component_name" : "DRPC_SERVER",
            "is_client" : false,
            "is_master" : true,
            "service_name" : "STORM",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "MASTER",
            "component_name" : "LOGVIEWER_SERVER",
            "is_client" : false,
            "is_master" : true,
            "service_name" : "STORM",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "MASTER",
            "component_name" : "NIMBUS",
            "is_client" : false,
            "is_master" : true,
            "service_name" : "STORM",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "MASTER",
            "component_name" : "STORM_REST_API",
            "is_client" : false,
            "is_master" : true,
            "service_name" : "STORM",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "MASTER",
            "component_name" : "STORM_UI_SERVER",
            "is_client" : false,
            "is_master" : true,
            "service_name" : "STORM",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "SLAVE",
            "component_name" : "SUPERVISOR",
            "is_client" : false,
            "is_master" : false,
            "service_name" : "STORM",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        }
      ]
    },
    {
      "StackServices" : {
        "comments" : "Tez is the next generation Hadoop Query Processing framework written on top of YARN.",
        "service_name" : "TEZ",
        "service_version" : "0.4.0.2.1",
        "stack_name" : "HDP",
        "stack_version" : "2.1"
      },
      "serviceComponents" : [
        {
          "StackServiceComponents" : {
            "component_category" : "CLIENT",
            "component_name" : "TEZ_CLIENT",
            "is_client" : true,
            "is_master" : false,
            "service_name" : "TEZ",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        }
      ]
    },
    {
      "StackServices" : {
        "comments" : "This is comment for WEBHCAT service",
        "service_name" : "WEBHCAT",
        "service_version" : "0.12.0.2.1",
        "stack_name" : "HDP",
        "stack_version" : "2.1"
      },
      "serviceComponents" : [
        {
          "StackServiceComponents" : {
            "component_category" : "MASTER",
            "component_name" : "WEBHCAT_SERVER",
            "is_client" : false,
            "is_master" : true,
            "service_name" : "WEBHCAT",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        }
      ]
    },
    {
      "StackServices" : {
        "comments" : "Apache Hadoop NextGen MapReduce (YARN)",
        "service_name" : "YARN",
        "service_version" : "2.1.0.2.1",
        "stack_name" : "HDP",
        "stack_version" : "2.1"
      },
      "serviceComponents" : [
        {
          "StackServiceComponents" : {
            "component_category" : "MASTER",
            "component_name" : "APP_TIMELINE_SERVER",
            "is_client" : false,
            "is_master" : true,
            "service_name" : "YARN",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "SLAVE",
            "component_name" : "NODEMANAGER",
            "is_client" : false,
            "is_master" : false,
            "service_name" : "YARN",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "MASTER",
            "component_name" : "RESOURCEMANAGER",
            "is_client" : false,
            "is_master" : true,
            "service_name" : "YARN",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "CLIENT",
            "component_name" : "YARN_CLIENT",
            "is_client" : true,
            "is_master" : false,
            "service_name" : "YARN",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        }
      ]
    },
    {
      "StackServices" : {
        "comments" : "Centralized service which provides highly reliable distributed\n        coordination.",
        "service_name" : "ZOOKEEPER",
        "service_version" : "3.4.5.2.1",
        "stack_name" : "HDP",
        "stack_version" : "2.1"
      },
      "serviceComponents" : [
        {
          "StackServiceComponents" : {
            "component_category" : "CLIENT",
            "component_name" : "ZOOKEEPER_CLIENT",
            "is_client" : true,
            "is_master" : false,
            "service_name" : "ZOOKEEPER",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        },
        {
          "StackServiceComponents" : {
            "component_category" : "MASTER",
            "component_name" : "ZOOKEEPER_SERVER",
            "is_client" : false,
            "is_master" : true,
            "service_name" : "ZOOKEEPER",
            "stack_name" : "HDP",
            "stack_version" : "2.1"
          }
        }
      ]
    }
  ]
};