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
  getAdjacentVisibleStep,
  getVisibleStepNumbers,
} from "./useStepWizard";

const step = (hidden = false) => ({
  label: "Step",
  completed: false,
  Component: null,
  canGoBack: true,
  isNextEnabled: true,
  hidden,
});

describe("step wizard navigation", () => {
  it("skips hidden steps while retaining stable physical step IDs", () => {
    const steps = {
      0: step(),
      1: step(),
      2: step(true),
      3: step(),
      4: step(true),
      5: step(),
    };

    expect(getVisibleStepNumbers(steps)).toEqual([0, 1, 3, 5]);
    expect(getAdjacentVisibleStep(steps, 1, 1)).toBe(3);
    expect(getAdjacentVisibleStep(steps, 5, -1)).toBe(3);
    expect(getAdjacentVisibleStep(steps, 2, 1)).toBe(3);
  });
});
