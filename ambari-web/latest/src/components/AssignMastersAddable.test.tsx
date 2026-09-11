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
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../store/context";

const apiMocks = vi.hoisted(() => ({
  getHostComponentsDetails: vi.fn(),
  postRecommendations: vi.fn(),
}));

vi.mock("../api/hostsApi", () => ({
  HostsApi: {
    getHostComponentsDetails: apiMocks.getHostComponentsDetails,
  },
}));

vi.mock("../api/assignMastersApi", () => ({
  default: {
    postRecommendations: apiMocks.postRecommendations,
  },
}));

vi.mock("../screens/ClusterWizard/hooks/useHostComponents", () => ({
  default: () => ({
    hostComponents: [{
      ServiceComponentInfo: {
        component_name: "NAMENODE",
        service_name: "HDFS",
      },
      host_components: [{
        HostRoles: {
          component_name: "NAMENODE",
          service_name: "HDFS",
          host_name: "host-a",
          display_name: "NameNode",
        },
      }],
    }],
    serviceComponents: [
      {
        StackServices: { service_name: "HDFS" },
        components: [{
          StackServiceComponents: {
            component_name: "NAMENODE",
            service_name: "HDFS",
            is_master: true,
            cardinality: "1",
          },
        }],
      },
      {
        StackServices: { service_name: "HBASE" },
        components: [{
          StackServiceComponents: {
            component_name: "HBASE_MASTER",
            service_name: "HBASE",
            is_master: true,
            cardinality: "1-3",
            isMasterWithMultipleInstances: true,
          },
        }],
      },
    ],
    isLoading: false,
  }),
}));

import {
  default as AssignMastersAddable,
  MasterAssignmentValidationAlert,
  buildAssignmentRecommendationRequest,
  canRemoveAdditionalMaster,
  recommendationDocumentFromResponse,
  restoreSavedMasterAssignments,
  sortAssignmentHosts,
  validateMasterAssignments,
} from "./AssignMastersAddable";

const hosts = [
  {
    Hosts: {
      host_name: "host-a",
      maintenance_state: "OFF",
    },
  },
  {
    Hosts: {
      host_name: "host-b",
      maintenance_state: "ON",
    },
  },
];

