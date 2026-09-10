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

import WorkflowStateApi, {
  type ScopedWorkflowState,
  type WorkflowScope,
} from "../api/workflowStateApi";
import { translate } from "./Utility";
import { createSecureUuid } from "./uuid";

const SENSITIVE_KEY = /(password|secret|private.?key|ssh.?key|credential|token|cookie|keytab)/i;
const VALUE_FIELDS = new Set([
  "value", "property_value", "propertyvalue", "savedvalue", "recommendedvalue",
  "initialvalue", "defaultvalue", "currentvalue", "previousvalue", "priorvalue",
  "originalvalue", "uservalue", "changedvalue", "newvalue", "oldvalue",
  "default_value", "initial_value", "previous_value", "recommended_value",
  "new_value", "old_value", "confirmpassword", "confirm_password",
]);
const REPOSITORY_URL_FIELDS = new Set([
  "baseurl", "defaulturl", "version_url", "base_url", "default_base_url",
]);
const REENTRY_VALUE_FIELDS = new Set([
  "value", "property_value", "propertyvalue", "savedvalue", "currentvalue",
  "uservalue", "changedvalue", "newvalue", "new_value", "confirmpassword",
  "confirm_password", "baseurl", "base_url", "version_url",
]);
const isSensitiveProperty = (value: Record<string, any>) => {
  const name = value.name ?? value.property_name ?? value.propertyName ?? value.key;
  if (name != null && SENSITIVE_KEY.test(String(name))) return true;
  const attributes = value.propertyAttributes
    ?? value.property_attributes
    ?? value.property_value_attributes;
  return String(attributes?.type || "").toLowerCase() === "password";
};

const safePathMetadata = (property: Record<string, any>, value: unknown, key?: string) => {
  if (typeof value !== "string" || !value.startsWith("/") || /[\n\0]/.test(value)) {
    return false;
  }
  const name = String(
    property.name ?? property.property_name ?? property.propertyName ?? property.key ?? key ?? "",
  ).toLowerCase();
  return name.includes("keytab") || name.includes("principal");
};

const containsUrlUserInfo = (value: unknown) => {
  if (typeof value !== "string") return false;
  try {
    return Boolean(new URL(value).username || new URL(value).password);
  } catch {
    return false;
  }
};

const sanitizeValue = (
  value: any,
  sensitiveContext = false,
  seen = new WeakSet<object>(),
): any => {
  if (value == null || typeof value !== "object") {
    return sensitiveContext ? undefined : value;
  }
  if (seen.has(value)) {
    return undefined;
  }
  seen.add(value);
  if (Array.isArray(value)) {
    const sanitized = sensitiveContext
      ? []
      : value.map((item) => sanitizeValue(item, false, seen)).filter((item) => item !== undefined);
    seen.delete(value);
    return sanitized;
  }

  const mapSensitive = sensitiveContext || isSensitiveProperty(value);
  const sanitized: Record<string, any> = {};
  let redacted = false;
  Object.entries(value).forEach(([key, item]) => {
    const normalizedKey = key.toLowerCase();
    const sensitiveKey = SENSITIVE_KEY.test(key);
    if ((mapSensitive && VALUE_FIELDS.has(normalizedKey) && !safePathMetadata(value, item, key))
      || (sensitiveKey && (item == null || typeof item !== "object")
        && !safePathMetadata(value, item, key))
      || (REPOSITORY_URL_FIELDS.has(normalizedKey) && containsUrlUserInfo(item))) {
      redacted = true;
      return;
    }
    const nextValue = sanitizeValue(item, sensitiveKey && !safePathMetadata(value, item, key), seen);
    if (nextValue !== undefined) sanitized[key] = nextValue;
    else redacted = true;
  });
  if (redacted) sanitized.requires_reentry = true;
  // Only ancestor references are cycles; shared intent/config objects must survive each path.
  seen.delete(value);
  return sanitized;
};

export const sanitizeWorkflowValues = (values: Record<string, any>) =>
  sanitizeValue(values) as Record<string, any>;

export const containsReentryMarker = (value: any): boolean => {
  if (!value || typeof value !== "object") return false;
  if (!Array.isArray(value) && value.requires_reentry === true) return true;
  return Object.values(value).some(containsReentryMarker);
};

const hasReenteredValue = (value: Record<string, any>) => Object.entries(value).some(
  ([key, item]) => (REENTRY_VALUE_FIELDS.has(key.toLowerCase())
    || SENSITIVE_KEY.test(key)
    || /^(localrepovdfdata|versiondefinitionsource)$/i.test(key))
    && item !== undefined
    && item !== null
    && (typeof item !== "string" || item.trim().length > 0),
);

