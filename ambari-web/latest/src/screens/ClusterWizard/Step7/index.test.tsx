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

import { createContext, type ContextType, type ReactNode } from "react";
import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../../store/context";
import { ServiceContext } from "../../../store/ServiceContext";
import { ContextWrapper } from "..";
import "../../../i18n";

type TestConfigProperty = {
  propertyName: string;
  value?: unknown;
  [key: string]: unknown;
};

type TestConfigCategory = {
  properties?: Record<string, TestConfigProperty>;
};

type TestConfigProperties = Record<
  string,
  Record<string, TestConfigCategory>
>;

type ConfigurationPayload = {
  data?: { configProperties?: TestConfigProperties };
};

const mocks = vi.hoisted(() => ({
  dispatch: vi.fn(),
  flushStateToDb: vi.fn(),
  getConfigValues: vi.fn(),
  getConfigsByTags: vi.fn(),
  getRecommendations: vi.fn(),
  getStackConfigurations: vi.fn(),
  getStackLevelConfigurations: vi.fn(),
  getStackThemes: vi.fn(),
  handleNextImperitive: vi.fn(),
  jumpToStep: vi.fn(),
  loadConfigTags: vi.fn(),
  loadAddServiceRecommendations: vi.fn(),
  processRecommendations: vi.fn(),
  recommendedChanges: {},
  setRecommendedChanges: vi.fn(),
  useEnhancedConfigsArgs: [] as unknown[],
  updateVisibilityForDependsOn: vi.fn((configProperties: object) =>
    configProperties
  ),
}));

vi.mock("../../../api/wizardApi", () => ({
  default: {
    getStackConfigurations: mocks.getStackConfigurations,
    getStackLevelConfigurations: mocks.getStackLevelConfigurations,
    getStackThemes: mocks.getStackThemes,
  },
}));

vi.mock("../../../api/configsApi", () => ({
  default: {
    getConfigValues: mocks.getConfigValues,
    getConfigsByTags: mocks.getConfigsByTags,
    getRecommendations: mocks.getRecommendations,
    loadConfigTags: mocks.loadConfigTags,
    validateConfigProperties: vi.fn(),
  },
}));

vi.mock("../../CommonConfigs/ConfigUtils", () => ({
  addTabNames: (configProperties: object) => configProperties,
  buildConfigsJSON: vi.fn(() => []),
  fetchComponentHostNamesByComponent: vi.fn(() => []),
  formatPropertyValue: (_property: object, value: unknown) => value,
  getConfigByName: vi.fn(),
  getConfigCategories: vi.fn(() => []),
  getConfigPropertyByName: vi.fn(),
  removeRangerConfigs: (configProperties: object) => configProperties,
  shouldSupportFinal: vi.fn(() => false),
  updateVisibilityByForeignKeys: (configProperties: object) => configProperties,
  updateVisibilityForDependsOn: mocks.updateVisibilityForDependsOn,
  validateAllProperties: (configProperties: object) => configProperties,
}));

vi.mock("../hooks/useServiceComponents", () => ({
  default: () => ({
    ComponentCategory: { CLIENT: "CLIENT", SLAVE: "SLAVE" },
    allServiceComponentsList: [],
    getClientComponents: () => [],
    hosts: { host1: {} },
    serviceComponents: [],
  }),
}));

vi.mock("../../../hooks/useEnhancedConfigs", () => ({
  default: (...args: unknown[]) => {
    mocks.useEnhancedConfigsArgs.push(args);
    return {
    loadAddServiceRecommendations: mocks.loadAddServiceRecommendations,
    processRecommendations: mocks.processRecommendations,
    recommendedChanges: mocks.recommendedChanges,
    setRecommendedChanges: mocks.setRecommendedChanges,
      recommendationsInProgress: false,
      processingConfig: false,
      recommendationError: null,
    };
  },
}));

vi.mock("../../../hooks/useDebounce", () => ({
  useDebounce: () => vi.fn(),
}));

vi.mock("../../../Initializers/WizardConfigInitializer", () => ({
  default: () => ({
    initialValue: (property: Record<string, unknown>) => ({
      ...property,
      initialValue: property.value,
    }),
  }),
}));