describe("master assignment validation", () => {
  afterEach(() => {
    cleanup();
    apiMocks.getHostComponentsDetails.mockReset();
    apiMocks.postRecommendations.mockReset();
  });

  it("requires at least one assignment", () => {
    expect(validateMasterAssignments([], hosts)).toEqual([
      "At least one master component must be assigned to a host.",
    ]);
  });

  it("reports empty, duplicate, unavailable, and maintenance hosts", () => {
    const errors = validateMasterAssignments(
      [
        {
          component_name: "RESOURCEMANAGER",
          display_name: "ResourceManager",
          selectedHost: "host-a",
        },
        {
          component_name: "RESOURCEMANAGER",
          display_name: "ResourceManager",
          selectedHost: "host-a",
        },
        {
          component_name: "RANGER_ADMIN",
          display_name: "Ranger Admin",
          selectedHost: "",
        },
        {
          component_name: "ZOOKEEPER_SERVER",
          display_name: "ZooKeeper Server",
          selectedHost: "host-b",
        },
        {
          component_name: "HIVE_SERVER",
          display_name: "HiveServer2",
          selectedHost: "removed-host",
        },
      ],
      hosts,
    );

    expect(errors).toEqual([
      "ResourceManager cannot be assigned to host-a more than once.",
      "Ranger Admin must be assigned to a host.",
      "ZooKeeper Server host host-b is in maintenance mode.",
      "HiveServer2 host removed-host is no longer available.",
    ]);
  });

  it("allows different components to share an available host", () => {
    expect(
      validateMasterAssignments(
        [
          { component_name: "NAMENODE", selectedHost: "host-a" },
          { component_name: "RESOURCEMANAGER", selectedHost: "host-a" },
        ],
        hosts,
      ),
    ).toEqual([]);
  });

  it("requires the configured number of additional masters", () => {
    expect(
      validateMasterAssignments(
        [
          {
            component_name: "RANGER_ADMIN",
            selectedHost: "host-a",
            isInstalled: true,
          },
        ],
        hosts,
        { RANGER_ADMIN: 1 },
      ),
    ).toContain("Assign at least 1 additional Ranger Admin instance.");
  });

  it("treats whitespace-only host assignments as empty", () => {
    expect(
      validateMasterAssignments(
        [
          {
            component_name: "RANGER_ADMIN",
            selectedHost: "   ",
          },
        ],
        hosts,
      ),
    ).toEqual(["Ranger Admin must be assigned to a host."]);
  });

  it("does not allow removing an additional master at its configured minimum", () => {
    const current = {
      component_name: "RANGER_ADMIN",
      selectedHost: "host-a",
      isInstalled: true,
    };
    const firstAdditional = {
      component_name: "RANGER_ADMIN",
      selectedHost: "host-b",
      isInstalled: false,
    };
    const secondAdditional = {
      component_name: "RANGER_ADMIN",
      selectedHost: "host-c",
      isInstalled: false,
    };

    expect(
      canRemoveAdditionalMaster(
        [current, firstAdditional],
        firstAdditional,
        { RANGER_ADMIN: 1 },
      ),
    ).toBe(false);
    expect(
      canRemoveAdditionalMaster(
        [current, firstAdditional, secondAdditional],
        secondAdditional,
        { RANGER_ADMIN: 1 },
      ),
    ).toBe(true);
  });

  it("renders every validation error", () => {
    render(
      <MasterAssignmentValidationAlert
        errors={[
          "ResourceManager must be assigned to a host.",
          "Ranger Admin host host-b is in maintenance mode.",
        ]}
      />,
    );

    expect(screen.getByRole("alert")).toBeTruthy();
    expect(
      screen.getByText("ResourceManager must be assigned to a host."),
    ).toBeTruthy();
    expect(
      screen.getByText("Ranger Admin host host-b is in maintenance mode."),
    ).toBeTruthy();
  });

  it("sorts assignment hosts by memory, CPU, then hostname", () => {
    const unsortedHosts = [
      { Hosts: { host_name: "host-z", total_mem: 64, cpu_count: 8 } },
      { Hosts: { host_name: "host-b", total_mem: 128, cpu_count: 8 } },
      { Hosts: { host_name: "host-a", total_mem: 128, cpu_count: 8 } },
      { Hosts: { host_name: "host-c", total_mem: 128, cpu_count: 16 } },
    ];

    expect(
      sortAssignmentHosts(unsortedHosts).map((host) => host.Hosts.host_name),
    ).toEqual(["host-c", "host-a", "host-b", "host-z"]);
    expect(unsortedHosts[0].Hosts.host_name).toBe("host-z");
  });

  it("requires complete Stack Advisor recommendations", () => {
    expect(() => recommendationDocumentFromResponse({ resources: [] })).toThrow(
      "Stack Advisor returned an incomplete host assignment recommendation.",
    );
    expect(() => recommendationDocumentFromResponse({
      resources: [{
        recommendations: {
          blueprint: { host_groups: [{ name: "group-1", components: [] }] },
        },
      }],
    })).toThrow(
      "Stack Advisor returned an incomplete host assignment recommendation.",
    );
    expect(() => recommendationDocumentFromResponse({
      resources: [{
        recommendations: {
          blueprint: {
            host_groups: [{
              name: "group-1",
              components: [{ name: "RANGER_ADMIN" }],
            }],
          },
          blueprint_cluster_binding: {
            host_groups: [{ name: "different-group", hosts: [] }],
          },
        },
      }],
    })).toThrow(
      "Stack Advisor returned an incomplete host assignment recommendation.",
    );
  });

  it("forwards the complete recommendation document to Advisor", () => {
    const recommendations = recommendationDocumentFromResponse({
      resources: [{
        recommendations: {
          blueprint: {
            host_groups: [{
              name: "group-1",
              components: [{ name: "RANGER_ADMIN" }],
            }],
          },
          blueprint_cluster_binding: {
            host_groups: [{
              name: "group-1",
              hosts: [{ fqdn: "host-a" }],
            }],
          },
        },
      }],
    });

    expect(buildAssignmentRecommendationRequest({
      hosts: ["host-a"],
      services: ["RANGER"],
      recommendations,
    })).toEqual({
      recommend: "host_groups",
      hosts: ["host-a"],
      services: ["RANGER"],
      recommendations,
    });
  });

  it("restores persisted placement only by component occurrence", () => {
    const restored = restoreSavedMasterAssignments(
      [
        { component_name: "RANGER_ADMIN", selectedHost: "host-a" },
        { component_name: "RANGER_ADMIN", selectedHost: "host-a" },
        { component_name: "NAMENODE", selectedHost: "host-a" },
      ],
      [{
        host_name: "host-b",
        masterServices: [
          { component: "RANGER_ADMIN", hostName: "host-b", isInstalled: false },
          { component: "RANGER_ADMIN", hostName: "host-c", isInstalled: false },
        ],
      }],
    );

    expect(restored.map((assignment) => assignment.selectedHost)).toEqual([
      "host-b",
      "host-c",
      "host-a",
    ]);
  });

  it("keeps installed topology authoritative while restoring extra fresh rows", () => {
    const restored = restoreSavedMasterAssignments(
      [
        {
          component_name: "NAMENODE",
          selectedHost: "host-a",
          isInstalled: true,
        },
        {
          component_name: "HBASE_MASTER",
          selectedHost: "host-a",
          isInstalled: false,
        },
      ],
      [{
        host_name: "host-b",
        masterServices: [{
          component: "NAMENODE",
          hostName: "host-c",
          isInstalled: false,
        }, {
          component: "HBASE_MASTER",
          hostName: "host-b",
          isInstalled: true,
        }, {
          component: "HBASE_MASTER",
          hostName: "host-c",
          isInstalled: true,
        }],
      }],
      {
        availableHosts: ["host-a", "host-b", "host-c"],
        maxAssignmentsByComponent: (componentName) =>
          componentName === "HBASE_MASTER" ? 3 : 1,
      },
    );

    expect(restored).toEqual([
      expect.objectContaining({
        component_name: "NAMENODE",
        selectedHost: "host-a",
        isInstalled: true,
      }),
      expect.objectContaining({
        component_name: "HBASE_MASTER",
        selectedHost: "host-b",
        isInstalled: false,
      }),
      expect.objectContaining({
        component_name: "HBASE_MASTER",
        selectedHost: "host-c",
        isInstalled: false,
      }),
    ]);
  });

  it("does not append saved rows when only installed rows provide a template", () => {
    const restored = restoreSavedMasterAssignments(
      [{
        component_name: "ZOOKEEPER_SERVER",
        selectedHost: "host-a",
        isInstalled: true,
      }],
      [{
        host_name: "host-a",
        masterServices: [{
          component: "ZOOKEEPER_SERVER",
          hostName: "host-a",
          isInstalled: true,
        }, {
          component: "ZOOKEEPER_SERVER",
          hostName: "host-b",
          isInstalled: false,
        }],
      }],
      {
        availableHosts: ["host-a", "host-b"],
        maxAssignmentsByComponent: () => 3,
      },
    );

    expect(restored).toEqual([
      expect.objectContaining({
        component_name: "ZOOKEEPER_SERVER",
        selectedHost: "host-a",
        isInstalled: true,
      }),
    ]);
  });

  it("runs Add Service HBASE recommendations through the real child and preserves retry state", async () => {
    const recommendationResponse = {
      resources: [{
        recommendations: {
          blueprint: {
            host_groups: [{
              name: "group-1",
              components: [
                { name: "NAMENODE" },
                { name: "HBASE_MASTER" },
              ],
            }],
          },
          blueprint_cluster_binding: {
            host_groups: [{
              name: "group-1",
              hosts: [{ fqdn: "host-a" }],
            }],
          },
        },
      }],
    };
    apiMocks.getHostComponentsDetails.mockResolvedValue({
      items: [
        {
          Hosts: {
            host_name: "host-a",
            maintenance_state: "OFF",
            cpu_count: 4,
            total_mem: 8192,
          },
        },
        {
          Hosts: {
            host_name: "host-b",
            maintenance_state: "OFF",
            cpu_count: 2,
            total_mem: 4096,
          },
        },
        {
          Hosts: {
            host_name: "host-c",
            maintenance_state: "OFF",
            cpu_count: 2,
            total_mem: 4096,
          },
        },
      ],
    });
    apiMocks.postRecommendations
      .mockRejectedValueOnce(new Error("Advisor unavailable"))
      .mockResolvedValue(recommendationResponse);
    const onLoadStateChange = vi.fn();
    const onAssignmentValidationChange = vi.fn();
    const dispatch = vi.fn();
    const runWithAdvisorRequest = vi.fn(async (request: any) =>
      request({
        isCurrent: () => true,
        properties: {
          managed_dependency_plan: {
            consumer: { expected_revision: 17, scope: "SERVICE_PLAN" },
          },
        },
      }),
    );

    const rendered = render(
      <AppContext.Provider
        value={{
          clusterName: "cluster-a",
          cluster: { stack: "HDP", versionNum: "3.1" },
        } as never}
      >
        <AssignMastersAddable
          services={["HDFS", "HBASE"]}
          isInstallFlow
          wizardName="addService"
          servicesData={{
            HDFS: { selected: true, installed: true },
            HBASE: { selected: true, installed: false },
          }}
          dispatch={dispatch}
          validateAssignments
          onLoadStateChange={onLoadStateChange}
          onAssignmentValidationChange={onAssignmentValidationChange}
          runWithAdvisorRequest={runWithAdvisorRequest}
          savedMasters={[{
            host_name: "host-b",
            masterServices: [{
              component: "HBASE_MASTER",
              hostName: "host-b",
              isInstalled: false,
            }, {
              component: "HBASE_MASTER",
              hostName: "host-c",
              isInstalled: false,
            }],
          }]}
          advisorInputKey="input-a"
        />
      </AppContext.Provider>,
    );

    expect((await screen.findByRole("alert")).textContent).toContain(
      "Advisor unavailable",
    );
    await waitFor(() => {
      expect(onLoadStateChange).toHaveBeenLastCalledWith({
        status: "error",
        error: "Advisor unavailable",
      });
    });

    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    await waitFor(() => {
      expect(apiMocks.postRecommendations).toHaveBeenCalledTimes(3);
      expect(onLoadStateChange).toHaveBeenLastCalledWith({ status: "ready" });
    });

    expect(runWithAdvisorRequest).toHaveBeenCalledTimes(2);
    expect(apiMocks.postRecommendations.mock.calls[1][0]).toEqual(
      expect.objectContaining({
        managed_dependency_plan: {
          consumer: { expected_revision: 17, scope: "SERVICE_PLAN" },
        },
      }),
    );
    expect(apiMocks.postRecommendations.mock.calls[2][0]).toEqual(
      expect.objectContaining({
        managed_dependency_plan: {
          consumer: { expected_revision: 17, scope: "SERVICE_PLAN" },
        },
      }),
    );
    expect(onAssignmentValidationChange).toHaveBeenLastCalledWith(true, []);
    expect(dispatch).toHaveBeenCalledWith(
      expect.objectContaining({
        advisorInputKey: "input-a",
        mastersData: expect.arrayContaining([
          expect.objectContaining({
            host_name: "host-a",
            masterServices: expect.arrayContaining([
              expect.objectContaining({
                component: "NAMENODE",
                selectedHost: "host-a",
                isInstalled: true,
              }),
            ]),
          }),
          expect.objectContaining({
            host_name: "host-b",
            masterServices: expect.arrayContaining([
              expect.objectContaining({
                component: "HBASE_MASTER",
                selectedHost: "host-b",
              }),
            ]),
          }),
          expect.objectContaining({
            host_name: "host-c",
            masterServices: expect.arrayContaining([
              expect.objectContaining({
                component: "HBASE_MASTER",
                selectedHost: "host-c",
              }),
            ]),
          }),
        ]),
      }),
    );

    dispatch.mockClear();
    rendered.rerender(
      <AppContext.Provider
        value={{
          clusterName: "cluster-a",
          cluster: { stack: "HDP", versionNum: "3.1" },
        } as never}
      >
        <AssignMastersAddable
          key="same-input-remount"
          services={["HDFS", "HBASE"]}
          isInstallFlow
          wizardName="addService"
          servicesData={{
            HDFS: { selected: true, installed: true },
            HBASE: { selected: true, installed: false },
          }}
          dispatch={dispatch}
          validateAssignments
          onLoadStateChange={onLoadStateChange}
          onAssignmentValidationChange={onAssignmentValidationChange}
          runWithAdvisorRequest={runWithAdvisorRequest}
          savedMasters={[{
            host_name: "host-b",
            masterServices: [{
              component: "HBASE_MASTER",
              hostName: "host-b",
              isInstalled: false,
            }, {
              component: "HBASE_MASTER",
              hostName: "host-c",
              isInstalled: false,
            }],
          }]}
          advisorInputKey="input-a"
        />
      </AppContext.Provider>,
    );
    await waitFor(() => {
      expect(apiMocks.postRecommendations).toHaveBeenCalledTimes(5);
    });
    expect(dispatch).toHaveBeenCalledWith(
      expect.objectContaining({
        advisorInputKey: "input-a",
        mastersData: expect.arrayContaining([
          expect.objectContaining({
            host_name: "host-b",
            masterServices: expect.arrayContaining([
              expect.objectContaining({
                component: "HBASE_MASTER",
                selectedHost: "host-b",
              }),
            ]),
          }),
          expect.objectContaining({
            host_name: "host-c",
            masterServices: expect.arrayContaining([
              expect.objectContaining({
                component: "HBASE_MASTER",
                selectedHost: "host-c",
              }),
            ]),
          }),
        ]),
      }),
    );

    dispatch.mockClear();
    rendered.rerender(
      <AppContext.Provider
        value={{
          clusterName: "cluster-a",
          cluster: { stack: "HDP", versionNum: "3.1" },
        } as never}
      >
        <AssignMastersAddable
          key="changed-input-remount"
          services={["HDFS", "HBASE"]}
          isInstallFlow
          wizardName="addService"
          servicesData={{
            HDFS: { selected: true, installed: true },
            HBASE: { selected: true, installed: false },
          }}
          dispatch={dispatch}
          validateAssignments
          onLoadStateChange={onLoadStateChange}
          onAssignmentValidationChange={onAssignmentValidationChange}
          runWithAdvisorRequest={runWithAdvisorRequest}
          savedMasters={[]}
          advisorInputKey="input-b"
        />
      </AppContext.Provider>,
    );
    await waitFor(() => {
      expect(apiMocks.postRecommendations).toHaveBeenCalledTimes(7);
    });
    expect(dispatch).toHaveBeenCalledWith(
      expect.objectContaining({
        advisorInputKey: "input-b",
        mastersData: expect.arrayContaining([
          expect.objectContaining({
            host_name: "host-a",
            masterServices: expect.arrayContaining([
              expect.objectContaining({
                component: "NAMENODE",
                selectedHost: "host-a",
                isInstalled: true,
              }),
              expect.objectContaining({
                component: "HBASE_MASTER",
                selectedHost: "host-a",
                isInstalled: false,
              }),
            ]),
          }),
        ]),
      }),
    );
  });

  it("reports host loading failures and offers retry", async () => {
    apiMocks.getHostComponentsDetails.mockRejectedValue(
      new Error("Host API unavailable"),
    );
    const onLoadStateChange = vi.fn();

    render(
      <AssignMastersAddable
        services={["RANGER"]}
        dispatch={vi.fn()}
        onLoadStateChange={onLoadStateChange}
      />,
    );

    expect((await screen.findByRole("alert")).textContent).toContain(
      "Host API unavailable",
    );
    await waitFor(() => {
      expect(onLoadStateChange).toHaveBeenLastCalledWith({
        status: "error",
        error: "Host API unavailable",
      });
    });

    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    await waitFor(() => {
      expect(apiMocks.getHostComponentsDetails).toHaveBeenCalledTimes(2);
    });
  });

  it("ignores a delayed recommendation after the advisor input changes", async () => {
    let resolveFirstRecommendation!: (response: unknown) => void;
    const firstRecommendation = new Promise((resolve) => {
      resolveFirstRecommendation = resolve;
    });
    const recommendationResponse = {
      resources: [{
        recommendations: {
          blueprint: {
            host_groups: [{
              name: "group-1",
              components: [
                { name: "NAMENODE" },
                { name: "HBASE_MASTER" },
              ],
            }],
          },
          blueprint_cluster_binding: {
            host_groups: [{
              name: "group-1",
              hosts: [{ fqdn: "host-a" }],
            }],
          },
        },
      }],
    };
    apiMocks.getHostComponentsDetails.mockResolvedValue({
      items: [{
        Hosts: {
          host_name: "host-a",
          maintenance_state: "OFF",
          cpu_count: 4,
          total_mem: 8192,
        },
      }],
    });
    let recommendationCall = 0;
    apiMocks.postRecommendations.mockImplementation(() => {
      recommendationCall += 1;
      return recommendationCall === 1
        ? firstRecommendation
        : Promise.resolve(recommendationResponse);
    });
    const dispatch = vi.fn();
    const onLoadStateChange = vi.fn();

    const rendered = render(
      <AppContext.Provider
        value={{
          clusterName: "cluster-a",
          cluster: { stack: "HDP", versionNum: "3.1" },
        } as never}
      >
        <AssignMastersAddable
          services={["HDFS", "HBASE"]}
          isInstallFlow
          wizardName="addService"
          servicesData={{
            HDFS: { selected: true, installed: true },
            HBASE: { selected: true, installed: false },
          }}
          dispatch={dispatch}
          onLoadStateChange={onLoadStateChange}
          advisorInputKey="input-a"
        />
      </AppContext.Provider>,
    );

    await waitFor(() => {
      expect(apiMocks.postRecommendations).toHaveBeenCalledOnce();
    });
    rendered.rerender(
      <AppContext.Provider
        value={{
          clusterName: "cluster-a",
          cluster: { stack: "HDP", versionNum: "3.1" },
        } as never}
      >
        <AssignMastersAddable
          services={["HDFS", "HBASE"]}
          isInstallFlow
          wizardName="addService"
          servicesData={{
            HDFS: { selected: true, installed: true },
            HBASE: { selected: true, installed: false },
          }}
          dispatch={dispatch}
          onLoadStateChange={onLoadStateChange}
          advisorInputKey="input-b"
        />
      </AppContext.Provider>,
    );

    await act(async () => {
      resolveFirstRecommendation(recommendationResponse);
    });
    await waitFor(() => {
      expect(apiMocks.postRecommendations).toHaveBeenCalledTimes(3);
    });
    expect(dispatch.mock.calls.every(([payload]) => payload.advisorInputKey !== "input-a")).toBe(
      true,
    );
    expect(
      onLoadStateChange.mock.calls.some(([state]) => state.status === "error"),
    ).toBe(false);
  });
});
