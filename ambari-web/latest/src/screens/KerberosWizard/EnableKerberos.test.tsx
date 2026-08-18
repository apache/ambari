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
import { ComponentProps, ReactNode } from "react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import KerberosApi from "../../api/kerberosApi";
import ClusterApi from "../../api/clusterApi";
import { AppContext } from "../../store/context";
import credentialsUtils from "../../Utils/credentialsUtils";
import EnableKerberos from "./EnableKerberos";

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual<typeof import("react-router-dom")>(
    "react-router-dom",
  );
  return {
    ...actual,
    useBlocker: () => ({ state: "unblocked" }),
  };
});

const testState = vi.hoisted(() => ({
  canDownloadCsv: true,
  userName: "operator",
  mode: {
    isLoaded: true,
    isManualKerberos: false,
    kdcType: "mit-kdc",
    loadError: "",
    retry: vi.fn(),
  },
}));

vi.mock("../../hooks/useKerberosMode", () => ({
  default: () => testState.mode,
}));

vi.mock("../../hooks/useAuth", () => ({
  useAuthorization: () => testState.canDownloadCsv,
  useAuth: () => ({ user: { user_name: testState.userName } }),
}));

vi.mock("../../Utils/credentialsUtils", () => ({
  default: {
    isStorePersisted: vi.fn(),
  },
}));

vi.mock("../../components/UpgradeGuard", () => ({
  default: ({ children }: { children: ReactNode }) => <>{children}</>,
}));

vi.mock("./KerberosIdentities", () => ({
  default: ({ onIdentitiesSaved }: { onIdentitiesSaved?: () => void }) => (
    <button onClick={onIdentitiesSaved}>Simulate identity save</button>
  ),
}));

vi.mock("../Kerberos/RegenerateKeytabs", () => ({
  default: ({ onFinished }: { onFinished: () => void }) => (
    <div>
      Regenerate request started
      <button onClick={onFinished}>Finish regenerate</button>
    </div>
  ),
}));

vi.mock("../Kerberos/DisableKerberos", () => ({
  default: () => <div>Disable workflow</div>,
}));

vi.mock("./index", () => ({
  default: () => <div>Enable wizard</div>,
}));

vi.mock("../../components/Modal", () => ({
  default: ({
    isOpen,
    modalTitle,
    modalBody,
    successCallback,
    options,
  }: {
    isOpen: boolean;
    modalTitle: ReactNode;
    modalBody: ReactNode;
    successCallback: () => void | Promise<void>;
    options: { okButtonDisabled?: boolean; okButtonText?: string };
  }) =>
    isOpen ? (
      <section role="dialog">
        <h2>{modalTitle}</h2>
        {modalBody}
        <button onClick={successCallback} disabled={options.okButtonDisabled}>
          {options.okButtonText ?? "OK"}
        </button>
      </section>
    ) : null,
}));

const renderPage = ({
  contextKerberosEnabled = true,
  services = [],
  supports = { preKerberizeCheck: false },
  initialEntry = "/main/admin/kerberos",
}: {
  contextKerberosEnabled?: boolean;
  services?: Array<{ ServiceInfo?: { service_name?: string } }>;
  supports?: Record<string, boolean>;
  initialEntry?: string;
} = {}) => {
  return render(
    <AppContext.Provider
      value={
        {
          clusterName: "c1",
          services,
          supports,
          isKerberosEnabled: contextKerberosEnabled,
        } as unknown as ComponentProps<typeof AppContext.Provider>["value"]
      }
    >
      <MemoryRouter initialEntries={[initialEntry]}>
        <EnableKerberos />
      </MemoryRouter>
    </AppContext.Provider>,
  );
};

