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

import { FormEvent, useCallback, useContext, useEffect, useState } from "react";
import {
  Alert,
  Badge,
  Button,
  Form,
  Modal,
  Spinner,
  Table,
} from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faPen, faPlug, faPlus, faRotate, faTrash } from "@fortawesome/free-solid-svg-icons";
import toast from "react-hot-toast";
import MetricsApi from "../../api/metricsApi";
import { AppContext } from "../../store/context";
import { useAuth } from "../../hooks/useAuth";
import { Datasource, DatasourceInput, JsonObject } from "./types";

const BUILTIN_PLUGINS = [
  ["prometheus", "Prometheus"],
  ["elasticsearch", "Elasticsearch"],
  ["loki", "Loki"],
  ["jaeger", "Jaeger"],
  ["tdengine", "TDengine"],
] as const;

interface EditorState {
  id?: number;
  name: string;
  description: string;
  pluginType: string;
  pluginTypeName: string;
  pluginId: number;
  category: string;
  url: string;
  timeout: string;
  skipTlsVerify: boolean;
  settings: string;
  headers: string;
  username: string;
  password: string;
  status: "enabled" | "disabled";
  isDefault: boolean;
}

const emptyEditor = (): EditorState => ({
  name: "",
  description: "",
  pluginType: "prometheus",
  pluginTypeName: "Prometheus",
  pluginId: 0,
  category: "prometheus",
  url: "",
  timeout: "10000",
  skipTlsVerify: false,
  settings: "{}",
  headers: "[]",
  username: "",
  password: "",
  status: "enabled",
  isDefault: false,
});

const formatted = (value: unknown, fallback: unknown) => JSON.stringify(value ?? fallback, null, 2);

const editorFromDatasource = (datasource: Datasource): EditorState => ({
  id: datasource.id,
  name: datasource.name,
  description: datasource.description || "",
  pluginType: datasource.plugin_type,
  pluginTypeName: datasource.plugin_type_name || datasource.plugin_type,
  pluginId: datasource.plugin_id,
  category: datasource.category || datasource.plugin_type,
  url: typeof datasource.http.url === "string" ? datasource.http.url : "",
  timeout: String(datasource.http.timeout ?? 10000),
  skipTlsVerify: Boolean((datasource.http.tls as JsonObject | undefined)?.skip_tls_verify),
  settings: formatted(datasource.settings, {}),
  headers: formatted(datasource.http.headers, []),
  username: "",
  password: "",
  status: datasource.status,
  isDefault: datasource.is_default,
});

const parseObject = (value: string, name: string): JsonObject => {
  const parsed: unknown = JSON.parse(value);
  if (!parsed || Array.isArray(parsed) || typeof parsed !== "object") {
    throw new Error(`${name} must be a JSON object`);
  }
  return parsed as JsonObject;
};

const parseHeaders = (value: string): JsonObject | unknown[] => {
  const parsed: unknown = JSON.parse(value);
  if (!parsed || (!Array.isArray(parsed) && typeof parsed !== "object")) {
    throw new Error("Headers must be a JSON object or array");
  }
  return parsed as JsonObject | unknown[];
};

