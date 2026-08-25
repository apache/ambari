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

const renderConnection = ({
  configs = configProperties,
  requiredProperties,
  ambariProperties = {},
  installed = true,
  disabled = false,
}: {
  configs?: ConfigPropertiesType;
  requiredProperties?: Record<string, string>;
  ambariProperties?: Record<string, unknown>;
  installed?: boolean;
  disabled?: boolean;
} = {}) =>
  render(
    <AppContext.Provider
      value={
        {
          clusterName: "c1",
          services: installed
            ? [{ ServiceInfo: { service_name: "HIVE" } }]
            : [],
          ambariProperties,
        } as unknown as ComponentProps<typeof AppContext.Provider>["value"]
      }
    >
      <TestConnection
        buttonLabel="Test Connection"
        serviceName="HIVE"
        configProperties={configs}
        requiredProperties={requiredProperties}
        disabled={disabled}
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

  it("redacts configured passwords from task diagnostics before rendering", async () => {
    const configs = structuredClone(configProperties);
    configs.HIVE["hive-site"].properties[
      "javax.jdo.option.ConnectionPassword"
    ].value = "theme-secret";
    mocks.createClusterCustomAction.mockResolvedValue({
      Requests: { id: "17", status: "Accepted" },
    });
    mocks.getTaskId.mockResolvedValue({
      items: [{ Tasks: { id: "3" } }],
    });
    mocks.getTaskStatus.mockResolvedValue({
      Tasks: {
        status: "FAILED",
        error_log: "password=server-secret",
        stderr: "Login failed for theme-secret",
        output_log: "credential: unknown-secret",
        stdout: 'user_passwd="theme-secret"',
      },
    });
    renderConnection({ configs });

    fireEvent.click(
      await screen.findByRole("button", { name: "Test Connection" }),
    );
    await waitFor(() => expect(mocks.resumePolling).toHaveBeenCalled());
    await act(async () => {
      await mocks.pollCallback?.();
    });
    fireEvent.click(await screen.findByText("Connection Failed"));

    expect(await screen.findAllByText(/\[REDACTED\]/)).not.toHaveLength(0);
    expect(document.body.textContent).not.toContain("theme-secret");
    expect(document.body.textContent).not.toContain("server-secret");
    expect(document.body.textContent).not.toContain("unknown-secret");
  });

  it("does not run a UI-only connection action when the consumer is read-only", async () => {
    renderConnection({ disabled: true });
    const button = await screen.findByRole("button", {
      name: "Test Connection",
    });

    expect((button as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(button);
    expect(mocks.createClusterCustomAction).not.toHaveBeenCalled();
  });

  it("reports a completed zero-exit DB check as success", async () => {
    mocks.createClusterCustomAction.mockResolvedValue({
      Requests: { id: "17", status: "Accepted" },
    });
    mocks.getTaskId.mockResolvedValue({
      items: [{ Tasks: { id: "3" } }],
    });
    mocks.getTaskStatus.mockResolvedValue({
      Tasks: {
        status: "COMPLETED",
        structured_out: {
          db_connection_check: { exit_code: 0, message: "Connected" },
        },
      },
    });
    renderConnection();

    fireEvent.click(
      await screen.findByRole("button", { name: "Test Connection" }),
    );
    await waitFor(() => expect(mocks.resumePolling).toHaveBeenCalled());
    await act(async () => {
      await mocks.pollCallback?.();
    });

    expect(await screen.findByText("Connection OK")).toBeTruthy();
    expect(screen.queryByText("Connection Failed")).toBeNull();
    expect(mocks.pausePolling).toHaveBeenCalled();
  });

  it.each(["FAILED", "ABORTED", "TIMEDOUT"])(
    "recovers from the terminal task status %s",
    async (status) => {
      mocks.createClusterCustomAction.mockResolvedValue({
        Requests: { id: "17", status: "Accepted" },
      });
      mocks.getTaskId.mockResolvedValue({
        items: [{ Tasks: { id: "3" } }],
      });
      mocks.getTaskStatus.mockResolvedValue({ Tasks: { status } });
      renderConnection();
      const button = await screen.findByRole("button", {
        name: "Test Connection",
      });

      fireEvent.click(button);
      await waitFor(() => expect(mocks.resumePolling).toHaveBeenCalled());
      await act(async () => {
        await mocks.pollCallback?.();
      });

      expect(await screen.findByText("Connection Failed")).toBeTruthy();
      expect((button as HTMLButtonElement).disabled).toBe(false);
      expect(mocks.pausePolling).toHaveBeenCalled();
    },
  );

  it.each([
    [
      "request ID",
      { Requests: { status: "Accepted" } },
      { items: [{ Tasks: { id: "3" } }] },
    ],
    [
      "task ID",
      { Requests: { id: "17", status: "Accepted" } },
      { items: [] },
    ],
  ])("recovers when an accepted action omits its %s", async (_label, action, tasks) => {
    mocks.createClusterCustomAction.mockResolvedValue(action);
    mocks.getTaskId.mockResolvedValue(tasks);
    renderConnection();
    const button = await screen.findByRole("button", {
      name: "Test Connection",
    });

    fireEvent.click(button);

    expect(await screen.findByText("Connection Failed")).toBeTruthy();
    expect((button as HTMLButtonElement).disabled).toBe(false);
    expect(mocks.pausePolling).toHaveBeenCalled();
  });

  it("builds the custom action payload from Theme required-properties", async () => {
    const configs = structuredClone(configProperties);
    configs.HIVE["hive-site"].properties[
      "javax.jdo.option.ConnectionPassword"
    ].value = "theme-secret";
    mocks.createClusterCustomAction.mockResolvedValue({
      Requests: { status: "Rejected" },
    });
    renderConnection({
      configs,
      requiredProperties: {
        "jdbc.driver.class":
          "hive-site/javax.jdo.option.ConnectionDriverName",
        "jdbc.driver.url": "hive-site/javax.jdo.option.ConnectionURL",
        "db.connection.source.host":
          "HIVE_METASTORE/hive_metastore_hosts",
        "db.type": "hive-env/hive_database_type",
        "db.connection.user":
          "hive-site/javax.jdo.option.ConnectionUserName",
        "db.connection.password":
          "hive-site/javax.jdo.option.ConnectionPassword",
      },
      ambariProperties: {
        "java.home": "/opt/java",
        jdk_location: "/var/lib/ambari-server/resources",
        "jdk.name": "jdk.tar.gz",
      },
    });
    const button = await screen.findByRole("button", {
      name: "Test Connection",
    });
    await waitFor(() =>
      expect((button as HTMLButtonElement).disabled).toBe(false),
    );

    fireEvent.click(button);
    await waitFor(() =>
      expect(mocks.createClusterCustomAction).toHaveBeenCalledWith(
        "c1",
        {
          RequestInfo: {
            action: "check_host",
            context: "Check host",
            parameters: {
              user_name: "hive",
              user_passwd: "theme-secret",
              db_connection_url: "jdbc:test",
              db_name: "postgres",
              ambari_server_host: window.location.hostname,
              java_home: "/opt/java",
              jdk_location: "/var/lib/ambari-server/resources",
              jdk_name: "jdk.tar.gz",
              check_execute_list: "db_connection_check",
              threshold: "60",
            },
          },
          "Requests/resource_filters": [{ hosts: "host1" }],
        },
      ),
    );
  });

  it("uses the clusterless custom-action API before the service is installed", async () => {
    const configs = structuredClone(configProperties);
    configs.HIVE["hive-site"].properties[
      "javax.jdo.option.ConnectionPassword"
    ].value = "not-logged-secret";
    mocks.createCustomAction.mockResolvedValue({
      Requests: { status: "Rejected" },
    });
    renderConnection({ configs, installed: false });
    const button = await screen.findByRole("button", {
      name: "Test Connection",
    });

    fireEvent.click(button);
    await waitFor(() => expect(mocks.createCustomAction).toHaveBeenCalled());
    expect(mocks.createClusterCustomAction).not.toHaveBeenCalled();
    expect(mocks.createCustomAction.mock.calls[0][0]).toMatchObject({
      RequestInfo: {
        action: "check_host",
        parameters: { user_passwd: "not-logged-secret" },
      },
      "Requests/resource_filters": [{ hosts: "host1" }],
    });
  });
});