vi.mock("../utils", () => ({
  blueprintUtils: {
    getBlueprint: vi.fn(() => ({ blueprint: { configurations: [] } })),
    mergeBlueprints: vi.fn(() => ({ blueprint: { configurations: [] } })),
  },
  isShownOnInstallerSlaveClientPage: vi.fn(() => true),
  isValidUserName: vi.fn(() => true),
  minToInstall: vi.fn(() => 0),
}));

vi.mock("./RestAllTabs", () => ({
  default: ({
    configProperties,
    setConfigProperties,
    services,
    selectedService,
    onServiceChange,
    onConfigEdit,
    tabName,
  }: {
    configProperties: TestConfigProperties;
    setConfigProperties: (
      value:
        | TestConfigProperties
        | ((current: TestConfigProperties) => TestConfigProperties),
    ) => void;
    services: string[];
    selectedService?: string;
    onServiceChange?: (serviceName: string) => void;
    onConfigEdit?: () => void;
    tabName: string;
  }) => {
    const properties = Object.values(configProperties).flatMap((service) =>
      Object.values(service).flatMap((category) =>
        Object.values(category.properties || {})
      )
    );
    const existingProperty = properties.find(
      (property) => property.propertyName === "existing.property"
    );
    return (
      <div data-testid={`config-${tabName}`}>
        {String(existingProperty?.value ?? "missing")}
        <span data-testid={`selected-service-${tabName}`}>
          {selectedService || services[0] || ""}
        </span>
        {services.map((serviceName) => (
          <button
            key={serviceName}
            aria-label={`${tabName} service ${serviceName}`}
            onClick={() => onServiceChange?.(serviceName)}
          >
            {serviceName}
          </button>
        ))}
        <button
          onClick={() => {
            setConfigProperties((current) => {
              const updated = structuredClone(current);
              const property = Object.values(updated).flatMap((service) =>
                Object.values(service).flatMap((category) =>
                  Object.values(category.properties || {})
                )
              ).find((candidate) => candidate.propertyName === "existing.property");
              if (property) {
                property.value = property.value === "edited-during-fallback"
                  ? "edited-during-retry"
                  : "edited-during-fallback";
              }
              return updated;
            });
            onConfigEdit?.();
          }}
        >
          Edit current property
        </button>
      </div>
    );
  },
}));

vi.mock("../../../components/StepWizard/WizardFooter", () => ({
  default: ({
    isNextEnabled,
    onNext,
  }: {
    isNextEnabled: boolean;
    onNext: () => void;
    sideItems?: ReactNode;
  }) => (
    <button disabled={!isNextEnabled} onClick={onNext}>
      Next
    </button>
  ),
}));

import Step7, {
  findNextEnabledConfigurationTab,
  findPreviousEnabledConfigurationTab,
} from ".";

const stackConfigurations = {
  items: [
    {
      configurations: [
        {
          StackConfigurations: {
            property_name: "existing.property",
            property_value: "stack-default",
            property_value_attributes: { type: "string", visible: true },
            property_type: [],
            service_name: "HDFS",
            type: "hdfs-site.xml",
          },
          dependencies: [],
        },
      ],
    },
  ],
};

const existingClusterValues = {
  items: [
    {
      group_name: "Default",
      service_name: "HDFS",
      configurations: [
        {
          type: "hdfs-site",
          properties: { "existing.property": "cluster-current" },
          properties_attributes: { final: {} },
        },
      ],
    },
  ],
};

const wizardSteps = {
  VERSION: {
    data: {
      selectedStack: { stack_name: "BIGTOP" },
      selectedVersion: { stack_version: "3.3.0" },
    },
  },
  HOST_STATUS: {
    data: { hosts: [{ bootStatus: "REGISTERED", name: "host1" }] },
  },
  SERVICES: {
    data: { services: { HDFS: { selected: true } } },
  },
  MASTERS: { data: { mastersData: [] } },
  SLAVES_AND_CLIENTS: { data: { serviceComponents: [] } },
};

type WizardStepsFixture = Omit<typeof wizardSteps, "SERVICES"> & {
  SERVICES: {
    data: {
      services: Record<string, { selected: boolean }>;
    };
  };
  CONFIGURATION?: { data: Record<string, unknown> };
};

const createWizardSteps = () =>
  structuredClone(wizardSteps) as unknown as WizardStepsFixture;

