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
import {
    fireEvent,
    render,
    screen,
    waitFor,
    within,
  } from "@testing-library/react";
  import Users from "../screens/Users";
  import { describe, it, beforeEach, expect, vi } from "vitest";
  import "@testing-library/jest-dom/vitest";
  import { Router } from "react-router";
  import AppContent from "../context/AppContext";
  import { createMemoryHistory } from "history";
  import UserGroupApi from "../api/userGroupApi";
  import { usersList, paginatedUsersList } from "../__mocks__/mockUsersList";
  import { groupsList, paginatedGroupsList } from "../__mocks__/mockGroupsList";
  import { rbacData } from "../__mocks__/mockRbacData";
  import PrivilegeApi from "../api/privilegeApi";
  import toast from "react-hot-toast";
  import { get } from "lodash";
  
  describe("Users and Groups List component", () => {
    const mockClusterName = "testCluster";
    const mockContext = {
      cluster: { cluster_name: mockClusterName },
      setSelectedOption: () => "Users",
      rbacData: {},
      setRbacData: vi.fn(),
      permissionLabelList: [],
      setPermissionLabelList: vi.fn(),
    };
  
    let mockToastSuccessMessage = "";
  
    toast.success = (message) => {
      mockToastSuccessMessage = message as string;
      return "";
    };
  
    beforeEach(async () => {
      vi.clearAllMocks();
      mockToastSuccessMessage = "";
      UserGroupApi.usersList = async () => usersList;
      UserGroupApi.groupsList = async () => groupsList;
      UserGroupApi.getPermissions = async () => rbacData;
      UserGroupApi.userNames = async () => ({ items: [] });
    });
  
    const renderUserComponent = () => {
      render(
        <AppContent.Provider value={mockContext}>
          <Router history={createMemoryHistory()}>
            <Users />
          </Router>
        </AppContent.Provider>
      );
    };
  
    it("renders user component without crashing", async () => {
      renderUserComponent();
    });
  
    //Users Test Cases
  
    it("renders users page without crashing", async () => {
      renderUserComponent();
  
      expect(screen.getByTestId("users-tab")).toBeInTheDocument();
      expect(screen.getByTestId("users-tab")).toHaveClass("active");
    });
  
    it("renders appropriate message if users list is empty", async () => {
      const mockEmptyUsersList = { href: "http://example.com", items: [] };
      UserGroupApi.usersList = async () => mockEmptyUsersList;
  
      renderUserComponent();
  
      await waitFor(() => {
        expect(screen.getByTestId("empty-user-list")).toBeInTheDocument();
      });
    });
  
    it("renders correct number of items in users list", async () => {
      renderUserComponent();
  
      await waitFor(() => {
        screen.getByText(/dsasd/i);
      });
  
      const listContainer = screen.getByTestId("users-list-container");
      const listItems = within(listContainer).getAllByRole("listitem");
  
      expect(listItems).toHaveLength(usersList.items.length);
    });
  
    it("renders users list data for a specific item correctly", async () => {
      renderUserComponent();
  
      await waitFor(() => {
        screen.getByText(/dsasd/i);
      });
  
      const listContainer = screen.getByTestId("users-list-container");
      const allRows = within(listContainer).getAllByRole("listitem");
      const specificRow = allRows.find((row) => within(row).queryByText("dsasd"));
  
      if (!specificRow) {
        throw new Error("No row with the specified text found");
      }
  
      expect(
        within(specificRow).getByText(/Cluster Administrator/i)
      ).toBeInTheDocument();
      expect(within(specificRow).getByText(/Active/i)).toBeInTheDocument();
      expect(within(specificRow).getByText(/Local/i)).toBeInTheDocument();
      expect(within(specificRow).getByText(/gdgdeg/i)).toBeInTheDocument();
      expect(within(specificRow).getByText(/group1/i)).toBeInTheDocument();
    });
  
    it("renders loading spinner when users list data is being fetched", async () => {
      renderUserComponent();
  
      const spinner = screen.getByTestId("admin-spinner");
      expect(spinner).toBeInTheDocument();
    });
  
    it("renders pagination for users list page when items are more than 10", async () => {
      UserGroupApi.usersList = async () => paginatedUsersList;
  
      renderUserComponent();
  
      await waitFor(() => screen.getByText(/dsasd/i));
  
      const pagination = screen.getByTestId("pagination");
      expect(pagination).toBeInTheDocument();
    });
  
    it("checks if AddUser Form is coming on clicking AddUser button", async () => {
      renderUserComponent();
  
      const addUserBtn = screen.getByTestId("add-users-btn");
      expect(addUserBtn).toBeInTheDocument();
  
      fireEvent.click(addUserBtn);
  
      await waitFor(() => {
        screen.getByTestId("add-user-modal");
      });
    });
  
    it("renders delete user confirmation modal on clicking delete button", async () => {
      renderUserComponent();
  
      await waitFor(() => {
        screen.getByText(/dsasd/i);
      });
  
      const deleteUserBtn = screen.getByTestId("delete-user-dsasd-btn");
      expect(deleteUserBtn).toBeInTheDocument();
  
      fireEvent.click(deleteUserBtn);
  
      await waitFor(() => {
        screen.getByTestId("confirmation-modal");
      });
    });
  
    it("renders search filters for users list page", async () => {
      renderUserComponent();
  
      const searchFilterBtn = screen.getByTestId("filter-users-btn");
      expect(searchFilterBtn).toBeInTheDocument();
  
      fireEvent.click(searchFilterBtn);
  
      await waitFor(() => {
        screen.getByTestId("search-filters");
      });
    });
  
    it("deletes a user", async () => {
      UserGroupApi.userData = async () => {};
      UserGroupApi.removeUser = async (userName: string) => {
        toast.success(`${userName} deleted successfully.`);
        return { status: 200 };
      };
      PrivilegeApi.removeViewPrivileges = async () => {
        return { status: 200 };
      };
      PrivilegeApi.removeClusterPrivileges = async () => {
        return { status: 200 };
      };
  
      renderUserComponent();
  
      await waitFor(() => {
        screen.getByText(/dsasd/i);
      });
  
      const deleteUserBtn = screen.getByTestId("delete-user-dsasd-btn");
      expect(deleteUserBtn).toBeInTheDocument();
  
      fireEvent.click(deleteUserBtn);
  
      await waitFor(() => {
        screen.getByTestId("confirmation-modal");
        screen.getByText(/Are you sure you want to delete user "dsasd"/i);
      });
  
      const confirmButton = screen.getByTestId("confirm-ok-btn");
      fireEvent.click(confirmButton);
  
      await waitFor(() => {
        expect(get(mockToastSuccessMessage, "props.children", []).join("")).toBe(
          "dsasd deleted successfully."
        );
      });
    });
  
    //Groups Test Cases
  
    it("renders groups page without crashing", async () => {
      renderUserComponent();
  
      const groupsTab = screen.getByTestId("groups-tab");
      expect(groupsTab).toBeInTheDocument();
  
      fireEvent.click(groupsTab);
  
      await waitFor(() => {
        expect(groupsTab).toHaveClass("active");
      });
    });
  
    it("renders appropriate message if groups list is empty", async () => {
      const mockEmptyGroupsList = { href: "http://example.com", items: [] };
      UserGroupApi.groupsList = async () => mockEmptyGroupsList;
  
      renderUserComponent();
  
      const groupsTab = screen.getByTestId("groups-tab");
      expect(groupsTab).toBeInTheDocument();
  
      fireEvent.click(groupsTab);
  
      await waitFor(() => {
        expect(screen.getByTestId("empty-group-list")).toBeInTheDocument();
      });
    });
  
    it("renders correct number of items in groups list", async () => {
      renderUserComponent();
  
      const groupsTab = screen.getByTestId("groups-tab");
      expect(groupsTab).toBeInTheDocument();
  
      fireEvent.click(groupsTab);
  
      await waitFor(() => {
        screen.getByText(/group5/i);
      });
  
      const listContainer = screen.getByTestId("groups-list-container");
      const listItems = within(listContainer).getAllByRole("listitem");
  
      expect(listItems).toHaveLength(groupsList.items.length);
    });
  
    it("renders groups list data for a specific item correctly", async () => {
      renderUserComponent();
  
      const groupsTab = screen.getByTestId("groups-tab");
      expect(groupsTab).toBeInTheDocument();
  
      fireEvent.click(groupsTab);
  
      await waitFor(() => {
        screen.getByText(/group5/i);
      });
  
      const listContainer = screen.getByTestId("groups-list-container");
      const allRows = within(listContainer).getAllByRole("listitem");
      const specificRow = allRows.find((row) =>
        within(row).queryByText("group5")
      );
  
      if (!specificRow) {
        throw new Error("No row with the specified text found");
      }
  
      expect(within(specificRow).getByText(/Local/i)).toBeInTheDocument();
      expect(within(specificRow).getByText(/0 members/i)).toBeInTheDocument();
    });
  
    it("renders loading spinner when groups list data is being fetched", async () => {
      renderUserComponent();
  
      const groupsTab = screen.getByTestId("groups-tab");
      expect(groupsTab).toBeInTheDocument();
  
      fireEvent.click(groupsTab);
  
      const spinner = screen.getByTestId("admin-spinner");
      expect(spinner).toBeInTheDocument();
    });
  
    it("renders pagination for groups list page when items are more than 10", async () => {
      UserGroupApi.groupsList = async () => paginatedGroupsList;
  
      renderUserComponent();
  
      const groupsTab = screen.getByTestId("groups-tab");
      expect(groupsTab).toBeInTheDocument();
  
      fireEvent.click(groupsTab);
  
      await waitFor(() => screen.getByText(/group5/i));
  
      const pagination = screen.getByTestId("pagination");
      expect(pagination).toBeInTheDocument();
    });
  
    it("checks if AddGroup Form is coming on clicking AddGroup button", async () => {
      renderUserComponent();
  
      const groupsTab = screen.getByTestId("groups-tab");
      expect(groupsTab).toBeInTheDocument();
  
      fireEvent.click(groupsTab);
  
      const addGroupBtn = screen.getByTestId("add-groups-btn");
      expect(addGroupBtn).toBeInTheDocument();
  
      fireEvent.click(addGroupBtn);
  
      await waitFor(() => {
        screen.getByTestId("add-group-modal");
      });
    });
  
    it("renders delete group confirmation modal on clicking delete button", async () => {
      renderUserComponent();
  
      const groupsTab = screen.getByTestId("groups-tab");
      expect(groupsTab).toBeInTheDocument();
  
      fireEvent.click(groupsTab);
  
      await waitFor(() => {
        screen.getByText(/group5/i);
      });
  
      const deleteGroupBtn = screen.getByTestId("delete-group-group5-btn");
      expect(deleteGroupBtn).toBeInTheDocument();
  
      fireEvent.click(deleteGroupBtn);
  
      await waitFor(() => {
        screen.getByTestId("confirmation-modal");
      });
    });
  
    it("renders search filters for groups list page", async () => {
      renderUserComponent();
  
      const groupsTab = screen.getByTestId("groups-tab");
      expect(groupsTab).toBeInTheDocument();
  
      fireEvent.click(groupsTab);
  
      const searchFilterBtn = screen.getByTestId("filter-groups-btn");
      expect(searchFilterBtn).toBeInTheDocument();
  
      fireEvent.click(searchFilterBtn);
  
      await waitFor(() => {
        screen.getByTestId("search-filters");
      });
    });
  
    it("deletes a group", async () => {
      UserGroupApi.groupData = async () => {};
      UserGroupApi.removeGroup = async (groupName: string) => {
        toast.success(`${groupName} deleted successfully.`);
        return { status: 200 };
      };
      PrivilegeApi.removeViewPrivileges = async () => {
        return { status: 200 };
      };
      PrivilegeApi.removeClusterPrivileges = async () => {
        return { status: 200 };
      };
  
      renderUserComponent();
  
      const groupsTab = screen.getByTestId("groups-tab");
      expect(groupsTab).toBeInTheDocument();
  
      fireEvent.click(groupsTab);
  
      await waitFor(() => {
        screen.getByText(/group5/i);
      });
  
      const deleteGroupBtn = screen.getByTestId("delete-group-group5-btn");
      expect(deleteGroupBtn).toBeInTheDocument();
  
      fireEvent.click(deleteGroupBtn);
  
      await waitFor(() => {
        screen.getByTestId("confirmation-modal");
      });
  
      const confirmButton = screen.getByTestId("confirm-ok-btn");
      fireEvent.click(confirmButton);
  
      await waitFor(() => {
        expect(get(mockToastSuccessMessage, "props.children", []).join("")).toBe(
          "group5 deleted successfully."
        );
      });
    });
  });
  