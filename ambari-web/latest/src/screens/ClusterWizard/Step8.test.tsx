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

import { createContext, type ContextType, type ReactNode } from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../store/context";
import { ContextWrapper } from ".";

const mocks = vi.hoisted(() => ({
  addRequestToCreateComponent: vi.fn(),
  applyClusterConfigs: vi.fn(),
  createCluster: vi.fn(),
  createSelectedServices: vi.fn(),
  deleteCluster: vi.fn(),
  deleteRepositoryVersion: vi.fn(),
  dispatch: vi.fn(),
  flushStateToDb: vi.fn(),
  getKerberosDescriptorArtifact: vi.fn(),
  getKerberosDescriptorProperties: vi.fn(),
  getAllClusters: vi.fn(),
  getAllVersionDefinitions: vi.fn(),
  getServices: vi.fn(),
  handleBackImperitive: vi.fn(),
  handleNextImperitive: vi.fn(),
  kerberosMode: {
    error: "",
    isLoaded: true,
    isManualKerberos: false,
    kdcType: "mit-kdc",
    reload: vi.fn(),
  },
  postVersionDefinitionFile: vi.fn(),
  print: vi.fn(),
  registerHostToCluster: vi.fn(),
  saveAndEditKerberosData: vi.fn(),
  saveAs: vi.fn(),
  saveKerberosData: vi.fn(),
  downloadKerberosIdentitiesCsv: vi.fn(),
  updateRepoOSInfo: vi.fn(),
  updateService: vi.fn(),
}));

vi.mock("../../api/chooseServicesApi", () => ({
  ChooseServicesApi: { getServices: mocks.getServices },
}));
vi.mock("../../api/versionsApi", () => ({
  default: {
    deleteRepositoryVersion: mocks.deleteRepositoryVersion,
    getAllVersionDefinitions: mocks.getAllVersionDefinitions,
    postVersionDefinitionFile: mocks.postVersionDefinitionFile,
    updateRepoOSInfo: mocks.updateRepoOSInfo,
  },
}));
vi.mock("../../api/clusterApi", () => ({
  default: {
    deleteCluster: mocks.deleteCluster,
    getAllClusters: mocks.getAllClusters,
  },
}));
vi.mock("../../api/clusterDeployment", () => ({
  default: {
    addRequestToCreateComponent: mocks.addRequestToCreateComponent,
    applyClusterConfigs: mocks.applyClusterConfigs,
    createCluster: mocks.createCluster,
    createSelectedServices: mocks.createSelectedServices,
    registerHostToCluster: mocks.registerHostToCluster,
  },
}));
vi.mock("../../api/serviceApi", () => ({
  ServiceApi: { updateService: mocks.updateService },
}));
vi.mock("../../api/configGroupApi", () => ({
  default: { addConfigGroup: vi.fn(), updateConfigGroup: vi.fn() },
}));
vi.mock("../../api/kerberosApi", () => ({
  default: {
    downloadKerberosIdentitiesCsv: mocks.downloadKerberosIdentitiesCsv,
    getKerberosDescriptorArtifact: mocks.getKerberosDescriptorArtifact,
    getKerberosDescriptorProperties: mocks.getKerberosDescriptorProperties,
    saveAndEditKerberosData: mocks.saveAndEditKerberosData,
    saveKerberosData: mocks.saveKerberosData,
  },
}));
vi.mock("../../hooks/useKerberosMode", () => ({
  default: () => mocks.kerberosMode,
}));
vi.mock("../../hooks/useKDCSessionState", () => ({
  default: () => ({
    getKDCSessionState: (callback: () => void | Promise<void>) => callback(),
  }),
}));
vi.mock("file-saver", () => ({ saveAs: mocks.saveAs }));
vi.mock("../../components/StepWizard/WizardFooter", () => ({
  default: ({
    isNextEnabled,
    onNext,
    sideItems,
    step,
  }: {
    isNextEnabled: boolean;
    onNext: () => void;
    sideItems: ReactNode;
    step: { nextLabel: string };
  }) => (
    <>
      {sideItems}
      <button disabled={!isNextEnabled} onClick={onNext}>
        {step.nextLabel}
      </button>
    </>
  ),
}));

import Step8 from "./Step8";

