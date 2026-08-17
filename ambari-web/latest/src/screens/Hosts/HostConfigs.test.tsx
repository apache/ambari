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

import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { AppContext } from "../../store/context";
import { ServiceContext } from "../../store/ServiceContext";

const mocks = vi.hoisted(() => ({
  getConfigGroupsForServices: vi.fn(),
  getConfigValues: vi.fn(),
  getHostData: vi.fn(),
  getStackConfigurations: vi.fn(),
  getStackThemes: vi.fn(),
  hasAuthorization: vi.fn(),
  updateConfigGroup: vi.fn(),
}));

vi.mock("../../api/configGroupApi", () => ({
  default: {
    getConfigGroupsForServices: mocks.getConfigGroupsForServices,
    updateConfigGroup: mocks.updateConfigGroup,
  },
}));
vi.mock("../../api/configsApi", () => ({
  default: { getConfigValues: mocks.getConfigValues },
}));
vi.mock("../../api/hostsApi", () => ({
  HostsApi: { getHostData: mocks.getHostData },
}));
vi.mock("../../api/wizardApi", () => ({
  default: {
    getStackConfigurations: mocks.getStackConfigurations,
    getStackThemes: mocks.getStackThemes,
  },
}));
vi.mock("../../hooks/useAuth", () => ({
  useAuth: () => ({ hasAuthorization: mocks.hasAuthorization }),
}));
vi.mock("../../components/Spinner", () => ({
  default: () => <div>Loading host configurations</div>,
}));
vi.mock("../../components/Modal", () => ({
  default: ({ isOpen, modalBody, successCallback }: any) => isOpen ? (
    <div>
      {modalBody}
      <button onClick={successCallback}>Confirm group change</button>
    </div>
  ) : null,
}));
vi.mock("../CommonConfigs/Config", () => ({
  default: ({ configGroup, hostConfigs, onServiceChange, servicesList }: any) => (
    <div>
      <div data-testid="config-probe">
        {hostConfigs ? "Read-only" : "Editable"} group: {configGroup}
      </div>
      {servicesList.map((serviceName: string) => (
        <button key={serviceName} onClick={() => onServiceChange(serviceName)}>
          Show {serviceName}
        </button>
      ))}
    </div>
  ),
}));

import HostConfigs from "./HostConfigs";

const services = [
  { ServiceInfo: { service_name: "HDFS" } },
  { ServiceInfo: { service_name: "YARN" } },
];

const configGroups = {
  items: [
    {
      ConfigGroup: {
        id: 1,
        group_name: "HDFS Blue",
        service_name: "HDFS",
        tag: "HDFS",
        hosts: [{ host_name: "host1" }],
        desired_configs: [],
      },
    },
    {
      ConfigGroup: {
        id: 2,
        group_name: "YARN Green",
        service_name: "YARN",
        tag: "YARN",
        hosts: [{ host_name: "host2" }],
        desired_configs: [],
      },
    },
    {
      ConfigGroup: {
        id: 3,
        group_name: "HDFS Green",
        service_name: "HDFS",
        tag: "HDFS",
        hosts: [{ host_name: "host3" }],
        desired_configs: [],
      },
    },
  ],
};

function renderHostConfigs() {
  return render(
    <MemoryRouter initialEntries={["/hosts/host1/configs"]}>
      <AppContext.Provider value={{
        clusterName: "cluster1",
        cluster: { stack: "HDP", versionNum: "3.1" },
        services,
      } as any}>
        <ServiceContext.Provider value={{ allServiceModels: {} } as any}>
          <Routes>
            <Route path="/hosts/:hostname/configs" element={<HostConfigs />} />
          </Routes>
        </ServiceContext.Provider>
      </AppContext.Provider>
    </MemoryRouter>,
  );
}

describe("Host Configs", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.hasAuthorization.mockReturnValue(true);
    mocks.getHostData.mockResolvedValue({
      host_components: [
        { HostRoles: { service_name: "HDFS", component_name: "DATANODE" } },
        { HostRoles: { service_name: "YARN", component_name: "NODEMANAGER" } },
      ],
    });
    mocks.getStackConfigurations.mockResolvedValue({ items: [] });
    mocks.getStackThemes.mockResolvedValue({ items: [] });
    mocks.getConfigGroupsForServices.mockResolvedValue(configGroups);
    mocks.getConfigValues.mockResolvedValue({ items: [] });
  });

  afterEach(() => cleanup());

  it("bootstraps once and restores a service-specific assigned group", async () => {
    renderHostConfigs();

    expect(await screen.findByText("Read-only group: HDFS Blue")).toBeTruthy();
    expect(mocks.getConfigGroupsForServices).toHaveBeenCalledWith(
      "cluster1",
      ["HDFS", "YARN"],
    );
    fireEvent.click(screen.getByRole("button", { name: "Show YARN" }));
    expect(await screen.findByText("Read-only group: Default")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Change" }));
    expect(screen.getByRole("option", { name: "YARN Green" })).toBeTruthy();
  });

  it("does not expose config-group reassignment without authorization", async () => {
    mocks.hasAuthorization.mockReturnValue(false);
    renderHostConfigs();

    expect(await screen.findByText("Read-only group: HDFS Blue")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Change" })).toBeNull();
  });

  it("recovers from a failed bootstrap request", async () => {
    mocks.getHostData
      .mockRejectedValueOnce({ response: { data: { message: "Host lookup failed" } } })
      .mockResolvedValueOnce({
        host_components: [
          { HostRoles: { service_name: "HDFS", component_name: "DATANODE" } },
          { HostRoles: { service_name: "YARN", component_name: "NODEMANAGER" } },
        ],
      });
    renderHostConfigs();

    expect(await screen.findByText("Host lookup failed")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    await waitFor(() => expect(mocks.getHostData).toHaveBeenCalledTimes(2));
    expect(await screen.findByText("Read-only group: HDFS Blue")).toBeTruthy();
  });

  it("serializes both writes when moving between non-default groups", async () => {
    let completeSourceUpdate: () => void = () => undefined;
    mocks.updateConfigGroup
      .mockReturnValueOnce(new Promise<void>((resolve) => {
        completeSourceUpdate = resolve;
      }))
      .mockResolvedValueOnce({});
    renderHostConfigs();

    expect(await screen.findByText("Read-only group: HDFS Blue")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Change" }));
    fireEvent.change(screen.getByRole("combobox", { name: "Group" }), {
      target: { value: "HDFS Green" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Confirm group change" }));

    await waitFor(() => expect(mocks.updateConfigGroup).toHaveBeenCalledTimes(1));
    expect(mocks.updateConfigGroup).toHaveBeenNthCalledWith(
      1,
      "cluster1",
      "1",
      expect.any(Array),
    );
    completeSourceUpdate();
    await waitFor(() => expect(mocks.updateConfigGroup).toHaveBeenCalledTimes(2));
    expect(mocks.updateConfigGroup).toHaveBeenNthCalledWith(
      2,
      "cluster1",
      "3",
      expect.any(Array),
    );
    expect(await screen.findByText("Read-only group: HDFS Green")).toBeTruthy();
  });
});
