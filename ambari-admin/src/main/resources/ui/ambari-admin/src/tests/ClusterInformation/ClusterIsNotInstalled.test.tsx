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
import AppContent from "../../../src/context/AppContext";
import "@testing-library/jest-dom/vitest";
import ClusterInformation from "../../screens/ClusterManagement/ClusterInformation";

describe("Cluster is not selected", () => {
  afterEach(cleanup);

  it("offers an explicit choice when multiple clusters are available", () => {
    const selectCluster = vi.fn();
    render(
      <AppContent.Provider value={{
        cluster: {},
        setClusterInfo: vi.fn(),
        setSelectedOption: vi.fn(),
        availableClusters: [
          { cluster_id: 11, cluster_name: "analytics" },
          { cluster_id: 22, cluster_name: "warehouse" },
        ],
        selectCluster,
      }}>
        <ClusterInformation />
      </AppContent.Provider>,
    );

    expect(screen.getByRole("heading", { name: "Choose a cluster" })).toBeInTheDocument();
    expect(screen.getByText("Select a cluster to view its details or return to its Dashboard."))
      .toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "warehouse" }));

    expect(selectCluster).toHaveBeenCalledOnce();
    expect(selectCluster).toHaveBeenCalledWith("warehouse");
  });

  it("does not render cluster-specific controls without a selected cluster", () => {
    const { container } = render(
      <AppContent.Provider value={{
        cluster: {},
        setClusterInfo: vi.fn(),
        setSelectedOption: vi.fn(),
        availableClusters: [],
        selectCluster: vi.fn(),
      }}>
        <ClusterInformation />
      </AppContent.Provider>,
    );

    expect(container).toBeEmptyDOMElement();
    expect(screen.queryByRole("button", { name: /download/i })).not.toBeInTheDocument();
  });
});
