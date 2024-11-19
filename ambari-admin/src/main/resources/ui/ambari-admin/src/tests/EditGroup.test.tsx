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
import EditGroup from "../screens/Users/EditGroup";
import { describe, it, beforeEach, expect, vi } from "vitest";
import "@testing-library/jest-dom/vitest";
import UserGroupApi from "../api/userGroupApi";
import PrivilegeApi from "../api/privilegeApi";
import { Router } from "react-router";
import AppContent from "../context/AppContext";
import { createMemoryHistory } from "history";
import toast from "react-hot-toast";
import { rbacData } from "../__mocks__/mockRbacData";
import { userNames } from "../__mocks__/mockUserNames";
import { groupData } from "../__mocks__/mockGroupData";

describe("EditGroup component", () => {
  const mockClusterName = "testCluster";
  const mockContext = {
    cluster: { cluster_name: mockClusterName },
    setSelectedOption: vi.fn(),
    selectedOption: "Groups",
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
    UserGroupApi.userNames = async () => userNames;
    UserGroupApi.groupData = async () => groupData;
  });

  const renderComponent = () => {
    render(
      <AppContent.Provider value={mockContext}>
        <Router history={createMemoryHistory()}>
          <EditGroup />
        </Router>
      </AppContent.Provider>
    );
  };

  it("renders EditGroup component without crashing", async () => {
    renderComponent();
    expect(screen.getByText(/Groups/i)).toBeInTheDocument();
  });

  it("handles loading state", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByTestId("admin-spinner")).toBeInTheDocument();
    });
  });

  it("fetches and displays group data", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/group5/i)).toBeInTheDocument();
    });

    expect(screen.getByText(/Local/i)).toBeInTheDocument();
    expect(screen.getByText(/dsasd/i)).toBeInTheDocument();
    expect(screen.getAllByText(/Cluster User/i)).toHaveLength(2);
    expect(screen.getAllByText(/View User/i)).toHaveLength(1);
    expect(screen.getByText(mockClusterName)).toBeInTheDocument();
  });

  it("updates group members", async () => {
    UserGroupApi.updateMembers = async () => {
      toast.success("Local Members updated for testGroup");
      return { status: 200 };
    };
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/group5/i)).toBeInTheDocument();
    });

    const groupSelect = screen.getByLabelText("select-group");

    fireEvent.mouseDown(groupSelect);
    fireEvent.click(screen.getByText("cdscs"));

    const saveButton = screen.getByTestId("save-user-btn");
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(mockToastSuccessMessage).toBe(
        "Local Members updated for testGroup"
      );
    });
  });

  it("updates group privileges", async () => {
    PrivilegeApi.addClusterPrivileges = async () => {
      toast.success("group5 changed to Service Operator");
      return { status: 200 };
    };
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/group5/i)).toBeInTheDocument();
    });

    const privilegeSelect = screen.getByTestId("group-access-dropdown");
    fireEvent.change(privilegeSelect, {
      target: { value: "Service Operator" },
    });

    await waitFor(() => {
      expect(mockToastSuccessMessage).toBe(
        "group5 changed to Service Operator"
      );
    });
  });

  it("shows unsaved changes warning modal", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/group5/i)).toBeInTheDocument();
    });

    const groupSelect = screen.getByLabelText("select-group");

    fireEvent.mouseDown(groupSelect);
    fireEvent.click(screen.getByText("cdscs"));

    const groupsListButton = screen.getByTestId("groups-list-link");
    fireEvent.click(groupsListButton);

    await waitFor(() => {
      expect(screen.getByTestId("warning-modal")).toBeInTheDocument();
    });

    const cancelButton = screen.getByText("Cancel");
    fireEvent.click(cancelButton);

    await waitFor(() => {
      expect(screen.queryByTestId("warning-modal")).toBeNull();
    });
  });

  it("renders RBAC modal for group access tooltip", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/group5/i)).toBeInTheDocument();
    });

    const groupAccessTooltipIcon = screen.getByTestId("group-access-info-icon");
    fireEvent.click(groupAccessTooltipIcon);

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
      expect(screen.getByText(/group5/i)).toBeInTheDocument();
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
    UserGroupApi.groupData = async () => {
      toast.error("Failed to fetch group data");
      return { status: 400 };
    };

    renderComponent();

    await waitFor(() => {
      expect(mockToastErrorMessage).toBe("Failed to fetch group data");
    });
  });

  it("disables Local Members by default unless changes are made and submitted", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/group5/i)).toBeInTheDocument();
    });

    const saveButton = screen.getByTestId("save-user-btn");
    expect(saveButton).toHaveClass("cursor-not-allowed opacity-50");
  });
});
