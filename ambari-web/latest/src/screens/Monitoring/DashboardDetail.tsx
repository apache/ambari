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

import { useCallback, useContext, useEffect, useState } from "react";
import { Alert, Button, Form, Modal, Spinner } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faArrowLeft, faCode, faRotate } from "@fortawesome/free-solid-svg-icons";
import toast from "react-hot-toast";
import { Link, useParams } from "react-router-dom";
import MetricsApi from "../../api/metricsApi";
import { AppContext } from "../../store/context";
import { useAuth } from "../../hooks/useAuth";
import { Dashboard, DashboardPanel as Panel, DashboardPayload, Datasource, JsonObject } from "./types";
import DashboardPanel from "./DashboardPanel";
import { RESERVED_DASHBOARD_VARIABLES } from "./utils";

const toLocalInput = (date: Date) => {
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
};

const variableName = (variable: JsonObject) => typeof variable.name === "string" ? variable.name : "";

interface DashboardDetailProps {
  dashboardId?: string;
  embedded?: boolean;
}

export default function DashboardDetail({ dashboardId: dashboardIdProp, embedded = false }: DashboardDetailProps) {
  const { dashboardId: routeDashboardId = "" } = useParams<{ dashboardId: string }>();
  const dashboardId = dashboardIdProp || routeDashboardId;
  const { clusterName } = useContext(AppContext);
  const { hasAuthorization } = useAuth();
  const canManage = hasAuthorization("CLUSTER.MANAGE_USER_PERSISTED_DATA");
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [payload, setPayload] = useState<DashboardPayload>({ panels: [], var: [] });
  const [rawPayload, setRawPayload] = useState("");
  const [datasources, setDatasources] = useState<Datasource[]>([]);
  const [variables, setVariables] = useState<Record<string, string | number | string[]>>({});
  const [start, setStart] = useState(toLocalInput(new Date(Date.now() - 60 * 60 * 1000)));
  const [end, setEnd] = useState(toLocalInput(new Date()));
  const [refreshKey, setRefreshKey] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showJson, setShowJson] = useState(false);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    if (!clusterName || !dashboardId) return;
    setLoading(true);
    setError("");
    try {
      const [detail, sourceItems] = await Promise.all([
        MetricsApi.getDashboard(clusterName, dashboardId),
        MetricsApi.listDatasources(clusterName),
      ]);
      const raw = detail.configs || "{}";
      const parsed = JSON.parse(raw) as DashboardPayload;
      setDashboard(detail);
      setRawPayload(raw);
      setPayload(parsed);
      setDatasources(sourceItems);
      const initialVariables: Record<string, string | number | string[]> = {};
      (parsed.var || []).forEach((variable) => {
        const name = variableName(variable);
        if (!name || RESERVED_DASHBOARD_VARIABLES.has(name)) return;
        if (variable.type === "datasource") {
          const category = typeof variable.definition === "string" ? variable.definition : "prometheus";
          const options = sourceItems.filter((item) => item.status === "enabled"
            && (item.category === category || item.plugin_type === category));
          initialVariables[name] = (options.find((item) => item.is_default) || options[0])?.id || "";
        } else if (typeof variable.value === "string") {
          initialVariables[name] = variable.value;
        } else {
          initialVariables[name] = "";
        }
      });
      setVariables(initialVariables);
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : "Unable to load dashboard");
    } finally {
      setLoading(false);
    }
  }, [clusterName, dashboardId]);

  useEffect(() => { void load(); }, [load]);

  const saveJson = async () => {
    if (!dashboard) return;
    setSaving(true);
    try {
      const parsed = JSON.parse(rawPayload) as DashboardPayload;
      await MetricsApi.updateDashboardConfigs(clusterName, dashboard.id, rawPayload);
      setPayload(parsed);
      setShowJson(false);
      toast.success("Dashboard payload saved");
      setRefreshKey((value) => value + 1);
    } catch (caught: unknown) {
      toast.error(caught instanceof Error ? caught.message : "Unable to save dashboard payload");
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="monitoring-empty"><Spinner size="sm" className="me-2" />Loading dashboard</div>;
  if (error || !dashboard) return <Alert variant="danger">{error || "Dashboard not found"}</Alert>;

  const panels = [...(payload.panels || [])].sort((left, right) => {
    const y = (left.layout?.y || 0) - (right.layout?.y || 0);
    return y || (left.layout?.x || 0) - (right.layout?.x || 0);
  });
  const startSeconds = Math.floor(new Date(start).getTime() / 1000);
  const endSeconds = Math.floor(new Date(end).getTime() / 1000);

  return (
    <section className="dashboard-detail">
      <div className="monitoring-toolbar">
        <div className="d-flex align-items-start gap-3">{!embedded && <Link className="btn btn-sm btn-outline-secondary" title="Back to dashboards" to="/main/monitoring/dashboards"><FontAwesomeIcon icon={faArrowLeft} /></Link>}<div><h2 className="h4 mb-1">{dashboard.name}</h2><div className="text-muted small">ID {dashboard.id}{dashboard.ident ? ` / ${dashboard.ident}` : ""} · updated {new Date(dashboard.update_at * 1000).toLocaleString()}</div></div></div>
        <div className="d-flex flex-wrap gap-2 align-items-end"><Form.Group><Form.Label className="small mb-1">Start</Form.Label><Form.Control size="sm" type="datetime-local" value={start} onChange={(event) => setStart(event.target.value)} /></Form.Group><Form.Group><Form.Label className="small mb-1">End</Form.Label><Form.Control size="sm" type="datetime-local" value={end} onChange={(event) => setEnd(event.target.value)} /></Form.Group><Button size="sm" variant="outline-secondary" title="Refresh panels" onClick={() => setRefreshKey((value) => value + 1)}><FontAwesomeIcon icon={faRotate} /></Button>{canManage && <Button size="sm" variant="outline-secondary" onClick={() => setShowJson(true)}><FontAwesomeIcon icon={faCode} className="me-2" />Edit JSON</Button>}</div>
      </div>
      {(payload.var || []).length > 0 && <div className="monitoring-panel p-3 mb-3 d-flex gap-3 flex-wrap">{(payload.var || []).map((variable, index) => {
        const name = variableName(variable);
        if (!name || RESERVED_DASHBOARD_VARIABLES.has(name)) return null;
        if (variable.type === "datasource") {
          const category = typeof variable.definition === "string" ? variable.definition : "prometheus";
          const options = datasources.filter((item) => item.status === "enabled" && (item.category === category || item.plugin_type === category));
          return <Form.Group key={`${name}-${index}`}><Form.Label className="small mb-1">{String(variable.label || name)}</Form.Label><Form.Select size="sm" value={String(variables[name] ?? "")} onChange={(event) => setVariables({ ...variables, [name]: Number(event.target.value) })}>{options.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</Form.Select></Form.Group>;
        }
        return <Form.Group key={`${name}-${index}`}><Form.Label className="small mb-1">{String(variable.label || name)}</Form.Label><Form.Control size="sm" placeholder={String(variable.definition || "Value")} value={String(variables[name] ?? "")} onChange={(event) => setVariables({ ...variables, [name]: event.target.value })} /></Form.Group>;
      })}</div>}
      {panels.length === 0 ? <div className="monitoring-panel monitoring-empty">This dashboard has no panels yet.</div> : <div className="dashboard-grid">{panels.map((panel: Panel, index) => <div key={panel.id || index} className={panel.type === "row" ? "dashboard-grid-row" : ""} style={{ gridColumn: `span ${Math.max(1, Math.min(24, panel.layout?.w || 8))}` }}><DashboardPanel panel={panel} start={startSeconds} end={endSeconds} refreshKey={refreshKey} variables={{ ...variables, cluster: clusterName }} datasources={datasources} /></div>)}</div>}
      <Modal show={showJson} onHide={() => setShowJson(false)} size="xl" centered><Modal.Header closeButton><Modal.Title>Dashboard payload</Modal.Title></Modal.Header><Modal.Body><Alert variant="info" className="small">Saving replaces only the raw board payload. Unknown panel, variable, transform, override, and datasource fields are retained unless removed here.</Alert><Form.Control className="monitoring-code" as="textarea" rows={28} value={rawPayload} onChange={(event) => setRawPayload(event.target.value)} /></Modal.Body><Modal.Footer><Button variant="outline-secondary" onClick={() => setShowJson(false)}>Cancel</Button><Button variant="success" disabled={saving} onClick={() => void saveJson()}>{saving && <Spinner size="sm" className="me-2" />}Save payload</Button></Modal.Footer></Modal>
    </section>
  );
}
