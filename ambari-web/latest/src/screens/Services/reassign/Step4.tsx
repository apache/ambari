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
import { useParams } from "react-router-dom";
import { Alert, Button } from "react-bootstrap";
import { ReassignContext } from "./store/context";
import { ActionTypes } from "./store/types";
import WizardFooter from "../../../components/StepWizard/WizardFooter";
import {
  componentsWithCheckDBStep,
  reassignSteps,
  relatedServicesMap,
  serviceToConfigSiteMap,
} from "./constants";
import ClusterApi from "../../../api/clusterApi";
import { ServiceContext } from "../../../store/ServiceContext";
import { AppContext } from "../../../store/context";
import { cloneDeep, filter, find, get, isEmpty, map } from "lodash";
import {
  getStepData,
  translate,
  translateWithVariables,
} from "../../../Utils/Utility";
import { HostsApi } from "../../../api/hostsApi";
import { ServiceApi } from "../../../api/serviceApi";
import {
  stopServices,
} from "../../../Utils/taskUtils";
import { RequestApi } from "../../../api/requestApi";
import Spinner from "../../../components/Spinner";
import OperationsProgress from "../../../components/OperationsProgress";
import InvalidKDCPopup from "../../Kerberos/InvalidKdcPopup";
import KerberosApi from "../../../api/kerberosApi";
import useKDCSessionState from "../../../hooks/useKDCSessionState";
import modalManager from "../../../store/ModalManager";
import { isMissingHostComponentError } from "../../../Utils/reassignValidation";

interface ReassignData {
  component_name: string;
  service_id: string;
  sourceHost: string;
  targetHost: string;
  hasManualSteps: boolean;
}

