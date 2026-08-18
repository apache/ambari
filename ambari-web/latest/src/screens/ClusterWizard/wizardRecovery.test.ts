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
import { resolveRecoveryStep } from "./wizardRecovery";

describe("resolveRecoveryStep", () => {
  it("restores active requests to Deploy and completed requests to Summary", () => {
    expect(resolveRecoveryStep("clusterCreation", "CLUSTER_INSTALLING_3")).toBe(9);
    expect(resolveRecoveryStep("clusterCreation", "CLUSTER_INSTALLED_4")).toBe(10);
    expect(resolveRecoveryStep("addHost", "ADD_HOSTS_INSTALLING_3")).toBe(6);
    expect(resolveRecoveryStep("addHost", "ADD_HOSTS_INSTALLED_4")).toBe(7);
    expect(resolveRecoveryStep("addService", "SERVICE_STARTING_3")).toBe(6);
    expect(resolveRecoveryStep("addService", "ADD_SERVICES_INSTALLED_4")).toBe(7);
  });

  it("returns undefined for unknown or malformed state", () => {
    expect(resolveRecoveryStep("clusterCreation", "UNKNOWN")).toBeUndefined();
    expect(resolveRecoveryStep("clusterCreation", null)).toBeUndefined();
  });
});
