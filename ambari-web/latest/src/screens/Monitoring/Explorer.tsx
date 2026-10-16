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

import { FormEvent, useContext, useEffect, useRef, useState } from "react";
import { Alert, Button, ButtonGroup, Form, Spinner, Table } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faPlay, faRotate } from "@fortawesome/free-solid-svg-icons";
import MetricsApi, { isMetricsScopeUnsupported } from "../../api/metricsApi";
import { AppContext } from "../../store/context";
import { Datasource, PrometheusResult } from "./types";
import { normalizePrometheusResults } from "./utils";
import PrometheusChart from "./PrometheusChart";
import { translate } from "../../Utils/Utility";
import { explorerParameters, localDateTime as toLocalInput, useWorkspaceText } from "./workspace";

const metricLabel = (metric: Record<string, string>) => Object.entries(metric)
  .map(([name, value]) => `${name}="${value}"`)
  .join(", ");

const queryHistoryKey = (
  loginName: string,
  clusterId: string | number | undefined,
) => loginName && clusterId != null && /^\d+$/.test(String(clusterId))
  ? `ambari-promql-history:${JSON.stringify([
      "principal-cluster",
      loginName,
      `id:${clusterId}`,
    ])}`
  : "";

const queryHistory = (storageKey: string): string[] => {
  try {
    const value = JSON.parse(localStorage.getItem(storageKey) || "[]") as unknown;
    return Array.isArray(value) ? value.filter((item): item is string => typeof item === "string") : [];
  } catch {
    return [];
  }
};

