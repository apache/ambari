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
import { Alert, Badge, Button, ButtonGroup, Form, Offcanvas, Spinner, Table } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faRotate } from "@fortawesome/free-solid-svg-icons";
import MetricsApi, { isMetricsScopeUnsupported } from "../../api/metricsApi";
import { AppContext } from "../../store/context";
import { Datasource, PrometheusTarget } from "./types";
import { translate } from "../../Utils/Utility";
import { managedTargetObservations } from "./managedTargets";
import { useWorkspaceText } from "./workspace";
import { clusterPath } from "../../Utils/clusterRoute";

export default function Targets() {
  const text = useWorkspaceText();
  const { clusterName } = useContext(AppContext);
  const [datasources, setDatasources] = useState<Datasource[]>([]);
  const [datasourceId, setDatasourceId] = useState(0);
  const [targets, setTargets] = useState<PrometheusTarget[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [unsupportedDatasourceIds, setUnsupportedDatasourceIds] = useState<number[]>([]);
  const [loadState, setLoadState] = useState<"idle" | "loading" | "ready" | "error" | "unsupported">("idle");
  const requestGeneration = useRef(0);
  const [filter, setFilter] = useState<"all" | "unhealthy">("all");
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState<PrometheusTarget | null>(null);
  const datasource = datasources.find(item => item.id === datasourceId);
  const managed = datasource?.settings?.managed === true && datasource.settings.provider === "victoriametrics";

  useEffect(() => {
    if (!clusterName) return;
    let active = true;
    setDatasourceId(0);
    setDatasources([]);
    setUnsupportedDatasourceIds([]);
    void MetricsApi.listDatasources(clusterName).then((items) => {
      if (!active) return;
      const enabled = items.filter((item) => item.status === "enabled"
        && (item.plugin_type === "prometheus" || item.category === "prometheus"));
      setDatasources(enabled);
      setDatasourceId((enabled.find((item) => item.is_default) || enabled[0])?.id || 0);
    }).catch(() => { if (active) setError("Unable to load Prometheus datasources"); });
    return () => { active = false; };
  }, [clusterName]);

  const load = useCallback(async () => {
    const generation = ++requestGeneration.current;
    setTargets([]);
    setSelected(null);
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
      let activeTargets: unknown;
      if (managed) {
        const scope = `{cluster=${JSON.stringify(clusterName)}}`;
        const now = Math.floor(Date.now() / 1000);
        const [discovery, observations] = await Promise.all([
          MetricsApi.discoverTargets(clusterName),
          MetricsApi.queryInstantBatch(datasourceId, [
            { refId: "up", query: `up${scope}`, time: now },
            { refId: "timestamp", query: `timestamp(up${scope})`, time: now },
            { refId: "duration", query: `scrape_duration_seconds${scope}`, time: now },
          ]),
        ]);
        activeTargets = managedTargetObservations(discovery, observations, clusterName, now);
      } else {
        const response = await MetricsApi.targets(datasourceId);
        activeTargets = response.data?.activeTargets;
        if (!Array.isArray(activeTargets)) throw new Error("Invalid target metadata response");
      }
      if (requestGeneration.current !== generation) return;
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
  }, [clusterName, datasourceId, managed, unsupportedDatasourceIds]);

  useEffect(() => {
    void load();
    return () => {
      requestGeneration.current += 1;
    };
  }, [load]);
  const visibleTargets = targets.filter(target => (filter === "all" || target.health !== "up")
    && [target.scrapeUrl, ...Object.values(target.labels || {})].join(" ").toLowerCase().includes(query.toLowerCase()));

  return (
    <section>
      <div className="monitoring-toolbar">
        <div><h2 className="h4 mb-1">{text("targetTitle")}</h2><div className="text-muted small">{text("targetHelp")}</div></div>
        <div className="d-flex gap-2"><Form.Select aria-label="Datasource" size="sm" value={datasourceId} onChange={(event) => setDatasourceId(Number(event.target.value))}><option value={0}>{text("selectDatasource")}</option>{datasources.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</Form.Select><Button variant="outline-secondary" size="sm" title="Refresh targets" aria-label={text("refresh")} onClick={() => void load()} disabled={loading || loadState === "unsupported"}><FontAwesomeIcon icon={faRotate} /></Button></div>
      </div>
      {error && <Alert variant={loadState === "unsupported" ? "warning" : "danger"}>{error}</Alert>}
      <div className="monitoring-toolbar"><ButtonGroup><Button variant={filter === "all" ? "secondary" : "outline-secondary"} aria-pressed={filter === "all"} onClick={() => setFilter("all")}>{text("all")} {targets.length}</Button><Button variant={filter === "unhealthy" ? "secondary" : "outline-secondary"} aria-pressed={filter === "unhealthy"} onClick={() => setFilter("unhealthy")}>{text("unhealthy")} {targets.filter(target => target.health !== "up").length}</Button></ButtonGroup><Form.Control className="monitoring-filter" aria-label={text("targetSearch")} placeholder={text("targetSearch")} value={query} onChange={event => setQuery(event.target.value)} /></div>
      {managed && <p className="monitoring-collection-note">{text("managedTargets")}</p>}
      {loadState !== "unsupported" && <div className="monitoring-panel overflow-hidden">
        {loading ? <div className="monitoring-empty" role="status"><Spinner size="sm" className="me-2" />{text("loading")}</div> : loadState === "ready" && visibleTargets.length === 0 ? <div className="monitoring-empty">{text(targets.length ? "noTargetMatch" : "noTargets")}</div> : targets.length > 0 ? (
          <Table responsive hover className="mb-0 align-middle"><thead><tr><th>{text("endpoint")}</th><th>{text("component")}</th><th>{text("health")}</th><th>{text("lastScrape")}</th><th>{text("duration")}</th><th aria-label={text("inspect")} /></tr></thead><tbody>{visibleTargets.map((target, index) => <tr key={`${target.scrapeUrl}-${index}`}><td><strong>{target.labels?.host}</strong><div className="monitoring-code text-break">{target.scrapeUrl || target.globalUrl || "-"}</div></td><td>{target.labels?.component || target.labels?.job || target.scrapePool || "-"}</td><td><Badge bg={target.health === "up" ? "success" : target.health === "down" ? "danger" : "secondary"}>{text(target.health === "up" ? "healthy" : target.health === "down" ? "down" : "unknown")}</Badge></td><td>{target.lastScrape ? new Date(target.lastScrape).toLocaleString() : "-"}</td><td>{target.lastScrapeDuration == null ? "-" : `${(target.lastScrapeDuration * 1000).toFixed(1)} ms`}</td><td><Button variant="link" size="sm" onClick={() => setSelected(target)}>{text("inspect")}</Button></td></tr>)}</tbody></Table>
        ) : null}
      </div>}
      <Offcanvas show={Boolean(selected)} onHide={() => setSelected(null)} placement="end"><Offcanvas.Header closeButton><Offcanvas.Title>{text("targetDetails")}</Offcanvas.Title></Offcanvas.Header><Offcanvas.Body>{selected && <>
        <h3 className="h5">{selected.labels?.host || selected.scrapePool}</h3><p className="monitoring-code text-break">{selected.scrapeUrl}</p>
        {selected.lastError && <Alert variant="danger">{selected.lastError}</Alert>}
        {selected.health === "unknown" && <Alert variant="secondary">{text("staleSample")}</Alert>}
        <dl className="monitoring-detail-list">{Object.entries(selected.labels || {}).filter(([key]) => !key.startsWith("__")).map(([key, value]) => <div key={key}><dt>{key}</dt><dd>{value}</dd></div>)}</dl>
        <a className="btn btn-primary" href={`#${clusterPath(clusterName, "/main/monitoring/explorer")}?${new URLSearchParams({ datasource: String(datasourceId), query: `up{${Object.entries(selected.labels || {}).filter(([key]) => ["cluster", "ambari_cluster_id", "host", "component", "service", "ambari_target"].includes(key)).map(([key, value]) => `${key}=${JSON.stringify(value)}`).join(",")}}` })}`}>{text("viewQuery")}</a>
      </>}</Offcanvas.Body></Offcanvas>
    </section>
  );
}
