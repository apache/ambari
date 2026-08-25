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
import Spinner from "../../../../../components/Spinner";
import modalManager from "../../../../../store/ModalManager";
import {
  parsePersistedValue,
  persistedPayload,
} from "../../../../../Utils/persistedSettings";
import useAuth from "../../../../../hooks/useAuth";
import { Step } from "../../../../../types/StepWizard";

export const RANGER_ADMIN_HA_PERSIST_KEY = "HIGH_AVAILIBILITY_RANGER_HA";

type PersistOperation =
  | "default"
  | "cancel"
  | "complete"
  | "back"
  | "next"
  | "jump";

type WizardStepDefinition = Partial<Step> & {
  name?: string;
  keysToRemove?: string[];
};

type StepWizardUtilities = {
  activeStep: number;
  prevStepNumber?: number;
  nextStepNumber?: number;
  wizardSteps: Record<string, WizardStepDefinition>;
  currentStep: Step & { name: string; keysToRemove?: string[] };
  jumpToStep: (step: number, imperative?: boolean) => void;
  handleNextImperitive: (targetStep?: number) => Promise<void>;
  handleBackImperitive: () => Promise<void>;
};

type RequestError = {
  message?: string;
  status?: number;
  response?: { status?: number; data?: { message?: string } };
};

interface EnableHighAvailibilityRangerAdminContextProps {
  state: State;
  dispatch: Dispatch<Action>;
  stepWizardUtilities: StepWizardUtilities;
  flushStateToDb: (
    operation?: PersistOperation,
    jumpStep?: number,
  ) => Promise<void>;
}

// The provider and context intentionally share this module to match the HA stores.
// eslint-disable-next-line react-refresh/only-export-components
export const EnableHighAvailibilityRangerAdminContext =
  createContext<EnableHighAvailibilityRangerAdminContextProps>({
    state: initialState,
    dispatch: () => undefined,
    stepWizardUtilities: {
      activeStep: 1,
      wizardSteps: {},
      currentStep: {
        name: "GET_STARTED",
        label: "Get Started",
        completed: false,
        Component: null,
        canGoBack: false,
        isNextEnabled: false,
      },
      jumpToStep: () => undefined,
      handleNextImperitive: async () => undefined,
      handleBackImperitive: async () => undefined,
    },
    flushStateToDb: async () => undefined,
  });

// eslint-disable-next-line react-refresh/only-export-components
export const clearRangerAdminHaPersistedState = () =>
  ClusterApi.postPersistData(
    persistedPayload({
      [RANGER_ADMIN_HA_PERSIST_KEY]: initialState,
      CLUSTER_STATE: {},
      "wizard-data": {},
    }),
  );

export const EnableHighAvailibilityProvider: React.FC<{
  stepWizardUtilities: unknown;
  children: React.ReactNode;
}> = ({ stepWizardUtilities: stepWizardUtilitiesInput, children }) => {
  const stepWizardUtilities =
    stepWizardUtilitiesInput as StepWizardUtilities;
  const { user } = useAuth();
  const workflowOwner = user?.user_name || "";
  const [state, reducerDispatch] = useReducer(reducer, initialState);
  const [isHydrated, setIsHydrated] = useState(false);
  const [initializationError, setInitializationError] = useState("");
  const [retryCount, setRetryCount] = useState(0);
  const stateRef = useRef<State>(initialState);
  const currentStepRef = useRef<Record<string, unknown>>({});
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
    // Hydration is intentionally retried only through retryCount.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [retryCount]);

  async function syncUserPersistedData() {
    setIsHydrated(false);
    setInitializationError("");
    try {
      let response: unknown = initialState;
      try {
        response = await ClusterApi.getPersistData(RANGER_ADMIN_HA_PERSIST_KEY);
      } catch (error: unknown) {
        const requestError = error as RequestError;
        if (
          requestError.response?.status !== 404 &&
          requestError.status !== 404
        ) {
          throw error;
        }
      }
      const persistedData = parsePersistedValue(response, initialState);
      if (
        !isEmpty(
          get(persistedData, "enableHighAvailibilityRangerAdminSteps", {}),
        )
      ) {
        dispatch({ type: ActionTypes.SYNC_STATE, payload: persistedData });
      }
      const activeStepName = get(persistedData, "activeStep", "");
      if (activeStepName) {
        const restoredStep = {
          progressStatus: ClusterProgressStatus.ENABLING_RANGER_ADMIN_HA,
          stepName: activeStepName,
        };
        currentStepRef.current = restoredStep;
        const activeStepNumber = Object.keys(
          stepWizardUtilities.wizardSteps,
        ).find(
          (stepNumber) =>
            stepWizardUtilities.wizardSteps[stepNumber]?.name === activeStepName,
        );
        if (activeStepNumber !== undefined) {
          stepWizardUtilities.jumpToStep(Number(activeStepNumber), true);
        }
      } else {
        stepWizardUtilities.jumpToStep(1, true);
      }
      setIsHydrated(true);
    } catch (error: unknown) {
      const requestError = error as RequestError;
      setInitializationError(
        requestError.response?.data?.message ||
          requestError.message ||
          "Ambari could not restore the Ranger Admin HA workflow.",
      );
    }
  }

  async function flushCurrentData(
    stateSnapshot = stateRef.current,
    stepSnapshot = currentStepRef.current,
  ) {
    await ClusterApi.postPersistData(
      persistedPayload({
        [RANGER_ADMIN_HA_PERSIST_KEY]: {
          ...stateSnapshot,
          activeStep: get(stepSnapshot, "stepName", ""),
        },
        CLUSTER_STATE: stepSnapshot,
        "wizard-data": { userName: workflowOwner },
      }),
    );
  }

  async function clearPersistedState() {
    await queuePersistence(clearRangerAdminHaPersistedState);
  }

  async function flushOnCancel() {
    if (stepWizardUtilities.activeStep >= 4) {
      await queuePersistence(() => flushCurrentData());
    } else {
      await clearPersistedState();
    }
    modalManager.hide();
    window.location.href = "/#/main/services/RANGER/summary";
  }

  async function flushOnStepChange(nextStep: number | undefined) {
    if (nextStep === undefined || nextStep < 1) return;
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
      progressStatus: ClusterProgressStatus.ENABLING_RANGER_ADMIN_HA,
      stepName: nextStepDetails?.name,
    };
    currentStepRef.current = nextStepData;
    await queuePersistence(() => flushCurrentData(nextState, nextStepData));
  }

  async function flushStateToDb(
    operation: PersistOperation = "default",
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
    <EnableHighAvailibilityRangerAdminContext.Provider
      value={{ state, dispatch, stepWizardUtilities, flushStateToDb }}
    >
      {children}
    </EnableHighAvailibilityRangerAdminContext.Provider>
  );
};
