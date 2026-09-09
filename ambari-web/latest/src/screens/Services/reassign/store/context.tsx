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

import React, { createContext, Dispatch, useContext, useEffect, useReducer, useRef, useState } from "react";
import { State, Action, ActionTypes } from "./types";
import { reducer, initialState } from "./reducer";
import { useDebounce } from "../../../../hooks/useDebounce";
import { ClusterProgressStatus } from "../../../../constants";
import { get, isEmpty } from "lodash";
import modalManager from "../../../../store/ModalManager";
import { AppContext } from "../../../../store/context";
import useClusterWorkflowPersistence from "../../../../hooks/useClusterWorkflowPersistence";
import { Alert, Button } from "react-bootstrap";
import Spinner from "../../../../components/Spinner";
import { containsReentryMarker, workflowErrorMessage } from "../../../../Utils/scopedWorkflow";
import { translate } from "../../../../Utils/Utility";

interface ReassignContextProps {
  state: State;
  dispatch: Dispatch<Action>;
  stepWizardUtilities?: any;
  flushStateToDb?: any;
  hasManualCommands: boolean;
}

export const ReassignContext = createContext<ReassignContextProps>({
  state: initialState,
  dispatch: () => undefined,
  flushStateToDb: () => undefined,
  hasManualCommands: false,
});

