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
import { AppContext } from "../../../../store/context";
import { forEach, get, isEmpty, isEqual } from "lodash";
import VersionsApi from "../../../../api/versionsApi";
import { HostsApi } from "../../../../api/hostsApi";
import { getAllComponents } from "../../../Hosts/utils";
import { ServiceApi } from "../../../../api/serviceApi";
import { ClusterProgressStatus } from "../../../../constants";
import modalManager from "../../../../store/ModalManager";
import {
  CANCEL_ADD_SERVICE_WIZARD_EVENT,
} from "../../../../Utils/addServicePersistence";
import { Alert, Button } from "react-bootstrap";
import { resolveRecoveryStep } from "../../../ClusterWizard/wizardRecovery";
import useClusterWorkflowPersistence from "../../../../hooks/useClusterWorkflowPersistence";
import Spinner from "../../../../components/Spinner";
import {
  containsReentryMarker,
  WorkflowQueueInvalidatedError,
  workflowErrorMessage,
  WorkflowMutationQueue,
} from "../../../../Utils/scopedWorkflow";
import { translate } from "../../../../Utils/Utility";
import { consumeWorkflowReturnPath } from "../../../../Utils/workflowReturnPath";

interface AddServiceContextProps {
  state: State;
  dispatch: Dispatch<Action>;
  stepWizardUtilities?: any;
  flushStateToDb?: any;
  storeStepDataAndFlush?: (
    step: string,
    data: Record<string, unknown>,
  ) => Promise<number>;
  withStateCheckpoint?: <T>(
    request: (revision: number) => Promise<T>,
  ) => Promise<T>;
  getWorkflowRevision?: () => number;
  installedHosts: string[];
  serviceContextLoading: boolean;
  installedServices: string[];
  workflowMaterializedServices: string[];
}

export const AddServiceContext = createContext<AddServiceContextProps>({
  state: initialState,
  dispatch: () => undefined,
  flushStateToDb: () => undefined,
  storeStepDataAndFlush: async () => 0,
  withStateCheckpoint: async (request) => request(0),
  getWorkflowRevision: () => 0,
  installedHosts: [],
  serviceContextLoading: false,
  installedServices: [],
  workflowMaterializedServices: [],
});

export const classifyAddServiceServices = (
  items: Array<{ ServiceInfo?: { service_name?: string; state?: string } }>,
  state: State,
  target: { clusterId: number; clusterName: string },
) => {
  const creationIntent = get(
    state,
    "addServiceSteps.REVIEW.data.serviceCreationIntent",
    null,
  );
  const ownsCreationIntent = Number(creationIntent?.clusterId) === target.clusterId
    && creationIntent?.clusterName === target.clusterName;
  const workflowFreshServices = new Set<string>(
    ownsCreationIntent && Array.isArray(creationIntent?.serviceNames)
      ? creationIntent.serviceNames
      : [],
  );
  const installedServices: string[] = [];
  const workflowMaterializedServices: string[] = [];
  items.forEach((item) => {
    const serviceName = item.ServiceInfo?.service_name;
    if (!serviceName) return;
    if (item.ServiceInfo?.state === "INIT" && workflowFreshServices.has(serviceName)) {
      workflowMaterializedServices.push(serviceName);
    } else {
      installedServices.push(serviceName);
    }
  });
  return { installedServices, workflowMaterializedServices };
};

