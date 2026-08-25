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

import ViewsListPage from "./ViewsListPage";

describe("Views directory", () => {
  beforeEach(() => {
    mocks.context.error = null;
    mocks.context.instances = [{
      contextPath: "/views/TEZ/1.0/INSTANCE",
      description: "Tez jobs",
      iconPath: "",
      instanceName: "INSTANCE",
      label: "Tez",
      shortUrl: "tez",
      version: "1.0",
      viewName: "TEZ",
      visible: true,
    }];
    mocks.context.isLoading = false;
    mocks.context.reload.mockReset();
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("opens the preferred hash route in a new browsing context", () => {
    const open = vi.spyOn(window, "open").mockImplementation(() => null);
    render(<ViewsListPage />);

    fireEvent.click(screen.getByRole("button", { name: /Tez/ }));
    expect(open).toHaveBeenCalledWith(
      "#/main/view/TEZ/tez",
      "_blank",
      "noopener,noreferrer",
    );
  });

  it("distinguishes an empty directory from a recoverable request error", () => {
    mocks.context.instances = [];
    const rendered = render(<ViewsListPage />);
    expect(screen.getByText("No views")).toBeTruthy();

    rendered.unmount();
    mocks.context.error = "View discovery failed";
    render(<ViewsListPage />);
    expect(screen.getByText("View discovery failed")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    expect(mocks.context.reload).toHaveBeenCalledTimes(1);
  });
});
