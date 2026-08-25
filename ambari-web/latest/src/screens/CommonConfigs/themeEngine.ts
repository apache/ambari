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

import { ConfigPropertiesType } from "./types";

export const ADVANCED_THEME_TAB = "Advanced";

export type ThemeValueAttributes = Record<string, unknown>;

export type ThemeCondition = {
  configs?: string[];
  resource?: string;
  type?: string;
  if?: string;
  then?: { property_value_attributes?: ThemeValueAttributes };
  else?: { property_value_attributes?: ThemeValueAttributes };
};

export type ThemePlacement = {
  configPath: string;
  configType: string;
  propertyName: string;
  subsectionName: string;
  subsectionTabName?: string;
  dependsOn: ThemeCondition[];
  valueAttributes: ThemeValueAttributes;
};

export type ThemeWidget = {
  configPath: string;
  type: string;
  units?: Array<{ unitName?: string; minValue?: string; maxValue?: string }>;
  displayName?: string;
  requiredProperties: Record<string, string>;
  metadata: Record<string, unknown>;
};

export type ThemeSubsectionTab = {
  id: string;
  name: string;
  displayName: string;
  dependsOn: ThemeCondition[];
  placements: ThemePlacement[];
};

export type ThemeSubsection = {
  id: string;
  name: string;
  displayName: string;
  rowIndex: number;
  columnIndex: number;
  rowSpan: number;
  columnSpan: number;
  border: boolean;
  leftVerticalSplitter: boolean;
  dependsOn: ThemeCondition[];
  placements: ThemePlacement[];
  tabs: ThemeSubsectionTab[];
};

export type ThemeSection = {
  id: string;
  name: string;
  displayName: string;
  rowIndex: number;
  columnIndex: number;
  rowSpan: number;
  columnSpan: number;
  rows: number;
  columns: number;
  subsections: ThemeSubsection[];
};

export type ThemeTab = {
  id: string;
  layoutName?: string;
  name: string;
  displayName: string;
  rows: number;
  columns: number;
  sections: ThemeSection[];
  isAdvanced: boolean;
};

export type ServiceTheme = {
  serviceName: string;
  themeName: string;
  sourceFile?: string;
  layoutNames: string[];
  tabs: ThemeTab[];
  placements: ThemePlacement[];
  widgetsByConfigPath: Record<string, ThemeWidget>;
  isFallback: boolean;
};

export type ThemeDiagnostic = {
  code:
    | "INVALID_SERVICE_ITEM"
    | "INVALID_THEME"
    | "DUPLICATE_THEME"
    | "MISSING_LAYOUT"
    | "INVALID_LAYOUT"
    | "INVALID_PLACEMENT"
    | "INVALID_WIDGET"
    | "INVALID_COLLECTION";
  serviceName?: string;
  themeName?: string;
  sourceFile?: string;
  message: string;
};

export type ThemeConditionDiagnostic = {
  code: "INVALID_CONDITION" | "MISSING_CONFIG_REFERENCE";
  statement?: string;
  message: string;
};

export type ThemeConditionEvaluation = {
  matches: boolean;
  valid: boolean;
  diagnostics: ThemeConditionDiagnostic[];
};

export type NormalizedThemes = {
  services: string[];
  themedServices: string[];
  byService: Record<string, ServiceTheme>;
  diagnostics: ThemeDiagnostic[];
};

export type ConfigThemeView = Record<
  string,
  {
    tabs: Record<string, ThemeTab>;
    subsectionProperties: Record<
      string,
      { properties: ThemePlacement[] }
    >;
    widgets: Record<string, ThemeWidget>;
    isFallback: boolean;
  }
>;

export const themeTabKey = (
  tab: ThemeTab,
  tabs: readonly ThemeTab[],
  index: number,
) => {
  if (tab.isAdvanced || tabs.filter((item) => item.name === tab.name).length === 1) {
    return tab.name;
  }
  const layoutKey = `${tab.layoutName ?? "layout"}:${tab.name}`;
  return tabs.filter(
    (item) => `${item.layoutName ?? "layout"}:${item.name}` === layoutKey,
  ).length === 1
    ? layoutKey
    : `${layoutKey}:${index + 1}`;
};

