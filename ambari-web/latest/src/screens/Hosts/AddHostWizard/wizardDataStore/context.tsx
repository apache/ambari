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
import { Alert, Button } from "react-bootstrap";

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
  const [state, reducerDispatch] = useReducer(reducer, initialState);
  const [currStepData, setCurrStepData] = useState({});
  const [installedHosts, setInstalledHosts] = useState([]);
  const [isHydrated, setIsHydrated] = useState(false);
  const [initializationError, setInitializationError] = useState<string | null>(null);
  const [retryCount, setRetryCount] = useState(0);
  const { clusterName, services, serviceComponentInfo } =
    useContext(AppContext);

  const isDataPersisted = useRef(false);
  const persistenceQueue = useRef<Promise<void>>(Promise.resolve());
  const stateRef = useRef<State>(initialState);
  const currStepDataRef = useRef<Record<string, any>>({});

  const dispatch: Dispatch<Action> = (action) => {
    stateRef.current = reducer(stateRef.current, action);
    reducerDispatch(action);
  };

  const queuePersistence = (operation: () => Promise<void>) => {
    const nextOperation = persistenceQueue.current
      .catch(() => undefined)
      .then(operation);
    persistenceQueue.current = nextOperation.catch(() => undefined);
    return nextOperation;
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
    try {
      const response = await VersionsApi.getServices(clusterName);
      
      // Find the current stack version instead of just using the first one
      const currentStack = response.items.find(
        (stack: any) => stack.ClusterStackVersions.state === "CURRENT"
      );
      
      // Use the current stack if found, otherwise fallback to the first one
      const stackToUse = currentStack || response.items[0];
      if (!stackToUse) {
        throw new Error("No stack version is available for this cluster.");
      }
      
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
      throw error;
    }
  };

  useEffect(() => {
    void syncUserPersistedData();
  }, [retryCount]);

  useEffect(() => {
    if (isHydrated && clusterName && !state.addHostSteps?.NAME) {
      setClusterName();
    }
  }, [clusterName, isHydrated, state.addHostSteps?.NAME]);

  useEffect(() => {
    if (
      isHydrated
      && !isEmpty(services)
      && !isEmpty(serviceComponentInfo)
      && !state.addHostSteps?.SERVICES
    ) {
      getInstalledServices();
    }
  }, [isHydrated, services, serviceComponentInfo, state.addHostSteps?.SERVICES]);

  useEffect(() => {
    if (
      isHydrated
      && clusterName
      && !isEmpty(services)
      && !isEmpty(serviceComponentInfo)
      && !state.addHostSteps?.VERSION
    ) {
      void setStackAndVersion().catch((error: any) => {
        setInitializationError(
          error?.response?.data?.message || error?.message || "Ambari could not load the cluster stack version.",
        );
      });
    }
  }, [clusterName, isHydrated, services, serviceComponentInfo, state.addHostSteps?.VERSION]);

  useEffect(() => {
    if (isHydrated && clusterName && !isEmpty(serviceComponentInfo)) {
      void getHostComponents().catch((error: any) => {
        setInitializationError(
          error?.response?.data?.message || "Ambari could not load installed hosts.",
        );
      });
    }
  }, [clusterName, isHydrated, serviceComponentInfo, retryCount]);

  useEffect(() => {
    if (isDataPersisted.current) {
      void queuePersistence(() => flushCurrentData(state, currStepData));
    }
  }, [state.addHostSteps, currStepData]);

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
    setInitializationError(null);
    setIsHydrated(false);
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
          const restoredStepData = {
            progressStatus: ClusterProgressStatus.ADDING_HOST,
            stepName: activeStepName,
          };
          currStepDataRef.current = restoredStepData;
          setCurrStepData(restoredStepData);
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
        stepWizardUtilities.jumpToStep(1, true);
      }
      isDataPersisted.current = true;
      setIsHydrated(true);
    } catch (error: any) {
      isDataPersisted.current = false;
      setInitializationError(
        error?.response?.data?.message || "Ambari could not restore the Add Host wizard.",
      );
      setIsHydrated(false);
    }
  }

  async function flushCurrentData(
    stateSnapshot: State = stateRef.current,
    stepSnapshot: Record<string, any> = currStepDataRef.current,
  ) {
    await ClusterApi.postPersistData(
      JSON.stringify({
        ADD_HOST: JSON.stringify({
          ...stateSnapshot,
          activeStep: get(stepSnapshot, "stepName", ""),
        }),
        CLUSTER_STATE: JSON.stringify(stepSnapshot),
      })
    );
  }

  async function flushOnCancel() {
    await queuePersistence(() => ClusterApi.postPersistData(
      JSON.stringify({
        ADD_HOST: JSON.stringify(initialState),
        CLUSTER_STATE: JSON.stringify({}),
      }),
    ));
    window.location.assign("/main/hosts");
  }

  async function flushOnStepChange(nextStep: number) {
    if (nextStep >= 1) {
      const nextStepDetails = stepWizardUtilities.wizardSteps?.[nextStep];
      const nextAddHostSteps = { ...stateRef.current.addHostSteps };
      if (nextStepDetails?.keysToRemove) {
        nextStepDetails.keysToRemove.forEach((key: string) => {
          delete nextAddHostSteps[key];
        });
      }
      const nextState = {
        ...stateRef.current,
        addHostSteps: nextAddHostSteps,
      };
      dispatch({ type: ActionTypes.SYNC_STATE, payload: nextState });
      const nextStepData = {
        progressStatus: ClusterProgressStatus.ADDING_HOST,
        stepName: stepWizardUtilities?.wizardSteps?.[nextStep]?.name,
      };
      currStepDataRef.current = nextStepData;
      setCurrStepData(nextStepData);
      await queuePersistence(() => flushCurrentData(nextState, nextStepData));
    }
  }

  async function flushStateToDb(
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
        await flushOnCancel();
        break;
      case "back":
        await flushOnStepChange(Number(activeStep) - 1);
        break;
      case "next":
        await flushOnStepChange(Number(activeStep) + 1);
        break;
      case "jump":
        await flushOnStepChange(jumpStep);
        break;
      default:
        await queuePersistence(() => flushCurrentData(
          stateRef.current,
          currStepDataRef.current,
        ));
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
      {initializationError ? (
        <Alert variant="danger" className="m-4">
          {initializationError}{" "}
          <Button
            size="sm"
            variant="outline-danger"
            onClick={() => setRetryCount((value) => value + 1)}
          >
            Retry
          </Button>
        </Alert>
      ) : children}
    </AddHostContext.Provider>
  );
};
