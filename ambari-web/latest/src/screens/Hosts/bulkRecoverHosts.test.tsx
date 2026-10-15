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

import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  batchRequest: vi.fn(),
  getHostsBulkOperations: vi.fn(),
  hide: vi.fn(),
  show: vi.fn(),
  showErrorModal: vi.fn(),
  showRollingNothingToDoModal: vi.fn(),
}));

vi.mock("../../api/hostsApi", () => ({
  HostsApi: {
    batchRequest: mocks.batchRequest,
    getHostsBulkOperations: mocks.getHostsBulkOperations,
  },
}));
vi.mock("../../store/ModalManager", () => ({
  default: { hide: mocks.hide, show: mocks.show },
}));
vi.mock("../../Utils/Utility", async (importOriginal) => ({
  ...await importOriginal<typeof import("../../Utils/Utility")>(),
  showErrorModal: mocks.showErrorModal,
  showRollingNothingToDoModal: mocks.showRollingNothingToDoModal,
}));

import BackgroundOperations from "../BackgroundOperations";
import {
  RecoverBatchStep,
  bulkOperationConfirm,
  buildRecoverHostsBatches,
  doBulkRecoverHosts,
  partitionHostsForRecovery,
} from "./bulkOperations";
import RecoverHostsModal, {
  RecoverHost,
  RecoverHostComponent,
} from "./RecoverHostsModal";

const component = (componentName: string, state: string) => ({
  HostRoles: { component_name: componentName, state },
});

const host = (
  hostName: string,
  components: RecoverHostComponent[]
): RecoverHost => ({
  hostName,
  allHostComponents: components,
});

const apiHost = (hostName: string, components: RecoverHostComponent[]) => ({
  Hosts: { host_name: hostName },
  host_components: components,
});

const runKdcCallback = async (callback: () => Promise<void>) => {
  await callback();
};

const recoverOperation = (isKerberosEnabled = false) => ({
  action: "RECOVER_HOST",
  message: "Recover Hosts",
  isKerberosEnabled,
});

const scheduleResponse = (id: number) => ({
  data: { resources: [{ RequestSchedule: { id } }] },
});

describe("partitionHostsForRecovery", () => {
  it("accepts hosts whose components are all INSTALLED, INSTALL_FAILED, or INIT", () => {
    const recoverable = host("h1", [
      component("DATANODE", "INSTALLED"),
      component("NODEMANAGER", "INSTALL_FAILED"),
      component("HDFS_CLIENT", "INIT"),
    ]);

    const { eligible, skipped } = partitionHostsForRecovery([recoverable]);

    expect(eligible).toEqual([recoverable]);
    expect(skipped).toEqual([]);
  });

  it("skips hosts with any other component state and names those components", () => {
    const running = host("h1", [
      component("DATANODE", "STARTED"),
      component("NODEMANAGER", "INSTALLED"),
      component("ZOOKEEPER_SERVER", "UNKNOWN"),
    ]);
    const stopped = host("h2", [component("DATANODE", "INSTALLED")]);

    const { eligible, skipped } = partitionHostsForRecovery([running, stopped]);

    expect(eligible).toEqual([stopped]);
    expect(skipped).toHaveLength(1);
    expect(skipped[0].host).toBe(running);
    expect(skipped[0].reason).toBe(
      "Components not in a recoverable state: DATANODE, ZOOKEEPER_SERVER"
    );
  });

  it("skips hosts that have no components to recover", () => {
    const empty = host("h1", []);

    const { eligible, skipped } = partitionHostsForRecovery([empty]);

    expect(eligible).toEqual([]);
    expect(skipped).toEqual([
      { host: empty, reason: "Host has no components to recover" },
    ]);
  });
});

