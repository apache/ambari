/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { ManagedDependencyAdvisorPlan } from "../api/serviceDependenciesApi";

const mocks = vi.hoisted(() => ({
  getCpuInfo: vi.fn(),
  getServices: vi.fn(),
  postRecommendations: vi.fn(),
  postValidations: vi.fn(),
}));

vi.mock("../api/assignMastersApi", () => ({
  default: {
    getCpuInfo: mocks.getCpuInfo,
    postRecommendations: mocks.postRecommendations,
    postValidations: mocks.postValidations,
  },
}));
vi.mock("../api/chooseServicesApi", () => ({
  ChooseServicesApi: { getServices: mocks.getServices },
}));

import AssignMasters from "./AssignMasters";

const managedPlan: ManagedDependencyAdvisorPlan = {
  consumer: {
    scope: "DRAFT",
    draft_id: "11111111-1111-4111-8111-111111111111",
    expected_revision: 8,
  },
  selections: [
    {
      binding_id: "22222222-2222-4222-8222-222222222222",
      dependency_type: "HDFS",
      expected_consumer_descriptor_fingerprint: "consumer-hdfs",
      expected_provider_fingerprint: "provider-hdfs",
      expected_snapshot_fingerprint: "snapshot-hdfs",
      preview_schema_version: 2,
      provider: { cluster_id: 31, service_name: "HDFS" },
    },
    {
      binding_id: "33333333-3333-4333-8333-333333333333",
      dependency_type: "ZOOKEEPER",
      expected_consumer_descriptor_fingerprint: "consumer-zk",
      expected_provider_fingerprint: "provider-zk",
      expected_snapshot_fingerprint: "snapshot-zk",
      preview_schema_version: 2,
      provider: { cluster_id: 32, service_name: "ZOOKEEPER" },
    },
  ],
};

const recommendation = {
  resources: [{
    recommendations: {
      blueprint: {
        host_groups: [{
          components: [{ name: "HBASE_MASTER" }],
          name: "host-group-1",
        }],
      },
      blueprint_cluster_binding: {
        host_groups: [{
          hosts: [{ fqdn: "host1.example.com" }],
          name: "host-group-1",
        }],
      },
    },
  }],
};

const twoHostRecommendation = {
  resources: [{
    recommendations: {
      blueprint: {
        host_groups: [
          { components: [{ name: "HBASE_MASTER" }], name: "host-group-1" },
          { components: [], name: "host-group-2" },
        ],
      },
      blueprint_cluster_binding: {
        host_groups: [
          { hosts: [{ fqdn: "host1.example.com" }], name: "host-group-1" },
          { hosts: [{ fqdn: "host2.example.com" }], name: "host-group-2" },
        ],
      },
    },
  }],
};

const servicesMetadata = {
  items: [{
    StackServices: { service_name: "HBASE" },
    components: [{
      StackServiceComponents: {
        cardinality: "1",
        component_category: "MASTER",
        component_name: "HBASE_MASTER",
        component_type: null,
        display_name: "HBase Master",
        is_client: false,
        is_master: true,
        reassign_allowed: false,
        service_name: "HBASE",
        stack_name: "BIGTOP",
        stack_version: "3.2.0",
      },
      href: "",
    }],
  }],
};

const renderAssignments = ({
  advisorInputKey = "advisor-input-a",
  hostsList = ["host1.example.com"],
  isCurrent = () => true,
  onReview = vi.fn(),
  parentState = { clusterCreationSteps: {} },
}: {
  advisorInputKey?: string;
  hostsList?: string[];
  isCurrent?: () => boolean;
  onReview?: () => void;
  parentState?: any;
} = {}) => {
  const dispatch = vi.fn();
  const setCanProceed = vi.fn();
  const rendered = render(
    <AssignMasters
      STACK="BIGTOP"
      VERSION="3.2.0"
      advisorInputKey={advisorInputKey}
      dispatch={dispatch}
      hostsList={hostsList}
      onReviewManagedDependencies={onReview}
      parentState={parentState}
      runWithAdvisorRequest={async (request) => request({
        isCurrent,
        properties: { managed_dependency_plan: managedPlan },
      })}
      services={["HBASE"]}
      setCanProceed={setCanProceed}
    />,
  );
  return { dispatch, onReview, setCanProceed, ...rendered };
};

