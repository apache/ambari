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

import { describe, expect, it } from "vitest";
import {
  buildWorkflowClearPayload,
  buildWorkflowPersistencePayload,
  emptyWorkflowState,
  removeWorkflowSteps,
  storeWorkflowStep,
} from "./workflowPersistence";

describe("Federation workflow persistence", () => {
  it("stores immutable step snapshots and removes invalidated later steps", () => {
    const initial = emptyWorkflowState();
    const withHosts = storeWorkflowStep(initial, "SELECT_HOSTS", {
      hosts: ["h1"],
    });
    const withProgress = storeWorkflowStep(withHosts, "PROGRESS", {
      operations: [{ id: "install" }],
    });
    expect(initial.steps).toEqual({});
    expect(removeWorkflowSteps(withProgress, ["PROGRESS"]).steps).toEqual({
      SELECT_HOSTS: { hosts: ["h1"] },
    });
  });

  it("serializes workflow, cluster checkpoint, and owner exactly once", () => {
    const payload = buildWorkflowPersistencePayload({
      storageKey: "ROUTER_FEDERATION",
      state: { steps: { REVIEW: { saved: true } } },
      activeStep: "PROGRESS",
      progressStatus: "ENABLING_ROUTER_FEDERATION",
      owner: "alice",
      controllerName: "routerFederationWizardController",
    });
    expect(JSON.parse(payload.ROUTER_FEDERATION)).toEqual({
      steps: { REVIEW: { saved: true } },
      activeStep: "PROGRESS",
    });
    expect(JSON.parse(payload.CLUSTER_STATE)).toEqual({
      progressStatus: "ENABLING_ROUTER_FEDERATION",
      stepName: "PROGRESS",
    });
    expect(JSON.parse(payload["wizard-data"])).toEqual({
      userName: "alice",
      controllerName: "routerFederationWizardController",
    });
  });

  it("clears workflow ownership on completion", () => {
    const payload = buildWorkflowClearPayload("ADD_HAWQ_STANDBY");
    expect(JSON.parse(payload.ADD_HAWQ_STANDBY)).toEqual({ steps: {} });
    expect(JSON.parse(payload["wizard-data"])).toEqual({});
  });
});
