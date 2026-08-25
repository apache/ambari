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
import type { ComponentProps } from "react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../store/context";
import { ServiceContext } from "../../store/ServiceContext";

const mocks = vi.hoisted(() => ({
  actionRequest: vi.fn(),
  actionRequestRebalanceHDFS: vi.fn(),
  fetchBackgroundOperationsSnapshot: vi.fn().mockResolvedValue({ items: [] }),
  modalShow: vi.fn(),
  requestScheduleFetch: vi.fn(),
  serviceAction: vi.fn(),
  toastError: vi.fn(),
}));

vi.mock("../../api/actionsApi", () => ({
  ActionsApi: {
    actionRequest: mocks.actionRequest,
    actionRequestRebalanceHDFS: mocks.actionRequestRebalanceHDFS,
    regenerateKeytabsForService: vi.fn(),
    serviceAction: mocks.serviceAction,
    submitActionRequest: vi.fn(),
    turnOnOffMaintenance: vi.fn(),
  },
}));
vi.mock("../../api/requestScheduleApi", () => ({
  default: { fetch: mocks.requestScheduleFetch },
}));
vi.mock("../../store/ModalManager", () => ({
  default: { hide: vi.fn(), show: mocks.modalShow },
}));
vi.mock("react-hot-toast", () => ({
  default: { error: mocks.toastError },
}));
vi.mock("../../components/ConfirmationModal", () => ({
  default: ({
    isOpen,
    isOkDisabled,
    okButtonText,
    successCallback,
  }: {
    isOpen: boolean;
    isOkDisabled?: boolean;
    okButtonText?: string;
    successCallback: () => void;
  }) => isOpen ? (
    <div data-testid="confirmation-modal">
      <button disabled={isOkDisabled} onClick={successCallback}>
        {okButtonText || "OK"}
      </button>
    </div>
  ) : null,
}));
vi.mock("../../hooks/useAuthorizationPolicy", () => ({
  default: () => ({
    havePermissions: () => true,
    isAuthorized: (permission: string) => permission
      .split(",")
      .map((value) => value.trim())
      .includes("SERVICE.START_STOP"),
  }),
}));
vi.mock("../../hooks/useStackServices", () => ({
  default: () => ({ services: [], loading: false, error: null }),
}));
vi.mock("../../hooks/useConfigs", () => ({
  useConfigs: () => ({ getConfigByName: vi.fn() }),
}));
vi.mock("../Hosts/hooks/useComponentAddDelete", () => ({
  default: () => ({ addAndReconfigureComponent: vi.fn() }),
}));
vi.mock("../../hooks/useKDCSessionState", () => ({
  default: () => ({ getKDCSessionState: vi.fn() }),
}));
vi.mock("../../hooks/useServiceDeletion", () => ({
  useServiceDeletion: () => ({ deleteService: vi.fn() }),
}));
vi.mock("../../api/serviceApi", () => ({
  ServiceApi: {
    getServiceState: vi.fn(),
    isServiceCheckSupported: vi.fn().mockResolvedValue({
      data: { StackServices: { service_check_supported: false } },
    }),
  },
}));
vi.mock("./highAvailibility/WorkflowActions", () => ({ default: () => null }));
vi.mock("./highAvailibility/nameNode", () => ({ default: () => null }));
vi.mock("./highAvailibility/journalNode", () => ({ default: () => null }));
vi.mock("./highAvailibility/Federation", () => ({ default: () => null }));
vi.mock("./highAvailibility/rangerAdmin", () => ({ default: () => null }));
vi.mock("./highAvailibility/resourceManager", () => ({ default: () => null }));
vi.mock("./reassign", () => ({ default: () => null }));
vi.mock("./ServiceActionsUrlMapping", () => ({ default: () => null }));

import { Actions } from "./Actions";

let consoleErrorSpy: ReturnType<typeof vi.spyOn>;

beforeEach(() => {
  consoleErrorSpy = vi.spyOn(console, "error").mockImplementation(() => undefined);
});

afterEach(() => {
  cleanup();
  vi.useRealTimers();
  consoleErrorSpy.mockRestore();
  vi.clearAllMocks();
});

