/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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
import { restoreVersionOperationCheckpoint } from "./ListVersion";

describe("upgrade operation checkpoint recovery", () => {
  it("replaces a redacted derived checkpoint before returning live operations", async () => {
    const persistence = {
      getPersistData: vi.fn().mockResolvedValue({
        versionOperations: [{ id: 17, requires_reentry: true }],
      }),
      replacePersistData: vi.fn().mockResolvedValue(undefined),
    };

    await expect(restoreVersionOperationCheckpoint(persistence as any)).resolves.toEqual({
      operations: [],
      regenerated: true,
    });
    expect(persistence.replacePersistData).toHaveBeenCalledWith(
      { versionOperations: [] },
      "RECOVERY_RECONCILED",
    );
  });

  it("restores non-sensitive operation rows without rewriting the checkpoint", async () => {
    const operations = [{ id: 21, status: "IN_PROGRESS" }];
    const persistence = {
      getPersistData: vi.fn().mockResolvedValue({ versionOperations: operations }),
      replacePersistData: vi.fn(),
    };

    await expect(restoreVersionOperationCheckpoint(persistence as any)).resolves.toEqual({
      operations,
      regenerated: false,
    });
    expect(persistence.replacePersistData).not.toHaveBeenCalled();
  });
});
