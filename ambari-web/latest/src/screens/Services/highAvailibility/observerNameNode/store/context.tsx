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
import { State, Action, ActionTypes } from "./types";
import { reducer, initialState } from "./reducer";
import { get, isEmpty } from "lodash";
import { ClusterProgressStatus } from "../../../../../constants";
import modalManager from "../../../../../store/ModalManager";
import { AppContext } from "../../../../../store/context";
import Spinner from "../../../../../components/Spinner";
import useAuth from "../../../../../hooks/useAuth";
import useClusterWorkflowPersistence from "../../../../../hooks/useClusterWorkflowPersistence";
import { containsReentryMarker, workflowErrorMessage } from "../../../../../Utils/scopedWorkflow";
import { translate } from "../../../../../Utils/Utility";

interface AddObserverNamenodeContextProps {
  state: State;
  dispatch: Dispatch<Action>;
  stepWizardUtilities?: any;
  flushStateToDb?: any;
}

export const AddObserverNamenodeContext =
  createContext<AddObserverNamenodeContextProps>({
    state: initialState,
    dispatch: () => undefined,
    flushStateToDb: () => undefined,
  });

export const AddObserverNamenodeProvider: React.FC<{
  stepWizardUtilities: any;
  children: React.ReactNode;
}> = ({ stepWizardUtilities, children }) => {
  const { navigateCluster } = useContext(AppContext);
  const { hasAuthorization } = useAuth();
  const canPersist = hasAuthorization("CLUSTER.MANAGE_USER_PERSISTED_DATA");
  const persistence = useClusterWorkflowPersistence("OBSERVER_NAMENODE", {
    keys: ["OBSERVER_NAMENODE", "CLUSTER_STATE"],
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
      const persistedData = (values?.OBSERVER_NAMENODE || initialState) as State;
      const currentWizardUtilities = stepWizardUtilitiesRef.current;
      const needsReentry = containsReentryMarker(persistedData);
      setReentryRequired(needsReentry);
      if (!isEmpty(get(persistedData, "addObserverNamenodeSteps", {}))) {
        dispatch({
          type: ActionTypes.SYNC_STATE,
          payload: persistedData,
        });
      }
      if (needsReentry || get(persistedData, "activeStep", "")) {
        try {
          const activeStepName = needsReentry
            ? currentWizardUtilities.wizardSteps[2]?.name
            : get(persistedData, "activeStep");
          currStepDataRef.current = {
            progressStatus: ClusterProgressStatus.ADDING_OBSERVER_NAMENODE,
            stepName: activeStepName,
          };
          const activeStepNumber = Object.keys(
            currentWizardUtilities.wizardSteps
          ).find((stepName) => {
            return (
              currentWizardUtilities.wizardSteps?.[stepName]?.name ===
              activeStepName
            );
          });
          if (activeStepNumber !== undefined) {
            currentWizardUtilities.jumpToStep(Number(activeStepNumber), true);
          }
        } catch (err) {
          console.error("Error while jumping to step", err);
        }
      } else {
        currentWizardUtilities.jumpToStep(0, true);
      }
      setIsHydrated(true);
    } catch (error) {
      if (hydrationGeneration.current !== generation) return;
      setInitializationError(workflowErrorMessage(
        error,
        translate("workflow.persistence.haLoadFailed"),
      ));
    }
  }

  async function flushCurrentData(
    stateSnapshot = stateRef.current,
    stepSnapshot = currStepDataRef.current,
  ) {
    if (!persistence) throw new Error(translate("workflow.persistence.explicitCluster"));
    const activeStep = typeof stepSnapshot.stepName === "string" ? stepSnapshot.stepName : "";
    await persistence.savePersistData({
      OBSERVER_NAMENODE: { ...stateSnapshot, activeStep },
      CLUSTER_STATE: stepSnapshot,
    }, activeStep || ClusterProgressStatus.ADDING_OBSERVER_NAMENODE);
    setReentryRequired(false);
  }

  async function flushOnCancel() {
    if (!persistence) throw new Error(translate("workflow.persistence.explicitCluster"));
    await persistence.release();
    modalManager.hide();
    navigateCluster("/main/services/HDFS/summary");
  }

  async function flushOnStepChange(nextStep: number) {
    if (nextStep >= 0) {
      const nextStepDetails = stepWizardUtilitiesRef.current.wizardSteps?.[nextStep];
      let nextState = stateRef.current;
      if (nextStepDetails?.keysToRemove) {
        nextStepDetails.keysToRemove.forEach((key: string) => {
          if (nextState?.addObserverNamenodeSteps?.[key]) {
            nextState = reducer(nextState, {
              type: ActionTypes.REMOVE_KEY,
              payload: { key },
            });
          }
        });
      }
      if (nextState !== stateRef.current) {
        dispatch({ type: ActionTypes.SYNC_STATE, payload: nextState });
      }
      const nextStepData = {
        progressStatus: ClusterProgressStatus.ADDING_OBSERVER_NAMENODE,
        stepName: nextStepDetails?.name,
      };
      currStepDataRef.current = nextStepData;
      await flushCurrentData(nextState, nextStepData);
    }
  }

  async function flushStateToDb(
    operation: string = "default",
    jumpStep: number = -1
  ) {
    const activeStep = Object.keys(stepWizardUtilities.wizardSteps).find(
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
        await flushCurrentData();
    }
  }

  if (initializationError) {
    return (
      <Alert variant="danger">
        {initializationError}
        <Button
          className="ms-3"
          disabled={!canPersist || !persistence}
          onClick={() => setRetryCount((value) => value + 1)}
          size="sm"
        >
          {translate("common.retry")}
        </Button>
      </Alert>
    );
  }
  if (!isHydrated) return <Spinner />;

  return (
    <AddObserverNamenodeContext.Provider
      value={{ state, dispatch, stepWizardUtilities, flushStateToDb }}
    >
      {reentryRequired && (
        <Alert variant="warning">
          {translate("workflow.persistence.reentryRequired")}
        </Alert>
      )}
      {children}
    </AddObserverNamenodeContext.Provider>
  );
};
