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

/* eslint-disable @typescript-eslint/no-explicit-any */

import { Button, Col, Nav, Row, Tab } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faFilter,
  faPencil,
  faTrashCan,
} from "@fortawesome/free-solid-svg-icons";
import DefaultButton from "../../components/DefaultButton";
import { useContext, useEffect, useState } from "react";
import { Link, useHistory, useLocation } from "react-router-dom";
import {
  GroupInfoType,
  GroupsListType,
  UserInfoType,
  UsersListType,
} from "./types";
import AppContent from "../../context/AppContext";
import { get, set, startCase } from "lodash";
import { PrivilegeType } from "./enums";
import toast from "react-hot-toast";
import UserGroupApi from "../../api/userGroupApi";
import PrivilegeApi from "../../api/privilegeApi";
import ConfirmationModal from "../../components/ConfirmationModal";
import Spinner from "../../components/Spinner";
import usePagination from "../../hooks/usePagination";
import Paginator from "../../components/Paginator";
import ComboSearch from "../../components/ComboSearch";
import Table from "../../components/Table";
import {
  decryptData,
  getFromLocalStorage,
  parseJSONData,
} from "../../api/Utility";

export default function Users() {
  const [currentLoggedInUser, setCurrentLoggedInUser] = useState("");
  const [showAddUserModal, setShowAddUserModal] = useState(false);
  const [showAddGroupModal, setShowAddGroupModal] = useState(false);
  const [loading, setLoading] = useState(false);
  const [usersList, setUsersList] = useState<UsersListType | null>(null);
  const [groupsList, setGroupsList] = useState<GroupsListType | null>(null);
  const {
    cluster: { cluster_name: clusterName },
    setSelectedOption,
  } = useContext(AppContent);
  const history = useHistory();
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const [activeTab, setActiveTab] = useState(
    searchParams.get("tab") || "users"
  );
  const [showDeleteGroupModal, setShowDeleteGroupModal] = useState(false);
  const [groupToDelete, setGroupToDelete] = useState("");
  const [showDeleteUserModal, setShowDeleteUserModal] = useState(false);
  const [userToDelete, setUserToDelete] = useState("");
  const [showUserFilters, setShowUserFilters] = useState(false);
  const [filteredUsers, setFilteredUsers] = useState<UserInfoType[]>([]);
  const [showGroupFilters, setShowGroupFilters] = useState(false);
  const [filteredGroups, setFilteredGroups] = useState<GroupInfoType[]>([]);

  const {
    currentItems: currentItemsUsers,
    changePage: changePageUsers,
    currentPage: currentPageUsers,
    maxPage: maxPageUsers,
    itemsPerPage: usersPerPage,
    setItemsPerPage: setUsersPerPage,
  } = usePagination(filteredUsers);
  const {
    currentItems: currentItemsGroups,
    changePage: changePageGroups,
    currentPage: currentPageGroups,
    maxPage: maxPageGroups,
    itemsPerPage: groupsPerPage,
    setItemsPerPage: setGroupsPerPage,
  } = usePagination(filteredGroups);

  useEffect(() => {
    let ambariKey = getFromLocalStorage("ambari");
    if (ambariKey) {
      let parsedAmbariKey = parseJSONData(decryptData(ambariKey));
      setCurrentLoggedInUser(get(parsedAmbariKey, "app.loginName", ""));
    }
  }, []);

  useEffect(() => {
    if (activeTab === "users") {
      setSelectedOption("Users");
      getUsersList();
    } else if (activeTab === "groups") {
      setSelectedOption("Users");
      getGroupsList();
    }
    searchParams.set("tab", activeTab);
    history.push({
      pathname: location.pathname,
      search: searchParams.toString(),
    });
  }, [activeTab]);

  useEffect(() => {
    const searchParams = new URLSearchParams(location.search);
    const tab = searchParams.get("tab");
    if (tab) {
      setActiveTab(tab);
    }
  }, [location]);

  const columnsInUserList = [
    {
      header: "Username",
      accessorKey: "Users.user_name",
      id: "user_name",
      width: "30%",
    },
    {
      header: "Role",
      accessorKey: "Users.user_role",
      id: "user_role",
      width: "15%",
    },
    {
      header: "Status",
      accessorKey: "active",
      id: "active",
      width: "10%",
      cell: (info: any) => {
        return info.row.original.Users.active === true ? "Active" : "Inactive";
      },
    },
    {
      header: "Type",
      accessorKey: "Users.user_type",
      id: "user_type",
      width: "10%",
    },
    {
      header: "Group",
      accessorKey: "groups",
      id: "groups",
      width: "30%",
      cell: (info: any) => {
        return get(info, "row.original.Users.groups")?.length
          ? get(info, "row.original.Users.groups", [])
              ?.map((group: any) => group)
              .join(" ")
          : "-";
      },
    },
    {
      header: "Actions",
      width: "5%",
      cell: (info: any) => {
        return (
          <div className="d-flex">
            <Link
              to={`/users/${get(info, "row.original.Users.user_name")}/edit`}
            >
              <FontAwesomeIcon
                icon={faPencil}
                className="me-3"
                style={{ cursor: "pointer" }}
              />
            </Link>
            <Button
              className={
                get(info, "row.original.Users.user_name") ===
                  currentLoggedInUser ||
                get(info, "row.original.Users.user_type").toLowerCase() !==
                  "local"
                  ? "btn-wrapping-icon opacity-25 cursor-not-allowed"
                  : "btn-wrapping-icon"
              }
              onClick={() => {
                if (
                  get(info, "row.original.Users.user_name") !==
                    currentLoggedInUser &&
                  get(info, "row.original.Users.user_type").toLowerCase() ===
                    "local"
                ) {
                  setUserToDelete(get(info, "row.original.Users.user_name"));
                  setShowDeleteUserModal(true);
                }
              }}
              data-testid={`delete-user-${get(
                info,
                "row.original.Users.user_name"
              )}-btn`}
            >
              <FontAwesomeIcon icon={faTrashCan} />
            </Button>
          </div>
        );
      },
    },
  ];

  const columnsInGroupList = [
    {
      header: "Group name",
      accessorKey: "Groups.group_name",
      id: "group_name",
      width: "65%",
    },
    {
      header: "Type",
      accessorKey: "Groups.group_type",
      id: "group_type",
      width: "15%",
    },
    {
      header: "Members",
      width: "15%",
      cell: (info: any) => {
        return get(info, "row.original.members")?.length
          ? get(info, "row.original.members")?.length.toString() + " members"
          : "0 members";
      },
    },
    {
      header: "Actions",
      width: "5%",
      cell: (info: any) => {
        return (
          <div className="d-flex">
            <Link
              to={`/groups/${get(info, "row.original.Groups.group_name")}/edit`}
            >
              <FontAwesomeIcon
                icon={faPencil}
                className="me-3"
                style={{ cursor: "pointer" }}
              />
            </Link>
            <Button
              className={
                get(info, "row.original.Groups.group_type").toLowerCase() !==
                "local"
                  ? "btn-wrapping-icon opacity-25 cursor-not-allowed"
                  : "btn-wrapping-icon"
              }
              onClick={() => {
                if (
                  get(info, "row.original.Groups.group_type").toLowerCase() ===
                  "local"
                ) {
                  setGroupToDelete(get(info, "row.original.Groups.group_name"));
                  setShowDeleteGroupModal(true);
                }
              }}
              data-testid={`delete-group-${get(
                info,
                "row.original.Groups.group_name"
              )}-btn`}
            >
              <FontAwesomeIcon icon={faTrashCan} />
            </Button>
          </div>
        );
      },
    },
  ];

  async function getUsersList() {
    setLoading(true);
    const data: any = await UserGroupApi.usersList("Users/*,privileges/*");
    get(data, "items", []).forEach((item: any) => {
      set(
        item,
        "Users.user_type",
        startCase(get(item, "Users.user_type", "").toLowerCase())
      );
      const ambariAdminPrivileges = get(
        get(item, "privileges", []).filter(
          (privilege: any) =>
            get(privilege, "PrivilegeInfo.type") === PrivilegeType.AMBARI
        ),
        "[0].PrivilegeInfo.permission_label",
        ""
      );
      const clusterUserPrivileges = get(
        get(item, "privileges", []).filter(
          (privilege: any) =>
            get(privilege, "PrivilegeInfo.type") === PrivilegeType.CLUSTER
        ),
        "[0].PrivilegeInfo.permission_label",
        ""
      );
      if (ambariAdminPrivileges !== "") {
        set(item, "Users.user_role", ambariAdminPrivileges);
      } else {
        set(item, "Users.user_role", clusterUserPrivileges);
      }
      if (get(item, "Users.active")) {
        set(item, "Users.user_status", "Active");
      } else {
        set(item, "Users.user_status", "Inactive");
      }
    });
    setUsersList(data);
    setFilteredUsers(get(data, "items", []));
    setLoading(false);
  }

  async function getGroupsList() {
    setLoading(true);
    const data: any = await UserGroupApi.groupsList("*");
    get(data, "items", []).forEach((item: any) => {
      set(
        item,
        "Groups.group_type",
        startCase(get(item, "Groups.group_type", "").toLowerCase())
      );
    });
    setGroupsList(data);
    setFilteredGroups(get(data, "items", []));
    setLoading(false);
  }

  const removePrivileges = async (privileges: any) => {
    get(privileges, "privileges", []).forEach(async (privilege: any) => {
      if (get(privilege, "PrivilegeInfo.type") === PrivilegeType.CLUSTER) {
        await PrivilegeApi.removeClusterPrivileges(
          clusterName,
          get(privilege, "PrivilegeInfo.privilege_id")
        );
      } else if (get(privilege, "PrivilegeInfo.type") === PrivilegeType.VIEW) {
        await PrivilegeApi.removeViewPrivileges(
          get(privilege, "PrivilegeInfo.view_name"),
          get(privilege, "PrivilegeInfo.version"),
          get(privilege, "PrivilegeInfo.instance_name"),
          get(privilege, "PrivilegeInfo.privilege_id")
        );
      }
    });
  };

  const deleteUser = async (userName: string) => {
    setShowDeleteUserModal(false);
    const privileges = await UserGroupApi.userData(
      userName,
      "privileges/PrivilegeInfo/*"
    );
    await UserGroupApi.removeUser(userName);
    toast.success(
      <div className="toast-message">{userName} deleted successfully.</div>
    );
    await removePrivileges(privileges);
    getUsersList();
  };

  const deleteGroup = async (groupName: string) => {
    setShowDeleteGroupModal(false);
    const privileges = await UserGroupApi.groupData(
      groupName,
      "privileges/PrivilegeInfo/*"
    );
    await UserGroupApi.removeGroup(groupName);
    toast.success(
      <div className="toast-message">{groupName} deleted successfully.</div>
    );
    await removePrivileges(privileges);
    getGroupsList();
  };

  return (
    <div className="make-all-grey">
      {showDeleteUserModal ? (
        <ConfirmationModal
          isOpen={showDeleteUserModal}
          onClose={() => setShowDeleteUserModal(false)}
          modalTitle={"Delete User"}
          modalBody={`Are you sure you want to delete user "${userToDelete}"?`}
          successCallback={() => deleteUser(userToDelete)}
          buttonVariant="danger"
        />
      ) : null}
      {showDeleteGroupModal ? (
        <ConfirmationModal
          isOpen={showDeleteGroupModal}
          onClose={() => setShowDeleteGroupModal(false)}
          modalTitle={"Delete Group"}
          modalBody={`Are you sure you want to delete group "${groupToDelete}"?`}
          successCallback={() => deleteGroup(groupToDelete)}
          buttonVariant="danger"
        />
      ) : null}
      <Tab.Container activeKey={activeTab}>
        <Row>
          <Col>
            <Nav
              variant="underline"
              onSelect={(selectedKey) => {
                if (selectedKey && activeTab !== selectedKey) {
                  setActiveTab(selectedKey);
                }
              }}
            >
              <Nav.Item>
                <Nav.Link
                  eventKey="users"
                  className={
                    activeTab === "users"
                      ? "tab-border active p-2"
                      : "tab-border p-2"
                  }
                  data-testid="users-tab"
                >
                  USERS
                </Nav.Link>
              </Nav.Item>
              <Nav.Item>
                <Nav.Link
                  eventKey="groups"
                  className={
                    activeTab === "groups"
                      ? "tab-border active p-2"
                      : "tab-border p-2"
                  }
                  data-testid="groups-tab"
                >
                  GROUPS
                </Nav.Link>
              </Nav.Item>
            </Nav>
          </Col>
          <Col className="d-flex justify-content-end">
            <Tab.Content>
              <Tab.Pane eventKey="users">
                <DefaultButton
                  className="me-2"
                  onClick={() => setShowUserFilters(!showUserFilters)}
                  data-testid="filter-users-btn"
                >
                  <FontAwesomeIcon icon={faFilter} />
                </DefaultButton>
                <DefaultButton
                  onClick={() => setShowAddUserModal(true)}
                  data-testid="add-users-btn"
                >
                  ADD USERS
                </DefaultButton>
              </Tab.Pane>
              <Tab.Pane eventKey="groups">
                <DefaultButton
                  className="me-2"
                  onClick={() => setShowGroupFilters(!showGroupFilters)}
                  data-testid="filter-groups-btn"
                >
                  <FontAwesomeIcon icon={faFilter} />
                </DefaultButton>
                <DefaultButton
                  onClick={() => setShowAddGroupModal(true)}
                  data-testid="add-groups-btn"
                >
                  ADD GROUPS
                </DefaultButton>
              </Tab.Pane>
            </Tab.Content>
          </Col>
        </Row>
        {loading ? (
          <Spinner />
        ) : (
          <Row className="scrollable">
            <Tab.Content className="mt-3">
              <Tab.Pane eventKey="users" data-testid="users-list-container">
                {showUserFilters ? (
                  <div className="d-flex">
                    <ComboSearch
                      fields={[
                        { label: "Username", value: "Users.user_name" },
                        { label: "Role", value: "Users.user_role" },
                        { label: "Status", value: "Users.user_status" },
                        { label: "Type", value: "Users.user_type" },
                        { label: "Group", value: "Users.groups" },
                      ]}
                      valueMappings={{
                        username: "Users.user_name",
                        role: "permission_label",
                        status: "Users.active",
                        type: "Users.user_type",
                        group: "Users.groups",
                      }}
                      searchCallback={(filteredData: UserInfoType[]) => {
                        setFilteredUsers(filteredData);
                      }}
                      data={usersList?.items || []}
                    />
                  </div>
                ) : null}
                <Table
                  striped={true}
                  hover={true}
                  columns={columnsInUserList}
                  data={currentItemsUsers}
                />
                {usersList && usersList.items.length === 0 ? (
                  <div
                    className="d-flex justify-content-center mt-4"
                    data-testid="empty-user-list"
                  >
                    NO USERS TO DISPLAY.
                  </div>
                ) : null}
              </Tab.Pane>
              <Tab.Pane eventKey="groups" data-testid="groups-list-container">
                {showGroupFilters ? (
                  <div className="d-flex">
                    <ComboSearch
                      fields={[
                        { label: "Group name", value: "Groups.group_name" },
                        { label: "Type", value: "Groups.group_type" },
                      ]}
                      valueMappings={{
                        groupname: "Groups.group_name",
                        type: "Groups.group_type",
                      }}
                      searchCallback={(filteredData: GroupInfoType[]) => {
                        setFilteredGroups(filteredData);
                      }}
                      data={groupsList?.items || []}
                    />
                  </div>
                ) : null}
                <Table
                  striped={true}
                  hover={true}
                  columns={columnsInGroupList}
                  data={currentItemsGroups}
                />
                {groupsList && groupsList.items.length === 0 ? (
                  <div
                    className="d-flex justify-content-center mt-4"
                    data-testid="empty-group-list"
                  >
                    NO GROUPS TO DISPLAY.
                  </div>
                ) : null}
              </Tab.Pane>
            </Tab.Content>
          </Row>
        )}
      </Tab.Container>
      <div>
        {activeTab === "users" ? (
          <Paginator
            currentPage={currentPageUsers}
            maxPage={maxPageUsers}
            changePage={changePageUsers}
            itemsPerPage={usersPerPage}
            setItemsPerPage={setUsersPerPage}
            totalItems={filteredUsers.length}
          />
        ) : (
          <Paginator
            currentPage={currentPageGroups}
            maxPage={maxPageGroups}
            changePage={changePageGroups}
            itemsPerPage={groupsPerPage}
            setItemsPerPage={setGroupsPerPage}
            totalItems={filteredGroups.length}
          />
        )}
      </div>
    </div>
  );
}
