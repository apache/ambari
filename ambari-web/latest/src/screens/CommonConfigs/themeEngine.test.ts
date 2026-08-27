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

import { readFileSync, readdirSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it, vi } from "vitest";
import { ConfigPropertiesType } from "./types";
import {
  evaluateConfigCondition,
  evaluateConfigConditionResult,
  evaluateThemeVisibility,
  normalizeDefaultThemeResponse,
  normalizeThemeResponse,
  resolveThemeConditionAttributes,
  ThemeCondition,
  ThemeConditionDiagnostic,
  toConfigThemeView,
} from "./themeEngine";

const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), "../../../../..");

const readTheme = (relativePath: string) =>
  JSON.parse(
    readFileSync(resolve(repoRoot, relativePath), "utf8"),
  );

type ShippedCondition = {
  sourceFile: string;
  resource?: string;
  if: string;
};

const collectConditions = (
  value: unknown,
  sourceFile: string,
  result: ShippedCondition[],
) => {
  if (Array.isArray(value)) {
    value.forEach((entry) => collectConditions(entry, sourceFile, result));
    return;
  }
  if (!value || typeof value !== "object") return;

  const record = value as Record<string, unknown>;
  if (typeof record.if === "string") {
    result.push({
      sourceFile,
      resource:
        typeof record.resource === "string" ? record.resource : undefined,
      if: record.if,
    });
  }
  Object.values(record).forEach((entry) =>
    collectConditions(entry, sourceFile, result),
  );
};

const shippedThemeConditions = (): ShippedCondition[] => {
  const result: ShippedCondition[] = [];
  [
    "ambari-server/src/main/resources/stacks",
    "ambari-server/src/main/resources/common-services",
  ].forEach((relativeRoot) => {
    const root = resolve(repoRoot, relativeRoot);
    const visit = (directory: string) => {
      readdirSync(directory, { withFileTypes: true }).forEach((entry) => {
        const path = resolve(directory, entry.name);
        if (entry.isDirectory()) {
          visit(path);
        } else if (
          entry.name.endsWith(".json") &&
          path.split("/").includes("themes") &&
          !path.split("/").includes("AMBARI_METRICS")
        ) {
          collectConditions(
            JSON.parse(readFileSync(path, "utf8")),
            path.slice(repoRoot.length + 1),
            result,
          );
        }
      });
    };
    visit(root);
  });
  return result;
};

const configsForCondition = (statement: string): ConfigPropertiesType => {
  const sections: ConfigPropertiesType[string] = {};
  statement.split(/&&|\|\|/).forEach((rawAtom) => {
    const atom = rawAtom.trim().match(/^\$\{([^{}]+)\}(?:\s*===\s*(.+))?$/);
    if (!atom) return;
    const slash = atom[1].indexOf("/");
    const configType = atom[1].slice(0, slash);
    const propertyName = atom[1].slice(slash + 1);
    sections[configType] ??= { errors: 0, properties: {} };
    sections[configType].properties[propertyName] = {
      ...property(atom[2]?.trim() || "true", `${configType}.xml`),
      propertyName,
    };
  });
  return { SHIPPED_THEME: sections };
};

const responseFor = (
  serviceName: string,
  themes: Array<{ fileName: string; theme: unknown }>,
) => ({
  items: [
    {
      StackServices: { service_name: serviceName },
      themes: themes.map(({ fileName, theme }) => ({
        ThemeInfo: {
          file_name: fileName,
          service_name: serviceName,
          theme_data: { Theme: theme },
        },
      })),
    },
  ],
});

const property = (value: unknown, fileName?: string) => ({
  propertyName: "property",
  propertyDisplayname: "Property",
  propertyValue: value,
  propertyAttributes: {},
  previousValue: value,
  value,
  final: "false",
  fileName,
  isEditable: true,
});

