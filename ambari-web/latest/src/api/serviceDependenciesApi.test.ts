/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
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

const mocks = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn() }));
vi.mock("./config/axiosConfig", () => ({
  ambariApi: { get: mocks.get, post: mocks.post },
}));

import ServiceDependenciesApi, {
  type CreateManagedDependencyRequest,
} from "./serviceDependenciesApi";

describe("ServiceDependenciesApi", () => {
  beforeEach(() => vi.resetAllMocks());

  it("sends the exact owned draft revision for discovery and preview", async () => {
    const signal = new AbortController().signal;
    mocks.get.mockResolvedValue({ data: { items: [] } });
    mocks.post.mockResolvedValue({ data: { binding_id: "binding-a" } });

    await ServiceDependenciesApi.getDraftCandidates({
      dependencyType: "HDFS",
      draftId: "draft-a",
      expectedDraftRevision: 7,
      signal,
    });
    await ServiceDependenciesApi.previewDraft({
      dependencyType: "HDFS",
      draftId: "draft-a",
      expectedRevision: 8,
      provider: { cluster_id: 31, service_name: "HDFS" },
      signal,
    });

    expect(mocks.get).toHaveBeenCalledWith("/service-dependencies/candidates", {
      params: {
        consumer_scope: "draft",
        dependency_type: "HDFS",
        draft_id: "draft-a",
        expected_draft_revision: 7,
      },
      signal,
    });
    expect(mocks.post).toHaveBeenCalledWith("/service-dependencies/preview", {
      consumer: { scope: "DRAFT", draft_id: "draft-a", expected_revision: 8 },
      dependency_type: "HDFS",
      provider: { cluster_id: 31, service_name: "HDFS" },
    }, { signal });
  });

  it("plans Add Service against the numeric cluster before HBase exists", async () => {
    const signal = new AbortController().signal;
    mocks.get.mockResolvedValue({ data: { items: [] } });
    mocks.post.mockResolvedValue({ data: { binding_id: "binding-a" } });

    await ServiceDependenciesApi.getServicePlanCandidates({
      clusterId: 27,
      dependencyType: "ZOOKEEPER",
      expectedWorkflowRevision: 5,
      signal,
    });
    await ServiceDependenciesApi.previewServicePlan({
      clusterId: 27,
      dependencyType: "ZOOKEEPER",
      expectedRevision: 6,
      provider: { cluster_id: 42, service_name: "ZOOKEEPER" },
      signal,
    });

    expect(mocks.get).toHaveBeenCalledWith("/service-dependencies/candidates", {
      params: {
        cluster_id: 27,
        consumer_scope: "service_plan",
        dependency_type: "ZOOKEEPER",
        expected_workflow_revision: 5,
      },
      signal,
    });
    expect(mocks.post).toHaveBeenCalledWith("/service-dependencies/preview", {
      consumer: { scope: "SERVICE_PLAN", cluster_id: 27, expected_revision: 6 },
      dependency_type: "ZOOKEEPER",
      provider: { cluster_id: 42, service_name: "ZOOKEEPER" },
    }, { signal });
  });

  it("preserves an approved binding identity when previewing again", async () => {
    const signal = new AbortController().signal;
    mocks.post.mockResolvedValue({ data: { binding_id: "binding-a" } });

    await ServiceDependenciesApi.previewServicePlan({
      bindingId: "00000000-0000-4000-8000-000000000001",
      clusterId: 27,
      dependencyType: "HDFS",
      expectedRevision: 8,
      provider: { cluster_id: 31, service_name: "HDFS" },
      signal,
    });
    await ServiceDependenciesApi.previewService(
      "consumer",
      "HDFS",
      { cluster_id: 31, service_name: "HDFS" },
      signal,
      "00000000-0000-4000-8000-000000000001",
    );

    expect(mocks.post).toHaveBeenNthCalledWith(1, "/service-dependencies/preview", {
      binding_id: "00000000-0000-4000-8000-000000000001",
      consumer: { scope: "SERVICE_PLAN", cluster_id: 27, expected_revision: 8 },
      dependency_type: "HDFS",
      provider: { cluster_id: 31, service_name: "HDFS" },
    }, { signal });
    expect(mocks.post).toHaveBeenNthCalledWith(
      2,
      "/clusters/consumer/services/HBASE/dependencies/preview",
      {
        binding_id: "00000000-0000-4000-8000-000000000001",
        dependency_type: "HDFS",
        provider: { cluster_id: 31, service_name: "HDFS" },
      },
      { signal },
    );
  });

  it("sends the complete selected plan and creates every request in one POST", async () => {
    const signal = new AbortController().signal;
    const requests: CreateManagedDependencyRequest[] = [
      {
        binding_id: "00000000-0000-4000-8000-000000000001",
        dependency_type: "HDFS",
        expected_consumer_descriptor_fingerprint: "sha256:consumer",
        expected_provider_fingerprint: "sha256:hdfs",
        expected_snapshot_fingerprint: "sha256:hdfs-snapshot",
        operation_id: "00000000-0000-4000-8000-000000000003",
        preview_schema_version: 2,
        provider: { cluster_id: 31, service_name: "HDFS" },
      },
      {
        binding_id: "00000000-0000-4000-8000-000000000004",
        dependency_type: "ZOOKEEPER",
        expected_consumer_descriptor_fingerprint: "sha256:consumer",
        expected_provider_fingerprint: "sha256:zookeeper",
        expected_snapshot_fingerprint: "sha256:zookeeper-snapshot",
        operation_id: "00000000-0000-4000-8000-000000000005",
        preview_schema_version: 2,
        provider: { cluster_id: 42, service_name: "ZOOKEEPER" },
      },
    ];
    mocks.post
      .mockResolvedValueOnce({ data: { items: [{ binding_id: requests[0].binding_id }] } })
      .mockResolvedValueOnce({ data: { items: requests.map(({ binding_id }) => ({ binding_id })) } });

    await ServiceDependenciesApi.previewPlan({
      consumer: { scope: "SERVICE_PLAN", cluster_id: 27, expected_revision: 9 },
      selections: requests.map(({ binding_id, dependency_type, provider }) => ({
        binding_id,
        dependency_type,
        provider,
      })),
      signal,
    });
    const bindings = await ServiceDependenciesApi.createMany("consumer", requests, signal);

    expect(mocks.post).toHaveBeenNthCalledWith(1, "/service-dependencies/preview", {
      consumer: { scope: "SERVICE_PLAN", cluster_id: 27, expected_revision: 9 },
      selections: [
        {
          binding_id: requests[0].binding_id,
          dependency_type: "HDFS",
          provider: { cluster_id: 31, service_name: "HDFS" },
        },
        {
          binding_id: requests[1].binding_id,
          dependency_type: "ZOOKEEPER",
          provider: { cluster_id: 42, service_name: "ZOOKEEPER" },
        },
      ],
    }, { signal });
    expect(mocks.post).toHaveBeenNthCalledWith(
      2,
      "/clusters/consumer/services/HBASE/dependencies",
      { items: requests },
      { signal },
    );
    expect(bindings).toEqual(requests.map(({ binding_id }) => ({ binding_id })));
  });

  it("encodes the selected cluster and replays an unchanged creation identity", async () => {
    const request: CreateManagedDependencyRequest = {
      binding_id: "00000000-0000-4000-8000-000000000001",
      dependency_type: "ZOOKEEPER",
      draft: { id: "00000000-0000-4000-8000-000000000002", revision: 11 },
      expected_consumer_descriptor_fingerprint: "sha256:consumer",
      expected_provider_fingerprint: "sha256:provider",
      expected_snapshot_fingerprint: "sha256:snapshot",
      operation_id: "00000000-0000-4000-8000-000000000003",
      preview_schema_version: 2,
      provider: { cluster_id: 42, service_name: "ZOOKEEPER" },
    };
    mocks.post.mockResolvedValue({ data: { binding_id: request.binding_id } });
    mocks.get.mockResolvedValue({ data: { items: [] } });

    await ServiceDependenciesApi.create("cluster-one", request);
    await ServiceDependenciesApi.list("cluster-one");
    await ServiceDependenciesApi.get("cluster-one", "binding/id");

    expect(mocks.post).toHaveBeenCalledWith(
      "/clusters/cluster-one/services/HBASE/dependencies",
      request,
      { signal: undefined },
    );
    expect(mocks.get).toHaveBeenNthCalledWith(
      1,
      "/clusters/cluster-one/services/HBASE/dependencies",
      { signal: undefined },
    );
    expect(mocks.get).toHaveBeenNthCalledWith(
      2,
      "/clusters/cluster-one/services/HBASE/dependencies/binding%2Fid",
      { signal: undefined },
    );
  });

  it("reads provider dependents and STOP impact from the explicit provider service", async () => {
    const signal = new AbortController().signal;
    mocks.get
      .mockResolvedValueOnce({ data: { hidden_dependent_count: 0, impact_revision: "r1", items: [] } })
      .mockResolvedValueOnce({ data: {
        action: "STOP",
        dependent_count: 0,
        impact_revision: "r1",
        requires_confirmation: false,
      } });

    await ServiceDependenciesApi.getDependents("storage / east", "HDFS", signal);
    await ServiceDependenciesApi.getImpact("storage / east", "HDFS", signal);

    expect(mocks.get).toHaveBeenNthCalledWith(
      1,
      "/clusters/storage%20%2F%20east/services/HDFS/dependents",
      { signal },
    );
    expect(mocks.get).toHaveBeenNthCalledWith(
      2,
      "/clusters/storage%20%2F%20east/services/HDFS/dependency-impact",
      { params: { action: "STOP" }, signal },
    );
  });
});
