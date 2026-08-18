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
import { Alert, Button } from "react-bootstrap";
import { useNavigate } from "react-router-dom";
import VersionsApi from "../../../api/versionsApi";
import useKDCSessionState from "../../../hooks/useKDCSessionState";
import { AppContext } from "../../../store/context";
import { useAuth } from "../../../hooks/useAuth";

type Check = {
  id: string;
  check?: string;
  reason?: string;
  failed_on?: string[];
  failed_detail?: Array<{
    host_name?: string;
    service_name?: string;
    component_name?: string;
  }>;
};

type Props = {
  check: Check;
  repositoryVersionId: number;
  upgradeType: string;
  onRecheck: (data: { id: number; type: string }) => Promise<void>;
};

export default function PreUpgradeCheckItem({
  check,
  repositoryVersionId,
  upgradeType,
  onRecheck,
}: Props) {
  const {
    clusterName,
    upgradeId,
    upgradeSuspended,
    isNonWizardUser,
    setUpgradeState,
  } = useContext(AppContext);
  const { hasAuthorization } = useAuth();
  const { getKDCSessionState } = useKDCSessionState(null);
  const navigate = useNavigate();
  const [requestKey, setRequestKey] = useState("");
  const [error, setError] = useState<string | null>(null);
  const canRepair = hasAuthorization("CLUSTER.UPGRADE_DOWNGRADE_STACK") && !isNonWizardUser;
  const failedOn = Array.isArray(check.failed_on) ? check.failed_on : [];
  const failedDetails = Array.isArray(check.failed_detail) ? check.failed_detail : [];

  function requestErrorMessage(requestError: any) {
    return requestError?.response?.data?.message
      || requestError?.message
      || "The pre-upgrade repair request failed";
  }

  async function runRepair(key: string, repair: () => Promise<unknown>) {
    if (!canRepair || requestKey) return;
    setRequestKey(key);
    setError(null);
    try {
      const completed = await repair();
      if (completed !== false) {
        await onRecheck({ id: repositoryVersionId, type: upgradeType });
      }
    } catch (requestError: any) {
      setError(requestErrorMessage(requestError));
    } finally {
      setRequestKey("");
    }
  }

  async function runKdcProtectedRepair(repair: () => Promise<unknown>): Promise<false> {
    await getKDCSessionState(
      async () => {
        await repair();
        await onRecheck({ id: repositoryVersionId, type: upgradeType });
      },
      (requestError) => setError(requestErrorMessage(requestError)),
    );
    return false;
  }

  const repairButton = (
    key: string,
    label: string,
    repair: () => Promise<unknown>,
  ) => (
    <Button
      size="sm"
      variant="outline-primary"
      disabled={!canRepair || Boolean(requestKey)}
      onClick={() => void runRepair(key, repair)}
    >
      {requestKey === key ? "WORKING..." : label}
    </Button>
  );

  function customActions() {
    switch (check.id) {
      case "SERVICES_UP":
        return failedOn.length
          ? repairButton("services", "START SERVICES", () =>
              VersionsApi.startRequiredServices(clusterName, failedOn))
          : null;
      case "HOSTS_MASTER_MAINTENANCE":
        return failedOn.map((hostName) => (
          <li key={hostName} className="d-flex justify-content-between align-items-center gap-3 mb-2">
            <span>{hostName}</span>
            {repairButton(`maintenance-${hostName}`, "TURN OFF MAINTENANCE", () =>
              VersionsApi.disableHostsMaintenance(clusterName, [hostName]))}
          </li>
        ));
      case "HOSTS_HEARTBEAT":
        return failedOn.map((hostName) => (
          <li key={hostName} className="d-flex justify-content-between align-items-center gap-3 mb-2">
            <span>{hostName}</span>
            <Button
              size="sm"
              variant="outline-primary"
              onClick={() => navigate(`/main/hosts/${encodeURIComponent(hostName)}/summary`)}
            >
              REVIEW HOST
            </Button>
          </li>
        ));
      case "COMPONENTS_INSTALLATION":
        return failedDetails.map((component) => {
          const hostName = component.host_name || "";
          const serviceName = component.service_name || "";
          const componentName = component.component_name || "";
          const key = `${hostName}-${componentName}`;
          return (
            <li key={key} className="d-flex justify-content-between align-items-center gap-3 mb-2">
              <span>{serviceName}: {componentName} - {hostName}</span>
              {repairButton(key, "REINSTALL", () =>
                runKdcProtectedRepair(() => VersionsApi.reinstallFailedComponent(
                  clusterName,
                  hostName,
                  serviceName,
                  componentName,
                )))}
            </li>
          );
        });
      case "SERVICE_CHECK":
        return failedOn.map((serviceName) => (
          <li key={serviceName} className="d-flex justify-content-between align-items-center gap-3 mb-2">
            <span>{serviceName}</span>
            {repairButton(`service-check-${serviceName}`, "RUN SERVICE CHECK", () =>
              VersionsApi.runServiceCheck(clusterName, serviceName))}
          </li>
        ));
      case "PREVIOUS_UPGRADE_COMPLETED":
        if (!upgradeId) return null;
        return upgradeSuspended
          ? repairButton("resume", "RESUME PREVIOUS UPGRADE", async () => {
              await VersionsApi.retryUpgrade(clusterName, upgradeId);
              setUpgradeState("PENDING");
            })
          : repairButton("abort", "ABORT PREVIOUS UPGRADE", async () => {
              await VersionsApi.abortUpgrade(clusterName, upgradeId);
              setUpgradeState("ABORTED");
            });
      default:
        return null;
    }
  }

  const actions = customActions();
  return (
    <li className="mb-3">
      <div className="text-dark fw-semibold">{check.check || check.id}</div>
      {check.reason && <div className="my-2">{check.reason}</div>}
      {!actions && failedOn.length > 0 && <div>Failed on: {failedOn.join(", ")}</div>}
      {actions && <ul className="list-unstyled mb-0">{actions}</ul>}
      {error && <Alert variant="danger" className="mt-2 mb-0">{error}</Alert>}
    </li>
  );
}
