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

import { useContext, useEffect, useState } from "react";
import { Alert, Button, Spinner, Table } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faShareNodes } from "@fortawesome/free-solid-svg-icons";
import toast from "react-hot-toast";
import MetricsApi from "../../api/metricsApi";
import { AppContext } from "../../store/context";
import {
  DashboardPanel as Panel,
  DashboardTarget,
  Datasource,
  PrometheusResult,
} from "./types";
import {
  normalizePrometheusResults,
  replaceDashboardVariables,
  resolvePanelDatasourceId,
  withDashboardBuiltIns,
} from "./utils";
import PrometheusChart from "./PrometheusChart";
import { formatMetricValue, getPanelUnit } from "./valueFormatter";

interface DashboardPanelProps {
  panel: Panel;
  start: number;
  end: number;
  refreshKey: number;
  variables: Record<string, string | number | string[]>;
  datasources: Datasource[];
  allowShare?: boolean;
}

interface PanelResult extends PrometheusResult {
  displayName: string;
  seriesKey: string;
}

const lastValue = (result: PrometheusResult) => {
  const point = result.values?.at(-1) || result.value;
  return point?.[1] ?? "-";
};

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
}: DashboardPanelProps) {
  const { clusterName } = useContext(AppContext);
  const [results, setResults] = useState<PanelResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [sharing, setSharing] = useState(false);
  const [error, setError] = useState("");
  const type = panel.type || "unknown";
  const isStatic = type === "row" || type === "text" || type === "iframe";
  const panelCategory = panel.datasourceCate || "prometheus";
  const unit = getPanelUnit(panel.options);
  const selectedDatasourceId = resolvePanelDatasourceId(panel, variables, datasources);

  useEffect(() => {
    if (isStatic) return;
    const targets = (Array.isArray(panel.targets) ? panel.targets : []).filter((target) => !target.hide
      && typeof target.expr === "string" && target.expr.trim());
    if (panelCategory !== "prometheus" || !selectedDatasourceId || targets.length === 0) {
      setResults([]);
      setError(panelCategory !== "prometheus"
        ? `${panelCategory} panel rendering is not available yet`
        : "Panel has no queryable Prometheus target");
      return;
    }
    let active = true;
    setLoading(true);
    setError("");
    const duration = Math.max(end - start, 1);
    const step = Math.max(Math.ceil(duration / 240), 1);
    const effectiveVariables = withDashboardBuiltIns(variables, clusterName, step);
    const displayVariables = {
      ...effectiveVariables,
      cluster: clusterName || "",
    };
    void Promise.allSettled(targets.map((target) => {
      const expression = replaceDashboardVariables(target.expr || "", effectiveVariables);
      return target.instant || ["stat", "gauge", "barGauge"].includes(type)
        ? MetricsApi.query(selectedDatasourceId, expression, end)
        : MetricsApi.queryRange(selectedDatasourceId, expression, start, end, step);
    })).then((responses) => {
      if (!active) return;
      const values: PanelResult[] = [];
      let failures = 0;
      responses.forEach((response, targetIndex) => {
        if (response.status === "fulfilled" && response.value.status === "success") {
          const target = targets[targetIndex];
          normalizePrometheusResults(response.value.data?.result).forEach((result, resultIndex) => {
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
        } else {
          failures += 1;
        }
      });
      setResults(values);
      if (failures) setError(`${failures} panel ${failures === 1 ? "query" : "queries"} failed`);
    }).finally(() => {
      if (active) setLoading(false);
    });
    return () => { active = false; };
  }, [panel, panelCategory, start, end, refreshKey, variables, selectedDatasourceId, isStatic, type, clusterName]);

  const share = async () => {
    if (!clusterName || !selectedDatasourceId) return;
    setSharing(true);
    try {
      const datasourceName = datasources.find((item) => item.id === selectedDatasourceId)?.name || "";
      const duration = Math.max(end - start, 1);
      const step = Math.max(Math.ceil(duration / 240), 1);
      const effectiveVariables = withDashboardBuiltIns(variables, clusterName, step);
      const configs = JSON.stringify({
        dataProps: {
          ...panel,
          datasourceValue: selectedDatasourceId,
          datasourceName,
          targets: (Array.isArray(panel.targets) ? panel.targets : []).map((target) => ({
            ...target,
            expr: replaceDashboardVariables(
              typeof target.expr === "string" ? target.expr : "",
              effectiveVariables,
            ),
          })),
          range: { start, end },
        },
      });
      const ids = await MetricsApi.createChartShares(clusterName, [{
        datasource_id: selectedDatasourceId,
        configs,
      }]);
      if (!ids.length) throw new Error("The server did not return a chart share ID");
      const route = `/main/monitoring/charts/${ids.join(",")}`;
      window.open(`${window.location.origin}${window.location.pathname}#${route}`, "_blank", "noopener,noreferrer");
    } catch (caught: unknown) {
      toast.error(caught instanceof Error ? caught.message : "Unable to share chart");
    } finally {
      setSharing(false);
    }
  };

  if (type === "row") {
    return <div className="dashboard-row-title">{panel.name || "Section"}</div>;
  }

  if (type === "text") {
    const custom = panel.custom as Record<string, unknown> | undefined;
    return <article className="dashboard-panel dashboard-panel-text"><h3>{panel.name || "Text"}</h3><div>{String(custom?.content || panel.description || "")}</div></article>;
  }

  if (type === "iframe") {
    const custom = panel.custom as Record<string, unknown> | undefined;
    const url = String(custom?.url || custom?.iframe_url || "");
    return <article className="dashboard-panel"><h3>{panel.name || "Embedded content"}</h3>{url ? <iframe title={panel.name || "Embedded content"} src={url} sandbox="allow-forms allow-scripts allow-same-origin" /> : <div className="monitoring-empty">No iframe URL is configured.</div>}</article>;
  }

  return (
    <article className="dashboard-panel">
      <header><div><h3>{panel.name || "Untitled panel"}</h3>{panel.description && <span title={panel.description}>Info</span>}</div><div className="d-flex align-items-center gap-2"><small>{type}</small>{allowShare && selectedDatasourceId > 0 && <Button variant="link" size="sm" disabled={sharing} title="Share chart" onClick={() => void share()}><FontAwesomeIcon icon={faShareNodes} /></Button>}</div></header>
      {loading && <div className="dashboard-panel-loading"><Spinner size="sm" /></div>}
      {error && <Alert variant="warning" className="m-2 py-1 small">{error}</Alert>}
      {!loading && results.length === 0 && !error && <div className="monitoring-empty">No data</div>}
      {results.length > 0 && ["stat", "gauge", "barGauge"].includes(type) && (
        <div className="dashboard-stat-grid">{results.map((result) => <div key={result.seriesKey}><strong>{formatMetricValue(lastValue(result), unit)}</strong><span>{result.displayName}</span></div>)}</div>
      )}
      {results.length > 0 && type === "table" && (
        <div className="overflow-auto"><Table size="sm" className="mb-0"><thead><tr><th>Series</th><th>Value</th></tr></thead><tbody>{results.map((result) => <tr key={result.seriesKey}><td className="monitoring-code">{result.displayName}</td><td>{formatMetricValue(lastValue(result), unit)}</td></tr>)}</tbody></Table></div>
      )}
      {results.length > 0 && !["stat", "gauge", "barGauge", "table"].includes(type) && <PrometheusChart results={results} unit={unit} />}
      {!isStatic && !["timeseries", "stat", "gauge", "barGauge", "table", "pie", "barChart", "heatmap", "hexbin"].includes(type) && <div className="monitoring-empty">Unsupported panel type: {type}. Its configuration remains preserved.</div>}
    </article>
  );
}
