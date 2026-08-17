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
import KerberosApi from "../../api/kerberosApi";
import { AppContext } from "../../store/context";
import OperationsProgress from "../../components/OperationsProgress";
import StopServices from "./StopServices";
import { EnableKerberosContext } from "./KerberosStore/context";

type ProgressProps = ComponentProps<typeof OperationsProgress>;

const progressState = vi.hoisted(() => ({
  props: null as ProgressProps | null,
}));

vi.mock("../../components/OperationsProgress", () => ({
  default: (props: ProgressProps) => {
    progressState.props = props;
    return <div>Stop operations</div>;
  },
}));

vi.mock("../../components/StepWizard/WizardFooter", () => ({
  default: () => null,
}));

const stack = (cardinality: string) => ({
  items: [{
    StackServices: { service_name: "YARN" },
    components: [{
      StackServiceComponents: {
        component_name: "APP_TIMELINE_SERVER",
        cardinality,
      },
    }],
  }],
});

const renderStep = (serviceComponentInfo: ReturnType<typeof stack>) => render(
  <AppContext.Provider
    value={
      {
        clusterName: "c1",
        services: [{ ServiceInfo: { service_name: "YARN" } }],
        serviceComponentInfo,
      } as unknown as ComponentProps<typeof AppContext.Provider>["value"]
    }
  >
    <EnableKerberosContext.Provider
      value={
        {
          state: { kerberosWizardSteps: {} },
          dispatch: vi.fn(),
          flushStateToDb: vi.fn(),
          onExitPopUp: vi.fn(),
          stepWizardUtilities: {
            wizardSteps: { 6: { name: "STOP_SERVICES" } },
            currentStep: { name: "STOP_SERVICES" },
            handleNextImperitive: vi.fn(),
            jumpToStep: vi.fn(),
          },
        } as ComponentProps<typeof EnableKerberosContext.Provider>["value"]
      }
    >
      <StopServices />
    </EnableKerberosContext.Provider>
  </AppContext.Provider>,
);

describe("StopServices", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
    progressState.props = null;
  });

  it("adds ATS removal after Stop Services when the stack cannot Kerberize ATS", async () => {
    vi.spyOn(KerberosApi, "getAppTimelineServerHosts").mockResolvedValue({
      items: [{ HostRoles: { host_name: "ats.example" } }],
    } as Awaited<ReturnType<typeof KerberosApi.getAppTimelineServerHosts>>);
    const deleteATS = vi.spyOn(KerberosApi, "deleteAppTimelineServer")
      .mockResolvedValue(
        { status: 200 } as Awaited<
          ReturnType<typeof KerberosApi.deleteAppTimelineServer>
        >,
      );
    renderStep(stack("0-1"));

    await waitFor(() => expect(progressState.props?.operations).toHaveLength(2));
    expect(progressState.props!.operations.map((operation) => operation.label))
      .toEqual(["Stop services", "Delete ATS"]);
    await progressState.props!.operations[1].callback();
    expect(deleteATS).toHaveBeenCalledWith("c1", "ats.example");
  });

  it("does not query or remove ATS when its stack cardinality requires it", async () => {
    const discover = vi.spyOn(KerberosApi, "getAppTimelineServerHosts");
    renderStep(stack("1"));

    await waitFor(() => expect(progressState.props?.operations).toHaveLength(1));
    expect(discover).not.toHaveBeenCalled();
  });

  it("treats an already removed ATS component as completed", async () => {
    vi.spyOn(KerberosApi, "getAppTimelineServerHosts").mockResolvedValue({
      items: [{ HostRoles: { host_name: "ats.example" } }],
    } as Awaited<ReturnType<typeof KerberosApi.getAppTimelineServerHosts>>);
    vi.spyOn(KerberosApi, "deleteAppTimelineServer").mockRejectedValue({
      response: { status: 404 },
    });
    renderStep(stack("0-1"));

    await waitFor(() => expect(progressState.props?.operations).toHaveLength(2));
    await expect(progressState.props!.operations[1].callback())
      .resolves.toEqual({ status: 204 });
  });
});