type UnknownRecord = Record<string, unknown>;

const isRecord = (value: unknown): value is UnknownRecord =>
  typeof value === "object" && value !== null && !Array.isArray(value);

const asRecord = (value: unknown): UnknownRecord =>
  isRecord(value) ? value : {};

const asArray = (value: unknown): unknown[] =>
  Array.isArray(value) ? value : [];

const asThemeCollection = (
  value: unknown,
  field: string,
  diagnostics: ThemeDiagnostic[],
  context: Pick<
    ThemeDiagnostic,
    "serviceName" | "themeName" | "sourceFile"
  > = {},
): unknown[] => {
  if (value === undefined || value === null) return [];
  if (Array.isArray(value)) return value;
  diagnostics.push({
    code: "INVALID_COLLECTION",
    ...context,
    message: `${field} must be an array.`,
  });
  return [];
};

const asString = (value: unknown, fallback = ""): string =>
  typeof value === "string" ? value : fallback;

const asBoolean = (value: unknown, fallback: boolean): boolean => {
  if (typeof value === "boolean") return value;
  if (value === 1 || value === "1" || value === "true") return true;
  if (value === 0 || value === "0" || value === "false") return false;
  return fallback;
};

const gridNumber = (value: unknown, fallback = 0): number => {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed >= 0 ? Math.floor(parsed) : fallback;
};

const gridSpan = (value: unknown): number => Math.max(1, gridNumber(value, 1));

const conditions = (value: unknown): ThemeCondition[] =>
  asArray(value)
    .filter(isRecord)
    .map((condition) => ({
      configs: asArray(condition.configs).filter(
        (config): config is string => typeof config === "string",
      ),
      resource: asString(condition.resource) || undefined,
      type: asString(condition.type) || undefined,
      if: asString(condition.if) || undefined,
      then: isRecord(condition.then)
        ? {
            property_value_attributes: isRecord(
              condition.then.property_value_attributes,
            )
              ? condition.then.property_value_attributes
              : undefined,
          }
        : undefined,
      else: isRecord(condition.else)
        ? {
            property_value_attributes: isRecord(
              condition.else.property_value_attributes,
            )
              ? condition.else.property_value_attributes
              : undefined,
          }
        : undefined,
    }));

export const splitThemeConfigPath = (
  configPath: string,
): { configType: string; propertyName: string } | null => {
  const separator = configPath.indexOf("/");
  if (separator <= 0 || separator === configPath.length - 1) return null;
  return {
    configType: configPath.slice(0, separator),
    propertyName: configPath.slice(separator + 1),
  };
};

export const findThemeConfigProperty = (
  configProperties: ConfigPropertiesType,
  serviceName: string,
  configPath: string,
) => {
  const path = splitThemeConfigPath(configPath);
  if (!path) return undefined;
  const exact =
    configProperties[serviceName]?.[path.configType]?.properties?.[
      path.propertyName
    ];
  if (exact) return { sectionName: path.configType, property: exact };

  const serviceConfigs = configProperties[serviceName] ?? {};
  for (const sectionName of Object.keys(serviceConfigs)) {
    const property =
      serviceConfigs[sectionName]?.properties?.[path.propertyName];
    const propertyConfigType =
      property?.type || property?.fileName?.replace(/\.xml$/, "");
    if (property && propertyConfigType === path.configType) {
      return { sectionName, property };
    }
  }
  return undefined;
};

const advancedTab = (serviceName: string): ThemeTab => ({
  id: `${serviceName}:${ADVANCED_THEME_TAB}`,
  name: ADVANCED_THEME_TAB,
  displayName: ADVANCED_THEME_TAB,
  rows: 1,
  columns: 1,
  sections: [],
  isAdvanced: true,
});

const fallbackTheme = (
  serviceName: string,
  configSection: string,
): ServiceTheme => ({
  serviceName,
  themeName: configSection,
  layoutNames: [],
  tabs: configSection === "default" ? [advancedTab(serviceName)] : [],
  placements: [],
  widgetsByConfigPath: {},
  isFallback: true,
});

