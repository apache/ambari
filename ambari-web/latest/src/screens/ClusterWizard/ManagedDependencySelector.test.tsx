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

import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { useState } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../../i18n";

const mocks = vi.hoisted(() => ({
  getDraftCandidates: vi.fn(),
  getServiceCandidates: vi.fn(),
  getServicePlanCandidates: vi.fn(),
  previewDraft: vi.fn(),
  previewPlan: vi.fn(),
  previewService: vi.fn(),
  previewServicePlan: vi.fn(),
}));

vi.mock("../../api/serviceDependenciesApi", () => ({
  default: mocks,
}));

import ManagedDependencySelector from "./ManagedDependencySelector";
import type { ManagedDependencyCandidate } from "../../api/serviceDependenciesApi";
import type { ManagedDependencySelections } from "./managedDependencySelection";

const candidate = (
  clusterId: number,
  clusterName: string,
  serviceName: "HDFS" | "ZOOKEEPER",
): ManagedDependencyCandidate => ({
  cluster_id: clusterId,
  cluster_name: clusterName,
  compatible: true,
  errors: [],
  healthy: true,
  installed: true,
  security_mode: "NONE",
  service_name: serviceName,
  version: {
    active: true,
    client_features: [],
    resolved_versions: { distribution: "3.3.6" },
    service_version: "3.3.6",
    stack_name: "BIGTOP",
    stack_version: "3.2.0",
  },
});

const preview = (provider: ManagedDependencyCandidate) => ({
  binding_id: "00000000-0000-4000-8000-000000000001",
  compatible: true,
  consumer: {
    lifecycle: "DRAFT",
    planned_hbase_user: "hbase",
    scope: "DRAFT",
    service_name: "HBASE" as const,
  },
  dependency_type: provider.service_name,
  errors: [],
  preview_schema_version: 1,
  provider,
  provider_fingerprint: "provider-fingerprint",
  consumer_descriptor_fingerprint: "consumer-fingerprint",
});

