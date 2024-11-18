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
import { useEffect } from "react";
import { Button, Form, Modal } from "react-bootstrap";
import { faCircleXmark, faQuestionCircle } from "@fortawesome/free-solid-svg-icons";
import DefaultButton from "../../components/DefaultButton";
import { useRef, useState } from "react";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import ErrorOverlay from "../../components/ErrorOverlay";
import RoleBasedAccessControl from "./RoleBasedAccessControl";
import { permissionLabelToName, userAccessOptions } from "./constants";
import WarningModal from "./WarningModal";
import { PrivilegesDataType, UserDataType } from "../../api/types";
import UserGroupApi from "../../api/userGroupApi";
import toast from "react-hot-toast";
import { DefaultAccess, PrincipalType } from "./enums";
import { PermissionLabel } from "./types";
import PrivilegeApi from "../../api/privilegeApi";
import { Link } from "react-router-dom";
import { get, set } from "lodash";

type AddUserProps = {
  clusterName: string;
  showAddUserModal: boolean;
  setShowAddUserModal: (showAddUserModal: boolean) => void;
  successCallback: () => void;
};

enum UserNameCriteria {
  REGEX = "^[^\\\\&|<>`]*$",
  MAX_LENGTH = 80,
}

export default function AddUser({
  clusterName,
  showAddUserModal,
  setShowAddUserModal,
  successCallback,
}: AddUserProps) {
  const [showUsernameTooltip, setShowUsernameTooltip] = useState(false);
  const [showRoleAccessModal, setShowRoleAccessModal] = useState(false);
  const [showUserAmbariAdminTooltip, setShowUserAmbariAdminTooltip] =
    useState(false);
  const [showUserStatusTooltip, setShowUserStatusTooltip] = useState(false);

  const [userName, setUserName] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [userAccess, setUserAccess] = useState<PermissionLabel>("None");
  const [isUserAmbariAdmin, setIsUserAmbariAdmin] = useState(false);
  const [isUserActive, setIsUserActive] = useState(true);
  const [showAddUserCancelWarning, setShowAddUserCancelWarning] =
    useState(false);
  const [validationError, setValidationError] = useState({
    userName: "Field required!",
    password: "Field required!",
    confirmPassword: "Field required!",
  });
  const [isFormSubmitted, setIsFormSubmitted] = useState(false);

  const userNameTarget = useRef(null);
  const userAccessTarget = useRef(null);
  const userAmbariAdminTarget = useRef(null);
  const userStatusTarget = useRef(null);

  useEffect(() => {
    if (!showAddUserModal) {
      resetValues();
      setShowUsernameTooltip(false);
      setShowRoleAccessModal(false);
      setShowUserAmbariAdminTooltip(false);
      setShowUserStatusTooltip(false);
      setShowAddUserModal(false);
      setValidationError({
        userName: "Field required!",
        password: "Field required!",
        confirmPassword: "Field required!",
      });
      setIsFormSubmitted(false);
    }
  }, [showAddUserModal]);

  useEffect(() => {
    let error = {
      ...validationError,
    };
    if (password) {
      set(error, "password", "");
    } else {
      set(error, "password", "Field required!");
    }
    if (!confirmPassword) {
      set(error, "confirmPassword", "Field required!");
    } else if (password !== confirmPassword) {
      set(error, "confirmPassword", "Password must match!");
    } else {
      set(error, "confirmPassword", "");
    }
    setValidationError(error);
  }, [password, confirmPassword]);

  const validateUserName = (userNameValue: string) => {
    const regex = new RegExp(UserNameCriteria.REGEX);
    if (!userNameValue) {
      setValidationError({
        ...validationError,
        userName: "Field required!",
      });
    } else if (!regex.test(userNameValue)) {
      setValidationError({
        ...validationError,
        userName: "Must not contain special characters!",
      });
    } else if (userNameValue.length > UserNameCriteria.MAX_LENGTH) {
      setValidationError({
        ...validationError,
        userName: "Must not be longer than 80 characters!",
      });
    } else {
      setValidationError({
        ...validationError,
        userName: "",
      });
    }
  };

  const handleSave = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (
      userName &&
      password &&
      confirmPassword &&
      !get(validationError, "userName") &&
      !get(validationError, "password") &&
      !get(validationError, "confirmPassword")
    ) {
      const userData: UserDataType = {
        "Users/active": isUserActive,
        "Users/admin": isUserAmbariAdmin,
        "Users/password": password,
        "Users/user_name": userName,
      };
      await UserGroupApi.addUser(userData);
      toast.success(
        <div className="toast-message">
          Created user{" "}
          <Link to={`/users/${userName}/edit`} className="custom-link">
            {userName}
          </Link>
        </div>
      );
      if (userAccess !== DefaultAccess.NONE) {
        const privilegesData: PrivilegesDataType[] = [
          {
            PrivilegeInfo: {
              permission_name: permissionLabelToName[userAccess],
              principal_name: userName,
              principal_type: PrincipalType.USER,
            },
          },
        ];
        await PrivilegeApi.addClusterPrivileges(clusterName, privilegesData);
      }
      resetValues();
      setShowAddUserModal(false);
      successCallback();
    }
  };

  const resetValues = () => {
    setUserName("");
    setPassword("");
    setConfirmPassword("");
    setUserAccess("None");
    setIsUserAmbariAdmin(false);
    setIsUserActive(true);
  };

  const handleCancel = () => {
    if (
      userName ||
      password ||
      confirmPassword ||
      userAccess !== "None" ||
      isUserAmbariAdmin ||
      !isUserActive
    ) {
      setShowAddUserCancelWarning(true);
    } else {
      setShowAddUserModal(false);
    }
  };

  const handleWarningSave = (event: any) => {
    setShowAddUserCancelWarning(false);
    handleSave(event);
  };

  const handleWarningDiscard = () => {
    setShowAddUserCancelWarning(false);
    setShowAddUserModal(false);
  };

  return (
    <Modal
      show={showAddUserModal}
      onHide={handleCancel}
      size="lg"
      className="custom-modal-container"
      data-testid="add-user-modal"
    >
      <Modal.Header closeButton>
        <Modal.Title className="ms-2">Add Users</Modal.Title>
      </Modal.Header>
      <Form onSubmit={handleSave}>
        <Modal.Body>
          <Form.Group className="mb-4">
            <Form.Label>
              Username *{" "}
              <FontAwesomeIcon
                icon={faQuestionCircle}
                onClick={() => setShowUsernameTooltip(!showUsernameTooltip)}
                data-testid="username-tooltip-icon"
              />
            </Form.Label>
            <Form.Control
              type="text"
              value={userName}
              ref={userNameTarget}
              placeholder="User name"
              className={
                get(validationError, "userName", "") && isFormSubmitted
                  ? "custom-form-control border-danger"
                  : "custom-form-control"
              }
              onChange={(e) => {
                setUserName(e.target.value);
                validateUserName(e.target.value);
              }}
              data-testid="username-input"
            />
            {get(validationError, "userName", "") && isFormSubmitted ? (
              <div className="text-danger mt-1">
                <FontAwesomeIcon icon={faCircleXmark} />{" "}
                {get(validationError, "userName")}
              </div>
            ) : null}
            <ErrorOverlay
              target={userNameTarget}
              showTooltip={showUsernameTooltip}
              errorMessage="Maximum length is 80 characters. \, &amp;, |, &lt;, &gt;, `
                  are not allowed."
            />
          </Form.Group>
          <Form.Group className="mb-4 d-flex ">
            <Form.Group className="me-auto">
              <Form.Label>Password *</Form.Label>
              <Form.Control
                type="password"
                value={password}
                htmlSize={40}
                placeholder="Password"
                onChange={(e) => setPassword(e.target.value)}
                className={
                  get(validationError, "password", "") && isFormSubmitted
                    ? "custom-form-control border-danger"
                    : "custom-form-control"
                }
                data-testid="password-input"
              />
              {get(validationError, "password", "") && isFormSubmitted ? (
                <div className="text-danger mt-1">
                  <FontAwesomeIcon icon={faCircleXmark} />{" "}
                  {get(validationError, "password")}
                </div>
              ) : null}
            </Form.Group>
            <Form.Group>
              <Form.Label>Confirm Password *</Form.Label>
              <Form.Control
                type="password"
                value={confirmPassword}
                htmlSize={40}
                placeholder="Confirm Password"
                onChange={(e) => setConfirmPassword(e.target.value)}
                className={
                  get(validationError, "confirmPassword", "") && isFormSubmitted
                    ? "custom-form-control border-danger"
                    : "custom-form-control"
                }
                data-testid="confirm-password-input"
              />
              {get(validationError, "confirmPassword", "") &&
              isFormSubmitted ? (
                <div className="text-danger mt-1">
                  <FontAwesomeIcon icon={faCircleXmark} />{" "}
                  {get(validationError, "confirmPassword")}
                </div>
              ) : null}
            </Form.Group>
          </Form.Group>
          <Form.Group className="mb-4">
            <Form.Label>
              User Access *{" "}
              <FontAwesomeIcon
                icon={faQuestionCircle}
                onClick={() => setShowRoleAccessModal(true)}
                ref={userAccessTarget}
                data-testid="user-access-help-icon"
              />
              <RoleBasedAccessControl
                isOpen={showRoleAccessModal}
                onClose={() => setShowRoleAccessModal(false)}
              />
            </Form.Label>
            <Form.Select
              aria-label="Select"
              className="w-50 custom-form-control"
              onChange={(e) => setUserAccess(e.target.value as PermissionLabel)}
              data-testid="user-access-dropdown"
            >
              {userAccessOptions.map((accessOption, idx) => (
                <option value={accessOption} key={idx}>
                  {accessOption}
                </option>
              ))}
            </Form.Select>
          </Form.Group>
          <Form.Group className="mb-4">
            <Form.Label>
              Is this user an Ambari Admin? *{"  "}
              <FontAwesomeIcon
                icon={faQuestionCircle}
                onClick={() =>
                  setShowUserAmbariAdminTooltip(!showUserAmbariAdminTooltip)
                }
                ref={userAmbariAdminTarget}
                data-testid="ambari-admin-tooltip-icon"
              />
              <ErrorOverlay
                target={userAmbariAdminTarget}
                showTooltip={showUserAmbariAdminTooltip}
                errorMessage="An Ambari Admin can create new clusters and other Ambari Admin Users."
              />
            </Form.Label>
            <div className="d-flex">
              <Form.Check
                type="switch"
                checked={isUserAmbariAdmin}
                onChange={() => setIsUserAmbariAdmin(!isUserAmbariAdmin)}
                className="custom-form-check"
                data-testid="ambari-admin-switch"
              />
              {isUserAmbariAdmin ? (
                <span className="m-2 ps-2">Yes</span>
              ) : (
                <span className="m-2 ps-2">No</span>
              )}
            </div>
          </Form.Group>
          <Form.Group className="mb-4">
            <Form.Label>
              User Status *{"  "}
              <FontAwesomeIcon
                icon={faQuestionCircle}
                onClick={() => setShowUserStatusTooltip(!showUserStatusTooltip)}
                ref={userStatusTarget}
                data-testid="user-status-tooltip-icon"
              />
              <ErrorOverlay
                target={userStatusTarget}
                showTooltip={showUserStatusTooltip}
                errorMessage="Active Users can log in to Ambari. Inactive Users cannot."
              />
            </Form.Label>
            <div className="d-flex">
              <Form.Check
                type="switch"
                checked={isUserActive}
                onChange={() => setIsUserActive(!isUserActive)}
                className="custom-form-check"
                data-testid="user-status-switch"
              />
              {isUserActive ? (
                <span className="m-2 ps-2">Active</span>
              ) : (
                <span className="m-2 ps-2">Inactive</span>
              )}
            </div>
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <DefaultButton onClick={handleCancel} data-testid="cancel-btn">Cancel</DefaultButton>
          <WarningModal
            isOpen={showAddUserCancelWarning}
            onClose={() => setShowAddUserCancelWarning(false)}
            handleWarningDiscard={handleWarningDiscard}
            handleWarningSave={handleWarningSave}
          />
          <Button
            className="custom-btn"
            type="submit"
            variant="success"
            data-testid="save-btn"
            onClick={() => setIsFormSubmitted(true)}
          >
            Save
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}
