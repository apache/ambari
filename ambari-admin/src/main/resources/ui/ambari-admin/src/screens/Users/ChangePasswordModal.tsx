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
import { Alert, Button, Form, Modal } from "react-bootstrap";
import DefaultButton from "../../components/DefaultButton";
import { useEffect, useState } from "react";
import { get, set } from "lodash";

type ChangePasswordModalProps = {
  isOpen: boolean;
  onClose: () => void;
  userName: string;
  updatePassword: (yourPassword: string, newUserPassword: string) => void;
};

export default function ChangePasswordModal({
  isOpen,
  onClose,
  userName,
  updatePassword,
}: ChangePasswordModalProps) {
  const [yourPassword, setYourPassword] = useState("");
  const [newUserPassword, setNewUserPassword] = useState("");
  const [newUserPasswordConfirmation, setNewUserPasswordConfirmation] =
    useState("");
  const [validationError, setValidationError] = useState({
    yourPassword: "",
    newUserPassword: "",
  });

  useEffect(() => {
    if (newUserPassword !== newUserPasswordConfirmation) {
      setValidationError({
        ...validationError,
        newUserPassword: "Password must match!",
      });
    } else {
      setValidationError({
        ...validationError,
        newUserPassword: "",
      });
    }
  }, [newUserPassword, newUserPasswordConfirmation]);

  useEffect(() => {
    if (!isOpen) {
      setYourPassword("");
      setNewUserPassword("");
      setNewUserPasswordConfirmation("");
      setValidationError({
        yourPassword: "",
        newUserPassword: "",
      });
    }
  }, [isOpen]);

  const handlePasswordChangeModalSave = async (event: any) => {
    event.preventDefault();
    let error = {
      ...validationError
    };
    if (!yourPassword) {
      set(error, "yourPassword", "Password required!");
    }
    if (!newUserPassword) {
      set(error, "newUserPassword", "Password required!");
    }
    setValidationError(error);
    if (
      yourPassword &&
      newUserPassword &&
      newUserPasswordConfirmation &&
      !get(error, "yourPassword") &&
      !get(error, "newUserPassword")
    ) {
      updatePassword(yourPassword, newUserPassword);
    }
  };

  return (
    <Modal
      show={isOpen}
      onHide={onClose}
      size="lg"
      className="custom-modal-container"
      data-testid="change-password-modal"
    >
      <Modal.Header>
        <Modal.Title><h3>Change Password for {userName}</h3></Modal.Title>
      </Modal.Header>
      <Form onSubmit={handlePasswordChangeModalSave}>
        <Modal.Body>
          <Form.Group className="mb-4 d-flex">
            <div className="w-25 d-flex justify-content-end ms-5 me-4 mt-2">
              <Form.Label
              className={
                get(validationError, "yourPassword", "")
                  ? "text-danger"
                  : ""
              }
              >Your Password</Form.Label>
            </div>
            <div className="w-100">
              <Form.Control
                type="password"
                value={yourPassword}
                placeholder="Your Password"
                className={
                  get(validationError, "yourPassword", "")
                    ? "custom-form-control border-danger"
                    : "custom-form-control"
                }
                onChange={(e) => {
                  setYourPassword(e.target.value);
                  if (e.target.value) {
                    setValidationError({
                      ...validationError,
                      yourPassword: "",
                    });
                  }
                }}
                data-testid="your-password-input"
              />
              {get(validationError, "yourPassword", "") ? (
                <Alert className="mt-2 mb-0 p-2 rounded-0 text-danger" variant="danger">
                  {get(validationError, "yourPassword")}
                </Alert>
              ) : null}
            </div>
          </Form.Group>
          <Form.Group className="mb-3 d-flex">
            <div className="w-25 d-flex justify-content-end ms-5 me-4 mt-2">
              <Form.Label
              className={
                get(validationError, "newUserPassword", "")
                  ? "text-danger"
                  : ""
              }
              >New User Password</Form.Label>
            </div>
            <div className="w-100">
              <Form.Control
                type="password"
                value={newUserPassword}
                placeholder="New User Password"
                className={
                  get(validationError, "newUserPassword", "")
                    ? "custom-form-control mb-2 border-danger"
                    : "custom-form-control mb-2"
                }
                onChange={(e) => setNewUserPassword(e.target.value)}
                data-testid="new-password-input"
              />
              <Form.Control
                type="password"
                value={newUserPasswordConfirmation}
                placeholder="New User Password Confirmation"
                className={
                  get(validationError, "newUserPassword", "")
                    ? "custom-form-control border-danger"
                    : "custom-form-control"
                }
                onChange={(e) => setNewUserPasswordConfirmation(e.target.value)}
                data-testid="new-confirm-password-input"
              />
              {get(validationError, "newUserPassword", "") ? (
                <Alert className="mt-2 mb-0 p-2 rounded-0 text-danger" variant="danger">
                  {get(validationError, "newUserPassword")}
                </Alert>
              ) : null}
            </div>
          </Form.Group>
        </Modal.Body>
        <Modal.Footer className="d-flex justify-content-end">
          <DefaultButton onClick={onClose}>CANCEL</DefaultButton>
          <Button type="submit" className="custom-btn" variant="success" data-testid="save-password-btn">
            OK
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}
