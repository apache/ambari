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
import { faPlus, faTrash } from "@fortawesome/free-solid-svg-icons";
import type {
  DashboardPanel as Panel,
  DashboardPanelType,
  DashboardTarget,
  Datasource,
  JsonObject,
} from "../types";
import DashboardPanel from "./DashboardPanel";
import DashboardVisualizationOptions from "./DashboardVisualizationOptions";
import {
  createDashboardPanel,
  PANEL_TYPE_OPTIONS,
  STATIC_PANEL_TYPES,
} from "./dashboardWorkspace";

interface DashboardPanelEditorProps {
  show: boolean;
  panel: Panel | null;
  datasources: Datasource[];
  start: number;
  end: number;
  refreshKey: number;
  variables: Record<string, string | number | string[]>;
  onCancel: () => void;
  onSave: (panel: Panel) => void;
}

const UNIT_OPTIONS = [
  ["none", "None"],
  ["bytesIEC", "Bytes (IEC)"],
  ["bytesSecIEC", "Bytes/sec (IEC)"],
  ["bitsSecSI", "Bits/sec (SI)"],
  ["percent", "Percent (0-100)"],
  ["percentUnit", "Percent (0-1)"],
  ["seconds", "Seconds"],
  ["milliseconds", "Milliseconds"],
  ["cps", "Counts/sec"],
  ["reqps", "Requests/sec"],
  ["packetsSec", "Packets/sec"],
  ["iops", "Operations/sec"],
] as const;

const asObject = (value: unknown): JsonObject => (
  value && typeof value === "object" && !Array.isArray(value) ? value as JsonObject : {}
);

const optionalNumber = (value: string) => {
  if (value.trim() === "") return undefined;
  const numeric = Number(value);
  return Number.isFinite(numeric) ? numeric : undefined;
};

