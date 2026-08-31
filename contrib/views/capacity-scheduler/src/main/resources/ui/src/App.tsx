/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {
  AlertTriangle,
  Braces,
  Check,
  ChevronDown,
  Download,
  History,
  LoaderCircle,
  Plus,
  RefreshCw,
  RotateCcw,
  Save,
  Settings2,
  SlidersHorizontal,
  Trash2,
} from "lucide-react";
import { useEffect, useState } from "react";
import { CapacityApiError, createCapacityApi, type CapacityApi } from "./api";
import {
  addQueue,
  deleteQueue,
  parseCapacityModel,
  propertiesToXml,
  propertyDiff,
  renameQueue,
  serializeCapacityModel,
  validateCapacityModel,
} from "./capacityModel";
import Dialog from "./components/Dialog";
import QueueEditor from "./components/QueueEditor";
import QueueTree from "./components/QueueTree";
import type { CapacityModel, Properties, RuntimeInfo, SchedulerConfig, VersionInfo } from "./types";

type Tab = "queues" | "scheduler" | "mappings" | "advanced";
type SaveMode = "save" | "refresh" | "restart";

const defaultApi = createCapacityApi();
const cloneModel = (model: CapacityModel) => structuredClone(model);
const describeError = (reason: unknown) => reason instanceof CapacityApiError
  ? `${reason.message}${reason.status ? ` (HTTP ${reason.status})` : ""}`
  : reason instanceof Error ? reason.message : String(reason);
const booleanConfig = (value: string | null) => value === "true";
const currentProperties = (model: CapacityModel | null) => model ? serializeCapacityModel(model) : {};
const propertiesMatch = (left: Properties, right: Properties) => JSON.stringify(left) === JSON.stringify(right);

const isKnownProperty = (key: string, queuePaths: string[] = []) => {
  const scheduler = /^yarn\.scheduler\.capacity\.(maximum-applications|maximum-am-resource-percent|node-locality-delay|resource-calculator|queue-mappings|queue-mappings-override\.enable)$/;
  if (scheduler.test(key)) return true;
  const base = [...queuePaths]
    .sort((left, right) => right.length - left.length)
    .map((path) => `yarn.scheduler.capacity.${path}.`)
    .find((prefix) => key.startsWith(prefix));
  if (!base) return false;
  const suffix = key.slice(base.length);
  return /^(queues|capacity|maximum-capacity|state|acl_administer_queue|acl_submit_applications|user-limit-factor|minimum-user-limit-percent|maximum-applications|maximum-am-resource-percent|ordering-policy|ordering-policy\.fair\.enable-size-based-weight|priority|maximum-allocation-mb|maximum-allocation-vcores|maximum-application-lifetime|default-application-lifetime|disable_preemption|default-node-label-expression|accessible-node-labels)$/.test(suffix)
    || /^accessible-node-labels\.[^.]+\.(capacity|maximum-capacity)$/.test(suffix);
};

const saveBlob = (content: string, type: string, filename: string) => {
  const url = URL.createObjectURL(new Blob([content], { type }));
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
};

const absoluteCapacity = (model: CapacityModel, path: string) => {
  let queue = model.queues.find((candidate) => candidate.path === path);
  let ratio = 1;
  while (queue) {
    ratio *= queue.capacity / 100;
    queue = model.queues.find((candidate) => candidate.path === queue?.parentPath);
  }
  return ratio * 100;
};

const requiresRestart = (model: CapacityModel) => {
  const currentPaths = new Set(model.queues.map((queue) => queue.path));
  if (model.originalQueuePaths.some((path) => !currentPaths.has(path))) return true;
  const originalChildren = new Set(model.originalQueuePaths.map((path) => path.split(".").slice(0, -1).join(".")));
  return model.queues.some((queue) => !queue.sourcePath && !originalChildren.has(queue.parentPath));
};

