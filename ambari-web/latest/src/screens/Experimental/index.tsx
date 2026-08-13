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

import { useContext, useState } from "react";
import { Alert, Button, Form, Table } from "react-bootstrap";
import { useNavigate } from "react-router-dom";
import ClusterApi from "../../api/clusterApi";
import Modal from "../../components/Modal";
import { useAuth } from "../../hooks/useAuth";
import { db } from "../../Utils/db";
import { persistedPayload } from "../../Utils/persistedSettings";
import { AppContext } from "../../store/context";
import { responseErrorMessage } from "../../Utils/httpError";

export default function Experimental() {
  const { user, hasAuthorization } = useAuth();
  const {
    isNonWizardUser,
    setSupports: setSharedSupports,
    supports: sharedSupports,
  } = useContext(AppContext);
  const navigate = useNavigate();
  const [supports, setSupports] = useState(sharedSupports);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [showReset, setShowReset] = useState(false);
  const key = `user-pref-${user?.user_name || ""}-supports`;
  const canReset = hasAuthorization("CLUSTER.MANAGE_USER_PERSISTED_DATA");

  const save = async () => {
    setSaving(true);
    setError("");
    try {
      await ClusterApi.postPersistData(persistedPayload({ [key]: supports }));
      setSharedSupports(supports);
      navigate("/", { replace: true });
    } catch (saveError) {
      setError(responseErrorMessage(saveError, "Unable to save experimental settings."));
    } finally {
      setSaving(false);
    }
  };

  const reset = async () => {
    setSaving(true);
    setError("");
    try {
      await ClusterApi.postPersistData(persistedPayload({ "wizard-data": {} }));
      db.cleanUp();
      localStorage.removeItem("lastVisitedURL");
      window.location.hash = "/";
      window.location.reload();
    } catch (resetError) {
      setError(responseErrorMessage(resetError, "Unable to reset UI state."));
      setShowReset(false);
    } finally {
      setSaving(false);
    }
  };

  return (
    <main className="container py-4">
      <Alert variant="danger">
        <h2>Ambari's Experimental Functionality</h2>
        <strong>Changes made here are neither tested nor supported by Ambari.</strong>
        <p className="mb-0 mt-2">
          Experimental functionality can change or be removed without notice.
        </p>
      </Alert>
      {error ? <Alert variant="danger">{error}</Alert> : null}
      <Table hover responsive>
          <thead>
            <tr><th>Experimental Functionality</th><th>Enabled?</th></tr>
          </thead>
          <tbody>
            {Object.entries(supports).map(([name, enabled]) => (
              <tr key={name} className={enabled ? "table-active" : ""}>
                <td>{name}</td>
                <td>
                  <Form.Check
                    type="checkbox"
                    checked={enabled}
                    onChange={(event) => setSupports((current) => ({
                      ...current,
                      [name]: event.target.checked,
                    }))}
                    aria-label={`Enable ${name}`}
                  />
                </td>
              </tr>
            ))}
          </tbody>
      </Table>
      <div className="d-flex justify-content-between">
        <div>
          {canReset ? (
            <Button
              variant="danger"
              onClick={() => setShowReset(true)}
              disabled={saving || isNonWizardUser}
              title={isNonWizardUser ? "Another user owns the active wizard." : undefined}
            >
              Reset UI States
            </Button>
          ) : null}
        </div>
        <div className="d-flex gap-2">
          <Button variant="secondary" onClick={() => navigate("/", { replace: true })}>Cancel</Button>
          <Button variant="primary" onClick={() => void save()} disabled={saving}>Save</Button>
        </div>
      </div>
      {showReset ? (
        <Modal
          isOpen
          onClose={() => setShowReset(false)}
          modalTitle="Reset UI States"
          modalBody="Reset UI state locally and on the server?"
          successCallback={() => void reset()}
          options={{ okButtonText: "Yes", okButtonVariant: "danger" }}
        />
      ) : null}
    </main>
  );
}
