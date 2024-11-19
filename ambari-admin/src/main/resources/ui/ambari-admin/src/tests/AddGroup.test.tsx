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
import AddGroup from "../screens/Users/AddGroup";
import { describe, it, beforeEach, expect, vi } from "vitest";
import "@testing-library/jest-dom/vitest";
import UserGroupApi from "../api/userGroupApi";
import { Router } from "react-router";
import AppContent from "../context/AppContext";
import { createMemoryHistory } from "history";
import toast from "react-hot-toast";
import { userNames } from "../__mocks__/mockUserNames";
import { GroupDataType } from "../api/types";
import { get } from "lodash";
import { rbacData } from "../__mocks__/mockRbacData";

describe("AddGroup component", () => {
  const mockClusterName = "testCluster";
  const mockContext = {
    cluster: { cluster_name: mockClusterName },
    rbacData: {},
    setRbacData: () => vi.fn(),
    permissionLabelList: [],
    setPermissionLabelList: vi.fn(),
  };
  const mockProps = {
    showAddGroupModal: true,
    setShowAddGroupModal: vi.fn(),
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
    UserGroupApi.userNames = async () => userNames;
    UserGroupApi.getPermissions = async () => rbacData;
  });

  const renderComponent = (props = mockProps) => {
    render(
      <AppContent.Provider value={mockContext}>
        <Router history={createMemoryHistory()}>
          <AddGroup {...props} />
        </Router>
      </AppContent.Provider>
    );
  };

  it("renders AddGroup modal without crashing", () => {
    renderComponent();
    expect(screen.getByTestId("add-group-modal")).toBeInTheDocument();
  });

  it("renders spinner while fetching user names", () => {
    renderComponent();
    expect(screen.getByTestId("admin-spinner")).toBeInTheDocument();
  });

  it("should display required errors when input fields are empty", async () => {
    renderComponent();

    await waitFor(() => {
      screen.getByLabelText("select-user");
    });

    const saveButton = screen.getByTestId("add-group-save-btn");
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(screen.getByText("Field required!")).toBeInTheDocument();
    });
  });

  it("renders group name input and validates input", async () => {
    renderComponent();

    await waitFor(() => {
      screen.getByLabelText("select-user");
    });

    const saveButton = screen.getByTestId("add-group-save-btn");
    fireEvent.click(saveButton);

    const groupNameInput = screen.getByTestId("group-name-input");
    fireEvent.change(groupNameInput, {
      target: { value: "invalid&groupname" },
    });

    await waitFor(() => {
      expect(
        screen.getByText("Must not contain special characters!")
      ).toBeInTheDocument();
    });

    fireEvent.change(groupNameInput, {
      target: {
        value:
          "012345678901234567890123456789012345678901234567890123456789012345678901234567891",
      },
    });

    await waitFor(() => {
      expect(
        screen.getByText("Must not be longer than 80 characters!")
      ).toBeInTheDocument();
    });

    fireEvent.change(groupNameInput, {
      target: { value: "validgroupname" },
    });

    await waitFor(() => {
      expect(
        screen.queryByText("Must not be longer than 80 characters!")
      ).not.toBeInTheDocument();
    });
  });

  it("renders the multi-select for user addition to the group", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByLabelText("select-user")).toBeInTheDocument();
    });

    const userSelect = screen.getByLabelText("select-user");

    fireEvent.mouseDown(userSelect);
    fireEvent.click(screen.getByText("cdscs"));

    expect(screen.getByText("cdscs")).toBeInTheDocument();
  });

  it("renders group access dropdown and selects an option", async () => {
    renderComponent();

    await waitFor(() => {
      screen.getByLabelText("select-user");
    });

    const groupAccessSelect = screen.getByTestId(
      "group-access-dropdown"
    ) as HTMLSelectElement;
    fireEvent.change(groupAccessSelect, { target: { value: "Cluster User" } });

    expect(groupAccessSelect.value).toBe("Cluster User");
  });

  it("calls handleSave on form submit with valid data", async () => {
    UserGroupApi.addGroup = async (groupData: GroupDataType[]) => {
      toast.success(
        `Created group ${get(groupData[0], "Groups/group_name", "")}`
      );
      return { status: 200 };
    };

    renderComponent();

    await waitFor(() => {
      screen.getByLabelText("select-user");
    });

    const groupNameInput = screen.getByTestId("group-name-input");
    fireEvent.change(groupNameInput, { target: { value: "validgroupname" } });

    const saveButton = screen.getByTestId("add-group-save-btn");
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(mockToastSuccessMessage).toBe("Created group validgroupname");
    });
  });

  it("calls handleSave on form submit but API got failed", async () => {
    UserGroupApi.addGroup = async (groupData: GroupDataType[]) => {
      toast.error(
        `Error while adding group ${get(groupData[0], "Groups/group_name", "")}`
      );
      return { status: 400 };
    };

    renderComponent();

    await waitFor(() => {
      screen.getByLabelText("select-user");
    });

    const groupNameInput = screen.getByTestId("group-name-input");
    fireEvent.change(groupNameInput, { target: { value: "validgroupname" } });

    const saveButton = screen.getByTestId("add-group-save-btn");
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(mockToastErrorMessage).toBe(
        "Error while adding group validgroupname"
      );
    });
  });

  it("calls handleCancel and shows warning modal if form is dirty", async () => {
    renderComponent();

    await waitFor(() => {
      screen.getByLabelText("select-user");
    });

    const groupNameInput = screen.getByTestId("group-name-input");
    fireEvent.change(groupNameInput, { target: { value: "validgroupname" } });

    const cancelButton = screen.getByTestId("add-group-cancel-btn");
    fireEvent.click(cancelButton);

    expect(screen.getByTestId("warning-modal")).toBeInTheDocument();
  });

  it("renders RoleBasedAccessControl modal", async () => {
    renderComponent();

    await waitFor(() => {
      screen.getByLabelText("select-user");
    });

    const roleAccessIcon = screen.getByTestId("group-access-help-icon");
    fireEvent.click(roleAccessIcon);

    expect(screen.getByText("Role Based Access Control")).toBeInTheDocument();
  });
});
