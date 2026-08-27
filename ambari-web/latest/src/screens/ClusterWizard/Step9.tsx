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
import { ActionTypes } from "./clusterStore/types";
import {
  canEnterSummary,
  canRetryInstallation,
  failedTaskStatuses,
  InstallWizardName,
  InstallationPhase,
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
    case "INSTALL": return "Installing services and components";
    case "KEYTABS": return "Regenerating Kerberos keytabs";
    case "START": return "Starting services and running service checks";
    default: return "Deployment complete";
  }
}

function Step9({ wizardName = "clusterCreation" }: Step9Props) {
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
  const initialStatus = restoredInstall.clusterStatus || reviewStatus;
  const initialPhase: InstallationPhase = restoredInstall.phase
    || initialStatus.phase
    || (initialStatus.status === "STARTED" ? "COMPLETE" : "INSTALL");
  const initialTerminal = canEnterSummary(wizardName, initialStatus.status || "");
  const registeredHosts = (getStepData("HOST_STATUS", "hosts") || [])
    .filter((host: any) => host.bootStatus === "REGISTERED");
  const initialHosts = restoredInstall.hostInfo?.length
    ? restoredInstall.hostInfo
    : registeredHosts.map((host: any) => ({
      name: host.name,
      status: initialStatus.status === "STARTED" ? "success" : "pending",
      progress: initialStatus.status === "STARTED" ? 100 : 0,
      message: initialStatus.status === "STARTED"
        ? "Install and start completed"
        : "Waiting",
      logTasks: [],
    }));

  const [hosts, setHosts] = useState<any[]>(initialHosts);
  const [clusterStatus, setClusterStatus] = useState<any>(initialStatus);
  const [phase, setPhase] = useState<InstallationPhase>(initialPhase);
  const [working, setWorking] = useState(!initialTerminal);
  const [terminal, setTerminal] = useState(initialTerminal);
  const [pollError, setPollError] = useState("");
  const [operationError, setOperationError] = useState(
    initialStatus.operationError || "",
  );
  const [selectedHost, setSelectedHost] = useState("");
  const [selectedRequestId, setSelectedRequestId] = useState<
    string | number | null
  >(null);

  const active = useRef(false);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const requestInFlight = useRef(false);
  const hostsRef = useRef<any[]>(initialHosts);
  const clusterStatusRef = useRef<any>(initialStatus);
  const phaseRef = useRef<InstallationPhase>(initialPhase);
  const requestIdRef = useRef<string | number | undefined>(initialStatus.requestId);

  const blocker = useBlocker(({ currentLocation, nextLocation }) =>
    working && currentLocation.pathname !== nextLocation.pathname,
  );

  const persist = (
    nextHosts: any[],
    nextStatus: any,
    nextPhase: InstallationPhase,
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
    if (!initialTerminal && requestIdRef.current != null) {
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
