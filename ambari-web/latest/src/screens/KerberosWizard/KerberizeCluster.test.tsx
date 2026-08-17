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

import { cleanup, render, waitFor } from "@testing-library/react";
import { ComponentProps } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { RequestApi } from "../../api/requestApi";
import { ProgressStatus } from "../../constants";
import { AppContext } from "../../store/context";
import OperationsProgress from "../../components/OperationsProgress";
import KerberizeCluster from "./KerberizeCluster";
import { EnableKerberosContext } from "./KerberosStore/context";

type ProgressProps = ComponentProps<typeof OperationsProgress>;

const progressState = vi.hoisted(() => ({
  props: null as ProgressProps | null,
}));

vi.mock("../../components/OperationsProgress", () => ({
  default: (props: ProgressProps) => {
    progressState.props = props;
    return <div>Kerberize operation</div>;
  },
}));

vi.mock("../../components/StepWizard/WizardFooter", () => ({
  default: () => null,
}));

vi.mock("../../hooks/useKDCSessionState", () => ({
  default: () => ({
    getKDCSessionState: async (
      callback: () => Promise<void>,
      _errorCallback: (error: unknown) => void,
      _options: { forceCheck?: boolean },
    ) => callback(),
  }),
}));

const wizardSteps = {
  1: { name: "GET_STARTED" },
  7: { name: "KERBERIZE_CLUSTER" },
};

const renderStep = (savedOperationsState?: Array<{
  id: string;
  label: string;
  status?: string;
  requestId?: number;
}>) =>
  render(
    <AppContext.Provider
      value={
        { clusterName: "c1" } as unknown as ComponentProps<
          typeof AppContext.Provider
        >["value"]
      }
    >
      <EnableKerberosContext.Provider
        value={
          {
            state: {
              kerberosWizardSteps: savedOperationsState
                ? {
                    KERBERIZE_CLUSTER: {
                      data: { operationsState: savedOperationsState },
                    },
                  }
                : {},
            },
            dispatch: vi.fn(),
            flushStateToDb: vi.fn(),
            onExitPopUp: vi.fn(),
            stepWizardUtilities: {
              currentStep: wizardSteps[7],
              wizardSteps,
              handleNextImperitive: vi.fn(),
              jumpToStep: vi.fn(),
            },
          } as ComponentProps<typeof EnableKerberosContext.Provider>["value"]
        }
      >
        <KerberizeCluster />
      </EnableKerberosContext.Provider>
    </AppContext.Provider>,
  );

describe("KerberizeCluster", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
    progressState.props = null;
  });

  it("forces the first retry after recovering a failed operation", async () => {
    const prepare = vi
      .spyOn(RequestApi, "preparingOperations")
      .mockResolvedValue(
        {} as Awaited<ReturnType<typeof RequestApi.preparingOperations>>,
      );
    renderStep([
      {
        id: "1",
        label: "Preparing Operations",
        status: ProgressStatus.FAILED,
        requestId: 42,
      },
    ]);

    await waitFor(() => expect(progressState.props).not.toBeNull());
    await progressState.props!.operations[0].callback();

    expect(prepare).toHaveBeenCalledWith(
      "c1",
      { Clusters: { security_type: "KERBEROS" } },
      "force_toggle_kerberos=true",
    );
  });

  it("does not force the initial kerberize request", async () => {
    const prepare = vi
      .spyOn(RequestApi, "preparingOperations")
      .mockResolvedValue(
        {} as Awaited<ReturnType<typeof RequestApi.preparingOperations>>,
      );
    renderStep();

    await waitFor(() => expect(progressState.props).not.toBeNull());
    await progressState.props!.operations[0].callback();

    expect(prepare).toHaveBeenCalledWith(
      "c1",
      { Clusters: { security_type: "KERBEROS" } },
      "",
    );
  });
});
