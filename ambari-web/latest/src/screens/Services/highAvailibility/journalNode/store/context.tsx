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

interface ManageJournalNodesContextProps {
  state: State;
  dispatch: Dispatch<Action>;
  stepWizardUtilities?: any;
  flushStateToDb?: any;
}

export const ManageJournalNodesContext =
  createContext<ManageJournalNodesContextProps>({
    state: initialState,
    dispatch: () => undefined,
    flushStateToDb: () => undefined,
  });

export const ManageJournalNodesProvider: React.FC<{
  stepWizardUtilities: any;
  children: React.ReactNode;
}> = ({ stepWizardUtilities, children }) => {
  const { navigateCluster } = useContext(AppContext);
  const { hasAuthorization } = useAuth();
  const canPersist = hasAuthorization("CLUSTER.MANAGE_USER_PERSISTED_DATA");
  const persistence = useClusterWorkflowPersistence("MANAGE_JOURNALNODES", {
    controllerNames: ["manageJournalNodeWizardController"],
    keys: ["MANAGE_JOURNALNODES", "CLUSTER_STATE"],
  });
  const [state, reducerDispatch] = useReducer(reducer, initialState);
  const [isHydrated, setIsHydrated] = useState(false);
  const [initializationError, setInitializationError] = useState("");
  const [reentryRequired, setReentryRequired] = useState(false);
  const [retryCount, setRetryCount] = useState(0);
  const stateRef = useRef<State>(initialState);
  const currStepDataRef = useRef<Record<string, any>>({});
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
      if (hydrationGeneration.current === generation) {
        hydrationGeneration.current += 1;
      }
    };
    // The latest wizard utilities are read through a ref so step navigation does not rehydrate.
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
      const persistedData = (values?.MANAGE_JOURNALNODES || initialState) as State;
      const needsReentry = containsReentryMarker(persistedData);
      setReentryRequired(needsReentry);
      const currentWizardUtilities = stepWizardUtilitiesRef.current;
      if (!isEmpty(get(persistedData, "manageJournalNodesSteps", {}))) {
        dispatch({ type: ActionTypes.SYNC_STATE, payload: persistedData });
      }
      const isDeleteOnly = get(
        persistedData,
        "manageJournalNodesSteps.REVIEW.data.isDeleteOnly",
        false,
      );
      currentWizardUtilities.setStepsHidden([2, 4], isDeleteOnly);
      const activeStepName = needsReentry
        ? currentWizardUtilities.wizardSteps[1]?.name
        : get(persistedData, "activeStep", "");
      if (activeStepName) {
        const restoredStepData = {
          progressStatus: ClusterProgressStatus.MANAGING_JOURNALNODES,
          stepName: activeStepName,
        };
        currStepDataRef.current = restoredStepData;
        const activeStepNumber = Object.keys(
          currentWizardUtilities.wizardSteps,
        ).find(
          (stepName) =>
            currentWizardUtilities.wizardSteps[stepName]?.name === activeStepName,
        );
        if (activeStepNumber !== undefined) {
          let restoredStep = Number(activeStepNumber);
          if (isDeleteOnly && restoredStep === 2) restoredStep = 3;
          if (isDeleteOnly && restoredStep === 4) restoredStep = 5;
          currentWizardUtilities.jumpToStep(restoredStep, true);
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
    stepSnapshot: Record<string, any> = currStepDataRef.current,
  ) {
    if (!persistence) throw new Error(translate("workflow.persistence.explicitCluster"));
    const activeStep = get(stepSnapshot, "stepName", "");
    await persistence.savePersistData({
      MANAGE_JOURNALNODES: { ...stateSnapshot, activeStep },
      CLUSTER_STATE: stepSnapshot,
    }, activeStep || ClusterProgressStatus.MANAGING_JOURNALNODES);
    setReentryRequired(false);
  }

  async function clearPersistedState() {
    if (!persistence) throw new Error(translate("workflow.persistence.explicitCluster"));
    await persistence.release();
  }

  async function flushOnCancel() {
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
      progressStatus: ClusterProgressStatus.MANAGING_JOURNALNODES,
      stepName: nextStepDetails?.name,
    };
    currStepDataRef.current = nextStepData;
    await flushCurrentData(nextState, nextStepData);
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
    <ManageJournalNodesContext.Provider
      value={{ state, dispatch, stepWizardUtilities, flushStateToDb }}
    >
      {reentryRequired && (
        <Alert variant="warning">
          {translate("workflow.persistence.reentryRequired")}
        </Alert>
      )}
      {children}
    </ManageJournalNodesContext.Provider>
  );
};
