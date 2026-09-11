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
  consumeAddServiceSelectionIntent,
  saveAddServiceSelectionIntent,
} from "./workflowSelectionIntent";

const memoryStorage = () => {
  const values = new Map<string, string>();
  return {
    getItem: (key: string) => values.get(key) ?? null,
    removeItem: (key: string) => { values.delete(key); },
    setItem: (key: string, value: string) => { values.set(key, value); },
  };
};

describe("Add Service selection intent", () => {
  it("isolates one-time service choices across tabs, principals and clusters", () => {
    const tabA = memoryStorage();
    const tabB = memoryStorage();
    saveAddServiceSelectionIntent({ principal: "alice", clusterId: 11 }, "HBASE", tabA);
    saveAddServiceSelectionIntent({ principal: "alice", clusterId: 11 }, "HIVE", tabB);
    saveAddServiceSelectionIntent({ principal: "alice", clusterId: 12 }, "KAFKA", tabA);

    expect(consumeAddServiceSelectionIntent(
      { principal: "bob", clusterId: 11 },
      tabA,
    )).toBeNull();
    expect(consumeAddServiceSelectionIntent(
      { principal: "alice", clusterId: 11 },
      tabB,
    )).toBe("HIVE");
    expect(consumeAddServiceSelectionIntent(
      { principal: "alice", clusterId: 11 },
      tabA,
    )).toBe("HBASE");
    expect(consumeAddServiceSelectionIntent(
      { principal: "alice", clusterId: 12 },
      tabA,
    )).toBe("KAFKA");
  });

  it("rejects unverified scopes and invalid service names and consumes once", () => {
    const storage = memoryStorage();
    expect(saveAddServiceSelectionIntent(
      { principal: "", clusterId: 11 },
      "HBASE",
      storage,
    )).toBe(false);
    expect(saveAddServiceSelectionIntent(
      { principal: "alice", clusterId: 0 },
      "HBASE",
      storage,
    )).toBe(false);
    expect(saveAddServiceSelectionIntent(
      { principal: "alice", clusterId: 11 },
      "../HBASE",
      storage,
    )).toBe(false);
    expect(saveAddServiceSelectionIntent(
      { principal: "alice", clusterId: 11 },
      "HBASE",
      storage,
    )).toBe(true);
    expect(consumeAddServiceSelectionIntent(
      { principal: "alice", clusterId: 11 },
      storage,
    )).toBe("HBASE");
    expect(consumeAddServiceSelectionIntent(
      { principal: "alice", clusterId: 11 },
      storage,
    )).toBeNull();
  });
});