const themeResource = (serviceName: string, themeName: string) => ({
  ThemeInfo: {
    service_name: serviceName,
    file_name: `${themeName}.json`,
    theme_data: {
      Theme: {
        name: themeName,
        configuration: {
          layouts: [
            {
              name: themeName,
              tabs: [
                {
                  name: themeName,
                  layout: {
                    sections: [
                      {
                        name: themeName,
                        subsections: [{ name: `${themeName}-settings` }],
                      },
                    ],
                  },
                },
              ],
            },
          ],
          placement: { configs: [] },
          widgets: [],
        },
      },
    },
  },
});

const themedServicesResponse = (
  services: string[],
  themeNames = ["credentials", "database", "directories", "default"],
) => ({
  items: services.map((serviceName) => ({
    StackServices: { service_name: serviceName },
    themes: themeNames.map((themeName) =>
      themeResource(serviceName, themeName),
    ),
  })),
});

const findStoredProperty = (
  payload: ConfigurationPayload,
  propertyName = "existing.property"
) => {
  const configProperties = payload?.data?.configProperties || {};
  return Object.values(configProperties).flatMap((service) =>
    Object.values(service).flatMap((category) =>
      Object.values(category.properties || {})
    )
  ).find((property) => property.propertyName === propertyName);
};

function renderStep(
  wizardName: "clusterCreation" | "addService",
  steps: WizardStepsFixture = createWizardSteps(),
) {
  const state = {
    [`${wizardName}Steps`]: steps,
  };
  const contextValue = {
    dispatch: mocks.dispatch,
    flushStateToDb: mocks.flushStateToDb,
    installedHosts: ["host1"],
    installedServices: wizardName === "addService" ? ["HDFS"] : [],
    withStateCheckpoint: async (request: (revision: number) => Promise<unknown>) =>
      request(19),
    state,
    stepWizardUtilities: {
      currentStep: { name: "CONFIGURATION" },
      handleNextImperitive: mocks.handleNextImperitive,
      jumpToStep: mocks.jumpToStep,
    },
  };
  const WizardContext = createContext(contextValue);

  return render(
    <AppContext.Provider
      value={
        {
          clusterName: "cluster1",
          cluster: { cluster_id: 27 },
          supports: { preInstallChecks: false },
        } as unknown as ContextType<typeof AppContext>
      }
    >
      <ServiceContext.Provider
        value={
          { allServiceModels: {} } as unknown as ContextType<
            typeof ServiceContext
          >
        }
      >
        <ContextWrapper.Provider value={{ Context: WizardContext }}>
          <WizardContext.Provider value={contextValue}>
            <Step7 wizardName={wizardName} />
          </WizardContext.Provider>
        </ContextWrapper.Provider>
      </ServiceContext.Provider>
    </AppContext.Provider>
  );
}

