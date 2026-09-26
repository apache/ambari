/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import type { ReactNode } from "react";

vi.mock("../../store/context.tsx", async () => {
  const { createContext } = await import("react");
  return { AppContext: createContext({ clusterName: "reference", supports: {},
    services: ["AIRFLOW", "CELEBORN", "Gateway.Mixed"].map(service_name => ({
      ServiceInfo: { service_name, state: "STARTED" },
    })) }) };
});
vi.mock("../../store/ServiceContext.tsx", async () => {
  const { createContext } = await import("react");
  return { ServiceContext: createContext({ allServiceModels: { reference: {} },
    serviceStatesData: new Map(), polledHostComponentsData: { items: [] } }) };
});
vi.mock("../../hooks/useAuthorizationPolicy", () => ({
  default: () => ({ havePermissions: () => true, isAuthorized: () => true }),
}));
vi.mock("../../constants.ts", () => ({
  serviceNameModelMapping: {},
  serviceNameDisplayMapping: { AIRFLOW: "Apache Airflow", CELEBORN: "Apache Celeborn", "Gateway.Mixed": "Friendly Gateway" },
}));
vi.mock("../../screens/ClusterWizard/constants", () => ({ displayOrder: [] }));
vi.mock("./SidebarItem", () => ({
  default: ({ ele }: { ele: { path: string; name: ReactNode } }) => <a href={ele.path}>{ele.name}</a>,
}));
vi.mock("./SidebarItemCollapsed", () => ({ default: () => null }));
import SideBar from "./Sidebar";
import "../../i18n";

describe("declared service navigation", () => {
  it("keeps API service identity independent of branded labels and capitalization", async () => {
    render(<MemoryRouter initialEntries={["/main/services/AIRFLOW/summary"]}>
      <SideBar isSidebarCollapsed={false} setIsSidebarCollapsed={vi.fn()} />
    </MemoryRouter>);
    expect((await screen.findByRole("link", { name: "Apache Airflow" })).getAttribute("href"))
      .toBe("/main/services/AIRFLOW/summary");
    expect(screen.getByRole("link", { name: "Apache Celeborn" }).getAttribute("href"))
      .toBe("/main/services/CELEBORN/summary");
    expect(screen.getByRole("link", { name: "Friendly Gateway" }).getAttribute("href"))
      .toBe("/main/services/Gateway.Mixed/summary");
  });
});
