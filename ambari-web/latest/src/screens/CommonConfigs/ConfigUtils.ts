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

import { cloneDeep, get, isArray } from "lodash";
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
  const hasSliderLimits = property.propertyAttributes.maximum !== undefined ||
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

    const isPropertyVisible = property.isVisible !== false && !property.isHidden;

    if (isPropertyVisible) {
      if (
        property.errorMessage &&
        !property.tabName
      ) {
        errorCount++;
      }
      if (
        property.overrideValues &&
        Array.isArray(property.overrideValues)
      ) {
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
  originalValue: any
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
  configProperties: ConfigPropertiesType
) => {
  for (const serviceName of Object.keys(configProperties)) {
    for (const configType of Object.keys(configProperties[serviceName])) {
      for (const propertyName of Object.keys(
        configProperties[serviceName][configType].properties
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
  tabName: string
) => {
  let errorCount = 0;
  if (!configProperties[serviceName]) {
    return 0;
  }
  Object.keys(configProperties[serviceName]).map((sections: string) => {
    Object.keys(configProperties[serviceName][sections].properties).map(
      (propertyName: string) => {
        const property = configProperties[serviceName][sections].properties[propertyName];

        const isPropertyVisible = property.isVisible !== false && !property.isHidden;

        if (
          isPropertyVisible &&
          property.tabName === tabName &&
          property.hasError
        ) {
          errorCount++;
        }
      }
    );
  });

  return errorCount;
};

const getAdvancedErrorCount = (
  configProperties: ConfigPropertiesType,
  serviceName: string
) => {
  let errorCount = 0;
  Object.keys(configProperties[serviceName]).map((section: string) => {
    errorCount += configProperties[serviceName][section].errors;
  });
  return errorCount;
};

export function getConfigCategories(
  serviceName: string,
  optiions: { isHaEnabled?: boolean } = {}
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
        { name: "NFS_GATEWAY", displayName: "NFS Gateway", showHost: true }
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
        { name: "Registry", displayName: "Registry" }
      );
      break;
    case "MAPREDUCE2":
      categories.push(
        {
          name: "HISTORYSERVER",
          displayName: "History Server",
          showHost: true,
        },
        { name: "General", displayName: "General" }
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
        { name: "HIVE_CLIENT", displayName: "Hive Client" }
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
        { name: "General", displayName: "General" }
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
        }
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
        { name: "General", displayName: "General" }
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
        { name: "General", displayName: "General" }
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
        { name: "General", displayName: "General" }
      );
      break;
    case "AMBARI_METRICS":
      categories.push(
        { name: "General", displayName: "General" },
        { name: "MetricCollector", displayName: "Metric Collector" }
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
        { name: "KnoxSSOSettings", displayName: "Knox SSO Settings" }
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
        { name: "AdvancedHawqCheck", displayName: "Advanced HAWQ Check" }
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
        { name: "Environment", displayName: "Environment" }
      );
      break;
    case "SQOOP":
      categories.push(
        { name: "General", displayName: "General" },
        { name: "Performance", displayName: "Performance" },
        { name: "Security", displayName: "Security" },
        { name: "Environment", displayName: "Environment" }
      );
      break;
    default:
      categories.push({ name: "General", displayName: "General" });
  }
  return categories;
}

export function fetchComponentHostNamesByComponent(
  components: any[],
  componentName: string
): string[] {
  const component = components.find((c) => c.componentName === componentName);
  if (!component || !component.hostComponents) return [];
  return component.hostComponents.map((hc: any) => hc.HostRoles.host_name);
}

export const buildConfigsJSON = (
  configProperties: ConfigPropertiesType,
  excludeKerberos: boolean = false
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

          if (property.value === null && serviceName !== "MISC") {
            return;
          }

          if (!configurations[type]) {
            configurations[type] = { properties: {} };
          }
          
          const configValue = serviceName === "MISC" && property.value === null ? "" : property.value;
          configurations[type]["properties"][property.propertyName] = configValue;
          
        }
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
  configProperties: ConfigPropertiesType
) => {
  if (propertyName && categoryName && serviceName) {
    return get(
      configProperties,
      [serviceName, categoryName, "properties", propertyName],
      null
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
  }
): ConfigPropertiesType {
  const configCopy = cloneDeep(configProperties);
  const lowerSearch = search.toLowerCase().replace(/\s+/g, "");

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
          const propertyDisplayName = (property.propertyDisplayname || "")
            .toLowerCase()
            .replace(/\s+/g, "");
          const propertyValue = property.value
            ? property.value.toString().toLowerCase().replace(/\s+/g, "")
            : "";

          const matchesSearch =
            propertyName
              .toLowerCase()
              .replace(/\s+/g, "")
              .includes(lowerSearch) ||
            propertyDisplayName.includes(lowerSearch) ||
            (!!property.value &&
              property.value !== "" &&
              propertyValue.includes(lowerSearch));

          // Apply property filters (overridden, final, issues)
          let matchesPropertyFilters = true;

          if (propertyFilters) {
            const hasActiveFilters =
              propertyFilters.showOverridden ||
              propertyFilters.showFinal ||
              propertyFilters.showIssues;

            if (hasActiveFilters) {
              matchesPropertyFilters = false;

              // Check if property matches any active filter
              if (propertyFilters.showOverridden) {
                const isOverridden =
                  property.overrideValues &&
                  Array.isArray(property.overrideValues) &&
                  property.overrideValues.length > 0;
                if (isOverridden) {
                  matchesPropertyFilters = true;
                }
              }

              if (propertyFilters.showFinal) {
                const isFinal =
                  property.final === "true" || property.final === "True";
                if (isFinal) {
                  matchesPropertyFilters = true;
                }
              }

              if (propertyFilters.showIssues) {
                const hasIssues = !!(
                  property.errorMessage || property.hasError
                );
                if (hasIssues) {
                  matchesPropertyFilters = true;
                }
              }
            }
          }

          property.isVisible =
            !search && !propertyFilters
              ? true
              : (search ? matchesSearch : true) && matchesPropertyFilters;
        }
      )
    )
  );

  return configCopy;
}

