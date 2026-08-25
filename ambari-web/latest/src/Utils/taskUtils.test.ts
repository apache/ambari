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

import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  getInstalled: vi.fn(),
  createComponent: vi.fn(),
  register: vi.fn(),
  update: vi.fn(),
  updateService: vi.fn(),
  deleteHostComponent: vi.fn(),
}));

vi.mock("../api/hostsApi", () => ({
  HostsApi: {
    getInstalledHostsForHostComponents: mocks.getInstalled,
    registerHostToComponent: mocks.register,
    updateHostComponents: mocks.update,
    deleteHostComponent: mocks.deleteHostComponent,
  },
}));
vi.mock("../api/serviceApi", () => ({
  ServiceApi: {
    createComponent: mocks.createComponent,
    updateService: mocks.updateService,
  },
}));

import {
  createInstallComponentTask,
  deleteComponent,
  startAllServices,
  startServices,
  stopServices,
} from "./taskUtils";

const serviceModel = {
  masterComponents: [{ componentName: "JOURNALNODE" }],
  clientComponents: [],
  slaveComponents: [],
};

describe("HA component install chain", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getInstalled.mockResolvedValue({ items: [] });
    mocks.register.mockResolvedValue({ status: 201 });
    mocks.update.mockResolvedValue({ status: 202, Requests: { id: 1 } });
    mocks.updateService.mockResolvedValue({ status: 202, Requests: { id: 2 } });
    mocks.deleteHostComponent.mockResolvedValue({ status: 200 });
  });

  it("registers missing host components before installing them", async () => {
    await createInstallComponentTask(
      "JOURNALNODE",
      ["jn1", "jn2"],
      "HDFS",
      "c1",
      ["HDFS"],
      serviceModel,
    );

    expect(mocks.createComponent).not.toHaveBeenCalled();
    expect(mocks.register).toHaveBeenCalledOnce();
    expect(mocks.update).toHaveBeenCalledOnce();
    expect(mocks.register.mock.invocationCallOrder[0]).toBeLessThan(
      mocks.update.mock.invocationCallOrder[0],
    );
  });

  it("stops when host-component registration fails", async () => {
    mocks.register.mockRejectedValue(new Error("registration failed"));

    await expect(
      createInstallComponentTask(
        "JOURNALNODE",
        "jn1",
        "HDFS",
        "c1",
        ["HDFS"],
        serviceModel,
      ),
    ).rejects.toThrow("registration failed");
    expect(mocks.update).not.toHaveBeenCalled();
  });

  it("stops when service-component creation fails", async () => {
    mocks.createComponent.mockRejectedValue(new Error("create failed"));

    await expect(
      createInstallComponentTask(
        "ZKFC",
        "nn1",
        "HDFS",
        "c1",
        ["HDFS"],
        serviceModel,
      ),
    ).rejects.toThrow("create failed");
    expect(mocks.register).not.toHaveBeenCalled();
    expect(mocks.update).not.toHaveBeenCalled();
  });

  it("continues only for an explicit already-exists create response", async () => {
    mocks.createComponent.mockRejectedValue(
      new Error("ResourceAlreadyExists: ZKFC already exists"),
    );

    await createInstallComponentTask(
      "ZKFC",
      "nn1",
      "HDFS",
      "c1",
      ["HDFS"],
      serviceModel,
    );

    expect(mocks.register).toHaveBeenCalledOnce();
    expect(mocks.update).toHaveBeenCalledOnce();
  });

  it("treats an HTTP 409 create response as already existing", async () => {
    mocks.createComponent.mockRejectedValue({
      response: { status: 409, data: {} },
    });

    await createInstallComponentTask(
      "ZKFC",
      "nn1",
      "HDFS",
      "c1",
      ["HDFS"],
      serviceModel,
    );

    expect(mocks.register).toHaveBeenCalledOnce();
    expect(mocks.update).toHaveBeenCalledOnce();
  });

  it("does not reinstall host components that are already present", async () => {
    mocks.getInstalled.mockResolvedValue({
      items: [
        { HostRoles: { host_name: "jn1" } },
        { HostRoles: { host_name: "jn2" } },
      ],
    });

    await expect(
      createInstallComponentTask(
        "JOURNALNODE",
        ["jn1", "jn2"],
        "HDFS",
        "c1",
        ["HDFS"],
        serviceModel,
      ),
    ).resolves.toEqual({ status: 200 });

    expect(mocks.register).not.toHaveBeenCalled();
    expect(mocks.update).not.toHaveBeenCalled();
  });

  it("registers and installs only missing host components", async () => {
    mocks.getInstalled.mockResolvedValue({
      items: [{ HostRoles: { host_name: "jn1" } }],
    });

    await createInstallComponentTask(
      "JOURNALNODE",
      ["jn1", "jn2"],
      "HDFS",
      "c1",
      ["HDFS"],
      serviceModel,
    );

    expect(mocks.register.mock.calls[0][1].RequestInfo.query).toBe(
      "Hosts/host_name=jn2",
    );
    expect(mocks.update.mock.calls[0][2].query).toContain(
      "HostRoles/host_name.in(jn2)",
    );
  });

  it("reconciles every target when all host components already exist", async () => {
    mocks.getInstalled.mockResolvedValue({
      items: [
        { HostRoles: { host_name: "jn1" } },
        { HostRoles: { host_name: "jn2" } },
      ],
    });

    await createInstallComponentTask(
      "JOURNALNODE",
      ["jn1", "jn2"],
      "HDFS",
      "c1",
      ["HDFS"],
      serviceModel,
      undefined,
      { reconcileHosts: true },
    );

    expect(mocks.register).not.toHaveBeenCalled();
    expect(mocks.update.mock.calls[0][2].query).toContain(
      "HostRoles/host_name.in(jn1,jn2)",
    );
  });

  it("registers only missing hosts and reconciles all target hosts", async () => {
    mocks.getInstalled
      .mockResolvedValueOnce({
        items: [{ HostRoles: { host_name: "jn1" } }],
      })
      .mockResolvedValueOnce({
        items: [
          { HostRoles: { host_name: "jn1" } },
          { HostRoles: { host_name: "jn2" } },
        ],
      });

    await createInstallComponentTask(
      "JOURNALNODE",
      ["jn1", "jn2"],
      "HDFS",
      "c1",
      ["HDFS"],
      serviceModel,
      undefined,
      { reconcileHosts: true },
    );

    expect(mocks.register.mock.calls[0][1].RequestInfo.query).toBe(
      "Hosts/host_name=jn2",
    );
    expect(mocks.update.mock.calls[0][2].query).toContain(
      "HostRoles/host_name.in(jn1,jn2)",
    );
  });

  it("reconciles mixed Router targets in install order while excluding maintenance mode", async () => {
    mocks.getInstalled
      .mockResolvedValueOnce({
        items: [
          { HostRoles: { host_name: "router-existing" } },
          { HostRoles: { host_name: "router-maintenance" } },
        ],
      })
      .mockResolvedValueOnce({
        items: [
          { HostRoles: { host_name: "router-existing" } },
          { HostRoles: { host_name: "router-new" } },
          { HostRoles: { host_name: "router-maintenance" } },
        ],
      });

    await createInstallComponentTask(
      "ROUTER",
      ["router-existing", "router-new", "router-maintenance"],
      "HDFS",
      "c1",
      ["HDFS"],
      {
        masterComponents: [{ componentName: "ROUTER" }],
        clientComponents: [],
        slaveComponents: [],
      },
      undefined,
      { reconcileHosts: true },
    );

    expect(mocks.register.mock.calls[0][1].RequestInfo.query).toBe(
      "Hosts/host_name=router-new",
    );
    expect(mocks.update.mock.calls[0][2]).toMatchObject({
      HostRoles: { state: "INSTALLED" },
      query:
        "HostRoles/component_name=ROUTER" +
        "&HostRoles/host_name.in(router-existing,router-new,router-maintenance)" +
        "&HostRoles/maintenance_state=OFF",
    });
    expect(mocks.register.mock.invocationCallOrder[0]).toBeLessThan(
      mocks.update.mock.invocationCallOrder[0],
    );
  });

  it("rechecks topology after service-component creation fails", async () => {
    mocks.createComponent.mockRejectedValue(new Error("create response lost"));
    mocks.getInstalled
      .mockResolvedValueOnce({ items: [] })
      .mockResolvedValueOnce({
        items: [{ HostRoles: { host_name: "nn1" } }],
      });

    await createInstallComponentTask(
      "ZKFC",
      "nn1",
      "HDFS",
      "c1",
      ["HDFS"],
      serviceModel,
      undefined,
      { reconcileHosts: true },
    );

    expect(mocks.getInstalled).toHaveBeenCalledTimes(2);
    expect(mocks.register).not.toHaveBeenCalled();
    expect(mocks.update).toHaveBeenCalledOnce();
  });

  it("registers missing hosts after a service-component create response is lost", async () => {
    mocks.createComponent.mockRejectedValue(new Error("create response lost"));
    mocks.getInstalled
      .mockResolvedValueOnce({ items: [] })
      .mockResolvedValueOnce({ items: [] })
      .mockResolvedValueOnce({
        items: [{ HostRoles: { host_name: "nn1" } }],
      });

    await createInstallComponentTask(
      "ZKFC",
      "nn1",
      "HDFS",
      "c1",
      ["HDFS"],
      serviceModel,
      undefined,
      { reconcileHosts: true },
    );

    expect(mocks.getInstalled).toHaveBeenCalledTimes(3);
    expect(mocks.register).toHaveBeenCalledOnce();
    expect(mocks.update).toHaveBeenCalledOnce();
  });

  it("rechecks topology after registration fails", async () => {
    mocks.register.mockRejectedValue(new Error("registration response lost"));
    mocks.getInstalled
      .mockResolvedValueOnce({ items: [] })
      .mockResolvedValueOnce({
        items: [{ HostRoles: { host_name: "jn1" } }],
      });

    await createInstallComponentTask(
      "JOURNALNODE",
      "jn1",
      "HDFS",
      "c1",
      ["HDFS"],
      serviceModel,
      undefined,
      { reconcileHosts: true },
    );

    expect(mocks.getInstalled).toHaveBeenCalledTimes(2);
    expect(mocks.update).toHaveBeenCalledOnce();
  });

  it("keeps registration failures fatal when targets are still missing", async () => {
    mocks.register.mockRejectedValue(new Error("registration failed"));
    mocks.getInstalled.mockResolvedValue({ items: [] });

    await expect(
      createInstallComponentTask(
        "JOURNALNODE",
        "jn1",
        "HDFS",
        "c1",
        ["HDFS"],
        serviceModel,
        undefined,
        { reconcileHosts: true },
      ),
    ).rejects.toThrow("registration failed");
    expect(mocks.getInstalled).toHaveBeenCalledTimes(2);
    expect(mocks.update).not.toHaveBeenCalled();
  });

  it("treats an empty host target as an idempotent no-op", async () => {
    await expect(
      createInstallComponentTask(
        "JOURNALNODE",
        [],
        "HDFS",
        "c1",
        ["HDFS"],
        serviceModel,
        undefined,
        { reconcileHosts: true },
      ),
    ).resolves.toEqual({ status: 200 });

    expect(mocks.getInstalled).not.toHaveBeenCalled();
    expect(mocks.register).not.toHaveBeenCalled();
    expect(mocks.update).not.toHaveBeenCalled();
  });

  it("treats a missing JournalNode as an idempotent delete", async () => {
    mocks.update.mockRejectedValue({
      response: { status: 404, data: { message: "NoSuchResourceException" } },
    });

    await expect(
      deleteComponent("c1", "JOURNALNODE", "jn1", "HDFS", true),
    ).resolves.toEqual({ status: 200 });
    expect(mocks.deleteHostComponent).not.toHaveBeenCalled();
  });

  it("keeps missing-component errors fatal unless explicitly tolerated", async () => {
    const missing = new Error("NoSuchResourceException");
    mocks.update.mockRejectedValue(missing);
    const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});

    await expect(
      deleteComponent("c1", "SECONDARY_NAMENODE", "nn1", "HDFS"),
    ).rejects.toBe(missing);
    consoleError.mockRestore();
  });
});

