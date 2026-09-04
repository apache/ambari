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
import { Alert, Badge, Button, Dropdown, Form, Modal, Spinner } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faArrowLeft,
  faClone,
  faCode,
  faCopy,
  faFloppyDisk,
  faGear,
  faPen,
  faPlus,
  faRotate,
  faSliders,
  faTrash,
  faXmark,
} from "@fortawesome/free-solid-svg-icons";
import toast from "react-hot-toast";
import { Link, useNavigate, useParams, useSearchParams } from "react-router-dom";
import MetricsApi from "../../../api/metricsApi";
import { AppContext } from "../../../store/context";
import { useAuth } from "../../../hooks/useAuth";
import {
  DASHBOARD_SCHEMA_VERSION,
  type Dashboard,
  type DashboardPanel as Panel,
  type DashboardPayload,
  type Datasource,
} from "../types";
import DashboardPanel from "./DashboardPanel";
import DashboardLayout from "./DashboardLayout";
import DashboardPanelEditor from "./DashboardPanelEditor";
import { DashboardSettingsDialog, DashboardVariablesDialog } from "./DashboardWorkspaceDialogs";
import { dashboardPanelHeight } from "./layout/dashboardLayout";
import { normalizeDashboardPayload, parseDashboardPayload, RESERVED_DASHBOARD_VARIABLES } from "../utils";
import {
  applyDashboardLayout,
  cloneDashboardPayload,
  createDashboardPanel,
  duplicateDashboardPanel,
  PANEL_TYPE_OPTIONS,
} from "./dashboardWorkspace";

const toLocalInput = (date: Date) => {
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
};

const metadataSnapshot = (dashboard: Dashboard | null) => dashboard ? JSON.stringify({
  name: dashboard.name,
  tags: dashboard.tags || "",
  public: dashboard.public,
}) : "";

const initialVariableValues = (payload: DashboardPayload, datasources: Datasource[]) => {
  const values: Record<string, string | number | string[]> = {};
  payload.var.forEach((variable) => {
    if (RESERVED_DASHBOARD_VARIABLES.has(variable.name)) return;
    if (variable.type === "datasource") {
      const category = variable.definition || "prometheus";
      const options = datasources.filter((item) => item.status === "enabled"
        && (item.category === category || item.plugin_type === category));
      values[variable.name] = (options.find((item) => item.is_default) || options[0])?.id || "";
    } else {
      values[variable.name] = variable.value || "";
    }
  });
  return values;
};

export interface DashboardPageProps {
  dashboardId?: string;
  embedded?: boolean;
}