describe("Step 7 Theme fallback", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.useEnhancedConfigsArgs.length = 0;
    mocks.flushStateToDb.mockResolvedValue(undefined);
    mocks.getConfigValues.mockResolvedValue(existingClusterValues);
    mocks.getConfigsByTags.mockResolvedValue({ items: [] });
    mocks.getRecommendations.mockResolvedValue({});
    mocks.getStackConfigurations.mockResolvedValue(stackConfigurations);
    mocks.getStackLevelConfigurations.mockResolvedValue({ configurations: [] });
    mocks.getStackThemes.mockRejectedValueOnce(new Error("Theme request failed"));
    mocks.loadConfigTags.mockResolvedValue({ Clusters: { desired_configs: {} } });
  });

  afterEach(cleanup);

  it("skips disabled configuration categories when advancing", () => {
    expect(
      findNextEnabledConfigurationTab(
        "credentials",
        {
          credentials: { nextTab: "databases" },
          databases: { nextTab: "directories" },
          directories: { nextTab: "accounts" },
          accounts: { nextTab: "allConfigurations" },
          allConfigurations: {},
        },
        ["databases", "directories"],
      ),
    ).toBe("accounts");
  });

  it("skips disabled configuration categories when going back", () => {
    expect(
      findPreviousEnabledConfigurationTab(
        "accounts",
        {
          credentials: { nextTab: "databases" },
          databases: { nextTab: "directories" },
          directories: { nextTab: "accounts" },
          accounts: { nextTab: "allConfigurations" },
          allConfigurations: {},
        },
        ["databases", "directories"],
      ),
    ).toBe("credentials");
  });

  it("waits for a successful Theme request to settle before creating configs", async () => {
    let resolveThemes: (value: { items: never[] }) => void = () => undefined;
    mocks.getStackThemes.mockReset();
    mocks.getStackThemes.mockImplementationOnce(
      () =>
        new Promise<{ items: never[] }>((resolve) => {
          resolveThemes = resolve;
        })
    );

    renderStep("clusterCreation");

    await waitFor(() =>
      expect(mocks.getStackConfigurations).toHaveBeenCalledTimes(1)
    );
    expect(screen.queryByTestId("config-default")).toBeNull();

    await act(async () => resolveThemes({ items: [] }));
    expect(
      (await screen.findByTestId("config-default")).textContent
    ).toContain("stack-default");
  });

  it("enables all five new-cluster categories when selected services define every specialized Theme", async () => {
    mocks.getStackThemes.mockReset();
    mocks.getStackThemes.mockResolvedValueOnce(
      themedServicesResponse(["HDFS"]),
    );

    renderStep("clusterCreation");

    for (const tabName of [
      "CREDENTIALS",
      "DATABASES",
      "DIRECTORIES",
      "ACCOUNTS",
      "ALL CONFIGURATIONS",
    ]) {
      expect(
        (
          await screen.findByRole("tab", { name: new RegExp(tabName) })
        ).getAttribute("aria-disabled"),
      ).not.toBe("true");
    }
  });

  it("keeps the active category when a malformed Theme response is retried", async () => {
    const steps = createWizardSteps();
    steps.SERVICES.data.services = {
      HDFS: { selected: true },
      HIVE: { selected: true },
    };
    mocks.getStackThemes.mockReset();
    mocks.getStackThemes.mockResolvedValueOnce({
      items: [
        themedServicesResponse(["HDFS"]).items[0],
        {
          StackServices: { service_name: "HIVE" },
          themes: [{ ThemeInfo: { service_name: "HIVE" } }],
        },
      ],
    });

    renderStep("clusterCreation", steps);

    fireEvent.click(
      await screen.findByRole("tab", { name: /DIRECTORIES/ }),
    );
    expect(screen.getByTestId("config-directories")).toBeTruthy();
    expect(screen.getByText(/missing service_name or theme_data.Theme/)).toBeTruthy();

    mocks.getStackThemes.mockResolvedValueOnce(
      themedServicesResponse(["HDFS", "HIVE"]),
    );
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));

    await waitFor(() => expect(mocks.getStackThemes).toHaveBeenCalledTimes(2));
    await waitFor(() =>
      expect(
        screen.getByRole("tab", { name: /DIRECTORIES/ }).getAttribute(
          "aria-selected",
        ),
      ).toBe("true"),
    );
    expect(screen.getByTestId("config-directories")).toBeTruthy();
  });

  it("restores the selected service independently for each specialized category", async () => {
    const steps = createWizardSteps();
    steps.SERVICES.data.services = {
      HDFS: { selected: true },
      HIVE: { selected: true },
    };
    mocks.getStackThemes.mockReset();
    mocks.getStackThemes.mockResolvedValueOnce(
      themedServicesResponse(["HDFS", "HIVE"]),
    );

    renderStep("clusterCreation", steps);

    fireEvent.click(await screen.findByRole("tab", { name: /DATABASES/ }));
    fireEvent.click(screen.getByRole("button", { name: "database service HIVE" }));
    expect(screen.getByTestId("selected-service-database").textContent).toBe(
      "HIVE",
    );

    fireEvent.click(screen.getByRole("tab", { name: /DIRECTORIES/ }));
    fireEvent.click(
      screen.getByRole("button", { name: "directories service HDFS" }),
    );
    fireEvent.click(screen.getByRole("tab", { name: /DATABASES/ }));

    expect(screen.getByTestId("selected-service-database").textContent).toBe(
      "HIVE",
    );
    const storedNavigation = mocks.dispatch.mock.calls
      .map(([action]) => action.payload?.data?.navigation)
      .filter(Boolean)
      .at(-1);
    expect(storedNavigation.selectedServicesByTab).toMatchObject({
      databases: "HIVE",
      directories: "HDFS",
    });
  });

  it("opens All Configurations when stored Step 7 values are re-entered", async () => {
    const steps = createWizardSteps();
    steps.CONFIGURATION = {
      data: {
        configProperties: {
          HDFS: {
            "hdfs-site": {
              errors: 0,
              properties: {
                "existing.property": {
                  propertyName: "existing.property",
                  propertyDisplayname: "Existing property",
                  propertyValue: "saved-value",
                  propertyAttributes: { type: "string" },
                  previousValue: "saved-value",
                  value: "saved-value",
                  final: "false",
                  type: "hdfs-site",
                  serviceName: "HDFS",
                  isEditable: true,
                },
              },
            },
          },
        },
        configs: stackConfigurations,
        stackLevelConfigs: { configurations: [] },
        themes: themedServicesResponse(["HDFS"]),
        navigation: { selectedTab: "databases" },
      },
    };

    renderStep("clusterCreation", steps);

    expect(
      (
        await screen.findByRole("tab", { name: /ALL CONFIGURATIONS/ })
      ).getAttribute("aria-selected"),
    ).toBe("true");
    expect(screen.getByTestId("config-default").textContent).toContain(
      "saved-value",
    );
  });

  it("merges reviewed provider values into read-only configuration state", async () => {
    const steps = createWizardSteps() as any;
    steps.SERVICES.data.services = { HBASE: { selected: true } };
    steps.SERVICES.data.managedDependencies = {
      HDFS: {
        mode: "managed",
        provider: {
          cluster_id: 9,
          cluster_name: "provider-a",
          service_name: "HDFS",
        },
        preview: {
          binding_id: "11111111-1111-4111-8111-111111111111",
          client_config: {
            "hbase-site": { "existing.property": "provider-required" },
          },
          compatible: true,
          consumer: {
            lifecycle: "INIT",
            planned_hbase_user: "hbase_a",
            scope: "DRAFT",
            service_name: "HBASE",
          },
          dependency_type: "HDFS",
          errors: [],
          preview_schema_version: 2,
          provider: {
            cluster_id: 9,
            cluster_name: "provider-a",
            service_name: "HDFS",
          },
          consumer_descriptor_fingerprint: "consumer-fingerprint",
          provider_fingerprint: "provider-fingerprint",
          snapshot_fingerprint: "snapshot-fingerprint",
        },
      },
    };
    steps.CONFIGURATION = {
      data: {
        configProperties: {
          HBASE: {
            General: {
              errors: 0,
              properties: {
                existing: {
                  fileName: "hbase-site.xml",
                  final: "false",
                  isEditable: true,
                  propertyAttributes: { type: "string" },
                  propertyDisplayname: "Existing property",
                  propertyName: "existing.property",
                  propertyValue: "local-edited",
                  previousValue: "local-default",
                  serviceName: "HBASE",
                  type: "hbase-site",
                  value: "local-edited",
                },
              },
            },
          },
        },
        configs: stackConfigurations,
        stackLevelConfigs: { configurations: [] },
        themes: themedServicesResponse(["HBASE"]),
      },
    };

    renderStep("clusterCreation", steps);

    expect((await screen.findByTestId("config-default")).textContent)
      .toContain("provider-required");
    await waitFor(() => expect(mocks.dispatch.mock.calls.some(([action]) =>
      action.payload?.data?.managedDependencyClientConfig?.["hdfs-site"]?.[
        "existing.property"
      ] === "provider-required")).toBe(false));
    await waitFor(() => expect(mocks.dispatch.mock.calls.some(([action]) =>
      action.payload?.data?.managedDependencyClientConfig?.["hbase-site"]?.[
        "existing.property"
      ] === "provider-required")).toBe(true));
    const mergedProperty = mocks.dispatch.mock.calls
      .map(([action]) => action.payload?.data?.configProperties)
      .filter(Boolean)
      .flatMap((properties) => Object.values(properties as TestConfigProperties))
      .flatMap((service) => Object.values(service))
      .flatMap((category) => Object.values(category.properties || {}))
      .find((property) => property.propertyName === "existing.property"
        && property.value === "provider-required");
    expect(mergedProperty).toMatchObject({
      isEditable: false,
      isManagedDependency: true,
    });
  });

  it("restores ordinary config metadata when a recovered provider choice is local", async () => {
    const steps = createWizardSteps() as any;
    steps.SERVICES.data.services = { HBASE: { selected: true } };
    steps.SERVICES.data.managedDependencies = { HDFS: { mode: "local" } };
    steps.CONFIGURATION = {
      data: {
        configProperties: {
          HBASE: {
            General: {
              errors: 0,
              properties: {
                existing: {
                  _managedDependencyBase: {
                    isEditable: { present: true, value: true },
                    recommendedValue: { present: true, value: "local-default" },
                    value: { present: true, value: "local-edited" },
                  },
                  fileName: "hbase-site.xml",
                  final: "false",
                  isEditable: false,
                  isManagedDependency: true,
                  propertyAttributes: { type: "string" },
                  propertyDisplayname: "Existing property",
                  propertyName: "existing.property",
                  propertyValue: "local-default",
                  previousValue: "local-default",
                  recommendedValue: "provider-required",
                  serviceName: "HBASE",
                  type: "hbase-site",
                  value: "provider-required",
                },
              },
            },
          },
        },
        configs: stackConfigurations,
        stackLevelConfigs: { configurations: [] },
        themes: themedServicesResponse(["HBASE"]),
      },
    };

    renderStep("clusterCreation", steps);

    expect((await screen.findByTestId("config-default")).textContent)
      .toContain("local-edited");
    const restoredProperty = await waitFor(() => {
      const property = mocks.dispatch.mock.calls
        .map(([action]) => action.payload?.data?.configProperties)
        .filter(Boolean)
        .flatMap((properties) => Object.values(properties as TestConfigProperties))
        .flatMap((service) => Object.values(service))
        .flatMap((category) => Object.values(category.properties || {}))
        .find((candidate) => candidate.propertyName === "existing.property"
          && candidate.value === "local-edited");
      expect(property).toBeTruthy();
      return property;
    });
    expect(restoredProperty).toMatchObject({
      isEditable: true,
      recommendedValue: "local-default",
    });
    expect(restoredProperty).not.toHaveProperty("_managedDependencyBase");
    expect(restoredProperty).not.toHaveProperty("isManagedDependency");
  });

  it("treats a successful empty Theme collection as non-retryable fallback", async () => {
    mocks.getStackThemes.mockReset();
    mocks.getStackThemes.mockResolvedValueOnce({ items: [] });

    renderStep("clusterCreation");

    expect(
      await screen.findByText(/No Theme layouts are defined/),
    ).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Retry" })).toBeNull();
    expect(screen.getByTestId("config-default")).toBeTruthy();
  });

  it("keeps new-cluster Advanced configurations usable when Theme loading fails", async () => {
    renderStep("clusterCreation");

    expect(
      (await screen.findByTestId("config-default")).textContent
    ).toContain("stack-default");
    expect(screen.getByText(/Theme request failed/)).toBeTruthy();
    expect(screen.getByText(/Advanced configurations remain available/)).toBeTruthy();
    expect(mocks.getStackConfigurations).toHaveBeenCalledTimes(1);
    expect(mocks.getStackLevelConfigurations).toHaveBeenCalledTimes(1);

    fireEvent.click(
      screen.getByRole("button", { name: "Edit current property" }),
    );
    expect(screen.getByTestId("config-default").textContent).toContain(
      "edited-during-fallback",
    );

    mocks.getStackThemes.mockResolvedValueOnce({ items: [] });
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    await waitFor(() => expect(mocks.getStackThemes).toHaveBeenCalledTimes(2));
    await waitFor(() =>
      expect(screen.queryByText(/Theme request failed/)).toBeNull()
    );
    expect(screen.getByTestId("config-default").textContent).toContain(
      "edited-during-fallback"
    );
    expect(mocks.updateVisibilityForDependsOn).toHaveBeenCalledTimes(2);
  });

  it("preserves edits made while a Theme retry is in flight", async () => {
    let resolveThemes: (value: { items: never[] }) => void = () => undefined;
    renderStep("clusterCreation");

    await screen.findByTestId("config-default");
    fireEvent.click(screen.getByRole("button", { name: "Edit current property" }));
    expect(screen.getByTestId("config-default").textContent).toContain(
      "edited-during-fallback",
    );

    mocks.getStackThemes.mockImplementationOnce(
      () => new Promise<{ items: never[] }>((resolve) => {
        resolveThemes = resolve;
      }),
    );
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    await waitFor(() => expect(mocks.getStackThemes).toHaveBeenCalledTimes(2));

    fireEvent.click(screen.getByRole("button", { name: "Edit current property" }));
    expect(screen.getByTestId("config-default").textContent).toContain(
      "edited-during-retry",
    );

    await act(async () => resolveThemes({ items: [] }));
    await waitFor(() =>
      expect(screen.queryByText(/Theme request failed/)).toBeNull(),
    );
    expect(screen.getByTestId("config-default").textContent).toContain(
      "edited-during-retry",
    );
  });

  it("retains and persists current cluster values before Add Service advances", async () => {
    mocks.getStackLevelConfigurations.mockResolvedValueOnce({
      configurations: [
        {
          StackLevelConfigurations: {
            property_name: "existing.cluster.property",
            property_value: "stack-cluster-default",
            property_value_attributes: { type: "string" },
            property_type: [],
            type: "cluster-env.xml",
          },
        },
      ],
    });
    mocks.loadConfigTags.mockResolvedValueOnce({
      Clusters: { desired_configs: { "cluster-env": { tag: "current" } } },
    });
    mocks.getConfigsByTags.mockResolvedValueOnce({
      items: [
        {
          type: "cluster-env",
          properties: { "existing.cluster.property": "cluster-env-current" },
        },
      ],
    });
    renderStep("addService");

    expect(
      (await screen.findByTestId("config-default")).textContent
    ).toContain("cluster-current");
    expect(screen.getByText(/Theme request failed/)).toBeTruthy();

    mocks.dispatch.mockClear();
    fireEvent.click(screen.getByRole("button", { name: "Next" }));

    await waitFor(() =>
      expect(mocks.flushStateToDb).toHaveBeenCalledWith("jump", 5)
    );
    const storedConfiguration = mocks.dispatch.mock.calls
      .map(([action]) => action)
      .filter((action) => action.type === "STORE INFORMATION")
      .at(-1)?.payload;
    expect(findStoredProperty(storedConfiguration)?.value).toBe(
      "cluster-current"
    );
    expect(
      findStoredProperty(
        storedConfiguration,
        "existing.cluster.property"
      )?.value
    ).toBe("cluster-env-current");
    expect(mocks.dispatch.mock.invocationCallOrder.at(-1)).toBeLessThan(
      mocks.flushStateToDb.mock.invocationCallOrder[0]
    );
    expect(mocks.jumpToStep).toHaveBeenCalledWith(5);
  });

  it("loads installed and newly selected Add Service context while recommending only new services", async () => {
    const steps = createWizardSteps();
    steps.SERVICES.data.services = { HIVE: { selected: true } };

    renderStep("addService", steps);

    await waitFor(() =>
      expect(mocks.getStackThemes).toHaveBeenCalledWith(
        "BIGTOP",
        "3.3.0",
        "HDFS,HIVE",
        "themes/*",
      ),
    );
    expect(mocks.getStackConfigurations).toHaveBeenCalledWith(
      "BIGTOP",
      "3.3.0",
      "HDFS,HIVE",
      "configurations/*,configurations/dependencies/*,StackServices/config_types/*",
    );
    await waitFor(() =>
      expect(mocks.loadAddServiceRecommendations).toHaveBeenCalled(),
    );
    expect(mocks.loadAddServiceRecommendations.mock.calls[0][1]).toEqual([
      "HIVE",
    ]);
  });

  it("uses ClusterCreate recommendation context for a new cluster", async () => {
    renderStep("clusterCreation");

    await waitFor(() => expect(mocks.getRecommendations).toHaveBeenCalled());
    expect(mocks.getRecommendations.mock.calls[0][2]).toMatchObject({
      services: ["HDFS"],
      user_context: { operation: "ClusterCreate" },
    });
  });

  it("passes the owned SERVICE_PLAN runner to the Add Service recommendation caller", async () => {
    const steps = createWizardSteps() as any;
    steps.SERVICES.data.services = {
      HBASE: { selected: true, installed: false },
    };
    steps.SERVICES.data.managedDependencies = {
      HDFS: {
        mode: "managed",
        provider: {
          cluster_id: 31,
          service_name: "HDFS",
        },
        preview: {
          binding_id: "11111111-1111-4111-8111-111111111111",
          compatible: true,
          consumer: {
            lifecycle: "ADD_SERVICE_PLAN",
            planned_hbase_user: "hbase_a",
            scope: "SERVICE_PLAN",
            service_name: "HBASE",
          },
          consumer_descriptor_fingerprint: "consumer-fingerprint",
          dependency_type: "HDFS",
          errors: [],
          preview_schema_version: 2,
          provider: {
            cluster_id: 31,
            service_name: "HDFS",
          },
          provider_fingerprint: "provider-fingerprint",
          snapshot_fingerprint: "snapshot-fingerprint",
        },
      },
    };

    renderStep("addService", steps);

    await waitFor(() => expect(mocks.useEnhancedConfigsArgs.length).toBeGreaterThan(0));
    const latestArgs = mocks.useEnhancedConfigsArgs.at(-1) as unknown[];
    const runner = latestArgs[9] as (
      request: (prepared: unknown) => Promise<unknown>
    ) => Promise<unknown>;
    const prepared = await runner(async (request: unknown) => request);

    expect((prepared as any).properties.managed_dependency_plan.consumer).toEqual({
      scope: "SERVICE_PLAN",
      cluster_id: 27,
      expected_revision: 19,
    });
    expect(
      (prepared as any).properties.managed_dependency_plan.selections,
    ).toMatchObject([{
      dependency_type: "HDFS",
      expected_consumer_descriptor_fingerprint: "consumer-fingerprint",
      expected_provider_fingerprint: "provider-fingerprint",
      expected_snapshot_fingerprint: "snapshot-fingerprint",
    }]);
  });

  it("does not rerun initial advice when a configuration edit changes scope", async () => {
    const steps = createWizardSteps() as any;
    steps.SERVICES.data.services = {
      HBASE: { selected: true, installed: false },
    };
    steps.SERVICES.data.managedDependencies = {
      HDFS: {
        mode: "managed",
        provider: { cluster_id: 31, service_name: "HDFS" },
        preview: {
          binding_id: "11111111-1111-4111-8111-111111111111",
          compatible: true,
          consumer: {
            lifecycle: "DRAFT",
            planned_hbase_user: "hbase",
            scope: "DRAFT",
            service_name: "HBASE",
          },
          consumer_descriptor_fingerprint: "consumer-fingerprint",
          dependency_type: "HDFS",
          errors: [],
          preview_schema_version: 2,
          provider: { cluster_id: 31, service_name: "HDFS" },
          provider_fingerprint: "provider-fingerprint",
          snapshot_fingerprint: "snapshot-fingerprint",
        },
      },
    };
    renderStep("clusterCreation", steps);

    await waitFor(() => expect(mocks.getRecommendations).toHaveBeenCalledTimes(1));
    fireEvent.click(await screen.findByRole("button", { name: "Edit current property" }));
    await act(async () => {
      await Promise.resolve();
    });

    expect(mocks.getRecommendations).toHaveBeenCalledTimes(1);
  });

  it("keeps Step 7 configuration visible and offers Retry after advice failure", async () => {
    const steps = createWizardSteps() as any;
    steps.CONFIGURATION = {
      data: {
        configProperties: {
          HDFS: {
            "hdfs-site": {
              errors: 0,
              properties: {
                existing: {
                  propertyName: "existing.property",
                  propertyDisplayname: "Existing property",
                  propertyValue: "cluster-current",
                  propertyAttributes: { type: "string" },
                  previousValue: "cluster-current",
                  value: "cluster-current",
                  final: "false",
                  type: "hdfs-site",
                  serviceName: "HDFS",
                  isEditable: true,
                },
              },
            },
          },
        },
        configs: stackConfigurations,
        stackLevelConfigs: { configurations: [] },
        themes: themedServicesResponse(["HDFS"]),
        navigation: { selectedTab: "allConfigurations" },
      },
    };
    mocks.getRecommendations.mockRejectedValueOnce(new Error("advisor failed"));

    renderStep("clusterCreation", steps);

    expect(await screen.findByText("advisor failed")).toBeTruthy();
    expect(screen.getByRole("button", { name: "Retry" })).toBeTruthy();
    expect(screen.getByTestId("config-default")).toBeTruthy();

    mocks.getRecommendations.mockResolvedValueOnce({});
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    await waitFor(() => expect(mocks.getRecommendations).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(screen.queryByText("advisor failed")).toBeNull());
  });
});
