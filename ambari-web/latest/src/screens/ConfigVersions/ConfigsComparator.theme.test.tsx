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

import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  getConfigGroups: vi.fn(),
  getMultipleVersionConfigValues: vi.fn(),
}));

vi.mock("../../api/configsApi", () => ({
  default: {
    getConfigGroups: mocks.getConfigGroups,
    getMultipleVersionConfigValues: mocks.getMultipleVersionConfigValues,
  },
}));
vi.mock("../../components/Spinner", () => ({
  default: () => <div>Loading comparator</div>,
}));

import ConfigsComparator from "./ConfigsComparator";

const stackConfigs = {
  items: [
    {
      configurations: [
        {
          StackConfigurations: {
            type: "hive-site.xml",
            property_name: "shared",
            property_display_name: "Shared Property",
            property_value: "old",
            property_value_attributes: {},
            property_type: [] as string[],
            service_name: "HIVE",
          },
        },
      ],
    },
  ],
};

const versionResponse = {
  items: [
    {
      service_config_version: 1,
      service_name: "HIVE",
      group_name: "Default",
      configurations: [
        {
          type: "hive-site",
          tag: "version1",
          properties: { shared: "one" } as Record<string, string>,
          properties_attributes: {},
        },
      ],
    },
    {
      service_config_version: 2,
      service_name: "HIVE",
      group_name: "Default",
      configurations: [
        {
          type: "hive-site",
          tag: "version2",
          properties: { shared: "two" } as Record<string, string>,
          properties_attributes: {},
        },
      ],
    },
  ],
};

const themeResponse = {
  items: [
    {
      StackServices: { service_name: "HIVE" },
      themes: [
        {
          ThemeInfo: {
            service_name: "HIVE",
            theme_data: {
              Theme: {
                name: "database",
                configuration: {
                  placement: {
                    "configuration-layout": "database",
                    configs: [
                      {
                        config: "hive-site/shared",
                        "subsection-name": "nondefault-subsection",
                      },
                    ],
                  },
                  layouts: [
                    {
                      name: "database",
                      tabs: [
                        {
                          name: "nondefault-database",
                          "display-name": "Nondefault Database",
                          layout: { sections: [] },
                        },
                      ],
                    },
                  ],
                  widgets: [],
                },
              },
            },
          },
        },
        {
          ThemeInfo: {
            service_name: "HIVE",
            theme_data: {
              Theme: {
                name: "default",
                configuration: {
                  placement: {
                    "configuration-layout": "default",
                    configs: [
                      {
                        config: "hive-site/shared",
                        "subsection-name": "default-subsection",
                      },
                    ],
                  },
                  layouts: [
                    {
                      name: "default",
                      tabs: [
                        {
                          name: "hive-database",
                          "display-name": "Default Database",
                          layout: {
                            "tab-rows": 1,
                            "tab-columns": 1,
                            sections: [
                              {
                                name: "default-section",
                                "row-index": 0,
                                "column-index": 0,
                                "row-span": 1,
                                "column-span": 1,
                                "section-rows": 1,
                                "section-columns": 1,
                                subsections: [
                                  {
                                    name: "default-subsection",
                                    "display-name": "Default Property",
                                    "row-index": 0,
                                    "column-index": 0,
                                    "row-span": 1,
                                    "column-span": 1,
                                  },
                                ],
                              },
                            ],
                          },
                        },
                      ],
                    },
                  ],
                  widgets: [],
                },
              },
            },
          },
        },
      ],
    },
  ],
};

const renderComparator = (themeData: unknown, stackConfigData = stackConfigs) =>
  render(
    <ConfigsComparator
      version1="1"
      version2="2"
      defaultVersion="2"
      clusterName="c1"
      serviceName="HIVE"
      configs={stackConfigData}
      themeData={themeData}
      currentVersion="2"
    />,
  );

const deepFreeze = (value: unknown): unknown => {
  if (value && typeof value === "object") {
    Object.freeze(value);
    Object.values(value).forEach(deepFreeze);
  }
  return value;
};

