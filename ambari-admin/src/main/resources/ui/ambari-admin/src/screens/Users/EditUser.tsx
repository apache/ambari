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
import { useContext, useEffect, useRef, useState } from "react";
import { Alert, Button, Form, OverlayTrigger, Tooltip } from "react-bootstrap";
import { useParams } from "react-router";
import { Link, useHistory, Prompt } from "react-router-dom";
import DefaultButton from "../../components/DefaultButton";
import RoleBasedAccessControl from "./RoleBasedAccessControl";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faCheck,
  faCloud,
  faQuestionCircle,
  faTh,
  faTrashCan,
} from "@fortawesome/free-solid-svg-icons";
import Select from "react-select";
import { get, startCase } from "lodash";
import { permissionLabelToName, userAccessOptions } from "./constants";
import ChangePasswordModal from "./ChangePasswordModal";
import {
  UserInfoType,
  GroupNamesType,
  PermissionLabel,
  SelectOptionType,
} from "./types";
import { PrivilegeType, PrincipalType, PermissionNameType, DefaultAccess } from "./enums";
import ConfirmationModal from "../../components/ConfirmationModal";
import UserGroupApi from "../../api/userGroupApi";
import Spinner from "../../components/Spinner";
import { PrivilegesDataType } from "../../api/types";
import PrivilegeApi from "../../api/privilegeApi";
import toast from "react-hot-toast";
import AppContent from "../../context/AppContext";
import Table from "../../components/Table";
import WarningModal from "./WarningModal";
import {
  decryptData,
  getFromLocalStorage,
  parseJSONData,
} from "../../api/Utility";

type ParamsType = {
  userName: string;
};

export const constructLinkToEditInstance = (
  viewName: string,
  version: string,
  instanceName: string
) => {
  return `/views/${viewName}/versions/${version}/instances/${instanceName}/edit`;
};

