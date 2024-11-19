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
import { Button, Form, Modal } from "react-bootstrap";
import { faCircleXmark, faQuestionCircle } from "@fortawesome/free-solid-svg-icons";
import DefaultButton from "../../components/DefaultButton";
import { useContext, useEffect, useState } from "react";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import RoleBasedAccessControl from "./RoleBasedAccessControl";
import Select from "react-select";
import { userAccessOptions, permissionLabelToName } from "./constants";
import { PermissionLabel, SelectOptionType, UserNamesType } from "./types";
import {
  GroupDataType,
  MembersDataType,
  PrivilegesDataType,
} from "../../api/types";
import WarningModal from "./WarningModal";
import toast from "react-hot-toast";
import { get } from "lodash";
import { DefaultAccess, PrincipalType } from "./enums";
import UserGroupApi from "../../api/userGroupApi";
import PrivilegeApi from "../../api/privilegeApi";
import AppContent from "../../context/AppContext";
import Spinner from "../../components/Spinner";
import { Link } from "react-router-dom";

type AddGroupProps = {
  showAddGroupModal: boolean;
  setShowAddGroupModal: (showAddUserModal: boolean) => void;
  successCallback: () => void;
};

enum GroupNameCriteria {
  REGEX = "^([a-zA-Z0-9._\\s]*)$",
  MAX_LENGTH = 80,
}

