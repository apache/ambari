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
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ComponentProps } from "react";
import { ConfigPropertiesType } from "./types";

const mocks = vi.hoisted(() => ({
  createClusterCustomAction: vi.fn(),
  createCustomAction: vi.fn(),
  getTaskId: vi.fn(),
  getTaskStatus: vi.fn(),
  pausePolling: vi.fn(),
  resumePolling: vi.fn(),
  pollCallback: undefined as undefined | (() => Promise<void>),
  usePolling: vi.fn(),
}));
vi.mock("../../api/clusterApi", () => ({
  default: {
    createClusterCustomAction: mocks.createClusterCustomAction,
    createCustomAction: mocks.createCustomAction,
  },
}));
vi.mock("../../api/requestApi", () => ({
  RequestApi: {
    getTaskId: mocks.getTaskId,
    getTaskStatus: mocks.getTaskStatus,
  },
}));
vi.mock("../../api/kerberosApi", () => ({
  default: { testKdcConnection: vi.fn() },
}));
vi.mock("../../hooks/usePolling", () => ({
  default: mocks.usePolling,
}));

import TestConnection from "./TestConnection";
import { AppContext } from "../../store/context";

const configProperty = (name: string, value: string | string[]) => ({
  propertyName: name,
  propertyDisplayname: name,
  propertyValue: value,
  propertyAttributes: {},
  previousValue: value,
  value,
  final: "false",
  isEditable: true,
});

const configProperties: ConfigPropertiesType = {
  HIVE: {
    "hive-site": {
      errors: 0,
      properties: {
        "ambari.hive.db.schema.name": configProperty(
          "ambari.hive.db.schema.name",
          "hive"
        ),
        "javax.jdo.option.ConnectionUserName": configProperty(
          "javax.jdo.option.ConnectionUserName",
          "hive"
        ),
        "javax.jdo.option.ConnectionPassword": configProperty(
          "javax.jdo.option.ConnectionPassword",
          ""
        ),
        "javax.jdo.option.ConnectionDriverName": configProperty(
          "javax.jdo.option.ConnectionDriverName",
          "driver"
        ),
        "javax.jdo.option.ConnectionURL": configProperty(
          "javax.jdo.option.ConnectionURL",
          "jdbc:test"
        ),
      },
    },
    "hive-env": {
      errors: 0,
      properties: {
        hive_database_type: configProperty(
          "hive_database_type",
          "POSTGRES"
        ),
      },
    },
    HIVE_METASTORE: {
      errors: 0,
      properties: {
        hive_metastore_hosts: configProperty(
          "hive_metastore_hosts",
          ["host1"]
        ),
      },
    },
  },
};

const renderConnection = () =>
  render(
    <AppContext.Provider
      value={
        {
          clusterName: "c1",
          services: [{ ServiceInfo: { service_name: "HIVE" } }],
          ambariProperties: {},
        } as unknown as ComponentProps<typeof AppContext.Provider>["value"]
      }
    >
      <TestConnection
        buttonLabel="Test Connection"
        serviceName="HIVE"
        configProperties={configProperties}
      />
    </AppContext.Provider>
  );

describe("database test connection recovery", () => {
  afterEach(cleanup);

  beforeEach(() => {
    vi.clearAllMocks();
    mocks.pollCallback = undefined;
    mocks.usePolling.mockImplementation((callback) => {
      mocks.pollCallback = callback;
      return {
        pausePolling: mocks.pausePolling,
        resumePolling: mocks.resumePolling,
      };
    });
  });

  it("leaves Connecting and permits retry when request creation fails", async () => {
    mocks.createClusterCustomAction.mockRejectedValue(
      new Error("request unavailable")
    );
    renderConnection();
    const button = await screen.findByRole("button", {
      name: "Test Connection",
    });

    fireEvent.click(button);
    await screen.findByText("Connection Failed");
    expect((button as HTMLButtonElement).disabled).toBe(false);
    expect(mocks.pausePolling).toHaveBeenCalled();

    fireEvent.click(button);
    await waitFor(() =>
      expect(mocks.createClusterCustomAction).toHaveBeenCalledTimes(2)
    );
  });

  it("stops polling and permits retry when task polling fails", async () => {
    mocks.createClusterCustomAction.mockResolvedValue({
      Requests: { id: "17", status: "Accepted" },
    });
    mocks.getTaskId.mockResolvedValue({
      items: [{ Tasks: { id: "3" } }],
    });
    mocks.getTaskStatus.mockRejectedValue(new Error("poll unavailable"));
    renderConnection();
    const button = await screen.findByRole("button", {
      name: "Test Connection",
    });

    fireEvent.click(button);
    await waitFor(() => expect(mocks.resumePolling).toHaveBeenCalled());
    await act(async () => {
      await mocks.pollCallback?.();
    });

    await screen.findByText("Connection Failed");
    expect((button as HTMLButtonElement).disabled).toBe(false);
    expect(mocks.pausePolling).toHaveBeenCalled();
  });

  it("reports a completed task with a failed DB check as failure", async () => {
    mocks.createClusterCustomAction.mockResolvedValue({
      Requests: { id: "17", status: "Accepted" },
    });
    mocks.getTaskId.mockResolvedValue({
      items: [{ Tasks: { id: "3" } }],
    });
    mocks.getTaskStatus.mockResolvedValue({
      Tasks: {
        status: "COMPLETED",
        stderr: "database rejected the connection",
        stdout: "",
        structured_out: {
          db_connection_check: {
            exit_code: 1,
            message: "Invalid database credentials",
          },
        },
      },
    });
    renderConnection();
    const button = await screen.findByRole("button", {
      name: "Test Connection",
    });

    fireEvent.click(button);
    await waitFor(() => expect(mocks.resumePolling).toHaveBeenCalled());
    await act(async () => {
      await mocks.pollCallback?.();
    });

    await screen.findByText("Connection Failed");
    expect((button as HTMLButtonElement).disabled).toBe(false);
    expect(mocks.pausePolling).toHaveBeenCalled();
  });
});
