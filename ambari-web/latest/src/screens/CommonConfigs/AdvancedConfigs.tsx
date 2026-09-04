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

import { Fragment, useEffect, useState } from "react";
import {
  configGroupOverrides,
  ConfigPropertiesType,
  InputType,
  PropertyType,
  TruthValues,
} from "./types";
import Spinner from "../../components/Spinner";
import { Accordion, Col, Form, InputGroup, Row, Stack } from "react-bootstrap";
import { cloneDeep, isArray, isEmpty } from "lodash";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faLock,
  faMinusCircle,
  faPlusCircle,
  faRedo,
  faUndo,
  faTag,
  faList,
} from "@fortawesome/free-solid-svg-icons";
import {
  validateInput,
  getSectionErrorCount,
  updateVisibilityByForeignKeys,
  validateAllProperties,
  setTabErrorCounts,
  formatPropertyValue,
} from "./ConfigUtils";
import TestConnection from "./TestConnection";
import Modal from "../../components/Modal";
import useEnhancedConfigs from "../../hooks/useEnhancedConfigs";
import OverlayBackdrop from "../../components/OverlayBackdrop";
import useAuthorizationPolicy from "../../hooks/useAuthorizationPolicy";
import Tooltip from "../../components/Tooltip";
import { formatParamsForDisplay, formatParamsForSave, shouldUseMultilineFormatting } from "../../Utils/jvmFormatUtils";
import {
  parseCustomPropertyInput,
  validateCustomPropertyKey,
} from "../../Utils/customConfigProperties";

type AdvancedConfigsType = {
  configPropertiesData: any;
  setConfigProperties: (data: any) => void;
  chosenService: string;
  displayUndoRedo: Boolean;
  setTabErrors: (data: any) => void;
  configGroup?: string;
  setShowAddToGroupModal?: (show: boolean) => void;
  setConfigGroup?: (groupName: string) => void;
  hostConfigs?: boolean;
  recommendationsDataToSend?: Object;
  installedServices?: string[];
  installer?: boolean;
  stack?: string;
  stackVersion?: string;
  hosts?: string[];
  onValueUpdateProp?: (
    config: PropertyType,
    configProperties: ConfigPropertiesType,
  ) => void;
  searchString?: string;
  canEdit?: boolean;
};

// Map propertyType to InputType for rendering
const getInputTypeFromPropertyType = (propertyTypes: string[]): string => {
  if (!propertyTypes || propertyTypes.length === 0) {
    return InputType.STRING;
  }
  
  // Use the first property type to determine the input type
  const primaryType = propertyTypes[0];
  
  switch (primaryType) {
    case 'PASSWORD':
      return InputType.PASSWORD;
    case 'USER':
      return InputType.USER;
    case 'GROUP':
      return InputType.STRING; // Groups are typically rendered as text inputs
    case 'TEXT':
      return InputType.STRING;
    case 'ADDITIONAL_USER_PROPERTY':
      return InputType.STRING;
    case 'NOT_MANAGED_HDFS_PATH':
      return InputType.DIRECTORY;
    case 'VALUE_FROM_PROPERTY_FILE':
      return InputType.STRING;
    default:
      return InputType.STRING;
  }
};

