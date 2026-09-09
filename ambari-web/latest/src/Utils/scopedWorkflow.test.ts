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
import {
  ClusterWorkflowPersistence,
  ScopedWorkflowSession,
  WorkflowMutationQueue,
  WorkflowQueueInvalidatedError,
  WorkflowReentryRequiredError,
  clearResolvedReentryMarkers,
  clusterCreationReentrySteps,
  containsReentryMarker,
  projectClusterCreationValues,
  runAfterWorkflowCheckpoint,
  sanitizeWorkflowValues,
  workflowErrorMessage,
} from "./scopedWorkflow";

describe("scoped workflow persistence", () => {
  it("does not start a workflow mutation until its checkpoint succeeds", async () => {
    let resolveCheckpoint: () => void = () => undefined;
    const checkpoint = vi.fn(() => new Promise<void>((resolve) => {
      resolveCheckpoint = resolve;
    }));
    const mutation = vi.fn().mockResolvedValue("done");

    const operation = runAfterWorkflowCheckpoint(checkpoint, mutation);
    await Promise.resolve();
    expect(mutation).not.toHaveBeenCalled();
    resolveCheckpoint();

    await expect(operation).resolves.toBe("done");
    expect(checkpoint.mock.invocationCallOrder[0]).toBeLessThan(
      mutation.mock.invocationCallOrder[0],
    );
  });

  it("does not start a workflow mutation after a checkpoint failure", async () => {
    const mutation = vi.fn();

    await expect(runAfterWorkflowCheckpoint(
      () => Promise.reject(new Error("checkpoint failed")),
      mutation,
    )).rejects.toThrow("checkpoint failed");
    expect(mutation).not.toHaveBeenCalled();
  });

  it("uses the exact scope and returned revision at every checkpoint", async () => {
    const api = {
      get: vi.fn().mockResolvedValue({
        revision: 4,
        owner: "alice",
        workflow: "ADD_SERVICE",
        phase: "SERVICES",
        values: {},
      }),
      put: vi.fn()
        .mockResolvedValueOnce({ revision: 5, values: {} })
        .mockResolvedValueOnce({ revision: 6, values: {} }),
    };
    const session = new ScopedWorkflowSession(
      { type: "clusters", id: "72" },
      api as any,
    );

    await session.load();
    await session.save("ADD_SERVICE", "CONFIGURATION", { activeStep: 4 });
    await session.release();

    expect(api.get).toHaveBeenCalledWith({ type: "clusters", id: "72" });
    expect(api.put).toHaveBeenNthCalledWith(1, { type: "clusters", id: "72" }, {
      expected_revision: 4,
      workflow: "ADD_SERVICE",
      phase: "CONFIGURATION",
      values: { activeStep: 4 },
    });
    expect(api.put).toHaveBeenNthCalledWith(2, { type: "clusters", id: "72" }, {
      expected_revision: 5,
      workflow: "IDLE",
      phase: "IDLE",
      values: {},
    });
  });

  it("does not advance the revision after a two-tab conflict", async () => {
    const conflict = { response: { data: { code: "WORKFLOW_VERSION_CONFLICT" } } };
    const api = {
      get: vi.fn().mockResolvedValue({ revision: 8, values: {} }),
      put: vi.fn().mockRejectedValue(conflict),
    };
    const session = new ScopedWorkflowSession(
      { type: "drafts", id: "9d8e1f19-8fa5-4472-bb2a-c774b561f462" },
      api as any,
    );

    await session.load();
    await expect(session.save("CLUSTER_CREATE", "HOSTS", {})).rejects.toBe(conflict);

    expect(session.currentRevision).toBe(8);
    expect(workflowErrorMessage(conflict, "fallback")).toMatch(/another tab/i);
  });

  it("removes credentials and repository URL userinfo while preserving edits", () => {
    const values = sanitizeWorkflowValues({
      hosts: ["host1"],
      sshKey: "-----BEGIN PRIVATE KEY-----",
      configs: [{
        propertyName: "database.password",
        propertyAttributes: { type: "password" },
        value: "secret-value",
        recommendedValue: "other-secret",
      }, {
        propertyName: "dfs.namenode.keytab.file",
        value: "/etc/security/keytabs/nn.service.keytab",
      }],
      repository: {
        baseUrl: "https://user:secret@repo.example/hdp",
        name: "HDP",
      },
    });

    expect(values.hosts).toEqual(["host1"]);
    expect(values).not.toHaveProperty("sshKey");
    expect(values.configs[0]).not.toHaveProperty("value");
    expect(values.configs[0]).not.toHaveProperty("recommendedValue");
    expect(values.configs[1].value).toBe("/etc/security/keytabs/nn.service.keytab");
    expect(values.repository).not.toHaveProperty("baseUrl");
    expect(containsReentryMarker(values)).toBe(true);
  });

  it("keeps unresolved credential markers until a replacement value is entered", () => {
    expect(clearResolvedReentryMarkers({
      propertyName: "database.password",
      requires_reentry: true,
    })).toEqual({
      propertyName: "database.password",
      requires_reentry: true,
    });
    expect(clearResolvedReentryMarkers({
      propertyName: "database.password",
      requires_reentry: true,
      value: "replacement",
    })).toEqual({
      propertyName: "database.password",
      value: "replacement",
    });

    const corrected = clearResolvedReentryMarkers({
      state: {
        clusterCreationSteps: {
          VERSION: {
            data: {
              operatingSystems: {
                version1: [{
                  repos: [{ baseUrl: "https://repo.example/hdp", requires_reentry: true }],
                }],
              },
            },
          },
          CONFIGURATION: {
            data: {
              configProperties: [{
                propertyName: "database.password",
                requires_reentry: true,
              }],
            },
          },
        },
      },
    });
    expect(clusterCreationReentrySteps(corrected).map(({ key }) => key))
      .toEqual(["CONFIGURATION"]);
  });

  it("keeps direct keytab path metadata without retaining keytab content", () => {
    expect(sanitizeWorkflowValues({
      keytab: "/etc/security/keytabs/nn.service.keytab",
      keytabContent: "private material",
    })).toEqual({
      keytab: "/etc/security/keytabs/nn.service.keytab",
      requires_reentry: true,
    });
  });

  it("compacts regenerable installer catalogs while preserving configuration edits", () => {
    const projected = projectClusterCreationValues({
      state: {
        clusterCreationSteps: {
          CONFIGURATION: {
            data: {
              configs: { items: ["catalog"] },
              configProperties: { HDFS: { site: { properties: { edited: { value: "yes" } } } } },
              stackLevelConfigs: { items: ["stack catalog"] },
              themes: { items: ["layout"] },
            },
          },
        },
      },
    });

    const configuration = projected.state.clusterCreationSteps.CONFIGURATION.data;
    expect(configuration).not.toHaveProperty("configs");
    expect(configuration).not.toHaveProperty("stackLevelConfigs");
    expect(configuration).not.toHaveProperty("themes");
    expect(configuration.configProperties.HDFS.site.properties.edited.value).toBe("yes");
  });

  it("requires VDF file reentry without persisting opaque XML", () => {
    const projected = projectClusterCreationValues({
      state: {
        clusterCreationSteps: {
          VERSION: {
            data: {
              selectedVersion: { id: "HDP-3.0" },
              operatingSystems: { "HDP-3.0": [{ repos: [{ baseUrl: "https://repo.example/hdp" }] }] },
              versionDefinitionSource: {
                type: "xml",
                payload: "<repository><baseurl>https://user:secret@repo.example/hdp</baseurl></repository>",
                headers: { "Content-Type": "text/xml", Authorization: "Basic opaque" },
              },
            },
          },
        },
      },
    });

    const version = projected.state.clusterCreationSteps.VERSION.data;
    expect(version.versionDefinitionSource).toEqual({
      type: "xml",
      requires_reentry: true,
    });
    expect(version.selectedVersion.id).toBe("HDP-3.0");
    expect(version.operatingSystems["HDP-3.0"][0].repos[0].baseUrl)
      .toBe("https://repo.example/hdp");
    expect(clusterCreationReentrySteps(projected).map(({ key }) => key))
      .toEqual(["VERSION"]);
  });

  it("keeps a credential-free VDF URL descriptor and drops its headers", () => {
    const projected = projectClusterCreationValues({
      state: {
        clusterCreationSteps: {
          VERSION: {
            data: {
              versionDefinitionSource: {
                type: "url",
                payload: { VersionDefinition: { version_url: "https://repo.example/vdf.xml" } },
                headers: { Authorization: "Bearer opaque" },
              },
            },
          },
        },
      },
    });

    expect(projected.state.clusterCreationSteps.VERSION.data.versionDefinitionSource)
      .toEqual({
        type: "url",
        payload: { VersionDefinition: { version_url: "https://repo.example/vdf.xml" } },
      });
  });

  it("pauses after failure and invalidates queued snapshots before reset", async () => {
    const queue = new WorkflowMutationQueue();
    let rejectFirst!: (reason: Error) => void;
    let markStarted!: () => void;
    const started = new Promise<void>((resolve) => { markStarted = resolve; });
    const first = queue.enqueue(() => new Promise<void>((_resolve, reject) => {
      rejectFirst = reject;
      markStarted();
    }));
    const staleOperation = vi.fn();
    const stale = queue.enqueue(async () => staleOperation());

    await started;
    rejectFirst(new Error("conflict"));
    await expect(first).rejects.toThrow("conflict");
    await expect(stale).rejects.toBeInstanceOf(WorkflowQueueInvalidatedError);
    await queue.reset();
    await queue.enqueue(async () => undefined);

    expect(staleOperation).not.toHaveBeenCalled();
  });

  it("rejects an in-flight result after its scope is deactivated", async () => {
    const queue = new WorkflowMutationQueue();
    let resolveOperation!: (value: string) => void;
    let markStarted!: () => void;
    const started = new Promise<void>((resolve) => { markStarted = resolve; });
    const operation = queue.enqueue(() => new Promise<string>((resolve) => {
      resolveOperation = resolve;
      markStarted();
    }));

    await started;
    queue.deactivate();
    resolveOperation("stale result");

    await expect(operation).rejects.toBeInstanceOf(WorkflowQueueInvalidatedError);
  });

  it("pauses a cluster adapter after conflict and reloads before accepting new writes", async () => {
    const conflict = { response: { data: { code: "WORKFLOW_VERSION_CONFLICT" } } };
    const api = {
      get: vi.fn()
        .mockResolvedValueOnce({ revision: 4, workflow: "ADD_SERVICE", values: { saved: "old" } })
        .mockResolvedValueOnce({ revision: 8, workflow: "ADD_SERVICE", values: { saved: "remote" } }),
      put: vi.fn()
        .mockRejectedValueOnce(conflict)
        .mockResolvedValueOnce({ revision: 9, workflow: "ADD_SERVICE", values: { saved: "new" } }),
    };
    const persistence = new ClusterWorkflowPersistence(72, "ADD_SERVICE", api as any);

    expect(await persistence.getPersistData("saved")).toBe("old");
    await expect(persistence.savePersistData({ saved: "stale" })).rejects.toBe(conflict);
    await expect(persistence.savePersistData({ saved: "queued" }))
      .rejects.toBeInstanceOf(WorkflowQueueInvalidatedError);
    expect(await persistence.reload()).toEqual({ saved: "remote" });
    await persistence.savePersistData({ saved: "new" });

    expect(api.put).toHaveBeenLastCalledWith({ type: "clusters", id: "72" }, {
      expected_revision: 8,
      workflow: "ADD_SERVICE",
      phase: "ADD_SERVICE",
      values: { saved: "new" },
    });
  });

  it("blocks unresolved recovery but keeps a re-entered credential in runtime memory", async () => {
    const recoveredValues = {
      ENABLING_KERBEROS: {
        config: { propertyName: "admin_password", requires_reentry: true },
      },
    };
    const api = {
      get: vi.fn().mockResolvedValue({
        revision: 4,
        workflow: "ENABLING_KERBEROS",
        values: recoveredValues,
      }),
      put: vi.fn().mockResolvedValue({
        revision: 5,
        workflow: "ENABLING_KERBEROS",
        values: {
          ENABLING_KERBEROS: {
            config: { propertyName: "admin_password", requires_reentry: true },
          },
        },
      }),
    };
    const persistence = new ClusterWorkflowPersistence(
      72,
      "ENABLING_KERBEROS",
      api as any,
    );

    await persistence.getPersistData();
    await expect(persistence.savePersistData({ activeStep: "CONFIGURE_KERBEROS" }))
      .rejects.toBeInstanceOf(WorkflowReentryRequiredError);
    expect(api.put).not.toHaveBeenCalled();

    await persistence.savePersistData({
      ENABLING_KERBEROS: {
        config: {
          propertyName: "admin_password",
          value: "new-password",
          requires_reentry: true,
        },
      },
    }, "CONFIGURE_KERBEROS");

    expect(api.put.mock.calls[0][1].values.ENABLING_KERBEROS.config)
      .toEqual({ propertyName: "admin_password", requires_reentry: true });
    expect(await persistence.getPersistData("ENABLING_KERBEROS"))
      .toEqual({ config: { propertyName: "admin_password", value: "new-password" } });
  });

  it("does not let a deactivated cluster adapter complete an awaited save", async () => {
    let resolveSave!: (value: any) => void;
    const api = {
      get: vi.fn().mockResolvedValue({ revision: 2, workflow: "ADD_HOST", values: {} }),
      put: vi.fn().mockReturnValue(new Promise((resolve) => { resolveSave = resolve; })),
    };
    const persistence = new ClusterWorkflowPersistence(73, "ADD_HOST", api as any);
    await persistence.getPersistData();
    const save = persistence.savePersistData({ activeStep: "HOSTS" });
    await vi.waitFor(() => expect(api.put).toHaveBeenCalledTimes(1));

    persistence.deactivate();
    resolveSave({ revision: 3, workflow: "ADD_HOST", values: { activeStep: "HOSTS" } });

    await expect(save).rejects.toBeInstanceOf(WorkflowQueueInvalidatedError);
  });

  it("reactivates a StrictMode-remounted adapter without replaying stale work", async () => {
    const api = {
      get: vi.fn().mockResolvedValue({ revision: 1, workflow: "ADD_HOST", values: {} }),
      put: vi.fn().mockResolvedValue({
        revision: 2,
        workflow: "ADD_HOST",
        values: { activeStep: "HOSTS" },
      }),
    };
    const persistence = new ClusterWorkflowPersistence(73, "ADD_HOST", api as any);

    persistence.deactivate();
    persistence.activate();
    await persistence.savePersistData({ activeStep: "HOSTS" });

    expect(api.put).toHaveBeenCalledOnce();
  });

  it("imports a verified legacy snapshot only into an unused scoped revision", async () => {
    const legacy = {
      phase: "SERVICES",
      values: { ADD_SERVICE: { activeStep: "SERVICES" } },
    };
    const api = {
      get: vi.fn().mockResolvedValue({ revision: 0, workflow: "IDLE", values: {} }),
      put: vi.fn().mockResolvedValue({
        revision: 1,
        workflow: "ADD_SERVICE",
        phase: "SERVICES",
        values: legacy.values,
      }),
    };
    const persistence = new ClusterWorkflowPersistence(72, "ADD_SERVICE", api as any)
      .withLegacyLoader(vi.fn().mockResolvedValue(legacy));

    await expect(persistence.getPersistData("ADD_SERVICE"))
      .resolves.toEqual({ activeStep: "SERVICES" });
    expect(api.put).toHaveBeenCalledWith({ type: "clusters", id: "72" }, {
      expected_revision: 0,
      workflow: "ADD_SERVICE",
      phase: "SERVICES",
      values: legacy.values,
    });
  });

  it("does not consult legacy state after a scoped workflow has a revision", async () => {
    const legacyLoader = vi.fn();
    const api = {
      get: vi.fn().mockResolvedValue({
        revision: 3,
        workflow: "REASSIGN_COMPONENT",
        values: { REASSIGN_COMPONENT: { activeStep: "MOVE" } },
      }),
      put: vi.fn(),
    };
    const persistence = new ClusterWorkflowPersistence(72, "REASSIGN_COMPONENT", api as any)
      .withLegacyLoader(legacyLoader);

    await persistence.getPersistData();

    expect(legacyLoader).not.toHaveBeenCalled();
  });
});
