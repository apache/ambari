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
  useEffect,
  useReducer,
  useRef,
  useState,
} from "react";
import { Alert, Button } from "react-bootstrap";
import { get, isEmpty } from "lodash";
import { State, Action, ActionTypes } from "./types";
import { reducer, initialState } from "./reducer";
import ClusterApi from "../../../../../api/clusterApi";
import { ClusterProgressStatus } from "../../../../../constants";
import modalManager from "../../../../../store/ModalManager";
import Spinner from "../../../../../components/Spinner";
import {
  parsePersistedValue,
  persistedPayload,
} from "../../../../../Utils/persistedSettings";
import useAuth from "../../../../../hooks/useAuth";

interface EnableHighAvailibilityContextProps {
  state: State;
  dispatch: Dispatch<Action>;
  stepWizardUtilities?: any;
  flushStateToDb?: any;
}

export const EnableHighAvailibilityContext =
  createContext<EnableHighAvailibilityContextProps>({
    state: initialState,
    dispatch: () => undefined,
    flushStateToDb: () => undefined,
  });

export const EnableHighAvailibilityProvider: React.FC<{
  stepWizardUtilities: any;
  children: React.ReactNode;
}> = ({ stepWizardUtilities, children }) => {
  const { user } = useAuth();
  const workflowOwner = user?.user_name || "";
  const [state, reducerDispatch] = useReducer(reducer, initialState);
  const [isHydrated, setIsHydrated] = useState(false);
  const [initializationError, setInitializationError] = useState("");
  const [retryCount, setRetryCount] = useState(0);
  const stateRef = useRef<State>(initialState);
  const currStepDataRef = useRef<Record<string, any>>({});
  const persistenceQueue = useRef<Promise<void>>(Promise.resolve());

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

  useEffect(() => {
    void syncUserPersistedData();
  }, [retryCount]);

  async function syncUserPersistedData() {
    setIsHydrated(false);
    setInitializationError("");
    try {
      let response: unknown = initialState;
      try {
        response = await ClusterApi.getPersistData(
          "HIGH_AVAILIBILITY_NAMENODE",
        );
      } catch (error: any) {
        if (error?.response?.status !== 404 && error?.status !== 404) {
          throw error;
        }
      }
      const persistedData = parsePersistedValue(response, initialState);
      if (!isEmpty(get(persistedData, "enableHighAvailibilitySteps", {}))) {
        dispatch({ type: ActionTypes.SYNC_STATE, payload: persistedData });
      }
      const activeStepName = get(persistedData, "activeStep", "");
      if (activeStepName) {
        const restoredStepData = {
          progressStatus: ClusterProgressStatus.ENABLING_NAMENODE_HA,
          stepName: activeStepName,
        };
        currStepDataRef.current = restoredStepData;
        const activeStepNumber = Object.keys(
          stepWizardUtilities.wizardSteps,
        ).find(
          (stepName) =>
            stepWizardUtilities.wizardSteps[stepName]?.name === activeStepName,
        );
        if (activeStepNumber !== undefined) {
          stepWizardUtilities.jumpToStep(Number(activeStepNumber), true);
        }
      } else {
        stepWizardUtilities.jumpToStep(0, true);
      }
      setIsHydrated(true);
    } catch (error: any) {
      setInitializationError(
        error?.response?.data?.message ||
          "Ambari could not restore the NameNode HA workflow.",
      );
    }
  }

  async function flushCurrentData(
    stateSnapshot: State = stateRef.current,
    stepSnapshot: Record<string, any> = currStepDataRef.current,
  ) {
    await ClusterApi.postPersistData(
      persistedPayload({
        HIGH_AVAILIBILITY_NAMENODE: {
          ...stateSnapshot,
          activeStep: get(stepSnapshot, "stepName", ""),
        },
        CLUSTER_STATE: stepSnapshot,
        "wizard-data": { userName: workflowOwner },
      }),
    );
  }

  async function clearPersistedState() {
    await queuePersistence(() =>
      ClusterApi.postPersistData(
        persistedPayload({
          HIGH_AVAILIBILITY_NAMENODE: initialState,
          CLUSTER_STATE: {},
          "wizard-data": {},
        }),
      ),
    );
  }

  async function flushOnCancel() {
    if (stepWizardUtilities.activeStep >= 4) {
      await queuePersistence(() => flushCurrentData());
    } else {
      await clearPersistedState();
    }
    modalManager.hide();
    window.location.href = "/#/main/services/HDFS/summary";
  }

  async function flushOnStepChange(nextStep: number | undefined) {
    if (nextStep === undefined || nextStep < 0) return;
    const nextStepDetails = stepWizardUtilities.wizardSteps[nextStep];
    let nextState = stateRef.current;
    nextStepDetails?.keysToRemove?.forEach((key: string) => {
      nextState = reducer(nextState, {
        type: ActionTypes.REMOVE_KEY,
        payload: { key },
      });
    });
    if (nextState !== stateRef.current) {
      dispatch({ type: ActionTypes.SYNC_STATE, payload: nextState });
    }
    const nextStepData = {
      progressStatus: ClusterProgressStatus.ENABLING_NAMENODE_HA,
      stepName: nextStepDetails?.name,
    };
    currStepDataRef.current = nextStepData;
    await queuePersistence(() => flushCurrentData(nextState, nextStepData));
  }

  async function flushStateToDb(
    operation: string = "default",
    jumpStep: number = -1,
  ) {
    switch (operation) {
      case "cancel":
        await flushOnCancel();
        break;
      case "complete":
        await clearPersistedState();
        break;
      case "back":
        await flushOnStepChange(stepWizardUtilities.prevStepNumber);
        break;
      case "next":
        await flushOnStepChange(stepWizardUtilities.nextStepNumber);
        break;
      case "jump":
        await flushOnStepChange(jumpStep);
        break;
      default:
        await queuePersistence(() => flushCurrentData());
    }
  }

  if (initializationError) {
    return (
      <Alert variant="danger">
        {initializationError}
        <Button
          size="sm"
          className="ms-3"
          onClick={() => setRetryCount((value) => value + 1)}
        >
          Retry
        </Button>
      </Alert>
    );
  }
  if (!isHydrated) return <Spinner />;

  return (
    <EnableHighAvailibilityContext.Provider
      value={{ state, dispatch, stepWizardUtilities, flushStateToDb }}
    >
      {children}
    </EnableHighAvailibilityContext.Provider>
  );
};
