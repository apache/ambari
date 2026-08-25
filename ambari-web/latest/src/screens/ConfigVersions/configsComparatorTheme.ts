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
  normalizeThemeResponse,
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
    normalizeThemeResponse(response, "default", [serviceName]),
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
