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
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { useNavigate, useParams } from "react-router-dom";
import { HostsApi } from "../../api/hostsApi";
import { Alert, Button, Card, Dropdown } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faCheckCircle,
  faCog,
  faEllipsis,
  faMedkit,
  faMinusCircle,
  faPencil,
  faPlus,
  faQuestionCircle,
  faRefresh,
  faWarning,
} from "@fortawesome/free-solid-svg-icons";
import { get, isEmpty, startCase, uniq } from "lodash";
import Modal from "../../components/Modal";
import Table from "../../components/Table";
import { ComponentStatus, ComponentType, PassiveStateOnFilters } from "./enums";
import Tooltip from "../../components/Tooltip";
import { getCmponentsToBeRestarted } from "./HostsList";
import SelectTimeRangeModal from "../../components/SelectTimeRangeModal";
import {
  formatDate,
  getCurrTimeInSec,
  translate,
  translateWithVariables,
} from "../../Utils/Utility";
import { durationMap } from "../../components/constants";
import { Link } from "react-router-dom";
import {
  apiDataToHostComponentModel,
  getClientCustomCommands,
  getClusterComponentsCount,
  getComponentDisplayName,
  getComponentName,
  getCustomCommands,
  isActive,
  isAddableToHost,
  isDeletable,
  isDeleteComponentDisabled,
  isEnableHiveInteractive,
  isInit,
  isMoveComponentDisabled,
  isOozieServerAddable,
  isReassignable,
  isRefreshConfigsAllowed,
  isRestartable,
  isRestartComponentDisabled,
  isStart,
  maxToInstall,
} from "./utils";
import {
  checkNnLastCheckpointTime,
  decommission,
  recommission,
  restartAllStaleConfigComponents,
  restartComponent,
  startComponent,
  stopComponent,
  executeCustomCommand,
  installClients,
} from "./actions";
import { AppContext } from "../../store/context";
import IHost from "../../models/host";
import { IHostStackVersion } from "../../models/hostStackVersion";
import { IHostComponent } from "../../models/hostComponent";
import Spinner from "../../components/Spinner";
import modalManager from "../../store/ModalManager";
import SetRackInfoModal from "./SetRackInfoModal";
import {
  useDecommissionable,
  decommissionableComponents,
} from "../../hooks/useDecommissionable";
import { ServiceContext } from "../../store/ServiceContext";
import { HostMetrics } from "./HostMetrics";
import { hostMetricsOption } from "./constants";
import usePolling from "../../hooks/usePolling";
import classNames from "classnames";
import { useAuth } from "../../hooks/useAuth";

type HostSummaryProps = {
  allHostModels: IHost[];
  setAllHostModels: (
    data: IHost[] | ((prevModels: IHost[]) => IHost[])
  ) => void;
  clusterComponents: any;
};

