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
import { render, waitFor, screen, fireEvent } from "@testing-library/react";
import { HashRouter } from "react-router-dom";
import AppContent from "../context/AppContext";
import "@testing-library/jest-dom/vitest";
import ViewApi from "../api/viewApi";
import { mockViewsList, paginatedViews } from "../__mocks__/mockViewsList";
import Views from "../screens/Views";
import ClusterApi from "../api/clusterApi";
import ViewsInformationApi from "../api/viewsApiInfo";
import toast from "react-hot-toast";
import { mockRemoteClusterInfo } from "../__mocks__/mockCreateInstance";

describe("Views", () => {
  const mockClusterName = "testCluster";
  const mockContext = {
    cluster: { cluster_name: mockClusterName },
    setSelectedOption: () => "Views",
  };

  let mockToastSuccessMessage = "";
  let mockToastErrorMessage = "";

  toast.success = (message) => {
    mockToastSuccessMessage = message as string;
    return "";
  };

  toast.error = (message) => {
    mockToastErrorMessage = message as string;
    return "";
  };

  const mockClusterInfo = {
    href: "http://example.com",
    items: [
      {
        href: "http://example.com/1",
        Clusters: {
          cluster_id: 1,
          cluster_name: "Cluster 1",
          provisioning_state: "Provisioned",
          security_type: "Type 1",
          version: "v1.0",
        },
      },
    ],
  };

  const renderViewsComponent = () => {
    render(
      <AppContent.Provider value={mockContext}>
        <HashRouter>
          <Views />
        </HashRouter>
      </AppContent.Provider>
    );
  };

  beforeEach(async () => {
    ViewApi.viewsListAPI = async () => mockViewsList;
    ClusterApi.clusterInfo = async () => mockClusterInfo;
    ClusterApi.remoteClusterInfo = async () => mockRemoteClusterInfo;
  });

  it("renders Views component without crashing", () => {
    renderViewsComponent();
  });

  it("renders loading spinner when data is being fetched", async () => {
    renderViewsComponent();
    const spinner = screen.getByTestId("admin-spinner");
    expect(spinner).toBeInTheDocument();
  });

  it("renders appropriate message when list is empty", async () => {
    const mockEmptyViews = { href: "http://example.com", items: [] };

    ViewApi.viewsListAPI = async () => mockEmptyViews;

    renderViewsComponent();

    await waitFor(() => screen.getByText(/display/i), {
      timeout: 5000,
    });
    const emptyElement = screen.getByText(/to display/i);
    expect(emptyElement).toBeInTheDocument();
  });

  it("renders correct number of items", async () => {
    renderViewsComponent();

    await waitFor(() => screen.getByText(/Instance 1/i)); // wait for the data to be loaded

    const items = screen.getAllByRole("listitem");
    expect(items).toHaveLength(mockViewsList.items.length);
  });

  it("renders data for a specific item correctly", async () => {
    renderViewsComponent();
    await waitFor(() => screen.getByText(/Instance 1/i)); // wait for the data to be loaded

    const item = screen.getByText(/Instance 1/i);
    expect(item).toBeInTheDocument();
  });

  it("shows the create instance button and renders Create instance on click", async () => {
    renderViewsComponent();
    await waitFor(() => screen.getByText(/Instance 1/i)); // wait for the data to be loaded
    const button = screen.getByText(/Create Instance/i);
    expect(button).toBeInTheDocument();

    fireEvent.click(button);
    const CreateInstance = screen.getByTestId("Create-instance");
    expect(CreateInstance).toBeInTheDocument();
  });

  it("shows the filters button and renders the Filters component on click", async () => {
    renderViewsComponent();
    await waitFor(() => screen.getByText(/Instance 1/i)); // wait for the data to be loaded

    // Simulate click on the filter button
    const filterButton = screen.getByTestId("Filter button");
    fireEvent.click(filterButton);

    // Check for the presence of ComboSearch component
    const comboSearch = screen.getByTestId("search-filters");
    expect(comboSearch).toBeInTheDocument();
  });

  //Check for pagination if items > 10
  it("renders pagination when items are more than 10", async () => {
    ViewApi.viewsListAPI = async () => paginatedViews;
    renderViewsComponent();
    await waitFor(() => screen.getByText(/Instance some/i)); // wait for the data to be loaded
    const pagination = screen.getByTestId("pagination");
    expect(pagination).toBeInTheDocument();
  });

  it("Edit icon is rendered", async () => {
    renderViewsComponent();
    await waitFor(() => screen.getByText(/Instance 1/i));
    const editIcon = screen.getByTestId("edit-icon");
    expect(editIcon).toBeInTheDocument();
  });

  it("renders clone icon", async () => {
    renderViewsComponent();
    await waitFor(() => screen.getByText(/Instance 1/i));
    const cloneIcon = screen.getByTestId("clone-icon");
    expect(cloneIcon).toBeInTheDocument();
    fireEvent.click(cloneIcon);
    const createInstance = screen.getByText("Clone Instance");
    expect(createInstance).toBeInTheDocument();
  });

  it("shows delete modal on delete button click", async () => {
    renderViewsComponent();
    await waitFor(() => screen.getByText(/Instance 1/i)); // wait for the data to be loaded

    // Simulate click on the delete button
    const deleteButton = screen.getByTestId("delete-icon"); // Adjust based on your component
    fireEvent.click(deleteButton);

    // Check for the presence of delete modal
    const deleteModal = await screen.findByText(/Are you sure/i);
    expect(deleteModal).toBeInTheDocument();
  });

  it("should call the delete API and show success toast on clicking Yes button in the modal", async () => {
    ViewsInformationApi.deleteInstance = async () => {
      toast.success(`Instance deleted successfully`);
      return { status: 200 };
    };

    renderViewsComponent();
    await waitFor(() => screen.getByText(/Instance 1/i)); // wait for the data to be loaded

    const deleteIcon = screen.getByTestId("delete-icon");
    fireEvent.click(deleteIcon);
    const yesButton = await screen.findByText("OK");
    fireEvent.click(yesButton);

    await waitFor(() => {
      expect(mockToastSuccessMessage).not.toBeUndefined;
      expect(mockToastSuccessMessage).toBe(`Instance deleted successfully`);
    });
  });

  it("should call the delete API and show fail toast on clicking Yes button in the modal", async () => {
    ViewsInformationApi.deleteInstance = async () => {
      toast.error(`Cannot delete instance`);
      return { status: 404 };
    };

    renderViewsComponent();
    await waitFor(() => screen.getByText(/Instance 1/i)); // wait for the data to be loaded

    const deleteIcon = screen.getByTestId("delete-icon");
    fireEvent.click(deleteIcon);
    const yesButton = await screen.findByText("OK");
    fireEvent.click(yesButton);

    await waitFor(() => {
      expect(mockToastErrorMessage).not.toBeUndefined;
      expect(mockToastErrorMessage).toBe(`Cannot delete instance`);
    });
  });
});
