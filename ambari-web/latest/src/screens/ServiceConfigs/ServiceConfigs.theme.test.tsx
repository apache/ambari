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
import type { ComponentProps, ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { AppContext } from "../../store/context";
import { ServiceContext } from "../../store/ServiceContext";
import type { ConfigPropertiesType } from "../CommonConfigs/types";

const mocks = vi.hoisted(() => ({
  getConfigValues: vi.fn(),
  getServiceConfigurations: vi.fn(),
  getTheme: vi.fn(),
  getVersionConfigValues: vi.fn(),
  hasAuthorization: vi.fn(),
  saveStepConfigs: vi.fn(),
  validateConfigProperties: vi.fn(),
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual<typeof import("react-router-dom")>(
    "react-router-dom"
  );
  return {
    ...actual,
    useBlocker: () => ({ state: "unblocked", proceed: vi.fn() }),
  };
});

vi.mock("../../api/configsApi", () => ({
  default: {
    getConfigValues: mocks.getConfigValues,
    getServiceConfigurations: mocks.getServiceConfigurations,
    getTheme: mocks.getTheme,
    getVersionConfigValues: mocks.getVersionConfigValues,
  },
}));

vi.mock("../../hooks/useAuth", () => ({
  useAuth: () => ({ hasAuthorization: mocks.hasAuthorization }),
}));

vi.mock("../../components/AuthGuard", () => ({
  AuthGuard: ({ children }: { children: ReactNode }) => children,
}));

vi.mock("../../hooks/useConfigSaver", () => ({
  useConfigSaver: () => ({ saveStepConfigs: mocks.saveStepConfigs }),
}));

vi.mock("../../hooks/useEnhancedConfigs", () => ({
  default: () => ({
    loadRecommendationsForConfigOnLoad: () => undefined,
  }),
}));

vi.mock("../ClusterWizard/hooks/useHostComponents", () => ({
  default: () => ({ hostComponents: [] }),
}));

vi.mock("../../hooks/useServerValidation", () => ({
  default: () => ({
    validationErrors: [],
    validateConfigProperties: mocks.validateConfigProperties,
  }),
}));

vi.mock("../ConfigVersions/VersionsList", () => ({
  default: ({ onVersionChange }: { onVersionChange: (version: string) => void }) => (
    <div>
      <div>Versions</div>
      <button onClick={() => onVersionChange("1")}>Load version 1</button>
      <button onClick={() => onVersionChange("2")}>Load version 2</button>
    </div>
  ),
}));

vi.mock("../CommonConfigs/ChooseConfigGroup", () => ({
  default: () => <div>Default group</div>,
}));

vi.mock("../ConfigGroups/AddToConfigGroupModal", () => ({
  default: () => null,
}));

vi.mock("../ConfigGroups/ManageConfigGroups", () => ({
  default: () => null,
}));

vi.mock("../../components/Modal", () => ({
  default: () => null,
}));

vi.mock("../ConfigVersions/ConfigsComparator", () => ({
  default: () => null,
}));

vi.mock("../../components/Table", () => ({
  default: () => null,
}));

vi.mock("../CommonConfigs/Config", () => ({
  default: ({
    configProperties,
    servicesList,
    setConfigProperties,
    themeData,
  }: {
    configProperties: ConfigPropertiesType;
    servicesList: string[];
    setConfigProperties: (configs: ConfigPropertiesType) => void;
    themeData: { items?: unknown[] };
  }) => {
    const serviceName = servicesList[0];
    const sections = configProperties[serviceName] || {};
    const locatedProperty = Object.entries(sections).flatMap(
      ([sectionName, section]) =>
        Object.values(section.properties || {}).map((property) => ({
          property,
          section,
          sectionName,
        }))
    )[0];
    const property = locatedProperty?.property;
    const layoutName = property?.tabName
      ? `Theme ${property.tabName}`
      : locatedProperty?.section.displayName || locatedProperty?.sectionName;

    return (
      <div data-testid="config-probe">
        <div>{layoutName}</div>
        <div data-testid="property-value">{String(property?.value || "")}</div>
        <div data-testid="theme-count">{themeData?.items?.length || 0}</div>
        <button
          onClick={() => {
            const updated = structuredClone(configProperties);
            const updatedSections = updated[serviceName] || {};
            const updatedProperty = Object.values(updatedSections).flatMap(
              (section) => Object.values(section.properties || {})
            )[0];
            if (updatedProperty) {
              updatedProperty.value = "edited-during-fallback";
            }
            setConfigProperties(updated);
          }}
        >
          Edit property
        </button>
      </div>
    );
  },
}));

import ServiceConfigs from "./index";

const services = [
  { ServiceInfo: { service_name: "HDFS" } },
  { ServiceInfo: { service_name: "YARN" } },
];

const stackConfigurations = {
  items: [
    {
      StackServices: { service_name: "HDFS" },
      configurations: [
        {
          StackConfigurations: {
            property_name: "test.property",
            property_type: [],
            property_value: "stack-default",
            property_value_attributes: { type: "text", visible: true },
            service_name: "HDFS",
            type: "core-site.xml",
          },
          dependencies: [],
        },
      ],
    },
  ],
};

const propertyValues = {
  items: [
    {
      configurations: [
        {
          properties: { "test.property": "active-value" },
          properties_attributes: {},
          type: "core-site",
        },
      ],
      group_name: "Default",
      service_config_version: 7,
      service_name: "HDFS",
    },
  ],
};

const themeResponse = {
  items: [
    {
      StackServices: { service_name: "HDFS" },
      themes: [
        {
          ThemeInfo: {
            file_name: "theme.json",
            service_name: "HDFS",
            theme_data: {
              Theme: {
                name: "default",
                configuration: {
                  placement: {
                    "configuration-layout": "default",
                    configs: [
                      {
                        config: "core-site/test.property",
                        "subsection-name": "main",
                      },
                    ],
                  },
                  widgets: [
                    {
                      config: "core-site/test.property",
                      widget: { type: "text-field" },
                    },
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
                                name: "general",
                                subsections: [{ name: "main" }],
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
};

const fixtureForService = <T,>(fixture: T, serviceName: string): T =>
  JSON.parse(JSON.stringify(fixture).replaceAll('"HDFS"', `"${serviceName}"`));

function serviceConfigsElement(serviceName = "hdfs") {
  const appContextValue = {
    clusterName: "cluster1",
    cluster: {
      cluster_id: 1,
      stack: "HDP",
      versionNum: "3.1",
    },
    services,
  } as unknown as ComponentProps<typeof AppContext.Provider>["value"];
  const serviceContextValue = {
    allServiceModels: {
      hdfs: {
        isClientOnlyService: true,
        masterComponents: [],
        slaveComponents: [],
      },
      yarn: {
        isClientOnlyService: true,
        masterComponents: [],
        slaveComponents: [],
      },
    },
  } as unknown as ComponentProps<typeof ServiceContext.Provider>["value"];

  return (
    <MemoryRouter>
      <AppContext.Provider value={appContextValue}>
        <ServiceContext.Provider value={serviceContextValue}>
          <ServiceConfigs serviceName={serviceName} />
        </ServiceContext.Provider>
      </AppContext.Provider>
    </MemoryRouter>
  );
}

function renderServiceConfigs(serviceName = "hdfs") {
  return render(serviceConfigsElement(serviceName));
}

describe("Service Configs Theme loading", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(console, "error").mockImplementation(() => undefined);
    mocks.getConfigValues.mockResolvedValue(propertyValues);
    mocks.getServiceConfigurations.mockResolvedValue(stackConfigurations);
    mocks.getTheme.mockResolvedValue(themeResponse);
    mocks.hasAuthorization.mockReturnValue(true);
    mocks.saveStepConfigs.mockResolvedValue(true);
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("requests the Theme only for the selected service", async () => {
    renderServiceConfigs();

    expect(await screen.findByText("Theme settings")).toBeTruthy();
    expect(screen.getByTestId("property-value").textContent).toBe(
      "active-value"
    );
    expect(mocks.getTheme).toHaveBeenCalledTimes(1);
    expect(mocks.getTheme).toHaveBeenCalledWith("HDP", "3.1", "HDFS");
    expect(mocks.getServiceConfigurations).toHaveBeenCalledWith(
      "HDP",
      "3.1",
      "HDFS,YARN"
    );
  });

  it("renders Advanced configs after Theme failure and recovers without losing edits", async () => {
    mocks.getTheme
      .mockRejectedValueOnce(new Error("Theme unavailable"))
      .mockResolvedValueOnce(themeResponse);
    renderServiceConfigs();

    expect(await screen.findByText("Advanced core-site")).toBeTruthy();
    expect(screen.getByTestId("property-value").textContent).toBe(
      "active-value"
    );
    expect(
      screen.getByText(/Theme layout for HDFS could not be loaded/)
    ).toBeTruthy();
    expect(screen.getByText("Theme unavailable")).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: "Edit property" }));
    expect(screen.getByTestId("property-value").textContent).toBe(
      "edited-during-fallback"
    );
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));

    await waitFor(() => expect(mocks.getTheme).toHaveBeenCalledTimes(2));
    expect(await screen.findByText("Theme settings")).toBeTruthy();
    expect(screen.queryByText(/Theme layout for HDFS could not be loaded/)).toBeNull();
    expect(screen.getByTestId("theme-count").textContent).toBe("1");
    expect(screen.getByTestId("property-value").textContent).toBe(
      "edited-during-fallback"
    );
  });

  it("ignores an older configuration version response that finishes last", async () => {
    let resolveVersionOne: (value: typeof propertyValues) => void = () => undefined;
    let resolveVersionTwo: (value: typeof propertyValues) => void = () => undefined;
    const versionOne = new Promise<typeof propertyValues>((resolve) => {
      resolveVersionOne = resolve;
    });
    const versionTwo = new Promise<typeof propertyValues>((resolve) => {
      resolveVersionTwo = resolve;
    });
    mocks.getVersionConfigValues
      .mockReturnValueOnce(versionOne)
      .mockReturnValueOnce(versionTwo);
    renderServiceConfigs();

    expect(await screen.findByText("Theme settings")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Load version 1" }));
    fireEvent.click(screen.getByRole("button", { name: "Load version 2" }));
    await waitFor(() =>
      expect(mocks.getVersionConfigValues).toHaveBeenCalledTimes(2),
    );

    const versionTwoValues = structuredClone(propertyValues);
    versionTwoValues.items[0].configurations[0].properties["test.property"] =
      "version-two";
    await act(async () => resolveVersionTwo(versionTwoValues));
    await waitFor(() =>
      expect(screen.getByTestId("property-value").textContent).toBe(
        "version-two",
      ),
    );

    const versionOneValues = structuredClone(propertyValues);
    versionOneValues.items[0].configurations[0].properties["test.property"] =
      "version-one";
    await act(async () => resolveVersionOne(versionOneValues));
    expect(screen.getByTestId("property-value").textContent).toBe(
      "version-two",
    );
  });

  it("ignores a stale Theme response after switching services", async () => {
    const yarnStackConfigurations = fixtureForService(
      stackConfigurations,
      "YARN",
    );
    const yarnPropertyValues = fixtureForService(propertyValues, "YARN");
    yarnPropertyValues.items[0].configurations[0].properties[
      "test.property"
    ] = "yarn-value";
    const yarnThemeResponse = fixtureForService(themeResponse, "YARN");
    yarnThemeResponse.items[0].themes[0].ThemeInfo.theme_data.Theme.configuration.layouts[0].tabs[0].name =
      "yarn-settings";
    const combinedConfigurations = {
      items: [
        ...stackConfigurations.items,
        ...yarnStackConfigurations.items,
      ],
    };
    const combinedValues = {
      items: [...propertyValues.items, ...yarnPropertyValues.items],
    };
    let resolveHdfsTheme: (value: typeof themeResponse) => void = () =>
      undefined;
    let resolveHdfsConfigurations: (value: typeof stackConfigurations) => void =
      () => undefined;
    let resolveHdfsValues: (value: typeof propertyValues) => void = () =>
      undefined;
    const delayedHdfsTheme = new Promise<typeof themeResponse>((resolve) => {
      resolveHdfsTheme = resolve;
    });
    const delayedHdfsConfigurations = new Promise<typeof stackConfigurations>(
      (resolve) => {
        resolveHdfsConfigurations = resolve;
      },
    );
    const delayedHdfsValues = new Promise<typeof propertyValues>((resolve) => {
      resolveHdfsValues = resolve;
    });
    mocks.getServiceConfigurations
      .mockReset()
      .mockReturnValueOnce(delayedHdfsConfigurations)
      .mockResolvedValue(combinedConfigurations);
    mocks.getConfigValues
      .mockReset()
      .mockReturnValueOnce(delayedHdfsValues)
      .mockResolvedValue(combinedValues);
    mocks.getTheme.mockImplementation(
      (_stack: string, _version: string, requestedService: string) =>
        requestedService === "HDFS"
          ? delayedHdfsTheme
          : Promise.resolve(yarnThemeResponse),
    );

    const view = renderServiceConfigs("hdfs");
    await waitFor(() =>
      expect(mocks.getTheme).toHaveBeenCalledWith("HDP", "3.1", "HDFS"),
    );
    await waitFor(() =>
      expect(mocks.getServiceConfigurations).toHaveBeenCalledTimes(1),
    );
    await waitFor(() => expect(mocks.getConfigValues).toHaveBeenCalledTimes(1));
    view.rerender(serviceConfigsElement("yarn"));

    expect(await screen.findByText("Theme yarn-settings")).toBeTruthy();
    expect(screen.getByTestId("property-value").textContent).toBe("yarn-value");
    await act(async () => {
      resolveHdfsTheme(themeResponse);
      resolveHdfsConfigurations(stackConfigurations);
      resolveHdfsValues(propertyValues);
      await Promise.all([
        delayedHdfsTheme,
        delayedHdfsConfigurations,
        delayedHdfsValues,
      ]);
    });

    await waitFor(() =>
      expect(screen.getByText("Theme yarn-settings")).toBeTruthy(),
    );
    expect(screen.queryByText("Theme settings")).toBeNull();
    expect(screen.getByTestId("property-value").textContent).toBe("yarn-value");
  });
});
