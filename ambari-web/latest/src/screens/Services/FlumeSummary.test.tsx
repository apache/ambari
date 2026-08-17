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

import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { ComponentProps, isValidElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../store/context";
import { ServiceContext } from "../../store/ServiceContext";

const mocks = vi.hoisted(() => ({
  fetchAllServiceComponents: vi.fn(),
  hasAuthorization: vi.fn(),
  hideModal: vi.fn(),
  showModal: vi.fn(),
  updateFlumeAgent: vi.fn(),
}));

vi.mock("../../api/serviceApi", () => ({
  ServiceApi: { updateFlumeAgent: mocks.updateFlumeAgent },
}));
vi.mock("../../api/cachedServiceApi", () => ({
  cachedServiceApi: {
    fetchAllServiceComponents: mocks.fetchAllServiceComponents,
  },
}));
vi.mock("../../hooks/useAuth", () => ({
  useAuth: () => ({ hasAuthorization: mocks.hasAuthorization }),
}));
vi.mock("../../store/ModalManager", () => ({
  default: { hide: mocks.hideModal, show: mocks.showModal },
}));
vi.mock("../BackgroundOperations", () => ({
  default: ({ requestId }: { requestId: number }) => (
    <div>Background request {requestId}</div>
  ),
}));

import FlumeSummary from "./FlumeSummary";

const componentData = [
  {
    ServiceComponentInfo: {
      service_name: "FLUME",
      component_name: "FLUME_HANDLER",
    },
    host_components: [
      {
        HostRoles: { host_name: "host1" },
        processes: [
          {
            HostComponentProcess: {
              name: "stopped-agent",
              status: "NOT_RUNNING",
            },
          },
          {
            HostComponentProcess: {
              name: "running-agent",
              status: "RUNNING",
            },
          },
        ],
      },
    ],
  },
];

const renderSummary = () => render(
  <AppContext.Provider
    value={
      {
        backgroundOperations: [],
        clusterName: "c1",
        wizardIsNotFinished: false,
      } as unknown as ComponentProps<typeof AppContext.Provider>["value"]
    }
  >
    <ServiceContext.Provider
      value={
        {
          masterSlaveClientsData: componentData,
        } as unknown as ComponentProps<typeof ServiceContext.Provider>["value"]
      }
    >
      <FlumeSummary />
    </ServiceContext.Provider>
  </AppContext.Provider>
);

const confirmAction = (agentName: string, actionName: string) => {
  fireEvent.click(
    screen.getByRole("button", {
      name: `Actions for Flume agent ${agentName} on host1`,
    })
  );
  fireEvent.click(screen.getByText(actionName));
  const confirmation = mocks.showModal.mock.calls.at(-1)?.[0];
  expect(confirmation).toMatchObject({
    modalTitle: "Confirmation",
    options: { okButtonText: actionName.split(" ")[0].toUpperCase() },
  });
  return confirmation;
};

describe("Flume summary", () => {
  afterEach(cleanup);

  beforeEach(() => {
    mocks.fetchAllServiceComponents.mockReset();
    mocks.fetchAllServiceComponents.mockResolvedValue({});
    mocks.hasAuthorization.mockReset();
    mocks.hasAuthorization.mockReturnValue(true);
    mocks.hideModal.mockReset();
    mocks.showModal.mockReset();
    mocks.updateFlumeAgent.mockReset();
    mocks.updateFlumeAgent.mockResolvedValue({
      data: { Requests: { id: 42 } },
    });
  });

  it("submits a state-valid action and opens its background request", async () => {
    renderSummary();
    const confirmation = confirmAction("stopped-agent", "Start Agent");

    await act(async () => confirmation.successCallback());

    expect(mocks.updateFlumeAgent).toHaveBeenCalledWith(
      "c1",
      "host1",
      "stopped-agent",
      "STARTED",
      "Start Flume Agent stopped-agent"
    );
    await waitFor(() => expect(mocks.showModal).toHaveBeenCalledTimes(2));
    const backgroundModal = mocks.showModal.mock.calls.at(-1)?.[0];
    expect(isValidElement(backgroundModal)).toBe(true);
    expect(backgroundModal.props.requestId).toBe(42);
  });

  it("recovers from submission failure and exposes Retry", async () => {
    mocks.updateFlumeAgent.mockRejectedValueOnce(new Error("submit failed"));
    renderSummary();
    const confirmation = confirmAction("running-agent", "Stop Agent");

    await act(async () => confirmation.successCallback());

    await waitFor(() => {
      expect(mocks.showModal.mock.calls.at(-1)?.[0]).toMatchObject({
        modalTitle: "Stop Flume Agent Failed",
        modalBody: "submit failed",
        options: { okButtonText: "RETRY" },
      });
    });
    expect(
      screen.getByRole("button", {
        name: "Actions for Flume agent running-agent on host1",
      }).hasAttribute("disabled")
    ).toBe(false);

    const failure = mocks.showModal.mock.calls.at(-1)?.[0];
    await act(async () => failure.successCallback());
    expect(mocks.updateFlumeAgent).toHaveBeenCalledTimes(2);
  });

  it("renders status without mutation controls when authorization is absent", () => {
    mocks.hasAuthorization.mockReturnValue(false);
    renderSummary();

    expect(screen.getByText("Running")).toBeTruthy();
    expect(screen.queryByRole("button")).toBeNull();
  });
});
