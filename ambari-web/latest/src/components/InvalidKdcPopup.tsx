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

import { Alert, Form, Stack } from "react-bootstrap";
import Modal from "./Modal";
import { useContext, useEffect, useState } from "react";
import Tooltip from "./Tooltip";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faQuestionCircle } from "@fortawesome/free-solid-svg-icons";
import modalManager from "../store/ModalManager";
import credentialsUtils from "../Utils/credentialsUtils";
import { AppContext } from "../store/context";
import { responseErrorMessage } from "../Utils/httpError";

type InvalidKdcPopupProps = {
  getKdcSessionState?: () => void | Promise<void>;
  onCancel?: () => void;
};

function InvalidKdcPopup({
  getKdcSessionState,
  onCancel,
}: InvalidKdcPopupProps) {
  const [principal, setPrincipal] = useState("");
  const [password, setPassword] = useState("");
  const [saveCreds, setSaveCreds] = useState(false);
  const [persistentStoreAvailable, setPersistentStoreAvailable] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [saveError, setSaveError] = useState("");
  const { clusterName } = useContext(AppContext);

  useEffect(() => {
    let cancelled = false;
    const loadStorageCapability = async () => {
      try {
        const isPersistent = await credentialsUtils.isStorePersisted(clusterName);
        if (!cancelled) {
          setPersistentStoreAvailable(isPersistent);
        }
      } catch {
        if (!cancelled) {
          setPersistentStoreAvailable(false);
        }
      }
    };
    if (clusterName) {
      void loadStorageCapability();
    }
    return () => {
      cancelled = true;
    };
  }, [clusterName]);

  if(!clusterName){
    return null;
  }
  const principalError = !principal.trim()
    ? "Admin Principal is required."
    : /\s/.test(principal)
      ? "Admin Principal cannot contain whitespace."
      : "";
  const passwordError = password ? "" : "Admin Password is required.";
  const saveDisabled = isSaving || Boolean(principalError) || Boolean(passwordError);

  const close = () => {
    modalManager.hide();
    onCancel?.();
  };

  return (
    <Modal
      isOpen={true}
      onClose={close}
      successCallback={async () => {
        if (saveDisabled) {
          return;
        }
        setIsSaving(true);
        setSaveError("");
        const resource = credentialsUtils.createCredentialResource(
          principal.trim(),
          password,
          saveCreds
            ? credentialsUtils.STORE_TYPES.PERSISTENT
            : credentialsUtils.STORE_TYPES.TEMPORARY
        );
        try {
          await credentialsUtils.createOrUpdateCredentials(
            clusterName,
            credentialsUtils.ALIAS.KDC_CREDENTIALS,
            resource as any
          );
          modalManager.hide();
          await getKdcSessionState?.();
        } catch (error) {
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
        <>
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
              placeholder=""
              className="mb-2"
              value={principal}
              onChange={(e) => {
                setPrincipal(e.target.value);
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
              placeholder=""
              className="mb-2"
              value={password}
              onChange={(e) => {
                setPassword(e.target.value);
                setSaveError("");
              }}
              disabled={isSaving}
            />
            <div className="d-flex mt-2">
              <Form.Check
                id="save-creds"
                checked={saveCreds}
                disabled={!persistentStoreAvailable || isSaving}
                onChange={(e) => {
                  setSaveCreds(e.target.checked);
                }}
              ></Form.Check>
              <Form.Label className="ms-2 mt-1" htmlFor="save-creds">
                Save Admin Credentials
                <Tooltip
                  message={
                    persistentStoreAvailable
                      ? "Store these credentials in Ambari's persistent credential store"
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
        </>
      }
      options={{
        okButtonText: isSaving ? "SAVING..." : "SAVE",
        okButtonDisabled: saveDisabled,
      }}
    ></Modal>
  );
}
export default InvalidKdcPopup;