export default function Explorer() {
  const text = useWorkspaceText();
  const [initialParams] = useState(explorerParameters);
  const { cluster, clusterName, loginName } = useContext(AppContext);
  const [datasources, setDatasources] = useState<Datasource[]>([]);
  const [datasourceId, setDatasourceId] = useState(0);
  const [query, setQuery] = useState(() => initialParams.get("query") || "up");
  const [mode, setMode] = useState<"range" | "instant">("range");
  const initialDate = (name: string, fallback: number) => {
    const raw = initialParams.get(name);
    const value = raw === null ? fallback : Number(raw) * 1000;
    return toLocalInput(new Date(Number.isFinite(value) && value > 0 && value < 8.64e15 ? value : fallback));
  };
  const [start, setStart] = useState(() => initialDate("start", Date.now() - 3600000));
  const [end, setEnd] = useState(() => initialDate("end", Date.now()));
  const [results, setResults] = useState<PrometheusResult[]>([]);
  const [labels, setLabels] = useState<string[]>([]);
  const [metadataWarning, setMetadataWarning] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [executed, setExecuted] = useState(false);
  const [resultView, setResultView] = useState<"chart" | "data">("chart");
  const [resultContext, setResultContext] = useState<{ query: string; start: number; end: number; mode: string } | null>(null);
  const queryController = useRef<AbortController | null>(null);
  const metadataGeneration = useRef(0);
  const queryGeneration = useRef(0);
  const historyKey = queryHistoryKey(
    String(loginName || ""),
    cluster?.cluster_id,
  );

  useEffect(() => {
    if (!clusterName) return;
    let active = true;
    setDatasources([]);
    setDatasourceId(0);
    void MetricsApi.listDatasources(clusterName).then((items) => {
      if (!active) return;
      const prometheus = items.filter((item) => item.status === "enabled"
        && (item.plugin_type === "prometheus" || item.category === "prometheus"));
      setDatasources(prometheus);
      const requestedId = Number(initialParams.get("datasource"));
      const preferred = prometheus.find((item) => item.id === requestedId) || prometheus.find((item) => item.is_default) || prometheus[0];
      setDatasourceId(preferred?.id || 0);
    }).catch(() => { if (active) setError("Unable to load Prometheus datasources"); });
    return () => { active = false; };
  }, [clusterName, initialParams]);

  useEffect(() => {
    const generation = ++metadataGeneration.current;
    if (!datasourceId) {
      setLabels([]);
      setMetadataWarning("");
      return;
    }
    setLabels([]);
    setMetadataWarning("");
    void MetricsApi.labels(datasourceId).then((response) => {
      if (metadataGeneration.current !== generation) return;
      setLabels(Array.isArray(response.data)
        ? response.data.filter((label): label is string => typeof label === "string")
        : []);
    }).catch((caught: unknown) => {
      if (metadataGeneration.current !== generation) return;
      setLabels([]);
      if (isMetricsScopeUnsupported(caught)) {
        setMetadataWarning(String(translate("monitoring.metadataUnsupported")));
      }
    });
    return () => {
      metadataGeneration.current += 1;
    };
  }, [datasourceId]);

  useEffect(() => {
    queryGeneration.current += 1;
    setResults([]);
    setError("");
    setLoading(false);
    setExecuted(false);
    setResultContext(null);
    queryController.current?.abort();
    return () => {
      queryGeneration.current += 1;
      queryController.current?.abort();
    };
  }, [datasourceId, historyKey]);

  const execute = async (event?: FormEvent) => {
    event?.preventDefault();
    if (!datasourceId || !query.trim()) return;
    const generation = ++queryGeneration.current;
    const requestedDatasourceId = datasourceId;
    const requestedQuery = query.trim();
    const requestedHistoryKey = historyKey;
    queryController.current?.abort();
    const controller = new AbortController();
    queryController.current = controller;
    setLoading(true);
    setError("");
    try {
      const endSeconds = Math.floor(new Date(end).getTime() / 1000);
      const startSeconds = Math.floor(new Date(start).getTime() / 1000);
      if (!Number.isFinite(endSeconds) || !Number.isFinite(startSeconds) || (mode === "range" && startSeconds >= endSeconds)) throw new Error(text("invalidRange"));
      const duration = Math.max(endSeconds - startSeconds, 1);
      const response = mode === "instant"
        ? await MetricsApi.query(requestedDatasourceId, requestedQuery, endSeconds, controller.signal)
        : await MetricsApi.queryRange(requestedDatasourceId, requestedQuery, startSeconds, endSeconds, Math.max(Math.ceil(duration / 240), 1), controller.signal);
      if (queryGeneration.current !== generation || controller.signal.aborted) return;
      if (response.status !== "success") {
        throw new Error(response.error || "Prometheus query failed");
      }
      setResults(normalizePrometheusResults(response.data?.result));
      setExecuted(true);
      setResultContext({ query: requestedQuery, start: startSeconds, end: endSeconds, mode });
      if (requestedHistoryKey) {
        const history = queryHistory(requestedHistoryKey);
        try { localStorage.setItem(requestedHistoryKey, JSON.stringify(
          [requestedQuery, ...history.filter((item) => item !== requestedQuery)].slice(0, 20),
        )); } catch { /* Query results remain usable when browser storage is disabled. */ }
      }
    } catch (caught: unknown) {
      if (queryGeneration.current !== generation || controller.signal.aborted) return;
      setResults([]);
      setError(caught instanceof Error ? caught.message : "Prometheus query failed");
    } finally {
      if (queryGeneration.current === generation) setLoading(false);
    }
  };

  return (
    <section>
      <div className="monitoring-toolbar">
        <div><h2 className="h4 mb-1">{text("queryTitle")}</h2><div className="text-muted small">{text("queryHelp")}</div></div>
        <Button variant="outline-secondary" size="sm" onClick={() => {
          setEnd(toLocalInput(new Date()));
          setStart(toLocalInput(new Date(Date.now() - 60 * 60 * 1000)));
        }}><FontAwesomeIcon icon={faRotate} className="me-2" />{text("last60")}</Button>
      </div>
      <Form className="monitoring-panel monitoring-query-editor p-4 mb-3" onSubmit={execute} onKeyDown={(event) => { if ((event.ctrlKey || event.metaKey) && event.key === "Enter") { event.preventDefault(); void execute(); } }}>
        <div className="row g-3 align-items-end">
          <Form.Group controlId="monitoring-explorer-datasource" className="col-xl-4 col-md-6"><Form.Label>{text("datasource")}</Form.Label><Form.Select aria-label="Datasource" value={datasourceId} onChange={(event) => setDatasourceId(Number(event.target.value))}><option value={0}>{text("selectDatasource")}</option>{datasources.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</Form.Select></Form.Group>
          <Form.Group className="col-xl-2 col-md-6"><Form.Label>{text("type")}</Form.Label><ButtonGroup className="w-100"><Button variant={mode === "range" ? "secondary" : "outline-secondary"} aria-pressed={mode === "range"} onClick={() => setMode("range")}>{text("range")}</Button><Button variant={mode === "instant" ? "secondary" : "outline-secondary"} aria-pressed={mode === "instant"} onClick={() => setMode("instant")}>{text("instant")}</Button></ButtonGroup></Form.Group>
          <Form.Group controlId="monitoring-explorer-start" className="col-xl-3 col-md-6"><Form.Label>{text("start")}</Form.Label><Form.Control type="datetime-local" step={1} disabled={mode === "instant"} value={start} onChange={(event) => setStart(event.target.value)} /></Form.Group>
          <Form.Group controlId="monitoring-explorer-end" className="col-xl-3 col-md-6"><Form.Label>{text(mode === "instant" ? "evaluation" : "end")}</Form.Label><Form.Control type="datetime-local" step={1} value={end} onChange={(event) => setEnd(event.target.value)} /></Form.Group>
          <Form.Group controlId="monitoring-explorer-query" className="col-12"><Form.Label>PromQL</Form.Label><Form.Control className="monitoring-code" as="textarea" rows={4} spellCheck={false} value={query} onChange={(event) => setQuery(event.target.value)} /><datalist id="prometheus-labels">{labels.map((label) => <option key={label} value={label} />)}</datalist></Form.Group>
        </div>
        <div className="monitoring-query-actions">
          <span className="text-muted small">{text("queryShortcut")}</span>
          <div className="d-flex gap-2">{loading && <Button variant="outline-secondary" onClick={() => { queryController.current?.abort(); queryGeneration.current += 1; setLoading(false); }}>{text("cancel")}</Button>}<Button type="submit" aria-label="Run query" title={text("run")} variant="success" disabled={loading || !datasourceId || !query.trim()}>{loading ? <Spinner size="sm" className="me-2" /> : <FontAwesomeIcon icon={faPlay} className="me-2" />}{text("run")}</Button></div>
        </div>
        <details className="monitoring-query-library mt-3"><summary>{text("history")} / {text("metrics")}</summary><div className="d-flex flex-wrap gap-2 mt-2">{queryHistory(historyKey).map(item => <Button key={item} size="sm" variant="outline-secondary" className="text-break text-start" onClick={() => setQuery(item)}>{item}</Button>)}</div><div className="text-muted small mt-2">{labels.join(", ")}</div></details>
      </Form>
      {metadataWarning && <Alert variant="warning">{metadataWarning}</Alert>}
      {error && <Alert variant="danger">{error}</Alert>}
      {resultContext && (query.trim() !== resultContext.query || mode !== resultContext.mode || new Date(start).getTime() / 1000 !== resultContext.start || new Date(end).getTime() / 1000 !== resultContext.end) && <div className="monitoring-refresh-notice">{text("resultStale")}</div>}
      <div className="monitoring-panel p-3">
        {loading ? <div className="monitoring-empty" role="status"><Spinner size="sm" className="me-2" />{text("loading")}</div> : error ? <div className="monitoring-empty">{text("queryFailed")}</div> : results.length === 0 ? <div className="monitoring-empty" role="status"><h3>{text(executed ? "emptyTitle" : "idleTitle")}</h3><p>{text(executed ? "emptyHelp" : "idleHelp")}</p></div> : <>
          <div className="monitoring-toolbar"><ButtonGroup aria-label={text("results")}><Button variant={resultView === "chart" ? "secondary" : "outline-secondary"} aria-pressed={resultView === "chart"} onClick={() => setResultView("chart")}>{text("chart")}</Button><Button variant={resultView === "data" ? "secondary" : "outline-secondary"} aria-pressed={resultView === "data"} onClick={() => setResultView("data")}>{text("data")}</Button></ButtonGroup><span className="text-muted small">{results.length} {text("series")}</span></div>
          {resultView === "chart" && <PrometheusChart key={`${datasourceId}:${resultContext?.query}`} results={results} start={resultContext?.mode === "range" ? resultContext.start : undefined} end={resultContext?.mode === "range" ? resultContext.end : undefined} />}
          <div hidden={resultView !== "data"}><Table responsive size="sm" className="mt-3 mb-0"><thead><tr><th>{text("series")}</th><th>{text("lastValue")}</th><th>{text("samples")}</th></tr></thead><tbody>{results.map((result, index) => {
            const values = result.values || (result.value ? [result.value] : []);
            return <tr key={`${metricLabel(result.metric)}-${index}`}><td className="monitoring-code text-break">{metricLabel(result.metric) || "{}"}</td><td>{values.at(-1)?.[1] ?? "-"}</td><td>{values.length}</td></tr>;
          })}</tbody></Table></div>
        </>}
      </div>
    </section>
  );
}