const clusterCreationSteps = {
    NAME: { data: { clusterName: "cluster1" } },
    VERSION: {
      data: {
        operatingSystems: { "HDP-3.0": [] },
        selectedStack: { id: "HDP-3.0", stack_name: "HDP", stack_version: "3.0" },
        selectedVersion: { id: "HDP-3.0", stack_version: "3.0" },
        versionDefinitionSource: {
          type: "url",
          payload: { VersionDefinition: { version_url: "https://repo.invalid/vdf.xml" } },
        },
      },
    },
    HOSTS: { data: { installedHosts: [] } },
    HOST_STATUS: {
      data: { hosts: [{ bootStatus: "REGISTERED", name: "host1" }] },
    },
    SERVICES: {
      data: {
        services: {
          HDFS: {
            installed: false,
            selected: true,
            serviceName: "HDFS",
          },
        },
      },
    },
    MASTERS: { data: { mastersData: [] } },
    SLAVES_AND_CLIENTS: { data: { serviceComponents: [] } },
    CONFIGURATION: {
      data: {
        configProperties: {
          HDFS: {
            "core-site": {
              properties: {
                authentication: {
                  fileName: "core-site.xml",
                  isSecureConfig: true,
                  propertyAttributes: { type: "string" },
                  propertyName: "hadoop.security.authentication",
                  type: "core-site",
                  value: "custom-kerberos",
                },
              },
            },
          },
          MISC: { "Users and Groups": { properties: {} } },
        },
      },
    },
    REVIEW: { data: {} },
};

function wizardState(wizardName: "clusterCreation" | "addService") {
  return {
    [`${wizardName}Steps`]: structuredClone(clusterCreationSteps),
  };
}

function renderStep({
  isKerberosEnabled = false,
  wizardName = "clusterCreation",
}: {
  isKerberosEnabled?: boolean;
  wizardName?: "clusterCreation" | "addService";
} = {}) {
  const contextValue = {
    dispatch: mocks.dispatch,
    flushStateToDb: mocks.flushStateToDb,
    state: wizardState(wizardName),
    installedServices: [],
    stepWizardUtilities: {
      currentStep: { name: "REVIEW" },
      handleBackImperitive: mocks.handleBackImperitive,
      handleNextImperitive: mocks.handleNextImperitive,
      jumpToStep: vi.fn(),
    },
  };
  const WizardContext = createContext(contextValue);
  return render(
    <AppContext.Provider value={{
      clusterName: "cluster1",
      cluster: { stack: "HDP", versionNum: "3.0" },
      isKerberosEnabled,
    } as ContextType<typeof AppContext>}>
      <ContextWrapper.Provider value={{ Context: WizardContext }}>
        <WizardContext.Provider value={contextValue}>
          <Step8 wizardName={wizardName} />
        </WizardContext.Provider>
      </ContextWrapper.Provider>
    </AppContext.Provider>,
  );
}

