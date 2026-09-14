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

import { supressErrorAmbariApi } from "./config/axiosConfig";

export type WorkflowScope = {
  type: "clusters" | "drafts";
  id: string;
};

export type ScopedWorkflowState = {
  revision: number;
  owner: string | null;
  workflow: string;
  phase: string;
  values: Record<string, any>;
};

export type ScopedWorkflowUpdate = {
  expected_revision: number;
  workflow: string;
  phase: string;
  values: Record<string, any>;
};

export type ClusterCreationDraftSummary = {
  draft_id: string;
  revision: number;
  workflow: "CLUSTER_CREATE";
  phase: string;
  cluster_id?: number;
  cluster_name?: string;
};

export type ClusterCreationDraftTarget = {
  cluster_id: number;
  cluster_name: string;
};

const scopePath = (scope: WorkflowScope) =>
  `/persist/scopes/${scope.type}/${encodeURIComponent(scope.id)}`;

const WorkflowStateApi = {
  get: async (scope: WorkflowScope, summary = false): Promise<ScopedWorkflowState> => {
    const response = await supressErrorAmbariApi.request({
      url: scopePath(scope),
      method: "GET",
      params: summary ? { summary: true } : undefined,
    });
    return response.data;
  },

  put: async (
    scope: WorkflowScope,
    update: ScopedWorkflowUpdate,
  ): Promise<ScopedWorkflowState> => {
    const response = await supressErrorAmbariApi.request({
      url: scopePath(scope),
      method: "PUT",
      data: update,
    });
    return response.data;
  },

  getCreationDrafts: async (): Promise<ClusterCreationDraftSummary[]> => {
    const response = await supressErrorAmbariApi.request({
      url: "/persist/scopes/drafts",
      method: "GET",
    });
    return response.data?.items || [];
  },

  getCreationDraftCluster: async (
    draftId: string,
  ): Promise<ClusterCreationDraftTarget> => {
    const response = await supressErrorAmbariApi.request({
      url: `/persist/scopes/drafts/${encodeURIComponent(draftId)}/cluster`,
      method: "GET",
    });
    return response.data;
  },
};

export default WorkflowStateApi;