type ThemeCandidate = {
  serviceName: string;
  sourceFile?: string;
  theme: UnknownRecord;
};

const collectCandidates = (
  response: unknown,
  diagnostics: ThemeDiagnostic[],
): ThemeCandidate[] => {
  const items = asThemeCollection(
    asRecord(response).items,
    "items",
    diagnostics,
  );
  const candidates: ThemeCandidate[] = [];

  items.forEach((rawItem) => {
    if (!isRecord(rawItem)) {
      diagnostics.push({
        code: "INVALID_SERVICE_ITEM",
        message: "Theme response contains a non-object item.",
      });
      return;
    }

    const outerServiceName = asString(
      asRecord(rawItem.StackServices).service_name,
    );
    const rawThemes = rawItem.ThemeInfo
      ? [rawItem]
      : asThemeCollection(rawItem.themes, "themes", diagnostics, {
          serviceName: outerServiceName || undefined,
        });

    rawThemes.forEach((rawTheme) => {
      const themeInfo = asRecord(asRecord(rawTheme).ThemeInfo);
      const serviceName =
        asString(themeInfo.service_name) || outerServiceName;
      const theme = asRecord(asRecord(themeInfo.theme_data).Theme);
      const sourceFile = asString(themeInfo.file_name) || undefined;

      if (!serviceName || !Object.keys(theme).length) {
        diagnostics.push({
          code: "INVALID_THEME",
          serviceName: serviceName || outerServiceName || undefined,
          sourceFile,
          message: "ThemeInfo is missing service_name or theme_data.Theme.",
        });
        return;
      }
      candidates.push({ serviceName, sourceFile, theme });
    });
  });

  return candidates;
};

const parsePlacements = (
  configuration: UnknownRecord,
  serviceName: string,
  themeName: string,
  sourceFile: string | undefined,
  diagnostics: ThemeDiagnostic[],
): ThemePlacement[] => {
  const placement = asRecord(configuration.placement);
  const seenTargets = new Set<string>();
  return asThemeCollection(
    placement.configs,
    "configuration.placement.configs",
    diagnostics,
    { serviceName, themeName, sourceFile },
  ).flatMap((rawPlacement) => {
    const item = asRecord(rawPlacement);
    if (item.removed === true) return [];
    const configPath = asString(item.config);
    const path = splitThemeConfigPath(configPath);
    const subsectionName = asString(item["subsection-name"]);
    if (!path || !subsectionName) {
      diagnostics.push({
        code: "INVALID_PLACEMENT",
        serviceName,
        themeName,
        sourceFile,
        message: `Ignored placement with invalid config path or subsection: ${configPath || "(missing)"}.`,
      });
      return [];
    }
    const subsectionTabName =
      asString(item["subsection-tab-name"]) || undefined;
    const targetKey = `${configPath}\u0000${subsectionName}\u0000${subsectionTabName ?? ""}`;
    if (seenTargets.has(targetKey)) return [];
    seenTargets.add(targetKey);
    return [
      {
        configPath,
        ...path,
        subsectionName,
        subsectionTabName,
        dependsOn: conditions(item["depends-on"]),
        valueAttributes: asRecord(item.property_value_attributes),
      },
    ];
  });
};

const parseWidgets = (
  configuration: UnknownRecord,
  serviceName: string,
  themeName: string,
  sourceFile: string | undefined,
  diagnostics: ThemeDiagnostic[],
): Record<string, ThemeWidget> => {
  const widgets: Record<string, ThemeWidget> = {};
  asThemeCollection(
    configuration.widgets,
    "configuration.widgets",
    diagnostics,
    { serviceName, themeName, sourceFile },
  ).forEach((rawWidget) => {
    const item = asRecord(rawWidget);
    if (item.removed === true) return;
    const configPath = asString(item.config);
    const widget = asRecord(item.widget);
    const type = asString(widget.type);
    if (!splitThemeConfigPath(configPath) || !type) {
      diagnostics.push({
        code: "INVALID_WIDGET",
        serviceName,
        themeName,
        sourceFile,
        message: `Ignored widget with invalid config path or type: ${configPath || "(missing)"}.`,
      });
      return;
    }

    const requiredProperties = Object.fromEntries(
      Object.entries(asRecord(widget["required-properties"])).filter(
        (entry): entry is [string, string] => typeof entry[1] === "string",
      ),
    );
    const units = asArray(widget.units)
      .filter(isRecord)
      .map((unit) => ({
        unitName: asString(unit["unit-name"]) || undefined,
        minValue: asString(unit["min-value"]) || undefined,
        maxValue: asString(unit["max-value"]) || undefined,
      }));

    widgets[configPath] = {
      configPath,
      type,
      units: units.length ? units : undefined,
      displayName: asString(widget["display-name"]) || undefined,
      requiredProperties,
      metadata: widget,
    };
  });
  return widgets;
};

