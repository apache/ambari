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

import { useContext, useEffect, useRef, useState } from "react";
import { Alert, Button, Spinner } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faShareNodes } from "@fortawesome/free-solid-svg-icons";
import toast from "react-hot-toast";
import MetricsApi from "../../../api/metricsApi";
import { AppContext } from "../../../store/context";
import {
  DASHBOARD_SCHEMA_VERSION,
  DashboardPanel as Panel,
  DashboardTarget,
  Datasource,
  PrometheusResult,
} from "../types";
import {
  normalizePrometheusResults,
  replaceDashboardVariables,
  resolvePanelDatasourceId,
  withDashboardBuiltIns,
} from "../utils";
import PanelRenderer from "./PanelRenderer";
import type { DashboardPanelResult } from "./data/panelData";

interface DashboardPanelProps {
  panel: Panel;
  start: number;
  end: number;
  refreshKey: number;
  variables: Record<string, string | number | string[]>;
  datasources: Datasource[];
  allowShare?: boolean;
  panelHeight?: number;
  graphTooltip?: string;
}

const QUERY_BATCH_LIMIT = 64;
const INSTANT_PANEL_TYPES = new Set(["stat", "gauge", "barGauge", "pie", "barchart", "tableNG", "hexbin"]);

const defaultLabel = (result: PrometheusResult, fallback: string) => {
  const entries = Object.entries(result.metric).filter(([name]) => name !== "__name__");
  return entries.length ? entries.map(([name, value]) => `${name}=${value}`).join(", ") : result.metric.__name__ || fallback;
};

const resolveTargetLegend = (
  target: DashboardTarget,
  result: PrometheusResult,
  variables: Record<string, string | number | string[]>,
  fallback: string,
) => {
  const legend = replaceDashboardVariables(target.legend || "", variables)
    .replace(/\{\{\s*([^{}]+?)\s*\}\}/g, (_match, labelName: string) => result.metric[labelName] || "")
    .trim();
  return legend || defaultLabel(result, target.refId || fallback);
};

const seriesKey = (
  target: DashboardTarget,
  targetIndex: number,
  result: PrometheusResult,
  resultIndex: number,
) => {
  const metricKey = Object.entries(result.metric)
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([name, value]) => `${name}=${value}`)
    .join(",");
  return `${target.refId || targetIndex}:${metricKey}:${resultIndex}`;
};

