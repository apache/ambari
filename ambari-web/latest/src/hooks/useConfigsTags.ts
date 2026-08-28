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

import { useContext, useEffect, useState } from "react";
import ConfigsApi from "../api/configsApi";
import { AppContext } from "../store/context";
import { map } from "lodash";

interface ConfigResponse {
  Clusters: {
    desired_configs: {
      [key: string]: {
        tag: string;
      };
    };
  };
}

export default function useHDFSConfigsTags() {
  const { clusterName, services } = useContext(AppContext);
  const selectedServices = map(services, "ServiceInfo.service_name");
  const [configsData, setConfigsData] = useState<any>({});
  const [configsError, setConfigsError] = useState("");
  const [isConfigsLoading, setIsConfigsLoading] = useState(true);
  async function loadConfigsTags() {
    const inferredTags: any = {};
    setConfigsError("");
    setIsConfigsLoading(true);
    try {
      const data: ConfigResponse = await ConfigsApi.loadConfigTags(clusterName);
      const urlParams = [];
      const hdfsSiteTag = data.Clusters.desired_configs["hdfs-site"].tag;
      const coreSiteTag = data.Clusters.desired_configs["core-site"].tag;
      const zkSiteTag = data.Clusters.desired_configs["zoo.cfg"].tag;

      urlParams.push("(type=hdfs-site&tag=" + hdfsSiteTag + ")");
      urlParams.push("(type=core-site&tag=" + coreSiteTag + ")");
      urlParams.push("(type=zoo.cfg&tag=" + zkSiteTag + ")");
      inferredTags.hdfsSiteTag = { name: "hdfsSiteTag", value: hdfsSiteTag };
      inferredTags.coreSiteTag = { name: "coreSiteTag", value: coreSiteTag };
      inferredTags.zkSiteTag = { name: "zkSiteTag", value: zkSiteTag };
      if (selectedServices.includes("HBASE")) {
        const hbaseSiteTag = data.Clusters.desired_configs["hbase-site"].tag;
        urlParams.push("(type=hbase-site&tag=" + hbaseSiteTag + ")");
        inferredTags.hbaseSiteTag = {
          name: "hbaseSiteTag",
          value: hbaseSiteTag,
        };
      }
      if (selectedServices.includes("ACCUMULO")) {
        const accumuloSiteTag =
          data.Clusters.desired_configs["accumulo-site"].tag;
        urlParams.push("(type=accumulo-site&tag=" + accumuloSiteTag + ")");
        inferredTags.accumuloSiteTag = {
          name: "accumuloSiteTag",
          value: accumuloSiteTag,
        };
      }
      if (selectedServices.includes("HAWQ")) {
        const hawqSiteTag = data.Clusters.desired_configs["hawq-site"].tag;
        const hdfsClientTag = data.Clusters.desired_configs["hdfs-client"].tag;

        urlParams.push("(type=hawq-site&tag=" + hawqSiteTag + ")");
        urlParams.push("(type=hdfs-client&tag=" + hdfsClientTag + ")");

        inferredTags.hawqSiteTag = { name: "hawqSiteTag", value: hawqSiteTag };
        inferredTags.hdfsClientTag = {
          name: "hdfsClientTag",
          value: hdfsClientTag,
        };
      }
      if (selectedServices.includes("RANGER")) {
        const rangerEnvTag = data.Clusters.desired_configs["ranger-env"].tag;
        urlParams.push("(type=ranger-env&tag=" + rangerEnvTag + ")");
        inferredTags.rangerEnvTag = {
          name: "rangerEnvTag",
          value: rangerEnvTag,
        };
        if ("ranger-hdfs-plugin-properties" in data.Clusters.desired_configs) {
          const rangerHdfsPluginPropertiesTag =
            data.Clusters.desired_configs["ranger-hdfs-plugin-properties"].tag;
          urlParams.push(
            "(type=ranger-hdfs-plugin-properties&tag=" +
              rangerHdfsPluginPropertiesTag +
              ")"
          );
          inferredTags.rangerHdfsPluginPropertiesTag = {
            name: "rangerHdfsPluginPropertiesTag",
            value: rangerHdfsPluginPropertiesTag,
          };
        }
        if ("ranger-hdfs-audit" in data.Clusters.desired_configs) {
          const rangerHdfsAuditTag =
            data.Clusters.desired_configs["ranger-hdfs-audit"].tag;
          urlParams.push(
            "(type=ranger-hdfs-audit&tag=" + rangerHdfsAuditTag + ")"
          );
          inferredTags.rangerHdfsAuditTag = {
            name: "rangerHdfsAuditTag",
            value: rangerHdfsAuditTag,
          };
        }
        if ("ranger-yarn-audit" in data.Clusters.desired_configs) {
          const yarnAuditTag =
            data.Clusters.desired_configs["ranger-yarn-audit"].tag;
          urlParams.push("(type=ranger-yarn-audit&tag=" + yarnAuditTag + ")");
          inferredTags.yarnAuditTag = {
            name: "yarnAuditTag",
            value: yarnAuditTag,
          };
        }
        if (selectedServices.includes("HBASE")) {
          if ("ranger-hbase-audit" in data.Clusters.desired_configs) {
            const rangerHbaseAuditTag =
              data.Clusters.desired_configs["ranger-hbase-audit"].tag;
            urlParams.push(
              "(type=ranger-hbase-audit&tag=" + rangerHbaseAuditTag + ")"
            );
            inferredTags.rangerHbaseAuditTag = {
              name: "rangerHbaseAuditTag",
              value: rangerHbaseAuditTag,
            };
          }
          if (
            "ranger-hbase-plugin-properties" in data.Clusters.desired_configs
          ) {
            const rangerHbasePluginPropertiesTag =
              data.Clusters.desired_configs["ranger-hbase-plugin-properties"]
                .tag;
            urlParams.push(
              "(type=ranger-hbase-plugin-properties&tag=" +
                rangerHbasePluginPropertiesTag +
                ")"
            );
            inferredTags.rangerHbasePluginPropertiesTag = {
              name: "rangerHbasePluginPropertiesTag",
              value: rangerHbasePluginPropertiesTag,
            };
          }
        }
        if (selectedServices.includes("KAFKA")) {
          if ("ranger-kafka-audit" in data.Clusters.desired_configs) {
            const rangerKafkaAuditTag =
              data.Clusters.desired_configs["ranger-kafka-audit"].tag;
            urlParams.push(
              "(type=ranger-kafka-audit&tag=" + rangerKafkaAuditTag + ")"
            );
            inferredTags.rangerKafkaAuditTag = {
              name: "rangerKafkaAuditTag",
              value: rangerKafkaAuditTag,
            };
          }
        }
        if (selectedServices.includes("KNOX")) {
          if ("ranger-knox-audit" in data.Clusters.desired_configs) {
            const rangerKnoxAuditTag =
              data.Clusters.desired_configs["ranger-knox-audit"].tag;
            urlParams.push(
              "(type=ranger-knox-audit&tag=" + rangerKnoxAuditTag + ")"
            );
            inferredTags.rangerKnoxAuditTag = {
              name: "rangerKnoxAuditTag",
              value: rangerKnoxAuditTag,
            };
          }
          if (
            "ranger-knox-plugin-properties" in data.Clusters.desired_configs
          ) {
            const rangerKnoxPluginPropertiesTag =
              data.Clusters.desired_configs["ranger-knox-plugin-properties"]
                .tag;
            urlParams.push(
              "(type=ranger-knox-plugin-properties&tag=" +
                rangerKnoxPluginPropertiesTag +
                ")"
            );
            inferredTags.rangerKnoxPluginPropertiesTag = {
              name: "rangerKnoxPluginPropertiesTag",
              value: rangerKnoxPluginPropertiesTag,
            };
          }
        }
          if (selectedServices.includes("STORM")) {
            if ("ranger-storm-audit" in data.Clusters.desired_configs) {
              const rangerStormAuditTag =
                data.Clusters.desired_configs["ranger-storm-audit"].tag;
              urlParams.push(
                "(type=ranger-storm-audit&tag=" + rangerStormAuditTag + ")"
              );
              inferredTags.rangerStormAuditTag = {
                name: "rangerStormAuditTag",
                value: rangerStormAuditTag,
              };
            }
            if (
              "ranger-storm-plugin-properties" in data.Clusters.desired_configs
            ) {
              const rangerStormPluginPropertiesTag =
                data.Clusters.desired_configs["ranger-storm-plugin-properties"]
                  .tag;
              urlParams.push(
                "(type=ranger-storm-plugin-properties&tag=" +
                  rangerStormPluginPropertiesTag +
                  ")"
              );
              inferredTags.rangerStormPluginPropertiesTag = {
                name: "rangerStormPluginPropertiesTag",
                value: rangerStormPluginPropertiesTag,
              };
            }
          }
          if (selectedServices.includes("ATLAS")) {
            if ("ranger-atlas-audit" in data.Clusters.desired_configs) {
              const rangerAtlasAuditTag =
                data.Clusters.desired_configs["ranger-atlas-audit"].tag;
              urlParams.push(
                "(type=ranger-atlas-audit&tag=" + rangerAtlasAuditTag + ")"
              );
              inferredTags.rangerAtlasAuditTag = {
                name: "rangerAtlasAuditTag",
                value: rangerAtlasAuditTag,
              };
            }
          }
          if (selectedServices.includes("HIVE")) {
            if ("ranger-hive-audit" in data.Clusters.desired_configs) {
              const rangerHiveAuditTag =
                data.Clusters.desired_configs["ranger-hive-audit"].tag;
              urlParams.push(
                "(type=ranger-hive-audit&tag=" + rangerHiveAuditTag + ")"
              );
              inferredTags.rangerHiveAuditTag = {
                name: "rangerHiveAuditTag",
                value: rangerHiveAuditTag,
              };
            }
            if (
              "ranger-hive-plugin-properties" in data.Clusters.desired_configs
            ) {
              const rangerHivePluginPropertiesTag =
                data.Clusters.desired_configs["ranger-hive-plugin-properties"]
                  .tag;
              urlParams.push(
                "(type=ranger-hive-plugin-properties&tag=" +
                  rangerHivePluginPropertiesTag +
                  ")"
              );
              inferredTags.rangerHivePluginPropertiesTag = {
                name: "rangerHivePluginPropertiesTag",
                value: rangerHivePluginPropertiesTag,
              };
            }
          }
          if (selectedServices.includes("RANGER_KMS")) {
            if ("ranger-kms-audit" in data.Clusters.desired_configs) {
              const rangerKMSAuditTag =
                data.Clusters.desired_configs["ranger-kms-audit"].tag;
              urlParams.push(
                "(type=ranger-kms-audit&tag=" + rangerKMSAuditTag + ")"
              );
              inferredTags.rangerKMSAuditTag = {
                name: "rangerKMSAuditTag",
                value: rangerKMSAuditTag,
              };
            }
          }
      }
      const configsResponseData: any = await ConfigsApi.getConfigsByTags(
        clusterName,
        urlParams.join("|")
      );
      setConfigsData(configsResponseData);
    } catch (error: any) {
      setConfigsData({});
      setConfigsError(
        error?.response?.data?.message ||
          error?.message ||
          "Ambari could not load the current HDFS configurations."
      );
    } finally {
      setIsConfigsLoading(false);
    }
  }
  useEffect(()=>{
    void loadConfigsTags();
  },[])
  return {
    configsData,
    configsError,
    isConfigsLoading,
    reloadConfigs: loadConfigsTags,
  };
}