function findPropertyByPropertyName(
  config: ConfigPropertiesType,
  propertyName: string
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
            const enableUserSyncProperty = findPropertyByPropertyName(configCopy, "ranger.usersync.enabled");
            const syncSourceProperty = findPropertyByPropertyName(configCopy, "ranger.usersync.source.impl.class");

            if (enableUserSyncProperty && (enableUserSyncProperty.value === "false" || enableUserSyncProperty.value === "No")) {
              property.isVisible = false;
              property.isHidden = true;
              property.errorMessage = "";
            }
            else if (syncSourceProperty && propertyName.includes("ldap") && !syncSourceProperty.value.includes("Ldap")) {
              property.isVisible = false;
              property.isHidden = true;
              property.errorMessage = "";
            }
            else if (propertyName.includes("usersync.group")) {
              const groupPropertiesToHide = [
                "ranger.usersync.group.nameattribute",
                "ranger.usersync.group.objectclass",
                "ranger.usersync.group.searchbase",
                "ranger.usersync.group.memberattributename"
              ];

              if (groupPropertiesToHide.includes(propertyName)) {
                if (!syncSourceProperty || !syncSourceProperty.value.includes("LdapUserGroupBuilder")) {
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
                    foreignKey
                  );
                  if (foreignProperty) {
                    if (property.value === option.displayName) {
                      foreignProperty.isVisible = true;
                      foreignProperty.errorMessage = validateInput(
                        foreignProperty,
                        foreignProperty.value
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
        }
      )
    )
  );

  return configCopy;
}

function addTabNames(configProperties: ConfigPropertiesType, themes: any) {
  const configCopy = cloneDeep(configProperties);

  // Process each theme in the themes array
  if (themes && themes.items) {
    themes.items.forEach((item: any) => {
      const serviceName = item.StackServices.service_name;

      // Process each theme for this service
      item.themes.forEach((themeInfo: any) => {
        const themeData = themeInfo.ThemeInfo.theme_data;
        if (!themeData || !themeData.Theme) return;

        const theme = themeData.Theme;

        if (theme.name !== "default") {
          return;
        }

        // Skip if no configuration or placement data
        if (!theme.configuration || !theme.configuration.placement) return;

        // Create a mapping from config to tab name
        const configToTabMap: Record<string, string> = {};

        // Process layouts to find tab names
        if (theme.configuration.layouts) {
          theme.configuration.layouts.forEach((layout: any) => {
            if (layout.tabs) {
              layout.tabs.forEach((tab: any) => {
                const tabName = tab.name;

                // Process sections in this tab
                if (tab.layout && tab.layout.sections) {
                  tab.layout.sections.forEach((section: any) => {
                    // Each section can have subsections
                    if (section.subsections) {
                      section.subsections.forEach((subsection: any) => {
                        const subsectionName = subsection.name;

                        // Find all configs that belong to this subsection
                        if (theme.configuration.placement.configs) {
                          theme.configuration.placement.configs.forEach(
                            (config: any) => {
                              if (
                                config["subsection-name"] === subsectionName
                              ) {
                                // Map this config to the current tab
                                configToTabMap[config.config] = tabName;
                              }
                            }
                          );
                        }
                      });
                    }
                  });
                }
              });
            }
          });
        }

        // Now assign tab names to properties based on the mapping
        Object.keys(configCopy[serviceName] || {}).forEach((configType) => {
          Object.keys(
            configCopy[serviceName][configType].properties || {}
          ).forEach((propertyName) => {
            const property =
              configCopy[serviceName][configType].properties[propertyName];

            // Create the config key in the format used in the theme (e.g., "hadoop-env/hadoop_pid_dir_prefix")
            const configKey = `${configType}/${propertyName}`;

            // If this config is in our map, assign the tab name
            if (configToTabMap[configKey]) {
              property.tabName = configToTabMap[configKey];
            }
          });
        });
      });
    });
  }

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
        }
      );

      // Then, after all properties are validated, calculate the error count
      configCopy[service][type].errors = getSectionErrorCount(
        configCopy[service][type].properties
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
            const enableUserSyncProperty = findPropertyByPropertyName(configCopy, "ranger.usersync.enabled");
            const syncSourceProperty = findPropertyByPropertyName(configCopy, "ranger.usersync.source.impl.class");

            if (enableUserSyncProperty && (enableUserSyncProperty.value === "false" || enableUserSyncProperty.value === "No")) {
              property.isVisible = false;
              property.isHidden = true;
              property.errorMessage = "";
            }
            else if (syncSourceProperty && propertyName.includes("ldap") && !syncSourceProperty.value.includes("Ldap")) {
              property.isVisible = false;
              property.isHidden = true;
              property.errorMessage = "";
            }
            else if (propertyName.includes("usersync.group")) {
              const groupPropertiesToHide = [
                "ranger.usersync.group.nameattribute",
                "ranger.usersync.group.objectclass",
                "ranger.usersync.group.searchbase",
                "ranger.usersync.group.memberattributename"
              ];

              if (groupPropertiesToHide.includes(propertyName)) {
                if (!syncSourceProperty || !syncSourceProperty.value.includes("LdapUserGroupBuilder")) {
                  property.isVisible = false;
                  property.isHidden = true;
                  property.errorMessage = "";
                }
              }
            }
          }
        }
      )
    )
  );

  let tabErrorcounts: any = {};

  Object.keys(configCopy).forEach((service) =>
    Object.keys(configCopy[service]).forEach((type) => {
      Object.keys(configCopy[service][type].properties).forEach(
        (propertyName) => {
          const property =
            configCopy[service][type].properties[propertyName];

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

          const isPropertyVisible = property.isVisible !== false && !property.isHidden;

          if (isPropertyVisible) {
            // Check for property errors (main property and override values)
            const hasMainPropertyError = property.hasError || property.errorMessage;
            const hasOverrideErrors = property?.overrideValues?.some((o: any) => o.errorMessage && o.value !== null);

            // Increment error count if property has an error
            if (hasMainPropertyError || hasOverrideErrors) {
              tabErrorcounts[service].tabs[tabName] += 1;
              tabErrorcounts[service].total += 1; // Increment the service total as well
            }
          }
        }
      );
    })
  );

  return tabErrorcounts;
}

