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

import { ActionsApi } from "../../api/actionsApi";
import { useContext, useEffect, useState } from "react";
import { AppContext } from "../../store/context";
import { useAuth } from "../../hooks/useAuth";
import {
  cloneDeep,
  filter,
  find,
  get,
  isEmpty,
  lowerCase,
  map,
  set,
  startCase,
} from "lodash";
import ConfirmationModal from "../../components/ConfirmationModal";
import { ServiceApi } from "../../api/serviceApi";
import { ServiceActionEnums } from "../../enums/ServiceActionEnums";
import DefaultButton from "../../components/DefaultButton";
import {
  Alert,
  Button,
  Col,
  Dropdown,
  DropdownButton,
  DropdownItem,
  Form,
  Modal,
  OverlayTrigger,
  Row,
  Tooltip,
} from "react-bootstrap";
import { showRollingRestartPopup } from "../../screens/Hosts/batchUtils";
import { HostsApi } from "../../api/hostsApi";
import {
  ComponentNameEnums,
  getSlaveComponentMapping,
} from "../../enums/ComponentNameEnums";
import { ComponentActionsMapping } from "./ComponentActionsMapping";
import { getDynamicComponentActions } from "./DynamicComponentActions";
import EnableHighAvailibilityNameNode from "./highAvailibility/nameNode/index";
import BackgroundOperations from "../BackgroundOperations";
import modalManager from "../../store/ModalManager";
import ReassignComponent from "./reassign";
import {
  displayNameServiceMapping,
  // selectClientComponentsForService,
  // selectMasterComponentsForService,
  // selectSlaveComponentsForService,
  serviceNameDisplayMapping,
  serviceNameModelMapping,
} from "../../constants";
import EnableHighAvailibilityRangerAdmin from "./highAvailibility/rangerAdmin/index";
import ServiceActionsUrlMapping from "./ServiceActionsUrlMapping";
import {
  downloadClientConfigsCall,
  ResourceTypeEnum,
} from "../Hosts/supportClientConfigsDownload";
import { messages } from "../messages";
import { ServiceContext } from "../../store/ServiceContext";
import ManageJournalNodes from "./highAvailibility/journalNode/index";
import EnableNamenodeFederation from "./highAvailibility/Federation/index";
import useComponentAddDelete from "../Hosts/hooks/useComponentAddDelete";
import { useConfigs } from "../../hooks/useConfigs";
import useStackServices from "../../hooks/useStackServices";
import { addComponentWithCheck } from "../Hosts/actions";
import useKDCSessionState from "../../hooks/useKDCSessionState";
import EnableHighAvailibilityResourceManger from "./highAvailibility/resourceManager/index";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faClock,
  faDownload,
  faMedkit,
  faPlay,
  faPlayCircle,
  faPlus,
  faRefresh,
  faStop,
  faThumbsUp,
  faTimesCircle,
} from "@fortawesome/free-solid-svg-icons";
import { useServiceDeletion } from "../../hooks/useServiceDeletion";
import ConfigsApi from "../../api/configsApi";
import {
  serviceMap,
  translate,
  translateWithVariables,
} from "../../Utils/Utility";

interface ActionsProps {
  serviceName: string;
  className?: string;
}

