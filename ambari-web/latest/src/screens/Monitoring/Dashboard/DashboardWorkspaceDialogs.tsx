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

import { type FormEvent, useEffect, useState } from "react";
import { Alert, Button, Form, Modal } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faArrowDown, faArrowUp, faPlus, faTrash } from "@fortawesome/free-solid-svg-icons";
import type { Dashboard, DashboardPayload, DashboardVariable } from "../types";
import { RESERVED_DASHBOARD_VARIABLES } from "../utils";

interface VariablesDialogProps {
  show: boolean;
  variables: DashboardVariable[];
  onCancel: () => void;
  onApply: (variables: DashboardVariable[]) => void;
}

const moveItem = <T,>(items: T[], from: number, to: number) => {
  if (to < 0 || to >= items.length) return items;
  const result = [...items];
  const [item] = result.splice(from, 1);
  result.splice(to, 0, item);
  return result;
};

export function DashboardVariablesDialog({ show, variables, onCancel, onApply }: VariablesDialogProps) {
  const [draft, setDraft] = useState<DashboardVariable[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    setDraft(JSON.parse(JSON.stringify(variables)) as DashboardVariable[]);
    setError("");
  }, [show, variables]);

  const update = (index: number, values: Partial<DashboardVariable>) => setDraft((items) => (
    items.map((item, itemIndex) => itemIndex === index ? { ...item, ...values } : item)
  ));

  const submit = (event: FormEvent) => {
    event.preventDefault();
    const normalized = draft.map((variable) => ({
      ...variable,
      name: variable.name.trim(),
      label: variable.label?.trim() || undefined,
      definition: variable.definition?.trim() || undefined,
      value: variable.value?.trim() || undefined,
    }));
    const names = normalized.map((variable) => variable.name);
    const invalid = names.find((name) => !/^[A-Za-z_][A-Za-z0-9_]*$/.test(name));
    if (invalid !== undefined) {
      setError("Variable names must start with a letter or underscore and contain only letters, numbers, and underscores");
      return;
    }
    if (names.some((name) => RESERVED_DASHBOARD_VARIABLES.has(name))) {
      setError("cluster and __rate_interval are reserved Ambari variables");
      return;
    }
    if (new Set(names).size !== names.length) {
      setError("Variable names must be unique");
      return;
    }
    onApply(normalized);
  };

  return (
    <Modal show={show} onHide={onCancel} size="lg" centered>
      <Form onSubmit={submit}>
        <Modal.Header closeButton><Modal.Title>Dashboard variables</Modal.Title></Modal.Header>
        <Modal.Body>
          {error && <Alert variant="danger" className="py-2">{error}</Alert>}
          {draft.length === 0 && <div className="monitoring-empty dashboard-dialog-empty">No custom variables</div>}
          <div className="dashboard-variable-list">
            {draft.map((variable, index) => <div className="dashboard-variable-row" key={index}>
              <div className="dashboard-variable-order">
                <Button variant="link" size="sm" title="Move up" disabled={index === 0} onClick={() => setDraft((items) => moveItem(items, index, index - 1))}><FontAwesomeIcon icon={faArrowUp} /></Button>
                <Button variant="link" size="sm" title="Move down" disabled={index === draft.length - 1} onClick={() => setDraft((items) => moveItem(items, index, index + 1))}><FontAwesomeIcon icon={faArrowDown} /></Button>
              </div>
              <Form.Group>
                <Form.Label>Name</Form.Label>
                <Form.Control required size="sm" placeholder="host" value={variable.name} onChange={(event) => update(index, { name: event.target.value })} />
              </Form.Group>
              <Form.Group>
                <Form.Label>Label</Form.Label>
                <Form.Control size="sm" placeholder="Host" value={variable.label || ""} onChange={(event) => update(index, { label: event.target.value })} />
              </Form.Group>
              <Form.Group>
                <Form.Label>Type</Form.Label>
                <Form.Select size="sm" value={variable.type} onChange={(event) => update(index, {
                  type: event.target.value as DashboardVariable["type"],
                  definition: event.target.value === "datasource"
                    ? "prometheus"
                    : event.target.value === "query"
                      ? `max by (${variable.name || "host"}) (ambari_agent_host_info{cluster="\${cluster}",ambari_target="host"})`
                      : undefined,
                  value: event.target.value === "query" ? ".*" : variable.value,
                  includeAll: event.target.value === "query" ? true : undefined,
                })}>
                  <option value="textbox">Text box</option>
                  <option value="datasource">Data source</option>
                  <option value="query">Prometheus query</option>
                </Form.Select>
              </Form.Group>
              <Form.Group>
                <Form.Label>{variable.type === "datasource" ? "Category" : variable.type === "query" ? "PromQL" : "Default value"}</Form.Label>
                {variable.type === "datasource"
                  ? <Form.Select size="sm" value={variable.definition || "prometheus"} onChange={(event) => update(index, { definition: event.target.value })}><option value="prometheus">Prometheus</option></Form.Select>
                  : variable.type === "query"
                    ? <Form.Control className="monitoring-code" size="sm" value={variable.definition || ""} onChange={(event) => update(index, { definition: event.target.value })} />
                  : <Form.Control size="sm" value={variable.value || ""} onChange={(event) => update(index, { value: event.target.value })} />}
              </Form.Group>
              {variable.type === "query" && <div className="dashboard-variable-query-options"><Form.Check type="switch" label="Multiple" checked={variable.multi === true} onChange={(event) => update(index, { multi: event.target.checked })} /><Form.Check type="switch" label="Include all" checked={variable.includeAll === true} onChange={(event) => update(index, { includeAll: event.target.checked })} /></div>}
              <Button variant="link" size="sm" className="text-danger dashboard-variable-delete" title="Delete variable" onClick={() => setDraft((items) => items.filter((_item, itemIndex) => itemIndex !== index))}><FontAwesomeIcon icon={faTrash} /></Button>
            </div>)}
          </div>
          <Button variant="outline-secondary" size="sm" className="mt-3" onClick={() => setDraft((items) => [...items, { name: "", label: "", type: "textbox", value: "" }])}><FontAwesomeIcon icon={faPlus} className="me-2" />Variable</Button>
        </Modal.Body>
        <Modal.Footer><Button variant="outline-secondary" onClick={onCancel}>Cancel</Button><Button variant="success" type="submit">Apply variables</Button></Modal.Footer>
      </Form>
    </Modal>
  );
}

