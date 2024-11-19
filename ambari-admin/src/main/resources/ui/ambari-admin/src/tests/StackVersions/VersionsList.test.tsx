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
import { describe, it, beforeEach, expect } from "vitest";
import { render, waitFor, screen } from "@testing-library/react";
import { createMemoryHistory } from "history";
import { Router } from "react-router-dom";
import "@testing-library/jest-dom/vitest";
import VersionsApi from "../../api/versions";
import mockRepos, { paginatedRepos } from "../../__mocks__/mockRepos";
import mockClusterInfo from "../../__mocks__/mockClusterInfo";
import mockVersionList from "../../__mocks__/mockVersionList";
import AppContent from "../../context/AppContext";
import StackVersionsList from "../../screens/ClusterManagement/StackVersions/List";

describe("StackVersionsList", () => {
  const mockClusterName = "testCluster";
  const mockContext = {
    cluster: { cluster_name: mockClusterName },
    setSelectedOption: () => "Versions",
  };

  beforeEach(async () => {
    VersionsApi.getRepos = async () => mockRepos;
    VersionsApi.getClusterInfo = async () => mockClusterInfo;
    VersionsApi.versionsList = async () => mockVersionList;
  });

  it("renders without crashing", () => {
    render(
      <AppContent.Provider value={mockContext}>
        <Router history={createMemoryHistory()}>
          <StackVersionsList />
        </Router>
      </AppContent.Provider>
    );
  });

  // it("renders loading spinner when data is being fetched", async () => {
  //   render(
  //     <AppContent.Provider value={mockContext}>
  //       <Router history={createMemoryHistory()}>
  //         <StackVersionsList />
  //       </Router>
  //     </AppContent.Provider>
  //   );

  //   const spinner = screen.getByTestId("admin-spinner");
  //   expect(spinner).toBeInTheDocument();
  // });
  it("renders appropriate message when repos, cluster info, or version list is empty", async () => {
    const mockEmptyRepos = { href: "http://example.com", items: [] };
    const mockEmptyClusterInfo = { href: "http://example.com", items: [] };
    const mockEmptyVersionList = { href: "http://example.com", items: [] };

    VersionsApi.getRepos = async () => mockEmptyRepos;
    VersionsApi.getClusterInfo = async () => mockEmptyClusterInfo;
    VersionsApi.versionsList = async () => mockEmptyVersionList;

    render(
      <AppContent.Provider value={mockContext}>
        <Router history={createMemoryHistory()}>
          <StackVersionsList />
        </Router>
      </AppContent.Provider>
    );

    const emptyElement = screen.getByTestId("admin-spinner");
    expect(emptyElement).toBeInTheDocument();
  });
  it("renders correct number of items", async () => {
    render(
      <AppContent.Provider value={mockContext}>
        <Router history={createMemoryHistory()}>
          <StackVersionsList />
        </Router>
      </AppContent.Provider>
    );

    await waitFor(() => screen.getByText(/Stack1/i)); // wait for the data to be loaded

    const items = screen.getAllByRole("listitem");
    expect(items).toHaveLength(mockRepos.items.length);
  });

  it("renders data for a specific item correctly", async () => {
    render(
      <AppContent.Provider value={mockContext}>
        <Router history={createMemoryHistory()}>
          <StackVersionsList />
        </Router>
      </AppContent.Provider>
    );

    await waitFor(() => screen.getByText(/Stack1-1.0.0/i)); // wait for the data to be loaded

    const item = screen.getByText(/Stack1-1.0.0/i);
    expect(item).toBeInTheDocument();
  });

  it("renders loading spinner when data is being fetched", async () => {
    render(
      <AppContent.Provider value={mockContext}>
        <Router history={createMemoryHistory()}>
          <StackVersionsList />
        </Router>
      </AppContent.Provider>
    );

    const spinner = screen.getByTestId("admin-spinner");
    expect(spinner).toBeInTheDocument();
  });

  it("renders appropriate message when repos, cluster info, or version list is empty", async () => {
    const mockEmptyRepos = { href: "http://example.com", items: [] };
    const mockEmptyClusterInfo = { href: "http://example.com", items: [] };
    const mockEmptyVersionList = { href: "http://example.com", items: [] };

    VersionsApi.getRepos = async () => mockEmptyRepos;
    VersionsApi.getClusterInfo = async () => mockEmptyClusterInfo;
    VersionsApi.versionsList = async () => mockEmptyVersionList;

    render(
      <AppContent.Provider value={mockContext}>
        <Router history={createMemoryHistory()}>
          <StackVersionsList />
        </Router>
      </AppContent.Provider>
    );

    const emptyElement = screen.getByTestId("admin-spinner");
    expect(emptyElement).toBeInTheDocument();
  });

  it("renders Register Version button", async () => {
    render(
      <AppContent.Provider value={mockContext}>
        <Router history={createMemoryHistory()}>
          <StackVersionsList />
        </Router>
      </AppContent.Provider>
    );
    await waitFor(() => screen.getByText(/Stack1-1.0.0/i)); // wait for the data to be loaded
    const button = screen.getByText(/Register Version/i);
    expect(button).toBeInTheDocument();
  });

  //Check for pagination if items > 10
  it("renders pagination when items are more than 10", async () => {
    VersionsApi.getRepos = async () => paginatedRepos;
    VersionsApi.getClusterInfo = async () => mockClusterInfo;
    VersionsApi.versionsList = async () => mockVersionList;

    render(
      <AppContent.Provider value={mockContext}>
        <Router history={createMemoryHistory()}>
          <StackVersionsList />
        </Router>
      </AppContent.Provider>
    );

    await waitFor(() => screen.getByText(/Stack1-1.0.0/i)); // wait for the data to be loaded

    const pagination = screen.getByTestId("pagination");
    expect(pagination).toBeInTheDocument();
  });
});
