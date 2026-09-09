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
import { Step } from "../../../../../types/StepWizard";
import { AppContext } from "../../../../../store/context";
import { containsReentryMarker, workflowErrorMessage } from "../../../../../Utils/scopedWorkflow";
import { translate } from "../../../../../Utils/Utility";

type PersistOperation = "default" | "cancel" | "complete" | "back" | "next" | "jump";

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

interface EnableHighAvailibilityContextProps {
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
export const EnableHighAvailibilityContext =
  createContext<EnableHighAvailibilityContextProps>({
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

export const EnableHighAvailibilityProvider: React.FC<{
  stepWizardUtilities: unknown;
  children: React.ReactNode;
}> = ({ stepWizardUtilities: stepWizardUtilitiesInput, children }) => {
  const { navigateCluster } = useContext(AppContext);
  const stepWizardUtilities =
    stepWizardUtilitiesInput as StepWizardUtilities;
  const { hasAuthorization } = useAuth();
  const canPersist = hasAuthorization("CLUSTER.MANAGE_USER_PERSISTED_DATA");
  const persistence = useClusterWorkflowPersistence("HIGH_AVAILIBILITY_RM_HA", {
    controllerNames: ["rMHighAvailabilityWizardController"],
    keys: ["HIGH_AVAILIBILITY_RM_HA", "CLUSTER_STATE"],
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
      const persistedData = (values?.HIGH_AVAILIBILITY_RM_HA || initialState) as State;
      const currentWizardUtilities = stepWizardUtilitiesRef.current;
      const needsReentry = containsReentryMarker(persistedData);
      setReentryRequired(needsReentry);
      if (!isEmpty(get(persistedData, "enableHighAvailibilitySteps", {}))) {
        dispatch({ type: ActionTypes.SYNC_STATE, payload: persistedData });
      }
      const activeStepName = needsReentry
        ? currentWizardUtilities.wizardSteps[2]?.name
        : get(persistedData, "activeStep", "");
      if (activeStepName) {
        const restoredStepData = {
          progressStatus: ClusterProgressStatus.ENABLING_RM_HA,
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
          currentWizardUtilities.jumpToStep(Number(activeStepNumber), true);
        }
      } else {
        currentWizardUtilities.jumpToStep(1, true);
      }
      setIsHydrated(true);
    } catch (error: unknown) {
      if (hydrationGeneration.current !== generation) return;
      const requestError = error as RequestError;
      setInitializationError(workflowErrorMessage(
        requestError,
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
      HIGH_AVAILIBILITY_RM_HA: { ...stateSnapshot, activeStep },
      CLUSTER_STATE: stepSnapshot,
    }, activeStep || ClusterProgressStatus.ENABLING_RM_HA);
    setReentryRequired(false);
  }

  async function clearPersistedState() {
    if (!persistence) throw new Error(translate("workflow.persistence.explicitCluster"));
    await persistence.release();
  }

  async function flushOnCancel() {
    if (stepWizardUtilities.activeStep >= 4) {
      await flushCurrentData();
    } else {
      await clearPersistedState();
    }
    modalManager.hide();
    navigateCluster("/main/services/YARN/summary");
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
      progressStatus: ClusterProgressStatus.ENABLING_RM_HA,
      stepName: nextStepDetails?.name,
    };
    currStepDataRef.current = nextStepData;
    await flushCurrentData(nextState, nextStepData);
  }

  async function flushStateToDb(
    operation: PersistOperation = "default",
    jumpStep = -1,
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
    <EnableHighAvailibilityContext.Provider
      value={{ state, dispatch, stepWizardUtilities, flushStateToDb }}
    >
      {reentryRequired && (
        <Alert variant="warning">
          {translate("workflow.persistence.reentryRequired")}
        </Alert>
      )}
      {children}
    </EnableHighAvailibilityContext.Provider>
  );
};
