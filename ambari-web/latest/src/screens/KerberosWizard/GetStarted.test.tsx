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

import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { ComponentProps } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../store/context";
import { createKerberosPreconditionOptions } from "./constants";
import GetStartedKerberos from "./GetStarted";
import { EnableKerberosContext } from "./KerberosStore/context";

vi.mock("../../components/StepWizard/WizardFooter", () => ({
  default: ({ isNextEnabled, onNext }: {
    isNextEnabled: boolean;
    onNext: () => void;
  }) => (
    <button disabled={!isNextEnabled} onClick={onNext}>Next</button>
  ),
}));

describe("GetStartedKerberos", () => {
  afterEach(cleanup);

  it("restores the selected mode and completed prerequisites", () => {
    const preconditions = createKerberosPreconditionOptions([]);
    Object.values(preconditions["Existing Active Directory"].Options)
      .forEach((_value, index) => {
        const key = Object.keys(
          preconditions["Existing Active Directory"].Options,
        )[index];
        preconditions["Existing Active Directory"].Options[key] = true;
      });
    const dispatch = vi.fn();
    const currentStep = { name: "GET_STARTED" };

    render(
      <AppContext.Provider
        value={
          { services: [] } as unknown as ComponentProps<
            typeof AppContext.Provider
          >["value"]
        }
      >
        <EnableKerberosContext.Provider
          value={{
            state: {
              kerberosWizardSteps: {
                GET_STARTED: {
                  data: {
                    selectedKdcPlan: "Existing Active Directory",
                    preconditions,
                  },
                },
              },
            },
            dispatch,
            flushStateToDb: vi.fn(),
            onExitPopUp: vi.fn(),
            stepWizardUtilities: {
              currentStep,
              handleNextImperitive: vi.fn(),
            },
          }}
        >
          <GetStartedKerberos />
        </EnableKerberosContext.Provider>
      </AppContext.Provider>,
    );

    expect((screen.getByLabelText(
      "Existing Active Directory",
    ) as HTMLInputElement).checked).toBe(true);
    const next = screen.getByRole("button", { name: "Next" });
    expect(next.hasAttribute("disabled")).toBe(false);

    fireEvent.click(next);
    expect(dispatch).toHaveBeenCalledWith(expect.objectContaining({
      payload: expect.objectContaining({
        data: expect.objectContaining({
          selectedKdcPlan: "Existing Active Directory",
        }),
      }),
    }));
  });
});
