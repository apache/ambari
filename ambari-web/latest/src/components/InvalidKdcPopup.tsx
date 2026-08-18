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

import { faQuestionCircle } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { useContext, useEffect, useState } from "react";
import { Alert, Form, Stack } from "react-bootstrap";
import { AppContext } from "../store/context";
import modalManager from "../store/ModalManager";
import credentialsUtils from "../Utils/credentialsUtils";
import { responseErrorMessage } from "../Utils/httpError";
import Modal from "./Modal";
import Tooltip from "./Tooltip";

type InvalidKdcPopupProps = {
  getKdcSessionState?: () => void | Promise<void>;
  onCancel?: (error: Error) => void;
  onError?: (error: unknown) => void;
};

function InvalidKdcPopup({
  getKdcSessionState,
  onCancel,
  onError,
}: InvalidKdcPopupProps) {
  const { clusterName, cluster } = useContext(AppContext);
  const configuredPersistentStore =
    cluster?.Clusters?.credential_store_properties?.["storage.persistent"] ===
    "true";
  const [principal, setPrincipal] = useState("");
  const [password, setPassword] = useState("");
  const [saveCreds, setSaveCreds] = useState(false);
  const [persistentStoreAvailable, setPersistentStoreAvailable] = useState(
    configuredPersistentStore,
  );
  const [isSaving, setIsSaving] = useState(false);
  const [saveError, setSaveError] = useState("");

  useEffect(() => {
    let active = true;
    if (!clusterName || configuredPersistentStore) {
      setPersistentStoreAvailable(configuredPersistentStore);
      return () => {
        active = false;
      };
    }

    void credentialsUtils.isStorePersisted(clusterName)
      .then((isPersistent) => {
        if (active) setPersistentStoreAvailable(isPersistent);
      })
      .catch(() => {
        if (active) setPersistentStoreAvailable(false);
      });
    return () => {
      active = false;
    };
  }, [clusterName, configuredPersistentStore]);

  if (!clusterName) return null;

  const principalError = !principal.trim()
    ? "Admin Principal is required."
    : /\s/.test(principal)
      ? "Admin Principal cannot contain whitespace."
      : "";
  const passwordError = password ? "" : "Admin Password is required.";
  const saveDisabled =
    isSaving || Boolean(principalError) || Boolean(passwordError);

  const close = () => {
    modalManager.hide();
    onCancel?.(new Error("KDC credential entry was cancelled."));
  };

  return (
    <Modal
      isOpen
      onClose={close}
      successCallback={async () => {
        if (saveDisabled) return;
        setIsSaving(true);
        setSaveError("");
        const resource = credentialsUtils.createCredentialResource(
          principal.trim(),
          password,
          saveCreds
            ? credentialsUtils.STORE_TYPES.PERSISTENT
            : credentialsUtils.STORE_TYPES.TEMPORARY,
        );
        try {
          await credentialsUtils.createOrUpdateCredentials(
            clusterName,
            credentialsUtils.ALIAS.KDC_CREDENTIALS,
            resource,
          );
          modalManager.hide();
          await getKdcSessionState?.();
        } catch (error) {
          if (onError) {
            modalManager.hide();
            onError(error);
            return;
          }
          setSaveError(
            responseErrorMessage(
              error,
              "Ambari could not save the KDC administrator credentials.",
            ),
          );
        } finally {
          setIsSaving(false);
        }
      }}
      modalTitle="Admin session expiration error"
      modalBody={
        <Stack direction="vertical">
          <Alert variant="warning">
            Missing KDC administrator credentials. Please enter admin
            principal and password.
          </Alert>
          {saveError && <Alert variant="danger">{saveError}</Alert>}
          <Form.Label className="mt-2" htmlFor="invalid-kdc-principal">
            Admin Principal
          </Form.Label>
          <Form.Control
            id="invalid-kdc-principal"
            type="text"
            className="mb-2"
            value={principal}
            onChange={(event) => {
              setPrincipal(event.target.value);
              setSaveError("");
            }}
            disabled={isSaving}
            isInvalid={Boolean(principal) && Boolean(principalError)}
          />
          {principal && principalError && (
            <Form.Control.Feedback type="invalid">
              {principalError}
            </Form.Control.Feedback>
          )}
          <Form.Label className="mt-2" htmlFor="invalid-kdc-password">
            Admin password
          </Form.Label>
          <Form.Control
            id="invalid-kdc-password"
            type="password"
            className="mb-2"
            value={password}
            onChange={(event) => {
              setPassword(event.target.value);
              setSaveError("");
            }}
            disabled={isSaving}
          />
          <div className="d-flex mt-2">
            <Form.Check
              id="save-creds"
              checked={saveCreds}
              disabled={!persistentStoreAvailable || isSaving}
              onChange={(event) => setSaveCreds(event.target.checked)}
            />
            <Form.Label className="ms-2 mt-1" htmlFor="save-creds">
              Save Admin Credentials
              <Tooltip
                message={
                  persistentStoreAvailable
                    ? "Store the KDC credential in Ambari's persistent credential store"
                    : "Ambari is not configured for storing credentials"
                }
              >
                <FontAwesomeIcon
                  className="ms-1 custom-link cursor-pointer"
                  icon={faQuestionCircle}
                />
              </Tooltip>
            </Form.Label>
          </div>
        </Stack>
      }
      options={{
        okButtonText: isSaving ? "SAVING..." : "SAVE",
        okButtonDisabled: saveDisabled,
      }}
    />
  );
}

export default InvalidKdcPopup;