export default function App({ api = defaultApi }: { api?: CapacityApi }) {
  const [model, setModel] = useState<CapacityModel | null>(null);
  const [loadedModel, setLoadedModel] = useState<CapacityModel | null>(null);
  const [currentTag, setCurrentTag] = useState("");
  const [versions, setVersions] = useState<VersionInfo[]>([]);
  const [runtime, setRuntime] = useState<RuntimeInfo>({
    stackId: "",
    isOperator: false,
    isRmOffline: false,
    nodeLabelsEnabled: false,
    nodeLabelsConfigured: false,
    rangerEnabled: false,
    preemptionEnabled: false,
    rmQueueStates: {},
  });
  const [nodeLabels, setNodeLabels] = useState<string[]>([]);
  const [selectedPath, setSelectedPath] = useState("root");
  const [tab, setTab] = useState<Tab>("queues");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [addParent, setAddParent] = useState("");
  const [newQueueName, setNewQueueName] = useState("");
  const [deletePath, setDeletePath] = useState("");
  const [saveMode, setSaveMode] = useState<SaveMode | null>(null);
  const [configNote, setConfigNote] = useState("");
  const [showSaveMenu, setShowSaveMenu] = useState(false);
  const [customKey, setCustomKey] = useState("");
  const [customValue, setCustomValue] = useState("");

  const updateRmQueues = async () => {
    try {
      const queues = await api.rmQueues();
      setRuntime((current) => ({
        ...current,
        isRmOffline: false,
        rmQueueStates: Object.fromEntries(queues.map((queue) => [queue.path, queue.state])),
      }));
    } catch {
      setRuntime((current) => ({ ...current, isRmOffline: true, rmQueueStates: {} }));
    }
  };

  const load = async () => {
    setLoading(true);
    setError("");
    try {
      const [cluster, privilege] = await Promise.all([api.cluster(), api.privilege()]);
      const [labelsResult, versionsResult, rangerResult, labelsEnabledResult, preemptionResult] = await Promise.allSettled([
        api.nodeLabels(),
        api.versions(),
        api.configValue("ranger-yarn-plugin-properties", "ranger-yarn-plugin-enabled"),
        api.configValue("yarn-site", "yarn.node-labels.enabled"),
        api.configValue("yarn-site", "yarn.resourcemanager.scheduler.monitor.enable"),
      ]);
      const labels = labelsResult.status === "fulfilled" ? labelsResult.value : [];
      const latest = await api.latest();
      const parsed = parseCapacityModel(latest, labels);
      setNodeLabels(labels);
      setModel(parsed);
      setLoadedModel(cloneModel(parsed));
      setCurrentTag(parsed.tag);
      setSelectedPath("root");
      setVersions(versionsResult.status === "fulfilled" ? versionsResult.value : []);
      setRuntime((current) => ({
        ...current,
        stackId: cluster.Clusters?.version ?? "",
        isOperator: privilege,
        nodeLabelsEnabled: labelsEnabledResult.status === "fulfilled" && booleanConfig(labelsEnabledResult.value),
        nodeLabelsConfigured: labelsResult.status === "fulfilled",
        rangerEnabled: rangerResult.status === "fulfilled" && booleanConfig(rangerResult.value),
        preemptionEnabled: preemptionResult.status === "fulfilled" && booleanConfig(preemptionResult.value),
      }));
      await updateRmQueues();
    } catch (reason) {
      setError(describeError(reason));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [api]);
  useEffect(() => {
    const timer = window.setInterval(updateRmQueues, 60_000);
    return () => window.clearInterval(timer);
  }, [api]);

  if (loading) return <main className="center-state"><LoaderCircle className="spin" size={32} /><h1>Loading Capacity Scheduler</h1><p>Reading cluster configuration and ResourceManager state.</p></main>;
  if (!model || !loadedModel) return <main className="center-state error-state"><AlertTriangle size={34} /><h1>Capacity Scheduler is unavailable</h1><p>{error || "No capacity-scheduler configuration was returned."}</p><button type="button" onClick={load}><RefreshCw size={16} /> Retry</button></main>;

  const properties = currentProperties(model);
  const baselineProperties = currentProperties(loadedModel);
  const dirty = !propertiesMatch(properties, baselineProperties);
  const issues = validateCapacityModel(model);
  const selected = model.queues.find((queue) => queue.path === selectedPath) ?? model.queues[0];
  const selectedOriginal = selected?.sourcePath
    ? loadedModel.queues.find((queue) => queue.path === selected.sourcePath)
    : undefined;
  const mustRestart = requiresRestart(model);
  const invalidPaths = new Set(issues.map((issue) => issue.path));
  const schedulerIssues = issues.filter((issue) => issue.path === "scheduler");

  const updateQueue = (nextQueue: typeof selected) => {
    if (!nextQueue) return;
    setModel((current) => current ? { ...current, queues: current.queues.map((queue) => queue.path === nextQueue.path ? nextQueue : queue) } : current);
  };

  const updateScheduler = <K extends keyof SchedulerConfig>(key: K, value: SchedulerConfig[K]) => setModel((current) => current
    ? { ...current, scheduler: { ...current.scheduler, [key]: value } }
    : current);

  const createQueue = () => {
    const name = newQueueName.trim();
    if (!name || /\s/.test(name) || model.queues.some((queue) => queue.name.toLowerCase() === name.toLowerCase())) {
      setError(!name ? "Enter a queue name." : /\s/.test(name) ? "Queue names cannot contain whitespace." : "A queue with this name already exists.");
      return;
    }
    const next = addQueue(model, addParent, name);
    setModel(next);
    setSelectedPath(`${addParent}.${name}`);
    setAddParent("");
    setNewQueueName("");
    setError("");
  };

  const renameSelected = (name: string) => {
    if (!selected || !name || /\s/.test(name) || model.queues.some((queue) => queue.path !== selected.path && queue.name.toLowerCase() === name.toLowerCase())) {
      setError("Queue names must be non-empty, contain no whitespace, and be unique.");
      return;
    }
    const nextPath = `${selected.parentPath}.${name}`;
    setModel(renameQueue(model, selected.path, name));
    setSelectedPath(nextPath);
    setError("");
  };

  const removeSelected = () => {
    const parent = model.queues.find((queue) => queue.path === deletePath)?.parentPath || "root";
    setModel(deleteQueue(model, deletePath));
    setSelectedPath(parent);
    setDeletePath("");
  };

  const discard = () => {
    setModel(cloneModel(loadedModel));
    if (!loadedModel.queues.some((queue) => queue.path === selectedPath)) setSelectedPath("root");
    setNotice("Unsaved changes were discarded.");
    setError("");
  };

  const resetQueue = () => {
    if (!selected || !selectedOriginal) return;
    updateQueue({
      ...cloneModel({ ...loadedModel, queues: [selectedOriginal] }).queues[0],
      path: selected.path,
      parentPath: selected.parentPath,
      name: selected.name,
      sourcePath: selected.sourcePath,
    });
  };

  const save = async () => {
    if (!saveMode || issues.length) return;
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await api.save(properties, configNote);
      if (saveMode === "refresh") await api.refresh();
      if (saveMode === "restart") await api.restart();
      const latest = parseCapacityModel(await api.latest(), nodeLabels);
      setModel(latest);
      setLoadedModel(cloneModel(latest));
      setCurrentTag(latest.tag);
      setVersions(await api.versions().catch(() => versions));
      await updateRmQueues();
      setSaveMode(null);
      setConfigNote("");
      setNotice(saveMode === "save" ? "Configuration saved." : saveMode === "refresh" ? "Configuration saved and queue refresh requested." : "Configuration saved and ResourceManager restart requested.");
    } catch (reason) {
      setError(describeError(reason));
    } finally {
      setBusy(false);
    }
  };

  const loadVersion = async (version: VersionInfo) => {
    if (dirty && !window.confirm("Discard unsaved changes and load this version?")) return;
    setBusy(true);
    setError("");
    try {
      const parsed = parseCapacityModel(await api.byTag(version.tag), nodeLabels);
      setModel(parsed);
      setLoadedModel(cloneModel(parsed));
      setSelectedPath("root");
      setNotice(`Loaded ${version.tag}. Saving will create a new configuration version.`);
    } catch (reason) {
      setError(describeError(reason));
    } finally {
      setBusy(false);
    }
  };

  const setCustomProperty = (key: string, value: string | null) => setModel((current) => {
    if (!current) return current;
    const rawProperties = { ...current.rawProperties };
    if (value === null) delete rawProperties[key]; else rawProperties[key] = value;
    return { ...current, rawProperties };
  });

  const addCustomProperty = () => {
    const key = customKey.trim();
    if (!key || isKnownProperty(key, model.queues.map((queue) => queue.path))) {
      setError(!key ? "Enter a property name." : "Use the Scheduler, Queues, or Mappings editor for managed properties.");
      return;
    }
    setCustomProperty(key, customValue);
    setCustomKey("");
    setCustomValue("");
    setError("");
  };

  const tabs: Array<{ id: Tab; label: string; icon: React.ReactNode }> = [
    { id: "queues", label: "Queues", icon: <SlidersHorizontal size={16} /> },
    { id: "scheduler", label: "Scheduler", icon: <Settings2 size={16} /> },
    { id: "mappings", label: "Queue mappings", icon: <Braces size={16} /> },
    { id: "advanced", label: "Advanced", icon: <History size={16} /> },
  ];

  return <main className="capacity-app">
    <header className="app-header">
      <div><span className="eyebrow">YARN queue manager</span><h1>Capacity Scheduler</h1></div>
      <div className="header-status">
        <span className={`rm-status ${runtime.isRmOffline ? "offline" : "online"}`}>{runtime.isRmOffline ? "ResourceManager offline" : "ResourceManager connected"}</span>
        <button className="icon-button dark" type="button" title="Reload ResourceManager state" onClick={updateRmQueues}><RefreshCw size={17} /></button>
      </div>
    </header>

    <nav className="tabs" aria-label="Capacity Scheduler sections">
      {tabs.map((item) => <button type="button" key={item.id} className={tab === item.id ? "active" : ""} onClick={() => setTab(item.id)}>{item.icon}{item.label}</button>)}
    </nav>

    {(error || notice || !runtime.isOperator) && <div className="messages">
      {error && <div className="message error" role="alert"><AlertTriangle size={17} /><span>{error}</span><button type="button" title="Dismiss" onClick={() => setError("")}>×</button></div>}
      {notice && <div className="message success"><Check size={17} /><span>{notice}</span><button type="button" title="Dismiss" onClick={() => setNotice("")}>×</button></div>}
      {!runtime.isOperator && <div className="message warning"><AlertTriangle size={17} /><span>You can inspect the configuration, but only Ambari or cluster administrators and operators can edit it.</span></div>}
    </div>}

    <div className="workspace">
      <section className="main-column">
        {tab === "queues" && selected && <div className="queues-layout">
          <aside className="queue-sidebar">
            <div className="panel-heading"><div><span className="eyebrow">Hierarchy</span><h2>Queues</h2></div><span>{model.queues.length}</span></div>
            <QueueTree
              queues={model.queues}
              selectedPath={selected.path}
              operator={runtime.isOperator}
              rmOffline={runtime.isRmOffline}
              rmStates={runtime.rmQueueStates}
              issues={invalidPaths}
              onSelect={setSelectedPath}
              onAdd={(path) => { setAddParent(path); setNewQueueName(""); }}
              onDelete={setDeletePath}
            />
          </aside>
          <QueueEditor
            queue={selected}
            original={selectedOriginal}
            children={model.queues.filter((queue) => queue.parentPath === selected.path)}
            runtime={runtime}
            absoluteCapacity={absoluteCapacity(model, selected.path)}
            issues={issues.filter((issue) => issue.path === selected.path)}
            onChange={updateQueue}
            onRename={renameSelected}
            onReset={resetQueue}
          />
        </div>}

        {tab === "scheduler" && <section className="content-panel">
          <div className="panel-heading"><div><span className="eyebrow">Cluster defaults</span><h2>Scheduler</h2></div></div>
          {schedulerIssues.filter((issue) => issue.field !== "queueMappings").map((issue) => <div className="inline-error" key={issue.message}>{issue.message}</div>)}
          <fieldset disabled={!runtime.isOperator}><div className="field-grid scheduler-grid">
            <label className="field"><span>Maximum applications</span><input type="number" min="1" value={model.scheduler.maximumApplications} onChange={(event) => updateScheduler("maximumApplications", Number(event.target.value))} /></label>
            <label className="field"><span>Maximum AM resource (%)</span><input type="number" min="0" max="100" value={model.scheduler.maximumAmResourcePercent} onChange={(event) => updateScheduler("maximumAmResourcePercent", Number(event.target.value))} /></label>
            <label className="field"><span>Node locality delay</span><input type="number" min="0" value={model.scheduler.nodeLocalityDelay} onChange={(event) => updateScheduler("nodeLocalityDelay", Number(event.target.value))} /></label>
            <label className="field wide"><span>Resource calculator</span><select value={model.scheduler.resourceCalculator} onChange={(event) => updateScheduler("resourceCalculator", event.target.value)}><option value="org.apache.hadoop.yarn.util.resource.DefaultResourceCalculator">Default Resource Calculator</option><option value="org.apache.hadoop.yarn.util.resource.DominantResourceCalculator">Dominant Resource Calculator</option></select></label>
          </div></fieldset>
        </section>}

        {tab === "mappings" && <section className="content-panel">
          <div className="panel-heading"><div><span className="eyebrow">Application placement</span><h2>User queue mappings</h2></div></div>
          {schedulerIssues.filter((issue) => issue.field === "queueMappings").map((issue) => <div className="inline-error" key={issue.message}>{issue.message}</div>)}
          <fieldset disabled={!runtime.isOperator}>
            <label className="field"><span>Mappings</span><textarea rows={7} value={model.scheduler.queueMappings} placeholder="u:%user:%primary_group" onChange={(event) => updateScheduler("queueMappings", event.target.value)} /><small>Comma-separated mappings use <code>u|g:name:leaf_queue</code>. The two dynamic targets <code>%user</code> and <code>%primary_group</code> are supported.</small></label>
            <label className="check-row"><input type="checkbox" checked={model.scheduler.queueMappingsOverride} onChange={(event) => updateScheduler("queueMappingsOverride", event.target.checked)} /> Override a queue explicitly requested by the application</label>
          </fieldset>
        </section>}

        {tab === "advanced" && <section className="content-panel advanced-panel">
          <div className="panel-heading"><div><span className="eyebrow">Raw configuration</span><h2>Advanced properties and diff</h2></div><div className="download-actions"><button type="button" className="secondary-button" onClick={() => saveBlob(propertiesToXml(properties), "application/xml", "capacity-scheduler.xml")}><Download size={15} /> XML</button><button type="button" className="secondary-button" onClick={() => saveBlob(Object.entries(properties).map(([key, value]) => `${key}=${value}`).join("\n"), "text/plain", "capacity-scheduler.txt")}><Download size={15} /> Properties</button></div></div>
          <h3>Custom properties</h3>
          <p className="section-copy">Properties not managed by the form are preserved across queue edits, renames, and saves.</p>
          <div className="custom-add"><input aria-label="Property name" placeholder="Property name" value={customKey} onChange={(event) => setCustomKey(event.target.value)} disabled={!runtime.isOperator} /><input aria-label="Property value" placeholder="Value" value={customValue} onChange={(event) => setCustomValue(event.target.value)} disabled={!runtime.isOperator} /><button type="button" className="secondary-button" disabled={!runtime.isOperator} onClick={addCustomProperty}><Plus size={15} /> Add</button></div>
          <div className="property-table">
            {Object.entries(model.rawProperties).filter(([key]) => !isKnownProperty(key, model.originalQueuePaths)).sort(([left], [right]) => left.localeCompare(right)).map(([key, value]) => <div className="property-row" key={key}><code>{key}</code><input value={value} disabled={!runtime.isOperator} onChange={(event) => setCustomProperty(key, event.target.value)} /><button type="button" title="Remove property" disabled={!runtime.isOperator} onClick={() => setCustomProperty(key, null)}><Trash2 size={15} /></button></div>)}
          </div>
          <h3>Pending changes</h3>
          <div className="diff-table">
            {propertyDiff(baselineProperties, properties).length === 0 ? <p className="empty-state">No pending property changes.</p> : propertyDiff(baselineProperties, properties).map((item) => <div className="diff-row" key={item.key}><code>{item.key}</code><span className="before">{item.before ?? "(not set)"}</span><span className="after">{item.after ?? "(removed)"}</span></div>)}
          </div>
          <details><summary>Generated capacity-scheduler.xml</summary><pre>{propertiesToXml(properties)}</pre></details>
        </section>}
      </section>

      <aside className="right-column">
        <section className="versions-panel">
          <div className="panel-heading"><div><span className="eyebrow">Configuration history</span><h2>Versions</h2></div><History size={18} /></div>
          <div className="version-list">
            {[...versions].sort((left, right) => (right.version ?? right.created ?? 0) - (left.version ?? left.created ?? 0)).map((version) => <button type="button" key={`${version.version}-${version.tag}`} className={model.tag === version.tag ? "loaded" : ""} disabled={busy} onClick={() => loadVersion(version)}><span><strong>{version.version !== undefined ? `v${version.version}` : version.tag}</strong>{version.tag === currentTag && <small>Current</small>}{version.tag === model.tag && version.tag !== currentTag && <small>Loaded</small>}</span><code>{version.tag}</code></button>)}
            {!versions.length && <p className="empty-state">No configuration history is available.</p>}
          </div>
        </section>
      </aside>
    </div>

    <footer className="save-bar">
      <div><strong>{dirty ? `${propertyDiff(baselineProperties, properties).length} pending property change(s)` : "Configuration is unchanged"}</strong>{issues.length > 0 && <span className="invalid-summary">{issues.length} validation issue(s) must be resolved.</span>}{mustRestart && dirty && <span>Queue hierarchy changes require a ResourceManager restart.</span>}</div>
      <div className="save-actions">
        <button type="button" className="secondary-button" disabled={!dirty || busy} onClick={discard}><RotateCcw size={16} /> Discard</button>
        <div className="split-button">
          <button type="button" className="primary-button" disabled={!runtime.isOperator || !dirty || issues.length > 0 || busy} onClick={() => setSaveMode(mustRestart ? "restart" : "refresh")}><Save size={16} /> {mustRestart ? "Save and restart" : "Save and refresh"}</button>
          <button type="button" className="primary-button menu-trigger" aria-label="More save options" disabled={!runtime.isOperator || !dirty || issues.length > 0 || busy} onClick={() => setShowSaveMenu((current) => !current)}><ChevronDown size={16} /></button>
          {showSaveMenu && <div className="save-menu"><button type="button" onClick={() => { setSaveMode("save"); setShowSaveMenu(false); }}>Save only</button><button type="button" disabled={mustRestart} onClick={() => { setSaveMode("refresh"); setShowSaveMenu(false); }}>Save and refresh queues</button><button type="button" onClick={() => { setSaveMode("restart"); setShowSaveMenu(false); }}>Save and restart ResourceManager</button></div>}
        </div>
      </div>
    </footer>

    {addParent && <Dialog title={`Add child queue to ${addParent}`} onClose={() => setAddParent("")}><div className="dialog-body"><label className="field"><span>Queue name</span><input autoFocus value={newQueueName} onChange={(event) => setNewQueueName(event.target.value)} onKeyDown={(event) => { if (event.key === "Enter") createQueue(); }} /></label></div><footer><button type="button" className="secondary-button" onClick={() => setAddParent("")}>Cancel</button><button type="button" className="primary-button" onClick={createQueue}>Add queue</button></footer></Dialog>}
    {deletePath && <Dialog title="Delete queue" onClose={() => setDeletePath("")}><div className="dialog-body"><p>Delete <strong>{deletePath}</strong> and all of its child queues? Saving this hierarchy change requires a ResourceManager restart.</p></div><footer><button type="button" className="secondary-button" onClick={() => setDeletePath("")}>Cancel</button><button type="button" className="danger-button" onClick={removeSelected}><Trash2 size={15} /> Delete queue</button></footer></Dialog>}
    {saveMode && <Dialog title={saveMode === "save" ? "Save configuration" : saveMode === "refresh" ? "Save and refresh queues" : "Save and restart ResourceManager"} onClose={() => { if (!busy) setSaveMode(null); }}><div className="dialog-body"><label className="field"><span>Version note</span><textarea rows={4} placeholder="What changed?" value={configNote} disabled={busy} onChange={(event) => setConfigNote(event.target.value)} /></label>{saveMode === "restart" && <p className="operation-warning"><AlertTriangle size={17} /> This action submits a restart request for every ResourceManager host.</p>}{saveMode === "refresh" && <p className="operation-warning"><RefreshCw size={17} /> This action sends REFRESHQUEUES to every ResourceManager host.</p>}</div><footer><button type="button" className="secondary-button" disabled={busy} onClick={() => setSaveMode(null)}>Cancel</button><button type="button" className="primary-button" disabled={busy} onClick={save}>{busy ? <LoaderCircle className="spin" size={16} /> : <Save size={16} />} Confirm</button></footer></Dialog>}
  </main>;
}
