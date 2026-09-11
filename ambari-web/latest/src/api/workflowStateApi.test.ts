/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
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
vi.mock("./config/axiosConfig", () => ({
  supressErrorAmbariApi: { request: mocks.request },
}));

import WorkflowStateApi from "./workflowStateApi";

describe("WorkflowStateApi cluster creation recovery", () => {
  beforeEach(() => vi.clearAllMocks());

  it("lists only the authenticated user's draft summaries", async () => {
    const items = [{
      draft_id: "2e97ec6a-c03d-4c52-b910-1a58ae50f390",
      revision: 4,
      workflow: "CLUSTER_CREATE",
      phase: "HOSTS",
    }];
    mocks.request.mockResolvedValue({ data: { items } });

    await expect(WorkflowStateApi.getCreationDrafts()).resolves.toEqual(items);
    expect(mocks.request).toHaveBeenCalledWith({
      method: "GET",
      url: "/persist/scopes/drafts",
    });
  });

  it("resolves cluster ownership through the exact encoded draft identity", async () => {
    mocks.request.mockResolvedValue({
      data: { cluster_id: 84, cluster_name: "cluster1" },
    });

    await expect(WorkflowStateApi.getCreationDraftCluster("draft/id"))
      .resolves.toEqual({ cluster_id: 84, cluster_name: "cluster1" });
    expect(mocks.request).toHaveBeenCalledWith({
      method: "GET",
      url: "/persist/scopes/drafts/draft%2Fid/cluster",
    });
  });
});
