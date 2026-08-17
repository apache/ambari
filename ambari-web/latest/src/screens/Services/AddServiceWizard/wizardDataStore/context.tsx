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

import React, {
  createContext,
  Dispatch,
  useContext,
  useEffect,
  useReducer,
  useRef,
  useState,
} from "react";
import { State, Action, ActionTypes } from "./types";
import { reducer, initialState } from "./reducer";
import ClusterApi from "../../../../api/clusterApi";
import { AppContext } from "../../../../store/context";
import { forEach, get, isEmpty, map } from "lodash";
import { excludeServicesOnDisplay } from "../../../ClusterWizard/constants";
import VersionsApi from "../../../../api/versionsApi";
import { HostsApi } from "../../../../api/hostsApi";
import { getAllComponents } from "../../../Hosts/utils";
import { ServiceApi } from "../../../../api/serviceApi";
import { ClusterProgressStatus } from "../../../../constants";
import modalManager from "../../../../store/ModalManager";
import {
  CANCEL_ADD_SERVICE_WIZARD_EVENT,
  clearAddServiceWizardState,
} from "../../../../Utils/addServicePersistence";

interface AddServiceContextProps {
  state: State;
  dispatch: Dispatch<Action>;
  stepWizardUtilities?: any;
  flushStateToDb?: any;
  installedHosts: string[];
  serviceContextLoading: boolean;
  installedServices: string[];
}

export const AddServiceContext = createContext<AddServiceContextProps>({
  state: initialState,
  dispatch: () => undefined,
  flushStateToDb: () => undefined,
  installedHosts: [],
  serviceContextLoading: false,
  installedServices: [],
});

