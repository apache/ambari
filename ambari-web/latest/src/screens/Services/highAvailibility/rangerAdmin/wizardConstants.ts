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

export interface RangerAdminHaConfig {
  siteName: string;
  propertyName: string;
  serviceName: string;
  serviceDisplayName: string;
}

export const wizardConfigs: RangerAdminHaConfig[] = [
  {
    siteName: "admin-properties",
    propertyName: "policymgr_external_url",
    serviceName: "RANGER",
    serviceDisplayName: "Ranger",
  },
  {
    siteName: "ranger-hdfs-security",
    propertyName: "ranger.plugin.hdfs.policy.rest.url",
    serviceName: "HDFS",
    serviceDisplayName: "HDFS",
  },
  {
    siteName: "ranger-yarn-security",
    propertyName: "ranger.plugin.yarn.policy.rest.url",
    serviceName: "YARN",
    serviceDisplayName: "YARN",
  },
  {
    siteName: "ranger-hbase-security",
    propertyName: "ranger.plugin.hbase.policy.rest.url",
    serviceName: "HBASE",
    serviceDisplayName: "HBase",
  },
  {
    siteName: "ranger-hive-security",
    propertyName: "ranger.plugin.hive.policy.rest.url",
    serviceName: "HIVE",
    serviceDisplayName: "Hive",
  },
  {
    siteName: "ranger-knox-security",
    propertyName: "ranger.plugin.knox.policy.rest.url",
    serviceName: "KNOX",
    serviceDisplayName: "Knox",
  },
  {
    siteName: "ranger-kafka-security",
    propertyName: "ranger.plugin.kafka.policy.rest.url",
    serviceName: "KAFKA",
    serviceDisplayName: "Kafka",
  },
  {
    siteName: "ranger-kms-security",
    propertyName: "ranger.plugin.kms.policy.rest.url",
    serviceName: "RANGER_KMS",
    serviceDisplayName: "Ranger KMS",
  },
  {
    siteName: "ranger-storm-security",
    propertyName: "ranger.plugin.storm.policy.rest.url",
    serviceName: "STORM",
    serviceDisplayName: "Storm",
  },
  {
    siteName: "ranger-atlas-security",
    propertyName: "ranger.plugin.atlas.policy.rest.url",
    serviceName: "ATLAS",
    serviceDisplayName: "Atlas",
  },
];