const parseTabs = (
  layout: UnknownRecord,
  placements: ThemePlacement[],
  serviceName: string,
  themeName: string,
  sourceFile: string | undefined,
  diagnostics: ThemeDiagnostic[],
): ThemeTab[] =>
  asThemeCollection(layout.tabs, "layout.tabs", diagnostics, {
    serviceName,
    themeName,
    sourceFile,
  }).flatMap((rawTab) => {
    const layoutName = asString(layout.name) || "(unnamed-layout)";
    const tab = asRecord(rawTab);
    const tabName = asString(tab.name);
    const tabLayout = asRecord(tab.layout);
    if (!tabName || !Object.keys(tabLayout).length) {
      diagnostics.push({
        code: "INVALID_LAYOUT",
        serviceName,
        themeName,
        sourceFile,
        message: "Ignored a tab without a name or layout.",
      });
      return [];
    }

    const sections = asThemeCollection(
      tabLayout.sections,
      "tab.layout.sections",
      diagnostics,
      { serviceName, themeName, sourceFile },
    )
      .flatMap((rawSection) => {
        const section = asRecord(rawSection);
        const sectionName = asString(section.name);
        if (!sectionName) return [];
        const subsections = asThemeCollection(
          section.subsections,
          "section.subsections",
          diagnostics,
          { serviceName, themeName, sourceFile },
        )
          .flatMap((rawSubsection) => {
            const subsection = asRecord(rawSubsection);
            const subsectionName = asString(subsection.name);
            if (!subsectionName) return [];
            const subsectionPlacements = placements.filter(
              (item) => item.subsectionName === subsectionName,
            );
            const tabs = asThemeCollection(
              subsection["subsection-tabs"],
              "subsection.subsection-tabs",
              diagnostics,
              { serviceName, themeName, sourceFile },
            )
              .flatMap((rawSubsectionTab) => {
                const subsectionTab = asRecord(rawSubsectionTab);
                const name = asString(subsectionTab.name);
                if (!name) return [];
                return [
                  {
                    id: `${serviceName}:${themeName}:${layoutName}:${tabName}:${sectionName}:${subsectionName}:${name}`,
                    name,
                    displayName:
                      asString(subsectionTab["display-name"]) || name,
                    dependsOn: conditions(subsectionTab["depends-on"]),
                    placements: subsectionPlacements.filter(
                      (item) => item.subsectionTabName === name,
                    ),
                  },
                ];
              });

            return [
              {
                id: `${serviceName}:${themeName}:${layoutName}:${tabName}:${sectionName}:${subsectionName}`,
                name: subsectionName,
                displayName:
                  asString(subsection["display-name"]) || "",
                rowIndex: gridNumber(subsection["row-index"]),
                columnIndex: gridNumber(subsection["column-index"]),
                rowSpan: gridSpan(subsection["row-span"]),
                columnSpan: gridSpan(subsection["column-span"]),
                border: asBoolean(subsection.border, false),
                leftVerticalSplitter: asBoolean(
                  subsection["left-vertical-splitter"],
                  true,
                ),
                dependsOn: conditions(subsection["depends-on"]),
                placements: subsectionPlacements.filter(
                  (item) => !item.subsectionTabName,
                ),
                tabs,
              },
            ];
          });

        return [
          {
            id: `${serviceName}:${themeName}:${layoutName}:${tabName}:${sectionName}`,
            name: sectionName,
            displayName: asString(section["display-name"]) || "",
            rowIndex: gridNumber(section["row-index"]),
            columnIndex: gridNumber(section["column-index"]),
            rowSpan: gridSpan(section["row-span"]),
            columnSpan: gridSpan(section["column-span"]),
            rows: Math.max(1, gridNumber(section["section-rows"], 1)),
            columns: Math.max(
              1,
              gridNumber(section["section-columns"], 1),
            ),
            subsections,
          },
        ];
      });

    return [
      {
        id: `${serviceName}:${themeName}:${layoutName}:${tabName}`,
        layoutName,
        name: tabName,
        displayName: asString(tab["display-name"]) || tabName,
        rows: Math.max(1, gridNumber(tabLayout["tab-rows"], 1)),
        columns: Math.max(1, gridNumber(tabLayout["tab-columns"], 1)),
        sections,
        isAdvanced: false,
      },
    ];
  });

