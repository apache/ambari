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


export const componentsWithManualCommands = [
  "NAMENODE",
  "SECONDARY_NAMENODE",
  "OOZIE_SERVER",
  "MYSQL_SERVER",
  "APP_TIMELINE_SERVER",
];
export const componentsWithCheckDBStep = [
  "HIVE_METASTORE",
  "HIVE_SERVER",
  "OOZIE_SERVER",
];
export const componentsWithoutSecurityConfigs = ["MYSQL_SERVER"];
export const componentsToStopAllServices = ["NAMENODE", "SECONDARY_NAMENODE"];
export const serviceToConfigSiteMap:any = {
  NAMENODE: ["hdfs-site", "core-site"],
  SECONDARY_NAMENODE: ["hdfs-site", "core-site"],
  JOBTRACKER: ["mapred-site"],
  RESOURCEMANAGER: ["yarn-site"],
  WEBHCAT_SERVER: ["hive-env", "webhcat-site", "core-site"],
  APP_TIMELINE_SERVER: ["yarn-site", "yarn-env"],
  OOZIE_SERVER: ["oozie-site", "core-site", "oozie-env"],
  HIVE_SERVER: ["hive-site", "webhcat-site", "hive-env", "core-site"],
  HIVE_METASTORE: [
    "hive-site",
    "webhcat-site",
    "hive-env",
    "core-site",
    "hive-interactive-site",
  ],
  MYSQL_SERVER: ["hive-site"],
  HISTORYSERVER: ["mapred-site"],
  TIMELINE_READER: ["yarn-site"],
};

export const relatedServicesMap:any = {
  RESOURCEMANAGER: [
    "YARN",
    "MAPREDUCE2",
    "TEZ",
    "HIVE",
    "PIG",
    "OOZIE",
    "SLIDER",
    "SPARK",
  ],
  APP_TIMELINE_SERVER: [
    "YARN",
    "MAPREDUCE2",
    "TEZ",
    "HIVE",
    "OOZIE",
    "SLIDER",
    "SPARK",
  ],
  HISTORYSERVER: ["MAPREDUCE2", "HIVE", "PIG", "OOZIE"],
  HIVE_SERVER: ["HIVE", "FALCON", "ATLAS", "OOZIE"],
  HIVE_METASTORE: ["HIVE", "PIG", "FALCON", "ATLAS", "OOZIE"],
  WEBHCAT_SERVER: ["HIVE"],
  OOZIE_SERVER: ["OOZIE", "FALCON", "KNOX"],
  MYSQL_SERVER: ["HIVE", "OOZIE", "RANGER", "RANGER_KMS"],
  METRICS_COLLECTOR: ["AMBARI_METRICS"],
};

export const dbPropertyMap = {
  HIVE_SERVER: {
    type: "hive-env",
    name: "hive_database_type",
  },
  HIVE_METASTORE: {
    type: "hive-env",
    name: "hive_database_type",
  },
  OOZIE_SERVER: {
    type: "oozie-site",
    name: "oozie.service.JPAService.jdbc.driver",
  },
};

export enum reassignSteps {
  GET_STARTED = "GET_STARTED",
  ASSIGN_MASTER = "ASSIGN_MASTER",
  REVIEW = "REVIEW",
  CONFIGURE_COMPONENT="CONFIGURE_COMPONENT",
  MANUAL_COMMANDS="MANUAL_COMMANDS",
  START_AND_TEST_SERVICES="START_AND_TEST_SERVICES",
  FINALIZE_MOVE="FINALIZE_MOVE"
}