export default function Datasources() {
  const { clusterName } = useContext(AppContext);
  const { hasAuthorization } = useAuth();
  const canManage = hasAuthorization("AMBARI.MANAGE_SETTINGS");
  const [datasources, setDatasources] = useState<Datasource[]>([]);
  const [plugins, setPlugins] = useState<Array<{ type: string; name: string }>>(
    BUILTIN_PLUGINS.map(([type, name]) => ({ type, name })),
  );
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showEditor, setShowEditor] = useState(false);
  const [editor, setEditor] = useState<EditorState>(emptyEditor());
  const [original, setOriginal] = useState<EditorState | null>(null);
  const [saving, setSaving] = useState(false);
  const [testing, setTesting] = useState(false);

  const load = useCallback(async () => {
    if (!clusterName) return;
    setLoading(true);
    setError("");
    try {
      const [items, pluginItems] = await Promise.all([
        MetricsApi.listDatasources(clusterName),
        MetricsApi.listDatasourcePlugins(clusterName),
      ]);
      setDatasources(items);
      setPlugins(pluginItems);
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : "Unable to load datasources");
    } finally {
      setLoading(false);
    }
  }, [clusterName]);

  useEffect(() => {
    void load();
  }, [load]);

  const openCreate = () => {
    const next = emptyEditor();
    setEditor(next);
    setOriginal(null);
    setShowEditor(true);
  };

  const openEdit = (datasource: Datasource) => {
    const next = editorFromDatasource(datasource);
    setEditor(next);
    setOriginal(next);
    setShowEditor(true);
  };

  const save = async (event: FormEvent) => {
    event.preventDefault();
    if (!clusterName) return;
    setSaving(true);
    try {
      const settingsChanged = !original || editor.settings !== original.settings;
      const headersChanged = !original || editor.headers !== original.headers;
      const httpChanged = !original
        || editor.url !== original.url
        || editor.timeout !== original.timeout
        || editor.skipTlsVerify !== original.skipTlsVerify
        || headersChanged;
      const input: DatasourceInput = {
        id: editor.id,
        name: editor.name.trim(),
        description: editor.description,
        category: editor.category,
        plugin_id: editor.pluginId,
        plugin_type: editor.pluginType,
        plugin_type_name: editor.pluginTypeName || editor.pluginType,
        cluster_name: clusterName,
        status: editor.status,
        is_default: editor.isDefault,
      };
      if (settingsChanged) {
        input.settings = parseObject(editor.settings, "Settings");
      }
      if (httpChanged) {
        const httpPatch: JsonObject = {};
        if (!original || editor.url !== original.url) httpPatch.url = editor.url.trim();
        if (!original || editor.timeout !== original.timeout) httpPatch.timeout = Number(editor.timeout) || 10000;
        if (!original || editor.skipTlsVerify !== original.skipTlsVerify) {
          httpPatch.tls = { skip_tls_verify: editor.skipTlsVerify };
        }
        if (headersChanged) httpPatch.headers = parseHeaders(editor.headers);
        input.http = httpPatch;
      }
      if (editor.username || editor.password) {
        input.auth = {
          basic_auth_user: editor.username,
          basic_auth_password: editor.password,
        };
      }
      await MetricsApi.saveDatasource(input);
      setShowEditor(false);
      toast.success(editor.id ? "Datasource updated" : "Datasource created");
      await load();
    } catch (caught: unknown) {
      toast.error(caught instanceof Error ? caught.message : "Unable to save datasource");
    } finally {
      setSaving(false);
    }
  };

  const toggleStatus = async (datasource: Datasource) => {
    const status = datasource.status === "enabled" ? "disabled" : "enabled";
    try {
      await MetricsApi.updateDatasourceStatus(clusterName, datasource.id, status);
      await load();
    } catch (caught: unknown) {
      toast.error(caught instanceof Error ? caught.message : "Unable to update datasource");
    }
  };

  const testConnection = async () => {
    if (!editor.id || !clusterName) return;
    setTesting(true);
    try {
      await MetricsApi.testDatasource(clusterName, editor.id);
      toast.success("Datasource connection succeeded");
    } catch (caught: unknown) {
      toast.error(caught instanceof Error ? caught.message : "Datasource connection failed");
    } finally {
      setTesting(false);
    }
  };

  const remove = async (datasource: Datasource) => {
    if (!window.confirm(`Delete datasource "${datasource.name}"?`)) return;
    try {
      await MetricsApi.deleteDatasource(clusterName, datasource.id);
      toast.success("Datasource deleted");
      await load();
    } catch (caught: unknown) {
      toast.error(caught instanceof Error ? caught.message : "Unable to delete datasource");
    }
  };

  return (
    <section>
      <div className="monitoring-toolbar">
        <div>
          <h2 className="h4 mb-1">Datasources</h2>
          <div className="text-muted small">Connections are scoped to {clusterName || "the current cluster"}.</div>
        </div>
        {canManage && (
          <Button variant="success" size="sm" onClick={openCreate}>
            <FontAwesomeIcon icon={faPlus} className="me-2" />Add datasource
          </Button>
        )}
      </div>
      {error && <Alert variant="danger">{error}</Alert>}
      <div className="monitoring-panel overflow-hidden">
        {loading ? (
          <div className="monitoring-empty"><Spinner size="sm" className="me-2" />Loading datasources</div>
        ) : datasources.length === 0 ? (
          <div className="monitoring-empty">No datasources are configured for this cluster.</div>
        ) : (
          <Table responsive hover className="mb-0 align-middle">
            <thead><tr><th>Name</th><th>Type</th><th>Endpoint</th><th>Status</th><th>Updated</th><th aria-label="Actions" /></tr></thead>
            <tbody>
              {datasources.map((datasource) => (
                <tr key={datasource.id}>
                  <td><strong>{datasource.name}</strong>{datasource.is_default && <Badge bg="secondary" className="ms-2">Default</Badge>}<div className="text-muted small">{datasource.description}</div></td>
                  <td>{datasource.plugin_type_name || datasource.plugin_type}</td>
                  <td className="monitoring-code text-break">{String(datasource.http.url || "Not set")}</td>
                  <td><Badge bg={datasource.status === "enabled" ? "success" : "secondary"}>{datasource.status}</Badge></td>
                  <td>{datasource.updated_at ? new Date(datasource.updated_at * 1000).toLocaleString() : "-"}</td>
                  <td className="text-end text-nowrap">
                    {canManage && <>
                      <Button variant="link" size="sm" title="Edit datasource" onClick={() => openEdit(datasource)}><FontAwesomeIcon icon={faPen} /></Button>
                      <Button variant="link" size="sm" title={datasource.status === "enabled" ? "Disable datasource" : "Enable datasource"} onClick={() => void toggleStatus(datasource)}><FontAwesomeIcon icon={faRotate} /></Button>
                      <Button variant="link" size="sm" className="text-danger" title="Delete datasource" onClick={() => void remove(datasource)}><FontAwesomeIcon icon={faTrash} /></Button>
                    </>}
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </div>

      <Modal show={showEditor} onHide={() => setShowEditor(false)} size="lg" centered>
        <Form onSubmit={save}>
          <Modal.Header closeButton><Modal.Title>{editor.id ? "Edit datasource" : "Add datasource"}</Modal.Title></Modal.Header>
          <Modal.Body>
            <div className="row g-3">
              <Form.Group className="col-md-7"><Form.Label>Name</Form.Label><Form.Control required maxLength={191} value={editor.name} onChange={(event) => setEditor({ ...editor, name: event.target.value })} /></Form.Group>
              <Form.Group className="col-md-5"><Form.Label>Type</Form.Label><Form.Select value={editor.pluginType} onChange={(event) => {
                const plugin = plugins.find((item) => item.type === event.target.value);
                setEditor({
                  ...editor,
                  pluginType: event.target.value,
                  pluginTypeName: plugin?.name || event.target.value,
                  pluginId: 0,
                  category: event.target.value,
                });
              }}>{plugins.map((plugin) => <option key={plugin.type} value={plugin.type}>{plugin.name}</option>)}</Form.Select></Form.Group>
              <Form.Group className="col-12"><Form.Label>Description</Form.Label><Form.Control value={editor.description} onChange={(event) => setEditor({ ...editor, description: event.target.value })} /></Form.Group>
              <Form.Group className="col-md-8"><Form.Label>HTTP URL</Form.Label><Form.Control required={!editor.id} type="url" placeholder="http://prometheus:9090" value={editor.url} onChange={(event) => setEditor({ ...editor, url: event.target.value })} /></Form.Group>
              <Form.Group className="col-md-4"><Form.Label>Timeout (ms)</Form.Label><Form.Control type="number" min={1} max={60000} value={editor.timeout} onChange={(event) => setEditor({ ...editor, timeout: event.target.value })} /></Form.Group>
              <Form.Group className="col-md-6"><Form.Label>Basic auth username</Form.Label><Form.Control autoComplete="off" value={editor.username} onChange={(event) => setEditor({ ...editor, username: event.target.value })} /></Form.Group>
              <Form.Group className="col-md-6"><Form.Label>Basic auth password</Form.Label><Form.Control type="password" autoComplete="new-password" placeholder={editor.id ? "Leave blank to keep current credentials" : ""} value={editor.password} onChange={(event) => setEditor({ ...editor, password: event.target.value })} /></Form.Group>
              <Form.Group className="col-12"><Form.Check label="Skip TLS certificate verification" checked={editor.skipTlsVerify} onChange={(event) => setEditor({ ...editor, skipTlsVerify: event.target.checked })} /></Form.Group>
              <Form.Group className="col-md-6"><Form.Label>Plugin settings (JSON)</Form.Label><Form.Control className="monitoring-code" as="textarea" rows={7} value={editor.settings} onChange={(event) => setEditor({ ...editor, settings: event.target.value })} /></Form.Group>
              <Form.Group className="col-md-6"><Form.Label>HTTP headers (JSON object or array)</Form.Label><Form.Control className="monitoring-code" as="textarea" rows={7} value={editor.headers} onChange={(event) => setEditor({ ...editor, headers: event.target.value })} /></Form.Group>
              <Form.Group className="col-md-5"><Form.Label>Status</Form.Label><Form.Select value={editor.status} onChange={(event) => setEditor({ ...editor, status: event.target.value as "enabled" | "disabled" })}><option value="enabled">Enabled</option><option value="disabled">Disabled</option></Form.Select></Form.Group>
              <Form.Group className="col-md-7 d-flex align-items-end pb-2"><Form.Check label="Use as the default datasource for this cluster" checked={editor.isDefault} onChange={(event) => setEditor({ ...editor, isDefault: event.target.checked })} /></Form.Group>
            </div>
          </Modal.Body>
          <Modal.Footer>
            {editor.id && <Button variant="outline-primary" disabled={testing} onClick={() => void testConnection()}><FontAwesomeIcon icon={faPlug} className="me-2" />{testing ? "Testing" : "Test connection"}</Button>}
            <Button variant="outline-secondary" onClick={() => setShowEditor(false)}>Cancel</Button>
            <Button variant="success" type="submit" disabled={saving}>{saving && <Spinner size="sm" className="me-2" />}Save</Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </section>
  );
}
