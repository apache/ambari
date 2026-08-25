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
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({ getInstances: vi.fn() }));
vi.mock("../../api/viewApi", () => ({
  default: { getInstances: mocks.getInstances },
}));

import { useViewInstances, ViewInstancesProvider } from "./ViewInstancesContext";

function Consumer() {
  const { error, instances, isLoading, reload } = useViewInstances();
  if (isLoading) return <p>Loading</p>;
  return (
    <div>
      <p>{error || instances.map((instance) => instance.label).join(",") || "Empty"}</p>
      <button onClick={() => void reload()}>Reload</button>
    </div>
  );
}

describe("ViewInstancesProvider", () => {
  beforeEach(() => mocks.getInstances.mockReset());
  afterEach(cleanup);

  it("publishes only visible instances", async () => {
    mocks.getInstances.mockResolvedValue({
      items: [{
        ViewInfo: { view_name: "TEZ" },
        versions: [{
          ViewVersionInfo: { version: "1.0" },
          instances: [
            { ViewInstanceInfo: {
              context_path: "/views/TEZ/1.0/visible",
              instance_name: "visible",
              label: "Visible",
              visible: true,
            } },
            { ViewInstanceInfo: {
              context_path: "/views/TEZ/1.0/hidden",
              instance_name: "hidden",
              label: "Hidden",
              visible: false,
            } },
          ],
        }],
      }],
    });

    render(<ViewInstancesProvider><Consumer /></ViewInstancesProvider>);
    expect(await screen.findByText("Visible")).toBeTruthy();
    expect(screen.queryByText("Hidden")).toBeNull();
  });

  it("clears stale instances and retries a failed discovery", async () => {
    mocks.getInstances
      .mockRejectedValueOnce(new Error("failed"))
      .mockResolvedValueOnce({ items: [] });

    render(<ViewInstancesProvider><Consumer /></ViewInstancesProvider>);
    expect(await screen.findByText("Ambari could not load the available Views.")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Reload" }));
    await waitFor(() => expect(screen.getByText("Empty")).toBeTruthy());
    expect(mocks.getInstances).toHaveBeenCalledTimes(2);
  });
});
