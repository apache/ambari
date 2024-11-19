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
import EditUser from "../screens/Users/EditUser";
import { describe, it, beforeEach, expect, vi } from "vitest";
import "@testing-library/jest-dom/vitest";toast
import UserGroupApi from "../api/userGroupApi";
import { Router } from "react-router";
import AppContent from "../context/AppContext";
import { createMemoryHistory } from "history";
import toast from "react-hot-toast";
import { rbacData } from "../__mocks__/mockRbacData";
import { groupNames } from "../__mocks__/mockGroupNames";
import { userData } from "../__mocks__/mockUserData";
import PrivilegeApi from "../api/privilegeApi";

describe("EditUser component", () => {
  const mockClusterName = "testCluster";
  const mockContext = {
    cluster: { cluster_name: mockClusterName },
    setSelectedOption: vi.fn(),
    selectedOption: "Users",
    rbacData: {},
    setRbacData: () => vi.fn(),
    permissionLabelList: [],
    setPermissionLabelList: vi.fn(),
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
    UserGroupApi.groupNames = async () => groupNames;
    UserGroupApi.userData = async () => userData;
  });

  const renderComponent = () => {
    render(
      <AppContent.Provider value={mockContext}>
        <Router history={createMemoryHistory()}>
          <EditUser />
        </Router>
      </AppContent.Provider>
    );
  };

  it("renders EditUser component without crashing", async () => {
    renderComponent();
    expect(screen.getByText(/Users/i)).toBeInTheDocument();
  });

  it("handles loading state", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByTestId("admin-spinner")).toBeInTheDocument();
    });
  });

  it("fetches and displays user data", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/dsasd/i)).toBeInTheDocument();
    });

    expect(screen.getByText(/Local/i)).toBeInTheDocument();
    expect(screen.getByText(/Active/i)).toBeInTheDocument();
    expect(screen.getByText(/gdgdeg/i)).toBeInTheDocument();
    expect(screen.getByText(/group1/i)).toBeInTheDocument();
    expect(screen.getAllByText(/Cluster User/i)).toHaveLength(2);
    expect(screen.getAllByText(/View User/i)).toHaveLength(4);
    expect(screen.getByText(mockClusterName)).toBeInTheDocument();
  });

  it("toggles user status", async () => {
    UserGroupApi.updateUser = async (userName: string) => {
      toast.success(`The active status for ${userName} is updated`);
      return { status: 200 };
    };
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/dsasd/i)).toBeInTheDocument();
    });

    const statusSwitch = screen.getByTestId("user-status-switch");
    expect(statusSwitch).toBeChecked();
    fireEvent.click(statusSwitch);

    await waitFor(() => {
      screen.getByTestId("confirmation-modal");
    });

    const confirmButton = screen.getByTestId("confirm-ok-btn");
    fireEvent.click(confirmButton);

    await waitFor(() => {
      expect(mockToastSuccessMessage).toBe(
        "The active status for dsasd is updated"
      );
    });
  });

  it("toggles Ambari Admin status", async () => {
    UserGroupApi.updateUser = async (userName: string) => {
      toast.success(`The admin status for ${userName} is updated`);
      return { status: 200 };
    };
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/dsasd/i)).toBeInTheDocument();
    });

    const adminSwitch = screen.getByTestId("ambari-admin-switch");
    expect(adminSwitch).not.toBeChecked();
    fireEvent.click(adminSwitch);

    await waitFor(() => {
      screen.getByTestId("confirmation-modal");
    });

    const confirmButton = screen.getByTestId("confirm-ok-btn");
    fireEvent.click(confirmButton);

    await waitFor(() => {
      expect(mockToastSuccessMessage).toBe(
        "The admin status for dsasd is updated"
      );
    });
  });

  it("updates user password", async () => {
    UserGroupApi.updateUser = async (userName: string) => {
      toast.success(`Password changed for ${userName}`);
      return { status: 200 };
    };
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/dsasd/i)).toBeInTheDocument();
    });

    const changePasswordButton = screen.getByTestId("change-password-btn");
    fireEvent.click(changePasswordButton);

    await waitFor(() => {
      screen.getByTestId("change-password-modal");
    });

    expect(screen.getByText(/Change Password for dsasd/i)).toBeInTheDocument();

    const yourPasswordInput = screen.getByTestId("your-password-input");
    const newPasswordInput = screen.getByTestId("new-password-input");
    const newConfirmPasswordInput = screen.getByTestId(
      "new-confirm-password-input"
    );

    fireEvent.change(yourPasswordInput, { target: { value: "oldPassword" } });
    fireEvent.change(newPasswordInput, { target: { value: "newPassword" } });
    fireEvent.change(newConfirmPasswordInput, {
      target: { value: "newPassword" },
    });

    const savePasswordButton = screen.getByTestId("save-password-btn");
    fireEvent.click(savePasswordButton);

    await waitFor(() => {
      expect(mockToastSuccessMessage).toBe("Password changed for dsasd");
    });
  });

  it("updates local group membership", async () => {
    UserGroupApi.addMember = async (_, userName: string) => {
      toast.success(`Local Group Membership updated for ${userName}`);
      return { status: 200 };
    };
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/dsasd/i)).toBeInTheDocument();
    });

    const groupSelect = screen.getByLabelText("select-group");

    fireEvent.mouseDown(groupSelect);
    fireEvent.click(screen.getByText("ferfew"));

    const saveButton = screen.getByTestId("save-groups-btn");
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(mockToastSuccessMessage).toBe(
        "Local Group Membership updated for dsasd"
      );
    });
  });

  it("renders user access dropdown and selects an option", async () => {
    PrivilegeApi.addClusterPrivileges = async () => {
      toast.success("User access updated successfully");
      return { status: 200 };
    };
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/dsasd/i)).toBeInTheDocument();
    });

    const userAccessSelect = screen.getByTestId(
      "user-access-dropdown"
    ) as HTMLSelectElement;
    fireEvent.change(userAccessSelect, { target: { value: "Cluster User" } });

    expect(userAccessSelect.value).toBe("Cluster User");
    await waitFor(() => {
      expect(mockToastSuccessMessage).toBe("User access updated successfully");
    });
  });

  it("shows unsaved changes warning modal", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/dsasd/i)).toBeInTheDocument();
    });

    const groupSelect = screen.getByLabelText("select-group");
    
    fireEvent.mouseDown(groupSelect);
    fireEvent.click(screen.getByText("ferfew"));

    const usersListButton = screen.getByTestId("users-list-link");
    fireEvent.click(usersListButton);

    await waitFor(() => {
      expect(screen.getByTestId("warning-modal")).toBeInTheDocument();
    });

    const cancelButton = screen.getByText("Cancel");
    fireEvent.click(cancelButton);

    await waitFor(() => {
      expect(screen.queryByTestId("warning-modal")).toBeNull();
    });
  });

  it("renders RBAC modal for user access tooltip", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/dsasd/i)).toBeInTheDocument();
    });

    const userAccessTooltipIcon = screen.getByTestId("user-access-info-icon");
    fireEvent.click(userAccessTooltipIcon);

    await waitFor(() => {
      expect(
        screen.getByTestId("role-based-access-control-modal")
      ).toBeInTheDocument();
    });
  });

  it("removes view privileges", async () => {
    PrivilegeApi.removeViewPrivileges = async () => {
      toast.success("View privileges removed successfully");
      return { status: 200 };
    };

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/dsasd/i)).toBeInTheDocument();
    });

    const removeViewButton = screen.getByTestId(
      "remove-privilege-icon-jukende"
    );
    fireEvent.click(removeViewButton);

    await waitFor(() => {
      expect(mockToastSuccessMessage).toBe(
        "View privileges removed successfully"
      );
    });
  });

  it("handles API errors gracefully", async () => {
    UserGroupApi.userData = async () => {
      toast.error("Failed to fetch user data");
      return { status: 400 };
    };

    renderComponent();

    await waitFor(() => {
      expect(mockToastErrorMessage).toBe("Failed to fetch user data");
    });
  });

  it("disables Local Group Membership by default unless changes are made and submitted", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/dsasd/i)).toBeInTheDocument();
    });

    const saveButton = screen.getByTestId("save-groups-btn");
    expect(saveButton).toHaveClass("cursor-not-allowed opacity-50");
  });
});
