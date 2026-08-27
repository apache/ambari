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

import { useEffect, useState, useTransition } from "react";
import type { KeyboardEvent } from "react";
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
import { cloneDeep, get, isEmpty } from "lodash";
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
import { PropertyType, configGroupOverrides, TruthValues } from "./types";
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
  getThemePlacementProperty,
} from "./ConfigUtils";
import useEnhancedConfigs from "../../hooks/useEnhancedConfigs";
import OverlayBackdrop from "../../components/OverlayBackdrop";
import {
  widgetValueByConfigAttributes,
  configValueByWidget,
  formatTickLabel,
  getDisplayUnitLabel,
  getConfigUnitInfo,
  convertValue,
  parseTimeInterval,
  composeTimeInterval,
} from "../../Utils/unitConversionUtils";
import Modal from "../../components/Modal";
import Table from "../../components/Table";
import useAuthorizationPolicy from "../../hooks/useAuthorizationPolicy";
import Tooltip from "../../components/Tooltip";
import { useContext } from "react";
import { AppContext } from "../../store/context";
import { useDebounce } from "../../hooks/useDebounce";
import {
  formatParamsForDisplay,
  formatParamsForSave,
  shouldUseMultilineFormatting,
} from "../../Utils/jvmFormatUtils";
import {
  ConfigThemeView,
  findThemeConfigProperty,
  normalizeDefaultThemeResponse,
  normalizeThemeResponse,
  resolveThemeConditionAttributes,
  ThemePlacement,
  ThemeWidget,
  toConfigThemeView,
} from "./themeEngine";
import {
  ThemeDirectoriesControl,
  ThemeDirectoryControl,
  ThemeLabelControl,
  ThemeListControl,
  ThemeRadioControl,
} from "./ThemeWidgetControls";
import {
  areThemeEntriesEditable,
  getUnsupportedThemeEntryValues,
  getThemeCheckboxState,
  getThemeWidgetEntries,
  isThemeCheckboxValueSupported,
} from "./themeWidgetUtils";

dayjs.extend(duration);

const isThemeAttributeTrue = (value: unknown) =>
  value === true || value === 1 || value === "1" || value === "true";

const isThemeAttributeFalse = (value: unknown) =>
  value === false || value === 0 || value === "0" || value === "false";