export default function HostsSummary({
  allHostModels,
  setAllHostModels,
  clusterComponents,
}: HostSummaryProps) {
  const { clusterName, serviceComponentInfo, services, upgradeIsRunning, upgradeSuspended } =
    useContext(AppContext);
  const { allServiceModels: serviceModels } = useContext(ServiceContext);
  const params = useParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [metricsData, setMetricsData] = useState({});
  const [showSelectTimeModal, setShowSelectTimeModal] = useState(false);
  const [showConfirmationModal, setShowConfirmationModal] = useState(false);
  const [openDropdownId, setOpenDropdownId] = useState<string | null>(null);

  //Note:- Below states should be part of the context
  const [allComponents, setAllComponents] = useState<IHostComponent[]>([]);
  const [addableComponents, setAddableComponents] = useState<any[]>([]);

  const [summary, setSummary] = useState({
    Hostname: "",
    "IP Address": "",
    Rack: "",
    OS: "",
    "Cores (CPU)": "",
    Disk: "",
    Memory: "",
    "Load Avg": "",
    Heartbeat: "",
    "Current Version": "",
    "JCE Unlimited": "",
  });
  const [selectedMetricsOption, setSelectedMetricsOption] = useState(
    hostMetricsOption[0]
  );

  const selectedActionData = useRef({
    component: {},
    action: "",
    data: {},
    isCustom: false,
    successCallback: (_component: any, _data: any): any => {
      return -1;
    },
  });

  const populateHostMetricesData = () => {
    if (!selectedMetricsOption.toUpperCase().startsWith("CUSTOM")) {
      const duration = selectedMetricsOption.split(" ").slice(1).join(" ");
      const currTime = getCurrTimeInSec();
      const startTime = currTime - durationMap[duration];
      getHostMetrics(startTime, currTime);
    }
  };

  const { pausePolling, resumePolling } = usePolling(
    populateHostMetricesData,
    15000
  );

  useEffect(() => {
    if (!selectedMetricsOption.toUpperCase().startsWith("CUSTOM")) {
      populateHostMetricesData();
      resumePolling();
    }
  }, [selectedMetricsOption]);

  useEffect(() => {
    if (!isEmpty(serviceComponentInfo)) {
      let allComponentsCopy: any[] = [];
      get(serviceComponentInfo, "items", []).forEach((service: any) => {
        allComponentsCopy = allComponentsCopy.concat(
          get(service, "components", []).map((component: any) => {
            return {
              HostRoles: {
                ...get(component, "StackServiceComponents"),
                dependencies: get(component, "dependencies", []),
              },
            };
          })
        );
      });
      setAllComponents(apiDataToHostComponentModel(allComponentsCopy));
    }
  }, [serviceComponentInfo]);

  useEffect(() => {
    if (!isEmpty(allComponents)) {
      getAddableComponents();
    }
  }, [allComponents, allHostModels, clusterComponents]);

  useEffect(() => {
    if (!isEmpty(allHostModels)) {
      let tempSummary: any = {};
      const host = get(allHostModels, "[0]");
      tempSummary.Hostname = get(host, "hostName", "");
      tempSummary["IP Address"] = get(host, "ip", "");
      tempSummary.Rack = get(host, "rack", "");
      tempSummary.OS =
        get(host, "osType", "") + "(" + get(host, "osArch", "") + ")";
      tempSummary["Cores (CPU)"] = host.coresFormatted();
      tempSummary.Disk = get(host, "diskFree", "Data Unavailable");
      tempSummary.Memory = host.memoryFormatted();
      tempSummary["Load Avg"] = get(host, "loadOne", "");
      tempSummary.Heartbeat = get(host, "lastHeartBeatTime", "")
        ? "less than a minute ago"
        : "";
      tempSummary["Current Version"] = getCurrentVersion(host);
      tempSummary["JCE Unlimited"] = get(host, "hasJcePolicy", true)
        ? "true"
        : "false";
      setSummary(tempSummary);
    }
  }, [allHostModels]);

  useEffect(() => {
    if (!isEmpty(clusterComponents) && summary.Hostname) {
      setLoading(false);
    }
  }, [clusterComponents, summary]);

  const { decommissionable, isComponentDecommissionDisable } =
    useDecommissionable(get(allHostModels, "[0]", {} as IHost));

  // Authorization hooks - implementing Ember.js host component authorization patterns
  const { hasAuthorization } = useAuth();

  // Use computed upgrade properties instead of utility function
  const isUpgradeInProgress = upgradeIsRunning && !upgradeSuspended;

  // Check specific authorizations for host component operations
  const canStartStopServices = hasAuthorization("SERVICE.START_STOP");
  const canAddDeleteServices = hasAuthorization("HOST.ADD_DELETE_COMPONENTS");
  const canModifyConfigs = hasAuthorization("SERVICE.MODIFY_CONFIGS");
  const canManageHostComponents = hasAuthorization(
    "HOST.ADD_DELETE_COMPONENTS"
  );
  const canMoveComponents = hasAuthorization("SERVICE.MOVE");

  const canPerformActions =
    canStartStopServices ||
    canAddDeleteServices ||
    canModifyConfigs ||
    canManageHostComponents;

  const getHostMetrics = async (startTime: number, endTime: number) => {
    // Use a unique cache-busting parameter that includes the time range
    const cacheBuster = `${startTime}_${endTime}_${getCurrTimeInSec()}`;
    const response = await HostsApi.getHostData(
      clusterName,
      get(params, "hostname", ""),
      `metrics/cpu/cpu_user[${startTime},${endTime},15],metrics/cpu/cpu_wio[${startTime},${endTime},15],metrics/cpu/cpu_nice[${startTime},${endTime},15],metrics/cpu/cpu_aidle[${startTime},${endTime},15],metrics/cpu/cpu_system[${startTime},${endTime},15],metrics/cpu/cpu_idle[${startTime},${endTime},15],metrics/disk/disk_total[${startTime},${endTime},15],metrics/disk/disk_free[${startTime},${endTime},15],metrics/load/load_fifteen[${startTime},${endTime},15],metrics/load/load_one[${startTime},${endTime},15],metrics/load/load_five[${startTime},${endTime},15],metrics/memory/swap_free[${startTime},${endTime},15],metrics/memory/mem_shared[${startTime},${endTime},15],metrics/memory/mem_free[${startTime},${endTime},15],metrics/memory/mem_cached[${startTime},${endTime},15],metrics/memory/mem_buffers[${startTime},${endTime},15],metrics/network/bytes_in[${startTime},${endTime},15],metrics/network/bytes_out[${startTime},${endTime},15],metrics/network/pkts_in[${startTime},${endTime},15],metrics/network/pkts_out[${startTime},${endTime},15],metrics/process/proc_total[${startTime},${endTime},15],metrics/process/proc_run[${startTime},${endTime},15]&_=${cacheBuster}`
    );
    setMetricsData(response);
  };
  //@ts-ignore
  const getClusterHosts = () => {
    let hosts: string[] = [];
    get(clusterComponents, "items", []).forEach((component: any) => {
      get(component, "host_components", []).forEach((host: any) => {
        hosts.push(get(host, "HostRoles.host_name", ""));
      });
    });
    return uniq(hosts);
  };

  const hasCardinalityConflict = (component: IHostComponent) => {
    const totalCount = get(
      getClusterComponentsCount(clusterComponents),
      getComponentName(component),
      0
    );
    const maxCount = maxToInstall(component);
    return !(totalCount < maxCount);
  };

  const getAddableComponents = () => {
    let components: any[] = [];
    const installedComponents = get(
      allHostModels,
      "[0].hostComponents",
      []
    ).map((component) => getComponentName(component));
    let installedServices: any[] = [];
    get(clusterComponents, "items", []).forEach((component: any) => {
      installedServices.push(
        get(component, "ServiceComponentInfo.service_name", "")
      );
    });
    installedServices = uniq(installedServices);
    const addableToHostComponents = allComponents.filter((component) =>
      isAddableToHost(component, serviceModels)
    );

    addableToHostComponents.forEach((component) => {
      if (
        installedServices.includes(get(component, "serviceName", "")) &&
        !installedComponents.includes(getComponentName(component)) &&
        !hasCardinalityConflict(component)
      ) {
        if (
          (getComponentName(component) === "OOZIE_SERVER" &&
            !isOozieServerAddable()) ||
          (getComponentName(component) === "HIVE_SERVER_INTERACTIVE" &&
            !isEnableHiveInteractive())
        ) {
          return;
        }
        components.push({
          component_name: getComponentName(component),
          service_name: get(component, "serviceName", ""),
          display_name: get(component, "displayName", ""),
          component_category: get(component, "componentCategory", ""),
        });
      }
    });
    setAddableComponents(components);
  };

  const getCurrentVersion = (hostData: IHost) => {
    const stackVersions = get(hostData, "stackVersions", []);
    const currentVersions = stackVersions.filter(
      (version: IHostStackVersion) => get(version, "status") === "CURRENT"
    );
    return get(currentVersions, "[0].repoVersion", "");
  };

  const getStateIcon = (component: IHostComponent) => {
    const state = get(component, "workStatus", "");
    const type = get(component, "componentCategory", "");
    const adminState = get(component, "adminState", "");
    if (adminState === "DECOMMISSIONED") {
      return (
        <Tooltip message="Decommissioned">
          <FontAwesomeIcon icon={faMinusCircle} className="text-orange" />
        </Tooltip>
      );
    }
    let message = "";
    let icon = <div></div>;
    switch (state) {
      case ComponentStatus.UNKNOWN:
        message = "Heartbeat Lost";
        icon = (
          <FontAwesomeIcon icon={faQuestionCircle} className="text-warning" />
        );
        break;
      case ComponentStatus.INIT:
        message = "Install Pending...";
        icon = (
          <FontAwesomeIcon icon={faQuestionCircle} className="text-warning" />
        );
        break;
      case ComponentStatus.INSTALLING:
        message = "Installing";
        icon = (
          <FontAwesomeIcon icon={faCog} className="text-info blinking-icon" />
        );
        break;
      case ComponentStatus.STOPPING:
        message = "Stopping";
        icon = (
          <FontAwesomeIcon
            icon={faWarning}
            className="text-danger blinking-icon"
          />
        );
        break;
      case ComponentStatus.STOPPED:
        if (type === ComponentType.CLIENT) {
          message = "Installed";
          icon = (
            <FontAwesomeIcon icon={faCheckCircle} className="text-success" />
          );
        } else {
          message = "Stopped";
          icon = <FontAwesomeIcon icon={faWarning} className="text-danger" />;
        }
        break;
      case ComponentStatus.STARTING:
        message = "Starting";
        icon = (
          <FontAwesomeIcon
            icon={faCheckCircle}
            className="success blinking-icon"
          />
        );
        break;
      case ComponentStatus.STARTED:
        message = "Started";
        icon = (
          <FontAwesomeIcon icon={faCheckCircle} className="text-success" />
        );
        break;
      case ComponentStatus.INSTALL_FAILED:
        message = "Install Failed";
        icon = <FontAwesomeIcon icon={faCog} className="text-danger" />;
        break;
    }
    return <Tooltip message={message}>{icon}</Tooltip>;
  };

  const getStatusIcons = (component: IHostComponent) => {
    const maintenanceState = get(component, "passiveState", "OFF");
    const hasStaleConfigs = get(component, "staleConfigs", false);
    return (
      <div className="d-flex">
        <div className="me-2">{getStateIcon(component)}</div>
        {hasStaleConfigs ? (
          <FontAwesomeIcon icon={faRefresh} className="text-warning me-2" />
        ) : null}
        {maintenanceState !== "OFF" ? (
          <FontAwesomeIcon icon={faMedkit} className="text-dark me-2" />
        ) : null}
      </div>
    );
  };

  const setSelectedActionData = (
    component: any,
    action: string,
    isCustom: boolean,
    successCallback: (component: any, data?: any) => any,
    data?: any
  ) => {
    data = data || {};
    selectedActionData.current.component = component;
    selectedActionData.current.action = action;
    selectedActionData.current.data = data;
    selectedActionData.current.isCustom = isCustom;
    selectedActionData.current.successCallback = successCallback;
  };

  const isComponentDecommissionAvailable = (component: IHostComponent) => {
    return get(
      decommissionable,
      getComponentName(component) + ".isComponentDecommissionAvailable",
      false
    );
  };

  const isComponentRecommissionAvailable = (component: IHostComponent) => {
    return get(
      decommissionable,
      getComponentName(component) + ".isComponentRecommissionAvailable",
      false
    );
  };

  const isToggleMaintenanceModeAvailable = (component: IHostComponent) => {
    return (
      isActive(component) ||
      ![
        PassiveStateOnFilters.IMPLIED_FROM_SERVICE,
        PassiveStateOnFilters.IMPLIED_FROM_SERVICE_AND_HOST,
      ].includes(get(component, "passiveState") as PassiveStateOnFilters)
    );
  };

  const getActions = useCallback(
    (component: IHostComponent) => {
      const actions: React.ReactElement[] = [];
      const state = get(component, "workStatus", "") as ComponentStatus;

      //Actions of Clients
      if (get(component, "componentCategory", "") === ComponentType.CLIENT) {
        // Refresh configs - Requires SERVICE.MODIFY_CONFIGS authorization
        if (canModifyConfigs) {
          actions.push(
            <div
              key="refresh-configs"
              onClick={() => {
                //TODO: Will be implemented in future PR
              }}
            >
              Refresh configs
            </div>
          );
        }

        // Install - Requires SERVICE.ADD_DELETE_SERVICES authorization
        if (canAddDeleteServices) {
          actions.push(
            <div
              key="install"
              onClick={() => {
                if (isInit(component)) {
                  const data = {
                    allComponents,
                    clusterComponents,
                    services,
                    // getKDCSessionState, TODO: will be added in future PR.
                    host: allHostModels[0],
                  };
                  setSelectedActionData(
                    [component],
                    "install",
                    false,
                    installClients,
                    data
                  );
                  setShowConfirmationModal(true);
                }
              }}
              className={isInit(component) ? "" : "disabled-btn"}
            >
              Install
            </div>
          );
        }

        // Re-Install - Requires SERVICE.ADD_DELETE_SERVICES authorization
        if (state === ComponentStatus.INSTALL_FAILED && canAddDeleteServices) {
          actions.push(
            <div
              key="re-install"
              onClick={() => {
                  const data = {
                  allComponents,
                  clusterComponents,
                  services,
                  // getKDCSessionState, TODO: will be added in future PR.
                };
                setSelectedActionData(
                  [component],
                  "re-install",
                  false,
                  installClients,
                  data
                );
                setShowConfirmationModal(true);
              }}
            >
              Re-Install
            </div>
          );
        }

        // Custom commands - Requires SERVICE.START_STOP authorization
        if (canStartStopServices) {
          getClientCustomCommands(component).forEach(
            (cmd: any, index: number) => {
              actions.push(
                <div
                  key={`custom-${index}`}
                  onClick={() => {
                    executeCustomCommand(cmd, component);
                  }}
                >
                  {get(cmd, "label", "")}
                </div>
              );
            }
          );
        }
      }
      //Actions of Masters and Slaves
      else {
        // Decommission - Requires SERVICE.START_STOP authorization
        if (
          isComponentDecommissionAvailable(component) &&
          canStartStopServices
        ) {
          actions.push(
            <div
              key="decommission"
              onClick={() => {
                if (!isComponentDecommissionDisable(component)) {
                  const data = { clusterComponents };
                  setSelectedActionData(
                    component,
                    "decommission",
                    false,
                    decommission,
                    data
                  );
                  setShowConfirmationModal(true);
                }
              }}
              className={
                isComponentDecommissionDisable(component) ? "disabled-btn" : ""
              }
            >
              Decommission
            </div>
          );
        }

        // Recommission - Requires SERVICE.START_STOP authorization
        if (
          isComponentRecommissionAvailable(component) &&
          canStartStopServices
        ) {
          actions.push(
            <div
              key="recommission"
              onClick={() => {
                if (!isComponentDecommissionDisable(component)) {
                  setSelectedActionData(
                    component,
                    "recommission",
                    false,
                    recommission
                  );
                  setShowConfirmationModal(true);
                }
              }}
              className={
                isComponentDecommissionDisable(component) ? "disabled-btn" : ""
              }
            >
              Recommission
            </div>
          );
        }

        const isDecommissionableComponent = decommissionableComponents.includes(
          getComponentName(component)
        );
        const canRestart = isDecommissionableComponent
          ? isComponentDecommissionAvailable(component) &&
          isRestartable(component)
          : !isRestartComponentDisabled(component) && isRestartable(component);

        // Restart - Requires SERVICE.START_STOP authorization
        if (canRestart && canStartStopServices) {
          actions.push(
            <div
              key="restart"
              onClick={() => {
                setSelectedActionData(
                  component,
                  "restart",
                  false,
                  restartComponent
                );
                setShowConfirmationModal(true);
              }}
            >
              Restart
            </div>
          );
        }

        if (state !== ComponentStatus.INSTALLING) {
            // Stop - Requires SERVICE.START_STOP authorization
            if (isStart(component) && canStartStopServices) {
                actions.push(
                    <div
                        key="stop"
                        onClick={() => {
                            setSelectedActionData(
                                component,
                                "stop",
                                false,
                                stopComponent
                            );
                            if (getComponentName(component) === "NAMENODE") {
                                checkNnLastCheckpointTime(
                                    () => setShowConfirmationModal(true),
                                    get(component, "hostName", ""),
                                    clusterName
                                );
                            } else {
                                setShowConfirmationModal(true);
                            }
                        }}
                    >
                        Stop
                    </div>
                );
            }

            // Start - Requires SERVICE.START_STOP authorization
            if (!isStart(component) && canStartStopServices) {
                if (!isInit(component)) {
                    if (
                        ![
                            ComponentStatus.UPGRADE_FAILED,
                            ComponentStatus.INSTALL_FAILED,
                        ].includes(state)
                    ) {
                        actions.push(
                            <div
                                key="start"
                                onClick={() => {
                                    setSelectedActionData(
                                        component,
                                        "start",
                                        false,
                                        startComponent
                                    );
                                    setShowConfirmationModal(true);
                                }}
                            >
                                Start
                            </div>
                        );
                    }
                }
            }

            if (state === ComponentStatus.UPGRADE_FAILED) {
                actions.push(<div key="retry-upgrade">Retry Upgrade</div>);
            }

            // Re-Install Failed - Requires SERVICE.ADD_DELETE_SERVICES authorization
            if (
                state === ComponentStatus.INSTALL_FAILED &&
                canAddDeleteServices
            ) {
                actions.push(
                    <div
                        key="re-install-failed"
                        onClick={() => {
                            //TODO: Will be implemented in future PR
                        }}
                    >
                        Re-Install
                    </div>
                );
            }

            // Move operations - Requires SERVICE.MOVE authorization
            if (canMoveComponents && isReassignable(component, getClusterHosts().length)) {
                actions.push(
                    <div
                        key="move"
                        onClick={() => moveComponent(component)}
                        className={
                            isMoveComponentDisabled(
                                component,
                                getClusterHosts().length,
                                get(clusterComponents, "items", [])
                            )
                                ? "disabled-btn"
                                : ""
                        }
                    >
                        Move
                    </div>
                );
            }
        }

        // Maintenance Mode - Always available (no specific authorization required)
        actions.push(
          <div
            key="maintenance-mode"
            onClick={() => {
              //TODO: Will be implemented in future PR
            }}
            className={
              isToggleMaintenanceModeAvailable(component) ? "" : "disabled-btn"
            }
          >
            {isActive(component)
              ? "Turn On Maintenance Mode"
              : "Turn Off Maintenance Mode"}
          </div>
        );

        // Re-Install Init - Requires SERVICE.ADD_DELETE_SERVICES authorization
        if (isInit(component) && canAddDeleteServices) {
          actions.push(
            <div
              key="re-install-init"
              onClick={() => {
                //TODO: Will be implemented in future PR
              }}
            >
              Re-Install
            </div>
          );
        }

        // Delete - Requires SERVICE.ADD_DELETE_SERVICES authorization
        if (isDeletable(component, serviceModels) && canAddDeleteServices) {
          actions.push(
            <div
              key="delete"
              className={
                isDeleteComponentDisabled(
                  component,
                  get(clusterComponents, "items", [])
                )
                  ? "disabled-btn"
                  : ""
              }
              onClick={() => {
                //TODO: Will be implemented in future PR
              }}
            >
              Delete
            </div>
          );
        }

        // Refresh configs - Requires SERVICE.MODIFY_CONFIGS authorization
        if (isRefreshConfigsAllowed(component) && canModifyConfigs) {
          actions.push(
            <div
              key="refresh-component-configs"
              onClick={() => {
                //TODO: Will be implemented in future PR
              }}
            >
              Refresh configs
            </div>
          );
        }

        // Custom commands - Requires SERVICE.START_STOP authorization
        if (canStartStopServices) {
          getCustomCommands(
            component,
            get(clusterComponents, "items", [])
          ).forEach((cmd: any, index: number) => {
            actions.push(
              <div
                key={`custom-master-${index}`}
                onClick={() => {
                  executeCustomCommand(cmd, component);
                }}
              >
                {get(cmd, "label", "")}
              </div>
            );
          });
        }
      }
      return actions;
    },
    [
      allComponents,
      clusterComponents,
      services,
      JSON.stringify(allHostModels),
      clusterName,
      JSON.stringify(serviceModels),
    ]
  );

  const moveComponent = (component: IHostComponent) => {
    const modalProps = {
      modalTitle: translate("popup.confirmation.commonHeader"),
      modalBody: translateWithVariables("question.sure.move", {
        "0": getComponentDisplayName(component),
      }),
      onClose: () => { },
      successCallback: () => {
        navigate(
          "/main/service/reassign/" + getComponentName(component) + "/step1"
        );
        modalManager.hide();
      },
      options: {
        buttonSize: "sm" as "sm" | "lg" | undefined,
        cancelableViaIcon: true,
        cancelableViaBtn: true,
        okButtonVariant: "primary",
      },
    };
    modalManager.show(modalProps);
  };

  const handleDropdownToggle = (
    isOpen: boolean,
    componentId: string,
    isStarting: boolean
  ) => {
    if (isOpen && !isStarting) {
      setOpenDropdownId(componentId);
    } else {
      setOpenDropdownId(null);
    }
  };

  const componentActionsMap = useMemo(() => {
    if (
      !allHostModels ||
      !allHostModels[0] ||
      !allHostModels[0].hostComponents
    ) {
      return {};
    }

    const actionsMap: Record<string, React.ReactElement[]> = {};
    allHostModels[0].hostComponents.forEach((component: IHostComponent) => {
      const componentId = `${component.serviceName}-${component.componentName}-${component.hostName}`;
      actionsMap[componentId] = getActions(component);
    });
    return actionsMap;
  }, [getActions, allHostModels]);

  const columnsInComponentsTable = useMemo(
    () => [
      {
        header: "Status",
        id: "status",
        width: "12%",
        cell: (info: any) => {
          return getStatusIcons(get(info, "row.original", {}));
        },
      },
      {
        header: "Name",
        id: "name",
        width: "50%",
        cell: (info: any) => {
          const serviceName = get(info, "row.original.serviceName", "");
          return (
            <div className="d-flex">
              <div className="me-2">
                {get(info, "row.original.nnHAState", "") ? startCase(get(info, "row.original.nnHAState", "")) + " " : ""}
                {get(info, "row.original.displayName", "")}
                {" / "}
              </div>
              <Link
                to={`/main/services/${serviceName}/summary`}
                className="custom-link me-1"
              >
                <div>{startCase(serviceName.toLowerCase())}</div>
              </Link>
              <div>{get(info, "row.original.nnHAState", "") ? " - " + clusterName : ""}</div>
            </div>
          );
        },
      },
      {
        header: "Type",
        id: "type",
        width: "15%",
        cell: (info: any) => {
          return startCase(
            get(info, "row.original.componentCategory", "").toLowerCase()
          );
        },
      },
      {
        header: "Action",
        id: "action",
        width: "10%",
        cell: (info: any) => {
          const component = get(info, "row.original", {});
          const componentId = `${component.serviceName}-${component.componentName}-${component.hostName}`;
          const isStarting =
            get(component, "workStatus", "") === ComponentStatus.STARTING;
          const availableActions = componentActionsMap[componentId] || [];

          // Hide dropdown if user has no access to any actions
          if (availableActions.length === 0 || isUpgradeInProgress) {
            return null;
          }

          return (!canPerformActions ? null : (
            <Dropdown
              drop="down-centered"
              show={openDropdownId === componentId}
              onToggle={(isOpen) =>
                handleDropdownToggle(isOpen, componentId, isStarting)
              }
            >
              <Dropdown.Toggle
                variant="transparent border-0"
                className={classNames("custom-link p-0 m-0", {
                  "disabled-btn disabled": isStarting,
                })}
              >
                <FontAwesomeIcon icon={faEllipsis} className="fs-6 me-1" />
              </Dropdown.Toggle>
              {!isStarting && (
                <Dropdown.Menu className="rounded-0">
                  {availableActions.map(
                    (action: React.ReactElement, index: number) => (
                      <Dropdown.Item key={index}>{action}</Dropdown.Item>
                    )
                  )}
                </Dropdown.Menu>
              )}
            </Dropdown>
          ));
        },
      },
    ],
    [getActions, componentActionsMap, openDropdownId]
  );

  if (loading) {
    return <Spinner />;
  }

  return (
    <div>
      {showConfirmationModal ? (
        <Modal
          isOpen={showConfirmationModal}
          onClose={() => setShowConfirmationModal(false)}
          modalTitle={translate("popup.confirmation.commonHeader")}
          modalBody={
            selectedActionData.current.isCustom
              ? translate("question.sure")
              : `Are you sure you want to ${selectedActionData.current.action
              } ${getComponentDisplayName(
                selectedActionData.current.component as IHostComponent
              )}?`
          }
          successCallback={async () => {
            await selectedActionData.current.successCallback(
              selectedActionData.current.component,
              selectedActionData.current.data
            );
            setShowConfirmationModal(false);
          }}
          options={{
            modalSize: "modal-sm",
            cancelableViaIcon: true,
            cancelableViaBtn: true,
            okButtonVariant: "primary",
          }}
        />
      ) : null}
      {showSelectTimeModal ? (
        <SelectTimeRangeModal
          isOpen={showSelectTimeModal}
          onClose={() => setShowSelectTimeModal(false)}
          successCallback={(data) => {
            pausePolling();
            setSelectedMetricsOption(
              "CUSTOM: " +
                formatDate(new Date(data.startTime * 1000))
                  .split("T")
                  .join(" ")
            );
            getHostMetrics(data.startTime, data.endTime);
            setShowSelectTimeModal(false);
          }}
        />
      ) : null}
      <div className="d-flex w-100 justify-content-center">
        <div className="w-100 mx-5">
          {getCmponentsToBeRestarted(get(allHostModels, "[0]", {} as IHost))
            .length ? (
            <div>
              <Alert className="rounded-0" variant="warning">
                <div className="d-flex justify-content-between">
                  <div className="pt-2">
                    <FontAwesomeIcon icon={faRefresh} className="me-1" />
                    {translateWithVariables(
                      "hosts.host.details.needToRestart",
                      {
                        "0": getCmponentsToBeRestarted(
                          get(allHostModels, "[0]", {} as IHost)
                        )?.length?.toString(),
                        "1": String(
                          translate("common.components")
                        ).toLowerCase(),
                      }
                    )}
                  </div>
                  {/* Restart Button - Requires SERVICE.START_STOP authorization */}
                  {canStartStopServices && !isUpgradeInProgress && (
                    <Button
                      variant="warning"
                      className="text-light custom-btn"
                      onClick={() => {
                        const components = getCmponentsToBeRestarted(
                          get(allHostModels, "[0]", {} as IHost)
                        );
                        const data = { clusterName: clusterName };
                        setSelectedActionData(
                          components,
                          "",
                          true,
                          restartAllStaleConfigComponents,
                          data
                        );
                        const nameNodeComponent = components.filter(
                          (component: any) =>
                            getComponentName(component) === "NAMENODE"
                        )[0];
                        if (
                          nameNodeComponent &&
                          get(nameNodeComponent, "workStatus", "") ===
                            ComponentStatus.STARTED
                        ) {
                          checkNnLastCheckpointTime(
                            () => setShowConfirmationModal(true),
                            get(nameNodeComponent, "hostName", ""),
                            clusterName
                          );
                        } else {
                          setShowConfirmationModal(true);
                        }
                      }}
                    >
                      {String(translate("common.restart")).toUpperCase()}
                    </Button>
                  )}
                </div>
              </Alert>
            </div>
          ) : null}
          <div className="d-flex w-100 mb-4">
            <Card className="w-50 rounded-0 me-4">
              <div className="d-flex justify-content-between px-3 pt-3">
                <h3 className="mt-2">{translate("common.components")}</h3>
                {/* Add Component Dropdown - Requires SERVICE.ADD_DELETE_SERVICES authorization */}
                {canAddDeleteServices && !isUpgradeInProgress && (
                  <Dropdown>
                    <Dropdown.Toggle
                      variant="transparent"
                      className="btn-default"
                    >
                      <FontAwesomeIcon icon={faPlus} className="me-2" />
                      <span className="me-2">
                        {String(translate("common.add")).toUpperCase()}
                      </span>
                    </Dropdown.Toggle>
                    <Dropdown.Menu className="rounded-0">
                      {addableComponents.map((component) => (
                        <Dropdown.Item
                          key={component.component_name}
                          onClick={() => {
                            //TODO: Will be implemented in future PR
                          }}
                        >
                          {component.display_name}
                        </Dropdown.Item>
                      ))}
                    </Dropdown.Menu>
                  </Dropdown>
                )}
              </div>
              <hr />
              <Table
                data={get(allHostModels, "[0].hostComponents", [])}
                columns={columnsInComponentsTable}
                scrollable={false}
              />
            </Card>
            <HostMetrics
              metricsData={metricsData}
              allHostModels={allHostModels}
              selectedMetricsOption={selectedMetricsOption}
              setSelectedMetricsOption={setSelectedMetricsOption}
              setShowSelectTimeModal={setShowSelectTimeModal}
            />
          </div>
          <div className="d-flex w-100 mb-4">
            <Card className="w-50 rounded-0 me-4">
              <div className="d-flex justify-content-between px-3 pt-3">
                <h3 className="mt-2">{translate("common.summary")}</h3>
              </div>
              <hr />
              <div className="pb-3">
                {Object.keys(summary).map((key: string) => {
                  return (
                    <div className="d-flex" key={key}>
                      <div className="d-flex justify-content-end mb-2 w-40">
                        <div className="me-2 fw-bold">{key}:</div>
                      </div>
                      <div>{get(summary, key, "")}</div>
                      {/* Edit Rack - Requires HOST.ADD_DELETE_COMPONENTS authorization */}
                      {key === "Rack" &&
                      get(summary, key) &&
                      canManageHostComponents &&
                      !isUpgradeInProgress ? (
                        <FontAwesomeIcon
                          icon={faPencil}
                          className="ms-2 custom-link"
                          onClick={() => {
                            const data = {
                              RequestInfo: {
                                context: "Set Rack",
                                query: `Hosts/host_name.in(${params.hostname})`,
                              },
                              Body: {
                                Hosts: {
                                  rack_info: get(summary, key, ""),
                                },
                              },
                            };
                            modalManager.show(
                              <SetRackInfoModal
                                clusterName={clusterName}
                                data={data}
                                callback={setAllHostModels}
                                hostNames={[get(summary, "Hostname", "")]}
                              />
                            );
                          }}
                        />
                      ) : null}
                    </div>
                  );
                })}
              </div>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}