export const Actions = ({ serviceName, className }: ActionsProps) => {
  if (!serviceName) {
    return null;
  }

  const {
    cluster,
    isKerberosEnabled,
    clusterName,
    upgradeIsRunning,
    upgradeSuspended,
    isClusterInstalled
  } = useContext(AppContext);
  const { allServiceModels } = useContext(ServiceContext);

  // Authorization hooks - implementing Ember.js App.isAuthorized patterns
  const { hasAuthorization } = useAuth();

  // Check specific authorizations like in Ember.js ui/app/views/main/service/item.js
  const canStartStop = hasAuthorization("SERVICE.START_STOP");
  const canRunCustomCommands =
    hasAuthorization("SERVICE.RUN_CUSTOM_COMMAND") ||
    hasAuthorization("SERVICE.RUN_SERVICE_CHECK") ||
    hasAuthorization("SERVICE.TOGGLE_MAINTENANCE") ||
    hasAuthorization("SERVICE.ENABLE_HA");
  const canAddDeleteServices = hasAuthorization("SERVICE.ADD_DELETE_SERVICES");
  const canAddDeleteComponents = hasAuthorization("HOST.ADD_DELETE_COMPONENTS");
  const canEnableHA = hasAuthorization("SERVICE.ENABLE_HA");
  const canMoveComponents = hasAuthorization("SERVICE.MOVE");

  // Check if user has any service-related permissions at all
  // This matches Ember.js logic where Actions dropdown is hidden if no permissions
  const hasAnyServicePermissions =
    canStartStop ||
    canRunCustomCommands ||
    canAddDeleteServices ||
    canAddDeleteComponents ||
    canEnableHA ||
    canMoveComponents ||
    hasAuthorization("SERVICE.RUN_SERVICE_CHECK") ||
    hasAuthorization("SERVICE.TOGGLE_MAINTENANCE");

  // Use computed upgrade properties instead of basic state checks
  // This matches Ember.js logic: upgradeIsRunning && !upgradeSuspended blocks operations
  // FIXED: Add additional check for upgrade suspended state to prevent flaky behavior
  // When upgrade is suspended/paused, actions should be available
  const isUpgradeBlocking = upgradeIsRunning && !upgradeSuspended;

  // If user has no service permissions, don't render the Actions dropdown
  // FIXED: Remove upgrade blocking check when upgrade is suspended to prevent dropdown from disappearing
  if (!hasAnyServicePermissions) {
    return null;
  }

  // FIXED: Only block actions when upgrade is actively running (not suspended/paused)
  // This prevents the flaky behavior where dropdown disappears during upgrade pause
  if (isUpgradeBlocking) {
    return null;
  }
  const [showConfirmationModal, setShowConfirmationModal] = useState(false);
  const [stopMaintenanceMode, setStopMaintenanceMode] = useState(false);
  const [startMaintenanceModeOff, setStartMaintenanceModeOff] = useState(false);
  const [showStopConfirmation, setShowStopConfirmation] = useState(false);
  const [showStartConfirmation, setShowStartConfirmation] = useState(false);
  const [modalInfo, setModalInfo] = useState<any>({});
  const {
    parsedSocketMessages: socketMessages,
    services,
    allHostNames,
  } = useContext(AppContext);
  const { serviceModels } = useContext(ServiceContext);
  const payloadOperationLevel = {
    level: `${ServiceActionEnums.clusterLevel}`,
    cluster_name: clusterName,
  };

  const [serviceState, setServiceState] = useState<any>({});
  const [
    showRestartComponentConfirmationModal,
    setShowRestartComponentConfirmationModal,
  ] = useState<boolean>(false);
  const [showAlert, setShowAlert] = useState<boolean>(false);
  const [alertMessages, setAlertMessages] = useState<string[]>([]);
  const [batchSize, setBatchSize] = useState<string>("");
  const [waitTimeBetweenBatches, setWaitTimeBetweenBatches] =
    useState<string>("");
  const [tolerateFailures, setTolerateFailures] = useState<string>("");
  //set components for service
  const [componentToBeRestarted, setComponentToBeRestarted] =
    useState<string>("");
  const [threshold, setThreshold] = useState<string>("");
  const [rebalanceConfirmationModal, setRebalanceConfirmationModal] =
    useState<boolean>(false);
  const stackInfo = get(cluster, "version", "").split("-");
  const stackName = stackInfo[0];
  const stackVersion = stackInfo[1];
  const [isServiceCheckSupported, setIsServiceCheckSupported] =
    useState<boolean>(false);
  const [
    rollingRestartSupportedComponents,
    setRollingRestartSupportedComponents,
  ] = useState<{ [key: string]: boolean }>({});
  const [isLoadingComponentDetails, setIsLoadingComponentDetails] =
    useState<boolean>(true);
  const [isRestartAllSelected, setIsRestartAllSelected] =
    useState<boolean>(false);

  const [clusterComponents, setClusterComponents] = useState<any>({});
  const { services: stackServices } = useStackServices();
  const { getConfigByName } = useConfigs([], stackServices as any);
  const { addAndReconfigureComponent } = useComponentAddDelete(
    clusterComponents,
    stackServices,
    getConfigByName
  );
  let validDropDownHosts: string[] = [];
  let allHostsOnCluster: string[] = [];

  const installedServiceNames = map(services, "ServiceInfo.service_name");
  const allStackServicesFromHook = useStackServices();
  const installedStackServicesFromHook =
    allStackServicesFromHook.services.filter((allStackServiceItem) => {
      return installedServiceNames.includes(
        allStackServiceItem?.StackServices?.service_name
      );
    });

  //from hooks
  const { getKDCSessionState } = useKDCSessionState(() => {});
  const serviceNames = map(services, "ServiceInfo.service_name").join(",");
  const [stackDataWithDependencies, setStackDataWithDependencies] =
    useState<any>({});
  const getClusterComponents = async () => {
    //setLoading(true);
    const response = await HostsApi.getClusterComponents(
      clusterName,
      "ServiceComponentInfo/service_name,host_components/HostRoles/display_name,host_components/HostRoles/host_name,host_components/HostRoles/public_host_name,host_components/HostRoles/state,host_components/HostRoles/maintenance_state,host_components/HostRoles/stale_configs,host_components/HostRoles/ha_state,host_components/HostRoles/desired_admin_state,&minimal_response=true"
    );

    setClusterComponents(response);
  };

  const checkIfServiceIsClientOnly = () => {
    return allServiceModels[
      serviceNameModelMapping[serviceName]
    ]?.hasOwnProperty("isClientOnlyService");
  };

  const isServiceClientOnly = checkIfServiceIsClientOnly();

  //TODO: maybe a future optimization, code can be corrected
  // useEffect(() => {
  //   const currentModel = allServiceModels[serviceNameModelMapping[serviceName]];
  //   const prevModel = prevModelRef.current;

  //   console.log("Current Model:", currentModel);
  //   console.log("Previous Model:", prevModel);
  //   if (JSON.stringify(currentModel) !== JSON.stringify(prevModel)) {
  //     isDeleteServiceNotSupportedOnClick();
  //     // Update the ref with current value for next comparison
  //     prevModelRef.current = currentModel;
  //   }
  // }, [allServiceModels, serviceName]);

  useEffect(() => {
    const fetchData = async () => {
      const data = await ConfigsApi.getServiceConfigurations(
        stackName,
        stackVersion,
        serviceNames
      );
      setStackDataWithDependencies(data);
    };
    if(isClusterInstalled)
    fetchData();
  }, [stackName, stackVersion, serviceNames,isClusterInstalled]);

  const { deleteService } = useServiceDeletion(
    serviceName,
    installedStackServicesFromHook,
    stackDataWithDependencies.items
  );

  const payloadForClientServiceCheck = () => {
    const payloadForClientOnlyService = {
      RequestInfo: {
        //@ts-ignore
        context: `${serviceNameDisplayMapping[serviceName]} Service Check`,
        command: `${serviceName}_SERVICE_CHECK`,
      },
      "Requests/resource_filters": [
        {
          service_name: `${serviceName}`,
        },
      ],
    };
    return payloadForClientOnlyService;
  };

  useEffect(() => {
    //@ts-ignore
    function isClientOnlyServiceIsNotInstalled() {
      const serviceModelForClientOnlyService =
        serviceModels[serviceNameModelMapping[serviceName]];
      const clientOnlyValue =
        serviceModelForClientOnlyService?.isClientOnlyService;

      if (
        clientOnlyValue &&
        serviceModelForClientOnlyService[
          `${serviceName.toLowerCase()}ClientsInstalled`
        ] === 0
      ) {
        return true;
      }
      return false;
    }

    async function fetchServiceState() {
      if (
        clusterName &&
        serviceName &&
        installedServiceNames.includes(serviceName)
      ) {
        const response = await ServiceApi.getServiceState(
          clusterName,
          serviceName.toUpperCase()
        );
        setServiceState(response.data.ServiceInfo);
      }
    }

    async function fetchServiceCheckSupported() {
      if (!allServiceModels || !allServiceModels[serviceNameModelMapping[serviceName]]) {
        return;
      }

      // Match Ember.js logic exactly: check if service supports service check from stack definition
      let isServiceCheckSupportedFromStack = false;
      
      try {
        const response = await ServiceApi.isServiceCheckSupported(
          clusterName,
          //@ts-ignore
          serviceName,
          stackName,
          stackVersion
        );
        isServiceCheckSupportedFromStack = get(
          response.data,
          "StackServices.service_check_supported",
          false
        );
      } catch (error) {
        console.error("Error fetching service check support", error);
        // Default to false if we can't determine support
        isServiceCheckSupportedFromStack = false;
      }

      // Apply Ember.js isSmokeTestDisabled logic
      let isSmokeTestDisabled = false;
      
      if (isServiceClientOnly) {
        // For client-only services, service check is always enabled (not disabled)
        isSmokeTestDisabled = false;
      } else if (serviceName === "PXF") {
        // Special case for PXF: disabled if any PXF component is down
        // This would need to be implemented if PXF is supported
        isSmokeTestDisabled = false; // Simplified for now
      } else {
        // For all other services, use isStopDisabled logic
        // Service check is disabled when service is stopped (matches Ember.js isStopDisabled)
        isSmokeTestDisabled = !serviceModelHasStartedMasterOrSlaveComps();
      }

      // Final decision: service check is supported if stack supports it AND it's not disabled
      const finalServiceCheckSupported = isServiceCheckSupportedFromStack && !isSmokeTestDisabled;
      setIsServiceCheckSupported(finalServiceCheckSupported);
    }

    //fetchClusterName();
    fetchServiceState();
    fetchServiceCheckSupported();
  }, [serviceName, clusterName, allServiceModels, serviceModels]);

  useEffect(() => {
    const message = socketMessages[0];
    if (
      get(message, "maintenance_state", "") &&
      get(message, "service_name", "") === serviceName
    ) {
      const serviceStateCopy: any = cloneDeep(serviceState);
      set(
        serviceStateCopy,
        "maintenance_state",
        get(message, "maintenance_state", "")
      );
      setServiceState(serviceStateCopy);
    }
  }, [socketMessages.length]);

  useEffect(() => {
    if(isClusterInstalled)
    getClusterComponents();
  }, [isClusterInstalled]);

  // Check component addition availability - simplified approach like Ember.js
  const checkComponentAdditionEnabled = (componentName: string, serviceModelName: string) => {
    if (!serviceModels || !serviceModels[serviceModelName]?.masterComponents || !allHostNames) {
      return false;
    }

    // Find the component in the service model
    const masterComponents = serviceModels[serviceModelName].masterComponents;
    const component = masterComponents.find((comp: any) => comp.componentName === componentName);
    
    if (!component) {
      return false;
    }

    // Following Ember.js logic: enabled if installed instances < total hosts
    const installedCount = component.totalCount || 0;
    const totalHosts = allHostNames.length;
    
    return installedCount < totalHosts;
  };

  // Fetch components that support rolling restart
  const fetchRollingRestartSupportedComponents = async () => {
    setIsLoadingComponentDetails(true);
    try {
      if (!stackName || !stackVersion || !serviceName) {
        setIsLoadingComponentDetails(false);
        return;
      }

      const response = await ServiceApi.getServiceComponentDetails(
        stackName,
        stackVersion,
        serviceName
      );

      // Process the response to extract components with has_bulk_commands_definition = true
      const components = response.data.components || [];
      const supportedComponents: { [key: string]: boolean } = {};

      components.forEach((component: any) => {
        const componentName = component.StackServiceComponents?.component_name;
        const isRollingRestartSupported =
          component.StackServiceComponents?.component_category === "SLAVE" ||
          component.StackServiceComponents?.rolling_restart_supported === true;

        if (componentName) {
          supportedComponents[componentName] = isRollingRestartSupported;
        }
      });

      setRollingRestartSupportedComponents(supportedComponents);
    } catch (error) {
      console.error("Error fetching component details", error);
    } finally {
      setIsLoadingComponentDetails(false);
    }
  };

  // Call the function when the component mounts
  useEffect(() => {
    if (serviceName && stackName && stackVersion) {
      fetchRollingRestartSupportedComponents();
    }
  }, [serviceName, stackName, stackVersion]);

  const getAllHosts = async () => {
    let allHostsOnClusterResponse: any = {};
    try {
      allHostsOnClusterResponse = await HostsApi.getAllHosts(clusterName);
    } catch (error) {
      console.error("Failed to load all hosts:", error);
    }

    allHostsOnCluster = allHostsOnClusterResponse.items.map((hostItem: any) => {
      return hostItem.Hosts.host_name;
    });
  };

  const getHostsWithoutComponent = async (
    componentName: string,
    serviceModelName: string
  ) => {
    const hiveMasterComponents =
      serviceModels[serviceModelName].masterComponents;
    if (allHostsOnCluster.length === 0 || hiveMasterComponents.length === 0) {
      return;
    }

    // Get hosts where the specified component is already installed
    const hostsWithComponent = hiveMasterComponents
      .filter((component: any) => component.componentName === componentName)
      .reduce((acc: any[], component: any) => {
        const hostMappings = component.hostComponents.map(
          (host: any) => host.HostRoles.host_name
        );
        return acc.concat(hostMappings);
      }, []);

    // Exclude hosts where the component is already installed
    const availableHosts = allHostsOnCluster.filter(
      (host: string) => !hostsWithComponent.includes(host)
    );

    validDropDownHosts = availableHosts;
    //setValidDropDownHosts(validDropDownHosts);
    // return availableHosts;
  };

  const onStart = async () => {
    if (startMaintenanceModeOff) {
      await ActionsApi.turnOnOffMaintenance(
        clusterName,
        // @ts-ignore
        serviceName,
        {
          requestInfo: `Turn ${startCase(
            lowerCase("OFF")
          )} Maintenance Mode for ${serviceName}`,
          passive_state: "OFF",
        }
      );
    }
    const payloadData = {
      RequestInfo: {
        context: `_PARSE_.START.${serviceName}`,
        operation_level: {
          level: "SERVICE",
          cluster_name: clusterName,
          service_name: serviceName,
        },
      },
      Body: {
        ServiceInfo: {
          state: `${ServiceActionEnums.startedServiceState}`,
        },
      },
    };
    const response = await ActionsApi.serviceAction(
      clusterName,
      serviceName.toUpperCase(),
      payloadData
    );
    if (response && response.status === 202) {
      setServiceState({
        ...serviceState,
        state: ServiceActionEnums.startedServiceState,
      });
    }
    const requestId = get(response, "data.Requests.id", -1);
    setShowStartConfirmation(false);
    if (requestId !== -1) {
      modalManager.show(
        <BackgroundOperations
          isOpen={true}
          onClose={() => {
            modalManager.hide();
          }}
          requestId={requestId}
        />
      );
    }
  };

  const onStop = async () => {
    if (stopMaintenanceMode) {
      await ActionsApi.turnOnOffMaintenance(
        clusterName,
        // @ts-ignore
        serviceName,
        {
          requestInfo: `Turn ${startCase(
            lowerCase("ON")
          )} Maintenance Mode for ${serviceName}`,
          passive_state: "ON",
        }
      );
    }
    {
      const payloadData = {
        RequestInfo: {
          context: `_PARSE_.STOP.${serviceName}`,
          operation_level: {
            level: "SERVICE",
            cluster_name: clusterName,
            service_name: serviceName,
          },
        },
        Body: {
          ServiceInfo: {
            state: `${ServiceActionEnums.stoppedServiceState}`,
          },
        },
      };
      const response = await ActionsApi.serviceAction(
        clusterName,
        serviceName.toUpperCase(),
        payloadData
      );
      if (response && response.status === 202) {
        setServiceState({
          ...serviceState,
          state: ServiceActionEnums.stoppedServiceState,
        });
      }
      const requestId = get(response, "data.Requests.id", -1);
      setShowStopConfirmation(false);
      if (requestId !== -1) {
        modalManager.show(
          <BackgroundOperations
            isOpen={true}
            onClose={() => {
              modalManager.hide();
            }}
            requestId={requestId}
          />
        );
      }
    }
  };

  const createResourceFilterObjForRestartAll = () => {
    let restartAllResourceFilterObj: any = {
      "Requests/resource_filters": [], // Initialize as an empty array
    };
    const clientComponents =
      serviceModels[
        // @ts-ignore
        serviceNameModelMapping[serviceName]
      ]?.clientComponents;
    const masterComponents =
      serviceModels[
        // @ts-ignore
        serviceNameModelMapping[serviceName]
      ]?.masterComponents;
    const slaveComponents =
      serviceModels[
        // @ts-ignore
        serviceNameModelMapping[serviceName]
      ]?.slaveComponents;

    // Include ALL master components for the service (like Ember version)
    masterComponents.forEach((component: any) => {
      if (component.hostComponents.length > 0) {
        const hosts = component.hostComponents
          .map((hc: any) => hc.HostRoles.host_name)
          .join(",");

        restartAllResourceFilterObj["Requests/resource_filters"].push({
          // @ts-ignore
          service_name: serviceName,
          component_name: component.componentName,
          hosts: hosts,
        });
      }
    });

    // Include ALL client components for the service (like Ember version)
    clientComponents.forEach((component: any) => {
      if (component.hostComponents.length > 0) {
        const hosts = component.hostComponents
          .map((hc: any) => hc.HostRoles.host_name)
          .join(",");

        restartAllResourceFilterObj["Requests/resource_filters"].push({
          // @ts-ignore
          service_name: serviceName,
          component_name: component.componentName,
          hosts: hosts,
        });
      }
    });

    // Include ALL slave components for the service (like Ember version)
    slaveComponents.forEach((component: any) => {
      if (component.hostComponents.length > 0) {
        const hosts = component.hostComponents
          .map((hc: any) => hc.HostRoles.host_name)
          .join(",");

        restartAllResourceFilterObj["Requests/resource_filters"].push({
          // @ts-ignore
          service_name: serviceName,
          component_name: component.componentName,
          hosts: hosts,
        });
      }
    });

    return restartAllResourceFilterObj;
  };

  const onRestartAll = async () => {
    if (stopMaintenanceMode) {
      await ActionsApi.turnOnOffMaintenance(
        clusterName,
        // @ts-ignore
        serviceName,
        {
          requestInfo: `Turn ${startCase(
            lowerCase("ON")
          )} Maintenance Mode for ${serviceName}`,
          passive_state: "ON",
        }
      );
    }
    const resourceFilterReqForServiceRestartAllObj =
      createResourceFilterObjForRestartAll();

    const payloadData = {
      RequestInfo: {
        command: "RESTART",
        context: `Restart all components for ${serviceName}`,
        operation_level: {
          level: "SERVICE",
          cluster_name: clusterName,
          service_name: serviceName,
        },
      },
      "Requests/resource_filters":
        resourceFilterReqForServiceRestartAllObj["Requests/resource_filters"],
    };

    const response = await ActionsApi.actionRequestRebalanceHDFS(
      clusterName,
      payloadData
    );
    if (response && response.status === 202) {
      setServiceState({
        ...serviceState,
        state: ServiceActionEnums.startedServiceState,
      });
    }
    setShowStopConfirmation(false);
    setIsRestartAllSelected(false);
    const requestId = get(response, "data.Requests.id", -1);
    if (requestId !== -1) {
      modalManager.show(
        <BackgroundOperations
          isOpen={true}
          onClose={() => {
            modalManager.hide();
          }}
          requestId={requestId}
        />
      );
    }
  };

  const toggleMaintenanceMode = async () => {
    //@ts-ignore
    await ActionsApi.turnOnOffMaintenance(
      clusterName,
      // @ts-ignore
      serviceName,
      {
        requestInfo: `Turn ${startCase(
          lowerCase(desiredMaintenceState())
        )} Maintenance Mode for ${serviceName}`,
        passive_state: desiredMaintenceState(),
      }
    );
    setModalInfo({
      title: `Information`,
      body: `Maintenance Mode has been turned ${lowerCase(
        desiredMaintenceState()
      )}. It may take a few minutes for the alerts to be suppressed.`,
    });
    setShowConfirmationModal(true);
  };
  const desiredMaintenceState = (): string => {
    return get(serviceState, "maintenance_state", "") === "OFF" ? "ON" : "OFF";
  };

  const restartComponent = (componentName: any) => {
    setComponentToBeRestarted(componentName);

    // Check if the component supports rolling restart
    if (rollingRestartSupportedComponents[componentName]) {
      // Get the host components for this component - check both slave and master components
      const serviceModel = allServiceModels[serviceNameModelMapping[serviceName]];
      
      let hostComponents = 
        serviceModel?.slaveComponents?.find(
          (comp: any) => comp.componentName === componentName
        )?.hostComponents || 
        serviceModel?.masterComponents?.find(
          (comp: any) => comp.componentName === componentName
        )?.hostComponents || [];

      // Get stale configs information from clusterComponents
      const staleConfigsByHost: { [key: string]: boolean } = {};

      if (clusterComponents && clusterComponents.items) {
        clusterComponents.items.forEach((item: any) => {
          if (item.host_components) {
            item.host_components.forEach((component: any) => {
              if (component.HostRoles.component_name === componentName) {
                const hostName = component.HostRoles.host_name;
                staleConfigsByHost[hostName] =
                  component.HostRoles.stale_configs === true;
              }
            });
          }
        });
      }

      // Check if any components have stale configs
      const hasAnyStaleConfigs = Object.values(staleConfigsByHost).some(
        (value) => value === true
      );

      // Format the host components to ensure they have componentName, serviceName, and staleConfigs
      hostComponents = hostComponents.map((component: any) => {
        const hostName = component.HostRoles?.host_name || component.hostName;
        return {
          ...component,
          componentName: componentName,
          serviceName: serviceName,
          hostName: hostName,
          staleConfigs: staleConfigsByHost[hostName] || false,
        };
      });

      // Use the showRollingRestartPopup function
      showRollingRestartPopup(
        componentName,
        serviceName,
        get(serviceState, "maintenance_state", "") !== "OFF",
        hasAnyStaleConfigs,
        hostComponents,
        clusterName
      );
    } else {
      // Use the old modal for components that don't support rolling restart
      setShowRestartComponentConfirmationModal(true);
    }
  };

  const handleChangeBatchSize = (e: React.ChangeEvent<HTMLInputElement>) => {
    const inputBatchSize = e.target.value;
    const newAlertMessages: any[] = [];

    // Check if the value is a number
    if (/^\d*$/.test(inputBatchSize)) {
      setBatchSize(inputBatchSize);
      if (parseInt(inputBatchSize) < 1) {
        newAlertMessages.push("Value must be greater than 0");
      } else if (parseInt(inputBatchSize) > 10000) {
        newAlertMessages.push("Value must be less than 10,000");
      }
    } else {
      newAlertMessages.push("Value must be a number");
    }

    setAlertMessages((prevMessages) => [...prevMessages, ...newAlertMessages]);
    setShowAlert(newAlertMessages.length > 0);
  };

  const handleChangeWaitTimeBetweenBatches = (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    const inputWaitTimeBetweenBatches = e.target.value;
    const newAlertMessages: any[] = [];

    setWaitTimeBetweenBatches(inputWaitTimeBetweenBatches);

    if (!inputWaitTimeBetweenBatches) {
      newAlertMessages.push("Value cannot be empty or invalid");
    } else if (parseInt(inputWaitTimeBetweenBatches) > 10000) {
      newAlertMessages.push("Invalid number: Value must be less than 10,000");
    } else if (parseInt(inputWaitTimeBetweenBatches) < 1) {
      newAlertMessages.push("Invalid restart batch size: value less than 1");
    }

    setAlertMessages((prevMessages) => [...prevMessages, ...newAlertMessages]);
    setShowAlert(newAlertMessages.length > 0);
  };

  const handleChangeTolerateFailures = (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    const inputTolerateFailures = e.target.value;
    const newAlertMessages: any[] = [];

    setTolerateFailures(inputTolerateFailures);

    if (!inputTolerateFailures) {
      newAlertMessages.push(
        "Invalid failure toleration count: cannot be empty or invalid"
      );
    } else if (parseInt(inputTolerateFailures) > 1) {
      newAlertMessages.push(
        "Invalid failure toleration count: value greater than 1"
      );
    }
    setAlertMessages((prevMessages) => [...prevMessages, ...newAlertMessages]);
    setShowAlert(newAlertMessages.length > 0);
  };

  const installedServices = map(services, "ServiceInfo.service_name");
  const isHAEnabled = () => {
    const isHFDSInstalled = installedServices.includes("HDFS");
    const hdfsModel = allServiceModels["hdfs"];
    const hasSNameNode = hdfsModel?.["masterComponents"]?.some(
      (component: any) => {
        return (
          component.component_name === "SECONDARY_NAMENODE" ||
          component.componentName === "SECONDARY_NAMENODE"
        );
      }
    );
    return isHFDSInstalled && !hasSNameNode;
  };
  const canManageJournalNodes = () => {
    const isHFDSInstalled = installedServices.includes("HDFS");
    const hdfsModel = allServiceModels["hdfs"];
    const hasSNameNode = hdfsModel?.["masterComponents"]?.some(
      (component: any) => {
        return (
          component.component_name === "SECONDARY_NAMENODE" ||
          component.componentName === "SECONDARY_NAMENODE"
        );
      }
    );
    const isHAEnabled = isHFDSInstalled && !hasSNameNode;
    if (!isHAEnabled) {
      return false;
    }
    const allJournalNodes = find(hdfsModel?.["slaveComponents"], [
      "componentName",
      "JOURNALNODE",
    ])?.totalCount;
    if (allHostNames.length === 3 && allJournalNodes === 3) {
      return false;
    }
    return true;
  };

  const calcBatchPayload = async (
    batchSize: any,
    componentName: any,
    clusterName: any
  ) => {
    if (!clusterName && !componentName && !batchSize) {
      return;
    }

    const fields =
      "fields=Hosts/host_name,host_components/HostRoles/component_name,host_components/HostRoles/stale_configs,host_components/HostRoles/maintenance_state";
    const hostDetailsResponse = await HostsApi.getHostComponentsDetails(
      clusterName,
      fields
    );

    let componentDetails: any[] = [];
    hostDetailsResponse.items.forEach((host: any) => {
      //filter the host components where the component_name matches
      const componentsOnHosts = host.host_components.filter(
        (component: any) => component.HostRoles.component_name === componentName
      );
      // For each matching component, add the host information
      componentsOnHosts.forEach(() => {
        componentDetails.push({
          hostName: host.Hosts.host_name,
          componentName: componentName,
        });
      });
    });

    const batchCount = Math.ceil(componentDetails.length / batchSize);

    // Initialize the starting index for hosts
    let hostIndex = 0;

    // Array to hold the new batches
    let newBatches = [];

    // Loop over the number of batches
    for (let count = 0; count < batchCount; count++) {
      // Get the host names for the current batch
      const hostNames = componentDetails
        .slice(hostIndex, hostIndex + batchSize)
        .map((detail) => detail.hostName);

      // Increment the host index
      hostIndex += batchSize;

      // If there are host names, create a new batch object
      if (hostNames.length) {
        newBatches.push({
          order_id: count + 1,
          type: "POST",
          uri: `/clusters/${clusterName}/requests`,
          RequestBodyInfo: {
            RequestInfo: {
              context: `_PARSE_.ROLLING-RESTART.${componentName}.${
                count + 1
              }.${batchCount}`,
              command: "RESTART",
            },
            "Requests/resource_filters": [
              {
                service_name: serviceName,
                component_name: componentName,
                hosts: hostNames.join(","),
              },
            ],
          },
        });
      }
    }
    return newBatches;
  };
  //@ts-ignore
  function isClusterHAEnabled() {
    return allServiceModels?.["hdfs"]?.isNameNodeHaEnabled;
  }

  const isRMHAEnabled = () => {
    return allServiceModels?.["yarn"]?.masterComponents?.some(
      (component: any) =>
        component.componentName === "RESOURCEMANAGER" &&
        component.totalCount > 1
    );
  };

  const handleTriggerRollingRestart = async (
    batchSize: any,
    waitTimeBetweenBatches: any,
    tolerateFailures: any
  ) => {
    setShowRestartComponentConfirmationModal(false);
    if (!clusterName && !componentToBeRestarted) {
      return;
    }

    // Get the component display name using the dynamic mapping
    // const componentDisplayName = getSlaveComponentMapping(serviceName)[componentToBeRestarted]

    const batches = await calcBatchPayload(
      batchSize,
      componentToBeRestarted,
      clusterName
    );

    const payloadData = JSON.stringify([
      {
        RequestSchedule: {
          batch: [
            {
              requests: batches,
            },
            {
              batch_settings: {
                batch_separation_in_seconds: waitTimeBetweenBatches,
                task_failure_tolerance: tolerateFailures,
              },
            },
          ],
        },
      },
    ]);

    //@ts-ignore
    const response = await ActionsApi.actionRequest(clusterName, payloadData);
    const requestId = get(response.data.resources[0], "RequestSchedule.id", -1);
    if (requestId !== -1) {
      modalManager.show(
        <BackgroundOperations
          isOpen={true}
          onClose={() => {
            modalManager.hide();
          }}
          requestId={requestId}
        />
      );
    }
  };

  const getClientName = () => {
    const serviceObject = get(
      serviceModels,
      get(serviceNameModelMapping, get(displayNameServiceMapping, serviceName)),
      {}
    );
    return get(serviceObject, "clientComponents.[0].componentName", "");
  };

  const handleThresholdChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    //is a number
    if (
      value === "" ||
      (!isNaN(Number(value)) && Number(value) > 0 && Number(value) <= 100)
    ) {
      setThreshold(value);
    }
  };

  const actionRebalanceHDFS = async () => {
    const nameNodeComponent: any = allServiceModels[
      "hdfs"
    ]?.masterComponents?.find(
      (component: any) => component.componentName === "NAMENODE"
    );

    if (!nameNodeComponent || !nameNodeComponent.hostComponents) {
      console.error("NameNode component or hostComponents not found");
      return;
    }

    const namenodeHostNames = nameNodeComponent?.hostComponents?.map(
      (hostComponent: any) => hostComponent.HostRoles.host_name
    );

    const payloadData = JSON.stringify({
      RequestInfo: {
        context: "Rebalance HDFS",
        command: "REBALANCEHDFS",
        namenode: JSON.stringify({ threshold: threshold }),
      },
      "Requests/resource_filters": [
        {
          service_name: "HDFS",
          component_name: "NAMENODE",
          hosts: namenodeHostNames[0],
        },
      ],
    });
    try {
      const response = await ActionsApi.actionRequestRebalanceHDFS(
        clusterName,
        // @ts-ignore
        payloadData
      );
      if (response.status === 202) {
        const requestId = get(response, "data.Requests.id", -1);
        if (requestId !== -1) {
          modalManager.show(
            <BackgroundOperations
              isOpen={true}
              onClose={() => {
                modalManager.hide();
              }}
              requestId={requestId}
            />
          );
        }
      }
    } catch (error) {
      console.error("Error rebalancing HDFS", error);
      setRebalanceConfirmationModal(false);
    }
  };

  const rebalanceComponent = async () => {
    setThreshold("");
    setRebalanceConfirmationModal(true);
  };

  const actionRunServiceCheck = async () => {
    // const serviceNameData = serviceName
    let payloadData = {};
    if (serviceName === "ZOOKEEPER") {
      payloadData = {
        RequestInfo: {
          // @ts-ignore
          command: `${serviceName}_QUORUM_SERVICE_CHECK`,
          // @ts-ignore
          context: `${serviceName} Service Check`,
          operation_level: payloadOperationLevel,
        },
        "Requests/resource_filters": [
          {
            service_name: serviceName,
          },
        ],
      };
    } else if (serviceName === "TEZ" || serviceName === "SQOOP") {
      payloadData = payloadForClientServiceCheck();
    } else {
      payloadData = {
        RequestInfo: {
          // @ts-ignore
          command: `${serviceName}_SERVICE_CHECK`,
          // @ts-ignore
          context: `${serviceName} Service Check`,
          operation_level: payloadOperationLevel,
        },
        "Requests/resource_filters": [
          {
            service_name: serviceName,
          },
        ],
      };
    }
    const response = await ActionsApi.actionRequestRebalanceHDFS(
      clusterName,
      payloadData
    );
    const requestId = get(response, "data.Requests.id", -1);
    if (requestId !== -1) {
      modalManager.show(
        <BackgroundOperations
          isOpen={true}
          onClose={() => {
            modalManager.hide();
          }}
          requestId={requestId}
        />
      );
    }
  };
  const runServiceCheck = async () => {
    if (!isServiceCheckSupported) {
      console.error("Service check is not supported for this service");
      return;
    }
    modalManager.show(
      <ConfirmationModal
        isOpen={true}
        onClose={() => modalManager.hide()}
        modalTitle="Run Service Check"
        modalBody="Are you sure you want to run the service check?"
        cancellable={true}
        successCallback={async () => {
          modalManager.hide();
          await actionRunServiceCheck();
        }}
      />
    );
  };

  const performComponentAction = async (
    componentName: string,
    componentAction: any,
    command: string,
    params: string,
    serviceName: any,
    clusterName: string
  ) => {
    const fields =
      "fields=Hosts/host_name,host_components/HostRoles/component_name,host_components/HostRoles/stale_configs,host_components/HostRoles/maintenance_state";
    const hostDetailsResponse = await HostsApi.getHostComponentsDetails(
      clusterName,
      fields
    );
    const hostNames = hostDetailsResponse.items.flatMap((host: any) => {
      const matchingComponents = filter(
        host.host_components,
        (component) => component.HostRoles.component_name === componentName
      );
      // Return the host name for each matching component
      return matchingComponents.map(() => host.Hosts.host_name);
    });

    const payloadData = {
      RequestInfo: {
        command: command,
        context: `${componentAction}`,
      },
      "Requests/resource_filters": [
        {
          service_name: serviceName,
          component_name: componentName,
          hosts: hostNames.join(","),
        },
      ],
    };
    if (params) {
      //@ts-ignore
      payloadData.RequestInfo["parameters/forceRefreshConfigTags"] = params;
    }
    const response = await ActionsApi.submitActionRequest(
      clusterName,
      payloadData
    );
    const requestId = get(response, "data.Requests.id", -1);
    if (requestId !== -1) {
      modalManager.show(
        <BackgroundOperations
          isOpen={true}
          onClose={() => {
            modalManager.hide();
          }}
          requestId={requestId}
        />
      );
    }
  };

  const serviceModelHasStartedMasterOrSlaveComps = () => {
    //if it's a client only service, return false else check for master/slave components
    if (checkIfServiceIsClientOnly()) {
      return false;
    }

    const serviceModelForServiceMasterSlave =
      allServiceModels[serviceNameModelMapping[serviceName]];
    const hasStartedComponentsMasterSlave =
      serviceModelForServiceMasterSlave?.masterComponents?.some(
        (component: any) => (component.startedCount > 0 ? true : false)
      ) ||
      serviceModelForServiceMasterSlave?.slaveComponents?.some(
        (component: any) => (component.startedCount > 0 ? true : false)
      );
    return hasStartedComponentsMasterSlave;
  };

  const hasClientComponentsWithInstances = () => {
    const serviceModel = allServiceModels[serviceNameModelMapping[serviceName]];
    const clientComponents = serviceModel?.clientComponents || [];
    return clientComponents.some((comp: any) => (comp.totalCount || 0) > 0);
  };

  const regenerateKeytabsForService = async () => {
    modalManager.show(
      <ConfirmationModal
        isOpen={true}
        onClose={() => modalManager.hide()}
        modalTitle={translate("popup.confirmation.commonHeader")}
        modalBody={translateWithVariables(
          "question.sure.regenerateKeytab.service",
          {
            "0": serviceName || "",
          }
        )}
        cancellable={true}
        successCallback={async () => {
          modalManager.hide();
          await actionRegenerateKeytabs();
        }}
      />
    );
  };

  const actionRegenerateKeytabs = async () => {
    try {
      const payloadData = {
        Clusters: {
          security_type: "KERBEROS",
        },
      };
      getKDCSessionState(async () => {
        const response = await ActionsApi.regenerateKeytabsForService(
          clusterName,
          serviceName,
          payloadData
        );

        const requestId = get(response, "data.Requests.id", -1);
        if (requestId !== -1) {
          modalManager.show(
            <BackgroundOperations
              isOpen={true}
              onClose={() => {
                modalManager.hide();
              }}
              requestId={requestId}
            />
          );
        }
      });
    } catch (error) {
      console.error("Error regenerating keytabs for service:", error);
      modalManager.show(
        <ConfirmationModal
          isOpen={true}
          onClose={() => modalManager.hide()}
          modalTitle="Error"
          modalBody={translateWithVariables(
            "alerts.notifications.regenerateKeytab.service.error",
            {
              "0": serviceName || "",
            }
          )}
          cancellable={false}
          successCallback={async () => {
            modalManager.hide();
          }}
        />
      );
    }
  };
  const serviceModelHasStoppedMasterOrSlaveComps = () => {
    //if it's a client only service, return false else check for master/slave components
    if (checkIfServiceIsClientOnly()) {
      return false;
    }

    const serviceModelForServiceMasterSlave =
      allServiceModels[serviceNameModelMapping[serviceName]];
    const hasStoppedComponentsMasterSlave =
      serviceModelForServiceMasterSlave?.masterComponents?.some(
        (component: any) => {
          const totalCount = component.totalCount || 0;
          const startedCount = component.startedCount || 0;
          return totalCount > startedCount; // Has stopped components
        }
      ) ||
      serviceModelForServiceMasterSlave?.slaveComponents?.some(
        (component: any) => {
          const totalCount = component.totalCount || 0;
          const startedCount = component.startedCount || 0;
          return totalCount > startedCount; // Has stopped components
        }
      );
    return hasStoppedComponentsMasterSlave;
  };

  const refreshClientConfigs = async () => {
    modalManager.show(
      <ConfirmationModal
        isOpen={true}
        onClose={() => modalManager.hide()}
        modalTitle="Refresh Configs"
        modalBody="Are you sure?"
        cancellable={true}
        successCallback={async () => {
          modalManager.hide();
          await actionRefreshClientConfigs();
        }}
      />
    );
  };

  const actionRefreshClientConfigs = async () => {
    try {
      // Get client component hosts
      const serviceModel =
        allServiceModels[serviceNameModelMapping[serviceName]];
      const clientComponents = serviceModel?.clientComponents || [];

      if (clientComponents.length === 0) {
        console.error("No client components found for service:", serviceName);
        return;
      }

      // Create resource filters for each client component
      const resourceFilters = clientComponents
        .map((component: any) => {
          const clientHosts =
            component.hostComponents?.map(
              (hc: any) => hc.HostRoles?.host_name || hc.hostName
            ) || [];

          return {
            service_name: serviceName,
            component_name: component.componentName,
            hosts: clientHosts.join(","),
          };
        })
        .filter((filter: any) => filter.hosts.length > 0); // Only include components that have hosts

      if (resourceFilters.length === 0) {
        console.error("No client hosts found for service:", serviceName);
        return;
      }

      const payloadData = {
        RequestInfo: {
          command: "RESTART",
          context: `Restart all components for ${serviceName}`,
          operation_level: {
            level: "SERVICE",
            cluster_name: clusterName,
            service_name: serviceName,
          },
        },
        "Requests/resource_filters": resourceFilters,
      };

      const response = await ActionsApi.actionRequestRebalanceHDFS(
        clusterName,
        payloadData
      );

      const requestId = get(response, "data.Requests.id", -1);
      if (requestId !== -1) {
        modalManager.show(
          <BackgroundOperations
            isOpen={true}
            onClose={() => {
              modalManager.hide();
            }}
            requestId={requestId}
          />
        );
      }
    } catch (error) {
      console.error("Error refreshing client configs:", error);
    }
  };

  return (
    <div className={`d-flex justify-content-end text-white ${className}`}>
      <ServiceActionsUrlMapping serviceName={serviceName} />
      <Modal
        show={showRestartComponentConfirmationModal}
        onHide={() => setShowRestartComponentConfirmationModal(false)}
        size="lg"
        className="custom-modal-container modal-width"
        data-testid="restart=datanode-confirmation-modal"
      >
        <Modal.Header>
          <Modal.Title>
            <h3>
              Restart{" "}
              {getSlaveComponentMapping(serviceName, allServiceModels)[
                componentToBeRestarted
              ] ||
                ComponentNameEnums[
                  componentToBeRestarted as keyof typeof ComponentNameEnums
                ] ||
                `${componentToBeRestarted}s`}
            </h3>
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Alert variant="success" show={!!componentToBeRestarted}>
            This will restart a specified number of{" "}
            {getSlaveComponentMapping(serviceName, allServiceModels)[
              componentToBeRestarted
            ] ||
              ComponentNameEnums[
                componentToBeRestarted as keyof typeof ComponentNameEnums
              ] ||
              `${componentToBeRestarted}s`}{" "}
            at a time. Note: This will trigger alerts. To suppress alerts, turn
            on Maintenance Mode for HDFS prior to triggering a rolling restart
          </Alert>
          <Row className="d-flex justify-content-end">
            <Col className="d-flex justify-content-end align-items-center">
              Restart
            </Col>
            <Col
              className="d-flex justify-content-start align-items-center"
              md={3}
            >
              <Form className="d-flex w-100">
                <Form.Group controlId="" className="w-100">
                  <Form.Control
                    type="text"
                    size="sm"
                    width={100}
                    value={batchSize}
                    onChange={handleChangeBatchSize}
                    className="d-flex custom-restart-dn-text-input-width w-25"
                  />
                </Form.Group>
              </Form>
            </Col>
            <Col className="d-flex justify-content-start align-items-center">
              {getSlaveComponentMapping(serviceName, allServiceModels)[
                componentToBeRestarted
              ] ||
                ComponentNameEnums[
                  componentToBeRestarted as keyof typeof ComponentNameEnums
                ] ||
                `${componentToBeRestarted}s`}{" "}
              at a time
            </Col>
          </Row>
          <Row className="d-flex justify-content-end mt-2">
            <Col className="d-flex justify-content-end align-items-center">
              Wait
            </Col>
            <Col
              className="d-flex justify-content-start align-items-center"
              md={3}
            >
              <Form className="d-flex w-100">
                <Form.Group controlId="" className="w-100">
                  <Form.Control
                    type="text"
                    size="sm"
                    width={100}
                    value={waitTimeBetweenBatches}
                    onChange={handleChangeWaitTimeBetweenBatches}
                    className="d-flex custom-restart-dn-text-input-width w-25"
                  />
                </Form.Group>
              </Form>
            </Col>
            <Col className="d-flex justify-content-start align-items-center">
              seconds between batches
            </Col>
          </Row>
          <Row className="d-flex justify-content-end mt-2">
            <Col className="d-flex justify-content-end align-items-center">
              Tolerate up to
            </Col>
            <Col
              className="d-flex justify-content-start align-items-center"
              md={3}
            >
              <Form className="d-flex w-100">
                <Form.Group controlId="" className="w-100">
                  <Form.Control
                    type="text"
                    size="sm"
                    width={100}
                    value={tolerateFailures}
                    onChange={handleChangeTolerateFailures}
                    className="d-flex custom-restart-dn-text-input-width w-25"
                  />
                </Form.Group>
              </Form>
            </Col>
            <Col className="d-flex justify-content-start align-items-center">
              restart failures
            </Col>
          </Row>
          <Row>
            <Col>
              <Alert
                variant="danger"
                show={showAlert}
                onClose={() => setShowAlert(false)}
              >
                <ul>
                  {alertMessages.map((message, index) => (
                    <li key={index}>{message}</li>
                  ))}
                </ul>
              </Alert>
            </Col>
          </Row>
          <Row className="d-flex justify-content-between align-items-center mt-2">
            <Col className="d-flex justify-content-center">
              <Form.Group className="mb-3" controlId="formBasicCheckbox">
                <Form.Check 
                  type="checkbox" 
                  id="only-restart-components-checkbox"
                  label="Only restart " 
                />
              </Form.Group>
            </Col>
          </Row>
        </Modal.Body>
        <Modal.Footer className="d-flex justify-content-end">
          <DefaultButton
            size="sm"
            onClick={() => setShowRestartComponentConfirmationModal(false)}
            data-testid="confirm-cancel-btn"
          >
            CANCEL
          </DefaultButton>

          <Button
            className="custom-btn"
            variant="success"
            onClick={() =>
              handleTriggerRollingRestart(
                parseInt(batchSize),
                waitTimeBetweenBatches,
                tolerateFailures
              )
            }
            size="sm"
            data-testid="confirm-ok-btn"
            disabled={showAlert}
          >
            TRIGGER ROLLING RESTART
          </Button>
        </Modal.Footer>
      </Modal>
      <ConfirmationModal
        isOpen={showConfirmationModal}
        onClose={() => setShowConfirmationModal(false)}
        modalTitle={modalInfo.title}
        modalBody={modalInfo.body}
        cancellable={false}
        successCallback={() => setShowConfirmationModal(false)}
      />
      <ConfirmationModal
        isOpen={showStartConfirmation}
        onClose={() => setShowStartConfirmation(false)}
        modalTitle="Confirmation"
        modalBody={
          <div>
            You are about to start {serviceMap[serviceName] || serviceName}
            {get(serviceState, "maintenance_state", "") === "ON" ? (
              <div className="mt-2 d-flex align-items-center">
                <Form.Check
                  type="checkbox"
                  id="start-maintenance-mode-off-checkbox"
                  label={<div className="mt-1">Turn Off Maintenance Mode</div>}
                  checked={startMaintenanceModeOff}
                  onChange={(e) => setStartMaintenanceModeOff(e.target.checked)}
                />
              </div>
            ) : null}
          </div>
        }
        okButtonText="CONFIRM START"
        successCallback={onStart}
      />
      <ConfirmationModal
        isOpen={showStopConfirmation}
        onClose={() => {
          setShowStopConfirmation(false);
          setIsRestartAllSelected(false);
        }}
        modalTitle="Confirmation"
        modalBody={
          <div>
            You are about to{" "}
            {isRestartAllSelected ? "perform restartAll operation for" : "stop"}{" "}
            {serviceMap[serviceName] || serviceName}
            {get(serviceState, "maintenance_state", "") === "OFF" ? (
              <Alert className="my-3" variant="warning">
                This will generate alerts as the service is
                {isRestartAllSelected ? " restarted" : " stopped"}. To suppress
                alerts, turn on Maintenance Mode for{" "}
                {serviceMap[serviceName] || serviceName} prior to{" "}
                {isRestartAllSelected ? "restartAll" : "stopping"}.
              </Alert>
            ) : null}
            {get(serviceState, "maintenance_state", "") === "OFF" ? (
              <div className="mt-2 d-flex align-items-center">
                <Form.Check
                  type="checkbox"
                  id="stop-maintenance-mode-on-checkbox"
                  label={<div className="mt-1">Turn On Maintenance Mode</div>}
                  checked={stopMaintenanceMode}
                  onChange={(e) => setStopMaintenanceMode(e.target.checked)}
                />
              </div>
            ) : null}
          </div>
        }
        okButtonText={
          isRestartAllSelected ? "CONFIRM RESTART ALL" : "CONFIRM STOP"
        }
        successCallback={isRestartAllSelected ? onRestartAll : onStop}
      />
      <ConfirmationModal
        isOpen={rebalanceConfirmationModal}
        onClose={() => setRebalanceConfirmationModal(false)}
        modalTitle="Rebalance HDFS"
        // @ts-ignore
        modalBody={
          <div>
            Balancer threshold (percentage of disk capacity):
            <OverlayTrigger
              placement="bottom"
              overlay={
                <Tooltip id="threshold-tooltip">
                  Percentage of disk capacity. This overwrites the default
                  threshold
                </Tooltip>
              }
            >
              <Form.Control
                type="text"
                className="mt-2 w-25"
                size="sm"
                value={threshold}
                onChange={handleThresholdChange}
              />
            </OverlayTrigger>
          </div>
        }
        cancellable={true}
        successCallback={async () => {
          if (!threshold) {
            alert("Please enter a threshold value.");
            return;
          }
          setRebalanceConfirmationModal(false);
          await actionRebalanceHDFS();
        }}
      />
      <DropdownButton
        id="dropdown-basic-button"
        title="ACTIONS"
        variant="primary"
      >
        {!isServiceClientOnly && (
          <>
            {/* SERVICE.START_STOP authorization check - like Ember.js ui/app/views/main/service/item.js */}
            {canStartStop && (
              <>
                <Dropdown.Item
                  onClick={() => {
                    setShowStartConfirmation(true);
                  }}
                  // @ts-ignore
                  disabled={
                    serviceState.state ===
                      ServiceActionEnums.startedServiceState &&
                    !serviceModelHasStoppedMasterOrSlaveComps()
                  }
                >
                  <FontAwesomeIcon
                    className="text-success me-2"
                    icon={faPlay}
                  />
                  {ServiceActionEnums.startAction}
                </Dropdown.Item>

                <Dropdown.Item
                  onClick={() => {
                    setShowStopConfirmation(true);
                  }}
                  // @ts-ignore
                  disabled={
                    serviceState.state ===
                      ServiceActionEnums.stoppedServiceState &&
                    !serviceModelHasStartedMasterOrSlaveComps()
                  }
                >
                  <FontAwesomeIcon className="text-danger me-2" icon={faStop} />
                  {ServiceActionEnums.stopAction}
                </Dropdown.Item>

                <Dropdown.Item
                  onClick={async () => {
                    setIsRestartAllSelected(true);
                    setShowStopConfirmation(true);
                  }}
                >
                  <FontAwesomeIcon
                    className="text-secondary me-2"
                    icon={faClock}
                  />
                  {ServiceActionEnums.restartAllAction}
                </Dropdown.Item>
              </>
            )}

            {/* SERVICE.TOGGLE_MAINTENANCE authorization check */}
            {hasAuthorization("SERVICE.TOGGLE_MAINTENANCE") && (
              <Dropdown.Item onClick={toggleMaintenanceMode}>
                <FontAwesomeIcon
                  className="text-secondary me-2"
                  icon={faMedkit}
                />
                {desiredMaintenceState() === "ON"
                  ? ServiceActionEnums.turnOnMaintenanceMode
                  : ServiceActionEnums.turnOffMaintenanceMode}
              </Dropdown.Item>
            )}
          </>
        )}
        {/* To restart any component based on component action mappings and service name*/}
        {(() => {
          // If still loading component details, show a loading indicator
          if (isLoadingComponentDetails) {
            return (
              <DropdownItem disabled>Loading component actions...</DropdownItem>
            );
          }

          // Get dynamic component actions
          const dynamicComponentActions = getDynamicComponentActions(
            serviceName,
            allServiceModels
          );

          // Combine with static actions
          const combinedComponentActions = [
            ...(ComponentActionsMapping[
              serviceName as keyof typeof ComponentActionsMapping
            ] || []),
            ...dynamicComponentActions,
          ];

          // Remove duplicates
          const uniqueComponentActions = combinedComponentActions.filter(
            (action, index, self) =>
              index === self.findIndex((a) => a.component === action.component)
          );

          // Filter out components that don't support rolling restart, but keep explicitly mapped components
          // Also filter out components with installedCount = 0
          const filteredComponentActions = uniqueComponentActions.filter(
            (action) => {
              // Check if component has installedCount > 0
              const serviceModel =
                allServiceModels[serviceNameModelMapping[serviceName]];
              
              // Check both slave and master components for installed instances
              const slaveComponent = serviceModel?.slaveComponents?.find(
                (comp: any) => comp.componentName === action.component
              );
              const masterComponent = serviceModel?.masterComponents?.find(
                (comp: any) => comp.componentName === action.component
              );
              
              const hasInstalledInstances =
                (slaveComponent?.totalCount || 0) > 0 ||
                (masterComponent?.totalCount || 0) > 0;

              // Don't show restart option if component has no installed instances
              if (!hasInstalledInstances) {
                return false;
              }

              // Always include components that are explicitly defined in ComponentActionsMapping
              const isExplicitlyMapped = ComponentActionsMapping[
                serviceName as keyof typeof ComponentActionsMapping
              ]?.some(
                (mappedAction) => mappedAction.component === action.component
              );

              // Include if explicitly mapped OR if rolling restart is supported
              return (
                isExplicitlyMapped ||
                rollingRestartSupportedComponents[action.component] !== false
              );
            }
          );

          // Return the mapped components
          return filteredComponentActions.map((componentAction) => (
            <DropdownItem
              key={componentAction.component}
              onClick={() => restartComponent(componentAction.component)}
            >
              <FontAwesomeIcon className="text-secondary me-2" icon={faClock} />
              {
                //@ts-ignore
                componentAction.actionMap.actionRestart
                  ? //@ts-ignore
                    componentAction.actionMap.actionRestart
                  : componentAction.component === "NAMENODE"
                  ? // @ts-ignore
                    componentAction.actionMap.actionRebalanceHDFS
                  : null
              }
            </DropdownItem>
          ));
        })()}
        {/* High Availability Features - Requires SERVICE.ENABLE_HA authorization */}
        {canEnableHA && serviceName === "HDFS" && !isHAEnabled() && (
          <EnableHighAvailibilityNameNode />
        )}
        {/* Add New HDFS Namespace (Federation) - Requires SERVICE.ENABLE_HA authorization, HDFS service, and FEDERATION serviceType (matches Ember.js logic) */}
        {canEnableHA && serviceName === "HDFS" && (
          <EnableNamenodeFederation />
        )}
        {/* Manage JournalNodes - No specific authorization required, only HA state and host count checks */}
        {serviceName === "HDFS" && canManageJournalNodes() && (
          <ManageJournalNodes />
        )}
        {canEnableHA && serviceName === "YARN" && !isRMHAEnabled() && (
          <EnableHighAvailibilityResourceManger />
        )}
        {canEnableHA && serviceName === "RANGER" && (
          <EnableHighAvailibilityRangerAdmin />
        )}

        {/* Component Reassignment - Requires SERVICE.MOVE authorization */}
        {canMoveComponents &&
          (serviceName === "HDFS" ||
            serviceName === "YARN" ||
            serviceName === "HIVE" ||
            serviceName === "AMBARI_METRICS" ||
            serviceName === "SSM" ||
            serviceName === "HIVE" ||
            serviceName === "MAPREDUCE2") && (
            <ReassignComponent serviceName={serviceName} />
          )}

        {hasAuthorization("CLUSTER.VIEW_CONFIGS") &&
          serviceName !== "KYUUBI" &&
          serviceName !== "TRINO_GATEWAY" &&
          hasClientComponentsWithInstances() && (
            <Dropdown.Item
              onClick={() => {
                downloadClientConfigsCall({
                  clusterName: clusterName,
                  componentName: getClientName(),
                  serviceName: serviceName,
                  resourceType: ResourceTypeEnum.SERVICE_COMPONENT,
                });
              }}
            >
              <FontAwesomeIcon
                className="text-secondary me-2"
                icon={faDownload}
              />
              <span>
                {get(
                  messages,
                  "services.service.actions.downloadClientConfigs"
                )}
              </span>
            </Dropdown.Item>
          )}

        {serviceName === "HDFS" ? (
          <DropdownItem
            // key={componentAction.component}
            onClick={() => rebalanceComponent()}
          >
            <FontAwesomeIcon className="text-secondary me-2" icon={faRefresh} />

            {
              // @ts-ignore
              ServiceActionEnums.rebalanceHDFS
            }
          </DropdownItem>
        ) : null}
        {/* Component Addition Actions - Requires HOST.ADD_DELETE_COMPONENTS authorization */}
        {canAddDeleteComponents && serviceName === "HIVE" && (
          <>
            <Dropdown.Item
              disabled={
                isEmpty(serviceModels["hive"]?.masterComponents) ||
                !checkComponentAdditionEnabled("HIVE_METASTORE", "hive")
              }
              onClick={async () => {
                await getAllHosts();
                await getHostsWithoutComponent("HIVE_METASTORE", "hive");

                const component = {
                  componentName: "HIVE_METASTORE",
                  //@ts-ignore
                  serviceName: serviceName,
                  //@ts-ignore
                  displayName: serviceName,
                  component_category: "MASTER",
                  hostName: validDropDownHosts[0] || "", // Default to first valid host or empty string
                  isInstalled: false,
                  clusterName: clusterName,
                };
                const data = {
                  addAndReconfigureComponent,
                  getKDCSessionState,
                  clusterComponents,
                  fromServiceSummary: true,
                  validDropDownHosts: validDropDownHosts,
                };
                //from this call all the necessary props will be set to be passed to the recommendation modal
                //@ts-ignore
                await addComponentWithCheck(component, data);
              }}
            >
              <FontAwesomeIcon className="text-secondary me-2" icon={faPlus} />
              Add Hive Metastore
            </Dropdown.Item>
            <Dropdown.Item
              disabled={
                isEmpty(serviceModels["hive"]?.masterComponents) ||
                !checkComponentAdditionEnabled("HIVE_SERVER", "hive")
              }
              onClick={async () => {
                await getAllHosts();
                await getHostsWithoutComponent("HIVE_SERVER", "hive");

                const component = {
                  componentName: "HIVE_SERVER",
                  //@ts-ignore
                  serviceName: serviceName,
                  //@ts-ignore
                  displayName: serviceName,
                  component_category: "MASTER",
                  hostName: validDropDownHosts[0] || "", // Default to first valid host or empty string
                  isInstalled: false,
                  clusterName: clusterName,
                };
                const data = {
                  addAndReconfigureComponent,
                  getKDCSessionState,
                  clusterComponents,
                  fromServiceSummary: true,
                  validDropDownHosts: validDropDownHosts,
                };
                //@ts-ignore
                await addComponentWithCheck(component, data);
              }}
            >
              <FontAwesomeIcon className="text-secondary me-2" icon={faPlus} />
              Add HiveServer2
            </Dropdown.Item>
          </>
        )}
        {canAddDeleteComponents && serviceName === "RANGER_KMS" && (
          <Dropdown.Item
            disabled={
              isEmpty(serviceModels["ranger_kms"]?.masterComponents) ||
              !checkComponentAdditionEnabled("RANGER_KMS_SERVER", "ranger_kms")
            }
            onClick={async () => {
              await getAllHosts();
              await getHostsWithoutComponent("RANGER_KMS_SERVER", "ranger_kms");

              const component = {
                componentName: "RANGER_KMS_SERVER",
                //@ts-ignore
                serviceName: serviceName,
                //@ts-ignore
                displayName: serviceName,
                component_category: "MASTER",
                hostName: validDropDownHosts[0] || "", // Default to first valid host or empty string
                isInstalled: false,
                clusterName: clusterName,
              };
              const data = {
                addAndReconfigureComponent,
                getKDCSessionState,
                clusterComponents,
                fromServiceSummary: true,
                validDropDownHosts: validDropDownHosts,
              };
              //@ts-ignore
              await addComponentWithCheck(component, data);
            }}
          >
            <FontAwesomeIcon className="text-secondary me-2" icon={faPlus} />
            Add Ranger KMS Server
          </Dropdown.Item>
        )}
        {canAddDeleteComponents && serviceName === "HBASE" && (
          <Dropdown.Item
            disabled={
              isEmpty(serviceModels["hbase"]?.masterComponents) ||
              !checkComponentAdditionEnabled("HBASE_MASTER", "hbase")
            }
            onClick={async () => {
              // const handleHostChange = async (newHostName: any) => {
              await getAllHosts();
              await getHostsWithoutComponent("HBASE_MASTER", "hbase");

              const component = {
                componentName: "HBASE_MASTER",
                //@ts-ignore
                serviceName: serviceName,
                //@ts-ignore
                displayName: serviceName,
                component_category: "MASTER",
                hostName: validDropDownHosts[0] || "",
                isInstalled: false,
                clusterName: clusterName,
              };
              const data = {
                addAndReconfigureComponent,
                getKDCSessionState,
                clusterComponents,
                fromServiceSummary: true,
                validDropDownHosts: validDropDownHosts,
              };
              //@ts-ignore
              await addComponentWithCheck(component, data);
            }}
          >
            <FontAwesomeIcon className="text-secondary me-2" icon={faPlus} />
            Add HBase Master
          </Dropdown.Item>
        )}
        {canAddDeleteComponents && serviceName === "ZOOKEEPER" && (
          <Dropdown.Item
            disabled={
              isEmpty(serviceModels["zk"]?.masterComponents) ||
              !checkComponentAdditionEnabled("ZOOKEEPER_SERVER", "zk")
            }
            onClick={async () => {
              // const handleHostChange = async (newHostName: any) => {
              await getAllHosts();
              await getHostsWithoutComponent("ZOOKEEPER_SERVER", "zk");

              const component = {
                componentName: "ZOOKEEPER_SERVER",
                //@ts-ignore
                serviceName: serviceName,
                //@ts-ignore
                displayName: serviceName,
                component_category: "MASTER",
                hostName: validDropDownHosts[0] || "", // Default to first valid host or empty string
                isInstalled: false,
                clusterName: clusterName,
              };
              const data = {
                addAndReconfigureComponent,
                getKDCSessionState,
                clusterComponents,
                fromServiceSummary: true,
                validDropDownHosts: validDropDownHosts,
              };
              //@ts-ignore
              await addComponentWithCheck(component, data);
            }}
          >
            <FontAwesomeIcon className="text-secondary me-2" icon={faPlus} />
            Add Zookeeper Server
          </Dropdown.Item>
        )}
        {serviceName === "HDFS" ? (
          <DropdownItem
            onClick={async () => {
              //show popup modal
              //@ts-ignore
              let performActionParams: any;

              if (serviceName === "HDFS") {
                // displayText = "Refresh Nodes";
                performActionParams = [
                  "NAMENODE",
                  ServiceActionEnums.executeRefreshNodes,
                  "REFRESH_NODES",
                  "",
                  serviceName,
                  clusterName,
                ];
              }
              //@ts-ignore
              await performComponentAction(...performActionParams);
            }}
          >
            <FontAwesomeIcon
              className="text-secondary me-2"
              icon={faPlayCircle}
            />

            {ServiceActionEnums.refreshNodes}
          </DropdownItem>
        ) : null}
        {serviceName === "YARN" ? (
          <DropdownItem
            onClick={async () => {
              //show popup modal
              //@ts-ignore
              let performActionParams: any;

              if (serviceName === "YARN") {
                performActionParams = [
                  "RESOURCEMANAGER",
                  ServiceActionEnums.refreshYarnCapacityScheduler,
                  "REFRESHQUEUES",
                  "",
                  serviceName,
                  clusterName,
                ];
              }
              //@ts-ignore
              await performComponentAction(...performActionParams);
            }}
          >
            <FontAwesomeIcon
              className="text-secondary me-2"
              icon={faPlayCircle}
            />
            {ServiceActionEnums.refreshYarnCapacityScheduler}
          </DropdownItem>
        ) : null}
        {/* Regenerate Keytabs - Requires SERVICE.RUN_CUSTOM_COMMAND authorization, Kerberos enabled, and NOT client-only service */}
        {!isServiceClientOnly && canRunCustomCommands && isKerberosEnabled && (
          <DropdownItem onClick={() => regenerateKeytabsForService()}>
            <FontAwesomeIcon className="text-secondary me-2" icon={faRefresh} />
            Regenerate Keytabs
          </DropdownItem>
        )}

        {/* Service Deletion - Requires SERVICE.ADD_DELETE_SERVICES authorization */}
        {canAddDeleteServices && (
          <DropdownItem
            onClick={() => {
              const serviceModel =
                allServiceModels[serviceNameModelMapping[serviceName]];

              // Check if Kerberos - show specific error
              if (
                serviceModel?.serviceName?.toLowerCase() === "kerberos" ||
                serviceName?.toLowerCase() === "kerberos"
              ) {
                modalManager.show(
                  <ConfirmationModal
                    isOpen={true}
                    onClose={() => modalManager.hide()}
                    modalTitle="Delete Service"
                    modalBody="Kerberos service cannot be deleted from service actions. Please use the Kerberos wizard to disable security."
                    cancellable={false}
                    successCallback={() => modalManager.hide()}
                  />
                );
                return;
              }

              // Check if service has started components
              const hasStartedMasterComponents =
                serviceModel?.masterComponents?.some(
                  (component: any) => (component.startedCount || 0) > 0
                ) || false;

              const hasStartedSlaveComponents =
                serviceModel?.slaveComponents?.some(
                  (component: any) => (component.startedCount || 0) > 0
                ) || false;

              const hasStartedComponents =
                hasStartedMasterComponents || hasStartedSlaveComponents;

              // Check service state
              const allowedServiceStates = [
                "INIT",
                "INSTALL_FAILED",
                "INSTALLED",
                "UNKNOWN",
              ];
              const serviceStateAllowsDeletion = allowedServiceStates.includes(
                serviceModel?.serviceState
              );

              // Show error if service/components are started or service state doesn't allow deletion
              if (!serviceStateAllowsDeletion || hasStartedComponents) {
                modalManager.show(
                  <ConfirmationModal
                    isOpen={true}
                    onClose={() => modalManager.hide()}
                    modalTitle="Delete Service"
                    modalBody={`Prior to deleting ${serviceName}, you must stop the service and each slave and master component.`}
                    cancellable={false}
                    successCallback={() => modalManager.hide()}
                  />
                );
                return;
              }

              // Service can be deleted - directly call deleteService like Ember.js
              deleteService();
            }}
          >
            <FontAwesomeIcon
              className="text-danger me-2"
              icon={faTimesCircle}
            />
            {ServiceActionEnums.deleteServiceAction}
          </DropdownItem>
        )}

        {/* Refresh Configs for Client-Only Services */}
        {isServiceClientOnly && canRunCustomCommands && (
          <DropdownItem onClick={() => refreshClientConfigs()}>
            <FontAwesomeIcon className="text-secondary me-2" icon={faRefresh} />
            {ServiceActionEnums.refreshConfigs}
          </DropdownItem>
        )}

        {/* Service Check - Requires SERVICE.RUN_CUSTOM_COMMAND, SERVICE.RUN_SERVICE_CHECK, SERVICE.TOGGLE_MAINTENANCE, or SERVICE.ENABLE_HA authorization (matches Ember.js logic) */}
        {(hasAuthorization("SERVICE.RUN_CUSTOM_COMMAND") || 
          hasAuthorization("SERVICE.RUN_SERVICE_CHECK") || 
          hasAuthorization("SERVICE.TOGGLE_MAINTENANCE") || 
          hasAuthorization("SERVICE.ENABLE_HA")) && (
          <DropdownItem
            // key={serviceName}
            onClick={() => runServiceCheck()}
            disabled={!isServiceCheckSupported}
          >
            <FontAwesomeIcon className="text-muted me-2" icon={faThumbsUp} />
            {ServiceActionEnums.runServiceCheck}
          </DropdownItem>
        )}
      </DropdownButton>
    </div>
  );
};