const getTotalErros = (tabErrors: any): boolean => {
  return Object.keys(tabErrors).every(
    (section) => tabErrors[section].total === 0
  );
};

const evaluateDependsOnForConfig = (
  configProperties: ConfigPropertiesType,
  chosenService: string,
  dependsOn: any,
  services: any
) => {
  if (!dependsOn || !Array.isArray(dependsOn)) return true;

  for (const dependency of dependsOn) {
    const condition = dependency.if;
    const resource = dependency.resource;

    let isConditionMet = false;

    try {
      if (resource?.toLowerCase() === "service") {
        isConditionMet = services.includes(condition);
      } else {
        isConditionMet = calculateConfigCondition(
          condition,
          configProperties,
          chosenService
        );
      }
    } catch (error) {
      isConditionMet = true;
    }

    if (isConditionMet) {
      return dependency.then?.property_value_attributes?.visible ?? true;
    } else {
      return dependency.else?.property_value_attributes?.visible ?? true;
    }
  }

  return true;
};

const calculateConfigCondition = (
  ifStatement: string,
  configProperties: ConfigPropertiesType,
  chosenService: string
): boolean => {
  if (!ifStatement) return true;

  try {
    // Split `if` statement if it has logical operators (exactly like Ember.js)
    const ifStatementRegex = /(&&|\|\|)/;
    const ifConditions = ifStatement.split(ifStatementRegex);
    const allConditionResult: (boolean | string)[] = [];

    ifConditions.forEach((condition) => {
      const trimmedCondition = condition.trim();
      if (trimmedCondition === "&&" || trimmedCondition === "||") {
        allConditionResult.push(trimmedCondition);
      } else {
        // Handle conditions like "${site/config} === value" or just "${site/config}"
        const splitIfCondition = trimmedCondition.split("===");
        const ifCondition = splitIfCondition[0].trim();
        const result = splitIfCondition[1]
          ? splitIfCondition[1].trim()
          : "true";

        let parseIfConditionVal = ifCondition;
        const regex = /\$\{.*?\}/g;
        const configStrings = ifCondition.match(regex);

        if (configStrings) {
          configStrings.forEach((configString) => {
            // Extract config path from ${site/config} format (exactly like Ember.js)
            const configObject = configString
              .substring(2, configString.length - 1)
              .split("/");
            const site = configObject[0]; // e.g., "ranger-kms-site"
            const propertyName = configObject[1]; // e.g., "ranger_service_name"
            const filename = site + ".xml"; // e.g., "ranger-kms-site.xml"

            // Find the config property using Ember.js-style lookup
            // Look for property with matching filename and name
            let configValue = null;
            let foundProperty = null;

            // First try to find in the chosen service
            Object.keys(configProperties[chosenService] || {}).forEach(
              (configType) => {
                Object.keys(
                  configProperties[chosenService][configType].properties || {}
                ).forEach((propName) => {
                  const property =
                    configProperties[chosenService][configType].properties[
                      propName
                    ];
                  // Match by filename and property name (like Ember.js)
                  if (
                    property.fileName === filename &&
                    property.propertyName === propertyName
                  ) {
                    foundProperty = property;
                    configValue = property.value;
                  }
                });
              }
            );

            // If not found, search across all services (fallback)
            if (!foundProperty) {
              const globalProperty = findPropertyByPropertyName(
                configProperties,
                propertyName
              );
              if (globalProperty) {
                foundProperty = globalProperty;
                configValue = globalProperty.value;
              }
            }

            if (configValue !== null) {
              parseIfConditionVal = parseIfConditionVal.replace(
                configString,
                configValue
              );
            }
          });
        }

        // Evaluate the condition exactly like Ember.js does
        const conditionResult =
          eval(JSON.stringify(parseIfConditionVal.trim())) === result.trim();
        allConditionResult.push(conditionResult);
      }
    });

    // Join and evaluate the final result (exactly like Ember.js)
    return Boolean(eval(allConditionResult.join("")));
  } catch (error) {
    // Default to true to avoid hiding properties on evaluation errors
    return true;
  }
};

