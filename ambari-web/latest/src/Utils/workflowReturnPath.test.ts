/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
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
  consumeWorkflowReturnPath,
  saveWorkflowReturnPath,
  type WorkflowReturnScope,
} from "./workflowReturnPath";

const memoryStorage = () => {
  const values = new Map<string, string>();
  return {
    getItem: (key: string) => values.get(key) ?? null,
    removeItem: (key: string) => values.delete(key),
    setItem: (key: string, value: string) => values.set(key, value),
  };
};

const scope = (
  principal: string,
  clusterId: number,
  workflow: WorkflowReturnScope["workflow"] = "ADD_SERVICE",
): WorkflowReturnScope => ({ clusterId, principal, workflow });

describe("scoped workflow return paths", () => {
  it("isolates destinations by tab, principal, cluster and workflow", () => {
    const tabA = memoryStorage();
    const tabB = memoryStorage();
    saveWorkflowReturnPath(scope("alice", 11), "/main/admin/stack/services", tabA);
    saveWorkflowReturnPath(scope("alice", 11), "/main/services", tabB);
    saveWorkflowReturnPath(
      scope("alice", 11, "ENABLING_KERBEROS"),
      "/main/admin/stack/services?source=kerberos",
      tabA,
    );

    expect(consumeWorkflowReturnPath(
      scope("alice", 11),
      "/main/services",
      tabB,
    )).toBe("/main/services");
    expect(consumeWorkflowReturnPath(
      scope("alice", 11),
      "/main/services",
      tabA,
    )).toBe("/main/admin/stack/services");
    expect(consumeWorkflowReturnPath(
      scope("alice", 11, "ENABLING_KERBEROS"),
      "/main/admin/kerberos",
      tabA,
    )).toBe("/main/admin/stack/services?source=kerberos");

    saveWorkflowReturnPath(scope("alice", 12), "/main/hosts", tabA);
    expect(consumeWorkflowReturnPath(
      scope("bob", 12),
      "/main/services",
      tabA,
    )).toBe("/main/services");
    expect(consumeWorkflowReturnPath(
      scope("alice", 12),
      "/main/services",
      tabA,
    )).toBe("/main/hosts");
  });

  it("rejects unsafe or unverified destinations and consumes a valid path once", () => {
    const storage = memoryStorage();
    expect(saveWorkflowReturnPath(
      scope("alice", 11),
      "/main/%2e%2e/adminView",
      storage,
    )).toBe(false);
    expect(saveWorkflowReturnPath(
      scope("", 11),
      "/main/services",
      storage,
    )).toBe(false);
    expect(saveWorkflowReturnPath(
      scope("alice", 0),
      "/main/services",
      storage,
    )).toBe(false);
    expect(consumeWorkflowReturnPath(
      scope("alice", 11),
      "/main/services",
      storage,
    )).toBe("/main/services");

    expect(saveWorkflowReturnPath(
      scope("alice", 11),
      "/main/admin/stack/services?tab=versions",
      storage,
    )).toBe(true);
    expect(consumeWorkflowReturnPath(
      scope("alice", 11),
      "/main/services",
      storage,
    )).toBe("/main/admin/stack/services?tab=versions");
    expect(consumeWorkflowReturnPath(
      scope("alice", 11),
      "/main/services",
      storage,
    )).toBe("/main/services");
  });
});
