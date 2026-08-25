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

import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  findComparatorThemeLocation,
  getComparatorActiveTabs,
  normalizeComparatorTheme,
} from "./configsComparatorTheme";

const repoRoot = resolve(
  dirname(fileURLToPath(import.meta.url)),
  "../../../../..",
);

const theme = (
  name: string,
  layoutName: string,
  tabs: unknown[],
  placements: unknown[],
  widgets: unknown[] = [],
) => ({
  name,
  configuration: {
    placement: {
      "configuration-layout": layoutName,
      configs: placements,
    },
    layouts: [{ name: layoutName, tabs }],
    widgets,
  },
});

const response = (...themes: unknown[]) => ({
  items: [
    {
      StackServices: { service_name: "HIVE" },
      themes: themes.map((item, index) => ({
        ThemeInfo: {
          file_name: `theme-${index}.json`,
          service_name: "HIVE",
          theme_data: { Theme: item },
        },
      })),
    },
  ],
});

const tab = (
  name: string,
  sections: unknown[],
  displayName = name,
) => ({
  name,
  "display-name": displayName,
  layout: {
    "tab-rows": 2,
    "tab-columns": 2,
    sections,
  },
});

const section = (
  name: string,
  columnIndex: number,
  subsections: unknown[],
) => ({
  name,
  "row-index": 0,
  "column-index": columnIndex,
  "col-index": 99,
  "row-span": 1,
  "column-span": 1,
  "section-rows": 1,
  "section-columns": 2,
  subsections,
});

const subsection = (
  name: string,
  displayName: string,
  columnIndex = 0,
) => ({
  name,
  "display-name": displayName,
  "row-index": 0,
  "column-index": columnIndex,
  "row-span": 1,
  "column-span": 1,
});

