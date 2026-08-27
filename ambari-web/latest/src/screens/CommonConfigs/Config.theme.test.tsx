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

import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ContextType } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../store/context";
import Config from "./Config";
import { ConfigPropertiesType } from "./types";

const mocks = vi.hoisted(() => ({
  testConnectionProps: vi.fn(),
}));

vi.mock("../../hooks/useAuth", () => ({
  useAuth: () => ({
    havePermissions: () => true,
    hasAuthorization: () => true,
  }),
}));
vi.mock("../../hooks/useEnhancedConfigs", () => ({
  default: () => ({
    onValueUpdate: vi.fn(),
    processingConfig: false,
    recommendedChanges: {},
    setRecommendedChanges: vi.fn(),
  }),
}));
vi.mock("../../components/Tooltip", () => ({
  default: ({ children }: { children: unknown }) => children,
}));
vi.mock("../../components/Modal", () => ({
  default: () => null,
}));
vi.mock("./ChooseConfigGroup", () => ({
  default: () => null,
}));
vi.mock("../ConfigGroups/ManageConfigGroups", () => ({
  default: () => null,
}));
vi.mock("./AdvancedConfigs", () => ({
  default: () => <div>Advanced configuration fallback</div>,
}));
vi.mock("./TestConnection", () => ({
  default: (props: Record<string, unknown>) => {
    mocks.testConnectionProps(props);
    return (
      <button disabled={Boolean(props.disabled)}>
        {String(props.buttonLabel)}
      </button>
    );
  },
}));

const configProperty = (
  propertyName: string,
  value: unknown,
  propertyAttributes: Record<string, unknown> = { type: "string" },
) => ({
  propertyName,
  propertyDisplayname: propertyName,
  propertyValue: value,
  propertyAttributes,
  previousValue: value,
  value,
  final: "false",
  fileName: "site.xml",
  type: "site",
  isEditable: true,
  isVisible: true,
});

const configs = (): ConfigPropertiesType => ({
  SVC: {
    site: {
      errors: 0,
      properties: {
        mode: configProperty("mode", "show"),
        primary: configProperty("primary", "primary value"),
        secondary: configProperty("secondary", "secondary value"),
        interval: configProperty("interval", 90000000, {
          type: "int",
          unit: "milliseconds",
          minimum: 0,
          maximum: 172800000,
          increment_step: 1000,
        }),
      },
    },
  },
});