type ThemeTargetCounts = {
  subsections: Map<string, number>;
  subsectionTabs: Map<string, number>;
};

const subsectionTabTarget = (subsectionName: string, tabName: string) =>
  `${subsectionName}\u0000${tabName}`;

const collectThemeTargetCounts = (layouts: UnknownRecord[]): ThemeTargetCounts => {
  const counts: ThemeTargetCounts = {
    subsections: new Map<string, number>(),
    subsectionTabs: new Map<string, number>(),
  };
  const increment = (target: Map<string, number>, key: string) => {
    target.set(key, (target.get(key) ?? 0) + 1);
  };

  layouts.forEach((layout) => {
    asArray(layout.tabs).filter(isRecord).forEach((tab) => {
      asArray(asRecord(tab.layout).sections).filter(isRecord).forEach((section) => {
        asArray(section.subsections).filter(isRecord).forEach((subsection) => {
          const subsectionName = asString(subsection.name);
          if (!subsectionName) return;
          increment(counts.subsections, subsectionName);
          asArray(subsection["subsection-tabs"])
            .filter(isRecord)
            .forEach((subsectionTab) => {
              const tabName = asString(subsectionTab.name);
              if (tabName) {
                increment(
                  counts.subsectionTabs,
                  subsectionTabTarget(subsectionName, tabName),
                );
              }
            });
        });
      });
    });
  });
  return counts;
};

const validatePlacementTargets = (
  placements: ThemePlacement[],
  layouts: UnknownRecord[],
  serviceName: string,
  themeName: string,
  sourceFile: string | undefined,
  diagnostics: ThemeDiagnostic[],
) => {
  const targets = collectThemeTargetCounts(layouts);
  return placements.filter((placement) => {
    const targetCount = placement.subsectionTabName
      ? targets.subsectionTabs.get(
          subsectionTabTarget(
            placement.subsectionName,
            placement.subsectionTabName,
          ),
        ) ?? 0
      : targets.subsections.get(placement.subsectionName) ?? 0;
    if (targetCount === 1) return true;
    if (targetCount === 0) return false;

    diagnostics.push({
      code: "INVALID_PLACEMENT",
      serviceName,
      themeName,
      sourceFile,
      message: `Ignored layout attachment for ${placement.configPath} because its target is ambiguous.`,
    });
    return false;
  });
};

