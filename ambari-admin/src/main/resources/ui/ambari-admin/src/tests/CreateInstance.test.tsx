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
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, beforeEach, expect, vi } from "vitest";
import { createMemoryHistory } from "history";
import { Router } from "react-router-dom";
import AppContent from "../context/AppContext";
import CreateInstance from "../screens/Views/CreateInstance";
import ClusterApi from "../api/clusterApi";
import {
  mockClusterInfo,
  mockRemoteClusterInfo,
  mockViewsDetails,
} from "../__mocks__/mockCreateInstance";
import "@testing-library/jest-dom/vitest";
import toast from "react-hot-toast";
import ViewApi from "../api/viewApi";


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

type CreateInstanceProps = {
  isOpen: boolean;
  onClose: () => void;
  viewDetails: any;
  successCallback: (() => void);
  viewInstanceInfoToBeCloned?: any;
};

describe("Create Instance UTs", () => {
  const mockContext = {
    cluster: { cluster_name: "testCluster" },
    setSelectedOption: () => "Views",
  };

  const renderCreateInstanceComponent = (props : CreateInstanceProps) => {
    render(
      <AppContent.Provider value={mockContext}>
        <Router history={createMemoryHistory()}>
          <CreateInstance {...props} />
        </Router>
      </AppContent.Provider>
    );
  };

  beforeEach(async () => {
    ClusterApi.clusterInfo = async () => mockClusterInfo;
    ClusterApi.remoteClusterInfo = async () => mockRemoteClusterInfo;
  });

  it("renders CreateInstance component without crashing", () => {
    renderCreateInstanceComponent({
      isOpen: true,
      onClose: () => {},
      viewDetails: mockViewsDetails,
      successCallback: () => {},
    });
  });

  it("renders loading spinner when data is being fetched", async () => {
    renderCreateInstanceComponent({
      isOpen: true,
      onClose: () => {},
      viewDetails: mockViewsDetails,
      successCallback: () => {},
    });
    const spinner = screen.getByTestId("admin-spinner");
    expect(spinner).toBeInTheDocument();
  });

  it("renders the component correctly", async () => {
    renderCreateInstanceComponent({
      isOpen: true,
      onClose: () => {},
      viewDetails: mockViewsDetails,
      successCallback: () => {},
    });

    await waitFor(() => screen.getByText(/Create Instance/i));

    expect(screen.getByText(/Create Instance/i)).toBeInTheDocument();
    expect(screen.getByText(/Instance Name/i)).toBeInTheDocument();
    expect(screen.getByText(/Display Name/i)).toBeInTheDocument();
    expect(screen.getByText(/Description/i)).toBeInTheDocument();
    expect(screen.getByText(/Visible/i)).toBeInTheDocument();
  });

  it("handles form field changes", async () => {
    renderCreateInstanceComponent({
      isOpen: true,
      onClose: () => {},
      viewDetails: mockViewsDetails,
      successCallback: () => {},
    });

    await waitFor(() => screen.getByText(/Create Instance/i));

    const instanceNameInput = screen.getByTestId(/instance-name/i);
    fireEvent.change(instanceNameInput, {
      target: { value: "Updated Instance Name" },
    });
    expect((instanceNameInput as HTMLInputElement).value).toBe(
      "Updated Instance Name"
    );

    const displayNameInput = screen.getByTestId(/display-name/i);
    fireEvent.change(displayNameInput, { target: { value: "Updated Label" } });
    expect((displayNameInput as HTMLInputElement).value).toBe("Updated Label");

    const descriptionInput = screen.getByTestId(/description/i);
    fireEvent.change(descriptionInput, {
      target: { value: "Updated Description" },
    });
    expect((descriptionInput as HTMLInputElement).value).toBe(
      "Updated Description"
    );
  });

  it("calls onClose when the close button is clicked", async () => {
    const mockOnClose = vi.fn();
    renderCreateInstanceComponent({
      isOpen: true,
      onClose: mockOnClose,
      viewDetails: mockViewsDetails,
      successCallback: () => {},
    });

    await waitFor(() => screen.getByText(/Create Instance/i));

    const closeButton = screen.getByText(/CANCEL/i);
    fireEvent.click(closeButton);
    expect(mockOnClose).toHaveBeenCalled();
  });

  it("calls the create instance API when the form is submitted", async () => {
    ViewApi.addView = async () => {
      toast.success("Created instance Updated Instance Name");
      return { status: 200 };
    };

    renderCreateInstanceComponent({
      isOpen: true,
      onClose: () => {},
      viewDetails: mockViewsDetails,
      successCallback: () => {},
    });

    await waitFor(() => screen.getByText(/Create Instance/i));

    const instanceNameInput = screen.getByTestId(/instance-name/i);
    fireEvent.change(instanceNameInput, {
      target: { value: "UpdatedInstanceName" },
    });
    const displayNameInput = screen.getByTestId(/display-name/i);
    fireEvent.change(displayNameInput, { target: { value: "UpdatedLabel" } });
    const descriptionInput = screen.getByTestId(/description/i);
    fireEvent.change(descriptionInput, {
      target: { value: "UpdatedDescription" },
    });

    const saveButton = screen.getByText(/SAVE/i);
    fireEvent.click(saveButton);


    await waitFor(() => {
      expect(mockToastSuccessMessage).not.toBeUndefined;
      expect(mockToastSuccessMessage).toBe("Created instance Updated Instance Name");
    });
  });

  it("should display remote clusters", async () => {
    renderCreateInstanceComponent({
      isOpen: true,
      onClose: () => {},
      viewDetails: mockViewsDetails,
      successCallback: () => {},
    });

    await waitFor(() => screen.getByText(/Create Instance/i));

    const remoteClusterSelect = screen.getByTestId(/remote-toggle-button/i);
    fireEvent.click(remoteClusterSelect);

    expect(screen.getByText(/test_cluster_remote/i)).toBeInTheDocument();
  });

  it("should display select options", async () => {
    renderCreateInstanceComponent({
      isOpen: true,
      onClose: () => {},
      viewDetails: mockViewsDetails,
      successCallback: () => {},
    });

    await waitFor(() => screen.getByText(/Create Instance/i));

    const selectView = screen.getByLabelText(/Select view/i);
    fireEvent.click(selectView);

    expect(screen.getByText(/Item 1 view/i)).toBeInTheDocument();
  });

  it("should render the warning modal", async () => {
    ViewApi.addView = async () => {
      toast.success("Instance created successfully");
      return { status: 200 };
    };

    renderCreateInstanceComponent({
      isOpen: true,
      onClose: () => {},
      viewDetails: mockViewsDetails,
      successCallback: () => {},
    });

    await waitFor(() => screen.getByText(/Create Instance/i));

    const instanceNameInput = screen.getByTestId(/instance-name/i);
    fireEvent.change(instanceNameInput, {
      target: { value: "Updated Instance Name" },
    });

    const cancelButton = screen.getByText(/CANCEL/i);
    fireEvent.click(cancelButton);

    expect(screen.getByText(/Warning/i)).toBeInTheDocument();
  });

  it("displays the correct version options", async () => {
    renderCreateInstanceComponent({
      isOpen: true,
      onClose: () => {},
      viewDetails: mockViewsDetails,
      successCallback: () => {},
    });

    await waitFor(() => screen.getByText(/Create Instance/i));

    const versionSelect = screen.getByText(/Select Version/i);
    fireEvent.click(versionSelect);

    mockViewsDetails.items[0].versions.forEach((version) => {
      expect(
        screen.getByText(version.ViewVersionInfo.version)
      ).toBeInTheDocument();
    });
  });

  it("handles API errors gracefully", async () => {
    ViewApi.addView = async () => {
      toast.error("Failed to create instance");
      return { status: 404 };
    };

    renderCreateInstanceComponent({
      isOpen: true,
      onClose: () => {},
      viewDetails: mockViewsDetails,
      successCallback: () => {},
    });

    await waitFor(() => screen.getByText(/Create Instance/i));

    const instanceNameInput = screen.getByTestId(/instance-name/i);
    fireEvent.change(instanceNameInput, {
      target: { value: "UpdatedInstanceName" },
    });
    const displayNameInput = screen.getByTestId(/display-name/i);
    fireEvent.change(displayNameInput, { target: { value: "UpdatedLabel" } });
    const descriptionInput = screen.getByTestId(/description/i);
    fireEvent.change(descriptionInput, {
      target: { value: "UpdatedDescription" },
    });

    const saveButton = screen.getByText(/SAVE/i);
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(mockToastErrorMessage).not.toBeUndefined;
      expect(mockToastErrorMessage).toBe("Failed to create instance");
    });
  });

  it("toggles visibility checkbox", async () => {
    renderCreateInstanceComponent({
      isOpen: true,
      onClose: () => {},
      viewDetails: mockViewsDetails,
      successCallback: () => {},
    });

    await waitFor(() => screen.getByText(/Create Instance/i));

    const visibilityCheckbox = screen.getByLabelText(/Visible/i);
    expect(visibilityCheckbox).toBeChecked();

    fireEvent.click(visibilityCheckbox);
    expect(visibilityCheckbox).not.toBeChecked();

    fireEvent.click(visibilityCheckbox);
    expect(visibilityCheckbox).toBeChecked();
  });

  it("renders the component when isOpen is false", () => {
    renderCreateInstanceComponent({
      isOpen: false,
      onClose: () => {},
      viewDetails: mockViewsDetails,
      successCallback: () => {},
    });

    expect(screen.queryByText(/Create Instance/i)).not.toBeInTheDocument();
  });

  it("displays error message when required fields are empty", async () => {
    renderCreateInstanceComponent({
      isOpen: true,
      onClose: () => {},
      viewDetails: mockViewsDetails,
      successCallback: () => {},
    });

    await waitFor(() => screen.getByText(/Create Instance/i));

    const saveButton = screen.getByText(/SAVE/i);
    fireEvent.click(saveButton);

    await waitFor(() => {
      const warningMessages = screen.getAllByText(/Field required!/i);
      expect(warningMessages.length).toBeGreaterThanOrEqual(3);
    });
  });

  it("resets form after successful submission", async () => {
    ViewApi.addView = async () => {
      toast.success("Instance created successfully");
      return { status: 200 };
    };

    renderCreateInstanceComponent({
      isOpen: true,
      onClose: () => {},
      viewDetails: mockViewsDetails,
      successCallback: () => {},
    });

    await waitFor(() => screen.getByText(/Create Instance/i));

    const instanceNameInput = screen.getByTestId(/instance-name/i);
    fireEvent.change(instanceNameInput, {
      target: { value: "UpdatedInstanceName" },
    });
    const displayNameInput = screen.getByTestId(/display-name/i);
    fireEvent.change(displayNameInput, { target: { value: "UpdatedLabel" } });
    const descriptionInput = screen.getByTestId(/description/i);
    fireEvent.change(descriptionInput, {
      target: { value: "UpdatedDescription" },
    });

    const saveButton = screen.getByText(/SAVE/i);
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(mockToastSuccessMessage).not.toBeUndefined;
      expect(mockToastSuccessMessage).toBe("Instance created successfully");
    });

    expect((instanceNameInput as HTMLInputElement).value).toBe("");
    expect((displayNameInput as HTMLInputElement).value).toBe("");
    expect((descriptionInput as HTMLInputElement).value).toBe("");
  });
});
