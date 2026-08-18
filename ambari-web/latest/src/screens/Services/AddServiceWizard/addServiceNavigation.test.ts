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
  deriveAddServiceFlow,
  nextAddServiceStep,
  previousAddServiceStep,
} from "./addServiceNavigation";

describe("Add Service conditional navigation", () => {
  it("skips master-only gaps and returns to the last applicable step", () => {
    const flow = deriveAddServiceFlow({
      CLIENT_ONLY: {
        hasClient: true,
        hasConfigs: false,
        selected: true,
      },
    });

    expect(flow).toEqual({
      skipConfigStep: true,
      skipMasterStep: true,
      skipSlavesStep: false,
    });
    expect(nextAddServiceStep(1, flow)).toBe(3);
    expect(nextAddServiceStep(3, flow)).toBe(5);
    expect(previousAddServiceStep(5, flow)).toBe(3);
  });

  it("goes directly from Services to Review when no assignment or config step applies", () => {
    const flow = deriveAddServiceFlow({
      HEADLESS: { hasConfigs: false, selected: true },
    });

    expect(nextAddServiceStep(1, flow)).toBe(5);
    expect(previousAddServiceStep(5, flow)).toBe(1);
  });
});
