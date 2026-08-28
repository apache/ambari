/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
import { Pencil, RotateCcw } from "lucide-react";
import { useState } from "react";
import type { QueueConfig, RuntimeInfo, ValidationIssue } from "../types";

type Props = {
  queue: QueueConfig;
  original?: QueueConfig;
  children: QueueConfig[];
  runtime: RuntimeInfo;
  absoluteCapacity: number;
  issues: ValidationIssue[];
  onChange: (queue: QueueConfig) => void;
  onRename: (name: string) => void;
  onReset: () => void;
};

const numberOrNull = (value: string) => value === "" ? null : Number(value);

const Field = ({ label, help, children }: { label: string; help?: string; children: React.ReactNode }) => (
  <label className="field">
    <span>{label}</span>
    {children}
    {help && <small>{help}</small>}
  </label>
);

export default function QueueEditor({ queue, original, children, runtime, absoluteCapacity, issues, onChange, onRename, onReset }: Props) {
  const [renaming, setRenaming] = useState(false);
  const [name, setName] = useState(queue.name);
  const operator = runtime.isOperator;
  const isLeaf = children.length === 0;
  const stackAtLeast = (major: number, minor: number) => {
    const [actualMajor = 0, actualMinor = 0] = (runtime.stackId.split("-").pop() ?? "").split(".").map(Number);
    return actualMajor > major || (actualMajor === major && actualMinor >= minor);
  };
  const update = <K extends keyof QueueConfig>(key: K, value: QueueConfig[K]) => onChange({ ...queue, [key]: value });
  const issueMessages = [...new Set(issues.map((issue) => issue.message))];
  return (
    <section className="editor-panel">
      <header className="editor-header">
        <div>
          <span className="eyebrow">Queue configuration</span>
          {renaming ? (
            <form className="rename-form" onSubmit={(event) => {
              event.preventDefault();
              onRename(name.trim());
              setRenaming(false);
            }}>
              <input autoFocus value={name} onChange={(event) => setName(event.target.value)} disabled={!operator} />
              <button type="submit" disabled={!name.trim()}>Apply</button>
              <button type="button" onClick={() => { setName(queue.name); setRenaming(false); }}>Cancel</button>
            </form>
          ) : (
            <h2>{queue.path} {operator && queue.path !== "root" && <button className="inline-icon" type="button" title="Rename queue" onClick={() => { setName(queue.name); setRenaming(true); }}><Pencil size={15} /></button>}</h2>
          )}
        </div>
        <div className="header-actions">
          <span className={`state-badge ${queue.state.toLowerCase()}`}>{queue.state}</span>
          {original && <button type="button" className="secondary-button" onClick={onReset}><RotateCcw size={15} /> Reset queue</button>}
        </div>
      </header>

      {issueMessages.length > 0 && <div className="validation-banner" role="alert">{issueMessages.map((message) => <div key={message}>{message}</div>)}</div>}

      <div className="editor-sections">
        <fieldset disabled={!operator}>
          <legend>Capacity and state</legend>
          <div className="field-grid">
            <Field label="Capacity (%)" help={`Configured absolute share: ${absoluteCapacity.toFixed(2)}%`}>
              <input type="number" min="0" max="100" step="0.01" value={queue.capacity} onChange={(event) => update("capacity", Number(event.target.value))} />
            </Field>
            <Field label="Maximum capacity (%)">
              <input type="number" min="0" max="100" step="0.01" value={queue.maximumCapacity} onChange={(event) => update("maximumCapacity", Number(event.target.value))} />
            </Field>
            <Field label="Queue state">
              <select value={queue.state} onChange={(event) => update("state", event.target.value as QueueConfig["state"])}>
                <option value="RUNNING">Running</option>
                <option value="STOPPED">Stopped</option>
              </select>
            </Field>
          </div>
        </fieldset>

        <fieldset disabled={!operator}>
          <legend>Resources</legend>
          <div className="field-grid">
            <Field label="User limit factor"><input type="number" min="0" step="0.01" value={queue.userLimitFactor} onChange={(event) => update("userLimitFactor", Number(event.target.value))} /></Field>
            <Field label="Minimum user limit (%)"><input type="number" min="0" max="100" value={queue.minimumUserLimitPercent} onChange={(event) => update("minimumUserLimitPercent", Number(event.target.value))} /></Field>
            <Field label="Maximum applications"><input type="number" min="0" placeholder="Inherited" value={queue.maximumApplications ?? ""} onChange={(event) => update("maximumApplications", numberOrNull(event.target.value))} /></Field>
            <Field label="Maximum AM resource (%)"><input type="number" min="0" max="100" placeholder="Inherited" value={queue.maximumAmResourcePercent ?? ""} onChange={(event) => update("maximumAmResourcePercent", numberOrNull(event.target.value))} /></Field>
            {stackAtLeast(2, 6) && <Field label="Priority"><input type="number" min="0" value={queue.priority} onChange={(event) => update("priority", Number(event.target.value))} /></Field>}
            <Field label="Maximum allocation (MB)"><input type="number" min="0" placeholder="Inherited" value={queue.maximumAllocationMb ?? ""} onChange={(event) => update("maximumAllocationMb", numberOrNull(event.target.value))} /></Field>
            <Field label="Maximum allocation (vcores)"><input type="number" min="0" placeholder="Inherited" value={queue.maximumAllocationVcores ?? ""} onChange={(event) => update("maximumAllocationVcores", numberOrNull(event.target.value))} /></Field>
            {isLeaf && <Field label="Ordering policy"><select value={queue.orderingPolicy} onChange={(event) => update("orderingPolicy", event.target.value)}><option value="fifo">FIFO</option><option value="fair">Fair</option></select></Field>}
            {isLeaf && queue.orderingPolicy === "fair" && <Field label="Size-based weight"><span className="switch-field"><input type="checkbox" checked={queue.enableSizeBasedWeight} onChange={(event) => update("enableSizeBasedWeight", event.target.checked)} /> Enabled</span></Field>}
            {isLeaf && <Field label="Maximum application lifetime (sec)"><input type="number" placeholder="Inherited" value={queue.maximumApplicationLifetime ?? ""} onChange={(event) => update("maximumApplicationLifetime", numberOrNull(event.target.value))} /></Field>}
            {isLeaf && <Field label="Default application lifetime (sec)"><input type="number" placeholder="Inherited" value={queue.defaultApplicationLifetime ?? ""} onChange={(event) => update("defaultApplicationLifetime", numberOrNull(event.target.value))} /></Field>}
          </div>
        </fieldset>

        <fieldset disabled={!operator}>
          <legend>Access control</legend>
          {runtime.rangerEnabled ? <p className="muted-message">Permissions are managed by Ranger.</p> : <div className="field-grid">
            <Field label="Administer queue ACL" help="Use * for anyone, a single space for nobody, or users and groups separated by a space."><input value={queue.aclAdministerQueue} onChange={(event) => update("aclAdministerQueue", event.target.value)} /></Field>
            <Field label="Submit applications ACL"><input value={queue.aclSubmitApplications} onChange={(event) => update("aclSubmitApplications", event.target.value)} /></Field>
          </div>}
        </fieldset>

        {stackAtLeast(2, 3) && runtime.preemptionEnabled && <fieldset disabled={!operator}>
          <legend>Preemption</legend>
          <div className="field-grid"><Field label="Preemption override"><select value={queue.preemptionOverride} onChange={(event) => update("preemptionOverride", event.target.value as QueueConfig["preemptionOverride"])}><option value="inherit">Inherit</option><option value="enabled">Enabled</option><option value="disabled">Disabled</option></select></Field></div>
        </fieldset>}

        {runtime.nodeLabelsEnabled && runtime.nodeLabelsConfigured && !runtime.isRmOffline && <fieldset disabled={!operator}>
          <legend>Node labels</legend>
          <label className="check-row"><input type="checkbox" checked={queue.labelsEnabled} onChange={(event) => update("labelsEnabled", event.target.checked)} /> Enable node-label access for this queue</label>
          {queue.labelsEnabled && <>
            <label className="check-row"><input type="checkbox" checked={queue.accessAllLabels} onChange={(event) => update("accessAllLabels", event.target.checked)} /> Access all labels</label>
            <div className="label-table">
              {queue.labels.map((label) => {
                const accessible = queue.accessAllLabels || queue.accessibleLabels.includes(label.name);
                return <div className="label-row" key={label.name}>
                  <label><input type="checkbox" checked={accessible} disabled={queue.accessAllLabels} onChange={(event) => update("accessibleLabels", event.target.checked ? [...queue.accessibleLabels, label.name] : queue.accessibleLabels.filter((name) => name !== label.name))} /> {label.name}</label>
                  <input aria-label={`${label.name} capacity`} type="number" min="0" max="100" value={label.capacity} disabled={!accessible} onChange={(event) => update("labels", queue.labels.map((item) => item.name === label.name ? { ...item, capacity: Number(event.target.value) } : item))} />
                  <input aria-label={`${label.name} maximum capacity`} type="number" min="0" max="100" value={label.maximumCapacity} disabled={!accessible} onChange={(event) => update("labels", queue.labels.map((item) => item.name === label.name ? { ...item, maximumCapacity: Number(event.target.value) } : item))} />
                  <label><input type="radio" name="default-node-label" checked={queue.defaultNodeLabelExpression === label.name} disabled={!accessible} onChange={() => update("defaultNodeLabelExpression", label.name)} /> Default</label>
                </div>;
              })}
            </div>
          </>}
        </fieldset>}
      </div>
    </section>
  );
}
