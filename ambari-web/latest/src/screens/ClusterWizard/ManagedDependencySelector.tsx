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

import { useEffect, useMemo, useRef, useState } from "react";
import { Alert, Badge, Button, Form, Spinner } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import ServiceDependenciesApi, {
  type ManagedDependencyCandidate,
  type ManagedDependencyIssue,
  type ManagedDependencyPlanPreviewResponse,
  type ManagedDependencyType,
} from "../../api/serviceDependenciesApi";
import {
  candidateCanBePlanned,
  dependencyTypes,
  isIncompleteDependencyPlan,
  type ManagedDependencyChoice,
  type ManagedDependencySelections,
  providerIdentity,
} from "./managedDependencySelection";
import { createSecureUuid } from "../../Utils/uuid";

export type DependencyConsumerScope =
  | {
      clusterId: number;
      checkpoint: () => Promise<number>;
      kind: "servicePlan";
    }
  | {
      checkpoint: () => Promise<number>;
      draftId: string;
      kind: "draft";
    }
  | {
      checkpoint: () => Promise<number>;
      clusterId: number;
      clusterName: string;
      kind: "service";
    };

type CandidateState = {
  error?: string;
  items: ManagedDependencyCandidate[];
  loading: boolean;
};

type SelectionFailure = {
  message: string;
  retry: () => void;
};

const emptyCandidates = (): Record<ManagedDependencyType, CandidateState> => ({
  HDFS: { items: [], loading: false },
  ZOOKEEPER: { items: [], loading: false },
});

const errorDetails = (error: any, fallback: string): ManagedDependencyIssue => ({
  code: String(error?.response?.data?.code || "DEPENDENCY_REQUEST_FAILED"),
  message: String(
    error?.response?.data?.message
      || error?.message
      || fallback,
  ),
});

