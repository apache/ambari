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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { useContext, useEffect, useRef, useState } from "react";
import { Alert, Button, ProgressBar, Spinner, Table } from "react-bootstrap";
import { get } from "lodash";
import { AppContext } from "../../../store/context";
import { ContextWrapper } from "../../ClusterWizard";
import WizardFooter from "../../../components/StepWizard/WizardFooter";
import BackgroundOperations from "../../BackgroundOperations";
import { ViewLevel } from "../../../constants";
import { RequestApi } from "../../../api/requestApi";
import { HostsApi } from "../../../api/hostsApi";
import {
  buildAddHostComponentAssignments,
  AddHostComponentMetadata,
} from "../../../Utils/hostWizard";
import { ActionTypes } from "./wizardDataStore/types";

type DeploymentPhase = "INSTALL" | "KEYTABS" | "START" | "COMPLETE";

const terminalRequestStatuses = new Set([
  "ABORTED",
  "COMPLETED",
  "FAILED",
  "TIMEDOUT",
]);
const failedRequestStatuses = new Set(["ABORTED", "FAILED", "TIMEDOUT"]);
const terminalTaskStatuses = new Set(["ABORTED", "COMPLETED", "FAILED", "TIMEDOUT"]);
const failedTaskStatuses = new Set(["ABORTED", "FAILED", "TIMEDOUT"]);

function requestIdFrom(response: any): string | number | undefined {
  return response?.Requests?.id ?? response?.data?.Requests?.id ?? response?.id;
}

function requestError(error: any) {
  return error?.response?.data?.message
    || error?.response?.data
    || error?.message
    || "Ambari could not continue the Add Host deployment.";
}

function mergeTasks(existing: any[], incoming: any[]) {
  const tasks = new Map(existing.map((task) => [
    `${task.Tasks?.request_id || ""}:${task.Tasks?.id}`,
    task,
  ]));
  incoming.forEach((task) => {
    tasks.set(`${task.Tasks?.request_id || ""}:${task.Tasks?.id}`, task);
  });
  return [...tasks.values()];
}

function phaseLabel(phase: DeploymentPhase) {
  switch (phase) {
    case "INSTALL": return "Installing components";
    case "KEYTABS": return "Regenerating Kerberos keytabs";
    case "START": return "Starting components";
    default: return "Deployment complete";
  }
}

