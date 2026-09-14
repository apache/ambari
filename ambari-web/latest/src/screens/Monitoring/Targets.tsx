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

import { useCallback, useContext, useEffect, useRef, useState } from "react";
import { Alert, Badge, Button, Form, Spinner, Table } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faRotate } from "@fortawesome/free-solid-svg-icons";
import MetricsApi, { isMetricsScopeUnsupported } from "../../api/metricsApi";
import { AppContext } from "../../store/context";
import { Datasource, PrometheusTarget } from "./types";
import { translate } from "../../Utils/Utility";

export default function Targets() {
  const { clusterName } = useContext(AppContext);
  const [datasources, setDatasources] = useState<Datasource[]>([]);
  const [datasourceId, setDatasourceId] = useState(0);
  const [targets, setTargets] = useState<PrometheusTarget[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [unsupportedDatasourceIds, setUnsupportedDatasourceIds] = useState<number[]>([]);
  const [loadState, setLoadState] = useState<"idle" | "loading" | "ready" | "error" | "unsupported">("idle");
  const requestGeneration = useRef(0);

  useEffect(() => {
    if (!clusterName) return;
    void MetricsApi.listDatasources(clusterName).then((items) => {
      const enabled = items.filter((item) => item.status === "enabled"
        && (item.plugin_type === "prometheus" || item.category === "prometheus"));
      setDatasources(enabled);
      setDatasourceId((enabled.find((item) => item.is_default) || enabled[0])?.id || 0);
    }).catch(() => setError("Unable to load Prometheus datasources"));
  }, [clusterName]);

  const load = useCallback(async () => {
    const generation = ++requestGeneration.current;
    setTargets([]);
    setError("");
    setLoading(false);
    setLoadState("idle");
    if (!datasourceId) return;
    if (unsupportedDatasourceIds.includes(datasourceId)) {
      setError(String(translate("monitoring.targetsUnsupported")));
      setLoadState("unsupported");
      return;
    }
    setLoading(true);
    setLoadState("loading");
    try {
      const response = await MetricsApi.targets(datasourceId);
      if (requestGeneration.current !== generation) return;
      const activeTargets = response.data?.activeTargets;
      setTargets(Array.isArray(activeTargets)
        ? activeTargets.filter((target): target is PrometheusTarget => target !== null
          && typeof target === "object" && !Array.isArray(target))
        : []);
      setLoadState("ready");
    } catch (caught: unknown) {
      if (requestGeneration.current !== generation) return;
      setTargets([]);
      if (isMetricsScopeUnsupported(caught)) {
        setUnsupportedDatasourceIds((current) => current.includes(datasourceId)
          ? current
          : [...current, datasourceId]);
        setError(String(translate("monitoring.targetsUnsupported")));
        setLoadState("unsupported");
      } else {
        setError(caught instanceof Error ? caught.message : "Unable to load scrape targets");
        setLoadState("error");
      }
    } finally {
      if (requestGeneration.current === generation) setLoading(false);
    }
  }, [datasourceId, unsupportedDatasourceIds]);

  useEffect(() => {
    void load();
    return () => {
      requestGeneration.current += 1;
    };
  }, [load]);

  return (
    <section>
      <div className="monitoring-toolbar">
        <div><h2 className="h4 mb-1">Scrape targets</h2><div className="text-muted small">Current Prometheus service discovery and scrape health.</div></div>
        <div className="d-flex gap-2"><Form.Select aria-label="Datasource" size="sm" value={datasourceId} onChange={(event) => setDatasourceId(Number(event.target.value))}><option value={0}>Select datasource</option>{datasources.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</Form.Select><Button variant="outline-secondary" size="sm" title="Refresh targets" onClick={() => void load()} disabled={loading || loadState === "unsupported"}><FontAwesomeIcon icon={faRotate} /></Button></div>
      </div>
      {error && <Alert variant={loadState === "unsupported" ? "warning" : "danger"}>{error}</Alert>}
      {loadState !== "unsupported" && <div className="monitoring-panel overflow-hidden">
        {loading ? <div className="monitoring-empty"><Spinner size="sm" className="me-2" />Loading targets</div> : loadState === "ready" && targets.length === 0 ? <div className="monitoring-empty">No active targets were returned by this datasource.</div> : targets.length > 0 ? (
          <Table responsive hover className="mb-0 align-middle"><thead><tr><th>Endpoint</th><th>Job / pool</th><th>Health</th><th>Last scrape</th><th>Duration</th><th>Error</th></tr></thead><tbody>{targets.map((target, index) => <tr key={`${target.scrapeUrl}-${index}`}><td className="monitoring-code text-break">{target.scrapeUrl || target.globalUrl || "-"}</td><td>{target.labels?.job || target.scrapePool || "-"}</td><td><Badge bg={target.health === "up" ? "success" : target.health === "unknown" ? "secondary" : "danger"}>{target.health || "unknown"}</Badge></td><td>{target.lastScrape ? new Date(target.lastScrape).toLocaleString() : "-"}</td><td>{target.lastScrapeDuration == null ? "-" : `${(target.lastScrapeDuration * 1000).toFixed(1)} ms`}</td><td className="text-danger small">{target.lastError || ""}</td></tr>)}</tbody></Table>
        ) : null}
      </div>}
    </section>
  );
}
