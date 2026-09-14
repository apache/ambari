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

import { useContext, useEffect, useRef, useState } from "react";
import { Alert, Button, Spinner, Table } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { ContextWrapper } from ".";
import { AppContext } from "../../store/context";
import ServiceDependenciesApi, { type ManagedDeployment } from "../../api/serviceDependenciesApi";
import { dependencyPhaseTranslationKey } from "../../Utils/managedDependencyPresentation";
import { createSecureUuid } from "../../Utils/uuid";
import WizardFooter from "../../components/StepWizard/WizardFooter";
import BackgroundOperations from "../BackgroundOperations";
import { ViewLevel } from "../../constants";
import { ActionTypes } from "./clusterStore/types";
import { type InstallWizardName, type ManagedDependencyInstallIntent, wizardCheckpoint } from "./installationProgress";

const labels: Record<string, string> = {
  NEW: "Preparing deployment",
  WAIT_PROVIDER: "Waiting for provider preparation",
  INSTALLING: "Installing selected components and credentials",
  WAIT_DEPENDENCIES: "Checking dependency connections",
  STARTING: "Starting selected services",
  CHECKING: "Running service checks",
  COMPLETE: "Deployment complete",
  INSTALL_ONLY: "Installation complete; services have not been started",
  FAILED: "Deployment needs attention",
  UNRESOLVED: "Deployment ownership could not be verified",
};

