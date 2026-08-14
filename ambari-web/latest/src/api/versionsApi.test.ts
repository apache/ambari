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

import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({ request: vi.fn() }));
vi.mock("./config/axiosConfig", () => ({ ambariApi: { request: mocks.request } }));

import VersionsApi from "./versionsApi";

describe("versions API", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.request.mockResolvedValue({ data: { Requests: { id: 17 } } });
  });

  it("installs a repository version on one encoded host", async () => {
    const response = await VersionsApi.installHostStackVersion(
      "cluster/name",
      "host one",
      {
        stack: "HDP-3.1",
        version: "3.1.5",
        repoVersion: "3.1.5.0-1",
      },
    );

    expect(mocks.request).toHaveBeenCalledWith({
      url: "/clusters/cluster%2Fname/hosts/host%20one/stack_versions",
      method: "POST",
      data: {
        HostStackVersions: {
          stack: "HDP-3.1",
          version: "3.1.5",
          repository_version: "3.1.5.0-1",
        },
      },
    });
    expect(response).toEqual({ Requests: { id: 17 } });
  });

  it("loads repository versions compatible with the current stack", async () => {
    await VersionsApi.getCompatibleRepositoryVersions("HDP/name", "3.1 version");

    expect(mocks.request).toHaveBeenCalledWith({
      url: "/stacks/HDP%2Fname/versions/3.1%20version/compatible_repository_versions",
      method: "GET",
      params: {
        fields: "CompatibleRepositoryVersions/repository_version",
        minimal_response: true,
      },
    });
  });
});
