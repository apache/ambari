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

import {
  cloneDeep,
  every,
  filter,
  find,
  flatten,
  forEach,
  get,
  isEmpty,
  map,
  set,
  some,
  uniq,
} from "lodash";
import { useContext, useEffect, useRef, useState } from "react";
import LogApi from "../../api/logApi";
import { HostsApi } from "../../api/hostsApi";
import {
  commandDetail,
  getFormattedStringFromArray,
  isFinished,
  role,
} from "../../Utils/Utility";
import { ServiceApi } from "../../api/serviceApi";
import { RequestApi } from "../../api/requestApi";
import { Card, ProgressBar, Stack } from "react-bootstrap";
import Table from "../../components/Table";
import classNames from "classnames";
import usePagination from "../../hooks/usePagination";
import Paginator from "../../components/Paginator";
import BackgroundOperations from "../BackgroundOperations";
import { ProgressStatus, ViewLevel } from "../../constants";
import WizardFooter from "../../components/StepWizard/WizardFooter";
import { ActionTypes } from "./clusterStore/types";
import DefaultButton from "../../components/DefaultButton";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faUndo } from "@fortawesome/free-solid-svg-icons";
import { ContextWrapper } from ".";
import { messages } from "../messages";
import usePolling from "../../hooks/usePolling";
import { AppContext } from "../../store/context";

type Step9Props = {
  wizardName?: string;
};

