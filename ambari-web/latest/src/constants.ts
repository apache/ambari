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

import { get } from "lodash";
import { kerberos_ui_properties, UIProperty } from "./data/configs/kerberos_ui_properties";
import { alert_notifications } from "./data/configs/alert_notifications";
import { messages } from "./screens/messages";

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
export const filenameExceptions = ["alert_notification"];
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
const getConfigTagFromFileName = (fileName: any) => {
  if (fileName === "") return "";
  return fileName.endsWith(".xml") ? fileName.slice(0, -4) : fileName;
};
export const redirectToLogin = () => {
  window.location.href = "#/login";
  window.location.reload();
};

const getDefaultDisplayType = (value: any) => {
  return value && typeof value === "string" && value.includes("\n")
    ? "multiLine"
    : "string";
};
const configId = (name: any, fileName: any) => {
  return name + "__" + getConfigTagFromFileName(fileName);
};
const parseConfig = (config: any, configToPlain: any) => {
  const parsedConfig: Record<string, any> = {};
  for (const [key, value] of Object.entries(configToPlain)) {
    parsedConfig[key] = getValue(config, value);
  }
  return parsedConfig;
};
const trimProperty = (property: any) => {
  const displayType = get(property, "displayType", "");
  const value = get(property, "value", "");
  const name = get(property, "name", "");
  let rez;

  switch (displayType) {
    case "directories":
    case "directory":
      rez = value.replace(/,/g, " ").trim().split(/\s+/g).join(",");
      break;
    case "host":
      rez = value.trim();
      break;
    case "password":
      break;
    default:
      if (
        name === "javax.jdo.option.ConnectionURL" ||
        name === "oozie.service.JPAService.jdbc.url"
      ) {
        rez = value.trim();
      } else {
        rez = typeof value === "string" ? value.replace(/(\s+$)/g, "") : value;
      }
  }

  return rez === "" || rez === undefined ? value : rez;
};
const getValue = (obj: any, path: any) => {
  return path.split(".").reduce((acc: any, part: any) => acc && acc[part], obj);
};
const formatPropertyValue = (
  serviceConfigProperty: any,
  originalValue: any
) => {
  const value =
    originalValue == null ? serviceConfigProperty.value : originalValue;
  const displayType =
    serviceConfigProperty.displayType ||
    serviceConfigProperty.valueAttributes?.type;

  if (serviceConfigProperty.name === "kdc_type") {
    return "";
  }

  if (/^\s+$/.test("" + value)) {
    return " ";
  }

  switch (displayType) {
    case "int":
      if (/\d+m$/.test(value)) {
        return value.slice(0, value.length - 1);
      } else {
        const intValue = parseInt(value, 10);
        return isNaN(intValue) ? "" : intValue.toString();
      }
    case "float":
      const floatValue = parseFloat(value);
      return isNaN(floatValue) ? "" : floatValue.toString();
    case "componentHosts":
      if (typeof value === "string") {
        return value.replace(/\[|]|'|&apos;/g, "").split(",");
      }
      return value;
    case "content":
    case "string":
    case "multiLine":
    case "directories":
    case "directory":
      return trimProperty({ displayType, value });
    default:
      return value;
  }
};
const transformAlertNotifications = (notifications: any[]): UIProperty[] => {
  return notifications.map((notification) => ({
    name: notification.name,
    displayName: notification.displayName,
    description: notification.description,
    displayType: notification.displayType,
    isRequiredByAgent: false,
    isOverridable: notification.isOverridable,
    isVisible: notification.isVisible,
    isRequired: notification.isRequired,
    isReconfigurable: notification.isReconfigurable,
    serviceName: notification.serviceName,
    category: notification.category,
    recommendedValue: notification.recommendedValue,
    rowStyleClass: notification.rowStyleClass,
    filename: notification.filename,
    index: undefined,
  }));
};
const addUIOnlyProperties = (configs: any) => {
  const transformedAlertNotifications =
    transformAlertNotifications(alert_notifications);
  const combinedProperties = kerberos_ui_properties.concat(
    transformedAlertNotifications
  );
  combinedProperties.forEach((p: any) => {
    if (p.name === "dfs.ha.fencing.methods") return;

    configs.push({
      id: configId(p.name, p.filename),
      name: p.name,
      display_name: p.displayName,
      file_name: p.filename,
      description: p.description || "",
      is_required_by_agent: p.isRequiredByAgent !== false,
      service_name: p.serviceName,
      supports_final: false,
      category: p.category,
      index: p.index,
    });
  });
};

const getDescription = (description: any, displayType: any) => {
  const additionalDescription = get(
    messages,
    "services.service.config.password.additionalDescription"
  );

  if (displayType === "password") {
    if (description && !description.includes(additionalDescription)) {
      return `${description}\n${additionalDescription}`;
    } else {
      return additionalDescription;
    }
  }
  return description;
};
export function mapStackConfigProperties(json: any) {
  const configToPlain = {
    id: "id",
    name: "StackConfigurations.property_name",
    displayName: "StackConfigurations.property_display_name",
    fileName: "StackConfigurations.type",
    filename: "StackConfigurations.type",
    description: "StackConfigurations.property_description",
    value: "StackConfigurations.property_value",
    recommendedValue: "StackConfigurations.property_value",
    serviceName: "StackConfigurations.service_name",
    stackName: "StackConfigurations.stack_name",
    stackVersion: "StackConfigurations.stack_version",
    isOverridable: "StackConfigurations.property_value_attributes.overridable",
    isVisible: "StackConfigurations.property_value_attributes.visible",
    showLabel:
      "StackConfigurations.property_value_attributes.show_property_name",
    displayType: "StackConfigurations.property_value_attributes.type",
    unit: "StackConfigurations.property_value_attributes.unit",
    isRequired: "is_required",
    isReconfigurable: "is_reconfigurable",
    isEditable: "is_editable",
    isRequiredByAgent: "is_required_by_agent",
    isFinal: "recommended_is_final",
    recommendedIsFinal: "recommended_is_final",
    supportsFinal: "supports_final",
    propertyDependedBy: "StackConfigurations.property_depended_by",
    propertyDependsOn: "StackConfigurations.property_depends_on",
    valueAttributes: "StackConfigurations.property_value_attributes",
    category: "category",
    index: "index",
    radioName: "radioName",
    options: "options",
    dependentConfigPattern: "dependentConfigPattern",
  };

  let filteredConfigs = [];
  var clusterConfigs: boolean = false;
  if (json && json.Versions) {
    json = { items: [json] };
    clusterConfigs = true;
  }

  if (json && json.items) {
    const configs: any = [];

    json.items.forEach((stackItem: any) => {
      var configTypeInfo = clusterConfigs
        ? get(stackItem, "Versions.config_types")
        : get(stackItem, "StackServices.config_types");
      stackItem.configurations.forEach((config: any) => {
        if (clusterConfigs) {
          config.StackConfigurations = config.StackLevelConfigurations;
        }
        const configType = getConfigTagFromFileName(
          get(config, "StackConfigurations.type", "")
        );
        config.id = configId(
          config.StackConfigurations?.property_name,
          configType
        );
        config.recommended_is_final =
          config.StackConfigurations?.final === "true";
        config.supports_final =
          !!configTypeInfo[configType] &&
          configTypeInfo[configType].supports.final === "true";

        const attributes =
          config.StackConfigurations?.property_value_attributes;
        if (attributes) {
          config.is_required =
            !attributes?.empty_value_valid &&
            config.StackConfigurations?.property_value !== null;
          config.is_reconfigurable = !(
            attributes?.editable_only_at_install ||
            config.StackConfigurations?.type === "cluster-env.xml"
          );
          config.is_editable = !attributes?.read_only;
          config.is_required_by_agent = !attributes?.ui_only_property;
        }

        config.StackConfigurations = config.StackConfigurations || {};
        if (!config.StackConfigurations?.property_display_name) {
          config.StackConfigurations.property_display_name =
            config.StackConfigurations?.property_name;
        }

        if (!config.StackConfigurations?.service_name) {
          config.StackConfigurations.service_name = "MISC";
        }

        if (!attributes || !attributes.type) {
          if (!attributes) {
            config.StackConfigurations.property_value_attributes = {};
          }
          config.StackConfigurations.property_value_attributes.type =
            getDefaultDisplayType(config.StackConfigurations?.property_value);
        }

        config.StackConfigurations.property_depended_by = [];
        if (config.dependencies && config.dependencies.length > 0) {
          config.dependencies.forEach((dep: any) => {
            config.StackConfigurations?.property_depended_by.push({
              type: dep.StackConfigurationDependency.dependency_type,
              name: dep.StackConfigurationDependency.dependency_name,
            });
          });
        }

        const staticConfigInfo = parseConfig(config, configToPlain);
        const value =
          staticConfigInfo.recommendedValue || staticConfigInfo.value;
        staticConfigInfo.value = staticConfigInfo.recommendedValue =
          formatPropertyValue(staticConfigInfo, value);
        staticConfigInfo.isSecureConfig = false;
        staticConfigInfo.description = getDescription(
          staticConfigInfo.description,
          staticConfigInfo.displayType
        );
        staticConfigInfo.name = JSON.parse(`"${staticConfigInfo.name}"`);
        staticConfigInfo.isUserProperty = false;
        staticConfigInfo.index = staticConfigInfo.index ?? null;

        configs.push(staticConfigInfo);
      });
    });
    addUIOnlyProperties(configs);
    filteredConfigs = configs;
  }
  return filteredConfigs;
}