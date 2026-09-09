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
import { Alert, Button } from "react-bootstrap";
import { get, isEmpty } from "lodash";
import { State, Action, ActionTypes } from "./types";
import { reducer, initialState } from "./reducer";
import { ClusterProgressStatus } from "../../../../../constants";
import modalManager from "../../../../../store/ModalManager";
import Spinner from "../../../../../components/Spinner";
import useAuth from "../../../../../hooks/useAuth";
import useClusterWorkflowPersistence from "../../../../../hooks/useClusterWorkflowPersistence";
import { AppContext } from "../../../../../store/context";
import { containsReentryMarker, workflowErrorMessage } from "../../../../../Utils/scopedWorkflow";
import { translate } from "../../../../../Utils/Utility";

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
  const { navigateCluster } = useContext(AppContext);
  const { hasAuthorization } = useAuth();
  const canPersist = hasAuthorization("CLUSTER.MANAGE_USER_PERSISTED_DATA");
  const persistence = useClusterWorkflowPersistence("NAMENODE_FEDERATION", {
    controllerNames: ["nameNodeFederationWizardController"],
    keys: ["NAMENODE_FEDERATION", "CLUSTER_STATE"],
  });
  const [state, reducerDispatch] = useReducer(reducer, initialState);
  const [isHydrated, setIsHydrated] = useState(false);
  const [initializationError, setInitializationError] = useState("");
  const [reentryRequired, setReentryRequired] = useState(false);
  const [retryCount, setRetryCount] = useState(0);
  const stateRef = useRef<State>(initialState);
  const currStepDataRef = useRef<Record<string, unknown>>({});
  const hydrationGeneration = useRef(0);
  const stepWizardUtilitiesRef = useRef(stepWizardUtilities);
  stepWizardUtilitiesRef.current = stepWizardUtilities;

  const dispatch: Dispatch<Action> = (action) => {
    stateRef.current = reducer(stateRef.current, action);
    reducerDispatch(action);
  };

  useEffect(() => {
    const generation = ++hydrationGeneration.current;
    void syncUserPersistedData(generation);
    return () => {
      if (hydrationGeneration.current === generation) hydrationGeneration.current += 1;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [canPersist, persistence, retryCount]);

  async function syncUserPersistedData(generation: number) {
    setIsHydrated(false);
    setInitializationError("");
    if (!canPersist || !persistence) {
      setInitializationError(
        !canPersist
          ? translate("workflow.persistence.permissionRequired")
          : translate("workflow.persistence.explicitCluster"),
      );
      return;
    }
    try {
      const values = retryCount > 0
        ? await persistence.reload()
        : await persistence.getPersistData();
      if (hydrationGeneration.current !== generation) return;
      const persistedData = (values?.NAMENODE_FEDERATION || initialState) as State;
      const currentWizardUtilities = stepWizardUtilitiesRef.current;
      const needsReentry = containsReentryMarker(persistedData);
      setReentryRequired(needsReentry);
      if (!isEmpty(persistedData.enableNamenodeFederationSteps)) {
        dispatch({ type: ActionTypes.SYNC_STATE, payload: persistedData });
      }
      const activeStepName = needsReentry
        ? currentWizardUtilities.wizardSteps[1]?.name
        : get(persistedData, "activeStep", "");
      if (activeStepName) {
        currStepDataRef.current = {
          progressStatus:
            ClusterProgressStatus.ENABLING_NAMENODE_FEDERATION,
          stepName: activeStepName,
        };
        const activeStepNumber = Object.keys(
          currentWizardUtilities.wizardSteps,
        ).find(
          (stepName) =>
            currentWizardUtilities.wizardSteps[stepName]?.name === activeStepName,
        );
        if (activeStepNumber !== undefined) {
          currentWizardUtilities.jumpToStep(Number(activeStepNumber), true);
        }
      } else {
        currentWizardUtilities.jumpToStep(0, true);
      }
      setIsHydrated(true);
    } catch (error: any) {
      if (hydrationGeneration.current !== generation) return;
      setInitializationError(workflowErrorMessage(
        error,
        translate("workflow.persistence.haLoadFailed"),
      ));
    }
  }

  async function flushCurrentData(
    stateSnapshot: State = stateRef.current,
    stepSnapshot: Record<string, unknown> = currStepDataRef.current,
  ) {
    if (!persistence) throw new Error(translate("workflow.persistence.explicitCluster"));
    const activeStep = get(stepSnapshot, "stepName", "");
    await persistence.savePersistData({
      NAMENODE_FEDERATION: { ...stateSnapshot, activeStep },
      CLUSTER_STATE: stepSnapshot,
    }, activeStep || ClusterProgressStatus.ENABLING_NAMENODE_FEDERATION);
    setReentryRequired(false);
  }

  async function clearPersistedState() {
    if (!persistence) throw new Error(translate("workflow.persistence.explicitCluster"));
    await persistence.release();
  }

  async function exitWorkflow() {
    if (stepWizardUtilities.activeStep >= 3) {
      await flushCurrentData();
    } else {
      await clearPersistedState();
    }
    modalManager.hide();
    navigateCluster("/main/services/HDFS/summary");
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
    await flushCurrentData(nextState, nextStepData);
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
        await flushCurrentData();
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
          disabled={!canPersist || !persistence}
        >
          {translate("common.retry")}
        </Button>
      </Alert>
    );
  }
  if (!isHydrated) return <Spinner />;

  return (
    <EnableNamenodeFederationContext.Provider
      value={{ state, dispatch, stepWizardUtilities, flushStateToDb }}
    >
      {reentryRequired && (
        <Alert variant="warning">
          {translate("workflow.persistence.reentryRequired")}
        </Alert>
      )}
      {children}
    </EnableNamenodeFederationContext.Provider>
  );
};