interface SettingsDialogProps {
  show: boolean;
  dashboard: Dashboard;
  payload: DashboardPayload;
  onCancel: () => void;
  onApply: (dashboard: Dashboard, payload: DashboardPayload) => void;
}

export function DashboardSettingsDialog({ show, dashboard, payload, onCancel, onApply }: SettingsDialogProps) {
  const [metadata, setMetadata] = useState<Dashboard>(dashboard);
  const [settings, setSettings] = useState<DashboardPayload>(payload);

  useEffect(() => {
    setMetadata({ ...dashboard });
    setSettings(JSON.parse(JSON.stringify(payload)) as DashboardPayload);
  }, [show, dashboard, payload]);

  const submit = (event: FormEvent) => {
    event.preventDefault();
    onApply({ ...metadata, name: metadata.name.trim(), tags: metadata.tags.trim() }, settings);
  };

  return (
    <Modal show={show} onHide={onCancel} centered>
      <Form onSubmit={submit}>
        <Modal.Header closeButton><Modal.Title>Dashboard settings</Modal.Title></Modal.Header>
        <Modal.Body>
          <Form.Group className="mb-3">
            <Form.Label>Name</Form.Label>
            <Form.Control required maxLength={191} value={metadata.name} onChange={(event) => setMetadata({ ...metadata, name: event.target.value })} />
          </Form.Group>
          <Form.Group className="mb-3">
            <Form.Label>Tags</Form.Label>
            <Form.Control value={metadata.tags || ""} onChange={(event) => setMetadata({ ...metadata, tags: event.target.value })} />
          </Form.Group>
          <Form.Check className="mb-3" type="switch" label="Visible to all cluster users" checked={Boolean(metadata.public)} onChange={(event) => setMetadata({ ...metadata, public: event.target.checked ? 1 : 0 })} />
          <Form.Group className="mb-3">
            <Form.Label>Tooltip behavior</Form.Label>
            <Form.Select value={settings.graphTooltip || "single"} onChange={(event) => setSettings({ ...settings, graphTooltip: event.target.value })}>
              <option value="single">Focused series</option>
              <option value="shared">All series at timestamp</option>
            </Form.Select>
          </Form.Group>
        </Modal.Body>
        <Modal.Footer><Button variant="outline-secondary" onClick={onCancel}>Cancel</Button><Button variant="success" type="submit">Apply settings</Button></Modal.Footer>
      </Form>
    </Modal>
  );
}
