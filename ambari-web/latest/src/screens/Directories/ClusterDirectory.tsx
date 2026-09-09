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
import { faPlus, faSort } from "@fortawesome/free-solid-svg-icons";
import { Link, useLocation, useSearchParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { AppContext } from "../../store/context";
import { useAuth } from "../../hooks/useAuth";
import { clusterPath, safeLegacyMainPath } from "../../Utils/clusterRoute";
import { clusterDraftPath } from "../../Utils/scopedWorkflow";
import DirectoryPagination from "./DirectoryPagination";
import {
  authorizedDirectoryClusters,
  clusterCreationDraftPhaseKey,
} from "./directoryUtils";
import DirectorySortControls from "./DirectorySortControls";
import WorkflowStateApi, {
  type ClusterCreationDraftSummary,
} from "../../api/workflowStateApi";

const PAGE_SIZE = 10;

export default function ClusterDirectory() {
  const { t } = useTranslation();
  const { availableClusters } = useContext(AppContext);
  const {
    canAccessCluster,
    canViewClusterTasks,
    hasClusterAuthorization,
    hasGlobalAuthorization,
  } = useAuth();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const [createPath] = useState(() => clusterDraftPath());
  const [creationDrafts, setCreationDrafts] = useState<ClusterCreationDraftSummary[]>([]);
  const [draftLoadError, setDraftLoadError] = useState(false);
  const [draftsLoading, setDraftsLoading] = useState(false);
  const [draftReload, setDraftReload] = useState(0);
  const draftLoadGeneration = useRef(0);
  const canCreateCluster = hasGlobalAuthorization("AMBARI.ADD_DELETE_CLUSTERS");
  const query = searchParams.get("q") || "";
  const sort = searchParams.get("sort") === "version" || searchParams.get("sort") === "state"
    ? searchParams.get("sort") as "version" | "state"
    : "name";
  const direction = searchParams.get("direction") === "desc" ? "desc" : "asc";
  const requestedPage = Number(searchParams.get("page"));
  const page = Number.isInteger(requestedPage) && requestedPage > 0 ? requestedPage : 1;
  const continuation = safeLegacyMainPath(searchParams.get("continue"));

  useEffect(() => {
    const generation = ++draftLoadGeneration.current;
    if (!canCreateCluster) {
      setCreationDrafts([]);
      setDraftLoadError(false);
      setDraftsLoading(false);
      return;
    }
    setDraftsLoading(true);
    setDraftLoadError(false);
    void WorkflowStateApi.getCreationDrafts().then((items) => {
      if (draftLoadGeneration.current !== generation) return;
      setCreationDrafts(items);
      setDraftsLoading(false);
    }, () => {
      if (draftLoadGeneration.current !== generation) return;
      setCreationDrafts([]);
      setDraftLoadError(true);
      setDraftsLoading(false);
    });
    return () => {
      if (draftLoadGeneration.current === generation) draftLoadGeneration.current += 1;
    };
  }, [canCreateCluster, draftReload]);

  const retryDrafts = useCallback(() => setDraftReload((value) => value + 1), []);

  const updateQuery = (updates: Record<string, string | number | undefined>) => {
    const next = new URLSearchParams(searchParams);
    Object.entries(updates).forEach(([key, value]) => {
      if (value === undefined || value === "") next.delete(key);
      else next.set(key, String(value));
    });
    setSearchParams(next, { replace: true });
  };

  const clusters = useMemo(() => authorizedDirectoryClusters(
    availableClusters,
    canAccessCluster,
  ).filter((cluster) => cluster.clusterName.toLowerCase().includes(query.trim().toLowerCase()))
    .sort((left, right) => {
      const leftValue = sort === "name" ? left.clusterName
        : sort === "version" ? left.version : left.provisioningState;
      const rightValue = sort === "name" ? right.clusterName
        : sort === "version" ? right.version : right.provisioningState;
      const comparison = leftValue.localeCompare(rightValue);
      return direction === "asc" ? comparison : -comparison;
    }), [availableClusters, canAccessCluster, direction, query, sort]);
  const maxPage = Math.max(1, Math.ceil(clusters.length / PAGE_SIZE));
  const currentPage = Math.min(page, maxPage);
  const visibleClusters = clusters.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  );
  const sortBy = (column: "name" | "version" | "state") => updateQuery({
    direction: sort === column && direction === "asc" ? "desc" : "asc",
    page: undefined,
    sort: column === "name" ? undefined : column,
  });
  const setSort = (column: "name" | "version" | "state") => updateQuery({
    page: undefined,
    sort: column === "name" ? undefined : column,
  });
  const sortDirection = (column: "name" | "version" | "state") => (
    sort === column ? (direction === "asc" ? "ascending" : "descending") : "none"
  );
  return (
    <main className="directory-page container-fluid px-3 px-md-4 py-4">
      <div className="d-flex flex-wrap align-items-end justify-content-between gap-3 mb-3">
        <div>
          <h1 className="h3 mb-1">{t("directory.clusters")}</h1>
          <p className="text-body-secondary mb-0">{t("directory.clustersSubtitle")}</p>
        </div>
        {canCreateCluster ? (
          <Button as={Link as any} to={createPath} size="sm">
            <FontAwesomeIcon icon={faPlus} className="me-2" />
            {t("directory.createCluster")}
          </Button>
        ) : null}
      </div>

      {canCreateCluster && (draftsLoading || draftLoadError || creationDrafts.length) ? (
        <section aria-labelledby="cluster-drafts-heading" className="directory-drafts mb-4">
          <div className="d-flex align-items-center justify-content-between gap-3 mb-2">
            <h2 className="h5 mb-0" id="cluster-drafts-heading">
              {t("directory.resumeInstallations")}
            </h2>
            {draftsLoading ? (
              <span aria-live="polite" className="text-body-secondary small">
                {t("directory.loadingDrafts")}
              </span>
            ) : null}
          </div>
          {draftLoadError ? (
            <Alert className="mb-0" variant="warning">
              {t("directory.draftLoadFailed")}{" "}
              <Button onClick={retryDrafts} size="sm" variant="outline-warning">
                {t("directory.retry")}
              </Button>
            </Alert>
          ) : creationDrafts.length ? (
            <div className="list-group">
              {creationDrafts.map((draft) => (
                <div
                  className="list-group-item d-flex flex-wrap align-items-center justify-content-between gap-2"
                  key={draft.draft_id}
                >
                  <div className="min-w-0">
                    <div className="fw-semibold text-break">
                      {draft.cluster_name || t("directory.unnamedInstallation")}
                    </div>
                    <span className="text-body-secondary small">
                      {t(clusterCreationDraftPhaseKey(draft.phase))}
                    </span>
                  </div>
                  <Button
                    as={Link as any}
                    size="sm"
                    to={clusterDraftPath(draft.draft_id)}
                    variant="outline-primary"
                  >
                    {t("common.resume")}
                  </Button>
                </div>
              ))}
            </div>
          ) : null}
        </section>
      ) : null}

      <div className="directory-toolbar mb-3">
        <label className="form-label" htmlFor="cluster-directory-search">
          {t("directory.searchClusters")}
        </label>
        <input
          className="form-control"
          id="cluster-directory-search"
          onChange={(event) => updateQuery({ q: event.target.value, page: undefined })}
          placeholder={t("directory.clusterName")}
          type="search"
          value={query}
        />
      </div>

      <DirectorySortControls
        direction={direction}
        onDirectionChange={() => updateQuery({
          direction: direction === "asc" ? "desc" : undefined,
          page: undefined,
        })}
        onSortChange={setSort}
        options={[
          { label: t("directory.cluster"), value: "name" },
          { label: t("directory.stackVersion"), value: "version" },
          { label: t("directory.installationState"), value: "state" },
        ]}
        sort={sort}
      />

      {visibleClusters.length ? (
        <div className="table-responsive directory-table-wrap">
          <table className="table table-hover align-middle directory-table">
            <thead>
              <tr>
                <th aria-sort={sortDirection("name")} scope="col">
                  <button className="directory-sort" onClick={() => sortBy("name")}>
                    {t("directory.cluster")} <FontAwesomeIcon icon={faSort} />
                  </button>
                </th>
                <th aria-sort={sortDirection("version")} scope="col">
                  <button className="directory-sort" onClick={() => sortBy("version")}>
                    {t("directory.stackVersion")} <FontAwesomeIcon icon={faSort} />
                  </button>
                </th>
                <th aria-sort={sortDirection("state")} scope="col">
                  <button className="directory-sort" onClick={() => sortBy("state")}>
                    {t("directory.installationState")} <FontAwesomeIcon icon={faSort} />
                  </button>
                </th>
                <th scope="col" className="text-end">{t("directory.actions")}</th>
              </tr>
            </thead>
            <tbody>
              {visibleClusters.map((cluster) => {
                const installed = cluster.provisioningState === "INSTALLED";
                const canViewStatus = hasClusterAuthorization(
                  cluster.clusterName,
                  "CLUSTER.VIEW_STATUS_INFO",
                );
                const canViewHosts = canViewStatus || hasClusterAuthorization(
                  cluster.clusterName,
                  "HOST.VIEW_STATUS_INFO",
                );
                const canViewAlerts = hasClusterAuthorization(
                  cluster.clusterName,
                  "CLUSTER.VIEW_ALERTS",
                );
                const canOpenTasks = canViewClusterTasks(cluster.clusterName);
                return (
                  <tr key={cluster.clusterId}>
                    <td data-label={t("directory.cluster")}>
                      <Link className="fw-semibold" to={clusterPath(cluster.clusterName, continuation)}>
                        {cluster.clusterName}
                      </Link>
                    </td>
                    <td data-label={t("directory.stackVersion")}>{cluster.version || t("directory.unknown")}</td>
                    <td data-label={t("directory.installationState")}>
                      <Badge bg={installed ? "success" : "warning"} text={installed ? undefined : "dark"}>
                        {cluster.provisioningState}
                      </Badge>
                    </td>
                    <td data-label={t("directory.actions")}>
                      {installed ? (
                        <div className="directory-actions justify-content-md-end">
                          {canViewStatus ? (
                            <Link to={clusterPath(cluster.clusterName, "/main/dashboard/metrics")}>{t("directory.overview")}</Link>
                          ) : null}
                          {canViewHosts ? (
                            <Link to={clusterPath(cluster.clusterName, "/main/hosts")}>{t("directory.hosts")}</Link>
                          ) : null}
                          {canViewAlerts ? (
                            <Link to={clusterPath(cluster.clusterName, "/main/alerts")}>{t("directory.alerts")}</Link>
                          ) : null}
                          {canOpenTasks ? (
                            <Link
                              state={{ returnTo: `${location.pathname}${location.search}` }}
                              to={clusterPath(cluster.clusterName, "/main/requests")}
                            >
                              {t("directory.tasks")}
                            </Link>
                          ) : null}
                        </div>
                      ) : (
                        <span className="text-body-secondary">{t("directory.incompleteInstallation")}</span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      ) : (
        <Alert variant="info">
          {query ? t("directory.noClusterMatches") : t("directory.noAuthorizedClusters")}
        </Alert>
      )}
      <DirectoryPagination
        currentPage={currentPage}
        onPageChange={(nextPage) => updateQuery({ page: nextPage === 1 ? undefined : nextPage })}
        pageSize={PAGE_SIZE}
        totalItems={clusters.length}
      />
    </main>
  );
}
