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

import { describe, expect, it } from "vitest";
import {
  buildConfigsJSON,
  filterConfigProperties,
  getThemePlacementProperty,
  setTabErrorCounts,
  updateVisibilityForDependsOn,
} from "./ConfigUtils";
import { ConfigPropertiesType } from "./types";
import { normalizeDefaultThemeResponse } from "./themeEngine";

type Placement = {
  config: string;
  "subsection-name": string;
  "subsection-tab-name"?: string;
  "depends-on"?: unknown[];
  property_value_attributes?: Record<string, unknown>;
};

const configProperty = (
  propertyName: string,
  value: unknown,
  overrides: Record<string, unknown> = {},
) => ({
  propertyName,
  propertyDisplayname: propertyName,
  propertyValue: value,
  propertyAttributes: {},
  previousValue: value,
  value,
  final: "false",
  isEditable: true,
  isVisible: true,
  isHidden: false,
  ...overrides,
});

const configs = (): ConfigPropertiesType => ({
  SVC: {
    "type-a": {
      errors: 0,
      properties: {
        shared: configProperty("shared", "a", {
          fileName: "type-a.xml",
          errorMessage: "existing error",
        }),
        switch: configProperty("switch", "false", {
          fileName: "type-a.xml",
        }),
        staticHidden: configProperty("staticHidden", "value", {
          fileName: "type-a.xml",
          isHidden: true,
          isVisible: false,
        }),
      },
    },
    "type-b": {
      errors: 0,
      properties: {
        shared: configProperty("shared", "b", {
          fileName: "type-b.xml",
        }),
        tabbed: configProperty("tabbed", "value", {
          fileName: "type-b.xml",
        }),
      },
    },
  },
});

const condition = (
  expression: string,
  visibleWhenTrue: boolean,
  resource = "config",
) => ({
  resource,
  if: expression,
  then: { property_value_attributes: { visible: visibleWhenTrue } },
  else: { property_value_attributes: { visible: !visibleWhenTrue } },
});

