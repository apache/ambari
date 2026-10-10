/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it } from "vitest";
import HostAlertBadge from "./HostAlertBadge";

const mount = (critical: number, warning: number) => render(
  <MemoryRouter initialEntries={["/clusters/example/main/hosts"]}><Routes>
    <Route path="/clusters/:clusterName/main/hosts" element={<HostAlertBadge hostName="worker3.example" critical={critical} warning={warning} />} />
  </Routes></MemoryRouter>,
);

describe("host alert navigation badge", () => {
  it("keeps combined counts and host-scoped navigation without nesting a button", () => {
    mount(2, 1);
    const link = screen.getByRole("link", { name: "View alerts for worker3.example: 2 critical, 1 warning" });
    expect(link.textContent).toBe("3");
    expect(link.getAttribute("href")).toBe("/clusters/example/main/hosts/worker3.example/alerts");
    expect(link.classList.contains("host-alert-badge--critical")).toBe(true);
    expect(screen.queryByRole("button")).toBeNull();
  });
  it("distinguishes warning-only hosts and omits an empty badge", () => {
    const view = mount(0, 2);
    expect(screen.getByRole("link").classList.contains("host-alert-badge--warning")).toBe(true);
    view.unmount();
    mount(0, 0);
    expect(screen.queryByRole("link")).toBeNull();
  });
});
