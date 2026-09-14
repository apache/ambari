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
import Spinner from "../../../../components/Spinner";
import useAuth from "../../../../hooks/useAuth";
import useClusterNavigate from "../../../../hooks/useClusterNavigate";
import useClusterWorkflowPersistence from "../../../../hooks/useClusterWorkflowPersistence";
import modalManager from "../../../../store/ModalManager";
import { containsReentryMarker, workflowErrorMessage } from "../../../../Utils/scopedWorkflow";
import { translate } from "../../../../Utils/Utility";
import {
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
  summaryPath: string;
  stepWizardUtilities: any;
  children: React.ReactNode;
}

export function PersistedWorkflowProvider({
  storageKey,
  controllerName,
  progressStatus,
  progressStepIndex,
  summaryPath,
  stepWizardUtilities,
  children,
}: PersistedWorkflowProviderProps) {
  const { hasAuthorization } = useAuth();
  const navigate = useClusterNavigate();
  const canPersist = hasAuthorization("CLUSTER.MANAGE_USER_PERSISTED_DATA");
  const persistence = useClusterWorkflowPersistence(storageKey, {
    controllerNames: [controllerName],
    keys: [storageKey, "CLUSTER_STATE"],
  });
  const [state, setState] = useState(emptyWorkflowState);
  const [isHydrated, setIsHydrated] = useState(false);
  const [initializationError, setInitializationError] = useState("");
  const [reentryRequired, setReentryRequired] = useState(false);
  const [retryCount, setRetryCount] = useState(0);
  const stateRef = useRef(emptyWorkflowState());
  const activeStepRef = useRef("");
  const hydrationGeneration = useRef(0);
  const stepWizardUtilitiesRef = useRef(stepWizardUtilities);
  stepWizardUtilitiesRef.current = stepWizardUtilities;

  const storeStep = (stepName: string, data: Record<string, unknown>) => {
    stateRef.current = storeWorkflowStep(stateRef.current, stepName, data);
    setState(stateRef.current);
  };

  useEffect(() => {
    const generation = ++hydrationGeneration.current;
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
    const load = retryCount > 0
      ? persistence.reload()
      : persistence.getPersistData();
    void load.then((values) => {
      if (hydrationGeneration.current !== generation) return;
      const restored = (values?.[storageKey] || emptyWorkflowState()) as PersistedWorkflowState;
      const needsReentry = containsReentryMarker(restored);
      setReentryRequired(needsReentry);
      stateRef.current = { ...restored, steps: restored.steps || {} };
      setState(stateRef.current);
      const currentWizardUtilities = stepWizardUtilitiesRef.current;
      activeStepRef.current = needsReentry
        ? currentWizardUtilities.wizardSteps[1]?.name || ""
        : restored.activeStep || "";
      if (activeStepRef.current) {
        const restoredStep = Object.keys(currentWizardUtilities.wizardSteps).find(
          (stepNumber) =>
            currentWizardUtilities.wizardSteps[stepNumber]?.name === activeStepRef.current,
        );
        if (restoredStep !== undefined) {
          currentWizardUtilities.jumpToStep(Number(restoredStep), true);
        }
      } else {
        currentWizardUtilities.jumpToStep(0, true);
      }
      setIsHydrated(true);
    }, (error) => {
      if (hydrationGeneration.current !== generation) return;
      setInitializationError(workflowErrorMessage(
        error,
        translate("workflow.persistence.restoreFailed"),
      ));
    });
    return () => {
      if (hydrationGeneration.current === generation) {
        hydrationGeneration.current += 1;
      }
    };
  }, [canPersist, persistence, retryCount, storageKey]);

  useEffect(() => {
    if (!isHydrated) return;
    const warnBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault();
    };
    window.addEventListener("beforeunload", warnBeforeUnload);
    return () => window.removeEventListener("beforeunload", warnBeforeUnload);
  }, [isHydrated]);

  async function writeState(activeStep = activeStepRef.current) {
    if (!persistence) throw new Error(translate("workflow.persistence.explicitCluster"));
    await persistence.savePersistData({
      [storageKey]: { ...stateRef.current, activeStep },
      CLUSTER_STATE: { progressStatus, stepName: activeStep },
    }, activeStep || progressStatus);
    setReentryRequired(false);
  }

  async function clearState() {
    if (!persistence) throw new Error(translate("workflow.persistence.explicitCluster"));
    await persistence.release();
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
    await writeState(activeStepRef.current);
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
          await writeState();
        } else {
          await clearState();
        }
        modalManager.hide();
        navigate(summaryPath);
        break;
      default:
        await writeState();
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
          {translate("common.retry")}
        </Button>
      </Alert>
    );
  }
  if (!isHydrated) return <Spinner />;

  return (
    <PersistedWorkflowContext.Provider
      value={{ state, stepWizardUtilities, storeStep, persist }}
    >
      {reentryRequired && (
        <Alert variant="warning">
          {translate("workflow.persistence.reentryRequired")}
        </Alert>
      )}
      {children}
    </PersistedWorkflowContext.Provider>
  );
}
