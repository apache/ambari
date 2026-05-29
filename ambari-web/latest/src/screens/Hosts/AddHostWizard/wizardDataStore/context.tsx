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
import { forEach, get, isEmpty } from "lodash";
import { excludeServicesOnDisplay } from "../../../ClusterWizard/constants";
import VersionsApi from "../../../../api/versionsApi";
import { HostsApi } from "../../../../api/hostsApi";
import { getAllComponents } from "../../utils";
import { ClusterProgressStatus } from "../../../../constants";

interface AddHostContextProps {
  state: State;
  dispatch: Dispatch<Action>;
  stepWizardUtilities?: any;
  flushStateToDb?: any;
  installedHosts: string[];
}

export const AddHostContext = createContext<AddHostContextProps>({
  state: initialState,
  dispatch: () => undefined,
  flushStateToDb: () => undefined,
  installedHosts: [],
});

export const AddHostProvider: React.FC<{
  stepWizardUtilities: any;
  children: React.ReactNode;
}> = ({ stepWizardUtilities, children }) => {
  const [state, dispatch] = useReducer(reducer, initialState);
  const [currStepData, setCurrStepData] = useState({});
  const [installedHosts, setInstalledHosts] = useState([]);
  const { clusterName, services, serviceComponentInfo } =
    useContext(AppContext);

  const isDataPersisted = useRef(false);

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
    try {
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
    } catch (error) {
      console.error("Error setting stack and version:", error);
      // Set fallback data to prevent undefined errors
      const fallbackData = {
        selectedVersion: {
          id: "",
          stack_name: "",
          stack_version: "",
        },
        selectedStack: {
          id: "",
          stack_name: "",
          stack_version: "",
        },
        operatingSystems: {},
      };
      dispatch({
        type: ActionTypes.STORE_INFORMATION,
        payload: {
          step: "VERSION",
          data: fallbackData,
        },
      });
    }
  };

  useEffect(() => {
    syncUserPersistedData();
    getHostComponents();
  }, []);

  // Ensure cluster name, services, and stack/version are loaded when AppContext data becomes available
  useEffect(() => {
    if (clusterName) {
      setClusterName();
      
      // Also load stack and version when we have all required data
      if (!isEmpty(services) && !isEmpty(serviceComponentInfo)) {
        setStackAndVersion();
      }
    }
  }, [clusterName, services, serviceComponentInfo]);

  useEffect(() => {
    if (isDataPersisted.current) {
      flushCurrentData();
    }
  }, [state.addHostSteps, currStepData]);

  useEffect(() => {
    if (!isEmpty(services) && !isEmpty(serviceComponentInfo)) {
      getInstalledServices();
    }
  }, [services, serviceComponentInfo]);

  const getHostComponents = async () => {
    const response = await HostsApi.getHostComponentsDetails(
      clusterName,
      "fields=host_components/HostRoles/state&minimal_response=true"
    );
    const hostsList = get(response, "items", []).map((item: any) =>
      get(item, "Hosts.host_name")
    );
    setInstalledHosts(hostsList);
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

  async function syncUserPersistedData() {
    try {
      const persistedData = await ClusterApi.getPersistData("ADD_HOST");
      if (!isEmpty(get(persistedData, "addHostSteps", {}))) {
        dispatch({
          type: ActionTypes.SYNC_STATE,
          payload: persistedData,
        });
      }
      if (get(persistedData, "activeStep", "")) {
        try {
          const activeStepName = get(persistedData, "activeStep");
          setCurrStepData({
            progressStatus: ClusterProgressStatus.ADDING_HOST,
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

  async function flushCurrentData() {
    await ClusterApi.postPersistData(
      JSON.stringify({
        ADD_HOST: JSON.stringify({
          ...state,
          activeStep: get(currStepData, "stepName", ""),
        }),
        CLUSTER_STATE: JSON.stringify(currStepData),
      })
    );
  }

  function flushOnCancel() {
    ClusterApi.postPersistData(
      JSON.stringify({
        ADD_HOST: JSON.stringify(initialState),
        CLUSTER_STATE: JSON.stringify({}),
      })
    );
    window.location.href = "/#/main/hosts";
  }

  async function flushOnStepChange(nextStep: number) {
    if (nextStep >= 1) {
      let nextStepDetails = stepWizardUtilities.wizardSteps?.[nextStep];
      if (nextStepDetails?.keysToRemove) {
        nextStepDetails.keysToRemove.forEach((key: string) => {
          if (state?.addHostSteps?.[key]) {
            dispatch({
              type: ActionTypes.REMOVE_KEY,
              payload: { key },
            });
          }
        });
      }
      setCurrStepData({
        progressStatus: ClusterProgressStatus.ADDING_HOST,
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
        flushOnCancel();
        break;
      case "back":
        flushOnStepChange(Number(activeStep) - 1);
        break;
      case "next":
        flushOnStepChange(Number(activeStep) + 1);
        break;
      case "jump":
        flushOnStepChange(jumpStep);
        break;
      default:
        flushCurrentData();
    }
  }

  return (
    <AddHostContext.Provider
      value={{
        state,
        dispatch,
        stepWizardUtilities,
        flushStateToDb,
        installedHosts,
      }}
    >
      {children}
    </AddHostContext.Provider>
  );
};
