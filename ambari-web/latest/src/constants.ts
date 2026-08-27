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



export enum ReadOptions {
  FILE = "file",
  URL = "url",
}

export const clusterName = "tusker2041024";

export enum ProgressStatus {
  IN_PROGRESS = "IN_PROGRESS",
  COMPLETED = "COMPLETED",
  FAILED = "FAILED",
}
export enum TaskExecutionStatus {
  IN_PROGRESS = "IN_PROGRESS",
  FAILED = "FAILED",
  COMPLETED = "COMPLETED",
}
export enum ViewLevel {
  REQUESTS = 1,
  HOSTS = 2,
  TASKS_LIST = 3,
  TASK_LOGS = 4,
}

export const mastersNotShown = [
    "MYSQL_SERVER",
    "POSTGRESQL_SERVER",
    "HIVE_SERVER_INTERACTIVE",
  ];

export const serviceNames = {
  HDFS: "HDFS",
  YARN: "YARN",
  RANGER: "RANGER",
  ZOOKEEPER: "ZOOKEEPER",
  HIVE: "HIVE",
  RANGER_KMS: "RANGER_KMS",
  HBASE: "HBASE",
};

export const serviceNameModelMapping: { [key: string]: string } = {
  HDFS: "hdfs",
  YARN: "yarn",
  MAPREDUCE2: "mapreduce2",
  TEZ: "tez",
  HIVE: "hive",
  HBASE: "hbase",
  ZOOKEEPER: "zk",
  AMBARI_METRICS: "ambari_metrics",
  RANGER: "ranger",
  RANGER_KMS: "ranger_kms",
  KERBEROS: "kerberos",
  SPARK3: "spark3",
  SSM: "ssm",
  TRINO: "trino",
  SQOOP: "sqoop",
  KYUUBI: "kyuubi",
  TRINO_GATEWAY: "trino_gateway",
  PINOT: "pinot",
};

export const modelKeyNameToServiceNameMapping = {
  hdfs: "HDFS",
  yarn: "YARN",
  mapreduce2: "MAPREDUCE2",
  tez: "TEZ",
  hive: "HIVE",
  hbase: "HBASE",
  zk: "ZOOKEEPER",
  ambari_metrics: "AMBARI_METRICS",
  ranger: "RANGER",
  ranger_kms: "RANGER_KMS",
  kerberos: "KERBEROS",
  spark3: "SPARK3",
  ssm: "SSM",
  trino: "TRINO",
  sqoop: "SQOOP",
  kyuubi: "KYUUBI",
  trino_gateway: "TRINO_GATEWAY",
  pinot: "PINOT",
}

export const serviceNameDisplayMapping = {
  HDFS: "HDFS",
  YARN: "YARN",
  RANGER: "Ranger",
  ZOOKEEPER: "Zookeeper",
  HIVE: "Hive",
  SPARK: "Spark3",
  MAPREDUCE2: "MapReduce2",
  TEZ: "Tez",
  HBASE: "HBase",
  KERBEROS: "Kerberos",
  RANGER_KMS: "Ranger KMS",
  AMBARI_METRICS: "Ambari Metrics",
  TRINO: "Trino",
  SSM: "SSM",
  SQOOP: "Sqoop",
  KYUUBI: "Kyuubi",
  TRINO_GATEWAY: "Trino Gateway",
  PINOT: "Pinot",
};

export const displayNameServiceMapping = {
  HDFS: "HDFS",
  YARN: "YARN",
  Ranger: "RANGER",
  Zookeeper: "ZOOKEEPER",
  Hive: "HIVE",
  Spark3: "SPARK",
  MapReduce2: "MAPREDUCE2",
  Tez: "TEZ",
  HBase: "HBASE",
  Kerberos: "KERBEROS",
  "Ranger KMS": "RANGER_KMS",
  "Ambari Metrics": "AMBARI_METRICS",
  Trino: "TRINO",
  SSM: "SSM",
  Sqoop: "SQOOP",
  Kyuubi: "KYUUBI",
  "Trino Gateway": "TRINO_GATEWAY",
  Pinot: "PINOT",
};

export const toBePreservedPaths = {
  highAvailability: "HIGH_AVAILABILITY_LAST_PATH",
  kerberos: "KERBEROS_WIZARD_LAST_PATH",
};

