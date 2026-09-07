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

import { FormEvent, useContext, useEffect, useState } from "react";
import { Alert, Button, ButtonGroup, Form, Spinner, Table } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faPlay, faRotate } from "@fortawesome/free-solid-svg-icons";
import MetricsApi from "../../api/metricsApi";
import { AppContext } from "../../store/context";
import { Datasource, PrometheusResult } from "./types";
import { normalizePrometheusResults } from "./utils";
import PrometheusChart from "./PrometheusChart";

const toLocalInput = (date: Date) => {
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
};

const metricLabel = (metric: Record<string, string>) => Object.entries(metric)
  .map(([name, value]) => `${name}="${value}"`)
  .join(", ");

const queryHistory = (): string[] => {
  try {
    const value = JSON.parse(localStorage.getItem("ambari-promql-history") || "[]") as unknown;
    return Array.isArray(value) ? value.filter((item): item is string => typeof item === "string") : [];
  } catch {
    return [];
  }
};

export default function Explorer() {
  const { clusterName } = useContext(AppContext);
  const [datasources, setDatasources] = useState<Datasource[]>([]);
  const [datasourceId, setDatasourceId] = useState(0);
  const [query, setQuery] = useState("up");
  const [mode, setMode] = useState<"range" | "instant">("range");
  const [start, setStart] = useState(toLocalInput(new Date(Date.now() - 60 * 60 * 1000)));
  const [end, setEnd] = useState(toLocalInput(new Date()));
  const [results, setResults] = useState<PrometheusResult[]>([]);
  const [labels, setLabels] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!clusterName) return;
    void MetricsApi.listDatasources(clusterName).then((items) => {
      const prometheus = items.filter((item) => item.status === "enabled"
        && (item.plugin_type === "prometheus" || item.category === "prometheus"));
      setDatasources(prometheus);
      const preferred = prometheus.find((item) => item.is_default) || prometheus[0];
      setDatasourceId(preferred?.id || 0);
    }).catch(() => setError("Unable to load Prometheus datasources"));
  }, [clusterName]);

  useEffect(() => {
    if (!datasourceId) {
      setLabels([]);
      return;
    }
    void MetricsApi.labels(datasourceId).then((response) => {
      setLabels(Array.isArray(response.data)
        ? response.data.filter((label): label is string => typeof label === "string")
        : []);
    }).catch(() => setLabels([]));
  }, [datasourceId]);

  const execute = async (event?: FormEvent) => {
    event?.preventDefault();
    if (!datasourceId || !query.trim()) return;
    setLoading(true);
    setError("");
    try {
      const endSeconds = Math.floor(new Date(end).getTime() / 1000);
      const startSeconds = Math.floor(new Date(start).getTime() / 1000);
      const duration = Math.max(endSeconds - startSeconds, 1);
      const response = mode === "instant"
        ? await MetricsApi.query(datasourceId, query.trim(), endSeconds)
        : await MetricsApi.queryRange(datasourceId, query.trim(), startSeconds, endSeconds, Math.max(Math.ceil(duration / 240), 1));
      if (response.status !== "success") {
        throw new Error(response.error || "Prometheus query failed");
      }
      setResults(normalizePrometheusResults(response.data?.result));
      const history = queryHistory();
      localStorage.setItem("ambari-promql-history", JSON.stringify(
        [query.trim(), ...history.filter((item) => item !== query.trim())].slice(0, 20),
      ));
    } catch (caught: unknown) {
      setResults([]);
      setError(caught instanceof Error ? caught.message : "Prometheus query failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <section>
      <div className="monitoring-toolbar">
        <div><h2 className="h4 mb-1">PromQL explorer</h2><div className="text-muted small">Inspect live and historical series without leaving Ambari.</div></div>
        <Button variant="outline-secondary" size="sm" onClick={() => {
          setEnd(toLocalInput(new Date()));
          setStart(toLocalInput(new Date(Date.now() - 60 * 60 * 1000)));
        }}><FontAwesomeIcon icon={faRotate} className="me-2" />Last hour</Button>
      </div>
      <Form className="monitoring-panel p-3 mb-3" onSubmit={execute}>
        <div className="row g-3 align-items-end">
          <Form.Group className="col-lg-3"><Form.Label>Datasource</Form.Label><Form.Select value={datasourceId} onChange={(event) => setDatasourceId(Number(event.target.value))}><option value={0}>Select a datasource</option>{datasources.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</Form.Select></Form.Group>
          <Form.Group className="col-lg-2"><Form.Label>Mode</Form.Label><ButtonGroup className="w-100"><Button variant={mode === "range" ? "secondary" : "outline-secondary"} onClick={() => setMode("range")}>Range</Button><Button variant={mode === "instant" ? "secondary" : "outline-secondary"} onClick={() => setMode("instant")}>Instant</Button></ButtonGroup></Form.Group>
          <Form.Group className="col-lg-3"><Form.Label>Start</Form.Label><Form.Control type="datetime-local" disabled={mode === "instant"} value={start} onChange={(event) => setStart(event.target.value)} /></Form.Group>
          <Form.Group className="col-lg-3"><Form.Label>{mode === "instant" ? "Evaluation time" : "End"}</Form.Label><Form.Control type="datetime-local" value={end} onChange={(event) => setEnd(event.target.value)} /></Form.Group>
          <div className="col-lg-1 d-grid"><Button type="submit" variant="success" disabled={loading || !datasourceId}>{loading ? <Spinner size="sm" /> : <FontAwesomeIcon icon={faPlay} />}</Button></div>
          <Form.Group className="col-12"><Form.Label>PromQL</Form.Label><Form.Control className="monitoring-code" as="textarea" rows={3} list="prometheus-labels" value={query} onChange={(event) => setQuery(event.target.value)} /><datalist id="prometheus-labels">{labels.map((label) => <option key={label} value={label} />)}</datalist></Form.Group>
        </div>
      </Form>
      {error && <Alert variant="danger">{error}</Alert>}
      <div className="monitoring-panel p-3">
        {results.length === 0 ? <div className="monitoring-empty">Run a query to inspect its result.</div> : <>
          <PrometheusChart results={results} />
          <Table responsive size="sm" className="mt-3 mb-0"><thead><tr><th>Series</th><th>Last value</th><th>Samples</th></tr></thead><tbody>{results.map((result, index) => {
            const values = result.values || (result.value ? [result.value] : []);
            return <tr key={`${metricLabel(result.metric)}-${index}`}><td className="monitoring-code text-break">{metricLabel(result.metric) || "{}"}</td><td>{values.at(-1)?.[1] ?? "-"}</td><td>{values.length}</td></tr>;
          })}</tbody></Table>
        </>}
      </div>
    </section>
  );
}
