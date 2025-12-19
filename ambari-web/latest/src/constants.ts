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

export enum ClusterProgressStatus {
  PROVISIONING = "PROVISIONING",
  ENABLING_NAMENODE_HA = "ENABLING_NAMENODE_HA",
  ADDING_HOST = "ADDING_HOST",
  ADDING_SERVICE = "ADDING_SERVICE",
  ENABLING_KERBEROS = "ENABLING_KERBEROS",
}
export enum ProgressStatus {
  IN_PROGRESS = "IN_PROGRESS",
  COMPLETED = "COMPLETED",
  FAILED = "FAILED",
}
export enum ViewLevel {
  REQUESTS = 1,
  HOSTS = 2,
  TASKS_LIST = 3,
  TASK_LOGS = 4,
}
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