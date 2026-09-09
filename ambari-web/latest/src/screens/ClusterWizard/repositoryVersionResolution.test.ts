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

import { describe, expect, it } from "vitest";
import {
  buildAtomicVersionDefinitionPayload,
  buildInitialOperatingSystems,
  repositorySelectionRequiresDeferredWrite,
  resolveRepositoryVersion,
} from "./repositoryVersionResolution";

const desired = {
  ambariManagedRepositories: true,
  operatingSystems: [{
    isAdded: true,
    os: "redhat8",
    repos: [{
      applicableServices: ["HDFS"],
      baseUrl: "https://repo.example/hdp",
      components: ["NAMENODE"],
      distribution: "GA",
      id: "HDP",
      mirrorsList: "https://repo.example/mirrors",
      name: "HDP",
      tags: ["default"],
      unique: true,
    }],
  }],
  repositoryVersion: "3.0.1.0-1",
  stackName: "HDP",
  stackVersion: "3.0",
};

const definition = (
  baseUrl: string,
  id: string | number = 101,
  unique = true,
) => ({
  VersionDefinition: {
    id,
    repository_version: "3.0.1.0-1",
    stack_name: "HDP",
    stack_version: "3.0",
  },
  operating_systems: [{
    OperatingSystems: {
      ambari_managed_repositories: true,
      os_type: "redhat8",
    },
    repositories: [{
      Repositories: {
        applicable_services: ["HDFS"],
        components: ["NAMENODE"],
        distribution: "GA",
        mirrors_list: "https://repo.example/mirrors",
        repo_id: "HDP",
        repo_name: "HDP",
        base_url: baseUrl,
        tags: ["default"],
        unique,
      },
    }],
  }],
});

describe("repository version resolution", () => {
  it("reuses an identical materialized repository definition", () => {
    expect(resolveRepositoryVersion({
      ...desired,
      items: [definition("https://repo.example/hdp")],
    })).toEqual({
      kind: "reuse",
      repositoryVersionId: "101",
      stackName: "HDP",
      stackVersion: "3.0",
    });
  });

  it("reports a conflict instead of modifying a shared definition", () => {
    expect(resolveRepositoryVersion({
      ...desired,
      items: [definition("https://other.example/hdp")],
    })).toEqual(expect.objectContaining({ kind: "conflict" }));
  });

  it("treats Satellite mode as part of repository compatibility", () => {
    expect(resolveRepositoryVersion({
      ...desired,
      ambariManagedRepositories: false,
      items: [definition("https://repo.example/hdp")],
    })).toEqual(expect.objectContaining({ kind: "conflict" }));
  });

  it("treats the unique repository flag as part of compatibility", () => {
    expect(resolveRepositoryVersion({
      ...desired,
      items: [definition("https://repo.example/hdp", 101, false)],
    })).toEqual(expect.objectContaining({ kind: "conflict" }));
  });

  it("builds the complete atomic repository payload and base64 encodes XML", () => {
    const operatingSystems = buildInitialOperatingSystems(
      desired.operatingSystems,
      desired.ambariManagedRepositories,
    );
    const payload = buildAtomicVersionDefinitionPayload("<repository-version/>", operatingSystems);

    expect(payload).toEqual({
      VersionDefinition: {
        version_base64: btoa("<repository-version/>"),
      },
      operating_systems: [{
        "OperatingSystems/ambari_managed_repositories": true,
        "OperatingSystems/os_type": "redhat8",
        repositories: [{
          "Repositories/applicable_services": ["HDFS"],
          "Repositories/base_url": "https://repo.example/hdp",
          "Repositories/components": ["NAMENODE"],
          "Repositories/distribution": "GA",
          "Repositories/mirrors_list": "https://repo.example/mirrors",
          "Repositories/repo_id": "HDP",
          "Repositories/repo_name": "HDP",
          "Repositories/tags": ["default"],
          "Repositories/unique": true,
        }],
      }],
    });
  });

  it("omits absent optional repository metadata from the serialized default payload", () => {
    const operatingSystems = buildInitialOperatingSystems([{
      isAdded: true,
      os: "redhat8",
      repos: [{
        baseUrl: "https://repo.example/hdp",
        id: "HDP",
        name: "HDP",
      }],
    }], true);

    expect(JSON.parse(JSON.stringify(operatingSystems))).toEqual([{
      "OperatingSystems/ambari_managed_repositories": true,
      "OperatingSystems/os_type": "redhat8",
      repositories: [{
        "Repositories/base_url": "https://repo.example/hdp",
        "Repositories/repo_id": "HDP",
        "Repositories/repo_name": "HDP",
      }],
    }]);
  });

  it("materializes available templates and defers installer repository writes", () => {
    expect(resolveRepositoryVersion({
      ...desired,
      items: [definition("https://repo.example/hdp", "HDP-3.0")],
    })).toEqual({ kind: "create" });
    expect(repositorySelectionRequiresDeferredWrite("clusterCreation")).toBe(true);
    expect(repositorySelectionRequiresDeferredWrite("addHost")).toBe(false);
  });
});
