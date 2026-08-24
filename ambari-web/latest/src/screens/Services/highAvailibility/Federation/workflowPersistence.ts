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

import { persistedPayload } from "../../../../Utils/persistedSettings";

export interface PersistedWorkflowState {
  steps: Record<string, Record<string, unknown>>;
  activeStep?: string;
}

export const emptyWorkflowState = (): PersistedWorkflowState => ({
  steps: {},
});

export function storeWorkflowStep(
  state: PersistedWorkflowState,
  stepName: string,
  data: Record<string, unknown>,
): PersistedWorkflowState {
  return {
    ...state,
    steps: {
      ...state.steps,
      [stepName]: {
        ...(state.steps[stepName] || {}),
        ...data,
      },
    },
  };
}

export function removeWorkflowSteps(
  state: PersistedWorkflowState,
  stepNames: string[],
): PersistedWorkflowState {
  const steps = { ...state.steps };
  stepNames.forEach((stepName) => delete steps[stepName]);
  return { ...state, steps };
}

export function buildWorkflowPersistencePayload(input: {
  storageKey: string;
  state: PersistedWorkflowState;
  activeStep: string;
  progressStatus: string;
  owner: string;
  controllerName: string;
}) {
  return persistedPayload({
    [input.storageKey]: { ...input.state, activeStep: input.activeStep },
    CLUSTER_STATE: {
      progressStatus: input.progressStatus,
      stepName: input.activeStep,
    },
    "wizard-data": {
      userName: input.owner,
      controllerName: input.controllerName,
    },
  });
}

export function buildWorkflowClearPayload(storageKey: string) {
  return persistedPayload({
    [storageKey]: emptyWorkflowState(),
    CLUSTER_STATE: {},
    "wizard-data": {},
  });
}