describe("buildRecoverHostsBatches", () => {
  const h1 = host("h1", [
    component("DATANODE", "INSTALLED"),
    component("HDFS_CLIENT", "INIT"),
  ]);
  const h2 = host("h2", [component("NODEMANAGER", "INSTALL_FAILED")]);

  it("reproduces the single-host Recover Host request for one host", () => {
    expect(buildRecoverHostsBatches([h1], "c1", false)).toEqual(
      ["INIT", "INSTALLED"].map((state, index) => ({
        order_id: index + 1,
        type: "PUT",
        uri: "/clusters/c1/hosts/h1/host_components",
        RequestBodyInfo: {
          RequestInfo: {
            context:
              state === "INIT"
                ? "Init All Host Components"
                : "Install All Host Components",
            operation_level: {
              level: "HOST",
              cluster_name: "c1",
              host_name: "h1",
            },
            query: "HostRoles/component_name.in(DATANODE,HDFS_CLIENT)",
          },
          Body: { HostRoles: { state } },
        },
      }))
    );
  });

  it("initializes every host before installing any host", () => {
    const batches = buildRecoverHostsBatches([h1, h2], "c1", false);

    expect(
      batches.map((batch) => [
        batch.order_id,
        batch.RequestBodyInfo.RequestInfo.operation_level?.host_name,
        batch.RequestBodyInfo.Body.HostRoles?.state,
      ])
    ).toEqual([
      [1, "h1", "INIT"],
      [2, "h2", "INIT"],
      [3, "h1", "INSTALLED"],
      [4, "h2", "INSTALLED"],
    ]);
    expect(batches[1].RequestBodyInfo.RequestInfo.query).toBe(
      "HostRoles/component_name.in(NODEMANAGER)"
    );
  });

  it("regenerates keytabs once for all recovered hosts on Kerberos clusters", () => {
    const batches = buildRecoverHostsBatches([h1, h2], "c1", true);

    expect(batches).toHaveLength(5);
    expect(batches[4]).toEqual({
      order_id: 5,
      type: "PUT",
      uri: "/clusters/c1",
      RequestBodyInfo: {
        RequestInfo: {
          context: "Regenerate keytabs",
          query:
            "regenerate_keytabs=all&regenerate_hosts=h1,h2&config_update_policy=none",
        },
        Body: { Clusters: { security_type: "KERBEROS" } },
      },
    });
  });

  it("produces no steps when there are no eligible hosts", () => {
    expect(buildRecoverHostsBatches([], "c1", true)).toEqual([]);
  });
});

