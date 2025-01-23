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
import { Alert, Button, Form } from "react-bootstrap";
import { useParams } from "react-router";
import { Link, useHistory, Prompt } from "react-router-dom";
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
import { startCase } from "lodash";
import { permissionLabelToName, userAccessOptions } from "./constants";
import { get } from "lodash";
import {
  GroupInfoType,
  PermissionLabel,
  SelectOptionType,
  UserNamesType,
} from "./types";
import { DefaultAccess, PrincipalType, PrivilegeType } from "./enums";
import { MembersDataType, PrivilegesDataType } from "../../api/types";
import toast from "react-hot-toast";
import DefaultButton from "../../components/DefaultButton";
import UserGroupApi from "../../api/userGroupApi";
import PrivilegeApi from "../../api/privilegeApi";
import AppContent from "../../context/AppContext";
import Spinner from "../../components/Spinner";
import Table from "../../components/Table";
import WarningModal from "./WarningModal";
import { constructLinkToEditInstance } from "../../screens/Users/EditUser"

type ParamsType = {
  groupName: string;
};

export default function EditGroup() {
  const params: ParamsType = useParams();

  const [showGroupAccessModal, setShowGroupAccessModal] = useState(false);

  const [loading, setLoading] = useState(false);
  const [groupInfo, setGroupInfo] = useState<GroupInfoType>(
    {} as GroupInfoType
  );
  const [userNames, setUserNames] = useState<UserNamesType>(
    {} as UserNamesType
  );
  const [groupMembers, setGroupMembers] = useState<SelectOptionType[]>([]);
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
    setSelectedOption("Groups");
    async function getUserNames() {
      setLoading(true);
      const data: UserNamesType = await UserGroupApi.userNames();
      setUserNames(data);
    }
    getUserNames();
    getGroupData();
  }, []);

  useEffect(() => {
    previousMembers.current = get(groupInfo, "members", []).map((member) =>
      get(member, "MemberInfo.user_name")
    );
  }, [groupInfo]);

  useEffect(() => {
    newMembers.current = groupMembers.map((member) => member.value);
    setHasUnsavedChanges(
      JSON.stringify(previousMembers) !== JSON.stringify(newMembers)
    );
  }, [groupMembers]);

  const userOptions = get(userNames, "items", []).map((user: any) => ({
    value: get(user, "Users.user_name"),
    label: get(user, "Users.user_name"),
  }));

  const columnsInGroupPrivilegesCluster = [
    {
      header: "Cluster",
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
      cell: (info: any) => {
        return (
          <div>{get(info, "row.original.PrivilegeInfo.permission_label")}</div>
        );
      },
    },
  ];

  const columnsInGroupPrivilegesView = [
    {
      header: "View",
      cell: (info: any) => {
        return (
          <div className="w-50">
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
              className="btn-wrapping-icon"
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

  const getGroupData = async () => {
    setLoading(true);
    const data: GroupInfoType = await UserGroupApi.groupData(
      params.groupName,
      "Groups,privileges/PrivilegeInfo/*,members/MemberInfo"
    );
    setGroupInfo(data);
    setGroupMembers(
      get(data, "members", []).map((member) => ({
        value: get(member, "MemberInfo.user_name"),
        label: get(member, "MemberInfo.user_name"),
      }))
    );
    setLoading(false);
  };

  const updateGroupDataMembers = async () => {
    const membersData: MembersDataType[] = groupMembers.map((member) => ({
      "MemberInfo/group_name": get(groupInfo, "Groups.group_name", ""),
      "MemberInfo/user_name": member.value,
    }));
    await UserGroupApi.updateMembers(
      get(groupInfo, "Groups.group_name", ""),
      membersData
    );
    toast.success(
      <div className="toast-message">
        Local Members updated for {get(groupInfo, "Groups.group_name", "")}
      </div>
    );
    getGroupData();
  };

  const updateGroupDataPrivileges = async (value: PermissionLabel) => {
    if (value === DefaultAccess.NONE) {
      const currentClusterPrivileges = get(groupInfo, "privileges", []).filter(
        (privilige) =>
          get(privilige, "PrivilegeInfo.type") === PrivilegeType.CLUSTER
      );
      const removePrivilegesPromises = currentClusterPrivileges.map(
        (privilege: any) =>
          PrivilegeApi.removeClusterPrivileges(
            clusterName,
            get(privilege, "PrivilegeInfo.privilege_id")
          )
      );
      await Promise.all(removePrivilegesPromises);
      toast.success(
        <div className="toast-message">
          {get(groupInfo, "Groups.group_name", "")}'s explicit privilege has
          been changed to 'NONE'. Any privilege now seen for this user comes
          through its Group(s).
        </div>
      );
    } else {
      const privilegesData: PrivilegesDataType[] = [
        {
          PrivilegeInfo: {
            permission_name: permissionLabelToName[value],
            principal_name: get(groupInfo, "Groups.group_name", ""),
            principal_type: PrincipalType.GROUP,
          },
        },
      ];
      await PrivilegeApi.addClusterPrivileges(clusterName, privilegesData);
      toast.success(
        <div className="toast-message">
          {get(groupInfo, "Groups.group_name", "")} changed to {value}
        </div>
      );
    }
    getGroupData();
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
        {get(groupInfo, "Groups.group_name", "")}
      </div>
    );
    getGroupData();
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
    updateGroupDataMembers();
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
        <Link to={"/userManagement?tab=groups"} className="custom-link" data-testid="groups-list-link">
          <h4>Groups</h4>
        </Link>
        <h4 className="ms-2 make-all-grey">{`/ ${get(groupInfo, "Groups.group_name", "")}`}</h4>
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
                get(groupInfo, "Groups.group_type", "").toLowerCase()
              )}
              readOnly
              plaintext
              className="ps-4"
            />
          </Form.Group>
          <Form.Group className="d-flex mb-4">
            <Form.Label className="mt-2 width-15">Local Members</Form.Label>
            <Select
              isMulti
              name="groups"
              options={userOptions}
              className="basic-multi-select w-75"
              placeholder="Add Group"
              value={groupMembers}
              onChange={(e) => setGroupMembers(e as SelectOptionType[])}
              isDisabled={
                get(groupInfo, "Groups.group_type", "").toLowerCase() !==
                "local"
              }
              aria-label="select-group"
            ></Select>
            <DefaultButton
              onClick={() => {
                if (
                  get(groupInfo, "Groups.group_type", "").toLowerCase() ===
                  "local" && JSON.stringify(previousMembers) !== JSON.stringify(newMembers)
                ) {
                  updateGroupDataMembers();
                }
              }}
              className={
                get(groupInfo, "Groups.group_type", "").toLowerCase() !==
                "local" || JSON.stringify(previousMembers) === JSON.stringify(newMembers)
                  ? "cursor-not-allowed opacity-50 ms-2"
                  : "ms-2"
              }
              data-testid="save-user-btn"
            >
              <FontAwesomeIcon icon={faCheck} />
            </DefaultButton>
          </Form.Group>
          <Form.Group className="d-flex mb-4">
            <Form.Label className="mt-2 width-15">
              Group Access{" "}
              <FontAwesomeIcon
                icon={faQuestionCircle}
                onClick={() => setShowGroupAccessModal(true)}
                data-testid="group-access-info-icon"
              />
              <RoleBasedAccessControl
                isOpen={showGroupAccessModal}
                onClose={() => setShowGroupAccessModal(false)}
              />
            </Form.Label>
            <Form.Select
              aria-label="Select"
              className="w-25 custom-form-control"
              onChange={(e) =>
                updateGroupDataPrivileges(e.target.value as PermissionLabel)
              }
              value={get(
                get(groupInfo, "privileges", []).filter(
                  (privilige) =>
                    get(privilige, "PrivilegeInfo.type") ===
                    PrivilegeType.CLUSTER
                ),
                "[0].PrivilegeInfo.permission_label"
              )}
              data-testid="group-access-dropdown"
            >
              {userAccessOptions.map((accessOption, idx) => (
                <option value={accessOption} key={idx}>
                  {accessOption}
                </option>
              ))}
            </Form.Select>
          </Form.Group>
          <Form.Group className="d-flex mb-4">
            <Form.Label className="mt-2 width-15">Privileges</Form.Label>
            {!get(groupInfo, "privileges", []).filter(
              (privilige) =>
                get(privilige, "PrivilegeInfo.type") === PrivilegeType.CLUSTER
            )?.length &&
            !get(groupInfo, "privileges", []).filter(
              (privilige) =>
                get(privilige, "PrivilegeInfo.type") === PrivilegeType.VIEW &&
                get(privilige, "PrivilegeInfo.principal_type") ===
                  PrincipalType.GROUP
            )?.length ? (
              <Alert className="w-75" variant="info">
                This group does not have any privileges.
              </Alert>
            ) : (
              <div className="w-75 scrollable">
                <Table
                  data={get(groupInfo, "privileges", []).filter(
                    (privilige) =>
                      get(privilige, "PrivilegeInfo.type") ===
                      PrivilegeType.CLUSTER
                  )}
                  columns={columnsInGroupPrivilegesCluster}
                />
                {get(groupInfo, "privileges", []).filter(
                  (privilige) =>
                    get(privilige, "PrivilegeInfo.type") ===
                    PrivilegeType.CLUSTER
                )?.length ? null : (
                  <div className="ps-2">No cluster privileges</div>
                )}
                <Table
                  data={get(groupInfo, "privileges", []).filter(
                    (privilige) =>
                      get(privilige, "PrivilegeInfo.type") ===
                        PrivilegeType.VIEW &&
                      get(privilige, "PrivilegeInfo.principal_type") ===
                        PrincipalType.GROUP
                  )}
                  columns={columnsInGroupPrivilegesView}
                  className="mt-5"
                />
                {get(groupInfo, "privileges", []).filter(
                  (privilige) =>
                    get(privilige, "PrivilegeInfo.type") ===
                      PrivilegeType.VIEW &&
                    get(privilige, "PrivilegeInfo.principal_type") ===
                      PrincipalType.GROUP
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
