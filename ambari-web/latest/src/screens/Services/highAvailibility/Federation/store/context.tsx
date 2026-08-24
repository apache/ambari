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

interface EnableNamenodeFederationContextProps {
  state: State;
  dispatch: Dispatch<Action>;
  stepWizardUtilities?: any;
  flushStateToDb: (
    operation?: "default" | "cancel" | "complete" | "back" | "next" | "jump",
    jumpStep?: number,
  ) => Promise<void>;
}

export const EnableNamenodeFederationContext =
  createContext<EnableNamenodeFederationContextProps>({
    state: initialState,
    dispatch: () => undefined,
    flushStateToDb: async () => undefined,
  });

export const EnableNamenodeFederationProvider: React.FC<{
  stepWizardUtilities: any;
  children: React.ReactNode;
}> = ({ stepWizardUtilities, children }) => {
  const { user, hasAuthorization } = useAuth();
  const workflowOwner = user?.user_name || "";
  const canPersist = hasAuthorization("CLUSTER.MANAGE_USER_PERSISTED_DATA");
  const [state, reducerDispatch] = useReducer(reducer, initialState);
  const [isHydrated, setIsHydrated] = useState(false);
  const [initializationError, setInitializationError] = useState("");
  const [retryCount, setRetryCount] = useState(0);
  const stateRef = useRef<State>(initialState);
  const currStepDataRef = useRef<Record<string, unknown>>({});
  const persistenceQueue = useRef<Promise<void>>(Promise.resolve());

  const dispatch: Dispatch<Action> = (action) => {
    stateRef.current = reducer(stateRef.current, action);
    reducerDispatch(action);
  };

  const queuePersistence = (operation: () => Promise<void>) => {
    const queued = persistenceQueue.current
      .catch(() => undefined)
      .then(operation);
    persistenceQueue.current = queued.catch(() => undefined);
    return queued;
  };

  useEffect(() => {
    void syncUserPersistedData();
  }, [retryCount]);

  async function getOptionalPersistedValue(key: string) {
    try {
      return await ClusterApi.getPersistData(key);
    } catch (error: any) {
      if (error?.response?.status === 404 || error?.status === 404) return null;
      throw error;
    }
  }

  async function syncUserPersistedData() {
    setIsHydrated(false);
    setInitializationError("");
    if (!canPersist) {
      setInitializationError(
        "NameNode Federation requires permission to persist wizard recovery data.",
      );
      return;
    }
    try {
      const [workflowResponse, ownerResponse] = await Promise.all([
        getOptionalPersistedValue("NAMENODE_FEDERATION"),
        getOptionalPersistedValue("wizard-data"),
      ]);
      const persistedData = parsePersistedValue(
        workflowResponse,
        initialState,
      );
      const owner = parsePersistedValue<Record<string, string>>(
        ownerResponse,
        {},
      );
      if (
        !isEmpty(persistedData.enableNamenodeFederationSteps) &&
        owner.userName &&
        workflowOwner &&
        owner.userName !== workflowOwner
      ) {
        throw new Error(
          `This workflow is owned by ${owner.userName}. Ask that user to finish or clear it.`,
        );
      }
      if (!isEmpty(persistedData.enableNamenodeFederationSteps)) {
        dispatch({ type: ActionTypes.SYNC_STATE, payload: persistedData });
      }
      const activeStepName = get(persistedData, "activeStep", "");
      if (activeStepName) {
        currStepDataRef.current = {
          progressStatus:
            ClusterProgressStatus.ENABLING_NAMENODE_FEDERATION,
          stepName: activeStepName,
        };
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
          error?.message ||
          "Ambari could not restore the NameNode Federation workflow.",
      );
    }
  }

  async function flushCurrentData(
    stateSnapshot: State = stateRef.current,
    stepSnapshot: Record<string, unknown> = currStepDataRef.current,
  ) {
    await ClusterApi.postPersistData(
      persistedPayload({
        NAMENODE_FEDERATION: {
          ...stateSnapshot,
          activeStep: get(stepSnapshot, "stepName", ""),
        },
        CLUSTER_STATE: stepSnapshot,
        "wizard-data": {
          userName: workflowOwner,
          controllerName: "nameNodeFederationWizardController",
        },
      }),
    );
  }

  async function clearPersistedState() {
    await queuePersistence(() =>
      ClusterApi.postPersistData(
        persistedPayload({
          NAMENODE_FEDERATION: initialState,
          CLUSTER_STATE: {},
          "wizard-data": {},
        }),
      ),
    );
  }

  async function exitWorkflow() {
    if (stepWizardUtilities.activeStep >= 3) {
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
      progressStatus: ClusterProgressStatus.ENABLING_NAMENODE_FEDERATION,
      stepName: nextStepDetails?.name,
    };
    currStepDataRef.current = nextStepData;
    await queuePersistence(() => flushCurrentData(nextState, nextStepData));
  }

  async function flushStateToDb(
    operation:
      | "default"
      | "cancel"
      | "complete"
      | "back"
      | "next"
      | "jump" = "default",
    jumpStep = -1,
  ) {
    switch (operation) {
      case "cancel":
        await exitWorkflow();
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
          disabled={!canPersist}
        >
          Retry
        </Button>
      </Alert>
    );
  }
  if (!isHydrated) return <Spinner />;

  return (
    <EnableNamenodeFederationContext.Provider
      value={{ state, dispatch, stepWizardUtilities, flushStateToDb }}
    >
      {children}
    </EnableNamenodeFederationContext.Provider>
  );
};
