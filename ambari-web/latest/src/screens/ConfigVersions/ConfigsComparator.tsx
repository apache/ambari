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

import { useEffect, useState } from "react";
import ConfigsApi from "../../api/configsApi";
import { cloneDeep, get, isEmpty, isObject } from "lodash";
import {
  ConfigPropertiesType,
  SubsectionPropertiesType,
  TabType,
  ThemeType,
} from "../CommonConfigs/types";
import { formatPropertyValue } from "../CommonConfigs/ConfigUtils";
import { ambari_metrics_properties } from "../../data/configs/services/ambari_metrics_properties";
import { hbase_properties } from "../../data/configs/services/hbase_properties";
import { hdfs_properties } from "../../data/configs/services/hdfs_properties";
import { hive_properties } from "../../data/configs/services/hive_properties";
import { kerberos_properties } from "../../data/configs/services/kerberos_properties";
import { kyuubi_properties } from "../../data/configs/services/kyuubi_properties";
import { mapreduce2_properties } from "../../data/configs/services/mapreduce2_properties";
import { ranger_properties } from "../../data/configs/services/ranger_properties";
import { sqoop_properties } from "../../data/configs/services/sqoop_properties";
import { tez_properties } from "../../data/configs/services/tez_properties";
import { yarn_properties } from "../../data/configs/services/yarn_properties";
import { zookeeper_properties } from "../../data/configs/services/zookeeper_properties";
import { Tab, Accordion, Tabs, Form, InputGroup } from "react-bootstrap";
import Spinner from "../../components/Spinner";
import ComparatorFilter from "./ComparatorFilter";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faSearch, faLock } from "@fortawesome/free-solid-svg-icons";
import { FilterLevels } from "./constants";

