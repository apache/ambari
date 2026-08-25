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
  useEffect,
  useRef,
  useState,
} from "react";
import { Alert, Button } from "react-bootstrap";
import ClusterApi from "../../../../api/clusterApi";
import Spinner from "../../../../components/Spinner";
import useAuth from "../../../../hooks/useAuth";
import modalManager from "../../../../store/ModalManager";
import { parsePersistedValue } from "../../../../Utils/persistedSettings";
import {
  buildWorkflowClearPayload,
  buildWorkflowPersistencePayload,
  emptyWorkflowState,
  PersistedWorkflowState,
  removeWorkflowSteps,
  storeWorkflowStep,
} from "./workflowPersistence";

type PersistenceOperation =
  | "default"
  | "next"
  | "back"
  | "jump"
  | "cancel"
  | "complete";

interface PersistedWorkflowContextValue {
  state: PersistedWorkflowState;
  stepWizardUtilities?: any;
  storeStep: (stepName: string, data: Record<string, unknown>) => void;
  persist: (operation?: PersistenceOperation, jumpStep?: number) => Promise<void>;
}

export const PersistedWorkflowContext =
  createContext<PersistedWorkflowContextValue>({
    state: emptyWorkflowState(),
    storeStep: () => undefined,
    persist: async () => undefined,
  });

interface PersistedWorkflowProviderProps {
  storageKey: string;
  controllerName: string;
  progressStatus: string;
  progressStepIndex: number;
  summaryUrl: string;
  stepWizardUtilities: any;
  children: React.ReactNode;
}

export function PersistedWorkflowProvider({
  storageKey,
  controllerName,
  progressStatus,
  progressStepIndex,
  summaryUrl,
  stepWizardUtilities,
  children,
}: PersistedWorkflowProviderProps) {
  const { user, hasAuthorization } = useAuth();
  const owner = user?.user_name || "";
  const canPersist = hasAuthorization("CLUSTER.MANAGE_USER_PERSISTED_DATA");
  const [state, setState] = useState(emptyWorkflowState);
  const [isHydrated, setIsHydrated] = useState(false);
  const [initializationError, setInitializationError] = useState("");
  const [retryCount, setRetryCount] = useState(0);
  const stateRef = useRef(emptyWorkflowState());
  const activeStepRef = useRef("");
  const queueRef = useRef<Promise<void>>(Promise.resolve());

  const queue = (operation: () => Promise<void>) => {
    const queued = queueRef.current.catch(() => undefined).then(operation);
    queueRef.current = queued.catch(() => undefined);
    return queued;
  };

  const storeStep = (stepName: string, data: Record<string, unknown>) => {
    stateRef.current = storeWorkflowStep(stateRef.current, stepName, data);
    setState(stateRef.current);
  };

  useEffect(() => {
    void hydrate();
  }, [retryCount]);

  useEffect(() => {
    if (!isHydrated) return;
    const warnBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault();
    };
    window.addEventListener("beforeunload", warnBeforeUnload);
    return () => window.removeEventListener("beforeunload", warnBeforeUnload);
  }, [isHydrated]);

  async function optionalPersistedValue(key: string) {
    try {
      return await ClusterApi.getPersistData(key);
    } catch (error: any) {
      if (error?.response?.status === 404 || error?.status === 404) return null;
      throw error;
    }
  }

  async function hydrate() {
    setIsHydrated(false);
    setInitializationError("");
    if (!canPersist) {
      setInitializationError(
        "This workflow requires permission to persist recovery checkpoints.",
      );
      return;
    }
    try {
      const [stateResponse, ownerResponse] = await Promise.all([
        optionalPersistedValue(storageKey),
        optionalPersistedValue("wizard-data"),
      ]);
      const restored = parsePersistedValue<PersistedWorkflowState>(
        stateResponse,
        emptyWorkflowState(),
      );
      const restoredOwner = parsePersistedValue<Record<string, string>>(
        ownerResponse,
        {},
      );
      if (
        Object.keys(restored.steps || {}).length &&
        restoredOwner.userName &&
        owner &&
        restoredOwner.userName !== owner
      ) {
        throw new Error(
          `This workflow is owned by ${restoredOwner.userName}. Ask that user to finish or clear it.`,
        );
      }
      stateRef.current = { ...restored, steps: restored.steps || {} };
      setState(stateRef.current);
      activeStepRef.current = restored.activeStep || "";
      if (restored.activeStep) {
        const restoredStep = Object.keys(stepWizardUtilities.wizardSteps).find(
          (stepNumber) =>
            stepWizardUtilities.wizardSteps[stepNumber]?.name ===
            restored.activeStep,
        );
        if (restoredStep !== undefined) {
          stepWizardUtilities.jumpToStep(Number(restoredStep), true);
        }
      } else {
        stepWizardUtilities.jumpToStep(0, true);
      }
      setIsHydrated(true);
    } catch (error: any) {
      setInitializationError(
        error?.response?.data?.message ||
          error?.message ||
          "Ambari could not restore this workflow.",
      );
    }
  }

  async function writeState(activeStep = activeStepRef.current) {
    await ClusterApi.postPersistData(
      buildWorkflowPersistencePayload({
        storageKey,
        state: stateRef.current,
        activeStep,
        progressStatus,
        owner,
        controllerName,
      }),
    );
  }

  async function clearState() {
    await queue(() =>
      ClusterApi.postPersistData(buildWorkflowClearPayload(storageKey)),
    );
    stateRef.current = emptyWorkflowState();
    setState(stateRef.current);
    activeStepRef.current = "";
  }

  async function moveToStep(stepNumber: number | undefined) {
    if (stepNumber === undefined || stepNumber < 0) return;
    const step = stepWizardUtilities.wizardSteps[stepNumber];
    if (step?.keysToRemove?.length) {
      stateRef.current = removeWorkflowSteps(
        stateRef.current,
        step.keysToRemove,
      );
      setState(stateRef.current);
    }
    activeStepRef.current = step?.name || "";
    await queue(() => writeState(activeStepRef.current));
  }

  async function persist(
    operation: PersistenceOperation = "default",
    jumpStep = -1,
  ) {
    switch (operation) {
      case "next":
        await moveToStep(stepWizardUtilities.nextStepNumber);
        break;
      case "back":
        await moveToStep(stepWizardUtilities.prevStepNumber);
        break;
      case "jump":
        await moveToStep(jumpStep);
        break;
      case "complete":
        await clearState();
        break;
      case "cancel":
        if (stepWizardUtilities.activeStep >= progressStepIndex) {
          await queue(() => writeState());
        } else {
          await clearState();
        }
        modalManager.hide();
        window.location.href = summaryUrl;
        break;
      default:
        await queue(() => writeState());
    }
  }

  if (initializationError) {
    return (
      <Alert variant="danger">
        {initializationError}
        <Button
          size="sm"
          className="ms-3"
          disabled={!canPersist}
          onClick={() => setRetryCount((value) => value + 1)}
        >
          Retry
        </Button>
      </Alert>
    );
  }
  if (!isHydrated) return <Spinner />;

  return (
    <PersistedWorkflowContext.Provider
      value={{ state, stepWizardUtilities, storeStep, persist }}
    >
      {children}
    </PersistedWorkflowContext.Provider>
  );
}
