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
  useEffect,
  useReducer,
  useRef,
  useState,
} from "react";
import { State, Action, ActionTypes } from "./types";
import { reducer, initialState } from "./reducer";
import ClusterApi from "../../../api/clusterApi";
import { get, isEmpty } from "lodash";
import { redirectToAdminView } from "../../../Utils/adminViewRedirect";
import { ClusterProgressStatus } from "../../../constants";
import { Alert, Button } from "react-bootstrap";
import { claimWizard, releaseWizard } from "../../../Utils/wizardOwnership";
import { resolveRecoveryStep } from "../wizardRecovery";
import { AppContext } from "../../../store/context";

interface ClusterCreationContextProps {
  state: State;
  dispatch: Dispatch<Action>;
  stepWizardUtilities?: any;
  flushStateToDb?: any;
}

export const ClusterCreationContext =
  createContext<ClusterCreationContextProps>({
    state: initialState,
    dispatch: () => undefined,
    flushStateToDb: () => undefined,
  });

export const ClusterCreationProvider: React.FC<{
  stepWizardUtilities: any;
  children: React.ReactNode;
}> = ({ stepWizardUtilities, children }) => {
  const [state, reducerDispatch] = useReducer(reducer, initialState);
  const [currStepData, setCurrStepData] = useState({});
  const [initializationError, setInitializationError] = useState<string | null>(null);
  const [retryCount, setRetryCount] = useState(0);
  const { loginName } = React.useContext(AppContext);

  const isDataPersisted = useRef(false);
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

  useEffect(() => {
    void syncUserPersistedData();
  }, [retryCount]);

  useEffect(() => {
    if (isDataPersisted.current) {
      void queuePersistence(() => flushCurrentData(state, currStepData));
    }
  }, [state.clusterCreationSteps, currStepData]);

  async function syncUserPersistedData() {
    setInitializationError(null);
    isDataPersisted.current = false;
    try {
      const persistedData = await ClusterApi.getPersistData("CLUSTER_CURRENT");
      if (!isEmpty(get(persistedData, "clusterCreationSteps", {}))) {
        dispatch({
          type: ActionTypes.SYNC_STATE,
          payload: persistedData,
        });
      }
      const clusterState = await ClusterApi.getPersistData("CLUSTER_STATE");
      const classicStep = resolveRecoveryStep(
        "clusterCreation",
        get(clusterState, "clusterState"),
      );
      const activeStepName = get(clusterState, "stepName", "");
      const storedStep = Object.keys(stepWizardUtilities.wizardSteps).find(
        (stepNumber) =>
          stepWizardUtilities.wizardSteps?.[stepNumber]?.name === activeStepName,
      );
      const activeStep = classicStep ?? (storedStep === undefined ? 0 : Number(storedStep));
      if (clusterState && (classicStep !== undefined || activeStepName)) {
        currStepDataRef.current = clusterState;
        setCurrStepData(clusterState);
      }
      stepWizardUtilities.jumpToStep(activeStep, true);
      if (loginName) {
        await claimWizard(loginName, "clusterCreation");
      }
      isDataPersisted.current = true;
    } catch (error: any) {
      setInitializationError(
        error?.response?.data?.message
          || error?.message
          || "Ambari could not restore the cluster installation wizard.",
      );
    }
  }

  async function flushCurrentData(
    stateSnapshot: State = stateRef.current,
    stepSnapshot: Record<string, any> = currStepDataRef.current,
  ) {
    await ClusterApi.postPersistData(
      JSON.stringify({
        CLUSTER_CURRENT: JSON.stringify(stateSnapshot),
        CLUSTER_STATE: JSON.stringify(stepSnapshot),
      })
    );
  }

  async function flushOnCancel() {
    await queuePersistence(() => flushCurrentData());
    await releaseWizard();
    redirectToAdminView();
  }

  async function flushOnComplete() {
    await queuePersistence(() => ClusterApi.postPersistData(
      JSON.stringify({
        CLUSTER_CURRENT: JSON.stringify(initialState),
        CLUSTER_STATE: JSON.stringify({}),
      })
    ));
    await releaseWizard();
  }

  async function flushOnStepChange(nextStep: number, clusterState?: string) {
    if (nextStep >= 0) {
      const nextStepDetails = stepWizardUtilities.wizardSteps?.[nextStep];
      const nextClusterCreationSteps = {
        ...stateRef.current.clusterCreationSteps,
      };
      if (nextStepDetails?.keysToRemove) {
        nextStepDetails.keysToRemove.forEach((key: string) => {
          delete nextClusterCreationSteps[key];
        });
      }
      const nextState = {
        ...stateRef.current,
        clusterCreationSteps: nextClusterCreationSteps,
      };
      dispatch({ type: ActionTypes.SYNC_STATE, payload: nextState });
      const nextStepData = {
        progressStatus: ClusterProgressStatus.PROVISIONING,
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
        await flushOnCancel();
        break;
      case "complete":
        await flushOnComplete();
        break;
      case "back":
        await flushOnStepChange(Number(activeStep) - 1, clusterState);
        break;
      case "next":
        await flushOnStepChange(Number(activeStep) + 1, clusterState);
        break;
      case "jump":
        await flushOnStepChange(jumpStep, clusterState);
        break;
      case "checkpoint": {
        const nextStepData = {
          ...currStepDataRef.current,
          progressStatus: ClusterProgressStatus.PROVISIONING,
          stepName: stepWizardUtilities.currentStep.name,
          clusterState,
        };
        currStepDataRef.current = nextStepData;
        setCurrStepData(nextStepData);
        await queuePersistence(() => flushCurrentData(stateRef.current, nextStepData));
        break;
      }
      default:
        await queuePersistence(() => flushCurrentData());
    }
  }

  return (
    <ClusterCreationContext.Provider
      value={{
        state,
        dispatch,
        stepWizardUtilities,
        flushStateToDb,
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
    </ClusterCreationContext.Provider>
  );
};