const parseCandidate = (
  candidate: ThemeCandidate,
  configSection: string,
  diagnostics: ThemeDiagnostic[],
): ServiceTheme | null => {
  const themeName = asString(candidate.theme.name);
  if (themeName !== configSection) return null;
  const configuration = asRecord(candidate.theme.configuration);
  const parsedPlacements = parsePlacements(
    configuration,
    candidate.serviceName,
    themeName,
    candidate.sourceFile,
    diagnostics,
  );
  const widgetsByConfigPath = parseWidgets(
    configuration,
    candidate.serviceName,
    themeName,
    candidate.sourceFile,
    diagnostics,
  );
  const layouts = asThemeCollection(
    configuration.layouts,
    "configuration.layouts",
    diagnostics,
    {
      serviceName: candidate.serviceName,
      themeName,
      sourceFile: candidate.sourceFile,
    },
  ).filter(isRecord);
  if (!layouts.length) {
    diagnostics.push({
      code: "MISSING_LAYOUT",
      serviceName: candidate.serviceName,
      themeName,
      sourceFile: candidate.sourceFile,
      message: "Theme does not contain a configuration layout.",
    });
  }

  const layoutPlacements = validatePlacementTargets(
    parsedPlacements,
    layouts,
    candidate.serviceName,
    themeName,
    candidate.sourceFile,
    diagnostics,
  );

  const tabs = layouts.flatMap((layout) =>
    parseTabs(
        layout,
        layoutPlacements,
        candidate.serviceName,
        themeName,
        candidate.sourceFile,
        diagnostics,
      ),
  );
  if (!tabs.length) {
    if (layouts.length) {
      diagnostics.push({
        code: "INVALID_LAYOUT",
        serviceName: candidate.serviceName,
        themeName,
        sourceFile: candidate.sourceFile,
        message: "Theme does not contain a usable configuration tab.",
      });
    }
    return fallbackTheme(candidate.serviceName, configSection);
  }
  if (configSection === "default") tabs.push(advancedTab(candidate.serviceName));

  return {
    serviceName: candidate.serviceName,
    themeName,
    sourceFile: candidate.sourceFile,
    layoutNames: layouts.map((layout) => asString(layout.name)).filter(Boolean),
    tabs,
    placements: parsedPlacements,
    widgetsByConfigPath,
    isFallback: false,
  };
};

export const normalizeThemeResponse = (
  response: unknown,
  configSection: string,
  requestedServices: readonly string[],
): NormalizedThemes => {
  const diagnostics: ThemeDiagnostic[] = [];
  const candidates = collectCandidates(response, diagnostics);
  const byService: Record<string, ServiceTheme> = {};

  candidates.forEach((candidate) => {
    const parsed = parseCandidate(candidate, configSection, diagnostics);
    if (!parsed) return;
    if (byService[candidate.serviceName] && !byService[candidate.serviceName].isFallback) {
      diagnostics.push({
        code: "DUPLICATE_THEME",
        serviceName: candidate.serviceName,
        themeName: configSection,
        sourceFile: candidate.sourceFile,
        message: `Ignored duplicate ${configSection} theme for ${candidate.serviceName}.`,
      });
      return;
    }
    byService[candidate.serviceName] = parsed;
  });

  requestedServices.forEach((serviceName) => {
    if (!byService[serviceName]) {
      byService[serviceName] = fallbackTheme(serviceName, configSection);
    }
  });

  const themedServices = requestedServices.filter(
    (serviceName) => !byService[serviceName]?.isFallback,
  );
  return {
    services:
      configSection === "default" ? [...requestedServices] : themedServices,
    themedServices,
    byService,
    diagnostics,
  };
};

export const toConfigThemeView = (
  normalized: NormalizedThemes,
): ConfigThemeView =>
  Object.fromEntries(
    Object.entries(normalized.byService).map(([serviceName, serviceTheme]) => {
      const subsectionProperties: Record<
        string,
        { properties: ThemePlacement[] }
      > = {};
      serviceTheme.placements.forEach((placement) => {
        subsectionProperties[placement.subsectionName] ??= { properties: [] };
        subsectionProperties[placement.subsectionName].properties.push(
          placement,
        );
      });
      return [
        serviceName,
        {
          tabs: Object.fromEntries(
            serviceTheme.tabs.map((tab, index, tabs) => [
              themeTabKey(tab, tabs, index),
              tab,
            ]),
          ),
          subsectionProperties,
          widgets: serviceTheme.widgetsByConfigPath,
          isFallback: serviceTheme.isFallback,
        },
      ];
    }),
  );

const findConfigValue = (
  configProperties: ConfigPropertiesType,
  serviceName: string,
  configPath: string,
): unknown => {
  return findThemeConfigProperty(configProperties, serviceName, configPath)
    ?.property.value;
};

const invalidCondition = (
  statement: string | undefined,
  message: string,
  code: ThemeConditionDiagnostic["code"] = "INVALID_CONDITION",
): ThemeConditionEvaluation => ({
  matches: false,
  valid: false,
  diagnostics: [{ code, statement, message }],
});