function updateVisibilityForDependsOn(
  configProperties: ConfigPropertiesType,
  themeData: any,
  configSection: string,
  servicesList: string[]
) {
  let configsCopy = cloneDeep(configProperties);

  if (themeData?.items?.length) {
    themeData.items.forEach((serviceItem: any) => {
      const serviceName = serviceItem?.StackServices?.service_name;

      if (!configsCopy[serviceName]) return;

      serviceItem?.themes?.forEach((item: any) => {
        const themeData = item.ThemeInfo.theme_data.Theme;
        if (themeData.name === configSection) {
          const configByPath: Record<
            string,
            {
              type: string;
              propertyName: string;
              property: any;
            }
          > = {};

          Object.keys(configsCopy[serviceName]).forEach((type) => {
            Object.keys(configsCopy[serviceName][type].properties).forEach(
              (propName) => {
                const property =
                  configsCopy[serviceName][type].properties[propName];
                const configPath = `${type}/${propName}`;
                configByPath[configPath] = {
                  type,
                  propertyName: propName,
                  property,
                };
              }
            );
          });

          themeData.configuration.placement.configs.forEach((config: any) => {
            if (config["depends-on"]) {
              const configPath = config.config;
              const [, propertyName] = configPath.split("/");

              let configInfo = configByPath[configPath];
              let property = configInfo?.property;

              if (!property) {
                const foundProperty = findPropertyByPropertyName(
                  configsCopy,
                  propertyName
                );
                if (foundProperty) {
                  property = foundProperty;
                }
              }

              if (!property) {
                return;
              }

              const isVisible = isArray(config["depends-on"])
                ? evaluateDependsOnForConfig(
                    configsCopy,
                    serviceName,
                    config["depends-on"],
                    servicesList
                  )
                : true;

              property.isVisible = isVisible;
              property.isHidden = !isVisible;

              if (!isVisible) {
                property.errorMessage = "";
              }

            }
          });

          const configMap: Record<
            string,
            {
              subsectionName: string;
              tabName?: string;
              configPath: string;
              type: string;
              propertyName: string;
            }
          > = {};

          themeData.configuration.placement.configs.forEach((config: any) => {
            if (config["subsection-name"]) {
              const configPath = config.config;
              const [type, propertyName] = configPath.split("/");
              configMap[configPath] = {
                subsectionName: config["subsection-name"],
                tabName: config["subsection-tab-name"],
                configPath,
                type,
                propertyName,
              };
            }
          });

          if (themeData.configuration.layouts) {
            themeData.configuration.layouts.forEach((layout: any) => {
              if (layout.tabs) {
                layout.tabs.forEach((tab: any) => {
                  if (tab.layout && tab.layout.sections) {
                    tab.layout.sections.forEach((section: any) => {
                      if (section.subsections) {
                        section.subsections.forEach((subsection: any) => {
                          if (subsection["depends-on"]) {
                            const isSubsectionVisible =
                              evaluateDependsOnForConfig(
                                configsCopy,
                                serviceName,
                                subsection["depends-on"],
                                servicesList
                              );

                            Object.values(configMap).forEach((configInfo) => {
                              if (
                                configInfo.subsectionName === subsection.name
                              ) {
                                let property =
                                  configByPath[configInfo.configPath]?.property;

                                if (
                                  !property &&
                                  configsCopy[serviceName]?.[configInfo.type]
                                    ?.properties?.[configInfo.propertyName]
                                ) {
                                  property =
                                    configsCopy[serviceName][configInfo.type]
                                      .properties[configInfo.propertyName];
                                }

                                if (!property) {
                                  const foundProperty =
                                    findPropertyByPropertyName(
                                      configsCopy,
                                      configInfo.propertyName
                                    );
                                  if (foundProperty) {
                                    property = foundProperty;
                                  }
                                }

                                if (!property) return;

                                const configHasOwnDependsOn =
                                  themeData.configuration.placement.configs.some(
                                    (c: any) =>
                                      c.config === configInfo.configPath &&
                                      c["depends-on"]
                                  );

                                if (
                                  !isSubsectionVisible &&
                                  !configHasOwnDependsOn
                                ) {
                                  property.isVisible = false;
                                  property.isHidden = true;
                                } else if (
                                  isSubsectionVisible &&
                                  !configHasOwnDependsOn
                                ) {
                                  property.isVisible = true;
                                  property.isHidden = false;
                                }
                              }
                            });
                          }

                          if (subsection["subsection-tabs"]) {
                            subsection["subsection-tabs"].forEach(
                              (subsectionTab: any) => {
                                if (subsectionTab["depends-on"]) {
                                  const isTabVisible =
                                    evaluateDependsOnForConfig(
                                      configsCopy,
                                      serviceName,
                                      subsectionTab["depends-on"],
                                      servicesList
                                    );

                                  Object.values(configMap).forEach(
                                    (configInfo) => {
                                      if (
                                        configInfo.subsectionName ===
                                          subsection.name &&
                                        configInfo.tabName ===
                                          subsectionTab.name
                                      ) {
                                        let property =
                                          configByPath[configInfo.configPath]
                                            ?.property;

                                        if (
                                          !property &&
                                          configsCopy[serviceName]?.[
                                            configInfo.type
                                          ]?.properties?.[
                                            configInfo.propertyName
                                          ]
                                        ) {
                                          property =
                                            configsCopy[serviceName][
                                              configInfo.type
                                            ].properties[
                                              configInfo.propertyName
                                            ];
                                        }

                                        if (!property) {
                                          const foundProperty =
                                            findPropertyByPropertyName(
                                              configsCopy,
                                              configInfo.propertyName
                                            );
                                          if (foundProperty) {
                                            property = foundProperty;
                                          }
                                        }

                                        if (!property) return;

                                        const configHasOwnDependsOn =
                                          themeData.configuration.placement.configs.some(
                                            (c: any) =>
                                              c.config ===
                                                configInfo.configPath &&
                                              c["depends-on"]
                                          );

                                        if (
                                          !isTabVisible &&
                                          !configHasOwnDependsOn
                                        ) {
                                          property.isVisible = false;
                                          property.isHidden = true;
                                        } else if (
                                          isTabVisible &&
                                          !configHasOwnDependsOn
                                        ) {
                                          property.isVisible = true;
                                          property.isHidden = false;
                                        }
                                      }
                                    }
                                  );
                                }
                              }
                            );
                          }
                        });
                      }
                    });
                  }
                });
              }
            });
          }
        }
      });
    });
  }

  return configsCopy;
}