export const DEFAULT_SUPPORTS: Record<string, boolean> = {
  preUpgradeCheck: true,
  displayOlderVersions: false,
  autoRollbackHA: false,
  alwaysEnableManagedMySQLForHive: false,
  preKerberizeCheck: false,
  customizeAgentUserAccount: false,
  installGanglia: false,
  opsDuringRollingUpgrade: false,
  customizedWidgetLayout: false,
  showPageLoadTime: false,
  skipComponentStartAfterInstall: false,
  preInstallChecks: false,
  serviceAutoStart: true,
  logSearch: true,
  redhatSatellite: false,
  addingNewRepository: false,
  kerberosStackAdvisor: true,
  logCountVizualization: false,
  createAlerts: false,
  enabledWizardForHostOrderedUpgrade: true,
  manageJournalNode: true,
  enableToggleKerberos: true,
  enableAddDeleteServices: true,
  regenerateKeytabsOnSingleHost: false,
  disableCredentialsAutocompleteForRepoUrls: true,
  enableNewServiceRestartOptions: false,
};

export const selectMasterComponentsForService = {
  HIVE: ["HIVE_METASTORE", "HIVE_SERVER"],
  ZOOKEEPER: ["ZOOKEEPER_SERVER"],
  AMBARI_METRICS: ["METRICS_COLLECTOR", "METRICS_GRAFANA"],
  MAPREDUCE2: ["HISTORYSERVER"],
  YARN: ["RESOURCEMANAGER", "NODEMANAGER"],
  SSM: ["SSM_SERVER"],
  TRINO: ["TRINO_COORDINATOR"],
  SPARK3: ["SPARK3_JOBHISTORYSERVER"],
  RANGER: ["RANGER_ADMIN", "RANGER_USERSYNC"],
  RANGER_KMS: ["RANGER_KMS_SERVER"],
  HBASE: ["HBASE_MASTER"],
  HDFS: ["NAMENODE", "SECONDARY_NAMENODE"],
  TRINO_GATEWAY: ["TRINO_GATEWAY"],
  KYUUBI: ["KYUUBI"],
  PINOT: ["PINOT_CONTROLLER"]
};

export const selectSlaveComponentsForService = {
  AMBARI_METRICS: ["METRICS_MONITOR"],
  SSM: ["SSM_AGENT"],
  TRINO: ["TRINO_WORKER"],
  SPARK3: ["LIVY3_SERVER", "SPARK3_THRIFTSERVER"],
  RANGER: ["RANGER_TAGSYNC"],
  HBASE: ["HBASE_REGIONSERVER", "PHOENIX_QUERY_SERVER"],
  HDFS: ["DATANODE", "JOURNALNODE", "NFS_GATEWAY", "ROUTER", "ZKFC"],
  PINOT: ["PINOT_BROKER", "PINOT_MINION", "PINOT_SERVER"]
};

export const selectClientComponentsForService = {
  HIVE: ["HIVE_CLIENT"],
  ZOOKEEPER: ["ZOOKEEPER_CLIENT"],
  MAPREDUCE2: ["MAPREDUCE2_CLIENT"],
  TRINO: ["TRINO_CLI"],
  SPARK3: ["SPARK3_CLIENT"],
  YARN: ["YARN_CLIENT"],
};

export const filenameExceptions = ["alert_notification"];

export enum ClusterProgressStatus {
  PROVISIONING = "PROVISIONING",
  ENABLING_NAMENODE_HA = "ENABLING_NAMENODE_HA",
  ENABLING_NAMENODE_FEDERATION = "ENABLING_NAMENODE_FEDERATION",
  ADDING_OBSERVER_NAMENODE = "ADDING_OBSERVER_NAMENODE",
  MANAGING_JOURNALNODES = "MANAGING_JOURNALNODES",
  ADDING_HOST = "ADDING_HOST",
  ADDING_SERVICE = "ADDING_SERVICE",
  ENABLING_KERBEROS = "ENABLING_KERBEROS",
  ENABLING_RANGER_ADMIN_HA = "ENABLING_RANGER_ADMIN_HA",
  ENABLING_RM_HA = "ENABLING_RM_HA",
  REASSIGNING_COMPONENT = "REASSIGNING_COMPONENT",
}