const themeResponse = ({
  themeName = "default",
  placements,
  subsectionDependsOn = [],
  subsectionTabs = [],
}: {
  themeName?: string;
  placements: Placement[];
  subsectionDependsOn?: unknown[];
  subsectionTabs?: unknown[];
}) => ({
  items: [
    {
      StackServices: { service_name: "SVC" },
      themes: [
        {
          ThemeInfo: {
            service_name: "SVC",
            file_name: `${themeName}.json`,
            theme_data: {
              Theme: {
                name: themeName,
                configuration: {
                  placement: {
                    "configuration-layout": themeName,
                    configs: placements,
                  },
                  widgets: placements.map((placement) => ({
                    config: placement.config,
                    widget: { type: "text-field" },
                  })),
                  layouts: [
                    {
                      name: themeName,
                      tabs: [
                        {
                          name: "settings",
                          layout: {
                            sections: [
                              {
                                name: "section",
                                subsections: [
                                  {
                                    name: "main",
                                    "depends-on": subsectionDependsOn,
                                    "subsection-tabs": subsectionTabs,
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
              },
            },
          },
        },
      ],
    },
  ],
});

describe("Service Theme config visibility", () => {
  it.each([
    ["description", "searchable description"],
    ["saved value", "saved-before-edit"],
    ["override value", "group-specific-value"],
    ["override group", "analytics hosts"],
    ["falsy numeric value", "0"],
  ])("searches the %s", (_label, search) => {
    const source = configs();
    const property = source.SVC["type-a"].properties.shared;
    property.propertyDescription = "Searchable description";
    property.savedValue = "saved-before-edit";
    property.value = 0;
    property.overrideValues = [
      {
        value: "group-specific-value",
        groupName: "Analytics Hosts",
      },
    ];

    const result = filterConfigProperties(source, search);

    expect(result.SVC["type-a"].properties.shared.isVisible).toBe(true);
    expect(result.SVC["type-b"].properties.shared.isVisible).toBe(false);
  });

  it("never indexes password values while retaining password metadata search", () => {
    const source = configs();
    const password = source.SVC["type-a"].properties.shared;
    password.propertyDisplayname = "Database password";
    password.propertyAttributes = { type: "password" };
    password.value = "current-secret";
    password.savedValue = "saved-secret";
    password.overrideValues = [
      { value: "override-secret", groupName: "Sensitive group" },
    ];

    expect(
      filterConfigProperties(source, "current-secret").SVC["type-a"].properties
        .shared.isVisible,
    ).toBe(false);
    expect(
      filterConfigProperties(source, "saved-secret").SVC["type-a"].properties
        .shared.isVisible,
    ).toBe(false);
    expect(
      filterConfigProperties(source, "override-secret").SVC["type-a"].properties
        .shared.isVisible,
    ).toBe(false);
    expect(
      filterConfigProperties(source, "Database password").SVC["type-a"]
        .properties.shared.isVisible,
    ).toBe(true);
  });

  it("combines selected property filters with AND semantics", () => {
    const source = configs();
    source.SVC["type-a"].properties.shared.final = "true";
    source.SVC["type-a"].properties.shared.hasError = true;
    source.SVC["type-a"].properties.shared.overrideValues = [
      { value: "a", groupName: "group-a" },
    ];
    source.SVC["type-b"].properties.shared.final = "true";
    source.SVC["type-b"].properties.shared.hasError = true;
    source.SVC["type-b"].properties.tabbed.overrideValues = [
      { value: "value", groupName: "group-b" },
    ];

    const result = filterConfigProperties(source, "", {
      showOverridden: true,
      showFinal: true,
      showIssues: true,
    });

    expect(result.SVC["type-a"].properties.shared.isVisible).toBe(true);
    expect(result.SVC["type-b"].properties.shared.isVisible).toBe(false);
    expect(result.SVC["type-b"].properties.tabbed.isVisible).toBe(false);
  });

  it("counts errors only for effectively visible properties", () => {
    const source = configs();
    source.SVC["type-a"].properties.shared.tabName = "General";
    source.SVC["type-a"].properties.shared.errorMessage = "visible failure";
    source.SVC["type-b"].properties.shared.tabName = "General";
    source.SVC["type-b"].properties.shared.errorMessage = "hidden failure";
    source.SVC["type-b"].properties.shared.isVisible = false;

    expect(setTabErrorCounts(source)).toEqual({
      SVC: {
        total: 1,
        tabs: {
          General: 1,
          Advanced: 0,
        },
      },
    });
  });

  it("excludes Theme UI-only properties from the canonical save payload", () => {
    const source = configs();
    Object.values(source.SVC["type-a"].properties).forEach((property) => {
      property.type = "type-a";
    });
    Object.values(source.SVC["type-b"].properties).forEach((property) => {
      property.type = "type-b";
    });
    source.SVC["type-a"].properties.shared.isRequiredByAgent = false;

    expect(buildConfigsJSON(source)).toEqual({
      "type-a": { properties: { switch: "false", staticHidden: "value" } },
      "type-b": { properties: { shared: "b", tabbed: "value" } },
    });
    expect(JSON.stringify(buildConfigsJSON(source))).not.toContain(
      'shared":"a',
    );
  });

  it("saves a password value without its confirmation field", () => {
    const source = configs();
    const password = source.SVC["type-a"].properties.shared;
    password.type = "type-a";
    password.propertyAttributes = { type: "password" };
    password.value = "saved-password";
    password.confirmPassword = "saved-password";

    const payload = buildConfigsJSON(source);
    expect(payload["type-a"].properties.shared).toBe("saved-password");
    expect(JSON.stringify(payload)).not.toContain("confirmPassword");
  });

  it("uses the full config path and restores a property when its condition changes", () => {
    const theme = themeResponse({
      placements: [
        {
          config: "type-a/shared",
          "subsection-name": "main",
          "depends-on": [condition("${type-a/switch}", true)],
        },
        { config: "type-b/shared", "subsection-name": "main" },
      ],
    });

    const hidden = updateVisibilityForDependsOn(configs(), theme, "default", []);
    expect(hidden.SVC["type-a"].properties.shared).toMatchObject({
      isVisible: false,
      isHidden: false,
      errorMessage: "",
    });
    expect(hidden.SVC["type-b"].properties.shared.isVisible).toBe(true);

    hidden.SVC["type-a"].properties.switch.value = "true";
    const restored = updateVisibilityForDependsOn(
      hidden,
      theme,
      "default",
      [],
    );
    expect(restored.SVC["type-a"].properties.shared.isVisible).toBe(true);
    expect(restored.SVC["type-b"].properties.shared.isVisible).toBe(true);
  });

  it("keeps visibility and attributes isolated for repeated paths across Themes", () => {
    const directories = themeResponse({
      themeName: "directories",
      placements: [
        {
          config: "type-a/shared",
          "subsection-name": "main",
          property_value_attributes: { read_only: true },
        },
      ],
    });
    const hiddenDefault = themeResponse({
      placements: [
        {
          config: "type-a/shared",
          "subsection-name": "main",
          "depends-on": [condition("${type-a/switch}", true)],
        },
      ],
    });
    directories.items[0].themes.push(hiddenDefault.items[0].themes[0]);

    const normalized = normalizeDefaultThemeResponse(directories, ["SVC"]);
    const [directoriesPlacement, defaultPlacement] =
      normalized.byService.SVC.placements;
    const property = updateVisibilityForDependsOn(
      configs(),
      directories,
      "default",
      [],
      true,
    ).SVC["type-a"].properties.shared;

    expect(property.isVisible).toBe(true);
    expect(property.propertyAttributes.visible).toBe(true);
    expect(
      getThemePlacementProperty(property, directoriesPlacement.id),
    ).toMatchObject({ isVisible: true, isEditable: false });
    expect(
      getThemePlacementProperty(property, defaultPlacement.id),
    ).toMatchObject({ isVisible: false, isEditable: true });
  });

  it("does not let a config condition reveal a statically hidden property", () => {
    const theme = themeResponse({
      placements: [
        {
          config: "type-a/staticHidden",
          "subsection-name": "main",
          "depends-on": [condition("${type-a/switch} === false", true)],
        },
      ],
    });

    const result = updateVisibilityForDependsOn(configs(), theme, "default", []);
    expect(result.SVC["type-a"].properties.staticHidden).toMatchObject({
      isVisible: false,
      isHidden: true,
    });
  });

  it("requires subsection, subsection-tab, and config conditions to all be visible", () => {
    const theme = themeResponse({
      placements: [
        {
          config: "type-b/tabbed",
          "subsection-name": "main",
          "subsection-tab-name": "details",
          "depends-on": [condition("${type-a/switch} === false", true)],
        },
      ],
      subsectionDependsOn: [condition("${type-a/switch}", true)],
      subsectionTabs: [
        {
          name: "details",
          "display-name": "Details",
          "depends-on": [condition("${type-a/switch} === false", true)],
        },
      ],
    });

    const result = updateVisibilityForDependsOn(configs(), theme, "default", []);
    expect(result.SVC["type-b"].properties.tabbed.isVisible).toBe(false);
  });

  it("evaluates service-resource conditions against installed services", () => {
    const theme = themeResponse({
      placements: [
        {
          config: "type-a/shared",
          "subsection-name": "main",
          "depends-on": [condition("HDFS", true, "service")],
        },
      ],
    });

    expect(
      updateVisibilityForDependsOn(configs(), theme, "default", []).SVC[
        "type-a"
      ].properties.shared.isVisible,
    ).toBe(false);
    expect(
      updateVisibilityForDependsOn(configs(), theme, "default", ["HDFS"])
        .SVC["type-a"].properties.shared.isVisible,
    ).toBe(true);
  });

  it("selects the requested non-default Theme", () => {
    const database = themeResponse({
      themeName: "database",
      placements: [
        {
          config: "type-a/shared",
          "subsection-name": "main",
          "depends-on": [condition("${type-a/switch}", true)],
        },
      ],
    });

    const result = updateVisibilityForDependsOn(
      configs(),
      database,
      "database",
      [],
    );
    expect(result.SVC["type-a"].properties.shared.isVisible).toBe(false);
  });

  it("restores base visibility when a later Theme response is malformed", () => {
    const theme = themeResponse({
      placements: [
        {
          config: "type-a/shared",
          "subsection-name": "main",
          "depends-on": [condition("${type-a/switch}", true)],
        },
      ],
    });
    const hidden = updateVisibilityForDependsOn(configs(), theme, "default", []);

    const recovered = updateVisibilityForDependsOn(
      hidden,
      { items: [{ StackServices: { service_name: "SVC" }, themes: [{}] }] },
      "default",
      [],
    );
    expect(recovered.SVC["type-a"].properties.shared.isVisible).toBe(true);
  });

  it("applies the static Ember value-attribute mapping", () => {
    const theme = themeResponse({
      placements: [
        {
          config: "type-a/shared",
          "subsection-name": "main",
          property_value_attributes: {
            overridable: false,
            empty_value_valid: true,
            editable_only_at_install: true,
            show_property_name: false,
            read_only: true,
            ui_only_property: true,
            unit: "MB",
            custom_theme_attribute: "static",
          },
        },
      ],
    });

    const property = updateVisibilityForDependsOn(
      configs(),
      theme,
      "default",
      [],
    ).SVC["type-a"].properties.shared;
    expect(property).toMatchObject({
      isOverridable: false,
      isRequired: false,
      isReconfigurable: false,
      showLabel: false,
      isEditable: false,
      isRequiredByAgent: false,
      unit: "MB",
      custom_theme_attribute: "static",
    });
  });

  it("applies config condition attributes in order and restores omitted attributes", () => {
    const conditionAttributes = (
      value: string,
      showLabel: boolean,
      editable: boolean,
      unit: string,
    ) => ({
      resource: "config",
      if: "${type-a/switch}",
      then: {
        property_value_attributes: {
          value,
          show_property_name: showLabel,
          read_only: editable,
          ui_only_property: editable,
          unit,
        },
      },
      else: { property_value_attributes: {} },
    });
    const theme = themeResponse({
      placements: [
        {
          config: "type-a/shared",
          "subsection-name": "main",
          "depends-on": [
            conditionAttributes("first", false, false, "MB"),
            conditionAttributes("second", true, true, "GB"),
          ],
        },
      ],
    });
    const initial = configs();
    initial.SVC["type-a"].properties.switch.value = "true";

    const applied = updateVisibilityForDependsOn(initial, theme, "default", []);
    expect(applied.SVC["type-a"].properties.shared).toMatchObject({
      value: "second",
      showLabel: true,
      isEditable: false,
      isRequiredByAgent: false,
      unit: "GB",
    });

    applied.SVC["type-a"].properties.switch.value = "false";
    const restored = updateVisibilityForDependsOn(
      applied,
      theme,
      "default",
      [],
    ).SVC["type-a"].properties.shared;
    expect(restored.value).toBe("a");
    expect(restored.isEditable).toBe(true);
    expect("isRequiredByAgent" in restored).toBe(false);
    expect("showLabel" in restored).toBe(false);
    expect("unit" in restored).toBe(false);
  });

  it("does not let Theme attributes bypass an existing edit restriction", () => {
    const initial = configs();
    initial.SVC["type-a"].properties.shared.isEditable = false;
    const theme = themeResponse({
      placements: [
        {
          config: "type-a/shared",
          "subsection-name": "main",
          "depends-on": [
            {
              resource: "config",
              if: "${type-a/switch} === false",
              then: {
                property_value_attributes: {
                  read_only: false,
                  overridable: true,
                  visible: true,
                },
              },
              else: { property_value_attributes: {} },
            },
          ],
        },
      ],
    });

    const applied = updateVisibilityForDependsOn(initial, theme, "default", []);
    expect(applied.SVC["type-a"].properties.shared.isEditable).toBe(false);

    const restored = updateVisibilityForDependsOn(
      applied,
      { items: [] },
      "default",
      [],
    );
    expect(restored.SVC["type-a"].properties.shared.isEditable).toBe(false);
    expect("isOverridable" in restored.SVC["type-a"].properties.shared).toBe(
      false,
    );
  });
});
