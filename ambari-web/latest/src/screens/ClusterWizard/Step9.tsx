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
import { Alert, Button, ProgressBar, Spinner, Table } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { get } from "lodash";
import { useBlocker } from "react-router-dom";
import { AppContext } from "../../store/context";
import { ContextWrapper } from ".";
import WizardFooter from "../../components/StepWizard/WizardFooter";
import BackgroundOperations from "../BackgroundOperations";
import { ViewLevel } from "../../constants";
import { RequestApi } from "../../api/requestApi";
import { HostsApi } from "../../api/hostsApi";
import { ServiceApi } from "../../api/serviceApi";
import ServiceDependenciesApi, {
  type ManagedDependencyBinding,
  type ManagedDependencyPreparationRequest,
} from "../../api/serviceDependenciesApi";
import { ActionTypes } from "./clusterStore/types";
import {
  canEnterSummary,
  canRetryInstallation,
  failedTaskStatuses,
  InstallWizardName,
  InstallationPhase,
  type ManagedDependencyInstallIntent,
  mergeInstallTasks,
  requestFailed,
  requestFinished,
  requestIdFrom,
  terminalTaskStatuses,
  wizardCheckpoint,
} from "./installationProgress";

type Step9Props = {
  wizardName?: InstallWizardName;
};

function errorMessage(error: any): string {
  const message = error?.response?.data?.message
    || error?.response?.data
    || error?.message
    || "Ambari could not continue deployment.";
  return typeof message === "string" ? message : JSON.stringify(message);
}

function phaseLabel(phase: InstallationPhase): string {
  switch (phase) {
    case "WAIT_FOR_PROVIDER_PREPARATION": return "Waiting for provider preparation";
    case "INSTALL": return "Installing services and components";
    case "KEYTABS": return "Regenerating Kerberos keytabs";
    case "START": return "Starting services and running service checks";
    default: return "Deployment complete";
  }
}

type ManagedDependencyHandoff = {
  phase: "WAIT_FOR_PROVIDER_PREPARATION";
  clusterId?: number;
  clusterName?: string;
  consumerServiceName?: "HBASE";
  installIntent?: ManagedDependencyInstallIntent;
  items: Array<{
    bindingId: string;
    dependencyType: "HDFS" | "ZOOKEEPER";
    operationId: string;
  }>;
};

const sanctionedInstallAuxiliaryCommands = new Set([
  "EXECUTE",
  "INSTALL_PACKAGES",
  "SERVICE_CHECK",
]);
const terminalPreparationStates = new Set(["SUCCEEDED", "COMPLETED", "SUCCESS"]);

const taskData = (task: any) => task?.Tasks || task || {};

const requestTaskId = (task: any) => taskData(task).id ?? taskData(task).task_id;

const installTargetKey = (target: {
  serviceName: string;
  componentName: string;
  hostName: string;
}) => `${target.serviceName}:${target.componentName}:${target.hostName}`;

