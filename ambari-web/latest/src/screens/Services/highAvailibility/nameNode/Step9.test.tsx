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
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../../../store/context";
import { ServiceContext } from "../../../../store/ServiceContext";
import { EnableHighAvailibilityContext } from "./store/context";

const mocks = vi.hoisted(() => ({
  modalShow: vi.fn(),
  modalHide: vi.fn(),
}));

vi.mock("../../../../store/ModalManager", () => ({
  default: { show: mocks.modalShow, hide: mocks.modalHide },
}));
vi.mock("i18next", () => ({
  t: (key: string) =>
    ({
      "admin.highAvailability.step9.save.configuration.note":
        "NameNode HA configuration",
      "admin.highAvailability.wizard.step9.hawq.confirmPopup.header":
        "Additional Steps Required for HAWQ",
      "admin.highAvailability.wizard.step9.hawq.confirmPopup.body":
        "Follow HAWQ Filespaces and High Availability Enabled HDFS.",
    })[key],
}));
vi.mock("../../../../hooks/useKDCSessionState", () => ({
  default: () => ({ getKDCSessionState: vi.fn() }),
}));
vi.mock("../../../../components/OperationsProgress", () => ({
  default: ({ setCompletionStatus }: { setCompletionStatus: (value: boolean) => void }) => (
    <button onClick={() => setCompletionStatus(true)}>Finish operations</button>
  ),
}));
vi.mock("../../../../components/StepWizard/WizardFooter", () => ({
  default: ({
    onNext,
    isNextEnabled,
  }: {
    onNext: () => void;
    isNextEnabled: boolean;
  }) => (
    <button disabled={!isNextEnabled} onClick={onNext}>
      Complete wizard
    </button>
  ),
}));

import Step9 from "./Step9";

const wizardState = {
  enableHighAvailibilitySteps: {
    SELECT_HOSTS: {
      data: {
        masterComponentHosts: [
          { component: "NAMENODE", hostName: "nn1", isInstalled: true },
          { component: "NAMENODE", hostName: "nn2", isInstalled: false },
          {
            component: "SECONDARY_NAMENODE",
            hostName: "snn1",
            isInstalled: true,
          },
        ],
      },
    },
    REVIEW: { data: { overridenProperties: { items: [] } } },
  },
};

describe("NameNode HA completion", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("requires the Classic HAWQ filespace acknowledgement before clearing state", () => {
    const flushStateToDb = vi.fn();
    render(
      <AppContext.Provider
        value={
          {
            clusterName: "c1",
            services: [{ ServiceInfo: { service_name: "HAWQ" } }],
          } as never
        }
      >
        <ServiceContext.Provider
          value={
            {
              serviceModels: {},
              masterSlaveClientsData: {},
            } as never
          }
        >
          <EnableHighAvailibilityContext.Provider
            value={
              {
                state: wizardState,
                dispatch: vi.fn(),
                flushStateToDb,
                stepWizardUtilities: {
                  currentStep: { name: "FINALIZE" },
                },
              } as never
            }
          >
            <Step9 />
          </EnableHighAvailibilityContext.Provider>
        </ServiceContext.Provider>
      </AppContext.Provider>,
    );

    fireEvent.click(screen.getByRole("button", { name: "Finish operations" }));
    fireEvent.click(screen.getByRole("button", { name: "Complete wizard" }));

    expect(mocks.modalShow).toHaveBeenCalledOnce();
    expect(mocks.modalShow.mock.calls[0][0]).toMatchObject({
      modalTitle: "Additional Steps Required for HAWQ",
    });
    expect(mocks.modalShow.mock.calls[0][0].modalBody).toContain(
      "HAWQ Filespaces and High Availability Enabled HDFS",
    );
    expect(flushStateToDb).not.toHaveBeenCalled();
  });
});
