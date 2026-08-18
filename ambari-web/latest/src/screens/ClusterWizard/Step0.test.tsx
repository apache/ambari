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

import { createContext } from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ContextWrapper } from ".";
import Step0 from "./Step0";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));
vi.mock("../../components/StepWizard/WizardFooter", () => ({
  default: ({
    isNextEnabled,
    onNext,
  }: {
    isNextEnabled: boolean;
    onNext: () => void;
  }) => (
    <button disabled={!isNextEnabled} onClick={onNext}>NEXT</button>
  ),
}));

describe("cluster Name navigation", () => {
  it("persists the entered name before advancing", async () => {
    let resolvePersistence: () => void = () => undefined;
    const flushStateToDb = vi.fn(() => new Promise<void>((resolve) => {
      resolvePersistence = resolve;
    }));
    const dispatch = vi.fn();
    const handleNextImperitive = vi.fn();
    const value = {
      dispatch,
      flushStateToDb,
      state: { clusterCreationSteps: {} },
      stepWizardUtilities: {
        currentStep: { canGoBack: false, name: "NAME" },
        handleNextImperitive,
      },
    };
    const WizardContext = createContext(value);
    render(
      <ContextWrapper.Provider value={{ Context: WizardContext }}>
        <WizardContext.Provider value={value}>
          <Step0 />
        </WizardContext.Provider>
      </ContextWrapper.Provider>,
    );

    fireEvent.change(screen.getByRole("textbox"), { target: { value: "cluster1" } });
    fireEvent.click(screen.getByRole("button", { name: "NEXT" }));
    expect(dispatch).toHaveBeenCalledWith(expect.objectContaining({
      payload: { data: { clusterName: "cluster1" }, step: "NAME" },
    }));
    expect(flushStateToDb).toHaveBeenCalledWith("next");
    expect(handleNextImperitive).not.toHaveBeenCalled();

    resolvePersistence();
    await waitFor(() => expect(handleNextImperitive).toHaveBeenCalledOnce());
  });
});
