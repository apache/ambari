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

import React, { useEffect, useState, useTransition } from "react";
import {
  Card,
  Col,
  Form,
  InputGroup,
  Nav,
  Row,
  Tab,
  Stack,
  Alert,
  Dropdown,
  Badge,
  Button,
} from "react-bootstrap";
import { cloneDeep, forEach, get, isArray, isEmpty } from "lodash";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faLock,
  faRedo,
  faUndo,
  faPlusCircle,
  faMinusCircle,
  faPen,
  faBell,
  faCheckCircle,
} from "@fortawesome/free-solid-svg-icons";
import "rc-slider/assets/index.css";
import Select from "react-select";
import dayjs from "dayjs";
import duration from "dayjs/plugin/duration";
import CustomSlider from "../../components/CustomSlider";
import AdvancedConfigs from "./AdvancedConfigs";
import ChooseConfigGroup from "./ChooseConfigGroup";
import ManageConfigGroups from "../ConfigGroups/ManageConfigGroups";
import {
  ThemeType,
  TabType,
  SubsectionPropertiesType,
  PropertyType,
  configGroupOverrides,
  TruthValues,
} from "./types";
import TestConnection from "./TestConnection";
import Spinner from "../../components/Spinner";
import {
  filterConfigProperties,
  setTabErrorCounts,
  updateVisibilityByForeignKeys,
  validateAllProperties,
  updateVisibilityForDependsOn,
  validateInput,
  formatPropertyValue,
  evaluateDependsOnForConfig,
  getSectionErrorCount,
} from "./ConfigUtils";
import useEnhancedConfigs from "../../hooks/useEnhancedConfigs";
import OverlayBackdrop from "../../components/OverlayBackdrop";
import {
  widgetValueByConfigAttributes,
  configValueByWidget,
  formatTickLabel,
  getDisplayUnitLabel,
  getConfigUnitInfo,
  parseTimeInterval,
  composeTimeInterval,
} from "../../Utils/unitConversionUtils";
import Modal from "../../components/Modal";
import Table from "../../components/Table";
import { useAuth } from "../../hooks/useAuth";
import Tooltip from "../../components/Tooltip";
import { useContext } from "react";
import { AppContext } from "../../store/context";
import { useDebounce } from "../../hooks/useDebounce";
import { formatParamsForDisplay, formatParamsForSave, shouldUseMultilineFormatting } from "../../Utils/jvmFormatUtils";

dayjs.extend(duration);

type ConfigProps = {
  configSection: string;
  themeData: any;
  configPropertiesData: any;
  propertyValues?: any;
  servicesList: string[];
  configProperties: any;
  setConfigProperties: any;
  recommendationsDataToSend?: Object;
  installer?: boolean;
  wizardName?: string;
  configGroup?: string;
  setShowAddToGroupModal?: (show: boolean) => void;
  setConfigGroup?: (groupName: string) => void;
  hostNames?: string[];
  hostGroups?: any;
  version?: any;
  hostConfigs?: boolean;
  displayUndoRedo?: boolean;
  installedServices?: string[];
  stack?: string;
  stackVersion?: string;
  hosts?: string[];
  validationErrors?: any;
  onServiceChange?: (serviceName: string) => void;
  configsLoading?: boolean;
};

interface PropertyFilter {
  id: string;
  label: string;
  attributeName: "isOverridden" | "isFinal" | "hasIssues";
  selected: boolean;
  disabled?: boolean;
}