const getConfigTypesInfoFromService = (
  stackService: StackServices
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
  stackData: StackServicesRoot
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
    (item) => item.StackServices.service_name === serviceName
  );

  if (!serviceItem) {
    return false;
  }

  const configTypesInfo = getConfigTypesInfoFromService(
    serviceItem.StackServices
  );

  return !!configTypesInfo.supportsFinal.find((configType) =>
    filename.startsWith(configType)
  );
};

const shouldSupportAddingForbidden = (
  serviceName: string,
  filename: string,
  stackData: StackServicesRoot
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
    (item) => item.StackServices.service_name === serviceName
  );

  if (!serviceItem) {
    return false;
  }

  const configTypesInfo = getConfigTypesInfoFromService(
    serviceItem.StackServices
  );
  return !!configTypesInfo.supportsAddingForbidden.find((configType) =>
    filename.startsWith(configType)
  );
};


const kerberosIdentitiesDescription = (propertyName: string): string => {
  const identity = kerberosIdentities.find((id) => id.name === propertyName);
  return identity ? identity.displayName : "";
};

const setPropertyIsEditable = (
  configProperties: ConfigPropertiesType,
  selectedConfigGroup: { isDefault: boolean },
  isKerberosEnabled: boolean
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
        }
      );
    });
  });

  return configCopy;
};

const hideComponentConfigsBasedOnAvailability = (
  configProperties: ConfigPropertiesType,
  allServiceModels: any
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
          }
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
        }
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
