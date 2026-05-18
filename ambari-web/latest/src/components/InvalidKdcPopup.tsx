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
import { useContext, useState } from "react";
import Tooltip from "./Tooltip";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faQuestionCircle } from "@fortawesome/free-solid-svg-icons";
import modalManager from "../store/ModalManager";
import credentialsUtils from "../Utils/credentialsUtils";
import { AppContext } from "../store/context";

function InvalidKdcPopup({getKdcSessionState}:any) {
  const [principal, setPrincipal] = useState("");
  const [password, setPassword] = useState("");
  const [saveCreds, setSaveCreds] = useState(false);
  const { clusterName } = useContext(AppContext);
  if(!clusterName){
    return null;
  }
  return (
    <Modal
      isOpen={true}
      onClose={() => {
        modalManager.hide();
      }}
      successCallback={async () => {
        modalManager.hide();
        const resource = credentialsUtils.createCredentialResource(
          principal,
          password,
          saveCreds
            ? credentialsUtils.STORE_TYPES.PERSISTENT
            : credentialsUtils.STORE_TYPES.TEMPORARY
        );
        await credentialsUtils.createOrUpdateCredentials(
          clusterName,
          credentialsUtils.ALIAS.KDC_CREDENTIALS,
          resource as any
        );
        setTimeout(getKdcSessionState, 1000);
      }}
      modalTitle="Admin session expiration error"
      modalBody={
        <>
          <Stack direction="vertical">
            <Alert variant="warning">
              Missing KDC administrator credentials. Please enter admin
              principal and password.
            </Alert>
            <Form.Label className="mt-2">Admin Principal</Form.Label>

            <Form.Control
              type="text"
              placeholder=""
              className="mb-2"
              value={principal}
              onChange={(e) => setPrincipal(e.target.value)}
            />
            <Form.Label className="mt-2">Admin password</Form.Label>
            <Form.Control
              type="password"
              placeholder=""
              className="mb-2"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            <div className="d-flex mt-2">
              <Form.Check
                id="save-creds"
                checked={saveCreds}
                onChange={(e) => {
                  setSaveCreds(e.target.checked);
                }}
              ></Form.Check>
              <Form.Label className="ms-2 mt-1" htmlFor="save-creds">
                Save Admin Credentials
                <Tooltip message="Ambari is not configured for storing credentials">
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
        okButtonText: "SAVE",
      }}
    ></Modal>
  );
}
export default InvalidKdcPopup;
