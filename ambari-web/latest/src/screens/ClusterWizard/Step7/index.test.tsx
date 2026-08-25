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
  default: () => ({
    loadAddServiceRecommendations: mocks.loadAddServiceRecommendations,
    processRecommendations: mocks.processRecommendations,
    recommendedChanges: mocks.recommendedChanges,
    setRecommendedChanges: mocks.setRecommendedChanges,
  }),
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
    tabName,
  }: {
    configProperties: TestConfigProperties;
    setConfigProperties: (
      value:
        | TestConfigProperties
        | ((current: TestConfigProperties) => TestConfigProperties),
    ) => void;
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
        <button
          onClick={() =>
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
            })
          }
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

function renderStep(wizardName: "clusterCreation" | "addService") {
  const state = {
    [`${wizardName}Steps`]: structuredClone(wizardSteps),
  };
  const contextValue = {
    dispatch: mocks.dispatch,
    flushStateToDb: mocks.flushStateToDb,
    installedHosts: ["host1"],
    installedServices: wizardName === "addService" ? ["HDFS"] : [],
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
});