export const componentSpecificTypesMap:any= {
    'NAMENODE': [
      {
        serviceName: 'HBASE',
        configTypes: ['hbase-site']
      },
      {
        serviceName: 'ACCUMULO',
        configTypes: ['accumulo-site']
      },
      {
        serviceName: 'HAWQ',
        configTypes: ['hawq-site', 'hdfs-client']
      }
    ],
    'RESOURCEMANAGER': [
      {
        serviceName: 'HAWQ',
        configTypes: ['hawq-site', 'yarn-client']
      }
    ]
  }

  export const secureConfigsMap= [
    {
      componentName: 'NAMENODE',
      configs: [
        {
          site: 'hdfs-site',
          keytab: 'dfs.namenode.keytab.file',
          principal: 'dfs.namenode.kerberos.principal'
        },
        {
          site: 'hdfs-site',
          keytab: 'dfs.web.authentication.kerberos.keytab',
          principal: 'dfs.web.authentication.kerberos.principal'
        }
      ]
    },
    {
      componentName: 'SECONDARY_NAMENODE',
      configs: [
        {
          site: 'hdfs-site',
          keytab: 'dfs.secondary.namenode.keytab.file',
          principal: 'dfs.secondary.namenode.kerberos.principal'
        },
        {
          site: 'hdfs-site',
          keytab: 'dfs.web.authentication.kerberos.keytab',
          principal: 'dfs.web.authentication.kerberos.principal'
        }
      ]
    },
    {
      componentName: 'RESOURCEMANAGER',
      configs: [
        {
          site: 'yarn-site',
          keytab: 'yarn.resourcemanager.keytab',
          principal: 'yarn.resourcemanager.principal'
        },
        {
          site: 'yarn-site',
          keytab: 'yarn.resourcemanager.webapp.spnego-keytab-file',
          principal: 'yarn.resourcemanager.webapp.spnego-principal'
        }
      ]
    },
    {
      componentName: 'OOZIE_SERVER',
      configs: [
        {
          site: 'oozie-site',
          keytab: 'oozie.authentication.kerberos.keytab',
          principal: 'oozie.authentication.kerberos.principal'
        },
        {
          site: 'oozie-site',
          keytab: 'oozie.service.HadoopAccessorService.keytab.file',
          principal: 'oozie.service.HadoopAccessorService.kerberos.principal'
        }
      ]
    },
    {
      componentName: 'WEBHCAT_SERVER',
      configs: [
        {
          site: 'webhcat-site',
          keytab: 'templeton.kerberos.keytab',
          principal: 'templeton.kerberos.principal'
        }
      ]
    },
    {
      componentName: 'HIVE_SERVER',
      configs: [
        {
          site: 'hive-site',
          keytab: 'hive.server2.authentication.kerberos.keytab',
          principal: 'hive.server2.authentication.kerberos.principal'
        },
        {
          site: 'hive-site',
          keytab: 'hive.server2.authentication.spnego.keytab',
          principal: 'hive.server2.authentication.spnego.principal'
        }
      ]
    },
    {
      componentName: 'HIVE_METASTORE',
      configs: [
        {
          site: 'hive-site',
          keytab: 'hive.metastore.kerberos.keytab.file',
          principal: 'hive.metastore.kerberos.principal'
        }
      ]
    }

  ]

  export const additionalConfigsMap= [
    {
      componentName: 'RESOURCEMANAGER',
      configs: {
        'yarn-site': {
          'yarn.resourcemanager.address': '<replace-value>:8050',
          'yarn.resourcemanager.admin.address': '<replace-value>:8141',
          'yarn.resourcemanager.resource-tracker.address': '<replace-value>:8025',
          'yarn.resourcemanager.scheduler.address': '<replace-value>:8030',
          'yarn.resourcemanager.webapp.address': '<replace-value>:8088',
          'yarn.resourcemanager.webapp.https.address': '<replace-value>:8090',
          'yarn.resourcemanager.hostname': '<replace-value>'
        }
      }
    },
    {
      componentName: 'JOBTRACKER',
      configs: {
        'mapred-site': {
          'mapred.job.tracker.http.address': '<replace-value>:50030',
          'mapred.job.tracker': '<replace-value>:50300'
        }
      }
    },
    {
      componentName: 'SECONDARY_NAMENODE',
      configs: {
        'hdfs-site': {
          'dfs.secondary.http.address': '<replace-value>:50090'
        }
      },
      configs_Hadoop2: {
        'hdfs-site': {
          'dfs.namenode.secondary.http-address': '<replace-value>:50090'
        }
      }
    },
    {
      componentName: 'NAMENODE',
      configs: {
        'hdfs-site': {
          'dfs.http.address': '<replace-value>:50070',
          'dfs.https.address': '<replace-value>:50470'
        },
        'core-site': {
          'fs.default.name': 'hdfs://<replace-value>:8020'
        }
      },
      configs_Hadoop2: {
        'hdfs-site': {
          'dfs.namenode.rpc-address': '<replace-value>:8020',
          'dfs.namenode.http-address': '<replace-value>:50070',
          'dfs.namenode.https-address': '<replace-value>:50470'
        },
        'core-site': {
          'fs.defaultFS': 'hdfs://<replace-value>:8020'
        }
      }
    },
    {
      componentName: 'APP_TIMELINE_SERVER',
      configs: {
        'yarn-site': {
          'yarn.timeline-service.webapp.address': '<replace-value>:8188',
          'yarn.timeline-service.webapp.https.address': '<replace-value>:8190',
          'yarn.timeline-service.address': '<replace-value>:10200'
        }
      }
    },
    {
      componentName: 'TIMELINE_READER',
      configs: {
        'yarn-site': {
          'yarn.timeline-service.reader.webapp.address': '<replace-value>:8198',
          'yarn.timeline-service.reader.webapp.https.address': '<replace-value>:8199'
        }
      }
    },
    {
      componentName: 'OOZIE_SERVER',
      configs: {
        'oozie-site': {
          'oozie.base.url': 'http://<replace-value>:11000/oozie'
        },
        'core-site': {
          'hadoop.proxyuser.oozie.hosts': '<replace-value>'
        }
      }
    },
    {
      componentName: 'HIVE_METASTORE',
      configs: {
        'hive-site': {}
      }
    },
    {
      componentName: 'MYSQL_SERVER',
      configs: {
        'hive-site': {
          'javax.jdo.option.ConnectionURL': 'jdbc:mysql://<replace-value>/hive?createDatabaseIfNotExist=true'
        }
      }
    },
    {
      componentName: 'HISTORYSERVER',
      configs: {
        'mapred-site': {
          'mapreduce.jobhistory.webapp.address': '<replace-value>:19888',
          'mapreduce.jobhistory.address': '<replace-value>:10020'
        }
      }
    }
  ]