const configs: ConfigPropertiesType = {
  RANGER: {
    "ranger-env": {
      errors: 0,
      properties: {
        create_db_dbuser: {
          ...property("true", "ranger-env.xml"),
          propertyName: "create_db_dbuser",
        },
        source: {
          ...property("ldap", "ranger-env.xml"),
          propertyName: "source",
        },
      },
    },
    "other-site": {
      errors: 0,
      properties: {
        source: {
          ...property("unix", "other-site.xml"),
          propertyName: "source",
        },
      },
    },
  },
};

describe("Service Theme normalizer", () => {
  it("compiles every server-default artifact for an installed custom service", () => {
    const zookeeper = readTheme(
      "ambari-server/src/main/resources/stacks/BIGTOP/3.2.0/services/ZOOKEEPER/themes/directories.json",
    );
    const result = normalizeDefaultThemeResponse(
      responseFor("CUSTOM_ZK", [
        { fileName: "directories.json", theme: zookeeper },
      ]),
      ["CUSTOM_ZK"],
    );
    const theme = result.byService.CUSTOM_ZK;

    expect(theme.isFallback).toBe(false);
    expect(theme.tabs.map((tab) => tab.name)).toEqual([
      "directories",
      "Advanced",
    ]);
    expect(theme.tabs[0].sections.map((section) => section.displayName)).toEqual([
      "DATA DIRS",
      "LOG DIRS",
      "PID DIRS",
    ]);
    expect(theme.placements.map((placement) => placement.widget?.type)).toEqual([
      "text-field",
      "text-field",
      "text-field",
    ]);
    expect(result.diagnostics).toEqual([]);
  });

  it("keeps Widget metadata scoped to its placement across returned Themes", () => {
    const themed = (name: string, type: string) => ({
      name,
      configuration: {
        placement: {
          configs: [{ config: "site/shared", "subsection-name": `${name}-sub` }],
        },
        widgets: [{ config: "site/shared", widget: { type } }],
        layouts: [
          {
            name,
            tabs: [
              {
                name: `${name}-tab`,
                layout: {
                  sections: [
                    {
                      name: `${name}-section`,
                      subsections: [{ name: `${name}-sub` }],
                    },
                  ],
                },
              },
            ],
          },
        ],
      },
    });
    const theme = normalizeDefaultThemeResponse(
      responseFor("CUSTOM", [
        { fileName: "first.json", theme: themed("first", "text-field") },
        { fileName: "second.json", theme: themed("second", "toggle") },
      ]),
      ["CUSTOM"],
    ).byService.CUSTOM;

    expect(theme.placements.map((placement) => placement.widget?.type)).toEqual([
      "text-field",
      "toggle",
    ]);
    expect(theme.placements[0].id).not.toBe(theme.placements[1].id);
  });

  it("preserves the real BIGTOP HIVE grid, placements, widgets, and UI-only metadata", () => {
    const hive = readTheme(
      "ambari-server/src/main/resources/stacks/BIGTOP/3.2.0/services/HIVE/themes/theme.json",
    );
    const result = normalizeThemeResponse(
      responseFor("HIVE", [{ fileName: "theme.json", theme: hive }]),
      "default",
      ["HIVE"],
    );
    const theme = result.byService.HIVE;
    const settings = theme.tabs.find((tab) => tab.name === "settings");
    const sections = settings?.sections ?? [];

    expect(theme.layoutNames).toEqual(["default"]);
    expect(settings).toMatchObject({ rows: 6, columns: 3 });
    expect(sections.map((section) => section.name)).toEqual([
      "misc-settings",
      "security",
      "optimization",
    ]);
    const optimization = sections.find(
      (section) => section.name === "optimization",
    );
    expect(optimization).toMatchObject({
      rowIndex: 1,
      columnIndex: 0,
      rowSpan: 1,
      columnSpan: 3,
      rows: 2,
      columns: 3,
    });
    expect(
      optimization?.subsections.map((subsection) => [
        subsection.name,
        subsection.rowIndex,
        subsection.columnIndex,
      ]),
    ).toEqual([
      ["optimization-row1-col1", 0, 0],
      ["optimization-row2-col2", 1, 1],
      ["optimization-row2-col3", 1, 2],
    ]);
    expect(
      theme.placements.find(
        (placement) => placement.configPath === "hive-env/test_db_connection",
      ),
    ).toMatchObject({
      subsectionName: "hive-database-configurations-col-2",
      valueAttributes: { ui_only_property: true, keyStore: false },
    });
    expect(theme.widgetsByConfigPath["hive-env/test_db_connection"]).toMatchObject(
      {
        type: "test-db-connection",
        requiredProperties: {
          "db.connection.user": "hive-site/javax.jdo.option.ConnectionUserName",
          "db.connection.password":
            "hive-site/javax.jdo.option.ConnectionPassword",
        },
      },
    );
    expect(theme.tabs.at(-1)).toMatchObject({
      name: "Advanced",
      isAdvanced: true,
    });
    expect(result.diagnostics).toEqual([]);
  });

  it("preserves real RANGER condition geometry and normalizes zero spans to a usable cell", () => {
    const ranger = readTheme(
      "ambari-server/src/main/resources/stacks/BIGTOP/3.3.0/services/RANGER/themes/database.json",
    );
    const result = normalizeThemeResponse(
      responseFor("RANGER", [{ fileName: "database.json", theme: ranger }]),
      "database",
      ["RANGER", "HDFS"],
    );
    const section = result.byService.RANGER.tabs[0].sections[0];

    expect(result.services).toEqual(["RANGER"]);
    expect(section).toMatchObject({
      rowIndex: 0,
      columnIndex: 0,
      rowSpan: 3,
      columnSpan: 2,
      rows: 3,
      columns: 2,
    });
    expect(section.subsections[3].dependsOn[0]).toMatchObject({
      if: "${ranger-env/create_db_dbuser}",
    });

    const hive = readTheme(
      "ambari-server/src/main/resources/stacks/BIGTOP/3.2.0/services/HIVE/themes/theme.json",
    );
    const hiveResult = normalizeThemeResponse(
      responseFor("HIVE", [{ fileName: "theme.json", theme: hive }]),
      "default",
      ["HIVE"],
    );
    const databaseSection = hiveResult.byService.HIVE.tabs
      .find((tab) => tab.name === "hive-database")
      ?.sections[0];
    expect(databaseSection).toMatchObject({ rowSpan: 1, columnSpan: 1 });
  });

  it("parses the real YARN default Theme without flattening multi-column spans", () => {
    const yarn = readTheme(
      "ambari-server/src/main/resources/stacks/BIGTOP/3.2.0/services/YARN/themes/theme.json",
    );
    const result = normalizeThemeResponse(
      responseFor("YARN", [{ fileName: "theme.json", theme: yarn }]),
      "default",
      ["YARN"],
    );
    const tab = result.byService.YARN.tabs.find((item) => !item.isAdvanced);

    expect(tab?.columns).toBeGreaterThan(1);
    expect(
      tab?.sections.some(
        (section) => section.columnIndex > 0 || section.columnSpan > 1,
      ),
    ).toBe(true);
  });

  it("maps the real HIVE directories layout even when configuration-layout says default", () => {
    const hive = readTheme(
      "ambari-server/src/main/resources/stacks/BIGTOP/3.2.0/services/HIVE/themes/directories.json",
    );
    const result = normalizeThemeResponse(
      responseFor("HIVE", [{ fileName: "directories.json", theme: hive }]),
      "directories",
      ["HIVE"],
    );
    const theme = result.byService.HIVE;

    expect(theme.layoutNames).toEqual(["directories"]);
    expect(theme.tabs[0].sections).toHaveLength(3);
    expect(theme.placements).toHaveLength(9);
    expect(theme.tabs[0].sections.map((section) => section.displayName)).toEqual([
      "DATA DIRS",
      "LOG DIRS",
      "PID DIRS",
    ]);
    expect(result.diagnostics).toEqual([]);
  });

  it("keeps the three real HDFS directories widgets and their full paths", () => {
    const hdfs = readTheme(
      "ambari-server/src/main/resources/stacks/BIGTOP/3.2.0/services/HDFS/themes/directories.json",
    );
    const theme = normalizeThemeResponse(
      responseFor("HDFS", [{ fileName: "directories.json", theme: hdfs }]),
      "directories",
      ["HDFS"],
    ).byService.HDFS;

    expect(
      Object.values(theme.widgetsByConfigPath)
        .filter((widget) => widget.type === "directories")
        .map((widget) => widget.configPath),
    ).toEqual([
      "hdfs-site/dfs.datanode.data.dir",
      "hdfs-site/dfs.namenode.name.dir",
      "hdfs-site/dfs.namenode.checkpoint.dir",
    ]);
  });

  it("preserves the real SOLR single-directory widget contract", () => {
    const solr = readTheme(
      "ambari-server/src/main/resources/stacks/BIGTOP/3.2.0/services/SOLR/themes/theme.json",
    );
    const widgets = normalizeThemeResponse(
      responseFor("SOLR", [{ fileName: "theme.json", theme: solr }]),
      "default",
      ["SOLR"],
    ).byService.SOLR.widgetsByConfigPath;

    expect(widgets["solr-env/solr_datadir"].type).toBe("directory");
    expect(widgets["solr-env/solr_znode"].type).toBe("directory");
  });

  it("isolates placements and same-name widgets by selected Theme and full config path", () => {
    const layout = (name: string, subsectionName: string) => ({
      name,
      configuration: {
        placement: {
          "configuration-layout": name,
          configs: [
            { config: "type-a/shared", "subsection-name": subsectionName },
            { config: "type-b/shared", "subsection-name": subsectionName },
          ],
        },
        widgets: [
          { config: "type-a/shared", widget: { type: "slider" } },
          { config: "type-b/shared", widget: { type: "toggle" } },
        ],
        layouts: [
          {
            name,
            tabs: [
              {
                name: `${name}-tab`,
                layout: {
                  sections: [
                    {
                      name: `${name}-section`,
                      subsections: [{ name: subsectionName }],
                    },
                  ],
                },
              },
            ],
          },
        ],
      },
    });
    const response = responseFor("SVC", [
      { fileName: "default.json", theme: layout("default", "shared-sub") },
      { fileName: "database.json", theme: layout("database", "shared-sub") },
    ]);
    const result = normalizeThemeResponse(response, "default", ["SVC"]);
    const theme = result.byService.SVC;

    expect(theme.sourceFile).toBe("default.json");
    expect(theme.placements).toHaveLength(2);
    expect(theme.widgetsByConfigPath["type-a/shared"].type).toBe("slider");
    expect(theme.widgetsByConfigPath["type-b/shared"].type).toBe("toggle");
    expect(Object.keys(theme.widgetsByConfigPath)).toHaveLength(2);
  });

  it("models subsection tabs as distinct panes with their own conditions and placements", () => {
    const theme = {
      name: "default",
      configuration: {
        placement: {
          "configuration-layout": "default",
          configs: [
            {
              config: "site/first",
              "subsection-name": "sub",
              "subsection-tab-name": "one",
            },
            {
              config: "site/second",
              "subsection-name": "sub",
              "subsection-tab-name": "two",
            },
          ],
        },
        widgets: [
          { config: "site/first", widget: { type: "text-field" } },
          { config: "site/second", widget: { type: "text-field" } },
        ],
        layouts: [
          {
            name: "default",
            tabs: [
              {
                name: "settings",
                layout: {
                  sections: [
                    {
                      name: "section",
                      subsections: [
                        {
                          name: "sub",
                          "subsection-tabs": [
                            { name: "one", "display-name": "First" },
                            {
                              name: "two",
                              "display-name": "Second",
                              "depends-on": [
                                {
                                  resource: "service",
                                  if: "HDFS",
                                  then: {
                                    property_value_attributes: { visible: true },
                                  },
                                  else: {
                                    property_value_attributes: { visible: false },
                                  },
                                },
                              ],
                            },
                          ],
                        },
                      ],
                    },
                  ],
                },
              },
            ],
          },
        ],
      },
    };
    const result = normalizeThemeResponse(
      responseFor("SVC", [{ fileName: "theme.json", theme }]),
      "default",
      ["SVC"],
    );
    const tabs = result.byService.SVC.tabs[0].sections[0].subsections[0].tabs;

    expect(tabs.map((tab) => tab.displayName)).toEqual(["First", "Second"]);
    expect(tabs[0].placements[0].configPath).toBe("site/first");
    expect(tabs[1].placements[0].configPath).toBe("site/second");
    expect(tabs[1].dependsOn[0]).toMatchObject({
      resource: "service",
      if: "HDFS",
    });
  });

  it("falls back per requested service and isolates malformed ThemeInfo records", () => {
    const result = normalizeThemeResponse(
      {
        items: [
          { StackServices: { service_name: "BROKEN" }, themes: [{}] },
          "not-an-object",
        ],
      },
      "default",
      ["BROKEN", "MISSING"],
    );

    expect(result.services).toEqual(["BROKEN", "MISSING"]);
    expect(result.byService.BROKEN).toMatchObject({
      isFallback: true,
      tabs: [{ name: "Advanced" }],
    });
    expect(result.byService.MISSING.isFallback).toBe(true);
    expect(result.diagnostics.map((item) => item.code)).toEqual([
      "INVALID_THEME",
      "INVALID_SERVICE_ITEM",
    ]);
  });

  it("diagnoses wrong collection types with service-scoped fallback", () => {
    const result = normalizeThemeResponse(
      {
        items: [
          {
            StackServices: { service_name: "SVC" },
            themes: [
              {
                ThemeInfo: {
                  file_name: "broken.json",
                  service_name: "SVC",
                  theme_data: {
                    Theme: {
                      name: "default",
                      configuration: {
                        layouts: {},
                        placement: { configs: {} },
                        widgets: {},
                      },
                    },
                  },
                },
              },
            ],
          },
        ],
      },
      "default",
      ["SVC"],
    );

    expect(result.byService.SVC).toMatchObject({
      isFallback: true,
      tabs: [{ name: "Advanced" }],
    });
    const collectionDiagnostics = result.diagnostics.filter(
      (item) => item.code === "INVALID_COLLECTION",
    );
    expect(collectionDiagnostics).toHaveLength(3);
    collectionDiagnostics.forEach((diagnostic) =>
      expect(diagnostic).toMatchObject({
        serviceName: "SVC",
        themeName: "default",
        sourceFile: "broken.json",
      }),
    );
    expect(collectionDiagnostics.map((item) => item.message)).toEqual([
      "configuration.placement.configs must be an array.",
      "configuration.widgets must be an array.",
      "configuration.layouts must be an array.",
    ]);
  });

  it("maps every valid layout like Ember and only reports a Theme with no layouts", () => {
    const theme = {
      name: "default",
      configuration: {
        placement: {
          "configuration-layout": "selected",
          configs: [],
        },
        widgets: [],
        layouts: [
          {
            name: "additional",
            tabs: [{ name: "also-shown", layout: { sections: [] } }],
          },
          {
            name: "selected",
            tabs: [{ name: "chosen", layout: { sections: [] } }],
          },
        ],
      },
    };
    const selected = normalizeThemeResponse(
      responseFor("SVC", [{ fileName: "theme.json", theme }]),
      "default",
      ["SVC"],
    );
    expect(selected.byService.SVC.tabs.map((tab) => tab.name)).toEqual([
      "also-shown",
      "chosen",
      "Advanced",
    ]);
    expect(selected.byService.SVC.layoutNames).toEqual([
      "additional",
      "selected",
    ]);

    theme.configuration.layouts = [];
    const missing = normalizeThemeResponse(
      responseFor("SVC", [{ fileName: "theme.json", theme }]),
      "default",
      ["SVC"],
    );
    expect(missing.diagnostics).toContainEqual(
      expect.objectContaining({ code: "MISSING_LAYOUT" }),
    );
    expect(missing.byService.SVC.tabs.map((tab) => tab.name)).toEqual([
      "Advanced",
    ]);
  });

  it("keeps duplicate tab names layout-qualified and rejects ambiguous placement targets", () => {
    const theme = {
      name: "default",
      configuration: {
        placement: {
          configs: [
            { config: "site/value", "subsection-name": "repeated" },
          ],
        },
        widgets: [
          { config: "site/value", widget: { type: "text-field" } },
        ],
        layouts: ["first", "second"].map((layoutName) => ({
          name: layoutName,
          tabs: [
            {
              name: "settings",
              layout: {
                sections: [
                  {
                    name: `${layoutName}-section`,
                    subsections: [{ name: "repeated" }],
                  },
                ],
              },
            },
          ],
        })),
      },
    };
    const normalized = normalizeThemeResponse(
      responseFor("SVC", [{ fileName: "theme.json", theme }]),
      "default",
      ["SVC"],
    );

    expect(Object.keys(toConfigThemeView(normalized).SVC.tabs)).toEqual([
      "first:settings",
      "second:settings",
      "Advanced",
    ]);
    expect(normalized.byService.SVC.placements).toHaveLength(1);
    expect(
      normalized.byService.SVC.tabs.flatMap((tab) =>
        tab.sections.flatMap((section) =>
          section.subsections.flatMap((subsection) => [
            ...subsection.placements,
            ...subsection.tabs.flatMap((tab) => tab.placements),
          ]),
        ),
      ),
    ).toEqual([]);
    expect(normalized.diagnostics).toContainEqual(
      expect.objectContaining({
        code: "INVALID_PLACEMENT",
        message: expect.stringContaining("ambiguous"),
      }),
    );
  });

  it("preserves declaration order, parent-qualified IDs, and unique placement targets", () => {
    const theme = {
      name: "default",
      configuration: {
        placement: {
          configs: [
            { config: "site/value", "subsection-name": "later-subsection" },
            { config: "site/value", "subsection-name": "later-subsection" },
          ],
        },
        widgets: [
          { config: "site/value", widget: { type: "text-field" } },
        ],
        layouts: [
          {
            name: "layout-one",
            tabs: [
              {
                name: "settings",
                layout: {
                  sections: [
                    {
                      name: "later-section",
                      "row-index": 2,
                      subsections: [
                        { name: "later-subsection", "row-index": 2 },
                        { name: "earlier-subsection", "row-index": 0 },
                      ],
                    },
                    {
                      name: "earlier-section",
                      "row-index": 0,
                      subsections: [],
                    },
                  ],
                },
              },
            ],
          },
        ],
      },
    };
    const normalized = normalizeThemeResponse(
      responseFor("SVC", [{ fileName: "theme.json", theme }]),
      "default",
      ["SVC"],
    );
    const tab = normalized.byService.SVC.tabs[0];

    expect(tab.sections.map((section) => section.name)).toEqual([
      "later-section",
      "earlier-section",
    ]);
    expect(tab.sections[0].subsections.map((subsection) => subsection.name)).toEqual([
      "later-subsection",
      "earlier-subsection",
    ]);
    expect(tab.sections[0].id).toContain(":layout-one:settings:");
    expect(tab.sections[0].subsections[0].id).toContain(
      ":layout-one:settings:later-section:",
    );
    expect(normalized.byService.SVC.placements).toHaveLength(1);
    expect(tab.sections[0].subsections[0].placements).toHaveLength(1);
  });

  it("coerces the boolean encodings accepted by the Ember data model", () => {
    const theme = {
      name: "default",
      configuration: {
        placement: { configs: [] },
        widgets: [],
        layouts: [
          {
            name: "default",
            tabs: [
              {
                name: "layout",
                layout: {
                  sections: [
                    {
                      name: "section",
                      subsections: [
                        {
                          name: "subsection",
                          border: 1,
                          "left-vertical-splitter": "false",
                        },
                      ],
                    },
                  ],
                },
              },
            ],
          },
        ],
      },
    };
    const subsection = normalizeThemeResponse(
      responseFor("SVC", [{ fileName: "theme.json", theme }]),
      "default",
      ["SVC"],
    ).byService.SVC.tabs[0].sections[0].subsections[0];

    expect(subsection.border).toBe(true);
    expect(subsection.leftVerticalSplitter).toBe(false);
  });
});

