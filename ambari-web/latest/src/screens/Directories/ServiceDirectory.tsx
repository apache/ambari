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

import { useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { Alert, Badge, Button } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faRotateRight, faSort } from "@fortawesome/free-solid-svg-icons";
import { Link, useLocation, useSearchParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { ServiceApi } from "../../api/serviceApi";
import ServiceDependenciesApi, {
  type ManagedDependencyBindingSummary,
} from "../../api/serviceDependenciesApi";
import { useAuth } from "../../hooks/useAuth";
import { AppContext } from "../../store/context";
import { clusterPath } from "../../Utils/clusterRoute";
import { dependencyPhaseTranslationKey } from "../../Utils/managedDependencyPresentation";
import DirectoryPagination from "./DirectoryPagination";
import {
  authorizedDirectoryClusters,
  DirectoryRequestCancelledError,
  DirectoryRequestLimiter,
  DirectoryCluster,
  ServiceDeploymentRow,
  serviceRowKey,
  serviceRowsForCluster,
} from "./directoryUtils";
import DirectorySortControls from "./DirectorySortControls";

const PAGE_SIZE = 10;
const REQUEST_CONCURRENCY = 4;

type ServiceLoadGeneration = {
  attempts: Record<string, number>;
  controller: AbortController;
  dependencyAttempts: Record<string, number>;
  dependencyLoads: Record<string, "error" | "loaded" | "loading" | "queued">;
  id: number;
  limiter: DirectoryRequestLimiter;
};

const dependencyRowKey = (cluster: Pick<DirectoryCluster, "clusterId" | "clusterName">) =>
  `${cluster.clusterId}:${cluster.clusterName}:HBASE`;

export default function ServiceDirectory() {
  const { t } = useTranslation();
  const { availableClusters } = useContext(AppContext);
  const {
    canAccessCluster,
    canViewClusterTasks,
    hasClusterAuthorization,
  } = useAuth();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const [rowsByCluster, setRowsByCluster] = useState<Record<string, ServiceDeploymentRow[]>>({});
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loadingClusters, setLoadingClusters] = useState<Record<string, boolean>>({});
  const generationCounterRef = useRef(0);
  const generationRef = useRef<ServiceLoadGeneration | null>(null);
  const visibleDependencyKeysRef = useRef<Set<string>>(new Set());

  const clusters = useMemo(() => authorizedDirectoryClusters(
    availableClusters,
    canAccessCluster,
  ).filter((cluster) => cluster.provisioningState === "INSTALLED"), [
    availableClusters,
    canAccessCluster,
  ]);
  const loadDependencySummary = useCallback(async (
    cluster: DirectoryCluster,
    generation: ServiceLoadGeneration,
    retry = false,
  ) => {
    const clusterKey = String(cluster.clusterId);
    const dependencyKey = dependencyRowKey(cluster);
    if (!retry && generation.dependencyLoads[dependencyKey]) return;
    const attempt = (generation.dependencyAttempts[clusterKey] || 0) + 1;
    generation.dependencyAttempts[clusterKey] = attempt;
    generation.dependencyLoads[dependencyKey] = "queued";
    const isCurrent = () => generationRef.current === generation
      && generation.dependencyAttempts[clusterKey] === attempt;
    const updateHBaseRow = (
      update: (row: ServiceDeploymentRow) => ServiceDeploymentRow,
    ) => setRowsByCluster((current) => ({
      ...current,
      [clusterKey]: (current[clusterKey] || []).map((row) =>
        row.clusterId === cluster.clusterId
          && row.clusterName === cluster.clusterName
          && row.serviceName === "HBASE"
          ? update(row)
          : row),
    }));
    updateHBaseRow((row) => ({
      ...row,
      dependencyLoadError: undefined,
      dependencyLoading: true,
    }));
    const operation = async () => {
      if (!isCurrent()) return;
      if (!visibleDependencyKeysRef.current.has(dependencyKey)) {
        delete generation.dependencyLoads[dependencyKey];
        updateHBaseRow((row) => ({ ...row, dependencyLoading: false }));
        return;
      }
      generation.dependencyLoads[dependencyKey] = "loading";
      try {
        const dependencySummaries = await ServiceDependenciesApi.list(
          cluster.clusterName,
          generation.controller.signal,
        );
        if (!isCurrent()) return;
        generation.dependencyLoads[dependencyKey] = "loaded";
        updateHBaseRow((row) => ({
          ...row,
          dependencyLoading: false,
          dependencySummaries,
        }));
      } catch (dependencyError: any) {
        if (!isCurrent()) return;
        generation.dependencyLoads[dependencyKey] = "error";
        updateHBaseRow((row) => ({
          ...row,
          dependencyLoadError: String(
            dependencyError?.response?.data?.message
              || t("directory.providerSummaryLoadFailed"),
          ),
          dependencyLoading: false,
        }));
      }
    };
    try {
      await generation.limiter.run(operation);
    } catch (error) {
      if (!(error instanceof DirectoryRequestCancelledError)) throw error;
    }
  }, [t]);

  const loadCluster = useCallback(async (
    cluster: DirectoryCluster,
    generation: ServiceLoadGeneration,
  ) => {
    try {
      const clusterKey = String(cluster.clusterId);
      const attempt = (generation.attempts[clusterKey] || 0) + 1;
      generation.attempts[clusterKey] = attempt;
      await generation.limiter.run(async () => {
        const isCurrent = () => generationRef.current === generation
          && generation.attempts[clusterKey] === attempt;
        try {
          const response = await ServiceApi.getAllServices(
            cluster.clusterName,
            generation.controller.signal,
          );
          if (!isCurrent()) return;
          const serviceRows = serviceRowsForCluster(cluster, response);
          setRowsByCluster((current) => ({
            ...current,
            [clusterKey]: serviceRows,
          }));
          setErrors((current) => {
            const next = { ...current };
            delete next[clusterKey];
            return next;
          });
          setLoadingClusters((current) => {
            const next = { ...current };
            delete next[clusterKey];
            return next;
          });
        } catch (error: any) {
          if (!isCurrent()) return;
          setRowsByCluster((current) => {
            const next = { ...current };
            delete next[clusterKey];
            return next;
          });
          const status = error?.response?.status;
          setErrors((current) => ({
            ...current,
            [clusterKey]: status === 403 || status === 404
              ? t("directory.clusterAccessChanged")
              : error?.response?.data?.message || t("directory.serviceLoadFailed"),
          }));
        } finally {
          if (!isCurrent()) return;
          setLoadingClusters((current) => {
            const next = { ...current };
            delete next[clusterKey];
            return next;
          });
        }
      });
    } catch (error) {
      if (!(error instanceof DirectoryRequestCancelledError)) {
        throw error;
      }
    }
  }, [t]);

  useEffect(() => {
    const generation: ServiceLoadGeneration = {
      attempts: {},
      controller: new AbortController(),
      dependencyAttempts: {},
      dependencyLoads: {},
      id: generationCounterRef.current + 1,
      limiter: new DirectoryRequestLimiter(REQUEST_CONCURRENCY),
    };
    generationCounterRef.current = generation.id;
    generationRef.current?.limiter.cancelPending();
    generationRef.current?.controller.abort();
    generationRef.current = generation;
    setRowsByCluster({});
    setErrors({});
    setLoadingClusters(Object.fromEntries(clusters.map((cluster) => [String(cluster.clusterId), true])));
    clusters.forEach((cluster) => {
      void loadCluster(cluster, generation);
    });
    return () => {
      generation.limiter.cancelPending();
      generation.controller.abort();
      if (generationRef.current === generation) {
        generationRef.current = null;
      }
    };
  }, [clusters, loadCluster]);

  const retryCluster = (clusterId: number) => {
    const cluster = clusters.find((item) => item.clusterId === clusterId);
    const generation = generationRef.current;
    if (!cluster || !generation) return;
    const clusterKey = String(clusterId);
    setLoadingClusters((current) => ({ ...current, [clusterKey]: true }));
    setErrors((current) => {
      const next = { ...current };
      delete next[clusterKey];
      return next;
    });
    void loadCluster(cluster, generation);
  };

  const retryDependencySummary = (clusterId: number) => {
    const cluster = clusters.find((item) => item.clusterId === clusterId);
    const generation = generationRef.current;
    if (!cluster || !generation
      || !visibleDependencyKeysRef.current.has(dependencyRowKey(cluster))) return;
    void loadDependencySummary(cluster, generation, true);
  };

  const query = searchParams.get("q") || "";
  const clusterFilter = searchParams.get("cluster") || "";
  const typeFilter = searchParams.get("type") || "";
  const sort = searchParams.get("sort") === "cluster" || searchParams.get("sort") === "state"
    ? searchParams.get("sort") as "cluster" | "state"
    : "service";
  const direction = searchParams.get("direction") === "desc" ? "desc" : "asc";
  const requestedPage = Number(searchParams.get("page"));
  const page = Number.isInteger(requestedPage) && requestedPage > 0 ? requestedPage : 1;
  const updateQuery = (updates: Record<string, string | number | undefined>) => {
    const next = new URLSearchParams(searchParams);
    Object.entries(updates).forEach(([key, value]) => {
      if (value === undefined || value === "") next.delete(key);
      else next.set(key, String(value));
    });
    setSearchParams(next, { replace: true });
  };

  const currentClusterNamesById = useMemo(() => new Map(
    clusters.map((cluster) => [cluster.clusterId, cluster.clusterName]),
  ), [clusters]);
  const allRows = useMemo(() => Object.values(rowsByCluster).flat().filter((row) => (
    currentClusterNamesById.get(row.clusterId) === row.clusterName
  )), [currentClusterNamesById, rowsByCluster]);
  const serviceTypes = useMemo(() => [...new Set(allRows.map((row) => row.serviceName))].sort(), [allRows]);
  const filteredRows = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    return allRows.filter((row) => (!clusterFilter || row.clusterName === clusterFilter)
      && (!typeFilter || row.serviceName === typeFilter)
      && (!normalizedQuery || `${row.serviceName} ${row.clusterName}`.toLowerCase().includes(normalizedQuery)))
      .sort((left, right) => {
        const leftValue = sort === "service" ? left.serviceName
          : sort === "cluster" ? left.clusterName : left.state;
        const rightValue = sort === "service" ? right.serviceName
          : sort === "cluster" ? right.clusterName : right.state;
        const comparison = leftValue.localeCompare(rightValue)
          || left.clusterId - right.clusterId
          || left.serviceName.localeCompare(right.serviceName);
        return direction === "asc" ? comparison : -comparison;
      });
  }, [allRows, clusterFilter, direction, query, sort, typeFilter]);
  const maxPage = Math.max(1, Math.ceil(filteredRows.length / PAGE_SIZE));
  const currentPage = Math.min(page, maxPage);
  const visibleRows = useMemo(() => filteredRows.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  ), [currentPage, filteredRows]);
  const visibleDependencyKeys = new Set(visibleRows
    .filter((row) => row.serviceName === "HBASE")
    .map(dependencyRowKey));
  visibleDependencyKeysRef.current = visibleDependencyKeys;
  useEffect(() => {
    const generation = generationRef.current;
    if (!generation) return;
    visibleRows.forEach((row) => {
      if (row.serviceName !== "HBASE") return;
      const cluster = clusters.find((item) => item.clusterId === row.clusterId
        && item.clusterName === row.clusterName);
      if (cluster) void loadDependencySummary(cluster, generation);
    });
  }, [clusters, loadDependencySummary, visibleRows]);
  const sortBy = (column: "service" | "cluster" | "state") => updateQuery({
    direction: sort === column && direction === "asc" ? "desc" : "asc",
    page: undefined,
    sort: column === "service" ? undefined : column,
  });
  const setSort = (column: "service" | "cluster" | "state") => updateQuery({
    page: undefined,
    sort: column === "service" ? undefined : column,
  });
  const sortDirection = (column: "service" | "cluster" | "state") => (
    sort === column ? (direction === "asc" ? "ascending" : "descending") : "none"
  );
  const currentClusterIds = new Set(clusters.map((cluster) => String(cluster.clusterId)));
  const visibleErrors = Object.entries(errors).filter(([clusterId]) => currentClusterIds.has(clusterId));
  const isLoading = Object.keys(loadingClusters).some((clusterId) => currentClusterIds.has(clusterId));
  const dependencyLabel = (summary: ManagedDependencyBindingSummary) => {
    const type = t(`serviceDependencies.${summary.dependency_type.toLocaleLowerCase()}.shortLabel`);
    const owner = summary.ownership === "managed"
      ? summary.provider?.cluster_name || t("serviceDependencies.unavailable")
      : t(`serviceDependencies.ownership.${summary.ownership || "unknown"}`);
    return { owner, type };
  };
  return (
    <main className="directory-page container-fluid px-3 px-md-4 py-4">
      <div className="mb-3">
        <h1 className="h3 mb-1">{t("directory.services")}</h1>
        <p className="text-body-secondary mb-0">{t("directory.servicesSubtitle")}</p>
      </div>

      <div className="directory-filters mb-3">
        <div>
          <label className="form-label" htmlFor="service-directory-search">{t("directory.searchServices")}</label>
          <input
            className="form-control"
            id="service-directory-search"
            onChange={(event) => updateQuery({ q: event.target.value, page: undefined })}
            placeholder={t("directory.serviceOrCluster")}
            type="search"
            value={query}
          />
        </div>
        <div>
          <label className="form-label" htmlFor="service-cluster-filter">{t("directory.cluster")}</label>
          <select
            className="form-select"
            id="service-cluster-filter"
            onChange={(event) => updateQuery({ cluster: event.target.value, page: undefined })}
            value={clusterFilter}
          >
            <option value="">{t("directory.allClusters")}</option>
            {clusters.map((cluster) => (
              <option key={cluster.clusterId} value={cluster.clusterName}>{cluster.clusterName}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="form-label" htmlFor="service-type-filter">{t("directory.serviceType")}</label>
          <select
            className="form-select"
            id="service-type-filter"
            onChange={(event) => updateQuery({ page: undefined, type: event.target.value })}
            value={typeFilter}
          >
            <option value="">{t("directory.allServiceTypes")}</option>
            {serviceTypes.map((serviceName) => (
              <option key={serviceName} value={serviceName}>{serviceName}</option>
            ))}
          </select>
        </div>
      </div>

      <DirectorySortControls
        direction={direction}
        onDirectionChange={() => updateQuery({
          direction: direction === "asc" ? "desc" : undefined,
          page: undefined,
        })}
        onSortChange={setSort}
        options={[
          { label: t("directory.service"), value: "service" },
          { label: t("directory.owningCluster"), value: "cluster" },
          { label: t("directory.status"), value: "state" },
        ]}
        sort={sort}
      />

      {visibleErrors.map(([clusterId, message]) => {
        const cluster = clusters.find((item) => String(item.clusterId) === clusterId);
        if (!cluster) return null;
        return (
          <Alert className="d-flex flex-wrap align-items-center justify-content-between gap-2" key={clusterId} variant="warning">
            <span><strong>{cluster.clusterName}</strong>: {message}</span>
            <Button
              aria-label={t("directory.retryCluster", { cluster: cluster.clusterName })}
              disabled={Boolean(loadingClusters[clusterId])}
              onClick={() => retryCluster(cluster.clusterId)}
              size="sm"
              variant="outline-warning"
            >
              <FontAwesomeIcon icon={faRotateRight} className="me-2" />
              {t("directory.retry")}
            </Button>
          </Alert>
        );
      })}

      {isLoading ? (
        <div aria-live="polite" className="text-body-secondary mb-2">
          {allRows.length ? t("directory.loadingRemainingServices") : t("directory.loadingServices")}
        </div>
      ) : null}

      {visibleRows.length ? (
        <div className="table-responsive directory-table-wrap">
          <table className="table table-hover align-middle directory-table">
            <thead>
              <tr>
                <th aria-sort={sortDirection("service")} scope="col">
                  <button className="directory-sort" onClick={() => sortBy("service")}>
                    {t("directory.service")} <FontAwesomeIcon icon={faSort} />
                  </button>
                </th>
                <th aria-sort={sortDirection("cluster")} scope="col">
                  <button className="directory-sort" onClick={() => sortBy("cluster")}>
                    {t("directory.owningCluster")} <FontAwesomeIcon icon={faSort} />
                  </button>
                </th>
                <th aria-sort={sortDirection("state")} scope="col">
                  <button className="directory-sort" onClick={() => sortBy("state")}>
                    {t("directory.status")} <FontAwesomeIcon icon={faSort} />
                  </button>
                </th>
                <th scope="col" className="text-end">{t("directory.actions")}</th>
              </tr>
            </thead>
            <tbody>
              {visibleRows.map((row) => (
                <tr key={serviceRowKey(row)}>
                  <td data-label={t("directory.service")}>
                    <div className="directory-row-content">
                      <Link className="fw-semibold" to={clusterPath(row.clusterName, `/main/services/${row.serviceName}/summary`)}>
                        {row.serviceName}
                      </Link>
                      {row.maintenanceState !== "OFF" ? (
                        <Badge bg="warning" className="ms-2" text="dark">{t("directory.maintenance")}</Badge>
                      ) : null}
                      {row.serviceName === "HBASE" && row.dependencySummaries?.length ? (
                        <div className="small text-body-secondary mt-1 text-break">
                          {row.dependencySummaries.map((summary) => {
                            const { owner, type } = dependencyLabel(summary);
                            return (
                              <div key={summary.dependency_type}>
                                <span>{type}: {owner}</span>
                                {summary.phase ? (
                                  <span className="ms-1">
                                    ({t(dependencyPhaseTranslationKey(summary.phase))})
                                  </span>
                                ) : null}
                              </div>
                            );
                          })}
                        </div>
                      ) : null}
                      {row.dependencyLoading ? (
                        <div aria-live="polite" className="small text-body-secondary mt-1">
                          {t("directory.loadingProviderSummary")}
                        </div>
                      ) : null}
                      {row.dependencyLoadError ? (
                        <div className="small text-warning mt-1">
                          {row.dependencyLoadError}{" "}
                          <Button
                            className="p-0 align-baseline"
                            onClick={() => retryDependencySummary(row.clusterId)}
                            size="sm"
                            variant="link"
                          >
                            {t("directory.retryProviderSummary")}
                          </Button>
                        </div>
                      ) : null}
                    </div>
                  </td>
                  <td data-label={t("directory.owningCluster")}>
                    <Link to={clusterPath(row.clusterName, "/main/dashboard/metrics")}>{row.clusterName}</Link>
                  </td>
                  <td data-label={t("directory.status")}>
                    <Badge bg={row.state === "STARTED" ? "success" : "secondary"}>{row.state}</Badge>
                  </td>
                  <td data-label={t("directory.actions")}>
                    <div className="directory-actions justify-content-md-end">
                      <Link to={clusterPath(row.clusterName, `/main/services/${row.serviceName}/summary`)}>{t("directory.summary")}</Link>
                      {row.serviceName === "HBASE" ? (
                        <Link
                          state={{ returnTo: `${location.pathname}${location.search}` }}
                          to={clusterPath(row.clusterName, "/main/services/HBASE/dependencies")}
                        >
                          {t("serviceDependencies.tab")}
                        </Link>
                      ) : null}
                      {hasClusterAuthorization(row.clusterName, "CLUSTER.VIEW_CONFIGS") ? (
                        <Link to={clusterPath(row.clusterName, `/main/services/${row.serviceName}/configs`)}>{t("directory.config")}</Link>
                      ) : null}
                      {canViewClusterTasks(row.clusterName) ? (
                        <Link
                          state={{ returnTo: `${location.pathname}${location.search}` }}
                          to={clusterPath(row.clusterName, "/main/requests")}
                        >
                          {t("directory.tasks")}
                        </Link>
                      ) : null}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : !isLoading ? (
        <Alert variant="info">
          {allRows.length ? t("directory.noServiceMatches") : t("directory.noServices")}
        </Alert>
      ) : null}
      <DirectoryPagination
        currentPage={currentPage}
        onPageChange={(nextPage) => updateQuery({ page: nextPage === 1 ? undefined : nextPage })}
        pageSize={PAGE_SIZE}
        totalItems={filteredRows.length}
      />
    </main>
  );
}
