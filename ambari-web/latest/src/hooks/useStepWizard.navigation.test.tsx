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
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { afterEach, describe, expect, it } from "vitest";
import useStepWizard from "./useStepWizard";

afterEach(cleanup);
function Wizard() {
  const wizard = useStepWizard([{ name: "START" }, { name: "VERSION" }]);
  const location = useLocation();
  return <><output data-testid="route">{location.pathname}{location.search}</output>
    <button onClick={() => wizard.handleNextImperitive()}>Next</button>
    <button onClick={() => wizard.handleBackImperitive()}>Back</button></>;
}
describe("wizard navigation lineage", () => {
  it("preserves the exact draft ID on mount, next and back", async () => {
    const query = "?draft=00000000-0000-4000-8000-000000000001";
    render(<MemoryRouter initialEntries={[`/installer/step0${query}`]}>
      <Routes><Route path="/installer/:stepNumber" element={<Wizard />} /></Routes>
    </MemoryRouter>);
    expect(screen.getByTestId("route").textContent).toBe(`/installer/step0${query}`);
    fireEvent.click(screen.getByRole("button", { name: "Next" }));
    await waitFor(() => expect(screen.getByTestId("route").textContent).toBe(`/installer/step1${query}`));
    fireEvent.click(screen.getByRole("button", { name: "Back" }));
    await waitFor(() => expect(screen.getByTestId("route").textContent).toBe(`/installer/step0${query}`));
  });
  it("retains cluster identity and recovery query in an existing cluster wizard", async () => {
    render(<MemoryRouter initialEntries={["/clusters/alpha/main/service/add/step0?deployment=operation-1"]}>
      <Routes><Route path="/clusters/:clusterName/main/service/add/:stepNumber" element={<Wizard />} /></Routes>
    </MemoryRouter>);
    fireEvent.click(screen.getByRole("button", { name: "Next" }));
    await waitFor(() => expect(screen.getByTestId("route").textContent)
      .toBe("/clusters/alpha/main/service/add/step1?deployment=operation-1"));
  });
});