function AdvancedConfigs({
  configPropertiesData,
  setConfigProperties,
  chosenService,
  displayUndoRedo,
  setTabErrors,
  configGroup="Default",
  setShowAddToGroupModal,
  setConfigGroup,
  hostConfigs = false,
  installedServices = [],
  installer = false,
  recommendationsDataToSend = {},
  stack,
  stackVersion,
  hosts = [],
  onValueUpdateProp,
  searchString = "",
  canEdit,
}: AdvancedConfigsType) {
  const [advancedConfigs, setAdvancedConfigs] = useState(configPropertiesData);
  const [configPropertiesLoading] = useState(false);
  const [showAddPropertyModal, setShowAddPropertyModal] =
    useState<boolean>(false);
  const { isAuthorized } = useAuthorizationPolicy();
  const canEditConfigs = isAuthorized("SERVICE.MODIFY_CONFIGS");
  const canEditProperties = canEdit ?? (installer || canEditConfigs);
  const newPropertyFields = {
    propertyName: "",
    propertyAttributes: { 
      type: getInputTypeFromPropertyType(['TEXT']),
      empty_value_valid: true // Allow empty values for custom properties (matches Ember behavior)
    },
    previousValue: "",
    propertyValue: "",
    value: "",
    type: "TEXT",
    propertyType: ['TEXT'],
    isEditable: true,
  };
  const [customPropertyType, setCustomPropertyType] = useState<string>("");
  const [newCustomProperty, setNewCustomProperty] =
    useState<PropertyType>(newPropertyFields);
  const [multiPropertyInput, setMultiPropertyInput] = useState<string>("");
  const [multiPropertyErrors, setMultiPropertyErrors] = useState<string[]>([]);
  const [isMultiPropertyMode, setIsMultiPropertyMode] = useState<boolean>(false);
  const [selectedPropertyTypes, setSelectedPropertyTypes] = useState<string[]>([]);
  const [hostsModalProperty, setHostsModalProperty] = useState<PropertyType | null>(null);

  const configSectionNames = Object.keys(
    advancedConfigs?.[chosenService] || {},
  ).sort((left, right) => {
    const displayName = (name: string) =>
      advancedConfigs?.[chosenService]?.[name]?.displayName || name;
    const categoryRank = (name: string) =>
      displayName(name).startsWith("Advanced ")
        ? 1
        : displayName(name).startsWith("Custom ")
          ? 2
          : 0;
    return categoryRank(left) - categoryRank(right) ||
      displayName(left).localeCompare(displayName(right));
  });
  const defaultOpenSections = configSectionNames.filter(
    (name) => {
      const displayName =
        advancedConfigs?.[chosenService]?.[name]?.displayName || name;
      return !displayName.includes("Advanced") &&
        !displayName.includes("CapacityScheduler") &&
        !displayName.includes("Custom");
    },
  );

  // Property type options matching Ember.js implementation
  const propertyTypeOptions = [
    'PASSWORD',
    'USER', 
    'GROUP',
    'TEXT',
    'ADDITIONAL_USER_PROPERTY',
    'NOT_MANAGED_HDFS_PATH',
    'VALUE_FROM_PROPERTY_FILE'
  ];

  const { onValueUpdate: onValueUpdateHook, processingConfig } =
    useEnhancedConfigs(
      setConfigProperties,
      chosenService,
      installedServices ?? [],
      recommendationsDataToSend,
      installer ? "clusterCreation" : "serviceConfigs",
      stack,
      stackVersion,
      hosts
    );

  const onValueUpdate = onValueUpdateProp || onValueUpdateHook;

  const commitAdvancedConfigs = (nextConfigs: any) => {
    setAdvancedConfigs(nextConfigs);
    setConfigProperties(nextConfigs);
    setTabErrors(setTabErrorCounts(nextConfigs));
  };

  useEffect(() => {
    setAdvancedConfigs(configPropertiesData);
    setTabErrors(setTabErrorCounts(configPropertiesData));
  }, [configPropertiesData]);

  const handleChange = (
    section: string,
    property: string,
    value: string,
    confirmPassword?: boolean
  ) => {
    let advancedDataCopy = cloneDeep(advancedConfigs);
    const existingValue =
      advancedDataCopy[chosenService][section]["properties"][property].value;

    switch (
      advancedDataCopy[chosenService][section]["properties"][property]
        .propertyAttributes.type
    ) {
      case InputType.CHECKBOX:
      case InputType.BOOLEAN:
      case InputType.BOOLEANINVERTED:
        if (existingValue === "true") {
          advancedDataCopy[chosenService][section]["properties"][
            property
          ].value = "false";
        } else if (existingValue === TruthValues.YES) {
          advancedDataCopy[chosenService][section]["properties"][
            property
          ].value = TruthValues.NO;
        } else if (existingValue === TruthValues.NO) {
          advancedDataCopy[chosenService][section]["properties"][
            property
          ].value = TruthValues.YES;
        } else {
          advancedDataCopy[chosenService][section]["properties"][
            property
          ].value = "true";
        }
        break;
      case InputType.PASSWORD:
        if (confirmPassword) {
          advancedDataCopy[chosenService][section]["properties"][
            property
          ].confirmPassword = value;
        } else {
          advancedDataCopy[chosenService][section]["properties"][
            property
          ].value = value;
        }
        break;

      case InputType.RADIOBUTTON:
        advancedDataCopy[chosenService][section]["properties"][property].value =
          value;

        break;

      default:
        advancedDataCopy[chosenService][section]["properties"][property].value =
          value;
    }

    advancedDataCopy[chosenService][section]["properties"][
      property
    ].errorMessage = validateInput(
      advancedDataCopy[chosenService][section]["properties"][property],
      value
    );

    advancedDataCopy = updateVisibilityByForeignKeys(advancedDataCopy);
    advancedDataCopy = validateAllProperties(advancedDataCopy);

    commitAdvancedConfigs(advancedDataCopy);

    onValueUpdate(
      advancedDataCopy[chosenService][section]["properties"][property],
      advancedDataCopy
    );
  };

  // capacity-scheduler.xml properties are edited as one combined "key=value"
  // per-line textarea (matching the old Ember UI's App.CapacitySceduler widget)
  // instead of one field per property. The underlying properties map is still
  // individual key/value entries - only the presentation is combined here.
  const CAPACITY_SCHEDULER_SECTION = "capacity-scheduler";
  // Matches ui/app/models/stack_service.js:381 - the category that shows as "Scheduler".
  const CAPACITY_SCHEDULER_CATEGORY = "CapacityScheduler";

  const getCapacitySchedulerTextValue = (properties: {
    [key: string]: PropertyType;
  }) => {
    return Object.keys(properties)
      .sort()
      .map((name) => `${name}=${properties[name]?.value ?? ""}`)
      .join("\n");
  };

  const handleCapacitySchedulerChange = (section: string, rawValue: string) => {
    let advancedDataCopy = cloneDeep(advancedConfigs);
    const properties = advancedDataCopy[chosenService][section].properties;
    const templateProperty = Object.values(properties)[0] as
      | PropertyType
      | undefined;

    const seenNames = new Set<string>();
    rawValue.split("\n").forEach((line) => {
      if (!line.trim()) {
        return;
      }
      const separatorIndex = line.indexOf("=");
      const name =
        separatorIndex === -1 ? line.trim() : line.slice(0, separatorIndex).trim();
      if (!name) {
        return;
      }
      const value = separatorIndex === -1 ? "" : line.slice(separatorIndex + 1);
      seenNames.add(name);

      if (properties[name]) {
        properties[name].value = value;
      } else if (templateProperty) {
        properties[name] = {
          ...cloneDeep(templateProperty),
          propertyName: name,
          propertyDisplayname: name,
          value,
          previousValue: value,
          overrideValues: [],
        };
      }
    });

    Object.keys(properties).forEach((name) => {
      if (!seenNames.has(name)) {
        delete properties[name];
      }
    });

    advancedDataCopy = updateVisibilityByForeignKeys(advancedDataCopy);
    advancedDataCopy = validateAllProperties(advancedDataCopy);

    commitAdvancedConfigs(advancedDataCopy);

    const changedProperty = Object.values(properties)[0] as
      | PropertyType
      | undefined;
    if (changedProperty) {
      onValueUpdate(changedProperty, advancedDataCopy);
    }
  };

   const handleChangeForOverridenValues = (
    section: string,
    property: string,
    value: string,
    index: number,
    confirmPassword?: boolean
  ) => {
    let advancedDataCopy = cloneDeep(advancedConfigs);
    const existingValue =
      advancedDataCopy[chosenService][section]["properties"][property].overrideValues[index].value;

    switch (
      advancedDataCopy[chosenService][section]["properties"][property]
        .propertyAttributes.type
    ) {
      case InputType.CHECKBOX:
      case InputType.BOOLEAN:
      case InputType.BOOLEANINVERTED:
        if (existingValue === "true") {
          advancedDataCopy[chosenService][section]["properties"][
            property
          ].overrideValues[index].value = "false";
        } else if (existingValue === TruthValues.YES) {
          advancedDataCopy[chosenService][section]["properties"][
            property
          ].overrideValues[index].value = TruthValues.NO;
        } else if (existingValue === TruthValues.NO) {
          advancedDataCopy[chosenService][section]["properties"][
            property
          ].overrideValues[index].value = TruthValues.YES;
        } else {
          advancedDataCopy[chosenService][section]["properties"][
            property
          ].overrideValues[index].value = "true";
        }
        break;
      case InputType.PASSWORD:
        if (confirmPassword) {
          advancedDataCopy[chosenService][section]["properties"][
            property
          ].overrideValues[index].confirmPassword = value;
        } else {
          advancedDataCopy[chosenService][section]["properties"][
            property
          ].overrideValues[index].value = value;
        }
        break;

      case InputType.RADIOBUTTON:
        advancedDataCopy[chosenService][section]["properties"][property].overrideValues[index].value =
          value;

        break;

      default:
        advancedDataCopy[chosenService][section]["properties"][property].overrideValues[index].value =
          value;
    }

    advancedDataCopy[chosenService][section]["properties"][
      property
    ].overrideValues[index].errorMessage = validateInput(
      advancedDataCopy[chosenService][section]["properties"][property],
      value
    );

    advancedDataCopy = updateVisibilityByForeignKeys(advancedDataCopy);
    advancedDataCopy = validateAllProperties(advancedDataCopy);

    commitAdvancedConfigs(advancedDataCopy);


  };

  const renderInput = (property: PropertyType, onChange: any) => {
    const propertyToRender = cloneDeep(property);
    if (property.propertyName === "content") {
      propertyToRender.propertyAttributes.type = InputType.CONTENT;
    }
    switch (propertyToRender?.propertyAttributes?.type) {
      case InputType.BOOLEAN:
      case InputType.CHECKBOX:
      case InputType.BOOLEANINVERTED:
        const checkboxId = `advanced-config-checkbox-${property.propertyName}`;
        return (
          <Form.Check
            id={checkboxId}
            checked={
              property.value === "true" || property.value === TruthValues.YES
            }
            onChange={onChange}
            disabled={!property.isEditable}
          />
        );
      case InputType.PASSWORD:
        return (
          <Row>
            <Col md={6}>
              <Form.Control
                type="password"
                value={property.value}
                onChange={(e) => onChange(e, false)}
                placeholder="Type password"
                disabled={!property.isEditable}
              />
            </Col>
            <Col md={6}>
              <Form.Control
                type="password"
                value={property.confirmPassword}
                onChange={(e) => onChange(e, true)}
                placeholder="Confirm password"
                disabled={!property.isEditable}
              />
            </Col>
          </Row>
        );
      case InputType.DIRECTORY:
        return (
          <Form.Control
            type="text"
            value={property.value}
            onChange={onChange}
            placeholder={property.propertyValue}
            disabled={!property.isEditable}
          />
        );
      case InputType.DIRECTORIES:
        return (
          <Form.Control
            as="textarea"
            rows={5}
            value={property.value}
            onChange={onChange}
            disabled={!property.isEditable}
          />
        );
      case InputType.VALUELIST:
        return (
          <Form.Select
            value={property.value}
            onChange={onChange}
            disabled={!property.isEditable}
          >
            {property.propertyAttributes.entries.map(
              (entry: { value: string; label: string }, index: number) => (
                <option key={index} value={entry.value}>
                  {entry.label}
                </option>
              )
            )}
          </Form.Select>
        );
      case InputType.MULTILINE:
      case InputType.CONTENT:
        // Check if this property should use multiline formatting
        const useMultilineFormatting = shouldUseMultilineFormatting(property.value, property.propertyAttributes?.type);
        const displayValue = useMultilineFormatting ? formatParamsForDisplay(property.value, property.propertyAttributes?.type) : property.value;
        
        return (
          <Form.Control
            as="textarea"
            rows={15}
            value={displayValue}
            onChange={(e) => {
              const valueToSave = useMultilineFormatting ? formatParamsForSave(e.target.value) : e.target.value;
              onChange({ target: { value: valueToSave } });
            }}
            disabled={!property.isEditable}
          />
        );
      case InputType.COMPONENTHOST:
        return <p>{property.value}</p>;

      case InputType.BUTTON:
        return (
          <div className="mt-2">
            <TestConnection
              buttonLabel={property.value}
              serviceName={chosenService}
              configProperties={advancedConfigs}
            />
          </div>
        );
      case InputType.HOSTS:
        if (isEmpty(property?.value) || !isArray(property?.value)) {
          return "No host assigned";
        }
        return property.value.length > 1 ? (
          <a
            href="#"
            onClick={(e) => {
              e.preventDefault();
              setHostsModalProperty(property);
            }}
          >
            {property.value[0]} and {property.value.length - 1}{" "}
            {property.value.length - 1 === 1 ? "other" : "others"}
          </a>
        ) : (
          <span>{property.value[0]}</span>
        );

      case InputType.RADIOBUTTON:
        return (
          <div>
            {property?.propertyAttributes?.options?.map(
              (
                option: { displayName: string; value: string },
                index: number
              ) => (
                <Form.Check
                  type="radio"
                  name={property.propertyName}
                  id={`${property.propertyName}-${index}`}
                  value={option.displayName}
                  label={option.displayName}
                  checked={property.value === option.displayName}
                  onChange={onChange}
                  disabled={!property.isEditable}
                  key={index}
                />
              )
            )}
          </div>
        );

      default:
        // Check if this property should use multiline formatting
        // console.log("InputType.DEFAULT:")
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
                onChange({ target: { value: valueToSave } });
              }}
              disabled={!property.isEditable}
            />
          );
        }
        
        return (
          <InputGroup
            className={property?.propertyAttributes?.unit ? "w-50" : "w-100"}
          >
            <Form.Control
              type="text"
              value={property.value}
              onChange={onChange}
              placeholder={property.propertyValue}
              disabled={!property.isEditable}
            />
            {property?.propertyAttributes?.unit ? (
              <InputGroup.Text>
                {property.propertyAttributes.unit}
              </InputGroup.Text>
            ) : null}
          </InputGroup>
        );
    }
  };
  const handleUndo = (configType: string, property: PropertyType) => {
    const newConfigs = cloneDeep(advancedConfigs);
    newConfigs[chosenService][configType].properties[
      property.propertyName
    ].value = property.previousValue;
    newConfigs[chosenService][configType].properties[
      property.propertyName
    ].errorMessage = validateInput(property, property.previousValue);

    newConfigs[chosenService][configType].errors = getSectionErrorCount(
      newConfigs[chosenService][configType].properties
    );

    commitAdvancedConfigs(newConfigs);
  };

  const setToDefault = (configType: string, property: PropertyType) => {
    const newConfigs = cloneDeep(advancedConfigs);
    newConfigs[chosenService][configType].properties[
      property.propertyName
    ].value = formatPropertyValue(property, property.propertyValue);

    newConfigs[chosenService][configType].properties[
      property.propertyName
    ].errorMessage = validateInput(
      property,
      formatPropertyValue(property, property.propertyValue)
    );

    newConfigs[chosenService][configType].errors = getSectionErrorCount(
      newConfigs[chosenService][configType].properties
    );

    commitAdvancedConfigs(newConfigs);
  };

  const parseMultiPropertyInput = (input: string) => {
    const existingProperties =
      advancedConfigs[chosenService][customPropertyType]?.properties || {};
    const parsed = parseCustomPropertyInput(input, existingProperties);
    const propertyTypes =
      selectedPropertyTypes.length > 0 ? selectedPropertyTypes : ["TEXT"];
    const properties: PropertyType[] = parsed.properties.map(({ key, value }) => ({
        propertyName: key,
        propertyAttributes: { 
          type: getInputTypeFromPropertyType(propertyTypes),
          empty_value_valid: true // Allow empty values for custom properties (matches Ember behavior)
        },
        previousValue: "",
        propertyValue: value,
        value: value,
        type: customPropertyType.replace(/^Custom\s*/, ""),
        fileName: customPropertyType.replace(/^Custom\s*/, "") + ".xml",
        propertyType: propertyTypes,
        isEditable: true,
      }));

    return { properties, errors: parsed.errors };
  };

  const validateMultiPropertyInput = (input: string) => {
    const { errors } = parseMultiPropertyInput(input);
    setMultiPropertyErrors(errors);
    return errors.length === 0;
  };

  const validateSingleProperty = (property: PropertyType) => {
    return validateCustomPropertyKey(
      property.propertyName,
      advancedConfigs[chosenService][customPropertyType]?.properties || {}
    );
  };

  const handleModeToggle = (multiMode: boolean) => {
    setIsMultiPropertyMode(multiMode);

    setMultiPropertyInput("");
    setMultiPropertyErrors([]);
    setNewCustomProperty(newPropertyFields);
    setSelectedPropertyTypes(['TEXT']);
  };

  const handleAddProperty = () => {
    if (isMultiPropertyMode) {

      if (multiPropertyInput.trim() && validateMultiPropertyInput(multiPropertyInput)) {
        const { properties } = parseMultiPropertyInput(multiPropertyInput);
        const advancedConfigsCopy = cloneDeep(advancedConfigs);
        
        if (!advancedConfigsCopy[chosenService][customPropertyType].properties) {
          advancedConfigsCopy[chosenService][customPropertyType].properties = {};
        }

        properties.forEach(property => {
          advancedConfigsCopy[chosenService][customPropertyType].properties[
            property.propertyName
          ] = {
            ...property,
            // Mark as not found in original property values since it's a new custom property
            foundInPropertyValues: false,
          };
        });

        commitAdvancedConfigs(advancedConfigsCopy);
        setShowAddPropertyModal(false);
        setMultiPropertyInput("");
        setMultiPropertyErrors([]);
        setNewCustomProperty(newPropertyFields);
        setCustomPropertyType("");
        setSelectedPropertyTypes([]);
        setIsMultiPropertyMode(false); 
      }
    } else {
      const validationError = validateSingleProperty(newCustomProperty);
      if (validationError) {
        const updatedProperty = cloneDeep(newCustomProperty);
        updatedProperty.errorMessage = validationError;
        setNewCustomProperty(updatedProperty);
        return; 
      }

      if (newCustomProperty && newCustomProperty.propertyName.trim()) {
        const advancedConfigsCopy = cloneDeep(advancedConfigs);
        const propertyName = newCustomProperty.propertyName.trim();
        if (!advancedConfigsCopy[chosenService][customPropertyType].properties) {
          advancedConfigsCopy[chosenService][customPropertyType].properties = {};
        }
        advancedConfigsCopy[chosenService][customPropertyType].properties[
          propertyName
        ] = {
          ...newCustomProperty,
          propertyName,
          // Mark as not found in original property values since it's a new custom property
          foundInPropertyValues: false,
        };

        commitAdvancedConfigs(advancedConfigsCopy);
        setShowAddPropertyModal(false);
        setNewCustomProperty(newPropertyFields);
        setCustomPropertyType("");
        setSelectedPropertyTypes([]);
        setIsMultiPropertyMode(false); 
      }
    }
  };

  if (configPropertiesLoading || !advancedConfigs || isEmpty(advancedConfigs)) {
    return <Spinner />;
  }

  // Check if there are any visible properties across all sections
  const hasAnyVisibleProperties = advancedConfigs[chosenService] && 
    Object.keys(advancedConfigs[chosenService]).some(config => {
      if (config.includes("Custom") && config.endsWith("env")) {
        return false;
      }
      const currentConfigValue = advancedConfigs[chosenService][config];
      const filteredPropertiesCount = Object.keys(currentConfigValue.properties || {}).filter(
        (property) =>
          !currentConfigValue.properties[property].tabName &&
          currentConfigValue.properties[property].isVisible !== false &&
          !!!currentConfigValue.properties[property].isHidden
      ).length;
      return filteredPropertiesCount > 0;
    });

  // If no visible properties across all sections, show root level message
  if (!hasAnyVisibleProperties) {
    return (
      <>
        <OverlayBackdrop isOpen={processingConfig} />
        <div className="bg-info-subtle p-4 text-center border rounded">
          <p className="text-muted mb-0">No properties to display.</p>
        </div>
      </>
    );
  }

  return (
    <>
      <OverlayBackdrop isOpen={processingConfig} />
      <Accordion
        alwaysOpen
        defaultActiveKey={defaultOpenSections}
        activeKey={
          searchString
            ? configSectionNames
            : undefined
        }
      >
        {advancedConfigs &&
          advancedConfigs[chosenService] &&
          configSectionNames.map(
            (config) => {
              const currentConfigValue = advancedConfigs[chosenService][config];
              const errorCount = advancedConfigs[chosenService][config].errors;
              const filteredPropertiesCount = Object.keys(
                currentConfigValue.properties
              ).filter(
                (property) =>
                  !currentConfigValue.properties[property].tabName &&
                  currentConfigValue.properties[property].isVisible !== false &&
                  !!!currentConfigValue.properties[property].isHidden
              ).length;
              // Don't show sections that are custom env configs
              if (config.includes("Custom") && config.endsWith("env")) {
                return null;
              }

              // capacity-scheduler.xml properties live in their own catch-all
              // "capacity-scheduler" bucket, but Ember shows them inside the
              // "CapacityScheduler" category (displayName "Scheduler") next to
              // yarn.resourcemanager.scheduler.class - see
              // ui/app/models/stack_service.js:381. Fold that bucket's combined
              // textarea into the CapacityScheduler section below instead of
              // rendering it as its own accordion item.
              if (config === CAPACITY_SCHEDULER_SECTION) {
                return null;
              }
              const capacitySchedulerBucket =
                config === CAPACITY_SCHEDULER_CATEGORY
                  ? advancedConfigs[chosenService][CAPACITY_SCHEDULER_SECTION]
                  : undefined;
              const capacitySchedulerProperties =
                capacitySchedulerBucket?.properties || {};
              const hasCapacitySchedulerProperties =
                Object.keys(capacitySchedulerProperties).length > 0;

              // Check if section has no visible properties
              const hasNoVisibleProperties = isEmpty(currentConfigValue.properties) ||
                filteredPropertiesCount === 0;

              // For custom sections with search active, hide if no properties match
              if (hasNoVisibleProperties && config.includes("Custom") && searchString) {
                return null;
              }

              // For NON-custom sections, skip if no visible properties
              // Custom sections should always be shown so users can add properties via "Add Property..."
              // Keep the CapacityScheduler section visible when it has nothing of its
              // own but the folded-in capacity-scheduler.xml bucket does.
              if (
                hasNoVisibleProperties &&
                !config.includes("Custom") &&
                !hasCapacitySchedulerProperties
              ) {
                return null;
              }

              return (
                <Accordion.Item key={config} eventKey={config}>
                  <Accordion.Header>
                    <div className="d-flex align-items-center fs-18">
                      {advancedConfigs[chosenService][config].displayName
                        ? advancedConfigs[chosenService][config].displayName
                        : config === CAPACITY_SCHEDULER_CATEGORY
                        ? "Scheduler"
                        : config}{" "}
                      {errorCount > 0 && (
                        <span className="ms-2 badge rounded-pill bg-danger">
                          {errorCount}
                        </span>
                      )}
                    </div>
                  </Accordion.Header>
                  <Accordion.Body>
                    {Object.keys(currentConfigValue.properties).map(
                      (configProperty) => {
                        const currentConfigPropertyValue =
                          currentConfigValue.properties?.[configProperty];
                        if (
                          !currentConfigPropertyValue.tabName &&
                          currentConfigPropertyValue?.isVisible !== false &&
                          !!!currentConfigPropertyValue?.isHidden
                        ) {
                          return (
                            <Fragment key={configProperty}>
                              <Row
                                className="mt-2 align-items-center"
                                key={configProperty}
                              >
                                <Col md={4}>
                                  <Tooltip
                                    message={currentConfigPropertyValue.propertyDescription || currentConfigPropertyValue.description || currentConfigPropertyValue.property_description}
                                    heading={currentConfigPropertyValue?.propertyDisplayname || currentConfigPropertyValue.propertyName}
                                    placement="top"
                                  >
                                    <Form.Label className="p-2">
                                      {currentConfigPropertyValue?.propertyDisplayname
                                        ? currentConfigPropertyValue.propertyDisplayname
                                        : currentConfigPropertyValue.propertyName}
                                    </Form.Label>
                                  </Tooltip>
                                  {currentConfigPropertyValue?.isSecureConfig &&
                                    canEditProperties && (
                                      <Tooltip
                                        message="This is a secure configuration property"
                                        placement="top"
                                      >
                                        <FontAwesomeIcon
                                          className="ms-2 text-primary"
                                          icon={faLock}
                                        />
                                      </Tooltip>
                                    )}
                                </Col>
                                <Col>
                                  <Stack direction="vertical">
                                            <Tooltip
                                              message={currentConfigPropertyValue.propertyDescription || currentConfigPropertyValue.description || currentConfigPropertyValue.property_description}
                                              heading={currentConfigPropertyValue?.propertyDisplayname || currentConfigPropertyValue.propertyName}
                                              placement="top"
                                            >
                                              <div>
                                                {renderInput(
                                                  {
                                                    ...currentConfigPropertyValue,
                                                    isEditable:
                                                      !hostConfigs &&
                                                      canEditProperties &&
                                                      configGroup === "Default" &&
                                                      currentConfigPropertyValue.isEditable
                                                  },
                                                  function (
                                                    e: any,
                                                    confirmPassword: boolean = false
                                                  ) {
                                                    handleChange(
                                                      config,
                                                      configProperty,
                                                      e.target.value,
                                                      confirmPassword
                                                    );
                                                  }
                                                )}
                                              </div>
                                            </Tooltip>
                                    {currentConfigPropertyValue.errorMessage ? (
                                      <Col className="mt-2 text-danger">
                                        {
                                          currentConfigPropertyValue.errorMessage
                                        }
                                      </Col>
                                    ) : null}
                                  </Stack>
                                </Col>
                                <Col md={2}>
                                  <Stack direction="vertical">
                                    <Stack direction="horizontal" gap={2}>
                                      {configGroup === "Default" &&
                                      !hostConfigs &&
                                      currentConfigPropertyValue?.supportsFinal &&
                                      canEditProperties &&
                                      currentConfigPropertyValue.isEditable ? (
                                        <Tooltip
                                          message={
                                            currentConfigPropertyValue.final === "true"
                                              ? "This property is marked as final and cannot be overridden. Click to make it overridable."
                                              : "Click to mark this property as final (cannot be overridden by child configurations)"
                                          }
                                          placement="top"
                                        >
                                          <FontAwesomeIcon
                                            icon={faLock}
                                            className={
                                              currentConfigPropertyValue.final ===
                                              "true"
                                                ? "lock-selected"
                                                : "text-light pointer"
                                            }
                                            onClick={() => {
                                              const advancedConfigsCopy =
                                                cloneDeep(advancedConfigs);
                                              advancedConfigsCopy[chosenService][
                                                config
                                              ].properties[configProperty].final =
                                                currentConfigPropertyValue.final ===
                                                "true"
                                                  ? "false"
                                                  : "true";
                                              commitAdvancedConfigs(
                                                advancedConfigsCopy
                                              );
                                            }}
                                          />
                                        </Tooltip>
                                      ) : null}
                                      {currentConfigPropertyValue
                                        ?.propertyAttributes?.overridable ===
                                        false ||
                                      currentConfigPropertyValue.final === "true" ||
                                      currentConfigPropertyValue.isEditable === false ||
                                      (configGroup !== "Default" &&
                                        currentConfigPropertyValue
                                          ?.overrideValues?.some((overrideValue:any)=> overrideValue.value !== null))
                                        ? null
                                        : !hostConfigs &&
                                          canEditProperties && (
                                            <Tooltip
                                              message={
                                                configGroup === "Default"
                                                  ? "Add this property to a config group"
                                                  : "Add override value for this config group"
                                              }
                                              placement="top"
                                            >
                                              <FontAwesomeIcon
                                                className="text-primary pointer"
                                                icon={faPlusCircle}
                                                onClick={() => {
                                                  if (configGroup === "Default") {
                                                    setShowAddToGroupModal?.(
                                                      true
                                                    );
                                                  } else {
                                                    const advancedConfigsCopy =
                                                      cloneDeep(advancedConfigs);
                                                    if (
                                                      !advancedConfigsCopy[
                                                        chosenService
                                                      ][config].properties[
                                                        configProperty
                                                      ].overrideValues
                                                    ) {
                                                      advancedConfigsCopy[
                                                        chosenService
                                                      ][config].properties[
                                                        configProperty
                                                      ].overrideValues = [];
                                                    }
                                                    const errorMessage =
                                                      validateInput(
                                                        advancedConfigsCopy[
                                                          chosenService
                                                        ][config].properties[
                                                          configProperty
                                                        ],
                                                        ""
                                                      );

                                                    advancedConfigsCopy[
                                                      chosenService
                                                    ][config].properties[
                                                      configProperty
                                                    ].overrideValues.push({
                                                      value: "",
                                                      groupName: configGroup,
                                                      previousValue: "",
                                                      final: "false",
                                                      savedFinal: "false",
                                                      errorMessage: errorMessage,
                                                    });
                                                    advancedConfigsCopy[
                                                      chosenService
                                                    ][config].errors =
                                                      getSectionErrorCount(
                                                        advancedConfigsCopy[
                                                          chosenService
                                                        ][config].properties
                                                      );
                                                    commitAdvancedConfigs(
                                                      advancedConfigsCopy
                                                    );
                                                  }
                                                }}
                                              />
                                            </Tooltip>
                                          )}

                                      {!hostConfigs &&
                                        config.includes("Custom") &&
                                        canEditProperties &&
                                        currentConfigPropertyValue.isEditable && (
                                          <Tooltip
                                            message="Remove this custom property"
                                            placement="top"
                                          >
                                          <FontAwesomeIcon
                                            className="text-danger pointer"
                                            icon={faMinusCircle}
                                            onClick={() => {
                                              const advancedConfigsCopy =
                                                cloneDeep(advancedConfigs);
                                              advancedConfigsCopy[
                                                chosenService
                                              ][config].properties[
                                                configProperty
                                              ] = {
                                                ...currentConfigPropertyValue,
                                                isVisible: false,
                                                value: null,
                                                // Preserve the foundInPropertyValues flag to help with change detection
                                                foundInPropertyValues: currentConfigPropertyValue.foundInPropertyValues,
                                              };
                                              commitAdvancedConfigs(
                                                advancedConfigsCopy
                                              );
                                            }}
                                          />
                                          </Tooltip>
                                        )}

                                      {!hostConfigs &&
                                      displayUndoRedo &&
                                      canEditProperties &&
                                      currentConfigPropertyValue.isEditable &&
                                      currentConfigPropertyValue.value !==
                                        currentConfigPropertyValue.previousValue &&
                                      configGroup === "Default" ? (
                                        <Tooltip
                                          message="Undo changes and restore previous value"
                                          placement="top"
                                        >
                                        <FontAwesomeIcon
                                          className="text-light pointer"
                                          icon={faUndo}
                                          onClick={() =>
                                            handleUndo(
                                              config,
                                              currentConfigPropertyValue
                                            )
                                          }
                                        />
                                        </Tooltip>
                                      ) : null}
                                      {!hostConfigs &&
                                        displayUndoRedo &&
                                        canEditProperties &&
                                        currentConfigPropertyValue.isEditable &&
                                        configGroup === "Default" && (
                                        <Tooltip
                                          message="Reset to default value"
                                          placement="top"
                                        >
                                        <FontAwesomeIcon
                                          className="text-light pointer"
                                          icon={faRedo}
                                          onClick={() =>
                                            setToDefault(
                                              config,
                                              currentConfigPropertyValue
                                            )
                                          }
                                        />
                                        </Tooltip>
                                      )}
                                    </Stack>
                                  </Stack>
                                </Col>
                              </Row>
                              {currentConfigPropertyValue.overrideValues &&
                              Array.isArray(
                                currentConfigPropertyValue.overrideValues
                              ) &&
                              currentConfigPropertyValue.overrideValues.length >
                                0
                                ? currentConfigPropertyValue.overrideValues.map(
                                    (
                                      overrideValue: configGroupOverrides,
                                      index: number
                                    ) => {
                                      // Only render if value is not null
                                      if (overrideValue.value === null) {
                                        return null;
                                      }

                                      return (
                                        <Row
                                          key={index}
                                          className="mt-2 align-items-center"
                                        >
                                          <Col md={{ span: 6, offset: 4 }}>
                                            <Stack direction="vertical">
                                              {renderInput(
                                                {
                                                  ...currentConfigPropertyValue,
                                                  value: overrideValue.value,
                                                  propertyValue:
                                                    overrideValue.previousValue,
                                                  isEditable:
                                                    !hostConfigs &&
                                                    canEditProperties &&
                                                    configGroup !== "Default" &&
                                                    currentConfigPropertyValue.isEditable &&
                                                    currentConfigPropertyValue.isOverridable !== false &&
                                                    currentConfigPropertyValue.propertyAttributes?.overridable !== false &&
                                                    currentConfigPropertyValue.final !== "true",
                                                },
                                                function (
                                                  e: any,
                                                  confirmPassword: boolean = false
                                                ) {
                                                  handleChangeForOverridenValues(
                                                    config,
                                                    configProperty,
                                                    e.target.value,
                                                    index,
                                                    confirmPassword
                                                  );
                                                }
                                              )}
                                              {overrideValue.errorMessage ? (
                                                <Col className="mt-2 text-danger">
                                                  {overrideValue.errorMessage}
                                                </Col>
                                              ) : null}
                                            </Stack>
                                          </Col>
                                          <Col md={2}>
                                            {!hostConfigs && (configGroup === "Default" ? (
                                              <h4
                                                className="text-info"
                                                onClick={() => {
                                                  setConfigGroup?.(
                                                    overrideValue.groupName
                                                  );
                                                }}
                                              >
                                                Switch to{" "}
                                                {overrideValue.groupName}
                                              </h4>
                                            ) : canEditProperties &&
                                              currentConfigPropertyValue.isEditable &&
                                              currentConfigPropertyValue.isOverridable !== false &&
                                              currentConfigPropertyValue.propertyAttributes?.overridable !== false &&
                                              currentConfigPropertyValue.final !== "true" ? (
                                              <Stack
                                                direction="horizontal"
                                                gap={2}
                                              >
                                                {currentConfigPropertyValue.supportsFinal && (
                                                  <Tooltip
                                                    message={
                                                      overrideValue.final === "true"
                                                        ? "This override is marked as final. Click to make it overridable."
                                                        : "Click to mark this override as final."
                                                    }
                                                    placement="top"
                                                  >
                                                    <FontAwesomeIcon
                                                      icon={faLock}
                                                      className={
                                                        overrideValue.final === "true"
                                                          ? "lock-selected pointer"
                                                          : "text-light pointer"
                                                      }
                                                      onClick={() => {
                                                        const advancedConfigsCopy =
                                                          cloneDeep(advancedConfigs);
                                                        const currentOverride =
                                                          advancedConfigsCopy[
                                                            chosenService
                                                          ][config].properties[
                                                            configProperty
                                                          ].overrideValues[index];
                                                        currentOverride.final =
                                                          currentOverride.final === "true"
                                                            ? "false"
                                                            : "true";
                                                        commitAdvancedConfigs(
                                                          advancedConfigsCopy
                                                        );
                                                      }}
                                                    />
                                                  </Tooltip>
                                                )}
                                                <Tooltip
                                                  message="Remove this override value"
                                                  placement="top"
                                                >
                                                  <FontAwesomeIcon
                                                    className="text-danger pointer"
                                                    icon={faMinusCircle}
                                                    onClick={() => {
                                                      const advancedConfigsCopy =
                                                        cloneDeep(advancedConfigs);
                                                      advancedConfigsCopy[
                                                        chosenService
                                                      ][config].properties[
                                                        configProperty
                                                      ].overrideValues[
                                                        index
                                                      ].value = null;
                                                      
                                                      // Recalculate error counts after removing override
                                                      const validatedConfigs = validateAllProperties(advancedConfigsCopy);
                                                      commitAdvancedConfigs(
                                                        validatedConfigs
                                                      );
                                                    }}
                                                  />
                                                </Tooltip>
                                              </Stack>
                                            ) : null)}
                                          </Col>
                                        </Row>
                                      );
                                    }
                                  )
                                : null}
                            </Fragment>
                          );
                        }
                        return null;
                      }
                    )}
                    {config === CAPACITY_SCHEDULER_CATEGORY &&
                      hasCapacitySchedulerProperties && (
                        <Row className="mt-2 align-items-center">
                          <Col md={4}>
                            <Form.Label className="p-2">
                              Capacity Scheduler
                            </Form.Label>
                          </Col>
                          <Col>
                            <Form.Control
                              as="textarea"
                              rows={16}
                              value={getCapacitySchedulerTextValue(
                                capacitySchedulerProperties
                              )}
                              onChange={(e) =>
                                handleCapacitySchedulerChange(
                                  CAPACITY_SCHEDULER_SECTION,
                                  e.target.value
                                )
                              }
                              disabled={
                                !(
                                  canEditProperties &&
                                  (Object.values(capacitySchedulerProperties)[0] as
                                    | PropertyType
                                    | undefined)?.isEditable !== false
                                )
                              }
                            />
                          </Col>
                        </Row>
                      )}
                    {!hostConfigs && canEditProperties && config.includes("Custom") ? (
                      <h4
                        className="text-info ms-2 mt-2"
                        onClick={() => {
                          setShowAddPropertyModal(true);
                          setCustomPropertyType(config);
                          setSelectedPropertyTypes(['TEXT']);
                          setNewCustomProperty({
                            ...newPropertyFields,
                            type: config.replace(/^Custom\s*/, ""),
                            fileName: config.replace(/^Custom\s*/, "") + ".xml",
                            propertyType: ['TEXT'],
                            propertyAttributes: { 
                              type: getInputTypeFromPropertyType(['TEXT']),
                              empty_value_valid: true // Allow empty values for custom properties (matches Ember behavior)
                            },
                          });
                        }}
                      >
                        Add Property ...
                      </h4>
                    ) : null}
                  </Accordion.Body>
                </Accordion.Item>
              );
            }
          )}
      </Accordion>
      <Modal
        isOpen={showAddPropertyModal && customPropertyType !== ""}
        onClose={() => {
          setShowAddPropertyModal(false);
          setNewCustomProperty(newPropertyFields);
          setMultiPropertyInput("");
          setMultiPropertyErrors([]);
          setCustomPropertyType("");
          setSelectedPropertyTypes([]);
        }}
        modalTitle={"Add Property"}
        modalBody={
          <div>
            <Form>
              <Form.Group as={Row} className="mb-3">
                <Form.Label column sm="2">
                  Type
                </Form.Label>
                <Col sm="8">
                  <Form.Control
                    type="text"
                    disabled={true}
                    value={
                      customPropertyType.replace(/^Custom\s*/, "") + ".xml"
                    }
                  />
                </Col>
                <Col sm="2" className="d-flex justify-content-end">
                  <div className="btn-group" role="group">
                    <button
                      type="button"
                      className={`btn btn-sm ${!isMultiPropertyMode ? 'btn-primary' : 'btn-outline-secondary'}`}
                      onClick={() => handleModeToggle(false)}
                      title="Single Property Mode"
                    >
                      <FontAwesomeIcon icon={faTag} />
                    </button>
                    <button
                      type="button"
                      className={`btn btn-sm ${isMultiPropertyMode ? 'btn-primary' : 'btn-outline-secondary'}`}
                      onClick={() => handleModeToggle(true)}
                      title="Multi Property Mode"
                    >
                      <FontAwesomeIcon icon={faList} />
                    </button>
                  </div>
                </Col>
              </Form.Group>
              
              {isMultiPropertyMode ? (
                <Form.Group as={Row} className="mb-3">
                  <Form.Label column sm="2">
                    Properties
                    <div className="text-muted small">key=value (one per line)</div>
                  </Form.Label>
                  <Col sm="10">
                    <Form.Control
                      as="textarea"
                      rows={8}
                      placeholder="Enter key=value (one per line)"
                      value={multiPropertyInput}
                      onChange={(e) => {
                        setMultiPropertyInput(e.target.value);
                        if (multiPropertyErrors.length > 0) {
                          setMultiPropertyErrors([]);
                        }
                      }}
                      onBlur={() => {
                        if (multiPropertyInput.trim()) {
                          validateMultiPropertyInput(multiPropertyInput);
                        }
                      }}
                    />
                    {multiPropertyErrors.length > 0 && (
                      <div className="mt-2">
                        {multiPropertyErrors.map((error, index) => (
                          <div key={index} className="text-danger small">
                            {error}
                          </div>
                        ))}
                      </div>
                    )}
                    <div className="mt-2 text-muted small">
                      Example:<br />
                      property1=value1<br />
                      property2=value2<br />
                      property3=value3
                    </div>
                  </Col>
                </Form.Group>
              ) : (
                <>
                  <Form.Group as={Row} className="mb-3">
                    <Form.Label column sm="2">
                      Key
                    </Form.Label>
                    <Col sm="10">
                      <Form.Control
                        type="text"
                        placeholder="Enter property key"
                        value={newCustomProperty.propertyName}
                        onChange={(e) => {
                          const newProperty = cloneDeep(newCustomProperty);
                          newProperty.propertyName = e.target.value;
                          newProperty.errorMessage = validateCustomPropertyKey(
                            newProperty.propertyName,
                            advancedConfigs[chosenService][customPropertyType]
                              ?.properties || {}
                          );
                          setNewCustomProperty(newProperty);
                        }}
                      />
                      {newCustomProperty.errorMessage && (
                        <div className="mt-2 text-danger">
                          {newCustomProperty.errorMessage}
                        </div>
                      )}
                    </Col>
                  </Form.Group>
                  <Form.Group as={Row} className="mb-3">
                    <Form.Label column sm="2">
                      Value
                    </Form.Label>
                    <Col sm="10">
                      <Form.Control
                        as="textarea"
                        rows={4}
                        placeholder="Enter property value"
                        value={newCustomProperty.value}
                        onChange={(e) => {
                          const newProperty = cloneDeep(newCustomProperty);
                          newProperty.value = e.target.value;
                          newProperty.propertyValue = e.target.value;
                          newProperty.previousValue = "";
                          setNewCustomProperty(newProperty);
                        }}
                      />
                    </Col>
                  </Form.Group>
                  <Form.Group as={Row} className="mb-3">
                    <Form.Label column sm="2">
                      Property Type
                    </Form.Label>
                    <Col sm="10">
                      <Form.Select
                        multiple
                        value={selectedPropertyTypes}
                        onChange={(e) => {
                          const selectedOptions = Array.from(e.target.selectedOptions, option => option.value);
                          setSelectedPropertyTypes(selectedOptions);
                          const newProperty = cloneDeep(newCustomProperty);
                          newProperty.propertyType = selectedOptions;
                          newProperty.propertyAttributes = {
                            ...newProperty.propertyAttributes,
                            type: getInputTypeFromPropertyType(selectedOptions),
                            empty_value_valid: true // Preserve empty_value_valid for custom properties
                          };
                          setNewCustomProperty(newProperty);
                        }}
                      >
                        {propertyTypeOptions.map((option) => (
                          <option key={option} value={option}>
                            {option}
                          </option>
                        ))}
                      </Form.Select>
                      <div className="mt-2 text-muted small">
                        Hold Ctrl (Cmd on Mac) to select multiple property types
                      </div>
                    </Col>
                  </Form.Group>
                </>
              )}
            </Form>
          </div>
        }
        successCallback={handleAddProperty}
        options={{}}
      ></Modal>
      <Modal
        isOpen={!!hostsModalProperty}
        onClose={() => setHostsModalProperty(null)}
        modalTitle={
          hostsModalProperty?.propertyDisplayname ||
          hostsModalProperty?.propertyName ||
          "Hosts"
        }
        modalBody={
          <ul className="list-unstyled mb-0">
            {(hostsModalProperty?.value || []).map((host: string) => (
              <li className="mt-2" key={host}>{host}</li>
            ))}
          </ul>
        }
        successCallback={() => setHostsModalProperty(null)}
        options={{ cancelableViaIcon: true, cancelableViaBtn: false }}
      ></Modal>
    </>
  );
}

export default AdvancedConfigs;