describe("Service Theme conditions", () => {
  it("accepts every condition expression shipped by non-Metrics Theme JSON", () => {
    const conditions = shippedThemeConditions();
    const configConditions = conditions.filter(
      (condition) => condition.resource?.toLowerCase() !== "service",
    );
    const serviceConditions = conditions.filter(
      (condition) => condition.resource?.toLowerCase() === "service",
    );

    expect(configConditions.length).toBeGreaterThanOrEqual(53);
    expect(serviceConditions.length).toBeGreaterThanOrEqual(9);
    configConditions.forEach((condition) => {
      const result = evaluateConfigConditionResult(
        condition.if,
        configsForCondition(condition.if),
        "SHIPPED_THEME",
      );
      expect(result, `${condition.sourceFile}: ${condition.if}`).toMatchObject({
        valid: true,
        diagnostics: [],
      });
    });
    serviceConditions.forEach((condition) => {
      const diagnostics: ThemeConditionDiagnostic[] = [];
      resolveThemeConditionAttributes(
        [
          {
            resource: "service",
            if: condition.if,
            then: { property_value_attributes: { visible: true } },
          },
        ],
        {},
        "SHIPPED_THEME",
        [condition.if],
        diagnostics,
      );
      expect(diagnostics, `${condition.sourceFile}: ${condition.if}`).toEqual([]);
    });
  });

  it.each([
    ["${ranger-env/create_db_dbuser}", true],
    ["${ranger-env/create_db_dbuser} === true", true],
    ["${ranger-env/create_db_dbuser} === false", false],
    ["${ranger-env/source} === ldap", true],
    ["${other-site/source} === unix", true],
    ["${ranger-env/source} === unix", false],
    [
      "${ranger-env/create_db_dbuser} && ${ranger-env/source} === ldap",
      true,
    ],
    [
      "${ranger-env/create_db_dbuser} === false || ${other-site/source} === unix",
      true,
    ],
    ["${missing/source}", false],
  ])("evaluates Ember expression %s without basename collisions", (statement, expected) => {
    expect(evaluateConfigCondition(statement, configs, "RANGER")).toBe(expected);
  });

  it("uses logical AND precedence before OR", () => {
    expect(
      evaluateConfigCondition(
        "${ranger-env/create_db_dbuser} || ${ranger-env/source} === unix && ${other-site/source} === missing",
        configs,
        "RANGER",
      ),
    ).toBe(true);
  });

  it("supports resource=service and applies every depends-on entry in order", () => {
    const dependsOn: ThemeCondition[] = [
      {
        resource: "service",
        if: "HDFS",
        then: { property_value_attributes: { visible: false, read_only: true } },
        else: { property_value_attributes: { visible: true } },
      },
      {
        if: "${ranger-env/create_db_dbuser}",
        then: { property_value_attributes: { visible: true, value: "enabled" } },
        else: { property_value_attributes: { visible: false } },
      },
    ];

    expect(
      resolveThemeConditionAttributes(dependsOn, configs, "RANGER", ["HDFS"]),
    ).toEqual({ visible: true, read_only: true, value: "enabled" });
    expect(evaluateThemeVisibility(dependsOn, configs, "RANGER", ["HDFS"])).toBe(
      true,
    );
  });

  it("matches service conditions exactly and recomputes when the service set changes", () => {
    const dependsOn: ThemeCondition[] = [
      {
        resource: "service",
        if: "HDFS",
        then: { property_value_attributes: { visible: true } },
        else: { property_value_attributes: { visible: false } },
      },
    ];

    expect(evaluateThemeVisibility(dependsOn, configs, "RANGER", [])).toBe(
      false,
    );
    expect(
      evaluateThemeVisibility(dependsOn, configs, "RANGER", ["hdfs"]),
    ).toBe(false);
    expect(
      evaluateThemeVisibility(dependsOn, configs, "RANGER", ["HDFS"]),
    ).toBe(true);
    expect(
      evaluateThemeVisibility(dependsOn, configs, "RANGER", ["YARN"]),
    ).toBe(false);
  });

  it("defaults to visible when actions do not declare visibility", () => {
    expect(
      evaluateThemeVisibility(
        [
          {
            if: "${ranger-env/create_db_dbuser}",
            then: { property_value_attributes: { read_only: true } },
          },
        ],
        configs,
        "RANGER",
      ),
    ).toBe(true);
  });

  it.each([
    [undefined, "INVALID_CONDITION"],
    ["plain text", "INVALID_CONDITION"],
    ["${missing/value}", "MISSING_CONFIG_REFERENCE"],
    ["${ranger-env/source} !== ldap", "INVALID_CONDITION"],
    ["${ranger-env/source} === ldap;globalThis.pwned=true", "INVALID_CONDITION"],
    ["${__proto__/polluted}", "MISSING_CONFIG_REFERENCE"],
  ])("rejects unsafe condition %s and returns a safe diagnostic", (statement, code) => {
    const result = evaluateConfigConditionResult(statement, configs, "RANGER");
    expect(result).toMatchObject({ matches: false, valid: false });
    expect(result.diagnostics).toContainEqual(expect.objectContaining({ code }));
  });

  it("preserves earlier safe attributes when a later condition is invalid", () => {
    const diagnostics: ThemeConditionDiagnostic[] = [];
    const attributes = resolveThemeConditionAttributes(
      [
        {
          if: "${ranger-env/create_db_dbuser}",
          then: { property_value_attributes: { visible: false } },
        },
        {
          if: "globalThis.compromised = true",
          then: { property_value_attributes: { visible: true } },
          else: { property_value_attributes: { visible: true } },
        },
      ],
      configs,
      "RANGER",
      [],
      diagnostics,
    );

    expect(attributes).toEqual({ visible: false });
    expect(diagnostics).toContainEqual(
      expect.objectContaining({ code: "INVALID_CONDITION" }),
    );
  });

  it("never invokes dynamic execution or script injection for hostile metadata", () => {
    const evalSpy = vi.spyOn(globalThis, "eval");
    const functionSpy = vi.spyOn(globalThis, "Function");
    const createElementSpy = vi.spyOn(document, "createElement");

    try {
      [
        "globalThis.compromised = true",
        "${ranger-env/source} === ldap;globalThis.compromised=true",
        "${ranger-env/source} || (() => true)()",
        "${constructor/prototype} === polluted",
      ].forEach((statement) => {
        expect(
          evaluateConfigConditionResult(statement, configs, "RANGER").valid,
        ).toBe(false);
      });
      expect(evalSpy).not.toHaveBeenCalled();
      expect(functionSpy).not.toHaveBeenCalled();
      expect(createElementSpy).not.toHaveBeenCalled();
    } finally {
      evalSpy.mockRestore();
      functionSpy.mockRestore();
      createElementSpy.mockRestore();
    }
  });
});
