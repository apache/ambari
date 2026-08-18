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

import { cleanup, render } from "@testing-library/react";
import { ComponentProps } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import KerberosApi from "../../api/kerberosApi";
import { RequestApi } from "../../api/requestApi";
import { AppContext } from "../../store/context";
import DisableKerberos from "./DisableKerberos";

const progressState = vi.hoisted(() => ({ props: null as any }));

vi.mock("../../components/OperationsProgress", () => ({
  default: (props: any) => {
    progressState.props = props;
    return <div>Disable operations</div>;
  },
}));

vi.mock("../../hooks/useKDCSessionState", () => ({
  default: () => ({
    getKDCSessionState: async (callback: () => Promise<void>) => callback(),
  }),
}));

const renderDisable = () =>
  render(
    <AppContext.Provider
      value={
        {
          clusterName: "c1",
          services: [
            { ServiceInfo: { service_name: "ZOOKEEPER" } },
            { ServiceInfo: { service_name: "HDFS" } },
            { ServiceInfo: { service_name: "KERBEROS" } },
          ],
          ambariProperties: { "skip.service.checks": "true" },
        } as unknown as ComponentProps<typeof AppContext.Provider>["value"]
      }
    >
      <DisableKerberos setDisableKerberosInProgress={vi.fn()} />
    </AppContext.Provider>,
  );

describe("DisableKerberos", () => {
  beforeEach(() => {
    progressState.props = null;
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("deletes the Kerberos service in the remove operation", async () => {
    const remove = vi
      .spyOn(KerberosApi, "deleteKerberosService")
      .mockResolvedValue({} as any);
    renderDisable();

    await progressState.props.operations[3].callback();

    expect(remove).toHaveBeenCalledWith("c1", "KERBEROS");
  });

  it("uses the no-identity-management unkerberize skip branch", async () => {
    const unkerberize = vi
      .spyOn(RequestApi, "preparingOperations")
      .mockResolvedValue({} as any);
    renderDisable();

    const operation = progressState.props.operations[2];
    expect(operation.skippable).toBe(true);
    await operation.skipCallback();

    expect(unkerberize).toHaveBeenCalledWith(
      "c1",
      { Clusters: { security_type: "NONE" } },
      "manage_kerberos_identities=false",
    );
  });

  it("honors skip.service.checks when starting services", async () => {
    const start = vi
      .spyOn(RequestApi, "startServices")
      .mockResolvedValue({} as any);
    renderDisable();

    await progressState.props.operations[4].callback();

    expect(start).toHaveBeenCalledWith(
      "c1",
      expect.any(Object),
      "run_smoke_test=false",
    );
  });
});