export default function DashboardPage({ dashboardId: dashboardIdProp, embedded = false }: DashboardPageProps) {
  const { dashboardId: routeDashboardId = "" } = useParams<{ dashboardId: string }>();
  const dashboardId = dashboardIdProp || routeDashboardId;
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { clusterName } = useContext(AppContext);
  const { hasAuthorization } = useAuth();
  const canManage = hasAuthorization("CLUSTER.MANAGE_USER_PERSISTED_DATA");
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [savedDashboard, setSavedDashboard] = useState<Dashboard | null>(null);
  const [payload, setPayload] = useState<DashboardPayload>({ version: DASHBOARD_SCHEMA_VERSION, panels: [], var: [] });
  const [savedPayload, setSavedPayload] = useState<DashboardPayload>({ version: DASHBOARD_SCHEMA_VERSION, panels: [], var: [] });
  const [rawPayload, setRawPayload] = useState("");
  const [datasources, setDatasources] = useState<Datasource[]>([]);
  const [variables, setVariables] = useState<Record<string, string | number | string[]>>({});
  const [start, setStart] = useState(toLocalInput(new Date(Date.now() - 60 * 60 * 1000)));
  const [end, setEnd] = useState(toLocalInput(new Date()));
  const [rangeMinutes, setRangeMinutes] = useState(60);
  const [autoRefresh, setAutoRefresh] = useState(0);
  const [refreshKey, setRefreshKey] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [isEditing, setIsEditing] = useState(false);
  const [showJson, setShowJson] = useState(false);
  const [showVariables, setShowVariables] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [panelEditor, setPanelEditor] = useState<Panel | null>(null);
  const [saving, setSaving] = useState(false);
  const [cloning, setCloning] = useState(false);

  const payloadDirty = JSON.stringify(payload) !== JSON.stringify(savedPayload);
  const metadataDirty = metadataSnapshot(dashboard) !== metadataSnapshot(savedDashboard);
  const dirty = payloadDirty || metadataDirty;

  const load = useCallback(async () => {
    if (!clusterName || !dashboardId) return;
    setLoading(true);
    setError("");
    try {
      const [detail, sourceItems] = await Promise.all([
        MetricsApi.getDashboard(clusterName, dashboardId),
        MetricsApi.listDatasources(clusterName),
      ]);
      const parsed = parseDashboardPayload(detail.configs || "");
      setDashboard(detail);
      setSavedDashboard({ ...detail });
      setPayload(parsed);
      setSavedPayload(cloneDashboardPayload(parsed));
      setRawPayload(JSON.stringify(parsed, null, 2));
      setDatasources(sourceItems);
      setVariables(initialVariableValues(parsed, sourceItems));
      setIsEditing(false);
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : "Unable to load dashboard");
    } finally {
      setLoading(false);
    }
  }, [clusterName, dashboardId]);

  useEffect(() => { void load(); }, [load]);

  useEffect(() => {
    if (!dashboard || embedded || !canManage || dashboard.built_in) return;
    if (searchParams.get("edit") === "1") setIsEditing(true);
  }, [canManage, dashboard, embedded, searchParams]);

  useEffect(() => {
    if (!dirty) return undefined;
    const warn = (event: BeforeUnloadEvent) => event.preventDefault();
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [dirty]);

  const refresh = useCallback(() => {
    if (rangeMinutes > 0) {
      const now = new Date();
      setEnd(toLocalInput(now));
      setStart(toLocalInput(new Date(now.getTime() - rangeMinutes * 60_000)));
    }
    setRefreshKey((value) => value + 1);
  }, [rangeMinutes]);

  useEffect(() => {
    if (!autoRefresh) return undefined;
    const timer = window.setInterval(refresh, autoRefresh * 1000);
    return () => window.clearInterval(timer);
  }, [autoRefresh, refresh]);

  const beginEditing = () => {
    if (embedded && dashboard) {
      navigate(`/main/monitoring/dashboards/${dashboard.ident || dashboard.id}?edit=1`);
      return;
    }
    setIsEditing(true);
    setSearchParams({ edit: "1" }, { replace: true });
  };

  const discardChanges = () => {
    if (dirty && !window.confirm("Discard all unsaved dashboard changes?")) return;
    if (savedDashboard) setDashboard({ ...savedDashboard });
    setPayload(cloneDashboardPayload(savedPayload));
    setRawPayload(JSON.stringify(savedPayload, null, 2));
    setIsEditing(false);
    setSearchParams({}, { replace: true });
  };

  const save = async () => {
    if (!dashboard || dashboard.built_in) return;
    setSaving(true);
    try {
      const canonical = normalizeDashboardPayload(payload);
      let updated = dashboard;
      if (metadataDirty) {
        updated = await MetricsApi.updateDashboard(clusterName, dashboard.id, {
          name: dashboard.name,
          tags: dashboard.tags || "",
          public: dashboard.public,
        });
      }
      if (payloadDirty) {
        updated = await MetricsApi.updateDashboardConfigs(clusterName, dashboard.id, JSON.stringify(canonical));
      }
      const nextDashboard = { ...dashboard, ...updated };
      setDashboard(nextDashboard);
      setSavedDashboard({ ...nextDashboard });
      setPayload(canonical);
      setSavedPayload(cloneDashboardPayload(canonical));
      setRawPayload(JSON.stringify(canonical, null, 2));
      toast.success("Dashboard saved");
    } catch (caught: unknown) {
      toast.error(caught instanceof Error ? caught.message : "Unable to save dashboard");
    } finally {
      setSaving(false);
    }
  };

  const cloneAndEdit = async () => {
    if (!dashboard) return;
    setCloning(true);
    try {
      const cloned = await MetricsApi.cloneDashboard(clusterName, dashboard.id);
      toast.success("Editable dashboard copy created");
      navigate(`/main/monitoring/dashboards/${cloned.ident || cloned.id}?edit=1`);
    } catch (caught: unknown) {
      toast.error(caught instanceof Error ? caught.message : "Unable to clone dashboard");
    } finally {
      setCloning(false);
    }
  };

  const applyJson = () => {
    try {
      const parsed = parseDashboardPayload(rawPayload);
      setPayload(parsed);
      setVariables((current) => ({ ...initialVariableValues(parsed, datasources), ...current }));
      setRawPayload(JSON.stringify(parsed, null, 2));
      setShowJson(false);
    } catch (caught: unknown) {
      toast.error(caught instanceof Error ? caught.message : "Invalid dashboard payload");
    }
  };

  const savePanel = (panel: Panel) => {
    setPayload((current) => {
      const exists = current.panels.some((item) => item.id === panel.id);
      return {
        ...current,
        panels: exists
          ? current.panels.map((item) => item.id === panel.id ? panel : item)
          : [...current.panels, panel],
      };
    });
    setPanelEditor(null);
  };

  const removePanel = (panel: Panel) => {
    if (!window.confirm(`Delete panel "${panel.name}"?`)) return;
    setPayload((current) => ({ ...current, panels: current.panels.filter((item) => item.id !== panel.id) }));
  };

  const duplicatePanel = (panel: Panel) => setPayload((current) => ({
    ...current,
    panels: [...current.panels, duplicateDashboardPanel(panel, current.panels)],
  }));

  const openNewPanel = (type: Panel["type"]) => {
    const defaultDatasource = datasources.find((item) => item.status === "enabled" && item.is_default)
      || datasources.find((item) => item.status === "enabled" && (item.category === "prometheus" || item.plugin_type === "prometheus"));
    setPanelEditor(createDashboardPanel(type, payload.panels, defaultDatasource?.id));
  };

  if (loading) return <div className="monitoring-empty"><Spinner size="sm" className="me-2" />Loading dashboard</div>;
  if (error || !dashboard) return <Alert variant="danger">{error || "Dashboard not found"}</Alert>;

  const panels = payload.panels || [];
  const startSeconds = Math.floor(new Date(start).getTime() / 1000);
  const endSeconds = Math.floor(new Date(end).getTime() / 1000);
  const runtimeVariables = { ...variables, cluster: clusterName };

  return (
    <section className={`dashboard-workspace ${isEditing ? "dashboard-workspace-editing" : ""}`}>
      <div className="dashboard-workspace-header">
        <div className="dashboard-workspace-identity">
          {!embedded && <Link className="btn btn-sm btn-outline-secondary" title="Back to dashboards" to="/main/monitoring/dashboards" onClick={(event) => {
            if (dirty && !window.confirm("Leave this dashboard and discard unsaved changes?")) event.preventDefault();
          }}><FontAwesomeIcon icon={faArrowLeft} /></Link>}
          <div className="dashboard-workspace-title">
            <div className="d-flex align-items-center gap-2">
              <h2>{dashboard.name}</h2>
              {dashboard.built_in ? <Badge bg="info">Built in</Badge> : null}
              {isEditing && <Badge bg={dirty ? "warning" : "secondary"} text={dirty ? "dark" : undefined}>{dirty ? "Unsaved" : "Editing"}</Badge>}
            </div>
            <div className="dashboard-workspace-meta">{dashboard.tags || "No tags"}<span />Updated {new Date(dashboard.update_at * 1000).toLocaleString()}</div>
          </div>
        </div>
        {canManage && <div className="dashboard-workspace-actions">
          {dashboard.built_in
            ? <Button size="sm" variant="success" disabled={cloning} onClick={() => void cloneAndEdit()}>{cloning ? <Spinner size="sm" className="me-2" /> : <FontAwesomeIcon icon={faClone} className="me-2" />}Customize charts</Button>
            : isEditing ? <>
              <Dropdown>
                <Dropdown.Toggle size="sm" variant="outline-secondary"><FontAwesomeIcon icon={faPlus} className="me-2" />Panel</Dropdown.Toggle>
                <Dropdown.Menu className="dashboard-add-panel-menu">
                  {PANEL_TYPE_OPTIONS.map((option) => <Dropdown.Item key={option.value} onClick={() => openNewPanel(option.value)}>{option.label}</Dropdown.Item>)}
                </Dropdown.Menu>
              </Dropdown>
              <Button size="sm" variant="outline-secondary" title="Dashboard variables" onClick={() => setShowVariables(true)}><FontAwesomeIcon icon={faSliders} /><span>Variables</span></Button>
              <Button size="sm" variant="outline-secondary" title="Dashboard settings" onClick={() => setShowSettings(true)}><FontAwesomeIcon icon={faGear} /></Button>
              <Button size="sm" variant="outline-secondary" title="Edit dashboard JSON" onClick={() => { setRawPayload(JSON.stringify(payload, null, 2)); setShowJson(true); }}><FontAwesomeIcon icon={faCode} /></Button>
              <Button size="sm" variant="outline-secondary" title="Discard changes" onClick={discardChanges}><FontAwesomeIcon icon={faXmark} /></Button>
              <Button size="sm" variant="success" disabled={saving || !dirty} onClick={() => void save()}>{saving ? <Spinner size="sm" className="me-2" /> : <FontAwesomeIcon icon={faFloppyDisk} className="me-2" />}Save</Button>
            </> : <Button size="sm" variant="success" onClick={beginEditing}><FontAwesomeIcon icon={faPen} className="me-2" />Edit charts</Button>}
        </div>}
      </div>

      <div className="dashboard-runtime-toolbar">
        <div className="dashboard-variable-controls">
          {payload.var.map((variable, index) => {
            const name = variable.name;
            if (RESERVED_DASHBOARD_VARIABLES.has(name)) return null;
            if (variable.type === "datasource") {
              const category = variable.definition || "prometheus";
              const options = datasources.filter((item) => item.status === "enabled" && (item.category === category || item.plugin_type === category));
              return <Form.Group key={`${name}-${index}`}><Form.Label>{variable.label || name}</Form.Label><Form.Select size="sm" value={String(variables[name] ?? "")} onChange={(event) => setVariables({ ...variables, [name]: Number(event.target.value) })}>{options.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</Form.Select></Form.Group>;
            }
            return <Form.Group key={`${name}-${index}`}><Form.Label>{variable.label || name}</Form.Label><Form.Control size="sm" value={String(variables[name] ?? "")} onChange={(event) => setVariables({ ...variables, [name]: event.target.value })} /></Form.Group>;
          })}
        </div>
        <div className="dashboard-time-controls">
          <Form.Select aria-label="Time range" size="sm" value={rangeMinutes} onChange={(event) => {
            const minutes = Number(event.target.value);
            setRangeMinutes(minutes);
            if (minutes > 0) {
              const now = new Date();
              setEnd(toLocalInput(now));
              setStart(toLocalInput(new Date(now.getTime() - minutes * 60_000)));
            }
          }}>
            <option value={15}>Last 15 minutes</option><option value={60}>Last hour</option><option value={360}>Last 6 hours</option><option value={1440}>Last 24 hours</option><option value={10080}>Last 7 days</option><option value={0}>Custom range</option>
          </Form.Select>
          {rangeMinutes === 0 && <><Form.Control aria-label="Start time" size="sm" type="datetime-local" value={start} onChange={(event) => setStart(event.target.value)} /><Form.Control aria-label="End time" size="sm" type="datetime-local" value={end} onChange={(event) => setEnd(event.target.value)} /></>}
          <Form.Select aria-label="Auto refresh" title="Auto refresh" size="sm" className="dashboard-auto-refresh" value={autoRefresh} onChange={(event) => setAutoRefresh(Number(event.target.value))}>
            <option value={0}>Refresh off</option><option value={10}>Every 10s</option><option value={30}>Every 30s</option><option value={60}>Every 1m</option><option value={300}>Every 5m</option>
          </Form.Select>
          <Button size="sm" variant="outline-secondary" title="Refresh panels" onClick={refresh}><FontAwesomeIcon icon={faRotate} /></Button>
        </div>
      </div>

      {panels.length === 0
        ? <div className="dashboard-empty-canvas"><strong>No panels yet</strong>{isEditing ? <Button variant="success" size="sm" onClick={() => openNewPanel("timeseries")}><FontAwesomeIcon icon={faPlus} className="me-2" />Add first panel</Button> : <span>This dashboard does not contain any panels.</span>}</div>
        : <DashboardLayout
          panels={panels}
          editable={isEditing}
          onLayoutChange={(layout) => setPayload((current) => ({ ...current, panels: applyDashboardLayout(current.panels, layout) }))}
          renderActions={isEditing ? (panel) => <>
            <Button variant="light" size="sm" title="Edit panel" onClick={() => setPanelEditor(panel)}><FontAwesomeIcon icon={faPen} /></Button>
            <Button variant="light" size="sm" title="Copy panel" onClick={() => duplicatePanel(panel)}><FontAwesomeIcon icon={faCopy} /></Button>
            <Button variant="light" size="sm" className="text-danger" title="Delete panel" onClick={() => removePanel(panel)}><FontAwesomeIcon icon={faTrash} /></Button>
          </> : undefined}
          renderPanel={(panel, layout) => <DashboardPanel panel={panel} panelHeight={dashboardPanelHeight(layout)} start={startSeconds} end={endSeconds} refreshKey={refreshKey} variables={runtimeVariables} datasources={datasources} allowShare={!isEditing} graphTooltip={payload.graphTooltip} />}
        />}

      <DashboardPanelEditor show={Boolean(panelEditor)} panel={panelEditor} datasources={datasources} start={startSeconds} end={endSeconds} refreshKey={refreshKey} variables={runtimeVariables} onCancel={() => setPanelEditor(null)} onSave={savePanel} />
      <DashboardVariablesDialog show={showVariables} variables={payload.var} onCancel={() => setShowVariables(false)} onApply={(nextVariables) => {
        setPayload((current) => ({ ...current, var: nextVariables }));
        setVariables((current) => ({ ...initialVariableValues({ ...payload, var: nextVariables }, datasources), ...current }));
        setShowVariables(false);
      }} />
      <DashboardSettingsDialog show={showSettings} dashboard={dashboard} payload={payload} onCancel={() => setShowSettings(false)} onApply={(nextDashboard, nextPayload) => {
        setDashboard(nextDashboard);
        setPayload(nextPayload);
        setShowSettings(false);
      }} />
      <Modal show={showJson} onHide={() => setShowJson(false)} size="xl" centered>
        <Modal.Header closeButton><Modal.Title>Dashboard JSON</Modal.Title></Modal.Header>
        <Modal.Body><Alert variant="info" className="small">Advanced editor for the Ambari Monitoring {DASHBOARD_SCHEMA_VERSION} schema. Changes remain in the workspace until the dashboard is saved.</Alert><Form.Control className="monitoring-code" as="textarea" rows={28} value={rawPayload} onChange={(event) => setRawPayload(event.target.value)} /></Modal.Body>
        <Modal.Footer><Button variant="outline-secondary" onClick={() => setShowJson(false)}>Cancel</Button><Button variant="success" onClick={applyJson}>Apply JSON</Button></Modal.Footer>
      </Modal>
    </section>
  );
}