const theme = {
  items: [
    {
      StackServices: { service_name: "SVC" },
      themes: [
        {
          ThemeInfo: {
            file_name: "theme.json",
            service_name: "SVC",
            theme_data: {
              Theme: {
                name: "default",
                configuration: {
                  layouts: [
                    {
                      name: "default",
                      tabs: [
                        {
                          name: "general",
                          "display-name": "General",
                          layout: {
                            "tab-columns": "3",
                            "tab-rows": "4",
                            sections: [
                              {
                                name: "main-section",
                                "display-name": "Main section",
                                "row-index": "1",
                                "column-index": "1",
                                "row-span": "2",
                                "column-span": "2",
                                "section-columns": "2",
                                "section-rows": "2",
                                subsections: [
                                  {
                                    name: "tabbed-subsection",
                                    "display-name": "Tabbed settings",
                                    "row-index": "0",
                                    "column-index": "1",
                                    "row-span": "2",
                                    "column-span": "1",
                                    border: true,
                                    "left-vertical-splitter": true,
                                    "subsection-tabs": [
                                      {
                                        name: "primary-tab",
                                        "display-name": "Primary group",
                                      },
                                      {
                                        name: "secondary-tab",
                                        "display-name": "Secondary group",
                                      },
                                      {
                                        name: "hidden-tab",
                                        "display-name": "Hidden group",
                                        "depends-on": [
                                          {
                                            if: "${site/mode} === hidden",
                                            then: {
                                              property_value_attributes: {
                                                visible: true,
                                              },
                                            },
                                            else: {
                                              property_value_attributes: {
                                                visible: false,
                                              },
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
                  placement: {
                    "configuration-layout": "default",
                    configs: [
                      {
                        config: "site/primary",
                        "subsection-name": "tabbed-subsection",
                        "subsection-tab-name": "primary-tab",
                      },
                      {
                        config: "site/secondary",
                        "subsection-name": "tabbed-subsection",
                        "subsection-tab-name": "secondary-tab",
                      },
                      {
                        config: "site/interval",
                        "subsection-name": "tabbed-subsection",
                        "subsection-tab-name": "secondary-tab",
                      },
                      {
                        config: "site/test_connection",
                        "subsection-name": "tabbed-subsection",
                        "subsection-tab-name": "secondary-tab",
                        property_value_attributes: { ui_only_property: true },
                      },
                      {
                        config: "site/mode",
                        "subsection-name": "tabbed-subsection",
                        "subsection-tab-name": "hidden-tab",
                      },
                    ],
                  },
                  widgets: [
                    {
                      config: "site/primary",
                      widget: { type: "text-field" },
                    },
                    {
                      config: "site/secondary",
                      widget: { type: "text-field" },
                    },
                    {
                      config: "site/interval",
                      widget: {
                        type: "time-interval-spinner",
                        units: [{ "unit-name": "hours,minutes" }],
                      },
                    },
                    {
                      config: "site/test_connection",
                      widget: {
                        type: "test-db-connection",
                        "display-name": "Verify custom database",
                        "required-properties": {
                          "db.connection.user": "site/primary",
                          "db.connection.password": "site/secondary",
                        },
                      },
                    },
                    {
                      config: "site/mode",
                      widget: { type: "text-field" },
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
};

function configElement(
  themeData: unknown = theme,
  configProperties: ConfigPropertiesType = configs(),
  configProps: Record<string, unknown> = {},
) {
  return (
    <AppContext.Provider
      value={{
        clusterName: "cluster1",
        allHostNames: ["host1"],
      } as ContextType<typeof AppContext>}
    >
      <Config
        configSection="default"
        themeData={themeData}
        configPropertiesData={{ items: [] }}
        servicesList={["SVC"]}
        configProperties={configProperties}
        setConfigProperties={vi.fn()}
        installedServices={["SVC"]}
        {...configProps}
      />
    </AppContext.Provider>
  );
}

function renderConfig(
  themeData: unknown = theme,
  configProperties: ConfigPropertiesType = configs(),
  configProps: Record<string, unknown> = {},
) {
  return render(configElement(themeData, configProperties, configProps));
}

const compactTheme = (
  placements: Array<Record<string, unknown>>,
  widgets: Array<Record<string, unknown>>,
) => ({
  items: [
    {
      StackServices: { service_name: "SVC" },
      themes: [
        {
          ThemeInfo: {
            service_name: "SVC",
            theme_data: {
              Theme: {
                name: "default",
                configuration: {
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
                                subsections: [{ name: "subsection" }],
                              },
                            ],
                          },
                        },
                      ],
                    },
                  ],
                  placement: { configs: placements },
                  widgets,
                },
              },
            },
          },
        },
      ],
    },
  ],
});

type MutableThemeConfiguration = {
  layouts: Array<{ tabs: Array<Record<string, unknown>> }>;
  placement: { configs: Array<Record<string, unknown>> };
  widgets: Array<Record<string, unknown>>;
};

type MutableThemeResponse = {
  items: Array<{
    themes: Array<{
      ThemeInfo: {
        theme_data: { Theme: { configuration: MutableThemeConfiguration } };
      };
    }>;
  }>;
};

const mutableTheme = () =>
  structuredClone(theme) as unknown as MutableThemeResponse;

const topTabTheme = () => {
  const response = mutableTheme();
  const configuration =
    response.items[0].themes[0].ThemeInfo.theme_data.Theme.configuration;
  configuration.layouts[0].tabs = [
    {
      name: "first",
      "display-name": "First settings",
      layout: {
        sections: [
          {
            name: "first-section",
            subsections: [{ name: "first-subsection" }],
          },
        ],
      },
    },
    {
      name: "second",
      "display-name": "Second settings",
      layout: {
        sections: [
          {
            name: "second-section",
            subsections: [{ name: "second-subsection" }],
          },
        ],
      },
    },
  ];
  configuration.placement.configs = [
    { config: "site/primary", "subsection-name": "first-subsection" },
    { config: "site/secondary", "subsection-name": "second-subsection" },
  ];
  configuration.widgets = [
    { config: "site/primary", widget: { type: "text-field" } },
    { config: "site/secondary", widget: { type: "text-field" } },
  ];
  return response;
};

const geometryTheme = () => {
  const response = mutableTheme();
  const configuration =
    response.items[0].themes[0].ThemeInfo.theme_data.Theme.configuration;
  configuration.layouts[0].tabs = [
    {
      name: "geometry",
      "display-name": "Geometry",
      layout: {
        "tab-columns": "3",
        "tab-rows": "3",
        sections: [
          {
            name: "late-section",
            "row-index": "1",
            "column-index": "1",
            "row-span": "2",
            "column-span": "2",
            "section-columns": "2",
            "section-rows": "2",
            subsections: [
              {
                name: "no-title",
                "row-index": "1",
                "column-index": "1",
              },
              {
                name: "with-title",
                "display-name": "Named subsection",
                "row-index": "1",
                "column-index": "0",
              },
            ],
          },
          {
            name: "early-section",
            "row-index": "0",
            "column-index": "0",
            subsections: [{ name: "early-subsection" }],
          },
        ],
      },
    },
  ];
  configuration.placement.configs = [
    { config: "site/secondary", "subsection-name": "no-title" },
    { config: "site/mode", "subsection-name": "with-title" },
    { config: "site/primary", "subsection-name": "early-subsection" },
  ];
  configuration.widgets = [
    { config: "site/secondary", widget: { type: "text-field" } },
    { config: "site/mode", widget: { type: "text-field" } },
    { config: "site/primary", widget: { type: "text-field" } },
  ];
  return response;
};

describe("Ember Service Theme page integration", () => {
  beforeEach(() => {
    mocks.testConnectionProps.mockClear();
  });

  afterEach(cleanup);

  it("renders every section from a non-default named installed-service Theme", async () => {
    type MutableThemeBody = {
      name: string;
      configuration: {
        layouts: unknown[];
        placement: { configs: unknown[] };
        widgets: unknown[];
      };
    };
    const directoriesTheme = structuredClone(theme) as unknown as {
      items: Array<{
        themes: Array<{
          ThemeInfo: { theme_data: { Theme: MutableThemeBody } };
        }>;
      }>;
    };
    const themeBody =
      directoriesTheme.items[0].themes[0].ThemeInfo.theme_data.Theme;
    themeBody.name = "directories";
    themeBody.configuration.layouts = [
      {
        name: "directories",
        tabs: [
          {
            name: "directories",
            "display-name": "Directories",
            layout: {
              "tab-columns": "1",
              "tab-rows": "3",
              sections: [
                {
                  name: "data-section",
                  "display-name": "DATA DIRS",
                  subsections: [{ name: "data-subsection" }],
                },
                {
                  name: "log-section",
                  "display-name": "LOG DIRS",
                  subsections: [{ name: "log-subsection" }],
                },
                {
                  name: "pid-section",
                  "display-name": "PID DIRS",
                  subsections: [{ name: "pid-subsection" }],
                },
              ],
            },
          },
        ],
      },
    ];
    themeBody.configuration.placement.configs = [
      { config: "site/primary", "subsection-name": "data-subsection" },
      { config: "site/secondary", "subsection-name": "log-subsection" },
      { config: "site/mode", "subsection-name": "pid-subsection" },
    ];
    themeBody.configuration.widgets = [
      { config: "site/primary", widget: { type: "text-field" } },
      { config: "site/secondary", widget: { type: "text-field" } },
      { config: "site/mode", widget: { type: "text-field" } },
    ];

    renderConfig(directoriesTheme, configs(), { allThemes: true });

    expect(await screen.findByRole("tab", { name: "Directories" })).toBeTruthy();
    expect(screen.getByText("DATA DIRS")).toBeTruthy();
    expect(screen.getByText("LOG DIRS")).toBeTruthy();
    expect(screen.getByText("PID DIRS")).toBeTruthy();
    expect(screen.getByDisplayValue("primary value")).toBeTruthy();
    expect(screen.getByDisplayValue("secondary value")).toBeTruthy();
    expect(screen.getByDisplayValue("show")).toBeTruthy();
  });

  it("selects the first visible top tab, rejects disabled tabs, and renders on demand", async () => {
    const source = configs();
    source.SVC.site.properties.primary.isVisible = false;
    source.SVC.site.properties.primary.isHidden = true;
    renderConfig(topTabTheme(), source);

    expect(await screen.findByDisplayValue("secondary value")).toBeTruthy();
    expect(screen.queryByTestId("theme-grid-SVC-first")).toBeNull();
    expect(screen.getByTestId("theme-grid-SVC-second")).toBeTruthy();

    const firstTab = screen.getByText("First settings").closest(".nav-link");
    expect(firstTab?.getAttribute("aria-disabled")).toBe("true");
    fireEvent.click(screen.getByText("First settings"));
    expect(screen.queryByDisplayValue("primary value")).toBeNull();
    expect(screen.getByDisplayValue("secondary value")).toBeTruthy();

    fireEvent.click(screen.getByText("Advanced"));
    expect(await screen.findByText("Advanced configuration fallback")).toBeTruthy();
  });

  it("operates visible top-level tabs with the keyboard", async () => {
    renderConfig(topTabTheme());
    const firstTab = await screen.findByRole("tab", {
      name: /First settings/,
    });
    const secondTab = screen.getByRole("tab", { name: /Second settings/ });

    firstTab.focus();
    fireEvent.keyDown(firstTab, { key: "ArrowRight" });

    expect(document.activeElement).toBe(secondTab);
    expect(secondTab.getAttribute("aria-selected")).toBe("true");
    expect(await screen.findByDisplayValue("secondary value")).toBeTruthy();

    fireEvent.keyDown(secondTab, { key: "Home" });
    expect(document.activeElement).toBe(firstTab);
    expect(await screen.findByDisplayValue("primary value")).toBeTruthy();
  });

  it("hands the active top tab to a visible sibling and reports an all-empty Theme", async () => {
    const view = renderConfig(topTabTheme());
    expect(await screen.findByDisplayValue("primary value")).toBeTruthy();

    const secondOnly = configs();
    secondOnly.SVC.site.properties.primary.isVisible = false;
    secondOnly.SVC.site.properties.primary.isHidden = true;
    view.rerender(configElement(topTabTheme(), secondOnly));
    expect(await screen.findByDisplayValue("secondary value")).toBeTruthy();
    expect(screen.queryByDisplayValue("primary value")).toBeNull();

    const empty = configs();
    Object.values(empty.SVC.site.properties).forEach((property) => {
      property.isVisible = false;
      property.isHidden = true;
    });
    view.rerender(configElement(topTabTheme(), empty));
    expect((await screen.findByTestId("theme-no-content")).textContent).toContain(
      "No configuration properties are available.",
    );
  });

  it("renders Theme grid geometry, borders, splitters, and filtered subsection tabs", async () => {
    const { container } = renderConfig();
    const grid = await screen.findByTestId("theme-grid-SVC-general");
    expect(grid.style.gridTemplateColumns).toBe("repeat(3, minmax(0, 1fr))");
    expect(grid.style.gridTemplateRows).toBe(
      "repeat(4, minmax(min-content, auto))",
    );

    const section = container.querySelector(
      '[data-theme-section="main-section"]',
    ) as HTMLElement;
    expect(section.style.gridColumn).toBe("2 / span 2");
    expect(section.style.gridRow).toBe("2 / span 2");

    const subsection = container.querySelector(
      '[data-theme-subsection="tabbed-subsection"]',
    ) as HTMLElement;
    expect(subsection.classList.contains("service-theme-subsection-bordered")).toBe(
      true,
    );
    expect(subsection.classList.contains("service-theme-subsection-split")).toBe(
      true,
    );
    expect(subsection.style.gridColumn).toBe("2 / span 1");
    expect(screen.queryByRole("tab", { name: "Hidden group" })).toBeNull();
  });

  it("preserves empty grid cells, simultaneous spans, title gaps, and semantic focus order", async () => {
    const { container } = renderConfig(geometryTheme());
    await screen.findByTestId("theme-grid-SVC-geometry");
    const sections = Array.from(
      container.querySelectorAll<HTMLElement>("[data-theme-section]"),
    );
    expect(sections.map((section) => section.dataset.themeSection)).toEqual([
      "late-section",
      "early-section",
    ]);
    expect(sections[0].style.gridColumn).toBe("2 / span 2");
    expect(sections[0].style.gridRow).toBe("2 / span 2");
    expect(sections[1].style.gridColumn).toBe("1 / span 1");
    expect(sections[1].style.gridRow).toBe("1 / span 1");

    const subsections = Array.from(
      sections[0].querySelectorAll<HTMLElement>("[data-theme-subsection]"),
    );
    expect(
      subsections.map((subsection) => subsection.dataset.themeSubsection),
    ).toEqual(["no-title", "with-title"]);
    subsections.forEach((subsection) => {
      expect(
        subsection.classList.contains("service-theme-subsection-top-split"),
      ).toBe(true);
    });
    expect(
      subsections[0].querySelector(".service-theme-subsection-title")
        ?.textContent,
    ).toBe("\u00a0");
    expect(screen.getByText("Named subsection")).toBeTruthy();

    expect(
      Array.from(container.querySelectorAll<HTMLInputElement>("input")).map(
        (input) => input.value,
      ),
    ).toEqual(["secondary value", "show", "primary value"]);
  });

  it("switches subsection tabs and preserves Theme spinner units", async () => {
    renderConfig();
    expect(await screen.findByDisplayValue("primary value")).toBeTruthy();
    expect(screen.queryByDisplayValue("secondary value")).toBeNull();

    fireEvent.click(screen.getByRole("tab", { name: "Secondary group" }));
    expect(await screen.findByDisplayValue("secondary value")).toBeTruthy();
    expect(screen.queryByDisplayValue("primary value")).toBeNull();
    expect(screen.getByRole("spinbutton", { name: "Hours" })).toBeTruthy();
    expect(screen.getByRole("spinbutton", { name: "Minutes" })).toBeTruthy();
    expect(screen.queryByRole("spinbutton", { name: "Days" })).toBeNull();
    expect(screen.queryByRole("spinbutton", { name: "Seconds" })).toBeNull();
  });

  it("operates visible subsection tabs with the keyboard", async () => {
    renderConfig();
    const primaryTab = await screen.findByRole("tab", {
      name: /Primary group/,
    });
    const secondaryTab = screen.getByRole("tab", {
      name: /Secondary group/,
    });

    primaryTab.focus();
    fireEvent.keyDown(primaryTab, { key: "End" });

    expect(document.activeElement).toBe(secondaryTab);
    expect(secondaryTab.getAttribute("aria-selected")).toBe("true");
    expect(await screen.findByDisplayValue("secondary value")).toBeTruthy();
  });

  it("hands off from a nested tab when its condition hides it", async () => {
    const visibleHiddenTab = configs();
    visibleHiddenTab.SVC.site.properties.mode.value = "hidden";
    const view = renderConfig(theme, visibleHiddenTab);
    const hiddenTab = await screen.findByRole("tab", {
      name: "Hidden group",
    });

    fireEvent.click(hiddenTab);
    expect(await screen.findByDisplayValue("hidden")).toBeTruthy();
    expect(screen.queryByDisplayValue("primary value")).toBeNull();

    const hiddenAgain = configs();
    hiddenAgain.SVC.site.properties.mode.value = "show";
    view.rerender(configElement(theme, hiddenAgain));

    await waitFor(() =>
      expect(screen.queryByRole("tab", { name: "Hidden group" })).toBeNull(),
    );
    expect(await screen.findByDisplayValue("primary value")).toBeTruthy();
    expect(screen.queryByDisplayValue("secondary value")).toBeNull();
  });

  it("passes Theme display name and required-properties to the UI-only connection widget", async () => {
    renderConfig();
    fireEvent.click(await screen.findByRole("tab", { name: "Secondary group" }));
    expect(
      await screen.findByRole("button", { name: "Verify custom database" }),
    ).toBeTruthy();
    await waitFor(() => expect(mocks.testConnectionProps).toHaveBeenCalled());
    expect(mocks.testConnectionProps.mock.lastCall?.[0]).toMatchObject({
      buttonLabel: "Verify custom database",
      serviceName: "SVC",
      requiredProperties: {
        "db.connection.user": "site/primary",
        "db.connection.password": "site/secondary",
      },
    });
  });

  it("disables UI-only Theme actions in the read-only Host consumer", async () => {
    renderConfig(theme, configs(), { hostConfigs: true });
    fireEvent.click(await screen.findByRole("tab", { name: "Secondary group" }));
    const button = await screen.findByRole("button", {
      name: "Verify custom database",
    });

    expect((button as HTMLButtonElement).disabled).toBe(true);
  });

  it("applies Theme read-only and non-overridable state to every mutation control", async () => {
    const readOnlyConfigs = configs();
    const primary = readOnlyConfigs.SVC.site.properties.primary;
    primary.value = "changed value";
    primary.previousValue = "primary value";
    primary.supportsFinal = true;
    primary.overrideValues = [
      {
        value: "group override",
        previousValue: "group override",
        groupName: "Blue",
      },
    ];
    const readOnlyTheme = compactTheme(
      [
        {
          config: "site/primary",
          "subsection-name": "subsection",
          property_value_attributes: {
            read_only: true,
            overridable: false,
          },
        },
      ],
      [{ config: "site/primary", widget: { type: "text-field" } }],
    );

    const { container } = renderConfig(readOnlyTheme, readOnlyConfigs, {
      configGroup: "Blue",
      displayUndoRedo: true,
    });

    const inputs = [
      await screen.findByDisplayValue("changed value"),
      screen.getByDisplayValue("group override"),
    ];
    inputs.forEach((input) =>
      expect((input as HTMLInputElement).disabled).toBe(true),
    );
    expect(container.querySelector('[data-icon="lock"]')).toBeNull();
    expect(container.querySelector('[data-icon="circle-plus"]')).toBeNull();
    expect(container.querySelector('[data-icon="arrow-rotate-left"]')).toBeNull();
    expect(container.querySelector('[data-icon="arrow-rotate-right"]')).toBeNull();
    expect(container.querySelector('[data-icon="circle-minus"]')).toBeNull();
  });

  it("falls back to Advanced when Theme data is malformed", async () => {
    renderConfig({ items: [{ StackServices: { service_name: "SVC" }, themes: [{}] }] });
    expect(await screen.findByText("Advanced configuration fallback")).toBeTruthy();
  });

  it("isolates widget mode state by full config path", async () => {
    const sameNameConfigs: ConfigPropertiesType = {
      SVC: {
        "type-a": {
          errors: 0,
          properties: {
            shared: {
              ...configProperty("shared", "one", {
                type: "string",
                entries: ["one", "two"],
              }),
              fileName: "type-a.xml",
              type: "type-a",
            },
          },
        },
        "type-b": {
          errors: 0,
          properties: {
            shared: {
              ...configProperty("shared", "two", {
                type: "string",
                entries: ["one", "two"],
              }),
              fileName: "type-b.xml",
              type: "type-b",
            },
          },
        },
      },
    };
    const themeData = compactTheme(
      ["type-a/shared", "type-b/shared"].map((config) => ({
        config,
        "subsection-name": "subsection",
      })),
      ["type-a/shared", "type-b/shared"].map((config) => ({
        config,
        widget: { type: "combo" },
      })),
    );
    const { container } = renderConfig(themeData, sameNameConfigs);
    await waitFor(() =>
      expect(
        container.querySelector('[data-theme-widget-config="type-a/shared"]'),
      ).not.toBeNull(),
    );
    const first = container.querySelector(
      '[data-theme-widget-config="type-a/shared"]',
    ) as HTMLElement;
    const second = container.querySelector(
      '[data-theme-widget-config="type-b/shared"]',
    ) as HTMLElement;
    expect(first.querySelector(".input-group")).toBeNull();
    expect(second.querySelector(".input-group")).toBeNull();

    fireEvent.click(first.querySelector('[data-icon="pen"]') as Element);
    expect(first.querySelector(".input-group")).not.toBeNull();
    expect(second.querySelector(".input-group")).toBeNull();
  });

  it("reports an unknown widget instead of creating an editable text field", async () => {
    const propertyConfigs: ConfigPropertiesType = {
      SVC: {
        site: {
          errors: 0,
          properties: {
            primary: configProperty("primary", "do not edit"),
          },
        },
      },
    };
    renderConfig(
      compactTheme(
        [
          {
            config: "site/primary",
            "subsection-name": "subsection",
          },
        ],
        [{ config: "site/primary", widget: { type: "future-widget" } }],
      ),
      propertyConfigs,
    );

    expect((await screen.findByRole("status")).textContent).toContain(
      "Unsupported Theme widget type: future-widget",
    );
    expect(screen.queryByDisplayValue("do not edit")).toBeNull();
  });

  it("renders unsupported checkbox and toggle values without replacing them", async () => {
    const propertyConfigs: ConfigPropertiesType = {
      SVC: {
        site: {
          errors: 0,
          properties: {
            checkbox: configProperty("checkbox", "custom-checkbox"),
            toggle: configProperty("toggle", "custom-toggle", {
              type: "string",
              entries: [
                { value: "enabled", label: "Enabled" },
                { value: "disabled", label: "Disabled" },
              ],
            }),
          },
        },
      },
    };
    renderConfig(
      compactTheme(
        ["checkbox", "toggle"].map((name) => ({
          config: `site/${name}`,
          "subsection-name": "subsection",
        })),
        [
          { config: "site/checkbox", widget: { type: "checkbox" } },
          { config: "site/toggle", widget: { type: "toggle" } },
        ],
      ),
      propertyConfigs,
    );

    expect(await screen.findByDisplayValue("custom-checkbox")).toBeTruthy();
    expect(screen.getByDisplayValue("custom-toggle")).toBeTruthy();
    expect(screen.queryByRole("checkbox")).toBeNull();
  });
});
