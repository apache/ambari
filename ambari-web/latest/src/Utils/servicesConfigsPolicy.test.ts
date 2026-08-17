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

import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({ postPersistData: vi.fn() }));
vi.mock("../api/clusterApi", () => ({
  default: { postPersistData: mocks.postPersistData },
}));

import {
  buildClearedAddServiceState,
  clearAddServiceWizardState,
} from "./addServicePersistence";
import { canManageServices } from "./servicePermissions";

describe("services and configs policy", () => {
  beforeEach(() => {
    mocks.postPersistData.mockReset();
    mocks.postPersistData.mockResolvedValue({});
  });

  it("requires authorization, the feature flag, and no conflicting wizard", () => {
    expect(
      canManageServices({
        authorized: true,
        featureEnabled: true,
        wizardIsNotFinished: false,
      })
    ).toBe(true);
    expect(
      canManageServices({
        authorized: false,
        featureEnabled: true,
        wizardIsNotFinished: false,
      })
    ).toBe(false);
    expect(
      canManageServices({
        authorized: true,
        featureEnabled: false,
        wizardIsNotFinished: false,
      })
    ).toBe(false);
    expect(
      canManageServices({
        authorized: true,
        featureEnabled: true,
        wizardIsNotFinished: true,
      })
    ).toBe(false);
  });

  it("clears only Add Service and cluster progress persistence", async () => {
    const payload = buildClearedAddServiceState({ addServiceSteps: {} }, 7);
    expect(JSON.parse(payload)).toEqual({
      ADD_SERVICE: JSON.stringify({ addServiceSteps: {}, requestSequence: 7 }),
      CLUSTER_STATE: JSON.stringify({}),
    });

    await clearAddServiceWizardState({ addServiceSteps: {} }, 7);
    expect(mocks.postPersistData).toHaveBeenCalledWith(payload);
  });
});
