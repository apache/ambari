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

import React, { createContext, Dispatch, useEffect, useReducer, useRef, useState } from "react";
import { State, Action, ActionTypes } from "./types";
import { reducer, initialState } from "./reducer";
import ClusterApi from "../../../../api/clusterApi";
import { useDebounce } from "../../../../hooks/useDebounce";
import { ClusterProgressStatus } from "../../../../constants";
import { get, isEmpty } from "lodash";
import modalManager from "../../../../store/ModalManager";

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
}> = ({ stepWizardUtilities, hasManualCommands, children }) => {
  const [state, dispatch] = useReducer(reducer, initialState);
  const [currStepData, setCurrStepData] = useState({});
  const isDataPersisted = useRef(false);
  const debouncedPersist = useDebounce(flushCurrentData, 500);

  useEffect(() => {
    syncUserPersistedData();
  }, []);

  useEffect(() => {
    if (isDataPersisted.current) {
      debouncedPersist();
    }
  }, [state.reassignSteps, currStepData]);

  async function syncUserPersistedData() {
    try {
      const persistedData = await ClusterApi.getPersistData("REASSIGN_COMPONENT");
      if (!isEmpty(get(persistedData, "reassignSteps", {}))) {
        dispatch({
          type: ActionTypes.SYNC_STATE,
          payload: persistedData,
        });
      }
      
      // Only jump to persisted step if we haven't already progressed beyond step 1
      // This prevents the race condition where persisted data overrides user navigation
      const currentActiveStep = stepWizardUtilities.activeStep;
      const persistedActiveStepName = get(persistedData, "activeStep", "");
      
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
            if (currentActiveStep === 1 || persistedStepNum > currentActiveStep) {
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
    } catch (error) {
      console.error("Error syncing persisted data:", error);
    } finally {
      isDataPersisted.current = true;
    }
  }

  async function flushCurrentData() {
    await ClusterApi.postPersistData(
      JSON.stringify({
        REASSIGN_COMPONENT: JSON.stringify({
          ...state,
          activeStep: get(currStepData, "stepName", ""),
        }),
        CLUSTER_STATE: JSON.stringify(currStepData),
      })
    );
  }

  async function flushOnCancel() {
    await ClusterApi.postPersistData(
      JSON.stringify({
        REASSIGN_COMPONENT: JSON.stringify(initialState),
        CLUSTER_STATE: JSON.stringify({}),
      })
    );
    modalManager.hide();
    window.location.href="/#/main/dashboard/metrics";
    // window.location.reload();
  }

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

  function flushStateToDb(
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
        flushOnCancel();
        break;
      case "back":
        flushOnStepChange(Number(activeStep) - 1);
        break;
      case "next":
        flushOnStepChange(Number(activeStep) + 1);
        break;
      case "jump":
        flushOnStepChange(jumpStep);
        break;
      case "complete":
        // Clear persistence on completion
        ClusterApi.postPersistData(
          JSON.stringify({
            REASSIGN_COMPONENT: JSON.stringify(initialState),
            CLUSTER_STATE: JSON.stringify({}),
          })
        );
        break;
      default:
        flushCurrentData();
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
      {children}
    </ReassignContext.Provider>
  );
};