describe("Config versions comparator Theme adapter", () => {
  it("maps the real BIGTOP HIVE default Theme used by the comparator", () => {
    const hiveTheme = JSON.parse(
      readFileSync(
        resolve(
          repoRoot,
          "ambari-server/src/main/resources/stacks/BIGTOP/3.2.0/services/HIVE/themes/theme.json",
        ),
        "utf8",
      ),
    );
    const result = normalizeComparatorTheme(response(hiveTheme), "HIVE");

    expect(
      getComparatorActiveTabs(result, "HIVE").map((item) => item.name),
    ).toContain("hive-database");
    const location = findComparatorThemeLocation(
      result,
      "HIVE",
      "hive-env",
      "test_db_connection",
    );
    expect(location).toMatchObject({
      tabName: "hive-database",
      categoryDisplayName: "hive-database-configurations-col-2",
    });
    expect(location?.categoryName).toContain(
      ":hive-database-configurations-col-2",
    );
    expect(result.HIVE.widgets["hive-env/test_db_connection"]).toMatchObject({
      configPath: "hive-env/test_db_connection",
      type: "test-db-connection",
    });
  });

  it("uses only the default Theme and retains full config paths", () => {
    const input = response(
      theme(
        "database",
        "database",
        [
          tab("database", [
            section("database-section", 0, [
              subsection("database-subsection", "Database"),
            ]),
          ]),
        ],
        [
          {
            config: "hive-site/shared",
            "subsection-name": "database-subsection",
          },
        ],
        [{ config: "hive-site/shared", widget: { type: "text-field" } }],
      ),
      theme(
        "default",
        "default",
        [
          tab("settings", [
            section("settings-section", 0, [
              subsection("hive-subsection", "Hive Property", 0),
              subsection("other-subsection", "Other Property", 1),
            ]),
          ]),
          tab("hive-database", []),
        ],
        [
          { config: "hive-site/shared", "subsection-name": "hive-subsection" },
          { config: "other-site/shared", "subsection-name": "other-subsection" },
        ],
        [
          { config: "hive-site/shared", widget: { type: "text-field" } },
          { config: "other-site/shared", widget: { type: "password" } },
        ],
      ),
    );

    const result = normalizeComparatorTheme(input, "HIVE");

    expect(Object.keys(result.HIVE.tabs)).toEqual([
      "settings",
      "hive-database",
      "Advanced",
    ]);
    expect(result.HIVE.subsectionProperties["hive-subsection"].properties[0])
      .toMatchObject({
        configPath: "hive-site/shared",
        configType: "hive-site",
        propertyName: "shared",
      });
    expect(Object.keys(result.HIVE.widgets)).toEqual([
      "hive-site/shared",
      "other-site/shared",
    ]);
    expect(result.HIVE.widgets["hive-site/shared"].type).toBe("text-field");
    expect(result.HIVE.widgets["other-site/shared"].type).toBe("password");
    expect(
      result.HIVE.subsectionProperties["database-subsection"],
    ).toBeUndefined();
  });

  it("locates same-named properties by config type and property name", () => {
    const result = normalizeComparatorTheme(
      response(
        theme(
          "default",
          "default",
          [
            tab("settings", [
              section("settings-section", 0, [
                subsection("hive-subsection", "Hive Property", 0),
                subsection("other-subsection", "Other Property", 1),
              ]),
            ]),
          ],
          [
            { config: "hive-site/shared", "subsection-name": "hive-subsection" },
            { config: "other-site/shared", "subsection-name": "other-subsection" },
          ],
        ),
      ),
      "HIVE",
    );

    expect(
      findComparatorThemeLocation(result, "HIVE", "hive-site", "shared"),
    ).toMatchObject({
      tabName: "settings",
      categoryDisplayName: "Hive Property",
    });
    expect(
      findComparatorThemeLocation(result, "HIVE", "other-site", "shared"),
    ).toMatchObject({
      tabName: "settings",
      categoryDisplayName: "Other Property",
    });
    expect(
      findComparatorThemeLocation(result, "HIVE", "missing-site", "shared"),
    ).toBeNull();
  });

  it("reads column-index and preserves section and subsection declaration order", () => {
    const result = normalizeComparatorTheme(
      response(
        theme(
          "default",
          "default",
          [
            tab("settings", [
              section("right", 1, [
                subsection("right-second", "Right Second", 1),
                subsection("right-first", "Right First", 0),
              ]),
              section("left", 0, [subsection("left-first", "Left First")]),
            ]),
          ],
          [],
        ),
      ),
      "HIVE",
    );
    const sections = result.HIVE.tabs.settings.sections;

    expect(sections.map((item) => [item.name, item.columnIndex])).toEqual([
      ["right", 1],
      ["left", 0],
    ]);
    expect(
      sections[0].subsections.map((item) => [item.name, item.columnIndex]),
    ).toEqual([
      ["right-second", 1],
      ["right-first", 0],
    ]);
  });

  it("preserves layout-qualified identities for duplicate local tab names", () => {
    const duplicateTabs = {
      name: "default",
      configuration: {
        placement: {
          configs: [
            { config: "one-site/value", "subsection-name": "one-subsection" },
            { config: "two-site/value", "subsection-name": "two-subsection" },
          ],
        },
        layouts: [
          {
            name: "layout-one",
            tabs: [
              tab("settings", [
                section("one-section", 0, [
                  subsection("one-subsection", "First Settings"),
                ]),
              ]),
            ],
          },
          {
            name: "layout-two",
            tabs: [
              tab("settings", [
                section("two-section", 0, [
                  subsection("two-subsection", "Second Settings"),
                ]),
              ]),
            ],
          },
        ],
        widgets: [],
      },
    };
    const result = normalizeComparatorTheme(response(duplicateTabs), "HIVE");

    expect(getComparatorActiveTabs(result, "HIVE").map((item) => item.key))
      .toEqual(["layout-one:settings", "layout-two:settings", "Advanced"]);
    expect(
      findComparatorThemeLocation(result, "HIVE", "one-site", "value")
        ?.tabName,
    ).toBe("layout-one:settings");
    expect(
      findComparatorThemeLocation(result, "HIVE", "two-site", "value")
        ?.tabName,
    ).toBe("layout-two:settings");
  });

  it.each([
    ["empty", {}],
    [
      "malformed",
      response({ name: "default", configuration: { placement: {} } }),
    ],
  ])("falls back to Advanced for an %s response", (_case, input) => {
    const snapshot = JSON.stringify(input);
    const result = normalizeComparatorTheme(input, "HIVE");

    expect(getComparatorActiveTabs(result, "HIVE").map((item) => item.name))
      .toEqual(["Advanced"]);
    expect(result.HIVE.isFallback).toBe(true);
    expect(JSON.stringify(input)).toBe(snapshot);
  });
});