export const AddServiceProvider: React.FC<{
  stepWizardUtilities: any;
  children: React.ReactNode;
}> = ({ stepWizardUtilities, children }) => {
  const [state, reducerDispatch] = useReducer(reducer, initialState);
  const [installedHosts, setInstalledHosts] = useState([]);
  const [installedServices, setInstalledServices] = useState([]);
  const [workflowMaterializedServices, setWorkflowMaterializedServices] = useState<string[]>([]);
  const [installedServicesLoaded, setInstalledServicesLoaded] = useState(false);
  const [serviceContextLoading, setServiceContextLoading] = useState(true);
  const [currStepData, setCurrStepData] = useState({});
  const [initializationError, setInitializationError] = useState<string | null>(null);
  const [errorGeneration, setErrorGeneration] = useState(-1);
  const [reentryRequired, setReentryRequired] = useState(false);
  const [retryCount, setRetryCount] = useState(0);
  const {
    cluster,
    clusterName,
    loginName,
    navigateCluster,
    serviceComponentInfo,
  } =
    useContext(AppContext);
  const persistence = useClusterWorkflowPersistence("ADD_SERVICE", {
    controllerNames: ["addServiceController"],
    keys: ["ADD_SERVICE", "CLUSTER_STATE"],
  });
  const [hydratedScope, setHydratedScope] = useState<{
    persistence: typeof persistence;
    generation: number;
  } | null>(null);

  const isDataPersisted = useRef(false);
  const isCancelled = useRef(false);
  const cancelWizardRef = useRef<(() => Promise<void>) | null>(null);
  const stateRef = useRef<State>(initialState);
  const currStepDataRef = useRef<Record<string, any>>({});
  const scopeGeneration = useRef(0);
  const workflowQueueRef = useRef(new WorkflowMutationQueue());
  const persistenceRef = useRef<typeof persistence>(null);
  const explicitlyPersistedStateRef = useRef<State | null>(null);
  const hasCurrentHydration = hydratedScope?.persistence === persistence
    && hydratedScope?.generation === scopeGeneration.current;

  const dispatch: Dispatch<Action> = (action) => {
    stateRef.current = reducer(stateRef.current, action);
    reducerDispatch(action);
  };

  const queuePersistence = useCallback((operation: () => Promise<any>) => {
    if (!persistence) {
      return Promise.reject(new Error(String(translate("workflow.persistence.explicitCluster"))));
    }
    const generation = scopeGeneration.current;
    const queueGeneration = workflowQueueRef.current.currentGeneration;
    const queued = workflowQueueRef.current.enqueue(async () => {
      await operation();
      if (generation !== scopeGeneration.current
        || persistenceRef.current !== persistence
        || !workflowQueueRef.current.isCurrent(queueGeneration)) {
        throw new WorkflowQueueInvalidatedError("Add Service persistence scope changed.");
      }
    });
    void queued.catch((error) => {
      if (error instanceof WorkflowQueueInvalidatedError
        || !workflowQueueRef.current.isCurrent(queueGeneration)) return;
      if (generation === scopeGeneration.current && persistenceRef.current === persistence) {
        isDataPersisted.current = false;
        setErrorGeneration(generation);
        setInitializationError(workflowErrorMessage(
          error,
          translate("workflow.persistence.addServiceSaveFailed"),
        ));
      }
    });
    return queued;
  }, [persistence]);

  useEffect(() => {
    workflowQueueRef.current.activate();
    return () => {
      isDataPersisted.current = false;
      workflowQueueRef.current.deactivate();
    };
  }, []);

  const setClusterName = () => {
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: "NAME",
        data: { clusterName },
      },
    });
  };

  const setStackAndVersion = async (generation: number) => {
    const response = await VersionsApi.getServices(clusterName);
    if (generation !== scopeGeneration.current) return;
    
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
    if (generation !== scopeGeneration.current) return;
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
    if (hasCurrentHydration && clusterName) {
      const generation = scopeGeneration.current;
      void getAlreadyInstalledServices(generation).catch((error) =>
        handleInitializationError(error, generation));
    }
  }, [clusterName, hasCurrentHydration, hydratedScope]);

  useEffect(() => {
    const generation = scopeGeneration.current + 1;
    scopeGeneration.current = generation;
    persistenceRef.current = persistence;
    isDataPersisted.current = false;
    stateRef.current = initialState;
    currStepDataRef.current = {};
    dispatch({ type: ActionTypes.SYNC_STATE, payload: initialState });
    setCurrStepData({});
    setReentryRequired(false);
    setInstalledHosts([]);
    setInstalledServices([]);
    setWorkflowMaterializedServices([]);
    setInstalledServicesLoaded(false);
    setServiceContextLoading(true);
    const persistenceSnapshot = persistence;
    void syncUserPersistedData(generation, persistenceSnapshot);
    return () => {
      if (scopeGeneration.current === generation) scopeGeneration.current += 1;
    };
  }, [persistence, retryCount]);

  useEffect(() => {
    if (hasCurrentHydration && clusterName && !state.addServiceSteps?.NAME) {
      setClusterName();
    }
  }, [clusterName, hasCurrentHydration, hydratedScope, state.addServiceSteps?.NAME]);

  useEffect(() => {
    if (
      hasCurrentHydration
      && clusterName
      && !isEmpty(serviceComponentInfo)
      && (!state.addServiceSteps?.HOST_STATUS || !state.addServiceSteps?.MASTERS)
    ) {
      const generation = scopeGeneration.current;
      void getHostComponents(generation).catch((error) =>
        handleInitializationError(error, generation));
    }
  }, [clusterName, hasCurrentHydration, hydratedScope, serviceComponentInfo]);

  useEffect(() => {
    if (hasCurrentHydration && clusterName && !state.addServiceSteps?.VERSION) {
      setServiceContextLoading(true);
      const generation = scopeGeneration.current;
      void setStackAndVersion(generation).catch((error) =>
        handleInitializationError(error, generation));
    } else if (hasCurrentHydration) {
      setServiceContextLoading(false);
    }
  }, [clusterName, hasCurrentHydration, hydratedScope, state.addServiceSteps?.VERSION]);

  useEffect(() => {
    if (isDataPersisted.current && !reentryRequired) {
      if (explicitlyPersistedStateRef.current
        && isEqual(explicitlyPersistedStateRef.current, state)) {
        explicitlyPersistedStateRef.current = null;
        return;
      }
      void queuePersistence(() => flushCurrentData(state, currStepData))
        .catch(() => undefined);
    }
  }, [state.addServiceSteps, currStepData, reentryRequired]);

  useEffect(() => {
    if (hasCurrentHydration) isDataPersisted.current = true;
  }, [hasCurrentHydration, hydratedScope]);

  const handleInitializationError = (error: any, generation = scopeGeneration.current) => {
    if (generation !== scopeGeneration.current) return;
    isDataPersisted.current = false;
    setErrorGeneration(generation);
    setInitializationError(
      workflowErrorMessage(error, translate("workflow.persistence.addServiceLoadFailed")),
    );
  };

  async function syncUserPersistedData(
    generation: number,
    persistenceSnapshot: typeof persistence,
  ) {
    const queueGeneration = await workflowQueueRef.current.reset();
    if (!workflowQueueRef.current.isCurrent(queueGeneration)
      || generation !== scopeGeneration.current
      || persistenceRef.current !== persistenceSnapshot) return;
    setInitializationError(null);
    setErrorGeneration(-1);
    setHydratedScope(null);
    isDataPersisted.current = false;
    isCancelled.current = false;
    try {
      if (!persistenceSnapshot) {
        throw new Error(String(translate("workflow.persistence.explicitCluster")));
      }
      const persistedValues = retryCount > 0
        ? await persistenceSnapshot.reload()
        : await persistenceSnapshot.getPersistData();
      if (generation !== scopeGeneration.current || persistenceRef.current !== persistenceSnapshot) {
        return;
      }
      const persistedData = get(persistedValues, "ADD_SERVICE", {});
      const restoredData = !isEmpty(get(persistedData, "addServiceSteps", {}))
        ? persistedData
        : initialState;
      const needsReentry = containsReentryMarker(restoredData);
      setReentryRequired(needsReentry);
      dispatch({
        type: ActionTypes.SYNC_STATE,
        payload: restoredData,
      });
      const clusterState = get(persistedValues, "CLUSTER_STATE", {});
      const classicStep = resolveRecoveryStep(
        "addService",
        get(clusterState, "clusterState"),
      );
      const activeStepName = get(restoredData, "activeStep", "");
      const storedStep = Object.keys(stepWizardUtilities.wizardSteps).find(
        (stepNumber) =>
          stepWizardUtilities.wizardSteps?.[stepNumber]?.name === activeStepName,
      );
      const activeStep = needsReentry
        ? 4
        : classicStep ?? (storedStep === undefined ? 1 : Number(storedStep));
      const restoredStepData = clusterState && !isEmpty(clusterState)
        ? clusterState
        : {
            progressStatus: ClusterProgressStatus.ADDING_SERVICE,
            stepName: stepWizardUtilities.wizardSteps?.[activeStep]?.name,
          };
      currStepDataRef.current = restoredStepData;
      setCurrStepData(restoredStepData);
      stepWizardUtilities.jumpToStep(activeStep, true);
      setHydratedScope({ persistence: persistenceSnapshot, generation });
    } catch (error: any) {
      handleInitializationError(error, generation);
    }
  }

  const getAlreadyInstalledServices = async (generation: number) => {
    const installedServicesApi = await ServiceApi.getAllServices(clusterName);
    if (generation !== scopeGeneration.current) return;
    const classified = classifyAddServiceServices(
      installedServicesApi.items,
      stateRef.current,
      { clusterId: Number(cluster?.cluster_id), clusterName },
    );
    setInstalledServices(classified.installedServices as any);
    setWorkflowMaterializedServices(classified.workflowMaterializedServices);
    setInstalledServicesLoaded(true);
  };

  const getHostComponents = async (generation: number) => {
    const response = await HostsApi.getHostComponentsDetails(
      clusterName,
      "fields=host_components/HostRoles/state&minimal_response=true"
    );
    if (generation !== scopeGeneration.current) return;
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
    if (!persistence) throw new Error(String(translate("workflow.persistence.explicitCluster")));
    await persistence.savePersistData({
      ADD_SERVICE: {
        ...stateSnapshot,
        activeStep: get(stepSnapshot, "stepName", ""),
      },
      CLUSTER_STATE: stepSnapshot,
    }, String(get(stepSnapshot, "clusterState") || get(stepSnapshot, "stepName") || "ADD_SERVICE"));
    setReentryRequired(false);
  }

  async function storeStepDataAndFlush(
    step: string,
    data: Record<string, unknown>,
  ) {
    const action: Action = {
      type: ActionTypes.STORE_INFORMATION,
      payload: { step, data },
    };
    const nextState = reducer(stateRef.current, action);
    stateRef.current = nextState;
    explicitlyPersistedStateRef.current = nextState;
    reducerDispatch(action);
    await queuePersistence(() => flushCurrentData(nextState, currStepDataRef.current));
    return persistence?.currentRevision || 0;
  }

  async function withStateCheckpoint<T>(
    request: (revision: number) => Promise<T>,
  ): Promise<T> {
    const stateSnapshot = stateRef.current;
    const stepSnapshot = currStepDataRef.current;
    explicitlyPersistedStateRef.current = stateSnapshot;
    const generation = scopeGeneration.current;
    const queueGeneration = workflowQueueRef.current.currentGeneration;
    const queued = workflowQueueRef.current.enqueue(async () => {
      await flushCurrentData(stateSnapshot, stepSnapshot);
      if (generation !== scopeGeneration.current
        || persistenceRef.current !== persistence
        || !workflowQueueRef.current.isCurrent(queueGeneration)) {
        throw new WorkflowQueueInvalidatedError("Add Service persistence scope changed.");
      }
      const revision = persistence?.currentRevision || 0;
      try {
        return { ok: true as const, value: await request(revision) };
      } catch (error) {
        return { ok: false as const, error };
      }
    });
    void queued.catch((error) => {
      if (error instanceof WorkflowQueueInvalidatedError
        || !workflowQueueRef.current.isCurrent(queueGeneration)) return;
      if (generation === scopeGeneration.current && persistenceRef.current === persistence) {
        isDataPersisted.current = false;
        setErrorGeneration(generation);
        setInitializationError(workflowErrorMessage(
          error,
          translate("workflow.persistence.addServiceSaveFailed"),
        ));
      }
    });
    const outcome = await queued;
    if (!outcome.ok) throw outcome.error;
    return outcome.value;
  }

  async function flushOnCancel() {
    const generation = scopeGeneration.current;
    isCancelled.current = true;
    try {
      await queuePersistence(async () => {
        if (!persistence) return;
        await persistence.release();
      });
    } catch (error: any) {
      if (generation === scopeGeneration.current) isCancelled.current = false;
      throw error;
    }
    const returnPath = consumeWorkflowReturnPath({
      clusterId: cluster?.cluster_id,
      principal: loginName,
      workflow: "ADD_SERVICE",
    }, "/main/services");
    modalManager.hide();
    navigateCluster(returnPath);
  }

  cancelWizardRef.current = flushOnCancel;

  useEffect(() => {
    const cancelWizard = () => {
      const cancellation = cancelWizardRef.current?.();
      void cancellation?.catch(() => undefined);
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

  const currentInitializationError = persistenceRef.current === persistence
    && errorGeneration === scopeGeneration.current
    ? initializationError
    : null;
  return (
    <AddServiceContext.Provider
      value={{
        state,
        dispatch,
        stepWizardUtilities,
        flushStateToDb,
        storeStepDataAndFlush,
        withStateCheckpoint,
        getWorkflowRevision: () => persistence?.currentRevision || 0,
        installedHosts,
        serviceContextLoading,
        installedServices,
        workflowMaterializedServices,
      }}
    >
      {hasCurrentHydration && reentryRequired && (
        <Alert variant="warning" className="m-4">
          {translate("workflow.persistence.reentryRequired")}
        </Alert>
      )}
      {currentInitializationError ? (
        <Alert variant="danger" className="m-4">
          {currentInitializationError}{" "}
          <Button
            size="sm"
            variant="outline-danger"
            onClick={() => setRetryCount((value) => value + 1)}
          >
            {translate("common.retry")}
          </Button>
        </Alert>
      ) : !hasCurrentHydration || !installedServicesLoaded ? (
        <Spinner />
      ) : children}
    </AddServiceContext.Provider>
  );
};
