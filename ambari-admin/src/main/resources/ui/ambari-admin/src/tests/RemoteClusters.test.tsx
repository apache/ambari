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
import { describe, it, expect } from "vitest";
import { render, waitFor, screen, fireEvent } from "@testing-library/react";
import { createMemoryHistory } from "history";
import { Router } from "react-router-dom";
import "@testing-library/jest-dom/vitest";
import RemoteClusters from "../screens/ClusterManagement/RemoteClusters/Index"
import RemoteClusterApi from "../api/remoteCluster";
import {
  mockData,
  paginatedMockData,
} from "../__mocks__/mockRemoteCluster";
import AppContent from "../context/AppContext";

const mockClusterName = "testCluster";
const mockContext = {
  cluster: { cluster_name: mockClusterName },
  setSelectedOption: () => "RemoteCluster",
};

const renderRemoteCluster = () => {
  render(
    <AppContent.Provider value={mockContext}>
      <Router history={createMemoryHistory()}>
        <RemoteClusters />
      </Router>
    </AppContent.Provider>
  );
};

describe("RemoteClusters component", () => {
  it("renders without crashing", () => {
    RemoteClusterApi.getRemoteClusters = async () => mockData;
    renderRemoteCluster();
  });

  it("shows loading spinner when data is being fetched.", async () => {
    RemoteClusterApi.getRemoteClusters = async () => [];
    renderRemoteCluster();

    const spinner = screen.getByTestId("admin-spinner");
    expect(spinner).toBeInTheDocument();
  });

  it("display appropriate message when no cluster is rendered.", async () => {
    RemoteClusterApi.getRemoteClusters = async () => [];
    renderRemoteCluster();
    expect(screen.findByText(/No Remote Cluster to show/i));
  });

  it("renders correct number of items ", async () => {
    RemoteClusterApi.getRemoteClusters = async () => mockData;
    renderRemoteCluster();

    await waitFor(() => screen.getByText(/TestCluster/i));

    const items = screen.getAllByRole("listitem");
    expect(items).toHaveLength(mockData.length);
  });

  it("renders data for a specific item correctly", async () => {
    RemoteClusterApi.getRemoteClusters = async () => mockData;
    renderRemoteCluster();

    await waitFor(() => screen.getByText(/TestCluster/i));
    expect(screen.getByText(/TestCluster1/i)).toBeInTheDocument();
    expect(screen.getByText(/Service1/i)).toBeInTheDocument();
    expect(screen.getByText(/Service2/i)).toBeInTheDocument();
  });

  it("renders the Register Remote cluster button and navigates to the create route on click", async () => {
    RemoteClusterApi.getRemoteClusters = async () => mockData;
    const history = createMemoryHistory();
    render(
      <AppContent.Provider value={mockContext}>
        <Router history={history}>
          <RemoteClusters />
        </Router>
      </AppContent.Provider>
    );
    await waitFor(() => screen.getByText(/TestCluster1/i));
    expect(screen.getByText(/TestCluster1/i)).toBeInTheDocument();

    const registerButton = screen.getByRole("button", {
      name: /Register Remote cluster/i,
    });
    expect(registerButton).toBeInTheDocument();

    fireEvent.click(registerButton);
    await waitFor(() =>
      expect(history.location.pathname).toBe("/remoteClusters/create")
    );
  });

  it("renders pagination when items are more than 10", async () => {
    RemoteClusterApi.getRemoteClusters = async () => paginatedMockData;
    renderRemoteCluster();
    await waitFor(() => screen.getByText(/TestCluster2/i));

    const pagination = screen.getByTestId("pagination");
    expect(pagination).toBeInTheDocument();
  });
});
