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
  clearDeploymentSignatureScope,
  createDeploymentSignatureScope,
  deploymentInputSignature,
  reconcileDeploymentInputSignatures,
} from "./deploymentInputRecovery";

describe("deployment input recovery", () => {
  it("uses stable object-key ordering without persisting values or hiding placement changes", async () => {
    const first = await deploymentInputSignature({
      b: 2,
      a: { d: 4, c: 3 },
    });
    const reordered = await deploymentInputSignature({
      a: { c: 3, d: 4 },
      b: 2,
    });
    expect(first).toBe(reordered);
    expect(first).toMatch(/^sha256:[a-f0-9]{64}$/);
    expect(await deploymentInputSignature([{ host: "a" }, { host: "b" }]))
      .not.toBe(await deploymentInputSignature([{ host: "b" }, { host: "a" }]));
  });

  it("replays ordinary config but identifies changed materialization topology", async () => {
    const previous = {
      configuration: await deploymentInputSignature({ "hbase-site": { tuning: "old" } }),
      hosts: await deploymentInputSignature(["host-a"]),
      masters: await deploymentInputSignature([{ component: "HBASE_MASTER", host: "host-a" }]),
      services: await deploymentInputSignature(["HBASE"]),
    };
    const current = {
      ...previous,
      configuration: await deploymentInputSignature({ "hbase-site": { tuning: "new" } }),
      masters: await deploymentInputSignature([{ component: "HBASE_MASTER", host: "host-b" }]),
    };

    const result = reconcileDeploymentInputSignatures({
      completedOperationIds: [
        "create-cluster",
        "create-services",
        "apply-configurations-HDFS:binding-a",
        "create-components",
        "register-hosts",
        "register-masters",
        "register-slaves-clients",
      ],
      current,
      previous,
    });

    expect(result.changedInputs).toEqual(["configuration", "masters"]);
    expect(result.topologyChanges).toEqual(["masters"]);
    expect(result.completedOperationIds).toEqual([
      "create-cluster",
      "create-services",
      "create-components",
      "register-hosts",
      "register-slaves-clients",
    ]);
  });

  it("does not trust legacy completed operations without their input signatures", async () => {
    const result = reconcileDeploymentInputSignatures({
      completedOperationIds: ["create-cluster", "create-services", "create-components"],
      current: { services: await deploymentInputSignature(["HBASE"]) },
    });

    expect(result.completedOperationIds).toEqual(["create-cluster"]);
  });

  it("uses a stable sanitized digest when insecure HTTP omits subtle and randomUUID", async () => {
    const source = () => ({
      getRandomValues: (bytes: Uint8Array) => {
        bytes.fill(7);
        return bytes;
      },
    });
    const first = await deploymentInputSignature({ hosts: ["host-a"] }, source());
    const reloaded = await deploymentInputSignature({ hosts: ["host-a"] }, source());
    const changed = await deploymentInputSignature({ hosts: ["host-b"] }, source());

    expect(first).toBe(reloaded);
    expect(first).toMatch(/^local:[a-f0-9]{16}$/);
    expect(changed).not.toBe(first);
    expect(reconcileDeploymentInputSignatures({
      completedOperationIds: ["register-masters"],
      current: { masters: reloaded },
      previous: { masters: first },
    }).topologyChanges).toEqual([]);
    expect(reconcileDeploymentInputSignatures({
      completedOperationIds: ["register-masters"],
      current: { masters: changed },
      previous: { masters: first },
    }).topologyChanges).toEqual(["masters"]);
  });

  it("does not persist a password-derived digest and keeps its epoch stable in memory", async () => {
    let randomValue = 6;
    const scope = createDeploymentSignatureScope();
    const source = {
      getRandomValues: (bytes: Uint8Array) => {
        randomValue += 1;
        bytes.fill(randomValue);
        return bytes;
      },
    };
    const first = await deploymentInputSignature({
      propertyName: "database_password",
      value: "never-persist-this-value",
    }, source, scope);
    const repeated = await deploymentInputSignature({
      propertyName: "database_password",
      value: "never-persist-this-value",
    }, source, scope);
    const changed = await deploymentInputSignature({
      propertyName: "database_password",
      value: "a-new-secret",
    }, source, scope);

    expect(first).toBe(repeated);
    expect(changed).not.toBe(first);
    expect(first).toMatch(/^local:[a-f0-9]{16}:07070707-0707-4707-8707-070707070707$/);
    expect(first).not.toContain("never-persist-this-value");
    expect(changed).not.toContain("a-new-secret");
    clearDeploymentSignatureScope(scope);
    expect(scope.sensitiveEpochs.size).toBe(0);
  });

  it("allows placement edits before an assignment operation materializes", async () => {
    const previous = {
      masters: await deploymentInputSignature([{ component: "NAMENODE", hostName: "host-a" }]),
    };
    const current = {
      masters: await deploymentInputSignature([{ component: "NAMENODE", hostName: "host-b" }]),
    };

    const result = reconcileDeploymentInputSignatures({
      completedOperationIds: ["create-cluster", "create-services"],
      current,
      previous,
    });

    expect(result.topologyChanges).toEqual([]);
    expect(result.changedInputs).toEqual(["masters"]);
  });

  it("preserves topology once its exact host-component write was attempted", async () => {
    const previous = {
      masters: await deploymentInputSignature([{ component: "NAMENODE", hostName: "host-a" }]),
    };
    const current = {
      masters: await deploymentInputSignature([{ component: "NAMENODE", hostName: "host-b" }]),
    };

    expect(reconcileDeploymentInputSignatures({
      attemptedTopologyInputs: ["masters"],
      completedOperationIds: ["create-components"],
      current,
      previous,
    }).topologyChanges).toEqual(["masters"]);
  });
});