export default function ManagedDependencySelector({
  consumer,
  onSelectionChange,
  onPlanSelectionChange,
  selections,
}: {
  consumer: DependencyConsumerScope;
  onSelectionChange: (
    type: ManagedDependencyType,
    choice: ManagedDependencyChoice,
  ) => Promise<number>;
  onPlanSelectionChange?: (
    selections: ManagedDependencySelections,
  ) => Promise<number>;
  selections: ManagedDependencySelections;
}) {
  const { t } = useTranslation();
  const [candidateState, setCandidateState] = useState(emptyCandidates);
  const [filter, setFilter] = useState("");
  const [previewing, setPreviewing] = useState<ManagedDependencyType | null>(null);
  const [selectionFailure, setSelectionFailure] = useState<SelectionFailure | null>(null);
  const scopeKey = consumer.kind === "draft"
    ? JSON.stringify(["draft", consumer.draftId])
    : consumer.kind === "service"
      ? JSON.stringify(["service", consumer.clusterId, consumer.clusterName])
      : JSON.stringify(["service-plan", consumer.clusterId]);
  const scopeKeyRef = useRef(scopeKey);
  const loadGenerationRef = useRef(0);
  const selectionGenerationRef = useRef<Record<ManagedDependencyType, number>>({
    HDFS: 0,
    ZOOKEEPER: 0,
  });
  const selectionControllersRef = useRef<Partial<Record<ManagedDependencyType, AbortController>>>({});
  const planControllerRef = useRef<AbortController | null>(null);
  const planGenerationRef = useRef(0);
  const bindingIdsRef = useRef<Record<string, string>>({});
  const loadControllerRef = useRef<AbortController | null>(null);
  scopeKeyRef.current = scopeKey;

  const loadCandidates = async () => {
    const generation = ++loadGenerationRef.current;
    const capturedScope = scopeKey;
    loadControllerRef.current?.abort();
    const controller = new AbortController();
    loadControllerRef.current = controller;
    setCandidateState({
      HDFS: { items: [], loading: true },
      ZOOKEEPER: { items: [], loading: true },
    });
    let expectedRevision = 0;
    try {
      expectedRevision = await consumer.checkpoint();
    } catch (error: any) {
      if (controller.signal.aborted || generation !== loadGenerationRef.current
        || capturedScope !== scopeKeyRef.current) return;
      const message = errorDetails(error, t("managedDependencies.requestFailed")).message;
      setCandidateState({
        HDFS: { items: [], loading: false, error: message },
        ZOOKEEPER: { items: [], loading: false, error: message },
      });
      return;
    }
    if (controller.signal.aborted || generation !== loadGenerationRef.current
      || capturedScope !== scopeKeyRef.current) return;
    await Promise.all(dependencyTypes.map(async (type) => {
      try {
        const items = consumer.kind === "draft"
          ? await ServiceDependenciesApi.getDraftCandidates({
              dependencyType: type,
              draftId: consumer.draftId,
              expectedDraftRevision: expectedRevision,
              signal: controller.signal,
            })
          : consumer.kind === "service"
            ? await ServiceDependenciesApi.getServiceCandidates(
                consumer.clusterName,
                type,
                controller.signal,
              )
            : await ServiceDependenciesApi.getServicePlanCandidates({
                clusterId: consumer.clusterId,
                dependencyType: type,
                expectedWorkflowRevision: expectedRevision,
                signal: controller.signal,
              });
        if (generation !== loadGenerationRef.current || capturedScope !== scopeKeyRef.current) return;
        setCandidateState((current) => ({
          ...current,
          [type]: { items, loading: false },
        }));
      } catch (error: any) {
        if (controller.signal.aborted || generation !== loadGenerationRef.current
          || capturedScope !== scopeKeyRef.current) return;
        setCandidateState((current) => ({
          ...current,
          [type]: {
            items: [],
            loading: false,
            error: errorDetails(error, t("managedDependencies.requestFailed")).message,
          },
        }));
      }
    }));
  };

  useEffect(() => {
    setSelectionFailure(null);
    setPreviewing(null);
    void loadCandidates();
    return () => {
      loadGenerationRef.current += 1;
      loadControllerRef.current?.abort();
      dependencyTypes.forEach((type) => {
        selectionGenerationRef.current[type] += 1;
        selectionControllersRef.current[type]?.abort();
      });
      planGenerationRef.current += 1;
      planControllerRef.current?.abort();
    };
  }, [scopeKey]);

  const selectionBindingId = (
    type: ManagedDependencyType,
    choice: ManagedDependencyChoice,
  ) => {
    const provider = choice.provider || choice.preview?.provider;
    if (!provider) return undefined;
    const key = `${type}:${provider.cluster_id}:${provider.service_name}`;
    const bindingId = choice.preview?.binding_id || bindingIdsRef.current[key] || createSecureUuid();
    bindingIdsRef.current[key] = bindingId;
    return bindingId;
  };

  const previewWholeSelection = async (
    nextSelections: ManagedDependencySelections,
    capturedScope: string,
    generation: number,
    controller: AbortController,
  ) => {
    const revision = await onPlanSelectionChange!(nextSelections);
    if (generation !== planGenerationRef.current
      || controller.signal.aborted
      || capturedScope !== scopeKeyRef.current) return;
    const managed = dependencyTypes.flatMap((type) => {
      const choice = nextSelections[type];
      return choice?.mode === "managed" && (choice.provider || choice.preview?.provider)
        ? [{ type, choice }]
        : [];
    });
    if (!managed.length) return;
    const selections = managed.map(({ type, choice }) => ({
      binding_id: selectionBindingId(type, choice)!,
      dependency_type: type,
      provider: {
        cluster_id: (choice.provider || choice.preview?.provider)!.cluster_id,
        service_name: (choice.provider || choice.preview?.provider)!.service_name,
      },
    }));
    const planConsumer = consumer.kind === "draft"
      ? {
          scope: "DRAFT" as const,
          draft_id: consumer.draftId,
          expected_revision: revision,
        }
      : consumer.kind === "servicePlan"
        ? {
            scope: "SERVICE_PLAN" as const,
            cluster_id: consumer.clusterId,
            expected_revision: revision,
          }
        : { scope: "SERVICE" as const, cluster_id: consumer.clusterId };
    const response: ManagedDependencyPlanPreviewResponse =
      await ServiceDependenciesApi.previewPlan({
        consumer: planConsumer,
        selections,
        signal: controller.signal,
      });
    if (generation !== planGenerationRef.current
      || controller.signal.aborted
      || capturedScope !== scopeKeyRef.current) return;
    if (!Array.isArray(response?.items) || response.items.length !== managed.length) {
      throw new Error(t("managedDependencies.requestFailed"));
    }
    const byType = new Map(response.items.map((item) => [item.dependency_type, item]));
    const completedSelections = { ...nextSelections };
    for (const { type, choice } of managed) {
      const preview = byType.get(type);
      if (!preview) throw new Error(t("managedDependencies.requestFailed"));
      completedSelections[type] = {
        ...choice,
        preview,
        planningIssue: undefined,
      };
    }
    await onPlanSelectionChange!(completedSelections);
  };

  const chooseLocal = async (type: ManagedDependencyType) => {
    if (onPlanSelectionChange) {
      const generation = ++planGenerationRef.current;
      planControllerRef.current?.abort();
      const controller = new AbortController();
      planControllerRef.current = controller;
      const capturedScope = scopeKey;
      const nextSelections = {
        ...selections,
        [type]: { mode: "local" as const },
      };
      setPreviewing(type);
      setSelectionFailure(null);
      try {
        await previewWholeSelection(nextSelections, capturedScope, generation, controller);
      } catch (error: any) {
        if (controller.signal.aborted || generation !== planGenerationRef.current
          || capturedScope !== scopeKeyRef.current) return;
        setSelectionFailure({
          message: errorDetails(error, t("managedDependencies.requestFailed")).message,
          retry: () => void chooseLocal(type),
        });
      } finally {
        if (generation === planGenerationRef.current
          && capturedScope === scopeKeyRef.current) setPreviewing(null);
      }
      return;
    }
    const generation = selectionGenerationRef.current[type] + 1;
    selectionGenerationRef.current[type] = generation;
    selectionControllersRef.current[type]?.abort();
    const capturedScope = scopeKey;
    setPreviewing((current) => current === type ? null : current);
    setSelectionFailure(null);
    try {
      await onSelectionChange(type, { mode: "local" });
    } catch (error: any) {
      if (generation !== selectionGenerationRef.current[type]
        || capturedScope !== scopeKeyRef.current) return;
      setSelectionFailure({
        message: errorDetails(error, t("managedDependencies.requestFailed")).message,
        retry: () => void chooseLocal(type),
      });
    }
  };

  const chooseProvider = async (
    type: ManagedDependencyType,
    provider: ManagedDependencyCandidate,
  ) => {
    if (onPlanSelectionChange) {
      const generation = ++planGenerationRef.current;
      planControllerRef.current?.abort();
      const controller = new AbortController();
      planControllerRef.current = controller;
      const capturedScope = scopeKey;
      setSelectionFailure(null);
      setPreviewing(type);
      const nextSelections = {
        ...selections,
        [type]: { mode: "managed" as const, provider },
      };
      try {
        await previewWholeSelection(nextSelections, capturedScope, generation, controller);
      } catch (error: any) {
        if (controller.signal.aborted || generation !== planGenerationRef.current
          || capturedScope !== scopeKeyRef.current) return;
        const issue = errorDetails(error, t("managedDependencies.requestFailed"));
        if (isIncompleteDependencyPlan(issue)) {
          const incompleteSelections = { ...nextSelections };
          dependencyTypes.forEach((dependencyType) => {
            const choice = incompleteSelections[dependencyType];
            if (choice?.mode === "managed") {
              incompleteSelections[dependencyType] = {
                ...choice,
                planningIssue: issue,
                preview: undefined,
              };
            }
          });
          try {
            await onPlanSelectionChange(incompleteSelections);
          } catch (saveError: any) {
            if (controller.signal.aborted || generation !== planGenerationRef.current
              || capturedScope !== scopeKeyRef.current) return;
            setSelectionFailure({
              message: errorDetails(saveError, t("managedDependencies.requestFailed")).message,
              retry: () => void chooseProvider(type, provider),
            });
          }
        } else {
          setSelectionFailure({
            message: issue.message,
            retry: () => void chooseProvider(type, provider),
          });
        }
      } finally {
        if (generation === planGenerationRef.current
          && capturedScope === scopeKeyRef.current) setPreviewing(null);
      }
      return;
    }
    const generation = selectionGenerationRef.current[type] + 1;
    selectionGenerationRef.current[type] = generation;
    selectionControllersRef.current[type]?.abort();
    const controller = new AbortController();
    selectionControllersRef.current[type] = controller;
    const capturedScope = scopeKey;
    setSelectionFailure(null);
    setPreviewing(type);
    const existingChoice = selections[type];
    const existingBindingId = existingChoice?.mode === "managed"
      && existingChoice.provider
      && providerIdentity(existingChoice.provider) === providerIdentity(provider)
      ? existingChoice.preview?.binding_id
      : undefined;
    const pendingChoice: ManagedDependencyChoice = { mode: "managed", provider };
    try {
      const revision = await onSelectionChange(type, pendingChoice);
      if (generation !== selectionGenerationRef.current[type]
        || capturedScope !== scopeKeyRef.current) return;
      const preview = consumer.kind === "draft"
        ? await ServiceDependenciesApi.previewDraft({
            ...(existingBindingId ? { bindingId: existingBindingId } : {}),
            dependencyType: type,
            draftId: consumer.draftId,
            expectedRevision: revision,
            provider: { cluster_id: provider.cluster_id, service_name: provider.service_name },
            signal: controller.signal,
          })
        : consumer.kind === "service"
          ? await ServiceDependenciesApi.previewService(
              consumer.clusterName,
              type,
              { cluster_id: provider.cluster_id, service_name: provider.service_name },
              controller.signal,
              existingBindingId,
            )
          : await ServiceDependenciesApi.previewServicePlan({
              ...(existingBindingId ? { bindingId: existingBindingId } : {}),
              clusterId: consumer.clusterId,
              dependencyType: type,
              expectedRevision: revision,
              provider: { cluster_id: provider.cluster_id, service_name: provider.service_name },
              signal: controller.signal,
            });
      if (generation !== selectionGenerationRef.current[type]
        || capturedScope !== scopeKeyRef.current) return;
      await onSelectionChange(type, {
        mode: "managed",
        provider,
        preview,
      });
    } catch (error: any) {
      if (controller.signal.aborted || generation !== selectionGenerationRef.current[type]
        || capturedScope !== scopeKeyRef.current) return;
      const issue = errorDetails(error, t("managedDependencies.requestFailed"));
      if (isIncompleteDependencyPlan(issue)) {
        try {
          await onSelectionChange(type, {
            mode: "managed",
            planningIssue: issue,
            provider,
          });
        } catch (saveError: any) {
          if (controller.signal.aborted || generation !== selectionGenerationRef.current[type]
            || capturedScope !== scopeKeyRef.current) return;
          setSelectionFailure({
            message: errorDetails(
              saveError,
              t("managedDependencies.requestFailed"),
            ).message,
            retry: () => void chooseProvider(type, provider),
          });
        }
      } else {
        setSelectionFailure({
          message: issue.message,
          retry: () => void chooseProvider(type, provider),
        });
      }
    } finally {
      if (generation === selectionGenerationRef.current[type]
        && capturedScope === scopeKeyRef.current) {
        setPreviewing(null);
      }
    }
  };

  const normalizedFilter = filter.trim().toLocaleLowerCase();
  const filteredCandidates = useMemo(() => Object.fromEntries(
    dependencyTypes.map((type) => [
      type,
      candidateState[type].items.filter((candidate) => [
        candidate.cluster_name,
        candidate.service_name,
        candidate.version?.service_version,
        candidate.version?.stack_version,
      ].some((value) => String(value || "").toLocaleLowerCase().includes(normalizedFilter))),
    ]),
  ) as Record<ManagedDependencyType, ManagedDependencyCandidate[]>, [candidateState, normalizedFilter]);

  return (
    <section aria-labelledby="managed-dependency-heading" className="mt-4">
      <h4 className="step-title" id="managed-dependency-heading">
        {t("managedDependencies.heading")}
      </h4>
      <p className="step-description">{t("managedDependencies.description")}</p>
      <Form.Group className="mb-3" controlId="managed-dependency-search">
        <Form.Label>{t("managedDependencies.search")}</Form.Label>
        <Form.Control
          onChange={(event) => setFilter(event.target.value)}
          placeholder={t("managedDependencies.searchPlaceholder")}
          type="search"
          value={filter}
        />
      </Form.Group>
      {selectionFailure ? (
        <Alert variant="danger">
          {selectionFailure.message}{" "}
          <Button onClick={selectionFailure.retry} size="sm" variant="outline-danger">
            {t("common.retry")}
          </Button>
        </Alert>
      ) : null}
      <div className="row g-3">
        {dependencyTypes.map((type) => {
          const choice = selections[type] || { mode: "local" };
          const state = candidateState[type];
          return (
            <div className="col-12 col-xl-6" key={type}>
              <fieldset className="border rounded p-3 h-100">
                <legend className="fs-6 fw-semibold float-none w-auto px-1">
                  {t(`managedDependencies.${type.toLocaleLowerCase()}.label`)}
                </legend>
                <Form.Check
                  checked={choice.mode === "local"}
                  disabled={previewing !== null && previewing !== type}
                  id={`dependency-${type}-local`}
                  label={t("managedDependencies.local")}
                  name={`dependency-${type}`}
                  onChange={() => void chooseLocal(type)}
                  type="radio"
                />
                <div className="text-body-secondary small mb-2 ms-4">
                  {t("managedDependencies.localDescription")}
                </div>
                {state.loading ? (
                  <div aria-live="polite" className="small text-body-secondary">
                    <Spinner animation="border" className="me-2" size="sm" />
                    {t("managedDependencies.loading")}
                  </div>
                ) : state.error ? (
                  <Alert className="py-2" variant="warning">
                    {state.error}{" "}
                    <Button onClick={() => void loadCandidates()} size="sm" variant="outline-warning">
                      {t("common.retry")}
                    </Button>
                  </Alert>
                ) : filteredCandidates[type].length ? (
                  <div className="d-grid gap-2">
                    {filteredCandidates[type].map((candidate) => {
                      const identity = providerIdentity(candidate);
                      const selected = choice.mode === "managed"
                        && choice.provider
                        && providerIdentity(choice.provider) === identity;
                      const selectable = candidateCanBePlanned(candidate);
                      return (
                        <div className="border-top pt-2" key={identity}>
                          <Form.Check
                            checked={Boolean(selected)}
                            disabled={!selectable || (previewing !== null && previewing !== type)}
                            id={`dependency-${type}-${candidate.cluster_id}`}
                            label={`${candidate.cluster_name || t("managedDependencies.unknownCluster")} / ${candidate.service_name}`}
                            name={`dependency-${type}`}
                            onChange={() => void chooseProvider(type, candidate)}
                            type="radio"
                          />
                          <div className="small ms-4 text-body-secondary">
                            {candidate.version?.service_version
                              ? t("managedDependencies.version", { version: candidate.version.service_version })
                              : t("managedDependencies.versionUnavailable")}
                            {candidate.security_mode ? ` | ${candidate.security_mode}` : ""}
                            {candidate.healthy === false ? (
                              <Badge bg="warning" className="ms-2" text="dark">
                                {t("managedDependencies.unhealthy")}
                              </Badge>
                            ) : null}
                          </div>
                          {!candidate.compatible && candidate.errors.length ? (
                            <div className="small ms-4 text-warning-emphasis">
                              {candidate.errors[0].message}
                            </div>
                          ) : null}
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <div className="small text-body-secondary">
                    {normalizedFilter
                      ? t("managedDependencies.noMatches")
                      : t("managedDependencies.noCandidates")}
                  </div>
                )}
                {previewing === type ? (
                  <div aria-live="polite" className="small mt-2">
                    <Spinner animation="border" className="me-2" size="sm" />
                    {t("managedDependencies.reviewing")}
                  </div>
                ) : null}
                {choice.mode === "managed" && choice.preview?.compatible ? (
                  <Alert className="mt-2 mb-0 py-2" variant="success">
                    {t("managedDependencies.reviewed", {
                      cluster: choice.provider?.cluster_name,
                    })}
                  </Alert>
                ) : null}
                {choice.mode === "managed" && choice.provider
                  && choice.preview && !choice.preview.compatible ? (
                    <Alert className="mt-2 mb-0 py-2" variant="warning">
                      {choice.preview.errors?.length ? (
                        <ul className="mb-2 ps-3">
                          {choice.preview.errors.map((issue) => (
                            <li key={`${issue.code}:${issue.message}`}>{issue.message}</li>
                          ))}
                        </ul>
                      ) : (
                        <div className="mb-2">{t("managedDependencies.incompatible")}</div>
                      )}
                      <Button
                        onClick={() => void chooseProvider(type, choice.provider!)}
                        size="sm"
                        variant="outline-warning"
                      >
                        {t("managedDependencies.reviewAgain")}
                      </Button>
                    </Alert>
                  ) : null}
                {choice.mode === "managed" && choice.planningIssue ? (
                  <Alert className="mt-2 mb-0 py-2" variant="info">
                    {choice.planningIssue.message}
                    <div className="small mt-1">
                      {choice.planningIssue.code === "DEPENDENCY_DRAFT_VERSION_UNMATERIALIZED"
                        ? t("managedDependencies.finishVersion")
                        : t("managedDependencies.finishSecurity")}
                    </div>
                  </Alert>
                ) : null}
              </fieldset>
            </div>
          );
        })}
      </div>
    </section>
  );
}
