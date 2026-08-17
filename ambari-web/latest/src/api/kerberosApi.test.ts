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

const mocks = vi.hoisted(() => ({ request: vi.fn() }));
vi.mock("./config/axiosConfig", () => ({
  ambariApi: { request: mocks.request },
  supressErrorAmbariApi: { request: mocks.request },
}));

import KerberosApi from "./kerberosApi";

describe("Kerberos API", () => {
  beforeEach(() => {
    mocks.request.mockReset();
    mocks.request.mockResolvedValue({ data: {} });
  });

  it("encodes cluster names and KDC host lists as path segments", async () => {
    await KerberosApi.getSecurityType("cluster/name");
    await KerberosApi.testKdcConnection("kdc1.example,kdc2.example");

    expect(mocks.request).toHaveBeenNthCalledWith(1, {
      url: "clusters/cluster%2Fname?fields=Clusters/security_type",
      method: "GET",
    });
    expect(mocks.request).toHaveBeenNthCalledWith(2, {
      url: "kdc_check/kdc1.example%2Ckdc2.example",
      method: "GET",
    });
  });

  it("preserves the Classic pre-Kerberize check root contract", async () => {
    await KerberosApi.runPreKerberizeChecks();

    expect(mocks.request).toHaveBeenCalledWith({
      url: "",
      method: "GET",
    });
  });

  it("uses exact encoded ATS discovery and deletion resources", async () => {
    mocks.request
      .mockResolvedValueOnce({ data: { items: [] } })
      .mockResolvedValueOnce({ data: { deleted: true }, status: 202 });
    await KerberosApi.getAppTimelineServerHosts("cluster/name");
    await expect(
      KerberosApi.deleteAppTimelineServer("cluster/name", "host/one"),
    ).resolves.toEqual({ deleted: true, status: 202 });

    expect(mocks.request).toHaveBeenNthCalledWith(1, {
      url: "clusters/cluster%2Fname/host_components?HostRoles/component_name=APP_TIMELINE_SERVER&fields=HostRoles/host_name&minimal_response=true",
      method: "GET",
    });
    expect(mocks.request).toHaveBeenNthCalledWith(2, {
      url: "clusters/cluster%2Fname/hosts/host%2Fone/host_components/APP_TIMELINE_SERVER",
      method: "DELETE",
    });
  });

  it("retains the HTTP status for a successful Kerberos service deletion", async () => {
    mocks.request.mockResolvedValueOnce({ data: {}, status: 204 });

    await expect(KerberosApi.deleteKerberosService("c1", "KERBEROS"))
      .resolves.toEqual({ status: 204 });
  });

  it("uses the Classic Step 3 client state and install contracts", async () => {
    mocks.request
      .mockResolvedValueOnce({ data: { ServiceComponentInfo: { state: "INIT" } } })
      .mockResolvedValueOnce({ data: { Requests: { id: 7 } }, status: 202 })
      .mockResolvedValueOnce({ data: { Requests: { id: 8 } }, status: 202 });

    await KerberosApi.getKerberosClientState("cluster/name");
    await KerberosApi.installKerberosService("cluster/name");
    await KerberosApi.installKerberosClients(
      "cluster/name",
      ["host1.example", "host2.example"],
    );

    expect(mocks.request).toHaveBeenNthCalledWith(1, {
      url: "clusters/cluster%2Fname/services/KERBEROS/components/KERBEROS_CLIENT?fields=ServiceComponentInfo/state",
      method: "GET",
    });
    expect(mocks.request).toHaveBeenNthCalledWith(2, {
      url: "clusters/cluster%2Fname/services?ServiceInfo/state=INSTALLED&ServiceInfo/service_name=KERBEROS",
      method: "PUT",
      data: {
        RequestInfo: {
          context: "Install Kerberos Service",
          operation_level: {
            level: "CLUSTER",
            cluster_name: "cluster/name",
          },
        },
        Body: { ServiceInfo: { state: "INSTALLED" } },
      },
      headers: { "Content-Type": "text/plain" },
    });
    expect(mocks.request).toHaveBeenNthCalledWith(3, {
      url: "clusters/cluster%2Fname/host_components",
      method: "PUT",
      data: {
        RequestInfo: {
          context: "Install Kerberos Client",
          operation_level: {
            level: "CLUSTER",
            cluster_name: "cluster/name",
          },
          query: "HostRoles/component_name=KERBEROS_CLIENT&HostRoles/host_name.in(host1.example,host2.example)&HostRoles/maintenance_state=OFF",
        },
        Body: { HostRoles: { state: "INSTALLED" } },
      },
      headers: { "Content-Type": "text/plain" },
    });
  });

  it("falls back from descriptor create to update only on conflict", async () => {
    mocks.request
      .mockRejectedValueOnce({ response: { status: 409 } })
      .mockResolvedValueOnce({ data: { updated: true } });

    await expect(KerberosApi.createKerberosDescriptor("c1", { artifact_data: {} }))
      .resolves.toEqual({ updated: true });
    expect(mocks.request).toHaveBeenNthCalledWith(1, expect.objectContaining({ method: "POST" }));
    expect(mocks.request).toHaveBeenNthCalledWith(2, expect.objectContaining({ method: "PUT" }));
  });

  it("falls back from descriptor update to create only when it is absent", async () => {
    mocks.request
      .mockRejectedValueOnce({ response: { status: 404 } })
      .mockResolvedValueOnce({ data: { created: true } });

    await expect(KerberosApi.updateKerberosDescriptor("c1", { artifact_data: {} }))
      .resolves.toEqual({ created: true });
    expect(mocks.request).toHaveBeenNthCalledWith(1, expect.objectContaining({ method: "PUT" }));
    expect(mocks.request).toHaveBeenNthCalledWith(2, expect.objectContaining({ method: "POST" }));
  });

  it("does not convert authorization and server failures into creates", async () => {
    const error = { response: { status: 403 } };
    mocks.request.mockRejectedValueOnce(error);

    await expect(KerberosApi.updateKerberosDescriptor("c1", {})).rejects.toBe(error);
    expect(mocks.request).toHaveBeenCalledTimes(1);
  });
});