function renderActions(
  requests: unknown[],
  options: {
    enableNewServiceRestartOptions?: boolean;
    serviceModel?: Record<string, unknown>;
  } = {},
) {
  const serviceModel = options.serviceModel || {
    masterComponents: [{ totalCount: 1, startedCount: 1, hostComponents: [] }],
    slaveComponents: [],
    clientComponents: [],
  };
  return render(
    <MemoryRouter>
      <AppContext.Provider
        value={
          {
            cluster: {},
            clusterName: "c1",
            isClusterInstalled: false,
            isKerberosEnabled: false,
            supports: {
              opsDuringRollingUpgrade: false,
              enableNewServiceRestartOptions:
                options.enableNewServiceRestartOptions || false,
            },
            upgradeIsRunning: false,
            upgradeSuspended: false,
            wizardIsNotFinished: false,
            parsedSocketMessages: [],
            services: [],
            allHostNames: [],
            backgroundOperations: requests,
            fetchBackgroundOperationsSnapshot:
              mocks.fetchBackgroundOperationsSnapshot,
          } as unknown as ComponentProps<typeof AppContext.Provider>["value"]
        }
      >
        <ServiceContext.Provider
          value={
            {
              allServiceModels: { hdfs: serviceModel },
              serviceModels: { hdfs: serviceModel },
            } as unknown as ComponentProps<
              typeof ServiceContext.Provider
            >["value"]
          }
        >
          <Actions serviceName="HDFS" />
        </ServiceContext.Provider>
      </AppContext.Provider>
    </MemoryRouter>,
  );
}