export default function EditUser() {
  const params: ParamsType = useParams();

  const [currentLoggedInUser, setCurrentLoggedInUser] = useState("");
  const [showUserAccessModal, setShowUserAccessModal] = useState(false);
  const [showChangeStatusModal, setShowChangeStatusModal] = useState(false);
  const [showChangeAdminPriviligesModal, setShowChangeAdminPriviligesModal] =
    useState(false);
  const [showChangePasswordModal, setShowChangePasswordModal] = useState(false);

  const [loading, setLoading] = useState(false);
  const [userInfo, setUserInfo] = useState<UserInfoType | null>(null);
  const [groupNames, setGroupNames] = useState<GroupNamesType | null>(null);
  const [userGroups, setUserGroups] = useState<SelectOptionType[]>([]);
  const {
    cluster: { cluster_name: clusterName },
    setSelectedOption,
  } = useContext(AppContent);
  const [showUnsavedChangesWarning, setShowUnsavedChangesWarning] =
    useState(false);
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(true);
  const [isNavigating, setIsNavigating] = useState(false);
  const [nextLocation, setNextLocation] = useState();
  const history = useHistory();

  const previousMembers = useRef<string[]>([]);
  const newMembers = useRef<string[]>([]);

  useEffect(() => {
    setSelectedOption("Users");
    async function getGroupNames() {
      setLoading(true);
      const data: any = await UserGroupApi.groupNames();
      setGroupNames(data);
      setLoading(false);
    }
    getGroupNames();
    getUserData();
    let ambariKey = getFromLocalStorage("ambari");
    if (ambariKey) {
      let parsedAmbariKey = parseJSONData(decryptData(ambariKey));
      setCurrentLoggedInUser(get(parsedAmbariKey, "app.loginName", ""));
    }
  }, []);

  useEffect(() => {
    previousMembers.current = get(userInfo, "Users.groups", [] as string[]);
  }, [userInfo]);

  useEffect(() => {
    newMembers.current = userGroups.map(
      (group: SelectOptionType) => group.value
    );
    setHasUnsavedChanges(
      JSON.stringify(previousMembers.current) !==
        JSON.stringify(newMembers.current)
    );
  }, [userGroups]);

  const groupOptions = get(groupNames, "items", []).map((group: any) => ({
    value: get(group, "Groups.group_name"),
    label: get(group, "Groups.group_name"),
  }));

  const columnsInUserPrivilegesCluster = [
    {
      header: "Cluster",
      width: "80%",
      cell: (info: any) => {
        return (
          <div>
            <FontAwesomeIcon
              icon={faCloud}
              height={13}
              width={13}
              className="me-1"
            />
            <Link to={"/clusterInformation"} className="custom-link">
              {get(info, "row.original.PrivilegeInfo.cluster_name")}
            </Link>
          </div>
        );
      },
    },
    {
      header: "Cluster Role",
      width: "20%",
      cell: (info: any) => {
        return (
          <div>{get(info, "row.original.PrivilegeInfo.permission_label")}</div>
        );
      },
    },
  ];

  const columnsInUserPrivilegesView = [
    {
      header: "View",
      width: "80%",
      cell: (info: any) => {
        return (
          <div className="w-100">
            <FontAwesomeIcon
              icon={faTh}
              height={13}
              width={13}
              className="me-1"
            />
            <Link
              to={constructLinkToEditInstance(
                get(info, "row.original.PrivilegeInfo.view_name"),
                get(info, "row.original.PrivilegeInfo.version"),
                get(info, "row.original.PrivilegeInfo.instance_name")
              )}
              className="custom-link"
            >
              {get(info, "row.original.PrivilegeInfo.instance_name")}
            </Link>
          </div>
        );
      },
    },
    {
      header: "View Permissions",
      cell: (info: any) => {
        return (
          <div className="d-flex justify-content-between">
            <div className="d-flex align-items-center">
              {startCase(
                get(
                  info,
                  "row.original.PrivilegeInfo.permission_label",
                  ""
                ).toLowerCase()
              )}
            </div>
            <Button
              className="btn-wrapping-icon make-all-grey"
              onClick={() =>
                deleteViewPrivilege(
                  get(info, "row.original.PrivilegeInfo.view_name"),
                  get(info, "row.original.PrivilegeInfo.version"),
                  get(info, "row.original.PrivilegeInfo.instance_name"),
                  get(info, "row.original.PrivilegeInfo.privilege_id")
                )
              }
            >
              <FontAwesomeIcon
                icon={faTrashCan}
                data-testid={`remove-privilege-icon-${get(
                  info,
                  "row.original.PrivilegeInfo.instance_name"
                )}`}
              />
            </Button>
          </div>
        );
      },
    },
  ];

  async function getUserData() {
    setLoading(true);
    const data: any = await UserGroupApi.userData(
      params.userName,
      "privileges/PrivilegeInfo,Users"
    );
    setUserInfo(data);
    setUserGroups(
      get(data, "Users.groups", []).map((group: any) => ({
        value: group,
        label: group,
      })) as SelectOptionType[]
    );
    setLoading(false);
  }

  const updateUserData = async (key: string, value?: boolean) => {
    if (key === "groups") {
      const membersToAdd: string[] = newMembers.current.filter(
        (member) => !previousMembers.current.includes(member)
      );
      const membersToRemove: string[] = previousMembers.current.filter(
        (member) => !newMembers.current.includes(member)
      );
      const addMembersPromises = membersToAdd.map((member) =>
        UserGroupApi.addMember(member, get(userInfo, "Users.user_name", ""))
      );
      const removeMembersPromises = membersToRemove.map((member) =>
        UserGroupApi.removeMember(member, get(userInfo, "Users.user_name", ""))
      );
      await Promise.all([...addMembersPromises, ...removeMembersPromises]);
      toast.success(
        <div className="toast-message">
          Local Group Membership updated for{" "}
          {get(userInfo, "Users.user_name", "")}
        </div>
      );
    } else {
      const userData = {
        ["Users/" + key]: value,
      };
      await UserGroupApi.updateUser(
        get(userInfo, "Users.user_name", ""),
        userData
      );
      toast.success(
        <div className="toast-message">
          The {key} status for {get(userInfo, "Users.user_name", "")} is
          updated.
        </div>
      );
    }
    getUserData();
  };

  const updatePassword = async (
    yourPassword: string,
    newUserPassword: string
  ) => {
    const userPasswordData = {
      "Users/old_password": yourPassword,
      "Users/password": newUserPassword,
    };
    await UserGroupApi.updateUser(
      get(userInfo, "Users.user_name", ""),
      userPasswordData
    );
    toast.success(
      <div className="toast-message">
        Password changed for {get(userInfo, "Users.user_name", "")}
      </div>
    );
    setShowChangePasswordModal(false);
  };

  const updateUserDataPrivileges = async (value: PermissionLabel) => {
    if (value === DefaultAccess.NONE) {
      const currentClusterPrivileges = get(userInfo, "privileges", []).filter(
        (privilige) =>
          get(privilige, "PrivilegeInfo.type") === PrivilegeType.CLUSTER &&
          get(privilige, "PrivilegeInfo.principal_type") === PrincipalType.USER
      );
      const removePrivilegesPromises = currentClusterPrivileges.map(
        (privilege: any) =>
          PrivilegeApi.removeClusterPrivileges(
            clusterName,
            get(privilege, "PrivilegeInfo.privilege_id")
          )
      );
      Promise.all(removePrivilegesPromises).then(() => {
        toast.success(
          <div className="toast-message">
            {get(userInfo, "Users.user_name", "")}'s explicit privilege has been
            changed to 'NONE'. Any privilege now seen for this user comes
            through its Group(s).
          </div>
        );
        getUserData();
      });
    } else {
      const privilegesData: PrivilegesDataType[] = [
        {
          PrivilegeInfo: {
            permission_name: permissionLabelToName[value],
            principal_name: get(userInfo, "Users.user_name", ""),
            principal_type: PrincipalType.USER,
          },
        },
      ];
      await PrivilegeApi.addClusterPrivileges(clusterName, privilegesData);
      toast.success(
        <div className="toast-message">
          {get(userInfo, "Users.user_name", "")} changed to {value}
        </div>
      );
      getUserData();
    }
  };

  const deleteViewPrivilege = async (
    view_name: string,
    version: string,
    instance_name: string,
    privilege_id: string
  ) => {
    await PrivilegeApi.removeViewPrivileges(
      view_name,
      version,
      instance_name,
      privilege_id
    );
    toast.success(
      <div className="toast-message">
        {instance_name} view privilege is removed for{" "}
        {get(userInfo, "Users.user_name", "")}
      </div>
    );
    getUserData();
  };

  const handleBlockedNavigation = (nextLocation: any) => {
    if (hasUnsavedChanges) {
      setShowUnsavedChangesWarning(true);
      setIsNavigating(true);
      setNextLocation(nextLocation);
      return false;
    }
    return true;
  };

  const handleWarningDiscard = () => {
    if (nextLocation) {
      setShowUnsavedChangesWarning(false);
      setIsNavigating(false);
      setHasUnsavedChanges(false);
      setTimeout(() => {
        history.push(get(nextLocation, "pathname"));
      }, 0);
    }
  };

  const handleWarningSave = () => {
    updateUserData("groups");
    setShowUnsavedChangesWarning(false);
    setIsNavigating(false);
  };

  return (
    <div>
      <Prompt when={hasUnsavedChanges} message={handleBlockedNavigation} />
      {isNavigating && showUnsavedChangesWarning ? (
        <WarningModal
          isOpen={showUnsavedChangesWarning}
          onClose={() => setShowUnsavedChangesWarning(false)}
          handleWarningDiscard={handleWarningDiscard}
          handleWarningSave={handleWarningSave}
        />
      ) : null}
      <div className="d-flex flex-wrap">
        <Link
          to={"/userManagement?tab=users"}
          className="custom-link"
          data-testid="users-list-link"
        >
          <h4>Users</h4>
        </Link>
        <h4 className="ms-2 make-all-grey">{`/ ${get(
          userInfo,
          "Users.user_name",
          ""
        )}`}</h4>
      </div>
      <hr className="mb-4" />
      {loading ? (
        <Spinner />
      ) : (
        <Form className="d-flex flex-column">
          <Form.Group className="d-flex mb-4">
            <Form.Label className="width-15 mt-2">Type</Form.Label>
            <Form.Control
              value={startCase(
                get(userInfo, "Users.user_type", "").toLowerCase()
              )}
              readOnly
              plaintext
              className="ps-4"
            />
          </Form.Group>
          <Form.Group className="d-flex mb-4">
            <Form.Label className="mt-2 width-15">Status</Form.Label>
            <div className="d-flex">
              {get(userInfo, "Users.user_name", "") === currentLoggedInUser ? (
                <OverlayTrigger
                  key="top"
                  placement="top"
                  overlay={<Tooltip>Cannot Change Status</Tooltip>}
                >
                  <div>
                    <Form.Check
                      type="switch"
                      checked={get(userInfo, "Users.active", false)}
                      onChange={() => setShowChangeStatusModal(true)}
                      className="custom-form-check cursor-not-allowed"
                      disabled={
                        get(userInfo, "Users.user_name", "") ===
                        currentLoggedInUser
                      }
                    />
                  </div>
                </OverlayTrigger>
              ) : (
                <Form.Check
                  type="switch"
                  checked={get(userInfo, "Users.active", false)}
                  onChange={() => setShowChangeStatusModal(true)}
                  className="custom-form-check"
                  data-testid="user-status-switch"
                />
              )}
              {get(userInfo, "Users.active", false) ? (
                <span className="m-2 ps-2">Active</span>
              ) : (
                <span className="m-2 ps-2">Inactive</span>
              )}
            </div>
            <ConfirmationModal
              isOpen={showChangeStatusModal}
              onClose={() => setShowChangeStatusModal(false)}
              modalTitle={"Change Status"}
              modalBody={`Are you sure you want to change status for user "${get(
                userInfo,
                "Users.user_name",
                ""
              )}
            " to 
            ${get(userInfo, "Users.active", false) ? "Inactive" : "Active"}
            ?`}
              successCallback={() => {
                updateUserData("active", !get(userInfo, "Users.active", false));
                setShowChangeStatusModal(false);
              }}
            />
          </Form.Group>
          <Form.Group className="d-flex mb-4">
            <Form.Label className="mt-2 width-15">Ambari Admin</Form.Label>
            <div className="d-flex">
              {get(userInfo, "Users.user_name", "") === currentLoggedInUser ? (
                <OverlayTrigger
                  key="top"
                  placement="top"
                  overlay={<Tooltip>Cannot Change Admin</Tooltip>}
                >
                  <div>
                    <Form.Check
                      type="switch"
                      checked={get(userInfo, "Users.admin", false)}
                      onChange={() => setShowChangeAdminPriviligesModal(true)}
                      className="custom-form-check cursor-not-allowed"
                      disabled={
                        get(userInfo, "Users.user_name", "") ===
                        currentLoggedInUser
                      }
                    />
                  </div>
                </OverlayTrigger>
              ) : (
                <Form.Check
                  type="switch"
                  checked={get(userInfo, "Users.admin", false)}
                  onChange={() => setShowChangeAdminPriviligesModal(true)}
                  className="custom-form-check"
                  data-testid="ambari-admin-switch"
                />
              )}
              {get(userInfo, "Users.admin", false) ? (
                <span className="m-2 ps-2">Yes</span>
              ) : (
                <span className="m-2 ps-2">No</span>
              )}
            </div>
            <ConfirmationModal
              isOpen={showChangeAdminPriviligesModal}
              onClose={() => setShowChangeAdminPriviligesModal(false)}
              modalTitle={"Change Admin Privilege"}
              modalBody={`Are you sure you want to
            ${get(userInfo, "Users.admin", false) ? "revoke" : "grant"} 
            Admin privilege to user "${get(userInfo, "Users.user_name", "")}"?`}
              successCallback={() => {
                updateUserData("admin", !get(userInfo, "Users.admin", false));
                setShowChangeAdminPriviligesModal(false);
              }}
            />
          </Form.Group>
          <Form.Group className="d-flex mb-4">
            <Form.Label className="mt-2 width-15">Password</Form.Label>
            <DefaultButton
              onClick={() => {
                if (
                  get(userInfo, "Users.user_type", "").toLowerCase() === "local"
                ) {
                  setShowChangePasswordModal(true);
                }
              }}
              className={
                get(userInfo, "Users.user_type", "").toLowerCase() !== "local"
                  ? "cursor-not-allowed opacity-50"
                  : ""
              }
              data-testid="change-password-btn"
            >
              Change Password
            </DefaultButton>
            <ChangePasswordModal
              isOpen={showChangePasswordModal}
              onClose={() => setShowChangePasswordModal(false)}
              userName={get(userInfo, "Users.user_name", "")}
              updatePassword={(yourPassword, newUserPassword) => {
                updatePassword(yourPassword, newUserPassword);
              }}
            />
          </Form.Group>
          <Form.Group className="d-flex mb-4">
            <Form.Label className="mt-2 width-15">
              Local Group Membership
            </Form.Label>
            <Select
              isMulti
              name="groups"
              options={groupOptions}
              className="basic-multi-select w-75"
              placeholder="Add Group"
              value={userGroups}
              onChange={(e) => setUserGroups(e as SelectOptionType[])}
              isDisabled={
                get(userInfo, "Users.user_type", "").toLowerCase() !== "local"
              }
              aria-label="select-group"
            ></Select>
            <DefaultButton
              onClick={() => {
                if (
                  get(userInfo, "Users.user_type", "").toLowerCase() ===
                    "local" &&
                  JSON.stringify(previousMembers) !== JSON.stringify(newMembers)
                ) {
                  updateUserData("groups");
                }
              }}
              className={
                get(userInfo, "Users.user_type", "").toLowerCase() !==
                  "local" ||
                JSON.stringify(previousMembers) === JSON.stringify(newMembers)
                  ? "cursor-not-allowed opacity-50 ms-2"
                  : "ms-2"
              }
              data-testid="save-groups-btn"
            >
              <FontAwesomeIcon icon={faCheck} />
            </DefaultButton>
          </Form.Group>
          <Form.Group className="d-flex mb-4">
            <Form.Label className="mt-2 width-15">
              User Access{" "}
              <FontAwesomeIcon
                icon={faQuestionCircle}
                onClick={() => setShowUserAccessModal(true)}
                data-testid="user-access-info-icon"
              />
              <RoleBasedAccessControl
                isOpen={showUserAccessModal}
                onClose={() => setShowUserAccessModal(false)}
              />
            </Form.Label>
            {get(userInfo, "Users.admin", false) ? (
              <Form.Control
                value={"Ambari Administrator"}
                readOnly
                plaintext
                className="ps-4"
              />
            ) : (
              <Form.Select
                aria-label="Select"
                className="w-25 custom-form-control"
                onChange={(e) =>
                  updateUserDataPrivileges(e.target.value as PermissionLabel)
                }
                value={get(
                  get(userInfo, "privileges", []).filter(
                    (privilige) =>
                      get(privilige, "PrivilegeInfo.type") ===
                      PrivilegeType.CLUSTER
                  ),
                  "[0].PrivilegeInfo.permission_label"
                )}
                data-testid="user-access-dropdown"
              >
                {userAccessOptions.map((option, idx) => (
                  <option value={option} key={idx}>
                    {option}
                  </option>
                ))}
              </Form.Select>
            )}
          </Form.Group>
          <Form.Group className="d-flex mb-4">
            <Form.Label className="mt-2 width-15">Privileges</Form.Label>
            {get(userInfo, "Users.admin", false) ? (
              <Alert className="w-75" variant="info">
                This user is an Ambari Admin and has all privileges.
              </Alert>
            ) : !get(userInfo, "privileges", []).filter(
                (privilige) =>
                  get(privilige, "PrivilegeInfo.type") ===
                    PrivilegeType.CLUSTER &&
                  get(privilige, "PrivilegeInfo.principal_type") ===
                    PrincipalType.USER
              )?.length &&
              !get(userInfo, "privileges", []).filter(
                (privilige) =>
                  get(privilige, "PrivilegeInfo.permission_name") ===
                  PermissionNameType.VIEW_USER
              )?.length ? (
              <Alert className="w-75" variant="info">
                This user does not have any privileges.
              </Alert>
            ) : (
              <div className="w-75 scrollable">
                <Table
                  data={get(userInfo, "privileges", []).filter(
                    (privilige) =>
                      get(privilige, "PrivilegeInfo.type") ===
                        PrivilegeType.CLUSTER &&
                      get(privilige, "PrivilegeInfo.principal_type") ===
                        PrincipalType.USER
                  )}
                  columns={columnsInUserPrivilegesCluster}
                />
                {get(userInfo, "privileges", []).filter(
                  (privilige) =>
                    get(privilige, "PrivilegeInfo.type") ===
                      PrivilegeType.CLUSTER &&
                    get(privilige, "PrivilegeInfo.principal_type") ===
                      PrincipalType.USER
                )?.length ? null : (
                  <div className="ps-2">No cluster privileges</div>
                )}
                <Table
                  data={get(userInfo, "privileges", []).filter(
                    (privilige) =>
                      get(privilige, "PrivilegeInfo.permission_name") ===
                      PermissionNameType.VIEW_USER
                  )}
                  columns={columnsInUserPrivilegesView}
                  className="mt-5"
                />
                {get(userInfo, "privileges", []).filter(
                  (privilige) =>
                    get(privilige, "PrivilegeInfo.permission_name") ===
                    PermissionNameType.VIEW_USER
                )?.length ? null : (
                  <div className="ps-2">No view privileges</div>
                )}
              </div>
            )}
          </Form.Group>
        </Form>
      )}
    </div>
  );
}