export default function ConfigsComparator({
  version1,
  version2,
  clusterName,
  serviceName,
  configs,
  themeData,
  currentVersion,
}: {
  version1: string;
  version2: string;
  defaultVersion: string;
  clusterName: string;
  serviceName: string;
  configs: any;
  themeData: any;
  currentVersion: any;
}) {
  const [firstVersion, setFirstVersion] = useState<string>(version1);
  const [secondVersion, setSecondVersion] = useState<string>(version2);
  const [selectedFilters, setSelectedFilters] = useState<any[]>([
    { label: "Changed Properties", value: FilterLevels.CHANGED },
  ]);
  const [searchString, setSearchString] = useState<string>("");
  const [firstVersionProperties, setFirstVersionProperties] =
    useState<any>(null);
  const [secondVersionProperties, setSecondVersionProperties] =
    useState<any>(null);
  const [configProperties, setConfigProperties] =
    useState<ConfigPropertiesType>({});
  const [theme, setTheme] = useState<ThemeType>({});
  const [transformedData, setTransformedData] = useState<any>({});
  const [configGroupsData, setConfigGroupsData] = useState<any[]>([]);


  // Tab filtering logic based on Ember.js implementation
  const getActiveServiceTabs = (serviceName: string, themeData: ThemeType) => {
    if (!serviceName) {
      return [];
    }

    // If no theme data exists for the service, return just the Advanced tab
    if (!themeData[serviceName]) {
      console.log(`No theme data found for service: ${serviceName}, returning Advanced tab only`);
      return [{
        key: "Advanced",
        name: "Advanced",
        displayName: "Advanced",
      }];
    }

    const serviceTheme = themeData[serviceName];
    const tabs = serviceTheme.tabs;

    // Filter tabs based on Ember.js logic:
    // 1. Service-specific tabs (serviceName matches)
    // 2. Non-categorized tabs (exclude special themed tabs)
    // 3. Non-hidden tabs

    const activeServiceTabs = Object.keys(tabs).filter((tabKey) => {
      const tab = tabs[tabKey];

      // Check if tab is categorized (special themed tabs to exclude)
      const isCategorized = isTabCategorized(tab);

      // Return tabs that are NOT categorized
      return !isCategorized;
    });

    return activeServiceTabs.map((tabKey) => ({
      key: tabKey,
      ...tabs[tabKey],
    }));
  };

  // Determine if tab is categorized (should be excluded from main config view)
  // Based on Ember.js logic: !isAdvanced && themeName !== 'default'
  const isTabCategorized = (tab: any) => {
    // In React implementation, we need to check if this is a special themed tab
    // that should be handled separately (like database, directories, credentials)

    // Advanced tabs are never categorized
    if (tab.name === "Advanced" || tab.displayName === "Advanced") {
      return false;
    }

    // Check for special theme names that should be categorized
    // These correspond to Ember.js categorized tabs
    const categorizedThemes = ["database", "directories", "credentials"];

    // For now, we'll consider all non-Advanced tabs as non-categorized
    // unless they match specific patterns that indicate they're special themed tabs
    const tabName = tab.name?.toLowerCase() || "";
    const displayName = tab.displayName?.toLowerCase() || "";

    return categorizedThemes.some(
      (theme) => tabName.includes(theme) || displayName.includes(theme)
    );
  };

  // Create proper display name from subsection internal name
  const createDisplayNameFromSubsectionName = (subsectionName: string) => {
    // Convert names like "subsection-datanode-col1" to "Datanode"
    // Extract the meaningful part and capitalize it
    if (subsectionName.includes("subsection-")) {
      const parts = subsectionName.split("-");
      if (parts.length >= 2) {
        // Take the second part (after 'subsection-') and capitalize it
        const meaningfulPart = parts[1];
        return (
          meaningfulPart.charAt(0).toUpperCase() +
          meaningfulPart.slice(1).toLowerCase()
        );
      }
    }

    // For other patterns, try to extract meaningful words and capitalize them
    const words = subsectionName
      .split(/[-_]/)
      .filter(
        (word) =>
          word && !["subsection", "col", "row"].includes(word.toLowerCase())
      );

    if (words.length > 0) {
      return words
        .map(
          (word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
        )
        .join(" ");
    }

    // Fallback: just capitalize the whole name
    return (
      subsectionName.charAt(0).toUpperCase() +
      subsectionName.slice(1).toLowerCase()
    );
  };

  // Get filtered tabs for the current service
  const activeServiceTabs = getActiveServiceTabs(serviceName, theme);

  useEffect(() => {
    setFirstVersion(version1);
  }, [version1]);

  useEffect(() => {
    setSecondVersion(version2);
  }, [version2]);

  const propertiesFileMap: { [key: string]: any } = {
    HDFS: hdfs_properties,
    YARN: yarn_properties,
    HIVE: hive_properties,
    HBASE: hbase_properties,
    RANGER: ranger_properties,
    MAPREDUCE2: mapreduce2_properties,
    TEZ: tez_properties,
    ZOOKEEPER: zookeeper_properties,
    AMBARI_METRICS: ambari_metrics_properties,
    KERBEROS: kerberos_properties,
    KYUUBI: kyuubi_properties,
    SQOOP: sqoop_properties,
  };

  useEffect(() => {
    if (firstVersionProperties && secondVersionProperties) {
      const config1 = getConfigPropertiesWithProperties(
        {},
        firstVersionProperties,
        "value1"
      );

      const updatedConfigProperties = getConfigPropertiesWithProperties(
        config1,
        secondVersionProperties,
        "value2"
      );
      setConfigProperties(updatedConfigProperties);
    }
  }, [firstVersionProperties, secondVersionProperties]);

  // Fetch config groups data for override detection
  useEffect(() => {
    async function fetchConfigGroups() {
      if (clusterName && serviceName) {
        try {
          const response = await ConfigsApi.getConfigGroups(
            clusterName,
            serviceName
          );
          setConfigGroupsData(response.items || []);
        } catch (error) {
          console.error("Error fetching config groups:", error);
          setConfigGroupsData([]);
        }
      }
    }

    fetchConfigGroups();
  }, [clusterName, serviceName]);

  // New API call for comparing multiple versions - similar to Ember.js ConfigsComparator
  useEffect(() => {
    async function getCompareVersionConfigs() {
      if (firstVersion && secondVersion) {
        try {
          const response = await ConfigsApi.getMultipleVersionConfigValues(
            clusterName,
            serviceName,
            firstVersion,
            secondVersion
          );

          // Process the response similar to Ember.js initCompareConfig
          processCompareVersionData(response);
        } catch (error) {
          console.error("Error fetching compare version configs:", error);
        }
      }
    }

    getCompareVersionConfigs();
  }, [firstVersion, secondVersion, clusterName, serviceName, theme]);

  // Process the compare version data and transform into tab/category structure
  const processCompareVersionData = (response: any) => {
    console.log('Processing compare version data:', response);
    
    const serviceVersionMap: any = {};
    const compareVersionNumber = firstVersion;
    const selectedVersionNumber = secondVersion;

    // Initialize version maps
    serviceVersionMap[compareVersionNumber] = {};
    serviceVersionMap[selectedVersionNumber] = {};

    // Process each version's configurations
    response.items?.forEach((item: any) => {
      const versionNumber = item.service_config_version.toString();
      console.log(`Processing version ${versionNumber} with ${item.configurations?.length || 0} configurations`);

      item.configurations?.forEach((configuration: any) => {
        const configType = configuration.type;
        const properties = configuration.properties || {};
        const propertiesAttributes = configuration.properties_attributes || {};
        
        console.log(`Processing config type: ${configType} with ${Object.keys(properties).length} properties`);

        // Process each property in the configuration (first pass - create config objects)
        Object.keys(properties).forEach((propertyName: string) => {
          const configKey = `${propertyName}-${configType}`;

          serviceVersionMap[versionNumber][configKey] = {
            name: propertyName,
            value: properties[propertyName],
            type: configType,
            tag: configuration.tag,
            version: configuration.version,
            service_config_version: versionNumber,
            filename: `${configType}.xml`,
            isFinal: false, // Default to false, will be set in second pass
          };
        });

        // Process final attributes (second pass - same as Ember.js logic)
        if (propertiesAttributes && propertiesAttributes.final) {
          Object.keys(propertiesAttributes.final).forEach(
            (finalPropertyName) => {
              const configKey = `${finalPropertyName}-${configType}`;
              const config = serviceVersionMap[versionNumber][configKey];
              if (config) {
                config.isFinal =
                  propertiesAttributes.final[finalPropertyName] === "true";

                // Debug logging for final properties
                console.log(
                  `Final property detected: ${finalPropertyName} in ${configType}, isFinal: ${config.isFinal}`
                );
              }
            }
          );
        }
      });
    });

    console.log('Service version map:', serviceVersionMap);

    // Transform data into tab/category structure and store in state
    const processedTransformedData =
      transformDataByTabsAndCategories(serviceVersionMap);
    console.log('Processed transformed data:', processedTransformedData);
    setTransformedData(processedTransformedData);

    // Store the processed data for comparison (don't overwrite, just store the response)
    const firstVersionData =
      response.items?.filter(
        (item: any) =>
          item.service_config_version.toString() === compareVersionNumber
      ) || [];

    const secondVersionData =
      response.items?.filter(
        (item: any) =>
          item.service_config_version.toString() === selectedVersionNumber
      ) || [];

    console.log(`First version data: ${firstVersionData.length} items, Second version data: ${secondVersionData.length} items`);

    setFirstVersionProperties({ items: firstVersionData });
    setSecondVersionProperties({ items: secondVersionData });
    
    // For comparison view, we don't need the complex configProperties processing
    // Just set a minimal configProperties to bypass the loading spinner
    setConfigProperties({
      [serviceName]: {
        "placeholder": {
          errors: 0,
          properties: {}
        }
      }
    });
  };

  // Transform data into the requested structure: [tabName]: { [categoryName]: { propertyValue pairs } }
  const transformDataByTabsAndCategories = (serviceVersionMap: any) => {
    const transformedData: any = {};

    // Get theme data for the current service
    const serviceTheme = theme[serviceName];
    
    // If no theme data exists for the service, create a minimal theme with just Advanced tab
    if (!serviceTheme) {
      console.log(`No theme data found for service: ${serviceName}, creating minimal theme`);
      // Initialize with just Advanced tab for services without theme data
      transformedData["Advanced"] = {};
    } else {
      // Initialize structure for each tab from theme
      Object.keys(serviceTheme.tabs).forEach((tabName) => {
        transformedData[tabName] = {};
      });
    }

    // Process each version's data
    Object.keys(serviceVersionMap).forEach((versionNumber) => {
      const versionData = serviceVersionMap[versionNumber];

      Object.keys(versionData).forEach((configKey) => {
        const configData = versionData[configKey];
        const propertyName = configData.name;
        const configType = configData.type;

        // Determine which tab and category this property belongs to
        const { tabName, categoryName } = determineTabAndCategory(
          propertyName,
          configType,
          serviceTheme
        );

        // Initialize tab and category if they don't exist
        if (!transformedData[tabName]) {
          transformedData[tabName] = {};
        }
        if (!transformedData[tabName][categoryName]) {
          transformedData[tabName][categoryName] = {};
        }

        // Add property to the appropriate tab/category
        if (!transformedData[tabName][categoryName][propertyName]) {
          transformedData[tabName][categoryName][propertyName] = {};
        }

        // Store version-specific data
        const versionKey = `version_${versionNumber}`;
        transformedData[tabName][categoryName][propertyName][versionKey] = {
          value: configData.value,
          isFinal: configData.isFinal,
          type: configData.type,
          filename: configData.filename,
        };
      });
    });

    return transformedData;
  };

  // Determine which tab and category a property belongs to
  const determineTabAndCategory = (
    propertyName: string,
    configType: string,
    serviceTheme: any
  ) => {
    let tabName = "Advanced"; // Default tab
    let categoryName = getDefaultCategoryName(propertyName, configType); // Default category based on Ember.js logic

    // First, check if property is defined in theme subsection properties
    // If it's in theme, it goes to the themed tab, otherwise it goes to Advanced
    let foundInTheme = false;

    if (serviceTheme && serviceTheme.subsectionProperties) {
      Object.keys(serviceTheme.subsectionProperties).forEach(
        (subsectionName) => {
          const subsection = serviceTheme.subsectionProperties[subsectionName];
          const propertyExists = subsection.properties?.some((prop: any) => {
            const [propConfigType, propName] = prop.config?.split("/") || [];
            return propName === propertyName && propConfigType === configType;
          });

          if (propertyExists) {
            foundInTheme = true;
            // Find which tab this subsection belongs to
            if (serviceTheme.tabs) {
              Object.keys(serviceTheme.tabs).forEach((currentTabName) => {
                const tab = serviceTheme.tabs[currentTabName];
                if (tab.sections) {
                  Object.values(tab.sections).forEach((section: any) => {
                    if (section.subsections) {
                      Object.values(section.subsections).forEach(
                        (subsection: any) => {
                          if (subsection.name === subsectionName) {
                            tabName = currentTabName;
                            // Use displayName first, fallback to name if displayName doesn't exist
                            categoryName =
                              subsection.displayName || subsection.name;
                          }
                        }
                      );
                    }
                  });
                }
              });
            }
          }
        }
      );
    }

    // If NOT found in theme, check properties file map for category information
    // Based on Ember.js logic: properties not in theme go to Advanced tab with their category
    if (!foundInTheme && propertiesFileMap[serviceName]) {
      const propertyInfo = propertiesFileMap[serviceName].find(
        (prop: any) =>
          prop.name === propertyName && prop.filename === `${configType}.xml`
      );

      if (propertyInfo && propertyInfo.category) {
        // All properties not in theme go to Advanced tab, but keep their category name
        tabName = "Advanced";
        categoryName = propertyInfo.category;
      }
    }

    return { tabName, categoryName };
  };

  // Get default category name based on Ember.js logic
  // Ember.js: (stackConfigProperty ? 'Advanced ' : 'Custom ') + configType
  const getDefaultCategoryName = (propertyName: string, configType: string) => {
    // Check if property is defined in stack (configs collection)
    const isDefinedInStack = isPropertyDefinedInStack(propertyName, configType);

    // Follow Ember.js logic: stackConfigProperty ? 'Advanced ' : 'Custom '
    return (isDefinedInStack ? "Advanced " : "Custom ") + configType;
  };

  // Check if property is defined in stack configurations
  const isPropertyDefinedInStack = (
    propertyName: string,
    configType: string
  ) => {
    // Check if property exists in the configs collection (stack-defined properties)
    if (configs?.items) {
      const stackProperty = configs.items.some((service: any) =>
        service.configurations?.some(
          (config: any) =>
            config.StackConfigurations.property_name === propertyName &&
            config.StackConfigurations.type === `${configType}.xml`
        )
      );

      if (stackProperty) {
        return true;
      }
    }

    // Also check properties file map as these are considered stack-defined
    if (propertiesFileMap[serviceName]) {
      const propertyInfo = propertiesFileMap[serviceName].find(
        (prop: any) =>
          prop.name === propertyName && prop.filename === `${configType}.xml`
      );

      if (propertyInfo) {
        return true;
      }
    }

    // If not found in either, it's a custom/user property
    return false;
  };

  useEffect(() => {
    setTheme(getTheme(themeData));
  }, [version1, version2, themeData]);

  const getConfigPropertiesWithProperties = (
    configPropertiesCopy: ConfigPropertiesType,
    propertyValues: any,
    valueKey: string
  ) => {
    let updatedConfigProperties: ConfigPropertiesType = {};

    configs?.items?.forEach((service: any) => {
      service.configurations?.forEach((config: any) => {
        const fileName = config.StackConfigurations.type as string;
        const configType = fileName.slice(0, -4);
        const propertyName = config.StackConfigurations.property_name as string;
        const serviceName = config.StackConfigurations.service_name;
        const propertyType = config.StackConfigurations.property_type;

        if (!configPropertiesCopy[serviceName]) {
          configPropertiesCopy[serviceName] = {};
        }
        if (!configPropertiesCopy[serviceName][configType]) {
          configPropertiesCopy[serviceName][configType] = {
            errors: 0,
            properties: {},
          };
        }

        configPropertiesCopy[serviceName][configType].properties[propertyName] =
          {
            ...(configPropertiesCopy[serviceName][configType].properties[
              propertyName
            ] || {}),
            propertyName: propertyName,
            ...(config.StackConfigurations.property_display_name && {
              propertyDisplayname:
                config.StackConfigurations.property_display_name,
            }),
            propertyValue: config.StackConfigurations.property_value,
            propertyAttributes:
              config.StackConfigurations.property_value_attributes,
            previousValue: config.StackConfigurations.property_value,
            [valueKey]: config.StackConfigurations.property_value,
            final: config.StackConfigurations.final
              ? config.StackConfigurations.final
              : "",
            fileName: fileName,
            propertyType: propertyType ? propertyType : [],
            type: configType,
            isEditable: false,
            isVisible: true,
          };

        if (
          configPropertiesCopy[serviceName][configType].properties[propertyName]
            .propertyAttributes.type == "password"
        ) {
          configPropertiesCopy[serviceName][configType].properties[
            propertyName
          ] = {
            ...configPropertiesCopy[serviceName][configType].properties[
              propertyName
            ],
            confirmPassword: config.StackConfigurations.property_value,
          };
        }

        if (
          propertyType &&
          (propertyType.includes("USER") || propertyType.includes("GROUP"))
        ) {
          delete configPropertiesCopy[serviceName][configType].properties[
            propertyName
          ];
        }
      });
    });

    if (isEmpty(configPropertiesCopy)) {
      configPropertiesCopy?.[serviceName] &&
        isObject(configPropertiesCopy?.[serviceName]) &&
        Object.keys(configPropertiesCopy?.[serviceName])?.map(
          (configType: string) => {
            if (!!!configType.endsWith("env")) {
              configPropertiesCopy[serviceName]["Custom " + configType] = {
                errors: 0,
                properties: {},
                displayName: "Custom " + configType,
              };
            }
          }
        );
    }

    if (!isEmpty(propertyValues)) {
      const defaultItems = propertyValues?.items?.filter(
        (item: any) => item.group_name === "Default"
      );

      defaultItems?.forEach((item: any) => {
        item?.configurations?.forEach((config: any) => {
          const type = config.type;
          const properties = config.properties;
          const serviceName = get(item, "service_name", "");

          Object.keys(properties).forEach((propertyName: string) => {
            if (configPropertiesCopy[serviceName]?.[type]) {
              if (
                configPropertiesCopy[serviceName][type]?.properties[
                  propertyName
                ]
              ) {
                configPropertiesCopy[serviceName][type].properties[
                  propertyName
                ][valueKey] = formatPropertyValue(
                  configPropertiesCopy[serviceName][type]?.properties[
                    propertyName
                  ],
                  properties[propertyName]
                );

                if (
                  configPropertiesCopy[serviceName][type].properties[
                    propertyName
                  ].propertyAttributes.type === "password"
                ) {
                  configPropertiesCopy[serviceName][type].properties[
                    propertyName
                  ].confirmPassword =
                    configPropertiesCopy[serviceName][type].properties[
                      propertyName
                    ][valueKey];
                }
              } else {
                if (configPropertiesCopy[serviceName]["Custom " + type]) {
                  configPropertiesCopy[serviceName][
                    "Custom " + type
                  ].properties[propertyName] = {
                    ...(configPropertiesCopy[serviceName]["Custom " + type]
                      .properties[propertyName] || {}),
                    propertyName: propertyName,
                    propertyDisplayname: propertyName,
                    propertyValue: properties[propertyName],
                    propertyAttributes: {},
                    previousValue: properties[propertyName],
                    [valueKey]: properties[propertyName],
                    final: "",
                    fileName: type + ".xml",
                    propertyType: [],
                    type: type,
                    isEditable: false,
                  };
                }
              }
            }
          });
        });
      });
    }

    Object.keys(propertiesFileMap).map((service: string) => {
      if (configPropertiesCopy[service]) {
        propertiesFileMap[service].forEach((property: any) => {
          const { serviceName, filename, name, category } = property;
          if (!category) {
            return;
          }
          const configType = filename.slice(0, -4);

          if (configPropertiesCopy[serviceName][configType]?.properties[name]) {
            if (!category.includes("Advanced")) {
              if (!updatedConfigProperties[serviceName]) {
                updatedConfigProperties[serviceName] = {};
              }
              if (!updatedConfigProperties[serviceName][category]) {
                updatedConfigProperties[serviceName][category] = {
                  errors: 0,
                  properties: {},
                };
              }

              updatedConfigProperties[serviceName][category].properties[name] =
                configPropertiesCopy[serviceName][configType].properties[name];

              delete configPropertiesCopy[serviceName][configType].properties[
                name
              ];
            }
          }
        });
      }
    });

    Object.keys(configPropertiesCopy).forEach((serviceName) => {
      if (!updatedConfigProperties[serviceName]) {
        updatedConfigProperties[serviceName] = {};
      }
      Object.keys(configPropertiesCopy[serviceName]).forEach((configType) => {
        if (!updatedConfigProperties[serviceName][configType]) {
          updatedConfigProperties[serviceName][configType] = {
            errors: 0,
            properties: {},
            displayName: !configType.includes("Custom")
              ? "Advanced " + configType
              : configType,
          };
        }
        Object.keys(
          configPropertiesCopy[serviceName][configType].properties
        ).forEach((propertyName) => {
          updatedConfigProperties[serviceName][configType].properties[
            propertyName
          ] =
            configPropertiesCopy[serviceName][configType].properties[
              propertyName
            ];
        });
      });
    });

    return updatedConfigProperties;
  };

  const getTheme = (themeData: any) => {
    let theme: ThemeType = {};
    let reqServices = new Set<string>();
    const configSection = "default";
    themeData?.items?.forEach((serviceItem: any) => {
      const serviceName = serviceItem?.StackServices?.service_name;
      let tabsData: TabType = {};
      serviceItem?.themes?.forEach((item: any) => {
        const themeData = item.ThemeInfo.theme_data.Theme;
        if (themeData.name === configSection) {
          reqServices.add(serviceName);
          themeData.configuration.layouts.forEach((layout: any) => {
            layout.tabs.forEach((tab: any) => {
              tabsData[tab.name] = {
                name: tab.name,
                displayName: tab["display-name"],
                tabColumns: tab.layout["tab-columns"],
                tabRows: tab.layout["tab-rows"],
                sections: {},
              };
              tab.layout.sections.forEach((section: any) => {
                tabsData[tab.name].sections[section.name] = {
                  name: section.name,
                  displayName: section["display-name"],
                  rowSpan: section["row-span"],
                  columnSpan: section["column-span"],
                  rowIndex: section["row-index"],
                  columnIndex: section["col-index"],
                  sectionRows: section["section-rows"],
                  sectionColumns: section["section-columns"],
                  subsections: {},
                };
                section.subsections.forEach((subsection: any) => {
                  // Create proper displayName from subsection name if display-name is missing
                  const properDisplayName =
                    subsection["display-name"] ||
                    createDisplayNameFromSubsectionName(subsection.name);

                  tabsData[tab.name].sections[section.name].subsections[
                    subsection.name
                  ] = {
                    name: subsection.name,
                    displayName: properDisplayName,
                    rowSpan: subsection["row-span"],
                    columnSpan: subsection["column-span"],
                    rowIndex: subsection["row-index"],
                    columnIndex: subsection["column-index"],
                    ...(subsection["depends-on"] && {
                      "depends-on": subsection["depends-on"],
                    }),
                    ...(subsection["subsection-tabs"] && {
                      subsectionTabs: subsection["subsection-tabs"],
                    }),
                  };
                });
              });
            });
          });
        }
      });

      if (configSection === "default") {
        tabsData["Advanced"] = {
          name: "Advanced",
          displayName: "Advanced",
          sections: {},
        };
      }

      const propertiesData: SubsectionPropertiesType = {};

      serviceItem?.themes?.forEach((item: any) => {
        const themeData = item.ThemeInfo.theme_data.Theme;
        themeData.configuration.placement.configs.forEach((config: any) => {
          if (!propertiesData[config["subsection-name"]]) {
            propertiesData[config["subsection-name"]] = { properties: [] };
          }
          if (
            !propertiesData[config["subsection-name"]].properties.some(
              (existingConfig: any) =>
                existingConfig["config"] === config["config"]
            )
          ) {
            propertiesData[config["subsection-name"]].properties.push(config);
          }
        });
      });

      const propertyWidgets: any = {};

      serviceItem?.themes?.forEach((item: any) => {
        const themeData = item.ThemeInfo.theme_data.Theme;
        themeData.configuration.widgets.map((widget: any) => {
          const propertyName = widget.config.split("/")[1];
          propertyWidgets[propertyName] = widget;
        });
      });

      theme = {
        ...theme,
        [serviceName]: {
          tabs: sortTabs(tabsData),
          subsectionProperties: propertiesData,
          widgets: propertyWidgets,
        },
      };
    });
    return theme;
  };

  const sortTabs = (tabsData: TabType) => {
    const sortedTabs = cloneDeep(tabsData);

    Object.keys(sortedTabs).forEach((tabKey) => {
      const tab = sortedTabs[tabKey];

      if (tab.sections) {
        tab.sections = Object.values(tab.sections).sort((a: any, b: any) => {
          if (a.rowIndex === b.rowIndex) {
            return a.columnIndex - b.columnIndex;
          }
          return a.rowIndex - b.rowIndex;
        });

        tab.sections.forEach((section: any) => {
          if (section.subsections) {
            section.subsections = Object.values(section.subsections).sort(
              (a: any, b: any) => {
                if (a.rowIndex === b.rowIndex) {
                  return a.columnIndex - b.columnIndex;
                }
                return a.rowIndex - b.rowIndex;
              }
            );
          }
        });
      }
    });

    return sortedTabs;
  };

  // Check if property should be hidden from comparison (based on Ember.js logic)
  const shouldHideProperty = (propertyName: string, configType: string) => {
    // 1. Check if it's a password field (Ember.js: notShownTypes = ['password'])
    if (isPasswordProperty(propertyName, configType)) {
      return true;
    }

    // 2. Check if property is marked as not visible in stack configs
    if (configs?.items) {
      const stackProperty = configs.items.find((service: any) =>
        service.configurations?.find(
          (config: any) =>
            config.StackConfigurations.property_name === propertyName &&
            config.StackConfigurations.type === `${configType}.xml`
        )
      );

      if (stackProperty) {
        const configData = stackProperty.configurations.find(
          (config: any) =>
            config.StackConfigurations.property_name === propertyName &&
            config.StackConfigurations.type === `${configType}.xml`
        );

        // Check if property is marked as not visible
        if (
          configData?.StackConfigurations?.property_value_attributes
            ?.visible === false
        ) {
          return true;
        }
      }
    }

    // 3. Check properties file map for visibility
    if (propertiesFileMap[serviceName]) {
      const propertyInfo = propertiesFileMap[serviceName].find(
        (prop: any) =>
          prop.name === propertyName && prop.filename === `${configType}.xml`
      );

      if (propertyInfo && propertyInfo.isVisible === false) {
        return true;
      }
    }

    return false;
  };

  // Check if property is a password field
  const isPasswordProperty = (propertyName: string, configType: string) => {
    // Check stack configurations for password displayType
    if (configs?.items) {
      const stackProperty = configs.items.find((service: any) =>
        service.configurations?.find(
          (config: any) =>
            config.StackConfigurations.property_name === propertyName &&
            config.StackConfigurations.type === `${configType}.xml`
        )
      );

      if (stackProperty) {
        const configData = stackProperty.configurations.find(
          (config: any) =>
            config.StackConfigurations.property_name === propertyName &&
            config.StackConfigurations.type === `${configType}.xml`
        );

        if (
          configData?.StackConfigurations?.property_value_attributes?.type ===
          "password"
        ) {
          return true;
        }
      }
    }

    // Check for common password property name patterns
    const passwordPatterns = [
      /password/i,
      /passwd/i,
      /secret/i,
      /keystore\.password/i,
      /truststore\.password/i,
    ];

    return passwordPatterns.some((pattern) => pattern.test(propertyName));
  };

  // Get display name for property (similar to Ember.js logic)
  const getPropertyDisplayName = (propertyName: string, configType: string) => {
    // Check stack configurations for property_display_name
    if (configs?.items) {
      const stackProperty = configs.items.find((service: any) =>
        service.configurations?.find(
          (config: any) =>
            config.StackConfigurations.property_name === propertyName &&
            config.StackConfigurations.type === `${configType}.xml`
        )
      );

      if (stackProperty) {
        const configData = stackProperty.configurations.find(
          (config: any) =>
            config.StackConfigurations.property_name === propertyName &&
            config.StackConfigurations.type === `${configType}.xml`
        );

        // Use property_display_name if available, fallback to property_name
        const displayName =
          configData?.StackConfigurations?.property_display_name;
        if (displayName) {
          return displayName;
        }
      }
    }

    // Check properties file map for display name
    if (propertiesFileMap[serviceName]) {
      const propertyInfo = propertiesFileMap[serviceName].find(
        (prop: any) =>
          prop.name === propertyName && prop.filename === `${configType}.xml`
      );

      if (propertyInfo && propertyInfo.displayName) {
        return propertyInfo.displayName;
      }
    }

    // Fallback to property name (same as Ember.js logic)
    return propertyName;
  };

  // Format word break similar to Ember.js formatWordBreak helper
  const formatWordBreak = (text: string) => {
    // Add word break opportunities for long property names
    return text.replace(/([._-])/g, "$1\u200B"); // Add zero-width space after separators
  };

  // Check if property supports final attribute (similar to Ember.js supportsFinal)
  const shouldShowFinalIcon = (configType: string) => {
    // Based on Ember.js logic: check if config type supports final attribute
    // Most config types support final, but some don't (like MISC, Cluster)
    const unsupportedTypes = ["alert_notification"]; // Add more if needed
    return !unsupportedTypes.includes(configType);
  };

  // Get category display name (similar to Ember.js ServiceConfigCategory displayName)
  const getCategoryDisplayName = (categoryName: string) => {
    // Handle Advanced and Custom categories with proper formatting (like Ember.js)
    if (categoryName.startsWith("Advanced ")) {
      const configType = categoryName.replace("Advanced ", "");
      return `Advanced ${configType}`; // Ember.js: Em.I18n.t('common.advanced') + " " + type
    }

    if (categoryName.startsWith("Custom ")) {
      const configType = categoryName.replace("Custom ", "");
      return `Custom ${configType}`; // Ember.js: Em.I18n.t('common.custom') + " " + type
    }

    // Based on Ember.js App.StackService.configCategories hardcoded mapping
    const categoryDisplayNameMap: { [key: string]: string } = {
      // HDFS categories
      NAMENODE: "NameNode",
      SECONDARY_NAMENODE: "Secondary NameNode",
      DATANODE: "DataNode",
      NFS_GATEWAY: "NFS Gateway",
      General: "General",

      // YARN categories
      RESOURCEMANAGER: "Resource Manager",
      NODEMANAGER: "Node Manager",
      APP_TIMELINE_SERVER: "Application Timeline Server",
      ResourceTypes: "Resource Types",
      FaultTolerance: "Fault Tolerance",
      Isolation: "Isolation",
      CapacityScheduler: "Scheduler",
      ContainerExecutor: "Container Executor",
      Registry: "Registry",

      // MAPREDUCE2 categories
      HISTORYSERVER: "History Server",

      // HIVE categories
      HIVE_METASTORE: "Hive Metastore",
      WEBHCAT_SERVER: "WebHCat Server",
      Performance: "Performance",
      HIVE_SERVER2: "Hive Server2",
      HIVE_CLIENT: "Hive Client",

      // HBASE categories
      HBASE_MASTER: "HBase Master",
      HBASE_REGIONSERVER: "RegionServer",

      // ZOOKEEPER categories
      ZOOKEEPER_SERVER: "ZooKeeper Server",

      // OOZIE categories
      OOZIE_SERVER: "Oozie Server",
      "Falcon - Oozie integration": "Falcon - Oozie integration",

      // FALCON categories
      FALCON_SERVER: "Falcon Server",
      FalconStartupSite: "Falcon startup.properties",
      FalconRuntimeSite: "Falcon runtime.properties",

      // STORM categories
      NIMBUS: "Nimbus",
      SUPERVISOR: "Supervisor",
      STORM_UI_SERVER: "Storm UI Server",
      STORM_REST_API: "Storm REST API Server",
      DRPC_SERVER: "DRPC Server",

      // FLUME categories
      FLUME_HANDLER: "flume.conf",

      // KNOX categories
      KNOX_GATEWAY: "Knox Gateway",

      // KAFKA categories
      KAFKA_BROKER: "Kafka Broker",

      // KERBEROS categories
      KDC: "KDC",
      Kadmin: "Kadmin",

      // AMBARI_METRICS categories
      MetricCollector: "Metric Collector",

      // RANGER categories
      RANGER_ADMIN: "Admin Settings",
      DBSettings: "DB Settings",
      RangerSettings: "Ranger Settings",
      UnixAuthenticationSettings: "Unix Authentication Settings",
      ADSettings: "AD Settings",
      LDAPSettings: "LDAP Settings",
      KnoxSSOSettings: "Knox SSO Settings",

      // HAWQ categories
      AdvancedHawqCheck: "Advanced HAWQ Check",

      // LOGSEARCH categories
      LogsearchAdminJson: "Advanced logsearch-admin-json",

      // Advanced category
      Advanced: "Advanced",
    };

    // Return display name if exists, otherwise return the category name as-is
    return categoryDisplayNameMap[categoryName] || categoryName;
  };

  // Check if two values have differences (similar to Ember.js hasCompareDiffs)
  const hasCompareDiffs = (
    value1: any,
    value2: any,
    isFinal1: boolean,
    isFinal2: boolean,
    propertyName: string,
    configType: string
  ) => {
    // Handle password fields - don't show differences for security
    // (This matches Ember.js logic that excludes password displayType)
    if (shouldHideProperty(propertyName, configType)) {
      return false;
    }

    // Convert values to strings for comparison
    const val1 = String(value1 || "").trim();
    const val2 = String(value2 || "").trim();

    // Check if values are different or final attributes are different
    return val1 !== val2 || isFinal1 !== isFinal2;
  };

  // Get all categories for a specific tab, marking which properties have differences
  const getAllCategoriesWithDifferenceMarkers = (
    tabName: string,
    transformedData: any
  ) => {
    if (!transformedData[tabName]) {
      return {};
    }

    const categoriesWithMarkers: any = {};
    let totalDifferencesInTab = 0;

    Object.keys(transformedData[tabName]).forEach((categoryName) => {
      const category = transformedData[tabName][categoryName];
      const propertiesWithMarkers: any = {};
      let differencesInCategory = 0;

      Object.keys(category).forEach((propertyName) => {
        const property = category[propertyName];
        const version1Key = `version_${firstVersion}`;
        const version2Key = `version_${secondVersion}`;
        const version1Data = property[version1Key];
        const version2Data = property[version2Key];

        // Check if this property has differences
        const hasDiff =
          version1Data && version2Data
            ? hasCompareDiffs(
                version1Data.value,
                version2Data.value,
                version1Data.isFinal || false,
                version2Data.isFinal || false,
                propertyName,
                version1Data.type
              )
            : !version1Data || !version2Data;

        // Check if this property is overridden (has config group overrides or is custom)
        const isOverridden = isPropertyOverridden(
          propertyName,
          version1Data?.type || version2Data?.type || ""
        );

        // Check if this property has issues (errors, warnings, or validation issues)
        const hasIssues = checkPropertyHasIssues(
          propertyName,
          version1Data?.type || version2Data?.type || ""
        );

        // Check if property matches search string
        const matchesSearch = matchesSearchString(propertyName);

        // Debug logging for final properties filter
        const isFinal1 = version1Data?.isFinal || false;
        const isFinal2 = version2Data?.isFinal || false;

        if (isFinal1 || isFinal2) {
          console.log(
            `Final property in filter: ${propertyName}, version1Final: ${isFinal1}, version2Final: ${isFinal2}`
          );
        }

        // Check if property should be included based on selected filters
        const shouldIncludeProperty =
          shouldIncludeBasedOnFilters(
            hasDiff,
            isFinal1,
            isFinal2,
            isOverridden,
            hasIssues
          ) && matchesSearch;

        // Only include properties that match the filter criteria
        if (shouldIncludeProperty) {
          propertiesWithMarkers[propertyName] = {
            ...property,
            hasDifference: hasDiff,
          };

          if (hasDiff) {
            differencesInCategory++;
            totalDifferencesInTab++;
          }
        }
      });

      // Only include categories that have properties after filtering
      if (Object.keys(propertiesWithMarkers).length > 0) {
        categoriesWithMarkers[categoryName] = {
          properties: propertiesWithMarkers,
          differenceCount: differencesInCategory,
        };
      }
    });

    return {
      categories: categoriesWithMarkers,
      totalDifferences: totalDifferencesInTab,
    };
  };

  // Check if property is overridden (based on Ember.js logic)
  const isPropertyOverridden = (propertyName: string, configType: string) => {
    // In Ember.js: isOverridden = Em.computed.or('overrides.length', '!isOriginalSCP')
    // This means a property is overridden if:
    // 1. It has config group overrides (overrides.length > 0)
    // 2. OR it's not an original stack config property (!isOriginalSCP - meaning it's custom/user property)

    // Check if property is NOT defined in stack (making it a custom/user property)
    const isCustomProperty = !isPropertyDefinedInStack(
      propertyName,
      configType
    );

    // Check if property has actual config group overrides
    const hasConfigGroupOverrides = checkForConfigGroupOverrides(
      propertyName,
      configType
    );

    // Property is overridden if it's custom OR has config group overrides
    return isCustomProperty || hasConfigGroupOverrides;
  };

  // Check if property has config group overrides
  const checkForConfigGroupOverrides = (
    propertyName: string,
    configType: string
  ) => {
    if (!configGroupsData || configGroupsData.length === 0) {
      return false;
    }

    // Look through all non-default config groups to see if this property is overridden
    return configGroupsData.some((configGroup: any) => {
      // Skip default config group
      if (configGroup.ConfigGroup?.group_name === "Default") {
        return false;
      }

      // Check if this config group has desired configs for the property's config type
      const desiredConfigs = configGroup.ConfigGroup?.desired_configs || [];

      return desiredConfigs.some((desiredConfig: any) => {
        // Check if the config type matches
        if (desiredConfig.type === configType) {
          // Check if this property exists in the config group's properties
          const properties = desiredConfig.properties || {};
          return properties.hasOwnProperty(propertyName);
        }
        return false;
      });
    });
  };

  // Check if property has issues (based on Ember.js hasIssues logic)
  const checkPropertyHasIssues = (propertyName: string, configType: string) => {
    // Check if property exists in configProperties and has error/warn messages
    if (configProperties[serviceName]) {
      // Look through all config types to find the property
      for (const configTypeKey of Object.keys(configProperties[serviceName])) {
        const properties = configProperties[serviceName][configTypeKey]?.properties || {};
        
        if (properties[propertyName]) {
          const property = properties[propertyName];
          
          // Check for error message (Ember: error = Em.computed.bool('errorMessage.length'))
          const hasError = !!(property.errorMessage && property.errorMessage.length > 0);
          
          // Check for warn message (Ember: warn = Em.computed.bool('warnMessage.length'))
          const hasWarn = !!(property.warnMessage && property.warnMessage.length > 0);
          
          // Check for validation errors/warnings arrays
          const hasValidationErrors = !!(property.validationErrors && property.validationErrors.length > 0);
          const hasValidationWarnings = !!(property.validationWarnings && property.validationWarnings.length > 0);
          
          // Check if property has overrides with issues
          const hasOverridesWithIssues = !!(
            property.overrideValues && 
            Array.isArray(property.overrideValues) && 
            property.overrideValues.some((override: any) => 
              override.errorMessage || override.warnMessage
            )
          );
          
          // Return true if any issue type exists (matching Ember.js Em.computed.or logic)
          return hasError || hasWarn || hasValidationErrors || hasValidationWarnings || hasOverridesWithIssues;
        }
      }
    }

    // Fallback: run validation checks if property not found in configProperties
    const hasValidationIssues = checkForValidationIssues(
      propertyName,
      configType
    );

    const hasConfigurationIssues = checkForConfigurationIssues(
      propertyName,
      configType
    );

    return hasValidationIssues || hasConfigurationIssues;
  };

  // Check for validation issues (based on Ember.js validation logic)
  const checkForValidationIssues = (
    propertyName: string,
    configType: string
  ) => {
    // Get the property's display type to determine validation rules
    const displayType = getPropertyDisplayType(propertyName, configType);

    // Get the property value from either version for validation
    const propertyValue = getPropertyValueForValidation(
      propertyName,
      configType
    );

    // Apply Ember.js validation logic based on display type
    const errorMessage = getErrorValidationMessage(
      displayType,
      propertyValue,
      propertyName
    );
    const warnMessage = getWarningValidationMessage(
      displayType,
      propertyValue,
      propertyName,
      configType
    );

    // Property has issues if it has error or warning messages
    return !!(errorMessage || warnMessage);
  };

  // Get display type for property (based on Ember.js logic)
  const getPropertyDisplayType = (propertyName: string, configType: string) => {
    // Check stack configurations for display type
    if (configs?.items) {
      const stackProperty = configs.items.find((service: any) =>
        service.configurations?.find(
          (config: any) =>
            config.StackConfigurations.property_name === propertyName &&
            config.StackConfigurations.type === `${configType}.xml`
        )
      );

      if (stackProperty) {
        const configData = stackProperty.configurations.find(
          (config: any) =>
            config.StackConfigurations.property_name === propertyName &&
            config.StackConfigurations.type === `${configType}.xml`
        );

        const propertyType =
          configData?.StackConfigurations?.property_type || [];
        const valueAttributes =
          configData?.StackConfigurations?.property_value_attributes;

        // Determine display type based on property attributes
        if (
          propertyType.includes("PASSWORD") ||
          valueAttributes?.type === "password"
        ) {
          return "password";
        }
        if (valueAttributes?.type === "int") {
          return "int";
        }
        if (valueAttributes?.type === "float") {
          return "float";
        }
        if (
          propertyName.includes("dir") ||
          propertyName.includes("directory")
        ) {
          return "directory";
        }
        if (propertyName.includes("host")) {
          return "host";
        }
        if (propertyName.includes("email")) {
          return "email";
        }
      }
    }

    // Default display type
    return "string";
  };

  // Get property value for validation
  const getPropertyValueForValidation = (
    propertyName: string,
    configType: string
  ) => {
    // Try to get value from transformed data
    const tabData = transformedData["Advanced"] || {};
    const categoryData =
      tabData[getDefaultCategoryName(propertyName, configType)] || {};
    const propertyData = categoryData[propertyName];

    if (propertyData) {
      const version1Data = propertyData[`version_${firstVersion}`];
      const version2Data = propertyData[`version_${secondVersion}`];
      return version1Data?.value || version2Data?.value || "";
    }

    return "";
  };

  // Get error validation message (based on Ember.js getErrorValidator)
  const getErrorValidationMessage = (
    displayType: string,
    value: string,
    propertyName: string
  ) => {
    switch (displayType) {
      case "int":
        return !isValidInt(value) ? "Must contain digits only" : "";
      case "float":
        return !isValidFloat(value) ? "Must be a valid number" : "";
      case "directory":
        if (!isValidDirectory(value))
          return "Must be a slash or drive at the start, and must not contain white spaces";
        if (!isAllowedDirectory(value)) return 'Can\'t start with "home(s)"';
        return hasTrailingSpaces(value)
          ? "Cannot contain trailing whitespace"
          : "";
      case "email":
        return !isValidEmail(value) ? "Must be a valid email address" : "";
      case "password":
        if (propertyName === "ranger_admin_password" && value.length < 9) {
          return "Password should contain at least 9 symbols";
        }
        return "";
      case "host":
        return hasLeadingOrTrailingSpaces(value)
          ? "Cannot contain leading or trailing whitespace"
          : "";
      default:
        return hasTrailingSpaces(value)
          ? "Cannot contain trailing whitespace"
          : "";
    }
  };

  // Get warning validation message (based on Ember.js getWarningValidator)
  const getWarningValidationMessage = (
    displayType: string,
    value: string,
    propertyName: string,
    configType: string
  ) => {
    switch (displayType) {
      case "int":
      case "float":
        return checkNumericBoundaries(value, propertyName, configType);
      default:
        return "";
    }
  };

  // Helper validation functions (simplified versions of Ember.js validators)
  const isValidInt = (value: string) => /^\d+$/.test(value.trim());
  const isValidFloat = (value: string) => /^\d*\.?\d+$/.test(value.trim());
  const isValidEmail = (value: string) =>
    /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
  const isValidDirectory = (value: string) =>
    /^[\/\\]/.test(value) && !/\s/.test(value);
  const isAllowedDirectory = (value: string) =>
    !/^home[s]?[\/\\]/.test(value.toLowerCase());
  const hasTrailingSpaces = (value: string) => /\s+$/.test(value);
  const hasLeadingOrTrailingSpaces = (value: string) => /^\s|\s$/.test(value);

  // Check numeric boundaries (simplified version)
  const checkNumericBoundaries = (
    _value: string,
    _propertyName: string,
    _configType: string
  ) => {
    // This would need to check stack configuration for min/max values
    // For now, return empty string (no warnings)
    return "";
  };

  // Check for configuration issues (basic implementation)
  const checkForConfigurationIssues = (
    propertyName: string,
    _configType: string
  ) => {
    // Check for deprecated or problematic properties
    const deprecatedProperties: any = [];

    return deprecatedProperties.includes(propertyName);
  };

  // Check if property matches search string
  const matchesSearchString = (propertyName: string) => {
    if (!searchString || searchString.trim() === "") {
      return true; // Show all properties if no search string
    }

    // Get display name for more comprehensive search
    const displayName = getPropertyDisplayName(propertyName, "");

    // Search in both property name and display name (case-insensitive)
    const searchTerm = searchString.toLowerCase().trim();
    return (
      propertyName.toLowerCase().includes(searchTerm) ||
      displayName.toLowerCase().includes(searchTerm)
    );
  };

  // Check if property should be included based on selected filters
  const shouldIncludeBasedOnFilters = (
    hasDifference: boolean,
    isFinal1: boolean,
    isFinal2: boolean,
    isOverridden: boolean,
    hasIssues: boolean
  ) => {
    // If no filters are selected, show all properties
    if (!selectedFilters || selectedFilters.length === 0) {
      return true;
    }

    // Handle "Clear Filters" option - if selected, show all properties
    if (selectedFilters.some((filter: any) => filter.value === "Clear")) {
      return true;
    }

    // For multiple filters, use intersection (AND logic) - property must match ALL selected filters
    return selectedFilters.every((filter: any) => {
      switch (filter.value) {
        case "Changed":
          return hasDifference;
        case "Final":
          return isFinal1 || isFinal2;
        case "Overridden":
          return isOverridden;
        case "Issues":
          return hasIssues;
        case "Clear":
          return true;
        default:
          return true;
      }
    });
  };

  if (isEmpty(configProperties)) {
    return <Spinner />;
  }

  // For services without theme data, ensure we have at least the Advanced tab
  if (isEmpty(theme) || !theme[serviceName]) {
    // Create minimal theme structure if missing
    if (!theme[serviceName]) {
      theme[serviceName] = {
        tabs: {
          Advanced: {
            name: "Advanced",
            displayName: "Advanced",
            sections: {},
          }
        },
        subsectionProperties: {},
        widgets: {},
      };
    }
  }

  return (
    <div className="configs-comparator p-3">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <div />
        <div className="d-flex align-items-center w-50">
          <InputGroup className="me-3 w-100">
            <InputGroup.Text>
              <FontAwesomeIcon icon={faSearch} className="text-muted" />
            </InputGroup.Text>
            <Form.Control
              type="text"
              placeholder="Search properties..."
              value={searchString}
              onChange={(e) => setSearchString(e.target.value)}
            />
          </InputGroup>
          <ComparatorFilter
            selectedFilters={selectedFilters}
            setSelectedFilters={setSelectedFilters}
          />
        </div>
      </div>
      <Tabs className="ambari-tabs">
        {activeServiceTabs.map((tab) => {
          const tabData = getAllCategoriesWithDifferenceMarkers(
            tab.key,
            transformedData
          );
          const allCategories = tabData.categories || {};
          const totalDifferences = tabData.totalDifferences || 0;

          return (
            <Tab
              eventKey={tab.key}
              title={`${tab.displayName} (${totalDifferences})`}
              key={tab.key}
              className="mt-4"
            >
              <div className="mb-3">
                <div className="comparison-header border-bottom p-3">
                  <div className="row">
                    <div className="col-md-4 ps-4">
                      <strong>Property Name</strong>
                    </div>
                    <div className="col-md-4">
                      <strong>Version {firstVersion}</strong>
                      {currentVersion === firstVersion && (
                        <span className="badge bg-success ms-2">Current</span>
                      )}
                    </div>
                    <div className="col-md-4">
                      <strong>Version {secondVersion}</strong>
                      {currentVersion === secondVersion && (
                        <span className="badge bg-success ms-2">Current</span>
                      )}
                    </div>
                  </div>
                </div>
              </div>
              {Object.keys(allCategories).length > 0 ? (
                <Accordion alwaysOpen>
                  {Object.keys(allCategories).map((categoryName, index) => {
                    const categoryData = allCategories[categoryName];
                    const categoryDisplayName =
                      getCategoryDisplayName(categoryName);
                    const categoryProperties = categoryData.properties;
                    const differenceCount = categoryData.differenceCount;

                    return (
                      <Accordion.Item
                        eventKey={index.toString()}
                        key={categoryName}
                      >
                        <Accordion.Header>
                          <div className="w-100 d-flex justify-content-between">
                            <div title={categoryName} className="fs-18">
                              {categoryDisplayName}
                            </div>
                            <span
                              className={`badge ${
                                differenceCount > 0
                                  ? "bg-primary"
                                  : "bg-secondary"
                              }`}
                            >
                              {Object.keys(categoryProperties).length}{" "}
                              properties
                              {differenceCount > 0 && (
                                <span className="ms-1">
                                  ({differenceCount}{" "}
                                  {differenceCount === 1
                                    ? "difference"
                                    : "differences"}
                                  )
                                </span>
                              )}
                            </span>
                          </div>
                        </Accordion.Header>
                        <Accordion.Body className="mt-3">
                          {Object.keys(categoryProperties).map(
                            (propertyName) => {
                              const property = categoryProperties[propertyName];
                              const version1Data =
                                property[`version_${firstVersion}`];
                              const version2Data =
                                property[`version_${secondVersion}`];
                              const hasDifference = property.hasDifference;

                              // Get proper display name for the property
                              const displayName = getPropertyDisplayName(
                                propertyName,
                                version1Data?.type || version2Data?.type || ""
                              );
                              const formattedDisplayName =
                                formatWordBreak(displayName);

                              return (
                                <div
                                  key={propertyName}
                                  className={`comparison-row border-bottom py-2 p-3 fs-14 ${
                                    hasDifference ? "bg-info-subtle" : ""
                                  }`}
                                >
                                  <div className="row">
                                    <div className="col-md-4 p-2 px-3">
                                      <strong
                                        className={`fs-14 ${
                                          hasDifference
                                            ? "text-warning-emphasis"
                                            : ""
                                        }`}
                                        title={propertyName} // Show raw name on hover
                                        dangerouslySetInnerHTML={{
                                          __html: formattedDisplayName,
                                        }}
                                      />
                                      {hasDifference && (
                                        <i
                                          className="fas fa-exclamation-triangle text-warning ms-2"
                                          title="Property has differences"
                                        ></i>
                                      )}
                                    </div>
                                    <div className="d-flex align-items-center col-md-4 p-2 px-3 mw-100 overflow-scroll">
                                      <div className="compare-config-cell">
                                        {version1Data ? (
                                          <div className="d-flex align-items-center">
                                            <div className="fs-14">
                                              {version1Data.value ||
                                                "UNDEFINED"}
                                            </div>
                                            {version1Data.isFinal &&
                                              shouldShowFinalIcon(
                                                version1Data.type
                                              ) && (
                                                <FontAwesomeIcon
                                                  icon={faLock}
                                                  className="ms-1"
                                                  title="Final"
                                                />
                                              )}
                                          </div>
                                        ) : (
                                          <span className="text-muted">
                                            UNDEFINED
                                          </span>
                                        )}
                                      </div>
                                    </div>
                                    <div className="col-md-4 p-2 px-3 mw-100 overflow-scroll">
                                      <div className="compare-config-cell">
                                        {version2Data ? (
                                          <div className="d-flex align-items-center">
                                            <div className="fs-14">
                                              {version2Data.value ||
                                                "UNDEFINED"}
                                            </div>
                                            {version2Data.isFinal &&
                                              shouldShowFinalIcon(
                                                version2Data.type
                                              ) && (
                                                <FontAwesomeIcon
                                                  icon={faLock}
                                                  className="ms-1"
                                                  title="Final"
                                                />
                                              )}
                                          </div>
                                        ) : (
                                          <span className="text-muted">
                                            UNDEFINED
                                          </span>
                                        )}
                                      </div>
                                    </div>
                                  </div>
                                </div>
                              );
                            }
                          )}
                        </Accordion.Body>
                      </Accordion.Item>
                    );
                  })}
                </Accordion>
              ) : (
                <div className="text-center py-4 text-muted">
                  <p>No properties found in this tab</p>
                </div>
              )}
            </Tab>
          );
        })}
      </Tabs>
    </div>
  );
}