export default function Config({
  configSection,
  themeData,
  configPropertiesData,
  servicesList,
  configProperties,
  setConfigProperties,
  installer,
  wizardName,
  configGroup,
  setShowAddToGroupModal,
  setConfigGroup,
  version,
  recommendationsDataToSend = {},
  hostConfigs = false,
  displayUndoRedo = true,
  installedServices,
  stack,
  stackVersion,
  hosts = [],
  validationErrors = [],
  onServiceChange,
  configsLoading = false,
}: ConfigProps) {
  const [chosenService, setChosenService] = useState<string>("");
  const [chosenTab, setChosenTab] = useState<string>("");
  const [theme, setTheme] = useState<ThemeType>({});
  const [services, setServices] = useState<string[]>([]);
  const [tabErrors, setTabErrors] = useState<any>({});
  const [versionLoading, setVersionLoading] = useState(false);
  const [searchString, setSearchString] = useState("");
  const [widgetTextModeMap, setWidgetTextModeMap] = useState<
    Record<string, boolean>
  >({});
  // @ts-ignore
  const [isFullyLoaded, setIsFullyLoaded] = useState(false);
  const [isServiceSwitching, setIsServiceSwitching] = useState(false);
  const { havePermissions } = useAuth();
  const canEditConfigs = havePermissions("SERVICE.MODIFY_CONFIGS");
  const [showDependedConfigsModal, setShowDependedConfigsModal] =
    useState(false);
  const [, startTransition] = useTransition();

  // Get cluster context for config groups
  const { clusterName: contextClusterName, allHostNames } =
    useContext(AppContext);

  // Config group state for installer mode - service-specific
  const [selectedConfigGroups, setSelectedConfigGroups] = useState<
    Record<string, string>
  >({});
  const [configGroupsData, setConfigGroupsData] = useState<any[]>([]);
  const [showManageConfigGroupModal, setShowManageConfigGroupModal] =
    useState(false);
  const [refetchTrigger, setRefetchTrigger] = useState<number>(0);

  // Get the current config group for the chosen service
  const getSelectedConfigGroupForService = (serviceName: string) => {
    return selectedConfigGroups[serviceName] || "Default";
  };

  // Get the current config group (use service-specific selectedConfigGroup in installer mode, otherwise use configGroup prop)
  const currentConfigGroup = installer
    ? getSelectedConfigGroupForService(chosenService)
    : configGroup || "Default";

  const [propertyFilters, setPropertyFilters] = useState<PropertyFilter[]>([
    {
      id: "overridden",
      label: "Overridden properties",
      attributeName: "isOverridden",
      selected: false,
      disabled: false,
    },
    {
      id: "final",
      label: "Final properties",
      attributeName: "isFinal",
      selected: false,
    },
    {
      id: "issues",
      label: "Show property issues",
      attributeName: "hasIssues",
      selected: false,
    },
  ]);

  const {
    onValueUpdate,
    processingConfig,
    recommendedChanges,
    setRecommendedChanges,
  } = useEnhancedConfigs(
    setConfigProperties,
    chosenService,
    installedServices ?? [],
    recommendationsDataToSend,
    installer ? "clusterCreation" : "serviceConfigs",
    stack,
    stackVersion,
    hosts
  );

  const handleRecommendationChange = (propertyName: string, fileName: string, isChecked: boolean) => {
    const updatedChanges = { ...recommendedChanges };
    const key = `${propertyName}${fileName}`;
    
    if (updatedChanges[key]) {
      // Update the recommendation state
      updatedChanges[key] = {
        ...updatedChanges[key],
        isChanged: isChecked,
      };

      // Create a new config properties object and find the property to update
      const newConfigs = cloneDeep(configProperties);
      let propertyFound = false;

      // Search through all services and config types to find the property
      for (const serviceName in newConfigs) {
        if (!newConfigs[serviceName]) continue;
        
        for (const configType in newConfigs[serviceName]) {
          if (!newConfigs[serviceName][configType] || !newConfigs[serviceName][configType].properties) continue;
          
          for (const propKey in newConfigs[serviceName][configType].properties) {
            const property = newConfigs[serviceName][configType].properties[propKey];
            if (property && property.propertyName === propertyName && property.fileName === fileName) {
              console.log(`Found property ${propertyName} in ${serviceName}/${configType}`);
              console.log(`Current value: ${property.value}`);
              console.log(`Recommended value: ${updatedChanges[key].recommendedValue}`);
              console.log(`Original value: ${updatedChanges[key].initialValue || updatedChanges[key].originalValue}`);
              
              if (isChecked) {
                // Apply recommended value
                property.value = updatedChanges[key].recommendedValue;
                console.log(`Applied recommended value: ${property.value}`);
              } else {
                // Revert to original value
                const originalValue = updatedChanges[key].initialValue || updatedChanges[key].originalValue;
                property.value = originalValue;
                console.log(`Reverted to original value: ${property.value}`);
              }

              // Clear any error messages
              property.errorMessage = "";
              property.warnMessage = "";
              
              propertyFound = true;
              break;
            }
          }
          if (propertyFound) break;
        }
        if (propertyFound) break;
      }

      if (propertyFound) {
        console.log(`Updating config properties state`);
        setConfigProperties(newConfigs);
      } else {
        console.warn(`Property ${propertyName} in ${fileName} not found in config properties`);
        console.log('Available properties:', Object.keys(configProperties).map(service => 
          Object.keys(configProperties[service]).map(configType => 
            Object.keys(configProperties[service][configType].properties || {}).map(prop => 
              `${service}/${configType}/${configProperties[service][configType].properties[prop].propertyName} (${configProperties[service][configType].properties[prop].fileName})`
            )
          ).flat()
        ).flat());
      }
    }
    
    setRecommendedChanges(updatedChanges);
  };

  useEffect(() => {
    setVersionLoading(true);
    setTimeout(() => {
      setVersionLoading(false);
    }, 2000);
  }, [version]);

  useEffect(() => {
    // Don't reset property filters in installer mode when config group changes
    if (!installer) {
      setPropertyFilters((prev) =>
        prev.map((filter) => {
          return { ...filter, selected: false };
        })
      );
    }
  }, [configGroup, installer]);

  const handleFilterToggle = (filterId: string) => {
    setPropertyFilters((prev) =>
      prev.map((filter) =>
        filter.id === filterId
          ? { ...filter, selected: !filter.selected }
          : filter
      )
    );
  };

  const handleClearFilters = () => {
    setPropertyFilters((prev) =>
      prev.map((filter) => ({ ...filter, selected: false }))
    );
  };

  // Debounced filtering function to improve performance
  const applyFilters = (searchStr: string, filters: PropertyFilter[]) => {
    // Check if any filters are actually selected
    const hasActiveFilters = filters.some((f) => f.selected);

    let configsCopy = cloneDeep(configProperties);

    // If no search string and no active filters, restore all properties to visible
    if (isEmpty(searchStr) && !hasActiveFilters) {
      // Reset all properties to visible state (remove any previous filtering)
      Object.keys(configsCopy).forEach((service) => {
        Object.keys(configsCopy[service]).forEach((configType) => {
          Object.keys(configsCopy[service][configType].properties).forEach(
            (propertyName) => {
              const property =
                configsCopy[service][configType].properties[propertyName];
              // Restore visibility unless the property was originally hidden
              if (property.isHidden !== true) {
                property.isVisible = true;
              }
            }
          );
        });
      });
    } else {
      // Apply filters
      const activeFilters = {
        showOverridden:
          filters.find((f) => f.id === "overridden")?.selected || false,
        showFinal: filters.find((f) => f.id === "final")?.selected || false,
        showIssues: filters.find((f) => f.id === "issues")?.selected || false,
      };

      configsCopy = filterConfigProperties(
        configsCopy,
        searchStr,
        activeFilters
      );
    }

    // Don't automatically switch tabs when filtering - preserve current tab
    setConfigProperties(configsCopy);
  };

  // Create debounced version of the filter function
  const debouncedApplyFilters = useDebounce(applyFilters, 300);

  useEffect(() => {
    debouncedApplyFilters(searchString, propertyFilters);
  }, [searchString, propertyFilters, debouncedApplyFilters]);

  const getTheme = async () => {
    let theme: ThemeType = {};
    let reqServices = new Set<string>();

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
                  tabsData[tab.name].sections[section.name].subsections[
                    subsection.name
                  ] = {
                    name: subsection.name,
                    displayName: subsection["display-name"],
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

    forEach(servicesList, (service) => {
      // Skip Kerberos when in installer mode (add service flow)
      if (installer && service === "KERBEROS") {
        return;
      }

      if (!theme[service]) {
        theme[service] = {
          tabs: {
            Advanced: {
              name: "Advanced",
              displayName: "Advanced",
            },
          },
          subsectionProperties: {},
          widgets: {},
        };
      }
    });

    if (configSection !== "default") {
      setServices(Array.from(reqServices));
      setChosenService(Array.from(reqServices)[0]);
      setChosenTab(
        theme[Array.from(reqServices)[0]] &&
          Object.keys(theme[Array.from(reqServices)[0]].tabs)[0]
      );
    } else {
      if (servicesList) {
        if (installer && !servicesList.includes("MISC")) {
          servicesList.push("MISC");
        }
        setServices(servicesList);
        setChosenService(servicesList[0]);
        setChosenTab(
          theme[servicesList[0]] && Object.keys(theme[servicesList[0]].tabs)[0]
        );
      }
    }

    setTheme(theme);
  };

  useEffect(() => {
    startTransition(() => {
      setTabErrors(setTabErrorCounts(configProperties));
    });
  }, [configProperties]);

  useEffect(() => {
    if (
      !isEmpty(themeData) &&
      themeData?.items?.length &&
      !isEmpty(configPropertiesData)
    ) {
      getTheme();
    }
    if (!themeData?.items?.length) {
      setChosenTab("Advanced");
    }
  }, [themeData, configPropertiesData, JSON.stringify(servicesList)]);

  // Track service changes and set switching state
  useEffect(() => {
    if (chosenService) {
      setIsServiceSwitching(true);
      setSearchString(""); // Clear search when switching services

      // Set a longer timeout to allow for any processing to complete
      const timer = setTimeout(() => {
        setIsServiceSwitching(false);
      }, 2000);

      return () => clearTimeout(timer);
    }
  }, [chosenService]);

  // Track when everything is fully loaded
  useEffect(() => {
    // Check if current service has actual config properties with data
    const hasCurrentServiceConfigs =
      chosenService &&
      configProperties[chosenService] &&
      !isEmpty(configProperties[chosenService]) &&
      Object.keys(configProperties[chosenService]).some(
        (configType) =>
          configProperties[chosenService][configType].properties &&
          Object.keys(configProperties[chosenService][configType].properties)
            .length > 0
      );

    const hasCurrentServiceTheme =
      chosenService &&
      theme[chosenService] &&
      !isEmpty(theme[chosenService]) &&
      (theme[chosenService].tabs ||
        theme[chosenService].subsectionProperties ||
        theme[chosenService].widgets);

    const fullyLoaded =
      !configsLoading &&
      !isServiceSwitching &&
      !isEmpty(configProperties) &&
      !isEmpty(themeData) &&
      !isEmpty(theme) &&
      chosenService !== "" &&
      hasCurrentServiceConfigs &&
      hasCurrentServiceTheme;
    setIsFullyLoaded(fullyLoaded);
  }, [
    configsLoading,
    isServiceSwitching,
    configProperties,
    themeData,
    theme,
    chosenService,
  ]);

  const handleUndo = (configType: string, property: PropertyType) => {
    const newConfigs = cloneDeep(configProperties);
    newConfigs[chosenService][configType].properties[
      property.propertyName
    ].value = property.previousValue;
    setConfigProperties(newConfigs);
  };

  const setToDefault = (configType: string, property: PropertyType) => {
    const newConfigs = cloneDeep(configProperties);
    newConfigs[chosenService][configType].properties[
      property.propertyName
    ].value = formatPropertyValue(property, property.propertyValue);
    setConfigProperties(newConfigs);
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

  const evaluateDependsOn = (dependsOn: any): boolean => {
    return evaluateDependsOnForConfig(
      configProperties,
      chosenService,
      dependsOn,
      installer ? servicesList : installedServices
    );
  };

  // Check if current tab has any visible properties
  const hasVisiblePropertiesInCurrentTab = () => {
    if (chosenTab === "Advanced") {
      // For Advanced tab, check if AdvancedConfigs has any visible properties
      return (
        configProperties[chosenService] &&
        Object.keys(configProperties[chosenService]).some((config) => {
          if (config.includes("Custom") && config.endsWith("env")) {
            return false;
          }
          const currentConfigValue = configProperties[chosenService][config];
          const filteredPropertiesCount = Object.keys(
            currentConfigValue.properties || {}
          ).filter(
            (property) =>
              !currentConfigValue.properties[property].tabName &&
              currentConfigValue.properties[property].isVisible !== false &&
              !!!currentConfigValue.properties[property].isHidden
          ).length;
          return filteredPropertiesCount > 0;
        })
      );
    } else {
      // For themed tabs, check if any section has visible properties
      return theme[chosenService]?.tabs[chosenTab]?.sections?.some(
        (section: any) => {
          return section.subsections.some((subsection: any) => {
            const isVisible = subsection?.["depends-on"]
              ? evaluateDependsOn(subsection["depends-on"])
              : true;
            if (!isVisible) return false;

            const hasProperties = theme[chosenService].subsectionProperties?.[
              subsection.name
            ]?.properties?.some((config: any) => {
              const type = config["config"].split("/")[0];
              const propertyName = config["config"].split("/")[1];
              const property =
                configProperties?.[chosenService]?.[type]?.properties[
                  propertyName
                ];

              const isPropertyVisible = isArray(config?.["depends-on"])
                ? evaluateDependsOnForConfig(
                    configProperties,
                    chosenService,
                    config["depends-on"],
                    installer ? servicesList : installedServices
                  )
                : true;

              return property && property?.isVisible && isPropertyVisible;
            });

            return hasProperties;
          });
        }
      );
    }
  };

  const renderWidgets = (
    widgetType: string,
    property: PropertyType,
    onChange: any
  ) => {
    switch (widgetType) {
      case "directories":
        return (
          <Form.Control
            as="textarea"
            rows={5}
            value={property.value}
            onChange={(e) => onChange(e.target.value)}
            disabled={!property.isEditable}
          />
        );
      case "slider":
        // Get proper unit information using the helper function
        const unitInfo = getConfigUnitInfo(property);
        const { configUnit, widgetUnit, dimensionType, configType } = unitInfo;

        // Convert boundaries and step from config units to widget units
        let minimum = widgetValueByConfigAttributes(
          Number(property.propertyAttributes["minimum"]) || 0,
          configUnit,
          widgetUnit,
          dimensionType
        );
        let maximum = widgetValueByConfigAttributes(
          Number(property.propertyAttributes["maximum"]) || 100,
          configUnit,
          widgetUnit,
          dimensionType
        );
        let step = widgetValueByConfigAttributes(
          Number(property.propertyAttributes["increment_step"]) || 1,
          configUnit,
          widgetUnit,
          dimensionType
        );

        // Convert current value from config units to widget units
        let value = widgetValueByConfigAttributes(
          Number(property.value) || 0,
          configUnit,
          widgetUnit,
          dimensionType
        );

        // Get display unit label (will be empty for int/float)
        const displayUnit = getDisplayUnitLabel(widgetUnit);

        // Create marks with proper formatting
        const marks = {
          [minimum]: formatTickLabel(
            minimum,
            displayUnit,
            displayUnit ? " " : ""
          ),
          [minimum + (maximum - minimum) / 2]: formatTickLabel(
            minimum + (maximum - minimum) / 2,
            displayUnit,
            displayUnit ? " " : ""
          ),
          [maximum]: formatTickLabel(
            maximum,
            displayUnit,
            displayUnit ? " " : ""
          ),
        };

        // Check if this slider is in text input mode
        const isTextMode = widgetTextModeMap[property.propertyName] || false;

        // Toggle between slider and text input mode
        const toggleMode = () => {
          setWidgetTextModeMap((prev) => ({
            ...prev,
            [property.propertyName]: !prev[property.propertyName],
          }));
        };

        return (
          <div className="d-flex align-items-center">
            {isTextMode ? (
              <InputGroup className="w-50 me-2">
                <Form.Control
                  type="text"
                  value={property.value}
                  onChange={(e) => onChange(e.target.value)}
                  disabled={!property.isEditable}
                  autoFocus
                />
                {displayUnit && (
                  <InputGroup.Text>{displayUnit}</InputGroup.Text>
                )}
              </InputGroup>
            ) : (
              <div className="flex-grow-1">
                <CustomSlider
                  min={minimum}
                  max={maximum}
                  step={step}
                  marks={marks}
                  value={value}
                  unit={displayUnit || ""}
                  onChange={(sliderValue: any) => {
                    // Convert slider value back to config units before saving
                    const configValue = configValueByWidget(
                      sliderValue,
                      widgetUnit,
                      configUnit,
                      configType,
                      dimensionType
                    );
                    onChange(configValue);
                  }}
                  disabled={!property.isEditable}
                  propertyUnit={displayUnit}
                />
              </div>
            )}
            {canEditConfigs && (
              <Tooltip
                message={
                  isTextMode
                    ? "Switch back to slider mode"
                    : "Switch to text input mode"
                }
                placement="top"
              >
                <FontAwesomeIcon
                  icon={faPen}
                  className={`ms-4 ${isTextMode ? "text-primary" : ""} ${
                    property.isEditable ? "pointer" : ""
                  }`}
                  onClick={property.isEditable ? toggleMode : undefined}
                />
              </Tooltip>
            )}
          </div>
        );

      case "text-field":
        return (
          <InputGroup
            className={property.propertyAttributes.unit ? "w-50" : "w-100"}
          >
            <Form.Control
              type="text"
              value={property.value}
              onChange={(e) => onChange(e.target.value)}
              placeholder={property.propertyValue}
              disabled={!property.isEditable}
            />
            {property.propertyAttributes.unit ? (
              <InputGroup.Text>
                {property.propertyAttributes.unit}
              </InputGroup.Text>
            ) : null}
          </InputGroup>
        );

      case "combo":
        // Check if this combo is in text input mode
        const isComboTextMode =
          widgetTextModeMap[property.propertyName] || false;

        // Database configuration fields that should not be switchable to text mode
        const databaseConfigFields = [
          "hive_database",
          "oozie_database",
          "DB_FLAVOR",
          "ssm_database",
        ];
        const isDatabaseConfigField = databaseConfigFields.includes(
          property.propertyName
        );

        // Hide edit button when:
        // 1. entriesEditable is explicitly set to false, OR
        // 2. This is a database configuration field (hive_database, oozie_database, DB_FLAVOR, ssm_database)
        const allowSwitchToTextBox = !(
          (property.propertyAttributes.hasOwnProperty("entriesEditable") &&
            property.propertyAttributes.entriesEditable === false) ||
          isDatabaseConfigField
        );

        // Toggle between combo and text input mode
        const toggleComboMode = () => {
          setWidgetTextModeMap((prev) => ({
            ...prev,
            [property.propertyName]: !prev[property.propertyName],
          }));
        };

        return (
          <div className="d-flex align-items-center">
            {isComboTextMode ? (
              <InputGroup className="w-50 me-2">
                <Form.Control
                  type="text"
                  value={property.value}
                  onChange={(e) => onChange(e.target.value)}
                  disabled={!property.isEditable}
                  autoFocus
                />
              </InputGroup>
            ) : (
              <div className="flex-grow-1">
                <Select
                  value={{ label: property.value, value: property.value }}
                  onChange={(newValue) => {
                    onChange(newValue?.value);
                  }}
                  options={property.propertyAttributes.entries.map(
                    (entry: { value: string; label?: string }) => ({
                      label: entry.label || entry.value,
                      value: entry.value,
                    })
                  )}
                  isDisabled={!property.isEditable}
                />
              </div>
            )}
            {allowSwitchToTextBox && canEditConfigs && (
              <Tooltip
                message={
                  isComboTextMode
                    ? "Switch back to dropdown mode"
                    : "Switch to text input mode"
                }
                placement="top"
              >
                <FontAwesomeIcon
                  icon={faPen}
                  className={`ms-4 ${isComboTextMode ? "text-primary" : ""} ${
                    property.isEditable ? "pointer" : ""
                  }`}
                  onClick={property.isEditable ? toggleComboMode : undefined}
                />
              </Tooltip>
            )}
          </div>
        );

      case "time-interval-spinner":
        // Get the config unit from property attributes, default to milliseconds
        const timeConfigUnit =
          property?.propertyAttributes?.unit || "milliseconds";

        // Parse the time interval using the unit conversion utilities
        const timeComponents = parseTimeInterval(
          Number(property.value) || 0,
          timeConfigUnit
        );

        // Check if this time-interval is in text input mode
        const isTimeTextMode =
          widgetTextModeMap[property.propertyName] || false;

        // Toggle between time-interval and text input mode
        const toggleTimeMode = () => {
          setWidgetTextModeMap((prev) => ({
            ...prev,
            [property.propertyName]: !prev[property.propertyName],
          }));
        };

        const handleDaysChange = (e: React.ChangeEvent<HTMLInputElement>) => {
          const newDays = parseInt(e.target.value, 10);
          if (!isNaN(newDays)) {
            // Compose the new value back to the config unit
            const newValue = composeTimeInterval(
              newDays,
              timeComponents.hours,
              timeComponents.minutes,
              timeComponents.seconds,
              timeConfigUnit
            );
            onChange(newValue);
          }
        };

        const handleHoursChange = (e: React.ChangeEvent<HTMLInputElement>) => {
          const newHours = parseInt(e.target.value, 10);
          if (!isNaN(newHours)) {
            // Compose the new value back to the config unit
            const newValue = composeTimeInterval(
              timeComponents.days,
              newHours,
              timeComponents.minutes,
              timeComponents.seconds,
              timeConfigUnit
            );
            onChange(newValue);
          }
        };

        const handleMinutesChange = (
          e: React.ChangeEvent<HTMLInputElement>
        ) => {
          const newMinutes = parseInt(e.target.value, 10);
          if (!isNaN(newMinutes)) {
            // Compose the new value back to the config unit
            const newValue = composeTimeInterval(
              timeComponents.days,
              timeComponents.hours,
              newMinutes,
              timeComponents.seconds,
              timeConfigUnit
            );
            onChange(newValue);
          }
        };

        const handleSecondsChange = (
          e: React.ChangeEvent<HTMLInputElement>
        ) => {
          const newSeconds = parseInt(e.target.value, 10);
          if (!isNaN(newSeconds)) {
            // Compose the new value back to the config unit
            const newValue = composeTimeInterval(
              timeComponents.days,
              timeComponents.hours,
              timeComponents.minutes,
              newSeconds,
              timeConfigUnit
            );
            onChange(newValue);
          }
        };

        return (
          <div className="d-flex align-items-center">
            {isTimeTextMode ? (
              <InputGroup className="w-50 me-2">
                <Form.Control
                  type="text"
                  value={property.value}
                  onChange={(e) => onChange(e.target.value)}
                  disabled={!property.isEditable}
                  autoFocus
                />
                {timeConfigUnit && (
                  <InputGroup.Text>{timeConfigUnit}</InputGroup.Text>
                )}
              </InputGroup>
            ) : (
              <div className="d-flex w-75">
                {timeComponents.days > 0 && (
                  <div className="d-flex flex-column me-2">
                    <Form.Control
                      type="number"
                      value={timeComponents.days}
                      onChange={handleDaysChange}
                      disabled={!property.isEditable}
                    />
                    <small>Days</small>
                  </div>
                )}
                {(timeComponents.hours > 0 || timeComponents.days > 0) && (
                  <div className="d-flex flex-column me-2">
                    <Form.Control
                      type="number"
                      value={timeComponents.hours}
                      onChange={handleHoursChange}
                      disabled={!property.isEditable}
                    />
                    <small>Hours</small>
                  </div>
                )}
                <div className="d-flex flex-column me-2">
                  <Form.Control
                    type="number"
                    value={timeComponents.minutes}
                    onChange={handleMinutesChange}
                    disabled={!property.isEditable}
                  />
                  <small>Minutes</small>
                </div>
                <div className="d-flex flex-column">
                  <Form.Control
                    type="number"
                    value={timeComponents.seconds}
                    onChange={handleSecondsChange}
                    disabled={!property.isEditable}
                  />
                  <small>Seconds</small>
                </div>
              </div>
            )}
            <Tooltip
              message={
                isTimeTextMode
                  ? "Switch back to time interval mode"
                  : "Switch to text input mode"
              }
              placement="top"
            >
              <FontAwesomeIcon
                icon={faPen}
                className={`ms-4 ${isTimeTextMode ? "text-primary" : ""} ${
                  property.isEditable ? "pointer" : ""
                }`}
                onClick={property.isEditable ? toggleTimeMode : undefined}
              />
            </Tooltip>
          </div>
        );

      case "toggle":
        const entries = property.propertyAttributes.entries || [];
        const valueLabel =
          entries.find(
            (entry: { value: string }) => entry.value === property.value
          )?.label || property.value;

        // Assume there are always two options available for toggle
        // Check if current value matches the first option (checked/Yes state)
        const isChecked =
          entries.length >= 2 ? property.value === entries[0].value : false;

        // Check if this toggle is in text input mode
        const isToggleTextMode =
          widgetTextModeMap[property.propertyName] || false;

        // Toggle between switch and text input mode
        const toggleToggleMode = () => {
          setWidgetTextModeMap((prev) => ({
            ...prev,
            [property.propertyName]: !prev[property.propertyName],
          }));
        };

        return (
          <div className="d-flex align-items-center">
            {isToggleTextMode ? (
              <InputGroup className="w-50 me-2">
                <Form.Control
                  type="text"
                  value={property.value}
                  onChange={(e) => onChange(e.target.value)}
                  disabled={!property.isEditable}
                  autoFocus
                />
              </InputGroup>
            ) : (
              <div className="flex-grow-1">
                <Form>
                  <Form.Check
                    type="switch"
                    className="labelled-switch ms-2"
                    label={valueLabel}
                    checked={isChecked}
                    onChange={(e) => onChange(e)}
                    disabled={!property.isEditable}
                  />
                </Form>
              </div>
            )}
            <Tooltip
              message={
                isToggleTextMode
                  ? "Switch back to toggle mode"
                  : "Switch to text input mode"
              }
              placement="top"
            >
              <FontAwesomeIcon
                icon={faPen}
                className={`ms-4 ${isToggleTextMode ? "text-primary" : ""} ${
                  property.isEditable ? "pointer" : ""
                }`}
                onClick={property.isEditable ? toggleToggleMode : undefined}
              />
            </Tooltip>
          </div>
        );

      case "checkbox":
        const checkboxId = `config-checkbox-${property.propertyName}`;
        return (
          <Form.Check
            id={checkboxId}
            checked={property.value === true || property.value === "true"}
            onChange={onChange}
            disabled={!property.isEditable}
          />
        );
      case "text-area":
        // Check if this property should use multiline formatting
        const useMultilineFormatting = shouldUseMultilineFormatting(property.value, property.propertyAttributes?.type);
        const displayValue = useMultilineFormatting ? formatParamsForDisplay(property.value, property.propertyAttributes?.type) : property.value;
        
        return (
          <Form.Control
            as="textarea"
            rows={10}
            value={displayValue}
            onChange={(e) => {
              // Format the value for saving if it's a multiline config
              const valueToSave = useMultilineFormatting ? formatParamsForSave(e.target.value) : e.target.value;
              onChange(valueToSave);
            }}
            disabled={!property.isEditable}
          />
        );
      case "password":
        return (
          <Row>
            <Col md={6}>
              <Form.Control
                type="password"
                value={property.value}
                onChange={(e) => onChange(e.target.value, false)}
                placeholder="Type password"
                disabled={!property.isEditable}
              />
            </Col>
            <Col md={6}>
              <Form.Control
                type="password"
                value={property.confirmPassword}
                onChange={(e) => onChange(e.target.value, true)}
                disabled={!property.isEditable}
              />
            </Col>
          </Row>
        );
      default:
        // Check if this property should use multiline formatting
        const useMultilineFormattingDefault = shouldUseMultilineFormatting(property.value, property.propertyAttributes?.type);
        
        if (useMultilineFormattingDefault) {
          const displayValueDefault = formatParamsForDisplay(property.value, property.propertyAttributes?.type);
          
          return (
            <Form.Control
              as="textarea"
              rows={10}
              value={displayValueDefault}
              onChange={(e) => {
                const valueToSave = formatParamsForSave(e.target.value);
                onChange(valueToSave);
              }}
              disabled={!property.isEditable}
            />
          );
        }
        
        return (
          <InputGroup
            className={property.propertyAttributes.unit ? "w-50" : "w-100"}
          >
            <Form.Control
              type="text"
              value={property.value}
              onChange={(e) => onChange(e.target.value)}
              placeholder={property.propertyValue}
              disabled={!property.isEditable}
            />
            {property.propertyAttributes.unit ? (
              <InputGroup.Text>
                {property.propertyAttributes.unit}
              </InputGroup.Text>
            ) : null}
          </InputGroup>
        );
    }
  };

  const renderUIOnlyWidgets = (widget: any) => {
    switch (widget.type) {
      case "test-db-connection":
        return (
          <TestConnection
            buttonLabel="Test DB Connection"
            serviceName={chosenService}
            configProperties={configProperties}
          />
        );
    }
  };

  const handleInputChangeWidget = (
    configType: string,
    property: PropertyType,
    value: any,
    widgetType: string,
    confirmPassword?: boolean
  ) => {
    let newConfigs = cloneDeep(configProperties);
    switch (widgetType) {
      case "checkbox":
        if (property.value === "true") {
          newConfigs[chosenService][configType].properties[
            property.propertyName
          ].value = "false";
        } else if (property.value === "false") {
          newConfigs[chosenService][configType].properties[
            property.propertyName
          ].value = "true";
        } else if (property.value === TruthValues.YES) {
          newConfigs[chosenService][configType].properties[
            property.propertyName
          ].value = TruthValues.NO;
        } else if (property.value === TruthValues.NO) {
          newConfigs[chosenService][configType].properties[
            property.propertyName
          ].value = TruthValues.YES;
        }
        break;
      case "toggle":
        const entries = property.propertyAttributes.entries || [];
        if (entries.length >= 2) {
          const currentValue = property.value;
          // Use String() conversion to handle boolean vs string type mismatch
          const newValue =
            String(currentValue) === String(entries[0].value)
              ? entries[1].value
              : entries[0].value;
          newConfigs[chosenService][configType].properties[
            property.propertyName
          ].value = newValue;
        }
        break;

      case "password":
        if (confirmPassword) {
          newConfigs[chosenService][configType].properties[
            property.propertyName
          ].confirmPassword = value;
        } else {
          newConfigs[chosenService][configType].properties[
            property.propertyName
          ].value = value;
        }
        break;

      default:
        newConfigs[chosenService][configType].properties[
          property.propertyName
        ].value = value;
        break;
    }
    
    // Only validate the specific property that was changed
    newConfigs[chosenService][configType].properties[
      property.propertyName
    ].errorMessage = validateInput(
      newConfigs[chosenService][configType].properties[property.propertyName],
      value
    );

    // Update section error count for the specific config type
    newConfigs[chosenService][configType].errors = getSectionErrorCount(
      newConfigs[chosenService][configType].properties
    );

    newConfigs = updateVisibilityByForeignKeys(newConfigs);
    newConfigs = updateVisibilityForDependsOn(
      newConfigs,
      themeData,
      "default",
      installedServices || []
    );
    // Remove global validation - only validate the changed property above

    setConfigProperties(newConfigs);
    onValueUpdate(
      newConfigs[chosenService][configType].properties[property.propertyName],
      newConfigs
    );
  };

  const handleInputChangeWidgetForOverrideValues = (
    configType: string,
    property: PropertyType,
    value: any,
    widgetType: string,
    index: number,
    confirmPassword?: boolean
  ) => {
    let newConfigs = cloneDeep(configProperties);

    const existingOverrideValue = get(
      newConfigs,
      [
        chosenService,
        configType,
        "properties",
        property.propertyName,
        "overrideValues",
        index,
        "value",
      ],
      ""
    );

    switch (widgetType) {
      case "checkbox":
        if (existingOverrideValue === "true") {
          newConfigs[chosenService][configType].properties[
            property.propertyName
          ].overrideValues[index].value = "false";
        } else if (existingOverrideValue === "false") {
          newConfigs[chosenService][configType].properties[
            property.propertyName
          ].overrideValues[index].value = "true";
        } else if (existingOverrideValue === TruthValues.YES) {
          newConfigs[chosenService][configType].properties[
            property.propertyName
          ].overrideValues[index].value = TruthValues.NO;
        } else if (existingOverrideValue === TruthValues.NO) {
          newConfigs[chosenService][configType].properties[
            property.propertyName
          ].overrideValues[index].value = TruthValues.YES;
        }
        break;
      case "toggle":
        const entries = property.propertyAttributes.entries || [];
        if (entries.length >= 2) {
          const currentValue = existingOverrideValue;
          // Use String() conversion to handle boolean vs string type mismatch
          const newValue =
            String(currentValue) === String(entries[0].value)
              ? entries[1].value
              : entries[0].value;
          newConfigs[chosenService][configType].properties[
            property.propertyName
          ].overrideValues[index].value = newValue;
        }
        break;

      case "password":
        if (confirmPassword) {
          newConfigs[chosenService][configType].properties[
            property.propertyName
          ].overrideValues[index].confirmPassword = value;
        } else {
          newConfigs[chosenService][configType].properties[
            property.propertyName
          ].overrideValues[index].value = value;
        }
        break;

      default:
        newConfigs[chosenService][configType].properties[
          property.propertyName
        ].overrideValues[index].value = value;
        break;
    }
    newConfigs[chosenService][configType].properties[
      property.propertyName
    ].overrideValues[index].errorMessage = validateInput(
      newConfigs[chosenService][configType].properties[property.propertyName],
      value
    );

    // Update section error count for the specific config type
    newConfigs[chosenService][configType].errors = getSectionErrorCount(
      newConfigs[chosenService][configType].properties
    );

    newConfigs = updateVisibilityByForeignKeys(newConfigs);
    newConfigs = updateVisibilityForDependsOn(
      newConfigs,
      themeData,
      "default",
      installedServices || []
    );
    // Remove global validation - only validate the changed property above

    setConfigProperties(newConfigs);
  };

  function getValidationNotificationsBody() {
    function clientSideErrors() {
      const columns = [
        {
          header: "Service",
          accessorKey: "serviceName",
          id: "serviceName",
        },
        {
          header: "Property",
          accessorKey: "propertyName",
          id: "propertyName",
          cell: (info: any) => {
            return (
              <span
                className="text-info cursor-pointer"
                onClick={() => {
                  setSearchString(info.getValue());
                  setChosenService(info.row.original.serviceName);
                }}
              >
                {info.getValue() || "N/A"}
              </span>
            );
          },
        },
        {
          header: "Actions",
          accessorKey: "",
          id: "actions",
          cell: (info: any) => {
            return (
              <Button
                size="sm"
                variant="outline-info"
                onClick={() => {
                  setSearchString(info.row.original.propertyName);
                  setChosenService(info.row.original.serviceName);
                }}
              >
                EDIT
              </Button>
            );
          },
        },
      ];
      if (validationErrors?.clientSideErrors?.length > 0) {
        return (
          <>
            <div className="d-flex align-items-center">
              <h3 className="text-dark mt-2">Required configurations</h3>
              <Badge className="bg-danger ms-2">
                {validationErrors.clientSideErrors.length}
              </Badge>
            </div>
            <div className="text-muted my-2 fs-12">
              The following properties must be set to proceed with the install.
            </div>
            <Table
              scrollable={false}
              data={validationErrors.clientSideErrors}
              columns={columns}
            />
          </>
        );
      }
    }
    function getCriticalErrors() {
      const columns = [
        {
          header: "Type",
          accessorKey: "type",
          id: "type",
        },
        {
          header: "Service",
          accessorKey: "serviceName",
          id: "serviceName",
        },
        {
          header: "Property",
          accessorKey: "propertyName",
          id: "propertyName",
          cell: (info: any) => {
            return (
              <span
                className="text-info cursor-pointer"
                onClick={() => {
                  setSearchString(info.getValue());
                  setChosenService(info.row.original.serviceName);
                }}
              >
                {info.getValue() || "N/A"}
              </span>
            );
          },
        },
        {
          header: "Value",
          accessorKey: "value",
          id: "value",
        },
        {
          header: "Description",
          accessorKey: "message",
          id: "description",
          width: "20%",
          className: "text-wrap",
        },
      ];
      if (validationErrors?.criticalErrors?.length > 0) {
        return (
          <>
            <div className="d-flex align-items-center mt-2">
              <h3 className="text-dark mt-2">
                You must correct the following critical issues before
                proceeding.
              </h3>
              <Badge className="bg-danger ms-2">
                {validationErrors.criticalErrors.length}
              </Badge>
            </div>
            <Table
              scrollable={false}
              data={validationErrors.criticalErrors}
              columns={columns}
            />
          </>
        );
      }
    }
    function getWarningErrors() {
      const columns = [
        {
          header: "Type",
          accessorKey: "type",
          id: "type",
        },
        {
          header: "Service",
          accessorKey: "serviceName",
          id: "serviceName",
        },
        {
          header: "Property",
          accessorKey: "propertyName",
          id: "propertyName",
          cell: (info: any) => {
            return (
              <span
                className="text-info cursor-pointer"
                onClick={() => {
                  setSearchString(info.getValue());
                  setChosenService(info.row.original.serviceName);
                }}
              >
                {info.getValue() || "N/A"}
              </span>
            );
          },
        },
        {
          header: "Value",
          accessorKey: "value",
          id: "value",
          width: "20%",
          className: "text-wrap",
        },
        {
          header: "Description",
          accessorKey: "message",
          id: "description",
          width: "20%",
          className: "text-wrap",
        },
      ];
      if (validationErrors?.warnings?.length > 0) {
        return (
          <>
            <div className="d-flex align-items-center mt-2">
              <h3 className="text-dark mt-2">
                Highly Recommended Configurations
              </h3>
              <Badge className="bg-danger ms-2">
                {validationErrors.warnings.length}
              </Badge>
            </div>
            <div className="text-muted my-2 fs-12">
              Please review the folowing recommended changes, and click on the
              property name to change its value.
            </div>
            <Table
              scrollable={false}
              data={validationErrors.warnings}
              columns={columns}
            />
          </>
        );
      }
    }
    return (
      <>
        {clientSideErrors()}
        {getCriticalErrors()}
        {getWarningErrors()}
      </>
    );
  }

  const renderRecommendedChangesAlert = (recommendedChanges: any) => {
    const totalRecommendations = Object.values(recommendedChanges);
    
    if (totalRecommendations.length === 0) {
      return null;
    }
    
    const selectedRecommendations = totalRecommendations.filter(
      (change: any) => change.isChanged
    );
    
    const recommendationsCount = selectedRecommendations.length;
    const uniqueServiceCount = new Set(
      selectedRecommendations
        .map((value: any) => value.serviceName)
        .filter(Boolean)
    ).size;

    const message = recommendationsCount > 0 
      ? `There are ${recommendationsCount} changes in ${uniqueServiceCount} services. `
      : `There are ${totalRecommendations.length} available recommendations (none selected). `;

    return (
      <Alert variant="warning">
        {message}
        <span
          className="text-info cursor-pointer"
          onClick={() => {
            setShowDependedConfigsModal(true);
          }}
        >
          More details
        </span>
      </Alert>
    );
  };

  const getValidationErrorCount = () => {
    return (
      validationErrors?.criticalErrors?.length +
      validationErrors?.clientSideErrors?.length +
      validationErrors?.warnings?.length
    );
  };

  const dependentChangesPopupColumns = [
    {
      header: () => (
        <Form.Check
          id="select-all-recommended-changes"
          checked={
            Object.values(recommendedChanges || {}).length > 0 &&
            Object.values(recommendedChanges || {}).every(
              (change: any) => change.isChanged
            )
          }
          onChange={() => {
            const allChecked = Object.values(recommendedChanges || {}).every(
              (change: any) => change.isChanged
            );

            // Apply the change to all recommendations
            Object.keys(recommendedChanges || {}).forEach((key) => {
              const recommendation = recommendedChanges[key];
              handleRecommendationChange(
                recommendation.propertyName,
                recommendation.fileName,
                !allChecked
              );
            });
          }}
        />
      ),
      id: "selectCheckbox",
      cell: ({ row }: { row: { original: any } }) => {
        const checkboxId = `recommended-change-${row.original.propertyName}-${row.original.fileName}`;
        return (
          <Form.Check
            id={checkboxId}
            checked={row.original.isChanged}
            onChange={() => {
              handleRecommendationChange(
                row.original.propertyName,
                row.original.fileName,
                !row.original.isChanged
              );
            }}
          />
        );
      },
    },

    {
      header: "Property",
      accessorKey: "propertyName",
      id: "propertyName",
    },
    {
      header: "Service",
      accessorKey: "serviceName",
      id: "serviceName",
    },
    {
      header: "Config Group",
      accessorKey: "configGroup",
      id: "configGroup",
    },
    {
      header: "File Name",
      accessorKey: "fileName",
      id: "fileName",
    },
    {
      header: "Original Value",
      accessorKey: "originalValue",
      id: "originalValue",
    },
    {
      header: "Recommended Value",
      accessorKey: "recommendedValue",
      id: "recommendedValue",
    },
  ];

  return (
    <>
      <Modal
        isOpen={showDependedConfigsModal}
        onClose={() => setShowDependedConfigsModal(false)}
        modalTitle="Dependent Configurations"
        modalBody={
          <>
            <h4>Recommended Changes</h4>
            <Alert variant="warning">
              Based on your configuration changes, Ambari is recommending the
              following dependent configuration changes. Ambari will update all
              checked configuration changes to the Recommended Value. Uncheck
              any configuration to retain the Current Value.
            </Alert>
            <Table
              columns={dependentChangesPopupColumns}
              data={Object.values(recommendedChanges || {})}
            />
          </>
        }
        successCallback={() => {
          setShowDependedConfigsModal(false);
        }}
        options={{}}
      />
      {showManageConfigGroupModal && (
        <ManageConfigGroups
          isOpen={showManageConfigGroupModal}
          onClose={() => setShowManageConfigGroupModal(false)}
          serviceName={chosenService}
          successCallback={() => {
            setShowManageConfigGroupModal(false);
            // Trigger refetch of config groups data to reflect changes
            setRefetchTrigger((prev) => prev + 1);
          }}
          clusterName={contextClusterName}
          hostNames={installer ? hosts?.join(",") : allHostNames?.join(",")}
        />
      )}
      <div className="d-flex justify-content-end mx-3 gap-2">
        <OverlayBackdrop isOpen={processingConfig} />
        {isFullyLoaded && !installer && (
          <>
            <InputGroup className="w-25 mb-1">
              <Form.Control
                type="text"
                placeholder="Filter..."
                value={searchString}
                onChange={(e) => {
                  setSearchString(e.target.value);
                }}
              />
              <Dropdown>
                <Dropdown.Toggle
                  variant="transparent"
                  className="btn-dropdown rounded-end-2 px-2 py-2"
                >
                  <span className="caret"></span>
                </Dropdown.Toggle>

                <Dropdown.Menu>
                  {propertyFilters.map((filter) => (
                    <Dropdown.Item
                      key={filter.id}
                      as="div"
                      onClick={(e) => {
                        e.preventDefault();
                        if (!filter.disabled) {
                          handleFilterToggle(filter.id);
                        }
                      }}
                      className={filter.disabled ? "disabled" : ""}
                    >
                      <div>
                        {filter.selected && (
                          <FontAwesomeIcon
                            icon={faCheckCircle}
                            className="text-success me-2"
                          />
                        )}
                        {filter.label}
                      </div>
                    </Dropdown.Item>
                  ))}
                  {propertyFilters.some((f) => f.selected) && (
                    <>
                      <Dropdown.Divider />
                      <Dropdown.Item onClick={handleClearFilters}>
                        Clear Filters
                      </Dropdown.Item>
                    </>
                  )}
                </Dropdown.Menu>
              </Dropdown>
            </InputGroup>
          </>
        )}
      </div>
      <div className="mx-3 mt-3">
        {!installer &&
          recommendedChanges &&
          renderRecommendedChangesAlert(recommendedChanges)}
      </div>
      <Card className="my-1 mx-3">
        <div>
          <Tab.Container activeKey={chosenService}>
            <Row className="mx-2">
              {servicesList.length > 1 && (
                <>
                  {hostConfigs ? (
                    <Col lg={3}>
                      <Card className="p-3 my-3">
                        <Card.Body className="p-0">
                          <div>
                            {services.map((serviceKey) => (
                              <div
                                key={serviceKey}
                                className={`btn ${
                                  chosenService === serviceKey
                                    ? "btn-primary"
                                    : "btn-outline-primary"
                                } mx-2 my-2`}
                                onClick={() => {
                                  setChosenService(serviceKey);
                                  setChosenTab(
                                    theme[serviceKey]?.tabs
                                      ? Object.keys(
                                          theme[serviceKey]?.tabs
                                        )?.[0]
                                      : "Advanced"
                                  );
                                  onServiceChange?.(serviceKey);
                                }}
                              >
                                {serviceKey}
                              </div>
                            ))}
                          </div>
                        </Card.Body>
                      </Card>
                    </Col>
                  ) : (
                    <Col lg={12} className="mt-2">
                      <Nav variant="underline" className="d-flex flex-row">
                        {services.map((serviceKey) => (
                          <Nav.Item
                            key={serviceKey}
                            onClick={() => {
                              setChosenService(serviceKey);
                              setChosenTab(
                                theme[serviceKey]?.tabs
                                  ? Object.keys(theme[serviceKey]?.tabs)?.[0]
                                  : "Advanced"
                              );
                              onServiceChange?.(serviceKey);
                            }}
                          >
                            <Nav.Link
                              eventKey={serviceKey}
                              as="div"
                              className="ambari-tabs nav-link nav-link-underlined ms-3"
                            >
                              {serviceKey}
                              {configSection === "default" && (
                                <span className="bg-danger rounded-pill badge ms-2">
                                  {tabErrors?.[serviceKey]?.total || ""}
                                </span>
                              )}
                            </Nav.Link>
                          </Nav.Item>
                        ))}
                      </Nav>
                    </Col>
                  )}
                </>
              )}
              <Col className="positin-relative">
                <Tab.Content>
                  {theme &&
                    services.map((serviceKey) => (
                      <Tab.Pane eventKey={serviceKey} key={serviceKey}>
                        <Row>
                          <Tab.Container activeKey={chosenTab}>
                            <Row className="mx-2">
                              {/* Config Group and Filter Controls - only for add service wizard */}
                              {installer &&
                                wizardName === "addService" &&
                                serviceKey !== "MISC" && (
                                  <Col lg={12} className="mt-3 mb-3">
                                    <div className="d-flex justify-content-end align-items-center gap-3">
                                      <ChooseConfigGroup
                                        serviceName={serviceKey}
                                        selectedConfigGroup={getSelectedConfigGroupForService(
                                          serviceKey
                                        )}
                                        onConfigGroupChange={(
                                          configGroup: string
                                        ) => {
                                          setSelectedConfigGroups((prev) => ({
                                            ...prev,
                                            [serviceKey]: configGroup,
                                          }));
                                        }}
                                        setShowManageConfigGroupModal={
                                          setShowManageConfigGroupModal
                                        }
                                        configGroupsData={configGroupsData}
                                        setConfigGroupsData={
                                          setConfigGroupsData
                                        }
                                        refetchTrigger={refetchTrigger}
                                        hostsList={hosts}
                                      />
                                      <div className="d-flex justify-content-end">
                                        <OverlayBackdrop
                                          isOpen={processingConfig}
                                        />
                                        {(isFullyLoaded || installer) && (
                                          <>
                                            <InputGroup className="w-100 mb-1">
                                              <Form.Control
                                                type="text"
                                                placeholder="Filter..."
                                                value={searchString}
                                                onChange={(e) => {
                                                  setSearchString(
                                                    e.target.value
                                                  );
                                                }}
                                              />
                                              <Dropdown>
                                                <Dropdown.Toggle
                                                  variant="transparent"
                                                  className="btn-dropdown rounded-end-2 px-2 py-2"
                                                >
                                                  <span className="caret"></span>
                                                </Dropdown.Toggle>

                                                <Dropdown.Menu>
                                                  {propertyFilters.map(
                                                    (filter) => (
                                                      <Dropdown.Item
                                                        key={filter.id}
                                                        as="div"
                                                        onClick={(e) => {
                                                          e.preventDefault();
                                                          if (
                                                            !filter.disabled
                                                          ) {
                                                            handleFilterToggle(
                                                              filter.id
                                                            );
                                                          }
                                                        }}
                                                        className={
                                                          filter.disabled
                                                            ? "disabled"
                                                            : ""
                                                        }
                                                      >
                                                        <div>
                                                          {filter.selected && (
                                                            <FontAwesomeIcon
                                                              icon={
                                                                faCheckCircle
                                                              }
                                                              className="text-success me-2"
                                                            />
                                                          )}
                                                          {filter.label}
                                                        </div>
                                                      </Dropdown.Item>
                                                    )
                                                  )}
                                                  {propertyFilters.some(
                                                    (f) => f.selected
                                                  ) && (
                                                    <>
                                                      <Dropdown.Divider />
                                                      <Dropdown.Item
                                                        onClick={
                                                          handleClearFilters
                                                        }
                                                      >
                                                        Clear Filters
                                                      </Dropdown.Item>
                                                    </>
                                                  )}
                                                </Dropdown.Menu>
                                              </Dropdown>
                                            </InputGroup>
                                          </>
                                        )}
                                      </div>
                                      <Dropdown>
                                        <Dropdown.Toggle
                                          variant="link"
                                          id="config-notifications"
                                        >
                                          <div className="d-flex align-items-center">
                                            <FontAwesomeIcon
                                              icon={faBell}
                                              className="fs-18 text-light"
                                            />
                                            {!isNaN(
                                              getValidationErrorCount()
                                            ) ? (
                                              <Badge className="bg-danger rounded-5">
                                                {getValidationErrorCount()}
                                              </Badge>
                                            ) : null}
                                          </div>
                                        </Dropdown.Toggle>

                                        <Dropdown.Menu className="configurations-dropdown">
                                          <Dropdown.Item>
                                            {getValidationNotificationsBody()}
                                          </Dropdown.Item>
                                        </Dropdown.Menu>
                                      </Dropdown>
                                    </div>
                                  </Col>
                                )}
                              {/* Filter Controls and Notifications - for cluster creation (no config groups) */}
                              {installer &&
                                wizardName === "clusterCreation" &&
                                serviceKey !== "MISC" && (
                                  <Col lg={12} className="mt-3 mb-3">
                                    <div className="d-flex justify-content-end align-items-center gap-3">
                                      <div className="d-flex justify-content-end">
                                        <OverlayBackdrop
                                          isOpen={processingConfig}
                                        />
                                        {(isFullyLoaded || installer) && (
                                          <>
                                            <InputGroup className="w-100 mb-1">
                                              <Form.Control
                                                type="text"
                                                placeholder="Filter..."
                                                value={searchString}
                                                onChange={(e) => {
                                                  setSearchString(
                                                    e.target.value
                                                  );
                                                }}
                                              />
                                              <Dropdown>
                                                <Dropdown.Toggle
                                                  variant="transparent"
                                                  className="btn-dropdown rounded-end-2 px-2 py-2"
                                                >
                                                  <span className="caret"></span>
                                                </Dropdown.Toggle>

                                                <Dropdown.Menu>
                                                  {propertyFilters.map(
                                                    (filter) => (
                                                      <Dropdown.Item
                                                        key={filter.id}
                                                        as="div"
                                                        onClick={(e) => {
                                                          e.preventDefault();
                                                          if (
                                                            !filter.disabled
                                                          ) {
                                                            handleFilterToggle(
                                                              filter.id
                                                            );
                                                          }
                                                        }}
                                                        className={
                                                          filter.disabled
                                                            ? "disabled"
                                                            : ""
                                                        }
                                                      >
                                                        <div>
                                                          {filter.selected && (
                                                            <FontAwesomeIcon
                                                              icon={
                                                                faCheckCircle
                                                              }
                                                              className="text-success me-2"
                                                            />
                                                          )}
                                                          {filter.label}
                                                        </div>
                                                      </Dropdown.Item>
                                                    )
                                                  )}
                                                  {propertyFilters.some(
                                                    (f) => f.selected
                                                  ) && (
                                                    <>
                                                      <Dropdown.Divider />
                                                      <Dropdown.Item
                                                        onClick={
                                                          handleClearFilters
                                                        }
                                                      >
                                                        Clear Filters
                                                      </Dropdown.Item>
                                                    </>
                                                  )}
                                                </Dropdown.Menu>
                                              </Dropdown>
                                            </InputGroup>
                                          </>
                                        )}
                                      </div>
                                      <Dropdown>
                                        <Dropdown.Toggle
                                          variant="link"
                                          id="config-notifications"
                                        >
                                          <div className="d-flex align-items-center">
                                            <FontAwesomeIcon
                                              icon={faBell}
                                              className="fs-18 text-light"
                                            />
                                            {!isNaN(
                                              getValidationErrorCount()
                                            ) ? (
                                              <Badge className="bg-danger rounded-5">
                                                {getValidationErrorCount()}
                                              </Badge>
                                            ) : null}
                                          </div>
                                        </Dropdown.Toggle>

                                        <Dropdown.Menu className="configurations-dropdown">
                                          <Dropdown.Item>
                                            {getValidationNotificationsBody()}
                                          </Dropdown.Item>
                                        </Dropdown.Menu>
                                      </Dropdown>
                                    </div>
                                  </Col>
                                )}
                              {installer && serviceKey === "MISC" && (
                                <Col lg={12} className="mt-3 mb-3">
                                  <div className="d-flex justify-content-end align-items-center gap-3">
                                    <Dropdown>
                                      <Dropdown.Toggle
                                        variant="link"
                                        id="config-notifications"
                                      >
                                        <div className="d-flex align-items-center">
                                          <FontAwesomeIcon
                                            icon={faBell}
                                            className="fs-18 text-light"
                                          />
                                          {!isNaN(getValidationErrorCount()) ? (
                                            <Badge className="bg-danger rounded-5">
                                              {getValidationErrorCount()}
                                            </Badge>
                                          ) : null}
                                        </div>
                                      </Dropdown.Toggle>

                                      <Dropdown.Menu className="configurations-dropdown">
                                        <Dropdown.Item>
                                          {getValidationNotificationsBody()}
                                        </Dropdown.Item>
                                      </Dropdown.Menu>
                                    </Dropdown>
                                  </div>
                                </Col>
                              )}

                              <Col lg={12} className="mt-3">
                                <Nav
                                  variant="underline"
                                  className="d-flex flex-row"
                                >
                                  {theme[serviceKey]?.tabs &&
                                    Object.keys(theme[serviceKey].tabs).length >
                                      1 &&
                                    Object.keys(theme[serviceKey].tabs).map(
                                      (tabName) => (
                                        <Nav.Item
                                          key={tabName}
                                          onClick={() => setChosenTab(tabName)}
                                        >
                                          <Nav.Link
                                            eventKey={tabName}
                                            as="div"
                                            className="ambari-tabs nav-link nav-link-underlined"
                                          >
                                            {
                                              theme[serviceKey].tabs[tabName]
                                                .displayName
                                            }{" "}
                                            <span className="bg-danger rounded-pill badge ms-2">
                                              {tabErrors?.[serviceKey]?.tabs[
                                                tabName
                                              ] || ""}
                                            </span>
                                          </Nav.Link>
                                        </Nav.Item>
                                      )
                                    )}
                                </Nav>
                              </Col>

                              <Col className="p-4">
                                <Tab.Content>
                                  {chosenTab !== "Advanced" ? (
                                    theme[serviceKey]?.tabs &&
                                    Object.keys(theme[serviceKey].tabs).map(
                                      (key) => (
                                        <Tab.Pane eventKey={key} key={key}>
                                          {!hasVisiblePropertiesInCurrentTab() &&
                                          searchString ? (
                                            <div className="bg-info-subtle p-4 text-center border rounded">
                                              <p className="text-muted mb-0">
                                                No properties match the current
                                                filter.
                                              </p>
                                            </div>
                                          ) : (
                                            <Row>
                                              {theme[serviceKey]?.tabs[key]
                                                ?.sections &&
                                                theme[serviceKey].tabs[
                                                  key
                                                ].sections.map(
                                                  (section: any) => (
                                                    <Col
                                                      className="p-1"
                                                      md={
                                                        (12 /
                                                          (theme[serviceKey]
                                                            ?.tabs[key]
                                                            ?.tabColumns ??
                                                            1)) *
                                                        section.columnSpan
                                                      }
                                                      key={section.name}
                                                    >
                                                      <Card className="p-2 h-100">
                                                        <div className="p-2">
                                                          {section.displayName}
                                                        </div>
                                                        <Row>
                                                          {section.subsections.map(
                                                            (
                                                              subsection: any
                                                            ) => {
                                                              const isVisible =
                                                                subsection?.[
                                                                  "depends-on"
                                                                ]
                                                                  ? evaluateDependsOn(
                                                                      subsection[
                                                                        "depends-on"
                                                                      ]
                                                                    )
                                                                  : true;

                                                              if (!isVisible)
                                                                return null;

                                                              return (
                                                                <Col
                                                                  md={
                                                                    (12 /
                                                                      section.sectionColumns) *
                                                                    subsection.columnSpan
                                                                  }
                                                                  key={
                                                                    subsection.name
                                                                  }
                                                                >
                                                                  <div className="p-3">
                                                                    {subsection.displayName &&
                                                                      subsection.displayName}
                                                                    {theme[
                                                                      serviceKey
                                                                    ]
                                                                      .subsectionProperties?.[
                                                                      subsection
                                                                        .name
                                                                    ]
                                                                      ?.properties && (
                                                                      <Row>
                                                                        {theme[
                                                                          serviceKey
                                                                        ].subsectionProperties[
                                                                          subsection
                                                                            .name
                                                                        ].properties.map(
                                                                          (
                                                                            config: any
                                                                          ) => {
                                                                            const type =
                                                                              config[
                                                                                "config"
                                                                              ].split(
                                                                                "/"
                                                                              )[0];
                                                                            const propertyName =
                                                                              config[
                                                                                "config"
                                                                              ].split(
                                                                                "/"
                                                                              )[1];
                                                                            const property =
                                                                              configProperties?.[
                                                                                serviceKey
                                                                              ]?.[
                                                                                type
                                                                              ]
                                                                                ?.properties[
                                                                                propertyName
                                                                              ];

                                                                            const isVisible =
                                                                              isArray(
                                                                                config?.[
                                                                                  "depends-on"
                                                                                ]
                                                                              )
                                                                                ? evaluateDependsOnForConfig(
                                                                                    configProperties,
                                                                                    serviceKey,
                                                                                    config[
                                                                                      "depends-on"
                                                                                    ],
                                                                                    installer
                                                                                      ? servicesList
                                                                                      : installedServices
                                                                                  )
                                                                                : true;
                                                                            if (
                                                                              !isVisible
                                                                            )
                                                                              return null;

                                                                            // Check subsection tab visibility if property has subsection-tab-name
                                                                            let isSubsectionTabVisible =
                                                                              true;
                                                                            if (
                                                                              config[
                                                                                "subsection-tab-name"
                                                                              ] &&
                                                                              subsection.subsectionTabs
                                                                            ) {
                                                                              const matchingSubsectionTab =
                                                                                subsection.subsectionTabs.find(
                                                                                  (
                                                                                    tab: any
                                                                                  ) =>
                                                                                    tab.name ===
                                                                                    config[
                                                                                      "subsection-tab-name"
                                                                                    ]
                                                                                );
                                                                              if (
                                                                                matchingSubsectionTab &&
                                                                                matchingSubsectionTab[
                                                                                  "depends-on"
                                                                                ]
                                                                              ) {
                                                                                isSubsectionTabVisible =
                                                                                  evaluateDependsOnForConfig(
                                                                                    configProperties,
                                                                                    serviceKey,
                                                                                    matchingSubsectionTab[
                                                                                      "depends-on"
                                                                                    ],
                                                                                    installer
                                                                                      ? servicesList
                                                                                      : installedServices
                                                                                  );
                                                                              }
                                                                            }

                                                                            return property &&
                                                                              property?.isVisible &&
                                                                              isSubsectionTabVisible ? (
                                                                              <Row className="mt-4">
                                                                                <Row>
                                                                                  <Col>
                                                                                    <Tooltip
                                                                                      message={
                                                                                        property.propertyDescription ||
                                                                                        property.description ||
                                                                                        property.property_description
                                                                                      }
                                                                                      heading={
                                                                                        property.propertyDisplayname ||
                                                                                        property.propertyName
                                                                                      }
                                                                                      placement="top"
                                                                                    >
                                                                                      <Form.Label
                                                                                        className={
                                                                                          property.hasError ||
                                                                                          property.errorMessage
                                                                                            ? "p-2 text-danger"
                                                                                            : "p-2"
                                                                                        }
                                                                                      >
                                                                                        {property.propertyDisplayname ||
                                                                                          property.propertyName}
                                                                                      </Form.Label>
                                                                                    </Tooltip>
                                                                                  </Col>
                                                                                </Row>
                                                                                <Row className="d-flex align-items-center">
                                                                                  <Col>
                                                                                    <Tooltip
                                                                                      message={
                                                                                        property.propertyDescription ||
                                                                                        property.description ||
                                                                                        property.property_description
                                                                                      }
                                                                                      heading={
                                                                                        property.propertyDisplayname ||
                                                                                        property.propertyName
                                                                                      }
                                                                                      placement="top"
                                                                                    >
                                                                                      <div>
                                                                                        {renderWidgets(
                                                                                          theme[
                                                                                            serviceKey
                                                                                          ]
                                                                                            .widgets[
                                                                                            propertyName
                                                                                          ]
                                                                                            .widget
                                                                                            .type,
                                                                                          {
                                                                                            ...property,
                                                                                            isEditable:
                                                                                              currentConfigGroup ===
                                                                                                "Default" &&
                                                                                              property.isEditable,
                                                                                          },
                                                                                          function (
                                                                                            e: any,
                                                                                            confirmPassword: boolean = false
                                                                                          ) {
                                                                                            handleInputChangeWidget(
                                                                                              type,
                                                                                              property,
                                                                                              e,
                                                                                              theme[
                                                                                                serviceKey
                                                                                              ]
                                                                                                .widgets[
                                                                                                propertyName
                                                                                              ]
                                                                                                .widget
                                                                                                .type,
                                                                                              confirmPassword
                                                                                            );
                                                                                          }
                                                                                        )}
                                                                                      </div>
                                                                                    </Tooltip>
                                                                                    {/* Display error message for main property */}
                                                                                    {property.errorMessage && (
                                                                                      <div className="mt-2 text-danger">
                                                                                        {property.errorMessage}
                                                                                      </div>
                                                                                    )}
                                                                                    {/* MySQL Warning for Ranger DB_FLAVOR - positioned after the dropdown */}
                                                                                    {serviceKey ===
                                                                                      "RANGER" &&
                                                                                      (chosenTab ===
                                                                                        "RANGER ADMIN" ||
                                                                                        chosenTab ===
                                                                                          "ranger_admin_settings") &&
                                                                                      property.propertyName ===
                                                                                        "DB_FLAVOR" &&
                                                                                      property.value ===
                                                                                        "MYSQL" && (
                                                                                        <div
                                                                                          className="mt-3"
                                                                                          style={{
                                                                                            backgroundColor:
                                                                                              "#fff3cd",
                                                                                            border:
                                                                                              "1px solid #ffeaa7",
                                                                                            borderRadius:
                                                                                              "4px",
                                                                                            padding:
                                                                                              "12px 15px",
                                                                                            color:
                                                                                              "#856404",
                                                                                            fontSize:
                                                                                              "14px",
                                                                                            lineHeight:
                                                                                              "1.4",
                                                                                          }}
                                                                                        >
                                                                                          To
                                                                                          use
                                                                                          MySQL
                                                                                          with
                                                                                          Ranger,
                                                                                          you
                                                                                          must
                                                                                          download
                                                                                          the{" "}
                                                                                          <a
                                                                                            href="https://dev.mysql.com/downloads/connector/j/"
                                                                                            target="_blank"
                                                                                            rel="noopener noreferrer"
                                                                                            style={{
                                                                                              color:
                                                                                                "#856404",
                                                                                              textDecoration:
                                                                                                "underline",
                                                                                            }}
                                                                                          >
                                                                                            https://dev.mysql.com/downloads/connector/j/
                                                                                          </a>{" "}
                                                                                          from
                                                                                          MySQL.
                                                                                          Once
                                                                                          downloaded
                                                                                          to
                                                                                          the
                                                                                          Ambari
                                                                                          Server
                                                                                          host,
                                                                                          run:
                                                                                          <br />
                                                                                          <span
                                                                                            style={{
                                                                                              backgroundColor:
                                                                                                "#ffeaa7",
                                                                                              padding:
                                                                                                "2px 4px",
                                                                                              borderRadius:
                                                                                                "3px",
                                                                                              fontFamily:
                                                                                                "monospace",
                                                                                              fontSize:
                                                                                                "13px",
                                                                                              color:
                                                                                                "#6c4e00",
                                                                                            }}
                                                                                          >
                                                                                            ambari-server
                                                                                            setup
                                                                                            --jdbc-db=mysql
                                                                                            --jdbc-driver=/path/to/mysql/mysql-connector-java.jar
                                                                                          </span>
                                                                                        </div>
                                                                                      )}
                                                                                  </Col>
                                                                                  <Col
                                                                                    md={
                                                                                      1
                                                                                    }
                                                                                  >
                                                                                    <Stack
                                                                                      direction="horizontal"
                                                                                      gap={
                                                                                        2
                                                                                      }
                                                                                    >
                                                                                      {!hostConfigs &&
                                                                                        property?.supportsFinal && (
                                                                                          <Tooltip
                                                                                            message={
                                                                                              property.final ===
                                                                                              "true"
                                                                                                ? "This property is marked as final and cannot be overridden. Click to make it overridable."
                                                                                                : "Click to mark this property as final (cannot be overridden by child configurations)"
                                                                                            }
                                                                                            placement="top"
                                                                                          >
                                                                                            <FontAwesomeIcon
                                                                                              icon={
                                                                                                faLock
                                                                                              }
                                                                                              className={
                                                                                                property.final ===
                                                                                                "true"
                                                                                                  ? "lock-selected"
                                                                                                  : "text-light pointer"
                                                                                              }
                                                                                              onClick={() => {
                                                                                                const configsCopy =
                                                                                                  cloneDeep(
                                                                                                    configProperties
                                                                                                  );
                                                                                                configsCopy[
                                                                                                  serviceKey
                                                                                                ][
                                                                                                  type
                                                                                                ].properties[
                                                                                                  propertyName
                                                                                                ].final =
                                                                                                  property.final ===
                                                                                                  "true"
                                                                                                    ? "false"
                                                                                                    : "true";
                                                                                                setConfigProperties(
                                                                                                  configsCopy
                                                                                                );
                                                                                              }}
                                                                                            />
                                                                                          </Tooltip>
                                                                                        )}
                                                                                      {property
                                                                                        .propertyAttributes
                                                                                        .overridable ===
                                                                                        false ||
                                                                                      (currentConfigGroup !==
                                                                                        "Default" &&
                                                                                        property?.overrideValues?.some(
                                                                                          (
                                                                                            overrideValue: any
                                                                                          ) =>
                                                                                            overrideValue.value !==
                                                                                            null
                                                                                        ))
                                                                                        ? null
                                                                                        : !hostConfigs &&
                                                                                          canEditConfigs && (
                                                                                            <Tooltip
                                                                                              message={
                                                                                                currentConfigGroup ===
                                                                                                "Default"
                                                                                                  ? "Add this property to a config group"
                                                                                                  : "Add override value for this config group"
                                                                                              }
                                                                                              placement="top"
                                                                                            >
                                                                                              <FontAwesomeIcon
                                                                                                className="text-primary pointer"
                                                                                                icon={
                                                                                                  faPlusCircle
                                                                                                }
                                                                                                onClick={() => {
                                                                                                  if (
                                                                                                    currentConfigGroup ===
                                                                                                    "Default"
                                                                                                  ) {
                                                                                                    setShowAddToGroupModal?.(
                                                                                                      true
                                                                                                    );
                                                                                                  } else {
                                                                                                    const configsCopy =
                                                                                                      cloneDeep(
                                                                                                        configProperties
                                                                                                      );
                                                                                                    if (
                                                                                                      !configsCopy[
                                                                                                        serviceKey
                                                                                                      ][
                                                                                                        type
                                                                                                      ]
                                                                                                        .properties[
                                                                                                        propertyName
                                                                                                      ]
                                                                                                        .overrideValues
                                                                                                    ) {
                                                                                                      configsCopy[
                                                                                                        serviceKey
                                                                                                      ][
                                                                                                        type
                                                                                                      ].properties[
                                                                                                        propertyName
                                                                                                      ].overrideValues =
                                                                                                        [];
                                                                                                    }
                                                                                                    configsCopy[
                                                                                                      serviceKey
                                                                                                    ][
                                                                                                      type
                                                                                                    ].properties[
                                                                                                      propertyName
                                                                                                    ].overrideValues.push(
                                                                                                      {
                                                                                                        value:
                                                                                                          property.value,
                                                                                                        groupName:
                                                                                                          currentConfigGroup,
                                                                                                      }
                                                                                                    );

                                                                                                    setConfigProperties(
                                                                                                      configsCopy
                                                                                                    );
                                                                                                  }
                                                                                                }}
                                                                                              />
                                                                                            </Tooltip>
                                                                                          )}
                                                                                      {displayUndoRedo &&
                                                                                      property.value !==
                                                                                        property.previousValue &&
                                                                                      canEditConfigs &&
                                                                                      currentConfigGroup ===
                                                                                        "Default" ? (
                                                                                        <FontAwesomeIcon
                                                                                          className="text-light pointer"
                                                                                          icon={
                                                                                            faUndo
                                                                                          }
                                                                                          onClick={() =>
                                                                                            handleUndo(
                                                                                              type,
                                                                                              property
                                                                                            )
                                                                                          }
                                                                                        />
                                                                                      ) : null}
                                                                                      {displayUndoRedo &&
                                                                                        canEditConfigs &&
                                                                                        currentConfigGroup ===
                                                                                          "Default" && (
                                                                                          <Tooltip
                                                                                            message="Reset to default value"
                                                                                            placement="top"
                                                                                          >
                                                                                            <FontAwesomeIcon
                                                                                              className="text-light pointer"
                                                                                              icon={
                                                                                                faRedo
                                                                                              }
                                                                                              onClick={() =>
                                                                                                setToDefault(
                                                                                                  type,
                                                                                                  property
                                                                                                )
                                                                                              }
                                                                                            />
                                                                                          </Tooltip>
                                                                                        )}
                                                                                    </Stack>
                                                                                  </Col>
                                                                                </Row>
                                                                                {property.overrideValues &&
                                                                                Array.isArray(
                                                                                  property.overrideValues
                                                                                ) &&
                                                                                property
                                                                                  .overrideValues
                                                                                  .length >
                                                                                  0
                                                                                  ? property.overrideValues.map(
                                                                                      (
                                                                                        overrideValue: configGroupOverrides,
                                                                                        index: number
                                                                                      ) => {
                                                                                        // Only render if value is not null
                                                                                        if (
                                                                                          overrideValue.value ===
                                                                                          null
                                                                                        ) {
                                                                                          return null;
                                                                                        }

                                                                                        return (
                                                                                          <Row
                                                                                            key={
                                                                                              index
                                                                                            }
                                                                                            className="mt-5"
                                                                                          >
                                                                                            <Col>
                                                                                              {renderWidgets(
                                                                                                theme[
                                                                                                  serviceKey
                                                                                                ]
                                                                                                  .widgets[
                                                                                                  propertyName
                                                                                                ]
                                                                                                  .widget
                                                                                                  .type,
                                                                                                {
                                                                                                  ...property,
                                                                                                  value:
                                                                                                    overrideValue.value,
                                                                                                  isEditable:
                                                                                                    currentConfigGroup !==
                                                                                                    "Default",
                                                                                                },
                                                                                                function (
                                                                                                  e: any,
                                                                                                  confirmPassword: boolean = false
                                                                                                ) {
                                                                                                  handleInputChangeWidgetForOverrideValues(
                                                                                                    type,
                                                                                                    property,
                                                                                                    e,
                                                                                                    theme[
                                                                                                      serviceKey
                                                                                                    ]
                                                                                                      .widgets[
                                                                                                      propertyName
                                                                                                    ]
                                                                                                      .widget
                                                                                                      .type,
                                                                                                    index,
                                                                                                    confirmPassword
                                                                                                  );
                                                                                                }
                                                                                              )}
                                                                                              {overrideValue.errorMessage ? (
                                                                                                <Col className="mt-2 text-danger">
                                                                                                  {
                                                                                                    overrideValue.errorMessage
                                                                                                  }
                                                                                                </Col>
                                                                                              ) : null}
                                                                                            </Col>
                                                                                            <Col
                                                                                              md={
                                                                                                1
                                                                                              }
                                                                                            >
                                                                                              {currentConfigGroup ===
                                                                                              "Default" ? (
                                                                                                <h4
                                                                                                  className="text-info"
                                                                                                  onClick={() => {
                                                                                                    if (
                                                                                                      installer
                                                                                                    ) {
                                                                                                      setSelectedConfigGroups(
                                                                                                        (
                                                                                                          prev
                                                                                                        ) => ({
                                                                                                          ...prev,
                                                                                                          [serviceKey]:
                                                                                                            overrideValue.groupName,
                                                                                                        })
                                                                                                      );
                                                                                                    } else {
                                                                                                      setConfigGroup?.(
                                                                                                        overrideValue.groupName
                                                                                                      );
                                                                                                    }
                                                                                                  }}
                                                                                                >
                                                                                                  Switch
                                                                                                  to{" "}
                                                                                                  {
                                                                                                    overrideValue.groupName
                                                                                                  }
                                                                                                </h4>
                                                                                              ) : (
                                                                                                <Stack
                                                                                                  direction="horizontal"
                                                                                                  gap={
                                                                                                    2
                                                                                                  }
                                                                                                >
                                                                                                  <FontAwesomeIcon
                                                                                                    className="text-danger pointer"
                                                                                                    icon={
                                                                                                      faMinusCircle
                                                                                                    }
                                                                                                    onClick={() => {
                                                                                                      const configsCopy =
                                                                                                        cloneDeep(
                                                                                                          configProperties
                                                                                                        );
                                                                                                      configsCopy[
                                                                                                        serviceKey
                                                                                                      ][
                                                                                                        type
                                                                                                      ].properties[
                                                                                                        propertyName
                                                                                                      ].overrideValues[
                                                                                                        index
                                                                                                      ].value =
                                                                                                        null;

                                                                                                      // Recalculate error counts after removing override
                                                                                                      const validatedConfigs =
                                                                                                        validateAllProperties(
                                                                                                          configsCopy
                                                                                                        );
                                                                                                      setConfigProperties(
                                                                                                        validatedConfigs
                                                                                                      );
                                                                                                    }}
                                                                                                  />
                                                                                                </Stack>
                                                                                              )}
                                                                                            </Col>
                                                                                          </Row>
                                                                                        );
                                                                                      }
                                                                                    )
                                                                                  : null}
                                                                              </Row>
                                                                            ) : (
                                                                              <Row className="mt-4">
                                                                                {renderUIOnlyWidgets(
                                                                                  theme[
                                                                                    serviceKey
                                                                                  ]
                                                                                    .widgets[
                                                                                    propertyName
                                                                                  ]
                                                                                    .widget
                                                                                )}
                                                                              </Row>
                                                                            );
                                                                          }
                                                                        )}
                                                                      </Row>
                                                                    )}
                                                                  </div>
                                                                </Col>
                                                              );
                                                            }
                                                          )}
                                                        </Row>
                                                      </Card>
                                                    </Col>
                                                  )
                                                )}
                                            </Row>
                                          )}
                                        </Tab.Pane>
                                      )
                                    )
                                  ) : (
                                    <Tab.Pane eventKey={"Advanced"}>
                                      {chosenTab === "Advanced" ? (
                                        versionLoading ? (
                                          <Spinner />
                                        ) : (
                                          <div className="mt-4">
                                            <AdvancedConfigs
                                              chosenService={chosenService}
                                              setTabErrors={setTabErrors}
                                              setConfigProperties={
                                                setConfigProperties
                                              }
                                              configPropertiesData={
                                                configProperties
                                              }
                                              displayUndoRedo={displayUndoRedo}
                                              configGroup={currentConfigGroup}
                                              setShowAddToGroupModal={
                                                setShowAddToGroupModal
                                              }
                                              setConfigGroup={
                                                installer
                                                  ? (groupName: string) =>
                                                      setSelectedConfigGroups(
                                                        (prev) => ({
                                                          ...prev,
                                                          [chosenService]:
                                                            groupName,
                                                        })
                                                      )
                                                  : setConfigGroup
                                              }
                                              hostConfigs={hostConfigs}
                                              installedServices={
                                                installedServices
                                              }
                                              installer={installer}
                                              recommendationsDataToSend={
                                                recommendationsDataToSend
                                              }
                                              stack={stack}
                                              stackVersion={stackVersion}
                                              hosts={hosts}
                                              onValueUpdateProp={onValueUpdate}
                                              searchString={searchString}
                                            />
                                          </div>
                                        )
                                      ) : null}
                                    </Tab.Pane>
                                  )}
                                </Tab.Content>
                              </Col>
                            </Row>
                          </Tab.Container>
                        </Row>
                      </Tab.Pane>
                    ))}
                </Tab.Content>
              </Col>
            </Row>
          </Tab.Container>
        </div>
      </Card>
    </>
  );
}