describe("bulk Recover Hosts flow", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const openRecoverConfirmation = async (
    items: ReturnType<typeof apiHost>[]
  ) => {
    mocks.getHostsBulkOperations.mockResolvedValue({ items });
    const getKDCSessionState = vi.fn(runKdcCallback);
    bulkOperationConfirm(
      recoverOperation(),
      ["h1", "h2"],
      "selected",
      "c1",
      [],
      {},
      {},
      getKDCSessionState,
      []
    );
    await vi.waitFor(() =>
      expect(
        mocks.show.mock.calls.length +
          mocks.showRollingNothingToDoModal.mock.calls.length
      ).toBeGreaterThan(0)
    );
    return getKDCSessionState;
  };

  it("confirms eligible hosts, discloses skipped hosts, and submits one batch for the eligible set", async () => {
    mocks.batchRequest.mockResolvedValue(scheduleResponse(7));
    const getKDCSessionState = await openRecoverConfirmation([
      apiHost("h1", [component("DATANODE", "INSTALLED")]),
      apiHost("h2", [component("DATANODE", "STARTED")]),
    ]);

    const confirmation = mocks.show.mock.calls[0][0];
    expect(confirmation.type).toBe(RecoverHostsModal);
    expect(
      confirmation.props.eligibleHosts.map((h: RecoverHost) => h.hostName)
    ).toEqual(["h1"]);
    expect(confirmation.props.skippedHosts).toEqual([
      {
        hostName: "h2",
        reason: "Components not in a recoverable state: DATANODE",
      },
    ]);
    expect(mocks.batchRequest).not.toHaveBeenCalled();

    confirmation.props.successCallback();

    await vi.waitFor(() => expect(mocks.batchRequest).toHaveBeenCalledTimes(1));
    expect(getKDCSessionState).toHaveBeenCalledTimes(1);
    const [clusterName, payload] = mocks.batchRequest.mock.calls[0];
    expect(clusterName).toBe("c1");
    expect(payload.tolerateSize).toBe(0);
    expect(
      payload.batches.map(
        (batch: RecoverBatchStep) =>
          batch.RequestBodyInfo.RequestInfo.operation_level?.host_name
      )
    ).toEqual(["h1", "h1"]);
    await vi.waitFor(() => expect(mocks.show).toHaveBeenCalledTimes(2));
    expect(mocks.show.mock.calls[1][0].type).toBe(BackgroundOperations);
  });

  it("does not open a confirmation or submit anything when no host is recoverable", async () => {
    await openRecoverConfirmation([
      apiHost("h1", [component("DATANODE", "STARTED")]),
      apiHost("h2", []),
    ]);

    expect(mocks.showRollingNothingToDoModal).toHaveBeenCalledWith(
      "None of the selected hosts can be recovered. Please stop all components on the selected hosts and try again."
    );
    expect(mocks.show).not.toHaveBeenCalled();
    expect(mocks.batchRequest).not.toHaveBeenCalled();
  });

  it("submits nothing when the KDC session is not confirmed", async () => {
    const getKDCSessionState = vi.fn(async () => {});

    await doBulkRecoverHosts(
      recoverOperation(true),
      [host("h1", [component("DATANODE", "INSTALLED")])],
      "c1",
      getKDCSessionState
    );

    expect(getKDCSessionState).toHaveBeenCalledTimes(1);
    expect(mocks.batchRequest).not.toHaveBeenCalled();
  });

  it("reports a failed batch submission and does not open Background Operations", async () => {
    mocks.batchRequest.mockRejectedValue(new Error("schedule rejected"));

    await doBulkRecoverHosts(
      recoverOperation(),
      [host("h1", [component("DATANODE", "INSTALLED")])],
      "c1",
      runKdcCallback
    );

    expect(mocks.showErrorModal).toHaveBeenCalledWith(
      "Recover Hosts schedule rejected"
    );
    expect(mocks.show).not.toHaveBeenCalled();
  });

  it("does not treat a response without a request schedule id as started", async () => {
    mocks.batchRequest.mockResolvedValue({ data: { resources: [] } });

    await doBulkRecoverHosts(
      recoverOperation(),
      [host("h1", [component("DATANODE", "INSTALLED")])],
      "c1",
      runKdcCallback
    );

    expect(mocks.show).not.toHaveBeenCalled();
    expect(mocks.showErrorModal).not.toHaveBeenCalled();
  });

  it("does not pass the request schedule id to Background Operations as a request id", async () => {
    mocks.batchRequest.mockResolvedValue(scheduleResponse(42));

    await doBulkRecoverHosts(
      recoverOperation(),
      [host("h1", [component("DATANODE", "INSTALLED")])],
      "c1",
      runKdcCallback
    );

    const progress = mocks.show.mock.calls[0][0];
    expect(progress.type).toBe(BackgroundOperations);
    expect(progress.props.requestId).toBeUndefined();
  });
});

describe("RecoverHostsModal", () => {
  it("submits only once and reveals skip reasons on demand", () => {
    const successCallback = vi.fn();
    render(
      <RecoverHostsModal
        isOpen={true}
        onClose={vi.fn()}
        eligibleHosts={[{ hostName: "h1" }]}
        skippedHosts={[{ hostName: "h2", reason: "busy components" }]}
        successCallback={successCallback}
      />
    );

    expect(screen.getByText("h1")).toBeTruthy();
    expect(screen.queryByText("busy components")).toBeNull();
    fireEvent.click(screen.getByText("h2"));
    expect(screen.getByText("busy components")).toBeTruthy();

    const okButton = screen.getByRole("button", { name: /ok/i });
    fireEvent.click(okButton);
    fireEvent.click(okButton);

    expect(successCallback).toHaveBeenCalledTimes(1);
  });
});
