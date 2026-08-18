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
  useCallback,
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
import { Alert, Button } from "react-bootstrap";
import { claimWizard, releaseWizard } from "../../../../Utils/wizardOwnership";
import { resolveRecoveryStep } from "../../../ClusterWizard/wizardRecovery";

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
  const [state, reducerDispatch] = useReducer(reducer, initialState);
  const [installedHosts, setInstalledHosts] = useState([]);
  const [installedServices, setInstalledServices] = useState([]);
  const [serviceContextLoading, setServiceContextLoading] = useState(true);
  const [currStepData, setCurrStepData] = useState({});
  const [isHydrated, setIsHydrated] = useState(false);
  const [initializationError, setInitializationError] = useState<string | null>(null);
  const [retryCount, setRetryCount] = useState(0);
  const { clusterName, services, serviceComponentInfo, loginName } =
    useContext(AppContext);

  const isDataPersisted = useRef(false);
  const isCancelled = useRef(false);
  const cancelWizardRef = useRef<(() => Promise<void>) | null>(null);
  const persistenceQueue = useRef<Promise<void>>(Promise.resolve());
  const stateRef = useRef<State>(initialState);
  const currStepDataRef = useRef<Record<string, any>>({});

  const dispatch: Dispatch<Action> = (action) => {
    stateRef.current = reducer(stateRef.current, action);
    reducerDispatch(action);
  };

  const queuePersistence = useCallback((operation: () => Promise<any>) => {
    const nextOperation = persistenceQueue.current
      .catch(() => undefined)
      .then(operation)
      .then(() => undefined);
    persistenceQueue.current = nextOperation.catch(() => undefined);
    return nextOperation;
  }, []);

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
    setServiceContextLoading(false);
  };

  useEffect(() => {
    if (isHydrated && clusterName) {
      void getAlreadyInstalledServices().catch(handleInitializationError);
    }
  }, [clusterName, isHydrated, retryCount]);

  useEffect(() => {
    if (
      isHydrated
      && !isEmpty(services)
      && !isEmpty(serviceComponentInfo)
      && !state.addServiceSteps?.SERVICES
    ) {
      getInstalledServices();
    }
  }, [isHydrated, services, serviceComponentInfo, state.addServiceSteps?.SERVICES]);

  useEffect(() => {
    void syncUserPersistedData();
  }, [retryCount]);

  useEffect(() => {
    if (isHydrated && clusterName && !state.addServiceSteps?.NAME) {
      setClusterName();
    }
  }, [clusterName, isHydrated, state.addServiceSteps?.NAME]);

  useEffect(() => {
    if (
      isHydrated
      && clusterName
      && !isEmpty(serviceComponentInfo)
      && (!state.addServiceSteps?.HOST_STATUS || !state.addServiceSteps?.MASTERS)
    ) {
      void getHostComponents().catch(handleInitializationError);
    }
  }, [clusterName, isHydrated, serviceComponentInfo, retryCount]);

  useEffect(() => {
    if (isHydrated && clusterName && !state.addServiceSteps?.VERSION) {
      setServiceContextLoading(true);
      void setStackAndVersion().catch(handleInitializationError);
    } else if (isHydrated) {
      setServiceContextLoading(false);
    }
  }, [clusterName, isHydrated, state.addServiceSteps?.VERSION, retryCount]);

  useEffect(() => {
    if (isDataPersisted.current) {
      void queuePersistence(() => flushCurrentData(state, currStepData));
    }
  }, [state.addServiceSteps, currStepData]);

  const handleInitializationError = (error: any) => {
    isDataPersisted.current = false;
    setInitializationError(
      error?.response?.data?.message
        || error?.message
        || "Ambari could not initialize the Add Service wizard.",
    );
  };

  async function syncUserPersistedData() {
    setInitializationError(null);
    setIsHydrated(false);
    isDataPersisted.current = false;
    isCancelled.current = false;
    try {
      const persistedData = await ClusterApi.getPersistData("ADD_SERVICE");
      if (!isEmpty(get(persistedData, "addServiceSteps", {}))) {
        dispatch({
          type: ActionTypes.SYNC_STATE,
          payload: persistedData,
        });
      }
      const clusterState = await ClusterApi.getPersistData("CLUSTER_STATE");
      const classicStep = resolveRecoveryStep(
        "addService",
        get(clusterState, "clusterState"),
      );
      const activeStepName = get(persistedData, "activeStep", "");
      const storedStep = Object.keys(stepWizardUtilities.wizardSteps).find(
        (stepNumber) =>
          stepWizardUtilities.wizardSteps?.[stepNumber]?.name === activeStepName,
      );
      const activeStep = classicStep ?? (storedStep === undefined ? 1 : Number(storedStep));
      const restoredStepData = clusterState && !isEmpty(clusterState)
        ? clusterState
        : {
            progressStatus: ClusterProgressStatus.ADDING_SERVICE,
            stepName: stepWizardUtilities.wizardSteps?.[activeStep]?.name,
          };
      currStepDataRef.current = restoredStepData;
      setCurrStepData(restoredStepData);
      stepWizardUtilities.jumpToStep(activeStep, true);
      if (loginName) {
        await claimWizard(loginName, "addServiceController");
      }
      isDataPersisted.current = true;
      setIsHydrated(true);
    } catch (error: any) {
      handleInitializationError(error);
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

  async function flushCurrentData(
    stateSnapshot: State = stateRef.current,
    stepSnapshot: Record<string, any> = currStepDataRef.current,
  ) {
    if (isCancelled.current) {
      return;
    }
    await ClusterApi.postPersistData(JSON.stringify({
      ADD_SERVICE: JSON.stringify({
        ...stateSnapshot,
        activeStep: get(stepSnapshot, "stepName", ""),
      }),
      CLUSTER_STATE: JSON.stringify(stepSnapshot),
    }));
  }

  async function flushOnCancel() {
    isCancelled.current = true;
    try {
      await queuePersistence(() => clearAddServiceWizardState(initialState));
      await releaseWizard();
    } catch (error: any) {
      isCancelled.current = false;
      throw error;
    }
    const returnPath = localStorage.getItem("module06WizardReturnPath")
      || "/main/dashboard/metrics";
    localStorage.removeItem("module06WizardReturnPath");
    modalManager.hide();
    window.location.href = `/#${returnPath}`;
    window.location.reload();
  }

  cancelWizardRef.current = flushOnCancel;

  useEffect(() => {
    const cancelWizard = () => {
      void cancelWizardRef.current?.();
    };
    window.addEventListener(CANCEL_ADD_SERVICE_WIZARD_EVENT, cancelWizard);
    return () => {
      window.removeEventListener(CANCEL_ADD_SERVICE_WIZARD_EVENT, cancelWizard);
      isCancelled.current = true;
    };
  }, []);

  async function flushOnStepChange(nextStep: number, clusterState?: string) {
    if (nextStep >= 1) {
      const nextStepDetails = stepWizardUtilities.wizardSteps?.[nextStep];
      const nextAddServiceSteps = { ...stateRef.current.addServiceSteps };
      if (nextStepDetails?.keysToRemove) {
        nextStepDetails.keysToRemove.forEach((key: string) => {
          delete nextAddServiceSteps[key];
        });
      }
      const nextState = {
        ...stateRef.current,
        addServiceSteps: nextAddServiceSteps,
      };
      dispatch({ type: ActionTypes.SYNC_STATE, payload: nextState });
      const nextStepData = {
        progressStatus: ClusterProgressStatus.ADDING_SERVICE,
        stepName: stepWizardUtilities?.wizardSteps?.[nextStep]?.name,
        ...(clusterState ? { clusterState } : {}),
      };
      currStepDataRef.current = nextStepData;
      setCurrStepData(nextStepData);
      await queuePersistence(() => flushCurrentData(nextState, nextStepData));
    }
  }

  async function flushStateToDb(
    operation: string = "default",
    jumpStep: number = -1,
    clusterState?: string,
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
        return await flushOnCancel();
      case "complete":
        return await flushOnCancel();
      case "back":
        return await flushOnStepChange(Number(activeStep) - 1, clusterState);
      case "next":
        return await flushOnStepChange(Number(activeStep) + 1, clusterState);
      case "jump":
        return await flushOnStepChange(jumpStep, clusterState);
      case "checkpoint": {
        const nextStepData = {
          ...currStepDataRef.current,
          progressStatus: ClusterProgressStatus.ADDING_SERVICE,
          stepName: stepWizardUtilities.currentStep.name,
          clusterState,
        };
        currStepDataRef.current = nextStepData;
        setCurrStepData(nextStepData);
        return await queuePersistence(() => flushCurrentData(stateRef.current, nextStepData));
      }
      default:
        return await queuePersistence(() => flushCurrentData());
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
    </AddServiceContext.Provider>
  );
};