function Step9({ wizardName = "clusterCreation" }: Step9Props) {
  const { t } = useTranslation();
  const { Context } = useContext(ContextWrapper);
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { currentStep, handleNextImperitive },
  }: any = useContext(Context);
  const { isKerberosEnabled, supports } = useContext(AppContext);
  const stepPrefix = `${wizardName}Steps`;
  const getStepData = (stepName: string, dataKey = "") => get(
    state,
    `${stepPrefix}.${stepName}.data${dataKey ? `.${dataKey}` : ""}`,
    dataKey ? undefined : {},
  );

  const clusterName = getStepData("NAME", "clusterName") || "";
  const reviewStatus = getStepData("REVIEW", "clusterStatus") || {};
  const restoredInstall = getStepData("INSTALL_START_TEST") || {};
  const managedDependencyHandoff = (
    restoredInstall.managedDependencyHandoff
    || getStepData("REVIEW", "managedDependencyHandoff")
  ) as ManagedDependencyHandoff | undefined;
  const managedDependencyInstallIntent = (
    restoredInstall.managedDependencyInstallIntent
    || managedDependencyHandoff?.installIntent
    || getStepData("REVIEW", "managedDependencyInstallIntent")
  ) as ManagedDependencyInstallIntent | undefined;
  const initialStatus = restoredInstall.clusterStatus || reviewStatus;
  const initialRequestId = initialStatus.requestId ?? managedDependencyInstallIntent?.requestId;
  const restoredStatus = initialRequestId != null && initialStatus.requestId == null
    ? { ...initialStatus, requestId: initialRequestId }
    : initialStatus;
  const initialPhase: InstallationPhase = restoredInstall.phase
    || restoredStatus.phase
    || managedDependencyHandoff?.phase
    || (restoredStatus.status === "STARTED" ? "COMPLETE" : "INSTALL");
  const initialTerminal = canEnterSummary(wizardName, restoredStatus.status || "");
  const registeredHosts = (getStepData("HOST_STATUS", "hosts") || [])
    .filter((host: any) => host.bootStatus === "REGISTERED");
  const initialHosts = restoredInstall.hostInfo?.length
    ? restoredInstall.hostInfo
    : registeredHosts.map((host: any) => ({
      name: host.name,
      status: restoredStatus.status === "STARTED" ? "success" : "pending",
      progress: restoredStatus.status === "STARTED" ? 100 : 0,
      message: restoredStatus.status === "STARTED"
        ? "Install and start completed"
        : "Waiting",
      logTasks: [],
    }));

  const [hosts, setHosts] = useState<any[]>(initialHosts);
  const [clusterStatus, setClusterStatus] = useState<any>(restoredStatus);
  const [phase, setPhase] = useState<InstallationPhase>(initialPhase);
  const [working, setWorking] = useState(!initialTerminal);
  const [terminal, setTerminal] = useState(initialTerminal);
  const [pollError, setPollError] = useState("");
  const [operationError, setOperationError] = useState(
    restoredStatus.operationError || "",
  );
  const [selectedHost, setSelectedHost] = useState("");
  const [selectedRequestId, setSelectedRequestId] = useState<
    string | number | null
  >(null);
  const [managedDependencyBindings, setManagedDependencyBindings] = useState<
    ManagedDependencyBinding[]
  >([]);
  const [managedDependencyPollError, setManagedDependencyPollError] = useState("");

  const active = useRef(false);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const managedDependencyTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const requestInFlight = useRef(false);
  const managedDependencyPollInFlight = useRef(false);
  const managedInstallStarted = useRef(
    managedDependencyInstallIntent?.state === "SUBMITTING"
    || managedDependencyInstallIntent?.state === "SUBMITTED",
  );
  const managedDependencyHandoffRef = useRef(managedDependencyHandoff);
  const managedDependencyInstallIntentRef = useRef(managedDependencyInstallIntent);
  const managedDependencyScopeKey = JSON.stringify([
    managedDependencyHandoff?.clusterId,
    managedDependencyHandoff?.clusterName,
    managedDependencyHandoff?.consumerServiceName,
    managedDependencyHandoff?.items || [],
    managedDependencyInstallIntent?.intentId,
    managedDependencyInstallIntent?.serviceNames || [],
    managedDependencyInstallIntent?.targets || [],
  ]);
  const managedDependencyScopeKeyRef = useRef(managedDependencyScopeKey);
  managedDependencyScopeKeyRef.current = managedDependencyScopeKey;
  const hostsRef = useRef<any[]>(initialHosts);
  const clusterStatusRef = useRef<any>(restoredStatus);
  const phaseRef = useRef<InstallationPhase>(initialPhase);
  const requestIdRef = useRef<string | number | undefined>(initialRequestId);

  managedDependencyHandoffRef.current = managedDependencyHandoff;

  const blocker = useBlocker(({ currentLocation, nextLocation }) =>
    working && currentLocation.pathname !== nextLocation.pathname,
  );

  const persist = (
    nextHosts: any[],
    nextStatus: any,
    nextPhase: InstallationPhase,
    extraData: Record<string, unknown> = {},
  ) => {
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: "INSTALL_START_TEST",
        data: {
          hostInfo: nextHosts,
          clusterStatus: nextStatus,
          phase: nextPhase,
          ...(managedDependencyHandoffRef.current
            ? { managedDependencyHandoff: managedDependencyHandoffRef.current }
            : {}),
          ...(managedDependencyInstallIntentRef.current
            ? { managedDependencyInstallIntent: managedDependencyInstallIntentRef.current }
            : {}),
          ...extraData,
        },
      },
    });
  };

  const updateDeploymentState = (
    nextStatus: any,
    nextPhase: InstallationPhase,
    nextHosts = hostsRef.current,
  ) => {
    hostsRef.current = nextHosts;
    clusterStatusRef.current = nextStatus;
    phaseRef.current = nextPhase;
    requestIdRef.current = nextStatus.requestId;
    setHosts(nextHosts);
    setClusterStatus(nextStatus);
    setPhase(nextPhase);
    persist(nextHosts, nextStatus, nextPhase);
  };

  const schedulePoll = (delay = 0) => {
    if (!active.current || requestIdRef.current == null) return;
    if (timer.current) clearTimeout(timer.current);
    timer.current = setTimeout(() => void pollCurrentRequest(), delay);
  };

  const setRequest = async (
    requestId: string | number,
    nextPhase: InstallationPhase,
    status: string,
    extraData: Record<string, unknown> = {},
  ) => {
    const nextStatus = {
      ...clusterStatusRef.current,
      status,
      requestId,
      oldRequestsId: Array.from(new Set([
        ...(clusterStatusRef.current.oldRequestsId || []),
        requestId,
      ])),
      isCompleted: false,
      operationError: "",
      phase: nextPhase,
    };
    setOperationError("");
    setPollError("");
    setTerminal(false);
    setWorking(true);
    updateDeploymentState(nextStatus, nextPhase);
    if (Object.keys(extraData).length) {
      persist(hostsRef.current, nextStatus, nextPhase, extraData);
    }
    await Promise.resolve(flushStateToDb(
      "checkpoint",
      -1,
      wizardCheckpoint(
        wizardName,
        nextPhase === "INSTALL" ? "INSTALLING" : "STARTING",
      ),
    ));
    schedulePoll();
  };

  const completeSuccess = async (status = "STARTED") => {
    const completedHosts = hostsRef.current.map((host) => ({
      ...host,
      status: host.status === "failed" ? "warning" : "success",
      progress: 100,
      message: host.status === "failed"
        ? "Deployment completed with warnings"
        : status === "START_SKIPPED"
          ? "Install completed; component start was skipped"
          : "Install and start completed",
    }));
    const nextStatus = {
      ...clusterStatusRef.current,
      status,
      isCompleted: true,
      phase: "COMPLETE",
      operationError: "",
    };
    updateDeploymentState(nextStatus, "COMPLETE", completedHosts);
    setWorking(false);
    setTerminal(true);
    await Promise.resolve(flushStateToDb("default"));
  };

  const completeFailure = async (
    failedPhase: InstallationPhase,
    message = "",
  ) => {
    const nextStatus = {
      ...clusterStatusRef.current,
      status: failedPhase === "INSTALL" ? "INSTALL FAILED" : "START FAILED",
      isCompleted: true,
      failedPhase,
      phase: failedPhase,
      operationError: message,
    };
    updateDeploymentState(nextStatus, failedPhase);
    setOperationError(message);
    setWorking(false);
    setTerminal(true);
    await Promise.resolve(flushStateToDb("default"));
  };

  const launchKeytabRegeneration = async () => {
    try {
      const response = await RequestApi.regenerateKeytabs(
        clusterName,
        { Clusters: { security_type: "KERBEROS" } },
        "regenerate_keytabs=all",
      );
      if (!active.current) return;
      const requestId = requestIdFrom(response);
      if (requestId == null) {
        throw new Error("Ambari did not return a keytab request ID.");
      }
      await setRequest(requestId, "KEYTABS", "INSTALLED");
    } catch (error: any) {
      if (active.current) await completeFailure("KEYTABS", errorMessage(error));
    }
  };

  const loadSkipServiceChecks = async (): Promise<boolean> => {
    try {
      const response = await ServiceApi.ambariService(
        "?fields=RootServiceComponents/properties/skip.service.checks",
      );
      return get(
        response,
        "RootServiceComponents.properties.skip.service.checks",
      ) === "true";
    } catch {
      return false;
    }
  };

  const selectedServices = () => Object.values(
    getStepData("SERVICES", "services") || {},
  ).filter((service: any) => service.selected && !service.installed)
    .map((service: any) => service.serviceName);

  const managedInstallServices = () =>
    managedDependencyInstallIntentRef.current?.serviceNames?.length
      ? managedDependencyInstallIntentRef.current.serviceNames
      : selectedServices();

  const validateManagedDependencyBinding = (
    binding: ManagedDependencyBinding,
    item: ManagedDependencyHandoff["items"][number],
    handoff: ManagedDependencyHandoff,
  ) => {
    const expectedClusterId = handoff.clusterId
      ?? managedDependencyInstallIntentRef.current?.clusterId;
    const expectedServiceName = handoff.consumerServiceName || "HBASE";
    if (binding.binding_id !== item.bindingId
      || binding.dependency_type !== item.dependencyType
      || !binding.consumer
      || binding.consumer.cluster_id !== expectedClusterId
      || binding.consumer.service_name !== expectedServiceName) {
      throw new Error(t("installer.step9.dependencyBindingMismatch"));
    }
  };

  const expectedManagedInstallTargets = (intent: ManagedDependencyInstallIntent) => {
    const targets = new Map<string, ManagedDependencyInstallIntent["targets"][number]>();
    intent.targets.forEach((target) => targets.set(installTargetKey(target), target));
    return targets;
  };

  const targetForTask = (
    task: any,
    intent: ManagedDependencyInstallIntent,
    allowAuxiliary = true,
  ) => {
    const data = taskData(task);
    const componentName = data.component_name || data.componentName || data.role;
    const hostName = data.host_name || data.hostName;
    if (!componentName || !hostName) {
      const command = String(data.command || "").toUpperCase();
      if (allowAuxiliary && sanctionedInstallAuxiliaryCommands.has(command)) return null;
      throw new Error(t("installer.step9.installTargetMismatch"));
    }
    const serviceName = data.service_name || data.serviceName;
    const matches = intent.targets.filter((target) =>
      target.componentName === componentName
      && target.hostName === hostName
      && (!serviceName || target.serviceName === serviceName));
    if (matches.length !== 1) {
      throw new Error(t("installer.step9.installTargetMismatch"));
    }
    return matches[0];
  };

  const validateManagedInstallRequestTargets = (response: any) => {
    const intent = managedDependencyInstallIntentRef.current;
    if (!intent) return;
    const expectedTargets = expectedManagedInstallTargets(intent);
    if (!expectedTargets.size || !Array.isArray(response?.tasks) || !response.tasks.length) {
      throw new Error(t("installer.step9.installTargetMismatch"));
    }
    const responseRequestId = response.Requests?.id ?? response.Requests?.request_id;
    if (responseRequestId == null) {
      throw new Error(t("installer.step9.installTargetMismatch"));
    }
    const seen = new Set<string>();
    response.tasks.forEach((task: any) => {
      const target = targetForTask(task, intent);
      if (!target) return;
      const key = installTargetKey(target);
      if (seen.has(key)) throw new Error(t("installer.step9.installTargetMismatch"));
      seen.add(key);
      const data = taskData(task);
      const taskRequestId = data.request_id ?? data.requestId;
      if (requestTaskId(task) == null || taskRequestId == null
        || String(taskRequestId) !== String(responseRequestId)) {
        throw new Error(t("installer.step9.installTargetMismatch"));
      }
    });
    if (seen.size !== expectedTargets.size
      || Array.from(expectedTargets.keys()).some((key) => !seen.has(key))) {
      throw new Error(t("installer.step9.installTargetMismatch"));
    }
  };

  const reconcilePreparationLineage = async (
    bindings: ManagedDependencyBinding[],
    handoff: ManagedDependencyHandoff,
    intent: ManagedDependencyInstallIntent,
    capturedScope: string,
  ) => {
    const expectedTargets = expectedManagedInstallTargets(intent);
    if (!expectedTargets.size) {
      throw new Error(t("installer.step9.installTargetMismatch"));
    }
    const coveredTargets = new Set<string>();
    const preparationRequestIds = new Set<string>();
    for (let index = 0; index < handoff.items.length; index += 1) {
      const item = handoff.items[index];
      const binding = bindings[index];
      const readiness = binding.readiness;
      const rows = readiness?.preparation_requests;
      if (!Array.isArray(rows)) {
        throw new Error(t("installer.step9.dependencyBindingMismatch"));
      }
      const bindingRows = rows.filter((row) => row.binding_id === item.bindingId);
      const currentRows = bindingRows.filter((row) => row.operation_id === item.operationId);
      const currentEpoch = binding.operation_epoch ?? binding.operation?.epoch;
      const currentSnapshot = binding.desired_snapshot_version;
      if (!Number.isInteger(currentEpoch) || !Number.isInteger(currentSnapshot)
        || !currentRows.length || bindingRows.length !== currentRows.length) {
        throw new Error(t("installer.step9.dependencyBindingMismatch"));
      }
      const itemCoveredTargets = new Set<string>();
      for (const row of currentRows as ManagedDependencyPreparationRequest[]) {
        if (row.epoch !== currentEpoch || row.snapshot_version !== currentSnapshot
          || !terminalPreparationStates.has(String(row.state).toUpperCase())
          || row.request_id == null || row.task_id == null) {
          throw new Error(t("installer.step9.dependencyBindingMismatch"));
        }
        preparationRequestIds.add(String(row.request_id));
        const response = await RequestApi.getRequestStatus(
          handoff.clusterName || clusterName,
          String(row.request_id),
        );
        if (!active.current || managedDependencyScopeKeyRef.current !== capturedScope) {
          return null;
        }
        const responseRequestId = response?.Requests?.id ?? response?.Requests?.request_id;
        if (responseRequestId == null || String(responseRequestId) !== String(row.request_id)) {
          throw new Error(t("installer.step9.dependencyBindingMismatch"));
        }
        const matchingTasks = (response?.tasks || []).filter((task: any) =>
          String(requestTaskId(task)) === String(row.task_id));
        if (matchingTasks.length !== 1) {
          throw new Error(t("installer.step9.dependencyBindingMismatch"));
        }
        const task = taskData(matchingTasks[0]);
        const componentName = task.component_name || task.componentName || task.role;
        const hostName = task.host_name || task.hostName;
        const taskHostId = task.host_id ?? task.hostId;
        if (componentName !== row.component_name
          || !hostName
          || (taskHostId != null && String(taskHostId) !== String(row.host_id))) {
          throw new Error(t("installer.step9.dependencyBindingMismatch"));
        }
        const candidates = intent.targets.filter((target) =>
          target.componentName === componentName && target.hostName === hostName);
        const taskServiceName = task.service_name || task.serviceName;
        const target = candidates.length === 1
          ? candidates[0]
          : candidates.filter((candidate) => candidate.serviceName === taskServiceName)[0];
        if (!target || (taskServiceName && target.serviceName !== taskServiceName)) {
          throw new Error(t("installer.step9.installTargetMismatch"));
        }
        const key = installTargetKey(target);
        if (!expectedTargets.has(key) || itemCoveredTargets.has(key)) {
          throw new Error(t("installer.step9.installTargetMismatch"));
        }
        itemCoveredTargets.add(key);
        coveredTargets.add(key);
      }
      if (itemCoveredTargets.size !== expectedTargets.size) {
        throw new Error(t("installer.step9.installTargetMismatch"));
      }
    }
    if (coveredTargets.size !== expectedTargets.size
      || Array.from(expectedTargets.keys()).some((key) => !coveredTargets.has(key))) {
      throw new Error(t("installer.step9.installTargetMismatch"));
    }
    return preparationRequestIds;
  };

  const reconcileLostInstallSubmission = async (
    bindings: ManagedDependencyBinding[],
    handoff: ManagedDependencyHandoff,
    intent: ManagedDependencyInstallIntent,
    capturedScope: string,
  ) => {
    const preparationRequestIds = await reconcilePreparationLineage(
      bindings,
      handoff,
      intent,
      capturedScope,
    );
    if (!preparationRequestIds
      || !active.current
      || managedDependencyScopeKeyRef.current !== capturedScope) return;
    const response = await RequestApi.getRequests(handoff.clusterName || clusterName);
    if (!active.current || managedDependencyScopeKeyRef.current !== capturedScope) return;
    const requests = Array.isArray(response?.items) ? response.items : [];
    const candidates = requests.filter((request: any) => {
      const requestId = request?.Requests?.id ?? request?.Requests?.request_id;
      if (requestId == null || preparationRequestIds.has(String(requestId))
        || request?.Requests?.request_context !== "Install Services") return false;
      if (intent.submissionStartedAt != null) {
        const rawStart = request?.Requests?.start_time;
        const startedAt = typeof rawStart === "number"
          ? rawStart
          : Date.parse(String(rawStart || ""));
        if (!Number.isFinite(startedAt) || startedAt < intent.submissionStartedAt) return false;
      }
      try {
        validateManagedInstallRequestTargets(request);
        return true;
      } catch {
        return false;
      }
    });
    if (candidates.length !== 1) {
      throw new Error(t("installer.step9.installSubmissionUnknown"));
    }
    const requestId = candidates[0].Requests?.id ?? candidates[0].Requests?.request_id;
    if (requestId == null) {
      throw new Error(t("installer.step9.installSubmissionUnknown"));
    }
    const submittedIntent = { ...intent, state: "SUBMITTED" as const, requestId };
    const submittedHandoff = {
      ...handoff,
      installIntent: submittedIntent,
    };
    managedDependencyInstallIntentRef.current = submittedIntent;
    managedDependencyHandoffRef.current = submittedHandoff;
    await setRequest(requestId, "INSTALL", "PENDING", {
      managedDependencyInstallIntent: submittedIntent,
      managedDependencyHandoff: submittedHandoff,
    });
  };

  const managedDependencyNextAction = (binding: ManagedDependencyBinding) => {
    const nextAction = binding.capabilities?.next_action || binding.next_action;
    switch (nextAction) {
      case "WAIT_FOR_PROVIDER_PREPARATION":
        return t("installer.step9.providerPreparationWaiting");
      case "INSTALL_OR_CONFIGURE":
        return t("installer.step9.providerPreparationReady");
      default:
        return t("installer.step9.providerStatusUnavailable");
    }
  };

  const managedDependencyInstallAllowed = (binding: ManagedDependencyBinding) =>
    binding.capabilities?.install_or_configure_allowed === true
    && binding.capabilities.allowed_actions?.includes("INSTALL_OR_CONFIGURE") === true;

  const scheduleManagedDependencyPoll = (delay = 0) => {
    if (!active.current || !managedDependencyHandoffRef.current?.items.length) return;
    if (managedDependencyTimer.current) clearTimeout(managedDependencyTimer.current);
    managedDependencyTimer.current = setTimeout(
      () => void pollManagedDependencies(),
      delay,
    );
  };

  const launchManagedInstall = async () => {
    if (managedInstallStarted.current) return;
    const capturedScope = managedDependencyScopeKeyRef.current;
    const intent = managedDependencyInstallIntentRef.current;
    const serviceNames = intent?.serviceNames?.length
      ? intent.serviceNames
      : managedInstallServices();
    if (!serviceNames.length) {
      throw new Error(t("installer.step9.noSelectedService"));
    }
    if (intent && intent.clusterName !== clusterName) {
      throw new Error(t("installer.step9.dependencyBindingMismatch"));
    }
    const targetGroups = new Map<string, {
      serviceName: string;
      componentName: string;
      hostNames: string[];
    }>();
    (intent?.targets || []).forEach((target) => {
      const groupKey = `${target.serviceName}:${target.componentName}`;
      const group = targetGroups.get(groupKey) || {
        serviceName: target.serviceName,
        componentName: target.componentName,
        hostNames: [],
      };
      if (!group.hostNames.includes(target.hostName)) group.hostNames.push(target.hostName);
      targetGroups.set(groupKey, group);
    });
    const exactTargetQuery = Array.from(targetGroups.values()).map((group) =>
      `(HostRoles/service_name=${group.serviceName}`
      + `&HostRoles/component_name=${group.componentName}`
      + `&HostRoles/host_name.in(${group.hostNames.join(",")})`
      + "&HostRoles/state=INIT)").join("|");
    if (intent && !exactTargetQuery) {
      throw new Error(t("installer.step9.installTargetMismatch"));
    }
    managedInstallStarted.current = true;
    const submittingIntent = intent
      ? {
        ...intent,
        state: "SUBMITTING" as const,
        submissionStartedAt: intent.submissionStartedAt || Date.now(),
      }
      : undefined;
    const submittingHandoff = submittingIntent && managedDependencyHandoffRef.current
      ? { ...managedDependencyHandoffRef.current, installIntent: submittingIntent }
      : managedDependencyHandoffRef.current;
    if (submittingIntent) {
      managedDependencyInstallIntentRef.current = submittingIntent;
      managedDependencyHandoffRef.current = submittingHandoff;
      try {
        persist(
          hostsRef.current,
          clusterStatusRef.current,
          "WAIT_FOR_PROVIDER_PREPARATION",
          {
            managedDependencyInstallIntent: submittingIntent,
            managedDependencyHandoff: submittingHandoff,
          },
        );
        await Promise.resolve(flushStateToDb(
          "checkpoint",
          -1,
          wizardCheckpoint(wizardName, "INSTALLING"),
        ));
        if (!active.current || managedDependencyScopeKeyRef.current !== capturedScope) return;
      } catch (error) {
        managedInstallStarted.current = false;
        managedDependencyInstallIntentRef.current = intent;
        managedDependencyHandoffRef.current = managedDependencyHandoff;
        throw error;
      }
    }
    const urlParams = `ServiceInfo/service_name.in(${serviceNames.join(",")})`;
    let response: any;
    try {
      response = intent
        ? await HostsApi.updateHostComponents(
          clusterName,
          exactTargetQuery,
          {
            context: "Install Services",
            HostRoles: { state: "INSTALLED" },
            level: "HOST_COMPONENT",
            query: exactTargetQuery,
          },
        )
        : await ServiceApi.updateService(
          clusterName,
          {
            context: "Install Services",
            ServiceInfo: { state: "INSTALLED" },
          },
          urlParams,
        );
    } catch (error) {
      throw error;
    }
    if (!active.current || managedDependencyScopeKeyRef.current !== capturedScope) return;
    const requestId = requestIdFrom(response);
    if (requestId == null) {
      setManagedDependencyPollError(t("installer.step9.installSubmissionUnknown"));
      setWorking(false);
      return;
    }
    if (submittingIntent) {
      const submittedIntent = {
        ...submittingIntent,
        state: "SUBMITTED" as const,
        requestId,
      };
      managedDependencyInstallIntentRef.current = submittedIntent;
      const submittedHandoff = managedDependencyHandoffRef.current
        ? { ...managedDependencyHandoffRef.current, installIntent: submittedIntent }
        : managedDependencyHandoffRef.current;
      managedDependencyHandoffRef.current = submittedHandoff;
      await setRequest(requestId, "INSTALL", "PENDING", {
        managedDependencyInstallIntent: submittedIntent,
        managedDependencyHandoff: submittedHandoff,
      });
    } else {
      await setRequest(requestId, "INSTALL", "PENDING");
    }
  };

  async function pollManagedDependencies() {
    const handoff = managedDependencyHandoffRef.current;
    const capturedScope = managedDependencyScopeKeyRef.current;
    if (!active.current || !handoff?.items.length || managedDependencyPollInFlight.current) return;
    managedDependencyPollInFlight.current = true;
    setManagedDependencyPollError("");
    try {
      const dependencyClusterName = handoff.clusterName || clusterName;
      const bindings = await Promise.all(handoff.items.map(({ bindingId }) =>
        ServiceDependenciesApi.get(dependencyClusterName, bindingId)));
      if (!active.current || managedDependencyScopeKeyRef.current !== capturedScope) return;
      bindings.forEach((binding, index) =>
        validateManagedDependencyBinding(binding, handoff.items[index], handoff));
      if (managedDependencyScopeKeyRef.current !== capturedScope) return;
      setManagedDependencyBindings(bindings);
      const intent = managedDependencyInstallIntentRef.current;
      if (intent?.state === "SUBMITTING" && intent.requestId == null) {
        await reconcileLostInstallSubmission(bindings, handoff, intent, capturedScope);
        return;
      }
      if (intent?.state === "SUBMITTED" && intent.requestId != null) {
        await setRequest(intent.requestId, "INSTALL", "PENDING", {
          managedDependencyInstallIntent: intent,
          managedDependencyHandoff: handoff,
        });
        return;
      }
      const blocked = bindings.find((binding) => !managedDependencyInstallAllowed(binding));
      if (blocked) {
        setWorking(true);
        scheduleManagedDependencyPoll(3000);
        return;
      }
      await launchManagedInstall();
    } catch (error: any) {
      if (active.current) {
        setManagedDependencyPollError(errorMessage(error));
        setWorking(false);
      }
    } finally {
      managedDependencyPollInFlight.current = false;
    }
  }

  const launchStart = async () => {
    if (supports.skipComponentStartAfterInstall) {
      await completeSuccess("START_SKIPPED");
      return;
    }
    try {
      let serviceNames = selectedServices();
      let urlParams: string;
      let context: string;
      if (wizardName === "addService") {
        if (serviceNames.includes("OOZIE")) {
          serviceNames = Array.from(new Set([
            ...serviceNames,
            "HDFS",
            "YARN",
            "MAPREDUCE2",
          ]));
        }
        urlParams = "ServiceInfo/state=INSTALLED"
          + `&ServiceInfo/service_name.in(${serviceNames.join(",")})`
          + "&params/run_smoke_test=true&params/reconfigure_client=false";
        context = "Start Added Services";
      } else {
        const skipServiceChecks = await loadSkipServiceChecks();
        urlParams = "ServiceInfo/state=INSTALLED"
          + `&params/run_smoke_test=${!skipServiceChecks}`
          + "&params/reconfigure_client=false";
        context = "Start Services";
      }
      const response = await ServiceApi.updateService(
        clusterName,
        { context, ServiceInfo: { state: "STARTED" } },
        urlParams,
      );
      if (!active.current) return;
      const requestId = requestIdFrom(response);
      if (requestId == null) {
        await completeSuccess();
        return;
      }
      await setRequest(requestId, "START", "INSTALLED");
    } catch (error: any) {
      if (active.current) await completeFailure("START", errorMessage(error));
    }
  };

  const advanceAfterSuccess = async (completedPhase: InstallationPhase) => {
    if (completedPhase === "INSTALL") {
      if (isKerberosEnabled) {
        await launchKeytabRegeneration();
      } else {
        await launchStart();
      }
    } else if (completedPhase === "KEYTABS") {
      await launchStart();
    } else if (completedPhase === "START") {
      await completeSuccess();
    }
  };

  const updateHostsFromTasks = (
    tasks: any[],
    currentPhase: InstallationPhase,
    currentRequestId: string | number,
  ) => {
    const installOnly = supports.skipComponentStartAfterInstall;
    const baseProgress = currentPhase === "INSTALL" ? 0 : 33;
    const phaseWeight = currentPhase === "INSTALL"
      ? installOnly ? 100 : 33
      : currentPhase === "START" ? 67 : 0;
    const nextHosts = hostsRef.current.map((host) => {
      const hostTasks = tasks.filter((task) => task.Tasks?.host_name === host.name);
      const mergedTasks = mergeInstallTasks(host.logTasks || [], hostTasks);
      if (!hostTasks.length) {
        return {
          ...host,
          progress: currentPhase === "KEYTABS" ? 33 : host.progress,
          message: currentPhase === "KEYTABS" ? phaseLabel(currentPhase) : host.message,
          logTasks: mergedTasks,
        };
      }
      const completed = hostTasks.filter((task) =>
        terminalTaskStatuses.has(task.Tasks?.status),
      ).length;
      const failed = hostTasks.some((task) =>
        failedTaskStatuses.has(task.Tasks?.status),
      );
      const requestComplete = completed === hostTasks.length;
      return {
        ...host,
        lastRequestId: currentRequestId,
        logTasks: mergedTasks,
        progress: Math.round(baseProgress + phaseWeight * (completed / hostTasks.length)),
        status: failed
          ? currentPhase === "INSTALL" ? "failed" : "warning"
          : requestComplete ? "success" : "in_progress",
        message: failed
          ? `${phaseLabel(currentPhase)} failed`
          : requestComplete
            ? `${phaseLabel(currentPhase)} completed`
            : phaseLabel(currentPhase),
      };
    });
    hostsRef.current = nextHosts;
    setHosts(nextHosts);
    persist(nextHosts, clusterStatusRef.current, currentPhase);
  };

  async function pollCurrentRequest() {
    const currentRequestId = requestIdRef.current;
    if (!active.current || requestInFlight.current || currentRequestId == null) return;
    requestInFlight.current = true;
    setWorking(true);
    setPollError("");
    try {
      const response = await RequestApi.getRequestStatus(
        clusterName,
        String(currentRequestId),
      );
      if (!active.current || requestIdRef.current !== currentRequestId) return;
      const currentPhase = phaseRef.current;
      if (currentPhase === "INSTALL") validateManagedInstallRequestTargets(response);
      const tasks = (response.tasks || []).map((task: any) => ({
        ...task,
        Tasks: { ...task.Tasks, request_id: currentRequestId },
      }));
      updateHostsFromTasks(tasks, currentPhase, currentRequestId);
      if (!requestFinished(response)) {
        schedulePoll(3000);
        return;
      }
      if (requestFailed(response)) {
        await completeFailure(currentPhase);
      } else {
        await advanceAfterSuccess(currentPhase);
      }
    } catch (error: any) {
      if (active.current) {
        setPollError(errorMessage(error));
        setWorking(false);
      }
    } finally {
      requestInFlight.current = false;
    }
  }

  const retryInstall = async () => {
    if (!canRetryInstallation(clusterStatusRef.current.status)) return;
    setTerminal(false);
    setWorking(true);
    setOperationError("");
    try {
      const response = await HostsApi.updateHostComponents(
        clusterName,
        "HostRoles/desired_state=INSTALLED&HostRoles/state!=INSTALLED",
        {
          context: "Retry Install Components",
          HostRoles: { state: "INSTALLED" },
          level: "HOST_COMPONENT",
          query: "HostRoles/desired_state=INSTALLED&HostRoles/state!=INSTALLED",
        },
      );
      const requestId = requestIdFrom(response);
      if (requestId == null) {
        throw new Error("Ambari did not return a retry request ID.");
      }
      await setRequest(requestId, "INSTALL", "PENDING");
    } catch (error: any) {
      await completeFailure("INSTALL", errorMessage(error));
    }
  };

  useEffect(() => {
    active.current = true;
    if (!initialTerminal
      && initialPhase === "WAIT_FOR_PROVIDER_PREPARATION"
      && managedDependencyHandoffRef.current?.items.length) {
      void pollManagedDependencies();
    } else if (!initialTerminal && requestIdRef.current != null) {
      schedulePoll();
    } else if (!initialTerminal) {
      setPollError(
        "The installation request ID is missing. Return to Review and deploy again.",
      );
      setWorking(false);
    }
    return () => {
      active.current = false;
      if (timer.current) clearTimeout(timer.current);
      if (managedDependencyTimer.current) clearTimeout(managedDependencyTimer.current);
    };
  }, []);

  useEffect(() => {
    const warnBeforeUnload = (event: BeforeUnloadEvent) => {
      if (!working) return;
      event.preventDefault();
    };
    window.addEventListener("beforeunload", warnBeforeUnload);
    return () => window.removeEventListener("beforeunload", warnBeforeUnload);
  }, [working]);

  const overallProgress = hosts.length
    ? Math.round(hosts.reduce(
      (sum, host) => sum + Number(host.progress || 0),
      0,
    ) / hosts.length)
    : terminal ? 100 : 0;
  const canContinue = canEnterSummary(wizardName, clusterStatus.status || "");

  return (
    <>
      {selectedRequestId != null ? (
        <BackgroundOperations
          isExplicitClick
          rootLevel={ViewLevel.TASKS_LIST}
          clusterName={clusterName}
          requestId={selectedRequestId}
          host={selectedHost}
          isOpen
          onClose={() => setSelectedRequestId(null)}
        />
      ) : null}

      {blocker.state === "blocked" ? (
        <Alert variant="warning" className="d-flex justify-content-between">
          <span>Installation is still running.</span>
          <span>
            <Button size="sm" variant="outline-secondary" onClick={() => blocker.reset()}>
              Stay
            </Button>{" "}
            <Button size="sm" variant="danger" onClick={() => blocker.proceed()}>
              Leave
            </Button>
          </span>
        </Alert>
      ) : null}

      <div className="step-title">Install, Start and Test</div>
      <p className="step-description mt-2">
        Ambari is installing, starting, and validating the selected services.
      </p>

      {pollError ? (
        <Alert variant="danger">
          {pollError}{" "}
          {requestIdRef.current != null ? (
            <Button size="sm" variant="outline-danger" onClick={() => schedulePoll()}>
              Retry Poll
            </Button>
          ) : null}
        </Alert>
      ) : null}
      {initialPhase === "WAIT_FOR_PROVIDER_PREPARATION"
        && managedDependencyHandoff?.items.length ? (
        <Alert variant={managedDependencyPollError ? "danger" : "info"}>
          <div>
            {managedDependencyPollError
              || (managedDependencyBindings.length
              ? managedDependencyBindings.map(managedDependencyNextAction).join("; ")
                : t("installer.step9.providerPreparationWaiting"))}
          </div>
          <Button
            className="mt-2"
            size="sm"
            variant={managedDependencyPollError ? "outline-danger" : "outline-secondary"}
            onClick={() => {
              setWorking(true);
              scheduleManagedDependencyPoll();
            }}
          >
            {t("installer.step9.reloadDependencyStatus")}
          </Button>
        </Alert>
      ) : null}
      {operationError ? <Alert variant="danger">{operationError}</Alert> : null}

      <div className="d-flex align-items-center gap-3 mt-3">
        <ProgressBar
          className="flex-grow-1"
          now={overallProgress}
          variant={clusterStatus.status?.includes("FAILED")
            ? "danger"
            : canContinue ? "success" : "info"}
        />
        <span>{overallProgress}% overall</span>
      </div>
      <div className="mt-2 text-muted d-flex align-items-center gap-2">
        {working ? <Spinner animation="border" size="sm" /> : null}
        {phase === "WAIT_FOR_PROVIDER_PREPARATION"
          ? t("installer.step9.providerPreparationWaiting")
          : phaseLabel(phase)}
      </div>

      <Table responsive hover className="mt-3 mb-5">
        <thead>
          <tr>
            <th>Host</th>
            <th>Status</th>
            <th>Progress</th>
            <th>Tasks</th>
          </tr>
        </thead>
        <tbody>
          {hosts.map((host) => (
            <tr key={host.name}>
              <td>{host.name}</td>
              <td>{host.message}</td>
              <td>{host.progress}%</td>
              <td>
                <div>{(host.logTasks || []).length} task(s)</div>
                {host.lastRequestId != null ? (
                  <Button
                    size="sm"
                    variant="link"
                    className="p-0"
                    onClick={() => {
                      setSelectedHost(host.name);
                      setSelectedRequestId(host.lastRequestId);
                    }}
                  >
                    View task logs
                  </Button>
                ) : null}
              </td>
            </tr>
          ))}
        </tbody>
      </Table>

      <WizardFooter
        isNextEnabled={canContinue}
        isBackEnabled={false}
        isCancelEnabled={!working}
        lifted
        step={currentStep}
        sideItems={canRetryInstallation(clusterStatus.status) ? (
          <Button
            variant="outline-primary"
            className="me-3"
            disabled={working}
            onClick={() => void retryInstall()}
          >
            RETRY
          </Button>
        ) : null}
        onNext={async () => {
          persist(hostsRef.current, clusterStatusRef.current, phaseRef.current);
          await Promise.resolve(flushStateToDb(
            "next",
            -1,
            wizardCheckpoint(wizardName, "INSTALLED"),
          ));
          handleNextImperitive();
        }}
        onCancel={() => {
          if (!working) return flushStateToDb("cancel");
        }}
        onBack={() => {}}
      />
    </>
  );
}

export default Step9;