describe("cluster deployment Review", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    Object.defineProperty(window, "print", {
      configurable: true,
      value: mocks.print,
    });
    mocks.flushStateToDb.mockResolvedValue(undefined);
    mocks.getAllClusters.mockResolvedValue({ items: [] });
    mocks.getAllVersionDefinitions.mockResolvedValue({ items: [] });
    mocks.getServices.mockResolvedValue({
      items: [{ StackServices: { service_name: "HDFS" }, components: [] }],
    });
    mocks.postVersionDefinitionFile.mockResolvedValue({
      resources: [{
        VersionDefinition: {
          id: "101",
          stack_name: "HDP",
          stack_version: "3.0",
        },
      }],
    });
    mocks.updateRepoOSInfo.mockResolvedValue({});
    mocks.createSelectedServices.mockResolvedValue({});
    mocks.downloadKerberosIdentitiesCsv.mockResolvedValue(
      "principal,keytab\nservice/host@EXAMPLE.COM,/etc/security/keytabs/service.keytab",
    );
    mocks.getKerberosDescriptorArtifact.mockResolvedValue({ artifact_data: {} });
    mocks.getKerberosDescriptorProperties.mockResolvedValue({
      KerberosDescriptor: {
        kerberos_descriptor: {
          identities: [],
          services: [{
            name: "HDFS",
            configurations: [{
              "core-site": { "hadoop.security.authentication": "kerberos" },
            }],
          }],
        },
      },
    });
    mocks.kerberosMode.error = "";
    mocks.kerberosMode.isLoaded = true;
    mocks.kerberosMode.isManualKerberos = false;
    mocks.kerberosMode.kdcType = "mit-kdc";
    mocks.applyClusterConfigs.mockResolvedValue({});
    mocks.registerHostToCluster.mockResolvedValue({});
    mocks.updateService.mockResolvedValue({ Requests: { id: 41 } });
  });

  afterEach(() => cleanup());

  it("retains a custom VDF source, blocks install on failure, and resumes completed stages", async () => {
    mocks.createCluster
      .mockRejectedValueOnce(new Error("Cluster creation failed"))
      .mockResolvedValueOnce({});
    renderStep();
    await screen.findByRole("button", { name: "DEPLOY" });

    fireEvent.click(screen.getByRole("button", { name: "DEPLOY" }));
    expect(await screen.findByText("Cluster creation failed")).toBeTruthy();
    expect(mocks.updateService).not.toHaveBeenCalled();
    expect(mocks.postVersionDefinitionFile).toHaveBeenCalledWith(
      { VersionDefinition: { version_url: "https://repo.invalid/vdf.xml" } },
      {},
    );

    fireEvent.click(screen.getByRole("button", { name: "DEPLOY" }));
    await waitFor(() => expect(mocks.handleNextImperitive).toHaveBeenCalledOnce());
    expect(mocks.postVersionDefinitionFile).toHaveBeenCalledOnce();
    expect(mocks.createCluster).toHaveBeenCalledTimes(2);
    expect(mocks.updateService).toHaveBeenCalledOnce();
    expect(mocks.flushStateToDb).toHaveBeenLastCalledWith(
      "next",
      -1,
      "CLUSTER_INSTALLING_3",
    );
  });

  it("prints Review and downloads the Blueprint archive", async () => {
    renderStep();
    await screen.findByRole("button", { name: "DEPLOY" });

    fireEvent.click(screen.getByRole("button", { name: "Print Review" }));
    expect(mocks.print).toHaveBeenCalledOnce();

    fireEvent.click(screen.getByRole("button", { name: "Generate Blueprint" }));
    await waitFor(() => expect(mocks.saveAs).toHaveBeenCalledWith(
      expect.any(Blob),
      "cluster1-blueprint.zip",
    ));
  });

  it("prefetches identities and updates the existing descriptor for managed Kerberos", async () => {
    renderStep({ isKerberosEnabled: true, wizardName: "addService" });

    expect(await screen.findByText("Kerberos KDC type: mit-kdc")).toBeTruthy();
    expect(mocks.downloadKerberosIdentitiesCsv).toHaveBeenCalledWith("cluster1");

    fireEvent.click(screen.getByRole("button", { name: "DEPLOY" }));
    await waitFor(() => expect(mocks.saveAndEditKerberosData).toHaveBeenCalledOnce());
    expect(mocks.saveAndEditKerberosData).toHaveBeenCalledWith(
      "cluster1",
      expect.objectContaining({
        artifact_data: expect.objectContaining({
          services: [expect.objectContaining({
            configurations: [{
              "core-site": {
                "hadoop.security.authentication": "custom-kerberos",
              },
            }],
          })],
        }),
      }),
    );
    await waitFor(() => expect(mocks.handleNextImperitive).toHaveBeenCalledOnce());
  });

  it("creates the descriptor early and explains principal ownership for manual Kerberos", async () => {
    mocks.kerberosMode.isManualKerberos = true;
    mocks.kerberosMode.kdcType = "none";
    mocks.getKerberosDescriptorArtifact.mockRejectedValueOnce({
      response: { status: 404 },
    });

    renderStep({ isKerberosEnabled: true, wizardName: "addService" });

    expect(await screen.findByText(/you must create and distribute principals and keytabs/)).toBeTruthy();
    expect(mocks.saveKerberosData).toHaveBeenCalledOnce();
    expect(mocks.saveAndEditKerberosData).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: "Download Kerberos CSV" }));
    await waitFor(() => expect(mocks.saveAs).toHaveBeenCalledOnce());
  });

  it("blocks deployment when descriptor validation fails and allows preparation retry", async () => {
    mocks.getKerberosDescriptorProperties
      .mockResolvedValueOnce({ KerberosDescriptor: { kerberos_descriptor: {} } })
      .mockResolvedValue({
        KerberosDescriptor: {
          kerberos_descriptor: { identities: [], services: [] },
        },
      });
    renderStep({ isKerberosEnabled: true, wizardName: "addService" });

    expect(await screen.findByText("Ambari returned an invalid Kerberos descriptor.")).toBeTruthy();
    expect((screen.getByRole("button", { name: "DEPLOY" }) as HTMLButtonElement).disabled).toBe(true);

    fireEvent.click(screen.getByRole("button", { name: "Retry Kerberos Preparation" }));
    await waitFor(() => {
      expect((screen.getByRole("button", { name: "DEPLOY" }) as HTMLButtonElement).disabled).toBe(false);
    });
  });
});
