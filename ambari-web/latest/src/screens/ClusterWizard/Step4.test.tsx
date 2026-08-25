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

import { createContext } from "react";
import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ContextWrapper } from ".";

const mocks = vi.hoisted(() => ({
  getServices: vi.fn(),
}));

vi.mock("../../api/chooseServicesApi", () => ({
  ChooseServicesApi: { getServices: mocks.getServices },
}));
vi.mock("../../components/Table", () => ({
  default: ({ data }: { data: Array<{ displayName: string }> }) => (
    <div>{data.map((service) => (
      <span key={service.displayName}>{service.displayName}</span>
    ))}</div>
  ),
}));
vi.mock("../../components/Spinner", () => ({
  default: () => <div>Loading services</div>,
}));
vi.mock("../../components/StepWizard/WizardFooter", () => ({
  default: () => null,
}));

import Step4 from "./Step4";

const stackService = (
  serviceName: string,
  displayName: string,
  isInstallable?: boolean,
) => ({
  StackServices: {
    comments: `${displayName} description`,
    display_name: displayName,
    is_installable: isInstallable,
    required_services: [],
    service_name: serviceName,
    service_type: "SERVICE",
    service_version: "1.0",
  },
  components: [],
});

describe("Choose Services stack metadata", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getServices.mockResolvedValue({
      items: [
        stackService("HDFS", "HDFS"),
        stackService("KERBEROS", "Kerberos", true),
        stackService("CUSTOM", "Unavailable Service", false),
      ],
    });
  });

  function renderStep(wizardName: "clusterCreation" | "addService") {
    const value = {
      dispatch: vi.fn(),
      flushStateToDb: vi.fn(),
      handleBackImperitive: vi.fn(),
      installedServices: [],
      state: {
        [`${wizardName}Steps`]: {
          VERSION: {
            data: {
              selectedStack: { stack_name: "HDP" },
              selectedVersion: { stack_version: "3.1" },
            },
          },
        },
      },
      stepWizardUtilities: {
        currentStep: { canGoBack: true, name: "SERVICES" },
        handleNextImperitive: vi.fn(),
        jumpToStep: vi.fn(),
      },
    };
    const WizardContext = createContext(value);

    render(
      <ContextWrapper.Provider value={{ Context: WizardContext }}>
        <WizardContext.Provider value={value}>
          <Step4 wizardName={wizardName} />
        </WizardContext.Provider>
      </ContextWrapper.Provider>,
    );
  }

  it.each(["clusterCreation", "addService"] as const)(
    "does not offer non-installable services in %s",
    async (wizardName) => {
      renderStep(wizardName);

      await waitFor(() => expect(screen.getByText("HDFS")).toBeTruthy());
      expect(screen.queryByText("Kerberos")).toBeNull();
      expect(screen.queryByText("Unavailable Service")).toBeNull();
      expect(mocks.getServices).toHaveBeenCalledWith("HDP", "3.1");
    },
  );
});