const operateThemeTabs = (event: KeyboardEvent<HTMLElement>) => {
  if (!["ArrowLeft", "ArrowRight", "Home", "End"].includes(event.key)) {
    return;
  }

  const tabList = event.currentTarget.closest('[role="tablist"]');
  const tabs = Array.from(
    tabList?.querySelectorAll<HTMLElement>(
      '[role="tab"]:not([aria-disabled="true"])',
    ) ?? [],
  );
  const currentIndex = tabs.indexOf(event.currentTarget);
  if (currentIndex < 0 || tabs.length < 2) return;

  event.preventDefault();
  const nextIndex =
    event.key === "Home"
      ? 0
      : event.key === "End"
        ? tabs.length - 1
        : event.key === "ArrowRight"
          ? (currentIndex + 1) % tabs.length
          : (currentIndex - 1 + tabs.length) % tabs.length;
  tabs[nextIndex].focus();
  tabs[nextIndex].click();
};

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
  allThemes?: boolean;
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
  allThemes = false,
}: ConfigProps) {
  const [chosenService, setChosenService] = useState<string>("");
  const [chosenTab, setChosenTab] = useState<string>("");
  const [theme, setTheme] = useState<ConfigThemeView>({});
  const [services, setServices] = useState<string[]>([]);
  const [tabErrors, setTabErrors] = useState<any>({});
  const [versionLoading, setVersionLoading] = useState(false);
  const [searchString, setSearchString] = useState("");
  const [widgetTextModeMap, setWidgetTextModeMap] = useState<
    Record<string, boolean>
  >({});
  const [activeSubsectionTabs, setActiveSubsectionTabs] = useState<
    Record<string, string>
  >({});
  // @ts-ignore
  const [isFullyLoaded, setIsFullyLoaded] = useState(false);
  const [isServiceSwitching, setIsServiceSwitching] = useState(false);
  const { isAuthorized } = useAuthorizationPolicy();
  const canEditConfigs = isAuthorized("SERVICE.MODIFY_CONFIGS");
  const canEditConfigsInContext = installer || canEditConfigs;
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
    hosts,
  );

  const handleRecommendationChange = (
    propertyName: string,
    fileName: string,
    isChecked: boolean,
  ) => {
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
          if (
            !newConfigs[serviceName][configType] ||
            !newConfigs[serviceName][configType].properties
          )
            continue;

          for (const propKey in newConfigs[serviceName][configType]
            .properties) {
            const property =
              newConfigs[serviceName][configType].properties[propKey];
            if (
              property &&
              property.propertyName === propertyName &&
              property.fileName === fileName
            ) {
              if (isChecked) {
                // Apply recommended value
                property.value = updatedChanges[key].recommendedValue;
              } else {
                // Revert to original value
                const originalValue =
                  updatedChanges[key].initialValue ||
                  updatedChanges[key].originalValue;
                property.value = originalValue;
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
        setConfigProperties(newConfigs);
      }
    }

    setRecommendedChanges(updatedChanges);
  };

  useEffect(() => {
    if (version === undefined || version === null) {
      setVersionLoading(false);
      return;
    }
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
        }),
      );
    }
  }, [configGroup, installer]);

  const handleFilterToggle = (filterId: string) => {
    setPropertyFilters((prev) =>
      prev.map((filter) =>
        filter.id === filterId
          ? { ...filter, selected: !filter.selected }
          : filter,
      ),
    );
  };

  const handleClearFilters = () => {
    setPropertyFilters((prev) =>
      prev.map((filter) => ({ ...filter, selected: false })),
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
            },
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
        activeFilters,
      );
    }

    configsCopy = updateVisibilityForDependsOn(
      configsCopy,
      themeData,
      configSection,
      installedServices || [],
      allThemes,
    );

    setConfigProperties(configsCopy);
  };

  // Create debounced version of the filter function
  const debouncedApplyFilters = useDebounce(applyFilters, 300);

  useEffect(() => {
    debouncedApplyFilters(searchString, propertyFilters);
  }, [searchString, propertyFilters, debouncedApplyFilters]);

  const getTheme = () => {
    const requestedServices = servicesList.filter(
      (service) => !(installer && service === "KERBEROS"),
    );
    if (
      configSection === "default" &&
      installer &&
      !requestedServices.includes("MISC")
    ) {
      requestedServices.push("MISC");
    }

    const normalized = allThemes
      ? normalizeDefaultThemeResponse(themeData, requestedServices)
      : normalizeThemeResponse(themeData, configSection, requestedServices);
    const nextTheme = toConfigThemeView(normalized);
    const firstService = normalized.services[0] ?? "";

    setTheme(nextTheme);
    setServices(normalized.services);
    setChosenService((currentService) =>
      normalized.services.includes(currentService)
        ? currentService
        : firstService,
    );
    setChosenTab((currentTab) => {
      const service = normalized.services.includes(chosenService)
        ? chosenService
        : firstService;
      const serviceTabs = Object.keys(nextTheme[service]?.tabs ?? {});
      return serviceTabs.includes(currentTab)
        ? currentTab
        : (serviceTabs[0] ?? "");
    });
  };

  useEffect(() => {
    startTransition(() => {
      setTabErrors(setTabErrorCounts(configProperties));
    });
  }, [configProperties]);

  useEffect(() => {
    getTheme();
  }, [
    themeData,
    configPropertiesData,
    configSection,
    allThemes,
    JSON.stringify(servicesList),
  ]);

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
            .length > 0,
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

  const evaluateDependsOn = (dependsOn: any): boolean => {
    return evaluateDependsOnForConfig(
      configProperties,
      chosenService,
      dependsOn,
      installer ? servicesList : installedServices,
    );
  };

  const getPlacementErrorCount = (
    serviceName: string,
    placements: ThemePlacement[],
  ) =>
    placements.filter((placement) => {
      const sourceProperty = findThemeConfigProperty(
        configProperties,
        serviceName,
        placement.configPath,
      )?.property;
      const property = sourceProperty
        ? getThemePlacementProperty(sourceProperty, placement.id)
        : undefined;
      if (!property || property.isVisible === false || property.isHidden) {
        return false;
      }
      const placementIsVisible = placement.dependsOn.length
        ? evaluateDependsOnForConfig(
            configProperties,
            serviceName,
            placement.dependsOn,
            installer ? servicesList : installedServices,
          )
        : true;
      return (
        placementIsVisible && Boolean(property.hasError || property.errorMessage)
      );
    }).length;

  const hasVisiblePropertiesInTab = (serviceName: string, tabName: string) => {
    if (tabName === "Advanced") {
      // For Advanced tab, check if AdvancedConfigs has any visible properties
      return (
        configProperties[serviceName] &&
        Object.keys(configProperties[serviceName]).some((config) => {
          if (config.includes("Custom") && config.endsWith("env")) {
            return false;
          }
          const currentConfigValue = configProperties[serviceName][config];
          const filteredPropertiesCount = Object.keys(
            currentConfigValue.properties || {},
          ).filter(
            (property) =>
              !currentConfigValue.properties[property].tabName &&
              currentConfigValue.properties[property].isVisible !== false &&
              !!!currentConfigValue.properties[property].isHidden,
          ).length;
          return filteredPropertiesCount > 0;
        })
      );
    } else {
      // For themed tabs, check if any section has visible properties
      return theme[serviceName]?.tabs[tabName]?.sections?.some(
        (section: any) => {
          return section.subsections.some((subsection: any) => {
            const isVisible = subsection.dependsOn.length
              ? evaluateDependsOnForConfig(
                  configProperties,
                  serviceName,
                  subsection.dependsOn,
                  installer ? servicesList : installedServices,
                )
              : true;
            if (!isVisible) return false;

            const visibleSubsectionTabs = subsection.tabs.filter(
              (tab: any) =>
                tab.dependsOn.length === 0 ||
                evaluateDependsOnForConfig(
                  configProperties,
                  serviceName,
                  tab.dependsOn,
                  installer ? servicesList : installedServices,
                ),
            );
            const requestedTab = activeSubsectionTabs[subsection.id];
            const activeTab =
              visibleSubsectionTabs.find(
                (tab: any) => tab.name === requestedTab,
              ) ?? visibleSubsectionTabs[0];
            const placements = [
              ...subsection.placements,
              ...(activeTab?.placements ?? []),
            ];
            const hasProperties = placements.some((config: ThemePlacement) => {
              const sourceProperty = findThemeConfigProperty(
                configProperties,
                serviceName,
                config.configPath,
              )?.property;
              const property = sourceProperty
                ? getThemePlacementProperty(sourceProperty, config.id)
                : undefined;

              const isPropertyVisible = config.dependsOn.length
                ? evaluateDependsOnForConfig(
                    configProperties,
                    serviceName,
                    config.dependsOn,
                    installer ? servicesList : installedServices,
                  )
                : true;

              return property && property?.isVisible && isPropertyVisible;
            });

            return hasProperties;
          });
        },
      );
    }
  };

  const hasVisiblePropertiesInCurrentTab = () =>
    hasVisiblePropertiesInTab(chosenService, chosenTab);

  useEffect(() => {
    const tabNames = Object.keys(theme[chosenService]?.tabs ?? {});
    if (tabNames.length === 0) return;
    const visibleTabNames = tabNames.filter((tabName) =>
      hasVisiblePropertiesInTab(chosenService, tabName),
    );
    if (visibleTabNames.includes(chosenTab)) return;
    setChosenTab(visibleTabNames[0] ?? tabNames[0]);
  }, [chosenService, chosenTab, configProperties, theme]);

  const renderWidgets = (
    widgetType: string,
    property: PropertyType,
    onChange: any,
    widgetStateKey: string,
  ) => {
    switch (widgetType) {
      case "directory":
        return (
          <ThemeDirectoryControl property={property} onChange={onChange} />
        );
      case "directories":
        return (
          <ThemeDirectoriesControl property={property} onChange={onChange} />
        );
      case "slider":
        // Get proper unit information using the helper function
        const unitInfo = getConfigUnitInfo(property);
        const { configUnit, widgetUnit, dimensionType, configType } = unitInfo;
        const defaultValueAttributes = property.propertyAttributes ?? {};
        const groupValueAttributes =
          currentConfigGroup !== "Default" &&
          typeof defaultValueAttributes[currentConfigGroup] === "object" &&
          defaultValueAttributes[currentConfigGroup] !== null
            ? defaultValueAttributes[currentConfigGroup]
            : {};
        const getSliderAttribute = (name: string, fallback: number) => {
          const rawValue =
            groupValueAttributes[name] ?? defaultValueAttributes[name];
          const numericValue = Number(rawValue);
          return Number.isFinite(numericValue) ? numericValue : fallback;
        };

        // Convert boundaries and step from config units to widget units
        let minimum = widgetValueByConfigAttributes(
          getSliderAttribute("minimum", 0),
          configUnit,
          widgetUnit,
          dimensionType,
        );
        let maximum = widgetValueByConfigAttributes(
          getSliderAttribute("maximum", 100),
          configUnit,
          widgetUnit,
          dimensionType,
        );
        const configuredStep = getSliderAttribute(
          "increment_step",
          configType === "int" ? 1 : 0.1,
        );
        let step = widgetValueByConfigAttributes(
          configuredStep > 0
            ? configuredStep
            : configType === "int"
              ? 1
              : 0.1,
          configUnit,
          widgetUnit,
          dimensionType,
        );

        // Convert current value from config units to widget units
        let value = widgetValueByConfigAttributes(
          Number(property.value) || 0,
          configUnit,
          widgetUnit,
          dimensionType,
        );

        // Get display unit label (will be empty for int/float)
        const displayUnit = getDisplayUnitLabel(widgetUnit);
        const rawDisplayUnit = getDisplayUnitLabel(configUnit);

        // Create marks with proper formatting
        const marks = {
          [minimum]: formatTickLabel(
            minimum,
            displayUnit,
            displayUnit ? " " : "",
          ),
          [minimum + (maximum - minimum) / 2]: formatTickLabel(
            minimum + (maximum - minimum) / 2,
            displayUnit,
            displayUnit ? " " : "",
          ),
          [maximum]: formatTickLabel(
            maximum,
            displayUnit,
            displayUnit ? " " : "",
          ),
        };

        // Check if this slider is in text input mode
        const isTextMode = widgetTextModeMap[widgetStateKey] || false;

        // Toggle between slider and text input mode
        const toggleMode = () => {
          setWidgetTextModeMap((prev) => ({
            ...prev,
            [widgetStateKey]: !prev[widgetStateKey],
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
                {rawDisplayUnit && (
                  <InputGroup.Text>{rawDisplayUnit}</InputGroup.Text>
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
                      dimensionType,
                    );
                    onChange(configValue);
                  }}
                  disabled={!property.isEditable}
                  propertyUnit={displayUnit}
                />
              </div>
            )}
            {!hostConfigs && canEditConfigsInContext && (
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
            className={
              property.unit || property.propertyAttributes.unit
                ? "w-50"
                : "w-100"
            }
          >
            <Form.Control
              type="text"
              value={property.value}
              onChange={(e) => onChange(e.target.value)}
              placeholder={property.propertyValue}
              disabled={!property.isEditable}
            />
            {property.unit || property.propertyAttributes.unit ? (
              <InputGroup.Text>
                {property.unit || property.propertyAttributes.unit}
              </InputGroup.Text>
            ) : null}
          </InputGroup>
        );

      case "combo":
        // Check if this combo is in text input mode
        const isComboTextMode = widgetTextModeMap[widgetStateKey] || false;

        const allowSwitchToTextBox = areThemeEntriesEditable(property);
        const comboEntries = getThemeWidgetEntries(property);
        const hasUnsupportedComboValue =
          getUnsupportedThemeEntryValues(property).length > 0;
        const showComboTextMode = isComboTextMode || hasUnsupportedComboValue;

        // Toggle between combo and text input mode
        const toggleComboMode = () => {
          setWidgetTextModeMap((prev) => ({
            ...prev,
            [widgetStateKey]: !prev[widgetStateKey],
          }));
        };

        return (
          <div className="d-flex align-items-center">
            {showComboTextMode ? (
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
                  options={comboEntries}
                  isDisabled={!property.isEditable}
                />
              </div>
            )}
            {!hostConfigs &&
              !hasUnsupportedComboValue &&
              allowSwitchToTextBox &&
              canEditConfigsInContext && (
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
          property?.unit ||
          property?.propertyAttributes?.unit ||
          "milliseconds";

        // Parse the time interval using the unit conversion utilities
        const timeComponents = parseTimeInterval(
          Number(property.value) || 0,
          timeConfigUnit,
        );

        // Check if this time-interval is in text input mode
        const isTimeTextMode = widgetTextModeMap[widgetStateKey] || false;

        // Toggle between time-interval and text input mode
        const toggleTimeMode = () => {
          setWidgetTextModeMap((prev) => ({
            ...prev,
            [widgetStateKey]: !prev[widgetStateKey],
          }));
        };

        const configuredTimeUnits = String(
          property.widget?.units?.[0]?.["unit-name"] ||
            property.widget?.units?.[0]?.unit ||
            "days,hours,minutes,seconds",
        )
          .split(",")
          .map((unit) => unit.trim().toLowerCase())
          .filter((unit) =>
            ["days", "hours", "minutes", "seconds", "milliseconds"].includes(
              unit,
            ),
          );
        const timeUnitValues = {
          days: timeComponents.days,
          hours: timeComponents.hours,
          minutes: timeComponents.minutes,
          seconds: timeComponents.seconds,
          milliseconds: timeComponents.milliseconds,
        };
        const timeUnitLabels = {
          days: "Days",
          hours: "Hours",
          minutes: "Minutes",
          seconds: "Seconds",
          milliseconds: "Milliseconds",
        };
        const timeUnitMaximums = {
          days: 365,
          hours: 23,
          minutes: 59,
          seconds: 59,
          milliseconds: 999,
        };
        const updateTimeUnit = (
          unit: keyof typeof timeUnitValues,
          nextValue: number,
        ) => {
          onChange(
            composeTimeInterval(
              unit === "days" ? nextValue : timeComponents.days,
              unit === "hours" ? nextValue : timeComponents.hours,
              unit === "minutes" ? nextValue : timeComponents.minutes,
              unit === "seconds" ? nextValue : timeComponents.seconds,
              timeConfigUnit,
              unit === "milliseconds" ? nextValue : timeComponents.milliseconds,
            ),
          );
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
                {configuredTimeUnits.map((unit, index) => {
                  const typedUnit = unit as keyof typeof timeUnitValues;
                  return (
                    <div className="d-flex flex-column me-2" key={typedUnit}>
                      <Form.Control
                        type="number"
                        aria-label={timeUnitLabels[typedUnit]}
                        min={0}
                        max={timeUnitMaximums[typedUnit]}
                        step={
                          index === configuredTimeUnits.length - 1
                            ? Number(
                                convertValue(
                                  Number(
                                    property.propertyAttributes
                                      ?.increment_step || 1,
                                  ),
                                  timeConfigUnit,
                                  typedUnit,
                                ),
                              ) || 1
                            : 1
                        }
                        value={timeUnitValues[typedUnit]}
                        onChange={(event) => {
                          const nextValue = Number(event.target.value);
                          if (Number.isFinite(nextValue)) {
                            updateTimeUnit(typedUnit, nextValue);
                          }
                        }}
                        disabled={!property.isEditable}
                      />
                      <small>{timeUnitLabels[typedUnit]}</small>
                    </div>
                  );
                })}
              </div>
            )}
            {!hostConfigs && canEditConfigsInContext && (
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
            )}
          </div>
        );

      case "toggle":
        const entries = getThemeWidgetEntries(property);
        const hasUnsupportedToggleValue =
          entries.length < 2 ||
          getUnsupportedThemeEntryValues(property).length > 0;
        const valueLabel =
          entries.find(
            (entry: { value: string }) => entry.value === property.value,
          )?.label || property.value;

        // Assume there are always two options available for toggle
        // Check if current value matches the first option (checked/Yes state)
        const isChecked =
          entries.length >= 2 ? property.value === entries[0].value : false;

        // Check if this toggle is in text input mode
        const isToggleTextMode = widgetTextModeMap[widgetStateKey] || false;

        // Toggle between switch and text input mode
        const toggleToggleMode = () => {
          setWidgetTextModeMap((prev) => ({
            ...prev,
            [widgetStateKey]: !prev[widgetStateKey],
          }));
        };

        return (
          <div className="d-flex align-items-center">
            {isToggleTextMode || hasUnsupportedToggleValue ? (
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
            {!hostConfigs &&
              !hasUnsupportedToggleValue &&
              canEditConfigsInContext && (
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
              )}
          </div>
        );

      case "checkbox":
        const checkboxId = `config-checkbox-${widgetStateKey}`;
        if (!isThemeCheckboxValueSupported(property)) {
          return (
            <Form.Control
              id={checkboxId}
              type="text"
              value={String(property.value ?? "")}
              aria-label={property.propertyDisplayname || property.propertyName}
              onChange={(event) => onChange(event.target.value)}
              disabled={!property.isEditable}
            />
          );
        }
        const checkboxState = getThemeCheckboxState(property);
        return (
          <Form.Check
            id={checkboxId}
            checked={checkboxState.checked}
            onChange={onChange}
            disabled={!property.isEditable}
          />
        );
      case "list":
        return <ThemeListControl property={property} onChange={onChange} />;
      case "radio-buttons":
        return <ThemeRadioControl property={property} onChange={onChange} />;
      case "label":
        return <ThemeLabelControl property={property} />;
      case "text-area":
        // Check if this property should use multiline formatting
        const useMultilineFormatting = shouldUseMultilineFormatting(
          property.value,
          property.propertyAttributes?.type,
        );
        const displayValue = useMultilineFormatting
          ? formatParamsForDisplay(
              property.value,
              property.propertyAttributes?.type,
            )
          : property.value;

        return (
          <Form.Control
            as="textarea"
            rows={10}
            value={displayValue}
            onChange={(e) => {
              // Format the value for saving if it's a multiline config
              const valueToSave = useMultilineFormatting
                ? formatParamsForSave(e.target.value)
                : e.target.value;
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
      case "text":
        return (
          <InputGroup
            className={
              property.unit || property.propertyAttributes.unit
                ? "w-50"
                : "w-100"
            }
          >
            <Form.Control
              type="text"
              value={property.value}
              onChange={(e) => onChange(e.target.value)}
              placeholder={property.propertyValue}
              disabled={!property.isEditable}
            />
            {property.unit || property.propertyAttributes.unit ? (
              <InputGroup.Text>
                {property.unit || property.propertyAttributes.unit}
              </InputGroup.Text>
            ) : null}
          </InputGroup>
        );
      default:
        return (
          <Alert variant="warning" role="status" className="mb-0">
            Unsupported Theme widget type: <strong>{widgetType}</strong>. This
            property is available from Advanced configurations.
          </Alert>
        );
    }
  };

  const renderUIOnlyWidgets = (
    widget: ThemeWidget,
    actionsDisabled: boolean,
  ) => {
    switch (widget.type) {
      case "test-db-connection":
        return (
          <TestConnection
            buttonLabel={widget.displayName || "Test DB Connection"}
            serviceName={chosenService}
            configProperties={configProperties}
            requiredProperties={widget.requiredProperties}
            disabled={actionsDisabled}
          />
        );
      case "label":
        return <span>{widget.displayName || widget.configPath}</span>;
      default:
        return (
          <Alert variant="warning" role="status" className="mb-0">
            Unsupported Theme widget type: <strong>{widget.type}</strong>.
          </Alert>
        );
    }
  };

  const isExplicitUIOnlyPlacement = (placement: ThemePlacement) =>
    placement.valueAttributes.ui_only_property === true ||
    placement.valueAttributes.ui_only_property === "true";

  const handleInputChangeWidget = (
    configType: string,
    property: PropertyType,
    value: any,
    widgetType: string,
    confirmPassword?: boolean,
  ) => {
    let newConfigs = cloneDeep(configProperties);
    switch (widgetType) {
      case "checkbox":
        const checkboxState = getThemeCheckboxState(property);
        newConfigs[chosenService][configType].properties[
          property.propertyName
        ].value = checkboxState.checked
          ? checkboxState.uncheckedValue
          : checkboxState.checkedValue;
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
      value,
    );

    // Update section error count for the specific config type
    newConfigs[chosenService][configType].errors = getSectionErrorCount(
      newConfigs[chosenService][configType].properties,
    );

    newConfigs = updateVisibilityByForeignKeys(newConfigs);
    newConfigs = updateVisibilityForDependsOn(
      newConfigs,
      themeData,
      configSection,
      installedServices || [],
      allThemes,
    );
    // Remove global validation - only validate the changed property above

    setConfigProperties(newConfigs);
    onValueUpdate(
      newConfigs[chosenService][configType].properties[property.propertyName],
      newConfigs,
    );
  };

  const handleInputChangeWidgetForOverrideValues = (
    configType: string,
    property: PropertyType,
    value: any,
    widgetType: string,
    index: number,
    confirmPassword?: boolean,
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
      "",
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
      value,
    );

    // Update section error count for the specific config type
    newConfigs[chosenService][configType].errors = getSectionErrorCount(
      newConfigs[chosenService][configType].properties,
    );

    newConfigs = updateVisibilityByForeignKeys(newConfigs);
    newConfigs = updateVisibilityForDependsOn(
      newConfigs,
      themeData,
      configSection,
      installedServices || [],
      allThemes,
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
      (change: any) => change.isChanged,
    );

    const recommendationsCount = selectedRecommendations.length;
    const uniqueServiceCount = new Set(
      selectedRecommendations
        .map((value: any) => value.serviceName)
        .filter(Boolean),
    ).size;

    const message =
      recommendationsCount > 0
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
              (change: any) => change.isChanged,
            )
          }
          onChange={() => {
            const allChecked = Object.values(recommendedChanges || {}).every(
              (change: any) => change.isChanged,
            );

            // Apply the change to all recommendations
            Object.keys(recommendedChanges || {}).forEach((key) => {
              const recommendation = recommendedChanges[key];
              handleRecommendationChange(
                recommendation.propertyName,
                recommendation.fileName,
                !allChecked,
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
                !row.original.isChanged,
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
                                          theme[serviceKey]?.tabs,
                                        )?.[0]
                                      : "Advanced",
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
                                  : "Advanced",
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
                                          serviceKey,
                                        )}
                                        onConfigGroupChange={(
                                          configGroup: string,
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
                                                    e.target.value,
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
                                                              filter.id,
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
                                                    ),
                                                  )}
                                                  {propertyFilters.some(
                                                    (f) => f.selected,
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
                                              getValidationErrorCount(),
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
                                                    e.target.value,
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
                                                              filter.id,
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
                                                    ),
                                                  )}
                                                  {propertyFilters.some(
                                                    (f) => f.selected,
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
                                              getValidationErrorCount(),
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
                                  role="tablist"
                                  aria-label={`${serviceKey} configuration sections`}
                                >
                                  {theme[serviceKey]?.tabs &&
                                    Object.keys(theme[serviceKey].tabs).length >
                                      1 &&
                                    Object.keys(theme[serviceKey].tabs).map(
                                      (tabName) => {
                                        const tabIsVisible =
                                          hasVisiblePropertiesInTab(
                                            serviceKey,
                                            tabName,
                                          );
                                        return (
                                          <Nav.Item
                                            key={tabName}
                                            className={
                                              tabIsVisible
                                                ? undefined
                                                : "disabled"
                                            }
                                          >
                                            <Nav.Link
                                              eventKey={tabName}
                                              as="button"
                                              type="button"
                                              role="tab"
                                              active={chosenTab === tabName}
                                              className="ambari-tabs nav-link nav-link-underlined"
                                              aria-disabled={!tabIsVisible}
                                              aria-selected={chosenTab === tabName}
                                              tabIndex={tabIsVisible ? 0 : -1}
                                              onClick={() => {
                                                if (tabIsVisible) {
                                                  setChosenTab(tabName);
                                                }
                                              }}
                                              onKeyDown={operateThemeTabs}
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
                                        );
                                      },
                                    )}
                                </Nav>
                              </Col>

                              <Col className="p-4">
                                <Tab.Content>
                                  {!hasVisiblePropertiesInCurrentTab() ? (
                                    <div
                                      className="bg-info-subtle p-4 text-center border rounded"
                                      data-testid="theme-no-content"
                                    >
                                      <p className="text-muted mb-0">
                                        {searchString
                                          ? "No properties match the current filter."
                                          : "No configuration properties are available."}
                                      </p>
                                    </div>
                                  ) : chosenTab !== "Advanced" ? (
                                    theme[serviceKey]?.tabs &&
                                    Object.keys(theme[serviceKey].tabs)
                                      .filter((key) => key === chosenTab)
                                      .map((key) => (
                                        <Tab.Pane eventKey={key} key={key}>
                                          <div
                                            className="service-theme-grid"
                                            data-testid={`theme-grid-${serviceKey}-${key}`}
                                            style={{
                                              display: "grid",
                                              gridTemplateColumns: `repeat(${theme[serviceKey].tabs[key].columns}, minmax(0, 1fr))`,
                                              gridTemplateRows: `repeat(${theme[serviceKey].tabs[key].rows}, minmax(min-content, auto))`,
                                              gap: "0.5rem",
                                            }}
                                          >
                                            {theme[serviceKey]?.tabs[key]
                                              ?.sections &&
                                              theme[serviceKey].tabs[
                                                key
                                              ].sections.map((section) => (
                                                <div
                                                  className="p-1"
                                                  data-theme-section={
                                                    section.name
                                                  }
                                                  data-row-index={
                                                    section.rowIndex
                                                  }
                                                  data-column-index={
                                                    section.columnIndex
                                                  }
                                                  style={{
                                                    gridColumn: `${section.columnIndex + 1} / span ${section.columnSpan}`,
                                                    gridRow: `${section.rowIndex + 1} / span ${section.rowSpan}`,
                                                    minWidth: 0,
                                                  }}
                                                  key={section.id}
                                                >
                                                  <Card className="p-2 h-100">
                                                    <div className="p-2">
                                                      {section.displayName}
                                                    </div>
                                                    <div
                                                      className="service-theme-subsection-grid"
                                                      style={{
                                                        display: "grid",
                                                        gridTemplateColumns: `repeat(${section.columns}, minmax(0, 1fr))`,
                                                        gridTemplateRows: `repeat(${section.rows}, minmax(min-content, auto))`,
                                                      }}
                                                    >
                                                      {section.subsections.map(
                                                        (subsection) => {
                                                          const isVisible =
                                                            subsection.dependsOn
                                                              .length
                                                              ? evaluateDependsOn(
                                                                  subsection.dependsOn,
                                                                )
                                                              : true;

                                                          if (!isVisible)
                                                            return null;

                                                          const subsectionTabs =
                                                            subsection.tabs.filter(
                                                              (tab) =>
                                                                tab.dependsOn
                                                                  .length ===
                                                                  0 ||
                                                                evaluateDependsOn(
                                                                  tab.dependsOn,
                                                                ),
                                                            );
                                                          const activeSubsectionTabName =
                                                            subsectionTabs.some(
                                                              (tab) =>
                                                                tab.name ===
                                                                activeSubsectionTabs[
                                                                  subsection.id
                                                                ],
                                                            )
                                                              ? activeSubsectionTabs[
                                                                  subsection.id
                                                                ]
                                                              : subsectionTabs[0]
                                                                  ?.name;
                                                          const activeSubsectionTab =
                                                            subsectionTabs.find(
                                                              (tab) =>
                                                                tab.name ===
                                                                activeSubsectionTabName,
                                                            );
                                                          const subsectionPlacements =
                                                            [
                                                              ...subsection.placements,
                                                              ...(activeSubsectionTab?.placements ??
                                                                []),
                                                            ];
                                                          const rowHasTitle =
                                                            section.subsections.some(
                                                              (candidate) =>
                                                                candidate.rowIndex ===
                                                                  subsection.rowIndex &&
                                                                Boolean(
                                                                  candidate.displayName,
                                                                ),
                                                            );

                                                          return (
                                                            <div
                                                              data-theme-subsection={
                                                                subsection.name
                                                              }
                                                              data-row-index={
                                                                subsection.rowIndex
                                                              }
                                                              data-column-index={
                                                                subsection.columnIndex
                                                              }
                                                              className={`${
                                                                subsection.border
                                                                  ? "service-theme-subsection-bordered"
                                                                  : ""
                                                              } ${
                                                                subsection.columnIndex >
                                                                  0 &&
                                                                subsection.leftVerticalSplitter
                                                                  ? "service-theme-subsection-split"
                                                                  : ""
                                                              } ${
                                                                subsection.rowIndex >
                                                                  0 &&
                                                                !subsection.border
                                                                  ? "service-theme-subsection-top-split"
                                                                  : ""
                                                              }`}
                                                              style={{
                                                                gridColumn: `${subsection.columnIndex + 1} / span ${subsection.columnSpan}`,
                                                                gridRow: `${subsection.rowIndex + 1} / span ${subsection.rowSpan}`,
                                                                minWidth: 0,
                                                              }}
                                                              key={
                                                                subsection.id
                                                              }
                                                            >
                                                              <div className="p-3">
                                                                {rowHasTitle && (
                                                                  <div className="service-theme-subsection-title">
                                                                    {subsection.displayName ||
                                                                      "\u00a0"}
                                                                  </div>
                                                                )}
                                                                {subsectionTabs.length >
                                                                  0 && (
                                                                  <Nav
                                                                    variant="tabs"
                                                                    className="mt-3"
                                                                    role="tablist"
                                                                    aria-label={`${subsection.displayName || subsection.name} configuration groups`}
                                                                  >
                                                                    {subsectionTabs.map(
                                                                      (
                                                                        subsectionTab,
                                                                      ) => {
                                                                        const errorCount =
                                                                          getPlacementErrorCount(
                                                                            serviceKey,
                                                                            subsectionTab.placements,
                                                                          );
                                                                        return (
                                                                          <Nav.Item
                                                                            key={
                                                                              subsectionTab.id
                                                                            }
                                                                          >
                                                                            <Nav.Link
                                                                              as="button"
                                                                              type="button"
                                                                              role="tab"
                                                                              id={`theme-subtab-${subsectionTab.id}`}
                                                                              aria-controls={`theme-subtab-panel-${subsection.id}`}
                                                                              active={
                                                                                activeSubsectionTabName ===
                                                                                subsectionTab.name
                                                                              }
                                                                              aria-selected={
                                                                                activeSubsectionTabName ===
                                                                                subsectionTab.name
                                                                              }
                                                                              onClick={() => {
                                                                                setActiveSubsectionTabs(
                                                                                  (
                                                                                    current,
                                                                                  ) => ({
                                                                                    ...current,
                                                                                    [subsection.id]:
                                                                                      subsectionTab.name,
                                                                                  }),
                                                                                );
                                                                              }}
                                                                              onKeyDown={
                                                                                operateThemeTabs
                                                                              }
                                                                            >
                                                                              {
                                                                                subsectionTab.displayName
                                                                              }
                                                                              {errorCount >
                                                                                0 && (
                                                                                <Badge
                                                                                  bg="danger"
                                                                                  className="ms-2"
                                                                                >
                                                                                  {
                                                                                    errorCount
                                                                                  }
                                                                                </Badge>
                                                                              )}
                                                                            </Nav.Link>
                                                                          </Nav.Item>
                                                                        );
                                                                      },
                                                                    )}
                                                                  </Nav>
                                                                )}
                                                                {subsectionPlacements && (
                                                                  <Row
                                                                    role={
                                                                      subsectionTabs.length
                                                                        ? "tabpanel"
                                                                        : undefined
                                                                    }
                                                                    id={
                                                                      subsectionTabs.length
                                                                        ? `theme-subtab-panel-${subsection.id}`
                                                                        : undefined
                                                                    }
                                                                    aria-labelledby={
                                                                      activeSubsectionTab
                                                                        ? `theme-subtab-${activeSubsectionTab.id}`
                                                                        : undefined
                                                                    }
                                                                  >
                                                                    {subsectionPlacements.map(
                                                                      (
                                                                        config: ThemePlacement,
                                                                      ) => {
                                                                        const propertyLocation =
                                                                          findThemeConfigProperty(
                                                                            configProperties,
                                                                            serviceKey,
                                                                            config.configPath,
                                                                          );
                                                                        const type =
                                                                          propertyLocation?.sectionName ??
                                                                          config.configType;
                                                                        const propertyName =
                                                                          config.propertyName;
                                                                        const sourceProperty =
                                                                          propertyLocation?.property
                                                                            ? getThemePlacementProperty(
                                                                                propertyLocation.property,
                                                                                config.id,
                                                                              )
                                                                            : undefined;
                                                                        const effectiveValueAttributes =
                                                                          {
                                                                            ...config.valueAttributes,
                                                                            ...resolveThemeConditionAttributes(
                                                                              config.dependsOn,
                                                                              configProperties,
                                                                              serviceKey,
                                                                              installer
                                                                                ? servicesList
                                                                                : installedServices,
                                                                            ),
                                                                          };
                                                                        const property =
                                                                          sourceProperty
                                                                            ? ({
                                                                                ...sourceProperty,
                                                                                propertyAttributes:
                                                                                  {
                                                                                    ...sourceProperty.propertyAttributes,
                                                                                    ...effectiveValueAttributes,
                                                                                  },
                                                                                isEditable:
                                                                                  sourceProperty.isEditable !==
                                                                                    false &&
                                                                                  !isThemeAttributeTrue(
                                                                                    effectiveValueAttributes.read_only,
                                                                                  ) &&
                                                                                  !(
                                                                                    isThemeAttributeTrue(
                                                                                      effectiveValueAttributes.editable_only_at_install,
                                                                                    ) &&
                                                                                    !installer
                                                                                  ),
                                                                                isOverridable:
                                                                                  sourceProperty.isOverridable !==
                                                                                    false &&
                                                                                  !isThemeAttributeFalse(
                                                                                    effectiveValueAttributes.overridable,
                                                                                  ),
                                                                                isVisible:
                                                                                  sourceProperty.isVisible !==
                                                                                    false &&
                                                                                  !isThemeAttributeFalse(
                                                                                    effectiveValueAttributes.visible,
                                                                                  ),
                                                                                showLabel:
                                                                                  isThemeAttributeFalse(
                                                                                    effectiveValueAttributes.show_property_name,
                                                                                  )
                                                                                    ? false
                                                                                    : sourceProperty.showLabel,
                                                                              } as typeof sourceProperty &
                                                                                Record<
                                                                                  string,
                                                                                  any
                                                                                >)
                                                                            : undefined;
                                                                        const widget =
                                                                          config.widget ??
                                                                          theme[
                                                                            serviceKey
                                                                          ]
                                                                            .widgets[
                                                                            config
                                                                              .configPath
                                                                          ];

                                                                        const isVisible =
                                                                          config
                                                                            .dependsOn
                                                                            .length
                                                                            ? evaluateDependsOnForConfig(
                                                                                configProperties,
                                                                                serviceKey,
                                                                                config.dependsOn,
                                                                                installer
                                                                                  ? servicesList
                                                                                  : installedServices,
                                                                              )
                                                                            : true;
                                                                        if (
                                                                          !isVisible ||
                                                                          !widget
                                                                        )
                                                                          return null;

                                                                        return property &&
                                                                          property?.isVisible &&
                                                                          !property?.isHidden ? (
                                                                          <Row
                                                                            className="mt-4"
                                                                            key={
                                                                              config.configPath
                                                                            }
                                                                          >
                                                                            {property.showLabel !==
                                                                              false && (
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
                                                                            )}
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
                                                                                  <div
                                                                                    data-theme-widget-config={
                                                                                      config.configPath
                                                                                    }
                                                                                  >
                                                                                    {renderWidgets(
                                                                                      widget.type,
                                                                                      {
                                                                                        ...property,
                                                                                        widget:
                                                                                          widget.metadata,
                                                                                        isEditable:
                                                                                          !hostConfigs &&
                                                                                          canEditConfigsInContext &&
                                                                                          currentConfigGroup ===
                                                                                            "Default" &&
                                                                                          property.isEditable,
                                                                                      },
                                                                                      function (
                                                                                        e: any,
                                                                                        confirmPassword: boolean = false,
                                                                                      ) {
                                                                                        handleInputChangeWidget(
                                                                                          type,
                                                                                          property,
                                                                                          e,
                                                                                          widget.type,
                                                                                          confirmPassword,
                                                                                        );
                                                                                      },
                                                                                      config.configPath,
                                                                                    )}
                                                                                  </div>
                                                                                </Tooltip>
                                                                                {/* Display error message for main property */}
                                                                                {property.errorMessage && (
                                                                                  <div className="mt-2 text-danger">
                                                                                    {
                                                                                      property.errorMessage
                                                                                    }
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
                                                                                    canEditConfigsInContext &&
                                                                                    property.isEditable &&
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
                                                                                                configProperties,
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
                                                                                              configsCopy,
                                                                                            );
                                                                                          }}
                                                                                        />
                                                                                      </Tooltip>
                                                                                    )}
                                                                                  {property.isEditable ===
                                                                                    false ||
                                                                                  property.isOverridable ===
                                                                                    false ||
                                                                                  property
                                                                                    .propertyAttributes
                                                                                    ?.overridable ===
                                                                                    false ||
                                                                                  (currentConfigGroup !==
                                                                                    "Default" &&
                                                                                    property?.overrideValues?.some(
                                                                                      (
                                                                                        overrideValue: any,
                                                                                      ) =>
                                                                                        overrideValue.value !==
                                                                                        null,
                                                                                    ))
                                                                                    ? null
                                                                                    : !hostConfigs &&
                                                                                      canEditConfigsInContext && (
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
                                                                                                  true,
                                                                                                );
                                                                                              } else {
                                                                                                const configsCopy =
                                                                                                  cloneDeep(
                                                                                                    configProperties,
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
                                                                                                  },
                                                                                                );

                                                                                                setConfigProperties(
                                                                                                  configsCopy,
                                                                                                );
                                                                                              }
                                                                                            }}
                                                                                          />
                                                                                        </Tooltip>
                                                                                      )}
                                                                                  {!hostConfigs &&
                                                                                  displayUndoRedo &&
                                                                                  property.value !==
                                                                                    property.previousValue &&
                                                                                  canEditConfigsInContext &&
                                                                                  property.isEditable &&
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
                                                                                          property,
                                                                                        )
                                                                                      }
                                                                                    />
                                                                                  ) : null}
                                                                                  {!hostConfigs &&
                                                                                    displayUndoRedo &&
                                                                                    canEditConfigsInContext &&
                                                                                    property.isEditable &&
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
                                                                                              property,
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
                                                                              property.overrideValues,
                                                                            ) &&
                                                                            property
                                                                              .overrideValues
                                                                              .length >
                                                                              0
                                                                              ? property.overrideValues.map(
                                                                                  (
                                                                                    overrideValue: configGroupOverrides,
                                                                                    index: number,
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
                                                                                            widget.type,
                                                                                            {
                                                                                              ...property,
                                                                                              widget:
                                                                                                widget.metadata,
                                                                                              value:
                                                                                                overrideValue.value,
                                                                                              isEditable:
                                                                                                !hostConfigs &&
                                                                                                canEditConfigsInContext &&
                                                                                                currentConfigGroup !==
                                                                                                  "Default" &&
                                                                                                property.isEditable &&
                                                                                                property.isOverridable !==
                                                                                                  false,
                                                                                            },
                                                                                            function (
                                                                                              e: any,
                                                                                              confirmPassword: boolean = false,
                                                                                            ) {
                                                                                              handleInputChangeWidgetForOverrideValues(
                                                                                                type,
                                                                                                property,
                                                                                                e,
                                                                                                widget.type,
                                                                                                index,
                                                                                                confirmPassword,
                                                                                              );
                                                                                            },
                                                                                            `${config.configPath}:override:${index}`,
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
                                                                                          {!hostConfigs &&
                                                                                            (currentConfigGroup ===
                                                                                            "Default" ? (
                                                                                              <h4
                                                                                                className="text-info"
                                                                                                onClick={() => {
                                                                                                  if (
                                                                                                    installer
                                                                                                  ) {
                                                                                                    setSelectedConfigGroups(
                                                                                                      (
                                                                                                        prev,
                                                                                                      ) => ({
                                                                                                        ...prev,
                                                                                                        [serviceKey]:
                                                                                                          overrideValue.groupName,
                                                                                                      }),
                                                                                                    );
                                                                                                  } else {
                                                                                                    setConfigGroup?.(
                                                                                                      overrideValue.groupName,
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
                                                                                            ) : canEditConfigsInContext &&
                                                                                              property.isEditable &&
                                                                                              property.isOverridable !==
                                                                                                false ? (
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
                                                                                                        configProperties,
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
                                                                                                        configsCopy,
                                                                                                      );
                                                                                                    setConfigProperties(
                                                                                                      validatedConfigs,
                                                                                                    );
                                                                                                  }}
                                                                                                />
                                                                                              </Stack>
                                                                                            ) : null)}
                                                                                        </Col>
                                                                                      </Row>
                                                                                    );
                                                                                  },
                                                                                )
                                                                              : null}
                                                                          </Row>
                                                                        ) : isExplicitUIOnlyPlacement(
                                                                            config,
                                                                          ) ? (
                                                                          <Row
                                                                            className="mt-4"
                                                                            key={
                                                                              config.configPath
                                                                            }
                                                                          >
                                                                            {renderUIOnlyWidgets(
                                                                              widget,
                                                                              hostConfigs ||
                                                                                !canEditConfigsInContext ||
                                                                                currentConfigGroup !==
                                                                                  "Default",
                                                                            )}
                                                                          </Row>
                                                                        ) : null;
                                                                      },
                                                                    )}
                                                                  </Row>
                                                                )}
                                                              </div>
                                                            </div>
                                                          );
                                                        },
                                                      )}
                                                    </div>
                                                  </Card>
                                                </div>
                                              ))}
                                          </div>
                                        </Tab.Pane>
                                      ))
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
                                                        }),
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