describe("EnableKerberos management entry", () => {
  beforeEach(() => {
    testState.canDownloadCsv = true;
    testState.userName = "operator";
    testState.mode = {
      isLoaded: true,
      isManualKerberos: false,
      kdcType: "mit-kdc",
      loadError: "",
      retry: vi.fn(),
    };
    vi.mocked(credentialsUtils.isStorePersisted).mockResolvedValue(true);
    vi.spyOn(ClusterApi, "postPersistData").mockResolvedValue(
      {} as Awaited<ReturnType<typeof ClusterApi.postPersistData>>,
    );
    vi.spyOn(KerberosApi, "getSecurityType").mockResolvedValue({
      Clusters: { security_type: "KERBEROS" },
    } as Awaited<ReturnType<typeof KerberosApi.getSecurityType>>);
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("hides automatic management controls in Manual mode", async () => {
    testState.mode = {
      ...testState.mode,
      isManualKerberos: true,
      kdcType: "none",
    };
    renderPage();

    expect(await screen.findByText(/Kerberos security is Enabled/)).toBeTruthy();
    expect(
      screen.queryByRole("button", { name: "REGENERATE KEYTABS" }),
    ).toBeNull();
    expect(
      screen.queryByRole("button", { name: "MANAGE KDC CREDENTIALS" }),
    ).toBeNull();
    expect(screen.getByRole("button", { name: "DOWNLOAD CSV" })).toBeTruthy();
  });

  it("shows managed controls only with persistent storage and CSV authorization", async () => {
    testState.canDownloadCsv = false;
    renderPage();

    expect(
      await screen.findByRole("button", { name: "MANAGE KDC CREDENTIALS" }),
    ).toBeTruthy();
    expect(
      screen.getByRole("button", { name: "REGENERATE KEYTABS" }),
    ).toBeTruthy();
    expect(
      screen.queryByRole("button", { name: "DOWNLOAD CSV" }),
    ).toBeNull();
  });

  it("regenerates directly after a Manual identity save", async () => {
    testState.mode = {
      ...testState.mode,
      isManualKerberos: true,
      kdcType: "none",
    };
    renderPage();
    await screen.findByText(/Kerberos security is Enabled/);

    fireEvent.click(screen.getByRole("button", { name: "Simulate identity save" }));

    expect(await screen.findByText("Regenerate request started")).toBeTruthy();
  });

  it("shows the YARN warning before starting Enable", async () => {
    vi.mocked(KerberosApi.getSecurityType).mockResolvedValue({
      Clusters: { security_type: "NONE" },
    } as Awaited<ReturnType<typeof KerberosApi.getSecurityType>>);
    renderPage({
      contextKerberosEnabled: false,
      services: [{ ServiceInfo: { service_name: "YARN" } }],
    });
    await screen.findByText("Kerberos security is disabled");

    fireEvent.click(screen.getByRole("button", { name: "Enable Kerberos" }));

    expect(
      await screen.findByText(/ResourceManager state will be formatted/),
    ).toBeTruthy();
  });

  it("persists wizard ownership before entering Step 1", async () => {
    vi.mocked(KerberosApi.getSecurityType).mockResolvedValue({
      Clusters: { security_type: "NONE" },
    } as Awaited<ReturnType<typeof KerberosApi.getSecurityType>>);
    renderPage({ contextKerberosEnabled: false });
    await screen.findByText("Kerberos security is disabled");

    fireEvent.click(screen.getByRole("button", { name: "Enable Kerberos" }));

    await waitFor(() => expect(ClusterApi.postPersistData).toHaveBeenCalledTimes(1));
    const persisted = JSON.parse(
      vi.mocked(ClusterApi.postPersistData).mock.calls[0][0],
    );
    expect(JSON.parse(persisted.CLUSTER_STATE)).toEqual({
      progressStatus: "ENABLING_KERBEROS",
      stepName: "GET_STARTED",
    });
    expect(JSON.parse(persisted["wizard-data"])).toEqual({
      userName: "operator",
      controllerName: "kerberosWizardController",
    });
    expect(await screen.findByText("Enable wizard")).toBeTruthy();
  });

  it("opens Disable from the Classic deep link", async () => {
    renderPage({ initialEntry: "/main/admin/kerberos/disableSecurity" });

    expect(await screen.findByText("Confirmation")).toBeTruthy();
  });

  it("reloads security state after Disable completes", async () => {
    vi.mocked(KerberosApi.getSecurityType)
      .mockResolvedValueOnce({
        Clusters: { security_type: "KERBEROS" },
      } as Awaited<ReturnType<typeof KerberosApi.getSecurityType>>)
      .mockResolvedValueOnce({
        Clusters: { security_type: "NONE" },
      } as Awaited<ReturnType<typeof KerberosApi.getSecurityType>>);
    renderPage();
    await screen.findByText(/Kerberos security is Enabled/);

    fireEvent.click(screen.getByRole("button", { name: "DISABLE KERBEROS" }));
    fireEvent.click(await screen.findByRole("button", { name: "OK" }));
    expect(await screen.findByText("Disable workflow")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Complete" }));

    expect(await screen.findByText("Kerberos security is disabled")).toBeTruthy();
    expect(KerberosApi.getSecurityType).toHaveBeenCalledTimes(2);
  });

  it("allows Regenerate to be started again after its tracker finishes", async () => {
    renderPage();
    await screen.findByText(/Kerberos security is Enabled/);

    fireEvent.click(screen.getByRole("button", { name: "REGENERATE KEYTABS" }));
    fireEvent.click(await screen.findByRole("button", { name: "OK" }));
    fireEvent.click(await screen.findByRole("button", { name: "OK" }));
    expect(await screen.findByText("Regenerate request started")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Finish regenerate" }));
    await waitFor(() => {
      expect(screen.queryByText("Regenerate request started")).toBeNull();
    });

    fireEvent.click(screen.getByRole("button", { name: "REGENERATE KEYTABS" }));
    expect(await screen.findByText("Regenerate Keytabs")).toBeTruthy();
  });

  it("blocks Enable when an optional pre-Kerberize check fails", async () => {
    vi.mocked(KerberosApi.getSecurityType).mockResolvedValue({
      Clusters: { security_type: "NONE" },
    } as Awaited<ReturnType<typeof KerberosApi.getSecurityType>>);
    vi.spyOn(KerberosApi, "runPreKerberizeChecks").mockResolvedValue({
      items: [{
        UpgradeChecks: {
          id: "KDC_REACHABLE",
          status: "FAIL",
          check: "KDC reachability",
          reason: "The KDC cannot be reached",
        },
      }],
    } as Awaited<ReturnType<typeof KerberosApi.runPreKerberizeChecks>>);
    renderPage({
      contextKerberosEnabled: false,
      supports: { preKerberizeCheck: true },
    });
    await screen.findByText("Kerberos security is disabled");

    fireEvent.click(screen.getByRole("button", { name: "Enable Kerberos" }));

    expect(await screen.findByText("KDC reachability", { exact: false })).toBeTruthy();
    expect(screen.queryByText("Enable wizard")).toBeNull();
  });

  it("reports a pre-Kerberize request failure and permits Retry", async () => {
    vi.mocked(KerberosApi.getSecurityType).mockResolvedValue({
      Clusters: { security_type: "NONE" },
    } as Awaited<ReturnType<typeof KerberosApi.getSecurityType>>);
    vi.spyOn(KerberosApi, "runPreKerberizeChecks").mockRejectedValue(
      new Error("unavailable"),
    );
    renderPage({
      contextKerberosEnabled: false,
      supports: { preKerberizeCheck: true },
    });
    await screen.findByText("Kerberos security is disabled");

    fireEvent.click(screen.getByRole("button", { name: "Enable Kerberos" }));

    expect(await screen.findByText(
      "Ambari could not run the pre-Kerberize checks.",
    )).toBeTruthy();
    expect(screen.getByRole("button", { name: "Retry" })).toBeTruthy();
  });

  it("offers Retry when cluster security status cannot be loaded", async () => {
    vi.mocked(KerberosApi.getSecurityType).mockRejectedValue(
      new Error("status unavailable"),
    );
    renderPage();

    expect(
      await screen.findByText(
        "Ambari could not load the cluster security status.",
      ),
    ).toBeTruthy();
    expect(screen.getByRole("button", { name: "Retry" })).toBeTruthy();
    await waitFor(() => expect(KerberosApi.getSecurityType).toHaveBeenCalled());
  });
});