describe("Assign Masters managed dependency advice", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getCpuInfo.mockResolvedValue({
      data: { items: [{ Hosts: {
        cpu_count: 4,
        host_name: "host1.example.com",
        total_mem: 8192,
      } }] },
    });
    mocks.getServices.mockResolvedValue(servicesMetadata);
    mocks.postRecommendations.mockResolvedValue(recommendation);
    mocks.postValidations.mockResolvedValue({ resources: [] });
  });

  it("uses one exact reviewed plan for both advisor calls without adding local ZooKeeper", async () => {
    const { dispatch } = renderAssignments();

    await waitFor(() => expect(mocks.postRecommendations).toHaveBeenCalledTimes(2));
    expect(mocks.postRecommendations.mock.calls[0][0].managed_dependency_plan)
      .toEqual(managedPlan);
    expect(mocks.postRecommendations.mock.calls[1][0].managed_dependency_plan)
      .toEqual(managedPlan);
    await waitFor(() => expect(mocks.postValidations).toHaveBeenCalled());
    expect(mocks.postValidations.mock.calls.at(-1)?.[0].managed_dependency_plan)
      .toEqual(managedPlan);
    await waitFor(() => expect(dispatch).toHaveBeenCalledWith(
      expect.objectContaining({
        hostsData: expect.objectContaining({
          "host1.example.com": expect.objectContaining({
            components: ["HBASE_MASTER"],
          }),
        }),
      }),
    ));
  });

  it("drops a late first response after its draft scope is replaced", async () => {
    let resolveFirst!: (value: typeof recommendation) => void;
    const first = new Promise<typeof recommendation>((resolve) => {
      resolveFirst = resolve;
    });
    let current = true;
    mocks.postRecommendations.mockReset().mockReturnValueOnce(first);
    const { dispatch } = renderAssignments({ isCurrent: () => current });
    await waitFor(() => expect(mocks.postRecommendations).toHaveBeenCalledOnce());

    current = false;
    resolveFirst(recommendation);

    await Promise.resolve();
    await Promise.resolve();
    expect(mocks.postRecommendations).toHaveBeenCalledOnce();
    expect(mocks.getServices).not.toHaveBeenCalled();
    expect(dispatch.mock.calls.some(([value]) => value.hostsData)).toBe(false);
  });

  it("keeps a stale-plan failure retryable and links back to provider review", async () => {
    const user = userEvent.setup();
    mocks.postRecommendations.mockRejectedValueOnce({
      isAxiosError: true,
      response: {
        data: {
          code: "WORKFLOW_VERSION_CONFLICT",
          message: "Reload the saved provider plan.",
        },
        status: 409,
      },
    });
    const onReview = vi.fn();
    renderAssignments({ onReview });

    expect(await screen.findByText("Reload the saved provider plan.")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Review provider settings" }));
    expect(onReview).toHaveBeenCalledOnce();
    expect(screen.getByRole("button", { name: "Retry" })).toBeEnabled();
  });

  it("retries validation for the edited placement without requesting new recommendations", async () => {
    const user = userEvent.setup();
    mocks.getCpuInfo.mockResolvedValue({
      data: { items: [
        { Hosts: { cpu_count: 4, host_name: "host1.example.com", total_mem: 8192 } },
        { Hosts: { cpu_count: 4, host_name: "host2.example.com", total_mem: 8192 } },
      ] },
    });
    mocks.postRecommendations.mockResolvedValue(twoHostRecommendation);
    const { dispatch, setCanProceed } = renderAssignments({
      hostsList: ["host1.example.com", "host2.example.com"],
    });
    await waitFor(() => expect(mocks.postValidations).toHaveBeenCalledOnce());

    setCanProceed.mockClear();
    mocks.postValidations.mockRejectedValueOnce({
      isAxiosError: true,
      response: { data: { message: "The edited placement is temporarily stale." }, status: 409 },
    });
    const hostSelect = screen.getByRole("combobox");
    await user.click(hostSelect);
    await user.type(hostSelect, "host2.example.com{enter}");

    expect(await screen.findByText("The edited placement is temporarily stale."))
      .toBeInTheDocument();
    expect(mocks.postRecommendations).toHaveBeenCalledTimes(2);
    expect(dispatch.mock.calls.some(([value]) => value.mastersData?.some(
      (host: any) => host.host_name === "host2.example.com"
        && host.masterServices.some(
          (master: any) => master.component === "HBASE_MASTER",
        ),
    ))).toBe(true);
    expect(setCanProceed).toHaveBeenLastCalledWith(false);

    await user.click(screen.getByRole("button", { name: "Retry" }));
    await waitFor(() => expect(mocks.postValidations).toHaveBeenCalledTimes(3));
    expect(mocks.postRecommendations).toHaveBeenCalledTimes(2);
    expect(mocks.postValidations.mock.calls.at(-1)?.[0].recommendations)
      .toEqual(expect.objectContaining({
        blueprint_cluster_binding: expect.objectContaining({
          host_groups: expect.arrayContaining([
            expect.objectContaining({
              hosts: [{ fqdn: "host2.example.com" }],
            }),
          ]),
        }),
      }));
    await waitFor(() => expect(setCanProceed).toHaveBeenLastCalledWith(true));
  });

  it("clears a stale validation error when a later manual edit validates", async () => {
    const user = userEvent.setup();
    mocks.getCpuInfo.mockResolvedValue({
      data: { items: [
        { Hosts: { cpu_count: 4, host_name: "host1.example.com", total_mem: 8192 } },
        { Hosts: { cpu_count: 4, host_name: "host2.example.com", total_mem: 8192 } },
      ] },
    });
    mocks.postRecommendations.mockResolvedValue(twoHostRecommendation);
    const { setCanProceed } = renderAssignments({
      hostsList: ["host1.example.com", "host2.example.com"],
    });
    await waitFor(() => expect(mocks.postValidations).toHaveBeenCalledOnce());

    setCanProceed.mockClear();
    mocks.postValidations.mockRejectedValueOnce({
      isAxiosError: true,
      response: { data: { message: "Review this placement." }, status: 409 },
    });
    const hostSelect = screen.getByRole("combobox");
    await user.click(hostSelect);
    await user.type(hostSelect, "host2.example.com{enter}");
    expect(await screen.findByText("Review this placement.")).toBeInTheDocument();

    await user.click(hostSelect);
    await user.type(hostSelect, "host1.example.com{enter}");
    await waitFor(() => expect(screen.queryByText("Review this placement."))
      .not.toBeInTheDocument());
    expect(mocks.postRecommendations).toHaveBeenCalledTimes(2);
    expect(setCanProceed).toHaveBeenLastCalledWith(true);
  });

  it("restores manual masters only while the authoritative advice inputs match", async () => {
    mocks.getCpuInfo.mockResolvedValue({
      data: { items: [
        { Hosts: { cpu_count: 4, host_name: "host1.example.com", total_mem: 8192 } },
        { Hosts: { cpu_count: 4, host_name: "host2.example.com", total_mem: 8192 } },
      ] },
    });
    mocks.postRecommendations.mockResolvedValue(twoHostRecommendation);
    const parentState = {
      clusterCreationSteps: {
        MASTERS: { data: {
          advisorInputKey: "unchanged-input",
          state: { hosts: {
            "host1.example.com": {
              components: [], cores: 4, hostname: "host1.example.com", memory: 8192,
            },
            "host2.example.com": {
              components: ["HBASE_MASTER"], cores: 4,
              hostname: "host2.example.com", memory: 8192,
            },
          } },
        } },
      },
    };
    const restored = renderAssignments({
      advisorInputKey: "unchanged-input",
      hostsList: ["host1.example.com", "host2.example.com"],
      parentState,
    });

    await waitFor(() => expect(mocks.postValidations).toHaveBeenCalledOnce());
    expect(mocks.getCpuInfo).not.toHaveBeenCalled();
    expect(mocks.postRecommendations).not.toHaveBeenCalled();
    expect(mocks.postValidations.mock.calls[0][0].recommendations)
      .toEqual(expect.objectContaining({
        blueprint_cluster_binding: expect.objectContaining({
          host_groups: expect.arrayContaining([
            expect.objectContaining({
              hosts: [{ fqdn: "host2.example.com" }],
            }),
          ]),
        }),
      }));

    restored.unmount();
    vi.clearAllMocks();
    mocks.getCpuInfo.mockResolvedValue({
      data: { items: [
        { Hosts: { cpu_count: 4, host_name: "host1.example.com", total_mem: 8192 } },
        { Hosts: { cpu_count: 4, host_name: "host2.example.com", total_mem: 8192 } },
      ] },
    });
    mocks.getServices.mockResolvedValue(servicesMetadata);
    mocks.postRecommendations.mockResolvedValue(twoHostRecommendation);
    mocks.postValidations.mockResolvedValue({ resources: [] });
    const changed = renderAssignments({
      advisorInputKey: "changed-input",
      hostsList: ["host1.example.com", "host2.example.com"],
      parentState,
    });

    await waitFor(() => expect(mocks.postRecommendations).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(changed.dispatch.mock.calls.some(([value]) =>
      value.mastersData?.some(
        (host: any) => host.host_name === "host1.example.com"
          && host.masterServices.some(
            (master: any) => master.component === "HBASE_MASTER",
          ),
      ))).toBe(true));
  });
});