export default function AddGroup({
  showAddGroupModal,
  setShowAddGroupModal,
  successCallback,
}: AddGroupProps) {
  const [showRoleAccessModal, setShowRoleAccessModal] = useState(false);
  const [groupName, setGroupName] = useState("");
  const [showAddGroupCancelWarning, setShowAddGroupCancelWarning] =
    useState(false);
  const [groupAccess, setGroupAccess] = useState<PermissionLabel>("None");
  const [loading, setLoading] = useState(false);
  const [userNames, setUserNames] = useState<UserNamesType | null>(null);
  const [groupMembers, setGroupMembers] = useState<string[]>([]);
  const [validationError, setValidationError] = useState("");
  const {
    cluster: { cluster_name: clusterName },
  } = useContext(AppContent);

  useEffect(() => {
    async function getUserNames() {
      setLoading(true);
      const data: any = await UserGroupApi.userNames();
      setUserNames(data);
      setLoading(false);
    }
    getUserNames();
  }, []);

  useEffect(() => {
    if (!showAddGroupModal) {
      resetValues();
      setShowAddGroupModal(false);
    }
  }, [showAddGroupModal]);

  const userOptions: SelectOptionType[] = get(userNames, "items", []).map(
    (user: any) => ({
      value: get(user, "Users.user_name"),
      label: get(user, "Users.user_name"),
    })
  );

  const validateGroupName = (groupNameValue: string) => {
    const regex = new RegExp(GroupNameCriteria.REGEX);
    if (!regex.test(groupNameValue)) {
      setValidationError("Must not contain special characters!");
    } else if (groupNameValue.length > GroupNameCriteria.MAX_LENGTH) {
      setValidationError("Must not be longer than 80 characters!");
    } else {
      setValidationError("");
    }
  };

  const handleSave = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!groupName) {
      setValidationError("Field required!");
    }
    if (groupName && !validationError) {
      const groupData: GroupDataType[] = [
        {
          "Groups/group_name": groupName,
        },
      ];
      await UserGroupApi.addGroup(groupData);
      toast.success(
        <div className="toast-message">
          Created group{" "}
          <Link to={`/groups/${groupName}/edit`} className="custom-link">
            {groupName}
          </Link>
        </div>
      );
      if (groupMembers.length) {
        const membersData: MembersDataType[] = groupMembers.map((member) => ({
          "MemberInfo/group_name": groupName,
          "MemberInfo/user_name": member,
        }));
        await UserGroupApi.updateMembers(groupName, membersData);
      }
      if (groupAccess !== DefaultAccess.NONE) {
        const privilegesData: PrivilegesDataType[] = [
          {
            PrivilegeInfo: {
              permission_name: permissionLabelToName[groupAccess],
              principal_name: groupName,
              principal_type: PrincipalType.GROUP,
            },
          },
        ];
        await PrivilegeApi.addClusterPrivileges(clusterName, privilegesData);
      }
      resetValues();
      setShowAddGroupModal(false);
      successCallback();
    }
  };

  const resetValues = () => {
    setGroupName("");
    setGroupMembers([]);
    setGroupAccess("None");
  };

  const handleCancel = () => {
    if (groupName || groupMembers.length || groupAccess !== "None") {
      setShowAddGroupCancelWarning(true);
    } else {
      setShowAddGroupModal(false);
    }
  };

  const handleWarningSave = (event: any) => {
    setShowAddGroupCancelWarning(false);
    handleSave(event);
  };

  const handleWarningDiscard = () => {
    setShowAddGroupCancelWarning(false);
    setShowAddGroupModal(false);
  };

  return (
    <Modal
      show={showAddGroupModal}
      onHide={handleCancel}
      size="lg"
      className="custom-modal-container"
      data-testid="add-group-modal"
    >
      <Modal.Header closeButton>
        <Modal.Title className="ms-2">Add Groups</Modal.Title>
      </Modal.Header>
      {loading ? (
        <Spinner />
      ) : (
        <Form onSubmit={handleSave}>
          <Modal.Body>
            <Form.Group className="mb-4">
              <Form.Label>Group name *</Form.Label>
              <Form.Control
                type="text"
                value={groupName}
                placeholder="Group name"
                className={
                  validationError
                    ? "custom-form-control border-danger"
                    : "custom-form-control"
                }
                onChange={(e) => {
                  setGroupName(e.target.value);
                  validateGroupName(e.target.value);
                }}
                data-testid="group-name-input"
              />
              {validationError ? (
                <div className="text-danger mt-1">
                  <FontAwesomeIcon icon={faCircleXmark} /> {validationError}
                </div>
              ) : null}
            </Form.Group>
            <Form.Group className="mb-4">
              <Form.Label>Add users to this group</Form.Label>
              <Select<SelectOptionType, true>
                isMulti
                name="users"
                options={userOptions}
                className="basic-multi-select"
                placeholder="Add User"
                value={groupMembers.map((user) => ({
                  value: user,
                  label: user,
                }))}
                onChange={(e) => setGroupMembers(e.map((user) => user.value))}
                aria-label="select-user"
              ></Select>
            </Form.Group>
            <Form.Group className="mb-4">
              <Form.Label>
                Group Access *{" "}
                <FontAwesomeIcon
                  icon={faQuestionCircle}
                  onClick={() => setShowRoleAccessModal(true)}
                  data-testid="group-access-help-icon"
                />
                <RoleBasedAccessControl
                  isOpen={showRoleAccessModal}
                  onClose={() => setShowRoleAccessModal(false)}
                />
              </Form.Label>
              <Form.Select
                aria-label="Select"
                className="w-50 custom-form-control"
                onChange={(e) =>
                  setGroupAccess(e.target.value as PermissionLabel)
                }
                data-testid="group-access-dropdown"
              >
                {userAccessOptions.map((accessOption, idx) => {
                  return (
                    <option key={idx} value={accessOption}>
                      {accessOption}
                    </option>
                  );
                })}
              </Form.Select>
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <DefaultButton onClick={handleCancel} data-testid="add-group-cancel-btn">Cancel</DefaultButton>
            <WarningModal
              isOpen={showAddGroupCancelWarning}
              onClose={() => setShowAddGroupCancelWarning(false)}
              handleWarningDiscard={handleWarningDiscard}
              handleWarningSave={handleWarningSave}
            />
            <Button type="submit" variant="success" data-testid="add-group-save-btn">
              SAVE
            </Button>
          </Modal.Footer>
        </Form>
      )}
    </Modal>
  );
}
