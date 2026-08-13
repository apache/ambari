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
import { Alert, Form, Table } from "react-bootstrap";
import ClusterApi from "../api/clusterApi";
import LoginApi from "../api/loginApi";
import { useAuth } from "../hooks/useAuth";
import { detectUserTimezone, parseTimezones } from "../Utils/timezone";
import { parsePersistedValue, persistedPayload } from "../Utils/persistedSettings";
import Modal from "./Modal";
import { AppContext } from "../store/context";

type UserSettingsModalProps = {
  isOpen: boolean;
  onClose: () => void;
};

type PrivilegeGroup = {
  name: string;
  privileges: string[];
};

export default function UserSettingsModal({ isOpen, onClose }: UserSettingsModalProps) {
  const { user, isAdmin, hasAuthorization } = useAuth();
  const { syncUserBgPreferences } = useContext(AppContext);
  const [showBackgroundOperations, setShowBackgroundOperations] = useState(true);
  const [timezone, setTimezone] = useState(detectUserTimezone());
  const [initialTimezone, setInitialTimezone] = useState("");
  const [clusterPrivileges, setClusterPrivileges] = useState<PrivilegeGroup[]>([]);
  const [viewPrivileges, setViewPrivileges] = useState<PrivilegeGroup[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const loginName = user?.user_name || "";
  const showBackgroundKey = `admin-settings-show-bg-${loginName}`;
  const timezoneKey = `admin-settings-timezone-${loginName}`;
  const canPersist = hasAuthorization("CLUSTER.MANAGE_USER_PERSISTED_DATA");

  useEffect(() => {
    if (!isOpen || !loginName) {
      return;
    }
    async function load() {
      setLoading(true);
      setError("");
      try {
        const [settings, privilegesResponse] = await Promise.all([
          ClusterApi.getPersistData(),
          LoginApi.loadPrivileges(loginName),
        ]);
        const detectedTimezone = detectUserTimezone();
        const savedTimezone = parsePersistedValue(settings?.[timezoneKey], detectedTimezone);
        setTimezone(savedTimezone);
        setInitialTimezone(savedTimezone);
        setShowBackgroundOperations(parsePersistedValue(settings?.[showBackgroundKey], true));
        if (canPersist) {
          const missingDefaults: Record<string, unknown> = {};
          if (settings?.[showBackgroundKey] === undefined) {
            missingDefaults[showBackgroundKey] = true;
          }
          if (settings?.[timezoneKey] === undefined) {
            missingDefaults[timezoneKey] = detectedTimezone;
          }
          if (Object.keys(missingDefaults).length) {
            void ClusterApi.postPersistData(persistedPayload(missingDefaults)).catch(() => undefined);
          }
        }

        const clusters = new Map<string, string[]>();
        const views = new Map<string, string[]>();
        (privilegesResponse.data.items || []).forEach((item: any) => {
          const privilege = item.PrivilegeInfo;
          const target = privilege.type === "CLUSTER" ? clusters : privilege.type === "VIEW" ? views : null;
          const name = privilege.type === "CLUSTER" ? privilege.cluster_name : privilege.instance_name;
          if (target && name) {
            target.set(name, [...(target.get(name) || []), privilege.permission_label]);
          }
        });
        setClusterPrivileges(Array.from(clusters, ([name, privileges]) => ({ name, privileges })));
        setViewPrivileges(Array.from(views, ([name, privileges]) => ({ name, privileges })));
      } catch (loadError: any) {
        setError(loadError?.response?.data?.message || "Unable to load user settings.");
      } finally {
        setLoading(false);
      }
    }
    void load();
  }, [canPersist, isOpen, loginName, showBackgroundKey, timezoneKey]);

  const save = async () => {
    if (!canPersist) return;
    setSaving(true);
    setError("");
    try {
      await ClusterApi.postPersistData(persistedPayload({
        [showBackgroundKey]: showBackgroundOperations,
        [timezoneKey]: timezone,
      }));
      syncUserBgPreferences(showBackgroundOperations);
      onClose();
      if (timezone !== initialTimezone) {
        window.location.reload();
      }
    } catch (saveError: any) {
      setError(saveError?.response?.data?.message || "Unable to save user settings.");
    } finally {
      setSaving(false);
    }
  };

  const privilegeRows = (groups: PrivilegeGroup[]) => groups.map((group) => (
    <tr key={group.name}>
      <td>{group.name}</td>
      <td>{group.privileges.join(", ")}</td>
    </tr>
  ));

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      modalTitle="User Settings"
      modalBody={loading ? <p>Loading...</p> : (
        <div>
          {error ? <Alert variant="danger">{error}</Alert> : null}
          <h4>General</h4>
          <Form.Check
            className="mb-4"
            type="checkbox"
            label="Do not show the Background Operations dialog automatically"
            checked={!showBackgroundOperations}
            onChange={(event) => setShowBackgroundOperations(!event.target.checked)}
          />
          <h4>Locale</h4>
          <Form.Select
            className="mb-4"
            value={timezone}
            onChange={(event) => setTimezone(event.target.value)}
            aria-label="Timezone"
          >
            {parseTimezones().map((item) => (
              <option key={item.value} value={item.value}>{item.label}</option>
            ))}
          </Form.Select>
          {isAdmin() ? (
            <Alert variant="info">Ambari administrators have all privileges.</Alert>
          ) : (
            <Table hover responsive>
              <thead><tr><th>Cluster</th><th>Cluster Role</th></tr></thead>
              <tbody>
                {clusterPrivileges.length ? privilegeRows(clusterPrivileges) : (
                  <tr><td colSpan={2}>No cluster privileges</td></tr>
                )}
              </tbody>
              <thead><tr><th>View</th><th>View Permissions</th></tr></thead>
              <tbody>
                {viewPrivileges.length ? privilegeRows(viewPrivileges) : (
                  <tr><td colSpan={2}>No View privileges</td></tr>
                )}
              </tbody>
            </Table>
          )}
          {!canPersist ? (
            <Alert variant="warning">
              You do not have permission to persist user settings.
            </Alert>
          ) : null}
        </div>
      )}
      successCallback={() => void save()}
      options={{
        modalSize: "modal-lg",
        okButtonText: saving ? "SAVING..." : "SAVE",
        cancelButtonText: "CANCEL",
        okButtonVariant: "success",
        okButtonDisabled: loading || saving || !canPersist,
      }}
    />
  );
}
