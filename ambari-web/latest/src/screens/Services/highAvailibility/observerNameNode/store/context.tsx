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
import { get, isEmpty } from "lodash";
import { ClusterProgressStatus } from "../../../../../constants";
import modalManager from "../../../../../store/ModalManager";

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
  const [state, dispatch] = useReducer(reducer, initialState);
  const [currStepData, setCurrStepData] = useState({});

  const isDataPersisted = useRef(false);

  useEffect(() => {
    syncUserPersistedData();
  }, []);

  useEffect(() => {
    if (isDataPersisted.current) {
      flushCurrentData();
    }
  }, [state.addObserverNamenodeSteps, currStepData]);

  async function syncUserPersistedData() {
    try {
      const persistedData = await ClusterApi.getPersistData(
        "OBSERVER_NAMENODE"
      );
      if (!isEmpty(get(persistedData, "addObserverNamenodeSteps", {}))) {
        dispatch({
          type: ActionTypes.SYNC_STATE,
          payload: persistedData,
        });
      }
      if (get(persistedData, "activeStep", "")) {
        try {
          const activeStepName = get(persistedData, "activeStep");
          setCurrStepData({
            progressStatus: ClusterProgressStatus.ADDING_OBSERVER_NAMENODE,
            stepName: activeStepName,
          });
          const activeStepNumber = Object.keys(
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
        stepWizardUtilities.jumpToStep(0, true);
      }
    } finally {
      isDataPersisted.current = true;
    }
  }

  async function flushCurrentData() {
    await ClusterApi.postPersistData(
      JSON.stringify({
        OBSERVER_NAMENODE: JSON.stringify({
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
        OBSERVER_NAMENODE: JSON.stringify(initialState),
        CLUSTER_STATE: JSON.stringify({}),
      })
    );
    modalManager.hide();
    window.location.href = "/#/main/services/HDFS/summary";
    window.location.reload();
  }

  async function flushOnStepChange(nextStep: number) {
    if (nextStep >= 0) {
      const nextStepDetails = stepWizardUtilities.wizardSteps?.[nextStep];
      if (nextStepDetails?.keysToRemove) {
        nextStepDetails.keysToRemove.forEach((key: string) => {
          if (state?.addObserverNamenodeSteps?.[key]) {
            dispatch({
              type: ActionTypes.REMOVE_KEY,
              payload: { key },
            });
          }
        });
      }
      setCurrStepData({
        progressStatus: ClusterProgressStatus.ADDING_OBSERVER_NAMENODE,
        stepName: stepWizardUtilities?.wizardSteps?.[nextStep]?.name,
      });
    }
  }

  function flushStateToDb(
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
    <AddObserverNamenodeContext.Provider
      value={{ state, dispatch, stepWizardUtilities, flushStateToDb }}
    >
      {children}
    </AddObserverNamenodeContext.Provider>
  );
};
