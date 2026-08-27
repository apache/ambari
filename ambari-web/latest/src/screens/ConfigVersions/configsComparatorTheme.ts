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

import {
  ConfigThemeView,
  normalizeDefaultThemeResponse,
  toConfigThemeView,
} from "../CommonConfigs/themeEngine";

export type ComparatorThemeLocation = {
  tabName: string;
  categoryName: string;
  categoryDisplayName: string;
};

export const normalizeComparatorTheme = (
  response: unknown,
  serviceName: string,
): ConfigThemeView => {
  if (!serviceName) return {};
  const view = toConfigThemeView(
    normalizeDefaultThemeResponse(response, [serviceName]),
  );
  const serviceTheme = view[serviceName];
  if (
    serviceTheme &&
    !Object.values(serviceTheme.tabs).some((tab) => !tab.isAdvanced)
  ) {
    return {
      ...view,
      [serviceName]: { ...serviceTheme, isFallback: true },
    };
  }
  return view;
};

export const getComparatorActiveTabs = (
  theme: ConfigThemeView,
  serviceName: string,
) =>
  Object.entries(theme[serviceName]?.tabs ?? {}).map(([key, tab]) => ({
    ...tab,
    key,
  }));

export const findComparatorThemeLocation = (
  theme: ConfigThemeView,
  serviceName: string,
  configType: string,
  propertyName: string,
): ComparatorThemeLocation | null => {
  const serviceTheme = theme[serviceName];
  if (!serviceTheme) return null;

  for (const [tabKey, tab] of Object.entries(serviceTheme.tabs)) {
    for (const section of tab.sections) {
      for (const subsection of section.subsections) {
        const placements = [
          ...subsection.placements,
          ...subsection.tabs.flatMap((subsectionTab) =>
            subsectionTab.placements,
          ),
        ];
        const containsProperty = placements.some(
          (placement) =>
            placement.configType === configType &&
            placement.propertyName === propertyName,
        );
        if (containsProperty) {
          return {
            tabName: tabKey,
            categoryName: subsection.id,
            categoryDisplayName: subsection.displayName || subsection.name,
          };
        }
      }
    }
  }
  return null;
};

const isThemeBooleanTrue = (value: unknown) =>
  value === true || value === 1 || value === "1" || value === "true";

export const isComparatorThemeUIOnly = (
  theme: ConfigThemeView,
  serviceName: string,
  configType: string,
  propertyName: string,
) => {
  const serviceTheme = theme[serviceName];
  if (!serviceTheme) return false;
  const configPath = `${configType}/${propertyName}`;
  return Object.values(serviceTheme.subsectionProperties).some(({ properties }) =>
    properties.some(
      (placement) =>
        placement.configPath === configPath &&
        isThemeBooleanTrue(placement.valueAttributes.ui_only_property),
    ),
  );
};
