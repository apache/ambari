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

import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
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
vi.mock("./ComparatorFilter", () => ({
  default: ({ setSelectedFilters }: { setSelectedFilters: (filters: unknown[]) => void }) => (
    <button onClick={() => setSelectedFilters([])}>Show all properties</button>
  ),
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
                        "subsection-tab-name": "connection",
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
                                    "subsection-tabs": [
                                      {
                                        name: "connection",
                                        "display-name": "Connection",
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

const comparatorElement = (
  themeData: unknown,
  stackConfigData = stackConfigs,
  clusterName = "c1",
) =>
  <ConfigsComparator
    version1="1"
    version2="2"
    defaultVersion="2"
    clusterName={clusterName}
    serviceName="HIVE"
    configs={stackConfigData}
    themeData={themeData}
    currentVersion="2"
  />;

const renderComparator = (themeData: unknown, stackConfigData = stackConfigs) =>
  render(
    comparatorElement(themeData, stackConfigData),
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
      "subsection-tab-name": "connection",
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

  it("renders added, removed, changed, and unchanged values with exact fallback semantics", async () => {
    const response = structuredClone(versionResponse);
    response.items[0].configurations[0].properties = {
      shared: "one",
      removed: "old-only",
      unchanged: "same",
      empty: "",
      db_password: "secret-one",
      ui_action: "run-one",
      stack_ui_action: "stack-run-one",
    };
    response.items[1].configurations[0].properties = {
      shared: "two",
      added: "new-only",
      unchanged: "same",
      empty: "",
      db_password: "secret-two",
      ui_action: "run-two",
      stack_ui_action: "stack-run-two",
    };
    const stackData: any = structuredClone(stackConfigs);
    const configurations = stackData.items[0].configurations;
    ["removed", "unchanged", "empty", "added"].forEach((propertyName) => {
      configurations.push({
        StackConfigurations: {
          type: "hive-site.xml",
          property_name: propertyName,
          property_display_name:
            propertyName === "removed" ? "Legacy Removed Setting" : propertyName,
          property_value: "",
          property_value_attributes: {},
          property_type: [] as string[],
          service_name: "HIVE",
        },
      });
    });
    configurations.push({
      StackConfigurations: {
        type: "hive-site.xml",
        property_name: "db_password",
        property_display_name: "Database Password",
        property_value: "stack-secret",
        property_value_attributes: {},
        property_type: ["PASSWORD"],
        service_name: "HIVE",
      },
    });
    configurations.push({
      StackConfigurations: {
        type: "hive-site.xml",
        property_name: "stack_ui_action",
        property_display_name: "Stack UI Action",
        property_value: "run",
        property_value_attributes: {},
        property_type: [] as string[],
        is_required_by_agent: false,
        service_name: "HIVE",
      },
    });
    const themeWithUiOnly = structuredClone(themeResponse);
    themeWithUiOnly.items[0].themes[1].ThemeInfo.theme_data.Theme.configuration
      .placement.configs.push({
        config: "hive-site/ui_action",
        "subsection-name": "default-subsection",
        property_value_attributes: { ui_only_property: true },
      } as any);
    configurations.push({
      StackConfigurations: {
        type: "hive-site.xml",
        property_name: "ui_action",
        property_display_name: "UI Action",
        property_value: "run",
        property_value_attributes: {},
        property_type: [] as string[],
        service_name: "HIVE",
      },
    });
    mocks.getMultipleVersionConfigValues.mockResolvedValueOnce(response);

    const { container } = renderComparator(themeWithUiOnly, stackData);

    fireEvent.click(await screen.findByRole("tab", { name: "Advanced (2)" }));
    expect(container.querySelector('[data-config-path="hive-site/removed"]')).toBeTruthy();
    expect(container.querySelector('[data-config-path="hive-site/added"]')).toBeTruthy();
    expect(screen.getAllByText("Undefined")).toHaveLength(2);
    expect(document.body.textContent).not.toContain("secret-one");
    expect(document.body.textContent).not.toContain("secret-two");
    expect(screen.queryByText("UI Action")).toBeNull();
    expect(screen.queryByText("Stack UI Action")).toBeNull();

    fireEvent.change(screen.getByPlaceholderText("Search properties..."), {
      target: { value: "Legacy Removed" },
    });
    expect(container.querySelector('[data-config-path="hive-site/removed"]')).toBeTruthy();
    expect(container.querySelector('[data-config-path="hive-site/added"]')).toBeNull();
    fireEvent.change(screen.getByPlaceholderText("Search properties..."), {
      target: { value: "" },
    });

    fireEvent.click(screen.getByRole("button", { name: "Show all properties" }));
    expect(await screen.findByText("unchanged")).toBeTruthy();
    const emptyRow = container.querySelector('[data-config-path="hive-site/empty"]');
    expect(emptyRow).toBeTruthy();
    expect(emptyRow?.textContent).not.toContain("Undefined");
  });

  it("ignores a comparison response from an obsolete cluster context", async () => {
    let resolveOld: (value: typeof versionResponse) => void = () => undefined;
    let resolveNew: (value: typeof versionResponse) => void = () => undefined;
    const oldRequest = new Promise<typeof versionResponse>((resolve) => {
      resolveOld = resolve;
    });
    const newRequest = new Promise<typeof versionResponse>((resolve) => {
      resolveNew = resolve;
    });
    mocks.getMultipleVersionConfigValues
      .mockReset()
      .mockReturnValueOnce(oldRequest)
      .mockReturnValueOnce(newRequest);

    const view = render(comparatorElement(themeResponse, stackConfigs, "old"));
    await waitFor(() =>
      expect(mocks.getMultipleVersionConfigValues).toHaveBeenCalledWith(
        "old", "HIVE", "1", "2",
      ),
    );
    view.rerender(comparatorElement(themeResponse, stackConfigs, "new"));

    const newResponse = structuredClone(versionResponse);
    newResponse.items[0].configurations[0].properties.shared = "new-one";
    newResponse.items[1].configurations[0].properties.shared = "new-two";
    await act(async () => resolveNew(newResponse));
    expect(await screen.findByText("new-one")).toBeTruthy();

    const oldResponse = structuredClone(versionResponse);
    oldResponse.items[0].configurations[0].properties.shared = "stale-one";
    oldResponse.items[1].configurations[0].properties.shared = "stale-two";
    await act(async () => resolveOld(oldResponse));
    expect(screen.queryByText("stale-one")).toBeNull();
    expect(screen.getByText("new-one")).toBeTruthy();
  });

  it("recovers after the comparison request fails", async () => {
    mocks.getMultipleVersionConfigValues
      .mockRejectedValueOnce(new Error("Version request failed"))
      .mockResolvedValueOnce(versionResponse);
    renderComparator(themeResponse);

    expect(await screen.findByText("Version request failed")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Retry comparison" }));

    expect(await screen.findByText("one")).toBeTruthy();
    expect(mocks.getMultipleVersionConfigValues).toHaveBeenCalledTimes(2);
  });
});
