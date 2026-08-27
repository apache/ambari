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
import { commandDetail, isFailed, isFinished } from "./Utility";

describe("operation terminal states", () => {
  it.each(["FAILED", "TIMEDOUT", "ABORTED"])(
    "treats %s as a retryable terminal failure",
    (status) => {
      expect(isFinished(status)).toBe(true);
      expect(isFailed(status)).toBe(true);
    },
  );

  it("treats COMPLETED as terminal but successful", () => {
    expect(isFinished("COMPLETED")).toBe(true);
    expect(isFailed("COMPLETED")).toBe(false);
  });
});

describe("background operation command details", () => {
  it("formats a complete ZooKeeper command detail", () => {
    expect(commandDetail(
      "RESTART ZOOKEEPER/ZOOKEEPER_SERVER",
      null,
      null,
    )).toBe(" Restart ZooKeeper Server");
  });

  it("uses the operation display name without requiring a command detail", () => {
    expect(commandDetail(undefined, null, "Restart ZooKeeper")).toBe(
      " Restart ZooKeeper",
    );
  });

  it.each([null, undefined])(
    "returns an empty label when command detail is %s",
    (detail) => {
      expect(commandDetail(detail, null, null)).toBe("");
    },
  );
});
