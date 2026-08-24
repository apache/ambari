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
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  MemoryRouter,
  Route,
  Routes,
  useNavigate,
} from "react-router-dom";
import { ViewInstance } from "../../Utils/viewUtils";

const mocks = vi.hoisted(() => ({
  context: {
    error: null as string | null,
    instances: [] as ViewInstance[],
    isLoading: false,
    reload: vi.fn(),
  },
}));

vi.mock("./ViewInstancesContext", () => ({
  useViewInstances: () => mocks.context,
}));

import ViewDetails from "./ViewDetails";

const instance: ViewInstance = {
  contextPath: "/gateway/default/views/TEZ/1.0/INSTANCE",
  description: "Tez jobs",
  iconPath: "",
  instanceName: "INSTANCE",
  label: "Tez",
  shortUrl: "tez",
  version: "1.0",
  viewName: "TEZ",
  visible: true,
};

function renderDetails(path: string, route: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path={route} element={<ViewDetails />} />
        <Route path="/main/view" element={<div>View directory</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

function WarmNavigationHarness() {
  const navigate = useNavigate();
  return (
    <>
      <button onClick={() => navigate("/main/views/TEZ/1.0/DELETED")}>Open deleted View</button>
      <Routes>
        <Route
          path="/main/views/:viewName/:viewVersion/:instanceName/*"
          element={<ViewDetails />}
        />
        <Route path="/main/view" element={<div>View directory</div>} />
      </Routes>
    </>
  );
}

describe("View details", () => {
  beforeEach(() => {
    vi.spyOn(window, "scrollTo").mockImplementation(() => undefined);
    mocks.context.error = null;
    mocks.context.instances = [instance];
    mocks.context.isLoading = false;
    mocks.context.reload.mockReset();
  });

  afterEach(() => {
    cleanup();
    document.body.classList.remove("contrib-view", "contribview");
    vi.restoreAllMocks();
  });

  it("hosts a regular deep link at the server-returned context path", () => {
    renderDetails(
      "/main/views/TEZ/1.0/INSTANCE?viewPath=%2F%23%2Ftez-app%2Fapplication_1",
      "/main/views/:viewName/:viewVersion/:instanceName/*",
    );

    expect(screen.getByTitle("Tez View").getAttribute("src")).toBe(
      "http://localhost:3000/gateway/default/views/TEZ/1.0/INSTANCE/#/tez-app/application_1",
    );
    expect(document.body.classList.contains("contrib-view")).toBe(true);
    expect(document.body.classList.contains("contribview")).toBe(true);
  });

  it("matches the short route and recovers when the instance disappears", () => {
    const rendered = renderDetails(
      "/main/view/TEZ/tez",
      "/main/view/:viewName/:shortName/*",
    );
    expect(screen.getByTitle("Tez View")).toBeTruthy();

    rendered.unmount();
    mocks.context.instances = [];
    renderDetails(
      "/main/view/TEZ/deleted",
      "/main/view/:viewName/:shortName/*",
    );
    expect(screen.getByText("View not available")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Return to Views" }));
    expect(screen.getByText("View directory")).toBeTruthy();
  });

  it("does not reuse the previous iframe after warm navigation to an unknown View", async () => {
    render(
      <MemoryRouter initialEntries={["/main/views/TEZ/1.0/INSTANCE"]}>
        <WarmNavigationHarness />
      </MemoryRouter>,
    );
    expect(screen.getByTitle("Tez View")).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: "Open deleted View" }));
    expect(await screen.findByText("View not available")).toBeTruthy();
    expect(screen.queryByTitle("Tez View")).toBeNull();
  });

  it("keeps a failed directory load recoverable", () => {
    mocks.context.error = "View discovery failed";
    renderDetails(
      "/main/views/TEZ/1.0/INSTANCE",
      "/main/views/:viewName/:viewVersion/:instanceName/*",
    );

    expect(screen.getByText("View discovery failed")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    expect(mocks.context.reload).toHaveBeenCalledTimes(1);
  });
});