function Step4() {
  const { componentName } = useParams<{ componentName: string }>();
  const { allServiceModels } = useContext(ServiceContext);
  const { services, serviceComponentInfo, clusterName, ambariProperties } =
    useContext(AppContext);
  const {
    state,
    dispatch,
    flushStateToDb,
    hasManualCommands,
    stepWizardUtilities: { currentStep, handleNextImperitive, jumpToStep },
  } = useContext(ReassignContext);
  const installedServices = map(services, "ServiceInfo.service_name");

  // Check if this is the final step (no manual commands)
  const isLastStep = !hasManualCommands;

  // State management
  const [tasks, setTasks] = useState<any[]>([]);
  const [isLoaded, setIsLoaded] = useState(false);
  const [completionStatus, setCompletionStatus] = useState(false);
  const [allHostComponents, setAllHostComponents] = useState<any>([]);
  const [allServiceComponents, setAllServiceComponents] = useState<any>([]);
  const [hostComponents, setHostComponents] = useState<string[]>([]);
  const [dependentHostComponents, setDependentHostComponents] = useState<
    string[]
  >([]);
  const { getKDCSessionState } = useKDCSessionState(null);
  const [showInvalidKDCPopup, setShowInvalidKDCPopup] = useState(false);
  const [stepOperations, setStepOperations] = useState<any>([]);
  const [rollbackInProgress, setRollbackInProgress] = useState(false);
  const [rollbackError, setRollbackError] = useState("");

  const assignMastersData = getStepData(
    state,
    reassignSteps.ASSIGN_MASTER,
    "masterComponentHosts",
    "reassignSteps"
  );

  const standardCommands = [
    "stopRequiredServices",
    "cleanMySqlServer",
    "createHostComponents",
    "putHostComponentsInMaintenanceMode",
    "reconfigure",
    "installHostComponents",
    "startZooKeeperServers",
    "startNameNode",
    "stopHostComponentsInMaintenanceMode",
    // "stopSourceHostComponents",
    "deleteHostComponents",
    "configureMySqlServer",
    "startMySqlServer",
    "startNewMySqlServer",
    "startRequiredServices",
  ];

  const dbCommands = [
    "createHostComponents",
    "installHostComponents",
    "configureMySqlServer",
    "restartMySqlServer",
    "testDBConnection",
    "stopRequiredServices",
    "cleanMySqlServer",
    "putHostComponentsInMaintenanceMode",
    "reconfigure",
    "stopHostComponentsInMaintenanceMode",
    // "stopSourceHostComponents",
    "deleteHostComponents",
    "configureMySqlServer",
    "startRequiredServices",
  ];

  useEffect(() => {
    getAllHostComponents();
    getAllServiceComponents();
  }, []);

  useEffect(() => {
    if (!isEmpty(allHostComponents) && !isEmpty(allServiceComponents)) {
      initializeHostComponents();
      initializeDependentHostComponents();
    }
  }, [allHostComponents, allServiceComponents]);

  useEffect(() => {
    if (hostComponents.length > 0 && !isEmpty(serviceComponentInfo)) {
      initializeTasks();
      checkKdcCredentials();
    }
  }, [hostComponents, serviceComponentInfo]);

  const checkKdcCredentials = async () => {
    try {
      console.log("Checking KDC credentials...");
      // Try to get KDC credentials
      await KerberosApi.getKDCAdminCredentials(clusterName);
      console.log("KDC credentials are valid.");
      // If successful, proceed with operations
      setIsLoaded(true);
    } catch (error) {
      console.error("KDC credentials check failed:", error);
      // If failed, show KDC credentials popup
      setShowInvalidKDCPopup(true);
    }
  };

  const handleSaveInvalidKDC = async (
    adminPrincipal: string,
    adminPassword: string,
    saveCredentials: boolean
  ) => {
    setShowInvalidKDCPopup(false);
    const payload = {
      Credential: {
        key: adminPassword,
        principal: adminPrincipal,
        type: saveCredentials ? "persisted" : "temporary",
      },
    };

    try {
      await KerberosApi.postKDCAdminCredentials(clusterName, payload);

      // Retry the operation that failed
      // This will depend on which operation failed, but likely it's one of the tasks
      // For now, let's assume we need to restart the task list execution
      setTasks(tasks);
      setIsLoaded(true);
    } catch (error) {
      console.error("Error posting KDC Admin Credentials:", error);
    }
  };

  const getAllHostComponents = async () => {
    const response = await HostsApi.getHostComponentsDetails(
      clusterName,
      "fields=Hosts/host_name,host_components/HostRoles/component_name,host_components/HostRoles/stale_configs,host_components/HostRoles/maintenance_state"
    );
    setAllHostComponents(response.items);
  };

  const getAllServiceComponents = async () => {
    const response = await HostsApi.getClusterComponents(
      clusterName,
      "ServiceComponentInfo/component_name,host_components/HostRoles/host_name"
    );
    setAllServiceComponents(response.items);
  };

  const getReassignData = (): ReassignData => {
    const sourceHost = find(
      assignMastersData,
      (master: any) =>
        master.component_name === componentName &&
        master.isMoving &&
        master.movedHost
    )?.hostName;
    const targetHost = find(
      assignMastersData,
      (master: any) =>
        master.component_name === componentName &&
        master.isMoving &&
        master.movedHost
    )?.movedHost;

    return {
      component_name: componentName || "",
      service_id: getServiceForComponent(componentName || ""),
      sourceHost: sourceHost,
      targetHost: targetHost,
      hasManualSteps: hasManualCommands,
    };
  };

  const getServiceForComponent = (component: string): string => {
    if (!serviceComponentInfo || !serviceComponentInfo.items) {
      return "";
    }

    for (const service of serviceComponentInfo.items) {
      if (!service.components) {
        continue;
      }
      const componentNames = service.components.map(
        (comp: any) => comp.StackServiceComponents.component_name
      );
      if (componentNames.includes(component)) {
        return service.StackServices.service_name;
      }
    }

    return "";
  };

  const isComponentWithDB = () => {
    return componentsWithCheckDBStep.includes(componentName || "");
  };

  const initializeHostComponents = () => {
    if (!componentName) return;

    let components = [componentName];

    if (componentName === "NAMENODE") {
      const isHaEnabled = get(
        allServiceModels,
        "hdfs.isNameNodeHaEnabled",
        false
      );
      if (isHaEnabled) {
        components = ["NAMENODE", "ZKFC"];
      }
    }

    setHostComponents(components);
  };

  const getTargetHostComponents = (targetHost: string) => {
    return find(allHostComponents, (host: any) => {
      return host?.Hosts?.host_name === targetHost;
    })?.host_components;
  };

  const initializeDependentHostComponents = () => {
    if (!componentName || !serviceComponentInfo) return;

    const installedServices = services.map(
      (service: any) => service?.ServiceInfo?.service_name
    );

    const targetHost = getReassignData().targetHost;

    const hostInstalledComponents = getTargetHostComponents(targetHost)?.map(
      (comp: any) => comp?.HostRoles?.component_name
    );

    const clusterInstalledComponents = filter(
      allServiceComponents,
      (comp: any) => comp?.host_components?.length > 0
    ).map((comp: any) => comp?.ServiceComponentInfo?.component_name);

    let componentDependencies: Array<{
      componentName: string;
      serviceName: string;
      scope: string;
    }> = [];

    if (serviceComponentInfo.items) {
      for (const service of serviceComponentInfo.items) {
        if (service.components) {
          for (const component of service.components) {
            if (
              component.StackServiceComponents?.component_name === componentName
            ) {
              componentDependencies = get(component, "dependencies", []).map(
                (dep: any) => ({
                  componentName: dep?.Dependencies?.component_name,
                  serviceName: dep?.Dependencies?.service_name,
                  scope: dep?.Dependencies?.scope,
                })
              );
              break;
            }
          }
        }
        if (componentDependencies.length > 0) break;
      }
    }

    const dependenciesToInstall = componentDependencies
      .filter((dependency) => {
        if (!installedServices.includes(dependency.serviceName)) {
          return false;
        }

        const isInstalled =
          dependency.scope === "host"
            ? hostInstalledComponents.includes(dependency.componentName)
            : clusterInstalledComponents.includes(dependency.componentName);

        if (componentName === "NAMENODE") {
          const isHaEnabled = get(
            allServiceModels,
            "hdfs.isNameNodeHaEnabled",
            false
          );
          return !isInstalled && isHaEnabled;
        }

        return !isInstalled;
      })
      .map((dep) => dep.componentName);

    setDependentHostComponents(dependenciesToInstall);
  };

  const isComponentWithReconfiguration = (component: string) => {
    return Object.keys(serviceToConfigSiteMap).includes(component);
  };

  const getReassignComponentsInMM = () => {
    const targetHost = getReassignData().targetHost;

    const hostInstalledComponents = getTargetHostComponents(targetHost);
    return (
      hostInstalledComponents?.filter(
        (comp: any) => comp?.HostRoles?.maintenance_state === "ON"
      ) || []
    );
  };

  const removeUnneededTasks = (taskList: any[]) => {
    const isHaEnabled = get(
      allServiceModels,
      "hdfs.isNameNodeHaEnabled",
      false
    );
    const reassignData = getReassignData();
    let filteredTasks = cloneDeep(taskList);

    if (isComponentWithDB()) {
      const isRemoteDb =
        getStepData(
          state,
          reassignSteps.GET_STARTED,
          "isRemoteDb",
          "reassignSteps"
        ) || true;
      const dbType =
        getStepData(
          state,
          reassignSteps.GET_STARTED,
          "dbType",
          "reassignSteps"
        ) || "mysql";

      if (isRemoteDb || dbType !== "mysql") {
        filteredTasks = filteredTasks.filter(
          (task) =>
            ![
              "configureMySqlServer",
              "startMySqlServer",
              "restartMySqlServer",
              "cleanMySqlServer",
            ].includes(task.command)
        );
      }

      if (dbType === "derby") {
        filteredTasks = filteredTasks.filter(
          (task) => task.command !== "testDBConnection"
        );
      }
    }

    if (componentName !== "MYSQL_SERVER" && !isComponentWithDB()) {
      filteredTasks = filteredTasks.filter(
        (task) =>
          ![
            "configureMySqlServer",
            "startMySqlServer",
            "restartMySqlServer",
            "cleanMySqlServer",
            "startNewMySqlServer",
          ].includes(task.command)
      );
    }

    if (componentName === "MYSQL_SERVER") {
      filteredTasks = filteredTasks.filter(
        (task) => task.command !== "cleanMySqlServer"
      );
    }

    if (reassignData.hasManualSteps) {
      if (componentName === "NAMENODE" && isHaEnabled) {
        // For NameNode HA with manual steps, keep startZooKeeperServers and startNameNode
        // Only remove the post-manual-step tasks
        filteredTasks = filteredTasks.filter(
          (task) =>
            ![
              "stopHostComponentsInMaintenanceMode",
              "deleteHostComponents",
              "startRequiredServices",
            ].includes(task.command)
        );
      } else {
        // For non-HA components with manual steps, remove all these tasks
        filteredTasks = filteredTasks.filter(
          (task) =>
            ![
              "startZooKeeperServers",
              "startNameNode",
              "stopHostComponentsInMaintenanceMode",
              "deleteHostComponents",
              "startRequiredServices",
            ].includes(task.command)
        );
      }
    } else {
      // For components without manual steps, don't need these as they're handled in Step 6
      filteredTasks = filteredTasks.filter(
        (task) =>
          !["startZooKeeperServers", "startNameNode"].includes(task.command)
      );
    }

    if (!isComponentWithReconfiguration(componentName || "")) {
      filteredTasks = filteredTasks.filter(
        (task) => task.command !== "reconfigure"
      );
    }

    if (getReassignComponentsInMM().length == 0) {
      filteredTasks = filteredTasks.filter(
        (task) => task.command !== "stopHostComponentsInMaintenanceMode"
      );
    }

    return filteredTasks;
  };

  const initializeTasks = () => {
    if (!componentName) return;

    const commands = isComponentWithDB() ? dbCommands : standardCommands;
    const hostComponentsNames = getHostComponentsNames();
    const serviceName = allServiceComponents.find(
      (comp: any) =>
        comp?.ServiceComponentInfo?.component_name === componentName
    )?.ServiceComponentInfo?.service_name;

    const initialTasks: any[] = commands.map((command, index) => {
      let taskLabel = hostComponentsNames;
      if (index === 3) {
        taskLabel = serviceName || "";
      }
      
      const title = translateWithVariables(
        "services.reassign.step4.tasks." + command + ".title",
        { "0": taskLabel, "1": serviceName || "" }
      ) as string;
      return {
        title: title,
        status: "PENDING",
        id: index,
        command: command,
        showRetry: false,
        showRollback: false,
        name: title,
        displayName: title,
        progress: 0,
        isRunning: false,
        hosts: [],
      };
    });

    const filteredTasks = removeUnneededTasks(initialTasks);
    setTasks(filteredTasks);
    setIsLoaded(true);
  };

  function formatComponentName(componentName: string): string {
    return componentName
      .replace(/_/g, " ")
      .toLowerCase()
      .replace(/\b\w/g, (l) => l.toUpperCase());
  }

  const getHostComponentsNames = () => {
    let hostComponentsNames = "";
    hostComponents.forEach((comp, index) => {
      hostComponentsNames += index ? "+" : "";
      hostComponentsNames += formatComponentName(comp)
    });
    return hostComponentsNames;
  };

  function getRestartingServices() {
    let listOfServices = [];
    const componentsToStopAllServices = ["NAMENODE", "SECONDARY_NAMENODE"];
    const installedServices = map(services, "ServiceInfo.service_name");
    if (componentsToStopAllServices.includes(componentName!)) {
      listOfServices = installedServices;
    } else {
      listOfServices = relatedServicesMap[componentName!] || [];
      if (!listOfServices.length) {
        listOfServices = installedServices.filter(function (service) {
          return service != "HDFS";
        });
      } else {
        const installedServicesToStop = listOfServices.filter(function (
          service: any
        ) {
          return installedServices.includes(service);
        });
        listOfServices = installedServicesToStop;
      }
    }
    return listOfServices;
  }

  const stopRequiredServices = async () => {
    const servicesToStop = getRestartingServices();
    return await stopServices(
      clusterName,
      servicesToStop,
      true,
      false,
      installedServices
    );
  };

  const cleanMySqlServer = async () => {
    const reassignData = getReassignData();
    let hostname = "";

    const mysqlComponent = allHostComponents.find((host: any) =>
      host.host_components?.some(
        (comp: any) => comp.HostRoles?.component_name === "MYSQL_SERVER"
      )
    );

    if (reassignData.component_name === "MYSQL_SERVER") {
      hostname = reassignData.targetHost;
    } else if (mysqlComponent) {
      hostname = mysqlComponent.Hosts?.host_name;
    }

    if (hostname) {
      return await HostsApi.executeCustomCommand(clusterName, {
        context: "Clean MYSQL Server",
        command: "CLEAN",
        serviceName: "HIVE",
        componentName: "MYSQL_SERVER",
        hosts: hostname,
      });
    }
  };

  const createComponent = async (componentName: string, hostName: string, serviceName: string) => {
    try {
      // First create the component in the service if it doesn't exist
      await ServiceApi.createComponent(clusterName, serviceName, componentName);
    } catch (error: any) {
      // Component might already exist, which is fine
      if (!error?.response?.data?.message?.includes("already exists")) {
        console.warn(`Component ${componentName} might already exist in service ${serviceName}`);
      }
    }

    // Then register the component to the host (without installing)
    const requestData = {
      RequestInfo: {
        query: `Hosts/host_name=${hostName}`,
      },
      Body: {
        host_components: [
          {
            HostRoles: {
              component_name: componentName,
            },
          },
        ],
      },
    };

    try {
      return await HostsApi.registerHostToComponent(clusterName, requestData);
    } catch (error: any) {
      // Component might already be registered to host, which is fine
      if (error?.response?.data?.message?.includes("already exists")) {
        console.warn(`Component ${componentName} already registered to host ${hostName}`);
        return { status: 200 };
      }
      throw error;
    }
  };

  const createHostComponents = async () => {
    return new Promise((resolve, reject) => {
      // Check KDC session first
      getKDCSessionState(async () => {
        try {
          const reassignData = getReassignData();
          const componentsToCreate = [
            ...hostComponents,
            ...dependentHostComponents,
          ];
          const requests = [];

          for (const componentName of componentsToCreate) {
            const serviceName = getServiceForComponent(componentName);
            requests.push(
              createComponent(componentName, reassignData.targetHost, serviceName)
            );
          }

          const results = await Promise.all(requests);
          resolve({ status: 200, data: results[0] });
        } catch (error) {
          console.error("Error in createHostComponents:", error);
          reject({ status: 500, error });
        }
      });
    });
  };

  // const createHostComponents = async () => {
  //   const reassignData = getReassignData();
  //   const componentsToCreate = [...hostComponents, ...dependentHostComponents];
  //   const requests = [];

  //   for (const componentName of componentsToCreate) {
  //     const serviceName = getServiceForComponent(componentName);
  //     requests.push(
  //       createInstallComponentTask(
  //         componentName,
  //         reassignData.targetHost,
  //         serviceName,
  //         clusterName,
  //         [serviceName],
  //         allServiceModels[serviceNameToModelKeyMap[serviceName]]
  //       )
  //     );
  //   }

  //   return await Promise.all(requests);
  // };

  const putHostComponentsInMaintenanceMode = async () => {
    const reassignData = getReassignData();
    const requests = [];

    for (const componentName of hostComponents) {
      requests.push(
        HostsApi.updateHostComponentPassiveState(
          clusterName,
          reassignData.sourceHost,
          componentName,
          {
            context: undefined,
            passive_state: "ON",
          }
        )
      );
    }
    try {
      await Promise.all(requests);
      return new Promise<{ status: number }>((resolve) => {
        resolve({ status: 200 });
      });
    } catch (error) {
      console.error("Error in putHostComponentsInMaintenanceMode:", error);
      return new Promise<{ status: number }>((_resolve, reject) => {
        reject({ status: 500 });
      });
    }
  };

  //   const reconfigure = async () => {
  //   const configs = getStepData(
  //     state,
  //     reassignSteps.REVIEW,
  //     "configs",
  //     "reassignSteps"
  //   );

  //   const propertiesToChange = getStepData(
  //     state,
  //     reassignSteps.REVIEW,
  //     "propertiesToChange",
  //     "reassignSteps"
  //   );

  //   const attributes = getStepData(
  //     state,
  //     reassignSteps.REVIEW,
  //     "configsAttributes",
  //     "reassignSteps"
  //   );

  //   console.log("Properties to change:", propertiesToChange);

  //   // Check if there are any properties to change
  //   if (!propertiesToChange || Object.keys(propertiesToChange).length === 0) {
  //     console.log("No properties to change, skipping config update");
  //     return { status: 200 }; // Return success without making API call
  //   }

  //   if (configs && attributes) {
  //     return await saveConfigsToServer(configs, attributes, propertiesToChange);
  //   }
  // };

  // const saveConfigsToServer = async (configs: any, attributes: any, propertiesToChange: any) => {
  //   const serviceConfigData = getServiceConfigData(configs, attributes, propertiesToChange);

  //   // Log the data for debugging
  //   console.log("Service config data:", serviceConfigData);

  //   return await HostsApi.commonServiceConfigurations(clusterName, {
  //     desired_config: serviceConfigData[0].Clusters.desired_config
  //   });
  // };

  // const getServiceConfigData = (configs: any, attributes: any, propertiesToChange: any) => {
  //   // Create filtered configs with only the changed properties
  //   const filteredConfigs = {} as any;

  //   if (propertiesToChange) {
  //     Object.keys(propertiesToChange).forEach(siteName => {
  //       if (!filteredConfigs[siteName]) {
  //         filteredConfigs[siteName] = {};
  //       }

  //       if (Array.isArray(propertiesToChange[siteName])) {
  //         propertiesToChange[siteName].forEach(prop => {
  //           if (prop && prop.name && configs[siteName] && configs[siteName][prop.name]) {
  //             filteredConfigs[siteName][prop.name] = configs[siteName][prop.name];
  //           }
  //         });
  //       }
  //     });
  //   }

  //   console.log("Filtered configs:", filteredConfigs);

  //   // Create config data objects for each site
  //   const configData = Object.keys(filteredConfigs).map(function (_siteName) {
  //     return {
  //       type: _siteName,
  //       properties: filteredConfigs[_siteName],
  //       properties_attributes: attributes && attributes[_siteName] ? attributes[_siteName] : {},
  //       service_config_version_note: translateWithVariables(
  //         "services.reassign.step4.save.configuration.note",
  //         { "0": componentName || "" }
  //       ),
  //     }
  //   });

  //   console.log("Config data before service filtering:", configData);

  //   // If no configs to change, return empty array with structure expected by saveConfigsToServer
  //   if (configData.length === 0) {
  //     console.warn("No configs to change");
  //     return [{
  //       Clusters: {
  //         desired_config: []
  //       }
  //     }];
  //   }

  //   // Check if serviceComponentInfo is properly initialized
  //   if (!serviceComponentInfo || !serviceComponentInfo.items) {
  //     console.warn("serviceComponentInfo is not properly initialized, returning configs without service filtering");
  //     return [{
  //       Clusters: {
  //         desired_config: configData
  //       }
  //     }];
  //   }

  //   // Group configs by service
  //   const allConfigData = [] as any[];
  //   let anyServiceConfigsAdded = false;

  //   if (Array.isArray(services)) {
  //     services.forEach((service: any) => {
  //       const serviceName = service?.ServiceInfo?.service_name;
  //       if (serviceName) {
  //         const stackService = serviceComponentInfo?.items?.find(
  //           (item: any) => item?.ServiceInfo?.service_name === serviceName
  //         );

  //         if (stackService) {
  //           const configTypes = stackService.configTypes ||
  //                              (stackService.StackServices && stackService.StackServices.config_types);

  //           if (configTypes) {
  //             const serviceConfigData: any[] = [];
  //             Object.keys(configTypes).forEach((type) => {
  //               const serviceConfigTag = configData.find(
  //                 (config) => config.type === type
  //               );
  //               if (serviceConfigTag) {
  //                 serviceConfigData.push(serviceConfigTag);
  //               }
  //             });

  //             if (serviceConfigData.length > 0) {
  //               allConfigData.push({
  //                 Clusters: {
  //                   desired_config: serviceConfigData,
  //                 },
  //               });
  //               anyServiceConfigsAdded = true;
  //             }
  //           }
  //         }
  //       }
  //     });
  //   }

  //   console.log("All config data after service filtering:", allConfigData);

  //   // If no service configs were added, return the original config data
  //   if (!anyServiceConfigsAdded) {
  //     console.warn("No service configs were added, returning configs without service filtering");
  //     return [{
  //       Clusters: {
  //         desired_config: configData
  //       }
  //     }];
  //   }

  //   return allConfigData;
  // };

  const refreshNameNodeConfigs = async () => {
    const reassignData = getReassignData();

    // Only refresh configs for NAMENODE component
    if (reassignData.component_name !== "NAMENODE") {
      return { status: 200 };
    }

    const context = translateWithVariables(
      "Refreshing NameNode configurations",
      { "0": "NAMENODE" }
    ) as string;

    const resource_filters = [
      {
        service_name: "HDFS",
        component_name: "NAMENODE",
        hosts: reassignData.targetHost,
      },
    ];

    const data = {
      RequestInfo: {
        command: "CONFIGURE",
        context: context,
      },
      "Requests/resource_filters": resource_filters,
    };

    return await HostsApi.clusterRequests(clusterName, data);
  };

  const reconfigure = async () => {
    const configs = getStepData(
      state,
      reassignSteps.REVIEW,
      "configs",
      "reassignSteps"
    );
    const attributes = getStepData(
      state,
      reassignSteps.REVIEW,
      "configsAttributes",
      "reassignSteps"
    );

    if (configs && attributes) {
      const response = await saveConfigsToServer(configs, attributes);
      return new Promise((resolve) => {
        setTimeout(() => {
          resolve(response);
        }, 2000);
      });
    }
  };

  const saveConfigsToServer = async (configs: any, attributes: any) => {

    const configPayload = getServiceConfigData(configs, attributes);
    const response = await HostsApi.commonServiceConfigurationsMove(
      clusterName,
      configPayload
    );
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve(response);
      }, 2000);
    });
  };

  // const saveConfigsToServer = async (configs: any, attributes: any) => {
  //   const serviceConfigData = getServiceConfigData(configs, attributes);

  //   return await HostsApi.commonServiceConfigurations(clusterName, {
  //     desired_config: "[" + serviceConfigData.toString() + "]",
  //   });
  // };

  const getServiceConfigData = (configs: any, attributes: any) => {
    const configData = Object.keys(configs).map(function (_siteName) {
      return {
        type: _siteName,
        properties: configs[_siteName],
        properties_attributes: attributes[_siteName] || {},
        service_config_version_note: translateWithVariables(
          "services.reassign.step4.save.configuration.note",
          { "0": componentName || "" }
        ),
      };
    });

    console.log("Config data before filtering:", configData);

    // Check if serviceComponentInfo is properly initialized
    // if (!serviceComponentInfo || !serviceComponentInfo.items) {
    //   console.warn("serviceComponentInfo is not properly initialized, including all configs");
    //   return [{
    //     Clusters: {
    //       desired_config: configData,
    //     },
    //   }];
    // }

    const allConfigData = [] as any[];
    //@ts-ignore
    let anyServiceConfigsAdded = false;

    //shaurya - fallback to including all configs if none match - impl1
    services.forEach((service: any) => {
      const serviceName = service?.ServiceInfo?.service_name;
      if (serviceName) {
        const stackService = serviceComponentInfo?.items?.find(
          (item: any) => item?.StackServices?.service_name === serviceName
        );

        console.log(
          `Processing service ${serviceName}, stackService:`,
          stackService
        );

        if (stackService) {
          const configTypes =
            stackService.configTypes ||
            stackService.StackServices?.config_types;
          console.log(`Config types for ${serviceName}:`, configTypes);

          if (configTypes) {
            const serviceConfigData: any[] = [];
            Object.keys(configTypes).forEach((type) => {
              const serviceConfigTag = configData.find(
                (config) => config.type === type
              );
              if (serviceConfigTag) {
                serviceConfigData.push(serviceConfigTag);
                console.log(`Added config ${type} for service ${serviceName}`);
              }
            });

            if (serviceConfigData.length > 0) {
              allConfigData.push({
                Clusters: {
                  desired_config: serviceConfigData,
                },
              });
              anyServiceConfigsAdded = true;
            }
          }
        }
      }
    });

    console.log("All config data after filtering:", allConfigData);

    // If no service configs were added, include all configs
    // if (!anyServiceConfigsAdded) {
    //   console.warn("No service configs were added, including all configs");
    //   return [{
    //     Clusters: {
    //       desired_config: configData,
    //     },
    //   }];
    // }

    return allConfigData;
  };

  // const getServiceConfigData = (configs: any, attributes: any) => {
  //   const configData = Object.keys(configs).map((_siteName) => ({
  //     type: _siteName,
  //     properties: configs[_siteName],
  //     properties_attributes: attributes[_siteName] || {},
  //     service_config_version_note: translateWithVariables(
  //       "services.reassign.step4.save.configuration.note",
  //       { "0": componentName || "" }
  //     ),
  //   }));

  //   const allConfigData: any[] = [];

  //   services.forEach((service: any) => {
  //     const serviceName = service?.ServiceInfo?.service_name;
  //     if (serviceName) {
  //       const stackService = serviceComponentInfo?.items?.find(
  //         (item: any) => item?.ServiceInfo?.service_name === serviceName
  //       );

  //       if (stackService && stackService.configTypes) {
  //         const serviceConfigData: any[] = [];
  //         Object.keys(stackService.configTypes).forEach((type) => {
  //           const serviceConfigTag = configData.find(
  //             (config) => config.type === type
  //           );
  //           if (serviceConfigTag) {
  //             serviceConfigData.push(serviceConfigTag);
  //           }
  //         });

  //         if (serviceConfigData.length > 0) {
  //           allConfigData.push({
  //             Clusters: {
  //               desired_config: serviceConfigData,
  //             },
  //           });
  //         }
  //       }
  //     }
  //   });

  //   return allConfigData;
  // };

  const installHostComponents = async () => {
    const reassignData = getReassignData();
    const componentsToInstall = [...hostComponents, ...dependentHostComponents];
    const requests = [];

    // // First check which components are already being installed
    // const response = await HostsApi.getHostComponentsDetails(
    //   clusterName,
    //   `fields=host_components/HostRoles/state,host_components/HostRoles/component_name&host_components/HostRoles/host_name=${reassignData.targetHost}`
    // );

    // const existingComponents = response.items.flatMap(
    //   (item: any) => item.host_components || []
    // ).map((comp: any) => comp.HostRoles?.component_name);

    // Only install components that aren't already being installed
    for (const componentName of componentsToInstall) {
      const serviceName = getServiceForComponent(componentName);
      // const isAlreadyInstalling = existingComponents.includes(componentName) &&
      //   existingComponents.find((comp: any) => comp.HostRoles?.component_name === componentName)?.HostRoles?.state === "INSTALLING";

      // if (!isAlreadyInstalling) {
      //   ops.push({
      //     id: opId++,
      //     label: `Install ${componentName}`,
      //     callback: async () => {
      //       return await updateComponent(
      //         componentName,
      //         reassignData.targetHost,
      //         serviceName,
      //         "Install",
      //         componentsToInstall.length
      //       );
      //     },
      //     skippable: false,
      //   });
      // }
      requests.push(
        updateComponent(
          componentName,
          reassignData.targetHost,
          serviceName,
          "Install",
          componentsToInstall.length
        )
      );
    }
    try {
      if (requests.length > 0) {
        const requestsStatus = await Promise.all(requests);
        return new Promise<any>((resolve) => {
          resolve({ status: 200, ...requestsStatus.at(0) });
        });
      } else {
        return new Promise<any>((resolve) => {
          resolve({ status: 200 });
        });
      }
    } catch (error) {
      return new Promise<any>((_resolve, reject) => {
        reject({ status: 500, error });
      });
    }
    // return requests.length > 0 ? await Promise.all(requests) : { status: 200 };
  };

  // const installHostComponents = async () => {
  //   const reassignData = getReassignData();
  //   const componentsToInstall = [...hostComponents, ...dependentHostComponents];
  //   const requests = [];

  //   for (const componentName of componentsToInstall) {
  //     const serviceName = getServiceForComponent(componentName);
  //     requests.push(
  //       updateComponent(
  //         componentName,
  //         reassignData.targetHost,
  //         serviceName,
  //         "Install",
  //         componentsToInstall.length
  //       )
  //     );
  //   }

  //   return await Promise.all(requests);
  // };

  const updateComponent = async (
    componentName: string,
    hostName: string | string[],
    serviceName: string,
    context: string,
    taskNum: number
  ) => {
    const hostNames = Array.isArray(hostName) ? hostName : [hostName];
    const state = context.toLowerCase() === "start" ? "STARTED" : "INSTALLED";

    const urlParams = `HostRoles/component_name=${componentName}&HostRoles/host_name.in(${hostNames.join(
      ","
    )})&HostRoles/maintenance_state=OFF`;

    return await HostsApi.updateHostComponents(clusterName, urlParams, {
      context: `${context} ${componentName}`,
      HostRoles: {
        state: state,
      },
      level: "HOST_COMPONENT",
      query: urlParams,
      hostName: hostNames,
      componentName: componentName,
      serviceName: serviceName,
      taskNum: taskNum,
    });
  };

  const startZooKeeperServers = async () => {
    const zkHosts = allHostComponents
      .filter((host: any) =>
        host.host_components?.some(
          (comp: any) => comp.HostRoles?.component_name === "ZOOKEEPER_SERVER"
        )
      )
      .map((host: any) => host.Hosts?.host_name);

    if (zkHosts.length > 0) {
      return await updateComponent(
        "ZOOKEEPER_SERVER",
        zkHosts,
        "ZOOKEEPER",
        "Start",
        1
      );
    }
  };

  const startNameNode = async () => {
    const reassignData = getReassignData();
    const nameNodeHosts = allHostComponents
      .filter((host: any) =>
        host.host_components?.some(
          (comp: any) => comp.HostRoles?.component_name === "NAMENODE"
        )
      )
      .map((host: any) => host.Hosts?.host_name)
      .filter(
        (hostName: string) =>
          hostName !== reassignData.sourceHost &&
          hostName !== reassignData.targetHost
      );

    if (nameNodeHosts.length > 0) {
      await refreshNameNodeConfigs();
      return await updateComponent(
        "NAMENODE",
        nameNodeHosts,
        "HDFS",
        "Start",
        1
      );
    }
  };

  const stopHostComponentsInMaintenanceMode = async () => {
    const reassignData = getReassignData();
    const hostComponentsInMM = getReassignComponentsInMM();

    if (hostComponentsInMM.length > 0) {
      const componentsToStop = hostComponentsInMM.map((component: any) => ({
        hostName: reassignData.sourceHost,
        serviceName: reassignData.service_id,
        componentName: component.HostRoles?.component_name,
      }));

      return await updateComponentsState(componentsToStop, "INSTALLED");
    }
  };

  // const stopSourceHostComponents = async () => {
  //   const reassignData = getReassignData();
  //   const componentsToStop = hostComponents.map(componentName => ({
  //     hostName: reassignData.sourceHost,
  //     serviceName: getServiceForComponent(componentName),
  //     componentName: componentName
  //   }));

  //   console.log("Explicitly stopping components on source host before deletion:", componentsToStop);
  //   return await updateComponentsState(componentsToStop, "INSTALLED");
  // };

  const updateComponentsState = async (components: any[], state: string) => {
    const requests = [];

    for (const component of components) {
      requests.push(
        HostsApi.updateHostComponentForHost(
          clusterName,
          component.hostName,
          component.componentName,
          {
            RequestInfo: {
              context: undefined,
              operation_level: {
                level: "HOST_COMPONENT",
                cluster_name: clusterName,
                host_name: component.hostName,
                service_name: component.serviceName || null,
              },
            },
            Body: {
              HostRoles: {
                state: state,
              },
            },
          }
        )
      );
    }

    return await Promise.all(requests);
  };

  const deleteHostComponents = async () => {
    const reassignData = getReassignData();
    const requests = [];

    for (const componentName of hostComponents) {
      requests.push(
        HostsApi.deleteHostComponent(
          clusterName,
          reassignData.sourceHost,
          componentName
        )
      );
    }

    return await Promise.all(requests);
  };

  const putTargetHostComponentsInMaintenanceMode = async () => {
    const reassignData = getReassignData();
    await Promise.all(
      hostComponents.map((component) =>
        HostsApi.updateHostComponentPassiveState(
          clusterName,
          reassignData.targetHost,
          component,
          { context: undefined, passive_state: "ON" }
        )
      )
    );
  };

  const deleteTargetHostComponents = async () => {
    const reassignData = getReassignData();
    await Promise.all(
      hostComponents.map(async (component) => {
        try {
          await HostsApi.deleteHostComponent(
            clusterName,
            reassignData.targetHost,
            component
          );
        } catch (error) {
          if (!isMissingHostComponentError(error)) {
            throw error;
          }
        }
      })
    );
  };

  const configureMySqlServer = async () => {
    const reassignData = getReassignData();
    let hostname = "";

    if (reassignData.component_name === "MYSQL_SERVER") {
      hostname = reassignData.targetHost;
    } else {
      const mysqlComponent = allHostComponents.find((host: any) =>
        host.host_components?.some(
          (comp: any) => comp.HostRoles?.component_name === "MYSQL_SERVER"
        )
      );
      if (mysqlComponent) {
        hostname = mysqlComponent.Hosts?.host_name;
      }
    }

    if (hostname) {
      return await HostsApi.executeCustomCommand(clusterName, {
        context: "Configure MYSQL Server",
        command: "CONFIGURE",
        serviceName: "HIVE",
        componentName: "MYSQL_SERVER",
        hosts: hostname,
      });
    }
  };

  const startMySqlServer = async () => {
    const mysqlComponent = allHostComponents.find((host: any) =>
      host.host_components?.some(
        (comp: any) => comp.HostRoles?.component_name === "MYSQL_SERVER"
      )
    );

    if (mysqlComponent) {
      const hostName = mysqlComponent.Hosts?.host_name;
      return await HostsApi.updateHostComponentForHost(
        clusterName,
        hostName,
        "MYSQL_SERVER",
        {
          RequestInfo: {
            context: "Start MySQL Server",
            operation_level: {
              level: "HOST_COMPONENT",
              cluster_name: clusterName,
              host_name: hostName,
              service_name: "HIVE",
            },
          },
          Body: {
            HostRoles: {
              state: "STARTED",
            },
          },
        }
      );
    }
  };

  const restartMySqlServer = async () => {
    const mysqlComponent = allHostComponents.find((host: any) =>
      host.host_components?.some(
        (comp: any) => comp.HostRoles?.component_name === "MYSQL_SERVER"
      )
    );

    if (mysqlComponent) {
      const hostname = mysqlComponent.Hosts?.host_name;
      return await HostsApi.clusterRequests(clusterName, {
        RequestInfo: {
          context: "Restart MySQL Server",
          command: "RESTART",
          operation_level: {
            level: "HOST_COMPONENT",
            cluster_name: clusterName,
            service_name: "HIVE",
            hostcomponent_name: "MYSQL_SERVER",
          },
        },
        "Requests/resource_filters": [
          {
            component_name: "MYSQL_SERVER",
            hosts: hostname,
            service_name: "HIVE",
          },
        ],
      });
    }
  };

  const startNewMySqlServer = async () => {
    const reassignData = getReassignData();

    return await HostsApi.updateHostComponentForHost(
      clusterName,
      reassignData.targetHost,
      "MYSQL_SERVER",
      {
        RequestInfo: {
          context: "Start MySQL Server",
          operation_level: {
            level: "HOST_COMPONENT",
            cluster_name: clusterName,
            host_name: reassignData.targetHost,
            service_name: "HIVE",
          },
        },
        Body: {
          HostRoles: {
            state: "STARTED",
          },
        },
      }
    );
  };

  const getPropertiesPattern = () => ({
    user_name: /(username|dblogin)$/gi,
    user_passwd: /(dbpassword|password)$/gi,
    db_connection_url: /jdbc\.url|connectionurl/gi,
    driver_class: /ConnectionDriverName|jdbc\.driver/gi,
    schema_name: /db\.schema\.name/gi,
  });

  const getRequiredProperties = () => {
    const reassignData = getReassignData();
    const propertiesMap: any = {
      OOZIE: {
        type: "oozie-site",
        names: [
          "oozie.db.schema.name",
          "oozie.service.JPAService.jdbc.username",
          "oozie.service.JPAService.jdbc.password",
          "oozie.service.JPAService.jdbc.driver",
          "oozie.service.JPAService.jdbc.url",
        ],
      },
      HIVE: {
        type: "hive-site",
        names: [
          "ambari.hive.db.schema.name",
          "javax.jdo.option.ConnectionUserName",
          "javax.jdo.option.ConnectionPassword",
          "javax.jdo.option.ConnectionDriverName",
          "javax.jdo.option.ConnectionURL",
        ],
      },
    };

    return propertiesMap[reassignData.service_id];
  };

  const getConnectionProperty = (regexp: RegExp) => {
    const configs = getStepData(
      state,
      reassignSteps.REVIEW,
      "configs",
      "reassignSteps"
    );
    const requiredProps = getRequiredProperties();

    if (!configs || !requiredProps) return "";

    const propertyName = requiredProps.names.find((item: string) =>
      regexp.test(item)
    );
    return configs[requiredProps.type]?.[propertyName] || "";
  };

  const getPreparedDBProperties = () => {
    const propertiesPattern = getPropertiesPattern();
    const propObj: any = {};

    for (const key in propertiesPattern) {
      propObj[key] = getConnectionProperty(
        propertiesPattern[key as keyof typeof propertiesPattern]
      );
    }

    return propObj;
  };

  const testDBConnection = async () => {
    const reassignData = getReassignData();
    const dbType =
      getStepData(
        state,
        reassignSteps.GET_STARTED,
        "dbType",
        "reassignSteps"
      ) || "mysql";
    const params = getPreparedDBProperties();

    params["db_name"] = dbType;
    params["jdk_location"] = ambariProperties?.["jdk_location"] || "";
    params["jdk_name"] = ambariProperties?.["jdk.name"] || "";
    params["java_home"] = ambariProperties?.["java.home"] || "";
    params["threshold"] = 60;
    params["ambari_server_host"] = window.location.hostname;
    params["check_execute_list"] = "db_connection_check";

    try {
      await HostsApi.clusterRequests(clusterName, {
        RequestInfo: {
          context: "Check host",
          action: "check_host",
          parameters: params,
        },
        "Requests/resource_filters": [
          {
            hosts: reassignData.targetHost,
          },
        ],
      });

      return new Promise((resolve) => {
        resolve({ status: 200 });
      });
    } catch (error: any) {
      return handleConnectionError(error);
    }
  };
  //@ts-ignore
  const onCreateActionSuccess = async (data: any) => {
    const checkDBRequestId = data.Requests.id;

    try {
      const requestResponse = await RequestApi.getTaskId(checkDBRequestId);
      if (requestResponse?.items?.[0]?.Tasks?.id) {
        const taskResponse = await getDBConnTaskInfo(
          checkDBRequestId,
          requestResponse.items[0].Tasks.id
        );
        return taskResponse;
      }
    } catch (error: any) {
      return handleConnectionError(error);
    }
  };

  const getDBConnTaskInfo = async (
    checkDBRequestId: number,
    checkDBTaskId: number
  ) => {
    const taskResponse = await RequestApi.getTaskStatus(
      checkDBRequestId.toString(),
      checkDBTaskId.toString()
    );

    const task = taskResponse?.items?.[0]?.Tasks;

    if (!task || !task.status || task.status === "FAILED") {
      return handleConnectionError(task);
    }

    if (task.status === "COMPLETED") {
      const structuredOut = task.structured_out?.db_connection_check;
      if (structuredOut && structuredOut.exit_code !== 0) {
        return handleConnectionError(structuredOut);
      } else {
        return new Promise((resolve) =>
          resolve({
            ...structuredOut,
            Requests: {
              id: checkDBRequestId,
              request_status: "COMPLETED",
              progress_percent: 100,
            },
          })
        );
      }
    }

    if (/PENDING|QUEUED|IN_PROGRESS/.test(task.status)) {
      return new Promise((resolve, reject) => {
        setTimeout(() => {
          getDBConnTaskInfo(checkDBRequestId, checkDBTaskId).then(
            resolve,
            reject
          );
        }, 3000);
      });
    }
  };

  const handleConnectionError = (error: any) => {
    return new Promise((_, reject) => {
      reject({
        message: error?.message || "Database Connection Error",
        Requests: {
          request_status: "FAILED",
        },
      });
    });
  };

  const startRequiredServices = async () => {
    const reassignData = getReassignData();
    const relatedServices = getRelatedServices(reassignData.component_name);

    if (relatedServices && relatedServices.length > 0) {
      return await startServices(false, relatedServices, true);
    } else {
      return await startServices(true);
    }
  };

  const startServices = async (
    runSmokeTest: boolean,
    servicesToStart?: string[],
    startListedServicesFlag: boolean = false
  ) => {
    const skipServiceCheck =
      ambariProperties?.["skip.service.checks"] === "true";
    const data: any = {
      ServiceInfo: {
        state: "STARTED",
      },
    };

    let servicesList: string = "";
    if (servicesToStart && servicesToStart.length > 0) {
      if (startListedServicesFlag) {
        servicesList = servicesToStart.join(",");
      } else {
        const allServiceNames = services
          .map((service: any) => service?.ServiceInfo?.service_name)
          .filter(Boolean);
        servicesList = allServiceNames
          .filter((s: string) => !servicesToStart.includes(s))
          .join(",");
      }
      data.context = "Start required services";
      data.urlParams = `ServiceInfo/service_name.in(${servicesList})`;
    } else {
      data.context = "Start all services";
    }

    if (runSmokeTest) {
      data.urlParams = data.urlParams ? data.urlParams + "&" : "";
      data.urlParams += "params/run_smoke_test=" + !skipServiceCheck;
    }

    return await ServiceApi.updateService(
      clusterName,
      data,
      data.urlParams || ""
    );
  };

  const getRelatedServices = (componentName: string): string[] => {
    return relatedServicesMap[componentName] || [];
  };

  const getMessageForAlert = () => {
    if (completionStatus) {
      if(isLastStep){
      return translateWithVariables("services.reassign.step4.status.success", {
        "0": componentName || "",
        "1": getReassignData().sourceHost || "",
        "2": getReassignData().targetHost || "",
      });
    }
    else{
      return "Proceed to the next step";
    }
    } else {
      return translateWithVariables("services.reassign.step4.status.info", {
        "0": componentName || "",
      });
    }
  };


  const initializeOperationsFromTasks = () => {
    const operations = tasks.map((task) => ({
      id: task.id,
      label: task.title,
      command: task.command,
      skippable: false,
      callback: async () => {
        try {
          const functionMap: { [key: string]: () => Promise<any> } = {
            stopRequiredServices,
            cleanMySqlServer,
            createHostComponents,
            putHostComponentsInMaintenanceMode,
            reconfigure,
            installHostComponents,
            startZooKeeperServers,
            startNameNode,
            stopHostComponentsInMaintenanceMode,
            // stopSourceHostComponents,
            deleteHostComponents,
            configureMySqlServer,
            startMySqlServer,
            restartMySqlServer,
            startNewMySqlServer,
            testDBConnection,
            startRequiredServices,
          };

          const callbackFunction = functionMap[task.command];
          if (callbackFunction) {
            return await callbackFunction();
          }

          console.log();
          return { status: 200 };
        } catch (error: any) {
          console.error("Error occurred while executing task:", error);
          console.log("error in catch", error);
          // Check if the error is about missing KDC credentials
          if (
            error?.message?.includes("Missing KDC administrator credentials") ||
            (error?.response?.data?.message &&
              error?.response?.data?.message.includes(
                "Missing KDC administrator credentials"
              ))
          ) {
            console.warn("KDC credentials are missing or invalid.");
            setShowInvalidKDCPopup(true);
            // Return a promise that never resolves to pause the operation
            return new Promise(() => { });
          }
          throw error;
        }
      },
    }));
    return operations;
  }

  const rollbackDatabaseMove = async () => {
    setRollbackInProgress(true);
    setRollbackError("");
    try {
      await putTargetHostComponentsInMaintenanceMode();
      await deleteTargetHostComponents();
      await cleanMySqlServer();
      await configureMySqlServer();
      await startRequiredServices();
      await flushStateToDb("complete");
      const serviceName = getServiceForComponent(componentName || "");
      window.location.href = `/#/main/services/${serviceName}/summary`;
    } catch (error: unknown) {
      const fallbackMessage =
        error instanceof Error
          ? error.message
          : "The rollback operation failed.";
      setRollbackError(
        get(
          error,
          "response.data.message",
          fallbackMessage
        )
      );
    } finally {
      setRollbackInProgress(false);
    }
  };

  const confirmDatabaseRollback = () => {
    modalManager.show({
      modalTitle: "Rollback Component Move",
      modalBody:
        "The database connection test failed. Remove the component from the target host, restore the database service configuration, and restart the affected services?",
      successCallback: () => {
        modalManager.hide();
        void rollbackDatabaseMove();
      },
      onClose: () => modalManager.hide(),
      options: { okButtonText: "ROLLBACK" },
    });
  };

  const handleNext = async () => {
    if (isLastStep) {
      // If this is the final step, complete the wizard and redirect
      await flushStateToDb("complete");
      await ClusterApi.postPersistData(
        JSON.stringify({
          USER_REDIRECTION_URL: "",
        })
      );
      // Get the service name for the component
      const serviceName = getServiceForComponent(componentName || "");
      // Redirect to service summary page
      window.location.href = `/#/main/services/${serviceName}/summary`;
    } else {
      // If not the final step, proceed to next step
      flushStateToDb("next");
      handleNextImperitive();
    }
  };

  const savedOperationsState = getStepData(
    state,
    reassignSteps.CONFIGURE_COMPONENT,
    "operationsState",
    "reassignSteps"
  );

  useEffect(() => {
    const finalOperations = (() => {
      const initialOperations = initializeOperationsFromTasks();
      if (savedOperationsState && Array.isArray(savedOperationsState)) {
        return initialOperations.map((originalOp: any) => {
          const savedOp = savedOperationsState.find(
            (saved: any) => saved.id === originalOp.id
          );
          return savedOp
            ? { ...originalOp, ...savedOp, callback: originalOp.callback }
            : originalOp;
        });
      }

      return initialOperations;
    })();
    setStepOperations(finalOperations);
  }, [JSON.stringify(savedOperationsState), tasks])

  if (!stepOperations.length) {
    return (
      <div
        className="d-flex justify-content-center align-items-center"
        style={{ height: "200px" }}
      >
        <div>Loading operations...</div>
      </div>
    );
  }


  return (
    <>
      <h3 className="step-title">
        {translate("services.reassign.step4.header")}
      </h3>
      <Alert variant={completionStatus ? "success" : "info"} className="mb-4">
        {getMessageForAlert()}
      </Alert>

      {isLoaded ? (
        <OperationsProgress
          title=""
          description=""
          setCompletionStatus={setCompletionStatus}
          operations={stepOperations as any}
          dispatch={(operationsState: any) => {
            dispatch({
              type: ActionTypes.STORE_INFORMATION,
              payload: {
                step: currentStep.name,
                data: {
                  operationsState,
                },
              },
            });
          }}
        />
      ) : (
        <Spinner />
      )}

      {stepOperations.some(
        (operation: { command?: string; status?: string }) =>
          operation.command === "testDBConnection" &&
          operation.status === "FAILED"
      ) ? (
        <div className="mt-3">
          {rollbackError ? <Alert variant="danger">{rollbackError}</Alert> : null}
          <Button
            variant="danger"
            disabled={rollbackInProgress}
            onClick={confirmDatabaseRollback}
          >
            {rollbackInProgress ? "ROLLING BACK" : "ROLLBACK"}
          </Button>
        </div>
      ) : null}

      <WizardFooter
        step={currentStep}
        isNextEnabled={completionStatus}
        onNext={handleNext}
        onBack={() => {
          flushStateToDb("back");
          jumpToStep(4);
        }}
        onCancel={() => {
          flushStateToDb("cancel");
        }}
      />
      <InvalidKDCPopup
        isOpen={showInvalidKDCPopup}
        onClose={() => setShowInvalidKDCPopup(false)}
        handleSave={handleSaveInvalidKDC}
      />
    </>
  );
}

export default Step4;
