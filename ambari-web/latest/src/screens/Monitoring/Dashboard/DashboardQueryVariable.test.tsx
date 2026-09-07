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
import { afterEach, describe, expect, it, vi } from "vitest";
import DashboardQueryVariable from "./DashboardQueryVariable";

describe("DashboardQueryVariable", () => {
  afterEach(cleanup);

  it("keeps an all-host selection compact and replaces it with a concrete host", () => {
    const onChange = vi.fn();
    render(<DashboardQueryVariable
      variable={{ name: "host", label: "Host", type: "query", multi: true, includeAll: true }}
      options={["controller", "worker-1"]}
      value={[".*"]}
      onChange={onChange}
    />);

    fireEvent.click(screen.getByRole("button", { name: "All" }));
    fireEvent.click(screen.getByLabelText("worker-1"));

    expect(onChange).toHaveBeenCalledWith(["worker-1"]);
  });

  it("uses a regular select for a single-value query variable", () => {
    const onChange = vi.fn();
    render(<DashboardQueryVariable
      variable={{ name: "host", label: "Host", type: "query" }}
      options={["controller", "worker-1"]}
      value="controller"
      onChange={onChange}
    />);

    fireEvent.change(screen.getByLabelText("Host"), { target: { value: "worker-1" } });

    expect(onChange).toHaveBeenCalledWith("worker-1");
  });
});
