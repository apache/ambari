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
import { runDeploymentPlan } from "./deploymentQueue";

describe("runDeploymentPlan", () => {
  it("runs dependent stages in order and parallelizes only within a stage", async () => {
    const events: string[] = [];
    let releaseFirstBatch!: () => void;
    const firstBatch = new Promise<void>((resolve) => {
      releaseFirstBatch = resolve;
    });
    const run = (name: string, wait?: Promise<void>) => async () => {
      events.push(`${name}:start`);
      await wait;
      events.push(`${name}:end`);
    };
    const plan = runDeploymentPlan([
      {
        parallel: true,
        operations: [
          { id: "delete-a", label: "delete a", run: run("a", firstBatch) },
          { id: "delete-b", label: "delete b", run: run("b", firstBatch) },
        ],
      },
      {
        operations: [{ id: "create", label: "create", run: run("create") }],
      },
    ]);

    await Promise.resolve();
    expect(events).toEqual(["a:start", "b:start"]);
    releaseFirstBatch();
    await plan;
    expect(events.slice(-2)).toEqual(["create:start", "create:end"]);
  });

  it("aborts later stages after the first failed prerequisite", async () => {
    const later = vi.fn();
    await expect(runDeploymentPlan([
      {
        operations: [{
          id: "failed",
          label: "failed",
          run: async () => {
            throw new Error("failed prerequisite");
          },
        }],
      },
      { operations: [{ id: "later", label: "later", run: later }] },
    ])).rejects.toThrow("failed prerequisite");
    expect(later).not.toHaveBeenCalled();
  });

  it("skips operations already persisted as complete", async () => {
    const skipped = vi.fn();
    const pending = vi.fn().mockResolvedValue(undefined);
    const completed = await runDeploymentPlan([{
      operations: [
        { id: "skipped", label: "skipped", run: skipped },
        { id: "pending", label: "pending", run: pending },
      ],
    }], new Set(["skipped"]));

    expect(skipped).not.toHaveBeenCalled();
    expect(pending).toHaveBeenCalledOnce();
    expect([...completed]).toEqual(["skipped", "pending"]);
  });
});
