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
  useCallback,
  useEffect,
  useReducer,
  useRef,
  useState,
} from "react";
import { State, Action, ActionTypes } from "./types";
import { reducer, initialState } from "./reducer";
import { get, isEqual } from "lodash";
import { redirectToAdminView } from "../../../Utils/adminViewRedirect";
import { ClusterProgressStatus } from "../../../constants";
import { Alert, Button } from "react-bootstrap";
import { resolveRecoveryStep } from "../wizardRecovery";
import {
  clusterCreationReentrySteps,
  clusterDraftPath,
  createClusterDraftId,
  isClusterDraftId,
  projectClusterCreationValues,
  ScopedWorkflowSession,
  WorkflowMutationQueue,
  WorkflowQueueInvalidatedError,
  workflowErrorMessage,
} from "../../../Utils/scopedWorkflow";
import { useLocation, useNavigate, useSearchParams } from "react-router-dom";
import Spinner from "../../../components/Spinner";
import { useTranslation } from "react-i18next";

interface ClusterCreationContextProps {
  state: State;
  dispatch: Dispatch<Action>;
  stepWizardUtilities?: any;
  flushStateToDb?: any;
  storeStepDataAndFlush?: (
    step: string,
    data: Record<string, unknown>,
  ) => Promise<number>;
  withStateCheckpoint?: <T>(
    request: (revision: number) => Promise<T>,
  ) => Promise<T>;
  draftId?: string;
  draftRevision?: number;
  getDraftRevision?: () => number;
}

export const ClusterCreationContext =
  createContext<ClusterCreationContextProps>({
    state: initialState,
    dispatch: () => undefined,
    flushStateToDb: () => undefined,
    storeStepDataAndFlush: async () => 0,
    withStateCheckpoint: async (request) => request(0),
    draftId: undefined,
    draftRevision: 0,
    getDraftRevision: () => 0,
  });

