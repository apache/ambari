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

import { cloneDeep, get, isEqual } from "lodash";
import { messages } from "../messages";
import {
  ConfigPropertiesType,
  ConfigTypeInfo,
  InputType,
  PropertyType,
  StackServices,
  StackServicesRoot,
} from "./types";
import { configValidator } from "../../Utils/validators";
import secureMappingObj from "../../data/secure_mappings";
import {
  kerberosIdentities,
  KerberosIdentity,
} from "../Kerberos/Kerberos_identitites";
import {
  evaluateThemeVisibility,
  normalizeDefaultThemeResponse,
  normalizeThemeResponse,
  resolveThemeConditionAttributes,
  ServiceTheme,
  themeTabKey,
  ThemePlacement,
} from "./themeEngine";

const secureConfigsMap: Record<string, boolean> = {};
secureMappingObj.forEach((sc: any) => {
  secureConfigsMap[sc.name] = true;
});

const kerberosIdentitiesMap: Record<string, boolean> = {};
kerberosIdentities.forEach((identity: KerberosIdentity) => {
  kerberosIdentitiesMap[identity.name] = true;
});

// Caching mechanism for config types info
const configTypesInfoMap: { [key: string]: ConfigTypeInfo } = {};

const isConfigValueLink = (value: any) => {
  return (
    configValidator.isConfigValueLink &&
    configValidator.isConfigValueLink(value)
  );
};

const isDirHeterogeneous = (name: string) => {
  return ["dfs.datanode.data.dir"].includes(name);
};

/**
 * Validate slider bounds for properties with slider widgets using dynamic property attributes
 */
const validateSliderBounds = (property: PropertyType, value: any): string => {
  // Skip validation for invisible/hidden properties, empty values, or config value links
  if (
    property.isVisible === false ||
    property.isHidden ||
    !value ||
    isConfigValueLink(value)
  ) {
    return "";
  }

  // Check if this property has slider widget configuration (maximum or minimum limits)
  const hasSliderLimits =
    property.propertyAttributes.maximum !== undefined ||
    property.propertyAttributes.minimum !== undefined;

  if (!hasSliderLimits) {
    return "";
  }

  const numericValue = parseFloat(value);
  if (isNaN(numericValue)) {
    return "";
  }

  // Validate maximum bounds
  if (property.propertyAttributes.maximum !== undefined) {
    const maxValue = Number(property.propertyAttributes.maximum);

    if (!isNaN(maxValue) && numericValue > maxValue) {
      const unit = property.propertyAttributes.unit || "";
      const unitSuffix = unit ? ` ${unit}` : "";
      const errorMessage = `Value cannot exceed ${maxValue}${unitSuffix}`;
      return errorMessage;
    }
  }

  // Validate minimum bounds
  if (property.propertyAttributes.minimum !== undefined) {
    const minValue = Number(property.propertyAttributes.minimum);

    if (!isNaN(minValue) && numericValue < minValue) {
      const unit = property.propertyAttributes.unit || "";
      const unitSuffix = unit ? ` ${unit}` : "";
      const errorMessage = `Value must be at least ${minValue}${unitSuffix}`;
      return errorMessage;
    }
  }

  return "";
};

const validateInput = (property: PropertyType, newValue: any) => {
  const isEmptyValueValid = property.propertyAttributes.empty_value_valid;
  const displayType = property.propertyAttributes.type;
  const propertyName = property.propertyName;

  if (
    property.isVisible === false ||
    property.isHidden ||
    displayType === InputType.CUSTOM
  ) {
    return "";
  }

  if (
    isEmptyValueValid &&
    typeof newValue === "string" &&
    newValue.trim() === ""
  ) {
    return "";
  }

  if (!isEmptyValueValid && newValue === "") {
    return "This is required";
  }

  // Check slider bounds validation first for all editable properties
  const sliderBoundsError = validateSliderBounds(property, newValue);
  if (sliderBoundsError) {
    return sliderBoundsError;
  }

  switch (displayType) {
    case InputType.CHECKBOX:
    case InputType.CUSTOM:
      return "";

    case InputType.INT:
      if (
        !configValidator.isValidInt(newValue) &&
        !isConfigValueLink(newValue)
      ) {
        return "Must contain digits only";
      }
      return "";

    case InputType.FLOAT:
      if (
        !configValidator.isValidFloat(newValue) &&
        !isConfigValueLink(newValue)
      ) {
        return "Must be a valid number";
      }
      return "";

    case InputType.DIRECTORY:
    case InputType.DIRECTORIES:
      if (isDirHeterogeneous(propertyName)) {
        if (!configValidator.isValidDataNodeDir(newValue)) {
          return 'dir format is wrong, can be "[{storage type}]/{dir name}"';
        }
      } else {
        if (!configValidator.isValidDir(newValue)) {
          return "Must be a slash or drive at the start, and must not contain white spaces";
        }
      }
      if (!configValidator.isAllowedDir(newValue)) {
        return 'Can\'t start with "home(s)"';
      }
      return configValidator.isNotTrimmedRight(newValue)
        ? "Trailing white spaces are not allowed"
        : "";

    case "email":
      return !configValidator.isValidEmail(newValue)
        ? "Must be a valid email address"
        : "";

    case InputType.SUPPORTTEXTCONNECTION:
    case InputType.HOST:
      return configValidator.isNotTrimmed(newValue)
        ? "Cannot contain leading or trailing whitespace"
        : "";

    case InputType.PASSWORD:
      if (propertyName === "ranger_admin_password") {
        if (String(newValue).length < 9) {
          return "Password should contain at least 9 symbols";
        }
      }
      return newValue !== property.confirmPassword
        ? "Passwords do not match"
        : "";

    case InputType.USER:
    case InputType.DATABASE:
    case InputType.DB_USER:
      return !configValidator.isValidDbName(newValue)
        ? "Value is not valid"
        : "";

    case InputType.LDAPURL:
      return !configValidator.isValidLdapsURL(newValue)
        ? "Must be a valid LDAP url"
        : "";

    default:
      if (
        [
          "javax.jdo.option.ConnectionURL",
          "oozie.service.JPAService.jdbc.url",
        ].includes(propertyName) &&
        !isConfigValueLink(newValue) &&
        configValidator.isNotTrimmed &&
        configValidator.isNotTrimmed(newValue)
      ) {
        return "Cannot contain leading or trailing whitespace";
      }
      return configValidator.isNotTrimmedRight(newValue)
        ? "Trailing white spaces are not allowed"
        : "";
  }
};