export const ReassignProvider: React.FC<{
  stepWizardUtilities: any;
  hasManualCommands: boolean;
  children: React.ReactNode;
  onCancelReady?: (cancel: () => Promise<void>) => void;
}> = ({ stepWizardUtilities, hasManualCommands, children, onCancelReady }) => {
  const { navigateCluster } = useContext(AppContext);
  const [state, dispatch] = useReducer(reducer, initialState);
  const [currStepData, setCurrStepData] = useState({});
  const [isHydrated, setIsHydrated] = useState(false);
  const [initializationError, setInitializationError] = useState("");
  const [reentryRequired, setReentryRequired] = useState(false);
  const [retryCount, setRetryCount] = useState(0);
  const isDataPersisted = useRef(false);
  const persistence = useClusterWorkflowPersistence("REASSIGN_COMPONENT", {
    controllerNames: ["reassignMasterController"],
    keys: ["REASSIGN_COMPONENT", "CLUSTER_STATE"],
  });
  const debouncedPersist = useDebounce(() => {
    void flushCurrentData().catch((error: any) => {
      isDataPersisted.current = false;
      setInitializationError(workflowErrorMessage(
        error,
        "Ambari could not save the component reassignment workflow.",
      ));
    });
  }, 500);

  useEffect(() => {
    void syncUserPersistedData();
  }, [persistence, retryCount]);

  useEffect(() => {
    if (isDataPersisted.current && !reentryRequired) {
      debouncedPersist();
    }
  }, [state.reassignSteps, currStepData, reentryRequired]);

  async function syncUserPersistedData() {
    setInitializationError("");
    setIsHydrated(false);
    isDataPersisted.current = false;
    try {
      if (!persistence) {
        throw new Error(String(translate("workflow.persistence.explicitCluster")));
      }
      const persistedValues = retryCount > 0
        ? await persistence.reload()
        : await persistence.getPersistData();
      const persistedData = get(persistedValues, "REASSIGN_COMPONENT", {});
      const needsReentry = containsReentryMarker(persistedData);
      setReentryRequired(needsReentry);
      if (!isEmpty(get(persistedData, "reassignSteps", {}))) {
        dispatch({
          type: ActionTypes.SYNC_STATE,
          payload: persistedData,
        });
      }
      
      // Only jump to persisted step if we haven't already progressed beyond step 1
      // This prevents the race condition where persisted data overrides user navigation
      const currentActiveStep = stepWizardUtilities.activeStep;
      const persistedActiveStepName = needsReentry
        ? "REVIEW"
        : get(persistedData, "activeStep", "");
      
      if (persistedActiveStepName) {
        try {
          setCurrStepData({
            progressStatus: ClusterProgressStatus.REASSIGNING_COMPONENT,
            stepName: persistedActiveStepName,
          });
          
          let persistedStepNumber = Object.keys(
            stepWizardUtilities.wizardSteps
          ).find((stepName) => {
            return (
              stepWizardUtilities.wizardSteps?.[stepName]?.name ===
              persistedActiveStepName
            );
          });
          
          if (persistedStepNumber) {
            const persistedStepNum = Number(persistedStepNumber);
            // Only jump to persisted step if:
            // 1. Current step is still 1 (user hasn't navigated yet), OR
            // 2. Persisted step is greater than current step (resume from further step)
            if (needsReentry || currentActiveStep === 1 || persistedStepNum > currentActiveStep) {
              stepWizardUtilities.jumpToStep(persistedStepNum, true);
            }
          }
        } catch (err) {
          console.error("Error while jumping to step", err);
        }
      } else if (currentActiveStep === 1) {
        // Only jump to step 1 if we're still at the initial step
        stepWizardUtilities.jumpToStep(1, true);
      }
      isDataPersisted.current = true;
      setIsHydrated(true);
    } catch (error) {
      setInitializationError(workflowErrorMessage(
        error,
        translate("workflow.persistence.reassignLoadFailed"),
      ));
    }
  }

  async function flushCurrentData() {
    if (!persistence) throw new Error(translate("workflow.persistence.explicitCluster"));
    await persistence.savePersistData({
        REASSIGN_COMPONENT: {
          ...state,
          activeStep: get(currStepData, "stepName", ""),
        },
        CLUSTER_STATE: currStepData,
      }, String(get(currStepData, "stepName") || "REASSIGN_COMPONENT"));
    setReentryRequired(false);
  }

  async function releaseWorkflow() {
    if (!persistence) throw new Error(translate("workflow.persistence.explicitCluster"));
    await persistence.release();
  }

  async function flushOnCancel() {
    await releaseWorkflow();
    modalManager.hide();
    navigateCluster("/main/dashboard/metrics");
    // window.location.reload();
  }

  onCancelReady?.(releaseWorkflow);

  async function flushOnStepChange(nextStep: number) {
    if (nextStep >= 1) {
      let nextStepDetails = stepWizardUtilities.wizardSteps?.[nextStep];
      if (nextStepDetails?.keysToRemove) {
        nextStepDetails.keysToRemove.forEach((key: string) => {
          if (state?.reassignSteps?.[key]) {
            dispatch({
              type: ActionTypes.REMOVE_KEY,
              payload: { key },
            });
          }
        });
      }
      setCurrStepData({
        progressStatus: ClusterProgressStatus.REASSIGNING_COMPONENT,
        stepName: stepWizardUtilities?.wizardSteps?.[nextStep]?.name,
      });
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
        return flushOnCancel();
      case "back":
        return flushOnStepChange(Number(activeStep) - 1);
      case "next":
        return flushOnStepChange(Number(activeStep) + 1);
      case "jump":
        return flushOnStepChange(jumpStep);
      case "complete":
        // Clear persistence on completion
        if (!persistence) throw new Error(translate("workflow.persistence.explicitCluster"));
        return persistence.release();
      default:
        return flushCurrentData();
    }
  }

  return (
    <ReassignContext.Provider
      value={{
        state,
        dispatch,
        stepWizardUtilities,
        flushStateToDb,
        hasManualCommands,
      }}
    >
      {reentryRequired && (
        <Alert className="m-3" variant="warning">
          {translate("workflow.persistence.reentryRequired")}
        </Alert>
      )}
      {initializationError ? (
        <Alert className="m-3" variant="danger">
          <div>{initializationError}</div>
          <Button className="mt-3" onClick={() => setRetryCount((value) => value + 1)}>
            {translate("common.retry")}
          </Button>
        </Alert>
      ) : !isHydrated ? <Spinner /> : children}
    </ReassignContext.Provider>
  );
};