describe("Config versions comparator Theme integration", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getConfigGroups.mockResolvedValue({ items: [] });
    mocks.getMultipleVersionConfigValues.mockResolvedValue(versionResponse);
  });

  afterEach(cleanup);

  it("renders tabs from every server-default Theme", async () => {
    renderComparator(themeResponse);

    expect(
      await screen.findByRole("tab", { name: "Default Database (1)" }),
    ).toBeTruthy();
    expect(screen.getByRole("tab", { name: "Advanced (0)" })).toBeTruthy();
    expect(
      screen.getByRole("tab", { name: "Nondefault Database (0)" }),
    ).toBeTruthy();
    fireEvent.click(screen.getByRole("tab", { name: "Default Database (1)" }));
    expect(await screen.findByText("Default Property")).toBeTruthy();
  });

  it("renders the Advanced fallback without mutating malformed Theme data", async () => {
    const malformed = deepFreeze({
      items: [
        {
          StackServices: { service_name: "HIVE" },
          themes: [
            {
              ThemeInfo: {
                service_name: "HIVE",
                theme_data: {
                  Theme: {
                    name: "default",
                    configuration: { placement: {} },
                  },
                },
              },
            },
          ],
        },
      ],
    });

    renderComparator(malformed);

    expect(
      await screen.findByRole("tab", { name: "Advanced (1)" }),
    ).toBeTruthy();
    expect(Object.isFrozen(malformed)).toBe(true);
  });

  it("does not render a password that exists in only one compared version", async () => {
    const responseWithPassword = structuredClone(versionResponse);
    responseWithPassword.items[0].configurations[0].properties.db_password =
      "version-one-secret";
    const configsWithPassword = structuredClone(stackConfigs);
    configsWithPassword.items[0].configurations.push({
      StackConfigurations: {
        type: "hive-site.xml",
        property_name: "db_password",
        property_display_name: "Database Password",
        property_value: "stack-secret",
        property_value_attributes: { type: "password" },
        property_type: ["PASSWORD"],
        service_name: "HIVE",
      },
    });
    mocks.getMultipleVersionConfigValues.mockResolvedValueOnce(
      responseWithPassword,
    );

    renderComparator(themeResponse, configsWithPassword);

    expect(await screen.findByText("Default Property")).toBeTruthy();
    expect(document.body.textContent).not.toContain("version-one-secret");
    expect(document.body.textContent).not.toContain("stack-secret");
    expect(screen.queryByText("Database Password")).toBeNull();
  });

  it("keeps same-named properties from different config types independent", async () => {
    const responseWithCollision = structuredClone(versionResponse);
    responseWithCollision.items.forEach((item, index) => {
      item.configurations.push({
        type: "other-site",
        tag: `other-version${index + 1}`,
        properties: { shared: index === 0 ? "other-one" : "other-two" },
        properties_attributes: {},
      });
    });
    const configsWithCollision = structuredClone(stackConfigs);
    configsWithCollision.items[0].configurations[0].StackConfigurations
      .property_display_name = "Hive Shared";
    configsWithCollision.items[0].configurations.push({
      StackConfigurations: {
        type: "other-site.xml",
        property_name: "shared",
        property_display_name: "Other Shared",
        property_value: "old-other",
        property_value_attributes: {},
        property_type: [] as string[],
        service_name: "HIVE",
      },
    });
    const themeWithCollision = structuredClone(themeResponse);
    const defaultTheme = themeWithCollision.items[0].themes[1].ThemeInfo
      .theme_data.Theme;
    defaultTheme.configuration.placement.configs.push({
      config: "other-site/shared",
      "subsection-name": "default-subsection",
    });
    mocks.getMultipleVersionConfigValues.mockResolvedValueOnce(
      responseWithCollision,
    );

    renderComparator(themeWithCollision, configsWithCollision);

    expect(await screen.findByText("Hive Shared")).toBeTruthy();
    expect(screen.getByText("Other Shared")).toBeTruthy();
    expect(screen.getByText("one")).toBeTruthy();
    expect(screen.getByText("two")).toBeTruthy();
    expect(screen.getByText("other-one")).toBeTruthy();
    expect(screen.getByText("other-two")).toBeTruthy();
  });
});
