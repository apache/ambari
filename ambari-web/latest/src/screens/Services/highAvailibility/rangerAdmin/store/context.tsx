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
import { State, Action, ActionTypes } from "./types";
import { reducer, initialState } from "./reducer";
import ClusterApi from "../../../../../api/clusterApi";
import { ClusterProgressStatus } from "../../../../../constants";
import { get, isEmpty } from "lodash";

interface EnableHighAvailibilityRangerAdminContextProps {
  state: State;
  dispatch: Dispatch<Action>;
  stepWizardUtilities?: any;
  flushStateToDb?: any;
}

export const EnableHighAvailibilityRangerAdminContext =
  createContext<EnableHighAvailibilityRangerAdminContextProps>({
    state: initialState,
    dispatch: () => undefined,
    flushStateToDb: () => undefined,
  });

export const EnableHighAvailibilityProvider: React.FC<{
  stepWizardUtilities: any;
  children: React.ReactNode;
}> = ({ stepWizardUtilities, children }) => {
  const [state, dispatch] = useReducer(reducer, initialState);
  const isDataPersisted = useRef(false);
  const [currStepData, setCurrStepData] = useState({});

  useEffect(() => {
    syncUserPersistedData();
  }, []);

  useEffect(() => {
    if (isDataPersisted.current) {
      flushCurrentData();
    }
  }, [state.enableHighAvailibilityRangerAdminSteps, currStepData]);

  async function syncUserPersistedData() {
    try {
      const persistedData = await ClusterApi.getPersistData(
        "HIGH_AVAILIBILITY_RANGER_HA"
      );
      if (
        !isEmpty(
          get(persistedData, "enableHighAvailibilityRangerAdminSteps", {})
        )
      ) {
        dispatch({
          type: ActionTypes.SYNC_STATE,
          payload: persistedData,
        });
      }
      if (get(persistedData, "activeStep", "")) {
        try {
          const activeStepName = get(persistedData, "activeStep");
          setCurrStepData({
            progressStatus: ClusterProgressStatus.ENABLING_RANGER_ADMIN_HA,
            stepName: activeStepName,
          });
          let activeStepNumber = Object.keys(
            stepWizardUtilities.wizardSteps
          ).find((stepName) => {
            return (
              stepWizardUtilities.wizardSteps?.[stepName]?.name ===
              activeStepName
            );
          });
          stepWizardUtilities.jumpToStep(Number(activeStepNumber), true);
        } catch (err) {
          console.error("Error while jumping to step", err);
        }
      } else {
        stepWizardUtilities.jumpToStep(1, true);
      }
    } finally {
      isDataPersisted.current = true;
    }
  }

  async function flushCurrentData() {
    await ClusterApi.postPersistData(
      JSON.stringify({
        HIGH_AVAILIBILITY_RANGER_HA: JSON.stringify({
          ...state,
          activeStep: get(currStepData, "stepName", ""),
        }),
        CLUSTER_STATE: JSON.stringify(currStepData),
      })
    );
  }

  function flushOnCancel() {
    ClusterApi.postPersistData(
      JSON.stringify({
        HIGH_AVAILIBILITY_RANGER_HA: JSON.stringify(initialState),
        CLUSTER_STATE: JSON.stringify({}),
      })
    );
    window.location.href = "/#/main/services/RANGER/summary";
  }

  async function flushOnStepChange(nextStep: number) {
    if (nextStep >= 1) {
      let nextStepDetails = stepWizardUtilities.wizardSteps?.[nextStep];
      if (nextStepDetails?.keysToRemove) {
        nextStepDetails.keysToRemove.forEach((key: string) => {
          if (state?.enableHighAvailibilityRangerAdminSteps?.[key]) {
            dispatch({
              type: ActionTypes.REMOVE_KEY,
              payload: { key },
            });
          }
        });
      }
      setCurrStepData({
        progressStatus: ClusterProgressStatus.ENABLING_RANGER_ADMIN_HA,
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
      default:
        flushCurrentData();
    }
  }

  return (
    <EnableHighAvailibilityRangerAdminContext.Provider
      value={{ state, dispatch, stepWizardUtilities, flushStateToDb }}
    >
      {children}
    </EnableHighAvailibilityRangerAdminContext.Provider>
  );
};