const evaluateConditionAtom = (
  atom: string,
  configProperties: ConfigPropertiesType,
  serviceName: string,
): ThemeConditionEvaluation => {
  const comparison = atom.match(/^\$\{([^{}]+)\}(?:\s*===\s*(.+))?$/);
  if (!comparison) {
    return invalidCondition(atom, "Condition atom uses unsupported syntax.");
  }

  const configPath = comparison[1].trim();
  if (!splitThemeConfigPath(configPath)) {
    return invalidCondition(atom, "Condition contains an invalid config path.");
  }
  const expected = (comparison[2] ?? "true").trim();
  if (
    !expected ||
    /\$\{|&&|\|\||===|[();{}]/.test(expected)
  ) {
    return invalidCondition(atom, "Condition comparison value uses unsupported syntax.");
  }

  const value = findConfigValue(configProperties, serviceName, configPath);
  if (value === undefined || value === null) {
    return invalidCondition(
      atom,
      `Condition references missing config ${configPath}.`,
      "MISSING_CONFIG_REFERENCE",
    );
  }
  return {
    matches: String(value).trim() === expected,
    valid: true,
    diagnostics: [],
  };
};

export const evaluateConfigConditionResult = (
  statement: string | undefined,
  configProperties: ConfigPropertiesType,
  serviceName: string,
): ThemeConditionEvaluation => {
  if (!statement?.trim()) {
    return invalidCondition(statement, "Condition expression is empty.");
  }

  const diagnostics: ThemeConditionDiagnostic[] = [];
  let matches = false;
  statement.split("||").forEach((orExpression) => {
    const atoms = orExpression.split("&&");
    let conjunctionMatches = atoms.length > 0;
    atoms.forEach((atom) => {
      const result = evaluateConditionAtom(
        atom.trim(),
        configProperties,
        serviceName,
      );
      diagnostics.push(...result.diagnostics);
      conjunctionMatches = conjunctionMatches && result.matches;
    });
    matches = matches || conjunctionMatches;
  });

  return {
    matches: diagnostics.length === 0 && matches,
    valid: diagnostics.length === 0,
    diagnostics,
  };
};

export const evaluateConfigCondition = (
  statement: string | undefined,
  configProperties: ConfigPropertiesType,
  serviceName: string,
): boolean =>
  evaluateConfigConditionResult(statement, configProperties, serviceName)
    .matches;

export const resolveThemeConditionAttributes = (
  dependsOn: readonly ThemeCondition[] | undefined,
  configProperties: ConfigPropertiesType,
  serviceName: string,
  installedServices: readonly string[] = [],
  diagnostics: ThemeConditionDiagnostic[] = [],
): ThemeValueAttributes => {
  const attributes: ThemeValueAttributes = {};
  (dependsOn ?? []).forEach((dependency) => {
    let conditionMatches: boolean;
    if (dependency.resource?.toLowerCase() === "service") {
      if (!dependency.if?.trim()) {
        diagnostics.push({
          code: "INVALID_CONDITION",
          statement: dependency.if,
          message: "Service condition is missing a service name.",
        });
        return;
      }
      conditionMatches = installedServices.includes(dependency.if.trim());
    } else {
      const result = evaluateConfigConditionResult(
        dependency.if,
        configProperties,
        serviceName,
      );
      diagnostics.push(...result.diagnostics);
      if (!result.valid) return;
      conditionMatches = result.matches;
    }
    const action = conditionMatches ? dependency.then : dependency.else;
    Object.assign(attributes, action?.property_value_attributes ?? {});
  });
  return attributes;
};

export const evaluateThemeVisibility = (
  dependsOn: readonly ThemeCondition[] | undefined,
  configProperties: ConfigPropertiesType,
  serviceName: string,
  installedServices: readonly string[] = [],
): boolean => {
  const visible = resolveThemeConditionAttributes(
    dependsOn,
    configProperties,
    serviceName,
    installedServices,
  ).visible;
  return typeof visible === "boolean" ? visible : true;
};
