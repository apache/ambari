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

export const dfsServices = ["HDFS", "GLUSTERFS"];
export const displayOrder: string[] = [
  "HDFS",
  "GLUSTERFS",
  "YARN",
  "MAPREDUCE2",
  "TEZ",
  "GANGLIA",
  "HIVE",
  "HAWQ",
  "PXF",
  "HBASE",
  "PIG",
  "SQOOP",
  "OOZIE",
  "ZOOKEEPER",
  "FALCON",
  "STORM",
  "FLUME",
  "ACCUMULO",
  "AMBARI_INFRA_SOLR",
  "AMBARI_METRICS",
  "ATLAS",
  "KAFKA",
  "KNOX",
  "LOGSEARCH",
  "RANGER",
  "RANGER_KMS",
  "SMARTSENSE",
  "SPARK",
  "SPARK2",
  "ZEPPELIN",
  "SPARK3",
  "SSM",
  "TRINO",
];

export const coSelectedServices: { [key: string]: string[] } = {
  YARN: ["MAPREDUCE2"],
};

export const excludeServicesOnDisplay: string[] = [
  "KERBEROS",
  "GANGLIA",
  "MAPREDUCE2",
];

export const warnningMessages: { [key: string]: string } = {
  RANGER:
    "Apache Ranger provides fine grained authorization and audit of access attempts for many Hadoop ecosystem services. If you do not install the Apache Ranger Service and enable Kerberos, the security of your cluster will be diminished. Are you sure you want to proceed without it?",
  AMBARI_METRICS:
    "Ambari Metrics collects metrics from the cluster and makes them available to Ambari. If you do not install Ambari Metrics service, metrics will not be accessible from Ambari. Are you sure you want to proceed without Ambari Metrics?",
};

export enum ModalType {
  MISSING_DEPENDANT_SERVICE = "Missing Dependant Service",
  MISSING_SERVICE = "Missing Service",
  MISSING_FILE_SYSTEM = "Missing File System",
}
