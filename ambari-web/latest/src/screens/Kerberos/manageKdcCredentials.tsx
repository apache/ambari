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

import { useContext, useEffect, useState } from "react";
import { Alert, Form } from "react-bootstrap";
import { get } from "lodash";
import ConfirmationModal from "../../components/ConfirmationModal";
import Modal from "../../components/Modal";
import { AppContext } from "../../store/context";
import credentialsUtils from "../../Utils/credentialsUtils";
import { responseErrorMessage } from "../../Utils/httpError";
import { messages } from "../messages";

type ManageKdcCredentialsProps = {
  isOpen: boolean;
  onClose: () => void;
};

export default function ManageKdcCredentials({
  isOpen,
  onClose,
}: ManageKdcCredentialsProps) {
  const [adminPrincipal, setAdminPrincipal] = useState("");
  const [adminPassword, setAdminPassword] = useState("");
  const [isRemovable, setIsRemovable] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isActionInProgress, setIsActionInProgress] = useState(false);
  const [actionError, setActionError] = useState("");
  const [confirmRemove, setConfirmRemove] = useState(false);
  const { clusterName } = useContext(AppContext);

  const principalError = !adminPrincipal.trim()
    ? "Admin Principal is required."
    : /\s/.test(adminPrincipal)
      ? "Admin Principal cannot contain whitespace."
      : "";
  const passwordError = adminPassword ? "" : "Admin Password is required.";
  const submitDisabled =
    isLoading ||
    isActionInProgress ||
    Boolean(principalError) ||
    Boolean(passwordError);

  useEffect(() => {
    if (!isOpen || !clusterName) {
      return;
    }

    let cancelled = false;
    const loadCredentials = async () => {
      setIsLoading(true);
      setActionError("");
      try {
        let credentials: any[] = [];
        await credentialsUtils.credentials(clusterName, (items: any[]) => {
          credentials = items;
        });
        if (!cancelled) {
          setIsRemovable(
            credentials.some(
              (credential) =>
                credential.alias === credentialsUtils.ALIAS.KDC_CREDENTIALS,
            ),
          );
        }
      } catch (error) {
        if (!cancelled) {
          setActionError(
            responseErrorMessage(
              error,
              "Ambari could not load the KDC credential status.",
            ),
          );
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    };

    void loadCredentials();
    return () => {
      cancelled = true;
    };
  }, [clusterName, isOpen]);

  const handleSaveDetails = async () => {
    if (
      isActionInProgress ||
      !adminPrincipal.trim() ||
      /\s/.test(adminPrincipal) ||
      !adminPassword
    ) {
      return;
    }

    setIsActionInProgress(true);
    setActionError("");
    try {
      const resource = credentialsUtils.createCredentialResource(
        adminPrincipal.trim(),
        adminPassword,
        credentialsUtils.STORE_TYPES.PERSISTENT,
      );
      await credentialsUtils.createOrUpdateCredentials(
        clusterName,
        credentialsUtils.ALIAS.KDC_CREDENTIALS,
        resource,
      );
      setIsRemovable(true);
      setAdminPrincipal("");
      setAdminPassword("");
      onClose();
    } catch (error) {
      setActionError(
        responseErrorMessage(error, "Ambari could not save the KDC credentials."),
      );
    } finally {
      setIsActionInProgress(false);
    }
  };

  const removeCredentials = async () => {
    if (isActionInProgress) {
      return;
    }

    setIsActionInProgress(true);
    setActionError("");
    try {
      await credentialsUtils.removeCredentials(
        clusterName,
        credentialsUtils.ALIAS.KDC_CREDENTIALS,
      );
      setIsRemovable(false);
      setAdminPrincipal("");
      setAdminPassword("");
      setConfirmRemove(false);
      onClose();
    } catch (error) {
      setConfirmRemove(false);
      setActionError(
        responseErrorMessage(
          error,
          "Ambari could not remove the KDC credentials.",
        ),
      );
    } finally {
      setIsActionInProgress(false);
    }
  };

  const updatePrincipal = (value: string) => {
    setAdminPrincipal(value);
    setActionError("");
  };

  const updatePassword = (value: string) => {
    setAdminPassword(value);
    setActionError("");
  };

  return (
    <>
      <Modal
        isOpen={isOpen}
        onClose={onClose}
        modalTitle={get(
          messages,
          "admin.kerberos.credentials.store.menu.label",
        )}
        modalBody={
          <>
            <Alert variant="info">
              {isRemovable
                ? get(messages, "admin.kerberos.credentials.form.header.stored")
                : get(
                    messages,
                    "admin.kerberos.credentials.form.header.not.stored",
                  )}
            </Alert>
            {actionError && <Alert variant="danger">{actionError}</Alert>}
            <Form noValidate>
              <Form.Group controlId="adminPrincipal">
                <Form.Label>Admin Principal</Form.Label>
                <Form.Control
                  type="text"
                  value={adminPrincipal}
                  onChange={(event) => updatePrincipal(event.target.value)}
                  isInvalid={Boolean(adminPrincipal) && Boolean(principalError)}
                  disabled={isLoading || isActionInProgress}
                  autoComplete="off"
                />
                {adminPrincipal && principalError && (
                  <Form.Control.Feedback type="invalid">
                    {principalError}
                  </Form.Control.Feedback>
                )}
              </Form.Group>
              <Form.Group controlId="adminPassword" className="mt-3">
                <Form.Label>Admin Password</Form.Label>
                <Form.Control
                  type="password"
                  value={adminPassword}
                  onChange={(event) => updatePassword(event.target.value)}
                  disabled={isLoading || isActionInProgress}
                  autoComplete="new-password"
                />
              </Form.Group>
            </Form>
          </>
        }
        options={{
          okButtonText: isActionInProgress ? "SAVING..." : "SAVE",
          modalSize: "modal-md",
          cancelableViaBtn: false,
          okButtonDisabled: submitDisabled,
          extraButtons: [
            ...(isRemovable
              ? [
                  {
                    text: "REMOVE",
                    onClick: () => setConfirmRemove(true),
                    variant: "danger",
                    order: 1,
                    disabled: isLoading || isActionInProgress,
                  },
                ]
              : []),
            {
              text: "CANCEL",
              onClick: onClose,
              variant: "dark",
              order: 2,
              disabled: isActionInProgress,
            },
          ],
        }}
        successCallback={() => void handleSaveDetails()}
      />
      <ConfirmationModal
        isOpen={confirmRemove}
        onClose={() => setConfirmRemove(false)}
        modalTitle="Remove KDC Credentials"
        modalBody="Are you sure you want to remove the stored KDC administrator credentials?"
        successCallback={() => void removeCredentials()}
        buttonVariant="danger"
        okButtonText={isActionInProgress ? "REMOVING..." : "REMOVE"}
        isOkDisabled={isActionInProgress}
      />
    </>
  );
}