export const ClusterCreationProvider: React.FC<{
  stepWizardUtilities: any;
  children: React.ReactNode;
}> = ({ stepWizardUtilities, children }) => {
  const { t } = useTranslation();
  const [state, reducerDispatch] = useReducer(reducer, initialState);
  const [currStepData, setCurrStepData] = useState({});
  const [isHydrated, setIsHydrated] = useState(false);
  const [hydratedDraftId, setHydratedDraftId] = useState<string | null>(null);
  const [initializationError, setInitializationError] = useState<string | null>(null);
  const [retryCount, setRetryCount] = useState(0);
  const [draftRevision, setDraftRevision] = useState(0);
  const [searchParams] = useSearchParams();
  const location = useLocation();
  const navigate = useNavigate();
  const routeDraftId = searchParams.get("draft");
  const generatedDraftIdRef = useRef<string | null>(null);
  if (!generatedDraftIdRef.current) generatedDraftIdRef.current = createClusterDraftId();
  const draftId = isClusterDraftId(routeDraftId)
    ? routeDraftId
    : generatedDraftIdRef.current as string;
  const hasInvalidDraftId = Boolean(routeDraftId && !isClusterDraftId(routeDraftId));

  const isDataPersisted = useRef(false);
  const stateRef = useRef<State>(initialState);
  const currStepDataRef = useRef<Record<string, any>>({});
  const workflowSessionRef = useRef<ScopedWorkflowSession | null>(null);
  const workflowQueueRef = useRef(new WorkflowMutationQueue());
  const explicitlyPersistedStateRef = useRef<State | null>(null);
  const reentrySteps = clusterCreationReentrySteps({ state });

  const dispatch: Dispatch<Action> = (action) => {
    stateRef.current = reducer(stateRef.current, action);
    reducerDispatch(action);
  };

  const queuePersistence = useCallback((operation: () => Promise<any>) => {
    const generation = workflowQueueRef.current.currentGeneration;
    const nextOperation = workflowQueueRef.current.enqueue(operation).then(() => undefined);
    void nextOperation.catch((error) => {
      if (error instanceof WorkflowQueueInvalidatedError
        || !workflowQueueRef.current.isCurrent(generation)) return;
      setInitializationError(workflowErrorMessage(
        error,
        t("workflow.persistence.clusterCreateSaveFailed"),
      ));
    });
    return nextOperation;
  }, []);

  useEffect(() => {
    workflowQueueRef.current.activate();
    return () => {
      isDataPersisted.current = false;
      workflowQueueRef.current.deactivate();
    };
  }, []);

  useEffect(() => {
    if (!routeDraftId) {
      const nextSearch = new URLSearchParams(searchParams);
      nextSearch.set("draft", draftId);
      navigate({ pathname: location.pathname, search: nextSearch.toString() }, { replace: true });
      return;
    }
  }, [draftId, location.pathname, navigate, routeDraftId, searchParams]);

  useEffect(() => {
    if (hasInvalidDraftId) {
      isDataPersisted.current = false;
      workflowSessionRef.current = null;
      setDraftRevision(0);
      void workflowQueueRef.current.reset();
      setInitializationError(t("workflow.persistence.invalidDraft"));
      setIsHydrated(false);
      setHydratedDraftId(null);
      return;
    }
    if (routeDraftId) void syncUserPersistedData(routeDraftId);
  }, [hasInvalidDraftId, retryCount, routeDraftId]);

  useEffect(() => {
    if (isDataPersisted.current) {
      if (explicitlyPersistedStateRef.current
        && isEqual(explicitlyPersistedStateRef.current, state)) {
        explicitlyPersistedStateRef.current = null;
        return;
      }
      void queuePersistence(() => flushCurrentData(state, currStepData)).catch(() => undefined);
    }
  }, [state.clusterCreationSteps, currStepData]);

  async function syncUserPersistedData(scopeDraftId: string) {
    const recoveryGeneration = await workflowQueueRef.current.reset();
    if (!workflowQueueRef.current.isCurrent(recoveryGeneration)) return;
    setInitializationError(null);
    setIsHydrated(false);
    setHydratedDraftId(null);
    isDataPersisted.current = false;
    workflowSessionRef.current = null;
    setDraftRevision(0);
    const session = new ScopedWorkflowSession({ type: "drafts", id: scopeDraftId });
    try {
      const persisted = await session.load();
      if (!workflowQueueRef.current.isCurrent(recoveryGeneration)) return;
      if (persisted.workflow !== "IDLE" && persisted.workflow !== "CLUSTER_CREATE") {
        throw new Error(t("workflow.persistence.differentWorkflow"));
      }
      const persistedState = persisted.workflow === "CLUSTER_CREATE"
        ? get(persisted, "values.state", initialState)
        : initialState;
      dispatch({
        type: ActionTypes.SYNC_STATE,
        payload: persistedState?.clusterCreationSteps ? persistedState : initialState,
      });
      const clusterState = get(persisted, "values.step", {});
      const classicStep = resolveRecoveryStep(
        "clusterCreation",
        get(clusterState, "clusterState"),
      );
      const activeStepName = get(clusterState, "stepName", "");
      const storedStep = Object.keys(stepWizardUtilities.wizardSteps).find(
        (stepNumber) =>
          stepWizardUtilities.wizardSteps?.[stepNumber]?.name === activeStepName,
      );
      const activeStep = classicStep ?? (storedStep === undefined ? 0 : Number(storedStep));
      const restoredStepData = clusterState && (classicStep !== undefined || activeStepName)
        ? clusterState
        : {};
      currStepDataRef.current = restoredStepData;
      setCurrStepData(restoredStepData);
      stepWizardUtilities.jumpToStep(activeStep, true);
      if (persisted.workflow === "IDLE") {
        await session.save("CLUSTER_CREATE", "START", {
          ...projectClusterCreationValues({ state: stateRef.current, step: {} }),
        });
        if (!workflowQueueRef.current.isCurrent(recoveryGeneration)) return;
      }
      workflowSessionRef.current = session;
      setDraftRevision(session.currentRevision);
      isDataPersisted.current = true;
      setHydratedDraftId(scopeDraftId);
      setIsHydrated(true);
    } catch (error: any) {
      if (!workflowQueueRef.current.isCurrent(recoveryGeneration)) return;
      setInitializationError(
        workflowErrorMessage(error, t("workflow.persistence.clusterCreateLoadFailed")),
      );
    }
  }

  async function flushCurrentData(
    stateSnapshot: State = stateRef.current,
    stepSnapshot: Record<string, any> = currStepDataRef.current,
  ) {
    const session = workflowSessionRef.current;
    if (!session) throw new Error(t("workflow.persistence.clusterCreateNotLoaded"));
    await session.save(
      "CLUSTER_CREATE",
      get(stepSnapshot, "clusterState") || get(stepSnapshot, "stepName") || "START",
      projectClusterCreationValues({ state: stateSnapshot, step: stepSnapshot }),
    );
    if (workflowSessionRef.current === session) {
      setDraftRevision(session.currentRevision);
    }
  }

  async function storeStepDataAndFlush(
    step: string,
    data: Record<string, unknown>,
  ) {
    const action: Action = {
      type: ActionTypes.STORE_INFORMATION,
      payload: { step, data },
    };
    const nextState = reducer(stateRef.current, action);
    stateRef.current = nextState;
    explicitlyPersistedStateRef.current = nextState;
    reducerDispatch(action);
    await queuePersistence(() => flushCurrentData(nextState, currStepDataRef.current));
    return workflowSessionRef.current?.currentRevision || 0;
  }

  async function withStateCheckpoint<T>(
    request: (revision: number) => Promise<T>,
  ): Promise<T> {
    const stateSnapshot = stateRef.current;
    const stepSnapshot = currStepDataRef.current;
    explicitlyPersistedStateRef.current = stateSnapshot;
    const generation = workflowQueueRef.current.currentGeneration;
    const queued = workflowQueueRef.current.enqueue(async () => {
      await flushCurrentData(stateSnapshot, stepSnapshot);
      const revision = workflowSessionRef.current?.currentRevision || 0;
      try {
        return { ok: true as const, value: await request(revision) };
      } catch (error) {
        return { ok: false as const, error };
      }
    });
    void queued.catch((error) => {
      if (error instanceof WorkflowQueueInvalidatedError
        || !workflowQueueRef.current.isCurrent(generation)) return;
      setInitializationError(workflowErrorMessage(
        error,
        t("workflow.persistence.clusterCreateSaveFailed"),
      ));
    });
    const outcome = await queued;
    if (!outcome.ok) throw outcome.error;
    return outcome.value;
  }

  async function flushOnCancel() {
    await queuePersistence(() => flushCurrentData());
    await queuePersistence(async () => {
      if (!workflowSessionRef.current) return;
      await workflowSessionRef.current.release();
    });
    redirectToAdminView();
  }

  async function flushOnComplete() {
    await queuePersistence(async () => {
      if (!workflowSessionRef.current) return;
      await workflowSessionRef.current.release();
    });
  }

  async function flushOnStepChange(nextStep: number, clusterState?: string) {
    if (nextStep >= 0) {
      const nextStepDetails = stepWizardUtilities.wizardSteps?.[nextStep];
      const nextClusterCreationSteps = {
        ...stateRef.current.clusterCreationSteps,
      };
      if (nextStepDetails?.keysToRemove) {
        nextStepDetails.keysToRemove.forEach((key: string) => {
          delete nextClusterCreationSteps[key];
        });
      }
      const nextState = {
        ...stateRef.current,
        clusterCreationSteps: nextClusterCreationSteps,
      };
      dispatch({ type: ActionTypes.SYNC_STATE, payload: nextState });
      const nextStepData = {
        progressStatus: ClusterProgressStatus.PROVISIONING,
        stepName: stepWizardUtilities?.wizardSteps?.[nextStep]?.name,
        ...(clusterState ? { clusterState } : {}),
      };
      currStepDataRef.current = nextStepData;
      setCurrStepData(nextStepData);
      await queuePersistence(() => flushCurrentData(nextState, nextStepData));
    }
  }

  async function flushStateToDb(
    operation: string = "default",
    jumpStep: number = -1,
    clusterState?: string,
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
        await flushOnCancel();
        break;
      case "complete":
        await flushOnComplete();
        break;
      case "back":
        await flushOnStepChange(Number(activeStep) - 1, clusterState);
        break;
      case "next":
        await flushOnStepChange(Number(activeStep) + 1, clusterState);
        break;
      case "jump":
        await flushOnStepChange(jumpStep, clusterState);
        break;
      case "checkpoint": {
        const nextStepData = {
          ...currStepDataRef.current,
          progressStatus: ClusterProgressStatus.PROVISIONING,
          stepName: stepWizardUtilities.currentStep.name,
          clusterState,
        };
        currStepDataRef.current = nextStepData;
        setCurrStepData(nextStepData);
        await queuePersistence(() => flushCurrentData(stateRef.current, nextStepData));
        break;
      }
      default:
        await queuePersistence(() => flushCurrentData());
    }
  }

  return (
    <ClusterCreationContext.Provider
      value={{
        state,
        dispatch,
        stepWizardUtilities,
        flushStateToDb,
        storeStepDataAndFlush,
        withStateCheckpoint,
        draftId: routeDraftId || undefined,
        draftRevision,
        getDraftRevision: () => workflowSessionRef.current?.currentRevision || 0,
      }}
    >
      {initializationError ? (
        <Alert variant="danger" className="m-4">
          {initializationError}{" "}
          <Button
            size="sm"
            variant="outline-danger"
            onClick={() => {
              if (hasInvalidDraftId) {
                navigate(clusterDraftPath(), { replace: true });
              } else {
                setRetryCount((value) => value + 1);
              }
            }}
          >
            {hasInvalidDraftId
              ? t("workflow.persistence.startNew")
              : t("common.retry")}
          </Button>
        </Alert>
      ) : !isHydrated || hydratedDraftId !== routeDraftId ? (
        <Spinner />
      ) : (
        <>
          {reentrySteps.length ? (
            <Alert variant="warning" className="m-4 mb-0">
              <div>{t("workflow.persistence.clusterCreateReentry")}</div>
              <div className="d-flex flex-wrap gap-2 mt-2">
                {reentrySteps.map((reentryStep) => (
                  <Button
                    key={reentryStep.step}
                    size="sm"
                    variant="outline-warning"
                    onClick={() => stepWizardUtilities.jumpToStep(reentryStep.step)}
                  >
                    {t("workflow.persistence.reviewReentry", {
                      step: t(reentryStep.labelKey),
                    })}
                  </Button>
                ))}
              </div>
            </Alert>
          ) : null}
          {children}
        </>
      )}
    </ClusterCreationContext.Provider>
  );
};
