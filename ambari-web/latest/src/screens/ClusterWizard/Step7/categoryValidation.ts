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

import { ConfigPropertiesType } from "../../CommonConfigs/types";
import {
  getThemePlacementProperty,
  updateVisibilityForDependsOn,
} from "../../CommonConfigs/ConfigUtils";
import {
  findThemeConfigProperty,
  normalizeThemeResponse,
} from "../../CommonConfigs/themeEngine";

type CategoryValidationInput = {
  configProperties: ConfigPropertiesType;
  selectedTab: string;
  serviceNames: string[];
  themes: unknown;
};

const isActiveProperty = (property: Record<string, unknown>) =>
  property.isVisible !== false && property.isHidden !== true;

const hasClientError = (property: Record<string, unknown>) =>
  property.hasError === true || Boolean(property.errorMessage);

const allActiveErrors = (configProperties: ConfigPropertiesType) =>
  Object.values(configProperties).flatMap((service) =>
    Object.values(service).flatMap((section) =>
      Object.values(section.properties || {}).filter(
        (property) => isActiveProperty(property) && hasClientError(property),
      ),
    ),
  );

export const getCategoryClientErrors = ({
  configProperties,
  selectedTab,
  serviceNames,
  themes,
}: CategoryValidationInput) => {
  if (selectedTab === "allConfigurations") {
    return allActiveErrors(configProperties);
  }

  if (selectedTab === "accounts") {
    const accountConfigs = configProperties.MISC?.["Users and Groups"];
    return Object.values(accountConfigs?.properties || {}).filter(
      (property) => isActiveProperty(property) && hasClientError(property),
    );
  }

  const themeName =
    selectedTab === "databases"
      ? "database"
      : selectedTab === "directories"
        ? "directories"
        : "";
  if (!themeName) return [];

  const effectiveConfigs = updateVisibilityForDependsOn(
    configProperties,
    themes,
    themeName,
    serviceNames,
  );
  const normalized = normalizeThemeResponse(
    themes,
    themeName,
    Object.keys(configProperties),
  );
  const seen = new Set<string>();

  return normalized.themedServices.flatMap((serviceName) =>
    normalized.byService[serviceName].placements.flatMap((placement) => {
      const identity = `${serviceName}/${placement.configPath}`;
      if (seen.has(identity)) return [];
      seen.add(identity);
      const match = findThemeConfigProperty(
        effectiveConfigs,
        serviceName,
        placement.configPath,
      );
      if (!match) return [];
      const property = getThemePlacementProperty(
        match.property,
        placement.id,
      );
      return isActiveProperty(property) && hasClientError(property)
        ? [property]
        : [];
    }),
  );
};