export const AddServiceProvider: React.FC<{
  stepWizardUtilities: any;
  children: React.ReactNode;
}> = ({ stepWizardUtilities, children }) => {
  const [state, dispatch] = useReducer(reducer, initialState);
  const [installedHosts, setInstalledHosts] = useState([]);
  const [installedServices, setInstalledServices] = useState([]);
  const [serviceContextLoading, setServiceContextLoading] = useState(true);
  const [currStepData, setCurrStepData] = useState({});
  const { clusterName, services, serviceComponentInfo } =
    useContext(AppContext);

  const isDataPersisted = useRef(false);
  const isCancelled = useRef(false);
  const requestSequence = useRef(0);
  const debounceTimer = useRef<NodeJS.Timeout | null>(null);
  const cancelWizardRef = useRef<(() => Promise<void>) | null>(null);

  // Custom debounced persist function with cancellation capability
  const debouncedPersist = () => {
    // Clear any existing timer
    if (debounceTimer.current) {
      clearTimeout(debounceTimer.current);
    }
    
    // Set new timer
    debounceTimer.current = setTimeout(() => {
      flushCurrentData();
    }, 500);
  };

  // Function to cancel the debounced persist
  const cancelDebouncedPersist = () => {
    if (debounceTimer.current) {
      clearTimeout(debounceTimer.current);
      debounceTimer.current = null;
    }
  };

  const getInstalledServices = () => {
    const serviceNames = services.map((service: any) =>
      get(service, "ServiceInfo.service_name", "")
    );
    let installedServicesData: any = {};
    forEach(serviceComponentInfo.items, (service: any) => {
      if (serviceNames.includes(get(service, "StackServices.service_name"))) {
        installedServicesData[service.StackServices.service_name] = {
          displayName: service.StackServices.display_name,
          serviceName: service.StackServices.service_name,
          serviceType: service.StackServices.service_type,
          version: service.StackServices.service_version,
          comments: service.StackServices.comments,
          selected: true,
          required: service.StackServices.required_services,
          isIgnored: false,
          isHiddenOnDisplay: excludeServicesOnDisplay.includes(
            service.StackServices.service_name
          ),
        };
      }
    });
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: "SERVICES",
        data: { services: installedServicesData },
      },
    });
  };

  const setClusterName = () => {
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: "NAME",
        data: { clusterName },
      },
    });
  };

  const setStackAndVersion = async () => {
    const response = await VersionsApi.getServices(clusterName);
    
    // Find the current stack version instead of just using the first one
    const currentStack = response.items.find(
      (stack: any) => stack.ClusterStackVersions.state === "CURRENT"
    );
    
    // Use the current stack if found, otherwise fallback to the first one
    const stackToUse = currentStack || response.items[0];
    
    const repoVersionId = get(
      stackToUse,
      "ClusterStackVersions.repository_version",
      ""
    );
    const repo = get(stackToUse, "repository_versions", []).find(
      (version: any) => get(version, "RepositoryVersions.id") === repoVersionId
    )?.RepositoryVersions;
    const repoVersion = get(repo, "repository_version", "");
    const repoDisplayName = get(repo, "display_name", "");
    const stackName = get(stackToUse, "ClusterStackVersions.stack", "");
    const stackVersion = get(
      stackToUse,
      "ClusterStackVersions.version",
      ""
    );
    const repoData = await VersionsApi.getRepoDetails(stackName, repoVersion);
    const os = get(
      repoData,
      "items.[0].repository_versions.[0].operating_systems.[0]",
      {}
    );
    const repos = get(os, "repositories", []).map((repo: any) => {
      return {
        id: get(repo, "Repositories.repo_id"),
        defaultId: get(repo, "Repositories.repo_id"),
        baseUrl: get(repo, "Repositories.base_url"),
        name: get(repo, "Repositories.repo_name"),
        defaultUrl: get(repo, "Repositories.default_base_url"),
      };
    });
    const data = {
      selectedVersion: {
        id: repoDisplayName,
        stack_name: stackName,
        stack_version: stackVersion,
      },
      selectedStack: {
        id: repoDisplayName,
        stack_name: stackName,
        stack_version: stackVersion,
      },
      operatingSystems: {
        [repoDisplayName]: [
          {
            os: get(os, "OperatingSystems.os_type", ""),
            isAdded: true,
            repos: repos,
          },
        ],
      },
    };
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: "VERSION",
        data: data,
      },
    });
    setServiceContextLoading(false);
  };

  useEffect(() => {
    getAlreadyInstalledServices();
  }, []);

  useEffect(() => {
    if (!isEmpty(services) && !isEmpty(serviceComponentInfo)) {
      getInstalledServices();
    }
  }, [services, serviceComponentInfo]);

  useEffect(() => {
    syncUserPersistedData();
    getHostComponents();
  }, []);

  useEffect(() => {
    if (isDataPersisted.current) {
      debouncedPersist();
    }
  }, [state.addServiceSteps, currStepData]);

  async function syncUserPersistedData() {
    try {
      const persistedData = await ClusterApi.getPersistData("ADD_SERVICE");
      if (!isEmpty(get(persistedData, "addServiceSteps", {}))) {
        dispatch({
          type: ActionTypes.SYNC_STATE,
          payload: persistedData,
        });
      }
      if (get(persistedData, "activeStep", "")) {
        try {
          const activeStepName = get(persistedData, "activeStep");
          setCurrStepData({
            progressStatus: ClusterProgressStatus.ADDING_SERVICE,
            stepName: activeStepName,
          });
          let activeStepNumber = Object.keys(
            stepWizardUtilities.wizardSteps
          ).find((stepName) => {
            return (
              stepWizardUtilities.wizardSteps?.[stepName]?.name ===
              activeStepName
            );
          });
          stepWizardUtilities.jumpToStep(Number(activeStepNumber), true);
        } catch (err) {
          console.error("Error while jumping to step", err);
        }
      } else {
        getInstalledServices();
        setClusterName();
        setStackAndVersion();
        stepWizardUtilities.jumpToStep(1, true);
      }
    } finally {
      isDataPersisted.current = true;
    }
  }

  const getAlreadyInstalledServices = async () => {
    const installedServicesApi = await ServiceApi.getAllServices(clusterName);
    setInstalledServices(
      map(installedServicesApi.items, "ServiceInfo.service_name") as any
    );
  };

  const getHostComponents = async () => {
    const response = await HostsApi.getHostComponentsDetails(
      clusterName,
      "fields=host_components/HostRoles/state&minimal_response=true"
    );
    const hostsList = get(response, "items", []).map((item: any) =>
      get(item, "Hosts.host_name")
    );
    setInstalledHosts(hostsList);
    const data = get(response, "items", []).map((item: any) => {
      return {
        name: get(item, "Hosts.host_name"),
        bootStatus: "REGISTERED",
        isInstalled: true,
      };
    });
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: "HOST_STATUS",
        data: { hosts: data },
      },
    });
    let mastersData: any[] = [];
    const allComponents = getAllComponents(serviceComponentInfo).filter(
      (c) => get(c, "HostRoles.is_master") === true
    );
    forEach(response.items, (host: any, idx: number) => {
      let masterServicesData: any[] = [];
      forEach(get(host, "host_components", []), (component) => {
        const componentName = get(component, "HostRoles.component_name");
        const componentData = allComponents.find(
          (c: any) =>
            get(c, "HostRoles.component_name") ===
            get(component, "HostRoles.component_name")
        );
        if (!isEmpty(componentData)) {
          masterServicesData.push({
            display_name: get(componentData, "HostRoles.display_name"),
            component: componentName,
            serviceId: get(componentData, "HostRoles.service_name"),
            isInstalled: true,
            host_id: idx + 1,
            hostName: get(host, "Hosts.host_name"),
          });
        }
      });
      const data = {
        host_name: get(host, "Hosts.host_name"),
        masterServices: masterServicesData,
      };
      mastersData.push(data);
    });
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: "MASTERS",
        data: { mastersData },
      },
    });
  };

  async function flushCurrentData() {
    // Assign a sequence number to this request
    const currentSequence = ++requestSequence.current;
    
    // Don't persist if wizard has been cancelled
    if (isCancelled.current) {
      return;
    }

    try {
      const result = await ClusterApi.postPersistData(
        JSON.stringify({
          ADD_SERVICE: JSON.stringify({
            ...state,
            activeStep: get(currStepData, "stepName", ""),
            requestSequence: currentSequence, // Include sequence number in the data
          }),
          CLUSTER_STATE: JSON.stringify(currStepData),
        })
      );
      
      // Check if this request is still the latest after completion
      if (currentSequence !== requestSequence.current || isCancelled.current) {
        return;
      }
      
      return result;
    } catch (error: any) {
      // Only log errors if the wizard hasn't been cancelled and this is still the latest request
      if (!isCancelled.current && currentSequence === requestSequence.current) {
        console.error('Error persisting data:', error);
      }
    }
  }

  async function flushOnCancel() {
    // Set cancellation flag to prevent any pending flushCurrentData calls
    isCancelled.current = true;
    
    // Increment sequence to invalidate any pending requests
    const cancelSequence = ++requestSequence.current;

    // Cancel the debounced function to prevent it from executing
    cancelDebouncedPersist();

    try {
      // Clear the persisted state with the latest sequence number
      await clearAddServiceWizardState(initialState, cancelSequence);
    } catch (error: any) {
      console.error('Error clearing persisted data:', error);
    } finally {
      modalManager.hide();
      window.location.href = "/#/main/dashboard/metrics";
      window.location.reload();
    }
  }

  cancelWizardRef.current = flushOnCancel;

  useEffect(() => {
    const cancelWizard = () => {
      void cancelWizardRef.current?.();
    };
    window.addEventListener(CANCEL_ADD_SERVICE_WIZARD_EVENT, cancelWizard);
    return () => {
      window.removeEventListener(CANCEL_ADD_SERVICE_WIZARD_EVENT, cancelWizard);
      if (debounceTimer.current) {
        clearTimeout(debounceTimer.current);
        debounceTimer.current = null;
      }
      isCancelled.current = true;
    };
  }, []);

  async function flushOnStepChange(nextStep: number) {
    if (nextStep >= 1) {
      let nextStepDetails = stepWizardUtilities.wizardSteps?.[nextStep];
      if (nextStepDetails?.keysToRemove) {
        nextStepDetails.keysToRemove.forEach((key: string) => {
          if (state?.addServiceSteps?.[key]) {
            dispatch({
              type: ActionTypes.REMOVE_KEY,
              payload: { key },
            });
          }
        });
      }
      setCurrStepData({
        progressStatus: ClusterProgressStatus.ADDING_SERVICE,
        stepName: stepWizardUtilities?.wizardSteps?.[nextStep]?.name,
      });
    }
  }

  function flushStateToDb(
    operation: string = "default",
    jumpStep: number = -1
  ) {
    let activeStep = Object.keys(stepWizardUtilities.wizardSteps).find(
      (stepName) => {
        return (
          stepWizardUtilities.wizardSteps?.[stepName]?.name ===
          stepWizardUtilities.currentStep.name
        );
      }
    );
    switch (operation) {
      case "cancel":
        return flushOnCancel();
      case "back":
        return flushOnStepChange(Number(activeStep) - 1);
      case "next":
        return flushOnStepChange(Number(activeStep) + 1);
      case "jump":
        return flushOnStepChange(jumpStep);
      default:
        return flushCurrentData();
    }
  }

  return (
    <AddServiceContext.Provider
      value={{
        state,
        dispatch,
        stepWizardUtilities,
        flushStateToDb,
        installedHosts,
        serviceContextLoading,
        installedServices,
      }}
    >
      {children}
    </AddServiceContext.Provider>
  );
};
