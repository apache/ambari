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
import AddUser from "../screens/Users/AddUser";
import { describe, it, beforeEach, expect, vi } from "vitest";
import "@testing-library/jest-dom/vitest";
import UserGroupApi from "../api/userGroupApi";
import { Router } from "react-router";
import AppContent from "../context/AppContext";
import { createMemoryHistory } from "history";
import { rbacData } from "../__mocks__/mockRbacData";
import toast from "react-hot-toast";
import { UserDataType } from "../api/types";
import { get } from "lodash";

describe("AddUser component", () => {
  const mockClusterName = "testCluster";
  const mockContext = {
    cluster: { cluster_name: mockClusterName },
    rbacData: {},
    setRbacData: () => vi.fn(),
    permissionLabelList: [],
    setPermissionLabelList: vi.fn(),
  };
  const mockProps = {
    clusterName: mockClusterName,
    showAddUserModal: true,
    setShowAddUserModal: vi.fn(),
    successCallback: vi.fn(),
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

  beforeEach(() => {
    vi.clearAllMocks();
    mockToastSuccessMessage = "";
    mockToastErrorMessage = "";
    UserGroupApi.getPermissions = async () => rbacData;
  });

  const renderComponent = (props = mockProps) => {
    render(
      <AppContent.Provider value={mockContext}>
        <Router history={createMemoryHistory()}>
          <AddUser {...props} />
        </Router>
      </AppContent.Provider>
    );
  };

  it("renders AddUser modal without crashing", () => {
    renderComponent();
    expect(screen.getByTestId("add-user-modal")).toBeInTheDocument();
  });

  it("should display required errors when input fields are empty", async () => {
    renderComponent();

    const saveButton = screen.getByTestId("save-btn");
    fireEvent.click(saveButton);

    await waitFor(async () => {
      const errorMessages = await screen.findAllByText(/required/i);
      expect(errorMessages).toHaveLength(3);
    });
  });

  it("renders username input and validates input", async () => {
    renderComponent();

    const saveButton = screen.getByTestId("save-btn");
    fireEvent.click(saveButton);

    const usernameInput = screen.getByTestId("username-input");
    fireEvent.change(usernameInput, { target: { value: "invalid&username" } });

    await waitFor(() => {
      expect(
        screen.getByText("Must not contain special characters!")
      ).toBeInTheDocument();
    });

    fireEvent.change(usernameInput, {
      target: {
        value:
          "012345678901234567890123456789012345678901234567890123456789012345678901234567891",
      },
    });

    await waitFor(() => {
      expect(
        screen.queryByText("Must not be longer than 80 characters!")
      ).toBeInTheDocument();
    });

    fireEvent.change(usernameInput, { target: { value: "validusername" } });

    await waitFor(() => {
      expect(
        screen.queryByText("Must not be longer than 80 characters!")
      ).not.toBeInTheDocument();
    });
  });

  it("renders password and confirm password inputs and validates matching", async () => {
    renderComponent();

    const saveButton = screen.getByTestId("save-btn");
    fireEvent.click(saveButton);

    const passwordInput = screen.getByTestId("password-input");
    const confirmPasswordInput = screen.getByTestId("confirm-password-input");

    fireEvent.change(passwordInput, { target: { value: "password" } });
    fireEvent.change(confirmPasswordInput, {
      target: { value: "differentpassword" },
    });

    await waitFor(() => {
      expect(screen.getByText("Password must match!")).toBeInTheDocument();
    });

    fireEvent.change(confirmPasswordInput, { target: { value: "password" } });

    await waitFor(() => {
      expect(
        screen.queryByText("Password must match!")
      ).not.toBeInTheDocument();
    });
  });

  it("renders user access dropdown and selects an option", () => {
    renderComponent();

    const userAccessSelect = screen.getByTestId(
      "user-access-dropdown"
    ) as HTMLSelectElement;
    fireEvent.change(userAccessSelect, { target: { value: "Cluster User" } });

    expect(userAccessSelect.value).toBe("Cluster User");
  });

  it("renders Ambari Admin switch and toggles it", () => {
    renderComponent();

    const ambariAdminSwitch = screen.getByTestId("ambari-admin-switch");
    fireEvent.click(ambariAdminSwitch);

    expect(ambariAdminSwitch).toBeChecked();
  });

  it("renders User Status switch and toggles it", () => {
    renderComponent();

    const userStatusSwitch = screen.getByTestId("user-status-switch");
    fireEvent.click(userStatusSwitch);

    expect(userStatusSwitch).not.toBeChecked();
  });

  it("calls handleSave on form submit with valid data", async () => {
    UserGroupApi.addUser = async (userData: UserDataType) => {
      toast.success(
        `Created user ${get(userData, "Users/user_name", "")}`
      );
      return { status: 200 };
    };

    renderComponent();

    const usernameInput = screen.getByTestId("username-input");
    const passwordInput = screen.getByTestId("password-input");
    const confirmPasswordInput = screen.getByTestId("confirm-password-input");

    fireEvent.change(usernameInput, { target: { value: "validusername" } });
    fireEvent.change(passwordInput, { target: { value: "password" } });
    fireEvent.change(confirmPasswordInput, { target: { value: "password" } });

    const saveButton = screen.getByTestId("save-btn");
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(mockToastSuccessMessage).toBe(
        "Created user validusername"
      );
    });
  });

  it("calls handleSave on form submit but API got failed", async () => {
    UserGroupApi.addUser = async (userName: UserDataType) => {
      toast.error(
        `Error while adding user ${get(userName, "Users/user_name", "")}`
      );
      return { status: 400 };
    };

    renderComponent();

    const usernameInput = screen.getByTestId("username-input");
    const passwordInput = screen.getByTestId("password-input");
    const confirmPasswordInput = screen.getByTestId("confirm-password-input");

    fireEvent.change(usernameInput, { target: { value: "validusername" } });
    fireEvent.change(passwordInput, { target: { value: "password" } });
    fireEvent.change(confirmPasswordInput, { target: { value: "password" } });

    const saveButton = screen.getByTestId("save-btn");
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(mockToastErrorMessage).toBe(
        "Error while adding user validusername"
      );
    });
  });

  it("calls handleCancel and shows warning modal if form is dirty", () => {
    renderComponent();

    const usernameInput = screen.getByTestId("username-input");
    fireEvent.change(usernameInput, { target: { value: "validusername" } });

    const cancelButton = screen.getByTestId("cancel-btn");
    fireEvent.click(cancelButton);

    expect(screen.getByTestId("warning-modal")).toBeInTheDocument();
  });

  it("renders ErrorOverlay for username tooltip", async () => {
    renderComponent();

    const usernameTooltipIcon = screen.getByTestId("username-tooltip-icon");
    fireEvent.click(usernameTooltipIcon);

    await waitFor(() => {
      expect(screen.getByText(/are not allowed/i)).toBeInTheDocument();
    });
  });

  it("renders RoleBasedAccessControl modal", () => {
    renderComponent();

    const roleAccessIcon = screen.getByTestId("user-access-help-icon");
    fireEvent.click(roleAccessIcon);

    expect(screen.getByText("Role Based Access Control")).toBeInTheDocument();
  });

  it("renders ErrorOverlay for Ambari Admin tooltip", () => {
    renderComponent();

    const ambariAdminTooltipIcon = screen.getByTestId(
      "ambari-admin-tooltip-icon"
    );
    fireEvent.click(ambariAdminTooltipIcon);

    expect(
      screen.getByText(/can create new clusters and other Ambari Admin Users/i)
    ).toBeInTheDocument();
  });

  it("renders ErrorOverlay for User Status tooltip", () => {
    renderComponent();

    const userStatusTooltipIcon = screen.getByTestId(
      "user-status-tooltip-icon"
    );
    fireEvent.click(userStatusTooltipIcon);

    expect(
      screen.getByText(
        /Active Users can log in to Ambari. Inactive Users cannot/i
      )
    ).toBeInTheDocument();
  });
});
