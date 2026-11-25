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

import { ConfigPropertiesType } from "../screens/CommonConfigs/types";

export const customSectionName = (configType: string) => `Custom ${configType}`;

/**
 * Returns true when any section of any service already holds the property for
 * the given config type, so a recommendation must update it instead of adding it.
 */
export const hasConfigProperty = (
  configProperties: ConfigPropertiesType,
  propertyName: string,
  configType: string,
): boolean =>
  Object.values(configProperties).some((sections) =>
    Object.values(sections || {}).some((section) =>
      Object.values(section?.properties || {}).some(
        (property) =>
          property?.propertyName === propertyName && property?.type === configType,
      ),
    ),
  );

/**
 * Port of Ember's config_recommendation_parser#_createNewProperty.
 *
 * Stack Advisor can recommend properties that the stack does not define, such
 * as hadoop.proxyuser.<user>.hosts in core-site. Ember adds them to the step
 * configs so they are deployed; they are placed in the service's
 * "Custom <config type>" section, matching how Ember categorizes properties
 * that are not defined in the stack.
 *
 * Mutates configProperties. Returns true when a property was added; false when
 * the service is not part of the configuration or the property already exists.
 */
export const addRecommendedConfigProperty = (
  configProperties: ConfigPropertiesType,
  serviceName: string,
  configType: string,
  propertyName: string,
  recommendedValue: unknown,
): boolean => {
  const sections = configProperties[serviceName];
  if (!sections || recommendedValue === undefined || recommendedValue === null) {
    return false;
  }
  if (hasConfigProperty(configProperties, propertyName, configType)) {
    return false;
  }
  const sectionName = customSectionName(configType);
  if (!sections[sectionName]) {
    sections[sectionName] = {
      errors: 0,
      properties: {},
      displayName: sectionName,
    };
  }
  sections[sectionName].properties[propertyName] = {
    propertyName,
    propertyDisplayname: propertyName,
    propertyValue: recommendedValue,
    propertyAttributes: { type: "custom" },
    previousValue: null,
    value: recommendedValue,
    recommendedValue,
    initialValue: null,
    hasInitialValue: false,
    final: "false",
    savedFinal: "false",
    fileName: `${configType}.xml`,
    propertyType: [],
    type: configType,
    serviceName,
    isEditable: true,
    isUserProperty: true,
    isNotSaved: true,
    foundInPropertyValues: true,
  };
  return true;
};