export default function DashboardPanel({
  panel,
  start,
  end,
  refreshKey,
  variables,
  datasources,
  allowShare = true,
  panelHeight,
  graphTooltip,
}: DashboardPanelProps) {
  const { clusterName } = useContext(AppContext);
  const [results, setResults] = useState<DashboardPanelResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [sharing, setSharing] = useState(false);
  const [error, setError] = useState("");
  const [retryKey, setRetryKey] = useState(0);
  const panelRef = useRef<HTMLElement>(null);
  const requestSequence = useRef(0);
  const requestController = useRef<AbortController | null>(null);
  const [inViewport, setInViewport] = useState(true);
  const type = panel.type;
  const isStatic = type === "row" || type === "text" || type === "iframe";
  const panelCategory = panel.datasourceCate || "prometheus";
  const selectedDatasourceId = resolvePanelDatasourceId(panel, variables, datasources);

  useEffect(() => {
    const element = panelRef.current;
    if (!element || typeof IntersectionObserver === "undefined") return undefined;
    const observer = new IntersectionObserver(
      (entries) => setInViewport(entries.some((entry) => entry.isIntersecting)),
      { rootMargin: "320px" },
    );
    observer.observe(element);
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    const sequence = requestSequence.current + 1;
    requestSequence.current = sequence;
    requestController.current?.abort();
    requestController.current = null;
    if (isStatic || !inViewport) return undefined;
    const targets = (Array.isArray(panel.targets) ? panel.targets : []).filter((target) => !target.hide
      && typeof target.expr === "string" && target.expr.trim());
    if (panelCategory !== "prometheus" || !selectedDatasourceId || targets.length === 0) {
      setResults([]);
      setError(panelCategory !== "prometheus"
        ? `${panelCategory} panel rendering is not available yet`
        : "Panel has no queryable Prometheus target");
      return undefined;
    }
    const controller = new AbortController();
    requestController.current = controller;
    setLoading(true);
    setError("");
    const duration = Math.max(end - start, 1);
    const panelWidth = panelRef.current?.clientWidth || 800;
    const measuredPoints = Math.min(2400, Math.max(120, Math.floor(panelWidth / 4)));
    const maxDataPoints = targets.reduce((limit, target) => (
      typeof target.maxDataPoints === "number" && target.maxDataPoints > 0
        ? Math.min(limit, target.maxDataPoints)
        : limit
    ), measuredPoints);
    const step = Math.max(Math.ceil(duration / maxDataPoints), 1);
    const effectiveVariables = withDashboardBuiltIns(variables, clusterName, step);
    const displayVariables = {
      ...effectiveVariables,
      cluster: clusterName || "",
    };
    const grouped = new Map<"instant" | "range", Array<{ targetIndex: number; query: string }>>();
    targets.forEach((target, targetIndex) => {
      const mode = target.instant || INSTANT_PANEL_TYPES.has(type) ? "instant" : "range";
      const list = grouped.get(mode) || [];
      list.push({ targetIndex, query: replaceDashboardVariables(target.expr, effectiveVariables) });
      grouped.set(mode, list);
    });
    const batches = Array.from(grouped.entries()).flatMap(([mode, items]) => {
      const chunks: Array<{ mode: "instant" | "range"; items: typeof items }> = [];
      for (let index = 0; index < items.length; index += QUERY_BATCH_LIMIT) {
        chunks.push({ mode, items: items.slice(index, index + QUERY_BATCH_LIMIT) });
      }
      return chunks;
    });
    void Promise.allSettled(batches.map(({ mode, items }) => {
      const queries = items.map(({ query, targetIndex }) => mode === "instant"
        ? { query, refId: targets[targetIndex].refId, time: end }
        : { query, refId: targets[targetIndex].refId, start, end, step });
      return mode === "instant"
        ? MetricsApi.queryInstantBatch(selectedDatasourceId, queries, controller.signal)
        : MetricsApi.queryRangeBatch(selectedDatasourceId, queries, controller.signal);
    })).then((responses) => {
      if (sequence !== requestSequence.current || controller.signal.aborted) return;
      const values: DashboardPanelResult[] = [];
      let failures = 0;
      responses.forEach((response, batchIndex) => {
        const batch = batches[batchIndex];
        if (response.status !== "fulfilled" || !Array.isArray(response.value.data)) {
          failures += batch.items.length;
          return;
        }
        batch.items.forEach(({ targetIndex }, itemIndex) => {
          const target = targets[targetIndex];
          const item = response.value.data?.[itemIndex];
          if (!item || item.status !== "success" || !Array.isArray(item.result)) {
            failures += 1;
            return;
          }
          normalizePrometheusResults(item.result).forEach((result, resultIndex) => {
              values.push({
                ...result,
                displayName: resolveTargetLegend(
                  target,
                  result,
                  displayVariables,
                  `Series ${values.length + 1}`,
                ),
                seriesKey: seriesKey(target, targetIndex, result, resultIndex),
              });
            });
        });
      });
      setResults(values);
      if (failures) setError(`${failures} panel ${failures === 1 ? "query" : "queries"} failed`);
    }).finally(() => {
      if (sequence === requestSequence.current && !controller.signal.aborted) setLoading(false);
    });
    return () => {
      controller.abort();
      if (requestController.current === controller) requestController.current = null;
    };
  }, [panel, panelCategory, start, end, refreshKey, retryKey, variables, selectedDatasourceId, isStatic, type, clusterName, inViewport]);

  const share = async () => {
    if (!clusterName || !selectedDatasourceId) return;
    setSharing(true);
    try {
      const duration = Math.max(end - start, 1);
      const step = Math.max(Math.ceil(duration / 240), 1);
      const effectiveVariables = withDashboardBuiltIns(variables, clusterName, step);
      const configs = JSON.stringify({
        version: DASHBOARD_SCHEMA_VERSION,
        panel: {
          ...panel,
          datasourceValue: selectedDatasourceId,
          targets: panel.targets.map((target) => ({
            ...target,
            expr: replaceDashboardVariables(target.expr, effectiveVariables),
          })),
        },
        range: { start, end },
      });
      const ids = await MetricsApi.createChartShares(clusterName, [{
        datasource_id: selectedDatasourceId,
        configs,
      }]);
      if (!ids.length) throw new Error("The server did not return a chart share ID");
      const route = `/main/monitoring/shared-charts/${ids.join(",")}`;
      window.open(`${window.location.origin}${window.location.pathname}#${route}`, "_blank", "noopener,noreferrer");
    } catch (caught: unknown) {
      toast.error(caught instanceof Error ? caught.message : "Unable to share chart");
    } finally {
      setSharing(false);
    }
  };

  if (type === "row") return <PanelRenderer panel={panel} results={[]} />;

  if (isStatic) {
    return (
      <article className={`dashboard-panel dashboard-panel-${type} ${type === "text" ? "dashboard-panel-text" : ""}`}>
        <header><h3>{panel.name || (type === "text" ? "Text" : "Embedded content")}</h3></header>
        <PanelRenderer panel={panel} results={[]} height={panelHeight} graphTooltip={graphTooltip} />
      </article>
    );
  }

  return (
    <article ref={panelRef} className={`dashboard-panel dashboard-panel-${type}`}>
      <header><div><h3>{panel.name || "Untitled panel"}</h3>{panel.description && <span title={panel.description}>Info</span>}</div><div className="d-flex align-items-center gap-2"><small>{type}</small>{allowShare && selectedDatasourceId > 0 && <Button variant="link" size="sm" disabled={sharing} title="Share chart" onClick={() => void share()}><FontAwesomeIcon icon={faShareNodes} /></Button>}</div></header>
      {loading && <div className="dashboard-panel-loading"><Spinner size="sm" /></div>}
      {error && <Alert variant="warning" className="m-2 py-1 small d-flex justify-content-between align-items-center"><span>{error}</span><Button variant="link" size="sm" className="p-0" onClick={() => setRetryKey((value) => value + 1)}>Retry</Button></Alert>}
      {!loading && results.length === 0 && !error && inViewport && <div className="monitoring-empty">No data</div>}
      {results.length > 0 && <PanelRenderer panel={panel} results={results} height={panelHeight} graphTooltip={graphTooltip} />}
    </article>
  );
}