const pickerColor = (value: unknown) => {
  if (typeof value !== "string") return "#6c757d";
  if (/^#[0-9a-f]{6}$/i.test(value)) return value;
  if (/^#[0-9a-f]{3}$/i.test(value)) return `#${value.slice(1).split("").map((part) => part.repeat(2)).join("")}`;
  return "#6c757d";
};

const ColorEditor = ({ label, value, onChange }: {
  label: string;
  value: unknown;
  onChange: (value: string) => void;
}) => {
  const color = typeof value === "string" && value ? value : "#6c757d";
  return <div className="dashboard-color-editor">
    <Form.Control aria-label={`${label} picker`} size="sm" type="color" value={pickerColor(color)} onChange={(event) => onChange(event.target.value)} />
    <Form.Control aria-label={label} size="sm" value={color} onChange={(event) => onChange(event.target.value)} />
  </div>;
};

const nextRefId = (targets: DashboardTarget[]) => {
  const used = new Set(targets.map((target) => target.refId));
  for (let code = 65; code <= 90; code += 1) {
    const candidate = String.fromCharCode(code);
    if (!used.has(candidate)) return candidate;
  }
  return `Q${targets.length + 1}`;
};

export default function DashboardPanelEditor({
  show,
  panel,
  datasources,
  start,
  end,
  refreshKey,
  variables,
  onCancel,
  onSave,
}: DashboardPanelEditorProps) {
  const [draft, setDraft] = useState<Panel | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    setDraft(panel ? JSON.parse(JSON.stringify(panel)) as Panel : null);
    setError("");
  }, [panel, show]);

  if (!draft) return null;

  const isStatic = STATIC_PANEL_TYPES.has(draft.type);
  const options = asObject(draft.options);
  const standardOptions = asObject(options.standardOptions);
  const thresholdConfig = asObject(options.thresholds);
  const thresholds = Array.isArray(thresholdConfig.steps)
    ? thresholdConfig.steps.map(asObject)
    : [{ value: null, color: "green" }];
  const valueMappings = Array.isArray(options.valueMappings)
    ? options.valueMappings.map(asObject)
    : [];
  const custom = asObject(draft.custom);
  const availableDatasources = datasources.filter((item) => item.status === "enabled"
    && (item.category === "prometheus" || item.plugin_type === "prometheus"));

  const patchDraft = (values: Partial<Panel>) => setDraft((current) => current ? { ...current, ...values } : current);

  const setStandardOption = (name: string, value: unknown) => patchDraft({
    options: {
      ...options,
      standardOptions: {
        ...standardOptions,
        [name]: value,
      },
    },
  });

  const setThresholds = (steps: JsonObject[]) => patchDraft({
    options: {
      ...options,
      thresholds: {
        ...thresholdConfig,
        mode: "absolute",
        steps,
      },
    },
  });

  const setValueMappings = (mappings: JsonObject[]) => patchDraft({
    options: {
      ...options,
      valueMappings: mappings,
    },
  });

  const updateTarget = (index: number, values: Partial<DashboardTarget>) => patchDraft({
    targets: draft.targets.map((target, targetIndex) => targetIndex === index ? { ...target, ...values } : target),
  });

  const changeType = (type: DashboardPanelType) => {
    const defaults = createDashboardPanel(type, [], availableDatasources.find((item) => item.is_default)?.id
      || availableDatasources[0]?.id);
    const movingBetweenStaticAndQuery = STATIC_PANEL_TYPES.has(draft.type) !== STATIC_PANEL_TYPES.has(type);
    setDraft({
      ...draft,
      type,
      datasourceCate: STATIC_PANEL_TYPES.has(type) ? undefined : draft.datasourceCate || defaults.datasourceCate,
      datasourceValue: STATIC_PANEL_TYPES.has(type) ? undefined : draft.datasourceValue || defaults.datasourceValue,
      targets: movingBetweenStaticAndQuery ? defaults.targets : draft.targets,
      custom: STATIC_PANEL_TYPES.has(type) ? defaults.custom : STATIC_PANEL_TYPES.has(draft.type) ? defaults.custom : draft.custom,
      options: STATIC_PANEL_TYPES.has(type) ? undefined : draft.options || defaults.options,
      layout: {
        ...draft.layout,
        w: type === "row" ? 24 : Math.min(draft.layout.w, 24),
        h: type === "row" ? 1 : Math.max(draft.layout.h, 2),
        isResizable: type !== "row",
      },
    });
  };

  const submit = (event: FormEvent) => {
    event.preventDefault();
    const name = draft.name.trim();
    if (!name) {
      setError("Panel name is required");
      return;
    }
    if (!isStatic && (!draft.targets.length || draft.targets.some((target) => !target.expr.trim()))) {
      setError("Every query target requires a PromQL expression");
      return;
    }
    const normalizedTargets = draft.targets.map((target) => ({
      ...target,
      refId: target.refId.trim(),
      expr: target.expr.trim(),
      legend: target.legend?.trim() || undefined,
    }));
    onSave({
      ...draft,
      name,
      description: draft.description?.trim() || undefined,
      targets: normalizedTargets,
    });
  };

  return (
    <Modal show={show} onHide={onCancel} size="xl" fullscreen="lg-down" centered dialogClassName="dashboard-editor-dialog">
      <Form onSubmit={submit}>
        <Modal.Header closeButton>
          <Modal.Title>{panel?.name ? `Edit ${panel.name}` : "Panel editor"}</Modal.Title>
        </Modal.Header>
        <Modal.Body className="dashboard-panel-editor-body">
          {error && <Alert variant="danger" className="py-2">{error}</Alert>}
          <div className="dashboard-panel-editor-grid">
            <div className="dashboard-editor-main">
              <section className="dashboard-editor-preview" aria-label="Panel preview">
                <div className="dashboard-editor-section-title">Live preview</div>
                <div className="dashboard-editor-preview-canvas">
                  <DashboardPanel
                    panel={draft}
                    start={start}
                    end={end}
                    refreshKey={refreshKey}
                    variables={variables}
                    datasources={datasources}
                    allowShare={false}
                    panelHeight={330}
                  />
                </div>
              </section>

              {!isStatic && <div className="dashboard-editor-queries">
                <section className="dashboard-editor-section dashboard-editor-datasource">
                  <div className="dashboard-editor-section-title">Data source</div>
                  <Form.Select value={String(draft.datasourceValue || "")} onChange={(event) => patchDraft({ datasourceValue: event.target.value.startsWith("${") ? event.target.value : Number(event.target.value) })}>
                    <option value="" disabled>Select a Prometheus data source</option>
                    {typeof draft.datasourceValue === "string" && <option value={draft.datasourceValue}>Dashboard variable ({draft.datasourceValue})</option>}
                    {availableDatasources.map((item) => <option key={item.id} value={item.id}>{item.name}{item.is_default ? " (default)" : ""}</option>)}
                  </Form.Select>
                </section>

                <section className="dashboard-editor-section">
                  <div className="d-flex align-items-center justify-content-between mb-3">
                    <div className="dashboard-editor-section-title mb-0">Queries</div>
                    <Button size="sm" variant="outline-secondary" onClick={() => patchDraft({ targets: [...draft.targets, { refId: nextRefId(draft.targets), expr: "", instant: draft.type !== "timeseries" && draft.type !== "heatmap" }] })}>
                      <FontAwesomeIcon icon={faPlus} className="me-2" />Query
                    </Button>
                  </div>
                  {draft.targets.map((target, index) => <div className="dashboard-query-editor" key={`${target.refId}-${index}`}>
                    <div className="dashboard-query-editor-head">
                      <Form.Control aria-label="Query reference" className="dashboard-query-ref" size="sm" maxLength={8} value={target.refId} onChange={(event) => updateTarget(index, { refId: event.target.value })} />
                      <Form.Check type="switch" label="Instant" checked={Boolean(target.instant)} onChange={(event) => updateTarget(index, { instant: event.target.checked })} />
                      <Form.Check type="switch" label="Hidden" checked={Boolean(target.hide)} onChange={(event) => updateTarget(index, { hide: event.target.checked })} />
                      <Button variant="link" size="sm" className="text-danger ms-auto" title="Delete query" disabled={draft.targets.length === 1} onClick={() => patchDraft({ targets: draft.targets.filter((_item, targetIndex) => targetIndex !== index) })}><FontAwesomeIcon icon={faTrash} /></Button>
                    </div>
                    <Form.Group className="mb-2">
                      <Form.Label>PromQL</Form.Label>
                      <Form.Control className="monitoring-code" as="textarea" rows={4} value={target.expr} onChange={(event) => updateTarget(index, { expr: event.target.value })} />
                    </Form.Group>
                    <div className="dashboard-query-row">
                      <Form.Group>
                        <Form.Label>Legend</Form.Label>
                        <Form.Control size="sm" placeholder="{{instance}}" value={target.legend || ""} onChange={(event) => updateTarget(index, { legend: event.target.value })} />
                      </Form.Group>
                      <Form.Group>
                        <Form.Label>Max data points</Form.Label>
                        <Form.Control size="sm" min={1} type="number" value={target.maxDataPoints ?? ""} onChange={(event) => updateTarget(index, { maxDataPoints: optionalNumber(event.target.value) })} />
                      </Form.Group>
                    </div>
                  </div>)}
                </section>
              </div>}
            </div>

            <div className="dashboard-editor-form">
              <section className="dashboard-editor-section">
                <div className="dashboard-editor-section-title">Panel</div>
                <Form.Group className="mb-3">
                  <Form.Label>Name</Form.Label>
                  <Form.Control aria-label="Panel name" required maxLength={160} value={draft.name} onChange={(event) => patchDraft({ name: event.target.value, titleKey: undefined })} />
                </Form.Group>
                <Form.Group className="mb-3">
                  <Form.Label>Visualization</Form.Label>
                  <Form.Select value={draft.type} onChange={(event) => changeType(event.target.value as DashboardPanelType)}>
                    {PANEL_TYPE_OPTIONS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
                  </Form.Select>
                </Form.Group>
                <Form.Group>
                  <Form.Label>Description</Form.Label>
                  <Form.Control as="textarea" rows={2} maxLength={500} value={draft.description || ""} onChange={(event) => patchDraft({ description: event.target.value })} />
                </Form.Group>
              </section>

              {draft.type === "text" && <section className="dashboard-editor-section">
                <div className="dashboard-editor-section-title">Content</div>
                <Form.Control as="textarea" rows={8} value={String(custom.content || "")} onChange={(event) => patchDraft({ custom: { ...custom, content: event.target.value } })} />
              </section>}

              {draft.type === "iframe" && <section className="dashboard-editor-section">
                <div className="dashboard-editor-section-title">Embedded page</div>
                <Form.Group>
                  <Form.Label>Same-origin URL</Form.Label>
                  <Form.Control type="url" placeholder="/path/on/this/ambari/server" value={String(custom.url || "")} onChange={(event) => patchDraft({ custom: { ...custom, url: event.target.value } })} />
                </Form.Group>
              </section>}

              <DashboardVisualizationOptions panel={draft} onChange={setDraft} />

              {!isStatic && <>
                <section className="dashboard-editor-section">
                  <div className="dashboard-editor-section-title">Value display</div>
                  <div className="dashboard-option-grid">
                    <Form.Group>
                      <Form.Label>Unit</Form.Label>
                      <Form.Select value={String(standardOptions.util || "none")} onChange={(event) => setStandardOption("util", event.target.value)}>
                        {UNIT_OPTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                      </Form.Select>
                    </Form.Group>
                    <Form.Group>
                      <Form.Label>Decimals</Form.Label>
                      <Form.Control min={0} max={8} type="number" value={String(standardOptions.decimals ?? "")} onChange={(event) => setStandardOption("decimals", optionalNumber(event.target.value))} />
                    </Form.Group>
                    <Form.Group>
                      <Form.Label>Minimum</Form.Label>
                      <Form.Control type="number" value={String(standardOptions.min ?? "")} onChange={(event) => setStandardOption("min", optionalNumber(event.target.value))} />
                    </Form.Group>
                    <Form.Group>
                      <Form.Label>Maximum</Form.Label>
                      <Form.Control type="number" value={String(standardOptions.max ?? "")} onChange={(event) => setStandardOption("max", optionalNumber(event.target.value))} />
                    </Form.Group>
                  </div>
                </section>

                <section className="dashboard-editor-section">
                  <div className="d-flex align-items-center justify-content-between mb-3">
                    <div className="dashboard-editor-section-title mb-0">Thresholds</div>
                    <Button size="sm" variant="outline-secondary" onClick={() => setThresholds([...thresholds, { value: 0, color: "red" }])}><FontAwesomeIcon icon={faPlus} className="me-2" />Threshold</Button>
                  </div>
                  <div className="dashboard-threshold-list">
                    {thresholds.map((threshold, index) => <div className="dashboard-threshold-row" key={index}>
                      <ColorEditor label={`Threshold ${index + 1} color`} value={threshold.color} onChange={(color) => setThresholds(thresholds.map((item, itemIndex) => itemIndex === index ? { ...item, color } : item))} />
                      <Form.Control aria-label={`Threshold ${index + 1} value`} size="sm" type="number" disabled={index === 0} placeholder={index === 0 ? "Base" : "Value"} value={index === 0 ? "" : String(threshold.value ?? "")} onChange={(event) => setThresholds(thresholds.map((item, itemIndex) => itemIndex === index ? { ...item, value: optionalNumber(event.target.value) ?? 0 } : item))} />
                      <Button variant="link" size="sm" className="text-danger" title="Delete threshold" disabled={index === 0} onClick={() => setThresholds(thresholds.filter((_item, itemIndex) => itemIndex !== index))}><FontAwesomeIcon icon={faTrash} /></Button>
                    </div>)}
                  </div>
                </section>

                <section className="dashboard-editor-section">
                  <div className="d-flex align-items-center justify-content-between mb-3">
                    <div className="dashboard-editor-section-title mb-0">Value mappings</div>
                    <Button size="sm" variant="outline-secondary" onClick={() => setValueMappings([...valueMappings, { type: "range", match: { from: 0, to: 0 }, result: { color: "#278541" } }])}><FontAwesomeIcon icon={faPlus} className="me-2" />Range</Button>
                  </div>
                  {valueMappings.length === 0
                    ? <p className="dashboard-editor-empty">No value mappings configured.</p>
                    : <div className="dashboard-value-mapping-list">{valueMappings.map((mapping, index) => {
                      const match = asObject(mapping.match);
                      const result = asObject(mapping.result);
                      const updateMapping = (values: JsonObject) => setValueMappings(valueMappings.map((item, itemIndex) => itemIndex === index ? { ...item, ...values } : item));
                      return <div className="dashboard-value-mapping-row" key={index}>
                        <Form.Group><Form.Label>From</Form.Label><Form.Control aria-label={`Mapping ${index + 1} from`} size="sm" type="number" placeholder="-infinity" value={typeof match.from === "number" ? match.from : ""} onChange={(event) => updateMapping({ match: { ...match, from: optionalNumber(event.target.value) ?? null } })} /></Form.Group>
                        <Form.Group><Form.Label>To</Form.Label><Form.Control aria-label={`Mapping ${index + 1} to`} size="sm" type="number" placeholder="+infinity" value={typeof match.to === "number" ? match.to : ""} onChange={(event) => updateMapping({ match: { ...match, to: optionalNumber(event.target.value) ?? null } })} /></Form.Group>
                        <Form.Group className="dashboard-value-mapping-color"><Form.Label>Color</Form.Label><ColorEditor label={`Mapping ${index + 1} color`} value={result.color} onChange={(color) => updateMapping({ result: { ...result, color } })} /></Form.Group>
                        <Form.Group><Form.Label>Display text</Form.Label><Form.Control aria-label={`Mapping ${index + 1} text`} size="sm" placeholder="Optional" value={typeof result.text === "string" ? result.text : ""} onChange={(event) => updateMapping({ result: { ...result, text: event.target.value || undefined } })} /></Form.Group>
                        <Button variant="link" size="sm" className="text-danger dashboard-value-mapping-delete" title="Delete value mapping" onClick={() => setValueMappings(valueMappings.filter((_item, itemIndex) => itemIndex !== index))}><FontAwesomeIcon icon={faTrash} /></Button>
                      </div>;
                    })}</div>}
                </section>
              </>}
            </div>
          </div>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="outline-secondary" onClick={onCancel}>Cancel</Button>
          <Button variant="success" type="submit">Apply panel</Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}