export const clearResolvedReentryMarkers = (value: any): any => {
  if (Array.isArray(value)) return value.map(clearResolvedReentryMarkers);
  if (!value || typeof value !== "object") return value;
  return Object.fromEntries(
    Object.entries(value)
      .filter(([key]) => key !== "requires_reentry" || !hasReenteredValue(value))
      .map(([key, item]) => [key, clearResolvedReentryMarkers(item)]),
  );
};

export class WorkflowReentryRequiredError extends Error {
  constructor() {
    super(String(translate("workflow.persistence.reentryRequired")));
    this.name = "WorkflowReentryRequiredError";
  }
}

export async function runAfterWorkflowCheckpoint<T>(
  checkpoint: () => Promise<unknown>,
  mutation: () => Promise<T>,
) {
  await checkpoint();
  return mutation();
}

export const prepareWorkflowValuesForSave = (value: Record<string, any>) => {
  const prepared = clearResolvedReentryMarkers(value);
  if (containsReentryMarker(prepared)) {
    throw new WorkflowReentryRequiredError();
  }
  return prepared;
};

export const clusterCreationReentrySteps = (values: Record<string, any>) => {
  const steps = values?.state?.clusterCreationSteps || values?.clusterCreationSteps || {};
  return [
    { key: "VERSION", labelKey: "workflow.persistence.reentryStep.version", step: 1 },
    { key: "HOSTS", labelKey: "workflow.persistence.reentryStep.hosts", step: 2 },
    { key: "CONFIGURATION", labelKey: "workflow.persistence.reentryStep.configuration", step: 7 },
  ].filter(({ key }) => containsReentryMarker(steps[key]));
};

export const projectClusterCreationValues = (values: Record<string, any>) => {
  const projected = sanitizeWorkflowValues(values);
  const version = projected?.state?.clusterCreationSteps?.VERSION?.data;
  const source = version?.versionDefinitionSource;
  if (source && typeof source === "object") {
    if (source.type === "xml") {
      version.versionDefinitionSource = {
        type: "xml",
        requires_reentry: true,
      };
    } else if (source.type === "url") {
      version.versionDefinitionSource = {
        type: "url",
        payload: source.payload,
      };
    } else {
      version.versionDefinitionSource = { requires_reentry: true };
    }
  }
  const configuration = projected?.state?.clusterCreationSteps?.CONFIGURATION?.data;
  if (configuration && typeof configuration === "object") {
    delete configuration.configs;
    delete configuration.stackLevelConfigs;
    delete configuration.themes;
  }
  return projected;
};

export const workflowErrorMessage = (error: any, fallback: string) => {
  const code = error?.response?.data?.code;
  if (code === "WORKFLOW_VERSION_CONFLICT") {
    return String(translate("workflow.persistence.versionConflict"));
  }
  if (code === "WORKFLOW_OWNED" || code === "WORKFLOW_ACTIVE") {
    return String(translate("workflow.persistence.owned"));
  }
  return error?.response?.data?.message || error?.message || fallback;
};

type WorkflowApi = Pick<typeof WorkflowStateApi, "get" | "put">;

export type LegacyWorkflowSnapshot = {
  phase: string;
  values: Record<string, any>;
};

type LegacyWorkflowLoader = () => Promise<LegacyWorkflowSnapshot | null>;

export class ScopedWorkflowSession {
  private loaded = false;
  private revision = 0;

  constructor(
    readonly scope: WorkflowScope,
    private readonly api: WorkflowApi = WorkflowStateApi,
  ) {}

  get currentRevision() {
    return this.revision;
  }

  async load(): Promise<ScopedWorkflowState> {
    const state = await this.api.get(this.scope);
    this.revision = state.revision;
    this.loaded = true;
    return state;
  }

  async save(workflow: string, phase: string, values: Record<string, any>) {
    if (!this.loaded) {
      throw new Error("Workflow state must be loaded before it is updated.");
    }
    const state = await this.api.put(this.scope, {
      expected_revision: this.revision,
      workflow,
      phase,
      values: sanitizeWorkflowValues(values),
    });
    this.revision = state.revision;
    return state;
  }

  release() {
    return this.save("IDLE", "IDLE", {});
  }
}

const decodeStoredValue = (value: any) => {
  if (typeof value !== "string") return value;
  try {
    return JSON.parse(value);
  } catch {
    return value;
  }
};

const normalizeLegacyPayload = (payload: string | Record<string, any>) => {
  const parsed = typeof payload === "string" ? JSON.parse(payload) : payload;
  return Object.fromEntries(
    Object.entries(parsed).map(([key, value]) => [key, decodeStoredValue(value)]),
  );
};

export class ClusterWorkflowPersistence {
  private currentWorkflow = "IDLE";
  private loaded = false;
  private values: Record<string, any> = {};
  private readonly queue = new WorkflowMutationQueue();

  readonly session: ScopedWorkflowSession;

  get currentRevision() {
    return this.session.currentRevision;
  }