/** Renders one durable backend deployment; it never discovers or schedules Ambari requests. */
export default function ManagedDeploymentProgress({ wizardName }: { wizardName: InstallWizardName }) {
  const { t } = useTranslation();
  const { Context } = useContext(ContextWrapper);
  const { state, dispatch, flushStateToDb, stepWizardUtilities }: any = useContext(Context);
  const { supports } = useContext(AppContext);
  const steps = state?.[`${wizardName}Steps`] || {};
  const review = steps.REVIEW?.data || {};
  const restored = steps.INSTALL_START_TEST?.data || {};
  const handoff = restored.managedDependencyHandoff || review.managedDependencyHandoff;
  const intent = (restored.managedDependencyInstallIntent || handoff?.installIntent
    || review.managedDependencyInstallIntent) as ManagedDependencyInstallIntent | undefined;
  const clusterName = steps.NAME?.data?.clusterName || "";
  const [deployment, setDeployment] = useState<ManagedDeployment | null>(null);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);
  const [busy, setBusy] = useState(false);
  const [requestId, setRequestId] = useState<number | null>(null);
  const active = useRef(false);
  const pendingRetry = useRef<string | null>(restored.managedDeploymentRetryId || null);
  const checkpointed = useRef(restored.managedDeploymentVersion === 1);
  const acknowledged = useRef(restored.managedDeploymentAcknowledged === true);
  const installOnly = useRef(restored.managedDeploymentInstallOnly ?? Boolean(supports.skipComponentStartAfterInstall));
  const current = useRef({ dispatch, flushStateToDb });
  current.current = { dispatch, flushStateToDb };
  const savedStep = useRef({ ...restored, managedDependencyInstallIntent: intent,
    managedDependencyHandoff: handoff });
  const messages = (key: string, defaultValue: string) => t(`installer.step9.managedDeployment.${key}`, { defaultValue });
  const persist = (data: Record<string, unknown>) => {
    // Wizard reducers replace the whole step; retain the immutable handoff on every checkpoint.
    savedStep.current = { ...savedStep.current, ...data };
    current.current.dispatch({ type: ActionTypes.STORE_INFORMATION,
      payload: { step: "INSTALL_START_TEST", data: savedStep.current } });
  };
  const errorMessage = (reason: any) => reason?.response?.data?.message || reason?.message
    || messages("unavailable", "Ambari could not load the deployment. Reload its status before continuing.");

  useEffect(() => {
    active.current = true;
    const abort = new AbortController();
    let timer: ReturnType<typeof setTimeout> | undefined;
    const load = async () => {
      if (!intent?.intentId || !intent.targets?.length || intent.clusterName !== clusterName
        || intent.clusterId !== handoff?.clusterId) {
        setError(messages("missingIdentity", "The saved deployment identity is missing or belongs to another cluster. Return to its review."));
        return;
      }
      try {
        let result: ManagedDeployment;
        try {
          result = await ServiceDependenciesApi.getDeployment(clusterName, intent.intentId, abort.signal);
        } catch (reason: any) {
          if (reason?.response?.status !== 404) throw reason;
          if (acknowledged.current) {
            throw new Error(messages("lostIdentity", "The previously confirmed deployment is missing. Restore its server record before continuing; no replacement will be submitted."));
          }
          if (!checkpointed.current && (intent.state !== "READY" || intent.requestId != null)) {
            throw new Error(messages("legacyUnresolved", "This installation predates durable deployment tracking. Its original operation must be reconciled before another submission."));
          }
          if (!checkpointed.current) {
            persist({ managedDeploymentVersion: 1, managedDeploymentInstallOnly: installOnly.current, managedDependencyInstallIntent: intent,
              managedDependencyHandoff: handoff });
            await current.current.flushStateToDb("default");
            checkpointed.current = true;
          }
          if (abort.signal.aborted) return;
          // Repeating this exact ID reconciles an already committed launch after a lost response.
          result = await ServiceDependenciesApi.launchDeployment(clusterName, intent.intentId,
            intent.targets, installOnly.current, abort.signal);
        }
        if (abort.signal.aborted) return;
        if (result.cluster_id !== intent.clusterId || result.deployment_id !== intent.intentId) {
          throw new Error(messages("wrongIdentity", "Ambari returned a different deployment identity."));
        }
        if (!acknowledged.current) {
          acknowledged.current = true;
          persist({ managedDeploymentAcknowledged: true });
          await current.current.flushStateToDb("default");
          if (abort.signal.aborted) return;
        }
        setDeployment(result);
        setError("");
        if (pendingRetry.current && (result.attempt_id === pendingRetry.current
          || result.history.some(attempt => attempt.attemptId === pendingRetry.current))) {
          pendingRetry.current = null;
          persist({ managedDeploymentRetryId: null });
        }
        if (!["COMPLETE", "INSTALL_ONLY", "UNRESOLVED"].includes(result.state)) {
          timer = setTimeout(load, 3000);
        }
      } catch (reason: any) {
        if (!abort.signal.aborted) setError(errorMessage(reason));
      }
    };
    void load();
    return () => {
      active.current = false;
      abort.abort();
      if (timer) clearTimeout(timer);
    };
  }, [clusterName, intent?.intentId, reload]);

  const retry = async () => {
    if (!deployment?.retry_allowed || !intent || busy) return;
    setBusy(true);
    try {
      const operationId = pendingRetry.current || createSecureUuid();
      pendingRetry.current = operationId;
      persist({ managedDeploymentRetryId: operationId });
      await current.current.flushStateToDb("default");
      if (!active.current) return;
      await ServiceDependenciesApi.retryDeployment(clusterName, intent.intentId, operationId);
      if (active.current) setReload(value => value + 1);
    } catch (reason) {
      if (active.current) setError(errorMessage(reason));
    } finally {
      if (active.current) setBusy(false);
    }
  };

  const verifyCredentials = async (bindingId: string, epoch: number) => {
    setBusy(true);
    try {
      await ServiceDependenciesApi.verifyCredentials(clusterName, bindingId, epoch);
      if (active.current) setReload(value => value + 1);
    } catch (reason) {
      if (active.current) setError(errorMessage(reason));
    } finally {
      if (active.current) setBusy(false);
    }
  };

  const finished = !error && Boolean(deployment?.completed || deployment?.install_only);
  return <>
    <div className="step-title">{messages("title", "Deploy selected services")}</div>
    <p>{clusterName}</p>
    <Alert variant={deployment?.completed ? "success" : deployment?.state === "FAILED" || error ? "danger" : "info"}>
      {!deployment && !error ? <Spinner size="sm" className="me-2" /> : null}
      {deployment ? messages(deployment.state, labels[deployment.state] || "Loading deployment status")
        : messages("loading", "Loading deployment status")}
      {deployment?.state === "UNRESOLVED" ? <p>{messages("unresolved", "The original request or binding association is missing. Ambari will not guess ownership or submit a replacement.")}</p> : null}
    </Alert>
    {error ? <Alert variant="danger">{error}</Alert> : null}
    <Button variant="outline-secondary" size="sm" disabled={busy} onClick={() => setReload(value => value + 1)}>
      {messages("reload", "Reload status")}
    </Button>
    {deployment?.bindings.filter(binding => binding.ownership === "managed").map(binding =>
      <div className="mt-3" key={binding.binding_id}>
        <strong>{binding.dependency_type}</strong>{" — "}{binding.provider?.cluster_name || clusterName}
        <div>{binding.failure_message || t(dependencyPhaseTranslationKey(binding.phase))}</div>
        {binding.capabilities?.manual_credentials_allowed && binding.binding_id && binding.operation_epoch ?
          <Button variant="outline-primary" size="sm" disabled={busy}
            onClick={() => void verifyCredentials(binding.binding_id!, binding.operation_epoch!)}>
            {messages("verifyManual", "Verify manually installed credentials")}
          </Button> : null}
      </div>)}
    <Table className="mt-3" responsive>
      <thead><tr><th>{messages("operation", "Operation")}</th><th>{messages("tasks", "Tasks")}</th></tr></thead>
      <tbody>{deployment?.history.map((attempt, index) => <tr key={`${attempt.attemptId}:${attempt.phase}:${index}`}>
        <td>{attempt.phase === "CHECKS" ? messages("checks", "Service checks")
          : attempt.phase === "START" ? messages("start", "Start services") : messages("install", "Install components")}</td>
        <td>{attempt.requestId != null ? <Button variant="link" size="sm" onClick={() => setRequestId(attempt.requestId)}>
          {messages("logs", "View task logs")}
        </Button> : messages("noChanges", "Targets already satisfied")}</td>
      </tr>)}</tbody>
    </Table>
    {requestId != null ? <BackgroundOperations isExplicitClick rootLevel={ViewLevel.TASKS_LIST}
      clusterName={clusterName} requestId={requestId} isOpen onClose={() => setRequestId(null)} /> : null}
    <WizardFooter lifted onBack={() => {}} step={stepWizardUtilities.currentStep} isNextEnabled={finished}
      isBackEnabled={false} isCancelEnabled={false}
      sideItems={deployment?.retry_allowed ? <Button disabled={busy} onClick={() => void retry()}>
        {messages("retry", "Retry failed deployment")}</Button> : null}
      onNext={async () => {
        if (!finished || !deployment) return;
        persist({ hostInfo: [...new Set(deployment.targets.map(target => target.hostName))].map(name => ({
          name, status: "success", progress: 100, logTasks: [], lastRequestId: deployment.request_id,
          message: deployment.install_only ? "Installation complete" : "Deployment complete",
        })), clusterStatus: { status: deployment.install_only ? "START_SKIPPED" : "STARTED",
          isCompleted: true, requestId: deployment.request_id,
          oldRequestsId: deployment.history.map(attempt => attempt.requestId).filter(id => id != null),
          phase: "COMPLETE" }, phase: "COMPLETE" });
        await current.current.flushStateToDb("next", -1, wizardCheckpoint(wizardName, "INSTALLED"));
        if (active.current) stepWizardUtilities.handleNextImperitive();
      }} />
  </>;
}