describe("managed dependency selector", () => {
  afterEach(cleanup);

  beforeEach(() => {
    vi.resetAllMocks();
    mocks.getDraftCandidates.mockImplementation(({ dependencyType }) => Promise.resolve(
      dependencyType === "HDFS" ? [candidate(41, "storage-east", "HDFS")] : [],
    ));
  });

  it("saves the exact draft choice before preview and persists the approved preview", async () => {
    const onSelectionChange = vi.fn()
      .mockResolvedValueOnce(8)
      .mockResolvedValueOnce(9);
    const checkpoint = vi.fn().mockResolvedValue(7);
    const provider = candidate(41, "storage-east", "HDFS");
    mocks.previewDraft.mockResolvedValue(preview(provider));

    render(
      <ManagedDependencySelector
        consumer={{ checkpoint, draftId: "00000000-0000-4000-8000-000000000010", kind: "draft" }}
        onSelectionChange={onSelectionChange}
        selections={{ HDFS: { mode: "local" }, ZOOKEEPER: { mode: "local" } }}
      />,
    );

    fireEvent.click(await screen.findByRole("radio", { name: "storage-east / HDFS" }));

    expect(checkpoint).toHaveBeenCalledBefore(mocks.getDraftCandidates);
    expect(mocks.getDraftCandidates).toHaveBeenCalledWith(expect.objectContaining({
      expectedDraftRevision: 7,
    }));
    await waitFor(() => expect(mocks.previewDraft).toHaveBeenCalledWith({
      dependencyType: "HDFS",
      draftId: "00000000-0000-4000-8000-000000000010",
      expectedRevision: 8,
      provider: { cluster_id: 41, service_name: "HDFS" },
      signal: expect.any(AbortSignal),
    }));
    expect(onSelectionChange.mock.calls[0][1]).toEqual({ mode: "managed", provider });
    await waitFor(() => expect(onSelectionChange.mock.calls[1][1].preview.compatible).toBe(true));
  });

  it("previews every managed provider together after a selection change", async () => {
    const hdfs = candidate(41, "storage-east", "HDFS");
    const zookeeper = candidate(42, "coord-east", "ZOOKEEPER");
    mocks.getDraftCandidates.mockImplementation(({ dependencyType }) => Promise.resolve(
      dependencyType === "HDFS" ? [hdfs] : [zookeeper],
    ));
    mocks.previewPlan.mockResolvedValue({
      items: [
        { ...preview(hdfs), dependency_type: "HDFS" },
        {
          ...preview(zookeeper),
          binding_id: "00000000-0000-4000-8000-000000000002",
          dependency_type: "ZOOKEEPER",
        },
      ],
    });
    let revision = 7;
    function Harness() {
      const [selections, setSelections] = useState<ManagedDependencySelections>({
        HDFS: { mode: "local" },
        ZOOKEEPER: { mode: "managed", provider: zookeeper, preview: preview(zookeeper) },
      });
      return (
        <ManagedDependencySelector
          consumer={{ checkpoint: async () => revision, draftId: "draft-1", kind: "draft" }}
          onSelectionChange={vi.fn()}
          onPlanSelectionChange={async (next) => {
            setSelections(next);
            revision += 1;
            return revision;
          }}
          selections={selections}
        />
      );
    }

    render(<Harness />);
    fireEvent.click(await screen.findByRole("radio", { name: "storage-east / HDFS" }));

    await waitFor(() => expect(mocks.previewPlan).toHaveBeenCalledWith(expect.objectContaining({
      consumer: { scope: "DRAFT", draft_id: "draft-1", expected_revision: 8 },
      selections: expect.arrayContaining([
        expect.objectContaining({ dependency_type: "HDFS" }),
        expect.objectContaining({ dependency_type: "ZOOKEEPER" }),
      ]),
      signal: expect.any(AbortSignal),
    })));
    expect(mocks.previewDraft).not.toHaveBeenCalled();
  });

  it("previews an Add Service plan by numeric cluster before HBase exists", async () => {
    const provider = candidate(41, "storage-east", "HDFS");
    mocks.getServicePlanCandidates.mockImplementation(({ dependencyType }) => Promise.resolve(
      dependencyType === "HDFS" ? [provider] : [],
    ));
    mocks.previewServicePlan.mockResolvedValue({
      ...preview(provider),
      consumer: {
        cluster_id: 27,
        cluster_name: "analytics",
        lifecycle: "ADD_SERVICE_PLAN",
        planned_hbase_user: "hbase",
        scope: "SERVICE_PLAN",
        service_name: "HBASE" as const,
      },
    });
    const onSelectionChange = vi.fn()
      .mockResolvedValueOnce(6)
      .mockResolvedValueOnce(7);

    render(
      <ManagedDependencySelector
        consumer={{ checkpoint: async () => 5, clusterId: 27, kind: "servicePlan" }}
        onSelectionChange={onSelectionChange}
        selections={{ HDFS: { mode: "local" }, ZOOKEEPER: { mode: "local" } }}
      />,
    );
    fireEvent.click(await screen.findByRole("radio", { name: "storage-east / HDFS" }));

    await waitFor(() => expect(mocks.previewServicePlan).toHaveBeenCalledWith({
      clusterId: 27,
      dependencyType: "HDFS",
      expectedRevision: 6,
      provider: { cluster_id: 41, service_name: "HDFS" },
      signal: expect.any(AbortSignal),
    }));
    expect(mocks.getServicePlanCandidates).toHaveBeenCalledWith(expect.objectContaining({
      clusterId: 27,
      expectedWorkflowRevision: 5,
    }));
    expect(mocks.getServiceCandidates).not.toHaveBeenCalled();
  });

  it("switches an interrupted Add Service materialization to the live HBase API", async () => {
    const provider = candidate(41, "storage-east", "HDFS");
    mocks.getServiceCandidates.mockImplementation((_clusterName, dependencyType) =>
      Promise.resolve(dependencyType === "HDFS" ? [provider] : []));
    mocks.previewService.mockResolvedValue({
      ...preview(provider),
      consumer: {
        cluster_id: 27,
        cluster_name: "analytics",
        lifecycle: "INIT",
        planned_hbase_user: "hbase",
        scope: "SERVICE",
        service_name: "HBASE" as const,
      },
    });
    const onSelectionChange = vi.fn()
      .mockResolvedValueOnce(9)
      .mockResolvedValueOnce(10);

    render(
      <ManagedDependencySelector
        consumer={{
          checkpoint: async () => 8,
          clusterId: 27,
          clusterName: "analytics",
          kind: "service",
        }}
        onSelectionChange={onSelectionChange}
        selections={{ HDFS: { mode: "local" }, ZOOKEEPER: { mode: "local" } }}
      />,
    );
    fireEvent.click(await screen.findByRole("radio", { name: "storage-east / HDFS" }));

    await waitFor(() => expect(mocks.previewService).toHaveBeenCalledWith(
      "analytics",
      "HDFS",
      { cluster_id: 41, service_name: "HDFS" },
      expect.any(AbortSignal),
      undefined,
    ));
    expect(mocks.getServicePlanCandidates).not.toHaveBeenCalled();
  });

  it("renders a recovered approved provider without issuing another preview", async () => {
    const provider = candidate(41, "storage-east", "HDFS");
    render(
      <ManagedDependencySelector
        consumer={{ checkpoint: async () => 12, draftId: "00000000-0000-4000-8000-000000000010", kind: "draft" }}
        onSelectionChange={vi.fn()}
        selections={{
          HDFS: { mode: "managed", provider, preview: preview(provider) },
          ZOOKEEPER: { mode: "local" },
        }}
      />,
    );

    expect(await screen.findByText(/Provider in storage-east has been reviewed/)).toBeTruthy();
    expect(mocks.previewDraft).not.toHaveBeenCalled();
  });

  it("keeps an incomplete secure draft editable without reporting it compatible", async () => {
    const provider = candidate(41, "storage-east", "HDFS");
    const onSelectionChange = vi.fn();
    mocks.previewDraft.mockRejectedValue({
      response: { data: {
        code: "DEPENDENCY_SECURITY_PLAN_INCOMPLETE",
        message: "Enter the effective Kerberos realm before final review.",
      }, status: 422 },
    });

    function Harness() {
      const [selections, setSelections] = useState<ManagedDependencySelections>({
        HDFS: { mode: "local" },
        ZOOKEEPER: { mode: "local" },
      });
      return (
        <ManagedDependencySelector
          consumer={{ checkpoint: async () => 13, draftId: "00000000-0000-4000-8000-000000000010", kind: "draft" }}
          onSelectionChange={async (type, choice) => {
            onSelectionChange(type, choice);
            setSelections((current) => ({ ...current, [type]: choice }));
            return 14;
          }}
          selections={selections}
        />
      );
    }
    render(<Harness />);
    fireEvent.click(await screen.findByRole("radio", { name: "storage-east / HDFS" }));

    expect(await screen.findByText("Enter the effective Kerberos realm before final review.")).toBeTruthy();
    expect(screen.queryByText(/has been reviewed/)).toBeNull();
    expect(onSelectionChange.mock.calls[1][1].planningIssue.code)
      .toBe("DEPENDENCY_SECURITY_PLAN_INCOMPLETE");
  });

  it("stops before preview when the scoped checkpoint conflicts", async () => {
    mocks.getDraftCandidates.mockImplementation(({ dependencyType }) => Promise.resolve(
      dependencyType === "HDFS" ? [candidate(41, "storage-east", "HDFS")] : [],
    ));
    const onSelectionChange = vi.fn().mockRejectedValue({
      response: { data: { code: "WORKFLOW_VERSION_CONFLICT", message: "Reload the saved draft." } },
    });
    render(
      <ManagedDependencySelector
        consumer={{ checkpoint: async () => 7, draftId: "00000000-0000-4000-8000-000000000010", kind: "draft" }}
        onSelectionChange={onSelectionChange}
        selections={{ HDFS: { mode: "local" }, ZOOKEEPER: { mode: "local" } }}
      />,
    );

    fireEvent.click(await screen.findByRole("radio", { name: "storage-east / HDFS" }));

    expect(await screen.findByText("Reload the saved draft.")).toBeTruthy();
    expect(mocks.previewDraft).not.toHaveBeenCalled();
  });

  it("retries a failed preview for the retained provider selection", async () => {
    const provider = candidate(41, "storage-east", "HDFS");
    mocks.previewDraft
      .mockRejectedValueOnce(new Error("preview transport failed"))
      .mockResolvedValueOnce(preview(provider));
    let revision = 20;

    function Harness() {
      const [selections, setSelections] = useState<ManagedDependencySelections>({
        HDFS: { mode: "local" },
        ZOOKEEPER: { mode: "local" },
      });
      return (
        <ManagedDependencySelector
          consumer={{ checkpoint: async () => revision, draftId: "00000000-0000-4000-8000-000000000010", kind: "draft" }}
          onSelectionChange={async (type, choice) => {
            setSelections((current) => ({ ...current, [type]: choice }));
            revision += 1;
            return revision;
          }}
          selections={selections}
        />
      );
    }
    render(<Harness />);
    fireEvent.click(await screen.findByRole("radio", { name: "storage-east / HDFS" }));

    expect(await screen.findByText("preview transport failed")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));

    expect(await screen.findByText(/Provider in storage-east has been reviewed/)).toBeTruthy();
    expect(mocks.previewDraft).toHaveBeenCalledTimes(2);
    expect(mocks.previewDraft.mock.calls[1][0].expectedRevision).toBe(22);
  });

  it("shows an incompatible preview after provider drift and allows another review", async () => {
    const provider = candidate(41, "storage-east", "HDFS");
    const driftedProvider = {
      ...provider,
      version: {
        ...provider.version,
        resolved_versions: { distribution: "3.3.7" },
        service_version: "3.3.7",
      },
    };
    mocks.previewDraft
      .mockResolvedValueOnce({
        ...preview(driftedProvider),
        compatible: false,
        errors: [{
          code: "DEPENDENCY_PREVIEW_STALE",
          message: "The provider changed after candidate discovery.",
        }],
      })
      .mockResolvedValueOnce(preview(provider));
    let revision = 30;

    function Harness() {
      const [selections, setSelections] = useState<ManagedDependencySelections>({
        HDFS: { mode: "local" },
        ZOOKEEPER: { mode: "local" },
      });
      return (
        <ManagedDependencySelector
          consumer={{ checkpoint: async () => revision, draftId: "00000000-0000-4000-8000-000000000010", kind: "draft" }}
          onSelectionChange={async (type, choice) => {
            setSelections((current) => ({ ...current, [type]: choice }));
            revision += 1;
            return revision;
          }}
          selections={selections}
        />
      );
    }
    render(<Harness />);
    fireEvent.click(await screen.findByRole("radio", { name: "storage-east / HDFS" }));

    expect(await screen.findByText("The provider changed after candidate discovery.")).toBeTruthy();
    expect(screen.queryByText(/has been reviewed/)).toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "Review again" }));

    expect(await screen.findByText(/Provider in storage-east has been reviewed/)).toBeTruthy();
    expect(mocks.previewDraft).toHaveBeenCalledTimes(2);
    expect(mocks.previewDraft.mock.calls[1][0].bindingId)
      .toBe("00000000-0000-4000-8000-000000000001");
  });

  it("aborts an old provider preview and retains the replacement selection", async () => {
    const providerA = candidate(41, "storage-east", "HDFS");
    const providerB = candidate(42, "storage-west", "HDFS");
    mocks.getDraftCandidates.mockImplementation(({ dependencyType }) => Promise.resolve(
      dependencyType === "HDFS" ? [providerA, providerB] : [],
    ));
    let resolveOld!: (value: unknown) => void;
    mocks.previewDraft.mockImplementation(({ provider }) => provider.cluster_id === 41
      ? new Promise((resolve) => { resolveOld = resolve; })
      : Promise.resolve(preview(providerB)));

    function Harness() {
      const [selections, setSelections] = useState<ManagedDependencySelections>({
        HDFS: { mode: "local" },
        ZOOKEEPER: { mode: "local" },
      });
      return (
        <ManagedDependencySelector
          consumer={{ checkpoint: async () => 2, draftId: "00000000-0000-4000-8000-000000000010", kind: "draft" }}
          onSelectionChange={async (type, choice) => {
            setSelections((current) => ({ ...current, [type]: choice }));
            return choice.provider?.cluster_id === 41 ? 3 : 4;
          }}
          selections={selections}
        />
      );
    }
    render(<Harness />);
    fireEvent.click(await screen.findByRole("radio", { name: "storage-east / HDFS" }));
    await waitFor(() => expect(mocks.previewDraft).toHaveBeenCalledWith(expect.objectContaining({
      provider: { cluster_id: 41, service_name: "HDFS" },
    })));
    fireEvent.click(screen.getByRole("radio", { name: "storage-west / HDFS" }));

    expect(await screen.findByText(/Provider in storage-west has been reviewed/)).toBeTruthy();
    resolveOld(preview(providerA));
    await waitFor(() => expect(screen.queryByText(/Provider in storage-east has been reviewed/)).toBeNull());
  });

  it("ignores late candidate rows from a replaced draft scope", async () => {
    const resolveOld: Array<(items: ManagedDependencyCandidate[]) => void> = [];
    mocks.getDraftCandidates.mockImplementation(({ draftId, dependencyType }) => {
      if (draftId.endsWith("10")) return new Promise((resolve) => { resolveOld.push(resolve); });
      return Promise.resolve(dependencyType === "HDFS"
        ? [candidate(52, "current-provider", "HDFS")]
        : []);
    });
    const view = render(
      <ManagedDependencySelector
        consumer={{ checkpoint: async () => 2, draftId: "00000000-0000-4000-8000-000000000010", kind: "draft" }}
        onSelectionChange={vi.fn()}
        selections={{}}
      />,
    );
    await waitFor(() => expect(mocks.getDraftCandidates).toHaveBeenCalled());

    view.rerender(
      <ManagedDependencySelector
        consumer={{ checkpoint: async () => 1, draftId: "00000000-0000-4000-8000-000000000011", kind: "draft" }}
        onSelectionChange={vi.fn()}
        selections={{}}
      />,
    );
    expect(await screen.findByRole("radio", { name: "current-provider / HDFS" })).toBeTruthy();
    resolveOld.forEach((resolve) => resolve([candidate(9, "stale-provider", "HDFS")]));
    await waitFor(() => expect(screen.queryByRole("radio", { name: "stale-provider / HDFS" })).toBeNull());
  });
});