  constructor(clusterId: string | number, readonly workflow: string, api?: WorkflowApi) {
    if (!/^\d+$/.test(String(clusterId)) || Number(clusterId) <= 0) {
      throw new Error("A numeric cluster ID is required for workflow persistence.");
    }
    this.session = new ScopedWorkflowSession(
      { type: "clusters", id: String(clusterId) },
      api,
    );
  }

  private legacyLoader?: LegacyWorkflowLoader;

  withLegacyLoader(loader: LegacyWorkflowLoader) {
    this.legacyLoader = loader;
    return this;
  }

  private async loadOnce() {
    if (this.loaded) return;
    let state = await this.session.load();
    if (state.revision === 0 && state.workflow === "IDLE" && this.legacyLoader) {
      const legacy = await this.legacyLoader();
      if (legacy) {
        state = await this.session.save(this.workflow, legacy.phase, legacy.values);
      }
    }
    if (state.workflow !== "IDLE" && state.workflow !== this.workflow) {
      throw new Error(String(translate("workflow.persistence.otherActive")));
    }
    this.currentWorkflow = state.workflow;
    this.values = Object.fromEntries(
      Object.entries(state.values || {}).map(([key, value]) => [key, decodeStoredValue(value)]),
    );
    this.loaded = true;
  }

  async getPersistData(key?: string) {
    return this.queue.enqueue(async () => {
      await this.loadOnce();
      return key === undefined ? this.values : this.values[key];
    });
  }

  claim(phase = "START") {
    return this.queue.enqueue(async () => {
      await this.loadOnce();
      if (this.currentWorkflow === this.workflow) return;
      const state = await this.session.save(this.workflow, phase, this.values);
      this.currentWorkflow = state.workflow;
      this.values = state.values || {};
    });
  }

  savePersistData(payload: string | Record<string, any>, phase?: string) {
    return this.queue.enqueue(async () => {
      await this.loadOnce();
      const updates = normalizeLegacyPayload(payload);
      const nextValues = prepareWorkflowValuesForSave({ ...this.values, ...updates });
      const stateData = updates.CLUSTER_STATE;
      const nextPhase = phase
        || stateData?.clusterState
        || stateData?.stepName
        || this.workflow;
      const state = await this.session.save(this.workflow, String(nextPhase), nextValues);
      this.currentWorkflow = state.workflow;
      this.values = nextValues;
    });
  }

  replacePersistData(values: Record<string, any>, phase: string) {
    return this.queue.enqueue(async () => {
      await this.loadOnce();
      const nextValues = prepareWorkflowValuesForSave(values);
      const state = await this.session.save(this.workflow, phase, nextValues);
      this.currentWorkflow = state.workflow;
      this.values = nextValues;
    });
  }

  release() {
    return this.queue.enqueue(async () => {
      await this.loadOnce();
      if (this.currentWorkflow === "IDLE") return;
      await this.session.release();
      this.currentWorkflow = "IDLE";
      this.values = {};
    });
  }

  async reload() {
    await this.queue.reset();
    this.currentWorkflow = "IDLE";
    this.loaded = false;
    this.values = {};
    return this.getPersistData();
  }

  activate() {
    this.queue.activate();
  }

  deactivate() {
    this.queue.deactivate();
  }
}

export class WorkflowQueueInvalidatedError extends Error {}

export class WorkflowMutationQueue {
  private active = true;
  private generation = 0;
  private paused = false;
  private tail: Promise<void> = Promise.resolve();

  get currentGeneration() {
    return this.generation;
  }

  isCurrent(generation: number) {
    return this.active && this.generation === generation;
  }

  enqueue<T>(operation: () => Promise<T>) {
    const generation = this.generation;
    const result = this.tail.then(async () => {
      if (!this.isCurrent(generation) || this.paused) {
        throw new WorkflowQueueInvalidatedError("Workflow persistence was invalidated.");
      }
      try {
        const value = await operation();
        if (!this.isCurrent(generation) || this.paused) {
          throw new WorkflowQueueInvalidatedError("Workflow persistence was invalidated.");
        }
        return value;
      } catch (error) {
        if (this.isCurrent(generation)) this.paused = true;
        throw error;
      }
    });
    this.tail = result.then(() => undefined, () => undefined);
    return result;
  }

  async reset() {
    this.paused = true;
    const generation = ++this.generation;
    await this.tail;
    if (this.active && this.generation === generation) this.paused = false;
    return generation;
  }

  activate() {
    this.active = true;
    this.paused = false;
  }

  deactivate() {
    this.active = false;
    this.paused = true;
    this.generation += 1;
  }
}

export const CLUSTER_DRAFT_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export const isClusterDraftId = (value: string | null | undefined): value is string =>
  Boolean(value && CLUSTER_DRAFT_PATTERN.test(value));

export const createClusterDraftId = () => createSecureUuid();

export const clusterDraftPath = (draftId = createClusterDraftId(), step = 0) =>
  `/installer/step${step}?draft=${encodeURIComponent(draftId)}`;
