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
import { afterEach, describe, expect, it, vi } from "vitest";

const apiMocks = vi.hoisted(() => ({
  getHostComponentsDetails: vi.fn(),
}));

vi.mock("../api/hostsApi", () => ({
  HostsApi: {
    getHostComponentsDetails: apiMocks.getHostComponentsDetails,
  },
}));

vi.mock("../screens/ClusterWizard/hooks/useHostComponents", () => ({
  default: () => ({
    hostComponents: [{ ServiceComponentInfo: { component_name: "RANGER_ADMIN" } }],
    serviceComponents: [],
    isLoading: false,
  }),
}));

import {
  default as AssignMastersAddable,
  MasterAssignmentValidationAlert,
  buildAssignmentRecommendationRequest,
  canRemoveAdditionalMaster,
  recommendationDocumentFromResponse,
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
});