const getSectionErrorCount = (propertiesList: any) => {
  let errorCount = 0;
  Object.keys(propertiesList).forEach((propertyName) => {
    const property = propertiesList[propertyName];

    const isPropertyVisible =
      property.isVisible !== false && !property.isHidden;

    if (isPropertyVisible) {
      if (property.errorMessage && !property.tabName) {
        errorCount++;
      }
      if (property.overrideValues && Array.isArray(property.overrideValues)) {
        property.overrideValues.forEach((override: any) => {
          // Only count errors for override values that haven't been removed (value !== null)
          if (override.errorMessage && override.value !== null) {
            errorCount++;
          }
        });
      }
    }
  });

  return errorCount;
};

const kdcTypesValues: { [key: string]: string } = {
  "mit-kdc": messages["admin.kerberos.wizard.step1.option.kdc"],
  "active-directory": messages["admin.kerberos.wizard.step1.option.ad"],
  ipa: messages["admin.kerberos.wizard.step1.option.ipa"],
  none: messages["admin.kerberos.wizard.step1.option.manual"],
};

const formatPropertyValue = (
  serviceConfigProperty: PropertyType,
  originalValue: any,
) => {
  const value =
    originalValue == null ? serviceConfigProperty.value : originalValue;
  const displayType = serviceConfigProperty?.propertyAttributes?.type;

  if (serviceConfigProperty?.propertyName === "kdc_type") {
    return kdcTypesValues[value];
  }

  if (/^\s+$/.test("" + value)) {
    return " ";
  }

  switch (displayType) {
    case InputType.INT:
      if (/\d+m$/.test(value)) {
        return value.slice(0, value.length - 1);
      } else {
        const int = parseInt(value, 10);
        return isNaN(int) ? "" : int.toString();
      }
    case InputType.FLOAT:
      const float = parseFloat(value);
      return isNaN(float) ? "" : float.toString();
    case InputType.COMPONENTHOSTS:
      if (typeof value === InputType.STRING) {
        return value.replace(/\[|]|'|&apos;/g, "").split(",");
      }
      return value;
    case InputType.CONTENT:
    case InputType.STRING:
    case InputType.MULTILINE:
    case InputType.DIRECTORIES:
    case InputType.DIRECTORY:
      return trimProperty(serviceConfigProperty, originalValue);
    default:
      //this is a fallback to trim properties when type is undefined
      if (typeof value === "string") {
        return trimProperty(serviceConfigProperty, value);
      }
      return value;
  }
};

const trimProperty = (property: PropertyType, value: any) => {
  const displayType = property.propertyAttributes.type;
  const name = property.propertyName;

  let rez;

  switch (displayType) {
    case InputType.DIRECTORIES:
    case InputType.DIRECTORY:
      rez = value.replace(/,/g, " ").trim().split(/\s+/g).join(",");
      break;
    case InputType.HOST:
      rez = value.trim();
      break;
    case InputType.PASSWORD:
      // No trimming for passwords, assuming sensitive data handling
      break;
    default:
      if (
        name === "javax.jdo.option.ConnectionURL" ||
        name === "oozie.service.JPAService.jdbc.url"
      ) {
        rez = value.trim();
      } else {
        rez =
          typeof value === InputType.STRING
            ? value.replace(/(\s+$)/g, "")
            : value;
      }
  }
  return rez === "" || rez === undefined ? value : rez;
};

const getConfigTagFromFileName = (fileName: string) => {
  const configTag = fileName.endsWith(".xml")
    ? fileName.slice(0, -4)
    : fileName;
  return configTag;
};

const formatValue = (value: any) => {
  return configValidator.isValidFloat(value)
    ? parseFloat(value).toString()
    : value;
};

const getConfigPropertyByName = (
  name: string,
  configProperties: ConfigPropertiesType,
) => {
  for (const serviceName of Object.keys(configProperties)) {
    for (const configType of Object.keys(configProperties[serviceName])) {
      for (const propertyName of Object.keys(
        configProperties[serviceName][configType].properties,
      )) {
        const property =
          configProperties[serviceName][configType].properties[propertyName];
        if (property.propertyName === name) {
          return property;
        }
      }
    }
  }
  return null;
};

const getTabErrorCount = (
  configProperties: ConfigPropertiesType,
  serviceName: string,
  tabName: string,
) => {
  let errorCount = 0;
  if (!configProperties[serviceName]) {
    return 0;
  }
  Object.keys(configProperties[serviceName]).map((sections: string) => {
    Object.keys(configProperties[serviceName][sections].properties).map(
      (propertyName: string) => {
        const property =
          configProperties[serviceName][sections].properties[propertyName];

        const isPropertyVisible =
          property.isVisible !== false && !property.isHidden;

        if (
          isPropertyVisible &&
          property.tabName === tabName &&
          property.hasError
        ) {
          errorCount++;
        }
      },
    );
  });

  return errorCount;
};

const getAdvancedErrorCount = (
  configProperties: ConfigPropertiesType,
  serviceName: string,
) => {
  let errorCount = 0;
  Object.keys(configProperties[serviceName]).map((section: string) => {
    errorCount += configProperties[serviceName][section].errors;
  });
  return errorCount;
};

export function getConfigCategories(
  serviceName: string,
  optiions: { isHaEnabled?: boolean } = {},
) {
  const { isHaEnabled = false } = optiions;
  const categories = [];
  switch (serviceName) {
    case "HDFS":
      categories.push({
        name: "NAMENODE",
        displayName: "NameNode",
        showHost: true,
      });
      if (!isHaEnabled) {
        categories.push({
          name: "SECONDARY_NAMENODE",
          displayName: "Secondary NameNode",
          showHost: true,
        });
      }
      categories.push(
        { name: "DATANODE", displayName: "DataNode", showHost: true },
        { name: "General", displayName: "General" },
        { name: "NFS_GATEWAY", displayName: "NFS Gateway", showHost: true },
      );
      break;
    case "GLUSTERFS":
      categories.push({ name: "General", displayName: "General" });
      break;
    case "YARN":
      categories.push(
        {
          name: "RESOURCEMANAGER",
          displayName: "Resource Manager",
          showHost: true,
        },
        { name: "NODEMANAGER", displayName: "Node Manager", showHost: true },
        {
          name: "APP_TIMELINE_SERVER",
          displayName: "Application Timeline Server",
          showHost: true,
        },
        { name: "General", displayName: "General" },
        { name: "ResourceTypes", displayName: "Resource Types" },
        { name: "FaultTolerance", displayName: "Fault Tolerance" },
        { name: "Isolation", displayName: "Isolation" },
        {
          name: "CapacityScheduler",
          displayName: "Scheduler",
          siteFileName: "capacity-scheduler.xml",
        },
        {
          name: "ContainerExecutor",
          displayName: "Container Executor",
          siteFileName: "container-executor.xml",
        },
        { name: "Registry", displayName: "Registry" },
      );
      break;
    case "MAPREDUCE2":
      categories.push(
        {
          name: "HISTORYSERVER",
          displayName: "History Server",
          showHost: true,
        },
        { name: "General", displayName: "General" },
      );
      break;
    case "HIVE":
      categories.push(
        {
          name: "HIVE_METASTORE",
          displayName: "Hive Metastore",
          showHost: true,
        },
        {
          name: "WEBHCAT_SERVER",
          displayName: "WebHCat Server",
          showHost: true,
        },
        { name: "General", displayName: "General" },
        { name: "Performance", displayName: "Performance" },
        { name: "HIVE_SERVER2", displayName: "Hive Server2" },
        { name: "HIVE_CLIENT", displayName: "Hive Client" },
      );
      break;
    case "HBASE":
      categories.push(
        { name: "HBASE_MASTER", displayName: "HBase Master", showHost: true },
        {
          name: "HBASE_REGIONSERVER",
          displayName: "RegionServer",
          showHost: true,
        },
        { name: "General", displayName: "General" },
      );
      break;
    case "ZOOKEEPER":
      categories.push({
        name: "ZOOKEEPER_SERVER",
        displayName: "ZooKeeper Server",
        showHost: true,
      });
      break;
    case "OOZIE":
      categories.push(
        { name: "OOZIE_SERVER", displayName: "Oozie Server", showHost: true },
        {
          name: "Falcon - Oozie integration",
          displayName: "Falcon - Oozie integration",
        },
      );
      break;
    case "FALCON":
      categories.push(
        { name: "FALCON_SERVER", displayName: "Falcon Server", showHost: true },
        {
          name: "Falcon - Oozie integration",
          displayName: "Falcon - Oozie integration",
        },
        { name: "FalconStartupSite", displayName: "Falcon startup.properties" },
        { name: "FalconRuntimeSite", displayName: "Falcon runtime.properties" },
        { name: "General", displayName: "General" },
      );
      break;
    case "STORM":
      categories.push(
        { name: "NIMBUS", displayName: "Nimbus", showHost: true },
        { name: "SUPERVISOR", displayName: "Supervisor", showHost: true },
        {
          name: "STORM_UI_SERVER",
          displayName: "Storm UI Server",
          showHost: true,
        },
        {
          name: "STORM_REST_API",
          displayName: "Storm REST API Server",
          showHost: true,
        },
        { name: "DRPC_SERVER", displayName: "DRPC Server", showHost: true },
        { name: "General", displayName: "General" },
      );
      break;
    case "TEZ":
      categories.push({ name: "General", displayName: "General" });
      break;
    case "FLUME":
      categories.push({
        name: "FLUME_HANDLER",
        displayName: "flume.conf",
        siteFileName: "flume-conf",
        canAddProperty: false,
      });
      break;
    case "KNOX":
      categories.push({
        name: "KNOX_GATEWAY",
        displayName: "Knox Gateway",
        showHost: true,
      });
      break;
    case "KAFKA":
      categories.push({
        name: "KAFKA_BROKER",
        displayName: "Kafka Broker",
        showHost: true,
      });
      break;
    case "KERBEROS":
      categories.push(
        { name: "KDC", displayName: "KDC" },
        { name: "Kadmin", displayName: "Kadmin" },
        { name: "General", displayName: "General" },
      );
      break;
    case "AMBARI_METRICS":
      categories.push(
        { name: "General", displayName: "General" },
        { name: "MetricCollector", displayName: "Metric Collector" },
      );
      break;
    case "RANGER":
      categories.push(
        { name: "RANGER_ADMIN", displayName: "Admin Settings", showHost: true },
        { name: "DBSettings", displayName: "DB Settings" },
        { name: "RangerSettings", displayName: "Ranger Settings" },
        {
          name: "UnixAuthenticationSettings",
          displayName: "Unix Authentication Settings",
        },
        { name: "ADSettings", displayName: "AD Settings" },
        { name: "LDAPSettings", displayName: "LDAP Settings" },
        { name: "KnoxSSOSettings", displayName: "Knox SSO Settings" },
      );
      break;
    case "RANGER_KMS":
      categories.push({
        name: "RANGER_KMS_SERVER",
        displayName: "Ranger KMS Server",
        showHost: true,
      });
      break;
    case "ACCUMULO":
      categories.push({ name: "General", displayName: "General" });
      break;
    case "HAWQ":
      categories.push(
        { name: "General", displayName: "General" },
        { name: "AdvancedHawqCheck", displayName: "Advanced HAWQ Check" },
      );
      break;
    case "LOGSEARCH":
      categories.push({
        name: "LogsearchAdminJson",
        displayName: "Advanced logsearch-admin-json",
      });
      break;
    case "KYUUBI":
      categories.push(
        { name: "General", displayName: "General" },
        { name: "Session", displayName: "Session" },
        { name: "Engine", displayName: "Engine" },
        { name: "Security", displayName: "Security" },
        { name: "Performance", displayName: "Performance" },
        { name: "Environment", displayName: "Environment" },
      );
      break;
    case "SQOOP":
      categories.push(
        { name: "General", displayName: "General" },
        { name: "Performance", displayName: "Performance" },
        { name: "Security", displayName: "Security" },
        { name: "Environment", displayName: "Environment" },
      );
      break;
    default:
      categories.push({ name: "General", displayName: "General" });
  }
  return categories;
}

export function fetchComponentHostNamesByComponent(
  components: any[],
  componentName: string,
): string[] {
  const component = components.find((c) => c.componentName === componentName);
  if (!component || !component.hostComponents) return [];
  return component.hostComponents.map((hc: any) => hc.HostRoles.host_name);
}

export const buildConfigsJSON = (
  configProperties: ConfigPropertiesType,
  excludeKerberos: boolean = false,
) => {
  const configurations: {
    [key: string]: { properties: { [key: string]: string } };
  } = {};

  Object.keys(configProperties).forEach((serviceName) => {
    // Skip Kerberos service if excludeKerberos is true
    if (excludeKerberos && serviceName === "KERBEROS") {
      return;
    }

    Object.keys(configProperties[serviceName]).forEach((configType) => {
      Object.keys(configProperties[serviceName][configType].properties).forEach(
        (propertyName) => {
          const property =
            configProperties[serviceName][configType].properties[propertyName];
          const type = get(property, "type", "");

          if (
            property.isRequiredByAgent === false ||
            (property.value === null && serviceName !== "MISC")
          ) {
            return;
          }

          if (!configurations[type]) {
            configurations[type] = { properties: {} };
          }

          const configValue =
            serviceName === "MISC" && property.value === null
              ? ""
              : property.value;
          configurations[type]["properties"][property.propertyName] =
            configValue;
        },
      );
    });
  });

  return configurations;
};

const removeRangerConfigs = (configProps: ConfigPropertiesType) => {
  const updatedConfigs = { ...configProps };

  Object.keys(updatedConfigs).forEach((serviceName) => {
    Object.keys(updatedConfigs[serviceName]).forEach((sectionName) => {
      if (sectionName.toLowerCase().includes("ranger")) {
        delete updatedConfigs[serviceName][sectionName];
      }
    });
  });

  return updatedConfigs;
};

const getConfigByName = (
  propertyName: string,
  categoryName: string,
  serviceName: string,
  configProperties: ConfigPropertiesType,
) => {
  if (propertyName && categoryName && serviceName) {
    return get(
      configProperties,
      [serviceName, categoryName, "properties", propertyName],
      null,
    );
  }
};

function filterConfigProperties(
  configProperties: ConfigPropertiesType,
  search: string,
  propertyFilters?: {
    showOverridden: boolean;
    showFinal: boolean;
    showIssues: boolean;
  },
): ConfigPropertiesType {
  const configCopy = cloneDeep(configProperties);
  const normalizeSearchValue = (value: unknown) =>
    String(value ?? "")
      .toLowerCase()
      .replace(/\s+/g, "");
  const lowerSearch = normalizeSearchValue(search);

  Object.keys(configCopy).forEach((service) =>
    Object.keys(configCopy[service]).forEach((type) =>
      Object.keys(configCopy[service][type].properties).forEach(
        (propertyName) => {
          const property = configCopy[service][type].properties[propertyName];
          if (property.value === null) {
            property.isVisible = false;
            return;
          }

          // Text search filter
          const searchableValues = [
            propertyName,
            property.propertyDisplayname,
            property.propertyDescription,
            property.description,
            property.property_description,
            property.savedValue,
            property.value,
            ...(Array.isArray(property.overrideValues)
              ? property.overrideValues.flatMap((override: any) => [
                  override?.value,
                  override?.groupName,
                  override?.group?.name,
                ])
              : []),
          ];
          const matchesSearch = searchableValues.some((value) =>
            normalizeSearchValue(value).includes(lowerSearch),
          );

          // Apply property filters (overridden, final, issues)
          let matchesPropertyFilters = true;

          if (propertyFilters) {
            const hasActiveFilters =
              propertyFilters.showOverridden ||
              propertyFilters.showFinal ||
              propertyFilters.showIssues;

            if (hasActiveFilters) {
              const isOverridden = Boolean(
                Array.isArray(property.overrideValues) &&
                property.overrideValues.length > 0,
              );
              const isFinal =
                property.final === "true" || property.final === "True";
              const hasIssues = Boolean(
                property.errorMessage || property.hasError,
              );

              matchesPropertyFilters =
                (!propertyFilters.showOverridden || isOverridden) &&
                (!propertyFilters.showFinal || isFinal) &&
                (!propertyFilters.showIssues || hasIssues);
            }
          }

          property.isVisible =
            !search && !propertyFilters
              ? true
              : (search ? matchesSearch : true) && matchesPropertyFilters;
        },
      ),
    ),
  );

  return configCopy;
}

function findPropertyByPropertyName(
  config: ConfigPropertiesType,
  propertyName: string,
) {
  for (const groupKey in config) {
    const group = config[groupKey];
    for (const subGroupKey in group) {
      const subGroup = group[subGroupKey];
      const props = subGroup.properties;
      for (const propKey in props) {
        const prop = props[propKey];
        if (prop.propertyName === propertyName) {
          return prop;
        }
      }
    }
  }
  return null;
}

function updateVisibilityByForeignKeys(configProperties: ConfigPropertiesType) {
  const configCopy = cloneDeep(configProperties);

  Object.keys(configCopy).forEach((service) =>
    Object.keys(configCopy[service]).forEach((type) =>
      Object.keys(configCopy[service][type].properties).forEach(
        (propertyName) => {
          const property = configCopy[service][type].properties[propertyName];

          if (service === "RANGER" && propertyName.includes("usersync")) {
            const enableUserSyncProperty = findPropertyByPropertyName(
              configCopy,
              "ranger.usersync.enabled",
            );
            const syncSourceProperty = findPropertyByPropertyName(
              configCopy,
              "ranger.usersync.source.impl.class",
            );

            if (
              enableUserSyncProperty &&
              (enableUserSyncProperty.value === "false" ||
                enableUserSyncProperty.value === "No")
            ) {
              property.isVisible = false;
              property.isHidden = true;
              property.errorMessage = "";
            } else if (
              syncSourceProperty &&
              propertyName.includes("ldap") &&
              !syncSourceProperty.value.includes("Ldap")
            ) {
              property.isVisible = false;
              property.isHidden = true;
              property.errorMessage = "";
            } else if (propertyName.includes("usersync.group")) {
              const groupPropertiesToHide = [
                "ranger.usersync.group.nameattribute",
                "ranger.usersync.group.objectclass",
                "ranger.usersync.group.searchbase",
                "ranger.usersync.group.memberattributename",
              ];

              if (groupPropertiesToHide.includes(propertyName)) {
                if (
                  !syncSourceProperty ||
                  !syncSourceProperty.value.includes("LdapUserGroupBuilder")
                ) {
                  property.isVisible = false;
                  property.isHidden = true;
                  property.errorMessage = "";
                }
              }
            }
          }

          if (property.propertyAttributes.options) {
            for (const option of property.propertyAttributes.options) {
              if (option.foreignKeys && Array.isArray(option.foreignKeys)) {
                option.foreignKeys.forEach((foreignKey: string) => {
                  const foreignProperty = findPropertyByPropertyName(
                    configCopy,
                    foreignKey,
                  );
                  if (foreignProperty) {
                    if (property.value === option.displayName) {
                      foreignProperty.isVisible = true;
                      foreignProperty.errorMessage = validateInput(
                        foreignProperty,
                        foreignProperty.value,
                      );
                      foreignProperty.isHidden = false;
                    } else {
                      foreignProperty.isVisible = false;
                      foreignProperty.isHidden = true;
                      foreignProperty.errorMessage = "";
                    }
                  }
                });
              }
            }
          }
        },
      ),
    ),
  );

  return configCopy;
}

function addTabNames(
  configProperties: ConfigPropertiesType,
  themes: any,
  allThemes = false,
) {
  const configCopy = cloneDeep(configProperties);
  const services = Object.keys(configCopy);
  const normalized = allThemes
    ? normalizeDefaultThemeResponse(themes, services)
    : normalizeThemeResponse(themes, "default", services);

  services.forEach((serviceName) => {
    const serviceTheme = normalized.byService[serviceName];
    serviceTheme?.tabs.forEach((tab, tabIndex, tabs) => {
      if (tab.isAdvanced) return;
      const attachedPlacements = tab.sections.flatMap((section) =>
        section.subsections.flatMap((subsection) => [
          ...subsection.placements,
          ...subsection.tabs.flatMap(
            (subsectionTab) => subsectionTab.placements,
          ),
        ]),
      );
      attachedPlacements.forEach((placement) => {
        if (!placement.widget) return;
        const property =
          configCopy[serviceName]?.[placement.configType]?.properties?.[
            placement.propertyName
          ];
        if (property) property.tabName = themeTabKey(tab, tabs, tabIndex);
      });
    });
  });

  return configCopy;
}

function validateAllProperties(configProperties: ConfigPropertiesType) {
  const configCopy = cloneDeep(configProperties);

  Object.keys(configCopy).forEach((service) => {
    Object.keys(configCopy[service]).forEach((type) => {
      Object.keys(configCopy[service][type].properties).forEach(
        (propertyName) => {
          const property = configCopy[service][type].properties[propertyName];

          if (property.isVisible !== false && !property.isHidden) {
            property.errorMessage = validateInput(property, property.value);
          } else {
            property.errorMessage = "";
          }
        },
      );

      // Then, after all properties are validated, calculate the error count
      configCopy[service][type].errors = getSectionErrorCount(
        configCopy[service][type].properties,
      );
    });
  });

  return configCopy;
}

function setTabErrorCounts(configProperties: any) {
  // Apply visibility logic first to ensure hidden properties don't contribute to error counts
  let configCopy = cloneDeep(configProperties);

  // Apply Ranger usersync visibility logic
  Object.keys(configCopy).forEach((service) =>
    Object.keys(configCopy[service]).forEach((type) =>
      Object.keys(configCopy[service][type].properties).forEach(
        (propertyName) => {
          const property = configCopy[service][type].properties[propertyName];

          // Special handling for Ranger usersync properties
          if (service === "RANGER" && propertyName.includes("usersync")) {
            const enableUserSyncProperty = findPropertyByPropertyName(
              configCopy,
              "ranger.usersync.enabled",
            );
            const syncSourceProperty = findPropertyByPropertyName(
              configCopy,
              "ranger.usersync.source.impl.class",
            );

            if (
              enableUserSyncProperty &&
              (enableUserSyncProperty.value === "false" ||
                enableUserSyncProperty.value === "No")
            ) {
              property.isVisible = false;
              property.isHidden = true;
              property.errorMessage = "";
            } else if (
              syncSourceProperty &&
              propertyName.includes("ldap") &&
              !syncSourceProperty.value.includes("Ldap")
            ) {
              property.isVisible = false;
              property.isHidden = true;
              property.errorMessage = "";
            } else if (propertyName.includes("usersync.group")) {
              const groupPropertiesToHide = [
                "ranger.usersync.group.nameattribute",
                "ranger.usersync.group.objectclass",
                "ranger.usersync.group.searchbase",
                "ranger.usersync.group.memberattributename",
              ];

              if (groupPropertiesToHide.includes(propertyName)) {
                if (
                  !syncSourceProperty ||
                  !syncSourceProperty.value.includes("LdapUserGroupBuilder")
                ) {
                  property.isVisible = false;
                  property.isHidden = true;
                  property.errorMessage = "";
                }
              }
            }
          }
        },
      ),
    ),
  );

  let tabErrorcounts: any = {};

  Object.keys(configCopy).forEach((service) =>
    Object.keys(configCopy[service]).forEach((type) => {
      Object.keys(configCopy[service][type].properties).forEach(
        (propertyName) => {
          const property = configCopy[service][type].properties[propertyName];

          let tabName = property.tabName || "Advanced";

          // Initialize service layer if it doesn't exist
          if (!tabErrorcounts[service]) {
            tabErrorcounts[service] = {
              total: 0, // Add a total counter for the service
              tabs: {}, // Store tab-specific counts in a nested object
            };
          }

          // Initialize tab layer if it doesn't exist
          if (!tabErrorcounts[service].tabs[tabName]) {
            tabErrorcounts[service].tabs[tabName] = 0;
          }

          const isPropertyVisible =
            property.isVisible !== false && !property.isHidden;

          if (isPropertyVisible) {
            // Check for property errors (main property and override values)
            const hasMainPropertyError =
              property.hasError || property.errorMessage;
            const hasOverrideErrors = property?.overrideValues?.some(
              (o: any) => o.errorMessage && o.value !== null,
            );

            // Increment error count if property has an error
            if (hasMainPropertyError || hasOverrideErrors) {
              tabErrorcounts[service].tabs[tabName] += 1;
              tabErrorcounts[service].total += 1; // Increment the service total as well
            }
          }
        },
      );
    }),
  );

  return tabErrorcounts;
}

const getTotalErros = (tabErrors: any): boolean => {
  return Object.keys(tabErrors).every(
    (section) => tabErrors[section].total === 0,
  );
};

const evaluateDependsOnForConfig = (
  configProperties: ConfigPropertiesType,
  chosenService: string,
  dependsOn: any,
  services: any,
) =>
  evaluateThemeVisibility(
    Array.isArray(dependsOn) ? dependsOn : [],
    configProperties,
    chosenService,
    Array.isArray(services) ? services : [],
  );

type ThemeAttributeSnapshot = {
  exists: boolean;
  value: unknown;
};

type ThemeAttributeState = {
  base: Record<string, ThemeAttributeSnapshot>;
  lastApplied: Record<string, unknown>;
  managed: string[];
};

const THEME_ATTRIBUTE_STATE = "__themeAttributeState";
const THEME_PLACEMENT_STATES = "__themePlacementStates";

const THEME_ATTRIBUTE_TARGETS: Record<string, string> = {
  type: "displayType",
  overridable: "isOverridable",
  visible: "isVisible",
  empty_value_valid: "isRequired",
  editable_only_at_install: "isReconfigurable",
  show_property_name: "showLabel",
  read_only: "isEditable",
  ui_only_property: "isRequiredByAgent",
};

const themeAttributeTarget = (attribute: string) =>
  THEME_ATTRIBUTE_TARGETS[attribute] ?? attribute;

const PROPERTY_ATTRIBUTES_PREFIX = "propertyAttributes.";

const propertyAttributeTarget = (attribute: string) =>
  `${PROPERTY_ATTRIBUTES_PREFIX}${attribute}`;

const propertyTargetValue = (
  property: Record<string, unknown>,
  target: string,
) => {
  if (!target.startsWith(PROPERTY_ATTRIBUTES_PREFIX)) return property[target];
  const attribute = target.slice(PROPERTY_ATTRIBUTES_PREFIX.length);
  const propertyAttributes = property.propertyAttributes as
    Record<string, unknown> | undefined;
  return propertyAttributes?.[attribute];
};

const propertyTargetExists = (
  property: Record<string, unknown>,
  target: string,
) => {
  if (!target.startsWith(PROPERTY_ATTRIBUTES_PREFIX)) {
    return Object.prototype.hasOwnProperty.call(property, target);
  }
  const attribute = target.slice(PROPERTY_ATTRIBUTES_PREFIX.length);
  const propertyAttributes = property.propertyAttributes as
    Record<string, unknown> | undefined;
  return Boolean(
    propertyAttributes &&
    Object.prototype.hasOwnProperty.call(propertyAttributes, attribute),
  );
};

const setPropertyTarget = (
  property: Record<string, unknown>,
  target: string,
  value: unknown,
) => {
  if (!target.startsWith(PROPERTY_ATTRIBUTES_PREFIX)) {
    property[target] = value;
    return;
  }
  const attribute = target.slice(PROPERTY_ATTRIBUTES_PREFIX.length);
  const propertyAttributes =
    (property.propertyAttributes as Record<string, unknown> | undefined) ?? {};
  property.propertyAttributes = propertyAttributes;
  propertyAttributes[attribute] = value;
};

const deletePropertyTarget = (
  property: Record<string, unknown>,
  target: string,
) => {
  if (!target.startsWith(PROPERTY_ATTRIBUTES_PREFIX)) {
    delete property[target];
    return;
  }
  const attribute = target.slice(PROPERTY_ATTRIBUTES_PREFIX.length);
  const propertyAttributes = property.propertyAttributes as
    Record<string, unknown> | undefined;
  if (propertyAttributes) delete propertyAttributes[attribute];
};

const snapshotProperty = (
  property: Record<string, unknown>,
  target: string,
): ThemeAttributeSnapshot => ({
  exists: propertyTargetExists(property, target),
  value: propertyTargetValue(property, target),
});

const restoreSnapshot = (
  property: Record<string, unknown>,
  target: string,
  snapshot: ThemeAttributeSnapshot,
) => {
  if (snapshot.exists) setPropertyTarget(property, target, snapshot.value);
  else deletePropertyTarget(property, target);
};

const preparePropertyThemeState = (
  property: Record<string, unknown>,
): ThemeAttributeState => {
  const existing = property[THEME_ATTRIBUTE_STATE] as
    ThemeAttributeState | undefined;
  const state = existing ?? { base: {}, lastApplied: {}, managed: [] };

  state.managed.forEach((target) => {
    if (
      !isEqual(propertyTargetValue(property, target), state.lastApplied[target])
    ) {
      state.base[target] = snapshotProperty(property, target);
    }
  });
  state.managed.forEach((target) => {
    const snapshot = state.base[target];
    if (snapshot) restoreSnapshot(property, target, snapshot);
  });
  state.lastApplied = {};
  state.managed = [];
  property[THEME_ATTRIBUTE_STATE] = state;
  return state;
};

const restorePropertyThemeState = (property: Record<string, unknown>) => {
  const state = preparePropertyThemeState(property);
  Object.keys(state.base).forEach((target) => {
    restoreSnapshot(property, target, state.base[target]);
  });
  delete property[THEME_ATTRIBUTE_STATE];
  delete property[THEME_PLACEMENT_STATES];
};

const invertedThemeAttributes = new Set([
  "empty_value_valid",
  "editable_only_at_install",
  "read_only",
  "ui_only_property",
]);

const themeBoolean = (value: unknown) => {
  if (value === "true") return true;
  if (value === "false") return false;
  return Boolean(value);
};

const effectiveThemeAttributeValue = (attribute: string, value: unknown) =>
  invertedThemeAttributes.has(attribute) ? !themeBoolean(value) : value;

const resolvedPlacementAttributes = (
  placement: ThemePlacement,
  configProperties: ConfigPropertiesType,
  serviceName: string,
  installedServices: readonly string[],
) => {
  const attributes: Record<string, unknown> = {};
  Object.entries(placement.valueAttributes).forEach(([attribute, value]) => {
    attributes[themeAttributeTarget(attribute)] = effectiveThemeAttributeValue(
      attribute,
      value,
    );
    if (attribute !== "value") {
      attributes[propertyAttributeTarget(attribute)] = value;
    }
  });
  Object.entries(
    resolveThemeConditionAttributes(
      placement.dependsOn,
      configProperties,
      serviceName,
      installedServices,
    ),
  ).forEach(([attribute, value]) => {
    attributes[themeAttributeTarget(attribute)] = effectiveThemeAttributeValue(
      attribute,
      value,
    );
    if (attribute !== "value") {
      attributes[propertyAttributeTarget(attribute)] = value;
    }
  });
  return attributes;
};

const findPropertyByThemePath = (
  configProperties: ConfigPropertiesType,
  serviceName: string,
  placement: ThemePlacement,
) => {
  const direct =
    configProperties[serviceName]?.[placement.configType]?.properties?.[
      placement.propertyName
    ];
  if (direct) return direct;

  const serviceConfigs = configProperties[serviceName] ?? {};
  for (const configType of Object.keys(serviceConfigs)) {
    const property =
      serviceConfigs[configType]?.properties?.[placement.propertyName];
    if (property?.fileName?.replace(/\.xml$/, "") === placement.configType) {
      return property;
    }
  }
  return undefined;
};

type PlacementThemeState = {
  placement: ThemePlacement;
  attributes: Record<string, unknown>;
  containerVisible: boolean;
};

const placementThemeStates = (
  serviceTheme: ServiceTheme,
  configProperties: ConfigPropertiesType,
  serviceName: string,
  installedServices: readonly string[],
) => {
  const statesById = new Map<string, PlacementThemeState>();

  serviceTheme.placements.forEach((placement) => {
    statesById.set(placement.id, {
      placement,
      attributes: resolvedPlacementAttributes(
        placement,
        configProperties,
        serviceName,
        installedServices,
      ),
      containerVisible: true,
    });
  });

  const constrain = (placement: ThemePlacement, containerVisible: boolean) => {
    const state = statesById.get(placement.id);
    if (state)
      state.containerVisible = state.containerVisible && containerVisible;
  };

  serviceTheme.tabs.forEach((tab) => {
    if (tab.isAdvanced) return;
    tab.sections.forEach((section) => {
      section.subsections.forEach((subsection) => {
        const subsectionVisible = evaluateThemeVisibility(
          subsection.dependsOn,
          configProperties,
          serviceName,
          installedServices,
        );
        subsection.placements.forEach((placement) =>
          constrain(placement, subsectionVisible),
        );
        subsection.tabs.forEach((subsectionTab) => {
          const subsectionTabVisible = evaluateThemeVisibility(
            subsectionTab.dependsOn,
            configProperties,
            serviceName,
            installedServices,
          );
          subsectionTab.placements.forEach((placement) =>
            constrain(placement, subsectionVisible && subsectionTabVisible),
          );
        });
      });
    });
  });

  return statesById;
};

const constrainedThemeBooleanTargets = new Set([
  "isEditable",
  "isOverridable",
  "isReconfigurable",
]);

const applyPlacementThemeState = (
  property: Record<string, unknown>,
  placementState: PlacementThemeState,
) => {
  const state = property[THEME_ATTRIBUTE_STATE] as ThemeAttributeState;
  const attributes = {
    isVisible: true,
    ...placementState.attributes,
  };

  Object.entries(attributes).forEach(([target, requestedValue]) => {
    state.base[target] ??= snapshotProperty(property, target);
    const baseValue = state.base[target].exists
      ? state.base[target].value
      : target === "isVisible" || constrainedThemeBooleanTargets.has(target);
    let value = requestedValue;

    if (target === "isVisible") {
      value =
        baseValue !== false &&
        requestedValue !== false &&
        placementState.containerVisible &&
        property.isHidden !== true;
    } else if (constrainedThemeBooleanTargets.has(target)) {
      value = baseValue !== false && requestedValue !== false;
    }

    setPropertyTarget(property, target, value);
    state.managed.push(target);
    state.lastApplied[target] = value;
  });

  Object.keys(state.base).forEach((target) => {
    if (!state.managed.includes(target)) delete state.base[target];
  });
};

const canonicalPlacementThemeState = (
  states: readonly PlacementThemeState[],
): PlacementThemeState => {
  const attributes = Object.assign(
    {},
    ...states.map((placementState) => placementState.attributes),
  );
  const isVisible = states.some(
    (placementState) =>
      placementState.containerVisible &&
      placementState.attributes.isVisible !== false,
  );
  attributes.isVisible = isVisible;
  attributes[propertyAttributeTarget("visible")] = isVisible;
  return {
    placement: states[states.length - 1].placement,
    attributes,
    containerVisible: true,
  };
};

export const getThemePlacementProperty = <
  T extends Record<string, unknown>,
>(
  property: T,
  placementId: string,
): T => {
  const placementStates = property[THEME_PLACEMENT_STATES] as
    | Record<string, PlacementThemeState>
    | undefined;
  const placementState = placementStates?.[placementId];
  if (!placementState) return property;

  const effectiveProperty = cloneDeep(property);
  preparePropertyThemeState(effectiveProperty);
  applyPlacementThemeState(effectiveProperty, placementState);
  delete effectiveProperty[THEME_ATTRIBUTE_STATE];
  delete effectiveProperty[THEME_PLACEMENT_STATES];
  return effectiveProperty;
};

function updateVisibilityForDependsOn(
  configProperties: ConfigPropertiesType,
  themeData: unknown,
  configSection: string,
  installedServices: string[],
  allThemes = false,
) {
  const configsCopy = cloneDeep(configProperties);
  const serviceNames = Object.keys(configsCopy);
  const normalized = allThemes
    ? normalizeDefaultThemeResponse(themeData, serviceNames)
    : normalizeThemeResponse(themeData, configSection, serviceNames);

  serviceNames.forEach((serviceName) => {
    Object.values(configsCopy[serviceName]).forEach((configType) => {
      Object.values(configType.properties).forEach((property) => {
        preparePropertyThemeState(property);
        delete property[THEME_PLACEMENT_STATES];
      });
    });

    const serviceTheme = normalized.byService[serviceName];
    if (!serviceTheme || serviceTheme.isFallback) {
      Object.values(configsCopy[serviceName]).forEach((configType) => {
        Object.values(configType.properties).forEach((property) => {
          restorePropertyThemeState(property);
        });
      });
      return;
    }

    const propertyPlacementStates = new Map<
      Record<string, unknown>,
      PlacementThemeState[]
    >();
    placementThemeStates(
      serviceTheme,
      configsCopy,
      serviceName,
      installedServices,
    ).forEach((placementState) => {
      const property = findPropertyByThemePath(
        configsCopy,
        serviceName,
        placementState.placement,
      );
      if (!property) return;
      const states = propertyPlacementStates.get(property) ?? [];
      states.push(placementState);
      propertyPlacementStates.set(property, states);
    });

    propertyPlacementStates.forEach((placementStates, property) => {
      property[THEME_PLACEMENT_STATES] = Object.fromEntries(
        placementStates.map((placementState) => [
          placementState.placement.id,
          placementState,
        ]),
      );
      applyPlacementThemeState(
        property,
        canonicalPlacementThemeState(placementStates),
      );
      if (!property.isVisible) property.errorMessage = "";
    });

    Object.values(configsCopy[serviceName]).forEach((configType) => {
      Object.values(configType.properties).forEach((property) => {
        const state = property[THEME_ATTRIBUTE_STATE] as ThemeAttributeState;
        if (!state.managed.length) delete property[THEME_ATTRIBUTE_STATE];
      });
    });
  });

  return configsCopy;
}

const getConfigTypesInfoFromService = (
  stackService: StackServices,
): ConfigTypeInfo => {
  const serviceName = stackService.service_name;

  if (configTypesInfoMap[serviceName]) {
    // Return cached result
    return configTypesInfoMap[serviceName];
  }

  const configTypes = stackService.config_types;
  const configTypesInfo: ConfigTypeInfo = {
    items: [],
    supportsFinal: [],
    supportsAddingForbidden: [],
  };

  if (configTypes) {
    for (const key in configTypes) {
      if (configTypes.hasOwnProperty(key)) {
        configTypesInfo.items.push(key);
        if (
          configTypes[key].supports &&
          configTypes[key].supports.final === "true"
        ) {
          configTypesInfo.supportsFinal.push(key);
        }
        if (
          configTypes[key].supports &&
          configTypes[key].supports.adding_forbidden === "true"
        ) {
          configTypesInfo.supportsAddingForbidden.push(key);
        }
      }
    }
  }

  // Cache the result
  configTypesInfoMap[serviceName] = configTypesInfo;
  return configTypesInfo;
};

const shouldSupportFinal = (
  serviceName: string,
  filename: string,
  stackData: StackServicesRoot,
): boolean => {
  const unsupportedServiceNames = ["MISC", "Cluster"];
  if (
    !serviceName ||
    unsupportedServiceNames.includes(serviceName) ||
    !filename
  ) {
    return false;
  }

  const serviceItem = stackData.items.find(
    (item) => item.StackServices.service_name === serviceName,
  );

  if (!serviceItem) {
    return false;
  }

  const configTypesInfo = getConfigTypesInfoFromService(
    serviceItem.StackServices,
  );

  return !!configTypesInfo.supportsFinal.find((configType) =>
    filename.startsWith(configType),
  );
};

const shouldSupportAddingForbidden = (
  serviceName: string,
  filename: string,
  stackData: StackServicesRoot,
): boolean => {
  const unsupportedServiceNames = ["MISC", "Cluster"];
  if (
    !serviceName ||
    unsupportedServiceNames.includes(serviceName) ||
    !filename
  ) {
    return false;
  }

  const serviceItem = stackData.items.find(
    (item) => item.StackServices.service_name === serviceName,
  );

  if (!serviceItem) {
    return false;
  }

  const configTypesInfo = getConfigTypesInfoFromService(
    serviceItem.StackServices,
  );
  return !!configTypesInfo.supportsAddingForbidden.find((configType) =>
    filename.startsWith(configType),
  );
};

const kerberosIdentitiesDescription = (propertyName: string): string => {
  const identity = kerberosIdentities.find((id) => id.name === propertyName);
  return identity ? identity.displayName : "";
};

const setPropertyIsEditable = (
  configProperties: ConfigPropertiesType,
  selectedConfigGroup: { isDefault: boolean },
  isKerberosEnabled: boolean,
): ConfigPropertiesType => {
  const configCopy = cloneDeep(configProperties);
  const identities = kerberosIdentitiesMap;

  Object.keys(configCopy).forEach((serviceName) => {
    Object.keys(configCopy[serviceName]).forEach((configType) => {
      Object.keys(configCopy[serviceName][configType].properties).forEach(
        (propertyName) => {
          const property =
            configCopy[serviceName][configType].properties[propertyName];

          const canEdit = property.isEditable;

          if (!selectedConfigGroup.isDefault && !canEdit) {
            property.isEditable = false;
            return;
          }

          if (isKerberosEnabled && identities[property.propertyName]) {
            property.isEditable = false;
            property.isSecureConfig = true;
            return;
          }

          property.isEditable = canEdit;
        },
      );
    });
  });

  return configCopy;
};

const hideComponentConfigsBasedOnAvailability = (
  configProperties: ConfigPropertiesType,
  allServiceModels: any,
): ConfigPropertiesType => {
  const configCopy = cloneDeep(configProperties);

  // Following Ember.js pattern: hide component configs when component is not available
  // Check if HDFS HA is enabled using the same logic as the HDFS service model
  const hdfsModel = allServiceModels?.hdfs;
  const isHAEnabled = hdfsModel?.isNameNodeHaEnabled || false;

  if (configCopy.HDFS) {
    // Following Ember.js logic: when HA is enabled, SECONDARY_NAMENODE component is not shown
    // Therefore, its configs should also be hidden (following the same pattern as isShownOnAddServiceAssignMasterPage)
    Object.keys(configCopy.HDFS).forEach((configType) => {
      // Hide entire SECONDARY_NAMENODE config type when HA is enabled (following Ember.js component visibility pattern)
      if (configType === "SECONDARY_NAMENODE") {
        Object.keys(configCopy.HDFS[configType].properties || {}).forEach(
          (propertyName) => {
            const property =
              configCopy.HDFS[configType].properties[propertyName];
            if (isHAEnabled) {
              property.isVisible = false;
              property.isHidden = true;
            }
          },
        );
      }

      // Also check for individual properties that might be SecondaryNameNode related
      Object.keys(configCopy.HDFS[configType].properties || {}).forEach(
        (propertyName) => {
          const property = configCopy.HDFS[configType].properties[propertyName];
          if (
            property.propertyName === "dfs.namenode.checkpoint.dir" ||
            (property.propertyDisplayname &&
              property.propertyDisplayname
                .toLowerCase()
                .includes("secondarynamenode"))
          ) {
            if (isHAEnabled) {
              property.isVisible = false;
              property.isHidden = true;
            }
          }
        },
      );
    });
  }

  return configCopy;
};

const getIsSecure = (propertyName: string): boolean => {
  return !!secureConfigsMap[propertyName];
};

export {
  validateInput,
  getSectionErrorCount,
  formatPropertyValue,
  trimProperty,
  getAdvancedErrorCount,
  getTabErrorCount,
  getConfigTagFromFileName,
  removeRangerConfigs,
  getConfigByName,
  filterConfigProperties,
  formatValue,
  getConfigPropertyByName,
  findPropertyByPropertyName,
  updateVisibilityByForeignKeys,
  addTabNames,
  validateAllProperties,
  setTabErrorCounts,
  getTotalErros,
  updateVisibilityForDependsOn,
  shouldSupportFinal,
  shouldSupportAddingForbidden,
  getConfigTypesInfoFromService,
  getIsSecure,
  kerberosIdentitiesDescription,
  setPropertyIsEditable,
  evaluateDependsOnForConfig,
  hideComponentConfigsBasedOnAvailability,
};
