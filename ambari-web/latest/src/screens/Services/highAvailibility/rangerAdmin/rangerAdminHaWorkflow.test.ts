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

import { describe, expect, it, vi } from "vitest";
import { createRangerAdminHaOperations } from "./rangerAdminHaWorkflow";

describe("Ranger Admin HA progress workflow", () => {
  it("uses stable ordered non-skippable operations", () => {
    const callback = vi.fn().mockResolvedValue({ status: 200 });
    const operations = createRangerAdminHaOperations({
      stopAllServices: callback,
      installAdditionalRangerAdmins: callback,
      reconfigureServices: callback,
      startAllServices: callback,
    });

    expect(operations.map(({ id, label, skippable }) => ({ id, label, skippable }))).toEqual([
      { id: "stopAllServices", label: "Stop All Services", skippable: false },
      { id: "installRangerAdmins", label: "Install Additional Ranger Admin", skippable: false },
      { id: "reconfigureServices", label: "Reconfigure Services", skippable: false },
      { id: "startAllServices", label: "Start All Services", skippable: false },
    ]);
  });

  it("does not mask callback failures", async () => {
    const failure = vi.fn().mockRejectedValue(new Error("save failed"));
    const operations = createRangerAdminHaOperations({
      stopAllServices: vi.fn().mockResolvedValue({ status: 200 }),
      installAdditionalRangerAdmins: vi.fn().mockResolvedValue({ status: 200 }),
      reconfigureServices: failure,
      startAllServices: vi.fn().mockResolvedValue({ status: 200 }),
    });

    await expect(operations[2].callback()).rejects.toThrow("save failed");
  });
});