function Step9({ wizardName = "clusterCreation" }: Step9Props) {
  const { Context } = useContext(ContextWrapper);
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { currentStep, handleNextImperitive },
  }: any = useContext(Context);
  const getStepData = (stepName: string, dataKey: string) => {
    const stepData = get(state, `${wizardName}Steps.${stepName}.data`, {});
    return get(stepData, dataKey, "");
  };
  const currentStepName = "INSTALL_START_TEST";
  const [hosts, setHosts] = useState(
    getStepData(currentStepName, "hosts") || []
  );
  const isPolling = useRef(getStepData(currentStepName, "isPolling") || false);
  const parseHostInfoRef = useRef(
    getStepData(currentStepName, "parseHostInfoRef") || false
  );
  const hostsWithHeartbeatLostRef = useRef<any>(
    getStepData(currentStepName, "hostsWithHeartbeatLostRef") || []
  );
  const [progress, setProgress] = useState<string>(
    getStepData(currentStepName, "progress") || "0"
  );
  const [status, setStatus] = useState(
    getStepData(currentStepName, "status") || ""
  );
  const overallProgressStatus = useRef(
    getStepData(currentStepName, "overallProgressStatus") || "0"
  );
  const [, setApiPolledData] = useState<any>();
  const [isBackgroundOperationsModalOpen, setIsBackgroundOperationsModalOpen] =
    useState(false);
  const [selectedHost, setSelectedHost] = useState("");
  const currentOpenTaskId = useRef(
    getStepData(currentStepName, "currentOpenTaskId") || 0
  );
  const logTasksChangesCounter = useRef(
    getStepData(currentStepName, "logTasksChangesCounter") || 0
  );
  const performServiceCheck = useRef(false);
  const clusterStatusRef = useRef<any>(
    getStepData(currentStepName, "clusterStatusRef") || {}
  );
  const apiPolledDataRef = useRef<any>(
    getStepData(currentStepName, "apiPolledDataRef")
  );
  const {isKerberosEnabled}=useContext(AppContext);
  const hostsRef = useRef<any>(getStepData(currentStepName, "hostsRef") || []);
  const statusRef = useRef(
    getStepData(currentStepName, "logTasksChangesCounter") || ""
  );
  const launchedStartServices = useRef(
    getStepData(currentStepName, "launchedStartServices") || false
  );
  const { stopPolling } = usePolling(
    getRequestStatus,
    3000
  );
  const requestId=useRef<string | number>("");

  const [showRetry, setShowRetry] = useState(true);
  const [selectedFilter, setSelectedFilter] = useState<{
    label: string;
    statusKey: string[];
  }>({
    label: "",
    statusKey: [],
  });
  useEffect(() => {
    setProgress(overallProgressStatus.current);
  }, [overallProgressStatus.current]);

  // Sync progress when status changes to failed
  useEffect(() => {
    if (status === "failed" && progress !== "100") {
      overallProgressStatus.current = "100";
      setProgress("100");
    }
  }, [status, progress]);
  const [clusterStatusState, setClusterStatusState] = useState<any>(
    getStepData(currentStepName, "clusterStatusState") || {}
  );

  // Monitor hosts and cluster status for retry button visibility (works for all wizard types)
  useEffect(() => {
    const clusterStatus = getClusterStatus();
    const hasFailedHosts = some(hosts, ["status", "failed"]);
    const shouldShowRetry =
      clusterStatus?.status === "INSTALL FAILED" || hasFailedHosts;
    setShowRetry(shouldShowRetry);
  }, [hosts, clusterStatusState]);

  const getClusterStatus = () => {
    if (isEmpty(clusterStatusRef.current)) {
      return getStepData("REVIEW", "clusterStatus");
    }
    return clusterStatusRef.current;
  };

  const setClusterStatus = (clusterStatus: any) => {
    if (clusterStatus.requestId) {
      clusterStatusRef.current = {
        ...clusterStatus,
        oldRequestsId: uniq([
          ...(clusterStatusRef.current.oldRequestsId || []),
          clusterStatus.requestId,
        ]),
      };
    } else {
      clusterStatusRef.current = {
        ...clusterStatusRef.current,
        ...clusterStatus,
      };
    }
    if (!isEmpty(clusterStatus)) {
      clusterStatusRef.current = {
        ...clusterStatus,
        oldRequestsId: uniq([
          ...(clusterStatusRef.current.oldRequestsId || []),
          clusterStatus.requestId,
        ]),
      };
    }
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: currentStep.name || "INSTALL_TEST__START",
        data: {
          hostInfo: hosts,
          clusterStatus: getClusterStatus(),
          hosts,
          isPolling: isPolling?.current,
          parseHostInfoRef: parseHostInfoRef?.current,
          hostsWithHeartbeatLostRef: hostsWithHeartbeatLostRef?.current,
          progress,
          status,
          overallProgressStatus: overallProgressStatus?.current,
          currentOpenTaskId: currentOpenTaskId?.current,
          logTasksChangesCounter: logTasksChangesCounter?.current,
          clusterStatusRef: clusterStatusRef?.current,
          apiPolledDataRef: apiPolledDataRef?.current,
          hostsRef: hostsRef?.current,
          statusRef: statusRef?.current,
          launchedStartService: launchedStartServices?.current,
          clusterStatusState: clusterStatus,
        },
      },
    });
  };

  const filters = [
    {
      label: "All",
      statusKey: ["in_progress", "pending", "failed", "warning", "success"],
    },
    {
      label: "In Progress",
      statusKey: ["in_progress", "pending"],
    },
    {
      label: "Warning",
      statusKey: ["warning"],
    },
    {
      label: "Success",
      statusKey: ["success"],
    },
    {
      label: "Fail",
      statusKey: ["failed"],
    },
  ];

  const {
    currentItems,
    changePage,
    currentPage,
    maxPage,
    itemsPerPage,
    setItemsPerPage,
  } = usePagination(hosts);

  useEffect(() => {
    setSelectedFilter(filters[0]);
  }, []);

  // Determine when to show retry button based on cluster status (matching Ember implementation)
  useEffect(() => {
    const clusterStatus = getClusterStatus();
    const shouldShowRetry = clusterStatus?.status === "INSTALL FAILED";
    setShowRetry(shouldShowRetry);
  }, [clusterStatusState]);

  useEffect(() => {
    setClusterStatusState(clusterStatusRef.current);
  }, [clusterStatusRef.current]);

  useEffect(() => {
    if (
      some(hosts, ["status", "failed"]) ||
      some(hosts, ["status", "heartbeat_lost"])
    ) {
      statusRef.current = "failed";
    } else if (some(hosts, ["status", "warning"])) {
      statusRef.current = "warning";
    } else if (
      progress === "100" &&
      getClusterStatus().status !== "INSTALL FAILED"
    ) {
      statusRef.current = "success";
    }
    setStatus(statusRef.current);
  }, [hostsRef.current, progress]);

  function getProgressBarColor(status: string) {
    switch (status.toLowerCase()) {
      case "pending":
      case "in_progress":
        return "info";
      case "failed":
      case "fail":
      case "failure":
      case "install failed":
        return "danger";
      case "success":
      case "installed":
        return "success";
      case "warning":
        return "warning";
      default:
        return "info";
    }
  }

  // useEffect(() => {
  //   if (hosts.length&&!logsLoadedOnce.current) {
  //     logsLoadedOnce.current=true;
  //     loadLogData(false);
  //   }
  // }, [hosts.length]);

  // Reset status and message of all hosts when retry install (matching Ember's resetHostsForRetry)
  function resetHostsForRetry() {
    const hostsCopy = cloneDeep(hostsRef.current);
    for (let host of hostsCopy) {
      host.status = "pending";
      host.message = "Waiting";
      host.progress = "0";
      host.isNoTasksForInstall = false;
      host.logTasks = [];
    }
    hostsRef.current = hostsCopy;
    setHosts(hostsCopy);
  }
  async function retry() {
    const clusterStatus = getClusterStatus();
    if (
      clusterStatus.status === "INSTALL FAILED" ||
      some(hosts, ["status", "failed"])
    ) {
      try {

        // Reset hosts for retry (matching Ember's resetHostsForRetry)
        resetHostsForRetry();

        // Reset polling flags
        isPolling.current = false;
        parseHostInfoRef.current = false;
        launchedStartServices.current = false;

        // Reset progress and status
        overallProgressStatus.current = "0";
        setProgress("0");
        statusRef.current = "info";
        setStatus("info");

        // Start installation services again
        await installServices(true);

        // Enable polling
        isPolling.current = true;

        // Start polling again
        startPolling();
      } catch (error) {
        console.error("Error during retry:", error);
        // Set failed status if retry fails
        const failedStatus = {
          status: "INSTALL FAILED",
          isInstallError: true,
          isCompleted: false,
        };
        setClusterStatus(failedStatus);
      }
    }
  }
  function hostHasClientsOnly(jsonError: any) {
    const hostsCopy = cloneDeep(hostsRef.current);
    forEach(hostsCopy, (host: any) => {
      let OnlyClients = true;
      const tasks = host.logTasks;
      forEach(tasks, (task: any) => {
        const allServiceComponents = getStepData(
          "SLAVES_AND_CLIENTS",
          "allServiceComponentsList"
        );
        const component: any = find(allServiceComponents, [
          "component_name",
          task.Tasks.role,
        ]);
        if (!(component && component?.is_client)) {
          OnlyClients = false;
        }
      });
      if (OnlyClients || jsonError) {
        set(host, "status", "success");
        set(host, "progress", "100");
      }
    });
    hostsRef.current = hostsCopy;
    setHosts(hostsCopy);
  }

  function isAddHostWizard() {
    return wizardName === "addHost";
  }
  function isAddServiceWizard() {
    return wizardName === "addService";
  }

  function getNewHosts() {
    return getStepData("HOST_STATUS", "hosts")?.filter(
      (host: any) => host.bootStatus === "REGISTERED"
    );
  }

  function startPolling() {
    //Disable Next
    //Start Polling from hook
    doPolling();
  }

  async function getRequestStatus() {
      // if (!restartCheckRef.current) pausePolling();
    const clusterName = getStepData("NAME", "clusterName");

  
      const requestStatus: any = await RequestApi.getRequestStatus(
        clusterName,
        requestId.current as string
      );
      const { Requests } = requestStatus;
      if (isFinished(Requests.request_status)) {
        if (Requests.request_status === ProgressStatus.COMPLETED) {
          stopPolling();
          launchStartServices();
        }
      }
    }
  //@ts-ignore
  async function loadDoServiceChecksFlag() {
    try {
      const data = await ServiceApi.ambariService(
        "?fields=RootServiceComponents/properties/skip.service.checks"
      );
      var properties = get(data, "RootServiceComponents.properties");
      if (properties && properties.hasOwnProperty("skip.service.checks")) {
        performServiceCheck.current =
          properties["skip.service.checks"] === "true";
      }
    } catch (err) {}
  }
  async function launchStartServices() {
    let data: any = {};
    if (isAddHostWizard()) {
      const slaves = getStepData(
        "SLAVES_AND_CLIENTS",
        "allServiceComponentsList"
      )
        .filter(
          (c: any) =>
            get(c, "is_client") === false && get(c, "is_master") === false
        )
        .map((c: any) => get(c, "component_name"));
      data = {
        context: get(messages, "requestInfo.startHostComponents"),
        HostRoles: { state: "STARTED" },
        urlParams: "",
        query: `HostRoles/component_name.in(${slaves.join(
          ","
        )})&HostRoles/state=INSTALLED&HostRoles/host_name.in(${getNewHosts()
          .map((host: any) => get(host, "name", ""))
          .join(",")})`,
      };
    } else if (isAddServiceWizard()) {
      let servicesList: string[] = map(
        filter(getStepData("SERVICES", "services"), function (service: any) {
          return !service.installed && service.selected;
        }),
        "serviceName"
      );
      if (servicesList.includes("OOZIE")) {
        servicesList = [...servicesList, "HDFS", "YARN", "MAPREDUCE2"];
      }
      data = {
        context: "Start Added Services",
        ServiceInfo: { state: "STARTED" },
        urlParams:
          "ServiceInfo/state=INSTALLED&ServiceInfo/service_name.in(" +
          servicesList.join(",") +
          ")&params/run_smoke_test=true&params/reconfigure_client=false",
      };
    } else {
      data = {
        context: "Start Services",
        ServiceInfo: { state: "STARTED" },
        urlParams: `ServiceInfo/state=INSTALLED&params/run_smoke_test=" +
            ${!performServiceCheck.current} +
            "&params/reconfigure_client=false`,
      };
    }
    const clusterName = getStepData("NAME", "clusterName");
    const updateStatus = isAddHostWizard()
      ? await HostsApi.updateHostComponents(clusterName, data.urlParams, data)
      : await ServiceApi.updateService(clusterName, data, data.urlParams);
    let clusterStatus = {};
    if (updateStatus) {
      // Handle different response structures for requestId
      let requestId;
      if (updateStatus?.data?.Requests?.id) {
        requestId = updateStatus.data.Requests.id;
      } else if (updateStatus?.Requests?.id) {
        requestId = updateStatus.Requests.id;
      } else if (updateStatus?.id) {
        requestId = updateStatus.id;
      }

      clusterStatus = {
        status: "INSTALLED",
        requestId,
        isStartError: false,
        isCompleted: false,
      };
      hostHasClientsOnly(false);
      //Save Cluster Status
      setClusterStatus(clusterStatus);
    } else {
      hostHasClientsOnly(true);
      clusterStatus = {
        status: "STARTED",
        isStartError: false,
        isCompleted: true,
      };
      setClusterStatus(clusterStatus);
      setStatus("success");
      overallProgressStatus.current = "100";
    }
    if (updateStatus) {
      startPolling();
    }
  }

  async function regenerate() {
    const clusterName = getStepData("NAME", "clusterName");
    const payload = {
      Clusters: {
        security_type: "KERBEROS",
      },
    };
    try {
      const params = "regenerate_keytabs=all";
      const requestData = await RequestApi.regenerateKeytabs(
        clusterName,
        payload,
        params
      );
      requestId.current = requestData.Requests.id;
    } catch (error) {
      console.log("Error regenerating keytabs: ", error);
    }
  }

  async function isAllComponentsInstalled() {
    try {
      const jsonData = await HostsApi.getHostStatus(
        getStepData("NAME", "clusterName")
      );
      const hostsCopy = cloneDeep(hostsRef.current);
      const clusterStatus = {
        status: "INSTALL FAILED",
        isStartError: true,
        isCompleted: false,
      };
      let usedHostWithHeartbeatLost = false;
      const hostsWithHeartbeatLost: any = [];
      const mastersData: any = getStepData("MASTERS", "mastersData");
      const allMasterServices = flatten(map(mastersData, "masterServices"));
      let usedHosts = map(
        filter(allMasterServices, ["isInstalled", false]),
        "hostName"
      );
      //Include Hosts from Client and slave components as well
      usedHosts = uniq(usedHosts);
      filter(jsonData.items, ["Hosts.host_state", "HEARTBEAT_LOST"]).forEach(
        function (host) {
          const hostComponentObj = {
            hostName: host.Hosts.host_name,
          };
          const componentsArr: any[] = [];
          host.host_components.forEach(function (_hostComponent: any) {
            const componentName = role(
              _hostComponent.HostRoles.component_name,
              false
            );
            componentsArr.push(componentName);
          });
          //@ts-expect-error
          hostComponentObj.componentNames = getFormattedStringFromArray(
            componentsArr,
            ""
          );
          hostsWithHeartbeatLost.push(hostComponentObj);
          if (
            !usedHostWithHeartbeatLost &&
            usedHosts.includes(host.Hosts.host_name)
          ) {
            usedHostWithHeartbeatLost = true;
          }
        }
      );
      hostsWithHeartbeatLostRef.current = hostsWithHeartbeatLost;
      if (usedHostWithHeartbeatLost) {
        hostsCopy.forEach(function (host: any) {
          if (find(hostsWithHeartbeatLost, ["hostName", host.name])) {
            set(host, "status", "heartbeat_lost");
          } else if (host.status !== "failed" && host.status !== "warning") {
            set(host, "message", "Install completed. Start aborted");
          }
          host.set("progress", "100");
        });
        overallProgressStatus.current = "100";
        hostsRef.current = hostsCopy;
        setHosts(hostsCopy);
        //Save Cluster Status, needs to be updated
        setClusterStatus(clusterStatus);
      } else if (getClusterStatus().status === "PENDING" && isPolling.current) {
        //Check for Skip
        if (!launchedStartServices.current) {
          launchedStartServices.current = true;
          if(isKerberosEnabled){
           await regenerate();
          }
          else{
          launchStartServices();
          }
        }
      }
    } catch (err) {
      const clusterStatus = {
        status: "INSTALL FAILED",
        isStartError: true,
        isCompleted: false,
      };
      overallProgressStatus.current = "100";
      const hostsCopy = cloneDeep(hosts);
      hostsCopy.forEach(function (host: any) {
        if (host.status !== "failed" && host.get("status") !== "warning") {
          set(host, "message", "Install completed. Start aborted");
          set(host, "progress", "100");
        }
      });
      //Save Cluster Status
      setClusterStatus(clusterStatus);
    }
  }
  async function loadCurrentTaskLog() {
    const taskId = currentOpenTaskId.current;
    const clusterStatus: any = getClusterStatus();
    const requestId = clusterStatus?.oldRequestsId?.at(-1);
    getStepData("NAME", "clusterName");
    if (taskId) {
      return;
    }
    try {
      const data = await RequestApi.getTask(
        getStepData("NAME", "clusterName"),
        requestId,
        taskId as any
      );
      if (taskId) {
        const host: any = find(hosts, ["name", data.Tasks.host_name]);
        if (host) {
          const currentTask = host.logTasks;
          if (currentTask) {
            currentTask.Tasks.stderr = data.Tasks.stderr;
            currentTask.Tasks.stdout = data.Tasks.stdout;
            currentTask.Tasks.output_log = data.Tasks.output_log;
            currentTask.Tasks.error_log = data.Tasks.error_log;
          }
        }
      }
      logTasksChangesCounter.current += 1;
    } catch (err) {
      currentOpenTaskId.current = 0;
    }
  }
  function doPolling() {
    getLogsByRequest(true);
  }
  function parseHostInfoPolling() {
    const result = parseHostInfoRef.current;
    if (!isPolling.current) {
      const clusterStatus: any = getClusterStatus();

      if (clusterStatus.status === "INSTALL FAILED") {
        isAllComponentsInstalled();
      }
      return;
    }
    if (result !== true) {
      window.setTimeout(function () {
        if (currentOpenTaskId.current) {
          loadCurrentTaskLog();
        }
        doPolling();
      }, 3000);
    }
  }
  function changeParseHostInfo(value: boolean) {
    parseHostInfoRef.current = value;
    parseHostInfoPolling();
  }
  function replacePolledData(polledData: any) {
    apiPolledDataRef.current = polledData;
    setApiPolledData(polledData);
  }
  function setLogTasksStatePerHost(tasksPerHost: any, host: any) {
    tasksPerHost.forEach(function (_task: any) {
      var task = find(host.logTasks, ["Tasks.id", _task.Tasks.id]);
      if (task) {
        task.Tasks.status = _task.Tasks.status;
        task.Tasks.start_time = _task.Tasks.start_time;
        task.Tasks.end_time = _task.Tasks.end_time;
        task.Tasks.exit_code = _task.Tasks.exit_code;
      } else {
        host.logTasks.push(_task);
      }
    });
  }
  function onSuccessPerHost(actions: any, contentHost: any) {
    let status = getClusterStatus()?.status;
    if (
      every(actions, ["Tasks.status", "COMPLETED"]) &&
      (status === "INSTALLED" || status === "STARTED")
    ) {
      set(contentHost, "status", "success");
    }
  }
  function isMasterFailed(polledData: any) {
    let result = false;
    map(
      filter(filter(polledData, ["Tasks.command", "INSTALL"]), [
        "Tasks.status",
        "FAILED",
      ]),
      "Tasks.role"
    ).forEach(function (role) {
      const slaveComponents = getStepData(
        "SLAVES_AND_CLIENTS",
        "serviceComponents"
      );
      const checkedSlaveComponents = map(
        filter(flatten(map(slaveComponents, "checkboxes")), ["checked", true]),
        "label"
      );
      if (checkedSlaveComponents.includes(role)) {
        result = true;
      }
    });
    return result;
  }
  async function installServices(isRetry = false) {
    const statusCopy = cloneDeep(clusterStatusRef.current);
    let data;
    if (isRetry) {
      data = {
        context: "Install Components",
        HostRoles: { state: "INSTALLED" },
        urlParams:
          "HostRoles/desired_state=INSTALLED&HostRoles/state!=INSTALLED",
      };
    } else {
      data = {
        context: "Install Services",
        HostRoles: { state: "INSTALLED" },
        urlParams: "ServiceInfo/state=INIT",
      };
    }
    statusCopy.status = "PENDING";
    setClusterStatus(statusCopy);
    try {
      let updateResponse;
      if (isRetry) {
        updateResponse = await HostsApi.updateHostComponents(
          getStepData("NAME", "clusterName"),
          data.urlParams,
          data
        );
      } else {
        updateResponse = await ServiceApi.updateService(
          getStepData("NAME", "clusterName"),
          data,
          data.urlParams as string
        );
      }
      // Handle different response structures
      let requestId;
      if (updateResponse?.data?.Requests?.id) {
        requestId = updateResponse.data.Requests.id;
      } else if (updateResponse?.Requests?.id) {
        requestId = updateResponse.Requests.id;
      } else if (updateResponse?.id) {
        requestId = updateResponse.id;
      } else {
        throw new Error("No requestId found in API response");
      }

      const updatedStatus = {
        status: "PENDING",
        requestId,
        isInstallError: false,
        isCompleted: false,
      };
      setClusterStatus(updatedStatus);
    } catch (err) {
      console.error("Error in installServices:", err);
      const updatedStatus = {
        status: "INSTALL FAILED",
        isInstallError: true,
        isCompleted: false,
      };
      setClusterStatus(updatedStatus);
    }
  }
  function onErrorPerHost(actions: any, contentHost: any) {
    let status = getClusterStatus()?.status;
    if (!actions) return;
    if (
      some(actions, ["Tasks.status", "FAILED"]) ||
      some(actions, ["Tasks.status", "ABORTED"]) ||
      some(actions, ["Tasks.status", "TIMEDOUT"])
    ) {
      set(contentHost, "status", "warning");
    }
    if (
      (status === "PENDING" && some(actions, ["Tasks.status", "FAILED"])) ||
      isMasterFailed(actions)
    ) {
      contentHost.status !== "heartbeat_lost"
        ? set(contentHost, "status", "failed")
        : "";
    }
  }
  function displayMessage(task: any) {
    let normalizedRole = role(task.role, false);
    /* istanbul ignore next */
    switch (task.command) {
      case "INSTALL":
        switch (task.status) {
          case "PENDING":
            return "Preparing to install " + normalizedRole;
          case "QUEUED":
            return "Waiting to install " + normalizedRole;
          case "IN_PROGRESS":
            return "Installing " + normalizedRole;
          case "COMPLETED":
            return "Successfully Installed " + normalizedRole;
          case "FAILED":
            return "Failed to install" + normalizedRole;
        }
        break;
      case "UNINSTALL":
        switch (task.status) {
          case "PENDING":
            return "Preparing to uninstall " + normalizedRole;
          case "QUEUED":
            return "Waiting to uninstall " + normalizedRole;
          case "IN_PROGRESS":
            return "Uninstalling " + normalizedRole;
          case "COMPLETED":
            return "Successfully uninstalled " + normalizedRole;
          case "FAILED":
            return "Failed to uninstall" + normalizedRole;
        }

        break;
      case "START":
        switch (task.status) {
          case "PENDING":
            return "Preparing to start " + normalizedRole;
          case "QUEUED":
            return "Waiting to start " + normalizedRole;
          case "IN_PROGRESS":
            return "Starting " + normalizedRole;
          case "COMPLETED":
            return "Started Successfully " + normalizedRole;
          case "FAILED":
            return "Failed to start" + normalizedRole;
        }
        break;
      case "STOP":
        switch (task.status) {
          case "PENDING":
            return "Preparing to stop " + normalizedRole;
          case "QUEUED":
            return "Waiting to stop " + normalizedRole;
          case "IN_PROGRESS":
            return "Stopping " + normalizedRole;
          case "COMPLETED":
            return "Successfully stopped " + normalizedRole;
          case "FAILED":
            return "Failed to stop" + normalizedRole;
        }
        break;
      //@ts-ignore
      case "CUSTOM_COMMAND":
        normalizedRole = commandDetail(
          task.command_detail,
          task.request_input,
          task.ops_display_name
        );
      case "EXECUTE":
      case "SERVICE_CHECK":
        switch (task.status) {
          case "PENDING":
            return "Preparing to execute " + normalizedRole;
          case "QUEUED":
            return "Waiting to execute " + normalizedRole;
          case "IN_PROGRESS":
            return "Executing " + normalizedRole;
          case "COMPLETED":
            return "Successfully executed " + normalizedRole;
          case "FAILED":
            return "Failed to execute" + normalizedRole;
        }
        break;
      case "ABORT":
        switch (task.status) {
          case "PENDING":
            return "Preparing to abort " + normalizedRole;
          case "QUEUED":
            return "Waiting to abort " + normalizedRole;
          case "IN_PROGRESS":
            return "Aborted " + normalizedRole;
          case "COMPLETED":
            return "Successfully aborted " + normalizedRole;
          case "FAILED":
            return "Failed to abort" + normalizedRole;
        }
        break;
    }
    return "";
  }
  function progressPerHost(actions: any, contentHost: any) {
    let progress = 0;
    let actionsPerHost = actions.length;
    let completedActions = 0;
    let queuedActions = 0;
    let inProgressActions = 0;
    let installProgressFactor = 33;
    actions.forEach(function (action: any) {
      if (
        ["COMPLETED", "FAILED", "ABORTED", "TIMEDOUT"].includes(
          action.Tasks.status
        )
      ) {
        completedActions += +[
          "COMPLETED",
          "FAILED",
          "ABORTED",
          "TIMEDOUT",
        ].includes(action.Tasks.status);
      }
      if (action.Tasks.status === "QUEUED") {
        queuedActions += +(action.Tasks.status === "QUEUED");
      }
      if (action.Tasks.status === "IN_PROGRESS") {
        inProgressActions += +(action.Tasks.status === "IN_PROGRESS");
      }
    });
    /** for the install phase (PENDING), % completed per host goes up to 33%; floor(100 / 3)
     * for the start phase (INSTALLED), % completed starts from 34%
     * when task in queued state means it's completed on 9%
     * in progress - 35%
     * completed - 100%
     */
    const clusterStatus: any = getClusterStatus();
    switch (clusterStatus.status) {
      case "PENDING":
        progress = actionsPerHost
          ? Math.ceil(
              ((queuedActions * 0.09 +
                inProgressActions * 0.35 +
                completedActions) /
                actionsPerHost) *
                installProgressFactor
            )
          : installProgressFactor;
        break;
      case "INSTALLED":
        progress = actionsPerHost
          ? 33 +
            Math.floor(
              ((queuedActions * 0.09 +
                inProgressActions * 0.35 +
                completedActions) /
                actionsPerHost) *
                67
            )
          : 100;
        break;
      default:
        progress = 100;
        break;
    }
    set(contentHost, "progress", progress.toString());
    return progress;
  }
  function onInProgressPerHost(actions: any, contentHost: any) {
    let runningAction = find(actions, ["Tasks.status", "IN_PROGRESS"]);
    if (runningAction === undefined || runningAction === null) {
      runningAction = find(actions, ["Tasks.status", "QUEUED"]);
    }
    if (runningAction === undefined || runningAction === null) {
      runningAction = find(actions, ["Tasks.status", "PENDING"]);
    }
    if (runningAction !== null && runningAction !== undefined) {
      set(contentHost, "status", "in_progress");
      set(contentHost, "message", displayMessage(runningAction.Tasks));
    }
  }
  function isSuccess(polledData: any) {
    return every(polledData, ["Tasks.status", "COMPLETED"]);
  }
  function isServicesStarted(polledData: any) {
    const clusterStatus: any = getClusterStatus();
    const requestId = clusterStatus?.oldRequestsId?.at(-1);
    let updatedClusterStatus: any = {};
    if (
      !some(polledData, ["Tasks.status", "PENDING"]) &&
      !some(polledData, ["Tasks.status", "QUEUED"]) &&
      !some(polledData, ["Tasks.status", "IN_PROGRESS"])
    ) {
      overallProgressStatus.current = "100";
      updatedClusterStatus = {
        status: "INSTALLED",
        requestId,
        isCompleted: true,
      };
      if (isSuccess(polledData)) {
        updatedClusterStatus.status = "STARTED";
      } else {
        updatedClusterStatus.status = "START FAILED"; // 'START FAILED' implies to step10 that installation was successful but start failed
      }
      //Save Cluster Status
      setClusterStatus(updatedClusterStatus);
      // this.saveInstalledHosts(this);
      return true;
    }
    return false;
  }
  function setIsServicesInstalled(polledData: any) {
    const clusterStatus: any = getClusterStatus();
    let updatedClusterStatus: any = {};
    const requestId = clusterStatus?.oldRequestsId?.at(-1);
    const inferredStatus = statusRef.current;
    if (
      !some(polledData, ["Tasks.status", "PENDING"]) &&
      !some(polledData, ["Tasks.status", "QUEUED"]) &&
      !some(polledData, ["Tasks.status", "IN_PROGRESS"])
    ) {
      updatedClusterStatus = {
        status: "PENDING",
        requestId,
        isCompleted: false,
      };
      if (inferredStatus === "failed") {
        updatedClusterStatus.status = "INSTALL FAILED";
        setClusterStatus(updatedClusterStatus);
        overallProgressStatus.current = "100";
        isPolling.current = false;
        const hostsCopy = cloneDeep(hostsRef.current);
        hostsCopy.forEach(function (host: any) {
          set(host, "progress", "100");
        });
        hostsRef.current = hostsCopy;
        setHosts(hostsCopy);
        isAllComponentsInstalled().then(function () {
          changeParseHostInfo(false);
        });
        return;
      }
      overallProgressStatus.current = "33";
      setProgress("33");
      isAllComponentsInstalled().then(function () {
        changeParseHostInfo(true);
      });
      return;
    }
    changeParseHostInfo(false);
  }
  function setFinishState(polledData: any) {
    const clusterStatus: any = getClusterStatus();
    if (clusterStatus.status === "INSTALLED") {
      changeParseHostInfo(isServicesStarted(polledData));
      return;
    } else if (clusterStatus.status === "PENDING") {
      setIsServicesInstalled(polledData);
      return;
    } else if (
      clusterStatus.status === "INSTALL FAILED" ||
      clusterStatus.status === "START FAILED" ||
      clusterStatus.status === "STARTED"
    ) {
      overallProgressStatus.current = "100";
      changeParseHostInfo(true);
      return;
    }
    changeParseHostInfo(true);
  }
  useEffect(() => {}, [hostsRef.current]);
  function setParseHostInfo(polledData: any) {
    let totalProgress = 0;
    let tasksData = polledData.tasks || [];
    const clusterStatus: any = getClusterStatus();
    const requestId = clusterStatus?.oldRequestsId?.at(-1);
    forEach(tasksData, (taskData) => {
      set(taskData, "Tasks.request_id", requestId);
    });
    if (
      polledData.Requests &&
      polledData.Requests.id &&
      polledData.Requests.id != requestId
    ) {
      // We don't want to use non-current requestId's tasks data to
      // determine the current install status.
      // Also, we don't want to keep polling if it is not the
      // current requestId.
      changeParseHostInfo(false);
      return;
    }
    replacePolledData(tasksData);
    const tasksHostMap: any = {};
    tasksData.forEach(function (task: any) {
      if (tasksHostMap[task.Tasks.host_name]) {
        tasksHostMap[task.Tasks.host_name].push(task);
      } else {
        tasksHostMap[task.Tasks.host_name] = [task];
      }
    });
    const hostsCopy = cloneDeep(hostsRef.current);
    hostsCopy.forEach((_host: any) => {
      const actionsPerHost = tasksHostMap[_host.name] || []; // retrieved from polled Data
      if (actionsPerHost.length === 0) {
        if (
          clusterStatus.status === "PENDING" ||
          clusterStatus.status === "INSTALL FAILED"
        ) {
          set(_host, "progress", "33");
          set(_host, "isNoTasksForInstall", true);
          set(_host, "status", "pending");
        }
        if (
          clusterStatus.status === "INSTALLED" ||
          clusterStatus.status === "FAILED"
        ) {
          set(_host, "progress", "100");
          set(_host, "status", "success");
        }
      } else {
        set(_host, "isNoTasksForInstall", false);
      }
      setLogTasksStatePerHost(actionsPerHost, _host);
      onSuccessPerHost(actionsPerHost, _host); // every action should be a success
      onErrorPerHost(actionsPerHost, _host); // any action should be a failure
      onInProgressPerHost(actionsPerHost, _host); // current running action for a host
      totalProgress += Number(progressPerHost(actionsPerHost, _host));
      if (
        _host.progress == 33 &&
        _host.status != "failed" &&
        _host.status != "warning"
      ) {
        set(_host, "message", "Install complete (Waiting to start)");
        set(_host, "status", "pending");
      }
      if (
        _host.progress == 100 &&
        _host.status != "failed" &&
        _host.status != "warning"
      ) {
        set(_host, "message", "Install and start completed");
        set(_host, "status", "success");
      }
      if (_host.progress == 100 && _host.status == "failed") {
        set(_host, "message", "Failed to install and start");
        set(_host, "status", "failed");
      }
      if (_host.progress == 100 && _host.status == "warning") {
        set(_host, "message", "Warnings encountered");
        set(_host, "status", "warning");
      }
    });
    logTasksChangesCounter.current += 1;
    
    // Calculate overall progress following Ember.js logic exactly
    totalProgress = Math.floor(totalProgress / hostsCopy.length);
    
    // Update progress state immediately (following Ember.js pattern)
    overallProgressStatus.current = totalProgress.toString();
    setProgress(totalProgress.toString());
    
    setFinishState(tasksData);
    hostsRef.current = hostsCopy;
    setHosts(hostsCopy);
  }
  async function getLogsByRequest(polling: boolean) {
    const clusterStatus: any = getClusterStatus();
    const requestId = clusterStatus?.oldRequestsId?.at(-1);
    try {
      const requestStatus = await LogApi.getLogData(
        getStepData("NAME", "clusterName"),
        requestId
      );
      isPolling.current = polling;
      setParseHostInfo(requestStatus);
    } catch (err) {
      loadLogData(true);
    }
  }
  function loadLogData(startPolling: boolean) {
    getLogsByRequest(startPolling);
  }
  function loadHosts(isRetry: boolean) {
    const registeredHosts = filter(getStepData("HOST_STATUS", "hosts"), [
      "bootStatus",
      "REGISTERED",
    ]);
    const hostsWithInfo = [];
    for (let host of registeredHosts) {
      const hostInfo = {
        name: host.name,
        status: "pending",
        logTasks: [],
        message: "Waiting",
        progress: 0,
        isNoTasksForInstall: false,
      };
      hostsWithInfo.push(hostInfo);
    }
    hostsRef.current = hostsWithInfo;
    setHosts(hostsWithInfo as any);
    const clusterStatus = getClusterStatus();
    if (
      clusterStatus?.status !== "INSTALL FAILED" &&
      clusterStatus !== "START FAILED" &&
      !isRetry
    ) {
      isPolling.current = true;
    }
    loadLogData(isPolling.current);
  }
  let loadStep = (isRetry = false) => {
    loadHosts(isRetry);
  };
  useEffect(() => {
    setHosts(hostsRef.current);
  }, [hostsRef.current]);

  useEffect(() => {
    loadStep();
  }, []);
  const columns = [
    {
      header: "Host",
      accessorKey: "name",
    },
    {
      header: "Status",
      id: "status",
      cell: (info: any) => {
        return (
          <Stack direction="horizontal">
            <ProgressBar
              className="w-100"
              striped
              now={info.row.original.progress}
              variant={getProgressBarColor(info.row.original.status)}
            />
            <div className="ms-2 text-nowrap">
              {info.row.original.progress}%
            </div>
          </Stack>
        );
      },
    },
    {
      header: "Message",
      id: "message",
      cell: ({ row: { original: data } }: any) => {
        return (
          <div
            className={data.progress == 100 ? "" : "custom-link"}
            onClick={() => {
              if (data.progress != 100) {
                setIsBackgroundOperationsModalOpen(true);
                setSelectedHost(data.name);
              }
            }}
          >
            {data.message}
          </div>
        );
      },
    },
  ];

  function getClusterBarColor() {
    if (some(hosts, ["status", "pending"])) {
      return "info";
    } else if (some(hosts, ["status", "warning"])) {
      return "warning";
    } else if (some(hosts, ["status", "failed"])) {
      return "danger";
    } else if (
      every(hosts, ["status", "success"]) &&
      getClusterStatus().status === "STARTED"
    ) {
      return "success";
    }
    return "info";
  }

  return (
    <>
      {isBackgroundOperationsModalOpen ? (
        <BackgroundOperations
          isExplicitClick
          rootLevel={ViewLevel.TASKS_LIST}
          clusterName={getStepData("NAME", "clusterName")}
          requestId={getClusterStatus().requestId}
          host={selectedHost}
          isOpen={isBackgroundOperationsModalOpen}
          onClose={() => {
            setIsBackgroundOperationsModalOpen(false);
          }}
        />
      ) : null}
      <div>
        <div className="step-title">Install, Start and Test</div>
        <div className="d-flex flex-column">
          <small className="light-text">
            Please wait while the selected services are installed and started.
          </small>
        </div>
        <Card className="mt-4 p-2">
          <Stack direction="horizontal">
            <ProgressBar
              className="w-100"
              now={progress as any}
              variant={getClusterBarColor()}
            />
            <div className="text-nowrap ms-3">{progress}% overall</div>
          </Stack>
          <div className="d-flex justify-content-between mt-2">
            <div></div>
            <div
              style={{ fontSize: 12 }}
              className="my-2 align-items-center d-flex justify-content-center"
            >
              Show :
              {filters.map((hostFilter: any) => {
                return (
                  <Stack direction="horizontal">
                    <div
                      key={hostFilter.label}
                      onClick={() => {
                        setSelectedFilter(hostFilter);
                      }}
                      className={classNames("ms-1 p-1", {
                        "bg-disabled":
                          selectedFilter.label === hostFilter.label,
                        rounded: selectedFilter.label === hostFilter.label,
                        "text-white": selectedFilter.label === hostFilter.label,
                        "custom-link":
                          selectedFilter.label !== hostFilter.label,
                      })}
                    >
                      {hostFilter.label}(
                      {
                        hosts.filter((host: any) => {
                          return hostFilter.statusKey.includes(host.status);
                        }).length
                      }
                      )
                    </div>
                    <div className="text-muted mx-2">|</div>
                  </Stack>
                );
              })}
            </div>
            <div className="d-flex gap-2 flex-wrap">
              {showRetry ? (
                <DefaultButton
                  onClick={() => {
                    retry();
                  }}
                >
                  <FontAwesomeIcon icon={faUndo} className="me-2" />
                  RETRY
                </DefaultButton>
              ) : null}
            </div>
          </div>
          <Table
            columns={columns}
            data={currentItems.filter((host: any) => {
              return selectedFilter?.statusKey?.includes(host.status);
            })}
          />
          <Paginator
            currentPage={currentPage}
            maxPage={maxPage}
            changePage={changePage}
            itemsPerPage={itemsPerPage}
            setItemsPerPage={setItemsPerPage}
            totalItems={hosts.length}
          />
        </Card>
        <WizardFooter
          isNextEnabled={true}
          step={currentStep}
          onNext={() => {
            dispatch({
              type: ActionTypes.STORE_INFORMATION,
              payload: {
                step: currentStep.name || "INSTALL_TEST__START",
                data: {
                  hostInfo: hosts,
                  clusterStatus: getClusterStatus(),
                },
              },
            });
            flushStateToDb("next");
            handleNextImperitive();
          }}
          onBack={() => {}}
        />
      </div>
    </>
  );
}
export default Step9;