describe("service actions", () => {
  it("disables start, stop, and restart while the service has active work", () => {
    renderActions([
      {
        Requests: {
          id: 7,
          request_context: "_PARSE_.START.HDFS",
          request_status: "IN_PROGRESS",
        },
      },
    ]);
    fireEvent.click(screen.getByRole("button", { name: "ACTIONS" }));

    ["Start", "Stop", "Restart All"].forEach((label) => {
      expect(screen.getByText(label).closest("button,a")?.classList)
        .toContain("disabled");
    });
  });

  it("locks start, stop, and restart after the server accepts a request", async () => {
    mocks.serviceAction.mockResolvedValue({
      status: 202,
      data: { Requests: { id: 77 } },
    });
    renderActions([]);

    fireEvent.click(screen.getByRole("button", { name: "ACTIONS" }));
    fireEvent.click(screen.getByText("Start"));
    fireEvent.click(screen.getByRole("button", { name: "CONFIRM START" }));

    await waitFor(() => expect(mocks.serviceAction).toHaveBeenCalledOnce());
    expect(mocks.fetchBackgroundOperationsSnapshot).toHaveBeenCalledOnce();
    expect(mocks.modalShow).toHaveBeenCalledOnce();

    fireEvent.click(screen.getByRole("button", { name: "ACTIONS" }));
    ["Start", "Stop", "Restart All"].forEach((label) => {
      expect(screen.getByText(label).closest("button,a")?.classList)
        .toContain("disabled");
    });
  });

  it("keeps the confirmation open and allows retry after submission fails", async () => {
    mocks.serviceAction
      .mockRejectedValueOnce(new Error("request failed"))
      .mockResolvedValueOnce({
        status: 202,
        data: { Requests: { id: 78 } },
      });
    renderActions([]);

    fireEvent.click(screen.getByRole("button", { name: "ACTIONS" }));
    fireEvent.click(screen.getByText("Start"));
    const confirmButton = screen.getByRole("button", { name: "CONFIRM START" });
    fireEvent.click(confirmButton);

    await waitFor(() => expect(mocks.toastError).toHaveBeenCalledOnce());
    expect(confirmButton.hasAttribute("disabled")).toBe(false);
    fireEvent.click(confirmButton);

    await waitFor(() => expect(mocks.serviceAction).toHaveBeenCalledTimes(2));
    expect(mocks.modalShow).toHaveBeenCalledOnce();
  });

  it("shows the service restart scopes only when the support flag is enabled", () => {
    const serviceModel = restartableHdfsModel();
    renderActions([], { serviceModel });
    fireEvent.click(screen.getByRole("button", { name: "ACTIONS" }));
    expect(screen.queryByTestId("service-restart-menu")).toBeNull();

    cleanup();
    renderActions([], {
      enableNewServiceRestartOptions: true,
      serviceModel,
    });
    fireEvent.click(screen.getByRole("button", { name: "ACTIONS" }));
    fireEvent.click(screen.getByTestId("service-restart-menu"));

    expect(screen.getByTestId("service-restart-all")).not.toBeNull();
    expect(screen.getByTestId("service-restart-masters")).not.toBeNull();
    expect(screen.getByTestId("service-restart-slaves")).not.toBeNull();
  });

  it("disables a restart scope when the service has no components in that group", () => {
    const serviceModel = restartableHdfsModel();
    serviceModel.slaveComponents = [];
    renderActions([], {
      enableNewServiceRestartOptions: true,
      serviceModel,
    });
    fireEvent.click(screen.getByRole("button", { name: "ACTIONS" }));
    fireEvent.click(screen.getByTestId("service-restart-menu"));

    expect(screen.getByTestId("service-restart-slaves").classList)
      .toContain("disabled");
    expect(screen.getByTestId("service-restart-masters").classList)
      .not.toContain("disabled");
  });

  it("rejects empty rolling interval and tolerance while accepting explicit zero", () => {
    renderActions([], {
      enableNewServiceRestartOptions: true,
      serviceModel: restartableHdfsModel(),
    });
    fireEvent.click(screen.getByRole("button", { name: "ACTIONS" }));
    fireEvent.click(screen.getByTestId("service-restart-menu"));
    fireEvent.click(screen.getByTestId("service-restart-slaves"));

    const submitButton = screen.getByRole("button", { name: "RESTART" });
    const intervalInput = screen.getByLabelText(
      "Interval between batches (seconds)",
    );
    const toleranceInput = screen.getByLabelText("Task failure tolerance");

    fireEvent.change(intervalInput, { target: { value: "" } });
    expect(submitButton.hasAttribute("disabled")).toBe(true);
    fireEvent.change(intervalInput, { target: { value: "0" } });
    expect(submitButton.hasAttribute("disabled")).toBe(false);

    fireEvent.change(toleranceInput, { target: { value: "" } });
    expect(submitButton.hasAttribute("disabled")).toBe(true);
    fireEvent.change(toleranceInput, { target: { value: "0" } });
    expect(submitButton.hasAttribute("disabled")).toBe(false);
    expect(mocks.actionRequest).not.toHaveBeenCalled();
  });

  it("submits all selected components as a rolling request schedule and locks the action", async () => {
    mocks.actionRequest.mockResolvedValue({
      data: { resources: [{ RequestSchedule: { id: 91 } }] },
    });
    renderActions([], {
      enableNewServiceRestartOptions: true,
      serviceModel: restartableHdfsModel(),
    });
    fireEvent.click(screen.getByRole("button", { name: "ACTIONS" }));
    fireEvent.click(screen.getByTestId("service-restart-menu"));
    fireEvent.click(screen.getByTestId("service-restart-all"));
    fireEvent.click(screen.getByRole("button", { name: "RESTART" }));

    await waitFor(() => expect(mocks.actionRequest).toHaveBeenCalledOnce());
    const payload = JSON.parse(mocks.actionRequest.mock.calls[0][1]);
    const scheduledRequests = payload[0].RequestSchedule.batch[0].requests as Array<{
      RequestBodyInfo: {
        "Requests/resource_filters": Array<{
          service_name: string;
          component_name: string;
          hosts: string;
        }>;
      };
    }>;
    expect(mocks.actionRequest.mock.calls[0][0]).toBe("c1");
    expect(scheduledRequests.map((request) =>
      request.RequestBodyInfo["Requests/resource_filters"][0],
    )).toEqual([
      { service_name: "HDFS", component_name: "NAMENODE", hosts: "nn1" },
      { service_name: "HDFS", component_name: "DATANODE", hosts: "dn1,dn2" },
    ]);
    expect(mocks.fetchBackgroundOperationsSnapshot).toHaveBeenCalledOnce();
    expect(mocks.modalShow).toHaveBeenCalledOnce();

    fireEvent.click(screen.getByRole("button", { name: "ACTIONS" }));
    expect(screen.getByTestId("service-restart-menu").hasAttribute("disabled"))
      .toBe(true);
  });

  it("keeps the schedule lock through polling errors and pauses until terminal", async () => {
    vi.useFakeTimers();
    mocks.actionRequest.mockResolvedValue({
      data: { resources: [{ RequestSchedule: { id: 95 } }] },
    });
    mocks.requestScheduleFetch
      .mockRejectedValueOnce(new Error("temporary polling failure"))
      .mockResolvedValueOnce({ RequestSchedule: { status: "PAUSED" } })
      .mockResolvedValueOnce({ RequestSchedule: { status: "COMPLETED" } });
    renderActions([], {
      enableNewServiceRestartOptions: true,
      serviceModel: restartableHdfsModel(),
    });
    fireEvent.click(screen.getByRole("button", { name: "ACTIONS" }));
    fireEvent.click(screen.getByTestId("service-restart-menu"));
    fireEvent.click(screen.getByTestId("service-restart-all"));

    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "RESTART" }));
      await Promise.resolve();
      await Promise.resolve();
      await Promise.resolve();
    });

    expect(mocks.actionRequest).toHaveBeenCalledOnce();
    fireEvent.click(screen.getByRole("button", { name: "ACTIONS" }));
    const restartMenu = screen.getByTestId("service-restart-menu");
    expect(restartMenu.hasAttribute("disabled")).toBe(true);

    await act(async () => vi.advanceTimersByTimeAsync(5000));
    expect(mocks.requestScheduleFetch).toHaveBeenCalledWith("c1", 95);
    expect(restartMenu.hasAttribute("disabled")).toBe(true);

    await act(async () => vi.advanceTimersByTimeAsync(5000));
    expect(mocks.requestScheduleFetch).toHaveBeenCalledTimes(2);
    expect(restartMenu.hasAttribute("disabled")).toBe(true);

    await act(async () => vi.advanceTimersByTimeAsync(5000));
    expect(mocks.requestScheduleFetch).toHaveBeenCalledTimes(3);
    expect(mocks.fetchBackgroundOperationsSnapshot).toHaveBeenCalledTimes(2);
    expect(restartMenu.hasAttribute("disabled")).toBe(false);
  });

  it("uses only masters for an express restart and locks accepted work", async () => {
    mocks.actionRequestRebalanceHDFS.mockResolvedValue({
      status: 202,
      data: { Requests: { id: 92 } },
    });
    renderActions([], {
      enableNewServiceRestartOptions: true,
      serviceModel: restartableHdfsModel(),
    });
    fireEvent.click(screen.getByRole("button", { name: "ACTIONS" }));
    fireEvent.click(screen.getByTestId("service-restart-menu"));
    fireEvent.click(screen.getByTestId("service-restart-masters"));
    fireEvent.click(screen.getByLabelText("Express"));
    fireEvent.click(screen.getByRole("button", { name: "RESTART" }));

    await waitFor(() =>
      expect(mocks.actionRequestRebalanceHDFS).toHaveBeenCalledOnce(),
    );
    expect(mocks.actionRequestRebalanceHDFS.mock.calls[0][1][
      "Requests/resource_filters"
    ]).toEqual([
      { service_name: "HDFS", component_name: "NAMENODE", hosts: "nn1" },
    ]);
    expect(mocks.fetchBackgroundOperationsSnapshot).toHaveBeenCalledOnce();
    expect(mocks.modalShow).toHaveBeenCalledOnce();

    fireEvent.click(screen.getByRole("button", { name: "ACTIONS" }));
    expect(screen.getByTestId("service-restart-menu").hasAttribute("disabled"))
      .toBe(true);
  });

  it("does not submit an express restart when every selected component is in maintenance", async () => {
    const serviceModel = restartableHdfsModel();
    serviceModel.masterComponents[0].hostComponents[0]
      .HostRoles.maintenance_state = "ON";
    renderActions([], {
      enableNewServiceRestartOptions: true,
      serviceModel,
    });
    fireEvent.click(screen.getByRole("button", { name: "ACTIONS" }));
    fireEvent.click(screen.getByTestId("service-restart-menu"));
    fireEvent.click(screen.getByTestId("service-restart-masters"));
    fireEvent.click(screen.getByLabelText("Express"));
    fireEvent.click(screen.getByRole("button", { name: "RESTART" }));

    expect(await screen.findByText(
      "No components outside maintenance mode are available for this restart.",
    )).not.toBeNull();
    expect(mocks.actionRequestRebalanceHDFS).not.toHaveBeenCalled();
    expect(screen.getByText("Configure Restart HDFS")).not.toBeNull();
  });

  it.each([
    ["null", null],
    ["zero", 0],
  ])("keeps the express dialog retryable for a %s request ID", async (
    _caseName,
    invalidRequestId,
  ) => {
    mocks.actionRequestRebalanceHDFS
      .mockResolvedValueOnce({
        status: 202,
        data: { Requests: { id: invalidRequestId } },
      })
      .mockResolvedValueOnce({
        status: 202,
        data: { Requests: { id: 97 } },
      });
    renderActions([], {
      enableNewServiceRestartOptions: true,
      serviceModel: restartableHdfsModel(),
    });
    fireEvent.click(screen.getByRole("button", { name: "ACTIONS" }));
    fireEvent.click(screen.getByTestId("service-restart-menu"));
    fireEvent.click(screen.getByTestId("service-restart-masters"));
    fireEvent.click(screen.getByLabelText("Express"));
    fireEvent.click(screen.getByRole("button", { name: "RESTART" }));

    expect(await screen.findByText(
      "Ambari did not return an accepted request ID.",
    )).not.toBeNull();
    expect(mocks.modalShow).not.toHaveBeenCalled();
    expect(mocks.fetchBackgroundOperationsSnapshot).not.toHaveBeenCalled();

    const retryButton = screen.getByRole("button", { name: "RESTART" });
    expect(retryButton.hasAttribute("disabled")).toBe(false);
    fireEvent.click(retryButton);

    await waitFor(() =>
      expect(mocks.actionRequestRebalanceHDFS).toHaveBeenCalledTimes(2),
    );
    expect(mocks.modalShow).toHaveBeenCalledOnce();
    expect(screen.queryByText("Configure Restart HDFS")).toBeNull();
  });

  it("keeps the service restart dialog open and permits retry after failure", async () => {
    mocks.actionRequest
      .mockRejectedValueOnce(new Error("schedule failed"))
      .mockResolvedValueOnce({
        data: { resources: [{ RequestSchedule: { id: 93 } }] },
      });
    renderActions([], {
      enableNewServiceRestartOptions: true,
      serviceModel: restartableHdfsModel(),
    });
    fireEvent.click(screen.getByRole("button", { name: "ACTIONS" }));
    fireEvent.click(screen.getByTestId("service-restart-menu"));
    fireEvent.click(screen.getByTestId("service-restart-slaves"));
    fireEvent.click(screen.getByRole("button", { name: "RESTART" }));

    await waitFor(() => expect(screen.getByText("schedule failed")).not.toBeNull());
    expect(mocks.toastError).toHaveBeenCalledOnce();
    const retryButton = screen.getByRole("button", { name: "RESTART" });
    expect(retryButton.hasAttribute("disabled")).toBe(false);
    fireEvent.click(retryButton);

    await waitFor(() => expect(mocks.actionRequest).toHaveBeenCalledTimes(2));
    expect(screen.queryByText("Configure Restart HDFS")).toBeNull();
  });

  it.each([
    ["missing", undefined],
    ["null", null],
    ["zero", 0],
  ])("keeps the restart dialog retryable for a %s schedule ID", async (
    _caseName,
    invalidScheduleId,
  ) => {
    mocks.actionRequest
      .mockResolvedValueOnce({
        data: { resources: [{ RequestSchedule: { id: invalidScheduleId } }] },
      })
      .mockResolvedValueOnce({
        data: { resources: [{ RequestSchedule: { id: 96 } }] },
      });
    renderActions([], {
      enableNewServiceRestartOptions: true,
      serviceModel: restartableHdfsModel(),
    });
    fireEvent.click(screen.getByRole("button", { name: "ACTIONS" }));
    fireEvent.click(screen.getByTestId("service-restart-menu"));
    fireEvent.click(screen.getByTestId("service-restart-slaves"));
    fireEvent.click(screen.getByRole("button", { name: "RESTART" }));

    expect(await screen.findByText(
      "Ambari did not return a request schedule ID.",
    )).not.toBeNull();
    expect(mocks.modalShow).not.toHaveBeenCalled();
    expect(mocks.fetchBackgroundOperationsSnapshot).not.toHaveBeenCalled();

    const retryButton = screen.getByRole("button", { name: "RESTART" });
    expect(retryButton.hasAttribute("disabled")).toBe(false);
    fireEvent.click(retryButton);

    await waitFor(() => expect(mocks.actionRequest).toHaveBeenCalledTimes(2));
    expect(mocks.modalShow).toHaveBeenCalledOnce();
    expect(screen.queryByText("Configure Restart HDFS")).toBeNull();
  });

  it("locks the service restart entry for an active component restart", () => {
    renderActions([
      {
        Requests: {
          id: 94,
          request_context: "_PARSE_.ROLLING-RESTART.DATANODE.1.1",
          request_status: "IN_PROGRESS",
        },
      },
    ], {
      enableNewServiceRestartOptions: true,
      serviceModel: restartableHdfsModel(),
    });
    fireEvent.click(screen.getByRole("button", { name: "ACTIONS" }));

    expect(screen.getByTestId("service-restart-menu").hasAttribute("disabled"))
      .toBe(true);
  });
});

function restartableHdfsModel() {
  return {
    isNameNodeHaEnabled: false,
    masterComponents: [
      {
        componentName: "NAMENODE",
        totalCount: 1,
        startedCount: 1,
        hostComponents: [
          {
            HostRoles: {
              host_name: "nn1",
              component_name: "NAMENODE",
              maintenance_state: "OFF",
            },
          },
        ],
      },
    ],
    slaveComponents: [
      {
        componentName: "DATANODE",
        totalCount: 2,
        startedCount: 2,
        hostComponents: ["dn1", "dn2"].map((hostName) => ({
          HostRoles: {
            host_name: hostName,
            component_name: "DATANODE",
            maintenance_state: "OFF",
          },
        })),
      },
    ],
    clientComponents: [],
  };
}
