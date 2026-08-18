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
  },
}));

import { createInstallComponentTask, deleteComponent } from "./taskUtils";

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