describe("service state helpers", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.updateService.mockResolvedValue({ status: 202, Requests: { id: 2 } });
  });

  it("stops every service except the excluded services", async () => {
    await stopServices(
      "c1",
      ["HDFS"],
      false,
      false,
      ["HDFS", "YARN", "ZOOKEEPER"],
    );

    expect(mocks.updateService).toHaveBeenCalledWith(
      "c1",
      expect.objectContaining({ context: "Stop required services" }),
      "ServiceInfo/service_name.in(YARN,ZOOKEEPER)",
    );
  });

  it("stops all services without adding an undefined query", async () => {
    await stopServices("c1", [], true, true, ["HDFS", "YARN"]);

    expect(mocks.updateService).toHaveBeenCalledWith(
      "c1",
      expect.objectContaining({ context: "Stop all services" }),
      "",
    );
  });

  it("does not send an empty service predicate after exclusions", async () => {
    await expect(
      stopServices("c1", ["HDFS"], false, false, ["HDFS"]),
    ).resolves.toEqual({ status: 200 });
    expect(mocks.updateService).not.toHaveBeenCalled();
  });

  it("starts selected services and passes the smoke-test query", async () => {
    await startServices("c1", true, ["YARN"], true, false);

    expect(mocks.updateService).toHaveBeenCalledWith(
      "c1",
      expect.objectContaining({ context: "Start required services" }),
      "ServiceInfo/service_name.in(YARN)&params/run_smoke_test=true",
    );
  });

  it("starts every service except the excluded services", async () => {
    await startServices(
      "c1",
      false,
      ["HDFS"],
      false,
      false,
      ["HDFS", "YARN", "ZOOKEEPER"],
    );

    expect(mocks.updateService).toHaveBeenCalledWith(
      "c1",
      expect.objectContaining({ context: "Start required services" }),
      "ServiceInfo/service_name.in(YARN,ZOOKEEPER)",
    );
  });

  it("does not send an empty start-service predicate after exclusions", async () => {
    await expect(
      startServices("c1", false, ["HDFS"], false, false, ["HDFS"]),
    ).resolves.toEqual({ status: 200 });
    expect(mocks.updateService).not.toHaveBeenCalled();
  });

  it("honors skip.service.checks when starting all services", async () => {
    await startAllServices("c1", {
      runSmokeTest: true,
      skipServiceChecks: true,
    });

    expect(mocks.updateService).toHaveBeenCalledWith(
      "c1",
      expect.objectContaining({ context: "Start all services" }),
      "params/run_smoke_test=false",
    );
  });
});