export default function AddHostInstall() {
  const { clusterName: contextClusterName, isKerberosEnabled } = useContext(AppContext);
  const { Context } = useContext(ContextWrapper);
  const {
    dispatch,
    flushStateToDb,
    state,
    stepWizardUtilities: { currentStep, handleNextImperitive },
  }: any = useContext(Context);

  const clusterName = get(
    state,
    "addHostSteps.NAME.data.clusterName",
    contextClusterName,
  );
  const reviewStatus = get(state, "addHostSteps.REVIEW.data.clusterStatus", {});
  const restoredInstall = get(state, "addHostSteps.INSTALL_START_TEST.data", {});
  const initialStatus = restoredInstall.clusterStatus || reviewStatus;
  const registeredHosts = get(
    state,
    "addHostSteps.HOST_STATUS.data.hosts",
    [],
  ).filter((host: any) => host.bootStatus === "REGISTERED");
  const assignments = get(
    state,
    "addHostSteps.SLAVES_AND_CLIENTS.data.serviceComponents",
    [],
  );
  const componentMetadata: AddHostComponentMetadata[] = get(
    state,
    "addHostSteps.SLAVES_AND_CLIENTS.data.allServiceComponentsList",
    [],
  );
  const componentAssignments = buildAddHostComponentAssignments(
    assignments,
    componentMetadata,
  );
  const metadataByComponent = Object.fromEntries(componentMetadata.map((component) => [
    component.component_name || "",
    component,
  ]));
  const startableComponents = Object.keys(componentAssignments).filter((componentName) => {
    const metadata = metadataByComponent[componentName];
    return metadata && metadata.is_client !== true && metadata.component_category !== "CLIENT";
  });

  const initialTerminal = ["STARTED", "INSTALL FAILED", "START FAILED"].includes(
    initialStatus.status,
  );
  const initialHosts = restoredInstall.hostInfo?.length
    ? restoredInstall.hostInfo
    : registeredHosts.map((host: any) => ({
      name: host.name,
      status: initialStatus.status === "STARTED" ? "success" : "pending",
      progress: initialStatus.status === "STARTED" ? 100 : 0,
      message: initialStatus.status === "STARTED" ? "Install and start completed" : "Waiting",
      logTasks: [],
    }));

  const [hosts, setHosts] = useState<any[]>(initialHosts);
  const [clusterStatus, setClusterStatus] = useState<any>(initialStatus);
  const [phase, setPhase] = useState<DeploymentPhase>(initialStatus.phase || "INSTALL");
  const [working, setWorking] = useState(!initialTerminal);
  const [terminal, setTerminal] = useState(initialTerminal);
  const [pollError, setPollError] = useState("");
  const [operationError, setOperationError] = useState(initialStatus.operationError || "");
  const [selectedHost, setSelectedHost] = useState("");
  const [selectedRequestId, setSelectedRequestId] = useState<string | number | null>(null);

  const active = useRef(false);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const requestInFlight = useRef(false);
  const hostsRef = useRef<any[]>(initialHosts);
  const clusterStatusRef = useRef<any>(initialStatus);
  const phaseRef = useRef<DeploymentPhase>(initialStatus.phase || "INSTALL");
  const requestIdRef = useRef<string | number | undefined>(initialStatus.requestId);

  const persist = (
    nextHosts: any[],
    nextStatus: any,
    nextPhase: DeploymentPhase,
  ) => {
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: "INSTALL_START_TEST",
        data: {
          hostInfo: nextHosts,
          clusterStatus: nextStatus,
          phase: nextPhase,
        },
      },
    });
  };

  const updateDeploymentState = (
    nextStatus: any,
    nextPhase: DeploymentPhase,
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
    if (timer.current) clearTimeout(timer.current);
    timer.current = setTimeout(() => void pollCurrentRequest(), delay);
  };

  const setRequest = (
    requestId: string | number,
    nextPhase: DeploymentPhase,
    status: string,
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
    schedulePoll();
  };

  const completeSuccess = () => {
    const completedHosts = hostsRef.current.map((host) => ({
      ...host,
      status: host.status === "failed" ? "warning" : "success",
      progress: 100,
      message: host.status === "failed"
        ? "Deployment completed with warnings"
        : "Install and start completed",
    }));
    const nextStatus = {
      ...clusterStatusRef.current,
      status: "STARTED",
      isCompleted: true,
      phase: "COMPLETE",
      operationError: "",
    };
    updateDeploymentState(nextStatus, "COMPLETE", completedHosts);
    setWorking(false);
    setTerminal(true);
  };

  const completeFailure = (failedPhase: DeploymentPhase, message = "") => {
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
      setRequest(requestId, "KEYTABS", "INSTALLED");
    } catch (error: any) {
      if (active.current) completeFailure("KEYTABS", String(requestError(error)));
    }
  };

  const launchStart = async () => {
    if (!startableComponents.length) {
      completeSuccess();
      return;
    }
    try {
      const response = await HostsApi.updateHostComponents(clusterName, "", {
        context: "Start Host Components",
        HostRoles: { state: "STARTED" },
        level: "HOST_COMPONENT",
        query: `HostRoles/component_name.in(${startableComponents.join(",")})`
          + `&HostRoles/state=INSTALLED&HostRoles/host_name.in(${registeredHosts
            .map((host: any) => host.name)
            .join(",")})`,
      });
      if (!active.current) return;
      const requestId = requestIdFrom(response);
      if (requestId == null) {
        completeSuccess();
        return;
      }
      setRequest(requestId, "START", "INSTALLED");
    } catch (error: any) {
      if (active.current) completeFailure("START", String(requestError(error)));
    }
  };

  const advanceAfterSuccess = async (completedPhase: DeploymentPhase) => {
    if (completedPhase === "INSTALL") {
      if (isKerberosEnabled) {
        await launchKeytabRegeneration();
      } else {
        await launchStart();
      }
    } else if (completedPhase === "KEYTABS") {
      await launchStart();
    } else if (completedPhase === "START") {
      completeSuccess();
    }
  };

  const updateHostsFromTasks = (
    tasks: any[],
    currentPhase: DeploymentPhase,
    currentRequestId: string | number,
  ) => {
    const baseProgress = currentPhase === "INSTALL" ? 0 : 50;
    const phaseWeight = currentPhase === "INSTALL" ? 50 : currentPhase === "START" ? 50 : 0;
    const nextHosts = hostsRef.current.map((host) => {
      const hostTasks = tasks.filter((task) => task.Tasks?.host_name === host.name);
      const mergedHostTasks = mergeTasks(host.logTasks || [], hostTasks);
      if (!hostTasks.length) {
        return {
          ...host,
          progress: currentPhase === "KEYTABS" ? 50 : host.progress,
          message: currentPhase === "KEYTABS" ? phaseLabel(currentPhase) : host.message,
          logTasks: mergedHostTasks,
        };
      }
      const completed = hostTasks.filter((task) =>
        terminalTaskStatuses.has(task.Tasks?.status),
      ).length;
      const failed = hostTasks.some((task) => failedTaskStatuses.has(task.Tasks?.status));
      const requestComplete = completed === hostTasks.length;
      return {
        ...host,
        lastRequestId: currentRequestId,
        logTasks: mergedHostTasks,
        progress: Math.round(baseProgress + phaseWeight * (completed / hostTasks.length)),
        status: failed ? "failed" : requestComplete ? "success" : "in_progress",
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
      if (!active.current) return;
      const currentPhase = phaseRef.current;
      const tasks = (response.tasks || []).map((task: any) => ({
        ...task,
        Tasks: { ...task.Tasks, request_id: currentRequestId },
      }));
      updateHostsFromTasks(tasks, currentPhase, currentRequestId);
      const requestStatus = response.Requests?.request_status;
      const finished = terminalRequestStatuses.has(requestStatus)
        || (tasks.length > 0 && tasks.every((task: any) =>
          terminalTaskStatuses.has(task.Tasks?.status),
        ));
      if (!finished) {
        schedulePoll(3000);
        return;
      }
      const failed = failedRequestStatuses.has(requestStatus)
        || tasks.some((task: any) => failedTaskStatuses.has(task.Tasks?.status));
      if (failed) {
        completeFailure(currentPhase);
      } else {
        await advanceAfterSuccess(currentPhase);
      }
    } catch (error: any) {
      if (active.current) {
        setPollError(String(requestError(error)));
        setWorking(false);
      }
    } finally {
      requestInFlight.current = false;
    }
  }

  const retryFailedOperation = async () => {
    const failedPhase: DeploymentPhase = clusterStatusRef.current.failedPhase
      || phaseRef.current;
    setTerminal(false);
    setWorking(true);
    setOperationError("");
    if (failedPhase === "INSTALL") {
      try {
        const response = await HostsApi.updateHostComponents(
          clusterName,
          "HostRoles/desired_state=INSTALLED&HostRoles/state!=INSTALLED",
          {
            context: "Retry Install Components",
            HostRoles: { state: "INSTALLED" },
            level: "HOST_COMPONENT",
            query: `HostRoles/host_name.in(${registeredHosts
              .map((host: any) => host.name)
              .join(",")})`,
          },
        );
        const requestId = requestIdFrom(response);
        if (requestId == null) throw new Error("Ambari did not return a retry request ID.");
        setRequest(requestId, "INSTALL", "PENDING");
      } catch (error: any) {
        completeFailure("INSTALL", String(requestError(error)));
      }
    } else if (failedPhase === "KEYTABS") {
      await launchKeytabRegeneration();
    } else {
      await launchStart();
    }
  };

  useEffect(() => {
    active.current = true;
    if (!initialTerminal && requestIdRef.current != null) {
      schedulePoll();
    } else if (!initialTerminal) {
      setPollError("The installation request ID is missing. Return to Review and deploy again.");
      setWorking(false);
    }
    return () => {
      active.current = false;
      if (timer.current) clearTimeout(timer.current);
    };
  }, []);

  const overallProgress = hosts.length
    ? Math.round(hosts.reduce((sum, host) => sum + Number(host.progress || 0), 0) / hosts.length)
    : terminal ? 100 : 0;

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

      <div className="step-title">Install, Start and Test</div>
      <p className="step-description mt-2">
        Ambari is installing and starting components on the new hosts.
      </p>

      {pollError ? (
        <Alert variant="danger">
          {pollError}{" "}
          <Button size="sm" variant="outline-danger" onClick={() => schedulePoll()}>
            Retry Poll
          </Button>
        </Alert>
      ) : null}
      {operationError ? <Alert variant="danger">{operationError}</Alert> : null}

      <div className="d-flex align-items-center gap-3 mt-3">
        <ProgressBar
          className="flex-grow-1"
          now={overallProgress}
          variant={clusterStatus.status?.includes("FAILED") ? "danger" : terminal ? "success" : "info"}
        />
        <span>{overallProgress}% overall</span>
      </div>
      <div className="mt-2 text-muted d-flex align-items-center gap-2">
        {working ? <Spinner animation="border" size="sm" /> : null}
        {phaseLabel(phase)}
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
        isNextEnabled={terminal}
        isBackEnabled={false}
        lifted
        step={currentStep}
        sideItems={clusterStatus.status?.includes("FAILED") ? (
          <Button
            variant="outline-primary"
            className="me-3"
            disabled={working}
            onClick={() => void retryFailedOperation()}
          >
            RETRY
          </Button>
        ) : null}
        onNext={async () => {
          persist(hostsRef.current, clusterStatusRef.current, phaseRef.current);
          await Promise.resolve(flushStateToDb("next"));
          handleNextImperitive();
        }}
        onCancel={() => void flushStateToDb("cancel")}
        onBack={() => {}}
      />
    </>
  );
}
