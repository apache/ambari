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

import { ChangeEvent, FormEvent, useCallback, useContext, useEffect, useRef, useState } from "react";
import { Alert, Badge, Button, Form, InputGroup, Modal, Spinner, Table } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faClone, faDownload, faPlus, faSearch, faTrash, faUpload } from "@fortawesome/free-solid-svg-icons";
import { saveAs } from "file-saver";
import toast from "react-hot-toast";
import { Link } from "react-router-dom";
import MetricsApi from "../../api/metricsApi";
import { AppContext } from "../../store/context";
import { useAuth } from "../../hooks/useAuth";
import { Dashboard, DashboardInput } from "./types";

const emptyPayload = JSON.stringify({ version: "3.0.0", var: [], panels: [] });

export default function Dashboards() {
  const { clusterName } = useContext(AppContext);
  const { hasAuthorization } = useAuth();
  const canManage = hasAuthorization("CLUSTER.MANAGE_USER_PERSISTED_DATA");
  const fileInput = useRef<HTMLInputElement>(null);
  const [dashboards, setDashboards] = useState<Dashboard[]>([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showCreate, setShowCreate] = useState(false);
  const [name, setName] = useState("");
  const [ident, setIdent] = useState("");
  const [tags, setTags] = useState("");
  const [saving, setSaving] = useState(false);

  const load = useCallback(async (filter: string) => {
    if (!clusterName) return;
    setLoading(true);
    setError("");
    try {
      setDashboards(await MetricsApi.listDashboards(clusterName, filter));
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : "Unable to load dashboards");
    } finally {
      setLoading(false);
    }
  }, [clusterName]);

  useEffect(() => { void load(""); }, [load]);

  const create = async (event: FormEvent) => {
    event.preventDefault();
    setSaving(true);
    try {
      await MetricsApi.createDashboard(clusterName, {
        name: name.trim(), ident: ident.trim(), tags, configs: emptyPayload,
      });
      setShowCreate(false);
      setName(""); setIdent(""); setTags("");
      toast.success("Dashboard created");
      await load(query);
    } catch (caught: unknown) {
      toast.error(caught instanceof Error ? caught.message : "Unable to create dashboard");
    } finally {
      setSaving(false);
    }
  };

  const clone = async (dashboard: Dashboard) => {
    try {
      await MetricsApi.cloneDashboard(clusterName, dashboard.id);
      toast.success("Dashboard cloned");
      await load(query);
    } catch (caught: unknown) {
      toast.error(caught instanceof Error ? caught.message : "Unable to clone dashboard");
    }
  };

  const remove = async (dashboard: Dashboard) => {
    if (!window.confirm(`Delete dashboard "${dashboard.name}"?`)) return;
    try {
      await MetricsApi.deleteDashboard(clusterName, dashboard.id);
      toast.success("Dashboard deleted");
      await load(query);
    } catch (caught: unknown) {
      toast.error(caught instanceof Error ? caught.message : "Unable to delete dashboard");
    }
  };

  const exportDashboard = async (dashboard: Dashboard) => {
    try {
      const detail = await MetricsApi.getDashboard(clusterName, dashboard.id);
      const document = { ...detail, configs: detail.configs ? JSON.parse(detail.configs) : {} };
      saveAs(new Blob([JSON.stringify(document, null, 2)], { type: "application/json" }), `${dashboard.ident || dashboard.id}.json`);
    } catch (caught: unknown) {
      toast.error(caught instanceof Error ? caught.message : "Unable to export dashboard");
    }
  };

  const importDashboard = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;
    try {
      const parsed = JSON.parse(await file.text()) as DashboardInput & { configs?: unknown };
      if (!parsed.name) throw new Error("Imported dashboard is missing a name");
      const configs = typeof parsed.configs === "string" ? parsed.configs : JSON.stringify(parsed.configs || {});
      await MetricsApi.createDashboard(clusterName, {
        name: parsed.name,
        ident: "",
        tags: parsed.tags || "",
        display_locations: parsed.display_locations || "",
        configs,
      });
      toast.success("Dashboard imported");
      await load(query);
    } catch (caught: unknown) {
      toast.error(caught instanceof Error ? caught.message : "Unable to import dashboard");
    }
  };

  return (
    <section>
      <div className="monitoring-toolbar">
        <div><h2 className="h4 mb-1">Dashboards</h2><div className="text-muted small">Existing 3.0 dashboard IDs and payloads remain authoritative.</div></div>
        {canManage && <div className="d-flex gap-2"><input ref={fileInput} type="file" accept="application/json,.json" hidden onChange={(event) => void importDashboard(event)} /><Button variant="outline-secondary" size="sm" onClick={() => fileInput.current?.click()}><FontAwesomeIcon icon={faUpload} className="me-2" />Import</Button><Button variant="success" size="sm" onClick={() => setShowCreate(true)}><FontAwesomeIcon icon={faPlus} className="me-2" />New dashboard</Button></div>}
      </div>
      <Form onSubmit={(event) => { event.preventDefault(); void load(query); }} className="mb-3"><InputGroup><InputGroup.Text><FontAwesomeIcon icon={faSearch} /></InputGroup.Text><Form.Control placeholder="Search names and tags; prefix a term with - to exclude it" value={query} onChange={(event) => setQuery(event.target.value)} /><Button type="submit" variant="outline-secondary">Search</Button></InputGroup></Form>
      {error && <Alert variant="danger">{error}</Alert>}
      <div className="monitoring-panel overflow-hidden">
        {loading ? <div className="monitoring-empty"><Spinner size="sm" className="me-2" />Loading dashboards</div> : dashboards.length === 0 ? <div className="monitoring-empty">No dashboards match this filter.</div> : <Table responsive hover className="mb-0 align-middle"><thead><tr><th>Name</th><th>Tags</th><th>Visibility</th><th>Updated</th><th aria-label="Actions" /></tr></thead><tbody>{dashboards.map((dashboard) => <tr key={dashboard.id}><td><Link to={`/main/monitoring/dashboards/${dashboard.ident || dashboard.id}`}><strong>{dashboard.name}</strong></Link><div className="text-muted small">ID {dashboard.id}{dashboard.ident ? ` / ${dashboard.ident}` : ""}</div></td><td>{dashboard.tags || "-"}</td><td><Badge bg={dashboard.public ? "success" : "secondary"}>{dashboard.public ? "Public" : "Private"}</Badge>{dashboard.built_in ? <Badge bg="info" className="ms-1">Built in</Badge> : null}</td><td>{new Date(dashboard.update_at * 1000).toLocaleString()}</td><td className="text-end text-nowrap"><Button variant="link" size="sm" title="Export dashboard" onClick={() => void exportDashboard(dashboard)}><FontAwesomeIcon icon={faDownload} /></Button>{canManage && <><Button variant="link" size="sm" title="Clone dashboard" onClick={() => void clone(dashboard)}><FontAwesomeIcon icon={faClone} /></Button><Button variant="link" size="sm" className="text-danger" title="Delete dashboard" onClick={() => void remove(dashboard)}><FontAwesomeIcon icon={faTrash} /></Button></>}</td></tr>)}</tbody></Table>}
      </div>
      <Modal show={showCreate} onHide={() => setShowCreate(false)} centered><Form onSubmit={create}><Modal.Header closeButton><Modal.Title>New dashboard</Modal.Title></Modal.Header><Modal.Body><Form.Group className="mb-3"><Form.Label>Name</Form.Label><Form.Control required maxLength={191} value={name} onChange={(event) => setName(event.target.value)} /></Form.Group><Form.Group className="mb-3"><Form.Label>Identifier</Form.Label><Form.Control maxLength={200} placeholder="Optional stable route identifier" value={ident} onChange={(event) => setIdent(event.target.value)} /></Form.Group><Form.Group><Form.Label>Tags</Form.Label><Form.Control value={tags} onChange={(event) => setTags(event.target.value)} /></Form.Group></Modal.Body><Modal.Footer><Button variant="outline-secondary" onClick={() => setShowCreate(false)}>Cancel</Button><Button variant="success" type="submit" disabled={saving}>{saving && <Spinner size="sm" className="me-2" />}Create</Button></Modal.Footer></Form></Modal>
    </section>
  );
}
